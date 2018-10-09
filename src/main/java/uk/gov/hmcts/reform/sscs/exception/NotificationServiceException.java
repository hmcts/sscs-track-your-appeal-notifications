package uk.gov.hmcts.reform.sscs.exception;

import static java.lang.String.format;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class NotificationServiceException extends UnknownErrorCodeException {

    public NotificationServiceException(String caseId, Exception ex) {
        super(AlertLevel.P3, format("Exception thrown for case [%s]", caseId), ex);
    }
}
