package finpago.fillingservice.repository;

import finpago.fillingservice.entity.Filling;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface FillingRepository extends JpaRepository<Filling, String> {
    Page<Filling> findByFillingTickerOrderBySubmitTimestampDesc(String ticker, Pageable pageable);

    Optional<Filling> findByFillingId(String fillingId);

    Page<Filling> findByFillingTickerAndFillingTypeOrderBySubmitTimestampDesc(String ticker, String fillingType, Pageable pageable);
}
