package com.redis.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_products_name",  columnList = "name"),
        @Index(name = "idx_products_price", columnList = "price")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"createdAt", "updatedAt"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    // ─── Primary Key ────────────────────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long id;

    // ─── Business Fields ────────────────────────────────────────────────────────
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    // ─── Optimistic Locking ─────────────────────────────────────────────────────
    // Concurrent writes se bachata hai — race condition prevent hoti hai
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // ─── Audit Fields ───────────────────────────────────────────────────────────
    // @PrePersist/@PreUpdate hatao — Hibernate annotations zyada reliable hain
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}