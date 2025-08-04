package com.stockpicks.backend.repository;

import com.stockpicks.backend.entity.GoogleSheetsSync;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoogleSheetsSyncRepository extends JpaRepository<GoogleSheetsSync, Long> {
    GoogleSheetsSync findTopByOrderByLastSyncTimeDesc();
}