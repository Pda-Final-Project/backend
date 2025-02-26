package finpago.executionservice.execution.repository;

import finpago.executionservice.execution.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TradeRepository extends JpaRepository<Trade, UUID> {
    List<Trade> findByBuyerUserIdOrSellerUserId(Long buyerUserId, Long sellerUserId);
}
