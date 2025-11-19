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
import { registry as contentManager } from "@aerokit/sdk/platform";
let acorn = require("acornjs/acorn");

const COMMENTS_OFFSET_LENGTH = 12;

String.prototype.replaceAll = function (search, replacement) {
    return this.replace(new RegExp(search, 'g'), replacement);
};

export const parse = (moduleName) => {
    let content = contentManager.getText(moduleName + ".js");
    let comments = [];
    let nodes = acorn.parse(content, {
        ecmaVersion: 10,
        onComment: comments,
        ranges: true
    });

    let objects = getObjects(nodes.body);
    let functions = getFunctions(nodes.body);
    let transformedFunctions = {};

    for (let i = 0; i < functions.length; i++) {
        let func = transformFunction(functions[i], comments);
        addTransformedFunction(transformedFunctions, "exports", func);
    }

    for (let i = 0; i < objects.length; i++) {
        getFunctions(objects[i].body.body).forEach(next => {
            let func = transformFunction(next, comments);
            addTransformedFunction(transformedFunctions, objects[i].id.name, func);
        });
    }

    return transformedFunctions;
}

function getObjects(body) {
    return body.filter(e => e.type === "FunctionDeclaration");
}

function getFunctions(body) {
    return body.filter(e => {
        let isFunction = false;
        if (e.type === "ExpressionStatement" && e.expression.type === "AssignmentExpression") {
            if (e.expression.operator === "=" && e.expression.left.object.name === "exports" || e.expression.left.object.type === "ThisExpression") {
                isFunction = true;
            }
        }
        return isFunction;
    });
}

function transformFunction(func, comments) {
    return {
        id: func.expression.left.property.name,
        params: getParams(func),
        documentation: getDocumentation(func, comments),
        returnType: getReturnType(func),
        isFunction: func.expression.right.type === "FunctionExpression"
    };
}

function addTransformedFunction(transformedFunctions, objectName, func) {
    if (!transformedFunctions[objectName]) {
        transformedFunctions[objectName] = {};
    }
    let functionId = func.id;
    if (func.isFunction) {
        func.definition = `${functionId}(${func.params.join(", ")})`;
    } else {
        func.definition = functionId;
    }
    delete func.id;

    transformedFunctions[objectName][functionId] = func;
    let documentation;
    if (func.documentation && !isEmptyObject(func.documentation)) {
        documentation = func.documentation.value;
    } else {
        documentation = transformedFunctions[objectName][functionId].definition;
    }
    transformedFunctions[objectName][functionId].documentation = formatDocumentation(documentation);
}

function getParams(func) {
    let params = func.expression.right.params;
    return params && params.length > 0 ? params.map(e => e.name) : [];
}

function getDocumentation(func, comments) {
    let selectedComments = comments.filter(e => {
        let matches = false;
        if (e.type === "Block") {
            matches = e.start > 0 // skip the first "Header/License" comment
                && e.end < func.expression.start
                && e.end + COMMENTS_OFFSET_LENGTH >= func.expression.start;
        }
        return matches;
    });
    return selectedComments && selectedComments.length > 0 ? selectedComments[selectedComments.length - 1] : {}
}

function getReturnType(func) {
    if (func.expression && func.expression.right && func.expression.right.body && func.expression.right.body.body) {
        let returnType = "void";
        let returnStatement = func.expression.right.body.body.filter(e => e.type === "ReturnStatement")[0];
        if (returnStatement && returnStatement.argument) {
            switch (returnStatement.argument.type) {
                case "NewExpression":
                    returnType = returnStatement.argument.callee.name;
                    break;
                case "Identifier":
                    let identifierName = returnStatement.argument.entity;
                    let returnObject = func.expression.right.body.body.filter(e => e.type === "VariableDeclaration" && e.declarations[0].name === identifierName)[0];
                    if (returnObject.declarations[0].init && returnObject.declarations[0].init.type === "NewExpression") {
                        returnType = returnObject.declarations[0].init.callee.name
                    }
                    break;
            }
        }
        return returnType;
    }
}

function formatDocumentation(documentation) {
    return documentation ? documentation.replaceAll("\\*", "") : documentation;
}

function isEmptyObject(obj) {
    return obj && Object.keys(obj).length === 0 && obj.constructor === Object
}
