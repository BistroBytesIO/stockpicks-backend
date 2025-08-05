package com.stockpicks.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class YahooFinanceService {

    private static final Logger logger = LoggerFactory.getLogger(YahooFinanceService.class);
    
    private final RestTemplate restTemplate;

    @Value("${yahoo.finance.api.key}")
    private String apiKey;

    @Value("${yahoo.finance.api.base.url}")
    private String baseUrl;

    public YahooFinanceService() {
        this.restTemplate = new RestTemplate();
    }
    
    private void addHeaders() {
        // Add required headers for RapidAPI - done per request to ensure apiKey is available
        this.restTemplate.getInterceptors().clear();
        this.restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("X-RapidAPI-Key", apiKey);
            request.getHeaders().add("X-RapidAPI-Host", "yh-finance.p.rapidapi.com");
            request.getHeaders().add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            return execution.execute(request, body);
        });
    }

    @Cacheable(value = "yahooFinanceChart", key = "#symbol + '_' + #period")
    public Map<String, Object> getChartData(String symbol, String period) {
        try {
            addHeaders(); // Ensure headers are set with the injected API key
            logger.info("Fetching Yahoo Finance chart data for symbol: {} with period: {}", symbol, period);
            logger.info("DEBUG: API Key configured: {}, Base URL: {}", (apiKey != null && !apiKey.isEmpty()) ? "YES" : "NO", baseUrl);
            
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/stock/v3/get-chart")
                    .queryParam("interval", mapPeriodToInterval(period))
                    .queryParam("symbol", symbol)
                    .queryParam("range", period)
                    .queryParam("region", "US")
                    .queryParam("includePrePost", "false")
                    .queryParam("useYfid", "true")
                    .queryParam("includeAdjustedClose", "true")
                    .queryParam("events", "capitalGain,div,split")
                    .toUriString();

            logger.info("Yahoo Finance API URL: {}", url.replace(apiKey, "***"));
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            // Debug: Log the actual response structure
            logger.info("DEBUG: Yahoo Finance API response for {}: {}", symbol, response);
            
            if (response == null) {
                logger.warn("No response from Yahoo Finance for symbol: {}", symbol);
                return createEmptyResponse();
            }
            
            // Debug: Log response keys and structure
            logger.info("DEBUG: Response keys for {}: {}", symbol, response.keySet());
            
            return processYahooResponse(response, symbol, period);
            
        } catch (Exception e) {
            logger.error("Error fetching Yahoo Finance data for symbol {}: {}", symbol, e.getMessage(), e);
            return createEmptyResponse();
        }
    }

    private Map<String, Object> processYahooResponse(Map<String, Object> response, String symbol, String period) {
        try {
            logger.info("DEBUG: Processing Yahoo response for {}, response structure: {}", symbol, response);
            
            // Navigate through Yahoo's response structure
            Map<String, Object> chart = (Map<String, Object>) response.get("chart");
            if (chart == null) {
                logger.warn("No chart data in Yahoo response for symbol: {}. Available keys: {}", symbol, response.keySet());
                return createEmptyResponse();
            }
            
            logger.info("DEBUG: Chart data found for {}, chart keys: {}", symbol, chart.keySet());
            
            List<Map<String, Object>> results = (List<Map<String, Object>>) chart.get("result");
            if (results == null || results.isEmpty()) {
                logger.warn("No result data in Yahoo chart response for symbol: {}. Chart contains: {}", symbol, chart);
                return createEmptyResponse();
            }
            
            logger.info("DEBUG: Found {} results for {}", results.size(), symbol);
            
            Map<String, Object> result = results.get(0);
            
            // Extract metadata
            Map<String, Object> meta = (Map<String, Object>) result.get("meta");
            
            // Extract timestamps
            List<Long> timestamps = (List<Long>) result.get("timestamp");
            if (timestamps == null || timestamps.isEmpty()) {
                logger.warn("No timestamp data for symbol: {}", symbol);
                return createEmptyResponse();
            }
            
            // Extract indicators
            Map<String, Object> indicators = (Map<String, Object>) result.get("indicators");
            if (indicators == null) {
                logger.warn("No indicators data for symbol: {}", symbol);
                return createEmptyResponse();
            }
            
            List<Map<String, Object>> quotes = (List<Map<String, Object>>) indicators.get("quote");
            if (quotes == null || quotes.isEmpty()) {
                logger.warn("No quote indicators for symbol: {}", symbol);
                return createEmptyResponse();
            }
            
            Map<String, Object> quote = quotes.get(0);
            
            // Extract OHLCV data
            List<Double> opens = (List<Double>) quote.get("open");
            List<Double> highs = (List<Double>) quote.get("high");
            List<Double> lows = (List<Double>) quote.get("low");
            List<Double> closes = (List<Double>) quote.get("close");
            List<Object> volumeObjects = (List<Object>) quote.get("volume");
            List<Long> volumes = new ArrayList<>();
            if (volumeObjects != null) {
                for (Object vol : volumeObjects) {
                    if (vol instanceof Integer) {
                        volumes.add(((Integer) vol).longValue());
                    } else if (vol instanceof Long) {
                        volumes.add((Long) vol);
                    } else if (vol != null) {
                        volumes.add(Long.valueOf(vol.toString()));
                    } else {
                        volumes.add(null);
                    }
                }
            }
            
            // Validate all arrays have data
            if (opens == null || highs == null || lows == null || closes == null || volumes == null) {
                logger.warn("Incomplete OHLCV data for symbol: {}", symbol);
                return createEmptyResponse();
            }
            
            // Create response in current format expected by frontend
            Map<String, Object> chartData = new HashMap<>();
            
            // Convert data to arrays and filter out null values
            List<Double> filteredOpens = new ArrayList<>();
            List<Double> filteredHighs = new ArrayList<>();
            List<Double> filteredLows = new ArrayList<>();
            List<Double> filteredCloses = new ArrayList<>();
            List<Long> filteredVolumes = new ArrayList<>();
            List<Long> filteredTimestamps = new ArrayList<>();
            
            for (int i = 0; i < timestamps.size(); i++) {
                if (i < opens.size() && i < highs.size() && i < lows.size() && 
                    i < closes.size() && i < volumes.size() &&
                    opens.get(i) != null && highs.get(i) != null && lows.get(i) != null && 
                    closes.get(i) != null && volumes.get(i) != null) {
                    
                    filteredOpens.add(opens.get(i));
                    filteredHighs.add(highs.get(i));
                    filteredLows.add(lows.get(i));
                    filteredCloses.add(closes.get(i));
                    filteredVolumes.add(volumes.get(i));
                    filteredTimestamps.add(timestamps.get(i));
                }
            }
            
            // Create chart data in format expected by frontend
            chartData.put("c", filteredCloses.toArray(new Double[0]));
            chartData.put("h", filteredHighs.toArray(new Double[0]));
            chartData.put("l", filteredLows.toArray(new Double[0]));
            chartData.put("o", filteredOpens.toArray(new Double[0]));
            chartData.put("v", filteredVolumes.toArray(new Long[0]));
            chartData.put("t", filteredTimestamps.toArray(new Long[0]));
            chartData.put("s", "ok");
            
            // Create comprehensive response with quote data
            Map<String, Object> fullResponse = new HashMap<>();
            fullResponse.put("symbol", symbol);
            fullResponse.put("candles", chartData);
            fullResponse.put("period", period);
            fullResponse.put("lastUpdated", LocalDateTime.now());
            
            // Add quote information from meta
            if (meta != null) {
                Map<String, Object> quoteData = new HashMap<>();
                quoteData.put("c", meta.get("regularMarketPrice"));  // current price
                quoteData.put("h", meta.get("regularMarketDayHigh")); // day high
                quoteData.put("l", meta.get("regularMarketDayLow"));  // day low
                quoteData.put("pc", meta.get("chartPreviousClose")); // previous close
                quoteData.put("t", System.currentTimeMillis() / 1000); // current timestamp
                
                fullResponse.put("quote", quoteData);
            }
            
            logger.info("Successfully processed {} data points for symbol: {}", filteredTimestamps.size(), symbol);
            return fullResponse;
            
        } catch (Exception e) {
            logger.error("Error processing Yahoo Finance response for symbol {}: {}", symbol, e.getMessage(), e);
            return createEmptyResponse();
        }
    }

    private String mapPeriodToInterval(String period) {
        // Map period to appropriate interval
        switch (period.toLowerCase()) {
            case "1d":
                return "5m";
            case "5d":
                return "15m";
            case "1mo":
                return "1d";
            case "3mo":
                return "1d";
            case "6mo":
                return "1d";
            case "1y":
                return "1wk";
            case "2y":
                return "1wk";
            case "5y":
                return "1mo";
            case "10y":
                return "1mo";
            case "ytd":
                return "1d";
            case "max":
                return "1mo";
            default:
                return "1d";
        }
    }

    @Cacheable(value = "yahooFinanceQuote", key = "#symbol")
    public Map<String, Object> getStockQuote(String symbol) {
        try {
            logger.info("Fetching Yahoo Finance quote for symbol: {}", symbol);
            
            // Get chart data with minimal period to extract current quote
            Map<String, Object> chartData = getChartData(symbol, "1d");
            
            if (chartData.containsKey("quote")) {
                logger.info("Successfully extracted quote for symbol: {}", symbol);
                return (Map<String, Object>) chartData.get("quote");
            } else {
                logger.warn("No quote data available for symbol: {}", symbol);
                return new HashMap<>();
            }
            
        } catch (Exception e) {
            logger.error("Error fetching quote for symbol {}: {}", symbol, e.getMessage());
            return new HashMap<>();
        }
    }

    private Map<String, Object> createEmptyResponse() {
        Map<String, Object> emptyResponse = new HashMap<>();
        emptyResponse.put("c", new Double[0]);
        emptyResponse.put("h", new Double[0]);
        emptyResponse.put("l", new Double[0]);
        emptyResponse.put("o", new Double[0]);
        emptyResponse.put("v", new Long[0]);
        emptyResponse.put("t", new Long[0]);
        emptyResponse.put("s", "no_data");
        return emptyResponse;
    }
}