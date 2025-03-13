"""
주식 종목에서 실시간 체결 내역(체결가, 체결량, 거래량, 시간) 및 현재가, 변동률을 가져오는 코드
"""
from redis_config import redis_client as r
import asyncio
import json
import time
import requests
import websockets
import pandas as pd
from stocks_data import stocks


### 📌 API Key 설정 ###
g_appkey = "PSKJKSd75HbuPliK70C0cUdl4OYAvyIGmROi"
g_appsecret = "ZtH0c5eLI1BJnBZ1f9LVn9ggNT7Af2NHVTOu7dMBdvxCy7OhDxnuBjKU8YkpiYXgxxHJgvuuOSbqKV/HeuQ+smMz0K88mNkiRtXGZASlotr+nMaOPCerfs/fduLBhbimdiimL4IobO6Ne0VPZEPanlFzz0x91nbDi/W8wmLOa5ZkkmRrDus="

# 웹소켓 접속키 발급
def get_approval(key, secret):
    url = 'https://openapi.koreainvestment.com:9443'  # 실전투자 계좌
    headers = {"content-type": "application/json"}
    body = {"grant_type": "client_credentials", "appkey": key, "secretkey": secret}
    PATH = "oauth2/Approval"
    URL = f"{url}/{PATH}"
    time.sleep(0.05)
    res = requests.post(URL, headers=headers, data=json.dumps(body))
    return res.json()["approval_key"]


# 환율 변환 함수
def get_exchange_rate():
    exchange_rate = r.get("stock:TSLA:exchange_rate")  # Redis에 저장된 환율 키
    print(exchange_rate)
    return float(exchange_rate)*1000 if exchange_rate else 0  # 환율이 없으면 0 반환 (예외 처리)


# 실시간 체결 내역 redis에 저장
def save_stock_purchase_data(ticker, price, volume, trade_volume, time, buy_sell_flag):
    key = f"stock:{ticker}:purchase"  # ex) stock:TSLA:purchase

    # 데이터 변환 (str → float/int)
    price = float(price) * get_exchange_rate()  # 현재가 (USD → KRW 변환)
    volume = int(volume)  # 체결량
    trade_volume = int(trade_volume)  # 거래량

    # 데이터 저장 형식
    data = {
        "trade_ticker": ticker,
        "current_price": round(price, 2),  # 가격 소수점 2자리 유지
        "volume": volume,
        "trade_volume": trade_volume,
        "time": time,
        "trade_type": buy_sell_flag
    }

    r.lpush(key, json.dumps(data))
    r.publish("trade_updates", json.dumps(data))
    r.ltrim(key, 0, 19)

    print(f"{ticker} 데이터 저장 완료: {data}")


# 현재가 및 변동률 redis에 저장
def save_stock_data(ticker, price, change_rate, trade_volume):
    key = f"stock:{ticker}"

    # 데이터 변환
    price = float(price) * get_exchange_rate()  # 현재가 (USD → KRW 변환)
    trade_volume=int(trade_volume)
    data = {
        "current_price": round(price, 2),
        "change_rate": change_rate,
        "volume": trade_volume
    }
    r.hmset(key, data)
    stock_update = {
        "ticker": ticker,
        "name": r.hget(key, "name"),
        "current_price": round(price, 2),
        "change_rate": change_rate,
        "volume": trade_volume
    }
    r.publish("stock_updates", json.dumps(stock_update))
    print(f"{ticker} 현재가 및 등락율 업데이트 완료: {data}")


# 체결 내역 가져오는 함수
def stocks_purchase_overseas(data_cnt, data):
    # 원본 데이터의 필드 목록
    menulist = "실시간종목코드|종목코드|수수점자리수|현지영업일자|현지일자|현지시간|한국일자|한국시간|시가|고가|저가|현재가|대비구분|전일대비|등락율|매수호가|매도호가|매수잔량|매도잔량|체결량|거래량|거래대금|매도체결량|매수체결량|체결강도|시장구분"
    menustr = menulist.split('|')
    pValue = data.split('^')

    data_list = []
    i = 0

    for cnt in range(data_cnt):
        row = {menu: pValue[i] for i, menu in enumerate(menustr, start=i)}
        i += len(menustr)
        data_list.append(row)

    # DataFrame 생성 후 원하는 컬럼만 필터링
    df = pd.DataFrame(data_list)[["종목코드","현재가", "등락율", "체결량", "거래량", "매도체결량", "매수체결량","한국시간"]].fillna(0)

    # 매수/매도 여부 추가
    df["buy_sell_flag"] = df.apply(
        lambda row: "BUY" if int(row["매수체결량"]) > int(row["매도체결량"])
        else "SELL" if int(row["매도체결량"]) > int(row["매수체결량"])
        else "NEUTRAL",
        axis=1
    )

    for _, row in df.iterrows():
        save_stock_purchase_data(
            ticker=row["종목코드"],
            price=row["현재가"],
            volume=row["체결량"],
            trade_volume=row["거래량"],
            time=row["한국시간"],
            buy_sell_flag=row["buy_sell_flag"]
        )
        save_stock_data(ticker=row["종목코드"], price=row["현재가"], change_rate=row["등락율"], trade_volume=row["거래량"],)


### 웹소켓 연결 ###
async def connect(code_list):
    try:
        approval_key = get_approval(g_appkey, g_appsecret)
        print(f"✅ 승인 키 발급 완료: {approval_key}")

        url = 'ws://ops.koreainvestment.com:21000'  # 실전투자 계좌 웹소켓

        senddata_list = [
            json.dumps({
                "header": {
                    "approval_key": approval_key,
                    "custtype": "P",
                    "tr_type": i,
                    "content-type": "utf-8"
                },
                "body": {
                    "input": {"tr_id": j, "tr_key": k}
                }
            }) for i, j, k in code_list
        ]

        async with websockets.connect(url, ping_interval=None) as websocket:
            for senddata in senddata_list:
                await websocket.send(senddata)
                print(f"📡 [요청] {senddata}")
                await asyncio.sleep(0.5)

            while True:
                data = await websocket.recv()

                # 해외주식 체결 이벤트만 처리
                if data[0] == '0':
                    recvstr = data.split('|')
                    trid0 = recvstr[1]

                    if trid0 == "HDFSCNT0":  # 해외주식 체결 데이터
                        print("✅ [해외주식 체결 이벤트 수신됨!]")
                        data_cnt = int(recvstr[2])
                        stocks_purchase_overseas(data_cnt, recvstr[3])

    except Exception as e:
        print('오류 발생:', e)
        print('재접속 시도 중...')
        time.sleep(0.1)
        await connect(code_list)


### 메인 실행 ###
async def get_current_price_for_stocks():
    # 해외주식 체결 이벤트 (HDFSCNT0) 요청
    code_list = []
    for stock in stocks:
        code_list.append(['1', 'HDFSCNT0', 'RBAQ'+stock["ticker"]])
    await connect(code_list)

if __name__ == "__main__":
    asyncio.run(get_current_price_for_stocks())