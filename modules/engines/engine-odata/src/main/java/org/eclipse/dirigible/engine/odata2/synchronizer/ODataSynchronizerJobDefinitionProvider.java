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
package org.eclipse.dirigible.engine.odata2.synchronizer;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.core.scheduler.api.IJobDefinitionProvider;
import org.eclipse.dirigible.core.scheduler.api.ISchedulerCoreService;
import org.eclipse.dirigible.core.scheduler.service.definition.JobDefinition;

/**
 * The Class ExtensionsSynchronizerJobDefinitionProvider.
 */
public class ODataSynchronizerJobDefinitionProvider implements IJobDefinitionProvider {

	private static final String DIRIGIBLE_JOB_EXPRESSION_ODATA = "DIRIGIBLE_JOB_EXPRESSION_ODATA";
	
	private static final String DIRIGIBLE_INTERNAL_ODATA_SYNCHRONIZER_JOB = "dirigible-internal-odata-synchronizer-job";
	
	static final String ODATA_SYNCHRONIZER_JOB = "OData Synchronizer Job";

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.core.scheduler.api.IJobDefinitionProvider#getJobDefinition()
	 */
	@Override
	public JobDefinition getJobDefinition() {
		JobDefinition jobDefinition = new JobDefinition();
		jobDefinition.setName(DIRIGIBLE_INTERNAL_ODATA_SYNCHRONIZER_JOB);
		jobDefinition.setGroup(ISchedulerCoreService.JOB_GROUP_INTERNAL);
		jobDefinition.setClazz(ODataSynchronizerJob.class.getCanonicalName());
		jobDefinition.setDescription(ODATA_SYNCHRONIZER_JOB);
		String expression = Configuration.get(DIRIGIBLE_JOB_EXPRESSION_ODATA, "0/45 * * * * ?");
		jobDefinition.setExpression(expression);
		jobDefinition.setSingleton(true);
		return jobDefinition;
	}

}
