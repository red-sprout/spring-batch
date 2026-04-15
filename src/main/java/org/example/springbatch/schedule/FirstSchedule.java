package org.example.springbatch.schedule;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

@Configuration
public class FirstSchedule {

    private final JobOperator jobOperator;
    private final JobRegistry jobRegistry;

    public FirstSchedule(JobOperator jobOperator, JobRegistry jobRegistry) {
        this.jobOperator = jobOperator;
        this.jobRegistry = jobRegistry;
    }

    @Scheduled(cron = "10 * * * * *", zone = "Asia/Seoul")
    public void runFirstJob() throws Exception {
        System.out.println("first schedule start");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        String date = dateFormat.format(new Date());

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("date", date)
                .toJobParameters();

        jobOperator.start(Objects.requireNonNull(jobRegistry.getJob("firstJob")), jobParameters);
    }
}
