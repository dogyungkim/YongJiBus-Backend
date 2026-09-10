package com.yongjibus.global.infra.gmail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "gmail")
public class GmailProperties {
    private boolean enabled = false;
    private String topicName = "projects/yongji-bus/topics/auth-mail-failure";
    private String watchedLabelId = "Label_6";
    private String pubsubAudience = "";
    private String pubsubServiceAccountEmail = "";
    private final OAuth oauth = new OAuth();

    @Getter
    @Setter
    public static class OAuth {
        private String clientId = "";
        private String clientSecret = "";
        private String refreshToken = "";
    }
}
