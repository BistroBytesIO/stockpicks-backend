package com.stockpicks.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "google_sheets_sync")
@Data
@NoArgsConstructor
public class GoogleSheetsSync {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String spreadsheetId;

    private String sheetName;

    private String lastSyncRange;

    @CreationTimestamp
    private LocalDateTime syncDate;

    private Integer processedRecords = 0;

    private String status = "PENDING";

    private String errorMessage;

    private LocalDateTime lastSyncTime;

    private Integer rowsProcessed;
}