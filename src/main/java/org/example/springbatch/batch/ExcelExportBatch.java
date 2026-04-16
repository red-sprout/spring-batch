package org.example.springbatch.batch;

import lombok.RequiredArgsConstructor;
import org.example.springbatch.entity.BeforeEntity;
import org.example.springbatch.repository.BeforeRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemStreamWriter;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class ExcelExportBatch {

    private final BeforeRepository beforeRepository;

    @Bean
    public Job excelExportJob(JobRepository jobRepository, Step excelExportStep) {
        return new JobBuilder("excelExportJob", jobRepository)
                .start(excelExportStep)
                .build();
    }

    @Bean
    public Step excelExportStep(
            JobRepository jobRepository,
            @Qualifier("dataTransactionManager") PlatformTransactionManager dataTransactionManager
    ) {
        return new StepBuilder("excelExportStep", jobRepository)
                .<BeforeEntity, BeforeEntity>chunk(10)
                .transactionManager(dataTransactionManager)
                .reader(excelExportBeforeReader())
                .processor(excelExportProcessor())
                .writer(excelWriter())
                .build();
    }

    @Bean
    public RepositoryItemReader<BeforeEntity> excelExportBeforeReader() {
        return new RepositoryItemReaderBuilder<BeforeEntity>()
                .name("beforeReader")
                .pageSize(10)
                .methodName("findAll")
                .repository(beforeRepository)
                .sorts(Map.of("id", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<BeforeEntity, BeforeEntity> excelExportProcessor() {
        return item -> item;
    }

    @Bean
    public ItemStreamWriter<BeforeEntity> excelWriter() {
        try {
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

            File outputDir = new File("output");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            return new ExcelRowWriter("output/result-" + timestamp + ".xlsx");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
