package com.eshoppingzone.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsDto {
    private long totalUsers;
    private long totalMerchants;
    private long totalDeliveryPartners;
    private long totalProducts;
    private long totalOrders;
    private double totalRevenue;
    private long pendingOrders;
    private long deliveredOrders;
}
