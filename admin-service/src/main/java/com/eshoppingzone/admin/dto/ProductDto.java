package com.eshoppingzone.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private int productId;
    private String productType;
    private String productName;
    private String category;
    private Map<Double, String> rating;
    private Map<Double, String> review;
    private List<String> image;
    private double price;
    private String description;
    private Map<String, String> specification;
    private Integer merchantId;
    private String sellerName;
    @Builder.Default
    private int stock = 50;
    @Builder.Default
    private String status = "ACTIVE";
    @Builder.Default
    private String brand = "Generic";
    @Builder.Default
    private double discountPercent = 0.0;
    private String variants;
    private List<String> reports;

    // Offers System
    private String offerTitle;
    private Double offerDiscountPercent;
    private Double offerPrice;
    private String offerType;
    private Boolean offerActive;
    private String offerDescription;
}

