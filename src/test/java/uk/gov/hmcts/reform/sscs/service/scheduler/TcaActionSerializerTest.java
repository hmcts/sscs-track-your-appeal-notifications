package uk.gov.hmcts.reform.sscs.service.scheduler;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class TcaActionSerializerTest {

    @Test
    public void canSerialize() {
        long caseId = 123L;
        TcaJobPayload payload = new TcaJobPayload(caseId);

        String payloadString = new TcaActionSerializer().serialize(payload);
        long payloadCaseId = new TcaActionDeserializer().deserialize(payloadString).getCaseId();

        assertThat(payloadString, containsString("tcaEvent"));
        assertThat(payloadCaseId, equalTo(caseId));
    }
}