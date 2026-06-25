package com.project.cursor_pagination.service;

import com.project.cursor_pagination.dto.FeedResponse;
import com.project.cursor_pagination.dto.ProductResponseDTO;
import com.project.cursor_pagination.model.Cursor;
import com.project.cursor_pagination.model.enums.Category;
import com.project.cursor_pagination.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepo;
    private final UtilService utilService;

    public FeedResponse generateProductFeed(String encodedCursor, int limit, Category filter) throws BadRequestException {
        limit = Math.clamp(limit, 10, 50);

        Cursor decodedCursor = utilService.decodeCursor(encodedCursor);

        // either page refresh or first request
        boolean isNewSession = Objects.isNull(decodedCursor)
                || !Objects.equals(decodedCursor.getCategory(), filter);

        // create snapshot on isNewSession == true only (to avoid new entries while scroll)
        Instant snapshotTime = isNewSession
                ? Instant.now()
                : decodedCursor.getSnapShotTime();

        List<ProductResponseDTO> result;

        if (isNewSession) {
            // filter does not match previous request filter

            if (Objects.equals(filter, Category.NONE)) {
                // (cursor == null || cursor !=null) && filter == null

                result = productRepo.findBySnapshot(snapshotTime, PageRequest.of(0, limit));
            } else {
                // (cursor == null || cursor !=null) && filter != null

                result = productRepo.findByFilterAndSnapshot(filter, snapshotTime, PageRequest.of(0, limit));
            }
        } else {
            // find subsequent page (continue scroll)

            if (Objects.equals(filter, Category.NONE)) {
                // cursor !=null && filter == null

                result = productRepo.findByCursor(
                        snapshotTime,
                        decodedCursor.getLastSeenId(),
                        decodedCursor.getLastSeenCreatedAt(),
                        PageRequest.of(0, limit)
                );
            } else {
                // cursor != null && filter != null

                result = productRepo.findByCursorAndFilter(
                        snapshotTime,
                        decodedCursor.getLastSeenId(),
                        decodedCursor.getLastSeenCreatedAt(),
                        filter,
                        PageRequest.of(0, limit)
                );
            }
        }

        if (result.isEmpty()) {
            return new FeedResponse(List.of(), null);
        }

        Cursor nextCursor = Cursor.builder()
                .lastSeenId(result.getLast().id())
                .lastSeenCreatedAt(result.getLast().createdAt())
                .category(filter)
                .snapShotTime(snapshotTime)
                .build();

        return new FeedResponse(result, utilService.encodeCursor(nextCursor));
    }
}
