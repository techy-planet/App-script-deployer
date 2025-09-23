package com.techyplanet.scriptdeployer.validator;

import com.techyplanet.scriptdeployer.component.AppSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class VariablesValidatorTest {

    @TempDir
    Path tmp;

    @Test
    public void validate_passesForExistingDirectory() {
        AppSettings s = new AppSettings();
        ReflectionTestUtils.setField(s, "scriptsLocation", tmp.toAbsolutePath().toString());
        VariablesValidator v = new VariablesValidator();
        ReflectionTestUtils.setField(v, "appSettings", s);
        v.validate();
    }

    @Test
    public void validate_throwsWhenLocationBlank() {
        AppSettings s = new AppSettings();
        ReflectionTestUtils.setField(s, "scriptsLocation", "");
        VariablesValidator v = new VariablesValidator();
        ReflectionTestUtils.setField(v, "appSettings", s);
        assertThrows(RuntimeException.class, v::validate);
    }

    @Test
    public void validate_throwsWhenDirectoryDoesNotExist() {
        AppSettings s = new AppSettings();
        ReflectionTestUtils.setField(s, "scriptsLocation", "/path/that/does/not/exist/xyz");
        VariablesValidator v = new VariablesValidator();
        ReflectionTestUtils.setField(v, "appSettings", s);
        assertThrows(RuntimeException.class, v::validate);
    }
}
