package uk.gov.hmcts.reform.sscs.controller;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.getNotificationByCcdEvent;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.NotificationService;

@RestController
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthorisationService authorisationService;
    private final SscsCaseCallbackDeserializer deserializer;

    @Autowired
    public NotificationController(NotificationService notificationService,
                                  AuthorisationService authorisationService,
                                  SscsCaseCallbackDeserializer deserializer) {
        this.notificationService = notificationService;
        this.authorisationService = authorisationService;
        this.deserializer = deserializer;
    }

    @PostMapping(value = "/send", produces = APPLICATION_JSON_VALUE)
    public void sendNotification(
            @RequestHeader(AuthorisationService.SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
            @RequestBody String message) {
        try {
            Callback<SscsCaseData> callback = deserializer.deserialize(message);

            CaseDetails<SscsCaseData> caseDetailsBefore = callback.getCaseDetailsBefore().orElse(null);

            SscsCaseDataWrapper sscsCaseDataWrapper = buildSscsCaseDataWrapper(
                callback.getCaseDetails().getCaseData(),
                caseDetailsBefore != null ? caseDetailsBefore.getCaseData() : null,
                getNotificationByCcdEvent(callback.getEvent()),
                callback.getCaseDetails().getState());

            if (NotificationEventType.ACTION_FURTHER_EVIDENCE.equals(sscsCaseDataWrapper.getNotificationEventType())
                && emptyIfNull(callback.getCaseDetails().getCaseData().getScannedDocuments()).stream()
                .noneMatch(doc -> doc.getValue() != null && StringUtils.isNotBlank(doc.getValue().getType())
                    && DocumentType.SET_ASIDE_APPLICATION.getValue().equals(doc.getValue().getType()))) {
                log.info("hi");
            }

            log.info("Ccd Response received for case id: {} , {}", sscsCaseDataWrapper.getNewSscsCaseData().getCcdCaseId(), sscsCaseDataWrapper.getNotificationEventType());

            callback.getCaseDetails().getCreatedDate();
            authorisationService.authorise(serviceAuthHeader);
            notificationService.manageNotificationAndSubscription(new CcdNotificationWrapper(sscsCaseDataWrapper), false);
        } catch (Exception e) {
            log.info("Exception thrown", e);
            throw e;
        }
    }

    private SscsCaseDataWrapper buildSscsCaseDataWrapper(SscsCaseData caseData, SscsCaseData caseDataBefore, NotificationEventType event, State state) {
        return SscsCaseDataWrapper.builder()
                .newSscsCaseData(caseData)
                .oldSscsCaseData(caseDataBefore)
                .notificationEventType(event)
                .state(state).build();
    }

}
