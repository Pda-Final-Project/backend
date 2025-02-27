package finpago.dataservice.news.controller;

import finpago.common.global.common.ApiResponse;
import finpago.dataservice.news.entity.News;
import finpago.dataservice.news.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("v1/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    // 최신 뉴스 조회 API
    @GetMapping
    public ResponseEntity<ApiResponse<List<News>>> getLatestNews() {
        List<News> news = newsService.getNews();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK, "해외 경제 뉴스 조회 성공", news));
    }
}

