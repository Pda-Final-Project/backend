package finpago.fillingservice.repository;

import finpago.fillingservice.entity.Filling;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FillingRepository extends JpaRepository<Filling, String> {

    Optional<Filling> findByFillingId(String fillingId);

    Page<Filling> findAllByOrderBySubmitTimestampDesc(Pageable pageable);

    Page<Filling> findBySubmitTimestampBetweenOrderBySubmitTimestampDesc(
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Page<Filling> findByFillingTypeInOrderBySubmitTimestampDesc(
            List<String> fillingTypes, Pageable pageable);

    Page<Filling> findByFillingTickerOrderBySubmitTimestampDesc(
            String fillingTicker, Pageable pageable);

    Page<Filling> findByFillingTypeInAndSubmitTimestampBetweenOrderBySubmitTimestampDesc(
            List<String> fillingTypes, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Page<Filling> findByFillingTickerAndSubmitTimestampBetweenOrderBySubmitTimestampDesc(
            String fillingTicker, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Page<Filling> findByFillingTickerAndFillingTypeInOrderBySubmitTimestampDesc(
            String fillingTicker, List<String> fillingTypes, Pageable pageable);

    Page<Filling> findByFillingTickerAndFillingTypeInAndSubmitTimestampBetweenOrderBySubmitTimestampDesc(
            String fillingTicker, List<String> fillingTypes, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}

