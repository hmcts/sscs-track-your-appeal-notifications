package uk.gov.hmcts.reform.sscs.domain.notify;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Map;
import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.reform.sscs.utility.PhoneNumbersUtil;

@Value
@Builder
public class Notification {

    private final Template template;
    private final Destination destination;
    private final Map<String, String> placeholders;
    private final Reference reference;
    private final String appealNumber;

    public Notification(Template template, Destination destination, Map<String, String> placeholders, Reference reference, String appealNumber) {
        this.template = template;
        this.destination = destination;
        this.placeholders = placeholders;
        this.reference = reference;
        this.appealNumber = appealNumber;
    }

    public boolean isEmail() {
        return isNotBlank(destination.email);
    }

    public boolean isSms() {
        return isNotBlank(destination.sms) && PhoneNumbersUtil.isValidUkMobileNumber(destination.sms);
    }

    public String getEmailTemplate() {
        return template.getEmailTemplateId();
    }

    public String getSmsTemplate() {
        return template.getSmsTemplateId();
    }

    public String getLetterTemplate() {
        return template.getLetterTemplateId();
    }

    public String getSmsSenderTemplate() {
        return template.getSmsSenderTemplateId();
    }

    public String getEmail() {
        return destination.email;
    }

    public String getMobile() {
        return PhoneNumbersUtil.cleanPhoneNumber(destination.sms).orElse(destination.sms);
    }

    public Map<String, String> getPlaceholders() {
        return placeholders;
    }

    public String getReference() {
        return reference.value;
    }

    public String getAppealNumber() {
        return appealNumber;
    }

}
