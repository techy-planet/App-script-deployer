package com.techyplanet.scriptdeployer;

import java.io.File;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationHome;

import com.techyplanet.scriptdeployer.component.AppSettings;
import com.techyplanet.scriptdeployer.component.DBSpooler;
import com.techyplanet.scriptdeployer.service.FileProcessorService;
import com.techyplanet.scriptdeployer.validator.VariablesValidator;

@SpringBootApplication
public class ScriptDeployerApplication implements CommandLineRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScriptDeployerApplication.class);

	@Autowired
	private AppSettings appSettings;

	@Autowired
	private FileProcessorService fileProcessorService;

	@Autowired
	private VariablesValidator variablesValidator;

	@Autowired
	private DBSpooler dbSpooler;

	public static void main(String[] args) {
		// Ensure application.properties placed next to the jar overrides packaged defaults
		// even when the jar is launched from a different working directory.
		// We add the jar's directory (and its config/) to Spring Boot's additional config locations
		// if it can be resolved. This has higher precedence than classpath resources.
		ApplicationHome home = new ApplicationHome(ScriptDeployerApplication.class);
		File source = home.getSource();
		File jarDir = (source != null && source.isFile()) ? source.getParentFile() : null;
		if (jarDir == null) {
			jarDir = home.getDir();
		}
		if (jarDir != null && jarDir.isDirectory()) {
			String existing = System.getProperty("spring.config.additional-location");
			String jarDirPath = jarDir.getAbsolutePath();
			String addLoc = "file:" + jarDirPath + "/,optional:" + jarDirPath + "/config/";
			if (existing == null || existing.trim().isEmpty()) {
				System.setProperty("spring.config.additional-location", addLoc);
			} else {
				System.setProperty("spring.config.additional-location", existing + "," + addLoc);
			}
		}
		SpringApplication.run(ScriptDeployerApplication.class, args);
	}

	@Override
	public void run(String... args) {
		try {
			dbSpooler.spoolDB("before_");
			LOGGER.info("=================================================");
			LOGGER.info("Execution Started");
			LOGGER.info("=================================================");
			variablesValidator.validate();
			File scriptsDir = Paths.get(appSettings.getScriptsLocation()).toFile();
			fileProcessorService.processPreRunFiles(scriptsDir);
			fileProcessorService.processOneTimeFiles(scriptsDir);
			fileProcessorService.processRepeatableFiles(scriptsDir);
			fileProcessorService.processPostRunFiles(scriptsDir);
			LOGGER.info("=================================================");
			LOGGER.info("Execution finished");
			LOGGER.info("=================================================");
			dbSpooler.spoolDB("after_");
			LOGGER.info("Execution completed.");
		} catch (Exception ex) {
			if (appSettings.isTraceRequired()) {
				LOGGER.error(ex.getMessage(), ex);
			} else {
				LOGGER.error(ex.getMessage());
			}
			LOGGER.error("=================================================");
			LOGGER.error("<<< Execution failed ! >>>");
			LOGGER.error("=================================================");
			System.exit(1000);
		}
	}

}
