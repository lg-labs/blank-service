package com.blanksystem.blankdomain.service.boot;


import com.lg5.spring.testcontainer.boot.Lg5TestBootPortNone;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.context.annotation.Import;

@Import(TestContainersLoader.class)
@CucumberContextConfiguration
public class CucumberHooks extends Lg5TestBootPortNone {

}
