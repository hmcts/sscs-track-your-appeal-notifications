package uk.gov.hmcts.sscs.domain.reminder;

import java.util.Objects;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;

public class Action {

    private String url;
    private String body;
    private String method;

    public Action(String appealNumber, String reminderType, String callbackUrl) {
        this.url = callbackUrl;
        this.body = createJsonBody(appealNumber, reminderType);
        this.method = "POST";
    }

    private String createJsonBody(String appealNumber, String reminderType) {
        JSONObject json = new JSONObject();
        json.put("appealNumber", appealNumber);
        json.put("reminderType", reminderType);

        return json.toString();
    }

    public String getUrl() {
        return url;
    }

    public String getBody() {
        return body;
    }

    public String getMethod() {
        return method;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Action action = (Action) o;
        return Objects.equals(url, action.url)
                && Objects.equals(body, action.body)
                && Objects.equals(method, action.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, body, method);
    }
}
