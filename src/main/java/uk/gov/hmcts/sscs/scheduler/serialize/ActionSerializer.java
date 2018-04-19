package uk.gov.hmcts.sscs.scheduler.serialize;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.scheduler.exceptions.JobActionSerializationException;
import uk.gov.hmcts.sscs.scheduler.model.HttpAction;

// Quartz can store only basic types as job data, we serialize it to json string.
@Component
public class ActionSerializer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String serialize(HttpAction action) {
        try {
            return objectMapper.writeValueAsString(action);
        } catch (JsonProcessingException exc) {
            throw new JobActionSerializationException("Unable to serialize action", exc);
        }
    }

    public HttpAction deserialize(String jsonString) {
        try {
            return objectMapper.readValue(jsonString, HttpAction.class);
        } catch (IOException exc) {
            throw new JobActionSerializationException("Unable to deserialize string to action", exc);
        }
    }
}
