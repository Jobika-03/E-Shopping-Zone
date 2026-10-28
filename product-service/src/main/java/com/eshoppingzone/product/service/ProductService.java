package com.eshoppingzone.product.service;

import com.eshoppingzone.product.entity.Product;
import java.util.List;
import java.util.Optional;

public interface ProductService {
    void addProduct(Product product);
    List<Product> getAllProducts();
    Optional<Product> getProductById(int productId);
    Optional<Product> getProductByName(String productName);
    Product updateProducts(Product product);
    void deleteProductById(int productId);
    List<Product> getProductByCategory(String category);
    List<Product> getProductByType(String productType);
    List<Product> getProductsByMerchantId(int merchantId);
    Product addReview(int productId, int userId, double rating, String reviewText);
    Product reportProduct(int productId, String reportReason);
    Product updateStock(int productId, int quantityChange);
    Product setStock(int productId, int stock);
    Product updateStatus(int productId, String status);
    List<Product> getLowStockProducts(int threshold);
    List<Product> filterProducts(String category, Double minPrice, Double maxPrice, String brand, Boolean inStock, String sortBy);

    // Offers System
    Product setMerchantOffer(int productId, String offerTitle, double discountPercent, String description);
    Product removeProductOffer(int productId);
    int applyAdminMegaSale(String saleTitle, double discountPercent, String category);
    int endAdminMegaSale();
    java.util.Map<String, Object> getActiveMegaSale();
}

