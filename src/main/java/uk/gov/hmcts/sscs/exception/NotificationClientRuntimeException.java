package uk.gov.hmcts.sscs.exception;


public class NotificationClientRuntimeException extends RuntimeException {

    public NotificationClientRuntimeException(Exception ex) {
        super(ex);
    }
}
