package com.rbctcsworld.ecommerce.qa.runners;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Runs every .feature under src/test/resources/features.
 * Filter by tag: mvn test -Dcucumber.filter.tags="@cart and not @wip"
 * Reports: target/cucumber-report.html and Allure (target/allure-results -> mvn allure:serve)
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.rbctcsworld.ecommerce.qa.stepdefinitions")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:target/cucumber-report.html, json:target/cucumber-report.json, io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm")
public class CucumberTest {
}
