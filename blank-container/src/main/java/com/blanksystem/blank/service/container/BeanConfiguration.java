package com.blanksystem.blank.service.container;


import com.blanksystem.blank.service.domain.BlankDomainService;
import com.blanksystem.blank.service.domain.BlankDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public BlankDomainService customerDomainService() {
        return new BlankDomainServiceImpl();
    }
}
