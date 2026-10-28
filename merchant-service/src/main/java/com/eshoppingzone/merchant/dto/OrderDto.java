package com.eshoppingzone.merchant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderDto {
    private int orderId;
    private LocalDate orderDate;
    private int customerId;
    private double ammountPaid;
    private String modeOfPayment;
    private String orderStatus;
    private int quantity;
    private Integer merchantId;
    private String sellerName;
    private Integer deliveryPartnerId;
    private String deliveryPartnerName;
    private String deliveryPartnerPhone;
    private String deliveryAssignedDate;
    private Map<String, Object> address;
    private Map<String, Object> product;
    private String customerName;
    private String returnReason;
    private String deliveryVehicleNumber;
    private String estimatedDeliveryTime;
}
