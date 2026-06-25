package com.project.cursor_pagination.dto;

import com.project.cursor_pagination.model.enums.Category;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponseDTO(Long id, String name, Category category, BigDecimal price, Instant createdAt, Instant updatedAt) {
}
