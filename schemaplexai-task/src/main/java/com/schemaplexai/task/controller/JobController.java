package com.schemaplexai.task.controller;

import com.schemaplexai.common.controller.BaseController;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.service.JobService;
import com.schemaplexai.task.vo.JobPageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Async job board REST endpoints ({@code schemaplexai-ui/src/api/task.ts}).
 * Jobs are projected from {@code sf_message_fail_log}; retry delegates to the
 * existing dead letter replay service.
 */
@RestController
@RequestMapping("/task/jobs")
@RequiredArgsConstructor
public class JobController extends BaseController {

    private final JobService jobService;

    @GetMapping
    public Result<JobPageResult> listJobs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String queue) {
        return success(jobService.listJobs(page, pageSize, queue));
    }

    @PostMapping("/{id}/retry")
    public Result<Void> retryJob(@PathVariable String id) {
        jobService.retryJob(parseId(id));
        return success();
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancelJob(@PathVariable String id) {
        jobService.cancelJob(parseId(id));
        return success();
    }

    private Long parseId(String id) {
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException e) {
            throw new BaseException(ResultCode.PARAM_ERROR, "invalid id: " + id);
        }
    }
}
