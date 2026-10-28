package com.eshoppingzone.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private Object address;
    private Object product;
}
