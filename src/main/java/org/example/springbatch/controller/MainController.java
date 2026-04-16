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

    public MainController(
            JobOperator jobOperator,
            @Qualifier("firstJob") Job firstJob,
            @Qualifier("secondJob") Job secondJob) {
        this.jobOperator = jobOperator;
        this.firstJob = firstJob;
        this.secondJob = secondJob;
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
}
