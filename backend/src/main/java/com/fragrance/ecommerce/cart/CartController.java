package com.fragrance.ecommerce.cart;

import com.fragrance.ecommerce.domain.Cart;
import com.fragrance.ecommerce.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {
    private final CartService cartService;
    public CartController(CartService cartService) { this.cartService = cartService; }

    @GetMapping
    public CartResponse get(@AuthenticationPrincipal AuthenticatedUser user) {
        return response(cartService.getOrCreate(user.id()));
    }
    @PostMapping("/items")
    public CartResponse add(@AuthenticationPrincipal AuthenticatedUser user,
                            @Valid @RequestBody CartDtos.ItemRequest request) {
        return response(cartService.add(user.id(), request.productId(), request.quantity()));
    }
    @PatchMapping("/items/{productId}")
    public CartResponse update(@AuthenticationPrincipal AuthenticatedUser user,
                               @PathVariable UUID productId,
                               @Valid @RequestBody CartDtos.QuantityRequest request) {
        return response(cartService.setQuantity(user.id(), productId, request.quantity()));
    }
    @DeleteMapping("/items/{productId}")
    public CartResponse remove(@AuthenticationPrincipal AuthenticatedUser user,
                               @PathVariable UUID productId) {
        return response(cartService.remove(user.id(), productId));
    }
    private CartResponse response(Cart cart) {
        var items = cart.getItems().stream().map(item -> new CartItemResponse(
            item.getProduct().getId(), item.getProduct().getName(), item.getProduct().getPrice(),
            item.getQuantity(), item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
        )).toList();
        return new CartResponse(items, items.stream().map(CartItemResponse::lineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
    public record CartResponse(java.util.List<CartItemResponse> items, BigDecimal total) {}
    public record CartItemResponse(UUID productId, String productName, BigDecimal unitPrice,
                                   int quantity, BigDecimal lineTotal) {}
}
