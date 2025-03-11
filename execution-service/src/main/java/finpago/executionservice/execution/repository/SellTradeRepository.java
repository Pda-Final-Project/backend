package finpago.executionservice.execution.repository;

import finpago.common.global.enums.TradeStatus;
import finpago.executionservice.execution.entity.SellTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SellTradeRepository extends JpaRepository<SellTrade, Long> {
    List<SellTrade> findBySellerUserId(Long sellerUserId);
    List<SellTrade> findBySellerUserIdAndTradeStatusIn(Long sellerUserId, List<TradeStatus> tradeStatus);
    List<SellTrade> findBySellerUserIdAndTradeStatus(Long sellerUserId, TradeStatus tradeStatus);
}