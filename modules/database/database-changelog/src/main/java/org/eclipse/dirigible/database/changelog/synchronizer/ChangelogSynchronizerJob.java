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
package org.eclipse.dirigible.database.changelog.synchronizer;

import org.eclipse.dirigible.core.scheduler.api.AbstractSynchronizerJob;
import org.eclipse.dirigible.core.scheduler.api.ISynchronizer;

/**
 * The Changelog Synchronizer Job.
 */
public class ChangelogSynchronizerJob extends AbstractSynchronizerJob {

	private ChangelogSynchronizer changelogSynchronizer = new ChangelogSynchronizer();

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.core.scheduler.api.AbstractSynchronizerJob#getSynchronizer()
	 */
	@Override
	public ISynchronizer getSynchronizer() {
		return changelogSynchronizer;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.core.scheduler.api.AbstractSynchronizerJob#getName()
	 */
	@Override
	public String getName() {
		return ChangelogSynchronizerJobDefinitionProvider.CHANGELOG_SYNCHRONIZER_JOB;
	}

}
