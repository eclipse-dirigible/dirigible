/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
/*
 * The single navigation group of the Partner shell: every partner perspective declares
 * groupId 'partner', defined once here (the same one-definition pattern as the application shell's
 * navigation groups).
 */
exports.getPerspectiveGroup = () => ({
	id: 'partner',
	label: 'Partner',
	order: 10,
	// The service classifies a group by the presence of `items` - the aggregated personal
	// perspectives are pushed into it by groupId.
	items: []
});
