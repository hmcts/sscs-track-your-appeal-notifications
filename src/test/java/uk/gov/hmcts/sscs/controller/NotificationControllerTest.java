package uk.gov.hmcts.sscs.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.service.AuthorisationService;
import uk.gov.hmcts.sscs.service.NotificationService;

public class NotificationControllerTest {

    private NotificationController notificationController;

    private CcdResponseWrapper ccdResponseWrapper;

    @Mock
    NotificationService notificationService;

    @Mock
    AuthorisationService authorisationService;

    @Before
    public void setUp() {
        initMocks(this);

        notificationController = new NotificationController(notificationService, authorisationService);
        ccdResponseWrapper = new CcdResponseWrapper(new CcdResponse(), new CcdResponse());
    }

    @Test
    public void shouldCreateAndSendNotificationForCcdResponse() throws Exception {
        notificationController.sendNotification("", ccdResponseWrapper);
        verify(notificationService).createAndSendNotification(ccdResponseWrapper);
    }
}