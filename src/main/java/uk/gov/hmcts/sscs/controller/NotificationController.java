package uk.gov.hmcts.sscs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.sscs.model.CcdCase;
import uk.gov.hmcts.sscs.service.NotificationService;

@RestController
public class NotificationController {

    private NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RequestMapping(value = "/send", method = RequestMethod.POST)
    public void sendNotification(@RequestBody CcdCase ccdCase) {
        notificationService.createAndSendNofitication(ccdCase);
    }
}
