package uk.gov.hmcts.sscs.exception;

public class AuthorisationException extends RuntimeException {

    public AuthorisationException(String message, Throwable cause) {
        super(message, cause);
    }
}
