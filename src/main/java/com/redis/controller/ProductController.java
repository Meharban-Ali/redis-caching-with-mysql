// ─────────────────────────────────────────────────────────────
// ProductController.java — Production Ready
// ─────────────────────────────────────────────────────────────
package com.redis.controller;

import com.redis.model.dto.request.ProductRequest;
import com.redis.model.dto.response.ApiResponse;
import com.redis.model.dto.response.ProductResponse;
import com.redis.service.ProductService;
import com.redis.util.RedisUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    // FIX #2: Implementation nahi — Interface inject karo (SOLID - DIP)
    private final ProductService productService;

    // FIX #3: RedisTemplate nahi — RedisUtil inject karo (SOLID - SRP)
    private final RedisUtil redisUtil;

    // ═══════════════════════════════════════════════════════════════════════════
    //  CREATE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Naya product create karta hai.
     * @Valid — Request body pe validation trigger hoti hai
     * 201 Created — naya resource bana toh 200 nahi 201 hona chahiye
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {

        log.info("POST /api/products — creating product: {}", request.getName());

        ProductResponse created = productService.createProduct(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)                    // 201
                .body(ApiResponse.success("Product created successfully", created));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  READ — Single
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * ID se product fetch karta hai.
     * Cache check — pehle Redis mein dhundho, phir DB
     * fromCache flag — client ko pata chale source kahan se hai
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @PathVariable Long id) {

        log.info("GET /api/products/{} — fetching product", id);

        // FIX #8: Cache check RedisUtil se — Controller clean rakho
        String cacheKey = "product::" + id;
        boolean inCache = redisUtil.exists(cacheKey);

        ProductResponse product = productService.getProductById(id);

        String message = inCache
                ? "Product fetched from Redis cache"
                : "Product fetched from database";

        return ResponseEntity.ok(ApiResponse.success(message, product, inCache));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  READ — All (Paginated)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Saare products paginated return karta hai.
     * FIX #4: Pageable add kiya — unbounded list nahi
     *
     * Usage: GET /api/products?page=0&size=10&sort=price,asc
     *
     * @param page  — page number (0-based, default 0)
     * @param size  — items per page (default 10)
     * @param sort  — sort field (default "id")
     * @param dir   — sort direction asc/desc (default "asc")
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "10")   int size,
            @RequestParam(defaultValue = "id")   String sort,
            @RequestParam(defaultValue = "asc")  String dir) {

        log.info("GET /api/products — page: {}, size: {}", page, size);

        // Sort direction — asc ya desc
        Sort.Direction direction = dir.equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        // FIX #8: Cache check
        boolean inCache = redisUtil.exists(
                "products::page_" + page + "_size_" + size);

        Page<ProductResponse> products = productService.getAllProducts(pageable);

        String message = inCache
                ? "Products fetched from Redis cache"
                : "Products fetched from database";

        return ResponseEntity.ok(ApiResponse.success(message, products, inCache));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  UPDATE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Product update karta hai.
     * @CachePut — update ke baad Redis cache bhi refresh hoti hai
     * Duplicate name check Service mein hota hai
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        log.info("PUT /api/products/{} — updating product", id);

        ProductResponse updated = productService.updateProduct(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Product updated and cache refreshed", updated));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  DELETE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Product delete karta hai.
     * @CacheEvict — delete ke baad Redis cache evict hoti hai
     * FIX #10: ApiResponse.success(message) — null nahi pass karte
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        log.info("DELETE /api/products/{} — deleting product", id);

        productService.deleteProduct(id);

        return ResponseEntity.ok(
                ApiResponse.success("Product deleted and cache evicted"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SEARCH
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Naam se search — case-insensitive, paginated
     * Usage: GET /api/products/search?name=laptop&page=0&size=10
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> searchProducts(
            @RequestParam String name,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /api/products/search — name: {}", name);

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> products =
                productService.searchProductsByName(name, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Search results for: " + name, products));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PRICE RANGE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Price range se products filter karta hai.
     * FIX #7: min/max → minPrice/maxPrice — Service ke saath consistent
     * Usage: GET /api/products/price-range?minPrice=1000&maxPrice=50000
     */
    @GetMapping("/price-range")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /api/products/price-range — min: {}, max: {}", minPrice, maxPrice);

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> products =
                productService.getProductsByPriceRange(minPrice, maxPrice, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Products in price range", products));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  RATING & STOCK — FIX #11: Missing endpoints add kiye
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Minimum rating se products filter karta hai.
     * Usage: GET /api/products/rating?minRating=4.0
     */
    @GetMapping("/rating")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getByMinRating(
            @RequestParam BigDecimal minRating) {

        log.info("GET /api/products/rating — minRating: {}", minRating);

        List<ProductResponse> products =
                productService.getProductsByMinRating(minRating);

        return ResponseEntity.ok(
                ApiResponse.success("Products with min rating: " + minRating, products));
    }

    /**
     * Low stock products — threshold ke neeche.
     * Usage: GET /api/products/low-stock?threshold=10
     */
    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getLowStock(
            @RequestParam(defaultValue = "10") int threshold) {

        log.info("GET /api/products/low-stock — threshold: {}", threshold);

        List<ProductResponse> products =
                productService.getLowStockProducts(threshold);

        return ResponseEntity.ok(
                ApiResponse.success("Low stock products (threshold: " + threshold + ")", products));
    }

    /**
     * Out of stock products — totalProduct = 0.
     * Usage: GET /api/products/out-of-stock
     */
    @GetMapping("/out-of-stock")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getOutOfStock() {
        log.info("GET /api/products/out-of-stock");

        List<ProductResponse> products = productService.getOutOfStockProducts();

        return ResponseEntity.ok(
                ApiResponse.success("Out of stock products", products));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CACHE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Manually saari product cache clear karta hai.
     * FIX #5: boolean result check kiya — success/fail alag response
     * Admin use case — normal users ke liye nahi
     */
    @DeleteMapping("/cache")
    public ResponseEntity<ApiResponse<Void>> clearCache() {
        log.warn("DELETE /api/products/cache — manual cache clear requested");

        // FIX #5: boolean return check karo
        boolean cleared = productService.clearProductCache();

        if (!cleared) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            "Failed to clear cache",
                            "CACHE_CLEAR_FAILED"
                    ));
        }

        return ResponseEntity.ok(ApiResponse.success("All product caches cleared"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CACHE DEMO — Learning/Debug purpose only
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * FIX #6: redisTemplate.keys() HATA DIYA — production mein Redis block karta hai
     *
     * Cache demo endpoint — sirf ek product ka cache status dikhata hai
     * Redis hit/miss demonstrate karta hai
     * Usage: GET /api/products/cache-demo/1
     */
    @GetMapping("/cache-demo/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> cacheDemoEndpoint(
            @PathVariable Long id) {

        log.info("GET /api/products/cache-demo/{}", id);

        String cacheKey       = "product::" + id;
        boolean beforeFetch   = redisUtil.exists(cacheKey);  // FIX #3: RedisUtil use karo
        long    startTime     = System.currentTimeMillis();

        ProductResponse product = productService.getProductById(id);

        long    responseTime  = System.currentTimeMillis() - startTime;
        boolean afterFetch    = redisUtil.exists(cacheKey);

        // Cache status message
        String cacheStatus = beforeFetch
                ? "CACHE HIT — Redis se aaya, DB hit nahi hua"
                : "CACHE MISS — DB se aaya, ab Redis mein store ho gaya";

        log.info("Cache demo — id: {}, hit: {}, time: {}ms",
                id, beforeFetch, responseTime, afterFetch);

        return ResponseEntity.ok(
                ApiResponse.success(cacheStatus, product, afterFetch));
    }
}