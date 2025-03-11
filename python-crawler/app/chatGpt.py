import openai
import tiktoken
import requests
import logging
from typing import List
from fil_order import fil_order
import json

# 로깅 설정
logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")

# OpenAI API 키 설정
client = openai.OpenAI(api_key="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")

# 최대 분할 토큰 수 (실제 모델의 컨텍스트 한계를 고려하여 설정)
MAX_CHUNK_TOKENS = 5000

# 토큰 분할 함수
def divide_text_by_tokens(text: str, max_tokens: int = MAX_CHUNK_TOKENS) -> List[str]:
    """
    텍스트를 최대 max_tokens 기준으로 분할하여 반환
    """
    encoding = tiktoken.encoding_for_model("gpt-4")
    tokens = encoding.encode(text)
    divided_texts: List[str] = []
    for i in range(0, len(tokens), max_tokens):
        chunk = tokens[i:i + max_tokens]
        divided_texts.append(encoding.decode(chunk))
    return divided_texts

# 분할 텍스트를 요약
def summarize_chunk(chunk: str) -> str:
    prompt_text = (
        f"아래는 공시 내용의 일부입니다:\n{chunk}\n\n"
        "위의 내용을 바탕으로 간단히 요약해 주세요. 그리고 특정 수치는 최대한 보존해주세요"
    )
    try:
        response = client.chat.completions.create(
            model="gpt-4o-mini",  # 실제 사용하는 모델 이름으로 변경
            messages=[
                {"role": "system", "content": "You are a concise financial summarizer."},
                {"role": "user", "content": prompt_text}
            ],
            max_tokens=1000,
            temperature=0.5
        )
        summary = response.choices[0].message.content.strip()
        logging.info("Chunk summarized successfully.")
        return summary
    except openai.OpenAIError as e:
        logging.error(f"Error with GPT API in summarize_chunk: {e}")
        return "요약 실패"

# 분할한 요약을 합쳐서 다시 요약
def analyze_combined_summary(summary_texts: List[str], filing_type: str) -> str:
    combined_summary = "\n".join(summary_texts)
    base_prompt = fil_order.get(filing_type, "")
    prompt = (
        f"{base_prompt}\n\n"
        "아래는 이전 단계에서 생성된 공시 요약 결과입니다. 이를 바탕으로 전체 공시 내용을 통합하여 "
        "공시 내용 요약 및 투자 의견을 도출해 주세요:\n"
        f"{combined_summary}"
    )
    try:
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": "You are an expert financial analyst."},
                {"role": "user", "content": prompt}
            ],
            max_tokens=1000,
            temperature=0.7
        )
        analysis_result = response.choices[0].message.content.strip()
        logging.info("Combined summary analysis completed.")
        return analysis_result
    except openai.OpenAIError as e:
        logging.error(f"Error with GPT API in analyze_combined_summary: {e}")
        return "분석 실패"

# 토큰 분할, 분할 텍스트 요약, 분할한 요약을 합쳐서 통합 요약
def process_summarize_fil(form_type: str, content: str) -> str:
    """
    공시 내용(content)을 지정된 공시 유형(form_type)에 따라 요약하는 전체 프로세스를 수행
    1. 긴 텍스트를 토큰 기준으로 분할
    2. 각 분할된 조각을 요약 (base_prompt 미포함)
    3. 모든 요약을 결합하여 최종 분석 단계에서 base_prompt를 적용
    """
    if not content or not content.strip():
        logging.warning("Empty content provided.")
        return ""

    chunks = divide_text_by_tokens(content, max_tokens=MAX_CHUNK_TOKENS)
    logging.info(f"Text divided into {len(chunks)} chunks.")

    summaries: List[str] = []
    for i, chunk in enumerate(chunks, 1):
        logging.info(f"Summarizing chunk {i}/{len(chunks)}")
        summary = summarize_chunk(chunk)
        summaries.append(summary)

    logging.info("All chunks summarized. Starting analysis of combined summaries.")
    summarize_result = analyze_combined_summary(summaries, form_type)
    return summarize_result

# 공시 타입을 base_prompt 공시 카테고리로 변환 ex) 10-Q => fil_10q
def map_filing_type(filing_type: str) -> str:
    mapping = {
        "SCHEDULE 13G": "fil_schedule13",
        "SCHEDULE 13G/A": "fil_schedule13",
        "SCHEDULE 13D": "fil_schedule13",
        "SCHEDULE 13D/A": "fil_schedule13",
        "SC 13D": "fil_schedule13",
        "SC 13D/A": "fil_schedule13",
        "SC 13G": "fil_schedule13",
        "SC 13G/A": "fil_schedule13",

        "Form S-1": "fil_s1",
        "S-1": "fil_s1",
        "Form S-1MEF": "fil_s1",
        "S-1MEF": "fil_s1",

        "10-Q": "fil_10q",
        "10-QT": "fil_10q",

        "8-K": "fil_8k",
        "8-K/A": "fil_8k",

        "Form 4": "fil_f4",
        "4": "fil_f4",
        "4/A": "fil_f4"
    }
    return mapping.get(filing_type, "unknown")

# html로 요약 출력하는 최종 함수
def save_summary_as_html(filling_url, filling_type, headers):
    try:
        response = requests.get(filling_url, headers=headers)
        response.raise_for_status()
        original_text = response.text
        order_filling_type = map_filing_type(filling_type)
        
        summary_content = process_summarize_fil(order_filling_type, original_text)
        summary_content = summary_content.strip("```").replace("html\n", "", 1).strip()
    except requests.exceptions.RequestException as e:
        logging.error(f"❌ 요청 실패: {e}")
    return summary_content

def get_summary_as_json(filling_url, filling_type, headers):
    try:
        response = requests.get(filling_url, headers=headers)
        response.raise_for_status()
        original_text = response.text
        order_filling_type = map_filing_type(filling_type)

        # 병렬 공시 요약 실행
        summary_content = process_summarize_fil(order_filling_type, original_text)

        summary_content = summary_content.strip("```").replace("json\n", "", 1).strip()

        if summary_content.startswith("{"):  
            parsed_json = json.loads(summary_content)
        else:
            logging.error("❌ JSON 변환 오류: 응답이 JSON 형식이 아닙니다.")
            return json.dumps({"error": "Response is not in JSON format"}, ensure_ascii=False, indent=4)

        return json.dumps(parsed_json, ensure_ascii=False, indent=4)

    except requests.exceptions.RequestException as e:
        logging.error(f"❌ 요청 실패: {e}")
        return json.dumps({"error": "Request failed"}, ensure_ascii=False, indent=4)

    except json.JSONDecodeError as e:
        logging.error(f"❌ JSON 변환 오류: {e}")
        return json.dumps({"error": "JSON parsing failed"}, ensure_ascii=False, indent=4)


def main():
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
    # 테스트할 URL 설정 (예: SEC 공시 문서 URL)
    filling_url = "https://www.sec.gov/Archives/edgar/data/36146/000106299325002486/xslF345X05/form4.xml"

    s1json = """
    {'total': {'value': 4, 'relation': 'eq'}, 'data': [{'id': '3c37a4a859d8cc1d30a23a357e7ccb5e', 'filedAt': '2001-03-15T00:00:00-05:00', 'accessionNo': '0000912057-01-007636', 'formType': '424B4', 'cik': '1088381', 'ticker': 'GWB-B', 'entityName': 'SPECTRUM BANCORPORATION INC', 'filingUrl': 'https://www.sec.gov/Archives/edgar/data/1088381/000091205701007636/a2041782z424b4.htm', 'tickers': [{'ticker': 'SBK.Pr.B', 'type': 'Preferred Securities', 'exchange': 'American Stock Exchange'}], 'securities': [{'name': '2,400,000 Preferred Securities'}, {'name': '9.75% Cumulative Preferred Securities'}], 'publicOfferingPrice': {'perShare': 10, 'perShareText': '$10.00', 'total': 24000000, 'totalText': '$24,000,000'}, 'underwritingDiscount': {'perShare': 0.35, 'perShareText': '$0.35', 'total': 840000, 'totalText': '$840,000'}, 'proceedsBeforeExpenses': {'perShare': 9.65, 'perShareText': '$9.65', 'total': 23160000, 'totalText': '$23,160,000'}, 'underwriters': [{'name': 'Howe Barnes Investments, Inc.'}, {'name': 'D.A. Davidson & Co.'}, {'name': 'Fahnestock & Co., Inc.'}, {'name': 'Friedman, Billings, Ramsey & Co., Inc.'}, {'name': 'Pacific Crest Securities'}, {'name': 'Ryan, Beck & Co., Inc.'}, {'name': "Sandler O'Neill & Partners, L.P."}, {'name': 'Stifel, Nicolaus & Company, Incorporated'}, {'name': 'Dougherty & Company LLC'}, {'name': 'Wayne Hummer Investments LLC'}, {'name': 'Kirkpatrick, Pettis, Smith, Polian Inc.'}, {'name': 'David A. Noyes & Company'}, {'name': 'SAMCO Capital Markets'}], 'lawFirms': [{'name': 'Richards, Layton & Finger, P.A.', 'location': 'Delaware, United States'}, {'name': 'Baird, Holm, McEachen, Pedersen, Hamann & Strasheim LLP', 'location': 'Nebraska, United States'}, {'name': 'Chapman and Cutler', 'location': 'Illinois, United States'}], 'auditors': [{'name': 'McGladrey & Pullen, LLP'}, {'name': 'Deloitte & Touche LLP'}], 'management': [{'name': 'Deryl F. Hamann', 'age': 68, 'position': 'Chairman of the Board, CEO and Director'}, {'name': 'Daniel A. Hamann', 'age': 42, 'position': 'President, COO and Director'}, {'name': 'Daniel J. Brabec', 'age': 42, 'position': 'Executive Vice President, CFO, Secretary, Treasurer and Director'}, {'name': 'Thomas B. Fischer', 'age': 54, 'position': 'Senior Vice President and Director'}, {'name': 'W. Eric Bunderson', 'age': 44, 'position': 'Vice President, Chief Credit Officer, Assistant Secretary and Director'}], 'employees': {'total': None, 'asOfDate': None, 'perDivision': [], 'perRegion': []}}]}
    """
    
    html_output = get_summary_as_json(filling_url, '4', headers)
    
    # HTML 파일로 저장
    output_filename = "sec_f4_summary_json.html"
    with open(output_filename, "w", encoding="utf-8") as file:
        file.write(html_output)

    logging.info(f"✅ HTML 요약 파일 저장 완료: {output_filename}")
    
if __name__ == "__main__":
    main()