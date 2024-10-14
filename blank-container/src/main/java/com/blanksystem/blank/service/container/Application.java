package com.blanksystem.blank.service.container;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * ::: -- NOTA -- :::
 * This annotation are important when we have jpa repositories and entities on DIFFERENT MODULES
 */
@EnableJpaRepositories(basePackages = {
        "com.blanksystem.blank.service.data"
})
@EntityScan(basePackages = {
        "com.blanksystem.blankdomain.service.external",
        "com.blanksystem.blank.service.data"
})
@EnableFeignClients(basePackages = "com.blanksystem.blankdomain.service.external")
@SpringBootApplication(scanBasePackages = {"com.blanksystem", "com.lg5.spring.kafka", "com.lg5.spring.mdc"})
@EnableAspectJAutoProxy
@Slf4j
public class Application {



    @Value("${kafka-config.bootstrap-servers}")
    private String bootstrapServers;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @PostConstruct
    public void init() {
        log.info("Application started");
        log.info("bootstrapServers: {}", bootstrapServers);
    }
}
