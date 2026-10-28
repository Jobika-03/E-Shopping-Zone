package com.eshoppingzone.product.config;

import com.eshoppingzone.product.entity.Product;
import com.eshoppingzone.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            List<Product> products = new ArrayList<>();

            // 1. COSMETICS
            products.add(createProduct(
                    "Matte Velvet Lipstick (Ruby Red)",
                    "Cosmetics",
                    "Cosmetics",
                    199.99,
                    "Long-lasting velvety smooth matte lipstick with intense color payoff and hydrating vitamin E.",
                    List.of("https://images.unsplash.com/photo-1586495777744-4413f21062fa?auto=format&fit=crop&w=600&q=80"),
                    Map.of(1, 4.8, 2, 5.0),
                    Map.of(1, "Stays on all day without drying my lips!", 2, "Gorgeous shade, perfect for parties."),
                    Map.of("Shade", "Ruby Red", "Finish", "Matte", "Waterproof", "Yes")
            ));

            products.add(createProduct(
                    "Hydrating Hyaluronic Glow Serum",
                    "Cosmetics",
                    "Cosmetics",
                    280.50,
                    "Lightweight multi-depth hydration serum infused with pure hyaluronic acid for plump, radiant skin.",
                    List.of("https://images.unsplash.com/photo-1620916566398-39f1143ab7be?auto=format&fit=crop&w=600&q=80"),
                    Map.of(1, 4.9),
                    Map.of(1, "My skin feels so smooth and glowing after using this!"),
                    Map.of("Volume", "30ml", "Skin Type", "All Skin Types", "Cruelty-Free", "Yes")
            ));

            products.add(createProduct(
                    "Waterproof Volume & Curl Mascara",
                    "Cosmetics",
                    "Cosmetics",
                    169.00,
                    "Smudge-proof volumizing mascara providing dramatic lash lift and 24-hour curl definition.",
                    List.of("https://images.unsplash.com/photo-1591360236480-4ed861025fa1?auto=format&fit=crop&w=600&q=80"),
                    Map.of(1, 4.7),
                    Map.of(1, "No clumps and withstands rain and sweat!"),
                    Map.of("Color", "Jet Black", "Wear Duration", "24 Hours", "Waterproof", "Yes")
            ));

            products.add(createProduct(
                    "Illuminating Nude Eyeshadow Palette",
                    "Cosmetics",
                    "Cosmetics",
                    349.99,
                    "12 ultra-pigmented matte and shimmer shades ranging from soft creams to rich golden bronzes.",
                    List.of("https://images.unsplash.com/photo-1512496015851-a90fb38ba796?auto=format&fit=crop&w=600&q=80"),
                    Map.of(1, 4.8),
                    Map.of(1, "Blends like a dream and colors are so versatile."),
                    Map.of("Shades", "12 Colors", "Texture", "Powder", "Mirror Included", "Yes")
            ));

            // 2. ELECTRONICS
            products.add(createProduct(
                    "Wireless ANC Noise-Cancelling Headphones",
                    "Electronics",
                    "Electronics",
                    1499.99,
                    "Premium over-ear Bluetooth headphones with active noise cancellation, 40h battery, and spatial audio.",
                    List.of("https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80"),
                    Map.of(1, 5.0, 2, 4.8),
                    Map.of(1, "Incredible sound quality and ANC blocks out all airplane noise.", 2, "Very comfortable cushioning."),
                    Map.of("Connectivity", "Bluetooth 5.3", "Battery Life", "40 Hours", "ANC", "Hybrid ANC")
            ));

            products.add(createProduct(
                    "Smart Fitness Watch with GPS & AMOLED",
                    "Electronics",
                    "Electronics",
                    899.99,
                    "Water-resistant smartwatch with vivid AMOLED display, heart rate, SpO2 monitoring, and 7-day battery.",
                    List.of("https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80"),
                    Map.of(1, 4.6),
                    Map.of(1, "Battery lasts over a week easily. Tracking is very accurate."),
                    Map.of("Display", "1.4 inch AMOLED", "Water Resistance", "5ATM (50m)", "Battery", "7 Days")
            ));

            products.add(createProduct(
                    "RGB Mechanical Gaming Keyboard",
                    "Electronics",
                    "Electronics",
                    749.50,
                    "Tenkeyless mechanical gaming keyboard with customizable per-key RGB backlighting and tactile brown switches.",
                    List.of("https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=600&q=80"),
                    Map.of(1, 4.9),
                    Map.of(1, "Satisfying tactile click without being too loud."),
                    Map.of("Switch Type", "Mechanical Brown", "Layout", "87-Key TKL", "Connection", "Detachable USB-C")
            ));

            products.add(createProduct(
                    "Ultra-Portable Waterproof Bluetooth Speaker",
                    "Electronics",
                    "Electronics",
                    499.99,
                    "Rugged 360-degree wireless speaker with punchy bass, IPX7 waterproof rating, and 12-hour playtime.",
                    List.of("https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?auto=format&fit=crop&w=600&q=80"),
                    Map.of(1, 4.7),
                    Map.of(1, "Surprising volume and bass for such a compact size."),
                    Map.of("Power", "20W RMS", "Waterproof", "IPX7", "Playtime", "12 Hours")
            ));

            // 3. BOOKS
            products.add(createProduct(
                    "Clean Code: A Handbook of Agile Software Craftsmanship",
                    "Books",
                    "Books",
                    389.00,
                    "Legendary guide by Robert C. Martin on best coding practices, refactoring, and crafting maintainable software.",
                    List.of("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=600&q=80"),
                    Map.of(1, 5.0),
                    Map.of(1, "A must-read for every professional software engineer."),
                    Map.of("Author", "Robert C. Martin", "Format", "Paperback", "Pages", "464")
            ));

            products.add(createProduct(
                    "The Pragmatic Programmer: 20th Anniversary Edition",
                    "Books",
                    "Books",
                    429.50,
                    "Classic developer handbook exploring modern best practices from core architectural decisions to career growth.",
                    List.of("https://images.unsplash.com/photo-1532012164546-f432f2e3777a?auto=format&fit=crop&w=600&q=80"),
                    Map.of(1, 4.9),
                    Map.of(1, "Timeless advice that changed how I approach coding."),
                    Map.of("Authors", "David Thomas, Andrew Hunt", "Format", "Hardcover", "Pages", "352")
            ));

            products.add(createProduct(
                    "Atomic Habits: Build Good Habits & Break Bad Ones",
                    "Books",
                    "Books",
                    220.00,
                    "World-renowned bestseller by James Clear demonstrating how tiny daily habits yield extraordinary life results.",
                    List.of("https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=600&q=80"),
                    Map.of(1, 5.0),
                    Map.of(1, "Actionable, scientific, and genuinely life changing."),
                    Map.of("Author", "James Clear", "Format", "Paperback", "Pages", "320")
            ));

            products.add(createProduct(
                    "Spring Boot in Action",
                    "Books",
                    "Books",
                    359.99,
                    "Comprehensive hands-on guide for quickly developing production-grade microservices and web APIs.",
                    List.of("https://images.unsplash.com/photo-1589829085413-56de8ae18c73?auto=format&fit=crop&w=600&q=80"),
                    Map.of(1, 4.8),
                    Map.of(1, "Great explanations for enterprise Spring Boot concepts."),
                    Map.of("Author", "Craig Walls", "Format", "Paperback", "Pages", "264")
            ));

            // 4. APPAREL
            products.add(createProduct(
                    "Classic Vintage Denim Jacket",
                    "Apparel",
                    "Apparel",
                    659.00,
                    "100% heavy-duty organic denim trucker jacket with button-front closure and dual chest flap pockets.",
                    List.of("https://images.unsplash.com/photo-1576995853123-5a10305d93c0?auto=format&fit=crop&w=600&q=80"),
                    Map.of(1, 4.7),
                    Map.of(1, "Great fit, durable stitching, and timeless look."),
                    Map.of("Material", "100% Organic Cotton Denim", "Fit", "Regular Fit", "Closure", "Button Front")
            ));

            products.add(createProduct(
                    "Premium Organic Cotton Crewneck T-Shirt",
                    "Apparel",
                    "Apparel",
                    249.99,
                    "Ultra-soft combed ring-spun cotton tee with breathable fabric, tailored fit, and reinforced seams.",
                    List.of("https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=600&q=80"),
                    Map.of(1, 4.8),
                    Map.of(1, "Super comfortable fabric that holds its shape after many washes."),
                    Map.of("Material", "100% Combed Cotton", "Neckline", "Crew Neck", "Sizes", "S, M, L, XL")
            ));

            products.add(createProduct(
                    "Urban Streetwear Heavyweight Fleece Hoodie",
                    "Apparel",
                    "Apparel",
                    499.00,
                    "Cozy heavyweight fleece hoodie with roomy kangaroo pocket, adjustable drawstring hood, and ribbed trims.",
                    List.of("https://images.unsplash.com/photo-1556905055-8f358a7a47b2?auto=format&fit=crop&w=600&q=80"),
                    Map.of(1, 4.9),
                    Map.of(1, "Warm, thick fabric with a modern streetwear fit."),
                    Map.of("Material", "80% Cotton / 20% Polyester", "Fit", "Relaxed", "Unisex", "Yes")
            ));

            products.add(createProduct(
                    "Breathable Lightweight Cushion Running Shoes",
                    "Apparel",
                    "Apparel",
                    799.95,
                    "High-performance athletic sneakers featuring responsive EVA cushioning and breathable engineered mesh.",
                    List.of("https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=600&q=80"),
                    Map.of(1, 4.8),
                    Map.of(1, "Extremely lightweight and provides great arch support during runs."),
                    Map.of("Sole Material", "EVA Cushioning", "Upper", "Breathable Mesh", "Closure", "Lace-Up")
            ));

            // 5. PERSONAL-CARE
            products.add(createProduct(
                    "Sonic Electric Toothbrush with Travel Case",
                    "Personal-Care",
                    "Personal-Care",
                    459.00,
                    "Advanced sonic toothbrush delivering 40,000 vibrations per minute, 5 cleaning modes, and 30-day battery life.",
                    List.of("https://images.unsplash.com/photo-1559591937-e10211d1cfdf?auto=format&fit=crop&w=600&q=80"),
                    Map.of(1, 4.9),
                    Map.of(1, "Leaves teeth feeling dentist-clean. Battery lasts a whole month!"),
                    Map.of("Modes", "5 Cleaning Modes", "VPM", "40,000", "Waterproof", "IPX7")
            ));

            products.add(createProduct(
                    "Organic Beard Grooming & Conditioning Kit",
                    "Personal-Care",
                    "Personal-Care",
                    299.99,
                    "Complete beard care kit with cedarwood beard oil, conditioning balm, sandalwood comb, and boar bristle brush.",
                    List.of("https://images.unsplash.com/photo-1621607512214-68297480165e?auto=format&fit=crop&w=600&q=80"),
                    Map.of(1, 4.8),
                    Map.of(1, "Keeps beard soft and non-itchy. Nice subtle cedarwood scent."),
                    Map.of("Includes", "Oil, Balm, Comb, Brush, Scissors", "Scent", "Cedarwood", "Organic", "Yes")
            ));

            products.add(createProduct(
                    "Ultrasonic Aromatherapy Essential Oil Diffuser",
                    "Personal-Care",
                    "Personal-Care",
                    329.50,
                    "Whisper-quiet 500ml ultrasonic cool mist diffuser with 7 ambient LED colors, timer, and auto shut-off.",
                    List.of("https://images.unsplash.com/photo-1608571423902-eed4a5ad8108?auto=format&fit=crop&w=600&q=80"),
                    Map.of(1, 4.7),
                    Map.of(1, "Fills the entire living room with fragrance and soothing light."),
                    Map.of("Capacity", "500ml", "LED Modes", "7 Colors", "Auto Shut-Off", "Yes")
            ));

            products.add(createProduct(
                    "Nourishing Shea & Cocoa Whipped Body Butter",
                    "Personal-Care",
                    "Personal-Care",
                    189.50,
                    "Rich whipped body moisturizer formulated with 100% raw African shea butter, cocoa butter, and sweet almond oil.",
                    List.of("https://images.unsplash.com/photo-1608248597359-53e3cb88a6d7?auto=format&fit=crop&w=600&q=80"),
                    Map.of(1, 4.9),
                    Map.of(1, "Absorbs quickly and keeps skin hydrated all day long."),
                    Map.of("Net Weight", "250g", "Skin Type", "Dry / Normal", "Paraben-Free", "Yes")
            ));

            productRepository.saveAll(products);
            System.out.println(">>> Seeded " + products.size() + " products successfully across all categories into eshopping_product_db!");
        } else {
            // Ensure existing products in database are not left with 0 stock or null status
            List<Product> existing = productRepository.findAll();
            boolean updatedAny = false;
            for (Product p : existing) {
                boolean changed = false;
                if (p.getStock() <= 0 && !"OUT_OF_STOCK".equalsIgnoreCase(p.getStatus())) {
                    p.setStock(50);
                    changed = true;
                }
                if (p.getStatus() == null || p.getStatus().trim().isEmpty()) {
                    p.setStatus("ACTIVE");
                    changed = true;
                }
                if (p.getBrand() == null || p.getBrand().trim().isEmpty()) {
                    p.setBrand("Premium Collection");
                    changed = true;
                }
                if (changed) {
                    productRepository.save(p);
                    updatedAny = true;
                }
            }
            if (updatedAny) {
                System.out.println(">>> Initialized stock and active status for existing products in eshopping_product_db.");
            }
        }
    }

    private Product createProduct(String name, String type, String category, double price, String desc,
                                  List<String> images, Map<Integer, Double> ratings,
                                  Map<Integer, String> reviews, Map<String, String> specs) {
        Product p = new Product();
        p.setProductName(name);
        p.setProductType(type);
        p.setCategory(category);
        p.setPrice(price);
        p.setDescription(desc);
        p.setImage(new ArrayList<>(images));
        p.setRating(new HashMap<>(ratings));
        p.setReview(new HashMap<>(reviews));
        p.setSpecification(new HashMap<>(specs));
        p.setStock(50);
        p.setStatus("ACTIVE");
        p.setBrand("Premium Collection");
        p.setDiscountPercent(10.0);
        return p;
    }
}
