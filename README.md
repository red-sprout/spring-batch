# spring-batch

Spring Batch를 알아보자!

## 기술 스택

- Java 17
- Spring Boot 4.0.5
- Spring Batch
- Spring Data JPA
- MySQL
- Apache POI (Excel 처리)

## 프로젝트 설정

### 1. secret.yml 생성

민감 정보(DB 접속 정보)는 `secret.yml`로 분리하여 관리합니다.  
해당 파일은 `.gitignore`에 등록되어 있으므로, 직접 생성해야 합니다.

`src/main/resources/secret.yml` 파일을 아래 내용을 참고하여 생성하세요.

```yaml
spring:
  datasource-meta:
    driver-class-name: com.mysql.cj.jdbc.Driver
    jdbc-url: jdbc:mysql://127.0.0.1:3306/meta_db?useSSL=false&useUnicode=true&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true
    username: your_username
    password: your_password

  datasource-data:
    driver-class-name: com.mysql.cj.jdbc.Driver
    jdbc-url: jdbc:mysql://127.0.0.1:3306/data_db?useSSL=false&useUnicode=true&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true
    username: your_username
    password: your_password

  jpa:
    hibernate:
      ddl-auto: update  # 운영 환경에서는 validate 또는 none으로 변경
```

### 2. DB 생성

```sql
CREATE DATABASE meta_db;
CREATE DATABASE data_db;
```

### 3. Excel 파일 준비

Excel Job은 `src/main/resources/files/` 경로의 파일을 읽습니다.  
해당 디렉토리에 처리할 `.xlsx` 파일을 위치시키세요.

```
src/main/resources/files/Book.xlsx
```

### 4. 실행

```bash
./gradlew bootRun
```

---

## API

| 엔드포인트 | Job | 설명 |
|---|---|---|
| `GET /first?value=` | firstJob | before_entity → after_entity 마이그레이션 |
| `GET /second?value=` | secondJob | - |
| `GET /excel?value=` | excelJob | Excel 파일 읽기 처리 |

`value`는 Job 중복 실행 방지를 위한 고유 식별자로 매 호출마다 다른 값을 사용해야 합니다.
