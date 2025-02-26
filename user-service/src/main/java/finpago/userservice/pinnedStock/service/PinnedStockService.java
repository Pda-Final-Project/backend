package finpago.userservice.pinnedStock.service;

import finpago.userservice.pinnedStock.entity.PinnedStock;
import finpago.userservice.pinnedStock.repository.PinnedStockRepository;
import finpago.userservice.user.entity.User;
import finpago.userservice.user.repository.UserRepository;
import finpago.userservice.user.util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PinnedStockService {

    private final PinnedStockRepository pinnedStockRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public void addPinnedStock(String token, String stockTicker) {
        Long userId = jwtUtil.extractUserId(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 유저입니다."));

        Optional<PinnedStock> existingPinnedStock = pinnedStockRepository.findByUserAndStockTicker(user, stockTicker);

        if (existingPinnedStock.isPresent()) {
            throw new IllegalArgumentException("이미 등록된 관심 종목입니다.");
        }

        PinnedStock pinnedStock = PinnedStock.builder()
                .pinnedStockId(UUID.fromString(UUID.randomUUID().toString()))
                .user(user)
                .stockTicker(stockTicker)
                .build();

        pinnedStockRepository.save(pinnedStock);
    }
}
