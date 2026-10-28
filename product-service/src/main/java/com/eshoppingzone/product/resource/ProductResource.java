package com.eshoppingzone.product.resource;

import com.eshoppingzone.product.entity.Product;
import com.eshoppingzone.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductResource {

    @Autowired
    private ProductService productService;

    @PostMapping
    public ResponseEntity<Product> addProducts(@RequestBody Product product) {
        productService.addProduct(product);
        return ResponseEntity.ok(product);
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable("id") int id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<Product> getProductByName(@PathVariable("name") String name) {
        return productService.getProductByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable("category") String category) {
        return ResponseEntity.ok(productService.getProductByCategory(category));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Product>> getProductByType(@PathVariable("type") String type) {
        return ResponseEntity.ok(productService.getProductByType(type));
    }

    @PutMapping
    public ResponseEntity<Product> updateProducts(@RequestBody Product product) {
        return ResponseEntity.ok(productService.updateProducts(product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable("id") int id) {
        productService.deleteProductById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<Product>> getProductsByMerchantId(@PathVariable("merchantId") int merchantId) {
        return ResponseEntity.ok(productService.getProductsByMerchantId(merchantId));
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<Product> addReview(@PathVariable("id") int id,
                                             @RequestParam("userId") int userId,
                                             @RequestParam("rating") double rating,
                                             @RequestParam(value = "review", required = false) String review) {
        Product updated = productService.addReview(id, userId, rating, review);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<Product> reportProduct(@PathVariable("id") int id,
                                                 @RequestParam("reason") String reason) {
        Product updated = productService.reportProduct(id, reason);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<Product> setStock(@PathVariable("id") int id, @RequestParam("stock") int stock) {
        Product updated = productService.setStock(id, stock);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/stock/deduct")
    public ResponseEntity<Product> deductStock(@PathVariable("id") int id, @RequestParam("quantity") int quantity) {
        Product updated = productService.updateStock(id, -Math.abs(quantity));
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/stock/restore")
    public ResponseEntity<Product> restoreStock(@PathVariable("id") int id, @RequestParam("quantity") int quantity) {
        Product updated = productService.updateStock(id, Math.abs(quantity));
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Product> updateStatus(@PathVariable("id") int id, @RequestParam("status") String status) {
        Product updated = productService.updateStatus(id, status);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<Product>> getLowStock(@RequestParam(value = "threshold", defaultValue = "5") int threshold) {
        return ResponseEntity.ok(productService.getLowStockProducts(threshold));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Product>> filterProducts(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "brand", required = false) String brand,
            @RequestParam(value = "inStock", required = false) Boolean inStock,
            @RequestParam(value = "sortBy", required = false, defaultValue = "newest") String sortBy) {
        return ResponseEntity.ok(productService.filterProducts(category, minPrice, maxPrice, brand, inStock, sortBy));
    }

    // --- Offers Endpoints (Merchant Small Offers & Admin Mega Sale) ---

    @PostMapping("/{id}/merchant-offer")
    public ResponseEntity<Product> setMerchantOffer(@PathVariable("id") int id,
                                                    @RequestParam("offerTitle") String offerTitle,
                                                    @RequestParam("discountPercent") double discountPercent,
                                                    @RequestParam(value = "description", required = false) String description) {
        Product updated = productService.setMerchantOffer(id, offerTitle, discountPercent, description);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}/offer")
    public ResponseEntity<Product> removeProductOffer(@PathVariable("id") int id) {
        Product updated = productService.removeProductOffer(id);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @PostMapping("/admin-mega-sale")
    public ResponseEntity<java.util.Map<String, Object>> launchAdminMegaSale(
            @RequestParam(value = "saleTitle", defaultValue = "Big Billion Days Sale") String saleTitle,
            @RequestParam(value = "discountPercent", defaultValue = "20.0") double discountPercent,
            @RequestParam(value = "category", required = false) String category) {
        int affected = productService.applyAdminMegaSale(saleTitle, discountPercent, category);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("success", true);
        resp.put("affectedProducts", affected);
        resp.put("saleTitle", saleTitle);
        resp.put("discountPercent", discountPercent);
        resp.put("category", category);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/end-mega-sale")
    public ResponseEntity<java.util.Map<String, Object>> endAdminMegaSale() {
        int restored = productService.endAdminMegaSale();
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("success", true);
        resp.put("restoredProducts", restored);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/active-mega-sale")
    public ResponseEntity<java.util.Map<String, Object>> getActiveMegaSale() {
        return ResponseEntity.ok(productService.getActiveMegaSale());
    }
}
