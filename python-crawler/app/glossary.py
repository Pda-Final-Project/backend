import requests

# DeepL API 키 설정
DEEPL_API_KEY = 'd827796e-abfe-45c1-a800-3773f2dc0c7a:fx'  # API 키는 안전하게 관리하세요.

# TSV 파일을 읽어오는 함수
def read_tsv_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as file:
        return file.read()

# 용어집 생성 함수
def create_glossary(entries):
    url = "https://api-free.deepl.com/v2/glossaries"
    headers = {
        'Authorization': f'DeepL-Auth-Key {DEEPL_API_KEY}',
        'Content-Type': 'application/json'
    }
    data = {
        "name": "Financial Glossary",
        "source_lang": "en",
        "target_lang": "ko",
        "entries": entries,
        "entries_format": "tsv"
    }
    print("Request Data:", data)  # 디버깅을 위해 요청 데이터를 출력합니다.
    response = requests.post(url, headers=headers, json=data)
    try:
        response.raise_for_status()
    except requests.exceptions.HTTPError as e:
        print("HTTP Error:", e)
        print("Response Content:", response.content)
        raise
    return response.json()

if __name__ == '__main__':
    # TSV 파일 경로
    tsv_filepath = 'glo.tsv'
    
    # TSV 파일 읽기
    tsv_entries = read_tsv_file(tsv_filepath)
    
    # 용어집 생성
    glossary_response = create_glossary(tsv_entries)
    
    print("Glossary created successfully!")
    print("Glossary ID:", glossary_response["glossary_id"])