package uk.gov.hmcts.reform.sscs.config;

import java.util.*;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
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
    @Value("${mya.link}")
    private String myaLink;
    @Value("${mya.evidence.submission.info.link}")
    private String myaEvidenceSubmissionInfoLink;
    @Value("${mya.hearing.info.link}")
    private String myaHearingInfoLink;
    @Value("${mya.claiming.expenses.link}")
    private String myaClaimingExpensesLink;

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

    public Link getMyaLink() {
        return Link.builder().linkUrl(myaLink).build();
    }

    public Link getMyaEvidenceSubmissionInfoLink() {
        return Link.builder().linkUrl(myaEvidenceSubmissionInfoLink).build();
    }

    public Link getMyaHearingInfoLink() {
        return Link.builder().linkUrl(myaHearingInfoLink).build();
    }

    public Link getMyaClaimingExpensesLink() {
        return Link.builder().linkUrl(myaClaimingExpensesLink).build();
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
                                String docmosisTemplateName, Benefit benefit, AppealHearingType appealHearingType, String createdInGapsFrom, LanguagePreference languagePreference) {

        String docmosisTemplateId = getTemplateId(appealHearingType, docmosisTemplateName, "docmosisId", languagePreference);
        if (StringUtils.isNotBlank(docmosisTemplateId)) {
            if (docmosisTemplateName.split("\\.")[0].equals("appealReceived") && !State.READY_TO_LIST.getId().equals(createdInGapsFrom)) {
                docmosisTemplateId = null;
            }
        }
        return Template.builder()
            .emailTemplateId(getTemplateId(appealHearingType, emailTemplateName, "emailId", languagePreference))
            .smsTemplateId(getSmsTemplates(appealHearingType, smsTemplateName, "smsId",languagePreference))
            .smsSenderTemplateId(benefit == null ? "" : env.getProperty("smsSender." + benefit.toString().toLowerCase(Locale.ENGLISH)))
            .letterTemplateId(getTemplateId(appealHearingType, letterTemplateName, "letterId",languagePreference))
            .docmosisTemplateId(docmosisTemplateId)
            .build();
    }

    private List<String> getSmsTemplates(@NotNull AppealHearingType appealHearingType, String smsTemplateName,
                                         final String notificationType, LanguagePreference languagePreference) {
        return Optional.ofNullable(getTemplateId(appealHearingType, smsTemplateName, notificationType, languagePreference)).map(value -> {
            List<String> ids = new ArrayList<>();
            ids.add(value);
            if (LanguagePreference.WELSH.equals(languagePreference)) {
                ids.add(getTemplateId(appealHearingType, smsTemplateName, notificationType,LanguagePreference.ENGLISH));
            }
            return ids;
        }).orElse(Collections.emptyList());
    }

    private String getTemplateId(@NotNull AppealHearingType appealHearingType, String templateName,
                                 final String notificationType, LanguagePreference languagePreference) {
        String hearingTypeName = appealHearingType.name().toLowerCase(Locale.ENGLISH);
        String name = "notification." + languagePreference.getCode() + "." + hearingTypeName + "." + templateName + "."
            + notificationType;
        String templateId = env.getProperty(name);
        if (templateId == null) {
            name = "notification." + languagePreference.getCode() + "."  + templateName + "." + notificationType;
            templateId = env.getProperty(name);
        }
        return StringUtils.stripToNull(templateId);
    }

}
