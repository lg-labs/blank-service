package com.blanksystem.blank.service.support.journal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "blanksystem.blank.events.journal.blank")
public class BlankServiceConfigData {
    private String topic;

}
