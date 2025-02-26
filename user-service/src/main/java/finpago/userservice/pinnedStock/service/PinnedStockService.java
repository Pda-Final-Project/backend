package finpago.userservice.pinnedStock.service;

import finpago.userservice.pinnedStock.dto.PinnedStockReqDto;
import finpago.userservice.pinnedStock.entity.PinnedStock;
import finpago.userservice.pinnedStock.repository.PinnedStockRepository;
import finpago.userservice.user.entity.User;
import finpago.userservice.user.repository.UserRepository;
import finpago.userservice.user.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PinnedStockService {

    private final PinnedStockRepository pinnedStockRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public PinnedStock addPinnedStock(HttpServletRequest request, PinnedStockReqDto requestDTO) {
        String userPhone = jwtUtil.extractUserPhone(getTokenFromRequest(request));
        User user = userRepository.findByUserPhone(userPhone)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 유저입니다."));

        PinnedStock pinnedStock = PinnedStock.builder()
                .user(user)
                .stockTicker(requestDTO.getStockTicker())
                .creationTimestamp(LocalDateTime.now())
                .updateTimestamp(LocalDateTime.now())
                .build();

        return pinnedStockRepository.save(pinnedStock);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new IllegalArgumentException("유효한 JWT 토큰이 필요합니다.");
    }
}
