package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name("approvalRequestEvent")
    label("approvalRequestEvent")
    description("Producer contract for ApprovalRequestEvent emitted by Agent-Engine when a high-risk tool call requires human approval.")

    input {
        triggeredBy('publishApprovalRequest()')
    }

    outputMessage {
        sentTo('approval.requests')
        body([
            approvalRequestId : $(uuid()),
            executionId       : $(regex('[0-9]+')),
            tenantId          : $(regex('[0-9]+')),
            agentId           : $(regex('[0-9]+')),
            triggeringSeq     : $(regex('[0-9]+')),
            requestType       : $(regex('FAST|BPMN')),
            riskLevel         : $(regex('LOW|MEDIUM|HIGH|CRITICAL')),
            actionDescription : $(regex('.+')),
            executionVersionAtPause : $(regex('[0-9]+')),
            createdAt         : $(iso8601WithOffset())
        ])
        headers {
            header('contentType', applicationJson())
            header('eventType', 'ApprovalRequestEvent')
        }
    }
}
