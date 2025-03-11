# Python 3.9 베이스 이미지 사용
FROM python:3.9

# 작업 디렉토리 설정
WORKDIR /app

# 필수 패키지 설치
RUN apt-get update && apt-get install -y \
    unzip \
    wget \
    curl \
    gnupg \
    && rm -rf /var/lib/apt/lists/*

# Google Chrome 및 ChromeDriver 설치
RUN wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    && wget -q "https://chromedriver.storage.googleapis.com/114.0.5735.90/chromedriver_linux64.zip" -O /tmp/chromedriver.zip \
    && unzip /tmp/chromedriver.zip -d /usr/local/bin/ \
    && rm /tmp/chromedriver.zip

COPY requirements.txt requirements.txt

RUN pip install --upgrade pip && pip install --no-cache-dir --ignore-installed  --use-deprecated=legacy-resolver -r requirements.txt

COPY . .

# 실행 가능하도록 설정
RUN chmod +x init_fillings.py init_stock.py init_chart.py update_news.py \
    update_fillings.py update_chart.py get_stock_price.py

CMD ["python", "get_stock_price.py"]
