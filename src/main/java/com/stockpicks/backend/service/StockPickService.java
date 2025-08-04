package com.stockpicks.backend.service;

import com.stockpicks.backend.entity.StockPick;
import com.stockpicks.backend.enums.PickType;
import com.stockpicks.backend.repository.StockPickRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class StockPickService {

    @Autowired
    private StockPickRepository stockPickRepository;

    @Autowired
    private GoogleSheetsService googleSheetsService;

    public List<StockPick> getAllStockPicks() {
        return stockPickRepository.findByOrderByPickDateDesc();
    }

    public List<StockPick> getStockPicksByType(PickType pickType) {
        return stockPickRepository.findByPickType(pickType);
    }

    public List<StockPick> getStockPicksBySymbol(String symbol) {
        return stockPickRepository.findBySymbol(symbol.toUpperCase());
    }

    public Optional<StockPick> getStockPickById(Long id) {
        return stockPickRepository.findById(id);
    }

    public List<StockPick> getRecentStockPicks(int limit) {
        return stockPickRepository.findTopByOrderByPickDateDesc(limit);
    }

    public List<StockPick> getStockPicksByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return stockPickRepository.findByPickDateBetweenOrderByPickDateDesc(startDate, endDate);
    }

    public StockPick saveStockPick(StockPick stockPick) {
        stockPick.setCreatedAt(LocalDateTime.now());
        stockPick.setUpdatedAt(LocalDateTime.now());
        return stockPickRepository.save(stockPick);
    }

    public StockPick updateStockPick(Long id, StockPick updatedStockPick) {
        Optional<StockPick> existingPick = stockPickRepository.findById(id);
        if (existingPick.isPresent()) {
            StockPick stockPick = existingPick.get();
            stockPick.setSymbol(updatedStockPick.getSymbol());
            stockPick.setCompanyName(updatedStockPick.getCompanyName());
            stockPick.setPickType(updatedStockPick.getPickType());
            stockPick.setEntryPrice(updatedStockPick.getEntryPrice());
            stockPick.setCurrentPrice(updatedStockPick.getCurrentPrice());
            stockPick.setTargetPrice(updatedStockPick.getTargetPrice());
            stockPick.setCommentary(updatedStockPick.getCommentary());
            stockPick.setUpdatedAt(LocalDateTime.now());
            return stockPickRepository.save(stockPick);
        }
        throw new RuntimeException("Stock pick not found with id: " + id);
    }

    public void deleteStockPick(Long id) {
        stockPickRepository.deleteById(id);
    }

    public int syncStockPicksFromGoogleSheets() {
        try {
            return googleSheetsService.syncStockPicks();
        } catch (Exception e) {
            throw new RuntimeException("Error syncing stock picks from Google Sheets: " + e.getMessage());
        }
    }
}