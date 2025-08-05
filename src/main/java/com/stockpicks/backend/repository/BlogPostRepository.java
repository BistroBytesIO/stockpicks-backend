package com.stockpicks.backend.repository;

import com.stockpicks.backend.entity.Admin;
import com.stockpicks.backend.entity.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    List<BlogPost> findByIsPublishedTrueOrderByPublishedAtDesc();
    List<BlogPost> findByAuthorOrderByCreatedAtDesc(Admin author);
    List<BlogPost> findByCategoryOrderByCreatedAtDesc(String category);
    List<BlogPost> findAllByOrderByCreatedAtDesc();
    
    @Query("SELECT bp FROM BlogPost bp WHERE bp.isPublished = true ORDER BY bp.publishedAt DESC")
    List<BlogPost> findPublishedPosts();
    
    @Query("SELECT COUNT(bp) FROM BlogPost bp WHERE bp.isPublished = true")
    long countPublishedPosts();
    
    @Query("SELECT COUNT(bp) FROM BlogPost bp WHERE bp.isPublished = false")
    long countDraftPosts();
}