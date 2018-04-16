package uk.gov.hmcts.sscs.domain.notify;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Template {

    private final String emailTemplateId;
    private final String smsTemplateId;

}
