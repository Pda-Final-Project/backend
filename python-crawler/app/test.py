import json
import requests
from redis_config import redis_client

# ✅ Daum 금융 해외 뉴스 API URL
NEWS_API_URL = "https://finance.daum.net/content/news?keyword=&category=global_economy&perPage=50&searchType=&page=1"

# ✅ 요청 헤더 설정
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36",
    "Accept-Language": "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
    "Referer": "https://finance.daum.net/"
}

def crawl_daum_finance_news():
    """📢 Daum 금융 해외 뉴스 크롤링 (JSON API 사용)"""
    response = requests.get(NEWS_API_URL, headers=HEADERS)

    if response.status_code != 200:
        print(f"❌ 요청 실패 (상태 코드: {response.status_code})")
        return []

    try:
        news_data = response.json()  # JSON 데이터 파싱
        articles = news_data.get("data", [])  # "data" 키에서 뉴스 리스트 추출
        print(f"🔍 찾은 기사 수: {len(articles)}")

        news_list = []
        for article in articles:
            title = article.get("title")
            summary = article.get("summary")
            news_url = f"https://v.daum.net/v/{article.get('newsId')}"
            news_date = article.get("createdAt")
            company = article.get("cpKorName")
            img = article.get("imageUrl")

            # ✅ 이미지가 없는 뉴스는 제외
            if not img:
                continue

            news = {
                "news_title": title,
                "news_company": company,
                "news_summary": summary,
                "news_img": img,
                "news_url": news_url,
                "news_date": news_date
            }
            news_list.append(news)

            # ✅ 최대 10개만 저장
            if len(news_list) == 10:
                break

        return news_list

    except json.JSONDecodeError as e:
        print(f"❌ JSON 디코딩 오류: {e}")
        return []

# ✅ Redis에 저장
def store_news_in_redis(news_list):
    """📦 Redis에 최신 뉴스 저장"""
    redis_client.delete("investing:news")  # 기존 데이터 삭제
    for news in news_list:
        news_json = json.dumps(news, ensure_ascii=False)
        redis_client.rpush("investing:news", news_json)
        print(f"✅ 저장 완료: {news['news_title']}")

# ✅ 뉴스 크롤링 실행
def process_news_crawling():
    print("🚀 Daum 금융 해외 뉴스 크롤링 시작...")
    newses = crawl_daum_finance_news()

    if not newses:
        print("⚠️ 새로운 뉴스가 없습니다.")
        return

    store_news_in_redis(newses)
    print("✅ 뉴스 크롤링 및 저장 완료!")

if __name__ == "__main__":
    process_news_crawling()
