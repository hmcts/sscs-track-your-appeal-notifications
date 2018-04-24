package uk.gov.hmcts.sscs.deserialize;

import static uk.gov.hmcts.sscs.config.AppConstants.ZONE_ID;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.time.*;
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

    public CcdResponseWrapper buildCcdResponseWrapper(JsonNode node) {
        CcdResponse newCcdResponse = null;
        CcdResponse oldCcdResponse = null;

        JsonNode caseDetailsNode = getNode(node, "case_details");
        JsonNode caseNode = getNode(caseDetailsNode, "case_data");

        if (caseNode != null) {
            newCcdResponse = createCcdResponseFromNode(caseNode, node, caseDetailsNode);
        }

        JsonNode oldCaseDetailsNode = getNode(node, "case_details_before");
        JsonNode oldCaseNode = getNode(oldCaseDetailsNode, "case_data");

        if (oldCaseNode != null) {
            oldCcdResponse = createCcdResponseFromNode(oldCaseNode, node, oldCaseDetailsNode);
        }

        return CcdResponseWrapper.builder().newCcdResponse(newCcdResponse).oldCcdResponse(oldCcdResponse).build();
    }

    private CcdResponse createCcdResponseFromNode(JsonNode caseNode, JsonNode node, JsonNode caseDetailsNode) {
        CcdResponse ccdResponse = deserializeCaseNode(caseNode);
        ccdResponse.setNotificationType(EventType.getNotificationById(getField(node, "event_id")));
        ccdResponse.setCaseId(getField(caseDetailsNode, "id"));
        return ccdResponse;
    }

    public CcdResponse deserializeCaseNode(JsonNode caseNode) {
        JsonNode appealNode = getNode(caseNode, "appeal");
        JsonNode subscriptionsNode = getNode(caseNode, "subscriptions");

        CcdResponse ccdResponse = CcdResponse.builder().caseReference(getField(caseNode, "caseReference")).build();

        deserializeBenefitDetailsJson(appealNode, ccdResponse);
        deserializeSubscriptionJson(appealNode, subscriptionsNode, ccdResponse);
        deserializeAppellantDetailsJson(appealNode, subscriptionsNode, ccdResponse);
        deserializeSupporterDetailsJson(appealNode, subscriptionsNode, ccdResponse);
        deserializeEventDetailsJson(caseNode, ccdResponse);
        deserializeHearingDetailsJson(caseNode, ccdResponse);
        deserializeEvidenceDetailsJson(caseNode, ccdResponse);

        return ccdResponse;
    }

    public void deserializeBenefitDetailsJson(JsonNode appealNode, CcdResponse ccdResponse) {
        JsonNode benefitTypeNode = getNode(appealNode, "benefitType");

        if (benefitTypeNode != null) {
            String benefitCode = getField(benefitTypeNode, "code");
            ccdResponse.setBenefitType(Benefit.getBenefitByCode(benefitCode));
        }
    }

    public void deserializeSubscriptionJson(JsonNode appealNode, JsonNode subscriptionsNode, CcdResponse ccdResponse) {
        Subscription appellantSubscription = deserializeAppellantDetailsJson(appealNode, subscriptionsNode, ccdResponse);
        Subscription supporterSubscription = deserializeSupporterDetailsJson(appealNode, subscriptionsNode, ccdResponse);

        ccdResponse.setSubscriptions(Subscriptions.builder()
                .appellantSubscription(appellantSubscription)
                .supporterSubscription(supporterSubscription).build());
    }

    private Subscription deserializeAppellantDetailsJson(JsonNode appealNode, JsonNode subscriptionsNode, CcdResponse ccdResponse) {
        JsonNode appellantNode = getNode(appealNode, "appellant");
        JsonNode appellantSubscriptionNode = getNode(subscriptionsNode, "appellantSubscription");

        Subscription appellantSubscription = Subscription.builder().build();

        if (appellantNode != null) {
            appellantSubscription = deserializeNameJson(appellantNode, appellantSubscription);
        }

        if (appellantSubscriptionNode != null) {
            appellantSubscription = deserializeSubscriberJson(appellantSubscriptionNode, appellantSubscription);
        }

        return appellantSubscription;
    }

    private Subscription deserializeSupporterDetailsJson(JsonNode appealNode, JsonNode subscriptionsNode, CcdResponse ccdResponse) {
        JsonNode supporterNode = getNode(appealNode, "supporter");
        JsonNode supporterSubscriptionNode = getNode(subscriptionsNode, "supporterSubscription");

        Subscription supporterSubscription = Subscription.builder().build();

        if (supporterNode != null) {
            supporterSubscription = deserializeNameJson(supporterNode, supporterSubscription);
        }

        if (supporterSubscriptionNode != null) {
            supporterSubscription = deserializeSubscriberJson(supporterSubscriptionNode, supporterSubscription);
        }

        return supporterSubscription;
    }

    public void deserializeEventDetailsJson(JsonNode caseNode, CcdResponse ccdResponse) {
        final JsonNode eventNode =  caseNode.get("events");

        if (eventNode != null && eventNode.isArray()) {
            List<Events> events = new ArrayList<>();

            for (final JsonNode objNode : eventNode) {
                JsonNode valueNode = getNode(objNode, "value");

                String date = getField(valueNode, "date");
                String eventType = getField(valueNode, "type");

                events.add(Events.builder().value(Event.builder().date(date).type(eventType).build()).build());

            }
            Collections.sort(events, Collections.reverseOrder());
            ccdResponse.setEvents(events);
        }
    }

    public void deserializeHearingDetailsJson(JsonNode caseNode, CcdResponse ccdResponse) {
        final JsonNode hearingNode = caseNode.get("hearings");

        if (hearingNode != null && hearingNode.isArray()) {
            List<Hearing> hearings = new ArrayList<>();

            for (final JsonNode objNode : hearingNode) {
                JsonNode valueNode = getNode(objNode, "value");
                JsonNode venueNode = getNode(valueNode, "venue");
                JsonNode addressNode = getNode(venueNode, "address");

                Hearing hearing = Hearing.builder().value(HearingDetails.builder()
                    .hearingDate(getField(valueNode, "hearingDate"))
                    .time(getField(valueNode, "time"))
                    .venue(Venue.builder()
                    .name(getField(venueNode, "name"))
                    .address(Address.builder()
                    .line1(getField(addressNode, "line1"))
                    .line2(getField(addressNode, "line2"))
                    .town(getField(addressNode, "town"))
                    .county(getField(addressNode, "county"))
                    .postcode(getField(addressNode, "postcode")).build())
                    .googleMapLink(getField(venueNode, "googleMapLink"))
                    .build()).build()).build();

                hearings.add(hearing);
            }
            Collections.sort(hearings, Collections.reverseOrder());
            ccdResponse.setHearings(hearings);
        }
    }

    public void deserializeEvidenceDetailsJson(JsonNode caseNode, CcdResponse ccdResponse) {
        JsonNode evidenceNode = getNode(caseNode, "evidence");

        if (evidenceNode != null) {
            final JsonNode documentsNode = evidenceNode.get("documents");

            if (documentsNode != null && documentsNode.isArray()) {
                List<Evidence> evidences = new ArrayList<>();
                for (final JsonNode objNode : documentsNode) {

                    JsonNode valueNode = getNode(objNode, "value");

                    Evidence evidence = Evidence.builder()
                            .dateReceived(LocalDate.parse(getField(valueNode, "dateReceived")))
                            .evidenceType(getField(valueNode, "evidenceType"))
                            .evidenceProvidedBy(getField(valueNode, "evidenceProvidedBy")).build();

                    evidences.add(evidence);
                }
                Collections.sort(evidences, Collections.reverseOrder());
                ccdResponse.setEvidences(evidences);
            }
        }
    }

    private static ZonedDateTime convertToUkLocalDateTime(String bstDateTimeinUtc) {
        return ZonedDateTime.parse(bstDateTimeinUtc + "Z").toInstant().atZone(ZoneId.of(ZONE_ID));
    }

    private Subscription deserializeNameJson(JsonNode node, Subscription subscription) {
        JsonNode nameNode = getNode(node, "name");

        if (nameNode != null) {
            subscription = subscription.toBuilder()
                .firstName(getField(nameNode, "firstName"))
                .surname(getField(nameNode, "lastName"))
                .title(getField(nameNode, "title")).build();
        }

        return subscription;
    }

    private Subscription deserializeSubscriberJson(JsonNode node, Subscription subscription) {
        if (node != null) {
            subscription = subscription.toBuilder()
                .tya(getField(node, "tya"))
                .email(getField(node, "email"))
                .mobile(getField(node, "mobile"))
                .subscribeSms(getField(node, "subscribeSms"))
                .subscribeEmail(getField(node, "subscribeEmail")).build();
        }

        return subscription;
    }

    public JsonNode getNode(JsonNode node, String field) {
        return node != null && node.has(field) ? node.get(field) : null;
    }

    public String getField(JsonNode node, String field) {
        return node != null && node.has(field) ? node.get(field).asText() : null;
    }

}
