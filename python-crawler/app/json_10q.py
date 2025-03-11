from sec_api import XbrlApi
import json

# API 키 설정
api_key = "9ee9dc08f4dc222d5ac9e425da27cca57326fa9404eed3feb56ec405dc371b74"
xbrlApi = XbrlApi(api_key)

# segment 필드가 없는 항목만 필터링
def filter_without_segment(data):
    return [item for item in data if "segment" not in item]

def get_filtered_10q_data(accession_no):
    try:
        # 데이터 요청
        xbrl_json = xbrlApi.xbrl_to_json(accession_no=accession_no)

        # 필요한 항목만 필터링 및 추출
        statements_of_income = xbrl_json.get("StatementsOfIncome", {})
        extracted_data = {
            "Revenue": filter_without_segment(statements_of_income.get("RevenueFromContractWithCustomerExcludingAssessedTax", [])),
            "OperatingIncome": filter_without_segment(statements_of_income.get("OperatingIncomeLoss", [])),
            "NetIncome": filter_without_segment(statements_of_income.get("NetIncomeLoss", [])),
            "BasicEarningsPerShare": filter_without_segment(statements_of_income.get("EarningsPerShareBasic", []))
        }

        return json.dumps(extracted_data, ensure_ascii=False, indent=4)


    except Exception as e:
        print(f"데이터 처리 중 오류 발생: {e}")
        return None