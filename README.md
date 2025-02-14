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
