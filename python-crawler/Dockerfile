# Python 3.9 베이스 이미지 사용
FROM python:3.9

# 작업 디렉토리 설정
WORKDIR /app

COPY requirements.txt requirements.txt

RUN pip install --upgrade pip && pip install --no-cache-dir --ignore-installed  --use-deprecated=legacy-resolver -r requirements.txt

COPY . .

# 실행 가능하도록 설정
RUN chmod +x init_fillings.py init_stock.py init_chart.py update_news.py \
    update_fillings.py update_chart.py get_stock_price.py

CMD ["python", "get_stock_price.py"]
