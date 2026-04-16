package org.example.springbatch.controller;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@ResponseBody
public class MainController {

    private final JobOperator jobOperator;
    private final Job firstJob;
    private final Job secondJob;
    private final Job excelJob;
    private final Job jdbcJob;
    private final Job mongoJob;

    public MainController(
            JobOperator jobOperator,
            @Qualifier("firstJob") Job firstJob,
            @Qualifier("secondJob") Job secondJob,
            @Qualifier("excelJob") Job excelJob,
            @Qualifier("jdbcJob") Job jdbcJob,
            @Qualifier("mongoJob") Job mongoJob
    ) {
        this.jobOperator = jobOperator;
        this.firstJob = firstJob;
        this.secondJob = secondJob;
        this.excelJob = excelJob;
        this.jdbcJob = jdbcJob;
        this.mongoJob = mongoJob;
    }

    @GetMapping("/first")
    public String firstApi(@RequestParam("value") String value) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("date", value)
                .toJobParameters();

        jobOperator.start(firstJob, jobParameters);
        return "ok";
    }

    @GetMapping("/second")
    public String secondApi(@RequestParam("value") String value) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("date", value)
                .toJobParameters();

        jobOperator.start(secondJob, jobParameters);
        return "ok";
    }

    @GetMapping("/excel")
    public String excelApi(@RequestParam("value") String value) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("date", value)
                .toJobParameters();

        jobOperator.start(excelJob, jobParameters);
        return "ok";
    }

    @GetMapping("/jdbc")
    public String jdbcApi(
            @RequestParam("value") String value,
            @RequestParam("credit") Long credit
    ) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("date", value)
                .addLong("credit", credit)
                .toJobParameters();

        jobOperator.start(jdbcJob, jobParameters);
        return "ok";
    }

    @GetMapping("/mongo")
    public String mongoApi(@RequestParam("value") String value) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("date", value)
                .toJobParameters();

        jobOperator.start(mongoJob, jobParameters);
        return "ok";
    }
}
