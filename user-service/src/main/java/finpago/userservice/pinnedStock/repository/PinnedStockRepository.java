package finpago.userservice.pinnedStock.repository;

import finpago.userservice.pinnedStock.entity.PinnedStock;
import finpago.userservice.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PinnedStockRepository extends JpaRepository<PinnedStock, UUID> {
    List<PinnedStock> findByUser(User user);
}
