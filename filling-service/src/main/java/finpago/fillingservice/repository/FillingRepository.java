package finpago.fillingservice.repository;

import finpago.fillingservice.entity.Filling;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FillingRepository extends JpaRepository<Filling, String> {
    Optional<Filling> findByFillingId(String fillingId);

    Page<Filling> findAllByOrderBySubmitTimestampDesc(Pageable pageable);

    Page<Filling> findBySubmitTimestampBetweenOrderBySubmitTimestampDesc(String startDate, String endDate, Pageable pageable);

    Page<Filling> findByFillingTypeOrderBySubmitTimestampDesc(String fillingType, Pageable pageable);

    Page<Filling> findByFillingTickerOrderBySubmitTimestampDesc(String ticker, Pageable pageable);

    Page<Filling> findByFillingTypeAndSubmitTimestampBetweenOrderBySubmitTimestampDesc(String fillingType, String startDate, String endDate, Pageable pageable);

    Page<Filling> findByFillingTickerAndSubmitTimestampBetweenOrderBySubmitTimestampDesc(String ticker, String startDate, String endDate, Pageable pageable);

    Page<Filling> findByFillingTickerAndFillingTypeOrderBySubmitTimestampDesc(String ticker, String fillingType, Pageable pageable);

    Page<Filling> findByFillingTickerAndFillingTypeAndSubmitTimestampBetweenOrderBySubmitTimestampDesc(String ticker, String fillingType, String startDate, String endDate, Pageable pageable);
}
