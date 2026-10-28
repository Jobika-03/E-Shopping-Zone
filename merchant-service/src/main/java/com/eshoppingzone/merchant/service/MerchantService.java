package com.eshoppingzone.merchant.service;

import com.eshoppingzone.merchant.dto.DashboardSummaryDto;
import com.eshoppingzone.merchant.dto.MerchantAuthDto;
import com.eshoppingzone.merchant.dto.OrderDto;
import com.eshoppingzone.merchant.dto.ProductDto;
import java.util.List;

public interface MerchantService {
    MerchantAuthDto.LoginResponse login(MerchantAuthDto.LoginRequest request);
    MerchantAuthDto.LoginResponse socialLogin(String provider, String email, String name);
    MerchantAuthDto.LoginResponse register(MerchantAuthDto.RegisterRequest request);
    DashboardSummaryDto getDashboardSummary(int merchantId);
    List<ProductDto> getMerchantProducts(int merchantId);
    ProductDto createProduct(ProductDto product);
    ProductDto updateProduct(ProductDto product);
    void deleteProduct(int productId);
    List<OrderDto> getMerchantOrders(int merchantId);
    List<OrderDto> getAllOrders();
    OrderDto getOrderById(int orderId);
    void updateOrderStatus(int orderId, String status);
    List<Object> getDeliveryPartners();
    Object assignDeliveryPartner(int orderId, int partnerId, String remarks);
    void updateStock(int productId, int stock);
    void updateProductStatus(int productId, String status);
    boolean approveReturn(int orderId);
    default boolean approveReturn(int orderId, Integer partnerId, String remarks) {
        return approveReturn(orderId);
    }
    boolean rejectReturn(int orderId);
    default boolean rejectReturn(int orderId, String reason) {
        return rejectReturn(orderId);
    }
    Double getMerchantWalletBalance(int merchantId);
    List<Object> getMerchantWalletStatements(int merchantId);
    Object depositMerchantWallet(int merchantId, double amount, String remarks);
    ProductDto setProductOffer(int productId, String offerTitle, double discountPercent, String description);
    void removeProductOffer(int productId);
}

