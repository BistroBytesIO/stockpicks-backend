package com.stockpicks.backend.controller;

import com.stockpicks.backend.service.MarketDataService;
import com.stockpicks.backend.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class MarketController {

    @Autowired
    private MarketDataService marketDataService;
    
    @Autowired
    private NewsService newsService;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getMarketDashboard() {
        try {
            Map<String, Object> dashboard = new HashMap<>();
            
            // Get market categories with stock data
            dashboard.put("marketCategories", marketDataService.getMarketCategories());
            
            // Get latest news (top 5)
            dashboard.put("latestNews", newsService.getTopStories());
            
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    @GetMapping("/categories")
    public ResponseEntity<Map<String, List<MarketDataService.MarketItem>>> getMarketCategories() {
        try {
            return ResponseEntity.ok(marketDataService.getMarketCategories());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}