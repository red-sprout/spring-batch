# spring-batch

Spring Batch를 알아보자!

## 기술 스택

- Java 17
- Spring Boot 4.0.5
- Spring Batch
- Spring Data JPA
- MySQL

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

### 3. 실행

```bash
./gradlew bootRun
```
