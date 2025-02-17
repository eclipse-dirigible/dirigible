/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
function initClipboard(graph) {
	// Public helper method for shared clipboard.
	mxClipboard.cellsToString = function (cells) {
		let codec = new mxCodec();
		let model = new mxGraphModel();
		let parent = model.getChildAt(model.getRoot(), 0);

		for (let i = 0; i < cells.length; i++) {
			model.add(parent, cells[i]);
		}

		return mxUtils.getXml(codec.encode(model));
	};

	// Focused but invisible textarea during control or meta key events
	let textInput = document.createElement('textarea');
	mxUtils.setOpacity(textInput, 0);
	textInput.style.width = '1px';
	textInput.style.height = '1px';
	let restoreFocus = false;
	let gs = graph.gridSize;
	let lastPaste = null;
	let dx = 0;
	let dy = 0;

	// Workaround for no copy event in IE/FF if empty
	textInput.value = ' ';

	// Shows a textarea when control/cmd is pressed to handle native clipboard actions
	mxEvent.addListener(document, 'keydown', function (evt) {
		// No dialog visible
		let source = mxEvent.getSource(evt);

		if (graph.isEnabled() && !graph.isMouseDown && !graph.isEditing() && source.nodeName != 'INPUT') {
			if (evt.keyCode == 224 /* FF */ || (!mxClient.IS_MAC && evt.keyCode == 17 /* Control */) || (mxClient.IS_MAC && evt.keyCode == 91 /* Meta */)) {
				// Cannot use parentNode for check in IE
				if (!restoreFocus) {
					// Avoid autoscroll but allow handling of events
					textInput.style.position = 'absolute';
					textInput.style.left = (graph.container.scrollLeft + 10) + 'px';
					textInput.style.top = (graph.container.scrollTop + 10) + 'px';
					graph.container.appendChild(textInput);

					restoreFocus = true;
					textInput.focus();
					textInput.select();
				}
			}
		}
	});

	// Restores focus on graph container and removes text input from DOM
	mxEvent.addListener(document, 'keyup', function (evt) {
		if (restoreFocus && (evt.keyCode == 224 /* FF */ || evt.keyCode == 17 /* Control */ ||
			evt.keyCode == 91 /* Meta */)) {
			restoreFocus = false;

			if (!graph.isEditing()) {
				graph.container.focus();
			}

			textInput.parentNode.removeChild(textInput);
		}
	});

	// Inserts the XML for the given cells into the text input for copy
	let copyCells = function (graph, cells) {
		if (cells.length > 0) {
			let clones = graph.cloneCells(cells);

			// Checks for orphaned relative children and makes absolute
			for (let i = 0; i < clones.length; i++) {
				let state = graph.view.getState(cells[i]);

				if (state !== null) {
					let geo = graph.getCellGeometry(clones[i]);

					if (geo !== null && geo.relative) {
						geo.relative = false;
						geo.x = state.x / state.view.scale - state.view.translate.x;
						geo.y = state.y / state.view.scale - state.view.translate.y;
					}
				}
			}

			textInput.value = mxClipboard.cellsToString(clones);
		}

		textInput.select();
		lastPaste = textInput.value;
	};

	// Handles copy event by putting XML for current selection into text input
	mxEvent.addListener(textInput, 'copy', mxUtils.bind(this, function (evt) {
		if (graph.isEnabled() && !graph.isSelectionEmpty()) {
			copyCells(graph, mxUtils.sortCells(graph.model.getTopmostCells(graph.getSelectionCells())));
			dx = 0;
			dy = 0;
		}
	}));

	// Handles cut event by removing cells putting XML into text input
	mxEvent.addListener(textInput, 'cut', mxUtils.bind(this, function (evt) {
		if (graph.isEnabled() && !graph.isSelectionEmpty()) {
			copyCells(graph, graph.removeCells());
			dx = -gs;
			dy = -gs;
		}
	}));

	// Merges XML into existing graph and layers
	let importXml = function (xml, dx, dy) {
		dx = (dx != null) ? dx : 0;
		dy = (dy != null) ? dy : 0;
		let cells = []

		try {
			let doc = mxUtils.parseXml(xml);
			let node = doc.documentElement;

			if (node !== null) {
				let model = new mxGraphModel();
				let codec = new mxCodec(node.ownerDocument);
				codec.decode(node, model);

				let childCount = model.getChildCount(model.getRoot());
				let targetChildCount = graph.model.getChildCount(graph.model.getRoot());

				// Merges existing layers and adds new layers
				graph.model.beginUpdate();
				try {
					for (let i = 0; i < childCount; i++) {
						let parent = model.getChildAt(model.getRoot(), i);

						// Adds cells to existing layers if not locked
						if (targetChildCount > i) {
							// Inserts into active layer if only one layer is being pasted
							let target = (childCount == 1) ? graph.getDefaultParent() : graph.model.getChildAt(graph.model.getRoot(), i);

							if (!graph.isCellLocked(target)) {
								let children = model.getChildren(parent);
								cells = cells.concat(graph.importCells(children, dx, dy, target));
							}
						}
						else {
							// Delta is non cascading, needs separate move for layers
							parent = graph.importCells([parent], 0, 0, graph.model.getRoot())[0];
							let children = graph.model.getChildren(parent);
							graph.moveCells(children, dx, dy);
							cells = cells.concat(children);
						}
					}
				}
				finally {
					graph.model.endUpdate();
				}
			}
		}
		catch (e) {
			alert(e);
			throw e;
		}

		return cells;
	};

	// Parses and inserts XML into graph
	let pasteText = function (text) {
		let xml = mxUtils.trim(text);
		let x = graph.container.scrollLeft / graph.view.scale - graph.view.translate.x;
		let y = graph.container.scrollTop / graph.view.scale - graph.view.translate.y;

		if (xml.length > 0) {
			if (lastPaste != xml) {
				lastPaste = xml;
				dx = 0;
				dy = 0;
			}
			else {
				dx += gs;
				dy += gs;
			}

			// Standard paste via control-v
			if (xml.substring(0, 14) == '<mxGraphModel>') {
				graph.setSelectionCells(importXml(xml, dx, dy));
				graph.scrollCellToVisible(graph.getSelectionCell());
			}
		}
	};

	// Cross-browser function to fetch text from paste events
	let extractGraphModelFromEvent = function (evt) {
		let data = null;

		if (evt !== null) {
			let provider = (evt.dataTransfer != null) ? evt.dataTransfer : evt.clipboardData;

			if (provider != null) {
				if (document.documentMode == 10 || document.documentMode == 11) {
					data = provider.getData('Text');
				}
				else {
					data = (mxUtils.indexOf(provider.types, 'text/html') >= 0) ? provider.getData('text/html') : null;

					if (mxUtils.indexOf(provider.types, 'text/plain' && (data == null || data.length == 0))) {
						data = provider.getData('text/plain');
					}
				}
			}
		}

		return data;
	};

	// Handles paste event by parsing and inserting XML
	mxEvent.addListener(textInput, 'paste', function (evt) {
		// Clears existing contents before paste - should not be needed
		// because all text is selected, but doesn't hurt since the
		// actual pasting of the new text is delayed in all cases.
		textInput.value = '';

		if (graph.isEnabled()) {
			let xml = extractGraphModelFromEvent(evt);

			if (xml !== null && xml.length > 0) {
				pasteText(xml);
			}
			else {
				// Timeout for new value to appear
				window.setTimeout(mxUtils.bind(this, function () {
					pasteText(textInput.value);
				}), 0);
			}
		}

		textInput.select();
	});

	// Enables rubberband selection
	new mxRubberband(graph);

	graph.setCloneInvalidEdges(true);
}