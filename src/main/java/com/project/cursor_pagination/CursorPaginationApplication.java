package com.project.cursor_pagination;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CursorPaginationApplication {

    public static void main(String[] args) {
        SpringApplication.run(CursorPaginationApplication.class, args);
    }

}
