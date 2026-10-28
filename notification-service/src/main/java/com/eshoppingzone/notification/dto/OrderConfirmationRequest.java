package com.eshoppingzone.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmationRequest {
    private int orderId;
    private String toEmail;
    private String customerName;
    private double totalAmount;
    private String paymentMode;
    private String orderStatus;
    private String shippingAddress;
    private List<OrderItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDto {
        private String productName;
        private int quantity;
        private double price;
    }
}
