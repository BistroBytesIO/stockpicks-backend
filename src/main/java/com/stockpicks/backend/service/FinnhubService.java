package com.stockpicks.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
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
    
    @Autowired
    private AlphaVantageService alphaVantageService;

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

    public Map<String, Object> getStockCandles(String symbol, String period) {
        try {
            logger.info("Using Alpha Vantage for candle data - symbol: {} with period: {}", symbol, period);
            
            // Use Alpha Vantage service for OHLC candlestick data
            Map<String, Object> candleData = alphaVantageService.getProcessedCandleData(symbol, period);
            
            if (candleData.isEmpty()) {
                logger.warn("No candle data received from Alpha Vantage for symbol: {}", symbol);
                return new HashMap<>();
            }
            
            logger.info("Successfully fetched candle data from Alpha Vantage for {}", symbol);
            return candleData;
            
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