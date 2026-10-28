package com.eshoppingzone.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPartnerDto {
    private Integer partnerId;
    private String partnerName;
    private String email;
    private String phone;
    private String vehicleType;
    private String serviceZone;
    private String password;
    private String status;
    private double rating;
    private int totalDeliveries;
    private boolean active;
}
