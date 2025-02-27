/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
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

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        assertSpringIsInitialized();
        return context.getBean(beanName, clazz);
    }

    private static void assertSpringIsInitialized() {
        if (!isInitialzed()) {
            throw new IllegalStateException("Spring is not initialized yet.");
        }
    }

    /**
     * Checks if is initialzed.
     *
     * @return true, if is initialzed
     */
    public static boolean isInitialzed() {
        return context != null;
    }

    public static <T> T getBeanByAnnotation(Class<T> interfaceType, Class<? extends Annotation> annotationClass) {
        assertSpringIsInitialized();
        Map<String, Object> beans = context.getBeansWithAnnotation(annotationClass);
        Set<Object> matchedBeans = beans.values()
                                        .stream()
                                        .filter(bean -> interfaceType.isAssignableFrom(bean.getClass()))
                                        .collect(Collectors.toSet());
        if (matchedBeans.size() != 1) {

            throw new IllegalStateException(
                    "Found [" + matchedBeans.size() + "] of type [" + interfaceType + "] annotated with [" + annotationClass + "]");
        }

        return (T) matchedBeans.iterator()
                               .next();
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
        assertSpringIsInitialized();
        return context.getBean(clazz);
    }

    public static <T> Optional<T> getOptionalBean(Class<T> clazz) {
        assertSpringIsInitialized();
        try {
            return Optional.of(context.getBean(clazz));
        } catch (NoSuchBeanDefinitionException ex) {
            LOGGER.debug("Missing bean for [{}]", clazz, ex);
            return Optional.empty();
        }
    }

    public static <T> Collection<T> getBeans(Class<T> clazz) {
        assertSpringIsInitialized();
        return context.getBeansOfType(clazz)
                      .values();
    }

}
