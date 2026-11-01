package com.fragrance.ecommerce.orders;

import com.fragrance.ecommerce.domain.Order;
import com.fragrance.ecommerce.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;
    public OrderController(OrderService orderService) { this.orderService = orderService; }
    @PostMapping
    public OrderDtos.OrderResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                          @Valid @RequestBody OrderDtos.CreateRequest request) {
        return response(orderService.create(user.id(), request.idempotencyKey()));
    }
    @GetMapping
    public OrderPage history(@AuthenticationPrincipal AuthenticatedUser user,
                             @PageableDefault(size = 20) Pageable pageable) {
        var page = orderService.history(user.id(), pageable);
        return new OrderPage(page.getContent().stream().map(this::response).toList(),
            page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
    @GetMapping("/{id}")
    public OrderDtos.OrderResponse get(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable UUID id) {
        return response(orderService.getOwned(user.id(), id));
    }
    private OrderDtos.OrderResponse response(Order order) {
        return new OrderDtos.OrderResponse(order.getId(), order.getStatus().name(), order.getTotal(),
            order.getItems().stream().map(item -> new OrderDtos.OrderItemResponse(
                item.getProductNameSnapshot(), item.getUnitPriceSnapshot(), item.getQuantity(),
                item.getLineTotal())).toList());
    }
    public record OrderPage(java.util.List<OrderDtos.OrderResponse> items, int page, int size,
                            long totalItems, int totalPages) {}
}
