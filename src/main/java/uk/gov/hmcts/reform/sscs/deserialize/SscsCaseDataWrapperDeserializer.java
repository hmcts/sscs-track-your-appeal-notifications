package uk.gov.hmcts.reform.sscs.deserialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;

@Service
public class SscsCaseDataWrapperDeserializer extends StdDeserializer<SscsCaseDataWrapper> {

    public SscsCaseDataWrapperDeserializer() {
        this(null);
    }

    public SscsCaseDataWrapperDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public SscsCaseDataWrapper deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        @SuppressWarnings("unchecked")
        ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree(jp);

        return buildSscsCaseDataWrapper(node);
    }

    public SscsCaseDataWrapper buildSscsCaseDataWrapper(JsonNode node) {
        SscsCaseData newSscsCaseData = null;
        SscsCaseData oldSscsCaseData = null;

        JsonNode caseDetailsNode = getNode(node, "case_details");
        JsonNode caseNode = getNode(caseDetailsNode, "case_data");

        if (caseNode != null) {
            newSscsCaseData = createSscsCaseDataFromNode(caseNode, node, caseDetailsNode);
        }

        if (getNode(node, "case_details_before") != null) {
            JsonNode oldCaseDetailsNode = getNode(node, "case_details_before");
            JsonNode oldCaseNode = getNode(oldCaseDetailsNode, "case_data");

            if (oldCaseNode != null) {
                oldSscsCaseData = createSscsCaseDataFromNode(oldCaseNode, node, oldCaseDetailsNode);
            }
        }

        return SscsCaseDataWrapper.builder().newSscsCaseData(newSscsCaseData).oldSscsCaseData(oldSscsCaseData).build();
    }

    private SscsCaseData createSscsCaseDataFromNode(JsonNode caseNode, JsonNode node, JsonNode caseDetailsNode) {
        SscsCaseData ccdResponse = deserializeCaseNode(caseNode);
        ccdResponse.setNotificationType(EventType.getEventTypeByCcdType(getField(node, "event_id")));
        ccdResponse.setCaseId(getField(caseDetailsNode, "id"));
        return ccdResponse;
    }

    public SscsCaseData deserializeCaseNode(JsonNode caseNode) {
        JsonNode appealNode = getNode(caseNode, "appeal");
        JsonNode subscriptionsNode = getNode(caseNode, "subscriptions");

        SscsCaseData ccdResponse = SscsCaseData.builder().caseReference(getField(caseNode, "caseReference")).build();

        deserializeAppealDetailsJson(appealNode, ccdResponse);
        deserializeSubscriptionJson(subscriptionsNode, ccdResponse);
        deserializeEventDetailsJson(caseNode, ccdResponse);
        deserializeHearingDetailsJson(caseNode, ccdResponse);
        deserializeEvidenceDetailsJson(caseNode, ccdResponse);
        deserializeRegionalProcessingCenterJson(caseNode, ccdResponse);

        return ccdResponse;
    }

    public void deserializeAppealDetailsJson(JsonNode appealNode, SscsCaseData ccdResponse) {

        Appeal appeal = Appeal.builder().build();
        deserializeMrnDetailsJson(appealNode, appeal);
        deserializeAppellantDetailsJson(appealNode, appeal);
        deserializeBenefitDetailsJson(appealNode, appeal);
        deserializeHearingOptionsJson(appealNode, appeal);
        deserializeAppealReasonsJson(appealNode, appeal);
        deserializeRepresentativeReasons(appealNode, appeal);

        appeal.setSigner(getField(appealNode, "signer"));

        ccdResponse.setAppeal(appeal);
    }

    public void deserializeMrnDetailsJson(JsonNode appealNode, Appeal appeal) {
        JsonNode mrnNode = getNode(appealNode, "mrnDetails");

        String dwpIssuingOffice = getField(mrnNode, "dwpIssuingOffice");
        String mrnDate = getField(mrnNode, "mrnDate");
        String mrnLateReason = getField(mrnNode, "mrnLateReason");
        String mrnMissingReason = getField(mrnNode, "mrnMissingReason");

        MrnDetails mrnDetails = MrnDetails.builder()
                .dwpIssuingOffice(dwpIssuingOffice)
                .mrnDate(mrnDate)
                .mrnLateReason(mrnLateReason)
                .mrnMissingReason(mrnMissingReason).build();

        appeal.setMrnDetails(mrnDetails);
    }

    public void deserializeAppellantDetailsJson(JsonNode appealNode, Appeal appeal) {
        JsonNode appellantNode = getNode(appealNode, "appellant");

        Name name = deserializeNameJson(appellantNode);
        Address address = deserializeAddressJson(appellantNode);
        Contact contact = deserializeContactJson(appellantNode);
        Identity identity = deserializeIdentityJson(appellantNode);
        String isAppointee = getField(appellantNode, "isAppointee");

        appeal.setAppellant(Appellant.builder()
                .name(name).address(address).contact(contact).identity(identity).isAppointee(isAppointee).build());
    }

    private Contact deserializeContactJson(JsonNode node) {
        JsonNode contactNode = getNode(node, "contact");

        String phone = getField(contactNode, "phone") != null ? getField(contactNode, "phone") : getField(contactNode, "mobile");

        return Contact.builder()
            .email(getField(contactNode, "email"))
            .phone(phone).build();
    }

    private Identity deserializeIdentityJson(JsonNode node) {
        JsonNode nameNode = getNode(node, "identity");

        return Identity.builder()
                .dob(getField(nameNode, "dob"))
                .nino(getField(nameNode, "nino")).build();
    }

    public void deserializeBenefitDetailsJson(JsonNode appealNode, Appeal appeal) {
        JsonNode benefitTypeNode = getNode(appealNode, "benefitType");

        if (benefitTypeNode != null) {
            String benefitCode = getField(benefitTypeNode, "code");
            appeal.setBenefitType(BenefitType.builder().code(benefitCode).build());
        }
    }

    public void deserializeHearingOptionsJson(JsonNode appealNode, Appeal appeal) {
        JsonNode hearingOptionsNode = getNode(appealNode, "hearingOptions");

        String wantsToAttend = getField(hearingOptionsNode, "wantsToAttend");
        String wantsSupport = getField(hearingOptionsNode, "wantsSupport");
        String languageInterpreter = getField(hearingOptionsNode, "languageInterpreter");
        String languages = getField(hearingOptionsNode, "languages");

        List<String> arrangements = buildArrangements(hearingOptionsNode);
        String scheduleHearing = getField(hearingOptionsNode, "scheduleHearing");

        List<ExcludeDate> excludeDates = buildExcludeDates(hearingOptionsNode);

        String other = getField(hearingOptionsNode, "other");

        appeal.setHearingOptions(HearingOptions.builder()
            .wantsToAttend(wantsToAttend)
            .wantsSupport(wantsSupport)
            .languageInterpreter(languageInterpreter)
            .languages(languages)
            .arrangements(arrangements)
            .scheduleHearing(scheduleHearing)
            .excludeDates(excludeDates)
            .other(other).build());
    }

    private List<String> buildArrangements(JsonNode hearingOptionsNode) {
        List<String> arrangements = new ArrayList<>();

        if (hearingOptionsNode != null) {
            final JsonNode arrangementsNode = hearingOptionsNode.get("arrangements");

            if (arrangementsNode != null && arrangementsNode.isArray()) {
                for (final JsonNode objNode : arrangementsNode) {
                    arrangements.add(objNode.asText());
                }
            }
        }
        return arrangements;
    }

    private List<ExcludeDate> buildExcludeDates(JsonNode hearingOptionsNode) {
        List<ExcludeDate> excludeDates = new ArrayList<>();

        if (hearingOptionsNode != null) {
            final JsonNode excludeDatesNode = hearingOptionsNode.get("excludeDates");

            if (excludeDatesNode != null && excludeDatesNode.isArray()) {
                for (final JsonNode objNode : excludeDatesNode) {
                    final JsonNode valueNode = getNode(objNode, "value");

                    excludeDates.add(ExcludeDate.builder().value(DateRange.builder()
                            .start(getField(valueNode, "start"))
                            .end(getField(valueNode, "end")).build()).build());
                }
            }
        }
        return excludeDates;
    }

    public void deserializeAppealReasonsJson(JsonNode appealNode, Appeal appeal) {
        JsonNode appealReasonsNode = getNode(appealNode, "appealReasons");

        List<AppealReason> reasons = buildAppealReasons(appealReasonsNode);
        String otherReasons = getField(appealReasonsNode, "otherReasons");

        appeal.setAppealReasons(AppealReasons.builder()
                .reasons(reasons)
                .otherReasons(otherReasons).build());
    }

    private List<AppealReason> buildAppealReasons(JsonNode appealReasonsNode) {
        List<AppealReason> appealReasons = new ArrayList<>();

        if (appealReasonsNode != null) {
            final JsonNode reasonsNode = appealReasonsNode.get("reasons");

            if (reasonsNode != null && reasonsNode.isArray()) {
                for (final JsonNode objNode : reasonsNode) {
                    final JsonNode valueNode = getNode(objNode, "value");

                    appealReasons.add(AppealReason.builder().value(AppealReasonDetails.builder()
                            .description(getField(valueNode, "description"))
                            .reason(getField(valueNode, "reason")).build()).build());
                }
            }
        }
        return appealReasons;
    }

    public void deserializeRepresentativeReasons(JsonNode appealReasonsNode, Appeal appeal) {
        final JsonNode repNode = appealReasonsNode.get("rep");

        String hasRepresentative = getField(repNode, "hasRepresentative");
        if (hasRepresentative != null && hasRepresentative.toLowerCase().equals("yes")) {
            Name name = deserializeNameJson(repNode);
            Address address = deserializeAddressJson(repNode);
            Contact contact = deserializeContactJson(repNode);
            String organisation = getField(repNode, "organisation");

            appeal.setRep(Representative.builder()
                    .name(name).address(address).contact(contact).organisation(organisation).build());
        }
    }

    public void deserializeSubscriptionJson(JsonNode subscriptionsNode, SscsCaseData ccdResponse) {
        Subscription appellantSubscription = deserializeAppellantSubscriptionJson(subscriptionsNode);
        Subscription supporterSubscription = deserializeSupporterSubscriptionJson(subscriptionsNode);

        ccdResponse.setSubscriptions(Subscriptions.builder()
                .appellantSubscription(appellantSubscription)
                .supporterSubscription(supporterSubscription).build());
    }

    private Subscription deserializeAppellantSubscriptionJson(JsonNode subscriptionsNode) {
        JsonNode appellantSubscriptionNode = getNode(subscriptionsNode, "appellantSubscription");

        Subscription appellantSubscription = Subscription.builder().build();

        if (appellantSubscriptionNode != null) {
            appellantSubscription = deserializeSubscriberJson(appellantSubscriptionNode, appellantSubscription);
        }

        return appellantSubscription;
    }

    private Subscription deserializeSupporterSubscriptionJson(JsonNode subscriptionsNode) {
        JsonNode supporterSubscriptionNode = getNode(subscriptionsNode, "supporterSubscription");

        Subscription supporterSubscription = Subscription.builder().build();

        if (supporterSubscriptionNode != null) {
            supporterSubscription = deserializeSubscriberJson(supporterSubscriptionNode, supporterSubscription);
        }

        return supporterSubscription;
    }

    public void deserializeEventDetailsJson(JsonNode caseNode, SscsCaseData ccdResponse) {
        final JsonNode eventNode =  caseNode.get("events");

        if (eventNode != null && eventNode.isArray()) {
            List<Event> events = new ArrayList<>();

            for (final JsonNode objNode : eventNode) {
                JsonNode valueNode = getNode(objNode, "value");

                String date = getField(valueNode, "date");
                String eventType = getField(valueNode, "type");

                events.add(Event.builder().value(EventDetails.builder().date(date).type(eventType).build()).build());

            }
            Collections.sort(events, Collections.reverseOrder());
            ccdResponse.setEvents(events);
        }
    }

    public void deserializeHearingDetailsJson(JsonNode caseNode, SscsCaseData ccdResponse) {
        final JsonNode hearingNode = caseNode.get("hearings");

        if (hearingNode != null && hearingNode.isArray()) {
            List<Hearing> hearings = new ArrayList<>();

            for (final JsonNode objNode : hearingNode) {
                JsonNode valueNode = getNode(objNode, "value");
                JsonNode venueNode = getNode(valueNode, "venue");

                Hearing hearing = Hearing.builder().value(HearingDetails.builder()
                    .hearingDate(getField(valueNode, "hearingDate"))
                    .time(getField(valueNode, "time"))
                    .venue(Venue.builder()
                    .name(getField(venueNode, "name"))
                    .address(deserializeAddressJson(venueNode))
                    .googleMapLink(getField(venueNode, "googleMapLink"))
                    .build()).build()).build();

                hearings.add(hearing);
            }
            Collections.sort(hearings, Collections.reverseOrder());
            ccdResponse.setHearings(hearings);
        }
    }

    public void deserializeEvidenceDetailsJson(JsonNode caseNode, SscsCaseData ccdResponse) {
        JsonNode evidenceNode = getNode(caseNode, "evidence");

        if (evidenceNode != null) {
            final JsonNode documentsNode = evidenceNode.get("documents");

            List<Document> documents = new ArrayList<>();
            if (documentsNode != null && documentsNode.isArray()) {
                for (final JsonNode objNode : documentsNode) {

                    JsonNode valueNode = getNode(objNode, "value");

                    Document document = Document.builder().value(DocumentDetails.builder()
                            .dateReceived(getField(valueNode, "dateReceived"))
                            .evidenceType(getField(valueNode, "evidenceType"))
                            .evidenceProvidedBy(getField(valueNode, "evidenceProvidedBy")).build()).build();

                    documents.add(document);
                }
                Collections.sort(documents, Collections.reverseOrder());
            }
            ccdResponse.setEvidence(Evidence.builder().documents(documents).build());
        }
    }

    private Address deserializeAddressJson(JsonNode node) {
        JsonNode addressNode = getNode(node, "address");

        return Address.builder().line1(getField(addressNode, "line1"))
                .line2(getField(addressNode, "line2"))
                .town(getField(addressNode, "town"))
                .county(getField(addressNode, "county"))
                .postcode(getField(addressNode, "postcode")).build();
    }

    private Name deserializeNameJson(JsonNode node) {
        JsonNode nameNode = getNode(node, "name");

        return Name.builder()
            .title(getField(nameNode, "title"))
            .firstName(getField(nameNode, "firstName"))
            .lastName(getField(nameNode, "lastName")).build();
    }

    private Subscription deserializeSubscriberJson(JsonNode node, Subscription subscription) {
        if (node != null) {
            subscription = subscription.toBuilder()
                .tya(getField(node, "tya"))
                .email(getField(node, "email"))
                .mobile(getField(node, "mobile"))
                .subscribeSms(convertEmptyToNo(getField(node, "subscribeSms")))
                .subscribeEmail(convertEmptyToNo(getField(node, "subscribeEmail")))
                    .build();
        }

        return subscription;
    }

    private String convertEmptyToNo(String field) {
        return field == null || field.equals("") ? "No" : field;
    }

    public JsonNode getNode(JsonNode node, String field) {
        return node != null && node.has(field) && !node.get(field).getNodeType().equals(JsonNodeType.NULL) ? node.get(field) : null;
    }

    public String getField(JsonNode node, String field) {
        return node != null && !node.asText().equals("null") && node.has(field) && !node.get(field).asText().equals("null") ? node.get(field).asText() : null;
    }

    public void deserializeRegionalProcessingCenterJson(JsonNode rpcNode, SscsCaseData ccdResponse) {
        JsonNode regionalProcessingCenterNode = getNode(rpcNode, "regionalProcessingCenter");

        if (null != regionalProcessingCenterNode) {
            RegionalProcessingCenter regionalProcessingCenter = RegionalProcessingCenter.builder()
                    .name(getField(regionalProcessingCenterNode, "name"))
                    .address1(getField(regionalProcessingCenterNode, "address1"))
                    .address2(getField(regionalProcessingCenterNode, "address2"))
                    .address3(getField(regionalProcessingCenterNode, "address3"))
                    .address4(getField(regionalProcessingCenterNode, "address4"))
                    .city(getField(regionalProcessingCenterNode, "city"))
                    .postcode(getField(regionalProcessingCenterNode, "postcode"))
                    .phoneNumber(getField(regionalProcessingCenterNode, "phoneNumber"))
                    .faxNumber(getField(regionalProcessingCenterNode, "faxNumber")).build();

            ccdResponse.setRegionalProcessingCenter(regionalProcessingCenter);
        }
    }
}
