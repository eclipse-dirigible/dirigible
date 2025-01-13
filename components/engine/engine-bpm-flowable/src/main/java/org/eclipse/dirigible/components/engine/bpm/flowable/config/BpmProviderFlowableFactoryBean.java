/*
 * Copyright (c) 2010-2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.config;

import org.eclipse.dirigible.components.engine.bpm.flowable.provider.BpmProviderFlowable;
import org.eclipse.dirigible.repository.api.IRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * The Class BpmProviderFlowableFactoryBean.
 */
@Component
class BpmProviderFlowableFactoryBean implements FactoryBean<BpmProviderFlowable>, DisposableBean, ApplicationContextAware {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BpmProviderFlowableFactoryBean.class);

    /** The bpm provider flowable. */
    private BpmProviderFlowable bpmProviderFlowable;

    /** The application context. */
    private ApplicationContext applicationContext;

    /**
     * Destroy.
     */
    @Override
    public void destroy() {
        LOGGER.info("Destroying bean...");
        if (bpmProviderFlowable != null) {
            bpmProviderFlowable.cleanup();
            this.bpmProviderFlowable = null;
        }
    }

    /**
     * Sets the application context.
     *
     * @param applicationContext the new application context
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Checks if is singleton.
     *
     * @return true, if is singleton
     */
    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * Gets the object.
     *
     * @return the object
     */
    @Override
    public synchronized BpmProviderFlowable getObject() {
        if (null == bpmProviderFlowable) {
            DataSource datasource = applicationContext.getBean("SystemDB", DataSource.class);
            IRepository repository = applicationContext.getBean(IRepository.class);
            DataSourceTransactionManager dataSourceTransactionManager = applicationContext.getBean(DataSourceTransactionManager.class);

            bpmProviderFlowable = new BpmProviderFlowable(datasource, repository, dataSourceTransactionManager, applicationContext);
        }
        return bpmProviderFlowable;
    }

    /**
     * Gets the object type.
     *
     * @return the object type
     */
    @Override
    public Class<BpmProviderFlowable> getObjectType() {
        return BpmProviderFlowable.class;
    }

}
