package uk.gov.hmcts.reform.sscs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correspondence;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.model.LetterType;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

@Slf4j
@Component
public class SaveCorrespondenceAsyncService {
    private final CcdNotificationsPdfService ccdNotificationsPdfService;

    @Value("${feature.notification.letter.correspondence.v2:false}")
    boolean notificationLetterCorrespondenceV2Enabled;

    @Value("${feature.notification.correspondence.v2:false}")
    boolean notificationCorrespondenceV2Enabled;

    @Autowired
    public SaveCorrespondenceAsyncService(CcdNotificationsPdfService ccdNotificationsPdfService) {
        this.ccdNotificationsPdfService = ccdNotificationsPdfService;
    }

    @Async
    @Retryable(maxAttemptsExpression = "#{${letterAsync.maxAttempts}}", backoff = @Backoff(delayExpression = "#{${letterAsync.delay}}", multiplierExpression = "#{${letterAsync.multiplier}}", random = true))
    public void saveLetter(NotificationClient client, String notificationId, Correspondence correspondence, String ccdCaseId) throws NotificationClientException {
        try {
            final byte[] pdfForLetter = client.getPdfForLetter(notificationId);
            if (notificationLetterCorrespondenceV2Enabled) {
                log.info("Using merge letter correspondence V2 to upload letter correspondence for {} ", ccdCaseId);
                ccdNotificationsPdfService.mergeLetterCorrespondenceIntoCcdV2(pdfForLetter, Long.valueOf(ccdCaseId), correspondence);
            } else {
                log.info("Using existing merge letter correspondence as V2 is feature toggled off to upload letter correspondence for {} ", ccdCaseId);
                ccdNotificationsPdfService.mergeLetterCorrespondenceIntoCcd(pdfForLetter, Long.valueOf(ccdCaseId), correspondence);
            }
        } catch (NotificationClientException e) {
            if (e.getMessage().contains("PDFNotReadyError")) {
                log.info("Got a PDFNotReadyError back from gov.notify for case id: {}.", ccdCaseId);
            } else {
                log.warn("Got a strange error '{}' back from gov.notify for case id: {}.", e.getMessage(), ccdCaseId);
            }
            throw e;
        }
    }

    @Async
    @Retryable(maxAttemptsExpression = "#{${letterAsync.maxAttempts}}", backoff = @Backoff(delayExpression = "#{${letterAsync.delay}}", multiplierExpression = "#{${letterAsync.multiplier}}", random = true))
    public void saveLetter(Correspondence correspondence, final byte[] pdfForLetter, String ccdCaseId) {
        ccdNotificationsPdfService.mergeLetterCorrespondenceIntoCcd(pdfForLetter, Long.valueOf(ccdCaseId), correspondence);
    }

    @Async
    @Retryable(maxAttemptsExpression = "#{${letterAsync.maxAttempts}}", backoff = @Backoff(delayExpression = "#{${letterAsync.delay}}", multiplierExpression = "#{${letterAsync.multiplier}}", random = true))
    public void saveLetter(final byte[] pdfForLetter, Correspondence correspondence, String ccdCaseId, SubscriptionType subscriptionType) {
        if (notificationLetterCorrespondenceV2Enabled) {
            log.info("Using notification letter correspondence V2 to upload reasonable adjustments correspondence for {} ", ccdCaseId);
            ccdNotificationsPdfService.mergeReasonableAdjustmentsCorrespondenceIntoCcdV2(pdfForLetter, Long.valueOf(ccdCaseId), correspondence, LetterType.findLetterTypeFromSubscription(subscriptionType.name()));
        } else {
            log.info("Using existing notification letter correspondence as V2 is feature toggled off to upload reasonable adjustments correspondence for {} ", ccdCaseId);
            ccdNotificationsPdfService.mergeReasonableAdjustmentsCorrespondenceIntoCcd(pdfForLetter, Long.valueOf(ccdCaseId), correspondence, LetterType.findLetterTypeFromSubscription(subscriptionType.name()));
        }
    }

    @Retryable
    public void saveEmailOrSms(final Correspondence correspondence, final SscsCaseData sscsCaseData) {
        int retry = (RetrySynchronizationManager.getContext() != null) ? RetrySynchronizationManager.getContext().getRetryCount() + 1 : 1;
        log.info("Retry number {} : to upload correspondence for {}", retry, correspondence.getValue().getCorrespondenceType().name());

        if (notificationCorrespondenceV2Enabled) {
            log.info("Using notification correspondence V2 to upload correspondence for {} ", correspondence.getValue().getCorrespondenceType().name());
            ccdNotificationsPdfService.mergeCorrespondenceIntoCcdV2(Long.valueOf(sscsCaseData.getCcdCaseId()), correspondence);
        } else {
            log.info("Using existing notification correspondence as V2 is feature toggled off to upload correspondence for {} ", correspondence.getValue().getCorrespondenceType().name());
            ccdNotificationsPdfService.mergeCorrespondenceIntoCcd(sscsCaseData, correspondence);
        }
    }

    @Recover
    @SuppressWarnings({"unused"})
    public void getBackendResponseFallback(Throwable e) {
        log.error("Failed saving correspondence.", e);
    }
}
