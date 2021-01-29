package uk.gov.hmcts.reform.sscs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correspondence;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.model.LetterType;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

@Slf4j
@Component
public class SaveLetterCorrespondenceAsyncService {
    private final CcdNotificationsPdfService ccdNotificationsPdfService;


    @Autowired
    public SaveLetterCorrespondenceAsyncService(CcdNotificationsPdfService ccdNotificationsPdfService) {
        this.ccdNotificationsPdfService = ccdNotificationsPdfService;
    }

    @Async
    @Retryable(maxAttemptsExpression =  "#{${letterAsync.maxAttempts}}", backoff = @Backoff(delayExpression = "#{${letterAsync.delay}}", multiplierExpression = "#{${letterAsync.multiplier}}", random = true))
    public void saveLetter(NotificationClient client, String notificationId, Correspondence correspondence, String ccdCaseId) throws NotificationClientException {
        try {
            final byte[] pdfForLetter = client.getPdfForLetter(notificationId);
            ccdNotificationsPdfService.mergeLetterCorrespondenceIntoCcd(pdfForLetter, Long.valueOf(ccdCaseId), correspondence);
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
    public void saveLetter(final byte[] pdfForLetter, Correspondence correspondence, String ccdCaseId, SubscriptionType subscriptionType) {
        ccdNotificationsPdfService.mergeReasonableAdjustmentsCorrespondenceIntoCcd(pdfForLetter, Long.valueOf(ccdCaseId), correspondence, LetterType.findLetterTypeFromSubscription(subscriptionType.name()));
    }

    @Recover
    @SuppressWarnings({"unused"})
    public void getBackendResponseFallback(Throwable e) {
        log.error("Failed saving letter correspondence.", e);
    }
}
