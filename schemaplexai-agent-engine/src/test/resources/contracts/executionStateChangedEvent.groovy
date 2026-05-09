package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name("executionStateChangedEvent")
    label("executionStateChangedEvent")
    description("Producer contract for ExecutionStateChangedEvent emitted by Agent-Engine when an execution transitions between states.")

    input {
        triggeredBy('publishStateTransition()')
    }

    outputMessage {
        sentTo('sf.agent.exec.event')
        body([
            eventId      : $(uuid()),
            executionId  : $(regex('[0-9]+')),
            seq          : $(regex('[0-9]+')),
            eventType    : $(regex('STATE_CHANGED|APPROVAL_REQUESTED|APPROVAL_GRANTED|APPROVAL_REJECTED|COMPLETED|FAILED|CANCELLED')),
            payload      : $(regex('.+')),
            occurredAt   : $(iso8601WithOffset()),
            tenantId     : $(regex('[0-9]+')),
            agentId      : $(regex('[0-9]+')),
            sensitivity  : $(regex('AUDIT|DEBUG|EPHEMERAL'))
        ])
        headers {
            header('contentType', applicationJson())
            header('eventType', 'ExecutionStateChangedEvent')
        }
    }
}
