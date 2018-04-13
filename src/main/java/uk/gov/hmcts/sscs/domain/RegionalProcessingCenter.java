package uk.gov.hmcts.sscs.domain;

import lombok.Data;

@Data
public class RegionalProcessingCenter {

    private String name;
    private String address1;
    private String address2;
    private String address3;
    private String address4;
    private String postcode;
    private String city;
    private String faxNumber;
    private String phoneNumber;

    public void createRegionalProcessingCenter(String name, String address1, String address2, String address3, String address4, String postcode, String city) {
        this.name = name;
        this.address1 = address1;
        this.address2 = address2;
        this.address3 = address3;
        this.address4 = address4;
        this.postcode = postcode;
        this.city = city;
    }
}
