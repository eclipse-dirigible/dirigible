/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.messaging.executor;

import org.eclipse.dirigible.engine.api.IEngineExecutor;

public class ActiveMQEngineExecutor implements IEngineExecutor {
	
	public static final String ENGINE_TYPE = "activemq";

	public static final String ENGINE_NAME = "ActiveMQ Messaging Engine";
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.engine.api.script.IEngineExecutor#getType()
	 */
	@Override
	public String getType() {
		return ENGINE_TYPE;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.engine.api.script.IEngineExecutor#getName()
	 */
	@Override
	public String getName() {
		return ENGINE_NAME;
	}

}
