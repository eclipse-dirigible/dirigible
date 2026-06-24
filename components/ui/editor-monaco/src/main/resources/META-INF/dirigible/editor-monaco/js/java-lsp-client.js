"use strict";
var JavaLspClientLib = (() => {
  var __create = Object.create;
  var __defProp = Object.defineProperty;
  var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
  var __getOwnPropNames = Object.getOwnPropertyNames;
  var __getProtoOf = Object.getPrototypeOf;
  var __hasOwnProp = Object.prototype.hasOwnProperty;
  var __defNormalProp = (obj, key, value) => key in obj ? __defProp(obj, key, { enumerable: true, configurable: true, writable: true, value }) : obj[key] = value;
  var __commonJS = (cb, mod) => function __require() {
    return mod || (0, cb[__getOwnPropNames(cb)[0]])((mod = { exports: {} }).exports, mod), mod.exports;
  };
  var __export = (target, all) => {
    for (var name in all)
      __defProp(target, name, { get: all[name], enumerable: true });
  };
  var __copyProps = (to, from, except, desc) => {
    if (from && typeof from === "object" || typeof from === "function") {
      for (let key of __getOwnPropNames(from))
        if (!__hasOwnProp.call(to, key) && key !== except)
          __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
    }
    return to;
  };
  var __toESM = (mod, isNodeMode, target) => (target = mod != null ? __create(__getProtoOf(mod)) : {}, __copyProps(
    // If the importer is in node compatibility mode or this is not an ESM
    // file that has been converted to a CommonJS file using a Babel-
    // compatible transform (i.e. "__esModule" has not been set), then set
    // "default" to the CommonJS "module.exports" for node compatibility.
    isNodeMode || !mod || !mod.__esModule ? __defProp(target, "default", { value: mod, enumerable: true }) : target,
    mod
  ));
  var __toCommonJS = (mod) => __copyProps(__defProp({}, "__esModule", { value: true }), mod);
  var __publicField = (obj, key, value) => __defNormalProp(obj, typeof key !== "symbol" ? key + "" : key, value);

  // node_modules/vscode-jsonrpc/lib/common/is.js
  var require_is = __commonJS({
    "node_modules/vscode-jsonrpc/lib/common/is.js"(exports) {
      "use strict";
      Object.defineProperty(exports, "__esModule", { value: true });
      exports.stringArray = exports.array = exports.func = exports.error = exports.number = exports.string = exports.boolean = void 0;
      function boolean(value) {
        return value === true || value === false;
      }
      exports.boolean = boolean;
      function string(value) {
        return typeof value === "string" || value instanceof String;
      }
      exports.string = string;
      function number(value) {
        return typeof value === "number" || value instanceof Number;
      }
      exports.number = number;
      function error(value) {
        return value instanceof Error;
      }
      exports.error = error;
      function func(value) {
        return typeof value === "function";
      }
      exports.func = func;
      function array(value) {
        return Array.isArray(value);
      }
      exports.array = array;
      function stringArray(value) {
        return array(value) && value.every((elem) => string(elem));
      }
      exports.stringArray = stringArray;
    }
  });

  // node_modules/vscode-jsonrpc/lib/common/messages.js
  var require_messages = __commonJS({
    "node_modules/vscode-jsonrpc/lib/common/messages.js"(exports) {
      "use strict";
      Object.defineProperty(exports, "__esModule", { value: true });
      exports.Message = exports.NotificationType9 = exports.NotificationType8 = exports.NotificationType7 = exports.NotificationType6 = exports.NotificationType5 = exports.NotificationType4 = exports.NotificationType3 = exports.NotificationType2 = exports.NotificationType1 = exports.NotificationType0 = exports.NotificationType = exports.RequestType9 = exports.RequestType8 = exports.RequestType7 = exports.RequestType6 = exports.RequestType5 = exports.RequestType4 = exports.RequestType3 = exports.RequestType2 = exports.RequestType1 = exports.RequestType = exports.RequestType0 = exports.AbstractMessageSignature = exports.ParameterStructures = exports.ResponseError = exports.ErrorCodes = void 0;
      var is = require_is();
      var ErrorCodes;
      (function(ErrorCodes2) {
        ErrorCodes2.ParseError = -32700;
        ErrorCodes2.InvalidRequest = -32600;
        ErrorCodes2.MethodNotFound = -32601;
        ErrorCodes2.InvalidParams = -32602;
        ErrorCodes2.InternalError = -32603;
        ErrorCodes2.jsonrpcReservedErrorRangeStart = -32099;
        ErrorCodes2.serverErrorStart = -32099;
        ErrorCodes2.MessageWriteError = -32099;
        ErrorCodes2.MessageReadError = -32098;
        ErrorCodes2.PendingResponseRejected = -32097;
        ErrorCodes2.ConnectionInactive = -32096;
        ErrorCodes2.ServerNotInitialized = -32002;
        ErrorCodes2.UnknownErrorCode = -32001;
        ErrorCodes2.jsonrpcReservedErrorRangeEnd = -32e3;
        ErrorCodes2.serverErrorEnd = -32e3;
      })(ErrorCodes || (exports.ErrorCodes = ErrorCodes = {}));
      var ResponseError = class _ResponseError extends Error {
        constructor(code, message, data) {
          super(message);
          this.code = is.number(code) ? code : ErrorCodes.UnknownErrorCode;
          this.data = data;
          Object.setPrototypeOf(this, _ResponseError.prototype);
        }
        toJson() {
          const result = {
            code: this.code,
            message: this.message
          };
          if (this.data !== void 0) {
            result.data = this.data;
          }
          return result;
        }
      };
      exports.ResponseError = ResponseError;
      var ParameterStructures = class _ParameterStructures {
        constructor(kind) {
          this.kind = kind;
        }
        static is(value) {
          return value === _ParameterStructures.auto || value === _ParameterStructures.byName || value === _ParameterStructures.byPosition;
        }
        toString() {
          return this.kind;
        }
      };
      exports.ParameterStructures = ParameterStructures;
      ParameterStructures.auto = new ParameterStructures("auto");
      ParameterStructures.byPosition = new ParameterStructures("byPosition");
      ParameterStructures.byName = new ParameterStructures("byName");
      var AbstractMessageSignature = class {
        constructor(method, numberOfParams) {
          this.method = method;
          this.numberOfParams = numberOfParams;
        }
        get parameterStructures() {
          return ParameterStructures.auto;
        }
      };
      exports.AbstractMessageSignature = AbstractMessageSignature;
      var RequestType0 = class extends AbstractMessageSignature {
        constructor(method) {
          super(method, 0);
        }
      };
      exports.RequestType0 = RequestType0;
      var RequestType = class extends AbstractMessageSignature {
        constructor(method, _parameterStructures = ParameterStructures.auto) {
          super(method, 1);
          this._parameterStructures = _parameterStructures;
        }
        get parameterStructures() {
          return this._parameterStructures;
        }
      };
      exports.RequestType = RequestType;
      var RequestType1 = class extends AbstractMessageSignature {
        constructor(method, _parameterStructures = ParameterStructures.auto) {
          super(method, 1);
          this._parameterStructures = _parameterStructures;
        }
        get parameterStructures() {
          return this._parameterStructures;
        }
      };
      exports.RequestType1 = RequestType1;
      var RequestType2 = class extends AbstractMessageSignature {
        constructor(method) {
          super(method, 2);
        }
      };
      exports.RequestType2 = RequestType2;
      var RequestType3 = class extends AbstractMessageSignature {
        constructor(method) {
          super(method, 3);
        }
      };
      exports.RequestType3 = RequestType3;
      var RequestType4 = class extends AbstractMessageSignature {
        constructor(method) {
          super(method, 4);
        }
      };
      exports.RequestType4 = RequestType4;
      var RequestType5 = class extends AbstractMessageSignature {
        constructor(method) {
          super(method, 5);
        }
      };
      exports.RequestType5 = RequestType5;
      var RequestType6 = class extends AbstractMessageSignature {
        constructor(method) {
          super(method, 6);
        }
      };
      exports.RequestType6 = RequestType6;
      var RequestType7 = class extends AbstractMessageSignature {
        constructor(method) {
          super(method, 7);
        }
      };
      exports.RequestType7 = RequestType7;
      var RequestType8 = class extends AbstractMessageSignature {
        constructor(method) {
          super(method, 8);
        }
      };
      exports.RequestType8 = RequestType8;
      var RequestType9 = class extends AbstractMessageSignature {
        constructor(method) {
          super(method, 9);
        }
      };
      exports.RequestType9 = RequestType9;
      var NotificationType = class extends AbstractMessageSignature {
        constructor(method, _parameterStructures = ParameterStructures.auto) {
          super(method, 1);
          this._parameterStructures = _parameterStructures;
        }
        get parameterStructures() {
          return this._parameterStructures;
        }
      };
      exports.NotificationType = NotificationType;
      var NotificationType0 = class extends AbstractMessageSignature {
        constructor(method) {
          super(method, 0);
        }
      };
      exports.NotificationType0 = NotificationType0;
      var NotificationType1 = class extends AbstractMessageSignature {
        constructor(method, _parameterStructures = ParameterStructures.auto) {
          super(method, 1);
          this._parameterStructures = _parameterStructures;
        }
        get parameterStructures() {
          return this._parameterStructures;
        }
      };
      exports.NotificationType1 = NotificationType1;
      var NotificationType2 = class extends AbstractMessageSignature {
        constructor(method) {
          super(method, 2);
        }
      };
      exports.NotificationType2 = NotificationType2;
      var NotificationType3 = class extends AbstractMessageSignature {
        constructor(method) {
          super(method, 3);
        }
      };
      exports.NotificationType3 = NotificationType3;
      var NotificationType4 = class extends AbstractMessageSignature {
        constructor(method) {
          super(method, 4);
        }
      };
      exports.NotificationType4 = NotificationType4;
      var NotificationType5 = class extends AbstractMessageSignature {
        constructor(method) {
          super(method, 5);
        }
      };
      exports.NotificationType5 = NotificationType5;
      var NotificationType6 = class extends AbstractMessageSignature {
        constructor(method) {
          super(method, 6);
        }
      };
      exports.NotificationType6 = NotificationType6;
      var NotificationType7 = class extends AbstractMessageSignature {
        constructor(method) {
          super(method, 7);
        }
      };
      exports.NotificationType7 = NotificationType7;
      var NotificationType8 = class extends AbstractMessageSignature {
        constructor(method) {
          super(method, 8);
        }
      };
      exports.NotificationType8 = NotificationType8;
      var NotificationType9 = class extends AbstractMessageSignature {
        constructor(method) {
          super(method, 9);
        }
      };
      exports.NotificationType9 = NotificationType9;
      var Message2;
      (function(Message3) {
        function isRequest(message) {
          const candidate = message;
          return candidate && is.string(candidate.method) && (is.string(candidate.id) || is.number(candidate.id));
        }
        Message3.isRequest = isRequest;
        function isNotification(message) {
          const candidate = message;
          return candidate && is.string(candidate.method) && message.id === void 0;
        }
        Message3.isNotification = isNotification;
        function isResponse(message) {
          const candidate = message;
          return candidate && (candidate.result !== void 0 || !!candidate.error) && (is.string(candidate.id) || is.number(candidate.id) || candidate.id === null);
        }
        Message3.isResponse = isResponse;
      })(Message2 || (exports.Message = Message2 = {}));
    }
  });

  // node_modules/vscode-jsonrpc/lib/common/linkedMap.js
  var require_linkedMap = __commonJS({
    "node_modules/vscode-jsonrpc/lib/common/linkedMap.js"(exports) {
      "use strict";
      var _a;
      Object.defineProperty(exports, "__esModule", { value: true });
      exports.LRUCache = exports.LinkedMap = exports.Touch = void 0;
      var Touch;
      (function(Touch2) {
        Touch2.None = 0;
        Touch2.First = 1;
        Touch2.AsOld = Touch2.First;
        Touch2.Last = 2;
        Touch2.AsNew = Touch2.Last;
      })(Touch || (exports.Touch = Touch = {}));
      var LinkedMap = class {
        constructor() {
          this[_a] = "LinkedMap";
          this._map = /* @__PURE__ */ new Map();
          this._head = void 0;
          this._tail = void 0;
          this._size = 0;
          this._state = 0;
        }
        clear() {
          this._map.clear();
          this._head = void 0;
          this._tail = void 0;
          this._size = 0;
          this._state++;
        }
        isEmpty() {
          return !this._head && !this._tail;
        }
        get size() {
          return this._size;
        }
        get first() {
          return this._head?.value;
        }
        get last() {
          return this._tail?.value;
        }
        has(key) {
          return this._map.has(key);
        }
        get(key, touch = Touch.None) {
          const item = this._map.get(key);
          if (!item) {
            return void 0;
          }
          if (touch !== Touch.None) {
            this.touch(item, touch);
          }
          return item.value;
        }
        set(key, value, touch = Touch.None) {
          let item = this._map.get(key);
          if (item) {
            item.value = value;
            if (touch !== Touch.None) {
              this.touch(item, touch);
            }
          } else {
            item = { key, value, next: void 0, previous: void 0 };
            switch (touch) {
              case Touch.None:
                this.addItemLast(item);
                break;
              case Touch.First:
                this.addItemFirst(item);
                break;
              case Touch.Last:
                this.addItemLast(item);
                break;
              default:
                this.addItemLast(item);
                break;
            }
            this._map.set(key, item);
            this._size++;
          }
          return this;
        }
        delete(key) {
          return !!this.remove(key);
        }
        remove(key) {
          const item = this._map.get(key);
          if (!item) {
            return void 0;
          }
          this._map.delete(key);
          this.removeItem(item);
          this._size--;
          return item.value;
        }
        shift() {
          if (!this._head && !this._tail) {
            return void 0;
          }
          if (!this._head || !this._tail) {
            throw new Error("Invalid list");
          }
          const item = this._head;
          this._map.delete(item.key);
          this.removeItem(item);
          this._size--;
          return item.value;
        }
        forEach(callbackfn, thisArg) {
          const state = this._state;
          let current = this._head;
          while (current) {
            if (thisArg) {
              callbackfn.bind(thisArg)(current.value, current.key, this);
            } else {
              callbackfn(current.value, current.key, this);
            }
            if (this._state !== state) {
              throw new Error(`LinkedMap got modified during iteration.`);
            }
            current = current.next;
          }
        }
        keys() {
          const state = this._state;
          let current = this._head;
          const iterator = {
            [Symbol.iterator]: () => {
              return iterator;
            },
            next: () => {
              if (this._state !== state) {
                throw new Error(`LinkedMap got modified during iteration.`);
              }
              if (current) {
                const result = { value: current.key, done: false };
                current = current.next;
                return result;
              } else {
                return { value: void 0, done: true };
              }
            }
          };
          return iterator;
        }
        values() {
          const state = this._state;
          let current = this._head;
          const iterator = {
            [Symbol.iterator]: () => {
              return iterator;
            },
            next: () => {
              if (this._state !== state) {
                throw new Error(`LinkedMap got modified during iteration.`);
              }
              if (current) {
                const result = { value: current.value, done: false };
                current = current.next;
                return result;
              } else {
                return { value: void 0, done: true };
              }
            }
          };
          return iterator;
        }
        entries() {
          const state = this._state;
          let current = this._head;
          const iterator = {
            [Symbol.iterator]: () => {
              return iterator;
            },
            next: () => {
              if (this._state !== state) {
                throw new Error(`LinkedMap got modified during iteration.`);
              }
              if (current) {
                const result = { value: [current.key, current.value], done: false };
                current = current.next;
                return result;
              } else {
                return { value: void 0, done: true };
              }
            }
          };
          return iterator;
        }
        [(_a = Symbol.toStringTag, Symbol.iterator)]() {
          return this.entries();
        }
        trimOld(newSize) {
          if (newSize >= this.size) {
            return;
          }
          if (newSize === 0) {
            this.clear();
            return;
          }
          let current = this._head;
          let currentSize = this.size;
          while (current && currentSize > newSize) {
            this._map.delete(current.key);
            current = current.next;
            currentSize--;
          }
          this._head = current;
          this._size = currentSize;
          if (current) {
            current.previous = void 0;
          }
          this._state++;
        }
        addItemFirst(item) {
          if (!this._head && !this._tail) {
            this._tail = item;
          } else if (!this._head) {
            throw new Error("Invalid list");
          } else {
            item.next = this._head;
            this._head.previous = item;
          }
          this._head = item;
          this._state++;
        }
        addItemLast(item) {
          if (!this._head && !this._tail) {
            this._head = item;
          } else if (!this._tail) {
            throw new Error("Invalid list");
          } else {
            item.previous = this._tail;
            this._tail.next = item;
          }
          this._tail = item;
          this._state++;
        }
        removeItem(item) {
          if (item === this._head && item === this._tail) {
            this._head = void 0;
            this._tail = void 0;
          } else if (item === this._head) {
            if (!item.next) {
              throw new Error("Invalid list");
            }
            item.next.previous = void 0;
            this._head = item.next;
          } else if (item === this._tail) {
            if (!item.previous) {
              throw new Error("Invalid list");
            }
            item.previous.next = void 0;
            this._tail = item.previous;
          } else {
            const next = item.next;
            const previous = item.previous;
            if (!next || !previous) {
              throw new Error("Invalid list");
            }
            next.previous = previous;
            previous.next = next;
          }
          item.next = void 0;
          item.previous = void 0;
          this._state++;
        }
        touch(item, touch) {
          if (!this._head || !this._tail) {
            throw new Error("Invalid list");
          }
          if (touch !== Touch.First && touch !== Touch.Last) {
            return;
          }
          if (touch === Touch.First) {
            if (item === this._head) {
              return;
            }
            const next = item.next;
            const previous = item.previous;
            if (item === this._tail) {
              previous.next = void 0;
              this._tail = previous;
            } else {
              next.previous = previous;
              previous.next = next;
            }
            item.previous = void 0;
            item.next = this._head;
            this._head.previous = item;
            this._head = item;
            this._state++;
          } else if (touch === Touch.Last) {
            if (item === this._tail) {
              return;
            }
            const next = item.next;
            const previous = item.previous;
            if (item === this._head) {
              next.previous = void 0;
              this._head = next;
            } else {
              next.previous = previous;
              previous.next = next;
            }
            item.next = void 0;
            item.previous = this._tail;
            this._tail.next = item;
            this._tail = item;
            this._state++;
          }
        }
        toJSON() {
          const data = [];
          this.forEach((value, key) => {
            data.push([key, value]);
          });
          return data;
        }
        fromJSON(data) {
          this.clear();
          for (const [key, value] of data) {
            this.set(key, value);
          }
        }
      };
      exports.LinkedMap = LinkedMap;
      var LRUCache = class extends LinkedMap {
        constructor(limit, ratio = 1) {
          super();
          this._limit = limit;
          this._ratio = Math.min(Math.max(0, ratio), 1);
        }
        get limit() {
          return this._limit;
        }
        set limit(limit) {
          this._limit = limit;
          this.checkTrim();
        }
        get ratio() {
          return this._ratio;
        }
        set ratio(ratio) {
          this._ratio = Math.min(Math.max(0, ratio), 1);
          this.checkTrim();
        }
        get(key, touch = Touch.AsNew) {
          return super.get(key, touch);
        }
        peek(key) {
          return super.get(key, Touch.None);
        }
        set(key, value) {
          super.set(key, value, Touch.Last);
          this.checkTrim();
          return this;
        }
        checkTrim() {
          if (this.size > this._limit) {
            this.trimOld(Math.round(this._limit * this._ratio));
          }
        }
      };
      exports.LRUCache = LRUCache;
    }
  });

  // node_modules/vscode-jsonrpc/lib/common/disposable.js
  var require_disposable = __commonJS({
    "node_modules/vscode-jsonrpc/lib/common/disposable.js"(exports) {
      "use strict";
      Object.defineProperty(exports, "__esModule", { value: true });
      exports.Disposable = void 0;
      var Disposable4;
      (function(Disposable5) {
        function create(func) {
          return {
            dispose: func
          };
        }
        Disposable5.create = create;
      })(Disposable4 || (exports.Disposable = Disposable4 = {}));
    }
  });

  // node_modules/vscode-jsonrpc/lib/common/ral.js
  var require_ral = __commonJS({
    "node_modules/vscode-jsonrpc/lib/common/ral.js"(exports) {
      "use strict";
      Object.defineProperty(exports, "__esModule", { value: true });
      var _ral;
      function RAL() {
        if (_ral === void 0) {
          throw new Error(`No runtime abstraction layer installed`);
        }
        return _ral;
      }
      (function(RAL2) {
        function install(ral) {
          if (ral === void 0) {
            throw new Error(`No runtime abstraction layer provided`);
          }
          _ral = ral;
        }
        RAL2.install = install;
      })(RAL || (RAL = {}));
      exports.default = RAL;
    }
  });

  // node_modules/vscode-jsonrpc/lib/common/events.js
  var require_events = __commonJS({
    "node_modules/vscode-jsonrpc/lib/common/events.js"(exports) {
      "use strict";
      Object.defineProperty(exports, "__esModule", { value: true });
      exports.Emitter = exports.Event = void 0;
      var ral_1 = require_ral();
      var Event;
      (function(Event2) {
        const _disposable = { dispose() {
        } };
        Event2.None = function() {
          return _disposable;
        };
      })(Event || (exports.Event = Event = {}));
      var CallbackList = class {
        add(callback, context = null, bucket) {
          if (!this._callbacks) {
            this._callbacks = [];
            this._contexts = [];
          }
          this._callbacks.push(callback);
          this._contexts.push(context);
          if (Array.isArray(bucket)) {
            bucket.push({ dispose: () => this.remove(callback, context) });
          }
        }
        remove(callback, context = null) {
          if (!this._callbacks) {
            return;
          }
          let foundCallbackWithDifferentContext = false;
          for (let i = 0, len = this._callbacks.length; i < len; i++) {
            if (this._callbacks[i] === callback) {
              if (this._contexts[i] === context) {
                this._callbacks.splice(i, 1);
                this._contexts.splice(i, 1);
                return;
              } else {
                foundCallbackWithDifferentContext = true;
              }
            }
          }
          if (foundCallbackWithDifferentContext) {
            throw new Error("When adding a listener with a context, you should remove it with the same context");
          }
        }
        invoke(...args) {
          if (!this._callbacks) {
            return [];
          }
          const ret = [], callbacks = this._callbacks.slice(0), contexts = this._contexts.slice(0);
          for (let i = 0, len = callbacks.length; i < len; i++) {
            try {
              ret.push(callbacks[i].apply(contexts[i], args));
            } catch (e) {
              (0, ral_1.default)().console.error(e);
            }
          }
          return ret;
        }
        isEmpty() {
          return !this._callbacks || this._callbacks.length === 0;
        }
        dispose() {
          this._callbacks = void 0;
          this._contexts = void 0;
        }
      };
      var Emitter = class _Emitter {
        constructor(_options) {
          this._options = _options;
        }
        /**
         * For the public to allow to subscribe
         * to events from this Emitter
         */
        get event() {
          if (!this._event) {
            this._event = (listener, thisArgs, disposables) => {
              if (!this._callbacks) {
                this._callbacks = new CallbackList();
              }
              if (this._options && this._options.onFirstListenerAdd && this._callbacks.isEmpty()) {
                this._options.onFirstListenerAdd(this);
              }
              this._callbacks.add(listener, thisArgs);
              const result = {
                dispose: () => {
                  if (!this._callbacks) {
                    return;
                  }
                  this._callbacks.remove(listener, thisArgs);
                  result.dispose = _Emitter._noop;
                  if (this._options && this._options.onLastListenerRemove && this._callbacks.isEmpty()) {
                    this._options.onLastListenerRemove(this);
                  }
                }
              };
              if (Array.isArray(disposables)) {
                disposables.push(result);
              }
              return result;
            };
          }
          return this._event;
        }
        /**
         * To be kept private to fire an event to
         * subscribers
         */
        fire(event) {
          if (this._callbacks) {
            this._callbacks.invoke.call(this._callbacks, event);
          }
        }
        dispose() {
          if (this._callbacks) {
            this._callbacks.dispose();
            this._callbacks = void 0;
          }
        }
      };
      exports.Emitter = Emitter;
      Emitter._noop = function() {
      };
    }
  });

  // node_modules/vscode-jsonrpc/lib/common/cancellation.js
  var require_cancellation = __commonJS({
    "node_modules/vscode-jsonrpc/lib/common/cancellation.js"(exports) {
      "use strict";
      Object.defineProperty(exports, "__esModule", { value: true });
      exports.CancellationTokenSource = exports.CancellationToken = void 0;
      var ral_1 = require_ral();
      var Is2 = require_is();
      var events_1 = require_events();
      var CancellationToken;
      (function(CancellationToken2) {
        CancellationToken2.None = Object.freeze({
          isCancellationRequested: false,
          onCancellationRequested: events_1.Event.None
        });
        CancellationToken2.Cancelled = Object.freeze({
          isCancellationRequested: true,
          onCancellationRequested: events_1.Event.None
        });
        function is(value) {
          const candidate = value;
          return candidate && (candidate === CancellationToken2.None || candidate === CancellationToken2.Cancelled || Is2.boolean(candidate.isCancellationRequested) && !!candidate.onCancellationRequested);
        }
        CancellationToken2.is = is;
      })(CancellationToken || (exports.CancellationToken = CancellationToken = {}));
      var shortcutEvent = Object.freeze(function(callback, context) {
        const handle = (0, ral_1.default)().timer.setTimeout(callback.bind(context), 0);
        return { dispose() {
          handle.dispose();
        } };
      });
      var MutableToken = class {
        constructor() {
          this._isCancelled = false;
        }
        cancel() {
          if (!this._isCancelled) {
            this._isCancelled = true;
            if (this._emitter) {
              this._emitter.fire(void 0);
              this.dispose();
            }
          }
        }
        get isCancellationRequested() {
          return this._isCancelled;
        }
        get onCancellationRequested() {
          if (this._isCancelled) {
            return shortcutEvent;
          }
          if (!this._emitter) {
            this._emitter = new events_1.Emitter();
          }
          return this._emitter.event;
        }
        dispose() {
          if (this._emitter) {
            this._emitter.dispose();
            this._emitter = void 0;
          }
        }
      };
      var CancellationTokenSource = class {
        get token() {
          if (!this._token) {
            this._token = new MutableToken();
          }
          return this._token;
        }
        cancel() {
          if (!this._token) {
            this._token = CancellationToken.Cancelled;
          } else {
            this._token.cancel();
          }
        }
        dispose() {
          if (!this._token) {
            this._token = CancellationToken.None;
          } else if (this._token instanceof MutableToken) {
            this._token.dispose();
          }
        }
      };
      exports.CancellationTokenSource = CancellationTokenSource;
    }
  });

  // node_modules/vscode-jsonrpc/lib/common/sharedArrayCancellation.js
  var require_sharedArrayCancellation = __commonJS({
    "node_modules/vscode-jsonrpc/lib/common/sharedArrayCancellation.js"(exports) {
      "use strict";
      Object.defineProperty(exports, "__esModule", { value: true });
      exports.SharedArrayReceiverStrategy = exports.SharedArraySenderStrategy = void 0;
      var cancellation_1 = require_cancellation();
      var CancellationState;
      (function(CancellationState2) {
        CancellationState2.Continue = 0;
        CancellationState2.Cancelled = 1;
      })(CancellationState || (CancellationState = {}));
      var SharedArraySenderStrategy = class {
        constructor() {
          this.buffers = /* @__PURE__ */ new Map();
        }
        enableCancellation(request) {
          if (request.id === null) {
            return;
          }
          const buffer = new SharedArrayBuffer(4);
          const data = new Int32Array(buffer, 0, 1);
          data[0] = CancellationState.Continue;
          this.buffers.set(request.id, buffer);
          request.$cancellationData = buffer;
        }
        async sendCancellation(_conn2, id) {
          const buffer = this.buffers.get(id);
          if (buffer === void 0) {
            return;
          }
          const data = new Int32Array(buffer, 0, 1);
          Atomics.store(data, 0, CancellationState.Cancelled);
        }
        cleanup(id) {
          this.buffers.delete(id);
        }
        dispose() {
          this.buffers.clear();
        }
      };
      exports.SharedArraySenderStrategy = SharedArraySenderStrategy;
      var SharedArrayBufferCancellationToken = class {
        constructor(buffer) {
          this.data = new Int32Array(buffer, 0, 1);
        }
        get isCancellationRequested() {
          return Atomics.load(this.data, 0) === CancellationState.Cancelled;
        }
        get onCancellationRequested() {
          throw new Error(`Cancellation over SharedArrayBuffer doesn't support cancellation events`);
        }
      };
      var SharedArrayBufferCancellationTokenSource = class {
        constructor(buffer) {
          this.token = new SharedArrayBufferCancellationToken(buffer);
        }
        cancel() {
        }
        dispose() {
        }
      };
      var SharedArrayReceiverStrategy = class {
        constructor() {
          this.kind = "request";
        }
        createCancellationTokenSource(request) {
          const buffer = request.$cancellationData;
          if (buffer === void 0) {
            return new cancellation_1.CancellationTokenSource();
          }
          return new SharedArrayBufferCancellationTokenSource(buffer);
        }
      };
      exports.SharedArrayReceiverStrategy = SharedArrayReceiverStrategy;
    }
  });

  // node_modules/vscode-jsonrpc/lib/common/semaphore.js
  var require_semaphore = __commonJS({
    "node_modules/vscode-jsonrpc/lib/common/semaphore.js"(exports) {
      "use strict";
      Object.defineProperty(exports, "__esModule", { value: true });
      exports.Semaphore = void 0;
      var ral_1 = require_ral();
      var Semaphore = class {
        constructor(capacity = 1) {
          if (capacity <= 0) {
            throw new Error("Capacity must be greater than 0");
          }
          this._capacity = capacity;
          this._active = 0;
          this._waiting = [];
        }
        lock(thunk) {
          return new Promise((resolve, reject) => {
            this._waiting.push({ thunk, resolve, reject });
            this.runNext();
          });
        }
        get active() {
          return this._active;
        }
        runNext() {
          if (this._waiting.length === 0 || this._active === this._capacity) {
            return;
          }
          (0, ral_1.default)().timer.setImmediate(() => this.doRunNext());
        }
        doRunNext() {
          if (this._waiting.length === 0 || this._active === this._capacity) {
            return;
          }
          const next = this._waiting.shift();
          this._active++;
          if (this._active > this._capacity) {
            throw new Error(`To many thunks active`);
          }
          try {
            const result = next.thunk();
            if (result instanceof Promise) {
              result.then((value) => {
                this._active--;
                next.resolve(value);
                this.runNext();
              }, (err) => {
                this._active--;
                next.reject(err);
                this.runNext();
              });
            } else {
              this._active--;
              next.resolve(result);
              this.runNext();
            }
          } catch (err) {
            this._active--;
            next.reject(err);
            this.runNext();
          }
        }
      };
      exports.Semaphore = Semaphore;
    }
  });

  // node_modules/vscode-jsonrpc/lib/common/messageReader.js
  var require_messageReader = __commonJS({
    "node_modules/vscode-jsonrpc/lib/common/messageReader.js"(exports) {
      "use strict";
      Object.defineProperty(exports, "__esModule", { value: true });
      exports.ReadableStreamMessageReader = exports.AbstractMessageReader = exports.MessageReader = void 0;
      var ral_1 = require_ral();
      var Is2 = require_is();
      var events_1 = require_events();
      var semaphore_1 = require_semaphore();
      var MessageReader2;
      (function(MessageReader3) {
        function is(value) {
          let candidate = value;
          return candidate && Is2.func(candidate.listen) && Is2.func(candidate.dispose) && Is2.func(candidate.onError) && Is2.func(candidate.onClose) && Is2.func(candidate.onPartialMessage);
        }
        MessageReader3.is = is;
      })(MessageReader2 || (exports.MessageReader = MessageReader2 = {}));
      var AbstractMessageReader2 = class {
        constructor() {
          this.errorEmitter = new events_1.Emitter();
          this.closeEmitter = new events_1.Emitter();
          this.partialMessageEmitter = new events_1.Emitter();
        }
        dispose() {
          this.errorEmitter.dispose();
          this.closeEmitter.dispose();
        }
        get onError() {
          return this.errorEmitter.event;
        }
        fireError(error) {
          this.errorEmitter.fire(this.asError(error));
        }
        get onClose() {
          return this.closeEmitter.event;
        }
        fireClose() {
          this.closeEmitter.fire(void 0);
        }
        get onPartialMessage() {
          return this.partialMessageEmitter.event;
        }
        firePartialMessage(info) {
          this.partialMessageEmitter.fire(info);
        }
        asError(error) {
          if (error instanceof Error) {
            return error;
          } else {
            return new Error(`Reader received error. Reason: ${Is2.string(error.message) ? error.message : "unknown"}`);
          }
        }
      };
      exports.AbstractMessageReader = AbstractMessageReader2;
      var ResolvedMessageReaderOptions;
      (function(ResolvedMessageReaderOptions2) {
        function fromOptions(options) {
          let charset;
          let result;
          let contentDecoder;
          const contentDecoders = /* @__PURE__ */ new Map();
          let contentTypeDecoder;
          const contentTypeDecoders = /* @__PURE__ */ new Map();
          if (options === void 0 || typeof options === "string") {
            charset = options ?? "utf-8";
          } else {
            charset = options.charset ?? "utf-8";
            if (options.contentDecoder !== void 0) {
              contentDecoder = options.contentDecoder;
              contentDecoders.set(contentDecoder.name, contentDecoder);
            }
            if (options.contentDecoders !== void 0) {
              for (const decoder of options.contentDecoders) {
                contentDecoders.set(decoder.name, decoder);
              }
            }
            if (options.contentTypeDecoder !== void 0) {
              contentTypeDecoder = options.contentTypeDecoder;
              contentTypeDecoders.set(contentTypeDecoder.name, contentTypeDecoder);
            }
            if (options.contentTypeDecoders !== void 0) {
              for (const decoder of options.contentTypeDecoders) {
                contentTypeDecoders.set(decoder.name, decoder);
              }
            }
          }
          if (contentTypeDecoder === void 0) {
            contentTypeDecoder = (0, ral_1.default)().applicationJson.decoder;
            contentTypeDecoders.set(contentTypeDecoder.name, contentTypeDecoder);
          }
          return { charset, contentDecoder, contentDecoders, contentTypeDecoder, contentTypeDecoders };
        }
        ResolvedMessageReaderOptions2.fromOptions = fromOptions;
      })(ResolvedMessageReaderOptions || (ResolvedMessageReaderOptions = {}));
      var ReadableStreamMessageReader = class extends AbstractMessageReader2 {
        constructor(readable, options) {
          super();
          this.readable = readable;
          this.options = ResolvedMessageReaderOptions.fromOptions(options);
          this.buffer = (0, ral_1.default)().messageBuffer.create(this.options.charset);
          this._partialMessageTimeout = 1e4;
          this.nextMessageLength = -1;
          this.messageToken = 0;
          this.readSemaphore = new semaphore_1.Semaphore(1);
        }
        set partialMessageTimeout(timeout) {
          this._partialMessageTimeout = timeout;
        }
        get partialMessageTimeout() {
          return this._partialMessageTimeout;
        }
        listen(callback) {
          this.nextMessageLength = -1;
          this.messageToken = 0;
          this.partialMessageTimer = void 0;
          this.callback = callback;
          const result = this.readable.onData((data) => {
            this.onData(data);
          });
          this.readable.onError((error) => this.fireError(error));
          this.readable.onClose(() => this.fireClose());
          return result;
        }
        onData(data) {
          try {
            this.buffer.append(data);
            while (true) {
              if (this.nextMessageLength === -1) {
                const headers = this.buffer.tryReadHeaders(true);
                if (!headers) {
                  return;
                }
                const contentLength = headers.get("content-length");
                if (!contentLength) {
                  this.fireError(new Error(`Header must provide a Content-Length property.
${JSON.stringify(Object.fromEntries(headers))}`));
                  return;
                }
                const length = parseInt(contentLength);
                if (isNaN(length)) {
                  this.fireError(new Error(`Content-Length value must be a number. Got ${contentLength}`));
                  return;
                }
                this.nextMessageLength = length;
              }
              const body = this.buffer.tryReadBody(this.nextMessageLength);
              if (body === void 0) {
                this.setPartialMessageTimer();
                return;
              }
              this.clearPartialMessageTimer();
              this.nextMessageLength = -1;
              this.readSemaphore.lock(async () => {
                const bytes = this.options.contentDecoder !== void 0 ? await this.options.contentDecoder.decode(body) : body;
                const message = await this.options.contentTypeDecoder.decode(bytes, this.options);
                this.callback(message);
              }).catch((error) => {
                this.fireError(error);
              });
            }
          } catch (error) {
            this.fireError(error);
          }
        }
        clearPartialMessageTimer() {
          if (this.partialMessageTimer) {
            this.partialMessageTimer.dispose();
            this.partialMessageTimer = void 0;
          }
        }
        setPartialMessageTimer() {
          this.clearPartialMessageTimer();
          if (this._partialMessageTimeout <= 0) {
            return;
          }
          this.partialMessageTimer = (0, ral_1.default)().timer.setTimeout((token, timeout) => {
            this.partialMessageTimer = void 0;
            if (token === this.messageToken) {
              this.firePartialMessage({ messageToken: token, waitingTime: timeout });
              this.setPartialMessageTimer();
            }
          }, this._partialMessageTimeout, this.messageToken, this._partialMessageTimeout);
        }
      };
      exports.ReadableStreamMessageReader = ReadableStreamMessageReader;
    }
  });

  // node_modules/vscode-jsonrpc/lib/common/messageWriter.js
  var require_messageWriter = __commonJS({
    "node_modules/vscode-jsonrpc/lib/common/messageWriter.js"(exports) {
      "use strict";
      Object.defineProperty(exports, "__esModule", { value: true });
      exports.WriteableStreamMessageWriter = exports.AbstractMessageWriter = exports.MessageWriter = void 0;
      var ral_1 = require_ral();
      var Is2 = require_is();
      var semaphore_1 = require_semaphore();
      var events_1 = require_events();
      var ContentLength = "Content-Length: ";
      var CRLF = "\r\n";
      var MessageWriter2;
      (function(MessageWriter3) {
        function is(value) {
          let candidate = value;
          return candidate && Is2.func(candidate.dispose) && Is2.func(candidate.onClose) && Is2.func(candidate.onError) && Is2.func(candidate.write);
        }
        MessageWriter3.is = is;
      })(MessageWriter2 || (exports.MessageWriter = MessageWriter2 = {}));
      var AbstractMessageWriter2 = class {
        constructor() {
          this.errorEmitter = new events_1.Emitter();
          this.closeEmitter = new events_1.Emitter();
        }
        dispose() {
          this.errorEmitter.dispose();
          this.closeEmitter.dispose();
        }
        get onError() {
          return this.errorEmitter.event;
        }
        fireError(error, message, count) {
          this.errorEmitter.fire([this.asError(error), message, count]);
        }
        get onClose() {
          return this.closeEmitter.event;
        }
        fireClose() {
          this.closeEmitter.fire(void 0);
        }
        asError(error) {
          if (error instanceof Error) {
            return error;
          } else {
            return new Error(`Writer received error. Reason: ${Is2.string(error.message) ? error.message : "unknown"}`);
          }
        }
      };
      exports.AbstractMessageWriter = AbstractMessageWriter2;
      var ResolvedMessageWriterOptions;
      (function(ResolvedMessageWriterOptions2) {
        function fromOptions(options) {
          if (options === void 0 || typeof options === "string") {
            return { charset: options ?? "utf-8", contentTypeEncoder: (0, ral_1.default)().applicationJson.encoder };
          } else {
            return { charset: options.charset ?? "utf-8", contentEncoder: options.contentEncoder, contentTypeEncoder: options.contentTypeEncoder ?? (0, ral_1.default)().applicationJson.encoder };
          }
        }
        ResolvedMessageWriterOptions2.fromOptions = fromOptions;
      })(ResolvedMessageWriterOptions || (ResolvedMessageWriterOptions = {}));
      var WriteableStreamMessageWriter = class extends AbstractMessageWriter2 {
        constructor(writable, options) {
          super();
          this.writable = writable;
          this.options = ResolvedMessageWriterOptions.fromOptions(options);
          this.errorCount = 0;
          this.writeSemaphore = new semaphore_1.Semaphore(1);
          this.writable.onError((error) => this.fireError(error));
          this.writable.onClose(() => this.fireClose());
        }
        async write(msg) {
          return this.writeSemaphore.lock(async () => {
            const payload = this.options.contentTypeEncoder.encode(msg, this.options).then((buffer) => {
              if (this.options.contentEncoder !== void 0) {
                return this.options.contentEncoder.encode(buffer);
              } else {
                return buffer;
              }
            });
            return payload.then((buffer) => {
              const headers = [];
              headers.push(ContentLength, buffer.byteLength.toString(), CRLF);
              headers.push(CRLF);
              return this.doWrite(msg, headers, buffer);
            }, (error) => {
              this.fireError(error);
              throw error;
            });
          });
        }
        async doWrite(msg, headers, data) {
          try {
            await this.writable.write(headers.join(""), "ascii");
            return this.writable.write(data);
          } catch (error) {
            this.handleError(error, msg);
            return Promise.reject(error);
          }
        }
        handleError(error, msg) {
          this.errorCount++;
          this.fireError(error, msg, this.errorCount);
        }
        end() {
          this.writable.end();
        }
      };
      exports.WriteableStreamMessageWriter = WriteableStreamMessageWriter;
    }
  });

  // node_modules/vscode-jsonrpc/lib/common/messageBuffer.js
  var require_messageBuffer = __commonJS({
    "node_modules/vscode-jsonrpc/lib/common/messageBuffer.js"(exports) {
      "use strict";
      Object.defineProperty(exports, "__esModule", { value: true });
      exports.AbstractMessageBuffer = void 0;
      var CR = 13;
      var LF = 10;
      var CRLF = "\r\n";
      var AbstractMessageBuffer = class {
        constructor(encoding = "utf-8") {
          this._encoding = encoding;
          this._chunks = [];
          this._totalLength = 0;
        }
        get encoding() {
          return this._encoding;
        }
        append(chunk) {
          const toAppend = typeof chunk === "string" ? this.fromString(chunk, this._encoding) : chunk;
          this._chunks.push(toAppend);
          this._totalLength += toAppend.byteLength;
        }
        tryReadHeaders(lowerCaseKeys = false) {
          if (this._chunks.length === 0) {
            return void 0;
          }
          let state = 0;
          let chunkIndex = 0;
          let offset = 0;
          let chunkBytesRead = 0;
          row: while (chunkIndex < this._chunks.length) {
            const chunk = this._chunks[chunkIndex];
            offset = 0;
            column: while (offset < chunk.length) {
              const value = chunk[offset];
              switch (value) {
                case CR:
                  switch (state) {
                    case 0:
                      state = 1;
                      break;
                    case 2:
                      state = 3;
                      break;
                    default:
                      state = 0;
                  }
                  break;
                case LF:
                  switch (state) {
                    case 1:
                      state = 2;
                      break;
                    case 3:
                      state = 4;
                      offset++;
                      break row;
                    default:
                      state = 0;
                  }
                  break;
                default:
                  state = 0;
              }
              offset++;
            }
            chunkBytesRead += chunk.byteLength;
            chunkIndex++;
          }
          if (state !== 4) {
            return void 0;
          }
          const buffer = this._read(chunkBytesRead + offset);
          const result = /* @__PURE__ */ new Map();
          const headers = this.toString(buffer, "ascii").split(CRLF);
          if (headers.length < 2) {
            return result;
          }
          for (let i = 0; i < headers.length - 2; i++) {
            const header = headers[i];
            const index = header.indexOf(":");
            if (index === -1) {
              throw new Error(`Message header must separate key and value using ':'
${header}`);
            }
            const key = header.substr(0, index);
            const value = header.substr(index + 1).trim();
            result.set(lowerCaseKeys ? key.toLowerCase() : key, value);
          }
          return result;
        }
        tryReadBody(length) {
          if (this._totalLength < length) {
            return void 0;
          }
          return this._read(length);
        }
        get numberOfBytes() {
          return this._totalLength;
        }
        _read(byteCount) {
          if (byteCount === 0) {
            return this.emptyBuffer();
          }
          if (byteCount > this._totalLength) {
            throw new Error(`Cannot read so many bytes!`);
          }
          if (this._chunks[0].byteLength === byteCount) {
            const chunk = this._chunks[0];
            this._chunks.shift();
            this._totalLength -= byteCount;
            return this.asNative(chunk);
          }
          if (this._chunks[0].byteLength > byteCount) {
            const chunk = this._chunks[0];
            const result2 = this.asNative(chunk, byteCount);
            this._chunks[0] = chunk.slice(byteCount);
            this._totalLength -= byteCount;
            return result2;
          }
          const result = this.allocNative(byteCount);
          let resultOffset = 0;
          let chunkIndex = 0;
          while (byteCount > 0) {
            const chunk = this._chunks[chunkIndex];
            if (chunk.byteLength > byteCount) {
              const chunkPart = chunk.slice(0, byteCount);
              result.set(chunkPart, resultOffset);
              resultOffset += byteCount;
              this._chunks[chunkIndex] = chunk.slice(byteCount);
              this._totalLength -= byteCount;
              byteCount -= byteCount;
            } else {
              result.set(chunk, resultOffset);
              resultOffset += chunk.byteLength;
              this._chunks.shift();
              this._totalLength -= chunk.byteLength;
              byteCount -= chunk.byteLength;
            }
          }
          return result;
        }
      };
      exports.AbstractMessageBuffer = AbstractMessageBuffer;
    }
  });

  // node_modules/vscode-jsonrpc/lib/common/connection.js
  var require_connection = __commonJS({
    "node_modules/vscode-jsonrpc/lib/common/connection.js"(exports) {
      "use strict";
      Object.defineProperty(exports, "__esModule", { value: true });
      exports.createMessageConnection = exports.ConnectionOptions = exports.MessageStrategy = exports.CancellationStrategy = exports.CancellationSenderStrategy = exports.CancellationReceiverStrategy = exports.RequestCancellationReceiverStrategy = exports.IdCancellationReceiverStrategy = exports.ConnectionStrategy = exports.ConnectionError = exports.ConnectionErrors = exports.LogTraceNotification = exports.SetTraceNotification = exports.TraceFormat = exports.TraceValues = exports.Trace = exports.NullLogger = exports.ProgressType = exports.ProgressToken = void 0;
      var ral_1 = require_ral();
      var Is2 = require_is();
      var messages_1 = require_messages();
      var linkedMap_1 = require_linkedMap();
      var events_1 = require_events();
      var cancellation_1 = require_cancellation();
      var CancelNotification;
      (function(CancelNotification2) {
        CancelNotification2.type = new messages_1.NotificationType("$/cancelRequest");
      })(CancelNotification || (CancelNotification = {}));
      var ProgressToken;
      (function(ProgressToken2) {
        function is(value) {
          return typeof value === "string" || typeof value === "number";
        }
        ProgressToken2.is = is;
      })(ProgressToken || (exports.ProgressToken = ProgressToken = {}));
      var ProgressNotification;
      (function(ProgressNotification2) {
        ProgressNotification2.type = new messages_1.NotificationType("$/progress");
      })(ProgressNotification || (ProgressNotification = {}));
      var ProgressType = class {
        constructor() {
        }
      };
      exports.ProgressType = ProgressType;
      var StarRequestHandler;
      (function(StarRequestHandler2) {
        function is(value) {
          return Is2.func(value);
        }
        StarRequestHandler2.is = is;
      })(StarRequestHandler || (StarRequestHandler = {}));
      exports.NullLogger = Object.freeze({
        error: () => {
        },
        warn: () => {
        },
        info: () => {
        },
        log: () => {
        }
      });
      var Trace;
      (function(Trace2) {
        Trace2[Trace2["Off"] = 0] = "Off";
        Trace2[Trace2["Messages"] = 1] = "Messages";
        Trace2[Trace2["Compact"] = 2] = "Compact";
        Trace2[Trace2["Verbose"] = 3] = "Verbose";
      })(Trace || (exports.Trace = Trace = {}));
      var TraceValues;
      (function(TraceValues2) {
        TraceValues2.Off = "off";
        TraceValues2.Messages = "messages";
        TraceValues2.Compact = "compact";
        TraceValues2.Verbose = "verbose";
      })(TraceValues || (exports.TraceValues = TraceValues = {}));
      (function(Trace2) {
        function fromString(value) {
          if (!Is2.string(value)) {
            return Trace2.Off;
          }
          value = value.toLowerCase();
          switch (value) {
            case "off":
              return Trace2.Off;
            case "messages":
              return Trace2.Messages;
            case "compact":
              return Trace2.Compact;
            case "verbose":
              return Trace2.Verbose;
            default:
              return Trace2.Off;
          }
        }
        Trace2.fromString = fromString;
        function toString(value) {
          switch (value) {
            case Trace2.Off:
              return "off";
            case Trace2.Messages:
              return "messages";
            case Trace2.Compact:
              return "compact";
            case Trace2.Verbose:
              return "verbose";
            default:
              return "off";
          }
        }
        Trace2.toString = toString;
      })(Trace || (exports.Trace = Trace = {}));
      var TraceFormat;
      (function(TraceFormat2) {
        TraceFormat2["Text"] = "text";
        TraceFormat2["JSON"] = "json";
      })(TraceFormat || (exports.TraceFormat = TraceFormat = {}));
      (function(TraceFormat2) {
        function fromString(value) {
          if (!Is2.string(value)) {
            return TraceFormat2.Text;
          }
          value = value.toLowerCase();
          if (value === "json") {
            return TraceFormat2.JSON;
          } else {
            return TraceFormat2.Text;
          }
        }
        TraceFormat2.fromString = fromString;
      })(TraceFormat || (exports.TraceFormat = TraceFormat = {}));
      var SetTraceNotification;
      (function(SetTraceNotification2) {
        SetTraceNotification2.type = new messages_1.NotificationType("$/setTrace");
      })(SetTraceNotification || (exports.SetTraceNotification = SetTraceNotification = {}));
      var LogTraceNotification;
      (function(LogTraceNotification2) {
        LogTraceNotification2.type = new messages_1.NotificationType("$/logTrace");
      })(LogTraceNotification || (exports.LogTraceNotification = LogTraceNotification = {}));
      var ConnectionErrors;
      (function(ConnectionErrors2) {
        ConnectionErrors2[ConnectionErrors2["Closed"] = 1] = "Closed";
        ConnectionErrors2[ConnectionErrors2["Disposed"] = 2] = "Disposed";
        ConnectionErrors2[ConnectionErrors2["AlreadyListening"] = 3] = "AlreadyListening";
      })(ConnectionErrors || (exports.ConnectionErrors = ConnectionErrors = {}));
      var ConnectionError = class _ConnectionError extends Error {
        constructor(code, message) {
          super(message);
          this.code = code;
          Object.setPrototypeOf(this, _ConnectionError.prototype);
        }
      };
      exports.ConnectionError = ConnectionError;
      var ConnectionStrategy;
      (function(ConnectionStrategy2) {
        function is(value) {
          const candidate = value;
          return candidate && Is2.func(candidate.cancelUndispatched);
        }
        ConnectionStrategy2.is = is;
      })(ConnectionStrategy || (exports.ConnectionStrategy = ConnectionStrategy = {}));
      var IdCancellationReceiverStrategy;
      (function(IdCancellationReceiverStrategy2) {
        function is(value) {
          const candidate = value;
          return candidate && (candidate.kind === void 0 || candidate.kind === "id") && Is2.func(candidate.createCancellationTokenSource) && (candidate.dispose === void 0 || Is2.func(candidate.dispose));
        }
        IdCancellationReceiverStrategy2.is = is;
      })(IdCancellationReceiverStrategy || (exports.IdCancellationReceiverStrategy = IdCancellationReceiverStrategy = {}));
      var RequestCancellationReceiverStrategy;
      (function(RequestCancellationReceiverStrategy2) {
        function is(value) {
          const candidate = value;
          return candidate && candidate.kind === "request" && Is2.func(candidate.createCancellationTokenSource) && (candidate.dispose === void 0 || Is2.func(candidate.dispose));
        }
        RequestCancellationReceiverStrategy2.is = is;
      })(RequestCancellationReceiverStrategy || (exports.RequestCancellationReceiverStrategy = RequestCancellationReceiverStrategy = {}));
      var CancellationReceiverStrategy;
      (function(CancellationReceiverStrategy2) {
        CancellationReceiverStrategy2.Message = Object.freeze({
          createCancellationTokenSource(_) {
            return new cancellation_1.CancellationTokenSource();
          }
        });
        function is(value) {
          return IdCancellationReceiverStrategy.is(value) || RequestCancellationReceiverStrategy.is(value);
        }
        CancellationReceiverStrategy2.is = is;
      })(CancellationReceiverStrategy || (exports.CancellationReceiverStrategy = CancellationReceiverStrategy = {}));
      var CancellationSenderStrategy;
      (function(CancellationSenderStrategy2) {
        CancellationSenderStrategy2.Message = Object.freeze({
          sendCancellation(conn, id) {
            return conn.sendNotification(CancelNotification.type, { id });
          },
          cleanup(_) {
          }
        });
        function is(value) {
          const candidate = value;
          return candidate && Is2.func(candidate.sendCancellation) && Is2.func(candidate.cleanup);
        }
        CancellationSenderStrategy2.is = is;
      })(CancellationSenderStrategy || (exports.CancellationSenderStrategy = CancellationSenderStrategy = {}));
      var CancellationStrategy;
      (function(CancellationStrategy2) {
        CancellationStrategy2.Message = Object.freeze({
          receiver: CancellationReceiverStrategy.Message,
          sender: CancellationSenderStrategy.Message
        });
        function is(value) {
          const candidate = value;
          return candidate && CancellationReceiverStrategy.is(candidate.receiver) && CancellationSenderStrategy.is(candidate.sender);
        }
        CancellationStrategy2.is = is;
      })(CancellationStrategy || (exports.CancellationStrategy = CancellationStrategy = {}));
      var MessageStrategy;
      (function(MessageStrategy2) {
        function is(value) {
          const candidate = value;
          return candidate && Is2.func(candidate.handleMessage);
        }
        MessageStrategy2.is = is;
      })(MessageStrategy || (exports.MessageStrategy = MessageStrategy = {}));
      var ConnectionOptions;
      (function(ConnectionOptions2) {
        function is(value) {
          const candidate = value;
          return candidate && (CancellationStrategy.is(candidate.cancellationStrategy) || ConnectionStrategy.is(candidate.connectionStrategy) || MessageStrategy.is(candidate.messageStrategy));
        }
        ConnectionOptions2.is = is;
      })(ConnectionOptions || (exports.ConnectionOptions = ConnectionOptions = {}));
      var ConnectionState;
      (function(ConnectionState2) {
        ConnectionState2[ConnectionState2["New"] = 1] = "New";
        ConnectionState2[ConnectionState2["Listening"] = 2] = "Listening";
        ConnectionState2[ConnectionState2["Closed"] = 3] = "Closed";
        ConnectionState2[ConnectionState2["Disposed"] = 4] = "Disposed";
      })(ConnectionState || (ConnectionState = {}));
      function createMessageConnection3(messageReader, messageWriter, _logger, options) {
        const logger = _logger !== void 0 ? _logger : exports.NullLogger;
        let sequenceNumber = 0;
        let notificationSequenceNumber = 0;
        let unknownResponseSequenceNumber = 0;
        const version = "2.0";
        let starRequestHandler = void 0;
        const requestHandlers = /* @__PURE__ */ new Map();
        let starNotificationHandler = void 0;
        const notificationHandlers = /* @__PURE__ */ new Map();
        const progressHandlers = /* @__PURE__ */ new Map();
        let timer;
        let messageQueue = new linkedMap_1.LinkedMap();
        let responsePromises = /* @__PURE__ */ new Map();
        let knownCanceledRequests = /* @__PURE__ */ new Set();
        let requestTokens = /* @__PURE__ */ new Map();
        let trace = Trace.Off;
        let traceFormat = TraceFormat.Text;
        let tracer;
        let state = ConnectionState.New;
        const errorEmitter = new events_1.Emitter();
        const closeEmitter = new events_1.Emitter();
        const unhandledNotificationEmitter = new events_1.Emitter();
        const unhandledProgressEmitter = new events_1.Emitter();
        const disposeEmitter = new events_1.Emitter();
        const cancellationStrategy = options && options.cancellationStrategy ? options.cancellationStrategy : CancellationStrategy.Message;
        function createRequestQueueKey(id) {
          if (id === null) {
            throw new Error(`Can't send requests with id null since the response can't be correlated.`);
          }
          return "req-" + id.toString();
        }
        function createResponseQueueKey(id) {
          if (id === null) {
            return "res-unknown-" + (++unknownResponseSequenceNumber).toString();
          } else {
            return "res-" + id.toString();
          }
        }
        function createNotificationQueueKey() {
          return "not-" + (++notificationSequenceNumber).toString();
        }
        function addMessageToQueue(queue, message) {
          if (messages_1.Message.isRequest(message)) {
            queue.set(createRequestQueueKey(message.id), message);
          } else if (messages_1.Message.isResponse(message)) {
            queue.set(createResponseQueueKey(message.id), message);
          } else {
            queue.set(createNotificationQueueKey(), message);
          }
        }
        function cancelUndispatched(_message) {
          return void 0;
        }
        function isListening() {
          return state === ConnectionState.Listening;
        }
        function isClosed() {
          return state === ConnectionState.Closed;
        }
        function isDisposed() {
          return state === ConnectionState.Disposed;
        }
        function closeHandler() {
          if (state === ConnectionState.New || state === ConnectionState.Listening) {
            state = ConnectionState.Closed;
            closeEmitter.fire(void 0);
          }
        }
        function readErrorHandler(error) {
          errorEmitter.fire([error, void 0, void 0]);
        }
        function writeErrorHandler(data) {
          errorEmitter.fire(data);
        }
        messageReader.onClose(closeHandler);
        messageReader.onError(readErrorHandler);
        messageWriter.onClose(closeHandler);
        messageWriter.onError(writeErrorHandler);
        function triggerMessageQueue() {
          if (timer || messageQueue.size === 0) {
            return;
          }
          timer = (0, ral_1.default)().timer.setImmediate(() => {
            timer = void 0;
            processMessageQueue();
          });
        }
        function handleMessage(message) {
          if (messages_1.Message.isRequest(message)) {
            handleRequest(message);
          } else if (messages_1.Message.isNotification(message)) {
            handleNotification(message);
          } else if (messages_1.Message.isResponse(message)) {
            handleResponse(message);
          } else {
            handleInvalidMessage(message);
          }
        }
        function processMessageQueue() {
          if (messageQueue.size === 0) {
            return;
          }
          const message = messageQueue.shift();
          try {
            const messageStrategy = options?.messageStrategy;
            if (MessageStrategy.is(messageStrategy)) {
              messageStrategy.handleMessage(message, handleMessage);
            } else {
              handleMessage(message);
            }
          } finally {
            triggerMessageQueue();
          }
        }
        const callback = (message) => {
          try {
            if (messages_1.Message.isNotification(message) && message.method === CancelNotification.type.method) {
              const cancelId = message.params.id;
              const key = createRequestQueueKey(cancelId);
              const toCancel = messageQueue.get(key);
              if (messages_1.Message.isRequest(toCancel)) {
                const strategy = options?.connectionStrategy;
                const response = strategy && strategy.cancelUndispatched ? strategy.cancelUndispatched(toCancel, cancelUndispatched) : cancelUndispatched(toCancel);
                if (response && (response.error !== void 0 || response.result !== void 0)) {
                  messageQueue.delete(key);
                  requestTokens.delete(cancelId);
                  response.id = toCancel.id;
                  traceSendingResponse(response, message.method, Date.now());
                  messageWriter.write(response).catch(() => logger.error(`Sending response for canceled message failed.`));
                  return;
                }
              }
              const cancellationToken = requestTokens.get(cancelId);
              if (cancellationToken !== void 0) {
                cancellationToken.cancel();
                traceReceivedNotification(message);
                return;
              } else {
                knownCanceledRequests.add(cancelId);
              }
            }
            addMessageToQueue(messageQueue, message);
          } finally {
            triggerMessageQueue();
          }
        };
        function handleRequest(requestMessage) {
          if (isDisposed()) {
            return;
          }
          function reply(resultOrError, method, startTime2) {
            const message = {
              jsonrpc: version,
              id: requestMessage.id
            };
            if (resultOrError instanceof messages_1.ResponseError) {
              message.error = resultOrError.toJson();
            } else {
              message.result = resultOrError === void 0 ? null : resultOrError;
            }
            traceSendingResponse(message, method, startTime2);
            messageWriter.write(message).catch(() => logger.error(`Sending response failed.`));
          }
          function replyError(error, method, startTime2) {
            const message = {
              jsonrpc: version,
              id: requestMessage.id,
              error: error.toJson()
            };
            traceSendingResponse(message, method, startTime2);
            messageWriter.write(message).catch(() => logger.error(`Sending response failed.`));
          }
          function replySuccess(result, method, startTime2) {
            if (result === void 0) {
              result = null;
            }
            const message = {
              jsonrpc: version,
              id: requestMessage.id,
              result
            };
            traceSendingResponse(message, method, startTime2);
            messageWriter.write(message).catch(() => logger.error(`Sending response failed.`));
          }
          traceReceivedRequest(requestMessage);
          const element = requestHandlers.get(requestMessage.method);
          let type;
          let requestHandler;
          if (element) {
            type = element.type;
            requestHandler = element.handler;
          }
          const startTime = Date.now();
          if (requestHandler || starRequestHandler) {
            const tokenKey = requestMessage.id ?? String(Date.now());
            const cancellationSource = IdCancellationReceiverStrategy.is(cancellationStrategy.receiver) ? cancellationStrategy.receiver.createCancellationTokenSource(tokenKey) : cancellationStrategy.receiver.createCancellationTokenSource(requestMessage);
            if (requestMessage.id !== null && knownCanceledRequests.has(requestMessage.id)) {
              cancellationSource.cancel();
            }
            if (requestMessage.id !== null) {
              requestTokens.set(tokenKey, cancellationSource);
            }
            try {
              let handlerResult;
              if (requestHandler) {
                if (requestMessage.params === void 0) {
                  if (type !== void 0 && type.numberOfParams !== 0) {
                    replyError(new messages_1.ResponseError(messages_1.ErrorCodes.InvalidParams, `Request ${requestMessage.method} defines ${type.numberOfParams} params but received none.`), requestMessage.method, startTime);
                    return;
                  }
                  handlerResult = requestHandler(cancellationSource.token);
                } else if (Array.isArray(requestMessage.params)) {
                  if (type !== void 0 && type.parameterStructures === messages_1.ParameterStructures.byName) {
                    replyError(new messages_1.ResponseError(messages_1.ErrorCodes.InvalidParams, `Request ${requestMessage.method} defines parameters by name but received parameters by position`), requestMessage.method, startTime);
                    return;
                  }
                  handlerResult = requestHandler(...requestMessage.params, cancellationSource.token);
                } else {
                  if (type !== void 0 && type.parameterStructures === messages_1.ParameterStructures.byPosition) {
                    replyError(new messages_1.ResponseError(messages_1.ErrorCodes.InvalidParams, `Request ${requestMessage.method} defines parameters by position but received parameters by name`), requestMessage.method, startTime);
                    return;
                  }
                  handlerResult = requestHandler(requestMessage.params, cancellationSource.token);
                }
              } else if (starRequestHandler) {
                handlerResult = starRequestHandler(requestMessage.method, requestMessage.params, cancellationSource.token);
              }
              const promise = handlerResult;
              if (!handlerResult) {
                requestTokens.delete(tokenKey);
                replySuccess(handlerResult, requestMessage.method, startTime);
              } else if (promise.then) {
                promise.then((resultOrError) => {
                  requestTokens.delete(tokenKey);
                  reply(resultOrError, requestMessage.method, startTime);
                }, (error) => {
                  requestTokens.delete(tokenKey);
                  if (error instanceof messages_1.ResponseError) {
                    replyError(error, requestMessage.method, startTime);
                  } else if (error && Is2.string(error.message)) {
                    replyError(new messages_1.ResponseError(messages_1.ErrorCodes.InternalError, `Request ${requestMessage.method} failed with message: ${error.message}`), requestMessage.method, startTime);
                  } else {
                    replyError(new messages_1.ResponseError(messages_1.ErrorCodes.InternalError, `Request ${requestMessage.method} failed unexpectedly without providing any details.`), requestMessage.method, startTime);
                  }
                });
              } else {
                requestTokens.delete(tokenKey);
                reply(handlerResult, requestMessage.method, startTime);
              }
            } catch (error) {
              requestTokens.delete(tokenKey);
              if (error instanceof messages_1.ResponseError) {
                reply(error, requestMessage.method, startTime);
              } else if (error && Is2.string(error.message)) {
                replyError(new messages_1.ResponseError(messages_1.ErrorCodes.InternalError, `Request ${requestMessage.method} failed with message: ${error.message}`), requestMessage.method, startTime);
              } else {
                replyError(new messages_1.ResponseError(messages_1.ErrorCodes.InternalError, `Request ${requestMessage.method} failed unexpectedly without providing any details.`), requestMessage.method, startTime);
              }
            }
          } else {
            replyError(new messages_1.ResponseError(messages_1.ErrorCodes.MethodNotFound, `Unhandled method ${requestMessage.method}`), requestMessage.method, startTime);
          }
        }
        function handleResponse(responseMessage) {
          if (isDisposed()) {
            return;
          }
          if (responseMessage.id === null) {
            if (responseMessage.error) {
              logger.error(`Received response message without id: Error is: 
${JSON.stringify(responseMessage.error, void 0, 4)}`);
            } else {
              logger.error(`Received response message without id. No further error information provided.`);
            }
          } else {
            const key = responseMessage.id;
            const responsePromise = responsePromises.get(key);
            traceReceivedResponse(responseMessage, responsePromise);
            if (responsePromise !== void 0) {
              responsePromises.delete(key);
              try {
                if (responseMessage.error) {
                  const error = responseMessage.error;
                  responsePromise.reject(new messages_1.ResponseError(error.code, error.message, error.data));
                } else if (responseMessage.result !== void 0) {
                  responsePromise.resolve(responseMessage.result);
                } else {
                  throw new Error("Should never happen.");
                }
              } catch (error) {
                if (error.message) {
                  logger.error(`Response handler '${responsePromise.method}' failed with message: ${error.message}`);
                } else {
                  logger.error(`Response handler '${responsePromise.method}' failed unexpectedly.`);
                }
              }
            }
          }
        }
        function handleNotification(message) {
          if (isDisposed()) {
            return;
          }
          let type = void 0;
          let notificationHandler;
          if (message.method === CancelNotification.type.method) {
            const cancelId = message.params.id;
            knownCanceledRequests.delete(cancelId);
            traceReceivedNotification(message);
            return;
          } else {
            const element = notificationHandlers.get(message.method);
            if (element) {
              notificationHandler = element.handler;
              type = element.type;
            }
          }
          if (notificationHandler || starNotificationHandler) {
            try {
              traceReceivedNotification(message);
              if (notificationHandler) {
                if (message.params === void 0) {
                  if (type !== void 0) {
                    if (type.numberOfParams !== 0 && type.parameterStructures !== messages_1.ParameterStructures.byName) {
                      logger.error(`Notification ${message.method} defines ${type.numberOfParams} params but received none.`);
                    }
                  }
                  notificationHandler();
                } else if (Array.isArray(message.params)) {
                  const params = message.params;
                  if (message.method === ProgressNotification.type.method && params.length === 2 && ProgressToken.is(params[0])) {
                    notificationHandler({ token: params[0], value: params[1] });
                  } else {
                    if (type !== void 0) {
                      if (type.parameterStructures === messages_1.ParameterStructures.byName) {
                        logger.error(`Notification ${message.method} defines parameters by name but received parameters by position`);
                      }
                      if (type.numberOfParams !== message.params.length) {
                        logger.error(`Notification ${message.method} defines ${type.numberOfParams} params but received ${params.length} arguments`);
                      }
                    }
                    notificationHandler(...params);
                  }
                } else {
                  if (type !== void 0 && type.parameterStructures === messages_1.ParameterStructures.byPosition) {
                    logger.error(`Notification ${message.method} defines parameters by position but received parameters by name`);
                  }
                  notificationHandler(message.params);
                }
              } else if (starNotificationHandler) {
                starNotificationHandler(message.method, message.params);
              }
            } catch (error) {
              if (error.message) {
                logger.error(`Notification handler '${message.method}' failed with message: ${error.message}`);
              } else {
                logger.error(`Notification handler '${message.method}' failed unexpectedly.`);
              }
            }
          } else {
            unhandledNotificationEmitter.fire(message);
          }
        }
        function handleInvalidMessage(message) {
          if (!message) {
            logger.error("Received empty message.");
            return;
          }
          logger.error(`Received message which is neither a response nor a notification message:
${JSON.stringify(message, null, 4)}`);
          const responseMessage = message;
          if (Is2.string(responseMessage.id) || Is2.number(responseMessage.id)) {
            const key = responseMessage.id;
            const responseHandler = responsePromises.get(key);
            if (responseHandler) {
              responseHandler.reject(new Error("The received response has neither a result nor an error property."));
            }
          }
        }
        function stringifyTrace(params) {
          if (params === void 0 || params === null) {
            return void 0;
          }
          switch (trace) {
            case Trace.Verbose:
              return JSON.stringify(params, null, 4);
            case Trace.Compact:
              return JSON.stringify(params);
            default:
              return void 0;
          }
        }
        function traceSendingRequest(message) {
          if (trace === Trace.Off || !tracer) {
            return;
          }
          if (traceFormat === TraceFormat.Text) {
            let data = void 0;
            if ((trace === Trace.Verbose || trace === Trace.Compact) && message.params) {
              data = `Params: ${stringifyTrace(message.params)}

`;
            }
            tracer.log(`Sending request '${message.method} - (${message.id})'.`, data);
          } else {
            logLSPMessage("send-request", message);
          }
        }
        function traceSendingNotification(message) {
          if (trace === Trace.Off || !tracer) {
            return;
          }
          if (traceFormat === TraceFormat.Text) {
            let data = void 0;
            if (trace === Trace.Verbose || trace === Trace.Compact) {
              if (message.params) {
                data = `Params: ${stringifyTrace(message.params)}

`;
              } else {
                data = "No parameters provided.\n\n";
              }
            }
            tracer.log(`Sending notification '${message.method}'.`, data);
          } else {
            logLSPMessage("send-notification", message);
          }
        }
        function traceSendingResponse(message, method, startTime) {
          if (trace === Trace.Off || !tracer) {
            return;
          }
          if (traceFormat === TraceFormat.Text) {
            let data = void 0;
            if (trace === Trace.Verbose || trace === Trace.Compact) {
              if (message.error && message.error.data) {
                data = `Error data: ${stringifyTrace(message.error.data)}

`;
              } else {
                if (message.result) {
                  data = `Result: ${stringifyTrace(message.result)}

`;
                } else if (message.error === void 0) {
                  data = "No result returned.\n\n";
                }
              }
            }
            tracer.log(`Sending response '${method} - (${message.id})'. Processing request took ${Date.now() - startTime}ms`, data);
          } else {
            logLSPMessage("send-response", message);
          }
        }
        function traceReceivedRequest(message) {
          if (trace === Trace.Off || !tracer) {
            return;
          }
          if (traceFormat === TraceFormat.Text) {
            let data = void 0;
            if ((trace === Trace.Verbose || trace === Trace.Compact) && message.params) {
              data = `Params: ${stringifyTrace(message.params)}

`;
            }
            tracer.log(`Received request '${message.method} - (${message.id})'.`, data);
          } else {
            logLSPMessage("receive-request", message);
          }
        }
        function traceReceivedNotification(message) {
          if (trace === Trace.Off || !tracer || message.method === LogTraceNotification.type.method) {
            return;
          }
          if (traceFormat === TraceFormat.Text) {
            let data = void 0;
            if (trace === Trace.Verbose || trace === Trace.Compact) {
              if (message.params) {
                data = `Params: ${stringifyTrace(message.params)}

`;
              } else {
                data = "No parameters provided.\n\n";
              }
            }
            tracer.log(`Received notification '${message.method}'.`, data);
          } else {
            logLSPMessage("receive-notification", message);
          }
        }
        function traceReceivedResponse(message, responsePromise) {
          if (trace === Trace.Off || !tracer) {
            return;
          }
          if (traceFormat === TraceFormat.Text) {
            let data = void 0;
            if (trace === Trace.Verbose || trace === Trace.Compact) {
              if (message.error && message.error.data) {
                data = `Error data: ${stringifyTrace(message.error.data)}

`;
              } else {
                if (message.result) {
                  data = `Result: ${stringifyTrace(message.result)}

`;
                } else if (message.error === void 0) {
                  data = "No result returned.\n\n";
                }
              }
            }
            if (responsePromise) {
              const error = message.error ? ` Request failed: ${message.error.message} (${message.error.code}).` : "";
              tracer.log(`Received response '${responsePromise.method} - (${message.id})' in ${Date.now() - responsePromise.timerStart}ms.${error}`, data);
            } else {
              tracer.log(`Received response ${message.id} without active response promise.`, data);
            }
          } else {
            logLSPMessage("receive-response", message);
          }
        }
        function logLSPMessage(type, message) {
          if (!tracer || trace === Trace.Off) {
            return;
          }
          const lspMessage = {
            isLSPMessage: true,
            type,
            message,
            timestamp: Date.now()
          };
          tracer.log(lspMessage);
        }
        function throwIfClosedOrDisposed() {
          if (isClosed()) {
            throw new ConnectionError(ConnectionErrors.Closed, "Connection is closed.");
          }
          if (isDisposed()) {
            throw new ConnectionError(ConnectionErrors.Disposed, "Connection is disposed.");
          }
        }
        function throwIfListening() {
          if (isListening()) {
            throw new ConnectionError(ConnectionErrors.AlreadyListening, "Connection is already listening");
          }
        }
        function throwIfNotListening() {
          if (!isListening()) {
            throw new Error("Call listen() first.");
          }
        }
        function undefinedToNull(param) {
          if (param === void 0) {
            return null;
          } else {
            return param;
          }
        }
        function nullToUndefined(param) {
          if (param === null) {
            return void 0;
          } else {
            return param;
          }
        }
        function isNamedParam(param) {
          return param !== void 0 && param !== null && !Array.isArray(param) && typeof param === "object";
        }
        function computeSingleParam(parameterStructures, param) {
          switch (parameterStructures) {
            case messages_1.ParameterStructures.auto:
              if (isNamedParam(param)) {
                return nullToUndefined(param);
              } else {
                return [undefinedToNull(param)];
              }
            case messages_1.ParameterStructures.byName:
              if (!isNamedParam(param)) {
                throw new Error(`Received parameters by name but param is not an object literal.`);
              }
              return nullToUndefined(param);
            case messages_1.ParameterStructures.byPosition:
              return [undefinedToNull(param)];
            default:
              throw new Error(`Unknown parameter structure ${parameterStructures.toString()}`);
          }
        }
        function computeMessageParams(type, params) {
          let result;
          const numberOfParams = type.numberOfParams;
          switch (numberOfParams) {
            case 0:
              result = void 0;
              break;
            case 1:
              result = computeSingleParam(type.parameterStructures, params[0]);
              break;
            default:
              result = [];
              for (let i = 0; i < params.length && i < numberOfParams; i++) {
                result.push(undefinedToNull(params[i]));
              }
              if (params.length < numberOfParams) {
                for (let i = params.length; i < numberOfParams; i++) {
                  result.push(null);
                }
              }
              break;
          }
          return result;
        }
        const connection = {
          sendNotification: (type, ...args) => {
            throwIfClosedOrDisposed();
            let method;
            let messageParams;
            if (Is2.string(type)) {
              method = type;
              const first = args[0];
              let paramStart = 0;
              let parameterStructures = messages_1.ParameterStructures.auto;
              if (messages_1.ParameterStructures.is(first)) {
                paramStart = 1;
                parameterStructures = first;
              }
              let paramEnd = args.length;
              const numberOfParams = paramEnd - paramStart;
              switch (numberOfParams) {
                case 0:
                  messageParams = void 0;
                  break;
                case 1:
                  messageParams = computeSingleParam(parameterStructures, args[paramStart]);
                  break;
                default:
                  if (parameterStructures === messages_1.ParameterStructures.byName) {
                    throw new Error(`Received ${numberOfParams} parameters for 'by Name' notification parameter structure.`);
                  }
                  messageParams = args.slice(paramStart, paramEnd).map((value) => undefinedToNull(value));
                  break;
              }
            } else {
              const params = args;
              method = type.method;
              messageParams = computeMessageParams(type, params);
            }
            const notificationMessage = {
              jsonrpc: version,
              method,
              params: messageParams
            };
            traceSendingNotification(notificationMessage);
            return messageWriter.write(notificationMessage).catch((error) => {
              logger.error(`Sending notification failed.`);
              throw error;
            });
          },
          onNotification: (type, handler) => {
            throwIfClosedOrDisposed();
            let method;
            if (Is2.func(type)) {
              starNotificationHandler = type;
            } else if (handler) {
              if (Is2.string(type)) {
                method = type;
                notificationHandlers.set(type, { type: void 0, handler });
              } else {
                method = type.method;
                notificationHandlers.set(type.method, { type, handler });
              }
            }
            return {
              dispose: () => {
                if (method !== void 0) {
                  notificationHandlers.delete(method);
                } else {
                  starNotificationHandler = void 0;
                }
              }
            };
          },
          onProgress: (_type, token, handler) => {
            if (progressHandlers.has(token)) {
              throw new Error(`Progress handler for token ${token} already registered`);
            }
            progressHandlers.set(token, handler);
            return {
              dispose: () => {
                progressHandlers.delete(token);
              }
            };
          },
          sendProgress: (_type, token, value) => {
            return connection.sendNotification(ProgressNotification.type, { token, value });
          },
          onUnhandledProgress: unhandledProgressEmitter.event,
          sendRequest: (type, ...args) => {
            throwIfClosedOrDisposed();
            throwIfNotListening();
            let method;
            let messageParams;
            let token = void 0;
            if (Is2.string(type)) {
              method = type;
              const first = args[0];
              const last = args[args.length - 1];
              let paramStart = 0;
              let parameterStructures = messages_1.ParameterStructures.auto;
              if (messages_1.ParameterStructures.is(first)) {
                paramStart = 1;
                parameterStructures = first;
              }
              let paramEnd = args.length;
              if (cancellation_1.CancellationToken.is(last)) {
                paramEnd = paramEnd - 1;
                token = last;
              }
              const numberOfParams = paramEnd - paramStart;
              switch (numberOfParams) {
                case 0:
                  messageParams = void 0;
                  break;
                case 1:
                  messageParams = computeSingleParam(parameterStructures, args[paramStart]);
                  break;
                default:
                  if (parameterStructures === messages_1.ParameterStructures.byName) {
                    throw new Error(`Received ${numberOfParams} parameters for 'by Name' request parameter structure.`);
                  }
                  messageParams = args.slice(paramStart, paramEnd).map((value) => undefinedToNull(value));
                  break;
              }
            } else {
              const params = args;
              method = type.method;
              messageParams = computeMessageParams(type, params);
              const numberOfParams = type.numberOfParams;
              token = cancellation_1.CancellationToken.is(params[numberOfParams]) ? params[numberOfParams] : void 0;
            }
            const id = sequenceNumber++;
            let disposable;
            if (token) {
              disposable = token.onCancellationRequested(() => {
                const p = cancellationStrategy.sender.sendCancellation(connection, id);
                if (p === void 0) {
                  logger.log(`Received no promise from cancellation strategy when cancelling id ${id}`);
                  return Promise.resolve();
                } else {
                  return p.catch(() => {
                    logger.log(`Sending cancellation messages for id ${id} failed`);
                  });
                }
              });
            }
            const requestMessage = {
              jsonrpc: version,
              id,
              method,
              params: messageParams
            };
            traceSendingRequest(requestMessage);
            if (typeof cancellationStrategy.sender.enableCancellation === "function") {
              cancellationStrategy.sender.enableCancellation(requestMessage);
            }
            return new Promise(async (resolve, reject) => {
              const resolveWithCleanup = (r) => {
                resolve(r);
                cancellationStrategy.sender.cleanup(id);
                disposable?.dispose();
              };
              const rejectWithCleanup = (r) => {
                reject(r);
                cancellationStrategy.sender.cleanup(id);
                disposable?.dispose();
              };
              const responsePromise = { method, timerStart: Date.now(), resolve: resolveWithCleanup, reject: rejectWithCleanup };
              try {
                responsePromises.set(id, responsePromise);
                await messageWriter.write(requestMessage);
              } catch (error) {
                responsePromises.delete(id);
                responsePromise.reject(new messages_1.ResponseError(messages_1.ErrorCodes.MessageWriteError, error.message ? error.message : "Unknown reason"));
                logger.error(`Sending request failed.`);
                throw error;
              }
            });
          },
          onRequest: (type, handler) => {
            throwIfClosedOrDisposed();
            let method = null;
            if (StarRequestHandler.is(type)) {
              method = void 0;
              starRequestHandler = type;
            } else if (Is2.string(type)) {
              method = null;
              if (handler !== void 0) {
                method = type;
                requestHandlers.set(type, { handler, type: void 0 });
              }
            } else {
              if (handler !== void 0) {
                method = type.method;
                requestHandlers.set(type.method, { type, handler });
              }
            }
            return {
              dispose: () => {
                if (method === null) {
                  return;
                }
                if (method !== void 0) {
                  requestHandlers.delete(method);
                } else {
                  starRequestHandler = void 0;
                }
              }
            };
          },
          hasPendingResponse: () => {
            return responsePromises.size > 0;
          },
          trace: async (_value, _tracer, sendNotificationOrTraceOptions) => {
            let _sendNotification = false;
            let _traceFormat = TraceFormat.Text;
            if (sendNotificationOrTraceOptions !== void 0) {
              if (Is2.boolean(sendNotificationOrTraceOptions)) {
                _sendNotification = sendNotificationOrTraceOptions;
              } else {
                _sendNotification = sendNotificationOrTraceOptions.sendNotification || false;
                _traceFormat = sendNotificationOrTraceOptions.traceFormat || TraceFormat.Text;
              }
            }
            trace = _value;
            traceFormat = _traceFormat;
            if (trace === Trace.Off) {
              tracer = void 0;
            } else {
              tracer = _tracer;
            }
            if (_sendNotification && !isClosed() && !isDisposed()) {
              await connection.sendNotification(SetTraceNotification.type, { value: Trace.toString(_value) });
            }
          },
          onError: errorEmitter.event,
          onClose: closeEmitter.event,
          onUnhandledNotification: unhandledNotificationEmitter.event,
          onDispose: disposeEmitter.event,
          end: () => {
            messageWriter.end();
          },
          dispose: () => {
            if (isDisposed()) {
              return;
            }
            state = ConnectionState.Disposed;
            disposeEmitter.fire(void 0);
            const error = new messages_1.ResponseError(messages_1.ErrorCodes.PendingResponseRejected, "Pending response rejected since connection got disposed");
            for (const promise of responsePromises.values()) {
              promise.reject(error);
            }
            responsePromises = /* @__PURE__ */ new Map();
            requestTokens = /* @__PURE__ */ new Map();
            knownCanceledRequests = /* @__PURE__ */ new Set();
            messageQueue = new linkedMap_1.LinkedMap();
            if (Is2.func(messageWriter.dispose)) {
              messageWriter.dispose();
            }
            if (Is2.func(messageReader.dispose)) {
              messageReader.dispose();
            }
          },
          listen: () => {
            throwIfClosedOrDisposed();
            throwIfListening();
            state = ConnectionState.Listening;
            messageReader.listen(callback);
          },
          inspect: () => {
            (0, ral_1.default)().console.log("inspect");
          }
        };
        connection.onNotification(LogTraceNotification.type, (params) => {
          if (trace === Trace.Off || !tracer) {
            return;
          }
          const verbose = trace === Trace.Verbose || trace === Trace.Compact;
          tracer.log(params.message, verbose ? params.verbose : void 0);
        });
        connection.onNotification(ProgressNotification.type, (params) => {
          const handler = progressHandlers.get(params.token);
          if (handler) {
            handler(params.value);
          } else {
            unhandledProgressEmitter.fire(params);
          }
        });
        return connection;
      }
      exports.createMessageConnection = createMessageConnection3;
    }
  });

  // node_modules/vscode-jsonrpc/lib/common/api.js
  var require_api = __commonJS({
    "node_modules/vscode-jsonrpc/lib/common/api.js"(exports) {
      "use strict";
      Object.defineProperty(exports, "__esModule", { value: true });
      exports.ProgressType = exports.ProgressToken = exports.createMessageConnection = exports.NullLogger = exports.ConnectionOptions = exports.ConnectionStrategy = exports.AbstractMessageBuffer = exports.WriteableStreamMessageWriter = exports.AbstractMessageWriter = exports.MessageWriter = exports.ReadableStreamMessageReader = exports.AbstractMessageReader = exports.MessageReader = exports.SharedArrayReceiverStrategy = exports.SharedArraySenderStrategy = exports.CancellationToken = exports.CancellationTokenSource = exports.Emitter = exports.Event = exports.Disposable = exports.LRUCache = exports.Touch = exports.LinkedMap = exports.ParameterStructures = exports.NotificationType9 = exports.NotificationType8 = exports.NotificationType7 = exports.NotificationType6 = exports.NotificationType5 = exports.NotificationType4 = exports.NotificationType3 = exports.NotificationType2 = exports.NotificationType1 = exports.NotificationType0 = exports.NotificationType = exports.ErrorCodes = exports.ResponseError = exports.RequestType9 = exports.RequestType8 = exports.RequestType7 = exports.RequestType6 = exports.RequestType5 = exports.RequestType4 = exports.RequestType3 = exports.RequestType2 = exports.RequestType1 = exports.RequestType0 = exports.RequestType = exports.Message = exports.RAL = void 0;
      exports.MessageStrategy = exports.CancellationStrategy = exports.CancellationSenderStrategy = exports.CancellationReceiverStrategy = exports.ConnectionError = exports.ConnectionErrors = exports.LogTraceNotification = exports.SetTraceNotification = exports.TraceFormat = exports.TraceValues = exports.Trace = void 0;
      var messages_1 = require_messages();
      Object.defineProperty(exports, "Message", { enumerable: true, get: function() {
        return messages_1.Message;
      } });
      Object.defineProperty(exports, "RequestType", { enumerable: true, get: function() {
        return messages_1.RequestType;
      } });
      Object.defineProperty(exports, "RequestType0", { enumerable: true, get: function() {
        return messages_1.RequestType0;
      } });
      Object.defineProperty(exports, "RequestType1", { enumerable: true, get: function() {
        return messages_1.RequestType1;
      } });
      Object.defineProperty(exports, "RequestType2", { enumerable: true, get: function() {
        return messages_1.RequestType2;
      } });
      Object.defineProperty(exports, "RequestType3", { enumerable: true, get: function() {
        return messages_1.RequestType3;
      } });
      Object.defineProperty(exports, "RequestType4", { enumerable: true, get: function() {
        return messages_1.RequestType4;
      } });
      Object.defineProperty(exports, "RequestType5", { enumerable: true, get: function() {
        return messages_1.RequestType5;
      } });
      Object.defineProperty(exports, "RequestType6", { enumerable: true, get: function() {
        return messages_1.RequestType6;
      } });
      Object.defineProperty(exports, "RequestType7", { enumerable: true, get: function() {
        return messages_1.RequestType7;
      } });
      Object.defineProperty(exports, "RequestType8", { enumerable: true, get: function() {
        return messages_1.RequestType8;
      } });
      Object.defineProperty(exports, "RequestType9", { enumerable: true, get: function() {
        return messages_1.RequestType9;
      } });
      Object.defineProperty(exports, "ResponseError", { enumerable: true, get: function() {
        return messages_1.ResponseError;
      } });
      Object.defineProperty(exports, "ErrorCodes", { enumerable: true, get: function() {
        return messages_1.ErrorCodes;
      } });
      Object.defineProperty(exports, "NotificationType", { enumerable: true, get: function() {
        return messages_1.NotificationType;
      } });
      Object.defineProperty(exports, "NotificationType0", { enumerable: true, get: function() {
        return messages_1.NotificationType0;
      } });
      Object.defineProperty(exports, "NotificationType1", { enumerable: true, get: function() {
        return messages_1.NotificationType1;
      } });
      Object.defineProperty(exports, "NotificationType2", { enumerable: true, get: function() {
        return messages_1.NotificationType2;
      } });
      Object.defineProperty(exports, "NotificationType3", { enumerable: true, get: function() {
        return messages_1.NotificationType3;
      } });
      Object.defineProperty(exports, "NotificationType4", { enumerable: true, get: function() {
        return messages_1.NotificationType4;
      } });
      Object.defineProperty(exports, "NotificationType5", { enumerable: true, get: function() {
        return messages_1.NotificationType5;
      } });
      Object.defineProperty(exports, "NotificationType6", { enumerable: true, get: function() {
        return messages_1.NotificationType6;
      } });
      Object.defineProperty(exports, "NotificationType7", { enumerable: true, get: function() {
        return messages_1.NotificationType7;
      } });
      Object.defineProperty(exports, "NotificationType8", { enumerable: true, get: function() {
        return messages_1.NotificationType8;
      } });
      Object.defineProperty(exports, "NotificationType9", { enumerable: true, get: function() {
        return messages_1.NotificationType9;
      } });
      Object.defineProperty(exports, "ParameterStructures", { enumerable: true, get: function() {
        return messages_1.ParameterStructures;
      } });
      var linkedMap_1 = require_linkedMap();
      Object.defineProperty(exports, "LinkedMap", { enumerable: true, get: function() {
        return linkedMap_1.LinkedMap;
      } });
      Object.defineProperty(exports, "LRUCache", { enumerable: true, get: function() {
        return linkedMap_1.LRUCache;
      } });
      Object.defineProperty(exports, "Touch", { enumerable: true, get: function() {
        return linkedMap_1.Touch;
      } });
      var disposable_1 = require_disposable();
      Object.defineProperty(exports, "Disposable", { enumerable: true, get: function() {
        return disposable_1.Disposable;
      } });
      var events_1 = require_events();
      Object.defineProperty(exports, "Event", { enumerable: true, get: function() {
        return events_1.Event;
      } });
      Object.defineProperty(exports, "Emitter", { enumerable: true, get: function() {
        return events_1.Emitter;
      } });
      var cancellation_1 = require_cancellation();
      Object.defineProperty(exports, "CancellationTokenSource", { enumerable: true, get: function() {
        return cancellation_1.CancellationTokenSource;
      } });
      Object.defineProperty(exports, "CancellationToken", { enumerable: true, get: function() {
        return cancellation_1.CancellationToken;
      } });
      var sharedArrayCancellation_1 = require_sharedArrayCancellation();
      Object.defineProperty(exports, "SharedArraySenderStrategy", { enumerable: true, get: function() {
        return sharedArrayCancellation_1.SharedArraySenderStrategy;
      } });
      Object.defineProperty(exports, "SharedArrayReceiverStrategy", { enumerable: true, get: function() {
        return sharedArrayCancellation_1.SharedArrayReceiverStrategy;
      } });
      var messageReader_1 = require_messageReader();
      Object.defineProperty(exports, "MessageReader", { enumerable: true, get: function() {
        return messageReader_1.MessageReader;
      } });
      Object.defineProperty(exports, "AbstractMessageReader", { enumerable: true, get: function() {
        return messageReader_1.AbstractMessageReader;
      } });
      Object.defineProperty(exports, "ReadableStreamMessageReader", { enumerable: true, get: function() {
        return messageReader_1.ReadableStreamMessageReader;
      } });
      var messageWriter_1 = require_messageWriter();
      Object.defineProperty(exports, "MessageWriter", { enumerable: true, get: function() {
        return messageWriter_1.MessageWriter;
      } });
      Object.defineProperty(exports, "AbstractMessageWriter", { enumerable: true, get: function() {
        return messageWriter_1.AbstractMessageWriter;
      } });
      Object.defineProperty(exports, "WriteableStreamMessageWriter", { enumerable: true, get: function() {
        return messageWriter_1.WriteableStreamMessageWriter;
      } });
      var messageBuffer_1 = require_messageBuffer();
      Object.defineProperty(exports, "AbstractMessageBuffer", { enumerable: true, get: function() {
        return messageBuffer_1.AbstractMessageBuffer;
      } });
      var connection_1 = require_connection();
      Object.defineProperty(exports, "ConnectionStrategy", { enumerable: true, get: function() {
        return connection_1.ConnectionStrategy;
      } });
      Object.defineProperty(exports, "ConnectionOptions", { enumerable: true, get: function() {
        return connection_1.ConnectionOptions;
      } });
      Object.defineProperty(exports, "NullLogger", { enumerable: true, get: function() {
        return connection_1.NullLogger;
      } });
      Object.defineProperty(exports, "createMessageConnection", { enumerable: true, get: function() {
        return connection_1.createMessageConnection;
      } });
      Object.defineProperty(exports, "ProgressToken", { enumerable: true, get: function() {
        return connection_1.ProgressToken;
      } });
      Object.defineProperty(exports, "ProgressType", { enumerable: true, get: function() {
        return connection_1.ProgressType;
      } });
      Object.defineProperty(exports, "Trace", { enumerable: true, get: function() {
        return connection_1.Trace;
      } });
      Object.defineProperty(exports, "TraceValues", { enumerable: true, get: function() {
        return connection_1.TraceValues;
      } });
      Object.defineProperty(exports, "TraceFormat", { enumerable: true, get: function() {
        return connection_1.TraceFormat;
      } });
      Object.defineProperty(exports, "SetTraceNotification", { enumerable: true, get: function() {
        return connection_1.SetTraceNotification;
      } });
      Object.defineProperty(exports, "LogTraceNotification", { enumerable: true, get: function() {
        return connection_1.LogTraceNotification;
      } });
      Object.defineProperty(exports, "ConnectionErrors", { enumerable: true, get: function() {
        return connection_1.ConnectionErrors;
      } });
      Object.defineProperty(exports, "ConnectionError", { enumerable: true, get: function() {
        return connection_1.ConnectionError;
      } });
      Object.defineProperty(exports, "CancellationReceiverStrategy", { enumerable: true, get: function() {
        return connection_1.CancellationReceiverStrategy;
      } });
      Object.defineProperty(exports, "CancellationSenderStrategy", { enumerable: true, get: function() {
        return connection_1.CancellationSenderStrategy;
      } });
      Object.defineProperty(exports, "CancellationStrategy", { enumerable: true, get: function() {
        return connection_1.CancellationStrategy;
      } });
      Object.defineProperty(exports, "MessageStrategy", { enumerable: true, get: function() {
        return connection_1.MessageStrategy;
      } });
      var ral_1 = require_ral();
      exports.RAL = ral_1.default;
    }
  });

  // node_modules/vscode-jsonrpc/lib/browser/ril.js
  var require_ril = __commonJS({
    "node_modules/vscode-jsonrpc/lib/browser/ril.js"(exports) {
      "use strict";
      Object.defineProperty(exports, "__esModule", { value: true });
      var api_1 = require_api();
      var MessageBuffer = class _MessageBuffer extends api_1.AbstractMessageBuffer {
        constructor(encoding = "utf-8") {
          super(encoding);
          this.asciiDecoder = new TextDecoder("ascii");
        }
        emptyBuffer() {
          return _MessageBuffer.emptyBuffer;
        }
        fromString(value, _encoding) {
          return new TextEncoder().encode(value);
        }
        toString(value, encoding) {
          if (encoding === "ascii") {
            return this.asciiDecoder.decode(value);
          } else {
            return new TextDecoder(encoding).decode(value);
          }
        }
        asNative(buffer, length) {
          if (length === void 0) {
            return buffer;
          } else {
            return buffer.slice(0, length);
          }
        }
        allocNative(length) {
          return new Uint8Array(length);
        }
      };
      MessageBuffer.emptyBuffer = new Uint8Array(0);
      var ReadableStreamWrapper = class {
        constructor(socket) {
          this.socket = socket;
          this._onData = new api_1.Emitter();
          this._messageListener = (event) => {
            const blob = event.data;
            blob.arrayBuffer().then((buffer) => {
              this._onData.fire(new Uint8Array(buffer));
            }, () => {
              (0, api_1.RAL)().console.error(`Converting blob to array buffer failed.`);
            });
          };
          this.socket.addEventListener("message", this._messageListener);
        }
        onClose(listener) {
          this.socket.addEventListener("close", listener);
          return api_1.Disposable.create(() => this.socket.removeEventListener("close", listener));
        }
        onError(listener) {
          this.socket.addEventListener("error", listener);
          return api_1.Disposable.create(() => this.socket.removeEventListener("error", listener));
        }
        onEnd(listener) {
          this.socket.addEventListener("end", listener);
          return api_1.Disposable.create(() => this.socket.removeEventListener("end", listener));
        }
        onData(listener) {
          return this._onData.event(listener);
        }
      };
      var WritableStreamWrapper = class {
        constructor(socket) {
          this.socket = socket;
        }
        onClose(listener) {
          this.socket.addEventListener("close", listener);
          return api_1.Disposable.create(() => this.socket.removeEventListener("close", listener));
        }
        onError(listener) {
          this.socket.addEventListener("error", listener);
          return api_1.Disposable.create(() => this.socket.removeEventListener("error", listener));
        }
        onEnd(listener) {
          this.socket.addEventListener("end", listener);
          return api_1.Disposable.create(() => this.socket.removeEventListener("end", listener));
        }
        write(data, encoding) {
          if (typeof data === "string") {
            if (encoding !== void 0 && encoding !== "utf-8") {
              throw new Error(`In a Browser environments only utf-8 text encoding is supported. But got encoding: ${encoding}`);
            }
            this.socket.send(data);
          } else {
            this.socket.send(data);
          }
          return Promise.resolve();
        }
        end() {
          this.socket.close();
        }
      };
      var _textEncoder = new TextEncoder();
      var _ril = Object.freeze({
        messageBuffer: Object.freeze({
          create: (encoding) => new MessageBuffer(encoding)
        }),
        applicationJson: Object.freeze({
          encoder: Object.freeze({
            name: "application/json",
            encode: (msg, options) => {
              if (options.charset !== "utf-8") {
                throw new Error(`In a Browser environments only utf-8 text encoding is supported. But got encoding: ${options.charset}`);
              }
              return Promise.resolve(_textEncoder.encode(JSON.stringify(msg, void 0, 0)));
            }
          }),
          decoder: Object.freeze({
            name: "application/json",
            decode: (buffer, options) => {
              if (!(buffer instanceof Uint8Array)) {
                throw new Error(`In a Browser environments only Uint8Arrays are supported.`);
              }
              return Promise.resolve(JSON.parse(new TextDecoder(options.charset).decode(buffer)));
            }
          })
        }),
        stream: Object.freeze({
          asReadableStream: (socket) => new ReadableStreamWrapper(socket),
          asWritableStream: (socket) => new WritableStreamWrapper(socket)
        }),
        console,
        timer: Object.freeze({
          setTimeout(callback, ms, ...args) {
            const handle = setTimeout(callback, ms, ...args);
            return { dispose: () => clearTimeout(handle) };
          },
          setImmediate(callback, ...args) {
            const handle = setTimeout(callback, 0, ...args);
            return { dispose: () => clearTimeout(handle) };
          },
          setInterval(callback, ms, ...args) {
            const handle = setInterval(callback, ms, ...args);
            return { dispose: () => clearInterval(handle) };
          }
        })
      });
      function RIL() {
        return _ril;
      }
      (function(RIL2) {
        function install() {
          api_1.RAL.install(_ril);
        }
        RIL2.install = install;
      })(RIL || (RIL = {}));
      exports.default = RIL;
    }
  });

  // node_modules/vscode-jsonrpc/lib/browser/main.js
  var require_main = __commonJS({
    "node_modules/vscode-jsonrpc/lib/browser/main.js"(exports) {
      "use strict";
      var __createBinding = exports && exports.__createBinding || (Object.create ? function(o, m2, k, k2) {
        if (k2 === void 0) k2 = k;
        var desc = Object.getOwnPropertyDescriptor(m2, k);
        if (!desc || ("get" in desc ? !m2.__esModule : desc.writable || desc.configurable)) {
          desc = { enumerable: true, get: function() {
            return m2[k];
          } };
        }
        Object.defineProperty(o, k2, desc);
      } : function(o, m2, k, k2) {
        if (k2 === void 0) k2 = k;
        o[k2] = m2[k];
      });
      var __exportStar = exports && exports.__exportStar || function(m2, exports2) {
        for (var p in m2) if (p !== "default" && !Object.prototype.hasOwnProperty.call(exports2, p)) __createBinding(exports2, m2, p);
      };
      Object.defineProperty(exports, "__esModule", { value: true });
      exports.createMessageConnection = exports.BrowserMessageWriter = exports.BrowserMessageReader = void 0;
      var ril_1 = require_ril();
      ril_1.default.install();
      var api_1 = require_api();
      __exportStar(require_api(), exports);
      var BrowserMessageReader = class extends api_1.AbstractMessageReader {
        constructor(port) {
          super();
          this._onData = new api_1.Emitter();
          this._messageListener = (event) => {
            this._onData.fire(event.data);
          };
          port.addEventListener("error", (event) => this.fireError(event));
          port.onmessage = this._messageListener;
        }
        listen(callback) {
          return this._onData.event(callback);
        }
      };
      exports.BrowserMessageReader = BrowserMessageReader;
      var BrowserMessageWriter = class extends api_1.AbstractMessageWriter {
        constructor(port) {
          super();
          this.port = port;
          this.errorCount = 0;
          port.addEventListener("error", (event) => this.fireError(event));
        }
        write(msg) {
          try {
            this.port.postMessage(msg);
            return Promise.resolve();
          } catch (error) {
            this.handleError(error, msg);
            return Promise.reject(error);
          }
        }
        handleError(error, msg) {
          this.errorCount++;
          this.fireError(error, msg, this.errorCount);
        }
        end() {
        }
      };
      exports.BrowserMessageWriter = BrowserMessageWriter;
      function createMessageConnection3(reader, writer, logger, options) {
        if (logger === void 0) {
          logger = api_1.NullLogger;
        }
        if (api_1.ConnectionStrategy.is(options)) {
          options = { connectionStrategy: options };
        }
        return (0, api_1.createMessageConnection)(reader, writer, logger, options);
      }
      exports.createMessageConnection = createMessageConnection3;
    }
  });

  // node_modules/vscode-jsonrpc/browser.js
  var require_browser = __commonJS({
    "node_modules/vscode-jsonrpc/browser.js"(exports, module) {
      "use strict";
      module.exports = require_main();
    }
  });

  // java-lsp-client.ts
  var java_lsp_client_exports = {};
  __export(java_lsp_client_exports, {
    connect: () => connect
  });
  var import_browser = __toESM(require_browser(), 1);

  // node_modules/vscode-ws-jsonrpc/lib/disposable.js
  var import_vscode_jsonrpc = __toESM(require_main(), 1);

  // node_modules/vscode-ws-jsonrpc/lib/socket/socket.js
  var import_vscode_jsonrpc2 = __toESM(require_main(), 1);

  // node_modules/vscode-ws-jsonrpc/lib/socket/reader.js
  var import_vscode_jsonrpc3 = __toESM(require_main(), 1);
  var import_vscode_jsonrpc4 = __toESM(require_main(), 1);
  var WebSocketMessageReader = class extends import_vscode_jsonrpc4.AbstractMessageReader {
    constructor(socket) {
      super();
      __publicField(this, "socket");
      __publicField(this, "state", "initial");
      __publicField(this, "callback");
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      __publicField(this, "events", []);
      this.socket = socket;
      this.socket.onMessage((message) => this.readMessage(message));
      this.socket.onError((error) => this.fireError(error));
      this.socket.onClose((code, reason) => {
        if (code !== 1e3) {
          const error = {
            name: "" + code,
            message: `Error during socket reconnect: code = ${code}, reason = ${reason}`
          };
          this.fireError(error);
        }
        this.fireClose();
      });
    }
    listen(callback) {
      if (this.state === "initial") {
        this.state = "listening";
        this.callback = callback;
        while (this.events.length !== 0) {
          const event = this.events.pop();
          if (event.message !== void 0) {
            this.readMessage(event.message);
          } else if (event.error !== void 0) {
            this.fireError(event.error);
          } else {
            this.fireClose();
          }
        }
      }
      return {
        dispose: () => {
          if (this.callback === callback) {
            this.state = "initial";
            this.callback = void 0;
          }
        }
      };
    }
    dispose() {
      super.dispose();
      this.state = "initial";
      this.callback = void 0;
      this.events.splice(0, this.events.length);
    }
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    readMessage(message) {
      if (this.state === "initial") {
        this.events.splice(0, 0, { message });
      } else if (this.state === "listening") {
        try {
          const data = JSON.parse(message);
          this.callback(data);
        } catch (err) {
          const error = {
            name: "400",
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            message: `Error during message parsing, reason = ${typeof err === "object" ? err.message : "unknown"}`
          };
          this.fireError(error);
        }
      }
    }
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    fireError(error) {
      if (this.state === "initial") {
        this.events.splice(0, 0, { error });
      } else if (this.state === "listening") {
        super.fireError(error);
      }
    }
    fireClose() {
      if (this.state === "initial") {
        this.events.splice(0, 0, {});
      } else if (this.state === "listening") {
        super.fireClose();
      }
      this.state = "closed";
    }
  };

  // node_modules/vscode-ws-jsonrpc/lib/socket/writer.js
  var import_vscode_jsonrpc5 = __toESM(require_main(), 1);
  var import_vscode_jsonrpc6 = __toESM(require_main(), 1);
  var WebSocketMessageWriter = class extends import_vscode_jsonrpc6.AbstractMessageWriter {
    constructor(socket) {
      super();
      __publicField(this, "errorCount", 0);
      __publicField(this, "socket");
      this.socket = socket;
    }
    end() {
    }
    async write(msg) {
      try {
        const content = JSON.stringify(msg);
        this.socket.send(content);
      } catch (e) {
        this.errorCount++;
        this.fireError(e, msg, this.errorCount);
      }
    }
  };

  // node_modules/vscode-ws-jsonrpc/lib/socket/connection.js
  var import_vscode_jsonrpc7 = __toESM(require_main(), 1);

  // node_modules/vscode-ws-jsonrpc/lib/connection.js
  function toSocket(webSocket) {
    return {
      send: (content) => webSocket.send(content),
      onMessage: (cb) => {
        webSocket.onmessage = (event) => cb(event.data);
      },
      onError: (cb) => {
        webSocket.onerror = (event) => {
          if (Object.hasOwn(event, "message")) {
            cb(event.message);
          }
        };
      },
      onClose: (cb) => {
        webSocket.onclose = (event) => cb(event.code, event.reason);
      },
      dispose: () => webSocket.close()
    };
  }

  // monaco-shim.ts
  function m() {
    return globalThis.monaco;
  }
  function ns(getter) {
    return new Proxy({}, {
      get: (_, k) => getter()[k]
    });
  }
  function cls(getter) {
    return new Proxy(function() {
    }, {
      construct: (_, args) => new (getter())(...args),
      get: (_, k) => getter()[k]
    });
  }
  var editor = ns(() => m().editor);
  var languages = ns(() => m().languages);
  var MarkerSeverity = ns(() => m().MarkerSeverity);
  var MarkerTag = ns(() => m().MarkerTag);
  var Uri = cls(() => m().Uri);
  var Range = cls(() => m().Range);
  var Position = cls(() => m().Position);
  var Selection = cls(() => m().Selection);
  var KeyCode = ns(() => m().KeyCode);
  var KeyMod = ns(() => m().KeyMod);

  // node_modules/vscode-languageserver-types/lib/esm/main.js
  var DocumentUri;
  (function(DocumentUri2) {
    function is(value) {
      return typeof value === "string";
    }
    DocumentUri2.is = is;
  })(DocumentUri || (DocumentUri = {}));
  var URI;
  (function(URI2) {
    function is(value) {
      return typeof value === "string";
    }
    URI2.is = is;
  })(URI || (URI = {}));
  var integer;
  (function(integer2) {
    integer2.MIN_VALUE = -2147483648;
    integer2.MAX_VALUE = 2147483647;
    function is(value) {
      return typeof value === "number" && integer2.MIN_VALUE <= value && value <= integer2.MAX_VALUE;
    }
    integer2.is = is;
  })(integer || (integer = {}));
  var uinteger;
  (function(uinteger2) {
    uinteger2.MIN_VALUE = 0;
    uinteger2.MAX_VALUE = 2147483647;
    function is(value) {
      return typeof value === "number" && uinteger2.MIN_VALUE <= value && value <= uinteger2.MAX_VALUE;
    }
    uinteger2.is = is;
  })(uinteger || (uinteger = {}));
  var Position2;
  (function(Position3) {
    function create(line, character) {
      if (line === Number.MAX_VALUE) {
        line = uinteger.MAX_VALUE;
      }
      if (character === Number.MAX_VALUE) {
        character = uinteger.MAX_VALUE;
      }
      return { line, character };
    }
    Position3.create = create;
    function is(value) {
      let candidate = value;
      return Is.objectLiteral(candidate) && Is.uinteger(candidate.line) && Is.uinteger(candidate.character);
    }
    Position3.is = is;
  })(Position2 || (Position2 = {}));
  var Range2;
  (function(Range3) {
    function create(one, two, three, four) {
      if (Is.uinteger(one) && Is.uinteger(two) && Is.uinteger(three) && Is.uinteger(four)) {
        return { start: Position2.create(one, two), end: Position2.create(three, four) };
      } else if (Position2.is(one) && Position2.is(two)) {
        return { start: one, end: two };
      } else {
        throw new Error(`Range#create called with invalid arguments[${one}, ${two}, ${three}, ${four}]`);
      }
    }
    Range3.create = create;
    function is(value) {
      let candidate = value;
      return Is.objectLiteral(candidate) && Position2.is(candidate.start) && Position2.is(candidate.end);
    }
    Range3.is = is;
  })(Range2 || (Range2 = {}));
  var Location;
  (function(Location2) {
    function create(uri, range) {
      return { uri, range };
    }
    Location2.create = create;
    function is(value) {
      let candidate = value;
      return Is.objectLiteral(candidate) && Range2.is(candidate.range) && (Is.string(candidate.uri) || Is.undefined(candidate.uri));
    }
    Location2.is = is;
  })(Location || (Location = {}));
  var LocationLink;
  (function(LocationLink2) {
    function create(targetUri, targetRange, targetSelectionRange, originSelectionRange) {
      return { targetUri, targetRange, targetSelectionRange, originSelectionRange };
    }
    LocationLink2.create = create;
    function is(value) {
      let candidate = value;
      return Is.objectLiteral(candidate) && Range2.is(candidate.targetRange) && Is.string(candidate.targetUri) && Range2.is(candidate.targetSelectionRange) && (Range2.is(candidate.originSelectionRange) || Is.undefined(candidate.originSelectionRange));
    }
    LocationLink2.is = is;
  })(LocationLink || (LocationLink = {}));
  var Color;
  (function(Color2) {
    function create(red, green, blue, alpha) {
      return {
        red,
        green,
        blue,
        alpha
      };
    }
    Color2.create = create;
    function is(value) {
      const candidate = value;
      return Is.objectLiteral(candidate) && Is.numberRange(candidate.red, 0, 1) && Is.numberRange(candidate.green, 0, 1) && Is.numberRange(candidate.blue, 0, 1) && Is.numberRange(candidate.alpha, 0, 1);
    }
    Color2.is = is;
  })(Color || (Color = {}));
  var ColorInformation;
  (function(ColorInformation2) {
    function create(range, color) {
      return {
        range,
        color
      };
    }
    ColorInformation2.create = create;
    function is(value) {
      const candidate = value;
      return Is.objectLiteral(candidate) && Range2.is(candidate.range) && Color.is(candidate.color);
    }
    ColorInformation2.is = is;
  })(ColorInformation || (ColorInformation = {}));
  var ColorPresentation;
  (function(ColorPresentation2) {
    function create(label, textEdit, additionalTextEdits) {
      return {
        label,
        textEdit,
        additionalTextEdits
      };
    }
    ColorPresentation2.create = create;
    function is(value) {
      const candidate = value;
      return Is.objectLiteral(candidate) && Is.string(candidate.label) && (Is.undefined(candidate.textEdit) || TextEdit.is(candidate)) && (Is.undefined(candidate.additionalTextEdits) || Is.typedArray(candidate.additionalTextEdits, TextEdit.is));
    }
    ColorPresentation2.is = is;
  })(ColorPresentation || (ColorPresentation = {}));
  var FoldingRangeKind;
  (function(FoldingRangeKind2) {
    FoldingRangeKind2.Comment = "comment";
    FoldingRangeKind2.Imports = "imports";
    FoldingRangeKind2.Region = "region";
  })(FoldingRangeKind || (FoldingRangeKind = {}));
  var FoldingRange;
  (function(FoldingRange2) {
    function create(startLine, endLine, startCharacter, endCharacter, kind, collapsedText) {
      const result = {
        startLine,
        endLine
      };
      if (Is.defined(startCharacter)) {
        result.startCharacter = startCharacter;
      }
      if (Is.defined(endCharacter)) {
        result.endCharacter = endCharacter;
      }
      if (Is.defined(kind)) {
        result.kind = kind;
      }
      if (Is.defined(collapsedText)) {
        result.collapsedText = collapsedText;
      }
      return result;
    }
    FoldingRange2.create = create;
    function is(value) {
      const candidate = value;
      return Is.objectLiteral(candidate) && Is.uinteger(candidate.startLine) && Is.uinteger(candidate.startLine) && (Is.undefined(candidate.startCharacter) || Is.uinteger(candidate.startCharacter)) && (Is.undefined(candidate.endCharacter) || Is.uinteger(candidate.endCharacter)) && (Is.undefined(candidate.kind) || Is.string(candidate.kind));
    }
    FoldingRange2.is = is;
  })(FoldingRange || (FoldingRange = {}));
  var DiagnosticRelatedInformation;
  (function(DiagnosticRelatedInformation2) {
    function create(location2, message) {
      return {
        location: location2,
        message
      };
    }
    DiagnosticRelatedInformation2.create = create;
    function is(value) {
      let candidate = value;
      return Is.defined(candidate) && Location.is(candidate.location) && Is.string(candidate.message);
    }
    DiagnosticRelatedInformation2.is = is;
  })(DiagnosticRelatedInformation || (DiagnosticRelatedInformation = {}));
  var DiagnosticSeverity;
  (function(DiagnosticSeverity2) {
    DiagnosticSeverity2.Error = 1;
    DiagnosticSeverity2.Warning = 2;
    DiagnosticSeverity2.Information = 3;
    DiagnosticSeverity2.Hint = 4;
  })(DiagnosticSeverity || (DiagnosticSeverity = {}));
  var DiagnosticTag;
  (function(DiagnosticTag2) {
    DiagnosticTag2.Unnecessary = 1;
    DiagnosticTag2.Deprecated = 2;
  })(DiagnosticTag || (DiagnosticTag = {}));
  var CodeDescription;
  (function(CodeDescription2) {
    function is(value) {
      const candidate = value;
      return Is.objectLiteral(candidate) && Is.string(candidate.href);
    }
    CodeDescription2.is = is;
  })(CodeDescription || (CodeDescription = {}));
  var Diagnostic;
  (function(Diagnostic2) {
    function create(range, message, severity, code, source, relatedInformation) {
      let result = { range, message };
      if (Is.defined(severity)) {
        result.severity = severity;
      }
      if (Is.defined(code)) {
        result.code = code;
      }
      if (Is.defined(source)) {
        result.source = source;
      }
      if (Is.defined(relatedInformation)) {
        result.relatedInformation = relatedInformation;
      }
      return result;
    }
    Diagnostic2.create = create;
    function is(value) {
      var _a;
      let candidate = value;
      return Is.defined(candidate) && Range2.is(candidate.range) && Is.string(candidate.message) && (Is.number(candidate.severity) || Is.undefined(candidate.severity)) && (Is.integer(candidate.code) || Is.string(candidate.code) || Is.undefined(candidate.code)) && (Is.undefined(candidate.codeDescription) || Is.string((_a = candidate.codeDescription) === null || _a === void 0 ? void 0 : _a.href)) && (Is.string(candidate.source) || Is.undefined(candidate.source)) && (Is.undefined(candidate.relatedInformation) || Is.typedArray(candidate.relatedInformation, DiagnosticRelatedInformation.is));
    }
    Diagnostic2.is = is;
  })(Diagnostic || (Diagnostic = {}));
  var Command;
  (function(Command2) {
    function create(title, command, ...args) {
      let result = { title, command };
      if (Is.defined(args) && args.length > 0) {
        result.arguments = args;
      }
      return result;
    }
    Command2.create = create;
    function is(value) {
      let candidate = value;
      return Is.defined(candidate) && Is.string(candidate.title) && Is.string(candidate.command);
    }
    Command2.is = is;
  })(Command || (Command = {}));
  var TextEdit;
  (function(TextEdit2) {
    function replace(range, newText) {
      return { range, newText };
    }
    TextEdit2.replace = replace;
    function insert(position, newText) {
      return { range: { start: position, end: position }, newText };
    }
    TextEdit2.insert = insert;
    function del(range) {
      return { range, newText: "" };
    }
    TextEdit2.del = del;
    function is(value) {
      const candidate = value;
      return Is.objectLiteral(candidate) && Is.string(candidate.newText) && Range2.is(candidate.range);
    }
    TextEdit2.is = is;
  })(TextEdit || (TextEdit = {}));
  var ChangeAnnotation;
  (function(ChangeAnnotation2) {
    function create(label, needsConfirmation, description) {
      const result = { label };
      if (needsConfirmation !== void 0) {
        result.needsConfirmation = needsConfirmation;
      }
      if (description !== void 0) {
        result.description = description;
      }
      return result;
    }
    ChangeAnnotation2.create = create;
    function is(value) {
      const candidate = value;
      return Is.objectLiteral(candidate) && Is.string(candidate.label) && (Is.boolean(candidate.needsConfirmation) || candidate.needsConfirmation === void 0) && (Is.string(candidate.description) || candidate.description === void 0);
    }
    ChangeAnnotation2.is = is;
  })(ChangeAnnotation || (ChangeAnnotation = {}));
  var ChangeAnnotationIdentifier;
  (function(ChangeAnnotationIdentifier2) {
    function is(value) {
      const candidate = value;
      return Is.string(candidate);
    }
    ChangeAnnotationIdentifier2.is = is;
  })(ChangeAnnotationIdentifier || (ChangeAnnotationIdentifier = {}));
  var AnnotatedTextEdit;
  (function(AnnotatedTextEdit2) {
    function replace(range, newText, annotation) {
      return { range, newText, annotationId: annotation };
    }
    AnnotatedTextEdit2.replace = replace;
    function insert(position, newText, annotation) {
      return { range: { start: position, end: position }, newText, annotationId: annotation };
    }
    AnnotatedTextEdit2.insert = insert;
    function del(range, annotation) {
      return { range, newText: "", annotationId: annotation };
    }
    AnnotatedTextEdit2.del = del;
    function is(value) {
      const candidate = value;
      return TextEdit.is(candidate) && (ChangeAnnotation.is(candidate.annotationId) || ChangeAnnotationIdentifier.is(candidate.annotationId));
    }
    AnnotatedTextEdit2.is = is;
  })(AnnotatedTextEdit || (AnnotatedTextEdit = {}));
  var TextDocumentEdit;
  (function(TextDocumentEdit2) {
    function create(textDocument, edits) {
      return { textDocument, edits };
    }
    TextDocumentEdit2.create = create;
    function is(value) {
      let candidate = value;
      return Is.defined(candidate) && OptionalVersionedTextDocumentIdentifier.is(candidate.textDocument) && Array.isArray(candidate.edits);
    }
    TextDocumentEdit2.is = is;
  })(TextDocumentEdit || (TextDocumentEdit = {}));
  var CreateFile;
  (function(CreateFile2) {
    function create(uri, options, annotation) {
      let result = {
        kind: "create",
        uri
      };
      if (options !== void 0 && (options.overwrite !== void 0 || options.ignoreIfExists !== void 0)) {
        result.options = options;
      }
      if (annotation !== void 0) {
        result.annotationId = annotation;
      }
      return result;
    }
    CreateFile2.create = create;
    function is(value) {
      let candidate = value;
      return candidate && candidate.kind === "create" && Is.string(candidate.uri) && (candidate.options === void 0 || (candidate.options.overwrite === void 0 || Is.boolean(candidate.options.overwrite)) && (candidate.options.ignoreIfExists === void 0 || Is.boolean(candidate.options.ignoreIfExists))) && (candidate.annotationId === void 0 || ChangeAnnotationIdentifier.is(candidate.annotationId));
    }
    CreateFile2.is = is;
  })(CreateFile || (CreateFile = {}));
  var RenameFile;
  (function(RenameFile2) {
    function create(oldUri, newUri, options, annotation) {
      let result = {
        kind: "rename",
        oldUri,
        newUri
      };
      if (options !== void 0 && (options.overwrite !== void 0 || options.ignoreIfExists !== void 0)) {
        result.options = options;
      }
      if (annotation !== void 0) {
        result.annotationId = annotation;
      }
      return result;
    }
    RenameFile2.create = create;
    function is(value) {
      let candidate = value;
      return candidate && candidate.kind === "rename" && Is.string(candidate.oldUri) && Is.string(candidate.newUri) && (candidate.options === void 0 || (candidate.options.overwrite === void 0 || Is.boolean(candidate.options.overwrite)) && (candidate.options.ignoreIfExists === void 0 || Is.boolean(candidate.options.ignoreIfExists))) && (candidate.annotationId === void 0 || ChangeAnnotationIdentifier.is(candidate.annotationId));
    }
    RenameFile2.is = is;
  })(RenameFile || (RenameFile = {}));
  var DeleteFile;
  (function(DeleteFile2) {
    function create(uri, options, annotation) {
      let result = {
        kind: "delete",
        uri
      };
      if (options !== void 0 && (options.recursive !== void 0 || options.ignoreIfNotExists !== void 0)) {
        result.options = options;
      }
      if (annotation !== void 0) {
        result.annotationId = annotation;
      }
      return result;
    }
    DeleteFile2.create = create;
    function is(value) {
      let candidate = value;
      return candidate && candidate.kind === "delete" && Is.string(candidate.uri) && (candidate.options === void 0 || (candidate.options.recursive === void 0 || Is.boolean(candidate.options.recursive)) && (candidate.options.ignoreIfNotExists === void 0 || Is.boolean(candidate.options.ignoreIfNotExists))) && (candidate.annotationId === void 0 || ChangeAnnotationIdentifier.is(candidate.annotationId));
    }
    DeleteFile2.is = is;
  })(DeleteFile || (DeleteFile = {}));
  var WorkspaceEdit;
  (function(WorkspaceEdit2) {
    function is(value) {
      let candidate = value;
      return candidate && (candidate.changes !== void 0 || candidate.documentChanges !== void 0) && (candidate.documentChanges === void 0 || candidate.documentChanges.every((change) => {
        if (Is.string(change.kind)) {
          return CreateFile.is(change) || RenameFile.is(change) || DeleteFile.is(change);
        } else {
          return TextDocumentEdit.is(change);
        }
      }));
    }
    WorkspaceEdit2.is = is;
  })(WorkspaceEdit || (WorkspaceEdit = {}));
  var TextDocumentIdentifier;
  (function(TextDocumentIdentifier2) {
    function create(uri) {
      return { uri };
    }
    TextDocumentIdentifier2.create = create;
    function is(value) {
      let candidate = value;
      return Is.defined(candidate) && Is.string(candidate.uri);
    }
    TextDocumentIdentifier2.is = is;
  })(TextDocumentIdentifier || (TextDocumentIdentifier = {}));
  var VersionedTextDocumentIdentifier;
  (function(VersionedTextDocumentIdentifier2) {
    function create(uri, version) {
      return { uri, version };
    }
    VersionedTextDocumentIdentifier2.create = create;
    function is(value) {
      let candidate = value;
      return Is.defined(candidate) && Is.string(candidate.uri) && Is.integer(candidate.version);
    }
    VersionedTextDocumentIdentifier2.is = is;
  })(VersionedTextDocumentIdentifier || (VersionedTextDocumentIdentifier = {}));
  var OptionalVersionedTextDocumentIdentifier;
  (function(OptionalVersionedTextDocumentIdentifier2) {
    function create(uri, version) {
      return { uri, version };
    }
    OptionalVersionedTextDocumentIdentifier2.create = create;
    function is(value) {
      let candidate = value;
      return Is.defined(candidate) && Is.string(candidate.uri) && (candidate.version === null || Is.integer(candidate.version));
    }
    OptionalVersionedTextDocumentIdentifier2.is = is;
  })(OptionalVersionedTextDocumentIdentifier || (OptionalVersionedTextDocumentIdentifier = {}));
  var TextDocumentItem;
  (function(TextDocumentItem2) {
    function create(uri, languageId, version, text) {
      return { uri, languageId, version, text };
    }
    TextDocumentItem2.create = create;
    function is(value) {
      let candidate = value;
      return Is.defined(candidate) && Is.string(candidate.uri) && Is.string(candidate.languageId) && Is.integer(candidate.version) && Is.string(candidate.text);
    }
    TextDocumentItem2.is = is;
  })(TextDocumentItem || (TextDocumentItem = {}));
  var MarkupKind;
  (function(MarkupKind3) {
    MarkupKind3.PlainText = "plaintext";
    MarkupKind3.Markdown = "markdown";
    function is(value) {
      const candidate = value;
      return candidate === MarkupKind3.PlainText || candidate === MarkupKind3.Markdown;
    }
    MarkupKind3.is = is;
  })(MarkupKind || (MarkupKind = {}));
  var MarkupContent;
  (function(MarkupContent3) {
    function is(value) {
      const candidate = value;
      return Is.objectLiteral(value) && MarkupKind.is(candidate.kind) && Is.string(candidate.value);
    }
    MarkupContent3.is = is;
  })(MarkupContent || (MarkupContent = {}));
  var CompletionItemKind;
  (function(CompletionItemKind2) {
    CompletionItemKind2.Text = 1;
    CompletionItemKind2.Method = 2;
    CompletionItemKind2.Function = 3;
    CompletionItemKind2.Constructor = 4;
    CompletionItemKind2.Field = 5;
    CompletionItemKind2.Variable = 6;
    CompletionItemKind2.Class = 7;
    CompletionItemKind2.Interface = 8;
    CompletionItemKind2.Module = 9;
    CompletionItemKind2.Property = 10;
    CompletionItemKind2.Unit = 11;
    CompletionItemKind2.Value = 12;
    CompletionItemKind2.Enum = 13;
    CompletionItemKind2.Keyword = 14;
    CompletionItemKind2.Snippet = 15;
    CompletionItemKind2.Color = 16;
    CompletionItemKind2.File = 17;
    CompletionItemKind2.Reference = 18;
    CompletionItemKind2.Folder = 19;
    CompletionItemKind2.EnumMember = 20;
    CompletionItemKind2.Constant = 21;
    CompletionItemKind2.Struct = 22;
    CompletionItemKind2.Event = 23;
    CompletionItemKind2.Operator = 24;
    CompletionItemKind2.TypeParameter = 25;
  })(CompletionItemKind || (CompletionItemKind = {}));
  var InsertTextFormat;
  (function(InsertTextFormat2) {
    InsertTextFormat2.PlainText = 1;
    InsertTextFormat2.Snippet = 2;
  })(InsertTextFormat || (InsertTextFormat = {}));
  var CompletionItemTag;
  (function(CompletionItemTag2) {
    CompletionItemTag2.Deprecated = 1;
  })(CompletionItemTag || (CompletionItemTag = {}));
  var InsertReplaceEdit;
  (function(InsertReplaceEdit2) {
    function create(newText, insert, replace) {
      return { newText, insert, replace };
    }
    InsertReplaceEdit2.create = create;
    function is(value) {
      const candidate = value;
      return candidate && Is.string(candidate.newText) && Range2.is(candidate.insert) && Range2.is(candidate.replace);
    }
    InsertReplaceEdit2.is = is;
  })(InsertReplaceEdit || (InsertReplaceEdit = {}));
  var InsertTextMode;
  (function(InsertTextMode2) {
    InsertTextMode2.asIs = 1;
    InsertTextMode2.adjustIndentation = 2;
  })(InsertTextMode || (InsertTextMode = {}));
  var CompletionItemLabelDetails;
  (function(CompletionItemLabelDetails2) {
    function is(value) {
      const candidate = value;
      return candidate && (Is.string(candidate.detail) || candidate.detail === void 0) && (Is.string(candidate.description) || candidate.description === void 0);
    }
    CompletionItemLabelDetails2.is = is;
  })(CompletionItemLabelDetails || (CompletionItemLabelDetails = {}));
  var CompletionItem;
  (function(CompletionItem2) {
    function create(label) {
      return { label };
    }
    CompletionItem2.create = create;
  })(CompletionItem || (CompletionItem = {}));
  var CompletionList;
  (function(CompletionList2) {
    function create(items, isIncomplete) {
      return { items: items ? items : [], isIncomplete: !!isIncomplete };
    }
    CompletionList2.create = create;
  })(CompletionList || (CompletionList = {}));
  var MarkedString;
  (function(MarkedString2) {
    function fromPlainText(plainText) {
      return plainText.replace(/[\\`*_{}[\]()#+\-.!]/g, "\\$&");
    }
    MarkedString2.fromPlainText = fromPlainText;
    function is(value) {
      const candidate = value;
      return Is.string(candidate) || Is.objectLiteral(candidate) && Is.string(candidate.language) && Is.string(candidate.value);
    }
    MarkedString2.is = is;
  })(MarkedString || (MarkedString = {}));
  var Hover;
  (function(Hover2) {
    function is(value) {
      let candidate = value;
      return !!candidate && Is.objectLiteral(candidate) && (MarkupContent.is(candidate.contents) || MarkedString.is(candidate.contents) || Is.typedArray(candidate.contents, MarkedString.is)) && (value.range === void 0 || Range2.is(value.range));
    }
    Hover2.is = is;
  })(Hover || (Hover = {}));
  var ParameterInformation;
  (function(ParameterInformation2) {
    function create(label, documentation) {
      return documentation ? { label, documentation } : { label };
    }
    ParameterInformation2.create = create;
  })(ParameterInformation || (ParameterInformation = {}));
  var SignatureInformation;
  (function(SignatureInformation2) {
    function create(label, documentation, ...parameters) {
      let result = { label };
      if (Is.defined(documentation)) {
        result.documentation = documentation;
      }
      if (Is.defined(parameters)) {
        result.parameters = parameters;
      } else {
        result.parameters = [];
      }
      return result;
    }
    SignatureInformation2.create = create;
  })(SignatureInformation || (SignatureInformation = {}));
  var DocumentHighlightKind;
  (function(DocumentHighlightKind2) {
    DocumentHighlightKind2.Text = 1;
    DocumentHighlightKind2.Read = 2;
    DocumentHighlightKind2.Write = 3;
  })(DocumentHighlightKind || (DocumentHighlightKind = {}));
  var DocumentHighlight;
  (function(DocumentHighlight2) {
    function create(range, kind) {
      let result = { range };
      if (Is.number(kind)) {
        result.kind = kind;
      }
      return result;
    }
    DocumentHighlight2.create = create;
  })(DocumentHighlight || (DocumentHighlight = {}));
  var SymbolKind;
  (function(SymbolKind2) {
    SymbolKind2.File = 1;
    SymbolKind2.Module = 2;
    SymbolKind2.Namespace = 3;
    SymbolKind2.Package = 4;
    SymbolKind2.Class = 5;
    SymbolKind2.Method = 6;
    SymbolKind2.Property = 7;
    SymbolKind2.Field = 8;
    SymbolKind2.Constructor = 9;
    SymbolKind2.Enum = 10;
    SymbolKind2.Interface = 11;
    SymbolKind2.Function = 12;
    SymbolKind2.Variable = 13;
    SymbolKind2.Constant = 14;
    SymbolKind2.String = 15;
    SymbolKind2.Number = 16;
    SymbolKind2.Boolean = 17;
    SymbolKind2.Array = 18;
    SymbolKind2.Object = 19;
    SymbolKind2.Key = 20;
    SymbolKind2.Null = 21;
    SymbolKind2.EnumMember = 22;
    SymbolKind2.Struct = 23;
    SymbolKind2.Event = 24;
    SymbolKind2.Operator = 25;
    SymbolKind2.TypeParameter = 26;
  })(SymbolKind || (SymbolKind = {}));
  var SymbolTag;
  (function(SymbolTag2) {
    SymbolTag2.Deprecated = 1;
  })(SymbolTag || (SymbolTag = {}));
  var SymbolInformation;
  (function(SymbolInformation2) {
    function create(name, kind, range, uri, containerName) {
      let result = {
        name,
        kind,
        location: { uri, range }
      };
      if (containerName) {
        result.containerName = containerName;
      }
      return result;
    }
    SymbolInformation2.create = create;
  })(SymbolInformation || (SymbolInformation = {}));
  var WorkspaceSymbol;
  (function(WorkspaceSymbol2) {
    function create(name, kind, uri, range) {
      return range !== void 0 ? { name, kind, location: { uri, range } } : { name, kind, location: { uri } };
    }
    WorkspaceSymbol2.create = create;
  })(WorkspaceSymbol || (WorkspaceSymbol = {}));
  var DocumentSymbol;
  (function(DocumentSymbol2) {
    function create(name, detail, kind, range, selectionRange, children) {
      let result = {
        name,
        detail,
        kind,
        range,
        selectionRange
      };
      if (children !== void 0) {
        result.children = children;
      }
      return result;
    }
    DocumentSymbol2.create = create;
    function is(value) {
      let candidate = value;
      return candidate && Is.string(candidate.name) && Is.number(candidate.kind) && Range2.is(candidate.range) && Range2.is(candidate.selectionRange) && (candidate.detail === void 0 || Is.string(candidate.detail)) && (candidate.deprecated === void 0 || Is.boolean(candidate.deprecated)) && (candidate.children === void 0 || Array.isArray(candidate.children)) && (candidate.tags === void 0 || Array.isArray(candidate.tags));
    }
    DocumentSymbol2.is = is;
  })(DocumentSymbol || (DocumentSymbol = {}));
  var CodeActionKind;
  (function(CodeActionKind2) {
    CodeActionKind2.Empty = "";
    CodeActionKind2.QuickFix = "quickfix";
    CodeActionKind2.Refactor = "refactor";
    CodeActionKind2.RefactorExtract = "refactor.extract";
    CodeActionKind2.RefactorInline = "refactor.inline";
    CodeActionKind2.RefactorRewrite = "refactor.rewrite";
    CodeActionKind2.Source = "source";
    CodeActionKind2.SourceOrganizeImports = "source.organizeImports";
    CodeActionKind2.SourceFixAll = "source.fixAll";
  })(CodeActionKind || (CodeActionKind = {}));
  var CodeActionTriggerKind;
  (function(CodeActionTriggerKind2) {
    CodeActionTriggerKind2.Invoked = 1;
    CodeActionTriggerKind2.Automatic = 2;
  })(CodeActionTriggerKind || (CodeActionTriggerKind = {}));
  var CodeActionContext;
  (function(CodeActionContext2) {
    function create(diagnostics, only, triggerKind) {
      let result = { diagnostics };
      if (only !== void 0 && only !== null) {
        result.only = only;
      }
      if (triggerKind !== void 0 && triggerKind !== null) {
        result.triggerKind = triggerKind;
      }
      return result;
    }
    CodeActionContext2.create = create;
    function is(value) {
      let candidate = value;
      return Is.defined(candidate) && Is.typedArray(candidate.diagnostics, Diagnostic.is) && (candidate.only === void 0 || Is.typedArray(candidate.only, Is.string)) && (candidate.triggerKind === void 0 || candidate.triggerKind === CodeActionTriggerKind.Invoked || candidate.triggerKind === CodeActionTriggerKind.Automatic);
    }
    CodeActionContext2.is = is;
  })(CodeActionContext || (CodeActionContext = {}));
  var CodeAction;
  (function(CodeAction2) {
    function create(title, kindOrCommandOrEdit, kind) {
      let result = { title };
      let checkKind = true;
      if (typeof kindOrCommandOrEdit === "string") {
        checkKind = false;
        result.kind = kindOrCommandOrEdit;
      } else if (Command.is(kindOrCommandOrEdit)) {
        result.command = kindOrCommandOrEdit;
      } else {
        result.edit = kindOrCommandOrEdit;
      }
      if (checkKind && kind !== void 0) {
        result.kind = kind;
      }
      return result;
    }
    CodeAction2.create = create;
    function is(value) {
      let candidate = value;
      return candidate && Is.string(candidate.title) && (candidate.diagnostics === void 0 || Is.typedArray(candidate.diagnostics, Diagnostic.is)) && (candidate.kind === void 0 || Is.string(candidate.kind)) && (candidate.edit !== void 0 || candidate.command !== void 0) && (candidate.command === void 0 || Command.is(candidate.command)) && (candidate.isPreferred === void 0 || Is.boolean(candidate.isPreferred)) && (candidate.edit === void 0 || WorkspaceEdit.is(candidate.edit));
    }
    CodeAction2.is = is;
  })(CodeAction || (CodeAction = {}));
  var CodeLens;
  (function(CodeLens2) {
    function create(range, data) {
      let result = { range };
      if (Is.defined(data)) {
        result.data = data;
      }
      return result;
    }
    CodeLens2.create = create;
    function is(value) {
      let candidate = value;
      return Is.defined(candidate) && Range2.is(candidate.range) && (Is.undefined(candidate.command) || Command.is(candidate.command));
    }
    CodeLens2.is = is;
  })(CodeLens || (CodeLens = {}));
  var FormattingOptions;
  (function(FormattingOptions2) {
    function create(tabSize, insertSpaces) {
      return { tabSize, insertSpaces };
    }
    FormattingOptions2.create = create;
    function is(value) {
      let candidate = value;
      return Is.defined(candidate) && Is.uinteger(candidate.tabSize) && Is.boolean(candidate.insertSpaces);
    }
    FormattingOptions2.is = is;
  })(FormattingOptions || (FormattingOptions = {}));
  var DocumentLink;
  (function(DocumentLink2) {
    function create(range, target, data) {
      return { range, target, data };
    }
    DocumentLink2.create = create;
    function is(value) {
      let candidate = value;
      return Is.defined(candidate) && Range2.is(candidate.range) && (Is.undefined(candidate.target) || Is.string(candidate.target));
    }
    DocumentLink2.is = is;
  })(DocumentLink || (DocumentLink = {}));
  var SelectionRange;
  (function(SelectionRange2) {
    function create(range, parent) {
      return { range, parent };
    }
    SelectionRange2.create = create;
    function is(value) {
      let candidate = value;
      return Is.objectLiteral(candidate) && Range2.is(candidate.range) && (candidate.parent === void 0 || SelectionRange2.is(candidate.parent));
    }
    SelectionRange2.is = is;
  })(SelectionRange || (SelectionRange = {}));
  var SemanticTokenTypes;
  (function(SemanticTokenTypes2) {
    SemanticTokenTypes2["namespace"] = "namespace";
    SemanticTokenTypes2["type"] = "type";
    SemanticTokenTypes2["class"] = "class";
    SemanticTokenTypes2["enum"] = "enum";
    SemanticTokenTypes2["interface"] = "interface";
    SemanticTokenTypes2["struct"] = "struct";
    SemanticTokenTypes2["typeParameter"] = "typeParameter";
    SemanticTokenTypes2["parameter"] = "parameter";
    SemanticTokenTypes2["variable"] = "variable";
    SemanticTokenTypes2["property"] = "property";
    SemanticTokenTypes2["enumMember"] = "enumMember";
    SemanticTokenTypes2["event"] = "event";
    SemanticTokenTypes2["function"] = "function";
    SemanticTokenTypes2["method"] = "method";
    SemanticTokenTypes2["macro"] = "macro";
    SemanticTokenTypes2["keyword"] = "keyword";
    SemanticTokenTypes2["modifier"] = "modifier";
    SemanticTokenTypes2["comment"] = "comment";
    SemanticTokenTypes2["string"] = "string";
    SemanticTokenTypes2["number"] = "number";
    SemanticTokenTypes2["regexp"] = "regexp";
    SemanticTokenTypes2["operator"] = "operator";
    SemanticTokenTypes2["decorator"] = "decorator";
  })(SemanticTokenTypes || (SemanticTokenTypes = {}));
  var SemanticTokenModifiers;
  (function(SemanticTokenModifiers2) {
    SemanticTokenModifiers2["declaration"] = "declaration";
    SemanticTokenModifiers2["definition"] = "definition";
    SemanticTokenModifiers2["readonly"] = "readonly";
    SemanticTokenModifiers2["static"] = "static";
    SemanticTokenModifiers2["deprecated"] = "deprecated";
    SemanticTokenModifiers2["abstract"] = "abstract";
    SemanticTokenModifiers2["async"] = "async";
    SemanticTokenModifiers2["modification"] = "modification";
    SemanticTokenModifiers2["documentation"] = "documentation";
    SemanticTokenModifiers2["defaultLibrary"] = "defaultLibrary";
  })(SemanticTokenModifiers || (SemanticTokenModifiers = {}));
  var SemanticTokens;
  (function(SemanticTokens2) {
    function is(value) {
      const candidate = value;
      return Is.objectLiteral(candidate) && (candidate.resultId === void 0 || typeof candidate.resultId === "string") && Array.isArray(candidate.data) && (candidate.data.length === 0 || typeof candidate.data[0] === "number");
    }
    SemanticTokens2.is = is;
  })(SemanticTokens || (SemanticTokens = {}));
  var InlineValueText;
  (function(InlineValueText2) {
    function create(range, text) {
      return { range, text };
    }
    InlineValueText2.create = create;
    function is(value) {
      const candidate = value;
      return candidate !== void 0 && candidate !== null && Range2.is(candidate.range) && Is.string(candidate.text);
    }
    InlineValueText2.is = is;
  })(InlineValueText || (InlineValueText = {}));
  var InlineValueVariableLookup;
  (function(InlineValueVariableLookup2) {
    function create(range, variableName, caseSensitiveLookup) {
      return { range, variableName, caseSensitiveLookup };
    }
    InlineValueVariableLookup2.create = create;
    function is(value) {
      const candidate = value;
      return candidate !== void 0 && candidate !== null && Range2.is(candidate.range) && Is.boolean(candidate.caseSensitiveLookup) && (Is.string(candidate.variableName) || candidate.variableName === void 0);
    }
    InlineValueVariableLookup2.is = is;
  })(InlineValueVariableLookup || (InlineValueVariableLookup = {}));
  var InlineValueEvaluatableExpression;
  (function(InlineValueEvaluatableExpression2) {
    function create(range, expression) {
      return { range, expression };
    }
    InlineValueEvaluatableExpression2.create = create;
    function is(value) {
      const candidate = value;
      return candidate !== void 0 && candidate !== null && Range2.is(candidate.range) && (Is.string(candidate.expression) || candidate.expression === void 0);
    }
    InlineValueEvaluatableExpression2.is = is;
  })(InlineValueEvaluatableExpression || (InlineValueEvaluatableExpression = {}));
  var InlineValueContext;
  (function(InlineValueContext2) {
    function create(frameId, stoppedLocation) {
      return { frameId, stoppedLocation };
    }
    InlineValueContext2.create = create;
    function is(value) {
      const candidate = value;
      return Is.defined(candidate) && Range2.is(value.stoppedLocation);
    }
    InlineValueContext2.is = is;
  })(InlineValueContext || (InlineValueContext = {}));
  var InlayHintKind;
  (function(InlayHintKind2) {
    InlayHintKind2.Type = 1;
    InlayHintKind2.Parameter = 2;
    function is(value) {
      return value === 1 || value === 2;
    }
    InlayHintKind2.is = is;
  })(InlayHintKind || (InlayHintKind = {}));
  var InlayHintLabelPart;
  (function(InlayHintLabelPart2) {
    function create(value) {
      return { value };
    }
    InlayHintLabelPart2.create = create;
    function is(value) {
      const candidate = value;
      return Is.objectLiteral(candidate) && (candidate.tooltip === void 0 || Is.string(candidate.tooltip) || MarkupContent.is(candidate.tooltip)) && (candidate.location === void 0 || Location.is(candidate.location)) && (candidate.command === void 0 || Command.is(candidate.command));
    }
    InlayHintLabelPart2.is = is;
  })(InlayHintLabelPart || (InlayHintLabelPart = {}));
  var InlayHint;
  (function(InlayHint2) {
    function create(position, label, kind) {
      const result = { position, label };
      if (kind !== void 0) {
        result.kind = kind;
      }
      return result;
    }
    InlayHint2.create = create;
    function is(value) {
      const candidate = value;
      return Is.objectLiteral(candidate) && Position2.is(candidate.position) && (Is.string(candidate.label) || Is.typedArray(candidate.label, InlayHintLabelPart.is)) && (candidate.kind === void 0 || InlayHintKind.is(candidate.kind)) && candidate.textEdits === void 0 || Is.typedArray(candidate.textEdits, TextEdit.is) && (candidate.tooltip === void 0 || Is.string(candidate.tooltip) || MarkupContent.is(candidate.tooltip)) && (candidate.paddingLeft === void 0 || Is.boolean(candidate.paddingLeft)) && (candidate.paddingRight === void 0 || Is.boolean(candidate.paddingRight));
    }
    InlayHint2.is = is;
  })(InlayHint || (InlayHint = {}));
  var StringValue;
  (function(StringValue2) {
    function createSnippet(value) {
      return { kind: "snippet", value };
    }
    StringValue2.createSnippet = createSnippet;
  })(StringValue || (StringValue = {}));
  var InlineCompletionItem;
  (function(InlineCompletionItem2) {
    function create(insertText, filterText, range, command) {
      return { insertText, filterText, range, command };
    }
    InlineCompletionItem2.create = create;
  })(InlineCompletionItem || (InlineCompletionItem = {}));
  var InlineCompletionList;
  (function(InlineCompletionList2) {
    function create(items) {
      return { items };
    }
    InlineCompletionList2.create = create;
  })(InlineCompletionList || (InlineCompletionList = {}));
  var InlineCompletionTriggerKind;
  (function(InlineCompletionTriggerKind2) {
    InlineCompletionTriggerKind2.Invoked = 0;
    InlineCompletionTriggerKind2.Automatic = 1;
  })(InlineCompletionTriggerKind || (InlineCompletionTriggerKind = {}));
  var SelectedCompletionInfo;
  (function(SelectedCompletionInfo2) {
    function create(range, text) {
      return { range, text };
    }
    SelectedCompletionInfo2.create = create;
  })(SelectedCompletionInfo || (SelectedCompletionInfo = {}));
  var InlineCompletionContext;
  (function(InlineCompletionContext2) {
    function create(triggerKind, selectedCompletionInfo) {
      return { triggerKind, selectedCompletionInfo };
    }
    InlineCompletionContext2.create = create;
  })(InlineCompletionContext || (InlineCompletionContext = {}));
  var WorkspaceFolder;
  (function(WorkspaceFolder2) {
    function is(value) {
      const candidate = value;
      return Is.objectLiteral(candidate) && URI.is(candidate.uri) && Is.string(candidate.name);
    }
    WorkspaceFolder2.is = is;
  })(WorkspaceFolder || (WorkspaceFolder = {}));
  var TextDocument;
  (function(TextDocument2) {
    function create(uri, languageId, version, content) {
      return new FullTextDocument(uri, languageId, version, content);
    }
    TextDocument2.create = create;
    function is(value) {
      let candidate = value;
      return Is.defined(candidate) && Is.string(candidate.uri) && (Is.undefined(candidate.languageId) || Is.string(candidate.languageId)) && Is.uinteger(candidate.lineCount) && Is.func(candidate.getText) && Is.func(candidate.positionAt) && Is.func(candidate.offsetAt) ? true : false;
    }
    TextDocument2.is = is;
    function applyEdits(document, edits) {
      let text = document.getText();
      let sortedEdits = mergeSort(edits, (a, b) => {
        let diff = a.range.start.line - b.range.start.line;
        if (diff === 0) {
          return a.range.start.character - b.range.start.character;
        }
        return diff;
      });
      let lastModifiedOffset = text.length;
      for (let i = sortedEdits.length - 1; i >= 0; i--) {
        let e = sortedEdits[i];
        let startOffset = document.offsetAt(e.range.start);
        let endOffset = document.offsetAt(e.range.end);
        if (endOffset <= lastModifiedOffset) {
          text = text.substring(0, startOffset) + e.newText + text.substring(endOffset, text.length);
        } else {
          throw new Error("Overlapping edit");
        }
        lastModifiedOffset = startOffset;
      }
      return text;
    }
    TextDocument2.applyEdits = applyEdits;
    function mergeSort(data, compare) {
      if (data.length <= 1) {
        return data;
      }
      const p = data.length / 2 | 0;
      const left = data.slice(0, p);
      const right = data.slice(p);
      mergeSort(left, compare);
      mergeSort(right, compare);
      let leftIdx = 0;
      let rightIdx = 0;
      let i = 0;
      while (leftIdx < left.length && rightIdx < right.length) {
        let ret = compare(left[leftIdx], right[rightIdx]);
        if (ret <= 0) {
          data[i++] = left[leftIdx++];
        } else {
          data[i++] = right[rightIdx++];
        }
      }
      while (leftIdx < left.length) {
        data[i++] = left[leftIdx++];
      }
      while (rightIdx < right.length) {
        data[i++] = right[rightIdx++];
      }
      return data;
    }
  })(TextDocument || (TextDocument = {}));
  var FullTextDocument = class {
    constructor(uri, languageId, version, content) {
      this._uri = uri;
      this._languageId = languageId;
      this._version = version;
      this._content = content;
      this._lineOffsets = void 0;
    }
    get uri() {
      return this._uri;
    }
    get languageId() {
      return this._languageId;
    }
    get version() {
      return this._version;
    }
    getText(range) {
      if (range) {
        let start = this.offsetAt(range.start);
        let end = this.offsetAt(range.end);
        return this._content.substring(start, end);
      }
      return this._content;
    }
    update(event, version) {
      this._content = event.text;
      this._version = version;
      this._lineOffsets = void 0;
    }
    getLineOffsets() {
      if (this._lineOffsets === void 0) {
        let lineOffsets = [];
        let text = this._content;
        let isLineStart = true;
        for (let i = 0; i < text.length; i++) {
          if (isLineStart) {
            lineOffsets.push(i);
            isLineStart = false;
          }
          let ch = text.charAt(i);
          isLineStart = ch === "\r" || ch === "\n";
          if (ch === "\r" && i + 1 < text.length && text.charAt(i + 1) === "\n") {
            i++;
          }
        }
        if (isLineStart && text.length > 0) {
          lineOffsets.push(text.length);
        }
        this._lineOffsets = lineOffsets;
      }
      return this._lineOffsets;
    }
    positionAt(offset) {
      offset = Math.max(Math.min(offset, this._content.length), 0);
      let lineOffsets = this.getLineOffsets();
      let low = 0, high = lineOffsets.length;
      if (high === 0) {
        return Position2.create(0, offset);
      }
      while (low < high) {
        let mid = Math.floor((low + high) / 2);
        if (lineOffsets[mid] > offset) {
          high = mid;
        } else {
          low = mid + 1;
        }
      }
      let line = low - 1;
      return Position2.create(line, offset - lineOffsets[line]);
    }
    offsetAt(position) {
      let lineOffsets = this.getLineOffsets();
      if (position.line >= lineOffsets.length) {
        return this._content.length;
      } else if (position.line < 0) {
        return 0;
      }
      let lineOffset = lineOffsets[position.line];
      let nextLineOffset = position.line + 1 < lineOffsets.length ? lineOffsets[position.line + 1] : this._content.length;
      return Math.max(Math.min(lineOffset + position.character, nextLineOffset), lineOffset);
    }
    get lineCount() {
      return this.getLineOffsets().length;
    }
  };
  var Is;
  (function(Is2) {
    const toString = Object.prototype.toString;
    function defined(value) {
      return typeof value !== "undefined";
    }
    Is2.defined = defined;
    function undefined2(value) {
      return typeof value === "undefined";
    }
    Is2.undefined = undefined2;
    function boolean(value) {
      return value === true || value === false;
    }
    Is2.boolean = boolean;
    function string(value) {
      return toString.call(value) === "[object String]";
    }
    Is2.string = string;
    function number(value) {
      return toString.call(value) === "[object Number]";
    }
    Is2.number = number;
    function numberRange(value, min, max) {
      return toString.call(value) === "[object Number]" && min <= value && value <= max;
    }
    Is2.numberRange = numberRange;
    function integer2(value) {
      return toString.call(value) === "[object Number]" && -2147483648 <= value && value <= 2147483647;
    }
    Is2.integer = integer2;
    function uinteger2(value) {
      return toString.call(value) === "[object Number]" && 0 <= value && value <= 2147483647;
    }
    Is2.uinteger = uinteger2;
    function func(value) {
      return toString.call(value) === "[object Function]";
    }
    Is2.func = func;
    function objectLiteral(value) {
      return value !== null && typeof value === "object";
    }
    Is2.objectLiteral = objectLiteral;
    function typedArray(value, check) {
      return Array.isArray(value) && value.every(check);
    }
    Is2.typedArray = typedArray;
  })(Is || (Is = {}));

  // java-lsp-client.ts
  var _conn = null;
  var _workspaceRoot = "";
  var _openFiles = /* @__PURE__ */ new Set();
  var _changeTimers = /* @__PURE__ */ new Map();
  var _diagnostics = /* @__PURE__ */ new Map();
  var _providersRegistered = false;
  var _semanticTokensLegend = null;
  async function connect(resourcePath) {
    const parts = resourcePath.replace(/^\//, "").split("/");
    const workspace = parts[0];
    const project = parts[1];
    const fileUri = `file:///workspace/${workspace}/${project}/${parts.slice(2).join("/")}`;
    if (_conn) {
      openFile(fileUri);
      return;
    }
    _workspaceRoot = `file:///workspace/${workspace}/`;
    const proto = location.protocol === "https:" ? "wss" : "ws";
    const wsUrl = `${proto}://${location.host}/websockets/ide/java-lsp?workspace=${encodeURIComponent(workspace)}`;
    const ws = new WebSocket(wsUrl);
    await new Promise((resolve, reject) => {
      ws.onopen = () => resolve();
      ws.onerror = () => reject(new Error(`[java-lsp] WebSocket connect failed: ${wsUrl}`));
    });
    const socket = toSocket(ws);
    const reader = new WebSocketMessageReader(socket);
    const writer = new WebSocketMessageWriter(socket);
    _conn = (0, import_browser.createMessageConnection)(reader, writer);
    _conn.onNotification("textDocument/publishDiagnostics", (params) => {
      _diagnostics.set(params.uri, params.diagnostics ?? []);
      const model = editor.getModels().find((m2) => m2.uri.toString() === params.uri);
      if (!model) return;
      editor.setModelMarkers(model, "java-lsp", params.diagnostics.map((d) => ({
        severity: lspSeverity(d.severity),
        message: d.message,
        source: d.source ?? "java",
        startLineNumber: d.range.start.line + 1,
        startColumn: d.range.start.character + 1,
        endLineNumber: d.range.end.line + 1,
        endColumn: d.range.end.character + 1
      })));
    });
    _conn.onRequest("workspace/applyEdit", (params) => {
      applyWorkspaceEdit(params.edit);
      return { applied: true };
    });
    _conn.onRequest("workspace/configuration", (params) => (params.items ?? []).map(() => jdtlsSettings().java));
    _conn.onRequest("client/registerCapability", () => null);
    _conn.onRequest("client/unregisterCapability", () => null);
    _conn.onRequest("window/showMessageRequest", () => null);
    _conn.onRequest("window/workDoneProgress/create", () => null);
    _conn.onNotification("window/logMessage", (p) => console.debug("[java-lsp]", p?.message));
    _conn.onNotification("window/showMessage", (p) => console.info("[java-lsp]", p?.message));
    _conn.onNotification("language/status", () => {
    });
    _conn.onNotification("language/progressReport", () => {
    });
    _conn.listen();
    const rootUri = _workspaceRoot;
    const initResult = await _conn.sendRequest("initialize", {
      processId: null,
      rootUri,
      initializationOptions: {
        settings: jdtlsSettings(),
        extendedClientCapabilities: {
          progressReportProvider: false,
          classFileContentsSupport: true,
          resolveAdditionalTextEditsSupport: true,
          // Do NOT advertise the *PromptSupport flags: those make JDT.LS return source actions
          // (generate toString/constructors/accessors, override/implement, organize imports) as
          // client-side "*Prompt" commands the vscode-java extension implements but we don't.
          // With them off, JDT.LS returns the same actions as resolvable WorkspaceEdits operating
          // on all members, which applyCodeAction resolves and applies directly.
          inferSelectionSupport: ["extractMethod", "extractVariable", "extractField"]
        }
      },
      workspaceFolders: [{ uri: rootUri, name: workspace }],
      capabilities: {
        textDocument: {
          synchronization: { dynamicRegistration: true, willSave: false, didSave: true, willSaveWaitUntil: false },
          completion: {
            dynamicRegistration: true,
            completionItem: {
              snippetSupport: true,
              documentationFormat: ["markdown", "plaintext"],
              deprecatedSupport: true,
              commitCharactersSupport: true,
              resolveSupport: { properties: ["documentation", "detail", "additionalTextEdits"] }
            },
            contextSupport: true
          },
          hover: { dynamicRegistration: true, contentFormat: ["markdown", "plaintext"] },
          signatureHelp: { dynamicRegistration: true, signatureInformation: { documentationFormat: ["markdown", "plaintext"], parameterInformation: { labelOffsetSupport: true } } },
          definition: { dynamicRegistration: true },
          references: { dynamicRegistration: true },
          implementation: { dynamicRegistration: true },
          typeDefinition: { dynamicRegistration: true },
          documentHighlight: { dynamicRegistration: true },
          documentSymbol: { dynamicRegistration: true, hierarchicalDocumentSymbolSupport: true },
          foldingRange: { dynamicRegistration: true, lineFoldingOnly: false },
          selectionRange: { dynamicRegistration: true },
          codeLens: { dynamicRegistration: true },
          inlayHint: { dynamicRegistration: true, resolveSupport: { properties: ["label"] } },
          semanticTokens: {
            dynamicRegistration: true,
            requests: { range: false, full: { delta: false } },
            tokenTypes: [
              "namespace",
              "type",
              "class",
              "enum",
              "interface",
              "struct",
              "typeParameter",
              "parameter",
              "variable",
              "property",
              "enumMember",
              "event",
              "function",
              "method",
              "macro",
              "keyword",
              "modifier",
              "comment",
              "string",
              "number",
              "regexp",
              "operator",
              "decorator"
            ],
            tokenModifiers: [
              "declaration",
              "definition",
              "readonly",
              "static",
              "deprecated",
              "abstract",
              "async",
              "modification",
              "documentation",
              "defaultLibrary"
            ],
            formats: ["relative"],
            overlappingTokenSupport: false,
            multilineTokenSupport: false
          },
          formatting: { dynamicRegistration: true },
          rangeFormatting: { dynamicRegistration: true },
          rename: { dynamicRegistration: true, prepareSupport: true },
          codeAction: {
            dynamicRegistration: true,
            codeActionLiteralSupport: {
              codeActionKind: {
                valueSet: [
                  "quickfix",
                  "refactor",
                  "refactor.extract",
                  "refactor.inline",
                  "refactor.rewrite",
                  "source",
                  "source.organizeImports"
                ]
              }
            },
            isPreferredSupport: true,
            dataSupport: true,
            resolveSupport: { properties: ["edit"] }
          },
          publishDiagnostics: { relatedInformation: true }
        },
        workspace: {
          applyEdit: true,
          configuration: true,
          executeCommand: { dynamicRegistration: true },
          didChangeConfiguration: { dynamicRegistration: true },
          workspaceEdit: { documentChanges: true, resourceOperations: ["create", "rename", "delete"] }
        }
      }
    });
    _semanticTokensLegend = initResult?.capabilities?.semanticTokensProvider?.legend ?? null;
    _conn.sendNotification("initialized", {});
    _conn.sendNotification("workspace/didChangeConfiguration", { settings: jdtlsSettings() });
    openFile(fileUri);
    if (!_providersRegistered) {
      _providersRegistered = true;
      registerProviders();
    }
  }
  function openFile(fileUri) {
    if (_openFiles.has(fileUri) || !_conn) return;
    _openFiles.add(fileUri);
    const model = editor.getModels().find((m2) => m2.uri.toString() === fileUri);
    _conn.sendNotification("textDocument/didOpen", {
      textDocument: {
        uri: fileUri,
        languageId: "java",
        version: 1,
        text: model?.getValue() ?? ""
      }
    });
    _conn.sendNotification("workspace/didChangeWatchedFiles", { changes: [{
      uri: fileUri,
      type: 1
      /* Created */
    }] });
    if (model) {
      model.onDidChangeContent(() => {
        const existing = _changeTimers.get(fileUri);
        if (existing) clearTimeout(existing);
        _changeTimers.set(fileUri, setTimeout(() => sendDidChange(fileUri), 400));
      });
    }
  }
  function sendDidChange(fileUri) {
    _changeTimers.delete(fileUri);
    const model = editor.getModel(Uri.parse(fileUri));
    if (_conn && model) {
      _conn.sendNotification("textDocument/didChange", {
        textDocument: { uri: fileUri, version: model.getVersionId() },
        contentChanges: [{ text: model.getValue() }]
      });
    }
  }
  function flushPendingChange(fileUri) {
    if (_changeTimers.has(fileUri)) {
      clearTimeout(_changeTimers.get(fileUri));
      sendDidChange(fileUri);
    }
  }
  function isWorkspaceFile(uri) {
    return _workspaceRoot !== "" && uri.startsWith(_workspaceRoot);
  }
  function registerProviders() {
    editor.registerCommand(APPLY_ACTION_COMMAND, (_accessor, action) => {
      applyCodeAction(action);
    });
    editor.registerCommand(NOOP_COMMAND, () => {
    });
    editor.registerEditorOpener({
      openCodeEditor: (source, resource, selectionOrPosition) => {
        const uri = resource.toString();
        if (!isWorkspaceFile(uri) || !uri.startsWith(VIRTUAL_FILE_PREFIX)) return false;
        const currentModel = source.getModel();
        if (currentModel && currentModel.uri.toString() === uri) return false;
        const opener = globalThis.javaLspOpenFile;
        if (typeof opener !== "function") return false;
        const pos = selectionOrPosition;
        const line = pos ? pos.startLineNumber ?? pos.lineNumber : void 0;
        const column = pos ? pos.startColumn ?? pos.column : void 0;
        opener(uri.substring(VIRTUAL_FILE_PREFIX.length), line, column);
        return true;
      }
    });
    languages.registerCompletionItemProvider("java", {
      triggerCharacters: [".", "@", "<"],
      provideCompletionItems: async (model, position, context) => {
        if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
        const fileUri = model.uri.toString();
        flushPendingChange(fileUri);
        const result = await _conn.sendRequest("textDocument/completion", {
          textDocument: { uri: fileUri },
          position: { line: position.lineNumber - 1, character: position.column - 1 },
          // Monaco trigger kinds are 0-based (Invoke/TriggerCharacter/ForIncomplete); LSP is 1-based.
          context: { triggerKind: (context.triggerKind ?? 0) + 1, triggerCharacter: context.triggerCharacter }
        });
        const items = Array.isArray(result) ? result : result?.items ?? [];
        return {
          suggestions: items.map((item) => lspCompletionToMonaco(item, model, position)),
          // JDT.LS returns a truncated list on the first keystrokes; propagating "incomplete" makes
          // Monaco re-query as the user types instead of caching the first (often empty) result.
          incomplete: Array.isArray(result) ? false : !!result?.isIncomplete
        };
      },
      // Resolve documentation and, crucially, the auto-import additionalTextEdits which JDT.LS only
      // attaches on resolve — selecting a type then inserts its import statement.
      resolveCompletionItem: async (item) => {
        const lsp = item._lsp;
        if (!_conn || !lsp) return item;
        try {
          const resolved = await _conn.sendRequest("completionItem/resolve", lsp);
          if (resolved.documentation) {
            item.documentation = { value: markupToString(resolved.documentation), isTrusted: false };
          }
          if (resolved.detail) item.detail = resolved.detail;
          if (resolved.additionalTextEdits?.length) {
            item.additionalTextEdits = resolved.additionalTextEdits.map(textEditToMonaco);
          }
        } catch (e) {
          console.debug("[java-lsp] completion resolve failed:", e?.message);
        }
        return item;
      }
    });
    languages.registerHoverProvider("java", {
      provideHover: async (model, position) => {
        if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
        const fileUri = model.uri.toString();
        const result = await _conn.sendRequest("textDocument/hover", {
          textDocument: { uri: fileUri },
          position: { line: position.lineNumber - 1, character: position.column - 1 }
        });
        if (!result?.contents) return null;
        const contents = Array.isArray(result.contents) ? result.contents : [result.contents];
        return {
          contents: contents.map((c) => ({
            value: typeof c === "string" ? c : c.value,
            isTrusted: false
          })),
          range: result.range ? lspRangeToMonaco(result.range) : void 0
        };
      }
    });
    languages.registerSignatureHelpProvider("java", {
      signatureHelpTriggerCharacters: ["(", ","],
      provideSignatureHelp: async (model, position) => {
        if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
        const fileUri = model.uri.toString();
        const result = await _conn.sendRequest("textDocument/signatureHelp", {
          textDocument: { uri: fileUri },
          position: { line: position.lineNumber - 1, character: position.column - 1 }
        });
        if (!result) return null;
        return {
          value: {
            signatures: result.signatures.map((sig) => ({
              label: sig.label,
              documentation: sig.documentation ? markupToString(sig.documentation) : void 0,
              parameters: (sig.parameters ?? []).map((p) => ({
                label: p.label,
                documentation: p.documentation ? markupToString(p.documentation) : void 0
              }))
            })),
            activeSignature: result.activeSignature ?? 0,
            activeParameter: result.activeParameter ?? 0
          },
          dispose: () => {
          }
        };
      }
    });
    languages.registerDefinitionProvider("java", {
      provideDefinition: async (model, position) => {
        if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
        const fileUri = model.uri.toString();
        const result = await _conn.sendRequest("textDocument/definition", {
          textDocument: { uri: fileUri },
          position: { line: position.lineNumber - 1, character: position.column - 1 }
        });
        if (!result) return null;
        const locations = (Array.isArray(result) ? result : [result]).map((loc) => ({
          uri: Uri.parse(loc.uri),
          range: lspRangeToMonaco(loc.range)
        }));
        await ensureModelsForLocations(locations);
        return locations;
      }
    });
    languages.registerReferenceProvider("java", {
      provideReferences: async (model, position, context) => {
        if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
        const result = await _conn.sendRequest("textDocument/references", {
          textDocument: { uri: model.uri.toString() },
          position: { line: position.lineNumber - 1, character: position.column - 1 },
          context: { includeDeclaration: context.includeDeclaration }
        });
        if (!result) return null;
        const locations = result.map((loc) => ({ uri: Uri.parse(loc.uri), range: lspRangeToMonaco(loc.range) }));
        await ensureModelsForLocations(locations);
        return locations;
      }
    });
    languages.registerRenameProvider("java", {
      provideRenameEdits: async (model, position, newName) => {
        if (!_conn || !isWorkspaceFile(model.uri.toString())) return { edits: [] };
        const edit = await _conn.sendRequest("textDocument/rename", {
          textDocument: { uri: model.uri.toString() },
          position: { line: position.lineNumber - 1, character: position.column - 1 },
          newName
        });
        if (!edit) return { edits: [] };
        if (typeof globalThis.javaLspPersistRename === "function") {
          try {
            await applyRenameAcrossWorkspace(model, edit);
            return { edits: [] };
          } catch (e) {
            console.error("[java-lsp] cross-file rename failed, applying to the current file only:", e);
            return workspaceEditToMonaco(edit);
          }
        }
        return workspaceEditToMonaco(edit);
      },
      resolveRenameLocation: async (model, position) => {
        if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
        try {
          const result = await _conn.sendRequest("textDocument/prepareRename", {
            textDocument: { uri: model.uri.toString() },
            position: { line: position.lineNumber - 1, character: position.column - 1 }
          });
          if (!result) return null;
          const range = "range" in result ? result.range : result;
          const placeholder = "placeholder" in result && result.placeholder ? result.placeholder : model.getWordAtPosition(position)?.word ?? "";
          return { range: lspRangeToMonaco(range), text: placeholder };
        } catch {
          const word = model.getWordAtPosition(position);
          return word ? {
            range: { startLineNumber: position.lineNumber, startColumn: word.startColumn, endLineNumber: position.lineNumber, endColumn: word.endColumn },
            text: word.word
          } : null;
        }
      }
    });
    languages.registerDocumentFormattingEditProvider("java", {
      provideDocumentFormattingEdits: async (model) => {
        if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
        const edits = await _conn.sendRequest("textDocument/formatting", {
          textDocument: { uri: model.uri.toString() },
          options: { tabSize: model.getOptions().tabSize, insertSpaces: model.getOptions().insertSpaces }
        });
        return edits ? edits.map(textEditToMonaco) : null;
      }
    });
    languages.registerCodeActionProvider("java", {
      provideCodeActions: async (model, range, context) => {
        const empty = { actions: [], dispose() {
        } };
        if (!_conn || !isWorkspaceFile(model.uri.toString())) return empty;
        const lspRange = monacoRangeToLsp(range);
        const diagnostics = (_diagnostics.get(model.uri.toString()) ?? []).filter((d) => rangesOverlap(d.range, lspRange));
        const result = await _conn.sendRequest("textDocument/codeAction", {
          textDocument: { uri: model.uri.toString() },
          range: lspRange,
          // Monaco's CodeActionTriggerType (Invoke=1, Auto=2) maps 1:1 to the LSP trigger kind.
          // Forwarding it lets JDT.LS compute only quick-fixes for the passive lightbulb (cheap)
          // and the full assists/refactorings only on explicit Ctrl+. / Refactor… (Invoked).
          context: { diagnostics, only: context.only ? [context.only] : void 0, triggerKind: context.trigger }
        });
        if (!result?.length) return empty;
        return {
          actions: result.map(lspCodeActionToMonaco),
          dispose() {
          }
        };
      }
    }, {
      providedCodeActionKinds: [
        "quickfix",
        "refactor",
        "refactor.extract",
        "refactor.inline",
        "refactor.rewrite",
        "source",
        "source.organizeImports"
      ]
    });
    languages.registerImplementationProvider("java", {
      provideImplementation: (model, position) => requestLocations("textDocument/implementation", model, position)
    });
    languages.registerTypeDefinitionProvider("java", {
      provideTypeDefinition: (model, position) => requestLocations("textDocument/typeDefinition", model, position)
    });
    languages.registerDocumentHighlightProvider("java", {
      provideDocumentHighlights: async (model, position) => {
        if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
        const result = await _conn.sendRequest("textDocument/documentHighlight", {
          textDocument: { uri: model.uri.toString() },
          position: { line: position.lineNumber - 1, character: position.column - 1 }
        });
        if (!result) return null;
        return result.map((h) => ({ range: lspRangeToMonaco(h.range), kind: h.kind ? h.kind - 1 : void 0 }));
      }
    });
    languages.registerDocumentSymbolProvider("java", {
      provideDocumentSymbols: async (model) => {
        if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
        const result = await _conn.sendRequest("textDocument/documentSymbol", {
          textDocument: { uri: model.uri.toString() }
        });
        return result ? mapDocumentSymbols(result) : null;
      }
    });
    languages.registerFoldingRangeProvider("java", {
      provideFoldingRanges: async (model) => {
        if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
        const result = await _conn.sendRequest("textDocument/foldingRange", {
          textDocument: { uri: model.uri.toString() }
        });
        if (!result) return null;
        return result.map((r) => ({ start: r.startLine + 1, end: r.endLine + 1, kind: foldingKind(r.kind) }));
      }
    });
    languages.registerSelectionRangeProvider("java", {
      provideSelectionRanges: async (model, positions) => {
        if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
        const result = await _conn.sendRequest("textDocument/selectionRange", {
          textDocument: { uri: model.uri.toString() },
          positions: positions.map((p) => ({ line: p.lineNumber - 1, character: p.column - 1 }))
        });
        if (!result) return null;
        return result.map(flattenSelectionRange);
      }
    });
    languages.registerDocumentRangeFormattingEditProvider("java", {
      provideDocumentRangeFormattingEdits: async (model, range) => {
        if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
        const edits = await _conn.sendRequest("textDocument/rangeFormatting", {
          textDocument: { uri: model.uri.toString() },
          range: monacoRangeToLsp(range),
          options: { tabSize: model.getOptions().tabSize, insertSpaces: model.getOptions().insertSpaces }
        });
        return edits ? edits.map(textEditToMonaco) : null;
      }
    });
    languages.registerInlayHintsProvider("java", {
      provideInlayHints: async (model, range) => {
        if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
        const result = await _conn.sendRequest("textDocument/inlayHint", {
          textDocument: { uri: model.uri.toString() },
          range: monacoRangeToLsp(range)
        });
        if (!result) return null;
        return {
          hints: result.map((h) => ({
            position: { lineNumber: h.position.line + 1, column: h.position.character + 1 },
            label: typeof h.label === "string" ? h.label : (h.label ?? []).map((p) => ({ label: p.value })),
            kind: h.kind,
            paddingLeft: h.paddingLeft,
            paddingRight: h.paddingRight,
            tooltip: h.tooltip ? markupToString(h.tooltip) : void 0
          })),
          dispose() {
          }
        };
      }
    });
    languages.registerDocumentSemanticTokensProvider("java", {
      getLegend: () => _semanticTokensLegend ?? { tokenTypes: [], tokenModifiers: [] },
      provideDocumentSemanticTokens: async (model) => {
        if (!_conn || !isWorkspaceFile(model.uri.toString()) || !_semanticTokensLegend) return null;
        const result = await _conn.sendRequest("textDocument/semanticTokens/full", {
          textDocument: { uri: model.uri.toString() }
        });
        if (!result?.data) return null;
        return { data: new Uint32Array(result.data), resultId: result.resultId };
      },
      releaseDocumentSemanticTokens: () => {
      }
    });
    languages.registerCodeLensProvider("java", {
      provideCodeLenses: async (model) => {
        if (!_conn || !isWorkspaceFile(model.uri.toString())) return { lenses: [], dispose() {
        } };
        const result = await _conn.sendRequest("textDocument/codeLens", {
          textDocument: { uri: model.uri.toString() }
        });
        const lenses = (result ?? []).map((lens, i) => ({
          range: lspRangeToMonaco(lens.range),
          id: String(i),
          command: lens.command ? mapLensCommand(lens.command) : void 0,
          _lsp: lens
        }));
        return { lenses, dispose() {
        } };
      },
      resolveCodeLens: async (_model, codeLens) => {
        const lsp = codeLens._lsp;
        if (_conn && lsp && !lsp.command) {
          try {
            const resolved = await _conn.sendRequest("codeLens/resolve", lsp);
            codeLens.command = resolved?.command ? mapLensCommand(resolved.command) : { id: NOOP_COMMAND, title: "" };
          } catch {
            codeLens.command = { id: NOOP_COMMAND, title: "" };
          }
        }
        return codeLens;
      }
    });
    languages.registerCompletionItemProvider("java", {
      provideCompletionItems: (model, position) => {
        if (!isWorkspaceFile(model.uri.toString())) return null;
        const word = model.getWordUntilPosition(position);
        const range = {
          startLineNumber: position.lineNumber,
          startColumn: word.startColumn,
          endLineNumber: position.lineNumber,
          endColumn: word.endColumn
        };
        return {
          suggestions: JAVA_KEYWORDS.map((keyword) => ({
            label: keyword,
            kind: languages.CompletionItemKind.Keyword,
            insertText: keyword,
            range,
            sortText: `9_${keyword}`
          }))
        };
      }
    });
  }
  function lspSeverity(severity) {
    switch (severity) {
      case DiagnosticSeverity.Error:
        return MarkerSeverity.Error;
      case DiagnosticSeverity.Warning:
        return MarkerSeverity.Warning;
      case DiagnosticSeverity.Information:
        return MarkerSeverity.Info;
      case DiagnosticSeverity.Hint:
        return MarkerSeverity.Hint;
      default:
        return MarkerSeverity.Error;
    }
  }
  function lspRangeToMonaco(r) {
    return {
      startLineNumber: r.start.line + 1,
      startColumn: r.start.character + 1,
      endLineNumber: r.end.line + 1,
      endColumn: r.end.character + 1
    };
  }
  function markupToString(c) {
    return typeof c === "string" ? c : c.value;
  }
  function lspCompletionKind(kind) {
    const K = languages.CompletionItemKind;
    switch (kind) {
      case CompletionItemKind.Text:
        return K.Text;
      case CompletionItemKind.Method:
        return K.Method;
      case CompletionItemKind.Function:
        return K.Function;
      case CompletionItemKind.Constructor:
        return K.Constructor;
      case CompletionItemKind.Field:
        return K.Field;
      case CompletionItemKind.Variable:
        return K.Variable;
      case CompletionItemKind.Class:
        return K.Class;
      case CompletionItemKind.Interface:
        return K.Interface;
      case CompletionItemKind.Module:
        return K.Module;
      case CompletionItemKind.Property:
        return K.Property;
      case CompletionItemKind.Keyword:
        return K.Keyword;
      case CompletionItemKind.Snippet:
        return K.Snippet;
      case CompletionItemKind.Constant:
        return K.Constant;
      case CompletionItemKind.Struct:
        return K.Struct;
      case CompletionItemKind.TypeParameter:
        return K.TypeParameter;
      default:
        return K.Text;
    }
  }
  function lspCompletionToMonaco(item, model, position) {
    const word = model.getWordUntilPosition(position);
    let range = {
      startLineNumber: position.lineNumber,
      startColumn: word.startColumn,
      endLineNumber: position.lineNumber,
      endColumn: word.endColumn
    };
    let insertText = item.insertText ?? item.label;
    const textEdit = item.textEdit;
    if (textEdit) {
      const r = textEdit.range ?? textEdit.replace ?? textEdit.insert;
      if (r) range = lspRangeToMonaco(r);
      if (typeof textEdit.newText === "string") insertText = textEdit.newText;
    }
    const result = {
      label: item.label,
      kind: lspCompletionKind(item.kind),
      detail: item.detail,
      documentation: item.documentation ? { value: markupToString(item.documentation), isTrusted: false } : void 0,
      insertText,
      insertTextRules: item.insertTextFormat === InsertTextFormat.Snippet ? languages.CompletionItemInsertTextRule.InsertAsSnippet : void 0,
      range,
      sortText: sdkPrioritisedSortText(item),
      filterText: item.filterText,
      preselect: item.preselect,
      commitCharacters: item.commitCharacters,
      additionalTextEdits: item.additionalTextEdits?.map(textEditToMonaco)
    };
    result._lsp = item;
    return result;
  }
  function sdkPrioritisedSortText(item) {
    const base = item.sortText ?? (typeof item.label === "string" ? item.label : "");
    const description = item.labelDetails && typeof item.labelDetails.description === "string" ? item.labelDetails.description : "";
    const haystack = `${item.detail ?? ""} ${description}`;
    return haystack.includes("org.eclipse.dirigible.sdk") ? `0_${base}` : `1_${base}`;
  }
  var APPLY_ACTION_COMMAND = "dirigible.java.applyCodeAction";
  var VIRTUAL_FILE_PREFIX = "file:///workspace";
  var NOOP_COMMAND = "dirigible.java.noopLens";
  var JAVA_KEYWORDS = [
    "abstract",
    "assert",
    "boolean",
    "break",
    "byte",
    "case",
    "catch",
    "char",
    "class",
    "const",
    "continue",
    "default",
    "do",
    "double",
    "else",
    "enum",
    "extends",
    "final",
    "finally",
    "float",
    "for",
    "goto",
    "if",
    "implements",
    "import",
    "instanceof",
    "int",
    "interface",
    "long",
    "native",
    "new",
    "package",
    "private",
    "protected",
    "public",
    "return",
    "short",
    "static",
    "strictfp",
    "super",
    "switch",
    "synchronized",
    "this",
    "throw",
    "throws",
    "transient",
    "try",
    "void",
    "volatile",
    "while",
    "var",
    "yield",
    "record",
    "sealed",
    "permits",
    "true",
    "false",
    "null"
  ];
  async function requestLocations(method, model, position) {
    if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
    const result = await _conn.sendRequest(method, {
      textDocument: { uri: model.uri.toString() },
      position: { line: position.lineNumber - 1, character: position.column - 1 }
    });
    if (!result) return null;
    const locations = (Array.isArray(result) ? result : [result]).map((loc) => ({ uri: Uri.parse(loc.uri), range: lspRangeToMonaco(loc.range) }));
    await ensureModelsForLocations(locations);
    return locations;
  }
  async function ensureModelsForLocations(locations) {
    const seen = /* @__PURE__ */ new Set();
    await Promise.all(locations.map(async ({ uri }) => {
      const uriStr = uri.toString();
      if (seen.has(uriStr) || !uriStr.startsWith(VIRTUAL_FILE_PREFIX) || editor.getModel(uri)) return;
      seen.add(uriStr);
      try {
        const text = await fetchWorkspaceFileText(uriToWorkspacePath(uriStr) ?? uriStr);
        if (!editor.getModel(uri)) editor.createModel(text, "java", uri);
      } catch {
      }
    }));
  }
  function mapDocumentSymbols(symbols) {
    return (symbols ?? []).map((s) => ({
      name: s.name,
      detail: s.detail ?? "",
      kind: (s.kind ?? 1) - 1,
      tags: s.tags ?? [],
      range: lspRangeToMonaco(s.range),
      selectionRange: lspRangeToMonaco(s.selectionRange ?? s.range),
      children: s.children ? mapDocumentSymbols(s.children) : []
    }));
  }
  function foldingKind(kind) {
    const FK = languages.FoldingRangeKind;
    switch (kind) {
      case "comment":
        return FK.Comment;
      case "imports":
        return FK.Imports;
      case "region":
        return FK.Region;
      default:
        return void 0;
    }
  }
  function flattenSelectionRange(selectionRange) {
    const ranges = [];
    let current = selectionRange;
    while (current) {
      ranges.push({ range: lspRangeToMonaco(current.range) });
      current = current.parent;
    }
    return ranges;
  }
  function mapLensCommand(cmd) {
    const args = cmd.arguments ?? [];
    if ((cmd.command === "java.show.references" || cmd.command === "java.show.implementations") && args.length >= 3) {
      const locations = (args[2] ?? []).map((l) => ({ uri: Uri.parse(l.uri), range: lspRangeToMonaco(l.range) }));
      return {
        id: "editor.action.showReferences",
        title: cmd.title,
        arguments: [Uri.parse(args[0]), { lineNumber: args[1].line + 1, column: args[1].character + 1 }, locations]
      };
    }
    return { id: NOOP_COMMAND, title: cmd.title };
  }
  function textEditToMonaco(edit) {
    return { range: lspRangeToMonaco(edit.range), text: edit.newText };
  }
  function monacoRangeToLsp(r) {
    return {
      start: { line: r.startLineNumber - 1, character: r.startColumn - 1 },
      end: { line: r.endLineNumber - 1, character: r.endColumn - 1 }
    };
  }
  function rangesOverlap(a, b) {
    const notAfter = (p, q) => p.line < q.line || p.line === q.line && p.character <= q.character;
    return notAfter(a.start, b.end) && notAfter(b.start, a.end);
  }
  function lspCodeActionToMonaco(action) {
    const isCommand = typeof action.command === "string";
    const title = action.title ?? (isCommand ? action.command : action.command?.title) ?? "Action";
    const kind = isCommand ? "quickfix" : action.kind ?? "quickfix";
    return {
      title,
      kind,
      diagnostics: [],
      isPreferred: action.isPreferred,
      // Apply lazily through our command so we can resolve, run server commands and apply edits uniformly.
      command: { id: APPLY_ACTION_COMMAND, title, arguments: [action] }
    };
  }
  async function applyCodeAction(action) {
    if (!_conn) return;
    try {
      if (typeof action.command === "string") {
        await runServerCommand(action);
        return;
      }
      let resolved = action;
      if (!resolved.edit && resolved.data !== void 0) {
        resolved = await _conn.sendRequest("codeAction/resolve", resolved);
      }
      if (resolved.edit) applyWorkspaceEdit(resolved.edit);
      if (resolved.command) await runServerCommand(resolved.command);
    } catch (e) {
      console.warn("[java-lsp] code action failed:", e?.message ?? e);
    }
  }
  function isWorkspaceEdit(value) {
    return !!value && (!!value.changes || !!value.documentChanges);
  }
  async function runServerCommand(cmd) {
    if (!_conn || !cmd?.command) return;
    if (GENERATE[cmd.command]) {
      await runGenerate(cmd.command, cmd.arguments ?? []);
      return;
    }
    const result = await _conn.sendRequest("workspace/executeCommand", { command: cmd.command, arguments: cmd.arguments ?? [] });
    if (isWorkspaceEdit(result)) applyWorkspaceEdit(result);
  }
  function fieldLabel(f) {
    const name = f?.name ?? f?.fieldName ?? "";
    const type = f?.type ?? f?.typeName;
    return type ? `${name}: ${type}` : `${name}`;
  }
  var GENERATE = {
    "java.action.generateConstructorsPrompt": {
      label: "Select fields and constructors",
      status: "java.action.checkConstructorsStatus",
      generate: "java.action.generateConstructors",
      members: (s) => (s?.fields ?? []).map((f) => ({ label: fieldLabel(f), ref: f })),
      buildArgs: (args, s, sel) => [args[0], { constructors: s?.constructors ?? [], fields: sel.map((m2) => m2.ref) }]
    },
    "java.action.generateToStringPrompt": {
      label: "Select fields to include in toString()",
      status: "java.action.checkToStringStatus",
      generate: "java.action.generateToString",
      members: (s) => (s?.fields ?? []).map((f) => ({ label: fieldLabel(f), ref: f })),
      buildArgs: (args, _s, sel) => [args[0], sel.map((m2) => m2.ref)]
    },
    "java.action.hashCodeEqualsPrompt": {
      label: "Select fields for hashCode() and equals()",
      status: "java.action.checkHashCodeEqualsStatus",
      generate: "java.action.generateHashCodeEquals",
      members: (s) => (s?.fields ?? []).map((f) => ({ label: fieldLabel(f), ref: f })),
      buildArgs: (args, _s, sel) => [args[0], sel.map((m2) => m2.ref), false]
    },
    "java.action.generateAccessorsPrompt": {
      label: "Select fields to generate getters and setters",
      status: "java.action.checkAccessorsStatus",
      generate: "java.action.generateAccessors",
      members: (s) => (s?.accessors ?? s ?? []).map((a) => ({ label: fieldLabel(a), ref: a })),
      buildArgs: (args, _s, sel) => [args[0], sel.map((m2) => m2.ref)]
    },
    "java.action.overrideMethodsPrompt": {
      label: "Select methods to override or implement",
      status: "java.action.listOverridableMethods",
      generate: "java.action.addOverridableMethods",
      members: (s) => (s?.methods ?? []).map((m2) => ({
        label: `${m2.name}(${(m2.parameters ?? []).join(", ")})${m2.declaringClass ? " : " + m2.declaringClass : ""}`,
        ref: m2
      })),
      buildArgs: (args, status, sel) => [args[0], { overridableMethods: sel.map((m2) => m2.ref), type: status?.type }]
    }
  };
  async function runGenerate(promptId, args) {
    if (!_conn) return;
    const spec = GENERATE[promptId];
    const status = await _conn.sendRequest("workspace/executeCommand", { command: spec.status, arguments: args });
    if (!status) return;
    const members = spec.members(status);
    let selected = members;
    const picker = globalThis.javaLspMemberPicker;
    if (members.length && typeof picker === "function") {
      const chosen = await picker(spec.label, members.map((m2) => m2.label));
      if (chosen === null) return;
      selected = members.filter((m2) => chosen.includes(m2.label));
    }
    const edit = await _conn.sendRequest("workspace/executeCommand", {
      command: spec.generate,
      arguments: spec.buildArgs(args, status, selected)
    });
    if (isWorkspaceEdit(edit)) applyWorkspaceEdit(edit);
  }
  function applyWorkspaceEdit(edit) {
    if (!edit) return;
    const byUri = {};
    if (edit.changes) {
      for (const uri in edit.changes) byUri[uri] = (byUri[uri] ?? []).concat(edit.changes[uri]);
    }
    if (edit.documentChanges) {
      for (const dc of edit.documentChanges) {
        if (dc?.textDocument?.uri && Array.isArray(dc.edits)) {
          byUri[dc.textDocument.uri] = (byUri[dc.textDocument.uri] ?? []).concat(dc.edits);
        }
      }
    }
    for (const uri in byUri) {
      const model = editor.getModels().find((m2) => m2.uri.toString() === uri);
      if (!model) continue;
      const ops = byUri[uri].map((e) => ({ range: lspRangeToMonaco(e.range), text: e.newText, forceMoveMarkers: true }));
      model.pushEditOperations([], ops, () => null);
    }
  }
  function workspaceEditToMonaco(edit) {
    const edits = [];
    const push = (uri, list) => {
      for (const e of list) {
        edits.push({ resource: Uri.parse(uri), textEdit: { range: lspRangeToMonaco(e.range), text: e.newText }, versionId: void 0 });
      }
    };
    if (edit?.changes) {
      for (const uri in edit.changes) push(uri, edit.changes[uri]);
    }
    if (edit?.documentChanges) {
      for (const dc of edit.documentChanges) {
        if (dc?.textDocument?.uri && Array.isArray(dc.edits)) push(dc.textDocument.uri, dc.edits);
      }
    }
    return { edits };
  }
  function uriToWorkspacePath(uri) {
    if (!uri.startsWith(VIRTUAL_FILE_PREFIX)) return null;
    return decodeURIComponent(uri.substring(VIRTUAL_FILE_PREFIX.length));
  }
  async function fetchWorkspaceFileText(idePath) {
    const response = await fetch("/services/ide/workspaces" + idePath, { headers: { "X-Requested-With": "Fetch" } });
    if (!response.ok) {
      throw new Error(`Could not read ${idePath} (HTTP ${response.status})`);
    }
    return response.text();
  }
  function applyEditsToText(text, edits) {
    const lineStarts = [0];
    for (let i = 0; i < text.length; i++) {
      if (text.charCodeAt(i) === 10) lineStarts.push(i + 1);
    }
    const offset = (p) => (lineStarts[p.line] ?? text.length) + p.character;
    const ordered = edits.slice().sort((a, b) => offset(b.range.start) - offset(a.range.start));
    let result = text;
    for (const e of ordered) {
      result = result.slice(0, offset(e.range.start)) + e.newText + result.slice(offset(e.range.end));
    }
    return result;
  }
  async function applyRenameAcrossWorkspace(model, edit) {
    const currentUri = model.uri.toString();
    const textByUri = {};
    const renameByUri = {};
    if (edit.changes) {
      for (const uri in edit.changes) textByUri[uri] = (textByUri[uri] ?? []).concat(edit.changes[uri]);
    }
    if (edit.documentChanges) {
      for (const dc of edit.documentChanges) {
        if (dc?.kind === "rename" && dc.oldUri && dc.newUri) {
          renameByUri[dc.oldUri] = dc.newUri;
        } else if (dc?.textDocument?.uri && Array.isArray(dc.edits)) {
          textByUri[dc.textDocument.uri] = (textByUri[dc.textDocument.uri] ?? []).concat(dc.edits);
        }
      }
    }
    const newToOld = {};
    for (const oldUri in renameByUri) newToOld[renameByUri[oldUri]] = oldUri;
    const editsByOld = {};
    for (const uri in textByUri) {
      const onDiskUri = newToOld[uri] ?? uri;
      editsByOld[onDiskUri] = (editsByOld[onDiskUri] ?? []).concat(textByUri[uri]);
    }
    const payload = { currentPath: uriToWorkspacePath(currentUri), currentContent: null, currentNewPath: null, writes: [], renames: [] };
    const watchedChanges = [];
    const oldUris = /* @__PURE__ */ new Set([...Object.keys(editsByOld), ...Object.keys(renameByUri)]);
    for (const oldUri of oldUris) {
      const edits = editsByOld[oldUri] ?? [];
      let content;
      if (oldUri === currentUri) {
        if (edits.length) {
          model.pushEditOperations([], edits.map((e) => ({ range: lspRangeToMonaco(e.range), text: e.newText, forceMoveMarkers: true })), () => null);
        }
        content = model.getValue();
      } else {
        const source = await fetchWorkspaceFileText(uriToWorkspacePath(oldUri) ?? oldUri);
        content = edits.length ? applyEditsToText(source, edits) : source;
      }
      const newUri = renameByUri[oldUri];
      if (oldUri === currentUri) {
        payload.currentContent = content;
        payload.currentNewPath = newUri ? uriToWorkspacePath(newUri) : null;
      } else if (newUri) {
        payload.renames.push({ oldPath: uriToWorkspacePath(oldUri), newPath: uriToWorkspacePath(newUri), content });
      } else {
        payload.writes.push({ path: uriToWorkspacePath(oldUri), content });
      }
      if (newUri) {
        watchedChanges.push({
          uri: oldUri,
          type: 3
          /* Deleted */
        }, {
          uri: newUri,
          type: 1
          /* Created */
        });
        _openFiles.delete(oldUri);
        _conn?.sendNotification("textDocument/didClose", { textDocument: { uri: oldUri } });
      } else {
        watchedChanges.push({
          uri: oldUri,
          type: 2
          /* Changed */
        });
      }
    }
    await globalThis.javaLspPersistRename(payload);
    if (_conn && watchedChanges.length) {
      _conn.sendNotification("workspace/didChangeWatchedFiles", { changes: watchedChanges });
    }
  }
  function jdtlsSettings() {
    return {
      java: {
        import: {
          maven: { enabled: true },
          gradle: { enabled: false },
          exclusions: ["**/node_modules/**", "**/.metadata/**", "**/archetype-resources/**"]
        },
        autobuild: { enabled: true },
        completion: {
          overwrite: true,
          guessMethodArguments: false,
          postfix: { enabled: true },
          filteredTypes: [
            "com.sun.*",
            "sun.*",
            "jdk.*",
            "org.eclipse.jdt.internal.*",
            "org.eclipse.core.internal.*",
            "org.eclipse.osgi.internal.*"
          ],
          importOrder: ["java", "javax", "org", "com", ""]
        },
        signatureHelp: { enabled: true },
        format: { enabled: true },
        saveActions: { organizeImports: false },
        inlayHints: { parameterNames: { enabled: "all" } },
        // Off by default: the reference/implementation search behind these CodeLenses runs for every
        // declaration on open and on every edit and dominates JDT.LS load on a large classpath.
        referencesCodeLens: { enabled: false },
        implementationsCodeLens: { enabled: false }
      }
    };
  }
  return __toCommonJS(java_lsp_client_exports);
})();
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtanNvbnJwYy9saWIvY29tbW9uL2lzLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9tZXNzYWdlcy5qcyIsICJsc3Avbm9kZV9tb2R1bGVzL3ZzY29kZS1qc29ucnBjL2xpYi9jb21tb24vbGlua2VkTWFwLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9kaXNwb3NhYmxlLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9yYWwuanMiLCAibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtanNvbnJwYy9saWIvY29tbW9uL2V2ZW50cy5qcyIsICJsc3Avbm9kZV9tb2R1bGVzL3ZzY29kZS1qc29ucnBjL2xpYi9jb21tb24vY2FuY2VsbGF0aW9uLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9zaGFyZWRBcnJheUNhbmNlbGxhdGlvbi5qcyIsICJsc3Avbm9kZV9tb2R1bGVzL3ZzY29kZS1qc29ucnBjL2xpYi9jb21tb24vc2VtYXBob3JlLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9tZXNzYWdlUmVhZGVyLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9tZXNzYWdlV3JpdGVyLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9tZXNzYWdlQnVmZmVyLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9jb25uZWN0aW9uLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9hcGkuanMiLCAibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtanNvbnJwYy9saWIvYnJvd3Nlci9yaWwuanMiLCAibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtanNvbnJwYy9saWIvYnJvd3Nlci9tYWluLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvYnJvd3Nlci5qcyIsICJsc3AvamF2YS1sc3AtY2xpZW50LnRzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLXdzLWpzb25ycGMvc3JjL2Rpc3Bvc2FibGUudHMiLCAibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtd3MtanNvbnJwYy9zcmMvc29ja2V0L3NvY2tldC50cyIsICJsc3Avbm9kZV9tb2R1bGVzL3ZzY29kZS13cy1qc29ucnBjL3NyYy9zb2NrZXQvcmVhZGVyLnRzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLXdzLWpzb25ycGMvc3JjL3NvY2tldC93cml0ZXIudHMiLCAibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtd3MtanNvbnJwYy9zcmMvc29ja2V0L2Nvbm5lY3Rpb24udHMiLCAibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtd3MtanNvbnJwYy9zcmMvY29ubmVjdGlvbi50cyIsICJsc3AvbW9uYWNvLXNoaW0udHMiLCAibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtbGFuZ3VhZ2VzZXJ2ZXItdHlwZXMvbGliL2VzbS9tYWluLmpzIl0sCiAgInNvdXJjZXNDb250ZW50IjogWyJcInVzZSBzdHJpY3RcIjtcbi8qIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiBDb3B5cmlnaHQgKGMpIE1pY3Jvc29mdCBDb3Jwb3JhdGlvbi4gQWxsIHJpZ2h0cyByZXNlcnZlZC5cbiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBNSVQgTGljZW5zZS4gU2VlIExpY2Vuc2UudHh0IGluIHRoZSBwcm9qZWN0IHJvb3QgZm9yIGxpY2Vuc2UgaW5mb3JtYXRpb24uXG4gKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0gKi9cbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIl9fZXNNb2R1bGVcIiwgeyB2YWx1ZTogdHJ1ZSB9KTtcbmV4cG9ydHMuc3RyaW5nQXJyYXkgPSBleHBvcnRzLmFycmF5ID0gZXhwb3J0cy5mdW5jID0gZXhwb3J0cy5lcnJvciA9IGV4cG9ydHMubnVtYmVyID0gZXhwb3J0cy5zdHJpbmcgPSBleHBvcnRzLmJvb2xlYW4gPSB2b2lkIDA7XG5mdW5jdGlvbiBib29sZWFuKHZhbHVlKSB7XG4gICAgcmV0dXJuIHZhbHVlID09PSB0cnVlIHx8IHZhbHVlID09PSBmYWxzZTtcbn1cbmV4cG9ydHMuYm9vbGVhbiA9IGJvb2xlYW47XG5mdW5jdGlvbiBzdHJpbmcodmFsdWUpIHtcbiAgICByZXR1cm4gdHlwZW9mIHZhbHVlID09PSAnc3RyaW5nJyB8fCB2YWx1ZSBpbnN0YW5jZW9mIFN0cmluZztcbn1cbmV4cG9ydHMuc3RyaW5nID0gc3RyaW5nO1xuZnVuY3Rpb24gbnVtYmVyKHZhbHVlKSB7XG4gICAgcmV0dXJuIHR5cGVvZiB2YWx1ZSA9PT0gJ251bWJlcicgfHwgdmFsdWUgaW5zdGFuY2VvZiBOdW1iZXI7XG59XG5leHBvcnRzLm51bWJlciA9IG51bWJlcjtcbmZ1bmN0aW9uIGVycm9yKHZhbHVlKSB7XG4gICAgcmV0dXJuIHZhbHVlIGluc3RhbmNlb2YgRXJyb3I7XG59XG5leHBvcnRzLmVycm9yID0gZXJyb3I7XG5mdW5jdGlvbiBmdW5jKHZhbHVlKSB7XG4gICAgcmV0dXJuIHR5cGVvZiB2YWx1ZSA9PT0gJ2Z1bmN0aW9uJztcbn1cbmV4cG9ydHMuZnVuYyA9IGZ1bmM7XG5mdW5jdGlvbiBhcnJheSh2YWx1ZSkge1xuICAgIHJldHVybiBBcnJheS5pc0FycmF5KHZhbHVlKTtcbn1cbmV4cG9ydHMuYXJyYXkgPSBhcnJheTtcbmZ1bmN0aW9uIHN0cmluZ0FycmF5KHZhbHVlKSB7XG4gICAgcmV0dXJuIGFycmF5KHZhbHVlKSAmJiB2YWx1ZS5ldmVyeShlbGVtID0+IHN0cmluZyhlbGVtKSk7XG59XG5leHBvcnRzLnN0cmluZ0FycmF5ID0gc3RyaW5nQXJyYXk7XG4iLCAiXCJ1c2Ugc3RyaWN0XCI7XG4vKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJfX2VzTW9kdWxlXCIsIHsgdmFsdWU6IHRydWUgfSk7XG5leHBvcnRzLk1lc3NhZ2UgPSBleHBvcnRzLk5vdGlmaWNhdGlvblR5cGU5ID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlOCA9IGV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTcgPSBleHBvcnRzLk5vdGlmaWNhdGlvblR5cGU2ID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlNSA9IGV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTQgPSBleHBvcnRzLk5vdGlmaWNhdGlvblR5cGUzID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlMiA9IGV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTEgPSBleHBvcnRzLk5vdGlmaWNhdGlvblR5cGUwID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlID0gZXhwb3J0cy5SZXF1ZXN0VHlwZTkgPSBleHBvcnRzLlJlcXVlc3RUeXBlOCA9IGV4cG9ydHMuUmVxdWVzdFR5cGU3ID0gZXhwb3J0cy5SZXF1ZXN0VHlwZTYgPSBleHBvcnRzLlJlcXVlc3RUeXBlNSA9IGV4cG9ydHMuUmVxdWVzdFR5cGU0ID0gZXhwb3J0cy5SZXF1ZXN0VHlwZTMgPSBleHBvcnRzLlJlcXVlc3RUeXBlMiA9IGV4cG9ydHMuUmVxdWVzdFR5cGUxID0gZXhwb3J0cy5SZXF1ZXN0VHlwZSA9IGV4cG9ydHMuUmVxdWVzdFR5cGUwID0gZXhwb3J0cy5BYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUgPSBleHBvcnRzLlBhcmFtZXRlclN0cnVjdHVyZXMgPSBleHBvcnRzLlJlc3BvbnNlRXJyb3IgPSBleHBvcnRzLkVycm9yQ29kZXMgPSB2b2lkIDA7XG5jb25zdCBpcyA9IHJlcXVpcmUoXCIuL2lzXCIpO1xuLyoqXG4gKiBQcmVkZWZpbmVkIGVycm9yIGNvZGVzLlxuICovXG52YXIgRXJyb3JDb2RlcztcbihmdW5jdGlvbiAoRXJyb3JDb2Rlcykge1xuICAgIC8vIERlZmluZWQgYnkgSlNPTiBSUENcbiAgICBFcnJvckNvZGVzLlBhcnNlRXJyb3IgPSAtMzI3MDA7XG4gICAgRXJyb3JDb2Rlcy5JbnZhbGlkUmVxdWVzdCA9IC0zMjYwMDtcbiAgICBFcnJvckNvZGVzLk1ldGhvZE5vdEZvdW5kID0gLTMyNjAxO1xuICAgIEVycm9yQ29kZXMuSW52YWxpZFBhcmFtcyA9IC0zMjYwMjtcbiAgICBFcnJvckNvZGVzLkludGVybmFsRXJyb3IgPSAtMzI2MDM7XG4gICAgLyoqXG4gICAgICogVGhpcyBpcyB0aGUgc3RhcnQgcmFuZ2Ugb2YgSlNPTiBSUEMgcmVzZXJ2ZWQgZXJyb3IgY29kZXMuXG4gICAgICogSXQgZG9lc24ndCBkZW5vdGUgYSByZWFsIGVycm9yIGNvZGUuIE5vIGFwcGxpY2F0aW9uIGVycm9yIGNvZGVzIHNob3VsZFxuICAgICAqIGJlIGRlZmluZWQgYmV0d2VlbiB0aGUgc3RhcnQgYW5kIGVuZCByYW5nZS4gRm9yIGJhY2t3YXJkc1xuICAgICAqIGNvbXBhdGliaWxpdHkgdGhlIGBTZXJ2ZXJOb3RJbml0aWFsaXplZGAgYW5kIHRoZSBgVW5rbm93bkVycm9yQ29kZWBcbiAgICAgKiBhcmUgbGVmdCBpbiB0aGUgcmFuZ2UuXG4gICAgICpcbiAgICAgKiBAc2luY2UgMy4xNi4wXG4gICAgKi9cbiAgICBFcnJvckNvZGVzLmpzb25ycGNSZXNlcnZlZEVycm9yUmFuZ2VTdGFydCA9IC0zMjA5OTtcbiAgICAvKiogQGRlcHJlY2F0ZWQgdXNlICBqc29ucnBjUmVzZXJ2ZWRFcnJvclJhbmdlU3RhcnQgKi9cbiAgICBFcnJvckNvZGVzLnNlcnZlckVycm9yU3RhcnQgPSAtMzIwOTk7XG4gICAgLyoqXG4gICAgICogQW4gZXJyb3Igb2NjdXJyZWQgd2hlbiB3cml0ZSBhIG1lc3NhZ2UgdG8gdGhlIHRyYW5zcG9ydCBsYXllci5cbiAgICAgKi9cbiAgICBFcnJvckNvZGVzLk1lc3NhZ2VXcml0ZUVycm9yID0gLTMyMDk5O1xuICAgIC8qKlxuICAgICAqIEFuIGVycm9yIG9jY3VycmVkIHdoZW4gcmVhZGluZyBhIG1lc3NhZ2UgZnJvbSB0aGUgdHJhbnNwb3J0IGxheWVyLlxuICAgICAqL1xuICAgIEVycm9yQ29kZXMuTWVzc2FnZVJlYWRFcnJvciA9IC0zMjA5ODtcbiAgICAvKipcbiAgICAgKiBUaGUgY29ubmVjdGlvbiBnb3QgZGlzcG9zZWQgb3IgbG9zdCBhbmQgYWxsIHBlbmRpbmcgcmVzcG9uc2VzIGdvdFxuICAgICAqIHJlamVjdGVkLlxuICAgICAqL1xuICAgIEVycm9yQ29kZXMuUGVuZGluZ1Jlc3BvbnNlUmVqZWN0ZWQgPSAtMzIwOTc7XG4gICAgLyoqXG4gICAgICogVGhlIGNvbm5lY3Rpb24gaXMgaW5hY3RpdmUgYW5kIGEgdXNlIG9mIGl0IGZhaWxlZC5cbiAgICAgKi9cbiAgICBFcnJvckNvZGVzLkNvbm5lY3Rpb25JbmFjdGl2ZSA9IC0zMjA5NjtcbiAgICAvKipcbiAgICAgKiBFcnJvciBjb2RlIGluZGljYXRpbmcgdGhhdCBhIHNlcnZlciByZWNlaXZlZCBhIG5vdGlmaWNhdGlvbiBvclxuICAgICAqIHJlcXVlc3QgYmVmb3JlIHRoZSBzZXJ2ZXIgaGFzIHJlY2VpdmVkIHRoZSBgaW5pdGlhbGl6ZWAgcmVxdWVzdC5cbiAgICAgKi9cbiAgICBFcnJvckNvZGVzLlNlcnZlck5vdEluaXRpYWxpemVkID0gLTMyMDAyO1xuICAgIEVycm9yQ29kZXMuVW5rbm93bkVycm9yQ29kZSA9IC0zMjAwMTtcbiAgICAvKipcbiAgICAgKiBUaGlzIGlzIHRoZSBlbmQgcmFuZ2Ugb2YgSlNPTiBSUEMgcmVzZXJ2ZWQgZXJyb3IgY29kZXMuXG4gICAgICogSXQgZG9lc24ndCBkZW5vdGUgYSByZWFsIGVycm9yIGNvZGUuXG4gICAgICpcbiAgICAgKiBAc2luY2UgMy4xNi4wXG4gICAgKi9cbiAgICBFcnJvckNvZGVzLmpzb25ycGNSZXNlcnZlZEVycm9yUmFuZ2VFbmQgPSAtMzIwMDA7XG4gICAgLyoqIEBkZXByZWNhdGVkIHVzZSAganNvbnJwY1Jlc2VydmVkRXJyb3JSYW5nZUVuZCAqL1xuICAgIEVycm9yQ29kZXMuc2VydmVyRXJyb3JFbmQgPSAtMzIwMDA7XG59KShFcnJvckNvZGVzIHx8IChleHBvcnRzLkVycm9yQ29kZXMgPSBFcnJvckNvZGVzID0ge30pKTtcbi8qKlxuICogQW4gZXJyb3Igb2JqZWN0IHJldHVybiBpbiBhIHJlc3BvbnNlIGluIGNhc2UgYSByZXF1ZXN0XG4gKiBoYXMgZmFpbGVkLlxuICovXG5jbGFzcyBSZXNwb25zZUVycm9yIGV4dGVuZHMgRXJyb3Ige1xuICAgIGNvbnN0cnVjdG9yKGNvZGUsIG1lc3NhZ2UsIGRhdGEpIHtcbiAgICAgICAgc3VwZXIobWVzc2FnZSk7XG4gICAgICAgIHRoaXMuY29kZSA9IGlzLm51bWJlcihjb2RlKSA/IGNvZGUgOiBFcnJvckNvZGVzLlVua25vd25FcnJvckNvZGU7XG4gICAgICAgIHRoaXMuZGF0YSA9IGRhdGE7XG4gICAgICAgIE9iamVjdC5zZXRQcm90b3R5cGVPZih0aGlzLCBSZXNwb25zZUVycm9yLnByb3RvdHlwZSk7XG4gICAgfVxuICAgIHRvSnNvbigpIHtcbiAgICAgICAgY29uc3QgcmVzdWx0ID0ge1xuICAgICAgICAgICAgY29kZTogdGhpcy5jb2RlLFxuICAgICAgICAgICAgbWVzc2FnZTogdGhpcy5tZXNzYWdlXG4gICAgICAgIH07XG4gICAgICAgIGlmICh0aGlzLmRhdGEgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmVzdWx0LmRhdGEgPSB0aGlzLmRhdGE7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICB9XG59XG5leHBvcnRzLlJlc3BvbnNlRXJyb3IgPSBSZXNwb25zZUVycm9yO1xuY2xhc3MgUGFyYW1ldGVyU3RydWN0dXJlcyB7XG4gICAgY29uc3RydWN0b3Ioa2luZCkge1xuICAgICAgICB0aGlzLmtpbmQgPSBraW5kO1xuICAgIH1cbiAgICBzdGF0aWMgaXModmFsdWUpIHtcbiAgICAgICAgcmV0dXJuIHZhbHVlID09PSBQYXJhbWV0ZXJTdHJ1Y3R1cmVzLmF1dG8gfHwgdmFsdWUgPT09IFBhcmFtZXRlclN0cnVjdHVyZXMuYnlOYW1lIHx8IHZhbHVlID09PSBQYXJhbWV0ZXJTdHJ1Y3R1cmVzLmJ5UG9zaXRpb247XG4gICAgfVxuICAgIHRvU3RyaW5nKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5raW5kO1xuICAgIH1cbn1cbmV4cG9ydHMuUGFyYW1ldGVyU3RydWN0dXJlcyA9IFBhcmFtZXRlclN0cnVjdHVyZXM7XG4vKipcbiAqIFRoZSBwYXJhbWV0ZXIgc3RydWN0dXJlIGlzIGF1dG9tYXRpY2FsbHkgaW5mZXJyZWQgb24gdGhlIG51bWJlciBvZiBwYXJhbWV0ZXJzXG4gKiBhbmQgdGhlIHBhcmFtZXRlciB0eXBlIGluIGNhc2Ugb2YgYSBzaW5nbGUgcGFyYW0uXG4gKi9cblBhcmFtZXRlclN0cnVjdHVyZXMuYXV0byA9IG5ldyBQYXJhbWV0ZXJTdHJ1Y3R1cmVzKCdhdXRvJyk7XG4vKipcbiAqIEZvcmNlcyBgYnlQb3NpdGlvbmAgcGFyYW1ldGVyIHN0cnVjdHVyZS4gVGhpcyBpcyB1c2VmdWwgaWYgeW91IGhhdmUgYSBzaW5nbGVcbiAqIHBhcmFtZXRlciB3aGljaCBoYXMgYSBsaXRlcmFsIHR5cGUuXG4gKi9cblBhcmFtZXRlclN0cnVjdHVyZXMuYnlQb3NpdGlvbiA9IG5ldyBQYXJhbWV0ZXJTdHJ1Y3R1cmVzKCdieVBvc2l0aW9uJyk7XG4vKipcbiAqIEZvcmNlcyBgYnlOYW1lYCBwYXJhbWV0ZXIgc3RydWN0dXJlLiBUaGlzIGlzIG9ubHkgdXNlZnVsIHdoZW4gaGF2aW5nIGEgc2luZ2xlXG4gKiBwYXJhbWV0ZXIuIFRoZSBsaWJyYXJ5IHdpbGwgcmVwb3J0IGVycm9ycyBpZiB1c2VkIHdpdGggYSBkaWZmZXJlbnQgbnVtYmVyIG9mXG4gKiBwYXJhbWV0ZXJzLlxuICovXG5QYXJhbWV0ZXJTdHJ1Y3R1cmVzLmJ5TmFtZSA9IG5ldyBQYXJhbWV0ZXJTdHJ1Y3R1cmVzKCdieU5hbWUnKTtcbi8qKlxuICogQW4gYWJzdHJhY3QgaW1wbGVtZW50YXRpb24gb2YgYSBNZXNzYWdlVHlwZS5cbiAqL1xuY2xhc3MgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QsIG51bWJlck9mUGFyYW1zKSB7XG4gICAgICAgIHRoaXMubWV0aG9kID0gbWV0aG9kO1xuICAgICAgICB0aGlzLm51bWJlck9mUGFyYW1zID0gbnVtYmVyT2ZQYXJhbXM7XG4gICAgfVxuICAgIGdldCBwYXJhbWV0ZXJTdHJ1Y3R1cmVzKCkge1xuICAgICAgICByZXR1cm4gUGFyYW1ldGVyU3RydWN0dXJlcy5hdXRvO1xuICAgIH1cbn1cbmV4cG9ydHMuQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlID0gQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlO1xuLyoqXG4gKiBDbGFzc2VzIHRvIHR5cGUgcmVxdWVzdCByZXNwb25zZSBwYWlyc1xuICovXG5jbGFzcyBSZXF1ZXN0VHlwZTAgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCkge1xuICAgICAgICBzdXBlcihtZXRob2QsIDApO1xuICAgIH1cbn1cbmV4cG9ydHMuUmVxdWVzdFR5cGUwID0gUmVxdWVzdFR5cGUwO1xuY2xhc3MgUmVxdWVzdFR5cGUgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCwgX3BhcmFtZXRlclN0cnVjdHVyZXMgPSBQYXJhbWV0ZXJTdHJ1Y3R1cmVzLmF1dG8pIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCAxKTtcbiAgICAgICAgdGhpcy5fcGFyYW1ldGVyU3RydWN0dXJlcyA9IF9wYXJhbWV0ZXJTdHJ1Y3R1cmVzO1xuICAgIH1cbiAgICBnZXQgcGFyYW1ldGVyU3RydWN0dXJlcygpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX3BhcmFtZXRlclN0cnVjdHVyZXM7XG4gICAgfVxufVxuZXhwb3J0cy5SZXF1ZXN0VHlwZSA9IFJlcXVlc3RUeXBlO1xuY2xhc3MgUmVxdWVzdFR5cGUxIGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QsIF9wYXJhbWV0ZXJTdHJ1Y3R1cmVzID0gUGFyYW1ldGVyU3RydWN0dXJlcy5hdXRvKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgMSk7XG4gICAgICAgIHRoaXMuX3BhcmFtZXRlclN0cnVjdHVyZXMgPSBfcGFyYW1ldGVyU3RydWN0dXJlcztcbiAgICB9XG4gICAgZ2V0IHBhcmFtZXRlclN0cnVjdHVyZXMoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLl9wYXJhbWV0ZXJTdHJ1Y3R1cmVzO1xuICAgIH1cbn1cbmV4cG9ydHMuUmVxdWVzdFR5cGUxID0gUmVxdWVzdFR5cGUxO1xuY2xhc3MgUmVxdWVzdFR5cGUyIGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QpIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCAyKTtcbiAgICB9XG59XG5leHBvcnRzLlJlcXVlc3RUeXBlMiA9IFJlcXVlc3RUeXBlMjtcbmNsYXNzIFJlcXVlc3RUeXBlMyBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVNpZ25hdHVyZSB7XG4gICAgY29uc3RydWN0b3IobWV0aG9kKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgMyk7XG4gICAgfVxufVxuZXhwb3J0cy5SZXF1ZXN0VHlwZTMgPSBSZXF1ZXN0VHlwZTM7XG5jbGFzcyBSZXF1ZXN0VHlwZTQgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCkge1xuICAgICAgICBzdXBlcihtZXRob2QsIDQpO1xuICAgIH1cbn1cbmV4cG9ydHMuUmVxdWVzdFR5cGU0ID0gUmVxdWVzdFR5cGU0O1xuY2xhc3MgUmVxdWVzdFR5cGU1IGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QpIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCA1KTtcbiAgICB9XG59XG5leHBvcnRzLlJlcXVlc3RUeXBlNSA9IFJlcXVlc3RUeXBlNTtcbmNsYXNzIFJlcXVlc3RUeXBlNiBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVNpZ25hdHVyZSB7XG4gICAgY29uc3RydWN0b3IobWV0aG9kKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgNik7XG4gICAgfVxufVxuZXhwb3J0cy5SZXF1ZXN0VHlwZTYgPSBSZXF1ZXN0VHlwZTY7XG5jbGFzcyBSZXF1ZXN0VHlwZTcgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCkge1xuICAgICAgICBzdXBlcihtZXRob2QsIDcpO1xuICAgIH1cbn1cbmV4cG9ydHMuUmVxdWVzdFR5cGU3ID0gUmVxdWVzdFR5cGU3O1xuY2xhc3MgUmVxdWVzdFR5cGU4IGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QpIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCA4KTtcbiAgICB9XG59XG5leHBvcnRzLlJlcXVlc3RUeXBlOCA9IFJlcXVlc3RUeXBlODtcbmNsYXNzIFJlcXVlc3RUeXBlOSBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVNpZ25hdHVyZSB7XG4gICAgY29uc3RydWN0b3IobWV0aG9kKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgOSk7XG4gICAgfVxufVxuZXhwb3J0cy5SZXF1ZXN0VHlwZTkgPSBSZXF1ZXN0VHlwZTk7XG5jbGFzcyBOb3RpZmljYXRpb25UeXBlIGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QsIF9wYXJhbWV0ZXJTdHJ1Y3R1cmVzID0gUGFyYW1ldGVyU3RydWN0dXJlcy5hdXRvKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgMSk7XG4gICAgICAgIHRoaXMuX3BhcmFtZXRlclN0cnVjdHVyZXMgPSBfcGFyYW1ldGVyU3RydWN0dXJlcztcbiAgICB9XG4gICAgZ2V0IHBhcmFtZXRlclN0cnVjdHVyZXMoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLl9wYXJhbWV0ZXJTdHJ1Y3R1cmVzO1xuICAgIH1cbn1cbmV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZSA9IE5vdGlmaWNhdGlvblR5cGU7XG5jbGFzcyBOb3RpZmljYXRpb25UeXBlMCBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVNpZ25hdHVyZSB7XG4gICAgY29uc3RydWN0b3IobWV0aG9kKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgMCk7XG4gICAgfVxufVxuZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlMCA9IE5vdGlmaWNhdGlvblR5cGUwO1xuY2xhc3MgTm90aWZpY2F0aW9uVHlwZTEgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCwgX3BhcmFtZXRlclN0cnVjdHVyZXMgPSBQYXJhbWV0ZXJTdHJ1Y3R1cmVzLmF1dG8pIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCAxKTtcbiAgICAgICAgdGhpcy5fcGFyYW1ldGVyU3RydWN0dXJlcyA9IF9wYXJhbWV0ZXJTdHJ1Y3R1cmVzO1xuICAgIH1cbiAgICBnZXQgcGFyYW1ldGVyU3RydWN0dXJlcygpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX3BhcmFtZXRlclN0cnVjdHVyZXM7XG4gICAgfVxufVxuZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlMSA9IE5vdGlmaWNhdGlvblR5cGUxO1xuY2xhc3MgTm90aWZpY2F0aW9uVHlwZTIgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCkge1xuICAgICAgICBzdXBlcihtZXRob2QsIDIpO1xuICAgIH1cbn1cbmV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTIgPSBOb3RpZmljYXRpb25UeXBlMjtcbmNsYXNzIE5vdGlmaWNhdGlvblR5cGUzIGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QpIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCAzKTtcbiAgICB9XG59XG5leHBvcnRzLk5vdGlmaWNhdGlvblR5cGUzID0gTm90aWZpY2F0aW9uVHlwZTM7XG5jbGFzcyBOb3RpZmljYXRpb25UeXBlNCBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVNpZ25hdHVyZSB7XG4gICAgY29uc3RydWN0b3IobWV0aG9kKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgNCk7XG4gICAgfVxufVxuZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlNCA9IE5vdGlmaWNhdGlvblR5cGU0O1xuY2xhc3MgTm90aWZpY2F0aW9uVHlwZTUgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCkge1xuICAgICAgICBzdXBlcihtZXRob2QsIDUpO1xuICAgIH1cbn1cbmV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTUgPSBOb3RpZmljYXRpb25UeXBlNTtcbmNsYXNzIE5vdGlmaWNhdGlvblR5cGU2IGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QpIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCA2KTtcbiAgICB9XG59XG5leHBvcnRzLk5vdGlmaWNhdGlvblR5cGU2ID0gTm90aWZpY2F0aW9uVHlwZTY7XG5jbGFzcyBOb3RpZmljYXRpb25UeXBlNyBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVNpZ25hdHVyZSB7XG4gICAgY29uc3RydWN0b3IobWV0aG9kKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgNyk7XG4gICAgfVxufVxuZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlNyA9IE5vdGlmaWNhdGlvblR5cGU3O1xuY2xhc3MgTm90aWZpY2F0aW9uVHlwZTggZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCkge1xuICAgICAgICBzdXBlcihtZXRob2QsIDgpO1xuICAgIH1cbn1cbmV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTggPSBOb3RpZmljYXRpb25UeXBlODtcbmNsYXNzIE5vdGlmaWNhdGlvblR5cGU5IGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QpIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCA5KTtcbiAgICB9XG59XG5leHBvcnRzLk5vdGlmaWNhdGlvblR5cGU5ID0gTm90aWZpY2F0aW9uVHlwZTk7XG52YXIgTWVzc2FnZTtcbihmdW5jdGlvbiAoTWVzc2FnZSkge1xuICAgIC8qKlxuICAgICAqIFRlc3RzIGlmIHRoZSBnaXZlbiBtZXNzYWdlIGlzIGEgcmVxdWVzdCBtZXNzYWdlXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXNSZXF1ZXN0KG1lc3NhZ2UpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gbWVzc2FnZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiBpcy5zdHJpbmcoY2FuZGlkYXRlLm1ldGhvZCkgJiYgKGlzLnN0cmluZyhjYW5kaWRhdGUuaWQpIHx8IGlzLm51bWJlcihjYW5kaWRhdGUuaWQpKTtcbiAgICB9XG4gICAgTWVzc2FnZS5pc1JlcXVlc3QgPSBpc1JlcXVlc3Q7XG4gICAgLyoqXG4gICAgICogVGVzdHMgaWYgdGhlIGdpdmVuIG1lc3NhZ2UgaXMgYSBub3RpZmljYXRpb24gbWVzc2FnZVxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzTm90aWZpY2F0aW9uKG1lc3NhZ2UpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gbWVzc2FnZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiBpcy5zdHJpbmcoY2FuZGlkYXRlLm1ldGhvZCkgJiYgbWVzc2FnZS5pZCA9PT0gdm9pZCAwO1xuICAgIH1cbiAgICBNZXNzYWdlLmlzTm90aWZpY2F0aW9uID0gaXNOb3RpZmljYXRpb247XG4gICAgLyoqXG4gICAgICogVGVzdHMgaWYgdGhlIGdpdmVuIG1lc3NhZ2UgaXMgYSByZXNwb25zZSBtZXNzYWdlXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXNSZXNwb25zZShtZXNzYWdlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IG1lc3NhZ2U7XG4gICAgICAgIHJldHVybiBjYW5kaWRhdGUgJiYgKGNhbmRpZGF0ZS5yZXN1bHQgIT09IHZvaWQgMCB8fCAhIWNhbmRpZGF0ZS5lcnJvcikgJiYgKGlzLnN0cmluZyhjYW5kaWRhdGUuaWQpIHx8IGlzLm51bWJlcihjYW5kaWRhdGUuaWQpIHx8IGNhbmRpZGF0ZS5pZCA9PT0gbnVsbCk7XG4gICAgfVxuICAgIE1lc3NhZ2UuaXNSZXNwb25zZSA9IGlzUmVzcG9uc2U7XG59KShNZXNzYWdlIHx8IChleHBvcnRzLk1lc3NhZ2UgPSBNZXNzYWdlID0ge30pKTtcbiIsICJcInVzZSBzdHJpY3RcIjtcbi8qLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiAgQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiAgTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTGljZW5zZS50eHQgaW4gdGhlIHByb2plY3Qgcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0qL1xudmFyIF9hO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiX19lc01vZHVsZVwiLCB7IHZhbHVlOiB0cnVlIH0pO1xuZXhwb3J0cy5MUlVDYWNoZSA9IGV4cG9ydHMuTGlua2VkTWFwID0gZXhwb3J0cy5Ub3VjaCA9IHZvaWQgMDtcbnZhciBUb3VjaDtcbihmdW5jdGlvbiAoVG91Y2gpIHtcbiAgICBUb3VjaC5Ob25lID0gMDtcbiAgICBUb3VjaC5GaXJzdCA9IDE7XG4gICAgVG91Y2guQXNPbGQgPSBUb3VjaC5GaXJzdDtcbiAgICBUb3VjaC5MYXN0ID0gMjtcbiAgICBUb3VjaC5Bc05ldyA9IFRvdWNoLkxhc3Q7XG59KShUb3VjaCB8fCAoZXhwb3J0cy5Ub3VjaCA9IFRvdWNoID0ge30pKTtcbmNsYXNzIExpbmtlZE1hcCB7XG4gICAgY29uc3RydWN0b3IoKSB7XG4gICAgICAgIHRoaXNbX2FdID0gJ0xpbmtlZE1hcCc7XG4gICAgICAgIHRoaXMuX21hcCA9IG5ldyBNYXAoKTtcbiAgICAgICAgdGhpcy5faGVhZCA9IHVuZGVmaW5lZDtcbiAgICAgICAgdGhpcy5fdGFpbCA9IHVuZGVmaW5lZDtcbiAgICAgICAgdGhpcy5fc2l6ZSA9IDA7XG4gICAgICAgIHRoaXMuX3N0YXRlID0gMDtcbiAgICB9XG4gICAgY2xlYXIoKSB7XG4gICAgICAgIHRoaXMuX21hcC5jbGVhcigpO1xuICAgICAgICB0aGlzLl9oZWFkID0gdW5kZWZpbmVkO1xuICAgICAgICB0aGlzLl90YWlsID0gdW5kZWZpbmVkO1xuICAgICAgICB0aGlzLl9zaXplID0gMDtcbiAgICAgICAgdGhpcy5fc3RhdGUrKztcbiAgICB9XG4gICAgaXNFbXB0eSgpIHtcbiAgICAgICAgcmV0dXJuICF0aGlzLl9oZWFkICYmICF0aGlzLl90YWlsO1xuICAgIH1cbiAgICBnZXQgc2l6ZSgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX3NpemU7XG4gICAgfVxuICAgIGdldCBmaXJzdCgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX2hlYWQ/LnZhbHVlO1xuICAgIH1cbiAgICBnZXQgbGFzdCgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX3RhaWw/LnZhbHVlO1xuICAgIH1cbiAgICBoYXMoa2V5KSB7XG4gICAgICAgIHJldHVybiB0aGlzLl9tYXAuaGFzKGtleSk7XG4gICAgfVxuICAgIGdldChrZXksIHRvdWNoID0gVG91Y2guTm9uZSkge1xuICAgICAgICBjb25zdCBpdGVtID0gdGhpcy5fbWFwLmdldChrZXkpO1xuICAgICAgICBpZiAoIWl0ZW0pIHtcbiAgICAgICAgICAgIHJldHVybiB1bmRlZmluZWQ7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKHRvdWNoICE9PSBUb3VjaC5Ob25lKSB7XG4gICAgICAgICAgICB0aGlzLnRvdWNoKGl0ZW0sIHRvdWNoKTtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gaXRlbS52YWx1ZTtcbiAgICB9XG4gICAgc2V0KGtleSwgdmFsdWUsIHRvdWNoID0gVG91Y2guTm9uZSkge1xuICAgICAgICBsZXQgaXRlbSA9IHRoaXMuX21hcC5nZXQoa2V5KTtcbiAgICAgICAgaWYgKGl0ZW0pIHtcbiAgICAgICAgICAgIGl0ZW0udmFsdWUgPSB2YWx1ZTtcbiAgICAgICAgICAgIGlmICh0b3VjaCAhPT0gVG91Y2guTm9uZSkge1xuICAgICAgICAgICAgICAgIHRoaXMudG91Y2goaXRlbSwgdG91Y2gpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgaXRlbSA9IHsga2V5LCB2YWx1ZSwgbmV4dDogdW5kZWZpbmVkLCBwcmV2aW91czogdW5kZWZpbmVkIH07XG4gICAgICAgICAgICBzd2l0Y2ggKHRvdWNoKSB7XG4gICAgICAgICAgICAgICAgY2FzZSBUb3VjaC5Ob25lOlxuICAgICAgICAgICAgICAgICAgICB0aGlzLmFkZEl0ZW1MYXN0KGl0ZW0pO1xuICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICBjYXNlIFRvdWNoLkZpcnN0OlxuICAgICAgICAgICAgICAgICAgICB0aGlzLmFkZEl0ZW1GaXJzdChpdGVtKTtcbiAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgY2FzZSBUb3VjaC5MYXN0OlxuICAgICAgICAgICAgICAgICAgICB0aGlzLmFkZEl0ZW1MYXN0KGl0ZW0pO1xuICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICBkZWZhdWx0OlxuICAgICAgICAgICAgICAgICAgICB0aGlzLmFkZEl0ZW1MYXN0KGl0ZW0pO1xuICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHRoaXMuX21hcC5zZXQoa2V5LCBpdGVtKTtcbiAgICAgICAgICAgIHRoaXMuX3NpemUrKztcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gdGhpcztcbiAgICB9XG4gICAgZGVsZXRlKGtleSkge1xuICAgICAgICByZXR1cm4gISF0aGlzLnJlbW92ZShrZXkpO1xuICAgIH1cbiAgICByZW1vdmUoa2V5KSB7XG4gICAgICAgIGNvbnN0IGl0ZW0gPSB0aGlzLl9tYXAuZ2V0KGtleSk7XG4gICAgICAgIGlmICghaXRlbSkge1xuICAgICAgICAgICAgcmV0dXJuIHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLl9tYXAuZGVsZXRlKGtleSk7XG4gICAgICAgIHRoaXMucmVtb3ZlSXRlbShpdGVtKTtcbiAgICAgICAgdGhpcy5fc2l6ZS0tO1xuICAgICAgICByZXR1cm4gaXRlbS52YWx1ZTtcbiAgICB9XG4gICAgc2hpZnQoKSB7XG4gICAgICAgIGlmICghdGhpcy5faGVhZCAmJiAhdGhpcy5fdGFpbCkge1xuICAgICAgICAgICAgcmV0dXJuIHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgICAgICBpZiAoIXRoaXMuX2hlYWQgfHwgIXRoaXMuX3RhaWwpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcignSW52YWxpZCBsaXN0Jyk7XG4gICAgICAgIH1cbiAgICAgICAgY29uc3QgaXRlbSA9IHRoaXMuX2hlYWQ7XG4gICAgICAgIHRoaXMuX21hcC5kZWxldGUoaXRlbS5rZXkpO1xuICAgICAgICB0aGlzLnJlbW92ZUl0ZW0oaXRlbSk7XG4gICAgICAgIHRoaXMuX3NpemUtLTtcbiAgICAgICAgcmV0dXJuIGl0ZW0udmFsdWU7XG4gICAgfVxuICAgIGZvckVhY2goY2FsbGJhY2tmbiwgdGhpc0FyZykge1xuICAgICAgICBjb25zdCBzdGF0ZSA9IHRoaXMuX3N0YXRlO1xuICAgICAgICBsZXQgY3VycmVudCA9IHRoaXMuX2hlYWQ7XG4gICAgICAgIHdoaWxlIChjdXJyZW50KSB7XG4gICAgICAgICAgICBpZiAodGhpc0FyZykge1xuICAgICAgICAgICAgICAgIGNhbGxiYWNrZm4uYmluZCh0aGlzQXJnKShjdXJyZW50LnZhbHVlLCBjdXJyZW50LmtleSwgdGhpcyk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICBjYWxsYmFja2ZuKGN1cnJlbnQudmFsdWUsIGN1cnJlbnQua2V5LCB0aGlzKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmICh0aGlzLl9zdGF0ZSAhPT0gc3RhdGUpIHtcbiAgICAgICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYExpbmtlZE1hcCBnb3QgbW9kaWZpZWQgZHVyaW5nIGl0ZXJhdGlvbi5gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGN1cnJlbnQgPSBjdXJyZW50Lm5leHQ7XG4gICAgICAgIH1cbiAgICB9XG4gICAga2V5cygpIHtcbiAgICAgICAgY29uc3Qgc3RhdGUgPSB0aGlzLl9zdGF0ZTtcbiAgICAgICAgbGV0IGN1cnJlbnQgPSB0aGlzLl9oZWFkO1xuICAgICAgICBjb25zdCBpdGVyYXRvciA9IHtcbiAgICAgICAgICAgIFtTeW1ib2wuaXRlcmF0b3JdOiAoKSA9PiB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIGl0ZXJhdG9yO1xuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIG5leHQ6ICgpID0+IHtcbiAgICAgICAgICAgICAgICBpZiAodGhpcy5fc3RhdGUgIT09IHN0YXRlKSB7XG4gICAgICAgICAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgTGlua2VkTWFwIGdvdCBtb2RpZmllZCBkdXJpbmcgaXRlcmF0aW9uLmApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBpZiAoY3VycmVudCkge1xuICAgICAgICAgICAgICAgICAgICBjb25zdCByZXN1bHQgPSB7IHZhbHVlOiBjdXJyZW50LmtleSwgZG9uZTogZmFsc2UgfTtcbiAgICAgICAgICAgICAgICAgICAgY3VycmVudCA9IGN1cnJlbnQubmV4dDtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiB7IHZhbHVlOiB1bmRlZmluZWQsIGRvbmU6IHRydWUgfTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH07XG4gICAgICAgIHJldHVybiBpdGVyYXRvcjtcbiAgICB9XG4gICAgdmFsdWVzKCkge1xuICAgICAgICBjb25zdCBzdGF0ZSA9IHRoaXMuX3N0YXRlO1xuICAgICAgICBsZXQgY3VycmVudCA9IHRoaXMuX2hlYWQ7XG4gICAgICAgIGNvbnN0IGl0ZXJhdG9yID0ge1xuICAgICAgICAgICAgW1N5bWJvbC5pdGVyYXRvcl06ICgpID0+IHtcbiAgICAgICAgICAgICAgICByZXR1cm4gaXRlcmF0b3I7XG4gICAgICAgICAgICB9LFxuICAgICAgICAgICAgbmV4dDogKCkgPT4ge1xuICAgICAgICAgICAgICAgIGlmICh0aGlzLl9zdGF0ZSAhPT0gc3RhdGUpIHtcbiAgICAgICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBMaW5rZWRNYXAgZ290IG1vZGlmaWVkIGR1cmluZyBpdGVyYXRpb24uYCk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGlmIChjdXJyZW50KSB7XG4gICAgICAgICAgICAgICAgICAgIGNvbnN0IHJlc3VsdCA9IHsgdmFsdWU6IGN1cnJlbnQudmFsdWUsIGRvbmU6IGZhbHNlIH07XG4gICAgICAgICAgICAgICAgICAgIGN1cnJlbnQgPSBjdXJyZW50Lm5leHQ7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4geyB2YWx1ZTogdW5kZWZpbmVkLCBkb25lOiB0cnVlIH07XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICB9O1xuICAgICAgICByZXR1cm4gaXRlcmF0b3I7XG4gICAgfVxuICAgIGVudHJpZXMoKSB7XG4gICAgICAgIGNvbnN0IHN0YXRlID0gdGhpcy5fc3RhdGU7XG4gICAgICAgIGxldCBjdXJyZW50ID0gdGhpcy5faGVhZDtcbiAgICAgICAgY29uc3QgaXRlcmF0b3IgPSB7XG4gICAgICAgICAgICBbU3ltYm9sLml0ZXJhdG9yXTogKCkgPT4ge1xuICAgICAgICAgICAgICAgIHJldHVybiBpdGVyYXRvcjtcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICBuZXh0OiAoKSA9PiB7XG4gICAgICAgICAgICAgICAgaWYgKHRoaXMuX3N0YXRlICE9PSBzdGF0ZSkge1xuICAgICAgICAgICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYExpbmtlZE1hcCBnb3QgbW9kaWZpZWQgZHVyaW5nIGl0ZXJhdGlvbi5gKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgaWYgKGN1cnJlbnQpIHtcbiAgICAgICAgICAgICAgICAgICAgY29uc3QgcmVzdWx0ID0geyB2YWx1ZTogW2N1cnJlbnQua2V5LCBjdXJyZW50LnZhbHVlXSwgZG9uZTogZmFsc2UgfTtcbiAgICAgICAgICAgICAgICAgICAgY3VycmVudCA9IGN1cnJlbnQubmV4dDtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiB7IHZhbHVlOiB1bmRlZmluZWQsIGRvbmU6IHRydWUgfTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH07XG4gICAgICAgIHJldHVybiBpdGVyYXRvcjtcbiAgICB9XG4gICAgWyhfYSA9IFN5bWJvbC50b1N0cmluZ1RhZywgU3ltYm9sLml0ZXJhdG9yKV0oKSB7XG4gICAgICAgIHJldHVybiB0aGlzLmVudHJpZXMoKTtcbiAgICB9XG4gICAgdHJpbU9sZChuZXdTaXplKSB7XG4gICAgICAgIGlmIChuZXdTaXplID49IHRoaXMuc2l6ZSkge1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGlmIChuZXdTaXplID09PSAwKSB7XG4gICAgICAgICAgICB0aGlzLmNsZWFyKCk7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgICAgbGV0IGN1cnJlbnQgPSB0aGlzLl9oZWFkO1xuICAgICAgICBsZXQgY3VycmVudFNpemUgPSB0aGlzLnNpemU7XG4gICAgICAgIHdoaWxlIChjdXJyZW50ICYmIGN1cnJlbnRTaXplID4gbmV3U2l6ZSkge1xuICAgICAgICAgICAgdGhpcy5fbWFwLmRlbGV0ZShjdXJyZW50LmtleSk7XG4gICAgICAgICAgICBjdXJyZW50ID0gY3VycmVudC5uZXh0O1xuICAgICAgICAgICAgY3VycmVudFNpemUtLTtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLl9oZWFkID0gY3VycmVudDtcbiAgICAgICAgdGhpcy5fc2l6ZSA9IGN1cnJlbnRTaXplO1xuICAgICAgICBpZiAoY3VycmVudCkge1xuICAgICAgICAgICAgY3VycmVudC5wcmV2aW91cyA9IHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLl9zdGF0ZSsrO1xuICAgIH1cbiAgICBhZGRJdGVtRmlyc3QoaXRlbSkge1xuICAgICAgICAvLyBGaXJzdCB0aW1lIEluc2VydFxuICAgICAgICBpZiAoIXRoaXMuX2hlYWQgJiYgIXRoaXMuX3RhaWwpIHtcbiAgICAgICAgICAgIHRoaXMuX3RhaWwgPSBpdGVtO1xuICAgICAgICB9XG4gICAgICAgIGVsc2UgaWYgKCF0aGlzLl9oZWFkKSB7XG4gICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoJ0ludmFsaWQgbGlzdCcpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgaXRlbS5uZXh0ID0gdGhpcy5faGVhZDtcbiAgICAgICAgICAgIHRoaXMuX2hlYWQucHJldmlvdXMgPSBpdGVtO1xuICAgICAgICB9XG4gICAgICAgIHRoaXMuX2hlYWQgPSBpdGVtO1xuICAgICAgICB0aGlzLl9zdGF0ZSsrO1xuICAgIH1cbiAgICBhZGRJdGVtTGFzdChpdGVtKSB7XG4gICAgICAgIC8vIEZpcnN0IHRpbWUgSW5zZXJ0XG4gICAgICAgIGlmICghdGhpcy5faGVhZCAmJiAhdGhpcy5fdGFpbCkge1xuICAgICAgICAgICAgdGhpcy5faGVhZCA9IGl0ZW07XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSBpZiAoIXRoaXMuX3RhaWwpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcignSW52YWxpZCBsaXN0Jyk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBpdGVtLnByZXZpb3VzID0gdGhpcy5fdGFpbDtcbiAgICAgICAgICAgIHRoaXMuX3RhaWwubmV4dCA9IGl0ZW07XG4gICAgICAgIH1cbiAgICAgICAgdGhpcy5fdGFpbCA9IGl0ZW07XG4gICAgICAgIHRoaXMuX3N0YXRlKys7XG4gICAgfVxuICAgIHJlbW92ZUl0ZW0oaXRlbSkge1xuICAgICAgICBpZiAoaXRlbSA9PT0gdGhpcy5faGVhZCAmJiBpdGVtID09PSB0aGlzLl90YWlsKSB7XG4gICAgICAgICAgICB0aGlzLl9oZWFkID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgdGhpcy5fdGFpbCA9IHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIGlmIChpdGVtID09PSB0aGlzLl9oZWFkKSB7XG4gICAgICAgICAgICAvLyBUaGlzIGNhbiBvbmx5IGhhcHBlbmVkIGlmIHNpemUgPT09IDEgd2hpY2ggaXMgaGFuZGxlXG4gICAgICAgICAgICAvLyBieSB0aGUgY2FzZSBhYm92ZS5cbiAgICAgICAgICAgIGlmICghaXRlbS5uZXh0KSB7XG4gICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdJbnZhbGlkIGxpc3QnKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGl0ZW0ubmV4dC5wcmV2aW91cyA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIHRoaXMuX2hlYWQgPSBpdGVtLm5leHQ7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSBpZiAoaXRlbSA9PT0gdGhpcy5fdGFpbCkge1xuICAgICAgICAgICAgLy8gVGhpcyBjYW4gb25seSBoYXBwZW5lZCBpZiBzaXplID09PSAxIHdoaWNoIGlzIGhhbmRsZVxuICAgICAgICAgICAgLy8gYnkgdGhlIGNhc2UgYWJvdmUuXG4gICAgICAgICAgICBpZiAoIWl0ZW0ucHJldmlvdXMpIHtcbiAgICAgICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoJ0ludmFsaWQgbGlzdCcpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaXRlbS5wcmV2aW91cy5uZXh0ID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgdGhpcy5fdGFpbCA9IGl0ZW0ucHJldmlvdXM7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBjb25zdCBuZXh0ID0gaXRlbS5uZXh0O1xuICAgICAgICAgICAgY29uc3QgcHJldmlvdXMgPSBpdGVtLnByZXZpb3VzO1xuICAgICAgICAgICAgaWYgKCFuZXh0IHx8ICFwcmV2aW91cykge1xuICAgICAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcignSW52YWxpZCBsaXN0Jyk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBuZXh0LnByZXZpb3VzID0gcHJldmlvdXM7XG4gICAgICAgICAgICBwcmV2aW91cy5uZXh0ID0gbmV4dDtcbiAgICAgICAgfVxuICAgICAgICBpdGVtLm5leHQgPSB1bmRlZmluZWQ7XG4gICAgICAgIGl0ZW0ucHJldmlvdXMgPSB1bmRlZmluZWQ7XG4gICAgICAgIHRoaXMuX3N0YXRlKys7XG4gICAgfVxuICAgIHRvdWNoKGl0ZW0sIHRvdWNoKSB7XG4gICAgICAgIGlmICghdGhpcy5faGVhZCB8fCAhdGhpcy5fdGFpbCkge1xuICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdJbnZhbGlkIGxpc3QnKTtcbiAgICAgICAgfVxuICAgICAgICBpZiAoKHRvdWNoICE9PSBUb3VjaC5GaXJzdCAmJiB0b3VjaCAhPT0gVG91Y2guTGFzdCkpIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBpZiAodG91Y2ggPT09IFRvdWNoLkZpcnN0KSB7XG4gICAgICAgICAgICBpZiAoaXRlbSA9PT0gdGhpcy5faGVhZCkge1xuICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNvbnN0IG5leHQgPSBpdGVtLm5leHQ7XG4gICAgICAgICAgICBjb25zdCBwcmV2aW91cyA9IGl0ZW0ucHJldmlvdXM7XG4gICAgICAgICAgICAvLyBVbmxpbmsgdGhlIGl0ZW1cbiAgICAgICAgICAgIGlmIChpdGVtID09PSB0aGlzLl90YWlsKSB7XG4gICAgICAgICAgICAgICAgLy8gcHJldmlvdXMgbXVzdCBiZSBkZWZpbmVkIHNpbmNlIGl0ZW0gd2FzIG5vdCBoZWFkIGJ1dCBpcyB0YWlsXG4gICAgICAgICAgICAgICAgLy8gU28gdGhlcmUgYXJlIG1vcmUgdGhhbiBvbiBpdGVtIGluIHRoZSBtYXBcbiAgICAgICAgICAgICAgICBwcmV2aW91cy5uZXh0ID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgICAgIHRoaXMuX3RhaWwgPSBwcmV2aW91cztcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIC8vIEJvdGggbmV4dCBhbmQgcHJldmlvdXMgYXJlIG5vdCB1bmRlZmluZWQgc2luY2UgaXRlbSB3YXMgbmVpdGhlciBoZWFkIG5vciB0YWlsLlxuICAgICAgICAgICAgICAgIG5leHQucHJldmlvdXMgPSBwcmV2aW91cztcbiAgICAgICAgICAgICAgICBwcmV2aW91cy5uZXh0ID0gbmV4dDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIC8vIEluc2VydCB0aGUgbm9kZSBhdCBoZWFkXG4gICAgICAgICAgICBpdGVtLnByZXZpb3VzID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgaXRlbS5uZXh0ID0gdGhpcy5faGVhZDtcbiAgICAgICAgICAgIHRoaXMuX2hlYWQucHJldmlvdXMgPSBpdGVtO1xuICAgICAgICAgICAgdGhpcy5faGVhZCA9IGl0ZW07XG4gICAgICAgICAgICB0aGlzLl9zdGF0ZSsrO1xuICAgICAgICB9XG4gICAgICAgIGVsc2UgaWYgKHRvdWNoID09PSBUb3VjaC5MYXN0KSB7XG4gICAgICAgICAgICBpZiAoaXRlbSA9PT0gdGhpcy5fdGFpbCkge1xuICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNvbnN0IG5leHQgPSBpdGVtLm5leHQ7XG4gICAgICAgICAgICBjb25zdCBwcmV2aW91cyA9IGl0ZW0ucHJldmlvdXM7XG4gICAgICAgICAgICAvLyBVbmxpbmsgdGhlIGl0ZW0uXG4gICAgICAgICAgICBpZiAoaXRlbSA9PT0gdGhpcy5faGVhZCkge1xuICAgICAgICAgICAgICAgIC8vIG5leHQgbXVzdCBiZSBkZWZpbmVkIHNpbmNlIGl0ZW0gd2FzIG5vdCB0YWlsIGJ1dCBpcyBoZWFkXG4gICAgICAgICAgICAgICAgLy8gU28gdGhlcmUgYXJlIG1vcmUgdGhhbiBvbiBpdGVtIGluIHRoZSBtYXBcbiAgICAgICAgICAgICAgICBuZXh0LnByZXZpb3VzID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgICAgIHRoaXMuX2hlYWQgPSBuZXh0O1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgLy8gQm90aCBuZXh0IGFuZCBwcmV2aW91cyBhcmUgbm90IHVuZGVmaW5lZCBzaW5jZSBpdGVtIHdhcyBuZWl0aGVyIGhlYWQgbm9yIHRhaWwuXG4gICAgICAgICAgICAgICAgbmV4dC5wcmV2aW91cyA9IHByZXZpb3VzO1xuICAgICAgICAgICAgICAgIHByZXZpb3VzLm5leHQgPSBuZXh0O1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaXRlbS5uZXh0ID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgaXRlbS5wcmV2aW91cyA9IHRoaXMuX3RhaWw7XG4gICAgICAgICAgICB0aGlzLl90YWlsLm5leHQgPSBpdGVtO1xuICAgICAgICAgICAgdGhpcy5fdGFpbCA9IGl0ZW07XG4gICAgICAgICAgICB0aGlzLl9zdGF0ZSsrO1xuICAgICAgICB9XG4gICAgfVxuICAgIHRvSlNPTigpIHtcbiAgICAgICAgY29uc3QgZGF0YSA9IFtdO1xuICAgICAgICB0aGlzLmZvckVhY2goKHZhbHVlLCBrZXkpID0+IHtcbiAgICAgICAgICAgIGRhdGEucHVzaChba2V5LCB2YWx1ZV0pO1xuICAgICAgICB9KTtcbiAgICAgICAgcmV0dXJuIGRhdGE7XG4gICAgfVxuICAgIGZyb21KU09OKGRhdGEpIHtcbiAgICAgICAgdGhpcy5jbGVhcigpO1xuICAgICAgICBmb3IgKGNvbnN0IFtrZXksIHZhbHVlXSBvZiBkYXRhKSB7XG4gICAgICAgICAgICB0aGlzLnNldChrZXksIHZhbHVlKTtcbiAgICAgICAgfVxuICAgIH1cbn1cbmV4cG9ydHMuTGlua2VkTWFwID0gTGlua2VkTWFwO1xuY2xhc3MgTFJVQ2FjaGUgZXh0ZW5kcyBMaW5rZWRNYXAge1xuICAgIGNvbnN0cnVjdG9yKGxpbWl0LCByYXRpbyA9IDEpIHtcbiAgICAgICAgc3VwZXIoKTtcbiAgICAgICAgdGhpcy5fbGltaXQgPSBsaW1pdDtcbiAgICAgICAgdGhpcy5fcmF0aW8gPSBNYXRoLm1pbihNYXRoLm1heCgwLCByYXRpbyksIDEpO1xuICAgIH1cbiAgICBnZXQgbGltaXQoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLl9saW1pdDtcbiAgICB9XG4gICAgc2V0IGxpbWl0KGxpbWl0KSB7XG4gICAgICAgIHRoaXMuX2xpbWl0ID0gbGltaXQ7XG4gICAgICAgIHRoaXMuY2hlY2tUcmltKCk7XG4gICAgfVxuICAgIGdldCByYXRpbygpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX3JhdGlvO1xuICAgIH1cbiAgICBzZXQgcmF0aW8ocmF0aW8pIHtcbiAgICAgICAgdGhpcy5fcmF0aW8gPSBNYXRoLm1pbihNYXRoLm1heCgwLCByYXRpbyksIDEpO1xuICAgICAgICB0aGlzLmNoZWNrVHJpbSgpO1xuICAgIH1cbiAgICBnZXQoa2V5LCB0b3VjaCA9IFRvdWNoLkFzTmV3KSB7XG4gICAgICAgIHJldHVybiBzdXBlci5nZXQoa2V5LCB0b3VjaCk7XG4gICAgfVxuICAgIHBlZWsoa2V5KSB7XG4gICAgICAgIHJldHVybiBzdXBlci5nZXQoa2V5LCBUb3VjaC5Ob25lKTtcbiAgICB9XG4gICAgc2V0KGtleSwgdmFsdWUpIHtcbiAgICAgICAgc3VwZXIuc2V0KGtleSwgdmFsdWUsIFRvdWNoLkxhc3QpO1xuICAgICAgICB0aGlzLmNoZWNrVHJpbSgpO1xuICAgICAgICByZXR1cm4gdGhpcztcbiAgICB9XG4gICAgY2hlY2tUcmltKCkge1xuICAgICAgICBpZiAodGhpcy5zaXplID4gdGhpcy5fbGltaXQpIHtcbiAgICAgICAgICAgIHRoaXMudHJpbU9sZChNYXRoLnJvdW5kKHRoaXMuX2xpbWl0ICogdGhpcy5fcmF0aW8pKTtcbiAgICAgICAgfVxuICAgIH1cbn1cbmV4cG9ydHMuTFJVQ2FjaGUgPSBMUlVDYWNoZTtcbiIsICJcInVzZSBzdHJpY3RcIjtcbi8qLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiAgQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiAgTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTGljZW5zZS50eHQgaW4gdGhlIHByb2plY3Qgcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0qL1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiX19lc01vZHVsZVwiLCB7IHZhbHVlOiB0cnVlIH0pO1xuZXhwb3J0cy5EaXNwb3NhYmxlID0gdm9pZCAwO1xudmFyIERpc3Bvc2FibGU7XG4oZnVuY3Rpb24gKERpc3Bvc2FibGUpIHtcbiAgICBmdW5jdGlvbiBjcmVhdGUoZnVuYykge1xuICAgICAgICByZXR1cm4ge1xuICAgICAgICAgICAgZGlzcG9zZTogZnVuY1xuICAgICAgICB9O1xuICAgIH1cbiAgICBEaXNwb3NhYmxlLmNyZWF0ZSA9IGNyZWF0ZTtcbn0pKERpc3Bvc2FibGUgfHwgKGV4cG9ydHMuRGlzcG9zYWJsZSA9IERpc3Bvc2FibGUgPSB7fSkpO1xuIiwgIlwidXNlIHN0cmljdFwiO1xuLyogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbiAqIENvcHlyaWdodCAoYykgTWljcm9zb2Z0IENvcnBvcmF0aW9uLiBBbGwgcmlnaHRzIHJlc2VydmVkLlxuICogTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTGljZW5zZS50eHQgaW4gdGhlIHByb2plY3Qgcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSAqL1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiX19lc01vZHVsZVwiLCB7IHZhbHVlOiB0cnVlIH0pO1xubGV0IF9yYWw7XG5mdW5jdGlvbiBSQUwoKSB7XG4gICAgaWYgKF9yYWwgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYE5vIHJ1bnRpbWUgYWJzdHJhY3Rpb24gbGF5ZXIgaW5zdGFsbGVkYCk7XG4gICAgfVxuICAgIHJldHVybiBfcmFsO1xufVxuKGZ1bmN0aW9uIChSQUwpIHtcbiAgICBmdW5jdGlvbiBpbnN0YWxsKHJhbCkge1xuICAgICAgICBpZiAocmFsID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgTm8gcnVudGltZSBhYnN0cmFjdGlvbiBsYXllciBwcm92aWRlZGApO1xuICAgICAgICB9XG4gICAgICAgIF9yYWwgPSByYWw7XG4gICAgfVxuICAgIFJBTC5pbnN0YWxsID0gaW5zdGFsbDtcbn0pKFJBTCB8fCAoUkFMID0ge30pKTtcbmV4cG9ydHMuZGVmYXVsdCA9IFJBTDtcbiIsICJcInVzZSBzdHJpY3RcIjtcbi8qIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiBDb3B5cmlnaHQgKGMpIE1pY3Jvc29mdCBDb3Jwb3JhdGlvbi4gQWxsIHJpZ2h0cyByZXNlcnZlZC5cbiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBNSVQgTGljZW5zZS4gU2VlIExpY2Vuc2UudHh0IGluIHRoZSBwcm9qZWN0IHJvb3QgZm9yIGxpY2Vuc2UgaW5mb3JtYXRpb24uXG4gKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0gKi9cbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIl9fZXNNb2R1bGVcIiwgeyB2YWx1ZTogdHJ1ZSB9KTtcbmV4cG9ydHMuRW1pdHRlciA9IGV4cG9ydHMuRXZlbnQgPSB2b2lkIDA7XG5jb25zdCByYWxfMSA9IHJlcXVpcmUoXCIuL3JhbFwiKTtcbnZhciBFdmVudDtcbihmdW5jdGlvbiAoRXZlbnQpIHtcbiAgICBjb25zdCBfZGlzcG9zYWJsZSA9IHsgZGlzcG9zZSgpIHsgfSB9O1xuICAgIEV2ZW50Lk5vbmUgPSBmdW5jdGlvbiAoKSB7IHJldHVybiBfZGlzcG9zYWJsZTsgfTtcbn0pKEV2ZW50IHx8IChleHBvcnRzLkV2ZW50ID0gRXZlbnQgPSB7fSkpO1xuY2xhc3MgQ2FsbGJhY2tMaXN0IHtcbiAgICBhZGQoY2FsbGJhY2ssIGNvbnRleHQgPSBudWxsLCBidWNrZXQpIHtcbiAgICAgICAgaWYgKCF0aGlzLl9jYWxsYmFja3MpIHtcbiAgICAgICAgICAgIHRoaXMuX2NhbGxiYWNrcyA9IFtdO1xuICAgICAgICAgICAgdGhpcy5fY29udGV4dHMgPSBbXTtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLl9jYWxsYmFja3MucHVzaChjYWxsYmFjayk7XG4gICAgICAgIHRoaXMuX2NvbnRleHRzLnB1c2goY29udGV4dCk7XG4gICAgICAgIGlmIChBcnJheS5pc0FycmF5KGJ1Y2tldCkpIHtcbiAgICAgICAgICAgIGJ1Y2tldC5wdXNoKHsgZGlzcG9zZTogKCkgPT4gdGhpcy5yZW1vdmUoY2FsbGJhY2ssIGNvbnRleHQpIH0pO1xuICAgICAgICB9XG4gICAgfVxuICAgIHJlbW92ZShjYWxsYmFjaywgY29udGV4dCA9IG51bGwpIHtcbiAgICAgICAgaWYgKCF0aGlzLl9jYWxsYmFja3MpIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBsZXQgZm91bmRDYWxsYmFja1dpdGhEaWZmZXJlbnRDb250ZXh0ID0gZmFsc2U7XG4gICAgICAgIGZvciAobGV0IGkgPSAwLCBsZW4gPSB0aGlzLl9jYWxsYmFja3MubGVuZ3RoOyBpIDwgbGVuOyBpKyspIHtcbiAgICAgICAgICAgIGlmICh0aGlzLl9jYWxsYmFja3NbaV0gPT09IGNhbGxiYWNrKSB7XG4gICAgICAgICAgICAgICAgaWYgKHRoaXMuX2NvbnRleHRzW2ldID09PSBjb250ZXh0KSB7XG4gICAgICAgICAgICAgICAgICAgIC8vIGNhbGxiYWNrICYgY29udGV4dCBtYXRjaCA9PiByZW1vdmUgaXRcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5fY2FsbGJhY2tzLnNwbGljZShpLCAxKTtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5fY29udGV4dHMuc3BsaWNlKGksIDEpO1xuICAgICAgICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICBmb3VuZENhbGxiYWNrV2l0aERpZmZlcmVudENvbnRleHQgPSB0cnVlO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICBpZiAoZm91bmRDYWxsYmFja1dpdGhEaWZmZXJlbnRDb250ZXh0KSB7XG4gICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoJ1doZW4gYWRkaW5nIGEgbGlzdGVuZXIgd2l0aCBhIGNvbnRleHQsIHlvdSBzaG91bGQgcmVtb3ZlIGl0IHdpdGggdGhlIHNhbWUgY29udGV4dCcpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGludm9rZSguLi5hcmdzKSB7XG4gICAgICAgIGlmICghdGhpcy5fY2FsbGJhY2tzKSB7XG4gICAgICAgICAgICByZXR1cm4gW107XG4gICAgICAgIH1cbiAgICAgICAgY29uc3QgcmV0ID0gW10sIGNhbGxiYWNrcyA9IHRoaXMuX2NhbGxiYWNrcy5zbGljZSgwKSwgY29udGV4dHMgPSB0aGlzLl9jb250ZXh0cy5zbGljZSgwKTtcbiAgICAgICAgZm9yIChsZXQgaSA9IDAsIGxlbiA9IGNhbGxiYWNrcy5sZW5ndGg7IGkgPCBsZW47IGkrKykge1xuICAgICAgICAgICAgdHJ5IHtcbiAgICAgICAgICAgICAgICByZXQucHVzaChjYWxsYmFja3NbaV0uYXBwbHkoY29udGV4dHNbaV0sIGFyZ3MpKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNhdGNoIChlKSB7XG4gICAgICAgICAgICAgICAgLy8gZXNsaW50LWRpc2FibGUtbmV4dC1saW5lIG5vLWNvbnNvbGVcbiAgICAgICAgICAgICAgICAoMCwgcmFsXzEuZGVmYXVsdCkoKS5jb25zb2xlLmVycm9yKGUpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXQ7XG4gICAgfVxuICAgIGlzRW1wdHkoKSB7XG4gICAgICAgIHJldHVybiAhdGhpcy5fY2FsbGJhY2tzIHx8IHRoaXMuX2NhbGxiYWNrcy5sZW5ndGggPT09IDA7XG4gICAgfVxuICAgIGRpc3Bvc2UoKSB7XG4gICAgICAgIHRoaXMuX2NhbGxiYWNrcyA9IHVuZGVmaW5lZDtcbiAgICAgICAgdGhpcy5fY29udGV4dHMgPSB1bmRlZmluZWQ7XG4gICAgfVxufVxuY2xhc3MgRW1pdHRlciB7XG4gICAgY29uc3RydWN0b3IoX29wdGlvbnMpIHtcbiAgICAgICAgdGhpcy5fb3B0aW9ucyA9IF9vcHRpb25zO1xuICAgIH1cbiAgICAvKipcbiAgICAgKiBGb3IgdGhlIHB1YmxpYyB0byBhbGxvdyB0byBzdWJzY3JpYmVcbiAgICAgKiB0byBldmVudHMgZnJvbSB0aGlzIEVtaXR0ZXJcbiAgICAgKi9cbiAgICBnZXQgZXZlbnQoKSB7XG4gICAgICAgIGlmICghdGhpcy5fZXZlbnQpIHtcbiAgICAgICAgICAgIHRoaXMuX2V2ZW50ID0gKGxpc3RlbmVyLCB0aGlzQXJncywgZGlzcG9zYWJsZXMpID0+IHtcbiAgICAgICAgICAgICAgICBpZiAoIXRoaXMuX2NhbGxiYWNrcykge1xuICAgICAgICAgICAgICAgICAgICB0aGlzLl9jYWxsYmFja3MgPSBuZXcgQ2FsbGJhY2tMaXN0KCk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGlmICh0aGlzLl9vcHRpb25zICYmIHRoaXMuX29wdGlvbnMub25GaXJzdExpc3RlbmVyQWRkICYmIHRoaXMuX2NhbGxiYWNrcy5pc0VtcHR5KCkpIHtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5fb3B0aW9ucy5vbkZpcnN0TGlzdGVuZXJBZGQodGhpcyk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIHRoaXMuX2NhbGxiYWNrcy5hZGQobGlzdGVuZXIsIHRoaXNBcmdzKTtcbiAgICAgICAgICAgICAgICBjb25zdCByZXN1bHQgPSB7XG4gICAgICAgICAgICAgICAgICAgIGRpc3Bvc2U6ICgpID0+IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICghdGhpcy5fY2FsbGJhY2tzKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8gZGlzcG9zYWJsZSBpcyBkaXNwb3NlZCBhZnRlciBlbWl0dGVyIGlzIGRpc3Bvc2VkLlxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIHRoaXMuX2NhbGxiYWNrcy5yZW1vdmUobGlzdGVuZXIsIHRoaXNBcmdzKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJlc3VsdC5kaXNwb3NlID0gRW1pdHRlci5fbm9vcDtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICh0aGlzLl9vcHRpb25zICYmIHRoaXMuX29wdGlvbnMub25MYXN0TGlzdGVuZXJSZW1vdmUgJiYgdGhpcy5fY2FsbGJhY2tzLmlzRW1wdHkoKSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHRoaXMuX29wdGlvbnMub25MYXN0TGlzdGVuZXJSZW1vdmUodGhpcyk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICAgIGlmIChBcnJheS5pc0FycmF5KGRpc3Bvc2FibGVzKSkge1xuICAgICAgICAgICAgICAgICAgICBkaXNwb3NhYmxlcy5wdXNoKHJlc3VsdCk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgICAgICAgICB9O1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiB0aGlzLl9ldmVudDtcbiAgICB9XG4gICAgLyoqXG4gICAgICogVG8gYmUga2VwdCBwcml2YXRlIHRvIGZpcmUgYW4gZXZlbnQgdG9cbiAgICAgKiBzdWJzY3JpYmVyc1xuICAgICAqL1xuICAgIGZpcmUoZXZlbnQpIHtcbiAgICAgICAgaWYgKHRoaXMuX2NhbGxiYWNrcykge1xuICAgICAgICAgICAgdGhpcy5fY2FsbGJhY2tzLmludm9rZS5jYWxsKHRoaXMuX2NhbGxiYWNrcywgZXZlbnQpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGRpc3Bvc2UoKSB7XG4gICAgICAgIGlmICh0aGlzLl9jYWxsYmFja3MpIHtcbiAgICAgICAgICAgIHRoaXMuX2NhbGxiYWNrcy5kaXNwb3NlKCk7XG4gICAgICAgICAgICB0aGlzLl9jYWxsYmFja3MgPSB1bmRlZmluZWQ7XG4gICAgICAgIH1cbiAgICB9XG59XG5leHBvcnRzLkVtaXR0ZXIgPSBFbWl0dGVyO1xuRW1pdHRlci5fbm9vcCA9IGZ1bmN0aW9uICgpIHsgfTtcbiIsICJcInVzZSBzdHJpY3RcIjtcbi8qLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiAgQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiAgTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTGljZW5zZS50eHQgaW4gdGhlIHByb2plY3Qgcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0qL1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiX19lc01vZHVsZVwiLCB7IHZhbHVlOiB0cnVlIH0pO1xuZXhwb3J0cy5DYW5jZWxsYXRpb25Ub2tlblNvdXJjZSA9IGV4cG9ydHMuQ2FuY2VsbGF0aW9uVG9rZW4gPSB2b2lkIDA7XG5jb25zdCByYWxfMSA9IHJlcXVpcmUoXCIuL3JhbFwiKTtcbmNvbnN0IElzID0gcmVxdWlyZShcIi4vaXNcIik7XG5jb25zdCBldmVudHNfMSA9IHJlcXVpcmUoXCIuL2V2ZW50c1wiKTtcbnZhciBDYW5jZWxsYXRpb25Ub2tlbjtcbihmdW5jdGlvbiAoQ2FuY2VsbGF0aW9uVG9rZW4pIHtcbiAgICBDYW5jZWxsYXRpb25Ub2tlbi5Ob25lID0gT2JqZWN0LmZyZWV6ZSh7XG4gICAgICAgIGlzQ2FuY2VsbGF0aW9uUmVxdWVzdGVkOiBmYWxzZSxcbiAgICAgICAgb25DYW5jZWxsYXRpb25SZXF1ZXN0ZWQ6IGV2ZW50c18xLkV2ZW50Lk5vbmVcbiAgICB9KTtcbiAgICBDYW5jZWxsYXRpb25Ub2tlbi5DYW5jZWxsZWQgPSBPYmplY3QuZnJlZXplKHtcbiAgICAgICAgaXNDYW5jZWxsYXRpb25SZXF1ZXN0ZWQ6IHRydWUsXG4gICAgICAgIG9uQ2FuY2VsbGF0aW9uUmVxdWVzdGVkOiBldmVudHNfMS5FdmVudC5Ob25lXG4gICAgfSk7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBjYW5kaWRhdGUgJiYgKGNhbmRpZGF0ZSA9PT0gQ2FuY2VsbGF0aW9uVG9rZW4uTm9uZVxuICAgICAgICAgICAgfHwgY2FuZGlkYXRlID09PSBDYW5jZWxsYXRpb25Ub2tlbi5DYW5jZWxsZWRcbiAgICAgICAgICAgIHx8IChJcy5ib29sZWFuKGNhbmRpZGF0ZS5pc0NhbmNlbGxhdGlvblJlcXVlc3RlZCkgJiYgISFjYW5kaWRhdGUub25DYW5jZWxsYXRpb25SZXF1ZXN0ZWQpKTtcbiAgICB9XG4gICAgQ2FuY2VsbGF0aW9uVG9rZW4uaXMgPSBpcztcbn0pKENhbmNlbGxhdGlvblRva2VuIHx8IChleHBvcnRzLkNhbmNlbGxhdGlvblRva2VuID0gQ2FuY2VsbGF0aW9uVG9rZW4gPSB7fSkpO1xuY29uc3Qgc2hvcnRjdXRFdmVudCA9IE9iamVjdC5mcmVlemUoZnVuY3Rpb24gKGNhbGxiYWNrLCBjb250ZXh0KSB7XG4gICAgY29uc3QgaGFuZGxlID0gKDAsIHJhbF8xLmRlZmF1bHQpKCkudGltZXIuc2V0VGltZW91dChjYWxsYmFjay5iaW5kKGNvbnRleHQpLCAwKTtcbiAgICByZXR1cm4geyBkaXNwb3NlKCkgeyBoYW5kbGUuZGlzcG9zZSgpOyB9IH07XG59KTtcbmNsYXNzIE11dGFibGVUb2tlbiB7XG4gICAgY29uc3RydWN0b3IoKSB7XG4gICAgICAgIHRoaXMuX2lzQ2FuY2VsbGVkID0gZmFsc2U7XG4gICAgfVxuICAgIGNhbmNlbCgpIHtcbiAgICAgICAgaWYgKCF0aGlzLl9pc0NhbmNlbGxlZCkge1xuICAgICAgICAgICAgdGhpcy5faXNDYW5jZWxsZWQgPSB0cnVlO1xuICAgICAgICAgICAgaWYgKHRoaXMuX2VtaXR0ZXIpIHtcbiAgICAgICAgICAgICAgICB0aGlzLl9lbWl0dGVyLmZpcmUodW5kZWZpbmVkKTtcbiAgICAgICAgICAgICAgICB0aGlzLmRpc3Bvc2UoKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgIH1cbiAgICBnZXQgaXNDYW5jZWxsYXRpb25SZXF1ZXN0ZWQoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLl9pc0NhbmNlbGxlZDtcbiAgICB9XG4gICAgZ2V0IG9uQ2FuY2VsbGF0aW9uUmVxdWVzdGVkKCkge1xuICAgICAgICBpZiAodGhpcy5faXNDYW5jZWxsZWQpIHtcbiAgICAgICAgICAgIHJldHVybiBzaG9ydGN1dEV2ZW50O1xuICAgICAgICB9XG4gICAgICAgIGlmICghdGhpcy5fZW1pdHRlcikge1xuICAgICAgICAgICAgdGhpcy5fZW1pdHRlciA9IG5ldyBldmVudHNfMS5FbWl0dGVyKCk7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHRoaXMuX2VtaXR0ZXIuZXZlbnQ7XG4gICAgfVxuICAgIGRpc3Bvc2UoKSB7XG4gICAgICAgIGlmICh0aGlzLl9lbWl0dGVyKSB7XG4gICAgICAgICAgICB0aGlzLl9lbWl0dGVyLmRpc3Bvc2UoKTtcbiAgICAgICAgICAgIHRoaXMuX2VtaXR0ZXIgPSB1bmRlZmluZWQ7XG4gICAgICAgIH1cbiAgICB9XG59XG5jbGFzcyBDYW5jZWxsYXRpb25Ub2tlblNvdXJjZSB7XG4gICAgZ2V0IHRva2VuKCkge1xuICAgICAgICBpZiAoIXRoaXMuX3Rva2VuKSB7XG4gICAgICAgICAgICAvLyBiZSBsYXp5IGFuZCBjcmVhdGUgdGhlIHRva2VuIG9ubHkgd2hlblxuICAgICAgICAgICAgLy8gYWN0dWFsbHkgbmVlZGVkXG4gICAgICAgICAgICB0aGlzLl90b2tlbiA9IG5ldyBNdXRhYmxlVG9rZW4oKTtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gdGhpcy5fdG9rZW47XG4gICAgfVxuICAgIGNhbmNlbCgpIHtcbiAgICAgICAgaWYgKCF0aGlzLl90b2tlbikge1xuICAgICAgICAgICAgLy8gc2F2ZSBhbiBvYmplY3QgYnkgcmV0dXJuaW5nIHRoZSBkZWZhdWx0XG4gICAgICAgICAgICAvLyBjYW5jZWxsZWQgdG9rZW4gd2hlbiBjYW5jZWxsYXRpb24gaGFwcGVuc1xuICAgICAgICAgICAgLy8gYmVmb3JlIHNvbWVvbmUgYXNrcyBmb3IgdGhlIHRva2VuXG4gICAgICAgICAgICB0aGlzLl90b2tlbiA9IENhbmNlbGxhdGlvblRva2VuLkNhbmNlbGxlZDtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHRoaXMuX3Rva2VuLmNhbmNlbCgpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGRpc3Bvc2UoKSB7XG4gICAgICAgIGlmICghdGhpcy5fdG9rZW4pIHtcbiAgICAgICAgICAgIC8vIGVuc3VyZSB0byBpbml0aWFsaXplIHdpdGggYW4gZW1wdHkgdG9rZW4gaWYgd2UgaGFkIG5vbmVcbiAgICAgICAgICAgIHRoaXMuX3Rva2VuID0gQ2FuY2VsbGF0aW9uVG9rZW4uTm9uZTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIGlmICh0aGlzLl90b2tlbiBpbnN0YW5jZW9mIE11dGFibGVUb2tlbikge1xuICAgICAgICAgICAgLy8gYWN0dWFsbHkgZGlzcG9zZVxuICAgICAgICAgICAgdGhpcy5fdG9rZW4uZGlzcG9zZSgpO1xuICAgICAgICB9XG4gICAgfVxufVxuZXhwb3J0cy5DYW5jZWxsYXRpb25Ub2tlblNvdXJjZSA9IENhbmNlbGxhdGlvblRva2VuU291cmNlO1xuIiwgIlwidXNlIHN0cmljdFwiO1xuLyogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbiAqIENvcHlyaWdodCAoYykgTWljcm9zb2Z0IENvcnBvcmF0aW9uLiBBbGwgcmlnaHRzIHJlc2VydmVkLlxuICogTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTGljZW5zZS50eHQgaW4gdGhlIHByb2plY3Qgcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSAqL1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiX19lc01vZHVsZVwiLCB7IHZhbHVlOiB0cnVlIH0pO1xuZXhwb3J0cy5TaGFyZWRBcnJheVJlY2VpdmVyU3RyYXRlZ3kgPSBleHBvcnRzLlNoYXJlZEFycmF5U2VuZGVyU3RyYXRlZ3kgPSB2b2lkIDA7XG5jb25zdCBjYW5jZWxsYXRpb25fMSA9IHJlcXVpcmUoXCIuL2NhbmNlbGxhdGlvblwiKTtcbnZhciBDYW5jZWxsYXRpb25TdGF0ZTtcbihmdW5jdGlvbiAoQ2FuY2VsbGF0aW9uU3RhdGUpIHtcbiAgICBDYW5jZWxsYXRpb25TdGF0ZS5Db250aW51ZSA9IDA7XG4gICAgQ2FuY2VsbGF0aW9uU3RhdGUuQ2FuY2VsbGVkID0gMTtcbn0pKENhbmNlbGxhdGlvblN0YXRlIHx8IChDYW5jZWxsYXRpb25TdGF0ZSA9IHt9KSk7XG5jbGFzcyBTaGFyZWRBcnJheVNlbmRlclN0cmF0ZWd5IHtcbiAgICBjb25zdHJ1Y3RvcigpIHtcbiAgICAgICAgdGhpcy5idWZmZXJzID0gbmV3IE1hcCgpO1xuICAgIH1cbiAgICBlbmFibGVDYW5jZWxsYXRpb24ocmVxdWVzdCkge1xuICAgICAgICBpZiAocmVxdWVzdC5pZCA9PT0gbnVsbCkge1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGNvbnN0IGJ1ZmZlciA9IG5ldyBTaGFyZWRBcnJheUJ1ZmZlcig0KTtcbiAgICAgICAgY29uc3QgZGF0YSA9IG5ldyBJbnQzMkFycmF5KGJ1ZmZlciwgMCwgMSk7XG4gICAgICAgIGRhdGFbMF0gPSBDYW5jZWxsYXRpb25TdGF0ZS5Db250aW51ZTtcbiAgICAgICAgdGhpcy5idWZmZXJzLnNldChyZXF1ZXN0LmlkLCBidWZmZXIpO1xuICAgICAgICByZXF1ZXN0LiRjYW5jZWxsYXRpb25EYXRhID0gYnVmZmVyO1xuICAgIH1cbiAgICBhc3luYyBzZW5kQ2FuY2VsbGF0aW9uKF9jb25uLCBpZCkge1xuICAgICAgICBjb25zdCBidWZmZXIgPSB0aGlzLmJ1ZmZlcnMuZ2V0KGlkKTtcbiAgICAgICAgaWYgKGJ1ZmZlciA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgICAgY29uc3QgZGF0YSA9IG5ldyBJbnQzMkFycmF5KGJ1ZmZlciwgMCwgMSk7XG4gICAgICAgIEF0b21pY3Muc3RvcmUoZGF0YSwgMCwgQ2FuY2VsbGF0aW9uU3RhdGUuQ2FuY2VsbGVkKTtcbiAgICB9XG4gICAgY2xlYW51cChpZCkge1xuICAgICAgICB0aGlzLmJ1ZmZlcnMuZGVsZXRlKGlkKTtcbiAgICB9XG4gICAgZGlzcG9zZSgpIHtcbiAgICAgICAgdGhpcy5idWZmZXJzLmNsZWFyKCk7XG4gICAgfVxufVxuZXhwb3J0cy5TaGFyZWRBcnJheVNlbmRlclN0cmF0ZWd5ID0gU2hhcmVkQXJyYXlTZW5kZXJTdHJhdGVneTtcbmNsYXNzIFNoYXJlZEFycmF5QnVmZmVyQ2FuY2VsbGF0aW9uVG9rZW4ge1xuICAgIGNvbnN0cnVjdG9yKGJ1ZmZlcikge1xuICAgICAgICB0aGlzLmRhdGEgPSBuZXcgSW50MzJBcnJheShidWZmZXIsIDAsIDEpO1xuICAgIH1cbiAgICBnZXQgaXNDYW5jZWxsYXRpb25SZXF1ZXN0ZWQoKSB7XG4gICAgICAgIHJldHVybiBBdG9taWNzLmxvYWQodGhpcy5kYXRhLCAwKSA9PT0gQ2FuY2VsbGF0aW9uU3RhdGUuQ2FuY2VsbGVkO1xuICAgIH1cbiAgICBnZXQgb25DYW5jZWxsYXRpb25SZXF1ZXN0ZWQoKSB7XG4gICAgICAgIHRocm93IG5ldyBFcnJvcihgQ2FuY2VsbGF0aW9uIG92ZXIgU2hhcmVkQXJyYXlCdWZmZXIgZG9lc24ndCBzdXBwb3J0IGNhbmNlbGxhdGlvbiBldmVudHNgKTtcbiAgICB9XG59XG5jbGFzcyBTaGFyZWRBcnJheUJ1ZmZlckNhbmNlbGxhdGlvblRva2VuU291cmNlIHtcbiAgICBjb25zdHJ1Y3RvcihidWZmZXIpIHtcbiAgICAgICAgdGhpcy50b2tlbiA9IG5ldyBTaGFyZWRBcnJheUJ1ZmZlckNhbmNlbGxhdGlvblRva2VuKGJ1ZmZlcik7XG4gICAgfVxuICAgIGNhbmNlbCgpIHtcbiAgICB9XG4gICAgZGlzcG9zZSgpIHtcbiAgICB9XG59XG5jbGFzcyBTaGFyZWRBcnJheVJlY2VpdmVyU3RyYXRlZ3kge1xuICAgIGNvbnN0cnVjdG9yKCkge1xuICAgICAgICB0aGlzLmtpbmQgPSAncmVxdWVzdCc7XG4gICAgfVxuICAgIGNyZWF0ZUNhbmNlbGxhdGlvblRva2VuU291cmNlKHJlcXVlc3QpIHtcbiAgICAgICAgY29uc3QgYnVmZmVyID0gcmVxdWVzdC4kY2FuY2VsbGF0aW9uRGF0YTtcbiAgICAgICAgaWYgKGJ1ZmZlciA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXR1cm4gbmV3IGNhbmNlbGxhdGlvbl8xLkNhbmNlbGxhdGlvblRva2VuU291cmNlKCk7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIG5ldyBTaGFyZWRBcnJheUJ1ZmZlckNhbmNlbGxhdGlvblRva2VuU291cmNlKGJ1ZmZlcik7XG4gICAgfVxufVxuZXhwb3J0cy5TaGFyZWRBcnJheVJlY2VpdmVyU3RyYXRlZ3kgPSBTaGFyZWRBcnJheVJlY2VpdmVyU3RyYXRlZ3k7XG4iLCAiXCJ1c2Ugc3RyaWN0XCI7XG4vKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJfX2VzTW9kdWxlXCIsIHsgdmFsdWU6IHRydWUgfSk7XG5leHBvcnRzLlNlbWFwaG9yZSA9IHZvaWQgMDtcbmNvbnN0IHJhbF8xID0gcmVxdWlyZShcIi4vcmFsXCIpO1xuY2xhc3MgU2VtYXBob3JlIHtcbiAgICBjb25zdHJ1Y3RvcihjYXBhY2l0eSA9IDEpIHtcbiAgICAgICAgaWYgKGNhcGFjaXR5IDw9IDApIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcignQ2FwYWNpdHkgbXVzdCBiZSBncmVhdGVyIHRoYW4gMCcpO1xuICAgICAgICB9XG4gICAgICAgIHRoaXMuX2NhcGFjaXR5ID0gY2FwYWNpdHk7XG4gICAgICAgIHRoaXMuX2FjdGl2ZSA9IDA7XG4gICAgICAgIHRoaXMuX3dhaXRpbmcgPSBbXTtcbiAgICB9XG4gICAgbG9jayh0aHVuaykge1xuICAgICAgICByZXR1cm4gbmV3IFByb21pc2UoKHJlc29sdmUsIHJlamVjdCkgPT4ge1xuICAgICAgICAgICAgdGhpcy5fd2FpdGluZy5wdXNoKHsgdGh1bmssIHJlc29sdmUsIHJlamVjdCB9KTtcbiAgICAgICAgICAgIHRoaXMucnVuTmV4dCgpO1xuICAgICAgICB9KTtcbiAgICB9XG4gICAgZ2V0IGFjdGl2ZSgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX2FjdGl2ZTtcbiAgICB9XG4gICAgcnVuTmV4dCgpIHtcbiAgICAgICAgaWYgKHRoaXMuX3dhaXRpbmcubGVuZ3RoID09PSAwIHx8IHRoaXMuX2FjdGl2ZSA9PT0gdGhpcy5fY2FwYWNpdHkpIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICAoMCwgcmFsXzEuZGVmYXVsdCkoKS50aW1lci5zZXRJbW1lZGlhdGUoKCkgPT4gdGhpcy5kb1J1bk5leHQoKSk7XG4gICAgfVxuICAgIGRvUnVuTmV4dCgpIHtcbiAgICAgICAgaWYgKHRoaXMuX3dhaXRpbmcubGVuZ3RoID09PSAwIHx8IHRoaXMuX2FjdGl2ZSA9PT0gdGhpcy5fY2FwYWNpdHkpIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBjb25zdCBuZXh0ID0gdGhpcy5fd2FpdGluZy5zaGlmdCgpO1xuICAgICAgICB0aGlzLl9hY3RpdmUrKztcbiAgICAgICAgaWYgKHRoaXMuX2FjdGl2ZSA+IHRoaXMuX2NhcGFjaXR5KSB7XG4gICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYFRvIG1hbnkgdGh1bmtzIGFjdGl2ZWApO1xuICAgICAgICB9XG4gICAgICAgIHRyeSB7XG4gICAgICAgICAgICBjb25zdCByZXN1bHQgPSBuZXh0LnRodW5rKCk7XG4gICAgICAgICAgICBpZiAocmVzdWx0IGluc3RhbmNlb2YgUHJvbWlzZSkge1xuICAgICAgICAgICAgICAgIHJlc3VsdC50aGVuKCh2YWx1ZSkgPT4ge1xuICAgICAgICAgICAgICAgICAgICB0aGlzLl9hY3RpdmUtLTtcbiAgICAgICAgICAgICAgICAgICAgbmV4dC5yZXNvbHZlKHZhbHVlKTtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5ydW5OZXh0KCk7XG4gICAgICAgICAgICAgICAgfSwgKGVycikgPT4ge1xuICAgICAgICAgICAgICAgICAgICB0aGlzLl9hY3RpdmUtLTtcbiAgICAgICAgICAgICAgICAgICAgbmV4dC5yZWplY3QoZXJyKTtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5ydW5OZXh0KCk7XG4gICAgICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICB0aGlzLl9hY3RpdmUtLTtcbiAgICAgICAgICAgICAgICBuZXh0LnJlc29sdmUocmVzdWx0KTtcbiAgICAgICAgICAgICAgICB0aGlzLnJ1bk5leHQoKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICBjYXRjaCAoZXJyKSB7XG4gICAgICAgICAgICB0aGlzLl9hY3RpdmUtLTtcbiAgICAgICAgICAgIG5leHQucmVqZWN0KGVycik7XG4gICAgICAgICAgICB0aGlzLnJ1bk5leHQoKTtcbiAgICAgICAgfVxuICAgIH1cbn1cbmV4cG9ydHMuU2VtYXBob3JlID0gU2VtYXBob3JlO1xuIiwgIlwidXNlIHN0cmljdFwiO1xuLyogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbiAqIENvcHlyaWdodCAoYykgTWljcm9zb2Z0IENvcnBvcmF0aW9uLiBBbGwgcmlnaHRzIHJlc2VydmVkLlxuICogTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTGljZW5zZS50eHQgaW4gdGhlIHByb2plY3Qgcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSAqL1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiX19lc01vZHVsZVwiLCB7IHZhbHVlOiB0cnVlIH0pO1xuZXhwb3J0cy5SZWFkYWJsZVN0cmVhbU1lc3NhZ2VSZWFkZXIgPSBleHBvcnRzLkFic3RyYWN0TWVzc2FnZVJlYWRlciA9IGV4cG9ydHMuTWVzc2FnZVJlYWRlciA9IHZvaWQgMDtcbmNvbnN0IHJhbF8xID0gcmVxdWlyZShcIi4vcmFsXCIpO1xuY29uc3QgSXMgPSByZXF1aXJlKFwiLi9pc1wiKTtcbmNvbnN0IGV2ZW50c18xID0gcmVxdWlyZShcIi4vZXZlbnRzXCIpO1xuY29uc3Qgc2VtYXBob3JlXzEgPSByZXF1aXJlKFwiLi9zZW1hcGhvcmVcIik7XG52YXIgTWVzc2FnZVJlYWRlcjtcbihmdW5jdGlvbiAoTWVzc2FnZVJlYWRlcikge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiBJcy5mdW5jKGNhbmRpZGF0ZS5saXN0ZW4pICYmIElzLmZ1bmMoY2FuZGlkYXRlLmRpc3Bvc2UpICYmXG4gICAgICAgICAgICBJcy5mdW5jKGNhbmRpZGF0ZS5vbkVycm9yKSAmJiBJcy5mdW5jKGNhbmRpZGF0ZS5vbkNsb3NlKSAmJiBJcy5mdW5jKGNhbmRpZGF0ZS5vblBhcnRpYWxNZXNzYWdlKTtcbiAgICB9XG4gICAgTWVzc2FnZVJlYWRlci5pcyA9IGlzO1xufSkoTWVzc2FnZVJlYWRlciB8fCAoZXhwb3J0cy5NZXNzYWdlUmVhZGVyID0gTWVzc2FnZVJlYWRlciA9IHt9KSk7XG5jbGFzcyBBYnN0cmFjdE1lc3NhZ2VSZWFkZXIge1xuICAgIGNvbnN0cnVjdG9yKCkge1xuICAgICAgICB0aGlzLmVycm9yRW1pdHRlciA9IG5ldyBldmVudHNfMS5FbWl0dGVyKCk7XG4gICAgICAgIHRoaXMuY2xvc2VFbWl0dGVyID0gbmV3IGV2ZW50c18xLkVtaXR0ZXIoKTtcbiAgICAgICAgdGhpcy5wYXJ0aWFsTWVzc2FnZUVtaXR0ZXIgPSBuZXcgZXZlbnRzXzEuRW1pdHRlcigpO1xuICAgIH1cbiAgICBkaXNwb3NlKCkge1xuICAgICAgICB0aGlzLmVycm9yRW1pdHRlci5kaXNwb3NlKCk7XG4gICAgICAgIHRoaXMuY2xvc2VFbWl0dGVyLmRpc3Bvc2UoKTtcbiAgICB9XG4gICAgZ2V0IG9uRXJyb3IoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLmVycm9yRW1pdHRlci5ldmVudDtcbiAgICB9XG4gICAgZmlyZUVycm9yKGVycm9yKSB7XG4gICAgICAgIHRoaXMuZXJyb3JFbWl0dGVyLmZpcmUodGhpcy5hc0Vycm9yKGVycm9yKSk7XG4gICAgfVxuICAgIGdldCBvbkNsb3NlKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5jbG9zZUVtaXR0ZXIuZXZlbnQ7XG4gICAgfVxuICAgIGZpcmVDbG9zZSgpIHtcbiAgICAgICAgdGhpcy5jbG9zZUVtaXR0ZXIuZmlyZSh1bmRlZmluZWQpO1xuICAgIH1cbiAgICBnZXQgb25QYXJ0aWFsTWVzc2FnZSgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMucGFydGlhbE1lc3NhZ2VFbWl0dGVyLmV2ZW50O1xuICAgIH1cbiAgICBmaXJlUGFydGlhbE1lc3NhZ2UoaW5mbykge1xuICAgICAgICB0aGlzLnBhcnRpYWxNZXNzYWdlRW1pdHRlci5maXJlKGluZm8pO1xuICAgIH1cbiAgICBhc0Vycm9yKGVycm9yKSB7XG4gICAgICAgIGlmIChlcnJvciBpbnN0YW5jZW9mIEVycm9yKSB7XG4gICAgICAgICAgICByZXR1cm4gZXJyb3I7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICByZXR1cm4gbmV3IEVycm9yKGBSZWFkZXIgcmVjZWl2ZWQgZXJyb3IuIFJlYXNvbjogJHtJcy5zdHJpbmcoZXJyb3IubWVzc2FnZSkgPyBlcnJvci5tZXNzYWdlIDogJ3Vua25vd24nfWApO1xuICAgICAgICB9XG4gICAgfVxufVxuZXhwb3J0cy5BYnN0cmFjdE1lc3NhZ2VSZWFkZXIgPSBBYnN0cmFjdE1lc3NhZ2VSZWFkZXI7XG52YXIgUmVzb2x2ZWRNZXNzYWdlUmVhZGVyT3B0aW9ucztcbihmdW5jdGlvbiAoUmVzb2x2ZWRNZXNzYWdlUmVhZGVyT3B0aW9ucykge1xuICAgIGZ1bmN0aW9uIGZyb21PcHRpb25zKG9wdGlvbnMpIHtcbiAgICAgICAgbGV0IGNoYXJzZXQ7XG4gICAgICAgIGxldCByZXN1bHQ7XG4gICAgICAgIGxldCBjb250ZW50RGVjb2RlcjtcbiAgICAgICAgY29uc3QgY29udGVudERlY29kZXJzID0gbmV3IE1hcCgpO1xuICAgICAgICBsZXQgY29udGVudFR5cGVEZWNvZGVyO1xuICAgICAgICBjb25zdCBjb250ZW50VHlwZURlY29kZXJzID0gbmV3IE1hcCgpO1xuICAgICAgICBpZiAob3B0aW9ucyA9PT0gdW5kZWZpbmVkIHx8IHR5cGVvZiBvcHRpb25zID09PSAnc3RyaW5nJykge1xuICAgICAgICAgICAgY2hhcnNldCA9IG9wdGlvbnMgPz8gJ3V0Zi04JztcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIGNoYXJzZXQgPSBvcHRpb25zLmNoYXJzZXQgPz8gJ3V0Zi04JztcbiAgICAgICAgICAgIGlmIChvcHRpb25zLmNvbnRlbnREZWNvZGVyICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICBjb250ZW50RGVjb2RlciA9IG9wdGlvbnMuY29udGVudERlY29kZXI7XG4gICAgICAgICAgICAgICAgY29udGVudERlY29kZXJzLnNldChjb250ZW50RGVjb2Rlci5uYW1lLCBjb250ZW50RGVjb2Rlcik7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAob3B0aW9ucy5jb250ZW50RGVjb2RlcnMgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgIGZvciAoY29uc3QgZGVjb2RlciBvZiBvcHRpb25zLmNvbnRlbnREZWNvZGVycykge1xuICAgICAgICAgICAgICAgICAgICBjb250ZW50RGVjb2RlcnMuc2V0KGRlY29kZXIubmFtZSwgZGVjb2Rlcik7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaWYgKG9wdGlvbnMuY29udGVudFR5cGVEZWNvZGVyICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICBjb250ZW50VHlwZURlY29kZXIgPSBvcHRpb25zLmNvbnRlbnRUeXBlRGVjb2RlcjtcbiAgICAgICAgICAgICAgICBjb250ZW50VHlwZURlY29kZXJzLnNldChjb250ZW50VHlwZURlY29kZXIubmFtZSwgY29udGVudFR5cGVEZWNvZGVyKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmIChvcHRpb25zLmNvbnRlbnRUeXBlRGVjb2RlcnMgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgIGZvciAoY29uc3QgZGVjb2RlciBvZiBvcHRpb25zLmNvbnRlbnRUeXBlRGVjb2RlcnMpIHtcbiAgICAgICAgICAgICAgICAgICAgY29udGVudFR5cGVEZWNvZGVycy5zZXQoZGVjb2Rlci5uYW1lLCBkZWNvZGVyKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgaWYgKGNvbnRlbnRUeXBlRGVjb2RlciA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICBjb250ZW50VHlwZURlY29kZXIgPSAoMCwgcmFsXzEuZGVmYXVsdCkoKS5hcHBsaWNhdGlvbkpzb24uZGVjb2RlcjtcbiAgICAgICAgICAgIGNvbnRlbnRUeXBlRGVjb2RlcnMuc2V0KGNvbnRlbnRUeXBlRGVjb2Rlci5uYW1lLCBjb250ZW50VHlwZURlY29kZXIpO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiB7IGNoYXJzZXQsIGNvbnRlbnREZWNvZGVyLCBjb250ZW50RGVjb2RlcnMsIGNvbnRlbnRUeXBlRGVjb2RlciwgY29udGVudFR5cGVEZWNvZGVycyB9O1xuICAgIH1cbiAgICBSZXNvbHZlZE1lc3NhZ2VSZWFkZXJPcHRpb25zLmZyb21PcHRpb25zID0gZnJvbU9wdGlvbnM7XG59KShSZXNvbHZlZE1lc3NhZ2VSZWFkZXJPcHRpb25zIHx8IChSZXNvbHZlZE1lc3NhZ2VSZWFkZXJPcHRpb25zID0ge30pKTtcbmNsYXNzIFJlYWRhYmxlU3RyZWFtTWVzc2FnZVJlYWRlciBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVJlYWRlciB7XG4gICAgY29uc3RydWN0b3IocmVhZGFibGUsIG9wdGlvbnMpIHtcbiAgICAgICAgc3VwZXIoKTtcbiAgICAgICAgdGhpcy5yZWFkYWJsZSA9IHJlYWRhYmxlO1xuICAgICAgICB0aGlzLm9wdGlvbnMgPSBSZXNvbHZlZE1lc3NhZ2VSZWFkZXJPcHRpb25zLmZyb21PcHRpb25zKG9wdGlvbnMpO1xuICAgICAgICB0aGlzLmJ1ZmZlciA9ICgwLCByYWxfMS5kZWZhdWx0KSgpLm1lc3NhZ2VCdWZmZXIuY3JlYXRlKHRoaXMub3B0aW9ucy5jaGFyc2V0KTtcbiAgICAgICAgdGhpcy5fcGFydGlhbE1lc3NhZ2VUaW1lb3V0ID0gMTAwMDA7XG4gICAgICAgIHRoaXMubmV4dE1lc3NhZ2VMZW5ndGggPSAtMTtcbiAgICAgICAgdGhpcy5tZXNzYWdlVG9rZW4gPSAwO1xuICAgICAgICB0aGlzLnJlYWRTZW1hcGhvcmUgPSBuZXcgc2VtYXBob3JlXzEuU2VtYXBob3JlKDEpO1xuICAgIH1cbiAgICBzZXQgcGFydGlhbE1lc3NhZ2VUaW1lb3V0KHRpbWVvdXQpIHtcbiAgICAgICAgdGhpcy5fcGFydGlhbE1lc3NhZ2VUaW1lb3V0ID0gdGltZW91dDtcbiAgICB9XG4gICAgZ2V0IHBhcnRpYWxNZXNzYWdlVGltZW91dCgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX3BhcnRpYWxNZXNzYWdlVGltZW91dDtcbiAgICB9XG4gICAgbGlzdGVuKGNhbGxiYWNrKSB7XG4gICAgICAgIHRoaXMubmV4dE1lc3NhZ2VMZW5ndGggPSAtMTtcbiAgICAgICAgdGhpcy5tZXNzYWdlVG9rZW4gPSAwO1xuICAgICAgICB0aGlzLnBhcnRpYWxNZXNzYWdlVGltZXIgPSB1bmRlZmluZWQ7XG4gICAgICAgIHRoaXMuY2FsbGJhY2sgPSBjYWxsYmFjaztcbiAgICAgICAgY29uc3QgcmVzdWx0ID0gdGhpcy5yZWFkYWJsZS5vbkRhdGEoKGRhdGEpID0+IHtcbiAgICAgICAgICAgIHRoaXMub25EYXRhKGRhdGEpO1xuICAgICAgICB9KTtcbiAgICAgICAgdGhpcy5yZWFkYWJsZS5vbkVycm9yKChlcnJvcikgPT4gdGhpcy5maXJlRXJyb3IoZXJyb3IpKTtcbiAgICAgICAgdGhpcy5yZWFkYWJsZS5vbkNsb3NlKCgpID0+IHRoaXMuZmlyZUNsb3NlKCkpO1xuICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cbiAgICBvbkRhdGEoZGF0YSkge1xuICAgICAgICB0cnkge1xuICAgICAgICAgICAgdGhpcy5idWZmZXIuYXBwZW5kKGRhdGEpO1xuICAgICAgICAgICAgd2hpbGUgKHRydWUpIHtcbiAgICAgICAgICAgICAgICBpZiAodGhpcy5uZXh0TWVzc2FnZUxlbmd0aCA9PT0gLTEpIHtcbiAgICAgICAgICAgICAgICAgICAgY29uc3QgaGVhZGVycyA9IHRoaXMuYnVmZmVyLnRyeVJlYWRIZWFkZXJzKHRydWUpO1xuICAgICAgICAgICAgICAgICAgICBpZiAoIWhlYWRlcnMpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBjb25zdCBjb250ZW50TGVuZ3RoID0gaGVhZGVycy5nZXQoJ2NvbnRlbnQtbGVuZ3RoJyk7XG4gICAgICAgICAgICAgICAgICAgIGlmICghY29udGVudExlbmd0aCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgdGhpcy5maXJlRXJyb3IobmV3IEVycm9yKGBIZWFkZXIgbXVzdCBwcm92aWRlIGEgQ29udGVudC1MZW5ndGggcHJvcGVydHkuXFxuJHtKU09OLnN0cmluZ2lmeShPYmplY3QuZnJvbUVudHJpZXMoaGVhZGVycykpfWApKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBjb25zdCBsZW5ndGggPSBwYXJzZUludChjb250ZW50TGVuZ3RoKTtcbiAgICAgICAgICAgICAgICAgICAgaWYgKGlzTmFOKGxlbmd0aCkpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHRoaXMuZmlyZUVycm9yKG5ldyBFcnJvcihgQ29udGVudC1MZW5ndGggdmFsdWUgbXVzdCBiZSBhIG51bWJlci4gR290ICR7Y29udGVudExlbmd0aH1gKSk7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgdGhpcy5uZXh0TWVzc2FnZUxlbmd0aCA9IGxlbmd0aDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgY29uc3QgYm9keSA9IHRoaXMuYnVmZmVyLnRyeVJlYWRCb2R5KHRoaXMubmV4dE1lc3NhZ2VMZW5ndGgpO1xuICAgICAgICAgICAgICAgIGlmIChib2R5ID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgLyoqIFdlIGhhdmVuJ3QgcmVjZWl2ZWQgdGhlIGZ1bGwgbWVzc2FnZSB5ZXQuICovXG4gICAgICAgICAgICAgICAgICAgIHRoaXMuc2V0UGFydGlhbE1lc3NhZ2VUaW1lcigpO1xuICAgICAgICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIHRoaXMuY2xlYXJQYXJ0aWFsTWVzc2FnZVRpbWVyKCk7XG4gICAgICAgICAgICAgICAgdGhpcy5uZXh0TWVzc2FnZUxlbmd0aCA9IC0xO1xuICAgICAgICAgICAgICAgIC8vIE1ha2Ugc3VyZSB0aGF0IHdlIGNvbnZlcnQgb25lIHJlY2VpdmVkIG1lc3NhZ2UgYWZ0ZXIgdGhlXG4gICAgICAgICAgICAgICAgLy8gb3RoZXIuIE90aGVyd2lzZSBpdCBjb3VsZCBoYXBwZW4gdGhhdCBhIGRlY29kaW5nIG9mIGEgc2Vjb25kXG4gICAgICAgICAgICAgICAgLy8gc21hbGxlciBtZXNzYWdlIGZpbmlzaGVkIGJlZm9yZSB0aGUgZGVjb2Rpbmcgb2YgYSBmaXJzdCBsYXJnZXJcbiAgICAgICAgICAgICAgICAvLyBtZXNzYWdlIGFuZCB0aGVuIHdlIHdvdWxkIGRlbGl2ZXIgdGhlIHNlY29uZCBtZXNzYWdlIGZpcnN0LlxuICAgICAgICAgICAgICAgIHRoaXMucmVhZFNlbWFwaG9yZS5sb2NrKGFzeW5jICgpID0+IHtcbiAgICAgICAgICAgICAgICAgICAgY29uc3QgYnl0ZXMgPSB0aGlzLm9wdGlvbnMuY29udGVudERlY29kZXIgIT09IHVuZGVmaW5lZFxuICAgICAgICAgICAgICAgICAgICAgICAgPyBhd2FpdCB0aGlzLm9wdGlvbnMuY29udGVudERlY29kZXIuZGVjb2RlKGJvZHkpXG4gICAgICAgICAgICAgICAgICAgICAgICA6IGJvZHk7XG4gICAgICAgICAgICAgICAgICAgIGNvbnN0IG1lc3NhZ2UgPSBhd2FpdCB0aGlzLm9wdGlvbnMuY29udGVudFR5cGVEZWNvZGVyLmRlY29kZShieXRlcywgdGhpcy5vcHRpb25zKTtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5jYWxsYmFjayhtZXNzYWdlKTtcbiAgICAgICAgICAgICAgICB9KS5jYXRjaCgoZXJyb3IpID0+IHtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5maXJlRXJyb3IoZXJyb3IpO1xuICAgICAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIGNhdGNoIChlcnJvcikge1xuICAgICAgICAgICAgdGhpcy5maXJlRXJyb3IoZXJyb3IpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGNsZWFyUGFydGlhbE1lc3NhZ2VUaW1lcigpIHtcbiAgICAgICAgaWYgKHRoaXMucGFydGlhbE1lc3NhZ2VUaW1lcikge1xuICAgICAgICAgICAgdGhpcy5wYXJ0aWFsTWVzc2FnZVRpbWVyLmRpc3Bvc2UoKTtcbiAgICAgICAgICAgIHRoaXMucGFydGlhbE1lc3NhZ2VUaW1lciA9IHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgIH1cbiAgICBzZXRQYXJ0aWFsTWVzc2FnZVRpbWVyKCkge1xuICAgICAgICB0aGlzLmNsZWFyUGFydGlhbE1lc3NhZ2VUaW1lcigpO1xuICAgICAgICBpZiAodGhpcy5fcGFydGlhbE1lc3NhZ2VUaW1lb3V0IDw9IDApIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLnBhcnRpYWxNZXNzYWdlVGltZXIgPSAoMCwgcmFsXzEuZGVmYXVsdCkoKS50aW1lci5zZXRUaW1lb3V0KCh0b2tlbiwgdGltZW91dCkgPT4ge1xuICAgICAgICAgICAgdGhpcy5wYXJ0aWFsTWVzc2FnZVRpbWVyID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgaWYgKHRva2VuID09PSB0aGlzLm1lc3NhZ2VUb2tlbikge1xuICAgICAgICAgICAgICAgIHRoaXMuZmlyZVBhcnRpYWxNZXNzYWdlKHsgbWVzc2FnZVRva2VuOiB0b2tlbiwgd2FpdGluZ1RpbWU6IHRpbWVvdXQgfSk7XG4gICAgICAgICAgICAgICAgdGhpcy5zZXRQYXJ0aWFsTWVzc2FnZVRpbWVyKCk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0sIHRoaXMuX3BhcnRpYWxNZXNzYWdlVGltZW91dCwgdGhpcy5tZXNzYWdlVG9rZW4sIHRoaXMuX3BhcnRpYWxNZXNzYWdlVGltZW91dCk7XG4gICAgfVxufVxuZXhwb3J0cy5SZWFkYWJsZVN0cmVhbU1lc3NhZ2VSZWFkZXIgPSBSZWFkYWJsZVN0cmVhbU1lc3NhZ2VSZWFkZXI7XG4iLCAiXCJ1c2Ugc3RyaWN0XCI7XG4vKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJfX2VzTW9kdWxlXCIsIHsgdmFsdWU6IHRydWUgfSk7XG5leHBvcnRzLldyaXRlYWJsZVN0cmVhbU1lc3NhZ2VXcml0ZXIgPSBleHBvcnRzLkFic3RyYWN0TWVzc2FnZVdyaXRlciA9IGV4cG9ydHMuTWVzc2FnZVdyaXRlciA9IHZvaWQgMDtcbmNvbnN0IHJhbF8xID0gcmVxdWlyZShcIi4vcmFsXCIpO1xuY29uc3QgSXMgPSByZXF1aXJlKFwiLi9pc1wiKTtcbmNvbnN0IHNlbWFwaG9yZV8xID0gcmVxdWlyZShcIi4vc2VtYXBob3JlXCIpO1xuY29uc3QgZXZlbnRzXzEgPSByZXF1aXJlKFwiLi9ldmVudHNcIik7XG5jb25zdCBDb250ZW50TGVuZ3RoID0gJ0NvbnRlbnQtTGVuZ3RoOiAnO1xuY29uc3QgQ1JMRiA9ICdcXHJcXG4nO1xudmFyIE1lc3NhZ2VXcml0ZXI7XG4oZnVuY3Rpb24gKE1lc3NhZ2VXcml0ZXIpIHtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBjYW5kaWRhdGUgJiYgSXMuZnVuYyhjYW5kaWRhdGUuZGlzcG9zZSkgJiYgSXMuZnVuYyhjYW5kaWRhdGUub25DbG9zZSkgJiZcbiAgICAgICAgICAgIElzLmZ1bmMoY2FuZGlkYXRlLm9uRXJyb3IpICYmIElzLmZ1bmMoY2FuZGlkYXRlLndyaXRlKTtcbiAgICB9XG4gICAgTWVzc2FnZVdyaXRlci5pcyA9IGlzO1xufSkoTWVzc2FnZVdyaXRlciB8fCAoZXhwb3J0cy5NZXNzYWdlV3JpdGVyID0gTWVzc2FnZVdyaXRlciA9IHt9KSk7XG5jbGFzcyBBYnN0cmFjdE1lc3NhZ2VXcml0ZXIge1xuICAgIGNvbnN0cnVjdG9yKCkge1xuICAgICAgICB0aGlzLmVycm9yRW1pdHRlciA9IG5ldyBldmVudHNfMS5FbWl0dGVyKCk7XG4gICAgICAgIHRoaXMuY2xvc2VFbWl0dGVyID0gbmV3IGV2ZW50c18xLkVtaXR0ZXIoKTtcbiAgICB9XG4gICAgZGlzcG9zZSgpIHtcbiAgICAgICAgdGhpcy5lcnJvckVtaXR0ZXIuZGlzcG9zZSgpO1xuICAgICAgICB0aGlzLmNsb3NlRW1pdHRlci5kaXNwb3NlKCk7XG4gICAgfVxuICAgIGdldCBvbkVycm9yKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5lcnJvckVtaXR0ZXIuZXZlbnQ7XG4gICAgfVxuICAgIGZpcmVFcnJvcihlcnJvciwgbWVzc2FnZSwgY291bnQpIHtcbiAgICAgICAgdGhpcy5lcnJvckVtaXR0ZXIuZmlyZShbdGhpcy5hc0Vycm9yKGVycm9yKSwgbWVzc2FnZSwgY291bnRdKTtcbiAgICB9XG4gICAgZ2V0IG9uQ2xvc2UoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLmNsb3NlRW1pdHRlci5ldmVudDtcbiAgICB9XG4gICAgZmlyZUNsb3NlKCkge1xuICAgICAgICB0aGlzLmNsb3NlRW1pdHRlci5maXJlKHVuZGVmaW5lZCk7XG4gICAgfVxuICAgIGFzRXJyb3IoZXJyb3IpIHtcbiAgICAgICAgaWYgKGVycm9yIGluc3RhbmNlb2YgRXJyb3IpIHtcbiAgICAgICAgICAgIHJldHVybiBlcnJvcjtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHJldHVybiBuZXcgRXJyb3IoYFdyaXRlciByZWNlaXZlZCBlcnJvci4gUmVhc29uOiAke0lzLnN0cmluZyhlcnJvci5tZXNzYWdlKSA/IGVycm9yLm1lc3NhZ2UgOiAndW5rbm93bid9YCk7XG4gICAgICAgIH1cbiAgICB9XG59XG5leHBvcnRzLkFic3RyYWN0TWVzc2FnZVdyaXRlciA9IEFic3RyYWN0TWVzc2FnZVdyaXRlcjtcbnZhciBSZXNvbHZlZE1lc3NhZ2VXcml0ZXJPcHRpb25zO1xuKGZ1bmN0aW9uIChSZXNvbHZlZE1lc3NhZ2VXcml0ZXJPcHRpb25zKSB7XG4gICAgZnVuY3Rpb24gZnJvbU9wdGlvbnMob3B0aW9ucykge1xuICAgICAgICBpZiAob3B0aW9ucyA9PT0gdW5kZWZpbmVkIHx8IHR5cGVvZiBvcHRpb25zID09PSAnc3RyaW5nJykge1xuICAgICAgICAgICAgcmV0dXJuIHsgY2hhcnNldDogb3B0aW9ucyA/PyAndXRmLTgnLCBjb250ZW50VHlwZUVuY29kZXI6ICgwLCByYWxfMS5kZWZhdWx0KSgpLmFwcGxpY2F0aW9uSnNvbi5lbmNvZGVyIH07XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICByZXR1cm4geyBjaGFyc2V0OiBvcHRpb25zLmNoYXJzZXQgPz8gJ3V0Zi04JywgY29udGVudEVuY29kZXI6IG9wdGlvbnMuY29udGVudEVuY29kZXIsIGNvbnRlbnRUeXBlRW5jb2Rlcjogb3B0aW9ucy5jb250ZW50VHlwZUVuY29kZXIgPz8gKDAsIHJhbF8xLmRlZmF1bHQpKCkuYXBwbGljYXRpb25Kc29uLmVuY29kZXIgfTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBSZXNvbHZlZE1lc3NhZ2VXcml0ZXJPcHRpb25zLmZyb21PcHRpb25zID0gZnJvbU9wdGlvbnM7XG59KShSZXNvbHZlZE1lc3NhZ2VXcml0ZXJPcHRpb25zIHx8IChSZXNvbHZlZE1lc3NhZ2VXcml0ZXJPcHRpb25zID0ge30pKTtcbmNsYXNzIFdyaXRlYWJsZVN0cmVhbU1lc3NhZ2VXcml0ZXIgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VXcml0ZXIge1xuICAgIGNvbnN0cnVjdG9yKHdyaXRhYmxlLCBvcHRpb25zKSB7XG4gICAgICAgIHN1cGVyKCk7XG4gICAgICAgIHRoaXMud3JpdGFibGUgPSB3cml0YWJsZTtcbiAgICAgICAgdGhpcy5vcHRpb25zID0gUmVzb2x2ZWRNZXNzYWdlV3JpdGVyT3B0aW9ucy5mcm9tT3B0aW9ucyhvcHRpb25zKTtcbiAgICAgICAgdGhpcy5lcnJvckNvdW50ID0gMDtcbiAgICAgICAgdGhpcy53cml0ZVNlbWFwaG9yZSA9IG5ldyBzZW1hcGhvcmVfMS5TZW1hcGhvcmUoMSk7XG4gICAgICAgIHRoaXMud3JpdGFibGUub25FcnJvcigoZXJyb3IpID0+IHRoaXMuZmlyZUVycm9yKGVycm9yKSk7XG4gICAgICAgIHRoaXMud3JpdGFibGUub25DbG9zZSgoKSA9PiB0aGlzLmZpcmVDbG9zZSgpKTtcbiAgICB9XG4gICAgYXN5bmMgd3JpdGUobXNnKSB7XG4gICAgICAgIHJldHVybiB0aGlzLndyaXRlU2VtYXBob3JlLmxvY2soYXN5bmMgKCkgPT4ge1xuICAgICAgICAgICAgY29uc3QgcGF5bG9hZCA9IHRoaXMub3B0aW9ucy5jb250ZW50VHlwZUVuY29kZXIuZW5jb2RlKG1zZywgdGhpcy5vcHRpb25zKS50aGVuKChidWZmZXIpID0+IHtcbiAgICAgICAgICAgICAgICBpZiAodGhpcy5vcHRpb25zLmNvbnRlbnRFbmNvZGVyICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIHRoaXMub3B0aW9ucy5jb250ZW50RW5jb2Rlci5lbmNvZGUoYnVmZmVyKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiBidWZmZXI7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICByZXR1cm4gcGF5bG9hZC50aGVuKChidWZmZXIpID0+IHtcbiAgICAgICAgICAgICAgICBjb25zdCBoZWFkZXJzID0gW107XG4gICAgICAgICAgICAgICAgaGVhZGVycy5wdXNoKENvbnRlbnRMZW5ndGgsIGJ1ZmZlci5ieXRlTGVuZ3RoLnRvU3RyaW5nKCksIENSTEYpO1xuICAgICAgICAgICAgICAgIGhlYWRlcnMucHVzaChDUkxGKTtcbiAgICAgICAgICAgICAgICByZXR1cm4gdGhpcy5kb1dyaXRlKG1zZywgaGVhZGVycywgYnVmZmVyKTtcbiAgICAgICAgICAgIH0sIChlcnJvcikgPT4ge1xuICAgICAgICAgICAgICAgIHRoaXMuZmlyZUVycm9yKGVycm9yKTtcbiAgICAgICAgICAgICAgICB0aHJvdyBlcnJvcjtcbiAgICAgICAgICAgIH0pO1xuICAgICAgICB9KTtcbiAgICB9XG4gICAgYXN5bmMgZG9Xcml0ZShtc2csIGhlYWRlcnMsIGRhdGEpIHtcbiAgICAgICAgdHJ5IHtcbiAgICAgICAgICAgIGF3YWl0IHRoaXMud3JpdGFibGUud3JpdGUoaGVhZGVycy5qb2luKCcnKSwgJ2FzY2lpJyk7XG4gICAgICAgICAgICByZXR1cm4gdGhpcy53cml0YWJsZS53cml0ZShkYXRhKTtcbiAgICAgICAgfVxuICAgICAgICBjYXRjaCAoZXJyb3IpIHtcbiAgICAgICAgICAgIHRoaXMuaGFuZGxlRXJyb3IoZXJyb3IsIG1zZyk7XG4gICAgICAgICAgICByZXR1cm4gUHJvbWlzZS5yZWplY3QoZXJyb3IpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGhhbmRsZUVycm9yKGVycm9yLCBtc2cpIHtcbiAgICAgICAgdGhpcy5lcnJvckNvdW50Kys7XG4gICAgICAgIHRoaXMuZmlyZUVycm9yKGVycm9yLCBtc2csIHRoaXMuZXJyb3JDb3VudCk7XG4gICAgfVxuICAgIGVuZCgpIHtcbiAgICAgICAgdGhpcy53cml0YWJsZS5lbmQoKTtcbiAgICB9XG59XG5leHBvcnRzLldyaXRlYWJsZVN0cmVhbU1lc3NhZ2VXcml0ZXIgPSBXcml0ZWFibGVTdHJlYW1NZXNzYWdlV3JpdGVyO1xuIiwgIlwidXNlIHN0cmljdFwiO1xuLyotLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbiAqICBDb3B5cmlnaHQgKGMpIE1pY3Jvc29mdCBDb3Jwb3JhdGlvbi4gQWxsIHJpZ2h0cyByZXNlcnZlZC5cbiAqICBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICotLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSovXG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJfX2VzTW9kdWxlXCIsIHsgdmFsdWU6IHRydWUgfSk7XG5leHBvcnRzLkFic3RyYWN0TWVzc2FnZUJ1ZmZlciA9IHZvaWQgMDtcbmNvbnN0IENSID0gMTM7XG5jb25zdCBMRiA9IDEwO1xuY29uc3QgQ1JMRiA9ICdcXHJcXG4nO1xuY2xhc3MgQWJzdHJhY3RNZXNzYWdlQnVmZmVyIHtcbiAgICBjb25zdHJ1Y3RvcihlbmNvZGluZyA9ICd1dGYtOCcpIHtcbiAgICAgICAgdGhpcy5fZW5jb2RpbmcgPSBlbmNvZGluZztcbiAgICAgICAgdGhpcy5fY2h1bmtzID0gW107XG4gICAgICAgIHRoaXMuX3RvdGFsTGVuZ3RoID0gMDtcbiAgICB9XG4gICAgZ2V0IGVuY29kaW5nKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5fZW5jb2Rpbmc7XG4gICAgfVxuICAgIGFwcGVuZChjaHVuaykge1xuICAgICAgICBjb25zdCB0b0FwcGVuZCA9IHR5cGVvZiBjaHVuayA9PT0gJ3N0cmluZycgPyB0aGlzLmZyb21TdHJpbmcoY2h1bmssIHRoaXMuX2VuY29kaW5nKSA6IGNodW5rO1xuICAgICAgICB0aGlzLl9jaHVua3MucHVzaCh0b0FwcGVuZCk7XG4gICAgICAgIHRoaXMuX3RvdGFsTGVuZ3RoICs9IHRvQXBwZW5kLmJ5dGVMZW5ndGg7XG4gICAgfVxuICAgIHRyeVJlYWRIZWFkZXJzKGxvd2VyQ2FzZUtleXMgPSBmYWxzZSkge1xuICAgICAgICBpZiAodGhpcy5fY2h1bmtzLmxlbmd0aCA9PT0gMCkge1xuICAgICAgICAgICAgcmV0dXJuIHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgICAgICBsZXQgc3RhdGUgPSAwO1xuICAgICAgICBsZXQgY2h1bmtJbmRleCA9IDA7XG4gICAgICAgIGxldCBvZmZzZXQgPSAwO1xuICAgICAgICBsZXQgY2h1bmtCeXRlc1JlYWQgPSAwO1xuICAgICAgICByb3c6IHdoaWxlIChjaHVua0luZGV4IDwgdGhpcy5fY2h1bmtzLmxlbmd0aCkge1xuICAgICAgICAgICAgY29uc3QgY2h1bmsgPSB0aGlzLl9jaHVua3NbY2h1bmtJbmRleF07XG4gICAgICAgICAgICBvZmZzZXQgPSAwO1xuICAgICAgICAgICAgY29sdW1uOiB3aGlsZSAob2Zmc2V0IDwgY2h1bmsubGVuZ3RoKSB7XG4gICAgICAgICAgICAgICAgY29uc3QgdmFsdWUgPSBjaHVua1tvZmZzZXRdO1xuICAgICAgICAgICAgICAgIHN3aXRjaCAodmFsdWUpIHtcbiAgICAgICAgICAgICAgICAgICAgY2FzZSBDUjpcbiAgICAgICAgICAgICAgICAgICAgICAgIHN3aXRjaCAoc3RhdGUpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBjYXNlIDA6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHN0YXRlID0gMTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgY2FzZSAyOlxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBzdGF0ZSA9IDM7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGRlZmF1bHQ6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHN0YXRlID0gMDtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICBjYXNlIExGOlxuICAgICAgICAgICAgICAgICAgICAgICAgc3dpdGNoIChzdGF0ZSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNhc2UgMTpcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgc3RhdGUgPSAyO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBjYXNlIDM6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHN0YXRlID0gNDtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgb2Zmc2V0Kys7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrIHJvdztcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBkZWZhdWx0OlxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBzdGF0ZSA9IDA7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICAgICAgZGVmYXVsdDpcbiAgICAgICAgICAgICAgICAgICAgICAgIHN0YXRlID0gMDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgb2Zmc2V0Kys7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBjaHVua0J5dGVzUmVhZCArPSBjaHVuay5ieXRlTGVuZ3RoO1xuICAgICAgICAgICAgY2h1bmtJbmRleCsrO1xuICAgICAgICB9XG4gICAgICAgIGlmIChzdGF0ZSAhPT0gNCkge1xuICAgICAgICAgICAgcmV0dXJuIHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgICAgICAvLyBUaGUgYnVmZmVyIGNvbnRhaW5zIHRoZSB0d28gQ1JMRiBhdCB0aGUgZW5kLiBTbyB3ZSB3aWxsXG4gICAgICAgIC8vIGhhdmUgdHdvIGVtcHR5IGxpbmVzIGFmdGVyIHRoZSBzcGxpdCBhdCB0aGUgZW5kIGFzIHdlbGwuXG4gICAgICAgIGNvbnN0IGJ1ZmZlciA9IHRoaXMuX3JlYWQoY2h1bmtCeXRlc1JlYWQgKyBvZmZzZXQpO1xuICAgICAgICBjb25zdCByZXN1bHQgPSBuZXcgTWFwKCk7XG4gICAgICAgIGNvbnN0IGhlYWRlcnMgPSB0aGlzLnRvU3RyaW5nKGJ1ZmZlciwgJ2FzY2lpJykuc3BsaXQoQ1JMRik7XG4gICAgICAgIGlmIChoZWFkZXJzLmxlbmd0aCA8IDIpIHtcbiAgICAgICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgICAgIH1cbiAgICAgICAgZm9yIChsZXQgaSA9IDA7IGkgPCBoZWFkZXJzLmxlbmd0aCAtIDI7IGkrKykge1xuICAgICAgICAgICAgY29uc3QgaGVhZGVyID0gaGVhZGVyc1tpXTtcbiAgICAgICAgICAgIGNvbnN0IGluZGV4ID0gaGVhZGVyLmluZGV4T2YoJzonKTtcbiAgICAgICAgICAgIGlmIChpbmRleCA9PT0gLTEpIHtcbiAgICAgICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYE1lc3NhZ2UgaGVhZGVyIG11c3Qgc2VwYXJhdGUga2V5IGFuZCB2YWx1ZSB1c2luZyAnOidcXG4ke2hlYWRlcn1gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNvbnN0IGtleSA9IGhlYWRlci5zdWJzdHIoMCwgaW5kZXgpO1xuICAgICAgICAgICAgY29uc3QgdmFsdWUgPSBoZWFkZXIuc3Vic3RyKGluZGV4ICsgMSkudHJpbSgpO1xuICAgICAgICAgICAgcmVzdWx0LnNldChsb3dlckNhc2VLZXlzID8ga2V5LnRvTG93ZXJDYXNlKCkgOiBrZXksIHZhbHVlKTtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cbiAgICB0cnlSZWFkQm9keShsZW5ndGgpIHtcbiAgICAgICAgaWYgKHRoaXMuX3RvdGFsTGVuZ3RoIDwgbGVuZ3RoKSB7XG4gICAgICAgICAgICByZXR1cm4gdW5kZWZpbmVkO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiB0aGlzLl9yZWFkKGxlbmd0aCk7XG4gICAgfVxuICAgIGdldCBudW1iZXJPZkJ5dGVzKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5fdG90YWxMZW5ndGg7XG4gICAgfVxuICAgIF9yZWFkKGJ5dGVDb3VudCkge1xuICAgICAgICBpZiAoYnl0ZUNvdW50ID09PSAwKSB7XG4gICAgICAgICAgICByZXR1cm4gdGhpcy5lbXB0eUJ1ZmZlcigpO1xuICAgICAgICB9XG4gICAgICAgIGlmIChieXRlQ291bnQgPiB0aGlzLl90b3RhbExlbmd0aCkge1xuICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBDYW5ub3QgcmVhZCBzbyBtYW55IGJ5dGVzIWApO1xuICAgICAgICB9XG4gICAgICAgIGlmICh0aGlzLl9jaHVua3NbMF0uYnl0ZUxlbmd0aCA9PT0gYnl0ZUNvdW50KSB7XG4gICAgICAgICAgICAvLyBzdXBlciBmYXN0IHBhdGgsIHByZWNpc2VseSBmaXJzdCBjaHVuayBtdXN0IGJlIHJldHVybmVkXG4gICAgICAgICAgICBjb25zdCBjaHVuayA9IHRoaXMuX2NodW5rc1swXTtcbiAgICAgICAgICAgIHRoaXMuX2NodW5rcy5zaGlmdCgpO1xuICAgICAgICAgICAgdGhpcy5fdG90YWxMZW5ndGggLT0gYnl0ZUNvdW50O1xuICAgICAgICAgICAgcmV0dXJuIHRoaXMuYXNOYXRpdmUoY2h1bmspO1xuICAgICAgICB9XG4gICAgICAgIGlmICh0aGlzLl9jaHVua3NbMF0uYnl0ZUxlbmd0aCA+IGJ5dGVDb3VudCkge1xuICAgICAgICAgICAgLy8gZmFzdCBwYXRoLCB0aGUgcmVhZGluZyBpcyBlbnRpcmVseSB3aXRoaW4gdGhlIGZpcnN0IGNodW5rXG4gICAgICAgICAgICBjb25zdCBjaHVuayA9IHRoaXMuX2NodW5rc1swXTtcbiAgICAgICAgICAgIGNvbnN0IHJlc3VsdCA9IHRoaXMuYXNOYXRpdmUoY2h1bmssIGJ5dGVDb3VudCk7XG4gICAgICAgICAgICB0aGlzLl9jaHVua3NbMF0gPSBjaHVuay5zbGljZShieXRlQ291bnQpO1xuICAgICAgICAgICAgdGhpcy5fdG90YWxMZW5ndGggLT0gYnl0ZUNvdW50O1xuICAgICAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICAgICAgfVxuICAgICAgICBjb25zdCByZXN1bHQgPSB0aGlzLmFsbG9jTmF0aXZlKGJ5dGVDb3VudCk7XG4gICAgICAgIGxldCByZXN1bHRPZmZzZXQgPSAwO1xuICAgICAgICBsZXQgY2h1bmtJbmRleCA9IDA7XG4gICAgICAgIHdoaWxlIChieXRlQ291bnQgPiAwKSB7XG4gICAgICAgICAgICBjb25zdCBjaHVuayA9IHRoaXMuX2NodW5rc1tjaHVua0luZGV4XTtcbiAgICAgICAgICAgIGlmIChjaHVuay5ieXRlTGVuZ3RoID4gYnl0ZUNvdW50KSB7XG4gICAgICAgICAgICAgICAgLy8gdGhpcyBjaHVuayB3aWxsIHN1cnZpdmVcbiAgICAgICAgICAgICAgICBjb25zdCBjaHVua1BhcnQgPSBjaHVuay5zbGljZSgwLCBieXRlQ291bnQpO1xuICAgICAgICAgICAgICAgIHJlc3VsdC5zZXQoY2h1bmtQYXJ0LCByZXN1bHRPZmZzZXQpO1xuICAgICAgICAgICAgICAgIHJlc3VsdE9mZnNldCArPSBieXRlQ291bnQ7XG4gICAgICAgICAgICAgICAgdGhpcy5fY2h1bmtzW2NodW5rSW5kZXhdID0gY2h1bmsuc2xpY2UoYnl0ZUNvdW50KTtcbiAgICAgICAgICAgICAgICB0aGlzLl90b3RhbExlbmd0aCAtPSBieXRlQ291bnQ7XG4gICAgICAgICAgICAgICAgYnl0ZUNvdW50IC09IGJ5dGVDb3VudDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIC8vIHRoaXMgY2h1bmsgd2lsbCBiZSBlbnRpcmVseSByZWFkXG4gICAgICAgICAgICAgICAgcmVzdWx0LnNldChjaHVuaywgcmVzdWx0T2Zmc2V0KTtcbiAgICAgICAgICAgICAgICByZXN1bHRPZmZzZXQgKz0gY2h1bmsuYnl0ZUxlbmd0aDtcbiAgICAgICAgICAgICAgICB0aGlzLl9jaHVua3Muc2hpZnQoKTtcbiAgICAgICAgICAgICAgICB0aGlzLl90b3RhbExlbmd0aCAtPSBjaHVuay5ieXRlTGVuZ3RoO1xuICAgICAgICAgICAgICAgIGJ5dGVDb3VudCAtPSBjaHVuay5ieXRlTGVuZ3RoO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxufVxuZXhwb3J0cy5BYnN0cmFjdE1lc3NhZ2VCdWZmZXIgPSBBYnN0cmFjdE1lc3NhZ2VCdWZmZXI7XG4iLCAiXCJ1c2Ugc3RyaWN0XCI7XG4vKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJfX2VzTW9kdWxlXCIsIHsgdmFsdWU6IHRydWUgfSk7XG5leHBvcnRzLmNyZWF0ZU1lc3NhZ2VDb25uZWN0aW9uID0gZXhwb3J0cy5Db25uZWN0aW9uT3B0aW9ucyA9IGV4cG9ydHMuTWVzc2FnZVN0cmF0ZWd5ID0gZXhwb3J0cy5DYW5jZWxsYXRpb25TdHJhdGVneSA9IGV4cG9ydHMuQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3kgPSBleHBvcnRzLkNhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kgPSBleHBvcnRzLlJlcXVlc3RDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5ID0gZXhwb3J0cy5JZENhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kgPSBleHBvcnRzLkNvbm5lY3Rpb25TdHJhdGVneSA9IGV4cG9ydHMuQ29ubmVjdGlvbkVycm9yID0gZXhwb3J0cy5Db25uZWN0aW9uRXJyb3JzID0gZXhwb3J0cy5Mb2dUcmFjZU5vdGlmaWNhdGlvbiA9IGV4cG9ydHMuU2V0VHJhY2VOb3RpZmljYXRpb24gPSBleHBvcnRzLlRyYWNlRm9ybWF0ID0gZXhwb3J0cy5UcmFjZVZhbHVlcyA9IGV4cG9ydHMuVHJhY2UgPSBleHBvcnRzLk51bGxMb2dnZXIgPSBleHBvcnRzLlByb2dyZXNzVHlwZSA9IGV4cG9ydHMuUHJvZ3Jlc3NUb2tlbiA9IHZvaWQgMDtcbmNvbnN0IHJhbF8xID0gcmVxdWlyZShcIi4vcmFsXCIpO1xuY29uc3QgSXMgPSByZXF1aXJlKFwiLi9pc1wiKTtcbmNvbnN0IG1lc3NhZ2VzXzEgPSByZXF1aXJlKFwiLi9tZXNzYWdlc1wiKTtcbmNvbnN0IGxpbmtlZE1hcF8xID0gcmVxdWlyZShcIi4vbGlua2VkTWFwXCIpO1xuY29uc3QgZXZlbnRzXzEgPSByZXF1aXJlKFwiLi9ldmVudHNcIik7XG5jb25zdCBjYW5jZWxsYXRpb25fMSA9IHJlcXVpcmUoXCIuL2NhbmNlbGxhdGlvblwiKTtcbnZhciBDYW5jZWxOb3RpZmljYXRpb247XG4oZnVuY3Rpb24gKENhbmNlbE5vdGlmaWNhdGlvbikge1xuICAgIENhbmNlbE5vdGlmaWNhdGlvbi50eXBlID0gbmV3IG1lc3NhZ2VzXzEuTm90aWZpY2F0aW9uVHlwZSgnJC9jYW5jZWxSZXF1ZXN0Jyk7XG59KShDYW5jZWxOb3RpZmljYXRpb24gfHwgKENhbmNlbE5vdGlmaWNhdGlvbiA9IHt9KSk7XG52YXIgUHJvZ3Jlc3NUb2tlbjtcbihmdW5jdGlvbiAoUHJvZ3Jlc3NUb2tlbikge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiB0eXBlb2YgdmFsdWUgPT09ICdzdHJpbmcnIHx8IHR5cGVvZiB2YWx1ZSA9PT0gJ251bWJlcic7XG4gICAgfVxuICAgIFByb2dyZXNzVG9rZW4uaXMgPSBpcztcbn0pKFByb2dyZXNzVG9rZW4gfHwgKGV4cG9ydHMuUHJvZ3Jlc3NUb2tlbiA9IFByb2dyZXNzVG9rZW4gPSB7fSkpO1xudmFyIFByb2dyZXNzTm90aWZpY2F0aW9uO1xuKGZ1bmN0aW9uIChQcm9ncmVzc05vdGlmaWNhdGlvbikge1xuICAgIFByb2dyZXNzTm90aWZpY2F0aW9uLnR5cGUgPSBuZXcgbWVzc2FnZXNfMS5Ob3RpZmljYXRpb25UeXBlKCckL3Byb2dyZXNzJyk7XG59KShQcm9ncmVzc05vdGlmaWNhdGlvbiB8fCAoUHJvZ3Jlc3NOb3RpZmljYXRpb24gPSB7fSkpO1xuY2xhc3MgUHJvZ3Jlc3NUeXBlIHtcbiAgICBjb25zdHJ1Y3RvcigpIHtcbiAgICB9XG59XG5leHBvcnRzLlByb2dyZXNzVHlwZSA9IFByb2dyZXNzVHlwZTtcbnZhciBTdGFyUmVxdWVzdEhhbmRsZXI7XG4oZnVuY3Rpb24gKFN0YXJSZXF1ZXN0SGFuZGxlcikge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiBJcy5mdW5jKHZhbHVlKTtcbiAgICB9XG4gICAgU3RhclJlcXVlc3RIYW5kbGVyLmlzID0gaXM7XG59KShTdGFyUmVxdWVzdEhhbmRsZXIgfHwgKFN0YXJSZXF1ZXN0SGFuZGxlciA9IHt9KSk7XG5leHBvcnRzLk51bGxMb2dnZXIgPSBPYmplY3QuZnJlZXplKHtcbiAgICBlcnJvcjogKCkgPT4geyB9LFxuICAgIHdhcm46ICgpID0+IHsgfSxcbiAgICBpbmZvOiAoKSA9PiB7IH0sXG4gICAgbG9nOiAoKSA9PiB7IH1cbn0pO1xudmFyIFRyYWNlO1xuKGZ1bmN0aW9uIChUcmFjZSkge1xuICAgIFRyYWNlW1RyYWNlW1wiT2ZmXCJdID0gMF0gPSBcIk9mZlwiO1xuICAgIFRyYWNlW1RyYWNlW1wiTWVzc2FnZXNcIl0gPSAxXSA9IFwiTWVzc2FnZXNcIjtcbiAgICBUcmFjZVtUcmFjZVtcIkNvbXBhY3RcIl0gPSAyXSA9IFwiQ29tcGFjdFwiO1xuICAgIFRyYWNlW1RyYWNlW1wiVmVyYm9zZVwiXSA9IDNdID0gXCJWZXJib3NlXCI7XG59KShUcmFjZSB8fCAoZXhwb3J0cy5UcmFjZSA9IFRyYWNlID0ge30pKTtcbnZhciBUcmFjZVZhbHVlcztcbihmdW5jdGlvbiAoVHJhY2VWYWx1ZXMpIHtcbiAgICAvKipcbiAgICAgKiBUdXJuIHRyYWNpbmcgb2ZmLlxuICAgICAqL1xuICAgIFRyYWNlVmFsdWVzLk9mZiA9ICdvZmYnO1xuICAgIC8qKlxuICAgICAqIFRyYWNlIG1lc3NhZ2VzIG9ubHkuXG4gICAgICovXG4gICAgVHJhY2VWYWx1ZXMuTWVzc2FnZXMgPSAnbWVzc2FnZXMnO1xuICAgIC8qKlxuICAgICAqIENvbXBhY3QgbWVzc2FnZSB0cmFjaW5nLlxuICAgICAqL1xuICAgIFRyYWNlVmFsdWVzLkNvbXBhY3QgPSAnY29tcGFjdCc7XG4gICAgLyoqXG4gICAgICogVmVyYm9zZSBtZXNzYWdlIHRyYWNpbmcuXG4gICAgICovXG4gICAgVHJhY2VWYWx1ZXMuVmVyYm9zZSA9ICd2ZXJib3NlJztcbn0pKFRyYWNlVmFsdWVzIHx8IChleHBvcnRzLlRyYWNlVmFsdWVzID0gVHJhY2VWYWx1ZXMgPSB7fSkpO1xuKGZ1bmN0aW9uIChUcmFjZSkge1xuICAgIGZ1bmN0aW9uIGZyb21TdHJpbmcodmFsdWUpIHtcbiAgICAgICAgaWYgKCFJcy5zdHJpbmcodmFsdWUpKSB7XG4gICAgICAgICAgICByZXR1cm4gVHJhY2UuT2ZmO1xuICAgICAgICB9XG4gICAgICAgIHZhbHVlID0gdmFsdWUudG9Mb3dlckNhc2UoKTtcbiAgICAgICAgc3dpdGNoICh2YWx1ZSkge1xuICAgICAgICAgICAgY2FzZSAnb2ZmJzpcbiAgICAgICAgICAgICAgICByZXR1cm4gVHJhY2UuT2ZmO1xuICAgICAgICAgICAgY2FzZSAnbWVzc2FnZXMnOlxuICAgICAgICAgICAgICAgIHJldHVybiBUcmFjZS5NZXNzYWdlcztcbiAgICAgICAgICAgIGNhc2UgJ2NvbXBhY3QnOlxuICAgICAgICAgICAgICAgIHJldHVybiBUcmFjZS5Db21wYWN0O1xuICAgICAgICAgICAgY2FzZSAndmVyYm9zZSc6XG4gICAgICAgICAgICAgICAgcmV0dXJuIFRyYWNlLlZlcmJvc2U7XG4gICAgICAgICAgICBkZWZhdWx0OlxuICAgICAgICAgICAgICAgIHJldHVybiBUcmFjZS5PZmY7XG4gICAgICAgIH1cbiAgICB9XG4gICAgVHJhY2UuZnJvbVN0cmluZyA9IGZyb21TdHJpbmc7XG4gICAgZnVuY3Rpb24gdG9TdHJpbmcodmFsdWUpIHtcbiAgICAgICAgc3dpdGNoICh2YWx1ZSkge1xuICAgICAgICAgICAgY2FzZSBUcmFjZS5PZmY6XG4gICAgICAgICAgICAgICAgcmV0dXJuICdvZmYnO1xuICAgICAgICAgICAgY2FzZSBUcmFjZS5NZXNzYWdlczpcbiAgICAgICAgICAgICAgICByZXR1cm4gJ21lc3NhZ2VzJztcbiAgICAgICAgICAgIGNhc2UgVHJhY2UuQ29tcGFjdDpcbiAgICAgICAgICAgICAgICByZXR1cm4gJ2NvbXBhY3QnO1xuICAgICAgICAgICAgY2FzZSBUcmFjZS5WZXJib3NlOlxuICAgICAgICAgICAgICAgIHJldHVybiAndmVyYm9zZSc7XG4gICAgICAgICAgICBkZWZhdWx0OlxuICAgICAgICAgICAgICAgIHJldHVybiAnb2ZmJztcbiAgICAgICAgfVxuICAgIH1cbiAgICBUcmFjZS50b1N0cmluZyA9IHRvU3RyaW5nO1xufSkoVHJhY2UgfHwgKGV4cG9ydHMuVHJhY2UgPSBUcmFjZSA9IHt9KSk7XG52YXIgVHJhY2VGb3JtYXQ7XG4oZnVuY3Rpb24gKFRyYWNlRm9ybWF0KSB7XG4gICAgVHJhY2VGb3JtYXRbXCJUZXh0XCJdID0gXCJ0ZXh0XCI7XG4gICAgVHJhY2VGb3JtYXRbXCJKU09OXCJdID0gXCJqc29uXCI7XG59KShUcmFjZUZvcm1hdCB8fCAoZXhwb3J0cy5UcmFjZUZvcm1hdCA9IFRyYWNlRm9ybWF0ID0ge30pKTtcbihmdW5jdGlvbiAoVHJhY2VGb3JtYXQpIHtcbiAgICBmdW5jdGlvbiBmcm9tU3RyaW5nKHZhbHVlKSB7XG4gICAgICAgIGlmICghSXMuc3RyaW5nKHZhbHVlKSkge1xuICAgICAgICAgICAgcmV0dXJuIFRyYWNlRm9ybWF0LlRleHQ7XG4gICAgICAgIH1cbiAgICAgICAgdmFsdWUgPSB2YWx1ZS50b0xvd2VyQ2FzZSgpO1xuICAgICAgICBpZiAodmFsdWUgPT09ICdqc29uJykge1xuICAgICAgICAgICAgcmV0dXJuIFRyYWNlRm9ybWF0LkpTT047XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICByZXR1cm4gVHJhY2VGb3JtYXQuVGV4dDtcbiAgICAgICAgfVxuICAgIH1cbiAgICBUcmFjZUZvcm1hdC5mcm9tU3RyaW5nID0gZnJvbVN0cmluZztcbn0pKFRyYWNlRm9ybWF0IHx8IChleHBvcnRzLlRyYWNlRm9ybWF0ID0gVHJhY2VGb3JtYXQgPSB7fSkpO1xudmFyIFNldFRyYWNlTm90aWZpY2F0aW9uO1xuKGZ1bmN0aW9uIChTZXRUcmFjZU5vdGlmaWNhdGlvbikge1xuICAgIFNldFRyYWNlTm90aWZpY2F0aW9uLnR5cGUgPSBuZXcgbWVzc2FnZXNfMS5Ob3RpZmljYXRpb25UeXBlKCckL3NldFRyYWNlJyk7XG59KShTZXRUcmFjZU5vdGlmaWNhdGlvbiB8fCAoZXhwb3J0cy5TZXRUcmFjZU5vdGlmaWNhdGlvbiA9IFNldFRyYWNlTm90aWZpY2F0aW9uID0ge30pKTtcbnZhciBMb2dUcmFjZU5vdGlmaWNhdGlvbjtcbihmdW5jdGlvbiAoTG9nVHJhY2VOb3RpZmljYXRpb24pIHtcbiAgICBMb2dUcmFjZU5vdGlmaWNhdGlvbi50eXBlID0gbmV3IG1lc3NhZ2VzXzEuTm90aWZpY2F0aW9uVHlwZSgnJC9sb2dUcmFjZScpO1xufSkoTG9nVHJhY2VOb3RpZmljYXRpb24gfHwgKGV4cG9ydHMuTG9nVHJhY2VOb3RpZmljYXRpb24gPSBMb2dUcmFjZU5vdGlmaWNhdGlvbiA9IHt9KSk7XG52YXIgQ29ubmVjdGlvbkVycm9ycztcbihmdW5jdGlvbiAoQ29ubmVjdGlvbkVycm9ycykge1xuICAgIC8qKlxuICAgICAqIFRoZSBjb25uZWN0aW9uIGlzIGNsb3NlZC5cbiAgICAgKi9cbiAgICBDb25uZWN0aW9uRXJyb3JzW0Nvbm5lY3Rpb25FcnJvcnNbXCJDbG9zZWRcIl0gPSAxXSA9IFwiQ2xvc2VkXCI7XG4gICAgLyoqXG4gICAgICogVGhlIGNvbm5lY3Rpb24gZ290IGRpc3Bvc2VkLlxuICAgICAqL1xuICAgIENvbm5lY3Rpb25FcnJvcnNbQ29ubmVjdGlvbkVycm9yc1tcIkRpc3Bvc2VkXCJdID0gMl0gPSBcIkRpc3Bvc2VkXCI7XG4gICAgLyoqXG4gICAgICogVGhlIGNvbm5lY3Rpb24gaXMgYWxyZWFkeSBpbiBsaXN0ZW5pbmcgbW9kZS5cbiAgICAgKi9cbiAgICBDb25uZWN0aW9uRXJyb3JzW0Nvbm5lY3Rpb25FcnJvcnNbXCJBbHJlYWR5TGlzdGVuaW5nXCJdID0gM10gPSBcIkFscmVhZHlMaXN0ZW5pbmdcIjtcbn0pKENvbm5lY3Rpb25FcnJvcnMgfHwgKGV4cG9ydHMuQ29ubmVjdGlvbkVycm9ycyA9IENvbm5lY3Rpb25FcnJvcnMgPSB7fSkpO1xuY2xhc3MgQ29ubmVjdGlvbkVycm9yIGV4dGVuZHMgRXJyb3Ige1xuICAgIGNvbnN0cnVjdG9yKGNvZGUsIG1lc3NhZ2UpIHtcbiAgICAgICAgc3VwZXIobWVzc2FnZSk7XG4gICAgICAgIHRoaXMuY29kZSA9IGNvZGU7XG4gICAgICAgIE9iamVjdC5zZXRQcm90b3R5cGVPZih0aGlzLCBDb25uZWN0aW9uRXJyb3IucHJvdG90eXBlKTtcbiAgICB9XG59XG5leHBvcnRzLkNvbm5lY3Rpb25FcnJvciA9IENvbm5lY3Rpb25FcnJvcjtcbnZhciBDb25uZWN0aW9uU3RyYXRlZ3k7XG4oZnVuY3Rpb24gKENvbm5lY3Rpb25TdHJhdGVneSkge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICYmIElzLmZ1bmMoY2FuZGlkYXRlLmNhbmNlbFVuZGlzcGF0Y2hlZCk7XG4gICAgfVxuICAgIENvbm5lY3Rpb25TdHJhdGVneS5pcyA9IGlzO1xufSkoQ29ubmVjdGlvblN0cmF0ZWd5IHx8IChleHBvcnRzLkNvbm5lY3Rpb25TdHJhdGVneSA9IENvbm5lY3Rpb25TdHJhdGVneSA9IHt9KSk7XG52YXIgSWRDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5O1xuKGZ1bmN0aW9uIChJZENhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kpIHtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiAoY2FuZGlkYXRlLmtpbmQgPT09IHVuZGVmaW5lZCB8fCBjYW5kaWRhdGUua2luZCA9PT0gJ2lkJykgJiYgSXMuZnVuYyhjYW5kaWRhdGUuY3JlYXRlQ2FuY2VsbGF0aW9uVG9rZW5Tb3VyY2UpICYmIChjYW5kaWRhdGUuZGlzcG9zZSA9PT0gdW5kZWZpbmVkIHx8IElzLmZ1bmMoY2FuZGlkYXRlLmRpc3Bvc2UpKTtcbiAgICB9XG4gICAgSWRDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5LmlzID0gaXM7XG59KShJZENhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kgfHwgKGV4cG9ydHMuSWRDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5ID0gSWRDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5ID0ge30pKTtcbnZhciBSZXF1ZXN0Q2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneTtcbihmdW5jdGlvbiAoUmVxdWVzdENhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kpIHtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiBjYW5kaWRhdGUua2luZCA9PT0gJ3JlcXVlc3QnICYmIElzLmZ1bmMoY2FuZGlkYXRlLmNyZWF0ZUNhbmNlbGxhdGlvblRva2VuU291cmNlKSAmJiAoY2FuZGlkYXRlLmRpc3Bvc2UgPT09IHVuZGVmaW5lZCB8fCBJcy5mdW5jKGNhbmRpZGF0ZS5kaXNwb3NlKSk7XG4gICAgfVxuICAgIFJlcXVlc3RDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5LmlzID0gaXM7XG59KShSZXF1ZXN0Q2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneSB8fCAoZXhwb3J0cy5SZXF1ZXN0Q2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneSA9IFJlcXVlc3RDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5ID0ge30pKTtcbnZhciBDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5O1xuKGZ1bmN0aW9uIChDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5KSB7XG4gICAgQ2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneS5NZXNzYWdlID0gT2JqZWN0LmZyZWV6ZSh7XG4gICAgICAgIGNyZWF0ZUNhbmNlbGxhdGlvblRva2VuU291cmNlKF8pIHtcbiAgICAgICAgICAgIHJldHVybiBuZXcgY2FuY2VsbGF0aW9uXzEuQ2FuY2VsbGF0aW9uVG9rZW5Tb3VyY2UoKTtcbiAgICAgICAgfVxuICAgIH0pO1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiBJZENhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kuaXModmFsdWUpIHx8IFJlcXVlc3RDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5LmlzKHZhbHVlKTtcbiAgICB9XG4gICAgQ2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneS5pcyA9IGlzO1xufSkoQ2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneSB8fCAoZXhwb3J0cy5DYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5ID0gQ2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneSA9IHt9KSk7XG52YXIgQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3k7XG4oZnVuY3Rpb24gKENhbmNlbGxhdGlvblNlbmRlclN0cmF0ZWd5KSB7XG4gICAgQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3kuTWVzc2FnZSA9IE9iamVjdC5mcmVlemUoe1xuICAgICAgICBzZW5kQ2FuY2VsbGF0aW9uKGNvbm4sIGlkKSB7XG4gICAgICAgICAgICByZXR1cm4gY29ubi5zZW5kTm90aWZpY2F0aW9uKENhbmNlbE5vdGlmaWNhdGlvbi50eXBlLCB7IGlkIH0pO1xuICAgICAgICB9LFxuICAgICAgICBjbGVhbnVwKF8pIHsgfVxuICAgIH0pO1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICYmIElzLmZ1bmMoY2FuZGlkYXRlLnNlbmRDYW5jZWxsYXRpb24pICYmIElzLmZ1bmMoY2FuZGlkYXRlLmNsZWFudXApO1xuICAgIH1cbiAgICBDYW5jZWxsYXRpb25TZW5kZXJTdHJhdGVneS5pcyA9IGlzO1xufSkoQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3kgfHwgKGV4cG9ydHMuQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3kgPSBDYW5jZWxsYXRpb25TZW5kZXJTdHJhdGVneSA9IHt9KSk7XG52YXIgQ2FuY2VsbGF0aW9uU3RyYXRlZ3k7XG4oZnVuY3Rpb24gKENhbmNlbGxhdGlvblN0cmF0ZWd5KSB7XG4gICAgQ2FuY2VsbGF0aW9uU3RyYXRlZ3kuTWVzc2FnZSA9IE9iamVjdC5mcmVlemUoe1xuICAgICAgICByZWNlaXZlcjogQ2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneS5NZXNzYWdlLFxuICAgICAgICBzZW5kZXI6IENhbmNlbGxhdGlvblNlbmRlclN0cmF0ZWd5Lk1lc3NhZ2VcbiAgICB9KTtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiBDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5LmlzKGNhbmRpZGF0ZS5yZWNlaXZlcikgJiYgQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3kuaXMoY2FuZGlkYXRlLnNlbmRlcik7XG4gICAgfVxuICAgIENhbmNlbGxhdGlvblN0cmF0ZWd5LmlzID0gaXM7XG59KShDYW5jZWxsYXRpb25TdHJhdGVneSB8fCAoZXhwb3J0cy5DYW5jZWxsYXRpb25TdHJhdGVneSA9IENhbmNlbGxhdGlvblN0cmF0ZWd5ID0ge30pKTtcbnZhciBNZXNzYWdlU3RyYXRlZ3k7XG4oZnVuY3Rpb24gKE1lc3NhZ2VTdHJhdGVneSkge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICYmIElzLmZ1bmMoY2FuZGlkYXRlLmhhbmRsZU1lc3NhZ2UpO1xuICAgIH1cbiAgICBNZXNzYWdlU3RyYXRlZ3kuaXMgPSBpcztcbn0pKE1lc3NhZ2VTdHJhdGVneSB8fCAoZXhwb3J0cy5NZXNzYWdlU3RyYXRlZ3kgPSBNZXNzYWdlU3RyYXRlZ3kgPSB7fSkpO1xudmFyIENvbm5lY3Rpb25PcHRpb25zO1xuKGZ1bmN0aW9uIChDb25uZWN0aW9uT3B0aW9ucykge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICYmIChDYW5jZWxsYXRpb25TdHJhdGVneS5pcyhjYW5kaWRhdGUuY2FuY2VsbGF0aW9uU3RyYXRlZ3kpIHx8IENvbm5lY3Rpb25TdHJhdGVneS5pcyhjYW5kaWRhdGUuY29ubmVjdGlvblN0cmF0ZWd5KSB8fCBNZXNzYWdlU3RyYXRlZ3kuaXMoY2FuZGlkYXRlLm1lc3NhZ2VTdHJhdGVneSkpO1xuICAgIH1cbiAgICBDb25uZWN0aW9uT3B0aW9ucy5pcyA9IGlzO1xufSkoQ29ubmVjdGlvbk9wdGlvbnMgfHwgKGV4cG9ydHMuQ29ubmVjdGlvbk9wdGlvbnMgPSBDb25uZWN0aW9uT3B0aW9ucyA9IHt9KSk7XG52YXIgQ29ubmVjdGlvblN0YXRlO1xuKGZ1bmN0aW9uIChDb25uZWN0aW9uU3RhdGUpIHtcbiAgICBDb25uZWN0aW9uU3RhdGVbQ29ubmVjdGlvblN0YXRlW1wiTmV3XCJdID0gMV0gPSBcIk5ld1wiO1xuICAgIENvbm5lY3Rpb25TdGF0ZVtDb25uZWN0aW9uU3RhdGVbXCJMaXN0ZW5pbmdcIl0gPSAyXSA9IFwiTGlzdGVuaW5nXCI7XG4gICAgQ29ubmVjdGlvblN0YXRlW0Nvbm5lY3Rpb25TdGF0ZVtcIkNsb3NlZFwiXSA9IDNdID0gXCJDbG9zZWRcIjtcbiAgICBDb25uZWN0aW9uU3RhdGVbQ29ubmVjdGlvblN0YXRlW1wiRGlzcG9zZWRcIl0gPSA0XSA9IFwiRGlzcG9zZWRcIjtcbn0pKENvbm5lY3Rpb25TdGF0ZSB8fCAoQ29ubmVjdGlvblN0YXRlID0ge30pKTtcbmZ1bmN0aW9uIGNyZWF0ZU1lc3NhZ2VDb25uZWN0aW9uKG1lc3NhZ2VSZWFkZXIsIG1lc3NhZ2VXcml0ZXIsIF9sb2dnZXIsIG9wdGlvbnMpIHtcbiAgICBjb25zdCBsb2dnZXIgPSBfbG9nZ2VyICE9PSB1bmRlZmluZWQgPyBfbG9nZ2VyIDogZXhwb3J0cy5OdWxsTG9nZ2VyO1xuICAgIGxldCBzZXF1ZW5jZU51bWJlciA9IDA7XG4gICAgbGV0IG5vdGlmaWNhdGlvblNlcXVlbmNlTnVtYmVyID0gMDtcbiAgICBsZXQgdW5rbm93blJlc3BvbnNlU2VxdWVuY2VOdW1iZXIgPSAwO1xuICAgIGNvbnN0IHZlcnNpb24gPSAnMi4wJztcbiAgICBsZXQgc3RhclJlcXVlc3RIYW5kbGVyID0gdW5kZWZpbmVkO1xuICAgIGNvbnN0IHJlcXVlc3RIYW5kbGVycyA9IG5ldyBNYXAoKTtcbiAgICBsZXQgc3Rhck5vdGlmaWNhdGlvbkhhbmRsZXIgPSB1bmRlZmluZWQ7XG4gICAgY29uc3Qgbm90aWZpY2F0aW9uSGFuZGxlcnMgPSBuZXcgTWFwKCk7XG4gICAgY29uc3QgcHJvZ3Jlc3NIYW5kbGVycyA9IG5ldyBNYXAoKTtcbiAgICBsZXQgdGltZXI7XG4gICAgbGV0IG1lc3NhZ2VRdWV1ZSA9IG5ldyBsaW5rZWRNYXBfMS5MaW5rZWRNYXAoKTtcbiAgICBsZXQgcmVzcG9uc2VQcm9taXNlcyA9IG5ldyBNYXAoKTtcbiAgICBsZXQga25vd25DYW5jZWxlZFJlcXVlc3RzID0gbmV3IFNldCgpO1xuICAgIGxldCByZXF1ZXN0VG9rZW5zID0gbmV3IE1hcCgpO1xuICAgIGxldCB0cmFjZSA9IFRyYWNlLk9mZjtcbiAgICBsZXQgdHJhY2VGb3JtYXQgPSBUcmFjZUZvcm1hdC5UZXh0O1xuICAgIGxldCB0cmFjZXI7XG4gICAgbGV0IHN0YXRlID0gQ29ubmVjdGlvblN0YXRlLk5ldztcbiAgICBjb25zdCBlcnJvckVtaXR0ZXIgPSBuZXcgZXZlbnRzXzEuRW1pdHRlcigpO1xuICAgIGNvbnN0IGNsb3NlRW1pdHRlciA9IG5ldyBldmVudHNfMS5FbWl0dGVyKCk7XG4gICAgY29uc3QgdW5oYW5kbGVkTm90aWZpY2F0aW9uRW1pdHRlciA9IG5ldyBldmVudHNfMS5FbWl0dGVyKCk7XG4gICAgY29uc3QgdW5oYW5kbGVkUHJvZ3Jlc3NFbWl0dGVyID0gbmV3IGV2ZW50c18xLkVtaXR0ZXIoKTtcbiAgICBjb25zdCBkaXNwb3NlRW1pdHRlciA9IG5ldyBldmVudHNfMS5FbWl0dGVyKCk7XG4gICAgY29uc3QgY2FuY2VsbGF0aW9uU3RyYXRlZ3kgPSAob3B0aW9ucyAmJiBvcHRpb25zLmNhbmNlbGxhdGlvblN0cmF0ZWd5KSA/IG9wdGlvbnMuY2FuY2VsbGF0aW9uU3RyYXRlZ3kgOiBDYW5jZWxsYXRpb25TdHJhdGVneS5NZXNzYWdlO1xuICAgIGZ1bmN0aW9uIGNyZWF0ZVJlcXVlc3RRdWV1ZUtleShpZCkge1xuICAgICAgICBpZiAoaWQgPT09IG51bGwpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgQ2FuJ3Qgc2VuZCByZXF1ZXN0cyB3aXRoIGlkIG51bGwgc2luY2UgdGhlIHJlc3BvbnNlIGNhbid0IGJlIGNvcnJlbGF0ZWQuYCk7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuICdyZXEtJyArIGlkLnRvU3RyaW5nKCk7XG4gICAgfVxuICAgIGZ1bmN0aW9uIGNyZWF0ZVJlc3BvbnNlUXVldWVLZXkoaWQpIHtcbiAgICAgICAgaWYgKGlkID09PSBudWxsKSB7XG4gICAgICAgICAgICByZXR1cm4gJ3Jlcy11bmtub3duLScgKyAoKyt1bmtub3duUmVzcG9uc2VTZXF1ZW5jZU51bWJlcikudG9TdHJpbmcoKTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHJldHVybiAncmVzLScgKyBpZC50b1N0cmluZygpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIGNyZWF0ZU5vdGlmaWNhdGlvblF1ZXVlS2V5KCkge1xuICAgICAgICByZXR1cm4gJ25vdC0nICsgKCsrbm90aWZpY2F0aW9uU2VxdWVuY2VOdW1iZXIpLnRvU3RyaW5nKCk7XG4gICAgfVxuICAgIGZ1bmN0aW9uIGFkZE1lc3NhZ2VUb1F1ZXVlKHF1ZXVlLCBtZXNzYWdlKSB7XG4gICAgICAgIGlmIChtZXNzYWdlc18xLk1lc3NhZ2UuaXNSZXF1ZXN0KG1lc3NhZ2UpKSB7XG4gICAgICAgICAgICBxdWV1ZS5zZXQoY3JlYXRlUmVxdWVzdFF1ZXVlS2V5KG1lc3NhZ2UuaWQpLCBtZXNzYWdlKTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIGlmIChtZXNzYWdlc18xLk1lc3NhZ2UuaXNSZXNwb25zZShtZXNzYWdlKSkge1xuICAgICAgICAgICAgcXVldWUuc2V0KGNyZWF0ZVJlc3BvbnNlUXVldWVLZXkobWVzc2FnZS5pZCksIG1lc3NhZ2UpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgcXVldWUuc2V0KGNyZWF0ZU5vdGlmaWNhdGlvblF1ZXVlS2V5KCksIG1lc3NhZ2UpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIGNhbmNlbFVuZGlzcGF0Y2hlZChfbWVzc2FnZSkge1xuICAgICAgICByZXR1cm4gdW5kZWZpbmVkO1xuICAgIH1cbiAgICBmdW5jdGlvbiBpc0xpc3RlbmluZygpIHtcbiAgICAgICAgcmV0dXJuIHN0YXRlID09PSBDb25uZWN0aW9uU3RhdGUuTGlzdGVuaW5nO1xuICAgIH1cbiAgICBmdW5jdGlvbiBpc0Nsb3NlZCgpIHtcbiAgICAgICAgcmV0dXJuIHN0YXRlID09PSBDb25uZWN0aW9uU3RhdGUuQ2xvc2VkO1xuICAgIH1cbiAgICBmdW5jdGlvbiBpc0Rpc3Bvc2VkKCkge1xuICAgICAgICByZXR1cm4gc3RhdGUgPT09IENvbm5lY3Rpb25TdGF0ZS5EaXNwb3NlZDtcbiAgICB9XG4gICAgZnVuY3Rpb24gY2xvc2VIYW5kbGVyKCkge1xuICAgICAgICBpZiAoc3RhdGUgPT09IENvbm5lY3Rpb25TdGF0ZS5OZXcgfHwgc3RhdGUgPT09IENvbm5lY3Rpb25TdGF0ZS5MaXN0ZW5pbmcpIHtcbiAgICAgICAgICAgIHN0YXRlID0gQ29ubmVjdGlvblN0YXRlLkNsb3NlZDtcbiAgICAgICAgICAgIGNsb3NlRW1pdHRlci5maXJlKHVuZGVmaW5lZCk7XG4gICAgICAgIH1cbiAgICAgICAgLy8gSWYgdGhlIGNvbm5lY3Rpb24gaXMgZGlzcG9zZWQgZG9uJ3Qgc2VudCBjbG9zZSBldmVudHMuXG4gICAgfVxuICAgIGZ1bmN0aW9uIHJlYWRFcnJvckhhbmRsZXIoZXJyb3IpIHtcbiAgICAgICAgZXJyb3JFbWl0dGVyLmZpcmUoW2Vycm9yLCB1bmRlZmluZWQsIHVuZGVmaW5lZF0pO1xuICAgIH1cbiAgICBmdW5jdGlvbiB3cml0ZUVycm9ySGFuZGxlcihkYXRhKSB7XG4gICAgICAgIGVycm9yRW1pdHRlci5maXJlKGRhdGEpO1xuICAgIH1cbiAgICBtZXNzYWdlUmVhZGVyLm9uQ2xvc2UoY2xvc2VIYW5kbGVyKTtcbiAgICBtZXNzYWdlUmVhZGVyLm9uRXJyb3IocmVhZEVycm9ySGFuZGxlcik7XG4gICAgbWVzc2FnZVdyaXRlci5vbkNsb3NlKGNsb3NlSGFuZGxlcik7XG4gICAgbWVzc2FnZVdyaXRlci5vbkVycm9yKHdyaXRlRXJyb3JIYW5kbGVyKTtcbiAgICBmdW5jdGlvbiB0cmlnZ2VyTWVzc2FnZVF1ZXVlKCkge1xuICAgICAgICBpZiAodGltZXIgfHwgbWVzc2FnZVF1ZXVlLnNpemUgPT09IDApIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICB0aW1lciA9ICgwLCByYWxfMS5kZWZhdWx0KSgpLnRpbWVyLnNldEltbWVkaWF0ZSgoKSA9PiB7XG4gICAgICAgICAgICB0aW1lciA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIHByb2Nlc3NNZXNzYWdlUXVldWUoKTtcbiAgICAgICAgfSk7XG4gICAgfVxuICAgIGZ1bmN0aW9uIGhhbmRsZU1lc3NhZ2UobWVzc2FnZSkge1xuICAgICAgICBpZiAobWVzc2FnZXNfMS5NZXNzYWdlLmlzUmVxdWVzdChtZXNzYWdlKSkge1xuICAgICAgICAgICAgaGFuZGxlUmVxdWVzdChtZXNzYWdlKTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIGlmIChtZXNzYWdlc18xLk1lc3NhZ2UuaXNOb3RpZmljYXRpb24obWVzc2FnZSkpIHtcbiAgICAgICAgICAgIGhhbmRsZU5vdGlmaWNhdGlvbihtZXNzYWdlKTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIGlmIChtZXNzYWdlc18xLk1lc3NhZ2UuaXNSZXNwb25zZShtZXNzYWdlKSkge1xuICAgICAgICAgICAgaGFuZGxlUmVzcG9uc2UobWVzc2FnZSk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBoYW5kbGVJbnZhbGlkTWVzc2FnZShtZXNzYWdlKTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBmdW5jdGlvbiBwcm9jZXNzTWVzc2FnZVF1ZXVlKCkge1xuICAgICAgICBpZiAobWVzc2FnZVF1ZXVlLnNpemUgPT09IDApIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBjb25zdCBtZXNzYWdlID0gbWVzc2FnZVF1ZXVlLnNoaWZ0KCk7XG4gICAgICAgIHRyeSB7XG4gICAgICAgICAgICBjb25zdCBtZXNzYWdlU3RyYXRlZ3kgPSBvcHRpb25zPy5tZXNzYWdlU3RyYXRlZ3k7XG4gICAgICAgICAgICBpZiAoTWVzc2FnZVN0cmF0ZWd5LmlzKG1lc3NhZ2VTdHJhdGVneSkpIHtcbiAgICAgICAgICAgICAgICBtZXNzYWdlU3RyYXRlZ3kuaGFuZGxlTWVzc2FnZShtZXNzYWdlLCBoYW5kbGVNZXNzYWdlKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIGhhbmRsZU1lc3NhZ2UobWVzc2FnZSk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgZmluYWxseSB7XG4gICAgICAgICAgICB0cmlnZ2VyTWVzc2FnZVF1ZXVlKCk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgY29uc3QgY2FsbGJhY2sgPSAobWVzc2FnZSkgPT4ge1xuICAgICAgICB0cnkge1xuICAgICAgICAgICAgLy8gV2UgaGF2ZSByZWNlaXZlZCBhIGNhbmNlbGxhdGlvbiBtZXNzYWdlLiBDaGVjayBpZiB0aGUgbWVzc2FnZSBpcyBzdGlsbCBpbiB0aGUgcXVldWVcbiAgICAgICAgICAgIC8vIGFuZCBjYW5jZWwgaXQgaWYgYWxsb3dlZCB0byBkbyBzby5cbiAgICAgICAgICAgIGlmIChtZXNzYWdlc18xLk1lc3NhZ2UuaXNOb3RpZmljYXRpb24obWVzc2FnZSkgJiYgbWVzc2FnZS5tZXRob2QgPT09IENhbmNlbE5vdGlmaWNhdGlvbi50eXBlLm1ldGhvZCkge1xuICAgICAgICAgICAgICAgIGNvbnN0IGNhbmNlbElkID0gbWVzc2FnZS5wYXJhbXMuaWQ7XG4gICAgICAgICAgICAgICAgY29uc3Qga2V5ID0gY3JlYXRlUmVxdWVzdFF1ZXVlS2V5KGNhbmNlbElkKTtcbiAgICAgICAgICAgICAgICBjb25zdCB0b0NhbmNlbCA9IG1lc3NhZ2VRdWV1ZS5nZXQoa2V5KTtcbiAgICAgICAgICAgICAgICBpZiAobWVzc2FnZXNfMS5NZXNzYWdlLmlzUmVxdWVzdCh0b0NhbmNlbCkpIHtcbiAgICAgICAgICAgICAgICAgICAgY29uc3Qgc3RyYXRlZ3kgPSBvcHRpb25zPy5jb25uZWN0aW9uU3RyYXRlZ3k7XG4gICAgICAgICAgICAgICAgICAgIGNvbnN0IHJlc3BvbnNlID0gKHN0cmF0ZWd5ICYmIHN0cmF0ZWd5LmNhbmNlbFVuZGlzcGF0Y2hlZCkgPyBzdHJhdGVneS5jYW5jZWxVbmRpc3BhdGNoZWQodG9DYW5jZWwsIGNhbmNlbFVuZGlzcGF0Y2hlZCkgOiBjYW5jZWxVbmRpc3BhdGNoZWQodG9DYW5jZWwpO1xuICAgICAgICAgICAgICAgICAgICBpZiAocmVzcG9uc2UgJiYgKHJlc3BvbnNlLmVycm9yICE9PSB1bmRlZmluZWQgfHwgcmVzcG9uc2UucmVzdWx0ICE9PSB1bmRlZmluZWQpKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBtZXNzYWdlUXVldWUuZGVsZXRlKGtleSk7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXF1ZXN0VG9rZW5zLmRlbGV0ZShjYW5jZWxJZCk7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXNwb25zZS5pZCA9IHRvQ2FuY2VsLmlkO1xuICAgICAgICAgICAgICAgICAgICAgICAgdHJhY2VTZW5kaW5nUmVzcG9uc2UocmVzcG9uc2UsIG1lc3NhZ2UubWV0aG9kLCBEYXRlLm5vdygpKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIG1lc3NhZ2VXcml0ZXIud3JpdGUocmVzcG9uc2UpLmNhdGNoKCgpID0+IGxvZ2dlci5lcnJvcihgU2VuZGluZyByZXNwb25zZSBmb3IgY2FuY2VsZWQgbWVzc2FnZSBmYWlsZWQuYCkpO1xuICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGNvbnN0IGNhbmNlbGxhdGlvblRva2VuID0gcmVxdWVzdFRva2Vucy5nZXQoY2FuY2VsSWQpO1xuICAgICAgICAgICAgICAgIC8vIFRoZSByZXF1ZXN0IGlzIGFscmVhZHkgcnVubmluZy4gQ2FuY2VsIHRoZSB0b2tlblxuICAgICAgICAgICAgICAgIGlmIChjYW5jZWxsYXRpb25Ub2tlbiAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICAgICAgICAgIGNhbmNlbGxhdGlvblRva2VuLmNhbmNlbCgpO1xuICAgICAgICAgICAgICAgICAgICB0cmFjZVJlY2VpdmVkTm90aWZpY2F0aW9uKG1lc3NhZ2UpO1xuICAgICAgICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAvLyBSZW1lbWJlciB0aGUgY2FuY2VsIGJ1dCBzdGlsbCBxdWV1ZSB0aGUgbWVzc2FnZSB0b1xuICAgICAgICAgICAgICAgICAgICAvLyBjbGVhbiB1cCBzdGF0ZSBpbiBwcm9jZXNzIG1lc3NhZ2UuXG4gICAgICAgICAgICAgICAgICAgIGtub3duQ2FuY2VsZWRSZXF1ZXN0cy5hZGQoY2FuY2VsSWQpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGFkZE1lc3NhZ2VUb1F1ZXVlKG1lc3NhZ2VRdWV1ZSwgbWVzc2FnZSk7XG4gICAgICAgIH1cbiAgICAgICAgZmluYWxseSB7XG4gICAgICAgICAgICB0cmlnZ2VyTWVzc2FnZVF1ZXVlKCk7XG4gICAgICAgIH1cbiAgICB9O1xuICAgIGZ1bmN0aW9uIGhhbmRsZVJlcXVlc3QocmVxdWVzdE1lc3NhZ2UpIHtcbiAgICAgICAgaWYgKGlzRGlzcG9zZWQoKSkge1xuICAgICAgICAgICAgLy8gd2UgcmV0dXJuIGhlcmUgc2lsZW50bHkgc2luY2Ugd2UgZmlyZWQgYW4gZXZlbnQgd2hlbiB0aGVcbiAgICAgICAgICAgIC8vIGNvbm5lY3Rpb24gZ290IGRpc3Bvc2VkLlxuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGZ1bmN0aW9uIHJlcGx5KHJlc3VsdE9yRXJyb3IsIG1ldGhvZCwgc3RhcnRUaW1lKSB7XG4gICAgICAgICAgICBjb25zdCBtZXNzYWdlID0ge1xuICAgICAgICAgICAgICAgIGpzb25ycGM6IHZlcnNpb24sXG4gICAgICAgICAgICAgICAgaWQ6IHJlcXVlc3RNZXNzYWdlLmlkXG4gICAgICAgICAgICB9O1xuICAgICAgICAgICAgaWYgKHJlc3VsdE9yRXJyb3IgaW5zdGFuY2VvZiBtZXNzYWdlc18xLlJlc3BvbnNlRXJyb3IpIHtcbiAgICAgICAgICAgICAgICBtZXNzYWdlLmVycm9yID0gcmVzdWx0T3JFcnJvci50b0pzb24oKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIG1lc3NhZ2UucmVzdWx0ID0gcmVzdWx0T3JFcnJvciA9PT0gdW5kZWZpbmVkID8gbnVsbCA6IHJlc3VsdE9yRXJyb3I7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICB0cmFjZVNlbmRpbmdSZXNwb25zZShtZXNzYWdlLCBtZXRob2QsIHN0YXJ0VGltZSk7XG4gICAgICAgICAgICBtZXNzYWdlV3JpdGVyLndyaXRlKG1lc3NhZ2UpLmNhdGNoKCgpID0+IGxvZ2dlci5lcnJvcihgU2VuZGluZyByZXNwb25zZSBmYWlsZWQuYCkpO1xuICAgICAgICB9XG4gICAgICAgIGZ1bmN0aW9uIHJlcGx5RXJyb3IoZXJyb3IsIG1ldGhvZCwgc3RhcnRUaW1lKSB7XG4gICAgICAgICAgICBjb25zdCBtZXNzYWdlID0ge1xuICAgICAgICAgICAgICAgIGpzb25ycGM6IHZlcnNpb24sXG4gICAgICAgICAgICAgICAgaWQ6IHJlcXVlc3RNZXNzYWdlLmlkLFxuICAgICAgICAgICAgICAgIGVycm9yOiBlcnJvci50b0pzb24oKVxuICAgICAgICAgICAgfTtcbiAgICAgICAgICAgIHRyYWNlU2VuZGluZ1Jlc3BvbnNlKG1lc3NhZ2UsIG1ldGhvZCwgc3RhcnRUaW1lKTtcbiAgICAgICAgICAgIG1lc3NhZ2VXcml0ZXIud3JpdGUobWVzc2FnZSkuY2F0Y2goKCkgPT4gbG9nZ2VyLmVycm9yKGBTZW5kaW5nIHJlc3BvbnNlIGZhaWxlZC5gKSk7XG4gICAgICAgIH1cbiAgICAgICAgZnVuY3Rpb24gcmVwbHlTdWNjZXNzKHJlc3VsdCwgbWV0aG9kLCBzdGFydFRpbWUpIHtcbiAgICAgICAgICAgIC8vIFRoZSBKU09OIFJQQyBkZWZpbmVzIHRoYXQgYSByZXNwb25zZSBtdXN0IGVpdGhlciBoYXZlIGEgcmVzdWx0IG9yIGFuIGVycm9yXG4gICAgICAgICAgICAvLyBTbyB3ZSBjYW4ndCB0cmVhdCB1bmRlZmluZWQgYXMgYSB2YWxpZCByZXNwb25zZSByZXN1bHQuXG4gICAgICAgICAgICBpZiAocmVzdWx0ID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICByZXN1bHQgPSBudWxsO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgY29uc3QgbWVzc2FnZSA9IHtcbiAgICAgICAgICAgICAgICBqc29ucnBjOiB2ZXJzaW9uLFxuICAgICAgICAgICAgICAgIGlkOiByZXF1ZXN0TWVzc2FnZS5pZCxcbiAgICAgICAgICAgICAgICByZXN1bHQ6IHJlc3VsdFxuICAgICAgICAgICAgfTtcbiAgICAgICAgICAgIHRyYWNlU2VuZGluZ1Jlc3BvbnNlKG1lc3NhZ2UsIG1ldGhvZCwgc3RhcnRUaW1lKTtcbiAgICAgICAgICAgIG1lc3NhZ2VXcml0ZXIud3JpdGUobWVzc2FnZSkuY2F0Y2goKCkgPT4gbG9nZ2VyLmVycm9yKGBTZW5kaW5nIHJlc3BvbnNlIGZhaWxlZC5gKSk7XG4gICAgICAgIH1cbiAgICAgICAgdHJhY2VSZWNlaXZlZFJlcXVlc3QocmVxdWVzdE1lc3NhZ2UpO1xuICAgICAgICBjb25zdCBlbGVtZW50ID0gcmVxdWVzdEhhbmRsZXJzLmdldChyZXF1ZXN0TWVzc2FnZS5tZXRob2QpO1xuICAgICAgICBsZXQgdHlwZTtcbiAgICAgICAgbGV0IHJlcXVlc3RIYW5kbGVyO1xuICAgICAgICBpZiAoZWxlbWVudCkge1xuICAgICAgICAgICAgdHlwZSA9IGVsZW1lbnQudHlwZTtcbiAgICAgICAgICAgIHJlcXVlc3RIYW5kbGVyID0gZWxlbWVudC5oYW5kbGVyO1xuICAgICAgICB9XG4gICAgICAgIGNvbnN0IHN0YXJ0VGltZSA9IERhdGUubm93KCk7XG4gICAgICAgIGlmIChyZXF1ZXN0SGFuZGxlciB8fCBzdGFyUmVxdWVzdEhhbmRsZXIpIHtcbiAgICAgICAgICAgIGNvbnN0IHRva2VuS2V5ID0gcmVxdWVzdE1lc3NhZ2UuaWQgPz8gU3RyaW5nKERhdGUubm93KCkpOyAvL1xuICAgICAgICAgICAgY29uc3QgY2FuY2VsbGF0aW9uU291cmNlID0gSWRDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5LmlzKGNhbmNlbGxhdGlvblN0cmF0ZWd5LnJlY2VpdmVyKVxuICAgICAgICAgICAgICAgID8gY2FuY2VsbGF0aW9uU3RyYXRlZ3kucmVjZWl2ZXIuY3JlYXRlQ2FuY2VsbGF0aW9uVG9rZW5Tb3VyY2UodG9rZW5LZXkpXG4gICAgICAgICAgICAgICAgOiBjYW5jZWxsYXRpb25TdHJhdGVneS5yZWNlaXZlci5jcmVhdGVDYW5jZWxsYXRpb25Ub2tlblNvdXJjZShyZXF1ZXN0TWVzc2FnZSk7XG4gICAgICAgICAgICBpZiAocmVxdWVzdE1lc3NhZ2UuaWQgIT09IG51bGwgJiYga25vd25DYW5jZWxlZFJlcXVlc3RzLmhhcyhyZXF1ZXN0TWVzc2FnZS5pZCkpIHtcbiAgICAgICAgICAgICAgICBjYW5jZWxsYXRpb25Tb3VyY2UuY2FuY2VsKCk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAocmVxdWVzdE1lc3NhZ2UuaWQgIT09IG51bGwpIHtcbiAgICAgICAgICAgICAgICByZXF1ZXN0VG9rZW5zLnNldCh0b2tlbktleSwgY2FuY2VsbGF0aW9uU291cmNlKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHRyeSB7XG4gICAgICAgICAgICAgICAgbGV0IGhhbmRsZXJSZXN1bHQ7XG4gICAgICAgICAgICAgICAgaWYgKHJlcXVlc3RIYW5kbGVyKSB7XG4gICAgICAgICAgICAgICAgICAgIGlmIChyZXF1ZXN0TWVzc2FnZS5wYXJhbXMgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKHR5cGUgIT09IHVuZGVmaW5lZCAmJiB0eXBlLm51bWJlck9mUGFyYW1zICE9PSAwKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgcmVwbHlFcnJvcihuZXcgbWVzc2FnZXNfMS5SZXNwb25zZUVycm9yKG1lc3NhZ2VzXzEuRXJyb3JDb2Rlcy5JbnZhbGlkUGFyYW1zLCBgUmVxdWVzdCAke3JlcXVlc3RNZXNzYWdlLm1ldGhvZH0gZGVmaW5lcyAke3R5cGUubnVtYmVyT2ZQYXJhbXN9IHBhcmFtcyBidXQgcmVjZWl2ZWQgbm9uZS5gKSwgcmVxdWVzdE1lc3NhZ2UubWV0aG9kLCBzdGFydFRpbWUpO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIGhhbmRsZXJSZXN1bHQgPSByZXF1ZXN0SGFuZGxlcihjYW5jZWxsYXRpb25Tb3VyY2UudG9rZW4pO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2UgaWYgKEFycmF5LmlzQXJyYXkocmVxdWVzdE1lc3NhZ2UucGFyYW1zKSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKHR5cGUgIT09IHVuZGVmaW5lZCAmJiB0eXBlLnBhcmFtZXRlclN0cnVjdHVyZXMgPT09IG1lc3NhZ2VzXzEuUGFyYW1ldGVyU3RydWN0dXJlcy5ieU5hbWUpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICByZXBseUVycm9yKG5ldyBtZXNzYWdlc18xLlJlc3BvbnNlRXJyb3IobWVzc2FnZXNfMS5FcnJvckNvZGVzLkludmFsaWRQYXJhbXMsIGBSZXF1ZXN0ICR7cmVxdWVzdE1lc3NhZ2UubWV0aG9kfSBkZWZpbmVzIHBhcmFtZXRlcnMgYnkgbmFtZSBidXQgcmVjZWl2ZWQgcGFyYW1ldGVycyBieSBwb3NpdGlvbmApLCByZXF1ZXN0TWVzc2FnZS5tZXRob2QsIHN0YXJ0VGltZSk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgaGFuZGxlclJlc3VsdCA9IHJlcXVlc3RIYW5kbGVyKC4uLnJlcXVlc3RNZXNzYWdlLnBhcmFtcywgY2FuY2VsbGF0aW9uU291cmNlLnRva2VuKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICh0eXBlICE9PSB1bmRlZmluZWQgJiYgdHlwZS5wYXJhbWV0ZXJTdHJ1Y3R1cmVzID09PSBtZXNzYWdlc18xLlBhcmFtZXRlclN0cnVjdHVyZXMuYnlQb3NpdGlvbikge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJlcGx5RXJyb3IobmV3IG1lc3NhZ2VzXzEuUmVzcG9uc2VFcnJvcihtZXNzYWdlc18xLkVycm9yQ29kZXMuSW52YWxpZFBhcmFtcywgYFJlcXVlc3QgJHtyZXF1ZXN0TWVzc2FnZS5tZXRob2R9IGRlZmluZXMgcGFyYW1ldGVycyBieSBwb3NpdGlvbiBidXQgcmVjZWl2ZWQgcGFyYW1ldGVycyBieSBuYW1lYCksIHJlcXVlc3RNZXNzYWdlLm1ldGhvZCwgc3RhcnRUaW1lKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBoYW5kbGVyUmVzdWx0ID0gcmVxdWVzdEhhbmRsZXIocmVxdWVzdE1lc3NhZ2UucGFyYW1zLCBjYW5jZWxsYXRpb25Tb3VyY2UudG9rZW4pO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2UgaWYgKHN0YXJSZXF1ZXN0SGFuZGxlcikge1xuICAgICAgICAgICAgICAgICAgICBoYW5kbGVyUmVzdWx0ID0gc3RhclJlcXVlc3RIYW5kbGVyKHJlcXVlc3RNZXNzYWdlLm1ldGhvZCwgcmVxdWVzdE1lc3NhZ2UucGFyYW1zLCBjYW5jZWxsYXRpb25Tb3VyY2UudG9rZW4pO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBjb25zdCBwcm9taXNlID0gaGFuZGxlclJlc3VsdDtcbiAgICAgICAgICAgICAgICBpZiAoIWhhbmRsZXJSZXN1bHQpIHtcbiAgICAgICAgICAgICAgICAgICAgcmVxdWVzdFRva2Vucy5kZWxldGUodG9rZW5LZXkpO1xuICAgICAgICAgICAgICAgICAgICByZXBseVN1Y2Nlc3MoaGFuZGxlclJlc3VsdCwgcmVxdWVzdE1lc3NhZ2UubWV0aG9kLCBzdGFydFRpbWUpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIGlmIChwcm9taXNlLnRoZW4pIHtcbiAgICAgICAgICAgICAgICAgICAgcHJvbWlzZS50aGVuKChyZXN1bHRPckVycm9yKSA9PiB7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXF1ZXN0VG9rZW5zLmRlbGV0ZSh0b2tlbktleSk7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXBseShyZXN1bHRPckVycm9yLCByZXF1ZXN0TWVzc2FnZS5tZXRob2QsIHN0YXJ0VGltZSk7XG4gICAgICAgICAgICAgICAgICAgIH0sIGVycm9yID0+IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJlcXVlc3RUb2tlbnMuZGVsZXRlKHRva2VuS2V5KTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmIChlcnJvciBpbnN0YW5jZW9mIG1lc3NhZ2VzXzEuUmVzcG9uc2VFcnJvcikge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJlcGx5RXJyb3IoZXJyb3IsIHJlcXVlc3RNZXNzYWdlLm1ldGhvZCwgc3RhcnRUaW1lKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIGVsc2UgaWYgKGVycm9yICYmIElzLnN0cmluZyhlcnJvci5tZXNzYWdlKSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJlcGx5RXJyb3IobmV3IG1lc3NhZ2VzXzEuUmVzcG9uc2VFcnJvcihtZXNzYWdlc18xLkVycm9yQ29kZXMuSW50ZXJuYWxFcnJvciwgYFJlcXVlc3QgJHtyZXF1ZXN0TWVzc2FnZS5tZXRob2R9IGZhaWxlZCB3aXRoIG1lc3NhZ2U6ICR7ZXJyb3IubWVzc2FnZX1gKSwgcmVxdWVzdE1lc3NhZ2UubWV0aG9kLCBzdGFydFRpbWUpO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgcmVwbHlFcnJvcihuZXcgbWVzc2FnZXNfMS5SZXNwb25zZUVycm9yKG1lc3NhZ2VzXzEuRXJyb3JDb2Rlcy5JbnRlcm5hbEVycm9yLCBgUmVxdWVzdCAke3JlcXVlc3RNZXNzYWdlLm1ldGhvZH0gZmFpbGVkIHVuZXhwZWN0ZWRseSB3aXRob3V0IHByb3ZpZGluZyBhbnkgZGV0YWlscy5gKSwgcmVxdWVzdE1lc3NhZ2UubWV0aG9kLCBzdGFydFRpbWUpO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB9KTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIHJlcXVlc3RUb2tlbnMuZGVsZXRlKHRva2VuS2V5KTtcbiAgICAgICAgICAgICAgICAgICAgcmVwbHkoaGFuZGxlclJlc3VsdCwgcmVxdWVzdE1lc3NhZ2UubWV0aG9kLCBzdGFydFRpbWUpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNhdGNoIChlcnJvcikge1xuICAgICAgICAgICAgICAgIHJlcXVlc3RUb2tlbnMuZGVsZXRlKHRva2VuS2V5KTtcbiAgICAgICAgICAgICAgICBpZiAoZXJyb3IgaW5zdGFuY2VvZiBtZXNzYWdlc18xLlJlc3BvbnNlRXJyb3IpIHtcbiAgICAgICAgICAgICAgICAgICAgcmVwbHkoZXJyb3IsIHJlcXVlc3RNZXNzYWdlLm1ldGhvZCwgc3RhcnRUaW1lKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSBpZiAoZXJyb3IgJiYgSXMuc3RyaW5nKGVycm9yLm1lc3NhZ2UpKSB7XG4gICAgICAgICAgICAgICAgICAgIHJlcGx5RXJyb3IobmV3IG1lc3NhZ2VzXzEuUmVzcG9uc2VFcnJvcihtZXNzYWdlc18xLkVycm9yQ29kZXMuSW50ZXJuYWxFcnJvciwgYFJlcXVlc3QgJHtyZXF1ZXN0TWVzc2FnZS5tZXRob2R9IGZhaWxlZCB3aXRoIG1lc3NhZ2U6ICR7ZXJyb3IubWVzc2FnZX1gKSwgcmVxdWVzdE1lc3NhZ2UubWV0aG9kLCBzdGFydFRpbWUpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgcmVwbHlFcnJvcihuZXcgbWVzc2FnZXNfMS5SZXNwb25zZUVycm9yKG1lc3NhZ2VzXzEuRXJyb3JDb2Rlcy5JbnRlcm5hbEVycm9yLCBgUmVxdWVzdCAke3JlcXVlc3RNZXNzYWdlLm1ldGhvZH0gZmFpbGVkIHVuZXhwZWN0ZWRseSB3aXRob3V0IHByb3ZpZGluZyBhbnkgZGV0YWlscy5gKSwgcmVxdWVzdE1lc3NhZ2UubWV0aG9kLCBzdGFydFRpbWUpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHJlcGx5RXJyb3IobmV3IG1lc3NhZ2VzXzEuUmVzcG9uc2VFcnJvcihtZXNzYWdlc18xLkVycm9yQ29kZXMuTWV0aG9kTm90Rm91bmQsIGBVbmhhbmRsZWQgbWV0aG9kICR7cmVxdWVzdE1lc3NhZ2UubWV0aG9kfWApLCByZXF1ZXN0TWVzc2FnZS5tZXRob2QsIHN0YXJ0VGltZSk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgZnVuY3Rpb24gaGFuZGxlUmVzcG9uc2UocmVzcG9uc2VNZXNzYWdlKSB7XG4gICAgICAgIGlmIChpc0Rpc3Bvc2VkKCkpIHtcbiAgICAgICAgICAgIC8vIFNlZSBoYW5kbGUgcmVxdWVzdC5cbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBpZiAocmVzcG9uc2VNZXNzYWdlLmlkID09PSBudWxsKSB7XG4gICAgICAgICAgICBpZiAocmVzcG9uc2VNZXNzYWdlLmVycm9yKSB7XG4gICAgICAgICAgICAgICAgbG9nZ2VyLmVycm9yKGBSZWNlaXZlZCByZXNwb25zZSBtZXNzYWdlIHdpdGhvdXQgaWQ6IEVycm9yIGlzOiBcXG4ke0pTT04uc3RyaW5naWZ5KHJlc3BvbnNlTWVzc2FnZS5lcnJvciwgdW5kZWZpbmVkLCA0KX1gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIGxvZ2dlci5lcnJvcihgUmVjZWl2ZWQgcmVzcG9uc2UgbWVzc2FnZSB3aXRob3V0IGlkLiBObyBmdXJ0aGVyIGVycm9yIGluZm9ybWF0aW9uIHByb3ZpZGVkLmApO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgY29uc3Qga2V5ID0gcmVzcG9uc2VNZXNzYWdlLmlkO1xuICAgICAgICAgICAgY29uc3QgcmVzcG9uc2VQcm9taXNlID0gcmVzcG9uc2VQcm9taXNlcy5nZXQoa2V5KTtcbiAgICAgICAgICAgIHRyYWNlUmVjZWl2ZWRSZXNwb25zZShyZXNwb25zZU1lc3NhZ2UsIHJlc3BvbnNlUHJvbWlzZSk7XG4gICAgICAgICAgICBpZiAocmVzcG9uc2VQcm9taXNlICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICByZXNwb25zZVByb21pc2VzLmRlbGV0ZShrZXkpO1xuICAgICAgICAgICAgICAgIHRyeSB7XG4gICAgICAgICAgICAgICAgICAgIGlmIChyZXNwb25zZU1lc3NhZ2UuZXJyb3IpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGNvbnN0IGVycm9yID0gcmVzcG9uc2VNZXNzYWdlLmVycm9yO1xuICAgICAgICAgICAgICAgICAgICAgICAgcmVzcG9uc2VQcm9taXNlLnJlamVjdChuZXcgbWVzc2FnZXNfMS5SZXNwb25zZUVycm9yKGVycm9yLmNvZGUsIGVycm9yLm1lc3NhZ2UsIGVycm9yLmRhdGEpKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBlbHNlIGlmIChyZXNwb25zZU1lc3NhZ2UucmVzdWx0ICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJlc3BvbnNlUHJvbWlzZS5yZXNvbHZlKHJlc3BvbnNlTWVzc2FnZS5yZXN1bHQpO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdTaG91bGQgbmV2ZXIgaGFwcGVuLicpO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGNhdGNoIChlcnJvcikge1xuICAgICAgICAgICAgICAgICAgICBpZiAoZXJyb3IubWVzc2FnZSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgbG9nZ2VyLmVycm9yKGBSZXNwb25zZSBoYW5kbGVyICcke3Jlc3BvbnNlUHJvbWlzZS5tZXRob2R9JyBmYWlsZWQgd2l0aCBtZXNzYWdlOiAke2Vycm9yLm1lc3NhZ2V9YCk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBsb2dnZXIuZXJyb3IoYFJlc3BvbnNlIGhhbmRsZXIgJyR7cmVzcG9uc2VQcm9taXNlLm1ldGhvZH0nIGZhaWxlZCB1bmV4cGVjdGVkbHkuYCk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICB9XG4gICAgZnVuY3Rpb24gaGFuZGxlTm90aWZpY2F0aW9uKG1lc3NhZ2UpIHtcbiAgICAgICAgaWYgKGlzRGlzcG9zZWQoKSkge1xuICAgICAgICAgICAgLy8gU2VlIGhhbmRsZSByZXF1ZXN0LlxuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGxldCB0eXBlID0gdW5kZWZpbmVkO1xuICAgICAgICBsZXQgbm90aWZpY2F0aW9uSGFuZGxlcjtcbiAgICAgICAgaWYgKG1lc3NhZ2UubWV0aG9kID09PSBDYW5jZWxOb3RpZmljYXRpb24udHlwZS5tZXRob2QpIHtcbiAgICAgICAgICAgIGNvbnN0IGNhbmNlbElkID0gbWVzc2FnZS5wYXJhbXMuaWQ7XG4gICAgICAgICAgICBrbm93bkNhbmNlbGVkUmVxdWVzdHMuZGVsZXRlKGNhbmNlbElkKTtcbiAgICAgICAgICAgIHRyYWNlUmVjZWl2ZWROb3RpZmljYXRpb24obWVzc2FnZSk7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBjb25zdCBlbGVtZW50ID0gbm90aWZpY2F0aW9uSGFuZGxlcnMuZ2V0KG1lc3NhZ2UubWV0aG9kKTtcbiAgICAgICAgICAgIGlmIChlbGVtZW50KSB7XG4gICAgICAgICAgICAgICAgbm90aWZpY2F0aW9uSGFuZGxlciA9IGVsZW1lbnQuaGFuZGxlcjtcbiAgICAgICAgICAgICAgICB0eXBlID0gZWxlbWVudC50eXBlO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIGlmIChub3RpZmljYXRpb25IYW5kbGVyIHx8IHN0YXJOb3RpZmljYXRpb25IYW5kbGVyKSB7XG4gICAgICAgICAgICB0cnkge1xuICAgICAgICAgICAgICAgIHRyYWNlUmVjZWl2ZWROb3RpZmljYXRpb24obWVzc2FnZSk7XG4gICAgICAgICAgICAgICAgaWYgKG5vdGlmaWNhdGlvbkhhbmRsZXIpIHtcbiAgICAgICAgICAgICAgICAgICAgaWYgKG1lc3NhZ2UucGFyYW1zID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICh0eXBlICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBpZiAodHlwZS5udW1iZXJPZlBhcmFtcyAhPT0gMCAmJiB0eXBlLnBhcmFtZXRlclN0cnVjdHVyZXMgIT09IG1lc3NhZ2VzXzEuUGFyYW1ldGVyU3RydWN0dXJlcy5ieU5hbWUpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbG9nZ2VyLmVycm9yKGBOb3RpZmljYXRpb24gJHttZXNzYWdlLm1ldGhvZH0gZGVmaW5lcyAke3R5cGUubnVtYmVyT2ZQYXJhbXN9IHBhcmFtcyBidXQgcmVjZWl2ZWQgbm9uZS5gKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBub3RpZmljYXRpb25IYW5kbGVyKCk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZWxzZSBpZiAoQXJyYXkuaXNBcnJheShtZXNzYWdlLnBhcmFtcykpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIC8vIFRoZXJlIGFyZSBKU09OLVJQQyBsaWJyYXJpZXMgdGhhdCBzZW5kIHByb2dyZXNzIG1lc3NhZ2UgYXMgcG9zaXRpb25hbCBwYXJhbXMgYWx0aG91Z2hcbiAgICAgICAgICAgICAgICAgICAgICAgIC8vIHNwZWNpZmllZCBhcyBuYW1lZC4gU28gY29udmVydCB0aGVtIGlmIHRoaXMgaXMgdGhlIGNhc2UuXG4gICAgICAgICAgICAgICAgICAgICAgICBjb25zdCBwYXJhbXMgPSBtZXNzYWdlLnBhcmFtcztcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmIChtZXNzYWdlLm1ldGhvZCA9PT0gUHJvZ3Jlc3NOb3RpZmljYXRpb24udHlwZS5tZXRob2QgJiYgcGFyYW1zLmxlbmd0aCA9PT0gMiAmJiBQcm9ncmVzc1Rva2VuLmlzKHBhcmFtc1swXSkpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBub3RpZmljYXRpb25IYW5kbGVyKHsgdG9rZW46IHBhcmFtc1swXSwgdmFsdWU6IHBhcmFtc1sxXSB9KTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGlmICh0eXBlICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaWYgKHR5cGUucGFyYW1ldGVyU3RydWN0dXJlcyA9PT0gbWVzc2FnZXNfMS5QYXJhbWV0ZXJTdHJ1Y3R1cmVzLmJ5TmFtZSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbG9nZ2VyLmVycm9yKGBOb3RpZmljYXRpb24gJHttZXNzYWdlLm1ldGhvZH0gZGVmaW5lcyBwYXJhbWV0ZXJzIGJ5IG5hbWUgYnV0IHJlY2VpdmVkIHBhcmFtZXRlcnMgYnkgcG9zaXRpb25gKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBpZiAodHlwZS5udW1iZXJPZlBhcmFtcyAhPT0gbWVzc2FnZS5wYXJhbXMubGVuZ3RoKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBsb2dnZXIuZXJyb3IoYE5vdGlmaWNhdGlvbiAke21lc3NhZ2UubWV0aG9kfSBkZWZpbmVzICR7dHlwZS5udW1iZXJPZlBhcmFtc30gcGFyYW1zIGJ1dCByZWNlaXZlZCAke3BhcmFtcy5sZW5ndGh9IGFyZ3VtZW50c2ApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5vdGlmaWNhdGlvbkhhbmRsZXIoLi4ucGFyYW1zKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICh0eXBlICE9PSB1bmRlZmluZWQgJiYgdHlwZS5wYXJhbWV0ZXJTdHJ1Y3R1cmVzID09PSBtZXNzYWdlc18xLlBhcmFtZXRlclN0cnVjdHVyZXMuYnlQb3NpdGlvbikge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGxvZ2dlci5lcnJvcihgTm90aWZpY2F0aW9uICR7bWVzc2FnZS5tZXRob2R9IGRlZmluZXMgcGFyYW1ldGVycyBieSBwb3NpdGlvbiBidXQgcmVjZWl2ZWQgcGFyYW1ldGVycyBieSBuYW1lYCk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBub3RpZmljYXRpb25IYW5kbGVyKG1lc3NhZ2UucGFyYW1zKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIGlmIChzdGFyTm90aWZpY2F0aW9uSGFuZGxlcikge1xuICAgICAgICAgICAgICAgICAgICBzdGFyTm90aWZpY2F0aW9uSGFuZGxlcihtZXNzYWdlLm1ldGhvZCwgbWVzc2FnZS5wYXJhbXMpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNhdGNoIChlcnJvcikge1xuICAgICAgICAgICAgICAgIGlmIChlcnJvci5tZXNzYWdlKSB7XG4gICAgICAgICAgICAgICAgICAgIGxvZ2dlci5lcnJvcihgTm90aWZpY2F0aW9uIGhhbmRsZXIgJyR7bWVzc2FnZS5tZXRob2R9JyBmYWlsZWQgd2l0aCBtZXNzYWdlOiAke2Vycm9yLm1lc3NhZ2V9YCk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICBsb2dnZXIuZXJyb3IoYE5vdGlmaWNhdGlvbiBoYW5kbGVyICcke21lc3NhZ2UubWV0aG9kfScgZmFpbGVkIHVuZXhwZWN0ZWRseS5gKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICB1bmhhbmRsZWROb3RpZmljYXRpb25FbWl0dGVyLmZpcmUobWVzc2FnZSk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgZnVuY3Rpb24gaGFuZGxlSW52YWxpZE1lc3NhZ2UobWVzc2FnZSkge1xuICAgICAgICBpZiAoIW1lc3NhZ2UpIHtcbiAgICAgICAgICAgIGxvZ2dlci5lcnJvcignUmVjZWl2ZWQgZW1wdHkgbWVzc2FnZS4nKTtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBsb2dnZXIuZXJyb3IoYFJlY2VpdmVkIG1lc3NhZ2Ugd2hpY2ggaXMgbmVpdGhlciBhIHJlc3BvbnNlIG5vciBhIG5vdGlmaWNhdGlvbiBtZXNzYWdlOlxcbiR7SlNPTi5zdHJpbmdpZnkobWVzc2FnZSwgbnVsbCwgNCl9YCk7XG4gICAgICAgIC8vIFRlc3Qgd2hldGhlciB3ZSBmaW5kIGFuIGlkIHRvIHJlamVjdCB0aGUgcHJvbWlzZVxuICAgICAgICBjb25zdCByZXNwb25zZU1lc3NhZ2UgPSBtZXNzYWdlO1xuICAgICAgICBpZiAoSXMuc3RyaW5nKHJlc3BvbnNlTWVzc2FnZS5pZCkgfHwgSXMubnVtYmVyKHJlc3BvbnNlTWVzc2FnZS5pZCkpIHtcbiAgICAgICAgICAgIGNvbnN0IGtleSA9IHJlc3BvbnNlTWVzc2FnZS5pZDtcbiAgICAgICAgICAgIGNvbnN0IHJlc3BvbnNlSGFuZGxlciA9IHJlc3BvbnNlUHJvbWlzZXMuZ2V0KGtleSk7XG4gICAgICAgICAgICBpZiAocmVzcG9uc2VIYW5kbGVyKSB7XG4gICAgICAgICAgICAgICAgcmVzcG9uc2VIYW5kbGVyLnJlamVjdChuZXcgRXJyb3IoJ1RoZSByZWNlaXZlZCByZXNwb25zZSBoYXMgbmVpdGhlciBhIHJlc3VsdCBub3IgYW4gZXJyb3IgcHJvcGVydHkuJykpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIHN0cmluZ2lmeVRyYWNlKHBhcmFtcykge1xuICAgICAgICBpZiAocGFyYW1zID09PSB1bmRlZmluZWQgfHwgcGFyYW1zID09PSBudWxsKSB7XG4gICAgICAgICAgICByZXR1cm4gdW5kZWZpbmVkO1xuICAgICAgICB9XG4gICAgICAgIHN3aXRjaCAodHJhY2UpIHtcbiAgICAgICAgICAgIGNhc2UgVHJhY2UuVmVyYm9zZTpcbiAgICAgICAgICAgICAgICByZXR1cm4gSlNPTi5zdHJpbmdpZnkocGFyYW1zLCBudWxsLCA0KTtcbiAgICAgICAgICAgIGNhc2UgVHJhY2UuQ29tcGFjdDpcbiAgICAgICAgICAgICAgICByZXR1cm4gSlNPTi5zdHJpbmdpZnkocGFyYW1zKTtcbiAgICAgICAgICAgIGRlZmF1bHQ6XG4gICAgICAgICAgICAgICAgcmV0dXJuIHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgIH1cbiAgICBmdW5jdGlvbiB0cmFjZVNlbmRpbmdSZXF1ZXN0KG1lc3NhZ2UpIHtcbiAgICAgICAgaWYgKHRyYWNlID09PSBUcmFjZS5PZmYgfHwgIXRyYWNlcikge1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGlmICh0cmFjZUZvcm1hdCA9PT0gVHJhY2VGb3JtYXQuVGV4dCkge1xuICAgICAgICAgICAgbGV0IGRhdGEgPSB1bmRlZmluZWQ7XG4gICAgICAgICAgICBpZiAoKHRyYWNlID09PSBUcmFjZS5WZXJib3NlIHx8IHRyYWNlID09PSBUcmFjZS5Db21wYWN0KSAmJiBtZXNzYWdlLnBhcmFtcykge1xuICAgICAgICAgICAgICAgIGRhdGEgPSBgUGFyYW1zOiAke3N0cmluZ2lmeVRyYWNlKG1lc3NhZ2UucGFyYW1zKX1cXG5cXG5gO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgdHJhY2VyLmxvZyhgU2VuZGluZyByZXF1ZXN0ICcke21lc3NhZ2UubWV0aG9kfSAtICgke21lc3NhZ2UuaWR9KScuYCwgZGF0YSk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBsb2dMU1BNZXNzYWdlKCdzZW5kLXJlcXVlc3QnLCBtZXNzYWdlKTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBmdW5jdGlvbiB0cmFjZVNlbmRpbmdOb3RpZmljYXRpb24obWVzc2FnZSkge1xuICAgICAgICBpZiAodHJhY2UgPT09IFRyYWNlLk9mZiB8fCAhdHJhY2VyKSB7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgICAgaWYgKHRyYWNlRm9ybWF0ID09PSBUcmFjZUZvcm1hdC5UZXh0KSB7XG4gICAgICAgICAgICBsZXQgZGF0YSA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIGlmICh0cmFjZSA9PT0gVHJhY2UuVmVyYm9zZSB8fCB0cmFjZSA9PT0gVHJhY2UuQ29tcGFjdCkge1xuICAgICAgICAgICAgICAgIGlmIChtZXNzYWdlLnBhcmFtcykge1xuICAgICAgICAgICAgICAgICAgICBkYXRhID0gYFBhcmFtczogJHtzdHJpbmdpZnlUcmFjZShtZXNzYWdlLnBhcmFtcyl9XFxuXFxuYDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIGRhdGEgPSAnTm8gcGFyYW1ldGVycyBwcm92aWRlZC5cXG5cXG4nO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHRyYWNlci5sb2coYFNlbmRpbmcgbm90aWZpY2F0aW9uICcke21lc3NhZ2UubWV0aG9kfScuYCwgZGF0YSk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBsb2dMU1BNZXNzYWdlKCdzZW5kLW5vdGlmaWNhdGlvbicsIG1lc3NhZ2UpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIHRyYWNlU2VuZGluZ1Jlc3BvbnNlKG1lc3NhZ2UsIG1ldGhvZCwgc3RhcnRUaW1lKSB7XG4gICAgICAgIGlmICh0cmFjZSA9PT0gVHJhY2UuT2ZmIHx8ICF0cmFjZXIpIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBpZiAodHJhY2VGb3JtYXQgPT09IFRyYWNlRm9ybWF0LlRleHQpIHtcbiAgICAgICAgICAgIGxldCBkYXRhID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgaWYgKHRyYWNlID09PSBUcmFjZS5WZXJib3NlIHx8IHRyYWNlID09PSBUcmFjZS5Db21wYWN0KSB7XG4gICAgICAgICAgICAgICAgaWYgKG1lc3NhZ2UuZXJyb3IgJiYgbWVzc2FnZS5lcnJvci5kYXRhKSB7XG4gICAgICAgICAgICAgICAgICAgIGRhdGEgPSBgRXJyb3IgZGF0YTogJHtzdHJpbmdpZnlUcmFjZShtZXNzYWdlLmVycm9yLmRhdGEpfVxcblxcbmA7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICBpZiAobWVzc2FnZS5yZXN1bHQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGRhdGEgPSBgUmVzdWx0OiAke3N0cmluZ2lmeVRyYWNlKG1lc3NhZ2UucmVzdWx0KX1cXG5cXG5gO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2UgaWYgKG1lc3NhZ2UuZXJyb3IgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgZGF0YSA9ICdObyByZXN1bHQgcmV0dXJuZWQuXFxuXFxuJztcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHRyYWNlci5sb2coYFNlbmRpbmcgcmVzcG9uc2UgJyR7bWV0aG9kfSAtICgke21lc3NhZ2UuaWR9KScuIFByb2Nlc3NpbmcgcmVxdWVzdCB0b29rICR7RGF0ZS5ub3coKSAtIHN0YXJ0VGltZX1tc2AsIGRhdGEpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgbG9nTFNQTWVzc2FnZSgnc2VuZC1yZXNwb25zZScsIG1lc3NhZ2UpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIHRyYWNlUmVjZWl2ZWRSZXF1ZXN0KG1lc3NhZ2UpIHtcbiAgICAgICAgaWYgKHRyYWNlID09PSBUcmFjZS5PZmYgfHwgIXRyYWNlcikge1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGlmICh0cmFjZUZvcm1hdCA9PT0gVHJhY2VGb3JtYXQuVGV4dCkge1xuICAgICAgICAgICAgbGV0IGRhdGEgPSB1bmRlZmluZWQ7XG4gICAgICAgICAgICBpZiAoKHRyYWNlID09PSBUcmFjZS5WZXJib3NlIHx8IHRyYWNlID09PSBUcmFjZS5Db21wYWN0KSAmJiBtZXNzYWdlLnBhcmFtcykge1xuICAgICAgICAgICAgICAgIGRhdGEgPSBgUGFyYW1zOiAke3N0cmluZ2lmeVRyYWNlKG1lc3NhZ2UucGFyYW1zKX1cXG5cXG5gO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgdHJhY2VyLmxvZyhgUmVjZWl2ZWQgcmVxdWVzdCAnJHttZXNzYWdlLm1ldGhvZH0gLSAoJHttZXNzYWdlLmlkfSknLmAsIGRhdGEpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgbG9nTFNQTWVzc2FnZSgncmVjZWl2ZS1yZXF1ZXN0JywgbWVzc2FnZSk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgZnVuY3Rpb24gdHJhY2VSZWNlaXZlZE5vdGlmaWNhdGlvbihtZXNzYWdlKSB7XG4gICAgICAgIGlmICh0cmFjZSA9PT0gVHJhY2UuT2ZmIHx8ICF0cmFjZXIgfHwgbWVzc2FnZS5tZXRob2QgPT09IExvZ1RyYWNlTm90aWZpY2F0aW9uLnR5cGUubWV0aG9kKSB7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgICAgaWYgKHRyYWNlRm9ybWF0ID09PSBUcmFjZUZvcm1hdC5UZXh0KSB7XG4gICAgICAgICAgICBsZXQgZGF0YSA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIGlmICh0cmFjZSA9PT0gVHJhY2UuVmVyYm9zZSB8fCB0cmFjZSA9PT0gVHJhY2UuQ29tcGFjdCkge1xuICAgICAgICAgICAgICAgIGlmIChtZXNzYWdlLnBhcmFtcykge1xuICAgICAgICAgICAgICAgICAgICBkYXRhID0gYFBhcmFtczogJHtzdHJpbmdpZnlUcmFjZShtZXNzYWdlLnBhcmFtcyl9XFxuXFxuYDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIGRhdGEgPSAnTm8gcGFyYW1ldGVycyBwcm92aWRlZC5cXG5cXG4nO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHRyYWNlci5sb2coYFJlY2VpdmVkIG5vdGlmaWNhdGlvbiAnJHttZXNzYWdlLm1ldGhvZH0nLmAsIGRhdGEpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgbG9nTFNQTWVzc2FnZSgncmVjZWl2ZS1ub3RpZmljYXRpb24nLCBtZXNzYWdlKTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBmdW5jdGlvbiB0cmFjZVJlY2VpdmVkUmVzcG9uc2UobWVzc2FnZSwgcmVzcG9uc2VQcm9taXNlKSB7XG4gICAgICAgIGlmICh0cmFjZSA9PT0gVHJhY2UuT2ZmIHx8ICF0cmFjZXIpIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBpZiAodHJhY2VGb3JtYXQgPT09IFRyYWNlRm9ybWF0LlRleHQpIHtcbiAgICAgICAgICAgIGxldCBkYXRhID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgaWYgKHRyYWNlID09PSBUcmFjZS5WZXJib3NlIHx8IHRyYWNlID09PSBUcmFjZS5Db21wYWN0KSB7XG4gICAgICAgICAgICAgICAgaWYgKG1lc3NhZ2UuZXJyb3IgJiYgbWVzc2FnZS5lcnJvci5kYXRhKSB7XG4gICAgICAgICAgICAgICAgICAgIGRhdGEgPSBgRXJyb3IgZGF0YTogJHtzdHJpbmdpZnlUcmFjZShtZXNzYWdlLmVycm9yLmRhdGEpfVxcblxcbmA7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICBpZiAobWVzc2FnZS5yZXN1bHQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGRhdGEgPSBgUmVzdWx0OiAke3N0cmluZ2lmeVRyYWNlKG1lc3NhZ2UucmVzdWx0KX1cXG5cXG5gO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2UgaWYgKG1lc3NhZ2UuZXJyb3IgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgZGF0YSA9ICdObyByZXN1bHQgcmV0dXJuZWQuXFxuXFxuJztcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmIChyZXNwb25zZVByb21pc2UpIHtcbiAgICAgICAgICAgICAgICBjb25zdCBlcnJvciA9IG1lc3NhZ2UuZXJyb3IgPyBgIFJlcXVlc3QgZmFpbGVkOiAke21lc3NhZ2UuZXJyb3IubWVzc2FnZX0gKCR7bWVzc2FnZS5lcnJvci5jb2RlfSkuYCA6ICcnO1xuICAgICAgICAgICAgICAgIHRyYWNlci5sb2coYFJlY2VpdmVkIHJlc3BvbnNlICcke3Jlc3BvbnNlUHJvbWlzZS5tZXRob2R9IC0gKCR7bWVzc2FnZS5pZH0pJyBpbiAke0RhdGUubm93KCkgLSByZXNwb25zZVByb21pc2UudGltZXJTdGFydH1tcy4ke2Vycm9yfWAsIGRhdGEpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgdHJhY2VyLmxvZyhgUmVjZWl2ZWQgcmVzcG9uc2UgJHttZXNzYWdlLmlkfSB3aXRob3V0IGFjdGl2ZSByZXNwb25zZSBwcm9taXNlLmAsIGRhdGEpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgbG9nTFNQTWVzc2FnZSgncmVjZWl2ZS1yZXNwb25zZScsIG1lc3NhZ2UpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIGxvZ0xTUE1lc3NhZ2UodHlwZSwgbWVzc2FnZSkge1xuICAgICAgICBpZiAoIXRyYWNlciB8fCB0cmFjZSA9PT0gVHJhY2UuT2ZmKSB7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgICAgY29uc3QgbHNwTWVzc2FnZSA9IHtcbiAgICAgICAgICAgIGlzTFNQTWVzc2FnZTogdHJ1ZSxcbiAgICAgICAgICAgIHR5cGUsXG4gICAgICAgICAgICBtZXNzYWdlLFxuICAgICAgICAgICAgdGltZXN0YW1wOiBEYXRlLm5vdygpXG4gICAgICAgIH07XG4gICAgICAgIHRyYWNlci5sb2cobHNwTWVzc2FnZSk7XG4gICAgfVxuICAgIGZ1bmN0aW9uIHRocm93SWZDbG9zZWRPckRpc3Bvc2VkKCkge1xuICAgICAgICBpZiAoaXNDbG9zZWQoKSkge1xuICAgICAgICAgICAgdGhyb3cgbmV3IENvbm5lY3Rpb25FcnJvcihDb25uZWN0aW9uRXJyb3JzLkNsb3NlZCwgJ0Nvbm5lY3Rpb24gaXMgY2xvc2VkLicpO1xuICAgICAgICB9XG4gICAgICAgIGlmIChpc0Rpc3Bvc2VkKCkpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBDb25uZWN0aW9uRXJyb3IoQ29ubmVjdGlvbkVycm9ycy5EaXNwb3NlZCwgJ0Nvbm5lY3Rpb24gaXMgZGlzcG9zZWQuJyk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgZnVuY3Rpb24gdGhyb3dJZkxpc3RlbmluZygpIHtcbiAgICAgICAgaWYgKGlzTGlzdGVuaW5nKCkpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBDb25uZWN0aW9uRXJyb3IoQ29ubmVjdGlvbkVycm9ycy5BbHJlYWR5TGlzdGVuaW5nLCAnQ29ubmVjdGlvbiBpcyBhbHJlYWR5IGxpc3RlbmluZycpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIHRocm93SWZOb3RMaXN0ZW5pbmcoKSB7XG4gICAgICAgIGlmICghaXNMaXN0ZW5pbmcoKSkge1xuICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdDYWxsIGxpc3RlbigpIGZpcnN0LicpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIHVuZGVmaW5lZFRvTnVsbChwYXJhbSkge1xuICAgICAgICBpZiAocGFyYW0gPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmV0dXJuIG51bGw7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICByZXR1cm4gcGFyYW07XG4gICAgICAgIH1cbiAgICB9XG4gICAgZnVuY3Rpb24gbnVsbFRvVW5kZWZpbmVkKHBhcmFtKSB7XG4gICAgICAgIGlmIChwYXJhbSA9PT0gbnVsbCkge1xuICAgICAgICAgICAgcmV0dXJuIHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHJldHVybiBwYXJhbTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBmdW5jdGlvbiBpc05hbWVkUGFyYW0ocGFyYW0pIHtcbiAgICAgICAgcmV0dXJuIHBhcmFtICE9PSB1bmRlZmluZWQgJiYgcGFyYW0gIT09IG51bGwgJiYgIUFycmF5LmlzQXJyYXkocGFyYW0pICYmIHR5cGVvZiBwYXJhbSA9PT0gJ29iamVjdCc7XG4gICAgfVxuICAgIGZ1bmN0aW9uIGNvbXB1dGVTaW5nbGVQYXJhbShwYXJhbWV0ZXJTdHJ1Y3R1cmVzLCBwYXJhbSkge1xuICAgICAgICBzd2l0Y2ggKHBhcmFtZXRlclN0cnVjdHVyZXMpIHtcbiAgICAgICAgICAgIGNhc2UgbWVzc2FnZXNfMS5QYXJhbWV0ZXJTdHJ1Y3R1cmVzLmF1dG86XG4gICAgICAgICAgICAgICAgaWYgKGlzTmFtZWRQYXJhbShwYXJhbSkpIHtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIG51bGxUb1VuZGVmaW5lZChwYXJhbSk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4gW3VuZGVmaW5lZFRvTnVsbChwYXJhbSldO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNhc2UgbWVzc2FnZXNfMS5QYXJhbWV0ZXJTdHJ1Y3R1cmVzLmJ5TmFtZTpcbiAgICAgICAgICAgICAgICBpZiAoIWlzTmFtZWRQYXJhbShwYXJhbSkpIHtcbiAgICAgICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBSZWNlaXZlZCBwYXJhbWV0ZXJzIGJ5IG5hbWUgYnV0IHBhcmFtIGlzIG5vdCBhbiBvYmplY3QgbGl0ZXJhbC5gKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgcmV0dXJuIG51bGxUb1VuZGVmaW5lZChwYXJhbSk7XG4gICAgICAgICAgICBjYXNlIG1lc3NhZ2VzXzEuUGFyYW1ldGVyU3RydWN0dXJlcy5ieVBvc2l0aW9uOlxuICAgICAgICAgICAgICAgIHJldHVybiBbdW5kZWZpbmVkVG9OdWxsKHBhcmFtKV07XG4gICAgICAgICAgICBkZWZhdWx0OlxuICAgICAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgVW5rbm93biBwYXJhbWV0ZXIgc3RydWN0dXJlICR7cGFyYW1ldGVyU3RydWN0dXJlcy50b1N0cmluZygpfWApO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIGNvbXB1dGVNZXNzYWdlUGFyYW1zKHR5cGUsIHBhcmFtcykge1xuICAgICAgICBsZXQgcmVzdWx0O1xuICAgICAgICBjb25zdCBudW1iZXJPZlBhcmFtcyA9IHR5cGUubnVtYmVyT2ZQYXJhbXM7XG4gICAgICAgIHN3aXRjaCAobnVtYmVyT2ZQYXJhbXMpIHtcbiAgICAgICAgICAgIGNhc2UgMDpcbiAgICAgICAgICAgICAgICByZXN1bHQgPSB1bmRlZmluZWQ7XG4gICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICBjYXNlIDE6XG4gICAgICAgICAgICAgICAgcmVzdWx0ID0gY29tcHV0ZVNpbmdsZVBhcmFtKHR5cGUucGFyYW1ldGVyU3RydWN0dXJlcywgcGFyYW1zWzBdKTtcbiAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgIGRlZmF1bHQ6XG4gICAgICAgICAgICAgICAgcmVzdWx0ID0gW107XG4gICAgICAgICAgICAgICAgZm9yIChsZXQgaSA9IDA7IGkgPCBwYXJhbXMubGVuZ3RoICYmIGkgPCBudW1iZXJPZlBhcmFtczsgaSsrKSB7XG4gICAgICAgICAgICAgICAgICAgIHJlc3VsdC5wdXNoKHVuZGVmaW5lZFRvTnVsbChwYXJhbXNbaV0pKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgaWYgKHBhcmFtcy5sZW5ndGggPCBudW1iZXJPZlBhcmFtcykge1xuICAgICAgICAgICAgICAgICAgICBmb3IgKGxldCBpID0gcGFyYW1zLmxlbmd0aDsgaSA8IG51bWJlck9mUGFyYW1zOyBpKyspIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJlc3VsdC5wdXNoKG51bGwpO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxuICAgIGNvbnN0IGNvbm5lY3Rpb24gPSB7XG4gICAgICAgIHNlbmROb3RpZmljYXRpb246ICh0eXBlLCAuLi5hcmdzKSA9PiB7XG4gICAgICAgICAgICB0aHJvd0lmQ2xvc2VkT3JEaXNwb3NlZCgpO1xuICAgICAgICAgICAgbGV0IG1ldGhvZDtcbiAgICAgICAgICAgIGxldCBtZXNzYWdlUGFyYW1zO1xuICAgICAgICAgICAgaWYgKElzLnN0cmluZyh0eXBlKSkge1xuICAgICAgICAgICAgICAgIG1ldGhvZCA9IHR5cGU7XG4gICAgICAgICAgICAgICAgY29uc3QgZmlyc3QgPSBhcmdzWzBdO1xuICAgICAgICAgICAgICAgIGxldCBwYXJhbVN0YXJ0ID0gMDtcbiAgICAgICAgICAgICAgICBsZXQgcGFyYW1ldGVyU3RydWN0dXJlcyA9IG1lc3NhZ2VzXzEuUGFyYW1ldGVyU3RydWN0dXJlcy5hdXRvO1xuICAgICAgICAgICAgICAgIGlmIChtZXNzYWdlc18xLlBhcmFtZXRlclN0cnVjdHVyZXMuaXMoZmlyc3QpKSB7XG4gICAgICAgICAgICAgICAgICAgIHBhcmFtU3RhcnQgPSAxO1xuICAgICAgICAgICAgICAgICAgICBwYXJhbWV0ZXJTdHJ1Y3R1cmVzID0gZmlyc3Q7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGxldCBwYXJhbUVuZCA9IGFyZ3MubGVuZ3RoO1xuICAgICAgICAgICAgICAgIGNvbnN0IG51bWJlck9mUGFyYW1zID0gcGFyYW1FbmQgLSBwYXJhbVN0YXJ0O1xuICAgICAgICAgICAgICAgIHN3aXRjaCAobnVtYmVyT2ZQYXJhbXMpIHtcbiAgICAgICAgICAgICAgICAgICAgY2FzZSAwOlxuICAgICAgICAgICAgICAgICAgICAgICAgbWVzc2FnZVBhcmFtcyA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICBjYXNlIDE6XG4gICAgICAgICAgICAgICAgICAgICAgICBtZXNzYWdlUGFyYW1zID0gY29tcHV0ZVNpbmdsZVBhcmFtKHBhcmFtZXRlclN0cnVjdHVyZXMsIGFyZ3NbcGFyYW1TdGFydF0pO1xuICAgICAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgICAgIGRlZmF1bHQ6XG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAocGFyYW1ldGVyU3RydWN0dXJlcyA9PT0gbWVzc2FnZXNfMS5QYXJhbWV0ZXJTdHJ1Y3R1cmVzLmJ5TmFtZSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgUmVjZWl2ZWQgJHtudW1iZXJPZlBhcmFtc30gcGFyYW1ldGVycyBmb3IgJ2J5IE5hbWUnIG5vdGlmaWNhdGlvbiBwYXJhbWV0ZXIgc3RydWN0dXJlLmApO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgbWVzc2FnZVBhcmFtcyA9IGFyZ3Muc2xpY2UocGFyYW1TdGFydCwgcGFyYW1FbmQpLm1hcCh2YWx1ZSA9PiB1bmRlZmluZWRUb051bGwodmFsdWUpKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIGNvbnN0IHBhcmFtcyA9IGFyZ3M7XG4gICAgICAgICAgICAgICAgbWV0aG9kID0gdHlwZS5tZXRob2Q7XG4gICAgICAgICAgICAgICAgbWVzc2FnZVBhcmFtcyA9IGNvbXB1dGVNZXNzYWdlUGFyYW1zKHR5cGUsIHBhcmFtcyk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBjb25zdCBub3RpZmljYXRpb25NZXNzYWdlID0ge1xuICAgICAgICAgICAgICAgIGpzb25ycGM6IHZlcnNpb24sXG4gICAgICAgICAgICAgICAgbWV0aG9kOiBtZXRob2QsXG4gICAgICAgICAgICAgICAgcGFyYW1zOiBtZXNzYWdlUGFyYW1zXG4gICAgICAgICAgICB9O1xuICAgICAgICAgICAgdHJhY2VTZW5kaW5nTm90aWZpY2F0aW9uKG5vdGlmaWNhdGlvbk1lc3NhZ2UpO1xuICAgICAgICAgICAgcmV0dXJuIG1lc3NhZ2VXcml0ZXIud3JpdGUobm90aWZpY2F0aW9uTWVzc2FnZSkuY2F0Y2goKGVycm9yKSA9PiB7XG4gICAgICAgICAgICAgICAgbG9nZ2VyLmVycm9yKGBTZW5kaW5nIG5vdGlmaWNhdGlvbiBmYWlsZWQuYCk7XG4gICAgICAgICAgICAgICAgdGhyb3cgZXJyb3I7XG4gICAgICAgICAgICB9KTtcbiAgICAgICAgfSxcbiAgICAgICAgb25Ob3RpZmljYXRpb246ICh0eXBlLCBoYW5kbGVyKSA9PiB7XG4gICAgICAgICAgICB0aHJvd0lmQ2xvc2VkT3JEaXNwb3NlZCgpO1xuICAgICAgICAgICAgbGV0IG1ldGhvZDtcbiAgICAgICAgICAgIGlmIChJcy5mdW5jKHR5cGUpKSB7XG4gICAgICAgICAgICAgICAgc3Rhck5vdGlmaWNhdGlvbkhhbmRsZXIgPSB0eXBlO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSBpZiAoaGFuZGxlcikge1xuICAgICAgICAgICAgICAgIGlmIChJcy5zdHJpbmcodHlwZSkpIHtcbiAgICAgICAgICAgICAgICAgICAgbWV0aG9kID0gdHlwZTtcbiAgICAgICAgICAgICAgICAgICAgbm90aWZpY2F0aW9uSGFuZGxlcnMuc2V0KHR5cGUsIHsgdHlwZTogdW5kZWZpbmVkLCBoYW5kbGVyIH0pO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgbWV0aG9kID0gdHlwZS5tZXRob2Q7XG4gICAgICAgICAgICAgICAgICAgIG5vdGlmaWNhdGlvbkhhbmRsZXJzLnNldCh0eXBlLm1ldGhvZCwgeyB0eXBlLCBoYW5kbGVyIH0pO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICAgICAgZGlzcG9zZTogKCkgPT4ge1xuICAgICAgICAgICAgICAgICAgICBpZiAobWV0aG9kICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIG5vdGlmaWNhdGlvbkhhbmRsZXJzLmRlbGV0ZShtZXRob2QpO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgc3Rhck5vdGlmaWNhdGlvbkhhbmRsZXIgPSB1bmRlZmluZWQ7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9O1xuICAgICAgICB9LFxuICAgICAgICBvblByb2dyZXNzOiAoX3R5cGUsIHRva2VuLCBoYW5kbGVyKSA9PiB7XG4gICAgICAgICAgICBpZiAocHJvZ3Jlc3NIYW5kbGVycy5oYXModG9rZW4pKSB7XG4gICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBQcm9ncmVzcyBoYW5kbGVyIGZvciB0b2tlbiAke3Rva2VufSBhbHJlYWR5IHJlZ2lzdGVyZWRgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHByb2dyZXNzSGFuZGxlcnMuc2V0KHRva2VuLCBoYW5kbGVyKTtcbiAgICAgICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICAgICAgZGlzcG9zZTogKCkgPT4ge1xuICAgICAgICAgICAgICAgICAgICBwcm9ncmVzc0hhbmRsZXJzLmRlbGV0ZSh0b2tlbik7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfTtcbiAgICAgICAgfSxcbiAgICAgICAgc2VuZFByb2dyZXNzOiAoX3R5cGUsIHRva2VuLCB2YWx1ZSkgPT4ge1xuICAgICAgICAgICAgLy8gVGhpcyBzaG91bGQgbm90IGF3YWl0IGJ1dCBzaW1wbGUgcmV0dXJuIHRvIGVuc3VyZSB0aGF0IHdlIGRvbid0IGhhdmUgYW5vdGhlclxuICAgICAgICAgICAgLy8gYXN5bmMgc2NoZWR1bGluZy4gT3RoZXJ3aXNlIG9uZSBzZW5kIGNvdWxkIG92ZXJ0YWtlIGFub3RoZXIgc2VuZC5cbiAgICAgICAgICAgIHJldHVybiBjb25uZWN0aW9uLnNlbmROb3RpZmljYXRpb24oUHJvZ3Jlc3NOb3RpZmljYXRpb24udHlwZSwgeyB0b2tlbiwgdmFsdWUgfSk7XG4gICAgICAgIH0sXG4gICAgICAgIG9uVW5oYW5kbGVkUHJvZ3Jlc3M6IHVuaGFuZGxlZFByb2dyZXNzRW1pdHRlci5ldmVudCxcbiAgICAgICAgc2VuZFJlcXVlc3Q6ICh0eXBlLCAuLi5hcmdzKSA9PiB7XG4gICAgICAgICAgICB0aHJvd0lmQ2xvc2VkT3JEaXNwb3NlZCgpO1xuICAgICAgICAgICAgdGhyb3dJZk5vdExpc3RlbmluZygpO1xuICAgICAgICAgICAgbGV0IG1ldGhvZDtcbiAgICAgICAgICAgIGxldCBtZXNzYWdlUGFyYW1zO1xuICAgICAgICAgICAgbGV0IHRva2VuID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgaWYgKElzLnN0cmluZyh0eXBlKSkge1xuICAgICAgICAgICAgICAgIG1ldGhvZCA9IHR5cGU7XG4gICAgICAgICAgICAgICAgY29uc3QgZmlyc3QgPSBhcmdzWzBdO1xuICAgICAgICAgICAgICAgIGNvbnN0IGxhc3QgPSBhcmdzW2FyZ3MubGVuZ3RoIC0gMV07XG4gICAgICAgICAgICAgICAgbGV0IHBhcmFtU3RhcnQgPSAwO1xuICAgICAgICAgICAgICAgIGxldCBwYXJhbWV0ZXJTdHJ1Y3R1cmVzID0gbWVzc2FnZXNfMS5QYXJhbWV0ZXJTdHJ1Y3R1cmVzLmF1dG87XG4gICAgICAgICAgICAgICAgaWYgKG1lc3NhZ2VzXzEuUGFyYW1ldGVyU3RydWN0dXJlcy5pcyhmaXJzdCkpIHtcbiAgICAgICAgICAgICAgICAgICAgcGFyYW1TdGFydCA9IDE7XG4gICAgICAgICAgICAgICAgICAgIHBhcmFtZXRlclN0cnVjdHVyZXMgPSBmaXJzdDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgbGV0IHBhcmFtRW5kID0gYXJncy5sZW5ndGg7XG4gICAgICAgICAgICAgICAgaWYgKGNhbmNlbGxhdGlvbl8xLkNhbmNlbGxhdGlvblRva2VuLmlzKGxhc3QpKSB7XG4gICAgICAgICAgICAgICAgICAgIHBhcmFtRW5kID0gcGFyYW1FbmQgLSAxO1xuICAgICAgICAgICAgICAgICAgICB0b2tlbiA9IGxhc3Q7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGNvbnN0IG51bWJlck9mUGFyYW1zID0gcGFyYW1FbmQgLSBwYXJhbVN0YXJ0O1xuICAgICAgICAgICAgICAgIHN3aXRjaCAobnVtYmVyT2ZQYXJhbXMpIHtcbiAgICAgICAgICAgICAgICAgICAgY2FzZSAwOlxuICAgICAgICAgICAgICAgICAgICAgICAgbWVzc2FnZVBhcmFtcyA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICBjYXNlIDE6XG4gICAgICAgICAgICAgICAgICAgICAgICBtZXNzYWdlUGFyYW1zID0gY29tcHV0ZVNpbmdsZVBhcmFtKHBhcmFtZXRlclN0cnVjdHVyZXMsIGFyZ3NbcGFyYW1TdGFydF0pO1xuICAgICAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgICAgIGRlZmF1bHQ6XG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAocGFyYW1ldGVyU3RydWN0dXJlcyA9PT0gbWVzc2FnZXNfMS5QYXJhbWV0ZXJTdHJ1Y3R1cmVzLmJ5TmFtZSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgUmVjZWl2ZWQgJHtudW1iZXJPZlBhcmFtc30gcGFyYW1ldGVycyBmb3IgJ2J5IE5hbWUnIHJlcXVlc3QgcGFyYW1ldGVyIHN0cnVjdHVyZS5gKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIG1lc3NhZ2VQYXJhbXMgPSBhcmdzLnNsaWNlKHBhcmFtU3RhcnQsIHBhcmFtRW5kKS5tYXAodmFsdWUgPT4gdW5kZWZpbmVkVG9OdWxsKHZhbHVlKSk7XG4gICAgICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICBjb25zdCBwYXJhbXMgPSBhcmdzO1xuICAgICAgICAgICAgICAgIG1ldGhvZCA9IHR5cGUubWV0aG9kO1xuICAgICAgICAgICAgICAgIG1lc3NhZ2VQYXJhbXMgPSBjb21wdXRlTWVzc2FnZVBhcmFtcyh0eXBlLCBwYXJhbXMpO1xuICAgICAgICAgICAgICAgIGNvbnN0IG51bWJlck9mUGFyYW1zID0gdHlwZS5udW1iZXJPZlBhcmFtcztcbiAgICAgICAgICAgICAgICB0b2tlbiA9IGNhbmNlbGxhdGlvbl8xLkNhbmNlbGxhdGlvblRva2VuLmlzKHBhcmFtc1tudW1iZXJPZlBhcmFtc10pID8gcGFyYW1zW251bWJlck9mUGFyYW1zXSA6IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNvbnN0IGlkID0gc2VxdWVuY2VOdW1iZXIrKztcbiAgICAgICAgICAgIGxldCBkaXNwb3NhYmxlO1xuICAgICAgICAgICAgaWYgKHRva2VuKSB7XG4gICAgICAgICAgICAgICAgZGlzcG9zYWJsZSA9IHRva2VuLm9uQ2FuY2VsbGF0aW9uUmVxdWVzdGVkKCgpID0+IHtcbiAgICAgICAgICAgICAgICAgICAgY29uc3QgcCA9IGNhbmNlbGxhdGlvblN0cmF0ZWd5LnNlbmRlci5zZW5kQ2FuY2VsbGF0aW9uKGNvbm5lY3Rpb24sIGlkKTtcbiAgICAgICAgICAgICAgICAgICAgaWYgKHAgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgbG9nZ2VyLmxvZyhgUmVjZWl2ZWQgbm8gcHJvbWlzZSBmcm9tIGNhbmNlbGxhdGlvbiBzdHJhdGVneSB3aGVuIGNhbmNlbGxpbmcgaWQgJHtpZH1gKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiBQcm9taXNlLnJlc29sdmUoKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiBwLmNhdGNoKCgpID0+IHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBsb2dnZXIubG9nKGBTZW5kaW5nIGNhbmNlbGxhdGlvbiBtZXNzYWdlcyBmb3IgaWQgJHtpZH0gZmFpbGVkYCk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9KTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgY29uc3QgcmVxdWVzdE1lc3NhZ2UgPSB7XG4gICAgICAgICAgICAgICAganNvbnJwYzogdmVyc2lvbixcbiAgICAgICAgICAgICAgICBpZDogaWQsXG4gICAgICAgICAgICAgICAgbWV0aG9kOiBtZXRob2QsXG4gICAgICAgICAgICAgICAgcGFyYW1zOiBtZXNzYWdlUGFyYW1zXG4gICAgICAgICAgICB9O1xuICAgICAgICAgICAgdHJhY2VTZW5kaW5nUmVxdWVzdChyZXF1ZXN0TWVzc2FnZSk7XG4gICAgICAgICAgICBpZiAodHlwZW9mIGNhbmNlbGxhdGlvblN0cmF0ZWd5LnNlbmRlci5lbmFibGVDYW5jZWxsYXRpb24gPT09ICdmdW5jdGlvbicpIHtcbiAgICAgICAgICAgICAgICBjYW5jZWxsYXRpb25TdHJhdGVneS5zZW5kZXIuZW5hYmxlQ2FuY2VsbGF0aW9uKHJlcXVlc3RNZXNzYWdlKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJldHVybiBuZXcgUHJvbWlzZShhc3luYyAocmVzb2x2ZSwgcmVqZWN0KSA9PiB7XG4gICAgICAgICAgICAgICAgY29uc3QgcmVzb2x2ZVdpdGhDbGVhbnVwID0gKHIpID0+IHtcbiAgICAgICAgICAgICAgICAgICAgcmVzb2x2ZShyKTtcbiAgICAgICAgICAgICAgICAgICAgY2FuY2VsbGF0aW9uU3RyYXRlZ3kuc2VuZGVyLmNsZWFudXAoaWQpO1xuICAgICAgICAgICAgICAgICAgICBkaXNwb3NhYmxlPy5kaXNwb3NlKCk7XG4gICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgICAgICBjb25zdCByZWplY3RXaXRoQ2xlYW51cCA9IChyKSA9PiB7XG4gICAgICAgICAgICAgICAgICAgIHJlamVjdChyKTtcbiAgICAgICAgICAgICAgICAgICAgY2FuY2VsbGF0aW9uU3RyYXRlZ3kuc2VuZGVyLmNsZWFudXAoaWQpO1xuICAgICAgICAgICAgICAgICAgICBkaXNwb3NhYmxlPy5kaXNwb3NlKCk7XG4gICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgICAgICBjb25zdCByZXNwb25zZVByb21pc2UgPSB7IG1ldGhvZDogbWV0aG9kLCB0aW1lclN0YXJ0OiBEYXRlLm5vdygpLCByZXNvbHZlOiByZXNvbHZlV2l0aENsZWFudXAsIHJlamVjdDogcmVqZWN0V2l0aENsZWFudXAgfTtcbiAgICAgICAgICAgICAgICB0cnkge1xuICAgICAgICAgICAgICAgICAgICByZXNwb25zZVByb21pc2VzLnNldChpZCwgcmVzcG9uc2VQcm9taXNlKTtcbiAgICAgICAgICAgICAgICAgICAgYXdhaXQgbWVzc2FnZVdyaXRlci53cml0ZShyZXF1ZXN0TWVzc2FnZSk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGNhdGNoIChlcnJvcikge1xuICAgICAgICAgICAgICAgICAgICAvLyBXcml0aW5nIHRoZSBtZXNzYWdlIGZhaWxlZC4gU28gd2UgbmVlZCB0byBkZWxldGUgaXQgZnJvbSB0aGUgcmVzcG9uc2UgcHJvbWlzZXMgYW5kXG4gICAgICAgICAgICAgICAgICAgIC8vIHJlamVjdCBpdC5cbiAgICAgICAgICAgICAgICAgICAgcmVzcG9uc2VQcm9taXNlcy5kZWxldGUoaWQpO1xuICAgICAgICAgICAgICAgICAgICByZXNwb25zZVByb21pc2UucmVqZWN0KG5ldyBtZXNzYWdlc18xLlJlc3BvbnNlRXJyb3IobWVzc2FnZXNfMS5FcnJvckNvZGVzLk1lc3NhZ2VXcml0ZUVycm9yLCBlcnJvci5tZXNzYWdlID8gZXJyb3IubWVzc2FnZSA6ICdVbmtub3duIHJlYXNvbicpKTtcbiAgICAgICAgICAgICAgICAgICAgbG9nZ2VyLmVycm9yKGBTZW5kaW5nIHJlcXVlc3QgZmFpbGVkLmApO1xuICAgICAgICAgICAgICAgICAgICB0aHJvdyBlcnJvcjtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9KTtcbiAgICAgICAgfSxcbiAgICAgICAgb25SZXF1ZXN0OiAodHlwZSwgaGFuZGxlcikgPT4ge1xuICAgICAgICAgICAgdGhyb3dJZkNsb3NlZE9yRGlzcG9zZWQoKTtcbiAgICAgICAgICAgIGxldCBtZXRob2QgPSBudWxsO1xuICAgICAgICAgICAgaWYgKFN0YXJSZXF1ZXN0SGFuZGxlci5pcyh0eXBlKSkge1xuICAgICAgICAgICAgICAgIG1ldGhvZCA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgICAgICBzdGFyUmVxdWVzdEhhbmRsZXIgPSB0eXBlO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSBpZiAoSXMuc3RyaW5nKHR5cGUpKSB7XG4gICAgICAgICAgICAgICAgbWV0aG9kID0gbnVsbDtcbiAgICAgICAgICAgICAgICBpZiAoaGFuZGxlciAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICAgICAgICAgIG1ldGhvZCA9IHR5cGU7XG4gICAgICAgICAgICAgICAgICAgIHJlcXVlc3RIYW5kbGVycy5zZXQodHlwZSwgeyBoYW5kbGVyOiBoYW5kbGVyLCB0eXBlOiB1bmRlZmluZWQgfSk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgaWYgKGhhbmRsZXIgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgICAgICBtZXRob2QgPSB0eXBlLm1ldGhvZDtcbiAgICAgICAgICAgICAgICAgICAgcmVxdWVzdEhhbmRsZXJzLnNldCh0eXBlLm1ldGhvZCwgeyB0eXBlLCBoYW5kbGVyIH0pO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICAgICAgZGlzcG9zZTogKCkgPT4ge1xuICAgICAgICAgICAgICAgICAgICBpZiAobWV0aG9kID09PSBudWxsKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgaWYgKG1ldGhvZCAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXF1ZXN0SGFuZGxlcnMuZGVsZXRlKG1ldGhvZCk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBzdGFyUmVxdWVzdEhhbmRsZXIgPSB1bmRlZmluZWQ7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9O1xuICAgICAgICB9LFxuICAgICAgICBoYXNQZW5kaW5nUmVzcG9uc2U6ICgpID0+IHtcbiAgICAgICAgICAgIHJldHVybiByZXNwb25zZVByb21pc2VzLnNpemUgPiAwO1xuICAgICAgICB9LFxuICAgICAgICB0cmFjZTogYXN5bmMgKF92YWx1ZSwgX3RyYWNlciwgc2VuZE5vdGlmaWNhdGlvbk9yVHJhY2VPcHRpb25zKSA9PiB7XG4gICAgICAgICAgICBsZXQgX3NlbmROb3RpZmljYXRpb24gPSBmYWxzZTtcbiAgICAgICAgICAgIGxldCBfdHJhY2VGb3JtYXQgPSBUcmFjZUZvcm1hdC5UZXh0O1xuICAgICAgICAgICAgaWYgKHNlbmROb3RpZmljYXRpb25PclRyYWNlT3B0aW9ucyAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICAgICAgaWYgKElzLmJvb2xlYW4oc2VuZE5vdGlmaWNhdGlvbk9yVHJhY2VPcHRpb25zKSkge1xuICAgICAgICAgICAgICAgICAgICBfc2VuZE5vdGlmaWNhdGlvbiA9IHNlbmROb3RpZmljYXRpb25PclRyYWNlT3B0aW9ucztcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIF9zZW5kTm90aWZpY2F0aW9uID0gc2VuZE5vdGlmaWNhdGlvbk9yVHJhY2VPcHRpb25zLnNlbmROb3RpZmljYXRpb24gfHwgZmFsc2U7XG4gICAgICAgICAgICAgICAgICAgIF90cmFjZUZvcm1hdCA9IHNlbmROb3RpZmljYXRpb25PclRyYWNlT3B0aW9ucy50cmFjZUZvcm1hdCB8fCBUcmFjZUZvcm1hdC5UZXh0O1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHRyYWNlID0gX3ZhbHVlO1xuICAgICAgICAgICAgdHJhY2VGb3JtYXQgPSBfdHJhY2VGb3JtYXQ7XG4gICAgICAgICAgICBpZiAodHJhY2UgPT09IFRyYWNlLk9mZikge1xuICAgICAgICAgICAgICAgIHRyYWNlciA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIHRyYWNlciA9IF90cmFjZXI7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAoX3NlbmROb3RpZmljYXRpb24gJiYgIWlzQ2xvc2VkKCkgJiYgIWlzRGlzcG9zZWQoKSkge1xuICAgICAgICAgICAgICAgIGF3YWl0IGNvbm5lY3Rpb24uc2VuZE5vdGlmaWNhdGlvbihTZXRUcmFjZU5vdGlmaWNhdGlvbi50eXBlLCB7IHZhbHVlOiBUcmFjZS50b1N0cmluZyhfdmFsdWUpIH0pO1xuICAgICAgICAgICAgfVxuICAgICAgICB9LFxuICAgICAgICBvbkVycm9yOiBlcnJvckVtaXR0ZXIuZXZlbnQsXG4gICAgICAgIG9uQ2xvc2U6IGNsb3NlRW1pdHRlci5ldmVudCxcbiAgICAgICAgb25VbmhhbmRsZWROb3RpZmljYXRpb246IHVuaGFuZGxlZE5vdGlmaWNhdGlvbkVtaXR0ZXIuZXZlbnQsXG4gICAgICAgIG9uRGlzcG9zZTogZGlzcG9zZUVtaXR0ZXIuZXZlbnQsXG4gICAgICAgIGVuZDogKCkgPT4ge1xuICAgICAgICAgICAgbWVzc2FnZVdyaXRlci5lbmQoKTtcbiAgICAgICAgfSxcbiAgICAgICAgZGlzcG9zZTogKCkgPT4ge1xuICAgICAgICAgICAgaWYgKGlzRGlzcG9zZWQoKSkge1xuICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHN0YXRlID0gQ29ubmVjdGlvblN0YXRlLkRpc3Bvc2VkO1xuICAgICAgICAgICAgZGlzcG9zZUVtaXR0ZXIuZmlyZSh1bmRlZmluZWQpO1xuICAgICAgICAgICAgY29uc3QgZXJyb3IgPSBuZXcgbWVzc2FnZXNfMS5SZXNwb25zZUVycm9yKG1lc3NhZ2VzXzEuRXJyb3JDb2Rlcy5QZW5kaW5nUmVzcG9uc2VSZWplY3RlZCwgJ1BlbmRpbmcgcmVzcG9uc2UgcmVqZWN0ZWQgc2luY2UgY29ubmVjdGlvbiBnb3QgZGlzcG9zZWQnKTtcbiAgICAgICAgICAgIGZvciAoY29uc3QgcHJvbWlzZSBvZiByZXNwb25zZVByb21pc2VzLnZhbHVlcygpKSB7XG4gICAgICAgICAgICAgICAgcHJvbWlzZS5yZWplY3QoZXJyb3IpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgcmVzcG9uc2VQcm9taXNlcyA9IG5ldyBNYXAoKTtcbiAgICAgICAgICAgIHJlcXVlc3RUb2tlbnMgPSBuZXcgTWFwKCk7XG4gICAgICAgICAgICBrbm93bkNhbmNlbGVkUmVxdWVzdHMgPSBuZXcgU2V0KCk7XG4gICAgICAgICAgICBtZXNzYWdlUXVldWUgPSBuZXcgbGlua2VkTWFwXzEuTGlua2VkTWFwKCk7XG4gICAgICAgICAgICAvLyBUZXN0IGZvciBiYWNrd2FyZHMgY29tcGF0aWJpbGl0eVxuICAgICAgICAgICAgaWYgKElzLmZ1bmMobWVzc2FnZVdyaXRlci5kaXNwb3NlKSkge1xuICAgICAgICAgICAgICAgIG1lc3NhZ2VXcml0ZXIuZGlzcG9zZSgpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaWYgKElzLmZ1bmMobWVzc2FnZVJlYWRlci5kaXNwb3NlKSkge1xuICAgICAgICAgICAgICAgIG1lc3NhZ2VSZWFkZXIuZGlzcG9zZSgpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9LFxuICAgICAgICBsaXN0ZW46ICgpID0+IHtcbiAgICAgICAgICAgIHRocm93SWZDbG9zZWRPckRpc3Bvc2VkKCk7XG4gICAgICAgICAgICB0aHJvd0lmTGlzdGVuaW5nKCk7XG4gICAgICAgICAgICBzdGF0ZSA9IENvbm5lY3Rpb25TdGF0ZS5MaXN0ZW5pbmc7XG4gICAgICAgICAgICBtZXNzYWdlUmVhZGVyLmxpc3RlbihjYWxsYmFjayk7XG4gICAgICAgIH0sXG4gICAgICAgIGluc3BlY3Q6ICgpID0+IHtcbiAgICAgICAgICAgIC8vIGVzbGludC1kaXNhYmxlLW5leHQtbGluZSBuby1jb25zb2xlXG4gICAgICAgICAgICAoMCwgcmFsXzEuZGVmYXVsdCkoKS5jb25zb2xlLmxvZygnaW5zcGVjdCcpO1xuICAgICAgICB9XG4gICAgfTtcbiAgICBjb25uZWN0aW9uLm9uTm90aWZpY2F0aW9uKExvZ1RyYWNlTm90aWZpY2F0aW9uLnR5cGUsIChwYXJhbXMpID0+IHtcbiAgICAgICAgaWYgKHRyYWNlID09PSBUcmFjZS5PZmYgfHwgIXRyYWNlcikge1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGNvbnN0IHZlcmJvc2UgPSB0cmFjZSA9PT0gVHJhY2UuVmVyYm9zZSB8fCB0cmFjZSA9PT0gVHJhY2UuQ29tcGFjdDtcbiAgICAgICAgdHJhY2VyLmxvZyhwYXJhbXMubWVzc2FnZSwgdmVyYm9zZSA/IHBhcmFtcy52ZXJib3NlIDogdW5kZWZpbmVkKTtcbiAgICB9KTtcbiAgICBjb25uZWN0aW9uLm9uTm90aWZpY2F0aW9uKFByb2dyZXNzTm90aWZpY2F0aW9uLnR5cGUsIChwYXJhbXMpID0+IHtcbiAgICAgICAgY29uc3QgaGFuZGxlciA9IHByb2dyZXNzSGFuZGxlcnMuZ2V0KHBhcmFtcy50b2tlbik7XG4gICAgICAgIGlmIChoYW5kbGVyKSB7XG4gICAgICAgICAgICBoYW5kbGVyKHBhcmFtcy52YWx1ZSk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICB1bmhhbmRsZWRQcm9ncmVzc0VtaXR0ZXIuZmlyZShwYXJhbXMpO1xuICAgICAgICB9XG4gICAgfSk7XG4gICAgcmV0dXJuIGNvbm5lY3Rpb247XG59XG5leHBvcnRzLmNyZWF0ZU1lc3NhZ2VDb25uZWN0aW9uID0gY3JlYXRlTWVzc2FnZUNvbm5lY3Rpb247XG4iLCAiXCJ1c2Ugc3RyaWN0XCI7XG4vKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG4vLy8gPHJlZmVyZW5jZSBwYXRoPVwiLi4vLi4vdHlwaW5ncy90aGVuYWJsZS5kLnRzXCIgLz5cbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIl9fZXNNb2R1bGVcIiwgeyB2YWx1ZTogdHJ1ZSB9KTtcbmV4cG9ydHMuUHJvZ3Jlc3NUeXBlID0gZXhwb3J0cy5Qcm9ncmVzc1Rva2VuID0gZXhwb3J0cy5jcmVhdGVNZXNzYWdlQ29ubmVjdGlvbiA9IGV4cG9ydHMuTnVsbExvZ2dlciA9IGV4cG9ydHMuQ29ubmVjdGlvbk9wdGlvbnMgPSBleHBvcnRzLkNvbm5lY3Rpb25TdHJhdGVneSA9IGV4cG9ydHMuQWJzdHJhY3RNZXNzYWdlQnVmZmVyID0gZXhwb3J0cy5Xcml0ZWFibGVTdHJlYW1NZXNzYWdlV3JpdGVyID0gZXhwb3J0cy5BYnN0cmFjdE1lc3NhZ2VXcml0ZXIgPSBleHBvcnRzLk1lc3NhZ2VXcml0ZXIgPSBleHBvcnRzLlJlYWRhYmxlU3RyZWFtTWVzc2FnZVJlYWRlciA9IGV4cG9ydHMuQWJzdHJhY3RNZXNzYWdlUmVhZGVyID0gZXhwb3J0cy5NZXNzYWdlUmVhZGVyID0gZXhwb3J0cy5TaGFyZWRBcnJheVJlY2VpdmVyU3RyYXRlZ3kgPSBleHBvcnRzLlNoYXJlZEFycmF5U2VuZGVyU3RyYXRlZ3kgPSBleHBvcnRzLkNhbmNlbGxhdGlvblRva2VuID0gZXhwb3J0cy5DYW5jZWxsYXRpb25Ub2tlblNvdXJjZSA9IGV4cG9ydHMuRW1pdHRlciA9IGV4cG9ydHMuRXZlbnQgPSBleHBvcnRzLkRpc3Bvc2FibGUgPSBleHBvcnRzLkxSVUNhY2hlID0gZXhwb3J0cy5Ub3VjaCA9IGV4cG9ydHMuTGlua2VkTWFwID0gZXhwb3J0cy5QYXJhbWV0ZXJTdHJ1Y3R1cmVzID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlOSA9IGV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTggPSBleHBvcnRzLk5vdGlmaWNhdGlvblR5cGU3ID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlNiA9IGV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTUgPSBleHBvcnRzLk5vdGlmaWNhdGlvblR5cGU0ID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlMyA9IGV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTIgPSBleHBvcnRzLk5vdGlmaWNhdGlvblR5cGUxID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlMCA9IGV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZSA9IGV4cG9ydHMuRXJyb3JDb2RlcyA9IGV4cG9ydHMuUmVzcG9uc2VFcnJvciA9IGV4cG9ydHMuUmVxdWVzdFR5cGU5ID0gZXhwb3J0cy5SZXF1ZXN0VHlwZTggPSBleHBvcnRzLlJlcXVlc3RUeXBlNyA9IGV4cG9ydHMuUmVxdWVzdFR5cGU2ID0gZXhwb3J0cy5SZXF1ZXN0VHlwZTUgPSBleHBvcnRzLlJlcXVlc3RUeXBlNCA9IGV4cG9ydHMuUmVxdWVzdFR5cGUzID0gZXhwb3J0cy5SZXF1ZXN0VHlwZTIgPSBleHBvcnRzLlJlcXVlc3RUeXBlMSA9IGV4cG9ydHMuUmVxdWVzdFR5cGUwID0gZXhwb3J0cy5SZXF1ZXN0VHlwZSA9IGV4cG9ydHMuTWVzc2FnZSA9IGV4cG9ydHMuUkFMID0gdm9pZCAwO1xuZXhwb3J0cy5NZXNzYWdlU3RyYXRlZ3kgPSBleHBvcnRzLkNhbmNlbGxhdGlvblN0cmF0ZWd5ID0gZXhwb3J0cy5DYW5jZWxsYXRpb25TZW5kZXJTdHJhdGVneSA9IGV4cG9ydHMuQ2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneSA9IGV4cG9ydHMuQ29ubmVjdGlvbkVycm9yID0gZXhwb3J0cy5Db25uZWN0aW9uRXJyb3JzID0gZXhwb3J0cy5Mb2dUcmFjZU5vdGlmaWNhdGlvbiA9IGV4cG9ydHMuU2V0VHJhY2VOb3RpZmljYXRpb24gPSBleHBvcnRzLlRyYWNlRm9ybWF0ID0gZXhwb3J0cy5UcmFjZVZhbHVlcyA9IGV4cG9ydHMuVHJhY2UgPSB2b2lkIDA7XG5jb25zdCBtZXNzYWdlc18xID0gcmVxdWlyZShcIi4vbWVzc2FnZXNcIik7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJNZXNzYWdlXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLk1lc3NhZ2U7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJSZXF1ZXN0VHlwZVwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5SZXF1ZXN0VHlwZTsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlJlcXVlc3RUeXBlMFwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5SZXF1ZXN0VHlwZTA7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJSZXF1ZXN0VHlwZTFcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VzXzEuUmVxdWVzdFR5cGUxOyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiUmVxdWVzdFR5cGUyXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLlJlcXVlc3RUeXBlMjsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlJlcXVlc3RUeXBlM1wiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5SZXF1ZXN0VHlwZTM7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJSZXF1ZXN0VHlwZTRcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VzXzEuUmVxdWVzdFR5cGU0OyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiUmVxdWVzdFR5cGU1XCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLlJlcXVlc3RUeXBlNTsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlJlcXVlc3RUeXBlNlwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5SZXF1ZXN0VHlwZTY7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJSZXF1ZXN0VHlwZTdcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VzXzEuUmVxdWVzdFR5cGU3OyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiUmVxdWVzdFR5cGU4XCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLlJlcXVlc3RUeXBlODsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlJlcXVlc3RUeXBlOVwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5SZXF1ZXN0VHlwZTk7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJSZXNwb25zZUVycm9yXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLlJlc3BvbnNlRXJyb3I7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJFcnJvckNvZGVzXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLkVycm9yQ29kZXM7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJOb3RpZmljYXRpb25UeXBlXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLk5vdGlmaWNhdGlvblR5cGU7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJOb3RpZmljYXRpb25UeXBlMFwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5Ob3RpZmljYXRpb25UeXBlMDsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIk5vdGlmaWNhdGlvblR5cGUxXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLk5vdGlmaWNhdGlvblR5cGUxOyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiTm90aWZpY2F0aW9uVHlwZTJcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VzXzEuTm90aWZpY2F0aW9uVHlwZTI7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJOb3RpZmljYXRpb25UeXBlM1wiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5Ob3RpZmljYXRpb25UeXBlMzsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIk5vdGlmaWNhdGlvblR5cGU0XCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLk5vdGlmaWNhdGlvblR5cGU0OyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiTm90aWZpY2F0aW9uVHlwZTVcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VzXzEuTm90aWZpY2F0aW9uVHlwZTU7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJOb3RpZmljYXRpb25UeXBlNlwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5Ob3RpZmljYXRpb25UeXBlNjsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIk5vdGlmaWNhdGlvblR5cGU3XCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLk5vdGlmaWNhdGlvblR5cGU3OyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiTm90aWZpY2F0aW9uVHlwZThcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VzXzEuTm90aWZpY2F0aW9uVHlwZTg7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJOb3RpZmljYXRpb25UeXBlOVwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5Ob3RpZmljYXRpb25UeXBlOTsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlBhcmFtZXRlclN0cnVjdHVyZXNcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VzXzEuUGFyYW1ldGVyU3RydWN0dXJlczsgfSB9KTtcbmNvbnN0IGxpbmtlZE1hcF8xID0gcmVxdWlyZShcIi4vbGlua2VkTWFwXCIpO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiTGlua2VkTWFwXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBsaW5rZWRNYXBfMS5MaW5rZWRNYXA7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJMUlVDYWNoZVwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbGlua2VkTWFwXzEuTFJVQ2FjaGU7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJUb3VjaFwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbGlua2VkTWFwXzEuVG91Y2g7IH0gfSk7XG5jb25zdCBkaXNwb3NhYmxlXzEgPSByZXF1aXJlKFwiLi9kaXNwb3NhYmxlXCIpO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiRGlzcG9zYWJsZVwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gZGlzcG9zYWJsZV8xLkRpc3Bvc2FibGU7IH0gfSk7XG5jb25zdCBldmVudHNfMSA9IHJlcXVpcmUoXCIuL2V2ZW50c1wiKTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkV2ZW50XCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBldmVudHNfMS5FdmVudDsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkVtaXR0ZXJcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGV2ZW50c18xLkVtaXR0ZXI7IH0gfSk7XG5jb25zdCBjYW5jZWxsYXRpb25fMSA9IHJlcXVpcmUoXCIuL2NhbmNlbGxhdGlvblwiKTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkNhbmNlbGxhdGlvblRva2VuU291cmNlXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjYW5jZWxsYXRpb25fMS5DYW5jZWxsYXRpb25Ub2tlblNvdXJjZTsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkNhbmNlbGxhdGlvblRva2VuXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjYW5jZWxsYXRpb25fMS5DYW5jZWxsYXRpb25Ub2tlbjsgfSB9KTtcbmNvbnN0IHNoYXJlZEFycmF5Q2FuY2VsbGF0aW9uXzEgPSByZXF1aXJlKFwiLi9zaGFyZWRBcnJheUNhbmNlbGxhdGlvblwiKTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlNoYXJlZEFycmF5U2VuZGVyU3RyYXRlZ3lcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIHNoYXJlZEFycmF5Q2FuY2VsbGF0aW9uXzEuU2hhcmVkQXJyYXlTZW5kZXJTdHJhdGVneTsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlNoYXJlZEFycmF5UmVjZWl2ZXJTdHJhdGVneVwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gc2hhcmVkQXJyYXlDYW5jZWxsYXRpb25fMS5TaGFyZWRBcnJheVJlY2VpdmVyU3RyYXRlZ3k7IH0gfSk7XG5jb25zdCBtZXNzYWdlUmVhZGVyXzEgPSByZXF1aXJlKFwiLi9tZXNzYWdlUmVhZGVyXCIpO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiTWVzc2FnZVJlYWRlclwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZVJlYWRlcl8xLk1lc3NhZ2VSZWFkZXI7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJBYnN0cmFjdE1lc3NhZ2VSZWFkZXJcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VSZWFkZXJfMS5BYnN0cmFjdE1lc3NhZ2VSZWFkZXI7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJSZWFkYWJsZVN0cmVhbU1lc3NhZ2VSZWFkZXJcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VSZWFkZXJfMS5SZWFkYWJsZVN0cmVhbU1lc3NhZ2VSZWFkZXI7IH0gfSk7XG5jb25zdCBtZXNzYWdlV3JpdGVyXzEgPSByZXF1aXJlKFwiLi9tZXNzYWdlV3JpdGVyXCIpO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiTWVzc2FnZVdyaXRlclwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZVdyaXRlcl8xLk1lc3NhZ2VXcml0ZXI7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJBYnN0cmFjdE1lc3NhZ2VXcml0ZXJcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VXcml0ZXJfMS5BYnN0cmFjdE1lc3NhZ2VXcml0ZXI7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJXcml0ZWFibGVTdHJlYW1NZXNzYWdlV3JpdGVyXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlV3JpdGVyXzEuV3JpdGVhYmxlU3RyZWFtTWVzc2FnZVdyaXRlcjsgfSB9KTtcbmNvbnN0IG1lc3NhZ2VCdWZmZXJfMSA9IHJlcXVpcmUoXCIuL21lc3NhZ2VCdWZmZXJcIik7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJBYnN0cmFjdE1lc3NhZ2VCdWZmZXJcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VCdWZmZXJfMS5BYnN0cmFjdE1lc3NhZ2VCdWZmZXI7IH0gfSk7XG5jb25zdCBjb25uZWN0aW9uXzEgPSByZXF1aXJlKFwiLi9jb25uZWN0aW9uXCIpO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiQ29ubmVjdGlvblN0cmF0ZWd5XCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjb25uZWN0aW9uXzEuQ29ubmVjdGlvblN0cmF0ZWd5OyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiQ29ubmVjdGlvbk9wdGlvbnNcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5Db25uZWN0aW9uT3B0aW9uczsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIk51bGxMb2dnZXJcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5OdWxsTG9nZ2VyOyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiY3JlYXRlTWVzc2FnZUNvbm5lY3Rpb25cIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5jcmVhdGVNZXNzYWdlQ29ubmVjdGlvbjsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlByb2dyZXNzVG9rZW5cIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5Qcm9ncmVzc1Rva2VuOyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiUHJvZ3Jlc3NUeXBlXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjb25uZWN0aW9uXzEuUHJvZ3Jlc3NUeXBlOyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiVHJhY2VcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5UcmFjZTsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlRyYWNlVmFsdWVzXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjb25uZWN0aW9uXzEuVHJhY2VWYWx1ZXM7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJUcmFjZUZvcm1hdFwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gY29ubmVjdGlvbl8xLlRyYWNlRm9ybWF0OyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiU2V0VHJhY2VOb3RpZmljYXRpb25cIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5TZXRUcmFjZU5vdGlmaWNhdGlvbjsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkxvZ1RyYWNlTm90aWZpY2F0aW9uXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjb25uZWN0aW9uXzEuTG9nVHJhY2VOb3RpZmljYXRpb247IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJDb25uZWN0aW9uRXJyb3JzXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjb25uZWN0aW9uXzEuQ29ubmVjdGlvbkVycm9yczsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkNvbm5lY3Rpb25FcnJvclwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gY29ubmVjdGlvbl8xLkNvbm5lY3Rpb25FcnJvcjsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkNhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3lcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5DYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5OyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3lcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5DYW5jZWxsYXRpb25TZW5kZXJTdHJhdGVneTsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkNhbmNlbGxhdGlvblN0cmF0ZWd5XCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjb25uZWN0aW9uXzEuQ2FuY2VsbGF0aW9uU3RyYXRlZ3k7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJNZXNzYWdlU3RyYXRlZ3lcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5NZXNzYWdlU3RyYXRlZ3k7IH0gfSk7XG5jb25zdCByYWxfMSA9IHJlcXVpcmUoXCIuL3JhbFwiKTtcbmV4cG9ydHMuUkFMID0gcmFsXzEuZGVmYXVsdDtcbiIsICJcInVzZSBzdHJpY3RcIjtcbi8qIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiBDb3B5cmlnaHQgKGMpIE1pY3Jvc29mdCBDb3Jwb3JhdGlvbi4gQWxsIHJpZ2h0cyByZXNlcnZlZC5cbiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBNSVQgTGljZW5zZS4gU2VlIExpY2Vuc2UudHh0IGluIHRoZSBwcm9qZWN0IHJvb3QgZm9yIGxpY2Vuc2UgaW5mb3JtYXRpb24uXG4gKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0gKi9cbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIl9fZXNNb2R1bGVcIiwgeyB2YWx1ZTogdHJ1ZSB9KTtcbmNvbnN0IGFwaV8xID0gcmVxdWlyZShcIi4uL2NvbW1vbi9hcGlcIik7XG5jbGFzcyBNZXNzYWdlQnVmZmVyIGV4dGVuZHMgYXBpXzEuQWJzdHJhY3RNZXNzYWdlQnVmZmVyIHtcbiAgICBjb25zdHJ1Y3RvcihlbmNvZGluZyA9ICd1dGYtOCcpIHtcbiAgICAgICAgc3VwZXIoZW5jb2RpbmcpO1xuICAgICAgICB0aGlzLmFzY2lpRGVjb2RlciA9IG5ldyBUZXh0RGVjb2RlcignYXNjaWknKTtcbiAgICB9XG4gICAgZW1wdHlCdWZmZXIoKSB7XG4gICAgICAgIHJldHVybiBNZXNzYWdlQnVmZmVyLmVtcHR5QnVmZmVyO1xuICAgIH1cbiAgICBmcm9tU3RyaW5nKHZhbHVlLCBfZW5jb2RpbmcpIHtcbiAgICAgICAgcmV0dXJuIChuZXcgVGV4dEVuY29kZXIoKSkuZW5jb2RlKHZhbHVlKTtcbiAgICB9XG4gICAgdG9TdHJpbmcodmFsdWUsIGVuY29kaW5nKSB7XG4gICAgICAgIGlmIChlbmNvZGluZyA9PT0gJ2FzY2lpJykge1xuICAgICAgICAgICAgcmV0dXJuIHRoaXMuYXNjaWlEZWNvZGVyLmRlY29kZSh2YWx1ZSk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICByZXR1cm4gKG5ldyBUZXh0RGVjb2RlcihlbmNvZGluZykpLmRlY29kZSh2YWx1ZSk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgYXNOYXRpdmUoYnVmZmVyLCBsZW5ndGgpIHtcbiAgICAgICAgaWYgKGxlbmd0aCA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXR1cm4gYnVmZmVyO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgcmV0dXJuIGJ1ZmZlci5zbGljZSgwLCBsZW5ndGgpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGFsbG9jTmF0aXZlKGxlbmd0aCkge1xuICAgICAgICByZXR1cm4gbmV3IFVpbnQ4QXJyYXkobGVuZ3RoKTtcbiAgICB9XG59XG5NZXNzYWdlQnVmZmVyLmVtcHR5QnVmZmVyID0gbmV3IFVpbnQ4QXJyYXkoMCk7XG5jbGFzcyBSZWFkYWJsZVN0cmVhbVdyYXBwZXIge1xuICAgIGNvbnN0cnVjdG9yKHNvY2tldCkge1xuICAgICAgICB0aGlzLnNvY2tldCA9IHNvY2tldDtcbiAgICAgICAgdGhpcy5fb25EYXRhID0gbmV3IGFwaV8xLkVtaXR0ZXIoKTtcbiAgICAgICAgdGhpcy5fbWVzc2FnZUxpc3RlbmVyID0gKGV2ZW50KSA9PiB7XG4gICAgICAgICAgICBjb25zdCBibG9iID0gZXZlbnQuZGF0YTtcbiAgICAgICAgICAgIGJsb2IuYXJyYXlCdWZmZXIoKS50aGVuKChidWZmZXIpID0+IHtcbiAgICAgICAgICAgICAgICB0aGlzLl9vbkRhdGEuZmlyZShuZXcgVWludDhBcnJheShidWZmZXIpKTtcbiAgICAgICAgICAgIH0sICgpID0+IHtcbiAgICAgICAgICAgICAgICAoMCwgYXBpXzEuUkFMKSgpLmNvbnNvbGUuZXJyb3IoYENvbnZlcnRpbmcgYmxvYiB0byBhcnJheSBidWZmZXIgZmFpbGVkLmApO1xuICAgICAgICAgICAgfSk7XG4gICAgICAgIH07XG4gICAgICAgIHRoaXMuc29ja2V0LmFkZEV2ZW50TGlzdGVuZXIoJ21lc3NhZ2UnLCB0aGlzLl9tZXNzYWdlTGlzdGVuZXIpO1xuICAgIH1cbiAgICBvbkNsb3NlKGxpc3RlbmVyKSB7XG4gICAgICAgIHRoaXMuc29ja2V0LmFkZEV2ZW50TGlzdGVuZXIoJ2Nsb3NlJywgbGlzdGVuZXIpO1xuICAgICAgICByZXR1cm4gYXBpXzEuRGlzcG9zYWJsZS5jcmVhdGUoKCkgPT4gdGhpcy5zb2NrZXQucmVtb3ZlRXZlbnRMaXN0ZW5lcignY2xvc2UnLCBsaXN0ZW5lcikpO1xuICAgIH1cbiAgICBvbkVycm9yKGxpc3RlbmVyKSB7XG4gICAgICAgIHRoaXMuc29ja2V0LmFkZEV2ZW50TGlzdGVuZXIoJ2Vycm9yJywgbGlzdGVuZXIpO1xuICAgICAgICByZXR1cm4gYXBpXzEuRGlzcG9zYWJsZS5jcmVhdGUoKCkgPT4gdGhpcy5zb2NrZXQucmVtb3ZlRXZlbnRMaXN0ZW5lcignZXJyb3InLCBsaXN0ZW5lcikpO1xuICAgIH1cbiAgICBvbkVuZChsaXN0ZW5lcikge1xuICAgICAgICB0aGlzLnNvY2tldC5hZGRFdmVudExpc3RlbmVyKCdlbmQnLCBsaXN0ZW5lcik7XG4gICAgICAgIHJldHVybiBhcGlfMS5EaXNwb3NhYmxlLmNyZWF0ZSgoKSA9PiB0aGlzLnNvY2tldC5yZW1vdmVFdmVudExpc3RlbmVyKCdlbmQnLCBsaXN0ZW5lcikpO1xuICAgIH1cbiAgICBvbkRhdGEobGlzdGVuZXIpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX29uRGF0YS5ldmVudChsaXN0ZW5lcik7XG4gICAgfVxufVxuY2xhc3MgV3JpdGFibGVTdHJlYW1XcmFwcGVyIHtcbiAgICBjb25zdHJ1Y3Rvcihzb2NrZXQpIHtcbiAgICAgICAgdGhpcy5zb2NrZXQgPSBzb2NrZXQ7XG4gICAgfVxuICAgIG9uQ2xvc2UobGlzdGVuZXIpIHtcbiAgICAgICAgdGhpcy5zb2NrZXQuYWRkRXZlbnRMaXN0ZW5lcignY2xvc2UnLCBsaXN0ZW5lcik7XG4gICAgICAgIHJldHVybiBhcGlfMS5EaXNwb3NhYmxlLmNyZWF0ZSgoKSA9PiB0aGlzLnNvY2tldC5yZW1vdmVFdmVudExpc3RlbmVyKCdjbG9zZScsIGxpc3RlbmVyKSk7XG4gICAgfVxuICAgIG9uRXJyb3IobGlzdGVuZXIpIHtcbiAgICAgICAgdGhpcy5zb2NrZXQuYWRkRXZlbnRMaXN0ZW5lcignZXJyb3InLCBsaXN0ZW5lcik7XG4gICAgICAgIHJldHVybiBhcGlfMS5EaXNwb3NhYmxlLmNyZWF0ZSgoKSA9PiB0aGlzLnNvY2tldC5yZW1vdmVFdmVudExpc3RlbmVyKCdlcnJvcicsIGxpc3RlbmVyKSk7XG4gICAgfVxuICAgIG9uRW5kKGxpc3RlbmVyKSB7XG4gICAgICAgIHRoaXMuc29ja2V0LmFkZEV2ZW50TGlzdGVuZXIoJ2VuZCcsIGxpc3RlbmVyKTtcbiAgICAgICAgcmV0dXJuIGFwaV8xLkRpc3Bvc2FibGUuY3JlYXRlKCgpID0+IHRoaXMuc29ja2V0LnJlbW92ZUV2ZW50TGlzdGVuZXIoJ2VuZCcsIGxpc3RlbmVyKSk7XG4gICAgfVxuICAgIHdyaXRlKGRhdGEsIGVuY29kaW5nKSB7XG4gICAgICAgIGlmICh0eXBlb2YgZGF0YSA9PT0gJ3N0cmluZycpIHtcbiAgICAgICAgICAgIGlmIChlbmNvZGluZyAhPT0gdW5kZWZpbmVkICYmIGVuY29kaW5nICE9PSAndXRmLTgnKSB7XG4gICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBJbiBhIEJyb3dzZXIgZW52aXJvbm1lbnRzIG9ubHkgdXRmLTggdGV4dCBlbmNvZGluZyBpcyBzdXBwb3J0ZWQuIEJ1dCBnb3QgZW5jb2Rpbmc6ICR7ZW5jb2Rpbmd9YCk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICB0aGlzLnNvY2tldC5zZW5kKGRhdGEpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgdGhpcy5zb2NrZXQuc2VuZChkYXRhKTtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gUHJvbWlzZS5yZXNvbHZlKCk7XG4gICAgfVxuICAgIGVuZCgpIHtcbiAgICAgICAgdGhpcy5zb2NrZXQuY2xvc2UoKTtcbiAgICB9XG59XG5jb25zdCBfdGV4dEVuY29kZXIgPSBuZXcgVGV4dEVuY29kZXIoKTtcbmNvbnN0IF9yaWwgPSBPYmplY3QuZnJlZXplKHtcbiAgICBtZXNzYWdlQnVmZmVyOiBPYmplY3QuZnJlZXplKHtcbiAgICAgICAgY3JlYXRlOiAoZW5jb2RpbmcpID0+IG5ldyBNZXNzYWdlQnVmZmVyKGVuY29kaW5nKVxuICAgIH0pLFxuICAgIGFwcGxpY2F0aW9uSnNvbjogT2JqZWN0LmZyZWV6ZSh7XG4gICAgICAgIGVuY29kZXI6IE9iamVjdC5mcmVlemUoe1xuICAgICAgICAgICAgbmFtZTogJ2FwcGxpY2F0aW9uL2pzb24nLFxuICAgICAgICAgICAgZW5jb2RlOiAobXNnLCBvcHRpb25zKSA9PiB7XG4gICAgICAgICAgICAgICAgaWYgKG9wdGlvbnMuY2hhcnNldCAhPT0gJ3V0Zi04Jykge1xuICAgICAgICAgICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYEluIGEgQnJvd3NlciBlbnZpcm9ubWVudHMgb25seSB1dGYtOCB0ZXh0IGVuY29kaW5nIGlzIHN1cHBvcnRlZC4gQnV0IGdvdCBlbmNvZGluZzogJHtvcHRpb25zLmNoYXJzZXR9YCk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIHJldHVybiBQcm9taXNlLnJlc29sdmUoX3RleHRFbmNvZGVyLmVuY29kZShKU09OLnN0cmluZ2lmeShtc2csIHVuZGVmaW5lZCwgMCkpKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSksXG4gICAgICAgIGRlY29kZXI6IE9iamVjdC5mcmVlemUoe1xuICAgICAgICAgICAgbmFtZTogJ2FwcGxpY2F0aW9uL2pzb24nLFxuICAgICAgICAgICAgZGVjb2RlOiAoYnVmZmVyLCBvcHRpb25zKSA9PiB7XG4gICAgICAgICAgICAgICAgaWYgKCEoYnVmZmVyIGluc3RhbmNlb2YgVWludDhBcnJheSkpIHtcbiAgICAgICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBJbiBhIEJyb3dzZXIgZW52aXJvbm1lbnRzIG9ubHkgVWludDhBcnJheXMgYXJlIHN1cHBvcnRlZC5gKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgcmV0dXJuIFByb21pc2UucmVzb2x2ZShKU09OLnBhcnNlKG5ldyBUZXh0RGVjb2RlcihvcHRpb25zLmNoYXJzZXQpLmRlY29kZShidWZmZXIpKSk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0pXG4gICAgfSksXG4gICAgc3RyZWFtOiBPYmplY3QuZnJlZXplKHtcbiAgICAgICAgYXNSZWFkYWJsZVN0cmVhbTogKHNvY2tldCkgPT4gbmV3IFJlYWRhYmxlU3RyZWFtV3JhcHBlcihzb2NrZXQpLFxuICAgICAgICBhc1dyaXRhYmxlU3RyZWFtOiAoc29ja2V0KSA9PiBuZXcgV3JpdGFibGVTdHJlYW1XcmFwcGVyKHNvY2tldClcbiAgICB9KSxcbiAgICBjb25zb2xlOiBjb25zb2xlLFxuICAgIHRpbWVyOiBPYmplY3QuZnJlZXplKHtcbiAgICAgICAgc2V0VGltZW91dChjYWxsYmFjaywgbXMsIC4uLmFyZ3MpIHtcbiAgICAgICAgICAgIGNvbnN0IGhhbmRsZSA9IHNldFRpbWVvdXQoY2FsbGJhY2ssIG1zLCAuLi5hcmdzKTtcbiAgICAgICAgICAgIHJldHVybiB7IGRpc3Bvc2U6ICgpID0+IGNsZWFyVGltZW91dChoYW5kbGUpIH07XG4gICAgICAgIH0sXG4gICAgICAgIHNldEltbWVkaWF0ZShjYWxsYmFjaywgLi4uYXJncykge1xuICAgICAgICAgICAgY29uc3QgaGFuZGxlID0gc2V0VGltZW91dChjYWxsYmFjaywgMCwgLi4uYXJncyk7XG4gICAgICAgICAgICByZXR1cm4geyBkaXNwb3NlOiAoKSA9PiBjbGVhclRpbWVvdXQoaGFuZGxlKSB9O1xuICAgICAgICB9LFxuICAgICAgICBzZXRJbnRlcnZhbChjYWxsYmFjaywgbXMsIC4uLmFyZ3MpIHtcbiAgICAgICAgICAgIGNvbnN0IGhhbmRsZSA9IHNldEludGVydmFsKGNhbGxiYWNrLCBtcywgLi4uYXJncyk7XG4gICAgICAgICAgICByZXR1cm4geyBkaXNwb3NlOiAoKSA9PiBjbGVhckludGVydmFsKGhhbmRsZSkgfTtcbiAgICAgICAgfSxcbiAgICB9KVxufSk7XG5mdW5jdGlvbiBSSUwoKSB7XG4gICAgcmV0dXJuIF9yaWw7XG59XG4oZnVuY3Rpb24gKFJJTCkge1xuICAgIGZ1bmN0aW9uIGluc3RhbGwoKSB7XG4gICAgICAgIGFwaV8xLlJBTC5pbnN0YWxsKF9yaWwpO1xuICAgIH1cbiAgICBSSUwuaW5zdGFsbCA9IGluc3RhbGw7XG59KShSSUwgfHwgKFJJTCA9IHt9KSk7XG5leHBvcnRzLmRlZmF1bHQgPSBSSUw7XG4iLCAiXCJ1c2Ugc3RyaWN0XCI7XG4vKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG52YXIgX19jcmVhdGVCaW5kaW5nID0gKHRoaXMgJiYgdGhpcy5fX2NyZWF0ZUJpbmRpbmcpIHx8IChPYmplY3QuY3JlYXRlID8gKGZ1bmN0aW9uKG8sIG0sIGssIGsyKSB7XG4gICAgaWYgKGsyID09PSB1bmRlZmluZWQpIGsyID0gaztcbiAgICB2YXIgZGVzYyA9IE9iamVjdC5nZXRPd25Qcm9wZXJ0eURlc2NyaXB0b3IobSwgayk7XG4gICAgaWYgKCFkZXNjIHx8IChcImdldFwiIGluIGRlc2MgPyAhbS5fX2VzTW9kdWxlIDogZGVzYy53cml0YWJsZSB8fCBkZXNjLmNvbmZpZ3VyYWJsZSkpIHtcbiAgICAgIGRlc2MgPSB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24oKSB7IHJldHVybiBtW2tdOyB9IH07XG4gICAgfVxuICAgIE9iamVjdC5kZWZpbmVQcm9wZXJ0eShvLCBrMiwgZGVzYyk7XG59KSA6IChmdW5jdGlvbihvLCBtLCBrLCBrMikge1xuICAgIGlmIChrMiA9PT0gdW5kZWZpbmVkKSBrMiA9IGs7XG4gICAgb1trMl0gPSBtW2tdO1xufSkpO1xudmFyIF9fZXhwb3J0U3RhciA9ICh0aGlzICYmIHRoaXMuX19leHBvcnRTdGFyKSB8fCBmdW5jdGlvbihtLCBleHBvcnRzKSB7XG4gICAgZm9yICh2YXIgcCBpbiBtKSBpZiAocCAhPT0gXCJkZWZhdWx0XCIgJiYgIU9iamVjdC5wcm90b3R5cGUuaGFzT3duUHJvcGVydHkuY2FsbChleHBvcnRzLCBwKSkgX19jcmVhdGVCaW5kaW5nKGV4cG9ydHMsIG0sIHApO1xufTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIl9fZXNNb2R1bGVcIiwgeyB2YWx1ZTogdHJ1ZSB9KTtcbmV4cG9ydHMuY3JlYXRlTWVzc2FnZUNvbm5lY3Rpb24gPSBleHBvcnRzLkJyb3dzZXJNZXNzYWdlV3JpdGVyID0gZXhwb3J0cy5Ccm93c2VyTWVzc2FnZVJlYWRlciA9IHZvaWQgMDtcbmNvbnN0IHJpbF8xID0gcmVxdWlyZShcIi4vcmlsXCIpO1xuLy8gSW5zdGFsbCB0aGUgYnJvd3NlciBydW50aW1lIGFic3RyYWN0LlxucmlsXzEuZGVmYXVsdC5pbnN0YWxsKCk7XG5jb25zdCBhcGlfMSA9IHJlcXVpcmUoXCIuLi9jb21tb24vYXBpXCIpO1xuX19leHBvcnRTdGFyKHJlcXVpcmUoXCIuLi9jb21tb24vYXBpXCIpLCBleHBvcnRzKTtcbmNsYXNzIEJyb3dzZXJNZXNzYWdlUmVhZGVyIGV4dGVuZHMgYXBpXzEuQWJzdHJhY3RNZXNzYWdlUmVhZGVyIHtcbiAgICBjb25zdHJ1Y3Rvcihwb3J0KSB7XG4gICAgICAgIHN1cGVyKCk7XG4gICAgICAgIHRoaXMuX29uRGF0YSA9IG5ldyBhcGlfMS5FbWl0dGVyKCk7XG4gICAgICAgIHRoaXMuX21lc3NhZ2VMaXN0ZW5lciA9IChldmVudCkgPT4ge1xuICAgICAgICAgICAgdGhpcy5fb25EYXRhLmZpcmUoZXZlbnQuZGF0YSk7XG4gICAgICAgIH07XG4gICAgICAgIHBvcnQuYWRkRXZlbnRMaXN0ZW5lcignZXJyb3InLCAoZXZlbnQpID0+IHRoaXMuZmlyZUVycm9yKGV2ZW50KSk7XG4gICAgICAgIHBvcnQub25tZXNzYWdlID0gdGhpcy5fbWVzc2FnZUxpc3RlbmVyO1xuICAgIH1cbiAgICBsaXN0ZW4oY2FsbGJhY2spIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX29uRGF0YS5ldmVudChjYWxsYmFjayk7XG4gICAgfVxufVxuZXhwb3J0cy5Ccm93c2VyTWVzc2FnZVJlYWRlciA9IEJyb3dzZXJNZXNzYWdlUmVhZGVyO1xuY2xhc3MgQnJvd3Nlck1lc3NhZ2VXcml0ZXIgZXh0ZW5kcyBhcGlfMS5BYnN0cmFjdE1lc3NhZ2VXcml0ZXIge1xuICAgIGNvbnN0cnVjdG9yKHBvcnQpIHtcbiAgICAgICAgc3VwZXIoKTtcbiAgICAgICAgdGhpcy5wb3J0ID0gcG9ydDtcbiAgICAgICAgdGhpcy5lcnJvckNvdW50ID0gMDtcbiAgICAgICAgcG9ydC5hZGRFdmVudExpc3RlbmVyKCdlcnJvcicsIChldmVudCkgPT4gdGhpcy5maXJlRXJyb3IoZXZlbnQpKTtcbiAgICB9XG4gICAgd3JpdGUobXNnKSB7XG4gICAgICAgIHRyeSB7XG4gICAgICAgICAgICB0aGlzLnBvcnQucG9zdE1lc3NhZ2UobXNnKTtcbiAgICAgICAgICAgIHJldHVybiBQcm9taXNlLnJlc29sdmUoKTtcbiAgICAgICAgfVxuICAgICAgICBjYXRjaCAoZXJyb3IpIHtcbiAgICAgICAgICAgIHRoaXMuaGFuZGxlRXJyb3IoZXJyb3IsIG1zZyk7XG4gICAgICAgICAgICByZXR1cm4gUHJvbWlzZS5yZWplY3QoZXJyb3IpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGhhbmRsZUVycm9yKGVycm9yLCBtc2cpIHtcbiAgICAgICAgdGhpcy5lcnJvckNvdW50Kys7XG4gICAgICAgIHRoaXMuZmlyZUVycm9yKGVycm9yLCBtc2csIHRoaXMuZXJyb3JDb3VudCk7XG4gICAgfVxuICAgIGVuZCgpIHtcbiAgICB9XG59XG5leHBvcnRzLkJyb3dzZXJNZXNzYWdlV3JpdGVyID0gQnJvd3Nlck1lc3NhZ2VXcml0ZXI7XG5mdW5jdGlvbiBjcmVhdGVNZXNzYWdlQ29ubmVjdGlvbihyZWFkZXIsIHdyaXRlciwgbG9nZ2VyLCBvcHRpb25zKSB7XG4gICAgaWYgKGxvZ2dlciA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgIGxvZ2dlciA9IGFwaV8xLk51bGxMb2dnZXI7XG4gICAgfVxuICAgIGlmIChhcGlfMS5Db25uZWN0aW9uU3RyYXRlZ3kuaXMob3B0aW9ucykpIHtcbiAgICAgICAgb3B0aW9ucyA9IHsgY29ubmVjdGlvblN0cmF0ZWd5OiBvcHRpb25zIH07XG4gICAgfVxuICAgIHJldHVybiAoMCwgYXBpXzEuY3JlYXRlTWVzc2FnZUNvbm5lY3Rpb24pKHJlYWRlciwgd3JpdGVyLCBsb2dnZXIsIG9wdGlvbnMpO1xufVxuZXhwb3J0cy5jcmVhdGVNZXNzYWdlQ29ubmVjdGlvbiA9IGNyZWF0ZU1lc3NhZ2VDb25uZWN0aW9uO1xuIiwgIi8qIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiBDb3B5cmlnaHQgKGMpIE1pY3Jvc29mdCBDb3Jwb3JhdGlvbi4gQWxsIHJpZ2h0cyByZXNlcnZlZC5cbiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBNSVQgTGljZW5zZS4gU2VlIExpY2Vuc2UudHh0IGluIHRoZSBwcm9qZWN0IHJvb3QgZm9yIGxpY2Vuc2UgaW5mb3JtYXRpb24uXG4gKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSAqL1xuJ3VzZSBzdHJpY3QnO1xuXG5tb2R1bGUuZXhwb3J0cyA9IHJlcXVpcmUoJy4vbGliL2Jyb3dzZXIvbWFpbicpOyIsICIvKlxuICogQ29weXJpZ2h0IChjKSAyMDEwLTIwMjYgRWNsaXBzZSBEaXJpZ2libGUgY29udHJpYnV0b3JzXG4gKlxuICogQWxsIHJpZ2h0cyByZXNlcnZlZC4gVGhpcyBwcm9ncmFtIGFuZCB0aGUgYWNjb21wYW55aW5nIG1hdGVyaWFscyBhcmUgbWFkZSBhdmFpbGFibGUgdW5kZXIgdGhlXG4gKiB0ZXJtcyBvZiB0aGUgRWNsaXBzZSBQdWJsaWMgTGljZW5zZSB2Mi4wIHdoaWNoIGFjY29tcGFuaWVzIHRoaXMgZGlzdHJpYnV0aW9uLCBhbmQgaXMgYXZhaWxhYmxlIGF0XG4gKiBodHRwOi8vd3d3LmVjbGlwc2Uub3JnL2xlZ2FsL2VwbC12MjAuaHRtbFxuICpcbiAqIFNQRFgtRmlsZUNvcHlyaWdodFRleHQ6IEVjbGlwc2UgRGlyaWdpYmxlIGNvbnRyaWJ1dG9ycyBTUERYLUxpY2Vuc2UtSWRlbnRpZmllcjogRVBMLTIuMFxuICovXG5cbi8qKlxuICogSmF2YSBMU1AgY2xpZW50IGJ1bmRsZS5cbiAqXG4gKiBVc2VzIHZzY29kZS13cy1qc29ucnBjIGZvciB0eXBlZCBKU09OLVJQQyBvdmVyIFdlYlNvY2tldCBhbmQgcmVnaXN0ZXJzIE1vbmFjb1xuICogcHJvdmlkZXJzIHRoYXQgZGVsZWdhdGUgdG8gSkRULkxTLiBFeHBvc2VkIGFzIGdsb2JhbCBKYXZhTHNwQ2xpZW50TGliIHNvIHRoYXRcbiAqIGVkaXRvci5qcyBjYW4gY2FsbCBKYXZhTHNwQ2xpZW50TGliLmNvbm5lY3QocmVzb3VyY2VQYXRoKS5cbiAqXG4gKiBPbmUgSkRULkxTIHByb2Nlc3MgY292ZXJzIHRoZSBlbnRpcmUgd29ya3NwYWNlLCBzbyBhIHNpbmdsZSBXZWJTb2NrZXQgY29ubmVjdGlvblxuICogaXMgc2hhcmVkIGFjcm9zcyBhbGwgSmF2YSBmaWxlcyBvcGVuIGluIHRoZSBzYW1lIGJyb3dzZXIgcGFnZS4gVGhlIGNvbm5lY3Rpb24gaXNcbiAqIGVzdGFibGlzaGVkIG9uIHRoZSBmaXJzdCBjb25uZWN0KCkgY2FsbCBhbmQgcmV1c2VkIGZvciBhbGwgc3Vic2VxdWVudCBjYWxscy5cbiAqXG4gKiBlZGl0b3IuanMgc2V0cyB3aW5kb3cubW9uYWNvIGJlZm9yZSBjYWxsaW5nIGNvbm5lY3QoKSwgc28gdGhlIG1vbmFjby1zaGltJ3MgbGF6eVxuICogUHJveGllcyByZXNvbHZlIGNvcnJlY3RseSBhdCBjYWxsIHRpbWUuXG4gKi9cblxuaW1wb3J0IHsgY3JlYXRlTWVzc2FnZUNvbm5lY3Rpb24sIE1lc3NhZ2VDb25uZWN0aW9uIH0gZnJvbSAndnNjb2RlLWpzb25ycGMvYnJvd3Nlcic7XG5pbXBvcnQgeyB0b1NvY2tldCwgV2ViU29ja2V0TWVzc2FnZVJlYWRlciwgV2ViU29ja2V0TWVzc2FnZVdyaXRlciB9IGZyb20gJ3ZzY29kZS13cy1qc29ucnBjJztcbmltcG9ydCAqIGFzIG1vbmFjbyBmcm9tICdtb25hY28tZWRpdG9yJztcbmltcG9ydCB7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLFxuICAgIERpYWdub3N0aWNTZXZlcml0eSxcbiAgICBJbnNlcnRUZXh0Rm9ybWF0LFxuICAgIE1hcmt1cENvbnRlbnQsXG4gICAgTWFya3VwS2luZCxcbiAgICB0eXBlIENvZGVBY3Rpb24sXG4gICAgdHlwZSBDb21tYW5kLFxuICAgIHR5cGUgQ29tcGxldGlvbkl0ZW0sXG4gICAgdHlwZSBDb21wbGV0aW9uTGlzdCxcbiAgICB0eXBlIERpYWdub3N0aWMsXG4gICAgdHlwZSBIb3ZlcixcbiAgICB0eXBlIExvY2F0aW9uLFxuICAgIHR5cGUgUGFyYW1ldGVySW5mb3JtYXRpb24sXG4gICAgdHlwZSBTaWduYXR1cmVIZWxwLFxuICAgIHR5cGUgU2lnbmF0dXJlSW5mb3JtYXRpb24sXG4gICAgdHlwZSBUZXh0RWRpdCxcbiAgICB0eXBlIFdvcmtzcGFjZUVkaXQsXG59IGZyb20gJ3ZzY29kZS1sYW5ndWFnZXNlcnZlci10eXBlcyc7XG5cbi8vIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbi8vIFNpbmdsZXRvbiBzdGF0ZVxuLy8gLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuXG4vKiogU2hhcmVkIGNvbm5lY3Rpb24gXHUyMDE0IG9uZSBwZXIgYnJvd3NlciBwYWdlLCBjb3ZlcmluZyBhbGwgcHJvamVjdHMgaW4gdGhlIHdvcmtzcGFjZS4gKi9cbmxldCBfY29ubjogTWVzc2FnZUNvbm5lY3Rpb24gfCBudWxsID0gbnVsbDtcblxuLyoqXG4gKiBWaXJ0dWFsIHdvcmtzcGFjZSByb290IFVSSSwgZS5nLiB7QGNvZGUgZmlsZTovLy93b3Jrc3BhY2Uvd29ya3NwYWNlL30uXG4gKiBTZXQgb25jZSBvbiBmaXJzdCBjb25uZWN0KCk7IHVzZWQgdG8gc2NvcGUgTW9uYWNvIHByb3ZpZGVycyB0byB3b3Jrc3BhY2UgZmlsZXMuXG4gKi9cbmxldCBfd29ya3NwYWNlUm9vdCA9ICcnO1xuXG4vKiogVVJJcyBmb3Igd2hpY2ggdGV4dERvY3VtZW50L2RpZE9wZW4gaGFzIGFscmVhZHkgYmVlbiBzZW50LiAqL1xuY29uc3QgX29wZW5GaWxlczogU2V0PHN0cmluZz4gPSBuZXcgU2V0KCk7XG5cbi8qKiBQZW5kaW5nIGRlYm91bmNlZCBkaWRDaGFuZ2UgdGltZXJzIHBlciBmaWxlIFVSSSwgc28gYSBjaGFuZ2UgY2FuIGJlIGZsdXNoZWQgYmVmb3JlIGNvbXBsZXRpb24uICovXG5jb25zdCBfY2hhbmdlVGltZXJzOiBNYXA8c3RyaW5nLCBSZXR1cm5UeXBlPHR5cGVvZiBzZXRUaW1lb3V0Pj4gPSBuZXcgTWFwKCk7XG5cbi8qKlxuICogTGFzdCBkaWFnbm9zdGljcyBwdWJsaXNoZWQgcGVyIGZpbGUgVVJJLCBrZXB0IHZlcmJhdGltICh3aXRoIHRoZWlyIExTUCBjb2RlL2RhdGEpLiBDb2RlLWFjdGlvblxuICogcmVxdWVzdHMgbXVzdCBzZW5kIHRoZXNlIFx1MjAxNCBKRFQuTFMgbWF0Y2hlcyBxdWljay1maXhlcyBieSB0aGUgZGlhZ25vc3RpYydzIGNvZGUvZGF0YSwgd2hpY2ggTW9uYWNvJ3NcbiAqIElNYXJrZXJEYXRhIGNhbm5vdCByb3VuZC10cmlwLCBzbyByZWNvbnN0cnVjdGluZyBkaWFnbm9zdGljcyBmcm9tIG1hcmtlcnMgeWllbGRzIG5vIHF1aWNrLWZpeGVzLlxuICovXG5jb25zdCBfZGlhZ25vc3RpY3M6IE1hcDxzdHJpbmcsIERpYWdub3N0aWNbXT4gPSBuZXcgTWFwKCk7XG5cbmxldCBfcHJvdmlkZXJzUmVnaXN0ZXJlZCA9IGZhbHNlO1xuXG4vKiogU2VtYW50aWMtdG9rZW4gbGVnZW5kIHJlcG9ydGVkIGJ5IEpEVC5MUyBpbiB0aGUgaW5pdGlhbGl6ZSByZXN1bHQ7IG5lZWRlZCB0byBkZWNvZGUgdG9rZW4gZGF0YS4gKi9cbmxldCBfc2VtYW50aWNUb2tlbnNMZWdlbmQ6IHsgdG9rZW5UeXBlczogc3RyaW5nW107IHRva2VuTW9kaWZpZXJzOiBzdHJpbmdbXSB9IHwgbnVsbCA9IG51bGw7XG5cbi8vIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbi8vIFB1YmxpYyBBUElcbi8vIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cblxuLyoqIENhbGxlZCBieSBlZGl0b3IuanMgd2hlbiBhIEphdmEgZmlsZSBpcyBvcGVuZWQuIFNhZmUgdG8gY2FsbCBtdWx0aXBsZSB0aW1lcy4gKi9cbmV4cG9ydCBhc3luYyBmdW5jdGlvbiBjb25uZWN0KHJlc291cmNlUGF0aDogc3RyaW5nKTogUHJvbWlzZTx2b2lkPiB7XG4gICAgY29uc3QgcGFydHMgPSByZXNvdXJjZVBhdGgucmVwbGFjZSgvXlxcLy8sICcnKS5zcGxpdCgnLycpO1xuICAgIGNvbnN0IHdvcmtzcGFjZSA9IHBhcnRzWzBdO1xuICAgIGNvbnN0IHByb2plY3QgICA9IHBhcnRzWzFdO1xuICAgIGNvbnN0IGZpbGVVcmkgPSBgZmlsZTovLy93b3Jrc3BhY2UvJHt3b3Jrc3BhY2V9LyR7cHJvamVjdH0vJHtwYXJ0cy5zbGljZSgyKS5qb2luKCcvJyl9YDtcblxuICAgIGlmIChfY29ubikge1xuICAgICAgICAvLyBDb25uZWN0aW9uIGFscmVhZHkgZXN0YWJsaXNoZWQgXHUyMDE0IGp1c3Qgb3BlbiB0aGUgbmV3IGZpbGUuXG4gICAgICAgIG9wZW5GaWxlKGZpbGVVcmkpO1xuICAgICAgICByZXR1cm47XG4gICAgfVxuXG4gICAgX3dvcmtzcGFjZVJvb3QgPSBgZmlsZTovLy93b3Jrc3BhY2UvJHt3b3Jrc3BhY2V9L2A7XG5cbiAgICBjb25zdCBwcm90byA9IGxvY2F0aW9uLnByb3RvY29sID09PSAnaHR0cHM6JyA/ICd3c3MnIDogJ3dzJztcbiAgICBjb25zdCB3c1VybCA9IGAke3Byb3RvfTovLyR7bG9jYXRpb24uaG9zdH0vd2Vic29ja2V0cy9pZGUvamF2YS1sc3BgXG4gICAgICAgICAgICAgICAgKyBgP3dvcmtzcGFjZT0ke2VuY29kZVVSSUNvbXBvbmVudCh3b3Jrc3BhY2UpfWA7XG5cbiAgICBjb25zdCB3cyA9IG5ldyBXZWJTb2NrZXQod3NVcmwpO1xuICAgIGF3YWl0IG5ldyBQcm9taXNlPHZvaWQ+KChyZXNvbHZlLCByZWplY3QpID0+IHtcbiAgICAgICAgd3Mub25vcGVuICA9ICgpID0+IHJlc29sdmUoKTtcbiAgICAgICAgd3Mub25lcnJvciA9ICgpID0+IHJlamVjdChuZXcgRXJyb3IoYFtqYXZhLWxzcF0gV2ViU29ja2V0IGNvbm5lY3QgZmFpbGVkOiAke3dzVXJsfWApKTtcbiAgICB9KTtcblxuICAgIGNvbnN0IHNvY2tldCA9IHRvU29ja2V0KHdzKTtcbiAgICBjb25zdCByZWFkZXIgPSBuZXcgV2ViU29ja2V0TWVzc2FnZVJlYWRlcihzb2NrZXQpO1xuICAgIGNvbnN0IHdyaXRlciA9IG5ldyBXZWJTb2NrZXRNZXNzYWdlV3JpdGVyKHNvY2tldCk7XG4gICAgX2Nvbm4gPSBjcmVhdGVNZXNzYWdlQ29ubmVjdGlvbihyZWFkZXIsIHdyaXRlcik7XG5cbiAgICAvLyBEaWFnbm9zdGljcyBub3RpZmljYXRpb24gXHUyMTkyIE1vbmFjbyBtYXJrZXJzIChhcHBsaWVzIHRvIGFueSB3b3Jrc3BhY2UgZmlsZSlcbiAgICBfY29ubi5vbk5vdGlmaWNhdGlvbigndGV4dERvY3VtZW50L3B1Ymxpc2hEaWFnbm9zdGljcycsIChwYXJhbXM6IHsgdXJpOiBzdHJpbmc7IGRpYWdub3N0aWNzOiBEaWFnbm9zdGljW10gfSkgPT4ge1xuICAgICAgICBfZGlhZ25vc3RpY3Muc2V0KHBhcmFtcy51cmksIHBhcmFtcy5kaWFnbm9zdGljcyA/PyBbXSk7XG4gICAgICAgIGNvbnN0IG1vZGVsID0gbW9uYWNvLmVkaXRvci5nZXRNb2RlbHMoKS5maW5kKG0gPT4gbS51cmkudG9TdHJpbmcoKSA9PT0gcGFyYW1zLnVyaSk7XG4gICAgICAgIGlmICghbW9kZWwpIHJldHVybjtcbiAgICAgICAgbW9uYWNvLmVkaXRvci5zZXRNb2RlbE1hcmtlcnMobW9kZWwsICdqYXZhLWxzcCcsIHBhcmFtcy5kaWFnbm9zdGljcy5tYXAoZCA9PiAoe1xuICAgICAgICAgICAgc2V2ZXJpdHk6ICAgICAgICBsc3BTZXZlcml0eShkLnNldmVyaXR5KSxcbiAgICAgICAgICAgIG1lc3NhZ2U6ICAgICAgICAgZC5tZXNzYWdlLFxuICAgICAgICAgICAgc291cmNlOiAgICAgICAgICBkLnNvdXJjZSA/PyAnamF2YScsXG4gICAgICAgICAgICBzdGFydExpbmVOdW1iZXI6IGQucmFuZ2Uuc3RhcnQubGluZSArIDEsXG4gICAgICAgICAgICBzdGFydENvbHVtbjogICAgIGQucmFuZ2Uuc3RhcnQuY2hhcmFjdGVyICsgMSxcbiAgICAgICAgICAgIGVuZExpbmVOdW1iZXI6ICAgZC5yYW5nZS5lbmQubGluZSArIDEsXG4gICAgICAgICAgICBlbmRDb2x1bW46ICAgICAgIGQucmFuZ2UuZW5kLmNoYXJhY3RlciArIDEsXG4gICAgICAgIH0pKSk7XG4gICAgfSk7XG5cbiAgICAvLyBTZXJ2ZXIgLT4gY2xpZW50IHJlcXVlc3RzIHRoYXQgZHJpdmUgcmVmYWN0b3IgLyBnZW5lcmF0ZSByZXN1bHRzIGFuZCBkeW5hbWljIHJlZ2lzdHJhdGlvbi5cbiAgICBfY29ubi5vblJlcXVlc3QoJ3dvcmtzcGFjZS9hcHBseUVkaXQnLCAocGFyYW1zOiB7IGVkaXQ6IFdvcmtzcGFjZUVkaXQgfSkgPT4ge1xuICAgICAgICBhcHBseVdvcmtzcGFjZUVkaXQocGFyYW1zLmVkaXQpO1xuICAgICAgICByZXR1cm4geyBhcHBsaWVkOiB0cnVlIH07XG4gICAgfSk7XG4gICAgX2Nvbm4ub25SZXF1ZXN0KCd3b3Jrc3BhY2UvY29uZmlndXJhdGlvbicsIChwYXJhbXM6IHsgaXRlbXM6IEFycmF5PHsgc2VjdGlvbj86IHN0cmluZyB9PiB9KSA9PlxuICAgICAgICAocGFyYW1zLml0ZW1zID8/IFtdKS5tYXAoKCkgPT4gamR0bHNTZXR0aW5ncygpLmphdmEpKTtcbiAgICBfY29ubi5vblJlcXVlc3QoJ2NsaWVudC9yZWdpc3RlckNhcGFiaWxpdHknLCAoKSA9PiBudWxsKTtcbiAgICBfY29ubi5vblJlcXVlc3QoJ2NsaWVudC91bnJlZ2lzdGVyQ2FwYWJpbGl0eScsICgpID0+IG51bGwpO1xuICAgIF9jb25uLm9uUmVxdWVzdCgnd2luZG93L3Nob3dNZXNzYWdlUmVxdWVzdCcsICgpID0+IG51bGwpO1xuICAgIF9jb25uLm9uUmVxdWVzdCgnd2luZG93L3dvcmtEb25lUHJvZ3Jlc3MvY3JlYXRlJywgKCkgPT4gbnVsbCk7XG4gICAgX2Nvbm4ub25Ob3RpZmljYXRpb24oJ3dpbmRvdy9sb2dNZXNzYWdlJywgKHA6IHsgbWVzc2FnZTogc3RyaW5nIH0pID0+IGNvbnNvbGUuZGVidWcoJ1tqYXZhLWxzcF0nLCBwPy5tZXNzYWdlKSk7XG4gICAgX2Nvbm4ub25Ob3RpZmljYXRpb24oJ3dpbmRvdy9zaG93TWVzc2FnZScsIChwOiB7IG1lc3NhZ2U6IHN0cmluZyB9KSA9PiBjb25zb2xlLmluZm8oJ1tqYXZhLWxzcF0nLCBwPy5tZXNzYWdlKSk7XG4gICAgLy8gSkRULkxTIGxhbmd1YWdlLXN0YXR1cyAvIHByb2dyZXNzIG5vdGlmaWNhdGlvbnMgXHUyMDE0IGFja25vd2xlZGdlZCBzaWxlbnRseS5cbiAgICBfY29ubi5vbk5vdGlmaWNhdGlvbignbGFuZ3VhZ2Uvc3RhdHVzJywgKCkgPT4geyAvKiBpbmRleGluZy9yZWFkeSBzdGF0dXMsIGlnbm9yZWQgKi8gfSk7XG4gICAgX2Nvbm4ub25Ob3RpZmljYXRpb24oJ2xhbmd1YWdlL3Byb2dyZXNzUmVwb3J0JywgKCkgPT4geyAvKiBidWlsZCBwcm9ncmVzcywgaWdub3JlZCAqLyB9KTtcblxuICAgIF9jb25uLmxpc3RlbigpO1xuXG4gICAgY29uc3Qgcm9vdFVyaSA9IF93b3Jrc3BhY2VSb290O1xuICAgIGNvbnN0IGluaXRSZXN1bHQ6IGFueSA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCdpbml0aWFsaXplJywge1xuICAgICAgICBwcm9jZXNzSWQ6IG51bGwsXG4gICAgICAgIHJvb3RVcmksXG4gICAgICAgIGluaXRpYWxpemF0aW9uT3B0aW9uczoge1xuICAgICAgICAgICAgc2V0dGluZ3M6IGpkdGxzU2V0dGluZ3MoKSxcbiAgICAgICAgICAgIGV4dGVuZGVkQ2xpZW50Q2FwYWJpbGl0aWVzOiB7XG4gICAgICAgICAgICAgICAgcHJvZ3Jlc3NSZXBvcnRQcm92aWRlcjogICAgICAgICAgICBmYWxzZSxcbiAgICAgICAgICAgICAgICBjbGFzc0ZpbGVDb250ZW50c1N1cHBvcnQ6ICAgICAgICAgIHRydWUsXG4gICAgICAgICAgICAgICAgcmVzb2x2ZUFkZGl0aW9uYWxUZXh0RWRpdHNTdXBwb3J0OiB0cnVlLFxuICAgICAgICAgICAgICAgIC8vIERvIE5PVCBhZHZlcnRpc2UgdGhlICpQcm9tcHRTdXBwb3J0IGZsYWdzOiB0aG9zZSBtYWtlIEpEVC5MUyByZXR1cm4gc291cmNlIGFjdGlvbnNcbiAgICAgICAgICAgICAgICAvLyAoZ2VuZXJhdGUgdG9TdHJpbmcvY29uc3RydWN0b3JzL2FjY2Vzc29ycywgb3ZlcnJpZGUvaW1wbGVtZW50LCBvcmdhbml6ZSBpbXBvcnRzKSBhc1xuICAgICAgICAgICAgICAgIC8vIGNsaWVudC1zaWRlIFwiKlByb21wdFwiIGNvbW1hbmRzIHRoZSB2c2NvZGUtamF2YSBleHRlbnNpb24gaW1wbGVtZW50cyBidXQgd2UgZG9uJ3QuXG4gICAgICAgICAgICAgICAgLy8gV2l0aCB0aGVtIG9mZiwgSkRULkxTIHJldHVybnMgdGhlIHNhbWUgYWN0aW9ucyBhcyByZXNvbHZhYmxlIFdvcmtzcGFjZUVkaXRzIG9wZXJhdGluZ1xuICAgICAgICAgICAgICAgIC8vIG9uIGFsbCBtZW1iZXJzLCB3aGljaCBhcHBseUNvZGVBY3Rpb24gcmVzb2x2ZXMgYW5kIGFwcGxpZXMgZGlyZWN0bHkuXG4gICAgICAgICAgICAgICAgaW5mZXJTZWxlY3Rpb25TdXBwb3J0OiAgICAgICAgICAgICBbJ2V4dHJhY3RNZXRob2QnLCAnZXh0cmFjdFZhcmlhYmxlJywgJ2V4dHJhY3RGaWVsZCddLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgfSxcbiAgICAgICAgd29ya3NwYWNlRm9sZGVyczogW3sgdXJpOiByb290VXJpLCBuYW1lOiB3b3Jrc3BhY2UgfV0sXG4gICAgICAgIGNhcGFiaWxpdGllczoge1xuICAgICAgICAgICAgdGV4dERvY3VtZW50OiB7XG4gICAgICAgICAgICAgICAgc3luY2hyb25pemF0aW9uOiB7IGR5bmFtaWNSZWdpc3RyYXRpb246IHRydWUsIHdpbGxTYXZlOiBmYWxzZSwgZGlkU2F2ZTogdHJ1ZSwgd2lsbFNhdmVXYWl0VW50aWw6IGZhbHNlIH0sXG4gICAgICAgICAgICAgICAgY29tcGxldGlvbjoge1xuICAgICAgICAgICAgICAgICAgICBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlLFxuICAgICAgICAgICAgICAgICAgICBjb21wbGV0aW9uSXRlbToge1xuICAgICAgICAgICAgICAgICAgICAgICAgc25pcHBldFN1cHBvcnQ6ICAgICAgICB0cnVlLFxuICAgICAgICAgICAgICAgICAgICAgICAgZG9jdW1lbnRhdGlvbkZvcm1hdDogICBbJ21hcmtkb3duJywgJ3BsYWludGV4dCddLFxuICAgICAgICAgICAgICAgICAgICAgICAgZGVwcmVjYXRlZFN1cHBvcnQ6ICAgICB0cnVlLFxuICAgICAgICAgICAgICAgICAgICAgICAgY29tbWl0Q2hhcmFjdGVyc1N1cHBvcnQ6IHRydWUsXG4gICAgICAgICAgICAgICAgICAgICAgICByZXNvbHZlU3VwcG9ydDogICAgICAgIHsgcHJvcGVydGllczogWydkb2N1bWVudGF0aW9uJywgJ2RldGFpbCcsICdhZGRpdGlvbmFsVGV4dEVkaXRzJ10gfSxcbiAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgY29udGV4dFN1cHBvcnQ6IHRydWUsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICBob3ZlcjogICAgICAgICAgeyBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlLCBjb250ZW50Rm9ybWF0OiBbJ21hcmtkb3duJywgJ3BsYWludGV4dCddIH0sXG4gICAgICAgICAgICAgICAgc2lnbmF0dXJlSGVscDogIHsgZHluYW1pY1JlZ2lzdHJhdGlvbjogdHJ1ZSwgc2lnbmF0dXJlSW5mb3JtYXRpb246IHsgZG9jdW1lbnRhdGlvbkZvcm1hdDogWydtYXJrZG93bicsICdwbGFpbnRleHQnXSwgcGFyYW1ldGVySW5mb3JtYXRpb246IHsgbGFiZWxPZmZzZXRTdXBwb3J0OiB0cnVlIH0gfSB9LFxuICAgICAgICAgICAgICAgIGRlZmluaXRpb246ICAgICB7IGR5bmFtaWNSZWdpc3RyYXRpb246IHRydWUgfSxcbiAgICAgICAgICAgICAgICByZWZlcmVuY2VzOiAgICAgeyBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlIH0sXG4gICAgICAgICAgICAgICAgaW1wbGVtZW50YXRpb246IHsgZHluYW1pY1JlZ2lzdHJhdGlvbjogdHJ1ZSB9LFxuICAgICAgICAgICAgICAgIHR5cGVEZWZpbml0aW9uOiB7IGR5bmFtaWNSZWdpc3RyYXRpb246IHRydWUgfSxcbiAgICAgICAgICAgICAgICBkb2N1bWVudEhpZ2hsaWdodDogeyBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlIH0sXG4gICAgICAgICAgICAgICAgZG9jdW1lbnRTeW1ib2w6IHsgZHluYW1pY1JlZ2lzdHJhdGlvbjogdHJ1ZSwgaGllcmFyY2hpY2FsRG9jdW1lbnRTeW1ib2xTdXBwb3J0OiB0cnVlIH0sXG4gICAgICAgICAgICAgICAgZm9sZGluZ1JhbmdlOiAgIHsgZHluYW1pY1JlZ2lzdHJhdGlvbjogdHJ1ZSwgbGluZUZvbGRpbmdPbmx5OiBmYWxzZSB9LFxuICAgICAgICAgICAgICAgIHNlbGVjdGlvblJhbmdlOiB7IGR5bmFtaWNSZWdpc3RyYXRpb246IHRydWUgfSxcbiAgICAgICAgICAgICAgICBjb2RlTGVuczogICAgICAgeyBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlIH0sXG4gICAgICAgICAgICAgICAgaW5sYXlIaW50OiAgICAgIHsgZHluYW1pY1JlZ2lzdHJhdGlvbjogdHJ1ZSwgcmVzb2x2ZVN1cHBvcnQ6IHsgcHJvcGVydGllczogWydsYWJlbCddIH0gfSxcbiAgICAgICAgICAgICAgICBzZW1hbnRpY1Rva2Vuczoge1xuICAgICAgICAgICAgICAgICAgICBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlLFxuICAgICAgICAgICAgICAgICAgICByZXF1ZXN0czogICAgICAgIHsgcmFuZ2U6IGZhbHNlLCBmdWxsOiB7IGRlbHRhOiBmYWxzZSB9IH0sXG4gICAgICAgICAgICAgICAgICAgIHRva2VuVHlwZXM6ICAgICAgWyduYW1lc3BhY2UnLCAndHlwZScsICdjbGFzcycsICdlbnVtJywgJ2ludGVyZmFjZScsICdzdHJ1Y3QnLCAndHlwZVBhcmFtZXRlcicsXG4gICAgICAgICAgICAgICAgICAgICAgICAncGFyYW1ldGVyJywgJ3ZhcmlhYmxlJywgJ3Byb3BlcnR5JywgJ2VudW1NZW1iZXInLCAnZXZlbnQnLCAnZnVuY3Rpb24nLCAnbWV0aG9kJywgJ21hY3JvJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICdrZXl3b3JkJywgJ21vZGlmaWVyJywgJ2NvbW1lbnQnLCAnc3RyaW5nJywgJ251bWJlcicsICdyZWdleHAnLCAnb3BlcmF0b3InLCAnZGVjb3JhdG9yJ10sXG4gICAgICAgICAgICAgICAgICAgIHRva2VuTW9kaWZpZXJzOiAgWydkZWNsYXJhdGlvbicsICdkZWZpbml0aW9uJywgJ3JlYWRvbmx5JywgJ3N0YXRpYycsICdkZXByZWNhdGVkJywgJ2Fic3RyYWN0JyxcbiAgICAgICAgICAgICAgICAgICAgICAgICdhc3luYycsICdtb2RpZmljYXRpb24nLCAnZG9jdW1lbnRhdGlvbicsICdkZWZhdWx0TGlicmFyeSddLFxuICAgICAgICAgICAgICAgICAgICBmb3JtYXRzOiAgICAgICAgIFsncmVsYXRpdmUnXSxcbiAgICAgICAgICAgICAgICAgICAgb3ZlcmxhcHBpbmdUb2tlblN1cHBvcnQ6IGZhbHNlLFxuICAgICAgICAgICAgICAgICAgICBtdWx0aWxpbmVUb2tlblN1cHBvcnQ6ICAgZmFsc2UsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICBmb3JtYXR0aW5nOiAgICAgeyBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlIH0sXG4gICAgICAgICAgICAgICAgcmFuZ2VGb3JtYXR0aW5nOiB7IGR5bmFtaWNSZWdpc3RyYXRpb246IHRydWUgfSxcbiAgICAgICAgICAgICAgICByZW5hbWU6ICAgICAgICAgeyBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlLCBwcmVwYXJlU3VwcG9ydDogdHJ1ZSB9LFxuICAgICAgICAgICAgICAgIGNvZGVBY3Rpb246IHtcbiAgICAgICAgICAgICAgICAgICAgZHluYW1pY1JlZ2lzdHJhdGlvbjogdHJ1ZSxcbiAgICAgICAgICAgICAgICAgICAgY29kZUFjdGlvbkxpdGVyYWxTdXBwb3J0OiB7XG4gICAgICAgICAgICAgICAgICAgICAgICBjb2RlQWN0aW9uS2luZDoge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhbHVlU2V0OiBbJ3F1aWNrZml4JywgJ3JlZmFjdG9yJywgJ3JlZmFjdG9yLmV4dHJhY3QnLCAncmVmYWN0b3IuaW5saW5lJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgJ3JlZmFjdG9yLnJld3JpdGUnLCAnc291cmNlJywgJ3NvdXJjZS5vcmdhbml6ZUltcG9ydHMnXSxcbiAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgIGlzUHJlZmVycmVkU3VwcG9ydDogdHJ1ZSxcbiAgICAgICAgICAgICAgICAgICAgZGF0YVN1cHBvcnQ6ICAgICAgICB0cnVlLFxuICAgICAgICAgICAgICAgICAgICByZXNvbHZlU3VwcG9ydDogICAgIHsgcHJvcGVydGllczogWydlZGl0J10gfSxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIHB1Ymxpc2hEaWFnbm9zdGljczogeyByZWxhdGVkSW5mb3JtYXRpb246IHRydWUgfSxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICB3b3Jrc3BhY2U6IHtcbiAgICAgICAgICAgICAgICBhcHBseUVkaXQ6ICAgICAgICAgICAgICB0cnVlLFxuICAgICAgICAgICAgICAgIGNvbmZpZ3VyYXRpb246ICAgICAgICAgIHRydWUsXG4gICAgICAgICAgICAgICAgZXhlY3V0ZUNvbW1hbmQ6ICAgICAgICAgeyBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlIH0sXG4gICAgICAgICAgICAgICAgZGlkQ2hhbmdlQ29uZmlndXJhdGlvbjogeyBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlIH0sXG4gICAgICAgICAgICAgICAgd29ya3NwYWNlRWRpdDogICAgICAgICAgeyBkb2N1bWVudENoYW5nZXM6IHRydWUsIHJlc291cmNlT3BlcmF0aW9uczogWydjcmVhdGUnLCAncmVuYW1lJywgJ2RlbGV0ZSddIH0sXG4gICAgICAgICAgICB9LFxuICAgICAgICB9LFxuICAgIH0pO1xuXG4gICAgX3NlbWFudGljVG9rZW5zTGVnZW5kID0gaW5pdFJlc3VsdD8uY2FwYWJpbGl0aWVzPy5zZW1hbnRpY1Rva2Vuc1Byb3ZpZGVyPy5sZWdlbmQgPz8gbnVsbDtcblxuICAgIF9jb25uLnNlbmROb3RpZmljYXRpb24oJ2luaXRpYWxpemVkJywge30pO1xuICAgIF9jb25uLnNlbmROb3RpZmljYXRpb24oJ3dvcmtzcGFjZS9kaWRDaGFuZ2VDb25maWd1cmF0aW9uJywgeyBzZXR0aW5nczogamR0bHNTZXR0aW5ncygpIH0pO1xuXG4gICAgb3BlbkZpbGUoZmlsZVVyaSk7XG5cbiAgICBpZiAoIV9wcm92aWRlcnNSZWdpc3RlcmVkKSB7XG4gICAgICAgIF9wcm92aWRlcnNSZWdpc3RlcmVkID0gdHJ1ZTtcbiAgICAgICAgcmVnaXN0ZXJQcm92aWRlcnMoKTtcbiAgICB9XG59XG5cbi8vIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbi8vIEZpbGUgbGlmZWN5Y2xlXG4vLyAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG5cbi8qKlxuICogU2VuZHMgdGV4dERvY3VtZW50L2RpZE9wZW4gZm9yIHRoZSBnaXZlbiBVUkkgKGlmIG5vdCBhbHJlYWR5IHNlbnQpIGFuZCByZWdpc3RlcnMgYSBkZWJvdW5jZWRcbiAqIHRleHREb2N1bWVudC9kaWRDaGFuZ2UgbGlzdGVuZXIgb24gdGhlIGNvcnJlc3BvbmRpbmcgTW9uYWNvIG1vZGVsLlxuICovXG5mdW5jdGlvbiBvcGVuRmlsZShmaWxlVXJpOiBzdHJpbmcpOiB2b2lkIHtcbiAgICBpZiAoX29wZW5GaWxlcy5oYXMoZmlsZVVyaSkgfHwgIV9jb25uKSByZXR1cm47XG4gICAgX29wZW5GaWxlcy5hZGQoZmlsZVVyaSk7XG5cbiAgICBjb25zdCBtb2RlbCA9IG1vbmFjby5lZGl0b3IuZ2V0TW9kZWxzKCkuZmluZChtID0+IG0udXJpLnRvU3RyaW5nKCkgPT09IGZpbGVVcmkpO1xuICAgIF9jb25uLnNlbmROb3RpZmljYXRpb24oJ3RleHREb2N1bWVudC9kaWRPcGVuJywge1xuICAgICAgICB0ZXh0RG9jdW1lbnQ6IHtcbiAgICAgICAgICAgIHVyaTogICAgICAgIGZpbGVVcmksXG4gICAgICAgICAgICBsYW5ndWFnZUlkOiAnamF2YScsXG4gICAgICAgICAgICB2ZXJzaW9uOiAgICAxLFxuICAgICAgICAgICAgdGV4dDogICAgICAgbW9kZWw/LmdldFZhbHVlKCkgPz8gJycsXG4gICAgICAgIH0sXG4gICAgfSk7XG4gICAgLy8gVGVsbCBKRFQuTFMgdGhlIGZpbGUgZXhpc3RzIG9uIGRpc2sgc28gaXRzIHByb2plY3QgbW9kZWwgaW5jbHVkZXMgaXQgZXZlbiBiZWZvcmUgYSBidWlsZCBcdTIwMTQgdGhpc1xuICAgIC8vIG1ha2VzIGEganVzdC1jcmVhdGVkIHR5cGUgKGUuZy4gYSBuZXcgaW50ZXJmYWNlKSByZXNvbHZhYmxlL29mZmVyZWQgaW4gc2libGluZyBmaWxlcyBpbW1lZGlhdGVseS5cbiAgICBfY29ubi5zZW5kTm90aWZpY2F0aW9uKCd3b3Jrc3BhY2UvZGlkQ2hhbmdlV2F0Y2hlZEZpbGVzJywgeyBjaGFuZ2VzOiBbeyB1cmk6IGZpbGVVcmksIHR5cGU6IDEgLyogQ3JlYXRlZCAqLyB9XSB9KTtcblxuICAgIGlmIChtb2RlbCkge1xuICAgICAgICBtb2RlbC5vbkRpZENoYW5nZUNvbnRlbnQoKCkgPT4ge1xuICAgICAgICAgICAgY29uc3QgZXhpc3RpbmcgPSBfY2hhbmdlVGltZXJzLmdldChmaWxlVXJpKTtcbiAgICAgICAgICAgIGlmIChleGlzdGluZykgY2xlYXJUaW1lb3V0KGV4aXN0aW5nKTtcbiAgICAgICAgICAgIF9jaGFuZ2VUaW1lcnMuc2V0KGZpbGVVcmksIHNldFRpbWVvdXQoKCkgPT4gc2VuZERpZENoYW5nZShmaWxlVXJpKSwgNDAwKSk7XG4gICAgICAgIH0pO1xuICAgIH1cbn1cblxuLyoqIFNlbmRzIHRoZSBjdXJyZW50IG1vZGVsIGNvbnRlbnQgYXMgYSBkaWRDaGFuZ2UgYW5kIGNsZWFycyBhbnkgcGVuZGluZyBkZWJvdW5jZSBmb3IgdGhlIGZpbGUuICovXG5mdW5jdGlvbiBzZW5kRGlkQ2hhbmdlKGZpbGVVcmk6IHN0cmluZyk6IHZvaWQge1xuICAgIF9jaGFuZ2VUaW1lcnMuZGVsZXRlKGZpbGVVcmkpO1xuICAgIGNvbnN0IG1vZGVsID0gbW9uYWNvLmVkaXRvci5nZXRNb2RlbChtb25hY28uVXJpLnBhcnNlKGZpbGVVcmkpKTtcbiAgICBpZiAoX2Nvbm4gJiYgbW9kZWwpIHtcbiAgICAgICAgX2Nvbm4uc2VuZE5vdGlmaWNhdGlvbigndGV4dERvY3VtZW50L2RpZENoYW5nZScsIHtcbiAgICAgICAgICAgIHRleHREb2N1bWVudDogICB7IHVyaTogZmlsZVVyaSwgdmVyc2lvbjogbW9kZWwuZ2V0VmVyc2lvbklkKCkgfSxcbiAgICAgICAgICAgIGNvbnRlbnRDaGFuZ2VzOiBbeyB0ZXh0OiBtb2RlbC5nZXRWYWx1ZSgpIH1dLFxuICAgICAgICB9KTtcbiAgICB9XG59XG5cbi8qKlxuICogRmx1c2hlcyBhIHBlbmRpbmcgZGVib3VuY2VkIGNoYW5nZSBpbW1lZGlhdGVseS4gQ2FsbGVkIGJlZm9yZSBhIGNvbXBsZXRpb24gcmVxdWVzdCBzbyBKRFQuTFMgc2VlcyB0aGVcbiAqIGp1c3QtdHlwZWQgdGV4dCBvbiB0aGUgZmlyc3QgQ3RybCtTcGFjZSwgaW5zdGVhZCBvZiBjb21wbGV0aW5nIGFnYWluc3Qgc3RhbGUgY29udGVudCAodGhlIGRlYm91bmNlXG4gKiBvdGhlcndpc2UgZGVsYXlzIHRoZSBjaGFuZ2UgYnkgdXAgdG8gNDAwbXMgXHUyMDE0IHRoZSBjYXVzZSBvZiBcImZpcnN0IEN0cmwrU3BhY2Ugc2hvd3Mgbm90aGluZ1wiKS5cbiAqL1xuZnVuY3Rpb24gZmx1c2hQZW5kaW5nQ2hhbmdlKGZpbGVVcmk6IHN0cmluZyk6IHZvaWQge1xuICAgIGlmIChfY2hhbmdlVGltZXJzLmhhcyhmaWxlVXJpKSkge1xuICAgICAgICBjbGVhclRpbWVvdXQoX2NoYW5nZVRpbWVycy5nZXQoZmlsZVVyaSkhKTtcbiAgICAgICAgc2VuZERpZENoYW5nZShmaWxlVXJpKTtcbiAgICB9XG59XG5cbi8vIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbi8vIFdvcmtzcGFjZSBmaWxlIHByZWRpY2F0ZVxuLy8gLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuXG4vKipcbiAqIFJldHVybnMge0Bjb2RlIHRydWV9IHdoZW4gdGhlIGdpdmVuIG1vZGVsIFVSSSBiZWxvbmdzIHRvIHRoZSBjdXJyZW50bHkgY29ubmVjdGVkIHdvcmtzcGFjZS4gVXNlZFxuICogYnkgTW9uYWNvIHByb3ZpZGVycyB0byBza2lwIG5vbi1KYXZhLXdvcmtzcGFjZSBtb2RlbHMgd2l0aG91dCBhbiBleGFjdC1VUkkgY29tcGFyaXNvbi5cbiAqL1xuZnVuY3Rpb24gaXNXb3Jrc3BhY2VGaWxlKHVyaTogc3RyaW5nKTogYm9vbGVhbiB7XG4gICAgcmV0dXJuIF93b3Jrc3BhY2VSb290ICE9PSAnJyAmJiB1cmkuc3RhcnRzV2l0aChfd29ya3NwYWNlUm9vdCk7XG59XG5cbi8vIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbi8vIE1vbmFjbyBwcm92aWRlciByZWdpc3RyYXRpb25cbi8vIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cblxuZnVuY3Rpb24gcmVnaXN0ZXJQcm92aWRlcnMoKTogdm9pZCB7XG5cbiAgICBtb25hY28uZWRpdG9yLnJlZ2lzdGVyQ29tbWFuZChBUFBMWV9BQ1RJT05fQ09NTUFORCwgKF9hY2Nlc3NvcjogdW5rbm93biwgYWN0aW9uOiBDb2RlQWN0aW9uIHwgQ29tbWFuZCkgPT4ge1xuICAgICAgICBhcHBseUNvZGVBY3Rpb24oYWN0aW9uKTtcbiAgICB9KTtcbiAgICBtb25hY28uZWRpdG9yLnJlZ2lzdGVyQ29tbWFuZChOT09QX0NPTU1BTkQsICgpID0+IHsgLyogZGlzcGxheS1vbmx5IENvZGVMZW5zICovIH0pO1xuXG4gICAgLy8gQ3Jvc3MtZmlsZSBuYXZpZ2F0aW9uOiB0aGlzIHNpbmdsZS1maWxlIGVkaXRvciBoYXMgbm8gbW9kZWwgZm9yIG90aGVyIHdvcmtzcGFjZSBmaWxlcywgc28gR28gdG9cbiAgICAvLyBEZWZpbml0aW9uIC8gRmluZCBSZWZlcmVuY2VzIHRvIGFub3RoZXIgZmlsZSB3b3VsZCBzaWxlbnRseSBkbyBub3RoaW5nLiBIYW5kIHRob3NlIG9mZiB0byB0aGUgSURFXG4gICAgLy8gdG8gb3BlbiB0aGUgdGFyZ2V0IGZpbGUgKGFuZCByZXZlYWwgdGhlIGxpbmUpLiBTYW1lLWZpbGUgdGFyZ2V0cyBmYWxsIHRocm91Z2ggdG8gTW9uYWNvLlxuICAgIG1vbmFjby5lZGl0b3IucmVnaXN0ZXJFZGl0b3JPcGVuZXIoe1xuICAgICAgICBvcGVuQ29kZUVkaXRvcjogKHNvdXJjZSwgcmVzb3VyY2UsIHNlbGVjdGlvbk9yUG9zaXRpb24pID0+IHtcbiAgICAgICAgICAgIGNvbnN0IHVyaSA9IHJlc291cmNlLnRvU3RyaW5nKCk7XG4gICAgICAgICAgICBpZiAoIWlzV29ya3NwYWNlRmlsZSh1cmkpIHx8ICF1cmkuc3RhcnRzV2l0aChWSVJUVUFMX0ZJTEVfUFJFRklYKSkgcmV0dXJuIGZhbHNlO1xuICAgICAgICAgICAgY29uc3QgY3VycmVudE1vZGVsID0gc291cmNlLmdldE1vZGVsKCk7XG4gICAgICAgICAgICBpZiAoY3VycmVudE1vZGVsICYmIGN1cnJlbnRNb2RlbC51cmkudG9TdHJpbmcoKSA9PT0gdXJpKSByZXR1cm4gZmFsc2U7IC8vIHNhbWUgZmlsZSBcdTIwMTQgbGV0IE1vbmFjbyBqdW1wXG4gICAgICAgICAgICBjb25zdCBvcGVuZXIgPSAoZ2xvYmFsVGhpcyBhcyBhbnkpLmphdmFMc3BPcGVuRmlsZTtcbiAgICAgICAgICAgIGlmICh0eXBlb2Ygb3BlbmVyICE9PSAnZnVuY3Rpb24nKSByZXR1cm4gZmFsc2U7XG4gICAgICAgICAgICBjb25zdCBwb3MgPSBzZWxlY3Rpb25PclBvc2l0aW9uIGFzIHsgc3RhcnRMaW5lTnVtYmVyPzogbnVtYmVyOyBsaW5lTnVtYmVyPzogbnVtYmVyOyBzdGFydENvbHVtbj86IG51bWJlcjsgY29sdW1uPzogbnVtYmVyIH0gfCB1bmRlZmluZWQ7XG4gICAgICAgICAgICBjb25zdCBsaW5lID0gcG9zID8gKHBvcy5zdGFydExpbmVOdW1iZXIgPz8gcG9zLmxpbmVOdW1iZXIpIDogdW5kZWZpbmVkO1xuICAgICAgICAgICAgY29uc3QgY29sdW1uID0gcG9zID8gKHBvcy5zdGFydENvbHVtbiA/PyBwb3MuY29sdW1uKSA6IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIG9wZW5lcih1cmkuc3Vic3RyaW5nKFZJUlRVQUxfRklMRV9QUkVGSVgubGVuZ3RoKSwgbGluZSwgY29sdW1uKTtcbiAgICAgICAgICAgIHJldHVybiB0cnVlO1xuICAgICAgICB9LFxuICAgIH0pO1xuXG4gICAgbW9uYWNvLmxhbmd1YWdlcy5yZWdpc3RlckNvbXBsZXRpb25JdGVtUHJvdmlkZXIoJ2phdmEnLCB7XG4gICAgICAgIHRyaWdnZXJDaGFyYWN0ZXJzOiBbJy4nLCAnQCcsICc8J10sXG4gICAgICAgIHByb3ZpZGVDb21wbGV0aW9uSXRlbXM6IGFzeW5jIChtb2RlbCwgcG9zaXRpb24sIGNvbnRleHQpID0+IHtcbiAgICAgICAgICAgIGlmICghX2Nvbm4gfHwgIWlzV29ya3NwYWNlRmlsZShtb2RlbC51cmkudG9TdHJpbmcoKSkpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgY29uc3QgZmlsZVVyaSA9IG1vZGVsLnVyaS50b1N0cmluZygpO1xuICAgICAgICAgICAgLy8gTWFrZSBzdXJlIEpEVC5MUyBoYXMgdGhlIGp1c3QtdHlwZWQgdGV4dCBiZWZvcmUgY29tcGxldGluZyAoc2VlIGZsdXNoUGVuZGluZ0NoYW5nZSkuXG4gICAgICAgICAgICBmbHVzaFBlbmRpbmdDaGFuZ2UoZmlsZVVyaSk7XG4gICAgICAgICAgICBjb25zdCByZXN1bHQ6IENvbXBsZXRpb25MaXN0IHwgQ29tcGxldGlvbkl0ZW1bXSB8IG51bGwgPSBhd2FpdCBfY29ubi5zZW5kUmVxdWVzdCgndGV4dERvY3VtZW50L2NvbXBsZXRpb24nLCB7XG4gICAgICAgICAgICAgICAgdGV4dERvY3VtZW50OiB7IHVyaTogZmlsZVVyaSB9LFxuICAgICAgICAgICAgICAgIHBvc2l0aW9uOiAgICAgeyBsaW5lOiBwb3NpdGlvbi5saW5lTnVtYmVyIC0gMSwgY2hhcmFjdGVyOiBwb3NpdGlvbi5jb2x1bW4gLSAxIH0sXG4gICAgICAgICAgICAgICAgLy8gTW9uYWNvIHRyaWdnZXIga2luZHMgYXJlIDAtYmFzZWQgKEludm9rZS9UcmlnZ2VyQ2hhcmFjdGVyL0ZvckluY29tcGxldGUpOyBMU1AgaXMgMS1iYXNlZC5cbiAgICAgICAgICAgICAgICBjb250ZXh0OiAgICAgIHsgdHJpZ2dlcktpbmQ6IChjb250ZXh0LnRyaWdnZXJLaW5kID8/IDApICsgMSwgdHJpZ2dlckNoYXJhY3RlcjogY29udGV4dC50cmlnZ2VyQ2hhcmFjdGVyIH0sXG4gICAgICAgICAgICB9KTtcbiAgICAgICAgICAgIGNvbnN0IGl0ZW1zID0gQXJyYXkuaXNBcnJheShyZXN1bHQpID8gcmVzdWx0IDogKHJlc3VsdD8uaXRlbXMgPz8gW10pO1xuICAgICAgICAgICAgcmV0dXJuIHtcbiAgICAgICAgICAgICAgICBzdWdnZXN0aW9uczogaXRlbXMubWFwKGl0ZW0gPT4gbHNwQ29tcGxldGlvblRvTW9uYWNvKGl0ZW0sIG1vZGVsLCBwb3NpdGlvbikpLFxuICAgICAgICAgICAgICAgIC8vIEpEVC5MUyByZXR1cm5zIGEgdHJ1bmNhdGVkIGxpc3Qgb24gdGhlIGZpcnN0IGtleXN0cm9rZXM7IHByb3BhZ2F0aW5nIFwiaW5jb21wbGV0ZVwiIG1ha2VzXG4gICAgICAgICAgICAgICAgLy8gTW9uYWNvIHJlLXF1ZXJ5IGFzIHRoZSB1c2VyIHR5cGVzIGluc3RlYWQgb2YgY2FjaGluZyB0aGUgZmlyc3QgKG9mdGVuIGVtcHR5KSByZXN1bHQuXG4gICAgICAgICAgICAgICAgaW5jb21wbGV0ZTogIEFycmF5LmlzQXJyYXkocmVzdWx0KSA/IGZhbHNlIDogISFyZXN1bHQ/LmlzSW5jb21wbGV0ZSxcbiAgICAgICAgICAgIH07XG4gICAgICAgIH0sXG4gICAgICAgIC8vIFJlc29sdmUgZG9jdW1lbnRhdGlvbiBhbmQsIGNydWNpYWxseSwgdGhlIGF1dG8taW1wb3J0IGFkZGl0aW9uYWxUZXh0RWRpdHMgd2hpY2ggSkRULkxTIG9ubHlcbiAgICAgICAgLy8gYXR0YWNoZXMgb24gcmVzb2x2ZSBcdTIwMTQgc2VsZWN0aW5nIGEgdHlwZSB0aGVuIGluc2VydHMgaXRzIGltcG9ydCBzdGF0ZW1lbnQuXG4gICAgICAgIHJlc29sdmVDb21wbGV0aW9uSXRlbTogYXN5bmMgKGl0ZW0pID0+IHtcbiAgICAgICAgICAgIGNvbnN0IGxzcCA9IChpdGVtIGFzIE1vbmFjb0NvbXBsZXRpb25JdGVtKS5fbHNwO1xuICAgICAgICAgICAgaWYgKCFfY29ubiB8fCAhbHNwKSByZXR1cm4gaXRlbTtcbiAgICAgICAgICAgIHRyeSB7XG4gICAgICAgICAgICAgICAgY29uc3QgcmVzb2x2ZWQ6IENvbXBsZXRpb25JdGVtID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ2NvbXBsZXRpb25JdGVtL3Jlc29sdmUnLCBsc3ApO1xuICAgICAgICAgICAgICAgIGlmIChyZXNvbHZlZC5kb2N1bWVudGF0aW9uKSB7XG4gICAgICAgICAgICAgICAgICAgIGl0ZW0uZG9jdW1lbnRhdGlvbiA9IHsgdmFsdWU6IG1hcmt1cFRvU3RyaW5nKHJlc29sdmVkLmRvY3VtZW50YXRpb24pLCBpc1RydXN0ZWQ6IGZhbHNlIH07XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGlmIChyZXNvbHZlZC5kZXRhaWwpIGl0ZW0uZGV0YWlsID0gcmVzb2x2ZWQuZGV0YWlsO1xuICAgICAgICAgICAgICAgIGlmIChyZXNvbHZlZC5hZGRpdGlvbmFsVGV4dEVkaXRzPy5sZW5ndGgpIHtcbiAgICAgICAgICAgICAgICAgICAgaXRlbS5hZGRpdGlvbmFsVGV4dEVkaXRzID0gcmVzb2x2ZWQuYWRkaXRpb25hbFRleHRFZGl0cy5tYXAodGV4dEVkaXRUb01vbmFjbyk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfSBjYXRjaCAoZSkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUuZGVidWcoJ1tqYXZhLWxzcF0gY29tcGxldGlvbiByZXNvbHZlIGZhaWxlZDonLCAoZSBhcyBFcnJvcik/Lm1lc3NhZ2UpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgcmV0dXJuIGl0ZW07XG4gICAgICAgIH0sXG4gICAgfSk7XG5cbiAgICBtb25hY28ubGFuZ3VhZ2VzLnJlZ2lzdGVySG92ZXJQcm92aWRlcignamF2YScsIHtcbiAgICAgICAgcHJvdmlkZUhvdmVyOiBhc3luYyAobW9kZWwsIHBvc2l0aW9uKSA9PiB7XG4gICAgICAgICAgICBpZiAoIV9jb25uIHx8ICFpc1dvcmtzcGFjZUZpbGUobW9kZWwudXJpLnRvU3RyaW5nKCkpKSByZXR1cm4gbnVsbDtcbiAgICAgICAgICAgIGNvbnN0IGZpbGVVcmkgPSBtb2RlbC51cmkudG9TdHJpbmcoKTtcbiAgICAgICAgICAgIGNvbnN0IHJlc3VsdDogSG92ZXIgfCBudWxsID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ3RleHREb2N1bWVudC9ob3ZlcicsIHtcbiAgICAgICAgICAgICAgICB0ZXh0RG9jdW1lbnQ6IHsgdXJpOiBmaWxlVXJpIH0sXG4gICAgICAgICAgICAgICAgcG9zaXRpb246ICAgICB7IGxpbmU6IHBvc2l0aW9uLmxpbmVOdW1iZXIgLSAxLCBjaGFyYWN0ZXI6IHBvc2l0aW9uLmNvbHVtbiAtIDEgfSxcbiAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgaWYgKCFyZXN1bHQ/LmNvbnRlbnRzKSByZXR1cm4gbnVsbDtcbiAgICAgICAgICAgIGNvbnN0IGNvbnRlbnRzID0gQXJyYXkuaXNBcnJheShyZXN1bHQuY29udGVudHMpID8gcmVzdWx0LmNvbnRlbnRzIDogW3Jlc3VsdC5jb250ZW50c107XG4gICAgICAgICAgICByZXR1cm4ge1xuICAgICAgICAgICAgICAgIGNvbnRlbnRzOiBjb250ZW50cy5tYXAoYyA9PiAoe1xuICAgICAgICAgICAgICAgICAgICB2YWx1ZTogdHlwZW9mIGMgPT09ICdzdHJpbmcnID8gYyA6IChjIGFzIE1hcmt1cENvbnRlbnQpLnZhbHVlLFxuICAgICAgICAgICAgICAgICAgICBpc1RydXN0ZWQ6IGZhbHNlLFxuICAgICAgICAgICAgICAgIH0pKSxcbiAgICAgICAgICAgICAgICByYW5nZTogcmVzdWx0LnJhbmdlID8gbHNwUmFuZ2VUb01vbmFjbyhyZXN1bHQucmFuZ2UpIDogdW5kZWZpbmVkLFxuICAgICAgICAgICAgfTtcbiAgICAgICAgfSxcbiAgICB9KTtcblxuICAgIG1vbmFjby5sYW5ndWFnZXMucmVnaXN0ZXJTaWduYXR1cmVIZWxwUHJvdmlkZXIoJ2phdmEnLCB7XG4gICAgICAgIHNpZ25hdHVyZUhlbHBUcmlnZ2VyQ2hhcmFjdGVyczogWycoJywgJywnXSxcbiAgICAgICAgcHJvdmlkZVNpZ25hdHVyZUhlbHA6IGFzeW5jIChtb2RlbCwgcG9zaXRpb24pID0+IHtcbiAgICAgICAgICAgIGlmICghX2Nvbm4gfHwgIWlzV29ya3NwYWNlRmlsZShtb2RlbC51cmkudG9TdHJpbmcoKSkpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgY29uc3QgZmlsZVVyaSA9IG1vZGVsLnVyaS50b1N0cmluZygpO1xuICAgICAgICAgICAgY29uc3QgcmVzdWx0OiBTaWduYXR1cmVIZWxwIHwgbnVsbCA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCd0ZXh0RG9jdW1lbnQvc2lnbmF0dXJlSGVscCcsIHtcbiAgICAgICAgICAgICAgICB0ZXh0RG9jdW1lbnQ6IHsgdXJpOiBmaWxlVXJpIH0sXG4gICAgICAgICAgICAgICAgcG9zaXRpb246ICAgICB7IGxpbmU6IHBvc2l0aW9uLmxpbmVOdW1iZXIgLSAxLCBjaGFyYWN0ZXI6IHBvc2l0aW9uLmNvbHVtbiAtIDEgfSxcbiAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgaWYgKCFyZXN1bHQpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgcmV0dXJuIHtcbiAgICAgICAgICAgICAgICB2YWx1ZToge1xuICAgICAgICAgICAgICAgICAgICBzaWduYXR1cmVzOiByZXN1bHQuc2lnbmF0dXJlcy5tYXAoKHNpZzogU2lnbmF0dXJlSW5mb3JtYXRpb24pID0+ICh7XG4gICAgICAgICAgICAgICAgICAgICAgICBsYWJlbDogICAgICAgICBzaWcubGFiZWwsXG4gICAgICAgICAgICAgICAgICAgICAgICBkb2N1bWVudGF0aW9uOiBzaWcuZG9jdW1lbnRhdGlvbiA/IG1hcmt1cFRvU3RyaW5nKHNpZy5kb2N1bWVudGF0aW9uKSA6IHVuZGVmaW5lZCxcbiAgICAgICAgICAgICAgICAgICAgICAgIHBhcmFtZXRlcnM6ICAgIChzaWcucGFyYW1ldGVycyA/PyBbXSkubWFwKChwOiBQYXJhbWV0ZXJJbmZvcm1hdGlvbikgPT4gKHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBsYWJlbDogICAgICAgICBwLmxhYmVsLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGRvY3VtZW50YXRpb246IHAuZG9jdW1lbnRhdGlvbiA/IG1hcmt1cFRvU3RyaW5nKHAuZG9jdW1lbnRhdGlvbikgOiB1bmRlZmluZWQsXG4gICAgICAgICAgICAgICAgICAgICAgICB9KSksXG4gICAgICAgICAgICAgICAgICAgIH0pKSxcbiAgICAgICAgICAgICAgICAgICAgYWN0aXZlU2lnbmF0dXJlOiByZXN1bHQuYWN0aXZlU2lnbmF0dXJlID8/IDAsXG4gICAgICAgICAgICAgICAgICAgIGFjdGl2ZVBhcmFtZXRlcjogcmVzdWx0LmFjdGl2ZVBhcmFtZXRlciA/PyAwLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgZGlzcG9zZTogKCkgPT4ge30sXG4gICAgICAgICAgICB9O1xuICAgICAgICB9LFxuICAgIH0pO1xuXG4gICAgbW9uYWNvLmxhbmd1YWdlcy5yZWdpc3RlckRlZmluaXRpb25Qcm92aWRlcignamF2YScsIHtcbiAgICAgICAgcHJvdmlkZURlZmluaXRpb246IGFzeW5jIChtb2RlbCwgcG9zaXRpb24pID0+IHtcbiAgICAgICAgICAgIGlmICghX2Nvbm4gfHwgIWlzV29ya3NwYWNlRmlsZShtb2RlbC51cmkudG9TdHJpbmcoKSkpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgY29uc3QgZmlsZVVyaSA9IG1vZGVsLnVyaS50b1N0cmluZygpO1xuICAgICAgICAgICAgY29uc3QgcmVzdWx0OiBMb2NhdGlvbiB8IExvY2F0aW9uW10gfCBudWxsID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ3RleHREb2N1bWVudC9kZWZpbml0aW9uJywge1xuICAgICAgICAgICAgICAgIHRleHREb2N1bWVudDogeyB1cmk6IGZpbGVVcmkgfSxcbiAgICAgICAgICAgICAgICBwb3NpdGlvbjogICAgIHsgbGluZTogcG9zaXRpb24ubGluZU51bWJlciAtIDEsIGNoYXJhY3RlcjogcG9zaXRpb24uY29sdW1uIC0gMSB9LFxuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICBpZiAoIXJlc3VsdCkgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICBjb25zdCBsb2NhdGlvbnMgPSAoQXJyYXkuaXNBcnJheShyZXN1bHQpID8gcmVzdWx0IDogW3Jlc3VsdF0pLm1hcChsb2MgPT4gKHtcbiAgICAgICAgICAgICAgICB1cmk6ICAgbW9uYWNvLlVyaS5wYXJzZShsb2MudXJpKSxcbiAgICAgICAgICAgICAgICByYW5nZTogbHNwUmFuZ2VUb01vbmFjbyhsb2MucmFuZ2UpLFxuICAgICAgICAgICAgfSkpO1xuICAgICAgICAgICAgYXdhaXQgZW5zdXJlTW9kZWxzRm9yTG9jYXRpb25zKGxvY2F0aW9ucyk7XG4gICAgICAgICAgICByZXR1cm4gbG9jYXRpb25zO1xuICAgICAgICB9LFxuICAgIH0pO1xuXG4gICAgbW9uYWNvLmxhbmd1YWdlcy5yZWdpc3RlclJlZmVyZW5jZVByb3ZpZGVyKCdqYXZhJywge1xuICAgICAgICBwcm92aWRlUmVmZXJlbmNlczogYXN5bmMgKG1vZGVsLCBwb3NpdGlvbiwgY29udGV4dCkgPT4ge1xuICAgICAgICAgICAgaWYgKCFfY29ubiB8fCAhaXNXb3Jrc3BhY2VGaWxlKG1vZGVsLnVyaS50b1N0cmluZygpKSkgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICBjb25zdCByZXN1bHQ6IExvY2F0aW9uW10gfCBudWxsID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ3RleHREb2N1bWVudC9yZWZlcmVuY2VzJywge1xuICAgICAgICAgICAgICAgIHRleHREb2N1bWVudDogeyB1cmk6IG1vZGVsLnVyaS50b1N0cmluZygpIH0sXG4gICAgICAgICAgICAgICAgcG9zaXRpb246ICAgICB7IGxpbmU6IHBvc2l0aW9uLmxpbmVOdW1iZXIgLSAxLCBjaGFyYWN0ZXI6IHBvc2l0aW9uLmNvbHVtbiAtIDEgfSxcbiAgICAgICAgICAgICAgICBjb250ZXh0OiAgICAgIHsgaW5jbHVkZURlY2xhcmF0aW9uOiBjb250ZXh0LmluY2x1ZGVEZWNsYXJhdGlvbiB9LFxuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICBpZiAoIXJlc3VsdCkgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICBjb25zdCBsb2NhdGlvbnMgPSByZXN1bHQubWFwKGxvYyA9PiAoeyB1cmk6IG1vbmFjby5VcmkucGFyc2UobG9jLnVyaSksIHJhbmdlOiBsc3BSYW5nZVRvTW9uYWNvKGxvYy5yYW5nZSkgfSkpO1xuICAgICAgICAgICAgLy8gVGhlIHJlZmVyZW5jZXMgcGVlayBjYW4gb25seSBzaG93IGEgY29kZSBwcmV2aWV3IGZvciBmaWxlcyBpdCBoYXMgYSBtb2RlbCBmb3I7IGNyZWF0ZVxuICAgICAgICAgICAgLy8gaW4tbWVtb3J5IG1vZGVscyBmb3IgdGhlIHJlZmVyZW5jZWQgKHBvc3NpYmx5IHVub3BlbmVkKSBmaWxlcyBzbyBwcmV2aWV3cyByZW5kZXIuXG4gICAgICAgICAgICBhd2FpdCBlbnN1cmVNb2RlbHNGb3JMb2NhdGlvbnMobG9jYXRpb25zKTtcbiAgICAgICAgICAgIHJldHVybiBsb2NhdGlvbnM7XG4gICAgICAgIH0sXG4gICAgfSk7XG5cbiAgICBtb25hY28ubGFuZ3VhZ2VzLnJlZ2lzdGVyUmVuYW1lUHJvdmlkZXIoJ2phdmEnLCB7XG4gICAgICAgIHByb3ZpZGVSZW5hbWVFZGl0czogYXN5bmMgKG1vZGVsLCBwb3NpdGlvbiwgbmV3TmFtZSkgPT4ge1xuICAgICAgICAgICAgaWYgKCFfY29ubiB8fCAhaXNXb3Jrc3BhY2VGaWxlKG1vZGVsLnVyaS50b1N0cmluZygpKSkgcmV0dXJuIHsgZWRpdHM6IFtdIH07XG4gICAgICAgICAgICBjb25zdCBlZGl0OiBXb3Jrc3BhY2VFZGl0IHwgbnVsbCA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCd0ZXh0RG9jdW1lbnQvcmVuYW1lJywge1xuICAgICAgICAgICAgICAgIHRleHREb2N1bWVudDogeyB1cmk6IG1vZGVsLnVyaS50b1N0cmluZygpIH0sXG4gICAgICAgICAgICAgICAgcG9zaXRpb246ICAgICB7IGxpbmU6IHBvc2l0aW9uLmxpbmVOdW1iZXIgLSAxLCBjaGFyYWN0ZXI6IHBvc2l0aW9uLmNvbHVtbiAtIDEgfSxcbiAgICAgICAgICAgICAgICBuZXdOYW1lLFxuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICBpZiAoIWVkaXQpIHJldHVybiB7IGVkaXRzOiBbXSB9O1xuICAgICAgICAgICAgLy8gQSBKRFQuTFMgcmVuYW1lIGNhbiBzcGFuIG1hbnkgZmlsZXMgYW5kIGV2ZW4gcmVuYW1lIHRoZSB0eXBlJ3Mgb3duIC5qYXZhIGZpbGUuIE1vbmFjbydzXG4gICAgICAgICAgICAvLyBzaW5nbGUtZmlsZSBlZGl0b3Igd291bGQgZHJvcCBldmVyeXRoaW5nIGJ1dCB0aGUgY3VycmVudCBtb2RlbCwgc28gYXBwbHkgdGhlIHdob2xlIGVkaXRcbiAgICAgICAgICAgIC8vIHRocm91Z2ggdGhlIHdvcmtzcGFjZSBvdXJzZWx2ZXMgd2hlbiB0aGUgSURFIHBlcnNpc3RlbmNlIGhvb2sgaXMgYXZhaWxhYmxlLlxuICAgICAgICAgICAgaWYgKHR5cGVvZiAoZ2xvYmFsVGhpcyBhcyBhbnkpLmphdmFMc3BQZXJzaXN0UmVuYW1lID09PSAnZnVuY3Rpb24nKSB7XG4gICAgICAgICAgICAgICAgdHJ5IHtcbiAgICAgICAgICAgICAgICAgICAgYXdhaXQgYXBwbHlSZW5hbWVBY3Jvc3NXb3Jrc3BhY2UobW9kZWwsIGVkaXQpO1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4geyBlZGl0czogW10gfTtcbiAgICAgICAgICAgICAgICB9IGNhdGNoIChlKSB7XG4gICAgICAgICAgICAgICAgICAgIGNvbnNvbGUuZXJyb3IoJ1tqYXZhLWxzcF0gY3Jvc3MtZmlsZSByZW5hbWUgZmFpbGVkLCBhcHBseWluZyB0byB0aGUgY3VycmVudCBmaWxlIG9ubHk6JywgZSk7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiB3b3Jrc3BhY2VFZGl0VG9Nb25hY28oZWRpdCk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICAgICAgcmV0dXJuIHdvcmtzcGFjZUVkaXRUb01vbmFjbyhlZGl0KTtcbiAgICAgICAgfSxcbiAgICAgICAgcmVzb2x2ZVJlbmFtZUxvY2F0aW9uOiBhc3luYyAobW9kZWwsIHBvc2l0aW9uKSA9PiB7XG4gICAgICAgICAgICBpZiAoIV9jb25uIHx8ICFpc1dvcmtzcGFjZUZpbGUobW9kZWwudXJpLnRvU3RyaW5nKCkpKSByZXR1cm4gbnVsbDtcbiAgICAgICAgICAgIHRyeSB7XG4gICAgICAgICAgICAgICAgY29uc3QgcmVzdWx0OiB7IHJhbmdlOiBMc3BSYW5nZTsgcGxhY2Vob2xkZXI/OiBzdHJpbmcgfSB8IExzcFJhbmdlIHwgbnVsbCA9XG4gICAgICAgICAgICAgICAgICAgIGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCd0ZXh0RG9jdW1lbnQvcHJlcGFyZVJlbmFtZScsIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHRleHREb2N1bWVudDogeyB1cmk6IG1vZGVsLnVyaS50b1N0cmluZygpIH0sXG4gICAgICAgICAgICAgICAgICAgICAgICBwb3NpdGlvbjogICAgIHsgbGluZTogcG9zaXRpb24ubGluZU51bWJlciAtIDEsIGNoYXJhY3RlcjogcG9zaXRpb24uY29sdW1uIC0gMSB9LFxuICAgICAgICAgICAgICAgICAgICB9KTtcbiAgICAgICAgICAgICAgICBpZiAoIXJlc3VsdCkgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICAgICAgY29uc3QgcmFuZ2UgPSAncmFuZ2UnIGluIHJlc3VsdCA/IHJlc3VsdC5yYW5nZSA6IHJlc3VsdDtcbiAgICAgICAgICAgICAgICBjb25zdCBwbGFjZWhvbGRlciA9ICdwbGFjZWhvbGRlcicgaW4gcmVzdWx0ICYmIHJlc3VsdC5wbGFjZWhvbGRlclxuICAgICAgICAgICAgICAgICAgICA/IHJlc3VsdC5wbGFjZWhvbGRlclxuICAgICAgICAgICAgICAgICAgICA6IG1vZGVsLmdldFdvcmRBdFBvc2l0aW9uKHBvc2l0aW9uKT8ud29yZCA/PyAnJztcbiAgICAgICAgICAgICAgICByZXR1cm4geyByYW5nZTogbHNwUmFuZ2VUb01vbmFjbyhyYW5nZSksIHRleHQ6IHBsYWNlaG9sZGVyIH07XG4gICAgICAgICAgICB9IGNhdGNoIHtcbiAgICAgICAgICAgICAgICBjb25zdCB3b3JkID0gbW9kZWwuZ2V0V29yZEF0UG9zaXRpb24ocG9zaXRpb24pO1xuICAgICAgICAgICAgICAgIHJldHVybiB3b3JkID8ge1xuICAgICAgICAgICAgICAgICAgICByYW5nZTogeyBzdGFydExpbmVOdW1iZXI6IHBvc2l0aW9uLmxpbmVOdW1iZXIsIHN0YXJ0Q29sdW1uOiB3b3JkLnN0YXJ0Q29sdW1uLCBlbmRMaW5lTnVtYmVyOiBwb3NpdGlvbi5saW5lTnVtYmVyLCBlbmRDb2x1bW46IHdvcmQuZW5kQ29sdW1uIH0sXG4gICAgICAgICAgICAgICAgICAgIHRleHQ6IHdvcmQud29yZCxcbiAgICAgICAgICAgICAgICB9IDogbnVsbDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSxcbiAgICB9KTtcblxuICAgIC8vIFJlZ2lzdGVyaW5nIHRoaXMgcHJvdmlkZXIgYWxzbyBlbmFibGVzIGVkaXRvci5qcydzIGV4aXN0aW5nIGZvcm1hdC1vbi1zYXZlIHBhdGggZm9yIEphdmE6IHRoZVxuICAgIC8vIHNoYXJlZCBTYXZlIGFjdGlvbiBydW5zIGVkaXRvci5hY3Rpb24uZm9ybWF0RG9jdW1lbnQgd2hlbiBhdXRvLWZvcm1hdHRpbmcgaXMgb24gKHRoZSBzYW1lXG4gICAgLy8gbWVjaGFuaXNtIGFuZCBnbG9iYWwgdG9nZ2xlIHVzZWQgZm9yIFR5cGVTY3JpcHQpLlxuICAgIG1vbmFjby5sYW5ndWFnZXMucmVnaXN0ZXJEb2N1bWVudEZvcm1hdHRpbmdFZGl0UHJvdmlkZXIoJ2phdmEnLCB7XG4gICAgICAgIHByb3ZpZGVEb2N1bWVudEZvcm1hdHRpbmdFZGl0czogYXN5bmMgKG1vZGVsKSA9PiB7XG4gICAgICAgICAgICBpZiAoIV9jb25uIHx8ICFpc1dvcmtzcGFjZUZpbGUobW9kZWwudXJpLnRvU3RyaW5nKCkpKSByZXR1cm4gbnVsbDtcbiAgICAgICAgICAgIGNvbnN0IGVkaXRzOiBUZXh0RWRpdFtdIHwgbnVsbCA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCd0ZXh0RG9jdW1lbnQvZm9ybWF0dGluZycsIHtcbiAgICAgICAgICAgICAgICB0ZXh0RG9jdW1lbnQ6IHsgdXJpOiBtb2RlbC51cmkudG9TdHJpbmcoKSB9LFxuICAgICAgICAgICAgICAgIG9wdGlvbnM6ICAgICAgeyB0YWJTaXplOiBtb2RlbC5nZXRPcHRpb25zKCkudGFiU2l6ZSwgaW5zZXJ0U3BhY2VzOiBtb2RlbC5nZXRPcHRpb25zKCkuaW5zZXJ0U3BhY2VzIH0sXG4gICAgICAgICAgICB9KTtcbiAgICAgICAgICAgIHJldHVybiBlZGl0cyA/IGVkaXRzLm1hcCh0ZXh0RWRpdFRvTW9uYWNvKSA6IG51bGw7XG4gICAgICAgIH0sXG4gICAgfSk7XG5cbiAgICBtb25hY28ubGFuZ3VhZ2VzLnJlZ2lzdGVyQ29kZUFjdGlvblByb3ZpZGVyKCdqYXZhJywge1xuICAgICAgICBwcm92aWRlQ29kZUFjdGlvbnM6IGFzeW5jIChtb2RlbCwgcmFuZ2UsIGNvbnRleHQpID0+IHtcbiAgICAgICAgICAgIGNvbnN0IGVtcHR5ID0geyBhY3Rpb25zOiBbXSwgZGlzcG9zZSgpIHsgLyogbm90aGluZyB0byBkaXNwb3NlICovIH0gfTtcbiAgICAgICAgICAgIGlmICghX2Nvbm4gfHwgIWlzV29ya3NwYWNlRmlsZShtb2RlbC51cmkudG9TdHJpbmcoKSkpIHJldHVybiBlbXB0eTtcbiAgICAgICAgICAgIC8vIFNlbmQgdGhlIG9yaWdpbmFsIExTUCBkaWFnbm9zdGljcyB0aGF0IG92ZXJsYXAgdGhlIHJhbmdlICh0aGV5IGNhcnJ5IHRoZSBjb2RlL2RhdGEgSkRULkxTXG4gICAgICAgICAgICAvLyBuZWVkcyB0byBjb21wdXRlIHF1aWNrLWZpeGVzKSwgbm90IG9uZXMgcmVjb25zdHJ1Y3RlZCBmcm9tIE1vbmFjbyBtYXJrZXJzLlxuICAgICAgICAgICAgY29uc3QgbHNwUmFuZ2UgPSBtb25hY29SYW5nZVRvTHNwKHJhbmdlKTtcbiAgICAgICAgICAgIGNvbnN0IGRpYWdub3N0aWNzID0gKF9kaWFnbm9zdGljcy5nZXQobW9kZWwudXJpLnRvU3RyaW5nKCkpID8/IFtdKS5maWx0ZXIoZCA9PiByYW5nZXNPdmVybGFwKGQucmFuZ2UsIGxzcFJhbmdlKSk7XG4gICAgICAgICAgICBjb25zdCByZXN1bHQ6IEFycmF5PENvZGVBY3Rpb24gfCBDb21tYW5kPiB8IG51bGwgPSBhd2FpdCBfY29ubi5zZW5kUmVxdWVzdCgndGV4dERvY3VtZW50L2NvZGVBY3Rpb24nLCB7XG4gICAgICAgICAgICAgICAgdGV4dERvY3VtZW50OiB7IHVyaTogbW9kZWwudXJpLnRvU3RyaW5nKCkgfSxcbiAgICAgICAgICAgICAgICByYW5nZTogICAgICAgIGxzcFJhbmdlLFxuICAgICAgICAgICAgICAgIC8vIE1vbmFjbydzIENvZGVBY3Rpb25UcmlnZ2VyVHlwZSAoSW52b2tlPTEsIEF1dG89MikgbWFwcyAxOjEgdG8gdGhlIExTUCB0cmlnZ2VyIGtpbmQuXG4gICAgICAgICAgICAgICAgLy8gRm9yd2FyZGluZyBpdCBsZXRzIEpEVC5MUyBjb21wdXRlIG9ubHkgcXVpY2stZml4ZXMgZm9yIHRoZSBwYXNzaXZlIGxpZ2h0YnVsYiAoY2hlYXApXG4gICAgICAgICAgICAgICAgLy8gYW5kIHRoZSBmdWxsIGFzc2lzdHMvcmVmYWN0b3JpbmdzIG9ubHkgb24gZXhwbGljaXQgQ3RybCsuIC8gUmVmYWN0b3JcdTIwMjYgKEludm9rZWQpLlxuICAgICAgICAgICAgICAgIGNvbnRleHQ6ICAgICAgeyBkaWFnbm9zdGljcywgb25seTogY29udGV4dC5vbmx5ID8gW2NvbnRleHQub25seV0gOiB1bmRlZmluZWQsIHRyaWdnZXJLaW5kOiBjb250ZXh0LnRyaWdnZXIgfSxcbiAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgaWYgKCFyZXN1bHQ/Lmxlbmd0aCkgcmV0dXJuIGVtcHR5O1xuICAgICAgICAgICAgcmV0dXJuIHtcbiAgICAgICAgICAgICAgICBhY3Rpb25zOiByZXN1bHQubWFwKGxzcENvZGVBY3Rpb25Ub01vbmFjbyksXG4gICAgICAgICAgICAgICAgZGlzcG9zZSgpIHsgLyogbm90aGluZyB0byBkaXNwb3NlICovIH0sXG4gICAgICAgICAgICB9O1xuICAgICAgICB9LFxuICAgIH0sIHtcbiAgICAgICAgcHJvdmlkZWRDb2RlQWN0aW9uS2luZHM6IFsncXVpY2tmaXgnLCAncmVmYWN0b3InLCAncmVmYWN0b3IuZXh0cmFjdCcsICdyZWZhY3Rvci5pbmxpbmUnLFxuICAgICAgICAgICAgJ3JlZmFjdG9yLnJld3JpdGUnLCAnc291cmNlJywgJ3NvdXJjZS5vcmdhbml6ZUltcG9ydHMnXSxcbiAgICB9KTtcblxuICAgIC8vIC0tLSBOYXZpZ2F0aW9uICYgc3RydWN0dXJlIChQYWNrIDEpIC0tLVxuXG4gICAgbW9uYWNvLmxhbmd1YWdlcy5yZWdpc3RlckltcGxlbWVudGF0aW9uUHJvdmlkZXIoJ2phdmEnLCB7XG4gICAgICAgIHByb3ZpZGVJbXBsZW1lbnRhdGlvbjogKG1vZGVsLCBwb3NpdGlvbikgPT4gcmVxdWVzdExvY2F0aW9ucygndGV4dERvY3VtZW50L2ltcGxlbWVudGF0aW9uJywgbW9kZWwsIHBvc2l0aW9uKSxcbiAgICB9KTtcblxuICAgIG1vbmFjby5sYW5ndWFnZXMucmVnaXN0ZXJUeXBlRGVmaW5pdGlvblByb3ZpZGVyKCdqYXZhJywge1xuICAgICAgICBwcm92aWRlVHlwZURlZmluaXRpb246IChtb2RlbCwgcG9zaXRpb24pID0+IHJlcXVlc3RMb2NhdGlvbnMoJ3RleHREb2N1bWVudC90eXBlRGVmaW5pdGlvbicsIG1vZGVsLCBwb3NpdGlvbiksXG4gICAgfSk7XG5cbiAgICBtb25hY28ubGFuZ3VhZ2VzLnJlZ2lzdGVyRG9jdW1lbnRIaWdobGlnaHRQcm92aWRlcignamF2YScsIHtcbiAgICAgICAgcHJvdmlkZURvY3VtZW50SGlnaGxpZ2h0czogYXN5bmMgKG1vZGVsLCBwb3NpdGlvbikgPT4ge1xuICAgICAgICAgICAgaWYgKCFfY29ubiB8fCAhaXNXb3Jrc3BhY2VGaWxlKG1vZGVsLnVyaS50b1N0cmluZygpKSkgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICBjb25zdCByZXN1bHQ6IGFueVtdIHwgbnVsbCA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCd0ZXh0RG9jdW1lbnQvZG9jdW1lbnRIaWdobGlnaHQnLCB7XG4gICAgICAgICAgICAgICAgdGV4dERvY3VtZW50OiB7IHVyaTogbW9kZWwudXJpLnRvU3RyaW5nKCkgfSxcbiAgICAgICAgICAgICAgICBwb3NpdGlvbjogICAgIHsgbGluZTogcG9zaXRpb24ubGluZU51bWJlciAtIDEsIGNoYXJhY3RlcjogcG9zaXRpb24uY29sdW1uIC0gMSB9LFxuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICBpZiAoIXJlc3VsdCkgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICByZXR1cm4gcmVzdWx0Lm1hcChoID0+ICh7IHJhbmdlOiBsc3BSYW5nZVRvTW9uYWNvKGgucmFuZ2UpLCBraW5kOiBoLmtpbmQgPyBoLmtpbmQgLSAxIDogdW5kZWZpbmVkIH0pKTtcbiAgICAgICAgfSxcbiAgICB9KTtcblxuICAgIG1vbmFjby5sYW5ndWFnZXMucmVnaXN0ZXJEb2N1bWVudFN5bWJvbFByb3ZpZGVyKCdqYXZhJywge1xuICAgICAgICBwcm92aWRlRG9jdW1lbnRTeW1ib2xzOiBhc3luYyAobW9kZWwpID0+IHtcbiAgICAgICAgICAgIGlmICghX2Nvbm4gfHwgIWlzV29ya3NwYWNlRmlsZShtb2RlbC51cmkudG9TdHJpbmcoKSkpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgY29uc3QgcmVzdWx0OiBhbnlbXSB8IG51bGwgPSBhd2FpdCBfY29ubi5zZW5kUmVxdWVzdCgndGV4dERvY3VtZW50L2RvY3VtZW50U3ltYm9sJywge1xuICAgICAgICAgICAgICAgIHRleHREb2N1bWVudDogeyB1cmk6IG1vZGVsLnVyaS50b1N0cmluZygpIH0sXG4gICAgICAgICAgICB9KTtcbiAgICAgICAgICAgIHJldHVybiByZXN1bHQgPyBtYXBEb2N1bWVudFN5bWJvbHMocmVzdWx0KSA6IG51bGw7XG4gICAgICAgIH0sXG4gICAgfSk7XG5cbiAgICBtb25hY28ubGFuZ3VhZ2VzLnJlZ2lzdGVyRm9sZGluZ1JhbmdlUHJvdmlkZXIoJ2phdmEnLCB7XG4gICAgICAgIHByb3ZpZGVGb2xkaW5nUmFuZ2VzOiBhc3luYyAobW9kZWwpID0+IHtcbiAgICAgICAgICAgIGlmICghX2Nvbm4gfHwgIWlzV29ya3NwYWNlRmlsZShtb2RlbC51cmkudG9TdHJpbmcoKSkpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgY29uc3QgcmVzdWx0OiBhbnlbXSB8IG51bGwgPSBhd2FpdCBfY29ubi5zZW5kUmVxdWVzdCgndGV4dERvY3VtZW50L2ZvbGRpbmdSYW5nZScsIHtcbiAgICAgICAgICAgICAgICB0ZXh0RG9jdW1lbnQ6IHsgdXJpOiBtb2RlbC51cmkudG9TdHJpbmcoKSB9LFxuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICBpZiAoIXJlc3VsdCkgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICByZXR1cm4gcmVzdWx0Lm1hcChyID0+ICh7IHN0YXJ0OiByLnN0YXJ0TGluZSArIDEsIGVuZDogci5lbmRMaW5lICsgMSwga2luZDogZm9sZGluZ0tpbmQoci5raW5kKSB9KSk7XG4gICAgICAgIH0sXG4gICAgfSk7XG5cbiAgICBtb25hY28ubGFuZ3VhZ2VzLnJlZ2lzdGVyU2VsZWN0aW9uUmFuZ2VQcm92aWRlcignamF2YScsIHtcbiAgICAgICAgcHJvdmlkZVNlbGVjdGlvblJhbmdlczogYXN5bmMgKG1vZGVsLCBwb3NpdGlvbnMpID0+IHtcbiAgICAgICAgICAgIGlmICghX2Nvbm4gfHwgIWlzV29ya3NwYWNlRmlsZShtb2RlbC51cmkudG9TdHJpbmcoKSkpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgY29uc3QgcmVzdWx0OiBhbnlbXSB8IG51bGwgPSBhd2FpdCBfY29ubi5zZW5kUmVxdWVzdCgndGV4dERvY3VtZW50L3NlbGVjdGlvblJhbmdlJywge1xuICAgICAgICAgICAgICAgIHRleHREb2N1bWVudDogeyB1cmk6IG1vZGVsLnVyaS50b1N0cmluZygpIH0sXG4gICAgICAgICAgICAgICAgcG9zaXRpb25zOiAgICBwb3NpdGlvbnMubWFwKHAgPT4gKHsgbGluZTogcC5saW5lTnVtYmVyIC0gMSwgY2hhcmFjdGVyOiBwLmNvbHVtbiAtIDEgfSkpLFxuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICBpZiAoIXJlc3VsdCkgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICByZXR1cm4gcmVzdWx0Lm1hcChmbGF0dGVuU2VsZWN0aW9uUmFuZ2UpO1xuICAgICAgICB9LFxuICAgIH0pO1xuXG4gICAgbW9uYWNvLmxhbmd1YWdlcy5yZWdpc3RlckRvY3VtZW50UmFuZ2VGb3JtYXR0aW5nRWRpdFByb3ZpZGVyKCdqYXZhJywge1xuICAgICAgICBwcm92aWRlRG9jdW1lbnRSYW5nZUZvcm1hdHRpbmdFZGl0czogYXN5bmMgKG1vZGVsLCByYW5nZSkgPT4ge1xuICAgICAgICAgICAgaWYgKCFfY29ubiB8fCAhaXNXb3Jrc3BhY2VGaWxlKG1vZGVsLnVyaS50b1N0cmluZygpKSkgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICBjb25zdCBlZGl0czogVGV4dEVkaXRbXSB8IG51bGwgPSBhd2FpdCBfY29ubi5zZW5kUmVxdWVzdCgndGV4dERvY3VtZW50L3JhbmdlRm9ybWF0dGluZycsIHtcbiAgICAgICAgICAgICAgICB0ZXh0RG9jdW1lbnQ6IHsgdXJpOiBtb2RlbC51cmkudG9TdHJpbmcoKSB9LFxuICAgICAgICAgICAgICAgIHJhbmdlOiAgICAgICAgbW9uYWNvUmFuZ2VUb0xzcChyYW5nZSksXG4gICAgICAgICAgICAgICAgb3B0aW9uczogICAgICB7IHRhYlNpemU6IG1vZGVsLmdldE9wdGlvbnMoKS50YWJTaXplLCBpbnNlcnRTcGFjZXM6IG1vZGVsLmdldE9wdGlvbnMoKS5pbnNlcnRTcGFjZXMgfSxcbiAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgcmV0dXJuIGVkaXRzID8gZWRpdHMubWFwKHRleHRFZGl0VG9Nb25hY28pIDogbnVsbDtcbiAgICAgICAgfSxcbiAgICB9KTtcblxuICAgIC8vIC0tLSBJbmxheSBoaW50cyArIHNlbWFudGljIGhpZ2hsaWdodGluZyAoUGFjayAyKSAtLS1cblxuICAgIG1vbmFjby5sYW5ndWFnZXMucmVnaXN0ZXJJbmxheUhpbnRzUHJvdmlkZXIoJ2phdmEnLCB7XG4gICAgICAgIHByb3ZpZGVJbmxheUhpbnRzOiBhc3luYyAobW9kZWwsIHJhbmdlKSA9PiB7XG4gICAgICAgICAgICBpZiAoIV9jb25uIHx8ICFpc1dvcmtzcGFjZUZpbGUobW9kZWwudXJpLnRvU3RyaW5nKCkpKSByZXR1cm4gbnVsbDtcbiAgICAgICAgICAgIGNvbnN0IHJlc3VsdDogYW55W10gfCBudWxsID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ3RleHREb2N1bWVudC9pbmxheUhpbnQnLCB7XG4gICAgICAgICAgICAgICAgdGV4dERvY3VtZW50OiB7IHVyaTogbW9kZWwudXJpLnRvU3RyaW5nKCkgfSxcbiAgICAgICAgICAgICAgICByYW5nZTogICAgICAgIG1vbmFjb1JhbmdlVG9Mc3AocmFuZ2UpLFxuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICBpZiAoIXJlc3VsdCkgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICByZXR1cm4ge1xuICAgICAgICAgICAgICAgIGhpbnRzOiByZXN1bHQubWFwKGggPT4gKHtcbiAgICAgICAgICAgICAgICAgICAgcG9zaXRpb246ICAgICB7IGxpbmVOdW1iZXI6IGgucG9zaXRpb24ubGluZSArIDEsIGNvbHVtbjogaC5wb3NpdGlvbi5jaGFyYWN0ZXIgKyAxIH0sXG4gICAgICAgICAgICAgICAgICAgIGxhYmVsOiAgICAgICAgdHlwZW9mIGgubGFiZWwgPT09ICdzdHJpbmcnID8gaC5sYWJlbCA6IChoLmxhYmVsID8/IFtdKS5tYXAoKHA6IGFueSkgPT4gKHsgbGFiZWw6IHAudmFsdWUgfSkpLFxuICAgICAgICAgICAgICAgICAgICBraW5kOiAgICAgICAgIGgua2luZCxcbiAgICAgICAgICAgICAgICAgICAgcGFkZGluZ0xlZnQ6ICBoLnBhZGRpbmdMZWZ0LFxuICAgICAgICAgICAgICAgICAgICBwYWRkaW5nUmlnaHQ6IGgucGFkZGluZ1JpZ2h0LFxuICAgICAgICAgICAgICAgICAgICB0b29sdGlwOiAgICAgIGgudG9vbHRpcCA/IG1hcmt1cFRvU3RyaW5nKGgudG9vbHRpcCkgOiB1bmRlZmluZWQsXG4gICAgICAgICAgICAgICAgfSkpLFxuICAgICAgICAgICAgICAgIGRpc3Bvc2UoKSB7IC8qIG5vdGhpbmcgdG8gZGlzcG9zZSAqLyB9LFxuICAgICAgICAgICAgfTtcbiAgICAgICAgfSxcbiAgICB9KTtcblxuICAgIG1vbmFjby5sYW5ndWFnZXMucmVnaXN0ZXJEb2N1bWVudFNlbWFudGljVG9rZW5zUHJvdmlkZXIoJ2phdmEnLCB7XG4gICAgICAgIGdldExlZ2VuZDogKCkgPT4gX3NlbWFudGljVG9rZW5zTGVnZW5kID8/IHsgdG9rZW5UeXBlczogW10sIHRva2VuTW9kaWZpZXJzOiBbXSB9LFxuICAgICAgICBwcm92aWRlRG9jdW1lbnRTZW1hbnRpY1Rva2VuczogYXN5bmMgKG1vZGVsKSA9PiB7XG4gICAgICAgICAgICBpZiAoIV9jb25uIHx8ICFpc1dvcmtzcGFjZUZpbGUobW9kZWwudXJpLnRvU3RyaW5nKCkpIHx8ICFfc2VtYW50aWNUb2tlbnNMZWdlbmQpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgY29uc3QgcmVzdWx0OiB7IGRhdGE6IG51bWJlcltdOyByZXN1bHRJZD86IHN0cmluZyB9IHwgbnVsbCA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCd0ZXh0RG9jdW1lbnQvc2VtYW50aWNUb2tlbnMvZnVsbCcsIHtcbiAgICAgICAgICAgICAgICB0ZXh0RG9jdW1lbnQ6IHsgdXJpOiBtb2RlbC51cmkudG9TdHJpbmcoKSB9LFxuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICBpZiAoIXJlc3VsdD8uZGF0YSkgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICByZXR1cm4geyBkYXRhOiBuZXcgVWludDMyQXJyYXkocmVzdWx0LmRhdGEpLCByZXN1bHRJZDogcmVzdWx0LnJlc3VsdElkIH07XG4gICAgICAgIH0sXG4gICAgICAgIHJlbGVhc2VEb2N1bWVudFNlbWFudGljVG9rZW5zOiAoKSA9PiB7IC8qIG5vdGhpbmcgdG8gcmVsZWFzZSAqLyB9LFxuICAgIH0pO1xuXG4gICAgLy8gLS0tIENvZGVMZW5zIChQYWNrIDMpIC0tLVxuXG4gICAgbW9uYWNvLmxhbmd1YWdlcy5yZWdpc3RlckNvZGVMZW5zUHJvdmlkZXIoJ2phdmEnLCB7XG4gICAgICAgIHByb3ZpZGVDb2RlTGVuc2VzOiBhc3luYyAobW9kZWwpID0+IHtcbiAgICAgICAgICAgIGlmICghX2Nvbm4gfHwgIWlzV29ya3NwYWNlRmlsZShtb2RlbC51cmkudG9TdHJpbmcoKSkpIHJldHVybiB7IGxlbnNlczogW10sIGRpc3Bvc2UoKSB7IC8qICovIH0gfTtcbiAgICAgICAgICAgIGNvbnN0IHJlc3VsdDogYW55W10gfCBudWxsID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ3RleHREb2N1bWVudC9jb2RlTGVucycsIHtcbiAgICAgICAgICAgICAgICB0ZXh0RG9jdW1lbnQ6IHsgdXJpOiBtb2RlbC51cmkudG9TdHJpbmcoKSB9LFxuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICBjb25zdCBsZW5zZXMgPSAocmVzdWx0ID8/IFtdKS5tYXAoKGxlbnMsIGkpID0+ICh7XG4gICAgICAgICAgICAgICAgcmFuZ2U6ICAgIGxzcFJhbmdlVG9Nb25hY28obGVucy5yYW5nZSksXG4gICAgICAgICAgICAgICAgaWQ6ICAgICAgIFN0cmluZyhpKSxcbiAgICAgICAgICAgICAgICBjb21tYW5kOiAgbGVucy5jb21tYW5kID8gbWFwTGVuc0NvbW1hbmQobGVucy5jb21tYW5kKSA6IHVuZGVmaW5lZCxcbiAgICAgICAgICAgICAgICBfbHNwOiAgICAgbGVucyxcbiAgICAgICAgICAgIH0pKTtcbiAgICAgICAgICAgIHJldHVybiB7IGxlbnNlcywgZGlzcG9zZSgpIHsgLyogKi8gfSB9O1xuICAgICAgICB9LFxuICAgICAgICByZXNvbHZlQ29kZUxlbnM6IGFzeW5jIChfbW9kZWwsIGNvZGVMZW5zKSA9PiB7XG4gICAgICAgICAgICBjb25zdCBsc3AgPSAoY29kZUxlbnMgYXMgYW55KS5fbHNwO1xuICAgICAgICAgICAgaWYgKF9jb25uICYmIGxzcCAmJiAhbHNwLmNvbW1hbmQpIHtcbiAgICAgICAgICAgICAgICB0cnkge1xuICAgICAgICAgICAgICAgICAgICBjb25zdCByZXNvbHZlZDogYW55ID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ2NvZGVMZW5zL3Jlc29sdmUnLCBsc3ApO1xuICAgICAgICAgICAgICAgICAgICBjb2RlTGVucy5jb21tYW5kID0gcmVzb2x2ZWQ/LmNvbW1hbmQgPyBtYXBMZW5zQ29tbWFuZChyZXNvbHZlZC5jb21tYW5kKSA6IHsgaWQ6IE5PT1BfQ09NTUFORCwgdGl0bGU6ICcnIH07XG4gICAgICAgICAgICAgICAgfSBjYXRjaCB7XG4gICAgICAgICAgICAgICAgICAgIGNvZGVMZW5zLmNvbW1hbmQgPSB7IGlkOiBOT09QX0NPTU1BTkQsIHRpdGxlOiAnJyB9O1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJldHVybiBjb2RlTGVucztcbiAgICAgICAgfSxcbiAgICB9KTtcblxuICAgIC8vIC0tLSBKYXZhIGtleXdvcmQgY29tcGxldGlvbiAoUGFjayAxYik6IGFsd2F5cy1hdmFpbGFibGUsIHJhbmtlZCBiZWxvdyBTREsvTFNQIHJlc3VsdHMgLS0tXG5cbiAgICBtb25hY28ubGFuZ3VhZ2VzLnJlZ2lzdGVyQ29tcGxldGlvbkl0ZW1Qcm92aWRlcignamF2YScsIHtcbiAgICAgICAgcHJvdmlkZUNvbXBsZXRpb25JdGVtczogKG1vZGVsLCBwb3NpdGlvbikgPT4ge1xuICAgICAgICAgICAgaWYgKCFpc1dvcmtzcGFjZUZpbGUobW9kZWwudXJpLnRvU3RyaW5nKCkpKSByZXR1cm4gbnVsbDtcbiAgICAgICAgICAgIGNvbnN0IHdvcmQgPSBtb2RlbC5nZXRXb3JkVW50aWxQb3NpdGlvbihwb3NpdGlvbik7XG4gICAgICAgICAgICBjb25zdCByYW5nZTogbW9uYWNvLklSYW5nZSA9IHtcbiAgICAgICAgICAgICAgICBzdGFydExpbmVOdW1iZXI6IHBvc2l0aW9uLmxpbmVOdW1iZXIsIHN0YXJ0Q29sdW1uOiB3b3JkLnN0YXJ0Q29sdW1uLFxuICAgICAgICAgICAgICAgIGVuZExpbmVOdW1iZXI6ICAgcG9zaXRpb24ubGluZU51bWJlciwgZW5kQ29sdW1uOiAgIHdvcmQuZW5kQ29sdW1uLFxuICAgICAgICAgICAgfTtcbiAgICAgICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICAgICAgc3VnZ2VzdGlvbnM6IEpBVkFfS0VZV09SRFMubWFwKGtleXdvcmQgPT4gKHtcbiAgICAgICAgICAgICAgICAgICAgbGFiZWw6ICAgICAga2V5d29yZCxcbiAgICAgICAgICAgICAgICAgICAga2luZDogICAgICAgbW9uYWNvLmxhbmd1YWdlcy5Db21wbGV0aW9uSXRlbUtpbmQuS2V5d29yZCxcbiAgICAgICAgICAgICAgICAgICAgaW5zZXJ0VGV4dDoga2V5d29yZCxcbiAgICAgICAgICAgICAgICAgICAgcmFuZ2UsXG4gICAgICAgICAgICAgICAgICAgIHNvcnRUZXh0OiAgIGA5XyR7a2V5d29yZH1gLFxuICAgICAgICAgICAgICAgIH0pKSxcbiAgICAgICAgICAgIH07XG4gICAgICAgIH0sXG4gICAgfSk7XG59XG5cbi8vIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbi8vIEhlbHBlcnNcbi8vIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cblxuZnVuY3Rpb24gbHNwU2V2ZXJpdHkoc2V2ZXJpdHk6IERpYWdub3N0aWNTZXZlcml0eSB8IHVuZGVmaW5lZCk6IG1vbmFjby5NYXJrZXJTZXZlcml0eSB7XG4gICAgc3dpdGNoIChzZXZlcml0eSkge1xuICAgICAgICBjYXNlIERpYWdub3N0aWNTZXZlcml0eS5FcnJvcjogICAgICAgcmV0dXJuIG1vbmFjby5NYXJrZXJTZXZlcml0eS5FcnJvcjtcbiAgICAgICAgY2FzZSBEaWFnbm9zdGljU2V2ZXJpdHkuV2FybmluZzogICAgIHJldHVybiBtb25hY28uTWFya2VyU2V2ZXJpdHkuV2FybmluZztcbiAgICAgICAgY2FzZSBEaWFnbm9zdGljU2V2ZXJpdHkuSW5mb3JtYXRpb246IHJldHVybiBtb25hY28uTWFya2VyU2V2ZXJpdHkuSW5mbztcbiAgICAgICAgY2FzZSBEaWFnbm9zdGljU2V2ZXJpdHkuSGludDogICAgICAgIHJldHVybiBtb25hY28uTWFya2VyU2V2ZXJpdHkuSGludDtcbiAgICAgICAgZGVmYXVsdDogICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiBtb25hY28uTWFya2VyU2V2ZXJpdHkuRXJyb3I7XG4gICAgfVxufVxuXG5mdW5jdGlvbiBsc3BSYW5nZVRvTW9uYWNvKHI6IHsgc3RhcnQ6IHsgbGluZTogbnVtYmVyOyBjaGFyYWN0ZXI6IG51bWJlciB9OyBlbmQ6IHsgbGluZTogbnVtYmVyOyBjaGFyYWN0ZXI6IG51bWJlciB9IH0pOiBtb25hY28uSVJhbmdlIHtcbiAgICByZXR1cm4ge1xuICAgICAgICBzdGFydExpbmVOdW1iZXI6IHIuc3RhcnQubGluZSArIDEsXG4gICAgICAgIHN0YXJ0Q29sdW1uOiAgICAgci5zdGFydC5jaGFyYWN0ZXIgKyAxLFxuICAgICAgICBlbmRMaW5lTnVtYmVyOiAgIHIuZW5kLmxpbmUgKyAxLFxuICAgICAgICBlbmRDb2x1bW46ICAgICAgIHIuZW5kLmNoYXJhY3RlciArIDEsXG4gICAgfTtcbn1cblxuZnVuY3Rpb24gbWFya3VwVG9TdHJpbmcoYzogc3RyaW5nIHwgTWFya3VwQ29udGVudCk6IHN0cmluZyB7XG4gICAgcmV0dXJuIHR5cGVvZiBjID09PSAnc3RyaW5nJyA/IGMgOiBjLnZhbHVlO1xufVxuXG5mdW5jdGlvbiBsc3BDb21wbGV0aW9uS2luZChraW5kOiBDb21wbGV0aW9uSXRlbUtpbmQgfCB1bmRlZmluZWQpOiBtb25hY28ubGFuZ3VhZ2VzLkNvbXBsZXRpb25JdGVtS2luZCB7XG4gICAgY29uc3QgSyA9IG1vbmFjby5sYW5ndWFnZXMuQ29tcGxldGlvbkl0ZW1LaW5kO1xuICAgIHN3aXRjaCAoa2luZCkge1xuICAgICAgICBjYXNlIENvbXBsZXRpb25JdGVtS2luZC5UZXh0OiAgICAgICAgICByZXR1cm4gSy5UZXh0O1xuICAgICAgICBjYXNlIENvbXBsZXRpb25JdGVtS2luZC5NZXRob2Q6ICAgICAgICByZXR1cm4gSy5NZXRob2Q7XG4gICAgICAgIGNhc2UgQ29tcGxldGlvbkl0ZW1LaW5kLkZ1bmN0aW9uOiAgICAgIHJldHVybiBLLkZ1bmN0aW9uO1xuICAgICAgICBjYXNlIENvbXBsZXRpb25JdGVtS2luZC5Db25zdHJ1Y3RvcjogICByZXR1cm4gSy5Db25zdHJ1Y3RvcjtcbiAgICAgICAgY2FzZSBDb21wbGV0aW9uSXRlbUtpbmQuRmllbGQ6ICAgICAgICAgcmV0dXJuIEsuRmllbGQ7XG4gICAgICAgIGNhc2UgQ29tcGxldGlvbkl0ZW1LaW5kLlZhcmlhYmxlOiAgICAgIHJldHVybiBLLlZhcmlhYmxlO1xuICAgICAgICBjYXNlIENvbXBsZXRpb25JdGVtS2luZC5DbGFzczogICAgICAgICByZXR1cm4gSy5DbGFzcztcbiAgICAgICAgY2FzZSBDb21wbGV0aW9uSXRlbUtpbmQuSW50ZXJmYWNlOiAgICAgcmV0dXJuIEsuSW50ZXJmYWNlO1xuICAgICAgICBjYXNlIENvbXBsZXRpb25JdGVtS2luZC5Nb2R1bGU6ICAgICAgICByZXR1cm4gSy5Nb2R1bGU7XG4gICAgICAgIGNhc2UgQ29tcGxldGlvbkl0ZW1LaW5kLlByb3BlcnR5OiAgICAgIHJldHVybiBLLlByb3BlcnR5O1xuICAgICAgICBjYXNlIENvbXBsZXRpb25JdGVtS2luZC5LZXl3b3JkOiAgICAgICByZXR1cm4gSy5LZXl3b3JkO1xuICAgICAgICBjYXNlIENvbXBsZXRpb25JdGVtS2luZC5TbmlwcGV0OiAgICAgICByZXR1cm4gSy5TbmlwcGV0O1xuICAgICAgICBjYXNlIENvbXBsZXRpb25JdGVtS2luZC5Db25zdGFudDogICAgICByZXR1cm4gSy5Db25zdGFudDtcbiAgICAgICAgY2FzZSBDb21wbGV0aW9uSXRlbUtpbmQuU3RydWN0OiAgICAgICAgcmV0dXJuIEsuU3RydWN0O1xuICAgICAgICBjYXNlIENvbXBsZXRpb25JdGVtS2luZC5UeXBlUGFyYW1ldGVyOiByZXR1cm4gSy5UeXBlUGFyYW1ldGVyO1xuICAgICAgICBkZWZhdWx0OiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICByZXR1cm4gSy5UZXh0O1xuICAgIH1cbn1cblxuZnVuY3Rpb24gbHNwQ29tcGxldGlvblRvTW9uYWNvKFxuICAgIGl0ZW06IENvbXBsZXRpb25JdGVtLFxuICAgIG1vZGVsOiBtb25hY28uZWRpdG9yLklUZXh0TW9kZWwsXG4gICAgcG9zaXRpb246IG1vbmFjby5Qb3NpdGlvbixcbik6IE1vbmFjb0NvbXBsZXRpb25JdGVtIHtcbiAgICBjb25zdCB3b3JkID0gbW9kZWwuZ2V0V29yZFVudGlsUG9zaXRpb24ocG9zaXRpb24pO1xuICAgIGxldCByYW5nZTogbW9uYWNvLklSYW5nZSA9IHtcbiAgICAgICAgc3RhcnRMaW5lTnVtYmVyOiBwb3NpdGlvbi5saW5lTnVtYmVyLFxuICAgICAgICBzdGFydENvbHVtbjogICAgIHdvcmQuc3RhcnRDb2x1bW4sXG4gICAgICAgIGVuZExpbmVOdW1iZXI6ICAgcG9zaXRpb24ubGluZU51bWJlcixcbiAgICAgICAgZW5kQ29sdW1uOiAgICAgICB3b3JkLmVuZENvbHVtbixcbiAgICB9O1xuICAgIGxldCBpbnNlcnRUZXh0ID0gaXRlbS5pbnNlcnRUZXh0ID8/IGl0ZW0ubGFiZWw7XG4gICAgY29uc3QgdGV4dEVkaXQgPSBpdGVtLnRleHRFZGl0IGFzIHsgcmFuZ2U/OiBMc3BSYW5nZTsgcmVwbGFjZT86IExzcFJhbmdlOyBpbnNlcnQ/OiBMc3BSYW5nZTsgbmV3VGV4dD86IHN0cmluZyB9IHwgdW5kZWZpbmVkO1xuICAgIGlmICh0ZXh0RWRpdCkge1xuICAgICAgICBjb25zdCByID0gdGV4dEVkaXQucmFuZ2UgPz8gdGV4dEVkaXQucmVwbGFjZSA/PyB0ZXh0RWRpdC5pbnNlcnQ7XG4gICAgICAgIGlmIChyKSByYW5nZSA9IGxzcFJhbmdlVG9Nb25hY28ocik7XG4gICAgICAgIGlmICh0eXBlb2YgdGV4dEVkaXQubmV3VGV4dCA9PT0gJ3N0cmluZycpIGluc2VydFRleHQgPSB0ZXh0RWRpdC5uZXdUZXh0O1xuICAgIH1cbiAgICBjb25zdCByZXN1bHQ6IE1vbmFjb0NvbXBsZXRpb25JdGVtID0ge1xuICAgICAgICBsYWJlbDogICAgICAgICAgIGl0ZW0ubGFiZWwsXG4gICAgICAgIGtpbmQ6ICAgICAgICAgICAgbHNwQ29tcGxldGlvbktpbmQoaXRlbS5raW5kKSxcbiAgICAgICAgZGV0YWlsOiAgICAgICAgICBpdGVtLmRldGFpbCxcbiAgICAgICAgZG9jdW1lbnRhdGlvbjogICBpdGVtLmRvY3VtZW50YXRpb24gPyB7IHZhbHVlOiBtYXJrdXBUb1N0cmluZyhpdGVtLmRvY3VtZW50YXRpb24pLCBpc1RydXN0ZWQ6IGZhbHNlIH0gOiB1bmRlZmluZWQsXG4gICAgICAgIGluc2VydFRleHQsXG4gICAgICAgIGluc2VydFRleHRSdWxlczogaXRlbS5pbnNlcnRUZXh0Rm9ybWF0ID09PSBJbnNlcnRUZXh0Rm9ybWF0LlNuaXBwZXRcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgPyBtb25hY28ubGFuZ3VhZ2VzLkNvbXBsZXRpb25JdGVtSW5zZXJ0VGV4dFJ1bGUuSW5zZXJ0QXNTbmlwcGV0XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgIDogdW5kZWZpbmVkLFxuICAgICAgICByYW5nZSxcbiAgICAgICAgc29ydFRleHQ6ICAgICAgICAgICAgc2RrUHJpb3JpdGlzZWRTb3J0VGV4dChpdGVtKSxcbiAgICAgICAgZmlsdGVyVGV4dDogICAgICAgICAgaXRlbS5maWx0ZXJUZXh0LFxuICAgICAgICBwcmVzZWxlY3Q6ICAgICAgICAgICBpdGVtLnByZXNlbGVjdCxcbiAgICAgICAgY29tbWl0Q2hhcmFjdGVyczogICAgaXRlbS5jb21taXRDaGFyYWN0ZXJzLFxuICAgICAgICBhZGRpdGlvbmFsVGV4dEVkaXRzOiBpdGVtLmFkZGl0aW9uYWxUZXh0RWRpdHM/Lm1hcCh0ZXh0RWRpdFRvTW9uYWNvKSxcbiAgICB9O1xuICAgIHJlc3VsdC5fbHNwID0gaXRlbTtcbiAgICByZXR1cm4gcmVzdWx0O1xufVxuXG4vKipcbiAqIFJhbmtzIERpcmlnaWJsZSBTREsgc3VnZ2VzdGlvbnMgKHtAY29kZSBvcmcuZWNsaXBzZS5kaXJpZ2libGUuc2RrLip9KSBhYm92ZSBldmVyeXRoaW5nIGVsc2UgYnlcbiAqIHByZWZpeGluZyB0aGUgc2VydmVyIHNvcnRUZXh0IHdpdGggYSBwcmlvcml0eSBidWNrZXQsIHByZXNlcnZpbmcgdGhlIHNlcnZlciBvcmRlciB3aXRoaW4gZWFjaCBidWNrZXQuXG4gKi9cbmZ1bmN0aW9uIHNka1ByaW9yaXRpc2VkU29ydFRleHQoaXRlbTogQ29tcGxldGlvbkl0ZW0pOiBzdHJpbmcge1xuICAgIGNvbnN0IGJhc2UgPSBpdGVtLnNvcnRUZXh0ID8/ICh0eXBlb2YgaXRlbS5sYWJlbCA9PT0gJ3N0cmluZycgPyBpdGVtLmxhYmVsIDogJycpO1xuICAgIGNvbnN0IGRlc2NyaXB0aW9uID0gKGl0ZW0ubGFiZWxEZXRhaWxzICYmIHR5cGVvZiBpdGVtLmxhYmVsRGV0YWlscy5kZXNjcmlwdGlvbiA9PT0gJ3N0cmluZycpXG4gICAgICAgID8gaXRlbS5sYWJlbERldGFpbHMuZGVzY3JpcHRpb24gOiAnJztcbiAgICBjb25zdCBoYXlzdGFjayA9IGAke2l0ZW0uZGV0YWlsID8/ICcnfSAke2Rlc2NyaXB0aW9ufWA7XG4gICAgcmV0dXJuIGhheXN0YWNrLmluY2x1ZGVzKCdvcmcuZWNsaXBzZS5kaXJpZ2libGUuc2RrJykgPyBgMF8ke2Jhc2V9YCA6IGAxXyR7YmFzZX1gO1xufVxuXG4vLyAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4vLyBDb2RlIGFjdGlvbnMsIGNvbW1hbmRzLCByZWZhY3Rvci9nZW5lcmF0ZSwgd29ya3NwYWNlIGVkaXRzXG4vLyAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG5cbi8qKiBNb25hY28gY29tbWFuZCBpZCB1c2VkIHRvIGFwcGx5IGEgZGVmZXJyZWQgTFNQIGNvZGUgYWN0aW9uIHdoZW4gdGhlIHVzZXIgc2VsZWN0cyBpdC4gKi9cbmNvbnN0IEFQUExZX0FDVElPTl9DT01NQU5EID0gJ2RpcmlnaWJsZS5qYXZhLmFwcGx5Q29kZUFjdGlvbic7XG5cbi8qKiBWaXJ0dWFsIFVSSSBwcmVmaXggb2YgZWRpdG9yIG1vZGVsczsgc3RyaXBwaW5nIGl0IHlpZWxkcyB0aGUgSURFIHdvcmtzcGFjZSBwYXRoICgvd3MvcHJvai8uLi4pLiAqL1xuY29uc3QgVklSVFVBTF9GSUxFX1BSRUZJWCA9ICdmaWxlOi8vL3dvcmtzcGFjZSc7XG5cbi8qKiBNb25hY28gY29tbWFuZCBpZCB1c2VkIGZvciBkaXNwbGF5LW9ubHkgQ29kZUxlbnNlcyAoZS5nLiBhIHJlZmVyZW5jZSBjb3VudCB3aXRoIG5vIHJlc29sdmVkIGFjdGlvbikuICovXG5jb25zdCBOT09QX0NPTU1BTkQgPSAnZGlyaWdpYmxlLmphdmEubm9vcExlbnMnO1xuXG4vKiogSmF2YSBTRSBrZXl3b3Jkcy9saXRlcmFscyBvZmZlcmVkIGFzIGxvdy1wcmlvcml0eSBjb21wbGV0aW9uIHNvIHRoZXkgYWx3YXlzIGFwcGVhciB3aGlsZSB0eXBpbmcuICovXG5jb25zdCBKQVZBX0tFWVdPUkRTID0gWydhYnN0cmFjdCcsICdhc3NlcnQnLCAnYm9vbGVhbicsICdicmVhaycsICdieXRlJywgJ2Nhc2UnLCAnY2F0Y2gnLCAnY2hhcicsICdjbGFzcycsXG4gICAgJ2NvbnN0JywgJ2NvbnRpbnVlJywgJ2RlZmF1bHQnLCAnZG8nLCAnZG91YmxlJywgJ2Vsc2UnLCAnZW51bScsICdleHRlbmRzJywgJ2ZpbmFsJywgJ2ZpbmFsbHknLCAnZmxvYXQnLFxuICAgICdmb3InLCAnZ290bycsICdpZicsICdpbXBsZW1lbnRzJywgJ2ltcG9ydCcsICdpbnN0YW5jZW9mJywgJ2ludCcsICdpbnRlcmZhY2UnLCAnbG9uZycsICduYXRpdmUnLCAnbmV3JyxcbiAgICAncGFja2FnZScsICdwcml2YXRlJywgJ3Byb3RlY3RlZCcsICdwdWJsaWMnLCAncmV0dXJuJywgJ3Nob3J0JywgJ3N0YXRpYycsICdzdHJpY3RmcCcsICdzdXBlcicsICdzd2l0Y2gnLFxuICAgICdzeW5jaHJvbml6ZWQnLCAndGhpcycsICd0aHJvdycsICd0aHJvd3MnLCAndHJhbnNpZW50JywgJ3RyeScsICd2b2lkJywgJ3ZvbGF0aWxlJywgJ3doaWxlJywgJ3ZhcicsXG4gICAgJ3lpZWxkJywgJ3JlY29yZCcsICdzZWFsZWQnLCAncGVybWl0cycsICd0cnVlJywgJ2ZhbHNlJywgJ251bGwnXTtcblxuLyoqIFNoYXJlZCBkZWZpbml0aW9uLXN0eWxlIGxvY2F0aW9uIHJlcXVlc3QgdXNlZCBieSBnby10by1kZWZpbml0aW9uL2ltcGxlbWVudGF0aW9uL3R5cGUtZGVmaW5pdGlvbi4gKi9cbmFzeW5jIGZ1bmN0aW9uIHJlcXVlc3RMb2NhdGlvbnMobWV0aG9kOiBzdHJpbmcsIG1vZGVsOiBtb25hY28uZWRpdG9yLklUZXh0TW9kZWwsIHBvc2l0aW9uOiBtb25hY28uUG9zaXRpb24pIHtcbiAgICBpZiAoIV9jb25uIHx8ICFpc1dvcmtzcGFjZUZpbGUobW9kZWwudXJpLnRvU3RyaW5nKCkpKSByZXR1cm4gbnVsbDtcbiAgICBjb25zdCByZXN1bHQ6IExvY2F0aW9uIHwgTG9jYXRpb25bXSB8IG51bGwgPSBhd2FpdCBfY29ubi5zZW5kUmVxdWVzdChtZXRob2QsIHtcbiAgICAgICAgdGV4dERvY3VtZW50OiB7IHVyaTogbW9kZWwudXJpLnRvU3RyaW5nKCkgfSxcbiAgICAgICAgcG9zaXRpb246ICAgICB7IGxpbmU6IHBvc2l0aW9uLmxpbmVOdW1iZXIgLSAxLCBjaGFyYWN0ZXI6IHBvc2l0aW9uLmNvbHVtbiAtIDEgfSxcbiAgICB9KTtcbiAgICBpZiAoIXJlc3VsdCkgcmV0dXJuIG51bGw7XG4gICAgY29uc3QgbG9jYXRpb25zID0gKEFycmF5LmlzQXJyYXkocmVzdWx0KSA/IHJlc3VsdCA6IFtyZXN1bHRdKS5tYXAobG9jID0+ICh7IHVyaTogbW9uYWNvLlVyaS5wYXJzZShsb2MudXJpKSwgcmFuZ2U6IGxzcFJhbmdlVG9Nb25hY28obG9jLnJhbmdlKSB9KSk7XG4gICAgYXdhaXQgZW5zdXJlTW9kZWxzRm9yTG9jYXRpb25zKGxvY2F0aW9ucyk7XG4gICAgcmV0dXJuIGxvY2F0aW9ucztcbn1cblxuLyoqXG4gKiBDcmVhdGVzIGluLW1lbW9yeSBNb25hY28gbW9kZWxzIGZvciB0aGUgd29ya3NwYWNlIGZpbGVzIHJlZmVyZW5jZWQgYnkgdGhlIGdpdmVuIGxvY2F0aW9ucyAoZmV0Y2hlZFxuICogb3ZlciBSRVNUKSBzbyB0aGUgcmVmZXJlbmNlcyAvIHBlZWsgd2lkZ2V0cyBjYW4gcmVuZGVyIGEgY29kZSBwcmV2aWV3IFx1MjAxNCBNb25hY28gY2FuIG9ubHkgcHJldmlldyBmaWxlc1xuICogaXQgaGFzIGEgbW9kZWwgZm9yLCBhbmQgdGhpcyBzaW5nbGUtZmlsZSBlZGl0b3Igb3RoZXJ3aXNlIGhhcyBub25lIGZvciBvdGhlciBmaWxlcy5cbiAqL1xuYXN5bmMgZnVuY3Rpb24gZW5zdXJlTW9kZWxzRm9yTG9jYXRpb25zKGxvY2F0aW9uczogQXJyYXk8eyB1cmk6IG1vbmFjby5VcmkgfT4pOiBQcm9taXNlPHZvaWQ+IHtcbiAgICBjb25zdCBzZWVuID0gbmV3IFNldDxzdHJpbmc+KCk7XG4gICAgYXdhaXQgUHJvbWlzZS5hbGwobG9jYXRpb25zLm1hcChhc3luYyAoeyB1cmkgfSkgPT4ge1xuICAgICAgICBjb25zdCB1cmlTdHIgPSB1cmkudG9TdHJpbmcoKTtcbiAgICAgICAgaWYgKHNlZW4uaGFzKHVyaVN0cikgfHwgIXVyaVN0ci5zdGFydHNXaXRoKFZJUlRVQUxfRklMRV9QUkVGSVgpIHx8IG1vbmFjby5lZGl0b3IuZ2V0TW9kZWwodXJpKSkgcmV0dXJuO1xuICAgICAgICBzZWVuLmFkZCh1cmlTdHIpO1xuICAgICAgICB0cnkge1xuICAgICAgICAgICAgY29uc3QgdGV4dCA9IGF3YWl0IGZldGNoV29ya3NwYWNlRmlsZVRleHQodXJpVG9Xb3Jrc3BhY2VQYXRoKHVyaVN0cikgPz8gdXJpU3RyKTtcbiAgICAgICAgICAgIGlmICghbW9uYWNvLmVkaXRvci5nZXRNb2RlbCh1cmkpKSBtb25hY28uZWRpdG9yLmNyZWF0ZU1vZGVsKHRleHQsICdqYXZhJywgdXJpKTtcbiAgICAgICAgfSBjYXRjaCB7XG4gICAgICAgICAgICAvLyBwcmV2aWV3IGp1c3Qgd29uJ3QgcmVuZGVyIGZvciB0aGlzIG9uZVxuICAgICAgICB9XG4gICAgfSkpO1xufVxuXG4vKiogUmVjdXJzaXZlbHkgbWFwcyBMU1AgaGllcmFyY2hpY2FsIERvY3VtZW50U3ltYm9scyB0byBNb25hY28ncyBzaGFwZSAoa2luZHMgYXJlIDEtYmFzZWQgdnMgMC1iYXNlZCkuICovXG5mdW5jdGlvbiBtYXBEb2N1bWVudFN5bWJvbHMoc3ltYm9sczogYW55W10pOiBtb25hY28ubGFuZ3VhZ2VzLkRvY3VtZW50U3ltYm9sW10ge1xuICAgIHJldHVybiAoc3ltYm9scyA/PyBbXSkubWFwKHMgPT4gKHtcbiAgICAgICAgbmFtZTogICAgICAgICAgIHMubmFtZSxcbiAgICAgICAgZGV0YWlsOiAgICAgICAgIHMuZGV0YWlsID8/ICcnLFxuICAgICAgICBraW5kOiAgICAgICAgICAgKHMua2luZCA/PyAxKSAtIDEsXG4gICAgICAgIHRhZ3M6ICAgICAgICAgICBzLnRhZ3MgPz8gW10sXG4gICAgICAgIHJhbmdlOiAgICAgICAgICBsc3BSYW5nZVRvTW9uYWNvKHMucmFuZ2UpLFxuICAgICAgICBzZWxlY3Rpb25SYW5nZTogbHNwUmFuZ2VUb01vbmFjbyhzLnNlbGVjdGlvblJhbmdlID8/IHMucmFuZ2UpLFxuICAgICAgICBjaGlsZHJlbjogICAgICAgcy5jaGlsZHJlbiA/IG1hcERvY3VtZW50U3ltYm9scyhzLmNoaWxkcmVuKSA6IFtdLFxuICAgIH0pKTtcbn1cblxuZnVuY3Rpb24gZm9sZGluZ0tpbmQoa2luZDogc3RyaW5nIHwgdW5kZWZpbmVkKTogbW9uYWNvLmxhbmd1YWdlcy5Gb2xkaW5nUmFuZ2VLaW5kIHwgdW5kZWZpbmVkIHtcbiAgICBjb25zdCBGSyA9IG1vbmFjby5sYW5ndWFnZXMuRm9sZGluZ1JhbmdlS2luZDtcbiAgICBzd2l0Y2ggKGtpbmQpIHtcbiAgICAgICAgY2FzZSAnY29tbWVudCc6IHJldHVybiBGSy5Db21tZW50O1xuICAgICAgICBjYXNlICdpbXBvcnRzJzogcmV0dXJuIEZLLkltcG9ydHM7XG4gICAgICAgIGNhc2UgJ3JlZ2lvbic6ICByZXR1cm4gRksuUmVnaW9uO1xuICAgICAgICBkZWZhdWx0OiAgICAgICAgcmV0dXJuIHVuZGVmaW5lZDtcbiAgICB9XG59XG5cbi8qKiBGbGF0dGVucyBhbiBMU1AgU2VsZWN0aW9uUmFuZ2UgcGFyZW50LWNoYWluIGludG8gTW9uYWNvJ3MgaW5uZXJtb3N0LXRvLW91dGVybW9zdCBhcnJheS4gKi9cbmZ1bmN0aW9uIGZsYXR0ZW5TZWxlY3Rpb25SYW5nZShzZWxlY3Rpb25SYW5nZTogYW55KTogbW9uYWNvLmxhbmd1YWdlcy5TZWxlY3Rpb25SYW5nZVtdIHtcbiAgICBjb25zdCByYW5nZXM6IG1vbmFjby5sYW5ndWFnZXMuU2VsZWN0aW9uUmFuZ2VbXSA9IFtdO1xuICAgIGxldCBjdXJyZW50ID0gc2VsZWN0aW9uUmFuZ2U7XG4gICAgd2hpbGUgKGN1cnJlbnQpIHtcbiAgICAgICAgcmFuZ2VzLnB1c2goeyByYW5nZTogbHNwUmFuZ2VUb01vbmFjbyhjdXJyZW50LnJhbmdlKSB9KTtcbiAgICAgICAgY3VycmVudCA9IGN1cnJlbnQucGFyZW50O1xuICAgIH1cbiAgICByZXR1cm4gcmFuZ2VzO1xufVxuXG4vKiogTWFwcyBhIEpEVC5MUyBDb2RlTGVucyBjb21tYW5kIHRvIGEgTW9uYWNvIGNvbW1hbmQsIHdpcmluZyB0aGUgcmVmZXJlbmNlcy9pbXBsZW1lbnRhdGlvbnMgcGVlay4gKi9cbmZ1bmN0aW9uIG1hcExlbnNDb21tYW5kKGNtZDogYW55KTogbW9uYWNvLmxhbmd1YWdlcy5Db21tYW5kIHtcbiAgICBjb25zdCBhcmdzID0gY21kLmFyZ3VtZW50cyA/PyBbXTtcbiAgICBpZiAoKGNtZC5jb21tYW5kID09PSAnamF2YS5zaG93LnJlZmVyZW5jZXMnIHx8IGNtZC5jb21tYW5kID09PSAnamF2YS5zaG93LmltcGxlbWVudGF0aW9ucycpICYmIGFyZ3MubGVuZ3RoID49IDMpIHtcbiAgICAgICAgY29uc3QgbG9jYXRpb25zID0gKGFyZ3NbMl0gPz8gW10pLm1hcCgobDogYW55KSA9PiAoeyB1cmk6IG1vbmFjby5VcmkucGFyc2UobC51cmkpLCByYW5nZTogbHNwUmFuZ2VUb01vbmFjbyhsLnJhbmdlKSB9KSk7XG4gICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICBpZDogICAgICAgICdlZGl0b3IuYWN0aW9uLnNob3dSZWZlcmVuY2VzJyxcbiAgICAgICAgICAgIHRpdGxlOiAgICAgY21kLnRpdGxlLFxuICAgICAgICAgICAgYXJndW1lbnRzOiBbbW9uYWNvLlVyaS5wYXJzZShhcmdzWzBdKSwgeyBsaW5lTnVtYmVyOiBhcmdzWzFdLmxpbmUgKyAxLCBjb2x1bW46IGFyZ3NbMV0uY2hhcmFjdGVyICsgMSB9LCBsb2NhdGlvbnNdLFxuICAgICAgICB9O1xuICAgIH1cbiAgICByZXR1cm4geyBpZDogTk9PUF9DT01NQU5ELCB0aXRsZTogY21kLnRpdGxlIH07XG59XG5cbnR5cGUgTHNwUmFuZ2UgPSB7IHN0YXJ0OiB7IGxpbmU6IG51bWJlcjsgY2hhcmFjdGVyOiBudW1iZXIgfTsgZW5kOiB7IGxpbmU6IG51bWJlcjsgY2hhcmFjdGVyOiBudW1iZXIgfSB9O1xuXG4vKiogTW9uYWNvIGNvbXBsZXRpb24gaXRlbSBjYXJyeWluZyB0aGUgb3JpZ2luYXRpbmcgTFNQIGl0ZW0gc28gcmVzb2x2ZSBjYW4gZmV0Y2ggaXRzIGltcG9ydCBlZGl0cy4gKi9cbnR5cGUgTW9uYWNvQ29tcGxldGlvbkl0ZW0gPSBtb25hY28ubGFuZ3VhZ2VzLkNvbXBsZXRpb25JdGVtICYgeyBfbHNwPzogQ29tcGxldGlvbkl0ZW0gfTtcblxuZnVuY3Rpb24gdGV4dEVkaXRUb01vbmFjbyhlZGl0OiBUZXh0RWRpdCk6IG1vbmFjby5sYW5ndWFnZXMuVGV4dEVkaXQge1xuICAgIHJldHVybiB7IHJhbmdlOiBsc3BSYW5nZVRvTW9uYWNvKGVkaXQucmFuZ2UpLCB0ZXh0OiBlZGl0Lm5ld1RleHQgfTtcbn1cblxuZnVuY3Rpb24gbW9uYWNvUmFuZ2VUb0xzcChyOiBtb25hY28uSVJhbmdlKTogTHNwUmFuZ2Uge1xuICAgIHJldHVybiB7XG4gICAgICAgIHN0YXJ0OiB7IGxpbmU6IHIuc3RhcnRMaW5lTnVtYmVyIC0gMSwgY2hhcmFjdGVyOiByLnN0YXJ0Q29sdW1uIC0gMSB9LFxuICAgICAgICBlbmQ6ICAgeyBsaW5lOiByLmVuZExpbmVOdW1iZXIgLSAxLCBjaGFyYWN0ZXI6IHIuZW5kQ29sdW1uIC0gMSB9LFxuICAgIH07XG59XG5cbi8qKiBUcnVlIHdoZW4gdHdvIExTUCByYW5nZXMgaW50ZXJzZWN0ICh1c2VkIHRvIHBpY2sgdGhlIGRpYWdub3N0aWNzIHJlbGV2YW50IHRvIGEgY29kZS1hY3Rpb24gcmVxdWVzdCkuICovXG5mdW5jdGlvbiByYW5nZXNPdmVybGFwKGE6IExzcFJhbmdlLCBiOiBMc3BSYW5nZSk6IGJvb2xlYW4ge1xuICAgIGNvbnN0IG5vdEFmdGVyID0gKHA6IHsgbGluZTogbnVtYmVyOyBjaGFyYWN0ZXI6IG51bWJlciB9LCBxOiB7IGxpbmU6IG51bWJlcjsgY2hhcmFjdGVyOiBudW1iZXIgfSkgPT5cbiAgICAgICAgcC5saW5lIDwgcS5saW5lIHx8IChwLmxpbmUgPT09IHEubGluZSAmJiBwLmNoYXJhY3RlciA8PSBxLmNoYXJhY3Rlcik7XG4gICAgcmV0dXJuIG5vdEFmdGVyKGEuc3RhcnQsIGIuZW5kKSAmJiBub3RBZnRlcihiLnN0YXJ0LCBhLmVuZCk7XG59XG5cbmZ1bmN0aW9uIGxzcENvZGVBY3Rpb25Ub01vbmFjbyhhY3Rpb246IENvZGVBY3Rpb24gfCBDb21tYW5kKTogbW9uYWNvLmxhbmd1YWdlcy5Db2RlQWN0aW9uIHtcbiAgICBjb25zdCBpc0NvbW1hbmQgPSB0eXBlb2YgKGFjdGlvbiBhcyBDb21tYW5kKS5jb21tYW5kID09PSAnc3RyaW5nJztcbiAgICBjb25zdCB0aXRsZSA9IGFjdGlvbi50aXRsZVxuICAgICAgICA/PyAoaXNDb21tYW5kID8gKGFjdGlvbiBhcyBDb21tYW5kKS5jb21tYW5kIDogKGFjdGlvbiBhcyBDb2RlQWN0aW9uKS5jb21tYW5kPy50aXRsZSlcbiAgICAgICAgPz8gJ0FjdGlvbic7XG4gICAgY29uc3Qga2luZCA9IGlzQ29tbWFuZCA/ICdxdWlja2ZpeCcgOiAoKGFjdGlvbiBhcyBDb2RlQWN0aW9uKS5raW5kID8/ICdxdWlja2ZpeCcpO1xuICAgIHJldHVybiB7XG4gICAgICAgIHRpdGxlLFxuICAgICAgICBraW5kLFxuICAgICAgICBkaWFnbm9zdGljczogW10sXG4gICAgICAgIGlzUHJlZmVycmVkOiAoYWN0aW9uIGFzIENvZGVBY3Rpb24pLmlzUHJlZmVycmVkLFxuICAgICAgICAvLyBBcHBseSBsYXppbHkgdGhyb3VnaCBvdXIgY29tbWFuZCBzbyB3ZSBjYW4gcmVzb2x2ZSwgcnVuIHNlcnZlciBjb21tYW5kcyBhbmQgYXBwbHkgZWRpdHMgdW5pZm9ybWx5LlxuICAgICAgICBjb21tYW5kOiB7IGlkOiBBUFBMWV9BQ1RJT05fQ09NTUFORCwgdGl0bGUsIGFyZ3VtZW50czogW2FjdGlvbl0gfSxcbiAgICB9O1xufVxuXG5hc3luYyBmdW5jdGlvbiBhcHBseUNvZGVBY3Rpb24oYWN0aW9uOiBDb2RlQWN0aW9uIHwgQ29tbWFuZCk6IFByb21pc2U8dm9pZD4ge1xuICAgIGlmICghX2Nvbm4pIHJldHVybjtcbiAgICB0cnkge1xuICAgICAgICBpZiAodHlwZW9mIChhY3Rpb24gYXMgQ29tbWFuZCkuY29tbWFuZCA9PT0gJ3N0cmluZycpIHtcbiAgICAgICAgICAgIGF3YWl0IHJ1blNlcnZlckNvbW1hbmQoYWN0aW9uIGFzIENvbW1hbmQpO1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGxldCByZXNvbHZlZCA9IGFjdGlvbiBhcyBDb2RlQWN0aW9uO1xuICAgICAgICBpZiAoIXJlc29sdmVkLmVkaXQgJiYgKHJlc29sdmVkIGFzIHsgZGF0YT86IHVua25vd24gfSkuZGF0YSAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXNvbHZlZCA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCdjb2RlQWN0aW9uL3Jlc29sdmUnLCByZXNvbHZlZCk7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKHJlc29sdmVkLmVkaXQpIGFwcGx5V29ya3NwYWNlRWRpdChyZXNvbHZlZC5lZGl0KTtcbiAgICAgICAgaWYgKHJlc29sdmVkLmNvbW1hbmQpIGF3YWl0IHJ1blNlcnZlckNvbW1hbmQocmVzb2x2ZWQuY29tbWFuZCk7XG4gICAgfSBjYXRjaCAoZSkge1xuICAgICAgICBjb25zb2xlLndhcm4oJ1tqYXZhLWxzcF0gY29kZSBhY3Rpb24gZmFpbGVkOicsIChlIGFzIEVycm9yKT8ubWVzc2FnZSA/PyBlKTtcbiAgICB9XG59XG5cbmZ1bmN0aW9uIGlzV29ya3NwYWNlRWRpdCh2YWx1ZTogdW5rbm93bik6IHZhbHVlIGlzIFdvcmtzcGFjZUVkaXQge1xuICAgIHJldHVybiAhIXZhbHVlICYmICghISh2YWx1ZSBhcyBXb3Jrc3BhY2VFZGl0KS5jaGFuZ2VzIHx8ICEhKHZhbHVlIGFzIFdvcmtzcGFjZUVkaXQpLmRvY3VtZW50Q2hhbmdlcyk7XG59XG5cbmFzeW5jIGZ1bmN0aW9uIHJ1blNlcnZlckNvbW1hbmQoY21kOiBDb21tYW5kKTogUHJvbWlzZTx2b2lkPiB7XG4gICAgaWYgKCFfY29ubiB8fCAhY21kPy5jb21tYW5kKSByZXR1cm47XG4gICAgaWYgKEdFTkVSQVRFW2NtZC5jb21tYW5kXSkge1xuICAgICAgICBhd2FpdCBydW5HZW5lcmF0ZShjbWQuY29tbWFuZCwgY21kLmFyZ3VtZW50cyA/PyBbXSk7XG4gICAgICAgIHJldHVybjtcbiAgICB9XG4gICAgY29uc3QgcmVzdWx0ID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ3dvcmtzcGFjZS9leGVjdXRlQ29tbWFuZCcsIHsgY29tbWFuZDogY21kLmNvbW1hbmQsIGFyZ3VtZW50czogY21kLmFyZ3VtZW50cyA/PyBbXSB9KTtcbiAgICBpZiAoaXNXb3Jrc3BhY2VFZGl0KHJlc3VsdCkpIGFwcGx5V29ya3NwYWNlRWRpdChyZXN1bHQpO1xufVxuXG5pbnRlcmZhY2UgR2VuZXJhdGVTcGVjIHtcbiAgICBsYWJlbDogc3RyaW5nO1xuICAgIHN0YXR1czogc3RyaW5nO1xuICAgIGdlbmVyYXRlOiBzdHJpbmc7XG4gICAgbWVtYmVyczogKHN0YXR1czogYW55KSA9PiBBcnJheTx7IGxhYmVsOiBzdHJpbmc7IHJlZjogYW55IH0+O1xuICAgIGJ1aWxkQXJnczogKHByb21wdEFyZ3M6IGFueVtdLCBzdGF0dXM6IGFueSwgc2VsZWN0ZWQ6IGFueVtdKSA9PiBhbnlbXTtcbn1cblxuLyoqIE1lbWJlci1uYW1lZCBmaWVsZHMgXHUyMTkyIHBpY2tlciBsYWJlbHMuICovXG5mdW5jdGlvbiBmaWVsZExhYmVsKGY6IGFueSk6IHN0cmluZyB7XG4gICAgY29uc3QgbmFtZSA9IGY/Lm5hbWUgPz8gZj8uZmllbGROYW1lID8/ICcnO1xuICAgIGNvbnN0IHR5cGUgPSBmPy50eXBlID8/IGY/LnR5cGVOYW1lO1xuICAgIHJldHVybiB0eXBlID8gYCR7bmFtZX06ICR7dHlwZX1gIDogYCR7bmFtZX1gO1xufVxuXG4vKipcbiAqIFRoZSBKRFQuTFMgc291cmNlLWdlbmVyYXRpb24gY29tbWFuZHMgKGNvbnN0cnVjdG9ycywgZ2V0dGVycy9zZXR0ZXJzLCB0b1N0cmluZywgaGFzaENvZGUvZXF1YWxzKS5cbiAqIEVhY2ggbWFwcyB0aGUgY2xpZW50IFwiKlByb21wdFwiIGNvbW1hbmQgdG8gdGhlIHNlcnZlciBzdGF0dXMgKyBnZW5lcmF0ZSBkZWxlZ2F0ZSBjb21tYW5kczsgdGhlIHN0YXR1c1xuICogY2FsbCB5aWVsZHMgdGhlIGNhbmRpZGF0ZSBmaWVsZHMgc2hvd24gaW4gdGhlIG1lbWJlciBwaWNrZXIsIHRoZSBnZW5lcmF0ZSBjYWxsIHJldHVybnMgdGhlIGVkaXQuXG4gKi9cbmNvbnN0IEdFTkVSQVRFOiBSZWNvcmQ8c3RyaW5nLCBHZW5lcmF0ZVNwZWM+ID0ge1xuICAgICdqYXZhLmFjdGlvbi5nZW5lcmF0ZUNvbnN0cnVjdG9yc1Byb21wdCc6IHtcbiAgICAgICAgbGFiZWw6ICdTZWxlY3QgZmllbGRzIGFuZCBjb25zdHJ1Y3RvcnMnLFxuICAgICAgICBzdGF0dXM6ICdqYXZhLmFjdGlvbi5jaGVja0NvbnN0cnVjdG9yc1N0YXR1cycsXG4gICAgICAgIGdlbmVyYXRlOiAnamF2YS5hY3Rpb24uZ2VuZXJhdGVDb25zdHJ1Y3RvcnMnLFxuICAgICAgICBtZW1iZXJzOiAocykgPT4gKHM/LmZpZWxkcyA/PyBbXSkubWFwKChmOiBhbnkpID0+ICh7IGxhYmVsOiBmaWVsZExhYmVsKGYpLCByZWY6IGYgfSkpLFxuICAgICAgICBidWlsZEFyZ3M6IChhcmdzLCBzLCBzZWwpID0+IFthcmdzWzBdLCB7IGNvbnN0cnVjdG9yczogcz8uY29uc3RydWN0b3JzID8/IFtdLCBmaWVsZHM6IHNlbC5tYXAobSA9PiBtLnJlZikgfV0sXG4gICAgfSxcbiAgICAnamF2YS5hY3Rpb24uZ2VuZXJhdGVUb1N0cmluZ1Byb21wdCc6IHtcbiAgICAgICAgbGFiZWw6ICdTZWxlY3QgZmllbGRzIHRvIGluY2x1ZGUgaW4gdG9TdHJpbmcoKScsXG4gICAgICAgIHN0YXR1czogJ2phdmEuYWN0aW9uLmNoZWNrVG9TdHJpbmdTdGF0dXMnLFxuICAgICAgICBnZW5lcmF0ZTogJ2phdmEuYWN0aW9uLmdlbmVyYXRlVG9TdHJpbmcnLFxuICAgICAgICBtZW1iZXJzOiAocykgPT4gKHM/LmZpZWxkcyA/PyBbXSkubWFwKChmOiBhbnkpID0+ICh7IGxhYmVsOiBmaWVsZExhYmVsKGYpLCByZWY6IGYgfSkpLFxuICAgICAgICBidWlsZEFyZ3M6IChhcmdzLCBfcywgc2VsKSA9PiBbYXJnc1swXSwgc2VsLm1hcChtID0+IG0ucmVmKV0sXG4gICAgfSxcbiAgICAnamF2YS5hY3Rpb24uaGFzaENvZGVFcXVhbHNQcm9tcHQnOiB7XG4gICAgICAgIGxhYmVsOiAnU2VsZWN0IGZpZWxkcyBmb3IgaGFzaENvZGUoKSBhbmQgZXF1YWxzKCknLFxuICAgICAgICBzdGF0dXM6ICdqYXZhLmFjdGlvbi5jaGVja0hhc2hDb2RlRXF1YWxzU3RhdHVzJyxcbiAgICAgICAgZ2VuZXJhdGU6ICdqYXZhLmFjdGlvbi5nZW5lcmF0ZUhhc2hDb2RlRXF1YWxzJyxcbiAgICAgICAgbWVtYmVyczogKHMpID0+IChzPy5maWVsZHMgPz8gW10pLm1hcCgoZjogYW55KSA9PiAoeyBsYWJlbDogZmllbGRMYWJlbChmKSwgcmVmOiBmIH0pKSxcbiAgICAgICAgYnVpbGRBcmdzOiAoYXJncywgX3MsIHNlbCkgPT4gW2FyZ3NbMF0sIHNlbC5tYXAobSA9PiBtLnJlZiksIGZhbHNlXSxcbiAgICB9LFxuICAgICdqYXZhLmFjdGlvbi5nZW5lcmF0ZUFjY2Vzc29yc1Byb21wdCc6IHtcbiAgICAgICAgbGFiZWw6ICdTZWxlY3QgZmllbGRzIHRvIGdlbmVyYXRlIGdldHRlcnMgYW5kIHNldHRlcnMnLFxuICAgICAgICBzdGF0dXM6ICdqYXZhLmFjdGlvbi5jaGVja0FjY2Vzc29yc1N0YXR1cycsXG4gICAgICAgIGdlbmVyYXRlOiAnamF2YS5hY3Rpb24uZ2VuZXJhdGVBY2Nlc3NvcnMnLFxuICAgICAgICBtZW1iZXJzOiAocykgPT4gKHM/LmFjY2Vzc29ycyA/PyBzID8/IFtdKS5tYXAoKGE6IGFueSkgPT4gKHsgbGFiZWw6IGZpZWxkTGFiZWwoYSksIHJlZjogYSB9KSksXG4gICAgICAgIGJ1aWxkQXJnczogKGFyZ3MsIF9zLCBzZWwpID0+IFthcmdzWzBdLCBzZWwubWFwKG0gPT4gbS5yZWYpXSxcbiAgICB9LFxuICAgICdqYXZhLmFjdGlvbi5vdmVycmlkZU1ldGhvZHNQcm9tcHQnOiB7XG4gICAgICAgIGxhYmVsOiAnU2VsZWN0IG1ldGhvZHMgdG8gb3ZlcnJpZGUgb3IgaW1wbGVtZW50JyxcbiAgICAgICAgc3RhdHVzOiAnamF2YS5hY3Rpb24ubGlzdE92ZXJyaWRhYmxlTWV0aG9kcycsXG4gICAgICAgIGdlbmVyYXRlOiAnamF2YS5hY3Rpb24uYWRkT3ZlcnJpZGFibGVNZXRob2RzJyxcbiAgICAgICAgbWVtYmVyczogKHMpID0+IChzPy5tZXRob2RzID8/IFtdKS5tYXAoKG06IGFueSkgPT4gKHtcbiAgICAgICAgICAgIGxhYmVsOiBgJHttLm5hbWV9KCR7KG0ucGFyYW1ldGVycyA/PyBbXSkuam9pbignLCAnKX0pJHttLmRlY2xhcmluZ0NsYXNzID8gJyA6ICcgKyBtLmRlY2xhcmluZ0NsYXNzIDogJyd9YCxcbiAgICAgICAgICAgIHJlZjogbSxcbiAgICAgICAgfSkpLFxuICAgICAgICBidWlsZEFyZ3M6IChhcmdzLCBzdGF0dXMsIHNlbCkgPT4gW2FyZ3NbMF0sIHsgb3ZlcnJpZGFibGVNZXRob2RzOiBzZWwubWFwKG0gPT4gbS5yZWYpLCB0eXBlOiBzdGF0dXM/LnR5cGUgfV0sXG4gICAgfSxcbn07XG5cbmFzeW5jIGZ1bmN0aW9uIHJ1bkdlbmVyYXRlKHByb21wdElkOiBzdHJpbmcsIGFyZ3M6IGFueVtdKTogUHJvbWlzZTx2b2lkPiB7XG4gICAgaWYgKCFfY29ubikgcmV0dXJuO1xuICAgIGNvbnN0IHNwZWMgPSBHRU5FUkFURVtwcm9tcHRJZF07XG4gICAgY29uc3Qgc3RhdHVzID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ3dvcmtzcGFjZS9leGVjdXRlQ29tbWFuZCcsIHsgY29tbWFuZDogc3BlYy5zdGF0dXMsIGFyZ3VtZW50czogYXJncyB9KTtcbiAgICBpZiAoIXN0YXR1cykgcmV0dXJuO1xuICAgIGNvbnN0IG1lbWJlcnMgPSBzcGVjLm1lbWJlcnMoc3RhdHVzKTtcbiAgICBsZXQgc2VsZWN0ZWQgPSBtZW1iZXJzO1xuICAgIGNvbnN0IHBpY2tlciA9IChnbG9iYWxUaGlzIGFzIGFueSkuamF2YUxzcE1lbWJlclBpY2tlcjtcbiAgICBpZiAobWVtYmVycy5sZW5ndGggJiYgdHlwZW9mIHBpY2tlciA9PT0gJ2Z1bmN0aW9uJykge1xuICAgICAgICBjb25zdCBjaG9zZW46IHN0cmluZ1tdIHwgbnVsbCA9IGF3YWl0IHBpY2tlcihzcGVjLmxhYmVsLCBtZW1iZXJzLm1hcChtID0+IG0ubGFiZWwpKTtcbiAgICAgICAgaWYgKGNob3NlbiA9PT0gbnVsbCkgcmV0dXJuOyAvLyB1c2VyIGNhbmNlbGxlZCB0aGUgZGlhbG9nXG4gICAgICAgIHNlbGVjdGVkID0gbWVtYmVycy5maWx0ZXIobSA9PiBjaG9zZW4uaW5jbHVkZXMobS5sYWJlbCkpO1xuICAgIH1cbiAgICBjb25zdCBlZGl0ID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ3dvcmtzcGFjZS9leGVjdXRlQ29tbWFuZCcsIHtcbiAgICAgICAgY29tbWFuZDogc3BlYy5nZW5lcmF0ZSxcbiAgICAgICAgYXJndW1lbnRzOiBzcGVjLmJ1aWxkQXJncyhhcmdzLCBzdGF0dXMsIHNlbGVjdGVkKSxcbiAgICB9KTtcbiAgICBpZiAoaXNXb3Jrc3BhY2VFZGl0KGVkaXQpKSBhcHBseVdvcmtzcGFjZUVkaXQoZWRpdCk7XG59XG5cbi8qKiBHcm91cHMgYSB3b3Jrc3BhY2UgZWRpdCdzIHRleHQgZWRpdHMgYnkgZG9jdW1lbnQgYW5kIGFwcGxpZXMgdGhlbSBpbiBwbGFjZSB0byBvcGVuIE1vbmFjbyBtb2RlbHMuICovXG5mdW5jdGlvbiBhcHBseVdvcmtzcGFjZUVkaXQoZWRpdDogV29ya3NwYWNlRWRpdCB8IG51bGwgfCB1bmRlZmluZWQpOiB2b2lkIHtcbiAgICBpZiAoIWVkaXQpIHJldHVybjtcbiAgICBjb25zdCBieVVyaTogUmVjb3JkPHN0cmluZywgVGV4dEVkaXRbXT4gPSB7fTtcbiAgICBpZiAoZWRpdC5jaGFuZ2VzKSB7XG4gICAgICAgIGZvciAoY29uc3QgdXJpIGluIGVkaXQuY2hhbmdlcykgYnlVcmlbdXJpXSA9IChieVVyaVt1cmldID8/IFtdKS5jb25jYXQoZWRpdC5jaGFuZ2VzW3VyaV0pO1xuICAgIH1cbiAgICBpZiAoZWRpdC5kb2N1bWVudENoYW5nZXMpIHtcbiAgICAgICAgZm9yIChjb25zdCBkYyBvZiBlZGl0LmRvY3VtZW50Q2hhbmdlcyBhcyBhbnlbXSkge1xuICAgICAgICAgICAgaWYgKGRjPy50ZXh0RG9jdW1lbnQ/LnVyaSAmJiBBcnJheS5pc0FycmF5KGRjLmVkaXRzKSkge1xuICAgICAgICAgICAgICAgIGJ5VXJpW2RjLnRleHREb2N1bWVudC51cmldID0gKGJ5VXJpW2RjLnRleHREb2N1bWVudC51cmldID8/IFtdKS5jb25jYXQoZGMuZWRpdHMpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgfVxuICAgIGZvciAoY29uc3QgdXJpIGluIGJ5VXJpKSB7XG4gICAgICAgIGNvbnN0IG1vZGVsID0gbW9uYWNvLmVkaXRvci5nZXRNb2RlbHMoKS5maW5kKG0gPT4gbS51cmkudG9TdHJpbmcoKSA9PT0gdXJpKTtcbiAgICAgICAgaWYgKCFtb2RlbCkgY29udGludWU7XG4gICAgICAgIGNvbnN0IG9wcyA9IGJ5VXJpW3VyaV0ubWFwKGUgPT4gKHsgcmFuZ2U6IGxzcFJhbmdlVG9Nb25hY28oZS5yYW5nZSksIHRleHQ6IGUubmV3VGV4dCwgZm9yY2VNb3ZlTWFya2VyczogdHJ1ZSB9KSk7XG4gICAgICAgIG1vZGVsLnB1c2hFZGl0T3BlcmF0aW9ucyhbXSwgb3BzLCAoKSA9PiBudWxsKTtcbiAgICB9XG59XG5cbi8qKiBDb252ZXJ0cyBhbiBMU1Agd29ya3NwYWNlIGVkaXQgaW50byB0aGUgTW9uYWNvIHNoYXBlIHJldHVybmVkIGJ5IHRoZSByZW5hbWUgcHJvdmlkZXIuICovXG5mdW5jdGlvbiB3b3Jrc3BhY2VFZGl0VG9Nb25hY28oZWRpdDogV29ya3NwYWNlRWRpdCB8IG51bGwpOiBtb25hY28ubGFuZ3VhZ2VzLldvcmtzcGFjZUVkaXQge1xuICAgIGNvbnN0IGVkaXRzOiBhbnlbXSA9IFtdO1xuICAgIGNvbnN0IHB1c2ggPSAodXJpOiBzdHJpbmcsIGxpc3Q6IFRleHRFZGl0W10pID0+IHtcbiAgICAgICAgZm9yIChjb25zdCBlIG9mIGxpc3QpIHtcbiAgICAgICAgICAgIGVkaXRzLnB1c2goeyByZXNvdXJjZTogbW9uYWNvLlVyaS5wYXJzZSh1cmkpLCB0ZXh0RWRpdDogeyByYW5nZTogbHNwUmFuZ2VUb01vbmFjbyhlLnJhbmdlKSwgdGV4dDogZS5uZXdUZXh0IH0sIHZlcnNpb25JZDogdW5kZWZpbmVkIH0pO1xuICAgICAgICB9XG4gICAgfTtcbiAgICBpZiAoZWRpdD8uY2hhbmdlcykge1xuICAgICAgICBmb3IgKGNvbnN0IHVyaSBpbiBlZGl0LmNoYW5nZXMpIHB1c2godXJpLCBlZGl0LmNoYW5nZXNbdXJpXSk7XG4gICAgfVxuICAgIGlmIChlZGl0Py5kb2N1bWVudENoYW5nZXMpIHtcbiAgICAgICAgZm9yIChjb25zdCBkYyBvZiBlZGl0LmRvY3VtZW50Q2hhbmdlcyBhcyBhbnlbXSkge1xuICAgICAgICAgICAgaWYgKGRjPy50ZXh0RG9jdW1lbnQ/LnVyaSAmJiBBcnJheS5pc0FycmF5KGRjLmVkaXRzKSkgcHVzaChkYy50ZXh0RG9jdW1lbnQudXJpLCBkYy5lZGl0cyk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgcmV0dXJuIHsgZWRpdHMgfTtcbn1cblxuLyoqIE1hcHMgYSB2aXJ0dWFsIGVkaXRvciBVUkkgYmFjayB0byB0aGUgSURFIHdvcmtzcGFjZSBwYXRoICh7QGNvZGUgL3dzL3Byb2ovLi4ufSkuICovXG5mdW5jdGlvbiB1cmlUb1dvcmtzcGFjZVBhdGgodXJpOiBzdHJpbmcpOiBzdHJpbmcgfCBudWxsIHtcbiAgICBpZiAoIXVyaS5zdGFydHNXaXRoKFZJUlRVQUxfRklMRV9QUkVGSVgpKSByZXR1cm4gbnVsbDtcbiAgICByZXR1cm4gZGVjb2RlVVJJQ29tcG9uZW50KHVyaS5zdWJzdHJpbmcoVklSVFVBTF9GSUxFX1BSRUZJWC5sZW5ndGgpKTtcbn1cblxuLyoqIFJlYWRzIGEgd29ya3NwYWNlIGZpbGUncyBjdXJyZW50IHRleHQgb3ZlciB0aGUgSURFIFJFU1QgQVBJLiAqL1xuYXN5bmMgZnVuY3Rpb24gZmV0Y2hXb3Jrc3BhY2VGaWxlVGV4dChpZGVQYXRoOiBzdHJpbmcpOiBQcm9taXNlPHN0cmluZz4ge1xuICAgIGNvbnN0IHJlc3BvbnNlID0gYXdhaXQgZmV0Y2goJy9zZXJ2aWNlcy9pZGUvd29ya3NwYWNlcycgKyBpZGVQYXRoLCB7IGhlYWRlcnM6IHsgJ1gtUmVxdWVzdGVkLVdpdGgnOiAnRmV0Y2gnIH0gfSk7XG4gICAgaWYgKCFyZXNwb25zZS5vaykge1xuICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYENvdWxkIG5vdCByZWFkICR7aWRlUGF0aH0gKEhUVFAgJHtyZXNwb25zZS5zdGF0dXN9KWApO1xuICAgIH1cbiAgICByZXR1cm4gcmVzcG9uc2UudGV4dCgpO1xufVxuXG4vKiogQXBwbGllcyBMU1AgdGV4dCBlZGl0cyB0byBhIHN0cmluZy4gT2Zmc2V0cyBhcmUgcmVzb2x2ZWQgYWdhaW5zdCB0aGUgb3JpZ2luYWwgdGV4dCBhbmQgZWRpdHMgYXJlXG4gKiAgYXBwbGllZCBmcm9tIHRoZSBlbmQgYmFja3dhcmRzLCBzbyBlYXJsaWVyIG9mZnNldHMgc3RheSB2YWxpZC4gKi9cbmZ1bmN0aW9uIGFwcGx5RWRpdHNUb1RleHQodGV4dDogc3RyaW5nLCBlZGl0czogVGV4dEVkaXRbXSk6IHN0cmluZyB7XG4gICAgY29uc3QgbGluZVN0YXJ0cyA9IFswXTtcbiAgICBmb3IgKGxldCBpID0gMDsgaSA8IHRleHQubGVuZ3RoOyBpKyspIHtcbiAgICAgICAgaWYgKHRleHQuY2hhckNvZGVBdChpKSA9PT0gMTAgLyogXFxuICovKSBsaW5lU3RhcnRzLnB1c2goaSArIDEpO1xuICAgIH1cbiAgICBjb25zdCBvZmZzZXQgPSAocDogeyBsaW5lOiBudW1iZXI7IGNoYXJhY3RlcjogbnVtYmVyIH0pID0+IChsaW5lU3RhcnRzW3AubGluZV0gPz8gdGV4dC5sZW5ndGgpICsgcC5jaGFyYWN0ZXI7XG4gICAgY29uc3Qgb3JkZXJlZCA9IGVkaXRzLnNsaWNlKClcbiAgICAgICAgICAgICAgICAgICAgICAgICAuc29ydCgoYSwgYikgPT4gb2Zmc2V0KGIucmFuZ2Uuc3RhcnQpIC0gb2Zmc2V0KGEucmFuZ2Uuc3RhcnQpKTtcbiAgICBsZXQgcmVzdWx0ID0gdGV4dDtcbiAgICBmb3IgKGNvbnN0IGUgb2Ygb3JkZXJlZCkge1xuICAgICAgICByZXN1bHQgPSByZXN1bHQuc2xpY2UoMCwgb2Zmc2V0KGUucmFuZ2Uuc3RhcnQpKSArIGUubmV3VGV4dCArIHJlc3VsdC5zbGljZShvZmZzZXQoZS5yYW5nZS5lbmQpKTtcbiAgICB9XG4gICAgcmV0dXJuIHJlc3VsdDtcbn1cblxuLyoqXG4gKiBBcHBsaWVzIGEgSkRULkxTIHJlbmFtZSB7QGxpbmsgV29ya3NwYWNlRWRpdH0gYWNyb3NzIHRoZSB3aG9sZSB3b3Jrc3BhY2U6IHRleHQgZWRpdHMgaW4gZXZlcnlcbiAqIGFmZmVjdGVkIGZpbGUgcGx1cyBhbnkge0Bjb2RlIFJlbmFtZUZpbGV9IG9wZXJhdGlvbiAoYSBwdWJsaWMtdHlwZSByZW5hbWUgcmVuYW1lcyBpdHMgb3duXG4gKiB7QGNvZGUgLmphdmF9IGZpbGUpLiBUaGUgY3VycmVudCBmaWxlJ3MgZWRpdHMgZ28gdGhyb3VnaCB0aGUgbGl2ZSBNb25hY28gbW9kZWw7IHRoZSByZXN0IGFyZSByZWFkLFxuICogZWRpdGVkIGFuZCB3cml0dGVuIGJhY2sgb3ZlciBSRVNULiBQZXJzaXN0ZW5jZSAoQ1NSRi1ndWFyZGVkIHdyaXRlcywgdGhlIHRhYiBzd2l0Y2ggd2hlbiB0aGUgY3VycmVudFxuICogZmlsZSBpcyByZW5hbWVkLCBhbmQgcmVsb2FkaW5nIG90aGVyIG9wZW4gZWRpdG9ycykgaXMgZGVsZWdhdGVkIHRvIHRoZSBJREUgdmlhIGphdmFMc3BQZXJzaXN0UmVuYW1lLlxuICovXG5hc3luYyBmdW5jdGlvbiBhcHBseVJlbmFtZUFjcm9zc1dvcmtzcGFjZShtb2RlbDogbW9uYWNvLmVkaXRvci5JVGV4dE1vZGVsLCBlZGl0OiBXb3Jrc3BhY2VFZGl0KTogUHJvbWlzZTx2b2lkPiB7XG4gICAgY29uc3QgY3VycmVudFVyaSA9IG1vZGVsLnVyaS50b1N0cmluZygpO1xuICAgIGNvbnN0IHRleHRCeVVyaTogUmVjb3JkPHN0cmluZywgVGV4dEVkaXRbXT4gPSB7fTtcbiAgICBjb25zdCByZW5hbWVCeVVyaTogUmVjb3JkPHN0cmluZywgc3RyaW5nPiA9IHt9O1xuXG4gICAgaWYgKGVkaXQuY2hhbmdlcykge1xuICAgICAgICBmb3IgKGNvbnN0IHVyaSBpbiBlZGl0LmNoYW5nZXMpIHRleHRCeVVyaVt1cmldID0gKHRleHRCeVVyaVt1cmldID8/IFtdKS5jb25jYXQoZWRpdC5jaGFuZ2VzW3VyaV0pO1xuICAgIH1cbiAgICBpZiAoZWRpdC5kb2N1bWVudENoYW5nZXMpIHtcbiAgICAgICAgZm9yIChjb25zdCBkYyBvZiBlZGl0LmRvY3VtZW50Q2hhbmdlcyBhcyBhbnlbXSkge1xuICAgICAgICAgICAgaWYgKGRjPy5raW5kID09PSAncmVuYW1lJyAmJiBkYy5vbGRVcmkgJiYgZGMubmV3VXJpKSB7XG4gICAgICAgICAgICAgICAgcmVuYW1lQnlVcmlbZGMub2xkVXJpXSA9IGRjLm5ld1VyaTtcbiAgICAgICAgICAgIH0gZWxzZSBpZiAoZGM/LnRleHREb2N1bWVudD8udXJpICYmIEFycmF5LmlzQXJyYXkoZGMuZWRpdHMpKSB7XG4gICAgICAgICAgICAgICAgdGV4dEJ5VXJpW2RjLnRleHREb2N1bWVudC51cmldID0gKHRleHRCeVVyaVtkYy50ZXh0RG9jdW1lbnQudXJpXSA/PyBbXSkuY29uY2F0KGRjLmVkaXRzKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgIH1cblxuICAgIC8vIEpEVC5MUyBtYXkga2V5IGEgZmlsZSdzIHRleHQgZWRpdHMgYnkgaXRzIFBPU1QtcmVuYW1lIFVSSSAoZG9jdW1lbnRDaGFuZ2VzIGFyZSBvcmRlcmVkLCBhbmQgdGhlXG4gICAgLy8gcmVuYW1lIGNhbiBwcmVjZWRlIHRoZSBlZGl0KS4gUmUtYXR0cmlidXRlIGV2ZXJ5IGVkaXQgdG8gdGhlIG9uLWRpc2sgKG9sZCkgVVJJIHNvIHRoZSBjb250ZW50IGlzXG4gICAgLy8gZWRpdGVkIGNvcnJlY3RseSBiZWZvcmUgdGhlIGZpbGUgaXMgd3JpdHRlbi9yZW5hbWVkIFx1MjAxNCBvdGhlcndpc2UgdGhlIG5ldyBmaWxlIGtlZXBzIHRoZSBvbGQgdHlwZVxuICAgIC8vIG5hbWUgYW5kIHRyaWdnZXJzIFwiVGhlIHB1YmxpYyB0eXBlIFggbXVzdCBiZSBkZWZpbmVkIGluIGl0cyBvd24gZmlsZVwiLlxuICAgIGNvbnN0IG5ld1RvT2xkOiBSZWNvcmQ8c3RyaW5nLCBzdHJpbmc+ID0ge307XG4gICAgZm9yIChjb25zdCBvbGRVcmkgaW4gcmVuYW1lQnlVcmkpIG5ld1RvT2xkW3JlbmFtZUJ5VXJpW29sZFVyaV1dID0gb2xkVXJpO1xuICAgIGNvbnN0IGVkaXRzQnlPbGQ6IFJlY29yZDxzdHJpbmcsIFRleHRFZGl0W10+ID0ge307XG4gICAgZm9yIChjb25zdCB1cmkgaW4gdGV4dEJ5VXJpKSB7XG4gICAgICAgIGNvbnN0IG9uRGlza1VyaSA9IG5ld1RvT2xkW3VyaV0gPz8gdXJpO1xuICAgICAgICBlZGl0c0J5T2xkW29uRGlza1VyaV0gPSAoZWRpdHNCeU9sZFtvbkRpc2tVcmldID8/IFtdKS5jb25jYXQodGV4dEJ5VXJpW3VyaV0pO1xuICAgIH1cblxuICAgIGNvbnN0IHBheWxvYWQ6IHtcbiAgICAgICAgY3VycmVudFBhdGg6IHN0cmluZyB8IG51bGw7XG4gICAgICAgIGN1cnJlbnRDb250ZW50OiBzdHJpbmcgfCBudWxsO1xuICAgICAgICBjdXJyZW50TmV3UGF0aDogc3RyaW5nIHwgbnVsbDtcbiAgICAgICAgd3JpdGVzOiBBcnJheTx7IHBhdGg6IHN0cmluZyB8IG51bGw7IGNvbnRlbnQ6IHN0cmluZyB9PjtcbiAgICAgICAgcmVuYW1lczogQXJyYXk8eyBvbGRQYXRoOiBzdHJpbmcgfCBudWxsOyBuZXdQYXRoOiBzdHJpbmcgfCBudWxsOyBjb250ZW50OiBzdHJpbmcgfT47XG4gICAgfSA9IHsgY3VycmVudFBhdGg6IHVyaVRvV29ya3NwYWNlUGF0aChjdXJyZW50VXJpKSwgY3VycmVudENvbnRlbnQ6IG51bGwsIGN1cnJlbnROZXdQYXRoOiBudWxsLCB3cml0ZXM6IFtdLCByZW5hbWVzOiBbXSB9O1xuXG4gICAgY29uc3Qgd2F0Y2hlZENoYW5nZXM6IEFycmF5PHsgdXJpOiBzdHJpbmc7IHR5cGU6IG51bWJlciB9PiA9IFtdO1xuICAgIGNvbnN0IG9sZFVyaXMgPSBuZXcgU2V0PHN0cmluZz4oWy4uLk9iamVjdC5rZXlzKGVkaXRzQnlPbGQpLCAuLi5PYmplY3Qua2V5cyhyZW5hbWVCeVVyaSldKTtcbiAgICBmb3IgKGNvbnN0IG9sZFVyaSBvZiBvbGRVcmlzKSB7XG4gICAgICAgIGNvbnN0IGVkaXRzID0gZWRpdHNCeU9sZFtvbGRVcmldID8/IFtdO1xuICAgICAgICBsZXQgY29udGVudDogc3RyaW5nO1xuICAgICAgICBpZiAob2xkVXJpID09PSBjdXJyZW50VXJpKSB7XG4gICAgICAgICAgICBpZiAoZWRpdHMubGVuZ3RoKSB7XG4gICAgICAgICAgICAgICAgbW9kZWwucHVzaEVkaXRPcGVyYXRpb25zKFtdLCBlZGl0cy5tYXAoZSA9PiAoeyByYW5nZTogbHNwUmFuZ2VUb01vbmFjbyhlLnJhbmdlKSwgdGV4dDogZS5uZXdUZXh0LCBmb3JjZU1vdmVNYXJrZXJzOiB0cnVlIH0pKSwgKCkgPT4gbnVsbCk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBjb250ZW50ID0gbW9kZWwuZ2V0VmFsdWUoKTtcbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIGNvbnN0IHNvdXJjZSA9IGF3YWl0IGZldGNoV29ya3NwYWNlRmlsZVRleHQodXJpVG9Xb3Jrc3BhY2VQYXRoKG9sZFVyaSkgPz8gb2xkVXJpKTtcbiAgICAgICAgICAgIGNvbnRlbnQgPSBlZGl0cy5sZW5ndGggPyBhcHBseUVkaXRzVG9UZXh0KHNvdXJjZSwgZWRpdHMpIDogc291cmNlO1xuICAgICAgICB9XG4gICAgICAgIGNvbnN0IG5ld1VyaSA9IHJlbmFtZUJ5VXJpW29sZFVyaV07XG4gICAgICAgIGlmIChvbGRVcmkgPT09IGN1cnJlbnRVcmkpIHtcbiAgICAgICAgICAgIHBheWxvYWQuY3VycmVudENvbnRlbnQgPSBjb250ZW50O1xuICAgICAgICAgICAgcGF5bG9hZC5jdXJyZW50TmV3UGF0aCA9IG5ld1VyaSA/IHVyaVRvV29ya3NwYWNlUGF0aChuZXdVcmkpIDogbnVsbDtcbiAgICAgICAgfSBlbHNlIGlmIChuZXdVcmkpIHtcbiAgICAgICAgICAgIHBheWxvYWQucmVuYW1lcy5wdXNoKHsgb2xkUGF0aDogdXJpVG9Xb3Jrc3BhY2VQYXRoKG9sZFVyaSksIG5ld1BhdGg6IHVyaVRvV29ya3NwYWNlUGF0aChuZXdVcmkpLCBjb250ZW50IH0pO1xuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgcGF5bG9hZC53cml0ZXMucHVzaCh7IHBhdGg6IHVyaVRvV29ya3NwYWNlUGF0aChvbGRVcmkpLCBjb250ZW50IH0pO1xuICAgICAgICB9XG4gICAgICAgIC8vIEZpbGUtY2hhbmdlIGV2ZW50cyBmb3IgSkRULkxTIHNvIGl0IHJlLXN5bmNzIHdpdGhvdXQgYSBwYWdlIHJlZnJlc2guXG4gICAgICAgIGlmIChuZXdVcmkpIHtcbiAgICAgICAgICAgIHdhdGNoZWRDaGFuZ2VzLnB1c2goeyB1cmk6IG9sZFVyaSwgdHlwZTogMyAvKiBEZWxldGVkICovIH0sIHsgdXJpOiBuZXdVcmksIHR5cGU6IDEgLyogQ3JlYXRlZCAqLyB9KTtcbiAgICAgICAgICAgIF9vcGVuRmlsZXMuZGVsZXRlKG9sZFVyaSk7XG4gICAgICAgICAgICBfY29ubj8uc2VuZE5vdGlmaWNhdGlvbigndGV4dERvY3VtZW50L2RpZENsb3NlJywgeyB0ZXh0RG9jdW1lbnQ6IHsgdXJpOiBvbGRVcmkgfSB9KTtcbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIHdhdGNoZWRDaGFuZ2VzLnB1c2goeyB1cmk6IG9sZFVyaSwgdHlwZTogMiAvKiBDaGFuZ2VkICovIH0pO1xuICAgICAgICB9XG4gICAgfVxuXG4gICAgYXdhaXQgKGdsb2JhbFRoaXMgYXMgYW55KS5qYXZhTHNwUGVyc2lzdFJlbmFtZShwYXlsb2FkKTtcblxuICAgIC8vIEluZm9ybSBKRFQuTFMgb2YgdGhlIG9uLWRpc2sgY2hhbmdlcyBzbyB0aGUgcmVuYW1lZCB0eXBlJ3MgZGlhZ25vc3RpY3MgY2xlYXIgaW1tZWRpYXRlbHkuXG4gICAgaWYgKF9jb25uICYmIHdhdGNoZWRDaGFuZ2VzLmxlbmd0aCkge1xuICAgICAgICBfY29ubi5zZW5kTm90aWZpY2F0aW9uKCd3b3Jrc3BhY2UvZGlkQ2hhbmdlV2F0Y2hlZEZpbGVzJywgeyBjaGFuZ2VzOiB3YXRjaGVkQ2hhbmdlcyB9KTtcbiAgICB9XG59XG5cbmZ1bmN0aW9uIGpkdGxzU2V0dGluZ3MoKSB7XG4gICAgcmV0dXJuIHtcbiAgICAgICAgamF2YToge1xuICAgICAgICAgICAgaW1wb3J0OiB7XG4gICAgICAgICAgICAgICAgbWF2ZW46ICAgICAgeyBlbmFibGVkOiB0cnVlIH0sXG4gICAgICAgICAgICAgICAgZ3JhZGxlOiAgICAgeyBlbmFibGVkOiBmYWxzZSB9LFxuICAgICAgICAgICAgICAgIGV4Y2x1c2lvbnM6IFsnKiovbm9kZV9tb2R1bGVzLyoqJywgJyoqLy5tZXRhZGF0YS8qKicsICcqKi9hcmNoZXR5cGUtcmVzb3VyY2VzLyoqJ10sXG4gICAgICAgICAgICB9LFxuICAgICAgICAgICAgYXV0b2J1aWxkOiB7IGVuYWJsZWQ6IHRydWUgfSxcbiAgICAgICAgICAgIGNvbXBsZXRpb246IHtcbiAgICAgICAgICAgICAgICBvdmVyd3JpdGU6ICAgICAgICAgICAgdHJ1ZSxcbiAgICAgICAgICAgICAgICBndWVzc01ldGhvZEFyZ3VtZW50czogZmFsc2UsXG4gICAgICAgICAgICAgICAgcG9zdGZpeDogICAgICAgICAgICAgIHsgZW5hYmxlZDogdHJ1ZSB9LFxuICAgICAgICAgICAgICAgIGZpbHRlcmVkVHlwZXM6IFtcbiAgICAgICAgICAgICAgICAgICAgJ2NvbS5zdW4uKicsICdzdW4uKicsICdqZGsuKicsXG4gICAgICAgICAgICAgICAgICAgICdvcmcuZWNsaXBzZS5qZHQuaW50ZXJuYWwuKicsXG4gICAgICAgICAgICAgICAgICAgICdvcmcuZWNsaXBzZS5jb3JlLmludGVybmFsLionLFxuICAgICAgICAgICAgICAgICAgICAnb3JnLmVjbGlwc2Uub3NnaS5pbnRlcm5hbC4qJyxcbiAgICAgICAgICAgICAgICBdLFxuICAgICAgICAgICAgICAgIGltcG9ydE9yZGVyOiBbJ2phdmEnLCAnamF2YXgnLCAnb3JnJywgJ2NvbScsICcnXSxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICBzaWduYXR1cmVIZWxwOiAgeyBlbmFibGVkOiB0cnVlIH0sXG4gICAgICAgICAgICBmb3JtYXQ6ICAgICAgICAgeyBlbmFibGVkOiB0cnVlIH0sXG4gICAgICAgICAgICBzYXZlQWN0aW9uczogICAgeyBvcmdhbml6ZUltcG9ydHM6IGZhbHNlIH0sXG4gICAgICAgICAgICBpbmxheUhpbnRzOiAgICAgeyBwYXJhbWV0ZXJOYW1lczogeyBlbmFibGVkOiAnYWxsJyB9IH0sXG4gICAgICAgICAgICAvLyBPZmYgYnkgZGVmYXVsdDogdGhlIHJlZmVyZW5jZS9pbXBsZW1lbnRhdGlvbiBzZWFyY2ggYmVoaW5kIHRoZXNlIENvZGVMZW5zZXMgcnVucyBmb3IgZXZlcnlcbiAgICAgICAgICAgIC8vIGRlY2xhcmF0aW9uIG9uIG9wZW4gYW5kIG9uIGV2ZXJ5IGVkaXQgYW5kIGRvbWluYXRlcyBKRFQuTFMgbG9hZCBvbiBhIGxhcmdlIGNsYXNzcGF0aC5cbiAgICAgICAgICAgIHJlZmVyZW5jZXNDb2RlTGVuczogICAgIHsgZW5hYmxlZDogZmFsc2UgfSxcbiAgICAgICAgICAgIGltcGxlbWVudGF0aW9uc0NvZGVMZW5zOiB7IGVuYWJsZWQ6IGZhbHNlIH0sXG4gICAgICAgIH0sXG4gICAgfTtcbn1cbiIsICIvKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSAyMDI0IFR5cGVGb3ggYW5kIG90aGVycy5cbiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBNSVQgTGljZW5zZS4gU2VlIExJQ0VOU0UgaW4gdGhlIHBhY2thZ2Ugcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSAqL1xuXG5pbXBvcnQgeyBEaXNwb3NhYmxlIH0gZnJvbSAndnNjb2RlLWpzb25ycGMnO1xuXG5leHBvcnQgY2xhc3MgRGlzcG9zYWJsZUNvbGxlY3Rpb24gaW1wbGVtZW50cyBEaXNwb3NhYmxlIHtcbiAgICBwcm90ZWN0ZWQgcmVhZG9ubHkgZGlzcG9zYWJsZXM6IERpc3Bvc2FibGVbXSA9IFtdO1xuXG4gICAgZGlzcG9zZSgpOiB2b2lkIHtcbiAgICAgICAgd2hpbGUgKHRoaXMuZGlzcG9zYWJsZXMubGVuZ3RoICE9PSAwKSB7XG4gICAgICAgICAgICB0aGlzLmRpc3Bvc2FibGVzLnBvcCgpIS5kaXNwb3NlKCk7XG4gICAgICAgIH1cbiAgICB9XG5cbiAgICBwdXNoKGRpc3Bvc2FibGU6IERpc3Bvc2FibGUpOiBEaXNwb3NhYmxlIHtcbiAgICAgICAgY29uc3QgZGlzcG9zYWJsZXMgPSB0aGlzLmRpc3Bvc2FibGVzO1xuICAgICAgICBkaXNwb3NhYmxlcy5wdXNoKGRpc3Bvc2FibGUpO1xuICAgICAgICByZXR1cm4ge1xuICAgICAgICAgICAgZGlzcG9zZSgpOiB2b2lkIHtcbiAgICAgICAgICAgICAgICBjb25zdCBpbmRleCA9IGRpc3Bvc2FibGVzLmluZGV4T2YoZGlzcG9zYWJsZSk7XG4gICAgICAgICAgICAgICAgaWYgKGluZGV4ICE9PSAtMSkge1xuICAgICAgICAgICAgICAgICAgICBkaXNwb3NhYmxlcy5zcGxpY2UoaW5kZXgsIDEpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgfTtcbiAgICB9XG59XG4iLCAiLyogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbiAqIENvcHlyaWdodCAoYykgMjAyNCBUeXBlRm94IGFuZCBvdGhlcnMuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMSUNFTlNFIGluIHRoZSBwYWNrYWdlIHJvb3QgZm9yIGxpY2Vuc2UgaW5mb3JtYXRpb24uXG4gKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0gKi9cblxuaW1wb3J0IHsgRGlzcG9zYWJsZSB9IGZyb20gJ3ZzY29kZS1qc29ucnBjJztcbmltcG9ydCB0eXBlIHsgSUNvbm5lY3Rpb24gfSBmcm9tICcuLi9zZXJ2ZXIvY29ubmVjdGlvbi5qcyc7XG5cbmV4cG9ydCBpbnRlcmZhY2UgSVdlYlNvY2tldCBleHRlbmRzIERpc3Bvc2FibGUge1xuICAgIHNlbmQoY29udGVudDogc3RyaW5nKTogdm9pZDtcbiAgICAvLyBlc2xpbnQtZGlzYWJsZS1uZXh0LWxpbmUgQHR5cGVzY3JpcHQtZXNsaW50L25vLWV4cGxpY2l0LWFueVxuICAgIG9uTWVzc2FnZShjYjogKGRhdGE6IGFueSkgPT4gdm9pZCk6IHZvaWQ7XG4gICAgLy8gZXNsaW50LWRpc2FibGUtbmV4dC1saW5lIEB0eXBlc2NyaXB0LWVzbGludC9uby1leHBsaWNpdC1hbnlcbiAgICBvbkVycm9yKGNiOiAocmVhc29uOiBhbnkpID0+IHZvaWQpOiB2b2lkO1xuICAgIG9uQ2xvc2UoY2I6IChjb2RlOiBudW1iZXIsIHJlYXNvbjogc3RyaW5nKSA9PiB2b2lkKTogdm9pZDtcbn1cblxuZXhwb3J0IGludGVyZmFjZSBJV2ViU29ja2V0Q29ubmVjdGlvbiBleHRlbmRzIElDb25uZWN0aW9uIHtcbiAgICByZWFkb25seSBzb2NrZXQ6IElXZWJTb2NrZXQ7XG59XG4iLCAiLyogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbiAqIENvcHlyaWdodCAoYykgMjAyNCBUeXBlRm94IGFuZCBvdGhlcnMuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMSUNFTlNFIGluIHRoZSBwYWNrYWdlIHJvb3QgZm9yIGxpY2Vuc2UgaW5mb3JtYXRpb24uXG4gKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0gKi9cblxuaW1wb3J0IHsgRGlzcG9zYWJsZSB9IGZyb20gJ3ZzY29kZS1qc29ucnBjJztcbi8vIFRPRE86IFVzZSBlbnZpcm9ubWVudC1zcGVjaWZpYyBpbXBvcnRzICh2c2NvZGUtanNvbnJwYy9icm93c2VyIG9yIHZzY29kZS1qc29ucnBjL25vZGUpXG4vLyB3aGVuIHVwZ3JhZGluZyB0byB2c2NvZGUtanNvbnJwY0A5LngueC1uZXh0Llggd2hpY2ggc3VwcG9ydHMgcHJvcGVyIGV4cG9ydCBtYXBzXG5pbXBvcnQgeyB0eXBlIERhdGFDYWxsYmFjaywgQWJzdHJhY3RNZXNzYWdlUmVhZGVyLCBNZXNzYWdlUmVhZGVyIH0gZnJvbSAndnNjb2RlLWpzb25ycGMnO1xuaW1wb3J0IHR5cGUgeyBJV2ViU29ja2V0IH0gZnJvbSAnLi9zb2NrZXQuanMnO1xuXG5leHBvcnQgY2xhc3MgV2ViU29ja2V0TWVzc2FnZVJlYWRlciBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVJlYWRlciBpbXBsZW1lbnRzIE1lc3NhZ2VSZWFkZXIge1xuICAgIHByb3RlY3RlZCByZWFkb25seSBzb2NrZXQ6IElXZWJTb2NrZXQ7XG4gICAgcHJvdGVjdGVkIHN0YXRlOiAnaW5pdGlhbCcgfCAnbGlzdGVuaW5nJyB8ICdjbG9zZWQnID0gJ2luaXRpYWwnO1xuICAgIHByb3RlY3RlZCBjYWxsYmFjazogRGF0YUNhbGxiYWNrIHwgdW5kZWZpbmVkO1xuICAgIC8vIGVzbGludC1kaXNhYmxlLW5leHQtbGluZSBAdHlwZXNjcmlwdC1lc2xpbnQvbm8tZXhwbGljaXQtYW55XG4gICAgcHJvdGVjdGVkIHJlYWRvbmx5IGV2ZW50czogQXJyYXk8eyBtZXNzYWdlPzogYW55LCBlcnJvcj86IGFueSB9PiA9IFtdO1xuXG4gICAgY29uc3RydWN0b3Ioc29ja2V0OiBJV2ViU29ja2V0KSB7XG4gICAgICAgIHN1cGVyKCk7XG4gICAgICAgIHRoaXMuc29ja2V0ID0gc29ja2V0O1xuICAgICAgICB0aGlzLnNvY2tldC5vbk1lc3NhZ2UobWVzc2FnZSA9PlxuICAgICAgICAgICAgdGhpcy5yZWFkTWVzc2FnZShtZXNzYWdlKVxuICAgICAgICApO1xuICAgICAgICB0aGlzLnNvY2tldC5vbkVycm9yKGVycm9yID0+XG4gICAgICAgICAgICB0aGlzLmZpcmVFcnJvcihlcnJvcilcbiAgICAgICAgKTtcbiAgICAgICAgdGhpcy5zb2NrZXQub25DbG9zZSgoY29kZSwgcmVhc29uKSA9PiB7XG4gICAgICAgICAgICBpZiAoY29kZSAhPT0gMTAwMCkge1xuICAgICAgICAgICAgICAgIGNvbnN0IGVycm9yOiBFcnJvciA9IHtcbiAgICAgICAgICAgICAgICAgICAgbmFtZTogJycgKyBjb2RlLFxuICAgICAgICAgICAgICAgICAgICBtZXNzYWdlOiBgRXJyb3IgZHVyaW5nIHNvY2tldCByZWNvbm5lY3Q6IGNvZGUgPSAke2NvZGV9LCByZWFzb24gPSAke3JlYXNvbn1gXG4gICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgICAgICB0aGlzLmZpcmVFcnJvcihlcnJvcik7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICB0aGlzLmZpcmVDbG9zZSgpO1xuICAgICAgICB9KTtcbiAgICB9XG5cbiAgICBsaXN0ZW4oY2FsbGJhY2s6IERhdGFDYWxsYmFjayk6IERpc3Bvc2FibGUge1xuICAgICAgICBpZiAodGhpcy5zdGF0ZSA9PT0gJ2luaXRpYWwnKSB7XG4gICAgICAgICAgICB0aGlzLnN0YXRlID0gJ2xpc3RlbmluZyc7XG4gICAgICAgICAgICB0aGlzLmNhbGxiYWNrID0gY2FsbGJhY2s7XG4gICAgICAgICAgICB3aGlsZSAodGhpcy5ldmVudHMubGVuZ3RoICE9PSAwKSB7XG4gICAgICAgICAgICAgICAgY29uc3QgZXZlbnQgPSB0aGlzLmV2ZW50cy5wb3AoKSE7XG4gICAgICAgICAgICAgICAgaWYgKGV2ZW50Lm1lc3NhZ2UgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgICAgICB0aGlzLnJlYWRNZXNzYWdlKGV2ZW50Lm1lc3NhZ2UpO1xuICAgICAgICAgICAgICAgIH0gZWxzZSBpZiAoZXZlbnQuZXJyb3IgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgICAgICB0aGlzLmZpcmVFcnJvcihldmVudC5lcnJvcik7XG4gICAgICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5maXJlQ2xvc2UoKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHtcbiAgICAgICAgICAgIGRpc3Bvc2U6ICgpID0+IHtcbiAgICAgICAgICAgICAgICBpZiAodGhpcy5jYWxsYmFjayA9PT0gY2FsbGJhY2spIHtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5zdGF0ZSA9ICdpbml0aWFsJztcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5jYWxsYmFjayA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH07XG4gICAgfVxuXG4gICAgb3ZlcnJpZGUgZGlzcG9zZSgpIHtcbiAgICAgICAgc3VwZXIuZGlzcG9zZSgpO1xuICAgICAgICB0aGlzLnN0YXRlID0gJ2luaXRpYWwnO1xuICAgICAgICB0aGlzLmNhbGxiYWNrID0gdW5kZWZpbmVkO1xuICAgICAgICB0aGlzLmV2ZW50cy5zcGxpY2UoMCwgdGhpcy5ldmVudHMubGVuZ3RoKTtcbiAgICB9XG5cbiAgICAvLyBlc2xpbnQtZGlzYWJsZS1uZXh0LWxpbmUgQHR5cGVzY3JpcHQtZXNsaW50L25vLWV4cGxpY2l0LWFueVxuICAgIHByb3RlY3RlZCByZWFkTWVzc2FnZShtZXNzYWdlOiBhbnkpOiB2b2lkIHtcbiAgICAgICAgaWYgKHRoaXMuc3RhdGUgPT09ICdpbml0aWFsJykge1xuICAgICAgICAgICAgdGhpcy5ldmVudHMuc3BsaWNlKDAsIDAsIHsgbWVzc2FnZSB9KTtcbiAgICAgICAgfSBlbHNlIGlmICh0aGlzLnN0YXRlID09PSAnbGlzdGVuaW5nJykge1xuICAgICAgICAgICAgdHJ5IHtcbiAgICAgICAgICAgICAgICBjb25zdCBkYXRhID0gSlNPTi5wYXJzZShtZXNzYWdlKTtcbiAgICAgICAgICAgICAgICB0aGlzLmNhbGxiYWNrIShkYXRhKTtcbiAgICAgICAgICAgIH0gY2F0Y2ggKGVycikge1xuICAgICAgICAgICAgICAgIGNvbnN0IGVycm9yOiBFcnJvciA9IHtcbiAgICAgICAgICAgICAgICAgICAgbmFtZTogJycgKyA0MDAsXG4gICAgICAgICAgICAgICAgICAgIC8vIGVzbGludC1kaXNhYmxlLW5leHQtbGluZSBAdHlwZXNjcmlwdC1lc2xpbnQvbm8tZXhwbGljaXQtYW55XG4gICAgICAgICAgICAgICAgICAgIG1lc3NhZ2U6IGBFcnJvciBkdXJpbmcgbWVzc2FnZSBwYXJzaW5nLCByZWFzb24gPSAke3R5cGVvZiBlcnIgPT09ICdvYmplY3QnID8gKGVyciBhcyBhbnkpLm1lc3NhZ2UgOiAndW5rbm93bid9YFxuICAgICAgICAgICAgICAgIH07XG4gICAgICAgICAgICAgICAgdGhpcy5maXJlRXJyb3IoZXJyb3IpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgfVxuXG4gICAgLy8gZXNsaW50LWRpc2FibGUtbmV4dC1saW5lIEB0eXBlc2NyaXB0LWVzbGludC9uby1leHBsaWNpdC1hbnlcbiAgICBwcm90ZWN0ZWQgb3ZlcnJpZGUgZmlyZUVycm9yKGVycm9yOiBhbnkpOiB2b2lkIHtcbiAgICAgICAgaWYgKHRoaXMuc3RhdGUgPT09ICdpbml0aWFsJykge1xuICAgICAgICAgICAgdGhpcy5ldmVudHMuc3BsaWNlKDAsIDAsIHsgZXJyb3IgfSk7XG4gICAgICAgIH0gZWxzZSBpZiAodGhpcy5zdGF0ZSA9PT0gJ2xpc3RlbmluZycpIHtcbiAgICAgICAgICAgIHN1cGVyLmZpcmVFcnJvcihlcnJvcik7XG4gICAgICAgIH1cbiAgICB9XG5cbiAgICBwcm90ZWN0ZWQgb3ZlcnJpZGUgZmlyZUNsb3NlKCk6IHZvaWQge1xuICAgICAgICBpZiAodGhpcy5zdGF0ZSA9PT0gJ2luaXRpYWwnKSB7XG4gICAgICAgICAgICB0aGlzLmV2ZW50cy5zcGxpY2UoMCwgMCwge30pO1xuICAgICAgICB9IGVsc2UgaWYgKHRoaXMuc3RhdGUgPT09ICdsaXN0ZW5pbmcnKSB7XG4gICAgICAgICAgICBzdXBlci5maXJlQ2xvc2UoKTtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLnN0YXRlID0gJ2Nsb3NlZCc7XG4gICAgfVxufVxuIiwgIi8qIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiBDb3B5cmlnaHQgKGMpIDIwMjQgVHlwZUZveCBhbmQgb3RoZXJzLlxuICogTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTElDRU5TRSBpbiB0aGUgcGFja2FnZSByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG5cbmltcG9ydCB7IE1lc3NhZ2UgfSBmcm9tICd2c2NvZGUtanNvbnJwYyc7XG5pbXBvcnQgeyBBYnN0cmFjdE1lc3NhZ2VXcml0ZXIsIE1lc3NhZ2VXcml0ZXIgfSBmcm9tICd2c2NvZGUtanNvbnJwYyc7XG5pbXBvcnQgdHlwZSB7IElXZWJTb2NrZXQgfSBmcm9tICcuL3NvY2tldC5qcyc7XG5cbmV4cG9ydCBjbGFzcyBXZWJTb2NrZXRNZXNzYWdlV3JpdGVyIGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlV3JpdGVyIGltcGxlbWVudHMgTWVzc2FnZVdyaXRlciB7XG4gICAgcHJvdGVjdGVkIGVycm9yQ291bnQgPSAwO1xuICAgIHByb3RlY3RlZCByZWFkb25seSBzb2NrZXQ6IElXZWJTb2NrZXQ7XG5cbiAgICBjb25zdHJ1Y3Rvcihzb2NrZXQ6IElXZWJTb2NrZXQpIHtcbiAgICAgICAgc3VwZXIoKTtcbiAgICAgICAgdGhpcy5zb2NrZXQgPSBzb2NrZXQ7XG4gICAgfVxuXG4gICAgZW5kKCk6IHZvaWQge1xuICAgIH1cblxuICAgIGFzeW5jIHdyaXRlKG1zZzogTWVzc2FnZSk6IFByb21pc2U8dm9pZD4ge1xuICAgICAgICB0cnkge1xuICAgICAgICAgICAgY29uc3QgY29udGVudCA9IEpTT04uc3RyaW5naWZ5KG1zZyk7XG4gICAgICAgICAgICB0aGlzLnNvY2tldC5zZW5kKGNvbnRlbnQpO1xuICAgICAgICB9IGNhdGNoIChlKSB7XG4gICAgICAgICAgICB0aGlzLmVycm9yQ291bnQrKztcbiAgICAgICAgICAgIHRoaXMuZmlyZUVycm9yKGUsIG1zZywgdGhpcy5lcnJvckNvdW50KTtcbiAgICAgICAgfVxuICAgIH1cbn1cbiIsICIvKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSAyMDI0IFR5cGVGb3ggYW5kIG90aGVycy5cbiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBNSVQgTGljZW5zZS4gU2VlIExJQ0VOU0UgaW4gdGhlIHBhY2thZ2Ugcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSAqL1xuXG5pbXBvcnQgdHlwZSB7IE1lc3NhZ2VDb25uZWN0aW9uLCBMb2dnZXIgfSBmcm9tICd2c2NvZGUtanNvbnJwYyc7XG5pbXBvcnQgeyBjcmVhdGVNZXNzYWdlQ29ubmVjdGlvbiB9IGZyb20gJ3ZzY29kZS1qc29ucnBjJztcbmltcG9ydCB0eXBlIHsgSVdlYlNvY2tldCB9IGZyb20gJy4vc29ja2V0LmpzJztcbmltcG9ydCB7IFdlYlNvY2tldE1lc3NhZ2VSZWFkZXIgfSBmcm9tICcuL3JlYWRlci5qcyc7XG5pbXBvcnQgeyBXZWJTb2NrZXRNZXNzYWdlV3JpdGVyIH0gZnJvbSAnLi93cml0ZXIuanMnO1xuXG5leHBvcnQgZnVuY3Rpb24gY3JlYXRlV2ViU29ja2V0Q29ubmVjdGlvbihzb2NrZXQ6IElXZWJTb2NrZXQsIGxvZ2dlcjogTG9nZ2VyKTogTWVzc2FnZUNvbm5lY3Rpb24ge1xuICAgIGNvbnN0IG1lc3NhZ2VSZWFkZXIgPSBuZXcgV2ViU29ja2V0TWVzc2FnZVJlYWRlcihzb2NrZXQpO1xuICAgIGNvbnN0IG1lc3NhZ2VXcml0ZXIgPSBuZXcgV2ViU29ja2V0TWVzc2FnZVdyaXRlcihzb2NrZXQpO1xuICAgIGNvbnN0IGNvbm5lY3Rpb24gPSBjcmVhdGVNZXNzYWdlQ29ubmVjdGlvbihtZXNzYWdlUmVhZGVyLCBtZXNzYWdlV3JpdGVyLCBsb2dnZXIpO1xuICAgIGNvbm5lY3Rpb24ub25DbG9zZSgoKSA9PiBjb25uZWN0aW9uLmRpc3Bvc2UoKSk7XG4gICAgcmV0dXJuIGNvbm5lY3Rpb247XG59XG4iLCAiLyogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbiAqIENvcHlyaWdodCAoYykgMjAyNCBUeXBlRm94IGFuZCBvdGhlcnMuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMSUNFTlNFIGluIHRoZSBwYWNrYWdlIHJvb3QgZm9yIGxpY2Vuc2UgaW5mb3JtYXRpb24uXG4gKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0gKi9cblxuaW1wb3J0IHR5cGUgeyBNZXNzYWdlQ29ubmVjdGlvbiwgTG9nZ2VyIH0gZnJvbSAndnNjb2RlLWpzb25ycGMnO1xuaW1wb3J0IHsgY3JlYXRlV2ViU29ja2V0Q29ubmVjdGlvbiB9IGZyb20gJy4vc29ja2V0L2Nvbm5lY3Rpb24uanMnO1xuaW1wb3J0IHR5cGUgeyBJV2ViU29ja2V0IH0gZnJvbSAnLi9zb2NrZXQvc29ja2V0LmpzJztcbmltcG9ydCB7IENvbnNvbGVMb2dnZXIgfSBmcm9tICcuL2xvZ2dlci5qcyc7XG5cbmV4cG9ydCBmdW5jdGlvbiBsaXN0ZW4ob3B0aW9uczoge1xuICAgIHdlYlNvY2tldDogV2ViU29ja2V0O1xuICAgIGxvZ2dlcj86IExvZ2dlcjtcbiAgICBvbkNvbm5lY3Rpb246IChjb25uZWN0aW9uOiBNZXNzYWdlQ29ubmVjdGlvbikgPT4gdm9pZDtcbn0pIHtcbiAgICBjb25zdCB7IHdlYlNvY2tldCwgb25Db25uZWN0aW9uIH0gPSBvcHRpb25zO1xuICAgIGNvbnN0IGxvZ2dlciA9IG9wdGlvbnMubG9nZ2VyIHx8IG5ldyBDb25zb2xlTG9nZ2VyKCk7XG4gICAgd2ViU29ja2V0Lm9ub3BlbiA9ICgpID0+IHtcbiAgICAgICAgY29uc3Qgc29ja2V0ID0gdG9Tb2NrZXQod2ViU29ja2V0KTtcbiAgICAgICAgY29uc3QgY29ubmVjdGlvbiA9IGNyZWF0ZVdlYlNvY2tldENvbm5lY3Rpb24oc29ja2V0LCBsb2dnZXIpO1xuICAgICAgICBvbkNvbm5lY3Rpb24oY29ubmVjdGlvbik7XG4gICAgfTtcbn1cblxuZXhwb3J0IGZ1bmN0aW9uIHRvU29ja2V0KHdlYlNvY2tldDogV2ViU29ja2V0KTogSVdlYlNvY2tldCB7XG4gICAgcmV0dXJuIHtcbiAgICAgICAgc2VuZDogY29udGVudCA9PiB3ZWJTb2NrZXQuc2VuZChjb250ZW50KSxcbiAgICAgICAgb25NZXNzYWdlOiBjYiA9PiB7XG4gICAgICAgICAgICB3ZWJTb2NrZXQub25tZXNzYWdlID0gZXZlbnQgPT4gY2IoZXZlbnQuZGF0YSk7XG4gICAgICAgIH0sXG4gICAgICAgIG9uRXJyb3I6IGNiID0+IHtcbiAgICAgICAgICAgIC8vIGVzbGludC1kaXNhYmxlLW5leHQtbGluZSBAdHlwZXNjcmlwdC1lc2xpbnQvbm8tZXhwbGljaXQtYW55XG4gICAgICAgICAgICB3ZWJTb2NrZXQub25lcnJvciA9IChldmVudDogYW55KSA9PiB7XG4gICAgICAgICAgICAgICAgaWYgKE9iamVjdC5oYXNPd24oZXZlbnQsICdtZXNzYWdlJykpIHtcbiAgICAgICAgICAgICAgICAgICAgY2IoZXZlbnQubWVzc2FnZSk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfTtcbiAgICAgICAgfSxcbiAgICAgICAgb25DbG9zZTogY2IgPT4ge1xuICAgICAgICAgICAgd2ViU29ja2V0Lm9uY2xvc2UgPSBldmVudCA9PiBjYihldmVudC5jb2RlLCBldmVudC5yZWFzb24pO1xuICAgICAgICB9LFxuICAgICAgICBkaXNwb3NlOiAoKSA9PiB3ZWJTb2NrZXQuY2xvc2UoKVxuICAgIH07XG59XG4iLCAiLypcbiAqIENvcHlyaWdodCAoYykgMjAxMC0yMDI2IEVjbGlwc2UgRGlyaWdpYmxlIGNvbnRyaWJ1dG9yc1xuICpcbiAqIEFsbCByaWdodHMgcmVzZXJ2ZWQuIFRoaXMgcHJvZ3JhbSBhbmQgdGhlIGFjY29tcGFueWluZyBtYXRlcmlhbHMgYXJlIG1hZGUgYXZhaWxhYmxlIHVuZGVyIHRoZVxuICogdGVybXMgb2YgdGhlIEVjbGlwc2UgUHVibGljIExpY2Vuc2UgdjIuMCB3aGljaCBhY2NvbXBhbmllcyB0aGlzIGRpc3RyaWJ1dGlvbiwgYW5kIGlzIGF2YWlsYWJsZSBhdFxuICogaHR0cDovL3d3dy5lY2xpcHNlLm9yZy9sZWdhbC9lcGwtdjIwLmh0bWxcbiAqXG4gKiBTUERYLUZpbGVDb3B5cmlnaHRUZXh0OiBFY2xpcHNlIERpcmlnaWJsZSBjb250cmlidXRvcnMgU1BEWC1MaWNlbnNlLUlkZW50aWZpZXI6IEVQTC0yLjBcbiAqL1xuXG4vKipcbiAqIExhenkgUHJveHkgc2hpbSBmb3IgTW9uYWNvIEVkaXRvciBsb2FkZWQgdmlhIEFNRC5cbiAqXG4gKiBlZGl0b3IuanMgc2V0cyBnbG9iYWxUaGlzLm1vbmFjbyBpbnNpZGUgdGhlIEFNRCByZXF1aXJlKCkgY2FsbGJhY2ssIGJlZm9yZSBhbnlcbiAqIEphdmFMc3BDbGllbnRMaWIuY29ubmVjdCgpIGNhbGwuIEFsbCBwcm9wZXJ0eSBhY2Nlc3NlcyBnbyB0aHJvdWdoIFByb3h5LmdldCgpIHRyYXBzXG4gKiBzbyB0aGV5IHJlc29sdmUgYWdhaW5zdCB0aGUgbGl2ZSB3aW5kb3cubW9uYWNvIGF0IGNhbGwgdGltZSwgbm90IGF0IGJ1bmRsZSBsb2FkIHRpbWUuXG4gKlxuICogVHlwZVNjcmlwdCByZXNvbHZlcyB0eXBlcyBmcm9tIHRoZSByZWFsIG1vbmFjby1lZGl0b3IgZGV2RGVwZW5kZW5jeTsgZXNidWlsZCByZXBsYWNlc1xuICogdGhlIFwibW9uYWNvLWVkaXRvclwiIGltcG9ydCB3aXRoIHRoaXMgZmlsZSBhdCBidW5kbGUgdGltZSB2aWEgdGhlIGFsaWFzIG9wdGlvbi5cbiAqL1xuXG50eXBlIE0gPSB0eXBlb2YgaW1wb3J0KCdtb25hY28tZWRpdG9yJyk7XG5cbmZ1bmN0aW9uIG0oKTogTSB7XG4gICAgcmV0dXJuIChnbG9iYWxUaGlzIGFzIGFueSkubW9uYWNvIGFzIE07XG59XG5cbmZ1bmN0aW9uIG5zPFQgZXh0ZW5kcyBvYmplY3Q+KGdldHRlcjogKCkgPT4gVCk6IFQge1xuICAgIHJldHVybiBuZXcgUHJveHkoe30gYXMgVCwge1xuICAgICAgICBnZXQ6IChfLCBrKSA9PiAoZ2V0dGVyKCkgYXMgYW55KVtrIGFzIHN0cmluZ10sXG4gICAgfSk7XG59XG5cbmZ1bmN0aW9uIGNsczxUPihnZXR0ZXI6ICgpID0+IFQpOiBUIHtcbiAgICByZXR1cm4gbmV3IFByb3h5KGZ1bmN0aW9uICgpIHt9IGFzIGFueSwge1xuICAgICAgICBjb25zdHJ1Y3Q6IChfLCBhcmdzKSA9PiBuZXcgKGdldHRlcigpIGFzIGFueSkoLi4uYXJncyksXG4gICAgICAgIGdldDogICAgICAgKF8sIGspICAgID0+IChnZXR0ZXIoKSBhcyBhbnkpW2sgYXMgc3RyaW5nXSxcbiAgICB9KSBhcyBUO1xufVxuXG5leHBvcnQgY29uc3QgZWRpdG9yICAgICAgICAgPSBucygoKSA9PiBtKCkuZWRpdG9yKTtcbmV4cG9ydCBjb25zdCBsYW5ndWFnZXMgICAgICA9IG5zKCgpID0+IG0oKS5sYW5ndWFnZXMpO1xuZXhwb3J0IGNvbnN0IE1hcmtlclNldmVyaXR5ID0gbnMoKCkgPT4gbSgpLk1hcmtlclNldmVyaXR5KTtcbmV4cG9ydCBjb25zdCBNYXJrZXJUYWcgICAgICA9IG5zKCgpID0+IChtKCkgYXMgYW55KS5NYXJrZXJUYWcpO1xuZXhwb3J0IGNvbnN0IFVyaSAgICAgICAgICAgID0gY2xzKCgpID0+IG0oKS5VcmkpO1xuZXhwb3J0IGNvbnN0IFJhbmdlICAgICAgICAgID0gY2xzKCgpID0+IG0oKS5SYW5nZSk7XG5leHBvcnQgY29uc3QgUG9zaXRpb24gICAgICAgPSBjbHMoKCkgPT4gbSgpLlBvc2l0aW9uKTtcbmV4cG9ydCBjb25zdCBTZWxlY3Rpb24gICAgICA9IGNscygoKSA9PiBtKCkuU2VsZWN0aW9uKTtcbmV4cG9ydCBjb25zdCBLZXlDb2RlICAgICAgICA9IG5zKCgpID0+IG0oKS5LZXlDb2RlKTtcbmV4cG9ydCBjb25zdCBLZXlNb2QgICAgICAgICA9IG5zKCgpID0+IG0oKS5LZXlNb2QpO1xuIiwgIi8qIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiBDb3B5cmlnaHQgKGMpIE1pY3Jvc29mdCBDb3Jwb3JhdGlvbi4gQWxsIHJpZ2h0cyByZXNlcnZlZC5cbiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBNSVQgTGljZW5zZS4gU2VlIExpY2Vuc2UudHh0IGluIHRoZSBwcm9qZWN0IHJvb3QgZm9yIGxpY2Vuc2UgaW5mb3JtYXRpb24uXG4gKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0gKi9cbid1c2Ugc3RyaWN0JztcbmV4cG9ydCB2YXIgRG9jdW1lbnRVcmk7XG4oZnVuY3Rpb24gKERvY3VtZW50VXJpKSB7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgcmV0dXJuIHR5cGVvZiB2YWx1ZSA9PT0gJ3N0cmluZyc7XG4gICAgfVxuICAgIERvY3VtZW50VXJpLmlzID0gaXM7XG59KShEb2N1bWVudFVyaSB8fCAoRG9jdW1lbnRVcmkgPSB7fSkpO1xuZXhwb3J0IHZhciBVUkk7XG4oZnVuY3Rpb24gKFVSSSkge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiB0eXBlb2YgdmFsdWUgPT09ICdzdHJpbmcnO1xuICAgIH1cbiAgICBVUkkuaXMgPSBpcztcbn0pKFVSSSB8fCAoVVJJID0ge30pKTtcbmV4cG9ydCB2YXIgaW50ZWdlcjtcbihmdW5jdGlvbiAoaW50ZWdlcikge1xuICAgIGludGVnZXIuTUlOX1ZBTFVFID0gLTIxNDc0ODM2NDg7XG4gICAgaW50ZWdlci5NQVhfVkFMVUUgPSAyMTQ3NDgzNjQ3O1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiB0eXBlb2YgdmFsdWUgPT09ICdudW1iZXInICYmIGludGVnZXIuTUlOX1ZBTFVFIDw9IHZhbHVlICYmIHZhbHVlIDw9IGludGVnZXIuTUFYX1ZBTFVFO1xuICAgIH1cbiAgICBpbnRlZ2VyLmlzID0gaXM7XG59KShpbnRlZ2VyIHx8IChpbnRlZ2VyID0ge30pKTtcbmV4cG9ydCB2YXIgdWludGVnZXI7XG4oZnVuY3Rpb24gKHVpbnRlZ2VyKSB7XG4gICAgdWludGVnZXIuTUlOX1ZBTFVFID0gMDtcbiAgICB1aW50ZWdlci5NQVhfVkFMVUUgPSAyMTQ3NDgzNjQ3O1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiB0eXBlb2YgdmFsdWUgPT09ICdudW1iZXInICYmIHVpbnRlZ2VyLk1JTl9WQUxVRSA8PSB2YWx1ZSAmJiB2YWx1ZSA8PSB1aW50ZWdlci5NQVhfVkFMVUU7XG4gICAgfVxuICAgIHVpbnRlZ2VyLmlzID0gaXM7XG59KSh1aW50ZWdlciB8fCAodWludGVnZXIgPSB7fSkpO1xuLyoqXG4gKiBUaGUgUG9zaXRpb24gbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgUG9zaXRpb259IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIFBvc2l0aW9uO1xuKGZ1bmN0aW9uIChQb3NpdGlvbikge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgUG9zaXRpb24gbGl0ZXJhbCBmcm9tIHRoZSBnaXZlbiBsaW5lIGFuZCBjaGFyYWN0ZXIuXG4gICAgICogQHBhcmFtIGxpbmUgVGhlIHBvc2l0aW9uJ3MgbGluZS5cbiAgICAgKiBAcGFyYW0gY2hhcmFjdGVyIFRoZSBwb3NpdGlvbidzIGNoYXJhY3Rlci5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUobGluZSwgY2hhcmFjdGVyKSB7XG4gICAgICAgIGlmIChsaW5lID09PSBOdW1iZXIuTUFYX1ZBTFVFKSB7XG4gICAgICAgICAgICBsaW5lID0gdWludGVnZXIuTUFYX1ZBTFVFO1xuICAgICAgICB9XG4gICAgICAgIGlmIChjaGFyYWN0ZXIgPT09IE51bWJlci5NQVhfVkFMVUUpIHtcbiAgICAgICAgICAgIGNoYXJhY3RlciA9IHVpbnRlZ2VyLk1BWF9WQUxVRTtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4geyBsaW5lLCBjaGFyYWN0ZXIgfTtcbiAgICB9XG4gICAgUG9zaXRpb24uY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgUG9zaXRpb259IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5vYmplY3RMaXRlcmFsKGNhbmRpZGF0ZSkgJiYgSXMudWludGVnZXIoY2FuZGlkYXRlLmxpbmUpICYmIElzLnVpbnRlZ2VyKGNhbmRpZGF0ZS5jaGFyYWN0ZXIpO1xuICAgIH1cbiAgICBQb3NpdGlvbi5pcyA9IGlzO1xufSkoUG9zaXRpb24gfHwgKFBvc2l0aW9uID0ge30pKTtcbi8qKlxuICogVGhlIFJhbmdlIG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIFJhbmdlfSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBSYW5nZTtcbihmdW5jdGlvbiAoUmFuZ2UpIHtcbiAgICBmdW5jdGlvbiBjcmVhdGUob25lLCB0d28sIHRocmVlLCBmb3VyKSB7XG4gICAgICAgIGlmIChJcy51aW50ZWdlcihvbmUpICYmIElzLnVpbnRlZ2VyKHR3bykgJiYgSXMudWludGVnZXIodGhyZWUpICYmIElzLnVpbnRlZ2VyKGZvdXIpKSB7XG4gICAgICAgICAgICByZXR1cm4geyBzdGFydDogUG9zaXRpb24uY3JlYXRlKG9uZSwgdHdvKSwgZW5kOiBQb3NpdGlvbi5jcmVhdGUodGhyZWUsIGZvdXIpIH07XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSBpZiAoUG9zaXRpb24uaXMob25lKSAmJiBQb3NpdGlvbi5pcyh0d28pKSB7XG4gICAgICAgICAgICByZXR1cm4geyBzdGFydDogb25lLCBlbmQ6IHR3byB9O1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBSYW5nZSNjcmVhdGUgY2FsbGVkIHdpdGggaW52YWxpZCBhcmd1bWVudHNbJHtvbmV9LCAke3R3b30sICR7dGhyZWV9LCAke2ZvdXJ9XWApO1xuICAgICAgICB9XG4gICAgfVxuICAgIFJhbmdlLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIFJhbmdlfSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMub2JqZWN0TGl0ZXJhbChjYW5kaWRhdGUpICYmIFBvc2l0aW9uLmlzKGNhbmRpZGF0ZS5zdGFydCkgJiYgUG9zaXRpb24uaXMoY2FuZGlkYXRlLmVuZCk7XG4gICAgfVxuICAgIFJhbmdlLmlzID0gaXM7XG59KShSYW5nZSB8fCAoUmFuZ2UgPSB7fSkpO1xuLyoqXG4gKiBUaGUgTG9jYXRpb24gbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgTG9jYXRpb259IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIExvY2F0aW9uO1xuKGZ1bmN0aW9uIChMb2NhdGlvbikge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBMb2NhdGlvbiBsaXRlcmFsLlxuICAgICAqIEBwYXJhbSB1cmkgVGhlIGxvY2F0aW9uJ3MgdXJpLlxuICAgICAqIEBwYXJhbSByYW5nZSBUaGUgbG9jYXRpb24ncyByYW5nZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUodXJpLCByYW5nZSkge1xuICAgICAgICByZXR1cm4geyB1cmksIHJhbmdlIH07XG4gICAgfVxuICAgIExvY2F0aW9uLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIExvY2F0aW9ufSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMub2JqZWN0TGl0ZXJhbChjYW5kaWRhdGUpICYmIFJhbmdlLmlzKGNhbmRpZGF0ZS5yYW5nZSkgJiYgKElzLnN0cmluZyhjYW5kaWRhdGUudXJpKSB8fCBJcy51bmRlZmluZWQoY2FuZGlkYXRlLnVyaSkpO1xuICAgIH1cbiAgICBMb2NhdGlvbi5pcyA9IGlzO1xufSkoTG9jYXRpb24gfHwgKExvY2F0aW9uID0ge30pKTtcbi8qKlxuICogVGhlIExvY2F0aW9uTGluayBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBMb2NhdGlvbkxpbmt9IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIExvY2F0aW9uTGluaztcbihmdW5jdGlvbiAoTG9jYXRpb25MaW5rKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIExvY2F0aW9uTGluayBsaXRlcmFsLlxuICAgICAqIEBwYXJhbSB0YXJnZXRVcmkgVGhlIGRlZmluaXRpb24ncyB1cmkuXG4gICAgICogQHBhcmFtIHRhcmdldFJhbmdlIFRoZSBmdWxsIHJhbmdlIG9mIHRoZSBkZWZpbml0aW9uLlxuICAgICAqIEBwYXJhbSB0YXJnZXRTZWxlY3Rpb25SYW5nZSBUaGUgc3BhbiBvZiB0aGUgc3ltYm9sIGRlZmluaXRpb24gYXQgdGhlIHRhcmdldC5cbiAgICAgKiBAcGFyYW0gb3JpZ2luU2VsZWN0aW9uUmFuZ2UgVGhlIHNwYW4gb2YgdGhlIHN5bWJvbCBiZWluZyBkZWZpbmVkIGluIHRoZSBvcmlnaW5hdGluZyBzb3VyY2UgZmlsZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUodGFyZ2V0VXJpLCB0YXJnZXRSYW5nZSwgdGFyZ2V0U2VsZWN0aW9uUmFuZ2UsIG9yaWdpblNlbGVjdGlvblJhbmdlKSB7XG4gICAgICAgIHJldHVybiB7IHRhcmdldFVyaSwgdGFyZ2V0UmFuZ2UsIHRhcmdldFNlbGVjdGlvblJhbmdlLCBvcmlnaW5TZWxlY3Rpb25SYW5nZSB9O1xuICAgIH1cbiAgICBMb2NhdGlvbkxpbmsuY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgTG9jYXRpb25MaW5rfSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMub2JqZWN0TGl0ZXJhbChjYW5kaWRhdGUpICYmIFJhbmdlLmlzKGNhbmRpZGF0ZS50YXJnZXRSYW5nZSkgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS50YXJnZXRVcmkpXG4gICAgICAgICAgICAmJiBSYW5nZS5pcyhjYW5kaWRhdGUudGFyZ2V0U2VsZWN0aW9uUmFuZ2UpXG4gICAgICAgICAgICAmJiAoUmFuZ2UuaXMoY2FuZGlkYXRlLm9yaWdpblNlbGVjdGlvblJhbmdlKSB8fCBJcy51bmRlZmluZWQoY2FuZGlkYXRlLm9yaWdpblNlbGVjdGlvblJhbmdlKSk7XG4gICAgfVxuICAgIExvY2F0aW9uTGluay5pcyA9IGlzO1xufSkoTG9jYXRpb25MaW5rIHx8IChMb2NhdGlvbkxpbmsgPSB7fSkpO1xuLyoqXG4gKiBUaGUgQ29sb3IgbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgQ29sb3J9IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIENvbG9yO1xuKGZ1bmN0aW9uIChDb2xvcikge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgQ29sb3IgbGl0ZXJhbC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUocmVkLCBncmVlbiwgYmx1ZSwgYWxwaGEpIHtcbiAgICAgICAgcmV0dXJuIHtcbiAgICAgICAgICAgIHJlZCxcbiAgICAgICAgICAgIGdyZWVuLFxuICAgICAgICAgICAgYmx1ZSxcbiAgICAgICAgICAgIGFscGhhLFxuICAgICAgICB9O1xuICAgIH1cbiAgICBDb2xvci5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBDb2xvcn0gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMub2JqZWN0TGl0ZXJhbChjYW5kaWRhdGUpICYmIElzLm51bWJlclJhbmdlKGNhbmRpZGF0ZS5yZWQsIDAsIDEpXG4gICAgICAgICAgICAmJiBJcy5udW1iZXJSYW5nZShjYW5kaWRhdGUuZ3JlZW4sIDAsIDEpXG4gICAgICAgICAgICAmJiBJcy5udW1iZXJSYW5nZShjYW5kaWRhdGUuYmx1ZSwgMCwgMSlcbiAgICAgICAgICAgICYmIElzLm51bWJlclJhbmdlKGNhbmRpZGF0ZS5hbHBoYSwgMCwgMSk7XG4gICAgfVxuICAgIENvbG9yLmlzID0gaXM7XG59KShDb2xvciB8fCAoQ29sb3IgPSB7fSkpO1xuLyoqXG4gKiBUaGUgQ29sb3JJbmZvcm1hdGlvbiBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBDb2xvckluZm9ybWF0aW9ufSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBDb2xvckluZm9ybWF0aW9uO1xuKGZ1bmN0aW9uIChDb2xvckluZm9ybWF0aW9uKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBDb2xvckluZm9ybWF0aW9uIGxpdGVyYWwuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKHJhbmdlLCBjb2xvcikge1xuICAgICAgICByZXR1cm4ge1xuICAgICAgICAgICAgcmFuZ2UsXG4gICAgICAgICAgICBjb2xvcixcbiAgICAgICAgfTtcbiAgICB9XG4gICAgQ29sb3JJbmZvcm1hdGlvbi5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBDb2xvckluZm9ybWF0aW9ufSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5vYmplY3RMaXRlcmFsKGNhbmRpZGF0ZSkgJiYgUmFuZ2UuaXMoY2FuZGlkYXRlLnJhbmdlKSAmJiBDb2xvci5pcyhjYW5kaWRhdGUuY29sb3IpO1xuICAgIH1cbiAgICBDb2xvckluZm9ybWF0aW9uLmlzID0gaXM7XG59KShDb2xvckluZm9ybWF0aW9uIHx8IChDb2xvckluZm9ybWF0aW9uID0ge30pKTtcbi8qKlxuICogVGhlIENvbG9yIG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIENvbG9yUHJlc2VudGF0aW9ufSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBDb2xvclByZXNlbnRhdGlvbjtcbihmdW5jdGlvbiAoQ29sb3JQcmVzZW50YXRpb24pIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IENvbG9ySW5mb3JtYXRpb24gbGl0ZXJhbC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUobGFiZWwsIHRleHRFZGl0LCBhZGRpdGlvbmFsVGV4dEVkaXRzKSB7XG4gICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICBsYWJlbCxcbiAgICAgICAgICAgIHRleHRFZGl0LFxuICAgICAgICAgICAgYWRkaXRpb25hbFRleHRFZGl0cyxcbiAgICAgICAgfTtcbiAgICB9XG4gICAgQ29sb3JQcmVzZW50YXRpb24uY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgQ29sb3JJbmZvcm1hdGlvbn0gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMub2JqZWN0TGl0ZXJhbChjYW5kaWRhdGUpICYmIElzLnN0cmluZyhjYW5kaWRhdGUubGFiZWwpXG4gICAgICAgICAgICAmJiAoSXMudW5kZWZpbmVkKGNhbmRpZGF0ZS50ZXh0RWRpdCkgfHwgVGV4dEVkaXQuaXMoY2FuZGlkYXRlKSlcbiAgICAgICAgICAgICYmIChJcy51bmRlZmluZWQoY2FuZGlkYXRlLmFkZGl0aW9uYWxUZXh0RWRpdHMpIHx8IElzLnR5cGVkQXJyYXkoY2FuZGlkYXRlLmFkZGl0aW9uYWxUZXh0RWRpdHMsIFRleHRFZGl0LmlzKSk7XG4gICAgfVxuICAgIENvbG9yUHJlc2VudGF0aW9uLmlzID0gaXM7XG59KShDb2xvclByZXNlbnRhdGlvbiB8fCAoQ29sb3JQcmVzZW50YXRpb24gPSB7fSkpO1xuLyoqXG4gKiBBIHNldCBvZiBwcmVkZWZpbmVkIHJhbmdlIGtpbmRzLlxuICovXG5leHBvcnQgdmFyIEZvbGRpbmdSYW5nZUtpbmQ7XG4oZnVuY3Rpb24gKEZvbGRpbmdSYW5nZUtpbmQpIHtcbiAgICAvKipcbiAgICAgKiBGb2xkaW5nIHJhbmdlIGZvciBhIGNvbW1lbnRcbiAgICAgKi9cbiAgICBGb2xkaW5nUmFuZ2VLaW5kLkNvbW1lbnQgPSAnY29tbWVudCc7XG4gICAgLyoqXG4gICAgICogRm9sZGluZyByYW5nZSBmb3IgYW4gaW1wb3J0IG9yIGluY2x1ZGVcbiAgICAgKi9cbiAgICBGb2xkaW5nUmFuZ2VLaW5kLkltcG9ydHMgPSAnaW1wb3J0cyc7XG4gICAgLyoqXG4gICAgICogRm9sZGluZyByYW5nZSBmb3IgYSByZWdpb24gKGUuZy4gYCNyZWdpb25gKVxuICAgICAqL1xuICAgIEZvbGRpbmdSYW5nZUtpbmQuUmVnaW9uID0gJ3JlZ2lvbic7XG59KShGb2xkaW5nUmFuZ2VLaW5kIHx8IChGb2xkaW5nUmFuZ2VLaW5kID0ge30pKTtcbi8qKlxuICogVGhlIGZvbGRpbmcgcmFuZ2UgbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgRm9sZGluZ1JhbmdlfSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBGb2xkaW5nUmFuZ2U7XG4oZnVuY3Rpb24gKEZvbGRpbmdSYW5nZSkge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgRm9sZGluZ1JhbmdlIGxpdGVyYWwuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKHN0YXJ0TGluZSwgZW5kTGluZSwgc3RhcnRDaGFyYWN0ZXIsIGVuZENoYXJhY3Rlciwga2luZCwgY29sbGFwc2VkVGV4dCkge1xuICAgICAgICBjb25zdCByZXN1bHQgPSB7XG4gICAgICAgICAgICBzdGFydExpbmUsXG4gICAgICAgICAgICBlbmRMaW5lXG4gICAgICAgIH07XG4gICAgICAgIGlmIChJcy5kZWZpbmVkKHN0YXJ0Q2hhcmFjdGVyKSkge1xuICAgICAgICAgICAgcmVzdWx0LnN0YXJ0Q2hhcmFjdGVyID0gc3RhcnRDaGFyYWN0ZXI7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKElzLmRlZmluZWQoZW5kQ2hhcmFjdGVyKSkge1xuICAgICAgICAgICAgcmVzdWx0LmVuZENoYXJhY3RlciA9IGVuZENoYXJhY3RlcjtcbiAgICAgICAgfVxuICAgICAgICBpZiAoSXMuZGVmaW5lZChraW5kKSkge1xuICAgICAgICAgICAgcmVzdWx0LmtpbmQgPSBraW5kO1xuICAgICAgICB9XG4gICAgICAgIGlmIChJcy5kZWZpbmVkKGNvbGxhcHNlZFRleHQpKSB7XG4gICAgICAgICAgICByZXN1bHQuY29sbGFwc2VkVGV4dCA9IGNvbGxhcHNlZFRleHQ7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICB9XG4gICAgRm9sZGluZ1JhbmdlLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIEZvbGRpbmdSYW5nZX0gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMub2JqZWN0TGl0ZXJhbChjYW5kaWRhdGUpICYmIElzLnVpbnRlZ2VyKGNhbmRpZGF0ZS5zdGFydExpbmUpICYmIElzLnVpbnRlZ2VyKGNhbmRpZGF0ZS5zdGFydExpbmUpXG4gICAgICAgICAgICAmJiAoSXMudW5kZWZpbmVkKGNhbmRpZGF0ZS5zdGFydENoYXJhY3RlcikgfHwgSXMudWludGVnZXIoY2FuZGlkYXRlLnN0YXJ0Q2hhcmFjdGVyKSlcbiAgICAgICAgICAgICYmIChJcy51bmRlZmluZWQoY2FuZGlkYXRlLmVuZENoYXJhY3RlcikgfHwgSXMudWludGVnZXIoY2FuZGlkYXRlLmVuZENoYXJhY3RlcikpXG4gICAgICAgICAgICAmJiAoSXMudW5kZWZpbmVkKGNhbmRpZGF0ZS5raW5kKSB8fCBJcy5zdHJpbmcoY2FuZGlkYXRlLmtpbmQpKTtcbiAgICB9XG4gICAgRm9sZGluZ1JhbmdlLmlzID0gaXM7XG59KShGb2xkaW5nUmFuZ2UgfHwgKEZvbGRpbmdSYW5nZSA9IHt9KSk7XG4vKipcbiAqIFRoZSBEaWFnbm9zdGljUmVsYXRlZEluZm9ybWF0aW9uIG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIERpYWdub3N0aWNSZWxhdGVkSW5mb3JtYXRpb259IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIERpYWdub3N0aWNSZWxhdGVkSW5mb3JtYXRpb247XG4oZnVuY3Rpb24gKERpYWdub3N0aWNSZWxhdGVkSW5mb3JtYXRpb24pIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IERpYWdub3N0aWNSZWxhdGVkSW5mb3JtYXRpb24gbGl0ZXJhbC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUobG9jYXRpb24sIG1lc3NhZ2UpIHtcbiAgICAgICAgcmV0dXJuIHtcbiAgICAgICAgICAgIGxvY2F0aW9uLFxuICAgICAgICAgICAgbWVzc2FnZVxuICAgICAgICB9O1xuICAgIH1cbiAgICBEaWFnbm9zdGljUmVsYXRlZEluZm9ybWF0aW9uLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIERpYWdub3N0aWNSZWxhdGVkSW5mb3JtYXRpb259IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5kZWZpbmVkKGNhbmRpZGF0ZSkgJiYgTG9jYXRpb24uaXMoY2FuZGlkYXRlLmxvY2F0aW9uKSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLm1lc3NhZ2UpO1xuICAgIH1cbiAgICBEaWFnbm9zdGljUmVsYXRlZEluZm9ybWF0aW9uLmlzID0gaXM7XG59KShEaWFnbm9zdGljUmVsYXRlZEluZm9ybWF0aW9uIHx8IChEaWFnbm9zdGljUmVsYXRlZEluZm9ybWF0aW9uID0ge30pKTtcbi8qKlxuICogVGhlIGRpYWdub3N0aWMncyBzZXZlcml0eS5cbiAqL1xuZXhwb3J0IHZhciBEaWFnbm9zdGljU2V2ZXJpdHk7XG4oZnVuY3Rpb24gKERpYWdub3N0aWNTZXZlcml0eSkge1xuICAgIC8qKlxuICAgICAqIFJlcG9ydHMgYW4gZXJyb3IuXG4gICAgICovXG4gICAgRGlhZ25vc3RpY1NldmVyaXR5LkVycm9yID0gMTtcbiAgICAvKipcbiAgICAgKiBSZXBvcnRzIGEgd2FybmluZy5cbiAgICAgKi9cbiAgICBEaWFnbm9zdGljU2V2ZXJpdHkuV2FybmluZyA9IDI7XG4gICAgLyoqXG4gICAgICogUmVwb3J0cyBhbiBpbmZvcm1hdGlvbi5cbiAgICAgKi9cbiAgICBEaWFnbm9zdGljU2V2ZXJpdHkuSW5mb3JtYXRpb24gPSAzO1xuICAgIC8qKlxuICAgICAqIFJlcG9ydHMgYSBoaW50LlxuICAgICAqL1xuICAgIERpYWdub3N0aWNTZXZlcml0eS5IaW50ID0gNDtcbn0pKERpYWdub3N0aWNTZXZlcml0eSB8fCAoRGlhZ25vc3RpY1NldmVyaXR5ID0ge30pKTtcbi8qKlxuICogVGhlIGRpYWdub3N0aWMgdGFncy5cbiAqXG4gKiBAc2luY2UgMy4xNS4wXG4gKi9cbmV4cG9ydCB2YXIgRGlhZ25vc3RpY1RhZztcbihmdW5jdGlvbiAoRGlhZ25vc3RpY1RhZykge1xuICAgIC8qKlxuICAgICAqIFVudXNlZCBvciB1bm5lY2Vzc2FyeSBjb2RlLlxuICAgICAqXG4gICAgICogQ2xpZW50cyBhcmUgYWxsb3dlZCB0byByZW5kZXIgZGlhZ25vc3RpY3Mgd2l0aCB0aGlzIHRhZyBmYWRlZCBvdXQgaW5zdGVhZCBvZiBoYXZpbmdcbiAgICAgKiBhbiBlcnJvciBzcXVpZ2dsZS5cbiAgICAgKi9cbiAgICBEaWFnbm9zdGljVGFnLlVubmVjZXNzYXJ5ID0gMTtcbiAgICAvKipcbiAgICAgKiBEZXByZWNhdGVkIG9yIG9ic29sZXRlIGNvZGUuXG4gICAgICpcbiAgICAgKiBDbGllbnRzIGFyZSBhbGxvd2VkIHRvIHJlbmRlcmVkIGRpYWdub3N0aWNzIHdpdGggdGhpcyB0YWcgc3RyaWtlIHRocm91Z2guXG4gICAgICovXG4gICAgRGlhZ25vc3RpY1RhZy5EZXByZWNhdGVkID0gMjtcbn0pKERpYWdub3N0aWNUYWcgfHwgKERpYWdub3N0aWNUYWcgPSB7fSkpO1xuLyoqXG4gKiBUaGUgQ29kZURlc2NyaXB0aW9uIG5hbWVzcGFjZSBwcm92aWRlcyBmdW5jdGlvbnMgdG8gZGVhbCB3aXRoIGRlc2NyaXB0aW9ucyBmb3IgZGlhZ25vc3RpYyBjb2Rlcy5cbiAqXG4gKiBAc2luY2UgMy4xNi4wXG4gKi9cbmV4cG9ydCB2YXIgQ29kZURlc2NyaXB0aW9uO1xuKGZ1bmN0aW9uIChDb2RlRGVzY3JpcHRpb24pIHtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLm9iamVjdExpdGVyYWwoY2FuZGlkYXRlKSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLmhyZWYpO1xuICAgIH1cbiAgICBDb2RlRGVzY3JpcHRpb24uaXMgPSBpcztcbn0pKENvZGVEZXNjcmlwdGlvbiB8fCAoQ29kZURlc2NyaXB0aW9uID0ge30pKTtcbi8qKlxuICogVGhlIERpYWdub3N0aWMgbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgRGlhZ25vc3RpY30gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgRGlhZ25vc3RpYztcbihmdW5jdGlvbiAoRGlhZ25vc3RpYykge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgRGlhZ25vc3RpYyBsaXRlcmFsLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShyYW5nZSwgbWVzc2FnZSwgc2V2ZXJpdHksIGNvZGUsIHNvdXJjZSwgcmVsYXRlZEluZm9ybWF0aW9uKSB7XG4gICAgICAgIGxldCByZXN1bHQgPSB7IHJhbmdlLCBtZXNzYWdlIH07XG4gICAgICAgIGlmIChJcy5kZWZpbmVkKHNldmVyaXR5KSkge1xuICAgICAgICAgICAgcmVzdWx0LnNldmVyaXR5ID0gc2V2ZXJpdHk7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKElzLmRlZmluZWQoY29kZSkpIHtcbiAgICAgICAgICAgIHJlc3VsdC5jb2RlID0gY29kZTtcbiAgICAgICAgfVxuICAgICAgICBpZiAoSXMuZGVmaW5lZChzb3VyY2UpKSB7XG4gICAgICAgICAgICByZXN1bHQuc291cmNlID0gc291cmNlO1xuICAgICAgICB9XG4gICAgICAgIGlmIChJcy5kZWZpbmVkKHJlbGF0ZWRJbmZvcm1hdGlvbikpIHtcbiAgICAgICAgICAgIHJlc3VsdC5yZWxhdGVkSW5mb3JtYXRpb24gPSByZWxhdGVkSW5mb3JtYXRpb247XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICB9XG4gICAgRGlhZ25vc3RpYy5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBEaWFnbm9zdGljfSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgdmFyIF9hO1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5kZWZpbmVkKGNhbmRpZGF0ZSlcbiAgICAgICAgICAgICYmIFJhbmdlLmlzKGNhbmRpZGF0ZS5yYW5nZSlcbiAgICAgICAgICAgICYmIElzLnN0cmluZyhjYW5kaWRhdGUubWVzc2FnZSlcbiAgICAgICAgICAgICYmIChJcy5udW1iZXIoY2FuZGlkYXRlLnNldmVyaXR5KSB8fCBJcy51bmRlZmluZWQoY2FuZGlkYXRlLnNldmVyaXR5KSlcbiAgICAgICAgICAgICYmIChJcy5pbnRlZ2VyKGNhbmRpZGF0ZS5jb2RlKSB8fCBJcy5zdHJpbmcoY2FuZGlkYXRlLmNvZGUpIHx8IElzLnVuZGVmaW5lZChjYW5kaWRhdGUuY29kZSkpXG4gICAgICAgICAgICAmJiAoSXMudW5kZWZpbmVkKGNhbmRpZGF0ZS5jb2RlRGVzY3JpcHRpb24pIHx8IChJcy5zdHJpbmcoKF9hID0gY2FuZGlkYXRlLmNvZGVEZXNjcmlwdGlvbikgPT09IG51bGwgfHwgX2EgPT09IHZvaWQgMCA/IHZvaWQgMCA6IF9hLmhyZWYpKSlcbiAgICAgICAgICAgICYmIChJcy5zdHJpbmcoY2FuZGlkYXRlLnNvdXJjZSkgfHwgSXMudW5kZWZpbmVkKGNhbmRpZGF0ZS5zb3VyY2UpKVxuICAgICAgICAgICAgJiYgKElzLnVuZGVmaW5lZChjYW5kaWRhdGUucmVsYXRlZEluZm9ybWF0aW9uKSB8fCBJcy50eXBlZEFycmF5KGNhbmRpZGF0ZS5yZWxhdGVkSW5mb3JtYXRpb24sIERpYWdub3N0aWNSZWxhdGVkSW5mb3JtYXRpb24uaXMpKTtcbiAgICB9XG4gICAgRGlhZ25vc3RpYy5pcyA9IGlzO1xufSkoRGlhZ25vc3RpYyB8fCAoRGlhZ25vc3RpYyA9IHt9KSk7XG4vKipcbiAqIFRoZSBDb21tYW5kIG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIENvbW1hbmR9IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIENvbW1hbmQ7XG4oZnVuY3Rpb24gKENvbW1hbmQpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IENvbW1hbmQgbGl0ZXJhbC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUodGl0bGUsIGNvbW1hbmQsIC4uLmFyZ3MpIHtcbiAgICAgICAgbGV0IHJlc3VsdCA9IHsgdGl0bGUsIGNvbW1hbmQgfTtcbiAgICAgICAgaWYgKElzLmRlZmluZWQoYXJncykgJiYgYXJncy5sZW5ndGggPiAwKSB7XG4gICAgICAgICAgICByZXN1bHQuYXJndW1lbnRzID0gYXJncztcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cbiAgICBDb21tYW5kLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIENvbW1hbmR9IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5kZWZpbmVkKGNhbmRpZGF0ZSkgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS50aXRsZSkgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS5jb21tYW5kKTtcbiAgICB9XG4gICAgQ29tbWFuZC5pcyA9IGlzO1xufSkoQ29tbWFuZCB8fCAoQ29tbWFuZCA9IHt9KSk7XG4vKipcbiAqIFRoZSBUZXh0RWRpdCBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9uIHRvIGNyZWF0ZSByZXBsYWNlLFxuICogaW5zZXJ0IGFuZCBkZWxldGUgZWRpdHMgbW9yZSBlYXNpbHkuXG4gKi9cbmV4cG9ydCB2YXIgVGV4dEVkaXQ7XG4oZnVuY3Rpb24gKFRleHRFZGl0KSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIHJlcGxhY2UgdGV4dCBlZGl0LlxuICAgICAqIEBwYXJhbSByYW5nZSBUaGUgcmFuZ2Ugb2YgdGV4dCB0byBiZSByZXBsYWNlZC5cbiAgICAgKiBAcGFyYW0gbmV3VGV4dCBUaGUgbmV3IHRleHQuXG4gICAgICovXG4gICAgZnVuY3Rpb24gcmVwbGFjZShyYW5nZSwgbmV3VGV4dCkge1xuICAgICAgICByZXR1cm4geyByYW5nZSwgbmV3VGV4dCB9O1xuICAgIH1cbiAgICBUZXh0RWRpdC5yZXBsYWNlID0gcmVwbGFjZTtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGFuIGluc2VydCB0ZXh0IGVkaXQuXG4gICAgICogQHBhcmFtIHBvc2l0aW9uIFRoZSBwb3NpdGlvbiB0byBpbnNlcnQgdGhlIHRleHQgYXQuXG4gICAgICogQHBhcmFtIG5ld1RleHQgVGhlIHRleHQgdG8gYmUgaW5zZXJ0ZWQuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaW5zZXJ0KHBvc2l0aW9uLCBuZXdUZXh0KSB7XG4gICAgICAgIHJldHVybiB7IHJhbmdlOiB7IHN0YXJ0OiBwb3NpdGlvbiwgZW5kOiBwb3NpdGlvbiB9LCBuZXdUZXh0IH07XG4gICAgfVxuICAgIFRleHRFZGl0Lmluc2VydCA9IGluc2VydDtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgZGVsZXRlIHRleHQgZWRpdC5cbiAgICAgKiBAcGFyYW0gcmFuZ2UgVGhlIHJhbmdlIG9mIHRleHQgdG8gYmUgZGVsZXRlZC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBkZWwocmFuZ2UpIHtcbiAgICAgICAgcmV0dXJuIHsgcmFuZ2UsIG5ld1RleHQ6ICcnIH07XG4gICAgfVxuICAgIFRleHRFZGl0LmRlbCA9IGRlbDtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLm9iamVjdExpdGVyYWwoY2FuZGlkYXRlKVxuICAgICAgICAgICAgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS5uZXdUZXh0KVxuICAgICAgICAgICAgJiYgUmFuZ2UuaXMoY2FuZGlkYXRlLnJhbmdlKTtcbiAgICB9XG4gICAgVGV4dEVkaXQuaXMgPSBpcztcbn0pKFRleHRFZGl0IHx8IChUZXh0RWRpdCA9IHt9KSk7XG5leHBvcnQgdmFyIENoYW5nZUFubm90YXRpb247XG4oZnVuY3Rpb24gKENoYW5nZUFubm90YXRpb24pIHtcbiAgICBmdW5jdGlvbiBjcmVhdGUobGFiZWwsIG5lZWRzQ29uZmlybWF0aW9uLCBkZXNjcmlwdGlvbikge1xuICAgICAgICBjb25zdCByZXN1bHQgPSB7IGxhYmVsIH07XG4gICAgICAgIGlmIChuZWVkc0NvbmZpcm1hdGlvbiAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXN1bHQubmVlZHNDb25maXJtYXRpb24gPSBuZWVkc0NvbmZpcm1hdGlvbjtcbiAgICAgICAgfVxuICAgICAgICBpZiAoZGVzY3JpcHRpb24gIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmVzdWx0LmRlc2NyaXB0aW9uID0gZGVzY3JpcHRpb247XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICB9XG4gICAgQ2hhbmdlQW5ub3RhdGlvbi5jcmVhdGUgPSBjcmVhdGU7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5vYmplY3RMaXRlcmFsKGNhbmRpZGF0ZSkgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS5sYWJlbCkgJiZcbiAgICAgICAgICAgIChJcy5ib29sZWFuKGNhbmRpZGF0ZS5uZWVkc0NvbmZpcm1hdGlvbikgfHwgY2FuZGlkYXRlLm5lZWRzQ29uZmlybWF0aW9uID09PSB1bmRlZmluZWQpICYmXG4gICAgICAgICAgICAoSXMuc3RyaW5nKGNhbmRpZGF0ZS5kZXNjcmlwdGlvbikgfHwgY2FuZGlkYXRlLmRlc2NyaXB0aW9uID09PSB1bmRlZmluZWQpO1xuICAgIH1cbiAgICBDaGFuZ2VBbm5vdGF0aW9uLmlzID0gaXM7XG59KShDaGFuZ2VBbm5vdGF0aW9uIHx8IChDaGFuZ2VBbm5vdGF0aW9uID0ge30pKTtcbmV4cG9ydCB2YXIgQ2hhbmdlQW5ub3RhdGlvbklkZW50aWZpZXI7XG4oZnVuY3Rpb24gKENoYW5nZUFubm90YXRpb25JZGVudGlmaWVyKSB7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5zdHJpbmcoY2FuZGlkYXRlKTtcbiAgICB9XG4gICAgQ2hhbmdlQW5ub3RhdGlvbklkZW50aWZpZXIuaXMgPSBpcztcbn0pKENoYW5nZUFubm90YXRpb25JZGVudGlmaWVyIHx8IChDaGFuZ2VBbm5vdGF0aW9uSWRlbnRpZmllciA9IHt9KSk7XG5leHBvcnQgdmFyIEFubm90YXRlZFRleHRFZGl0O1xuKGZ1bmN0aW9uIChBbm5vdGF0ZWRUZXh0RWRpdCkge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYW4gYW5ub3RhdGVkIHJlcGxhY2UgdGV4dCBlZGl0LlxuICAgICAqXG4gICAgICogQHBhcmFtIHJhbmdlIFRoZSByYW5nZSBvZiB0ZXh0IHRvIGJlIHJlcGxhY2VkLlxuICAgICAqIEBwYXJhbSBuZXdUZXh0IFRoZSBuZXcgdGV4dC5cbiAgICAgKiBAcGFyYW0gYW5ub3RhdGlvbiBUaGUgYW5ub3RhdGlvbi5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiByZXBsYWNlKHJhbmdlLCBuZXdUZXh0LCBhbm5vdGF0aW9uKSB7XG4gICAgICAgIHJldHVybiB7IHJhbmdlLCBuZXdUZXh0LCBhbm5vdGF0aW9uSWQ6IGFubm90YXRpb24gfTtcbiAgICB9XG4gICAgQW5ub3RhdGVkVGV4dEVkaXQucmVwbGFjZSA9IHJlcGxhY2U7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhbiBhbm5vdGF0ZWQgaW5zZXJ0IHRleHQgZWRpdC5cbiAgICAgKlxuICAgICAqIEBwYXJhbSBwb3NpdGlvbiBUaGUgcG9zaXRpb24gdG8gaW5zZXJ0IHRoZSB0ZXh0IGF0LlxuICAgICAqIEBwYXJhbSBuZXdUZXh0IFRoZSB0ZXh0IHRvIGJlIGluc2VydGVkLlxuICAgICAqIEBwYXJhbSBhbm5vdGF0aW9uIFRoZSBhbm5vdGF0aW9uLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGluc2VydChwb3NpdGlvbiwgbmV3VGV4dCwgYW5ub3RhdGlvbikge1xuICAgICAgICByZXR1cm4geyByYW5nZTogeyBzdGFydDogcG9zaXRpb24sIGVuZDogcG9zaXRpb24gfSwgbmV3VGV4dCwgYW5ub3RhdGlvbklkOiBhbm5vdGF0aW9uIH07XG4gICAgfVxuICAgIEFubm90YXRlZFRleHRFZGl0Lmluc2VydCA9IGluc2VydDtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGFuIGFubm90YXRlZCBkZWxldGUgdGV4dCBlZGl0LlxuICAgICAqXG4gICAgICogQHBhcmFtIHJhbmdlIFRoZSByYW5nZSBvZiB0ZXh0IHRvIGJlIGRlbGV0ZWQuXG4gICAgICogQHBhcmFtIGFubm90YXRpb24gVGhlIGFubm90YXRpb24uXG4gICAgICovXG4gICAgZnVuY3Rpb24gZGVsKHJhbmdlLCBhbm5vdGF0aW9uKSB7XG4gICAgICAgIHJldHVybiB7IHJhbmdlLCBuZXdUZXh0OiAnJywgYW5ub3RhdGlvbklkOiBhbm5vdGF0aW9uIH07XG4gICAgfVxuICAgIEFubm90YXRlZFRleHRFZGl0LmRlbCA9IGRlbDtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIFRleHRFZGl0LmlzKGNhbmRpZGF0ZSkgJiYgKENoYW5nZUFubm90YXRpb24uaXMoY2FuZGlkYXRlLmFubm90YXRpb25JZCkgfHwgQ2hhbmdlQW5ub3RhdGlvbklkZW50aWZpZXIuaXMoY2FuZGlkYXRlLmFubm90YXRpb25JZCkpO1xuICAgIH1cbiAgICBBbm5vdGF0ZWRUZXh0RWRpdC5pcyA9IGlzO1xufSkoQW5ub3RhdGVkVGV4dEVkaXQgfHwgKEFubm90YXRlZFRleHRFZGl0ID0ge30pKTtcbi8qKlxuICogVGhlIFRleHREb2N1bWVudEVkaXQgbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbiB0byBjcmVhdGVcbiAqIGFuIGVkaXQgdGhhdCBtYW5pcHVsYXRlcyBhIHRleHQgZG9jdW1lbnQuXG4gKi9cbmV4cG9ydCB2YXIgVGV4dERvY3VtZW50RWRpdDtcbihmdW5jdGlvbiAoVGV4dERvY3VtZW50RWRpdCkge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgYFRleHREb2N1bWVudEVkaXRgXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKHRleHREb2N1bWVudCwgZWRpdHMpIHtcbiAgICAgICAgcmV0dXJuIHsgdGV4dERvY3VtZW50LCBlZGl0cyB9O1xuICAgIH1cbiAgICBUZXh0RG9jdW1lbnRFZGl0LmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5kZWZpbmVkKGNhbmRpZGF0ZSlcbiAgICAgICAgICAgICYmIE9wdGlvbmFsVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllci5pcyhjYW5kaWRhdGUudGV4dERvY3VtZW50KVxuICAgICAgICAgICAgJiYgQXJyYXkuaXNBcnJheShjYW5kaWRhdGUuZWRpdHMpO1xuICAgIH1cbiAgICBUZXh0RG9jdW1lbnRFZGl0LmlzID0gaXM7XG59KShUZXh0RG9jdW1lbnRFZGl0IHx8IChUZXh0RG9jdW1lbnRFZGl0ID0ge30pKTtcbmV4cG9ydCB2YXIgQ3JlYXRlRmlsZTtcbihmdW5jdGlvbiAoQ3JlYXRlRmlsZSkge1xuICAgIGZ1bmN0aW9uIGNyZWF0ZSh1cmksIG9wdGlvbnMsIGFubm90YXRpb24pIHtcbiAgICAgICAgbGV0IHJlc3VsdCA9IHtcbiAgICAgICAgICAgIGtpbmQ6ICdjcmVhdGUnLFxuICAgICAgICAgICAgdXJpXG4gICAgICAgIH07XG4gICAgICAgIGlmIChvcHRpb25zICE9PSB1bmRlZmluZWQgJiYgKG9wdGlvbnMub3ZlcndyaXRlICE9PSB1bmRlZmluZWQgfHwgb3B0aW9ucy5pZ25vcmVJZkV4aXN0cyAhPT0gdW5kZWZpbmVkKSkge1xuICAgICAgICAgICAgcmVzdWx0Lm9wdGlvbnMgPSBvcHRpb25zO1xuICAgICAgICB9XG4gICAgICAgIGlmIChhbm5vdGF0aW9uICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHJlc3VsdC5hbm5vdGF0aW9uSWQgPSBhbm5vdGF0aW9uO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxuICAgIENyZWF0ZUZpbGUuY3JlYXRlID0gY3JlYXRlO1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiBjYW5kaWRhdGUua2luZCA9PT0gJ2NyZWF0ZScgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS51cmkpICYmIChjYW5kaWRhdGUub3B0aW9ucyA9PT0gdW5kZWZpbmVkIHx8XG4gICAgICAgICAgICAoKGNhbmRpZGF0ZS5vcHRpb25zLm92ZXJ3cml0ZSA9PT0gdW5kZWZpbmVkIHx8IElzLmJvb2xlYW4oY2FuZGlkYXRlLm9wdGlvbnMub3ZlcndyaXRlKSkgJiYgKGNhbmRpZGF0ZS5vcHRpb25zLmlnbm9yZUlmRXhpc3RzID09PSB1bmRlZmluZWQgfHwgSXMuYm9vbGVhbihjYW5kaWRhdGUub3B0aW9ucy5pZ25vcmVJZkV4aXN0cykpKSkgJiYgKGNhbmRpZGF0ZS5hbm5vdGF0aW9uSWQgPT09IHVuZGVmaW5lZCB8fCBDaGFuZ2VBbm5vdGF0aW9uSWRlbnRpZmllci5pcyhjYW5kaWRhdGUuYW5ub3RhdGlvbklkKSk7XG4gICAgfVxuICAgIENyZWF0ZUZpbGUuaXMgPSBpcztcbn0pKENyZWF0ZUZpbGUgfHwgKENyZWF0ZUZpbGUgPSB7fSkpO1xuZXhwb3J0IHZhciBSZW5hbWVGaWxlO1xuKGZ1bmN0aW9uIChSZW5hbWVGaWxlKSB7XG4gICAgZnVuY3Rpb24gY3JlYXRlKG9sZFVyaSwgbmV3VXJpLCBvcHRpb25zLCBhbm5vdGF0aW9uKSB7XG4gICAgICAgIGxldCByZXN1bHQgPSB7XG4gICAgICAgICAgICBraW5kOiAncmVuYW1lJyxcbiAgICAgICAgICAgIG9sZFVyaSxcbiAgICAgICAgICAgIG5ld1VyaVxuICAgICAgICB9O1xuICAgICAgICBpZiAob3B0aW9ucyAhPT0gdW5kZWZpbmVkICYmIChvcHRpb25zLm92ZXJ3cml0ZSAhPT0gdW5kZWZpbmVkIHx8IG9wdGlvbnMuaWdub3JlSWZFeGlzdHMgIT09IHVuZGVmaW5lZCkpIHtcbiAgICAgICAgICAgIHJlc3VsdC5vcHRpb25zID0gb3B0aW9ucztcbiAgICAgICAgfVxuICAgICAgICBpZiAoYW5ub3RhdGlvbiAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXN1bHQuYW5ub3RhdGlvbklkID0gYW5ub3RhdGlvbjtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cbiAgICBSZW5hbWVGaWxlLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBjYW5kaWRhdGUgJiYgY2FuZGlkYXRlLmtpbmQgPT09ICdyZW5hbWUnICYmIElzLnN0cmluZyhjYW5kaWRhdGUub2xkVXJpKSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLm5ld1VyaSkgJiYgKGNhbmRpZGF0ZS5vcHRpb25zID09PSB1bmRlZmluZWQgfHxcbiAgICAgICAgICAgICgoY2FuZGlkYXRlLm9wdGlvbnMub3ZlcndyaXRlID09PSB1bmRlZmluZWQgfHwgSXMuYm9vbGVhbihjYW5kaWRhdGUub3B0aW9ucy5vdmVyd3JpdGUpKSAmJiAoY2FuZGlkYXRlLm9wdGlvbnMuaWdub3JlSWZFeGlzdHMgPT09IHVuZGVmaW5lZCB8fCBJcy5ib29sZWFuKGNhbmRpZGF0ZS5vcHRpb25zLmlnbm9yZUlmRXhpc3RzKSkpKSAmJiAoY2FuZGlkYXRlLmFubm90YXRpb25JZCA9PT0gdW5kZWZpbmVkIHx8IENoYW5nZUFubm90YXRpb25JZGVudGlmaWVyLmlzKGNhbmRpZGF0ZS5hbm5vdGF0aW9uSWQpKTtcbiAgICB9XG4gICAgUmVuYW1lRmlsZS5pcyA9IGlzO1xufSkoUmVuYW1lRmlsZSB8fCAoUmVuYW1lRmlsZSA9IHt9KSk7XG5leHBvcnQgdmFyIERlbGV0ZUZpbGU7XG4oZnVuY3Rpb24gKERlbGV0ZUZpbGUpIHtcbiAgICBmdW5jdGlvbiBjcmVhdGUodXJpLCBvcHRpb25zLCBhbm5vdGF0aW9uKSB7XG4gICAgICAgIGxldCByZXN1bHQgPSB7XG4gICAgICAgICAgICBraW5kOiAnZGVsZXRlJyxcbiAgICAgICAgICAgIHVyaVxuICAgICAgICB9O1xuICAgICAgICBpZiAob3B0aW9ucyAhPT0gdW5kZWZpbmVkICYmIChvcHRpb25zLnJlY3Vyc2l2ZSAhPT0gdW5kZWZpbmVkIHx8IG9wdGlvbnMuaWdub3JlSWZOb3RFeGlzdHMgIT09IHVuZGVmaW5lZCkpIHtcbiAgICAgICAgICAgIHJlc3VsdC5vcHRpb25zID0gb3B0aW9ucztcbiAgICAgICAgfVxuICAgICAgICBpZiAoYW5ub3RhdGlvbiAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXN1bHQuYW5ub3RhdGlvbklkID0gYW5ub3RhdGlvbjtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cbiAgICBEZWxldGVGaWxlLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBjYW5kaWRhdGUgJiYgY2FuZGlkYXRlLmtpbmQgPT09ICdkZWxldGUnICYmIElzLnN0cmluZyhjYW5kaWRhdGUudXJpKSAmJiAoY2FuZGlkYXRlLm9wdGlvbnMgPT09IHVuZGVmaW5lZCB8fFxuICAgICAgICAgICAgKChjYW5kaWRhdGUub3B0aW9ucy5yZWN1cnNpdmUgPT09IHVuZGVmaW5lZCB8fCBJcy5ib29sZWFuKGNhbmRpZGF0ZS5vcHRpb25zLnJlY3Vyc2l2ZSkpICYmIChjYW5kaWRhdGUub3B0aW9ucy5pZ25vcmVJZk5vdEV4aXN0cyA9PT0gdW5kZWZpbmVkIHx8IElzLmJvb2xlYW4oY2FuZGlkYXRlLm9wdGlvbnMuaWdub3JlSWZOb3RFeGlzdHMpKSkpICYmIChjYW5kaWRhdGUuYW5ub3RhdGlvbklkID09PSB1bmRlZmluZWQgfHwgQ2hhbmdlQW5ub3RhdGlvbklkZW50aWZpZXIuaXMoY2FuZGlkYXRlLmFubm90YXRpb25JZCkpO1xuICAgIH1cbiAgICBEZWxldGVGaWxlLmlzID0gaXM7XG59KShEZWxldGVGaWxlIHx8IChEZWxldGVGaWxlID0ge30pKTtcbmV4cG9ydCB2YXIgV29ya3NwYWNlRWRpdDtcbihmdW5jdGlvbiAoV29ya3NwYWNlRWRpdCkge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJlxuICAgICAgICAgICAgKGNhbmRpZGF0ZS5jaGFuZ2VzICE9PSB1bmRlZmluZWQgfHwgY2FuZGlkYXRlLmRvY3VtZW50Q2hhbmdlcyAhPT0gdW5kZWZpbmVkKSAmJlxuICAgICAgICAgICAgKGNhbmRpZGF0ZS5kb2N1bWVudENoYW5nZXMgPT09IHVuZGVmaW5lZCB8fCBjYW5kaWRhdGUuZG9jdW1lbnRDaGFuZ2VzLmV2ZXJ5KChjaGFuZ2UpID0+IHtcbiAgICAgICAgICAgICAgICBpZiAoSXMuc3RyaW5nKGNoYW5nZS5raW5kKSkge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4gQ3JlYXRlRmlsZS5pcyhjaGFuZ2UpIHx8IFJlbmFtZUZpbGUuaXMoY2hhbmdlKSB8fCBEZWxldGVGaWxlLmlzKGNoYW5nZSk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4gVGV4dERvY3VtZW50RWRpdC5pcyhjaGFuZ2UpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH0pKTtcbiAgICB9XG4gICAgV29ya3NwYWNlRWRpdC5pcyA9IGlzO1xufSkoV29ya3NwYWNlRWRpdCB8fCAoV29ya3NwYWNlRWRpdCA9IHt9KSk7XG5jbGFzcyBUZXh0RWRpdENoYW5nZUltcGwge1xuICAgIGNvbnN0cnVjdG9yKGVkaXRzLCBjaGFuZ2VBbm5vdGF0aW9ucykge1xuICAgICAgICB0aGlzLmVkaXRzID0gZWRpdHM7XG4gICAgICAgIHRoaXMuY2hhbmdlQW5ub3RhdGlvbnMgPSBjaGFuZ2VBbm5vdGF0aW9ucztcbiAgICB9XG4gICAgaW5zZXJ0KHBvc2l0aW9uLCBuZXdUZXh0LCBhbm5vdGF0aW9uKSB7XG4gICAgICAgIGxldCBlZGl0O1xuICAgICAgICBsZXQgaWQ7XG4gICAgICAgIGlmIChhbm5vdGF0aW9uID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIGVkaXQgPSBUZXh0RWRpdC5pbnNlcnQocG9zaXRpb24sIG5ld1RleHQpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2UgaWYgKENoYW5nZUFubm90YXRpb25JZGVudGlmaWVyLmlzKGFubm90YXRpb24pKSB7XG4gICAgICAgICAgICBpZCA9IGFubm90YXRpb247XG4gICAgICAgICAgICBlZGl0ID0gQW5ub3RhdGVkVGV4dEVkaXQuaW5zZXJ0KHBvc2l0aW9uLCBuZXdUZXh0LCBhbm5vdGF0aW9uKTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHRoaXMuYXNzZXJ0Q2hhbmdlQW5ub3RhdGlvbnModGhpcy5jaGFuZ2VBbm5vdGF0aW9ucyk7XG4gICAgICAgICAgICBpZCA9IHRoaXMuY2hhbmdlQW5ub3RhdGlvbnMubWFuYWdlKGFubm90YXRpb24pO1xuICAgICAgICAgICAgZWRpdCA9IEFubm90YXRlZFRleHRFZGl0Lmluc2VydChwb3NpdGlvbiwgbmV3VGV4dCwgaWQpO1xuICAgICAgICB9XG4gICAgICAgIHRoaXMuZWRpdHMucHVzaChlZGl0KTtcbiAgICAgICAgaWYgKGlkICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHJldHVybiBpZDtcbiAgICAgICAgfVxuICAgIH1cbiAgICByZXBsYWNlKHJhbmdlLCBuZXdUZXh0LCBhbm5vdGF0aW9uKSB7XG4gICAgICAgIGxldCBlZGl0O1xuICAgICAgICBsZXQgaWQ7XG4gICAgICAgIGlmIChhbm5vdGF0aW9uID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIGVkaXQgPSBUZXh0RWRpdC5yZXBsYWNlKHJhbmdlLCBuZXdUZXh0KTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIGlmIChDaGFuZ2VBbm5vdGF0aW9uSWRlbnRpZmllci5pcyhhbm5vdGF0aW9uKSkge1xuICAgICAgICAgICAgaWQgPSBhbm5vdGF0aW9uO1xuICAgICAgICAgICAgZWRpdCA9IEFubm90YXRlZFRleHRFZGl0LnJlcGxhY2UocmFuZ2UsIG5ld1RleHQsIGFubm90YXRpb24pO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgdGhpcy5hc3NlcnRDaGFuZ2VBbm5vdGF0aW9ucyh0aGlzLmNoYW5nZUFubm90YXRpb25zKTtcbiAgICAgICAgICAgIGlkID0gdGhpcy5jaGFuZ2VBbm5vdGF0aW9ucy5tYW5hZ2UoYW5ub3RhdGlvbik7XG4gICAgICAgICAgICBlZGl0ID0gQW5ub3RhdGVkVGV4dEVkaXQucmVwbGFjZShyYW5nZSwgbmV3VGV4dCwgaWQpO1xuICAgICAgICB9XG4gICAgICAgIHRoaXMuZWRpdHMucHVzaChlZGl0KTtcbiAgICAgICAgaWYgKGlkICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHJldHVybiBpZDtcbiAgICAgICAgfVxuICAgIH1cbiAgICBkZWxldGUocmFuZ2UsIGFubm90YXRpb24pIHtcbiAgICAgICAgbGV0IGVkaXQ7XG4gICAgICAgIGxldCBpZDtcbiAgICAgICAgaWYgKGFubm90YXRpb24gPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgZWRpdCA9IFRleHRFZGl0LmRlbChyYW5nZSk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSBpZiAoQ2hhbmdlQW5ub3RhdGlvbklkZW50aWZpZXIuaXMoYW5ub3RhdGlvbikpIHtcbiAgICAgICAgICAgIGlkID0gYW5ub3RhdGlvbjtcbiAgICAgICAgICAgIGVkaXQgPSBBbm5vdGF0ZWRUZXh0RWRpdC5kZWwocmFuZ2UsIGFubm90YXRpb24pO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgdGhpcy5hc3NlcnRDaGFuZ2VBbm5vdGF0aW9ucyh0aGlzLmNoYW5nZUFubm90YXRpb25zKTtcbiAgICAgICAgICAgIGlkID0gdGhpcy5jaGFuZ2VBbm5vdGF0aW9ucy5tYW5hZ2UoYW5ub3RhdGlvbik7XG4gICAgICAgICAgICBlZGl0ID0gQW5ub3RhdGVkVGV4dEVkaXQuZGVsKHJhbmdlLCBpZCk7XG4gICAgICAgIH1cbiAgICAgICAgdGhpcy5lZGl0cy5wdXNoKGVkaXQpO1xuICAgICAgICBpZiAoaWQgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmV0dXJuIGlkO1xuICAgICAgICB9XG4gICAgfVxuICAgIGFkZChlZGl0KSB7XG4gICAgICAgIHRoaXMuZWRpdHMucHVzaChlZGl0KTtcbiAgICB9XG4gICAgYWxsKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5lZGl0cztcbiAgICB9XG4gICAgY2xlYXIoKSB7XG4gICAgICAgIHRoaXMuZWRpdHMuc3BsaWNlKDAsIHRoaXMuZWRpdHMubGVuZ3RoKTtcbiAgICB9XG4gICAgYXNzZXJ0Q2hhbmdlQW5ub3RhdGlvbnModmFsdWUpIHtcbiAgICAgICAgaWYgKHZhbHVlID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgVGV4dCBlZGl0IGNoYW5nZSBpcyBub3QgY29uZmlndXJlZCB0byBtYW5hZ2UgY2hhbmdlIGFubm90YXRpb25zLmApO1xuICAgICAgICB9XG4gICAgfVxufVxuLyoqXG4gKiBBIGhlbHBlciBjbGFzc1xuICovXG5jbGFzcyBDaGFuZ2VBbm5vdGF0aW9ucyB7XG4gICAgY29uc3RydWN0b3IoYW5ub3RhdGlvbnMpIHtcbiAgICAgICAgdGhpcy5fYW5ub3RhdGlvbnMgPSBhbm5vdGF0aW9ucyA9PT0gdW5kZWZpbmVkID8gT2JqZWN0LmNyZWF0ZShudWxsKSA6IGFubm90YXRpb25zO1xuICAgICAgICB0aGlzLl9jb3VudGVyID0gMDtcbiAgICAgICAgdGhpcy5fc2l6ZSA9IDA7XG4gICAgfVxuICAgIGFsbCgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX2Fubm90YXRpb25zO1xuICAgIH1cbiAgICBnZXQgc2l6ZSgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX3NpemU7XG4gICAgfVxuICAgIG1hbmFnZShpZE9yQW5ub3RhdGlvbiwgYW5ub3RhdGlvbikge1xuICAgICAgICBsZXQgaWQ7XG4gICAgICAgIGlmIChDaGFuZ2VBbm5vdGF0aW9uSWRlbnRpZmllci5pcyhpZE9yQW5ub3RhdGlvbikpIHtcbiAgICAgICAgICAgIGlkID0gaWRPckFubm90YXRpb247XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBpZCA9IHRoaXMubmV4dElkKCk7XG4gICAgICAgICAgICBhbm5vdGF0aW9uID0gaWRPckFubm90YXRpb247XG4gICAgICAgIH1cbiAgICAgICAgaWYgKHRoaXMuX2Fubm90YXRpb25zW2lkXSAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYElkICR7aWR9IGlzIGFscmVhZHkgaW4gdXNlLmApO1xuICAgICAgICB9XG4gICAgICAgIGlmIChhbm5vdGF0aW9uID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgTm8gYW5ub3RhdGlvbiBwcm92aWRlZCBmb3IgaWQgJHtpZH1gKTtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLl9hbm5vdGF0aW9uc1tpZF0gPSBhbm5vdGF0aW9uO1xuICAgICAgICB0aGlzLl9zaXplKys7XG4gICAgICAgIHJldHVybiBpZDtcbiAgICB9XG4gICAgbmV4dElkKCkge1xuICAgICAgICB0aGlzLl9jb3VudGVyKys7XG4gICAgICAgIHJldHVybiB0aGlzLl9jb3VudGVyLnRvU3RyaW5nKCk7XG4gICAgfVxufVxuLyoqXG4gKiBBIHdvcmtzcGFjZSBjaGFuZ2UgaGVscHMgY29uc3RydWN0aW5nIGNoYW5nZXMgdG8gYSB3b3Jrc3BhY2UuXG4gKi9cbmV4cG9ydCBjbGFzcyBXb3Jrc3BhY2VDaGFuZ2Uge1xuICAgIGNvbnN0cnVjdG9yKHdvcmtzcGFjZUVkaXQpIHtcbiAgICAgICAgdGhpcy5fdGV4dEVkaXRDaGFuZ2VzID0gT2JqZWN0LmNyZWF0ZShudWxsKTtcbiAgICAgICAgaWYgKHdvcmtzcGFjZUVkaXQgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgdGhpcy5fd29ya3NwYWNlRWRpdCA9IHdvcmtzcGFjZUVkaXQ7XG4gICAgICAgICAgICBpZiAod29ya3NwYWNlRWRpdC5kb2N1bWVudENoYW5nZXMpIHtcbiAgICAgICAgICAgICAgICB0aGlzLl9jaGFuZ2VBbm5vdGF0aW9ucyA9IG5ldyBDaGFuZ2VBbm5vdGF0aW9ucyh3b3Jrc3BhY2VFZGl0LmNoYW5nZUFubm90YXRpb25zKTtcbiAgICAgICAgICAgICAgICB3b3Jrc3BhY2VFZGl0LmNoYW5nZUFubm90YXRpb25zID0gdGhpcy5fY2hhbmdlQW5ub3RhdGlvbnMuYWxsKCk7XG4gICAgICAgICAgICAgICAgd29ya3NwYWNlRWRpdC5kb2N1bWVudENoYW5nZXMuZm9yRWFjaCgoY2hhbmdlKSA9PiB7XG4gICAgICAgICAgICAgICAgICAgIGlmIChUZXh0RG9jdW1lbnRFZGl0LmlzKGNoYW5nZSkpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGNvbnN0IHRleHRFZGl0Q2hhbmdlID0gbmV3IFRleHRFZGl0Q2hhbmdlSW1wbChjaGFuZ2UuZWRpdHMsIHRoaXMuX2NoYW5nZUFubm90YXRpb25zKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIHRoaXMuX3RleHRFZGl0Q2hhbmdlc1tjaGFuZ2UudGV4dERvY3VtZW50LnVyaV0gPSB0ZXh0RWRpdENoYW5nZTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSBpZiAod29ya3NwYWNlRWRpdC5jaGFuZ2VzKSB7XG4gICAgICAgICAgICAgICAgT2JqZWN0LmtleXMod29ya3NwYWNlRWRpdC5jaGFuZ2VzKS5mb3JFYWNoKChrZXkpID0+IHtcbiAgICAgICAgICAgICAgICAgICAgY29uc3QgdGV4dEVkaXRDaGFuZ2UgPSBuZXcgVGV4dEVkaXRDaGFuZ2VJbXBsKHdvcmtzcGFjZUVkaXQuY2hhbmdlc1trZXldKTtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5fdGV4dEVkaXRDaGFuZ2VzW2tleV0gPSB0ZXh0RWRpdENoYW5nZTtcbiAgICAgICAgICAgICAgICB9KTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHRoaXMuX3dvcmtzcGFjZUVkaXQgPSB7fTtcbiAgICAgICAgfVxuICAgIH1cbiAgICAvKipcbiAgICAgKiBSZXR1cm5zIHRoZSB1bmRlcmx5aW5nIHtAbGluayBXb3Jrc3BhY2VFZGl0fSBsaXRlcmFsXG4gICAgICogdXNlIHRvIGJlIHJldHVybmVkIGZyb20gYSB3b3Jrc3BhY2UgZWRpdCBvcGVyYXRpb24gbGlrZSByZW5hbWUuXG4gICAgICovXG4gICAgZ2V0IGVkaXQoKSB7XG4gICAgICAgIHRoaXMuaW5pdERvY3VtZW50Q2hhbmdlcygpO1xuICAgICAgICBpZiAodGhpcy5fY2hhbmdlQW5ub3RhdGlvbnMgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgaWYgKHRoaXMuX2NoYW5nZUFubm90YXRpb25zLnNpemUgPT09IDApIHtcbiAgICAgICAgICAgICAgICB0aGlzLl93b3Jrc3BhY2VFZGl0LmNoYW5nZUFubm90YXRpb25zID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgdGhpcy5fd29ya3NwYWNlRWRpdC5jaGFuZ2VBbm5vdGF0aW9ucyA9IHRoaXMuX2NoYW5nZUFubm90YXRpb25zLmFsbCgpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIHJldHVybiB0aGlzLl93b3Jrc3BhY2VFZGl0O1xuICAgIH1cbiAgICBnZXRUZXh0RWRpdENoYW5nZShrZXkpIHtcbiAgICAgICAgaWYgKE9wdGlvbmFsVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllci5pcyhrZXkpKSB7XG4gICAgICAgICAgICB0aGlzLmluaXREb2N1bWVudENoYW5nZXMoKTtcbiAgICAgICAgICAgIGlmICh0aGlzLl93b3Jrc3BhY2VFZGl0LmRvY3VtZW50Q2hhbmdlcyA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdXb3Jrc3BhY2UgZWRpdCBpcyBub3QgY29uZmlndXJlZCBmb3IgZG9jdW1lbnQgY2hhbmdlcy4nKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNvbnN0IHRleHREb2N1bWVudCA9IHsgdXJpOiBrZXkudXJpLCB2ZXJzaW9uOiBrZXkudmVyc2lvbiB9O1xuICAgICAgICAgICAgbGV0IHJlc3VsdCA9IHRoaXMuX3RleHRFZGl0Q2hhbmdlc1t0ZXh0RG9jdW1lbnQudXJpXTtcbiAgICAgICAgICAgIGlmICghcmVzdWx0KSB7XG4gICAgICAgICAgICAgICAgY29uc3QgZWRpdHMgPSBbXTtcbiAgICAgICAgICAgICAgICBjb25zdCB0ZXh0RG9jdW1lbnRFZGl0ID0ge1xuICAgICAgICAgICAgICAgICAgICB0ZXh0RG9jdW1lbnQsXG4gICAgICAgICAgICAgICAgICAgIGVkaXRzXG4gICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgICAgICB0aGlzLl93b3Jrc3BhY2VFZGl0LmRvY3VtZW50Q2hhbmdlcy5wdXNoKHRleHREb2N1bWVudEVkaXQpO1xuICAgICAgICAgICAgICAgIHJlc3VsdCA9IG5ldyBUZXh0RWRpdENoYW5nZUltcGwoZWRpdHMsIHRoaXMuX2NoYW5nZUFubm90YXRpb25zKTtcbiAgICAgICAgICAgICAgICB0aGlzLl90ZXh0RWRpdENoYW5nZXNbdGV4dERvY3VtZW50LnVyaV0gPSByZXN1bHQ7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgdGhpcy5pbml0Q2hhbmdlcygpO1xuICAgICAgICAgICAgaWYgKHRoaXMuX3dvcmtzcGFjZUVkaXQuY2hhbmdlcyA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdXb3Jrc3BhY2UgZWRpdCBpcyBub3QgY29uZmlndXJlZCBmb3Igbm9ybWFsIHRleHQgZWRpdCBjaGFuZ2VzLicpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgbGV0IHJlc3VsdCA9IHRoaXMuX3RleHRFZGl0Q2hhbmdlc1trZXldO1xuICAgICAgICAgICAgaWYgKCFyZXN1bHQpIHtcbiAgICAgICAgICAgICAgICBsZXQgZWRpdHMgPSBbXTtcbiAgICAgICAgICAgICAgICB0aGlzLl93b3Jrc3BhY2VFZGl0LmNoYW5nZXNba2V5XSA9IGVkaXRzO1xuICAgICAgICAgICAgICAgIHJlc3VsdCA9IG5ldyBUZXh0RWRpdENoYW5nZUltcGwoZWRpdHMpO1xuICAgICAgICAgICAgICAgIHRoaXMuX3RleHRFZGl0Q2hhbmdlc1trZXldID0gcmVzdWx0O1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICAgICAgfVxuICAgIH1cbiAgICBpbml0RG9jdW1lbnRDaGFuZ2VzKCkge1xuICAgICAgICBpZiAodGhpcy5fd29ya3NwYWNlRWRpdC5kb2N1bWVudENoYW5nZXMgPT09IHVuZGVmaW5lZCAmJiB0aGlzLl93b3Jrc3BhY2VFZGl0LmNoYW5nZXMgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgdGhpcy5fY2hhbmdlQW5ub3RhdGlvbnMgPSBuZXcgQ2hhbmdlQW5ub3RhdGlvbnMoKTtcbiAgICAgICAgICAgIHRoaXMuX3dvcmtzcGFjZUVkaXQuZG9jdW1lbnRDaGFuZ2VzID0gW107XG4gICAgICAgICAgICB0aGlzLl93b3Jrc3BhY2VFZGl0LmNoYW5nZUFubm90YXRpb25zID0gdGhpcy5fY2hhbmdlQW5ub3RhdGlvbnMuYWxsKCk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgaW5pdENoYW5nZXMoKSB7XG4gICAgICAgIGlmICh0aGlzLl93b3Jrc3BhY2VFZGl0LmRvY3VtZW50Q2hhbmdlcyA9PT0gdW5kZWZpbmVkICYmIHRoaXMuX3dvcmtzcGFjZUVkaXQuY2hhbmdlcyA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICB0aGlzLl93b3Jrc3BhY2VFZGl0LmNoYW5nZXMgPSBPYmplY3QuY3JlYXRlKG51bGwpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGNyZWF0ZUZpbGUodXJpLCBvcHRpb25zT3JBbm5vdGF0aW9uLCBvcHRpb25zKSB7XG4gICAgICAgIHRoaXMuaW5pdERvY3VtZW50Q2hhbmdlcygpO1xuICAgICAgICBpZiAodGhpcy5fd29ya3NwYWNlRWRpdC5kb2N1bWVudENoYW5nZXMgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdXb3Jrc3BhY2UgZWRpdCBpcyBub3QgY29uZmlndXJlZCBmb3IgZG9jdW1lbnQgY2hhbmdlcy4nKTtcbiAgICAgICAgfVxuICAgICAgICBsZXQgYW5ub3RhdGlvbjtcbiAgICAgICAgaWYgKENoYW5nZUFubm90YXRpb24uaXMob3B0aW9uc09yQW5ub3RhdGlvbikgfHwgQ2hhbmdlQW5ub3RhdGlvbklkZW50aWZpZXIuaXMob3B0aW9uc09yQW5ub3RhdGlvbikpIHtcbiAgICAgICAgICAgIGFubm90YXRpb24gPSBvcHRpb25zT3JBbm5vdGF0aW9uO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgb3B0aW9ucyA9IG9wdGlvbnNPckFubm90YXRpb247XG4gICAgICAgIH1cbiAgICAgICAgbGV0IG9wZXJhdGlvbjtcbiAgICAgICAgbGV0IGlkO1xuICAgICAgICBpZiAoYW5ub3RhdGlvbiA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICBvcGVyYXRpb24gPSBDcmVhdGVGaWxlLmNyZWF0ZSh1cmksIG9wdGlvbnMpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgaWQgPSBDaGFuZ2VBbm5vdGF0aW9uSWRlbnRpZmllci5pcyhhbm5vdGF0aW9uKSA/IGFubm90YXRpb24gOiB0aGlzLl9jaGFuZ2VBbm5vdGF0aW9ucy5tYW5hZ2UoYW5ub3RhdGlvbik7XG4gICAgICAgICAgICBvcGVyYXRpb24gPSBDcmVhdGVGaWxlLmNyZWF0ZSh1cmksIG9wdGlvbnMsIGlkKTtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLl93b3Jrc3BhY2VFZGl0LmRvY3VtZW50Q2hhbmdlcy5wdXNoKG9wZXJhdGlvbik7XG4gICAgICAgIGlmIChpZCAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXR1cm4gaWQ7XG4gICAgICAgIH1cbiAgICB9XG4gICAgcmVuYW1lRmlsZShvbGRVcmksIG5ld1VyaSwgb3B0aW9uc09yQW5ub3RhdGlvbiwgb3B0aW9ucykge1xuICAgICAgICB0aGlzLmluaXREb2N1bWVudENoYW5nZXMoKTtcbiAgICAgICAgaWYgKHRoaXMuX3dvcmtzcGFjZUVkaXQuZG9jdW1lbnRDaGFuZ2VzID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcignV29ya3NwYWNlIGVkaXQgaXMgbm90IGNvbmZpZ3VyZWQgZm9yIGRvY3VtZW50IGNoYW5nZXMuJyk7XG4gICAgICAgIH1cbiAgICAgICAgbGV0IGFubm90YXRpb247XG4gICAgICAgIGlmIChDaGFuZ2VBbm5vdGF0aW9uLmlzKG9wdGlvbnNPckFubm90YXRpb24pIHx8IENoYW5nZUFubm90YXRpb25JZGVudGlmaWVyLmlzKG9wdGlvbnNPckFubm90YXRpb24pKSB7XG4gICAgICAgICAgICBhbm5vdGF0aW9uID0gb3B0aW9uc09yQW5ub3RhdGlvbjtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIG9wdGlvbnMgPSBvcHRpb25zT3JBbm5vdGF0aW9uO1xuICAgICAgICB9XG4gICAgICAgIGxldCBvcGVyYXRpb247XG4gICAgICAgIGxldCBpZDtcbiAgICAgICAgaWYgKGFubm90YXRpb24gPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgb3BlcmF0aW9uID0gUmVuYW1lRmlsZS5jcmVhdGUob2xkVXJpLCBuZXdVcmksIG9wdGlvbnMpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgaWQgPSBDaGFuZ2VBbm5vdGF0aW9uSWRlbnRpZmllci5pcyhhbm5vdGF0aW9uKSA/IGFubm90YXRpb24gOiB0aGlzLl9jaGFuZ2VBbm5vdGF0aW9ucy5tYW5hZ2UoYW5ub3RhdGlvbik7XG4gICAgICAgICAgICBvcGVyYXRpb24gPSBSZW5hbWVGaWxlLmNyZWF0ZShvbGRVcmksIG5ld1VyaSwgb3B0aW9ucywgaWQpO1xuICAgICAgICB9XG4gICAgICAgIHRoaXMuX3dvcmtzcGFjZUVkaXQuZG9jdW1lbnRDaGFuZ2VzLnB1c2gob3BlcmF0aW9uKTtcbiAgICAgICAgaWYgKGlkICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHJldHVybiBpZDtcbiAgICAgICAgfVxuICAgIH1cbiAgICBkZWxldGVGaWxlKHVyaSwgb3B0aW9uc09yQW5ub3RhdGlvbiwgb3B0aW9ucykge1xuICAgICAgICB0aGlzLmluaXREb2N1bWVudENoYW5nZXMoKTtcbiAgICAgICAgaWYgKHRoaXMuX3dvcmtzcGFjZUVkaXQuZG9jdW1lbnRDaGFuZ2VzID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcignV29ya3NwYWNlIGVkaXQgaXMgbm90IGNvbmZpZ3VyZWQgZm9yIGRvY3VtZW50IGNoYW5nZXMuJyk7XG4gICAgICAgIH1cbiAgICAgICAgbGV0IGFubm90YXRpb247XG4gICAgICAgIGlmIChDaGFuZ2VBbm5vdGF0aW9uLmlzKG9wdGlvbnNPckFubm90YXRpb24pIHx8IENoYW5nZUFubm90YXRpb25JZGVudGlmaWVyLmlzKG9wdGlvbnNPckFubm90YXRpb24pKSB7XG4gICAgICAgICAgICBhbm5vdGF0aW9uID0gb3B0aW9uc09yQW5ub3RhdGlvbjtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIG9wdGlvbnMgPSBvcHRpb25zT3JBbm5vdGF0aW9uO1xuICAgICAgICB9XG4gICAgICAgIGxldCBvcGVyYXRpb247XG4gICAgICAgIGxldCBpZDtcbiAgICAgICAgaWYgKGFubm90YXRpb24gPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgb3BlcmF0aW9uID0gRGVsZXRlRmlsZS5jcmVhdGUodXJpLCBvcHRpb25zKTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIGlkID0gQ2hhbmdlQW5ub3RhdGlvbklkZW50aWZpZXIuaXMoYW5ub3RhdGlvbikgPyBhbm5vdGF0aW9uIDogdGhpcy5fY2hhbmdlQW5ub3RhdGlvbnMubWFuYWdlKGFubm90YXRpb24pO1xuICAgICAgICAgICAgb3BlcmF0aW9uID0gRGVsZXRlRmlsZS5jcmVhdGUodXJpLCBvcHRpb25zLCBpZCk7XG4gICAgICAgIH1cbiAgICAgICAgdGhpcy5fd29ya3NwYWNlRWRpdC5kb2N1bWVudENoYW5nZXMucHVzaChvcGVyYXRpb24pO1xuICAgICAgICBpZiAoaWQgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmV0dXJuIGlkO1xuICAgICAgICB9XG4gICAgfVxufVxuLyoqXG4gKiBUaGUgVGV4dERvY3VtZW50SWRlbnRpZmllciBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBUZXh0RG9jdW1lbnRJZGVudGlmaWVyfSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBUZXh0RG9jdW1lbnRJZGVudGlmaWVyO1xuKGZ1bmN0aW9uIChUZXh0RG9jdW1lbnRJZGVudGlmaWVyKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBUZXh0RG9jdW1lbnRJZGVudGlmaWVyIGxpdGVyYWwuXG4gICAgICogQHBhcmFtIHVyaSBUaGUgZG9jdW1lbnQncyB1cmkuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKHVyaSkge1xuICAgICAgICByZXR1cm4geyB1cmkgfTtcbiAgICB9XG4gICAgVGV4dERvY3VtZW50SWRlbnRpZmllci5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBUZXh0RG9jdW1lbnRJZGVudGlmaWVyfSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMuZGVmaW5lZChjYW5kaWRhdGUpICYmIElzLnN0cmluZyhjYW5kaWRhdGUudXJpKTtcbiAgICB9XG4gICAgVGV4dERvY3VtZW50SWRlbnRpZmllci5pcyA9IGlzO1xufSkoVGV4dERvY3VtZW50SWRlbnRpZmllciB8fCAoVGV4dERvY3VtZW50SWRlbnRpZmllciA9IHt9KSk7XG4vKipcbiAqIFRoZSBWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyIG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXJ9IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXI7XG4oZnVuY3Rpb24gKFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXIpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXIgbGl0ZXJhbC5cbiAgICAgKiBAcGFyYW0gdXJpIFRoZSBkb2N1bWVudCdzIHVyaS5cbiAgICAgKiBAcGFyYW0gdmVyc2lvbiBUaGUgZG9jdW1lbnQncyB2ZXJzaW9uLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZSh1cmksIHZlcnNpb24pIHtcbiAgICAgICAgcmV0dXJuIHsgdXJpLCB2ZXJzaW9uIH07XG4gICAgfVxuICAgIFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXIuY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllcn0gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLmRlZmluZWQoY2FuZGlkYXRlKSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLnVyaSkgJiYgSXMuaW50ZWdlcihjYW5kaWRhdGUudmVyc2lvbik7XG4gICAgfVxuICAgIFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXIuaXMgPSBpcztcbn0pKFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXIgfHwgKFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXIgPSB7fSkpO1xuLyoqXG4gKiBUaGUgT3B0aW9uYWxWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyIG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIE9wdGlvbmFsVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllcn0gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgT3B0aW9uYWxWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyO1xuKGZ1bmN0aW9uIChPcHRpb25hbFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXIpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IE9wdGlvbmFsVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllciBsaXRlcmFsLlxuICAgICAqIEBwYXJhbSB1cmkgVGhlIGRvY3VtZW50J3MgdXJpLlxuICAgICAqIEBwYXJhbSB2ZXJzaW9uIFRoZSBkb2N1bWVudCdzIHZlcnNpb24uXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKHVyaSwgdmVyc2lvbikge1xuICAgICAgICByZXR1cm4geyB1cmksIHZlcnNpb24gfTtcbiAgICB9XG4gICAgT3B0aW9uYWxWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIE9wdGlvbmFsVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllcn0gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLmRlZmluZWQoY2FuZGlkYXRlKSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLnVyaSkgJiYgKGNhbmRpZGF0ZS52ZXJzaW9uID09PSBudWxsIHx8IElzLmludGVnZXIoY2FuZGlkYXRlLnZlcnNpb24pKTtcbiAgICB9XG4gICAgT3B0aW9uYWxWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyLmlzID0gaXM7XG59KShPcHRpb25hbFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXIgfHwgKE9wdGlvbmFsVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllciA9IHt9KSk7XG4vKipcbiAqIFRoZSBUZXh0RG9jdW1lbnRJdGVtIG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIFRleHREb2N1bWVudEl0ZW19IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIFRleHREb2N1bWVudEl0ZW07XG4oZnVuY3Rpb24gKFRleHREb2N1bWVudEl0ZW0pIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IFRleHREb2N1bWVudEl0ZW0gbGl0ZXJhbC5cbiAgICAgKiBAcGFyYW0gdXJpIFRoZSBkb2N1bWVudCdzIHVyaS5cbiAgICAgKiBAcGFyYW0gbGFuZ3VhZ2VJZCBUaGUgZG9jdW1lbnQncyBsYW5ndWFnZSBpZGVudGlmaWVyLlxuICAgICAqIEBwYXJhbSB2ZXJzaW9uIFRoZSBkb2N1bWVudCdzIHZlcnNpb24gbnVtYmVyLlxuICAgICAqIEBwYXJhbSB0ZXh0IFRoZSBkb2N1bWVudCdzIHRleHQuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKHVyaSwgbGFuZ3VhZ2VJZCwgdmVyc2lvbiwgdGV4dCkge1xuICAgICAgICByZXR1cm4geyB1cmksIGxhbmd1YWdlSWQsIHZlcnNpb24sIHRleHQgfTtcbiAgICB9XG4gICAgVGV4dERvY3VtZW50SXRlbS5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBUZXh0RG9jdW1lbnRJdGVtfSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMuZGVmaW5lZChjYW5kaWRhdGUpICYmIElzLnN0cmluZyhjYW5kaWRhdGUudXJpKSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLmxhbmd1YWdlSWQpICYmIElzLmludGVnZXIoY2FuZGlkYXRlLnZlcnNpb24pICYmIElzLnN0cmluZyhjYW5kaWRhdGUudGV4dCk7XG4gICAgfVxuICAgIFRleHREb2N1bWVudEl0ZW0uaXMgPSBpcztcbn0pKFRleHREb2N1bWVudEl0ZW0gfHwgKFRleHREb2N1bWVudEl0ZW0gPSB7fSkpO1xuLyoqXG4gKiBEZXNjcmliZXMgdGhlIGNvbnRlbnQgdHlwZSB0aGF0IGEgY2xpZW50IHN1cHBvcnRzIGluIHZhcmlvdXNcbiAqIHJlc3VsdCBsaXRlcmFscyBsaWtlIGBIb3ZlcmAsIGBQYXJhbWV0ZXJJbmZvYCBvciBgQ29tcGxldGlvbkl0ZW1gLlxuICpcbiAqIFBsZWFzZSBub3RlIHRoYXQgYE1hcmt1cEtpbmRzYCBtdXN0IG5vdCBzdGFydCB3aXRoIGEgYCRgLiBUaGlzIGtpbmRzXG4gKiBhcmUgcmVzZXJ2ZWQgZm9yIGludGVybmFsIHVzYWdlLlxuICovXG5leHBvcnQgdmFyIE1hcmt1cEtpbmQ7XG4oZnVuY3Rpb24gKE1hcmt1cEtpbmQpIHtcbiAgICAvKipcbiAgICAgKiBQbGFpbiB0ZXh0IGlzIHN1cHBvcnRlZCBhcyBhIGNvbnRlbnQgZm9ybWF0XG4gICAgICovXG4gICAgTWFya3VwS2luZC5QbGFpblRleHQgPSAncGxhaW50ZXh0JztcbiAgICAvKipcbiAgICAgKiBNYXJrZG93biBpcyBzdXBwb3J0ZWQgYXMgYSBjb250ZW50IGZvcm1hdFxuICAgICAqL1xuICAgIE1hcmt1cEtpbmQuTWFya2Rvd24gPSAnbWFya2Rvd24nO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiB2YWx1ZSBpcyBhIHZhbHVlIG9mIHRoZSB7QGxpbmsgTWFya3VwS2luZH0gdHlwZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSA9PT0gTWFya3VwS2luZC5QbGFpblRleHQgfHwgY2FuZGlkYXRlID09PSBNYXJrdXBLaW5kLk1hcmtkb3duO1xuICAgIH1cbiAgICBNYXJrdXBLaW5kLmlzID0gaXM7XG59KShNYXJrdXBLaW5kIHx8IChNYXJrdXBLaW5kID0ge30pKTtcbmV4cG9ydCB2YXIgTWFya3VwQ29udGVudDtcbihmdW5jdGlvbiAoTWFya3VwQ29udGVudCkge1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiB2YWx1ZSBjb25mb3JtcyB0byB0aGUge0BsaW5rIE1hcmt1cENvbnRlbnR9IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLm9iamVjdExpdGVyYWwodmFsdWUpICYmIE1hcmt1cEtpbmQuaXMoY2FuZGlkYXRlLmtpbmQpICYmIElzLnN0cmluZyhjYW5kaWRhdGUudmFsdWUpO1xuICAgIH1cbiAgICBNYXJrdXBDb250ZW50LmlzID0gaXM7XG59KShNYXJrdXBDb250ZW50IHx8IChNYXJrdXBDb250ZW50ID0ge30pKTtcbi8qKlxuICogVGhlIGtpbmQgb2YgYSBjb21wbGV0aW9uIGVudHJ5LlxuICovXG5leHBvcnQgdmFyIENvbXBsZXRpb25JdGVtS2luZDtcbihmdW5jdGlvbiAoQ29tcGxldGlvbkl0ZW1LaW5kKSB7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLlRleHQgPSAxO1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5NZXRob2QgPSAyO1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5GdW5jdGlvbiA9IDM7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLkNvbnN0cnVjdG9yID0gNDtcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuRmllbGQgPSA1O1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5WYXJpYWJsZSA9IDY7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLkNsYXNzID0gNztcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuSW50ZXJmYWNlID0gODtcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuTW9kdWxlID0gOTtcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuUHJvcGVydHkgPSAxMDtcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuVW5pdCA9IDExO1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5WYWx1ZSA9IDEyO1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5FbnVtID0gMTM7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLktleXdvcmQgPSAxNDtcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuU25pcHBldCA9IDE1O1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5Db2xvciA9IDE2O1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5GaWxlID0gMTc7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLlJlZmVyZW5jZSA9IDE4O1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5Gb2xkZXIgPSAxOTtcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuRW51bU1lbWJlciA9IDIwO1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5Db25zdGFudCA9IDIxO1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5TdHJ1Y3QgPSAyMjtcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuRXZlbnQgPSAyMztcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuT3BlcmF0b3IgPSAyNDtcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuVHlwZVBhcmFtZXRlciA9IDI1O1xufSkoQ29tcGxldGlvbkl0ZW1LaW5kIHx8IChDb21wbGV0aW9uSXRlbUtpbmQgPSB7fSkpO1xuLyoqXG4gKiBEZWZpbmVzIHdoZXRoZXIgdGhlIGluc2VydCB0ZXh0IGluIGEgY29tcGxldGlvbiBpdGVtIHNob3VsZCBiZSBpbnRlcnByZXRlZCBhc1xuICogcGxhaW4gdGV4dCBvciBhIHNuaXBwZXQuXG4gKi9cbmV4cG9ydCB2YXIgSW5zZXJ0VGV4dEZvcm1hdDtcbihmdW5jdGlvbiAoSW5zZXJ0VGV4dEZvcm1hdCkge1xuICAgIC8qKlxuICAgICAqIFRoZSBwcmltYXJ5IHRleHQgdG8gYmUgaW5zZXJ0ZWQgaXMgdHJlYXRlZCBhcyBhIHBsYWluIHN0cmluZy5cbiAgICAgKi9cbiAgICBJbnNlcnRUZXh0Rm9ybWF0LlBsYWluVGV4dCA9IDE7XG4gICAgLyoqXG4gICAgICogVGhlIHByaW1hcnkgdGV4dCB0byBiZSBpbnNlcnRlZCBpcyB0cmVhdGVkIGFzIGEgc25pcHBldC5cbiAgICAgKlxuICAgICAqIEEgc25pcHBldCBjYW4gZGVmaW5lIHRhYiBzdG9wcyBhbmQgcGxhY2Vob2xkZXJzIHdpdGggYCQxYCwgYCQyYFxuICAgICAqIGFuZCBgJHszOmZvb31gLiBgJDBgIGRlZmluZXMgdGhlIGZpbmFsIHRhYiBzdG9wLCBpdCBkZWZhdWx0cyB0b1xuICAgICAqIHRoZSBlbmQgb2YgdGhlIHNuaXBwZXQuIFBsYWNlaG9sZGVycyB3aXRoIGVxdWFsIGlkZW50aWZpZXJzIGFyZSBsaW5rZWQsXG4gICAgICogdGhhdCBpcyB0eXBpbmcgaW4gb25lIHdpbGwgdXBkYXRlIG90aGVycyB0b28uXG4gICAgICpcbiAgICAgKiBTZWUgYWxzbzogaHR0cHM6Ly9taWNyb3NvZnQuZ2l0aHViLmlvL2xhbmd1YWdlLXNlcnZlci1wcm90b2NvbC9zcGVjaWZpY2F0aW9ucy9zcGVjaWZpY2F0aW9uLWN1cnJlbnQvI3NuaXBwZXRfc3ludGF4XG4gICAgICovXG4gICAgSW5zZXJ0VGV4dEZvcm1hdC5TbmlwcGV0ID0gMjtcbn0pKEluc2VydFRleHRGb3JtYXQgfHwgKEluc2VydFRleHRGb3JtYXQgPSB7fSkpO1xuLyoqXG4gKiBDb21wbGV0aW9uIGl0ZW0gdGFncyBhcmUgZXh0cmEgYW5ub3RhdGlvbnMgdGhhdCB0d2VhayB0aGUgcmVuZGVyaW5nIG9mIGEgY29tcGxldGlvblxuICogaXRlbS5cbiAqXG4gKiBAc2luY2UgMy4xNS4wXG4gKi9cbmV4cG9ydCB2YXIgQ29tcGxldGlvbkl0ZW1UYWc7XG4oZnVuY3Rpb24gKENvbXBsZXRpb25JdGVtVGFnKSB7XG4gICAgLyoqXG4gICAgICogUmVuZGVyIGEgY29tcGxldGlvbiBhcyBvYnNvbGV0ZSwgdXN1YWxseSB1c2luZyBhIHN0cmlrZS1vdXQuXG4gICAgICovXG4gICAgQ29tcGxldGlvbkl0ZW1UYWcuRGVwcmVjYXRlZCA9IDE7XG59KShDb21wbGV0aW9uSXRlbVRhZyB8fCAoQ29tcGxldGlvbkl0ZW1UYWcgPSB7fSkpO1xuLyoqXG4gKiBUaGUgSW5zZXJ0UmVwbGFjZUVkaXQgbmFtZXNwYWNlIHByb3ZpZGVzIGZ1bmN0aW9ucyB0byBkZWFsIHdpdGggaW5zZXJ0IC8gcmVwbGFjZSBlZGl0cy5cbiAqXG4gKiBAc2luY2UgMy4xNi4wXG4gKi9cbmV4cG9ydCB2YXIgSW5zZXJ0UmVwbGFjZUVkaXQ7XG4oZnVuY3Rpb24gKEluc2VydFJlcGxhY2VFZGl0KSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBpbnNlcnQgLyByZXBsYWNlIGVkaXRcbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUobmV3VGV4dCwgaW5zZXJ0LCByZXBsYWNlKSB7XG4gICAgICAgIHJldHVybiB7IG5ld1RleHQsIGluc2VydCwgcmVwbGFjZSB9O1xuICAgIH1cbiAgICBJbnNlcnRSZXBsYWNlRWRpdC5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBJbnNlcnRSZXBsYWNlRWRpdH0gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICYmIElzLnN0cmluZyhjYW5kaWRhdGUubmV3VGV4dCkgJiYgUmFuZ2UuaXMoY2FuZGlkYXRlLmluc2VydCkgJiYgUmFuZ2UuaXMoY2FuZGlkYXRlLnJlcGxhY2UpO1xuICAgIH1cbiAgICBJbnNlcnRSZXBsYWNlRWRpdC5pcyA9IGlzO1xufSkoSW5zZXJ0UmVwbGFjZUVkaXQgfHwgKEluc2VydFJlcGxhY2VFZGl0ID0ge30pKTtcbi8qKlxuICogSG93IHdoaXRlc3BhY2UgYW5kIGluZGVudGF0aW9uIGlzIGhhbmRsZWQgZHVyaW5nIGNvbXBsZXRpb25cbiAqIGl0ZW0gaW5zZXJ0aW9uLlxuICpcbiAqIEBzaW5jZSAzLjE2LjBcbiAqL1xuZXhwb3J0IHZhciBJbnNlcnRUZXh0TW9kZTtcbihmdW5jdGlvbiAoSW5zZXJ0VGV4dE1vZGUpIHtcbiAgICAvKipcbiAgICAgKiBUaGUgaW5zZXJ0aW9uIG9yIHJlcGxhY2Ugc3RyaW5ncyBpcyB0YWtlbiBhcyBpdCBpcy4gSWYgdGhlXG4gICAgICogdmFsdWUgaXMgbXVsdGkgbGluZSB0aGUgbGluZXMgYmVsb3cgdGhlIGN1cnNvciB3aWxsIGJlXG4gICAgICogaW5zZXJ0ZWQgdXNpbmcgdGhlIGluZGVudGF0aW9uIGRlZmluZWQgaW4gdGhlIHN0cmluZyB2YWx1ZS5cbiAgICAgKiBUaGUgY2xpZW50IHdpbGwgbm90IGFwcGx5IGFueSBraW5kIG9mIGFkanVzdG1lbnRzIHRvIHRoZVxuICAgICAqIHN0cmluZy5cbiAgICAgKi9cbiAgICBJbnNlcnRUZXh0TW9kZS5hc0lzID0gMTtcbiAgICAvKipcbiAgICAgKiBUaGUgZWRpdG9yIGFkanVzdHMgbGVhZGluZyB3aGl0ZXNwYWNlIG9mIG5ldyBsaW5lcyBzbyB0aGF0XG4gICAgICogdGhleSBtYXRjaCB0aGUgaW5kZW50YXRpb24gdXAgdG8gdGhlIGN1cnNvciBvZiB0aGUgbGluZSBmb3JcbiAgICAgKiB3aGljaCB0aGUgaXRlbSBpcyBhY2NlcHRlZC5cbiAgICAgKlxuICAgICAqIENvbnNpZGVyIGEgbGluZSBsaWtlIHRoaXM6IDwydGFicz48Y3Vyc29yPjwzdGFicz5mb28uIEFjY2VwdGluZyBhXG4gICAgICogbXVsdGkgbGluZSBjb21wbGV0aW9uIGl0ZW0gaXMgaW5kZW50ZWQgdXNpbmcgMiB0YWJzIGFuZCBhbGxcbiAgICAgKiBmb2xsb3dpbmcgbGluZXMgaW5zZXJ0ZWQgd2lsbCBiZSBpbmRlbnRlZCB1c2luZyAyIHRhYnMgYXMgd2VsbC5cbiAgICAgKi9cbiAgICBJbnNlcnRUZXh0TW9kZS5hZGp1c3RJbmRlbnRhdGlvbiA9IDI7XG59KShJbnNlcnRUZXh0TW9kZSB8fCAoSW5zZXJ0VGV4dE1vZGUgPSB7fSkpO1xuZXhwb3J0IHZhciBDb21wbGV0aW9uSXRlbUxhYmVsRGV0YWlscztcbihmdW5jdGlvbiAoQ29tcGxldGlvbkl0ZW1MYWJlbERldGFpbHMpIHtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiAoSXMuc3RyaW5nKGNhbmRpZGF0ZS5kZXRhaWwpIHx8IGNhbmRpZGF0ZS5kZXRhaWwgPT09IHVuZGVmaW5lZCkgJiZcbiAgICAgICAgICAgIChJcy5zdHJpbmcoY2FuZGlkYXRlLmRlc2NyaXB0aW9uKSB8fCBjYW5kaWRhdGUuZGVzY3JpcHRpb24gPT09IHVuZGVmaW5lZCk7XG4gICAgfVxuICAgIENvbXBsZXRpb25JdGVtTGFiZWxEZXRhaWxzLmlzID0gaXM7XG59KShDb21wbGV0aW9uSXRlbUxhYmVsRGV0YWlscyB8fCAoQ29tcGxldGlvbkl0ZW1MYWJlbERldGFpbHMgPSB7fSkpO1xuLyoqXG4gKiBUaGUgQ29tcGxldGlvbkl0ZW0gbmFtZXNwYWNlIHByb3ZpZGVzIGZ1bmN0aW9ucyB0byBkZWFsIHdpdGhcbiAqIGNvbXBsZXRpb24gaXRlbXMuXG4gKi9cbmV4cG9ydCB2YXIgQ29tcGxldGlvbkl0ZW07XG4oZnVuY3Rpb24gKENvbXBsZXRpb25JdGVtKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlIGEgY29tcGxldGlvbiBpdGVtIGFuZCBzZWVkIGl0IHdpdGggYSBsYWJlbC5cbiAgICAgKiBAcGFyYW0gbGFiZWwgVGhlIGNvbXBsZXRpb24gaXRlbSdzIGxhYmVsXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKGxhYmVsKSB7XG4gICAgICAgIHJldHVybiB7IGxhYmVsIH07XG4gICAgfVxuICAgIENvbXBsZXRpb25JdGVtLmNyZWF0ZSA9IGNyZWF0ZTtcbn0pKENvbXBsZXRpb25JdGVtIHx8IChDb21wbGV0aW9uSXRlbSA9IHt9KSk7XG4vKipcbiAqIFRoZSBDb21wbGV0aW9uTGlzdCBuYW1lc3BhY2UgcHJvdmlkZXMgZnVuY3Rpb25zIHRvIGRlYWwgd2l0aFxuICogY29tcGxldGlvbiBsaXN0cy5cbiAqL1xuZXhwb3J0IHZhciBDb21wbGV0aW9uTGlzdDtcbihmdW5jdGlvbiAoQ29tcGxldGlvbkxpc3QpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IGNvbXBsZXRpb24gbGlzdC5cbiAgICAgKlxuICAgICAqIEBwYXJhbSBpdGVtcyBUaGUgY29tcGxldGlvbiBpdGVtcy5cbiAgICAgKiBAcGFyYW0gaXNJbmNvbXBsZXRlIFRoZSBsaXN0IGlzIG5vdCBjb21wbGV0ZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUoaXRlbXMsIGlzSW5jb21wbGV0ZSkge1xuICAgICAgICByZXR1cm4geyBpdGVtczogaXRlbXMgPyBpdGVtcyA6IFtdLCBpc0luY29tcGxldGU6ICEhaXNJbmNvbXBsZXRlIH07XG4gICAgfVxuICAgIENvbXBsZXRpb25MaXN0LmNyZWF0ZSA9IGNyZWF0ZTtcbn0pKENvbXBsZXRpb25MaXN0IHx8IChDb21wbGV0aW9uTGlzdCA9IHt9KSk7XG5leHBvcnQgdmFyIE1hcmtlZFN0cmluZztcbihmdW5jdGlvbiAoTWFya2VkU3RyaW5nKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG1hcmtlZCBzdHJpbmcgZnJvbSBwbGFpbiB0ZXh0LlxuICAgICAqXG4gICAgICogQHBhcmFtIHBsYWluVGV4dCBUaGUgcGxhaW4gdGV4dC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBmcm9tUGxhaW5UZXh0KHBsYWluVGV4dCkge1xuICAgICAgICByZXR1cm4gcGxhaW5UZXh0LnJlcGxhY2UoL1tcXFxcYCpfe31bXFxdKCkjK1xcLS4hXS9nLCAnXFxcXCQmJyk7IC8vIGVzY2FwZSBtYXJrZG93biBzeW50YXggdG9rZW5zOiBodHRwOi8vZGFyaW5nZmlyZWJhbGwubmV0L3Byb2plY3RzL21hcmtkb3duL3N5bnRheCNiYWNrc2xhc2hcbiAgICB9XG4gICAgTWFya2VkU3RyaW5nLmZyb21QbGFpblRleHQgPSBmcm9tUGxhaW5UZXh0O1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiB2YWx1ZSBjb25mb3JtcyB0byB0aGUge0BsaW5rIE1hcmtlZFN0cmluZ30gdHlwZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLnN0cmluZyhjYW5kaWRhdGUpIHx8IChJcy5vYmplY3RMaXRlcmFsKGNhbmRpZGF0ZSkgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS5sYW5ndWFnZSkgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS52YWx1ZSkpO1xuICAgIH1cbiAgICBNYXJrZWRTdHJpbmcuaXMgPSBpcztcbn0pKE1hcmtlZFN0cmluZyB8fCAoTWFya2VkU3RyaW5nID0ge30pKTtcbmV4cG9ydCB2YXIgSG92ZXI7XG4oZnVuY3Rpb24gKEhvdmVyKSB7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIHZhbHVlIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgSG92ZXJ9IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiAhIWNhbmRpZGF0ZSAmJiBJcy5vYmplY3RMaXRlcmFsKGNhbmRpZGF0ZSkgJiYgKE1hcmt1cENvbnRlbnQuaXMoY2FuZGlkYXRlLmNvbnRlbnRzKSB8fFxuICAgICAgICAgICAgTWFya2VkU3RyaW5nLmlzKGNhbmRpZGF0ZS5jb250ZW50cykgfHxcbiAgICAgICAgICAgIElzLnR5cGVkQXJyYXkoY2FuZGlkYXRlLmNvbnRlbnRzLCBNYXJrZWRTdHJpbmcuaXMpKSAmJiAodmFsdWUucmFuZ2UgPT09IHVuZGVmaW5lZCB8fCBSYW5nZS5pcyh2YWx1ZS5yYW5nZSkpO1xuICAgIH1cbiAgICBIb3Zlci5pcyA9IGlzO1xufSkoSG92ZXIgfHwgKEhvdmVyID0ge30pKTtcbi8qKlxuICogVGhlIFBhcmFtZXRlckluZm9ybWF0aW9uIG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIFBhcmFtZXRlckluZm9ybWF0aW9ufSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBQYXJhbWV0ZXJJbmZvcm1hdGlvbjtcbihmdW5jdGlvbiAoUGFyYW1ldGVySW5mb3JtYXRpb24pIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IHBhcmFtZXRlciBpbmZvcm1hdGlvbiBsaXRlcmFsLlxuICAgICAqXG4gICAgICogQHBhcmFtIGxhYmVsIEEgbGFiZWwgc3RyaW5nLlxuICAgICAqIEBwYXJhbSBkb2N1bWVudGF0aW9uIEEgZG9jIHN0cmluZy5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUobGFiZWwsIGRvY3VtZW50YXRpb24pIHtcbiAgICAgICAgcmV0dXJuIGRvY3VtZW50YXRpb24gPyB7IGxhYmVsLCBkb2N1bWVudGF0aW9uIH0gOiB7IGxhYmVsIH07XG4gICAgfVxuICAgIFBhcmFtZXRlckluZm9ybWF0aW9uLmNyZWF0ZSA9IGNyZWF0ZTtcbn0pKFBhcmFtZXRlckluZm9ybWF0aW9uIHx8IChQYXJhbWV0ZXJJbmZvcm1hdGlvbiA9IHt9KSk7XG4vKipcbiAqIFRoZSBTaWduYXR1cmVJbmZvcm1hdGlvbiBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBTaWduYXR1cmVJbmZvcm1hdGlvbn0gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgU2lnbmF0dXJlSW5mb3JtYXRpb247XG4oZnVuY3Rpb24gKFNpZ25hdHVyZUluZm9ybWF0aW9uKSB7XG4gICAgZnVuY3Rpb24gY3JlYXRlKGxhYmVsLCBkb2N1bWVudGF0aW9uLCAuLi5wYXJhbWV0ZXJzKSB7XG4gICAgICAgIGxldCByZXN1bHQgPSB7IGxhYmVsIH07XG4gICAgICAgIGlmIChJcy5kZWZpbmVkKGRvY3VtZW50YXRpb24pKSB7XG4gICAgICAgICAgICByZXN1bHQuZG9jdW1lbnRhdGlvbiA9IGRvY3VtZW50YXRpb247XG4gICAgICAgIH1cbiAgICAgICAgaWYgKElzLmRlZmluZWQocGFyYW1ldGVycykpIHtcbiAgICAgICAgICAgIHJlc3VsdC5wYXJhbWV0ZXJzID0gcGFyYW1ldGVycztcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHJlc3VsdC5wYXJhbWV0ZXJzID0gW107XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICB9XG4gICAgU2lnbmF0dXJlSW5mb3JtYXRpb24uY3JlYXRlID0gY3JlYXRlO1xufSkoU2lnbmF0dXJlSW5mb3JtYXRpb24gfHwgKFNpZ25hdHVyZUluZm9ybWF0aW9uID0ge30pKTtcbi8qKlxuICogQSBkb2N1bWVudCBoaWdobGlnaHQga2luZC5cbiAqL1xuZXhwb3J0IHZhciBEb2N1bWVudEhpZ2hsaWdodEtpbmQ7XG4oZnVuY3Rpb24gKERvY3VtZW50SGlnaGxpZ2h0S2luZCkge1xuICAgIC8qKlxuICAgICAqIEEgdGV4dHVhbCBvY2N1cnJlbmNlLlxuICAgICAqL1xuICAgIERvY3VtZW50SGlnaGxpZ2h0S2luZC5UZXh0ID0gMTtcbiAgICAvKipcbiAgICAgKiBSZWFkLWFjY2VzcyBvZiBhIHN5bWJvbCwgbGlrZSByZWFkaW5nIGEgdmFyaWFibGUuXG4gICAgICovXG4gICAgRG9jdW1lbnRIaWdobGlnaHRLaW5kLlJlYWQgPSAyO1xuICAgIC8qKlxuICAgICAqIFdyaXRlLWFjY2VzcyBvZiBhIHN5bWJvbCwgbGlrZSB3cml0aW5nIHRvIGEgdmFyaWFibGUuXG4gICAgICovXG4gICAgRG9jdW1lbnRIaWdobGlnaHRLaW5kLldyaXRlID0gMztcbn0pKERvY3VtZW50SGlnaGxpZ2h0S2luZCB8fCAoRG9jdW1lbnRIaWdobGlnaHRLaW5kID0ge30pKTtcbi8qKlxuICogRG9jdW1lbnRIaWdobGlnaHQgbmFtZXNwYWNlIHRvIHByb3ZpZGUgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBEb2N1bWVudEhpZ2hsaWdodH0gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgRG9jdW1lbnRIaWdobGlnaHQ7XG4oZnVuY3Rpb24gKERvY3VtZW50SGlnaGxpZ2h0KSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlIGEgRG9jdW1lbnRIaWdobGlnaHQgb2JqZWN0LlxuICAgICAqIEBwYXJhbSByYW5nZSBUaGUgcmFuZ2UgdGhlIGhpZ2hsaWdodCBhcHBsaWVzIHRvLlxuICAgICAqIEBwYXJhbSBraW5kIFRoZSBoaWdobGlnaHQga2luZFxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShyYW5nZSwga2luZCkge1xuICAgICAgICBsZXQgcmVzdWx0ID0geyByYW5nZSB9O1xuICAgICAgICBpZiAoSXMubnVtYmVyKGtpbmQpKSB7XG4gICAgICAgICAgICByZXN1bHQua2luZCA9IGtpbmQ7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICB9XG4gICAgRG9jdW1lbnRIaWdobGlnaHQuY3JlYXRlID0gY3JlYXRlO1xufSkoRG9jdW1lbnRIaWdobGlnaHQgfHwgKERvY3VtZW50SGlnaGxpZ2h0ID0ge30pKTtcbi8qKlxuICogQSBzeW1ib2wga2luZC5cbiAqL1xuZXhwb3J0IHZhciBTeW1ib2xLaW5kO1xuKGZ1bmN0aW9uIChTeW1ib2xLaW5kKSB7XG4gICAgU3ltYm9sS2luZC5GaWxlID0gMTtcbiAgICBTeW1ib2xLaW5kLk1vZHVsZSA9IDI7XG4gICAgU3ltYm9sS2luZC5OYW1lc3BhY2UgPSAzO1xuICAgIFN5bWJvbEtpbmQuUGFja2FnZSA9IDQ7XG4gICAgU3ltYm9sS2luZC5DbGFzcyA9IDU7XG4gICAgU3ltYm9sS2luZC5NZXRob2QgPSA2O1xuICAgIFN5bWJvbEtpbmQuUHJvcGVydHkgPSA3O1xuICAgIFN5bWJvbEtpbmQuRmllbGQgPSA4O1xuICAgIFN5bWJvbEtpbmQuQ29uc3RydWN0b3IgPSA5O1xuICAgIFN5bWJvbEtpbmQuRW51bSA9IDEwO1xuICAgIFN5bWJvbEtpbmQuSW50ZXJmYWNlID0gMTE7XG4gICAgU3ltYm9sS2luZC5GdW5jdGlvbiA9IDEyO1xuICAgIFN5bWJvbEtpbmQuVmFyaWFibGUgPSAxMztcbiAgICBTeW1ib2xLaW5kLkNvbnN0YW50ID0gMTQ7XG4gICAgU3ltYm9sS2luZC5TdHJpbmcgPSAxNTtcbiAgICBTeW1ib2xLaW5kLk51bWJlciA9IDE2O1xuICAgIFN5bWJvbEtpbmQuQm9vbGVhbiA9IDE3O1xuICAgIFN5bWJvbEtpbmQuQXJyYXkgPSAxODtcbiAgICBTeW1ib2xLaW5kLk9iamVjdCA9IDE5O1xuICAgIFN5bWJvbEtpbmQuS2V5ID0gMjA7XG4gICAgU3ltYm9sS2luZC5OdWxsID0gMjE7XG4gICAgU3ltYm9sS2luZC5FbnVtTWVtYmVyID0gMjI7XG4gICAgU3ltYm9sS2luZC5TdHJ1Y3QgPSAyMztcbiAgICBTeW1ib2xLaW5kLkV2ZW50ID0gMjQ7XG4gICAgU3ltYm9sS2luZC5PcGVyYXRvciA9IDI1O1xuICAgIFN5bWJvbEtpbmQuVHlwZVBhcmFtZXRlciA9IDI2O1xufSkoU3ltYm9sS2luZCB8fCAoU3ltYm9sS2luZCA9IHt9KSk7XG4vKipcbiAqIFN5bWJvbCB0YWdzIGFyZSBleHRyYSBhbm5vdGF0aW9ucyB0aGF0IHR3ZWFrIHRoZSByZW5kZXJpbmcgb2YgYSBzeW1ib2wuXG4gKlxuICogQHNpbmNlIDMuMTZcbiAqL1xuZXhwb3J0IHZhciBTeW1ib2xUYWc7XG4oZnVuY3Rpb24gKFN5bWJvbFRhZykge1xuICAgIC8qKlxuICAgICAqIFJlbmRlciBhIHN5bWJvbCBhcyBvYnNvbGV0ZSwgdXN1YWxseSB1c2luZyBhIHN0cmlrZS1vdXQuXG4gICAgICovXG4gICAgU3ltYm9sVGFnLkRlcHJlY2F0ZWQgPSAxO1xufSkoU3ltYm9sVGFnIHx8IChTeW1ib2xUYWcgPSB7fSkpO1xuZXhwb3J0IHZhciBTeW1ib2xJbmZvcm1hdGlvbjtcbihmdW5jdGlvbiAoU3ltYm9sSW5mb3JtYXRpb24pIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IHN5bWJvbCBpbmZvcm1hdGlvbiBsaXRlcmFsLlxuICAgICAqXG4gICAgICogQHBhcmFtIG5hbWUgVGhlIG5hbWUgb2YgdGhlIHN5bWJvbC5cbiAgICAgKiBAcGFyYW0ga2luZCBUaGUga2luZCBvZiB0aGUgc3ltYm9sLlxuICAgICAqIEBwYXJhbSByYW5nZSBUaGUgcmFuZ2Ugb2YgdGhlIGxvY2F0aW9uIG9mIHRoZSBzeW1ib2wuXG4gICAgICogQHBhcmFtIHVyaSBUaGUgcmVzb3VyY2Ugb2YgdGhlIGxvY2F0aW9uIG9mIHN5bWJvbC5cbiAgICAgKiBAcGFyYW0gY29udGFpbmVyTmFtZSBUaGUgbmFtZSBvZiB0aGUgc3ltYm9sIGNvbnRhaW5pbmcgdGhlIHN5bWJvbC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUobmFtZSwga2luZCwgcmFuZ2UsIHVyaSwgY29udGFpbmVyTmFtZSkge1xuICAgICAgICBsZXQgcmVzdWx0ID0ge1xuICAgICAgICAgICAgbmFtZSxcbiAgICAgICAgICAgIGtpbmQsXG4gICAgICAgICAgICBsb2NhdGlvbjogeyB1cmksIHJhbmdlIH1cbiAgICAgICAgfTtcbiAgICAgICAgaWYgKGNvbnRhaW5lck5hbWUpIHtcbiAgICAgICAgICAgIHJlc3VsdC5jb250YWluZXJOYW1lID0gY29udGFpbmVyTmFtZTtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cbiAgICBTeW1ib2xJbmZvcm1hdGlvbi5jcmVhdGUgPSBjcmVhdGU7XG59KShTeW1ib2xJbmZvcm1hdGlvbiB8fCAoU3ltYm9sSW5mb3JtYXRpb24gPSB7fSkpO1xuZXhwb3J0IHZhciBXb3Jrc3BhY2VTeW1ib2w7XG4oZnVuY3Rpb24gKFdvcmtzcGFjZVN5bWJvbCkge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZSBhIG5ldyB3b3Jrc3BhY2Ugc3ltYm9sLlxuICAgICAqXG4gICAgICogQHBhcmFtIG5hbWUgVGhlIG5hbWUgb2YgdGhlIHN5bWJvbC5cbiAgICAgKiBAcGFyYW0ga2luZCBUaGUga2luZCBvZiB0aGUgc3ltYm9sLlxuICAgICAqIEBwYXJhbSB1cmkgVGhlIHJlc291cmNlIG9mIHRoZSBsb2NhdGlvbiBvZiB0aGUgc3ltYm9sLlxuICAgICAqIEBwYXJhbSByYW5nZSBBbiBvcHRpb25zIHJhbmdlIG9mIHRoZSBsb2NhdGlvbi5cbiAgICAgKiBAcmV0dXJucyBBIFdvcmtzcGFjZVN5bWJvbC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUobmFtZSwga2luZCwgdXJpLCByYW5nZSkge1xuICAgICAgICByZXR1cm4gcmFuZ2UgIT09IHVuZGVmaW5lZFxuICAgICAgICAgICAgPyB7IG5hbWUsIGtpbmQsIGxvY2F0aW9uOiB7IHVyaSwgcmFuZ2UgfSB9XG4gICAgICAgICAgICA6IHsgbmFtZSwga2luZCwgbG9jYXRpb246IHsgdXJpIH0gfTtcbiAgICB9XG4gICAgV29ya3NwYWNlU3ltYm9sLmNyZWF0ZSA9IGNyZWF0ZTtcbn0pKFdvcmtzcGFjZVN5bWJvbCB8fCAoV29ya3NwYWNlU3ltYm9sID0ge30pKTtcbmV4cG9ydCB2YXIgRG9jdW1lbnRTeW1ib2w7XG4oZnVuY3Rpb24gKERvY3VtZW50U3ltYm9sKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBzeW1ib2wgaW5mb3JtYXRpb24gbGl0ZXJhbC5cbiAgICAgKlxuICAgICAqIEBwYXJhbSBuYW1lIFRoZSBuYW1lIG9mIHRoZSBzeW1ib2wuXG4gICAgICogQHBhcmFtIGRldGFpbCBUaGUgZGV0YWlsIG9mIHRoZSBzeW1ib2wuXG4gICAgICogQHBhcmFtIGtpbmQgVGhlIGtpbmQgb2YgdGhlIHN5bWJvbC5cbiAgICAgKiBAcGFyYW0gcmFuZ2UgVGhlIHJhbmdlIG9mIHRoZSBzeW1ib2wuXG4gICAgICogQHBhcmFtIHNlbGVjdGlvblJhbmdlIFRoZSBzZWxlY3Rpb25SYW5nZSBvZiB0aGUgc3ltYm9sLlxuICAgICAqIEBwYXJhbSBjaGlsZHJlbiBDaGlsZHJlbiBvZiB0aGUgc3ltYm9sLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShuYW1lLCBkZXRhaWwsIGtpbmQsIHJhbmdlLCBzZWxlY3Rpb25SYW5nZSwgY2hpbGRyZW4pIHtcbiAgICAgICAgbGV0IHJlc3VsdCA9IHtcbiAgICAgICAgICAgIG5hbWUsXG4gICAgICAgICAgICBkZXRhaWwsXG4gICAgICAgICAgICBraW5kLFxuICAgICAgICAgICAgcmFuZ2UsXG4gICAgICAgICAgICBzZWxlY3Rpb25SYW5nZVxuICAgICAgICB9O1xuICAgICAgICBpZiAoY2hpbGRyZW4gIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmVzdWx0LmNoaWxkcmVuID0gY2hpbGRyZW47XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICB9XG4gICAgRG9jdW1lbnRTeW1ib2wuY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgRG9jdW1lbnRTeW1ib2x9IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBjYW5kaWRhdGUgJiZcbiAgICAgICAgICAgIElzLnN0cmluZyhjYW5kaWRhdGUubmFtZSkgJiYgSXMubnVtYmVyKGNhbmRpZGF0ZS5raW5kKSAmJlxuICAgICAgICAgICAgUmFuZ2UuaXMoY2FuZGlkYXRlLnJhbmdlKSAmJiBSYW5nZS5pcyhjYW5kaWRhdGUuc2VsZWN0aW9uUmFuZ2UpICYmXG4gICAgICAgICAgICAoY2FuZGlkYXRlLmRldGFpbCA9PT0gdW5kZWZpbmVkIHx8IElzLnN0cmluZyhjYW5kaWRhdGUuZGV0YWlsKSkgJiZcbiAgICAgICAgICAgIChjYW5kaWRhdGUuZGVwcmVjYXRlZCA9PT0gdW5kZWZpbmVkIHx8IElzLmJvb2xlYW4oY2FuZGlkYXRlLmRlcHJlY2F0ZWQpKSAmJlxuICAgICAgICAgICAgKGNhbmRpZGF0ZS5jaGlsZHJlbiA9PT0gdW5kZWZpbmVkIHx8IEFycmF5LmlzQXJyYXkoY2FuZGlkYXRlLmNoaWxkcmVuKSkgJiZcbiAgICAgICAgICAgIChjYW5kaWRhdGUudGFncyA9PT0gdW5kZWZpbmVkIHx8IEFycmF5LmlzQXJyYXkoY2FuZGlkYXRlLnRhZ3MpKTtcbiAgICB9XG4gICAgRG9jdW1lbnRTeW1ib2wuaXMgPSBpcztcbn0pKERvY3VtZW50U3ltYm9sIHx8IChEb2N1bWVudFN5bWJvbCA9IHt9KSk7XG4vKipcbiAqIEEgc2V0IG9mIHByZWRlZmluZWQgY29kZSBhY3Rpb24ga2luZHNcbiAqL1xuZXhwb3J0IHZhciBDb2RlQWN0aW9uS2luZDtcbihmdW5jdGlvbiAoQ29kZUFjdGlvbktpbmQpIHtcbiAgICAvKipcbiAgICAgKiBFbXB0eSBraW5kLlxuICAgICAqL1xuICAgIENvZGVBY3Rpb25LaW5kLkVtcHR5ID0gJyc7XG4gICAgLyoqXG4gICAgICogQmFzZSBraW5kIGZvciBxdWlja2ZpeCBhY3Rpb25zOiAncXVpY2tmaXgnXG4gICAgICovXG4gICAgQ29kZUFjdGlvbktpbmQuUXVpY2tGaXggPSAncXVpY2tmaXgnO1xuICAgIC8qKlxuICAgICAqIEJhc2Uga2luZCBmb3IgcmVmYWN0b3JpbmcgYWN0aW9uczogJ3JlZmFjdG9yJ1xuICAgICAqL1xuICAgIENvZGVBY3Rpb25LaW5kLlJlZmFjdG9yID0gJ3JlZmFjdG9yJztcbiAgICAvKipcbiAgICAgKiBCYXNlIGtpbmQgZm9yIHJlZmFjdG9yaW5nIGV4dHJhY3Rpb24gYWN0aW9uczogJ3JlZmFjdG9yLmV4dHJhY3QnXG4gICAgICpcbiAgICAgKiBFeGFtcGxlIGV4dHJhY3QgYWN0aW9uczpcbiAgICAgKlxuICAgICAqIC0gRXh0cmFjdCBtZXRob2RcbiAgICAgKiAtIEV4dHJhY3QgZnVuY3Rpb25cbiAgICAgKiAtIEV4dHJhY3QgdmFyaWFibGVcbiAgICAgKiAtIEV4dHJhY3QgaW50ZXJmYWNlIGZyb20gY2xhc3NcbiAgICAgKiAtIC4uLlxuICAgICAqL1xuICAgIENvZGVBY3Rpb25LaW5kLlJlZmFjdG9yRXh0cmFjdCA9ICdyZWZhY3Rvci5leHRyYWN0JztcbiAgICAvKipcbiAgICAgKiBCYXNlIGtpbmQgZm9yIHJlZmFjdG9yaW5nIGlubGluZSBhY3Rpb25zOiAncmVmYWN0b3IuaW5saW5lJ1xuICAgICAqXG4gICAgICogRXhhbXBsZSBpbmxpbmUgYWN0aW9uczpcbiAgICAgKlxuICAgICAqIC0gSW5saW5lIGZ1bmN0aW9uXG4gICAgICogLSBJbmxpbmUgdmFyaWFibGVcbiAgICAgKiAtIElubGluZSBjb25zdGFudFxuICAgICAqIC0gLi4uXG4gICAgICovXG4gICAgQ29kZUFjdGlvbktpbmQuUmVmYWN0b3JJbmxpbmUgPSAncmVmYWN0b3IuaW5saW5lJztcbiAgICAvKipcbiAgICAgKiBCYXNlIGtpbmQgZm9yIHJlZmFjdG9yaW5nIHJld3JpdGUgYWN0aW9uczogJ3JlZmFjdG9yLnJld3JpdGUnXG4gICAgICpcbiAgICAgKiBFeGFtcGxlIHJld3JpdGUgYWN0aW9uczpcbiAgICAgKlxuICAgICAqIC0gQ29udmVydCBKYXZhU2NyaXB0IGZ1bmN0aW9uIHRvIGNsYXNzXG4gICAgICogLSBBZGQgb3IgcmVtb3ZlIHBhcmFtZXRlclxuICAgICAqIC0gRW5jYXBzdWxhdGUgZmllbGRcbiAgICAgKiAtIE1ha2UgbWV0aG9kIHN0YXRpY1xuICAgICAqIC0gTW92ZSBtZXRob2QgdG8gYmFzZSBjbGFzc1xuICAgICAqIC0gLi4uXG4gICAgICovXG4gICAgQ29kZUFjdGlvbktpbmQuUmVmYWN0b3JSZXdyaXRlID0gJ3JlZmFjdG9yLnJld3JpdGUnO1xuICAgIC8qKlxuICAgICAqIEJhc2Uga2luZCBmb3Igc291cmNlIGFjdGlvbnM6IGBzb3VyY2VgXG4gICAgICpcbiAgICAgKiBTb3VyY2UgY29kZSBhY3Rpb25zIGFwcGx5IHRvIHRoZSBlbnRpcmUgZmlsZS5cbiAgICAgKi9cbiAgICBDb2RlQWN0aW9uS2luZC5Tb3VyY2UgPSAnc291cmNlJztcbiAgICAvKipcbiAgICAgKiBCYXNlIGtpbmQgZm9yIGFuIG9yZ2FuaXplIGltcG9ydHMgc291cmNlIGFjdGlvbjogYHNvdXJjZS5vcmdhbml6ZUltcG9ydHNgXG4gICAgICovXG4gICAgQ29kZUFjdGlvbktpbmQuU291cmNlT3JnYW5pemVJbXBvcnRzID0gJ3NvdXJjZS5vcmdhbml6ZUltcG9ydHMnO1xuICAgIC8qKlxuICAgICAqIEJhc2Uga2luZCBmb3IgYXV0by1maXggc291cmNlIGFjdGlvbnM6IGBzb3VyY2UuZml4QWxsYC5cbiAgICAgKlxuICAgICAqIEZpeCBhbGwgYWN0aW9ucyBhdXRvbWF0aWNhbGx5IGZpeCBlcnJvcnMgdGhhdCBoYXZlIGEgY2xlYXIgZml4IHRoYXQgZG8gbm90IHJlcXVpcmUgdXNlciBpbnB1dC5cbiAgICAgKiBUaGV5IHNob3VsZCBub3Qgc3VwcHJlc3MgZXJyb3JzIG9yIHBlcmZvcm0gdW5zYWZlIGZpeGVzIHN1Y2ggYXMgZ2VuZXJhdGluZyBuZXcgdHlwZXMgb3IgY2xhc3Nlcy5cbiAgICAgKlxuICAgICAqIEBzaW5jZSAzLjE1LjBcbiAgICAgKi9cbiAgICBDb2RlQWN0aW9uS2luZC5Tb3VyY2VGaXhBbGwgPSAnc291cmNlLmZpeEFsbCc7XG59KShDb2RlQWN0aW9uS2luZCB8fCAoQ29kZUFjdGlvbktpbmQgPSB7fSkpO1xuLyoqXG4gKiBUaGUgcmVhc29uIHdoeSBjb2RlIGFjdGlvbnMgd2VyZSByZXF1ZXN0ZWQuXG4gKlxuICogQHNpbmNlIDMuMTcuMFxuICovXG5leHBvcnQgdmFyIENvZGVBY3Rpb25UcmlnZ2VyS2luZDtcbihmdW5jdGlvbiAoQ29kZUFjdGlvblRyaWdnZXJLaW5kKSB7XG4gICAgLyoqXG4gICAgICogQ29kZSBhY3Rpb25zIHdlcmUgZXhwbGljaXRseSByZXF1ZXN0ZWQgYnkgdGhlIHVzZXIgb3IgYnkgYW4gZXh0ZW5zaW9uLlxuICAgICAqL1xuICAgIENvZGVBY3Rpb25UcmlnZ2VyS2luZC5JbnZva2VkID0gMTtcbiAgICAvKipcbiAgICAgKiBDb2RlIGFjdGlvbnMgd2VyZSByZXF1ZXN0ZWQgYXV0b21hdGljYWxseS5cbiAgICAgKlxuICAgICAqIFRoaXMgdHlwaWNhbGx5IGhhcHBlbnMgd2hlbiBjdXJyZW50IHNlbGVjdGlvbiBpbiBhIGZpbGUgY2hhbmdlcywgYnV0IGNhblxuICAgICAqIGFsc28gYmUgdHJpZ2dlcmVkIHdoZW4gZmlsZSBjb250ZW50IGNoYW5nZXMuXG4gICAgICovXG4gICAgQ29kZUFjdGlvblRyaWdnZXJLaW5kLkF1dG9tYXRpYyA9IDI7XG59KShDb2RlQWN0aW9uVHJpZ2dlcktpbmQgfHwgKENvZGVBY3Rpb25UcmlnZ2VyS2luZCA9IHt9KSk7XG4vKipcbiAqIFRoZSBDb2RlQWN0aW9uQ29udGV4dCBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBDb2RlQWN0aW9uQ29udGV4dH0gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgQ29kZUFjdGlvbkNvbnRleHQ7XG4oZnVuY3Rpb24gKENvZGVBY3Rpb25Db250ZXh0KSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBDb2RlQWN0aW9uQ29udGV4dCBsaXRlcmFsLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShkaWFnbm9zdGljcywgb25seSwgdHJpZ2dlcktpbmQpIHtcbiAgICAgICAgbGV0IHJlc3VsdCA9IHsgZGlhZ25vc3RpY3MgfTtcbiAgICAgICAgaWYgKG9ubHkgIT09IHVuZGVmaW5lZCAmJiBvbmx5ICE9PSBudWxsKSB7XG4gICAgICAgICAgICByZXN1bHQub25seSA9IG9ubHk7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKHRyaWdnZXJLaW5kICE9PSB1bmRlZmluZWQgJiYgdHJpZ2dlcktpbmQgIT09IG51bGwpIHtcbiAgICAgICAgICAgIHJlc3VsdC50cmlnZ2VyS2luZCA9IHRyaWdnZXJLaW5kO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxuICAgIENvZGVBY3Rpb25Db250ZXh0LmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIENvZGVBY3Rpb25Db250ZXh0fSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMuZGVmaW5lZChjYW5kaWRhdGUpICYmIElzLnR5cGVkQXJyYXkoY2FuZGlkYXRlLmRpYWdub3N0aWNzLCBEaWFnbm9zdGljLmlzKVxuICAgICAgICAgICAgJiYgKGNhbmRpZGF0ZS5vbmx5ID09PSB1bmRlZmluZWQgfHwgSXMudHlwZWRBcnJheShjYW5kaWRhdGUub25seSwgSXMuc3RyaW5nKSlcbiAgICAgICAgICAgICYmIChjYW5kaWRhdGUudHJpZ2dlcktpbmQgPT09IHVuZGVmaW5lZCB8fCBjYW5kaWRhdGUudHJpZ2dlcktpbmQgPT09IENvZGVBY3Rpb25UcmlnZ2VyS2luZC5JbnZva2VkIHx8IGNhbmRpZGF0ZS50cmlnZ2VyS2luZCA9PT0gQ29kZUFjdGlvblRyaWdnZXJLaW5kLkF1dG9tYXRpYyk7XG4gICAgfVxuICAgIENvZGVBY3Rpb25Db250ZXh0LmlzID0gaXM7XG59KShDb2RlQWN0aW9uQ29udGV4dCB8fCAoQ29kZUFjdGlvbkNvbnRleHQgPSB7fSkpO1xuZXhwb3J0IHZhciBDb2RlQWN0aW9uO1xuKGZ1bmN0aW9uIChDb2RlQWN0aW9uKSB7XG4gICAgZnVuY3Rpb24gY3JlYXRlKHRpdGxlLCBraW5kT3JDb21tYW5kT3JFZGl0LCBraW5kKSB7XG4gICAgICAgIGxldCByZXN1bHQgPSB7IHRpdGxlIH07XG4gICAgICAgIGxldCBjaGVja0tpbmQgPSB0cnVlO1xuICAgICAgICBpZiAodHlwZW9mIGtpbmRPckNvbW1hbmRPckVkaXQgPT09ICdzdHJpbmcnKSB7XG4gICAgICAgICAgICBjaGVja0tpbmQgPSBmYWxzZTtcbiAgICAgICAgICAgIHJlc3VsdC5raW5kID0ga2luZE9yQ29tbWFuZE9yRWRpdDtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIGlmIChDb21tYW5kLmlzKGtpbmRPckNvbW1hbmRPckVkaXQpKSB7XG4gICAgICAgICAgICByZXN1bHQuY29tbWFuZCA9IGtpbmRPckNvbW1hbmRPckVkaXQ7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICByZXN1bHQuZWRpdCA9IGtpbmRPckNvbW1hbmRPckVkaXQ7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKGNoZWNrS2luZCAmJiBraW5kICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHJlc3VsdC5raW5kID0ga2luZDtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cbiAgICBDb2RlQWN0aW9uLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBjYW5kaWRhdGUgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS50aXRsZSkgJiZcbiAgICAgICAgICAgIChjYW5kaWRhdGUuZGlhZ25vc3RpY3MgPT09IHVuZGVmaW5lZCB8fCBJcy50eXBlZEFycmF5KGNhbmRpZGF0ZS5kaWFnbm9zdGljcywgRGlhZ25vc3RpYy5pcykpICYmXG4gICAgICAgICAgICAoY2FuZGlkYXRlLmtpbmQgPT09IHVuZGVmaW5lZCB8fCBJcy5zdHJpbmcoY2FuZGlkYXRlLmtpbmQpKSAmJlxuICAgICAgICAgICAgKGNhbmRpZGF0ZS5lZGl0ICE9PSB1bmRlZmluZWQgfHwgY2FuZGlkYXRlLmNvbW1hbmQgIT09IHVuZGVmaW5lZCkgJiZcbiAgICAgICAgICAgIChjYW5kaWRhdGUuY29tbWFuZCA9PT0gdW5kZWZpbmVkIHx8IENvbW1hbmQuaXMoY2FuZGlkYXRlLmNvbW1hbmQpKSAmJlxuICAgICAgICAgICAgKGNhbmRpZGF0ZS5pc1ByZWZlcnJlZCA9PT0gdW5kZWZpbmVkIHx8IElzLmJvb2xlYW4oY2FuZGlkYXRlLmlzUHJlZmVycmVkKSkgJiZcbiAgICAgICAgICAgIChjYW5kaWRhdGUuZWRpdCA9PT0gdW5kZWZpbmVkIHx8IFdvcmtzcGFjZUVkaXQuaXMoY2FuZGlkYXRlLmVkaXQpKTtcbiAgICB9XG4gICAgQ29kZUFjdGlvbi5pcyA9IGlzO1xufSkoQ29kZUFjdGlvbiB8fCAoQ29kZUFjdGlvbiA9IHt9KSk7XG4vKipcbiAqIFRoZSBDb2RlTGVucyBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBDb2RlTGVuc30gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgQ29kZUxlbnM7XG4oZnVuY3Rpb24gKENvZGVMZW5zKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBDb2RlTGVucyBsaXRlcmFsLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShyYW5nZSwgZGF0YSkge1xuICAgICAgICBsZXQgcmVzdWx0ID0geyByYW5nZSB9O1xuICAgICAgICBpZiAoSXMuZGVmaW5lZChkYXRhKSkge1xuICAgICAgICAgICAgcmVzdWx0LmRhdGEgPSBkYXRhO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxuICAgIENvZGVMZW5zLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIENvZGVMZW5zfSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMuZGVmaW5lZChjYW5kaWRhdGUpICYmIFJhbmdlLmlzKGNhbmRpZGF0ZS5yYW5nZSkgJiYgKElzLnVuZGVmaW5lZChjYW5kaWRhdGUuY29tbWFuZCkgfHwgQ29tbWFuZC5pcyhjYW5kaWRhdGUuY29tbWFuZCkpO1xuICAgIH1cbiAgICBDb2RlTGVucy5pcyA9IGlzO1xufSkoQ29kZUxlbnMgfHwgKENvZGVMZW5zID0ge30pKTtcbi8qKlxuICogVGhlIEZvcm1hdHRpbmdPcHRpb25zIG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIEZvcm1hdHRpbmdPcHRpb25zfSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBGb3JtYXR0aW5nT3B0aW9ucztcbihmdW5jdGlvbiAoRm9ybWF0dGluZ09wdGlvbnMpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IEZvcm1hdHRpbmdPcHRpb25zIGxpdGVyYWwuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKHRhYlNpemUsIGluc2VydFNwYWNlcykge1xuICAgICAgICByZXR1cm4geyB0YWJTaXplLCBpbnNlcnRTcGFjZXMgfTtcbiAgICB9XG4gICAgRm9ybWF0dGluZ09wdGlvbnMuY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgRm9ybWF0dGluZ09wdGlvbnN9IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5kZWZpbmVkKGNhbmRpZGF0ZSkgJiYgSXMudWludGVnZXIoY2FuZGlkYXRlLnRhYlNpemUpICYmIElzLmJvb2xlYW4oY2FuZGlkYXRlLmluc2VydFNwYWNlcyk7XG4gICAgfVxuICAgIEZvcm1hdHRpbmdPcHRpb25zLmlzID0gaXM7XG59KShGb3JtYXR0aW5nT3B0aW9ucyB8fCAoRm9ybWF0dGluZ09wdGlvbnMgPSB7fSkpO1xuLyoqXG4gKiBUaGUgRG9jdW1lbnRMaW5rIG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIERvY3VtZW50TGlua30gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgRG9jdW1lbnRMaW5rO1xuKGZ1bmN0aW9uIChEb2N1bWVudExpbmspIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IERvY3VtZW50TGluayBsaXRlcmFsLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShyYW5nZSwgdGFyZ2V0LCBkYXRhKSB7XG4gICAgICAgIHJldHVybiB7IHJhbmdlLCB0YXJnZXQsIGRhdGEgfTtcbiAgICB9XG4gICAgRG9jdW1lbnRMaW5rLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIERvY3VtZW50TGlua30gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLmRlZmluZWQoY2FuZGlkYXRlKSAmJiBSYW5nZS5pcyhjYW5kaWRhdGUucmFuZ2UpICYmIChJcy51bmRlZmluZWQoY2FuZGlkYXRlLnRhcmdldCkgfHwgSXMuc3RyaW5nKGNhbmRpZGF0ZS50YXJnZXQpKTtcbiAgICB9XG4gICAgRG9jdW1lbnRMaW5rLmlzID0gaXM7XG59KShEb2N1bWVudExpbmsgfHwgKERvY3VtZW50TGluayA9IHt9KSk7XG4vKipcbiAqIFRoZSBTZWxlY3Rpb25SYW5nZSBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9uIHRvIHdvcmsgd2l0aFxuICogU2VsZWN0aW9uUmFuZ2UgbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgU2VsZWN0aW9uUmFuZ2U7XG4oZnVuY3Rpb24gKFNlbGVjdGlvblJhbmdlKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBTZWxlY3Rpb25SYW5nZVxuICAgICAqIEBwYXJhbSByYW5nZSB0aGUgcmFuZ2UuXG4gICAgICogQHBhcmFtIHBhcmVudCBhbiBvcHRpb25hbCBwYXJlbnQuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKHJhbmdlLCBwYXJlbnQpIHtcbiAgICAgICAgcmV0dXJuIHsgcmFuZ2UsIHBhcmVudCB9O1xuICAgIH1cbiAgICBTZWxlY3Rpb25SYW5nZS5jcmVhdGUgPSBjcmVhdGU7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMub2JqZWN0TGl0ZXJhbChjYW5kaWRhdGUpICYmIFJhbmdlLmlzKGNhbmRpZGF0ZS5yYW5nZSkgJiYgKGNhbmRpZGF0ZS5wYXJlbnQgPT09IHVuZGVmaW5lZCB8fCBTZWxlY3Rpb25SYW5nZS5pcyhjYW5kaWRhdGUucGFyZW50KSk7XG4gICAgfVxuICAgIFNlbGVjdGlvblJhbmdlLmlzID0gaXM7XG59KShTZWxlY3Rpb25SYW5nZSB8fCAoU2VsZWN0aW9uUmFuZ2UgPSB7fSkpO1xuLyoqXG4gKiBBIHNldCBvZiBwcmVkZWZpbmVkIHRva2VuIHR5cGVzLiBUaGlzIHNldCBpcyBub3QgZml4ZWRcbiAqIGFuIGNsaWVudHMgY2FuIHNwZWNpZnkgYWRkaXRpb25hbCB0b2tlbiB0eXBlcyB2aWEgdGhlXG4gKiBjb3JyZXNwb25kaW5nIGNsaWVudCBjYXBhYmlsaXRpZXMuXG4gKlxuICogQHNpbmNlIDMuMTYuMFxuICovXG5leHBvcnQgdmFyIFNlbWFudGljVG9rZW5UeXBlcztcbihmdW5jdGlvbiAoU2VtYW50aWNUb2tlblR5cGVzKSB7XG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1wibmFtZXNwYWNlXCJdID0gXCJuYW1lc3BhY2VcIjtcbiAgICAvKipcbiAgICAgKiBSZXByZXNlbnRzIGEgZ2VuZXJpYyB0eXBlLiBBY3RzIGFzIGEgZmFsbGJhY2sgZm9yIHR5cGVzIHdoaWNoIGNhbid0IGJlIG1hcHBlZCB0b1xuICAgICAqIGEgc3BlY2lmaWMgdHlwZSBsaWtlIGNsYXNzIG9yIGVudW0uXG4gICAgICovXG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1widHlwZVwiXSA9IFwidHlwZVwiO1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcImNsYXNzXCJdID0gXCJjbGFzc1wiO1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcImVudW1cIl0gPSBcImVudW1cIjtcbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJpbnRlcmZhY2VcIl0gPSBcImludGVyZmFjZVwiO1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcInN0cnVjdFwiXSA9IFwic3RydWN0XCI7XG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1widHlwZVBhcmFtZXRlclwiXSA9IFwidHlwZVBhcmFtZXRlclwiO1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcInBhcmFtZXRlclwiXSA9IFwicGFyYW1ldGVyXCI7XG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1widmFyaWFibGVcIl0gPSBcInZhcmlhYmxlXCI7XG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1wicHJvcGVydHlcIl0gPSBcInByb3BlcnR5XCI7XG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1wiZW51bU1lbWJlclwiXSA9IFwiZW51bU1lbWJlclwiO1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcImV2ZW50XCJdID0gXCJldmVudFwiO1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcImZ1bmN0aW9uXCJdID0gXCJmdW5jdGlvblwiO1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcIm1ldGhvZFwiXSA9IFwibWV0aG9kXCI7XG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1wibWFjcm9cIl0gPSBcIm1hY3JvXCI7XG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1wia2V5d29yZFwiXSA9IFwia2V5d29yZFwiO1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcIm1vZGlmaWVyXCJdID0gXCJtb2RpZmllclwiO1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcImNvbW1lbnRcIl0gPSBcImNvbW1lbnRcIjtcbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJzdHJpbmdcIl0gPSBcInN0cmluZ1wiO1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcIm51bWJlclwiXSA9IFwibnVtYmVyXCI7XG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1wicmVnZXhwXCJdID0gXCJyZWdleHBcIjtcbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJvcGVyYXRvclwiXSA9IFwib3BlcmF0b3JcIjtcbiAgICAvKipcbiAgICAgKiBAc2luY2UgMy4xNy4wXG4gICAgICovXG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1wiZGVjb3JhdG9yXCJdID0gXCJkZWNvcmF0b3JcIjtcbn0pKFNlbWFudGljVG9rZW5UeXBlcyB8fCAoU2VtYW50aWNUb2tlblR5cGVzID0ge30pKTtcbi8qKlxuICogQSBzZXQgb2YgcHJlZGVmaW5lZCB0b2tlbiBtb2RpZmllcnMuIFRoaXMgc2V0IGlzIG5vdCBmaXhlZFxuICogYW4gY2xpZW50cyBjYW4gc3BlY2lmeSBhZGRpdGlvbmFsIHRva2VuIHR5cGVzIHZpYSB0aGVcbiAqIGNvcnJlc3BvbmRpbmcgY2xpZW50IGNhcGFiaWxpdGllcy5cbiAqXG4gKiBAc2luY2UgMy4xNi4wXG4gKi9cbmV4cG9ydCB2YXIgU2VtYW50aWNUb2tlbk1vZGlmaWVycztcbihmdW5jdGlvbiAoU2VtYW50aWNUb2tlbk1vZGlmaWVycykge1xuICAgIFNlbWFudGljVG9rZW5Nb2RpZmllcnNbXCJkZWNsYXJhdGlvblwiXSA9IFwiZGVjbGFyYXRpb25cIjtcbiAgICBTZW1hbnRpY1Rva2VuTW9kaWZpZXJzW1wiZGVmaW5pdGlvblwiXSA9IFwiZGVmaW5pdGlvblwiO1xuICAgIFNlbWFudGljVG9rZW5Nb2RpZmllcnNbXCJyZWFkb25seVwiXSA9IFwicmVhZG9ubHlcIjtcbiAgICBTZW1hbnRpY1Rva2VuTW9kaWZpZXJzW1wic3RhdGljXCJdID0gXCJzdGF0aWNcIjtcbiAgICBTZW1hbnRpY1Rva2VuTW9kaWZpZXJzW1wiZGVwcmVjYXRlZFwiXSA9IFwiZGVwcmVjYXRlZFwiO1xuICAgIFNlbWFudGljVG9rZW5Nb2RpZmllcnNbXCJhYnN0cmFjdFwiXSA9IFwiYWJzdHJhY3RcIjtcbiAgICBTZW1hbnRpY1Rva2VuTW9kaWZpZXJzW1wiYXN5bmNcIl0gPSBcImFzeW5jXCI7XG4gICAgU2VtYW50aWNUb2tlbk1vZGlmaWVyc1tcIm1vZGlmaWNhdGlvblwiXSA9IFwibW9kaWZpY2F0aW9uXCI7XG4gICAgU2VtYW50aWNUb2tlbk1vZGlmaWVyc1tcImRvY3VtZW50YXRpb25cIl0gPSBcImRvY3VtZW50YXRpb25cIjtcbiAgICBTZW1hbnRpY1Rva2VuTW9kaWZpZXJzW1wiZGVmYXVsdExpYnJhcnlcIl0gPSBcImRlZmF1bHRMaWJyYXJ5XCI7XG59KShTZW1hbnRpY1Rva2VuTW9kaWZpZXJzIHx8IChTZW1hbnRpY1Rva2VuTW9kaWZpZXJzID0ge30pKTtcbi8qKlxuICogQHNpbmNlIDMuMTYuMFxuICovXG5leHBvcnQgdmFyIFNlbWFudGljVG9rZW5zO1xuKGZ1bmN0aW9uIChTZW1hbnRpY1Rva2Vucykge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMub2JqZWN0TGl0ZXJhbChjYW5kaWRhdGUpICYmIChjYW5kaWRhdGUucmVzdWx0SWQgPT09IHVuZGVmaW5lZCB8fCB0eXBlb2YgY2FuZGlkYXRlLnJlc3VsdElkID09PSAnc3RyaW5nJykgJiZcbiAgICAgICAgICAgIEFycmF5LmlzQXJyYXkoY2FuZGlkYXRlLmRhdGEpICYmIChjYW5kaWRhdGUuZGF0YS5sZW5ndGggPT09IDAgfHwgdHlwZW9mIGNhbmRpZGF0ZS5kYXRhWzBdID09PSAnbnVtYmVyJyk7XG4gICAgfVxuICAgIFNlbWFudGljVG9rZW5zLmlzID0gaXM7XG59KShTZW1hbnRpY1Rva2VucyB8fCAoU2VtYW50aWNUb2tlbnMgPSB7fSkpO1xuLyoqXG4gKiBUaGUgSW5saW5lVmFsdWVUZXh0IG5hbWVzcGFjZSBwcm92aWRlcyBmdW5jdGlvbnMgdG8gZGVhbCB3aXRoIElubGluZVZhbHVlVGV4dHMuXG4gKlxuICogQHNpbmNlIDMuMTcuMFxuICovXG5leHBvcnQgdmFyIElubGluZVZhbHVlVGV4dDtcbihmdW5jdGlvbiAoSW5saW5lVmFsdWVUZXh0KSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBJbmxpbmVWYWx1ZVRleHQgbGl0ZXJhbC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUocmFuZ2UsIHRleHQpIHtcbiAgICAgICAgcmV0dXJuIHsgcmFuZ2UsIHRleHQgfTtcbiAgICB9XG4gICAgSW5saW5lVmFsdWVUZXh0LmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAhPT0gdW5kZWZpbmVkICYmIGNhbmRpZGF0ZSAhPT0gbnVsbCAmJiBSYW5nZS5pcyhjYW5kaWRhdGUucmFuZ2UpICYmIElzLnN0cmluZyhjYW5kaWRhdGUudGV4dCk7XG4gICAgfVxuICAgIElubGluZVZhbHVlVGV4dC5pcyA9IGlzO1xufSkoSW5saW5lVmFsdWVUZXh0IHx8IChJbmxpbmVWYWx1ZVRleHQgPSB7fSkpO1xuLyoqXG4gKiBUaGUgSW5saW5lVmFsdWVWYXJpYWJsZUxvb2t1cCBuYW1lc3BhY2UgcHJvdmlkZXMgZnVuY3Rpb25zIHRvIGRlYWwgd2l0aCBJbmxpbmVWYWx1ZVZhcmlhYmxlTG9va3Vwcy5cbiAqXG4gKiBAc2luY2UgMy4xNy4wXG4gKi9cbmV4cG9ydCB2YXIgSW5saW5lVmFsdWVWYXJpYWJsZUxvb2t1cDtcbihmdW5jdGlvbiAoSW5saW5lVmFsdWVWYXJpYWJsZUxvb2t1cCkge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgSW5saW5lVmFsdWVUZXh0IGxpdGVyYWwuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKHJhbmdlLCB2YXJpYWJsZU5hbWUsIGNhc2VTZW5zaXRpdmVMb29rdXApIHtcbiAgICAgICAgcmV0dXJuIHsgcmFuZ2UsIHZhcmlhYmxlTmFtZSwgY2FzZVNlbnNpdGl2ZUxvb2t1cCB9O1xuICAgIH1cbiAgICBJbmxpbmVWYWx1ZVZhcmlhYmxlTG9va3VwLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAhPT0gdW5kZWZpbmVkICYmIGNhbmRpZGF0ZSAhPT0gbnVsbCAmJiBSYW5nZS5pcyhjYW5kaWRhdGUucmFuZ2UpICYmIElzLmJvb2xlYW4oY2FuZGlkYXRlLmNhc2VTZW5zaXRpdmVMb29rdXApXG4gICAgICAgICAgICAmJiAoSXMuc3RyaW5nKGNhbmRpZGF0ZS52YXJpYWJsZU5hbWUpIHx8IGNhbmRpZGF0ZS52YXJpYWJsZU5hbWUgPT09IHVuZGVmaW5lZCk7XG4gICAgfVxuICAgIElubGluZVZhbHVlVmFyaWFibGVMb29rdXAuaXMgPSBpcztcbn0pKElubGluZVZhbHVlVmFyaWFibGVMb29rdXAgfHwgKElubGluZVZhbHVlVmFyaWFibGVMb29rdXAgPSB7fSkpO1xuLyoqXG4gKiBUaGUgSW5saW5lVmFsdWVFdmFsdWF0YWJsZUV4cHJlc3Npb24gbmFtZXNwYWNlIHByb3ZpZGVzIGZ1bmN0aW9ucyB0byBkZWFsIHdpdGggSW5saW5lVmFsdWVFdmFsdWF0YWJsZUV4cHJlc3Npb24uXG4gKlxuICogQHNpbmNlIDMuMTcuMFxuICovXG5leHBvcnQgdmFyIElubGluZVZhbHVlRXZhbHVhdGFibGVFeHByZXNzaW9uO1xuKGZ1bmN0aW9uIChJbmxpbmVWYWx1ZUV2YWx1YXRhYmxlRXhwcmVzc2lvbikge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgSW5saW5lVmFsdWVFdmFsdWF0YWJsZUV4cHJlc3Npb24gbGl0ZXJhbC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUocmFuZ2UsIGV4cHJlc3Npb24pIHtcbiAgICAgICAgcmV0dXJuIHsgcmFuZ2UsIGV4cHJlc3Npb24gfTtcbiAgICB9XG4gICAgSW5saW5lVmFsdWVFdmFsdWF0YWJsZUV4cHJlc3Npb24uY3JlYXRlID0gY3JlYXRlO1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICE9PSB1bmRlZmluZWQgJiYgY2FuZGlkYXRlICE9PSBudWxsICYmIFJhbmdlLmlzKGNhbmRpZGF0ZS5yYW5nZSlcbiAgICAgICAgICAgICYmIChJcy5zdHJpbmcoY2FuZGlkYXRlLmV4cHJlc3Npb24pIHx8IGNhbmRpZGF0ZS5leHByZXNzaW9uID09PSB1bmRlZmluZWQpO1xuICAgIH1cbiAgICBJbmxpbmVWYWx1ZUV2YWx1YXRhYmxlRXhwcmVzc2lvbi5pcyA9IGlzO1xufSkoSW5saW5lVmFsdWVFdmFsdWF0YWJsZUV4cHJlc3Npb24gfHwgKElubGluZVZhbHVlRXZhbHVhdGFibGVFeHByZXNzaW9uID0ge30pKTtcbi8qKlxuICogVGhlIElubGluZVZhbHVlQ29udGV4dCBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBJbmxpbmVWYWx1ZUNvbnRleHR9IGxpdGVyYWxzLlxuICpcbiAqIEBzaW5jZSAzLjE3LjBcbiAqL1xuZXhwb3J0IHZhciBJbmxpbmVWYWx1ZUNvbnRleHQ7XG4oZnVuY3Rpb24gKElubGluZVZhbHVlQ29udGV4dCkge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgSW5saW5lVmFsdWVDb250ZXh0IGxpdGVyYWwuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKGZyYW1lSWQsIHN0b3BwZWRMb2NhdGlvbikge1xuICAgICAgICByZXR1cm4geyBmcmFtZUlkLCBzdG9wcGVkTG9jYXRpb24gfTtcbiAgICB9XG4gICAgSW5saW5lVmFsdWVDb250ZXh0LmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIElubGluZVZhbHVlQ29udGV4dH0gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMuZGVmaW5lZChjYW5kaWRhdGUpICYmIFJhbmdlLmlzKHZhbHVlLnN0b3BwZWRMb2NhdGlvbik7XG4gICAgfVxuICAgIElubGluZVZhbHVlQ29udGV4dC5pcyA9IGlzO1xufSkoSW5saW5lVmFsdWVDb250ZXh0IHx8IChJbmxpbmVWYWx1ZUNvbnRleHQgPSB7fSkpO1xuLyoqXG4gKiBJbmxheSBoaW50IGtpbmRzLlxuICpcbiAqIEBzaW5jZSAzLjE3LjBcbiAqL1xuZXhwb3J0IHZhciBJbmxheUhpbnRLaW5kO1xuKGZ1bmN0aW9uIChJbmxheUhpbnRLaW5kKSB7XG4gICAgLyoqXG4gICAgICogQW4gaW5sYXkgaGludCB0aGF0IGZvciBhIHR5cGUgYW5ub3RhdGlvbi5cbiAgICAgKi9cbiAgICBJbmxheUhpbnRLaW5kLlR5cGUgPSAxO1xuICAgIC8qKlxuICAgICAqIEFuIGlubGF5IGhpbnQgdGhhdCBpcyBmb3IgYSBwYXJhbWV0ZXIuXG4gICAgICovXG4gICAgSW5sYXlIaW50S2luZC5QYXJhbWV0ZXIgPSAyO1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiB2YWx1ZSA9PT0gMSB8fCB2YWx1ZSA9PT0gMjtcbiAgICB9XG4gICAgSW5sYXlIaW50S2luZC5pcyA9IGlzO1xufSkoSW5sYXlIaW50S2luZCB8fCAoSW5sYXlIaW50S2luZCA9IHt9KSk7XG5leHBvcnQgdmFyIElubGF5SGludExhYmVsUGFydDtcbihmdW5jdGlvbiAoSW5sYXlIaW50TGFiZWxQYXJ0KSB7XG4gICAgZnVuY3Rpb24gY3JlYXRlKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiB7IHZhbHVlIH07XG4gICAgfVxuICAgIElubGF5SGludExhYmVsUGFydC5jcmVhdGUgPSBjcmVhdGU7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5vYmplY3RMaXRlcmFsKGNhbmRpZGF0ZSlcbiAgICAgICAgICAgICYmIChjYW5kaWRhdGUudG9vbHRpcCA9PT0gdW5kZWZpbmVkIHx8IElzLnN0cmluZyhjYW5kaWRhdGUudG9vbHRpcCkgfHwgTWFya3VwQ29udGVudC5pcyhjYW5kaWRhdGUudG9vbHRpcCkpXG4gICAgICAgICAgICAmJiAoY2FuZGlkYXRlLmxvY2F0aW9uID09PSB1bmRlZmluZWQgfHwgTG9jYXRpb24uaXMoY2FuZGlkYXRlLmxvY2F0aW9uKSlcbiAgICAgICAgICAgICYmIChjYW5kaWRhdGUuY29tbWFuZCA9PT0gdW5kZWZpbmVkIHx8IENvbW1hbmQuaXMoY2FuZGlkYXRlLmNvbW1hbmQpKTtcbiAgICB9XG4gICAgSW5sYXlIaW50TGFiZWxQYXJ0LmlzID0gaXM7XG59KShJbmxheUhpbnRMYWJlbFBhcnQgfHwgKElubGF5SGludExhYmVsUGFydCA9IHt9KSk7XG5leHBvcnQgdmFyIElubGF5SGludDtcbihmdW5jdGlvbiAoSW5sYXlIaW50KSB7XG4gICAgZnVuY3Rpb24gY3JlYXRlKHBvc2l0aW9uLCBsYWJlbCwga2luZCkge1xuICAgICAgICBjb25zdCByZXN1bHQgPSB7IHBvc2l0aW9uLCBsYWJlbCB9O1xuICAgICAgICBpZiAoa2luZCAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXN1bHQua2luZCA9IGtpbmQ7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICB9XG4gICAgSW5sYXlIaW50LmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLm9iamVjdExpdGVyYWwoY2FuZGlkYXRlKSAmJiBQb3NpdGlvbi5pcyhjYW5kaWRhdGUucG9zaXRpb24pXG4gICAgICAgICAgICAmJiAoSXMuc3RyaW5nKGNhbmRpZGF0ZS5sYWJlbCkgfHwgSXMudHlwZWRBcnJheShjYW5kaWRhdGUubGFiZWwsIElubGF5SGludExhYmVsUGFydC5pcykpXG4gICAgICAgICAgICAmJiAoY2FuZGlkYXRlLmtpbmQgPT09IHVuZGVmaW5lZCB8fCBJbmxheUhpbnRLaW5kLmlzKGNhbmRpZGF0ZS5raW5kKSlcbiAgICAgICAgICAgICYmIChjYW5kaWRhdGUudGV4dEVkaXRzID09PSB1bmRlZmluZWQpIHx8IElzLnR5cGVkQXJyYXkoY2FuZGlkYXRlLnRleHRFZGl0cywgVGV4dEVkaXQuaXMpXG4gICAgICAgICAgICAmJiAoY2FuZGlkYXRlLnRvb2x0aXAgPT09IHVuZGVmaW5lZCB8fCBJcy5zdHJpbmcoY2FuZGlkYXRlLnRvb2x0aXApIHx8IE1hcmt1cENvbnRlbnQuaXMoY2FuZGlkYXRlLnRvb2x0aXApKVxuICAgICAgICAgICAgJiYgKGNhbmRpZGF0ZS5wYWRkaW5nTGVmdCA9PT0gdW5kZWZpbmVkIHx8IElzLmJvb2xlYW4oY2FuZGlkYXRlLnBhZGRpbmdMZWZ0KSlcbiAgICAgICAgICAgICYmIChjYW5kaWRhdGUucGFkZGluZ1JpZ2h0ID09PSB1bmRlZmluZWQgfHwgSXMuYm9vbGVhbihjYW5kaWRhdGUucGFkZGluZ1JpZ2h0KSk7XG4gICAgfVxuICAgIElubGF5SGludC5pcyA9IGlzO1xufSkoSW5sYXlIaW50IHx8IChJbmxheUhpbnQgPSB7fSkpO1xuZXhwb3J0IHZhciBTdHJpbmdWYWx1ZTtcbihmdW5jdGlvbiAoU3RyaW5nVmFsdWUpIHtcbiAgICBmdW5jdGlvbiBjcmVhdGVTbmlwcGV0KHZhbHVlKSB7XG4gICAgICAgIHJldHVybiB7IGtpbmQ6ICdzbmlwcGV0JywgdmFsdWUgfTtcbiAgICB9XG4gICAgU3RyaW5nVmFsdWUuY3JlYXRlU25pcHBldCA9IGNyZWF0ZVNuaXBwZXQ7XG59KShTdHJpbmdWYWx1ZSB8fCAoU3RyaW5nVmFsdWUgPSB7fSkpO1xuZXhwb3J0IHZhciBJbmxpbmVDb21wbGV0aW9uSXRlbTtcbihmdW5jdGlvbiAoSW5saW5lQ29tcGxldGlvbkl0ZW0pIHtcbiAgICBmdW5jdGlvbiBjcmVhdGUoaW5zZXJ0VGV4dCwgZmlsdGVyVGV4dCwgcmFuZ2UsIGNvbW1hbmQpIHtcbiAgICAgICAgcmV0dXJuIHsgaW5zZXJ0VGV4dCwgZmlsdGVyVGV4dCwgcmFuZ2UsIGNvbW1hbmQgfTtcbiAgICB9XG4gICAgSW5saW5lQ29tcGxldGlvbkl0ZW0uY3JlYXRlID0gY3JlYXRlO1xufSkoSW5saW5lQ29tcGxldGlvbkl0ZW0gfHwgKElubGluZUNvbXBsZXRpb25JdGVtID0ge30pKTtcbmV4cG9ydCB2YXIgSW5saW5lQ29tcGxldGlvbkxpc3Q7XG4oZnVuY3Rpb24gKElubGluZUNvbXBsZXRpb25MaXN0KSB7XG4gICAgZnVuY3Rpb24gY3JlYXRlKGl0ZW1zKSB7XG4gICAgICAgIHJldHVybiB7IGl0ZW1zIH07XG4gICAgfVxuICAgIElubGluZUNvbXBsZXRpb25MaXN0LmNyZWF0ZSA9IGNyZWF0ZTtcbn0pKElubGluZUNvbXBsZXRpb25MaXN0IHx8IChJbmxpbmVDb21wbGV0aW9uTGlzdCA9IHt9KSk7XG4vKipcbiAqIERlc2NyaWJlcyBob3cgYW4ge0BsaW5rIElubGluZUNvbXBsZXRpb25JdGVtUHJvdmlkZXIgaW5saW5lIGNvbXBsZXRpb24gcHJvdmlkZXJ9IHdhcyB0cmlnZ2VyZWQuXG4gKlxuICogQHNpbmNlIDMuMTguMFxuICogQHByb3Bvc2VkXG4gKi9cbmV4cG9ydCB2YXIgSW5saW5lQ29tcGxldGlvblRyaWdnZXJLaW5kO1xuKGZ1bmN0aW9uIChJbmxpbmVDb21wbGV0aW9uVHJpZ2dlcktpbmQpIHtcbiAgICAvKipcbiAgICAgKiBDb21wbGV0aW9uIHdhcyB0cmlnZ2VyZWQgZXhwbGljaXRseSBieSBhIHVzZXIgZ2VzdHVyZS5cbiAgICAgKi9cbiAgICBJbmxpbmVDb21wbGV0aW9uVHJpZ2dlcktpbmQuSW52b2tlZCA9IDA7XG4gICAgLyoqXG4gICAgICogQ29tcGxldGlvbiB3YXMgdHJpZ2dlcmVkIGF1dG9tYXRpY2FsbHkgd2hpbGUgZWRpdGluZy5cbiAgICAgKi9cbiAgICBJbmxpbmVDb21wbGV0aW9uVHJpZ2dlcktpbmQuQXV0b21hdGljID0gMTtcbn0pKElubGluZUNvbXBsZXRpb25UcmlnZ2VyS2luZCB8fCAoSW5saW5lQ29tcGxldGlvblRyaWdnZXJLaW5kID0ge30pKTtcbmV4cG9ydCB2YXIgU2VsZWN0ZWRDb21wbGV0aW9uSW5mbztcbihmdW5jdGlvbiAoU2VsZWN0ZWRDb21wbGV0aW9uSW5mbykge1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShyYW5nZSwgdGV4dCkge1xuICAgICAgICByZXR1cm4geyByYW5nZSwgdGV4dCB9O1xuICAgIH1cbiAgICBTZWxlY3RlZENvbXBsZXRpb25JbmZvLmNyZWF0ZSA9IGNyZWF0ZTtcbn0pKFNlbGVjdGVkQ29tcGxldGlvbkluZm8gfHwgKFNlbGVjdGVkQ29tcGxldGlvbkluZm8gPSB7fSkpO1xuZXhwb3J0IHZhciBJbmxpbmVDb21wbGV0aW9uQ29udGV4dDtcbihmdW5jdGlvbiAoSW5saW5lQ29tcGxldGlvbkNvbnRleHQpIHtcbiAgICBmdW5jdGlvbiBjcmVhdGUodHJpZ2dlcktpbmQsIHNlbGVjdGVkQ29tcGxldGlvbkluZm8pIHtcbiAgICAgICAgcmV0dXJuIHsgdHJpZ2dlcktpbmQsIHNlbGVjdGVkQ29tcGxldGlvbkluZm8gfTtcbiAgICB9XG4gICAgSW5saW5lQ29tcGxldGlvbkNvbnRleHQuY3JlYXRlID0gY3JlYXRlO1xufSkoSW5saW5lQ29tcGxldGlvbkNvbnRleHQgfHwgKElubGluZUNvbXBsZXRpb25Db250ZXh0ID0ge30pKTtcbmV4cG9ydCB2YXIgV29ya3NwYWNlRm9sZGVyO1xuKGZ1bmN0aW9uIChXb3Jrc3BhY2VGb2xkZXIpIHtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLm9iamVjdExpdGVyYWwoY2FuZGlkYXRlKSAmJiBVUkkuaXMoY2FuZGlkYXRlLnVyaSkgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS5uYW1lKTtcbiAgICB9XG4gICAgV29ya3NwYWNlRm9sZGVyLmlzID0gaXM7XG59KShXb3Jrc3BhY2VGb2xkZXIgfHwgKFdvcmtzcGFjZUZvbGRlciA9IHt9KSk7XG5leHBvcnQgY29uc3QgRU9MID0gWydcXG4nLCAnXFxyXFxuJywgJ1xcciddO1xuLyoqXG4gKiBAZGVwcmVjYXRlZCBVc2UgdGhlIHRleHQgZG9jdW1lbnQgZnJvbSB0aGUgbmV3IHZzY29kZS1sYW5ndWFnZXNlcnZlci10ZXh0ZG9jdW1lbnQgcGFja2FnZS5cbiAqL1xuZXhwb3J0IHZhciBUZXh0RG9jdW1lbnQ7XG4oZnVuY3Rpb24gKFRleHREb2N1bWVudCkge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgSVRleHREb2N1bWVudCBsaXRlcmFsIGZyb20gdGhlIGdpdmVuIHVyaSBhbmQgY29udGVudC5cbiAgICAgKiBAcGFyYW0gdXJpIFRoZSBkb2N1bWVudCdzIHVyaS5cbiAgICAgKiBAcGFyYW0gbGFuZ3VhZ2VJZCBUaGUgZG9jdW1lbnQncyBsYW5ndWFnZSBJZC5cbiAgICAgKiBAcGFyYW0gdmVyc2lvbiBUaGUgZG9jdW1lbnQncyB2ZXJzaW9uLlxuICAgICAqIEBwYXJhbSBjb250ZW50IFRoZSBkb2N1bWVudCdzIGNvbnRlbnQuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKHVyaSwgbGFuZ3VhZ2VJZCwgdmVyc2lvbiwgY29udGVudCkge1xuICAgICAgICByZXR1cm4gbmV3IEZ1bGxUZXh0RG9jdW1lbnQodXJpLCBsYW5ndWFnZUlkLCB2ZXJzaW9uLCBjb250ZW50KTtcbiAgICB9XG4gICAgVGV4dERvY3VtZW50LmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIElUZXh0RG9jdW1lbnR9IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5kZWZpbmVkKGNhbmRpZGF0ZSkgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS51cmkpICYmIChJcy51bmRlZmluZWQoY2FuZGlkYXRlLmxhbmd1YWdlSWQpIHx8IElzLnN0cmluZyhjYW5kaWRhdGUubGFuZ3VhZ2VJZCkpICYmIElzLnVpbnRlZ2VyKGNhbmRpZGF0ZS5saW5lQ291bnQpXG4gICAgICAgICAgICAmJiBJcy5mdW5jKGNhbmRpZGF0ZS5nZXRUZXh0KSAmJiBJcy5mdW5jKGNhbmRpZGF0ZS5wb3NpdGlvbkF0KSAmJiBJcy5mdW5jKGNhbmRpZGF0ZS5vZmZzZXRBdCkgPyB0cnVlIDogZmFsc2U7XG4gICAgfVxuICAgIFRleHREb2N1bWVudC5pcyA9IGlzO1xuICAgIGZ1bmN0aW9uIGFwcGx5RWRpdHMoZG9jdW1lbnQsIGVkaXRzKSB7XG4gICAgICAgIGxldCB0ZXh0ID0gZG9jdW1lbnQuZ2V0VGV4dCgpO1xuICAgICAgICBsZXQgc29ydGVkRWRpdHMgPSBtZXJnZVNvcnQoZWRpdHMsIChhLCBiKSA9PiB7XG4gICAgICAgICAgICBsZXQgZGlmZiA9IGEucmFuZ2Uuc3RhcnQubGluZSAtIGIucmFuZ2Uuc3RhcnQubGluZTtcbiAgICAgICAgICAgIGlmIChkaWZmID09PSAwKSB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIGEucmFuZ2Uuc3RhcnQuY2hhcmFjdGVyIC0gYi5yYW5nZS5zdGFydC5jaGFyYWN0ZXI7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICByZXR1cm4gZGlmZjtcbiAgICAgICAgfSk7XG4gICAgICAgIGxldCBsYXN0TW9kaWZpZWRPZmZzZXQgPSB0ZXh0Lmxlbmd0aDtcbiAgICAgICAgZm9yIChsZXQgaSA9IHNvcnRlZEVkaXRzLmxlbmd0aCAtIDE7IGkgPj0gMDsgaS0tKSB7XG4gICAgICAgICAgICBsZXQgZSA9IHNvcnRlZEVkaXRzW2ldO1xuICAgICAgICAgICAgbGV0IHN0YXJ0T2Zmc2V0ID0gZG9jdW1lbnQub2Zmc2V0QXQoZS5yYW5nZS5zdGFydCk7XG4gICAgICAgICAgICBsZXQgZW5kT2Zmc2V0ID0gZG9jdW1lbnQub2Zmc2V0QXQoZS5yYW5nZS5lbmQpO1xuICAgICAgICAgICAgaWYgKGVuZE9mZnNldCA8PSBsYXN0TW9kaWZpZWRPZmZzZXQpIHtcbiAgICAgICAgICAgICAgICB0ZXh0ID0gdGV4dC5zdWJzdHJpbmcoMCwgc3RhcnRPZmZzZXQpICsgZS5uZXdUZXh0ICsgdGV4dC5zdWJzdHJpbmcoZW5kT2Zmc2V0LCB0ZXh0Lmxlbmd0aCk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoJ092ZXJsYXBwaW5nIGVkaXQnKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGxhc3RNb2RpZmllZE9mZnNldCA9IHN0YXJ0T2Zmc2V0O1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiB0ZXh0O1xuICAgIH1cbiAgICBUZXh0RG9jdW1lbnQuYXBwbHlFZGl0cyA9IGFwcGx5RWRpdHM7XG4gICAgZnVuY3Rpb24gbWVyZ2VTb3J0KGRhdGEsIGNvbXBhcmUpIHtcbiAgICAgICAgaWYgKGRhdGEubGVuZ3RoIDw9IDEpIHtcbiAgICAgICAgICAgIC8vIHNvcnRlZFxuICAgICAgICAgICAgcmV0dXJuIGRhdGE7XG4gICAgICAgIH1cbiAgICAgICAgY29uc3QgcCA9IChkYXRhLmxlbmd0aCAvIDIpIHwgMDtcbiAgICAgICAgY29uc3QgbGVmdCA9IGRhdGEuc2xpY2UoMCwgcCk7XG4gICAgICAgIGNvbnN0IHJpZ2h0ID0gZGF0YS5zbGljZShwKTtcbiAgICAgICAgbWVyZ2VTb3J0KGxlZnQsIGNvbXBhcmUpO1xuICAgICAgICBtZXJnZVNvcnQocmlnaHQsIGNvbXBhcmUpO1xuICAgICAgICBsZXQgbGVmdElkeCA9IDA7XG4gICAgICAgIGxldCByaWdodElkeCA9IDA7XG4gICAgICAgIGxldCBpID0gMDtcbiAgICAgICAgd2hpbGUgKGxlZnRJZHggPCBsZWZ0Lmxlbmd0aCAmJiByaWdodElkeCA8IHJpZ2h0Lmxlbmd0aCkge1xuICAgICAgICAgICAgbGV0IHJldCA9IGNvbXBhcmUobGVmdFtsZWZ0SWR4XSwgcmlnaHRbcmlnaHRJZHhdKTtcbiAgICAgICAgICAgIGlmIChyZXQgPD0gMCkge1xuICAgICAgICAgICAgICAgIC8vIHNtYWxsZXJfZXF1YWwgLT4gdGFrZSBsZWZ0IHRvIHByZXNlcnZlIG9yZGVyXG4gICAgICAgICAgICAgICAgZGF0YVtpKytdID0gbGVmdFtsZWZ0SWR4KytdO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgLy8gZ3JlYXRlciAtPiB0YWtlIHJpZ2h0XG4gICAgICAgICAgICAgICAgZGF0YVtpKytdID0gcmlnaHRbcmlnaHRJZHgrK107XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgd2hpbGUgKGxlZnRJZHggPCBsZWZ0Lmxlbmd0aCkge1xuICAgICAgICAgICAgZGF0YVtpKytdID0gbGVmdFtsZWZ0SWR4KytdO1xuICAgICAgICB9XG4gICAgICAgIHdoaWxlIChyaWdodElkeCA8IHJpZ2h0Lmxlbmd0aCkge1xuICAgICAgICAgICAgZGF0YVtpKytdID0gcmlnaHRbcmlnaHRJZHgrK107XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIGRhdGE7XG4gICAgfVxufSkoVGV4dERvY3VtZW50IHx8IChUZXh0RG9jdW1lbnQgPSB7fSkpO1xuLyoqXG4gKiBAZGVwcmVjYXRlZCBVc2UgdGhlIHRleHQgZG9jdW1lbnQgZnJvbSB0aGUgbmV3IHZzY29kZS1sYW5ndWFnZXNlcnZlci10ZXh0ZG9jdW1lbnQgcGFja2FnZS5cbiAqL1xuY2xhc3MgRnVsbFRleHREb2N1bWVudCB7XG4gICAgY29uc3RydWN0b3IodXJpLCBsYW5ndWFnZUlkLCB2ZXJzaW9uLCBjb250ZW50KSB7XG4gICAgICAgIHRoaXMuX3VyaSA9IHVyaTtcbiAgICAgICAgdGhpcy5fbGFuZ3VhZ2VJZCA9IGxhbmd1YWdlSWQ7XG4gICAgICAgIHRoaXMuX3ZlcnNpb24gPSB2ZXJzaW9uO1xuICAgICAgICB0aGlzLl9jb250ZW50ID0gY29udGVudDtcbiAgICAgICAgdGhpcy5fbGluZU9mZnNldHMgPSB1bmRlZmluZWQ7XG4gICAgfVxuICAgIGdldCB1cmkoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLl91cmk7XG4gICAgfVxuICAgIGdldCBsYW5ndWFnZUlkKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5fbGFuZ3VhZ2VJZDtcbiAgICB9XG4gICAgZ2V0IHZlcnNpb24oKSB7XG4gICAgICAgIHJldHVybiB0aGlzLl92ZXJzaW9uO1xuICAgIH1cbiAgICBnZXRUZXh0KHJhbmdlKSB7XG4gICAgICAgIGlmIChyYW5nZSkge1xuICAgICAgICAgICAgbGV0IHN0YXJ0ID0gdGhpcy5vZmZzZXRBdChyYW5nZS5zdGFydCk7XG4gICAgICAgICAgICBsZXQgZW5kID0gdGhpcy5vZmZzZXRBdChyYW5nZS5lbmQpO1xuICAgICAgICAgICAgcmV0dXJuIHRoaXMuX2NvbnRlbnQuc3Vic3RyaW5nKHN0YXJ0LCBlbmQpO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiB0aGlzLl9jb250ZW50O1xuICAgIH1cbiAgICB1cGRhdGUoZXZlbnQsIHZlcnNpb24pIHtcbiAgICAgICAgdGhpcy5fY29udGVudCA9IGV2ZW50LnRleHQ7XG4gICAgICAgIHRoaXMuX3ZlcnNpb24gPSB2ZXJzaW9uO1xuICAgICAgICB0aGlzLl9saW5lT2Zmc2V0cyA9IHVuZGVmaW5lZDtcbiAgICB9XG4gICAgZ2V0TGluZU9mZnNldHMoKSB7XG4gICAgICAgIGlmICh0aGlzLl9saW5lT2Zmc2V0cyA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICBsZXQgbGluZU9mZnNldHMgPSBbXTtcbiAgICAgICAgICAgIGxldCB0ZXh0ID0gdGhpcy5fY29udGVudDtcbiAgICAgICAgICAgIGxldCBpc0xpbmVTdGFydCA9IHRydWU7XG4gICAgICAgICAgICBmb3IgKGxldCBpID0gMDsgaSA8IHRleHQubGVuZ3RoOyBpKyspIHtcbiAgICAgICAgICAgICAgICBpZiAoaXNMaW5lU3RhcnQpIHtcbiAgICAgICAgICAgICAgICAgICAgbGluZU9mZnNldHMucHVzaChpKTtcbiAgICAgICAgICAgICAgICAgICAgaXNMaW5lU3RhcnQgPSBmYWxzZTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgbGV0IGNoID0gdGV4dC5jaGFyQXQoaSk7XG4gICAgICAgICAgICAgICAgaXNMaW5lU3RhcnQgPSAoY2ggPT09ICdcXHInIHx8IGNoID09PSAnXFxuJyk7XG4gICAgICAgICAgICAgICAgaWYgKGNoID09PSAnXFxyJyAmJiBpICsgMSA8IHRleHQubGVuZ3RoICYmIHRleHQuY2hhckF0KGkgKyAxKSA9PT0gJ1xcbicpIHtcbiAgICAgICAgICAgICAgICAgICAgaSsrO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmIChpc0xpbmVTdGFydCAmJiB0ZXh0Lmxlbmd0aCA+IDApIHtcbiAgICAgICAgICAgICAgICBsaW5lT2Zmc2V0cy5wdXNoKHRleHQubGVuZ3RoKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHRoaXMuX2xpbmVPZmZzZXRzID0gbGluZU9mZnNldHM7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHRoaXMuX2xpbmVPZmZzZXRzO1xuICAgIH1cbiAgICBwb3NpdGlvbkF0KG9mZnNldCkge1xuICAgICAgICBvZmZzZXQgPSBNYXRoLm1heChNYXRoLm1pbihvZmZzZXQsIHRoaXMuX2NvbnRlbnQubGVuZ3RoKSwgMCk7XG4gICAgICAgIGxldCBsaW5lT2Zmc2V0cyA9IHRoaXMuZ2V0TGluZU9mZnNldHMoKTtcbiAgICAgICAgbGV0IGxvdyA9IDAsIGhpZ2ggPSBsaW5lT2Zmc2V0cy5sZW5ndGg7XG4gICAgICAgIGlmIChoaWdoID09PSAwKSB7XG4gICAgICAgICAgICByZXR1cm4gUG9zaXRpb24uY3JlYXRlKDAsIG9mZnNldCk7XG4gICAgICAgIH1cbiAgICAgICAgd2hpbGUgKGxvdyA8IGhpZ2gpIHtcbiAgICAgICAgICAgIGxldCBtaWQgPSBNYXRoLmZsb29yKChsb3cgKyBoaWdoKSAvIDIpO1xuICAgICAgICAgICAgaWYgKGxpbmVPZmZzZXRzW21pZF0gPiBvZmZzZXQpIHtcbiAgICAgICAgICAgICAgICBoaWdoID0gbWlkO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgbG93ID0gbWlkICsgMTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICAvLyBsb3cgaXMgdGhlIGxlYXN0IHggZm9yIHdoaWNoIHRoZSBsaW5lIG9mZnNldCBpcyBsYXJnZXIgdGhhbiB0aGUgY3VycmVudCBvZmZzZXRcbiAgICAgICAgLy8gb3IgYXJyYXkubGVuZ3RoIGlmIG5vIGxpbmUgb2Zmc2V0IGlzIGxhcmdlciB0aGFuIHRoZSBjdXJyZW50IG9mZnNldFxuICAgICAgICBsZXQgbGluZSA9IGxvdyAtIDE7XG4gICAgICAgIHJldHVybiBQb3NpdGlvbi5jcmVhdGUobGluZSwgb2Zmc2V0IC0gbGluZU9mZnNldHNbbGluZV0pO1xuICAgIH1cbiAgICBvZmZzZXRBdChwb3NpdGlvbikge1xuICAgICAgICBsZXQgbGluZU9mZnNldHMgPSB0aGlzLmdldExpbmVPZmZzZXRzKCk7XG4gICAgICAgIGlmIChwb3NpdGlvbi5saW5lID49IGxpbmVPZmZzZXRzLmxlbmd0aCkge1xuICAgICAgICAgICAgcmV0dXJuIHRoaXMuX2NvbnRlbnQubGVuZ3RoO1xuICAgICAgICB9XG4gICAgICAgIGVsc2UgaWYgKHBvc2l0aW9uLmxpbmUgPCAwKSB7XG4gICAgICAgICAgICByZXR1cm4gMDtcbiAgICAgICAgfVxuICAgICAgICBsZXQgbGluZU9mZnNldCA9IGxpbmVPZmZzZXRzW3Bvc2l0aW9uLmxpbmVdO1xuICAgICAgICBsZXQgbmV4dExpbmVPZmZzZXQgPSAocG9zaXRpb24ubGluZSArIDEgPCBsaW5lT2Zmc2V0cy5sZW5ndGgpID8gbGluZU9mZnNldHNbcG9zaXRpb24ubGluZSArIDFdIDogdGhpcy5fY29udGVudC5sZW5ndGg7XG4gICAgICAgIHJldHVybiBNYXRoLm1heChNYXRoLm1pbihsaW5lT2Zmc2V0ICsgcG9zaXRpb24uY2hhcmFjdGVyLCBuZXh0TGluZU9mZnNldCksIGxpbmVPZmZzZXQpO1xuICAgIH1cbiAgICBnZXQgbGluZUNvdW50KCkge1xuICAgICAgICByZXR1cm4gdGhpcy5nZXRMaW5lT2Zmc2V0cygpLmxlbmd0aDtcbiAgICB9XG59XG52YXIgSXM7XG4oZnVuY3Rpb24gKElzKSB7XG4gICAgY29uc3QgdG9TdHJpbmcgPSBPYmplY3QucHJvdG90eXBlLnRvU3RyaW5nO1xuICAgIGZ1bmN0aW9uIGRlZmluZWQodmFsdWUpIHtcbiAgICAgICAgcmV0dXJuIHR5cGVvZiB2YWx1ZSAhPT0gJ3VuZGVmaW5lZCc7XG4gICAgfVxuICAgIElzLmRlZmluZWQgPSBkZWZpbmVkO1xuICAgIGZ1bmN0aW9uIHVuZGVmaW5lZCh2YWx1ZSkge1xuICAgICAgICByZXR1cm4gdHlwZW9mIHZhbHVlID09PSAndW5kZWZpbmVkJztcbiAgICB9XG4gICAgSXMudW5kZWZpbmVkID0gdW5kZWZpbmVkO1xuICAgIGZ1bmN0aW9uIGJvb2xlYW4odmFsdWUpIHtcbiAgICAgICAgcmV0dXJuIHZhbHVlID09PSB0cnVlIHx8IHZhbHVlID09PSBmYWxzZTtcbiAgICB9XG4gICAgSXMuYm9vbGVhbiA9IGJvb2xlYW47XG4gICAgZnVuY3Rpb24gc3RyaW5nKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiB0b1N0cmluZy5jYWxsKHZhbHVlKSA9PT0gJ1tvYmplY3QgU3RyaW5nXSc7XG4gICAgfVxuICAgIElzLnN0cmluZyA9IHN0cmluZztcbiAgICBmdW5jdGlvbiBudW1iZXIodmFsdWUpIHtcbiAgICAgICAgcmV0dXJuIHRvU3RyaW5nLmNhbGwodmFsdWUpID09PSAnW29iamVjdCBOdW1iZXJdJztcbiAgICB9XG4gICAgSXMubnVtYmVyID0gbnVtYmVyO1xuICAgIGZ1bmN0aW9uIG51bWJlclJhbmdlKHZhbHVlLCBtaW4sIG1heCkge1xuICAgICAgICByZXR1cm4gdG9TdHJpbmcuY2FsbCh2YWx1ZSkgPT09ICdbb2JqZWN0IE51bWJlcl0nICYmIG1pbiA8PSB2YWx1ZSAmJiB2YWx1ZSA8PSBtYXg7XG4gICAgfVxuICAgIElzLm51bWJlclJhbmdlID0gbnVtYmVyUmFuZ2U7XG4gICAgZnVuY3Rpb24gaW50ZWdlcih2YWx1ZSkge1xuICAgICAgICByZXR1cm4gdG9TdHJpbmcuY2FsbCh2YWx1ZSkgPT09ICdbb2JqZWN0IE51bWJlcl0nICYmIC0yMTQ3NDgzNjQ4IDw9IHZhbHVlICYmIHZhbHVlIDw9IDIxNDc0ODM2NDc7XG4gICAgfVxuICAgIElzLmludGVnZXIgPSBpbnRlZ2VyO1xuICAgIGZ1bmN0aW9uIHVpbnRlZ2VyKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiB0b1N0cmluZy5jYWxsKHZhbHVlKSA9PT0gJ1tvYmplY3QgTnVtYmVyXScgJiYgMCA8PSB2YWx1ZSAmJiB2YWx1ZSA8PSAyMTQ3NDgzNjQ3O1xuICAgIH1cbiAgICBJcy51aW50ZWdlciA9IHVpbnRlZ2VyO1xuICAgIGZ1bmN0aW9uIGZ1bmModmFsdWUpIHtcbiAgICAgICAgcmV0dXJuIHRvU3RyaW5nLmNhbGwodmFsdWUpID09PSAnW29iamVjdCBGdW5jdGlvbl0nO1xuICAgIH1cbiAgICBJcy5mdW5jID0gZnVuYztcbiAgICBmdW5jdGlvbiBvYmplY3RMaXRlcmFsKHZhbHVlKSB7XG4gICAgICAgIC8vIFN0cmljdGx5IHNwZWFraW5nIGNsYXNzIGluc3RhbmNlcyBwYXNzIHRoaXMgY2hlY2sgYXMgd2VsbC4gU2luY2UgdGhlIExTUFxuICAgICAgICAvLyBkb2Vzbid0IHVzZSBjbGFzc2VzIHdlIGlnbm9yZSB0aGlzIGZvciBub3cuIElmIHdlIGRvIHdlIG5lZWQgdG8gYWRkIHNvbWV0aGluZ1xuICAgICAgICAvLyBsaWtlIHRoaXM6IGBPYmplY3QuZ2V0UHJvdG90eXBlT2YoT2JqZWN0LmdldFByb3RvdHlwZU9mKHgpKSA9PT0gbnVsbGBcbiAgICAgICAgcmV0dXJuIHZhbHVlICE9PSBudWxsICYmIHR5cGVvZiB2YWx1ZSA9PT0gJ29iamVjdCc7XG4gICAgfVxuICAgIElzLm9iamVjdExpdGVyYWwgPSBvYmplY3RMaXRlcmFsO1xuICAgIGZ1bmN0aW9uIHR5cGVkQXJyYXkodmFsdWUsIGNoZWNrKSB7XG4gICAgICAgIHJldHVybiBBcnJheS5pc0FycmF5KHZhbHVlKSAmJiB2YWx1ZS5ldmVyeShjaGVjayk7XG4gICAgfVxuICAgIElzLnR5cGVkQXJyYXkgPSB0eXBlZEFycmF5O1xufSkoSXMgfHwgKElzID0ge30pKTtcbiJdLAogICJtYXBwaW5ncyI6ICI7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7OztBQUFBO0FBQUE7QUFBQTtBQUtBLGFBQU8sZUFBZSxTQUFTLGNBQWMsRUFBRSxPQUFPLEtBQUssQ0FBQztBQUM1RCxjQUFRLGNBQWMsUUFBUSxRQUFRLFFBQVEsT0FBTyxRQUFRLFFBQVEsUUFBUSxTQUFTLFFBQVEsU0FBUyxRQUFRLFVBQVU7QUFDekgsZUFBUyxRQUFR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LEtBQUs7QUFBQSxRQUNoQjtBQUFBLE1BQ0o7QUFDQSxjQUFRLGVBQWU7QUFDdkIsVUFBTSxlQUFOLGNBQTJCLHlCQUF5QjtBQUFBLFFBQ2hELFlBQVksUUFBUTtBQUNoQixnQkFBTSxRQUFRLENBQUM7QUFBQSxRQUNuQjtBQUFBLE1BQ0o7QUFDQSxjQUFRLGVBQWU7QUFDdkIsVUFBTSxlQUFOLGNBQTJCLHlCQUF5QjtBQUFBLFFBQ2hELFlBQVksUUFBUTtBQUNoQixnQkFBTSxRQUFRLENBQUM7QUFBQSxRQUNuQjtBQUFBLE1BQ0o7QUFDQSxjQUFRLGVBQWU7QUFDdkIsVUFBTSxlQUFOLGNBQTJCLHlCQUF5QjtBQUFBLFFBQ2hELFlBQVksUUFBUTtBQUNoQixnQkFBTSxRQUFRLENBQUM7QUFBQSxRQUNuQjtBQUFBLE1BQ0o7QUFDQSxjQUFRLGVBQWU7QUFDdkIsVUFBTSxlQUFOLGNBQTJCLHlCQUF5QjtBQUFBLFFBQ2hELFlBQVksUUFBUTtBQUNoQixnQkFBTSxRQUFRLENBQUM7QUFBQSxRQUNuQjtBQUFBLE1BQ0o7QUFDQSxjQUFRLGVBQWU7QUFDdkIsVUFBTSxlQUFOLGNBQTJCLHlCQUF5QjtBQUFBLFFBQ2hELFlBQVksUUFBUTtBQUNoQixnQkFBTSxRQUFRLENBQUM7QUFBQSxRQUNuQjtBQUFBLE1BQ0o7QUFDQSxjQUFRLGVBQWU7QUFDdkIsVUFBTSxlQUFOLGNBQTJCLHlCQUF5QjtBQUFBLFFBQ2hELFlBQVksUUFBUTtBQUNoQixnQkFBTSxRQUFRLENBQUM7QUFBQSxRQUNuQjtBQUFBLE1BQ0o7QUFDQSxjQUFRLGVBQWU7QUFDdkIsVUFBTSxlQUFOLGNBQTJCLHlCQUF5QjtBQUFBLFFBQ2hELFlBQVksUUFBUTtBQUNoQixnQkFBTSxRQUFRLENBQUM7QUFBQSxRQUNuQjtBQUFBLE1BQ0o7QUFDQSxjQUFRLGVBQWU7QUFDdkIsVUFBTSxlQUFOLGNBQTJCLHlCQUF5QjtBQUFBLFFBQ2hELFlBQVksUUFBUTtBQUNoQixnQkFBTSxRQUFRLENBQUM7QUFBQSxRQUNuQjtBQUFBLE1BQ0o7QUFDQSxjQUFR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LE1BQU0sRUFBRTtBQUFBO0FBQUE7QUFBQSxRQUdyRCxvQkFBd0IsRUFBRSxTQUFTLE1BQU07QUFBQSxRQUN6Qyx5QkFBeUIsRUFBRSxTQUFTLE1BQU07QUFBQSxNQUM5QztBQUFBLElBQ0o7QUFBQSxFQUNKOyIsCiAgIm5hbWVzIjogWyJFcnJvckNvZGVzIiwgIk1lc3NhZ2UiLCAiVG91Y2giLCAiRGlzcG9zYWJsZSIsICJSQUwiLCAiRXZlbnQiLCAiSXMiLCAiQ2FuY2VsbGF0aW9uVG9rZW4iLCAiQ2FuY2VsbGF0aW9uU3RhdGUiLCAiX2Nvbm4iLCAiSXMiLCAiTWVzc2FnZVJlYWRlciIsICJBYnN0cmFjdE1lc3NhZ2VSZWFkZXIiLCAiUmVzb2x2ZWRNZXNzYWdlUmVhZGVyT3B0aW9ucyIsICJJcyIsICJNZXNzYWdlV3JpdGVyIiwgIkFic3RyYWN0TWVzc2FnZVdyaXRlciIsICJSZXNvbHZlZE1lc3NhZ2VXcml0ZXJPcHRpb25zIiwgInJlc3VsdCIsICJJcyIsICJDYW5jZWxOb3RpZmljYXRpb24iLCAiUHJvZ3Jlc3NUb2tlbiIsICJQcm9ncmVzc05vdGlmaWNhdGlvbiIsICJTdGFyUmVxdWVzdEhhbmRsZXIiLCAiVHJhY2UiLCAiVHJhY2VWYWx1ZXMiLCAiVHJhY2VGb3JtYXQiLCAiU2V0VHJhY2VOb3RpZmljYXRpb24iLCAiTG9nVHJhY2VOb3RpZmljYXRpb24iLCAiQ29ubmVjdGlvbkVycm9ycyIsICJDb25uZWN0aW9uU3RyYXRlZ3kiLCAiSWRDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5IiwgIlJlcXVlc3RDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5IiwgIkNhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kiLCAiQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3kiLCAiQ2FuY2VsbGF0aW9uU3RyYXRlZ3kiLCAiTWVzc2FnZVN0cmF0ZWd5IiwgIkNvbm5lY3Rpb25PcHRpb25zIiwgIkNvbm5lY3Rpb25TdGF0ZSIsICJjcmVhdGVNZXNzYWdlQ29ubmVjdGlvbiIsICJzdGFydFRpbWUiLCAiUklMIiwgIm0iLCAiZXhwb3J0cyIsICJjcmVhdGVNZXNzYWdlQ29ubmVjdGlvbiIsICJpbXBvcnRfdnNjb2RlX2pzb25ycGMiLCAiaW1wb3J0X3ZzY29kZV9qc29ucnBjIiwgImltcG9ydF92c2NvZGVfanNvbnJwYyIsICJpbXBvcnRfdnNjb2RlX2pzb25ycGMiLCAiRG9jdW1lbnRVcmkiLCAiVVJJIiwgImludGVnZXIiLCAidWludGVnZXIiLCAiUG9zaXRpb24iLCAiUmFuZ2UiLCAiTG9jYXRpb24iLCAiTG9jYXRpb25MaW5rIiwgIkNvbG9yIiwgIkNvbG9ySW5mb3JtYXRpb24iLCAiQ29sb3JQcmVzZW50YXRpb24iLCAiRm9sZGluZ1JhbmdlS2luZCIsICJGb2xkaW5nUmFuZ2UiLCAiRGlhZ25vc3RpY1JlbGF0ZWRJbmZvcm1hdGlvbiIsICJsb2NhdGlvbiIsICJEaWFnbm9zdGljU2V2ZXJpdHkiLCAiRGlhZ25vc3RpY1RhZyIsICJDb2RlRGVzY3JpcHRpb24iLCAiRGlhZ25vc3RpYyIsICJDb21tYW5kIiwgIlRleHRFZGl0IiwgIkNoYW5nZUFubm90YXRpb24iLCAiQ2hhbmdlQW5ub3RhdGlvbklkZW50aWZpZXIiLCAiQW5ub3RhdGVkVGV4dEVkaXQiLCAiVGV4dERvY3VtZW50RWRpdCIsICJDcmVhdGVGaWxlIiwgIlJlbmFtZUZpbGUiLCAiRGVsZXRlRmlsZSIsICJXb3Jrc3BhY2VFZGl0IiwgIlRleHREb2N1bWVudElkZW50aWZpZXIiLCAiVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllciIsICJPcHRpb25hbFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXIiLCAiVGV4dERvY3VtZW50SXRlbSIsICJNYXJrdXBLaW5kIiwgIk1hcmt1cENvbnRlbnQiLCAiQ29tcGxldGlvbkl0ZW1LaW5kIiwgIkluc2VydFRleHRGb3JtYXQiLCAiQ29tcGxldGlvbkl0ZW1UYWciLCAiSW5zZXJ0UmVwbGFjZUVkaXQiLCAiUmFuZ2UiLCAiSW5zZXJ0VGV4dE1vZGUiLCAiQ29tcGxldGlvbkl0ZW1MYWJlbERldGFpbHMiLCAiQ29tcGxldGlvbkl0ZW0iLCAiQ29tcGxldGlvbkxpc3QiLCAiTWFya2VkU3RyaW5nIiwgIkhvdmVyIiwgIlBhcmFtZXRlckluZm9ybWF0aW9uIiwgIlNpZ25hdHVyZUluZm9ybWF0aW9uIiwgIkRvY3VtZW50SGlnaGxpZ2h0S2luZCIsICJEb2N1bWVudEhpZ2hsaWdodCIsICJTeW1ib2xLaW5kIiwgIlN5bWJvbFRhZyIsICJTeW1ib2xJbmZvcm1hdGlvbiIsICJXb3Jrc3BhY2VTeW1ib2wiLCAiRG9jdW1lbnRTeW1ib2wiLCAiQ29kZUFjdGlvbktpbmQiLCAiQ29kZUFjdGlvblRyaWdnZXJLaW5kIiwgIkNvZGVBY3Rpb25Db250ZXh0IiwgIkNvZGVBY3Rpb24iLCAiQ29kZUxlbnMiLCAiRm9ybWF0dGluZ09wdGlvbnMiLCAiRG9jdW1lbnRMaW5rIiwgIlNlbGVjdGlvblJhbmdlIiwgIlNlbWFudGljVG9rZW5UeXBlcyIsICJTZW1hbnRpY1Rva2VuTW9kaWZpZXJzIiwgIlNlbWFudGljVG9rZW5zIiwgIklubGluZVZhbHVlVGV4dCIsICJJbmxpbmVWYWx1ZVZhcmlhYmxlTG9va3VwIiwgIklubGluZVZhbHVlRXZhbHVhdGFibGVFeHByZXNzaW9uIiwgIklubGluZVZhbHVlQ29udGV4dCIsICJJbmxheUhpbnRLaW5kIiwgIklubGF5SGludExhYmVsUGFydCIsICJJbmxheUhpbnQiLCAiUG9zaXRpb24iLCAiU3RyaW5nVmFsdWUiLCAiSW5saW5lQ29tcGxldGlvbkl0ZW0iLCAiSW5saW5lQ29tcGxldGlvbkxpc3QiLCAiSW5saW5lQ29tcGxldGlvblRyaWdnZXJLaW5kIiwgIlNlbGVjdGVkQ29tcGxldGlvbkluZm8iLCAiSW5saW5lQ29tcGxldGlvbkNvbnRleHQiLCAiV29ya3NwYWNlRm9sZGVyIiwgIlRleHREb2N1bWVudCIsICJQb3NpdGlvbiIsICJJcyIsICJ1bmRlZmluZWQiLCAiaW50ZWdlciIsICJ1aW50ZWdlciIsICJtIl0KfQo=
