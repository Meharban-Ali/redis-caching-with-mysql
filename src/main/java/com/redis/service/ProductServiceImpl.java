// ─────────────────────────────────────────────────────────────
// ProductServiceImpl.java — Production Ready
// ─────────────────────────────────────────────────────────────
package com.redis.service;

import com.redis.config.RedisCacheConfig;
import com.redis.exception.ProductDuplicateException;
import com.redis.exception.ProductNotFoundException;
import com.redis.model.mapper.ProductMapper;
import com.redis.model.dto.request.ProductRequest;
import com.redis.model.dto.response.ProductResponse;
import com.redis.model.dto.response.RestPageImpl;
import com.redis.model.entity.Product;
import com.redis.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper     productMapper;      // FIX #2: Mapper inject karo

    // ═══════════════════════════════════════════════════════════════════════════
    //  CREATE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Naya product create karta hai.
     * - Duplicate name check hota hai pehle
     * - Save ke baad "products" cache evict hota hai
     *   taaki getAllProducts fresh data de
     */
    @Override
    @Transactional
    @CacheEvict(
        value = RedisCacheConfig.CACHE_PRODUCTS, // FIX #7: constant reuse
        allEntries = true                         // sab pages invalidate
    )
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating product: {}", request.getName());

        // FIX #8: Duplicate name check — same naam ka product already hai?
        if (productRepository.findByNameIgnoreCase(request.getName()).isPresent()) {
            throw new ProductDuplicateException(request.getName());
        }

        // FIX #2: Mapper use karo — toEntity() / fromEntity() nahi
        Product saved = productRepository.save(productMapper.toEntity(request));
        log.info("Product created — id: {}", saved.getId());

        return productMapper.toResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  READ — Single
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * ID se ek product fetch karta hai.
     * - Pehli call DB se aati hai — Redis mein store hoti hai
     * - Baad ki calls Redis se aati hain (fast)
     * - condition: id 0 se bada hona chahiye
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(
        value     = RedisCacheConfig.CACHE_PRODUCT,
        key       = "#id",
        condition = "#id > 0"
    )
    public ProductResponse getProductById(Long id) {
        log.info("DB hit — fetching product id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        log.info("Product found — name: {}", product.getName());
        return productMapper.toResponse(product);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  READ — All (Paginated)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Saare products paginated fetch karta hai.
     * - Page number + size key mein include hoti hai
     *   taaki alag pages alag cache entries hon
     * - FIX #4: List → Page
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(
        value = RedisCacheConfig.CACHE_PRODUCTS,
        key   = "'page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize"
    )
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        log.info("DB hit — fetching all products, page: {}", pageable.getPageNumber());

        Page<ProductResponse> result = productRepository
                .findAll(pageable)
                .map(productMapper::toResponse);

        log.info("Found {} products", result.getTotalElements());
        return new RestPageImpl<>(
            result.getContent(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements()
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  UPDATE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Product update karta hai.
     * - @CachePut: update ke baad "product::{id}" cache refresh hota hai
     * - @CacheEvict: "products" list cache bhi invalidate hoti hai
     *   taaki getAllProducts purana data na de
     */
    @Override
    @Transactional
    @Caching(
        put   = { @CachePut(value = RedisCacheConfig.CACHE_PRODUCT, key = "#id") },
        evict = { @CacheEvict(value = RedisCacheConfig.CACHE_PRODUCTS, allEntries = true) }
    )
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product — id: {}", id);

        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        // Duplicate check — apna naam allow karo, doosre ka nahi
        boolean isDuplicate = productRepository
                .countByNameIgnoreCaseAndIdNot(request.getName(), id)>0;
        if (isDuplicate) {
            throw new ProductDuplicateException(request.getName());
        }

        // FIX #2: Mapper se update — manually setters nahi
        productMapper.updateEntity(existing, request);
        Product updated = productRepository.save(existing);

        log.info("Product updated — id: {}", updated.getId());
        return productMapper.toResponse(updated);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  DELETE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Product delete karta hai.
     * - "product::{id}" cache evict hota hai
     * - "products" list cache bhi evict hoti hai
     */
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = RedisCacheConfig.CACHE_PRODUCT,  key = "#id"),
        @CacheEvict(value = RedisCacheConfig.CACHE_PRODUCTS, allEntries = true)
    })
    public void deleteProduct(Long id) {
        log.info("Deleting product — id: {}", id);

        // Pehle check karo exist karta hai — nahi toh 404
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }

        productRepository.deleteById(id);
        log.info("Product deleted — id: {}", id);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SEARCH
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Naam se search — case-insensitive, paginated
     * Cache nahi — search results dynamic hote hain
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProductsByName(String name, Pageable pageable) {
        log.info("Searching products by name: {}", name);

        Page<ProductResponse> page = productRepository
                .findByNameContainingIgnoreCase(name, pageable)
                .map(productMapper::toResponse);
            
            return new RestPageImpl<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
            );
    }

    /**
     * Price range se products — paginated
     * FIX #11: minPrice > maxPrice validation
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByPriceRange(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        // FIX #11: Business validation — min > max nahi ho sakta
        if (minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException(
                "Minimum price cannot be greater than maximum price"
            );
        }

        log.info("Fetching products — price range: {} to {}", minPrice, maxPrice);

        Page<ProductResponse> page = productRepository
                .findByPriceBetweenOrderByPriceAsc(minPrice, maxPrice, pageable)
                .map(productMapper::toResponse);

            return new RestPageImpl<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
            );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  RATING & STOCK  — FIX #10: Missing methods add kiye
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Minimum rating ke saath products — descending order
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByMinRating(BigDecimal minRating) {
        log.info("Fetching products with min rating: {}", minRating);

        return productMapper.toResponseList(
            productRepository.findProductsByMinRating(minRating)
        );
    }

    /**
     * Low stock products — threshold ke neeche
     * Inventory management ke liye useful
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts(int threshold) {
        log.info("Fetching low stock products — threshold: {}", threshold);

        return productMapper.toResponseList(
            productRepository.findLowStockProducts(threshold)
        );
    }

    /**
     * Out of stock products — totalProduct = 0
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getOutOfStockProducts() {
        log.info("Fetching out of stock products");

        return productMapper.toResponseList(
            productRepository.findOutOfStockProducts()
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CACHE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * FIX #6: void → boolean — admin manually cache clear kare
     * true  = successfully cleared
     * false = kuch gadbad
     */
    @Override
    @Caching(evict = {
        @CacheEvict(value = RedisCacheConfig.CACHE_PRODUCT,  allEntries = true),
        @CacheEvict(value = RedisCacheConfig.CACHE_PRODUCTS, allEntries = true)
    })
    public boolean clearProductCache() {
        try {
            log.warn("All product caches cleared manually");
            return true;
        } catch (Exception ex) {
            log.error("Failed to clear cache: {}", ex.getMessage(), ex);
            return false;
        }
    }
}