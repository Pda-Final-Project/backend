import json
import boto3
import os

# ✅ 환경 변수에서 AWS 자격 증명 불러오기 (Kubernetes Secret 사용)
aws_access_key_id = os.getenv("AWS_ACCESS_KEY_ID")
aws_secret_access_key = os.getenv("AWS_SECRET_ACCESS_KEY")
region_name = os.getenv("AWS_REGION")

if not aws_access_key_id or not aws_secret_access_key or not region_name:
    raise ValueError("❌ AWS 자격 증명 환경 변수가 설정되지 않았습니다.")

# AWS 자격 증명 설정
s3 = boto3.client(
    's3',
    aws_access_key_id=aws_credentials['aws_access_key_id'],
    aws_secret_access_key=aws_credentials['aws_secret_access_key'],
    region_name=aws_credentials['region_name']
)

# S3 버킷 이름 설정
bucket_name = 'finpago-bucket'

def check_s3_file_exists(s3_key):
    try:
        s3.head_object(Bucket=bucket_name, Key=s3_key)
        print(f"File already exists in S3: {s3_key}")
        return True
    except s3.exceptions.ClientError as e:
        if e.response["Error"]["Code"] == "404":
            print(f"❌ File not found in S3: {s3_key}")
        elif e.response["Error"]["Code"] == "403":
            print(f"🚨 Permission denied for S3 file: {s3_key}")
            print(f"🚨 Permission denied for S3 file: {e}")
        else:
            print(f"⚠️ Unexpected S3 error: {e.response}")
        return False

def upload_translated_document_to_s3(s3_key, translate_html_file):
    try:
        s3.put_object(Bucket=bucket_name, Key=s3_key, Body=translate_html_file.encode('utf-8'))
        s3_url = f"https://{bucket_name}.s3.amazonaws.com/{s3_key}"
        print(f"Translated document uploaded to S3: {s3_url}")
        return s3_url   
    except Exception as e:
        print(f"Error uploading translated_html to S3: {e}")
        raise


def upload_json_to_s3(json_data, s3_key):
    try:
        json_content = json.dumps(json_data, ensure_ascii=False, indent=4)
        s3.put_object(Bucket=bucket_name, Key=s3_key, Body=json_content.encode('utf-8'))
        s3_url = f"https://{bucket_name}.s3.amazonaws.com/{s3_key}"
        print(f"JSON document uploaded to S3: {s3_url}")
        return s3_url
    except Exception as e:
        print(f"Error uploading JSON to S3: {e}")
        raise