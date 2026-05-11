package com.example.ecommerce.backend.order.service.impl;

import com.example.ecommerce.backend.cart.entity.Cart;
import com.example.ecommerce.backend.cart.entity.CartItem;
import com.example.ecommerce.backend.cart.repository.CartRepository;
import com.example.ecommerce.backend.cart.service.CartService;
import com.example.ecommerce.backend.common.exception.ResourceConflictException;
import com.example.ecommerce.backend.inventory.entity.Inventory;
import com.example.ecommerce.backend.inventory.repository.InventoryRepository;
import com.example.ecommerce.backend.order.dto.request.CreateOrderRequest;
import com.example.ecommerce.backend.order.dto.response.OrderCheckoutResponse;
import com.example.ecommerce.backend.order.dto.response.OrderResponse;
import com.example.ecommerce.backend.order.entity.Order;
import com.example.ecommerce.backend.order.entity.OrderItem;
import com.example.ecommerce.backend.order.entity.OrderStatus;
import com.example.ecommerce.backend.order.mapper.OrderMapper;
import com.example.ecommerce.backend.order.repository.OrderRepository;
import com.example.ecommerce.backend.order.service.OrderCancellationService;
import com.example.ecommerce.backend.order.service.OrderService;
import com.example.ecommerce.backend.payment.dto.response.PaymentResponse;
import com.example.ecommerce.backend.payment.service.PaymentService;
import com.example.ecommerce.backend.product.entity.Product;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Implementation of {@link OrderService} for checkout and cancellation flows.
 *
 * <p>Business logic stays in the service layer: cart validation, inventory
 * reservation/release, immutable item snapshot creation, order persistence,
 * cart clearing, and checkout payment initiation are coordinated in one
 * transaction.</p>
 *
 * @author Pial Kanti Samadder
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private static final Map<String, double[]> COUPONS = Map.of(
            "DESH10", new double[]{0.10, 100.0},
            "NINE11", new double[]{0.11, 200.0},
            "MIND20", new double[]{0.20, 1000.0}
    );

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final InventoryRepository inventoryRepository;
    private final CartService cartService;
    private final OrderMapper orderMapper;
    private final PaymentService paymentService;
    private final OrderCancellationService orderCancellationService;

    @Override
    @Transactional
    public OrderCheckoutResponse placeOrder(Long userId, CreateOrderRequest request) {
        log.info("Starting checkout for userId={}, cartId={}", userId, request.cartId());

        Cart cart = cartRepository.findById(request.cartId())
                .orElseThrow(() -> new EntityNotFoundException("Cart not found: " + request.cartId()));

        validateCartOwnership(cart, userId);
        validateCartHasItems(cart);

        Order order = Order.builder()
                .orderNumber(UUID.randomUUID())
                .userId(userId)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(0.0)
                .createdBy(userId)
                .modifiedBy(userId)
                .build();

        cart.getItems().forEach(cartItem -> {
            Product product = cartItem.getProduct();
            validateActiveProduct(product);
            reserveInventory(product.getId(), cartItem.getQuantity());
            order.getItems().add(toOrderItem(order, cartItem));
        });

        double total = calculateTotalAmount(order);
        total = applyDiscount(total, request.couponCode());
        order.setTotalAmount(total);

        Order savedOrder = orderRepository.saveAndFlush(order);
        cartService.clearCart(userId, cart.getId());
        PaymentResponse paymentResponse = paymentService.initiatePayment(savedOrder.getId());

        log.info("Checkout completed for userId={}, cartId={}, orderId={}", userId, cart.getId(), savedOrder.getId());
        return new OrderCheckoutResponse(orderMapper.toResponse(savedOrder), paymentResponse);
    }

    private double applyDiscount(double total, String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            return total;
        }
        double[] coupon = COUPONS.get(couponCode.toUpperCase());
        if (coupon == null) {
            throw new ResourceConflictException(
                    "Invalid coupon code: '" + couponCode + "'. Valid codes are: DESH10, NINE11, MIND20.");
        }
        double saving = Math.min(total * coupon[0], coupon[1]);
        log.info("Coupon '{}' applied: saving={}, finalTotal={}", couponCode, saving, total - saving);
        return total - saving;
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        log.info("Starting order cancellation for userId={}, orderId={}", userId, orderId);

        Order order = findOrderWithItems(orderId);
        validateOrderOwnership(order, userId);

        orderCancellationService.cancelConfirmedOrder(order, userId);

        Order savedOrder = orderRepository.saveAndFlush(order);
        log.info("Order cancellation completed for userId={}, orderId={}", userId, orderId);
        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long userId, Long orderId) {
        Order order = findOrderWithItems(orderId);
        validateOrderOwnership(order, userId);
        return orderMapper.toResponse(order);
    }

    private Order findOrderWithItems(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
    }

    private void validateCartOwnership(Cart cart, Long userId) {
        if (!cart.getUserId().equals(userId)) {
            log.warn("Cart ownership validation failed for userId={}, cartId={}", userId, cart.getId());
            throw new ResourceConflictException("Cart does not belong to current user: " + cart.getId());
        }
    }

    private void validateOrderOwnership(Order order, Long userId) {
        if (!order.getUserId().equals(userId)) {
            log.warn("Order ownership validation failed for userId={}, orderId={}", userId, order.getId());
            throw new ResourceConflictException("Order does not belong to current user: " + order.getId());
        }
    }

    private void validateCartHasItems(Cart cart) {
        if (cart.getItems().isEmpty()) {
            log.warn("Checkout rejected because cart is empty. cartId={}", cart.getId());
            throw new ResourceConflictException("Cannot checkout an empty cart.");
        }
    }

    private void validateActiveProduct(Product product) {
        if (!Boolean.TRUE.equals(product.getIsActive())) {
            log.warn("Checkout rejected because product is inactive. productId={}", product.getId());
            throw new ResourceConflictException("Inactive product cannot be ordered: " + product.getId());
        }
    }

    private void reserveInventory(Long productId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found for product: " + productId));

        int availableQuantity = inventory.getTotalQuantity() - inventory.getReservedQuantity();
        if (quantity > availableQuantity) {
            log.warn("Insufficient inventory for productId={}, requested={}, available={}",
                    productId, quantity, availableQuantity);
            throw new ResourceConflictException("Insufficient available inventory for product: " + productId);
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventoryRepository.saveAndFlush(inventory);
    }

    private OrderItem toOrderItem(Order order, CartItem cartItem) {
        Product product = cartItem.getProduct();
        Double totalPrice = cartItem.getUnitPrice() * cartItem.getQuantity();

        return OrderItem.builder()
                .order(order)
                .productId(product.getId())
                .productName(product.getName())
                .unitPrice(cartItem.getUnitPrice())
                .quantity(cartItem.getQuantity())
                .totalPrice(totalPrice)
                .build();
    }

    private Double calculateTotalAmount(Order order) {
        return order.getItems()
                .stream()
                .mapToDouble(OrderItem::getTotalPrice)
                .sum();
    }
}
