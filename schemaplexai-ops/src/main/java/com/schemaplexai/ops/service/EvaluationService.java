package com.schemaplexai.ops.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.ops.entity.SfEvalTask;

import java.util.List;

public interface EvaluationService extends IService<SfEvalTask> {

    /**
     * Run an evaluation task, updating its status to running.
     *
     * @param evalTaskId the evaluation task ID
     * @return the updated evaluation task
     */
    SfEvalTask runEvaluation(Long evalTaskId);

    /**
     * Get evaluation results for a task.
     *
     * @param evalTaskId the evaluation task ID
     * @return the evaluation task with results
     */
    SfEvalTask getEvaluationResults(Long evalTaskId);

    /**
     * List evaluation tasks by dataset.
     *
     * @param datasetId the dataset ID
     * @return list of evaluation tasks
     */
    List<SfEvalTask> listByDataset(Long datasetId);

    /**
     * List evaluation tasks by status.
     *
     * @param status the status code
     * @return list of evaluation tasks
     */
    List<SfEvalTask> listByStatus(Integer status);
}
