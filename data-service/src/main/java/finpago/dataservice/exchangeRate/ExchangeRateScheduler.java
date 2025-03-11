package finpago.dataservice.exchangeRate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRateScheduler {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateScheduler(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @Scheduled(fixedRate = 60000) // 60초마다 실행
    public void updateExchangeRates() {
        String[] tickers = {
                "AAPL", "MSFT", "NVDA", "GOOGL", "AMZN", "META", "TSLA", "LLY", "AVGO", "JPM",
                "IBM", "MA", "ORCL", "COST", "UNH", "NFLX", "AMD", "TSM", "ADBE", "CRM",
                "NOW", "INTC", "PLTR", "ASML", "V", "XOM", "KO", "WMT", "LIN", "MS"
        };
        // 나중에 동적으로 변경 가능
        exchangeRateService.getExchangeRate(tickers);
    }
}