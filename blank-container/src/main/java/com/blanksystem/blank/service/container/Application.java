package com.blanksystem.blank.service.container;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


/**
 * ::: -- NOTA -- :::
 * This annotation are important when we have jpa repositories and entities on DIFFERENT MODULES
 */
@EnableJpaRepositories(basePackages = {
        "com.blanksystem.blank.service.data"
})
@EntityScan(basePackages = {
        "com.blanksystem.blank.service.data"
})
@SpringBootApplication(scanBasePackages = {"com.blanksystem", "com.lg5.spring.kafka"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
