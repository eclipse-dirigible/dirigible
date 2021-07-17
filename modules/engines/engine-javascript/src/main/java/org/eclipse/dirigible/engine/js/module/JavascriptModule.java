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
package org.eclipse.dirigible.engine.js.module;

import java.util.ServiceLoader;

import org.eclipse.dirigible.commons.api.module.AbstractDirigibleModule;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.engine.js.api.IJavascriptEngineExecutor;

/**
 * The Javascript Module.
 */
public class JavascriptModule extends AbstractDirigibleModule {

	private static final String MODULE_NAME = "Javascript Module";

	/*
	 * (non-Javadoc)
	 * @see com.google.inject.AbstractModule#configure()
	 */
	@Override
	protected void configure() {
		ServiceLoader<IJavascriptEngineExecutor> javascriptEngineExecutors = ServiceLoader.load(IJavascriptEngineExecutor.class);

		Configuration.loadModuleConfig("/dirigible-js.properties");

		String javascriptEngineType = Configuration.get(IJavascriptEngineExecutor.DIRIGIBLE_JAVASCRIPT_ENGINE_TYPE_DEFAULT,
				IJavascriptEngineExecutor.JAVASCRIPT_TYPE_GRAALVM);
		for (IJavascriptEngineExecutor next : javascriptEngineExecutors) {
			if (next.getType().equals(javascriptEngineType)) {
				bind(IJavascriptEngineExecutor.class).toInstance(next);
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.commons.api.module.AbstractDirigibleModule#getName()
	 */
	@Override
	public String getName() {
		return MODULE_NAME;
	}

	@Override
	public int getPriority() {
		return HIGH_PRIORITY;
	}
}
