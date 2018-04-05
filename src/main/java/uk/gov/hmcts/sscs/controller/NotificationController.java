package uk.gov.hmcts.sscs.controller;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.service.AuthorisationService;
import uk.gov.hmcts.sscs.service.NotificationService;

@RestController
public class NotificationController {

    private static final org.slf4j.Logger LOG = getLogger(NotificationController.class);

    private final NotificationService service;
    private final AuthorisationService authorisationService;

    @Autowired
    public NotificationController(NotificationService service, AuthorisationService authorisationService) {
        this.service = service;
        this.authorisationService = authorisationService;
    }

    @RequestMapping(value = "/send", method = POST, produces = APPLICATION_JSON_VALUE)
    public void sendNotification(
            @RequestHeader(AuthorisationService.SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
            @RequestBody CcdResponseWrapper ccdResponseWrapper) {
        LOG.info("Ccd Response received: ", ccdResponseWrapper);

        authorisationService.authorise(serviceAuthHeader);
        service.createAndSendNotification(ccdResponseWrapper);
    }
}
