
import json
import time
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from bs4 import BeautifulSoup
from redis_config import redis_client

# Investing.com 뉴스 크롤링
def crawl_investing_news():
    url = "https://kr.investing.com/news"

    # Chrome 드라이버 옵션 설정 (headless 모드 활성화)
    chrome_options = Options()
    chrome_options.add_argument("--headless")
    chrome_options.add_argument("--disable-gpu")
    chrome_options.add_argument("--no-sandbox")
    chrome_options.add_argument("--disable-dev-shm-usage")
    chrome_options.add_argument("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
    chrome_options.add_argument("window-size=1920,1080")  # 해상도 설정

    driver = webdriver.Chrome(options=chrome_options)
    driver.get(url)

    # 페이지 하단까지 스크롤하여 동적 로딩된 기사들이 로드되도록 함
    SCROLL_PAUSE_TIME = 2
    driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
    time.sleep(SCROLL_PAUSE_TIME)

    # WebDriverWait로 기사 로딩 대기
    try:
        WebDriverWait(driver, 20).until(
            EC.presence_of_all_elements_located((By.CSS_SELECTOR, "article[data-test='article-item']"))
        )
    except Exception as e:
        print("뉴스 기사가 로딩되지 않았습니다:", e)

    # 추가: 각 뉴스 기사 요소를 개별 스크롤하여 lazy load가 작동하도록 함
    articles = driver.find_elements(By.CSS_SELECTOR, "article[data-test='article-item']")
    for article in articles:
        driver.execute_script("arguments[0].scrollIntoView(true);", article)
        time.sleep(0.3)  # 짧은 대기

    html = driver.page_source
    driver.quit()

    soup = BeautifulSoup(html, "html.parser")
    articles = soup.find_all("article", {"data-test": "article-item"})
    print(f"🔍 찾은 기사 수: {len(articles)}")

    news_list = []
    for article in articles:
        # 제목 및 URL 추출
        title_tag = article.find("a", {"data-test": "article-title-link"})
        title = title_tag.get_text(strip=True) if title_tag else None
        news_url = title_tag.get("href") if title_tag else None
        if news_url and not news_url.startswith("http"):
            news_url = "https://kr.investing.com" + news_url

        # 언론사 추출
        company_tag = article.find("span", {"data-test": "news-provider-name"})
        company = company_tag.get_text(strip=True) if company_tag else None

        # 이미지 URL 추출
        img_tag = article.find("img", {"data-test": "item-image"})
        img = img_tag.get("src") if img_tag else img_tag.get("data-src")

        # 뉴스 등록일 추출
        time_tag = article.find("time", {"data-test": "article-publish-date"})
        news_date = time_tag.get("datetime") if time_tag else None

        news = {
            "news_title": title,
            "news_company": company,
            "news_img": img,
            "news_url": news_url,
            "news_date": news_date
        }
        news_list.append(news)

    return news_list

# Redis에 저장
def store_news_in_redis(news_list):
    # 기존 저장된 뉴스 삭제 후 최신 뉴스 저장
    redis_client.delete("investing:news")
    for news in news_list:
        news_json = json.dumps(news, ensure_ascii=False)
        redis_client.rpush("investing:news", news_json)
        print(news_json)
        print(f"✅ 저장 완료: {news['news_title']}")

# 뉴스 크롤링 실행
def process_news_crawling():
    print("🚀 뉴스 크롤링 시작...")
    newses = crawl_investing_news()
    
    if not newses:
        print("⚠️ 새로운 뉴스가 없습니다.")
        return
    
    store_news_in_redis(newses)
    print("✅ 뉴스 크롤링 및 저장 완료!")

if __name__ == "__main__":
    process_news_crawling()
