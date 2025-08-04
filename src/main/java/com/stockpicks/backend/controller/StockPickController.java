package com.stockpicks.backend.controller;

import com.stockpicks.backend.entity.GoogleSheetsSync;
import com.stockpicks.backend.entity.StockPick;
import com.stockpicks.backend.enums.PickType;
import com.stockpicks.backend.service.FinnhubService;
import com.stockpicks.backend.service.GoogleSheetsService;
import com.stockpicks.backend.service.StockPickService;
import com.stockpicks.backend.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/stock-picks")
@CrossOrigin(origins = "http://localhost:3000")
public class StockPickController {

    @Autowired
    private StockPickService stockPickService;

    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private GoogleSheetsService googleSheetsService;
    
    @Autowired
    private FinnhubService finnhubService;

    @GetMapping
    public ResponseEntity<List<StockPick>> getAllStockPicks(Authentication authentication) {
        if (authentication != null && subscriptionService.hasActiveSubscription(authentication.getName())) {
            List<StockPick> stockPicks = stockPickService.getAllStockPicks();
            return ResponseEntity.ok(stockPicks);
        } else {
            List<StockPick> recentPicks = stockPickService.getRecentStockPicks(5);
            return ResponseEntity.ok(recentPicks);
        }
    }

    @GetMapping("/type/{pickType}")
    public ResponseEntity<List<StockPick>> getStockPicksByType(@PathVariable PickType pickType, Authentication authentication) {
        if (authentication == null || !subscriptionService.hasActiveSubscription(authentication.getName())) {
            return ResponseEntity.status(403).body(null);
        }
        
        List<StockPick> stockPicks = stockPickService.getStockPicksByType(pickType);
        return ResponseEntity.ok(stockPicks);
    }

    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<List<StockPick>> getStockPicksBySymbol(@PathVariable String symbol, Authentication authentication) {
        if (authentication == null || !subscriptionService.hasActiveSubscription(authentication.getName())) {
            return ResponseEntity.status(403).body(null);
        }
        
        List<StockPick> stockPicks = stockPickService.getStockPicksBySymbol(symbol);
        return ResponseEntity.ok(stockPicks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockPick> getStockPickById(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !subscriptionService.hasActiveSubscription(authentication.getName())) {
            return ResponseEntity.status(403).body(null);
        }
        
        Optional<StockPick> stockPick = stockPickService.getStockPickById(id);
        return stockPick.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/recent")
    public ResponseEntity<List<StockPick>> getRecentStockPicks(@RequestParam(defaultValue = "10") int limit) {
        List<StockPick> recentPicks = stockPickService.getRecentStockPicks(limit);
        return ResponseEntity.ok(recentPicks);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<StockPick>> getStockPicksByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication authentication) {
        
        if (authentication == null || !subscriptionService.hasActiveSubscription(authentication.getName())) {
            return ResponseEntity.status(403).body(null);
        }
        
        List<StockPick> stockPicks = stockPickService.getStockPicksByDateRange(startDate, endDate);
        return ResponseEntity.ok(stockPicks);
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncStockPicksFromGoogleSheets() {
        try {
            int newPicksCount = stockPickService.syncStockPicksFromGoogleSheets();
            
            // Create response object with sync results
            var syncResult = new java.util.HashMap<String, Object>();
            syncResult.put("newPicksCount", newPicksCount);
            
            if (newPicksCount > 0) {
                syncResult.put("message", newPicksCount + " new stock pick" + (newPicksCount == 1 ? "" : "s") + " added successfully!");
            } else {
                syncResult.put("message", "Your stock picks are already up-to-date! No new picks found.");
            }
            
            return ResponseEntity.ok(syncResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error syncing stock picks: " + e.getMessage());
        }
    }
    
    @GetMapping("/sync/status")
    public ResponseEntity<?> getSyncStatus() {
        try {
            GoogleSheetsSync lastSync = googleSheetsService.getLastSyncStatus();
            
            // Create a response object with sync status information
            var syncStatus = new java.util.HashMap<String, Object>();
            syncStatus.put("syncEnabled", googleSheetsService.isSyncEnabled());
            syncStatus.put("businessHoursEnabled", googleSheetsService.isBusinessHoursEnabled());
            syncStatus.put("businessHours", googleSheetsService.getBusinessHours());
            
            if (lastSync != null) {
                syncStatus.put("lastSyncTime", lastSync.getLastSyncTime());
                syncStatus.put("rowsProcessed", lastSync.getRowsProcessed());
                syncStatus.put("spreadsheetId", lastSync.getSpreadsheetId());
                syncStatus.put("lastSyncRange", lastSync.getLastSyncRange());
            } else {
                syncStatus.put("lastSyncTime", null);
                syncStatus.put("message", "No sync history available");
            }
            
            return ResponseEntity.ok(syncStatus);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting sync status: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<StockPick> createStockPick(@RequestBody StockPick stockPick) {
        StockPick savedStockPick = stockPickService.saveStockPick(stockPick);
        return ResponseEntity.ok(savedStockPick);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StockPick> updateStockPick(@PathVariable Long id, @RequestBody StockPick stockPick) {
        try {
            StockPick updatedStockPick = stockPickService.updateStockPick(id, stockPick);
            return ResponseEntity.ok(updatedStockPick);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStockPick(@PathVariable Long id) {
        stockPickService.deleteStockPick(id);
        return ResponseEntity.ok("Stock pick deleted successfully");
    }
    
    @GetMapping("/{symbol}/chart-data")
    public ResponseEntity<?> getChartData(@PathVariable String symbol, 
                                        @RequestParam(defaultValue = "1M") String period,
                                        Authentication authentication) {
        if (authentication == null || !subscriptionService.hasActiveSubscription(authentication.getName())) {
            return ResponseEntity.status(403).body("Active subscription required");
        }
        
        try {
            var chartData = finnhubService.getChartData(symbol, period);
            return ResponseEntity.ok(chartData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching chart data: " + e.getMessage());
        }
    }
    
    @GetMapping("/{symbol}/quote")
    public ResponseEntity<?> getStockQuote(@PathVariable String symbol, Authentication authentication) {
        if (authentication == null || !subscriptionService.hasActiveSubscription(authentication.getName())) {
            return ResponseEntity.status(403).body("Active subscription required");
        }
        
        try {
            var quote = finnhubService.getStockQuote(symbol);
            return ResponseEntity.ok(quote);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching quote: " + e.getMessage());
        }
    }
    
    @GetMapping("/charts/batch")
    public ResponseEntity<?> getBatchChartData(@RequestParam String symbols,
                                             @RequestParam(defaultValue = "1M") String period,
                                             Authentication authentication) {
        if (authentication == null || !subscriptionService.hasActiveSubscription(authentication.getName())) {
            return ResponseEntity.status(403).body("Active subscription required");
        }
        
        try {
            String[] symbolArray = symbols.split(",");
            var batchData = new java.util.HashMap<String, Object>();
            
            for (String symbol : symbolArray) {
                if (!symbol.trim().isEmpty()) {
                    batchData.put(symbol.trim(), finnhubService.getChartData(symbol.trim(), period));
                }
            }
            
            return ResponseEntity.ok(batchData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching batch chart data: " + e.getMessage());
        }
    }
}