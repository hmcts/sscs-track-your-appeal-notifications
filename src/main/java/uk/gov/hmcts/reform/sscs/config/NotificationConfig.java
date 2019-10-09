package uk.gov.hmcts.reform.sscs.config;

import java.util.Locale;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
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

    public Template getTemplate(String emailTemplateName, String smsTemplateName, String letterTemplateName,
                                String docmosisTemplateName, Benefit benefit, AppealHearingType appealHearingType) {

        String docmosisTemplateId = getTemplate(appealHearingType, docmosisTemplateName, "docmosisId");
        if (StringUtils.isNotBlank(docmosisTemplateId)) {
            if (!Boolean.valueOf(env.getProperty("feature.docmosis_leters." + docmosisTemplateName.split("\\.")[0] + "_on"))) {
                docmosisTemplateId = null;
            }
        }
        return Template.builder()
                .emailTemplateId(getTemplate(appealHearingType, emailTemplateName, "emailId"))
                .smsTemplateId(getTemplate(appealHearingType, smsTemplateName, "smsId"))
                .smsSenderTemplateId(benefit == null ? "" : env.getProperty("smsSender." + benefit.toString().toLowerCase(Locale.ENGLISH)))
                .letterTemplateId(getTemplate(appealHearingType, letterTemplateName, "letterId"))
                .docmosisTemplateId(docmosisTemplateId)
                .build();
    }

    private String getTemplate(@NotNull AppealHearingType appealHearingType, String templateName,
                               final String notificationType) {
        String hearingTypeName = appealHearingType.name().toLowerCase(Locale.ENGLISH);
        String name = "notification." + hearingTypeName + "." + templateName + "."
                + notificationType;
        String templateId = env.getProperty(name);
        if (templateId == null) {
            name = "notification." + templateName + "." + notificationType;
            templateId = env.getProperty(name);
        }
        return StringUtils.stripToNull(templateId);
    }

}
