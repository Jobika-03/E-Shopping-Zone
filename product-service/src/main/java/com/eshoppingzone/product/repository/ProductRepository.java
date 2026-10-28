package com.eshoppingzone.product.repository;

import com.eshoppingzone.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    Optional<Product> findByProductName(String productName);
    List<Product> findByCategory(String category);
    List<Product> findByCategoryIgnoreCase(String category);
    List<Product> findByProductType(String productType);
    List<Product> findByProductTypeIgnoreCase(String productType);
    List<Product> findByMerchantId(Integer merchantId);
}
