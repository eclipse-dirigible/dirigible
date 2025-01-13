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
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var System = Java.type("java.lang.System");
var ClassLoader = Java.type("java.lang.ClassLoader");
var FileInputStream = Java.type("java.io.FileInputStream");
var File = Java.type("java.io.File");
var URLClassLoader = Java.type("java.net.URLClassLoader");
var BufferedReader = Java.type("java.io.BufferedReader");
var InputStreamReader = Java.type("java.io.InputStreamReader");
(function (global) {
    var LineSeparator = System.getProperty("line.separator");
    var moduleCache = {};
    var classLoaderCache = {};
    var options;
    var ModuleContainer = (function () {
        function ModuleContainer(location) {
            this.location = location;
            this.exports = new ModuleExports();
        }
        ;
        return ModuleContainer;
    }());
    /**
     * ExposedModule represents the module API as seen by the required module code. This means that it only contain the
     * minimum required properties to fulfill the needs. For example, it doesn't expose a `location` property like
     * `ModuleContainer` does.
     */
    var ExposedModule = (function () {
        function ExposedModule(container) {
            // Note: The properties are created using Object.defineProperty because they need access to the container
            // object, but we don't want the container object to be a part of the API exposed to the required module.
            // A module should be requirable via 'id', which means we cannot use the original identifier, which may be
            // relative. By using the name of the location, which is an absolute path in the file system case, we get a stable
            // identifier. The only downside is that it's technically not a "top-level id", which the spec talks about.
            Object.defineProperty(this, "id", {
                get: function () { return container.location.name; }
            });
            // The exports property is has a setter so that a module can assign to module.exports, e.g. to let the exported
            // API be a function.
            Object.defineProperty(this, "exports", {
                get: function () { return container.exports; },
                set: function (value) { return container.exports = value; }
            });
        }
        return ExposedModule;
    }());
    var ModuleExports = (function () {
        function ModuleExports() {
        }
        return ModuleExports;
    }());
    var ModuleId = (function () {
        function ModuleId(id) {
            if (!id)
                throw new RequireError("Module ID cannot be empty");
            this.id = id;
        }
        ModuleId.prototype.isRelative = function () {
            return this.id[0] === "."; // ./ or ../
        };
        ModuleId.prototype.isAbsolutePath = function () {
            // Handle both Unix and Windows path, /foo/bar.js and c:\foo\bar.js
            return this.id[0] === "/" || this.id[1] === ":";
        };
        ModuleId.prototype.toString = function () {
            return this.id;
        };
        return ModuleId;
    }());
    var RequireError = (function (_super) {
        __extends(RequireError, _super);
        function RequireError(message, cause) {
            _super.call(this, message);
            this.message = message;
            this.cause = cause;
        }
        RequireError.prototype.toString = function () {
            var str = this.message;
            if (this.cause)
                str = str + " [Caused by: " + this.cause + "]";
            return str;
        };
        return RequireError;
    }(Error));
    var FileSystemBasedModuleLocation = (function () {
        function FileSystemBasedModuleLocation(file) {
            this.file = file;
            this.name = file.toString();
        }
        FileSystemBasedModuleLocation.prototype.getStream = function () {
            return new FileInputStream(this.file);
        };
        FileSystemBasedModuleLocation.prototype.resolve = function (id) {
            var parent = this.file.isDirectory() ? this.file : this.file.getParentFile();
            return new FileSystemBasedModuleLocation(newFile(parent, id.id));
        };
        FileSystemBasedModuleLocation.prototype.exists = function () {
            return this.file.exists();
        };
        FileSystemBasedModuleLocation.prototype.toString = function () {
            return "file " + this.name;
        };
        return FileSystemBasedModuleLocation;
    }());
    function isFile(x) {
        return File.class.isInstance(x);
    }
    function isClassLoader(x) {
        return ClassLoader.class.isInstance(x);
    }
    function getOrCreateClassLoader(file) {
        var url = file.toURI().toURL();
        var id = url.toString();
        var cachedLoader = classLoaderCache[id];
        if (cachedLoader)
            return cachedLoader;
        return classLoaderCache[id] = new URLClassLoader([url]);
    }
    function reject(message) {
        throw new Error(message);
    }
    var ResourceBasedModuleLocation = (function () {
        function ResourceBasedModuleLocation(jarFileOrClassLoader, maybeResourcePath, basePath) {
            if (isFile(jarFileOrClassLoader)) {
                if (maybeResourcePath || basePath)
                    throw new Error("Multiple arguments passed to ResourceBasedModuleLocation(java.io.File)");
                this.resourcePath = null;
                this.classLoader = getOrCreateClassLoader(jarFileOrClassLoader);
                this.basePath = jarFileOrClassLoader.toString() + "!";
                this.name = this.basePath;
            }
            else if (isClassLoader(jarFileOrClassLoader)) {
                this.classLoader = jarFileOrClassLoader;
                this.resourcePath = maybeResourcePath;
                this.basePath = basePath || "!";
                this.name = basePath + maybeResourcePath;
            }
            else
                throw new Error("Unknown ResourceBasedModuleLocation argument: " + jarFileOrClassLoader);
        }
        ResourceBasedModuleLocation.prototype.getStream = function () {
            if (!this.resourcePath)
                return null;
            print("getting resource as stream: " + this.resourcePath);
            return this.classLoader.getResourceAsStream(this.resourcePath);
        };
        ResourceBasedModuleLocation.prototype.resolve = function (id) {
            if (this.resourcePath) {
                // Treat the current resource path as a file, so get its "directory parent". Note that embedded resources use
                // forward slash as directory separator at all times, so some manual handling here.
                var directoryPart = new File(this.resourcePath).getParent().replace(/\\/g, "/");
                // We know that the module ID is relative (otherwise we would be a top-level location and have no resource path),
                // so we can safely strip off the leading dot of the ID.
                var newResourcePath = directoryPart + id.id.substr(1);
                return new ResourceBasedModuleLocation(this.classLoader, newResourcePath, this.basePath);
            }
            return new ResourceBasedModuleLocation(this.classLoader, id.id, this.basePath);
        };
        ResourceBasedModuleLocation.prototype.exists = function () {
            if (!this.resourcePath)
                return false;
            var stream;
            try {
                stream = this.getStream();
                return !!stream;
            }
            finally {
                if (stream)
                    stream.close();
            }
        };
        ResourceBasedModuleLocation.prototype.toString = function () {
            return "resource " + this.name;
        };
        return ResourceBasedModuleLocation;
    }());
    function getModuleLocationForPath(path) {
        var dotJarBang;
        var lowerPath = path.toLowerCase();
        if (lowerPath.lastIndexOf(".jar") === path.length - 4) {
            return new ResourceBasedModuleLocation(newFile(path));
        }
        else if ((dotJarBang = lowerPath.indexOf(".jar!")) >= 0) {
            var jarPath = path.substr(0, dotJarBang + 4); // exclude the bang
            var resourcePath = path.substr(dotJarBang + 5); // after the bang
            return getModuleLocationForPath(jarPath).resolve(new ModuleId(resourcePath));
        }
        return new FileSystemBasedModuleLocation(newFile(path));
    }
    function locateModule(id, parent) {
        var actions = [];
        if (id.isAbsolutePath()) {
            // For an absolute path, return the location for that path
            actions.push(function (mid) { return getModuleLocationForPath(mid.id); });
        }
        else if (id.isRelative() && parent) {
            // Resolve the id against the location of the parent module
            actions.push(function (mid) { return parent.location.resolve(mid); });
        }
        else {
            // Top-level ID, resolve against the possible roots.
            unique(options.fixedPaths.concat(options.paths)).forEach(function (root) {
                var rootLocation = getModuleLocationForPath(root);
                actions.push(function (mid) { return rootLocation.resolve(mid); });
            });
            // If we have a global classloader, try that one as well. This is done *after* all the fixed and user-configured
            // paths, so that it's possible to override the loading process.
            if (options.classLoader) {
                var clLocation_1 = new ResourceBasedModuleLocation(options.classLoader);
                actions.push(function (mid) { return clLocation_1.resolve(mid); });
            }
        }
        for (var i = 0; i < options.extensions.length; i++) {
            var ext = options.extensions[i];
            var newModuleId = new ModuleId(ensureExtension(id.id, ext));
            for (var j = 0; j < actions.length; j++) {
                var location = actions[j](newModuleId);
                if (!location)
                    continue;
                debugLog("Considering location (" + location + ") for module " + id);
                if (location.exists())
                    return location;
            }
        }
        throw new RequireError("Failed to locate module: " + id);
    }
    function doRequire(id, parent) {
        var moduleId = new ModuleId(id);
        var location = locateModule(moduleId, parent);
        return loadModule(moduleId, location);
    }
    // endsWith - also ES6
    function endsWith(str, suffix) {
        return str.length >= suffix.length && suffix === str.substr(str.length - suffix.length);
    }
    function debugLog(msg) {
        if (options.debug)
            print("[require] " + msg);
    }
    function ensureExtension(path, extension) {
        if (!extension)
            return path;
        if (extension[0] !== ".")
            extension = "." + extension;
        if (endsWith(path, extension))
            return path;
        return path + extension;
    }
    function readLocation(location) {
        try {
            return readFromStream(location.getStream());
        }
        catch (e) {
            throw new RequireError("Failed to read: " + location, e);
        }
    }
    function readFromStream(stream) {
        // more or less regular java code except for static types
        var buf = "", reader;
        try {
            reader = new BufferedReader(new InputStreamReader(stream));
            var line = void 0;
            while ((line = reader.readLine()) !== null) {
                // Make sure to add a line separator (stripped by readLine), so that line numbers are preserved and line
                // comments won't "hide" the remainder of the file.
                buf += line + LineSeparator;
            }
        }
        finally {
            if (reader)
                reader.close();
        }
        return buf;
    }
    function loadModule(id, location) {
        // Check the cache first. Use the location name since that is suppose to be stable regardless of how the module
        // was requested.
        var cachedModule = moduleCache[location.name];
        if (cachedModule) {
            debugLog("Using cached module for " + id);
            return cachedModule.exports;
        }
        debugLog("Loading module '" + id + "' from " + location);
        var body = readLocation(location);
        // TODO: , __filename, __dirname
        var wrappedBody = "var moduleFunction = function (exports, module, require) {" + body + "\n}; moduleFunction";
        var func = load({
            name: location.name,
            script: wrappedBody
        });
        var module = new ModuleContainer(location);
        var requireFn = createRequireFunction(module);
        // Cache before loading so that cyclic dependencies won't be a problem.
        moduleCache[location.name] = module;
        var exposed = new ExposedModule(module);
        func.apply(module, [exposed["exports"], exposed, requireFn]);
        return module.exports;
    }
    /**
     * Initialize nashorn-require. After this function returns, there is a global 'require' function together with
     * global 'module' and 'exports' objects. The main reason for having manual initialization is that it makes it
     * possible to determine which file is the main file/program. Consider Node as a comparison - when you run a JS
     * file with Node, that file is the main file.
     *
     * @param opts options for configuring nashorn-require
     */
    function init(opts) {
        if (!opts.mainFile)
            throw new Error("Missing main file");
        var mainFileAsFile = newFile(opts.mainFile);
        if (!mainFileAsFile.exists())
            throw new Error("Main file doesn't exist: " + opts.mainFile);
        // TODO: join extensions
        // Set the global options
        options = {};
        options.debug = opts.debug || false;
        options.extensions = opts.extensions || [".js", ""]; // TODO: combine
        options.paths = [mainFileAsFile.getParent()]; // TODO: curdir also?
        options.classLoader = opts.classLoader;
        // Also set the fixed paths. These are not exposed to the outside.
        options.fixedPaths = [mainFileAsFile.getParent()]; // TODO: curdir also?
        // Initialize main module
        // TODO: Reuse wrt loadModule!!
        var location = new FileSystemBasedModuleLocation(mainFileAsFile);
        var module = new ModuleContainer(location);
        var requireFn = createRequireFunction(module);
        moduleCache[location.name] = module; // TODO
        var exposed = new ExposedModule(module);
        Object.defineProperty(requireFn, "main", {
            get: function () { return exposed; },
            set: function () { } // noop
        });
        global.module = exposed;
        global.exports = exposed["exports"];
        global.require = requireFn;
    }
    function createRequireFunction(parent) {
        var requireFn = (function (id) { return doRequire(id, parent); });
        // TODO: require.main
        Object.defineProperty(requireFn, "paths", {
            get: function () { return options.paths; },
            set: function () { } // noop
        });
        return requireFn;
    }
    function newFile(parent, child) {
        var file;
        if (child) {
            // java.io.File doesn't necessarily recognize an absolute Windows path as an absolute child. Therefore we have
            // to handle that manually. Luckily, isAbsolute() works as expected!
            if (new File(child).isAbsolute())
                file = File(child);
            else
                file = new File(parent, child);
        }
        else if (parent) {
            file = new File(parent);
        }
        else {
            throw new Error("java.io.File takes one or two arguments");
        }
        // File.exists() works differently under Windows and Unix - on Unix all path parts must exist, even if a part is
        // "negated" by a subsequent ".." part, but Windows is more forgiving. To ensure consistent behavior across
        // system types, we use the canonical path for the final File instance.
        return new File(file.getCanonicalPath());
    }
    function unique(items) {
        var dict = {};
        items.forEach(function (item) {
            dict[item] = true;
        });
        return Object.keys(dict);
    }
    /**
     * Return the init function to the caller, so that the caller will receive the function from calling load. In other
     * words, initialization looks something like this:
     *
     * `
     * var initRequire = load(...path...);
     * initRequire({ ...options... });
     * `
     */
    var create = function (mainFile) {
        var opts = {
            mainFile: mainFile
        };
        init(opts);
    };
    return create;
})(this);
