package uk.gov.hmcts.reform.sscs.personalisation;

import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.DOCUMENT_TYPE_NAME;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.FURTHER_EVIDENCE_ACTION;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.LETTER_CONTENT_TYPE;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationMappingConstants.SENDER_NAME;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.CORRECTION_REQUEST;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.LIBERTY_TO_APPLY_REQUEST;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.PERMISSION_TO_APPEAL_REQUEST;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.STATEMENT_OF_REASONS_REQUEST;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.LetterUtils;

@Component
@Slf4j
public class ActionFurtherEvidencePersonalisation extends Personalisation<CcdNotificationWrapper> {
    @Override
    protected Map<String, Object> create(SscsCaseDataWrapper responseWrapper, SubscriptionWithType subscriptionWithType) {
        Map<String, Object> personalisation = super.create(responseWrapper, subscriptionWithType);

        NotificationEventType notificationEventType = responseWrapper.getNotificationEventType();
        SscsCaseData caseData = responseWrapper.getNewSscsCaseData();

        personalisation.put(DOCUMENT_TYPE_NAME, getPostHearingDocumentType(notificationEventType, responseWrapper.getNewSscsCaseData().getPostHearing().getRequestType()));
        personalisation.put(SENDER_NAME, LetterUtils.getNameForSender(caseData));
        personalisation.put(FURTHER_EVIDENCE_ACTION, caseData.getPostHearing().getRequestType());
        personalisation.put(LETTER_CONTENT_TYPE, LetterUtils.getNotificationTypeForActionFurtherEvidence(responseWrapper, subscriptionWithType));

        return personalisation;
    }

    private static String getPostHearingDocumentType(NotificationEventType eventType, PostHearingRequestType requestType) {
        if (CORRECTION_REQUEST.equals(eventType)) {
            return DocumentType.CORRECTION_APPLICATION.getLabel();
        } else if (LIBERTY_TO_APPLY_REQUEST.equals(eventType)) {
            return DocumentType.LIBERTY_TO_APPLY_APPLICATION.getLabel();
        } else if (STATEMENT_OF_REASONS_REQUEST.equals(eventType)) {
            return DocumentType.STATEMENT_OF_REASONS_APPLICATION.getLabel();
        } else if (PERMISSION_TO_APPEAL_REQUEST.equals(eventType)) {
            return DocumentType.PERMISSION_TO_APPEAL_APPLICATION.getLabel();
        }

        return DocumentType.SET_ASIDE_APPLICATION.getLabel();
    }
}
