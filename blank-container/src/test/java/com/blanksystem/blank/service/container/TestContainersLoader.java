package com.blanksystem.blank.service.container;

import com.lg5.spring.testcontainer.container.DataBaseContainerCustomConfig;
import com.lg5.spring.testcontainer.container.KafkaContainerCustomConfig;
import com.lg5.spring.testcontainer.container.WiremockContainerCustomConfig;
import org.springframework.context.annotation.Import;

@Import({
        DataBaseContainerCustomConfig.class,
        KafkaContainerCustomConfig.class,
        WiremockContainerCustomConfig.class
})
public final class TestContainersLoader {
}
