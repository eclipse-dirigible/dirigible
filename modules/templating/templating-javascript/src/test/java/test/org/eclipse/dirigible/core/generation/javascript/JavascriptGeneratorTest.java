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
package test.org.eclipse.dirigible.core.generation.javascript;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.dirigible.commons.api.context.ContextException;
import org.eclipse.dirigible.commons.api.context.ThreadContextFacade;
import org.eclipse.dirigible.commons.config.StaticObjects;
import org.eclipse.dirigible.core.generation.api.IGenerationEngine;
import org.eclipse.dirigible.core.generation.javascript.JavascriptGenerationEngine;
import org.eclipse.dirigible.core.test.AbstractDirigibleTest;
import org.eclipse.dirigible.repository.api.IRepository;
import org.junit.Before;
import org.junit.Test;

public class JavascriptGeneratorTest extends AbstractDirigibleTest {
	
	private IRepository repository;

	@Before
	public void setUp() throws Exception {
		this.repository = (IRepository) StaticObjects.get(StaticObjects.REPOSITORY);
	}
	
	@Test
	public void generate() throws IOException, ContextException {
		try {
			ThreadContextFacade.setUp();
			IGenerationEngine generationEngine = new JavascriptGenerationEngine();
			Map<String, Object> parameters = new HashMap<String, Object>();
			parameters.put("handler", "my-handler.js");
			parameters.put("testParameter", "testValue");
			byte[] result = generationEngine.generate(parameters, "/location", "test $testParameter".getBytes(), null, null);
			assertEquals("test testValue", new String(result));
		} finally {
			ThreadContextFacade.tearDown();
		}
	}

}
