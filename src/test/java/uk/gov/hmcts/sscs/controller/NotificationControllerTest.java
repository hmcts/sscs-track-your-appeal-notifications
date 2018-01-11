package uk.gov.hmcts.sscs.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.sscs.model.CcdCase;
import uk.gov.hmcts.sscs.service.NotificationService;


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