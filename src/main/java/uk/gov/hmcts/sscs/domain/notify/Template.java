package uk.gov.hmcts.sscs.domain.notify;

public class Template {

    private final String emailTemplateId;
    private final String smsTemplateId;

    public Template(String emailTemplateId, String smsTemplateId) {
        this.emailTemplateId = emailTemplateId;
        this.smsTemplateId = smsTemplateId;
    }

    public String getEmailTemplateId() {
        return emailTemplateId;
    }

    public String getSmsTemplateId() {
        return smsTemplateId;
    }
}
