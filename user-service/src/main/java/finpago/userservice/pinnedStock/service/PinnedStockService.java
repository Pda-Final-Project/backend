package finpago.userservice.pinnedStock.service;

import finpago.userservice.common.global.exception.error.DuplicatePinnedStockException;
import finpago.userservice.common.global.response.ResponseCode;
import finpago.userservice.pinnedStock.entity.PinnedStock;
import finpago.userservice.pinnedStock.repository.PinnedStockRepository;
import finpago.userservice.user.entity.User;
import finpago.userservice.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PinnedStockService {

    private final PinnedStockRepository pinnedStockRepository;
    private final UserRepository userRepository;

    @Transactional
    public void addPinnedStock(Long userId, String stockTicker) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(ResponseCode.INVALID_TOKEN.getMessage()));

        Optional<PinnedStock> existingPinnedStock = pinnedStockRepository.findByUserAndStockTicker(user, stockTicker);
        if (existingPinnedStock.isPresent()) {
            throw new DuplicatePinnedStockException(ResponseCode.PINNED_STOCK_ALREADY_EXISTS.getMessage());
        }

        PinnedStock pinnedStock = PinnedStock.builder()
                .user(user)
                .stockTicker(stockTicker)
                .build();

        pinnedStockRepository.save(pinnedStock);
    }
}
