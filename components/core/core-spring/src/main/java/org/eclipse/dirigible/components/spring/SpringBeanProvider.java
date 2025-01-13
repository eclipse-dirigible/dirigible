/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.spring;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * The Class SpringBeanProvider.
 */
@Component
public class SpringBeanProvider implements InitializingBean {

    /** The instance. */
    private static SpringBeanProvider INSTANCE;

    /** The application context. */
    private final ApplicationContext applicationContext;

    /**
     * Instantiates a new spring bean provider.
     *
     * @param applicationContext the application context
     */
    @Autowired
    public SpringBeanProvider(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * After properties set.
     */
    @Override
    public void afterPropertiesSet() {
        INSTANCE = this;
    }

    /**
     * Gets the bean.
     *
     * @param <T> the generic type
     * @param clazz the clazz
     * @return the bean
     */
    public static <T> T getBean(Class<T> clazz) {
        return INSTANCE.applicationContext.getBean(clazz);
    }

    /**
     * Gets the bean.
     *
     * @param <T> the generic type
     * @param className the class name
     * @return the bean
     */
    public static <T> T getBean(String className) {
        return (T) INSTANCE.applicationContext.getBean(className);
    }
}
