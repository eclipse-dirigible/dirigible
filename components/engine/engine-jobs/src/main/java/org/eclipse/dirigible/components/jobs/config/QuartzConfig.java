/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.config;

import java.io.IOException;
import java.util.Properties;
import javax.sql.DataSource;
import org.eclipse.dirigible.components.data.sources.config.SystemDataSourceName;
import org.eclipse.dirigible.components.jobs.telemetry.JobExecutionsCountListener;
import org.eclipse.dirigible.components.jobs.telemetry.JobExecutionsDurationListener;
import org.eclipse.dirigible.components.jobs.telemetry.JobFailuresCountListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.utils.DBConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The Class SystemScheduler.
 */
@Configuration
class QuartzConfig {

    private static final int STARTUP_DELAY_SECONDS = 10;
    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(QuartzConfig.class);

    @Bean
    Scheduler scheduler(SchedulerFactoryBean factory, @Qualifier("SystemDB") DataSource systemDataSource,
            @SystemDataSourceName String systemDataSourceName, JobExecutionsCountListener jobExecutionsCountListener,
            JobExecutionsDurationListener jobExecutionsDurationListener, JobFailuresCountListener jobFailuresCountListener)
            throws SchedulerException {
        factory.setDataSource(systemDataSource);
        DBConnectionManager.getInstance()
                           .addConnectionProvider(systemDataSourceName, new CustomConnectionProvider(systemDataSource));

        Scheduler scheduler = factory.getScheduler();
        logger.debug("Starting Scheduler threads");

        scheduler.getListenerManager()
                 .addJobListener(jobExecutionsCountListener);
        scheduler.getListenerManager()
                 .addJobListener(jobExecutionsDurationListener);
        scheduler.getListenerManager()
                 .addJobListener(jobFailuresCountListener);

        verifyTriggersAreReadable(scheduler);

        return scheduler;
    }

    /**
     * Reads the trigger keys once so that a job store which cannot talk to its own tables is reported
     * at startup. Without it a mismatched driver delegate degrades into a healthy looking instance that
     * fires nothing, its only trace an ERROR repeating on every scheduler poll.
     *
     * @param scheduler the scheduler
     */
    private void verifyTriggersAreReadable(Scheduler scheduler) {
        try {
            scheduler.getTriggerKeys(GroupMatcher.anyTriggerGroup());
        } catch (SchedulerException ex) {
            logger.error(
                    "The scheduler cannot read its triggers from the system database - no scheduled job will fire."
                            + " Check that the Quartz tables exist there and that the driver delegate matches that database -"
                            + " set [{}] to its delegate, for example [{}] for PostgreSQL",
                    QuartzDriverDelegateResolver.DELEGATE_OVERRIDE_KEY, QuartzDriverDelegateResolver.POSTGRESQL_DELEGATE, ex);
        }
    }

    /**
     * Scheduler factory bean.
     *
     * @param jobFactory the job factory
     * @param systemDataSourceName the name of the system data source
     * @param systemDataSource the system data source the job store runs on
     * @param transactionManager the transaction manager
     * @return the scheduler factory bean
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Bean
    SchedulerFactoryBean schedulerFactoryBean(AutoWiringSpringBeanJobFactory jobFactory, @SystemDataSourceName String systemDataSourceName,
            @Qualifier("SystemDB") DataSource systemDataSource, PlatformTransactionManager transactionManager) throws IOException {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setJobFactory(jobFactory);
        factory.setQuartzProperties(quartzProperties(systemDataSourceName, systemDataSource));
        factory.setTransactionManager(transactionManager);
        factory.setWaitForJobsToCompleteOnShutdown(false);

        logger.info("Creating QUARTZ with transaction manager [{}] for data source [{}]", transactionManager, systemDataSourceName);

        return factory;
    }

    /**
     * Quartz properties.
     *
     * @return the properties
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private Properties quartzProperties(String systemDataSourceName, DataSource systemDataSource) throws IOException {
        PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
        propertiesFactoryBean.setLocation(new ClassPathResource("/quartz.properties"));
        propertiesFactoryBean.afterPropertiesSet();

        Properties properties = propertiesFactoryBean.getObject();
        String jobStoreClass = properties.getProperty("org.quartz.jobStore.class");
        if (null != jobStoreClass && jobStoreClass.equals(JobStoreTX.class.getCanonicalName())) {
            properties.setProperty("org.quartz.jobStore.dataSource", systemDataSourceName);
            properties.setProperty("org.quartz.jobStore.driverDelegateClass", QuartzDriverDelegateResolver.resolve(systemDataSource));
        }
        return properties;
    }
}
