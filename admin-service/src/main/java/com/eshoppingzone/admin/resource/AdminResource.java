package com.eshoppingzone.admin.resource;

import com.eshoppingzone.admin.dto.*;
import com.eshoppingzone.admin.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class AdminResource {

    @Autowired
    private AdminService adminService;

    // 1. Auth
    @PostMapping("/auth/login")
    public ResponseEntity<AdminAuthDto.LoginResponse> login(@RequestBody AdminAuthDto.LoginRequest request) {
        AdminAuthDto.LoginResponse response = adminService.login(request);
        if (!response.isSuccess()) {
            return ResponseEntity.status(403).body(response);
        }
        return ResponseEntity.ok(response);
    }

    // 2. Stats
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> getStats() {
        return ResponseEntity.ok(adminService.getPlatformStats());
    }

    // 3. Users
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto user) {
        return ResponseEntity.ok(adminService.createUser(user));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") int id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    // 4. Merchants
    @GetMapping("/merchants")
    public ResponseEntity<List<UserDto>> getAllMerchants() {
        return ResponseEntity.ok(adminService.getAllMerchants());
    }

    @PostMapping("/merchants")
    public ResponseEntity<UserDto> createMerchant(@RequestBody UserDto merchant) {
        return ResponseEntity.ok(adminService.createMerchant(merchant));
    }

    @DeleteMapping("/merchants/{id}")
    public ResponseEntity<Void> deleteMerchant(@PathVariable("id") int id) {
        adminService.deleteMerchant(id);
        return ResponseEntity.ok().build();
    }

    // 5. Delivery Partners
    @GetMapping("/delivery-partners")
    public ResponseEntity<List<DeliveryPartnerDto>> getAllDeliveryPartners() {
        return ResponseEntity.ok(adminService.getAllDeliveryPartners());
    }

    @PostMapping("/delivery-partners")
    public ResponseEntity<DeliveryPartnerDto> createDeliveryPartner(@RequestBody DeliveryPartnerDto partner) {
        return ResponseEntity.ok(adminService.createDeliveryPartner(partner));
    }

    @DeleteMapping("/delivery-partners/{id}")
    public ResponseEntity<Void> deleteDeliveryPartner(@PathVariable("id") int id) {
        adminService.deleteDeliveryPartner(id);
        return ResponseEntity.ok().build();
    }

    // 6. Products
    @GetMapping("/products")
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        return ResponseEntity.ok(adminService.getAllProducts());
    }

    @PostMapping("/products")
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto product) {
        return ResponseEntity.ok(adminService.createProduct(product));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable("id") int id) {
        adminService.deleteProduct(id);
        return ResponseEntity.ok().build();
    }

    // 7. Orders
    @GetMapping("/orders")
    public ResponseEntity<List<OrderDto>> getAllOrders() {
        return ResponseEntity.ok(adminService.getAllOrders());
    }

    @RequestMapping(value = "/orders/{id}/status", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<Void> updateOrderStatus(@PathVariable("id") int orderId,
                                                  @RequestParam(value = "status", required = false) String statusParam,
                                                  @RequestBody(required = false) Map<String, String> body) {
        String status = statusParam;
        if ((status == null || status.trim().isEmpty()) && body != null && body.containsKey("status")) {
            status = body.get("status");
        }
        if (status != null && !status.trim().isEmpty()) {
            adminService.updateOrderStatus(orderId, status);
        }
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<Void> updateUserStatus(@PathVariable("id") int id, @RequestParam("status") String status) {
        adminService.updateUserStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/merchants/{id}/status")
    public ResponseEntity<Void> updateMerchantStatus(@PathVariable("id") int id, @RequestParam("status") String status) {
        adminService.updateMerchantStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/delivery-partners/{id}/status")
    public ResponseEntity<Void> updateDeliveryPartnerStatus(@PathVariable("id") int id, @RequestParam("status") String status) {
        adminService.updateDeliveryPartnerStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/reports/summary")
    public ResponseEntity<Map<String, Object>> getReportsSummary() {
        return ResponseEntity.ok(adminService.getReportsSummary());
    }

    // Mega Sale Endpoints
    @PostMapping("/mega-sale/launch")
    public ResponseEntity<Map<String, Object>> launchMegaSale(
            @RequestParam(value = "saleTitle", defaultValue = "Big Billion Days Sale") String saleTitle,
            @RequestParam(value = "discountPercent", defaultValue = "20.0") double discountPercent,
            @RequestParam(value = "category", required = false) String category) {
        return ResponseEntity.ok(adminService.launchMegaSale(saleTitle, discountPercent, category));
    }

    @PostMapping("/mega-sale/end")
    public ResponseEntity<Map<String, Object>> endMegaSale() {
        return ResponseEntity.ok(adminService.endMegaSale());
    }

    @GetMapping("/mega-sale/active")
    public ResponseEntity<Map<String, Object>> getActiveMegaSale() {
        return ResponseEntity.ok(adminService.getActiveMegaSale());
    }
}
