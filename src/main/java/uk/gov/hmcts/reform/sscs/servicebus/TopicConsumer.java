package uk.gov.hmcts.reform.sscs.servicebus;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.getNotificationByCcdEvent;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.callback.CallbackDispatcher;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;

@Slf4j
@Component
@Lazy(false)
public class TopicConsumer {

    private final CallbackDispatcher dispatcher;
    private final SscsCaseCallbackDeserializer sscsDeserializer;

    public TopicConsumer(CallbackDispatcher dispatcher,
                         SscsCaseCallbackDeserializer sscsDeserializer) {
        this.dispatcher = dispatcher;
        this.sscsDeserializer = sscsDeserializer;
    }

    @JmsListener(
        destination = "${amqp.topic}",
        containerFactory = "topicJmsListenerContainerFactory",
        subscription = "${amqp.subscription}"
    )
    public void onMessage(String message) {
        processMessage(message);
    }


    private void processMessage(String message) {
        try {
            Callback<SscsCaseData> callback = sscsDeserializer.deserialize(message);
            requireNonNull(callback, "callback must not be null");
            CaseDetails<SscsCaseData> caseDetailsBefore = callback.getCaseDetailsBefore().orElse(null);

            SscsCaseDataWrapper sscsCaseDataWrapper = buildSscsCaseDataWrapper(
                    callback.getCaseDetails().getCaseData(),
                    caseDetailsBefore != null ? caseDetailsBefore.getCaseData() : null,
                    getNotificationByCcdEvent(callback.getEvent()),
                    callback.getCaseDetails().getCreatedDate(),
                    callback.getCaseDetails().getState());

            log.info("Ccd Response received for case id: {} , {}", sscsCaseDataWrapper.getNewSscsCaseData().getCcdCaseId(), sscsCaseDataWrapper.getNotificationEventType());
            dispatcher.handle(sscsCaseDataWrapper);
            log.info("Sscs Case CCD callback `{}` handled for Case ID `{}`", callback.getEvent(), callback.getCaseDetails().getId());
        } catch (Exception exception) {
            // unrecoverable. Catch to remove it from the queue.
            log.error(format("Caught unrecoverable error: %s", exception.getMessage()), exception);
        }
    }

    private SscsCaseDataWrapper buildSscsCaseDataWrapper(SscsCaseData caseData, SscsCaseData caseDataBefore, NotificationEventType event, LocalDateTime createdDate, State state) {
        return SscsCaseDataWrapper.builder()
                .newSscsCaseData(caseData)
                .oldSscsCaseData(caseDataBefore)
                .notificationEventType(event)
                .createdDate(createdDate)
                .state(state).build();
    }
}
