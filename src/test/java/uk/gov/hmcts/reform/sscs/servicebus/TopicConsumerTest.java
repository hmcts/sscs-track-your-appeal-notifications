package uk.gov.hmcts.reform.sscs.servicebus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.callback.CallbackDispatcher;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.exception.ClientAuthorisationException;

public class TopicConsumerTest {

    private static final String MESSAGE = "message";
    private static final Exception EXCEPTION = new RuntimeException("blah");

    @Mock
    private CallbackDispatcher dispatcher;

    @Mock
    private SscsCaseCallbackDeserializer deserializer;

    private TopicConsumer topicConsumer;
    private Exception exception;

    @Before
    public void setup() {
        initMocks(this);
        topicConsumer = new TopicConsumer(dispatcher, deserializer);
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(
                123L,
                "jurisdiction",
                State.APPEAL_CREATED,
                SscsCaseData.builder().build(),
                LocalDateTime.now().minusMinutes(10)
        );
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED, true);
        when(deserializer.deserialize(any())).thenReturn(callback);
    }

    @Test
    public void anExceptionWillBeCaught() {
        exception = EXCEPTION;
        doThrow(exception).when(dispatcher).handle(any());
        topicConsumer.onMessage(MESSAGE, "1");
        verify(dispatcher, atLeastOnce()).handle(any());
    }


    @Test
    public void nullPointerExceptionWillBeCaught() {
        exception = new NullPointerException();
        doThrow(exception).when(dispatcher).handle(any());
        topicConsumer.onMessage(MESSAGE, "1");
        verify(dispatcher, atLeastOnce()).handle(any());
    }

    @Test
    public void clientAuthorisationExceptionWillBeCaught() {
        exception = new ClientAuthorisationException(EXCEPTION);
        doThrow(exception).when(dispatcher).handle(any());
        topicConsumer.onMessage(MESSAGE, "1");
        verify(dispatcher, atLeastOnce()).handle(any());
    }

    @Test
    public void handleValidRequest() {
        topicConsumer.onMessage(MESSAGE, "1");
        verify(dispatcher).handle(any());
    }

}
