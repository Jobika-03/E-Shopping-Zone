package com.eshoppingzone.merchant.resource;

import com.eshoppingzone.merchant.dto.DashboardSummaryDto;
import com.eshoppingzone.merchant.dto.MerchantAuthDto;
import com.eshoppingzone.merchant.dto.OrderDto;
import com.eshoppingzone.merchant.dto.ProductDto;
import com.eshoppingzone.merchant.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/merchant")
@CrossOrigin(origins = "*")
public class MerchantResource {

    @Autowired
    private MerchantService merchantService;

    // 1. Merchant Authentication
    @PostMapping("/auth/login")
    public ResponseEntity<MerchantAuthDto.LoginResponse> login(@RequestBody MerchantAuthDto.LoginRequest request) {
        MerchantAuthDto.LoginResponse response = merchantService.login(request);
        if (!response.isSuccess()) {
            return ResponseEntity.status(403).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/auth/social-login")
    public ResponseEntity<MerchantAuthDto.LoginResponse> socialLogin(@RequestBody java.util.Map<String, String> body) {
        String provider = body.getOrDefault("provider", "GOOGLE");
        String email = body.get("email");
        String name = body.get("name");
        MerchantAuthDto.LoginResponse response = merchantService.socialLogin(provider, email, name);
        if (!response.isSuccess()) {
            return ResponseEntity.status(400).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/auth/register")
    public ResponseEntity<MerchantAuthDto.LoginResponse> register(@RequestBody MerchantAuthDto.RegisterRequest request) {
        MerchantAuthDto.LoginResponse response = merchantService.register(request);
        if (!response.isSuccess()) {
            return ResponseEntity.status(400).body(response);
        }
        return ResponseEntity.ok(response);
    }

    // 2. Dashboard Analytics
    @GetMapping("/{merchantId}/dashboard")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary(@PathVariable("merchantId") int merchantId) {
        return ResponseEntity.ok(merchantService.getDashboardSummary(merchantId));
    }

    // 3. Merchant Product Management
    @GetMapping("/{merchantId}/products")
    public ResponseEntity<List<ProductDto>> getMerchantProducts(@PathVariable("merchantId") int merchantId) {
        return ResponseEntity.ok(merchantService.getMerchantProducts(merchantId));
    }

    @PostMapping("/products")
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto product) {
        return ResponseEntity.ok(merchantService.createProduct(product));
    }

    @PutMapping("/products")
    public ResponseEntity<ProductDto> updateProduct(@RequestBody ProductDto product) {
        return ResponseEntity.ok(merchantService.updateProduct(product));
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable("productId") int productId) {
        merchantService.deleteProduct(productId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/products/{productId}/offer")
    public ResponseEntity<ProductDto> setProductOffer(@PathVariable("productId") int productId,
                                                      @RequestParam("offerTitle") String offerTitle,
                                                      @RequestParam("discountPercent") double discountPercent,
                                                      @RequestParam(value = "description", required = false) String description) {
        ProductDto p = merchantService.setProductOffer(productId, offerTitle, discountPercent, description);
        return p != null ? ResponseEntity.ok(p) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/products/{productId}/offer")
    public ResponseEntity<Void> removeProductOffer(@PathVariable("productId") int productId) {
        merchantService.removeProductOffer(productId);
        return ResponseEntity.ok().build();
    }


    // 4. Merchant Customer Orders & Fulfillment
    @GetMapping("/orders")
    public ResponseEntity<List<OrderDto>> getAllOrders(@RequestParam(value = "merchantId", required = false) Integer merchantId) {
        if (merchantId != null && merchantId > 0) {
            return ResponseEntity.ok(merchantService.getMerchantOrders(merchantId));
        }
        return ResponseEntity.ok(merchantService.getAllOrders());
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable("orderId") int orderId) {
        OrderDto order = merchantService.getOrderById(orderId);
        if (order != null) {
            return ResponseEntity.ok(order);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{merchantId}/orders")
    public ResponseEntity<List<OrderDto>> getMerchantOrders(@PathVariable("merchantId") int merchantId) {
        return ResponseEntity.ok(merchantService.getMerchantOrders(merchantId));
    }

    @RequestMapping(value = "/orders/{orderId}/status", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<Void> updateOrderStatus(@PathVariable("orderId") int orderId,
                                                  @RequestParam(value = "status", required = false) String statusParam,
                                                  @RequestBody(required = false) java.util.Map<String, String> body) {
        String status = statusParam;
        if ((status == null || status.trim().isEmpty()) && body != null && body.containsKey("status")) {
            status = body.get("status");
        }
        if (status != null && !status.trim().isEmpty()) {
            merchantService.updateOrderStatus(orderId, status);
        }
        return ResponseEntity.ok().build();
    }

    // 5. Delivery Partner Assignment Workflow
    @GetMapping("/delivery-partners")
    public ResponseEntity<List<Object>> getDeliveryPartners() {
        return ResponseEntity.ok(merchantService.getDeliveryPartners());
    }

    @PostMapping("/orders/{orderId}/assign-delivery")
    public ResponseEntity<Object> assignDeliveryPartner(@PathVariable("orderId") int orderId,
                                                        @RequestParam(value = "partnerId", required = false, defaultValue = "0") int partnerId,
                                                        @RequestParam(value = "remarks", required = false) String remarks,
                                                        @RequestBody(required = false) java.util.Map<String, Object> body) {
        int targetPartnerId = partnerId;
        String targetRemarks = remarks;
        if (body != null) {
            if (body.get("partnerId") != null) targetPartnerId = ((Number) body.get("partnerId")).intValue();
            if (body.get("remarks") != null) targetRemarks = body.get("remarks").toString();
        }
        if (targetPartnerId <= 0) {
            targetPartnerId = 1; // default to first available partner
        }
        Object result = merchantService.assignDeliveryPartner(orderId, targetPartnerId, targetRemarks);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/products/{productId}/stock")
    public ResponseEntity<Void> updateStock(@PathVariable("productId") int productId, @RequestParam("stock") int stock) {
        merchantService.updateStock(productId, stock);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/products/{productId}/status")
    public ResponseEntity<Void> updateProductStatus(@PathVariable("productId") int productId, @RequestParam("status") String status) {
        merchantService.updateProductStatus(productId, status);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/orders/{orderId}/approve-return")
    public ResponseEntity<java.util.Map<String, Object>> approveReturn(
            @PathVariable("orderId") int orderId,
            @RequestParam(value = "partnerId", required = false) Integer partnerId,
            @RequestParam(value = "remarks", required = false) String remarks) {
        boolean ok = merchantService.approveReturn(orderId, partnerId, remarks);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("success", ok);
        resp.put("orderId", orderId);
        resp.put("status", "REFUNDED");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/orders/{orderId}/reject-return")
    public ResponseEntity<java.util.Map<String, Object>> rejectReturn(
            @PathVariable("orderId") int orderId,
            @RequestParam(value = "reason", required = false) String reason) {
        boolean ok = merchantService.rejectReturn(orderId, reason);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("success", ok);
        resp.put("orderId", orderId);
        resp.put("status", "RETURN_REJECTED");
        return ResponseEntity.ok(resp);
    }

    // 6. Merchant Wallet & Settlements
    @GetMapping("/{merchantId}/wallet")
    public ResponseEntity<java.util.Map<String, Object>> getMerchantWallet(@PathVariable("merchantId") int merchantId) {
        Double balance = merchantService.getMerchantWalletBalance(merchantId);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("merchantId", merchantId);
        resp.put("balance", balance);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{merchantId}/wallet/statements")
    public ResponseEntity<List<Object>> getMerchantWalletStatements(@PathVariable("merchantId") int merchantId) {
        return ResponseEntity.ok(merchantService.getMerchantWalletStatements(merchantId));
    }

    @PostMapping("/{merchantId}/wallet/deposit")
    public ResponseEntity<Object> depositMerchantWallet(@PathVariable("merchantId") int merchantId,
                                                        @RequestParam("amount") double amount,
                                                        @RequestParam(value = "remarks", required = false, defaultValue = "Merchant Settlement / Recharge") String remarks) {
        Object res = merchantService.depositMerchantWallet(merchantId, amount, remarks);
        return ResponseEntity.ok(res);
    }
}
