package uk.gov.hmcts.sscs.deserialize;

import static uk.gov.hmcts.sscs.config.AppConstants.ZONE_ID;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.domain.*;
import uk.gov.hmcts.sscs.domain.notify.Event;
import uk.gov.hmcts.sscs.domain.notify.EventType;

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

    public CcdResponseWrapper buildCcdResponseWrapper(JsonNode node) throws IOException {
        CcdResponse newCcdResponse = null;
        CcdResponse oldCcdResponse = null;

        JsonNode caseDetailsNode = getNode(node, "case_details");
        JsonNode caseNode = getNode(caseDetailsNode, "case_data");

        if (caseNode != null) {
            newCcdResponse = deserializeCaseNode(caseNode);
            newCcdResponse.setNotificationType(EventType.getNotificationById(getField(node, "event_id")));
        }

        JsonNode oldCaseDetailsNode = getNode(node, "case_details_before");
        JsonNode oldCaseNode = getNode(oldCaseDetailsNode, "case_data");

        if (oldCaseNode != null) {
            oldCcdResponse = deserializeCaseNode(oldCaseNode);
            oldCcdResponse.setNotificationType(EventType.getNotificationById(getField(node, "event_id")));
        }

        return new CcdResponseWrapper(newCcdResponse, oldCcdResponse);
    }

    public CcdResponse deserializeCaseNode(JsonNode caseNode) throws IOException {
        CcdResponse ccdResponse = new CcdResponse();

        JsonNode appealNode = getNode(caseNode, "appeal");
        JsonNode subscriptionsNode = getNode(caseNode, "subscriptions");

        ccdResponse.setCaseReference(getField(caseNode, "caseReference"));

        deserializeBenefitDetailsJson(appealNode, ccdResponse);
        deserializeAppellantDetailsJson(appealNode, subscriptionsNode, ccdResponse);
        deserializeSupporterDetailsJson(appealNode, subscriptionsNode, ccdResponse);
        deserializeEventDetailsJson(caseNode, ccdResponse);
        deserializeHearingDetailsJson(caseNode, ccdResponse);

        return ccdResponse;
    }

    public CcdResponse deserializeBenefitDetailsJson(JsonNode appealNode, CcdResponse ccdResponse) {
        JsonNode benefitTypeNode = getNode(appealNode, "benefitType");

        if (benefitTypeNode != null) {
            String benefitCode = getField(benefitTypeNode, "code");
            ccdResponse.setBenefitType(Benefit.getBenefitByCode(benefitCode));
        }

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

    public CcdResponse deserializeEventDetailsJson(JsonNode caseNode, CcdResponse ccdResponse) {
        final JsonNode eventNode =  caseNode.get("events");

        if (eventNode != null && eventNode.isArray()) {
            List<Event> events = new ArrayList<>();

            for (final JsonNode objNode : eventNode) {
                JsonNode valueNode = getNode(objNode, "value");

                ZonedDateTime date = convertToUkLocalDateTime(getField(valueNode, "date"));

                EventType eventType = EventType.getNotificationById(getField(valueNode, "type"));
                events.add(new Event(date, eventType));

            }
            Collections.sort(events, Collections.reverseOrder());
            ccdResponse.setEvents(events);
        }

        return ccdResponse;
    }

    public CcdResponse deserializeHearingDetailsJson(JsonNode caseNode, CcdResponse ccdResponse) {
        final JsonNode hearingNode = caseNode.get("hearings");

        if (hearingNode != null && hearingNode.isArray()) {
            List<Hearing> hearings = new ArrayList<>();

            for (final JsonNode objNode : hearingNode) {
                Hearing hearing = new Hearing();
                JsonNode valueNode = getNode(objNode, "value");
                JsonNode venueNode = getNode(valueNode, "venue");
                JsonNode addressNode = getNode(venueNode, "address");

                hearing.setHearingDateTime(buildHearingDateTime(getField(valueNode, "hearingDate"), getField(valueNode, "time")));
                hearing.setVenueName(getField(venueNode, "name"));
                hearing.setVenueAddressLine1(getField(addressNode, "line1"));
                hearing.setVenueAddressLine2(getField(addressNode, "line2"));
                hearing.setVenueTown(getField(addressNode, "town"));
                hearing.setVenueCounty(getField(addressNode, "county"));
                hearing.setVenuePostcode(getField(addressNode, "postcode"));
                hearing.setVenueGoogleMapUrl(getField(venueNode, "googleMapLink"));

                hearings.add(hearing);
            }
            Collections.sort(hearings, Collections.reverseOrder());
            ccdResponse.setHearings(hearings);
        }

        return ccdResponse;
    }

    private ZonedDateTime buildHearingDateTime(String hearingDate, String hearingTime) {
        return convertToUkLocalDateTime(hearingDate + "T" + hearingTime);
    }

    private static ZonedDateTime convertToUkLocalDateTime(String bstDateTimeinUtc) {
        return ZonedDateTime.parse(bstDateTimeinUtc + "Z").toInstant().atZone(ZoneId.of(ZONE_ID));
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
