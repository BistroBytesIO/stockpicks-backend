package com.stockpicks.backend.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.stockpicks.backend.entity.GoogleSheetsSync;
import com.stockpicks.backend.entity.StockPick;
import com.stockpicks.backend.enums.PickType;
import com.stockpicks.backend.repository.GoogleSheetsSyncRepository;
import com.stockpicks.backend.repository.StockPickRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleSheetsService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsService.class);
    
    private static final String APPLICATION_NAME = "Stock Picks Application";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "/service-account-key.json";

    @Value("${google.sheets.spreadsheet.id}")
    private String spreadsheetId;

    @Value("${google.sheets.range:Sheet1!A:H}")
    private String defaultRange;
    
    @Value("${google.sheets.sync.enabled:true}")
    private boolean syncEnabled;
    
    @Value("${google.sheets.sync.business.hours.enabled:true}")
    private boolean businessHoursEnabled;
    
    @Value("${google.sheets.sync.business.hours.start:9}")
    private int businessHoursStart;
    
    @Value("${google.sheets.sync.business.hours.end:18}")
    private int businessHoursEnd;

    @Value("${google.service.account.json:}")
    private String serviceAccountJson;

    @Autowired
    private StockPickRepository stockPickRepository;

    @Autowired
    private GoogleSheetsSyncRepository googleSheetsSyncRepository;

    private GoogleCredentials getCredentials() throws IOException {
        // First try to load from environment variable (for production)
        if (serviceAccountJson != null && !serviceAccountJson.isEmpty()) {
            logger.info("Loading Google Service Account credentials from environment variable");
            return GoogleCredentials.fromStream(
                    new java.io.ByteArrayInputStream(serviceAccountJson.getBytes())
            ).createScoped(SCOPES);
        }

        // Fallback to resource file (for local development)
        logger.info("Loading Google Service Account credentials from resource file");
        InputStream in = GoogleSheetsService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH + ". Please add your Google Service Account JSON file to src/main/resources/ or set GOOGLE_SERVICE_ACCOUNT_JSON environment variable");
        }
        return GoogleCredentials.fromStream(in).createScoped(SCOPES);
    }

    public int syncStockPicks() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        
        GoogleCredentials credentials = getCredentials();
        
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, 
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();

        GoogleSheetsSync lastSync = googleSheetsSyncRepository.findTopByOrderByLastSyncTimeDesc();
        String range = lastSync != null ? lastSync.getLastSyncRange() : defaultRange;

        ValueRange response = service.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        List<List<Object>> values = response.getValues();

        if (values == null || values.isEmpty()) {
            logger.info("No data found in Google Sheets");
            return 0;
        }

        int startRow = lastSync != null ? getStartRowFromRange(lastSync.getLastSyncRange()) + 1 : 1;
        int newPicksCount = 0;
        
        for (int i = 0; i < values.size(); i++) {
            List<Object> row = values.get(i);
            if (row.size() >= 5) { // Minimum required columns
                StockPick stockPick = createStockPickFromRow(row, startRow + i);
                if (stockPick != null && !stockPickRepository.existsBySymbolAndPickTypeAndPickDateOnly(
                        stockPick.getSymbol(), stockPick.getPickType(), stockPick.getPickDate())) {
                    stockPickRepository.save(stockPick);
                    newPicksCount++;
                }
            }
        }

        GoogleSheetsSync syncRecord = new GoogleSheetsSync();
        syncRecord.setSpreadsheetId(spreadsheetId);
        syncRecord.setLastSyncTime(LocalDateTime.now());
        syncRecord.setLastSyncRange(range);
        syncRecord.setRowsProcessed(values.size());
        googleSheetsSyncRepository.save(syncRecord);
        
        logger.info("Google Sheets sync completed. New picks added: {}", newPicksCount);
        return newPicksCount;
    }
    
    @Scheduled(fixedRateString = "#{${google.sheets.sync.interval.minutes:15} * 60 * 1000}")
    public void scheduledSyncStockPicks() {
        if (!syncEnabled) {
            logger.debug("Scheduled Google Sheets sync is disabled");
            return;
        }
        
        if (businessHoursEnabled && !isWithinBusinessHours()) {
            logger.debug("Skipping scheduled sync - outside business hours");
            return;
        }
        
        try {
            logger.info("Starting scheduled Google Sheets sync");
            int newPicks = syncStockPicks();
            logger.info("Scheduled Google Sheets sync completed successfully. New picks: {}", newPicks);
        } catch (Exception e) {
            logger.error("Error during scheduled Google Sheets sync: {}", e.getMessage(), e);
        }
    }
    
    private boolean isWithinBusinessHours() {
        LocalDateTime now = LocalDateTime.now();
        int currentHour = now.getHour();
        
        // Check if current time is within business hours
        return currentHour >= businessHoursStart && currentHour < businessHoursEnd;
    }

    private StockPick createStockPickFromRow(List<Object> row, int rowNumber) {
        try {
            StockPick stockPick = new StockPick();
            
            // Skip header row if it contains text like "Date", "Symbol", etc.
            if (row.get(0).toString().toLowerCase().contains("date") || 
                row.get(1).toString().toLowerCase().contains("symbol")) {
                return null;
            }
            
            // Assuming columns: Date, Symbol, Company Name, Pick Type, Entry Price, Commentary, Current Price, Target Price
            stockPick.setPickDate(parseDate(row.get(0).toString()));
            stockPick.setSymbol(row.get(1).toString().toUpperCase().trim());
            stockPick.setCompanyName(row.get(2).toString().trim());
            stockPick.setPickType(PickType.valueOf(row.get(3).toString().toUpperCase().trim()));
            stockPick.setEntryPrice(parsePrice(row.get(4).toString()));
            stockPick.setCommentary(row.size() > 5 ? row.get(5).toString().trim() : "");
            
            if (row.size() > 6 && !row.get(6).toString().trim().isEmpty()) {
                stockPick.setCurrentPrice(parsePrice(row.get(6).toString()));
            }
            
            if (row.size() > 7 && !row.get(7).toString().trim().isEmpty()) {
                stockPick.setTargetPrice(parsePrice(row.get(7).toString()));
            }
            
            stockPick.setCreatedAt(LocalDateTime.now());
            stockPick.setUpdatedAt(LocalDateTime.now());
            
            return stockPick;
        } catch (Exception e) {
            logger.error("Error processing row {}: {}", rowNumber, e.getMessage());
            return null;
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        try {
            // Try multiple date formats
            String cleanDate = dateStr.trim();
            String[] patterns = {
                "M/d/yyyy",
                "MM/dd/yyyy", 
                "yyyy-MM-dd",
                "M/d/yy",
                "MM/dd/yy"
            };
            
            for (String pattern : patterns) {
                try {
                    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(pattern);
                    LocalDate localDate = LocalDate.parse(cleanDate, dateFormatter);
                    return localDate.atStartOfDay(); // Convert to LocalDateTime at 00:00:00
                } catch (Exception ignored) {
                    // Try next formatter
                }
            }
            
            // If all fail, return current time
            logger.warn("Could not parse date: {}, using current time", dateStr);
            return LocalDateTime.now();
        } catch (Exception e) {
            logger.error("Error parsing date: {}, using current time", dateStr);
            return LocalDateTime.now();
        }
    }
    
    private BigDecimal parsePrice(String priceStr) {
        try {
            // Remove currency symbols and spaces
            String cleanPrice = priceStr.trim()
                .replace("$", "")
                .replace(",", "")
                .replace(" ", "");
            return new BigDecimal(cleanPrice);
        } catch (Exception e) {
            logger.error("Error parsing price: {}", priceStr);
            return BigDecimal.ZERO;
        }
    }

    private int getStartRowFromRange(String range) {
        try {
            String[] parts = range.split("!");
            if (parts.length > 1) {
                String cellRange = parts[1];
                String[] rangeParts = cellRange.split(":");
                if (rangeParts.length > 0) {
                    String startCell = rangeParts[0];
                    String rowNumber = startCell.replaceAll("[^0-9]", "");
                    return rowNumber.isEmpty() ? 1 : Integer.parseInt(rowNumber);
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing range: {}", range);
        }
        return 1;
    }

    public List<StockPick> getRecentStockPicks(int limit) {
        return stockPickRepository.findTopByOrderByPickDateDesc(limit);
    }
    
    public GoogleSheetsSync getLastSyncStatus() {
        return googleSheetsSyncRepository.findTopByOrderByLastSyncTimeDesc();
    }
    
    public boolean isSyncEnabled() {
        return syncEnabled;
    }
    
    public boolean isBusinessHoursEnabled() {
        return businessHoursEnabled;
    }
    
    public String getBusinessHours() {
        return businessHoursStart + ":00 - " + businessHoursEnd + ":00";
    }
}