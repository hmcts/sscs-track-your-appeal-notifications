package uk.gov.hmcts.reform.sscs.service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobPayloadDeserializer;

@Component
public class TcaActionDeserializer implements JobPayloadDeserializer<TcaJobPayload> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public TcaJobPayload deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, TcaJobPayload.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot deserialize payload as TcaJobPayload [" + payload + "]", e);
        }
    }
}
