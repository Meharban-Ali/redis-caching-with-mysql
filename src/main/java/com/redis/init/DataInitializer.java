package com.redis.init;

import com.redis.model.entity.Product;
import com.redis.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final DataInitializerProperties properties;
    

    // // ─── Yahan add karo — @RequiredArgsConstructor ke baad ───────────────────
    // @Value("${app.data-initializer.enabled:true}")
    // private boolean enabled;
    // // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void run(String... args) {
        // ─── Yahan condition add karo ─────────────────────────────────────────
        if (!properties.isEnabled()) {
            log.info("DataInitializer is disabled — skipping data initialization");
            return;
        }
        // ─────────────────────────────────────────────────────────────────────
        log.info("Initializing sample product data...");
        try {
            initializeProducts();
        } catch (Exception ex) {
            log.error("Failed to initialize sample data: {}", ex.getMessage(), ex);
        }
    }

    private void initializeProducts() {
        long existingCount = productRepository.count();

        if (existingCount > 0) {
            log.info("Database already has {} products, skipping initialization",
                    existingCount);
            return;
        }

        List<Product> sampleProducts = buildSampleProducts();
        productRepository.saveAll(sampleProducts);
        log.info("{} sample products inserted into database successfully!",
                sampleProducts.size());
    }

    private List<Product> buildSampleProducts() {
        return List.of(
                Product.builder()
                        .name("Gaming Laptop - ASUS ROG")
                        .price(new BigDecimal("85000.00"))
                        .rating(new BigDecimal("4.5"))
                        .stockQuantity(50)
                        .version(0L)
                        .build(),
                Product.builder()
                        .name("iPhone 15 Pro Max")
                        .price(new BigDecimal("134900.00"))
                        .rating(new BigDecimal("4.8"))
                        .stockQuantity(200)
                        .version(0L)
                        .build(),
                Product.builder()
                        .name("Samsung Galaxy S24 Ultra")
                        .price(new BigDecimal("129999.00"))
                        .rating(new BigDecimal("4.7"))
                        .stockQuantity(150)
                        .version(0L)
                        .build(),
                Product.builder()
                        .name("Sony WH-1000XM5 Headphones")
                        .price(new BigDecimal("29990.00"))
                        .rating(new BigDecimal("4.6"))
                        .stockQuantity(100)
                        .version(0L)
                        .build(),
                Product.builder()
                        .name("Dell 27-inch 4K Monitor")
                        .price(new BigDecimal("45000.00"))
                        .rating(new BigDecimal("4.4"))
                        .stockQuantity(75)
                        .version(0L)
                        .build()
        );
    }
}