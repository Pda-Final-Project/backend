import os
from deep_translator import GoogleTranslator
import requests
import re
from multiprocessing.dummy import Pool as ThreadPool
import time
from bs4 import BeautifulSoup

## 미국 공시 원본 html을 받아서 번역 후 수정된 html을 반환하는 함수

# DeepL API 키 설정
DEEPL_API_KEY = 'd827796e-abfe-45c1-a800-3773f2dc0c7a:fx'  # API 키는 안전하게 관리하세요.

# 번역 예외 처리 함수 (숫자, 날짜, 코드, 특정 키워드 등)
def should_translate(text):
    exclude_keywords = ["SEC"]
    special_characters_pattern = re.compile(r'[!@#$%^&*(),.?":{}|<>]')
    numbers_and_special_characters_pattern = re.compile(r'^[\d!@#$%^&*(),.?":{}|<>]+$')
    
    return (bool(text.strip()) and 
            not (re.match(r'^\d{4}-\d{2}-\d{2}$', text.strip()) or  # 날짜 형식 제외
                 re.match(r'^\d+$', text.strip()) or  # 숫자 제외
                 re.match(r'^[A-Z]{1,5}$', text.strip()) or  # 주식 코드 제외
                 any(keyword in text for keyword in exclude_keywords)   # 특정 키워드 제외 
                #  or special_characters_pattern.match(text.strip())  # 특수 문자만 포함된 경우 제외
                #  or   numbers_and_special_characters_pattern.match(text.strip())  # 숫자와 특수문자로만 이루어진 경우 제외
                )) 

# DeepL API를 사용하여 텍스트 번역
def translate_texts(texts, retries=3, delay=5):
    url = "https://api-free.deepl.com/v2/translate"
    params = {
        "auth_key": DEEPL_API_KEY,
        "source_lang": "EN",
        "target_lang": "KO",
        "tag_handling": "html",
        "split_sentences": "nonewlines",
        "preserve_formatting": "1",
        "formality": "prefer_more",
        "model_type": "prefer_quality_optimized",
        "glossary_id": "ea2e50b9-0986-4f1e-a1c2-d455b1a74e58",
        "context": "Financial disclosure document"
    }
    translations = []
    for i in range(0, len(texts), 10):  # 최대 10개의 텍스트를 한 번에 번역
        batch = texts[i:i + 10]
        params["text"] = batch
        for attempt in range(retries):
            try:
                response = requests.post(url, data=params)
                response.raise_for_status()
                translations.extend([t["text"] for t in response.json()["translations"]])
                break
            except requests.exceptions.HTTPError as e:
                print(f"HTTP Error on attempt {attempt + 1}: {e}")
                print("Response Content:", response.content)
                if attempt < retries - 1:
                    time.sleep(delay)
                else:
                    raise
    return translations

# 번역 요청 함수
def translate_texts_google(texts, retries=3, delay=5):
    translator = GoogleTranslator(timeout=5, source='en', target='ko')
    translations = []
    for i in range(0, len(texts), 50):  # 최대 50개의 텍스트를 한 번에 번역
        batch = texts[i:i + 50]
        for attempt in range(retries):
            try:
                translations.extend(translator.translate_batch(batch))
                break
            except Exception as e:
                print(f"Error on attempt {attempt + 1}: {e}")
                if attempt < retries - 1:
                    time.sleep(delay)
                else:
                    raise
    return translations


# HTML 파일 번역 함수
def translate_html_file(original_html):
    soup = BeautifulSoup(original_html, 'html.parser')

    body = soup.find('body')
    if body:
        # body를 문자열로 변환
        body_str = str(body)

        # <i>와 <b> 태그를 제거
        body_str = re.sub(r'</?(i|b)>', '', body_str)
        body_str = re.sub(r'\s+', ' ', body_str)

        # 다시 HTML로 변환
        body = BeautifulSoup(body_str, 'html.parser').find('body')

    texts_to_translate = [element for element in body.find_all(string=True) if should_translate(element)] if body else []

    # 번역 요청을 병렬로 처리
    num_threads = os.cpu_count() * 2  # CPU 코어 수의 2배로 쓰레드 수 설정
    chunk_size = len(texts_to_translate) // num_threads
    chunks = [texts_to_translate[i:i + chunk_size] for i in range(0, len(texts_to_translate), chunk_size)]

    pool = ThreadPool(num_threads)
    translations = pool.map(translate_texts_google, chunks)
    pool.close()
    pool.join()

    # 번역된 텍스트로 HTML 업데이트
    translations = [item for sublist in translations for item in sublist]  # 중첩 리스트 풀기
    for element, translated in zip(texts_to_translate, translations):
        if translated is not None:
            element.replace_with(translated)
        else:
            element.replace_with(element)

    # <meta charset="UTF-8"> 태그 추가
    head = soup.find('head')
    if head:
        meta_tag = soup.new_tag('meta', charset='UTF-8')
        head.insert(0, meta_tag)  # head의 첫 번째 위치에 추가
    
    soup.body.replace_with(body)

    return str(soup)

def translate_html(url, headers):
    start_time = time.time()

    response = requests.get(url, headers=headers)
    response.raise_for_status()
    original_html = response.content.decode('utf-8')
    
    end_time = time.time()
    print(f"Translating the HTML file took {end_time - start_time} seconds")
    
    # 번역된 HTML 파일 생성
    return translate_html_file(original_html)

def main():
    url = 'https://www.sec.gov/Archives/edgar/data/320193/000032019325000022/xslF345X05/wk-form4_1738712153.xml'
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36',
        'Accept-Language': 'ko,en-US;q=0.9,en;q=0.8',
        'Accept-Encoding': 'gzip, deflate, br, zstd',
        'Connection': 'keep-alive',
        'Cache-Control': 'max-age=0',
        'Priority': 'u=0, i',
        'Sec-CH-UA': '"Not(A:Brand";v="99", "Google Chrome";v="133", "Chromium";v="133"',
        'Sec-CH-UA-Mobile': '?0',
        'Sec-CH-UA-Platform': '"Windows"',
        'Sec-Fetch-Dest': 'document',
        'Sec-Fetch-Mode': 'navigate',
        'Sec-Fetch-Site': 'none',
        'Sec-Fetch-User': '?1',
        'Upgrade-Insecure-Requests': '1'
    }

    # 번역된 HTML 파일 생성
    translated_html = translate_html(url, headers)
    
    # 번역된 HTML 출력
    print(translated_html)

if __name__ == '__main__':
    main()