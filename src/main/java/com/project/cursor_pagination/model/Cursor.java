package com.project.cursor_pagination.model;

import com.project.cursor_pagination.model.enums.Category;
import lombok.*;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cursor {
    private Long lastSeenId;
    private Instant lastSeenCreatedAt;
    private Instant snapShotTime;
    private Category category;
}
