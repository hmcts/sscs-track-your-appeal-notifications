package uk.gov.hmcts.sscs.scheduler.serialize;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import uk.gov.hmcts.sscs.scheduler.model.HttpAction;

public class ActionSerializerTest {

    @Test
    public void should_serialize_and_deserialize_job_back_to_original_object() {
        ActionSerializer s = new ActionSerializer();
        HttpAction inputAction = new HttpAction("url", HttpMethod.POST, emptyMap(), null);

        String jsonAction = s.serialize(inputAction);
        HttpAction outputAction = s.deserialize(jsonAction);

        assertThat(outputAction).isEqualToComparingFieldByField(inputAction);
    }

    @Test
    public void should_serialize_and_deserialize_null() {
        ActionSerializer s = new ActionSerializer();

        String jsonAction = s.serialize(null);
        HttpAction outputAction = s.deserialize(jsonAction);

        assertThat(outputAction).isNull();
    }
}
