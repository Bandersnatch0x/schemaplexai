package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name("costRecordedEvent")
    label("costRecordedEvent")
    description("Producer contract for CostRecordedEvent emitted by Agent-Engine when token usage / LLM cost is recorded.")

    input {
        triggeredBy('publishCostRecorded()')
    }

    outputMessage {
        sentTo('sf.cost')
        body([
            eventId       : $(uuid()),
            executionId   : $(regex('[0-9]+')),
            tenantId      : $(regex('[0-9]+')),
            agentId       : $(regex('[0-9]+')),
            modelName     : $(regex('.+')),
            provider      : $(regex('.+')),
            requestType   : $(regex('.+')),
            inputTokens   : $(regex('[0-9]+')),
            outputTokens  : $(regex('[0-9]+')),
            totalTokens   : $(regex('[0-9]+')),
            costAmount    : $(regex('[0-9]+\\.?[0-9]*')),
            currency      : $(regex('[A-Z]{3}')),
            occurredAt    : $(iso8601WithOffset())
        ])
        headers {
            header('contentType', applicationJson())
            header('eventType', 'CostRecordedEvent')
        }
    }
}
