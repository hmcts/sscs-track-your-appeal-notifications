package uk.gov.hmcts.sscs.controller;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.service.NotificationService;

@RestController
public class NotificationController {

    private static final org.slf4j.Logger LOG = getLogger(NotificationController.class);

    private final NotificationService service;

    @Autowired
    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @RequestMapping(value = "/send", method = POST, produces = APPLICATION_JSON_VALUE)
    public void sendNotification(@RequestBody CcdResponse ccdResponse) throws Exception {
        LOG.info("Ccd Response received: " + ccdResponse);
        service.createAndSendNotification(ccdResponse);
    }
}
