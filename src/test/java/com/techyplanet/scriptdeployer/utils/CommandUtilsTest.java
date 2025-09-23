package com.techyplanet.scriptdeployer.utils;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CommandUtilsTest {

    private String successCommand() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return "echo hello";
        } else {
            return "echo hello";
        }
    }

    private String failCommand() {
        // Nonexistent command should fail on all OSes
        return "definitely_nonexistent_command_12345";
    }

    @Test
    public void execute_returnsTrueOnSuccess_falseOnFailureWhenNotStopping() {
        assertTrue(CommandUtils.execute(successCommand(), false));
        assertFalse(CommandUtils.execute(failCommand(), false));
    }

    @Test
    public void execute_throwsOnFailureWhenStopOnFailTrue() {
        assertThrows(RuntimeException.class, () -> CommandUtils.execute(failCommand(), true));
    }

    @Test
    public void executeAndPrintOnFail_behavesLikeExecuteRegardingReturn() {
        assertTrue(CommandUtils.executeAndPrintOnFail(successCommand(), false));
        assertFalse(CommandUtils.executeAndPrintOnFail(failCommand(), false));
    }

    @Test
    public void executeAndPrintOnFail_throwsOnFailureWhenStopOnFailTrue() {
        assertThrows(RuntimeException.class, () -> CommandUtils.executeAndPrintOnFail(failCommand(), true));
    }
}
