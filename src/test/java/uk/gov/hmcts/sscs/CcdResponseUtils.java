package uk.gov.hmcts.sscs;

import static java.time.ZoneId.of;
import static java.time.ZonedDateTime.now;
import static uk.gov.hmcts.sscs.config.AppConstants.ZONE_ID;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import uk.gov.hmcts.sscs.domain.*;
import uk.gov.hmcts.sscs.domain.notify.Event;
import uk.gov.hmcts.sscs.domain.notify.EventType;

public final class CcdResponseUtils {
    private CcdResponseUtils() {
    }

    public static CcdResponse buildCcdResponse(String caseReference) {

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

        Events event = Events.builder()
            .value(Event.builder()
            .type(EventType.APPEAL_RECEIVED.getId())
            .description("Appeal received")
            .date("2018-01-14T21:59:43.10")
            .build())
            .build();

        Subscription appellantSubscription = Subscription.builder()
            .tya("")
            .email("jack.maloney@hmcts.net")
            .mobile("07985289708")
            .subscribeEmail("Yes")
            .subscribeSms("Yes")
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
            .events(Collections.singletonList(event))
            .subscriptions(subscriptions)
            .build();
    }

    public static void addEventTypeToCase(CcdResponse response, EventType eventType) {
        Date now = new Date();
        SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-mm-dd'T'hh:MM:ss.SSS");

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
        Evidence evidence = Evidence.builder()
                .dateReceived(LocalDate.now())
                .evidenceType("Medical")
                .evidenceProvidedBy("Caseworker").build();

        List<Evidence> evidenceList = new ArrayList<>();
        evidenceList.add(evidence);

        response.setEvidences(evidenceList);
    }

    public static void addHearing(CcdResponse response) {
        Hearing hearing = Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2018-01-01")
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
