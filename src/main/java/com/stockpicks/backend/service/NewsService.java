package com.stockpicks.backend.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NewsService {

    private final RestTemplate restTemplate;
    private final XmlMapper xmlMapper;

    public NewsService() {
        this.restTemplate = new RestTemplate();
        this.xmlMapper = new XmlMapper();
        
        // Add User-Agent header to avoid blocking
        this.restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            return execution.execute(request, body);
        });
    }

    public List<NewsItem> getTopStories() {
        return fetchNewsFromRSS("https://finance.yahoo.com/news/rssindex", "Top Stories");
    }

    public List<NewsItem> getBonds() {
        return fetchNewsFromRSS("https://feeds.finance.yahoo.com/rss/2.0/category-bonds", "Bonds");
    }

    public List<NewsItem> getCurrencies() {
        return fetchNewsFromRSS("https://feeds.finance.yahoo.com/rss/2.0/category-currencies", "Currencies");
    }

    public List<NewsItem> getPersonalFinance() {
        return fetchNewsFromRSS("https://feeds.finance.yahoo.com/rss/2.0/category-personal-finance", "Personal Finance");
    }

    public List<NewsItem> getStockMarket() {
        return fetchNewsFromRSS("https://feeds.finance.yahoo.com/rss/2.0/category-stocks", "Stock Market");
    }

    public List<NewsItem> getEconomicNews() {
        return fetchNewsFromRSS("https://feeds.finance.yahoo.com/rss/2.0/category-economic-news", "Economic News");
    }

    public List<NewsItem> getOptionsAndFutures() {
        return fetchNewsFromRSS("https://feeds.finance.yahoo.com/rss/2.0/category-options", "Options & Futures");
    }

    private List<NewsItem> fetchNewsFromRSS(String rssUrl, String category) {
        try {
            System.out.println("Fetching RSS from: " + rssUrl);
            String rssXml = restTemplate.getForObject(rssUrl, String.class);
            if (rssXml == null || rssXml.trim().isEmpty()) {
                System.err.println("Empty or null RSS content from: " + rssUrl);
                return new ArrayList<>();
            }

            System.out.println("RSS XML length: " + rssXml.length());

            // Parse RSS XML
            Map<String, Object> rssData = xmlMapper.readValue(rssXml, Map.class);
            
            // More robust navigation through RSS structure
            Map<String, Object> rss = (Map<String, Object>) rssData.get("rss");
            if (rss == null) {
                System.err.println("No 'rss' element found in XML");
                return new ArrayList<>();
            }
            
            Map<String, Object> channel = (Map<String, Object>) rss.get("channel");
            if (channel == null) {
                System.err.println("No 'channel' element found in RSS");
                return new ArrayList<>();
            }
            
            Object itemsObj = channel.get("item");
            if (itemsObj == null) {
                System.err.println("No 'item' elements found in channel");
                return new ArrayList<>();
            }

            List<NewsItem> newsItems = new ArrayList<>();
            
            // Handle both single item and multiple items
            if (itemsObj instanceof List) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;
                System.out.println("Found " + items.size() + " news items");
                for (int i = 0; i < Math.min(10, items.size()); i++) {
                    NewsItem newsItem = createNewsItem(items.get(i), category);
                    if (newsItem != null) {
                        newsItems.add(newsItem);
                    }
                }
            } else if (itemsObj instanceof Map) {
                // Single item case
                System.out.println("Found single news item");
                NewsItem newsItem = createNewsItem((Map<String, Object>) itemsObj, category);
                if (newsItem != null) {
                    newsItems.add(newsItem);
                }
            }

            System.out.println("Successfully parsed " + newsItems.size() + " news items for " + category);
            return newsItems;
        } catch (Exception e) {
            System.err.println("Error fetching news from " + rssUrl + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private NewsItem createNewsItem(Map<String, Object> item, String category) {
        try {
            NewsItem newsItem = new NewsItem();
            
            // Extract title
            Object titleObj = item.get("title");
            newsItem.setTitle(titleObj != null ? titleObj.toString() : "No title");
            
            // Extract description
            Object descObj = item.get("description");
            newsItem.setDescription(descObj != null ? descObj.toString() : "No description");
            
            // Extract link
            Object linkObj = item.get("link");
            newsItem.setLink(linkObj != null ? linkObj.toString() : "");
            
            // Extract publication date
            Object pubDateObj = item.get("pubDate");
            newsItem.setPubDate(pubDateObj != null ? pubDateObj.toString() : "");
            
            newsItem.setCategory(category);
            
            // Only return item if it has at least a title
            return (newsItem.getTitle() != null && !newsItem.getTitle().equals("No title")) ? newsItem : null;
        } catch (Exception e) {
            System.err.println("Error creating news item: " + e.getMessage());
            return null;
        }
    }

    public static class NewsItem {
        private String title;
        private String description;
        private String link;
        private String pubDate;
        private String category;

        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getLink() { return link; }
        public void setLink(String link) { this.link = link; }

        public String getPubDate() { return pubDate; }
        public void setPubDate(String pubDate) { this.pubDate = pubDate; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }
}