package uk.gov.hmcts.reform.sscs.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.ONLINE;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.env.Environment;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;

@RunWith(JUnitParamsRunner.class)
public class NotificationConfigTest {

    private final Environment env = mock(Environment.class);

    @Test
    @Parameters({
            "emailTemplateName, notification.emailTemplateName.emailId, emailTemplateId, smsTemplateName, notification.smsTemplateName.smsId, smsTemplateId, letterTemplateName, notification.letterTemplateName.letterId, letterTemplateId",
            "emailTemplateName, notification.online.emailTemplateName.emailId, onlineEmailTemplateId, smsTemplateName, notification.online.smsTemplateName.smsId, onlineSmsTemplateId, letterTemplateName, notification.online.letterTemplateName.letterId, onlineLetterTemplateId"
    })
    public void getDefaultTemplate(String emailTemplateName, String emailTemplateKey, String emailTemplateId,
                                   String smsTemplateName, String smsTemplateKey, String smsTemplateId,
                                   String letterTemplateName, String letterTemplateKey, String letterTemplateId) {
        when(env.getProperty(emailTemplateKey)).thenReturn(emailTemplateId);
        when(env.getProperty(smsTemplateKey)).thenReturn(smsTemplateId);
        when(env.getProperty(letterTemplateKey)).thenReturn(letterTemplateId);

        Template template = new NotificationConfig(env).getTemplate(emailTemplateName, smsTemplateName, letterTemplateName, Benefit.PIP, ONLINE);

        assertThat(template.getEmailTemplateId(), is(emailTemplateId));
        assertThat(template.getSmsTemplateId(), is(smsTemplateId));
        assertThat(template.getLetterTemplateId(), is(letterTemplateId));
    }

}