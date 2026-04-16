package org.example.springbatch.batch;

import lombok.RequiredArgsConstructor;
import org.example.springbatch.entity.Person;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.data.MongoPagingItemReader;
import org.springframework.batch.infrastructure.item.data.MongoItemWriter;
import org.springframework.batch.infrastructure.item.data.builder.MongoPagingItemReaderBuilder;
import org.springframework.batch.infrastructure.item.data.builder.MongoItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class MongoBatch {

    private final MongoTemplate mongoTemplate;

    @Bean
    public Job mongoJob(JobRepository jobRepository, Step mongoStep) {
        return new JobBuilder("mongoJob", jobRepository)
                .start(mongoStep)
                .build();
    }

    @Bean
    public Step mongoStep(
            JobRepository jobRepository,
            @Qualifier("dataTransactionManager") PlatformTransactionManager dataTransactionManager
    ) {
        return new StepBuilder("mongoStep", jobRepository)
                .<Person, Person>chunk(10)
                .transactionManager(dataTransactionManager)
                .reader(mongoPagingItemReader())
                .processor(mongoProcessor())
                .writer(mongoItemWriter())
                .build();
    }

    @Bean
    public MongoPagingItemReader<Person> mongoPagingItemReader() {
        Map<String, Sort.Direction> sortOptions = new HashMap<>();
        sortOptions.put("name", Sort.Direction.DESC);

        return new MongoPagingItemReaderBuilder<Person>()
                .name("mongoPagingItemReader")
                .collection("person_in")
                .targetType(Person.class)
                .template(mongoTemplate)
                .jsonQuery("{}")
                .sorts(sortOptions)
                .build();
    }

    @Bean
    public ItemProcessor<Person, Person> mongoProcessor() {
        return item -> item;
    }

    @Bean
    public MongoItemWriter<Person> mongoItemWriter() {
        return new MongoItemWriterBuilder<Person>()
                .template(mongoTemplate)
                .collection("person_out")
                .build();
    }
}
