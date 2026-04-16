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

### SecondBatch
`WinEntity`를 읽어 `win >= 10`인 레코드의 `reward` 필드를 `true`로 업데이트한다.
`RepositoryItemReader`로 조건 쿼리, `RepositoryItemWriter`로 저장한다.

### MongoBatch
`MongoPagingItemReader`로 `person_in` 컬렉션을 읽어 `person_out` 컬렉션에 저장하는 MongoDB 기반 Job.
`MongoTemplate`을 `@RequiredArgsConstructor`로 주입받는다.
`jsonQuery("{}")`로 전체 도큐먼트를 읽고 `name` 기준 내림차순 정렬한다.

### JdbcBatch
`customerCredit` 테이블에서 `credit > :credit` 조건으로 데이터를 읽어 credit을 10% 인상 후 업데이트하는 JDBC 기반 Job.
`dataDBSource`를 명시적 생성자 주입으로 받는다.
`JdbcPagingItemReader`에 `@StepScope` + `@Value("#{jobParameters['credit']}")`를 적용해 Job 파라미터를 동적으로 주입받는다.
`JdbcCursorItemReader`는 커서 방식 참고 구현으로 함께 정의되어 있다.

---

## Reader 방식 비교

| Reader 종류 | 방식 | 커넥션 | 재시작 안전 | 사용 빈 |
|---|---|---|---|---|
| `RepositoryItemReader` | JPA / 페이징 | 청크마다 새 트랜잭션 | ✅ | `beforeReader`, `winReader` |
| `JdbcCursorItemReader` | SQL / 커서 스트리밍 | Step 내내 단일 커넥션 유지 | ⚠️ (커서 재오픈 필요) | `jdbcCursorItemReader` (참고용) |
| `JdbcPagingItemReader` | SQL / 오프셋 페이징 | 청크마다 새 쿼리 | ✅ | `jdbcPagingItemReader` |
| `MongoPagingItemReader` | MongoDB / 페이징 | 청크마다 새 쿼리 | ✅ | `mongoPagingItemReader` |
