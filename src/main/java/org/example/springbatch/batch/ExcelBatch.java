package org.example.springbatch.batch;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.example.springbatch.entity.AfterEntity;
import org.example.springbatch.repository.AfterRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.data.RepositoryItemWriter;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class ExcelBatch {

    private final AfterRepository afterRepository;

    @Bean
    public Job excelJob(JobRepository jobRepository, Step excelStep) {
        System.out.println("fourth job");

        return new JobBuilder("excelJob", jobRepository)
                .start(excelStep)
                .build();
    }

    @Bean
    public Step excelStep(
            JobRepository jobRepository,
            @Qualifier("dataTransactionManager") PlatformTransactionManager dataTransactionManager
    ) {
        return new StepBuilder("excelStep", jobRepository)
                .<Row, AfterEntity> chunk(10)
                .transactionManager(dataTransactionManager)
                .reader(excelReader())
                .processor(excelProcessor())
                .writer(excelAfterWriter())
                .build();
    }

    @Bean
    public ItemStreamReader<Row> excelReader() {
        try {
            return new ExcelRowReader("classpath:files/Book.xlsx");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public ItemProcessor<Row, AfterEntity> excelProcessor() {
        return item -> {
            AfterEntity afterEntity = new AfterEntity();
            afterEntity.setUsername(item.getCell(0).getStringCellValue());

            return afterEntity;
        };
    }

    @Bean
    public RepositoryItemWriter<AfterEntity> excelAfterWriter() {
        return new RepositoryItemWriterBuilder<AfterEntity>()
                .repository(afterRepository)
                .build();
    }
}
