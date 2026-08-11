/*
 * Copyright (c) 2026 Eclipse Dirigible contributors
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
 * The single navigation group of the Personal Shell, defined once here (the same one-definition
 * pattern as the application shell's navigation groups). Being the DEFAULT group of the
 * application-personal-perspectives extension point, it adopts every personal perspective the
 * aggregator cannot place: one declaring no groupId (the generator leaves the placement to this
 * shell) as well as one declaring a stale id, so renaming this group can never make an
 * already-generated module's personal pages disappear.
 */
exports.getPerspectiveGroup = () => ({
	id: 'personal',
	label: 'Personal',
	order: 10,
	isDefault: true,
	// The service classifies a group by the presence of `items` - the aggregated personal
	// perspectives are pushed into it.
	items: []
});
