package uk.gov.hmcts.reform.sscs.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.ONLINE;

import org.junit.Test;
import org.springframework.core.env.Environment;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;

public class NotificationConfigTest {
    @Test
    public void getDefaultTemplate() {
        Environment env = mock(Environment.class);
        when(env.getProperty("notification.emailTemplateName.emailId")).thenReturn("emailTemplateId");
        when(env.getProperty("notification.smsTemplateName.smsId")).thenReturn("smsTemplateId");

        Template template = new NotificationConfig(env).getTemplate("emailTemplateName",
                "smsTemplateName", Benefit.PIP, ONLINE);

        assertThat(template.getEmailTemplateId(), is("emailTemplateId"));
        assertThat(template.getSmsTemplateId(), is("smsTemplateId"));
    }

    @Test
    public void getHearingTypeSpecificTemplate() {
        Environment env = mock(Environment.class);
        when(env.getProperty("notification.online.emailTemplateName.emailId")).thenReturn("onlineEmailTemplateId");
        when(env.getProperty("notification.online.smsTemplateName.smsId")).thenReturn("onlineSmsTemplateId");

        Template template = new NotificationConfig(env).getTemplate("emailTemplateName",
                "smsTemplateName", Benefit.PIP, ONLINE);

        assertThat(template.getEmailTemplateId(), is("onlineEmailTemplateId"));
        assertThat(template.getSmsTemplateId(), is("onlineSmsTemplateId"));
    }
}