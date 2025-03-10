package finpago.executionservice.execution.service;

import finpago.common.global.enums.TradeStatus;
import finpago.executionservice.execution.dto.TradeDto;
import finpago.executionservice.execution.entity.BuyTrade;
import finpago.executionservice.execution.entity.SellTrade;
import finpago.executionservice.execution.repository.BuyTradeRepository;
import finpago.executionservice.execution.repository.SellTradeRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 체결내역 조회 비즈니스 로직
@Service
@RequiredArgsConstructor
public class TradeViewService {

    private final BuyTradeRepository buyTradeRepository;
    private final SellTradeRepository sellTradeRepository;

    @Transactional(readOnly = true)
    public List<TradeDto> getSuccessfulOrPendingTrades(Long userId) {
        System.out.println("또뭐가문제냐");
        List<TradeDto> buyTrades = buyTradeRepository.findByBuyerUserIdAndTradeStatusIn(userId, List.of(TradeStatus.SUCCESS, TradeStatus.PENDING))
                .stream()
                .map(this::convertToTradeDto)
                .collect(Collectors.toList());

        System.out.println("buyTrades: " + buyTrades);
        List<TradeDto> sellTrades = sellTradeRepository.findBySellerUserIdAndTradeStatusIn(userId, List.of(TradeStatus.SUCCESS, TradeStatus.PENDING))
                .stream()
                .map(this::convertToTradeDto)
                .collect(Collectors.toList());

        // 최신순 정렬
        return Stream.concat(buyTrades.stream(), sellTrades.stream())
                .sorted((t1, t2) -> t2.getTradeDate().compareTo(t1.getTradeDate()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TradeDto> getFailedTrades(Long userId) {
        List<TradeDto> buyTrades = buyTradeRepository.findByBuyerUserIdAndTradeStatus(userId, TradeStatus.FAILED)
                .stream()
                .map(this::convertToTradeDto)
                .collect(Collectors.toList());

        List<TradeDto> sellTrades = sellTradeRepository.findBySellerUserIdAndTradeStatus(userId, TradeStatus.FAILED)
                .stream()
                .map(this::convertToTradeDto)
                .collect(Collectors.toList());

        // 최신순 정렬
        return Stream.concat(buyTrades.stream(), sellTrades.stream())
                .sorted((t1, t2) -> t2.getTradeDate().compareTo(t1.getTradeDate()))
                .collect(Collectors.toList());
    }

    // 전체 체결 내역 조회 (SUCCESS, PENDING, FAILED)
    @Transactional(readOnly = true)
    public List<TradeDto> getAllTrades(Long userId) {
        List<TradeDto> buyTrades = buyTradeRepository.findByBuyerUserId(userId)
                .stream()
                .map(this::convertToTradeDto)
                .collect(Collectors.toList());

        List<TradeDto> sellTrades = sellTradeRepository.findBySellerUserId(userId)
                .stream()
                .map(this::convertToTradeDto)
                .collect(Collectors.toList());

        // 최신순 정렬
        return Stream.concat(buyTrades.stream(), sellTrades.stream())
                .sorted((t1, t2) -> t2.getTradeDate().compareTo(t1.getTradeDate()))
                .collect(Collectors.toList());
    }

    private TradeDto convertToTradeDto(BuyTrade trade) {
        return new TradeDto(
                trade.getTradeTicker(),
                "현금매수", // BUY 거래
                trade.getBuyerOfferPrice(),
                trade.getBuyerOrderQuantity(),
                trade.getTradePrice(),
                trade.getTradeQuantity(),
                trade.getUnfilledQuantity(),
                trade.getTradeStatus(),
                trade.getBuyTradeNumber(),
                trade.getTradeDate()
        );
    }

    private TradeDto convertToTradeDto(SellTrade trade) {
        return new TradeDto(
                trade.getTradeTicker(),
                "현금매도", // SELL 거래
                trade.getSellerOfferPrice(),
                trade.getSellerOrderQuantity(),
                trade.getTradePrice(),
                trade.getTradeQuantity(),
                trade.getUnfilledQuantity(),
                trade.getTradeStatus(),
                trade.getSellTradeNumber(),
                trade.getTradeDate()
        );
    }
}