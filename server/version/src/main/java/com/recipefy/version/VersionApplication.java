package com.recipefy.version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VersionApplication {

	private static final Logger logger = LoggerFactory.getLogger(VersionApplication.class);

	public static void main(String[] args) {
		logger.info("Starting Version Control Service application...");
		try {
			SpringApplication.run(VersionApplication.class, args);
			logger.info("Version Control Service application started successfully");
		} catch (Exception e) {
			logger.error("Failed to start Version Control Service application", e);
			throw e;
		}
	}

}
