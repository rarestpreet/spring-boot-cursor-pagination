package com.project.cursor_pagination.service;

import com.project.cursor_pagination.model.Cursor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UtilService {

    private final JsonMapper objectMapper;

    public String encodeCursor(Cursor cursor) {
        String json = objectMapper.writeValueAsString(cursor);

        return Base64.getUrlEncoder()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    public Cursor decodeCursor(String encodedCursor) {
        if (Objects.equals(encodedCursor, "")) {
            return null;
        }
        String json = new String(
                Base64.getUrlDecoder().decode(encodedCursor),
                StandardCharsets.UTF_8
        );

        log.info("decodedCursor: {}", json);

        return objectMapper.readValue(json, Cursor.class);
    }
}
