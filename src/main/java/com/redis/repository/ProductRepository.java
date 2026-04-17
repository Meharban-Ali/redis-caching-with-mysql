package com.redis.repository;

import com.redis.model.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ═══════════════════════════════════════════════════════════════════════════
    //  SEARCH QUERIES
    // ═══════════════════════════════════════════════════════════════════════════

    // Naam se search — case-insensitive, paginated
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Exact name match — duplicate check ke liye
    Optional<Product> findByNameIgnoreCase(String name);

    // Price range filter — sorted by price ascending
    Page<Product> findByPriceBetweenOrderByPriceAsc(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    );

    // ═══════════════════════════════════════════════════════════════════════════
    //  RATING QUERIES
    // ═══════════════════════════════════════════════════════════════════════════

    // Exact rating se products
    List<Product> findByRating(BigDecimal rating);

    // Minimum rating se products — descending order
    @Query("SELECT p FROM Product p WHERE p.rating >= :minRating ORDER BY p.rating DESC")
    List<Product> findProductsByMinRating(@Param("minRating") BigDecimal minRating);

    // ═══════════════════════════════════════════════════════════════════════════
    //  STOCK QUERIES
    // ═══════════════════════════════════════════════════════════════════════════

    // In-stock products jo price limit ke andar hon
    @Query("SELECT p FROM Product p WHERE p.price <= :maxPrice " +
           "AND p.stockQuantity > 0 " +
           "ORDER BY p.price ASC")
    List<Product> findAffordableProductsInStock(@Param("maxPrice") BigDecimal maxPrice);

    // FIX #1: ORDER BY p.totalProduct → p.stockQuantity
    @Query("SELECT p FROM Product p WHERE p.stockQuantity > 0 " +
           "AND p.stockQuantity <= :threshold " +
           "ORDER BY p.stockQuantity ASC")      // ← FIX: totalProduct → stockQuantity
    List<Product> findLowStockProducts(@Param("threshold") int threshold);

    // FIX #2: findBy hata diya — Spring Data "findBy" ko field naam samajhta hai
    @Query("SELECT p FROM Product p WHERE p.stockQuantity = 0")
    List<Product> findOutOfStockProducts();     // ← FIX: findBy → find

    // ═══════════════════════════════════════════════════════════════════════════
    //  UTILITY QUERIES
    // ═══════════════════════════════════════════════════════════════════════════

    // FIX #3: SELECT COUNT(p) > 0 invalid JPQL tha
    // COUNT result Java mein check karo
    @Query("SELECT COUNT(p) FROM Product p " +
           "WHERE LOWER(p.name) = LOWER(:name) " +
           "AND p.id <> :excludeId")
    long countByNameIgnoreCaseAndIdNot(        // ← FIX: boolean → long return type
            @Param("name") String name,
            @Param("excludeId") Long excludeId
    );

    // Bulk stock update — loop se behtar performance
    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = :stock WHERE p.id = :id")
    int updateStock(@Param("id") Long id, @Param("stock") int stock);
}