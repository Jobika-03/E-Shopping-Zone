package com.eshoppingzone.orders.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_addresses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer addressId;

    private Integer customerId;
    private String fullName;
    private String mobileNumber;
    private Integer flatNumber;
    private String streetName;
    private String city;
    private Integer pincode;
    private String state;
}
