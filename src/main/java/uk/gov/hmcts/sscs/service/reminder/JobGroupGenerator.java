package uk.gov.hmcts.sscs.service.reminder;

import org.springframework.stereotype.Component;

@Component
public class JobGroupGenerator {

    public String generate(
        String caseId,
        String group
    ) {
        return caseId + "_" + group;
    }
}
