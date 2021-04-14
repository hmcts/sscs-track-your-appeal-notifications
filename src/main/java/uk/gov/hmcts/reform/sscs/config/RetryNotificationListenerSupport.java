package uk.gov.hmcts.reform.sscs.config;

import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.listener.RetryListenerSupport;

public class RetryNotificationListenerSupport extends RetryListenerSupport {

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
                                               Throwable throwable) {
        super.close(context, callback, throwable);
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
                                                 Throwable throwable) {
        super.onError(context, callback, throwable);
    }

    @Override
    public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        return super.open(context, callback);
    }
}
