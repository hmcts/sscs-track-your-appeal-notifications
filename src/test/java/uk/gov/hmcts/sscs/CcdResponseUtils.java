package uk.gov.hmcts.sscs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

        Events event = Events.builder()
            .value(Event.builder()
            .type(EventType.APPEAL_RECEIVED.getId())
            .description("Appeal received")
            .date("2018-01-14T21:59:43.10")
            .build())
            .build();

        Subscription appellantSubscription = Subscription.builder()
            .tya("")
            .email("sscstest@greencroftconsulting.com")
            .mobile("")
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
            .hearings(hearingsList)
            .events(Collections.singletonList(event))
            .subscriptions(subscriptions)
            .build();
    }
}
