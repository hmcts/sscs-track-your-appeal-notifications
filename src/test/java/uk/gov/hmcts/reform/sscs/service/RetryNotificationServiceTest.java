package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SYA_APPEAL_CREATED_NOTIFICATION;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.config.RetryConfig;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.service.notify.NotificationClientException;

@RunWith(JUnitParamsRunner.class)
public class RetryNotificationServiceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    private static final int MAX_RETRY = 3;
    private final Map<Integer, Integer> delayInSecondsMap = new HashMap<Integer, Integer>() {{
            put(1, 100);
            put(2, 200);
            put(3, 300);
        }};
    private final RetryConfig retryConfig = new RetryConfig();
    private SscsCaseData newSscsCaseData = SscsCaseData.builder().ccdCaseId("456").build();
    private final SscsCaseDataWrapper wrapper = SscsCaseDataWrapper.builder().state(State.APPEAL_CREATED).newSscsCaseData(newSscsCaseData).notificationEventType(SYA_APPEAL_CREATED_NOTIFICATION).build();
    private final NotificationWrapper notificationWrapper = new CcdNotificationWrapper(wrapper);
    @Mock
    private NotificationHandler notificationHandler;
    private RetryNotificationService service;

    @Before
    public void setUp() {
        service = new RetryNotificationService(notificationHandler, retryConfig);
        retryConfig.setMax(MAX_RETRY);
        retryConfig.setDelayInSeconds(delayInSecondsMap);
    }

    @Test
    @Parameters({"400", "403"})
    public void shouldNotRescheduleErrorWhenGovNotifyHttpStatus(int govNotifyHttpStatus) {
        NotificationClientException exception = mock(NotificationClientException.class);
        when(exception.getHttpResult()).thenReturn(govNotifyHttpStatus);
        service.rescheduleIfHandledGovNotifyErrorStatus(1, notificationWrapper, new NotificationServiceException("123", exception));

        verifyNoInteractions(notificationHandler);
    }

    @Test
    @Parameters({"4", "5"})
    public void shouldNotRescheduleNotificationWhenRetryAboveMaxRetry(int retry) {
        NotificationClientException exception = mock(NotificationClientException.class);
        when(exception.getHttpResult()).thenReturn(500);
        service.rescheduleIfHandledGovNotifyErrorStatus(retry, notificationWrapper, new NotificationServiceException("123", exception));

        verifyNoInteractions(notificationHandler);
    }

    @Test
    @Parameters({"-1", "0", "-2"})
    public void shouldNotRescheduleNotificationWhenRetryIsBelowOne(int retry) {
        NotificationClientException exception = mock(NotificationClientException.class);
        when(exception.getHttpResult()).thenReturn(500);
        service.rescheduleIfHandledGovNotifyErrorStatus(retry, notificationWrapper, new NotificationServiceException("123", exception));

        verifyNoInteractions(notificationHandler);
    }

    @Test
    public void shouldNotRescheduleErrorWhenCaughtExceptionIsNotAGovNotifyException() {
        NotificationServiceException error = new NotificationServiceException("123", new RuntimeException("error"));
        service.rescheduleIfHandledGovNotifyErrorStatus(1, notificationWrapper, error);
        verifyNoInteractions(notificationHandler);
    }

    @Test
    @Parameters({"500,1", "492,2", "0,3"})
    public void shouldRescheduleErrorWhenGovNotifyHttpStatusAndRetry(int govNotifyHttpStatus, int retry) {
        NotificationClientException exception = mock(NotificationClientException.class);
        when(exception.getHttpResult()).thenReturn(govNotifyHttpStatus);
        service.rescheduleIfHandledGovNotifyErrorStatus(retry, notificationWrapper, new NotificationServiceException("123", exception));
        ZonedDateTime expectedRescheduledDateTime = ZonedDateTime.now().plusSeconds(delayInSecondsMap.get(retry));
        ArgumentCaptor<ZonedDateTime> argument = ArgumentCaptor.forClass(ZonedDateTime.class);
        verify(notificationHandler).scheduleNotification(eq(notificationWrapper), eq(retry), argument.capture());
        assertTrue(argument.getValue().isBefore(expectedRescheduledDateTime) || argument.getValue().isEqual(expectedRescheduledDateTime));
    }

}
