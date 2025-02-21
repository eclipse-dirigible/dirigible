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

/*
 * THIS IS DEPRECATED!
 * Critical bugfixes only
 */

let ViewRegistry = function () {

	let _views = {};

	let _factories = {};

	this.factory = function (name, func) {
		if (arguments.length == 1)
			return _factories[name];

		if (typeof func !== 'function')
			throw Error('Not a function');
		_factories[name] = func;
		return this;
	}

	this.factories = function () {
		return _factories;
	}

	this.view = function (viewid, factory, region, label, settings) {
		if (viewid === undefined)
			throw Error('Illegal argument for viewid ' + viewid);
		//get view
		if (arguments.length == 1)
			return _views[viewid];
		//set view
		if (factory === undefined)
			throw Error('Illegal argument for factory ' + factory);
		let componentName;
		if (typeof factory === 'string')
			componentName = factory;
		else if (typeof factory === 'function') {
			this.factory(viewid, factory);
			componentName = viewid;
		}
		region = region || 'center-bottom';
		settings = settings || [];
		settings.label = label;

		_views[viewid] = {
			id: viewid,
			type: 'component',
			componentName: componentName,
			componentState: settings,
			defaultRegionId: region
		}
		return this;
	};

	this.views = function () {
		return _views;
	}

	return this;

};

function LayoutController(viewRegistry, messageHub) {

	this.viewRegistry = viewRegistry;

	this.regions = {
		'main': {
			id: 'main',
			type: 'row',
			isClosable: false
		},
		'left': {
			id: 'left',
			type: 'column',
			width: 30,
			defaultRegionId: 'main'
		},
		'left-top': {
			id: 'left-top',
			type: 'stack',
			defaultRegionId: 'left'
		},
		'left-middle': {
			id: 'left-middle',
			type: 'stack',
			defaultRegionId: 'left'
		},
		'left-bottom': {
			id: 'left-bottom',
			type: 'stack',
			defaultRegionId: 'left'
		},
		'center': {
			id: 'center',
			type: 'column',
			defaultRegionId: 'main'
		},
		'center-top': {
			id: 'center-top',
			type: 'stack',
			defaultRegionId: 'center'
		},
		'center-middle': {
			id: 'center-middle',
			type: 'stack',
			defaultRegionId: 'center'
		},
		'center-bottom': {
			id: 'center-bottom',
			type: 'stack',
			defaultRegionId: 'center'
		},
		'right': {
			id: 'right',
			type: 'column',
			defaultRegionId: 'main'
		},
		'right-top': {
			id: 'right-top',
			type: 'stack',
			defaultRegionId: 'right'
		},
		'right-middle': {
			id: 'right-middle',
			type: 'stack',
			defaultRegionId: 'right'
		},
		'right-bottom': {
			id: 'right-bottom',
			type: 'stack',
			defaultRegionId: 'right'
		}
	}

	let parentIds = function (node, regionId, arr) {
		arr = arr || [];
		let _parentId = regionId || node.defaultRegionId;
		if (!_parentId)
			return arr;
		if (this.regions[_parentId]) {
			arr.push(_parentId);
			return parentIds.call(this, this.regions[_parentId], undefined, arr);
		}
		return arr;
	};

	let copy = function (src) {
		return JSON.parse(JSON.stringify(src));
	};

	let gridItem = function (top, id) {
		if (top.id === id)
			return top;
		if (top.content) {
			let item = top.content.find(function (_item) {
				return gridItem(_item, id);
			});
			return item && gridItem(item, id)
		}
	};

	let addView = function (views, view, grid) {
		let node;
		let path = [view.id].concat(parentIds.call(this, view));
		//build branches top-bottom
		path.reverse().forEach(function (compId, idx, arr) {
			let _gridNode = gridItem(grid, compId);
			if (_gridNode) {
				node = _gridNode;
				return;//next
			} else {
				let child = this.regions[compId] || views[compId];
				if (child) {
					child = copy(child);
					if (node && !node.content)
						node.content = [];
					if (node && !node.content.find(function (it) { return it.id == child.id }))
						node.content.push(child);
					node = child;
				}
			}
		}.bind(this));
	}.bind(this);

	this.layoutViews = function (views) {
		let grid = copy(this.regions.main);
		if (views) {
			Object.values(views).map(function (view) {
				if (view) {
					view = copy(view);
					addView(views, view, grid);
				}
				return view;
			}.bind(this));
		}
		return grid;
	}.bind(this);

	this.messageHub = messageHub || new FramesMessageHub();

	this.listeners = {};

	this.addListener = function (eventType, callback) {
		if (!this.listeners[eventType]) {
			this.listeners[eventType] = [];
		}
		let subscriber = this.messageHub.subscribe(callback.bind(this), eventType);

		let entry = {
			id: uuidv4(),
			handler: subscriber
		};
		this.listeners[eventType].push(entry);
		return entry.id;
	};
	this.removeListener = function (id, eventType) {
		if (!this.listeners[eventType])
			return;
		for (let i = this.listeners[eventType].length - 1; i >= 0; i--) {
			if (this.listeners[eventType][i].id == id) {
				let subscriber = this.listeners[eventType][i].handler;
				this.messageHub.unsibscribe(subscriber);
				this.listeners[eventType].splice(i, 1);
			}
		}
	};
	/**
	 * Provide id to enable save/restore state for browser's localStorage. No id will implicitly always reconstruct the instance and will not attempt future state changes save.
	 * Providing an id and the reconstruct flag set to true will not retrive from local storage last state, but will subscribe and save the reconstructed instance future changes.
	 * The layout will be always reconstructed on first init regardless of the reconstruct flag.
	 */
	this.init = function (containerEl, viewNames, id, reconstruct, viewSettings, layoutSettings) {
		this.containerEl = containerEl;
		this.viewNames = viewNames;
		this.viewSettings = viewSettings;
		this.layoutSettings = layoutSettings;
		id = id || $(containerEl).attr("id");

		if (id) {
			if (!reconstruct) {
				//load from localStorage
				let savedState = localStorage.getItem('DIRIGIBLE.IDE.GL.state.' + id);
				if (savedState !== null) {
					this.config = JSON.parse(savedState);
				}
			}
		}
		if (!id || reconstruct || !this.config) {
			//reconstruct (ignore previously saved state)
			let views = {};
			this.viewNames.forEach(function (viewName) {
				if (this.viewSettings) {
					// If this fails with undefined, then there is no view with the name of 'viewName'
					views[viewName] = Object.assign(this.viewRegistry.view(viewName), this.viewSettings[viewName]);
				}
				else views[viewName] = this.viewRegistry.view(viewName)
			}.bind(this));
			// Default settings
			this.config = {
				settings: {
					showPopoutIcon: false,
					showCloseIcon: false
				},
				dimensions: {
					headerHeight: 26,
					borderWidth: 3
				},
				content: [this.layoutViews.call(this, views)]
			};
			// Per layout settings
			if (this.layoutSettings) {
				this.config.settings = Object.assign(this.config.settings, this.layoutSettings);
			}
		}

		this.layout = new GoldenLayout(this.config, containerEl);

		Object.keys(this.viewRegistry.factories()).forEach(function (factoryname) {
			this.layout.registerComponent(factoryname, this.viewRegistry.factory(factoryname));
		}.bind(this));

		if (id) {
			this.layout.on('stateChanged', function () {
				//TODO: debounce or do that only with save button! This fires a lot
				let state = JSON.stringify(this.layout.toConfig());
				localStorage.setItem('DIRIGIBLE.IDE.GL.state.' + id, state);
			}.bind(this));
		}

		this.layout.on('tabCreated', function (tab) {
			if (tab.contentItem.config.title === "Welcome" && tab.contentItem.config.componentState.path === undefined) {
				tab.closeElement[0].style.display = "none";
			}
			tab.closeElement.off('click').click(function () {
				if (tab.contentItem.config.componentName && tab.contentItem.config.componentName === 'editor') {
					if (tab.contentItem.config.title && tab.contentItem.config.title.startsWith('*')) {
						if (confirm('You have unsaved changes, are you sure you want to close ' + tab.contentItem.config.title.substring(1))) {
							tab.contentItem.remove();
						}
					} else {
						tab.contentItem.remove();
					}
				} else {
					if (confirm('Are you sure you want to close ' + tab.contentItem.config.title)) {
						tab.contentItem.remove();
					}
				}
			});
		});

		this.layout.init();
	};

	this.openView = function (viewId/*, region*/) {
		let viewDef = this.viewRegistry.view(viewId);
		if (viewDef) {
			let node;
			let defaultPath = [viewId];
			defaultPath = defaultPath.concat(parentIds.call(this, viewDef/*, region*/));//TODO: consider custom region
			defaultPath.reverse().forEach(function (compId) {
				if (!node)
					node = this.layout.root;
				else
					node = this.layout.root.getItemsById(node.id || node.config.id)[0];
				let child = node.getItemsById(compId)[0] || this.regions[compId] || this.viewRegistry.view(compId);
				if (!node.isRoot && child && this.layout.root.getItemsById(child.id || child.config.id).length < 1) {
					node.addChild(child);
				}
				node = child;
			}.bind(this));
		}
	};

	this.openEditor = function (resourcePath, resourceLabel, contentType, editorId = "editor", extraArgs = null) {
		let newItemConfig = {
			id: resourcePath,
			title: resourceLabel,
			type: 'component',
			componentName: 'editor',
			componentState: {
				path: resourcePath,
				editorId: editorId,
				contentType: contentType,
				extraArgs: extraArgs
			}
		};
		//is an editor available to stack new children to it?
		if (this.layout.root.getItemsById('editor')[0]) {
			//is already open?
			if (this.layout.root.getItemsById(newItemConfig.id).length) {
				//replace content
				let panel = this.layout.root.getItemsById(newItemConfig.id)[0];
				//panel.instance.setContent(newItemConfig.id);
				panel.parent.setActiveContentItem(panel);
			} else {
				// open new tab
				this.layout.root.getItemsById('editor')[0].parent.addChild(newItemConfig);
			}
		} else {
			this.openView('editor');
			this.layout.root.getItemsById('editor')[0].parent.addChild(newItemConfig);
		}
	};

	function closeEditor(editor, layoutManager) {
		let title = editor.config.title;
		if (title.startsWith("*")) {
			if (confirm('You have unsaved changes, are you sure you want to close ' + title.substring(1))) {
				layoutManager.getItemsById('editor')[0].parent.removeChild(editor);
			}
		} else {
			layoutManager.getItemsById('editor')[0].parent.removeChild(editor);
		}
	}

	this.closeEditor = function (editorId) {
		let editor = this.layout.root.getItemsById(editorId)[0];
		closeEditor(editor, this.layout.root);
	};

	this.closeOtherEditors = function (editorId) {
		let editorIds = this.layout.root.getItemsById('editor')[0].parent.config.content
			.filter(e => e.id !== "editor" && e.id !== editorId)
			.map(e => e.id);
		let editors = [];
		editorIds.forEach(e => editors.push(this.layout.root.getItemsById(e)[0]))
		for (let i = 0; i < editors.length; i++) {
			closeEditor(editors[i], this.layout.root);
		}
	};

	this.closeAllEditors = function () {
		let editorIds = this.layout.root.getItemsById('editor')[0].parent.config.content
			.filter(e => e.id !== "editor")
			.map(e => e.id);
		let editors = [];
		editorIds.forEach(e => editors.push(this.layout.root.getItemsById(e)[0]))
		for (let i = 0; i < editors.length; i++) {
			closeEditor(editors[i], this.layout.root);
		}
	};

	this.setEditorDirty = function (resourcePath, dirty) {
		//is an editor available to stack new children to it?
		if (this.layout.root.getItemsById('editor')[0]) {
			//is already open?
			if (this.layout.root.getItemsById(resourcePath).length) {
				let panel = this.layout.root.getItemsById(resourcePath)[0];
				let title = panel.container.tab.titleElement.text();
				if (!title.startsWith('*') && dirty) {
					panel.setTitle(`*${title}`);
				} else if (title.startsWith('*') && !dirty) {
					panel.setTitle(title.substring(1, title.length));
				}
			}
		}
	};

	this.resize = function () {
		this.layout.updateSize();
	};

	this.reset = function () {
		this.layout.destroy();
		this.containerEl.empty();
		this.layout.init(this.containerEl, this.viewNames);
	};
}

function uuidv4() {
	return ([1e7] + -1e3 + -4e3 + -8e3 + -1e11).replace(/[018]/g, c =>
		(c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
	);
}
