# Spring Batch 동작 흐름

## 트리거

Job은 두 가지 방법으로 실행된다.

- **API 호출**: `GET /first?value=...` 요청이 들어오면 `MainController`가 `JobOperator.start()`를 호출
- **스케줄러**: `FirstSchedule`이 매분 10초에 자동으로 `JobOperator.start()`를 호출

---

## Job 실행

`JobOperator`가 `firstJob`을 실행한다. `JobParameters`에 `date` 값을 담아 전달하는데,
같은 파라미터로 이미 완료된 Job이 있으면 재실행되지 않는다. (Spring Batch 중복 실행 방지)

---

## Step 실행 (Chunk 방식)

`firstJob`은 `firstStep` 하나로 구성되어 있으며, **chunk size = 10** 으로 동작한다.

chunk 방식이란 데이터를 한 번에 처리하지 않고 지정한 크기만큼 잘라서 처리하는 방식이다.
10건을 읽고 → 10건을 변환하고 → 10건을 저장하는 것을 데이터가 없을 때까지 반복한다.

### Reader
`RepositoryItemReader`가 `before_entity` 테이블에서 데이터를 10건씩 페이징하여 읽는다.
`id` 기준 오름차순 정렬로 가져온다.

### Processor
`BeforeEntity`를 받아 `AfterEntity`로 변환한다.
현재는 `username` 필드를 그대로 복사하는 단순한 변환 로직이다.

### Writer
변환된 `AfterEntity` 10건을 `after_entity` 테이블에 저장한다.
`methodName`을 별도 지정하지 않아 `RepositoryItemWriter` 기본 동작인 `saveAll`을 사용한다.

---

## DataSource 역할 분리

### meta_db
Spring Batch가 Job 실행 이력을 관리하기 위해 사용하는 DB다.
아래 테이블들이 자동 생성된다.

- `BATCH_JOB_INSTANCE`: Job 실행 단위 (jobName + jobParameters 조합)
- `BATCH_JOB_EXECUTION`: 실제 실행 기록 (시작/종료 시간, 상태)
- `BATCH_JOB_EXECUTION_PARAMS`: 실행 시 사용된 파라미터
- `BATCH_STEP_EXECUTION`: Step 단위 실행 기록
- `BATCH_STEP_EXECUTION_CONTEXT`: Step 재시작을 위한 상태 저장

### data_db
실제 비즈니스 데이터가 저장되는 DB다.
Spring Data JPA를 통해 `before_entity`, `after_entity` 테이블을 관리한다.

---

## 설정 구조

### MetaDBConfig
meta_db DataSource(`metaDBSource`)와 TransactionManager(`metaTransactionManager`) 정의.
`@Primary`로 지정되어 있어 명시적 지정 없이 주입 시 이 빈이 선택된다.

### BatchConfig
Spring Batch 인프라 설정 담당.
`@EnableBatchProcessing` + `@EnableJdbcJobRepository`로 `metaDBSource`를 사용하는
JDBC 기반 `JobRepository`를 구성한다.
`DataSourceInitializer`로 애플리케이션 시작 시 `BATCH_*` 테이블을 자동 생성한다.

### DataDBConfig
비즈니스 데이터 JPA 설정 담당.
`HibernateJpaVendorAdapter`로 `dataDBSource`에 대한 JPA 설정을 구성하고,
`@EnableJpaRepositories`로 `BeforeRepository`, `AfterRepository`가 이 설정을 사용하도록 연결한다.
Step의 트랜잭션은 이 설정의 `dataTransactionManager`(`JpaTransactionManager`)를 사용한다.

### FirstBatch
Job / Step / Reader / Processor / Writer 정의에 집중.
`JobRepository`와 `PlatformTransactionManager`는 생성자가 아닌 각 `@Bean` 메서드 파라미터로 주입받는다.
`firstStep()`에서 `@Qualifier("dataTransactionManager")`로 JPA 트랜잭션 매니저를 명시적으로 지정한다.
