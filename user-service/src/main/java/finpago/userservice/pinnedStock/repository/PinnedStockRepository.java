package finpago.userservice.pinnedStock.repository;

import finpago.userservice.pinnedStock.entity.PinnedStock;
import finpago.userservice.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PinnedStockRepository extends JpaRepository<PinnedStock, String> {
    Optional<PinnedStock> findByUserAndStockTicker(User user, String stockTicker);
}
