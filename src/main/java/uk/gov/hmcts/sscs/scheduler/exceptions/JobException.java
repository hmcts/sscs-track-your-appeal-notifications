package uk.gov.hmcts.sscs.scheduler.exceptions;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

/**
 * SonarQube reports as error. Max allowed - 5 parents
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class JobException extends UnknownErrorCodeException {

    public JobException(String message, Throwable cause) {
        super(AlertLevel.P1, message, cause);
    }
}
