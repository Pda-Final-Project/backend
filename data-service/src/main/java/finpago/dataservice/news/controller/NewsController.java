package finpago.dataservice.news.controller;

import finpago.dataservice.news.entity.News;
import finpago.dataservice.news.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("v1/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    // 최신 뉴스 조회 API
    @GetMapping
    public ResponseEntity<List<News>> getLatestNews() {
        List<News> news = newsService.getNews();
        return ResponseEntity.ok(news);
    }
}

