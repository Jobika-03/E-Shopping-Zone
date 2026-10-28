package com.eshoppingzone.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {
    private int totalProducts;
    private int totalOrders;
    private double totalRevenue;
    private int pendingOrders;
    private List<ProductDto> recentProducts;
    private List<OrderDto> recentOrders;
}
