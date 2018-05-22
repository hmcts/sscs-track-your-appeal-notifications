package uk.gov.hmcts.sscs.scheduler.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import org.springframework.http.HttpMethod;

public class HttpAction {

    @NotBlank
    @URL
    public final String url;

    @NotNull
    public final HttpMethod method;

    public final Map<String, String> headers;

    public final String body;

    public HttpAction(
        @JsonProperty("url") String url,
        @JsonProperty("method") HttpMethod method,
        @JsonProperty("headers") Map<String, String> headers,
        @JsonProperty("body") String body
    ) {
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.body = body;
    }

    public HttpAction withHeader(String name, String value) {
        Map<String, String> newHeaders = new HashMap<>(headers);
        newHeaders.put(name, value);
        return new HttpAction(this.url, this.method, newHeaders, this.body);
    }
}
