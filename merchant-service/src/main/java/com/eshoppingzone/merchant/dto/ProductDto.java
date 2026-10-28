package com.eshoppingzone.merchant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDto {
    private int productId;
    private String productType;
    private String productName;
    private String category;
    private double price;
    private String description;
    private Integer merchantId;
    private String sellerName;
    private List<String> image;
    private Map<Integer, Double> rating;
    private Map<Integer, String> review;
    private Map<String, String> specification;
    private int stock = 50;
    private int lowStockThreshold = 5;
    private double discountPercent = 0.0;
    private String brand = "Generic";
    private String status = "ACTIVE";
    private String variants = "Standard";
    private List<String> reports;

    // Offers System
    private String offerTitle;
    private Double offerDiscountPercent = 0.0;
    private Double offerPrice = 0.0;
    private String offerType = "NONE";
    private Boolean offerActive = false;
    private String offerDescription;
}

