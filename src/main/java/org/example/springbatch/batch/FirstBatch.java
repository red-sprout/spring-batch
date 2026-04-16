package org.example.springbatch.batch;

import org.example.springbatch.entity.AfterEntity;
import org.example.springbatch.entity.BeforeEntity;
import org.example.springbatch.repository.AfterRepository;
import org.example.springbatch.repository.BeforeRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.batch.infrastructure.item.data.RepositoryItemWriter;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

@Configuration
public class FirstBatch {

    private final BeforeRepository beforeRepository;
    private final AfterRepository afterRepository;

    public FirstBatch(BeforeRepository beforeRepository, AfterRepository afterRepository) {
        this.beforeRepository = beforeRepository;
        this.afterRepository = afterRepository;
    }

    @Bean
    public Job firstJob(JobRepository jobRepository, Step firstStep) {
        return new JobBuilder("firstJob", jobRepository)
                .start(firstStep)
                .build();
    }

    @Bean
    public Step firstStep(
            JobRepository jobRepository,
            @Qualifier("dataTransactionManager") PlatformTransactionManager dataTransactionManager
    ) {
        return new StepBuilder("firstStep", jobRepository)
                .<BeforeEntity, AfterEntity> chunk(10)
                .transactionManager(dataTransactionManager)
                .reader(beforeReader())
                .processor(beforeProcessor())
                .writer(afterWriter())
                .build();
    }

    @Bean
    public RepositoryItemReader<BeforeEntity> beforeReader() {
        return new RepositoryItemReaderBuilder<BeforeEntity>()
                .name("beforeReader")
                .repository(beforeRepository)
                .methodName("findAll")
                .pageSize(10)
                .sorts(Map.of("id", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<BeforeEntity, AfterEntity> beforeProcessor() {
        return before -> {
            AfterEntity after = new AfterEntity();
            after.setUsername(before.getUsername());
            return after;
        };
    }

    @Bean
    public RepositoryItemWriter<AfterEntity> afterWriter() {
        return new RepositoryItemWriterBuilder<AfterEntity>()
                .repository(afterRepository)
                .build();
    }
}
