package finpago.userservice.pinnedStock.repository;

import finpago.userservice.pinnedStock.entity.PinnedStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import finpago.userservice.user.entity.User;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PinnedStockRepository extends JpaRepository<PinnedStock, UUID> {

    Optional<PinnedStock> findByUserAndStockTicker(User user, String stockTicker);

}

