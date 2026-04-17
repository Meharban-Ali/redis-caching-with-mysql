// ─────────────────────────────────────────────────────────────
// ProductService.java
// ─────────────────────────────────────────────────────────────
package com.redis.service;

import com.redis.model.dto.request.ProductRequest;
import com.redis.model.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    // ═══════════════════════════════════════════════════════════════════════════
    //  CRUD OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * FIX #2: Request lena chahiye — Response dena chahiye
     * Duplicate name check bhi hoga internally
     */
    ProductResponse createProduct(ProductRequest request);

    /**
     * Single product — id se fetch
     * Cache: @Cacheable — Redis se milega agar exist kare
     */
    ProductResponse getProductById(Long id);

    /**
     * FIX #5: Pageable add kiya — unbounded list nahi
     * Cache: @Cacheable — page wise cache hoga
     */
    Page<ProductResponse> getAllProducts(Pageable pageable);

    /**
     * FIX #3: Request lena chahiye — Response dena chahiye
     * Cache: @CachePut — update ke baad cache refresh hoga
     */
    ProductResponse updateProduct(Long id, ProductRequest request);

    /**
     * Cache: @CacheEvict — delete ke baad cache clear hoga
     */
    void deleteProduct(Long id);

    // ═══════════════════════════════════════════════════════════════════════════
    //  SEARCH OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * FIX #6: Pageable add kiya
     * Name se search — case-insensitive
     */
    Page<ProductResponse> searchProductsByName(String name, Pageable pageable);

    /**
     * FIX #7: Pageable add kiya + minPrice/maxPrice validation service mein hogi
     * Price range ke andar products
     */
    Page<ProductResponse> getProductsByPriceRange(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    );

    /**
     * FIX #9: Repository mein tha — interface mein add kiya
     * Rating ke basis pe products
     */
    List<ProductResponse> getProductsByMinRating(BigDecimal minRating);

    /**
     * FIX #9: Low stock products — inventory management
     * threshold ke neeche stock wale products
     */
    List<ProductResponse> getLowStockProducts(int threshold);

    /**
     * Out of stock products
     */
    List<ProductResponse> getOutOfStockProducts();

    // ═══════════════════════════════════════════════════════════════════════════
    //  CACHE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * FIX #8: void → boolean — success/fail pata chalega
     * Manually cache clear karna ho toh — admin use case
     */
    boolean clearProductCache();
}