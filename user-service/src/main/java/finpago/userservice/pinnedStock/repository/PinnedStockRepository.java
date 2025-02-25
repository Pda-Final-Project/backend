package finpago.userservice.pinnedStock.repository;

import finpago.userservice.pinnedStock.entity.PinnedStock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PinnedStockRepository extends JpaRepository<PinnedStock, String> {
    Optional<PinnedStock> findByUserIdAndStockTicker(String userId, String stockTicker);
}
