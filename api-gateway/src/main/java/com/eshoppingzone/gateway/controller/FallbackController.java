package com.eshoppingzone.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Graceful Circuit Breaker Fallback Controller for API Gateway.
 * Returns structured JSON responses when backend services are down or slow.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/default")
    public ResponseEntity<Map<String, Object>> defaultFallback() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SERVICE_UNAVAILABLE");
        resp.put("message", "Service is temporarily unavailable. Circuit breaker fallback triggered.");
        resp.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(resp);
    }

    @RequestMapping("/product")
    public ResponseEntity<Map<String, Object>> productFallback() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SERVICE_UNAVAILABLE");
        resp.put("service", "product-service");
        resp.put("message", "Product Catalog Service is taking longer than expected or currently down. Please try again shortly.");
        resp.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(resp);
    }

    @RequestMapping("/order")
    public ResponseEntity<Map<String, Object>> orderFallback() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SERVICE_UNAVAILABLE");
        resp.put("service", "order-service");
        resp.put("message", "Order Service is temporarily busy or unreachable. Your request has been protected by Circuit Breaker.");
        resp.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(resp);
    }

    @RequestMapping("/profile")
    public ResponseEntity<Map<String, Object>> profileFallback() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SERVICE_UNAVAILABLE");
        resp.put("service", "profile-service");
        resp.put("message", "User Profile / Authentication Service is temporarily unavailable. Please retry shortly.");
        resp.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(resp);
    }

    @RequestMapping("/cart")
    public ResponseEntity<Map<String, Object>> cartFallback() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SERVICE_UNAVAILABLE");
        resp.put("service", "cart-service");
        resp.put("message", "Shopping Cart Service is temporarily unavailable. Please retry shortly.");
        resp.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(resp);
    }

    @RequestMapping("/wallet")
    public ResponseEntity<Map<String, Object>> walletFallback() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SERVICE_UNAVAILABLE");
        resp.put("service", "wallet-service");
        resp.put("message", "E-Wallet Service is currently experiencing high load. Please try again in a few moments.");
        resp.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(resp);
    }
}