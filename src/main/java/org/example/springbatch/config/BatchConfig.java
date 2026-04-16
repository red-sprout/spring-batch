package org.example.springbatch.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Isolation;

import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository(
        dataSourceRef = "metaDBSource",
        transactionManagerRef = "metaTransactionManager",
        isolationLevelForCreate = Isolation.DEFAULT
)
public class BatchConfig {

    @Bean
    public DataSourceInitializer batchSchemaInitializer(@Qualifier("metaDBSource") DataSource metaDataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("org/springframework/batch/core/schema-mysql.sql"));
        populator.setContinueOnError(true);

        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(metaDataSource);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }
}