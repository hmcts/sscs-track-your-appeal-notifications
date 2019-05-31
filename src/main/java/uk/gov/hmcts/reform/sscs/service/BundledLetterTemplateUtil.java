package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DIRECTION_ISSUED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.STRUCK_OUT;

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

    public BundledLetterTemplateUtil(
            @Value("${strikeOutLetterTemplate.appelant.appeal.html.template.path}") String strikeOutLetterTemplate,
            @Value("${strikeOutLetterTemplate.rep.appeal.html.template.path}") String strikeOutLetterTemplateRep,
            @Value("${directionNoticeLetterTemplate.appelant.appeal.html.template.path}") String directionNoticeLetterTemplate,
            @Value("${directionNoticeLetterTemplate.rep.appeal.html.template.path}") String directionNoticeLetterTemplateRep
    ) {
        this.strikeOutLetterTemplate = strikeOutLetterTemplate;
        this.strikeOutLetterTemplateRep = strikeOutLetterTemplateRep;
        this.directionNoticeLetterTemplate = directionNoticeLetterTemplate;
        this.directionNoticeLetterTemplateRep = directionNoticeLetterTemplateRep;
    }

    public String getBundledLetterTemplate(NotificationEventType notificationEventType, SscsCaseData newSscsCaseData, SubscriptionType subscriptionType) {
        if ((STRUCK_OUT.equals(notificationEventType)) && hasSscsDocument(newSscsCaseData)) {
            if (REPRESENTATIVE.equals(subscriptionType)) {
                return strikeOutLetterTemplateRep;
            } else {
                return strikeOutLetterTemplate;
            }
        } else if ((DIRECTION_ISSUED.equals(notificationEventType)) && (hasSscsDocument(newSscsCaseData))) {
            if (REPRESENTATIVE.equals(subscriptionType)) {
                return directionNoticeLetterTemplateRep;
            } else {
                return directionNoticeLetterTemplate;
            }
        }
        return null;
    }

    private boolean hasSscsDocument(SscsCaseData newSscsCaseData) {
        return newSscsCaseData.getSscsDocument() != null
                && !newSscsCaseData.getSscsDocument().isEmpty();
    }
}
