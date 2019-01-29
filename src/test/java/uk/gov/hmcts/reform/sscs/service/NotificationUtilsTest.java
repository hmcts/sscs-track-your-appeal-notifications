package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SYA_APPEAL_CREATED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.service.NotificationServiceTest.APPELLANT_WITH_ADDRESS;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasAppointee;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasRepresentative;
import static uk.gov.hmcts.reform.sscs.service.SendNotificationServiceTest.APPELLANT_WITH_ADDRESS_AND_APPOINTEE;

import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

public class NotificationUtilsTest {
    @Test
    public void trueWhenHasPopulatedAppointee() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            null,
            null
        );

        assertTrue(hasAppointee(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void faleWhenHasNullPopulatedAppointee() {
        Appellant appellant = Appellant.builder()
            .name(Name.builder().firstName("Ap").lastName("pellant").build())
            .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 3LL").build())
            .appointee(Appointee.builder().build())
            .build();

        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            appellant,
            null,
            null
        );

        assertFalse(hasAppointee(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void faleWhenHasNullAppointee() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        assertFalse(hasAppointee(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void trueWhenHasPopulatedRep() {
        Representative rep = Representative.builder()
            .hasRepresentative("Yes")
            .name(Name.builder().firstName("Joe").lastName("Bloggs").build())
            .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
            .build();

        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS,
            rep,
            null
        );

        assertTrue(hasRepresentative(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void falseWhenHasPopulatedRepButHasRepSetToNo() {
        Representative rep = Representative.builder()
            .hasRepresentative("No")
            .name(Name.builder().firstName("Joe").lastName("Bloggs").build())
            .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
            .build();

        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS,
            rep,
            null
        );

        assertFalse(hasRepresentative(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void falseWhenHasPopulatedRepButHasRepNotSet() {
        Representative rep = Representative.builder()
            .name(Name.builder().firstName("Joe").lastName("Bloggs").build())
            .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
            .build();

        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS,
            rep,
            null
        );

        assertFalse(hasRepresentative(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void falseWhenHasNullPopulatedRep() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS,
            Representative.builder().build(),
            null
        );

        assertFalse(hasRepresentative(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void falseWhenHasNullRep() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        assertFalse(hasRepresentative(wrapper.getSscsCaseDataWrapper()));
    }
}
