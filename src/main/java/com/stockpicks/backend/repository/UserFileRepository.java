package com.stockpicks.backend.repository;

import com.stockpicks.backend.entity.UserFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFileRepository extends JpaRepository<UserFile, Long> {
    
    @Query("SELECT f FROM UserFile f WHERE f.isActive = true ORDER BY f.uploadedAt DESC")
    List<UserFile> findAllActiveFiles();
    
    @Query("SELECT f FROM UserFile f WHERE f.isActive = true AND f.fileType = ?1 ORDER BY f.uploadedAt DESC")
    List<UserFile> findActiveFilesByType(String fileType);
    
    @Query("SELECT f FROM UserFile f WHERE f.isActive = true AND f.uploadedBy = ?1 ORDER BY f.uploadedAt DESC")
    List<UserFile> findActiveFilesByUploader(String uploaderEmail);
}