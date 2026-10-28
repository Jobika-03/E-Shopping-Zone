package com.eshoppingzone.product.service;

import com.eshoppingzone.product.entity.Product;
import com.eshoppingzone.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Override
    public void addProduct(Product product) {
        productRepository.save(product);
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Optional<Product> getProductById(int productId) {
        return productRepository.findById(productId);
    }

    @Override
    public Optional<Product> getProductByName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            return Optional.empty();
        }
        Optional<Product> direct = productRepository.findByProductName(productName);
        if (direct.isPresent()) {
            return direct;
        }
        String clean = productName.trim();
        String normalized = clean.replace("+", " ").replaceAll("\\s+", " ").trim();
        return productRepository.findAll().stream()
                .filter(p -> p.getProductName() != null &&
                        (p.getProductName().equalsIgnoreCase(clean)
                                || p.getProductName().equalsIgnoreCase(normalized)
                                || p.getProductName().trim().equalsIgnoreCase(normalized)))
                .findFirst();
    }

    @Override
    public Product updateProducts(Product product) {
        return productRepository.save(product);
    }

    @Override
    public void deleteProductById(int productId) {
        productRepository.deleteById(productId);
    }

    @Override
    public List<Product> getProductByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return productRepository.findAll();
        }
        String cleanCategory = category.trim();
        List<Product> list = productRepository.findByCategoryIgnoreCase(cleanCategory);
        if (list.isEmpty()) {
            list = productRepository.findByProductTypeIgnoreCase(cleanCategory);
        }
        if (list.isEmpty() && cleanCategory.contains("-")) {
            list = productRepository.findByCategoryIgnoreCase(cleanCategory.replace("-", " "));
        }
        if (list.isEmpty() && cleanCategory.contains(" ")) {
            list = productRepository.findByCategoryIgnoreCase(cleanCategory.replace(" ", "-"));
        }
        return list;
    }

    @Override
    public List<Product> getProductByType(String productType) {
        if (productType == null || productType.trim().isEmpty()) {
            return productRepository.findAll();
        }
        String cleanType = productType.trim();
        List<Product> list = productRepository.findByProductTypeIgnoreCase(cleanType);
        if (list.isEmpty()) {
            list = productRepository.findByCategoryIgnoreCase(cleanType);
        }
        return list;
    }

    @Override
    public List<Product> getProductsByMerchantId(int merchantId) {
        return productRepository.findByMerchantId(merchantId);
    }

    @Override
    public Product addReview(int productId, int userId, double rating, String reviewText) {
        return productRepository.findById(productId).map(product -> {
            if (product.getRating() == null) product.setRating(new java.util.HashMap<>());
            if (product.getReview() == null) product.setReview(new java.util.HashMap<>());
            if (rating > 0) {
                product.getRating().put(userId, rating);
            }
            if (reviewText != null && !reviewText.trim().isEmpty()) {
                product.getReview().put(userId, reviewText.trim());
            }
            return productRepository.save(product);
        }).orElse(null);
    }

    @Override
    public Product reportProduct(int productId, String reportReason) {
        return productRepository.findById(productId).map(product -> {
            if (product.getReports() == null) product.setReports(new java.util.ArrayList<>());
            if (reportReason != null && !reportReason.trim().isEmpty()) {
                product.getReports().add(reportReason.trim());
            }
            return productRepository.save(product);
        }).orElse(null);
    }

    @Override
    public Product updateStock(int productId, int quantityChange) {
        return productRepository.findById(productId).map(product -> {
            int newStock = Math.max(0, product.getStock() + quantityChange);
            product.setStock(newStock);
            if (newStock == 0) {
                product.setStatus("OUT_OF_STOCK");
            } else if ("OUT_OF_STOCK".equalsIgnoreCase(product.getStatus())) {
                product.setStatus("ACTIVE");
            }
            return productRepository.save(product);
        }).orElse(null);
    }

    @Override
    public Product setStock(int productId, int stock) {
        return productRepository.findById(productId).map(product -> {
            product.setStock(Math.max(0, stock));
            if (stock == 0) {
                product.setStatus("OUT_OF_STOCK");
            } else if ("OUT_OF_STOCK".equalsIgnoreCase(product.getStatus())) {
                product.setStatus("ACTIVE");
            }
            return productRepository.save(product);
        }).orElse(null);
    }

    @Override
    public Product updateStatus(int productId, String status) {
        return productRepository.findById(productId).map(product -> {
            if (status != null && !status.trim().isEmpty()) {
                product.setStatus(status.trim().toUpperCase());
            }
            return productRepository.save(product);
        }).orElse(null);
    }

    @Override
    public List<Product> getLowStockProducts(int threshold) {
        int th = threshold > 0 ? threshold : 5;
        return productRepository.findAll().stream()
                .filter(p -> p.getStock() <= th)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<Product> filterProducts(String category, Double minPrice, Double maxPrice, String brand, Boolean inStock, String sortBy) {
        return productRepository.findAll().stream()
                .filter(p -> {
                    if (category != null && !category.equalsIgnoreCase("ALL") && !category.trim().isEmpty()) {
                        boolean matchCat = p.getCategory() != null && p.getCategory().equalsIgnoreCase(category.trim());
                        boolean matchType = p.getProductType() != null && p.getProductType().equalsIgnoreCase(category.trim());
                        if (!matchCat && !matchType) return false;
                    }
                    if (minPrice != null && p.getPrice() < minPrice) return false;
                    if (maxPrice != null && p.getPrice() > maxPrice) return false;
                    if (brand != null && !brand.trim().isEmpty()) {
                        if (p.getBrand() == null || !p.getBrand().equalsIgnoreCase(brand.trim())) return false;
                    }
                    if (Boolean.TRUE.equals(inStock)) {
                        if (p.getStock() <= 0 || "OUT_OF_STOCK".equalsIgnoreCase(p.getStatus())) return false;
                    }
                    return true;
                })
                .sorted((p1, p2) -> {
                    if ("price_asc".equalsIgnoreCase(sortBy)) {
                        return Double.compare(p1.getPrice(), p2.getPrice());
                    } else if ("price_desc".equalsIgnoreCase(sortBy)) {
                        return Double.compare(p2.getPrice(), p1.getPrice());
                    } else if ("name_asc".equalsIgnoreCase(sortBy)) {
                        return (p1.getProductName() != null ? p1.getProductName() : "").compareToIgnoreCase(p2.getProductName() != null ? p2.getProductName() : "");
                    } else if ("name_desc".equalsIgnoreCase(sortBy)) {
                        return (p2.getProductName() != null ? p2.getProductName() : "").compareToIgnoreCase(p1.getProductName() != null ? p1.getProductName() : "");
                    }
                    return Integer.compare(p2.getProductId(), p1.getProductId()); // default newest first
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Product setMerchantOffer(int productId, String offerTitle, double discountPercent, String description) {
        return productRepository.findById(productId).map(product -> {
            double pct = Math.max(0.0, Math.min(90.0, discountPercent));
            product.setOfferTitle(offerTitle != null && !offerTitle.trim().isEmpty() ? offerTitle.trim() : "Special Offer");
            product.setOfferDiscountPercent(pct);
            double discPrice = Math.round((product.getPrice() * (1.0 - (pct / 100.0))) * 100.0) / 100.0;
            product.setOfferPrice(discPrice);
            product.setOfferType("MERCHANT_OFFER");
            product.setOfferActive(pct > 0);
            product.setOfferDescription(description);
            return productRepository.save(product);
        }).orElse(null);
    }

    @Override
    public Product removeProductOffer(int productId) {
        return productRepository.findById(productId).map(product -> {
            product.setOfferTitle(null);
            product.setOfferDiscountPercent(0.0);
            product.setOfferPrice(product.getPrice());
            product.setOfferType("NONE");
            product.setOfferActive(false);
            product.setOfferDescription(null);
            return productRepository.save(product);
        }).orElse(null);
    }

    // Mega Sale Global Tracker
    private static volatile String activeMegaSaleTitle = null;
    private static volatile double activeMegaSalePercent = 0.0;
    private static volatile String activeMegaSaleCategory = null;
    private static volatile boolean isMegaSaleActive = false;

    @Override
    public int applyAdminMegaSale(String saleTitle, double discountPercent, String category) {
        double pct = Math.max(1.0, Math.min(90.0, discountPercent));
        String title = (saleTitle != null && !saleTitle.trim().isEmpty()) ? saleTitle.trim() : "Big Billion Days Sale";
        String cat = (category != null && !category.trim().equalsIgnoreCase("ALL") && !category.trim().isEmpty()) ? category.trim() : null;

        activeMegaSaleTitle = title;
        activeMegaSalePercent = pct;
        activeMegaSaleCategory = cat;
        isMegaSaleActive = true;

        List<Product> products = productRepository.findAll();
        int count = 0;
        for (Product p : products) {
            if (cat == null || (p.getCategory() != null && p.getCategory().equalsIgnoreCase(cat)) || (p.getProductType() != null && p.getProductType().equalsIgnoreCase(cat))) {
                // If product already has a higher merchant discount, keep it; otherwise apply mega sale
                double currentPct = (p.getOfferActive() != null && p.getOfferActive() && p.getOfferDiscountPercent() != null) ? p.getOfferDiscountPercent() : 0.0;
                if (pct >= currentPct) {
                    p.setOfferTitle(title);
                    p.setOfferDiscountPercent(pct);
                    double discPrice = Math.round((p.getPrice() * (1.0 - (pct / 100.0))) * 100.0) / 100.0;
                    p.setOfferPrice(discPrice);
                    p.setOfferType("ADMIN_SALE");
                    p.setOfferActive(true);
                    p.setOfferDescription("Admin Mega Sale: " + title + " (" + (int)pct + "% OFF)");
                    productRepository.save(p);
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public int endAdminMegaSale() {
        isMegaSaleActive = false;
        activeMegaSaleTitle = null;
        activeMegaSalePercent = 0.0;
        activeMegaSaleCategory = null;

        List<Product> products = productRepository.findAll();
        int count = 0;
        for (Product p : products) {
            if ("ADMIN_SALE".equalsIgnoreCase(p.getOfferType())) {
                p.setOfferTitle(null);
                p.setOfferDiscountPercent(0.0);
                p.setOfferPrice(p.getPrice());
                p.setOfferType("NONE");
                p.setOfferActive(false);
                p.setOfferDescription(null);
                productRepository.save(p);
                count++;
            }
        }
        return count;
    }

    @Override
    public java.util.Map<String, Object> getActiveMegaSale() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("active", isMegaSaleActive);
        map.put("title", activeMegaSaleTitle);
        map.put("discountPercent", activeMegaSalePercent);
        map.put("category", activeMegaSaleCategory);
        return map;
    }
}

