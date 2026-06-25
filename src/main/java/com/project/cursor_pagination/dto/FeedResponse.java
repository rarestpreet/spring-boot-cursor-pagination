package com.project.cursor_pagination.dto;

import java.util.List;

public record FeedResponse(List<ProductResponseDTO> productList, String encodedCursor) {
}
