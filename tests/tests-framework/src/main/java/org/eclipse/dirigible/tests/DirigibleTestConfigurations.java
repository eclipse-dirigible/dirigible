package org.eclipse.dirigible.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

@TestConfiguration
class DirigibleTestConfigurations {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirigibleTestConfigurations.class);

    @Autowired
    private DirigibleCleaner dirigibleCleaner;

    @EventListener
    @Order(Integer.MAX_VALUE)
    // Ensures this listener runs last
    void handleSpringAppStopped(ContextClosedEvent event) {
        dirigibleCleaner.afterEachMethodCleanup();
    }
}
