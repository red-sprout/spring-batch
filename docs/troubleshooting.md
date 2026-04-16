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

## 6. TransactionManager 불일치로 AfterEntity 저장 안 됨

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
