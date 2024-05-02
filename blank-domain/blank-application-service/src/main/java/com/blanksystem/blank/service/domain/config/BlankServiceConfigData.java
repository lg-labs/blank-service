package com.blanksystem.blank.service.domain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "blanksystem.blank.service")
public class BlankServiceConfigData {
    private String blankTopicName;

}
