# 💻 BonScore (Bons-Pops) Framework Project
> **Spring Boot 3 & Vue 3 기반의 고도화된 웹 애플리케이션 프레임워크 및 맛집 기록 시스템**

### **Back-end**
<p>
  <img src="https://img.shields.io/badge/Java_17-007396?style=for-the-badge&logo=java&logoColor=white">
  <img src="https://img.shields.io/badge/Spring_Boot_3.4.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
  <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white">
  <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white">
</p>

### **Persistence & Database**
<p>
  <img src="https://img.shields.io/badge/Oracle_19c-F80000?style=for-the-badge&logo=oracle&logoColor=white">
  <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white">
  <img src="https://img.shields.io/badge/JPA_Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white">
  <img src="https://img.shields.io/badge/MyBatis-C4A764?style=for-the-badge&logo=mybatis&logoColor=white">
</p>

### **Front-end**
<p>
  <img src="https://img.shields.io/badge/Vue.js_3-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white">
  <img src="https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white">
  <img src="https://img.shields.io/badge/Element_Plus-409EFF?style=for-the-badge&logo=elementplus&logoColor=white">
</p>

### **Tools & Dev Environment**
<p>
  <img src="https://img.shields.io/badge/IntelliJ_IDEA-000000?style=for-the-badge&logo=intellijidea&logoColor=white">
  <img src="https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white">
  <img src="https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white">
</p>

---

## 🎯 Project Overview
본 프로젝트는 단순한 기능 구현을 넘어 **"지속 가능한 코드"**와 **"엔터프라이즈급 보안"**을 목표로 설계된 웹 어플리케이션입니다. 과거 프로젝트의 한계를 극복하기 위해 계층형 아키텍처(Layered Architecture)를 엄격히 준수하며, 공통 프레임워크 모듈화를 통해 확장성을 확보했습니다.

### 핵심 목표
- **Framework Depth**: Spring Boot 3의 내부 동작 원리 및 JPA 영속성 컨텍스트 심층 활용
- **Security First**: 시큐어 코딩(XSS, SQL Injection 방어) 및 복합 인증 체계 구현
- **Scalability**: MyBatis에서 JPA로의 성공적인 기술 전환 및 공통 모듈(AOP, Exception) 구축

---

## 🏗️ System Architecture
백엔드와 프론트엔드의 명확한 관심사 분리(SoC)를 지향하는 계층형 구조입니다.

```text
       [ Browser ]                      [ Server (Spring Boot 3) ]
            |                                       |
    +-------v-------+               +---------------v---------------+
    |   Vue.js 3    |               |       Controller Layer        |
    | (TypeScript)  | <---REST--->  | (API Request/Response Handle) |
    +-------+-------+               +---------------|---------------+
            |                                       |
    +-------v-------+               +---------------v---------------+
    | Element Plus  |               |        Service Layer          |
    |   AG-Grid     |               |  (Business Logic & Auth)      |
    +---------------+               +---------------|---------------+
                                                    |
    +---------------+               +---------------v---------------+
    |     Redis     | <-----------  |       Repository Layer        |
    | (Session/Cache)|               |   (Spring Data JPA/MyBatis)   |
    +---------------+               +---------------|---------------+
                                                    |
                                            +-------v-------+
                                            | Oracle DB 19c |
                                            +---------------+
```

---

## 🛠 Tech Stack
### **Back-end**
- **Core**: Java 17, Spring Boot 3.4.1, Gradle
- **Persistence**: **Spring Data JPA (Hibernate)**, MyBatis (SqlSession 제어)
- **Security**: Spring Security, JWT/Session Hybrid, RSA 비대칭키 암호화, reCAPTCHA
- **Infrastructure**: Oracle DB, Redis (Cache/Dormant User), Google SMTP

### **Front-end**
- **Framework**: Vue.js 3 (Composition API), TypeScript
- **State/UI**: Pinia, Element Plus, AG-Grid (Enterprise급 대용량 처리)

---

## 🔐 Security Framework (Main Focus)
본 프로젝트는 보안 취약점 대응을 최우선 과제로 삼고 다음과 같은 기능을 프레임워크 레벨에서 구현했습니다.

- **인증/인가**: 세션 기반 인증 정보를 Security Context와 연동 및 URL 단위 접근 제어(AOP)
- **XSS 방어**: `XssEscapeFilter` 및 `ServletRequestWrapper`를 통한 입출력 값 정제
- **중복 로그인 제어**: Redis를 활용한 세션 관리로 동시 접속 차단 및 보안 강화
- **암호화 표준**: 비밀번호 단방향 해시(BCrypt), 민감 데이터 RSA 비대칭키 암호화 전송
- **기타**: reCAPTCHA 기반 봇 검증 및 유출된 비밀번호 사용 방지 로직 적용

---

## 📂 Directory Structure
도메인 기반 패키지 구조와 계층형 아키텍처를 결합하여 유지보수성을 극대화했습니다.

```text
src/main/java/com/koo/bonscore/
├── biz/                # 비즈니스 도메인
│   ├── auth/           # 인증(Authentication)
│   ├── authorization/  # 인가(Authorization)
│   ├── gourmet/        # 맛집 기록 도메인 (Core Business)
│   └── users/          # 사용자 정보 관리
├── common/             # 공통 모듈 (File, API, Masking)
├── core/               # 애플리케이션 핵심 기반
│   ├── aop/            # 로깅, 중복요청 방지
│   ├── config/         # JPA, Security, Redis 설정
│   └── exception/      # Global Exception Handler
└── log/                # 활동 로그 및 유틸리티
```

---

## 🔄 Persistence Evolution: MyBatis to JPA
초기 MyBatis 중심의 설계에서 데이터 정합성과 객체 지향 모델링을 위해 **Spring Data JPA**로 전환을 완료했습니다.

- **MyBatis**: 복잡한 통계 쿼리 및 세밀한 SQL 제어가 필요한 영역에 활용
- **JPA**: 생산성 향상, 엔티티 Auditing(`@EnableJpaAuditing`), 영속성 컨텍스트를 통한 데이터 일관성 보장
- **Optimization**: `open-in-view: false` 설정을 통한 DB 커넥션 리소스 최적화

---

## 📝 Gourmet Features (Service)
- **Gourmet Records**: AG-Grid를 활용한 개인별 맛집 기록 리스트 및 필터링
- **Image Handling**: 맛집 이미지 업로드 및 공통 파일 처리 모듈 연동
- **Activity Log**: Aspect(AOP)를 활용한 사용자 행위 로그 실시간 기록

---

## 🚀 Getting Started
```bash
# Backend Setup (Java 17 & Gradle 8.11+)
./gradlew bootRun

# Frontend Setup (Node.js LTS)
npm install
npm run dev
```

---
```text
________                         ________              _____
___  __ )____________________    __  ___/_________________(_)_____________ _
__  __  |  __ \_  __ \_  ___/    _____ \___  __ \_  ___/_  /__  __ \_  __ `/
_  /_/ // /_/ /  / / /(__  )     ____/ /__  /_/ /  /   _  / _  / / /  /_/ /
/_____/ \____//_/ /_//____/      /____/ _  .___//_/    /_/  /_/ /_/_\__, /
                                        /_/                        /____/
:: BonScore :: Powered by Spring Boot 3.4.1
```
