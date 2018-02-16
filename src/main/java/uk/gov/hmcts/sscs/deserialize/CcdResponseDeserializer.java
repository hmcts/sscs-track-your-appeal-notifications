package uk.gov.hmcts.sscs.deserialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.Subscription;
import uk.gov.hmcts.sscs.domain.notify.NotificationType;

@Service
public class CcdResponseDeserializer extends StdDeserializer<CcdResponseWrapper> {

    public CcdResponseDeserializer() {
        this(null);
    }

    public CcdResponseDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public CcdResponseWrapper deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        @SuppressWarnings("unchecked")
        ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree(jp);

        return buildCcdResponseWrapper(node);
    }

    public CcdResponseWrapper buildCcdResponseWrapper(JsonNode node) {
        CcdResponse newCcdResponse = null;
        CcdResponse oldCcdResponse = null;

        JsonNode caseDetailsNode = getNode(node, "case_details");
        JsonNode caseNode = getNode(caseDetailsNode, "case_data");

        if (caseNode != null) {
            newCcdResponse = deserializeCaseNode(caseNode);
            newCcdResponse.setNotificationType(NotificationType.getNotificationById(getField(node, "event_id")));
        }

        JsonNode oldCaseDetailsNode = getNode(node, "case_details_before");
        JsonNode oldCaseNode = getNode(oldCaseDetailsNode, "case_data");

        if (oldCaseNode != null) {
            oldCcdResponse = deserializeCaseNode(oldCaseNode);
            oldCcdResponse.setNotificationType(NotificationType.getNotificationById(getField(node, "event_id")));
        }

        return new CcdResponseWrapper(newCcdResponse, oldCcdResponse);
    }

    public CcdResponse deserializeCaseNode(JsonNode caseNode) {
        CcdResponse ccdResponse = new CcdResponse();

        JsonNode appealNode = getNode(caseNode, "appeal");
        JsonNode subscriptionsNode = getNode(caseNode, "subscriptions");

        ccdResponse.setCaseReference(getField(caseNode, "caseReference"));

        deserializeAppellantDetailsJson(appealNode, subscriptionsNode, ccdResponse);
        deserializeSupporterDetailsJson(appealNode, subscriptionsNode, ccdResponse);

        return ccdResponse;
    }

    public CcdResponse deserializeAppellantDetailsJson(JsonNode appealNode, JsonNode subscriptionsNode, CcdResponse ccdResponse) {
        JsonNode appellantNode = getNode(appealNode, "appellant");
        JsonNode appellantSubscriptionNode = getNode(subscriptionsNode, "appellantSubscription");

        Subscription appellantSubscription = new Subscription();

        if (appellantNode != null) {
            deserializeNameJson(appellantNode, appellantSubscription);
        }

        if (appellantSubscriptionNode != null) {
            deserializeSubscriberJson(appellantSubscriptionNode, appellantSubscription);
        }

        ccdResponse.setAppellantSubscription(appellantSubscription);

        return ccdResponse;
    }

    public CcdResponse deserializeSupporterDetailsJson(JsonNode appealNode, JsonNode subscriptionsNode, CcdResponse ccdResponse) {
        JsonNode supporterNode = getNode(appealNode, "supporter");
        JsonNode supporterSubscriptionNode = getNode(subscriptionsNode, "supporterSubscription");

        Subscription supporterSubscription = new Subscription();

        if (supporterNode != null) {
            deserializeNameJson(supporterNode, supporterSubscription);
        }

        if (supporterSubscriptionNode != null) {
            deserializeSubscriberJson(supporterSubscriptionNode, supporterSubscription);
        }

        ccdResponse.setSupporterSubscription(supporterSubscription);

        return ccdResponse;
    }


    private Subscription deserializeNameJson(JsonNode node, Subscription subscription) {
        JsonNode nameNode = getNode(node, "name");

        if (nameNode != null) {
            subscription.setFirstName(getField(nameNode, "firstName"));
            subscription.setSurname(getField(nameNode, "lastName"));
            subscription.setTitle(getField(nameNode, "title"));
        }

        return subscription;
    }

    private Subscription deserializeSubscriberJson(JsonNode node, Subscription subscription) {
        if (node != null) {
            subscription.setAppealNumber(getField(node, "tya"));
            subscription.setEmail(getField(node, "email"));
            subscription.setMobileNumber(getField(node, "mobile"));
            subscription.setSubscribeSms(convertYesNoToBoolean(getField(node, "subscribeSms")));
            subscription.setSubscribeEmail(convertYesNoToBoolean(getField(node, "subscribeEmail")));
        }

        return subscription;
    }

    public JsonNode getNode(JsonNode node, String field) {
        return node != null && node.has(field) ? node.get(field) : null;
    }

    public String getField(JsonNode node, String field) {
        return node != null && node.has(field) ? node.get(field).asText() : null;
    }

    public Boolean convertYesNoToBoolean(String text) {
        return text != null && text.equals("Yes") ? true : false;
    }

}
