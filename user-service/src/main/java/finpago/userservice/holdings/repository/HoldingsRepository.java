package finpago.userservice.holdings.repository;

import finpago.userservice.holdings.entity.Holdings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoldingsRepository extends JpaRepository<Holdings, Long> {

    // 특정 사용자가 보유한 특정 종목 정보 조회
    Optional<Holdings> findByUserIdAndStockTicker(Long userId, String stockTicker);

    // 특정 사용자의 모든 보유 주식 조회
    List<Holdings> findAllByUserId(Long userId);

    // 보유 주식 수량 및 총 가격 업데이트
    @Modifying
    @Transactional
    @Query("UPDATE Holdings h SET h.holdingQuantity = :newQuantity, h.holdingTotalPrice = :totalPrice WHERE h.userId = :userId AND h.stockTicker = :stockTicker")
    void updateHoldings(Long userId, String stockTicker, Long newQuantity, Long totalPrice);
}
