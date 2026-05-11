package com.example.ecommerce.backend.cart.service.impl;

import com.example.ecommerce.backend.cart.dto.request.CartItemAddRequest;
import com.example.ecommerce.backend.cart.dto.response.CartResponse;
import com.example.ecommerce.backend.cart.entity.Cart;
import com.example.ecommerce.backend.cart.entity.CartItem;
import com.example.ecommerce.backend.cart.mapper.CartMapper;
import com.example.ecommerce.backend.cart.repository.CartRepository;
import com.example.ecommerce.backend.cart.service.CartService;
import com.example.ecommerce.backend.common.exception.ResourceConflictException;
import com.example.ecommerce.backend.inventory.entity.Inventory;
import com.example.ecommerce.backend.inventory.repository.InventoryRepository;
import com.example.ecommerce.backend.product.dto.response.ProductResponse;
import com.example.ecommerce.backend.product.entity.Product;
import com.example.ecommerce.backend.product.mapper.ProductMapper;
import com.example.ecommerce.backend.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Implementation of {@link CartService} for managing user shopping carts.
 *
 * <p>Handles cart lookup, first-cart creation, product validation, inventory
 * availability checks, item quantity merging, persistence, and response
 * mapping. Inventory is checked before cart mutation, but it is not reserved by
 * cart operations.</p>
 *
 * @author Pial Kanti Samadder
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CartMapper cartMapper;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public CartResponse addItem(Long userId, CartItemAddRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + request.productId()));

        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new ResourceConflictException("Inactive product cannot be added to cart: " + request.productId());
        }

        Inventory inventory = inventoryRepository.findByProductId(request.productId())
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found for product: " + request.productId()));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createCart(userId));

        Optional<CartItem> existingItem = findItemByProductId(cart, request.productId());
        int requestedCartQuantity = request.quantity() + existingItem
                .map(CartItem::getQuantity)
                .orElse(0);

        validateAvailableQuantity(inventory, requestedCartQuantity);

        if (existingItem.isPresent()) {
            CartItem cartItem = existingItem.get();
            cartItem.setQuantity(requestedCartQuantity);
        } else {
            cart.getItems().add(CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.quantity())
                    .unitPrice(product.getPrice())
                    .build());
        }

        return cartMapper.toResponse(cartRepository.saveAndFlush(cart));
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(cartMapper::toResponse)
                .orElseGet(() -> cartMapper.toEmptyResponse(userId));
    }

    @Override
    @Transactional
    public void clearCart(Long userId, Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found: " + cartId));

        if (!cart.getUserId().equals(userId)) {
            throw new ResourceConflictException("Cart does not belong to current user: " + cartId);
        }

        cart.getItems().clear();
        cartRepository.saveAndFlush(cart);
    }

    private Cart createCart(Long userId) {
        return Cart.builder()
                .userId(userId)
                .createdBy(userId)
                .modifiedBy(userId)
                .build();
    }

    private Optional<CartItem> findItemByProductId(Cart cart, Long productId) {
        return cart.getItems()
                .stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();
    }

    private void validateAvailableQuantity(Inventory inventory, int requestedCartQuantity) {
        int availableQuantity = inventory.getTotalQuantity() - inventory.getReservedQuantity();
        if (requestedCartQuantity > availableQuantity) {
            throw new ResourceConflictException("Insufficient available inventory for product: "
                                                + inventory.getProduct().getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getCartSuggestions(Long userId) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Cart not found"));

        List<CartItem> cartItems = new ArrayList<>(cart.getItems());

        cartItems.sort(Comparator.comparingDouble(
                item -> item.getProduct().getPrice()
        ));

        List<ProductResponse> suggestions = new ArrayList<>();
        Set<Long> cartProductIds = new HashSet<>();

        for (CartItem item : cartItems) {
            cartProductIds.add(item.getProduct().getId());
        }
        for (CartItem item : cartItems) {
            if (suggestions.size() == 3) {
                break;
            }

            Product cartProduct = item.getProduct();
            List<Product> products =
                    productRepository.findByCategoryId(
                            cartProduct.getCategory().getId()
                    );

            Product bestMatch = null;
            int bestDistance = Integer.MAX_VALUE;

            for (Product candidateProduct : products) {
                if (candidateProduct.getId().equals(cartProduct.getId())) {
                    continue;
                }

                if (cartProductIds.contains(candidateProduct.getId())) {
                    continue;
                }
                if (Math.abs(candidateProduct.getPrice() - cartProduct.getPrice()) > 100) {
                    continue;
                }

                int distance = levenshteinDistance(
                        cartProduct.getName().toLowerCase(),
                        candidateProduct.getName().toLowerCase()
                );

                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestMatch = candidateProduct;
                }
            }
            if (bestMatch != null) {
                suggestions.add(productMapper.toResponse(bestMatch));
            }
        }
        return suggestions;
    }

    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1],
                            Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }

        return dp[a.length()][b.length()];
    }
}
