/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.camel.config;

import org.apache.camel.CamelContext;
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.component.platform.http.spi.PlatformHttpEngine;
import org.apache.camel.component.platform.http.springboot.CamelRequestHandlerMapping;
import org.apache.camel.component.platform.http.springboot.SpringBootPlatformHttpAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * The Class CamelDirigibleConfigurationTest.
 */
@ExtendWith(MockitoExtension.class)
class CamelDirigibleConfigurationTest {

    /** The camel context. */
    @Mock
    private CamelContext camelContext;

    /** The camel request handler mapping. */
    @Mock
    private CamelRequestHandlerMapping camelRequestHandlerMapping;

    /** The http engine. */
    @Mock
    private PlatformHttpEngine httpEngine;

    /** The http component. */
    @Mock
    private PlatformHttpComponent httpComponent;

    /**
     * Test create camel request handler mapping.
     */
    @Test
    void testCreateCamelRequestHandlerMapping() {
        when(camelContext.getComponent("platform-http", PlatformHttpComponent.class)).thenReturn(httpComponent);

        CamelRequestHandlerMapping res =
                new CamelDirigibleConfiguration().createCamelRequestHandlerMapping(camelContext, httpEngine, camelRequestHandlerMapping);

        verify(httpComponent, times(1)).removePlatformHttpListener(camelRequestHandlerMapping);
        assertNotNull(res, "CamelRequestHandlerMapping should not be null");
    }

    /**
     * Test spring boot platform http auto configuration full name.
     */
    @Test
    void testSpringBootPlatformHttpAutoConfigurationFullName() {
        assertEquals("org.apache.camel.component.platform.http.springboot.SpringBootPlatformHttpAutoConfiguration",
                SpringBootPlatformHttpAutoConfiguration.class.getName(),
                "Unexpected SpringBootPlatformHttpAutoConfiguration full class name");
    }

    /**
     * Test camel dirigible configuration correct spring annotations.
     */
    @Test
    void testCamelDirigibleConfigurationCorrectSpringAnnotations() {
        var classAnnotations = CamelDirigibleConfiguration.class.getAnnotations();
        assertEquals(1, classAnnotations.length, "Unexpected number of annotations");

        var configuration = firstAnnotationWithClass(classAnnotations, Configuration.class);
        assertTrue(configuration.isPresent(), "No @Configuration found on CamelDirigibleConfiguration");
    }

    /**
     * First annotation with class.
     *
     * @param annotations the annotations
     * @param annotationClass the annotation class
     * @return the optional
     */
    private static Optional<Annotation> firstAnnotationWithClass(Annotation[] annotations, Class<?> annotationClass) {
        return Arrays.stream(annotations)
                     .filter(a -> a.annotationType()
                                   .equals(annotationClass))
                     .findFirst();
    }

    /**
     * Test camel dirigible configuration correct spring bean annotations.
     */
    @Test
    void testCamelDirigibleConfigurationCorrectSpringBeanAnnotations() {
        Method beanFactoryMethod = getBeanFactoryMethodOrFail();

        var beanFactoryMethodAnnotations = beanFactoryMethod.getAnnotations();
        assertEquals(2, beanFactoryMethodAnnotations.length, "Unexpected number of annotations");

        var bean = firstAnnotationWithClass(beanFactoryMethodAnnotations, Bean.class);
        assertTrue(bean.isPresent(), "No @Bean found on CamelDirigibleConfiguration");

        var primary = firstAnnotationWithClass(beanFactoryMethodAnnotations, Primary.class);
        assertTrue(primary.isPresent(), "No @Primary found on CamelDirigibleConfiguration");
    }

    /**
     * Gets the bean factory method or fail.
     *
     * @return the bean factory method or fail
     */
    private static Method getBeanFactoryMethodOrFail() {
        try {
            return CamelDirigibleConfiguration.class.getMethod("createCamelRequestHandlerMapping", CamelContext.class,
                    PlatformHttpEngine.class, CamelRequestHandlerMapping.class);
        } catch (Exception e) {
            fail("CamelDirigibleConfiguration::createCamelRequestHandlerMapping does not exist");
            throw new RuntimeException(e);
        }
    }

    /**
     * Test camel dirigible configuration bean factory method return type.
     */
    @Test
    void testCamelDirigibleConfigurationBeanFactoryMethodReturnType() {
        Method beanFactoryMethod = getBeanFactoryMethodOrFail();
        assertEquals(CamelRequestHandlerMapping.class, beanFactoryMethod.getReturnType(),
                "Unexpected CamelDirigibleConfiguration::createCamelRequestHandlerMapping return type");
    }
}
