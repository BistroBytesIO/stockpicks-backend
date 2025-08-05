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

    @GetMapping("/bonds")
    public ResponseEntity<List<NewsService.NewsItem>> getBonds() {
        try {
            List<NewsService.NewsItem> news = newsService.getBonds();
            return ResponseEntity.ok(news);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/currencies")
    public ResponseEntity<List<NewsService.NewsItem>> getCurrencies() {
        try {
            List<NewsService.NewsItem> news = newsService.getCurrencies();
            return ResponseEntity.ok(news);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/personal-finance")
    public ResponseEntity<List<NewsService.NewsItem>> getPersonalFinance() {
        try {
            List<NewsService.NewsItem> news = newsService.getPersonalFinance();
            return ResponseEntity.ok(news);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/stock-market")
    public ResponseEntity<List<NewsService.NewsItem>> getStockMarket() {
        try {
            List<NewsService.NewsItem> news = newsService.getStockMarket();
            return ResponseEntity.ok(news);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/economic-news")
    public ResponseEntity<List<NewsService.NewsItem>> getEconomicNews() {
        try {
            List<NewsService.NewsItem> news = newsService.getEconomicNews();
            return ResponseEntity.ok(news);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/options-futures")
    public ResponseEntity<List<NewsService.NewsItem>> getOptionsAndFutures() {
        try {
            List<NewsService.NewsItem> news = newsService.getOptionsAndFutures();
            return ResponseEntity.ok(news);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/all-categories")
    public ResponseEntity<Map<String, List<NewsService.NewsItem>>> getAllCategories() {
        try {
            Map<String, List<NewsService.NewsItem>> allNews = new HashMap<>();
            allNews.put("topStories", newsService.getTopStories());
            allNews.put("bonds", newsService.getBonds());
            allNews.put("currencies", newsService.getCurrencies());
            allNews.put("personalFinance", newsService.getPersonalFinance());
            allNews.put("stockMarket", newsService.getStockMarket());
            allNews.put("economicNews", newsService.getEconomicNews());
            allNews.put("optionsFutures", newsService.getOptionsAndFutures());
            
            return ResponseEntity.ok(allNews);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}