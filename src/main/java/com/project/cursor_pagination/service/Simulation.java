package com.project.cursor_pagination.service;

import com.project.cursor_pagination.model.Product;
import com.project.cursor_pagination.model.enums.Category;
import com.project.cursor_pagination.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class Simulation {

    private final ProductRepository productRepo;
    int entriesCount = 0;

    public void addProducts() {
        long startTime = System.currentTimeMillis();
        int newDataCount = ThreadLocalRandom.current().nextInt(500, 5000 + 1);

        if (entriesCount >= 200000) {
            log.info("Product insertion simulation completed");
            return;
        }

        for (int i = 0; i < newDataCount; i++) {
            if (entriesCount >= 200000) {
                break;
            }
            int index = ThreadLocalRandom.current().nextInt(Category.values().length - 1);
            Category category = Category.values()[index];

            Product product = Product.builder()
                    .name(category.toString().concat(UUID.randomUUID().toString()))
                    .category(category)
                    .price(BigDecimal.valueOf(ThreadLocalRandom.current().nextFloat(1000.0F, 10000.0F)))
                    .build();

            entriesCount++;
            productRepo.save(product);
        }

        log.info("Products count: {}, added in {}", entriesCount, System.currentTimeMillis() - startTime);
    }

}
