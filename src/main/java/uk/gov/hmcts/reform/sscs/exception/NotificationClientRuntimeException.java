package uk.gov.hmcts.reform.sscs.exception;

import static java.lang.String.format;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class NotificationClientRuntimeException extends UnknownErrorCodeException {

    public NotificationClientRuntimeException(String caseId, Exception ex) {
        super(AlertLevel.P1, format("Exception thrown for case [%s]", caseId), ex);
    }
}
