package uk.gov.hmcts.reform.sscs.service.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobPayloadSerializer;

@Component
public class TcaActionSerializer implements JobPayloadSerializer<TcaJobPayload> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String serialize(TcaJobPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize TcaJobPayload " + payload);
        }
    }

    public static void main(String[] args) {
        System.out.println(new TcaActionSerializer().serialize(new TcaJobPayload(100)));
    }
}
