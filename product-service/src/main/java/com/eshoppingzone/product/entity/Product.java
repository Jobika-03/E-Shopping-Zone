package com.eshoppingzone.product.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int productId;

    private String productType; // Electronics, Books, Apparel, Personal-Care
    private String productName;
    private String category;
    private double price;
    private String description;
    private Integer merchantId;
    private String sellerName;

    private int stock = 50;
    private int lowStockThreshold = 5;
    private double discountPercent = 0.0;
    private String brand = "Generic";
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, OUT_OF_STOCK
    private String variants = "Standard";

    // Offers System: Merchant Small Offers & Admin Mega/Big Billion Sale
    private String offerTitle;           // e.g., "Merchant Deal: 10% OFF" or "Big Billion Days Sale"
    private Double offerDiscountPercent = 0.0;
    private Double offerPrice = 0.0;
    private String offerType = "NONE";   // "NONE", "MERCHANT_OFFER", "ADMIN_SALE"
    private Boolean offerActive = false;
    private String offerDescription;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_reports", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "report_text")
    private List<String> reports = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    private List<String> image = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_ratings", joinColumns = @JoinColumn(name = "product_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "rating")
    private Map<Integer, Double> rating = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_reviews", joinColumns = @JoinColumn(name = "product_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "review_text")
    private Map<Integer, String> review = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_specifications", joinColumns = @JoinColumn(name = "product_id"))
    @MapKeyColumn(name = "spec_key")
    @Column(name = "spec_value")
    private Map<String, String> specification = new HashMap<>();
}
