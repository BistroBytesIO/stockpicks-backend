package com.stockpicks.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "blog_posts")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlogPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "author_id")
    private Long authorId;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private boolean isPublished = false;

    private String featuredImageUrl;

    private String tags;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime publishedAt;

    // Constructor for creating blog post with basic info
    public BlogPost(String title, String content, String summary, Long authorId, String category) {
        this.title = title;
        this.content = content;
        this.summary = summary;
        this.authorId = authorId;
        this.category = category;
        this.isPublished = false;
    }
}