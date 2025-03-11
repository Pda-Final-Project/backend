from chart_api import init_chart_data

if __name__ == "__main__":
    print("🔄 초기 차트 데이터를 MySQL에 저장 중...")
    init_chart_data()
    print("✅ 완료!")