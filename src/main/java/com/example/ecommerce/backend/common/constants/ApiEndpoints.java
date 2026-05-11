package com.example.ecommerce.backend.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiEndpoints {
    private static final String API_VERSION = "/api/v1";

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Category {
        public static final String BASE_CATEGORY = API_VERSION + "/categories";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Product {
        public static final String BASE_PRODUCT = API_VERSION + "/products";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Inventory {
        public static final String BASE_INVENTORY = API_VERSION + "/inventory";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Cart {
        public static final String BASE_CART = API_VERSION + "/cart";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Order {
        public static final String BASE_ORDER = API_VERSION + "/orders";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Payment {
        public static final String BASE_PAYMENT = API_VERSION + "/payments";
    }
}
