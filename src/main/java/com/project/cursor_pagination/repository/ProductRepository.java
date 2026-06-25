package com.project.cursor_pagination.repository;

import com.project.cursor_pagination.dto.ProductResponseDTO;
import com.project.cursor_pagination.model.Product;
import com.project.cursor_pagination.model.enums.Category;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@NullMarked
public interface ProductRepository extends JpaRepository<Product, Long> {

    // @formatter:off
    @Query("""
            SELECT new com.project.cursor_pagination.dto.ProductResponseDTO(
                p.id, p.name, p.category, p.price, p.createdAt, p.updatedAt
            ) FROM Product p
            WHERE p.createdAt <= :cursor_snapshot
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    // @formatter:on
    List<ProductResponseDTO> findBySnapshot(
            @Param("cursor_snapshot") Instant snapShotTime,
            PageRequest pageRequest);

    // @formatter:off
    @Query("""
            SELECT new com.project.cursor_pagination.dto.ProductResponseDTO(
                p.id, p.name, p.category, p.price, p.createdAt, p.updatedAt
            ) FROM Product p
            WHERE p.createdAt <= :cursor_snapshot
            AND (
                    p.createdAt < :cursor_date
                    OR (
                        p.createdAt = :cursor_date
                        AND p.id < :cursor_id
                    )
                )
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    // @formatter:on
    List<ProductResponseDTO> findByCursor(
            @Param("cursor_snapshot") Instant snapShotTime,
            @Param("cursor_id") Long lastSeenId,
            @Param("cursor_date") Instant lastSeenCreatedAt,
            PageRequest pageRequest);

    // @formatter:off
    @Query("""
            SELECT new com.project.cursor_pagination.dto.ProductResponseDTO(
                p.id, p.name, p.category, p.price, p.createdAt, p.updatedAt
            ) FROM Product p
            WHERE p.category = :category_type
            AND p.createdAt <= :cursor_snapshot
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    // @formatter:on
    List<ProductResponseDTO> findByFilterAndSnapshot(
            @Param("category_type") Category filter,
            @Param("cursor_snapshot") Instant snapShotTime,
            PageRequest pageRequest);

    // @formatter:off
    @Query("""
            SELECT new com.project.cursor_pagination.dto.ProductResponseDTO(
                p.id, p.name, p.category, p.price, p.createdAt, p.updatedAt
            ) FROM Product p
            WHERE p.category = :category_type
            AND p.createdAt <= :cursor_snapshot
            AND (
                    p.createdAt < :cursor_date
                    OR (
                        p.createdAt = :cursor_date
                        AND p.id < :cursor_id
                    )
                )
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    // @formatter:on
    List<ProductResponseDTO> findByCursorAndFilter(
            @Param("cursor_snapshot") Instant snapShotTime,
            @Param("cursor_id") Long lastSeenId,
            @Param("cursor_date") Instant lastSeenCreatedAt,
            @Param("category_type") Category filter,
            PageRequest pageRequest);
}
