package com.stockpicks.backend.dto.blog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlogPostResponse {
    private Long id;
    private String title;
    private String content;
    private String summary;
    private String authorName;
    private String authorEmail;
    private String category;
    private boolean isPublished;
    private String featuredImageUrl;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}