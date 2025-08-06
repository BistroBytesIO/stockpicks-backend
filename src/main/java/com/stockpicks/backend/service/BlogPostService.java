package com.stockpicks.backend.service;

import com.stockpicks.backend.entity.Admin;
import com.stockpicks.backend.entity.BlogPost;
import com.stockpicks.backend.repository.BlogPostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BlogPostService {

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private AdminService adminService;

    public BlogPost createBlogPost(String title, String content, String summary, String authorEmail, String category, String tags, String featuredImageUrl) {
        Admin author = adminService.findByEmail(authorEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        BlogPost blogPost = new BlogPost(title, content, summary, author.getId(), category);
        blogPost.setTags(tags);
        blogPost.setFeaturedImageUrl(featuredImageUrl);
        return blogPostRepository.save(blogPost);
    }

    public BlogPost updateBlogPost(Long id, String title, String content, String summary, String category, String tags, String featuredImageUrl) {
        BlogPost blogPost = blogPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blog post not found"));

        blogPost.setTitle(title);
        blogPost.setContent(content);
        blogPost.setSummary(summary);
        blogPost.setCategory(category);
        blogPost.setTags(tags);
        blogPost.setFeaturedImageUrl(featuredImageUrl);

        return blogPostRepository.save(blogPost);
    }

    public BlogPost publishBlogPost(Long id) {
        BlogPost blogPost = blogPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blog post not found"));

        blogPost.setPublished(true);
        blogPost.setPublishedAt(LocalDateTime.now());

        return blogPostRepository.save(blogPost);
    }

    public BlogPost unpublishBlogPost(Long id) {
        BlogPost blogPost = blogPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blog post not found"));

        blogPost.setPublished(false);
        blogPost.setPublishedAt(null);

        return blogPostRepository.save(blogPost);
    }

    public void deleteBlogPost(Long id) {
        blogPostRepository.deleteById(id);
    }

    public Optional<BlogPost> findById(Long id) {
        return blogPostRepository.findById(id);
    }

    public List<BlogPost> getAllBlogPosts() {
        return blogPostRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<BlogPost> getPublishedBlogPosts() {
        return blogPostRepository.findByIsPublishedTrueOrderByPublishedAtDesc();
    }

    public List<BlogPost> getBlogPostsByAuthor(String authorEmail) {
        Admin author = adminService.findByEmail(authorEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        return blogPostRepository.findByAuthorIdOrderByCreatedAtDesc(author.getId());
    }

    public List<BlogPost> getBlogPostsByCategory(String category) {
        return blogPostRepository.findByCategoryOrderByCreatedAtDesc(category);
    }

    public long getPublishedPostsCount() {
        return blogPostRepository.countPublishedPosts();
    }

    public long getDraftPostsCount() {
        return blogPostRepository.countDraftPosts();
    }
}