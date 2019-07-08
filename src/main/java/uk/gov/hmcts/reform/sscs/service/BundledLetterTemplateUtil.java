package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;

@Component
public class BundledLetterTemplateUtil {

    private final String strikeOutLetterTemplate;
    private final String strikeOutLetterTemplateRep;
    private final String directionNoticeLetterTemplate;
    private final String directionNoticeLetterTemplateRep;
    private final String validAppealCreatedLetterTemplate;
    private final String validAppealCreatedLetterTemplateRep;

    public BundledLetterTemplateUtil(
            @Value("${strikeOutLetterTemplate.appelant.appeal.html.template.path}") String strikeOutLetterTemplate,
            @Value("${strikeOutLetterTemplate.rep.appeal.html.template.path}") String strikeOutLetterTemplateRep,
            @Value("${directionNoticeLetterTemplate.appelant.appeal.html.template.path}") String directionNoticeLetterTemplate,
            @Value("${directionNoticeLetterTemplate.rep.appeal.html.template.path}") String directionNoticeLetterTemplateRep,
            @Value("${validAppealCreatedLetterTemplate.appellant.appeal.html.template.path}") String validAppealCreatedLetterTemplate,
            @Value("${validAppealCreatedLetterTemplate.rep.appeal.html.template.path}") String validAppealCreatedLetterTemplateRep

    ) {
        this.strikeOutLetterTemplate = strikeOutLetterTemplate;
        this.strikeOutLetterTemplateRep = strikeOutLetterTemplateRep;
        this.directionNoticeLetterTemplate = directionNoticeLetterTemplate;
        this.directionNoticeLetterTemplateRep = directionNoticeLetterTemplateRep;
        this.validAppealCreatedLetterTemplate = validAppealCreatedLetterTemplate;
        this.validAppealCreatedLetterTemplateRep = validAppealCreatedLetterTemplateRep;
    }

    public String getBundledLetterTemplate(NotificationEventType notificationEventType, SscsCaseData newSscsCaseData, SubscriptionType subscriptionType) {
        if ((STRUCK_OUT.equals(notificationEventType)) && newSscsCaseData != null && newSscsCaseData.getSscsStrikeOutDocument() != null) {
            return (REPRESENTATIVE.equals(subscriptionType)) ? strikeOutLetterTemplateRep : strikeOutLetterTemplate;
        } else if ((DIRECTION_ISSUED.equals(notificationEventType)) && newSscsCaseData != null && newSscsCaseData.getSscsInterlocDirectionDocument() != null) {
            return (REPRESENTATIVE.equals(subscriptionType)) ? directionNoticeLetterTemplateRep : directionNoticeLetterTemplate;
        } else if ((JUDGE_DECISION_APPEAL_TO_PROCEED.equals(notificationEventType) || TCW_DECISION_APPEAL_TO_PROCEED.equals(notificationEventType))
                && (newSscsCaseData != null && newSscsCaseData.getSscsInterlocDecisionDocument() != null)) {
            return (REPRESENTATIVE.equals(subscriptionType)) ? validAppealCreatedLetterTemplateRep : validAppealCreatedLetterTemplate;
        }
        return null;
    }

    private boolean hasSscsDocument(SscsCaseData newSscsCaseData) {
        return newSscsCaseData.getSscsDocument() != null
                && !newSscsCaseData.getSscsDocument().isEmpty();
    }
}
