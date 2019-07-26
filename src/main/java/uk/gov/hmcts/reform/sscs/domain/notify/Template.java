package uk.gov.hmcts.reform.sscs.domain.notify;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Template {
    private final String emailTemplateId;
    private final String smsTemplateId;
    private final String smsSenderTemplateId;
    private final String letterTemplateId;
    private final String docmosisTemplateId;
}
