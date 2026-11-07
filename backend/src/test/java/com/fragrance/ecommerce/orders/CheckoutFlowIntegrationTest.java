package com.fragrance.ecommerce.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fragrance.ecommerce.auth.AuthDtos;
import com.fragrance.ecommerce.domain.Administrator;
import com.fragrance.ecommerce.domain.Brand;
import com.fragrance.ecommerce.domain.Category;
import com.fragrance.ecommerce.domain.Product;
import com.fragrance.ecommerce.payments.PaymentProvider;
import com.fragrance.ecommerce.repository.BrandRepository;
import com.fragrance.ecommerce.repository.CategoryRepository;
import com.fragrance.ecommerce.repository.ProductRepository;
import com.fragrance.ecommerce.repository.RoleRepository;
import com.fragrance.ecommerce.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end checkout smoke test through the HTTP API: a customer buys a product,
 * the (stubbed) payment provider confirms it through the webhook, and an
 * administrator fulfils the order. Stripe itself is replaced by a mock so the
 * test is deterministic and needs no credentials.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CheckoutFlowIntegrationTest {
    private static final String PASSWORD = "a-secure-password";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BrandRepository brands;
    @Autowired private CategoryRepository categories;
    @Autowired private ProductRepository products;
    @Autowired private RoleRepository roles;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean private PaymentProvider paymentProvider;

    private Product product;
    private String adminEmail;

    @BeforeEach
    void seed() {
        var suffix = UUID.randomUUID().toString().substring(0, 8);
        var brand = brands.save(new Brand("Smoke Brand " + suffix, "Test brand"));
        var category = categories.save(new Category("Smoke Category " + suffix, "Test category"));
        product = products.save(new Product(brand, category, "Smoke Dusk " + suffix,
            "Warm cedar and smoke.", new BigDecimal("42.00"), 5));
        adminEmail = "admin-" + suffix + "@example.test";
        var adminRole = roles.findByCode("ADMINISTRATOR").orElseThrow();
        users.save(new Administrator(adminEmail, passwordEncoder.encode(PASSWORD), "Smoke Admin", adminRole));
    }

    @Test
    void customerChecksOutAndAdministratorFulfilsTheOrder() throws Exception {
        var customer = registerAndLogin("buyer-" + UUID.randomUUID() + "@example.test");

        mockMvc.perform(post("/api/v1/cart/items").session(customer).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("productId", product.getId(), "quantity", 2))))
            .andExpect(status().is2xxSuccessful());

        var orderJson = mockMvc.perform(post("/api/v1/orders").session(customer).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("idempotencyKey", UUID.randomUUID().toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
            .andExpect(jsonPath("$.total").value(84.0))
            .andReturn().getResponse().getContentAsString();
        var orderId = UUID.fromString(objectMapper.readTree(orderJson).get("id").asText());
        assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(3);

        when(paymentProvider.createCheckout(any(), any()))
            .thenReturn(new PaymentProvider.CheckoutSession("cs_test_smoke", "https://checkout.example.test/cs_test_smoke"));
        mockMvc.perform(post("/api/v1/payments/checkout").session(customer).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("orderId", orderId))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.checkoutUrl").value("https://checkout.example.test/cs_test_smoke"));

        when(paymentProvider.verifyWebhook(eq("{}"), eq("t=1,v1=test")))
            .thenReturn(new PaymentProvider.VerifiedPayment(orderId, "cs_test_smoke", PaymentProvider.EventStatus.PAID));
        mockMvc.perform(post("/api/v1/webhooks/stripe")
                .header("Stripe-Signature", "t=1,v1=test")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/orders/" + orderId).session(customer))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(patch("/api/v1/admin/orders/" + orderId + "/status").session(customer).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", "SHIPPED"))))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/orders").session(customer))
            .andExpect(status().isForbidden());

        var admin = login(adminEmail);
        mockMvc.perform(get("/api/v1/admin/orders").param("status", "PAID").session(admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.id == '" + orderId + "')].customerEmail").exists());

        mockMvc.perform(patch("/api/v1/admin/orders/" + orderId + "/status").session(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", "DELIVERED"))))
            .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/v1/admin/orders/" + orderId + "/status").session(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", "SHIPPED"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SHIPPED"));
        mockMvc.perform(patch("/api/v1/admin/orders/" + orderId + "/status").session(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", "DELIVERED"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void checkoutProviderFailureMarksOrderFailedAndReleasesStock() throws Exception {
        var customer = registerAndLogin("failed-" + UUID.randomUUID() + "@example.test");
        mockMvc.perform(post("/api/v1/cart/items").session(customer).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("productId", product.getId(), "quantity", 1))))
            .andExpect(status().is2xxSuccessful());
        var orderJson = mockMvc.perform(post("/api/v1/orders").session(customer).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("idempotencyKey", UUID.randomUUID().toString()))))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        var orderId = objectMapper.readTree(orderJson).get("id").asText();

        when(paymentProvider.createCheckout(any(), any())).thenThrow(new IllegalStateException("provider down"));
        mockMvc.perform(post("/api/v1/payments/checkout").session(customer).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("orderId", orderId))))
            .andExpect(status().isBadGateway());

        mockMvc.perform(get("/api/v1/orders/" + orderId).session(customer))
            .andExpect(jsonPath("$.status").value("PAYMENT_FAILED"));
        assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(5);
    }

    private MockHttpSession registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new AuthDtos.RegisterRequest(email, PASSWORD, "Smoke Customer"))))
            .andExpect(status().isCreated());
        return login(email);
    }

    private MockHttpSession login(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new AuthDtos.LoginRequest(email, PASSWORD))))
            .andExpect(status().isOk())
            .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
