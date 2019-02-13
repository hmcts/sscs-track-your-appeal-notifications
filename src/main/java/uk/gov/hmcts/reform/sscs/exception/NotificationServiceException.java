package uk.gov.hmcts.reform.sscs.exception;

import static java.lang.String.format;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class NotificationServiceException extends RuntimeException {

    public NotificationServiceException(String caseId, Exception ex) {
        super(format("Exception thrown for case [%s]", caseId), ex);
    }
}
