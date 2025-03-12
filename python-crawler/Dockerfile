# Python 3.9 베이스 이미지 사용
FROM python:3.9

# 작업 디렉토리 설정
WORKDIR /app

# 모든 소스 코드 복사
COPY app /app

# 필수 패키지 설치 (주석 처리된 부분 생략 가능)
#RUN apt-get update && apt-get install -y \
#    unzip \
#    wget \
#    curl \
#    gnupg \
#    && rm -rf /var/lib/apt/lists/*
#
## Google Chrome 및 ChromeDriver 설치
#RUN wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - \
#    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list \
#    && apt-get update \
#    && apt-get install -y google-chrome-stable \
#    && wget -q "https://chromedriver.storage.googleapis.com/114.0.5735.90/chromedriver_linux64.zip" -O /tmp/chromedriver.zip \
#    && unzip /tmp/chromedriver.zip -d /usr/local/bin/ \
#    && rm /tmp/chromedriver.zip

# requirements.txt 복사 및 패키지 설치
COPY requirements.txt /app/
RUN pip install --upgrade pip && pip install --use-deprecated=legacy-resolver -r /app/requirements.txt


# 파일 존재 여부 확인 후 실행 가능하도록 설정
RUN ls -al /app && chmod +x /app/init_fillings.py /app/init_stock.py /app/init_chart.py /app/update_news.py \
    /app/update_fillings.py /app/update_chart.py /app/get_stock_price.py

# 기본 실행 명령
CMD ["python", "/app/get_stock_price.py"]
