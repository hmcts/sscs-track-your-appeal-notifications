package uk.gov.hmcts.reform.sscs.config;

import java.util.Locale;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.domain.notify.Link;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;

@Component
public class NotificationConfig {

    @Value("${manage.emails.link}")
    private String manageEmailsLink;
    @Value("${hmcts.phone.number}")
    private String hmctsPhoneNumber;
    @Value("${track.appeal.link}")
    private String trackAppealLink;
    @Value("${evidence.submission.info.link}")
    private String evidenceSubmissionInfoLink;
    @Value("${claiming.expenses.link}")
    private String claimingExpensesLink;
    @Value("${hearing.info.link}")
    private String hearingInfoLink;
    @Value("${online.hearing.link}")
    private String onlineHearingLink;

    private Environment env;

    NotificationConfig(@Autowired Environment env) {
        this.env = env;
    }

    public String getHmctsPhoneNumber() {
        return hmctsPhoneNumber;
    }

    public Link getManageEmailsLink() {
        return Link.builder().linkUrl(manageEmailsLink).build();
    }

    public Link getTrackAppealLink() {
        return Link.builder().linkUrl(trackAppealLink).build();
    }

    public Link getEvidenceSubmissionInfoLink() {
        return Link.builder().linkUrl(evidenceSubmissionInfoLink).build();
    }

    public Link getClaimingExpensesLink() {
        return Link.builder().linkUrl(claimingExpensesLink).build();
    }

    public Link getHearingInfoLink() {
        return Link.builder().linkUrl(hearingInfoLink).build();
    }

    public Link getOnlineHearingLinkWithEmail() {
        return Link.builder().linkUrl(onlineHearingLink + "?email={email}").build();
    }

    public String getOnlineHearingLink() {
        return onlineHearingLink;
    }

    public Template getTemplate(String emailTemplateName, String smsTemplateName, String letterTemplateName, Benefit benefit,
                                AppealHearingType appealHearingType) {
        return Template.builder()
                .emailTemplateId(getTemplate(appealHearingType, emailTemplateName, "emailId"))
                .smsTemplateId(getTemplate(appealHearingType, smsTemplateName, "smsId"))
                .smsSenderTemplateId(env.getProperty("smsSender." + benefit.toString().toLowerCase(Locale.ENGLISH)))
                .letterTemplateId(getTemplate(appealHearingType, letterTemplateName, "letterId"))
                .build();
    }

    private String getTemplate(@NotNull AppealHearingType appealHearingType, String templateName,
                               final String notificationType) {

        String templateNameWithHearing = "notification." + appealHearingType.name().toLowerCase(Locale.ENGLISH)
                + "." + templateName + "." + notificationType;

        String templateNameNoHearing = "notification." + templateName + "." + notificationType;

        return env.containsProperty(templateNameWithHearing)
                ? env.getProperty(templateNameWithHearing)
                : env.getProperty(templateNameNoHearing);
    }

}
