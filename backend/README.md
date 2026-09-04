# Neiro PC Music Streaming Server

초경량 Node.js 음악 스트리밍 단일 서버입니다.

## 1. 실행 전 준비 (PC)
PC에 [Node.js](https://nodejs.org/) (v16 이상 권장)가 설치되어 있어야 합니다.

## 2. 설치 및 실행 방법

```bash
# 1) 패키지 설치
npm install

# 2) 서버 실행
# 기본 음악 폴더(내 PC의 음악 폴더)로 실행:
node server.js

# 또는 원하는 음악 폴더 경로와 포트 지정 실행:
node server.js "D:\Music" 3000
```

## 3. 스마트폰 / 모바일 앱 연동
서버 실행 시 콘솔에 출력되는 모바일 접속 주소(예: `http://192.168.0.x:3000`)를 앱 설정창에 입력하면 즉시 스트리밍 및 동기화됩니다.

## 4. 지원 기능
- **MP3, FLAC, M4A, AAC, OGG, WAV** 메타데이터 자동 추출
- **임베디드 앨범 커버** 자동 추출 및 고속 서빙 (`/api/cover/:id`)
- **가사 완벽 지원**: 곡 내장 가사 태그 및 동명 `.lrc` 파일 자동 파싱 (`/api/lyrics/:id`)
- **HTTP 206 Partial Content (Range)** 지원: 시크바 드래그 및 즉각 탐색 최적화
