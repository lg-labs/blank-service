package com.blanksystem.blank.service.boot;

import com.lg5.spring.integration.test.boot.Lg5TestBootPortNone;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.context.annotation.Import;

@Import(TestContainersLoader.class)
@CucumberContextConfiguration
public class CucumberHooks extends Lg5TestBootPortNone {

}
