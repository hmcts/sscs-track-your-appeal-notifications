package uk.gov.hmcts.sscs.controller;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.sscs.domain.CcdCase;
import uk.gov.hmcts.sscs.service.NotificationService;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;


public class NotificationControllerTest {

    @Mock
    private CcdCase ccdCase;

    @Mock
    private NotificationService notificationService;

    private NotificationController notificationController;

    @Before
    public void setUp() throws Exception {

        initMocks(this);
        notificationController = new NotificationController(notificationService);
    }

    @Test
    public void shouldSendNotificationForSendRequest() throws Exception {
        notificationController.sendNotification(ccdCase);

        verify(notificationService).createAndSendNofitication(ccdCase);
    }
}