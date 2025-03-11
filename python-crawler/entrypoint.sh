#!/bin/bash

# 실행할 파이썬 스크립트 결정 (ENV 변수 사용)
case "$SCRIPT" in
    "init")
        python init.py ;;
    "earning")
        python earning.py ;;
    "init_stock")
        python init_stock.py ;;
    "init_chart_data")
        python init_chart_data.py ;;
    "background_stock")
        python initial_bring_stock.py ;;
    "update_chart")
        python update_chart_data.py ;;
    "schedule")
        python schedule.py ;;
    "news")
        python news.py ;;
    *)
        echo "❌ 알 수 없는 SCRIPT 값입니다. 실행을 중단합니다."
        exit 1 ;;
esac
