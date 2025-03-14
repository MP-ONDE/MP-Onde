# ONDE

**ONDE**는 잊혀져 가는 일상 속 스몰토크 문화를 되살리기 위해 기획된 소셜 미디어 서비스입니다.  
서로 가까이에 있는 사람들과 가벼운 안부 인사나 스몰토크를 자연스럽게 교환할 수 있게 함으로써,  
스마트폰에만 몰두하는 현대 사회에서 사라져 가던 “정겨운 스몰토크” 문화를 부활시키는 데 목표가 있습니다.

<br>

## 프로젝트 소개

- **일상 속 스몰토크 복원**  
  스마트폰과 SNS의 발달로 사라져 가는 길거리 인사, 엘리베이터 안 스몰토크 등  
  소소한 대화를 애플리케이션을 통해 다시 시작할 수 있도록 돕습니다.

- **가까운 사람들과의 연결**  
  기존 SNS가 먼 거리에 있는 지인들과의 소통에 집중했다면,  
  ONDE는 BLE(저전력 블루투스)를 활용하여 물리적으로 가까운 이용자들을 자동으로 탐색하고,  
  서로 프로필을 확인하며 가벼운 대화를 시작하도록 유도합니다.

- **간단한 호감 표시 & 일상 공유**  
  간단한 자기소개(나이, 관심사, 오늘의 OOTD, 스몰토크 멘트)를 설정하거나,  
  상대방 OOTD에 관해 “좋아요”나 “어디서 샀는지 궁금해요!” 등의 스몰토크를 통해  
  취향을 공유할 수 있는 계기를 제공합니다.

<br>

## 주요 기능

1. **회원가입 및 로그인**  
   - Firebase Auth 연동으로 이메일/비밀번호 회원가입 및 로그인 처리  
   - Google OAuth 연동(구글 계정 로그인)  
   - Firestore에 사용자 프로필(닉네임, 나이, 관심사, OOTD, 스몰토크 등) 저장

2. **프로필 설정/편집**  
   - 별도의 화면에서 닉네임, 나이, 성별, 관심사, OOTD, 스몰토크 멘트 등을 수정 가능  
   - 사진 업로드, 카메라 촬영을 통한 프로필 이미지 설정 (Firebase Storage 연동)

3. **BLE 스캔**  
   - BLE(저전력 블루투스) 기능으로 주변 ONDE 앱 유저 탐색  
   - 특정 범위 내의 이용자 프로필을 UI 상 버튼으로 표시  
   - RSSI값(신호 세기)에 따라 사용자 위치 대략적 분류(가까움/중간/멀리)

4. **채팅 기능**  
   - 두 사용자가 서로 연결되어 채팅방을 생성  
   - 실시간 메시지 송수신 (Cloud Firestore 실시간 업데이트)  
   - 수신 메시지 읽음 처리, 미확인 메시지 개수(unread count) 관리

5. **AI 멘트 추천(추가 기능)**  
   - AIRecommendationBottomSheetFragment를 통해 스몰토크 주제나 질문 문장을 추천  
   - ChatGPT API 등 외부 AI API를 연동 가능 (프로토타입/실험 단계)

6. **일정/메모(캘린더) 기능**  
   - 사용자가 날짜별 메모를 작성할 수 있도록 SQLite(Local DB)를 활용  
   - 흔들기 제스처(가속도 센서)로 BLE 스캔을 트리거하는 등 부가 UX 기능

<br>

## 기술 스택

- **언어**: Kotlin  
- **IDE 및 빌드 환경**: Android Studio (Gradle)  
- **Firebase**  
  - Auth: 이메일/비밀번호 & Google OAuth 로그인/회원가입  
  - Firestore: 사용자 프로필 및 채팅 메시지 저장  
  - Storage: 프로필 사진 등 이미지 업로드/관리  
  - FCM: 푸시 알림(토큰 관리 등)
- **로컬 DB**
  - SQLite (간단한 메모/캘린더 기능 용도)
- **Bluetooth Low Energy (BLE)**  
  - 주변 기기 스캔 & 광고(Advertise)
  - Android 12(SDK 31) 이상을 고려한 BLE 권한 처리(BLUETOOTH_SCAN / CONNECT / ADVERTISE)  
- **이미지 로딩**  
  - Picasso (프로필 사진 등 URL 이미지를 View에 로드)
- **기타**  
  - Android Sensor(Accelerometer) 사용 (흔들기 감지)  
  - Google Sign-InHelper (로그인 모듈)
  
<br>

## 빌드 및 실행 방법

1. **Android Studio 설치**
   - 최신 버전 또는 최소 2022.1 이상 권장
   
2. **프로젝트 복제**
   ```bash
   git clone https://github.com/your-repo/onde.git
   ```
3. **Firebase 프로젝트 설정**  
   - Firebase Console에서 새 Android 앱 등록 후 google-services.json 다운로드  
   - Android Studio의 `app/` 디렉터리에 `google-services.json` 파일 추가
   - build.gradle(Project)에 `classpath 'com.google.gms:google-services:4.3.X'` 등 설정
   - build.gradle(Module)에 `apply plugin: 'com.google.gms.google-services'` 추가

4. **SHA-1 인증서 핑거프린트 등록(구글 로그인 사용 시 필수)**  
   - 프로젝트에 구글 로그인 사용한다면 Firebase console에서 SHA-1 지문 등록
   - 에뮬레이터/실기기에서 구동 시 필요한 SHA-1 키도 각각 등록
     
5. **권한 설정**  
   - BLE 기능 사용을 위해 `AndroidManifest.xml`에  
     ```
     <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
     <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
     <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
     ...
     ```
     와 같은 권한 선언  
   - Android 12 이상에서 BLE 기능 정상 동작을 위해 런타임 권한 체크 필요

6. **실행**  
   - Android Studio에서 `Run` 클릭 (실기기 혹은 에뮬레이터)
   - 회원가입 후, 로그인 & BLE On → 주변 사용자 스캔 가능

---

/프로젝트 정보
ONDE  (크게 보이게, 가운데 정렬)

온데 로고 띄우기

프로젝트 정보
서울과학기술대학교 ITM 3학년 2학기의 MP의 팀프로젝트로 개발되었음 (과기대 로고)
개발기간 정보 

팀원 소개
프론트엔드 정동영
백엔드 이준서
프로젝트 관리? 김관우

프로젝트 소개
ONDE is Android OS based smalltalk app which focus on near-distance communication with random passerby.
Users can search and communicate other users only who is nearby them, using BLE.
If user like the person who they search, they can exchange their SNS address using the NFC.


/시작 가이드라인 
클론 받아오는 명령
추가적으로 입력해야 하는 API 키들값 명시

/기술스택
-환경
안드로이드 스튜디오 
<img src="https://img.shields.io/badge/React-#3DDC84?style=flat&logo=React&logoColor=white"/>
깃
깃허브
파이어베이스

-개발
코틀린
node.js

-의사소통
노션

/화면구성
로그인 페이지
메인 페이지
프로필 변경 페이지
캘린터 페이지
AI 검색 페이지
chat 페이지

/주요기능
근처에 있는 사용자 검색
사용자와 의사소통
AI를 통한 smalltalk 추천
캘린더를 통한 개인적 경험 저장


2024.11.14
Connect firebase auth, firestore.
Build 3 activities Main, Login(signup), profileEdit.
Add App class for wrapper activities.
Add Hashutils for genereating unifrom userIdHash
Edit gradles and Manifest
Make repository menu for menu, topbar

Main
- Specify requiredPermissions, Bluetooth and location
- Implement permission requests
- Implement bluetooth advertising and scanning(max 12 sec)
- Build callbacks for advertising and scanning
- Made simple views with icons by vector asset
Login(signup)
- Only email sign up available
- Password length >=6
- Can use main activity only after login
- user id, userIdHash, username, profile is stored after signup until 2024.12.13
ProfileEdit
- Implement editing username and profile

# MP-Onde
