package helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.sscs.service.AuthorisationService;

public class IntegrationTestHelper {

    private IntegrationTestHelper() {

    }

    public static MockHttpServletRequestBuilder getRequestWithAuthHeader(String json) {

        return getRequestWithoutAuthHeader(json)
                .header(AuthorisationService.SERVICE_AUTHORISATION_HEADER, "some-auth-header");
    }

    public static MockHttpServletRequestBuilder getRequestWithoutAuthHeader(String json) {

        return post("/send")
                .contentType(APPLICATION_JSON)
                .content(json);
    }

    public static void assertHttpStatus(HttpServletResponse response, HttpStatus status) {
        assertThat(response.getStatus()).isEqualTo(status.value());
    }

    public static String updateEmbeddedJson(String json, String value, String... keys) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        Map map = objectMapper.readValue(json, Map.class);
        Map t = map;
        for (int i = 0; i < keys.length - 1; i++) {
            t = (Map) t.get(keys[i]);
        }

        t.put(keys[keys.length - 1], value);

        return objectMapper.writeValueAsString(map);
    }
}
