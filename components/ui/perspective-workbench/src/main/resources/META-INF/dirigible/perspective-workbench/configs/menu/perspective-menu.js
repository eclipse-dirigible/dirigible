/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
exports.getMenu = () => ({
	perspectiveId: 'workbench',
	include: {
		window: true,
		help: true,
	},
	items: [
		{
			label: 'File',
			translation: {
				key: 'perspective-workbench:file',
			},
			items: [
				{
					label: 'New',
					translation: {
						key: 'common:new',
					},
					order: 1,
					items: [
						{
							label: 'Project',
							translation: {
								key: 'perspective-workbench:project',
							},
							order: 1,
							action: 'event',
							data: {
								topic: 'projects.create.project'
							},
						},
					],
				},
				{
					label: 'Save All',
					translation: {
						key: 'perspective-workbench:saveAll',
					},
					order: 2,
					action: 'event',
					data: {
						topic: 'projects.files.save.all'
					},
					separator: true,
				},
				{
					label: 'Publish All',
					translation: {
						key: 'perspective-workbench:publishAll',
					},
					order: 3,
					action: 'event',
					data: {
						topic: 'projects.publish.all'
					},
				},
				{
					label: 'Unpublish All',
					translation: {
						key: 'perspective-workbench:unpublishAll',
					},
					order: 4,
					action: 'event',
					data: {
						topic: 'projects.unpublish.all'
					},
					separator: true,
				},
				{
					label: 'Export All',
					translation: {
						key: 'perspective-workbench:exportAll',
					},
					order: 5,
					action: 'event',
					data: {
						topic: 'projects.export.all'
					},
				},
			]
		},
	]
});