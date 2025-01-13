/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.mail;

import java.util.Properties;

/**
 * The Interface IMailConfigurationProvider.
 */
public interface MailConfigurationProvider {

    /**
     * Gets the name of the provider.
     *
     * @return the name
     */
    String getName();

    /**
     * Get Properties for MailClient Instance.
     *
     * @return MailClient instance
     */
    Properties getProperties();
}
