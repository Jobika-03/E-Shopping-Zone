package com.eshoppingzone.orders.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int orderId;

    private LocalDate orderDate;
    private int customerId;
    private double ammountPaid;
    private String modeOfPayment; // WALLET, CASH_ON_DELIVERY
    private String orderStatus;    // PLACED, CONFIRMED, DELIVERED, CANCELLED
    private int quantity;
    private Integer merchantId;
    private String sellerName;

    private Integer deliveryPartnerId;
    private String deliveryPartnerName;
    private String deliveryPartnerPhone;
    private String deliveryAssignedDate;
    private String deliveryVehicleNumber;
    private String estimatedDeliveryTime;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "address_id")
    private Address address;

    @Embedded
    private Product product;

    private String returnReason;
    private String failureReason;
    private String deliveryOtp;
    private String customerName;

    // Return location map and review details
    private Double returnLatitude;
    private Double returnLongitude;
    @Column(length = 1000)
    private String returnLocationMapUrl;
    @Column(length = 1000)
    private String returnLocationAddress;
    private String returnRejectionReason;
}
