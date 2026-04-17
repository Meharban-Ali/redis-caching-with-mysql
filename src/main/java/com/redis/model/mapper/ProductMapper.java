// ─────────────────────────────────────────────────────────────
// ProductMapper.java
// ─────────────────────────────────────────────────────────────
package com.redis.model.mapper;

import com.redis.model.dto.request.ProductRequest;
import com.redis.model.dto.response.ProductResponse;
import com.redis.model.entity.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class ProductMapper {

    // ─── Constants ─────────────────────────────────────────────────────────────
    private static final int  LOW_STOCK_THRESHOLD = 10;
    private static final Locale INDIA_LOCALE      = new Locale("en", "IN");

    // ═══════════════════════════════════════════════════════════════════════════
    //  RequestDTO → Entity
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Create ke liye — fresh Entity banata hai
     */
    public Product toEntity(ProductRequest dto) {
        return Product.builder()
                .name(sanitizeName(dto.getName()))
                .price(roundPrice(dto.getPrice()))
                .rating(roundRating(dto.getRating()))
                .stockQuantity(dto.getStockQuantity())
                .build();
    }

    /**
     * Update ke liye — existing Entity fields update karta hai
     * (id, version, createdAt unchanged rehte hain)
     */
    public void updateEntity(Product product, ProductRequest dto) {
        product.setName(sanitizeName(dto.getName()));
        product.setPrice(roundPrice(dto.getPrice()));
        product.setRating(roundRating(dto.getRating()));
        product.setStockQuantity(dto.getStockQuantity());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Entity → ResponseDTO
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Single Entity → ResponseDTO
     */
    public ProductResponse toResponse(Product product) {
        int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .rating(product.getRating())
                .stockQuantity(stock)
                .stockStatus(resolveStockStatus(stock))
                .priceFormatted(formatPrice(product.getPrice()))
                .isAvailable(stock > 0)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    /**
     * List<Entity> → List<ResponseDTO>  — Service/Controller mein seedha use karo
     */
    public List<ProductResponse> toResponseList(List<Product> products) {
        return products.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Private Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    /** Extra spaces aur special chars clean karta hai */
    private String sanitizeName(String name) {
        if (name == null) return null;
        return name.trim().replaceAll("\\s{2,}", " ");  // multiple spaces → single
    }

    /** Price always 2 decimal places mein store hogi */
    private BigDecimal roundPrice(BigDecimal price) {
        if (price == null) return null;
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    /** Rating always 1 decimal place mein store hogi */
    private BigDecimal roundRating(BigDecimal rating) {
        if (rating == null) return null;
        return rating.setScale(1, RoundingMode.HALF_UP);
    }

    /** Stock quantity se readable status derive karta hai */
    private String resolveStockStatus(int stock) {
        if (stock == 0)                  return "OUT_OF_STOCK";
        if (stock <= LOW_STOCK_THRESHOLD) return "LOW_STOCK";
        return                                   "IN_STOCK";
    }

    /** Price ko Indian Rupee format mein convert karta hai — "₹ 1,999.00" */
    private String formatPrice(BigDecimal price) {
        if (price == null) return null;
        NumberFormat formatter = NumberFormat.getCurrencyInstance(INDIA_LOCALE);
        formatter.setCurrency(Currency.getInstance("INR"));
        return formatter.format(price);
    }
}