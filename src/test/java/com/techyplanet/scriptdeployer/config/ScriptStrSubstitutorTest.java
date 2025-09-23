package com.techyplanet.scriptdeployer.config;


import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import com.techyplanet.scriptdeployer.component.AppSettings;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class ScriptStrSubstitutorTest {

    @Test
    public void variablesSubstitutor_replacesConfiguredVariables() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("db.user", "alice")
                .withProperty("db.pass", "s3cr3t");

        AppSettings settings = new AppSettings();
        ReflectionTestUtils.setField(settings, "scriptVariables", "db.user,db.pass");

        ScriptStrSubstitutor cfg = new ScriptStrSubstitutor();
        ReflectionTestUtils.setField(cfg, "env", env);
        ReflectionTestUtils.setField(cfg, "appSettings", settings);

        org.apache.commons.text.StringSubstitutor substitutor = cfg.variablesSubstitutor();
        String in = "User=${db.user}, Pass=${db.pass}";
        String out = substitutor.replace(in);
        assertEquals("User=alice, Pass=s3cr3t", out);

        String unchanged = substitutor.replace("${unknown}");
        // Unknown stays as-is per StringSubstitutor default
        assertEquals("${unknown}", unchanged);
    }
}
