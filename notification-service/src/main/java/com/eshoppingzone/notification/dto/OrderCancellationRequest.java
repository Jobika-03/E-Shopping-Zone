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
public class OrderCancellationRequest {
    private int orderId;
    private String toEmail;
    private String customerName;
    private double totalAmount;
    private double refundAmount;
    private String paymentMode;
    private String orderStatus; // CANCELLED
    private String refundStatus; // e.g. "Refunded to E-Wallet Balance" or "Refund Processed"
    private String cancellationReason; // e.g. "Cancelled by customer"
    private String cancellationDate;
    private String shippingAddress;
    private String productName;
    private int quantity;
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
