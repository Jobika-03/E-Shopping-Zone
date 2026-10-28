package com.eshoppingzone.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAssignmentRequest {
    private int orderId;
    private int partnerId;
    private String partnerName;
    private String partnerEmail;
    private String partnerPhone;
    private String merchantName;
    private int customerId;
    private String customerName;
    private String customerPhone;
    private String shippingAddress;
    private String productName;
    private int quantity;
    private double totalAmount;
    private String paymentMode;
    private String assignmentDate;
    private String notes;
}