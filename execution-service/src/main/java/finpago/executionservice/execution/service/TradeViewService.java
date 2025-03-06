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

//체결내역 조회 비즈니스 로직
@Service
@RequiredArgsConstructor
public class TradeViewService {

    private final BuyTradeRepository buyTradeRepository;
    private final SellTradeRepository sellTradeRepository;

    @Transactional(readOnly = true)
    public List<TradeDto> getSuccessfulOrPendingTrades(Long userId) {
        List<TradeDto> buyTrades = buyTradeRepository.findByBuyerUserIdAndTradeStatusIn(userId, List.of(TradeStatus.SUCCESS, TradeStatus.PENDING))
                .stream()
                .map(this::convertToTradeDto)
                .collect(Collectors.toList());

        List<TradeDto> sellTrades = sellTradeRepository.findBySellerUserIdAndTradeStatusIn(userId, List.of(TradeStatus.SUCCESS, TradeStatus.PENDING))
                .stream()
                .map(this::convertToTradeDto)
                .collect(Collectors.toList());

        buyTrades.addAll(sellTrades);
        return buyTrades;
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

        buyTrades.addAll(sellTrades);
        return buyTrades;
    }

    private TradeDto convertToTradeDto(BuyTrade trade) {
        return new TradeDto(
                trade.getTradeTicker(),
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
