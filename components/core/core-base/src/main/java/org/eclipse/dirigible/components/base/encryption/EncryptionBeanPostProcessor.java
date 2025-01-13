/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.encryption;

import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import jakarta.persistence.EntityManagerFactory;

/**
 * https://github.com/galovics/hibernate-encryption-listener
 */
@Component
public class EncryptionBeanPostProcessor implements BeanPostProcessor {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(EncryptionBeanPostProcessor.class);

    /** The encryption listener. */
    @Autowired
    private EncryptionListener encryptionListener;

    /**
     * Post process before initialization.
     *
     * @param bean the bean
     * @param beanName the bean name
     * @return the object
     * @throws BeansException the beans exception
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * Post process after initialization.
     *
     * @param bean the bean
     * @param beanName the bean name
     * @return the object
     * @throws BeansException the beans exception
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof EntityManagerFactory emf) {
            SessionFactoryImpl sessionFactory = emf.unwrap(SessionFactoryImpl.class);
            EventListenerRegistry registry = sessionFactory.getServiceRegistry()
                                                           .getService(EventListenerRegistry.class);

            registry.appendListeners(EventType.PRE_LOAD, encryptionListener);
            registry.appendListeners(EventType.PRE_INSERT, encryptionListener);
            registry.appendListeners(EventType.PRE_UPDATE, encryptionListener);
            logger.info("Encryption has been successfully set up");
        }
        return bean;
    }
}
