package com.project.cursor_pagination.controller;

import com.project.cursor_pagination.dto.FeedResponse;
import com.project.cursor_pagination.model.enums.Category;
import com.project.cursor_pagination.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("generate-feed")
    public ResponseEntity<@NonNull FeedResponse> getProducts(
            @RequestParam(defaultValue = "") String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "NONE") Category filter
    ) throws BadRequestException {

        return ResponseEntity.ok()
                .body(productService.generateProductFeed(cursor, limit, filter));
    }
}
