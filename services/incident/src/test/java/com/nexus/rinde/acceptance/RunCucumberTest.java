package com.nexus.rinde.acceptance;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/incident")
@ConfigurationParameter(key = "cucumber.glue", value = "com.nexus.rinde.acceptance")
public class RunCucumberTest {}
