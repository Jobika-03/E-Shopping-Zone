package com.eshoppingzone.delivery.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "delivery_partners")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString
public class DeliveryPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int partnerId;

    private String partnerName;
    private String email;
    private String phone;
    private String password;
    private String vehicleType;  // Bike, Van, Truck, Electric Scooter
    private String vehicleNumber;
    private String serviceZone;  // Metro Area, North Zone, South Zone, Central Hub
    private String status;       // AVAILABLE, ON_DUTY, BUSY
    private double rating;
    private int totalDeliveries;
}