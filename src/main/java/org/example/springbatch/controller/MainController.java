package org.example.springbatch.controller;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@ResponseBody
public class MainController {

    private final JobOperator jobOperator;
    private final JobRegistry jobRegistry;

    public MainController(JobOperator jobOperator, JobRegistry jobRegistry) {
        this.jobOperator = jobOperator;
        this.jobRegistry = jobRegistry;
    }

    @GetMapping("/first")
    public String firstApi(@RequestParam("value") String value) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("date", value)
                .toJobParameters();

        jobOperator.start(jobRegistry.getJob("firstJob"), jobParameters);

        return "ok";
    }
}
