package uk.gov.hmcts.reform.sscs.servicebus.messaging;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@ConditionalOnProperty(name = "spring.config.activate.on-profile", havingValue = "local")
public class LocalMessagingConfig {

    @Bean
    public SSLContext jmsSslContext(@Value("${amqp.trustAllCerts}") final boolean trustAllCerts)
            throws NoSuchAlgorithmException, KeyManagementException {

        if (trustAllCerts) {
            // https://stackoverflow.com/a/2893932
            // DO NOT USE THIS IN PRODUCTION!
            TrustManager[] trustCerts = getTrustManagers();

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustCerts, new SecureRandom());

            return sc;
        }
        return null;
    }

    /*
     * DO NOT USE THIS IN PRODUCTION!
     * This was only used for testing unverified ssl certs locally!
     */
    @Deprecated
    private TrustManager[] getTrustManagers() {
        return new TrustManager[]{
            new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(
                        X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(
                        X509Certificate[] certs, String authType) {
                }
            }
        };
    }
}
