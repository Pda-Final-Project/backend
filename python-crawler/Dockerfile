# Python 베이스 이미지 사용
FROM python:3.9

# 작업 디렉토리 설정
WORKDIR /app

# 필요한 패키지 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 소스 코드 복사
COPY . .

# 실행 가능하게 설정
RUN chmod +x init_fillings.py init_stock.py init_chart.py update_news.py \
    update_fillings.py update_chart.py get_stock_price.py

# 컨테이너 실행 시 웹소켓 프로세스 실행
CMD ["python", "get_stock_price.py"]
