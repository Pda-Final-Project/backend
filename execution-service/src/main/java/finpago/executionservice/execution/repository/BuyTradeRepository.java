package finpago.executionservice.execution.repository;

import finpago.common.global.enums.TradeStatus;
import finpago.executionservice.execution.entity.BuyTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BuyTradeRepository extends JpaRepository<BuyTrade, UUID> {
    List<BuyTrade> findByBuyerUserId(Long buyerUserId);
    List<BuyTrade> findByBuyerUserIdAndTradeStatusIn(Long buyerUserId, List<TradeStatus> tradeStatus);
    List<BuyTrade> findByBuyerUserIdAndTradeStatus(Long buyerUserId, TradeStatus tradeStatus);

}
