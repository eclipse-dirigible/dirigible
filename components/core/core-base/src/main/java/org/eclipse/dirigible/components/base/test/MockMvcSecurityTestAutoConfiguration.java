/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.test;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcConfigurerAdapter;
import org.springframework.web.context.WebApplicationContext;

/**
 * Restores the Spring Boot 3.x behaviour of automatically applying the Spring Security MockMvc
 * configurer (i.e. {@code apply(springSecurity())}) so that {@code @WithMockUser} and friends
 * continue to work with {@link org.springframework.test.web.servlet.MockMvc} under Spring Boot 4.
 *
 * <p>
 * Mirrors the dropped {@code MockMvcSecurityConfiguration} from spring-boot-test-autoconfigure 3.x.
 */
@AutoConfiguration
@ConditionalOnClass({SecurityMockMvcRequestPostProcessors.class, MockMvcBuilderCustomizer.class})
public class MockMvcSecurityTestAutoConfiguration {

    @Bean
    MockMvcBuilderCustomizer securityMockMvcBuilderCustomizer() {
        return new SecurityMockMvcBuilderCustomizer();
    }

    static class SecurityMockMvcBuilderCustomizer implements MockMvcBuilderCustomizer {

        @Override
        public void customize(ConfigurableMockMvcBuilder<?> builder) {
            builder.apply(new MockMvcConfigurerAdapter() {
                @Override
                public RequestPostProcessor beforeMockMvcCreated(ConfigurableMockMvcBuilder<?> builder, WebApplicationContext context) {
                    return SecurityMockMvcRequestPostProcessors.testSecurityContext();
                }
            });
        }
    }
}
