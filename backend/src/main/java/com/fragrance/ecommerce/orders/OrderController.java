package com.fragrance.ecommerce.orders;

import com.fragrance.ecommerce.domain.Order;
import com.fragrance.ecommerce.domain.OrderStatus;
import com.fragrance.ecommerce.security.AuthenticatedUser;
import com.fragrance.ecommerce.service.AuditLogService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class OrderController {
    private final OrderService orderService;
    private final AuditLogService auditLogService;

    public OrderController(OrderService orderService, AuditLogService auditLogService) {
        this.orderService = orderService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/orders")
    public OrderDtos.OrderResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                          @Valid @RequestBody OrderDtos.CreateRequest request) {
        var order = orderService.create(user.id(), request.idempotencyKey());
        auditLogService.record(user.id(), "CREATE", "ORDER", order.getId().toString(), "SUCCESS",
            "total=" + order.getTotal());
        return response(order);
    }

    @GetMapping("/orders")
    public OrderPage history(@AuthenticationPrincipal AuthenticatedUser user,
                             @PageableDefault(size = 20) Pageable pageable) {
        var page = orderService.history(user.id(), pageable);
        return new OrderPage(page.getContent().stream().map(this::response).toList(),
            page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @GetMapping("/orders/{id}")
    public OrderDtos.OrderResponse get(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable UUID id) {
        return response(orderService.getOwned(user.id(), id));
    }

    @GetMapping("/admin/orders")
    public AdminOrderPage listAll(@RequestParam(required = false) OrderStatus status,
                                  @PageableDefault(size = 20) Pageable pageable) {
        var page = orderService.listAll(status, pageable);
        return new AdminOrderPage(page.getContent().stream().map(this::adminResponse).toList(),
            page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @PatchMapping("/admin/orders/{id}/status")
    public OrderDtos.AdminOrderResponse updateStatus(@PathVariable UUID id,
                                                     @Valid @RequestBody OrderDtos.StatusUpdateRequest request,
                                                     @AuthenticationPrincipal AuthenticatedUser user) {
        var order = orderService.updateStatus(id, request.status());
        auditLogService.record(user.id(), "UPDATE_STATUS", "ORDER", id.toString(), "SUCCESS", "status=" + request.status());
        return adminResponse(order);
    }

    private OrderDtos.OrderResponse response(Order order) {
        return new OrderDtos.OrderResponse(order.getId(), order.getStatus().name(), order.getTotal(),
            order.getItems().stream().map(item -> new OrderDtos.OrderItemResponse(
                item.getProductNameSnapshot(), item.getUnitPriceSnapshot(), item.getQuantity(),
                item.getLineTotal())).toList());
    }

    private OrderDtos.AdminOrderResponse adminResponse(Order order) {
        return new OrderDtos.AdminOrderResponse(order.getId(), order.getCustomer().getId(),
            order.getCustomer().getEmail(), order.getCustomer().getDisplayName(),
            order.getStatus().name(), order.getTotal(), order.getCreatedAt(),
            order.getItems().stream().map(item -> new OrderDtos.OrderItemResponse(
                item.getProductNameSnapshot(), item.getUnitPriceSnapshot(), item.getQuantity(),
                item.getLineTotal())).toList());
    }

    public record OrderPage(java.util.List<OrderDtos.OrderResponse> items, int page, int size,
                            long totalItems, int totalPages) {}

    public record AdminOrderPage(java.util.List<OrderDtos.AdminOrderResponse> items, int page, int size,
                                 long totalItems, int totalPages) {}
}
