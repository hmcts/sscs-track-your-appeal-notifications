package uk.gov.hmcts.sscs.domain.reminder;

import lombok.Value;
import org.json.JSONObject;

@Value
public class Action {

    private String caseId;
    private String reminderType;
}
