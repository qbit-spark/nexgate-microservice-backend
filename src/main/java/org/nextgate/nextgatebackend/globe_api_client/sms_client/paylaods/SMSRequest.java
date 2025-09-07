package org.nextgate.nextgatebackend.globe_api_client.sms_client.paylaods;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SMSRequest {
    @JsonProperty("from")
    private String from;

    @JsonProperty("to")
    private String to;

    @JsonProperty("text")
    private String text;

    @JsonProperty("reference")
    private String reference;

    public SMSRequest(String from, String to, String text, String reference) {
        this.from = from;
        this.to = to;
        this.text = text;
        this.reference = reference;
    }
}
