package com.schemaplexai.agent.engine.loop;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.entity.SfAgentMemory;
import com.schemaplexai.agent.engine.mapper.SfAgentMemoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentLoopCompletionHandler {

    private final SfAgentMemoryMapper memoryMapper;

    public void handleLoop(SfAgentExecution execution) {
        log.info("Handling loop completion for execution {}", execution.getId());
        SfAgentMemory memory = new SfAgentMemory();
        memory.setAgentId(execution.getAgentId());
        memory.setMemoryType("LOOP_DETECTED");
        memory.setContent("Loop detected in execution " + execution.getId());
        memory.setSourceExecutionId(execution.getId());
        memoryMapper.insert(memory);
    }
}
