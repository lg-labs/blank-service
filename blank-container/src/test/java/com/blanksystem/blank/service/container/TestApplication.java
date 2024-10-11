package com.blanksystem.blank.service.container;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration(proxyBeanMethods = false)
@Import(TestContainersLoader.class)
class TestApplication {

    public static void main(String[] args) {
        SpringApplication.from(Application::main)
                .with(TestApplication.class)
                .run(args);
    }

}
