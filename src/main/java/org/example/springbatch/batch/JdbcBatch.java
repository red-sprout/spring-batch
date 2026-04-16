package org.example.springbatch.batch;

import org.example.springbatch.entity.CustomerCredit;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader;
import org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader;
import org.springframework.batch.infrastructure.item.database.Order;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class JdbcBatch {

    private final DataSource dataDBSource;

    public JdbcBatch(@Qualifier("dataDBSource") DataSource dataDBSource) {
        this.dataDBSource = dataDBSource;
    }

    @Bean
    public Job jdbcJob(JobRepository jobRepository, Step jdbcStep) {
        return new JobBuilder("jdbcJob", jobRepository)
                .start(jdbcStep)
                .build();
    }

    @Bean
    public Step jdbcStep(
            JobRepository jobRepository,
            @Qualifier("dataTransactionManager") PlatformTransactionManager dataTransactionManager,
            JdbcPagingItemReader<CustomerCredit> jdbcPagingItemReader
    ) throws Exception {
        return new StepBuilder("jdbcStep", jobRepository)
                .<CustomerCredit, CustomerCredit>chunk(10)
                .transactionManager(dataTransactionManager)
                .reader(jdbcPagingItemReader)
                .processor(jdbcProcessor())
                .writer(jdbcBatchItemWriter())
                .build();
    }

    @Bean
    public RowMapper<CustomerCredit> customerCreditRowMapper() {
        return (ResultSet rs, int rowNum) -> {
            CustomerCredit credit = new CustomerCredit();
            credit.setId(rs.getLong("id"));
            credit.setName(rs.getString("name"));
            credit.setCredit(rs.getLong("credit"));
            return credit;
        };
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<CustomerCredit> jdbcPagingItemReader(
            @Value("#{jobParameters['credit']}") Long credit
    ) throws Exception {
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("credit", credit);

        return new JdbcPagingItemReaderBuilder<CustomerCredit>()
                .name("jdbcPagingItemReader")
                .dataSource(dataDBSource)
                .selectClause("SELECT id, name, credit")
                .fromClause("FROM customerCredit")
                .whereClause("WHERE credit > :credit")
                .sortKeys(Map.of("id", Order.ASCENDING))
                .rowMapper(customerCreditRowMapper())
                .parameterValues(parameterValues)
                .pageSize(10)
                .build();
    }

    @Bean
    @StepScope
    public JdbcCursorItemReader<CustomerCredit> jdbcCursorItemReader(
            @Value("#{jobParameters['credit']}") Long credit
    ) {
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("credit", credit);

        return new JdbcCursorItemReaderBuilder<CustomerCredit>()
                .name("jdbcCursorItemReader")
                .dataSource(dataDBSource)
                .sql("SELECT id, name, credit FROM customerCredit WHERE credit > :credit ORDER BY id ASC")
                .queryArguments(parameterValues)
                .rowMapper(customerCreditRowMapper())
                .fetchSize(100)
                .build();
    }

    @Bean
    public ItemProcessor<CustomerCredit, CustomerCredit> jdbcProcessor() {
        return item -> {
            item.setCredit((long) (item.getCredit() * 1.1));
            return item;
        };
    }

    @Bean
    public JdbcBatchItemWriter<CustomerCredit> jdbcBatchItemWriter() throws Exception {
        return new JdbcBatchItemWriterBuilder<CustomerCredit>()
                .dataSource(dataDBSource)
                .sql("UPDATE customerCredit SET credit = :credit WHERE id = :id")
                .beanMapped()
                .build();
    }
}
