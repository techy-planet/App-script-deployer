package com.techyplanet.scriptdeployer.utils;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandUtilsRedirectionTest {

    @Test
    public void testRedirection() throws Exception {
        File tempFile = File.createTempFile("redirection_test", ".txt");
        tempFile.deleteOnExit();
        
        String command;
        if (SystemUtils.IS_OS_WINDOWS) {
            command = "echo hello > " + tempFile.getAbsolutePath();
        } else {
            command = "echo hello > " + tempFile.getAbsolutePath();
        }
        
        boolean success = CommandUtils.execute(command, true);
        assertTrue(success, "Command should execute successfully");
        
        List<String> lines = Files.readAllLines(tempFile.toPath());
        assertEquals("hello", lines.get(0).trim(), "File should contain 'hello'");
    }
}
