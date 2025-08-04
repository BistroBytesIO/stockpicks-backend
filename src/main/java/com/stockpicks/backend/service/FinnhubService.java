package com.stockpicks.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
public class FinnhubService {

    private static final Logger logger = LoggerFactory.getLogger(FinnhubService.class);
    
    private final RestTemplate restTemplate;

    @Value("${finnhub.api.key}")
    private String apiKey;

    @Value("${finnhub.api.base.url}")
    private String baseUrl;

    public FinnhubService() {
        this.restTemplate = new RestTemplate();
    }

    @Cacheable(value = "stockQuotes", key = "#symbol")
    public Map<String, Object> getStockQuote(String symbol) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/quote")
                    .queryParam("symbol", symbol)
                    .queryParam("token", apiKey)
                    .toUriString();

            logger.info("Fetching quote for symbol: {}", symbol);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null) {
                logger.info("Successfully fetched quote for {}: current price = {}", symbol, response.get("c"));
                return response;
            } else {
                logger.warn("No quote data received for symbol: {}", symbol);
                return new HashMap<>();
            }
        } catch (Exception e) {
            logger.error("Error fetching quote for symbol {}: {}", symbol, e.getMessage());
            return new HashMap<>();
        }
    }

    @Cacheable(value = "stockCandles", key = "#symbol + '_' + #period")
    public Map<String, Object> getStockCandles(String symbol, String period) {
        try {
            // Calculate time range based on period
            long toTimestamp = Instant.now().getEpochSecond();
            long fromTimestamp;
            
            switch (period.toUpperCase()) {
                case "1D":
                    fromTimestamp = Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond();
                    break;
                case "1W":
                    fromTimestamp = Instant.now().minus(7, ChronoUnit.DAYS).getEpochSecond();
                    break;
                case "1M":
                    fromTimestamp = Instant.now().minus(30, ChronoUnit.DAYS).getEpochSecond();
                    break;
                case "3M":
                    fromTimestamp = Instant.now().minus(90, ChronoUnit.DAYS).getEpochSecond();
                    break;
                default:
                    fromTimestamp = Instant.now().minus(30, ChronoUnit.DAYS).getEpochSecond();
            }

            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/stock/candle")
                    .queryParam("symbol", symbol)
                    .queryParam("resolution", "D") // Daily resolution
                    .queryParam("from", fromTimestamp)
                    .queryParam("to", toTimestamp)
                    .queryParam("token", apiKey)
                    .toUriString();

            logger.info("Fetching candle data for symbol: {} with period: {}", symbol, period);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && "ok".equals(response.get("s"))) {
                logger.info("Successfully fetched candle data for {}", symbol);
                return response;
            } else {
                logger.warn("No candle data received for symbol: {} (status: {})", symbol, response != null ? response.get("s") : "null");
                return new HashMap<>();
            }
        } catch (Exception e) {
            logger.error("Error fetching candle data for symbol {}: {}", symbol, e.getMessage());
            return new HashMap<>();
        }
    }

    @Cacheable(value = "symbolSearch", key = "#query")
    public Map<String, Object> searchSymbol(String query) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/search")
                    .queryParam("q", query)
                    .queryParam("token", apiKey)
                    .toUriString();

            logger.info("Searching for symbol: {}", query);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null) {
                logger.info("Successfully found {} results for query: {}", response.get("count"), query);
                return response;
            } else {
                logger.warn("No search results for query: {}", query);
                return new HashMap<>();
            }
        } catch (Exception e) {
            logger.error("Error searching for symbol {}: {}", query, e.getMessage());
            return new HashMap<>();
        }
    }

    public Map<String, Object> getChartData(String symbol, String period) {
        Map<String, Object> chartData = new HashMap<>();
        
        try {
            // Get current quote
            Map<String, Object> quote = getStockQuote(symbol);
            
            // Get historical candle data
            Map<String, Object> candles = getStockCandles(symbol, period);
            
            // Combine data for frontend
            chartData.put("symbol", symbol);
            chartData.put("quote", quote);
            chartData.put("candles", candles);
            chartData.put("period", period);
            chartData.put("lastUpdated", LocalDateTime.now());
            
            return chartData;
        } catch (Exception e) {
            logger.error("Error getting chart data for symbol {}: {}", symbol, e.getMessage());
            return chartData;
        }
    }
}