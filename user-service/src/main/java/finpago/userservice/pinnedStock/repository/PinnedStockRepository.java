package finpago.userservice.pinnedStock.repository;

import finpago.userservice.pinnedStock.entity.PinnedStock;
import finpago.userservice.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PinnedStockRepository extends JpaRepository<PinnedStock, String> {

    // 특정 관심 종목 조회
    Optional<PinnedStock> findByUserAndStockTicker(User user, String stockTicker);

    // 모든 관심 종목 조회
    List<PinnedStock> findByUser(User user);
}
