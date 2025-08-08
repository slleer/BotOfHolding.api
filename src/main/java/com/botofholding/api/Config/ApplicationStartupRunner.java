package com.botofholding.api.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * This component runs on application startup to log essential build information.
 * It provides a clear, immediate confirmation in the logs of which version
 * of the application has been deployed.
 */
@Component
public class ApplicationStartupRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupRunner.class);

    private final ApplicationInfoService appInfo;

    @Autowired
    public ApplicationStartupRunner(ApplicationInfoService appInfo) {
        this.appInfo = appInfo;
    }

    @Override
    public void run(String... args) {
        logger.info("========================================================================");
        logger.info("  Bot-O-Holding API ({})", appInfo.getName());
        logger.info("  Version:     {} (Build: {})", appInfo.getVersion(), appInfo.getBuildTimestamp());
        logger.info("========================================================================");
    }
}