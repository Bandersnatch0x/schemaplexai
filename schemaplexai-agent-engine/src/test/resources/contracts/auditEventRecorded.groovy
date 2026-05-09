package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name("auditEventRecorded")
    label("auditEventRecorded")
    description("Producer contract for AuditEventRecorded emitted by Agent-Engine via ExecutionEventMessage for audit projection.")

    input {
        triggeredBy('publishAuditEvent()')
    }

    outputMessage {
        sentTo('sf.agent.exec.event')
        body([
            eventId      : $(uuid()),
            executionId  : $(regex('[0-9]+')),
            seq          : $(regex('[0-9]+')),
            eventType    : $(regex('.+')),
            payload      : $(regex('.+')),
            occurredAt   : $(iso8601WithOffset()),
            tenantId     : $(regex('[0-9]+')),
            agentId      : $(regex('[0-9]+')),
            sensitivity  : 'AUDIT'
        ])
        headers {
            header('contentType', applicationJson())
            header('eventType', 'AuditEventRecorded')
        }
    }
}
