package com.ghostchu.crowdincopydeploy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        LOG.info("CrowdinCopyDeploy v1.0.0");
        LOG.info("Starting up...");
        new CrowdinDeployer(new CrowdinAPI());
        LOG.info("All completed");
    }
}