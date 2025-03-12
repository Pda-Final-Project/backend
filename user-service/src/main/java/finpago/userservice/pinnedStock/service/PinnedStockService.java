package finpago.userservice.pinnedStock.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import finpago.common.global.common.ApiResponse;
import finpago.userservice.pinnedStock.dto.PinnedStockResDto;
import finpago.userservice.pinnedStock.dto.StockInfo;
import finpago.userservice.pinnedStock.entity.PinnedStock;
import finpago.userservice.pinnedStock.repository.PinnedStockRepository;
import finpago.userservice.user.entity.User;
import finpago.userservice.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PinnedStockService {

    private final PinnedStockRepository pinnedStockRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 관심 종목 추가 기능
     */
    @Transactional
    public ApiResponse<String> addPinnedStock(Long userId, String stockTicker) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 유저입니다."));

        Optional<PinnedStock> existingPinnedStock = pinnedStockRepository.findByUserAndStockTicker(user, stockTicker);
        if (existingPinnedStock.isPresent()) {
            return ApiResponse.fail(HttpStatus.CONFLICT, "이미 등록된 관심 종목입니다.");
        }

        PinnedStock pinnedStock = PinnedStock.builder()
                .user(user)
                .stockTicker(stockTicker)
                .build();

        pinnedStockRepository.save(pinnedStock);
        return ApiResponse.success(HttpStatus.CREATED, "관심 종목 추가 완료", "");
    }

    /**
     * 관심 종목 조회 기능 (Redis 현재가 & 변동률 불러오기)
     */
    @Transactional
    public ApiResponse<List<PinnedStockResDto>> getPinnedStocks(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 유저입니다."));

        List<PinnedStock> pinnedStocks = pinnedStockRepository.findByUser(user);

        // 관심 종목 리스트 조회
        List<PinnedStockResDto> pinnedStockList = pinnedStocks.stream()
                .map(stock -> {
                    String ticker = stock.getStockTicker();
                    String redisKey = "stock:" + ticker; // Redis에서 해당 종목 조회

                    Map<Object, Object> redisData = redisTemplate.opsForHash().entries(redisKey);

                    if (redisData.isEmpty()) {
                        log.warn("🚨 Redis에서 종목 데이터 없음: {}", ticker);
                        return new PinnedStockResDto(
                                ticker,
                                "정보 없음",  // 종목명 없음
                                0.0,        // 현재가 없음
                                0.0         // 변동률 없음
                        );
                    }

                    try {
                        String priceStr = redisData.getOrDefault("current_price", "0").toString();
                        String rateStr = redisData.getOrDefault("change_rate", "0").toString();
                        String name = redisData.getOrDefault("name", "정보 없음").toString();

                        double currentPrice = Double.parseDouble(priceStr);
                        double changeRate = Double.parseDouble(rateStr);

                        return new PinnedStockResDto(ticker, name, currentPrice, changeRate);
                    } catch (Exception e) {
                        log.error("🚨 Redis 데이터 파싱 오류 ({}): {}", ticker, e.getMessage());
                        return new PinnedStockResDto(
                                ticker,
                                "정보 없음",
                                0.0,
                                0.0
                        );
                    }
                })
                .collect(Collectors.toList());

        return ApiResponse.success(HttpStatus.OK, "관심 종목 조회 완료", pinnedStockList);
    }

    /**
     * 관심 종목 삭제
     */
    @Transactional
    public boolean deletePinnedStock(Long userId, String stockTicker) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 유저입니다."));

        Optional<PinnedStock> pinnedStock = pinnedStockRepository.findByUserAndStockTicker(user, stockTicker);

        if (pinnedStock.isPresent()) {
            pinnedStockRepository.delete(pinnedStock.get());
            return true;
        } else {
            return false;
        }
    }
}
