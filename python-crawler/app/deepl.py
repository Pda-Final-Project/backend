from bs4 import BeautifulSoup
import requests
import re
import time
from multiprocessing.dummy import Pool as ThreadPool
import time

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
                 any(keyword in text for keyword in exclude_keywords) or  # 특정 키워드 제외
                 special_characters_pattern.match(text.strip()) or  # 특수 문자만 포함된 경우 제외
                 numbers_and_special_characters_pattern.match(text.strip())))  # 숫자와 특수문자로만 이루어진 경우 제외

# DeepL API를 사용하여 텍스트 번역
def translate_text(text, retries=3, delay=5):
    url = "https://api-free.deepl.com/v2/translate"
    params = {
        "auth_key": DEEPL_API_KEY,
        "text": text,
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
    for attempt in range(retries):
        try:
            response = requests.post(url, data=params)
            response.raise_for_status()
            return response.json()["translations"][0]["text"]
        except requests.exceptions.HTTPError as e:
            print(f"HTTP Error on attempt {attempt + 1}: {e}")
            print("Response Content:", response.content)
            if attempt < retries - 1:
                time.sleep(delay)
            else:
                raise

# HTML 파일 번역 함수
def translate_html_file(original_html):
    soup = BeautifulSoup(original_html, 'html.parser')

    body = soup.find('body')
    texts_to_translate = [element for element in body.find_all(string=True) if should_translate(element)] if body else []

    translations = []
    for text in texts_to_translate:
        translated_text = translate_text(text)
        translations.append(translated_text)

    # 번역된 텍스트로 HTML 업데이트
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

    return str(soup)

def correct_translated_html(original_html, translated_html):
    corrected_html = translated_html

    # 수정된 HTML 저장
    with open('deepl.html', 'w', encoding='utf-8') as f:
        f.write(corrected_html)

if __name__ == '__main__':
    url = 'https://www.sec.gov/Archives/edgar/data/320193/000032019325000016/xslF345X05/wk-form4_1738711872.xml'
    
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

    response = requests.get(url, headers=headers)
    response.raise_for_status()
    original_html = response.content.decode('utf-8')
    
    # 번역된 HTML 파일 생성
    translated_html = translate_html_file(original_html)
    
    # 번역된 HTML 파일 수정
    correct_translated_html(original_html, translated_html)