package com.fragrance.ecommerce.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fragrance.ecommerce.domain.Customer;
import com.fragrance.ecommerce.domain.Role;
import com.fragrance.ecommerce.exception.ConflictException;
import com.fragrance.ecommerce.repository.RoleRepository;
import com.fragrance.ecommerce.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, roleRepository, new BCryptPasswordEncoder());
    }

    @Test
    void registerNormalizesEmailAndHashesPassword() {
        var role = new Role(UUID.randomUUID(), "CUSTOMER");
        when(userRepository.existsByEmailIgnoreCase("person@example.test")).thenReturn(false);
        when(roleRepository.findByCode("CUSTOMER")).thenReturn(Optional.of(role));
        when(userRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = authService.register(new AuthDtos.RegisterRequest(
            " Person@Example.Test ", "a-secure-password", "Sample Person"));

        assertThat(result.getEmail()).isEqualTo("person@example.test");
        assertThat(result.getPasswordHash()).isNotEqualTo("a-secure-password");
        assertThat(new BCryptPasswordEncoder().matches("a-secure-password", result.getPasswordHash()))
            .isTrue();
        verify(userRepository).save(any(Customer.class));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new AuthDtos.RegisterRequest(
            "person@example.test", "a-secure-password", "Sample Person")))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void registerRequiresConfiguredCustomerRole() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(roleRepository.findByCode("CUSTOMER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(new AuthDtos.RegisterRequest(
            "person@example.test", "a-secure-password", "Sample Person")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Customer role");
    }

    @Test
    void responseContainsOnlyPublicUserFields() {
        var role = new Role(UUID.randomUUID(), "CUSTOMER");
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(roleRepository.findByCode("CUSTOMER")).thenReturn(Optional.of(role));
        when(userRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.toResponse(authService.register(new AuthDtos.RegisterRequest(
            "person@example.test", "a-secure-password", "Sample Person")));

        assertThat(response.email()).isEqualTo("person@example.test");
        assertThat(response.roles()).containsExactly("CUSTOMER");
    }

    @Test
    void duplicateCheckUsesNormalizedEmail() {
        when(userRepository.existsByEmailIgnoreCase("person@example.test")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new AuthDtos.RegisterRequest(
            " PERSON@EXAMPLE.TEST ", "a-secure-password", "Sample Person")))
            .isInstanceOf(ConflictException.class);
        verify(userRepository).existsByEmailIgnoreCase("person@example.test");
    }
}
