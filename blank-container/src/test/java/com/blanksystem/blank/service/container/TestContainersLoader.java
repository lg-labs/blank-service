package com.blanksystem.blank.service.container;

import com.lg5.spring.testcontainer.DataBaseContainerCustomConfig;
import com.lg5.spring.testcontainer.KafkaContainerCustomConfig;
import com.lg5.spring.testcontainer.WiremockContainerCustomConfig;
import org.springframework.context.annotation.Import;

@Import({
        DataBaseContainerCustomConfig.class,
        KafkaContainerCustomConfig.class,
        WiremockContainerCustomConfig.class
})
public final class TestContainersLoader {
}
