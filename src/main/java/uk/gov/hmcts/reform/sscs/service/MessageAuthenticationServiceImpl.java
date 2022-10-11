package uk.gov.hmcts.reform.sscs.service;

import static java.lang.String.format;
import static java.time.ZonedDateTime.now;
import static java.util.Base64.getEncoder;
import static javax.crypto.Mac.getInstance;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.ZONE_LONDON;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.exception.MacException;
import uk.gov.hmcts.reform.sscs.exception.TokenException;

@Service
@Slf4j
public class MessageAuthenticationServiceImpl {
    public static final Charset CHARSET = StandardCharsets.UTF_8;

    private Mac mac;
    private String macString;

    @Autowired
    public MessageAuthenticationServiceImpl(@Value("${subscription.hmac.secret.text}") String macString) throws InvalidKeyException, NoSuchAlgorithmException {
        this.macString = macString;
        this.mac = initializeMac();
    }

    protected Mac initializeMac() throws NoSuchAlgorithmException, InvalidKeyException {
        try {
            SecretKeySpec key = new SecretKeySpec(macString.getBytes(CHARSET), AppConstants.MAC_ALGO);
            mac = getInstance(AppConstants.MAC_ALGO);
            mac.init(key);
            return mac;
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            log.error("Error while initializing MAC Key", new MacException(ex));
            throw ex;
        }
    }

    public String generateToken(String appealNumber, String benefitType)  {
        try {
            long timestamp = now(ZONE_LONDON).toInstant().toEpochMilli() / 1000;
            String originalMessage = format("%s|%s|%d", appealNumber, benefitType, timestamp);
            byte[] digest = mac.doFinal(originalMessage.getBytes(CHARSET));
            String macSubString =  printBase64Binary(digest).substring(0,10);
            String macToken = format("%s|%s", originalMessage, macSubString);
            return getEncoder().withoutPadding().encodeToString(macToken.getBytes(CHARSET));
        } catch (Exception ex) {
            TokenException tokenException = new TokenException(ex);
            log.error("Error while generating MAC", tokenException);
            throw tokenException;
        }
    }
}
