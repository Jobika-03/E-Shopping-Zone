package com.eshoppingzone.admin.service;

import com.eshoppingzone.admin.dto.*;
import java.util.List;

public interface AdminService {

    // Auth
    AdminAuthDto.LoginResponse login(AdminAuthDto.LoginRequest request);

    // Platform KPIs
    AdminStatsDto getPlatformStats();

    // User & Customer Management
    List<UserDto> getAllUsers();
    UserDto createUser(UserDto user);
    void deleteUser(int profileId);

    // Merchant Management
    List<UserDto> getAllMerchants();
    UserDto createMerchant(UserDto merchant);
    void deleteMerchant(int profileId);

    // Delivery Partner Management
    List<DeliveryPartnerDto> getAllDeliveryPartners();
    DeliveryPartnerDto createDeliveryPartner(DeliveryPartnerDto partner);
    void deleteDeliveryPartner(int partnerId);

    // Product Management
    List<ProductDto> getAllProducts();
    ProductDto createProduct(ProductDto product);
    void deleteProduct(int productId);

    // Order Management
    List<OrderDto> getAllOrders();
    void updateOrderStatus(int orderId, String status);

    // Status Moderation & Reports
    void updateUserStatus(int profileId, String status);
    void updateMerchantStatus(int profileId, String status);
    void updateDeliveryPartnerStatus(int partnerId, String status);
    java.util.Map<String, Object> getReportsSummary();

    // Mega Sale / Platform Offers
    java.util.Map<String, Object> launchMegaSale(String saleTitle, double discountPercent, String category);
    java.util.Map<String, Object> endMegaSale();
    java.util.Map<String, Object> getActiveMegaSale();
}

