package com.blanksystem.blankdomain.service.atdd;


import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({
        TestContainerConfig.class
        })
public class CucumberHooks {

}
