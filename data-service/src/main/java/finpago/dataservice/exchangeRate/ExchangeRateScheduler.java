package finpago.dataservice.exchangeRate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import finpago.dataservice.exchangeRate.ExchangeRateService;

import java.time.LocalDateTime;

@Component
public class ExchangeRateScheduler {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateScheduler(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @Scheduled(fixedRate = 60000) // 60초마다 실행
    public void updateExchangeRates() {
        String[] tickers = {"AAPL", "GOOGL", "TSLA"}; // 나중에 동적으로 변경 가능

        for (String ticker : tickers) {
            Double rate = exchangeRateService.getExchangeRate(ticker);
            if (rate != null) {
                System.out.println("[ " + LocalDateTime.now() + " ] " + ticker + " 환율 업데이트: 1 USD = " + rate + " KRW");
            } else {
                System.out.println("[" + ticker + "] 환율 데이터를 가져오지 못했습니다.");
            }
        }
    }
}
