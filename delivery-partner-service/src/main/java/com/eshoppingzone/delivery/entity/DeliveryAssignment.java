package com.eshoppingzone.delivery.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_assignments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString
public class DeliveryAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int assignmentId;

    private int orderId;
    private int partnerId;
    private String partnerName;
    private String partnerPhone;
    private String partnerEmail;

    private Integer merchantId;
    private String merchantName;

    private int customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    @Column(length = 1000)
    private String shippingAddress;

    private String productName;
    private int quantity;
    private double totalAmount;
    private String paymentMode;

    private String status; // ASSIGNED, SHIPPED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED

    private LocalDateTime assignedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime outForDeliveryAt;
    private LocalDateTime deliveredAt;

    private String remarks;
    private String estimatedDeliveryTime;

    private String assignmentType; // DELIVERY or RETURN_PICKUP
    private String returnReason;
    private Double returnLatitude;
    private Double returnLongitude;
    @Column(length = 1000)
    private String returnLocationMapUrl;
}