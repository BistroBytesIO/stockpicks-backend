package com.stockpicks.backend.controller;

import com.stockpicks.backend.dto.blog.BlogPostRequest;
import com.stockpicks.backend.dto.blog.BlogPostResponse;
import com.stockpicks.backend.entity.BlogPost;
import com.stockpicks.backend.service.BlogPostService;
import com.stockpicks.backend.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/blog")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class BlogPostController {

    @Autowired
    private BlogPostService blogPostService;

    @Autowired
    private AdminService adminService;

    @GetMapping("/posts")
    public ResponseEntity<List<BlogPostResponse>> getAllPublishedPosts() {
        try {
            List<BlogPost> posts = blogPostService.getPublishedBlogPosts();
            List<BlogPostResponse> response = posts.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<BlogPostResponse> getPost(@PathVariable Long id) {
        try {
            BlogPost post = blogPostService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Blog post not found"));
            
            // Only return published posts for public endpoint
            if (!post.isPublished()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(convertToResponse(post));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/admin/posts")
    public ResponseEntity<List<BlogPostResponse>> getAllPosts(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }

        try {
            List<BlogPost> posts = blogPostService.getAllBlogPosts();
            List<BlogPostResponse> response = posts.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/admin/posts")
    public ResponseEntity<BlogPostResponse> createPost(@Valid @RequestBody BlogPostRequest request, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }

        try {
            String authorEmail = authentication.getName();
            BlogPost post = blogPostService.createBlogPost(
                    request.getTitle(),
                    request.getContent(),
                    request.getSummary(),
                    authorEmail,
                    request.getCategory(),
                    request.getTags(),
                    request.getFeaturedImageUrl()
            );

            return ResponseEntity.ok(convertToResponse(post));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/admin/posts/{id}")
    public ResponseEntity<BlogPostResponse> updatePost(@PathVariable Long id, @Valid @RequestBody BlogPostRequest request, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }

        try {
            BlogPost post = blogPostService.updateBlogPost(
                    id,
                    request.getTitle(),
                    request.getContent(),
                    request.getSummary(),
                    request.getCategory(),
                    request.getTags(),
                    request.getFeaturedImageUrl()
            );

            return ResponseEntity.ok(convertToResponse(post));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/admin/posts/{id}/publish")
    public ResponseEntity<BlogPostResponse> publishPost(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }

        try {
            BlogPost post = blogPostService.publishBlogPost(id);
            return ResponseEntity.ok(convertToResponse(post));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/admin/posts/{id}/unpublish")
    public ResponseEntity<BlogPostResponse> unpublishPost(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }

        try {
            BlogPost post = blogPostService.unpublishBlogPost(id);
            return ResponseEntity.ok(convertToResponse(post));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/admin/posts/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }

        try {
            blogPostService.deleteBlogPost(id);
            return ResponseEntity.ok("Blog post deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting blog post");
        }
    }

    private BlogPostResponse convertToResponse(BlogPost post) {
        // Get author information using authorId
        String authorName = "Unknown Author";
        String authorEmail = "unknown@example.com";
        
        try {
            if (post.getAuthorId() != null) {
                var author = adminService.findById(post.getAuthorId());
                if (author.isPresent()) {
                    authorName = author.get().getFirstName() + " " + author.get().getLastName();
                    authorEmail = author.get().getEmail();
                }
            }
        } catch (Exception e) {
            // Use default values if author lookup fails
        }
        
        return new BlogPostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getSummary(),
                authorName,
                authorEmail,
                post.getCategory(),
                post.isPublished(),
                post.getFeaturedImageUrl(),
                post.getTags(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getPublishedAt()
        );
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Check if the authenticated user exists in admin table
        try {
            String email = authentication.getName();
            return adminService.findByEmail(email).isPresent();
        } catch (Exception e) {
            return false;
        }
    }
}