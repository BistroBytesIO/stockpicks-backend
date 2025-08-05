package com.stockpicks.backend.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MarketDataService {

    public Map<String, List<MarketItem>> getMarketCategories() {
        Map<String, List<MarketItem>> categories = new HashMap<>();
        
        categories.put("US", getUSStocks());
        categories.put("Commodities", getCommodities());
        categories.put("Futures", getFutures());
        categories.put("Treasuries", getTreasuries());
        
        return categories;
    }
    
    private List<MarketItem> getUSStocks() {
        List<MarketItem> stocks = new ArrayList<>();
        stocks.add(new MarketItem("S&P 500", "^GSPC", "5,870.62", "+15.87", "+0.27%"));
        stocks.add(new MarketItem("Dow Jones", "^DJI", "43,729.34", "+123.74", "+0.28%"));
        stocks.add(new MarketItem("Nasdaq", "^IXIC", "18,983.46", "+119.46", "+0.63%"));
        stocks.add(new MarketItem("Russell 2000", "^RUT", "2,315.09", "+8.91", "+0.39%"));
        stocks.add(new MarketItem("VIX", "^VIX", "14.28", "-0.42", "-2.86%"));
        stocks.add(new MarketItem("10-Yr Bond", "^TNX", "4.249", "+0.023", "+0.54%"));
        return stocks;
    }
    
    private List<MarketItem> getCommodities() {
        List<MarketItem> commodities = new ArrayList<>();
        commodities.add(new MarketItem("Gold", "GC=F", "2,657.10", "+4.50", "+0.17%"));
        commodities.add(new MarketItem("Silver", "SI=F", "30.89", "+0.12", "+0.39%"));
        commodities.add(new MarketItem("Crude Oil", "CL=F", "68.12", "-0.45", "-0.66%"));
        commodities.add(new MarketItem("Natural Gas", "NG=F", "3.142", "+0.089", "+2.92%"));
        commodities.add(new MarketItem("Copper", "HG=F", "4.1425", "+0.0175", "+0.42%"));
        commodities.add(new MarketItem("Platinum", "PL=F", "968.30", "+2.30", "+0.24%"));
        return commodities;
    }
    
    private List<MarketItem> getFutures() {
        List<MarketItem> futures = new ArrayList<>();
        futures.add(new MarketItem("Dow Futures", "YM=F", "44,247.00", "+74.00", "+0.17%"));
        futures.add(new MarketItem("S&P Futures", "ES=F", "5,901.25", "+11.75", "+0.20%"));
        futures.add(new MarketItem("Nasdaq Futures", "NQ=F", "20,456.50", "+46.25", "+0.23%"));
        futures.add(new MarketItem("Russell Futures", "RTY=F", "2,334.80", "+8.90", "+0.38%"));
        futures.add(new MarketItem("Bitcoin", "BTC-USD", "97,435.21", "+1,234.56", "+1.28%"));
        futures.add(new MarketItem("Ethereum", "ETH-USD", "3,421.78", "+87.45", "+2.62%"));
        return futures;
    }
    
    private List<MarketItem> getTreasuries() {
        List<MarketItem> treasuries = new ArrayList<>();
        treasuries.add(new MarketItem("3-Month", "^IRX", "4.225", "+0.012", "+0.28%"));
        treasuries.add(new MarketItem("6-Month", "^FVX", "4.187", "+0.008", "+0.19%"));
        treasuries.add(new MarketItem("2-Year", "^SML", "4.156", "+0.018", "+0.43%"));
        treasuries.add(new MarketItem("5-Year", "^FVX", "4.098", "+0.021", "+0.51%"));
        treasuries.add(new MarketItem("10-Year", "^TNX", "4.249", "+0.023", "+0.54%"));
        treasuries.add(new MarketItem("30-Year", "^TYX", "4.421", "+0.019", "+0.43%"));
        return treasuries;
    }

    public static class MarketItem {
        private String name;
        private String symbol;
        private String price;
        private String change;
        private String changePercent;

        public MarketItem(String name, String symbol, String price, String change, String changePercent) {
            this.name = name;
            this.symbol = symbol;
            this.price = price;
            this.change = change;
            this.changePercent = changePercent;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public String getPrice() { return price; }
        public void setPrice(String price) { this.price = price; }

        public String getChange() { return change; }
        public void setChange(String change) { this.change = change; }

        public String getChangePercent() { return changePercent; }
        public void setChangePercent(String changePercent) { this.changePercent = changePercent; }
    }
}