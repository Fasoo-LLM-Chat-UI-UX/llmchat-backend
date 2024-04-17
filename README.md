# 파수꾼 백엔드
주식회사 파수와 산학 협력을 통해 진행하는 LLM UI/UX 연구 프로젝트의 백엔드 서버입니다.

## 기술 스택
- Java 17
- Spring Boot 3.2.4
- JPA
- MySQL 8.0
- Docker

## 로컬 환경 설정
1. [JetBrains 학생 무료 라이선스](https://www.jetbrains.com/shop/eform/students/)를 발급받고, IntelliJ IDEA Ultimate를 설치합니다.
2. `./gradlew addKtlintFormatGitPreCommitHook` 명령어로 Git 커밋 시 ktlint 포맷을 적용합니다.
3. [Docker Desktop](https://www.docker.com/products/docker-desktop/)를 설치하고, `cd docker && docker-compose up -d` 명령어로 MySQL 컨테이너를 실행합니다.
4. `./gradlew bootRun` 명령어 혹은 IntelliJ IDEA에서 `MainApplication.kt`를 실행하여 서버를 실행합니다.

## API 문서 및 연동
1. 로컬 서버를 띄운 후, [Swagger UI](http://localhost:8080/swagger-ui.html) 페이지에서 API 문서를 확인할 수 있습니다.
2. [OpenAPI Generator](https://openapi-generator.tech/docs/generators)를 사용하여 클라이언트 코드를 자동 생성할 수 있습니다.

## ChatGPT API 연동
직접 ChatGPT API를 구현할 수도 있지만, Spring AI 프로젝트를 통해 ChatGPT API를 연동합니다.
적절한 수준의 추상화를 통해, 다른 AI API로 전환해야 할 때 용이할 것으로 기대합니다.

## 서버 배포
1. main 브랜치에 코드가 푸시되면 자동으로 서버에 배포됩니다. 이 프로세스는 [GitHub Actions](https://github.com/Fasoo-LLM-Chat-UI-UX/llmchat-backend/blob/main/.github/workflows/cd.yml)를 통해 진행됩니다.
