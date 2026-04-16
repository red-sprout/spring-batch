# Spring Boot 4.x + Spring Batch 6.x 트러블슈팅

> 강의 기준 버전(Spring Boot 3.x + Spring Batch 5.x)과 다른 최신 버전 사용으로 인해 발생한 문제들 정리

---

## 1. Batch Auto-Configuration 제거

**문제**
Spring Boot 4.x에서 Batch auto-configuration이 완전히 제거됨.
`spring.batch.jdbc.initialize-schema` 등 yml 설정이 동작하지 않음.

**원인**
Spring Boot 4.x는 "암묵적 자동화보다 명시적 설정" 방향으로 전환.
특히 다중 DataSource 환경에서 어떤 DataSource를 쓸지 자동 결정하는 게 예측 불가능한 동작을 유발했기 때문.

**해결**
`BatchConfig`에 `@EnableJdbcJobRepository`로 DataSource와 TransactionManager를 명시적으로 지정.
`DataSourceInitializer`로 `BATCH_*` 스키마를 직접 초기화.

```java
@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository(dataSourceRef = "metaDBSource", transactionManagerRef = "metaTransactionManager")
public class BatchConfig {

    @Bean
    public DataSourceInitializer batchSchemaInitializer(@Qualifier("metaDBSource") DataSource metaDataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("org/springframework/batch/core/schema-mysql.sql"));
        populator.setContinueOnError(true);
        ...
    }
}
```

---

## 2. Spring Batch 6.x 패키지 변경

**문제**
Spring Batch 5.x → 6.x에서 일부 클래스 패키지가 변경됨.

**변경 내역**

| 항목 | Spring Batch 5.x | Spring Batch 6.x |
|------|-----------------|-----------------|
| ItemProcessor | `org.springframework.batch.item` | `org.springframework.batch.infrastructure.item` |
| RepositoryItemReader | `org.springframework.batch.item.data` | `org.springframework.batch.infrastructure.item.data` |
| RepositoryItemWriter | `org.springframework.batch.item.data` | `org.springframework.batch.infrastructure.item.data` |
| Job | `org.springframework.batch.core.Job` | `org.springframework.batch.core.job.Job` |
| Step | `org.springframework.batch.core.Step` | `org.springframework.batch.core.step.Step` |
| JobParameters | `org.springframework.batch.core.JobParameters` | `org.springframework.batch.core.job.parameters.JobParameters` |

---

## 3. chunk() 문법 변경

**문제**
`StepBuilder.chunk(int, PlatformTransactionManager)` deprecated.

**해결**
```java
// 변경 전 (deprecated)
.<BeforeEntity, AfterEntity> chunk(10, platformTransactionManager)

// 변경 후
.<BeforeEntity, AfterEntity> chunk(10)
        .transactionManager(platformTransactionManager)
```

---

## 4. JobLauncher → JobOperator

**문제**
Spring Batch 6.x에서 `JobLauncher` deprecated.

**해결**
`JobOperator`의 `start(Job, JobParameters)` 사용.

```java
// 변경 전
jobLauncher.run(job, jobParameters);

// 변경 후
jobOperator.start(job, jobParameters);
```

---

## 5. JobRegistry 제거 → Job 직접 주입

**문제**
`JobRegistry.getJob("jobName")`으로 Job을 조회하는 방식은 구버전 패턴.

**해결**
[Spring Batch 공식 문서](https://docs.spring.io/spring-batch/reference/job/running.html) 권장 방식에 따라 `Job` Bean을 직접 주입.

```java
// 변경 전
jobOperator.start(jobRegistry.getJob("firstJob"), jobParameters);

// 변경 후
private final Job job;
jobOperator.start(job, jobParameters);
```

---

## 6. HibernateJpaVendorAdapter 사용 시 spring.jpa.* 속성 미적용

**문제**
`application.yml`의 `spring.jpa.hibernate.ddl-auto` 설정이 커스텀 EntityManager에 반영되지 않아
data_db 테이블이 자동 생성되지 않음.

**원인**
`spring.jpa.*` 속성은 Spring Boot가 자동 구성하는 `EntityManagerFactory`에만 적용됨.
`HibernateJpaVendorAdapter`를 직접 사용하는 커스텀 `LocalContainerEntityManagerFactoryBean`은
Spring Boot의 JPA 자동 구성 흐름을 타지 않아 해당 속성을 읽지 못함.

**해결**
`setJpaProperties()`로 직접 전달하되, 값은 `@Value`로 yml에서 주입.
환경별로 다른 값(`update` / `validate` / `none`)을 설정할 수 있도록 `secret.yml`에서 관리.

```java
// DataDBConfig.java
@Value("${spring.jpa.hibernate.ddl-auto:none}")  // 미설정 시 none (운영 안전)
private String ddlAuto;

Properties properties = new Properties();
properties.put("hibernate.hbm2ddl.auto", ddlAuto);
em.setJpaProperties(properties);
```

```yaml
# secret.yml — 환경별로 다르게 설정
spring:
  jpa:
    hibernate:
      ddl-auto: update   # 운영: validate 또는 none
```

---

## 7. API 호출 시 value 파라미터가 필수인 이유

**궁금증**
`GET /first?value=a` 처럼 매번 `value`를 넘겨야 하는 이유는?
파라미터 없이 `/first`만 호출하면 안 되나?

**원인**
Spring Batch는 `jobName + JobParameters` 조합으로 `JobInstance`를 식별한다.
동일한 조합으로 이미 `COMPLETED`된 `JobInstance`가 존재하면 재실행을 거부한다.

파라미터를 아예 넘기지 않으면 첫 번째 실행 성공 이후 해당 Job은 **영구적으로 재실행 불가** 상태가 된다.
`value`(→ `date` 파라미터)를 매번 다른 값으로 넘겨 새로운 `JobInstance`를 생성하는 것이 목적이다.

```
JobInstance = "firstJob" + {date=a}  → 최초 실행, COMPLETED
JobInstance = "firstJob" + {date=a}  → 재실행 거부 (이미 완료된 인스턴스)
JobInstance = "firstJob" + {date=b}  → 새 인스턴스 생성, 실행 가능
```

**스케줄러에서는**
`FirstSchedule`이 `yyyy-MM-dd-HH-mm-ss` 형식의 현재 시각을 `date`로 넣어
매 실행마다 자동으로 고유한 파라미터를 생성한다.

---

## 8. TransactionManager 불일치로 AfterEntity 저장 안 됨

**문제**
`after_entity` 테이블에 데이터가 저장되지 않음 (0행).

**원인**
`@Primary`인 `metaTransactionManager`(`DataSourceTransactionManager`)가 주입되면
`RepositoryItemWriter`는 JPA 기반이라 `dataTransactionManager`(`JpaTransactionManager`) 하에서
동작해야 하는데 트랜잭션 매니저가 달라 Writer가 커밋되지 않음.

**해결**
`firstStep()` 메서드 파라미터에 `@Qualifier("dataTransactionManager")`를 명시.
`JobRepository`와 `TransactionManager`를 생성자가 아닌 각 `@Bean` 메서드 파라미터로 주입받도록 구조 변경.

```java
// 변경 전 — 생성자 주입, @Qualifier 없음
public FirstBatch(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager, ...) { ... }

// 변경 후 — @Bean 메서드 파라미터로 주입, 명시적 @Qualifier 지정
@Bean
public Step firstStep(
        JobRepository jobRepository,
        @Qualifier("dataTransactionManager") PlatformTransactionManager dataTransactionManager) { ... }
```

---

## 9. FirstSchedule JobInstanceAlreadyCompleteException

**문제**
`FirstSchedule`에서 `JobInstanceAlreadyCompleteException` 발생.

**원인**
`SimpleDateFormat("yyyy-MM-dd-hh-mm-ss")`에서 `hh`는 12시간제(01~12)다.
오전 10시와 오후 10시에 동일한 문자열이 생성되어 같은 `JobInstance`로 인식된다.

**해결**
`HH`(24시간제)로 변경해 시각 충돌 제거.

```java
// 변경 전
SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");

// 변경 후
SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
```

---

## 10. jdbcJob credit 파라미터와 JobInstanceAlreadyCompleteException

**문제**
`/jdbc?value=a&credit=500` 호출 후 동일 URL 재호출 시 `JobInstanceAlreadyCompleteException` 발생.

**원인**
`addLong("credit", credit)`로 추가된 파라미터는 기본적으로 identifying이라
`date=a, credit=500` 조합이 이미 완료된 JobInstance와 충돌한다.

**동작**
`jdbcJob`의 JobInstance는 `date + credit` 두 파라미터 조합으로 식별된다.
재실행하려면 `value` 또는 `credit` 중 하나를 다른 값으로 변경해야 한다.

```
JobInstance = "jdbcJob" + {date=a, credit=500}  → 최초 실행, COMPLETED
JobInstance = "jdbcJob" + {date=a, credit=500}  → 재실행 거부
JobInstance = "jdbcJob" + {date=b, credit=500}  → 새 인스턴스, 실행 가능
JobInstance = "jdbcJob" + {date=a, credit=600}  → 새 인스턴스, 실행 가능
```
