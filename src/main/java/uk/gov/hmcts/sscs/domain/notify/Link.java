package uk.gov.hmcts.sscs.domain.notify;

public class Link {

    private final String linkUrl;

    public Link(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public String replace(String key, String value) {
        return linkUrl.replace(key, value);
    }
}
