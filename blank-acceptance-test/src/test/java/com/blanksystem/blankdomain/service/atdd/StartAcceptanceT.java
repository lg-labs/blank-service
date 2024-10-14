package com.blanksystem.blankdomain.service.atdd;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.ConfigurationParameters;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;
import org.springframework.context.annotation.Import;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameters({
        @ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty, json:target/cucumber.json, " +
                "html:target/cucumber-reports.html"),
        @ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.blanksystem.blankdomain.service")
})
class StartAcceptanceT {



    @Test
    void test() {
        File feature = new File("src/test/resources/features");
        assertTrue(feature.exists());
    }
}