package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name("approvalDecisionEvent")
    label("approvalDecisionEvent")
    description("Producer contract for ApprovalDecisionEvent emitted by Core (quality module) when a human approver makes a decision.")

    input {
        triggeredBy('publishApprovalDecision()')
    }

    outputMessage {
        sentTo('approval.decisions')
        body([
            ticketId                   : $(uuid()),
            executionId                : $(regex('[0-9]+')),
            action                     : $(regex('APPROVE|REJECT|ESCALATE')),
            approverId                 : $(regex('.+')),
            reason                     : $(regex('.*')),
            decidedAt                  : $(iso8601WithOffset()),
            decisionVersion            : $(regex('[0-9]+')),
            expectedExecutionVersion   : $(regex('[0-9]+'))
        ])
        headers {
            header('contentType', applicationJson())
            header('eventType', 'ApprovalDecisionEvent')
        }
    }
}
