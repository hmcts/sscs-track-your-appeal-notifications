package uk.gov.hmcts.sscs.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.domain.notify.Link;
import uk.gov.hmcts.sscs.domain.notify.Template;

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
    @Autowired
    private Environment env;

    public String getHmctsPhoneNumber() {
        return hmctsPhoneNumber;
    }

    public Link getManageEmailsLink() {
        return new Link(manageEmailsLink);
    }

    public Link getTrackAppealLink() {
        return new Link(trackAppealLink);
    }

    public Link getEvidenceSubmissionInfoLink() {
        return new Link(evidenceSubmissionInfoLink);
    }

    public Link getClaimingExpensesLink() {
        return new Link(claimingExpensesLink);
    }

    public Link getHearingInfoLink() {
        return new Link(hearingInfoLink);
    }

    public Template getTemplate(String emailTemplateName, String smsTemplateName) {
        return new Template(env.getProperty("notification." + emailTemplateName + ".emailId"),
                env.getProperty("notification." + smsTemplateName + ".smsId"));
    }
}
