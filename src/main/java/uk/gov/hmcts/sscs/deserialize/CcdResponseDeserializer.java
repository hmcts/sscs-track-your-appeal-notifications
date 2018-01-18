package uk.gov.hmcts.sscs.deserialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.notify.NotificationType;

@Service
public class CcdResponseDeserializer extends StdDeserializer<CcdResponse> {

    public CcdResponseDeserializer() {
        this(null);
    }

    public CcdResponseDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public CcdResponse deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        @SuppressWarnings("unchecked")
        ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree(jp);

        return deserializeCcdJson(node);
    }

    public CcdResponse deserializeCcdJson(JsonNode node) {
        CcdResponse ccdResponse = new CcdResponse();

        ccdResponse.setNotificationType(NotificationType.getNotificationById(getField(node, "state")));

        JsonNode caseNode = getNode(node, "case_data");

        deserializeAppellantJson(caseNode, ccdResponse);

        deserializeCaseIdJson(caseNode, ccdResponse);

        return ccdResponse;
    }

    public CcdResponse deserializeAppellantJson(JsonNode node, CcdResponse ccdResponse) {
        JsonNode appellantNode = getNode(node, "appellant");
        if (appellantNode != null) {
            deserializeAppellantNameJson(appellantNode, ccdResponse);
            deserializeAppellantContactJson(appellantNode, ccdResponse);
        }
        return ccdResponse;
    }

    private CcdResponse deserializeAppellantNameJson(JsonNode node, CcdResponse ccdResponse) {
        JsonNode nameNode = getNode(node, "name");

        if (nameNode != null) {
            ccdResponse.setAppellantFirstName(getField(nameNode, "firstName"));
            ccdResponse.setAppellantSurname(getField(nameNode, "lastName"));
            ccdResponse.setAppellantTitle(getField(nameNode, "title"));
        }

        return ccdResponse;
    }

    private CcdResponse deserializeAppellantContactJson(JsonNode node, CcdResponse ccdResponse) {
        JsonNode contactNode = getNode(node, "contact");

        if (contactNode != null) {
            ccdResponse.setEmail(getField(contactNode, "email"));
            ccdResponse.setMobileNumber(getField(contactNode, "mobile"));
        }

        return ccdResponse;
    }

    public CcdResponse deserializeCaseIdJson(JsonNode node, CcdResponse ccdResponse) {
        JsonNode idNode = getNode(node, "id");

        if (idNode != null) {
            ccdResponse.setAppealNumber(getField(idNode, "tya"));
            ccdResponse.setCaseReference(getField(idNode, "gaps2"));
        }

        return ccdResponse;
    }

    private JsonNode getNode(JsonNode node, String field) {
        return node.has(field) ? node.get(field) : null;
    }

    private String getField(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText() : null;
    }

}
