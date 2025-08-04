package com.stockpicks.backend.repository;

import com.stockpicks.backend.entity.StockPick;
import com.stockpicks.backend.enums.PickType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockPickRepository extends JpaRepository<StockPick, Long> {
    List<StockPick> findByPickType(PickType pickType);
    List<StockPick> findBySymbol(String symbol);
    List<StockPick> findByOrderByPickDateDesc();
    
    @Query("SELECT s FROM StockPick s ORDER BY s.pickDate DESC LIMIT :limit")
    List<StockPick> findTopByOrderByPickDateDesc(@Param("limit") int limit);
    
    boolean existsBySymbolAndPickDateAndPickType(String symbol, LocalDateTime pickDate, PickType pickType);
    
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM StockPick s WHERE s.symbol = :symbol AND s.pickType = :pickType AND DATE(s.pickDate) = DATE(:pickDate)")
    boolean existsBySymbolAndPickTypeAndPickDateOnly(@Param("symbol") String symbol, @Param("pickType") PickType pickType, @Param("pickDate") LocalDateTime pickDate);
    
    @Query("SELECT s FROM StockPick s WHERE s.pickDate >= :startDate AND s.pickDate <= :endDate ORDER BY s.pickDate DESC")
    List<StockPick> findByPickDateBetweenOrderByPickDateDesc(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}