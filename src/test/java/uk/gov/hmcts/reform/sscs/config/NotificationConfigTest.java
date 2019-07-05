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
            "emailTemplateName, notification.emailTemplateName.emailId, emailTemplateId, smsTemplateName, notification.smsTemplateName.smsId, smsTemplateId, letterTemplateName, notification.letterTemplateName.letterId, letterTemplateId, notification.letterTemplateName.docmosisId, docmosisTemplateId",
            "emailTemplateName, notification.online.emailTemplateName.emailId, onlineEmailTemplateId, smsTemplateName, notification.online.smsTemplateName.smsId, onlineSmsTemplateId, letterTemplateName, notification.online.letterTemplateName.letterId, onlineLetterTemplateId, notification.online.letterTemplateName.docmosisId, docmosisTemplateId"
    })
    public void getDefaultTemplate(String emailTemplateName, String emailTemplateKey, String emailTemplateId,
                                   String smsTemplateName, String smsTemplateKey, String smsTemplateId,
                                   String letterTemplateName, String letterTemplateKey, String letterTemplateId, String docmosisTemplateKey, String docmosisTemplateId) {
        when(env.getProperty(emailTemplateKey)).thenReturn(emailTemplateId);
        when(env.containsProperty(emailTemplateKey)).thenReturn(true);
        when(env.getProperty(smsTemplateKey)).thenReturn(smsTemplateId);
        when(env.containsProperty(smsTemplateKey)).thenReturn(true);
        when(env.getProperty(letterTemplateKey)).thenReturn(letterTemplateId);
        when(env.getProperty(docmosisTemplateKey)).thenReturn(docmosisTemplateId);
        when(env.containsProperty(letterTemplateKey)).thenReturn(true);

        Template template = new NotificationConfig(env).getTemplate(emailTemplateName, smsTemplateName, letterTemplateName, letterTemplateName, Benefit.PIP, ONLINE);

        assertThat(template.getEmailTemplateId(), is(emailTemplateId));
        assertThat(template.getSmsTemplateId(), is(smsTemplateId));
        assertThat(template.getLetterTemplateId(), is(letterTemplateId));
        assertThat(template.getDocmosisTemplateId(), is(docmosisTemplateId));
    }

}