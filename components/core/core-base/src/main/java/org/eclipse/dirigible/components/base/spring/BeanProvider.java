/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.spring;

import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;

/**
 * The Class BeanProvider.
 */
@Component
public class BeanProvider implements ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeanProvider.class);

    /** The context. */
    private static ApplicationContext context;

    /**
     * Sets the application context.
     *
     * @param argApplicationContext the new application context
     * @throws BeansException the beans exception
     */
    @Override
    public void setApplicationContext(ApplicationContext argApplicationContext) throws BeansException {
        context = argApplicationContext;
    }

    public static <T> T getBean(String beanName, Class<T> clazz) {
        if (!isInitialzed()) {
            throw new IllegalStateException("Spring is not initialized yet.");
        }
        return context.getBean(beanName, clazz);
    }

    /**
     * Checks if is initialzed.
     *
     * @return true, if is initialzed
     */
    public static boolean isInitialzed() {
        return context != null;
    }

    /**
     * Gets the tenant context.
     *
     * @return the tenant context
     */
    public static TenantContext getTenantContext() {
        return getBean(TenantContext.class);
    }

    /**
     * Gets the bean.
     *
     * @param <T> the generic type
     * @param clazz the clazz
     * @return the bean
     */
    public static <T> T getBean(Class<T> clazz) {
        if (!isInitialzed()) {
            throw new IllegalStateException("Spring is not initialized yet.");
        }
        return context.getBean(clazz);
    }

    public static <T> Optional<T> getOptionalBean(Class<T> clazz) {
        if (!isInitialzed()) {
            throw new IllegalStateException("Spring is not initialized yet.");
        }
        try {
            return Optional.of(context.getBean(clazz));
        } catch (NoSuchBeanDefinitionException ex) {
            LOGGER.debug("Missing bean for [{}]", clazz, ex);
            return Optional.empty();
        }
    }

    public static <T> Collection<T> getBeans(Class<T> clazz) {
        if (!isInitialzed()) {
            throw new IllegalStateException("Spring is not initialized yet.");
        }
        return context.getBeansOfType(clazz)
                      .values();
    }
}
