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
  var _providersRegistered = false;
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
    await _conn.sendRequest("initialize", {
      processId: null,
      rootUri,
      initializationOptions: {
        settings: jdtlsSettings(),
        extendedClientCapabilities: {
          progressReportProvider: false,
          classFileContentsSupport: true,
          resolveAdditionalTextEditsSupport: true,
          advancedGenerateAccessorsSupport: true,
          generateToStringPromptSupport: true,
          generateConstructorsPromptSupport: true,
          generateDelegateMethodsPromptSupport: true,
          hashCodeEqualsPromptSupport: true,
          advancedOrganizeImportsSupport: true,
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
          documentSymbol: { dynamicRegistration: true },
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
    if (model) {
      let timer;
      model.onDidChangeContent(() => {
        clearTimeout(timer);
        timer = setTimeout(() => {
          _conn?.sendNotification("textDocument/didChange", {
            textDocument: { uri: fileUri, version: model.getVersionId() },
            contentChanges: [{ text: model.getValue() }]
          });
        }, 400);
      });
    }
  }
  function isWorkspaceFile(uri) {
    return _workspaceRoot !== "" && uri.startsWith(_workspaceRoot);
  }
  function registerProviders() {
    editor.registerCommand(APPLY_ACTION_COMMAND, (_accessor, action) => {
      applyCodeAction(action);
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
        const locations = Array.isArray(result) ? result : [result];
        return locations.map((loc) => ({
          uri: Uri.parse(loc.uri),
          range: lspRangeToMonaco(loc.range)
        }));
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
        return result.map((loc) => ({ uri: Uri.parse(loc.uri), range: lspRangeToMonaco(loc.range) }));
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
        const diagnostics = (context.markers ?? []).map(markerToLspDiagnostic);
        const result = await _conn.sendRequest("textDocument/codeAction", {
          textDocument: { uri: model.uri.toString() },
          range: monacoRangeToLsp(range),
          context: { diagnostics, only: context.only ? [context.only] : void 0 }
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
  function textEditToMonaco(edit) {
    return { range: lspRangeToMonaco(edit.range), text: edit.newText };
  }
  function monacoRangeToLsp(r) {
    return {
      start: { line: r.startLineNumber - 1, character: r.startColumn - 1 },
      end: { line: r.endLineNumber - 1, character: r.endColumn - 1 }
    };
  }
  function monacoSeverityToLsp(severity) {
    switch (severity) {
      case MarkerSeverity.Error:
        return DiagnosticSeverity.Error;
      case MarkerSeverity.Warning:
        return DiagnosticSeverity.Warning;
      case MarkerSeverity.Info:
        return DiagnosticSeverity.Information;
      default:
        return DiagnosticSeverity.Hint;
    }
  }
  function markerToLspDiagnostic(m2) {
    return {
      range: {
        start: { line: m2.startLineNumber - 1, character: m2.startColumn - 1 },
        end: { line: m2.endLineNumber - 1, character: m2.endColumn - 1 }
      },
      message: m2.message,
      severity: monacoSeverityToLsp(m2.severity),
      source: m2.source
    };
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
    const payload = { currentPath: uriToWorkspacePath(currentUri), currentContent: null, currentNewPath: null, writes: [], renames: [] };
    const uris = /* @__PURE__ */ new Set([...Object.keys(textByUri), ...Object.keys(renameByUri)]);
    for (const uri of uris) {
      const edits = textByUri[uri] ?? [];
      let content;
      if (uri === currentUri) {
        if (edits.length) {
          model.pushEditOperations([], edits.map((e) => ({ range: lspRangeToMonaco(e.range), text: e.newText, forceMoveMarkers: true })), () => null);
        }
        content = model.getValue();
      } else {
        const source = await fetchWorkspaceFileText(uriToWorkspacePath(uri) ?? uri);
        content = edits.length ? applyEditsToText(source, edits) : source;
      }
      const newUri = renameByUri[uri];
      if (uri === currentUri) {
        payload.currentContent = content;
        payload.currentNewPath = newUri ? uriToWorkspacePath(newUri) : null;
      } else if (newUri) {
        payload.renames.push({ oldPath: uriToWorkspacePath(uri), newPath: uriToWorkspacePath(newUri), content });
      } else {
        payload.writes.push({ path: uriToWorkspacePath(uri), content });
      }
    }
    await globalThis.javaLspPersistRename(payload);
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
        saveActions: { organizeImports: false }
      }
    };
  }
  return __toCommonJS(java_lsp_client_exports);
})();
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtanNvbnJwYy9saWIvY29tbW9uL2lzLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9tZXNzYWdlcy5qcyIsICJsc3Avbm9kZV9tb2R1bGVzL3ZzY29kZS1qc29ucnBjL2xpYi9jb21tb24vbGlua2VkTWFwLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9kaXNwb3NhYmxlLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9yYWwuanMiLCAibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtanNvbnJwYy9saWIvY29tbW9uL2V2ZW50cy5qcyIsICJsc3Avbm9kZV9tb2R1bGVzL3ZzY29kZS1qc29ucnBjL2xpYi9jb21tb24vY2FuY2VsbGF0aW9uLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9zaGFyZWRBcnJheUNhbmNlbGxhdGlvbi5qcyIsICJsc3Avbm9kZV9tb2R1bGVzL3ZzY29kZS1qc29ucnBjL2xpYi9jb21tb24vc2VtYXBob3JlLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9tZXNzYWdlUmVhZGVyLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9tZXNzYWdlV3JpdGVyLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9tZXNzYWdlQnVmZmVyLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9jb25uZWN0aW9uLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9hcGkuanMiLCAibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtanNvbnJwYy9saWIvYnJvd3Nlci9yaWwuanMiLCAibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtanNvbnJwYy9saWIvYnJvd3Nlci9tYWluLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvYnJvd3Nlci5qcyIsICJsc3AvamF2YS1sc3AtY2xpZW50LnRzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLXdzLWpzb25ycGMvc3JjL2Rpc3Bvc2FibGUudHMiLCAibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtd3MtanNvbnJwYy9zcmMvc29ja2V0L3NvY2tldC50cyIsICJsc3Avbm9kZV9tb2R1bGVzL3ZzY29kZS13cy1qc29ucnBjL3NyYy9zb2NrZXQvcmVhZGVyLnRzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLXdzLWpzb25ycGMvc3JjL3NvY2tldC93cml0ZXIudHMiLCAibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtd3MtanNvbnJwYy9zcmMvc29ja2V0L2Nvbm5lY3Rpb24udHMiLCAibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtd3MtanNvbnJwYy9zcmMvY29ubmVjdGlvbi50cyIsICJsc3AvbW9uYWNvLXNoaW0udHMiLCAibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtbGFuZ3VhZ2VzZXJ2ZXItdHlwZXMvbGliL2VzbS9tYWluLmpzIl0sCiAgInNvdXJjZXNDb250ZW50IjogWyJcInVzZSBzdHJpY3RcIjtcbi8qIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiBDb3B5cmlnaHQgKGMpIE1pY3Jvc29mdCBDb3Jwb3JhdGlvbi4gQWxsIHJpZ2h0cyByZXNlcnZlZC5cbiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBNSVQgTGljZW5zZS4gU2VlIExpY2Vuc2UudHh0IGluIHRoZSBwcm9qZWN0IHJvb3QgZm9yIGxpY2Vuc2UgaW5mb3JtYXRpb24uXG4gKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0gKi9cbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIl9fZXNNb2R1bGVcIiwgeyB2YWx1ZTogdHJ1ZSB9KTtcbmV4cG9ydHMuc3RyaW5nQXJyYXkgPSBleHBvcnRzLmFycmF5ID0gZXhwb3J0cy5mdW5jID0gZXhwb3J0cy5lcnJvciA9IGV4cG9ydHMubnVtYmVyID0gZXhwb3J0cy5zdHJpbmcgPSBleHBvcnRzLmJvb2xlYW4gPSB2b2lkIDA7XG5mdW5jdGlvbiBib29sZWFuKHZhbHVlKSB7XG4gICAgcmV0dXJuIHZhbHVlID09PSB0cnVlIHx8IHZhbHVlID09PSBmYWxzZTtcbn1cbmV4cG9ydHMuYm9vbGVhbiA9IGJvb2xlYW47XG5mdW5jdGlvbiBzdHJpbmcodmFsdWUpIHtcbiAgICByZXR1cm4gdHlwZW9mIHZhbHVlID09PSAnc3RyaW5nJyB8fCB2YWx1ZSBpbnN0YW5jZW9mIFN0cmluZztcbn1cbmV4cG9ydHMuc3RyaW5nID0gc3RyaW5nO1xuZnVuY3Rpb24gbnVtYmVyKHZhbHVlKSB7XG4gICAgcmV0dXJuIHR5cGVvZiB2YWx1ZSA9PT0gJ251bWJlcicgfHwgdmFsdWUgaW5zdGFuY2VvZiBOdW1iZXI7XG59XG5leHBvcnRzLm51bWJlciA9IG51bWJlcjtcbmZ1bmN0aW9uIGVycm9yKHZhbHVlKSB7XG4gICAgcmV0dXJuIHZhbHVlIGluc3RhbmNlb2YgRXJyb3I7XG59XG5leHBvcnRzLmVycm9yID0gZXJyb3I7XG5mdW5jdGlvbiBmdW5jKHZhbHVlKSB7XG4gICAgcmV0dXJuIHR5cGVvZiB2YWx1ZSA9PT0gJ2Z1bmN0aW9uJztcbn1cbmV4cG9ydHMuZnVuYyA9IGZ1bmM7XG5mdW5jdGlvbiBhcnJheSh2YWx1ZSkge1xuICAgIHJldHVybiBBcnJheS5pc0FycmF5KHZhbHVlKTtcbn1cbmV4cG9ydHMuYXJyYXkgPSBhcnJheTtcbmZ1bmN0aW9uIHN0cmluZ0FycmF5KHZhbHVlKSB7XG4gICAgcmV0dXJuIGFycmF5KHZhbHVlKSAmJiB2YWx1ZS5ldmVyeShlbGVtID0+IHN0cmluZyhlbGVtKSk7XG59XG5leHBvcnRzLnN0cmluZ0FycmF5ID0gc3RyaW5nQXJyYXk7XG4iLCAiXCJ1c2Ugc3RyaWN0XCI7XG4vKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJfX2VzTW9kdWxlXCIsIHsgdmFsdWU6IHRydWUgfSk7XG5leHBvcnRzLk1lc3NhZ2UgPSBleHBvcnRzLk5vdGlmaWNhdGlvblR5cGU5ID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlOCA9IGV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTcgPSBleHBvcnRzLk5vdGlmaWNhdGlvblR5cGU2ID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlNSA9IGV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTQgPSBleHBvcnRzLk5vdGlmaWNhdGlvblR5cGUzID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlMiA9IGV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTEgPSBleHBvcnRzLk5vdGlmaWNhdGlvblR5cGUwID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlID0gZXhwb3J0cy5SZXF1ZXN0VHlwZTkgPSBleHBvcnRzLlJlcXVlc3RUeXBlOCA9IGV4cG9ydHMuUmVxdWVzdFR5cGU3ID0gZXhwb3J0cy5SZXF1ZXN0VHlwZTYgPSBleHBvcnRzLlJlcXVlc3RUeXBlNSA9IGV4cG9ydHMuUmVxdWVzdFR5cGU0ID0gZXhwb3J0cy5SZXF1ZXN0VHlwZTMgPSBleHBvcnRzLlJlcXVlc3RUeXBlMiA9IGV4cG9ydHMuUmVxdWVzdFR5cGUxID0gZXhwb3J0cy5SZXF1ZXN0VHlwZSA9IGV4cG9ydHMuUmVxdWVzdFR5cGUwID0gZXhwb3J0cy5BYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUgPSBleHBvcnRzLlBhcmFtZXRlclN0cnVjdHVyZXMgPSBleHBvcnRzLlJlc3BvbnNlRXJyb3IgPSBleHBvcnRzLkVycm9yQ29kZXMgPSB2b2lkIDA7XG5jb25zdCBpcyA9IHJlcXVpcmUoXCIuL2lzXCIpO1xuLyoqXG4gKiBQcmVkZWZpbmVkIGVycm9yIGNvZGVzLlxuICovXG52YXIgRXJyb3JDb2RlcztcbihmdW5jdGlvbiAoRXJyb3JDb2Rlcykge1xuICAgIC8vIERlZmluZWQgYnkgSlNPTiBSUENcbiAgICBFcnJvckNvZGVzLlBhcnNlRXJyb3IgPSAtMzI3MDA7XG4gICAgRXJyb3JDb2Rlcy5JbnZhbGlkUmVxdWVzdCA9IC0zMjYwMDtcbiAgICBFcnJvckNvZGVzLk1ldGhvZE5vdEZvdW5kID0gLTMyNjAxO1xuICAgIEVycm9yQ29kZXMuSW52YWxpZFBhcmFtcyA9IC0zMjYwMjtcbiAgICBFcnJvckNvZGVzLkludGVybmFsRXJyb3IgPSAtMzI2MDM7XG4gICAgLyoqXG4gICAgICogVGhpcyBpcyB0aGUgc3RhcnQgcmFuZ2Ugb2YgSlNPTiBSUEMgcmVzZXJ2ZWQgZXJyb3IgY29kZXMuXG4gICAgICogSXQgZG9lc24ndCBkZW5vdGUgYSByZWFsIGVycm9yIGNvZGUuIE5vIGFwcGxpY2F0aW9uIGVycm9yIGNvZGVzIHNob3VsZFxuICAgICAqIGJlIGRlZmluZWQgYmV0d2VlbiB0aGUgc3RhcnQgYW5kIGVuZCByYW5nZS4gRm9yIGJhY2t3YXJkc1xuICAgICAqIGNvbXBhdGliaWxpdHkgdGhlIGBTZXJ2ZXJOb3RJbml0aWFsaXplZGAgYW5kIHRoZSBgVW5rbm93bkVycm9yQ29kZWBcbiAgICAgKiBhcmUgbGVmdCBpbiB0aGUgcmFuZ2UuXG4gICAgICpcbiAgICAgKiBAc2luY2UgMy4xNi4wXG4gICAgKi9cbiAgICBFcnJvckNvZGVzLmpzb25ycGNSZXNlcnZlZEVycm9yUmFuZ2VTdGFydCA9IC0zMjA5OTtcbiAgICAvKiogQGRlcHJlY2F0ZWQgdXNlICBqc29ucnBjUmVzZXJ2ZWRFcnJvclJhbmdlU3RhcnQgKi9cbiAgICBFcnJvckNvZGVzLnNlcnZlckVycm9yU3RhcnQgPSAtMzIwOTk7XG4gICAgLyoqXG4gICAgICogQW4gZXJyb3Igb2NjdXJyZWQgd2hlbiB3cml0ZSBhIG1lc3NhZ2UgdG8gdGhlIHRyYW5zcG9ydCBsYXllci5cbiAgICAgKi9cbiAgICBFcnJvckNvZGVzLk1lc3NhZ2VXcml0ZUVycm9yID0gLTMyMDk5O1xuICAgIC8qKlxuICAgICAqIEFuIGVycm9yIG9jY3VycmVkIHdoZW4gcmVhZGluZyBhIG1lc3NhZ2UgZnJvbSB0aGUgdHJhbnNwb3J0IGxheWVyLlxuICAgICAqL1xuICAgIEVycm9yQ29kZXMuTWVzc2FnZVJlYWRFcnJvciA9IC0zMjA5ODtcbiAgICAvKipcbiAgICAgKiBUaGUgY29ubmVjdGlvbiBnb3QgZGlzcG9zZWQgb3IgbG9zdCBhbmQgYWxsIHBlbmRpbmcgcmVzcG9uc2VzIGdvdFxuICAgICAqIHJlamVjdGVkLlxuICAgICAqL1xuICAgIEVycm9yQ29kZXMuUGVuZGluZ1Jlc3BvbnNlUmVqZWN0ZWQgPSAtMzIwOTc7XG4gICAgLyoqXG4gICAgICogVGhlIGNvbm5lY3Rpb24gaXMgaW5hY3RpdmUgYW5kIGEgdXNlIG9mIGl0IGZhaWxlZC5cbiAgICAgKi9cbiAgICBFcnJvckNvZGVzLkNvbm5lY3Rpb25JbmFjdGl2ZSA9IC0zMjA5NjtcbiAgICAvKipcbiAgICAgKiBFcnJvciBjb2RlIGluZGljYXRpbmcgdGhhdCBhIHNlcnZlciByZWNlaXZlZCBhIG5vdGlmaWNhdGlvbiBvclxuICAgICAqIHJlcXVlc3QgYmVmb3JlIHRoZSBzZXJ2ZXIgaGFzIHJlY2VpdmVkIHRoZSBgaW5pdGlhbGl6ZWAgcmVxdWVzdC5cbiAgICAgKi9cbiAgICBFcnJvckNvZGVzLlNlcnZlck5vdEluaXRpYWxpemVkID0gLTMyMDAyO1xuICAgIEVycm9yQ29kZXMuVW5rbm93bkVycm9yQ29kZSA9IC0zMjAwMTtcbiAgICAvKipcbiAgICAgKiBUaGlzIGlzIHRoZSBlbmQgcmFuZ2Ugb2YgSlNPTiBSUEMgcmVzZXJ2ZWQgZXJyb3IgY29kZXMuXG4gICAgICogSXQgZG9lc24ndCBkZW5vdGUgYSByZWFsIGVycm9yIGNvZGUuXG4gICAgICpcbiAgICAgKiBAc2luY2UgMy4xNi4wXG4gICAgKi9cbiAgICBFcnJvckNvZGVzLmpzb25ycGNSZXNlcnZlZEVycm9yUmFuZ2VFbmQgPSAtMzIwMDA7XG4gICAgLyoqIEBkZXByZWNhdGVkIHVzZSAganNvbnJwY1Jlc2VydmVkRXJyb3JSYW5nZUVuZCAqL1xuICAgIEVycm9yQ29kZXMuc2VydmVyRXJyb3JFbmQgPSAtMzIwMDA7XG59KShFcnJvckNvZGVzIHx8IChleHBvcnRzLkVycm9yQ29kZXMgPSBFcnJvckNvZGVzID0ge30pKTtcbi8qKlxuICogQW4gZXJyb3Igb2JqZWN0IHJldHVybiBpbiBhIHJlc3BvbnNlIGluIGNhc2UgYSByZXF1ZXN0XG4gKiBoYXMgZmFpbGVkLlxuICovXG5jbGFzcyBSZXNwb25zZUVycm9yIGV4dGVuZHMgRXJyb3Ige1xuICAgIGNvbnN0cnVjdG9yKGNvZGUsIG1lc3NhZ2UsIGRhdGEpIHtcbiAgICAgICAgc3VwZXIobWVzc2FnZSk7XG4gICAgICAgIHRoaXMuY29kZSA9IGlzLm51bWJlcihjb2RlKSA/IGNvZGUgOiBFcnJvckNvZGVzLlVua25vd25FcnJvckNvZGU7XG4gICAgICAgIHRoaXMuZGF0YSA9IGRhdGE7XG4gICAgICAgIE9iamVjdC5zZXRQcm90b3R5cGVPZih0aGlzLCBSZXNwb25zZUVycm9yLnByb3RvdHlwZSk7XG4gICAgfVxuICAgIHRvSnNvbigpIHtcbiAgICAgICAgY29uc3QgcmVzdWx0ID0ge1xuICAgICAgICAgICAgY29kZTogdGhpcy5jb2RlLFxuICAgICAgICAgICAgbWVzc2FnZTogdGhpcy5tZXNzYWdlXG4gICAgICAgIH07XG4gICAgICAgIGlmICh0aGlzLmRhdGEgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmVzdWx0LmRhdGEgPSB0aGlzLmRhdGE7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICB9XG59XG5leHBvcnRzLlJlc3BvbnNlRXJyb3IgPSBSZXNwb25zZUVycm9yO1xuY2xhc3MgUGFyYW1ldGVyU3RydWN0dXJlcyB7XG4gICAgY29uc3RydWN0b3Ioa2luZCkge1xuICAgICAgICB0aGlzLmtpbmQgPSBraW5kO1xuICAgIH1cbiAgICBzdGF0aWMgaXModmFsdWUpIHtcbiAgICAgICAgcmV0dXJuIHZhbHVlID09PSBQYXJhbWV0ZXJTdHJ1Y3R1cmVzLmF1dG8gfHwgdmFsdWUgPT09IFBhcmFtZXRlclN0cnVjdHVyZXMuYnlOYW1lIHx8IHZhbHVlID09PSBQYXJhbWV0ZXJTdHJ1Y3R1cmVzLmJ5UG9zaXRpb247XG4gICAgfVxuICAgIHRvU3RyaW5nKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5raW5kO1xuICAgIH1cbn1cbmV4cG9ydHMuUGFyYW1ldGVyU3RydWN0dXJlcyA9IFBhcmFtZXRlclN0cnVjdHVyZXM7XG4vKipcbiAqIFRoZSBwYXJhbWV0ZXIgc3RydWN0dXJlIGlzIGF1dG9tYXRpY2FsbHkgaW5mZXJyZWQgb24gdGhlIG51bWJlciBvZiBwYXJhbWV0ZXJzXG4gKiBhbmQgdGhlIHBhcmFtZXRlciB0eXBlIGluIGNhc2Ugb2YgYSBzaW5nbGUgcGFyYW0uXG4gKi9cblBhcmFtZXRlclN0cnVjdHVyZXMuYXV0byA9IG5ldyBQYXJhbWV0ZXJTdHJ1Y3R1cmVzKCdhdXRvJyk7XG4vKipcbiAqIEZvcmNlcyBgYnlQb3NpdGlvbmAgcGFyYW1ldGVyIHN0cnVjdHVyZS4gVGhpcyBpcyB1c2VmdWwgaWYgeW91IGhhdmUgYSBzaW5nbGVcbiAqIHBhcmFtZXRlciB3aGljaCBoYXMgYSBsaXRlcmFsIHR5cGUuXG4gKi9cblBhcmFtZXRlclN0cnVjdHVyZXMuYnlQb3NpdGlvbiA9IG5ldyBQYXJhbWV0ZXJTdHJ1Y3R1cmVzKCdieVBvc2l0aW9uJyk7XG4vKipcbiAqIEZvcmNlcyBgYnlOYW1lYCBwYXJhbWV0ZXIgc3RydWN0dXJlLiBUaGlzIGlzIG9ubHkgdXNlZnVsIHdoZW4gaGF2aW5nIGEgc2luZ2xlXG4gKiBwYXJhbWV0ZXIuIFRoZSBsaWJyYXJ5IHdpbGwgcmVwb3J0IGVycm9ycyBpZiB1c2VkIHdpdGggYSBkaWZmZXJlbnQgbnVtYmVyIG9mXG4gKiBwYXJhbWV0ZXJzLlxuICovXG5QYXJhbWV0ZXJTdHJ1Y3R1cmVzLmJ5TmFtZSA9IG5ldyBQYXJhbWV0ZXJTdHJ1Y3R1cmVzKCdieU5hbWUnKTtcbi8qKlxuICogQW4gYWJzdHJhY3QgaW1wbGVtZW50YXRpb24gb2YgYSBNZXNzYWdlVHlwZS5cbiAqL1xuY2xhc3MgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QsIG51bWJlck9mUGFyYW1zKSB7XG4gICAgICAgIHRoaXMubWV0aG9kID0gbWV0aG9kO1xuICAgICAgICB0aGlzLm51bWJlck9mUGFyYW1zID0gbnVtYmVyT2ZQYXJhbXM7XG4gICAgfVxuICAgIGdldCBwYXJhbWV0ZXJTdHJ1Y3R1cmVzKCkge1xuICAgICAgICByZXR1cm4gUGFyYW1ldGVyU3RydWN0dXJlcy5hdXRvO1xuICAgIH1cbn1cbmV4cG9ydHMuQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlID0gQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlO1xuLyoqXG4gKiBDbGFzc2VzIHRvIHR5cGUgcmVxdWVzdCByZXNwb25zZSBwYWlyc1xuICovXG5jbGFzcyBSZXF1ZXN0VHlwZTAgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCkge1xuICAgICAgICBzdXBlcihtZXRob2QsIDApO1xuICAgIH1cbn1cbmV4cG9ydHMuUmVxdWVzdFR5cGUwID0gUmVxdWVzdFR5cGUwO1xuY2xhc3MgUmVxdWVzdFR5cGUgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCwgX3BhcmFtZXRlclN0cnVjdHVyZXMgPSBQYXJhbWV0ZXJTdHJ1Y3R1cmVzLmF1dG8pIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCAxKTtcbiAgICAgICAgdGhpcy5fcGFyYW1ldGVyU3RydWN0dXJlcyA9IF9wYXJhbWV0ZXJTdHJ1Y3R1cmVzO1xuICAgIH1cbiAgICBnZXQgcGFyYW1ldGVyU3RydWN0dXJlcygpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX3BhcmFtZXRlclN0cnVjdHVyZXM7XG4gICAgfVxufVxuZXhwb3J0cy5SZXF1ZXN0VHlwZSA9IFJlcXVlc3RUeXBlO1xuY2xhc3MgUmVxdWVzdFR5cGUxIGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QsIF9wYXJhbWV0ZXJTdHJ1Y3R1cmVzID0gUGFyYW1ldGVyU3RydWN0dXJlcy5hdXRvKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgMSk7XG4gICAgICAgIHRoaXMuX3BhcmFtZXRlclN0cnVjdHVyZXMgPSBfcGFyYW1ldGVyU3RydWN0dXJlcztcbiAgICB9XG4gICAgZ2V0IHBhcmFtZXRlclN0cnVjdHVyZXMoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLl9wYXJhbWV0ZXJTdHJ1Y3R1cmVzO1xuICAgIH1cbn1cbmV4cG9ydHMuUmVxdWVzdFR5cGUxID0gUmVxdWVzdFR5cGUxO1xuY2xhc3MgUmVxdWVzdFR5cGUyIGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QpIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCAyKTtcbiAgICB9XG59XG5leHBvcnRzLlJlcXVlc3RUeXBlMiA9IFJlcXVlc3RUeXBlMjtcbmNsYXNzIFJlcXVlc3RUeXBlMyBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVNpZ25hdHVyZSB7XG4gICAgY29uc3RydWN0b3IobWV0aG9kKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgMyk7XG4gICAgfVxufVxuZXhwb3J0cy5SZXF1ZXN0VHlwZTMgPSBSZXF1ZXN0VHlwZTM7XG5jbGFzcyBSZXF1ZXN0VHlwZTQgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCkge1xuICAgICAgICBzdXBlcihtZXRob2QsIDQpO1xuICAgIH1cbn1cbmV4cG9ydHMuUmVxdWVzdFR5cGU0ID0gUmVxdWVzdFR5cGU0O1xuY2xhc3MgUmVxdWVzdFR5cGU1IGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QpIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCA1KTtcbiAgICB9XG59XG5leHBvcnRzLlJlcXVlc3RUeXBlNSA9IFJlcXVlc3RUeXBlNTtcbmNsYXNzIFJlcXVlc3RUeXBlNiBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVNpZ25hdHVyZSB7XG4gICAgY29uc3RydWN0b3IobWV0aG9kKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgNik7XG4gICAgfVxufVxuZXhwb3J0cy5SZXF1ZXN0VHlwZTYgPSBSZXF1ZXN0VHlwZTY7XG5jbGFzcyBSZXF1ZXN0VHlwZTcgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCkge1xuICAgICAgICBzdXBlcihtZXRob2QsIDcpO1xuICAgIH1cbn1cbmV4cG9ydHMuUmVxdWVzdFR5cGU3ID0gUmVxdWVzdFR5cGU3O1xuY2xhc3MgUmVxdWVzdFR5cGU4IGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QpIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCA4KTtcbiAgICB9XG59XG5leHBvcnRzLlJlcXVlc3RUeXBlOCA9IFJlcXVlc3RUeXBlODtcbmNsYXNzIFJlcXVlc3RUeXBlOSBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVNpZ25hdHVyZSB7XG4gICAgY29uc3RydWN0b3IobWV0aG9kKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgOSk7XG4gICAgfVxufVxuZXhwb3J0cy5SZXF1ZXN0VHlwZTkgPSBSZXF1ZXN0VHlwZTk7XG5jbGFzcyBOb3RpZmljYXRpb25UeXBlIGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QsIF9wYXJhbWV0ZXJTdHJ1Y3R1cmVzID0gUGFyYW1ldGVyU3RydWN0dXJlcy5hdXRvKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgMSk7XG4gICAgICAgIHRoaXMuX3BhcmFtZXRlclN0cnVjdHVyZXMgPSBfcGFyYW1ldGVyU3RydWN0dXJlcztcbiAgICB9XG4gICAgZ2V0IHBhcmFtZXRlclN0cnVjdHVyZXMoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLl9wYXJhbWV0ZXJTdHJ1Y3R1cmVzO1xuICAgIH1cbn1cbmV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZSA9IE5vdGlmaWNhdGlvblR5cGU7XG5jbGFzcyBOb3RpZmljYXRpb25UeXBlMCBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVNpZ25hdHVyZSB7XG4gICAgY29uc3RydWN0b3IobWV0aG9kKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgMCk7XG4gICAgfVxufVxuZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlMCA9IE5vdGlmaWNhdGlvblR5cGUwO1xuY2xhc3MgTm90aWZpY2F0aW9uVHlwZTEgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCwgX3BhcmFtZXRlclN0cnVjdHVyZXMgPSBQYXJhbWV0ZXJTdHJ1Y3R1cmVzLmF1dG8pIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCAxKTtcbiAgICAgICAgdGhpcy5fcGFyYW1ldGVyU3RydWN0dXJlcyA9IF9wYXJhbWV0ZXJTdHJ1Y3R1cmVzO1xuICAgIH1cbiAgICBnZXQgcGFyYW1ldGVyU3RydWN0dXJlcygpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX3BhcmFtZXRlclN0cnVjdHVyZXM7XG4gICAgfVxufVxuZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlMSA9IE5vdGlmaWNhdGlvblR5cGUxO1xuY2xhc3MgTm90aWZpY2F0aW9uVHlwZTIgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCkge1xuICAgICAgICBzdXBlcihtZXRob2QsIDIpO1xuICAgIH1cbn1cbmV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTIgPSBOb3RpZmljYXRpb25UeXBlMjtcbmNsYXNzIE5vdGlmaWNhdGlvblR5cGUzIGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QpIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCAzKTtcbiAgICB9XG59XG5leHBvcnRzLk5vdGlmaWNhdGlvblR5cGUzID0gTm90aWZpY2F0aW9uVHlwZTM7XG5jbGFzcyBOb3RpZmljYXRpb25UeXBlNCBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVNpZ25hdHVyZSB7XG4gICAgY29uc3RydWN0b3IobWV0aG9kKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgNCk7XG4gICAgfVxufVxuZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlNCA9IE5vdGlmaWNhdGlvblR5cGU0O1xuY2xhc3MgTm90aWZpY2F0aW9uVHlwZTUgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCkge1xuICAgICAgICBzdXBlcihtZXRob2QsIDUpO1xuICAgIH1cbn1cbmV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTUgPSBOb3RpZmljYXRpb25UeXBlNTtcbmNsYXNzIE5vdGlmaWNhdGlvblR5cGU2IGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QpIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCA2KTtcbiAgICB9XG59XG5leHBvcnRzLk5vdGlmaWNhdGlvblR5cGU2ID0gTm90aWZpY2F0aW9uVHlwZTY7XG5jbGFzcyBOb3RpZmljYXRpb25UeXBlNyBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVNpZ25hdHVyZSB7XG4gICAgY29uc3RydWN0b3IobWV0aG9kKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgNyk7XG4gICAgfVxufVxuZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlNyA9IE5vdGlmaWNhdGlvblR5cGU3O1xuY2xhc3MgTm90aWZpY2F0aW9uVHlwZTggZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCkge1xuICAgICAgICBzdXBlcihtZXRob2QsIDgpO1xuICAgIH1cbn1cbmV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTggPSBOb3RpZmljYXRpb25UeXBlODtcbmNsYXNzIE5vdGlmaWNhdGlvblR5cGU5IGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QpIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCA5KTtcbiAgICB9XG59XG5leHBvcnRzLk5vdGlmaWNhdGlvblR5cGU5ID0gTm90aWZpY2F0aW9uVHlwZTk7XG52YXIgTWVzc2FnZTtcbihmdW5jdGlvbiAoTWVzc2FnZSkge1xuICAgIC8qKlxuICAgICAqIFRlc3RzIGlmIHRoZSBnaXZlbiBtZXNzYWdlIGlzIGEgcmVxdWVzdCBtZXNzYWdlXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXNSZXF1ZXN0KG1lc3NhZ2UpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gbWVzc2FnZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiBpcy5zdHJpbmcoY2FuZGlkYXRlLm1ldGhvZCkgJiYgKGlzLnN0cmluZyhjYW5kaWRhdGUuaWQpIHx8IGlzLm51bWJlcihjYW5kaWRhdGUuaWQpKTtcbiAgICB9XG4gICAgTWVzc2FnZS5pc1JlcXVlc3QgPSBpc1JlcXVlc3Q7XG4gICAgLyoqXG4gICAgICogVGVzdHMgaWYgdGhlIGdpdmVuIG1lc3NhZ2UgaXMgYSBub3RpZmljYXRpb24gbWVzc2FnZVxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzTm90aWZpY2F0aW9uKG1lc3NhZ2UpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gbWVzc2FnZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiBpcy5zdHJpbmcoY2FuZGlkYXRlLm1ldGhvZCkgJiYgbWVzc2FnZS5pZCA9PT0gdm9pZCAwO1xuICAgIH1cbiAgICBNZXNzYWdlLmlzTm90aWZpY2F0aW9uID0gaXNOb3RpZmljYXRpb247XG4gICAgLyoqXG4gICAgICogVGVzdHMgaWYgdGhlIGdpdmVuIG1lc3NhZ2UgaXMgYSByZXNwb25zZSBtZXNzYWdlXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXNSZXNwb25zZShtZXNzYWdlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IG1lc3NhZ2U7XG4gICAgICAgIHJldHVybiBjYW5kaWRhdGUgJiYgKGNhbmRpZGF0ZS5yZXN1bHQgIT09IHZvaWQgMCB8fCAhIWNhbmRpZGF0ZS5lcnJvcikgJiYgKGlzLnN0cmluZyhjYW5kaWRhdGUuaWQpIHx8IGlzLm51bWJlcihjYW5kaWRhdGUuaWQpIHx8IGNhbmRpZGF0ZS5pZCA9PT0gbnVsbCk7XG4gICAgfVxuICAgIE1lc3NhZ2UuaXNSZXNwb25zZSA9IGlzUmVzcG9uc2U7XG59KShNZXNzYWdlIHx8IChleHBvcnRzLk1lc3NhZ2UgPSBNZXNzYWdlID0ge30pKTtcbiIsICJcInVzZSBzdHJpY3RcIjtcbi8qLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiAgQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiAgTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTGljZW5zZS50eHQgaW4gdGhlIHByb2plY3Qgcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0qL1xudmFyIF9hO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiX19lc01vZHVsZVwiLCB7IHZhbHVlOiB0cnVlIH0pO1xuZXhwb3J0cy5MUlVDYWNoZSA9IGV4cG9ydHMuTGlua2VkTWFwID0gZXhwb3J0cy5Ub3VjaCA9IHZvaWQgMDtcbnZhciBUb3VjaDtcbihmdW5jdGlvbiAoVG91Y2gpIHtcbiAgICBUb3VjaC5Ob25lID0gMDtcbiAgICBUb3VjaC5GaXJzdCA9IDE7XG4gICAgVG91Y2guQXNPbGQgPSBUb3VjaC5GaXJzdDtcbiAgICBUb3VjaC5MYXN0ID0gMjtcbiAgICBUb3VjaC5Bc05ldyA9IFRvdWNoLkxhc3Q7XG59KShUb3VjaCB8fCAoZXhwb3J0cy5Ub3VjaCA9IFRvdWNoID0ge30pKTtcbmNsYXNzIExpbmtlZE1hcCB7XG4gICAgY29uc3RydWN0b3IoKSB7XG4gICAgICAgIHRoaXNbX2FdID0gJ0xpbmtlZE1hcCc7XG4gICAgICAgIHRoaXMuX21hcCA9IG5ldyBNYXAoKTtcbiAgICAgICAgdGhpcy5faGVhZCA9IHVuZGVmaW5lZDtcbiAgICAgICAgdGhpcy5fdGFpbCA9IHVuZGVmaW5lZDtcbiAgICAgICAgdGhpcy5fc2l6ZSA9IDA7XG4gICAgICAgIHRoaXMuX3N0YXRlID0gMDtcbiAgICB9XG4gICAgY2xlYXIoKSB7XG4gICAgICAgIHRoaXMuX21hcC5jbGVhcigpO1xuICAgICAgICB0aGlzLl9oZWFkID0gdW5kZWZpbmVkO1xuICAgICAgICB0aGlzLl90YWlsID0gdW5kZWZpbmVkO1xuICAgICAgICB0aGlzLl9zaXplID0gMDtcbiAgICAgICAgdGhpcy5fc3RhdGUrKztcbiAgICB9XG4gICAgaXNFbXB0eSgpIHtcbiAgICAgICAgcmV0dXJuICF0aGlzLl9oZWFkICYmICF0aGlzLl90YWlsO1xuICAgIH1cbiAgICBnZXQgc2l6ZSgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX3NpemU7XG4gICAgfVxuICAgIGdldCBmaXJzdCgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX2hlYWQ/LnZhbHVlO1xuICAgIH1cbiAgICBnZXQgbGFzdCgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX3RhaWw/LnZhbHVlO1xuICAgIH1cbiAgICBoYXMoa2V5KSB7XG4gICAgICAgIHJldHVybiB0aGlzLl9tYXAuaGFzKGtleSk7XG4gICAgfVxuICAgIGdldChrZXksIHRvdWNoID0gVG91Y2guTm9uZSkge1xuICAgICAgICBjb25zdCBpdGVtID0gdGhpcy5fbWFwLmdldChrZXkpO1xuICAgICAgICBpZiAoIWl0ZW0pIHtcbiAgICAgICAgICAgIHJldHVybiB1bmRlZmluZWQ7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKHRvdWNoICE9PSBUb3VjaC5Ob25lKSB7XG4gICAgICAgICAgICB0aGlzLnRvdWNoKGl0ZW0sIHRvdWNoKTtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gaXRlbS52YWx1ZTtcbiAgICB9XG4gICAgc2V0KGtleSwgdmFsdWUsIHRvdWNoID0gVG91Y2guTm9uZSkge1xuICAgICAgICBsZXQgaXRlbSA9IHRoaXMuX21hcC5nZXQoa2V5KTtcbiAgICAgICAgaWYgKGl0ZW0pIHtcbiAgICAgICAgICAgIGl0ZW0udmFsdWUgPSB2YWx1ZTtcbiAgICAgICAgICAgIGlmICh0b3VjaCAhPT0gVG91Y2guTm9uZSkge1xuICAgICAgICAgICAgICAgIHRoaXMudG91Y2goaXRlbSwgdG91Y2gpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgaXRlbSA9IHsga2V5LCB2YWx1ZSwgbmV4dDogdW5kZWZpbmVkLCBwcmV2aW91czogdW5kZWZpbmVkIH07XG4gICAgICAgICAgICBzd2l0Y2ggKHRvdWNoKSB7XG4gICAgICAgICAgICAgICAgY2FzZSBUb3VjaC5Ob25lOlxuICAgICAgICAgICAgICAgICAgICB0aGlzLmFkZEl0ZW1MYXN0KGl0ZW0pO1xuICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICBjYXNlIFRvdWNoLkZpcnN0OlxuICAgICAgICAgICAgICAgICAgICB0aGlzLmFkZEl0ZW1GaXJzdChpdGVtKTtcbiAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgY2FzZSBUb3VjaC5MYXN0OlxuICAgICAgICAgICAgICAgICAgICB0aGlzLmFkZEl0ZW1MYXN0KGl0ZW0pO1xuICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICBkZWZhdWx0OlxuICAgICAgICAgICAgICAgICAgICB0aGlzLmFkZEl0ZW1MYXN0KGl0ZW0pO1xuICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHRoaXMuX21hcC5zZXQoa2V5LCBpdGVtKTtcbiAgICAgICAgICAgIHRoaXMuX3NpemUrKztcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gdGhpcztcbiAgICB9XG4gICAgZGVsZXRlKGtleSkge1xuICAgICAgICByZXR1cm4gISF0aGlzLnJlbW92ZShrZXkpO1xuICAgIH1cbiAgICByZW1vdmUoa2V5KSB7XG4gICAgICAgIGNvbnN0IGl0ZW0gPSB0aGlzLl9tYXAuZ2V0KGtleSk7XG4gICAgICAgIGlmICghaXRlbSkge1xuICAgICAgICAgICAgcmV0dXJuIHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLl9tYXAuZGVsZXRlKGtleSk7XG4gICAgICAgIHRoaXMucmVtb3ZlSXRlbShpdGVtKTtcbiAgICAgICAgdGhpcy5fc2l6ZS0tO1xuICAgICAgICByZXR1cm4gaXRlbS52YWx1ZTtcbiAgICB9XG4gICAgc2hpZnQoKSB7XG4gICAgICAgIGlmICghdGhpcy5faGVhZCAmJiAhdGhpcy5fdGFpbCkge1xuICAgICAgICAgICAgcmV0dXJuIHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgICAgICBpZiAoIXRoaXMuX2hlYWQgfHwgIXRoaXMuX3RhaWwpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcignSW52YWxpZCBsaXN0Jyk7XG4gICAgICAgIH1cbiAgICAgICAgY29uc3QgaXRlbSA9IHRoaXMuX2hlYWQ7XG4gICAgICAgIHRoaXMuX21hcC5kZWxldGUoaXRlbS5rZXkpO1xuICAgICAgICB0aGlzLnJlbW92ZUl0ZW0oaXRlbSk7XG4gICAgICAgIHRoaXMuX3NpemUtLTtcbiAgICAgICAgcmV0dXJuIGl0ZW0udmFsdWU7XG4gICAgfVxuICAgIGZvckVhY2goY2FsbGJhY2tmbiwgdGhpc0FyZykge1xuICAgICAgICBjb25zdCBzdGF0ZSA9IHRoaXMuX3N0YXRlO1xuICAgICAgICBsZXQgY3VycmVudCA9IHRoaXMuX2hlYWQ7XG4gICAgICAgIHdoaWxlIChjdXJyZW50KSB7XG4gICAgICAgICAgICBpZiAodGhpc0FyZykge1xuICAgICAgICAgICAgICAgIGNhbGxiYWNrZm4uYmluZCh0aGlzQXJnKShjdXJyZW50LnZhbHVlLCBjdXJyZW50LmtleSwgdGhpcyk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICBjYWxsYmFja2ZuKGN1cnJlbnQudmFsdWUsIGN1cnJlbnQua2V5LCB0aGlzKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmICh0aGlzLl9zdGF0ZSAhPT0gc3RhdGUpIHtcbiAgICAgICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYExpbmtlZE1hcCBnb3QgbW9kaWZpZWQgZHVyaW5nIGl0ZXJhdGlvbi5gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGN1cnJlbnQgPSBjdXJyZW50Lm5leHQ7XG4gICAgICAgIH1cbiAgICB9XG4gICAga2V5cygpIHtcbiAgICAgICAgY29uc3Qgc3RhdGUgPSB0aGlzLl9zdGF0ZTtcbiAgICAgICAgbGV0IGN1cnJlbnQgPSB0aGlzLl9oZWFkO1xuICAgICAgICBjb25zdCBpdGVyYXRvciA9IHtcbiAgICAgICAgICAgIFtTeW1ib2wuaXRlcmF0b3JdOiAoKSA9PiB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIGl0ZXJhdG9yO1xuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIG5leHQ6ICgpID0+IHtcbiAgICAgICAgICAgICAgICBpZiAodGhpcy5fc3RhdGUgIT09IHN0YXRlKSB7XG4gICAgICAgICAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgTGlua2VkTWFwIGdvdCBtb2RpZmllZCBkdXJpbmcgaXRlcmF0aW9uLmApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBpZiAoY3VycmVudCkge1xuICAgICAgICAgICAgICAgICAgICBjb25zdCByZXN1bHQgPSB7IHZhbHVlOiBjdXJyZW50LmtleSwgZG9uZTogZmFsc2UgfTtcbiAgICAgICAgICAgICAgICAgICAgY3VycmVudCA9IGN1cnJlbnQubmV4dDtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiB7IHZhbHVlOiB1bmRlZmluZWQsIGRvbmU6IHRydWUgfTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH07XG4gICAgICAgIHJldHVybiBpdGVyYXRvcjtcbiAgICB9XG4gICAgdmFsdWVzKCkge1xuICAgICAgICBjb25zdCBzdGF0ZSA9IHRoaXMuX3N0YXRlO1xuICAgICAgICBsZXQgY3VycmVudCA9IHRoaXMuX2hlYWQ7XG4gICAgICAgIGNvbnN0IGl0ZXJhdG9yID0ge1xuICAgICAgICAgICAgW1N5bWJvbC5pdGVyYXRvcl06ICgpID0+IHtcbiAgICAgICAgICAgICAgICByZXR1cm4gaXRlcmF0b3I7XG4gICAgICAgICAgICB9LFxuICAgICAgICAgICAgbmV4dDogKCkgPT4ge1xuICAgICAgICAgICAgICAgIGlmICh0aGlzLl9zdGF0ZSAhPT0gc3RhdGUpIHtcbiAgICAgICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBMaW5rZWRNYXAgZ290IG1vZGlmaWVkIGR1cmluZyBpdGVyYXRpb24uYCk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGlmIChjdXJyZW50KSB7XG4gICAgICAgICAgICAgICAgICAgIGNvbnN0IHJlc3VsdCA9IHsgdmFsdWU6IGN1cnJlbnQudmFsdWUsIGRvbmU6IGZhbHNlIH07XG4gICAgICAgICAgICAgICAgICAgIGN1cnJlbnQgPSBjdXJyZW50Lm5leHQ7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4geyB2YWx1ZTogdW5kZWZpbmVkLCBkb25lOiB0cnVlIH07XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICB9O1xuICAgICAgICByZXR1cm4gaXRlcmF0b3I7XG4gICAgfVxuICAgIGVudHJpZXMoKSB7XG4gICAgICAgIGNvbnN0IHN0YXRlID0gdGhpcy5fc3RhdGU7XG4gICAgICAgIGxldCBjdXJyZW50ID0gdGhpcy5faGVhZDtcbiAgICAgICAgY29uc3QgaXRlcmF0b3IgPSB7XG4gICAgICAgICAgICBbU3ltYm9sLml0ZXJhdG9yXTogKCkgPT4ge1xuICAgICAgICAgICAgICAgIHJldHVybiBpdGVyYXRvcjtcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICBuZXh0OiAoKSA9PiB7XG4gICAgICAgICAgICAgICAgaWYgKHRoaXMuX3N0YXRlICE9PSBzdGF0ZSkge1xuICAgICAgICAgICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYExpbmtlZE1hcCBnb3QgbW9kaWZpZWQgZHVyaW5nIGl0ZXJhdGlvbi5gKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgaWYgKGN1cnJlbnQpIHtcbiAgICAgICAgICAgICAgICAgICAgY29uc3QgcmVzdWx0ID0geyB2YWx1ZTogW2N1cnJlbnQua2V5LCBjdXJyZW50LnZhbHVlXSwgZG9uZTogZmFsc2UgfTtcbiAgICAgICAgICAgICAgICAgICAgY3VycmVudCA9IGN1cnJlbnQubmV4dDtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiB7IHZhbHVlOiB1bmRlZmluZWQsIGRvbmU6IHRydWUgfTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH07XG4gICAgICAgIHJldHVybiBpdGVyYXRvcjtcbiAgICB9XG4gICAgWyhfYSA9IFN5bWJvbC50b1N0cmluZ1RhZywgU3ltYm9sLml0ZXJhdG9yKV0oKSB7XG4gICAgICAgIHJldHVybiB0aGlzLmVudHJpZXMoKTtcbiAgICB9XG4gICAgdHJpbU9sZChuZXdTaXplKSB7XG4gICAgICAgIGlmIChuZXdTaXplID49IHRoaXMuc2l6ZSkge1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGlmIChuZXdTaXplID09PSAwKSB7XG4gICAgICAgICAgICB0aGlzLmNsZWFyKCk7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgICAgbGV0IGN1cnJlbnQgPSB0aGlzLl9oZWFkO1xuICAgICAgICBsZXQgY3VycmVudFNpemUgPSB0aGlzLnNpemU7XG4gICAgICAgIHdoaWxlIChjdXJyZW50ICYmIGN1cnJlbnRTaXplID4gbmV3U2l6ZSkge1xuICAgICAgICAgICAgdGhpcy5fbWFwLmRlbGV0ZShjdXJyZW50LmtleSk7XG4gICAgICAgICAgICBjdXJyZW50ID0gY3VycmVudC5uZXh0O1xuICAgICAgICAgICAgY3VycmVudFNpemUtLTtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLl9oZWFkID0gY3VycmVudDtcbiAgICAgICAgdGhpcy5fc2l6ZSA9IGN1cnJlbnRTaXplO1xuICAgICAgICBpZiAoY3VycmVudCkge1xuICAgICAgICAgICAgY3VycmVudC5wcmV2aW91cyA9IHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLl9zdGF0ZSsrO1xuICAgIH1cbiAgICBhZGRJdGVtRmlyc3QoaXRlbSkge1xuICAgICAgICAvLyBGaXJzdCB0aW1lIEluc2VydFxuICAgICAgICBpZiAoIXRoaXMuX2hlYWQgJiYgIXRoaXMuX3RhaWwpIHtcbiAgICAgICAgICAgIHRoaXMuX3RhaWwgPSBpdGVtO1xuICAgICAgICB9XG4gICAgICAgIGVsc2UgaWYgKCF0aGlzLl9oZWFkKSB7XG4gICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoJ0ludmFsaWQgbGlzdCcpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgaXRlbS5uZXh0ID0gdGhpcy5faGVhZDtcbiAgICAgICAgICAgIHRoaXMuX2hlYWQucHJldmlvdXMgPSBpdGVtO1xuICAgICAgICB9XG4gICAgICAgIHRoaXMuX2hlYWQgPSBpdGVtO1xuICAgICAgICB0aGlzLl9zdGF0ZSsrO1xuICAgIH1cbiAgICBhZGRJdGVtTGFzdChpdGVtKSB7XG4gICAgICAgIC8vIEZpcnN0IHRpbWUgSW5zZXJ0XG4gICAgICAgIGlmICghdGhpcy5faGVhZCAmJiAhdGhpcy5fdGFpbCkge1xuICAgICAgICAgICAgdGhpcy5faGVhZCA9IGl0ZW07XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSBpZiAoIXRoaXMuX3RhaWwpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcignSW52YWxpZCBsaXN0Jyk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBpdGVtLnByZXZpb3VzID0gdGhpcy5fdGFpbDtcbiAgICAgICAgICAgIHRoaXMuX3RhaWwubmV4dCA9IGl0ZW07XG4gICAgICAgIH1cbiAgICAgICAgdGhpcy5fdGFpbCA9IGl0ZW07XG4gICAgICAgIHRoaXMuX3N0YXRlKys7XG4gICAgfVxuICAgIHJlbW92ZUl0ZW0oaXRlbSkge1xuICAgICAgICBpZiAoaXRlbSA9PT0gdGhpcy5faGVhZCAmJiBpdGVtID09PSB0aGlzLl90YWlsKSB7XG4gICAgICAgICAgICB0aGlzLl9oZWFkID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgdGhpcy5fdGFpbCA9IHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIGlmIChpdGVtID09PSB0aGlzLl9oZWFkKSB7XG4gICAgICAgICAgICAvLyBUaGlzIGNhbiBvbmx5IGhhcHBlbmVkIGlmIHNpemUgPT09IDEgd2hpY2ggaXMgaGFuZGxlXG4gICAgICAgICAgICAvLyBieSB0aGUgY2FzZSBhYm92ZS5cbiAgICAgICAgICAgIGlmICghaXRlbS5uZXh0KSB7XG4gICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdJbnZhbGlkIGxpc3QnKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGl0ZW0ubmV4dC5wcmV2aW91cyA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIHRoaXMuX2hlYWQgPSBpdGVtLm5leHQ7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSBpZiAoaXRlbSA9PT0gdGhpcy5fdGFpbCkge1xuICAgICAgICAgICAgLy8gVGhpcyBjYW4gb25seSBoYXBwZW5lZCBpZiBzaXplID09PSAxIHdoaWNoIGlzIGhhbmRsZVxuICAgICAgICAgICAgLy8gYnkgdGhlIGNhc2UgYWJvdmUuXG4gICAgICAgICAgICBpZiAoIWl0ZW0ucHJldmlvdXMpIHtcbiAgICAgICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoJ0ludmFsaWQgbGlzdCcpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaXRlbS5wcmV2aW91cy5uZXh0ID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgdGhpcy5fdGFpbCA9IGl0ZW0ucHJldmlvdXM7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBjb25zdCBuZXh0ID0gaXRlbS5uZXh0O1xuICAgICAgICAgICAgY29uc3QgcHJldmlvdXMgPSBpdGVtLnByZXZpb3VzO1xuICAgICAgICAgICAgaWYgKCFuZXh0IHx8ICFwcmV2aW91cykge1xuICAgICAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcignSW52YWxpZCBsaXN0Jyk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBuZXh0LnByZXZpb3VzID0gcHJldmlvdXM7XG4gICAgICAgICAgICBwcmV2aW91cy5uZXh0ID0gbmV4dDtcbiAgICAgICAgfVxuICAgICAgICBpdGVtLm5leHQgPSB1bmRlZmluZWQ7XG4gICAgICAgIGl0ZW0ucHJldmlvdXMgPSB1bmRlZmluZWQ7XG4gICAgICAgIHRoaXMuX3N0YXRlKys7XG4gICAgfVxuICAgIHRvdWNoKGl0ZW0sIHRvdWNoKSB7XG4gICAgICAgIGlmICghdGhpcy5faGVhZCB8fCAhdGhpcy5fdGFpbCkge1xuICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdJbnZhbGlkIGxpc3QnKTtcbiAgICAgICAgfVxuICAgICAgICBpZiAoKHRvdWNoICE9PSBUb3VjaC5GaXJzdCAmJiB0b3VjaCAhPT0gVG91Y2guTGFzdCkpIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBpZiAodG91Y2ggPT09IFRvdWNoLkZpcnN0KSB7XG4gICAgICAgICAgICBpZiAoaXRlbSA9PT0gdGhpcy5faGVhZCkge1xuICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNvbnN0IG5leHQgPSBpdGVtLm5leHQ7XG4gICAgICAgICAgICBjb25zdCBwcmV2aW91cyA9IGl0ZW0ucHJldmlvdXM7XG4gICAgICAgICAgICAvLyBVbmxpbmsgdGhlIGl0ZW1cbiAgICAgICAgICAgIGlmIChpdGVtID09PSB0aGlzLl90YWlsKSB7XG4gICAgICAgICAgICAgICAgLy8gcHJldmlvdXMgbXVzdCBiZSBkZWZpbmVkIHNpbmNlIGl0ZW0gd2FzIG5vdCBoZWFkIGJ1dCBpcyB0YWlsXG4gICAgICAgICAgICAgICAgLy8gU28gdGhlcmUgYXJlIG1vcmUgdGhhbiBvbiBpdGVtIGluIHRoZSBtYXBcbiAgICAgICAgICAgICAgICBwcmV2aW91cy5uZXh0ID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgICAgIHRoaXMuX3RhaWwgPSBwcmV2aW91cztcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIC8vIEJvdGggbmV4dCBhbmQgcHJldmlvdXMgYXJlIG5vdCB1bmRlZmluZWQgc2luY2UgaXRlbSB3YXMgbmVpdGhlciBoZWFkIG5vciB0YWlsLlxuICAgICAgICAgICAgICAgIG5leHQucHJldmlvdXMgPSBwcmV2aW91cztcbiAgICAgICAgICAgICAgICBwcmV2aW91cy5uZXh0ID0gbmV4dDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIC8vIEluc2VydCB0aGUgbm9kZSBhdCBoZWFkXG4gICAgICAgICAgICBpdGVtLnByZXZpb3VzID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgaXRlbS5uZXh0ID0gdGhpcy5faGVhZDtcbiAgICAgICAgICAgIHRoaXMuX2hlYWQucHJldmlvdXMgPSBpdGVtO1xuICAgICAgICAgICAgdGhpcy5faGVhZCA9IGl0ZW07XG4gICAgICAgICAgICB0aGlzLl9zdGF0ZSsrO1xuICAgICAgICB9XG4gICAgICAgIGVsc2UgaWYgKHRvdWNoID09PSBUb3VjaC5MYXN0KSB7XG4gICAgICAgICAgICBpZiAoaXRlbSA9PT0gdGhpcy5fdGFpbCkge1xuICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNvbnN0IG5leHQgPSBpdGVtLm5leHQ7XG4gICAgICAgICAgICBjb25zdCBwcmV2aW91cyA9IGl0ZW0ucHJldmlvdXM7XG4gICAgICAgICAgICAvLyBVbmxpbmsgdGhlIGl0ZW0uXG4gICAgICAgICAgICBpZiAoaXRlbSA9PT0gdGhpcy5faGVhZCkge1xuICAgICAgICAgICAgICAgIC8vIG5leHQgbXVzdCBiZSBkZWZpbmVkIHNpbmNlIGl0ZW0gd2FzIG5vdCB0YWlsIGJ1dCBpcyBoZWFkXG4gICAgICAgICAgICAgICAgLy8gU28gdGhlcmUgYXJlIG1vcmUgdGhhbiBvbiBpdGVtIGluIHRoZSBtYXBcbiAgICAgICAgICAgICAgICBuZXh0LnByZXZpb3VzID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgICAgIHRoaXMuX2hlYWQgPSBuZXh0O1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgLy8gQm90aCBuZXh0IGFuZCBwcmV2aW91cyBhcmUgbm90IHVuZGVmaW5lZCBzaW5jZSBpdGVtIHdhcyBuZWl0aGVyIGhlYWQgbm9yIHRhaWwuXG4gICAgICAgICAgICAgICAgbmV4dC5wcmV2aW91cyA9IHByZXZpb3VzO1xuICAgICAgICAgICAgICAgIHByZXZpb3VzLm5leHQgPSBuZXh0O1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaXRlbS5uZXh0ID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgaXRlbS5wcmV2aW91cyA9IHRoaXMuX3RhaWw7XG4gICAgICAgICAgICB0aGlzLl90YWlsLm5leHQgPSBpdGVtO1xuICAgICAgICAgICAgdGhpcy5fdGFpbCA9IGl0ZW07XG4gICAgICAgICAgICB0aGlzLl9zdGF0ZSsrO1xuICAgICAgICB9XG4gICAgfVxuICAgIHRvSlNPTigpIHtcbiAgICAgICAgY29uc3QgZGF0YSA9IFtdO1xuICAgICAgICB0aGlzLmZvckVhY2goKHZhbHVlLCBrZXkpID0+IHtcbiAgICAgICAgICAgIGRhdGEucHVzaChba2V5LCB2YWx1ZV0pO1xuICAgICAgICB9KTtcbiAgICAgICAgcmV0dXJuIGRhdGE7XG4gICAgfVxuICAgIGZyb21KU09OKGRhdGEpIHtcbiAgICAgICAgdGhpcy5jbGVhcigpO1xuICAgICAgICBmb3IgKGNvbnN0IFtrZXksIHZhbHVlXSBvZiBkYXRhKSB7XG4gICAgICAgICAgICB0aGlzLnNldChrZXksIHZhbHVlKTtcbiAgICAgICAgfVxuICAgIH1cbn1cbmV4cG9ydHMuTGlua2VkTWFwID0gTGlua2VkTWFwO1xuY2xhc3MgTFJVQ2FjaGUgZXh0ZW5kcyBMaW5rZWRNYXAge1xuICAgIGNvbnN0cnVjdG9yKGxpbWl0LCByYXRpbyA9IDEpIHtcbiAgICAgICAgc3VwZXIoKTtcbiAgICAgICAgdGhpcy5fbGltaXQgPSBsaW1pdDtcbiAgICAgICAgdGhpcy5fcmF0aW8gPSBNYXRoLm1pbihNYXRoLm1heCgwLCByYXRpbyksIDEpO1xuICAgIH1cbiAgICBnZXQgbGltaXQoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLl9saW1pdDtcbiAgICB9XG4gICAgc2V0IGxpbWl0KGxpbWl0KSB7XG4gICAgICAgIHRoaXMuX2xpbWl0ID0gbGltaXQ7XG4gICAgICAgIHRoaXMuY2hlY2tUcmltKCk7XG4gICAgfVxuICAgIGdldCByYXRpbygpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX3JhdGlvO1xuICAgIH1cbiAgICBzZXQgcmF0aW8ocmF0aW8pIHtcbiAgICAgICAgdGhpcy5fcmF0aW8gPSBNYXRoLm1pbihNYXRoLm1heCgwLCByYXRpbyksIDEpO1xuICAgICAgICB0aGlzLmNoZWNrVHJpbSgpO1xuICAgIH1cbiAgICBnZXQoa2V5LCB0b3VjaCA9IFRvdWNoLkFzTmV3KSB7XG4gICAgICAgIHJldHVybiBzdXBlci5nZXQoa2V5LCB0b3VjaCk7XG4gICAgfVxuICAgIHBlZWsoa2V5KSB7XG4gICAgICAgIHJldHVybiBzdXBlci5nZXQoa2V5LCBUb3VjaC5Ob25lKTtcbiAgICB9XG4gICAgc2V0KGtleSwgdmFsdWUpIHtcbiAgICAgICAgc3VwZXIuc2V0KGtleSwgdmFsdWUsIFRvdWNoLkxhc3QpO1xuICAgICAgICB0aGlzLmNoZWNrVHJpbSgpO1xuICAgICAgICByZXR1cm4gdGhpcztcbiAgICB9XG4gICAgY2hlY2tUcmltKCkge1xuICAgICAgICBpZiAodGhpcy5zaXplID4gdGhpcy5fbGltaXQpIHtcbiAgICAgICAgICAgIHRoaXMudHJpbU9sZChNYXRoLnJvdW5kKHRoaXMuX2xpbWl0ICogdGhpcy5fcmF0aW8pKTtcbiAgICAgICAgfVxuICAgIH1cbn1cbmV4cG9ydHMuTFJVQ2FjaGUgPSBMUlVDYWNoZTtcbiIsICJcInVzZSBzdHJpY3RcIjtcbi8qLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiAgQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiAgTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTGljZW5zZS50eHQgaW4gdGhlIHByb2plY3Qgcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0qL1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiX19lc01vZHVsZVwiLCB7IHZhbHVlOiB0cnVlIH0pO1xuZXhwb3J0cy5EaXNwb3NhYmxlID0gdm9pZCAwO1xudmFyIERpc3Bvc2FibGU7XG4oZnVuY3Rpb24gKERpc3Bvc2FibGUpIHtcbiAgICBmdW5jdGlvbiBjcmVhdGUoZnVuYykge1xuICAgICAgICByZXR1cm4ge1xuICAgICAgICAgICAgZGlzcG9zZTogZnVuY1xuICAgICAgICB9O1xuICAgIH1cbiAgICBEaXNwb3NhYmxlLmNyZWF0ZSA9IGNyZWF0ZTtcbn0pKERpc3Bvc2FibGUgfHwgKGV4cG9ydHMuRGlzcG9zYWJsZSA9IERpc3Bvc2FibGUgPSB7fSkpO1xuIiwgIlwidXNlIHN0cmljdFwiO1xuLyogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbiAqIENvcHlyaWdodCAoYykgTWljcm9zb2Z0IENvcnBvcmF0aW9uLiBBbGwgcmlnaHRzIHJlc2VydmVkLlxuICogTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTGljZW5zZS50eHQgaW4gdGhlIHByb2plY3Qgcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSAqL1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiX19lc01vZHVsZVwiLCB7IHZhbHVlOiB0cnVlIH0pO1xubGV0IF9yYWw7XG5mdW5jdGlvbiBSQUwoKSB7XG4gICAgaWYgKF9yYWwgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYE5vIHJ1bnRpbWUgYWJzdHJhY3Rpb24gbGF5ZXIgaW5zdGFsbGVkYCk7XG4gICAgfVxuICAgIHJldHVybiBfcmFsO1xufVxuKGZ1bmN0aW9uIChSQUwpIHtcbiAgICBmdW5jdGlvbiBpbnN0YWxsKHJhbCkge1xuICAgICAgICBpZiAocmFsID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgTm8gcnVudGltZSBhYnN0cmFjdGlvbiBsYXllciBwcm92aWRlZGApO1xuICAgICAgICB9XG4gICAgICAgIF9yYWwgPSByYWw7XG4gICAgfVxuICAgIFJBTC5pbnN0YWxsID0gaW5zdGFsbDtcbn0pKFJBTCB8fCAoUkFMID0ge30pKTtcbmV4cG9ydHMuZGVmYXVsdCA9IFJBTDtcbiIsICJcInVzZSBzdHJpY3RcIjtcbi8qIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiBDb3B5cmlnaHQgKGMpIE1pY3Jvc29mdCBDb3Jwb3JhdGlvbi4gQWxsIHJpZ2h0cyByZXNlcnZlZC5cbiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBNSVQgTGljZW5zZS4gU2VlIExpY2Vuc2UudHh0IGluIHRoZSBwcm9qZWN0IHJvb3QgZm9yIGxpY2Vuc2UgaW5mb3JtYXRpb24uXG4gKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0gKi9cbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIl9fZXNNb2R1bGVcIiwgeyB2YWx1ZTogdHJ1ZSB9KTtcbmV4cG9ydHMuRW1pdHRlciA9IGV4cG9ydHMuRXZlbnQgPSB2b2lkIDA7XG5jb25zdCByYWxfMSA9IHJlcXVpcmUoXCIuL3JhbFwiKTtcbnZhciBFdmVudDtcbihmdW5jdGlvbiAoRXZlbnQpIHtcbiAgICBjb25zdCBfZGlzcG9zYWJsZSA9IHsgZGlzcG9zZSgpIHsgfSB9O1xuICAgIEV2ZW50Lk5vbmUgPSBmdW5jdGlvbiAoKSB7IHJldHVybiBfZGlzcG9zYWJsZTsgfTtcbn0pKEV2ZW50IHx8IChleHBvcnRzLkV2ZW50ID0gRXZlbnQgPSB7fSkpO1xuY2xhc3MgQ2FsbGJhY2tMaXN0IHtcbiAgICBhZGQoY2FsbGJhY2ssIGNvbnRleHQgPSBudWxsLCBidWNrZXQpIHtcbiAgICAgICAgaWYgKCF0aGlzLl9jYWxsYmFja3MpIHtcbiAgICAgICAgICAgIHRoaXMuX2NhbGxiYWNrcyA9IFtdO1xuICAgICAgICAgICAgdGhpcy5fY29udGV4dHMgPSBbXTtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLl9jYWxsYmFja3MucHVzaChjYWxsYmFjayk7XG4gICAgICAgIHRoaXMuX2NvbnRleHRzLnB1c2goY29udGV4dCk7XG4gICAgICAgIGlmIChBcnJheS5pc0FycmF5KGJ1Y2tldCkpIHtcbiAgICAgICAgICAgIGJ1Y2tldC5wdXNoKHsgZGlzcG9zZTogKCkgPT4gdGhpcy5yZW1vdmUoY2FsbGJhY2ssIGNvbnRleHQpIH0pO1xuICAgICAgICB9XG4gICAgfVxuICAgIHJlbW92ZShjYWxsYmFjaywgY29udGV4dCA9IG51bGwpIHtcbiAgICAgICAgaWYgKCF0aGlzLl9jYWxsYmFja3MpIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBsZXQgZm91bmRDYWxsYmFja1dpdGhEaWZmZXJlbnRDb250ZXh0ID0gZmFsc2U7XG4gICAgICAgIGZvciAobGV0IGkgPSAwLCBsZW4gPSB0aGlzLl9jYWxsYmFja3MubGVuZ3RoOyBpIDwgbGVuOyBpKyspIHtcbiAgICAgICAgICAgIGlmICh0aGlzLl9jYWxsYmFja3NbaV0gPT09IGNhbGxiYWNrKSB7XG4gICAgICAgICAgICAgICAgaWYgKHRoaXMuX2NvbnRleHRzW2ldID09PSBjb250ZXh0KSB7XG4gICAgICAgICAgICAgICAgICAgIC8vIGNhbGxiYWNrICYgY29udGV4dCBtYXRjaCA9PiByZW1vdmUgaXRcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5fY2FsbGJhY2tzLnNwbGljZShpLCAxKTtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5fY29udGV4dHMuc3BsaWNlKGksIDEpO1xuICAgICAgICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICBmb3VuZENhbGxiYWNrV2l0aERpZmZlcmVudENvbnRleHQgPSB0cnVlO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICBpZiAoZm91bmRDYWxsYmFja1dpdGhEaWZmZXJlbnRDb250ZXh0KSB7XG4gICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoJ1doZW4gYWRkaW5nIGEgbGlzdGVuZXIgd2l0aCBhIGNvbnRleHQsIHlvdSBzaG91bGQgcmVtb3ZlIGl0IHdpdGggdGhlIHNhbWUgY29udGV4dCcpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGludm9rZSguLi5hcmdzKSB7XG4gICAgICAgIGlmICghdGhpcy5fY2FsbGJhY2tzKSB7XG4gICAgICAgICAgICByZXR1cm4gW107XG4gICAgICAgIH1cbiAgICAgICAgY29uc3QgcmV0ID0gW10sIGNhbGxiYWNrcyA9IHRoaXMuX2NhbGxiYWNrcy5zbGljZSgwKSwgY29udGV4dHMgPSB0aGlzLl9jb250ZXh0cy5zbGljZSgwKTtcbiAgICAgICAgZm9yIChsZXQgaSA9IDAsIGxlbiA9IGNhbGxiYWNrcy5sZW5ndGg7IGkgPCBsZW47IGkrKykge1xuICAgICAgICAgICAgdHJ5IHtcbiAgICAgICAgICAgICAgICByZXQucHVzaChjYWxsYmFja3NbaV0uYXBwbHkoY29udGV4dHNbaV0sIGFyZ3MpKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNhdGNoIChlKSB7XG4gICAgICAgICAgICAgICAgLy8gZXNsaW50LWRpc2FibGUtbmV4dC1saW5lIG5vLWNvbnNvbGVcbiAgICAgICAgICAgICAgICAoMCwgcmFsXzEuZGVmYXVsdCkoKS5jb25zb2xlLmVycm9yKGUpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXQ7XG4gICAgfVxuICAgIGlzRW1wdHkoKSB7XG4gICAgICAgIHJldHVybiAhdGhpcy5fY2FsbGJhY2tzIHx8IHRoaXMuX2NhbGxiYWNrcy5sZW5ndGggPT09IDA7XG4gICAgfVxuICAgIGRpc3Bvc2UoKSB7XG4gICAgICAgIHRoaXMuX2NhbGxiYWNrcyA9IHVuZGVmaW5lZDtcbiAgICAgICAgdGhpcy5fY29udGV4dHMgPSB1bmRlZmluZWQ7XG4gICAgfVxufVxuY2xhc3MgRW1pdHRlciB7XG4gICAgY29uc3RydWN0b3IoX29wdGlvbnMpIHtcbiAgICAgICAgdGhpcy5fb3B0aW9ucyA9IF9vcHRpb25zO1xuICAgIH1cbiAgICAvKipcbiAgICAgKiBGb3IgdGhlIHB1YmxpYyB0byBhbGxvdyB0byBzdWJzY3JpYmVcbiAgICAgKiB0byBldmVudHMgZnJvbSB0aGlzIEVtaXR0ZXJcbiAgICAgKi9cbiAgICBnZXQgZXZlbnQoKSB7XG4gICAgICAgIGlmICghdGhpcy5fZXZlbnQpIHtcbiAgICAgICAgICAgIHRoaXMuX2V2ZW50ID0gKGxpc3RlbmVyLCB0aGlzQXJncywgZGlzcG9zYWJsZXMpID0+IHtcbiAgICAgICAgICAgICAgICBpZiAoIXRoaXMuX2NhbGxiYWNrcykge1xuICAgICAgICAgICAgICAgICAgICB0aGlzLl9jYWxsYmFja3MgPSBuZXcgQ2FsbGJhY2tMaXN0KCk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGlmICh0aGlzLl9vcHRpb25zICYmIHRoaXMuX29wdGlvbnMub25GaXJzdExpc3RlbmVyQWRkICYmIHRoaXMuX2NhbGxiYWNrcy5pc0VtcHR5KCkpIHtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5fb3B0aW9ucy5vbkZpcnN0TGlzdGVuZXJBZGQodGhpcyk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIHRoaXMuX2NhbGxiYWNrcy5hZGQobGlzdGVuZXIsIHRoaXNBcmdzKTtcbiAgICAgICAgICAgICAgICBjb25zdCByZXN1bHQgPSB7XG4gICAgICAgICAgICAgICAgICAgIGRpc3Bvc2U6ICgpID0+IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICghdGhpcy5fY2FsbGJhY2tzKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8gZGlzcG9zYWJsZSBpcyBkaXNwb3NlZCBhZnRlciBlbWl0dGVyIGlzIGRpc3Bvc2VkLlxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIHRoaXMuX2NhbGxiYWNrcy5yZW1vdmUobGlzdGVuZXIsIHRoaXNBcmdzKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJlc3VsdC5kaXNwb3NlID0gRW1pdHRlci5fbm9vcDtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICh0aGlzLl9vcHRpb25zICYmIHRoaXMuX29wdGlvbnMub25MYXN0TGlzdGVuZXJSZW1vdmUgJiYgdGhpcy5fY2FsbGJhY2tzLmlzRW1wdHkoKSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHRoaXMuX29wdGlvbnMub25MYXN0TGlzdGVuZXJSZW1vdmUodGhpcyk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICAgIGlmIChBcnJheS5pc0FycmF5KGRpc3Bvc2FibGVzKSkge1xuICAgICAgICAgICAgICAgICAgICBkaXNwb3NhYmxlcy5wdXNoKHJlc3VsdCk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgICAgICAgICB9O1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiB0aGlzLl9ldmVudDtcbiAgICB9XG4gICAgLyoqXG4gICAgICogVG8gYmUga2VwdCBwcml2YXRlIHRvIGZpcmUgYW4gZXZlbnQgdG9cbiAgICAgKiBzdWJzY3JpYmVyc1xuICAgICAqL1xuICAgIGZpcmUoZXZlbnQpIHtcbiAgICAgICAgaWYgKHRoaXMuX2NhbGxiYWNrcykge1xuICAgICAgICAgICAgdGhpcy5fY2FsbGJhY2tzLmludm9rZS5jYWxsKHRoaXMuX2NhbGxiYWNrcywgZXZlbnQpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGRpc3Bvc2UoKSB7XG4gICAgICAgIGlmICh0aGlzLl9jYWxsYmFja3MpIHtcbiAgICAgICAgICAgIHRoaXMuX2NhbGxiYWNrcy5kaXNwb3NlKCk7XG4gICAgICAgICAgICB0aGlzLl9jYWxsYmFja3MgPSB1bmRlZmluZWQ7XG4gICAgICAgIH1cbiAgICB9XG59XG5leHBvcnRzLkVtaXR0ZXIgPSBFbWl0dGVyO1xuRW1pdHRlci5fbm9vcCA9IGZ1bmN0aW9uICgpIHsgfTtcbiIsICJcInVzZSBzdHJpY3RcIjtcbi8qLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiAgQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiAgTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTGljZW5zZS50eHQgaW4gdGhlIHByb2plY3Qgcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0qL1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiX19lc01vZHVsZVwiLCB7IHZhbHVlOiB0cnVlIH0pO1xuZXhwb3J0cy5DYW5jZWxsYXRpb25Ub2tlblNvdXJjZSA9IGV4cG9ydHMuQ2FuY2VsbGF0aW9uVG9rZW4gPSB2b2lkIDA7XG5jb25zdCByYWxfMSA9IHJlcXVpcmUoXCIuL3JhbFwiKTtcbmNvbnN0IElzID0gcmVxdWlyZShcIi4vaXNcIik7XG5jb25zdCBldmVudHNfMSA9IHJlcXVpcmUoXCIuL2V2ZW50c1wiKTtcbnZhciBDYW5jZWxsYXRpb25Ub2tlbjtcbihmdW5jdGlvbiAoQ2FuY2VsbGF0aW9uVG9rZW4pIHtcbiAgICBDYW5jZWxsYXRpb25Ub2tlbi5Ob25lID0gT2JqZWN0LmZyZWV6ZSh7XG4gICAgICAgIGlzQ2FuY2VsbGF0aW9uUmVxdWVzdGVkOiBmYWxzZSxcbiAgICAgICAgb25DYW5jZWxsYXRpb25SZXF1ZXN0ZWQ6IGV2ZW50c18xLkV2ZW50Lk5vbmVcbiAgICB9KTtcbiAgICBDYW5jZWxsYXRpb25Ub2tlbi5DYW5jZWxsZWQgPSBPYmplY3QuZnJlZXplKHtcbiAgICAgICAgaXNDYW5jZWxsYXRpb25SZXF1ZXN0ZWQ6IHRydWUsXG4gICAgICAgIG9uQ2FuY2VsbGF0aW9uUmVxdWVzdGVkOiBldmVudHNfMS5FdmVudC5Ob25lXG4gICAgfSk7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBjYW5kaWRhdGUgJiYgKGNhbmRpZGF0ZSA9PT0gQ2FuY2VsbGF0aW9uVG9rZW4uTm9uZVxuICAgICAgICAgICAgfHwgY2FuZGlkYXRlID09PSBDYW5jZWxsYXRpb25Ub2tlbi5DYW5jZWxsZWRcbiAgICAgICAgICAgIHx8IChJcy5ib29sZWFuKGNhbmRpZGF0ZS5pc0NhbmNlbGxhdGlvblJlcXVlc3RlZCkgJiYgISFjYW5kaWRhdGUub25DYW5jZWxsYXRpb25SZXF1ZXN0ZWQpKTtcbiAgICB9XG4gICAgQ2FuY2VsbGF0aW9uVG9rZW4uaXMgPSBpcztcbn0pKENhbmNlbGxhdGlvblRva2VuIHx8IChleHBvcnRzLkNhbmNlbGxhdGlvblRva2VuID0gQ2FuY2VsbGF0aW9uVG9rZW4gPSB7fSkpO1xuY29uc3Qgc2hvcnRjdXRFdmVudCA9IE9iamVjdC5mcmVlemUoZnVuY3Rpb24gKGNhbGxiYWNrLCBjb250ZXh0KSB7XG4gICAgY29uc3QgaGFuZGxlID0gKDAsIHJhbF8xLmRlZmF1bHQpKCkudGltZXIuc2V0VGltZW91dChjYWxsYmFjay5iaW5kKGNvbnRleHQpLCAwKTtcbiAgICByZXR1cm4geyBkaXNwb3NlKCkgeyBoYW5kbGUuZGlzcG9zZSgpOyB9IH07XG59KTtcbmNsYXNzIE11dGFibGVUb2tlbiB7XG4gICAgY29uc3RydWN0b3IoKSB7XG4gICAgICAgIHRoaXMuX2lzQ2FuY2VsbGVkID0gZmFsc2U7XG4gICAgfVxuICAgIGNhbmNlbCgpIHtcbiAgICAgICAgaWYgKCF0aGlzLl9pc0NhbmNlbGxlZCkge1xuICAgICAgICAgICAgdGhpcy5faXNDYW5jZWxsZWQgPSB0cnVlO1xuICAgICAgICAgICAgaWYgKHRoaXMuX2VtaXR0ZXIpIHtcbiAgICAgICAgICAgICAgICB0aGlzLl9lbWl0dGVyLmZpcmUodW5kZWZpbmVkKTtcbiAgICAgICAgICAgICAgICB0aGlzLmRpc3Bvc2UoKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgIH1cbiAgICBnZXQgaXNDYW5jZWxsYXRpb25SZXF1ZXN0ZWQoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLl9pc0NhbmNlbGxlZDtcbiAgICB9XG4gICAgZ2V0IG9uQ2FuY2VsbGF0aW9uUmVxdWVzdGVkKCkge1xuICAgICAgICBpZiAodGhpcy5faXNDYW5jZWxsZWQpIHtcbiAgICAgICAgICAgIHJldHVybiBzaG9ydGN1dEV2ZW50O1xuICAgICAgICB9XG4gICAgICAgIGlmICghdGhpcy5fZW1pdHRlcikge1xuICAgICAgICAgICAgdGhpcy5fZW1pdHRlciA9IG5ldyBldmVudHNfMS5FbWl0dGVyKCk7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHRoaXMuX2VtaXR0ZXIuZXZlbnQ7XG4gICAgfVxuICAgIGRpc3Bvc2UoKSB7XG4gICAgICAgIGlmICh0aGlzLl9lbWl0dGVyKSB7XG4gICAgICAgICAgICB0aGlzLl9lbWl0dGVyLmRpc3Bvc2UoKTtcbiAgICAgICAgICAgIHRoaXMuX2VtaXR0ZXIgPSB1bmRlZmluZWQ7XG4gICAgICAgIH1cbiAgICB9XG59XG5jbGFzcyBDYW5jZWxsYXRpb25Ub2tlblNvdXJjZSB7XG4gICAgZ2V0IHRva2VuKCkge1xuICAgICAgICBpZiAoIXRoaXMuX3Rva2VuKSB7XG4gICAgICAgICAgICAvLyBiZSBsYXp5IGFuZCBjcmVhdGUgdGhlIHRva2VuIG9ubHkgd2hlblxuICAgICAgICAgICAgLy8gYWN0dWFsbHkgbmVlZGVkXG4gICAgICAgICAgICB0aGlzLl90b2tlbiA9IG5ldyBNdXRhYmxlVG9rZW4oKTtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gdGhpcy5fdG9rZW47XG4gICAgfVxuICAgIGNhbmNlbCgpIHtcbiAgICAgICAgaWYgKCF0aGlzLl90b2tlbikge1xuICAgICAgICAgICAgLy8gc2F2ZSBhbiBvYmplY3QgYnkgcmV0dXJuaW5nIHRoZSBkZWZhdWx0XG4gICAgICAgICAgICAvLyBjYW5jZWxsZWQgdG9rZW4gd2hlbiBjYW5jZWxsYXRpb24gaGFwcGVuc1xuICAgICAgICAgICAgLy8gYmVmb3JlIHNvbWVvbmUgYXNrcyBmb3IgdGhlIHRva2VuXG4gICAgICAgICAgICB0aGlzLl90b2tlbiA9IENhbmNlbGxhdGlvblRva2VuLkNhbmNlbGxlZDtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHRoaXMuX3Rva2VuLmNhbmNlbCgpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGRpc3Bvc2UoKSB7XG4gICAgICAgIGlmICghdGhpcy5fdG9rZW4pIHtcbiAgICAgICAgICAgIC8vIGVuc3VyZSB0byBpbml0aWFsaXplIHdpdGggYW4gZW1wdHkgdG9rZW4gaWYgd2UgaGFkIG5vbmVcbiAgICAgICAgICAgIHRoaXMuX3Rva2VuID0gQ2FuY2VsbGF0aW9uVG9rZW4uTm9uZTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIGlmICh0aGlzLl90b2tlbiBpbnN0YW5jZW9mIE11dGFibGVUb2tlbikge1xuICAgICAgICAgICAgLy8gYWN0dWFsbHkgZGlzcG9zZVxuICAgICAgICAgICAgdGhpcy5fdG9rZW4uZGlzcG9zZSgpO1xuICAgICAgICB9XG4gICAgfVxufVxuZXhwb3J0cy5DYW5jZWxsYXRpb25Ub2tlblNvdXJjZSA9IENhbmNlbGxhdGlvblRva2VuU291cmNlO1xuIiwgIlwidXNlIHN0cmljdFwiO1xuLyogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbiAqIENvcHlyaWdodCAoYykgTWljcm9zb2Z0IENvcnBvcmF0aW9uLiBBbGwgcmlnaHRzIHJlc2VydmVkLlxuICogTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTGljZW5zZS50eHQgaW4gdGhlIHByb2plY3Qgcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSAqL1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiX19lc01vZHVsZVwiLCB7IHZhbHVlOiB0cnVlIH0pO1xuZXhwb3J0cy5TaGFyZWRBcnJheVJlY2VpdmVyU3RyYXRlZ3kgPSBleHBvcnRzLlNoYXJlZEFycmF5U2VuZGVyU3RyYXRlZ3kgPSB2b2lkIDA7XG5jb25zdCBjYW5jZWxsYXRpb25fMSA9IHJlcXVpcmUoXCIuL2NhbmNlbGxhdGlvblwiKTtcbnZhciBDYW5jZWxsYXRpb25TdGF0ZTtcbihmdW5jdGlvbiAoQ2FuY2VsbGF0aW9uU3RhdGUpIHtcbiAgICBDYW5jZWxsYXRpb25TdGF0ZS5Db250aW51ZSA9IDA7XG4gICAgQ2FuY2VsbGF0aW9uU3RhdGUuQ2FuY2VsbGVkID0gMTtcbn0pKENhbmNlbGxhdGlvblN0YXRlIHx8IChDYW5jZWxsYXRpb25TdGF0ZSA9IHt9KSk7XG5jbGFzcyBTaGFyZWRBcnJheVNlbmRlclN0cmF0ZWd5IHtcbiAgICBjb25zdHJ1Y3RvcigpIHtcbiAgICAgICAgdGhpcy5idWZmZXJzID0gbmV3IE1hcCgpO1xuICAgIH1cbiAgICBlbmFibGVDYW5jZWxsYXRpb24ocmVxdWVzdCkge1xuICAgICAgICBpZiAocmVxdWVzdC5pZCA9PT0gbnVsbCkge1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGNvbnN0IGJ1ZmZlciA9IG5ldyBTaGFyZWRBcnJheUJ1ZmZlcig0KTtcbiAgICAgICAgY29uc3QgZGF0YSA9IG5ldyBJbnQzMkFycmF5KGJ1ZmZlciwgMCwgMSk7XG4gICAgICAgIGRhdGFbMF0gPSBDYW5jZWxsYXRpb25TdGF0ZS5Db250aW51ZTtcbiAgICAgICAgdGhpcy5idWZmZXJzLnNldChyZXF1ZXN0LmlkLCBidWZmZXIpO1xuICAgICAgICByZXF1ZXN0LiRjYW5jZWxsYXRpb25EYXRhID0gYnVmZmVyO1xuICAgIH1cbiAgICBhc3luYyBzZW5kQ2FuY2VsbGF0aW9uKF9jb25uLCBpZCkge1xuICAgICAgICBjb25zdCBidWZmZXIgPSB0aGlzLmJ1ZmZlcnMuZ2V0KGlkKTtcbiAgICAgICAgaWYgKGJ1ZmZlciA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgICAgY29uc3QgZGF0YSA9IG5ldyBJbnQzMkFycmF5KGJ1ZmZlciwgMCwgMSk7XG4gICAgICAgIEF0b21pY3Muc3RvcmUoZGF0YSwgMCwgQ2FuY2VsbGF0aW9uU3RhdGUuQ2FuY2VsbGVkKTtcbiAgICB9XG4gICAgY2xlYW51cChpZCkge1xuICAgICAgICB0aGlzLmJ1ZmZlcnMuZGVsZXRlKGlkKTtcbiAgICB9XG4gICAgZGlzcG9zZSgpIHtcbiAgICAgICAgdGhpcy5idWZmZXJzLmNsZWFyKCk7XG4gICAgfVxufVxuZXhwb3J0cy5TaGFyZWRBcnJheVNlbmRlclN0cmF0ZWd5ID0gU2hhcmVkQXJyYXlTZW5kZXJTdHJhdGVneTtcbmNsYXNzIFNoYXJlZEFycmF5QnVmZmVyQ2FuY2VsbGF0aW9uVG9rZW4ge1xuICAgIGNvbnN0cnVjdG9yKGJ1ZmZlcikge1xuICAgICAgICB0aGlzLmRhdGEgPSBuZXcgSW50MzJBcnJheShidWZmZXIsIDAsIDEpO1xuICAgIH1cbiAgICBnZXQgaXNDYW5jZWxsYXRpb25SZXF1ZXN0ZWQoKSB7XG4gICAgICAgIHJldHVybiBBdG9taWNzLmxvYWQodGhpcy5kYXRhLCAwKSA9PT0gQ2FuY2VsbGF0aW9uU3RhdGUuQ2FuY2VsbGVkO1xuICAgIH1cbiAgICBnZXQgb25DYW5jZWxsYXRpb25SZXF1ZXN0ZWQoKSB7XG4gICAgICAgIHRocm93IG5ldyBFcnJvcihgQ2FuY2VsbGF0aW9uIG92ZXIgU2hhcmVkQXJyYXlCdWZmZXIgZG9lc24ndCBzdXBwb3J0IGNhbmNlbGxhdGlvbiBldmVudHNgKTtcbiAgICB9XG59XG5jbGFzcyBTaGFyZWRBcnJheUJ1ZmZlckNhbmNlbGxhdGlvblRva2VuU291cmNlIHtcbiAgICBjb25zdHJ1Y3RvcihidWZmZXIpIHtcbiAgICAgICAgdGhpcy50b2tlbiA9IG5ldyBTaGFyZWRBcnJheUJ1ZmZlckNhbmNlbGxhdGlvblRva2VuKGJ1ZmZlcik7XG4gICAgfVxuICAgIGNhbmNlbCgpIHtcbiAgICB9XG4gICAgZGlzcG9zZSgpIHtcbiAgICB9XG59XG5jbGFzcyBTaGFyZWRBcnJheVJlY2VpdmVyU3RyYXRlZ3kge1xuICAgIGNvbnN0cnVjdG9yKCkge1xuICAgICAgICB0aGlzLmtpbmQgPSAncmVxdWVzdCc7XG4gICAgfVxuICAgIGNyZWF0ZUNhbmNlbGxhdGlvblRva2VuU291cmNlKHJlcXVlc3QpIHtcbiAgICAgICAgY29uc3QgYnVmZmVyID0gcmVxdWVzdC4kY2FuY2VsbGF0aW9uRGF0YTtcbiAgICAgICAgaWYgKGJ1ZmZlciA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXR1cm4gbmV3IGNhbmNlbGxhdGlvbl8xLkNhbmNlbGxhdGlvblRva2VuU291cmNlKCk7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIG5ldyBTaGFyZWRBcnJheUJ1ZmZlckNhbmNlbGxhdGlvblRva2VuU291cmNlKGJ1ZmZlcik7XG4gICAgfVxufVxuZXhwb3J0cy5TaGFyZWRBcnJheVJlY2VpdmVyU3RyYXRlZ3kgPSBTaGFyZWRBcnJheVJlY2VpdmVyU3RyYXRlZ3k7XG4iLCAiXCJ1c2Ugc3RyaWN0XCI7XG4vKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJfX2VzTW9kdWxlXCIsIHsgdmFsdWU6IHRydWUgfSk7XG5leHBvcnRzLlNlbWFwaG9yZSA9IHZvaWQgMDtcbmNvbnN0IHJhbF8xID0gcmVxdWlyZShcIi4vcmFsXCIpO1xuY2xhc3MgU2VtYXBob3JlIHtcbiAgICBjb25zdHJ1Y3RvcihjYXBhY2l0eSA9IDEpIHtcbiAgICAgICAgaWYgKGNhcGFjaXR5IDw9IDApIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcignQ2FwYWNpdHkgbXVzdCBiZSBncmVhdGVyIHRoYW4gMCcpO1xuICAgICAgICB9XG4gICAgICAgIHRoaXMuX2NhcGFjaXR5ID0gY2FwYWNpdHk7XG4gICAgICAgIHRoaXMuX2FjdGl2ZSA9IDA7XG4gICAgICAgIHRoaXMuX3dhaXRpbmcgPSBbXTtcbiAgICB9XG4gICAgbG9jayh0aHVuaykge1xuICAgICAgICByZXR1cm4gbmV3IFByb21pc2UoKHJlc29sdmUsIHJlamVjdCkgPT4ge1xuICAgICAgICAgICAgdGhpcy5fd2FpdGluZy5wdXNoKHsgdGh1bmssIHJlc29sdmUsIHJlamVjdCB9KTtcbiAgICAgICAgICAgIHRoaXMucnVuTmV4dCgpO1xuICAgICAgICB9KTtcbiAgICB9XG4gICAgZ2V0IGFjdGl2ZSgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX2FjdGl2ZTtcbiAgICB9XG4gICAgcnVuTmV4dCgpIHtcbiAgICAgICAgaWYgKHRoaXMuX3dhaXRpbmcubGVuZ3RoID09PSAwIHx8IHRoaXMuX2FjdGl2ZSA9PT0gdGhpcy5fY2FwYWNpdHkpIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICAoMCwgcmFsXzEuZGVmYXVsdCkoKS50aW1lci5zZXRJbW1lZGlhdGUoKCkgPT4gdGhpcy5kb1J1bk5leHQoKSk7XG4gICAgfVxuICAgIGRvUnVuTmV4dCgpIHtcbiAgICAgICAgaWYgKHRoaXMuX3dhaXRpbmcubGVuZ3RoID09PSAwIHx8IHRoaXMuX2FjdGl2ZSA9PT0gdGhpcy5fY2FwYWNpdHkpIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBjb25zdCBuZXh0ID0gdGhpcy5fd2FpdGluZy5zaGlmdCgpO1xuICAgICAgICB0aGlzLl9hY3RpdmUrKztcbiAgICAgICAgaWYgKHRoaXMuX2FjdGl2ZSA+IHRoaXMuX2NhcGFjaXR5KSB7XG4gICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYFRvIG1hbnkgdGh1bmtzIGFjdGl2ZWApO1xuICAgICAgICB9XG4gICAgICAgIHRyeSB7XG4gICAgICAgICAgICBjb25zdCByZXN1bHQgPSBuZXh0LnRodW5rKCk7XG4gICAgICAgICAgICBpZiAocmVzdWx0IGluc3RhbmNlb2YgUHJvbWlzZSkge1xuICAgICAgICAgICAgICAgIHJlc3VsdC50aGVuKCh2YWx1ZSkgPT4ge1xuICAgICAgICAgICAgICAgICAgICB0aGlzLl9hY3RpdmUtLTtcbiAgICAgICAgICAgICAgICAgICAgbmV4dC5yZXNvbHZlKHZhbHVlKTtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5ydW5OZXh0KCk7XG4gICAgICAgICAgICAgICAgfSwgKGVycikgPT4ge1xuICAgICAgICAgICAgICAgICAgICB0aGlzLl9hY3RpdmUtLTtcbiAgICAgICAgICAgICAgICAgICAgbmV4dC5yZWplY3QoZXJyKTtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5ydW5OZXh0KCk7XG4gICAgICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICB0aGlzLl9hY3RpdmUtLTtcbiAgICAgICAgICAgICAgICBuZXh0LnJlc29sdmUocmVzdWx0KTtcbiAgICAgICAgICAgICAgICB0aGlzLnJ1bk5leHQoKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICBjYXRjaCAoZXJyKSB7XG4gICAgICAgICAgICB0aGlzLl9hY3RpdmUtLTtcbiAgICAgICAgICAgIG5leHQucmVqZWN0KGVycik7XG4gICAgICAgICAgICB0aGlzLnJ1bk5leHQoKTtcbiAgICAgICAgfVxuICAgIH1cbn1cbmV4cG9ydHMuU2VtYXBob3JlID0gU2VtYXBob3JlO1xuIiwgIlwidXNlIHN0cmljdFwiO1xuLyogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbiAqIENvcHlyaWdodCAoYykgTWljcm9zb2Z0IENvcnBvcmF0aW9uLiBBbGwgcmlnaHRzIHJlc2VydmVkLlxuICogTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTGljZW5zZS50eHQgaW4gdGhlIHByb2plY3Qgcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSAqL1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiX19lc01vZHVsZVwiLCB7IHZhbHVlOiB0cnVlIH0pO1xuZXhwb3J0cy5SZWFkYWJsZVN0cmVhbU1lc3NhZ2VSZWFkZXIgPSBleHBvcnRzLkFic3RyYWN0TWVzc2FnZVJlYWRlciA9IGV4cG9ydHMuTWVzc2FnZVJlYWRlciA9IHZvaWQgMDtcbmNvbnN0IHJhbF8xID0gcmVxdWlyZShcIi4vcmFsXCIpO1xuY29uc3QgSXMgPSByZXF1aXJlKFwiLi9pc1wiKTtcbmNvbnN0IGV2ZW50c18xID0gcmVxdWlyZShcIi4vZXZlbnRzXCIpO1xuY29uc3Qgc2VtYXBob3JlXzEgPSByZXF1aXJlKFwiLi9zZW1hcGhvcmVcIik7XG52YXIgTWVzc2FnZVJlYWRlcjtcbihmdW5jdGlvbiAoTWVzc2FnZVJlYWRlcikge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiBJcy5mdW5jKGNhbmRpZGF0ZS5saXN0ZW4pICYmIElzLmZ1bmMoY2FuZGlkYXRlLmRpc3Bvc2UpICYmXG4gICAgICAgICAgICBJcy5mdW5jKGNhbmRpZGF0ZS5vbkVycm9yKSAmJiBJcy5mdW5jKGNhbmRpZGF0ZS5vbkNsb3NlKSAmJiBJcy5mdW5jKGNhbmRpZGF0ZS5vblBhcnRpYWxNZXNzYWdlKTtcbiAgICB9XG4gICAgTWVzc2FnZVJlYWRlci5pcyA9IGlzO1xufSkoTWVzc2FnZVJlYWRlciB8fCAoZXhwb3J0cy5NZXNzYWdlUmVhZGVyID0gTWVzc2FnZVJlYWRlciA9IHt9KSk7XG5jbGFzcyBBYnN0cmFjdE1lc3NhZ2VSZWFkZXIge1xuICAgIGNvbnN0cnVjdG9yKCkge1xuICAgICAgICB0aGlzLmVycm9yRW1pdHRlciA9IG5ldyBldmVudHNfMS5FbWl0dGVyKCk7XG4gICAgICAgIHRoaXMuY2xvc2VFbWl0dGVyID0gbmV3IGV2ZW50c18xLkVtaXR0ZXIoKTtcbiAgICAgICAgdGhpcy5wYXJ0aWFsTWVzc2FnZUVtaXR0ZXIgPSBuZXcgZXZlbnRzXzEuRW1pdHRlcigpO1xuICAgIH1cbiAgICBkaXNwb3NlKCkge1xuICAgICAgICB0aGlzLmVycm9yRW1pdHRlci5kaXNwb3NlKCk7XG4gICAgICAgIHRoaXMuY2xvc2VFbWl0dGVyLmRpc3Bvc2UoKTtcbiAgICB9XG4gICAgZ2V0IG9uRXJyb3IoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLmVycm9yRW1pdHRlci5ldmVudDtcbiAgICB9XG4gICAgZmlyZUVycm9yKGVycm9yKSB7XG4gICAgICAgIHRoaXMuZXJyb3JFbWl0dGVyLmZpcmUodGhpcy5hc0Vycm9yKGVycm9yKSk7XG4gICAgfVxuICAgIGdldCBvbkNsb3NlKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5jbG9zZUVtaXR0ZXIuZXZlbnQ7XG4gICAgfVxuICAgIGZpcmVDbG9zZSgpIHtcbiAgICAgICAgdGhpcy5jbG9zZUVtaXR0ZXIuZmlyZSh1bmRlZmluZWQpO1xuICAgIH1cbiAgICBnZXQgb25QYXJ0aWFsTWVzc2FnZSgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMucGFydGlhbE1lc3NhZ2VFbWl0dGVyLmV2ZW50O1xuICAgIH1cbiAgICBmaXJlUGFydGlhbE1lc3NhZ2UoaW5mbykge1xuICAgICAgICB0aGlzLnBhcnRpYWxNZXNzYWdlRW1pdHRlci5maXJlKGluZm8pO1xuICAgIH1cbiAgICBhc0Vycm9yKGVycm9yKSB7XG4gICAgICAgIGlmIChlcnJvciBpbnN0YW5jZW9mIEVycm9yKSB7XG4gICAgICAgICAgICByZXR1cm4gZXJyb3I7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICByZXR1cm4gbmV3IEVycm9yKGBSZWFkZXIgcmVjZWl2ZWQgZXJyb3IuIFJlYXNvbjogJHtJcy5zdHJpbmcoZXJyb3IubWVzc2FnZSkgPyBlcnJvci5tZXNzYWdlIDogJ3Vua25vd24nfWApO1xuICAgICAgICB9XG4gICAgfVxufVxuZXhwb3J0cy5BYnN0cmFjdE1lc3NhZ2VSZWFkZXIgPSBBYnN0cmFjdE1lc3NhZ2VSZWFkZXI7XG52YXIgUmVzb2x2ZWRNZXNzYWdlUmVhZGVyT3B0aW9ucztcbihmdW5jdGlvbiAoUmVzb2x2ZWRNZXNzYWdlUmVhZGVyT3B0aW9ucykge1xuICAgIGZ1bmN0aW9uIGZyb21PcHRpb25zKG9wdGlvbnMpIHtcbiAgICAgICAgbGV0IGNoYXJzZXQ7XG4gICAgICAgIGxldCByZXN1bHQ7XG4gICAgICAgIGxldCBjb250ZW50RGVjb2RlcjtcbiAgICAgICAgY29uc3QgY29udGVudERlY29kZXJzID0gbmV3IE1hcCgpO1xuICAgICAgICBsZXQgY29udGVudFR5cGVEZWNvZGVyO1xuICAgICAgICBjb25zdCBjb250ZW50VHlwZURlY29kZXJzID0gbmV3IE1hcCgpO1xuICAgICAgICBpZiAob3B0aW9ucyA9PT0gdW5kZWZpbmVkIHx8IHR5cGVvZiBvcHRpb25zID09PSAnc3RyaW5nJykge1xuICAgICAgICAgICAgY2hhcnNldCA9IG9wdGlvbnMgPz8gJ3V0Zi04JztcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIGNoYXJzZXQgPSBvcHRpb25zLmNoYXJzZXQgPz8gJ3V0Zi04JztcbiAgICAgICAgICAgIGlmIChvcHRpb25zLmNvbnRlbnREZWNvZGVyICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICBjb250ZW50RGVjb2RlciA9IG9wdGlvbnMuY29udGVudERlY29kZXI7XG4gICAgICAgICAgICAgICAgY29udGVudERlY29kZXJzLnNldChjb250ZW50RGVjb2Rlci5uYW1lLCBjb250ZW50RGVjb2Rlcik7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAob3B0aW9ucy5jb250ZW50RGVjb2RlcnMgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgIGZvciAoY29uc3QgZGVjb2RlciBvZiBvcHRpb25zLmNvbnRlbnREZWNvZGVycykge1xuICAgICAgICAgICAgICAgICAgICBjb250ZW50RGVjb2RlcnMuc2V0KGRlY29kZXIubmFtZSwgZGVjb2Rlcik7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaWYgKG9wdGlvbnMuY29udGVudFR5cGVEZWNvZGVyICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICBjb250ZW50VHlwZURlY29kZXIgPSBvcHRpb25zLmNvbnRlbnRUeXBlRGVjb2RlcjtcbiAgICAgICAgICAgICAgICBjb250ZW50VHlwZURlY29kZXJzLnNldChjb250ZW50VHlwZURlY29kZXIubmFtZSwgY29udGVudFR5cGVEZWNvZGVyKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmIChvcHRpb25zLmNvbnRlbnRUeXBlRGVjb2RlcnMgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgIGZvciAoY29uc3QgZGVjb2RlciBvZiBvcHRpb25zLmNvbnRlbnRUeXBlRGVjb2RlcnMpIHtcbiAgICAgICAgICAgICAgICAgICAgY29udGVudFR5cGVEZWNvZGVycy5zZXQoZGVjb2Rlci5uYW1lLCBkZWNvZGVyKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgaWYgKGNvbnRlbnRUeXBlRGVjb2RlciA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICBjb250ZW50VHlwZURlY29kZXIgPSAoMCwgcmFsXzEuZGVmYXVsdCkoKS5hcHBsaWNhdGlvbkpzb24uZGVjb2RlcjtcbiAgICAgICAgICAgIGNvbnRlbnRUeXBlRGVjb2RlcnMuc2V0KGNvbnRlbnRUeXBlRGVjb2Rlci5uYW1lLCBjb250ZW50VHlwZURlY29kZXIpO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiB7IGNoYXJzZXQsIGNvbnRlbnREZWNvZGVyLCBjb250ZW50RGVjb2RlcnMsIGNvbnRlbnRUeXBlRGVjb2RlciwgY29udGVudFR5cGVEZWNvZGVycyB9O1xuICAgIH1cbiAgICBSZXNvbHZlZE1lc3NhZ2VSZWFkZXJPcHRpb25zLmZyb21PcHRpb25zID0gZnJvbU9wdGlvbnM7XG59KShSZXNvbHZlZE1lc3NhZ2VSZWFkZXJPcHRpb25zIHx8IChSZXNvbHZlZE1lc3NhZ2VSZWFkZXJPcHRpb25zID0ge30pKTtcbmNsYXNzIFJlYWRhYmxlU3RyZWFtTWVzc2FnZVJlYWRlciBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVJlYWRlciB7XG4gICAgY29uc3RydWN0b3IocmVhZGFibGUsIG9wdGlvbnMpIHtcbiAgICAgICAgc3VwZXIoKTtcbiAgICAgICAgdGhpcy5yZWFkYWJsZSA9IHJlYWRhYmxlO1xuICAgICAgICB0aGlzLm9wdGlvbnMgPSBSZXNvbHZlZE1lc3NhZ2VSZWFkZXJPcHRpb25zLmZyb21PcHRpb25zKG9wdGlvbnMpO1xuICAgICAgICB0aGlzLmJ1ZmZlciA9ICgwLCByYWxfMS5kZWZhdWx0KSgpLm1lc3NhZ2VCdWZmZXIuY3JlYXRlKHRoaXMub3B0aW9ucy5jaGFyc2V0KTtcbiAgICAgICAgdGhpcy5fcGFydGlhbE1lc3NhZ2VUaW1lb3V0ID0gMTAwMDA7XG4gICAgICAgIHRoaXMubmV4dE1lc3NhZ2VMZW5ndGggPSAtMTtcbiAgICAgICAgdGhpcy5tZXNzYWdlVG9rZW4gPSAwO1xuICAgICAgICB0aGlzLnJlYWRTZW1hcGhvcmUgPSBuZXcgc2VtYXBob3JlXzEuU2VtYXBob3JlKDEpO1xuICAgIH1cbiAgICBzZXQgcGFydGlhbE1lc3NhZ2VUaW1lb3V0KHRpbWVvdXQpIHtcbiAgICAgICAgdGhpcy5fcGFydGlhbE1lc3NhZ2VUaW1lb3V0ID0gdGltZW91dDtcbiAgICB9XG4gICAgZ2V0IHBhcnRpYWxNZXNzYWdlVGltZW91dCgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX3BhcnRpYWxNZXNzYWdlVGltZW91dDtcbiAgICB9XG4gICAgbGlzdGVuKGNhbGxiYWNrKSB7XG4gICAgICAgIHRoaXMubmV4dE1lc3NhZ2VMZW5ndGggPSAtMTtcbiAgICAgICAgdGhpcy5tZXNzYWdlVG9rZW4gPSAwO1xuICAgICAgICB0aGlzLnBhcnRpYWxNZXNzYWdlVGltZXIgPSB1bmRlZmluZWQ7XG4gICAgICAgIHRoaXMuY2FsbGJhY2sgPSBjYWxsYmFjaztcbiAgICAgICAgY29uc3QgcmVzdWx0ID0gdGhpcy5yZWFkYWJsZS5vbkRhdGEoKGRhdGEpID0+IHtcbiAgICAgICAgICAgIHRoaXMub25EYXRhKGRhdGEpO1xuICAgICAgICB9KTtcbiAgICAgICAgdGhpcy5yZWFkYWJsZS5vbkVycm9yKChlcnJvcikgPT4gdGhpcy5maXJlRXJyb3IoZXJyb3IpKTtcbiAgICAgICAgdGhpcy5yZWFkYWJsZS5vbkNsb3NlKCgpID0+IHRoaXMuZmlyZUNsb3NlKCkpO1xuICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cbiAgICBvbkRhdGEoZGF0YSkge1xuICAgICAgICB0cnkge1xuICAgICAgICAgICAgdGhpcy5idWZmZXIuYXBwZW5kKGRhdGEpO1xuICAgICAgICAgICAgd2hpbGUgKHRydWUpIHtcbiAgICAgICAgICAgICAgICBpZiAodGhpcy5uZXh0TWVzc2FnZUxlbmd0aCA9PT0gLTEpIHtcbiAgICAgICAgICAgICAgICAgICAgY29uc3QgaGVhZGVycyA9IHRoaXMuYnVmZmVyLnRyeVJlYWRIZWFkZXJzKHRydWUpO1xuICAgICAgICAgICAgICAgICAgICBpZiAoIWhlYWRlcnMpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBjb25zdCBjb250ZW50TGVuZ3RoID0gaGVhZGVycy5nZXQoJ2NvbnRlbnQtbGVuZ3RoJyk7XG4gICAgICAgICAgICAgICAgICAgIGlmICghY29udGVudExlbmd0aCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgdGhpcy5maXJlRXJyb3IobmV3IEVycm9yKGBIZWFkZXIgbXVzdCBwcm92aWRlIGEgQ29udGVudC1MZW5ndGggcHJvcGVydHkuXFxuJHtKU09OLnN0cmluZ2lmeShPYmplY3QuZnJvbUVudHJpZXMoaGVhZGVycykpfWApKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBjb25zdCBsZW5ndGggPSBwYXJzZUludChjb250ZW50TGVuZ3RoKTtcbiAgICAgICAgICAgICAgICAgICAgaWYgKGlzTmFOKGxlbmd0aCkpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHRoaXMuZmlyZUVycm9yKG5ldyBFcnJvcihgQ29udGVudC1MZW5ndGggdmFsdWUgbXVzdCBiZSBhIG51bWJlci4gR290ICR7Y29udGVudExlbmd0aH1gKSk7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgdGhpcy5uZXh0TWVzc2FnZUxlbmd0aCA9IGxlbmd0aDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgY29uc3QgYm9keSA9IHRoaXMuYnVmZmVyLnRyeVJlYWRCb2R5KHRoaXMubmV4dE1lc3NhZ2VMZW5ndGgpO1xuICAgICAgICAgICAgICAgIGlmIChib2R5ID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgLyoqIFdlIGhhdmVuJ3QgcmVjZWl2ZWQgdGhlIGZ1bGwgbWVzc2FnZSB5ZXQuICovXG4gICAgICAgICAgICAgICAgICAgIHRoaXMuc2V0UGFydGlhbE1lc3NhZ2VUaW1lcigpO1xuICAgICAgICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIHRoaXMuY2xlYXJQYXJ0aWFsTWVzc2FnZVRpbWVyKCk7XG4gICAgICAgICAgICAgICAgdGhpcy5uZXh0TWVzc2FnZUxlbmd0aCA9IC0xO1xuICAgICAgICAgICAgICAgIC8vIE1ha2Ugc3VyZSB0aGF0IHdlIGNvbnZlcnQgb25lIHJlY2VpdmVkIG1lc3NhZ2UgYWZ0ZXIgdGhlXG4gICAgICAgICAgICAgICAgLy8gb3RoZXIuIE90aGVyd2lzZSBpdCBjb3VsZCBoYXBwZW4gdGhhdCBhIGRlY29kaW5nIG9mIGEgc2Vjb25kXG4gICAgICAgICAgICAgICAgLy8gc21hbGxlciBtZXNzYWdlIGZpbmlzaGVkIGJlZm9yZSB0aGUgZGVjb2Rpbmcgb2YgYSBmaXJzdCBsYXJnZXJcbiAgICAgICAgICAgICAgICAvLyBtZXNzYWdlIGFuZCB0aGVuIHdlIHdvdWxkIGRlbGl2ZXIgdGhlIHNlY29uZCBtZXNzYWdlIGZpcnN0LlxuICAgICAgICAgICAgICAgIHRoaXMucmVhZFNlbWFwaG9yZS5sb2NrKGFzeW5jICgpID0+IHtcbiAgICAgICAgICAgICAgICAgICAgY29uc3QgYnl0ZXMgPSB0aGlzLm9wdGlvbnMuY29udGVudERlY29kZXIgIT09IHVuZGVmaW5lZFxuICAgICAgICAgICAgICAgICAgICAgICAgPyBhd2FpdCB0aGlzLm9wdGlvbnMuY29udGVudERlY29kZXIuZGVjb2RlKGJvZHkpXG4gICAgICAgICAgICAgICAgICAgICAgICA6IGJvZHk7XG4gICAgICAgICAgICAgICAgICAgIGNvbnN0IG1lc3NhZ2UgPSBhd2FpdCB0aGlzLm9wdGlvbnMuY29udGVudFR5cGVEZWNvZGVyLmRlY29kZShieXRlcywgdGhpcy5vcHRpb25zKTtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5jYWxsYmFjayhtZXNzYWdlKTtcbiAgICAgICAgICAgICAgICB9KS5jYXRjaCgoZXJyb3IpID0+IHtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5maXJlRXJyb3IoZXJyb3IpO1xuICAgICAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIGNhdGNoIChlcnJvcikge1xuICAgICAgICAgICAgdGhpcy5maXJlRXJyb3IoZXJyb3IpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGNsZWFyUGFydGlhbE1lc3NhZ2VUaW1lcigpIHtcbiAgICAgICAgaWYgKHRoaXMucGFydGlhbE1lc3NhZ2VUaW1lcikge1xuICAgICAgICAgICAgdGhpcy5wYXJ0aWFsTWVzc2FnZVRpbWVyLmRpc3Bvc2UoKTtcbiAgICAgICAgICAgIHRoaXMucGFydGlhbE1lc3NhZ2VUaW1lciA9IHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgIH1cbiAgICBzZXRQYXJ0aWFsTWVzc2FnZVRpbWVyKCkge1xuICAgICAgICB0aGlzLmNsZWFyUGFydGlhbE1lc3NhZ2VUaW1lcigpO1xuICAgICAgICBpZiAodGhpcy5fcGFydGlhbE1lc3NhZ2VUaW1lb3V0IDw9IDApIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLnBhcnRpYWxNZXNzYWdlVGltZXIgPSAoMCwgcmFsXzEuZGVmYXVsdCkoKS50aW1lci5zZXRUaW1lb3V0KCh0b2tlbiwgdGltZW91dCkgPT4ge1xuICAgICAgICAgICAgdGhpcy5wYXJ0aWFsTWVzc2FnZVRpbWVyID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgaWYgKHRva2VuID09PSB0aGlzLm1lc3NhZ2VUb2tlbikge1xuICAgICAgICAgICAgICAgIHRoaXMuZmlyZVBhcnRpYWxNZXNzYWdlKHsgbWVzc2FnZVRva2VuOiB0b2tlbiwgd2FpdGluZ1RpbWU6IHRpbWVvdXQgfSk7XG4gICAgICAgICAgICAgICAgdGhpcy5zZXRQYXJ0aWFsTWVzc2FnZVRpbWVyKCk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0sIHRoaXMuX3BhcnRpYWxNZXNzYWdlVGltZW91dCwgdGhpcy5tZXNzYWdlVG9rZW4sIHRoaXMuX3BhcnRpYWxNZXNzYWdlVGltZW91dCk7XG4gICAgfVxufVxuZXhwb3J0cy5SZWFkYWJsZVN0cmVhbU1lc3NhZ2VSZWFkZXIgPSBSZWFkYWJsZVN0cmVhbU1lc3NhZ2VSZWFkZXI7XG4iLCAiXCJ1c2Ugc3RyaWN0XCI7XG4vKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJfX2VzTW9kdWxlXCIsIHsgdmFsdWU6IHRydWUgfSk7XG5leHBvcnRzLldyaXRlYWJsZVN0cmVhbU1lc3NhZ2VXcml0ZXIgPSBleHBvcnRzLkFic3RyYWN0TWVzc2FnZVdyaXRlciA9IGV4cG9ydHMuTWVzc2FnZVdyaXRlciA9IHZvaWQgMDtcbmNvbnN0IHJhbF8xID0gcmVxdWlyZShcIi4vcmFsXCIpO1xuY29uc3QgSXMgPSByZXF1aXJlKFwiLi9pc1wiKTtcbmNvbnN0IHNlbWFwaG9yZV8xID0gcmVxdWlyZShcIi4vc2VtYXBob3JlXCIpO1xuY29uc3QgZXZlbnRzXzEgPSByZXF1aXJlKFwiLi9ldmVudHNcIik7XG5jb25zdCBDb250ZW50TGVuZ3RoID0gJ0NvbnRlbnQtTGVuZ3RoOiAnO1xuY29uc3QgQ1JMRiA9ICdcXHJcXG4nO1xudmFyIE1lc3NhZ2VXcml0ZXI7XG4oZnVuY3Rpb24gKE1lc3NhZ2VXcml0ZXIpIHtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBjYW5kaWRhdGUgJiYgSXMuZnVuYyhjYW5kaWRhdGUuZGlzcG9zZSkgJiYgSXMuZnVuYyhjYW5kaWRhdGUub25DbG9zZSkgJiZcbiAgICAgICAgICAgIElzLmZ1bmMoY2FuZGlkYXRlLm9uRXJyb3IpICYmIElzLmZ1bmMoY2FuZGlkYXRlLndyaXRlKTtcbiAgICB9XG4gICAgTWVzc2FnZVdyaXRlci5pcyA9IGlzO1xufSkoTWVzc2FnZVdyaXRlciB8fCAoZXhwb3J0cy5NZXNzYWdlV3JpdGVyID0gTWVzc2FnZVdyaXRlciA9IHt9KSk7XG5jbGFzcyBBYnN0cmFjdE1lc3NhZ2VXcml0ZXIge1xuICAgIGNvbnN0cnVjdG9yKCkge1xuICAgICAgICB0aGlzLmVycm9yRW1pdHRlciA9IG5ldyBldmVudHNfMS5FbWl0dGVyKCk7XG4gICAgICAgIHRoaXMuY2xvc2VFbWl0dGVyID0gbmV3IGV2ZW50c18xLkVtaXR0ZXIoKTtcbiAgICB9XG4gICAgZGlzcG9zZSgpIHtcbiAgICAgICAgdGhpcy5lcnJvckVtaXR0ZXIuZGlzcG9zZSgpO1xuICAgICAgICB0aGlzLmNsb3NlRW1pdHRlci5kaXNwb3NlKCk7XG4gICAgfVxuICAgIGdldCBvbkVycm9yKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5lcnJvckVtaXR0ZXIuZXZlbnQ7XG4gICAgfVxuICAgIGZpcmVFcnJvcihlcnJvciwgbWVzc2FnZSwgY291bnQpIHtcbiAgICAgICAgdGhpcy5lcnJvckVtaXR0ZXIuZmlyZShbdGhpcy5hc0Vycm9yKGVycm9yKSwgbWVzc2FnZSwgY291bnRdKTtcbiAgICB9XG4gICAgZ2V0IG9uQ2xvc2UoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLmNsb3NlRW1pdHRlci5ldmVudDtcbiAgICB9XG4gICAgZmlyZUNsb3NlKCkge1xuICAgICAgICB0aGlzLmNsb3NlRW1pdHRlci5maXJlKHVuZGVmaW5lZCk7XG4gICAgfVxuICAgIGFzRXJyb3IoZXJyb3IpIHtcbiAgICAgICAgaWYgKGVycm9yIGluc3RhbmNlb2YgRXJyb3IpIHtcbiAgICAgICAgICAgIHJldHVybiBlcnJvcjtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHJldHVybiBuZXcgRXJyb3IoYFdyaXRlciByZWNlaXZlZCBlcnJvci4gUmVhc29uOiAke0lzLnN0cmluZyhlcnJvci5tZXNzYWdlKSA/IGVycm9yLm1lc3NhZ2UgOiAndW5rbm93bid9YCk7XG4gICAgICAgIH1cbiAgICB9XG59XG5leHBvcnRzLkFic3RyYWN0TWVzc2FnZVdyaXRlciA9IEFic3RyYWN0TWVzc2FnZVdyaXRlcjtcbnZhciBSZXNvbHZlZE1lc3NhZ2VXcml0ZXJPcHRpb25zO1xuKGZ1bmN0aW9uIChSZXNvbHZlZE1lc3NhZ2VXcml0ZXJPcHRpb25zKSB7XG4gICAgZnVuY3Rpb24gZnJvbU9wdGlvbnMob3B0aW9ucykge1xuICAgICAgICBpZiAob3B0aW9ucyA9PT0gdW5kZWZpbmVkIHx8IHR5cGVvZiBvcHRpb25zID09PSAnc3RyaW5nJykge1xuICAgICAgICAgICAgcmV0dXJuIHsgY2hhcnNldDogb3B0aW9ucyA/PyAndXRmLTgnLCBjb250ZW50VHlwZUVuY29kZXI6ICgwLCByYWxfMS5kZWZhdWx0KSgpLmFwcGxpY2F0aW9uSnNvbi5lbmNvZGVyIH07XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICByZXR1cm4geyBjaGFyc2V0OiBvcHRpb25zLmNoYXJzZXQgPz8gJ3V0Zi04JywgY29udGVudEVuY29kZXI6IG9wdGlvbnMuY29udGVudEVuY29kZXIsIGNvbnRlbnRUeXBlRW5jb2Rlcjogb3B0aW9ucy5jb250ZW50VHlwZUVuY29kZXIgPz8gKDAsIHJhbF8xLmRlZmF1bHQpKCkuYXBwbGljYXRpb25Kc29uLmVuY29kZXIgfTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBSZXNvbHZlZE1lc3NhZ2VXcml0ZXJPcHRpb25zLmZyb21PcHRpb25zID0gZnJvbU9wdGlvbnM7XG59KShSZXNvbHZlZE1lc3NhZ2VXcml0ZXJPcHRpb25zIHx8IChSZXNvbHZlZE1lc3NhZ2VXcml0ZXJPcHRpb25zID0ge30pKTtcbmNsYXNzIFdyaXRlYWJsZVN0cmVhbU1lc3NhZ2VXcml0ZXIgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VXcml0ZXIge1xuICAgIGNvbnN0cnVjdG9yKHdyaXRhYmxlLCBvcHRpb25zKSB7XG4gICAgICAgIHN1cGVyKCk7XG4gICAgICAgIHRoaXMud3JpdGFibGUgPSB3cml0YWJsZTtcbiAgICAgICAgdGhpcy5vcHRpb25zID0gUmVzb2x2ZWRNZXNzYWdlV3JpdGVyT3B0aW9ucy5mcm9tT3B0aW9ucyhvcHRpb25zKTtcbiAgICAgICAgdGhpcy5lcnJvckNvdW50ID0gMDtcbiAgICAgICAgdGhpcy53cml0ZVNlbWFwaG9yZSA9IG5ldyBzZW1hcGhvcmVfMS5TZW1hcGhvcmUoMSk7XG4gICAgICAgIHRoaXMud3JpdGFibGUub25FcnJvcigoZXJyb3IpID0+IHRoaXMuZmlyZUVycm9yKGVycm9yKSk7XG4gICAgICAgIHRoaXMud3JpdGFibGUub25DbG9zZSgoKSA9PiB0aGlzLmZpcmVDbG9zZSgpKTtcbiAgICB9XG4gICAgYXN5bmMgd3JpdGUobXNnKSB7XG4gICAgICAgIHJldHVybiB0aGlzLndyaXRlU2VtYXBob3JlLmxvY2soYXN5bmMgKCkgPT4ge1xuICAgICAgICAgICAgY29uc3QgcGF5bG9hZCA9IHRoaXMub3B0aW9ucy5jb250ZW50VHlwZUVuY29kZXIuZW5jb2RlKG1zZywgdGhpcy5vcHRpb25zKS50aGVuKChidWZmZXIpID0+IHtcbiAgICAgICAgICAgICAgICBpZiAodGhpcy5vcHRpb25zLmNvbnRlbnRFbmNvZGVyICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIHRoaXMub3B0aW9ucy5jb250ZW50RW5jb2Rlci5lbmNvZGUoYnVmZmVyKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiBidWZmZXI7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICByZXR1cm4gcGF5bG9hZC50aGVuKChidWZmZXIpID0+IHtcbiAgICAgICAgICAgICAgICBjb25zdCBoZWFkZXJzID0gW107XG4gICAgICAgICAgICAgICAgaGVhZGVycy5wdXNoKENvbnRlbnRMZW5ndGgsIGJ1ZmZlci5ieXRlTGVuZ3RoLnRvU3RyaW5nKCksIENSTEYpO1xuICAgICAgICAgICAgICAgIGhlYWRlcnMucHVzaChDUkxGKTtcbiAgICAgICAgICAgICAgICByZXR1cm4gdGhpcy5kb1dyaXRlKG1zZywgaGVhZGVycywgYnVmZmVyKTtcbiAgICAgICAgICAgIH0sIChlcnJvcikgPT4ge1xuICAgICAgICAgICAgICAgIHRoaXMuZmlyZUVycm9yKGVycm9yKTtcbiAgICAgICAgICAgICAgICB0aHJvdyBlcnJvcjtcbiAgICAgICAgICAgIH0pO1xuICAgICAgICB9KTtcbiAgICB9XG4gICAgYXN5bmMgZG9Xcml0ZShtc2csIGhlYWRlcnMsIGRhdGEpIHtcbiAgICAgICAgdHJ5IHtcbiAgICAgICAgICAgIGF3YWl0IHRoaXMud3JpdGFibGUud3JpdGUoaGVhZGVycy5qb2luKCcnKSwgJ2FzY2lpJyk7XG4gICAgICAgICAgICByZXR1cm4gdGhpcy53cml0YWJsZS53cml0ZShkYXRhKTtcbiAgICAgICAgfVxuICAgICAgICBjYXRjaCAoZXJyb3IpIHtcbiAgICAgICAgICAgIHRoaXMuaGFuZGxlRXJyb3IoZXJyb3IsIG1zZyk7XG4gICAgICAgICAgICByZXR1cm4gUHJvbWlzZS5yZWplY3QoZXJyb3IpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGhhbmRsZUVycm9yKGVycm9yLCBtc2cpIHtcbiAgICAgICAgdGhpcy5lcnJvckNvdW50Kys7XG4gICAgICAgIHRoaXMuZmlyZUVycm9yKGVycm9yLCBtc2csIHRoaXMuZXJyb3JDb3VudCk7XG4gICAgfVxuICAgIGVuZCgpIHtcbiAgICAgICAgdGhpcy53cml0YWJsZS5lbmQoKTtcbiAgICB9XG59XG5leHBvcnRzLldyaXRlYWJsZVN0cmVhbU1lc3NhZ2VXcml0ZXIgPSBXcml0ZWFibGVTdHJlYW1NZXNzYWdlV3JpdGVyO1xuIiwgIlwidXNlIHN0cmljdFwiO1xuLyotLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbiAqICBDb3B5cmlnaHQgKGMpIE1pY3Jvc29mdCBDb3Jwb3JhdGlvbi4gQWxsIHJpZ2h0cyByZXNlcnZlZC5cbiAqICBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICotLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSovXG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJfX2VzTW9kdWxlXCIsIHsgdmFsdWU6IHRydWUgfSk7XG5leHBvcnRzLkFic3RyYWN0TWVzc2FnZUJ1ZmZlciA9IHZvaWQgMDtcbmNvbnN0IENSID0gMTM7XG5jb25zdCBMRiA9IDEwO1xuY29uc3QgQ1JMRiA9ICdcXHJcXG4nO1xuY2xhc3MgQWJzdHJhY3RNZXNzYWdlQnVmZmVyIHtcbiAgICBjb25zdHJ1Y3RvcihlbmNvZGluZyA9ICd1dGYtOCcpIHtcbiAgICAgICAgdGhpcy5fZW5jb2RpbmcgPSBlbmNvZGluZztcbiAgICAgICAgdGhpcy5fY2h1bmtzID0gW107XG4gICAgICAgIHRoaXMuX3RvdGFsTGVuZ3RoID0gMDtcbiAgICB9XG4gICAgZ2V0IGVuY29kaW5nKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5fZW5jb2Rpbmc7XG4gICAgfVxuICAgIGFwcGVuZChjaHVuaykge1xuICAgICAgICBjb25zdCB0b0FwcGVuZCA9IHR5cGVvZiBjaHVuayA9PT0gJ3N0cmluZycgPyB0aGlzLmZyb21TdHJpbmcoY2h1bmssIHRoaXMuX2VuY29kaW5nKSA6IGNodW5rO1xuICAgICAgICB0aGlzLl9jaHVua3MucHVzaCh0b0FwcGVuZCk7XG4gICAgICAgIHRoaXMuX3RvdGFsTGVuZ3RoICs9IHRvQXBwZW5kLmJ5dGVMZW5ndGg7XG4gICAgfVxuICAgIHRyeVJlYWRIZWFkZXJzKGxvd2VyQ2FzZUtleXMgPSBmYWxzZSkge1xuICAgICAgICBpZiAodGhpcy5fY2h1bmtzLmxlbmd0aCA9PT0gMCkge1xuICAgICAgICAgICAgcmV0dXJuIHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgICAgICBsZXQgc3RhdGUgPSAwO1xuICAgICAgICBsZXQgY2h1bmtJbmRleCA9IDA7XG4gICAgICAgIGxldCBvZmZzZXQgPSAwO1xuICAgICAgICBsZXQgY2h1bmtCeXRlc1JlYWQgPSAwO1xuICAgICAgICByb3c6IHdoaWxlIChjaHVua0luZGV4IDwgdGhpcy5fY2h1bmtzLmxlbmd0aCkge1xuICAgICAgICAgICAgY29uc3QgY2h1bmsgPSB0aGlzLl9jaHVua3NbY2h1bmtJbmRleF07XG4gICAgICAgICAgICBvZmZzZXQgPSAwO1xuICAgICAgICAgICAgY29sdW1uOiB3aGlsZSAob2Zmc2V0IDwgY2h1bmsubGVuZ3RoKSB7XG4gICAgICAgICAgICAgICAgY29uc3QgdmFsdWUgPSBjaHVua1tvZmZzZXRdO1xuICAgICAgICAgICAgICAgIHN3aXRjaCAodmFsdWUpIHtcbiAgICAgICAgICAgICAgICAgICAgY2FzZSBDUjpcbiAgICAgICAgICAgICAgICAgICAgICAgIHN3aXRjaCAoc3RhdGUpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBjYXNlIDA6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHN0YXRlID0gMTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgY2FzZSAyOlxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBzdGF0ZSA9IDM7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGRlZmF1bHQ6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHN0YXRlID0gMDtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICBjYXNlIExGOlxuICAgICAgICAgICAgICAgICAgICAgICAgc3dpdGNoIChzdGF0ZSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNhc2UgMTpcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgc3RhdGUgPSAyO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBjYXNlIDM6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHN0YXRlID0gNDtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgb2Zmc2V0Kys7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrIHJvdztcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBkZWZhdWx0OlxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBzdGF0ZSA9IDA7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICAgICAgZGVmYXVsdDpcbiAgICAgICAgICAgICAgICAgICAgICAgIHN0YXRlID0gMDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgb2Zmc2V0Kys7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBjaHVua0J5dGVzUmVhZCArPSBjaHVuay5ieXRlTGVuZ3RoO1xuICAgICAgICAgICAgY2h1bmtJbmRleCsrO1xuICAgICAgICB9XG4gICAgICAgIGlmIChzdGF0ZSAhPT0gNCkge1xuICAgICAgICAgICAgcmV0dXJuIHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgICAgICAvLyBUaGUgYnVmZmVyIGNvbnRhaW5zIHRoZSB0d28gQ1JMRiBhdCB0aGUgZW5kLiBTbyB3ZSB3aWxsXG4gICAgICAgIC8vIGhhdmUgdHdvIGVtcHR5IGxpbmVzIGFmdGVyIHRoZSBzcGxpdCBhdCB0aGUgZW5kIGFzIHdlbGwuXG4gICAgICAgIGNvbnN0IGJ1ZmZlciA9IHRoaXMuX3JlYWQoY2h1bmtCeXRlc1JlYWQgKyBvZmZzZXQpO1xuICAgICAgICBjb25zdCByZXN1bHQgPSBuZXcgTWFwKCk7XG4gICAgICAgIGNvbnN0IGhlYWRlcnMgPSB0aGlzLnRvU3RyaW5nKGJ1ZmZlciwgJ2FzY2lpJykuc3BsaXQoQ1JMRik7XG4gICAgICAgIGlmIChoZWFkZXJzLmxlbmd0aCA8IDIpIHtcbiAgICAgICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgICAgIH1cbiAgICAgICAgZm9yIChsZXQgaSA9IDA7IGkgPCBoZWFkZXJzLmxlbmd0aCAtIDI7IGkrKykge1xuICAgICAgICAgICAgY29uc3QgaGVhZGVyID0gaGVhZGVyc1tpXTtcbiAgICAgICAgICAgIGNvbnN0IGluZGV4ID0gaGVhZGVyLmluZGV4T2YoJzonKTtcbiAgICAgICAgICAgIGlmIChpbmRleCA9PT0gLTEpIHtcbiAgICAgICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYE1lc3NhZ2UgaGVhZGVyIG11c3Qgc2VwYXJhdGUga2V5IGFuZCB2YWx1ZSB1c2luZyAnOidcXG4ke2hlYWRlcn1gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNvbnN0IGtleSA9IGhlYWRlci5zdWJzdHIoMCwgaW5kZXgpO1xuICAgICAgICAgICAgY29uc3QgdmFsdWUgPSBoZWFkZXIuc3Vic3RyKGluZGV4ICsgMSkudHJpbSgpO1xuICAgICAgICAgICAgcmVzdWx0LnNldChsb3dlckNhc2VLZXlzID8ga2V5LnRvTG93ZXJDYXNlKCkgOiBrZXksIHZhbHVlKTtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cbiAgICB0cnlSZWFkQm9keShsZW5ndGgpIHtcbiAgICAgICAgaWYgKHRoaXMuX3RvdGFsTGVuZ3RoIDwgbGVuZ3RoKSB7XG4gICAgICAgICAgICByZXR1cm4gdW5kZWZpbmVkO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiB0aGlzLl9yZWFkKGxlbmd0aCk7XG4gICAgfVxuICAgIGdldCBudW1iZXJPZkJ5dGVzKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5fdG90YWxMZW5ndGg7XG4gICAgfVxuICAgIF9yZWFkKGJ5dGVDb3VudCkge1xuICAgICAgICBpZiAoYnl0ZUNvdW50ID09PSAwKSB7XG4gICAgICAgICAgICByZXR1cm4gdGhpcy5lbXB0eUJ1ZmZlcigpO1xuICAgICAgICB9XG4gICAgICAgIGlmIChieXRlQ291bnQgPiB0aGlzLl90b3RhbExlbmd0aCkge1xuICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBDYW5ub3QgcmVhZCBzbyBtYW55IGJ5dGVzIWApO1xuICAgICAgICB9XG4gICAgICAgIGlmICh0aGlzLl9jaHVua3NbMF0uYnl0ZUxlbmd0aCA9PT0gYnl0ZUNvdW50KSB7XG4gICAgICAgICAgICAvLyBzdXBlciBmYXN0IHBhdGgsIHByZWNpc2VseSBmaXJzdCBjaHVuayBtdXN0IGJlIHJldHVybmVkXG4gICAgICAgICAgICBjb25zdCBjaHVuayA9IHRoaXMuX2NodW5rc1swXTtcbiAgICAgICAgICAgIHRoaXMuX2NodW5rcy5zaGlmdCgpO1xuICAgICAgICAgICAgdGhpcy5fdG90YWxMZW5ndGggLT0gYnl0ZUNvdW50O1xuICAgICAgICAgICAgcmV0dXJuIHRoaXMuYXNOYXRpdmUoY2h1bmspO1xuICAgICAgICB9XG4gICAgICAgIGlmICh0aGlzLl9jaHVua3NbMF0uYnl0ZUxlbmd0aCA+IGJ5dGVDb3VudCkge1xuICAgICAgICAgICAgLy8gZmFzdCBwYXRoLCB0aGUgcmVhZGluZyBpcyBlbnRpcmVseSB3aXRoaW4gdGhlIGZpcnN0IGNodW5rXG4gICAgICAgICAgICBjb25zdCBjaHVuayA9IHRoaXMuX2NodW5rc1swXTtcbiAgICAgICAgICAgIGNvbnN0IHJlc3VsdCA9IHRoaXMuYXNOYXRpdmUoY2h1bmssIGJ5dGVDb3VudCk7XG4gICAgICAgICAgICB0aGlzLl9jaHVua3NbMF0gPSBjaHVuay5zbGljZShieXRlQ291bnQpO1xuICAgICAgICAgICAgdGhpcy5fdG90YWxMZW5ndGggLT0gYnl0ZUNvdW50O1xuICAgICAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICAgICAgfVxuICAgICAgICBjb25zdCByZXN1bHQgPSB0aGlzLmFsbG9jTmF0aXZlKGJ5dGVDb3VudCk7XG4gICAgICAgIGxldCByZXN1bHRPZmZzZXQgPSAwO1xuICAgICAgICBsZXQgY2h1bmtJbmRleCA9IDA7XG4gICAgICAgIHdoaWxlIChieXRlQ291bnQgPiAwKSB7XG4gICAgICAgICAgICBjb25zdCBjaHVuayA9IHRoaXMuX2NodW5rc1tjaHVua0luZGV4XTtcbiAgICAgICAgICAgIGlmIChjaHVuay5ieXRlTGVuZ3RoID4gYnl0ZUNvdW50KSB7XG4gICAgICAgICAgICAgICAgLy8gdGhpcyBjaHVuayB3aWxsIHN1cnZpdmVcbiAgICAgICAgICAgICAgICBjb25zdCBjaHVua1BhcnQgPSBjaHVuay5zbGljZSgwLCBieXRlQ291bnQpO1xuICAgICAgICAgICAgICAgIHJlc3VsdC5zZXQoY2h1bmtQYXJ0LCByZXN1bHRPZmZzZXQpO1xuICAgICAgICAgICAgICAgIHJlc3VsdE9mZnNldCArPSBieXRlQ291bnQ7XG4gICAgICAgICAgICAgICAgdGhpcy5fY2h1bmtzW2NodW5rSW5kZXhdID0gY2h1bmsuc2xpY2UoYnl0ZUNvdW50KTtcbiAgICAgICAgICAgICAgICB0aGlzLl90b3RhbExlbmd0aCAtPSBieXRlQ291bnQ7XG4gICAgICAgICAgICAgICAgYnl0ZUNvdW50IC09IGJ5dGVDb3VudDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIC8vIHRoaXMgY2h1bmsgd2lsbCBiZSBlbnRpcmVseSByZWFkXG4gICAgICAgICAgICAgICAgcmVzdWx0LnNldChjaHVuaywgcmVzdWx0T2Zmc2V0KTtcbiAgICAgICAgICAgICAgICByZXN1bHRPZmZzZXQgKz0gY2h1bmsuYnl0ZUxlbmd0aDtcbiAgICAgICAgICAgICAgICB0aGlzLl9jaHVua3Muc2hpZnQoKTtcbiAgICAgICAgICAgICAgICB0aGlzLl90b3RhbExlbmd0aCAtPSBjaHVuay5ieXRlTGVuZ3RoO1xuICAgICAgICAgICAgICAgIGJ5dGVDb3VudCAtPSBjaHVuay5ieXRlTGVuZ3RoO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxufVxuZXhwb3J0cy5BYnN0cmFjdE1lc3NhZ2VCdWZmZXIgPSBBYnN0cmFjdE1lc3NhZ2VCdWZmZXI7XG4iLCAiXCJ1c2Ugc3RyaWN0XCI7XG4vKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJfX2VzTW9kdWxlXCIsIHsgdmFsdWU6IHRydWUgfSk7XG5leHBvcnRzLmNyZWF0ZU1lc3NhZ2VDb25uZWN0aW9uID0gZXhwb3J0cy5Db25uZWN0aW9uT3B0aW9ucyA9IGV4cG9ydHMuTWVzc2FnZVN0cmF0ZWd5ID0gZXhwb3J0cy5DYW5jZWxsYXRpb25TdHJhdGVneSA9IGV4cG9ydHMuQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3kgPSBleHBvcnRzLkNhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kgPSBleHBvcnRzLlJlcXVlc3RDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5ID0gZXhwb3J0cy5JZENhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kgPSBleHBvcnRzLkNvbm5lY3Rpb25TdHJhdGVneSA9IGV4cG9ydHMuQ29ubmVjdGlvbkVycm9yID0gZXhwb3J0cy5Db25uZWN0aW9uRXJyb3JzID0gZXhwb3J0cy5Mb2dUcmFjZU5vdGlmaWNhdGlvbiA9IGV4cG9ydHMuU2V0VHJhY2VOb3RpZmljYXRpb24gPSBleHBvcnRzLlRyYWNlRm9ybWF0ID0gZXhwb3J0cy5UcmFjZVZhbHVlcyA9IGV4cG9ydHMuVHJhY2UgPSBleHBvcnRzLk51bGxMb2dnZXIgPSBleHBvcnRzLlByb2dyZXNzVHlwZSA9IGV4cG9ydHMuUHJvZ3Jlc3NUb2tlbiA9IHZvaWQgMDtcbmNvbnN0IHJhbF8xID0gcmVxdWlyZShcIi4vcmFsXCIpO1xuY29uc3QgSXMgPSByZXF1aXJlKFwiLi9pc1wiKTtcbmNvbnN0IG1lc3NhZ2VzXzEgPSByZXF1aXJlKFwiLi9tZXNzYWdlc1wiKTtcbmNvbnN0IGxpbmtlZE1hcF8xID0gcmVxdWlyZShcIi4vbGlua2VkTWFwXCIpO1xuY29uc3QgZXZlbnRzXzEgPSByZXF1aXJlKFwiLi9ldmVudHNcIik7XG5jb25zdCBjYW5jZWxsYXRpb25fMSA9IHJlcXVpcmUoXCIuL2NhbmNlbGxhdGlvblwiKTtcbnZhciBDYW5jZWxOb3RpZmljYXRpb247XG4oZnVuY3Rpb24gKENhbmNlbE5vdGlmaWNhdGlvbikge1xuICAgIENhbmNlbE5vdGlmaWNhdGlvbi50eXBlID0gbmV3IG1lc3NhZ2VzXzEuTm90aWZpY2F0aW9uVHlwZSgnJC9jYW5jZWxSZXF1ZXN0Jyk7XG59KShDYW5jZWxOb3RpZmljYXRpb24gfHwgKENhbmNlbE5vdGlmaWNhdGlvbiA9IHt9KSk7XG52YXIgUHJvZ3Jlc3NUb2tlbjtcbihmdW5jdGlvbiAoUHJvZ3Jlc3NUb2tlbikge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiB0eXBlb2YgdmFsdWUgPT09ICdzdHJpbmcnIHx8IHR5cGVvZiB2YWx1ZSA9PT0gJ251bWJlcic7XG4gICAgfVxuICAgIFByb2dyZXNzVG9rZW4uaXMgPSBpcztcbn0pKFByb2dyZXNzVG9rZW4gfHwgKGV4cG9ydHMuUHJvZ3Jlc3NUb2tlbiA9IFByb2dyZXNzVG9rZW4gPSB7fSkpO1xudmFyIFByb2dyZXNzTm90aWZpY2F0aW9uO1xuKGZ1bmN0aW9uIChQcm9ncmVzc05vdGlmaWNhdGlvbikge1xuICAgIFByb2dyZXNzTm90aWZpY2F0aW9uLnR5cGUgPSBuZXcgbWVzc2FnZXNfMS5Ob3RpZmljYXRpb25UeXBlKCckL3Byb2dyZXNzJyk7XG59KShQcm9ncmVzc05vdGlmaWNhdGlvbiB8fCAoUHJvZ3Jlc3NOb3RpZmljYXRpb24gPSB7fSkpO1xuY2xhc3MgUHJvZ3Jlc3NUeXBlIHtcbiAgICBjb25zdHJ1Y3RvcigpIHtcbiAgICB9XG59XG5leHBvcnRzLlByb2dyZXNzVHlwZSA9IFByb2dyZXNzVHlwZTtcbnZhciBTdGFyUmVxdWVzdEhhbmRsZXI7XG4oZnVuY3Rpb24gKFN0YXJSZXF1ZXN0SGFuZGxlcikge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiBJcy5mdW5jKHZhbHVlKTtcbiAgICB9XG4gICAgU3RhclJlcXVlc3RIYW5kbGVyLmlzID0gaXM7XG59KShTdGFyUmVxdWVzdEhhbmRsZXIgfHwgKFN0YXJSZXF1ZXN0SGFuZGxlciA9IHt9KSk7XG5leHBvcnRzLk51bGxMb2dnZXIgPSBPYmplY3QuZnJlZXplKHtcbiAgICBlcnJvcjogKCkgPT4geyB9LFxuICAgIHdhcm46ICgpID0+IHsgfSxcbiAgICBpbmZvOiAoKSA9PiB7IH0sXG4gICAgbG9nOiAoKSA9PiB7IH1cbn0pO1xudmFyIFRyYWNlO1xuKGZ1bmN0aW9uIChUcmFjZSkge1xuICAgIFRyYWNlW1RyYWNlW1wiT2ZmXCJdID0gMF0gPSBcIk9mZlwiO1xuICAgIFRyYWNlW1RyYWNlW1wiTWVzc2FnZXNcIl0gPSAxXSA9IFwiTWVzc2FnZXNcIjtcbiAgICBUcmFjZVtUcmFjZVtcIkNvbXBhY3RcIl0gPSAyXSA9IFwiQ29tcGFjdFwiO1xuICAgIFRyYWNlW1RyYWNlW1wiVmVyYm9zZVwiXSA9IDNdID0gXCJWZXJib3NlXCI7XG59KShUcmFjZSB8fCAoZXhwb3J0cy5UcmFjZSA9IFRyYWNlID0ge30pKTtcbnZhciBUcmFjZVZhbHVlcztcbihmdW5jdGlvbiAoVHJhY2VWYWx1ZXMpIHtcbiAgICAvKipcbiAgICAgKiBUdXJuIHRyYWNpbmcgb2ZmLlxuICAgICAqL1xuICAgIFRyYWNlVmFsdWVzLk9mZiA9ICdvZmYnO1xuICAgIC8qKlxuICAgICAqIFRyYWNlIG1lc3NhZ2VzIG9ubHkuXG4gICAgICovXG4gICAgVHJhY2VWYWx1ZXMuTWVzc2FnZXMgPSAnbWVzc2FnZXMnO1xuICAgIC8qKlxuICAgICAqIENvbXBhY3QgbWVzc2FnZSB0cmFjaW5nLlxuICAgICAqL1xuICAgIFRyYWNlVmFsdWVzLkNvbXBhY3QgPSAnY29tcGFjdCc7XG4gICAgLyoqXG4gICAgICogVmVyYm9zZSBtZXNzYWdlIHRyYWNpbmcuXG4gICAgICovXG4gICAgVHJhY2VWYWx1ZXMuVmVyYm9zZSA9ICd2ZXJib3NlJztcbn0pKFRyYWNlVmFsdWVzIHx8IChleHBvcnRzLlRyYWNlVmFsdWVzID0gVHJhY2VWYWx1ZXMgPSB7fSkpO1xuKGZ1bmN0aW9uIChUcmFjZSkge1xuICAgIGZ1bmN0aW9uIGZyb21TdHJpbmcodmFsdWUpIHtcbiAgICAgICAgaWYgKCFJcy5zdHJpbmcodmFsdWUpKSB7XG4gICAgICAgICAgICByZXR1cm4gVHJhY2UuT2ZmO1xuICAgICAgICB9XG4gICAgICAgIHZhbHVlID0gdmFsdWUudG9Mb3dlckNhc2UoKTtcbiAgICAgICAgc3dpdGNoICh2YWx1ZSkge1xuICAgICAgICAgICAgY2FzZSAnb2ZmJzpcbiAgICAgICAgICAgICAgICByZXR1cm4gVHJhY2UuT2ZmO1xuICAgICAgICAgICAgY2FzZSAnbWVzc2FnZXMnOlxuICAgICAgICAgICAgICAgIHJldHVybiBUcmFjZS5NZXNzYWdlcztcbiAgICAgICAgICAgIGNhc2UgJ2NvbXBhY3QnOlxuICAgICAgICAgICAgICAgIHJldHVybiBUcmFjZS5Db21wYWN0O1xuICAgICAgICAgICAgY2FzZSAndmVyYm9zZSc6XG4gICAgICAgICAgICAgICAgcmV0dXJuIFRyYWNlLlZlcmJvc2U7XG4gICAgICAgICAgICBkZWZhdWx0OlxuICAgICAgICAgICAgICAgIHJldHVybiBUcmFjZS5PZmY7XG4gICAgICAgIH1cbiAgICB9XG4gICAgVHJhY2UuZnJvbVN0cmluZyA9IGZyb21TdHJpbmc7XG4gICAgZnVuY3Rpb24gdG9TdHJpbmcodmFsdWUpIHtcbiAgICAgICAgc3dpdGNoICh2YWx1ZSkge1xuICAgICAgICAgICAgY2FzZSBUcmFjZS5PZmY6XG4gICAgICAgICAgICAgICAgcmV0dXJuICdvZmYnO1xuICAgICAgICAgICAgY2FzZSBUcmFjZS5NZXNzYWdlczpcbiAgICAgICAgICAgICAgICByZXR1cm4gJ21lc3NhZ2VzJztcbiAgICAgICAgICAgIGNhc2UgVHJhY2UuQ29tcGFjdDpcbiAgICAgICAgICAgICAgICByZXR1cm4gJ2NvbXBhY3QnO1xuICAgICAgICAgICAgY2FzZSBUcmFjZS5WZXJib3NlOlxuICAgICAgICAgICAgICAgIHJldHVybiAndmVyYm9zZSc7XG4gICAgICAgICAgICBkZWZhdWx0OlxuICAgICAgICAgICAgICAgIHJldHVybiAnb2ZmJztcbiAgICAgICAgfVxuICAgIH1cbiAgICBUcmFjZS50b1N0cmluZyA9IHRvU3RyaW5nO1xufSkoVHJhY2UgfHwgKGV4cG9ydHMuVHJhY2UgPSBUcmFjZSA9IHt9KSk7XG52YXIgVHJhY2VGb3JtYXQ7XG4oZnVuY3Rpb24gKFRyYWNlRm9ybWF0KSB7XG4gICAgVHJhY2VGb3JtYXRbXCJUZXh0XCJdID0gXCJ0ZXh0XCI7XG4gICAgVHJhY2VGb3JtYXRbXCJKU09OXCJdID0gXCJqc29uXCI7XG59KShUcmFjZUZvcm1hdCB8fCAoZXhwb3J0cy5UcmFjZUZvcm1hdCA9IFRyYWNlRm9ybWF0ID0ge30pKTtcbihmdW5jdGlvbiAoVHJhY2VGb3JtYXQpIHtcbiAgICBmdW5jdGlvbiBmcm9tU3RyaW5nKHZhbHVlKSB7XG4gICAgICAgIGlmICghSXMuc3RyaW5nKHZhbHVlKSkge1xuICAgICAgICAgICAgcmV0dXJuIFRyYWNlRm9ybWF0LlRleHQ7XG4gICAgICAgIH1cbiAgICAgICAgdmFsdWUgPSB2YWx1ZS50b0xvd2VyQ2FzZSgpO1xuICAgICAgICBpZiAodmFsdWUgPT09ICdqc29uJykge1xuICAgICAgICAgICAgcmV0dXJuIFRyYWNlRm9ybWF0LkpTT047XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICByZXR1cm4gVHJhY2VGb3JtYXQuVGV4dDtcbiAgICAgICAgfVxuICAgIH1cbiAgICBUcmFjZUZvcm1hdC5mcm9tU3RyaW5nID0gZnJvbVN0cmluZztcbn0pKFRyYWNlRm9ybWF0IHx8IChleHBvcnRzLlRyYWNlRm9ybWF0ID0gVHJhY2VGb3JtYXQgPSB7fSkpO1xudmFyIFNldFRyYWNlTm90aWZpY2F0aW9uO1xuKGZ1bmN0aW9uIChTZXRUcmFjZU5vdGlmaWNhdGlvbikge1xuICAgIFNldFRyYWNlTm90aWZpY2F0aW9uLnR5cGUgPSBuZXcgbWVzc2FnZXNfMS5Ob3RpZmljYXRpb25UeXBlKCckL3NldFRyYWNlJyk7XG59KShTZXRUcmFjZU5vdGlmaWNhdGlvbiB8fCAoZXhwb3J0cy5TZXRUcmFjZU5vdGlmaWNhdGlvbiA9IFNldFRyYWNlTm90aWZpY2F0aW9uID0ge30pKTtcbnZhciBMb2dUcmFjZU5vdGlmaWNhdGlvbjtcbihmdW5jdGlvbiAoTG9nVHJhY2VOb3RpZmljYXRpb24pIHtcbiAgICBMb2dUcmFjZU5vdGlmaWNhdGlvbi50eXBlID0gbmV3IG1lc3NhZ2VzXzEuTm90aWZpY2F0aW9uVHlwZSgnJC9sb2dUcmFjZScpO1xufSkoTG9nVHJhY2VOb3RpZmljYXRpb24gfHwgKGV4cG9ydHMuTG9nVHJhY2VOb3RpZmljYXRpb24gPSBMb2dUcmFjZU5vdGlmaWNhdGlvbiA9IHt9KSk7XG52YXIgQ29ubmVjdGlvbkVycm9ycztcbihmdW5jdGlvbiAoQ29ubmVjdGlvbkVycm9ycykge1xuICAgIC8qKlxuICAgICAqIFRoZSBjb25uZWN0aW9uIGlzIGNsb3NlZC5cbiAgICAgKi9cbiAgICBDb25uZWN0aW9uRXJyb3JzW0Nvbm5lY3Rpb25FcnJvcnNbXCJDbG9zZWRcIl0gPSAxXSA9IFwiQ2xvc2VkXCI7XG4gICAgLyoqXG4gICAgICogVGhlIGNvbm5lY3Rpb24gZ290IGRpc3Bvc2VkLlxuICAgICAqL1xuICAgIENvbm5lY3Rpb25FcnJvcnNbQ29ubmVjdGlvbkVycm9yc1tcIkRpc3Bvc2VkXCJdID0gMl0gPSBcIkRpc3Bvc2VkXCI7XG4gICAgLyoqXG4gICAgICogVGhlIGNvbm5lY3Rpb24gaXMgYWxyZWFkeSBpbiBsaXN0ZW5pbmcgbW9kZS5cbiAgICAgKi9cbiAgICBDb25uZWN0aW9uRXJyb3JzW0Nvbm5lY3Rpb25FcnJvcnNbXCJBbHJlYWR5TGlzdGVuaW5nXCJdID0gM10gPSBcIkFscmVhZHlMaXN0ZW5pbmdcIjtcbn0pKENvbm5lY3Rpb25FcnJvcnMgfHwgKGV4cG9ydHMuQ29ubmVjdGlvbkVycm9ycyA9IENvbm5lY3Rpb25FcnJvcnMgPSB7fSkpO1xuY2xhc3MgQ29ubmVjdGlvbkVycm9yIGV4dGVuZHMgRXJyb3Ige1xuICAgIGNvbnN0cnVjdG9yKGNvZGUsIG1lc3NhZ2UpIHtcbiAgICAgICAgc3VwZXIobWVzc2FnZSk7XG4gICAgICAgIHRoaXMuY29kZSA9IGNvZGU7XG4gICAgICAgIE9iamVjdC5zZXRQcm90b3R5cGVPZih0aGlzLCBDb25uZWN0aW9uRXJyb3IucHJvdG90eXBlKTtcbiAgICB9XG59XG5leHBvcnRzLkNvbm5lY3Rpb25FcnJvciA9IENvbm5lY3Rpb25FcnJvcjtcbnZhciBDb25uZWN0aW9uU3RyYXRlZ3k7XG4oZnVuY3Rpb24gKENvbm5lY3Rpb25TdHJhdGVneSkge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICYmIElzLmZ1bmMoY2FuZGlkYXRlLmNhbmNlbFVuZGlzcGF0Y2hlZCk7XG4gICAgfVxuICAgIENvbm5lY3Rpb25TdHJhdGVneS5pcyA9IGlzO1xufSkoQ29ubmVjdGlvblN0cmF0ZWd5IHx8IChleHBvcnRzLkNvbm5lY3Rpb25TdHJhdGVneSA9IENvbm5lY3Rpb25TdHJhdGVneSA9IHt9KSk7XG52YXIgSWRDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5O1xuKGZ1bmN0aW9uIChJZENhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kpIHtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiAoY2FuZGlkYXRlLmtpbmQgPT09IHVuZGVmaW5lZCB8fCBjYW5kaWRhdGUua2luZCA9PT0gJ2lkJykgJiYgSXMuZnVuYyhjYW5kaWRhdGUuY3JlYXRlQ2FuY2VsbGF0aW9uVG9rZW5Tb3VyY2UpICYmIChjYW5kaWRhdGUuZGlzcG9zZSA9PT0gdW5kZWZpbmVkIHx8IElzLmZ1bmMoY2FuZGlkYXRlLmRpc3Bvc2UpKTtcbiAgICB9XG4gICAgSWRDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5LmlzID0gaXM7XG59KShJZENhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kgfHwgKGV4cG9ydHMuSWRDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5ID0gSWRDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5ID0ge30pKTtcbnZhciBSZXF1ZXN0Q2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneTtcbihmdW5jdGlvbiAoUmVxdWVzdENhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kpIHtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiBjYW5kaWRhdGUua2luZCA9PT0gJ3JlcXVlc3QnICYmIElzLmZ1bmMoY2FuZGlkYXRlLmNyZWF0ZUNhbmNlbGxhdGlvblRva2VuU291cmNlKSAmJiAoY2FuZGlkYXRlLmRpc3Bvc2UgPT09IHVuZGVmaW5lZCB8fCBJcy5mdW5jKGNhbmRpZGF0ZS5kaXNwb3NlKSk7XG4gICAgfVxuICAgIFJlcXVlc3RDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5LmlzID0gaXM7XG59KShSZXF1ZXN0Q2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneSB8fCAoZXhwb3J0cy5SZXF1ZXN0Q2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneSA9IFJlcXVlc3RDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5ID0ge30pKTtcbnZhciBDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5O1xuKGZ1bmN0aW9uIChDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5KSB7XG4gICAgQ2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneS5NZXNzYWdlID0gT2JqZWN0LmZyZWV6ZSh7XG4gICAgICAgIGNyZWF0ZUNhbmNlbGxhdGlvblRva2VuU291cmNlKF8pIHtcbiAgICAgICAgICAgIHJldHVybiBuZXcgY2FuY2VsbGF0aW9uXzEuQ2FuY2VsbGF0aW9uVG9rZW5Tb3VyY2UoKTtcbiAgICAgICAgfVxuICAgIH0pO1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiBJZENhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kuaXModmFsdWUpIHx8IFJlcXVlc3RDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5LmlzKHZhbHVlKTtcbiAgICB9XG4gICAgQ2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneS5pcyA9IGlzO1xufSkoQ2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneSB8fCAoZXhwb3J0cy5DYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5ID0gQ2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneSA9IHt9KSk7XG52YXIgQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3k7XG4oZnVuY3Rpb24gKENhbmNlbGxhdGlvblNlbmRlclN0cmF0ZWd5KSB7XG4gICAgQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3kuTWVzc2FnZSA9IE9iamVjdC5mcmVlemUoe1xuICAgICAgICBzZW5kQ2FuY2VsbGF0aW9uKGNvbm4sIGlkKSB7XG4gICAgICAgICAgICByZXR1cm4gY29ubi5zZW5kTm90aWZpY2F0aW9uKENhbmNlbE5vdGlmaWNhdGlvbi50eXBlLCB7IGlkIH0pO1xuICAgICAgICB9LFxuICAgICAgICBjbGVhbnVwKF8pIHsgfVxuICAgIH0pO1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICYmIElzLmZ1bmMoY2FuZGlkYXRlLnNlbmRDYW5jZWxsYXRpb24pICYmIElzLmZ1bmMoY2FuZGlkYXRlLmNsZWFudXApO1xuICAgIH1cbiAgICBDYW5jZWxsYXRpb25TZW5kZXJTdHJhdGVneS5pcyA9IGlzO1xufSkoQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3kgfHwgKGV4cG9ydHMuQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3kgPSBDYW5jZWxsYXRpb25TZW5kZXJTdHJhdGVneSA9IHt9KSk7XG52YXIgQ2FuY2VsbGF0aW9uU3RyYXRlZ3k7XG4oZnVuY3Rpb24gKENhbmNlbGxhdGlvblN0cmF0ZWd5KSB7XG4gICAgQ2FuY2VsbGF0aW9uU3RyYXRlZ3kuTWVzc2FnZSA9IE9iamVjdC5mcmVlemUoe1xuICAgICAgICByZWNlaXZlcjogQ2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneS5NZXNzYWdlLFxuICAgICAgICBzZW5kZXI6IENhbmNlbGxhdGlvblNlbmRlclN0cmF0ZWd5Lk1lc3NhZ2VcbiAgICB9KTtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiBDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5LmlzKGNhbmRpZGF0ZS5yZWNlaXZlcikgJiYgQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3kuaXMoY2FuZGlkYXRlLnNlbmRlcik7XG4gICAgfVxuICAgIENhbmNlbGxhdGlvblN0cmF0ZWd5LmlzID0gaXM7XG59KShDYW5jZWxsYXRpb25TdHJhdGVneSB8fCAoZXhwb3J0cy5DYW5jZWxsYXRpb25TdHJhdGVneSA9IENhbmNlbGxhdGlvblN0cmF0ZWd5ID0ge30pKTtcbnZhciBNZXNzYWdlU3RyYXRlZ3k7XG4oZnVuY3Rpb24gKE1lc3NhZ2VTdHJhdGVneSkge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICYmIElzLmZ1bmMoY2FuZGlkYXRlLmhhbmRsZU1lc3NhZ2UpO1xuICAgIH1cbiAgICBNZXNzYWdlU3RyYXRlZ3kuaXMgPSBpcztcbn0pKE1lc3NhZ2VTdHJhdGVneSB8fCAoZXhwb3J0cy5NZXNzYWdlU3RyYXRlZ3kgPSBNZXNzYWdlU3RyYXRlZ3kgPSB7fSkpO1xudmFyIENvbm5lY3Rpb25PcHRpb25zO1xuKGZ1bmN0aW9uIChDb25uZWN0aW9uT3B0aW9ucykge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICYmIChDYW5jZWxsYXRpb25TdHJhdGVneS5pcyhjYW5kaWRhdGUuY2FuY2VsbGF0aW9uU3RyYXRlZ3kpIHx8IENvbm5lY3Rpb25TdHJhdGVneS5pcyhjYW5kaWRhdGUuY29ubmVjdGlvblN0cmF0ZWd5KSB8fCBNZXNzYWdlU3RyYXRlZ3kuaXMoY2FuZGlkYXRlLm1lc3NhZ2VTdHJhdGVneSkpO1xuICAgIH1cbiAgICBDb25uZWN0aW9uT3B0aW9ucy5pcyA9IGlzO1xufSkoQ29ubmVjdGlvbk9wdGlvbnMgfHwgKGV4cG9ydHMuQ29ubmVjdGlvbk9wdGlvbnMgPSBDb25uZWN0aW9uT3B0aW9ucyA9IHt9KSk7XG52YXIgQ29ubmVjdGlvblN0YXRlO1xuKGZ1bmN0aW9uIChDb25uZWN0aW9uU3RhdGUpIHtcbiAgICBDb25uZWN0aW9uU3RhdGVbQ29ubmVjdGlvblN0YXRlW1wiTmV3XCJdID0gMV0gPSBcIk5ld1wiO1xuICAgIENvbm5lY3Rpb25TdGF0ZVtDb25uZWN0aW9uU3RhdGVbXCJMaXN0ZW5pbmdcIl0gPSAyXSA9IFwiTGlzdGVuaW5nXCI7XG4gICAgQ29ubmVjdGlvblN0YXRlW0Nvbm5lY3Rpb25TdGF0ZVtcIkNsb3NlZFwiXSA9IDNdID0gXCJDbG9zZWRcIjtcbiAgICBDb25uZWN0aW9uU3RhdGVbQ29ubmVjdGlvblN0YXRlW1wiRGlzcG9zZWRcIl0gPSA0XSA9IFwiRGlzcG9zZWRcIjtcbn0pKENvbm5lY3Rpb25TdGF0ZSB8fCAoQ29ubmVjdGlvblN0YXRlID0ge30pKTtcbmZ1bmN0aW9uIGNyZWF0ZU1lc3NhZ2VDb25uZWN0aW9uKG1lc3NhZ2VSZWFkZXIsIG1lc3NhZ2VXcml0ZXIsIF9sb2dnZXIsIG9wdGlvbnMpIHtcbiAgICBjb25zdCBsb2dnZXIgPSBfbG9nZ2VyICE9PSB1bmRlZmluZWQgPyBfbG9nZ2VyIDogZXhwb3J0cy5OdWxsTG9nZ2VyO1xuICAgIGxldCBzZXF1ZW5jZU51bWJlciA9IDA7XG4gICAgbGV0IG5vdGlmaWNhdGlvblNlcXVlbmNlTnVtYmVyID0gMDtcbiAgICBsZXQgdW5rbm93blJlc3BvbnNlU2VxdWVuY2VOdW1iZXIgPSAwO1xuICAgIGNvbnN0IHZlcnNpb24gPSAnMi4wJztcbiAgICBsZXQgc3RhclJlcXVlc3RIYW5kbGVyID0gdW5kZWZpbmVkO1xuICAgIGNvbnN0IHJlcXVlc3RIYW5kbGVycyA9IG5ldyBNYXAoKTtcbiAgICBsZXQgc3Rhck5vdGlmaWNhdGlvbkhhbmRsZXIgPSB1bmRlZmluZWQ7XG4gICAgY29uc3Qgbm90aWZpY2F0aW9uSGFuZGxlcnMgPSBuZXcgTWFwKCk7XG4gICAgY29uc3QgcHJvZ3Jlc3NIYW5kbGVycyA9IG5ldyBNYXAoKTtcbiAgICBsZXQgdGltZXI7XG4gICAgbGV0IG1lc3NhZ2VRdWV1ZSA9IG5ldyBsaW5rZWRNYXBfMS5MaW5rZWRNYXAoKTtcbiAgICBsZXQgcmVzcG9uc2VQcm9taXNlcyA9IG5ldyBNYXAoKTtcbiAgICBsZXQga25vd25DYW5jZWxlZFJlcXVlc3RzID0gbmV3IFNldCgpO1xuICAgIGxldCByZXF1ZXN0VG9rZW5zID0gbmV3IE1hcCgpO1xuICAgIGxldCB0cmFjZSA9IFRyYWNlLk9mZjtcbiAgICBsZXQgdHJhY2VGb3JtYXQgPSBUcmFjZUZvcm1hdC5UZXh0O1xuICAgIGxldCB0cmFjZXI7XG4gICAgbGV0IHN0YXRlID0gQ29ubmVjdGlvblN0YXRlLk5ldztcbiAgICBjb25zdCBlcnJvckVtaXR0ZXIgPSBuZXcgZXZlbnRzXzEuRW1pdHRlcigpO1xuICAgIGNvbnN0IGNsb3NlRW1pdHRlciA9IG5ldyBldmVudHNfMS5FbWl0dGVyKCk7XG4gICAgY29uc3QgdW5oYW5kbGVkTm90aWZpY2F0aW9uRW1pdHRlciA9IG5ldyBldmVudHNfMS5FbWl0dGVyKCk7XG4gICAgY29uc3QgdW5oYW5kbGVkUHJvZ3Jlc3NFbWl0dGVyID0gbmV3IGV2ZW50c18xLkVtaXR0ZXIoKTtcbiAgICBjb25zdCBkaXNwb3NlRW1pdHRlciA9IG5ldyBldmVudHNfMS5FbWl0dGVyKCk7XG4gICAgY29uc3QgY2FuY2VsbGF0aW9uU3RyYXRlZ3kgPSAob3B0aW9ucyAmJiBvcHRpb25zLmNhbmNlbGxhdGlvblN0cmF0ZWd5KSA/IG9wdGlvbnMuY2FuY2VsbGF0aW9uU3RyYXRlZ3kgOiBDYW5jZWxsYXRpb25TdHJhdGVneS5NZXNzYWdlO1xuICAgIGZ1bmN0aW9uIGNyZWF0ZVJlcXVlc3RRdWV1ZUtleShpZCkge1xuICAgICAgICBpZiAoaWQgPT09IG51bGwpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgQ2FuJ3Qgc2VuZCByZXF1ZXN0cyB3aXRoIGlkIG51bGwgc2luY2UgdGhlIHJlc3BvbnNlIGNhbid0IGJlIGNvcnJlbGF0ZWQuYCk7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuICdyZXEtJyArIGlkLnRvU3RyaW5nKCk7XG4gICAgfVxuICAgIGZ1bmN0aW9uIGNyZWF0ZVJlc3BvbnNlUXVldWVLZXkoaWQpIHtcbiAgICAgICAgaWYgKGlkID09PSBudWxsKSB7XG4gICAgICAgICAgICByZXR1cm4gJ3Jlcy11bmtub3duLScgKyAoKyt1bmtub3duUmVzcG9uc2VTZXF1ZW5jZU51bWJlcikudG9TdHJpbmcoKTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHJldHVybiAncmVzLScgKyBpZC50b1N0cmluZygpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIGNyZWF0ZU5vdGlmaWNhdGlvblF1ZXVlS2V5KCkge1xuICAgICAgICByZXR1cm4gJ25vdC0nICsgKCsrbm90aWZpY2F0aW9uU2VxdWVuY2VOdW1iZXIpLnRvU3RyaW5nKCk7XG4gICAgfVxuICAgIGZ1bmN0aW9uIGFkZE1lc3NhZ2VUb1F1ZXVlKHF1ZXVlLCBtZXNzYWdlKSB7XG4gICAgICAgIGlmIChtZXNzYWdlc18xLk1lc3NhZ2UuaXNSZXF1ZXN0KG1lc3NhZ2UpKSB7XG4gICAgICAgICAgICBxdWV1ZS5zZXQoY3JlYXRlUmVxdWVzdFF1ZXVlS2V5KG1lc3NhZ2UuaWQpLCBtZXNzYWdlKTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIGlmIChtZXNzYWdlc18xLk1lc3NhZ2UuaXNSZXNwb25zZShtZXNzYWdlKSkge1xuICAgICAgICAgICAgcXVldWUuc2V0KGNyZWF0ZVJlc3BvbnNlUXVldWVLZXkobWVzc2FnZS5pZCksIG1lc3NhZ2UpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgcXVldWUuc2V0KGNyZWF0ZU5vdGlmaWNhdGlvblF1ZXVlS2V5KCksIG1lc3NhZ2UpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIGNhbmNlbFVuZGlzcGF0Y2hlZChfbWVzc2FnZSkge1xuICAgICAgICByZXR1cm4gdW5kZWZpbmVkO1xuICAgIH1cbiAgICBmdW5jdGlvbiBpc0xpc3RlbmluZygpIHtcbiAgICAgICAgcmV0dXJuIHN0YXRlID09PSBDb25uZWN0aW9uU3RhdGUuTGlzdGVuaW5nO1xuICAgIH1cbiAgICBmdW5jdGlvbiBpc0Nsb3NlZCgpIHtcbiAgICAgICAgcmV0dXJuIHN0YXRlID09PSBDb25uZWN0aW9uU3RhdGUuQ2xvc2VkO1xuICAgIH1cbiAgICBmdW5jdGlvbiBpc0Rpc3Bvc2VkKCkge1xuICAgICAgICByZXR1cm4gc3RhdGUgPT09IENvbm5lY3Rpb25TdGF0ZS5EaXNwb3NlZDtcbiAgICB9XG4gICAgZnVuY3Rpb24gY2xvc2VIYW5kbGVyKCkge1xuICAgICAgICBpZiAoc3RhdGUgPT09IENvbm5lY3Rpb25TdGF0ZS5OZXcgfHwgc3RhdGUgPT09IENvbm5lY3Rpb25TdGF0ZS5MaXN0ZW5pbmcpIHtcbiAgICAgICAgICAgIHN0YXRlID0gQ29ubmVjdGlvblN0YXRlLkNsb3NlZDtcbiAgICAgICAgICAgIGNsb3NlRW1pdHRlci5maXJlKHVuZGVmaW5lZCk7XG4gICAgICAgIH1cbiAgICAgICAgLy8gSWYgdGhlIGNvbm5lY3Rpb24gaXMgZGlzcG9zZWQgZG9uJ3Qgc2VudCBjbG9zZSBldmVudHMuXG4gICAgfVxuICAgIGZ1bmN0aW9uIHJlYWRFcnJvckhhbmRsZXIoZXJyb3IpIHtcbiAgICAgICAgZXJyb3JFbWl0dGVyLmZpcmUoW2Vycm9yLCB1bmRlZmluZWQsIHVuZGVmaW5lZF0pO1xuICAgIH1cbiAgICBmdW5jdGlvbiB3cml0ZUVycm9ySGFuZGxlcihkYXRhKSB7XG4gICAgICAgIGVycm9yRW1pdHRlci5maXJlKGRhdGEpO1xuICAgIH1cbiAgICBtZXNzYWdlUmVhZGVyLm9uQ2xvc2UoY2xvc2VIYW5kbGVyKTtcbiAgICBtZXNzYWdlUmVhZGVyLm9uRXJyb3IocmVhZEVycm9ySGFuZGxlcik7XG4gICAgbWVzc2FnZVdyaXRlci5vbkNsb3NlKGNsb3NlSGFuZGxlcik7XG4gICAgbWVzc2FnZVdyaXRlci5vbkVycm9yKHdyaXRlRXJyb3JIYW5kbGVyKTtcbiAgICBmdW5jdGlvbiB0cmlnZ2VyTWVzc2FnZVF1ZXVlKCkge1xuICAgICAgICBpZiAodGltZXIgfHwgbWVzc2FnZVF1ZXVlLnNpemUgPT09IDApIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICB0aW1lciA9ICgwLCByYWxfMS5kZWZhdWx0KSgpLnRpbWVyLnNldEltbWVkaWF0ZSgoKSA9PiB7XG4gICAgICAgICAgICB0aW1lciA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIHByb2Nlc3NNZXNzYWdlUXVldWUoKTtcbiAgICAgICAgfSk7XG4gICAgfVxuICAgIGZ1bmN0aW9uIGhhbmRsZU1lc3NhZ2UobWVzc2FnZSkge1xuICAgICAgICBpZiAobWVzc2FnZXNfMS5NZXNzYWdlLmlzUmVxdWVzdChtZXNzYWdlKSkge1xuICAgICAgICAgICAgaGFuZGxlUmVxdWVzdChtZXNzYWdlKTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIGlmIChtZXNzYWdlc18xLk1lc3NhZ2UuaXNOb3RpZmljYXRpb24obWVzc2FnZSkpIHtcbiAgICAgICAgICAgIGhhbmRsZU5vdGlmaWNhdGlvbihtZXNzYWdlKTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIGlmIChtZXNzYWdlc18xLk1lc3NhZ2UuaXNSZXNwb25zZShtZXNzYWdlKSkge1xuICAgICAgICAgICAgaGFuZGxlUmVzcG9uc2UobWVzc2FnZSk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBoYW5kbGVJbnZhbGlkTWVzc2FnZShtZXNzYWdlKTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBmdW5jdGlvbiBwcm9jZXNzTWVzc2FnZVF1ZXVlKCkge1xuICAgICAgICBpZiAobWVzc2FnZVF1ZXVlLnNpemUgPT09IDApIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBjb25zdCBtZXNzYWdlID0gbWVzc2FnZVF1ZXVlLnNoaWZ0KCk7XG4gICAgICAgIHRyeSB7XG4gICAgICAgICAgICBjb25zdCBtZXNzYWdlU3RyYXRlZ3kgPSBvcHRpb25zPy5tZXNzYWdlU3RyYXRlZ3k7XG4gICAgICAgICAgICBpZiAoTWVzc2FnZVN0cmF0ZWd5LmlzKG1lc3NhZ2VTdHJhdGVneSkpIHtcbiAgICAgICAgICAgICAgICBtZXNzYWdlU3RyYXRlZ3kuaGFuZGxlTWVzc2FnZShtZXNzYWdlLCBoYW5kbGVNZXNzYWdlKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIGhhbmRsZU1lc3NhZ2UobWVzc2FnZSk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgZmluYWxseSB7XG4gICAgICAgICAgICB0cmlnZ2VyTWVzc2FnZVF1ZXVlKCk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgY29uc3QgY2FsbGJhY2sgPSAobWVzc2FnZSkgPT4ge1xuICAgICAgICB0cnkge1xuICAgICAgICAgICAgLy8gV2UgaGF2ZSByZWNlaXZlZCBhIGNhbmNlbGxhdGlvbiBtZXNzYWdlLiBDaGVjayBpZiB0aGUgbWVzc2FnZSBpcyBzdGlsbCBpbiB0aGUgcXVldWVcbiAgICAgICAgICAgIC8vIGFuZCBjYW5jZWwgaXQgaWYgYWxsb3dlZCB0byBkbyBzby5cbiAgICAgICAgICAgIGlmIChtZXNzYWdlc18xLk1lc3NhZ2UuaXNOb3RpZmljYXRpb24obWVzc2FnZSkgJiYgbWVzc2FnZS5tZXRob2QgPT09IENhbmNlbE5vdGlmaWNhdGlvbi50eXBlLm1ldGhvZCkge1xuICAgICAgICAgICAgICAgIGNvbnN0IGNhbmNlbElkID0gbWVzc2FnZS5wYXJhbXMuaWQ7XG4gICAgICAgICAgICAgICAgY29uc3Qga2V5ID0gY3JlYXRlUmVxdWVzdFF1ZXVlS2V5KGNhbmNlbElkKTtcbiAgICAgICAgICAgICAgICBjb25zdCB0b0NhbmNlbCA9IG1lc3NhZ2VRdWV1ZS5nZXQoa2V5KTtcbiAgICAgICAgICAgICAgICBpZiAobWVzc2FnZXNfMS5NZXNzYWdlLmlzUmVxdWVzdCh0b0NhbmNlbCkpIHtcbiAgICAgICAgICAgICAgICAgICAgY29uc3Qgc3RyYXRlZ3kgPSBvcHRpb25zPy5jb25uZWN0aW9uU3RyYXRlZ3k7XG4gICAgICAgICAgICAgICAgICAgIGNvbnN0IHJlc3BvbnNlID0gKHN0cmF0ZWd5ICYmIHN0cmF0ZWd5LmNhbmNlbFVuZGlzcGF0Y2hlZCkgPyBzdHJhdGVneS5jYW5jZWxVbmRpc3BhdGNoZWQodG9DYW5jZWwsIGNhbmNlbFVuZGlzcGF0Y2hlZCkgOiBjYW5jZWxVbmRpc3BhdGNoZWQodG9DYW5jZWwpO1xuICAgICAgICAgICAgICAgICAgICBpZiAocmVzcG9uc2UgJiYgKHJlc3BvbnNlLmVycm9yICE9PSB1bmRlZmluZWQgfHwgcmVzcG9uc2UucmVzdWx0ICE9PSB1bmRlZmluZWQpKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBtZXNzYWdlUXVldWUuZGVsZXRlKGtleSk7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXF1ZXN0VG9rZW5zLmRlbGV0ZShjYW5jZWxJZCk7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXNwb25zZS5pZCA9IHRvQ2FuY2VsLmlkO1xuICAgICAgICAgICAgICAgICAgICAgICAgdHJhY2VTZW5kaW5nUmVzcG9uc2UocmVzcG9uc2UsIG1lc3NhZ2UubWV0aG9kLCBEYXRlLm5vdygpKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIG1lc3NhZ2VXcml0ZXIud3JpdGUocmVzcG9uc2UpLmNhdGNoKCgpID0+IGxvZ2dlci5lcnJvcihgU2VuZGluZyByZXNwb25zZSBmb3IgY2FuY2VsZWQgbWVzc2FnZSBmYWlsZWQuYCkpO1xuICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGNvbnN0IGNhbmNlbGxhdGlvblRva2VuID0gcmVxdWVzdFRva2Vucy5nZXQoY2FuY2VsSWQpO1xuICAgICAgICAgICAgICAgIC8vIFRoZSByZXF1ZXN0IGlzIGFscmVhZHkgcnVubmluZy4gQ2FuY2VsIHRoZSB0b2tlblxuICAgICAgICAgICAgICAgIGlmIChjYW5jZWxsYXRpb25Ub2tlbiAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICAgICAgICAgIGNhbmNlbGxhdGlvblRva2VuLmNhbmNlbCgpO1xuICAgICAgICAgICAgICAgICAgICB0cmFjZVJlY2VpdmVkTm90aWZpY2F0aW9uKG1lc3NhZ2UpO1xuICAgICAgICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAvLyBSZW1lbWJlciB0aGUgY2FuY2VsIGJ1dCBzdGlsbCBxdWV1ZSB0aGUgbWVzc2FnZSB0b1xuICAgICAgICAgICAgICAgICAgICAvLyBjbGVhbiB1cCBzdGF0ZSBpbiBwcm9jZXNzIG1lc3NhZ2UuXG4gICAgICAgICAgICAgICAgICAgIGtub3duQ2FuY2VsZWRSZXF1ZXN0cy5hZGQoY2FuY2VsSWQpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGFkZE1lc3NhZ2VUb1F1ZXVlKG1lc3NhZ2VRdWV1ZSwgbWVzc2FnZSk7XG4gICAgICAgIH1cbiAgICAgICAgZmluYWxseSB7XG4gICAgICAgICAgICB0cmlnZ2VyTWVzc2FnZVF1ZXVlKCk7XG4gICAgICAgIH1cbiAgICB9O1xuICAgIGZ1bmN0aW9uIGhhbmRsZVJlcXVlc3QocmVxdWVzdE1lc3NhZ2UpIHtcbiAgICAgICAgaWYgKGlzRGlzcG9zZWQoKSkge1xuICAgICAgICAgICAgLy8gd2UgcmV0dXJuIGhlcmUgc2lsZW50bHkgc2luY2Ugd2UgZmlyZWQgYW4gZXZlbnQgd2hlbiB0aGVcbiAgICAgICAgICAgIC8vIGNvbm5lY3Rpb24gZ290IGRpc3Bvc2VkLlxuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGZ1bmN0aW9uIHJlcGx5KHJlc3VsdE9yRXJyb3IsIG1ldGhvZCwgc3RhcnRUaW1lKSB7XG4gICAgICAgICAgICBjb25zdCBtZXNzYWdlID0ge1xuICAgICAgICAgICAgICAgIGpzb25ycGM6IHZlcnNpb24sXG4gICAgICAgICAgICAgICAgaWQ6IHJlcXVlc3RNZXNzYWdlLmlkXG4gICAgICAgICAgICB9O1xuICAgICAgICAgICAgaWYgKHJlc3VsdE9yRXJyb3IgaW5zdGFuY2VvZiBtZXNzYWdlc18xLlJlc3BvbnNlRXJyb3IpIHtcbiAgICAgICAgICAgICAgICBtZXNzYWdlLmVycm9yID0gcmVzdWx0T3JFcnJvci50b0pzb24oKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIG1lc3NhZ2UucmVzdWx0ID0gcmVzdWx0T3JFcnJvciA9PT0gdW5kZWZpbmVkID8gbnVsbCA6IHJlc3VsdE9yRXJyb3I7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICB0cmFjZVNlbmRpbmdSZXNwb25zZShtZXNzYWdlLCBtZXRob2QsIHN0YXJ0VGltZSk7XG4gICAgICAgICAgICBtZXNzYWdlV3JpdGVyLndyaXRlKG1lc3NhZ2UpLmNhdGNoKCgpID0+IGxvZ2dlci5lcnJvcihgU2VuZGluZyByZXNwb25zZSBmYWlsZWQuYCkpO1xuICAgICAgICB9XG4gICAgICAgIGZ1bmN0aW9uIHJlcGx5RXJyb3IoZXJyb3IsIG1ldGhvZCwgc3RhcnRUaW1lKSB7XG4gICAgICAgICAgICBjb25zdCBtZXNzYWdlID0ge1xuICAgICAgICAgICAgICAgIGpzb25ycGM6IHZlcnNpb24sXG4gICAgICAgICAgICAgICAgaWQ6IHJlcXVlc3RNZXNzYWdlLmlkLFxuICAgICAgICAgICAgICAgIGVycm9yOiBlcnJvci50b0pzb24oKVxuICAgICAgICAgICAgfTtcbiAgICAgICAgICAgIHRyYWNlU2VuZGluZ1Jlc3BvbnNlKG1lc3NhZ2UsIG1ldGhvZCwgc3RhcnRUaW1lKTtcbiAgICAgICAgICAgIG1lc3NhZ2VXcml0ZXIud3JpdGUobWVzc2FnZSkuY2F0Y2goKCkgPT4gbG9nZ2VyLmVycm9yKGBTZW5kaW5nIHJlc3BvbnNlIGZhaWxlZC5gKSk7XG4gICAgICAgIH1cbiAgICAgICAgZnVuY3Rpb24gcmVwbHlTdWNjZXNzKHJlc3VsdCwgbWV0aG9kLCBzdGFydFRpbWUpIHtcbiAgICAgICAgICAgIC8vIFRoZSBKU09OIFJQQyBkZWZpbmVzIHRoYXQgYSByZXNwb25zZSBtdXN0IGVpdGhlciBoYXZlIGEgcmVzdWx0IG9yIGFuIGVycm9yXG4gICAgICAgICAgICAvLyBTbyB3ZSBjYW4ndCB0cmVhdCB1bmRlZmluZWQgYXMgYSB2YWxpZCByZXNwb25zZSByZXN1bHQuXG4gICAgICAgICAgICBpZiAocmVzdWx0ID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICByZXN1bHQgPSBudWxsO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgY29uc3QgbWVzc2FnZSA9IHtcbiAgICAgICAgICAgICAgICBqc29ucnBjOiB2ZXJzaW9uLFxuICAgICAgICAgICAgICAgIGlkOiByZXF1ZXN0TWVzc2FnZS5pZCxcbiAgICAgICAgICAgICAgICByZXN1bHQ6IHJlc3VsdFxuICAgICAgICAgICAgfTtcbiAgICAgICAgICAgIHRyYWNlU2VuZGluZ1Jlc3BvbnNlKG1lc3NhZ2UsIG1ldGhvZCwgc3RhcnRUaW1lKTtcbiAgICAgICAgICAgIG1lc3NhZ2VXcml0ZXIud3JpdGUobWVzc2FnZSkuY2F0Y2goKCkgPT4gbG9nZ2VyLmVycm9yKGBTZW5kaW5nIHJlc3BvbnNlIGZhaWxlZC5gKSk7XG4gICAgICAgIH1cbiAgICAgICAgdHJhY2VSZWNlaXZlZFJlcXVlc3QocmVxdWVzdE1lc3NhZ2UpO1xuICAgICAgICBjb25zdCBlbGVtZW50ID0gcmVxdWVzdEhhbmRsZXJzLmdldChyZXF1ZXN0TWVzc2FnZS5tZXRob2QpO1xuICAgICAgICBsZXQgdHlwZTtcbiAgICAgICAgbGV0IHJlcXVlc3RIYW5kbGVyO1xuICAgICAgICBpZiAoZWxlbWVudCkge1xuICAgICAgICAgICAgdHlwZSA9IGVsZW1lbnQudHlwZTtcbiAgICAgICAgICAgIHJlcXVlc3RIYW5kbGVyID0gZWxlbWVudC5oYW5kbGVyO1xuICAgICAgICB9XG4gICAgICAgIGNvbnN0IHN0YXJ0VGltZSA9IERhdGUubm93KCk7XG4gICAgICAgIGlmIChyZXF1ZXN0SGFuZGxlciB8fCBzdGFyUmVxdWVzdEhhbmRsZXIpIHtcbiAgICAgICAgICAgIGNvbnN0IHRva2VuS2V5ID0gcmVxdWVzdE1lc3NhZ2UuaWQgPz8gU3RyaW5nKERhdGUubm93KCkpOyAvL1xuICAgICAgICAgICAgY29uc3QgY2FuY2VsbGF0aW9uU291cmNlID0gSWRDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5LmlzKGNhbmNlbGxhdGlvblN0cmF0ZWd5LnJlY2VpdmVyKVxuICAgICAgICAgICAgICAgID8gY2FuY2VsbGF0aW9uU3RyYXRlZ3kucmVjZWl2ZXIuY3JlYXRlQ2FuY2VsbGF0aW9uVG9rZW5Tb3VyY2UodG9rZW5LZXkpXG4gICAgICAgICAgICAgICAgOiBjYW5jZWxsYXRpb25TdHJhdGVneS5yZWNlaXZlci5jcmVhdGVDYW5jZWxsYXRpb25Ub2tlblNvdXJjZShyZXF1ZXN0TWVzc2FnZSk7XG4gICAgICAgICAgICBpZiAocmVxdWVzdE1lc3NhZ2UuaWQgIT09IG51bGwgJiYga25vd25DYW5jZWxlZFJlcXVlc3RzLmhhcyhyZXF1ZXN0TWVzc2FnZS5pZCkpIHtcbiAgICAgICAgICAgICAgICBjYW5jZWxsYXRpb25Tb3VyY2UuY2FuY2VsKCk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAocmVxdWVzdE1lc3NhZ2UuaWQgIT09IG51bGwpIHtcbiAgICAgICAgICAgICAgICByZXF1ZXN0VG9rZW5zLnNldCh0b2tlbktleSwgY2FuY2VsbGF0aW9uU291cmNlKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHRyeSB7XG4gICAgICAgICAgICAgICAgbGV0IGhhbmRsZXJSZXN1bHQ7XG4gICAgICAgICAgICAgICAgaWYgKHJlcXVlc3RIYW5kbGVyKSB7XG4gICAgICAgICAgICAgICAgICAgIGlmIChyZXF1ZXN0TWVzc2FnZS5wYXJhbXMgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKHR5cGUgIT09IHVuZGVmaW5lZCAmJiB0eXBlLm51bWJlck9mUGFyYW1zICE9PSAwKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgcmVwbHlFcnJvcihuZXcgbWVzc2FnZXNfMS5SZXNwb25zZUVycm9yKG1lc3NhZ2VzXzEuRXJyb3JDb2Rlcy5JbnZhbGlkUGFyYW1zLCBgUmVxdWVzdCAke3JlcXVlc3RNZXNzYWdlLm1ldGhvZH0gZGVmaW5lcyAke3R5cGUubnVtYmVyT2ZQYXJhbXN9IHBhcmFtcyBidXQgcmVjZWl2ZWQgbm9uZS5gKSwgcmVxdWVzdE1lc3NhZ2UubWV0aG9kLCBzdGFydFRpbWUpO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIGhhbmRsZXJSZXN1bHQgPSByZXF1ZXN0SGFuZGxlcihjYW5jZWxsYXRpb25Tb3VyY2UudG9rZW4pO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2UgaWYgKEFycmF5LmlzQXJyYXkocmVxdWVzdE1lc3NhZ2UucGFyYW1zKSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKHR5cGUgIT09IHVuZGVmaW5lZCAmJiB0eXBlLnBhcmFtZXRlclN0cnVjdHVyZXMgPT09IG1lc3NhZ2VzXzEuUGFyYW1ldGVyU3RydWN0dXJlcy5ieU5hbWUpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICByZXBseUVycm9yKG5ldyBtZXNzYWdlc18xLlJlc3BvbnNlRXJyb3IobWVzc2FnZXNfMS5FcnJvckNvZGVzLkludmFsaWRQYXJhbXMsIGBSZXF1ZXN0ICR7cmVxdWVzdE1lc3NhZ2UubWV0aG9kfSBkZWZpbmVzIHBhcmFtZXRlcnMgYnkgbmFtZSBidXQgcmVjZWl2ZWQgcGFyYW1ldGVycyBieSBwb3NpdGlvbmApLCByZXF1ZXN0TWVzc2FnZS5tZXRob2QsIHN0YXJ0VGltZSk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgaGFuZGxlclJlc3VsdCA9IHJlcXVlc3RIYW5kbGVyKC4uLnJlcXVlc3RNZXNzYWdlLnBhcmFtcywgY2FuY2VsbGF0aW9uU291cmNlLnRva2VuKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICh0eXBlICE9PSB1bmRlZmluZWQgJiYgdHlwZS5wYXJhbWV0ZXJTdHJ1Y3R1cmVzID09PSBtZXNzYWdlc18xLlBhcmFtZXRlclN0cnVjdHVyZXMuYnlQb3NpdGlvbikge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJlcGx5RXJyb3IobmV3IG1lc3NhZ2VzXzEuUmVzcG9uc2VFcnJvcihtZXNzYWdlc18xLkVycm9yQ29kZXMuSW52YWxpZFBhcmFtcywgYFJlcXVlc3QgJHtyZXF1ZXN0TWVzc2FnZS5tZXRob2R9IGRlZmluZXMgcGFyYW1ldGVycyBieSBwb3NpdGlvbiBidXQgcmVjZWl2ZWQgcGFyYW1ldGVycyBieSBuYW1lYCksIHJlcXVlc3RNZXNzYWdlLm1ldGhvZCwgc3RhcnRUaW1lKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBoYW5kbGVyUmVzdWx0ID0gcmVxdWVzdEhhbmRsZXIocmVxdWVzdE1lc3NhZ2UucGFyYW1zLCBjYW5jZWxsYXRpb25Tb3VyY2UudG9rZW4pO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2UgaWYgKHN0YXJSZXF1ZXN0SGFuZGxlcikge1xuICAgICAgICAgICAgICAgICAgICBoYW5kbGVyUmVzdWx0ID0gc3RhclJlcXVlc3RIYW5kbGVyKHJlcXVlc3RNZXNzYWdlLm1ldGhvZCwgcmVxdWVzdE1lc3NhZ2UucGFyYW1zLCBjYW5jZWxsYXRpb25Tb3VyY2UudG9rZW4pO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBjb25zdCBwcm9taXNlID0gaGFuZGxlclJlc3VsdDtcbiAgICAgICAgICAgICAgICBpZiAoIWhhbmRsZXJSZXN1bHQpIHtcbiAgICAgICAgICAgICAgICAgICAgcmVxdWVzdFRva2Vucy5kZWxldGUodG9rZW5LZXkpO1xuICAgICAgICAgICAgICAgICAgICByZXBseVN1Y2Nlc3MoaGFuZGxlclJlc3VsdCwgcmVxdWVzdE1lc3NhZ2UubWV0aG9kLCBzdGFydFRpbWUpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIGlmIChwcm9taXNlLnRoZW4pIHtcbiAgICAgICAgICAgICAgICAgICAgcHJvbWlzZS50aGVuKChyZXN1bHRPckVycm9yKSA9PiB7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXF1ZXN0VG9rZW5zLmRlbGV0ZSh0b2tlbktleSk7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXBseShyZXN1bHRPckVycm9yLCByZXF1ZXN0TWVzc2FnZS5tZXRob2QsIHN0YXJ0VGltZSk7XG4gICAgICAgICAgICAgICAgICAgIH0sIGVycm9yID0+IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJlcXVlc3RUb2tlbnMuZGVsZXRlKHRva2VuS2V5KTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmIChlcnJvciBpbnN0YW5jZW9mIG1lc3NhZ2VzXzEuUmVzcG9uc2VFcnJvcikge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJlcGx5RXJyb3IoZXJyb3IsIHJlcXVlc3RNZXNzYWdlLm1ldGhvZCwgc3RhcnRUaW1lKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIGVsc2UgaWYgKGVycm9yICYmIElzLnN0cmluZyhlcnJvci5tZXNzYWdlKSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJlcGx5RXJyb3IobmV3IG1lc3NhZ2VzXzEuUmVzcG9uc2VFcnJvcihtZXNzYWdlc18xLkVycm9yQ29kZXMuSW50ZXJuYWxFcnJvciwgYFJlcXVlc3QgJHtyZXF1ZXN0TWVzc2FnZS5tZXRob2R9IGZhaWxlZCB3aXRoIG1lc3NhZ2U6ICR7ZXJyb3IubWVzc2FnZX1gKSwgcmVxdWVzdE1lc3NhZ2UubWV0aG9kLCBzdGFydFRpbWUpO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgcmVwbHlFcnJvcihuZXcgbWVzc2FnZXNfMS5SZXNwb25zZUVycm9yKG1lc3NhZ2VzXzEuRXJyb3JDb2Rlcy5JbnRlcm5hbEVycm9yLCBgUmVxdWVzdCAke3JlcXVlc3RNZXNzYWdlLm1ldGhvZH0gZmFpbGVkIHVuZXhwZWN0ZWRseSB3aXRob3V0IHByb3ZpZGluZyBhbnkgZGV0YWlscy5gKSwgcmVxdWVzdE1lc3NhZ2UubWV0aG9kLCBzdGFydFRpbWUpO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB9KTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIHJlcXVlc3RUb2tlbnMuZGVsZXRlKHRva2VuS2V5KTtcbiAgICAgICAgICAgICAgICAgICAgcmVwbHkoaGFuZGxlclJlc3VsdCwgcmVxdWVzdE1lc3NhZ2UubWV0aG9kLCBzdGFydFRpbWUpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNhdGNoIChlcnJvcikge1xuICAgICAgICAgICAgICAgIHJlcXVlc3RUb2tlbnMuZGVsZXRlKHRva2VuS2V5KTtcbiAgICAgICAgICAgICAgICBpZiAoZXJyb3IgaW5zdGFuY2VvZiBtZXNzYWdlc18xLlJlc3BvbnNlRXJyb3IpIHtcbiAgICAgICAgICAgICAgICAgICAgcmVwbHkoZXJyb3IsIHJlcXVlc3RNZXNzYWdlLm1ldGhvZCwgc3RhcnRUaW1lKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSBpZiAoZXJyb3IgJiYgSXMuc3RyaW5nKGVycm9yLm1lc3NhZ2UpKSB7XG4gICAgICAgICAgICAgICAgICAgIHJlcGx5RXJyb3IobmV3IG1lc3NhZ2VzXzEuUmVzcG9uc2VFcnJvcihtZXNzYWdlc18xLkVycm9yQ29kZXMuSW50ZXJuYWxFcnJvciwgYFJlcXVlc3QgJHtyZXF1ZXN0TWVzc2FnZS5tZXRob2R9IGZhaWxlZCB3aXRoIG1lc3NhZ2U6ICR7ZXJyb3IubWVzc2FnZX1gKSwgcmVxdWVzdE1lc3NhZ2UubWV0aG9kLCBzdGFydFRpbWUpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgcmVwbHlFcnJvcihuZXcgbWVzc2FnZXNfMS5SZXNwb25zZUVycm9yKG1lc3NhZ2VzXzEuRXJyb3JDb2Rlcy5JbnRlcm5hbEVycm9yLCBgUmVxdWVzdCAke3JlcXVlc3RNZXNzYWdlLm1ldGhvZH0gZmFpbGVkIHVuZXhwZWN0ZWRseSB3aXRob3V0IHByb3ZpZGluZyBhbnkgZGV0YWlscy5gKSwgcmVxdWVzdE1lc3NhZ2UubWV0aG9kLCBzdGFydFRpbWUpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHJlcGx5RXJyb3IobmV3IG1lc3NhZ2VzXzEuUmVzcG9uc2VFcnJvcihtZXNzYWdlc18xLkVycm9yQ29kZXMuTWV0aG9kTm90Rm91bmQsIGBVbmhhbmRsZWQgbWV0aG9kICR7cmVxdWVzdE1lc3NhZ2UubWV0aG9kfWApLCByZXF1ZXN0TWVzc2FnZS5tZXRob2QsIHN0YXJ0VGltZSk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgZnVuY3Rpb24gaGFuZGxlUmVzcG9uc2UocmVzcG9uc2VNZXNzYWdlKSB7XG4gICAgICAgIGlmIChpc0Rpc3Bvc2VkKCkpIHtcbiAgICAgICAgICAgIC8vIFNlZSBoYW5kbGUgcmVxdWVzdC5cbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBpZiAocmVzcG9uc2VNZXNzYWdlLmlkID09PSBudWxsKSB7XG4gICAgICAgICAgICBpZiAocmVzcG9uc2VNZXNzYWdlLmVycm9yKSB7XG4gICAgICAgICAgICAgICAgbG9nZ2VyLmVycm9yKGBSZWNlaXZlZCByZXNwb25zZSBtZXNzYWdlIHdpdGhvdXQgaWQ6IEVycm9yIGlzOiBcXG4ke0pTT04uc3RyaW5naWZ5KHJlc3BvbnNlTWVzc2FnZS5lcnJvciwgdW5kZWZpbmVkLCA0KX1gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIGxvZ2dlci5lcnJvcihgUmVjZWl2ZWQgcmVzcG9uc2UgbWVzc2FnZSB3aXRob3V0IGlkLiBObyBmdXJ0aGVyIGVycm9yIGluZm9ybWF0aW9uIHByb3ZpZGVkLmApO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgY29uc3Qga2V5ID0gcmVzcG9uc2VNZXNzYWdlLmlkO1xuICAgICAgICAgICAgY29uc3QgcmVzcG9uc2VQcm9taXNlID0gcmVzcG9uc2VQcm9taXNlcy5nZXQoa2V5KTtcbiAgICAgICAgICAgIHRyYWNlUmVjZWl2ZWRSZXNwb25zZShyZXNwb25zZU1lc3NhZ2UsIHJlc3BvbnNlUHJvbWlzZSk7XG4gICAgICAgICAgICBpZiAocmVzcG9uc2VQcm9taXNlICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICByZXNwb25zZVByb21pc2VzLmRlbGV0ZShrZXkpO1xuICAgICAgICAgICAgICAgIHRyeSB7XG4gICAgICAgICAgICAgICAgICAgIGlmIChyZXNwb25zZU1lc3NhZ2UuZXJyb3IpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGNvbnN0IGVycm9yID0gcmVzcG9uc2VNZXNzYWdlLmVycm9yO1xuICAgICAgICAgICAgICAgICAgICAgICAgcmVzcG9uc2VQcm9taXNlLnJlamVjdChuZXcgbWVzc2FnZXNfMS5SZXNwb25zZUVycm9yKGVycm9yLmNvZGUsIGVycm9yLm1lc3NhZ2UsIGVycm9yLmRhdGEpKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBlbHNlIGlmIChyZXNwb25zZU1lc3NhZ2UucmVzdWx0ICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJlc3BvbnNlUHJvbWlzZS5yZXNvbHZlKHJlc3BvbnNlTWVzc2FnZS5yZXN1bHQpO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdTaG91bGQgbmV2ZXIgaGFwcGVuLicpO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGNhdGNoIChlcnJvcikge1xuICAgICAgICAgICAgICAgICAgICBpZiAoZXJyb3IubWVzc2FnZSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgbG9nZ2VyLmVycm9yKGBSZXNwb25zZSBoYW5kbGVyICcke3Jlc3BvbnNlUHJvbWlzZS5tZXRob2R9JyBmYWlsZWQgd2l0aCBtZXNzYWdlOiAke2Vycm9yLm1lc3NhZ2V9YCk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBsb2dnZXIuZXJyb3IoYFJlc3BvbnNlIGhhbmRsZXIgJyR7cmVzcG9uc2VQcm9taXNlLm1ldGhvZH0nIGZhaWxlZCB1bmV4cGVjdGVkbHkuYCk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICB9XG4gICAgZnVuY3Rpb24gaGFuZGxlTm90aWZpY2F0aW9uKG1lc3NhZ2UpIHtcbiAgICAgICAgaWYgKGlzRGlzcG9zZWQoKSkge1xuICAgICAgICAgICAgLy8gU2VlIGhhbmRsZSByZXF1ZXN0LlxuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGxldCB0eXBlID0gdW5kZWZpbmVkO1xuICAgICAgICBsZXQgbm90aWZpY2F0aW9uSGFuZGxlcjtcbiAgICAgICAgaWYgKG1lc3NhZ2UubWV0aG9kID09PSBDYW5jZWxOb3RpZmljYXRpb24udHlwZS5tZXRob2QpIHtcbiAgICAgICAgICAgIGNvbnN0IGNhbmNlbElkID0gbWVzc2FnZS5wYXJhbXMuaWQ7XG4gICAgICAgICAgICBrbm93bkNhbmNlbGVkUmVxdWVzdHMuZGVsZXRlKGNhbmNlbElkKTtcbiAgICAgICAgICAgIHRyYWNlUmVjZWl2ZWROb3RpZmljYXRpb24obWVzc2FnZSk7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBjb25zdCBlbGVtZW50ID0gbm90aWZpY2F0aW9uSGFuZGxlcnMuZ2V0KG1lc3NhZ2UubWV0aG9kKTtcbiAgICAgICAgICAgIGlmIChlbGVtZW50KSB7XG4gICAgICAgICAgICAgICAgbm90aWZpY2F0aW9uSGFuZGxlciA9IGVsZW1lbnQuaGFuZGxlcjtcbiAgICAgICAgICAgICAgICB0eXBlID0gZWxlbWVudC50eXBlO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIGlmIChub3RpZmljYXRpb25IYW5kbGVyIHx8IHN0YXJOb3RpZmljYXRpb25IYW5kbGVyKSB7XG4gICAgICAgICAgICB0cnkge1xuICAgICAgICAgICAgICAgIHRyYWNlUmVjZWl2ZWROb3RpZmljYXRpb24obWVzc2FnZSk7XG4gICAgICAgICAgICAgICAgaWYgKG5vdGlmaWNhdGlvbkhhbmRsZXIpIHtcbiAgICAgICAgICAgICAgICAgICAgaWYgKG1lc3NhZ2UucGFyYW1zID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICh0eXBlICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBpZiAodHlwZS5udW1iZXJPZlBhcmFtcyAhPT0gMCAmJiB0eXBlLnBhcmFtZXRlclN0cnVjdHVyZXMgIT09IG1lc3NhZ2VzXzEuUGFyYW1ldGVyU3RydWN0dXJlcy5ieU5hbWUpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbG9nZ2VyLmVycm9yKGBOb3RpZmljYXRpb24gJHttZXNzYWdlLm1ldGhvZH0gZGVmaW5lcyAke3R5cGUubnVtYmVyT2ZQYXJhbXN9IHBhcmFtcyBidXQgcmVjZWl2ZWQgbm9uZS5gKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBub3RpZmljYXRpb25IYW5kbGVyKCk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZWxzZSBpZiAoQXJyYXkuaXNBcnJheShtZXNzYWdlLnBhcmFtcykpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIC8vIFRoZXJlIGFyZSBKU09OLVJQQyBsaWJyYXJpZXMgdGhhdCBzZW5kIHByb2dyZXNzIG1lc3NhZ2UgYXMgcG9zaXRpb25hbCBwYXJhbXMgYWx0aG91Z2hcbiAgICAgICAgICAgICAgICAgICAgICAgIC8vIHNwZWNpZmllZCBhcyBuYW1lZC4gU28gY29udmVydCB0aGVtIGlmIHRoaXMgaXMgdGhlIGNhc2UuXG4gICAgICAgICAgICAgICAgICAgICAgICBjb25zdCBwYXJhbXMgPSBtZXNzYWdlLnBhcmFtcztcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmIChtZXNzYWdlLm1ldGhvZCA9PT0gUHJvZ3Jlc3NOb3RpZmljYXRpb24udHlwZS5tZXRob2QgJiYgcGFyYW1zLmxlbmd0aCA9PT0gMiAmJiBQcm9ncmVzc1Rva2VuLmlzKHBhcmFtc1swXSkpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBub3RpZmljYXRpb25IYW5kbGVyKHsgdG9rZW46IHBhcmFtc1swXSwgdmFsdWU6IHBhcmFtc1sxXSB9KTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGlmICh0eXBlICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaWYgKHR5cGUucGFyYW1ldGVyU3RydWN0dXJlcyA9PT0gbWVzc2FnZXNfMS5QYXJhbWV0ZXJTdHJ1Y3R1cmVzLmJ5TmFtZSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbG9nZ2VyLmVycm9yKGBOb3RpZmljYXRpb24gJHttZXNzYWdlLm1ldGhvZH0gZGVmaW5lcyBwYXJhbWV0ZXJzIGJ5IG5hbWUgYnV0IHJlY2VpdmVkIHBhcmFtZXRlcnMgYnkgcG9zaXRpb25gKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBpZiAodHlwZS5udW1iZXJPZlBhcmFtcyAhPT0gbWVzc2FnZS5wYXJhbXMubGVuZ3RoKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBsb2dnZXIuZXJyb3IoYE5vdGlmaWNhdGlvbiAke21lc3NhZ2UubWV0aG9kfSBkZWZpbmVzICR7dHlwZS5udW1iZXJPZlBhcmFtc30gcGFyYW1zIGJ1dCByZWNlaXZlZCAke3BhcmFtcy5sZW5ndGh9IGFyZ3VtZW50c2ApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5vdGlmaWNhdGlvbkhhbmRsZXIoLi4ucGFyYW1zKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICh0eXBlICE9PSB1bmRlZmluZWQgJiYgdHlwZS5wYXJhbWV0ZXJTdHJ1Y3R1cmVzID09PSBtZXNzYWdlc18xLlBhcmFtZXRlclN0cnVjdHVyZXMuYnlQb3NpdGlvbikge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGxvZ2dlci5lcnJvcihgTm90aWZpY2F0aW9uICR7bWVzc2FnZS5tZXRob2R9IGRlZmluZXMgcGFyYW1ldGVycyBieSBwb3NpdGlvbiBidXQgcmVjZWl2ZWQgcGFyYW1ldGVycyBieSBuYW1lYCk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBub3RpZmljYXRpb25IYW5kbGVyKG1lc3NhZ2UucGFyYW1zKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIGlmIChzdGFyTm90aWZpY2F0aW9uSGFuZGxlcikge1xuICAgICAgICAgICAgICAgICAgICBzdGFyTm90aWZpY2F0aW9uSGFuZGxlcihtZXNzYWdlLm1ldGhvZCwgbWVzc2FnZS5wYXJhbXMpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNhdGNoIChlcnJvcikge1xuICAgICAgICAgICAgICAgIGlmIChlcnJvci5tZXNzYWdlKSB7XG4gICAgICAgICAgICAgICAgICAgIGxvZ2dlci5lcnJvcihgTm90aWZpY2F0aW9uIGhhbmRsZXIgJyR7bWVzc2FnZS5tZXRob2R9JyBmYWlsZWQgd2l0aCBtZXNzYWdlOiAke2Vycm9yLm1lc3NhZ2V9YCk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICBsb2dnZXIuZXJyb3IoYE5vdGlmaWNhdGlvbiBoYW5kbGVyICcke21lc3NhZ2UubWV0aG9kfScgZmFpbGVkIHVuZXhwZWN0ZWRseS5gKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICB1bmhhbmRsZWROb3RpZmljYXRpb25FbWl0dGVyLmZpcmUobWVzc2FnZSk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgZnVuY3Rpb24gaGFuZGxlSW52YWxpZE1lc3NhZ2UobWVzc2FnZSkge1xuICAgICAgICBpZiAoIW1lc3NhZ2UpIHtcbiAgICAgICAgICAgIGxvZ2dlci5lcnJvcignUmVjZWl2ZWQgZW1wdHkgbWVzc2FnZS4nKTtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBsb2dnZXIuZXJyb3IoYFJlY2VpdmVkIG1lc3NhZ2Ugd2hpY2ggaXMgbmVpdGhlciBhIHJlc3BvbnNlIG5vciBhIG5vdGlmaWNhdGlvbiBtZXNzYWdlOlxcbiR7SlNPTi5zdHJpbmdpZnkobWVzc2FnZSwgbnVsbCwgNCl9YCk7XG4gICAgICAgIC8vIFRlc3Qgd2hldGhlciB3ZSBmaW5kIGFuIGlkIHRvIHJlamVjdCB0aGUgcHJvbWlzZVxuICAgICAgICBjb25zdCByZXNwb25zZU1lc3NhZ2UgPSBtZXNzYWdlO1xuICAgICAgICBpZiAoSXMuc3RyaW5nKHJlc3BvbnNlTWVzc2FnZS5pZCkgfHwgSXMubnVtYmVyKHJlc3BvbnNlTWVzc2FnZS5pZCkpIHtcbiAgICAgICAgICAgIGNvbnN0IGtleSA9IHJlc3BvbnNlTWVzc2FnZS5pZDtcbiAgICAgICAgICAgIGNvbnN0IHJlc3BvbnNlSGFuZGxlciA9IHJlc3BvbnNlUHJvbWlzZXMuZ2V0KGtleSk7XG4gICAgICAgICAgICBpZiAocmVzcG9uc2VIYW5kbGVyKSB7XG4gICAgICAgICAgICAgICAgcmVzcG9uc2VIYW5kbGVyLnJlamVjdChuZXcgRXJyb3IoJ1RoZSByZWNlaXZlZCByZXNwb25zZSBoYXMgbmVpdGhlciBhIHJlc3VsdCBub3IgYW4gZXJyb3IgcHJvcGVydHkuJykpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIHN0cmluZ2lmeVRyYWNlKHBhcmFtcykge1xuICAgICAgICBpZiAocGFyYW1zID09PSB1bmRlZmluZWQgfHwgcGFyYW1zID09PSBudWxsKSB7XG4gICAgICAgICAgICByZXR1cm4gdW5kZWZpbmVkO1xuICAgICAgICB9XG4gICAgICAgIHN3aXRjaCAodHJhY2UpIHtcbiAgICAgICAgICAgIGNhc2UgVHJhY2UuVmVyYm9zZTpcbiAgICAgICAgICAgICAgICByZXR1cm4gSlNPTi5zdHJpbmdpZnkocGFyYW1zLCBudWxsLCA0KTtcbiAgICAgICAgICAgIGNhc2UgVHJhY2UuQ29tcGFjdDpcbiAgICAgICAgICAgICAgICByZXR1cm4gSlNPTi5zdHJpbmdpZnkocGFyYW1zKTtcbiAgICAgICAgICAgIGRlZmF1bHQ6XG4gICAgICAgICAgICAgICAgcmV0dXJuIHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgIH1cbiAgICBmdW5jdGlvbiB0cmFjZVNlbmRpbmdSZXF1ZXN0KG1lc3NhZ2UpIHtcbiAgICAgICAgaWYgKHRyYWNlID09PSBUcmFjZS5PZmYgfHwgIXRyYWNlcikge1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGlmICh0cmFjZUZvcm1hdCA9PT0gVHJhY2VGb3JtYXQuVGV4dCkge1xuICAgICAgICAgICAgbGV0IGRhdGEgPSB1bmRlZmluZWQ7XG4gICAgICAgICAgICBpZiAoKHRyYWNlID09PSBUcmFjZS5WZXJib3NlIHx8IHRyYWNlID09PSBUcmFjZS5Db21wYWN0KSAmJiBtZXNzYWdlLnBhcmFtcykge1xuICAgICAgICAgICAgICAgIGRhdGEgPSBgUGFyYW1zOiAke3N0cmluZ2lmeVRyYWNlKG1lc3NhZ2UucGFyYW1zKX1cXG5cXG5gO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgdHJhY2VyLmxvZyhgU2VuZGluZyByZXF1ZXN0ICcke21lc3NhZ2UubWV0aG9kfSAtICgke21lc3NhZ2UuaWR9KScuYCwgZGF0YSk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBsb2dMU1BNZXNzYWdlKCdzZW5kLXJlcXVlc3QnLCBtZXNzYWdlKTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBmdW5jdGlvbiB0cmFjZVNlbmRpbmdOb3RpZmljYXRpb24obWVzc2FnZSkge1xuICAgICAgICBpZiAodHJhY2UgPT09IFRyYWNlLk9mZiB8fCAhdHJhY2VyKSB7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgICAgaWYgKHRyYWNlRm9ybWF0ID09PSBUcmFjZUZvcm1hdC5UZXh0KSB7XG4gICAgICAgICAgICBsZXQgZGF0YSA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIGlmICh0cmFjZSA9PT0gVHJhY2UuVmVyYm9zZSB8fCB0cmFjZSA9PT0gVHJhY2UuQ29tcGFjdCkge1xuICAgICAgICAgICAgICAgIGlmIChtZXNzYWdlLnBhcmFtcykge1xuICAgICAgICAgICAgICAgICAgICBkYXRhID0gYFBhcmFtczogJHtzdHJpbmdpZnlUcmFjZShtZXNzYWdlLnBhcmFtcyl9XFxuXFxuYDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIGRhdGEgPSAnTm8gcGFyYW1ldGVycyBwcm92aWRlZC5cXG5cXG4nO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHRyYWNlci5sb2coYFNlbmRpbmcgbm90aWZpY2F0aW9uICcke21lc3NhZ2UubWV0aG9kfScuYCwgZGF0YSk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBsb2dMU1BNZXNzYWdlKCdzZW5kLW5vdGlmaWNhdGlvbicsIG1lc3NhZ2UpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIHRyYWNlU2VuZGluZ1Jlc3BvbnNlKG1lc3NhZ2UsIG1ldGhvZCwgc3RhcnRUaW1lKSB7XG4gICAgICAgIGlmICh0cmFjZSA9PT0gVHJhY2UuT2ZmIHx8ICF0cmFjZXIpIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBpZiAodHJhY2VGb3JtYXQgPT09IFRyYWNlRm9ybWF0LlRleHQpIHtcbiAgICAgICAgICAgIGxldCBkYXRhID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgaWYgKHRyYWNlID09PSBUcmFjZS5WZXJib3NlIHx8IHRyYWNlID09PSBUcmFjZS5Db21wYWN0KSB7XG4gICAgICAgICAgICAgICAgaWYgKG1lc3NhZ2UuZXJyb3IgJiYgbWVzc2FnZS5lcnJvci5kYXRhKSB7XG4gICAgICAgICAgICAgICAgICAgIGRhdGEgPSBgRXJyb3IgZGF0YTogJHtzdHJpbmdpZnlUcmFjZShtZXNzYWdlLmVycm9yLmRhdGEpfVxcblxcbmA7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICBpZiAobWVzc2FnZS5yZXN1bHQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGRhdGEgPSBgUmVzdWx0OiAke3N0cmluZ2lmeVRyYWNlKG1lc3NhZ2UucmVzdWx0KX1cXG5cXG5gO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2UgaWYgKG1lc3NhZ2UuZXJyb3IgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgZGF0YSA9ICdObyByZXN1bHQgcmV0dXJuZWQuXFxuXFxuJztcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHRyYWNlci5sb2coYFNlbmRpbmcgcmVzcG9uc2UgJyR7bWV0aG9kfSAtICgke21lc3NhZ2UuaWR9KScuIFByb2Nlc3NpbmcgcmVxdWVzdCB0b29rICR7RGF0ZS5ub3coKSAtIHN0YXJ0VGltZX1tc2AsIGRhdGEpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgbG9nTFNQTWVzc2FnZSgnc2VuZC1yZXNwb25zZScsIG1lc3NhZ2UpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIHRyYWNlUmVjZWl2ZWRSZXF1ZXN0KG1lc3NhZ2UpIHtcbiAgICAgICAgaWYgKHRyYWNlID09PSBUcmFjZS5PZmYgfHwgIXRyYWNlcikge1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGlmICh0cmFjZUZvcm1hdCA9PT0gVHJhY2VGb3JtYXQuVGV4dCkge1xuICAgICAgICAgICAgbGV0IGRhdGEgPSB1bmRlZmluZWQ7XG4gICAgICAgICAgICBpZiAoKHRyYWNlID09PSBUcmFjZS5WZXJib3NlIHx8IHRyYWNlID09PSBUcmFjZS5Db21wYWN0KSAmJiBtZXNzYWdlLnBhcmFtcykge1xuICAgICAgICAgICAgICAgIGRhdGEgPSBgUGFyYW1zOiAke3N0cmluZ2lmeVRyYWNlKG1lc3NhZ2UucGFyYW1zKX1cXG5cXG5gO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgdHJhY2VyLmxvZyhgUmVjZWl2ZWQgcmVxdWVzdCAnJHttZXNzYWdlLm1ldGhvZH0gLSAoJHttZXNzYWdlLmlkfSknLmAsIGRhdGEpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgbG9nTFNQTWVzc2FnZSgncmVjZWl2ZS1yZXF1ZXN0JywgbWVzc2FnZSk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgZnVuY3Rpb24gdHJhY2VSZWNlaXZlZE5vdGlmaWNhdGlvbihtZXNzYWdlKSB7XG4gICAgICAgIGlmICh0cmFjZSA9PT0gVHJhY2UuT2ZmIHx8ICF0cmFjZXIgfHwgbWVzc2FnZS5tZXRob2QgPT09IExvZ1RyYWNlTm90aWZpY2F0aW9uLnR5cGUubWV0aG9kKSB7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgICAgaWYgKHRyYWNlRm9ybWF0ID09PSBUcmFjZUZvcm1hdC5UZXh0KSB7XG4gICAgICAgICAgICBsZXQgZGF0YSA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIGlmICh0cmFjZSA9PT0gVHJhY2UuVmVyYm9zZSB8fCB0cmFjZSA9PT0gVHJhY2UuQ29tcGFjdCkge1xuICAgICAgICAgICAgICAgIGlmIChtZXNzYWdlLnBhcmFtcykge1xuICAgICAgICAgICAgICAgICAgICBkYXRhID0gYFBhcmFtczogJHtzdHJpbmdpZnlUcmFjZShtZXNzYWdlLnBhcmFtcyl9XFxuXFxuYDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIGRhdGEgPSAnTm8gcGFyYW1ldGVycyBwcm92aWRlZC5cXG5cXG4nO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHRyYWNlci5sb2coYFJlY2VpdmVkIG5vdGlmaWNhdGlvbiAnJHttZXNzYWdlLm1ldGhvZH0nLmAsIGRhdGEpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgbG9nTFNQTWVzc2FnZSgncmVjZWl2ZS1ub3RpZmljYXRpb24nLCBtZXNzYWdlKTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBmdW5jdGlvbiB0cmFjZVJlY2VpdmVkUmVzcG9uc2UobWVzc2FnZSwgcmVzcG9uc2VQcm9taXNlKSB7XG4gICAgICAgIGlmICh0cmFjZSA9PT0gVHJhY2UuT2ZmIHx8ICF0cmFjZXIpIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBpZiAodHJhY2VGb3JtYXQgPT09IFRyYWNlRm9ybWF0LlRleHQpIHtcbiAgICAgICAgICAgIGxldCBkYXRhID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgaWYgKHRyYWNlID09PSBUcmFjZS5WZXJib3NlIHx8IHRyYWNlID09PSBUcmFjZS5Db21wYWN0KSB7XG4gICAgICAgICAgICAgICAgaWYgKG1lc3NhZ2UuZXJyb3IgJiYgbWVzc2FnZS5lcnJvci5kYXRhKSB7XG4gICAgICAgICAgICAgICAgICAgIGRhdGEgPSBgRXJyb3IgZGF0YTogJHtzdHJpbmdpZnlUcmFjZShtZXNzYWdlLmVycm9yLmRhdGEpfVxcblxcbmA7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICBpZiAobWVzc2FnZS5yZXN1bHQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGRhdGEgPSBgUmVzdWx0OiAke3N0cmluZ2lmeVRyYWNlKG1lc3NhZ2UucmVzdWx0KX1cXG5cXG5gO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2UgaWYgKG1lc3NhZ2UuZXJyb3IgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgZGF0YSA9ICdObyByZXN1bHQgcmV0dXJuZWQuXFxuXFxuJztcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmIChyZXNwb25zZVByb21pc2UpIHtcbiAgICAgICAgICAgICAgICBjb25zdCBlcnJvciA9IG1lc3NhZ2UuZXJyb3IgPyBgIFJlcXVlc3QgZmFpbGVkOiAke21lc3NhZ2UuZXJyb3IubWVzc2FnZX0gKCR7bWVzc2FnZS5lcnJvci5jb2RlfSkuYCA6ICcnO1xuICAgICAgICAgICAgICAgIHRyYWNlci5sb2coYFJlY2VpdmVkIHJlc3BvbnNlICcke3Jlc3BvbnNlUHJvbWlzZS5tZXRob2R9IC0gKCR7bWVzc2FnZS5pZH0pJyBpbiAke0RhdGUubm93KCkgLSByZXNwb25zZVByb21pc2UudGltZXJTdGFydH1tcy4ke2Vycm9yfWAsIGRhdGEpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgdHJhY2VyLmxvZyhgUmVjZWl2ZWQgcmVzcG9uc2UgJHttZXNzYWdlLmlkfSB3aXRob3V0IGFjdGl2ZSByZXNwb25zZSBwcm9taXNlLmAsIGRhdGEpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgbG9nTFNQTWVzc2FnZSgncmVjZWl2ZS1yZXNwb25zZScsIG1lc3NhZ2UpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIGxvZ0xTUE1lc3NhZ2UodHlwZSwgbWVzc2FnZSkge1xuICAgICAgICBpZiAoIXRyYWNlciB8fCB0cmFjZSA9PT0gVHJhY2UuT2ZmKSB7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgICAgY29uc3QgbHNwTWVzc2FnZSA9IHtcbiAgICAgICAgICAgIGlzTFNQTWVzc2FnZTogdHJ1ZSxcbiAgICAgICAgICAgIHR5cGUsXG4gICAgICAgICAgICBtZXNzYWdlLFxuICAgICAgICAgICAgdGltZXN0YW1wOiBEYXRlLm5vdygpXG4gICAgICAgIH07XG4gICAgICAgIHRyYWNlci5sb2cobHNwTWVzc2FnZSk7XG4gICAgfVxuICAgIGZ1bmN0aW9uIHRocm93SWZDbG9zZWRPckRpc3Bvc2VkKCkge1xuICAgICAgICBpZiAoaXNDbG9zZWQoKSkge1xuICAgICAgICAgICAgdGhyb3cgbmV3IENvbm5lY3Rpb25FcnJvcihDb25uZWN0aW9uRXJyb3JzLkNsb3NlZCwgJ0Nvbm5lY3Rpb24gaXMgY2xvc2VkLicpO1xuICAgICAgICB9XG4gICAgICAgIGlmIChpc0Rpc3Bvc2VkKCkpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBDb25uZWN0aW9uRXJyb3IoQ29ubmVjdGlvbkVycm9ycy5EaXNwb3NlZCwgJ0Nvbm5lY3Rpb24gaXMgZGlzcG9zZWQuJyk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgZnVuY3Rpb24gdGhyb3dJZkxpc3RlbmluZygpIHtcbiAgICAgICAgaWYgKGlzTGlzdGVuaW5nKCkpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBDb25uZWN0aW9uRXJyb3IoQ29ubmVjdGlvbkVycm9ycy5BbHJlYWR5TGlzdGVuaW5nLCAnQ29ubmVjdGlvbiBpcyBhbHJlYWR5IGxpc3RlbmluZycpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIHRocm93SWZOb3RMaXN0ZW5pbmcoKSB7XG4gICAgICAgIGlmICghaXNMaXN0ZW5pbmcoKSkge1xuICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdDYWxsIGxpc3RlbigpIGZpcnN0LicpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIHVuZGVmaW5lZFRvTnVsbChwYXJhbSkge1xuICAgICAgICBpZiAocGFyYW0gPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmV0dXJuIG51bGw7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICByZXR1cm4gcGFyYW07XG4gICAgICAgIH1cbiAgICB9XG4gICAgZnVuY3Rpb24gbnVsbFRvVW5kZWZpbmVkKHBhcmFtKSB7XG4gICAgICAgIGlmIChwYXJhbSA9PT0gbnVsbCkge1xuICAgICAgICAgICAgcmV0dXJuIHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHJldHVybiBwYXJhbTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBmdW5jdGlvbiBpc05hbWVkUGFyYW0ocGFyYW0pIHtcbiAgICAgICAgcmV0dXJuIHBhcmFtICE9PSB1bmRlZmluZWQgJiYgcGFyYW0gIT09IG51bGwgJiYgIUFycmF5LmlzQXJyYXkocGFyYW0pICYmIHR5cGVvZiBwYXJhbSA9PT0gJ29iamVjdCc7XG4gICAgfVxuICAgIGZ1bmN0aW9uIGNvbXB1dGVTaW5nbGVQYXJhbShwYXJhbWV0ZXJTdHJ1Y3R1cmVzLCBwYXJhbSkge1xuICAgICAgICBzd2l0Y2ggKHBhcmFtZXRlclN0cnVjdHVyZXMpIHtcbiAgICAgICAgICAgIGNhc2UgbWVzc2FnZXNfMS5QYXJhbWV0ZXJTdHJ1Y3R1cmVzLmF1dG86XG4gICAgICAgICAgICAgICAgaWYgKGlzTmFtZWRQYXJhbShwYXJhbSkpIHtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIG51bGxUb1VuZGVmaW5lZChwYXJhbSk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4gW3VuZGVmaW5lZFRvTnVsbChwYXJhbSldO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNhc2UgbWVzc2FnZXNfMS5QYXJhbWV0ZXJTdHJ1Y3R1cmVzLmJ5TmFtZTpcbiAgICAgICAgICAgICAgICBpZiAoIWlzTmFtZWRQYXJhbShwYXJhbSkpIHtcbiAgICAgICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBSZWNlaXZlZCBwYXJhbWV0ZXJzIGJ5IG5hbWUgYnV0IHBhcmFtIGlzIG5vdCBhbiBvYmplY3QgbGl0ZXJhbC5gKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgcmV0dXJuIG51bGxUb1VuZGVmaW5lZChwYXJhbSk7XG4gICAgICAgICAgICBjYXNlIG1lc3NhZ2VzXzEuUGFyYW1ldGVyU3RydWN0dXJlcy5ieVBvc2l0aW9uOlxuICAgICAgICAgICAgICAgIHJldHVybiBbdW5kZWZpbmVkVG9OdWxsKHBhcmFtKV07XG4gICAgICAgICAgICBkZWZhdWx0OlxuICAgICAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgVW5rbm93biBwYXJhbWV0ZXIgc3RydWN0dXJlICR7cGFyYW1ldGVyU3RydWN0dXJlcy50b1N0cmluZygpfWApO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIGNvbXB1dGVNZXNzYWdlUGFyYW1zKHR5cGUsIHBhcmFtcykge1xuICAgICAgICBsZXQgcmVzdWx0O1xuICAgICAgICBjb25zdCBudW1iZXJPZlBhcmFtcyA9IHR5cGUubnVtYmVyT2ZQYXJhbXM7XG4gICAgICAgIHN3aXRjaCAobnVtYmVyT2ZQYXJhbXMpIHtcbiAgICAgICAgICAgIGNhc2UgMDpcbiAgICAgICAgICAgICAgICByZXN1bHQgPSB1bmRlZmluZWQ7XG4gICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICBjYXNlIDE6XG4gICAgICAgICAgICAgICAgcmVzdWx0ID0gY29tcHV0ZVNpbmdsZVBhcmFtKHR5cGUucGFyYW1ldGVyU3RydWN0dXJlcywgcGFyYW1zWzBdKTtcbiAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgIGRlZmF1bHQ6XG4gICAgICAgICAgICAgICAgcmVzdWx0ID0gW107XG4gICAgICAgICAgICAgICAgZm9yIChsZXQgaSA9IDA7IGkgPCBwYXJhbXMubGVuZ3RoICYmIGkgPCBudW1iZXJPZlBhcmFtczsgaSsrKSB7XG4gICAgICAgICAgICAgICAgICAgIHJlc3VsdC5wdXNoKHVuZGVmaW5lZFRvTnVsbChwYXJhbXNbaV0pKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgaWYgKHBhcmFtcy5sZW5ndGggPCBudW1iZXJPZlBhcmFtcykge1xuICAgICAgICAgICAgICAgICAgICBmb3IgKGxldCBpID0gcGFyYW1zLmxlbmd0aDsgaSA8IG51bWJlck9mUGFyYW1zOyBpKyspIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJlc3VsdC5wdXNoKG51bGwpO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxuICAgIGNvbnN0IGNvbm5lY3Rpb24gPSB7XG4gICAgICAgIHNlbmROb3RpZmljYXRpb246ICh0eXBlLCAuLi5hcmdzKSA9PiB7XG4gICAgICAgICAgICB0aHJvd0lmQ2xvc2VkT3JEaXNwb3NlZCgpO1xuICAgICAgICAgICAgbGV0IG1ldGhvZDtcbiAgICAgICAgICAgIGxldCBtZXNzYWdlUGFyYW1zO1xuICAgICAgICAgICAgaWYgKElzLnN0cmluZyh0eXBlKSkge1xuICAgICAgICAgICAgICAgIG1ldGhvZCA9IHR5cGU7XG4gICAgICAgICAgICAgICAgY29uc3QgZmlyc3QgPSBhcmdzWzBdO1xuICAgICAgICAgICAgICAgIGxldCBwYXJhbVN0YXJ0ID0gMDtcbiAgICAgICAgICAgICAgICBsZXQgcGFyYW1ldGVyU3RydWN0dXJlcyA9IG1lc3NhZ2VzXzEuUGFyYW1ldGVyU3RydWN0dXJlcy5hdXRvO1xuICAgICAgICAgICAgICAgIGlmIChtZXNzYWdlc18xLlBhcmFtZXRlclN0cnVjdHVyZXMuaXMoZmlyc3QpKSB7XG4gICAgICAgICAgICAgICAgICAgIHBhcmFtU3RhcnQgPSAxO1xuICAgICAgICAgICAgICAgICAgICBwYXJhbWV0ZXJTdHJ1Y3R1cmVzID0gZmlyc3Q7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGxldCBwYXJhbUVuZCA9IGFyZ3MubGVuZ3RoO1xuICAgICAgICAgICAgICAgIGNvbnN0IG51bWJlck9mUGFyYW1zID0gcGFyYW1FbmQgLSBwYXJhbVN0YXJ0O1xuICAgICAgICAgICAgICAgIHN3aXRjaCAobnVtYmVyT2ZQYXJhbXMpIHtcbiAgICAgICAgICAgICAgICAgICAgY2FzZSAwOlxuICAgICAgICAgICAgICAgICAgICAgICAgbWVzc2FnZVBhcmFtcyA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICBjYXNlIDE6XG4gICAgICAgICAgICAgICAgICAgICAgICBtZXNzYWdlUGFyYW1zID0gY29tcHV0ZVNpbmdsZVBhcmFtKHBhcmFtZXRlclN0cnVjdHVyZXMsIGFyZ3NbcGFyYW1TdGFydF0pO1xuICAgICAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgICAgIGRlZmF1bHQ6XG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAocGFyYW1ldGVyU3RydWN0dXJlcyA9PT0gbWVzc2FnZXNfMS5QYXJhbWV0ZXJTdHJ1Y3R1cmVzLmJ5TmFtZSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgUmVjZWl2ZWQgJHtudW1iZXJPZlBhcmFtc30gcGFyYW1ldGVycyBmb3IgJ2J5IE5hbWUnIG5vdGlmaWNhdGlvbiBwYXJhbWV0ZXIgc3RydWN0dXJlLmApO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgbWVzc2FnZVBhcmFtcyA9IGFyZ3Muc2xpY2UocGFyYW1TdGFydCwgcGFyYW1FbmQpLm1hcCh2YWx1ZSA9PiB1bmRlZmluZWRUb051bGwodmFsdWUpKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIGNvbnN0IHBhcmFtcyA9IGFyZ3M7XG4gICAgICAgICAgICAgICAgbWV0aG9kID0gdHlwZS5tZXRob2Q7XG4gICAgICAgICAgICAgICAgbWVzc2FnZVBhcmFtcyA9IGNvbXB1dGVNZXNzYWdlUGFyYW1zKHR5cGUsIHBhcmFtcyk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBjb25zdCBub3RpZmljYXRpb25NZXNzYWdlID0ge1xuICAgICAgICAgICAgICAgIGpzb25ycGM6IHZlcnNpb24sXG4gICAgICAgICAgICAgICAgbWV0aG9kOiBtZXRob2QsXG4gICAgICAgICAgICAgICAgcGFyYW1zOiBtZXNzYWdlUGFyYW1zXG4gICAgICAgICAgICB9O1xuICAgICAgICAgICAgdHJhY2VTZW5kaW5nTm90aWZpY2F0aW9uKG5vdGlmaWNhdGlvbk1lc3NhZ2UpO1xuICAgICAgICAgICAgcmV0dXJuIG1lc3NhZ2VXcml0ZXIud3JpdGUobm90aWZpY2F0aW9uTWVzc2FnZSkuY2F0Y2goKGVycm9yKSA9PiB7XG4gICAgICAgICAgICAgICAgbG9nZ2VyLmVycm9yKGBTZW5kaW5nIG5vdGlmaWNhdGlvbiBmYWlsZWQuYCk7XG4gICAgICAgICAgICAgICAgdGhyb3cgZXJyb3I7XG4gICAgICAgICAgICB9KTtcbiAgICAgICAgfSxcbiAgICAgICAgb25Ob3RpZmljYXRpb246ICh0eXBlLCBoYW5kbGVyKSA9PiB7XG4gICAgICAgICAgICB0aHJvd0lmQ2xvc2VkT3JEaXNwb3NlZCgpO1xuICAgICAgICAgICAgbGV0IG1ldGhvZDtcbiAgICAgICAgICAgIGlmIChJcy5mdW5jKHR5cGUpKSB7XG4gICAgICAgICAgICAgICAgc3Rhck5vdGlmaWNhdGlvbkhhbmRsZXIgPSB0eXBlO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSBpZiAoaGFuZGxlcikge1xuICAgICAgICAgICAgICAgIGlmIChJcy5zdHJpbmcodHlwZSkpIHtcbiAgICAgICAgICAgICAgICAgICAgbWV0aG9kID0gdHlwZTtcbiAgICAgICAgICAgICAgICAgICAgbm90aWZpY2F0aW9uSGFuZGxlcnMuc2V0KHR5cGUsIHsgdHlwZTogdW5kZWZpbmVkLCBoYW5kbGVyIH0pO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgbWV0aG9kID0gdHlwZS5tZXRob2Q7XG4gICAgICAgICAgICAgICAgICAgIG5vdGlmaWNhdGlvbkhhbmRsZXJzLnNldCh0eXBlLm1ldGhvZCwgeyB0eXBlLCBoYW5kbGVyIH0pO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICAgICAgZGlzcG9zZTogKCkgPT4ge1xuICAgICAgICAgICAgICAgICAgICBpZiAobWV0aG9kICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIG5vdGlmaWNhdGlvbkhhbmRsZXJzLmRlbGV0ZShtZXRob2QpO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgc3Rhck5vdGlmaWNhdGlvbkhhbmRsZXIgPSB1bmRlZmluZWQ7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9O1xuICAgICAgICB9LFxuICAgICAgICBvblByb2dyZXNzOiAoX3R5cGUsIHRva2VuLCBoYW5kbGVyKSA9PiB7XG4gICAgICAgICAgICBpZiAocHJvZ3Jlc3NIYW5kbGVycy5oYXModG9rZW4pKSB7XG4gICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBQcm9ncmVzcyBoYW5kbGVyIGZvciB0b2tlbiAke3Rva2VufSBhbHJlYWR5IHJlZ2lzdGVyZWRgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHByb2dyZXNzSGFuZGxlcnMuc2V0KHRva2VuLCBoYW5kbGVyKTtcbiAgICAgICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICAgICAgZGlzcG9zZTogKCkgPT4ge1xuICAgICAgICAgICAgICAgICAgICBwcm9ncmVzc0hhbmRsZXJzLmRlbGV0ZSh0b2tlbik7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfTtcbiAgICAgICAgfSxcbiAgICAgICAgc2VuZFByb2dyZXNzOiAoX3R5cGUsIHRva2VuLCB2YWx1ZSkgPT4ge1xuICAgICAgICAgICAgLy8gVGhpcyBzaG91bGQgbm90IGF3YWl0IGJ1dCBzaW1wbGUgcmV0dXJuIHRvIGVuc3VyZSB0aGF0IHdlIGRvbid0IGhhdmUgYW5vdGhlclxuICAgICAgICAgICAgLy8gYXN5bmMgc2NoZWR1bGluZy4gT3RoZXJ3aXNlIG9uZSBzZW5kIGNvdWxkIG92ZXJ0YWtlIGFub3RoZXIgc2VuZC5cbiAgICAgICAgICAgIHJldHVybiBjb25uZWN0aW9uLnNlbmROb3RpZmljYXRpb24oUHJvZ3Jlc3NOb3RpZmljYXRpb24udHlwZSwgeyB0b2tlbiwgdmFsdWUgfSk7XG4gICAgICAgIH0sXG4gICAgICAgIG9uVW5oYW5kbGVkUHJvZ3Jlc3M6IHVuaGFuZGxlZFByb2dyZXNzRW1pdHRlci5ldmVudCxcbiAgICAgICAgc2VuZFJlcXVlc3Q6ICh0eXBlLCAuLi5hcmdzKSA9PiB7XG4gICAgICAgICAgICB0aHJvd0lmQ2xvc2VkT3JEaXNwb3NlZCgpO1xuICAgICAgICAgICAgdGhyb3dJZk5vdExpc3RlbmluZygpO1xuICAgICAgICAgICAgbGV0IG1ldGhvZDtcbiAgICAgICAgICAgIGxldCBtZXNzYWdlUGFyYW1zO1xuICAgICAgICAgICAgbGV0IHRva2VuID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgaWYgKElzLnN0cmluZyh0eXBlKSkge1xuICAgICAgICAgICAgICAgIG1ldGhvZCA9IHR5cGU7XG4gICAgICAgICAgICAgICAgY29uc3QgZmlyc3QgPSBhcmdzWzBdO1xuICAgICAgICAgICAgICAgIGNvbnN0IGxhc3QgPSBhcmdzW2FyZ3MubGVuZ3RoIC0gMV07XG4gICAgICAgICAgICAgICAgbGV0IHBhcmFtU3RhcnQgPSAwO1xuICAgICAgICAgICAgICAgIGxldCBwYXJhbWV0ZXJTdHJ1Y3R1cmVzID0gbWVzc2FnZXNfMS5QYXJhbWV0ZXJTdHJ1Y3R1cmVzLmF1dG87XG4gICAgICAgICAgICAgICAgaWYgKG1lc3NhZ2VzXzEuUGFyYW1ldGVyU3RydWN0dXJlcy5pcyhmaXJzdCkpIHtcbiAgICAgICAgICAgICAgICAgICAgcGFyYW1TdGFydCA9IDE7XG4gICAgICAgICAgICAgICAgICAgIHBhcmFtZXRlclN0cnVjdHVyZXMgPSBmaXJzdDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgbGV0IHBhcmFtRW5kID0gYXJncy5sZW5ndGg7XG4gICAgICAgICAgICAgICAgaWYgKGNhbmNlbGxhdGlvbl8xLkNhbmNlbGxhdGlvblRva2VuLmlzKGxhc3QpKSB7XG4gICAgICAgICAgICAgICAgICAgIHBhcmFtRW5kID0gcGFyYW1FbmQgLSAxO1xuICAgICAgICAgICAgICAgICAgICB0b2tlbiA9IGxhc3Q7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGNvbnN0IG51bWJlck9mUGFyYW1zID0gcGFyYW1FbmQgLSBwYXJhbVN0YXJ0O1xuICAgICAgICAgICAgICAgIHN3aXRjaCAobnVtYmVyT2ZQYXJhbXMpIHtcbiAgICAgICAgICAgICAgICAgICAgY2FzZSAwOlxuICAgICAgICAgICAgICAgICAgICAgICAgbWVzc2FnZVBhcmFtcyA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICBjYXNlIDE6XG4gICAgICAgICAgICAgICAgICAgICAgICBtZXNzYWdlUGFyYW1zID0gY29tcHV0ZVNpbmdsZVBhcmFtKHBhcmFtZXRlclN0cnVjdHVyZXMsIGFyZ3NbcGFyYW1TdGFydF0pO1xuICAgICAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgICAgIGRlZmF1bHQ6XG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAocGFyYW1ldGVyU3RydWN0dXJlcyA9PT0gbWVzc2FnZXNfMS5QYXJhbWV0ZXJTdHJ1Y3R1cmVzLmJ5TmFtZSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgUmVjZWl2ZWQgJHtudW1iZXJPZlBhcmFtc30gcGFyYW1ldGVycyBmb3IgJ2J5IE5hbWUnIHJlcXVlc3QgcGFyYW1ldGVyIHN0cnVjdHVyZS5gKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIG1lc3NhZ2VQYXJhbXMgPSBhcmdzLnNsaWNlKHBhcmFtU3RhcnQsIHBhcmFtRW5kKS5tYXAodmFsdWUgPT4gdW5kZWZpbmVkVG9OdWxsKHZhbHVlKSk7XG4gICAgICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICBjb25zdCBwYXJhbXMgPSBhcmdzO1xuICAgICAgICAgICAgICAgIG1ldGhvZCA9IHR5cGUubWV0aG9kO1xuICAgICAgICAgICAgICAgIG1lc3NhZ2VQYXJhbXMgPSBjb21wdXRlTWVzc2FnZVBhcmFtcyh0eXBlLCBwYXJhbXMpO1xuICAgICAgICAgICAgICAgIGNvbnN0IG51bWJlck9mUGFyYW1zID0gdHlwZS5udW1iZXJPZlBhcmFtcztcbiAgICAgICAgICAgICAgICB0b2tlbiA9IGNhbmNlbGxhdGlvbl8xLkNhbmNlbGxhdGlvblRva2VuLmlzKHBhcmFtc1tudW1iZXJPZlBhcmFtc10pID8gcGFyYW1zW251bWJlck9mUGFyYW1zXSA6IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNvbnN0IGlkID0gc2VxdWVuY2VOdW1iZXIrKztcbiAgICAgICAgICAgIGxldCBkaXNwb3NhYmxlO1xuICAgICAgICAgICAgaWYgKHRva2VuKSB7XG4gICAgICAgICAgICAgICAgZGlzcG9zYWJsZSA9IHRva2VuLm9uQ2FuY2VsbGF0aW9uUmVxdWVzdGVkKCgpID0+IHtcbiAgICAgICAgICAgICAgICAgICAgY29uc3QgcCA9IGNhbmNlbGxhdGlvblN0cmF0ZWd5LnNlbmRlci5zZW5kQ2FuY2VsbGF0aW9uKGNvbm5lY3Rpb24sIGlkKTtcbiAgICAgICAgICAgICAgICAgICAgaWYgKHAgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgbG9nZ2VyLmxvZyhgUmVjZWl2ZWQgbm8gcHJvbWlzZSBmcm9tIGNhbmNlbGxhdGlvbiBzdHJhdGVneSB3aGVuIGNhbmNlbGxpbmcgaWQgJHtpZH1gKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiBQcm9taXNlLnJlc29sdmUoKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiBwLmNhdGNoKCgpID0+IHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBsb2dnZXIubG9nKGBTZW5kaW5nIGNhbmNlbGxhdGlvbiBtZXNzYWdlcyBmb3IgaWQgJHtpZH0gZmFpbGVkYCk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9KTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgY29uc3QgcmVxdWVzdE1lc3NhZ2UgPSB7XG4gICAgICAgICAgICAgICAganNvbnJwYzogdmVyc2lvbixcbiAgICAgICAgICAgICAgICBpZDogaWQsXG4gICAgICAgICAgICAgICAgbWV0aG9kOiBtZXRob2QsXG4gICAgICAgICAgICAgICAgcGFyYW1zOiBtZXNzYWdlUGFyYW1zXG4gICAgICAgICAgICB9O1xuICAgICAgICAgICAgdHJhY2VTZW5kaW5nUmVxdWVzdChyZXF1ZXN0TWVzc2FnZSk7XG4gICAgICAgICAgICBpZiAodHlwZW9mIGNhbmNlbGxhdGlvblN0cmF0ZWd5LnNlbmRlci5lbmFibGVDYW5jZWxsYXRpb24gPT09ICdmdW5jdGlvbicpIHtcbiAgICAgICAgICAgICAgICBjYW5jZWxsYXRpb25TdHJhdGVneS5zZW5kZXIuZW5hYmxlQ2FuY2VsbGF0aW9uKHJlcXVlc3RNZXNzYWdlKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJldHVybiBuZXcgUHJvbWlzZShhc3luYyAocmVzb2x2ZSwgcmVqZWN0KSA9PiB7XG4gICAgICAgICAgICAgICAgY29uc3QgcmVzb2x2ZVdpdGhDbGVhbnVwID0gKHIpID0+IHtcbiAgICAgICAgICAgICAgICAgICAgcmVzb2x2ZShyKTtcbiAgICAgICAgICAgICAgICAgICAgY2FuY2VsbGF0aW9uU3RyYXRlZ3kuc2VuZGVyLmNsZWFudXAoaWQpO1xuICAgICAgICAgICAgICAgICAgICBkaXNwb3NhYmxlPy5kaXNwb3NlKCk7XG4gICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgICAgICBjb25zdCByZWplY3RXaXRoQ2xlYW51cCA9IChyKSA9PiB7XG4gICAgICAgICAgICAgICAgICAgIHJlamVjdChyKTtcbiAgICAgICAgICAgICAgICAgICAgY2FuY2VsbGF0aW9uU3RyYXRlZ3kuc2VuZGVyLmNsZWFudXAoaWQpO1xuICAgICAgICAgICAgICAgICAgICBkaXNwb3NhYmxlPy5kaXNwb3NlKCk7XG4gICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgICAgICBjb25zdCByZXNwb25zZVByb21pc2UgPSB7IG1ldGhvZDogbWV0aG9kLCB0aW1lclN0YXJ0OiBEYXRlLm5vdygpLCByZXNvbHZlOiByZXNvbHZlV2l0aENsZWFudXAsIHJlamVjdDogcmVqZWN0V2l0aENsZWFudXAgfTtcbiAgICAgICAgICAgICAgICB0cnkge1xuICAgICAgICAgICAgICAgICAgICByZXNwb25zZVByb21pc2VzLnNldChpZCwgcmVzcG9uc2VQcm9taXNlKTtcbiAgICAgICAgICAgICAgICAgICAgYXdhaXQgbWVzc2FnZVdyaXRlci53cml0ZShyZXF1ZXN0TWVzc2FnZSk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGNhdGNoIChlcnJvcikge1xuICAgICAgICAgICAgICAgICAgICAvLyBXcml0aW5nIHRoZSBtZXNzYWdlIGZhaWxlZC4gU28gd2UgbmVlZCB0byBkZWxldGUgaXQgZnJvbSB0aGUgcmVzcG9uc2UgcHJvbWlzZXMgYW5kXG4gICAgICAgICAgICAgICAgICAgIC8vIHJlamVjdCBpdC5cbiAgICAgICAgICAgICAgICAgICAgcmVzcG9uc2VQcm9taXNlcy5kZWxldGUoaWQpO1xuICAgICAgICAgICAgICAgICAgICByZXNwb25zZVByb21pc2UucmVqZWN0KG5ldyBtZXNzYWdlc18xLlJlc3BvbnNlRXJyb3IobWVzc2FnZXNfMS5FcnJvckNvZGVzLk1lc3NhZ2VXcml0ZUVycm9yLCBlcnJvci5tZXNzYWdlID8gZXJyb3IubWVzc2FnZSA6ICdVbmtub3duIHJlYXNvbicpKTtcbiAgICAgICAgICAgICAgICAgICAgbG9nZ2VyLmVycm9yKGBTZW5kaW5nIHJlcXVlc3QgZmFpbGVkLmApO1xuICAgICAgICAgICAgICAgICAgICB0aHJvdyBlcnJvcjtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9KTtcbiAgICAgICAgfSxcbiAgICAgICAgb25SZXF1ZXN0OiAodHlwZSwgaGFuZGxlcikgPT4ge1xuICAgICAgICAgICAgdGhyb3dJZkNsb3NlZE9yRGlzcG9zZWQoKTtcbiAgICAgICAgICAgIGxldCBtZXRob2QgPSBudWxsO1xuICAgICAgICAgICAgaWYgKFN0YXJSZXF1ZXN0SGFuZGxlci5pcyh0eXBlKSkge1xuICAgICAgICAgICAgICAgIG1ldGhvZCA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgICAgICBzdGFyUmVxdWVzdEhhbmRsZXIgPSB0eXBlO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSBpZiAoSXMuc3RyaW5nKHR5cGUpKSB7XG4gICAgICAgICAgICAgICAgbWV0aG9kID0gbnVsbDtcbiAgICAgICAgICAgICAgICBpZiAoaGFuZGxlciAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICAgICAgICAgIG1ldGhvZCA9IHR5cGU7XG4gICAgICAgICAgICAgICAgICAgIHJlcXVlc3RIYW5kbGVycy5zZXQodHlwZSwgeyBoYW5kbGVyOiBoYW5kbGVyLCB0eXBlOiB1bmRlZmluZWQgfSk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgaWYgKGhhbmRsZXIgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgICAgICBtZXRob2QgPSB0eXBlLm1ldGhvZDtcbiAgICAgICAgICAgICAgICAgICAgcmVxdWVzdEhhbmRsZXJzLnNldCh0eXBlLm1ldGhvZCwgeyB0eXBlLCBoYW5kbGVyIH0pO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICAgICAgZGlzcG9zZTogKCkgPT4ge1xuICAgICAgICAgICAgICAgICAgICBpZiAobWV0aG9kID09PSBudWxsKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgaWYgKG1ldGhvZCAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXF1ZXN0SGFuZGxlcnMuZGVsZXRlKG1ldGhvZCk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBzdGFyUmVxdWVzdEhhbmRsZXIgPSB1bmRlZmluZWQ7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9O1xuICAgICAgICB9LFxuICAgICAgICBoYXNQZW5kaW5nUmVzcG9uc2U6ICgpID0+IHtcbiAgICAgICAgICAgIHJldHVybiByZXNwb25zZVByb21pc2VzLnNpemUgPiAwO1xuICAgICAgICB9LFxuICAgICAgICB0cmFjZTogYXN5bmMgKF92YWx1ZSwgX3RyYWNlciwgc2VuZE5vdGlmaWNhdGlvbk9yVHJhY2VPcHRpb25zKSA9PiB7XG4gICAgICAgICAgICBsZXQgX3NlbmROb3RpZmljYXRpb24gPSBmYWxzZTtcbiAgICAgICAgICAgIGxldCBfdHJhY2VGb3JtYXQgPSBUcmFjZUZvcm1hdC5UZXh0O1xuICAgICAgICAgICAgaWYgKHNlbmROb3RpZmljYXRpb25PclRyYWNlT3B0aW9ucyAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICAgICAgaWYgKElzLmJvb2xlYW4oc2VuZE5vdGlmaWNhdGlvbk9yVHJhY2VPcHRpb25zKSkge1xuICAgICAgICAgICAgICAgICAgICBfc2VuZE5vdGlmaWNhdGlvbiA9IHNlbmROb3RpZmljYXRpb25PclRyYWNlT3B0aW9ucztcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIF9zZW5kTm90aWZpY2F0aW9uID0gc2VuZE5vdGlmaWNhdGlvbk9yVHJhY2VPcHRpb25zLnNlbmROb3RpZmljYXRpb24gfHwgZmFsc2U7XG4gICAgICAgICAgICAgICAgICAgIF90cmFjZUZvcm1hdCA9IHNlbmROb3RpZmljYXRpb25PclRyYWNlT3B0aW9ucy50cmFjZUZvcm1hdCB8fCBUcmFjZUZvcm1hdC5UZXh0O1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHRyYWNlID0gX3ZhbHVlO1xuICAgICAgICAgICAgdHJhY2VGb3JtYXQgPSBfdHJhY2VGb3JtYXQ7XG4gICAgICAgICAgICBpZiAodHJhY2UgPT09IFRyYWNlLk9mZikge1xuICAgICAgICAgICAgICAgIHRyYWNlciA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIHRyYWNlciA9IF90cmFjZXI7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAoX3NlbmROb3RpZmljYXRpb24gJiYgIWlzQ2xvc2VkKCkgJiYgIWlzRGlzcG9zZWQoKSkge1xuICAgICAgICAgICAgICAgIGF3YWl0IGNvbm5lY3Rpb24uc2VuZE5vdGlmaWNhdGlvbihTZXRUcmFjZU5vdGlmaWNhdGlvbi50eXBlLCB7IHZhbHVlOiBUcmFjZS50b1N0cmluZyhfdmFsdWUpIH0pO1xuICAgICAgICAgICAgfVxuICAgICAgICB9LFxuICAgICAgICBvbkVycm9yOiBlcnJvckVtaXR0ZXIuZXZlbnQsXG4gICAgICAgIG9uQ2xvc2U6IGNsb3NlRW1pdHRlci5ldmVudCxcbiAgICAgICAgb25VbmhhbmRsZWROb3RpZmljYXRpb246IHVuaGFuZGxlZE5vdGlmaWNhdGlvbkVtaXR0ZXIuZXZlbnQsXG4gICAgICAgIG9uRGlzcG9zZTogZGlzcG9zZUVtaXR0ZXIuZXZlbnQsXG4gICAgICAgIGVuZDogKCkgPT4ge1xuICAgICAgICAgICAgbWVzc2FnZVdyaXRlci5lbmQoKTtcbiAgICAgICAgfSxcbiAgICAgICAgZGlzcG9zZTogKCkgPT4ge1xuICAgICAgICAgICAgaWYgKGlzRGlzcG9zZWQoKSkge1xuICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHN0YXRlID0gQ29ubmVjdGlvblN0YXRlLkRpc3Bvc2VkO1xuICAgICAgICAgICAgZGlzcG9zZUVtaXR0ZXIuZmlyZSh1bmRlZmluZWQpO1xuICAgICAgICAgICAgY29uc3QgZXJyb3IgPSBuZXcgbWVzc2FnZXNfMS5SZXNwb25zZUVycm9yKG1lc3NhZ2VzXzEuRXJyb3JDb2Rlcy5QZW5kaW5nUmVzcG9uc2VSZWplY3RlZCwgJ1BlbmRpbmcgcmVzcG9uc2UgcmVqZWN0ZWQgc2luY2UgY29ubmVjdGlvbiBnb3QgZGlzcG9zZWQnKTtcbiAgICAgICAgICAgIGZvciAoY29uc3QgcHJvbWlzZSBvZiByZXNwb25zZVByb21pc2VzLnZhbHVlcygpKSB7XG4gICAgICAgICAgICAgICAgcHJvbWlzZS5yZWplY3QoZXJyb3IpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgcmVzcG9uc2VQcm9taXNlcyA9IG5ldyBNYXAoKTtcbiAgICAgICAgICAgIHJlcXVlc3RUb2tlbnMgPSBuZXcgTWFwKCk7XG4gICAgICAgICAgICBrbm93bkNhbmNlbGVkUmVxdWVzdHMgPSBuZXcgU2V0KCk7XG4gICAgICAgICAgICBtZXNzYWdlUXVldWUgPSBuZXcgbGlua2VkTWFwXzEuTGlua2VkTWFwKCk7XG4gICAgICAgICAgICAvLyBUZXN0IGZvciBiYWNrd2FyZHMgY29tcGF0aWJpbGl0eVxuICAgICAgICAgICAgaWYgKElzLmZ1bmMobWVzc2FnZVdyaXRlci5kaXNwb3NlKSkge1xuICAgICAgICAgICAgICAgIG1lc3NhZ2VXcml0ZXIuZGlzcG9zZSgpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaWYgKElzLmZ1bmMobWVzc2FnZVJlYWRlci5kaXNwb3NlKSkge1xuICAgICAgICAgICAgICAgIG1lc3NhZ2VSZWFkZXIuZGlzcG9zZSgpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9LFxuICAgICAgICBsaXN0ZW46ICgpID0+IHtcbiAgICAgICAgICAgIHRocm93SWZDbG9zZWRPckRpc3Bvc2VkKCk7XG4gICAgICAgICAgICB0aHJvd0lmTGlzdGVuaW5nKCk7XG4gICAgICAgICAgICBzdGF0ZSA9IENvbm5lY3Rpb25TdGF0ZS5MaXN0ZW5pbmc7XG4gICAgICAgICAgICBtZXNzYWdlUmVhZGVyLmxpc3RlbihjYWxsYmFjayk7XG4gICAgICAgIH0sXG4gICAgICAgIGluc3BlY3Q6ICgpID0+IHtcbiAgICAgICAgICAgIC8vIGVzbGludC1kaXNhYmxlLW5leHQtbGluZSBuby1jb25zb2xlXG4gICAgICAgICAgICAoMCwgcmFsXzEuZGVmYXVsdCkoKS5jb25zb2xlLmxvZygnaW5zcGVjdCcpO1xuICAgICAgICB9XG4gICAgfTtcbiAgICBjb25uZWN0aW9uLm9uTm90aWZpY2F0aW9uKExvZ1RyYWNlTm90aWZpY2F0aW9uLnR5cGUsIChwYXJhbXMpID0+IHtcbiAgICAgICAgaWYgKHRyYWNlID09PSBUcmFjZS5PZmYgfHwgIXRyYWNlcikge1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGNvbnN0IHZlcmJvc2UgPSB0cmFjZSA9PT0gVHJhY2UuVmVyYm9zZSB8fCB0cmFjZSA9PT0gVHJhY2UuQ29tcGFjdDtcbiAgICAgICAgdHJhY2VyLmxvZyhwYXJhbXMubWVzc2FnZSwgdmVyYm9zZSA/IHBhcmFtcy52ZXJib3NlIDogdW5kZWZpbmVkKTtcbiAgICB9KTtcbiAgICBjb25uZWN0aW9uLm9uTm90aWZpY2F0aW9uKFByb2dyZXNzTm90aWZpY2F0aW9uLnR5cGUsIChwYXJhbXMpID0+IHtcbiAgICAgICAgY29uc3QgaGFuZGxlciA9IHByb2dyZXNzSGFuZGxlcnMuZ2V0KHBhcmFtcy50b2tlbik7XG4gICAgICAgIGlmIChoYW5kbGVyKSB7XG4gICAgICAgICAgICBoYW5kbGVyKHBhcmFtcy52YWx1ZSk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICB1bmhhbmRsZWRQcm9ncmVzc0VtaXR0ZXIuZmlyZShwYXJhbXMpO1xuICAgICAgICB9XG4gICAgfSk7XG4gICAgcmV0dXJuIGNvbm5lY3Rpb247XG59XG5leHBvcnRzLmNyZWF0ZU1lc3NhZ2VDb25uZWN0aW9uID0gY3JlYXRlTWVzc2FnZUNvbm5lY3Rpb247XG4iLCAiXCJ1c2Ugc3RyaWN0XCI7XG4vKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG4vLy8gPHJlZmVyZW5jZSBwYXRoPVwiLi4vLi4vdHlwaW5ncy90aGVuYWJsZS5kLnRzXCIgLz5cbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIl9fZXNNb2R1bGVcIiwgeyB2YWx1ZTogdHJ1ZSB9KTtcbmV4cG9ydHMuUHJvZ3Jlc3NUeXBlID0gZXhwb3J0cy5Qcm9ncmVzc1Rva2VuID0gZXhwb3J0cy5jcmVhdGVNZXNzYWdlQ29ubmVjdGlvbiA9IGV4cG9ydHMuTnVsbExvZ2dlciA9IGV4cG9ydHMuQ29ubmVjdGlvbk9wdGlvbnMgPSBleHBvcnRzLkNvbm5lY3Rpb25TdHJhdGVneSA9IGV4cG9ydHMuQWJzdHJhY3RNZXNzYWdlQnVmZmVyID0gZXhwb3J0cy5Xcml0ZWFibGVTdHJlYW1NZXNzYWdlV3JpdGVyID0gZXhwb3J0cy5BYnN0cmFjdE1lc3NhZ2VXcml0ZXIgPSBleHBvcnRzLk1lc3NhZ2VXcml0ZXIgPSBleHBvcnRzLlJlYWRhYmxlU3RyZWFtTWVzc2FnZVJlYWRlciA9IGV4cG9ydHMuQWJzdHJhY3RNZXNzYWdlUmVhZGVyID0gZXhwb3J0cy5NZXNzYWdlUmVhZGVyID0gZXhwb3J0cy5TaGFyZWRBcnJheVJlY2VpdmVyU3RyYXRlZ3kgPSBleHBvcnRzLlNoYXJlZEFycmF5U2VuZGVyU3RyYXRlZ3kgPSBleHBvcnRzLkNhbmNlbGxhdGlvblRva2VuID0gZXhwb3J0cy5DYW5jZWxsYXRpb25Ub2tlblNvdXJjZSA9IGV4cG9ydHMuRW1pdHRlciA9IGV4cG9ydHMuRXZlbnQgPSBleHBvcnRzLkRpc3Bvc2FibGUgPSBleHBvcnRzLkxSVUNhY2hlID0gZXhwb3J0cy5Ub3VjaCA9IGV4cG9ydHMuTGlua2VkTWFwID0gZXhwb3J0cy5QYXJhbWV0ZXJTdHJ1Y3R1cmVzID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlOSA9IGV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTggPSBleHBvcnRzLk5vdGlmaWNhdGlvblR5cGU3ID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlNiA9IGV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTUgPSBleHBvcnRzLk5vdGlmaWNhdGlvblR5cGU0ID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlMyA9IGV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTIgPSBleHBvcnRzLk5vdGlmaWNhdGlvblR5cGUxID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlMCA9IGV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZSA9IGV4cG9ydHMuRXJyb3JDb2RlcyA9IGV4cG9ydHMuUmVzcG9uc2VFcnJvciA9IGV4cG9ydHMuUmVxdWVzdFR5cGU5ID0gZXhwb3J0cy5SZXF1ZXN0VHlwZTggPSBleHBvcnRzLlJlcXVlc3RUeXBlNyA9IGV4cG9ydHMuUmVxdWVzdFR5cGU2ID0gZXhwb3J0cy5SZXF1ZXN0VHlwZTUgPSBleHBvcnRzLlJlcXVlc3RUeXBlNCA9IGV4cG9ydHMuUmVxdWVzdFR5cGUzID0gZXhwb3J0cy5SZXF1ZXN0VHlwZTIgPSBleHBvcnRzLlJlcXVlc3RUeXBlMSA9IGV4cG9ydHMuUmVxdWVzdFR5cGUwID0gZXhwb3J0cy5SZXF1ZXN0VHlwZSA9IGV4cG9ydHMuTWVzc2FnZSA9IGV4cG9ydHMuUkFMID0gdm9pZCAwO1xuZXhwb3J0cy5NZXNzYWdlU3RyYXRlZ3kgPSBleHBvcnRzLkNhbmNlbGxhdGlvblN0cmF0ZWd5ID0gZXhwb3J0cy5DYW5jZWxsYXRpb25TZW5kZXJTdHJhdGVneSA9IGV4cG9ydHMuQ2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneSA9IGV4cG9ydHMuQ29ubmVjdGlvbkVycm9yID0gZXhwb3J0cy5Db25uZWN0aW9uRXJyb3JzID0gZXhwb3J0cy5Mb2dUcmFjZU5vdGlmaWNhdGlvbiA9IGV4cG9ydHMuU2V0VHJhY2VOb3RpZmljYXRpb24gPSBleHBvcnRzLlRyYWNlRm9ybWF0ID0gZXhwb3J0cy5UcmFjZVZhbHVlcyA9IGV4cG9ydHMuVHJhY2UgPSB2b2lkIDA7XG5jb25zdCBtZXNzYWdlc18xID0gcmVxdWlyZShcIi4vbWVzc2FnZXNcIik7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJNZXNzYWdlXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLk1lc3NhZ2U7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJSZXF1ZXN0VHlwZVwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5SZXF1ZXN0VHlwZTsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlJlcXVlc3RUeXBlMFwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5SZXF1ZXN0VHlwZTA7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJSZXF1ZXN0VHlwZTFcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VzXzEuUmVxdWVzdFR5cGUxOyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiUmVxdWVzdFR5cGUyXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLlJlcXVlc3RUeXBlMjsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlJlcXVlc3RUeXBlM1wiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5SZXF1ZXN0VHlwZTM7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJSZXF1ZXN0VHlwZTRcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VzXzEuUmVxdWVzdFR5cGU0OyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiUmVxdWVzdFR5cGU1XCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLlJlcXVlc3RUeXBlNTsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlJlcXVlc3RUeXBlNlwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5SZXF1ZXN0VHlwZTY7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJSZXF1ZXN0VHlwZTdcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VzXzEuUmVxdWVzdFR5cGU3OyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiUmVxdWVzdFR5cGU4XCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLlJlcXVlc3RUeXBlODsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlJlcXVlc3RUeXBlOVwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5SZXF1ZXN0VHlwZTk7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJSZXNwb25zZUVycm9yXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLlJlc3BvbnNlRXJyb3I7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJFcnJvckNvZGVzXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLkVycm9yQ29kZXM7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJOb3RpZmljYXRpb25UeXBlXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLk5vdGlmaWNhdGlvblR5cGU7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJOb3RpZmljYXRpb25UeXBlMFwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5Ob3RpZmljYXRpb25UeXBlMDsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIk5vdGlmaWNhdGlvblR5cGUxXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLk5vdGlmaWNhdGlvblR5cGUxOyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiTm90aWZpY2F0aW9uVHlwZTJcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VzXzEuTm90aWZpY2F0aW9uVHlwZTI7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJOb3RpZmljYXRpb25UeXBlM1wiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5Ob3RpZmljYXRpb25UeXBlMzsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIk5vdGlmaWNhdGlvblR5cGU0XCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLk5vdGlmaWNhdGlvblR5cGU0OyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiTm90aWZpY2F0aW9uVHlwZTVcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VzXzEuTm90aWZpY2F0aW9uVHlwZTU7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJOb3RpZmljYXRpb25UeXBlNlwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5Ob3RpZmljYXRpb25UeXBlNjsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIk5vdGlmaWNhdGlvblR5cGU3XCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLk5vdGlmaWNhdGlvblR5cGU3OyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiTm90aWZpY2F0aW9uVHlwZThcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VzXzEuTm90aWZpY2F0aW9uVHlwZTg7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJOb3RpZmljYXRpb25UeXBlOVwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5Ob3RpZmljYXRpb25UeXBlOTsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlBhcmFtZXRlclN0cnVjdHVyZXNcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VzXzEuUGFyYW1ldGVyU3RydWN0dXJlczsgfSB9KTtcbmNvbnN0IGxpbmtlZE1hcF8xID0gcmVxdWlyZShcIi4vbGlua2VkTWFwXCIpO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiTGlua2VkTWFwXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBsaW5rZWRNYXBfMS5MaW5rZWRNYXA7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJMUlVDYWNoZVwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbGlua2VkTWFwXzEuTFJVQ2FjaGU7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJUb3VjaFwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbGlua2VkTWFwXzEuVG91Y2g7IH0gfSk7XG5jb25zdCBkaXNwb3NhYmxlXzEgPSByZXF1aXJlKFwiLi9kaXNwb3NhYmxlXCIpO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiRGlzcG9zYWJsZVwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gZGlzcG9zYWJsZV8xLkRpc3Bvc2FibGU7IH0gfSk7XG5jb25zdCBldmVudHNfMSA9IHJlcXVpcmUoXCIuL2V2ZW50c1wiKTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkV2ZW50XCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBldmVudHNfMS5FdmVudDsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkVtaXR0ZXJcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGV2ZW50c18xLkVtaXR0ZXI7IH0gfSk7XG5jb25zdCBjYW5jZWxsYXRpb25fMSA9IHJlcXVpcmUoXCIuL2NhbmNlbGxhdGlvblwiKTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkNhbmNlbGxhdGlvblRva2VuU291cmNlXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjYW5jZWxsYXRpb25fMS5DYW5jZWxsYXRpb25Ub2tlblNvdXJjZTsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkNhbmNlbGxhdGlvblRva2VuXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjYW5jZWxsYXRpb25fMS5DYW5jZWxsYXRpb25Ub2tlbjsgfSB9KTtcbmNvbnN0IHNoYXJlZEFycmF5Q2FuY2VsbGF0aW9uXzEgPSByZXF1aXJlKFwiLi9zaGFyZWRBcnJheUNhbmNlbGxhdGlvblwiKTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlNoYXJlZEFycmF5U2VuZGVyU3RyYXRlZ3lcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIHNoYXJlZEFycmF5Q2FuY2VsbGF0aW9uXzEuU2hhcmVkQXJyYXlTZW5kZXJTdHJhdGVneTsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlNoYXJlZEFycmF5UmVjZWl2ZXJTdHJhdGVneVwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gc2hhcmVkQXJyYXlDYW5jZWxsYXRpb25fMS5TaGFyZWRBcnJheVJlY2VpdmVyU3RyYXRlZ3k7IH0gfSk7XG5jb25zdCBtZXNzYWdlUmVhZGVyXzEgPSByZXF1aXJlKFwiLi9tZXNzYWdlUmVhZGVyXCIpO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiTWVzc2FnZVJlYWRlclwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZVJlYWRlcl8xLk1lc3NhZ2VSZWFkZXI7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJBYnN0cmFjdE1lc3NhZ2VSZWFkZXJcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VSZWFkZXJfMS5BYnN0cmFjdE1lc3NhZ2VSZWFkZXI7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJSZWFkYWJsZVN0cmVhbU1lc3NhZ2VSZWFkZXJcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VSZWFkZXJfMS5SZWFkYWJsZVN0cmVhbU1lc3NhZ2VSZWFkZXI7IH0gfSk7XG5jb25zdCBtZXNzYWdlV3JpdGVyXzEgPSByZXF1aXJlKFwiLi9tZXNzYWdlV3JpdGVyXCIpO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiTWVzc2FnZVdyaXRlclwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZVdyaXRlcl8xLk1lc3NhZ2VXcml0ZXI7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJBYnN0cmFjdE1lc3NhZ2VXcml0ZXJcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VXcml0ZXJfMS5BYnN0cmFjdE1lc3NhZ2VXcml0ZXI7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJXcml0ZWFibGVTdHJlYW1NZXNzYWdlV3JpdGVyXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlV3JpdGVyXzEuV3JpdGVhYmxlU3RyZWFtTWVzc2FnZVdyaXRlcjsgfSB9KTtcbmNvbnN0IG1lc3NhZ2VCdWZmZXJfMSA9IHJlcXVpcmUoXCIuL21lc3NhZ2VCdWZmZXJcIik7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJBYnN0cmFjdE1lc3NhZ2VCdWZmZXJcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VCdWZmZXJfMS5BYnN0cmFjdE1lc3NhZ2VCdWZmZXI7IH0gfSk7XG5jb25zdCBjb25uZWN0aW9uXzEgPSByZXF1aXJlKFwiLi9jb25uZWN0aW9uXCIpO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiQ29ubmVjdGlvblN0cmF0ZWd5XCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjb25uZWN0aW9uXzEuQ29ubmVjdGlvblN0cmF0ZWd5OyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiQ29ubmVjdGlvbk9wdGlvbnNcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5Db25uZWN0aW9uT3B0aW9uczsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIk51bGxMb2dnZXJcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5OdWxsTG9nZ2VyOyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiY3JlYXRlTWVzc2FnZUNvbm5lY3Rpb25cIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5jcmVhdGVNZXNzYWdlQ29ubmVjdGlvbjsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlByb2dyZXNzVG9rZW5cIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5Qcm9ncmVzc1Rva2VuOyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiUHJvZ3Jlc3NUeXBlXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjb25uZWN0aW9uXzEuUHJvZ3Jlc3NUeXBlOyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiVHJhY2VcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5UcmFjZTsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlRyYWNlVmFsdWVzXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjb25uZWN0aW9uXzEuVHJhY2VWYWx1ZXM7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJUcmFjZUZvcm1hdFwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gY29ubmVjdGlvbl8xLlRyYWNlRm9ybWF0OyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiU2V0VHJhY2VOb3RpZmljYXRpb25cIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5TZXRUcmFjZU5vdGlmaWNhdGlvbjsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkxvZ1RyYWNlTm90aWZpY2F0aW9uXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjb25uZWN0aW9uXzEuTG9nVHJhY2VOb3RpZmljYXRpb247IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJDb25uZWN0aW9uRXJyb3JzXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjb25uZWN0aW9uXzEuQ29ubmVjdGlvbkVycm9yczsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkNvbm5lY3Rpb25FcnJvclwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gY29ubmVjdGlvbl8xLkNvbm5lY3Rpb25FcnJvcjsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkNhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3lcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5DYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5OyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3lcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5DYW5jZWxsYXRpb25TZW5kZXJTdHJhdGVneTsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkNhbmNlbGxhdGlvblN0cmF0ZWd5XCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjb25uZWN0aW9uXzEuQ2FuY2VsbGF0aW9uU3RyYXRlZ3k7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJNZXNzYWdlU3RyYXRlZ3lcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5NZXNzYWdlU3RyYXRlZ3k7IH0gfSk7XG5jb25zdCByYWxfMSA9IHJlcXVpcmUoXCIuL3JhbFwiKTtcbmV4cG9ydHMuUkFMID0gcmFsXzEuZGVmYXVsdDtcbiIsICJcInVzZSBzdHJpY3RcIjtcbi8qIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiBDb3B5cmlnaHQgKGMpIE1pY3Jvc29mdCBDb3Jwb3JhdGlvbi4gQWxsIHJpZ2h0cyByZXNlcnZlZC5cbiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBNSVQgTGljZW5zZS4gU2VlIExpY2Vuc2UudHh0IGluIHRoZSBwcm9qZWN0IHJvb3QgZm9yIGxpY2Vuc2UgaW5mb3JtYXRpb24uXG4gKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0gKi9cbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIl9fZXNNb2R1bGVcIiwgeyB2YWx1ZTogdHJ1ZSB9KTtcbmNvbnN0IGFwaV8xID0gcmVxdWlyZShcIi4uL2NvbW1vbi9hcGlcIik7XG5jbGFzcyBNZXNzYWdlQnVmZmVyIGV4dGVuZHMgYXBpXzEuQWJzdHJhY3RNZXNzYWdlQnVmZmVyIHtcbiAgICBjb25zdHJ1Y3RvcihlbmNvZGluZyA9ICd1dGYtOCcpIHtcbiAgICAgICAgc3VwZXIoZW5jb2RpbmcpO1xuICAgICAgICB0aGlzLmFzY2lpRGVjb2RlciA9IG5ldyBUZXh0RGVjb2RlcignYXNjaWknKTtcbiAgICB9XG4gICAgZW1wdHlCdWZmZXIoKSB7XG4gICAgICAgIHJldHVybiBNZXNzYWdlQnVmZmVyLmVtcHR5QnVmZmVyO1xuICAgIH1cbiAgICBmcm9tU3RyaW5nKHZhbHVlLCBfZW5jb2RpbmcpIHtcbiAgICAgICAgcmV0dXJuIChuZXcgVGV4dEVuY29kZXIoKSkuZW5jb2RlKHZhbHVlKTtcbiAgICB9XG4gICAgdG9TdHJpbmcodmFsdWUsIGVuY29kaW5nKSB7XG4gICAgICAgIGlmIChlbmNvZGluZyA9PT0gJ2FzY2lpJykge1xuICAgICAgICAgICAgcmV0dXJuIHRoaXMuYXNjaWlEZWNvZGVyLmRlY29kZSh2YWx1ZSk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICByZXR1cm4gKG5ldyBUZXh0RGVjb2RlcihlbmNvZGluZykpLmRlY29kZSh2YWx1ZSk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgYXNOYXRpdmUoYnVmZmVyLCBsZW5ndGgpIHtcbiAgICAgICAgaWYgKGxlbmd0aCA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXR1cm4gYnVmZmVyO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgcmV0dXJuIGJ1ZmZlci5zbGljZSgwLCBsZW5ndGgpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGFsbG9jTmF0aXZlKGxlbmd0aCkge1xuICAgICAgICByZXR1cm4gbmV3IFVpbnQ4QXJyYXkobGVuZ3RoKTtcbiAgICB9XG59XG5NZXNzYWdlQnVmZmVyLmVtcHR5QnVmZmVyID0gbmV3IFVpbnQ4QXJyYXkoMCk7XG5jbGFzcyBSZWFkYWJsZVN0cmVhbVdyYXBwZXIge1xuICAgIGNvbnN0cnVjdG9yKHNvY2tldCkge1xuICAgICAgICB0aGlzLnNvY2tldCA9IHNvY2tldDtcbiAgICAgICAgdGhpcy5fb25EYXRhID0gbmV3IGFwaV8xLkVtaXR0ZXIoKTtcbiAgICAgICAgdGhpcy5fbWVzc2FnZUxpc3RlbmVyID0gKGV2ZW50KSA9PiB7XG4gICAgICAgICAgICBjb25zdCBibG9iID0gZXZlbnQuZGF0YTtcbiAgICAgICAgICAgIGJsb2IuYXJyYXlCdWZmZXIoKS50aGVuKChidWZmZXIpID0+IHtcbiAgICAgICAgICAgICAgICB0aGlzLl9vbkRhdGEuZmlyZShuZXcgVWludDhBcnJheShidWZmZXIpKTtcbiAgICAgICAgICAgIH0sICgpID0+IHtcbiAgICAgICAgICAgICAgICAoMCwgYXBpXzEuUkFMKSgpLmNvbnNvbGUuZXJyb3IoYENvbnZlcnRpbmcgYmxvYiB0byBhcnJheSBidWZmZXIgZmFpbGVkLmApO1xuICAgICAgICAgICAgfSk7XG4gICAgICAgIH07XG4gICAgICAgIHRoaXMuc29ja2V0LmFkZEV2ZW50TGlzdGVuZXIoJ21lc3NhZ2UnLCB0aGlzLl9tZXNzYWdlTGlzdGVuZXIpO1xuICAgIH1cbiAgICBvbkNsb3NlKGxpc3RlbmVyKSB7XG4gICAgICAgIHRoaXMuc29ja2V0LmFkZEV2ZW50TGlzdGVuZXIoJ2Nsb3NlJywgbGlzdGVuZXIpO1xuICAgICAgICByZXR1cm4gYXBpXzEuRGlzcG9zYWJsZS5jcmVhdGUoKCkgPT4gdGhpcy5zb2NrZXQucmVtb3ZlRXZlbnRMaXN0ZW5lcignY2xvc2UnLCBsaXN0ZW5lcikpO1xuICAgIH1cbiAgICBvbkVycm9yKGxpc3RlbmVyKSB7XG4gICAgICAgIHRoaXMuc29ja2V0LmFkZEV2ZW50TGlzdGVuZXIoJ2Vycm9yJywgbGlzdGVuZXIpO1xuICAgICAgICByZXR1cm4gYXBpXzEuRGlzcG9zYWJsZS5jcmVhdGUoKCkgPT4gdGhpcy5zb2NrZXQucmVtb3ZlRXZlbnRMaXN0ZW5lcignZXJyb3InLCBsaXN0ZW5lcikpO1xuICAgIH1cbiAgICBvbkVuZChsaXN0ZW5lcikge1xuICAgICAgICB0aGlzLnNvY2tldC5hZGRFdmVudExpc3RlbmVyKCdlbmQnLCBsaXN0ZW5lcik7XG4gICAgICAgIHJldHVybiBhcGlfMS5EaXNwb3NhYmxlLmNyZWF0ZSgoKSA9PiB0aGlzLnNvY2tldC5yZW1vdmVFdmVudExpc3RlbmVyKCdlbmQnLCBsaXN0ZW5lcikpO1xuICAgIH1cbiAgICBvbkRhdGEobGlzdGVuZXIpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX29uRGF0YS5ldmVudChsaXN0ZW5lcik7XG4gICAgfVxufVxuY2xhc3MgV3JpdGFibGVTdHJlYW1XcmFwcGVyIHtcbiAgICBjb25zdHJ1Y3Rvcihzb2NrZXQpIHtcbiAgICAgICAgdGhpcy5zb2NrZXQgPSBzb2NrZXQ7XG4gICAgfVxuICAgIG9uQ2xvc2UobGlzdGVuZXIpIHtcbiAgICAgICAgdGhpcy5zb2NrZXQuYWRkRXZlbnRMaXN0ZW5lcignY2xvc2UnLCBsaXN0ZW5lcik7XG4gICAgICAgIHJldHVybiBhcGlfMS5EaXNwb3NhYmxlLmNyZWF0ZSgoKSA9PiB0aGlzLnNvY2tldC5yZW1vdmVFdmVudExpc3RlbmVyKCdjbG9zZScsIGxpc3RlbmVyKSk7XG4gICAgfVxuICAgIG9uRXJyb3IobGlzdGVuZXIpIHtcbiAgICAgICAgdGhpcy5zb2NrZXQuYWRkRXZlbnRMaXN0ZW5lcignZXJyb3InLCBsaXN0ZW5lcik7XG4gICAgICAgIHJldHVybiBhcGlfMS5EaXNwb3NhYmxlLmNyZWF0ZSgoKSA9PiB0aGlzLnNvY2tldC5yZW1vdmVFdmVudExpc3RlbmVyKCdlcnJvcicsIGxpc3RlbmVyKSk7XG4gICAgfVxuICAgIG9uRW5kKGxpc3RlbmVyKSB7XG4gICAgICAgIHRoaXMuc29ja2V0LmFkZEV2ZW50TGlzdGVuZXIoJ2VuZCcsIGxpc3RlbmVyKTtcbiAgICAgICAgcmV0dXJuIGFwaV8xLkRpc3Bvc2FibGUuY3JlYXRlKCgpID0+IHRoaXMuc29ja2V0LnJlbW92ZUV2ZW50TGlzdGVuZXIoJ2VuZCcsIGxpc3RlbmVyKSk7XG4gICAgfVxuICAgIHdyaXRlKGRhdGEsIGVuY29kaW5nKSB7XG4gICAgICAgIGlmICh0eXBlb2YgZGF0YSA9PT0gJ3N0cmluZycpIHtcbiAgICAgICAgICAgIGlmIChlbmNvZGluZyAhPT0gdW5kZWZpbmVkICYmIGVuY29kaW5nICE9PSAndXRmLTgnKSB7XG4gICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBJbiBhIEJyb3dzZXIgZW52aXJvbm1lbnRzIG9ubHkgdXRmLTggdGV4dCBlbmNvZGluZyBpcyBzdXBwb3J0ZWQuIEJ1dCBnb3QgZW5jb2Rpbmc6ICR7ZW5jb2Rpbmd9YCk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICB0aGlzLnNvY2tldC5zZW5kKGRhdGEpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgdGhpcy5zb2NrZXQuc2VuZChkYXRhKTtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gUHJvbWlzZS5yZXNvbHZlKCk7XG4gICAgfVxuICAgIGVuZCgpIHtcbiAgICAgICAgdGhpcy5zb2NrZXQuY2xvc2UoKTtcbiAgICB9XG59XG5jb25zdCBfdGV4dEVuY29kZXIgPSBuZXcgVGV4dEVuY29kZXIoKTtcbmNvbnN0IF9yaWwgPSBPYmplY3QuZnJlZXplKHtcbiAgICBtZXNzYWdlQnVmZmVyOiBPYmplY3QuZnJlZXplKHtcbiAgICAgICAgY3JlYXRlOiAoZW5jb2RpbmcpID0+IG5ldyBNZXNzYWdlQnVmZmVyKGVuY29kaW5nKVxuICAgIH0pLFxuICAgIGFwcGxpY2F0aW9uSnNvbjogT2JqZWN0LmZyZWV6ZSh7XG4gICAgICAgIGVuY29kZXI6IE9iamVjdC5mcmVlemUoe1xuICAgICAgICAgICAgbmFtZTogJ2FwcGxpY2F0aW9uL2pzb24nLFxuICAgICAgICAgICAgZW5jb2RlOiAobXNnLCBvcHRpb25zKSA9PiB7XG4gICAgICAgICAgICAgICAgaWYgKG9wdGlvbnMuY2hhcnNldCAhPT0gJ3V0Zi04Jykge1xuICAgICAgICAgICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYEluIGEgQnJvd3NlciBlbnZpcm9ubWVudHMgb25seSB1dGYtOCB0ZXh0IGVuY29kaW5nIGlzIHN1cHBvcnRlZC4gQnV0IGdvdCBlbmNvZGluZzogJHtvcHRpb25zLmNoYXJzZXR9YCk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIHJldHVybiBQcm9taXNlLnJlc29sdmUoX3RleHRFbmNvZGVyLmVuY29kZShKU09OLnN0cmluZ2lmeShtc2csIHVuZGVmaW5lZCwgMCkpKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSksXG4gICAgICAgIGRlY29kZXI6IE9iamVjdC5mcmVlemUoe1xuICAgICAgICAgICAgbmFtZTogJ2FwcGxpY2F0aW9uL2pzb24nLFxuICAgICAgICAgICAgZGVjb2RlOiAoYnVmZmVyLCBvcHRpb25zKSA9PiB7XG4gICAgICAgICAgICAgICAgaWYgKCEoYnVmZmVyIGluc3RhbmNlb2YgVWludDhBcnJheSkpIHtcbiAgICAgICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBJbiBhIEJyb3dzZXIgZW52aXJvbm1lbnRzIG9ubHkgVWludDhBcnJheXMgYXJlIHN1cHBvcnRlZC5gKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgcmV0dXJuIFByb21pc2UucmVzb2x2ZShKU09OLnBhcnNlKG5ldyBUZXh0RGVjb2RlcihvcHRpb25zLmNoYXJzZXQpLmRlY29kZShidWZmZXIpKSk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0pXG4gICAgfSksXG4gICAgc3RyZWFtOiBPYmplY3QuZnJlZXplKHtcbiAgICAgICAgYXNSZWFkYWJsZVN0cmVhbTogKHNvY2tldCkgPT4gbmV3IFJlYWRhYmxlU3RyZWFtV3JhcHBlcihzb2NrZXQpLFxuICAgICAgICBhc1dyaXRhYmxlU3RyZWFtOiAoc29ja2V0KSA9PiBuZXcgV3JpdGFibGVTdHJlYW1XcmFwcGVyKHNvY2tldClcbiAgICB9KSxcbiAgICBjb25zb2xlOiBjb25zb2xlLFxuICAgIHRpbWVyOiBPYmplY3QuZnJlZXplKHtcbiAgICAgICAgc2V0VGltZW91dChjYWxsYmFjaywgbXMsIC4uLmFyZ3MpIHtcbiAgICAgICAgICAgIGNvbnN0IGhhbmRsZSA9IHNldFRpbWVvdXQoY2FsbGJhY2ssIG1zLCAuLi5hcmdzKTtcbiAgICAgICAgICAgIHJldHVybiB7IGRpc3Bvc2U6ICgpID0+IGNsZWFyVGltZW91dChoYW5kbGUpIH07XG4gICAgICAgIH0sXG4gICAgICAgIHNldEltbWVkaWF0ZShjYWxsYmFjaywgLi4uYXJncykge1xuICAgICAgICAgICAgY29uc3QgaGFuZGxlID0gc2V0VGltZW91dChjYWxsYmFjaywgMCwgLi4uYXJncyk7XG4gICAgICAgICAgICByZXR1cm4geyBkaXNwb3NlOiAoKSA9PiBjbGVhclRpbWVvdXQoaGFuZGxlKSB9O1xuICAgICAgICB9LFxuICAgICAgICBzZXRJbnRlcnZhbChjYWxsYmFjaywgbXMsIC4uLmFyZ3MpIHtcbiAgICAgICAgICAgIGNvbnN0IGhhbmRsZSA9IHNldEludGVydmFsKGNhbGxiYWNrLCBtcywgLi4uYXJncyk7XG4gICAgICAgICAgICByZXR1cm4geyBkaXNwb3NlOiAoKSA9PiBjbGVhckludGVydmFsKGhhbmRsZSkgfTtcbiAgICAgICAgfSxcbiAgICB9KVxufSk7XG5mdW5jdGlvbiBSSUwoKSB7XG4gICAgcmV0dXJuIF9yaWw7XG59XG4oZnVuY3Rpb24gKFJJTCkge1xuICAgIGZ1bmN0aW9uIGluc3RhbGwoKSB7XG4gICAgICAgIGFwaV8xLlJBTC5pbnN0YWxsKF9yaWwpO1xuICAgIH1cbiAgICBSSUwuaW5zdGFsbCA9IGluc3RhbGw7XG59KShSSUwgfHwgKFJJTCA9IHt9KSk7XG5leHBvcnRzLmRlZmF1bHQgPSBSSUw7XG4iLCAiXCJ1c2Ugc3RyaWN0XCI7XG4vKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG52YXIgX19jcmVhdGVCaW5kaW5nID0gKHRoaXMgJiYgdGhpcy5fX2NyZWF0ZUJpbmRpbmcpIHx8IChPYmplY3QuY3JlYXRlID8gKGZ1bmN0aW9uKG8sIG0sIGssIGsyKSB7XG4gICAgaWYgKGsyID09PSB1bmRlZmluZWQpIGsyID0gaztcbiAgICB2YXIgZGVzYyA9IE9iamVjdC5nZXRPd25Qcm9wZXJ0eURlc2NyaXB0b3IobSwgayk7XG4gICAgaWYgKCFkZXNjIHx8IChcImdldFwiIGluIGRlc2MgPyAhbS5fX2VzTW9kdWxlIDogZGVzYy53cml0YWJsZSB8fCBkZXNjLmNvbmZpZ3VyYWJsZSkpIHtcbiAgICAgIGRlc2MgPSB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24oKSB7IHJldHVybiBtW2tdOyB9IH07XG4gICAgfVxuICAgIE9iamVjdC5kZWZpbmVQcm9wZXJ0eShvLCBrMiwgZGVzYyk7XG59KSA6IChmdW5jdGlvbihvLCBtLCBrLCBrMikge1xuICAgIGlmIChrMiA9PT0gdW5kZWZpbmVkKSBrMiA9IGs7XG4gICAgb1trMl0gPSBtW2tdO1xufSkpO1xudmFyIF9fZXhwb3J0U3RhciA9ICh0aGlzICYmIHRoaXMuX19leHBvcnRTdGFyKSB8fCBmdW5jdGlvbihtLCBleHBvcnRzKSB7XG4gICAgZm9yICh2YXIgcCBpbiBtKSBpZiAocCAhPT0gXCJkZWZhdWx0XCIgJiYgIU9iamVjdC5wcm90b3R5cGUuaGFzT3duUHJvcGVydHkuY2FsbChleHBvcnRzLCBwKSkgX19jcmVhdGVCaW5kaW5nKGV4cG9ydHMsIG0sIHApO1xufTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIl9fZXNNb2R1bGVcIiwgeyB2YWx1ZTogdHJ1ZSB9KTtcbmV4cG9ydHMuY3JlYXRlTWVzc2FnZUNvbm5lY3Rpb24gPSBleHBvcnRzLkJyb3dzZXJNZXNzYWdlV3JpdGVyID0gZXhwb3J0cy5Ccm93c2VyTWVzc2FnZVJlYWRlciA9IHZvaWQgMDtcbmNvbnN0IHJpbF8xID0gcmVxdWlyZShcIi4vcmlsXCIpO1xuLy8gSW5zdGFsbCB0aGUgYnJvd3NlciBydW50aW1lIGFic3RyYWN0LlxucmlsXzEuZGVmYXVsdC5pbnN0YWxsKCk7XG5jb25zdCBhcGlfMSA9IHJlcXVpcmUoXCIuLi9jb21tb24vYXBpXCIpO1xuX19leHBvcnRTdGFyKHJlcXVpcmUoXCIuLi9jb21tb24vYXBpXCIpLCBleHBvcnRzKTtcbmNsYXNzIEJyb3dzZXJNZXNzYWdlUmVhZGVyIGV4dGVuZHMgYXBpXzEuQWJzdHJhY3RNZXNzYWdlUmVhZGVyIHtcbiAgICBjb25zdHJ1Y3Rvcihwb3J0KSB7XG4gICAgICAgIHN1cGVyKCk7XG4gICAgICAgIHRoaXMuX29uRGF0YSA9IG5ldyBhcGlfMS5FbWl0dGVyKCk7XG4gICAgICAgIHRoaXMuX21lc3NhZ2VMaXN0ZW5lciA9IChldmVudCkgPT4ge1xuICAgICAgICAgICAgdGhpcy5fb25EYXRhLmZpcmUoZXZlbnQuZGF0YSk7XG4gICAgICAgIH07XG4gICAgICAgIHBvcnQuYWRkRXZlbnRMaXN0ZW5lcignZXJyb3InLCAoZXZlbnQpID0+IHRoaXMuZmlyZUVycm9yKGV2ZW50KSk7XG4gICAgICAgIHBvcnQub25tZXNzYWdlID0gdGhpcy5fbWVzc2FnZUxpc3RlbmVyO1xuICAgIH1cbiAgICBsaXN0ZW4oY2FsbGJhY2spIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX29uRGF0YS5ldmVudChjYWxsYmFjayk7XG4gICAgfVxufVxuZXhwb3J0cy5Ccm93c2VyTWVzc2FnZVJlYWRlciA9IEJyb3dzZXJNZXNzYWdlUmVhZGVyO1xuY2xhc3MgQnJvd3Nlck1lc3NhZ2VXcml0ZXIgZXh0ZW5kcyBhcGlfMS5BYnN0cmFjdE1lc3NhZ2VXcml0ZXIge1xuICAgIGNvbnN0cnVjdG9yKHBvcnQpIHtcbiAgICAgICAgc3VwZXIoKTtcbiAgICAgICAgdGhpcy5wb3J0ID0gcG9ydDtcbiAgICAgICAgdGhpcy5lcnJvckNvdW50ID0gMDtcbiAgICAgICAgcG9ydC5hZGRFdmVudExpc3RlbmVyKCdlcnJvcicsIChldmVudCkgPT4gdGhpcy5maXJlRXJyb3IoZXZlbnQpKTtcbiAgICB9XG4gICAgd3JpdGUobXNnKSB7XG4gICAgICAgIHRyeSB7XG4gICAgICAgICAgICB0aGlzLnBvcnQucG9zdE1lc3NhZ2UobXNnKTtcbiAgICAgICAgICAgIHJldHVybiBQcm9taXNlLnJlc29sdmUoKTtcbiAgICAgICAgfVxuICAgICAgICBjYXRjaCAoZXJyb3IpIHtcbiAgICAgICAgICAgIHRoaXMuaGFuZGxlRXJyb3IoZXJyb3IsIG1zZyk7XG4gICAgICAgICAgICByZXR1cm4gUHJvbWlzZS5yZWplY3QoZXJyb3IpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGhhbmRsZUVycm9yKGVycm9yLCBtc2cpIHtcbiAgICAgICAgdGhpcy5lcnJvckNvdW50Kys7XG4gICAgICAgIHRoaXMuZmlyZUVycm9yKGVycm9yLCBtc2csIHRoaXMuZXJyb3JDb3VudCk7XG4gICAgfVxuICAgIGVuZCgpIHtcbiAgICB9XG59XG5leHBvcnRzLkJyb3dzZXJNZXNzYWdlV3JpdGVyID0gQnJvd3Nlck1lc3NhZ2VXcml0ZXI7XG5mdW5jdGlvbiBjcmVhdGVNZXNzYWdlQ29ubmVjdGlvbihyZWFkZXIsIHdyaXRlciwgbG9nZ2VyLCBvcHRpb25zKSB7XG4gICAgaWYgKGxvZ2dlciA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgIGxvZ2dlciA9IGFwaV8xLk51bGxMb2dnZXI7XG4gICAgfVxuICAgIGlmIChhcGlfMS5Db25uZWN0aW9uU3RyYXRlZ3kuaXMob3B0aW9ucykpIHtcbiAgICAgICAgb3B0aW9ucyA9IHsgY29ubmVjdGlvblN0cmF0ZWd5OiBvcHRpb25zIH07XG4gICAgfVxuICAgIHJldHVybiAoMCwgYXBpXzEuY3JlYXRlTWVzc2FnZUNvbm5lY3Rpb24pKHJlYWRlciwgd3JpdGVyLCBsb2dnZXIsIG9wdGlvbnMpO1xufVxuZXhwb3J0cy5jcmVhdGVNZXNzYWdlQ29ubmVjdGlvbiA9IGNyZWF0ZU1lc3NhZ2VDb25uZWN0aW9uO1xuIiwgIi8qIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiBDb3B5cmlnaHQgKGMpIE1pY3Jvc29mdCBDb3Jwb3JhdGlvbi4gQWxsIHJpZ2h0cyByZXNlcnZlZC5cbiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBNSVQgTGljZW5zZS4gU2VlIExpY2Vuc2UudHh0IGluIHRoZSBwcm9qZWN0IHJvb3QgZm9yIGxpY2Vuc2UgaW5mb3JtYXRpb24uXG4gKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSAqL1xuJ3VzZSBzdHJpY3QnO1xuXG5tb2R1bGUuZXhwb3J0cyA9IHJlcXVpcmUoJy4vbGliL2Jyb3dzZXIvbWFpbicpOyIsICIvKlxuICogQ29weXJpZ2h0IChjKSAyMDEwLTIwMjYgRWNsaXBzZSBEaXJpZ2libGUgY29udHJpYnV0b3JzXG4gKlxuICogQWxsIHJpZ2h0cyByZXNlcnZlZC4gVGhpcyBwcm9ncmFtIGFuZCB0aGUgYWNjb21wYW55aW5nIG1hdGVyaWFscyBhcmUgbWFkZSBhdmFpbGFibGUgdW5kZXIgdGhlXG4gKiB0ZXJtcyBvZiB0aGUgRWNsaXBzZSBQdWJsaWMgTGljZW5zZSB2Mi4wIHdoaWNoIGFjY29tcGFuaWVzIHRoaXMgZGlzdHJpYnV0aW9uLCBhbmQgaXMgYXZhaWxhYmxlIGF0XG4gKiBodHRwOi8vd3d3LmVjbGlwc2Uub3JnL2xlZ2FsL2VwbC12MjAuaHRtbFxuICpcbiAqIFNQRFgtRmlsZUNvcHlyaWdodFRleHQ6IEVjbGlwc2UgRGlyaWdpYmxlIGNvbnRyaWJ1dG9ycyBTUERYLUxpY2Vuc2UtSWRlbnRpZmllcjogRVBMLTIuMFxuICovXG5cbi8qKlxuICogSmF2YSBMU1AgY2xpZW50IGJ1bmRsZS5cbiAqXG4gKiBVc2VzIHZzY29kZS13cy1qc29ucnBjIGZvciB0eXBlZCBKU09OLVJQQyBvdmVyIFdlYlNvY2tldCBhbmQgcmVnaXN0ZXJzIE1vbmFjb1xuICogcHJvdmlkZXJzIHRoYXQgZGVsZWdhdGUgdG8gSkRULkxTLiBFeHBvc2VkIGFzIGdsb2JhbCBKYXZhTHNwQ2xpZW50TGliIHNvIHRoYXRcbiAqIGVkaXRvci5qcyBjYW4gY2FsbCBKYXZhTHNwQ2xpZW50TGliLmNvbm5lY3QocmVzb3VyY2VQYXRoKS5cbiAqXG4gKiBPbmUgSkRULkxTIHByb2Nlc3MgY292ZXJzIHRoZSBlbnRpcmUgd29ya3NwYWNlLCBzbyBhIHNpbmdsZSBXZWJTb2NrZXQgY29ubmVjdGlvblxuICogaXMgc2hhcmVkIGFjcm9zcyBhbGwgSmF2YSBmaWxlcyBvcGVuIGluIHRoZSBzYW1lIGJyb3dzZXIgcGFnZS4gVGhlIGNvbm5lY3Rpb24gaXNcbiAqIGVzdGFibGlzaGVkIG9uIHRoZSBmaXJzdCBjb25uZWN0KCkgY2FsbCBhbmQgcmV1c2VkIGZvciBhbGwgc3Vic2VxdWVudCBjYWxscy5cbiAqXG4gKiBlZGl0b3IuanMgc2V0cyB3aW5kb3cubW9uYWNvIGJlZm9yZSBjYWxsaW5nIGNvbm5lY3QoKSwgc28gdGhlIG1vbmFjby1zaGltJ3MgbGF6eVxuICogUHJveGllcyByZXNvbHZlIGNvcnJlY3RseSBhdCBjYWxsIHRpbWUuXG4gKi9cblxuaW1wb3J0IHsgY3JlYXRlTWVzc2FnZUNvbm5lY3Rpb24sIE1lc3NhZ2VDb25uZWN0aW9uIH0gZnJvbSAndnNjb2RlLWpzb25ycGMvYnJvd3Nlcic7XG5pbXBvcnQgeyB0b1NvY2tldCwgV2ViU29ja2V0TWVzc2FnZVJlYWRlciwgV2ViU29ja2V0TWVzc2FnZVdyaXRlciB9IGZyb20gJ3ZzY29kZS13cy1qc29ucnBjJztcbmltcG9ydCAqIGFzIG1vbmFjbyBmcm9tICdtb25hY28tZWRpdG9yJztcbmltcG9ydCB7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLFxuICAgIERpYWdub3N0aWNTZXZlcml0eSxcbiAgICBJbnNlcnRUZXh0Rm9ybWF0LFxuICAgIE1hcmt1cENvbnRlbnQsXG4gICAgTWFya3VwS2luZCxcbiAgICB0eXBlIENvZGVBY3Rpb24sXG4gICAgdHlwZSBDb21tYW5kLFxuICAgIHR5cGUgQ29tcGxldGlvbkl0ZW0sXG4gICAgdHlwZSBDb21wbGV0aW9uTGlzdCxcbiAgICB0eXBlIERpYWdub3N0aWMsXG4gICAgdHlwZSBIb3ZlcixcbiAgICB0eXBlIExvY2F0aW9uLFxuICAgIHR5cGUgUGFyYW1ldGVySW5mb3JtYXRpb24sXG4gICAgdHlwZSBTaWduYXR1cmVIZWxwLFxuICAgIHR5cGUgU2lnbmF0dXJlSW5mb3JtYXRpb24sXG4gICAgdHlwZSBUZXh0RWRpdCxcbiAgICB0eXBlIFdvcmtzcGFjZUVkaXQsXG59IGZyb20gJ3ZzY29kZS1sYW5ndWFnZXNlcnZlci10eXBlcyc7XG5cbi8vIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbi8vIFNpbmdsZXRvbiBzdGF0ZVxuLy8gLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuXG4vKiogU2hhcmVkIGNvbm5lY3Rpb24gXHUyMDE0IG9uZSBwZXIgYnJvd3NlciBwYWdlLCBjb3ZlcmluZyBhbGwgcHJvamVjdHMgaW4gdGhlIHdvcmtzcGFjZS4gKi9cbmxldCBfY29ubjogTWVzc2FnZUNvbm5lY3Rpb24gfCBudWxsID0gbnVsbDtcblxuLyoqXG4gKiBWaXJ0dWFsIHdvcmtzcGFjZSByb290IFVSSSwgZS5nLiB7QGNvZGUgZmlsZTovLy93b3Jrc3BhY2Uvd29ya3NwYWNlL30uXG4gKiBTZXQgb25jZSBvbiBmaXJzdCBjb25uZWN0KCk7IHVzZWQgdG8gc2NvcGUgTW9uYWNvIHByb3ZpZGVycyB0byB3b3Jrc3BhY2UgZmlsZXMuXG4gKi9cbmxldCBfd29ya3NwYWNlUm9vdCA9ICcnO1xuXG4vKiogVVJJcyBmb3Igd2hpY2ggdGV4dERvY3VtZW50L2RpZE9wZW4gaGFzIGFscmVhZHkgYmVlbiBzZW50LiAqL1xuY29uc3QgX29wZW5GaWxlczogU2V0PHN0cmluZz4gPSBuZXcgU2V0KCk7XG5cbmxldCBfcHJvdmlkZXJzUmVnaXN0ZXJlZCA9IGZhbHNlO1xuXG4vLyAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4vLyBQdWJsaWMgQVBJXG4vLyAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG5cbi8qKiBDYWxsZWQgYnkgZWRpdG9yLmpzIHdoZW4gYSBKYXZhIGZpbGUgaXMgb3BlbmVkLiBTYWZlIHRvIGNhbGwgbXVsdGlwbGUgdGltZXMuICovXG5leHBvcnQgYXN5bmMgZnVuY3Rpb24gY29ubmVjdChyZXNvdXJjZVBhdGg6IHN0cmluZyk6IFByb21pc2U8dm9pZD4ge1xuICAgIGNvbnN0IHBhcnRzID0gcmVzb3VyY2VQYXRoLnJlcGxhY2UoL15cXC8vLCAnJykuc3BsaXQoJy8nKTtcbiAgICBjb25zdCB3b3Jrc3BhY2UgPSBwYXJ0c1swXTtcbiAgICBjb25zdCBwcm9qZWN0ICAgPSBwYXJ0c1sxXTtcbiAgICBjb25zdCBmaWxlVXJpID0gYGZpbGU6Ly8vd29ya3NwYWNlLyR7d29ya3NwYWNlfS8ke3Byb2plY3R9LyR7cGFydHMuc2xpY2UoMikuam9pbignLycpfWA7XG5cbiAgICBpZiAoX2Nvbm4pIHtcbiAgICAgICAgLy8gQ29ubmVjdGlvbiBhbHJlYWR5IGVzdGFibGlzaGVkIFx1MjAxNCBqdXN0IG9wZW4gdGhlIG5ldyBmaWxlLlxuICAgICAgICBvcGVuRmlsZShmaWxlVXJpKTtcbiAgICAgICAgcmV0dXJuO1xuICAgIH1cblxuICAgIF93b3Jrc3BhY2VSb290ID0gYGZpbGU6Ly8vd29ya3NwYWNlLyR7d29ya3NwYWNlfS9gO1xuXG4gICAgY29uc3QgcHJvdG8gPSBsb2NhdGlvbi5wcm90b2NvbCA9PT0gJ2h0dHBzOicgPyAnd3NzJyA6ICd3cyc7XG4gICAgY29uc3Qgd3NVcmwgPSBgJHtwcm90b306Ly8ke2xvY2F0aW9uLmhvc3R9L3dlYnNvY2tldHMvaWRlL2phdmEtbHNwYFxuICAgICAgICAgICAgICAgICsgYD93b3Jrc3BhY2U9JHtlbmNvZGVVUklDb21wb25lbnQod29ya3NwYWNlKX1gO1xuXG4gICAgY29uc3Qgd3MgPSBuZXcgV2ViU29ja2V0KHdzVXJsKTtcbiAgICBhd2FpdCBuZXcgUHJvbWlzZTx2b2lkPigocmVzb2x2ZSwgcmVqZWN0KSA9PiB7XG4gICAgICAgIHdzLm9ub3BlbiAgPSAoKSA9PiByZXNvbHZlKCk7XG4gICAgICAgIHdzLm9uZXJyb3IgPSAoKSA9PiByZWplY3QobmV3IEVycm9yKGBbamF2YS1sc3BdIFdlYlNvY2tldCBjb25uZWN0IGZhaWxlZDogJHt3c1VybH1gKSk7XG4gICAgfSk7XG5cbiAgICBjb25zdCBzb2NrZXQgPSB0b1NvY2tldCh3cyk7XG4gICAgY29uc3QgcmVhZGVyID0gbmV3IFdlYlNvY2tldE1lc3NhZ2VSZWFkZXIoc29ja2V0KTtcbiAgICBjb25zdCB3cml0ZXIgPSBuZXcgV2ViU29ja2V0TWVzc2FnZVdyaXRlcihzb2NrZXQpO1xuICAgIF9jb25uID0gY3JlYXRlTWVzc2FnZUNvbm5lY3Rpb24ocmVhZGVyLCB3cml0ZXIpO1xuXG4gICAgLy8gRGlhZ25vc3RpY3Mgbm90aWZpY2F0aW9uIFx1MjE5MiBNb25hY28gbWFya2VycyAoYXBwbGllcyB0byBhbnkgd29ya3NwYWNlIGZpbGUpXG4gICAgX2Nvbm4ub25Ob3RpZmljYXRpb24oJ3RleHREb2N1bWVudC9wdWJsaXNoRGlhZ25vc3RpY3MnLCAocGFyYW1zOiB7IHVyaTogc3RyaW5nOyBkaWFnbm9zdGljczogRGlhZ25vc3RpY1tdIH0pID0+IHtcbiAgICAgICAgY29uc3QgbW9kZWwgPSBtb25hY28uZWRpdG9yLmdldE1vZGVscygpLmZpbmQobSA9PiBtLnVyaS50b1N0cmluZygpID09PSBwYXJhbXMudXJpKTtcbiAgICAgICAgaWYgKCFtb2RlbCkgcmV0dXJuO1xuICAgICAgICBtb25hY28uZWRpdG9yLnNldE1vZGVsTWFya2Vycyhtb2RlbCwgJ2phdmEtbHNwJywgcGFyYW1zLmRpYWdub3N0aWNzLm1hcChkID0+ICh7XG4gICAgICAgICAgICBzZXZlcml0eTogICAgICAgIGxzcFNldmVyaXR5KGQuc2V2ZXJpdHkpLFxuICAgICAgICAgICAgbWVzc2FnZTogICAgICAgICBkLm1lc3NhZ2UsXG4gICAgICAgICAgICBzb3VyY2U6ICAgICAgICAgIGQuc291cmNlID8/ICdqYXZhJyxcbiAgICAgICAgICAgIHN0YXJ0TGluZU51bWJlcjogZC5yYW5nZS5zdGFydC5saW5lICsgMSxcbiAgICAgICAgICAgIHN0YXJ0Q29sdW1uOiAgICAgZC5yYW5nZS5zdGFydC5jaGFyYWN0ZXIgKyAxLFxuICAgICAgICAgICAgZW5kTGluZU51bWJlcjogICBkLnJhbmdlLmVuZC5saW5lICsgMSxcbiAgICAgICAgICAgIGVuZENvbHVtbjogICAgICAgZC5yYW5nZS5lbmQuY2hhcmFjdGVyICsgMSxcbiAgICAgICAgfSkpKTtcbiAgICB9KTtcblxuICAgIC8vIFNlcnZlciAtPiBjbGllbnQgcmVxdWVzdHMgdGhhdCBkcml2ZSByZWZhY3RvciAvIGdlbmVyYXRlIHJlc3VsdHMgYW5kIGR5bmFtaWMgcmVnaXN0cmF0aW9uLlxuICAgIF9jb25uLm9uUmVxdWVzdCgnd29ya3NwYWNlL2FwcGx5RWRpdCcsIChwYXJhbXM6IHsgZWRpdDogV29ya3NwYWNlRWRpdCB9KSA9PiB7XG4gICAgICAgIGFwcGx5V29ya3NwYWNlRWRpdChwYXJhbXMuZWRpdCk7XG4gICAgICAgIHJldHVybiB7IGFwcGxpZWQ6IHRydWUgfTtcbiAgICB9KTtcbiAgICBfY29ubi5vblJlcXVlc3QoJ3dvcmtzcGFjZS9jb25maWd1cmF0aW9uJywgKHBhcmFtczogeyBpdGVtczogQXJyYXk8eyBzZWN0aW9uPzogc3RyaW5nIH0+IH0pID0+XG4gICAgICAgIChwYXJhbXMuaXRlbXMgPz8gW10pLm1hcCgoKSA9PiBqZHRsc1NldHRpbmdzKCkuamF2YSkpO1xuICAgIF9jb25uLm9uUmVxdWVzdCgnY2xpZW50L3JlZ2lzdGVyQ2FwYWJpbGl0eScsICgpID0+IG51bGwpO1xuICAgIF9jb25uLm9uUmVxdWVzdCgnY2xpZW50L3VucmVnaXN0ZXJDYXBhYmlsaXR5JywgKCkgPT4gbnVsbCk7XG4gICAgX2Nvbm4ub25SZXF1ZXN0KCd3aW5kb3cvc2hvd01lc3NhZ2VSZXF1ZXN0JywgKCkgPT4gbnVsbCk7XG4gICAgX2Nvbm4ub25SZXF1ZXN0KCd3aW5kb3cvd29ya0RvbmVQcm9ncmVzcy9jcmVhdGUnLCAoKSA9PiBudWxsKTtcbiAgICBfY29ubi5vbk5vdGlmaWNhdGlvbignd2luZG93L2xvZ01lc3NhZ2UnLCAocDogeyBtZXNzYWdlOiBzdHJpbmcgfSkgPT4gY29uc29sZS5kZWJ1ZygnW2phdmEtbHNwXScsIHA/Lm1lc3NhZ2UpKTtcbiAgICBfY29ubi5vbk5vdGlmaWNhdGlvbignd2luZG93L3Nob3dNZXNzYWdlJywgKHA6IHsgbWVzc2FnZTogc3RyaW5nIH0pID0+IGNvbnNvbGUuaW5mbygnW2phdmEtbHNwXScsIHA/Lm1lc3NhZ2UpKTtcbiAgICAvLyBKRFQuTFMgbGFuZ3VhZ2Utc3RhdHVzIC8gcHJvZ3Jlc3Mgbm90aWZpY2F0aW9ucyBcdTIwMTQgYWNrbm93bGVkZ2VkIHNpbGVudGx5LlxuICAgIF9jb25uLm9uTm90aWZpY2F0aW9uKCdsYW5ndWFnZS9zdGF0dXMnLCAoKSA9PiB7IC8qIGluZGV4aW5nL3JlYWR5IHN0YXR1cywgaWdub3JlZCAqLyB9KTtcbiAgICBfY29ubi5vbk5vdGlmaWNhdGlvbignbGFuZ3VhZ2UvcHJvZ3Jlc3NSZXBvcnQnLCAoKSA9PiB7IC8qIGJ1aWxkIHByb2dyZXNzLCBpZ25vcmVkICovIH0pO1xuXG4gICAgX2Nvbm4ubGlzdGVuKCk7XG5cbiAgICBjb25zdCByb290VXJpID0gX3dvcmtzcGFjZVJvb3Q7XG4gICAgYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ2luaXRpYWxpemUnLCB7XG4gICAgICAgIHByb2Nlc3NJZDogbnVsbCxcbiAgICAgICAgcm9vdFVyaSxcbiAgICAgICAgaW5pdGlhbGl6YXRpb25PcHRpb25zOiB7XG4gICAgICAgICAgICBzZXR0aW5nczogamR0bHNTZXR0aW5ncygpLFxuICAgICAgICAgICAgZXh0ZW5kZWRDbGllbnRDYXBhYmlsaXRpZXM6IHtcbiAgICAgICAgICAgICAgICBwcm9ncmVzc1JlcG9ydFByb3ZpZGVyOiAgICAgICAgICAgIGZhbHNlLFxuICAgICAgICAgICAgICAgIGNsYXNzRmlsZUNvbnRlbnRzU3VwcG9ydDogICAgICAgICAgdHJ1ZSxcbiAgICAgICAgICAgICAgICByZXNvbHZlQWRkaXRpb25hbFRleHRFZGl0c1N1cHBvcnQ6IHRydWUsXG4gICAgICAgICAgICAgICAgYWR2YW5jZWRHZW5lcmF0ZUFjY2Vzc29yc1N1cHBvcnQ6ICB0cnVlLFxuICAgICAgICAgICAgICAgIGdlbmVyYXRlVG9TdHJpbmdQcm9tcHRTdXBwb3J0OiAgICAgdHJ1ZSxcbiAgICAgICAgICAgICAgICBnZW5lcmF0ZUNvbnN0cnVjdG9yc1Byb21wdFN1cHBvcnQ6IHRydWUsXG4gICAgICAgICAgICAgICAgZ2VuZXJhdGVEZWxlZ2F0ZU1ldGhvZHNQcm9tcHRTdXBwb3J0OiB0cnVlLFxuICAgICAgICAgICAgICAgIGhhc2hDb2RlRXF1YWxzUHJvbXB0U3VwcG9ydDogICAgICAgdHJ1ZSxcbiAgICAgICAgICAgICAgICBhZHZhbmNlZE9yZ2FuaXplSW1wb3J0c1N1cHBvcnQ6ICAgIHRydWUsXG4gICAgICAgICAgICAgICAgaW5mZXJTZWxlY3Rpb25TdXBwb3J0OiAgICAgICAgICAgICBbJ2V4dHJhY3RNZXRob2QnLCAnZXh0cmFjdFZhcmlhYmxlJywgJ2V4dHJhY3RGaWVsZCddLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgfSxcbiAgICAgICAgd29ya3NwYWNlRm9sZGVyczogW3sgdXJpOiByb290VXJpLCBuYW1lOiB3b3Jrc3BhY2UgfV0sXG4gICAgICAgIGNhcGFiaWxpdGllczoge1xuICAgICAgICAgICAgdGV4dERvY3VtZW50OiB7XG4gICAgICAgICAgICAgICAgc3luY2hyb25pemF0aW9uOiB7IGR5bmFtaWNSZWdpc3RyYXRpb246IHRydWUsIHdpbGxTYXZlOiBmYWxzZSwgZGlkU2F2ZTogdHJ1ZSwgd2lsbFNhdmVXYWl0VW50aWw6IGZhbHNlIH0sXG4gICAgICAgICAgICAgICAgY29tcGxldGlvbjoge1xuICAgICAgICAgICAgICAgICAgICBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlLFxuICAgICAgICAgICAgICAgICAgICBjb21wbGV0aW9uSXRlbToge1xuICAgICAgICAgICAgICAgICAgICAgICAgc25pcHBldFN1cHBvcnQ6ICAgICAgICB0cnVlLFxuICAgICAgICAgICAgICAgICAgICAgICAgZG9jdW1lbnRhdGlvbkZvcm1hdDogICBbJ21hcmtkb3duJywgJ3BsYWludGV4dCddLFxuICAgICAgICAgICAgICAgICAgICAgICAgZGVwcmVjYXRlZFN1cHBvcnQ6ICAgICB0cnVlLFxuICAgICAgICAgICAgICAgICAgICAgICAgY29tbWl0Q2hhcmFjdGVyc1N1cHBvcnQ6IHRydWUsXG4gICAgICAgICAgICAgICAgICAgICAgICByZXNvbHZlU3VwcG9ydDogICAgICAgIHsgcHJvcGVydGllczogWydkb2N1bWVudGF0aW9uJywgJ2RldGFpbCcsICdhZGRpdGlvbmFsVGV4dEVkaXRzJ10gfSxcbiAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgY29udGV4dFN1cHBvcnQ6IHRydWUsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICBob3ZlcjogICAgICAgICAgeyBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlLCBjb250ZW50Rm9ybWF0OiBbJ21hcmtkb3duJywgJ3BsYWludGV4dCddIH0sXG4gICAgICAgICAgICAgICAgc2lnbmF0dXJlSGVscDogIHsgZHluYW1pY1JlZ2lzdHJhdGlvbjogdHJ1ZSwgc2lnbmF0dXJlSW5mb3JtYXRpb246IHsgZG9jdW1lbnRhdGlvbkZvcm1hdDogWydtYXJrZG93bicsICdwbGFpbnRleHQnXSwgcGFyYW1ldGVySW5mb3JtYXRpb246IHsgbGFiZWxPZmZzZXRTdXBwb3J0OiB0cnVlIH0gfSB9LFxuICAgICAgICAgICAgICAgIGRlZmluaXRpb246ICAgICB7IGR5bmFtaWNSZWdpc3RyYXRpb246IHRydWUgfSxcbiAgICAgICAgICAgICAgICByZWZlcmVuY2VzOiAgICAgeyBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlIH0sXG4gICAgICAgICAgICAgICAgZG9jdW1lbnRTeW1ib2w6IHsgZHluYW1pY1JlZ2lzdHJhdGlvbjogdHJ1ZSB9LFxuICAgICAgICAgICAgICAgIGZvcm1hdHRpbmc6ICAgICB7IGR5bmFtaWNSZWdpc3RyYXRpb246IHRydWUgfSxcbiAgICAgICAgICAgICAgICByYW5nZUZvcm1hdHRpbmc6IHsgZHluYW1pY1JlZ2lzdHJhdGlvbjogdHJ1ZSB9LFxuICAgICAgICAgICAgICAgIHJlbmFtZTogICAgICAgICB7IGR5bmFtaWNSZWdpc3RyYXRpb246IHRydWUsIHByZXBhcmVTdXBwb3J0OiB0cnVlIH0sXG4gICAgICAgICAgICAgICAgY29kZUFjdGlvbjoge1xuICAgICAgICAgICAgICAgICAgICBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlLFxuICAgICAgICAgICAgICAgICAgICBjb2RlQWN0aW9uTGl0ZXJhbFN1cHBvcnQ6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGNvZGVBY3Rpb25LaW5kOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFsdWVTZXQ6IFsncXVpY2tmaXgnLCAncmVmYWN0b3InLCAncmVmYWN0b3IuZXh0cmFjdCcsICdyZWZhY3Rvci5pbmxpbmUnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAncmVmYWN0b3IucmV3cml0ZScsICdzb3VyY2UnLCAnc291cmNlLm9yZ2FuaXplSW1wb3J0cyddLFxuICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgaXNQcmVmZXJyZWRTdXBwb3J0OiB0cnVlLFxuICAgICAgICAgICAgICAgICAgICBkYXRhU3VwcG9ydDogICAgICAgIHRydWUsXG4gICAgICAgICAgICAgICAgICAgIHJlc29sdmVTdXBwb3J0OiAgICAgeyBwcm9wZXJ0aWVzOiBbJ2VkaXQnXSB9LFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgcHVibGlzaERpYWdub3N0aWNzOiB7IHJlbGF0ZWRJbmZvcm1hdGlvbjogdHJ1ZSB9LFxuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIHdvcmtzcGFjZToge1xuICAgICAgICAgICAgICAgIGFwcGx5RWRpdDogICAgICAgICAgICAgIHRydWUsXG4gICAgICAgICAgICAgICAgY29uZmlndXJhdGlvbjogICAgICAgICAgdHJ1ZSxcbiAgICAgICAgICAgICAgICBleGVjdXRlQ29tbWFuZDogICAgICAgICB7IGR5bmFtaWNSZWdpc3RyYXRpb246IHRydWUgfSxcbiAgICAgICAgICAgICAgICBkaWRDaGFuZ2VDb25maWd1cmF0aW9uOiB7IGR5bmFtaWNSZWdpc3RyYXRpb246IHRydWUgfSxcbiAgICAgICAgICAgICAgICB3b3Jrc3BhY2VFZGl0OiAgICAgICAgICB7IGRvY3VtZW50Q2hhbmdlczogdHJ1ZSwgcmVzb3VyY2VPcGVyYXRpb25zOiBbJ2NyZWF0ZScsICdyZW5hbWUnLCAnZGVsZXRlJ10gfSxcbiAgICAgICAgICAgIH0sXG4gICAgICAgIH0sXG4gICAgfSk7XG5cbiAgICBfY29ubi5zZW5kTm90aWZpY2F0aW9uKCdpbml0aWFsaXplZCcsIHt9KTtcbiAgICBfY29ubi5zZW5kTm90aWZpY2F0aW9uKCd3b3Jrc3BhY2UvZGlkQ2hhbmdlQ29uZmlndXJhdGlvbicsIHsgc2V0dGluZ3M6IGpkdGxzU2V0dGluZ3MoKSB9KTtcblxuICAgIG9wZW5GaWxlKGZpbGVVcmkpO1xuXG4gICAgaWYgKCFfcHJvdmlkZXJzUmVnaXN0ZXJlZCkge1xuICAgICAgICBfcHJvdmlkZXJzUmVnaXN0ZXJlZCA9IHRydWU7XG4gICAgICAgIHJlZ2lzdGVyUHJvdmlkZXJzKCk7XG4gICAgfVxufVxuXG4vLyAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4vLyBGaWxlIGxpZmVjeWNsZVxuLy8gLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuXG4vKipcbiAqIFNlbmRzIHRleHREb2N1bWVudC9kaWRPcGVuIGZvciB0aGUgZ2l2ZW4gVVJJIChpZiBub3QgYWxyZWFkeSBzZW50KSBhbmQgcmVnaXN0ZXJzIGEgZGVib3VuY2VkXG4gKiB0ZXh0RG9jdW1lbnQvZGlkQ2hhbmdlIGxpc3RlbmVyIG9uIHRoZSBjb3JyZXNwb25kaW5nIE1vbmFjbyBtb2RlbC5cbiAqL1xuZnVuY3Rpb24gb3BlbkZpbGUoZmlsZVVyaTogc3RyaW5nKTogdm9pZCB7XG4gICAgaWYgKF9vcGVuRmlsZXMuaGFzKGZpbGVVcmkpIHx8ICFfY29ubikgcmV0dXJuO1xuICAgIF9vcGVuRmlsZXMuYWRkKGZpbGVVcmkpO1xuXG4gICAgY29uc3QgbW9kZWwgPSBtb25hY28uZWRpdG9yLmdldE1vZGVscygpLmZpbmQobSA9PiBtLnVyaS50b1N0cmluZygpID09PSBmaWxlVXJpKTtcbiAgICBfY29ubi5zZW5kTm90aWZpY2F0aW9uKCd0ZXh0RG9jdW1lbnQvZGlkT3BlbicsIHtcbiAgICAgICAgdGV4dERvY3VtZW50OiB7XG4gICAgICAgICAgICB1cmk6ICAgICAgICBmaWxlVXJpLFxuICAgICAgICAgICAgbGFuZ3VhZ2VJZDogJ2phdmEnLFxuICAgICAgICAgICAgdmVyc2lvbjogICAgMSxcbiAgICAgICAgICAgIHRleHQ6ICAgICAgIG1vZGVsPy5nZXRWYWx1ZSgpID8/ICcnLFxuICAgICAgICB9LFxuICAgIH0pO1xuXG4gICAgaWYgKG1vZGVsKSB7XG4gICAgICAgIGxldCB0aW1lcjogUmV0dXJuVHlwZTx0eXBlb2Ygc2V0VGltZW91dD47XG4gICAgICAgIG1vZGVsLm9uRGlkQ2hhbmdlQ29udGVudCgoKSA9PiB7XG4gICAgICAgICAgICBjbGVhclRpbWVvdXQodGltZXIpO1xuICAgICAgICAgICAgdGltZXIgPSBzZXRUaW1lb3V0KCgpID0+IHtcbiAgICAgICAgICAgICAgICBfY29ubj8uc2VuZE5vdGlmaWNhdGlvbigndGV4dERvY3VtZW50L2RpZENoYW5nZScsIHtcbiAgICAgICAgICAgICAgICAgICAgdGV4dERvY3VtZW50OiAgIHsgdXJpOiBmaWxlVXJpLCB2ZXJzaW9uOiBtb2RlbC5nZXRWZXJzaW9uSWQoKSB9LFxuICAgICAgICAgICAgICAgICAgICBjb250ZW50Q2hhbmdlczogW3sgdGV4dDogbW9kZWwuZ2V0VmFsdWUoKSB9XSxcbiAgICAgICAgICAgICAgICB9KTtcbiAgICAgICAgICAgIH0sIDQwMCk7XG4gICAgICAgIH0pO1xuICAgIH1cbn1cblxuLy8gLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuLy8gV29ya3NwYWNlIGZpbGUgcHJlZGljYXRlXG4vLyAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG5cbi8qKlxuICogUmV0dXJucyB7QGNvZGUgdHJ1ZX0gd2hlbiB0aGUgZ2l2ZW4gbW9kZWwgVVJJIGJlbG9uZ3MgdG8gdGhlIGN1cnJlbnRseSBjb25uZWN0ZWQgd29ya3NwYWNlLiBVc2VkXG4gKiBieSBNb25hY28gcHJvdmlkZXJzIHRvIHNraXAgbm9uLUphdmEtd29ya3NwYWNlIG1vZGVscyB3aXRob3V0IGFuIGV4YWN0LVVSSSBjb21wYXJpc29uLlxuICovXG5mdW5jdGlvbiBpc1dvcmtzcGFjZUZpbGUodXJpOiBzdHJpbmcpOiBib29sZWFuIHtcbiAgICByZXR1cm4gX3dvcmtzcGFjZVJvb3QgIT09ICcnICYmIHVyaS5zdGFydHNXaXRoKF93b3Jrc3BhY2VSb290KTtcbn1cblxuLy8gLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuLy8gTW9uYWNvIHByb3ZpZGVyIHJlZ2lzdHJhdGlvblxuLy8gLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuXG5mdW5jdGlvbiByZWdpc3RlclByb3ZpZGVycygpOiB2b2lkIHtcblxuICAgIG1vbmFjby5lZGl0b3IucmVnaXN0ZXJDb21tYW5kKEFQUExZX0FDVElPTl9DT01NQU5ELCAoX2FjY2Vzc29yOiB1bmtub3duLCBhY3Rpb246IENvZGVBY3Rpb24gfCBDb21tYW5kKSA9PiB7XG4gICAgICAgIGFwcGx5Q29kZUFjdGlvbihhY3Rpb24pO1xuICAgIH0pO1xuXG4gICAgLy8gQ3Jvc3MtZmlsZSBuYXZpZ2F0aW9uOiB0aGlzIHNpbmdsZS1maWxlIGVkaXRvciBoYXMgbm8gbW9kZWwgZm9yIG90aGVyIHdvcmtzcGFjZSBmaWxlcywgc28gR28gdG9cbiAgICAvLyBEZWZpbml0aW9uIC8gRmluZCBSZWZlcmVuY2VzIHRvIGFub3RoZXIgZmlsZSB3b3VsZCBzaWxlbnRseSBkbyBub3RoaW5nLiBIYW5kIHRob3NlIG9mZiB0byB0aGUgSURFXG4gICAgLy8gdG8gb3BlbiB0aGUgdGFyZ2V0IGZpbGUgKGFuZCByZXZlYWwgdGhlIGxpbmUpLiBTYW1lLWZpbGUgdGFyZ2V0cyBmYWxsIHRocm91Z2ggdG8gTW9uYWNvLlxuICAgIG1vbmFjby5lZGl0b3IucmVnaXN0ZXJFZGl0b3JPcGVuZXIoe1xuICAgICAgICBvcGVuQ29kZUVkaXRvcjogKHNvdXJjZSwgcmVzb3VyY2UsIHNlbGVjdGlvbk9yUG9zaXRpb24pID0+IHtcbiAgICAgICAgICAgIGNvbnN0IHVyaSA9IHJlc291cmNlLnRvU3RyaW5nKCk7XG4gICAgICAgICAgICBpZiAoIWlzV29ya3NwYWNlRmlsZSh1cmkpIHx8ICF1cmkuc3RhcnRzV2l0aChWSVJUVUFMX0ZJTEVfUFJFRklYKSkgcmV0dXJuIGZhbHNlO1xuICAgICAgICAgICAgY29uc3QgY3VycmVudE1vZGVsID0gc291cmNlLmdldE1vZGVsKCk7XG4gICAgICAgICAgICBpZiAoY3VycmVudE1vZGVsICYmIGN1cnJlbnRNb2RlbC51cmkudG9TdHJpbmcoKSA9PT0gdXJpKSByZXR1cm4gZmFsc2U7IC8vIHNhbWUgZmlsZSBcdTIwMTQgbGV0IE1vbmFjbyBqdW1wXG4gICAgICAgICAgICBjb25zdCBvcGVuZXIgPSAoZ2xvYmFsVGhpcyBhcyBhbnkpLmphdmFMc3BPcGVuRmlsZTtcbiAgICAgICAgICAgIGlmICh0eXBlb2Ygb3BlbmVyICE9PSAnZnVuY3Rpb24nKSByZXR1cm4gZmFsc2U7XG4gICAgICAgICAgICBjb25zdCBwb3MgPSBzZWxlY3Rpb25PclBvc2l0aW9uIGFzIHsgc3RhcnRMaW5lTnVtYmVyPzogbnVtYmVyOyBsaW5lTnVtYmVyPzogbnVtYmVyOyBzdGFydENvbHVtbj86IG51bWJlcjsgY29sdW1uPzogbnVtYmVyIH0gfCB1bmRlZmluZWQ7XG4gICAgICAgICAgICBjb25zdCBsaW5lID0gcG9zID8gKHBvcy5zdGFydExpbmVOdW1iZXIgPz8gcG9zLmxpbmVOdW1iZXIpIDogdW5kZWZpbmVkO1xuICAgICAgICAgICAgY29uc3QgY29sdW1uID0gcG9zID8gKHBvcy5zdGFydENvbHVtbiA/PyBwb3MuY29sdW1uKSA6IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIG9wZW5lcih1cmkuc3Vic3RyaW5nKFZJUlRVQUxfRklMRV9QUkVGSVgubGVuZ3RoKSwgbGluZSwgY29sdW1uKTtcbiAgICAgICAgICAgIHJldHVybiB0cnVlO1xuICAgICAgICB9LFxuICAgIH0pO1xuXG4gICAgbW9uYWNvLmxhbmd1YWdlcy5yZWdpc3RlckNvbXBsZXRpb25JdGVtUHJvdmlkZXIoJ2phdmEnLCB7XG4gICAgICAgIHRyaWdnZXJDaGFyYWN0ZXJzOiBbJy4nLCAnQCcsICc8J10sXG4gICAgICAgIHByb3ZpZGVDb21wbGV0aW9uSXRlbXM6IGFzeW5jIChtb2RlbCwgcG9zaXRpb24sIGNvbnRleHQpID0+IHtcbiAgICAgICAgICAgIGlmICghX2Nvbm4gfHwgIWlzV29ya3NwYWNlRmlsZShtb2RlbC51cmkudG9TdHJpbmcoKSkpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgY29uc3QgZmlsZVVyaSA9IG1vZGVsLnVyaS50b1N0cmluZygpO1xuICAgICAgICAgICAgY29uc3QgcmVzdWx0OiBDb21wbGV0aW9uTGlzdCB8IENvbXBsZXRpb25JdGVtW10gfCBudWxsID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ3RleHREb2N1bWVudC9jb21wbGV0aW9uJywge1xuICAgICAgICAgICAgICAgIHRleHREb2N1bWVudDogeyB1cmk6IGZpbGVVcmkgfSxcbiAgICAgICAgICAgICAgICBwb3NpdGlvbjogICAgIHsgbGluZTogcG9zaXRpb24ubGluZU51bWJlciAtIDEsIGNoYXJhY3RlcjogcG9zaXRpb24uY29sdW1uIC0gMSB9LFxuICAgICAgICAgICAgICAgIC8vIE1vbmFjbyB0cmlnZ2VyIGtpbmRzIGFyZSAwLWJhc2VkIChJbnZva2UvVHJpZ2dlckNoYXJhY3Rlci9Gb3JJbmNvbXBsZXRlKTsgTFNQIGlzIDEtYmFzZWQuXG4gICAgICAgICAgICAgICAgY29udGV4dDogICAgICB7IHRyaWdnZXJLaW5kOiAoY29udGV4dC50cmlnZ2VyS2luZCA/PyAwKSArIDEsIHRyaWdnZXJDaGFyYWN0ZXI6IGNvbnRleHQudHJpZ2dlckNoYXJhY3RlciB9LFxuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICBjb25zdCBpdGVtcyA9IEFycmF5LmlzQXJyYXkocmVzdWx0KSA/IHJlc3VsdCA6IChyZXN1bHQ/Lml0ZW1zID8/IFtdKTtcbiAgICAgICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICAgICAgc3VnZ2VzdGlvbnM6IGl0ZW1zLm1hcChpdGVtID0+IGxzcENvbXBsZXRpb25Ub01vbmFjbyhpdGVtLCBtb2RlbCwgcG9zaXRpb24pKSxcbiAgICAgICAgICAgICAgICAvLyBKRFQuTFMgcmV0dXJucyBhIHRydW5jYXRlZCBsaXN0IG9uIHRoZSBmaXJzdCBrZXlzdHJva2VzOyBwcm9wYWdhdGluZyBcImluY29tcGxldGVcIiBtYWtlc1xuICAgICAgICAgICAgICAgIC8vIE1vbmFjbyByZS1xdWVyeSBhcyB0aGUgdXNlciB0eXBlcyBpbnN0ZWFkIG9mIGNhY2hpbmcgdGhlIGZpcnN0IChvZnRlbiBlbXB0eSkgcmVzdWx0LlxuICAgICAgICAgICAgICAgIGluY29tcGxldGU6ICBBcnJheS5pc0FycmF5KHJlc3VsdCkgPyBmYWxzZSA6ICEhcmVzdWx0Py5pc0luY29tcGxldGUsXG4gICAgICAgICAgICB9O1xuICAgICAgICB9LFxuICAgICAgICAvLyBSZXNvbHZlIGRvY3VtZW50YXRpb24gYW5kLCBjcnVjaWFsbHksIHRoZSBhdXRvLWltcG9ydCBhZGRpdGlvbmFsVGV4dEVkaXRzIHdoaWNoIEpEVC5MUyBvbmx5XG4gICAgICAgIC8vIGF0dGFjaGVzIG9uIHJlc29sdmUgXHUyMDE0IHNlbGVjdGluZyBhIHR5cGUgdGhlbiBpbnNlcnRzIGl0cyBpbXBvcnQgc3RhdGVtZW50LlxuICAgICAgICByZXNvbHZlQ29tcGxldGlvbkl0ZW06IGFzeW5jIChpdGVtKSA9PiB7XG4gICAgICAgICAgICBjb25zdCBsc3AgPSAoaXRlbSBhcyBNb25hY29Db21wbGV0aW9uSXRlbSkuX2xzcDtcbiAgICAgICAgICAgIGlmICghX2Nvbm4gfHwgIWxzcCkgcmV0dXJuIGl0ZW07XG4gICAgICAgICAgICB0cnkge1xuICAgICAgICAgICAgICAgIGNvbnN0IHJlc29sdmVkOiBDb21wbGV0aW9uSXRlbSA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCdjb21wbGV0aW9uSXRlbS9yZXNvbHZlJywgbHNwKTtcbiAgICAgICAgICAgICAgICBpZiAocmVzb2x2ZWQuZG9jdW1lbnRhdGlvbikge1xuICAgICAgICAgICAgICAgICAgICBpdGVtLmRvY3VtZW50YXRpb24gPSB7IHZhbHVlOiBtYXJrdXBUb1N0cmluZyhyZXNvbHZlZC5kb2N1bWVudGF0aW9uKSwgaXNUcnVzdGVkOiBmYWxzZSB9O1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBpZiAocmVzb2x2ZWQuZGV0YWlsKSBpdGVtLmRldGFpbCA9IHJlc29sdmVkLmRldGFpbDtcbiAgICAgICAgICAgICAgICBpZiAocmVzb2x2ZWQuYWRkaXRpb25hbFRleHRFZGl0cz8ubGVuZ3RoKSB7XG4gICAgICAgICAgICAgICAgICAgIGl0ZW0uYWRkaXRpb25hbFRleHRFZGl0cyA9IHJlc29sdmVkLmFkZGl0aW9uYWxUZXh0RWRpdHMubWFwKHRleHRFZGl0VG9Nb25hY28pO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH0gY2F0Y2ggKGUpIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmRlYnVnKCdbamF2YS1sc3BdIGNvbXBsZXRpb24gcmVzb2x2ZSBmYWlsZWQ6JywgKGUgYXMgRXJyb3IpPy5tZXNzYWdlKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJldHVybiBpdGVtO1xuICAgICAgICB9LFxuICAgIH0pO1xuXG4gICAgbW9uYWNvLmxhbmd1YWdlcy5yZWdpc3RlckhvdmVyUHJvdmlkZXIoJ2phdmEnLCB7XG4gICAgICAgIHByb3ZpZGVIb3ZlcjogYXN5bmMgKG1vZGVsLCBwb3NpdGlvbikgPT4ge1xuICAgICAgICAgICAgaWYgKCFfY29ubiB8fCAhaXNXb3Jrc3BhY2VGaWxlKG1vZGVsLnVyaS50b1N0cmluZygpKSkgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICBjb25zdCBmaWxlVXJpID0gbW9kZWwudXJpLnRvU3RyaW5nKCk7XG4gICAgICAgICAgICBjb25zdCByZXN1bHQ6IEhvdmVyIHwgbnVsbCA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCd0ZXh0RG9jdW1lbnQvaG92ZXInLCB7XG4gICAgICAgICAgICAgICAgdGV4dERvY3VtZW50OiB7IHVyaTogZmlsZVVyaSB9LFxuICAgICAgICAgICAgICAgIHBvc2l0aW9uOiAgICAgeyBsaW5lOiBwb3NpdGlvbi5saW5lTnVtYmVyIC0gMSwgY2hhcmFjdGVyOiBwb3NpdGlvbi5jb2x1bW4gLSAxIH0sXG4gICAgICAgICAgICB9KTtcbiAgICAgICAgICAgIGlmICghcmVzdWx0Py5jb250ZW50cykgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICBjb25zdCBjb250ZW50cyA9IEFycmF5LmlzQXJyYXkocmVzdWx0LmNvbnRlbnRzKSA/IHJlc3VsdC5jb250ZW50cyA6IFtyZXN1bHQuY29udGVudHNdO1xuICAgICAgICAgICAgcmV0dXJuIHtcbiAgICAgICAgICAgICAgICBjb250ZW50czogY29udGVudHMubWFwKGMgPT4gKHtcbiAgICAgICAgICAgICAgICAgICAgdmFsdWU6IHR5cGVvZiBjID09PSAnc3RyaW5nJyA/IGMgOiAoYyBhcyBNYXJrdXBDb250ZW50KS52YWx1ZSxcbiAgICAgICAgICAgICAgICAgICAgaXNUcnVzdGVkOiBmYWxzZSxcbiAgICAgICAgICAgICAgICB9KSksXG4gICAgICAgICAgICAgICAgcmFuZ2U6IHJlc3VsdC5yYW5nZSA/IGxzcFJhbmdlVG9Nb25hY28ocmVzdWx0LnJhbmdlKSA6IHVuZGVmaW5lZCxcbiAgICAgICAgICAgIH07XG4gICAgICAgIH0sXG4gICAgfSk7XG5cbiAgICBtb25hY28ubGFuZ3VhZ2VzLnJlZ2lzdGVyU2lnbmF0dXJlSGVscFByb3ZpZGVyKCdqYXZhJywge1xuICAgICAgICBzaWduYXR1cmVIZWxwVHJpZ2dlckNoYXJhY3RlcnM6IFsnKCcsICcsJ10sXG4gICAgICAgIHByb3ZpZGVTaWduYXR1cmVIZWxwOiBhc3luYyAobW9kZWwsIHBvc2l0aW9uKSA9PiB7XG4gICAgICAgICAgICBpZiAoIV9jb25uIHx8ICFpc1dvcmtzcGFjZUZpbGUobW9kZWwudXJpLnRvU3RyaW5nKCkpKSByZXR1cm4gbnVsbDtcbiAgICAgICAgICAgIGNvbnN0IGZpbGVVcmkgPSBtb2RlbC51cmkudG9TdHJpbmcoKTtcbiAgICAgICAgICAgIGNvbnN0IHJlc3VsdDogU2lnbmF0dXJlSGVscCB8IG51bGwgPSBhd2FpdCBfY29ubi5zZW5kUmVxdWVzdCgndGV4dERvY3VtZW50L3NpZ25hdHVyZUhlbHAnLCB7XG4gICAgICAgICAgICAgICAgdGV4dERvY3VtZW50OiB7IHVyaTogZmlsZVVyaSB9LFxuICAgICAgICAgICAgICAgIHBvc2l0aW9uOiAgICAgeyBsaW5lOiBwb3NpdGlvbi5saW5lTnVtYmVyIC0gMSwgY2hhcmFjdGVyOiBwb3NpdGlvbi5jb2x1bW4gLSAxIH0sXG4gICAgICAgICAgICB9KTtcbiAgICAgICAgICAgIGlmICghcmVzdWx0KSByZXR1cm4gbnVsbDtcbiAgICAgICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICAgICAgdmFsdWU6IHtcbiAgICAgICAgICAgICAgICAgICAgc2lnbmF0dXJlczogcmVzdWx0LnNpZ25hdHVyZXMubWFwKChzaWc6IFNpZ25hdHVyZUluZm9ybWF0aW9uKSA9PiAoe1xuICAgICAgICAgICAgICAgICAgICAgICAgbGFiZWw6ICAgICAgICAgc2lnLmxhYmVsLFxuICAgICAgICAgICAgICAgICAgICAgICAgZG9jdW1lbnRhdGlvbjogc2lnLmRvY3VtZW50YXRpb24gPyBtYXJrdXBUb1N0cmluZyhzaWcuZG9jdW1lbnRhdGlvbikgOiB1bmRlZmluZWQsXG4gICAgICAgICAgICAgICAgICAgICAgICBwYXJhbWV0ZXJzOiAgICAoc2lnLnBhcmFtZXRlcnMgPz8gW10pLm1hcCgocDogUGFyYW1ldGVySW5mb3JtYXRpb24pID0+ICh7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgbGFiZWw6ICAgICAgICAgcC5sYWJlbCxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBkb2N1bWVudGF0aW9uOiBwLmRvY3VtZW50YXRpb24gPyBtYXJrdXBUb1N0cmluZyhwLmRvY3VtZW50YXRpb24pIDogdW5kZWZpbmVkLFxuICAgICAgICAgICAgICAgICAgICAgICAgfSkpLFxuICAgICAgICAgICAgICAgICAgICB9KSksXG4gICAgICAgICAgICAgICAgICAgIGFjdGl2ZVNpZ25hdHVyZTogcmVzdWx0LmFjdGl2ZVNpZ25hdHVyZSA/PyAwLFxuICAgICAgICAgICAgICAgICAgICBhY3RpdmVQYXJhbWV0ZXI6IHJlc3VsdC5hY3RpdmVQYXJhbWV0ZXIgPz8gMCxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIGRpc3Bvc2U6ICgpID0+IHt9LFxuICAgICAgICAgICAgfTtcbiAgICAgICAgfSxcbiAgICB9KTtcblxuICAgIG1vbmFjby5sYW5ndWFnZXMucmVnaXN0ZXJEZWZpbml0aW9uUHJvdmlkZXIoJ2phdmEnLCB7XG4gICAgICAgIHByb3ZpZGVEZWZpbml0aW9uOiBhc3luYyAobW9kZWwsIHBvc2l0aW9uKSA9PiB7XG4gICAgICAgICAgICBpZiAoIV9jb25uIHx8ICFpc1dvcmtzcGFjZUZpbGUobW9kZWwudXJpLnRvU3RyaW5nKCkpKSByZXR1cm4gbnVsbDtcbiAgICAgICAgICAgIGNvbnN0IGZpbGVVcmkgPSBtb2RlbC51cmkudG9TdHJpbmcoKTtcbiAgICAgICAgICAgIGNvbnN0IHJlc3VsdDogTG9jYXRpb24gfCBMb2NhdGlvbltdIHwgbnVsbCA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCd0ZXh0RG9jdW1lbnQvZGVmaW5pdGlvbicsIHtcbiAgICAgICAgICAgICAgICB0ZXh0RG9jdW1lbnQ6IHsgdXJpOiBmaWxlVXJpIH0sXG4gICAgICAgICAgICAgICAgcG9zaXRpb246ICAgICB7IGxpbmU6IHBvc2l0aW9uLmxpbmVOdW1iZXIgLSAxLCBjaGFyYWN0ZXI6IHBvc2l0aW9uLmNvbHVtbiAtIDEgfSxcbiAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgaWYgKCFyZXN1bHQpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgY29uc3QgbG9jYXRpb25zID0gQXJyYXkuaXNBcnJheShyZXN1bHQpID8gcmVzdWx0IDogW3Jlc3VsdF07XG4gICAgICAgICAgICByZXR1cm4gbG9jYXRpb25zLm1hcChsb2MgPT4gKHtcbiAgICAgICAgICAgICAgICB1cmk6ICAgbW9uYWNvLlVyaS5wYXJzZShsb2MudXJpKSxcbiAgICAgICAgICAgICAgICByYW5nZTogbHNwUmFuZ2VUb01vbmFjbyhsb2MucmFuZ2UpLFxuICAgICAgICAgICAgfSkpO1xuICAgICAgICB9LFxuICAgIH0pO1xuXG4gICAgbW9uYWNvLmxhbmd1YWdlcy5yZWdpc3RlclJlZmVyZW5jZVByb3ZpZGVyKCdqYXZhJywge1xuICAgICAgICBwcm92aWRlUmVmZXJlbmNlczogYXN5bmMgKG1vZGVsLCBwb3NpdGlvbiwgY29udGV4dCkgPT4ge1xuICAgICAgICAgICAgaWYgKCFfY29ubiB8fCAhaXNXb3Jrc3BhY2VGaWxlKG1vZGVsLnVyaS50b1N0cmluZygpKSkgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICBjb25zdCByZXN1bHQ6IExvY2F0aW9uW10gfCBudWxsID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ3RleHREb2N1bWVudC9yZWZlcmVuY2VzJywge1xuICAgICAgICAgICAgICAgIHRleHREb2N1bWVudDogeyB1cmk6IG1vZGVsLnVyaS50b1N0cmluZygpIH0sXG4gICAgICAgICAgICAgICAgcG9zaXRpb246ICAgICB7IGxpbmU6IHBvc2l0aW9uLmxpbmVOdW1iZXIgLSAxLCBjaGFyYWN0ZXI6IHBvc2l0aW9uLmNvbHVtbiAtIDEgfSxcbiAgICAgICAgICAgICAgICBjb250ZXh0OiAgICAgIHsgaW5jbHVkZURlY2xhcmF0aW9uOiBjb250ZXh0LmluY2x1ZGVEZWNsYXJhdGlvbiB9LFxuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICBpZiAoIXJlc3VsdCkgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICByZXR1cm4gcmVzdWx0Lm1hcChsb2MgPT4gKHsgdXJpOiBtb25hY28uVXJpLnBhcnNlKGxvYy51cmkpLCByYW5nZTogbHNwUmFuZ2VUb01vbmFjbyhsb2MucmFuZ2UpIH0pKTtcbiAgICAgICAgfSxcbiAgICB9KTtcblxuICAgIG1vbmFjby5sYW5ndWFnZXMucmVnaXN0ZXJSZW5hbWVQcm92aWRlcignamF2YScsIHtcbiAgICAgICAgcHJvdmlkZVJlbmFtZUVkaXRzOiBhc3luYyAobW9kZWwsIHBvc2l0aW9uLCBuZXdOYW1lKSA9PiB7XG4gICAgICAgICAgICBpZiAoIV9jb25uIHx8ICFpc1dvcmtzcGFjZUZpbGUobW9kZWwudXJpLnRvU3RyaW5nKCkpKSByZXR1cm4geyBlZGl0czogW10gfTtcbiAgICAgICAgICAgIGNvbnN0IGVkaXQ6IFdvcmtzcGFjZUVkaXQgfCBudWxsID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ3RleHREb2N1bWVudC9yZW5hbWUnLCB7XG4gICAgICAgICAgICAgICAgdGV4dERvY3VtZW50OiB7IHVyaTogbW9kZWwudXJpLnRvU3RyaW5nKCkgfSxcbiAgICAgICAgICAgICAgICBwb3NpdGlvbjogICAgIHsgbGluZTogcG9zaXRpb24ubGluZU51bWJlciAtIDEsIGNoYXJhY3RlcjogcG9zaXRpb24uY29sdW1uIC0gMSB9LFxuICAgICAgICAgICAgICAgIG5ld05hbWUsXG4gICAgICAgICAgICB9KTtcbiAgICAgICAgICAgIGlmICghZWRpdCkgcmV0dXJuIHsgZWRpdHM6IFtdIH07XG4gICAgICAgICAgICAvLyBBIEpEVC5MUyByZW5hbWUgY2FuIHNwYW4gbWFueSBmaWxlcyBhbmQgZXZlbiByZW5hbWUgdGhlIHR5cGUncyBvd24gLmphdmEgZmlsZS4gTW9uYWNvJ3NcbiAgICAgICAgICAgIC8vIHNpbmdsZS1maWxlIGVkaXRvciB3b3VsZCBkcm9wIGV2ZXJ5dGhpbmcgYnV0IHRoZSBjdXJyZW50IG1vZGVsLCBzbyBhcHBseSB0aGUgd2hvbGUgZWRpdFxuICAgICAgICAgICAgLy8gdGhyb3VnaCB0aGUgd29ya3NwYWNlIG91cnNlbHZlcyB3aGVuIHRoZSBJREUgcGVyc2lzdGVuY2UgaG9vayBpcyBhdmFpbGFibGUuXG4gICAgICAgICAgICBpZiAodHlwZW9mIChnbG9iYWxUaGlzIGFzIGFueSkuamF2YUxzcFBlcnNpc3RSZW5hbWUgPT09ICdmdW5jdGlvbicpIHtcbiAgICAgICAgICAgICAgICB0cnkge1xuICAgICAgICAgICAgICAgICAgICBhd2FpdCBhcHBseVJlbmFtZUFjcm9zc1dvcmtzcGFjZShtb2RlbCwgZWRpdCk7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiB7IGVkaXRzOiBbXSB9O1xuICAgICAgICAgICAgICAgIH0gY2F0Y2ggKGUpIHtcbiAgICAgICAgICAgICAgICAgICAgY29uc29sZS5lcnJvcignW2phdmEtbHNwXSBjcm9zcy1maWxlIHJlbmFtZSBmYWlsZWQsIGFwcGx5aW5nIHRvIHRoZSBjdXJyZW50IGZpbGUgb25seTonLCBlKTtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIHdvcmtzcGFjZUVkaXRUb01vbmFjbyhlZGl0KTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICByZXR1cm4gd29ya3NwYWNlRWRpdFRvTW9uYWNvKGVkaXQpO1xuICAgICAgICB9LFxuICAgICAgICByZXNvbHZlUmVuYW1lTG9jYXRpb246IGFzeW5jIChtb2RlbCwgcG9zaXRpb24pID0+IHtcbiAgICAgICAgICAgIGlmICghX2Nvbm4gfHwgIWlzV29ya3NwYWNlRmlsZShtb2RlbC51cmkudG9TdHJpbmcoKSkpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgdHJ5IHtcbiAgICAgICAgICAgICAgICBjb25zdCByZXN1bHQ6IHsgcmFuZ2U6IExzcFJhbmdlOyBwbGFjZWhvbGRlcj86IHN0cmluZyB9IHwgTHNwUmFuZ2UgfCBudWxsID1cbiAgICAgICAgICAgICAgICAgICAgYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ3RleHREb2N1bWVudC9wcmVwYXJlUmVuYW1lJywge1xuICAgICAgICAgICAgICAgICAgICAgICAgdGV4dERvY3VtZW50OiB7IHVyaTogbW9kZWwudXJpLnRvU3RyaW5nKCkgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgIHBvc2l0aW9uOiAgICAgeyBsaW5lOiBwb3NpdGlvbi5saW5lTnVtYmVyIC0gMSwgY2hhcmFjdGVyOiBwb3NpdGlvbi5jb2x1bW4gLSAxIH0sXG4gICAgICAgICAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgICAgIGlmICghcmVzdWx0KSByZXR1cm4gbnVsbDtcbiAgICAgICAgICAgICAgICBjb25zdCByYW5nZSA9ICdyYW5nZScgaW4gcmVzdWx0ID8gcmVzdWx0LnJhbmdlIDogcmVzdWx0O1xuICAgICAgICAgICAgICAgIGNvbnN0IHBsYWNlaG9sZGVyID0gJ3BsYWNlaG9sZGVyJyBpbiByZXN1bHQgJiYgcmVzdWx0LnBsYWNlaG9sZGVyXG4gICAgICAgICAgICAgICAgICAgID8gcmVzdWx0LnBsYWNlaG9sZGVyXG4gICAgICAgICAgICAgICAgICAgIDogbW9kZWwuZ2V0V29yZEF0UG9zaXRpb24ocG9zaXRpb24pPy53b3JkID8/ICcnO1xuICAgICAgICAgICAgICAgIHJldHVybiB7IHJhbmdlOiBsc3BSYW5nZVRvTW9uYWNvKHJhbmdlKSwgdGV4dDogcGxhY2Vob2xkZXIgfTtcbiAgICAgICAgICAgIH0gY2F0Y2gge1xuICAgICAgICAgICAgICAgIGNvbnN0IHdvcmQgPSBtb2RlbC5nZXRXb3JkQXRQb3NpdGlvbihwb3NpdGlvbik7XG4gICAgICAgICAgICAgICAgcmV0dXJuIHdvcmQgPyB7XG4gICAgICAgICAgICAgICAgICAgIHJhbmdlOiB7IHN0YXJ0TGluZU51bWJlcjogcG9zaXRpb24ubGluZU51bWJlciwgc3RhcnRDb2x1bW46IHdvcmQuc3RhcnRDb2x1bW4sIGVuZExpbmVOdW1iZXI6IHBvc2l0aW9uLmxpbmVOdW1iZXIsIGVuZENvbHVtbjogd29yZC5lbmRDb2x1bW4gfSxcbiAgICAgICAgICAgICAgICAgICAgdGV4dDogd29yZC53b3JkLFxuICAgICAgICAgICAgICAgIH0gOiBudWxsO1xuICAgICAgICAgICAgfVxuICAgICAgICB9LFxuICAgIH0pO1xuXG4gICAgLy8gUmVnaXN0ZXJpbmcgdGhpcyBwcm92aWRlciBhbHNvIGVuYWJsZXMgZWRpdG9yLmpzJ3MgZXhpc3RpbmcgZm9ybWF0LW9uLXNhdmUgcGF0aCBmb3IgSmF2YTogdGhlXG4gICAgLy8gc2hhcmVkIFNhdmUgYWN0aW9uIHJ1bnMgZWRpdG9yLmFjdGlvbi5mb3JtYXREb2N1bWVudCB3aGVuIGF1dG8tZm9ybWF0dGluZyBpcyBvbiAodGhlIHNhbWVcbiAgICAvLyBtZWNoYW5pc20gYW5kIGdsb2JhbCB0b2dnbGUgdXNlZCBmb3IgVHlwZVNjcmlwdCkuXG4gICAgbW9uYWNvLmxhbmd1YWdlcy5yZWdpc3RlckRvY3VtZW50Rm9ybWF0dGluZ0VkaXRQcm92aWRlcignamF2YScsIHtcbiAgICAgICAgcHJvdmlkZURvY3VtZW50Rm9ybWF0dGluZ0VkaXRzOiBhc3luYyAobW9kZWwpID0+IHtcbiAgICAgICAgICAgIGlmICghX2Nvbm4gfHwgIWlzV29ya3NwYWNlRmlsZShtb2RlbC51cmkudG9TdHJpbmcoKSkpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgY29uc3QgZWRpdHM6IFRleHRFZGl0W10gfCBudWxsID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ3RleHREb2N1bWVudC9mb3JtYXR0aW5nJywge1xuICAgICAgICAgICAgICAgIHRleHREb2N1bWVudDogeyB1cmk6IG1vZGVsLnVyaS50b1N0cmluZygpIH0sXG4gICAgICAgICAgICAgICAgb3B0aW9uczogICAgICB7IHRhYlNpemU6IG1vZGVsLmdldE9wdGlvbnMoKS50YWJTaXplLCBpbnNlcnRTcGFjZXM6IG1vZGVsLmdldE9wdGlvbnMoKS5pbnNlcnRTcGFjZXMgfSxcbiAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgcmV0dXJuIGVkaXRzID8gZWRpdHMubWFwKHRleHRFZGl0VG9Nb25hY28pIDogbnVsbDtcbiAgICAgICAgfSxcbiAgICB9KTtcblxuICAgIG1vbmFjby5sYW5ndWFnZXMucmVnaXN0ZXJDb2RlQWN0aW9uUHJvdmlkZXIoJ2phdmEnLCB7XG4gICAgICAgIHByb3ZpZGVDb2RlQWN0aW9uczogYXN5bmMgKG1vZGVsLCByYW5nZSwgY29udGV4dCkgPT4ge1xuICAgICAgICAgICAgY29uc3QgZW1wdHkgPSB7IGFjdGlvbnM6IFtdLCBkaXNwb3NlKCkgeyAvKiBub3RoaW5nIHRvIGRpc3Bvc2UgKi8gfSB9O1xuICAgICAgICAgICAgaWYgKCFfY29ubiB8fCAhaXNXb3Jrc3BhY2VGaWxlKG1vZGVsLnVyaS50b1N0cmluZygpKSkgcmV0dXJuIGVtcHR5O1xuICAgICAgICAgICAgY29uc3QgZGlhZ25vc3RpY3MgPSAoY29udGV4dC5tYXJrZXJzID8/IFtdKS5tYXAobWFya2VyVG9Mc3BEaWFnbm9zdGljKTtcbiAgICAgICAgICAgIGNvbnN0IHJlc3VsdDogQXJyYXk8Q29kZUFjdGlvbiB8IENvbW1hbmQ+IHwgbnVsbCA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCd0ZXh0RG9jdW1lbnQvY29kZUFjdGlvbicsIHtcbiAgICAgICAgICAgICAgICB0ZXh0RG9jdW1lbnQ6IHsgdXJpOiBtb2RlbC51cmkudG9TdHJpbmcoKSB9LFxuICAgICAgICAgICAgICAgIHJhbmdlOiAgICAgICAgbW9uYWNvUmFuZ2VUb0xzcChyYW5nZSksXG4gICAgICAgICAgICAgICAgY29udGV4dDogICAgICB7IGRpYWdub3N0aWNzLCBvbmx5OiBjb250ZXh0Lm9ubHkgPyBbY29udGV4dC5vbmx5XSA6IHVuZGVmaW5lZCB9LFxuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICBpZiAoIXJlc3VsdD8ubGVuZ3RoKSByZXR1cm4gZW1wdHk7XG4gICAgICAgICAgICByZXR1cm4ge1xuICAgICAgICAgICAgICAgIGFjdGlvbnM6IHJlc3VsdC5tYXAobHNwQ29kZUFjdGlvblRvTW9uYWNvKSxcbiAgICAgICAgICAgICAgICBkaXNwb3NlKCkgeyAvKiBub3RoaW5nIHRvIGRpc3Bvc2UgKi8gfSxcbiAgICAgICAgICAgIH07XG4gICAgICAgIH0sXG4gICAgfSwge1xuICAgICAgICBwcm92aWRlZENvZGVBY3Rpb25LaW5kczogWydxdWlja2ZpeCcsICdyZWZhY3RvcicsICdyZWZhY3Rvci5leHRyYWN0JywgJ3JlZmFjdG9yLmlubGluZScsXG4gICAgICAgICAgICAncmVmYWN0b3IucmV3cml0ZScsICdzb3VyY2UnLCAnc291cmNlLm9yZ2FuaXplSW1wb3J0cyddLFxuICAgIH0pO1xufVxuXG4vLyAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4vLyBIZWxwZXJzXG4vLyAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG5cbmZ1bmN0aW9uIGxzcFNldmVyaXR5KHNldmVyaXR5OiBEaWFnbm9zdGljU2V2ZXJpdHkgfCB1bmRlZmluZWQpOiBtb25hY28uTWFya2VyU2V2ZXJpdHkge1xuICAgIHN3aXRjaCAoc2V2ZXJpdHkpIHtcbiAgICAgICAgY2FzZSBEaWFnbm9zdGljU2V2ZXJpdHkuRXJyb3I6ICAgICAgIHJldHVybiBtb25hY28uTWFya2VyU2V2ZXJpdHkuRXJyb3I7XG4gICAgICAgIGNhc2UgRGlhZ25vc3RpY1NldmVyaXR5Lldhcm5pbmc6ICAgICByZXR1cm4gbW9uYWNvLk1hcmtlclNldmVyaXR5Lldhcm5pbmc7XG4gICAgICAgIGNhc2UgRGlhZ25vc3RpY1NldmVyaXR5LkluZm9ybWF0aW9uOiByZXR1cm4gbW9uYWNvLk1hcmtlclNldmVyaXR5LkluZm87XG4gICAgICAgIGNhc2UgRGlhZ25vc3RpY1NldmVyaXR5LkhpbnQ6ICAgICAgICByZXR1cm4gbW9uYWNvLk1hcmtlclNldmVyaXR5LkhpbnQ7XG4gICAgICAgIGRlZmF1bHQ6ICAgICAgICAgICAgICAgICAgICAgICAgICAgICByZXR1cm4gbW9uYWNvLk1hcmtlclNldmVyaXR5LkVycm9yO1xuICAgIH1cbn1cblxuZnVuY3Rpb24gbHNwUmFuZ2VUb01vbmFjbyhyOiB7IHN0YXJ0OiB7IGxpbmU6IG51bWJlcjsgY2hhcmFjdGVyOiBudW1iZXIgfTsgZW5kOiB7IGxpbmU6IG51bWJlcjsgY2hhcmFjdGVyOiBudW1iZXIgfSB9KTogbW9uYWNvLklSYW5nZSB7XG4gICAgcmV0dXJuIHtcbiAgICAgICAgc3RhcnRMaW5lTnVtYmVyOiByLnN0YXJ0LmxpbmUgKyAxLFxuICAgICAgICBzdGFydENvbHVtbjogICAgIHIuc3RhcnQuY2hhcmFjdGVyICsgMSxcbiAgICAgICAgZW5kTGluZU51bWJlcjogICByLmVuZC5saW5lICsgMSxcbiAgICAgICAgZW5kQ29sdW1uOiAgICAgICByLmVuZC5jaGFyYWN0ZXIgKyAxLFxuICAgIH07XG59XG5cbmZ1bmN0aW9uIG1hcmt1cFRvU3RyaW5nKGM6IHN0cmluZyB8IE1hcmt1cENvbnRlbnQpOiBzdHJpbmcge1xuICAgIHJldHVybiB0eXBlb2YgYyA9PT0gJ3N0cmluZycgPyBjIDogYy52YWx1ZTtcbn1cblxuZnVuY3Rpb24gbHNwQ29tcGxldGlvbktpbmQoa2luZDogQ29tcGxldGlvbkl0ZW1LaW5kIHwgdW5kZWZpbmVkKTogbW9uYWNvLmxhbmd1YWdlcy5Db21wbGV0aW9uSXRlbUtpbmQge1xuICAgIGNvbnN0IEsgPSBtb25hY28ubGFuZ3VhZ2VzLkNvbXBsZXRpb25JdGVtS2luZDtcbiAgICBzd2l0Y2ggKGtpbmQpIHtcbiAgICAgICAgY2FzZSBDb21wbGV0aW9uSXRlbUtpbmQuVGV4dDogICAgICAgICAgcmV0dXJuIEsuVGV4dDtcbiAgICAgICAgY2FzZSBDb21wbGV0aW9uSXRlbUtpbmQuTWV0aG9kOiAgICAgICAgcmV0dXJuIEsuTWV0aG9kO1xuICAgICAgICBjYXNlIENvbXBsZXRpb25JdGVtS2luZC5GdW5jdGlvbjogICAgICByZXR1cm4gSy5GdW5jdGlvbjtcbiAgICAgICAgY2FzZSBDb21wbGV0aW9uSXRlbUtpbmQuQ29uc3RydWN0b3I6ICAgcmV0dXJuIEsuQ29uc3RydWN0b3I7XG4gICAgICAgIGNhc2UgQ29tcGxldGlvbkl0ZW1LaW5kLkZpZWxkOiAgICAgICAgIHJldHVybiBLLkZpZWxkO1xuICAgICAgICBjYXNlIENvbXBsZXRpb25JdGVtS2luZC5WYXJpYWJsZTogICAgICByZXR1cm4gSy5WYXJpYWJsZTtcbiAgICAgICAgY2FzZSBDb21wbGV0aW9uSXRlbUtpbmQuQ2xhc3M6ICAgICAgICAgcmV0dXJuIEsuQ2xhc3M7XG4gICAgICAgIGNhc2UgQ29tcGxldGlvbkl0ZW1LaW5kLkludGVyZmFjZTogICAgIHJldHVybiBLLkludGVyZmFjZTtcbiAgICAgICAgY2FzZSBDb21wbGV0aW9uSXRlbUtpbmQuTW9kdWxlOiAgICAgICAgcmV0dXJuIEsuTW9kdWxlO1xuICAgICAgICBjYXNlIENvbXBsZXRpb25JdGVtS2luZC5Qcm9wZXJ0eTogICAgICByZXR1cm4gSy5Qcm9wZXJ0eTtcbiAgICAgICAgY2FzZSBDb21wbGV0aW9uSXRlbUtpbmQuS2V5d29yZDogICAgICAgcmV0dXJuIEsuS2V5d29yZDtcbiAgICAgICAgY2FzZSBDb21wbGV0aW9uSXRlbUtpbmQuU25pcHBldDogICAgICAgcmV0dXJuIEsuU25pcHBldDtcbiAgICAgICAgY2FzZSBDb21wbGV0aW9uSXRlbUtpbmQuQ29uc3RhbnQ6ICAgICAgcmV0dXJuIEsuQ29uc3RhbnQ7XG4gICAgICAgIGNhc2UgQ29tcGxldGlvbkl0ZW1LaW5kLlN0cnVjdDogICAgICAgIHJldHVybiBLLlN0cnVjdDtcbiAgICAgICAgY2FzZSBDb21wbGV0aW9uSXRlbUtpbmQuVHlwZVBhcmFtZXRlcjogcmV0dXJuIEsuVHlwZVBhcmFtZXRlcjtcbiAgICAgICAgZGVmYXVsdDogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuIEsuVGV4dDtcbiAgICB9XG59XG5cbmZ1bmN0aW9uIGxzcENvbXBsZXRpb25Ub01vbmFjbyhcbiAgICBpdGVtOiBDb21wbGV0aW9uSXRlbSxcbiAgICBtb2RlbDogbW9uYWNvLmVkaXRvci5JVGV4dE1vZGVsLFxuICAgIHBvc2l0aW9uOiBtb25hY28uUG9zaXRpb24sXG4pOiBNb25hY29Db21wbGV0aW9uSXRlbSB7XG4gICAgY29uc3Qgd29yZCA9IG1vZGVsLmdldFdvcmRVbnRpbFBvc2l0aW9uKHBvc2l0aW9uKTtcbiAgICBsZXQgcmFuZ2U6IG1vbmFjby5JUmFuZ2UgPSB7XG4gICAgICAgIHN0YXJ0TGluZU51bWJlcjogcG9zaXRpb24ubGluZU51bWJlcixcbiAgICAgICAgc3RhcnRDb2x1bW46ICAgICB3b3JkLnN0YXJ0Q29sdW1uLFxuICAgICAgICBlbmRMaW5lTnVtYmVyOiAgIHBvc2l0aW9uLmxpbmVOdW1iZXIsXG4gICAgICAgIGVuZENvbHVtbjogICAgICAgd29yZC5lbmRDb2x1bW4sXG4gICAgfTtcbiAgICBsZXQgaW5zZXJ0VGV4dCA9IGl0ZW0uaW5zZXJ0VGV4dCA/PyBpdGVtLmxhYmVsO1xuICAgIGNvbnN0IHRleHRFZGl0ID0gaXRlbS50ZXh0RWRpdCBhcyB7IHJhbmdlPzogTHNwUmFuZ2U7IHJlcGxhY2U/OiBMc3BSYW5nZTsgaW5zZXJ0PzogTHNwUmFuZ2U7IG5ld1RleHQ/OiBzdHJpbmcgfSB8IHVuZGVmaW5lZDtcbiAgICBpZiAodGV4dEVkaXQpIHtcbiAgICAgICAgY29uc3QgciA9IHRleHRFZGl0LnJhbmdlID8/IHRleHRFZGl0LnJlcGxhY2UgPz8gdGV4dEVkaXQuaW5zZXJ0O1xuICAgICAgICBpZiAocikgcmFuZ2UgPSBsc3BSYW5nZVRvTW9uYWNvKHIpO1xuICAgICAgICBpZiAodHlwZW9mIHRleHRFZGl0Lm5ld1RleHQgPT09ICdzdHJpbmcnKSBpbnNlcnRUZXh0ID0gdGV4dEVkaXQubmV3VGV4dDtcbiAgICB9XG4gICAgY29uc3QgcmVzdWx0OiBNb25hY29Db21wbGV0aW9uSXRlbSA9IHtcbiAgICAgICAgbGFiZWw6ICAgICAgICAgICBpdGVtLmxhYmVsLFxuICAgICAgICBraW5kOiAgICAgICAgICAgIGxzcENvbXBsZXRpb25LaW5kKGl0ZW0ua2luZCksXG4gICAgICAgIGRldGFpbDogICAgICAgICAgaXRlbS5kZXRhaWwsXG4gICAgICAgIGRvY3VtZW50YXRpb246ICAgaXRlbS5kb2N1bWVudGF0aW9uID8geyB2YWx1ZTogbWFya3VwVG9TdHJpbmcoaXRlbS5kb2N1bWVudGF0aW9uKSwgaXNUcnVzdGVkOiBmYWxzZSB9IDogdW5kZWZpbmVkLFxuICAgICAgICBpbnNlcnRUZXh0LFxuICAgICAgICBpbnNlcnRUZXh0UnVsZXM6IGl0ZW0uaW5zZXJ0VGV4dEZvcm1hdCA9PT0gSW5zZXJ0VGV4dEZvcm1hdC5TbmlwcGV0XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgID8gbW9uYWNvLmxhbmd1YWdlcy5Db21wbGV0aW9uSXRlbUluc2VydFRleHRSdWxlLkluc2VydEFzU25pcHBldFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICA6IHVuZGVmaW5lZCxcbiAgICAgICAgcmFuZ2UsXG4gICAgICAgIHNvcnRUZXh0OiAgICAgICAgICAgIHNka1ByaW9yaXRpc2VkU29ydFRleHQoaXRlbSksXG4gICAgICAgIGZpbHRlclRleHQ6ICAgICAgICAgIGl0ZW0uZmlsdGVyVGV4dCxcbiAgICAgICAgcHJlc2VsZWN0OiAgICAgICAgICAgaXRlbS5wcmVzZWxlY3QsXG4gICAgICAgIGNvbW1pdENoYXJhY3RlcnM6ICAgIGl0ZW0uY29tbWl0Q2hhcmFjdGVycyxcbiAgICAgICAgYWRkaXRpb25hbFRleHRFZGl0czogaXRlbS5hZGRpdGlvbmFsVGV4dEVkaXRzPy5tYXAodGV4dEVkaXRUb01vbmFjbyksXG4gICAgfTtcbiAgICByZXN1bHQuX2xzcCA9IGl0ZW07XG4gICAgcmV0dXJuIHJlc3VsdDtcbn1cblxuLyoqXG4gKiBSYW5rcyBEaXJpZ2libGUgU0RLIHN1Z2dlc3Rpb25zICh7QGNvZGUgb3JnLmVjbGlwc2UuZGlyaWdpYmxlLnNkay4qfSkgYWJvdmUgZXZlcnl0aGluZyBlbHNlIGJ5XG4gKiBwcmVmaXhpbmcgdGhlIHNlcnZlciBzb3J0VGV4dCB3aXRoIGEgcHJpb3JpdHkgYnVja2V0LCBwcmVzZXJ2aW5nIHRoZSBzZXJ2ZXIgb3JkZXIgd2l0aGluIGVhY2ggYnVja2V0LlxuICovXG5mdW5jdGlvbiBzZGtQcmlvcml0aXNlZFNvcnRUZXh0KGl0ZW06IENvbXBsZXRpb25JdGVtKTogc3RyaW5nIHtcbiAgICBjb25zdCBiYXNlID0gaXRlbS5zb3J0VGV4dCA/PyAodHlwZW9mIGl0ZW0ubGFiZWwgPT09ICdzdHJpbmcnID8gaXRlbS5sYWJlbCA6ICcnKTtcbiAgICBjb25zdCBkZXNjcmlwdGlvbiA9IChpdGVtLmxhYmVsRGV0YWlscyAmJiB0eXBlb2YgaXRlbS5sYWJlbERldGFpbHMuZGVzY3JpcHRpb24gPT09ICdzdHJpbmcnKVxuICAgICAgICA/IGl0ZW0ubGFiZWxEZXRhaWxzLmRlc2NyaXB0aW9uIDogJyc7XG4gICAgY29uc3QgaGF5c3RhY2sgPSBgJHtpdGVtLmRldGFpbCA/PyAnJ30gJHtkZXNjcmlwdGlvbn1gO1xuICAgIHJldHVybiBoYXlzdGFjay5pbmNsdWRlcygnb3JnLmVjbGlwc2UuZGlyaWdpYmxlLnNkaycpID8gYDBfJHtiYXNlfWAgOiBgMV8ke2Jhc2V9YDtcbn1cblxuLy8gLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuLy8gQ29kZSBhY3Rpb25zLCBjb21tYW5kcywgcmVmYWN0b3IvZ2VuZXJhdGUsIHdvcmtzcGFjZSBlZGl0c1xuLy8gLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuXG4vKiogTW9uYWNvIGNvbW1hbmQgaWQgdXNlZCB0byBhcHBseSBhIGRlZmVycmVkIExTUCBjb2RlIGFjdGlvbiB3aGVuIHRoZSB1c2VyIHNlbGVjdHMgaXQuICovXG5jb25zdCBBUFBMWV9BQ1RJT05fQ09NTUFORCA9ICdkaXJpZ2libGUuamF2YS5hcHBseUNvZGVBY3Rpb24nO1xuXG4vKiogVmlydHVhbCBVUkkgcHJlZml4IG9mIGVkaXRvciBtb2RlbHM7IHN0cmlwcGluZyBpdCB5aWVsZHMgdGhlIElERSB3b3Jrc3BhY2UgcGF0aCAoL3dzL3Byb2ovLi4uKS4gKi9cbmNvbnN0IFZJUlRVQUxfRklMRV9QUkVGSVggPSAnZmlsZTovLy93b3Jrc3BhY2UnO1xuXG50eXBlIExzcFJhbmdlID0geyBzdGFydDogeyBsaW5lOiBudW1iZXI7IGNoYXJhY3RlcjogbnVtYmVyIH07IGVuZDogeyBsaW5lOiBudW1iZXI7IGNoYXJhY3RlcjogbnVtYmVyIH0gfTtcblxuLyoqIE1vbmFjbyBjb21wbGV0aW9uIGl0ZW0gY2FycnlpbmcgdGhlIG9yaWdpbmF0aW5nIExTUCBpdGVtIHNvIHJlc29sdmUgY2FuIGZldGNoIGl0cyBpbXBvcnQgZWRpdHMuICovXG50eXBlIE1vbmFjb0NvbXBsZXRpb25JdGVtID0gbW9uYWNvLmxhbmd1YWdlcy5Db21wbGV0aW9uSXRlbSAmIHsgX2xzcD86IENvbXBsZXRpb25JdGVtIH07XG5cbmZ1bmN0aW9uIHRleHRFZGl0VG9Nb25hY28oZWRpdDogVGV4dEVkaXQpOiBtb25hY28ubGFuZ3VhZ2VzLlRleHRFZGl0IHtcbiAgICByZXR1cm4geyByYW5nZTogbHNwUmFuZ2VUb01vbmFjbyhlZGl0LnJhbmdlKSwgdGV4dDogZWRpdC5uZXdUZXh0IH07XG59XG5cbmZ1bmN0aW9uIG1vbmFjb1JhbmdlVG9Mc3AocjogbW9uYWNvLklSYW5nZSk6IExzcFJhbmdlIHtcbiAgICByZXR1cm4ge1xuICAgICAgICBzdGFydDogeyBsaW5lOiByLnN0YXJ0TGluZU51bWJlciAtIDEsIGNoYXJhY3Rlcjogci5zdGFydENvbHVtbiAtIDEgfSxcbiAgICAgICAgZW5kOiAgIHsgbGluZTogci5lbmRMaW5lTnVtYmVyIC0gMSwgY2hhcmFjdGVyOiByLmVuZENvbHVtbiAtIDEgfSxcbiAgICB9O1xufVxuXG5mdW5jdGlvbiBtb25hY29TZXZlcml0eVRvTHNwKHNldmVyaXR5OiBtb25hY28uTWFya2VyU2V2ZXJpdHkpOiBEaWFnbm9zdGljU2V2ZXJpdHkge1xuICAgIHN3aXRjaCAoc2V2ZXJpdHkpIHtcbiAgICAgICAgY2FzZSBtb25hY28uTWFya2VyU2V2ZXJpdHkuRXJyb3I6ICAgcmV0dXJuIERpYWdub3N0aWNTZXZlcml0eS5FcnJvcjtcbiAgICAgICAgY2FzZSBtb25hY28uTWFya2VyU2V2ZXJpdHkuV2FybmluZzogcmV0dXJuIERpYWdub3N0aWNTZXZlcml0eS5XYXJuaW5nO1xuICAgICAgICBjYXNlIG1vbmFjby5NYXJrZXJTZXZlcml0eS5JbmZvOiAgICByZXR1cm4gRGlhZ25vc3RpY1NldmVyaXR5LkluZm9ybWF0aW9uO1xuICAgICAgICBkZWZhdWx0OiAgICAgICAgICAgICAgICAgICAgICAgICAgICByZXR1cm4gRGlhZ25vc3RpY1NldmVyaXR5LkhpbnQ7XG4gICAgfVxufVxuXG5mdW5jdGlvbiBtYXJrZXJUb0xzcERpYWdub3N0aWMobTogbW9uYWNvLmVkaXRvci5JTWFya2VyRGF0YSk6IERpYWdub3N0aWMge1xuICAgIHJldHVybiB7XG4gICAgICAgIHJhbmdlOiAgICB7IHN0YXJ0OiB7IGxpbmU6IG0uc3RhcnRMaW5lTnVtYmVyIC0gMSwgY2hhcmFjdGVyOiBtLnN0YXJ0Q29sdW1uIC0gMSB9LFxuICAgICAgICAgICAgICAgICAgICBlbmQ6ICAgeyBsaW5lOiBtLmVuZExpbmVOdW1iZXIgLSAxLCBjaGFyYWN0ZXI6IG0uZW5kQ29sdW1uIC0gMSB9IH0sXG4gICAgICAgIG1lc3NhZ2U6ICBtLm1lc3NhZ2UsXG4gICAgICAgIHNldmVyaXR5OiBtb25hY29TZXZlcml0eVRvTHNwKG0uc2V2ZXJpdHkpLFxuICAgICAgICBzb3VyY2U6ICAgbS5zb3VyY2UsXG4gICAgfSBhcyBEaWFnbm9zdGljO1xufVxuXG5mdW5jdGlvbiBsc3BDb2RlQWN0aW9uVG9Nb25hY28oYWN0aW9uOiBDb2RlQWN0aW9uIHwgQ29tbWFuZCk6IG1vbmFjby5sYW5ndWFnZXMuQ29kZUFjdGlvbiB7XG4gICAgY29uc3QgaXNDb21tYW5kID0gdHlwZW9mIChhY3Rpb24gYXMgQ29tbWFuZCkuY29tbWFuZCA9PT0gJ3N0cmluZyc7XG4gICAgY29uc3QgdGl0bGUgPSBhY3Rpb24udGl0bGVcbiAgICAgICAgPz8gKGlzQ29tbWFuZCA/IChhY3Rpb24gYXMgQ29tbWFuZCkuY29tbWFuZCA6IChhY3Rpb24gYXMgQ29kZUFjdGlvbikuY29tbWFuZD8udGl0bGUpXG4gICAgICAgID8/ICdBY3Rpb24nO1xuICAgIGNvbnN0IGtpbmQgPSBpc0NvbW1hbmQgPyAncXVpY2tmaXgnIDogKChhY3Rpb24gYXMgQ29kZUFjdGlvbikua2luZCA/PyAncXVpY2tmaXgnKTtcbiAgICByZXR1cm4ge1xuICAgICAgICB0aXRsZSxcbiAgICAgICAga2luZCxcbiAgICAgICAgZGlhZ25vc3RpY3M6IFtdLFxuICAgICAgICBpc1ByZWZlcnJlZDogKGFjdGlvbiBhcyBDb2RlQWN0aW9uKS5pc1ByZWZlcnJlZCxcbiAgICAgICAgLy8gQXBwbHkgbGF6aWx5IHRocm91Z2ggb3VyIGNvbW1hbmQgc28gd2UgY2FuIHJlc29sdmUsIHJ1biBzZXJ2ZXIgY29tbWFuZHMgYW5kIGFwcGx5IGVkaXRzIHVuaWZvcm1seS5cbiAgICAgICAgY29tbWFuZDogeyBpZDogQVBQTFlfQUNUSU9OX0NPTU1BTkQsIHRpdGxlLCBhcmd1bWVudHM6IFthY3Rpb25dIH0sXG4gICAgfTtcbn1cblxuYXN5bmMgZnVuY3Rpb24gYXBwbHlDb2RlQWN0aW9uKGFjdGlvbjogQ29kZUFjdGlvbiB8IENvbW1hbmQpOiBQcm9taXNlPHZvaWQ+IHtcbiAgICBpZiAoIV9jb25uKSByZXR1cm47XG4gICAgdHJ5IHtcbiAgICAgICAgaWYgKHR5cGVvZiAoYWN0aW9uIGFzIENvbW1hbmQpLmNvbW1hbmQgPT09ICdzdHJpbmcnKSB7XG4gICAgICAgICAgICBhd2FpdCBydW5TZXJ2ZXJDb21tYW5kKGFjdGlvbiBhcyBDb21tYW5kKTtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBsZXQgcmVzb2x2ZWQgPSBhY3Rpb24gYXMgQ29kZUFjdGlvbjtcbiAgICAgICAgaWYgKCFyZXNvbHZlZC5lZGl0ICYmIChyZXNvbHZlZCBhcyB7IGRhdGE/OiB1bmtub3duIH0pLmRhdGEgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmVzb2x2ZWQgPSBhd2FpdCBfY29ubi5zZW5kUmVxdWVzdCgnY29kZUFjdGlvbi9yZXNvbHZlJywgcmVzb2x2ZWQpO1xuICAgICAgICB9XG4gICAgICAgIGlmIChyZXNvbHZlZC5lZGl0KSBhcHBseVdvcmtzcGFjZUVkaXQocmVzb2x2ZWQuZWRpdCk7XG4gICAgICAgIGlmIChyZXNvbHZlZC5jb21tYW5kKSBhd2FpdCBydW5TZXJ2ZXJDb21tYW5kKHJlc29sdmVkLmNvbW1hbmQpO1xuICAgIH0gY2F0Y2ggKGUpIHtcbiAgICAgICAgY29uc29sZS53YXJuKCdbamF2YS1sc3BdIGNvZGUgYWN0aW9uIGZhaWxlZDonLCAoZSBhcyBFcnJvcik/Lm1lc3NhZ2UgPz8gZSk7XG4gICAgfVxufVxuXG5mdW5jdGlvbiBpc1dvcmtzcGFjZUVkaXQodmFsdWU6IHVua25vd24pOiB2YWx1ZSBpcyBXb3Jrc3BhY2VFZGl0IHtcbiAgICByZXR1cm4gISF2YWx1ZSAmJiAoISEodmFsdWUgYXMgV29ya3NwYWNlRWRpdCkuY2hhbmdlcyB8fCAhISh2YWx1ZSBhcyBXb3Jrc3BhY2VFZGl0KS5kb2N1bWVudENoYW5nZXMpO1xufVxuXG5hc3luYyBmdW5jdGlvbiBydW5TZXJ2ZXJDb21tYW5kKGNtZDogQ29tbWFuZCk6IFByb21pc2U8dm9pZD4ge1xuICAgIGlmICghX2Nvbm4gfHwgIWNtZD8uY29tbWFuZCkgcmV0dXJuO1xuICAgIGlmIChHRU5FUkFURVtjbWQuY29tbWFuZF0pIHtcbiAgICAgICAgYXdhaXQgcnVuR2VuZXJhdGUoY21kLmNvbW1hbmQsIGNtZC5hcmd1bWVudHMgPz8gW10pO1xuICAgICAgICByZXR1cm47XG4gICAgfVxuICAgIGNvbnN0IHJlc3VsdCA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCd3b3Jrc3BhY2UvZXhlY3V0ZUNvbW1hbmQnLCB7IGNvbW1hbmQ6IGNtZC5jb21tYW5kLCBhcmd1bWVudHM6IGNtZC5hcmd1bWVudHMgPz8gW10gfSk7XG4gICAgaWYgKGlzV29ya3NwYWNlRWRpdChyZXN1bHQpKSBhcHBseVdvcmtzcGFjZUVkaXQocmVzdWx0KTtcbn1cblxuaW50ZXJmYWNlIEdlbmVyYXRlU3BlYyB7XG4gICAgbGFiZWw6IHN0cmluZztcbiAgICBzdGF0dXM6IHN0cmluZztcbiAgICBnZW5lcmF0ZTogc3RyaW5nO1xuICAgIG1lbWJlcnM6IChzdGF0dXM6IGFueSkgPT4gQXJyYXk8eyBsYWJlbDogc3RyaW5nOyByZWY6IGFueSB9PjtcbiAgICBidWlsZEFyZ3M6IChwcm9tcHRBcmdzOiBhbnlbXSwgc3RhdHVzOiBhbnksIHNlbGVjdGVkOiBhbnlbXSkgPT4gYW55W107XG59XG5cbi8qKiBNZW1iZXItbmFtZWQgZmllbGRzIFx1MjE5MiBwaWNrZXIgbGFiZWxzLiAqL1xuZnVuY3Rpb24gZmllbGRMYWJlbChmOiBhbnkpOiBzdHJpbmcge1xuICAgIGNvbnN0IG5hbWUgPSBmPy5uYW1lID8/IGY/LmZpZWxkTmFtZSA/PyAnJztcbiAgICBjb25zdCB0eXBlID0gZj8udHlwZSA/PyBmPy50eXBlTmFtZTtcbiAgICByZXR1cm4gdHlwZSA/IGAke25hbWV9OiAke3R5cGV9YCA6IGAke25hbWV9YDtcbn1cblxuLyoqXG4gKiBUaGUgSkRULkxTIHNvdXJjZS1nZW5lcmF0aW9uIGNvbW1hbmRzIChjb25zdHJ1Y3RvcnMsIGdldHRlcnMvc2V0dGVycywgdG9TdHJpbmcsIGhhc2hDb2RlL2VxdWFscykuXG4gKiBFYWNoIG1hcHMgdGhlIGNsaWVudCBcIipQcm9tcHRcIiBjb21tYW5kIHRvIHRoZSBzZXJ2ZXIgc3RhdHVzICsgZ2VuZXJhdGUgZGVsZWdhdGUgY29tbWFuZHM7IHRoZSBzdGF0dXNcbiAqIGNhbGwgeWllbGRzIHRoZSBjYW5kaWRhdGUgZmllbGRzIHNob3duIGluIHRoZSBtZW1iZXIgcGlja2VyLCB0aGUgZ2VuZXJhdGUgY2FsbCByZXR1cm5zIHRoZSBlZGl0LlxuICovXG5jb25zdCBHRU5FUkFURTogUmVjb3JkPHN0cmluZywgR2VuZXJhdGVTcGVjPiA9IHtcbiAgICAnamF2YS5hY3Rpb24uZ2VuZXJhdGVDb25zdHJ1Y3RvcnNQcm9tcHQnOiB7XG4gICAgICAgIGxhYmVsOiAnU2VsZWN0IGZpZWxkcyBhbmQgY29uc3RydWN0b3JzJyxcbiAgICAgICAgc3RhdHVzOiAnamF2YS5hY3Rpb24uY2hlY2tDb25zdHJ1Y3RvcnNTdGF0dXMnLFxuICAgICAgICBnZW5lcmF0ZTogJ2phdmEuYWN0aW9uLmdlbmVyYXRlQ29uc3RydWN0b3JzJyxcbiAgICAgICAgbWVtYmVyczogKHMpID0+IChzPy5maWVsZHMgPz8gW10pLm1hcCgoZjogYW55KSA9PiAoeyBsYWJlbDogZmllbGRMYWJlbChmKSwgcmVmOiBmIH0pKSxcbiAgICAgICAgYnVpbGRBcmdzOiAoYXJncywgcywgc2VsKSA9PiBbYXJnc1swXSwgeyBjb25zdHJ1Y3RvcnM6IHM/LmNvbnN0cnVjdG9ycyA/PyBbXSwgZmllbGRzOiBzZWwubWFwKG0gPT4gbS5yZWYpIH1dLFxuICAgIH0sXG4gICAgJ2phdmEuYWN0aW9uLmdlbmVyYXRlVG9TdHJpbmdQcm9tcHQnOiB7XG4gICAgICAgIGxhYmVsOiAnU2VsZWN0IGZpZWxkcyB0byBpbmNsdWRlIGluIHRvU3RyaW5nKCknLFxuICAgICAgICBzdGF0dXM6ICdqYXZhLmFjdGlvbi5jaGVja1RvU3RyaW5nU3RhdHVzJyxcbiAgICAgICAgZ2VuZXJhdGU6ICdqYXZhLmFjdGlvbi5nZW5lcmF0ZVRvU3RyaW5nJyxcbiAgICAgICAgbWVtYmVyczogKHMpID0+IChzPy5maWVsZHMgPz8gW10pLm1hcCgoZjogYW55KSA9PiAoeyBsYWJlbDogZmllbGRMYWJlbChmKSwgcmVmOiBmIH0pKSxcbiAgICAgICAgYnVpbGRBcmdzOiAoYXJncywgX3MsIHNlbCkgPT4gW2FyZ3NbMF0sIHNlbC5tYXAobSA9PiBtLnJlZildLFxuICAgIH0sXG4gICAgJ2phdmEuYWN0aW9uLmhhc2hDb2RlRXF1YWxzUHJvbXB0Jzoge1xuICAgICAgICBsYWJlbDogJ1NlbGVjdCBmaWVsZHMgZm9yIGhhc2hDb2RlKCkgYW5kIGVxdWFscygpJyxcbiAgICAgICAgc3RhdHVzOiAnamF2YS5hY3Rpb24uY2hlY2tIYXNoQ29kZUVxdWFsc1N0YXR1cycsXG4gICAgICAgIGdlbmVyYXRlOiAnamF2YS5hY3Rpb24uZ2VuZXJhdGVIYXNoQ29kZUVxdWFscycsXG4gICAgICAgIG1lbWJlcnM6IChzKSA9PiAocz8uZmllbGRzID8/IFtdKS5tYXAoKGY6IGFueSkgPT4gKHsgbGFiZWw6IGZpZWxkTGFiZWwoZiksIHJlZjogZiB9KSksXG4gICAgICAgIGJ1aWxkQXJnczogKGFyZ3MsIF9zLCBzZWwpID0+IFthcmdzWzBdLCBzZWwubWFwKG0gPT4gbS5yZWYpLCBmYWxzZV0sXG4gICAgfSxcbiAgICAnamF2YS5hY3Rpb24uZ2VuZXJhdGVBY2Nlc3NvcnNQcm9tcHQnOiB7XG4gICAgICAgIGxhYmVsOiAnU2VsZWN0IGZpZWxkcyB0byBnZW5lcmF0ZSBnZXR0ZXJzIGFuZCBzZXR0ZXJzJyxcbiAgICAgICAgc3RhdHVzOiAnamF2YS5hY3Rpb24uY2hlY2tBY2Nlc3NvcnNTdGF0dXMnLFxuICAgICAgICBnZW5lcmF0ZTogJ2phdmEuYWN0aW9uLmdlbmVyYXRlQWNjZXNzb3JzJyxcbiAgICAgICAgbWVtYmVyczogKHMpID0+IChzPy5hY2Nlc3NvcnMgPz8gcyA/PyBbXSkubWFwKChhOiBhbnkpID0+ICh7IGxhYmVsOiBmaWVsZExhYmVsKGEpLCByZWY6IGEgfSkpLFxuICAgICAgICBidWlsZEFyZ3M6IChhcmdzLCBfcywgc2VsKSA9PiBbYXJnc1swXSwgc2VsLm1hcChtID0+IG0ucmVmKV0sXG4gICAgfSxcbn07XG5cbmFzeW5jIGZ1bmN0aW9uIHJ1bkdlbmVyYXRlKHByb21wdElkOiBzdHJpbmcsIGFyZ3M6IGFueVtdKTogUHJvbWlzZTx2b2lkPiB7XG4gICAgaWYgKCFfY29ubikgcmV0dXJuO1xuICAgIGNvbnN0IHNwZWMgPSBHRU5FUkFURVtwcm9tcHRJZF07XG4gICAgY29uc3Qgc3RhdHVzID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ3dvcmtzcGFjZS9leGVjdXRlQ29tbWFuZCcsIHsgY29tbWFuZDogc3BlYy5zdGF0dXMsIGFyZ3VtZW50czogYXJncyB9KTtcbiAgICBpZiAoIXN0YXR1cykgcmV0dXJuO1xuICAgIGNvbnN0IG1lbWJlcnMgPSBzcGVjLm1lbWJlcnMoc3RhdHVzKTtcbiAgICBsZXQgc2VsZWN0ZWQgPSBtZW1iZXJzO1xuICAgIGNvbnN0IHBpY2tlciA9IChnbG9iYWxUaGlzIGFzIGFueSkuamF2YUxzcE1lbWJlclBpY2tlcjtcbiAgICBpZiAobWVtYmVycy5sZW5ndGggJiYgdHlwZW9mIHBpY2tlciA9PT0gJ2Z1bmN0aW9uJykge1xuICAgICAgICBjb25zdCBjaG9zZW46IHN0cmluZ1tdIHwgbnVsbCA9IGF3YWl0IHBpY2tlcihzcGVjLmxhYmVsLCBtZW1iZXJzLm1hcChtID0+IG0ubGFiZWwpKTtcbiAgICAgICAgaWYgKGNob3NlbiA9PT0gbnVsbCkgcmV0dXJuOyAvLyB1c2VyIGNhbmNlbGxlZCB0aGUgZGlhbG9nXG4gICAgICAgIHNlbGVjdGVkID0gbWVtYmVycy5maWx0ZXIobSA9PiBjaG9zZW4uaW5jbHVkZXMobS5sYWJlbCkpO1xuICAgIH1cbiAgICBjb25zdCBlZGl0ID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ3dvcmtzcGFjZS9leGVjdXRlQ29tbWFuZCcsIHtcbiAgICAgICAgY29tbWFuZDogc3BlYy5nZW5lcmF0ZSxcbiAgICAgICAgYXJndW1lbnRzOiBzcGVjLmJ1aWxkQXJncyhhcmdzLCBzdGF0dXMsIHNlbGVjdGVkKSxcbiAgICB9KTtcbiAgICBpZiAoaXNXb3Jrc3BhY2VFZGl0KGVkaXQpKSBhcHBseVdvcmtzcGFjZUVkaXQoZWRpdCk7XG59XG5cbi8qKiBHcm91cHMgYSB3b3Jrc3BhY2UgZWRpdCdzIHRleHQgZWRpdHMgYnkgZG9jdW1lbnQgYW5kIGFwcGxpZXMgdGhlbSBpbiBwbGFjZSB0byBvcGVuIE1vbmFjbyBtb2RlbHMuICovXG5mdW5jdGlvbiBhcHBseVdvcmtzcGFjZUVkaXQoZWRpdDogV29ya3NwYWNlRWRpdCB8IG51bGwgfCB1bmRlZmluZWQpOiB2b2lkIHtcbiAgICBpZiAoIWVkaXQpIHJldHVybjtcbiAgICBjb25zdCBieVVyaTogUmVjb3JkPHN0cmluZywgVGV4dEVkaXRbXT4gPSB7fTtcbiAgICBpZiAoZWRpdC5jaGFuZ2VzKSB7XG4gICAgICAgIGZvciAoY29uc3QgdXJpIGluIGVkaXQuY2hhbmdlcykgYnlVcmlbdXJpXSA9IChieVVyaVt1cmldID8/IFtdKS5jb25jYXQoZWRpdC5jaGFuZ2VzW3VyaV0pO1xuICAgIH1cbiAgICBpZiAoZWRpdC5kb2N1bWVudENoYW5nZXMpIHtcbiAgICAgICAgZm9yIChjb25zdCBkYyBvZiBlZGl0LmRvY3VtZW50Q2hhbmdlcyBhcyBhbnlbXSkge1xuICAgICAgICAgICAgaWYgKGRjPy50ZXh0RG9jdW1lbnQ/LnVyaSAmJiBBcnJheS5pc0FycmF5KGRjLmVkaXRzKSkge1xuICAgICAgICAgICAgICAgIGJ5VXJpW2RjLnRleHREb2N1bWVudC51cmldID0gKGJ5VXJpW2RjLnRleHREb2N1bWVudC51cmldID8/IFtdKS5jb25jYXQoZGMuZWRpdHMpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgfVxuICAgIGZvciAoY29uc3QgdXJpIGluIGJ5VXJpKSB7XG4gICAgICAgIGNvbnN0IG1vZGVsID0gbW9uYWNvLmVkaXRvci5nZXRNb2RlbHMoKS5maW5kKG0gPT4gbS51cmkudG9TdHJpbmcoKSA9PT0gdXJpKTtcbiAgICAgICAgaWYgKCFtb2RlbCkgY29udGludWU7XG4gICAgICAgIGNvbnN0IG9wcyA9IGJ5VXJpW3VyaV0ubWFwKGUgPT4gKHsgcmFuZ2U6IGxzcFJhbmdlVG9Nb25hY28oZS5yYW5nZSksIHRleHQ6IGUubmV3VGV4dCwgZm9yY2VNb3ZlTWFya2VyczogdHJ1ZSB9KSk7XG4gICAgICAgIG1vZGVsLnB1c2hFZGl0T3BlcmF0aW9ucyhbXSwgb3BzLCAoKSA9PiBudWxsKTtcbiAgICB9XG59XG5cbi8qKiBDb252ZXJ0cyBhbiBMU1Agd29ya3NwYWNlIGVkaXQgaW50byB0aGUgTW9uYWNvIHNoYXBlIHJldHVybmVkIGJ5IHRoZSByZW5hbWUgcHJvdmlkZXIuICovXG5mdW5jdGlvbiB3b3Jrc3BhY2VFZGl0VG9Nb25hY28oZWRpdDogV29ya3NwYWNlRWRpdCB8IG51bGwpOiBtb25hY28ubGFuZ3VhZ2VzLldvcmtzcGFjZUVkaXQge1xuICAgIGNvbnN0IGVkaXRzOiBhbnlbXSA9IFtdO1xuICAgIGNvbnN0IHB1c2ggPSAodXJpOiBzdHJpbmcsIGxpc3Q6IFRleHRFZGl0W10pID0+IHtcbiAgICAgICAgZm9yIChjb25zdCBlIG9mIGxpc3QpIHtcbiAgICAgICAgICAgIGVkaXRzLnB1c2goeyByZXNvdXJjZTogbW9uYWNvLlVyaS5wYXJzZSh1cmkpLCB0ZXh0RWRpdDogeyByYW5nZTogbHNwUmFuZ2VUb01vbmFjbyhlLnJhbmdlKSwgdGV4dDogZS5uZXdUZXh0IH0sIHZlcnNpb25JZDogdW5kZWZpbmVkIH0pO1xuICAgICAgICB9XG4gICAgfTtcbiAgICBpZiAoZWRpdD8uY2hhbmdlcykge1xuICAgICAgICBmb3IgKGNvbnN0IHVyaSBpbiBlZGl0LmNoYW5nZXMpIHB1c2godXJpLCBlZGl0LmNoYW5nZXNbdXJpXSk7XG4gICAgfVxuICAgIGlmIChlZGl0Py5kb2N1bWVudENoYW5nZXMpIHtcbiAgICAgICAgZm9yIChjb25zdCBkYyBvZiBlZGl0LmRvY3VtZW50Q2hhbmdlcyBhcyBhbnlbXSkge1xuICAgICAgICAgICAgaWYgKGRjPy50ZXh0RG9jdW1lbnQ/LnVyaSAmJiBBcnJheS5pc0FycmF5KGRjLmVkaXRzKSkgcHVzaChkYy50ZXh0RG9jdW1lbnQudXJpLCBkYy5lZGl0cyk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgcmV0dXJuIHsgZWRpdHMgfTtcbn1cblxuLyoqIE1hcHMgYSB2aXJ0dWFsIGVkaXRvciBVUkkgYmFjayB0byB0aGUgSURFIHdvcmtzcGFjZSBwYXRoICh7QGNvZGUgL3dzL3Byb2ovLi4ufSkuICovXG5mdW5jdGlvbiB1cmlUb1dvcmtzcGFjZVBhdGgodXJpOiBzdHJpbmcpOiBzdHJpbmcgfCBudWxsIHtcbiAgICBpZiAoIXVyaS5zdGFydHNXaXRoKFZJUlRVQUxfRklMRV9QUkVGSVgpKSByZXR1cm4gbnVsbDtcbiAgICByZXR1cm4gZGVjb2RlVVJJQ29tcG9uZW50KHVyaS5zdWJzdHJpbmcoVklSVFVBTF9GSUxFX1BSRUZJWC5sZW5ndGgpKTtcbn1cblxuLyoqIFJlYWRzIGEgd29ya3NwYWNlIGZpbGUncyBjdXJyZW50IHRleHQgb3ZlciB0aGUgSURFIFJFU1QgQVBJLiAqL1xuYXN5bmMgZnVuY3Rpb24gZmV0Y2hXb3Jrc3BhY2VGaWxlVGV4dChpZGVQYXRoOiBzdHJpbmcpOiBQcm9taXNlPHN0cmluZz4ge1xuICAgIGNvbnN0IHJlc3BvbnNlID0gYXdhaXQgZmV0Y2goJy9zZXJ2aWNlcy9pZGUvd29ya3NwYWNlcycgKyBpZGVQYXRoLCB7IGhlYWRlcnM6IHsgJ1gtUmVxdWVzdGVkLVdpdGgnOiAnRmV0Y2gnIH0gfSk7XG4gICAgaWYgKCFyZXNwb25zZS5vaykge1xuICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYENvdWxkIG5vdCByZWFkICR7aWRlUGF0aH0gKEhUVFAgJHtyZXNwb25zZS5zdGF0dXN9KWApO1xuICAgIH1cbiAgICByZXR1cm4gcmVzcG9uc2UudGV4dCgpO1xufVxuXG4vKiogQXBwbGllcyBMU1AgdGV4dCBlZGl0cyB0byBhIHN0cmluZy4gT2Zmc2V0cyBhcmUgcmVzb2x2ZWQgYWdhaW5zdCB0aGUgb3JpZ2luYWwgdGV4dCBhbmQgZWRpdHMgYXJlXG4gKiAgYXBwbGllZCBmcm9tIHRoZSBlbmQgYmFja3dhcmRzLCBzbyBlYXJsaWVyIG9mZnNldHMgc3RheSB2YWxpZC4gKi9cbmZ1bmN0aW9uIGFwcGx5RWRpdHNUb1RleHQodGV4dDogc3RyaW5nLCBlZGl0czogVGV4dEVkaXRbXSk6IHN0cmluZyB7XG4gICAgY29uc3QgbGluZVN0YXJ0cyA9IFswXTtcbiAgICBmb3IgKGxldCBpID0gMDsgaSA8IHRleHQubGVuZ3RoOyBpKyspIHtcbiAgICAgICAgaWYgKHRleHQuY2hhckNvZGVBdChpKSA9PT0gMTAgLyogXFxuICovKSBsaW5lU3RhcnRzLnB1c2goaSArIDEpO1xuICAgIH1cbiAgICBjb25zdCBvZmZzZXQgPSAocDogeyBsaW5lOiBudW1iZXI7IGNoYXJhY3RlcjogbnVtYmVyIH0pID0+IChsaW5lU3RhcnRzW3AubGluZV0gPz8gdGV4dC5sZW5ndGgpICsgcC5jaGFyYWN0ZXI7XG4gICAgY29uc3Qgb3JkZXJlZCA9IGVkaXRzLnNsaWNlKClcbiAgICAgICAgICAgICAgICAgICAgICAgICAuc29ydCgoYSwgYikgPT4gb2Zmc2V0KGIucmFuZ2Uuc3RhcnQpIC0gb2Zmc2V0KGEucmFuZ2Uuc3RhcnQpKTtcbiAgICBsZXQgcmVzdWx0ID0gdGV4dDtcbiAgICBmb3IgKGNvbnN0IGUgb2Ygb3JkZXJlZCkge1xuICAgICAgICByZXN1bHQgPSByZXN1bHQuc2xpY2UoMCwgb2Zmc2V0KGUucmFuZ2Uuc3RhcnQpKSArIGUubmV3VGV4dCArIHJlc3VsdC5zbGljZShvZmZzZXQoZS5yYW5nZS5lbmQpKTtcbiAgICB9XG4gICAgcmV0dXJuIHJlc3VsdDtcbn1cblxuLyoqXG4gKiBBcHBsaWVzIGEgSkRULkxTIHJlbmFtZSB7QGxpbmsgV29ya3NwYWNlRWRpdH0gYWNyb3NzIHRoZSB3aG9sZSB3b3Jrc3BhY2U6IHRleHQgZWRpdHMgaW4gZXZlcnlcbiAqIGFmZmVjdGVkIGZpbGUgcGx1cyBhbnkge0Bjb2RlIFJlbmFtZUZpbGV9IG9wZXJhdGlvbiAoYSBwdWJsaWMtdHlwZSByZW5hbWUgcmVuYW1lcyBpdHMgb3duXG4gKiB7QGNvZGUgLmphdmF9IGZpbGUpLiBUaGUgY3VycmVudCBmaWxlJ3MgZWRpdHMgZ28gdGhyb3VnaCB0aGUgbGl2ZSBNb25hY28gbW9kZWw7IHRoZSByZXN0IGFyZSByZWFkLFxuICogZWRpdGVkIGFuZCB3cml0dGVuIGJhY2sgb3ZlciBSRVNULiBQZXJzaXN0ZW5jZSAoQ1NSRi1ndWFyZGVkIHdyaXRlcywgdGhlIHRhYiBzd2l0Y2ggd2hlbiB0aGUgY3VycmVudFxuICogZmlsZSBpcyByZW5hbWVkLCBhbmQgcmVsb2FkaW5nIG90aGVyIG9wZW4gZWRpdG9ycykgaXMgZGVsZWdhdGVkIHRvIHRoZSBJREUgdmlhIGphdmFMc3BQZXJzaXN0UmVuYW1lLlxuICovXG5hc3luYyBmdW5jdGlvbiBhcHBseVJlbmFtZUFjcm9zc1dvcmtzcGFjZShtb2RlbDogbW9uYWNvLmVkaXRvci5JVGV4dE1vZGVsLCBlZGl0OiBXb3Jrc3BhY2VFZGl0KTogUHJvbWlzZTx2b2lkPiB7XG4gICAgY29uc3QgY3VycmVudFVyaSA9IG1vZGVsLnVyaS50b1N0cmluZygpO1xuICAgIGNvbnN0IHRleHRCeVVyaTogUmVjb3JkPHN0cmluZywgVGV4dEVkaXRbXT4gPSB7fTtcbiAgICBjb25zdCByZW5hbWVCeVVyaTogUmVjb3JkPHN0cmluZywgc3RyaW5nPiA9IHt9O1xuXG4gICAgaWYgKGVkaXQuY2hhbmdlcykge1xuICAgICAgICBmb3IgKGNvbnN0IHVyaSBpbiBlZGl0LmNoYW5nZXMpIHRleHRCeVVyaVt1cmldID0gKHRleHRCeVVyaVt1cmldID8/IFtdKS5jb25jYXQoZWRpdC5jaGFuZ2VzW3VyaV0pO1xuICAgIH1cbiAgICBpZiAoZWRpdC5kb2N1bWVudENoYW5nZXMpIHtcbiAgICAgICAgZm9yIChjb25zdCBkYyBvZiBlZGl0LmRvY3VtZW50Q2hhbmdlcyBhcyBhbnlbXSkge1xuICAgICAgICAgICAgaWYgKGRjPy5raW5kID09PSAncmVuYW1lJyAmJiBkYy5vbGRVcmkgJiYgZGMubmV3VXJpKSB7XG4gICAgICAgICAgICAgICAgcmVuYW1lQnlVcmlbZGMub2xkVXJpXSA9IGRjLm5ld1VyaTtcbiAgICAgICAgICAgIH0gZWxzZSBpZiAoZGM/LnRleHREb2N1bWVudD8udXJpICYmIEFycmF5LmlzQXJyYXkoZGMuZWRpdHMpKSB7XG4gICAgICAgICAgICAgICAgdGV4dEJ5VXJpW2RjLnRleHREb2N1bWVudC51cmldID0gKHRleHRCeVVyaVtkYy50ZXh0RG9jdW1lbnQudXJpXSA/PyBbXSkuY29uY2F0KGRjLmVkaXRzKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgIH1cblxuICAgIGNvbnN0IHBheWxvYWQ6IHtcbiAgICAgICAgY3VycmVudFBhdGg6IHN0cmluZyB8IG51bGw7XG4gICAgICAgIGN1cnJlbnRDb250ZW50OiBzdHJpbmcgfCBudWxsO1xuICAgICAgICBjdXJyZW50TmV3UGF0aDogc3RyaW5nIHwgbnVsbDtcbiAgICAgICAgd3JpdGVzOiBBcnJheTx7IHBhdGg6IHN0cmluZyB8IG51bGw7IGNvbnRlbnQ6IHN0cmluZyB9PjtcbiAgICAgICAgcmVuYW1lczogQXJyYXk8eyBvbGRQYXRoOiBzdHJpbmcgfCBudWxsOyBuZXdQYXRoOiBzdHJpbmcgfCBudWxsOyBjb250ZW50OiBzdHJpbmcgfT47XG4gICAgfSA9IHsgY3VycmVudFBhdGg6IHVyaVRvV29ya3NwYWNlUGF0aChjdXJyZW50VXJpKSwgY3VycmVudENvbnRlbnQ6IG51bGwsIGN1cnJlbnROZXdQYXRoOiBudWxsLCB3cml0ZXM6IFtdLCByZW5hbWVzOiBbXSB9O1xuXG4gICAgY29uc3QgdXJpcyA9IG5ldyBTZXQ8c3RyaW5nPihbLi4uT2JqZWN0LmtleXModGV4dEJ5VXJpKSwgLi4uT2JqZWN0LmtleXMocmVuYW1lQnlVcmkpXSk7XG4gICAgZm9yIChjb25zdCB1cmkgb2YgdXJpcykge1xuICAgICAgICBjb25zdCBlZGl0cyA9IHRleHRCeVVyaVt1cmldID8/IFtdO1xuICAgICAgICBsZXQgY29udGVudDogc3RyaW5nO1xuICAgICAgICBpZiAodXJpID09PSBjdXJyZW50VXJpKSB7XG4gICAgICAgICAgICBpZiAoZWRpdHMubGVuZ3RoKSB7XG4gICAgICAgICAgICAgICAgbW9kZWwucHVzaEVkaXRPcGVyYXRpb25zKFtdLCBlZGl0cy5tYXAoZSA9PiAoeyByYW5nZTogbHNwUmFuZ2VUb01vbmFjbyhlLnJhbmdlKSwgdGV4dDogZS5uZXdUZXh0LCBmb3JjZU1vdmVNYXJrZXJzOiB0cnVlIH0pKSwgKCkgPT4gbnVsbCk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBjb250ZW50ID0gbW9kZWwuZ2V0VmFsdWUoKTtcbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIGNvbnN0IHNvdXJjZSA9IGF3YWl0IGZldGNoV29ya3NwYWNlRmlsZVRleHQodXJpVG9Xb3Jrc3BhY2VQYXRoKHVyaSkgPz8gdXJpKTtcbiAgICAgICAgICAgIGNvbnRlbnQgPSBlZGl0cy5sZW5ndGggPyBhcHBseUVkaXRzVG9UZXh0KHNvdXJjZSwgZWRpdHMpIDogc291cmNlO1xuICAgICAgICB9XG4gICAgICAgIGNvbnN0IG5ld1VyaSA9IHJlbmFtZUJ5VXJpW3VyaV07XG4gICAgICAgIGlmICh1cmkgPT09IGN1cnJlbnRVcmkpIHtcbiAgICAgICAgICAgIHBheWxvYWQuY3VycmVudENvbnRlbnQgPSBjb250ZW50O1xuICAgICAgICAgICAgcGF5bG9hZC5jdXJyZW50TmV3UGF0aCA9IG5ld1VyaSA/IHVyaVRvV29ya3NwYWNlUGF0aChuZXdVcmkpIDogbnVsbDtcbiAgICAgICAgfSBlbHNlIGlmIChuZXdVcmkpIHtcbiAgICAgICAgICAgIHBheWxvYWQucmVuYW1lcy5wdXNoKHsgb2xkUGF0aDogdXJpVG9Xb3Jrc3BhY2VQYXRoKHVyaSksIG5ld1BhdGg6IHVyaVRvV29ya3NwYWNlUGF0aChuZXdVcmkpLCBjb250ZW50IH0pO1xuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgcGF5bG9hZC53cml0ZXMucHVzaCh7IHBhdGg6IHVyaVRvV29ya3NwYWNlUGF0aCh1cmkpLCBjb250ZW50IH0pO1xuICAgICAgICB9XG4gICAgfVxuXG4gICAgYXdhaXQgKGdsb2JhbFRoaXMgYXMgYW55KS5qYXZhTHNwUGVyc2lzdFJlbmFtZShwYXlsb2FkKTtcbn1cblxuZnVuY3Rpb24gamR0bHNTZXR0aW5ncygpIHtcbiAgICByZXR1cm4ge1xuICAgICAgICBqYXZhOiB7XG4gICAgICAgICAgICBpbXBvcnQ6IHtcbiAgICAgICAgICAgICAgICBtYXZlbjogICAgICB7IGVuYWJsZWQ6IHRydWUgfSxcbiAgICAgICAgICAgICAgICBncmFkbGU6ICAgICB7IGVuYWJsZWQ6IGZhbHNlIH0sXG4gICAgICAgICAgICAgICAgZXhjbHVzaW9uczogWycqKi9ub2RlX21vZHVsZXMvKionLCAnKiovLm1ldGFkYXRhLyoqJywgJyoqL2FyY2hldHlwZS1yZXNvdXJjZXMvKionXSxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICBhdXRvYnVpbGQ6IHsgZW5hYmxlZDogdHJ1ZSB9LFxuICAgICAgICAgICAgY29tcGxldGlvbjoge1xuICAgICAgICAgICAgICAgIG92ZXJ3cml0ZTogICAgICAgICAgICB0cnVlLFxuICAgICAgICAgICAgICAgIGd1ZXNzTWV0aG9kQXJndW1lbnRzOiBmYWxzZSxcbiAgICAgICAgICAgICAgICBmaWx0ZXJlZFR5cGVzOiBbXG4gICAgICAgICAgICAgICAgICAgICdjb20uc3VuLionLCAnc3VuLionLCAnamRrLionLFxuICAgICAgICAgICAgICAgICAgICAnb3JnLmVjbGlwc2UuamR0LmludGVybmFsLionLFxuICAgICAgICAgICAgICAgICAgICAnb3JnLmVjbGlwc2UuY29yZS5pbnRlcm5hbC4qJyxcbiAgICAgICAgICAgICAgICAgICAgJ29yZy5lY2xpcHNlLm9zZ2kuaW50ZXJuYWwuKicsXG4gICAgICAgICAgICAgICAgXSxcbiAgICAgICAgICAgICAgICBpbXBvcnRPcmRlcjogWydqYXZhJywgJ2phdmF4JywgJ29yZycsICdjb20nLCAnJ10sXG4gICAgICAgICAgICB9LFxuICAgICAgICAgICAgc2lnbmF0dXJlSGVscDogIHsgZW5hYmxlZDogdHJ1ZSB9LFxuICAgICAgICAgICAgZm9ybWF0OiAgICAgICAgIHsgZW5hYmxlZDogdHJ1ZSB9LFxuICAgICAgICAgICAgc2F2ZUFjdGlvbnM6ICAgIHsgb3JnYW5pemVJbXBvcnRzOiBmYWxzZSB9LFxuICAgICAgICB9LFxuICAgIH07XG59XG4iLCAiLyogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbiAqIENvcHlyaWdodCAoYykgMjAyNCBUeXBlRm94IGFuZCBvdGhlcnMuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMSUNFTlNFIGluIHRoZSBwYWNrYWdlIHJvb3QgZm9yIGxpY2Vuc2UgaW5mb3JtYXRpb24uXG4gKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0gKi9cblxuaW1wb3J0IHsgRGlzcG9zYWJsZSB9IGZyb20gJ3ZzY29kZS1qc29ucnBjJztcblxuZXhwb3J0IGNsYXNzIERpc3Bvc2FibGVDb2xsZWN0aW9uIGltcGxlbWVudHMgRGlzcG9zYWJsZSB7XG4gICAgcHJvdGVjdGVkIHJlYWRvbmx5IGRpc3Bvc2FibGVzOiBEaXNwb3NhYmxlW10gPSBbXTtcblxuICAgIGRpc3Bvc2UoKTogdm9pZCB7XG4gICAgICAgIHdoaWxlICh0aGlzLmRpc3Bvc2FibGVzLmxlbmd0aCAhPT0gMCkge1xuICAgICAgICAgICAgdGhpcy5kaXNwb3NhYmxlcy5wb3AoKSEuZGlzcG9zZSgpO1xuICAgICAgICB9XG4gICAgfVxuXG4gICAgcHVzaChkaXNwb3NhYmxlOiBEaXNwb3NhYmxlKTogRGlzcG9zYWJsZSB7XG4gICAgICAgIGNvbnN0IGRpc3Bvc2FibGVzID0gdGhpcy5kaXNwb3NhYmxlcztcbiAgICAgICAgZGlzcG9zYWJsZXMucHVzaChkaXNwb3NhYmxlKTtcbiAgICAgICAgcmV0dXJuIHtcbiAgICAgICAgICAgIGRpc3Bvc2UoKTogdm9pZCB7XG4gICAgICAgICAgICAgICAgY29uc3QgaW5kZXggPSBkaXNwb3NhYmxlcy5pbmRleE9mKGRpc3Bvc2FibGUpO1xuICAgICAgICAgICAgICAgIGlmIChpbmRleCAhPT0gLTEpIHtcbiAgICAgICAgICAgICAgICAgICAgZGlzcG9zYWJsZXMuc3BsaWNlKGluZGV4LCAxKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH07XG4gICAgfVxufVxuIiwgIi8qIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiBDb3B5cmlnaHQgKGMpIDIwMjQgVHlwZUZveCBhbmQgb3RoZXJzLlxuICogTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTElDRU5TRSBpbiB0aGUgcGFja2FnZSByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG5cbmltcG9ydCB7IERpc3Bvc2FibGUgfSBmcm9tICd2c2NvZGUtanNvbnJwYyc7XG5pbXBvcnQgdHlwZSB7IElDb25uZWN0aW9uIH0gZnJvbSAnLi4vc2VydmVyL2Nvbm5lY3Rpb24uanMnO1xuXG5leHBvcnQgaW50ZXJmYWNlIElXZWJTb2NrZXQgZXh0ZW5kcyBEaXNwb3NhYmxlIHtcbiAgICBzZW5kKGNvbnRlbnQ6IHN0cmluZyk6IHZvaWQ7XG4gICAgLy8gZXNsaW50LWRpc2FibGUtbmV4dC1saW5lIEB0eXBlc2NyaXB0LWVzbGludC9uby1leHBsaWNpdC1hbnlcbiAgICBvbk1lc3NhZ2UoY2I6IChkYXRhOiBhbnkpID0+IHZvaWQpOiB2b2lkO1xuICAgIC8vIGVzbGludC1kaXNhYmxlLW5leHQtbGluZSBAdHlwZXNjcmlwdC1lc2xpbnQvbm8tZXhwbGljaXQtYW55XG4gICAgb25FcnJvcihjYjogKHJlYXNvbjogYW55KSA9PiB2b2lkKTogdm9pZDtcbiAgICBvbkNsb3NlKGNiOiAoY29kZTogbnVtYmVyLCByZWFzb246IHN0cmluZykgPT4gdm9pZCk6IHZvaWQ7XG59XG5cbmV4cG9ydCBpbnRlcmZhY2UgSVdlYlNvY2tldENvbm5lY3Rpb24gZXh0ZW5kcyBJQ29ubmVjdGlvbiB7XG4gICAgcmVhZG9ubHkgc29ja2V0OiBJV2ViU29ja2V0O1xufVxuIiwgIi8qIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiBDb3B5cmlnaHQgKGMpIDIwMjQgVHlwZUZveCBhbmQgb3RoZXJzLlxuICogTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTElDRU5TRSBpbiB0aGUgcGFja2FnZSByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG5cbmltcG9ydCB7IERpc3Bvc2FibGUgfSBmcm9tICd2c2NvZGUtanNvbnJwYyc7XG4vLyBUT0RPOiBVc2UgZW52aXJvbm1lbnQtc3BlY2lmaWMgaW1wb3J0cyAodnNjb2RlLWpzb25ycGMvYnJvd3NlciBvciB2c2NvZGUtanNvbnJwYy9ub2RlKVxuLy8gd2hlbiB1cGdyYWRpbmcgdG8gdnNjb2RlLWpzb25ycGNAOS54LngtbmV4dC5YIHdoaWNoIHN1cHBvcnRzIHByb3BlciBleHBvcnQgbWFwc1xuaW1wb3J0IHsgdHlwZSBEYXRhQ2FsbGJhY2ssIEFic3RyYWN0TWVzc2FnZVJlYWRlciwgTWVzc2FnZVJlYWRlciB9IGZyb20gJ3ZzY29kZS1qc29ucnBjJztcbmltcG9ydCB0eXBlIHsgSVdlYlNvY2tldCB9IGZyb20gJy4vc29ja2V0LmpzJztcblxuZXhwb3J0IGNsYXNzIFdlYlNvY2tldE1lc3NhZ2VSZWFkZXIgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VSZWFkZXIgaW1wbGVtZW50cyBNZXNzYWdlUmVhZGVyIHtcbiAgICBwcm90ZWN0ZWQgcmVhZG9ubHkgc29ja2V0OiBJV2ViU29ja2V0O1xuICAgIHByb3RlY3RlZCBzdGF0ZTogJ2luaXRpYWwnIHwgJ2xpc3RlbmluZycgfCAnY2xvc2VkJyA9ICdpbml0aWFsJztcbiAgICBwcm90ZWN0ZWQgY2FsbGJhY2s6IERhdGFDYWxsYmFjayB8IHVuZGVmaW5lZDtcbiAgICAvLyBlc2xpbnQtZGlzYWJsZS1uZXh0LWxpbmUgQHR5cGVzY3JpcHQtZXNsaW50L25vLWV4cGxpY2l0LWFueVxuICAgIHByb3RlY3RlZCByZWFkb25seSBldmVudHM6IEFycmF5PHsgbWVzc2FnZT86IGFueSwgZXJyb3I/OiBhbnkgfT4gPSBbXTtcblxuICAgIGNvbnN0cnVjdG9yKHNvY2tldDogSVdlYlNvY2tldCkge1xuICAgICAgICBzdXBlcigpO1xuICAgICAgICB0aGlzLnNvY2tldCA9IHNvY2tldDtcbiAgICAgICAgdGhpcy5zb2NrZXQub25NZXNzYWdlKG1lc3NhZ2UgPT5cbiAgICAgICAgICAgIHRoaXMucmVhZE1lc3NhZ2UobWVzc2FnZSlcbiAgICAgICAgKTtcbiAgICAgICAgdGhpcy5zb2NrZXQub25FcnJvcihlcnJvciA9PlxuICAgICAgICAgICAgdGhpcy5maXJlRXJyb3IoZXJyb3IpXG4gICAgICAgICk7XG4gICAgICAgIHRoaXMuc29ja2V0Lm9uQ2xvc2UoKGNvZGUsIHJlYXNvbikgPT4ge1xuICAgICAgICAgICAgaWYgKGNvZGUgIT09IDEwMDApIHtcbiAgICAgICAgICAgICAgICBjb25zdCBlcnJvcjogRXJyb3IgPSB7XG4gICAgICAgICAgICAgICAgICAgIG5hbWU6ICcnICsgY29kZSxcbiAgICAgICAgICAgICAgICAgICAgbWVzc2FnZTogYEVycm9yIGR1cmluZyBzb2NrZXQgcmVjb25uZWN0OiBjb2RlID0gJHtjb2RlfSwgcmVhc29uID0gJHtyZWFzb259YFxuICAgICAgICAgICAgICAgIH07XG4gICAgICAgICAgICAgICAgdGhpcy5maXJlRXJyb3IoZXJyb3IpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgdGhpcy5maXJlQ2xvc2UoKTtcbiAgICAgICAgfSk7XG4gICAgfVxuXG4gICAgbGlzdGVuKGNhbGxiYWNrOiBEYXRhQ2FsbGJhY2spOiBEaXNwb3NhYmxlIHtcbiAgICAgICAgaWYgKHRoaXMuc3RhdGUgPT09ICdpbml0aWFsJykge1xuICAgICAgICAgICAgdGhpcy5zdGF0ZSA9ICdsaXN0ZW5pbmcnO1xuICAgICAgICAgICAgdGhpcy5jYWxsYmFjayA9IGNhbGxiYWNrO1xuICAgICAgICAgICAgd2hpbGUgKHRoaXMuZXZlbnRzLmxlbmd0aCAhPT0gMCkge1xuICAgICAgICAgICAgICAgIGNvbnN0IGV2ZW50ID0gdGhpcy5ldmVudHMucG9wKCkhO1xuICAgICAgICAgICAgICAgIGlmIChldmVudC5tZXNzYWdlICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5yZWFkTWVzc2FnZShldmVudC5tZXNzYWdlKTtcbiAgICAgICAgICAgICAgICB9IGVsc2UgaWYgKGV2ZW50LmVycm9yICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5maXJlRXJyb3IoZXZlbnQuZXJyb3IpO1xuICAgICAgICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIHRoaXMuZmlyZUNsb3NlKCk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICBkaXNwb3NlOiAoKSA9PiB7XG4gICAgICAgICAgICAgICAgaWYgKHRoaXMuY2FsbGJhY2sgPT09IGNhbGxiYWNrKSB7XG4gICAgICAgICAgICAgICAgICAgIHRoaXMuc3RhdGUgPSAnaW5pdGlhbCc7XG4gICAgICAgICAgICAgICAgICAgIHRoaXMuY2FsbGJhY2sgPSB1bmRlZmluZWQ7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICB9O1xuICAgIH1cblxuICAgIG92ZXJyaWRlIGRpc3Bvc2UoKSB7XG4gICAgICAgIHN1cGVyLmRpc3Bvc2UoKTtcbiAgICAgICAgdGhpcy5zdGF0ZSA9ICdpbml0aWFsJztcbiAgICAgICAgdGhpcy5jYWxsYmFjayA9IHVuZGVmaW5lZDtcbiAgICAgICAgdGhpcy5ldmVudHMuc3BsaWNlKDAsIHRoaXMuZXZlbnRzLmxlbmd0aCk7XG4gICAgfVxuXG4gICAgLy8gZXNsaW50LWRpc2FibGUtbmV4dC1saW5lIEB0eXBlc2NyaXB0LWVzbGludC9uby1leHBsaWNpdC1hbnlcbiAgICBwcm90ZWN0ZWQgcmVhZE1lc3NhZ2UobWVzc2FnZTogYW55KTogdm9pZCB7XG4gICAgICAgIGlmICh0aGlzLnN0YXRlID09PSAnaW5pdGlhbCcpIHtcbiAgICAgICAgICAgIHRoaXMuZXZlbnRzLnNwbGljZSgwLCAwLCB7IG1lc3NhZ2UgfSk7XG4gICAgICAgIH0gZWxzZSBpZiAodGhpcy5zdGF0ZSA9PT0gJ2xpc3RlbmluZycpIHtcbiAgICAgICAgICAgIHRyeSB7XG4gICAgICAgICAgICAgICAgY29uc3QgZGF0YSA9IEpTT04ucGFyc2UobWVzc2FnZSk7XG4gICAgICAgICAgICAgICAgdGhpcy5jYWxsYmFjayEoZGF0YSk7XG4gICAgICAgICAgICB9IGNhdGNoIChlcnIpIHtcbiAgICAgICAgICAgICAgICBjb25zdCBlcnJvcjogRXJyb3IgPSB7XG4gICAgICAgICAgICAgICAgICAgIG5hbWU6ICcnICsgNDAwLFxuICAgICAgICAgICAgICAgICAgICAvLyBlc2xpbnQtZGlzYWJsZS1uZXh0LWxpbmUgQHR5cGVzY3JpcHQtZXNsaW50L25vLWV4cGxpY2l0LWFueVxuICAgICAgICAgICAgICAgICAgICBtZXNzYWdlOiBgRXJyb3IgZHVyaW5nIG1lc3NhZ2UgcGFyc2luZywgcmVhc29uID0gJHt0eXBlb2YgZXJyID09PSAnb2JqZWN0JyA/IChlcnIgYXMgYW55KS5tZXNzYWdlIDogJ3Vua25vd24nfWBcbiAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICAgIHRoaXMuZmlyZUVycm9yKGVycm9yKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgIH1cblxuICAgIC8vIGVzbGludC1kaXNhYmxlLW5leHQtbGluZSBAdHlwZXNjcmlwdC1lc2xpbnQvbm8tZXhwbGljaXQtYW55XG4gICAgcHJvdGVjdGVkIG92ZXJyaWRlIGZpcmVFcnJvcihlcnJvcjogYW55KTogdm9pZCB7XG4gICAgICAgIGlmICh0aGlzLnN0YXRlID09PSAnaW5pdGlhbCcpIHtcbiAgICAgICAgICAgIHRoaXMuZXZlbnRzLnNwbGljZSgwLCAwLCB7IGVycm9yIH0pO1xuICAgICAgICB9IGVsc2UgaWYgKHRoaXMuc3RhdGUgPT09ICdsaXN0ZW5pbmcnKSB7XG4gICAgICAgICAgICBzdXBlci5maXJlRXJyb3IoZXJyb3IpO1xuICAgICAgICB9XG4gICAgfVxuXG4gICAgcHJvdGVjdGVkIG92ZXJyaWRlIGZpcmVDbG9zZSgpOiB2b2lkIHtcbiAgICAgICAgaWYgKHRoaXMuc3RhdGUgPT09ICdpbml0aWFsJykge1xuICAgICAgICAgICAgdGhpcy5ldmVudHMuc3BsaWNlKDAsIDAsIHt9KTtcbiAgICAgICAgfSBlbHNlIGlmICh0aGlzLnN0YXRlID09PSAnbGlzdGVuaW5nJykge1xuICAgICAgICAgICAgc3VwZXIuZmlyZUNsb3NlKCk7XG4gICAgICAgIH1cbiAgICAgICAgdGhpcy5zdGF0ZSA9ICdjbG9zZWQnO1xuICAgIH1cbn1cbiIsICIvKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSAyMDI0IFR5cGVGb3ggYW5kIG90aGVycy5cbiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBNSVQgTGljZW5zZS4gU2VlIExJQ0VOU0UgaW4gdGhlIHBhY2thZ2Ugcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSAqL1xuXG5pbXBvcnQgeyBNZXNzYWdlIH0gZnJvbSAndnNjb2RlLWpzb25ycGMnO1xuaW1wb3J0IHsgQWJzdHJhY3RNZXNzYWdlV3JpdGVyLCBNZXNzYWdlV3JpdGVyIH0gZnJvbSAndnNjb2RlLWpzb25ycGMnO1xuaW1wb3J0IHR5cGUgeyBJV2ViU29ja2V0IH0gZnJvbSAnLi9zb2NrZXQuanMnO1xuXG5leHBvcnQgY2xhc3MgV2ViU29ja2V0TWVzc2FnZVdyaXRlciBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVdyaXRlciBpbXBsZW1lbnRzIE1lc3NhZ2VXcml0ZXIge1xuICAgIHByb3RlY3RlZCBlcnJvckNvdW50ID0gMDtcbiAgICBwcm90ZWN0ZWQgcmVhZG9ubHkgc29ja2V0OiBJV2ViU29ja2V0O1xuXG4gICAgY29uc3RydWN0b3Ioc29ja2V0OiBJV2ViU29ja2V0KSB7XG4gICAgICAgIHN1cGVyKCk7XG4gICAgICAgIHRoaXMuc29ja2V0ID0gc29ja2V0O1xuICAgIH1cblxuICAgIGVuZCgpOiB2b2lkIHtcbiAgICB9XG5cbiAgICBhc3luYyB3cml0ZShtc2c6IE1lc3NhZ2UpOiBQcm9taXNlPHZvaWQ+IHtcbiAgICAgICAgdHJ5IHtcbiAgICAgICAgICAgIGNvbnN0IGNvbnRlbnQgPSBKU09OLnN0cmluZ2lmeShtc2cpO1xuICAgICAgICAgICAgdGhpcy5zb2NrZXQuc2VuZChjb250ZW50KTtcbiAgICAgICAgfSBjYXRjaCAoZSkge1xuICAgICAgICAgICAgdGhpcy5lcnJvckNvdW50Kys7XG4gICAgICAgICAgICB0aGlzLmZpcmVFcnJvcihlLCBtc2csIHRoaXMuZXJyb3JDb3VudCk7XG4gICAgICAgIH1cbiAgICB9XG59XG4iLCAiLyogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbiAqIENvcHlyaWdodCAoYykgMjAyNCBUeXBlRm94IGFuZCBvdGhlcnMuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMSUNFTlNFIGluIHRoZSBwYWNrYWdlIHJvb3QgZm9yIGxpY2Vuc2UgaW5mb3JtYXRpb24uXG4gKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0gKi9cblxuaW1wb3J0IHR5cGUgeyBNZXNzYWdlQ29ubmVjdGlvbiwgTG9nZ2VyIH0gZnJvbSAndnNjb2RlLWpzb25ycGMnO1xuaW1wb3J0IHsgY3JlYXRlTWVzc2FnZUNvbm5lY3Rpb24gfSBmcm9tICd2c2NvZGUtanNvbnJwYyc7XG5pbXBvcnQgdHlwZSB7IElXZWJTb2NrZXQgfSBmcm9tICcuL3NvY2tldC5qcyc7XG5pbXBvcnQgeyBXZWJTb2NrZXRNZXNzYWdlUmVhZGVyIH0gZnJvbSAnLi9yZWFkZXIuanMnO1xuaW1wb3J0IHsgV2ViU29ja2V0TWVzc2FnZVdyaXRlciB9IGZyb20gJy4vd3JpdGVyLmpzJztcblxuZXhwb3J0IGZ1bmN0aW9uIGNyZWF0ZVdlYlNvY2tldENvbm5lY3Rpb24oc29ja2V0OiBJV2ViU29ja2V0LCBsb2dnZXI6IExvZ2dlcik6IE1lc3NhZ2VDb25uZWN0aW9uIHtcbiAgICBjb25zdCBtZXNzYWdlUmVhZGVyID0gbmV3IFdlYlNvY2tldE1lc3NhZ2VSZWFkZXIoc29ja2V0KTtcbiAgICBjb25zdCBtZXNzYWdlV3JpdGVyID0gbmV3IFdlYlNvY2tldE1lc3NhZ2VXcml0ZXIoc29ja2V0KTtcbiAgICBjb25zdCBjb25uZWN0aW9uID0gY3JlYXRlTWVzc2FnZUNvbm5lY3Rpb24obWVzc2FnZVJlYWRlciwgbWVzc2FnZVdyaXRlciwgbG9nZ2VyKTtcbiAgICBjb25uZWN0aW9uLm9uQ2xvc2UoKCkgPT4gY29ubmVjdGlvbi5kaXNwb3NlKCkpO1xuICAgIHJldHVybiBjb25uZWN0aW9uO1xufVxuIiwgIi8qIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiBDb3B5cmlnaHQgKGMpIDIwMjQgVHlwZUZveCBhbmQgb3RoZXJzLlxuICogTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTElDRU5TRSBpbiB0aGUgcGFja2FnZSByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG5cbmltcG9ydCB0eXBlIHsgTWVzc2FnZUNvbm5lY3Rpb24sIExvZ2dlciB9IGZyb20gJ3ZzY29kZS1qc29ucnBjJztcbmltcG9ydCB7IGNyZWF0ZVdlYlNvY2tldENvbm5lY3Rpb24gfSBmcm9tICcuL3NvY2tldC9jb25uZWN0aW9uLmpzJztcbmltcG9ydCB0eXBlIHsgSVdlYlNvY2tldCB9IGZyb20gJy4vc29ja2V0L3NvY2tldC5qcyc7XG5pbXBvcnQgeyBDb25zb2xlTG9nZ2VyIH0gZnJvbSAnLi9sb2dnZXIuanMnO1xuXG5leHBvcnQgZnVuY3Rpb24gbGlzdGVuKG9wdGlvbnM6IHtcbiAgICB3ZWJTb2NrZXQ6IFdlYlNvY2tldDtcbiAgICBsb2dnZXI/OiBMb2dnZXI7XG4gICAgb25Db25uZWN0aW9uOiAoY29ubmVjdGlvbjogTWVzc2FnZUNvbm5lY3Rpb24pID0+IHZvaWQ7XG59KSB7XG4gICAgY29uc3QgeyB3ZWJTb2NrZXQsIG9uQ29ubmVjdGlvbiB9ID0gb3B0aW9ucztcbiAgICBjb25zdCBsb2dnZXIgPSBvcHRpb25zLmxvZ2dlciB8fCBuZXcgQ29uc29sZUxvZ2dlcigpO1xuICAgIHdlYlNvY2tldC5vbm9wZW4gPSAoKSA9PiB7XG4gICAgICAgIGNvbnN0IHNvY2tldCA9IHRvU29ja2V0KHdlYlNvY2tldCk7XG4gICAgICAgIGNvbnN0IGNvbm5lY3Rpb24gPSBjcmVhdGVXZWJTb2NrZXRDb25uZWN0aW9uKHNvY2tldCwgbG9nZ2VyKTtcbiAgICAgICAgb25Db25uZWN0aW9uKGNvbm5lY3Rpb24pO1xuICAgIH07XG59XG5cbmV4cG9ydCBmdW5jdGlvbiB0b1NvY2tldCh3ZWJTb2NrZXQ6IFdlYlNvY2tldCk6IElXZWJTb2NrZXQge1xuICAgIHJldHVybiB7XG4gICAgICAgIHNlbmQ6IGNvbnRlbnQgPT4gd2ViU29ja2V0LnNlbmQoY29udGVudCksXG4gICAgICAgIG9uTWVzc2FnZTogY2IgPT4ge1xuICAgICAgICAgICAgd2ViU29ja2V0Lm9ubWVzc2FnZSA9IGV2ZW50ID0+IGNiKGV2ZW50LmRhdGEpO1xuICAgICAgICB9LFxuICAgICAgICBvbkVycm9yOiBjYiA9PiB7XG4gICAgICAgICAgICAvLyBlc2xpbnQtZGlzYWJsZS1uZXh0LWxpbmUgQHR5cGVzY3JpcHQtZXNsaW50L25vLWV4cGxpY2l0LWFueVxuICAgICAgICAgICAgd2ViU29ja2V0Lm9uZXJyb3IgPSAoZXZlbnQ6IGFueSkgPT4ge1xuICAgICAgICAgICAgICAgIGlmIChPYmplY3QuaGFzT3duKGV2ZW50LCAnbWVzc2FnZScpKSB7XG4gICAgICAgICAgICAgICAgICAgIGNiKGV2ZW50Lm1lc3NhZ2UpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH07XG4gICAgICAgIH0sXG4gICAgICAgIG9uQ2xvc2U6IGNiID0+IHtcbiAgICAgICAgICAgIHdlYlNvY2tldC5vbmNsb3NlID0gZXZlbnQgPT4gY2IoZXZlbnQuY29kZSwgZXZlbnQucmVhc29uKTtcbiAgICAgICAgfSxcbiAgICAgICAgZGlzcG9zZTogKCkgPT4gd2ViU29ja2V0LmNsb3NlKClcbiAgICB9O1xufVxuIiwgIi8qXG4gKiBDb3B5cmlnaHQgKGMpIDIwMTAtMjAyNiBFY2xpcHNlIERpcmlnaWJsZSBjb250cmlidXRvcnNcbiAqXG4gKiBBbGwgcmlnaHRzIHJlc2VydmVkLiBUaGlzIHByb2dyYW0gYW5kIHRoZSBhY2NvbXBhbnlpbmcgbWF0ZXJpYWxzIGFyZSBtYWRlIGF2YWlsYWJsZSB1bmRlciB0aGVcbiAqIHRlcm1zIG9mIHRoZSBFY2xpcHNlIFB1YmxpYyBMaWNlbnNlIHYyLjAgd2hpY2ggYWNjb21wYW5pZXMgdGhpcyBkaXN0cmlidXRpb24sIGFuZCBpcyBhdmFpbGFibGUgYXRcbiAqIGh0dHA6Ly93d3cuZWNsaXBzZS5vcmcvbGVnYWwvZXBsLXYyMC5odG1sXG4gKlxuICogU1BEWC1GaWxlQ29weXJpZ2h0VGV4dDogRWNsaXBzZSBEaXJpZ2libGUgY29udHJpYnV0b3JzIFNQRFgtTGljZW5zZS1JZGVudGlmaWVyOiBFUEwtMi4wXG4gKi9cblxuLyoqXG4gKiBMYXp5IFByb3h5IHNoaW0gZm9yIE1vbmFjbyBFZGl0b3IgbG9hZGVkIHZpYSBBTUQuXG4gKlxuICogZWRpdG9yLmpzIHNldHMgZ2xvYmFsVGhpcy5tb25hY28gaW5zaWRlIHRoZSBBTUQgcmVxdWlyZSgpIGNhbGxiYWNrLCBiZWZvcmUgYW55XG4gKiBKYXZhTHNwQ2xpZW50TGliLmNvbm5lY3QoKSBjYWxsLiBBbGwgcHJvcGVydHkgYWNjZXNzZXMgZ28gdGhyb3VnaCBQcm94eS5nZXQoKSB0cmFwc1xuICogc28gdGhleSByZXNvbHZlIGFnYWluc3QgdGhlIGxpdmUgd2luZG93Lm1vbmFjbyBhdCBjYWxsIHRpbWUsIG5vdCBhdCBidW5kbGUgbG9hZCB0aW1lLlxuICpcbiAqIFR5cGVTY3JpcHQgcmVzb2x2ZXMgdHlwZXMgZnJvbSB0aGUgcmVhbCBtb25hY28tZWRpdG9yIGRldkRlcGVuZGVuY3k7IGVzYnVpbGQgcmVwbGFjZXNcbiAqIHRoZSBcIm1vbmFjby1lZGl0b3JcIiBpbXBvcnQgd2l0aCB0aGlzIGZpbGUgYXQgYnVuZGxlIHRpbWUgdmlhIHRoZSBhbGlhcyBvcHRpb24uXG4gKi9cblxudHlwZSBNID0gdHlwZW9mIGltcG9ydCgnbW9uYWNvLWVkaXRvcicpO1xuXG5mdW5jdGlvbiBtKCk6IE0ge1xuICAgIHJldHVybiAoZ2xvYmFsVGhpcyBhcyBhbnkpLm1vbmFjbyBhcyBNO1xufVxuXG5mdW5jdGlvbiBuczxUIGV4dGVuZHMgb2JqZWN0PihnZXR0ZXI6ICgpID0+IFQpOiBUIHtcbiAgICByZXR1cm4gbmV3IFByb3h5KHt9IGFzIFQsIHtcbiAgICAgICAgZ2V0OiAoXywgaykgPT4gKGdldHRlcigpIGFzIGFueSlbayBhcyBzdHJpbmddLFxuICAgIH0pO1xufVxuXG5mdW5jdGlvbiBjbHM8VD4oZ2V0dGVyOiAoKSA9PiBUKTogVCB7XG4gICAgcmV0dXJuIG5ldyBQcm94eShmdW5jdGlvbiAoKSB7fSBhcyBhbnksIHtcbiAgICAgICAgY29uc3RydWN0OiAoXywgYXJncykgPT4gbmV3IChnZXR0ZXIoKSBhcyBhbnkpKC4uLmFyZ3MpLFxuICAgICAgICBnZXQ6ICAgICAgIChfLCBrKSAgICA9PiAoZ2V0dGVyKCkgYXMgYW55KVtrIGFzIHN0cmluZ10sXG4gICAgfSkgYXMgVDtcbn1cblxuZXhwb3J0IGNvbnN0IGVkaXRvciAgICAgICAgID0gbnMoKCkgPT4gbSgpLmVkaXRvcik7XG5leHBvcnQgY29uc3QgbGFuZ3VhZ2VzICAgICAgPSBucygoKSA9PiBtKCkubGFuZ3VhZ2VzKTtcbmV4cG9ydCBjb25zdCBNYXJrZXJTZXZlcml0eSA9IG5zKCgpID0+IG0oKS5NYXJrZXJTZXZlcml0eSk7XG5leHBvcnQgY29uc3QgTWFya2VyVGFnICAgICAgPSBucygoKSA9PiAobSgpIGFzIGFueSkuTWFya2VyVGFnKTtcbmV4cG9ydCBjb25zdCBVcmkgICAgICAgICAgICA9IGNscygoKSA9PiBtKCkuVXJpKTtcbmV4cG9ydCBjb25zdCBSYW5nZSAgICAgICAgICA9IGNscygoKSA9PiBtKCkuUmFuZ2UpO1xuZXhwb3J0IGNvbnN0IFBvc2l0aW9uICAgICAgID0gY2xzKCgpID0+IG0oKS5Qb3NpdGlvbik7XG5leHBvcnQgY29uc3QgU2VsZWN0aW9uICAgICAgPSBjbHMoKCkgPT4gbSgpLlNlbGVjdGlvbik7XG5leHBvcnQgY29uc3QgS2V5Q29kZSAgICAgICAgPSBucygoKSA9PiBtKCkuS2V5Q29kZSk7XG5leHBvcnQgY29uc3QgS2V5TW9kICAgICAgICAgPSBucygoKSA9PiBtKCkuS2V5TW9kKTtcbiIsICIvKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG4ndXNlIHN0cmljdCc7XG5leHBvcnQgdmFyIERvY3VtZW50VXJpO1xuKGZ1bmN0aW9uIChEb2N1bWVudFVyaSkge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiB0eXBlb2YgdmFsdWUgPT09ICdzdHJpbmcnO1xuICAgIH1cbiAgICBEb2N1bWVudFVyaS5pcyA9IGlzO1xufSkoRG9jdW1lbnRVcmkgfHwgKERvY3VtZW50VXJpID0ge30pKTtcbmV4cG9ydCB2YXIgVVJJO1xuKGZ1bmN0aW9uIChVUkkpIHtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICByZXR1cm4gdHlwZW9mIHZhbHVlID09PSAnc3RyaW5nJztcbiAgICB9XG4gICAgVVJJLmlzID0gaXM7XG59KShVUkkgfHwgKFVSSSA9IHt9KSk7XG5leHBvcnQgdmFyIGludGVnZXI7XG4oZnVuY3Rpb24gKGludGVnZXIpIHtcbiAgICBpbnRlZ2VyLk1JTl9WQUxVRSA9IC0yMTQ3NDgzNjQ4O1xuICAgIGludGVnZXIuTUFYX1ZBTFVFID0gMjE0NzQ4MzY0NztcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICByZXR1cm4gdHlwZW9mIHZhbHVlID09PSAnbnVtYmVyJyAmJiBpbnRlZ2VyLk1JTl9WQUxVRSA8PSB2YWx1ZSAmJiB2YWx1ZSA8PSBpbnRlZ2VyLk1BWF9WQUxVRTtcbiAgICB9XG4gICAgaW50ZWdlci5pcyA9IGlzO1xufSkoaW50ZWdlciB8fCAoaW50ZWdlciA9IHt9KSk7XG5leHBvcnQgdmFyIHVpbnRlZ2VyO1xuKGZ1bmN0aW9uICh1aW50ZWdlcikge1xuICAgIHVpbnRlZ2VyLk1JTl9WQUxVRSA9IDA7XG4gICAgdWludGVnZXIuTUFYX1ZBTFVFID0gMjE0NzQ4MzY0NztcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICByZXR1cm4gdHlwZW9mIHZhbHVlID09PSAnbnVtYmVyJyAmJiB1aW50ZWdlci5NSU5fVkFMVUUgPD0gdmFsdWUgJiYgdmFsdWUgPD0gdWludGVnZXIuTUFYX1ZBTFVFO1xuICAgIH1cbiAgICB1aW50ZWdlci5pcyA9IGlzO1xufSkodWludGVnZXIgfHwgKHVpbnRlZ2VyID0ge30pKTtcbi8qKlxuICogVGhlIFBvc2l0aW9uIG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIFBvc2l0aW9ufSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBQb3NpdGlvbjtcbihmdW5jdGlvbiAoUG9zaXRpb24pIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IFBvc2l0aW9uIGxpdGVyYWwgZnJvbSB0aGUgZ2l2ZW4gbGluZSBhbmQgY2hhcmFjdGVyLlxuICAgICAqIEBwYXJhbSBsaW5lIFRoZSBwb3NpdGlvbidzIGxpbmUuXG4gICAgICogQHBhcmFtIGNoYXJhY3RlciBUaGUgcG9zaXRpb24ncyBjaGFyYWN0ZXIuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKGxpbmUsIGNoYXJhY3Rlcikge1xuICAgICAgICBpZiAobGluZSA9PT0gTnVtYmVyLk1BWF9WQUxVRSkge1xuICAgICAgICAgICAgbGluZSA9IHVpbnRlZ2VyLk1BWF9WQUxVRTtcbiAgICAgICAgfVxuICAgICAgICBpZiAoY2hhcmFjdGVyID09PSBOdW1iZXIuTUFYX1ZBTFVFKSB7XG4gICAgICAgICAgICBjaGFyYWN0ZXIgPSB1aW50ZWdlci5NQVhfVkFMVUU7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHsgbGluZSwgY2hhcmFjdGVyIH07XG4gICAgfVxuICAgIFBvc2l0aW9uLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIFBvc2l0aW9ufSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMub2JqZWN0TGl0ZXJhbChjYW5kaWRhdGUpICYmIElzLnVpbnRlZ2VyKGNhbmRpZGF0ZS5saW5lKSAmJiBJcy51aW50ZWdlcihjYW5kaWRhdGUuY2hhcmFjdGVyKTtcbiAgICB9XG4gICAgUG9zaXRpb24uaXMgPSBpcztcbn0pKFBvc2l0aW9uIHx8IChQb3NpdGlvbiA9IHt9KSk7XG4vKipcbiAqIFRoZSBSYW5nZSBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBSYW5nZX0gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgUmFuZ2U7XG4oZnVuY3Rpb24gKFJhbmdlKSB7XG4gICAgZnVuY3Rpb24gY3JlYXRlKG9uZSwgdHdvLCB0aHJlZSwgZm91cikge1xuICAgICAgICBpZiAoSXMudWludGVnZXIob25lKSAmJiBJcy51aW50ZWdlcih0d28pICYmIElzLnVpbnRlZ2VyKHRocmVlKSAmJiBJcy51aW50ZWdlcihmb3VyKSkge1xuICAgICAgICAgICAgcmV0dXJuIHsgc3RhcnQ6IFBvc2l0aW9uLmNyZWF0ZShvbmUsIHR3byksIGVuZDogUG9zaXRpb24uY3JlYXRlKHRocmVlLCBmb3VyKSB9O1xuICAgICAgICB9XG4gICAgICAgIGVsc2UgaWYgKFBvc2l0aW9uLmlzKG9uZSkgJiYgUG9zaXRpb24uaXModHdvKSkge1xuICAgICAgICAgICAgcmV0dXJuIHsgc3RhcnQ6IG9uZSwgZW5kOiB0d28gfTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgUmFuZ2UjY3JlYXRlIGNhbGxlZCB3aXRoIGludmFsaWQgYXJndW1lbnRzWyR7b25lfSwgJHt0d299LCAke3RocmVlfSwgJHtmb3VyfV1gKTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBSYW5nZS5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBSYW5nZX0gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLm9iamVjdExpdGVyYWwoY2FuZGlkYXRlKSAmJiBQb3NpdGlvbi5pcyhjYW5kaWRhdGUuc3RhcnQpICYmIFBvc2l0aW9uLmlzKGNhbmRpZGF0ZS5lbmQpO1xuICAgIH1cbiAgICBSYW5nZS5pcyA9IGlzO1xufSkoUmFuZ2UgfHwgKFJhbmdlID0ge30pKTtcbi8qKlxuICogVGhlIExvY2F0aW9uIG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIExvY2F0aW9ufSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBMb2NhdGlvbjtcbihmdW5jdGlvbiAoTG9jYXRpb24pIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgTG9jYXRpb24gbGl0ZXJhbC5cbiAgICAgKiBAcGFyYW0gdXJpIFRoZSBsb2NhdGlvbidzIHVyaS5cbiAgICAgKiBAcGFyYW0gcmFuZ2UgVGhlIGxvY2F0aW9uJ3MgcmFuZ2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKHVyaSwgcmFuZ2UpIHtcbiAgICAgICAgcmV0dXJuIHsgdXJpLCByYW5nZSB9O1xuICAgIH1cbiAgICBMb2NhdGlvbi5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBMb2NhdGlvbn0gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLm9iamVjdExpdGVyYWwoY2FuZGlkYXRlKSAmJiBSYW5nZS5pcyhjYW5kaWRhdGUucmFuZ2UpICYmIChJcy5zdHJpbmcoY2FuZGlkYXRlLnVyaSkgfHwgSXMudW5kZWZpbmVkKGNhbmRpZGF0ZS51cmkpKTtcbiAgICB9XG4gICAgTG9jYXRpb24uaXMgPSBpcztcbn0pKExvY2F0aW9uIHx8IChMb2NhdGlvbiA9IHt9KSk7XG4vKipcbiAqIFRoZSBMb2NhdGlvbkxpbmsgbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgTG9jYXRpb25MaW5rfSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBMb2NhdGlvbkxpbms7XG4oZnVuY3Rpb24gKExvY2F0aW9uTGluaykge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBMb2NhdGlvbkxpbmsgbGl0ZXJhbC5cbiAgICAgKiBAcGFyYW0gdGFyZ2V0VXJpIFRoZSBkZWZpbml0aW9uJ3MgdXJpLlxuICAgICAqIEBwYXJhbSB0YXJnZXRSYW5nZSBUaGUgZnVsbCByYW5nZSBvZiB0aGUgZGVmaW5pdGlvbi5cbiAgICAgKiBAcGFyYW0gdGFyZ2V0U2VsZWN0aW9uUmFuZ2UgVGhlIHNwYW4gb2YgdGhlIHN5bWJvbCBkZWZpbml0aW9uIGF0IHRoZSB0YXJnZXQuXG4gICAgICogQHBhcmFtIG9yaWdpblNlbGVjdGlvblJhbmdlIFRoZSBzcGFuIG9mIHRoZSBzeW1ib2wgYmVpbmcgZGVmaW5lZCBpbiB0aGUgb3JpZ2luYXRpbmcgc291cmNlIGZpbGUuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKHRhcmdldFVyaSwgdGFyZ2V0UmFuZ2UsIHRhcmdldFNlbGVjdGlvblJhbmdlLCBvcmlnaW5TZWxlY3Rpb25SYW5nZSkge1xuICAgICAgICByZXR1cm4geyB0YXJnZXRVcmksIHRhcmdldFJhbmdlLCB0YXJnZXRTZWxlY3Rpb25SYW5nZSwgb3JpZ2luU2VsZWN0aW9uUmFuZ2UgfTtcbiAgICB9XG4gICAgTG9jYXRpb25MaW5rLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIExvY2F0aW9uTGlua30gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLm9iamVjdExpdGVyYWwoY2FuZGlkYXRlKSAmJiBSYW5nZS5pcyhjYW5kaWRhdGUudGFyZ2V0UmFuZ2UpICYmIElzLnN0cmluZyhjYW5kaWRhdGUudGFyZ2V0VXJpKVxuICAgICAgICAgICAgJiYgUmFuZ2UuaXMoY2FuZGlkYXRlLnRhcmdldFNlbGVjdGlvblJhbmdlKVxuICAgICAgICAgICAgJiYgKFJhbmdlLmlzKGNhbmRpZGF0ZS5vcmlnaW5TZWxlY3Rpb25SYW5nZSkgfHwgSXMudW5kZWZpbmVkKGNhbmRpZGF0ZS5vcmlnaW5TZWxlY3Rpb25SYW5nZSkpO1xuICAgIH1cbiAgICBMb2NhdGlvbkxpbmsuaXMgPSBpcztcbn0pKExvY2F0aW9uTGluayB8fCAoTG9jYXRpb25MaW5rID0ge30pKTtcbi8qKlxuICogVGhlIENvbG9yIG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIENvbG9yfSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBDb2xvcjtcbihmdW5jdGlvbiAoQ29sb3IpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IENvbG9yIGxpdGVyYWwuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKHJlZCwgZ3JlZW4sIGJsdWUsIGFscGhhKSB7XG4gICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICByZWQsXG4gICAgICAgICAgICBncmVlbixcbiAgICAgICAgICAgIGJsdWUsXG4gICAgICAgICAgICBhbHBoYSxcbiAgICAgICAgfTtcbiAgICB9XG4gICAgQ29sb3IuY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgQ29sb3J9IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLm9iamVjdExpdGVyYWwoY2FuZGlkYXRlKSAmJiBJcy5udW1iZXJSYW5nZShjYW5kaWRhdGUucmVkLCAwLCAxKVxuICAgICAgICAgICAgJiYgSXMubnVtYmVyUmFuZ2UoY2FuZGlkYXRlLmdyZWVuLCAwLCAxKVxuICAgICAgICAgICAgJiYgSXMubnVtYmVyUmFuZ2UoY2FuZGlkYXRlLmJsdWUsIDAsIDEpXG4gICAgICAgICAgICAmJiBJcy5udW1iZXJSYW5nZShjYW5kaWRhdGUuYWxwaGEsIDAsIDEpO1xuICAgIH1cbiAgICBDb2xvci5pcyA9IGlzO1xufSkoQ29sb3IgfHwgKENvbG9yID0ge30pKTtcbi8qKlxuICogVGhlIENvbG9ySW5mb3JtYXRpb24gbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgQ29sb3JJbmZvcm1hdGlvbn0gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgQ29sb3JJbmZvcm1hdGlvbjtcbihmdW5jdGlvbiAoQ29sb3JJbmZvcm1hdGlvbikge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgQ29sb3JJbmZvcm1hdGlvbiBsaXRlcmFsLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShyYW5nZSwgY29sb3IpIHtcbiAgICAgICAgcmV0dXJuIHtcbiAgICAgICAgICAgIHJhbmdlLFxuICAgICAgICAgICAgY29sb3IsXG4gICAgICAgIH07XG4gICAgfVxuICAgIENvbG9ySW5mb3JtYXRpb24uY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgQ29sb3JJbmZvcm1hdGlvbn0gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMub2JqZWN0TGl0ZXJhbChjYW5kaWRhdGUpICYmIFJhbmdlLmlzKGNhbmRpZGF0ZS5yYW5nZSkgJiYgQ29sb3IuaXMoY2FuZGlkYXRlLmNvbG9yKTtcbiAgICB9XG4gICAgQ29sb3JJbmZvcm1hdGlvbi5pcyA9IGlzO1xufSkoQ29sb3JJbmZvcm1hdGlvbiB8fCAoQ29sb3JJbmZvcm1hdGlvbiA9IHt9KSk7XG4vKipcbiAqIFRoZSBDb2xvciBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBDb2xvclByZXNlbnRhdGlvbn0gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgQ29sb3JQcmVzZW50YXRpb247XG4oZnVuY3Rpb24gKENvbG9yUHJlc2VudGF0aW9uKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBDb2xvckluZm9ybWF0aW9uIGxpdGVyYWwuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKGxhYmVsLCB0ZXh0RWRpdCwgYWRkaXRpb25hbFRleHRFZGl0cykge1xuICAgICAgICByZXR1cm4ge1xuICAgICAgICAgICAgbGFiZWwsXG4gICAgICAgICAgICB0ZXh0RWRpdCxcbiAgICAgICAgICAgIGFkZGl0aW9uYWxUZXh0RWRpdHMsXG4gICAgICAgIH07XG4gICAgfVxuICAgIENvbG9yUHJlc2VudGF0aW9uLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIENvbG9ySW5mb3JtYXRpb259IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLm9iamVjdExpdGVyYWwoY2FuZGlkYXRlKSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLmxhYmVsKVxuICAgICAgICAgICAgJiYgKElzLnVuZGVmaW5lZChjYW5kaWRhdGUudGV4dEVkaXQpIHx8IFRleHRFZGl0LmlzKGNhbmRpZGF0ZSkpXG4gICAgICAgICAgICAmJiAoSXMudW5kZWZpbmVkKGNhbmRpZGF0ZS5hZGRpdGlvbmFsVGV4dEVkaXRzKSB8fCBJcy50eXBlZEFycmF5KGNhbmRpZGF0ZS5hZGRpdGlvbmFsVGV4dEVkaXRzLCBUZXh0RWRpdC5pcykpO1xuICAgIH1cbiAgICBDb2xvclByZXNlbnRhdGlvbi5pcyA9IGlzO1xufSkoQ29sb3JQcmVzZW50YXRpb24gfHwgKENvbG9yUHJlc2VudGF0aW9uID0ge30pKTtcbi8qKlxuICogQSBzZXQgb2YgcHJlZGVmaW5lZCByYW5nZSBraW5kcy5cbiAqL1xuZXhwb3J0IHZhciBGb2xkaW5nUmFuZ2VLaW5kO1xuKGZ1bmN0aW9uIChGb2xkaW5nUmFuZ2VLaW5kKSB7XG4gICAgLyoqXG4gICAgICogRm9sZGluZyByYW5nZSBmb3IgYSBjb21tZW50XG4gICAgICovXG4gICAgRm9sZGluZ1JhbmdlS2luZC5Db21tZW50ID0gJ2NvbW1lbnQnO1xuICAgIC8qKlxuICAgICAqIEZvbGRpbmcgcmFuZ2UgZm9yIGFuIGltcG9ydCBvciBpbmNsdWRlXG4gICAgICovXG4gICAgRm9sZGluZ1JhbmdlS2luZC5JbXBvcnRzID0gJ2ltcG9ydHMnO1xuICAgIC8qKlxuICAgICAqIEZvbGRpbmcgcmFuZ2UgZm9yIGEgcmVnaW9uIChlLmcuIGAjcmVnaW9uYClcbiAgICAgKi9cbiAgICBGb2xkaW5nUmFuZ2VLaW5kLlJlZ2lvbiA9ICdyZWdpb24nO1xufSkoRm9sZGluZ1JhbmdlS2luZCB8fCAoRm9sZGluZ1JhbmdlS2luZCA9IHt9KSk7XG4vKipcbiAqIFRoZSBmb2xkaW5nIHJhbmdlIG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIEZvbGRpbmdSYW5nZX0gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgRm9sZGluZ1JhbmdlO1xuKGZ1bmN0aW9uIChGb2xkaW5nUmFuZ2UpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IEZvbGRpbmdSYW5nZSBsaXRlcmFsLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShzdGFydExpbmUsIGVuZExpbmUsIHN0YXJ0Q2hhcmFjdGVyLCBlbmRDaGFyYWN0ZXIsIGtpbmQsIGNvbGxhcHNlZFRleHQpIHtcbiAgICAgICAgY29uc3QgcmVzdWx0ID0ge1xuICAgICAgICAgICAgc3RhcnRMaW5lLFxuICAgICAgICAgICAgZW5kTGluZVxuICAgICAgICB9O1xuICAgICAgICBpZiAoSXMuZGVmaW5lZChzdGFydENoYXJhY3RlcikpIHtcbiAgICAgICAgICAgIHJlc3VsdC5zdGFydENoYXJhY3RlciA9IHN0YXJ0Q2hhcmFjdGVyO1xuICAgICAgICB9XG4gICAgICAgIGlmIChJcy5kZWZpbmVkKGVuZENoYXJhY3RlcikpIHtcbiAgICAgICAgICAgIHJlc3VsdC5lbmRDaGFyYWN0ZXIgPSBlbmRDaGFyYWN0ZXI7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKElzLmRlZmluZWQoa2luZCkpIHtcbiAgICAgICAgICAgIHJlc3VsdC5raW5kID0ga2luZDtcbiAgICAgICAgfVxuICAgICAgICBpZiAoSXMuZGVmaW5lZChjb2xsYXBzZWRUZXh0KSkge1xuICAgICAgICAgICAgcmVzdWx0LmNvbGxhcHNlZFRleHQgPSBjb2xsYXBzZWRUZXh0O1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxuICAgIEZvbGRpbmdSYW5nZS5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBGb2xkaW5nUmFuZ2V9IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLm9iamVjdExpdGVyYWwoY2FuZGlkYXRlKSAmJiBJcy51aW50ZWdlcihjYW5kaWRhdGUuc3RhcnRMaW5lKSAmJiBJcy51aW50ZWdlcihjYW5kaWRhdGUuc3RhcnRMaW5lKVxuICAgICAgICAgICAgJiYgKElzLnVuZGVmaW5lZChjYW5kaWRhdGUuc3RhcnRDaGFyYWN0ZXIpIHx8IElzLnVpbnRlZ2VyKGNhbmRpZGF0ZS5zdGFydENoYXJhY3RlcikpXG4gICAgICAgICAgICAmJiAoSXMudW5kZWZpbmVkKGNhbmRpZGF0ZS5lbmRDaGFyYWN0ZXIpIHx8IElzLnVpbnRlZ2VyKGNhbmRpZGF0ZS5lbmRDaGFyYWN0ZXIpKVxuICAgICAgICAgICAgJiYgKElzLnVuZGVmaW5lZChjYW5kaWRhdGUua2luZCkgfHwgSXMuc3RyaW5nKGNhbmRpZGF0ZS5raW5kKSk7XG4gICAgfVxuICAgIEZvbGRpbmdSYW5nZS5pcyA9IGlzO1xufSkoRm9sZGluZ1JhbmdlIHx8IChGb2xkaW5nUmFuZ2UgPSB7fSkpO1xuLyoqXG4gKiBUaGUgRGlhZ25vc3RpY1JlbGF0ZWRJbmZvcm1hdGlvbiBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBEaWFnbm9zdGljUmVsYXRlZEluZm9ybWF0aW9ufSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBEaWFnbm9zdGljUmVsYXRlZEluZm9ybWF0aW9uO1xuKGZ1bmN0aW9uIChEaWFnbm9zdGljUmVsYXRlZEluZm9ybWF0aW9uKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBEaWFnbm9zdGljUmVsYXRlZEluZm9ybWF0aW9uIGxpdGVyYWwuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKGxvY2F0aW9uLCBtZXNzYWdlKSB7XG4gICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICBsb2NhdGlvbixcbiAgICAgICAgICAgIG1lc3NhZ2VcbiAgICAgICAgfTtcbiAgICB9XG4gICAgRGlhZ25vc3RpY1JlbGF0ZWRJbmZvcm1hdGlvbi5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBEaWFnbm9zdGljUmVsYXRlZEluZm9ybWF0aW9ufSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMuZGVmaW5lZChjYW5kaWRhdGUpICYmIExvY2F0aW9uLmlzKGNhbmRpZGF0ZS5sb2NhdGlvbikgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS5tZXNzYWdlKTtcbiAgICB9XG4gICAgRGlhZ25vc3RpY1JlbGF0ZWRJbmZvcm1hdGlvbi5pcyA9IGlzO1xufSkoRGlhZ25vc3RpY1JlbGF0ZWRJbmZvcm1hdGlvbiB8fCAoRGlhZ25vc3RpY1JlbGF0ZWRJbmZvcm1hdGlvbiA9IHt9KSk7XG4vKipcbiAqIFRoZSBkaWFnbm9zdGljJ3Mgc2V2ZXJpdHkuXG4gKi9cbmV4cG9ydCB2YXIgRGlhZ25vc3RpY1NldmVyaXR5O1xuKGZ1bmN0aW9uIChEaWFnbm9zdGljU2V2ZXJpdHkpIHtcbiAgICAvKipcbiAgICAgKiBSZXBvcnRzIGFuIGVycm9yLlxuICAgICAqL1xuICAgIERpYWdub3N0aWNTZXZlcml0eS5FcnJvciA9IDE7XG4gICAgLyoqXG4gICAgICogUmVwb3J0cyBhIHdhcm5pbmcuXG4gICAgICovXG4gICAgRGlhZ25vc3RpY1NldmVyaXR5Lldhcm5pbmcgPSAyO1xuICAgIC8qKlxuICAgICAqIFJlcG9ydHMgYW4gaW5mb3JtYXRpb24uXG4gICAgICovXG4gICAgRGlhZ25vc3RpY1NldmVyaXR5LkluZm9ybWF0aW9uID0gMztcbiAgICAvKipcbiAgICAgKiBSZXBvcnRzIGEgaGludC5cbiAgICAgKi9cbiAgICBEaWFnbm9zdGljU2V2ZXJpdHkuSGludCA9IDQ7XG59KShEaWFnbm9zdGljU2V2ZXJpdHkgfHwgKERpYWdub3N0aWNTZXZlcml0eSA9IHt9KSk7XG4vKipcbiAqIFRoZSBkaWFnbm9zdGljIHRhZ3MuXG4gKlxuICogQHNpbmNlIDMuMTUuMFxuICovXG5leHBvcnQgdmFyIERpYWdub3N0aWNUYWc7XG4oZnVuY3Rpb24gKERpYWdub3N0aWNUYWcpIHtcbiAgICAvKipcbiAgICAgKiBVbnVzZWQgb3IgdW5uZWNlc3NhcnkgY29kZS5cbiAgICAgKlxuICAgICAqIENsaWVudHMgYXJlIGFsbG93ZWQgdG8gcmVuZGVyIGRpYWdub3N0aWNzIHdpdGggdGhpcyB0YWcgZmFkZWQgb3V0IGluc3RlYWQgb2YgaGF2aW5nXG4gICAgICogYW4gZXJyb3Igc3F1aWdnbGUuXG4gICAgICovXG4gICAgRGlhZ25vc3RpY1RhZy5Vbm5lY2Vzc2FyeSA9IDE7XG4gICAgLyoqXG4gICAgICogRGVwcmVjYXRlZCBvciBvYnNvbGV0ZSBjb2RlLlxuICAgICAqXG4gICAgICogQ2xpZW50cyBhcmUgYWxsb3dlZCB0byByZW5kZXJlZCBkaWFnbm9zdGljcyB3aXRoIHRoaXMgdGFnIHN0cmlrZSB0aHJvdWdoLlxuICAgICAqL1xuICAgIERpYWdub3N0aWNUYWcuRGVwcmVjYXRlZCA9IDI7XG59KShEaWFnbm9zdGljVGFnIHx8IChEaWFnbm9zdGljVGFnID0ge30pKTtcbi8qKlxuICogVGhlIENvZGVEZXNjcmlwdGlvbiBuYW1lc3BhY2UgcHJvdmlkZXMgZnVuY3Rpb25zIHRvIGRlYWwgd2l0aCBkZXNjcmlwdGlvbnMgZm9yIGRpYWdub3N0aWMgY29kZXMuXG4gKlxuICogQHNpbmNlIDMuMTYuMFxuICovXG5leHBvcnQgdmFyIENvZGVEZXNjcmlwdGlvbjtcbihmdW5jdGlvbiAoQ29kZURlc2NyaXB0aW9uKSB7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5vYmplY3RMaXRlcmFsKGNhbmRpZGF0ZSkgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS5ocmVmKTtcbiAgICB9XG4gICAgQ29kZURlc2NyaXB0aW9uLmlzID0gaXM7XG59KShDb2RlRGVzY3JpcHRpb24gfHwgKENvZGVEZXNjcmlwdGlvbiA9IHt9KSk7XG4vKipcbiAqIFRoZSBEaWFnbm9zdGljIG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIERpYWdub3N0aWN9IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIERpYWdub3N0aWM7XG4oZnVuY3Rpb24gKERpYWdub3N0aWMpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IERpYWdub3N0aWMgbGl0ZXJhbC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUocmFuZ2UsIG1lc3NhZ2UsIHNldmVyaXR5LCBjb2RlLCBzb3VyY2UsIHJlbGF0ZWRJbmZvcm1hdGlvbikge1xuICAgICAgICBsZXQgcmVzdWx0ID0geyByYW5nZSwgbWVzc2FnZSB9O1xuICAgICAgICBpZiAoSXMuZGVmaW5lZChzZXZlcml0eSkpIHtcbiAgICAgICAgICAgIHJlc3VsdC5zZXZlcml0eSA9IHNldmVyaXR5O1xuICAgICAgICB9XG4gICAgICAgIGlmIChJcy5kZWZpbmVkKGNvZGUpKSB7XG4gICAgICAgICAgICByZXN1bHQuY29kZSA9IGNvZGU7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKElzLmRlZmluZWQoc291cmNlKSkge1xuICAgICAgICAgICAgcmVzdWx0LnNvdXJjZSA9IHNvdXJjZTtcbiAgICAgICAgfVxuICAgICAgICBpZiAoSXMuZGVmaW5lZChyZWxhdGVkSW5mb3JtYXRpb24pKSB7XG4gICAgICAgICAgICByZXN1bHQucmVsYXRlZEluZm9ybWF0aW9uID0gcmVsYXRlZEluZm9ybWF0aW9uO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxuICAgIERpYWdub3N0aWMuY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgRGlhZ25vc3RpY30gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIHZhciBfYTtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMuZGVmaW5lZChjYW5kaWRhdGUpXG4gICAgICAgICAgICAmJiBSYW5nZS5pcyhjYW5kaWRhdGUucmFuZ2UpXG4gICAgICAgICAgICAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLm1lc3NhZ2UpXG4gICAgICAgICAgICAmJiAoSXMubnVtYmVyKGNhbmRpZGF0ZS5zZXZlcml0eSkgfHwgSXMudW5kZWZpbmVkKGNhbmRpZGF0ZS5zZXZlcml0eSkpXG4gICAgICAgICAgICAmJiAoSXMuaW50ZWdlcihjYW5kaWRhdGUuY29kZSkgfHwgSXMuc3RyaW5nKGNhbmRpZGF0ZS5jb2RlKSB8fCBJcy51bmRlZmluZWQoY2FuZGlkYXRlLmNvZGUpKVxuICAgICAgICAgICAgJiYgKElzLnVuZGVmaW5lZChjYW5kaWRhdGUuY29kZURlc2NyaXB0aW9uKSB8fCAoSXMuc3RyaW5nKChfYSA9IGNhbmRpZGF0ZS5jb2RlRGVzY3JpcHRpb24pID09PSBudWxsIHx8IF9hID09PSB2b2lkIDAgPyB2b2lkIDAgOiBfYS5ocmVmKSkpXG4gICAgICAgICAgICAmJiAoSXMuc3RyaW5nKGNhbmRpZGF0ZS5zb3VyY2UpIHx8IElzLnVuZGVmaW5lZChjYW5kaWRhdGUuc291cmNlKSlcbiAgICAgICAgICAgICYmIChJcy51bmRlZmluZWQoY2FuZGlkYXRlLnJlbGF0ZWRJbmZvcm1hdGlvbikgfHwgSXMudHlwZWRBcnJheShjYW5kaWRhdGUucmVsYXRlZEluZm9ybWF0aW9uLCBEaWFnbm9zdGljUmVsYXRlZEluZm9ybWF0aW9uLmlzKSk7XG4gICAgfVxuICAgIERpYWdub3N0aWMuaXMgPSBpcztcbn0pKERpYWdub3N0aWMgfHwgKERpYWdub3N0aWMgPSB7fSkpO1xuLyoqXG4gKiBUaGUgQ29tbWFuZCBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBDb21tYW5kfSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBDb21tYW5kO1xuKGZ1bmN0aW9uIChDb21tYW5kKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBDb21tYW5kIGxpdGVyYWwuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKHRpdGxlLCBjb21tYW5kLCAuLi5hcmdzKSB7XG4gICAgICAgIGxldCByZXN1bHQgPSB7IHRpdGxlLCBjb21tYW5kIH07XG4gICAgICAgIGlmIChJcy5kZWZpbmVkKGFyZ3MpICYmIGFyZ3MubGVuZ3RoID4gMCkge1xuICAgICAgICAgICAgcmVzdWx0LmFyZ3VtZW50cyA9IGFyZ3M7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICB9XG4gICAgQ29tbWFuZC5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBDb21tYW5kfSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMuZGVmaW5lZChjYW5kaWRhdGUpICYmIElzLnN0cmluZyhjYW5kaWRhdGUudGl0bGUpICYmIElzLnN0cmluZyhjYW5kaWRhdGUuY29tbWFuZCk7XG4gICAgfVxuICAgIENvbW1hbmQuaXMgPSBpcztcbn0pKENvbW1hbmQgfHwgKENvbW1hbmQgPSB7fSkpO1xuLyoqXG4gKiBUaGUgVGV4dEVkaXQgbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbiB0byBjcmVhdGUgcmVwbGFjZSxcbiAqIGluc2VydCBhbmQgZGVsZXRlIGVkaXRzIG1vcmUgZWFzaWx5LlxuICovXG5leHBvcnQgdmFyIFRleHRFZGl0O1xuKGZ1bmN0aW9uIChUZXh0RWRpdCkge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSByZXBsYWNlIHRleHQgZWRpdC5cbiAgICAgKiBAcGFyYW0gcmFuZ2UgVGhlIHJhbmdlIG9mIHRleHQgdG8gYmUgcmVwbGFjZWQuXG4gICAgICogQHBhcmFtIG5ld1RleHQgVGhlIG5ldyB0ZXh0LlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIHJlcGxhY2UocmFuZ2UsIG5ld1RleHQpIHtcbiAgICAgICAgcmV0dXJuIHsgcmFuZ2UsIG5ld1RleHQgfTtcbiAgICB9XG4gICAgVGV4dEVkaXQucmVwbGFjZSA9IHJlcGxhY2U7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhbiBpbnNlcnQgdGV4dCBlZGl0LlxuICAgICAqIEBwYXJhbSBwb3NpdGlvbiBUaGUgcG9zaXRpb24gdG8gaW5zZXJ0IHRoZSB0ZXh0IGF0LlxuICAgICAqIEBwYXJhbSBuZXdUZXh0IFRoZSB0ZXh0IHRvIGJlIGluc2VydGVkLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGluc2VydChwb3NpdGlvbiwgbmV3VGV4dCkge1xuICAgICAgICByZXR1cm4geyByYW5nZTogeyBzdGFydDogcG9zaXRpb24sIGVuZDogcG9zaXRpb24gfSwgbmV3VGV4dCB9O1xuICAgIH1cbiAgICBUZXh0RWRpdC5pbnNlcnQgPSBpbnNlcnQ7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIGRlbGV0ZSB0ZXh0IGVkaXQuXG4gICAgICogQHBhcmFtIHJhbmdlIFRoZSByYW5nZSBvZiB0ZXh0IHRvIGJlIGRlbGV0ZWQuXG4gICAgICovXG4gICAgZnVuY3Rpb24gZGVsKHJhbmdlKSB7XG4gICAgICAgIHJldHVybiB7IHJhbmdlLCBuZXdUZXh0OiAnJyB9O1xuICAgIH1cbiAgICBUZXh0RWRpdC5kZWwgPSBkZWw7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5vYmplY3RMaXRlcmFsKGNhbmRpZGF0ZSlcbiAgICAgICAgICAgICYmIElzLnN0cmluZyhjYW5kaWRhdGUubmV3VGV4dClcbiAgICAgICAgICAgICYmIFJhbmdlLmlzKGNhbmRpZGF0ZS5yYW5nZSk7XG4gICAgfVxuICAgIFRleHRFZGl0LmlzID0gaXM7XG59KShUZXh0RWRpdCB8fCAoVGV4dEVkaXQgPSB7fSkpO1xuZXhwb3J0IHZhciBDaGFuZ2VBbm5vdGF0aW9uO1xuKGZ1bmN0aW9uIChDaGFuZ2VBbm5vdGF0aW9uKSB7XG4gICAgZnVuY3Rpb24gY3JlYXRlKGxhYmVsLCBuZWVkc0NvbmZpcm1hdGlvbiwgZGVzY3JpcHRpb24pIHtcbiAgICAgICAgY29uc3QgcmVzdWx0ID0geyBsYWJlbCB9O1xuICAgICAgICBpZiAobmVlZHNDb25maXJtYXRpb24gIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmVzdWx0Lm5lZWRzQ29uZmlybWF0aW9uID0gbmVlZHNDb25maXJtYXRpb247XG4gICAgICAgIH1cbiAgICAgICAgaWYgKGRlc2NyaXB0aW9uICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHJlc3VsdC5kZXNjcmlwdGlvbiA9IGRlc2NyaXB0aW9uO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxuICAgIENoYW5nZUFubm90YXRpb24uY3JlYXRlID0gY3JlYXRlO1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMub2JqZWN0TGl0ZXJhbChjYW5kaWRhdGUpICYmIElzLnN0cmluZyhjYW5kaWRhdGUubGFiZWwpICYmXG4gICAgICAgICAgICAoSXMuYm9vbGVhbihjYW5kaWRhdGUubmVlZHNDb25maXJtYXRpb24pIHx8IGNhbmRpZGF0ZS5uZWVkc0NvbmZpcm1hdGlvbiA9PT0gdW5kZWZpbmVkKSAmJlxuICAgICAgICAgICAgKElzLnN0cmluZyhjYW5kaWRhdGUuZGVzY3JpcHRpb24pIHx8IGNhbmRpZGF0ZS5kZXNjcmlwdGlvbiA9PT0gdW5kZWZpbmVkKTtcbiAgICB9XG4gICAgQ2hhbmdlQW5ub3RhdGlvbi5pcyA9IGlzO1xufSkoQ2hhbmdlQW5ub3RhdGlvbiB8fCAoQ2hhbmdlQW5ub3RhdGlvbiA9IHt9KSk7XG5leHBvcnQgdmFyIENoYW5nZUFubm90YXRpb25JZGVudGlmaWVyO1xuKGZ1bmN0aW9uIChDaGFuZ2VBbm5vdGF0aW9uSWRlbnRpZmllcikge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMuc3RyaW5nKGNhbmRpZGF0ZSk7XG4gICAgfVxuICAgIENoYW5nZUFubm90YXRpb25JZGVudGlmaWVyLmlzID0gaXM7XG59KShDaGFuZ2VBbm5vdGF0aW9uSWRlbnRpZmllciB8fCAoQ2hhbmdlQW5ub3RhdGlvbklkZW50aWZpZXIgPSB7fSkpO1xuZXhwb3J0IHZhciBBbm5vdGF0ZWRUZXh0RWRpdDtcbihmdW5jdGlvbiAoQW5ub3RhdGVkVGV4dEVkaXQpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGFuIGFubm90YXRlZCByZXBsYWNlIHRleHQgZWRpdC5cbiAgICAgKlxuICAgICAqIEBwYXJhbSByYW5nZSBUaGUgcmFuZ2Ugb2YgdGV4dCB0byBiZSByZXBsYWNlZC5cbiAgICAgKiBAcGFyYW0gbmV3VGV4dCBUaGUgbmV3IHRleHQuXG4gICAgICogQHBhcmFtIGFubm90YXRpb24gVGhlIGFubm90YXRpb24uXG4gICAgICovXG4gICAgZnVuY3Rpb24gcmVwbGFjZShyYW5nZSwgbmV3VGV4dCwgYW5ub3RhdGlvbikge1xuICAgICAgICByZXR1cm4geyByYW5nZSwgbmV3VGV4dCwgYW5ub3RhdGlvbklkOiBhbm5vdGF0aW9uIH07XG4gICAgfVxuICAgIEFubm90YXRlZFRleHRFZGl0LnJlcGxhY2UgPSByZXBsYWNlO1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYW4gYW5ub3RhdGVkIGluc2VydCB0ZXh0IGVkaXQuXG4gICAgICpcbiAgICAgKiBAcGFyYW0gcG9zaXRpb24gVGhlIHBvc2l0aW9uIHRvIGluc2VydCB0aGUgdGV4dCBhdC5cbiAgICAgKiBAcGFyYW0gbmV3VGV4dCBUaGUgdGV4dCB0byBiZSBpbnNlcnRlZC5cbiAgICAgKiBAcGFyYW0gYW5ub3RhdGlvbiBUaGUgYW5ub3RhdGlvbi5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpbnNlcnQocG9zaXRpb24sIG5ld1RleHQsIGFubm90YXRpb24pIHtcbiAgICAgICAgcmV0dXJuIHsgcmFuZ2U6IHsgc3RhcnQ6IHBvc2l0aW9uLCBlbmQ6IHBvc2l0aW9uIH0sIG5ld1RleHQsIGFubm90YXRpb25JZDogYW5ub3RhdGlvbiB9O1xuICAgIH1cbiAgICBBbm5vdGF0ZWRUZXh0RWRpdC5pbnNlcnQgPSBpbnNlcnQ7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhbiBhbm5vdGF0ZWQgZGVsZXRlIHRleHQgZWRpdC5cbiAgICAgKlxuICAgICAqIEBwYXJhbSByYW5nZSBUaGUgcmFuZ2Ugb2YgdGV4dCB0byBiZSBkZWxldGVkLlxuICAgICAqIEBwYXJhbSBhbm5vdGF0aW9uIFRoZSBhbm5vdGF0aW9uLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGRlbChyYW5nZSwgYW5ub3RhdGlvbikge1xuICAgICAgICByZXR1cm4geyByYW5nZSwgbmV3VGV4dDogJycsIGFubm90YXRpb25JZDogYW5ub3RhdGlvbiB9O1xuICAgIH1cbiAgICBBbm5vdGF0ZWRUZXh0RWRpdC5kZWwgPSBkZWw7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBUZXh0RWRpdC5pcyhjYW5kaWRhdGUpICYmIChDaGFuZ2VBbm5vdGF0aW9uLmlzKGNhbmRpZGF0ZS5hbm5vdGF0aW9uSWQpIHx8IENoYW5nZUFubm90YXRpb25JZGVudGlmaWVyLmlzKGNhbmRpZGF0ZS5hbm5vdGF0aW9uSWQpKTtcbiAgICB9XG4gICAgQW5ub3RhdGVkVGV4dEVkaXQuaXMgPSBpcztcbn0pKEFubm90YXRlZFRleHRFZGl0IHx8IChBbm5vdGF0ZWRUZXh0RWRpdCA9IHt9KSk7XG4vKipcbiAqIFRoZSBUZXh0RG9jdW1lbnRFZGl0IG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb24gdG8gY3JlYXRlXG4gKiBhbiBlZGl0IHRoYXQgbWFuaXB1bGF0ZXMgYSB0ZXh0IGRvY3VtZW50LlxuICovXG5leHBvcnQgdmFyIFRleHREb2N1bWVudEVkaXQ7XG4oZnVuY3Rpb24gKFRleHREb2N1bWVudEVkaXQpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IGBUZXh0RG9jdW1lbnRFZGl0YFxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZSh0ZXh0RG9jdW1lbnQsIGVkaXRzKSB7XG4gICAgICAgIHJldHVybiB7IHRleHREb2N1bWVudCwgZWRpdHMgfTtcbiAgICB9XG4gICAgVGV4dERvY3VtZW50RWRpdC5jcmVhdGUgPSBjcmVhdGU7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMuZGVmaW5lZChjYW5kaWRhdGUpXG4gICAgICAgICAgICAmJiBPcHRpb25hbFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXIuaXMoY2FuZGlkYXRlLnRleHREb2N1bWVudClcbiAgICAgICAgICAgICYmIEFycmF5LmlzQXJyYXkoY2FuZGlkYXRlLmVkaXRzKTtcbiAgICB9XG4gICAgVGV4dERvY3VtZW50RWRpdC5pcyA9IGlzO1xufSkoVGV4dERvY3VtZW50RWRpdCB8fCAoVGV4dERvY3VtZW50RWRpdCA9IHt9KSk7XG5leHBvcnQgdmFyIENyZWF0ZUZpbGU7XG4oZnVuY3Rpb24gKENyZWF0ZUZpbGUpIHtcbiAgICBmdW5jdGlvbiBjcmVhdGUodXJpLCBvcHRpb25zLCBhbm5vdGF0aW9uKSB7XG4gICAgICAgIGxldCByZXN1bHQgPSB7XG4gICAgICAgICAgICBraW5kOiAnY3JlYXRlJyxcbiAgICAgICAgICAgIHVyaVxuICAgICAgICB9O1xuICAgICAgICBpZiAob3B0aW9ucyAhPT0gdW5kZWZpbmVkICYmIChvcHRpb25zLm92ZXJ3cml0ZSAhPT0gdW5kZWZpbmVkIHx8IG9wdGlvbnMuaWdub3JlSWZFeGlzdHMgIT09IHVuZGVmaW5lZCkpIHtcbiAgICAgICAgICAgIHJlc3VsdC5vcHRpb25zID0gb3B0aW9ucztcbiAgICAgICAgfVxuICAgICAgICBpZiAoYW5ub3RhdGlvbiAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXN1bHQuYW5ub3RhdGlvbklkID0gYW5ub3RhdGlvbjtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cbiAgICBDcmVhdGVGaWxlLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBjYW5kaWRhdGUgJiYgY2FuZGlkYXRlLmtpbmQgPT09ICdjcmVhdGUnICYmIElzLnN0cmluZyhjYW5kaWRhdGUudXJpKSAmJiAoY2FuZGlkYXRlLm9wdGlvbnMgPT09IHVuZGVmaW5lZCB8fFxuICAgICAgICAgICAgKChjYW5kaWRhdGUub3B0aW9ucy5vdmVyd3JpdGUgPT09IHVuZGVmaW5lZCB8fCBJcy5ib29sZWFuKGNhbmRpZGF0ZS5vcHRpb25zLm92ZXJ3cml0ZSkpICYmIChjYW5kaWRhdGUub3B0aW9ucy5pZ25vcmVJZkV4aXN0cyA9PT0gdW5kZWZpbmVkIHx8IElzLmJvb2xlYW4oY2FuZGlkYXRlLm9wdGlvbnMuaWdub3JlSWZFeGlzdHMpKSkpICYmIChjYW5kaWRhdGUuYW5ub3RhdGlvbklkID09PSB1bmRlZmluZWQgfHwgQ2hhbmdlQW5ub3RhdGlvbklkZW50aWZpZXIuaXMoY2FuZGlkYXRlLmFubm90YXRpb25JZCkpO1xuICAgIH1cbiAgICBDcmVhdGVGaWxlLmlzID0gaXM7XG59KShDcmVhdGVGaWxlIHx8IChDcmVhdGVGaWxlID0ge30pKTtcbmV4cG9ydCB2YXIgUmVuYW1lRmlsZTtcbihmdW5jdGlvbiAoUmVuYW1lRmlsZSkge1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShvbGRVcmksIG5ld1VyaSwgb3B0aW9ucywgYW5ub3RhdGlvbikge1xuICAgICAgICBsZXQgcmVzdWx0ID0ge1xuICAgICAgICAgICAga2luZDogJ3JlbmFtZScsXG4gICAgICAgICAgICBvbGRVcmksXG4gICAgICAgICAgICBuZXdVcmlcbiAgICAgICAgfTtcbiAgICAgICAgaWYgKG9wdGlvbnMgIT09IHVuZGVmaW5lZCAmJiAob3B0aW9ucy5vdmVyd3JpdGUgIT09IHVuZGVmaW5lZCB8fCBvcHRpb25zLmlnbm9yZUlmRXhpc3RzICE9PSB1bmRlZmluZWQpKSB7XG4gICAgICAgICAgICByZXN1bHQub3B0aW9ucyA9IG9wdGlvbnM7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKGFubm90YXRpb24gIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmVzdWx0LmFubm90YXRpb25JZCA9IGFubm90YXRpb247XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICB9XG4gICAgUmVuYW1lRmlsZS5jcmVhdGUgPSBjcmVhdGU7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICYmIGNhbmRpZGF0ZS5raW5kID09PSAncmVuYW1lJyAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLm9sZFVyaSkgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS5uZXdVcmkpICYmIChjYW5kaWRhdGUub3B0aW9ucyA9PT0gdW5kZWZpbmVkIHx8XG4gICAgICAgICAgICAoKGNhbmRpZGF0ZS5vcHRpb25zLm92ZXJ3cml0ZSA9PT0gdW5kZWZpbmVkIHx8IElzLmJvb2xlYW4oY2FuZGlkYXRlLm9wdGlvbnMub3ZlcndyaXRlKSkgJiYgKGNhbmRpZGF0ZS5vcHRpb25zLmlnbm9yZUlmRXhpc3RzID09PSB1bmRlZmluZWQgfHwgSXMuYm9vbGVhbihjYW5kaWRhdGUub3B0aW9ucy5pZ25vcmVJZkV4aXN0cykpKSkgJiYgKGNhbmRpZGF0ZS5hbm5vdGF0aW9uSWQgPT09IHVuZGVmaW5lZCB8fCBDaGFuZ2VBbm5vdGF0aW9uSWRlbnRpZmllci5pcyhjYW5kaWRhdGUuYW5ub3RhdGlvbklkKSk7XG4gICAgfVxuICAgIFJlbmFtZUZpbGUuaXMgPSBpcztcbn0pKFJlbmFtZUZpbGUgfHwgKFJlbmFtZUZpbGUgPSB7fSkpO1xuZXhwb3J0IHZhciBEZWxldGVGaWxlO1xuKGZ1bmN0aW9uIChEZWxldGVGaWxlKSB7XG4gICAgZnVuY3Rpb24gY3JlYXRlKHVyaSwgb3B0aW9ucywgYW5ub3RhdGlvbikge1xuICAgICAgICBsZXQgcmVzdWx0ID0ge1xuICAgICAgICAgICAga2luZDogJ2RlbGV0ZScsXG4gICAgICAgICAgICB1cmlcbiAgICAgICAgfTtcbiAgICAgICAgaWYgKG9wdGlvbnMgIT09IHVuZGVmaW5lZCAmJiAob3B0aW9ucy5yZWN1cnNpdmUgIT09IHVuZGVmaW5lZCB8fCBvcHRpb25zLmlnbm9yZUlmTm90RXhpc3RzICE9PSB1bmRlZmluZWQpKSB7XG4gICAgICAgICAgICByZXN1bHQub3B0aW9ucyA9IG9wdGlvbnM7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKGFubm90YXRpb24gIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmVzdWx0LmFubm90YXRpb25JZCA9IGFubm90YXRpb247XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICB9XG4gICAgRGVsZXRlRmlsZS5jcmVhdGUgPSBjcmVhdGU7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICYmIGNhbmRpZGF0ZS5raW5kID09PSAnZGVsZXRlJyAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLnVyaSkgJiYgKGNhbmRpZGF0ZS5vcHRpb25zID09PSB1bmRlZmluZWQgfHxcbiAgICAgICAgICAgICgoY2FuZGlkYXRlLm9wdGlvbnMucmVjdXJzaXZlID09PSB1bmRlZmluZWQgfHwgSXMuYm9vbGVhbihjYW5kaWRhdGUub3B0aW9ucy5yZWN1cnNpdmUpKSAmJiAoY2FuZGlkYXRlLm9wdGlvbnMuaWdub3JlSWZOb3RFeGlzdHMgPT09IHVuZGVmaW5lZCB8fCBJcy5ib29sZWFuKGNhbmRpZGF0ZS5vcHRpb25zLmlnbm9yZUlmTm90RXhpc3RzKSkpKSAmJiAoY2FuZGlkYXRlLmFubm90YXRpb25JZCA9PT0gdW5kZWZpbmVkIHx8IENoYW5nZUFubm90YXRpb25JZGVudGlmaWVyLmlzKGNhbmRpZGF0ZS5hbm5vdGF0aW9uSWQpKTtcbiAgICB9XG4gICAgRGVsZXRlRmlsZS5pcyA9IGlzO1xufSkoRGVsZXRlRmlsZSB8fCAoRGVsZXRlRmlsZSA9IHt9KSk7XG5leHBvcnQgdmFyIFdvcmtzcGFjZUVkaXQ7XG4oZnVuY3Rpb24gKFdvcmtzcGFjZUVkaXQpIHtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBjYW5kaWRhdGUgJiZcbiAgICAgICAgICAgIChjYW5kaWRhdGUuY2hhbmdlcyAhPT0gdW5kZWZpbmVkIHx8IGNhbmRpZGF0ZS5kb2N1bWVudENoYW5nZXMgIT09IHVuZGVmaW5lZCkgJiZcbiAgICAgICAgICAgIChjYW5kaWRhdGUuZG9jdW1lbnRDaGFuZ2VzID09PSB1bmRlZmluZWQgfHwgY2FuZGlkYXRlLmRvY3VtZW50Q2hhbmdlcy5ldmVyeSgoY2hhbmdlKSA9PiB7XG4gICAgICAgICAgICAgICAgaWYgKElzLnN0cmluZyhjaGFuZ2Uua2luZCkpIHtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIENyZWF0ZUZpbGUuaXMoY2hhbmdlKSB8fCBSZW5hbWVGaWxlLmlzKGNoYW5nZSkgfHwgRGVsZXRlRmlsZS5pcyhjaGFuZ2UpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIFRleHREb2N1bWVudEVkaXQuaXMoY2hhbmdlKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9KSk7XG4gICAgfVxuICAgIFdvcmtzcGFjZUVkaXQuaXMgPSBpcztcbn0pKFdvcmtzcGFjZUVkaXQgfHwgKFdvcmtzcGFjZUVkaXQgPSB7fSkpO1xuY2xhc3MgVGV4dEVkaXRDaGFuZ2VJbXBsIHtcbiAgICBjb25zdHJ1Y3RvcihlZGl0cywgY2hhbmdlQW5ub3RhdGlvbnMpIHtcbiAgICAgICAgdGhpcy5lZGl0cyA9IGVkaXRzO1xuICAgICAgICB0aGlzLmNoYW5nZUFubm90YXRpb25zID0gY2hhbmdlQW5ub3RhdGlvbnM7XG4gICAgfVxuICAgIGluc2VydChwb3NpdGlvbiwgbmV3VGV4dCwgYW5ub3RhdGlvbikge1xuICAgICAgICBsZXQgZWRpdDtcbiAgICAgICAgbGV0IGlkO1xuICAgICAgICBpZiAoYW5ub3RhdGlvbiA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICBlZGl0ID0gVGV4dEVkaXQuaW5zZXJ0KHBvc2l0aW9uLCBuZXdUZXh0KTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIGlmIChDaGFuZ2VBbm5vdGF0aW9uSWRlbnRpZmllci5pcyhhbm5vdGF0aW9uKSkge1xuICAgICAgICAgICAgaWQgPSBhbm5vdGF0aW9uO1xuICAgICAgICAgICAgZWRpdCA9IEFubm90YXRlZFRleHRFZGl0Lmluc2VydChwb3NpdGlvbiwgbmV3VGV4dCwgYW5ub3RhdGlvbik7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICB0aGlzLmFzc2VydENoYW5nZUFubm90YXRpb25zKHRoaXMuY2hhbmdlQW5ub3RhdGlvbnMpO1xuICAgICAgICAgICAgaWQgPSB0aGlzLmNoYW5nZUFubm90YXRpb25zLm1hbmFnZShhbm5vdGF0aW9uKTtcbiAgICAgICAgICAgIGVkaXQgPSBBbm5vdGF0ZWRUZXh0RWRpdC5pbnNlcnQocG9zaXRpb24sIG5ld1RleHQsIGlkKTtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLmVkaXRzLnB1c2goZWRpdCk7XG4gICAgICAgIGlmIChpZCAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXR1cm4gaWQ7XG4gICAgICAgIH1cbiAgICB9XG4gICAgcmVwbGFjZShyYW5nZSwgbmV3VGV4dCwgYW5ub3RhdGlvbikge1xuICAgICAgICBsZXQgZWRpdDtcbiAgICAgICAgbGV0IGlkO1xuICAgICAgICBpZiAoYW5ub3RhdGlvbiA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICBlZGl0ID0gVGV4dEVkaXQucmVwbGFjZShyYW5nZSwgbmV3VGV4dCk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSBpZiAoQ2hhbmdlQW5ub3RhdGlvbklkZW50aWZpZXIuaXMoYW5ub3RhdGlvbikpIHtcbiAgICAgICAgICAgIGlkID0gYW5ub3RhdGlvbjtcbiAgICAgICAgICAgIGVkaXQgPSBBbm5vdGF0ZWRUZXh0RWRpdC5yZXBsYWNlKHJhbmdlLCBuZXdUZXh0LCBhbm5vdGF0aW9uKTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHRoaXMuYXNzZXJ0Q2hhbmdlQW5ub3RhdGlvbnModGhpcy5jaGFuZ2VBbm5vdGF0aW9ucyk7XG4gICAgICAgICAgICBpZCA9IHRoaXMuY2hhbmdlQW5ub3RhdGlvbnMubWFuYWdlKGFubm90YXRpb24pO1xuICAgICAgICAgICAgZWRpdCA9IEFubm90YXRlZFRleHRFZGl0LnJlcGxhY2UocmFuZ2UsIG5ld1RleHQsIGlkKTtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLmVkaXRzLnB1c2goZWRpdCk7XG4gICAgICAgIGlmIChpZCAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXR1cm4gaWQ7XG4gICAgICAgIH1cbiAgICB9XG4gICAgZGVsZXRlKHJhbmdlLCBhbm5vdGF0aW9uKSB7XG4gICAgICAgIGxldCBlZGl0O1xuICAgICAgICBsZXQgaWQ7XG4gICAgICAgIGlmIChhbm5vdGF0aW9uID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIGVkaXQgPSBUZXh0RWRpdC5kZWwocmFuZ2UpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2UgaWYgKENoYW5nZUFubm90YXRpb25JZGVudGlmaWVyLmlzKGFubm90YXRpb24pKSB7XG4gICAgICAgICAgICBpZCA9IGFubm90YXRpb247XG4gICAgICAgICAgICBlZGl0ID0gQW5ub3RhdGVkVGV4dEVkaXQuZGVsKHJhbmdlLCBhbm5vdGF0aW9uKTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHRoaXMuYXNzZXJ0Q2hhbmdlQW5ub3RhdGlvbnModGhpcy5jaGFuZ2VBbm5vdGF0aW9ucyk7XG4gICAgICAgICAgICBpZCA9IHRoaXMuY2hhbmdlQW5ub3RhdGlvbnMubWFuYWdlKGFubm90YXRpb24pO1xuICAgICAgICAgICAgZWRpdCA9IEFubm90YXRlZFRleHRFZGl0LmRlbChyYW5nZSwgaWQpO1xuICAgICAgICB9XG4gICAgICAgIHRoaXMuZWRpdHMucHVzaChlZGl0KTtcbiAgICAgICAgaWYgKGlkICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHJldHVybiBpZDtcbiAgICAgICAgfVxuICAgIH1cbiAgICBhZGQoZWRpdCkge1xuICAgICAgICB0aGlzLmVkaXRzLnB1c2goZWRpdCk7XG4gICAgfVxuICAgIGFsbCgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuZWRpdHM7XG4gICAgfVxuICAgIGNsZWFyKCkge1xuICAgICAgICB0aGlzLmVkaXRzLnNwbGljZSgwLCB0aGlzLmVkaXRzLmxlbmd0aCk7XG4gICAgfVxuICAgIGFzc2VydENoYW5nZUFubm90YXRpb25zKHZhbHVlKSB7XG4gICAgICAgIGlmICh2YWx1ZSA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYFRleHQgZWRpdCBjaGFuZ2UgaXMgbm90IGNvbmZpZ3VyZWQgdG8gbWFuYWdlIGNoYW5nZSBhbm5vdGF0aW9ucy5gKTtcbiAgICAgICAgfVxuICAgIH1cbn1cbi8qKlxuICogQSBoZWxwZXIgY2xhc3NcbiAqL1xuY2xhc3MgQ2hhbmdlQW5ub3RhdGlvbnMge1xuICAgIGNvbnN0cnVjdG9yKGFubm90YXRpb25zKSB7XG4gICAgICAgIHRoaXMuX2Fubm90YXRpb25zID0gYW5ub3RhdGlvbnMgPT09IHVuZGVmaW5lZCA/IE9iamVjdC5jcmVhdGUobnVsbCkgOiBhbm5vdGF0aW9ucztcbiAgICAgICAgdGhpcy5fY291bnRlciA9IDA7XG4gICAgICAgIHRoaXMuX3NpemUgPSAwO1xuICAgIH1cbiAgICBhbGwoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLl9hbm5vdGF0aW9ucztcbiAgICB9XG4gICAgZ2V0IHNpemUoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLl9zaXplO1xuICAgIH1cbiAgICBtYW5hZ2UoaWRPckFubm90YXRpb24sIGFubm90YXRpb24pIHtcbiAgICAgICAgbGV0IGlkO1xuICAgICAgICBpZiAoQ2hhbmdlQW5ub3RhdGlvbklkZW50aWZpZXIuaXMoaWRPckFubm90YXRpb24pKSB7XG4gICAgICAgICAgICBpZCA9IGlkT3JBbm5vdGF0aW9uO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgaWQgPSB0aGlzLm5leHRJZCgpO1xuICAgICAgICAgICAgYW5ub3RhdGlvbiA9IGlkT3JBbm5vdGF0aW9uO1xuICAgICAgICB9XG4gICAgICAgIGlmICh0aGlzLl9hbm5vdGF0aW9uc1tpZF0gIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBJZCAke2lkfSBpcyBhbHJlYWR5IGluIHVzZS5gKTtcbiAgICAgICAgfVxuICAgICAgICBpZiAoYW5ub3RhdGlvbiA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYE5vIGFubm90YXRpb24gcHJvdmlkZWQgZm9yIGlkICR7aWR9YCk7XG4gICAgICAgIH1cbiAgICAgICAgdGhpcy5fYW5ub3RhdGlvbnNbaWRdID0gYW5ub3RhdGlvbjtcbiAgICAgICAgdGhpcy5fc2l6ZSsrO1xuICAgICAgICByZXR1cm4gaWQ7XG4gICAgfVxuICAgIG5leHRJZCgpIHtcbiAgICAgICAgdGhpcy5fY291bnRlcisrO1xuICAgICAgICByZXR1cm4gdGhpcy5fY291bnRlci50b1N0cmluZygpO1xuICAgIH1cbn1cbi8qKlxuICogQSB3b3Jrc3BhY2UgY2hhbmdlIGhlbHBzIGNvbnN0cnVjdGluZyBjaGFuZ2VzIHRvIGEgd29ya3NwYWNlLlxuICovXG5leHBvcnQgY2xhc3MgV29ya3NwYWNlQ2hhbmdlIHtcbiAgICBjb25zdHJ1Y3Rvcih3b3Jrc3BhY2VFZGl0KSB7XG4gICAgICAgIHRoaXMuX3RleHRFZGl0Q2hhbmdlcyA9IE9iamVjdC5jcmVhdGUobnVsbCk7XG4gICAgICAgIGlmICh3b3Jrc3BhY2VFZGl0ICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHRoaXMuX3dvcmtzcGFjZUVkaXQgPSB3b3Jrc3BhY2VFZGl0O1xuICAgICAgICAgICAgaWYgKHdvcmtzcGFjZUVkaXQuZG9jdW1lbnRDaGFuZ2VzKSB7XG4gICAgICAgICAgICAgICAgdGhpcy5fY2hhbmdlQW5ub3RhdGlvbnMgPSBuZXcgQ2hhbmdlQW5ub3RhdGlvbnMod29ya3NwYWNlRWRpdC5jaGFuZ2VBbm5vdGF0aW9ucyk7XG4gICAgICAgICAgICAgICAgd29ya3NwYWNlRWRpdC5jaGFuZ2VBbm5vdGF0aW9ucyA9IHRoaXMuX2NoYW5nZUFubm90YXRpb25zLmFsbCgpO1xuICAgICAgICAgICAgICAgIHdvcmtzcGFjZUVkaXQuZG9jdW1lbnRDaGFuZ2VzLmZvckVhY2goKGNoYW5nZSkgPT4ge1xuICAgICAgICAgICAgICAgICAgICBpZiAoVGV4dERvY3VtZW50RWRpdC5pcyhjaGFuZ2UpKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBjb25zdCB0ZXh0RWRpdENoYW5nZSA9IG5ldyBUZXh0RWRpdENoYW5nZUltcGwoY2hhbmdlLmVkaXRzLCB0aGlzLl9jaGFuZ2VBbm5vdGF0aW9ucyk7XG4gICAgICAgICAgICAgICAgICAgICAgICB0aGlzLl90ZXh0RWRpdENoYW5nZXNbY2hhbmdlLnRleHREb2N1bWVudC51cmldID0gdGV4dEVkaXRDaGFuZ2U7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9KTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2UgaWYgKHdvcmtzcGFjZUVkaXQuY2hhbmdlcykge1xuICAgICAgICAgICAgICAgIE9iamVjdC5rZXlzKHdvcmtzcGFjZUVkaXQuY2hhbmdlcykuZm9yRWFjaCgoa2V5KSA9PiB7XG4gICAgICAgICAgICAgICAgICAgIGNvbnN0IHRleHRFZGl0Q2hhbmdlID0gbmV3IFRleHRFZGl0Q2hhbmdlSW1wbCh3b3Jrc3BhY2VFZGl0LmNoYW5nZXNba2V5XSk7XG4gICAgICAgICAgICAgICAgICAgIHRoaXMuX3RleHRFZGl0Q2hhbmdlc1trZXldID0gdGV4dEVkaXRDaGFuZ2U7XG4gICAgICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICB0aGlzLl93b3Jrc3BhY2VFZGl0ID0ge307XG4gICAgICAgIH1cbiAgICB9XG4gICAgLyoqXG4gICAgICogUmV0dXJucyB0aGUgdW5kZXJseWluZyB7QGxpbmsgV29ya3NwYWNlRWRpdH0gbGl0ZXJhbFxuICAgICAqIHVzZSB0byBiZSByZXR1cm5lZCBmcm9tIGEgd29ya3NwYWNlIGVkaXQgb3BlcmF0aW9uIGxpa2UgcmVuYW1lLlxuICAgICAqL1xuICAgIGdldCBlZGl0KCkge1xuICAgICAgICB0aGlzLmluaXREb2N1bWVudENoYW5nZXMoKTtcbiAgICAgICAgaWYgKHRoaXMuX2NoYW5nZUFubm90YXRpb25zICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIGlmICh0aGlzLl9jaGFuZ2VBbm5vdGF0aW9ucy5zaXplID09PSAwKSB7XG4gICAgICAgICAgICAgICAgdGhpcy5fd29ya3NwYWNlRWRpdC5jaGFuZ2VBbm5vdGF0aW9ucyA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIHRoaXMuX3dvcmtzcGFjZUVkaXQuY2hhbmdlQW5ub3RhdGlvbnMgPSB0aGlzLl9jaGFuZ2VBbm5vdGF0aW9ucy5hbGwoKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gdGhpcy5fd29ya3NwYWNlRWRpdDtcbiAgICB9XG4gICAgZ2V0VGV4dEVkaXRDaGFuZ2Uoa2V5KSB7XG4gICAgICAgIGlmIChPcHRpb25hbFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXIuaXMoa2V5KSkge1xuICAgICAgICAgICAgdGhpcy5pbml0RG9jdW1lbnRDaGFuZ2VzKCk7XG4gICAgICAgICAgICBpZiAodGhpcy5fd29ya3NwYWNlRWRpdC5kb2N1bWVudENoYW5nZXMgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcignV29ya3NwYWNlIGVkaXQgaXMgbm90IGNvbmZpZ3VyZWQgZm9yIGRvY3VtZW50IGNoYW5nZXMuJyk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBjb25zdCB0ZXh0RG9jdW1lbnQgPSB7IHVyaToga2V5LnVyaSwgdmVyc2lvbjoga2V5LnZlcnNpb24gfTtcbiAgICAgICAgICAgIGxldCByZXN1bHQgPSB0aGlzLl90ZXh0RWRpdENoYW5nZXNbdGV4dERvY3VtZW50LnVyaV07XG4gICAgICAgICAgICBpZiAoIXJlc3VsdCkge1xuICAgICAgICAgICAgICAgIGNvbnN0IGVkaXRzID0gW107XG4gICAgICAgICAgICAgICAgY29uc3QgdGV4dERvY3VtZW50RWRpdCA9IHtcbiAgICAgICAgICAgICAgICAgICAgdGV4dERvY3VtZW50LFxuICAgICAgICAgICAgICAgICAgICBlZGl0c1xuICAgICAgICAgICAgICAgIH07XG4gICAgICAgICAgICAgICAgdGhpcy5fd29ya3NwYWNlRWRpdC5kb2N1bWVudENoYW5nZXMucHVzaCh0ZXh0RG9jdW1lbnRFZGl0KTtcbiAgICAgICAgICAgICAgICByZXN1bHQgPSBuZXcgVGV4dEVkaXRDaGFuZ2VJbXBsKGVkaXRzLCB0aGlzLl9jaGFuZ2VBbm5vdGF0aW9ucyk7XG4gICAgICAgICAgICAgICAgdGhpcy5fdGV4dEVkaXRDaGFuZ2VzW3RleHREb2N1bWVudC51cmldID0gcmVzdWx0O1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHRoaXMuaW5pdENoYW5nZXMoKTtcbiAgICAgICAgICAgIGlmICh0aGlzLl93b3Jrc3BhY2VFZGl0LmNoYW5nZXMgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcignV29ya3NwYWNlIGVkaXQgaXMgbm90IGNvbmZpZ3VyZWQgZm9yIG5vcm1hbCB0ZXh0IGVkaXQgY2hhbmdlcy4nKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGxldCByZXN1bHQgPSB0aGlzLl90ZXh0RWRpdENoYW5nZXNba2V5XTtcbiAgICAgICAgICAgIGlmICghcmVzdWx0KSB7XG4gICAgICAgICAgICAgICAgbGV0IGVkaXRzID0gW107XG4gICAgICAgICAgICAgICAgdGhpcy5fd29ya3NwYWNlRWRpdC5jaGFuZ2VzW2tleV0gPSBlZGl0cztcbiAgICAgICAgICAgICAgICByZXN1bHQgPSBuZXcgVGV4dEVkaXRDaGFuZ2VJbXBsKGVkaXRzKTtcbiAgICAgICAgICAgICAgICB0aGlzLl90ZXh0RWRpdENoYW5nZXNba2V5XSA9IHJlc3VsdDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgICAgIH1cbiAgICB9XG4gICAgaW5pdERvY3VtZW50Q2hhbmdlcygpIHtcbiAgICAgICAgaWYgKHRoaXMuX3dvcmtzcGFjZUVkaXQuZG9jdW1lbnRDaGFuZ2VzID09PSB1bmRlZmluZWQgJiYgdGhpcy5fd29ya3NwYWNlRWRpdC5jaGFuZ2VzID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHRoaXMuX2NoYW5nZUFubm90YXRpb25zID0gbmV3IENoYW5nZUFubm90YXRpb25zKCk7XG4gICAgICAgICAgICB0aGlzLl93b3Jrc3BhY2VFZGl0LmRvY3VtZW50Q2hhbmdlcyA9IFtdO1xuICAgICAgICAgICAgdGhpcy5fd29ya3NwYWNlRWRpdC5jaGFuZ2VBbm5vdGF0aW9ucyA9IHRoaXMuX2NoYW5nZUFubm90YXRpb25zLmFsbCgpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGluaXRDaGFuZ2VzKCkge1xuICAgICAgICBpZiAodGhpcy5fd29ya3NwYWNlRWRpdC5kb2N1bWVudENoYW5nZXMgPT09IHVuZGVmaW5lZCAmJiB0aGlzLl93b3Jrc3BhY2VFZGl0LmNoYW5nZXMgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgdGhpcy5fd29ya3NwYWNlRWRpdC5jaGFuZ2VzID0gT2JqZWN0LmNyZWF0ZShudWxsKTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBjcmVhdGVGaWxlKHVyaSwgb3B0aW9uc09yQW5ub3RhdGlvbiwgb3B0aW9ucykge1xuICAgICAgICB0aGlzLmluaXREb2N1bWVudENoYW5nZXMoKTtcbiAgICAgICAgaWYgKHRoaXMuX3dvcmtzcGFjZUVkaXQuZG9jdW1lbnRDaGFuZ2VzID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcignV29ya3NwYWNlIGVkaXQgaXMgbm90IGNvbmZpZ3VyZWQgZm9yIGRvY3VtZW50IGNoYW5nZXMuJyk7XG4gICAgICAgIH1cbiAgICAgICAgbGV0IGFubm90YXRpb247XG4gICAgICAgIGlmIChDaGFuZ2VBbm5vdGF0aW9uLmlzKG9wdGlvbnNPckFubm90YXRpb24pIHx8IENoYW5nZUFubm90YXRpb25JZGVudGlmaWVyLmlzKG9wdGlvbnNPckFubm90YXRpb24pKSB7XG4gICAgICAgICAgICBhbm5vdGF0aW9uID0gb3B0aW9uc09yQW5ub3RhdGlvbjtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIG9wdGlvbnMgPSBvcHRpb25zT3JBbm5vdGF0aW9uO1xuICAgICAgICB9XG4gICAgICAgIGxldCBvcGVyYXRpb247XG4gICAgICAgIGxldCBpZDtcbiAgICAgICAgaWYgKGFubm90YXRpb24gPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgb3BlcmF0aW9uID0gQ3JlYXRlRmlsZS5jcmVhdGUodXJpLCBvcHRpb25zKTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIGlkID0gQ2hhbmdlQW5ub3RhdGlvbklkZW50aWZpZXIuaXMoYW5ub3RhdGlvbikgPyBhbm5vdGF0aW9uIDogdGhpcy5fY2hhbmdlQW5ub3RhdGlvbnMubWFuYWdlKGFubm90YXRpb24pO1xuICAgICAgICAgICAgb3BlcmF0aW9uID0gQ3JlYXRlRmlsZS5jcmVhdGUodXJpLCBvcHRpb25zLCBpZCk7XG4gICAgICAgIH1cbiAgICAgICAgdGhpcy5fd29ya3NwYWNlRWRpdC5kb2N1bWVudENoYW5nZXMucHVzaChvcGVyYXRpb24pO1xuICAgICAgICBpZiAoaWQgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmV0dXJuIGlkO1xuICAgICAgICB9XG4gICAgfVxuICAgIHJlbmFtZUZpbGUob2xkVXJpLCBuZXdVcmksIG9wdGlvbnNPckFubm90YXRpb24sIG9wdGlvbnMpIHtcbiAgICAgICAgdGhpcy5pbml0RG9jdW1lbnRDaGFuZ2VzKCk7XG4gICAgICAgIGlmICh0aGlzLl93b3Jrc3BhY2VFZGl0LmRvY3VtZW50Q2hhbmdlcyA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoJ1dvcmtzcGFjZSBlZGl0IGlzIG5vdCBjb25maWd1cmVkIGZvciBkb2N1bWVudCBjaGFuZ2VzLicpO1xuICAgICAgICB9XG4gICAgICAgIGxldCBhbm5vdGF0aW9uO1xuICAgICAgICBpZiAoQ2hhbmdlQW5ub3RhdGlvbi5pcyhvcHRpb25zT3JBbm5vdGF0aW9uKSB8fCBDaGFuZ2VBbm5vdGF0aW9uSWRlbnRpZmllci5pcyhvcHRpb25zT3JBbm5vdGF0aW9uKSkge1xuICAgICAgICAgICAgYW5ub3RhdGlvbiA9IG9wdGlvbnNPckFubm90YXRpb247XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBvcHRpb25zID0gb3B0aW9uc09yQW5ub3RhdGlvbjtcbiAgICAgICAgfVxuICAgICAgICBsZXQgb3BlcmF0aW9uO1xuICAgICAgICBsZXQgaWQ7XG4gICAgICAgIGlmIChhbm5vdGF0aW9uID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIG9wZXJhdGlvbiA9IFJlbmFtZUZpbGUuY3JlYXRlKG9sZFVyaSwgbmV3VXJpLCBvcHRpb25zKTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIGlkID0gQ2hhbmdlQW5ub3RhdGlvbklkZW50aWZpZXIuaXMoYW5ub3RhdGlvbikgPyBhbm5vdGF0aW9uIDogdGhpcy5fY2hhbmdlQW5ub3RhdGlvbnMubWFuYWdlKGFubm90YXRpb24pO1xuICAgICAgICAgICAgb3BlcmF0aW9uID0gUmVuYW1lRmlsZS5jcmVhdGUob2xkVXJpLCBuZXdVcmksIG9wdGlvbnMsIGlkKTtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLl93b3Jrc3BhY2VFZGl0LmRvY3VtZW50Q2hhbmdlcy5wdXNoKG9wZXJhdGlvbik7XG4gICAgICAgIGlmIChpZCAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXR1cm4gaWQ7XG4gICAgICAgIH1cbiAgICB9XG4gICAgZGVsZXRlRmlsZSh1cmksIG9wdGlvbnNPckFubm90YXRpb24sIG9wdGlvbnMpIHtcbiAgICAgICAgdGhpcy5pbml0RG9jdW1lbnRDaGFuZ2VzKCk7XG4gICAgICAgIGlmICh0aGlzLl93b3Jrc3BhY2VFZGl0LmRvY3VtZW50Q2hhbmdlcyA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoJ1dvcmtzcGFjZSBlZGl0IGlzIG5vdCBjb25maWd1cmVkIGZvciBkb2N1bWVudCBjaGFuZ2VzLicpO1xuICAgICAgICB9XG4gICAgICAgIGxldCBhbm5vdGF0aW9uO1xuICAgICAgICBpZiAoQ2hhbmdlQW5ub3RhdGlvbi5pcyhvcHRpb25zT3JBbm5vdGF0aW9uKSB8fCBDaGFuZ2VBbm5vdGF0aW9uSWRlbnRpZmllci5pcyhvcHRpb25zT3JBbm5vdGF0aW9uKSkge1xuICAgICAgICAgICAgYW5ub3RhdGlvbiA9IG9wdGlvbnNPckFubm90YXRpb247XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBvcHRpb25zID0gb3B0aW9uc09yQW5ub3RhdGlvbjtcbiAgICAgICAgfVxuICAgICAgICBsZXQgb3BlcmF0aW9uO1xuICAgICAgICBsZXQgaWQ7XG4gICAgICAgIGlmIChhbm5vdGF0aW9uID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIG9wZXJhdGlvbiA9IERlbGV0ZUZpbGUuY3JlYXRlKHVyaSwgb3B0aW9ucyk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBpZCA9IENoYW5nZUFubm90YXRpb25JZGVudGlmaWVyLmlzKGFubm90YXRpb24pID8gYW5ub3RhdGlvbiA6IHRoaXMuX2NoYW5nZUFubm90YXRpb25zLm1hbmFnZShhbm5vdGF0aW9uKTtcbiAgICAgICAgICAgIG9wZXJhdGlvbiA9IERlbGV0ZUZpbGUuY3JlYXRlKHVyaSwgb3B0aW9ucywgaWQpO1xuICAgICAgICB9XG4gICAgICAgIHRoaXMuX3dvcmtzcGFjZUVkaXQuZG9jdW1lbnRDaGFuZ2VzLnB1c2gob3BlcmF0aW9uKTtcbiAgICAgICAgaWYgKGlkICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHJldHVybiBpZDtcbiAgICAgICAgfVxuICAgIH1cbn1cbi8qKlxuICogVGhlIFRleHREb2N1bWVudElkZW50aWZpZXIgbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgVGV4dERvY3VtZW50SWRlbnRpZmllcn0gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgVGV4dERvY3VtZW50SWRlbnRpZmllcjtcbihmdW5jdGlvbiAoVGV4dERvY3VtZW50SWRlbnRpZmllcikge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgVGV4dERvY3VtZW50SWRlbnRpZmllciBsaXRlcmFsLlxuICAgICAqIEBwYXJhbSB1cmkgVGhlIGRvY3VtZW50J3MgdXJpLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZSh1cmkpIHtcbiAgICAgICAgcmV0dXJuIHsgdXJpIH07XG4gICAgfVxuICAgIFRleHREb2N1bWVudElkZW50aWZpZXIuY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgVGV4dERvY3VtZW50SWRlbnRpZmllcn0gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLmRlZmluZWQoY2FuZGlkYXRlKSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLnVyaSk7XG4gICAgfVxuICAgIFRleHREb2N1bWVudElkZW50aWZpZXIuaXMgPSBpcztcbn0pKFRleHREb2N1bWVudElkZW50aWZpZXIgfHwgKFRleHREb2N1bWVudElkZW50aWZpZXIgPSB7fSkpO1xuLyoqXG4gKiBUaGUgVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllciBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyfSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyO1xuKGZ1bmN0aW9uIChWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyIGxpdGVyYWwuXG4gICAgICogQHBhcmFtIHVyaSBUaGUgZG9jdW1lbnQncyB1cmkuXG4gICAgICogQHBhcmFtIHZlcnNpb24gVGhlIGRvY3VtZW50J3MgdmVyc2lvbi5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUodXJpLCB2ZXJzaW9uKSB7XG4gICAgICAgIHJldHVybiB7IHVyaSwgdmVyc2lvbiB9O1xuICAgIH1cbiAgICBWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXJ9IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5kZWZpbmVkKGNhbmRpZGF0ZSkgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS51cmkpICYmIElzLmludGVnZXIoY2FuZGlkYXRlLnZlcnNpb24pO1xuICAgIH1cbiAgICBWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyLmlzID0gaXM7XG59KShWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyIHx8IChWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyID0ge30pKTtcbi8qKlxuICogVGhlIE9wdGlvbmFsVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllciBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBPcHRpb25hbFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXJ9IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIE9wdGlvbmFsVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllcjtcbihmdW5jdGlvbiAoT3B0aW9uYWxWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBPcHRpb25hbFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXIgbGl0ZXJhbC5cbiAgICAgKiBAcGFyYW0gdXJpIFRoZSBkb2N1bWVudCdzIHVyaS5cbiAgICAgKiBAcGFyYW0gdmVyc2lvbiBUaGUgZG9jdW1lbnQncyB2ZXJzaW9uLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZSh1cmksIHZlcnNpb24pIHtcbiAgICAgICAgcmV0dXJuIHsgdXJpLCB2ZXJzaW9uIH07XG4gICAgfVxuICAgIE9wdGlvbmFsVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllci5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBPcHRpb25hbFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXJ9IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5kZWZpbmVkKGNhbmRpZGF0ZSkgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS51cmkpICYmIChjYW5kaWRhdGUudmVyc2lvbiA9PT0gbnVsbCB8fCBJcy5pbnRlZ2VyKGNhbmRpZGF0ZS52ZXJzaW9uKSk7XG4gICAgfVxuICAgIE9wdGlvbmFsVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllci5pcyA9IGlzO1xufSkoT3B0aW9uYWxWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyIHx8IChPcHRpb25hbFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXIgPSB7fSkpO1xuLyoqXG4gKiBUaGUgVGV4dERvY3VtZW50SXRlbSBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBUZXh0RG9jdW1lbnRJdGVtfSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBUZXh0RG9jdW1lbnRJdGVtO1xuKGZ1bmN0aW9uIChUZXh0RG9jdW1lbnRJdGVtKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBUZXh0RG9jdW1lbnRJdGVtIGxpdGVyYWwuXG4gICAgICogQHBhcmFtIHVyaSBUaGUgZG9jdW1lbnQncyB1cmkuXG4gICAgICogQHBhcmFtIGxhbmd1YWdlSWQgVGhlIGRvY3VtZW50J3MgbGFuZ3VhZ2UgaWRlbnRpZmllci5cbiAgICAgKiBAcGFyYW0gdmVyc2lvbiBUaGUgZG9jdW1lbnQncyB2ZXJzaW9uIG51bWJlci5cbiAgICAgKiBAcGFyYW0gdGV4dCBUaGUgZG9jdW1lbnQncyB0ZXh0LlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZSh1cmksIGxhbmd1YWdlSWQsIHZlcnNpb24sIHRleHQpIHtcbiAgICAgICAgcmV0dXJuIHsgdXJpLCBsYW5ndWFnZUlkLCB2ZXJzaW9uLCB0ZXh0IH07XG4gICAgfVxuICAgIFRleHREb2N1bWVudEl0ZW0uY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgVGV4dERvY3VtZW50SXRlbX0gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLmRlZmluZWQoY2FuZGlkYXRlKSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLnVyaSkgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS5sYW5ndWFnZUlkKSAmJiBJcy5pbnRlZ2VyKGNhbmRpZGF0ZS52ZXJzaW9uKSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLnRleHQpO1xuICAgIH1cbiAgICBUZXh0RG9jdW1lbnRJdGVtLmlzID0gaXM7XG59KShUZXh0RG9jdW1lbnRJdGVtIHx8IChUZXh0RG9jdW1lbnRJdGVtID0ge30pKTtcbi8qKlxuICogRGVzY3JpYmVzIHRoZSBjb250ZW50IHR5cGUgdGhhdCBhIGNsaWVudCBzdXBwb3J0cyBpbiB2YXJpb3VzXG4gKiByZXN1bHQgbGl0ZXJhbHMgbGlrZSBgSG92ZXJgLCBgUGFyYW1ldGVySW5mb2Agb3IgYENvbXBsZXRpb25JdGVtYC5cbiAqXG4gKiBQbGVhc2Ugbm90ZSB0aGF0IGBNYXJrdXBLaW5kc2AgbXVzdCBub3Qgc3RhcnQgd2l0aCBhIGAkYC4gVGhpcyBraW5kc1xuICogYXJlIHJlc2VydmVkIGZvciBpbnRlcm5hbCB1c2FnZS5cbiAqL1xuZXhwb3J0IHZhciBNYXJrdXBLaW5kO1xuKGZ1bmN0aW9uIChNYXJrdXBLaW5kKSB7XG4gICAgLyoqXG4gICAgICogUGxhaW4gdGV4dCBpcyBzdXBwb3J0ZWQgYXMgYSBjb250ZW50IGZvcm1hdFxuICAgICAqL1xuICAgIE1hcmt1cEtpbmQuUGxhaW5UZXh0ID0gJ3BsYWludGV4dCc7XG4gICAgLyoqXG4gICAgICogTWFya2Rvd24gaXMgc3VwcG9ydGVkIGFzIGEgY29udGVudCBmb3JtYXRcbiAgICAgKi9cbiAgICBNYXJrdXBLaW5kLk1hcmtkb3duID0gJ21hcmtkb3duJztcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gdmFsdWUgaXMgYSB2YWx1ZSBvZiB0aGUge0BsaW5rIE1hcmt1cEtpbmR9IHR5cGUuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBjYW5kaWRhdGUgPT09IE1hcmt1cEtpbmQuUGxhaW5UZXh0IHx8IGNhbmRpZGF0ZSA9PT0gTWFya3VwS2luZC5NYXJrZG93bjtcbiAgICB9XG4gICAgTWFya3VwS2luZC5pcyA9IGlzO1xufSkoTWFya3VwS2luZCB8fCAoTWFya3VwS2luZCA9IHt9KSk7XG5leHBvcnQgdmFyIE1hcmt1cENvbnRlbnQ7XG4oZnVuY3Rpb24gKE1hcmt1cENvbnRlbnQpIHtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gdmFsdWUgY29uZm9ybXMgdG8gdGhlIHtAbGluayBNYXJrdXBDb250ZW50fSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5vYmplY3RMaXRlcmFsKHZhbHVlKSAmJiBNYXJrdXBLaW5kLmlzKGNhbmRpZGF0ZS5raW5kKSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLnZhbHVlKTtcbiAgICB9XG4gICAgTWFya3VwQ29udGVudC5pcyA9IGlzO1xufSkoTWFya3VwQ29udGVudCB8fCAoTWFya3VwQ29udGVudCA9IHt9KSk7XG4vKipcbiAqIFRoZSBraW5kIG9mIGEgY29tcGxldGlvbiBlbnRyeS5cbiAqL1xuZXhwb3J0IHZhciBDb21wbGV0aW9uSXRlbUtpbmQ7XG4oZnVuY3Rpb24gKENvbXBsZXRpb25JdGVtS2luZCkge1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5UZXh0ID0gMTtcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuTWV0aG9kID0gMjtcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuRnVuY3Rpb24gPSAzO1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5Db25zdHJ1Y3RvciA9IDQ7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLkZpZWxkID0gNTtcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuVmFyaWFibGUgPSA2O1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5DbGFzcyA9IDc7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLkludGVyZmFjZSA9IDg7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLk1vZHVsZSA9IDk7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLlByb3BlcnR5ID0gMTA7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLlVuaXQgPSAxMTtcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuVmFsdWUgPSAxMjtcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuRW51bSA9IDEzO1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5LZXl3b3JkID0gMTQ7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLlNuaXBwZXQgPSAxNTtcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuQ29sb3IgPSAxNjtcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuRmlsZSA9IDE3O1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5SZWZlcmVuY2UgPSAxODtcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuRm9sZGVyID0gMTk7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLkVudW1NZW1iZXIgPSAyMDtcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuQ29uc3RhbnQgPSAyMTtcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuU3RydWN0ID0gMjI7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLkV2ZW50ID0gMjM7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLk9wZXJhdG9yID0gMjQ7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLlR5cGVQYXJhbWV0ZXIgPSAyNTtcbn0pKENvbXBsZXRpb25JdGVtS2luZCB8fCAoQ29tcGxldGlvbkl0ZW1LaW5kID0ge30pKTtcbi8qKlxuICogRGVmaW5lcyB3aGV0aGVyIHRoZSBpbnNlcnQgdGV4dCBpbiBhIGNvbXBsZXRpb24gaXRlbSBzaG91bGQgYmUgaW50ZXJwcmV0ZWQgYXNcbiAqIHBsYWluIHRleHQgb3IgYSBzbmlwcGV0LlxuICovXG5leHBvcnQgdmFyIEluc2VydFRleHRGb3JtYXQ7XG4oZnVuY3Rpb24gKEluc2VydFRleHRGb3JtYXQpIHtcbiAgICAvKipcbiAgICAgKiBUaGUgcHJpbWFyeSB0ZXh0IHRvIGJlIGluc2VydGVkIGlzIHRyZWF0ZWQgYXMgYSBwbGFpbiBzdHJpbmcuXG4gICAgICovXG4gICAgSW5zZXJ0VGV4dEZvcm1hdC5QbGFpblRleHQgPSAxO1xuICAgIC8qKlxuICAgICAqIFRoZSBwcmltYXJ5IHRleHQgdG8gYmUgaW5zZXJ0ZWQgaXMgdHJlYXRlZCBhcyBhIHNuaXBwZXQuXG4gICAgICpcbiAgICAgKiBBIHNuaXBwZXQgY2FuIGRlZmluZSB0YWIgc3RvcHMgYW5kIHBsYWNlaG9sZGVycyB3aXRoIGAkMWAsIGAkMmBcbiAgICAgKiBhbmQgYCR7Mzpmb299YC4gYCQwYCBkZWZpbmVzIHRoZSBmaW5hbCB0YWIgc3RvcCwgaXQgZGVmYXVsdHMgdG9cbiAgICAgKiB0aGUgZW5kIG9mIHRoZSBzbmlwcGV0LiBQbGFjZWhvbGRlcnMgd2l0aCBlcXVhbCBpZGVudGlmaWVycyBhcmUgbGlua2VkLFxuICAgICAqIHRoYXQgaXMgdHlwaW5nIGluIG9uZSB3aWxsIHVwZGF0ZSBvdGhlcnMgdG9vLlxuICAgICAqXG4gICAgICogU2VlIGFsc286IGh0dHBzOi8vbWljcm9zb2Z0LmdpdGh1Yi5pby9sYW5ndWFnZS1zZXJ2ZXItcHJvdG9jb2wvc3BlY2lmaWNhdGlvbnMvc3BlY2lmaWNhdGlvbi1jdXJyZW50LyNzbmlwcGV0X3N5bnRheFxuICAgICAqL1xuICAgIEluc2VydFRleHRGb3JtYXQuU25pcHBldCA9IDI7XG59KShJbnNlcnRUZXh0Rm9ybWF0IHx8IChJbnNlcnRUZXh0Rm9ybWF0ID0ge30pKTtcbi8qKlxuICogQ29tcGxldGlvbiBpdGVtIHRhZ3MgYXJlIGV4dHJhIGFubm90YXRpb25zIHRoYXQgdHdlYWsgdGhlIHJlbmRlcmluZyBvZiBhIGNvbXBsZXRpb25cbiAqIGl0ZW0uXG4gKlxuICogQHNpbmNlIDMuMTUuMFxuICovXG5leHBvcnQgdmFyIENvbXBsZXRpb25JdGVtVGFnO1xuKGZ1bmN0aW9uIChDb21wbGV0aW9uSXRlbVRhZykge1xuICAgIC8qKlxuICAgICAqIFJlbmRlciBhIGNvbXBsZXRpb24gYXMgb2Jzb2xldGUsIHVzdWFsbHkgdXNpbmcgYSBzdHJpa2Utb3V0LlxuICAgICAqL1xuICAgIENvbXBsZXRpb25JdGVtVGFnLkRlcHJlY2F0ZWQgPSAxO1xufSkoQ29tcGxldGlvbkl0ZW1UYWcgfHwgKENvbXBsZXRpb25JdGVtVGFnID0ge30pKTtcbi8qKlxuICogVGhlIEluc2VydFJlcGxhY2VFZGl0IG5hbWVzcGFjZSBwcm92aWRlcyBmdW5jdGlvbnMgdG8gZGVhbCB3aXRoIGluc2VydCAvIHJlcGxhY2UgZWRpdHMuXG4gKlxuICogQHNpbmNlIDMuMTYuMFxuICovXG5leHBvcnQgdmFyIEluc2VydFJlcGxhY2VFZGl0O1xuKGZ1bmN0aW9uIChJbnNlcnRSZXBsYWNlRWRpdCkge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgaW5zZXJ0IC8gcmVwbGFjZSBlZGl0XG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKG5ld1RleHQsIGluc2VydCwgcmVwbGFjZSkge1xuICAgICAgICByZXR1cm4geyBuZXdUZXh0LCBpbnNlcnQsIHJlcGxhY2UgfTtcbiAgICB9XG4gICAgSW5zZXJ0UmVwbGFjZUVkaXQuY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgSW5zZXJ0UmVwbGFjZUVkaXR9IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLm5ld1RleHQpICYmIFJhbmdlLmlzKGNhbmRpZGF0ZS5pbnNlcnQpICYmIFJhbmdlLmlzKGNhbmRpZGF0ZS5yZXBsYWNlKTtcbiAgICB9XG4gICAgSW5zZXJ0UmVwbGFjZUVkaXQuaXMgPSBpcztcbn0pKEluc2VydFJlcGxhY2VFZGl0IHx8IChJbnNlcnRSZXBsYWNlRWRpdCA9IHt9KSk7XG4vKipcbiAqIEhvdyB3aGl0ZXNwYWNlIGFuZCBpbmRlbnRhdGlvbiBpcyBoYW5kbGVkIGR1cmluZyBjb21wbGV0aW9uXG4gKiBpdGVtIGluc2VydGlvbi5cbiAqXG4gKiBAc2luY2UgMy4xNi4wXG4gKi9cbmV4cG9ydCB2YXIgSW5zZXJ0VGV4dE1vZGU7XG4oZnVuY3Rpb24gKEluc2VydFRleHRNb2RlKSB7XG4gICAgLyoqXG4gICAgICogVGhlIGluc2VydGlvbiBvciByZXBsYWNlIHN0cmluZ3MgaXMgdGFrZW4gYXMgaXQgaXMuIElmIHRoZVxuICAgICAqIHZhbHVlIGlzIG11bHRpIGxpbmUgdGhlIGxpbmVzIGJlbG93IHRoZSBjdXJzb3Igd2lsbCBiZVxuICAgICAqIGluc2VydGVkIHVzaW5nIHRoZSBpbmRlbnRhdGlvbiBkZWZpbmVkIGluIHRoZSBzdHJpbmcgdmFsdWUuXG4gICAgICogVGhlIGNsaWVudCB3aWxsIG5vdCBhcHBseSBhbnkga2luZCBvZiBhZGp1c3RtZW50cyB0byB0aGVcbiAgICAgKiBzdHJpbmcuXG4gICAgICovXG4gICAgSW5zZXJ0VGV4dE1vZGUuYXNJcyA9IDE7XG4gICAgLyoqXG4gICAgICogVGhlIGVkaXRvciBhZGp1c3RzIGxlYWRpbmcgd2hpdGVzcGFjZSBvZiBuZXcgbGluZXMgc28gdGhhdFxuICAgICAqIHRoZXkgbWF0Y2ggdGhlIGluZGVudGF0aW9uIHVwIHRvIHRoZSBjdXJzb3Igb2YgdGhlIGxpbmUgZm9yXG4gICAgICogd2hpY2ggdGhlIGl0ZW0gaXMgYWNjZXB0ZWQuXG4gICAgICpcbiAgICAgKiBDb25zaWRlciBhIGxpbmUgbGlrZSB0aGlzOiA8MnRhYnM+PGN1cnNvcj48M3RhYnM+Zm9vLiBBY2NlcHRpbmcgYVxuICAgICAqIG11bHRpIGxpbmUgY29tcGxldGlvbiBpdGVtIGlzIGluZGVudGVkIHVzaW5nIDIgdGFicyBhbmQgYWxsXG4gICAgICogZm9sbG93aW5nIGxpbmVzIGluc2VydGVkIHdpbGwgYmUgaW5kZW50ZWQgdXNpbmcgMiB0YWJzIGFzIHdlbGwuXG4gICAgICovXG4gICAgSW5zZXJ0VGV4dE1vZGUuYWRqdXN0SW5kZW50YXRpb24gPSAyO1xufSkoSW5zZXJ0VGV4dE1vZGUgfHwgKEluc2VydFRleHRNb2RlID0ge30pKTtcbmV4cG9ydCB2YXIgQ29tcGxldGlvbkl0ZW1MYWJlbERldGFpbHM7XG4oZnVuY3Rpb24gKENvbXBsZXRpb25JdGVtTGFiZWxEZXRhaWxzKSB7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBjYW5kaWRhdGUgJiYgKElzLnN0cmluZyhjYW5kaWRhdGUuZGV0YWlsKSB8fCBjYW5kaWRhdGUuZGV0YWlsID09PSB1bmRlZmluZWQpICYmXG4gICAgICAgICAgICAoSXMuc3RyaW5nKGNhbmRpZGF0ZS5kZXNjcmlwdGlvbikgfHwgY2FuZGlkYXRlLmRlc2NyaXB0aW9uID09PSB1bmRlZmluZWQpO1xuICAgIH1cbiAgICBDb21wbGV0aW9uSXRlbUxhYmVsRGV0YWlscy5pcyA9IGlzO1xufSkoQ29tcGxldGlvbkl0ZW1MYWJlbERldGFpbHMgfHwgKENvbXBsZXRpb25JdGVtTGFiZWxEZXRhaWxzID0ge30pKTtcbi8qKlxuICogVGhlIENvbXBsZXRpb25JdGVtIG5hbWVzcGFjZSBwcm92aWRlcyBmdW5jdGlvbnMgdG8gZGVhbCB3aXRoXG4gKiBjb21wbGV0aW9uIGl0ZW1zLlxuICovXG5leHBvcnQgdmFyIENvbXBsZXRpb25JdGVtO1xuKGZ1bmN0aW9uIChDb21wbGV0aW9uSXRlbSkge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZSBhIGNvbXBsZXRpb24gaXRlbSBhbmQgc2VlZCBpdCB3aXRoIGEgbGFiZWwuXG4gICAgICogQHBhcmFtIGxhYmVsIFRoZSBjb21wbGV0aW9uIGl0ZW0ncyBsYWJlbFxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShsYWJlbCkge1xuICAgICAgICByZXR1cm4geyBsYWJlbCB9O1xuICAgIH1cbiAgICBDb21wbGV0aW9uSXRlbS5jcmVhdGUgPSBjcmVhdGU7XG59KShDb21wbGV0aW9uSXRlbSB8fCAoQ29tcGxldGlvbkl0ZW0gPSB7fSkpO1xuLyoqXG4gKiBUaGUgQ29tcGxldGlvbkxpc3QgbmFtZXNwYWNlIHByb3ZpZGVzIGZ1bmN0aW9ucyB0byBkZWFsIHdpdGhcbiAqIGNvbXBsZXRpb24gbGlzdHMuXG4gKi9cbmV4cG9ydCB2YXIgQ29tcGxldGlvbkxpc3Q7XG4oZnVuY3Rpb24gKENvbXBsZXRpb25MaXN0KSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBjb21wbGV0aW9uIGxpc3QuXG4gICAgICpcbiAgICAgKiBAcGFyYW0gaXRlbXMgVGhlIGNvbXBsZXRpb24gaXRlbXMuXG4gICAgICogQHBhcmFtIGlzSW5jb21wbGV0ZSBUaGUgbGlzdCBpcyBub3QgY29tcGxldGUuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKGl0ZW1zLCBpc0luY29tcGxldGUpIHtcbiAgICAgICAgcmV0dXJuIHsgaXRlbXM6IGl0ZW1zID8gaXRlbXMgOiBbXSwgaXNJbmNvbXBsZXRlOiAhIWlzSW5jb21wbGV0ZSB9O1xuICAgIH1cbiAgICBDb21wbGV0aW9uTGlzdC5jcmVhdGUgPSBjcmVhdGU7XG59KShDb21wbGV0aW9uTGlzdCB8fCAoQ29tcGxldGlvbkxpc3QgPSB7fSkpO1xuZXhwb3J0IHZhciBNYXJrZWRTdHJpbmc7XG4oZnVuY3Rpb24gKE1hcmtlZFN0cmluZykge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBtYXJrZWQgc3RyaW5nIGZyb20gcGxhaW4gdGV4dC5cbiAgICAgKlxuICAgICAqIEBwYXJhbSBwbGFpblRleHQgVGhlIHBsYWluIHRleHQuXG4gICAgICovXG4gICAgZnVuY3Rpb24gZnJvbVBsYWluVGV4dChwbGFpblRleHQpIHtcbiAgICAgICAgcmV0dXJuIHBsYWluVGV4dC5yZXBsYWNlKC9bXFxcXGAqX3t9W1xcXSgpIytcXC0uIV0vZywgJ1xcXFwkJicpOyAvLyBlc2NhcGUgbWFya2Rvd24gc3ludGF4IHRva2VuczogaHR0cDovL2RhcmluZ2ZpcmViYWxsLm5ldC9wcm9qZWN0cy9tYXJrZG93bi9zeW50YXgjYmFja3NsYXNoXG4gICAgfVxuICAgIE1hcmtlZFN0cmluZy5mcm9tUGxhaW5UZXh0ID0gZnJvbVBsYWluVGV4dDtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gdmFsdWUgY29uZm9ybXMgdG8gdGhlIHtAbGluayBNYXJrZWRTdHJpbmd9IHR5cGUuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5zdHJpbmcoY2FuZGlkYXRlKSB8fCAoSXMub2JqZWN0TGl0ZXJhbChjYW5kaWRhdGUpICYmIElzLnN0cmluZyhjYW5kaWRhdGUubGFuZ3VhZ2UpICYmIElzLnN0cmluZyhjYW5kaWRhdGUudmFsdWUpKTtcbiAgICB9XG4gICAgTWFya2VkU3RyaW5nLmlzID0gaXM7XG59KShNYXJrZWRTdHJpbmcgfHwgKE1hcmtlZFN0cmluZyA9IHt9KSk7XG5leHBvcnQgdmFyIEhvdmVyO1xuKGZ1bmN0aW9uIChIb3Zlcikge1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiB2YWx1ZSBjb25mb3JtcyB0byB0aGUge0BsaW5rIEhvdmVyfSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gISFjYW5kaWRhdGUgJiYgSXMub2JqZWN0TGl0ZXJhbChjYW5kaWRhdGUpICYmIChNYXJrdXBDb250ZW50LmlzKGNhbmRpZGF0ZS5jb250ZW50cykgfHxcbiAgICAgICAgICAgIE1hcmtlZFN0cmluZy5pcyhjYW5kaWRhdGUuY29udGVudHMpIHx8XG4gICAgICAgICAgICBJcy50eXBlZEFycmF5KGNhbmRpZGF0ZS5jb250ZW50cywgTWFya2VkU3RyaW5nLmlzKSkgJiYgKHZhbHVlLnJhbmdlID09PSB1bmRlZmluZWQgfHwgUmFuZ2UuaXModmFsdWUucmFuZ2UpKTtcbiAgICB9XG4gICAgSG92ZXIuaXMgPSBpcztcbn0pKEhvdmVyIHx8IChIb3ZlciA9IHt9KSk7XG4vKipcbiAqIFRoZSBQYXJhbWV0ZXJJbmZvcm1hdGlvbiBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBQYXJhbWV0ZXJJbmZvcm1hdGlvbn0gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgUGFyYW1ldGVySW5mb3JtYXRpb247XG4oZnVuY3Rpb24gKFBhcmFtZXRlckluZm9ybWF0aW9uKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBwYXJhbWV0ZXIgaW5mb3JtYXRpb24gbGl0ZXJhbC5cbiAgICAgKlxuICAgICAqIEBwYXJhbSBsYWJlbCBBIGxhYmVsIHN0cmluZy5cbiAgICAgKiBAcGFyYW0gZG9jdW1lbnRhdGlvbiBBIGRvYyBzdHJpbmcuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKGxhYmVsLCBkb2N1bWVudGF0aW9uKSB7XG4gICAgICAgIHJldHVybiBkb2N1bWVudGF0aW9uID8geyBsYWJlbCwgZG9jdW1lbnRhdGlvbiB9IDogeyBsYWJlbCB9O1xuICAgIH1cbiAgICBQYXJhbWV0ZXJJbmZvcm1hdGlvbi5jcmVhdGUgPSBjcmVhdGU7XG59KShQYXJhbWV0ZXJJbmZvcm1hdGlvbiB8fCAoUGFyYW1ldGVySW5mb3JtYXRpb24gPSB7fSkpO1xuLyoqXG4gKiBUaGUgU2lnbmF0dXJlSW5mb3JtYXRpb24gbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgU2lnbmF0dXJlSW5mb3JtYXRpb259IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIFNpZ25hdHVyZUluZm9ybWF0aW9uO1xuKGZ1bmN0aW9uIChTaWduYXR1cmVJbmZvcm1hdGlvbikge1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShsYWJlbCwgZG9jdW1lbnRhdGlvbiwgLi4ucGFyYW1ldGVycykge1xuICAgICAgICBsZXQgcmVzdWx0ID0geyBsYWJlbCB9O1xuICAgICAgICBpZiAoSXMuZGVmaW5lZChkb2N1bWVudGF0aW9uKSkge1xuICAgICAgICAgICAgcmVzdWx0LmRvY3VtZW50YXRpb24gPSBkb2N1bWVudGF0aW9uO1xuICAgICAgICB9XG4gICAgICAgIGlmIChJcy5kZWZpbmVkKHBhcmFtZXRlcnMpKSB7XG4gICAgICAgICAgICByZXN1bHQucGFyYW1ldGVycyA9IHBhcmFtZXRlcnM7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICByZXN1bHQucGFyYW1ldGVycyA9IFtdO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxuICAgIFNpZ25hdHVyZUluZm9ybWF0aW9uLmNyZWF0ZSA9IGNyZWF0ZTtcbn0pKFNpZ25hdHVyZUluZm9ybWF0aW9uIHx8IChTaWduYXR1cmVJbmZvcm1hdGlvbiA9IHt9KSk7XG4vKipcbiAqIEEgZG9jdW1lbnQgaGlnaGxpZ2h0IGtpbmQuXG4gKi9cbmV4cG9ydCB2YXIgRG9jdW1lbnRIaWdobGlnaHRLaW5kO1xuKGZ1bmN0aW9uIChEb2N1bWVudEhpZ2hsaWdodEtpbmQpIHtcbiAgICAvKipcbiAgICAgKiBBIHRleHR1YWwgb2NjdXJyZW5jZS5cbiAgICAgKi9cbiAgICBEb2N1bWVudEhpZ2hsaWdodEtpbmQuVGV4dCA9IDE7XG4gICAgLyoqXG4gICAgICogUmVhZC1hY2Nlc3Mgb2YgYSBzeW1ib2wsIGxpa2UgcmVhZGluZyBhIHZhcmlhYmxlLlxuICAgICAqL1xuICAgIERvY3VtZW50SGlnaGxpZ2h0S2luZC5SZWFkID0gMjtcbiAgICAvKipcbiAgICAgKiBXcml0ZS1hY2Nlc3Mgb2YgYSBzeW1ib2wsIGxpa2Ugd3JpdGluZyB0byBhIHZhcmlhYmxlLlxuICAgICAqL1xuICAgIERvY3VtZW50SGlnaGxpZ2h0S2luZC5Xcml0ZSA9IDM7XG59KShEb2N1bWVudEhpZ2hsaWdodEtpbmQgfHwgKERvY3VtZW50SGlnaGxpZ2h0S2luZCA9IHt9KSk7XG4vKipcbiAqIERvY3VtZW50SGlnaGxpZ2h0IG5hbWVzcGFjZSB0byBwcm92aWRlIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgRG9jdW1lbnRIaWdobGlnaHR9IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIERvY3VtZW50SGlnaGxpZ2h0O1xuKGZ1bmN0aW9uIChEb2N1bWVudEhpZ2hsaWdodCkge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZSBhIERvY3VtZW50SGlnaGxpZ2h0IG9iamVjdC5cbiAgICAgKiBAcGFyYW0gcmFuZ2UgVGhlIHJhbmdlIHRoZSBoaWdobGlnaHQgYXBwbGllcyB0by5cbiAgICAgKiBAcGFyYW0ga2luZCBUaGUgaGlnaGxpZ2h0IGtpbmRcbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUocmFuZ2UsIGtpbmQpIHtcbiAgICAgICAgbGV0IHJlc3VsdCA9IHsgcmFuZ2UgfTtcbiAgICAgICAgaWYgKElzLm51bWJlcihraW5kKSkge1xuICAgICAgICAgICAgcmVzdWx0LmtpbmQgPSBraW5kO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxuICAgIERvY3VtZW50SGlnaGxpZ2h0LmNyZWF0ZSA9IGNyZWF0ZTtcbn0pKERvY3VtZW50SGlnaGxpZ2h0IHx8IChEb2N1bWVudEhpZ2hsaWdodCA9IHt9KSk7XG4vKipcbiAqIEEgc3ltYm9sIGtpbmQuXG4gKi9cbmV4cG9ydCB2YXIgU3ltYm9sS2luZDtcbihmdW5jdGlvbiAoU3ltYm9sS2luZCkge1xuICAgIFN5bWJvbEtpbmQuRmlsZSA9IDE7XG4gICAgU3ltYm9sS2luZC5Nb2R1bGUgPSAyO1xuICAgIFN5bWJvbEtpbmQuTmFtZXNwYWNlID0gMztcbiAgICBTeW1ib2xLaW5kLlBhY2thZ2UgPSA0O1xuICAgIFN5bWJvbEtpbmQuQ2xhc3MgPSA1O1xuICAgIFN5bWJvbEtpbmQuTWV0aG9kID0gNjtcbiAgICBTeW1ib2xLaW5kLlByb3BlcnR5ID0gNztcbiAgICBTeW1ib2xLaW5kLkZpZWxkID0gODtcbiAgICBTeW1ib2xLaW5kLkNvbnN0cnVjdG9yID0gOTtcbiAgICBTeW1ib2xLaW5kLkVudW0gPSAxMDtcbiAgICBTeW1ib2xLaW5kLkludGVyZmFjZSA9IDExO1xuICAgIFN5bWJvbEtpbmQuRnVuY3Rpb24gPSAxMjtcbiAgICBTeW1ib2xLaW5kLlZhcmlhYmxlID0gMTM7XG4gICAgU3ltYm9sS2luZC5Db25zdGFudCA9IDE0O1xuICAgIFN5bWJvbEtpbmQuU3RyaW5nID0gMTU7XG4gICAgU3ltYm9sS2luZC5OdW1iZXIgPSAxNjtcbiAgICBTeW1ib2xLaW5kLkJvb2xlYW4gPSAxNztcbiAgICBTeW1ib2xLaW5kLkFycmF5ID0gMTg7XG4gICAgU3ltYm9sS2luZC5PYmplY3QgPSAxOTtcbiAgICBTeW1ib2xLaW5kLktleSA9IDIwO1xuICAgIFN5bWJvbEtpbmQuTnVsbCA9IDIxO1xuICAgIFN5bWJvbEtpbmQuRW51bU1lbWJlciA9IDIyO1xuICAgIFN5bWJvbEtpbmQuU3RydWN0ID0gMjM7XG4gICAgU3ltYm9sS2luZC5FdmVudCA9IDI0O1xuICAgIFN5bWJvbEtpbmQuT3BlcmF0b3IgPSAyNTtcbiAgICBTeW1ib2xLaW5kLlR5cGVQYXJhbWV0ZXIgPSAyNjtcbn0pKFN5bWJvbEtpbmQgfHwgKFN5bWJvbEtpbmQgPSB7fSkpO1xuLyoqXG4gKiBTeW1ib2wgdGFncyBhcmUgZXh0cmEgYW5ub3RhdGlvbnMgdGhhdCB0d2VhayB0aGUgcmVuZGVyaW5nIG9mIGEgc3ltYm9sLlxuICpcbiAqIEBzaW5jZSAzLjE2XG4gKi9cbmV4cG9ydCB2YXIgU3ltYm9sVGFnO1xuKGZ1bmN0aW9uIChTeW1ib2xUYWcpIHtcbiAgICAvKipcbiAgICAgKiBSZW5kZXIgYSBzeW1ib2wgYXMgb2Jzb2xldGUsIHVzdWFsbHkgdXNpbmcgYSBzdHJpa2Utb3V0LlxuICAgICAqL1xuICAgIFN5bWJvbFRhZy5EZXByZWNhdGVkID0gMTtcbn0pKFN5bWJvbFRhZyB8fCAoU3ltYm9sVGFnID0ge30pKTtcbmV4cG9ydCB2YXIgU3ltYm9sSW5mb3JtYXRpb247XG4oZnVuY3Rpb24gKFN5bWJvbEluZm9ybWF0aW9uKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBzeW1ib2wgaW5mb3JtYXRpb24gbGl0ZXJhbC5cbiAgICAgKlxuICAgICAqIEBwYXJhbSBuYW1lIFRoZSBuYW1lIG9mIHRoZSBzeW1ib2wuXG4gICAgICogQHBhcmFtIGtpbmQgVGhlIGtpbmQgb2YgdGhlIHN5bWJvbC5cbiAgICAgKiBAcGFyYW0gcmFuZ2UgVGhlIHJhbmdlIG9mIHRoZSBsb2NhdGlvbiBvZiB0aGUgc3ltYm9sLlxuICAgICAqIEBwYXJhbSB1cmkgVGhlIHJlc291cmNlIG9mIHRoZSBsb2NhdGlvbiBvZiBzeW1ib2wuXG4gICAgICogQHBhcmFtIGNvbnRhaW5lck5hbWUgVGhlIG5hbWUgb2YgdGhlIHN5bWJvbCBjb250YWluaW5nIHRoZSBzeW1ib2wuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKG5hbWUsIGtpbmQsIHJhbmdlLCB1cmksIGNvbnRhaW5lck5hbWUpIHtcbiAgICAgICAgbGV0IHJlc3VsdCA9IHtcbiAgICAgICAgICAgIG5hbWUsXG4gICAgICAgICAgICBraW5kLFxuICAgICAgICAgICAgbG9jYXRpb246IHsgdXJpLCByYW5nZSB9XG4gICAgICAgIH07XG4gICAgICAgIGlmIChjb250YWluZXJOYW1lKSB7XG4gICAgICAgICAgICByZXN1bHQuY29udGFpbmVyTmFtZSA9IGNvbnRhaW5lck5hbWU7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICB9XG4gICAgU3ltYm9sSW5mb3JtYXRpb24uY3JlYXRlID0gY3JlYXRlO1xufSkoU3ltYm9sSW5mb3JtYXRpb24gfHwgKFN5bWJvbEluZm9ybWF0aW9uID0ge30pKTtcbmV4cG9ydCB2YXIgV29ya3NwYWNlU3ltYm9sO1xuKGZ1bmN0aW9uIChXb3Jrc3BhY2VTeW1ib2wpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGUgYSBuZXcgd29ya3NwYWNlIHN5bWJvbC5cbiAgICAgKlxuICAgICAqIEBwYXJhbSBuYW1lIFRoZSBuYW1lIG9mIHRoZSBzeW1ib2wuXG4gICAgICogQHBhcmFtIGtpbmQgVGhlIGtpbmQgb2YgdGhlIHN5bWJvbC5cbiAgICAgKiBAcGFyYW0gdXJpIFRoZSByZXNvdXJjZSBvZiB0aGUgbG9jYXRpb24gb2YgdGhlIHN5bWJvbC5cbiAgICAgKiBAcGFyYW0gcmFuZ2UgQW4gb3B0aW9ucyByYW5nZSBvZiB0aGUgbG9jYXRpb24uXG4gICAgICogQHJldHVybnMgQSBXb3Jrc3BhY2VTeW1ib2wuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKG5hbWUsIGtpbmQsIHVyaSwgcmFuZ2UpIHtcbiAgICAgICAgcmV0dXJuIHJhbmdlICE9PSB1bmRlZmluZWRcbiAgICAgICAgICAgID8geyBuYW1lLCBraW5kLCBsb2NhdGlvbjogeyB1cmksIHJhbmdlIH0gfVxuICAgICAgICAgICAgOiB7IG5hbWUsIGtpbmQsIGxvY2F0aW9uOiB7IHVyaSB9IH07XG4gICAgfVxuICAgIFdvcmtzcGFjZVN5bWJvbC5jcmVhdGUgPSBjcmVhdGU7XG59KShXb3Jrc3BhY2VTeW1ib2wgfHwgKFdvcmtzcGFjZVN5bWJvbCA9IHt9KSk7XG5leHBvcnQgdmFyIERvY3VtZW50U3ltYm9sO1xuKGZ1bmN0aW9uIChEb2N1bWVudFN5bWJvbCkge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgc3ltYm9sIGluZm9ybWF0aW9uIGxpdGVyYWwuXG4gICAgICpcbiAgICAgKiBAcGFyYW0gbmFtZSBUaGUgbmFtZSBvZiB0aGUgc3ltYm9sLlxuICAgICAqIEBwYXJhbSBkZXRhaWwgVGhlIGRldGFpbCBvZiB0aGUgc3ltYm9sLlxuICAgICAqIEBwYXJhbSBraW5kIFRoZSBraW5kIG9mIHRoZSBzeW1ib2wuXG4gICAgICogQHBhcmFtIHJhbmdlIFRoZSByYW5nZSBvZiB0aGUgc3ltYm9sLlxuICAgICAqIEBwYXJhbSBzZWxlY3Rpb25SYW5nZSBUaGUgc2VsZWN0aW9uUmFuZ2Ugb2YgdGhlIHN5bWJvbC5cbiAgICAgKiBAcGFyYW0gY2hpbGRyZW4gQ2hpbGRyZW4gb2YgdGhlIHN5bWJvbC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUobmFtZSwgZGV0YWlsLCBraW5kLCByYW5nZSwgc2VsZWN0aW9uUmFuZ2UsIGNoaWxkcmVuKSB7XG4gICAgICAgIGxldCByZXN1bHQgPSB7XG4gICAgICAgICAgICBuYW1lLFxuICAgICAgICAgICAgZGV0YWlsLFxuICAgICAgICAgICAga2luZCxcbiAgICAgICAgICAgIHJhbmdlLFxuICAgICAgICAgICAgc2VsZWN0aW9uUmFuZ2VcbiAgICAgICAgfTtcbiAgICAgICAgaWYgKGNoaWxkcmVuICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHJlc3VsdC5jaGlsZHJlbiA9IGNoaWxkcmVuO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxuICAgIERvY3VtZW50U3ltYm9sLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIERvY3VtZW50U3ltYm9sfSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICYmXG4gICAgICAgICAgICBJcy5zdHJpbmcoY2FuZGlkYXRlLm5hbWUpICYmIElzLm51bWJlcihjYW5kaWRhdGUua2luZCkgJiZcbiAgICAgICAgICAgIFJhbmdlLmlzKGNhbmRpZGF0ZS5yYW5nZSkgJiYgUmFuZ2UuaXMoY2FuZGlkYXRlLnNlbGVjdGlvblJhbmdlKSAmJlxuICAgICAgICAgICAgKGNhbmRpZGF0ZS5kZXRhaWwgPT09IHVuZGVmaW5lZCB8fCBJcy5zdHJpbmcoY2FuZGlkYXRlLmRldGFpbCkpICYmXG4gICAgICAgICAgICAoY2FuZGlkYXRlLmRlcHJlY2F0ZWQgPT09IHVuZGVmaW5lZCB8fCBJcy5ib29sZWFuKGNhbmRpZGF0ZS5kZXByZWNhdGVkKSkgJiZcbiAgICAgICAgICAgIChjYW5kaWRhdGUuY2hpbGRyZW4gPT09IHVuZGVmaW5lZCB8fCBBcnJheS5pc0FycmF5KGNhbmRpZGF0ZS5jaGlsZHJlbikpICYmXG4gICAgICAgICAgICAoY2FuZGlkYXRlLnRhZ3MgPT09IHVuZGVmaW5lZCB8fCBBcnJheS5pc0FycmF5KGNhbmRpZGF0ZS50YWdzKSk7XG4gICAgfVxuICAgIERvY3VtZW50U3ltYm9sLmlzID0gaXM7XG59KShEb2N1bWVudFN5bWJvbCB8fCAoRG9jdW1lbnRTeW1ib2wgPSB7fSkpO1xuLyoqXG4gKiBBIHNldCBvZiBwcmVkZWZpbmVkIGNvZGUgYWN0aW9uIGtpbmRzXG4gKi9cbmV4cG9ydCB2YXIgQ29kZUFjdGlvbktpbmQ7XG4oZnVuY3Rpb24gKENvZGVBY3Rpb25LaW5kKSB7XG4gICAgLyoqXG4gICAgICogRW1wdHkga2luZC5cbiAgICAgKi9cbiAgICBDb2RlQWN0aW9uS2luZC5FbXB0eSA9ICcnO1xuICAgIC8qKlxuICAgICAqIEJhc2Uga2luZCBmb3IgcXVpY2tmaXggYWN0aW9uczogJ3F1aWNrZml4J1xuICAgICAqL1xuICAgIENvZGVBY3Rpb25LaW5kLlF1aWNrRml4ID0gJ3F1aWNrZml4JztcbiAgICAvKipcbiAgICAgKiBCYXNlIGtpbmQgZm9yIHJlZmFjdG9yaW5nIGFjdGlvbnM6ICdyZWZhY3RvcidcbiAgICAgKi9cbiAgICBDb2RlQWN0aW9uS2luZC5SZWZhY3RvciA9ICdyZWZhY3Rvcic7XG4gICAgLyoqXG4gICAgICogQmFzZSBraW5kIGZvciByZWZhY3RvcmluZyBleHRyYWN0aW9uIGFjdGlvbnM6ICdyZWZhY3Rvci5leHRyYWN0J1xuICAgICAqXG4gICAgICogRXhhbXBsZSBleHRyYWN0IGFjdGlvbnM6XG4gICAgICpcbiAgICAgKiAtIEV4dHJhY3QgbWV0aG9kXG4gICAgICogLSBFeHRyYWN0IGZ1bmN0aW9uXG4gICAgICogLSBFeHRyYWN0IHZhcmlhYmxlXG4gICAgICogLSBFeHRyYWN0IGludGVyZmFjZSBmcm9tIGNsYXNzXG4gICAgICogLSAuLi5cbiAgICAgKi9cbiAgICBDb2RlQWN0aW9uS2luZC5SZWZhY3RvckV4dHJhY3QgPSAncmVmYWN0b3IuZXh0cmFjdCc7XG4gICAgLyoqXG4gICAgICogQmFzZSBraW5kIGZvciByZWZhY3RvcmluZyBpbmxpbmUgYWN0aW9uczogJ3JlZmFjdG9yLmlubGluZSdcbiAgICAgKlxuICAgICAqIEV4YW1wbGUgaW5saW5lIGFjdGlvbnM6XG4gICAgICpcbiAgICAgKiAtIElubGluZSBmdW5jdGlvblxuICAgICAqIC0gSW5saW5lIHZhcmlhYmxlXG4gICAgICogLSBJbmxpbmUgY29uc3RhbnRcbiAgICAgKiAtIC4uLlxuICAgICAqL1xuICAgIENvZGVBY3Rpb25LaW5kLlJlZmFjdG9ySW5saW5lID0gJ3JlZmFjdG9yLmlubGluZSc7XG4gICAgLyoqXG4gICAgICogQmFzZSBraW5kIGZvciByZWZhY3RvcmluZyByZXdyaXRlIGFjdGlvbnM6ICdyZWZhY3Rvci5yZXdyaXRlJ1xuICAgICAqXG4gICAgICogRXhhbXBsZSByZXdyaXRlIGFjdGlvbnM6XG4gICAgICpcbiAgICAgKiAtIENvbnZlcnQgSmF2YVNjcmlwdCBmdW5jdGlvbiB0byBjbGFzc1xuICAgICAqIC0gQWRkIG9yIHJlbW92ZSBwYXJhbWV0ZXJcbiAgICAgKiAtIEVuY2Fwc3VsYXRlIGZpZWxkXG4gICAgICogLSBNYWtlIG1ldGhvZCBzdGF0aWNcbiAgICAgKiAtIE1vdmUgbWV0aG9kIHRvIGJhc2UgY2xhc3NcbiAgICAgKiAtIC4uLlxuICAgICAqL1xuICAgIENvZGVBY3Rpb25LaW5kLlJlZmFjdG9yUmV3cml0ZSA9ICdyZWZhY3Rvci5yZXdyaXRlJztcbiAgICAvKipcbiAgICAgKiBCYXNlIGtpbmQgZm9yIHNvdXJjZSBhY3Rpb25zOiBgc291cmNlYFxuICAgICAqXG4gICAgICogU291cmNlIGNvZGUgYWN0aW9ucyBhcHBseSB0byB0aGUgZW50aXJlIGZpbGUuXG4gICAgICovXG4gICAgQ29kZUFjdGlvbktpbmQuU291cmNlID0gJ3NvdXJjZSc7XG4gICAgLyoqXG4gICAgICogQmFzZSBraW5kIGZvciBhbiBvcmdhbml6ZSBpbXBvcnRzIHNvdXJjZSBhY3Rpb246IGBzb3VyY2Uub3JnYW5pemVJbXBvcnRzYFxuICAgICAqL1xuICAgIENvZGVBY3Rpb25LaW5kLlNvdXJjZU9yZ2FuaXplSW1wb3J0cyA9ICdzb3VyY2Uub3JnYW5pemVJbXBvcnRzJztcbiAgICAvKipcbiAgICAgKiBCYXNlIGtpbmQgZm9yIGF1dG8tZml4IHNvdXJjZSBhY3Rpb25zOiBgc291cmNlLmZpeEFsbGAuXG4gICAgICpcbiAgICAgKiBGaXggYWxsIGFjdGlvbnMgYXV0b21hdGljYWxseSBmaXggZXJyb3JzIHRoYXQgaGF2ZSBhIGNsZWFyIGZpeCB0aGF0IGRvIG5vdCByZXF1aXJlIHVzZXIgaW5wdXQuXG4gICAgICogVGhleSBzaG91bGQgbm90IHN1cHByZXNzIGVycm9ycyBvciBwZXJmb3JtIHVuc2FmZSBmaXhlcyBzdWNoIGFzIGdlbmVyYXRpbmcgbmV3IHR5cGVzIG9yIGNsYXNzZXMuXG4gICAgICpcbiAgICAgKiBAc2luY2UgMy4xNS4wXG4gICAgICovXG4gICAgQ29kZUFjdGlvbktpbmQuU291cmNlRml4QWxsID0gJ3NvdXJjZS5maXhBbGwnO1xufSkoQ29kZUFjdGlvbktpbmQgfHwgKENvZGVBY3Rpb25LaW5kID0ge30pKTtcbi8qKlxuICogVGhlIHJlYXNvbiB3aHkgY29kZSBhY3Rpb25zIHdlcmUgcmVxdWVzdGVkLlxuICpcbiAqIEBzaW5jZSAzLjE3LjBcbiAqL1xuZXhwb3J0IHZhciBDb2RlQWN0aW9uVHJpZ2dlcktpbmQ7XG4oZnVuY3Rpb24gKENvZGVBY3Rpb25UcmlnZ2VyS2luZCkge1xuICAgIC8qKlxuICAgICAqIENvZGUgYWN0aW9ucyB3ZXJlIGV4cGxpY2l0bHkgcmVxdWVzdGVkIGJ5IHRoZSB1c2VyIG9yIGJ5IGFuIGV4dGVuc2lvbi5cbiAgICAgKi9cbiAgICBDb2RlQWN0aW9uVHJpZ2dlcktpbmQuSW52b2tlZCA9IDE7XG4gICAgLyoqXG4gICAgICogQ29kZSBhY3Rpb25zIHdlcmUgcmVxdWVzdGVkIGF1dG9tYXRpY2FsbHkuXG4gICAgICpcbiAgICAgKiBUaGlzIHR5cGljYWxseSBoYXBwZW5zIHdoZW4gY3VycmVudCBzZWxlY3Rpb24gaW4gYSBmaWxlIGNoYW5nZXMsIGJ1dCBjYW5cbiAgICAgKiBhbHNvIGJlIHRyaWdnZXJlZCB3aGVuIGZpbGUgY29udGVudCBjaGFuZ2VzLlxuICAgICAqL1xuICAgIENvZGVBY3Rpb25UcmlnZ2VyS2luZC5BdXRvbWF0aWMgPSAyO1xufSkoQ29kZUFjdGlvblRyaWdnZXJLaW5kIHx8IChDb2RlQWN0aW9uVHJpZ2dlcktpbmQgPSB7fSkpO1xuLyoqXG4gKiBUaGUgQ29kZUFjdGlvbkNvbnRleHQgbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgQ29kZUFjdGlvbkNvbnRleHR9IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIENvZGVBY3Rpb25Db250ZXh0O1xuKGZ1bmN0aW9uIChDb2RlQWN0aW9uQ29udGV4dCkge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgQ29kZUFjdGlvbkNvbnRleHQgbGl0ZXJhbC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUoZGlhZ25vc3RpY3MsIG9ubHksIHRyaWdnZXJLaW5kKSB7XG4gICAgICAgIGxldCByZXN1bHQgPSB7IGRpYWdub3N0aWNzIH07XG4gICAgICAgIGlmIChvbmx5ICE9PSB1bmRlZmluZWQgJiYgb25seSAhPT0gbnVsbCkge1xuICAgICAgICAgICAgcmVzdWx0Lm9ubHkgPSBvbmx5O1xuICAgICAgICB9XG4gICAgICAgIGlmICh0cmlnZ2VyS2luZCAhPT0gdW5kZWZpbmVkICYmIHRyaWdnZXJLaW5kICE9PSBudWxsKSB7XG4gICAgICAgICAgICByZXN1bHQudHJpZ2dlcktpbmQgPSB0cmlnZ2VyS2luZDtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cbiAgICBDb2RlQWN0aW9uQ29udGV4dC5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBDb2RlQWN0aW9uQ29udGV4dH0gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLmRlZmluZWQoY2FuZGlkYXRlKSAmJiBJcy50eXBlZEFycmF5KGNhbmRpZGF0ZS5kaWFnbm9zdGljcywgRGlhZ25vc3RpYy5pcylcbiAgICAgICAgICAgICYmIChjYW5kaWRhdGUub25seSA9PT0gdW5kZWZpbmVkIHx8IElzLnR5cGVkQXJyYXkoY2FuZGlkYXRlLm9ubHksIElzLnN0cmluZykpXG4gICAgICAgICAgICAmJiAoY2FuZGlkYXRlLnRyaWdnZXJLaW5kID09PSB1bmRlZmluZWQgfHwgY2FuZGlkYXRlLnRyaWdnZXJLaW5kID09PSBDb2RlQWN0aW9uVHJpZ2dlcktpbmQuSW52b2tlZCB8fCBjYW5kaWRhdGUudHJpZ2dlcktpbmQgPT09IENvZGVBY3Rpb25UcmlnZ2VyS2luZC5BdXRvbWF0aWMpO1xuICAgIH1cbiAgICBDb2RlQWN0aW9uQ29udGV4dC5pcyA9IGlzO1xufSkoQ29kZUFjdGlvbkNvbnRleHQgfHwgKENvZGVBY3Rpb25Db250ZXh0ID0ge30pKTtcbmV4cG9ydCB2YXIgQ29kZUFjdGlvbjtcbihmdW5jdGlvbiAoQ29kZUFjdGlvbikge1xuICAgIGZ1bmN0aW9uIGNyZWF0ZSh0aXRsZSwga2luZE9yQ29tbWFuZE9yRWRpdCwga2luZCkge1xuICAgICAgICBsZXQgcmVzdWx0ID0geyB0aXRsZSB9O1xuICAgICAgICBsZXQgY2hlY2tLaW5kID0gdHJ1ZTtcbiAgICAgICAgaWYgKHR5cGVvZiBraW5kT3JDb21tYW5kT3JFZGl0ID09PSAnc3RyaW5nJykge1xuICAgICAgICAgICAgY2hlY2tLaW5kID0gZmFsc2U7XG4gICAgICAgICAgICByZXN1bHQua2luZCA9IGtpbmRPckNvbW1hbmRPckVkaXQ7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSBpZiAoQ29tbWFuZC5pcyhraW5kT3JDb21tYW5kT3JFZGl0KSkge1xuICAgICAgICAgICAgcmVzdWx0LmNvbW1hbmQgPSBraW5kT3JDb21tYW5kT3JFZGl0O1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgcmVzdWx0LmVkaXQgPSBraW5kT3JDb21tYW5kT3JFZGl0O1xuICAgICAgICB9XG4gICAgICAgIGlmIChjaGVja0tpbmQgJiYga2luZCAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXN1bHQua2luZCA9IGtpbmQ7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICB9XG4gICAgQ29kZUFjdGlvbi5jcmVhdGUgPSBjcmVhdGU7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICYmIElzLnN0cmluZyhjYW5kaWRhdGUudGl0bGUpICYmXG4gICAgICAgICAgICAoY2FuZGlkYXRlLmRpYWdub3N0aWNzID09PSB1bmRlZmluZWQgfHwgSXMudHlwZWRBcnJheShjYW5kaWRhdGUuZGlhZ25vc3RpY3MsIERpYWdub3N0aWMuaXMpKSAmJlxuICAgICAgICAgICAgKGNhbmRpZGF0ZS5raW5kID09PSB1bmRlZmluZWQgfHwgSXMuc3RyaW5nKGNhbmRpZGF0ZS5raW5kKSkgJiZcbiAgICAgICAgICAgIChjYW5kaWRhdGUuZWRpdCAhPT0gdW5kZWZpbmVkIHx8IGNhbmRpZGF0ZS5jb21tYW5kICE9PSB1bmRlZmluZWQpICYmXG4gICAgICAgICAgICAoY2FuZGlkYXRlLmNvbW1hbmQgPT09IHVuZGVmaW5lZCB8fCBDb21tYW5kLmlzKGNhbmRpZGF0ZS5jb21tYW5kKSkgJiZcbiAgICAgICAgICAgIChjYW5kaWRhdGUuaXNQcmVmZXJyZWQgPT09IHVuZGVmaW5lZCB8fCBJcy5ib29sZWFuKGNhbmRpZGF0ZS5pc1ByZWZlcnJlZCkpICYmXG4gICAgICAgICAgICAoY2FuZGlkYXRlLmVkaXQgPT09IHVuZGVmaW5lZCB8fCBXb3Jrc3BhY2VFZGl0LmlzKGNhbmRpZGF0ZS5lZGl0KSk7XG4gICAgfVxuICAgIENvZGVBY3Rpb24uaXMgPSBpcztcbn0pKENvZGVBY3Rpb24gfHwgKENvZGVBY3Rpb24gPSB7fSkpO1xuLyoqXG4gKiBUaGUgQ29kZUxlbnMgbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgQ29kZUxlbnN9IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIENvZGVMZW5zO1xuKGZ1bmN0aW9uIChDb2RlTGVucykge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgQ29kZUxlbnMgbGl0ZXJhbC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUocmFuZ2UsIGRhdGEpIHtcbiAgICAgICAgbGV0IHJlc3VsdCA9IHsgcmFuZ2UgfTtcbiAgICAgICAgaWYgKElzLmRlZmluZWQoZGF0YSkpIHtcbiAgICAgICAgICAgIHJlc3VsdC5kYXRhID0gZGF0YTtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cbiAgICBDb2RlTGVucy5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBDb2RlTGVuc30gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLmRlZmluZWQoY2FuZGlkYXRlKSAmJiBSYW5nZS5pcyhjYW5kaWRhdGUucmFuZ2UpICYmIChJcy51bmRlZmluZWQoY2FuZGlkYXRlLmNvbW1hbmQpIHx8IENvbW1hbmQuaXMoY2FuZGlkYXRlLmNvbW1hbmQpKTtcbiAgICB9XG4gICAgQ29kZUxlbnMuaXMgPSBpcztcbn0pKENvZGVMZW5zIHx8IChDb2RlTGVucyA9IHt9KSk7XG4vKipcbiAqIFRoZSBGb3JtYXR0aW5nT3B0aW9ucyBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBGb3JtYXR0aW5nT3B0aW9uc30gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgRm9ybWF0dGluZ09wdGlvbnM7XG4oZnVuY3Rpb24gKEZvcm1hdHRpbmdPcHRpb25zKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBGb3JtYXR0aW5nT3B0aW9ucyBsaXRlcmFsLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZSh0YWJTaXplLCBpbnNlcnRTcGFjZXMpIHtcbiAgICAgICAgcmV0dXJuIHsgdGFiU2l6ZSwgaW5zZXJ0U3BhY2VzIH07XG4gICAgfVxuICAgIEZvcm1hdHRpbmdPcHRpb25zLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIEZvcm1hdHRpbmdPcHRpb25zfSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMuZGVmaW5lZChjYW5kaWRhdGUpICYmIElzLnVpbnRlZ2VyKGNhbmRpZGF0ZS50YWJTaXplKSAmJiBJcy5ib29sZWFuKGNhbmRpZGF0ZS5pbnNlcnRTcGFjZXMpO1xuICAgIH1cbiAgICBGb3JtYXR0aW5nT3B0aW9ucy5pcyA9IGlzO1xufSkoRm9ybWF0dGluZ09wdGlvbnMgfHwgKEZvcm1hdHRpbmdPcHRpb25zID0ge30pKTtcbi8qKlxuICogVGhlIERvY3VtZW50TGluayBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBEb2N1bWVudExpbmt9IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIERvY3VtZW50TGluaztcbihmdW5jdGlvbiAoRG9jdW1lbnRMaW5rKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBEb2N1bWVudExpbmsgbGl0ZXJhbC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUocmFuZ2UsIHRhcmdldCwgZGF0YSkge1xuICAgICAgICByZXR1cm4geyByYW5nZSwgdGFyZ2V0LCBkYXRhIH07XG4gICAgfVxuICAgIERvY3VtZW50TGluay5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBEb2N1bWVudExpbmt9IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5kZWZpbmVkKGNhbmRpZGF0ZSkgJiYgUmFuZ2UuaXMoY2FuZGlkYXRlLnJhbmdlKSAmJiAoSXMudW5kZWZpbmVkKGNhbmRpZGF0ZS50YXJnZXQpIHx8IElzLnN0cmluZyhjYW5kaWRhdGUudGFyZ2V0KSk7XG4gICAgfVxuICAgIERvY3VtZW50TGluay5pcyA9IGlzO1xufSkoRG9jdW1lbnRMaW5rIHx8IChEb2N1bWVudExpbmsgPSB7fSkpO1xuLyoqXG4gKiBUaGUgU2VsZWN0aW9uUmFuZ2UgbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbiB0byB3b3JrIHdpdGhcbiAqIFNlbGVjdGlvblJhbmdlIGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIFNlbGVjdGlvblJhbmdlO1xuKGZ1bmN0aW9uIChTZWxlY3Rpb25SYW5nZSkge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgU2VsZWN0aW9uUmFuZ2VcbiAgICAgKiBAcGFyYW0gcmFuZ2UgdGhlIHJhbmdlLlxuICAgICAqIEBwYXJhbSBwYXJlbnQgYW4gb3B0aW9uYWwgcGFyZW50LlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShyYW5nZSwgcGFyZW50KSB7XG4gICAgICAgIHJldHVybiB7IHJhbmdlLCBwYXJlbnQgfTtcbiAgICB9XG4gICAgU2VsZWN0aW9uUmFuZ2UuY3JlYXRlID0gY3JlYXRlO1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLm9iamVjdExpdGVyYWwoY2FuZGlkYXRlKSAmJiBSYW5nZS5pcyhjYW5kaWRhdGUucmFuZ2UpICYmIChjYW5kaWRhdGUucGFyZW50ID09PSB1bmRlZmluZWQgfHwgU2VsZWN0aW9uUmFuZ2UuaXMoY2FuZGlkYXRlLnBhcmVudCkpO1xuICAgIH1cbiAgICBTZWxlY3Rpb25SYW5nZS5pcyA9IGlzO1xufSkoU2VsZWN0aW9uUmFuZ2UgfHwgKFNlbGVjdGlvblJhbmdlID0ge30pKTtcbi8qKlxuICogQSBzZXQgb2YgcHJlZGVmaW5lZCB0b2tlbiB0eXBlcy4gVGhpcyBzZXQgaXMgbm90IGZpeGVkXG4gKiBhbiBjbGllbnRzIGNhbiBzcGVjaWZ5IGFkZGl0aW9uYWwgdG9rZW4gdHlwZXMgdmlhIHRoZVxuICogY29ycmVzcG9uZGluZyBjbGllbnQgY2FwYWJpbGl0aWVzLlxuICpcbiAqIEBzaW5jZSAzLjE2LjBcbiAqL1xuZXhwb3J0IHZhciBTZW1hbnRpY1Rva2VuVHlwZXM7XG4oZnVuY3Rpb24gKFNlbWFudGljVG9rZW5UeXBlcykge1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcIm5hbWVzcGFjZVwiXSA9IFwibmFtZXNwYWNlXCI7XG4gICAgLyoqXG4gICAgICogUmVwcmVzZW50cyBhIGdlbmVyaWMgdHlwZS4gQWN0cyBhcyBhIGZhbGxiYWNrIGZvciB0eXBlcyB3aGljaCBjYW4ndCBiZSBtYXBwZWQgdG9cbiAgICAgKiBhIHNwZWNpZmljIHR5cGUgbGlrZSBjbGFzcyBvciBlbnVtLlxuICAgICAqL1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcInR5cGVcIl0gPSBcInR5cGVcIjtcbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJjbGFzc1wiXSA9IFwiY2xhc3NcIjtcbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJlbnVtXCJdID0gXCJlbnVtXCI7XG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1wiaW50ZXJmYWNlXCJdID0gXCJpbnRlcmZhY2VcIjtcbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJzdHJ1Y3RcIl0gPSBcInN0cnVjdFwiO1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcInR5cGVQYXJhbWV0ZXJcIl0gPSBcInR5cGVQYXJhbWV0ZXJcIjtcbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJwYXJhbWV0ZXJcIl0gPSBcInBhcmFtZXRlclwiO1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcInZhcmlhYmxlXCJdID0gXCJ2YXJpYWJsZVwiO1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcInByb3BlcnR5XCJdID0gXCJwcm9wZXJ0eVwiO1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcImVudW1NZW1iZXJcIl0gPSBcImVudW1NZW1iZXJcIjtcbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJldmVudFwiXSA9IFwiZXZlbnRcIjtcbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJmdW5jdGlvblwiXSA9IFwiZnVuY3Rpb25cIjtcbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJtZXRob2RcIl0gPSBcIm1ldGhvZFwiO1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcIm1hY3JvXCJdID0gXCJtYWNyb1wiO1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcImtleXdvcmRcIl0gPSBcImtleXdvcmRcIjtcbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJtb2RpZmllclwiXSA9IFwibW9kaWZpZXJcIjtcbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJjb21tZW50XCJdID0gXCJjb21tZW50XCI7XG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1wic3RyaW5nXCJdID0gXCJzdHJpbmdcIjtcbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJudW1iZXJcIl0gPSBcIm51bWJlclwiO1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcInJlZ2V4cFwiXSA9IFwicmVnZXhwXCI7XG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1wib3BlcmF0b3JcIl0gPSBcIm9wZXJhdG9yXCI7XG4gICAgLyoqXG4gICAgICogQHNpbmNlIDMuMTcuMFxuICAgICAqL1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcImRlY29yYXRvclwiXSA9IFwiZGVjb3JhdG9yXCI7XG59KShTZW1hbnRpY1Rva2VuVHlwZXMgfHwgKFNlbWFudGljVG9rZW5UeXBlcyA9IHt9KSk7XG4vKipcbiAqIEEgc2V0IG9mIHByZWRlZmluZWQgdG9rZW4gbW9kaWZpZXJzLiBUaGlzIHNldCBpcyBub3QgZml4ZWRcbiAqIGFuIGNsaWVudHMgY2FuIHNwZWNpZnkgYWRkaXRpb25hbCB0b2tlbiB0eXBlcyB2aWEgdGhlXG4gKiBjb3JyZXNwb25kaW5nIGNsaWVudCBjYXBhYmlsaXRpZXMuXG4gKlxuICogQHNpbmNlIDMuMTYuMFxuICovXG5leHBvcnQgdmFyIFNlbWFudGljVG9rZW5Nb2RpZmllcnM7XG4oZnVuY3Rpb24gKFNlbWFudGljVG9rZW5Nb2RpZmllcnMpIHtcbiAgICBTZW1hbnRpY1Rva2VuTW9kaWZpZXJzW1wiZGVjbGFyYXRpb25cIl0gPSBcImRlY2xhcmF0aW9uXCI7XG4gICAgU2VtYW50aWNUb2tlbk1vZGlmaWVyc1tcImRlZmluaXRpb25cIl0gPSBcImRlZmluaXRpb25cIjtcbiAgICBTZW1hbnRpY1Rva2VuTW9kaWZpZXJzW1wicmVhZG9ubHlcIl0gPSBcInJlYWRvbmx5XCI7XG4gICAgU2VtYW50aWNUb2tlbk1vZGlmaWVyc1tcInN0YXRpY1wiXSA9IFwic3RhdGljXCI7XG4gICAgU2VtYW50aWNUb2tlbk1vZGlmaWVyc1tcImRlcHJlY2F0ZWRcIl0gPSBcImRlcHJlY2F0ZWRcIjtcbiAgICBTZW1hbnRpY1Rva2VuTW9kaWZpZXJzW1wiYWJzdHJhY3RcIl0gPSBcImFic3RyYWN0XCI7XG4gICAgU2VtYW50aWNUb2tlbk1vZGlmaWVyc1tcImFzeW5jXCJdID0gXCJhc3luY1wiO1xuICAgIFNlbWFudGljVG9rZW5Nb2RpZmllcnNbXCJtb2RpZmljYXRpb25cIl0gPSBcIm1vZGlmaWNhdGlvblwiO1xuICAgIFNlbWFudGljVG9rZW5Nb2RpZmllcnNbXCJkb2N1bWVudGF0aW9uXCJdID0gXCJkb2N1bWVudGF0aW9uXCI7XG4gICAgU2VtYW50aWNUb2tlbk1vZGlmaWVyc1tcImRlZmF1bHRMaWJyYXJ5XCJdID0gXCJkZWZhdWx0TGlicmFyeVwiO1xufSkoU2VtYW50aWNUb2tlbk1vZGlmaWVycyB8fCAoU2VtYW50aWNUb2tlbk1vZGlmaWVycyA9IHt9KSk7XG4vKipcbiAqIEBzaW5jZSAzLjE2LjBcbiAqL1xuZXhwb3J0IHZhciBTZW1hbnRpY1Rva2VucztcbihmdW5jdGlvbiAoU2VtYW50aWNUb2tlbnMpIHtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLm9iamVjdExpdGVyYWwoY2FuZGlkYXRlKSAmJiAoY2FuZGlkYXRlLnJlc3VsdElkID09PSB1bmRlZmluZWQgfHwgdHlwZW9mIGNhbmRpZGF0ZS5yZXN1bHRJZCA9PT0gJ3N0cmluZycpICYmXG4gICAgICAgICAgICBBcnJheS5pc0FycmF5KGNhbmRpZGF0ZS5kYXRhKSAmJiAoY2FuZGlkYXRlLmRhdGEubGVuZ3RoID09PSAwIHx8IHR5cGVvZiBjYW5kaWRhdGUuZGF0YVswXSA9PT0gJ251bWJlcicpO1xuICAgIH1cbiAgICBTZW1hbnRpY1Rva2Vucy5pcyA9IGlzO1xufSkoU2VtYW50aWNUb2tlbnMgfHwgKFNlbWFudGljVG9rZW5zID0ge30pKTtcbi8qKlxuICogVGhlIElubGluZVZhbHVlVGV4dCBuYW1lc3BhY2UgcHJvdmlkZXMgZnVuY3Rpb25zIHRvIGRlYWwgd2l0aCBJbmxpbmVWYWx1ZVRleHRzLlxuICpcbiAqIEBzaW5jZSAzLjE3LjBcbiAqL1xuZXhwb3J0IHZhciBJbmxpbmVWYWx1ZVRleHQ7XG4oZnVuY3Rpb24gKElubGluZVZhbHVlVGV4dCkge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgSW5saW5lVmFsdWVUZXh0IGxpdGVyYWwuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKHJhbmdlLCB0ZXh0KSB7XG4gICAgICAgIHJldHVybiB7IHJhbmdlLCB0ZXh0IH07XG4gICAgfVxuICAgIElubGluZVZhbHVlVGV4dC5jcmVhdGUgPSBjcmVhdGU7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBjYW5kaWRhdGUgIT09IHVuZGVmaW5lZCAmJiBjYW5kaWRhdGUgIT09IG51bGwgJiYgUmFuZ2UuaXMoY2FuZGlkYXRlLnJhbmdlKSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLnRleHQpO1xuICAgIH1cbiAgICBJbmxpbmVWYWx1ZVRleHQuaXMgPSBpcztcbn0pKElubGluZVZhbHVlVGV4dCB8fCAoSW5saW5lVmFsdWVUZXh0ID0ge30pKTtcbi8qKlxuICogVGhlIElubGluZVZhbHVlVmFyaWFibGVMb29rdXAgbmFtZXNwYWNlIHByb3ZpZGVzIGZ1bmN0aW9ucyB0byBkZWFsIHdpdGggSW5saW5lVmFsdWVWYXJpYWJsZUxvb2t1cHMuXG4gKlxuICogQHNpbmNlIDMuMTcuMFxuICovXG5leHBvcnQgdmFyIElubGluZVZhbHVlVmFyaWFibGVMb29rdXA7XG4oZnVuY3Rpb24gKElubGluZVZhbHVlVmFyaWFibGVMb29rdXApIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IElubGluZVZhbHVlVGV4dCBsaXRlcmFsLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShyYW5nZSwgdmFyaWFibGVOYW1lLCBjYXNlU2Vuc2l0aXZlTG9va3VwKSB7XG4gICAgICAgIHJldHVybiB7IHJhbmdlLCB2YXJpYWJsZU5hbWUsIGNhc2VTZW5zaXRpdmVMb29rdXAgfTtcbiAgICB9XG4gICAgSW5saW5lVmFsdWVWYXJpYWJsZUxvb2t1cC5jcmVhdGUgPSBjcmVhdGU7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBjYW5kaWRhdGUgIT09IHVuZGVmaW5lZCAmJiBjYW5kaWRhdGUgIT09IG51bGwgJiYgUmFuZ2UuaXMoY2FuZGlkYXRlLnJhbmdlKSAmJiBJcy5ib29sZWFuKGNhbmRpZGF0ZS5jYXNlU2Vuc2l0aXZlTG9va3VwKVxuICAgICAgICAgICAgJiYgKElzLnN0cmluZyhjYW5kaWRhdGUudmFyaWFibGVOYW1lKSB8fCBjYW5kaWRhdGUudmFyaWFibGVOYW1lID09PSB1bmRlZmluZWQpO1xuICAgIH1cbiAgICBJbmxpbmVWYWx1ZVZhcmlhYmxlTG9va3VwLmlzID0gaXM7XG59KShJbmxpbmVWYWx1ZVZhcmlhYmxlTG9va3VwIHx8IChJbmxpbmVWYWx1ZVZhcmlhYmxlTG9va3VwID0ge30pKTtcbi8qKlxuICogVGhlIElubGluZVZhbHVlRXZhbHVhdGFibGVFeHByZXNzaW9uIG5hbWVzcGFjZSBwcm92aWRlcyBmdW5jdGlvbnMgdG8gZGVhbCB3aXRoIElubGluZVZhbHVlRXZhbHVhdGFibGVFeHByZXNzaW9uLlxuICpcbiAqIEBzaW5jZSAzLjE3LjBcbiAqL1xuZXhwb3J0IHZhciBJbmxpbmVWYWx1ZUV2YWx1YXRhYmxlRXhwcmVzc2lvbjtcbihmdW5jdGlvbiAoSW5saW5lVmFsdWVFdmFsdWF0YWJsZUV4cHJlc3Npb24pIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IElubGluZVZhbHVlRXZhbHVhdGFibGVFeHByZXNzaW9uIGxpdGVyYWwuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKHJhbmdlLCBleHByZXNzaW9uKSB7XG4gICAgICAgIHJldHVybiB7IHJhbmdlLCBleHByZXNzaW9uIH07XG4gICAgfVxuICAgIElubGluZVZhbHVlRXZhbHVhdGFibGVFeHByZXNzaW9uLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAhPT0gdW5kZWZpbmVkICYmIGNhbmRpZGF0ZSAhPT0gbnVsbCAmJiBSYW5nZS5pcyhjYW5kaWRhdGUucmFuZ2UpXG4gICAgICAgICAgICAmJiAoSXMuc3RyaW5nKGNhbmRpZGF0ZS5leHByZXNzaW9uKSB8fCBjYW5kaWRhdGUuZXhwcmVzc2lvbiA9PT0gdW5kZWZpbmVkKTtcbiAgICB9XG4gICAgSW5saW5lVmFsdWVFdmFsdWF0YWJsZUV4cHJlc3Npb24uaXMgPSBpcztcbn0pKElubGluZVZhbHVlRXZhbHVhdGFibGVFeHByZXNzaW9uIHx8IChJbmxpbmVWYWx1ZUV2YWx1YXRhYmxlRXhwcmVzc2lvbiA9IHt9KSk7XG4vKipcbiAqIFRoZSBJbmxpbmVWYWx1ZUNvbnRleHQgbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgSW5saW5lVmFsdWVDb250ZXh0fSBsaXRlcmFscy5cbiAqXG4gKiBAc2luY2UgMy4xNy4wXG4gKi9cbmV4cG9ydCB2YXIgSW5saW5lVmFsdWVDb250ZXh0O1xuKGZ1bmN0aW9uIChJbmxpbmVWYWx1ZUNvbnRleHQpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IElubGluZVZhbHVlQ29udGV4dCBsaXRlcmFsLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShmcmFtZUlkLCBzdG9wcGVkTG9jYXRpb24pIHtcbiAgICAgICAgcmV0dXJuIHsgZnJhbWVJZCwgc3RvcHBlZExvY2F0aW9uIH07XG4gICAgfVxuICAgIElubGluZVZhbHVlQ29udGV4dC5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBJbmxpbmVWYWx1ZUNvbnRleHR9IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLmRlZmluZWQoY2FuZGlkYXRlKSAmJiBSYW5nZS5pcyh2YWx1ZS5zdG9wcGVkTG9jYXRpb24pO1xuICAgIH1cbiAgICBJbmxpbmVWYWx1ZUNvbnRleHQuaXMgPSBpcztcbn0pKElubGluZVZhbHVlQ29udGV4dCB8fCAoSW5saW5lVmFsdWVDb250ZXh0ID0ge30pKTtcbi8qKlxuICogSW5sYXkgaGludCBraW5kcy5cbiAqXG4gKiBAc2luY2UgMy4xNy4wXG4gKi9cbmV4cG9ydCB2YXIgSW5sYXlIaW50S2luZDtcbihmdW5jdGlvbiAoSW5sYXlIaW50S2luZCkge1xuICAgIC8qKlxuICAgICAqIEFuIGlubGF5IGhpbnQgdGhhdCBmb3IgYSB0eXBlIGFubm90YXRpb24uXG4gICAgICovXG4gICAgSW5sYXlIaW50S2luZC5UeXBlID0gMTtcbiAgICAvKipcbiAgICAgKiBBbiBpbmxheSBoaW50IHRoYXQgaXMgZm9yIGEgcGFyYW1ldGVyLlxuICAgICAqL1xuICAgIElubGF5SGludEtpbmQuUGFyYW1ldGVyID0gMjtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICByZXR1cm4gdmFsdWUgPT09IDEgfHwgdmFsdWUgPT09IDI7XG4gICAgfVxuICAgIElubGF5SGludEtpbmQuaXMgPSBpcztcbn0pKElubGF5SGludEtpbmQgfHwgKElubGF5SGludEtpbmQgPSB7fSkpO1xuZXhwb3J0IHZhciBJbmxheUhpbnRMYWJlbFBhcnQ7XG4oZnVuY3Rpb24gKElubGF5SGludExhYmVsUGFydCkge1xuICAgIGZ1bmN0aW9uIGNyZWF0ZSh2YWx1ZSkge1xuICAgICAgICByZXR1cm4geyB2YWx1ZSB9O1xuICAgIH1cbiAgICBJbmxheUhpbnRMYWJlbFBhcnQuY3JlYXRlID0gY3JlYXRlO1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMub2JqZWN0TGl0ZXJhbChjYW5kaWRhdGUpXG4gICAgICAgICAgICAmJiAoY2FuZGlkYXRlLnRvb2x0aXAgPT09IHVuZGVmaW5lZCB8fCBJcy5zdHJpbmcoY2FuZGlkYXRlLnRvb2x0aXApIHx8IE1hcmt1cENvbnRlbnQuaXMoY2FuZGlkYXRlLnRvb2x0aXApKVxuICAgICAgICAgICAgJiYgKGNhbmRpZGF0ZS5sb2NhdGlvbiA9PT0gdW5kZWZpbmVkIHx8IExvY2F0aW9uLmlzKGNhbmRpZGF0ZS5sb2NhdGlvbikpXG4gICAgICAgICAgICAmJiAoY2FuZGlkYXRlLmNvbW1hbmQgPT09IHVuZGVmaW5lZCB8fCBDb21tYW5kLmlzKGNhbmRpZGF0ZS5jb21tYW5kKSk7XG4gICAgfVxuICAgIElubGF5SGludExhYmVsUGFydC5pcyA9IGlzO1xufSkoSW5sYXlIaW50TGFiZWxQYXJ0IHx8IChJbmxheUhpbnRMYWJlbFBhcnQgPSB7fSkpO1xuZXhwb3J0IHZhciBJbmxheUhpbnQ7XG4oZnVuY3Rpb24gKElubGF5SGludCkge1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShwb3NpdGlvbiwgbGFiZWwsIGtpbmQpIHtcbiAgICAgICAgY29uc3QgcmVzdWx0ID0geyBwb3NpdGlvbiwgbGFiZWwgfTtcbiAgICAgICAgaWYgKGtpbmQgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmVzdWx0LmtpbmQgPSBraW5kO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxuICAgIElubGF5SGludC5jcmVhdGUgPSBjcmVhdGU7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5vYmplY3RMaXRlcmFsKGNhbmRpZGF0ZSkgJiYgUG9zaXRpb24uaXMoY2FuZGlkYXRlLnBvc2l0aW9uKVxuICAgICAgICAgICAgJiYgKElzLnN0cmluZyhjYW5kaWRhdGUubGFiZWwpIHx8IElzLnR5cGVkQXJyYXkoY2FuZGlkYXRlLmxhYmVsLCBJbmxheUhpbnRMYWJlbFBhcnQuaXMpKVxuICAgICAgICAgICAgJiYgKGNhbmRpZGF0ZS5raW5kID09PSB1bmRlZmluZWQgfHwgSW5sYXlIaW50S2luZC5pcyhjYW5kaWRhdGUua2luZCkpXG4gICAgICAgICAgICAmJiAoY2FuZGlkYXRlLnRleHRFZGl0cyA9PT0gdW5kZWZpbmVkKSB8fCBJcy50eXBlZEFycmF5KGNhbmRpZGF0ZS50ZXh0RWRpdHMsIFRleHRFZGl0LmlzKVxuICAgICAgICAgICAgJiYgKGNhbmRpZGF0ZS50b29sdGlwID09PSB1bmRlZmluZWQgfHwgSXMuc3RyaW5nKGNhbmRpZGF0ZS50b29sdGlwKSB8fCBNYXJrdXBDb250ZW50LmlzKGNhbmRpZGF0ZS50b29sdGlwKSlcbiAgICAgICAgICAgICYmIChjYW5kaWRhdGUucGFkZGluZ0xlZnQgPT09IHVuZGVmaW5lZCB8fCBJcy5ib29sZWFuKGNhbmRpZGF0ZS5wYWRkaW5nTGVmdCkpXG4gICAgICAgICAgICAmJiAoY2FuZGlkYXRlLnBhZGRpbmdSaWdodCA9PT0gdW5kZWZpbmVkIHx8IElzLmJvb2xlYW4oY2FuZGlkYXRlLnBhZGRpbmdSaWdodCkpO1xuICAgIH1cbiAgICBJbmxheUhpbnQuaXMgPSBpcztcbn0pKElubGF5SGludCB8fCAoSW5sYXlIaW50ID0ge30pKTtcbmV4cG9ydCB2YXIgU3RyaW5nVmFsdWU7XG4oZnVuY3Rpb24gKFN0cmluZ1ZhbHVlKSB7XG4gICAgZnVuY3Rpb24gY3JlYXRlU25pcHBldCh2YWx1ZSkge1xuICAgICAgICByZXR1cm4geyBraW5kOiAnc25pcHBldCcsIHZhbHVlIH07XG4gICAgfVxuICAgIFN0cmluZ1ZhbHVlLmNyZWF0ZVNuaXBwZXQgPSBjcmVhdGVTbmlwcGV0O1xufSkoU3RyaW5nVmFsdWUgfHwgKFN0cmluZ1ZhbHVlID0ge30pKTtcbmV4cG9ydCB2YXIgSW5saW5lQ29tcGxldGlvbkl0ZW07XG4oZnVuY3Rpb24gKElubGluZUNvbXBsZXRpb25JdGVtKSB7XG4gICAgZnVuY3Rpb24gY3JlYXRlKGluc2VydFRleHQsIGZpbHRlclRleHQsIHJhbmdlLCBjb21tYW5kKSB7XG4gICAgICAgIHJldHVybiB7IGluc2VydFRleHQsIGZpbHRlclRleHQsIHJhbmdlLCBjb21tYW5kIH07XG4gICAgfVxuICAgIElubGluZUNvbXBsZXRpb25JdGVtLmNyZWF0ZSA9IGNyZWF0ZTtcbn0pKElubGluZUNvbXBsZXRpb25JdGVtIHx8IChJbmxpbmVDb21wbGV0aW9uSXRlbSA9IHt9KSk7XG5leHBvcnQgdmFyIElubGluZUNvbXBsZXRpb25MaXN0O1xuKGZ1bmN0aW9uIChJbmxpbmVDb21wbGV0aW9uTGlzdCkge1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShpdGVtcykge1xuICAgICAgICByZXR1cm4geyBpdGVtcyB9O1xuICAgIH1cbiAgICBJbmxpbmVDb21wbGV0aW9uTGlzdC5jcmVhdGUgPSBjcmVhdGU7XG59KShJbmxpbmVDb21wbGV0aW9uTGlzdCB8fCAoSW5saW5lQ29tcGxldGlvbkxpc3QgPSB7fSkpO1xuLyoqXG4gKiBEZXNjcmliZXMgaG93IGFuIHtAbGluayBJbmxpbmVDb21wbGV0aW9uSXRlbVByb3ZpZGVyIGlubGluZSBjb21wbGV0aW9uIHByb3ZpZGVyfSB3YXMgdHJpZ2dlcmVkLlxuICpcbiAqIEBzaW5jZSAzLjE4LjBcbiAqIEBwcm9wb3NlZFxuICovXG5leHBvcnQgdmFyIElubGluZUNvbXBsZXRpb25UcmlnZ2VyS2luZDtcbihmdW5jdGlvbiAoSW5saW5lQ29tcGxldGlvblRyaWdnZXJLaW5kKSB7XG4gICAgLyoqXG4gICAgICogQ29tcGxldGlvbiB3YXMgdHJpZ2dlcmVkIGV4cGxpY2l0bHkgYnkgYSB1c2VyIGdlc3R1cmUuXG4gICAgICovXG4gICAgSW5saW5lQ29tcGxldGlvblRyaWdnZXJLaW5kLkludm9rZWQgPSAwO1xuICAgIC8qKlxuICAgICAqIENvbXBsZXRpb24gd2FzIHRyaWdnZXJlZCBhdXRvbWF0aWNhbGx5IHdoaWxlIGVkaXRpbmcuXG4gICAgICovXG4gICAgSW5saW5lQ29tcGxldGlvblRyaWdnZXJLaW5kLkF1dG9tYXRpYyA9IDE7XG59KShJbmxpbmVDb21wbGV0aW9uVHJpZ2dlcktpbmQgfHwgKElubGluZUNvbXBsZXRpb25UcmlnZ2VyS2luZCA9IHt9KSk7XG5leHBvcnQgdmFyIFNlbGVjdGVkQ29tcGxldGlvbkluZm87XG4oZnVuY3Rpb24gKFNlbGVjdGVkQ29tcGxldGlvbkluZm8pIHtcbiAgICBmdW5jdGlvbiBjcmVhdGUocmFuZ2UsIHRleHQpIHtcbiAgICAgICAgcmV0dXJuIHsgcmFuZ2UsIHRleHQgfTtcbiAgICB9XG4gICAgU2VsZWN0ZWRDb21wbGV0aW9uSW5mby5jcmVhdGUgPSBjcmVhdGU7XG59KShTZWxlY3RlZENvbXBsZXRpb25JbmZvIHx8IChTZWxlY3RlZENvbXBsZXRpb25JbmZvID0ge30pKTtcbmV4cG9ydCB2YXIgSW5saW5lQ29tcGxldGlvbkNvbnRleHQ7XG4oZnVuY3Rpb24gKElubGluZUNvbXBsZXRpb25Db250ZXh0KSB7XG4gICAgZnVuY3Rpb24gY3JlYXRlKHRyaWdnZXJLaW5kLCBzZWxlY3RlZENvbXBsZXRpb25JbmZvKSB7XG4gICAgICAgIHJldHVybiB7IHRyaWdnZXJLaW5kLCBzZWxlY3RlZENvbXBsZXRpb25JbmZvIH07XG4gICAgfVxuICAgIElubGluZUNvbXBsZXRpb25Db250ZXh0LmNyZWF0ZSA9IGNyZWF0ZTtcbn0pKElubGluZUNvbXBsZXRpb25Db250ZXh0IHx8IChJbmxpbmVDb21wbGV0aW9uQ29udGV4dCA9IHt9KSk7XG5leHBvcnQgdmFyIFdvcmtzcGFjZUZvbGRlcjtcbihmdW5jdGlvbiAoV29ya3NwYWNlRm9sZGVyKSB7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5vYmplY3RMaXRlcmFsKGNhbmRpZGF0ZSkgJiYgVVJJLmlzKGNhbmRpZGF0ZS51cmkpICYmIElzLnN0cmluZyhjYW5kaWRhdGUubmFtZSk7XG4gICAgfVxuICAgIFdvcmtzcGFjZUZvbGRlci5pcyA9IGlzO1xufSkoV29ya3NwYWNlRm9sZGVyIHx8IChXb3Jrc3BhY2VGb2xkZXIgPSB7fSkpO1xuZXhwb3J0IGNvbnN0IEVPTCA9IFsnXFxuJywgJ1xcclxcbicsICdcXHInXTtcbi8qKlxuICogQGRlcHJlY2F0ZWQgVXNlIHRoZSB0ZXh0IGRvY3VtZW50IGZyb20gdGhlIG5ldyB2c2NvZGUtbGFuZ3VhZ2VzZXJ2ZXItdGV4dGRvY3VtZW50IHBhY2thZ2UuXG4gKi9cbmV4cG9ydCB2YXIgVGV4dERvY3VtZW50O1xuKGZ1bmN0aW9uIChUZXh0RG9jdW1lbnQpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IElUZXh0RG9jdW1lbnQgbGl0ZXJhbCBmcm9tIHRoZSBnaXZlbiB1cmkgYW5kIGNvbnRlbnQuXG4gICAgICogQHBhcmFtIHVyaSBUaGUgZG9jdW1lbnQncyB1cmkuXG4gICAgICogQHBhcmFtIGxhbmd1YWdlSWQgVGhlIGRvY3VtZW50J3MgbGFuZ3VhZ2UgSWQuXG4gICAgICogQHBhcmFtIHZlcnNpb24gVGhlIGRvY3VtZW50J3MgdmVyc2lvbi5cbiAgICAgKiBAcGFyYW0gY29udGVudCBUaGUgZG9jdW1lbnQncyBjb250ZW50LlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZSh1cmksIGxhbmd1YWdlSWQsIHZlcnNpb24sIGNvbnRlbnQpIHtcbiAgICAgICAgcmV0dXJuIG5ldyBGdWxsVGV4dERvY3VtZW50KHVyaSwgbGFuZ3VhZ2VJZCwgdmVyc2lvbiwgY29udGVudCk7XG4gICAgfVxuICAgIFRleHREb2N1bWVudC5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBJVGV4dERvY3VtZW50fSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMuZGVmaW5lZChjYW5kaWRhdGUpICYmIElzLnN0cmluZyhjYW5kaWRhdGUudXJpKSAmJiAoSXMudW5kZWZpbmVkKGNhbmRpZGF0ZS5sYW5ndWFnZUlkKSB8fCBJcy5zdHJpbmcoY2FuZGlkYXRlLmxhbmd1YWdlSWQpKSAmJiBJcy51aW50ZWdlcihjYW5kaWRhdGUubGluZUNvdW50KVxuICAgICAgICAgICAgJiYgSXMuZnVuYyhjYW5kaWRhdGUuZ2V0VGV4dCkgJiYgSXMuZnVuYyhjYW5kaWRhdGUucG9zaXRpb25BdCkgJiYgSXMuZnVuYyhjYW5kaWRhdGUub2Zmc2V0QXQpID8gdHJ1ZSA6IGZhbHNlO1xuICAgIH1cbiAgICBUZXh0RG9jdW1lbnQuaXMgPSBpcztcbiAgICBmdW5jdGlvbiBhcHBseUVkaXRzKGRvY3VtZW50LCBlZGl0cykge1xuICAgICAgICBsZXQgdGV4dCA9IGRvY3VtZW50LmdldFRleHQoKTtcbiAgICAgICAgbGV0IHNvcnRlZEVkaXRzID0gbWVyZ2VTb3J0KGVkaXRzLCAoYSwgYikgPT4ge1xuICAgICAgICAgICAgbGV0IGRpZmYgPSBhLnJhbmdlLnN0YXJ0LmxpbmUgLSBiLnJhbmdlLnN0YXJ0LmxpbmU7XG4gICAgICAgICAgICBpZiAoZGlmZiA9PT0gMCkge1xuICAgICAgICAgICAgICAgIHJldHVybiBhLnJhbmdlLnN0YXJ0LmNoYXJhY3RlciAtIGIucmFuZ2Uuc3RhcnQuY2hhcmFjdGVyO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgcmV0dXJuIGRpZmY7XG4gICAgICAgIH0pO1xuICAgICAgICBsZXQgbGFzdE1vZGlmaWVkT2Zmc2V0ID0gdGV4dC5sZW5ndGg7XG4gICAgICAgIGZvciAobGV0IGkgPSBzb3J0ZWRFZGl0cy5sZW5ndGggLSAxOyBpID49IDA7IGktLSkge1xuICAgICAgICAgICAgbGV0IGUgPSBzb3J0ZWRFZGl0c1tpXTtcbiAgICAgICAgICAgIGxldCBzdGFydE9mZnNldCA9IGRvY3VtZW50Lm9mZnNldEF0KGUucmFuZ2Uuc3RhcnQpO1xuICAgICAgICAgICAgbGV0IGVuZE9mZnNldCA9IGRvY3VtZW50Lm9mZnNldEF0KGUucmFuZ2UuZW5kKTtcbiAgICAgICAgICAgIGlmIChlbmRPZmZzZXQgPD0gbGFzdE1vZGlmaWVkT2Zmc2V0KSB7XG4gICAgICAgICAgICAgICAgdGV4dCA9IHRleHQuc3Vic3RyaW5nKDAsIHN0YXJ0T2Zmc2V0KSArIGUubmV3VGV4dCArIHRleHQuc3Vic3RyaW5nKGVuZE9mZnNldCwgdGV4dC5sZW5ndGgpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdPdmVybGFwcGluZyBlZGl0Jyk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBsYXN0TW9kaWZpZWRPZmZzZXQgPSBzdGFydE9mZnNldDtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gdGV4dDtcbiAgICB9XG4gICAgVGV4dERvY3VtZW50LmFwcGx5RWRpdHMgPSBhcHBseUVkaXRzO1xuICAgIGZ1bmN0aW9uIG1lcmdlU29ydChkYXRhLCBjb21wYXJlKSB7XG4gICAgICAgIGlmIChkYXRhLmxlbmd0aCA8PSAxKSB7XG4gICAgICAgICAgICAvLyBzb3J0ZWRcbiAgICAgICAgICAgIHJldHVybiBkYXRhO1xuICAgICAgICB9XG4gICAgICAgIGNvbnN0IHAgPSAoZGF0YS5sZW5ndGggLyAyKSB8IDA7XG4gICAgICAgIGNvbnN0IGxlZnQgPSBkYXRhLnNsaWNlKDAsIHApO1xuICAgICAgICBjb25zdCByaWdodCA9IGRhdGEuc2xpY2UocCk7XG4gICAgICAgIG1lcmdlU29ydChsZWZ0LCBjb21wYXJlKTtcbiAgICAgICAgbWVyZ2VTb3J0KHJpZ2h0LCBjb21wYXJlKTtcbiAgICAgICAgbGV0IGxlZnRJZHggPSAwO1xuICAgICAgICBsZXQgcmlnaHRJZHggPSAwO1xuICAgICAgICBsZXQgaSA9IDA7XG4gICAgICAgIHdoaWxlIChsZWZ0SWR4IDwgbGVmdC5sZW5ndGggJiYgcmlnaHRJZHggPCByaWdodC5sZW5ndGgpIHtcbiAgICAgICAgICAgIGxldCByZXQgPSBjb21wYXJlKGxlZnRbbGVmdElkeF0sIHJpZ2h0W3JpZ2h0SWR4XSk7XG4gICAgICAgICAgICBpZiAocmV0IDw9IDApIHtcbiAgICAgICAgICAgICAgICAvLyBzbWFsbGVyX2VxdWFsIC0+IHRha2UgbGVmdCB0byBwcmVzZXJ2ZSBvcmRlclxuICAgICAgICAgICAgICAgIGRhdGFbaSsrXSA9IGxlZnRbbGVmdElkeCsrXTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIC8vIGdyZWF0ZXIgLT4gdGFrZSByaWdodFxuICAgICAgICAgICAgICAgIGRhdGFbaSsrXSA9IHJpZ2h0W3JpZ2h0SWR4KytdO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIHdoaWxlIChsZWZ0SWR4IDwgbGVmdC5sZW5ndGgpIHtcbiAgICAgICAgICAgIGRhdGFbaSsrXSA9IGxlZnRbbGVmdElkeCsrXTtcbiAgICAgICAgfVxuICAgICAgICB3aGlsZSAocmlnaHRJZHggPCByaWdodC5sZW5ndGgpIHtcbiAgICAgICAgICAgIGRhdGFbaSsrXSA9IHJpZ2h0W3JpZ2h0SWR4KytdO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiBkYXRhO1xuICAgIH1cbn0pKFRleHREb2N1bWVudCB8fCAoVGV4dERvY3VtZW50ID0ge30pKTtcbi8qKlxuICogQGRlcHJlY2F0ZWQgVXNlIHRoZSB0ZXh0IGRvY3VtZW50IGZyb20gdGhlIG5ldyB2c2NvZGUtbGFuZ3VhZ2VzZXJ2ZXItdGV4dGRvY3VtZW50IHBhY2thZ2UuXG4gKi9cbmNsYXNzIEZ1bGxUZXh0RG9jdW1lbnQge1xuICAgIGNvbnN0cnVjdG9yKHVyaSwgbGFuZ3VhZ2VJZCwgdmVyc2lvbiwgY29udGVudCkge1xuICAgICAgICB0aGlzLl91cmkgPSB1cmk7XG4gICAgICAgIHRoaXMuX2xhbmd1YWdlSWQgPSBsYW5ndWFnZUlkO1xuICAgICAgICB0aGlzLl92ZXJzaW9uID0gdmVyc2lvbjtcbiAgICAgICAgdGhpcy5fY29udGVudCA9IGNvbnRlbnQ7XG4gICAgICAgIHRoaXMuX2xpbmVPZmZzZXRzID0gdW5kZWZpbmVkO1xuICAgIH1cbiAgICBnZXQgdXJpKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5fdXJpO1xuICAgIH1cbiAgICBnZXQgbGFuZ3VhZ2VJZCgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX2xhbmd1YWdlSWQ7XG4gICAgfVxuICAgIGdldCB2ZXJzaW9uKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5fdmVyc2lvbjtcbiAgICB9XG4gICAgZ2V0VGV4dChyYW5nZSkge1xuICAgICAgICBpZiAocmFuZ2UpIHtcbiAgICAgICAgICAgIGxldCBzdGFydCA9IHRoaXMub2Zmc2V0QXQocmFuZ2Uuc3RhcnQpO1xuICAgICAgICAgICAgbGV0IGVuZCA9IHRoaXMub2Zmc2V0QXQocmFuZ2UuZW5kKTtcbiAgICAgICAgICAgIHJldHVybiB0aGlzLl9jb250ZW50LnN1YnN0cmluZyhzdGFydCwgZW5kKTtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gdGhpcy5fY29udGVudDtcbiAgICB9XG4gICAgdXBkYXRlKGV2ZW50LCB2ZXJzaW9uKSB7XG4gICAgICAgIHRoaXMuX2NvbnRlbnQgPSBldmVudC50ZXh0O1xuICAgICAgICB0aGlzLl92ZXJzaW9uID0gdmVyc2lvbjtcbiAgICAgICAgdGhpcy5fbGluZU9mZnNldHMgPSB1bmRlZmluZWQ7XG4gICAgfVxuICAgIGdldExpbmVPZmZzZXRzKCkge1xuICAgICAgICBpZiAodGhpcy5fbGluZU9mZnNldHMgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgbGV0IGxpbmVPZmZzZXRzID0gW107XG4gICAgICAgICAgICBsZXQgdGV4dCA9IHRoaXMuX2NvbnRlbnQ7XG4gICAgICAgICAgICBsZXQgaXNMaW5lU3RhcnQgPSB0cnVlO1xuICAgICAgICAgICAgZm9yIChsZXQgaSA9IDA7IGkgPCB0ZXh0Lmxlbmd0aDsgaSsrKSB7XG4gICAgICAgICAgICAgICAgaWYgKGlzTGluZVN0YXJ0KSB7XG4gICAgICAgICAgICAgICAgICAgIGxpbmVPZmZzZXRzLnB1c2goaSk7XG4gICAgICAgICAgICAgICAgICAgIGlzTGluZVN0YXJ0ID0gZmFsc2U7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGxldCBjaCA9IHRleHQuY2hhckF0KGkpO1xuICAgICAgICAgICAgICAgIGlzTGluZVN0YXJ0ID0gKGNoID09PSAnXFxyJyB8fCBjaCA9PT0gJ1xcbicpO1xuICAgICAgICAgICAgICAgIGlmIChjaCA9PT0gJ1xccicgJiYgaSArIDEgPCB0ZXh0Lmxlbmd0aCAmJiB0ZXh0LmNoYXJBdChpICsgMSkgPT09ICdcXG4nKSB7XG4gICAgICAgICAgICAgICAgICAgIGkrKztcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAoaXNMaW5lU3RhcnQgJiYgdGV4dC5sZW5ndGggPiAwKSB7XG4gICAgICAgICAgICAgICAgbGluZU9mZnNldHMucHVzaCh0ZXh0Lmxlbmd0aCk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICB0aGlzLl9saW5lT2Zmc2V0cyA9IGxpbmVPZmZzZXRzO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiB0aGlzLl9saW5lT2Zmc2V0cztcbiAgICB9XG4gICAgcG9zaXRpb25BdChvZmZzZXQpIHtcbiAgICAgICAgb2Zmc2V0ID0gTWF0aC5tYXgoTWF0aC5taW4ob2Zmc2V0LCB0aGlzLl9jb250ZW50Lmxlbmd0aCksIDApO1xuICAgICAgICBsZXQgbGluZU9mZnNldHMgPSB0aGlzLmdldExpbmVPZmZzZXRzKCk7XG4gICAgICAgIGxldCBsb3cgPSAwLCBoaWdoID0gbGluZU9mZnNldHMubGVuZ3RoO1xuICAgICAgICBpZiAoaGlnaCA9PT0gMCkge1xuICAgICAgICAgICAgcmV0dXJuIFBvc2l0aW9uLmNyZWF0ZSgwLCBvZmZzZXQpO1xuICAgICAgICB9XG4gICAgICAgIHdoaWxlIChsb3cgPCBoaWdoKSB7XG4gICAgICAgICAgICBsZXQgbWlkID0gTWF0aC5mbG9vcigobG93ICsgaGlnaCkgLyAyKTtcbiAgICAgICAgICAgIGlmIChsaW5lT2Zmc2V0c1ttaWRdID4gb2Zmc2V0KSB7XG4gICAgICAgICAgICAgICAgaGlnaCA9IG1pZDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIGxvdyA9IG1pZCArIDE7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgLy8gbG93IGlzIHRoZSBsZWFzdCB4IGZvciB3aGljaCB0aGUgbGluZSBvZmZzZXQgaXMgbGFyZ2VyIHRoYW4gdGhlIGN1cnJlbnQgb2Zmc2V0XG4gICAgICAgIC8vIG9yIGFycmF5Lmxlbmd0aCBpZiBubyBsaW5lIG9mZnNldCBpcyBsYXJnZXIgdGhhbiB0aGUgY3VycmVudCBvZmZzZXRcbiAgICAgICAgbGV0IGxpbmUgPSBsb3cgLSAxO1xuICAgICAgICByZXR1cm4gUG9zaXRpb24uY3JlYXRlKGxpbmUsIG9mZnNldCAtIGxpbmVPZmZzZXRzW2xpbmVdKTtcbiAgICB9XG4gICAgb2Zmc2V0QXQocG9zaXRpb24pIHtcbiAgICAgICAgbGV0IGxpbmVPZmZzZXRzID0gdGhpcy5nZXRMaW5lT2Zmc2V0cygpO1xuICAgICAgICBpZiAocG9zaXRpb24ubGluZSA+PSBsaW5lT2Zmc2V0cy5sZW5ndGgpIHtcbiAgICAgICAgICAgIHJldHVybiB0aGlzLl9jb250ZW50Lmxlbmd0aDtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIGlmIChwb3NpdGlvbi5saW5lIDwgMCkge1xuICAgICAgICAgICAgcmV0dXJuIDA7XG4gICAgICAgIH1cbiAgICAgICAgbGV0IGxpbmVPZmZzZXQgPSBsaW5lT2Zmc2V0c1twb3NpdGlvbi5saW5lXTtcbiAgICAgICAgbGV0IG5leHRMaW5lT2Zmc2V0ID0gKHBvc2l0aW9uLmxpbmUgKyAxIDwgbGluZU9mZnNldHMubGVuZ3RoKSA/IGxpbmVPZmZzZXRzW3Bvc2l0aW9uLmxpbmUgKyAxXSA6IHRoaXMuX2NvbnRlbnQubGVuZ3RoO1xuICAgICAgICByZXR1cm4gTWF0aC5tYXgoTWF0aC5taW4obGluZU9mZnNldCArIHBvc2l0aW9uLmNoYXJhY3RlciwgbmV4dExpbmVPZmZzZXQpLCBsaW5lT2Zmc2V0KTtcbiAgICB9XG4gICAgZ2V0IGxpbmVDb3VudCgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuZ2V0TGluZU9mZnNldHMoKS5sZW5ndGg7XG4gICAgfVxufVxudmFyIElzO1xuKGZ1bmN0aW9uIChJcykge1xuICAgIGNvbnN0IHRvU3RyaW5nID0gT2JqZWN0LnByb3RvdHlwZS50b1N0cmluZztcbiAgICBmdW5jdGlvbiBkZWZpbmVkKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiB0eXBlb2YgdmFsdWUgIT09ICd1bmRlZmluZWQnO1xuICAgIH1cbiAgICBJcy5kZWZpbmVkID0gZGVmaW5lZDtcbiAgICBmdW5jdGlvbiB1bmRlZmluZWQodmFsdWUpIHtcbiAgICAgICAgcmV0dXJuIHR5cGVvZiB2YWx1ZSA9PT0gJ3VuZGVmaW5lZCc7XG4gICAgfVxuICAgIElzLnVuZGVmaW5lZCA9IHVuZGVmaW5lZDtcbiAgICBmdW5jdGlvbiBib29sZWFuKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiB2YWx1ZSA9PT0gdHJ1ZSB8fCB2YWx1ZSA9PT0gZmFsc2U7XG4gICAgfVxuICAgIElzLmJvb2xlYW4gPSBib29sZWFuO1xuICAgIGZ1bmN0aW9uIHN0cmluZyh2YWx1ZSkge1xuICAgICAgICByZXR1cm4gdG9TdHJpbmcuY2FsbCh2YWx1ZSkgPT09ICdbb2JqZWN0IFN0cmluZ10nO1xuICAgIH1cbiAgICBJcy5zdHJpbmcgPSBzdHJpbmc7XG4gICAgZnVuY3Rpb24gbnVtYmVyKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiB0b1N0cmluZy5jYWxsKHZhbHVlKSA9PT0gJ1tvYmplY3QgTnVtYmVyXSc7XG4gICAgfVxuICAgIElzLm51bWJlciA9IG51bWJlcjtcbiAgICBmdW5jdGlvbiBudW1iZXJSYW5nZSh2YWx1ZSwgbWluLCBtYXgpIHtcbiAgICAgICAgcmV0dXJuIHRvU3RyaW5nLmNhbGwodmFsdWUpID09PSAnW29iamVjdCBOdW1iZXJdJyAmJiBtaW4gPD0gdmFsdWUgJiYgdmFsdWUgPD0gbWF4O1xuICAgIH1cbiAgICBJcy5udW1iZXJSYW5nZSA9IG51bWJlclJhbmdlO1xuICAgIGZ1bmN0aW9uIGludGVnZXIodmFsdWUpIHtcbiAgICAgICAgcmV0dXJuIHRvU3RyaW5nLmNhbGwodmFsdWUpID09PSAnW29iamVjdCBOdW1iZXJdJyAmJiAtMjE0NzQ4MzY0OCA8PSB2YWx1ZSAmJiB2YWx1ZSA8PSAyMTQ3NDgzNjQ3O1xuICAgIH1cbiAgICBJcy5pbnRlZ2VyID0gaW50ZWdlcjtcbiAgICBmdW5jdGlvbiB1aW50ZWdlcih2YWx1ZSkge1xuICAgICAgICByZXR1cm4gdG9TdHJpbmcuY2FsbCh2YWx1ZSkgPT09ICdbb2JqZWN0IE51bWJlcl0nICYmIDAgPD0gdmFsdWUgJiYgdmFsdWUgPD0gMjE0NzQ4MzY0NztcbiAgICB9XG4gICAgSXMudWludGVnZXIgPSB1aW50ZWdlcjtcbiAgICBmdW5jdGlvbiBmdW5jKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiB0b1N0cmluZy5jYWxsKHZhbHVlKSA9PT0gJ1tvYmplY3QgRnVuY3Rpb25dJztcbiAgICB9XG4gICAgSXMuZnVuYyA9IGZ1bmM7XG4gICAgZnVuY3Rpb24gb2JqZWN0TGl0ZXJhbCh2YWx1ZSkge1xuICAgICAgICAvLyBTdHJpY3RseSBzcGVha2luZyBjbGFzcyBpbnN0YW5jZXMgcGFzcyB0aGlzIGNoZWNrIGFzIHdlbGwuIFNpbmNlIHRoZSBMU1BcbiAgICAgICAgLy8gZG9lc24ndCB1c2UgY2xhc3NlcyB3ZSBpZ25vcmUgdGhpcyBmb3Igbm93LiBJZiB3ZSBkbyB3ZSBuZWVkIHRvIGFkZCBzb21ldGhpbmdcbiAgICAgICAgLy8gbGlrZSB0aGlzOiBgT2JqZWN0LmdldFByb3RvdHlwZU9mKE9iamVjdC5nZXRQcm90b3R5cGVPZih4KSkgPT09IG51bGxgXG4gICAgICAgIHJldHVybiB2YWx1ZSAhPT0gbnVsbCAmJiB0eXBlb2YgdmFsdWUgPT09ICdvYmplY3QnO1xuICAgIH1cbiAgICBJcy5vYmplY3RMaXRlcmFsID0gb2JqZWN0TGl0ZXJhbDtcbiAgICBmdW5jdGlvbiB0eXBlZEFycmF5KHZhbHVlLCBjaGVjaykge1xuICAgICAgICByZXR1cm4gQXJyYXkuaXNBcnJheSh2YWx1ZSkgJiYgdmFsdWUuZXZlcnkoY2hlY2spO1xuICAgIH1cbiAgICBJcy50eXBlZEFycmF5ID0gdHlwZWRBcnJh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QUFBQSxNQUMzRyxPQUFPO0FBQ0gsZ0JBQVEsT0FBTyxLQUFLLEVBQUUsTUFBTSxtQkFBbUIsR0FBRyxHQUFHLFFBQVEsQ0FBQztBQUFBLE1BQ2xFO0FBQUEsSUFDSjtBQUVBLFVBQU8sV0FBbUIscUJBQXFCLE9BQU87QUFBQSxFQUMxRDtBQUVBLFdBQVMsZ0JBQWdCO0FBQ3JCLFdBQU87QUFBQSxNQUNILE1BQU07QUFBQSxRQUNGLFFBQVE7QUFBQSxVQUNKLE9BQVksRUFBRSxTQUFTLEtBQUs7QUFBQSxVQUM1QixRQUFZLEVBQUUsU0FBUyxNQUFNO0FBQUEsVUFDN0IsWUFBWSxDQUFDLHNCQUFzQixtQkFBbUIsMkJBQTJCO0FBQUEsUUFDckY7QUFBQSxRQUNBLFdBQVcsRUFBRSxTQUFTLEtBQUs7QUFBQSxRQUMzQixZQUFZO0FBQUEsVUFDUixXQUFzQjtBQUFBLFVBQ3RCLHNCQUFzQjtBQUFBLFVBQ3RCLGVBQWU7QUFBQSxZQUNYO0FBQUEsWUFBYTtBQUFBLFlBQVM7QUFBQSxZQUN0QjtBQUFBLFlBQ0E7QUFBQSxZQUNBO0FBQUEsVUFDSjtBQUFBLFVBQ0EsYUFBYSxDQUFDLFFBQVEsU0FBUyxPQUFPLE9BQU8sRUFBRTtBQUFBLFFBQ25EO0FBQUEsUUFDQSxlQUFnQixFQUFFLFNBQVMsS0FBSztBQUFBLFFBQ2hDLFFBQWdCLEVBQUUsU0FBUyxLQUFLO0FBQUEsUUFDaEMsYUFBZ0IsRUFBRSxpQkFBaUIsTUFBTTtBQUFBLE1BQzdDO0FBQUEsSUFDSjtBQUFBLEVBQ0o7IiwKICAibmFtZXMiOiBbIkVycm9yQ29kZXMiLCAiTWVzc2FnZSIsICJUb3VjaCIsICJEaXNwb3NhYmxlIiwgIlJBTCIsICJFdmVudCIsICJJcyIsICJDYW5jZWxsYXRpb25Ub2tlbiIsICJDYW5jZWxsYXRpb25TdGF0ZSIsICJfY29ubiIsICJJcyIsICJNZXNzYWdlUmVhZGVyIiwgIkFic3RyYWN0TWVzc2FnZVJlYWRlciIsICJSZXNvbHZlZE1lc3NhZ2VSZWFkZXJPcHRpb25zIiwgIklzIiwgIk1lc3NhZ2VXcml0ZXIiLCAiQWJzdHJhY3RNZXNzYWdlV3JpdGVyIiwgIlJlc29sdmVkTWVzc2FnZVdyaXRlck9wdGlvbnMiLCAicmVzdWx0IiwgIklzIiwgIkNhbmNlbE5vdGlmaWNhdGlvbiIsICJQcm9ncmVzc1Rva2VuIiwgIlByb2dyZXNzTm90aWZpY2F0aW9uIiwgIlN0YXJSZXF1ZXN0SGFuZGxlciIsICJUcmFjZSIsICJUcmFjZVZhbHVlcyIsICJUcmFjZUZvcm1hdCIsICJTZXRUcmFjZU5vdGlmaWNhdGlvbiIsICJMb2dUcmFjZU5vdGlmaWNhdGlvbiIsICJDb25uZWN0aW9uRXJyb3JzIiwgIkNvbm5lY3Rpb25TdHJhdGVneSIsICJJZENhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kiLCAiUmVxdWVzdENhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kiLCAiQ2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneSIsICJDYW5jZWxsYXRpb25TZW5kZXJTdHJhdGVneSIsICJDYW5jZWxsYXRpb25TdHJhdGVneSIsICJNZXNzYWdlU3RyYXRlZ3kiLCAiQ29ubmVjdGlvbk9wdGlvbnMiLCAiQ29ubmVjdGlvblN0YXRlIiwgImNyZWF0ZU1lc3NhZ2VDb25uZWN0aW9uIiwgInN0YXJ0VGltZSIsICJSSUwiLCAibSIsICJleHBvcnRzIiwgImNyZWF0ZU1lc3NhZ2VDb25uZWN0aW9uIiwgImltcG9ydF92c2NvZGVfanNvbnJwYyIsICJpbXBvcnRfdnNjb2RlX2pzb25ycGMiLCAiaW1wb3J0X3ZzY29kZV9qc29ucnBjIiwgImltcG9ydF92c2NvZGVfanNvbnJwYyIsICJEb2N1bWVudFVyaSIsICJVUkkiLCAiaW50ZWdlciIsICJ1aW50ZWdlciIsICJQb3NpdGlvbiIsICJSYW5nZSIsICJMb2NhdGlvbiIsICJMb2NhdGlvbkxpbmsiLCAiQ29sb3IiLCAiQ29sb3JJbmZvcm1hdGlvbiIsICJDb2xvclByZXNlbnRhdGlvbiIsICJGb2xkaW5nUmFuZ2VLaW5kIiwgIkZvbGRpbmdSYW5nZSIsICJEaWFnbm9zdGljUmVsYXRlZEluZm9ybWF0aW9uIiwgImxvY2F0aW9uIiwgIkRpYWdub3N0aWNTZXZlcml0eSIsICJEaWFnbm9zdGljVGFnIiwgIkNvZGVEZXNjcmlwdGlvbiIsICJEaWFnbm9zdGljIiwgIkNvbW1hbmQiLCAiVGV4dEVkaXQiLCAiQ2hhbmdlQW5ub3RhdGlvbiIsICJDaGFuZ2VBbm5vdGF0aW9uSWRlbnRpZmllciIsICJBbm5vdGF0ZWRUZXh0RWRpdCIsICJUZXh0RG9jdW1lbnRFZGl0IiwgIkNyZWF0ZUZpbGUiLCAiUmVuYW1lRmlsZSIsICJEZWxldGVGaWxlIiwgIldvcmtzcGFjZUVkaXQiLCAiVGV4dERvY3VtZW50SWRlbnRpZmllciIsICJWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyIiwgIk9wdGlvbmFsVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllciIsICJUZXh0RG9jdW1lbnRJdGVtIiwgIk1hcmt1cEtpbmQiLCAiTWFya3VwQ29udGVudCIsICJDb21wbGV0aW9uSXRlbUtpbmQiLCAiSW5zZXJ0VGV4dEZvcm1hdCIsICJDb21wbGV0aW9uSXRlbVRhZyIsICJJbnNlcnRSZXBsYWNlRWRpdCIsICJSYW5nZSIsICJJbnNlcnRUZXh0TW9kZSIsICJDb21wbGV0aW9uSXRlbUxhYmVsRGV0YWlscyIsICJDb21wbGV0aW9uSXRlbSIsICJDb21wbGV0aW9uTGlzdCIsICJNYXJrZWRTdHJpbmciLCAiSG92ZXIiLCAiUGFyYW1ldGVySW5mb3JtYXRpb24iLCAiU2lnbmF0dXJlSW5mb3JtYXRpb24iLCAiRG9jdW1lbnRIaWdobGlnaHRLaW5kIiwgIkRvY3VtZW50SGlnaGxpZ2h0IiwgIlN5bWJvbEtpbmQiLCAiU3ltYm9sVGFnIiwgIlN5bWJvbEluZm9ybWF0aW9uIiwgIldvcmtzcGFjZVN5bWJvbCIsICJEb2N1bWVudFN5bWJvbCIsICJDb2RlQWN0aW9uS2luZCIsICJDb2RlQWN0aW9uVHJpZ2dlcktpbmQiLCAiQ29kZUFjdGlvbkNvbnRleHQiLCAiQ29kZUFjdGlvbiIsICJDb2RlTGVucyIsICJGb3JtYXR0aW5nT3B0aW9ucyIsICJEb2N1bWVudExpbmsiLCAiU2VsZWN0aW9uUmFuZ2UiLCAiU2VtYW50aWNUb2tlblR5cGVzIiwgIlNlbWFudGljVG9rZW5Nb2RpZmllcnMiLCAiU2VtYW50aWNUb2tlbnMiLCAiSW5saW5lVmFsdWVUZXh0IiwgIklubGluZVZhbHVlVmFyaWFibGVMb29rdXAiLCAiSW5saW5lVmFsdWVFdmFsdWF0YWJsZUV4cHJlc3Npb24iLCAiSW5saW5lVmFsdWVDb250ZXh0IiwgIklubGF5SGludEtpbmQiLCAiSW5sYXlIaW50TGFiZWxQYXJ0IiwgIklubGF5SGludCIsICJQb3NpdGlvbiIsICJTdHJpbmdWYWx1ZSIsICJJbmxpbmVDb21wbGV0aW9uSXRlbSIsICJJbmxpbmVDb21wbGV0aW9uTGlzdCIsICJJbmxpbmVDb21wbGV0aW9uVHJpZ2dlcktpbmQiLCAiU2VsZWN0ZWRDb21wbGV0aW9uSW5mbyIsICJJbmxpbmVDb21wbGV0aW9uQ29udGV4dCIsICJXb3Jrc3BhY2VGb2xkZXIiLCAiVGV4dERvY3VtZW50IiwgIlBvc2l0aW9uIiwgIklzIiwgInVuZGVmaW5lZCIsICJpbnRlZ2VyIiwgInVpbnRlZ2VyIiwgIm0iXQp9Cg==
