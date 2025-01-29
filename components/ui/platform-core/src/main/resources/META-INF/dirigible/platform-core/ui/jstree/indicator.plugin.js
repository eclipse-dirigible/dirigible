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
(function (factory) {
    "use strict";
    if (typeof define === 'function' && define.amd) {
        define('jstree.indicator', ['jquery', './jstree.js'], factory);
    }
    else if (typeof exports === 'object') {
        factory(require('jquery'), require('./jstree.js'));
    }
    else {
        factory(jQuery, jQuery.jstree);
    }
}(function ($, _jstree, undefined) {
    "use strict";

    if ($.jstree.plugins.indicator) { return; }

    let indicatorClasses = {
        'CD': 'dg-jstree--changed',
        'S': 'dg-jstree--submodule',
        'A': 'dg-jstree--added',
        'M': 'dg-jstree--modified',
        'D': 'dg-jstree--deleted',
        'U': 'dg-jstree--untracked',
        'C': 'dg-jstree--conflict',
        'R': 'dg-jstree--renamed',
    };

    $.jstree.defaults.indicator = {
        sort: true,
        customSort: function (firstNodeId, secondNodeId) {
            let firstNode = this.get_node(firstNodeId);
            let secondNode = this.get_node(secondNodeId);
            if (firstNode.type === "spinner") return -1;
            else if (secondNode.type === "spinner") return 1;
            else if (firstNode.type === secondNode.type) {
                let res = firstNode.text.localeCompare(secondNode.text, "en-GB", { numeric: true, sensitivity: "base" });
                if (res < 0) return -1;
                else if (res > 0) return 1;
                return 0;
            } else if (firstNode.type === "folder") return -1;
            else if (secondNode.type === "folder") return 1;
            else {
                let res = firstNode.text.localeCompare(secondNode.text, "en-GB", { numeric: true, sensitivity: "base" });
                if (res < 0) return -1;
                else if (res > 0) return 1;
                return 0;
            }
        },
        rowIndicator: function (element, node) {
            if (node) {
                const row = element.querySelector(".jstree-wholerow");
                const indicator = document.createElement("span");
                indicator.classList.add("dg-jstree-indicator");
                let indicatorClass = "";
                let indicatorText = "";
                let isDotClass = false;
                if (node.state.containsChanges) {
                    isDotClass = true;
                    indicatorClass = indicatorClasses['CD'];
                } else if (node.state.status) {
                    indicatorText = node.state.status;
                    indicatorClass = indicatorClasses[node.state.status];
                }
                if (indicatorClass) {
                    const link = element.querySelector("a:first-of-type");
                    indicator.classList.add(indicatorClass);
                    if (isDotClass) {
                        const dot = document.createElement("div");
                        dot.classList.add("dg-jstree-dot");
                        indicator.appendChild(dot);
                    } else indicator.innerHTML = indicatorText;
                    link.classList.add(indicatorClass);
                }
                row.appendChild(indicator);
            }
        }
    };
    $.jstree.plugins.indicator = function (_options, parent) {
        this.bind = function () {
            parent.bind.call(this);
            this.element
                .on("model.jstree", function (e, data) {
                    this.sort(data.parent, true);
                }.bind(this))
                .on("rename_node.jstree create_node.jstree", function (e, data) {
                    this.sort(data.parent || data.node.parent, false);
                    this.redraw_node(data.parent || data.node.parent, true);
                }.bind(this))
                .on("move_node.jstree copy_node.jstree", function (e, data) {
                    this.sort(data.parent, false);
                    this.redraw_node(data.parent, true);
                }.bind(this));
            if (this.settings.core.animation) {
                this.element.on("init.jstree", function () {
                    if (typeof this.settings.core.animation !== "string" && this.settings.core.animation)
                        document.documentElement.style.setProperty("--jstree-fiori-animation", `0.${this.settings.core.animation}s`);
                    else document.documentElement.style.setProperty("--jstree-fiori-animation", `0s`);
                }.bind(this));
            }
        };

        this.close_node = function (obj, animation) {
            if (this.settings.core.animation && obj.parentElement) {
                let indicators = obj.parentElement.querySelectorAll("span.dg-jstree-indicator");
                for (let i = 1; i < indicators.length; i++) {
                    indicators[i].style.animation = `fadeOut 0.${this.settings.core.animation}s`;
                }
            }
            return parent.close_node.call(this, obj, animation);
        };

        this.redraw_node = function (obj, _deep, _callback, _force_draw, parentIndicator) {
            const element = parent.redraw_node.apply(this, arguments);
            if (element) this.settings.indicator.rowIndicator(element, typeof obj === 'object' ? obj : this._model.data[obj]);
            if (parentIndicator !== undefined) {
                for (let i = 0; i < obj.parents.length; i++) {
                    if (obj.parents[i] !== '#') {
                        let containsOtherChanges = false;
                        for (let c = 0; c < this._model.data[obj.parents[i]].children_d.length; c++) {
                            if (this._model.data[obj.parents[i]].children_d[c] !== obj.id && this._model.data[this._model.data[obj.parents[i]].children_d[c]].state.status) {
                                containsOtherChanges = true;
                                break;
                            }
                        }
                        if (!containsOtherChanges) {
                            this._model.data[obj.parents[i]].state.containsChanges = parentIndicator;
                            this.redraw_node(obj.parents[i]);
                        }
                    }
                }
            }
            return element;
        };

        this.sort = function (obj, deep = false) {
            let containsChanges = false;
            obj = this.get_node(obj);
            if (obj && obj.children && obj.children.length) {
                if (this.settings.indicator.sort) {
                    obj.children.sort(this.settings.indicator.customSort.bind(this));
                }
                if (deep) {
                    for (let i = 0; i < obj.children_d.length; i++) {
                        this.sort(obj.children_d[i], false);
                    }
                } else if (!obj.state.containsChanges) {
                    for (let i = 0; i < obj.children.length; i++) {
                        let child = this._model.data[obj.children[i]];
                        if (child.state.status) {
                            containsChanges = true;
                            for (let j = 0; j < child.parents.length; j++) {
                                let childParent = this._model.data[child.parents[j]];
                                if (childParent.state.containsChanges) break;
                                childParent.state.containsChanges = true;
                            }
                            break;
                        }
                    }
                }
                if (!deep && containsChanges) {
                    for (let i = 0; i < obj.parents.length; i++) {
                        if (obj.parents[i] !== "#") this.redraw_node(obj.parents[i], false);
                    }
                }
            }
        };
    };
}));