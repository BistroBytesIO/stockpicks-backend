package com.stockpicks.backend.controller;

import com.stockpicks.backend.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class NewsController {

    @Autowired
    private NewsService newsService;

    @GetMapping("/top-stories")
    public ResponseEntity<List<NewsService.NewsItem>> getTopStories() {
        try {
            List<NewsService.NewsItem> news = newsService.getTopStories();
            return ResponseEntity.ok(news);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }


}