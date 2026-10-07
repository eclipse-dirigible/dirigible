/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.perf;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@TestConfiguration
class PerformanceBaselineITConfig {

    /**
     * Ahead of every other filter, the security chain included, so that a measured request's count
     * covers everything it costs - its authentication too.
     *
     * @return the registration
     */
    @Bean
    FilterRegistrationBean<StatementCountingFilter> statementCountingFilter() {
        FilterRegistrationBean<StatementCountingFilter> registration = new FilterRegistrationBean<>(new StatementCountingFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
