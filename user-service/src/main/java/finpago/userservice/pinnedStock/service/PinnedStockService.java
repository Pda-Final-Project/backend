package finpago.userservice.pinnedStock.service;

import finpago.common.global.common.ApiResponse;
import finpago.userservice.pinnedStock.dto.PinnedStockResDto;
import finpago.userservice.pinnedStock.dto.StockInfo;
import finpago.userservice.pinnedStock.entity.PinnedStock;
import finpago.userservice.pinnedStock.repository.PinnedStockRepository;
import finpago.userservice.user.entity.User;
import finpago.userservice.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PinnedStockService {

    private final PinnedStockRepository pinnedStockRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 관심 종목 추가 기능
     */
    @Transactional
    public ApiResponse<String> addPinnedStock(Long userId, String stockTicker) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 유저입니다."));

        // 등록된 관심 종목인지 확인
        Optional<PinnedStock> existingPinnedStock = pinnedStockRepository.findByUserAndStockTicker(user, stockTicker);
        if (existingPinnedStock.isPresent()) {
            return ApiResponse.fail(HttpStatus.CONFLICT, "이미 등록된 관심 종목입니다.");
        }

        // 관심 종목 등록
        PinnedStock pinnedStock = PinnedStock.builder()
                .user(user)
                .stockTicker(stockTicker)
                .build();

        pinnedStockRepository.save(pinnedStock);
        return ApiResponse.success(HttpStatus.CREATED, "관심 종목 추가 완료", "");
    }

    /**
     * 관심 종목 조회 기능
     */
    @Transactional
    public ApiResponse<List<PinnedStockResDto>> getPinnedStocks(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 유저입니다."));

        // 관심 종목 리스트
        List<PinnedStock> pinnedStocks = pinnedStockRepository.findByUser(user);

        // Redis에서 티커 가져오기
        List<PinnedStockResDto> pinnedStockList = pinnedStocks.stream()
                .map(stock -> {
                    String ticker = stock.getStockTicker();
                    String redisKey = "stock:" + ticker;
                    StockInfo stockInfo = (StockInfo) redisTemplate.opsForValue().get(redisKey);

                    if (stockInfo == null) {
                        return new PinnedStockResDto(ticker, "정보 없음", 0, 0);
                    }
                    return new PinnedStockResDto(
                            ticker,
                            stockInfo.getName(),
                            stockInfo.getPrice(),
                            stockInfo.getChange()
                    );
                })
                .collect(Collectors.toList());

        return ApiResponse.success(HttpStatus.OK, "관심 종목 조회 완료", pinnedStockList);
    }
}
