package uk.gov.hmcts.reform.sscs.service;

import static com.google.common.collect.Maps.newHashMap;
import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.exception.RegionalProcessingCenterServiceException;

@Service
public class RegionalProcessingCenterService {
    private static final Logger LOG = getLogger(RegionalProcessingCenterService.class);

    public static final String RPC_DATA_JSON = "reference-data/rpc-data.json";
    private static final String CSV_FILE_PATH = "reference-data/sscs-venues.csv";
    public static final char SEPARATOR_CHAR = '/';
    public static final String SSCS_BIRMINGHAM = "SSCS Birmingham";

    private Map<String, RegionalProcessingCenter>  regionalProcessingCenterMap  = newHashMap();
    private final Map<String, String> scCodeRegionalProcessingCenterMap = newHashMap();


    @PostConstruct
    public void init() {
        loadScCodeRpcMetadata();
        populateRpcMetadata();
    }

    private void loadScCodeRpcMetadata() {
        ClassPathResource classPathResource = new ClassPathResource(CSV_FILE_PATH);
        try (CSVReader reader = new CSVReader(new InputStreamReader(classPathResource.getInputStream()))) {

            List<String[]> linesList = reader.readAll();
            linesList.forEach(line ->
                    scCodeRegionalProcessingCenterMap.put(line[1], line[2])
            );
        } catch (IOException e) {
            LOG.error("Error occurred while loading the sscs venues reference data file: " + CSV_FILE_PATH, new RegionalProcessingCenterServiceException(e));
        }
    }

    private void populateRpcMetadata() {
        ClassPathResource classPathResource = new ClassPathResource(RPC_DATA_JSON);
        try (InputStream inputStream  = classPathResource.getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            regionalProcessingCenterMap =
                    mapper.readValue(inputStream, new TypeReference<Map<String,RegionalProcessingCenter>>(){});

        } catch (IOException e) {
            LOG.error("Error while reading RegionalProcessingCenter from " + RPC_DATA_JSON, new RegionalProcessingCenterServiceException(e));
        }
    }


    public RegionalProcessingCenter getByScReferenceCode(String referenceNumber) {
        if (referenceNumber == null) {
            return regionalProcessingCenterMap.get(SSCS_BIRMINGHAM);
        }
        String[] splitReferenceNumber = StringUtils.split(referenceNumber, SEPARATOR_CHAR);
        String regionalProcessingCenter = scCodeRegionalProcessingCenterMap.get(splitReferenceNumber[0]);

        if (null != regionalProcessingCenter) {
            if (regionalProcessingCenterMap.get(regionalProcessingCenter) == null) {
                String text = "Venue could not be mapped to a valid RPC that SSCS knows about";
                LOG.error(text, new RegionalProcessingCenterServiceException(new Exception(text)));
                return null;
            } else {
                return regionalProcessingCenterMap.get(regionalProcessingCenter);
            }
        } else {
            return regionalProcessingCenterMap.get(SSCS_BIRMINGHAM);
        }
    }

    public Map<String, RegionalProcessingCenter> getRegionalProcessingCenterMap() {
        return regionalProcessingCenterMap;
    }

    public Map<String, String> getScCodeRegionalProcessingCenterMap() {
        return scCodeRegionalProcessingCenterMap;
    }

}
