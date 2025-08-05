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
            String rssXml = restTemplate.getForObject(rssUrl, String.class);
            if (rssXml == null) {
                return new ArrayList<>();
            }

            // Parse RSS XML
            Map<String, Object> rssData = xmlMapper.readValue(rssXml, Map.class);
            Map<String, Object> channel = (Map<String, Object>) ((Map<String, Object>) rssData.get("rss")).get("channel");
            Object itemsObj = channel.get("item");

            List<NewsItem> newsItems = new ArrayList<>();
            
            if (itemsObj instanceof List) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;
                for (int i = 0; i < Math.min(5, items.size()); i++) {
                    Map<String, Object> item = items.get(i);
                    NewsItem newsItem = new NewsItem();
                    newsItem.setTitle((String) item.get("title"));
                    newsItem.setDescription((String) item.get("description"));
                    newsItem.setLink((String) item.get("link"));
                    newsItem.setPubDate((String) item.get("pubDate"));
                    newsItem.setCategory(category);
                    newsItems.add(newsItem);
                }
            }

            return newsItems;
        } catch (Exception e) {
            System.err.println("Error fetching news from " + rssUrl + ": " + e.getMessage());
            return new ArrayList<>();
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