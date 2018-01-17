package uk.gov.hmcts.sscs.domain.notify;

public enum TemplateId {

    APPEAL_RECEIVED("dd955503-42f4-45f8-a692-39377a0f340f", "");

    public final Template template;

    TemplateId(String emailId, String smsId) {
        template = new Template(emailId, smsId);
    }
}
