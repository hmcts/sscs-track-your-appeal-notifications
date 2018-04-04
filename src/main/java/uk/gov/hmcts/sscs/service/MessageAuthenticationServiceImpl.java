package uk.gov.hmcts.sscs.service;

import static java.lang.String.format;
import static java.time.ZoneId.of;
import static java.time.ZonedDateTime.now;
import static java.util.Base64.getEncoder;
import static javax.crypto.Mac.getInstance;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.sscs.config.AppConstants.MAC_ALGO;
import static uk.gov.hmcts.sscs.config.AppConstants.ZONE_ID;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.exception.MacException;
import uk.gov.hmcts.sscs.exception.TokenException;

@Service
public class MessageAuthenticationServiceImpl {
    private static final Logger LOG = getLogger(MessageAuthenticationServiceImpl.class);
    public static final Charset CHARSET = StandardCharsets.UTF_8;

    private Mac mac;
    private String macString;

    @Autowired
    public MessageAuthenticationServiceImpl(@Value("${subscription.hmac.secret.text}") String macString) throws InvalidKeyException, NoSuchAlgorithmException {
        this.macString = macString;
        this.mac = initializeMac();
    }

    protected Mac initializeMac() {
        try {
            SecretKeySpec key = new SecretKeySpec(macString.getBytes(CHARSET), MAC_ALGO);
            mac = getInstance(MAC_ALGO);
            mac.init(key);
            return mac;
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            MacException macException = new MacException(ex);
            LOG.error("Error while initializing MAC Key: ", macException);
            throw macException;
        }
    }

    public String generateToken(String appealNumber, String benefitType)  {
        try {
            long timestamp = now(of(ZONE_ID)).toInstant().toEpochMilli() / 1000;
            String originalMessage = format("%s|%s|%d", appealNumber, benefitType, timestamp);
            byte[] digest = mac.doFinal(originalMessage.getBytes(CHARSET));
            String macSubString =  printBase64Binary(digest).substring(0,10);
            String macToken = format("%s|%s", originalMessage, macSubString);
            return getEncoder().withoutPadding().encodeToString(macToken.getBytes(CHARSET));
        } catch (Exception ex) {
            TokenException tokenException = new TokenException(ex);
            LOG.error("Error while generating MAC: ", tokenException);
            throw tokenException;
        }
    }
}
