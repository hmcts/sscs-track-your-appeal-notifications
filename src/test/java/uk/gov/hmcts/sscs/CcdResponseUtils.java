package uk.gov.hmcts.sscs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import uk.gov.hmcts.sscs.domain.*;
import uk.gov.hmcts.sscs.domain.notify.Event;
import uk.gov.hmcts.sscs.domain.notify.EventType;

public final class CcdResponseUtils {

    public static final String CASE_ID = "1234";

    private CcdResponseUtils() {
    }

    public static CcdResponse buildCcdResponse(String caseReference, String subscribeEmail, String subscribeSms) {
        return buildCcdResponse(
            caseReference,
            subscribeEmail,
            subscribeSms,
            EventType.APPEAL_RECEIVED
        );
    }

    public static CcdResponse buildCcdResponse(
        String caseReference,
        String subscribeEmail,
        String subscribeSms,
        EventType eventType
    ) {
        Name name = Name.builder()
            .title("Mr")
            .firstName("User")
            .lastName("Test")
            .build();
        Contact contact = Contact.builder()
            .email("mail@email.com")
            .phone("01234567890")
            .build();
        Identity identity = Identity.builder()
            .dob("1904-03-10")
            .nino("AB 22 55 66 B")
            .build();
        Appellant appellant = Appellant.builder()
            .name(name)
            .contact(contact)
            .identity(identity)
            .build();

        HearingOptions hearingOptions = HearingOptions.builder()
            .wantsToAttend("Yes")
            .wantsSupport("Yes")
            .languageInterpreter("Yes")
            .other("No")
            .build();

        final Appeal appeal = Appeal.builder()
            .appellant(appellant)
            .benefitType(BenefitType.builder().code("ESA").build())
            .hearingOptions(hearingOptions)
            .build();

        Events events = Events.builder()
            .value(Event.builder()
                .type(eventType.getId())
                .description("Some Events")
                .date("2017-05-24T14:01:18.243")
                .build())
            .build();

        Subscription appellantSubscription = Subscription.builder()
            .tya("")
            .email("sscstest+notify@greencroftconsulting.com")
            .mobile("07398785050")
            .subscribeEmail(subscribeEmail)
            .subscribeSms(subscribeSms)
            .build();
        Subscription supporterSubscription = Subscription.builder()
            .tya("")
            .email("")
            .mobile("")
            .subscribeEmail("No")
            .subscribeSms("No")
            .build();
        Subscriptions subscriptions = Subscriptions.builder()
            .appellantSubscription(appellantSubscription)
            .supporterSubscription(supporterSubscription)
            .build();

        return CcdResponse.builder()
            .caseReference(caseReference)
            .appeal(appeal)
            .events(Collections.singletonList(events))
            .subscriptions(subscriptions)
            .build();
    }

    public static CcdResponse buildBasicCcdResponse(EventType notificationType) {
        return CcdResponse.builder()
            .caseId(CASE_ID)
            .notificationType(notificationType)
            .events(Collections.emptyList())
            .hearings(Collections.emptyList())
            .build();
    }

    public static CcdResponse buildBasicCcdResponseWithEvent(
        EventType notificationType,
        EventType eventType,
        String eventDate
    ) {
        Events event = Events
            .builder()
            .value(Event
                .builder()
                .date(eventDate)
                .type(eventType.getId())
                .build()
            )
            .build();

        return CcdResponse.builder()
            .caseId(CASE_ID)
            .notificationType(notificationType)
            .events(Collections.singletonList(event))
            .hearings(Collections.emptyList())
            .build();
    }

    public static CcdResponse buildBasicCcdResponseWithHearing(
        EventType notificationType,
        String hearingDate,
        String hearingTime
    ) {
        Hearing hearing = Hearing
            .builder()
            .value(HearingDetails
                .builder()
                .hearingDate(hearingDate)
                .time(hearingTime)
                .build()
            )
            .build();

        return CcdResponse.builder()
            .caseId(CASE_ID)
            .notificationType(notificationType)
            .events(Collections.emptyList())
            .hearings(Collections.singletonList(hearing))
            .build();
    }

    public static void addEventTypeToCase(CcdResponse response, EventType eventType) {
        Date now = new Date();
        SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");

        Events events = Events.builder()
            .value(Event.builder()
                .type(eventType.getId())
                .description(eventType.getId())
                .date(dt1.format(now))
                .build())
            .build();

        List<Events> addedEvents = new ArrayList<>(response.getEvents());
        addedEvents.add(events);
        response.setEvents(addedEvents);
    }

    public static void addEvidence(CcdResponse response) {
        List<Documents> documents = new ArrayList<>();

        Documents doc = Documents.builder().value(Doc.builder()
            .dateReceived("2016-01-01")
            .evidenceType("Medical")
            .evidenceProvidedBy("Caseworker").build()).build();

        documents.add(doc);

        Evidence evidence = Evidence.builder().documents(documents).build();

        response.setEvidence(evidence);
    }

    public static void addHearing(CcdResponse response) {
        Hearing hearing = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2016-01-01")
            .time("12:00")
            .venue(Venue.builder()
                .name("The venue")
                .address(Address.builder()
                    .line1("12 The Road Avenue")
                    .line2("Village")
                    .town("Aberdeen")
                    .county("Aberdeenshire")
                    .postcode("AB12 0HN").build())
                .googleMapLink("http://www.googlemaps.com/aberdeenvenue")
                .build()).build()).build();

        List<Hearing> hearingsList = new ArrayList<>();
        hearingsList.add(hearing);

        response.setHearings(hearingsList);
    }

}
