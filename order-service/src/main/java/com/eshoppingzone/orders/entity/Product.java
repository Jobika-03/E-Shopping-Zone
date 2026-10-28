package com.eshoppingzone.orders.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString
public class Product {
    private String productId;
    private String productName;
}
