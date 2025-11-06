package com.yongjibus.auth.email;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GmailBounceMessageDTO(
    String emailAddress,
    @JsonProperty("HistoryId")
    String historyId
){}
