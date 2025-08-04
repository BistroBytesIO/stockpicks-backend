package com.stockpicks.backend.repository;

import com.stockpicks.backend.entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    List<ContactMessage> findByOrderByCreatedAtDesc();
    List<ContactMessage> findByResolvedFalseOrderByCreatedAtDesc();
    List<ContactMessage> findByResolvedTrueOrderByCreatedAtDesc();
}