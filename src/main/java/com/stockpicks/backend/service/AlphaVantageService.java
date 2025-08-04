package com.stockpicks.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AlphaVantageService {

    private static final Logger logger = LoggerFactory.getLogger(AlphaVantageService.class);
    
    private final RestTemplate restTemplate;

    @Value("${alphavantage.api.key}")
    private String apiKey;

    @Value("${alphavantage.api.base.url}")
    private String baseUrl;

    public AlphaVantageService() {
        this.restTemplate = new RestTemplate();
    }

    @Cacheable(value = "alphaVantageDaily", key = "#symbol")
    public Map<String, Object> getDailyTimeSeries(String symbol) {
        try {
            logger.info("Fetching Alpha Vantage daily data for symbol: {}", symbol);
            
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("function", "TIME_SERIES_DAILY")
                    .queryParam("symbol", symbol)
                    .queryParam("outputsize", "compact") // Last 100 data points
                    .queryParam("apikey", apiKey)
                    .toUriString();

            logger.info("Alpha Vantage API URL: {}", url.replace(apiKey, "***"));
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response == null) {
                logger.warn("No response from Alpha Vantage for symbol: {}", symbol);
                return new HashMap<>();
            }
            
            // Check for API error messages
            if (response.containsKey("Error Message")) {
                logger.error("Alpha Vantage API error for {}: {}", symbol, response.get("Error Message"));
                return new HashMap<>();
            }
            
            if (response.containsKey("Note")) {
                logger.warn("Alpha Vantage API limit warning for {}: {}", symbol, response.get("Note"));
                return new HashMap<>();
            }
            
            // Check if we have the expected data structure
            Map<String, Object> timeSeries = (Map<String, Object>) response.get("Time Series (Daily)");
            if (timeSeries == null || timeSeries.isEmpty()) {
                logger.warn("No time series data found for symbol: {}", symbol);
                return new HashMap<>();
            }
            
            logger.info("Successfully fetched {} days of data for {}", timeSeries.size(), symbol);
            return response;
            
        } catch (Exception e) {
            logger.error("Error fetching Alpha Vantage data for symbol {}: {}", symbol, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    public Map<String, Object> getProcessedCandleData(String symbol, String period) {
        try {
            Map<String, Object> rawData = getDailyTimeSeries(symbol);
            
            if (rawData.isEmpty()) {
                return new HashMap<>();
            }
            
            Map<String, Object> timeSeries = (Map<String, Object>) rawData.get("Time Series (Daily)");
            if (timeSeries == null) {
                return new HashMap<>();
            }
            
            // Convert to our expected format (similar to Finnhub)
            List<Double> opens = new ArrayList<>();
            List<Double> highs = new ArrayList<>();
            List<Double> lows = new ArrayList<>();
            List<Double> closes = new ArrayList<>();
            List<Long> volumes = new ArrayList<>();
            List<Long> timestamps = new ArrayList<>();
            
            // Sort dates to ensure chronological order
            List<String> sortedDates = new ArrayList<>(timeSeries.keySet());
            sortedDates.sort(String::compareTo);
            
            // Limit data based on period
            int maxDays = getMaxDaysForPeriod(period);
            if (sortedDates.size() > maxDays) {
                sortedDates = sortedDates.subList(sortedDates.size() - maxDays, sortedDates.size());
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            for (String date : sortedDates) {
                Map<String, String> dayData = (Map<String, String>) timeSeries.get(date);
                
                try {
                    opens.add(Double.parseDouble(dayData.get("1. open")));
                    highs.add(Double.parseDouble(dayData.get("2. high")));
                    lows.add(Double.parseDouble(dayData.get("3. low")));
                    closes.add(Double.parseDouble(dayData.get("4. close")));
                    volumes.add(Long.parseLong(dayData.get("5. volume")));
                    
                    // Convert date to timestamp
                    LocalDate localDate = LocalDate.parse(date, formatter);
                    long timestamp = localDate.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC);
                    timestamps.add(timestamp);
                    
                } catch (NumberFormatException e) {
                    logger.warn("Error parsing data for date {}: {}", date, e.getMessage());
                }
            }
            
            // Create response in Finnhub-compatible format
            Map<String, Object> processedData = new HashMap<>();
            processedData.put("c", closes.toArray(new Double[0]));
            processedData.put("h", highs.toArray(new Double[0]));
            processedData.put("l", lows.toArray(new Double[0]));
            processedData.put("o", opens.toArray(new Double[0]));
            processedData.put("v", volumes.toArray(new Long[0]));
            processedData.put("t", timestamps.toArray(new Long[0]));
            processedData.put("s", "ok");
            
            logger.info("Processed {} data points for symbol: {}", closes.size(), symbol);
            return processedData;
            
        } catch (Exception e) {
            logger.error("Error processing Alpha Vantage data for symbol {}: {}", symbol, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    private int getMaxDaysForPeriod(String period) {
        switch (period.toUpperCase()) {
            case "1D":
                return 1;
            case "1W":
                return 7;
            case "1M":
                return 30;
            case "3M":
                return 90;
            default:
                return 30;
        }
    }
}