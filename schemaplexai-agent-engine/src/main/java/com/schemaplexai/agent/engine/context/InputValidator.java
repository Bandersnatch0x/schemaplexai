package com.schemaplexai.agent.engine.context;

import com.schemaplexai.agent.engine.tool.ValidationResult;

/**
 * 输入验证接口，用于验证用户输入和 LLM 输出。
 */
public interface InputValidator {

    /**
     * 验证输入
     * @param input 输入文本
     * @return 验证结果
     */
    ValidationResult validate(String input);
}
