package com.techyplanet.scriptdeployer.utils;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.Level;

public class CommandUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(CommandUtils.class);

	public static final boolean executeAndPrintOnFail(String command, boolean stopOnScriptFail) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
		try {
			checkOSAndExecute(command, streamHandler);
			return true;
		} catch (Exception ex) {
			LOGGER.error(
					"Error occured in command execution.\n=============================Error Output===========================\n\n{}\n\n====================================================================",
					outputStream.toString());
			if (stopOnScriptFail) {
				throw new RuntimeException(ex);
			}
			return false;
		}
	}

	public static final boolean execute(String command, boolean stopOnScriptFail) {
		try {
			checkOSAndExecute(command, dynmaicLogStreamHandler());
			return true;
		} catch (Exception ex) {
			LOGGER.error("Error occured in command execution.");
			if (stopOnScriptFail) {
				throw new RuntimeException(ex);
			}
			return false;
		}
	}

	private static final int checkOSAndExecute(String command, ExecuteStreamHandler executeStreamHandler) {
		CommandLine commandLine = null;
		if (SystemUtils.IS_OS_WINDOWS) {
			commandLine = CommandLine.parse("cmd.exe ");
			commandLine.addArgument("/c");
			commandLine.addArgument(command, false);
		} else {
			commandLine = CommandLine.parse("/bin/bash");
			commandLine.addArgument("-c");
			commandLine.addArgument(command, false);
		}
		int exitCode = executeCommand(commandLine, executeStreamHandler);
		return exitCode;

	}

	private static final int executeCommand(CommandLine commandLine, ExecuteStreamHandler executeStreamHandler) {
		DefaultExecutor defaultExecutor = new DefaultExecutor();

		defaultExecutor.setExitValue(0);
		defaultExecutor.setStreamHandler(executeStreamHandler);

		try {
			return defaultExecutor.execute(commandLine);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private static PumpStreamHandler dynmaicLogStreamHandler() {
		final Map<String, String> loggerContextMap = MDC.getCopyOfContextMap();
		return new PumpStreamHandler(new LogOutputStream() {
			@Override
			protected void processLine(String logLine, int logLevel) {
				if (loggerContextMap != null)
					MDC.setContextMap(loggerContextMap);
				if (Level.ERROR_INT == logLevel) {
					LOGGER.error(logLine);
				} else if (Level.WARN_INT == logLevel) {
					LOGGER.warn(logLine);
				} else if (Level.DEBUG_INT == logLevel) {
					LOGGER.debug(logLine);
				} else {
					LOGGER.info(logLine);
				}
			}
		});
	}
}
