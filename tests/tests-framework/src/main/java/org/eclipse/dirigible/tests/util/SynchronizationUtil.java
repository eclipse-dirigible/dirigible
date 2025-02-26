package org.eclipse.dirigible.tests.util;

import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@Component
public class SynchronizationUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronizationUtil.class);

    public static void waitForSynchronizationExecution() {
        SynchronizationProcessor synchronizationProcessor = BeanProvider.getBean(SynchronizationProcessor.class);

        LOGGER.debug("Waiting until the synchronization is not needed...");

        await().atMost(30, TimeUnit.SECONDS)
               .pollInterval(500, TimeUnit.MILLISECONDS)
               .until(() -> {
                   boolean synchNotNeeded = !synchronizationProcessor.isSynchronizationNeeded();
                   boolean synchNotRunning = !synchronizationProcessor.isSynchronizationRunning();
                   LOGGER.debug("Synchronization NOT needed: [{}], synch NOT running [{}]", synchNotNeeded, synchNotRunning);
                   return synchNotNeeded && synchNotRunning;
               });
    }
}
