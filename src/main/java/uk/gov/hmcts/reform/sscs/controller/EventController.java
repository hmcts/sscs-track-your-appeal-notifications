package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.sscs.service.AuthorisationService.SERVICE_AUTHORISATION_HEADER;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.factory.TcaNotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.EventHandler;

@RestController
@Slf4j
public class EventController {

    private final EventHandler eventHadler;
    private final AuthorisationService authorisationService;

    @Autowired
    public EventController(EventHandler eventHadler, AuthorisationService authorisationService) {
        this.eventHadler = eventHadler;
        this.authorisationService = authorisationService;
    }

    @PostMapping(value = "/send-event", produces = APPLICATION_JSON_VALUE)
    void submitEvent(
            @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
            @RequestBody SscsCaseDataWrapper sscsCaseDataWrapper) {

        log.info("Submit event for case id: {} , {}", sscsCaseDataWrapper.getNewSscsCaseData().getCcdCaseId(),
                sscsCaseDataWrapper.getNotificationEventType());

        authorisationService.authorise(serviceAuthHeader);
        eventHadler.scheduleEvent(new TcaNotificationWrapper(sscsCaseDataWrapper));

        log.info("Event submitted for case id: {} , {}", sscsCaseDataWrapper.getNewSscsCaseData().getCcdCaseId(),
                sscsCaseDataWrapper.getNotificationEventType());
    }

}
