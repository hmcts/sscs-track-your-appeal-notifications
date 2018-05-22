package uk.gov.hmcts.sscs.scheduler.service;

import org.quartz.SchedulerException;
import uk.gov.hmcts.sscs.scheduler.exceptions.JobException;

@SuppressWarnings("HideUtilityClassConstructor")
public final class SchedulerExceptionHandlingHelper {

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws SchedulerException;
    }

    /**
     * Calls provided function and wraps SchedulerException in checked exception if it fails.
     *
     * @param throwingSupplier just like Supplier, but can throw SchedulerException
     * @return result of calling supplier. Throws unchecked exception if it failed.
     */
    public static <T> T call(ThrowingSupplier<T> throwingSupplier) {
        try {
            return throwingSupplier.get();
        } catch (SchedulerException exc) {
            throw new JobException(exc.getMessage(), exc);
        }
    }
}
