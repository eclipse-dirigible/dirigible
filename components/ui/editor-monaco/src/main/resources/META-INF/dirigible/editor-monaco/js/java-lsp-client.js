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
          // Cap results so import/type completion over the large platform classpath stays snappy.
          maxResults: 50,
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
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtanNvbnJwYy9saWIvY29tbW9uL2lzLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9tZXNzYWdlcy5qcyIsICJsc3Avbm9kZV9tb2R1bGVzL3ZzY29kZS1qc29ucnBjL2xpYi9jb21tb24vbGlua2VkTWFwLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9kaXNwb3NhYmxlLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9yYWwuanMiLCAibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtanNvbnJwYy9saWIvY29tbW9uL2V2ZW50cy5qcyIsICJsc3Avbm9kZV9tb2R1bGVzL3ZzY29kZS1qc29ucnBjL2xpYi9jb21tb24vY2FuY2VsbGF0aW9uLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9zaGFyZWRBcnJheUNhbmNlbGxhdGlvbi5qcyIsICJsc3Avbm9kZV9tb2R1bGVzL3ZzY29kZS1qc29ucnBjL2xpYi9jb21tb24vc2VtYXBob3JlLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9tZXNzYWdlUmVhZGVyLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9tZXNzYWdlV3JpdGVyLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9tZXNzYWdlQnVmZmVyLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9jb25uZWN0aW9uLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvbGliL2NvbW1vbi9hcGkuanMiLCAibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtanNvbnJwYy9saWIvYnJvd3Nlci9yaWwuanMiLCAibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtanNvbnJwYy9saWIvYnJvd3Nlci9tYWluLmpzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLWpzb25ycGMvYnJvd3Nlci5qcyIsICJsc3AvamF2YS1sc3AtY2xpZW50LnRzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLXdzLWpzb25ycGMvc3JjL2Rpc3Bvc2FibGUudHMiLCAibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtd3MtanNvbnJwYy9zcmMvc29ja2V0L3NvY2tldC50cyIsICJsc3Avbm9kZV9tb2R1bGVzL3ZzY29kZS13cy1qc29ucnBjL3NyYy9zb2NrZXQvcmVhZGVyLnRzIiwgImxzcC9ub2RlX21vZHVsZXMvdnNjb2RlLXdzLWpzb25ycGMvc3JjL3NvY2tldC93cml0ZXIudHMiLCAibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtd3MtanNvbnJwYy9zcmMvc29ja2V0L2Nvbm5lY3Rpb24udHMiLCAibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtd3MtanNvbnJwYy9zcmMvY29ubmVjdGlvbi50cyIsICJsc3AvbW9uYWNvLXNoaW0udHMiLCAibHNwL25vZGVfbW9kdWxlcy92c2NvZGUtbGFuZ3VhZ2VzZXJ2ZXItdHlwZXMvbGliL2VzbS9tYWluLmpzIl0sCiAgInNvdXJjZXNDb250ZW50IjogWyJcInVzZSBzdHJpY3RcIjtcbi8qIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiBDb3B5cmlnaHQgKGMpIE1pY3Jvc29mdCBDb3Jwb3JhdGlvbi4gQWxsIHJpZ2h0cyByZXNlcnZlZC5cbiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBNSVQgTGljZW5zZS4gU2VlIExpY2Vuc2UudHh0IGluIHRoZSBwcm9qZWN0IHJvb3QgZm9yIGxpY2Vuc2UgaW5mb3JtYXRpb24uXG4gKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0gKi9cbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIl9fZXNNb2R1bGVcIiwgeyB2YWx1ZTogdHJ1ZSB9KTtcbmV4cG9ydHMuc3RyaW5nQXJyYXkgPSBleHBvcnRzLmFycmF5ID0gZXhwb3J0cy5mdW5jID0gZXhwb3J0cy5lcnJvciA9IGV4cG9ydHMubnVtYmVyID0gZXhwb3J0cy5zdHJpbmcgPSBleHBvcnRzLmJvb2xlYW4gPSB2b2lkIDA7XG5mdW5jdGlvbiBib29sZWFuKHZhbHVlKSB7XG4gICAgcmV0dXJuIHZhbHVlID09PSB0cnVlIHx8IHZhbHVlID09PSBmYWxzZTtcbn1cbmV4cG9ydHMuYm9vbGVhbiA9IGJvb2xlYW47XG5mdW5jdGlvbiBzdHJpbmcodmFsdWUpIHtcbiAgICByZXR1cm4gdHlwZW9mIHZhbHVlID09PSAnc3RyaW5nJyB8fCB2YWx1ZSBpbnN0YW5jZW9mIFN0cmluZztcbn1cbmV4cG9ydHMuc3RyaW5nID0gc3RyaW5nO1xuZnVuY3Rpb24gbnVtYmVyKHZhbHVlKSB7XG4gICAgcmV0dXJuIHR5cGVvZiB2YWx1ZSA9PT0gJ251bWJlcicgfHwgdmFsdWUgaW5zdGFuY2VvZiBOdW1iZXI7XG59XG5leHBvcnRzLm51bWJlciA9IG51bWJlcjtcbmZ1bmN0aW9uIGVycm9yKHZhbHVlKSB7XG4gICAgcmV0dXJuIHZhbHVlIGluc3RhbmNlb2YgRXJyb3I7XG59XG5leHBvcnRzLmVycm9yID0gZXJyb3I7XG5mdW5jdGlvbiBmdW5jKHZhbHVlKSB7XG4gICAgcmV0dXJuIHR5cGVvZiB2YWx1ZSA9PT0gJ2Z1bmN0aW9uJztcbn1cbmV4cG9ydHMuZnVuYyA9IGZ1bmM7XG5mdW5jdGlvbiBhcnJheSh2YWx1ZSkge1xuICAgIHJldHVybiBBcnJheS5pc0FycmF5KHZhbHVlKTtcbn1cbmV4cG9ydHMuYXJyYXkgPSBhcnJheTtcbmZ1bmN0aW9uIHN0cmluZ0FycmF5KHZhbHVlKSB7XG4gICAgcmV0dXJuIGFycmF5KHZhbHVlKSAmJiB2YWx1ZS5ldmVyeShlbGVtID0+IHN0cmluZyhlbGVtKSk7XG59XG5leHBvcnRzLnN0cmluZ0FycmF5ID0gc3RyaW5nQXJyYXk7XG4iLCAiXCJ1c2Ugc3RyaWN0XCI7XG4vKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJfX2VzTW9kdWxlXCIsIHsgdmFsdWU6IHRydWUgfSk7XG5leHBvcnRzLk1lc3NhZ2UgPSBleHBvcnRzLk5vdGlmaWNhdGlvblR5cGU5ID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlOCA9IGV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTcgPSBleHBvcnRzLk5vdGlmaWNhdGlvblR5cGU2ID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlNSA9IGV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTQgPSBleHBvcnRzLk5vdGlmaWNhdGlvblR5cGUzID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlMiA9IGV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTEgPSBleHBvcnRzLk5vdGlmaWNhdGlvblR5cGUwID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlID0gZXhwb3J0cy5SZXF1ZXN0VHlwZTkgPSBleHBvcnRzLlJlcXVlc3RUeXBlOCA9IGV4cG9ydHMuUmVxdWVzdFR5cGU3ID0gZXhwb3J0cy5SZXF1ZXN0VHlwZTYgPSBleHBvcnRzLlJlcXVlc3RUeXBlNSA9IGV4cG9ydHMuUmVxdWVzdFR5cGU0ID0gZXhwb3J0cy5SZXF1ZXN0VHlwZTMgPSBleHBvcnRzLlJlcXVlc3RUeXBlMiA9IGV4cG9ydHMuUmVxdWVzdFR5cGUxID0gZXhwb3J0cy5SZXF1ZXN0VHlwZSA9IGV4cG9ydHMuUmVxdWVzdFR5cGUwID0gZXhwb3J0cy5BYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUgPSBleHBvcnRzLlBhcmFtZXRlclN0cnVjdHVyZXMgPSBleHBvcnRzLlJlc3BvbnNlRXJyb3IgPSBleHBvcnRzLkVycm9yQ29kZXMgPSB2b2lkIDA7XG5jb25zdCBpcyA9IHJlcXVpcmUoXCIuL2lzXCIpO1xuLyoqXG4gKiBQcmVkZWZpbmVkIGVycm9yIGNvZGVzLlxuICovXG52YXIgRXJyb3JDb2RlcztcbihmdW5jdGlvbiAoRXJyb3JDb2Rlcykge1xuICAgIC8vIERlZmluZWQgYnkgSlNPTiBSUENcbiAgICBFcnJvckNvZGVzLlBhcnNlRXJyb3IgPSAtMzI3MDA7XG4gICAgRXJyb3JDb2Rlcy5JbnZhbGlkUmVxdWVzdCA9IC0zMjYwMDtcbiAgICBFcnJvckNvZGVzLk1ldGhvZE5vdEZvdW5kID0gLTMyNjAxO1xuICAgIEVycm9yQ29kZXMuSW52YWxpZFBhcmFtcyA9IC0zMjYwMjtcbiAgICBFcnJvckNvZGVzLkludGVybmFsRXJyb3IgPSAtMzI2MDM7XG4gICAgLyoqXG4gICAgICogVGhpcyBpcyB0aGUgc3RhcnQgcmFuZ2Ugb2YgSlNPTiBSUEMgcmVzZXJ2ZWQgZXJyb3IgY29kZXMuXG4gICAgICogSXQgZG9lc24ndCBkZW5vdGUgYSByZWFsIGVycm9yIGNvZGUuIE5vIGFwcGxpY2F0aW9uIGVycm9yIGNvZGVzIHNob3VsZFxuICAgICAqIGJlIGRlZmluZWQgYmV0d2VlbiB0aGUgc3RhcnQgYW5kIGVuZCByYW5nZS4gRm9yIGJhY2t3YXJkc1xuICAgICAqIGNvbXBhdGliaWxpdHkgdGhlIGBTZXJ2ZXJOb3RJbml0aWFsaXplZGAgYW5kIHRoZSBgVW5rbm93bkVycm9yQ29kZWBcbiAgICAgKiBhcmUgbGVmdCBpbiB0aGUgcmFuZ2UuXG4gICAgICpcbiAgICAgKiBAc2luY2UgMy4xNi4wXG4gICAgKi9cbiAgICBFcnJvckNvZGVzLmpzb25ycGNSZXNlcnZlZEVycm9yUmFuZ2VTdGFydCA9IC0zMjA5OTtcbiAgICAvKiogQGRlcHJlY2F0ZWQgdXNlICBqc29ucnBjUmVzZXJ2ZWRFcnJvclJhbmdlU3RhcnQgKi9cbiAgICBFcnJvckNvZGVzLnNlcnZlckVycm9yU3RhcnQgPSAtMzIwOTk7XG4gICAgLyoqXG4gICAgICogQW4gZXJyb3Igb2NjdXJyZWQgd2hlbiB3cml0ZSBhIG1lc3NhZ2UgdG8gdGhlIHRyYW5zcG9ydCBsYXllci5cbiAgICAgKi9cbiAgICBFcnJvckNvZGVzLk1lc3NhZ2VXcml0ZUVycm9yID0gLTMyMDk5O1xuICAgIC8qKlxuICAgICAqIEFuIGVycm9yIG9jY3VycmVkIHdoZW4gcmVhZGluZyBhIG1lc3NhZ2UgZnJvbSB0aGUgdHJhbnNwb3J0IGxheWVyLlxuICAgICAqL1xuICAgIEVycm9yQ29kZXMuTWVzc2FnZVJlYWRFcnJvciA9IC0zMjA5ODtcbiAgICAvKipcbiAgICAgKiBUaGUgY29ubmVjdGlvbiBnb3QgZGlzcG9zZWQgb3IgbG9zdCBhbmQgYWxsIHBlbmRpbmcgcmVzcG9uc2VzIGdvdFxuICAgICAqIHJlamVjdGVkLlxuICAgICAqL1xuICAgIEVycm9yQ29kZXMuUGVuZGluZ1Jlc3BvbnNlUmVqZWN0ZWQgPSAtMzIwOTc7XG4gICAgLyoqXG4gICAgICogVGhlIGNvbm5lY3Rpb24gaXMgaW5hY3RpdmUgYW5kIGEgdXNlIG9mIGl0IGZhaWxlZC5cbiAgICAgKi9cbiAgICBFcnJvckNvZGVzLkNvbm5lY3Rpb25JbmFjdGl2ZSA9IC0zMjA5NjtcbiAgICAvKipcbiAgICAgKiBFcnJvciBjb2RlIGluZGljYXRpbmcgdGhhdCBhIHNlcnZlciByZWNlaXZlZCBhIG5vdGlmaWNhdGlvbiBvclxuICAgICAqIHJlcXVlc3QgYmVmb3JlIHRoZSBzZXJ2ZXIgaGFzIHJlY2VpdmVkIHRoZSBgaW5pdGlhbGl6ZWAgcmVxdWVzdC5cbiAgICAgKi9cbiAgICBFcnJvckNvZGVzLlNlcnZlck5vdEluaXRpYWxpemVkID0gLTMyMDAyO1xuICAgIEVycm9yQ29kZXMuVW5rbm93bkVycm9yQ29kZSA9IC0zMjAwMTtcbiAgICAvKipcbiAgICAgKiBUaGlzIGlzIHRoZSBlbmQgcmFuZ2Ugb2YgSlNPTiBSUEMgcmVzZXJ2ZWQgZXJyb3IgY29kZXMuXG4gICAgICogSXQgZG9lc24ndCBkZW5vdGUgYSByZWFsIGVycm9yIGNvZGUuXG4gICAgICpcbiAgICAgKiBAc2luY2UgMy4xNi4wXG4gICAgKi9cbiAgICBFcnJvckNvZGVzLmpzb25ycGNSZXNlcnZlZEVycm9yUmFuZ2VFbmQgPSAtMzIwMDA7XG4gICAgLyoqIEBkZXByZWNhdGVkIHVzZSAganNvbnJwY1Jlc2VydmVkRXJyb3JSYW5nZUVuZCAqL1xuICAgIEVycm9yQ29kZXMuc2VydmVyRXJyb3JFbmQgPSAtMzIwMDA7XG59KShFcnJvckNvZGVzIHx8IChleHBvcnRzLkVycm9yQ29kZXMgPSBFcnJvckNvZGVzID0ge30pKTtcbi8qKlxuICogQW4gZXJyb3Igb2JqZWN0IHJldHVybiBpbiBhIHJlc3BvbnNlIGluIGNhc2UgYSByZXF1ZXN0XG4gKiBoYXMgZmFpbGVkLlxuICovXG5jbGFzcyBSZXNwb25zZUVycm9yIGV4dGVuZHMgRXJyb3Ige1xuICAgIGNvbnN0cnVjdG9yKGNvZGUsIG1lc3NhZ2UsIGRhdGEpIHtcbiAgICAgICAgc3VwZXIobWVzc2FnZSk7XG4gICAgICAgIHRoaXMuY29kZSA9IGlzLm51bWJlcihjb2RlKSA/IGNvZGUgOiBFcnJvckNvZGVzLlVua25vd25FcnJvckNvZGU7XG4gICAgICAgIHRoaXMuZGF0YSA9IGRhdGE7XG4gICAgICAgIE9iamVjdC5zZXRQcm90b3R5cGVPZih0aGlzLCBSZXNwb25zZUVycm9yLnByb3RvdHlwZSk7XG4gICAgfVxuICAgIHRvSnNvbigpIHtcbiAgICAgICAgY29uc3QgcmVzdWx0ID0ge1xuICAgICAgICAgICAgY29kZTogdGhpcy5jb2RlLFxuICAgICAgICAgICAgbWVzc2FnZTogdGhpcy5tZXNzYWdlXG4gICAgICAgIH07XG4gICAgICAgIGlmICh0aGlzLmRhdGEgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmVzdWx0LmRhdGEgPSB0aGlzLmRhdGE7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICB9XG59XG5leHBvcnRzLlJlc3BvbnNlRXJyb3IgPSBSZXNwb25zZUVycm9yO1xuY2xhc3MgUGFyYW1ldGVyU3RydWN0dXJlcyB7XG4gICAgY29uc3RydWN0b3Ioa2luZCkge1xuICAgICAgICB0aGlzLmtpbmQgPSBraW5kO1xuICAgIH1cbiAgICBzdGF0aWMgaXModmFsdWUpIHtcbiAgICAgICAgcmV0dXJuIHZhbHVlID09PSBQYXJhbWV0ZXJTdHJ1Y3R1cmVzLmF1dG8gfHwgdmFsdWUgPT09IFBhcmFtZXRlclN0cnVjdHVyZXMuYnlOYW1lIHx8IHZhbHVlID09PSBQYXJhbWV0ZXJTdHJ1Y3R1cmVzLmJ5UG9zaXRpb247XG4gICAgfVxuICAgIHRvU3RyaW5nKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5raW5kO1xuICAgIH1cbn1cbmV4cG9ydHMuUGFyYW1ldGVyU3RydWN0dXJlcyA9IFBhcmFtZXRlclN0cnVjdHVyZXM7XG4vKipcbiAqIFRoZSBwYXJhbWV0ZXIgc3RydWN0dXJlIGlzIGF1dG9tYXRpY2FsbHkgaW5mZXJyZWQgb24gdGhlIG51bWJlciBvZiBwYXJhbWV0ZXJzXG4gKiBhbmQgdGhlIHBhcmFtZXRlciB0eXBlIGluIGNhc2Ugb2YgYSBzaW5nbGUgcGFyYW0uXG4gKi9cblBhcmFtZXRlclN0cnVjdHVyZXMuYXV0byA9IG5ldyBQYXJhbWV0ZXJTdHJ1Y3R1cmVzKCdhdXRvJyk7XG4vKipcbiAqIEZvcmNlcyBgYnlQb3NpdGlvbmAgcGFyYW1ldGVyIHN0cnVjdHVyZS4gVGhpcyBpcyB1c2VmdWwgaWYgeW91IGhhdmUgYSBzaW5nbGVcbiAqIHBhcmFtZXRlciB3aGljaCBoYXMgYSBsaXRlcmFsIHR5cGUuXG4gKi9cblBhcmFtZXRlclN0cnVjdHVyZXMuYnlQb3NpdGlvbiA9IG5ldyBQYXJhbWV0ZXJTdHJ1Y3R1cmVzKCdieVBvc2l0aW9uJyk7XG4vKipcbiAqIEZvcmNlcyBgYnlOYW1lYCBwYXJhbWV0ZXIgc3RydWN0dXJlLiBUaGlzIGlzIG9ubHkgdXNlZnVsIHdoZW4gaGF2aW5nIGEgc2luZ2xlXG4gKiBwYXJhbWV0ZXIuIFRoZSBsaWJyYXJ5IHdpbGwgcmVwb3J0IGVycm9ycyBpZiB1c2VkIHdpdGggYSBkaWZmZXJlbnQgbnVtYmVyIG9mXG4gKiBwYXJhbWV0ZXJzLlxuICovXG5QYXJhbWV0ZXJTdHJ1Y3R1cmVzLmJ5TmFtZSA9IG5ldyBQYXJhbWV0ZXJTdHJ1Y3R1cmVzKCdieU5hbWUnKTtcbi8qKlxuICogQW4gYWJzdHJhY3QgaW1wbGVtZW50YXRpb24gb2YgYSBNZXNzYWdlVHlwZS5cbiAqL1xuY2xhc3MgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QsIG51bWJlck9mUGFyYW1zKSB7XG4gICAgICAgIHRoaXMubWV0aG9kID0gbWV0aG9kO1xuICAgICAgICB0aGlzLm51bWJlck9mUGFyYW1zID0gbnVtYmVyT2ZQYXJhbXM7XG4gICAgfVxuICAgIGdldCBwYXJhbWV0ZXJTdHJ1Y3R1cmVzKCkge1xuICAgICAgICByZXR1cm4gUGFyYW1ldGVyU3RydWN0dXJlcy5hdXRvO1xuICAgIH1cbn1cbmV4cG9ydHMuQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlID0gQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlO1xuLyoqXG4gKiBDbGFzc2VzIHRvIHR5cGUgcmVxdWVzdCByZXNwb25zZSBwYWlyc1xuICovXG5jbGFzcyBSZXF1ZXN0VHlwZTAgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCkge1xuICAgICAgICBzdXBlcihtZXRob2QsIDApO1xuICAgIH1cbn1cbmV4cG9ydHMuUmVxdWVzdFR5cGUwID0gUmVxdWVzdFR5cGUwO1xuY2xhc3MgUmVxdWVzdFR5cGUgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCwgX3BhcmFtZXRlclN0cnVjdHVyZXMgPSBQYXJhbWV0ZXJTdHJ1Y3R1cmVzLmF1dG8pIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCAxKTtcbiAgICAgICAgdGhpcy5fcGFyYW1ldGVyU3RydWN0dXJlcyA9IF9wYXJhbWV0ZXJTdHJ1Y3R1cmVzO1xuICAgIH1cbiAgICBnZXQgcGFyYW1ldGVyU3RydWN0dXJlcygpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX3BhcmFtZXRlclN0cnVjdHVyZXM7XG4gICAgfVxufVxuZXhwb3J0cy5SZXF1ZXN0VHlwZSA9IFJlcXVlc3RUeXBlO1xuY2xhc3MgUmVxdWVzdFR5cGUxIGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QsIF9wYXJhbWV0ZXJTdHJ1Y3R1cmVzID0gUGFyYW1ldGVyU3RydWN0dXJlcy5hdXRvKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgMSk7XG4gICAgICAgIHRoaXMuX3BhcmFtZXRlclN0cnVjdHVyZXMgPSBfcGFyYW1ldGVyU3RydWN0dXJlcztcbiAgICB9XG4gICAgZ2V0IHBhcmFtZXRlclN0cnVjdHVyZXMoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLl9wYXJhbWV0ZXJTdHJ1Y3R1cmVzO1xuICAgIH1cbn1cbmV4cG9ydHMuUmVxdWVzdFR5cGUxID0gUmVxdWVzdFR5cGUxO1xuY2xhc3MgUmVxdWVzdFR5cGUyIGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QpIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCAyKTtcbiAgICB9XG59XG5leHBvcnRzLlJlcXVlc3RUeXBlMiA9IFJlcXVlc3RUeXBlMjtcbmNsYXNzIFJlcXVlc3RUeXBlMyBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVNpZ25hdHVyZSB7XG4gICAgY29uc3RydWN0b3IobWV0aG9kKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgMyk7XG4gICAgfVxufVxuZXhwb3J0cy5SZXF1ZXN0VHlwZTMgPSBSZXF1ZXN0VHlwZTM7XG5jbGFzcyBSZXF1ZXN0VHlwZTQgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCkge1xuICAgICAgICBzdXBlcihtZXRob2QsIDQpO1xuICAgIH1cbn1cbmV4cG9ydHMuUmVxdWVzdFR5cGU0ID0gUmVxdWVzdFR5cGU0O1xuY2xhc3MgUmVxdWVzdFR5cGU1IGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QpIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCA1KTtcbiAgICB9XG59XG5leHBvcnRzLlJlcXVlc3RUeXBlNSA9IFJlcXVlc3RUeXBlNTtcbmNsYXNzIFJlcXVlc3RUeXBlNiBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVNpZ25hdHVyZSB7XG4gICAgY29uc3RydWN0b3IobWV0aG9kKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgNik7XG4gICAgfVxufVxuZXhwb3J0cy5SZXF1ZXN0VHlwZTYgPSBSZXF1ZXN0VHlwZTY7XG5jbGFzcyBSZXF1ZXN0VHlwZTcgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCkge1xuICAgICAgICBzdXBlcihtZXRob2QsIDcpO1xuICAgIH1cbn1cbmV4cG9ydHMuUmVxdWVzdFR5cGU3ID0gUmVxdWVzdFR5cGU3O1xuY2xhc3MgUmVxdWVzdFR5cGU4IGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QpIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCA4KTtcbiAgICB9XG59XG5leHBvcnRzLlJlcXVlc3RUeXBlOCA9IFJlcXVlc3RUeXBlODtcbmNsYXNzIFJlcXVlc3RUeXBlOSBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVNpZ25hdHVyZSB7XG4gICAgY29uc3RydWN0b3IobWV0aG9kKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgOSk7XG4gICAgfVxufVxuZXhwb3J0cy5SZXF1ZXN0VHlwZTkgPSBSZXF1ZXN0VHlwZTk7XG5jbGFzcyBOb3RpZmljYXRpb25UeXBlIGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QsIF9wYXJhbWV0ZXJTdHJ1Y3R1cmVzID0gUGFyYW1ldGVyU3RydWN0dXJlcy5hdXRvKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgMSk7XG4gICAgICAgIHRoaXMuX3BhcmFtZXRlclN0cnVjdHVyZXMgPSBfcGFyYW1ldGVyU3RydWN0dXJlcztcbiAgICB9XG4gICAgZ2V0IHBhcmFtZXRlclN0cnVjdHVyZXMoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLl9wYXJhbWV0ZXJTdHJ1Y3R1cmVzO1xuICAgIH1cbn1cbmV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZSA9IE5vdGlmaWNhdGlvblR5cGU7XG5jbGFzcyBOb3RpZmljYXRpb25UeXBlMCBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVNpZ25hdHVyZSB7XG4gICAgY29uc3RydWN0b3IobWV0aG9kKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgMCk7XG4gICAgfVxufVxuZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlMCA9IE5vdGlmaWNhdGlvblR5cGUwO1xuY2xhc3MgTm90aWZpY2F0aW9uVHlwZTEgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCwgX3BhcmFtZXRlclN0cnVjdHVyZXMgPSBQYXJhbWV0ZXJTdHJ1Y3R1cmVzLmF1dG8pIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCAxKTtcbiAgICAgICAgdGhpcy5fcGFyYW1ldGVyU3RydWN0dXJlcyA9IF9wYXJhbWV0ZXJTdHJ1Y3R1cmVzO1xuICAgIH1cbiAgICBnZXQgcGFyYW1ldGVyU3RydWN0dXJlcygpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX3BhcmFtZXRlclN0cnVjdHVyZXM7XG4gICAgfVxufVxuZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlMSA9IE5vdGlmaWNhdGlvblR5cGUxO1xuY2xhc3MgTm90aWZpY2F0aW9uVHlwZTIgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCkge1xuICAgICAgICBzdXBlcihtZXRob2QsIDIpO1xuICAgIH1cbn1cbmV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTIgPSBOb3RpZmljYXRpb25UeXBlMjtcbmNsYXNzIE5vdGlmaWNhdGlvblR5cGUzIGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QpIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCAzKTtcbiAgICB9XG59XG5leHBvcnRzLk5vdGlmaWNhdGlvblR5cGUzID0gTm90aWZpY2F0aW9uVHlwZTM7XG5jbGFzcyBOb3RpZmljYXRpb25UeXBlNCBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVNpZ25hdHVyZSB7XG4gICAgY29uc3RydWN0b3IobWV0aG9kKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgNCk7XG4gICAgfVxufVxuZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlNCA9IE5vdGlmaWNhdGlvblR5cGU0O1xuY2xhc3MgTm90aWZpY2F0aW9uVHlwZTUgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCkge1xuICAgICAgICBzdXBlcihtZXRob2QsIDUpO1xuICAgIH1cbn1cbmV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTUgPSBOb3RpZmljYXRpb25UeXBlNTtcbmNsYXNzIE5vdGlmaWNhdGlvblR5cGU2IGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QpIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCA2KTtcbiAgICB9XG59XG5leHBvcnRzLk5vdGlmaWNhdGlvblR5cGU2ID0gTm90aWZpY2F0aW9uVHlwZTY7XG5jbGFzcyBOb3RpZmljYXRpb25UeXBlNyBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVNpZ25hdHVyZSB7XG4gICAgY29uc3RydWN0b3IobWV0aG9kKSB7XG4gICAgICAgIHN1cGVyKG1ldGhvZCwgNyk7XG4gICAgfVxufVxuZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlNyA9IE5vdGlmaWNhdGlvblR5cGU3O1xuY2xhc3MgTm90aWZpY2F0aW9uVHlwZTggZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VTaWduYXR1cmUge1xuICAgIGNvbnN0cnVjdG9yKG1ldGhvZCkge1xuICAgICAgICBzdXBlcihtZXRob2QsIDgpO1xuICAgIH1cbn1cbmV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTggPSBOb3RpZmljYXRpb25UeXBlODtcbmNsYXNzIE5vdGlmaWNhdGlvblR5cGU5IGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlU2lnbmF0dXJlIHtcbiAgICBjb25zdHJ1Y3RvcihtZXRob2QpIHtcbiAgICAgICAgc3VwZXIobWV0aG9kLCA5KTtcbiAgICB9XG59XG5leHBvcnRzLk5vdGlmaWNhdGlvblR5cGU5ID0gTm90aWZpY2F0aW9uVHlwZTk7XG52YXIgTWVzc2FnZTtcbihmdW5jdGlvbiAoTWVzc2FnZSkge1xuICAgIC8qKlxuICAgICAqIFRlc3RzIGlmIHRoZSBnaXZlbiBtZXNzYWdlIGlzIGEgcmVxdWVzdCBtZXNzYWdlXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXNSZXF1ZXN0KG1lc3NhZ2UpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gbWVzc2FnZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiBpcy5zdHJpbmcoY2FuZGlkYXRlLm1ldGhvZCkgJiYgKGlzLnN0cmluZyhjYW5kaWRhdGUuaWQpIHx8IGlzLm51bWJlcihjYW5kaWRhdGUuaWQpKTtcbiAgICB9XG4gICAgTWVzc2FnZS5pc1JlcXVlc3QgPSBpc1JlcXVlc3Q7XG4gICAgLyoqXG4gICAgICogVGVzdHMgaWYgdGhlIGdpdmVuIG1lc3NhZ2UgaXMgYSBub3RpZmljYXRpb24gbWVzc2FnZVxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzTm90aWZpY2F0aW9uKG1lc3NhZ2UpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gbWVzc2FnZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiBpcy5zdHJpbmcoY2FuZGlkYXRlLm1ldGhvZCkgJiYgbWVzc2FnZS5pZCA9PT0gdm9pZCAwO1xuICAgIH1cbiAgICBNZXNzYWdlLmlzTm90aWZpY2F0aW9uID0gaXNOb3RpZmljYXRpb247XG4gICAgLyoqXG4gICAgICogVGVzdHMgaWYgdGhlIGdpdmVuIG1lc3NhZ2UgaXMgYSByZXNwb25zZSBtZXNzYWdlXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXNSZXNwb25zZShtZXNzYWdlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IG1lc3NhZ2U7XG4gICAgICAgIHJldHVybiBjYW5kaWRhdGUgJiYgKGNhbmRpZGF0ZS5yZXN1bHQgIT09IHZvaWQgMCB8fCAhIWNhbmRpZGF0ZS5lcnJvcikgJiYgKGlzLnN0cmluZyhjYW5kaWRhdGUuaWQpIHx8IGlzLm51bWJlcihjYW5kaWRhdGUuaWQpIHx8IGNhbmRpZGF0ZS5pZCA9PT0gbnVsbCk7XG4gICAgfVxuICAgIE1lc3NhZ2UuaXNSZXNwb25zZSA9IGlzUmVzcG9uc2U7XG59KShNZXNzYWdlIHx8IChleHBvcnRzLk1lc3NhZ2UgPSBNZXNzYWdlID0ge30pKTtcbiIsICJcInVzZSBzdHJpY3RcIjtcbi8qLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiAgQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiAgTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTGljZW5zZS50eHQgaW4gdGhlIHByb2plY3Qgcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0qL1xudmFyIF9hO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiX19lc01vZHVsZVwiLCB7IHZhbHVlOiB0cnVlIH0pO1xuZXhwb3J0cy5MUlVDYWNoZSA9IGV4cG9ydHMuTGlua2VkTWFwID0gZXhwb3J0cy5Ub3VjaCA9IHZvaWQgMDtcbnZhciBUb3VjaDtcbihmdW5jdGlvbiAoVG91Y2gpIHtcbiAgICBUb3VjaC5Ob25lID0gMDtcbiAgICBUb3VjaC5GaXJzdCA9IDE7XG4gICAgVG91Y2guQXNPbGQgPSBUb3VjaC5GaXJzdDtcbiAgICBUb3VjaC5MYXN0ID0gMjtcbiAgICBUb3VjaC5Bc05ldyA9IFRvdWNoLkxhc3Q7XG59KShUb3VjaCB8fCAoZXhwb3J0cy5Ub3VjaCA9IFRvdWNoID0ge30pKTtcbmNsYXNzIExpbmtlZE1hcCB7XG4gICAgY29uc3RydWN0b3IoKSB7XG4gICAgICAgIHRoaXNbX2FdID0gJ0xpbmtlZE1hcCc7XG4gICAgICAgIHRoaXMuX21hcCA9IG5ldyBNYXAoKTtcbiAgICAgICAgdGhpcy5faGVhZCA9IHVuZGVmaW5lZDtcbiAgICAgICAgdGhpcy5fdGFpbCA9IHVuZGVmaW5lZDtcbiAgICAgICAgdGhpcy5fc2l6ZSA9IDA7XG4gICAgICAgIHRoaXMuX3N0YXRlID0gMDtcbiAgICB9XG4gICAgY2xlYXIoKSB7XG4gICAgICAgIHRoaXMuX21hcC5jbGVhcigpO1xuICAgICAgICB0aGlzLl9oZWFkID0gdW5kZWZpbmVkO1xuICAgICAgICB0aGlzLl90YWlsID0gdW5kZWZpbmVkO1xuICAgICAgICB0aGlzLl9zaXplID0gMDtcbiAgICAgICAgdGhpcy5fc3RhdGUrKztcbiAgICB9XG4gICAgaXNFbXB0eSgpIHtcbiAgICAgICAgcmV0dXJuICF0aGlzLl9oZWFkICYmICF0aGlzLl90YWlsO1xuICAgIH1cbiAgICBnZXQgc2l6ZSgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX3NpemU7XG4gICAgfVxuICAgIGdldCBmaXJzdCgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX2hlYWQ/LnZhbHVlO1xuICAgIH1cbiAgICBnZXQgbGFzdCgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX3RhaWw/LnZhbHVlO1xuICAgIH1cbiAgICBoYXMoa2V5KSB7XG4gICAgICAgIHJldHVybiB0aGlzLl9tYXAuaGFzKGtleSk7XG4gICAgfVxuICAgIGdldChrZXksIHRvdWNoID0gVG91Y2guTm9uZSkge1xuICAgICAgICBjb25zdCBpdGVtID0gdGhpcy5fbWFwLmdldChrZXkpO1xuICAgICAgICBpZiAoIWl0ZW0pIHtcbiAgICAgICAgICAgIHJldHVybiB1bmRlZmluZWQ7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKHRvdWNoICE9PSBUb3VjaC5Ob25lKSB7XG4gICAgICAgICAgICB0aGlzLnRvdWNoKGl0ZW0sIHRvdWNoKTtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gaXRlbS52YWx1ZTtcbiAgICB9XG4gICAgc2V0KGtleSwgdmFsdWUsIHRvdWNoID0gVG91Y2guTm9uZSkge1xuICAgICAgICBsZXQgaXRlbSA9IHRoaXMuX21hcC5nZXQoa2V5KTtcbiAgICAgICAgaWYgKGl0ZW0pIHtcbiAgICAgICAgICAgIGl0ZW0udmFsdWUgPSB2YWx1ZTtcbiAgICAgICAgICAgIGlmICh0b3VjaCAhPT0gVG91Y2guTm9uZSkge1xuICAgICAgICAgICAgICAgIHRoaXMudG91Y2goaXRlbSwgdG91Y2gpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgaXRlbSA9IHsga2V5LCB2YWx1ZSwgbmV4dDogdW5kZWZpbmVkLCBwcmV2aW91czogdW5kZWZpbmVkIH07XG4gICAgICAgICAgICBzd2l0Y2ggKHRvdWNoKSB7XG4gICAgICAgICAgICAgICAgY2FzZSBUb3VjaC5Ob25lOlxuICAgICAgICAgICAgICAgICAgICB0aGlzLmFkZEl0ZW1MYXN0KGl0ZW0pO1xuICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICBjYXNlIFRvdWNoLkZpcnN0OlxuICAgICAgICAgICAgICAgICAgICB0aGlzLmFkZEl0ZW1GaXJzdChpdGVtKTtcbiAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgY2FzZSBUb3VjaC5MYXN0OlxuICAgICAgICAgICAgICAgICAgICB0aGlzLmFkZEl0ZW1MYXN0KGl0ZW0pO1xuICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICBkZWZhdWx0OlxuICAgICAgICAgICAgICAgICAgICB0aGlzLmFkZEl0ZW1MYXN0KGl0ZW0pO1xuICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHRoaXMuX21hcC5zZXQoa2V5LCBpdGVtKTtcbiAgICAgICAgICAgIHRoaXMuX3NpemUrKztcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gdGhpcztcbiAgICB9XG4gICAgZGVsZXRlKGtleSkge1xuICAgICAgICByZXR1cm4gISF0aGlzLnJlbW92ZShrZXkpO1xuICAgIH1cbiAgICByZW1vdmUoa2V5KSB7XG4gICAgICAgIGNvbnN0IGl0ZW0gPSB0aGlzLl9tYXAuZ2V0KGtleSk7XG4gICAgICAgIGlmICghaXRlbSkge1xuICAgICAgICAgICAgcmV0dXJuIHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLl9tYXAuZGVsZXRlKGtleSk7XG4gICAgICAgIHRoaXMucmVtb3ZlSXRlbShpdGVtKTtcbiAgICAgICAgdGhpcy5fc2l6ZS0tO1xuICAgICAgICByZXR1cm4gaXRlbS52YWx1ZTtcbiAgICB9XG4gICAgc2hpZnQoKSB7XG4gICAgICAgIGlmICghdGhpcy5faGVhZCAmJiAhdGhpcy5fdGFpbCkge1xuICAgICAgICAgICAgcmV0dXJuIHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgICAgICBpZiAoIXRoaXMuX2hlYWQgfHwgIXRoaXMuX3RhaWwpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcignSW52YWxpZCBsaXN0Jyk7XG4gICAgICAgIH1cbiAgICAgICAgY29uc3QgaXRlbSA9IHRoaXMuX2hlYWQ7XG4gICAgICAgIHRoaXMuX21hcC5kZWxldGUoaXRlbS5rZXkpO1xuICAgICAgICB0aGlzLnJlbW92ZUl0ZW0oaXRlbSk7XG4gICAgICAgIHRoaXMuX3NpemUtLTtcbiAgICAgICAgcmV0dXJuIGl0ZW0udmFsdWU7XG4gICAgfVxuICAgIGZvckVhY2goY2FsbGJhY2tmbiwgdGhpc0FyZykge1xuICAgICAgICBjb25zdCBzdGF0ZSA9IHRoaXMuX3N0YXRlO1xuICAgICAgICBsZXQgY3VycmVudCA9IHRoaXMuX2hlYWQ7XG4gICAgICAgIHdoaWxlIChjdXJyZW50KSB7XG4gICAgICAgICAgICBpZiAodGhpc0FyZykge1xuICAgICAgICAgICAgICAgIGNhbGxiYWNrZm4uYmluZCh0aGlzQXJnKShjdXJyZW50LnZhbHVlLCBjdXJyZW50LmtleSwgdGhpcyk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICBjYWxsYmFja2ZuKGN1cnJlbnQudmFsdWUsIGN1cnJlbnQua2V5LCB0aGlzKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmICh0aGlzLl9zdGF0ZSAhPT0gc3RhdGUpIHtcbiAgICAgICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYExpbmtlZE1hcCBnb3QgbW9kaWZpZWQgZHVyaW5nIGl0ZXJhdGlvbi5gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGN1cnJlbnQgPSBjdXJyZW50Lm5leHQ7XG4gICAgICAgIH1cbiAgICB9XG4gICAga2V5cygpIHtcbiAgICAgICAgY29uc3Qgc3RhdGUgPSB0aGlzLl9zdGF0ZTtcbiAgICAgICAgbGV0IGN1cnJlbnQgPSB0aGlzLl9oZWFkO1xuICAgICAgICBjb25zdCBpdGVyYXRvciA9IHtcbiAgICAgICAgICAgIFtTeW1ib2wuaXRlcmF0b3JdOiAoKSA9PiB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIGl0ZXJhdG9yO1xuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIG5leHQ6ICgpID0+IHtcbiAgICAgICAgICAgICAgICBpZiAodGhpcy5fc3RhdGUgIT09IHN0YXRlKSB7XG4gICAgICAgICAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgTGlua2VkTWFwIGdvdCBtb2RpZmllZCBkdXJpbmcgaXRlcmF0aW9uLmApO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBpZiAoY3VycmVudCkge1xuICAgICAgICAgICAgICAgICAgICBjb25zdCByZXN1bHQgPSB7IHZhbHVlOiBjdXJyZW50LmtleSwgZG9uZTogZmFsc2UgfTtcbiAgICAgICAgICAgICAgICAgICAgY3VycmVudCA9IGN1cnJlbnQubmV4dDtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiB7IHZhbHVlOiB1bmRlZmluZWQsIGRvbmU6IHRydWUgfTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH07XG4gICAgICAgIHJldHVybiBpdGVyYXRvcjtcbiAgICB9XG4gICAgdmFsdWVzKCkge1xuICAgICAgICBjb25zdCBzdGF0ZSA9IHRoaXMuX3N0YXRlO1xuICAgICAgICBsZXQgY3VycmVudCA9IHRoaXMuX2hlYWQ7XG4gICAgICAgIGNvbnN0IGl0ZXJhdG9yID0ge1xuICAgICAgICAgICAgW1N5bWJvbC5pdGVyYXRvcl06ICgpID0+IHtcbiAgICAgICAgICAgICAgICByZXR1cm4gaXRlcmF0b3I7XG4gICAgICAgICAgICB9LFxuICAgICAgICAgICAgbmV4dDogKCkgPT4ge1xuICAgICAgICAgICAgICAgIGlmICh0aGlzLl9zdGF0ZSAhPT0gc3RhdGUpIHtcbiAgICAgICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBMaW5rZWRNYXAgZ290IG1vZGlmaWVkIGR1cmluZyBpdGVyYXRpb24uYCk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGlmIChjdXJyZW50KSB7XG4gICAgICAgICAgICAgICAgICAgIGNvbnN0IHJlc3VsdCA9IHsgdmFsdWU6IGN1cnJlbnQudmFsdWUsIGRvbmU6IGZhbHNlIH07XG4gICAgICAgICAgICAgICAgICAgIGN1cnJlbnQgPSBjdXJyZW50Lm5leHQ7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4geyB2YWx1ZTogdW5kZWZpbmVkLCBkb25lOiB0cnVlIH07XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICB9O1xuICAgICAgICByZXR1cm4gaXRlcmF0b3I7XG4gICAgfVxuICAgIGVudHJpZXMoKSB7XG4gICAgICAgIGNvbnN0IHN0YXRlID0gdGhpcy5fc3RhdGU7XG4gICAgICAgIGxldCBjdXJyZW50ID0gdGhpcy5faGVhZDtcbiAgICAgICAgY29uc3QgaXRlcmF0b3IgPSB7XG4gICAgICAgICAgICBbU3ltYm9sLml0ZXJhdG9yXTogKCkgPT4ge1xuICAgICAgICAgICAgICAgIHJldHVybiBpdGVyYXRvcjtcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICBuZXh0OiAoKSA9PiB7XG4gICAgICAgICAgICAgICAgaWYgKHRoaXMuX3N0YXRlICE9PSBzdGF0ZSkge1xuICAgICAgICAgICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYExpbmtlZE1hcCBnb3QgbW9kaWZpZWQgZHVyaW5nIGl0ZXJhdGlvbi5gKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgaWYgKGN1cnJlbnQpIHtcbiAgICAgICAgICAgICAgICAgICAgY29uc3QgcmVzdWx0ID0geyB2YWx1ZTogW2N1cnJlbnQua2V5LCBjdXJyZW50LnZhbHVlXSwgZG9uZTogZmFsc2UgfTtcbiAgICAgICAgICAgICAgICAgICAgY3VycmVudCA9IGN1cnJlbnQubmV4dDtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiB7IHZhbHVlOiB1bmRlZmluZWQsIGRvbmU6IHRydWUgfTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH07XG4gICAgICAgIHJldHVybiBpdGVyYXRvcjtcbiAgICB9XG4gICAgWyhfYSA9IFN5bWJvbC50b1N0cmluZ1RhZywgU3ltYm9sLml0ZXJhdG9yKV0oKSB7XG4gICAgICAgIHJldHVybiB0aGlzLmVudHJpZXMoKTtcbiAgICB9XG4gICAgdHJpbU9sZChuZXdTaXplKSB7XG4gICAgICAgIGlmIChuZXdTaXplID49IHRoaXMuc2l6ZSkge1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGlmIChuZXdTaXplID09PSAwKSB7XG4gICAgICAgICAgICB0aGlzLmNsZWFyKCk7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgICAgbGV0IGN1cnJlbnQgPSB0aGlzLl9oZWFkO1xuICAgICAgICBsZXQgY3VycmVudFNpemUgPSB0aGlzLnNpemU7XG4gICAgICAgIHdoaWxlIChjdXJyZW50ICYmIGN1cnJlbnRTaXplID4gbmV3U2l6ZSkge1xuICAgICAgICAgICAgdGhpcy5fbWFwLmRlbGV0ZShjdXJyZW50LmtleSk7XG4gICAgICAgICAgICBjdXJyZW50ID0gY3VycmVudC5uZXh0O1xuICAgICAgICAgICAgY3VycmVudFNpemUtLTtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLl9oZWFkID0gY3VycmVudDtcbiAgICAgICAgdGhpcy5fc2l6ZSA9IGN1cnJlbnRTaXplO1xuICAgICAgICBpZiAoY3VycmVudCkge1xuICAgICAgICAgICAgY3VycmVudC5wcmV2aW91cyA9IHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLl9zdGF0ZSsrO1xuICAgIH1cbiAgICBhZGRJdGVtRmlyc3QoaXRlbSkge1xuICAgICAgICAvLyBGaXJzdCB0aW1lIEluc2VydFxuICAgICAgICBpZiAoIXRoaXMuX2hlYWQgJiYgIXRoaXMuX3RhaWwpIHtcbiAgICAgICAgICAgIHRoaXMuX3RhaWwgPSBpdGVtO1xuICAgICAgICB9XG4gICAgICAgIGVsc2UgaWYgKCF0aGlzLl9oZWFkKSB7XG4gICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoJ0ludmFsaWQgbGlzdCcpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgaXRlbS5uZXh0ID0gdGhpcy5faGVhZDtcbiAgICAgICAgICAgIHRoaXMuX2hlYWQucHJldmlvdXMgPSBpdGVtO1xuICAgICAgICB9XG4gICAgICAgIHRoaXMuX2hlYWQgPSBpdGVtO1xuICAgICAgICB0aGlzLl9zdGF0ZSsrO1xuICAgIH1cbiAgICBhZGRJdGVtTGFzdChpdGVtKSB7XG4gICAgICAgIC8vIEZpcnN0IHRpbWUgSW5zZXJ0XG4gICAgICAgIGlmICghdGhpcy5faGVhZCAmJiAhdGhpcy5fdGFpbCkge1xuICAgICAgICAgICAgdGhpcy5faGVhZCA9IGl0ZW07XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSBpZiAoIXRoaXMuX3RhaWwpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcignSW52YWxpZCBsaXN0Jyk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBpdGVtLnByZXZpb3VzID0gdGhpcy5fdGFpbDtcbiAgICAgICAgICAgIHRoaXMuX3RhaWwubmV4dCA9IGl0ZW07XG4gICAgICAgIH1cbiAgICAgICAgdGhpcy5fdGFpbCA9IGl0ZW07XG4gICAgICAgIHRoaXMuX3N0YXRlKys7XG4gICAgfVxuICAgIHJlbW92ZUl0ZW0oaXRlbSkge1xuICAgICAgICBpZiAoaXRlbSA9PT0gdGhpcy5faGVhZCAmJiBpdGVtID09PSB0aGlzLl90YWlsKSB7XG4gICAgICAgICAgICB0aGlzLl9oZWFkID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgdGhpcy5fdGFpbCA9IHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIGlmIChpdGVtID09PSB0aGlzLl9oZWFkKSB7XG4gICAgICAgICAgICAvLyBUaGlzIGNhbiBvbmx5IGhhcHBlbmVkIGlmIHNpemUgPT09IDEgd2hpY2ggaXMgaGFuZGxlXG4gICAgICAgICAgICAvLyBieSB0aGUgY2FzZSBhYm92ZS5cbiAgICAgICAgICAgIGlmICghaXRlbS5uZXh0KSB7XG4gICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdJbnZhbGlkIGxpc3QnKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGl0ZW0ubmV4dC5wcmV2aW91cyA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIHRoaXMuX2hlYWQgPSBpdGVtLm5leHQ7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSBpZiAoaXRlbSA9PT0gdGhpcy5fdGFpbCkge1xuICAgICAgICAgICAgLy8gVGhpcyBjYW4gb25seSBoYXBwZW5lZCBpZiBzaXplID09PSAxIHdoaWNoIGlzIGhhbmRsZVxuICAgICAgICAgICAgLy8gYnkgdGhlIGNhc2UgYWJvdmUuXG4gICAgICAgICAgICBpZiAoIWl0ZW0ucHJldmlvdXMpIHtcbiAgICAgICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoJ0ludmFsaWQgbGlzdCcpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaXRlbS5wcmV2aW91cy5uZXh0ID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgdGhpcy5fdGFpbCA9IGl0ZW0ucHJldmlvdXM7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBjb25zdCBuZXh0ID0gaXRlbS5uZXh0O1xuICAgICAgICAgICAgY29uc3QgcHJldmlvdXMgPSBpdGVtLnByZXZpb3VzO1xuICAgICAgICAgICAgaWYgKCFuZXh0IHx8ICFwcmV2aW91cykge1xuICAgICAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcignSW52YWxpZCBsaXN0Jyk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBuZXh0LnByZXZpb3VzID0gcHJldmlvdXM7XG4gICAgICAgICAgICBwcmV2aW91cy5uZXh0ID0gbmV4dDtcbiAgICAgICAgfVxuICAgICAgICBpdGVtLm5leHQgPSB1bmRlZmluZWQ7XG4gICAgICAgIGl0ZW0ucHJldmlvdXMgPSB1bmRlZmluZWQ7XG4gICAgICAgIHRoaXMuX3N0YXRlKys7XG4gICAgfVxuICAgIHRvdWNoKGl0ZW0sIHRvdWNoKSB7XG4gICAgICAgIGlmICghdGhpcy5faGVhZCB8fCAhdGhpcy5fdGFpbCkge1xuICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdJbnZhbGlkIGxpc3QnKTtcbiAgICAgICAgfVxuICAgICAgICBpZiAoKHRvdWNoICE9PSBUb3VjaC5GaXJzdCAmJiB0b3VjaCAhPT0gVG91Y2guTGFzdCkpIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBpZiAodG91Y2ggPT09IFRvdWNoLkZpcnN0KSB7XG4gICAgICAgICAgICBpZiAoaXRlbSA9PT0gdGhpcy5faGVhZCkge1xuICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNvbnN0IG5leHQgPSBpdGVtLm5leHQ7XG4gICAgICAgICAgICBjb25zdCBwcmV2aW91cyA9IGl0ZW0ucHJldmlvdXM7XG4gICAgICAgICAgICAvLyBVbmxpbmsgdGhlIGl0ZW1cbiAgICAgICAgICAgIGlmIChpdGVtID09PSB0aGlzLl90YWlsKSB7XG4gICAgICAgICAgICAgICAgLy8gcHJldmlvdXMgbXVzdCBiZSBkZWZpbmVkIHNpbmNlIGl0ZW0gd2FzIG5vdCBoZWFkIGJ1dCBpcyB0YWlsXG4gICAgICAgICAgICAgICAgLy8gU28gdGhlcmUgYXJlIG1vcmUgdGhhbiBvbiBpdGVtIGluIHRoZSBtYXBcbiAgICAgICAgICAgICAgICBwcmV2aW91cy5uZXh0ID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgICAgIHRoaXMuX3RhaWwgPSBwcmV2aW91cztcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIC8vIEJvdGggbmV4dCBhbmQgcHJldmlvdXMgYXJlIG5vdCB1bmRlZmluZWQgc2luY2UgaXRlbSB3YXMgbmVpdGhlciBoZWFkIG5vciB0YWlsLlxuICAgICAgICAgICAgICAgIG5leHQucHJldmlvdXMgPSBwcmV2aW91cztcbiAgICAgICAgICAgICAgICBwcmV2aW91cy5uZXh0ID0gbmV4dDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIC8vIEluc2VydCB0aGUgbm9kZSBhdCBoZWFkXG4gICAgICAgICAgICBpdGVtLnByZXZpb3VzID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgaXRlbS5uZXh0ID0gdGhpcy5faGVhZDtcbiAgICAgICAgICAgIHRoaXMuX2hlYWQucHJldmlvdXMgPSBpdGVtO1xuICAgICAgICAgICAgdGhpcy5faGVhZCA9IGl0ZW07XG4gICAgICAgICAgICB0aGlzLl9zdGF0ZSsrO1xuICAgICAgICB9XG4gICAgICAgIGVsc2UgaWYgKHRvdWNoID09PSBUb3VjaC5MYXN0KSB7XG4gICAgICAgICAgICBpZiAoaXRlbSA9PT0gdGhpcy5fdGFpbCkge1xuICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNvbnN0IG5leHQgPSBpdGVtLm5leHQ7XG4gICAgICAgICAgICBjb25zdCBwcmV2aW91cyA9IGl0ZW0ucHJldmlvdXM7XG4gICAgICAgICAgICAvLyBVbmxpbmsgdGhlIGl0ZW0uXG4gICAgICAgICAgICBpZiAoaXRlbSA9PT0gdGhpcy5faGVhZCkge1xuICAgICAgICAgICAgICAgIC8vIG5leHQgbXVzdCBiZSBkZWZpbmVkIHNpbmNlIGl0ZW0gd2FzIG5vdCB0YWlsIGJ1dCBpcyBoZWFkXG4gICAgICAgICAgICAgICAgLy8gU28gdGhlcmUgYXJlIG1vcmUgdGhhbiBvbiBpdGVtIGluIHRoZSBtYXBcbiAgICAgICAgICAgICAgICBuZXh0LnByZXZpb3VzID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgICAgIHRoaXMuX2hlYWQgPSBuZXh0O1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgLy8gQm90aCBuZXh0IGFuZCBwcmV2aW91cyBhcmUgbm90IHVuZGVmaW5lZCBzaW5jZSBpdGVtIHdhcyBuZWl0aGVyIGhlYWQgbm9yIHRhaWwuXG4gICAgICAgICAgICAgICAgbmV4dC5wcmV2aW91cyA9IHByZXZpb3VzO1xuICAgICAgICAgICAgICAgIHByZXZpb3VzLm5leHQgPSBuZXh0O1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaXRlbS5uZXh0ID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgaXRlbS5wcmV2aW91cyA9IHRoaXMuX3RhaWw7XG4gICAgICAgICAgICB0aGlzLl90YWlsLm5leHQgPSBpdGVtO1xuICAgICAgICAgICAgdGhpcy5fdGFpbCA9IGl0ZW07XG4gICAgICAgICAgICB0aGlzLl9zdGF0ZSsrO1xuICAgICAgICB9XG4gICAgfVxuICAgIHRvSlNPTigpIHtcbiAgICAgICAgY29uc3QgZGF0YSA9IFtdO1xuICAgICAgICB0aGlzLmZvckVhY2goKHZhbHVlLCBrZXkpID0+IHtcbiAgICAgICAgICAgIGRhdGEucHVzaChba2V5LCB2YWx1ZV0pO1xuICAgICAgICB9KTtcbiAgICAgICAgcmV0dXJuIGRhdGE7XG4gICAgfVxuICAgIGZyb21KU09OKGRhdGEpIHtcbiAgICAgICAgdGhpcy5jbGVhcigpO1xuICAgICAgICBmb3IgKGNvbnN0IFtrZXksIHZhbHVlXSBvZiBkYXRhKSB7XG4gICAgICAgICAgICB0aGlzLnNldChrZXksIHZhbHVlKTtcbiAgICAgICAgfVxuICAgIH1cbn1cbmV4cG9ydHMuTGlua2VkTWFwID0gTGlua2VkTWFwO1xuY2xhc3MgTFJVQ2FjaGUgZXh0ZW5kcyBMaW5rZWRNYXAge1xuICAgIGNvbnN0cnVjdG9yKGxpbWl0LCByYXRpbyA9IDEpIHtcbiAgICAgICAgc3VwZXIoKTtcbiAgICAgICAgdGhpcy5fbGltaXQgPSBsaW1pdDtcbiAgICAgICAgdGhpcy5fcmF0aW8gPSBNYXRoLm1pbihNYXRoLm1heCgwLCByYXRpbyksIDEpO1xuICAgIH1cbiAgICBnZXQgbGltaXQoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLl9saW1pdDtcbiAgICB9XG4gICAgc2V0IGxpbWl0KGxpbWl0KSB7XG4gICAgICAgIHRoaXMuX2xpbWl0ID0gbGltaXQ7XG4gICAgICAgIHRoaXMuY2hlY2tUcmltKCk7XG4gICAgfVxuICAgIGdldCByYXRpbygpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX3JhdGlvO1xuICAgIH1cbiAgICBzZXQgcmF0aW8ocmF0aW8pIHtcbiAgICAgICAgdGhpcy5fcmF0aW8gPSBNYXRoLm1pbihNYXRoLm1heCgwLCByYXRpbyksIDEpO1xuICAgICAgICB0aGlzLmNoZWNrVHJpbSgpO1xuICAgIH1cbiAgICBnZXQoa2V5LCB0b3VjaCA9IFRvdWNoLkFzTmV3KSB7XG4gICAgICAgIHJldHVybiBzdXBlci5nZXQoa2V5LCB0b3VjaCk7XG4gICAgfVxuICAgIHBlZWsoa2V5KSB7XG4gICAgICAgIHJldHVybiBzdXBlci5nZXQoa2V5LCBUb3VjaC5Ob25lKTtcbiAgICB9XG4gICAgc2V0KGtleSwgdmFsdWUpIHtcbiAgICAgICAgc3VwZXIuc2V0KGtleSwgdmFsdWUsIFRvdWNoLkxhc3QpO1xuICAgICAgICB0aGlzLmNoZWNrVHJpbSgpO1xuICAgICAgICByZXR1cm4gdGhpcztcbiAgICB9XG4gICAgY2hlY2tUcmltKCkge1xuICAgICAgICBpZiAodGhpcy5zaXplID4gdGhpcy5fbGltaXQpIHtcbiAgICAgICAgICAgIHRoaXMudHJpbU9sZChNYXRoLnJvdW5kKHRoaXMuX2xpbWl0ICogdGhpcy5fcmF0aW8pKTtcbiAgICAgICAgfVxuICAgIH1cbn1cbmV4cG9ydHMuTFJVQ2FjaGUgPSBMUlVDYWNoZTtcbiIsICJcInVzZSBzdHJpY3RcIjtcbi8qLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiAgQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiAgTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTGljZW5zZS50eHQgaW4gdGhlIHByb2plY3Qgcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0qL1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiX19lc01vZHVsZVwiLCB7IHZhbHVlOiB0cnVlIH0pO1xuZXhwb3J0cy5EaXNwb3NhYmxlID0gdm9pZCAwO1xudmFyIERpc3Bvc2FibGU7XG4oZnVuY3Rpb24gKERpc3Bvc2FibGUpIHtcbiAgICBmdW5jdGlvbiBjcmVhdGUoZnVuYykge1xuICAgICAgICByZXR1cm4ge1xuICAgICAgICAgICAgZGlzcG9zZTogZnVuY1xuICAgICAgICB9O1xuICAgIH1cbiAgICBEaXNwb3NhYmxlLmNyZWF0ZSA9IGNyZWF0ZTtcbn0pKERpc3Bvc2FibGUgfHwgKGV4cG9ydHMuRGlzcG9zYWJsZSA9IERpc3Bvc2FibGUgPSB7fSkpO1xuIiwgIlwidXNlIHN0cmljdFwiO1xuLyogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbiAqIENvcHlyaWdodCAoYykgTWljcm9zb2Z0IENvcnBvcmF0aW9uLiBBbGwgcmlnaHRzIHJlc2VydmVkLlxuICogTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTGljZW5zZS50eHQgaW4gdGhlIHByb2plY3Qgcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSAqL1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiX19lc01vZHVsZVwiLCB7IHZhbHVlOiB0cnVlIH0pO1xubGV0IF9yYWw7XG5mdW5jdGlvbiBSQUwoKSB7XG4gICAgaWYgKF9yYWwgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYE5vIHJ1bnRpbWUgYWJzdHJhY3Rpb24gbGF5ZXIgaW5zdGFsbGVkYCk7XG4gICAgfVxuICAgIHJldHVybiBfcmFsO1xufVxuKGZ1bmN0aW9uIChSQUwpIHtcbiAgICBmdW5jdGlvbiBpbnN0YWxsKHJhbCkge1xuICAgICAgICBpZiAocmFsID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgTm8gcnVudGltZSBhYnN0cmFjdGlvbiBsYXllciBwcm92aWRlZGApO1xuICAgICAgICB9XG4gICAgICAgIF9yYWwgPSByYWw7XG4gICAgfVxuICAgIFJBTC5pbnN0YWxsID0gaW5zdGFsbDtcbn0pKFJBTCB8fCAoUkFMID0ge30pKTtcbmV4cG9ydHMuZGVmYXVsdCA9IFJBTDtcbiIsICJcInVzZSBzdHJpY3RcIjtcbi8qIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiBDb3B5cmlnaHQgKGMpIE1pY3Jvc29mdCBDb3Jwb3JhdGlvbi4gQWxsIHJpZ2h0cyByZXNlcnZlZC5cbiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBNSVQgTGljZW5zZS4gU2VlIExpY2Vuc2UudHh0IGluIHRoZSBwcm9qZWN0IHJvb3QgZm9yIGxpY2Vuc2UgaW5mb3JtYXRpb24uXG4gKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0gKi9cbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIl9fZXNNb2R1bGVcIiwgeyB2YWx1ZTogdHJ1ZSB9KTtcbmV4cG9ydHMuRW1pdHRlciA9IGV4cG9ydHMuRXZlbnQgPSB2b2lkIDA7XG5jb25zdCByYWxfMSA9IHJlcXVpcmUoXCIuL3JhbFwiKTtcbnZhciBFdmVudDtcbihmdW5jdGlvbiAoRXZlbnQpIHtcbiAgICBjb25zdCBfZGlzcG9zYWJsZSA9IHsgZGlzcG9zZSgpIHsgfSB9O1xuICAgIEV2ZW50Lk5vbmUgPSBmdW5jdGlvbiAoKSB7IHJldHVybiBfZGlzcG9zYWJsZTsgfTtcbn0pKEV2ZW50IHx8IChleHBvcnRzLkV2ZW50ID0gRXZlbnQgPSB7fSkpO1xuY2xhc3MgQ2FsbGJhY2tMaXN0IHtcbiAgICBhZGQoY2FsbGJhY2ssIGNvbnRleHQgPSBudWxsLCBidWNrZXQpIHtcbiAgICAgICAgaWYgKCF0aGlzLl9jYWxsYmFja3MpIHtcbiAgICAgICAgICAgIHRoaXMuX2NhbGxiYWNrcyA9IFtdO1xuICAgICAgICAgICAgdGhpcy5fY29udGV4dHMgPSBbXTtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLl9jYWxsYmFja3MucHVzaChjYWxsYmFjayk7XG4gICAgICAgIHRoaXMuX2NvbnRleHRzLnB1c2goY29udGV4dCk7XG4gICAgICAgIGlmIChBcnJheS5pc0FycmF5KGJ1Y2tldCkpIHtcbiAgICAgICAgICAgIGJ1Y2tldC5wdXNoKHsgZGlzcG9zZTogKCkgPT4gdGhpcy5yZW1vdmUoY2FsbGJhY2ssIGNvbnRleHQpIH0pO1xuICAgICAgICB9XG4gICAgfVxuICAgIHJlbW92ZShjYWxsYmFjaywgY29udGV4dCA9IG51bGwpIHtcbiAgICAgICAgaWYgKCF0aGlzLl9jYWxsYmFja3MpIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBsZXQgZm91bmRDYWxsYmFja1dpdGhEaWZmZXJlbnRDb250ZXh0ID0gZmFsc2U7XG4gICAgICAgIGZvciAobGV0IGkgPSAwLCBsZW4gPSB0aGlzLl9jYWxsYmFja3MubGVuZ3RoOyBpIDwgbGVuOyBpKyspIHtcbiAgICAgICAgICAgIGlmICh0aGlzLl9jYWxsYmFja3NbaV0gPT09IGNhbGxiYWNrKSB7XG4gICAgICAgICAgICAgICAgaWYgKHRoaXMuX2NvbnRleHRzW2ldID09PSBjb250ZXh0KSB7XG4gICAgICAgICAgICAgICAgICAgIC8vIGNhbGxiYWNrICYgY29udGV4dCBtYXRjaCA9PiByZW1vdmUgaXRcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5fY2FsbGJhY2tzLnNwbGljZShpLCAxKTtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5fY29udGV4dHMuc3BsaWNlKGksIDEpO1xuICAgICAgICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICBmb3VuZENhbGxiYWNrV2l0aERpZmZlcmVudENvbnRleHQgPSB0cnVlO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICBpZiAoZm91bmRDYWxsYmFja1dpdGhEaWZmZXJlbnRDb250ZXh0KSB7XG4gICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoJ1doZW4gYWRkaW5nIGEgbGlzdGVuZXIgd2l0aCBhIGNvbnRleHQsIHlvdSBzaG91bGQgcmVtb3ZlIGl0IHdpdGggdGhlIHNhbWUgY29udGV4dCcpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGludm9rZSguLi5hcmdzKSB7XG4gICAgICAgIGlmICghdGhpcy5fY2FsbGJhY2tzKSB7XG4gICAgICAgICAgICByZXR1cm4gW107XG4gICAgICAgIH1cbiAgICAgICAgY29uc3QgcmV0ID0gW10sIGNhbGxiYWNrcyA9IHRoaXMuX2NhbGxiYWNrcy5zbGljZSgwKSwgY29udGV4dHMgPSB0aGlzLl9jb250ZXh0cy5zbGljZSgwKTtcbiAgICAgICAgZm9yIChsZXQgaSA9IDAsIGxlbiA9IGNhbGxiYWNrcy5sZW5ndGg7IGkgPCBsZW47IGkrKykge1xuICAgICAgICAgICAgdHJ5IHtcbiAgICAgICAgICAgICAgICByZXQucHVzaChjYWxsYmFja3NbaV0uYXBwbHkoY29udGV4dHNbaV0sIGFyZ3MpKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNhdGNoIChlKSB7XG4gICAgICAgICAgICAgICAgLy8gZXNsaW50LWRpc2FibGUtbmV4dC1saW5lIG5vLWNvbnNvbGVcbiAgICAgICAgICAgICAgICAoMCwgcmFsXzEuZGVmYXVsdCkoKS5jb25zb2xlLmVycm9yKGUpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXQ7XG4gICAgfVxuICAgIGlzRW1wdHkoKSB7XG4gICAgICAgIHJldHVybiAhdGhpcy5fY2FsbGJhY2tzIHx8IHRoaXMuX2NhbGxiYWNrcy5sZW5ndGggPT09IDA7XG4gICAgfVxuICAgIGRpc3Bvc2UoKSB7XG4gICAgICAgIHRoaXMuX2NhbGxiYWNrcyA9IHVuZGVmaW5lZDtcbiAgICAgICAgdGhpcy5fY29udGV4dHMgPSB1bmRlZmluZWQ7XG4gICAgfVxufVxuY2xhc3MgRW1pdHRlciB7XG4gICAgY29uc3RydWN0b3IoX29wdGlvbnMpIHtcbiAgICAgICAgdGhpcy5fb3B0aW9ucyA9IF9vcHRpb25zO1xuICAgIH1cbiAgICAvKipcbiAgICAgKiBGb3IgdGhlIHB1YmxpYyB0byBhbGxvdyB0byBzdWJzY3JpYmVcbiAgICAgKiB0byBldmVudHMgZnJvbSB0aGlzIEVtaXR0ZXJcbiAgICAgKi9cbiAgICBnZXQgZXZlbnQoKSB7XG4gICAgICAgIGlmICghdGhpcy5fZXZlbnQpIHtcbiAgICAgICAgICAgIHRoaXMuX2V2ZW50ID0gKGxpc3RlbmVyLCB0aGlzQXJncywgZGlzcG9zYWJsZXMpID0+IHtcbiAgICAgICAgICAgICAgICBpZiAoIXRoaXMuX2NhbGxiYWNrcykge1xuICAgICAgICAgICAgICAgICAgICB0aGlzLl9jYWxsYmFja3MgPSBuZXcgQ2FsbGJhY2tMaXN0KCk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGlmICh0aGlzLl9vcHRpb25zICYmIHRoaXMuX29wdGlvbnMub25GaXJzdExpc3RlbmVyQWRkICYmIHRoaXMuX2NhbGxiYWNrcy5pc0VtcHR5KCkpIHtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5fb3B0aW9ucy5vbkZpcnN0TGlzdGVuZXJBZGQodGhpcyk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIHRoaXMuX2NhbGxiYWNrcy5hZGQobGlzdGVuZXIsIHRoaXNBcmdzKTtcbiAgICAgICAgICAgICAgICBjb25zdCByZXN1bHQgPSB7XG4gICAgICAgICAgICAgICAgICAgIGRpc3Bvc2U6ICgpID0+IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICghdGhpcy5fY2FsbGJhY2tzKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8gZGlzcG9zYWJsZSBpcyBkaXNwb3NlZCBhZnRlciBlbWl0dGVyIGlzIGRpc3Bvc2VkLlxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIHRoaXMuX2NhbGxiYWNrcy5yZW1vdmUobGlzdGVuZXIsIHRoaXNBcmdzKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJlc3VsdC5kaXNwb3NlID0gRW1pdHRlci5fbm9vcDtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICh0aGlzLl9vcHRpb25zICYmIHRoaXMuX29wdGlvbnMub25MYXN0TGlzdGVuZXJSZW1vdmUgJiYgdGhpcy5fY2FsbGJhY2tzLmlzRW1wdHkoKSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHRoaXMuX29wdGlvbnMub25MYXN0TGlzdGVuZXJSZW1vdmUodGhpcyk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICAgIGlmIChBcnJheS5pc0FycmF5KGRpc3Bvc2FibGVzKSkge1xuICAgICAgICAgICAgICAgICAgICBkaXNwb3NhYmxlcy5wdXNoKHJlc3VsdCk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgICAgICAgICB9O1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiB0aGlzLl9ldmVudDtcbiAgICB9XG4gICAgLyoqXG4gICAgICogVG8gYmUga2VwdCBwcml2YXRlIHRvIGZpcmUgYW4gZXZlbnQgdG9cbiAgICAgKiBzdWJzY3JpYmVyc1xuICAgICAqL1xuICAgIGZpcmUoZXZlbnQpIHtcbiAgICAgICAgaWYgKHRoaXMuX2NhbGxiYWNrcykge1xuICAgICAgICAgICAgdGhpcy5fY2FsbGJhY2tzLmludm9rZS5jYWxsKHRoaXMuX2NhbGxiYWNrcywgZXZlbnQpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGRpc3Bvc2UoKSB7XG4gICAgICAgIGlmICh0aGlzLl9jYWxsYmFja3MpIHtcbiAgICAgICAgICAgIHRoaXMuX2NhbGxiYWNrcy5kaXNwb3NlKCk7XG4gICAgICAgICAgICB0aGlzLl9jYWxsYmFja3MgPSB1bmRlZmluZWQ7XG4gICAgICAgIH1cbiAgICB9XG59XG5leHBvcnRzLkVtaXR0ZXIgPSBFbWl0dGVyO1xuRW1pdHRlci5fbm9vcCA9IGZ1bmN0aW9uICgpIHsgfTtcbiIsICJcInVzZSBzdHJpY3RcIjtcbi8qLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiAgQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiAgTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTGljZW5zZS50eHQgaW4gdGhlIHByb2plY3Qgcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0qL1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiX19lc01vZHVsZVwiLCB7IHZhbHVlOiB0cnVlIH0pO1xuZXhwb3J0cy5DYW5jZWxsYXRpb25Ub2tlblNvdXJjZSA9IGV4cG9ydHMuQ2FuY2VsbGF0aW9uVG9rZW4gPSB2b2lkIDA7XG5jb25zdCByYWxfMSA9IHJlcXVpcmUoXCIuL3JhbFwiKTtcbmNvbnN0IElzID0gcmVxdWlyZShcIi4vaXNcIik7XG5jb25zdCBldmVudHNfMSA9IHJlcXVpcmUoXCIuL2V2ZW50c1wiKTtcbnZhciBDYW5jZWxsYXRpb25Ub2tlbjtcbihmdW5jdGlvbiAoQ2FuY2VsbGF0aW9uVG9rZW4pIHtcbiAgICBDYW5jZWxsYXRpb25Ub2tlbi5Ob25lID0gT2JqZWN0LmZyZWV6ZSh7XG4gICAgICAgIGlzQ2FuY2VsbGF0aW9uUmVxdWVzdGVkOiBmYWxzZSxcbiAgICAgICAgb25DYW5jZWxsYXRpb25SZXF1ZXN0ZWQ6IGV2ZW50c18xLkV2ZW50Lk5vbmVcbiAgICB9KTtcbiAgICBDYW5jZWxsYXRpb25Ub2tlbi5DYW5jZWxsZWQgPSBPYmplY3QuZnJlZXplKHtcbiAgICAgICAgaXNDYW5jZWxsYXRpb25SZXF1ZXN0ZWQ6IHRydWUsXG4gICAgICAgIG9uQ2FuY2VsbGF0aW9uUmVxdWVzdGVkOiBldmVudHNfMS5FdmVudC5Ob25lXG4gICAgfSk7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBjYW5kaWRhdGUgJiYgKGNhbmRpZGF0ZSA9PT0gQ2FuY2VsbGF0aW9uVG9rZW4uTm9uZVxuICAgICAgICAgICAgfHwgY2FuZGlkYXRlID09PSBDYW5jZWxsYXRpb25Ub2tlbi5DYW5jZWxsZWRcbiAgICAgICAgICAgIHx8IChJcy5ib29sZWFuKGNhbmRpZGF0ZS5pc0NhbmNlbGxhdGlvblJlcXVlc3RlZCkgJiYgISFjYW5kaWRhdGUub25DYW5jZWxsYXRpb25SZXF1ZXN0ZWQpKTtcbiAgICB9XG4gICAgQ2FuY2VsbGF0aW9uVG9rZW4uaXMgPSBpcztcbn0pKENhbmNlbGxhdGlvblRva2VuIHx8IChleHBvcnRzLkNhbmNlbGxhdGlvblRva2VuID0gQ2FuY2VsbGF0aW9uVG9rZW4gPSB7fSkpO1xuY29uc3Qgc2hvcnRjdXRFdmVudCA9IE9iamVjdC5mcmVlemUoZnVuY3Rpb24gKGNhbGxiYWNrLCBjb250ZXh0KSB7XG4gICAgY29uc3QgaGFuZGxlID0gKDAsIHJhbF8xLmRlZmF1bHQpKCkudGltZXIuc2V0VGltZW91dChjYWxsYmFjay5iaW5kKGNvbnRleHQpLCAwKTtcbiAgICByZXR1cm4geyBkaXNwb3NlKCkgeyBoYW5kbGUuZGlzcG9zZSgpOyB9IH07XG59KTtcbmNsYXNzIE11dGFibGVUb2tlbiB7XG4gICAgY29uc3RydWN0b3IoKSB7XG4gICAgICAgIHRoaXMuX2lzQ2FuY2VsbGVkID0gZmFsc2U7XG4gICAgfVxuICAgIGNhbmNlbCgpIHtcbiAgICAgICAgaWYgKCF0aGlzLl9pc0NhbmNlbGxlZCkge1xuICAgICAgICAgICAgdGhpcy5faXNDYW5jZWxsZWQgPSB0cnVlO1xuICAgICAgICAgICAgaWYgKHRoaXMuX2VtaXR0ZXIpIHtcbiAgICAgICAgICAgICAgICB0aGlzLl9lbWl0dGVyLmZpcmUodW5kZWZpbmVkKTtcbiAgICAgICAgICAgICAgICB0aGlzLmRpc3Bvc2UoKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgIH1cbiAgICBnZXQgaXNDYW5jZWxsYXRpb25SZXF1ZXN0ZWQoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLl9pc0NhbmNlbGxlZDtcbiAgICB9XG4gICAgZ2V0IG9uQ2FuY2VsbGF0aW9uUmVxdWVzdGVkKCkge1xuICAgICAgICBpZiAodGhpcy5faXNDYW5jZWxsZWQpIHtcbiAgICAgICAgICAgIHJldHVybiBzaG9ydGN1dEV2ZW50O1xuICAgICAgICB9XG4gICAgICAgIGlmICghdGhpcy5fZW1pdHRlcikge1xuICAgICAgICAgICAgdGhpcy5fZW1pdHRlciA9IG5ldyBldmVudHNfMS5FbWl0dGVyKCk7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHRoaXMuX2VtaXR0ZXIuZXZlbnQ7XG4gICAgfVxuICAgIGRpc3Bvc2UoKSB7XG4gICAgICAgIGlmICh0aGlzLl9lbWl0dGVyKSB7XG4gICAgICAgICAgICB0aGlzLl9lbWl0dGVyLmRpc3Bvc2UoKTtcbiAgICAgICAgICAgIHRoaXMuX2VtaXR0ZXIgPSB1bmRlZmluZWQ7XG4gICAgICAgIH1cbiAgICB9XG59XG5jbGFzcyBDYW5jZWxsYXRpb25Ub2tlblNvdXJjZSB7XG4gICAgZ2V0IHRva2VuKCkge1xuICAgICAgICBpZiAoIXRoaXMuX3Rva2VuKSB7XG4gICAgICAgICAgICAvLyBiZSBsYXp5IGFuZCBjcmVhdGUgdGhlIHRva2VuIG9ubHkgd2hlblxuICAgICAgICAgICAgLy8gYWN0dWFsbHkgbmVlZGVkXG4gICAgICAgICAgICB0aGlzLl90b2tlbiA9IG5ldyBNdXRhYmxlVG9rZW4oKTtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gdGhpcy5fdG9rZW47XG4gICAgfVxuICAgIGNhbmNlbCgpIHtcbiAgICAgICAgaWYgKCF0aGlzLl90b2tlbikge1xuICAgICAgICAgICAgLy8gc2F2ZSBhbiBvYmplY3QgYnkgcmV0dXJuaW5nIHRoZSBkZWZhdWx0XG4gICAgICAgICAgICAvLyBjYW5jZWxsZWQgdG9rZW4gd2hlbiBjYW5jZWxsYXRpb24gaGFwcGVuc1xuICAgICAgICAgICAgLy8gYmVmb3JlIHNvbWVvbmUgYXNrcyBmb3IgdGhlIHRva2VuXG4gICAgICAgICAgICB0aGlzLl90b2tlbiA9IENhbmNlbGxhdGlvblRva2VuLkNhbmNlbGxlZDtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHRoaXMuX3Rva2VuLmNhbmNlbCgpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGRpc3Bvc2UoKSB7XG4gICAgICAgIGlmICghdGhpcy5fdG9rZW4pIHtcbiAgICAgICAgICAgIC8vIGVuc3VyZSB0byBpbml0aWFsaXplIHdpdGggYW4gZW1wdHkgdG9rZW4gaWYgd2UgaGFkIG5vbmVcbiAgICAgICAgICAgIHRoaXMuX3Rva2VuID0gQ2FuY2VsbGF0aW9uVG9rZW4uTm9uZTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIGlmICh0aGlzLl90b2tlbiBpbnN0YW5jZW9mIE11dGFibGVUb2tlbikge1xuICAgICAgICAgICAgLy8gYWN0dWFsbHkgZGlzcG9zZVxuICAgICAgICAgICAgdGhpcy5fdG9rZW4uZGlzcG9zZSgpO1xuICAgICAgICB9XG4gICAgfVxufVxuZXhwb3J0cy5DYW5jZWxsYXRpb25Ub2tlblNvdXJjZSA9IENhbmNlbGxhdGlvblRva2VuU291cmNlO1xuIiwgIlwidXNlIHN0cmljdFwiO1xuLyogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbiAqIENvcHlyaWdodCAoYykgTWljcm9zb2Z0IENvcnBvcmF0aW9uLiBBbGwgcmlnaHRzIHJlc2VydmVkLlxuICogTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTGljZW5zZS50eHQgaW4gdGhlIHByb2plY3Qgcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSAqL1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiX19lc01vZHVsZVwiLCB7IHZhbHVlOiB0cnVlIH0pO1xuZXhwb3J0cy5TaGFyZWRBcnJheVJlY2VpdmVyU3RyYXRlZ3kgPSBleHBvcnRzLlNoYXJlZEFycmF5U2VuZGVyU3RyYXRlZ3kgPSB2b2lkIDA7XG5jb25zdCBjYW5jZWxsYXRpb25fMSA9IHJlcXVpcmUoXCIuL2NhbmNlbGxhdGlvblwiKTtcbnZhciBDYW5jZWxsYXRpb25TdGF0ZTtcbihmdW5jdGlvbiAoQ2FuY2VsbGF0aW9uU3RhdGUpIHtcbiAgICBDYW5jZWxsYXRpb25TdGF0ZS5Db250aW51ZSA9IDA7XG4gICAgQ2FuY2VsbGF0aW9uU3RhdGUuQ2FuY2VsbGVkID0gMTtcbn0pKENhbmNlbGxhdGlvblN0YXRlIHx8IChDYW5jZWxsYXRpb25TdGF0ZSA9IHt9KSk7XG5jbGFzcyBTaGFyZWRBcnJheVNlbmRlclN0cmF0ZWd5IHtcbiAgICBjb25zdHJ1Y3RvcigpIHtcbiAgICAgICAgdGhpcy5idWZmZXJzID0gbmV3IE1hcCgpO1xuICAgIH1cbiAgICBlbmFibGVDYW5jZWxsYXRpb24ocmVxdWVzdCkge1xuICAgICAgICBpZiAocmVxdWVzdC5pZCA9PT0gbnVsbCkge1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGNvbnN0IGJ1ZmZlciA9IG5ldyBTaGFyZWRBcnJheUJ1ZmZlcig0KTtcbiAgICAgICAgY29uc3QgZGF0YSA9IG5ldyBJbnQzMkFycmF5KGJ1ZmZlciwgMCwgMSk7XG4gICAgICAgIGRhdGFbMF0gPSBDYW5jZWxsYXRpb25TdGF0ZS5Db250aW51ZTtcbiAgICAgICAgdGhpcy5idWZmZXJzLnNldChyZXF1ZXN0LmlkLCBidWZmZXIpO1xuICAgICAgICByZXF1ZXN0LiRjYW5jZWxsYXRpb25EYXRhID0gYnVmZmVyO1xuICAgIH1cbiAgICBhc3luYyBzZW5kQ2FuY2VsbGF0aW9uKF9jb25uLCBpZCkge1xuICAgICAgICBjb25zdCBidWZmZXIgPSB0aGlzLmJ1ZmZlcnMuZ2V0KGlkKTtcbiAgICAgICAgaWYgKGJ1ZmZlciA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgICAgY29uc3QgZGF0YSA9IG5ldyBJbnQzMkFycmF5KGJ1ZmZlciwgMCwgMSk7XG4gICAgICAgIEF0b21pY3Muc3RvcmUoZGF0YSwgMCwgQ2FuY2VsbGF0aW9uU3RhdGUuQ2FuY2VsbGVkKTtcbiAgICB9XG4gICAgY2xlYW51cChpZCkge1xuICAgICAgICB0aGlzLmJ1ZmZlcnMuZGVsZXRlKGlkKTtcbiAgICB9XG4gICAgZGlzcG9zZSgpIHtcbiAgICAgICAgdGhpcy5idWZmZXJzLmNsZWFyKCk7XG4gICAgfVxufVxuZXhwb3J0cy5TaGFyZWRBcnJheVNlbmRlclN0cmF0ZWd5ID0gU2hhcmVkQXJyYXlTZW5kZXJTdHJhdGVneTtcbmNsYXNzIFNoYXJlZEFycmF5QnVmZmVyQ2FuY2VsbGF0aW9uVG9rZW4ge1xuICAgIGNvbnN0cnVjdG9yKGJ1ZmZlcikge1xuICAgICAgICB0aGlzLmRhdGEgPSBuZXcgSW50MzJBcnJheShidWZmZXIsIDAsIDEpO1xuICAgIH1cbiAgICBnZXQgaXNDYW5jZWxsYXRpb25SZXF1ZXN0ZWQoKSB7XG4gICAgICAgIHJldHVybiBBdG9taWNzLmxvYWQodGhpcy5kYXRhLCAwKSA9PT0gQ2FuY2VsbGF0aW9uU3RhdGUuQ2FuY2VsbGVkO1xuICAgIH1cbiAgICBnZXQgb25DYW5jZWxsYXRpb25SZXF1ZXN0ZWQoKSB7XG4gICAgICAgIHRocm93IG5ldyBFcnJvcihgQ2FuY2VsbGF0aW9uIG92ZXIgU2hhcmVkQXJyYXlCdWZmZXIgZG9lc24ndCBzdXBwb3J0IGNhbmNlbGxhdGlvbiBldmVudHNgKTtcbiAgICB9XG59XG5jbGFzcyBTaGFyZWRBcnJheUJ1ZmZlckNhbmNlbGxhdGlvblRva2VuU291cmNlIHtcbiAgICBjb25zdHJ1Y3RvcihidWZmZXIpIHtcbiAgICAgICAgdGhpcy50b2tlbiA9IG5ldyBTaGFyZWRBcnJheUJ1ZmZlckNhbmNlbGxhdGlvblRva2VuKGJ1ZmZlcik7XG4gICAgfVxuICAgIGNhbmNlbCgpIHtcbiAgICB9XG4gICAgZGlzcG9zZSgpIHtcbiAgICB9XG59XG5jbGFzcyBTaGFyZWRBcnJheVJlY2VpdmVyU3RyYXRlZ3kge1xuICAgIGNvbnN0cnVjdG9yKCkge1xuICAgICAgICB0aGlzLmtpbmQgPSAncmVxdWVzdCc7XG4gICAgfVxuICAgIGNyZWF0ZUNhbmNlbGxhdGlvblRva2VuU291cmNlKHJlcXVlc3QpIHtcbiAgICAgICAgY29uc3QgYnVmZmVyID0gcmVxdWVzdC4kY2FuY2VsbGF0aW9uRGF0YTtcbiAgICAgICAgaWYgKGJ1ZmZlciA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXR1cm4gbmV3IGNhbmNlbGxhdGlvbl8xLkNhbmNlbGxhdGlvblRva2VuU291cmNlKCk7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIG5ldyBTaGFyZWRBcnJheUJ1ZmZlckNhbmNlbGxhdGlvblRva2VuU291cmNlKGJ1ZmZlcik7XG4gICAgfVxufVxuZXhwb3J0cy5TaGFyZWRBcnJheVJlY2VpdmVyU3RyYXRlZ3kgPSBTaGFyZWRBcnJheVJlY2VpdmVyU3RyYXRlZ3k7XG4iLCAiXCJ1c2Ugc3RyaWN0XCI7XG4vKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJfX2VzTW9kdWxlXCIsIHsgdmFsdWU6IHRydWUgfSk7XG5leHBvcnRzLlNlbWFwaG9yZSA9IHZvaWQgMDtcbmNvbnN0IHJhbF8xID0gcmVxdWlyZShcIi4vcmFsXCIpO1xuY2xhc3MgU2VtYXBob3JlIHtcbiAgICBjb25zdHJ1Y3RvcihjYXBhY2l0eSA9IDEpIHtcbiAgICAgICAgaWYgKGNhcGFjaXR5IDw9IDApIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcignQ2FwYWNpdHkgbXVzdCBiZSBncmVhdGVyIHRoYW4gMCcpO1xuICAgICAgICB9XG4gICAgICAgIHRoaXMuX2NhcGFjaXR5ID0gY2FwYWNpdHk7XG4gICAgICAgIHRoaXMuX2FjdGl2ZSA9IDA7XG4gICAgICAgIHRoaXMuX3dhaXRpbmcgPSBbXTtcbiAgICB9XG4gICAgbG9jayh0aHVuaykge1xuICAgICAgICByZXR1cm4gbmV3IFByb21pc2UoKHJlc29sdmUsIHJlamVjdCkgPT4ge1xuICAgICAgICAgICAgdGhpcy5fd2FpdGluZy5wdXNoKHsgdGh1bmssIHJlc29sdmUsIHJlamVjdCB9KTtcbiAgICAgICAgICAgIHRoaXMucnVuTmV4dCgpO1xuICAgICAgICB9KTtcbiAgICB9XG4gICAgZ2V0IGFjdGl2ZSgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX2FjdGl2ZTtcbiAgICB9XG4gICAgcnVuTmV4dCgpIHtcbiAgICAgICAgaWYgKHRoaXMuX3dhaXRpbmcubGVuZ3RoID09PSAwIHx8IHRoaXMuX2FjdGl2ZSA9PT0gdGhpcy5fY2FwYWNpdHkpIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICAoMCwgcmFsXzEuZGVmYXVsdCkoKS50aW1lci5zZXRJbW1lZGlhdGUoKCkgPT4gdGhpcy5kb1J1bk5leHQoKSk7XG4gICAgfVxuICAgIGRvUnVuTmV4dCgpIHtcbiAgICAgICAgaWYgKHRoaXMuX3dhaXRpbmcubGVuZ3RoID09PSAwIHx8IHRoaXMuX2FjdGl2ZSA9PT0gdGhpcy5fY2FwYWNpdHkpIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBjb25zdCBuZXh0ID0gdGhpcy5fd2FpdGluZy5zaGlmdCgpO1xuICAgICAgICB0aGlzLl9hY3RpdmUrKztcbiAgICAgICAgaWYgKHRoaXMuX2FjdGl2ZSA+IHRoaXMuX2NhcGFjaXR5KSB7XG4gICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYFRvIG1hbnkgdGh1bmtzIGFjdGl2ZWApO1xuICAgICAgICB9XG4gICAgICAgIHRyeSB7XG4gICAgICAgICAgICBjb25zdCByZXN1bHQgPSBuZXh0LnRodW5rKCk7XG4gICAgICAgICAgICBpZiAocmVzdWx0IGluc3RhbmNlb2YgUHJvbWlzZSkge1xuICAgICAgICAgICAgICAgIHJlc3VsdC50aGVuKCh2YWx1ZSkgPT4ge1xuICAgICAgICAgICAgICAgICAgICB0aGlzLl9hY3RpdmUtLTtcbiAgICAgICAgICAgICAgICAgICAgbmV4dC5yZXNvbHZlKHZhbHVlKTtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5ydW5OZXh0KCk7XG4gICAgICAgICAgICAgICAgfSwgKGVycikgPT4ge1xuICAgICAgICAgICAgICAgICAgICB0aGlzLl9hY3RpdmUtLTtcbiAgICAgICAgICAgICAgICAgICAgbmV4dC5yZWplY3QoZXJyKTtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5ydW5OZXh0KCk7XG4gICAgICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICB0aGlzLl9hY3RpdmUtLTtcbiAgICAgICAgICAgICAgICBuZXh0LnJlc29sdmUocmVzdWx0KTtcbiAgICAgICAgICAgICAgICB0aGlzLnJ1bk5leHQoKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICBjYXRjaCAoZXJyKSB7XG4gICAgICAgICAgICB0aGlzLl9hY3RpdmUtLTtcbiAgICAgICAgICAgIG5leHQucmVqZWN0KGVycik7XG4gICAgICAgICAgICB0aGlzLnJ1bk5leHQoKTtcbiAgICAgICAgfVxuICAgIH1cbn1cbmV4cG9ydHMuU2VtYXBob3JlID0gU2VtYXBob3JlO1xuIiwgIlwidXNlIHN0cmljdFwiO1xuLyogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbiAqIENvcHlyaWdodCAoYykgTWljcm9zb2Z0IENvcnBvcmF0aW9uLiBBbGwgcmlnaHRzIHJlc2VydmVkLlxuICogTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTGljZW5zZS50eHQgaW4gdGhlIHByb2plY3Qgcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSAqL1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiX19lc01vZHVsZVwiLCB7IHZhbHVlOiB0cnVlIH0pO1xuZXhwb3J0cy5SZWFkYWJsZVN0cmVhbU1lc3NhZ2VSZWFkZXIgPSBleHBvcnRzLkFic3RyYWN0TWVzc2FnZVJlYWRlciA9IGV4cG9ydHMuTWVzc2FnZVJlYWRlciA9IHZvaWQgMDtcbmNvbnN0IHJhbF8xID0gcmVxdWlyZShcIi4vcmFsXCIpO1xuY29uc3QgSXMgPSByZXF1aXJlKFwiLi9pc1wiKTtcbmNvbnN0IGV2ZW50c18xID0gcmVxdWlyZShcIi4vZXZlbnRzXCIpO1xuY29uc3Qgc2VtYXBob3JlXzEgPSByZXF1aXJlKFwiLi9zZW1hcGhvcmVcIik7XG52YXIgTWVzc2FnZVJlYWRlcjtcbihmdW5jdGlvbiAoTWVzc2FnZVJlYWRlcikge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiBJcy5mdW5jKGNhbmRpZGF0ZS5saXN0ZW4pICYmIElzLmZ1bmMoY2FuZGlkYXRlLmRpc3Bvc2UpICYmXG4gICAgICAgICAgICBJcy5mdW5jKGNhbmRpZGF0ZS5vbkVycm9yKSAmJiBJcy5mdW5jKGNhbmRpZGF0ZS5vbkNsb3NlKSAmJiBJcy5mdW5jKGNhbmRpZGF0ZS5vblBhcnRpYWxNZXNzYWdlKTtcbiAgICB9XG4gICAgTWVzc2FnZVJlYWRlci5pcyA9IGlzO1xufSkoTWVzc2FnZVJlYWRlciB8fCAoZXhwb3J0cy5NZXNzYWdlUmVhZGVyID0gTWVzc2FnZVJlYWRlciA9IHt9KSk7XG5jbGFzcyBBYnN0cmFjdE1lc3NhZ2VSZWFkZXIge1xuICAgIGNvbnN0cnVjdG9yKCkge1xuICAgICAgICB0aGlzLmVycm9yRW1pdHRlciA9IG5ldyBldmVudHNfMS5FbWl0dGVyKCk7XG4gICAgICAgIHRoaXMuY2xvc2VFbWl0dGVyID0gbmV3IGV2ZW50c18xLkVtaXR0ZXIoKTtcbiAgICAgICAgdGhpcy5wYXJ0aWFsTWVzc2FnZUVtaXR0ZXIgPSBuZXcgZXZlbnRzXzEuRW1pdHRlcigpO1xuICAgIH1cbiAgICBkaXNwb3NlKCkge1xuICAgICAgICB0aGlzLmVycm9yRW1pdHRlci5kaXNwb3NlKCk7XG4gICAgICAgIHRoaXMuY2xvc2VFbWl0dGVyLmRpc3Bvc2UoKTtcbiAgICB9XG4gICAgZ2V0IG9uRXJyb3IoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLmVycm9yRW1pdHRlci5ldmVudDtcbiAgICB9XG4gICAgZmlyZUVycm9yKGVycm9yKSB7XG4gICAgICAgIHRoaXMuZXJyb3JFbWl0dGVyLmZpcmUodGhpcy5hc0Vycm9yKGVycm9yKSk7XG4gICAgfVxuICAgIGdldCBvbkNsb3NlKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5jbG9zZUVtaXR0ZXIuZXZlbnQ7XG4gICAgfVxuICAgIGZpcmVDbG9zZSgpIHtcbiAgICAgICAgdGhpcy5jbG9zZUVtaXR0ZXIuZmlyZSh1bmRlZmluZWQpO1xuICAgIH1cbiAgICBnZXQgb25QYXJ0aWFsTWVzc2FnZSgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMucGFydGlhbE1lc3NhZ2VFbWl0dGVyLmV2ZW50O1xuICAgIH1cbiAgICBmaXJlUGFydGlhbE1lc3NhZ2UoaW5mbykge1xuICAgICAgICB0aGlzLnBhcnRpYWxNZXNzYWdlRW1pdHRlci5maXJlKGluZm8pO1xuICAgIH1cbiAgICBhc0Vycm9yKGVycm9yKSB7XG4gICAgICAgIGlmIChlcnJvciBpbnN0YW5jZW9mIEVycm9yKSB7XG4gICAgICAgICAgICByZXR1cm4gZXJyb3I7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICByZXR1cm4gbmV3IEVycm9yKGBSZWFkZXIgcmVjZWl2ZWQgZXJyb3IuIFJlYXNvbjogJHtJcy5zdHJpbmcoZXJyb3IubWVzc2FnZSkgPyBlcnJvci5tZXNzYWdlIDogJ3Vua25vd24nfWApO1xuICAgICAgICB9XG4gICAgfVxufVxuZXhwb3J0cy5BYnN0cmFjdE1lc3NhZ2VSZWFkZXIgPSBBYnN0cmFjdE1lc3NhZ2VSZWFkZXI7XG52YXIgUmVzb2x2ZWRNZXNzYWdlUmVhZGVyT3B0aW9ucztcbihmdW5jdGlvbiAoUmVzb2x2ZWRNZXNzYWdlUmVhZGVyT3B0aW9ucykge1xuICAgIGZ1bmN0aW9uIGZyb21PcHRpb25zKG9wdGlvbnMpIHtcbiAgICAgICAgbGV0IGNoYXJzZXQ7XG4gICAgICAgIGxldCByZXN1bHQ7XG4gICAgICAgIGxldCBjb250ZW50RGVjb2RlcjtcbiAgICAgICAgY29uc3QgY29udGVudERlY29kZXJzID0gbmV3IE1hcCgpO1xuICAgICAgICBsZXQgY29udGVudFR5cGVEZWNvZGVyO1xuICAgICAgICBjb25zdCBjb250ZW50VHlwZURlY29kZXJzID0gbmV3IE1hcCgpO1xuICAgICAgICBpZiAob3B0aW9ucyA9PT0gdW5kZWZpbmVkIHx8IHR5cGVvZiBvcHRpb25zID09PSAnc3RyaW5nJykge1xuICAgICAgICAgICAgY2hhcnNldCA9IG9wdGlvbnMgPz8gJ3V0Zi04JztcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIGNoYXJzZXQgPSBvcHRpb25zLmNoYXJzZXQgPz8gJ3V0Zi04JztcbiAgICAgICAgICAgIGlmIChvcHRpb25zLmNvbnRlbnREZWNvZGVyICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICBjb250ZW50RGVjb2RlciA9IG9wdGlvbnMuY29udGVudERlY29kZXI7XG4gICAgICAgICAgICAgICAgY29udGVudERlY29kZXJzLnNldChjb250ZW50RGVjb2Rlci5uYW1lLCBjb250ZW50RGVjb2Rlcik7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAob3B0aW9ucy5jb250ZW50RGVjb2RlcnMgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgIGZvciAoY29uc3QgZGVjb2RlciBvZiBvcHRpb25zLmNvbnRlbnREZWNvZGVycykge1xuICAgICAgICAgICAgICAgICAgICBjb250ZW50RGVjb2RlcnMuc2V0KGRlY29kZXIubmFtZSwgZGVjb2Rlcik7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaWYgKG9wdGlvbnMuY29udGVudFR5cGVEZWNvZGVyICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICBjb250ZW50VHlwZURlY29kZXIgPSBvcHRpb25zLmNvbnRlbnRUeXBlRGVjb2RlcjtcbiAgICAgICAgICAgICAgICBjb250ZW50VHlwZURlY29kZXJzLnNldChjb250ZW50VHlwZURlY29kZXIubmFtZSwgY29udGVudFR5cGVEZWNvZGVyKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmIChvcHRpb25zLmNvbnRlbnRUeXBlRGVjb2RlcnMgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgIGZvciAoY29uc3QgZGVjb2RlciBvZiBvcHRpb25zLmNvbnRlbnRUeXBlRGVjb2RlcnMpIHtcbiAgICAgICAgICAgICAgICAgICAgY29udGVudFR5cGVEZWNvZGVycy5zZXQoZGVjb2Rlci5uYW1lLCBkZWNvZGVyKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgaWYgKGNvbnRlbnRUeXBlRGVjb2RlciA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICBjb250ZW50VHlwZURlY29kZXIgPSAoMCwgcmFsXzEuZGVmYXVsdCkoKS5hcHBsaWNhdGlvbkpzb24uZGVjb2RlcjtcbiAgICAgICAgICAgIGNvbnRlbnRUeXBlRGVjb2RlcnMuc2V0KGNvbnRlbnRUeXBlRGVjb2Rlci5uYW1lLCBjb250ZW50VHlwZURlY29kZXIpO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiB7IGNoYXJzZXQsIGNvbnRlbnREZWNvZGVyLCBjb250ZW50RGVjb2RlcnMsIGNvbnRlbnRUeXBlRGVjb2RlciwgY29udGVudFR5cGVEZWNvZGVycyB9O1xuICAgIH1cbiAgICBSZXNvbHZlZE1lc3NhZ2VSZWFkZXJPcHRpb25zLmZyb21PcHRpb25zID0gZnJvbU9wdGlvbnM7XG59KShSZXNvbHZlZE1lc3NhZ2VSZWFkZXJPcHRpb25zIHx8IChSZXNvbHZlZE1lc3NhZ2VSZWFkZXJPcHRpb25zID0ge30pKTtcbmNsYXNzIFJlYWRhYmxlU3RyZWFtTWVzc2FnZVJlYWRlciBleHRlbmRzIEFic3RyYWN0TWVzc2FnZVJlYWRlciB7XG4gICAgY29uc3RydWN0b3IocmVhZGFibGUsIG9wdGlvbnMpIHtcbiAgICAgICAgc3VwZXIoKTtcbiAgICAgICAgdGhpcy5yZWFkYWJsZSA9IHJlYWRhYmxlO1xuICAgICAgICB0aGlzLm9wdGlvbnMgPSBSZXNvbHZlZE1lc3NhZ2VSZWFkZXJPcHRpb25zLmZyb21PcHRpb25zKG9wdGlvbnMpO1xuICAgICAgICB0aGlzLmJ1ZmZlciA9ICgwLCByYWxfMS5kZWZhdWx0KSgpLm1lc3NhZ2VCdWZmZXIuY3JlYXRlKHRoaXMub3B0aW9ucy5jaGFyc2V0KTtcbiAgICAgICAgdGhpcy5fcGFydGlhbE1lc3NhZ2VUaW1lb3V0ID0gMTAwMDA7XG4gICAgICAgIHRoaXMubmV4dE1lc3NhZ2VMZW5ndGggPSAtMTtcbiAgICAgICAgdGhpcy5tZXNzYWdlVG9rZW4gPSAwO1xuICAgICAgICB0aGlzLnJlYWRTZW1hcGhvcmUgPSBuZXcgc2VtYXBob3JlXzEuU2VtYXBob3JlKDEpO1xuICAgIH1cbiAgICBzZXQgcGFydGlhbE1lc3NhZ2VUaW1lb3V0KHRpbWVvdXQpIHtcbiAgICAgICAgdGhpcy5fcGFydGlhbE1lc3NhZ2VUaW1lb3V0ID0gdGltZW91dDtcbiAgICB9XG4gICAgZ2V0IHBhcnRpYWxNZXNzYWdlVGltZW91dCgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX3BhcnRpYWxNZXNzYWdlVGltZW91dDtcbiAgICB9XG4gICAgbGlzdGVuKGNhbGxiYWNrKSB7XG4gICAgICAgIHRoaXMubmV4dE1lc3NhZ2VMZW5ndGggPSAtMTtcbiAgICAgICAgdGhpcy5tZXNzYWdlVG9rZW4gPSAwO1xuICAgICAgICB0aGlzLnBhcnRpYWxNZXNzYWdlVGltZXIgPSB1bmRlZmluZWQ7XG4gICAgICAgIHRoaXMuY2FsbGJhY2sgPSBjYWxsYmFjaztcbiAgICAgICAgY29uc3QgcmVzdWx0ID0gdGhpcy5yZWFkYWJsZS5vbkRhdGEoKGRhdGEpID0+IHtcbiAgICAgICAgICAgIHRoaXMub25EYXRhKGRhdGEpO1xuICAgICAgICB9KTtcbiAgICAgICAgdGhpcy5yZWFkYWJsZS5vbkVycm9yKChlcnJvcikgPT4gdGhpcy5maXJlRXJyb3IoZXJyb3IpKTtcbiAgICAgICAgdGhpcy5yZWFkYWJsZS5vbkNsb3NlKCgpID0+IHRoaXMuZmlyZUNsb3NlKCkpO1xuICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cbiAgICBvbkRhdGEoZGF0YSkge1xuICAgICAgICB0cnkge1xuICAgICAgICAgICAgdGhpcy5idWZmZXIuYXBwZW5kKGRhdGEpO1xuICAgICAgICAgICAgd2hpbGUgKHRydWUpIHtcbiAgICAgICAgICAgICAgICBpZiAodGhpcy5uZXh0TWVzc2FnZUxlbmd0aCA9PT0gLTEpIHtcbiAgICAgICAgICAgICAgICAgICAgY29uc3QgaGVhZGVycyA9IHRoaXMuYnVmZmVyLnRyeVJlYWRIZWFkZXJzKHRydWUpO1xuICAgICAgICAgICAgICAgICAgICBpZiAoIWhlYWRlcnMpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBjb25zdCBjb250ZW50TGVuZ3RoID0gaGVhZGVycy5nZXQoJ2NvbnRlbnQtbGVuZ3RoJyk7XG4gICAgICAgICAgICAgICAgICAgIGlmICghY29udGVudExlbmd0aCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgdGhpcy5maXJlRXJyb3IobmV3IEVycm9yKGBIZWFkZXIgbXVzdCBwcm92aWRlIGEgQ29udGVudC1MZW5ndGggcHJvcGVydHkuXFxuJHtKU09OLnN0cmluZ2lmeShPYmplY3QuZnJvbUVudHJpZXMoaGVhZGVycykpfWApKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBjb25zdCBsZW5ndGggPSBwYXJzZUludChjb250ZW50TGVuZ3RoKTtcbiAgICAgICAgICAgICAgICAgICAgaWYgKGlzTmFOKGxlbmd0aCkpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHRoaXMuZmlyZUVycm9yKG5ldyBFcnJvcihgQ29udGVudC1MZW5ndGggdmFsdWUgbXVzdCBiZSBhIG51bWJlci4gR290ICR7Y29udGVudExlbmd0aH1gKSk7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgdGhpcy5uZXh0TWVzc2FnZUxlbmd0aCA9IGxlbmd0aDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgY29uc3QgYm9keSA9IHRoaXMuYnVmZmVyLnRyeVJlYWRCb2R5KHRoaXMubmV4dE1lc3NhZ2VMZW5ndGgpO1xuICAgICAgICAgICAgICAgIGlmIChib2R5ID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgLyoqIFdlIGhhdmVuJ3QgcmVjZWl2ZWQgdGhlIGZ1bGwgbWVzc2FnZSB5ZXQuICovXG4gICAgICAgICAgICAgICAgICAgIHRoaXMuc2V0UGFydGlhbE1lc3NhZ2VUaW1lcigpO1xuICAgICAgICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIHRoaXMuY2xlYXJQYXJ0aWFsTWVzc2FnZVRpbWVyKCk7XG4gICAgICAgICAgICAgICAgdGhpcy5uZXh0TWVzc2FnZUxlbmd0aCA9IC0xO1xuICAgICAgICAgICAgICAgIC8vIE1ha2Ugc3VyZSB0aGF0IHdlIGNvbnZlcnQgb25lIHJlY2VpdmVkIG1lc3NhZ2UgYWZ0ZXIgdGhlXG4gICAgICAgICAgICAgICAgLy8gb3RoZXIuIE90aGVyd2lzZSBpdCBjb3VsZCBoYXBwZW4gdGhhdCBhIGRlY29kaW5nIG9mIGEgc2Vjb25kXG4gICAgICAgICAgICAgICAgLy8gc21hbGxlciBtZXNzYWdlIGZpbmlzaGVkIGJlZm9yZSB0aGUgZGVjb2Rpbmcgb2YgYSBmaXJzdCBsYXJnZXJcbiAgICAgICAgICAgICAgICAvLyBtZXNzYWdlIGFuZCB0aGVuIHdlIHdvdWxkIGRlbGl2ZXIgdGhlIHNlY29uZCBtZXNzYWdlIGZpcnN0LlxuICAgICAgICAgICAgICAgIHRoaXMucmVhZFNlbWFwaG9yZS5sb2NrKGFzeW5jICgpID0+IHtcbiAgICAgICAgICAgICAgICAgICAgY29uc3QgYnl0ZXMgPSB0aGlzLm9wdGlvbnMuY29udGVudERlY29kZXIgIT09IHVuZGVmaW5lZFxuICAgICAgICAgICAgICAgICAgICAgICAgPyBhd2FpdCB0aGlzLm9wdGlvbnMuY29udGVudERlY29kZXIuZGVjb2RlKGJvZHkpXG4gICAgICAgICAgICAgICAgICAgICAgICA6IGJvZHk7XG4gICAgICAgICAgICAgICAgICAgIGNvbnN0IG1lc3NhZ2UgPSBhd2FpdCB0aGlzLm9wdGlvbnMuY29udGVudFR5cGVEZWNvZGVyLmRlY29kZShieXRlcywgdGhpcy5vcHRpb25zKTtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5jYWxsYmFjayhtZXNzYWdlKTtcbiAgICAgICAgICAgICAgICB9KS5jYXRjaCgoZXJyb3IpID0+IHtcbiAgICAgICAgICAgICAgICAgICAgdGhpcy5maXJlRXJyb3IoZXJyb3IpO1xuICAgICAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIGNhdGNoIChlcnJvcikge1xuICAgICAgICAgICAgdGhpcy5maXJlRXJyb3IoZXJyb3IpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGNsZWFyUGFydGlhbE1lc3NhZ2VUaW1lcigpIHtcbiAgICAgICAgaWYgKHRoaXMucGFydGlhbE1lc3NhZ2VUaW1lcikge1xuICAgICAgICAgICAgdGhpcy5wYXJ0aWFsTWVzc2FnZVRpbWVyLmRpc3Bvc2UoKTtcbiAgICAgICAgICAgIHRoaXMucGFydGlhbE1lc3NhZ2VUaW1lciA9IHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgIH1cbiAgICBzZXRQYXJ0aWFsTWVzc2FnZVRpbWVyKCkge1xuICAgICAgICB0aGlzLmNsZWFyUGFydGlhbE1lc3NhZ2VUaW1lcigpO1xuICAgICAgICBpZiAodGhpcy5fcGFydGlhbE1lc3NhZ2VUaW1lb3V0IDw9IDApIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLnBhcnRpYWxNZXNzYWdlVGltZXIgPSAoMCwgcmFsXzEuZGVmYXVsdCkoKS50aW1lci5zZXRUaW1lb3V0KCh0b2tlbiwgdGltZW91dCkgPT4ge1xuICAgICAgICAgICAgdGhpcy5wYXJ0aWFsTWVzc2FnZVRpbWVyID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgaWYgKHRva2VuID09PSB0aGlzLm1lc3NhZ2VUb2tlbikge1xuICAgICAgICAgICAgICAgIHRoaXMuZmlyZVBhcnRpYWxNZXNzYWdlKHsgbWVzc2FnZVRva2VuOiB0b2tlbiwgd2FpdGluZ1RpbWU6IHRpbWVvdXQgfSk7XG4gICAgICAgICAgICAgICAgdGhpcy5zZXRQYXJ0aWFsTWVzc2FnZVRpbWVyKCk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0sIHRoaXMuX3BhcnRpYWxNZXNzYWdlVGltZW91dCwgdGhpcy5tZXNzYWdlVG9rZW4sIHRoaXMuX3BhcnRpYWxNZXNzYWdlVGltZW91dCk7XG4gICAgfVxufVxuZXhwb3J0cy5SZWFkYWJsZVN0cmVhbU1lc3NhZ2VSZWFkZXIgPSBSZWFkYWJsZVN0cmVhbU1lc3NhZ2VSZWFkZXI7XG4iLCAiXCJ1c2Ugc3RyaWN0XCI7XG4vKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJfX2VzTW9kdWxlXCIsIHsgdmFsdWU6IHRydWUgfSk7XG5leHBvcnRzLldyaXRlYWJsZVN0cmVhbU1lc3NhZ2VXcml0ZXIgPSBleHBvcnRzLkFic3RyYWN0TWVzc2FnZVdyaXRlciA9IGV4cG9ydHMuTWVzc2FnZVdyaXRlciA9IHZvaWQgMDtcbmNvbnN0IHJhbF8xID0gcmVxdWlyZShcIi4vcmFsXCIpO1xuY29uc3QgSXMgPSByZXF1aXJlKFwiLi9pc1wiKTtcbmNvbnN0IHNlbWFwaG9yZV8xID0gcmVxdWlyZShcIi4vc2VtYXBob3JlXCIpO1xuY29uc3QgZXZlbnRzXzEgPSByZXF1aXJlKFwiLi9ldmVudHNcIik7XG5jb25zdCBDb250ZW50TGVuZ3RoID0gJ0NvbnRlbnQtTGVuZ3RoOiAnO1xuY29uc3QgQ1JMRiA9ICdcXHJcXG4nO1xudmFyIE1lc3NhZ2VXcml0ZXI7XG4oZnVuY3Rpb24gKE1lc3NhZ2VXcml0ZXIpIHtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBjYW5kaWRhdGUgJiYgSXMuZnVuYyhjYW5kaWRhdGUuZGlzcG9zZSkgJiYgSXMuZnVuYyhjYW5kaWRhdGUub25DbG9zZSkgJiZcbiAgICAgICAgICAgIElzLmZ1bmMoY2FuZGlkYXRlLm9uRXJyb3IpICYmIElzLmZ1bmMoY2FuZGlkYXRlLndyaXRlKTtcbiAgICB9XG4gICAgTWVzc2FnZVdyaXRlci5pcyA9IGlzO1xufSkoTWVzc2FnZVdyaXRlciB8fCAoZXhwb3J0cy5NZXNzYWdlV3JpdGVyID0gTWVzc2FnZVdyaXRlciA9IHt9KSk7XG5jbGFzcyBBYnN0cmFjdE1lc3NhZ2VXcml0ZXIge1xuICAgIGNvbnN0cnVjdG9yKCkge1xuICAgICAgICB0aGlzLmVycm9yRW1pdHRlciA9IG5ldyBldmVudHNfMS5FbWl0dGVyKCk7XG4gICAgICAgIHRoaXMuY2xvc2VFbWl0dGVyID0gbmV3IGV2ZW50c18xLkVtaXR0ZXIoKTtcbiAgICB9XG4gICAgZGlzcG9zZSgpIHtcbiAgICAgICAgdGhpcy5lcnJvckVtaXR0ZXIuZGlzcG9zZSgpO1xuICAgICAgICB0aGlzLmNsb3NlRW1pdHRlci5kaXNwb3NlKCk7XG4gICAgfVxuICAgIGdldCBvbkVycm9yKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5lcnJvckVtaXR0ZXIuZXZlbnQ7XG4gICAgfVxuICAgIGZpcmVFcnJvcihlcnJvciwgbWVzc2FnZSwgY291bnQpIHtcbiAgICAgICAgdGhpcy5lcnJvckVtaXR0ZXIuZmlyZShbdGhpcy5hc0Vycm9yKGVycm9yKSwgbWVzc2FnZSwgY291bnRdKTtcbiAgICB9XG4gICAgZ2V0IG9uQ2xvc2UoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLmNsb3NlRW1pdHRlci5ldmVudDtcbiAgICB9XG4gICAgZmlyZUNsb3NlKCkge1xuICAgICAgICB0aGlzLmNsb3NlRW1pdHRlci5maXJlKHVuZGVmaW5lZCk7XG4gICAgfVxuICAgIGFzRXJyb3IoZXJyb3IpIHtcbiAgICAgICAgaWYgKGVycm9yIGluc3RhbmNlb2YgRXJyb3IpIHtcbiAgICAgICAgICAgIHJldHVybiBlcnJvcjtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHJldHVybiBuZXcgRXJyb3IoYFdyaXRlciByZWNlaXZlZCBlcnJvci4gUmVhc29uOiAke0lzLnN0cmluZyhlcnJvci5tZXNzYWdlKSA/IGVycm9yLm1lc3NhZ2UgOiAndW5rbm93bid9YCk7XG4gICAgICAgIH1cbiAgICB9XG59XG5leHBvcnRzLkFic3RyYWN0TWVzc2FnZVdyaXRlciA9IEFic3RyYWN0TWVzc2FnZVdyaXRlcjtcbnZhciBSZXNvbHZlZE1lc3NhZ2VXcml0ZXJPcHRpb25zO1xuKGZ1bmN0aW9uIChSZXNvbHZlZE1lc3NhZ2VXcml0ZXJPcHRpb25zKSB7XG4gICAgZnVuY3Rpb24gZnJvbU9wdGlvbnMob3B0aW9ucykge1xuICAgICAgICBpZiAob3B0aW9ucyA9PT0gdW5kZWZpbmVkIHx8IHR5cGVvZiBvcHRpb25zID09PSAnc3RyaW5nJykge1xuICAgICAgICAgICAgcmV0dXJuIHsgY2hhcnNldDogb3B0aW9ucyA/PyAndXRmLTgnLCBjb250ZW50VHlwZUVuY29kZXI6ICgwLCByYWxfMS5kZWZhdWx0KSgpLmFwcGxpY2F0aW9uSnNvbi5lbmNvZGVyIH07XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICByZXR1cm4geyBjaGFyc2V0OiBvcHRpb25zLmNoYXJzZXQgPz8gJ3V0Zi04JywgY29udGVudEVuY29kZXI6IG9wdGlvbnMuY29udGVudEVuY29kZXIsIGNvbnRlbnRUeXBlRW5jb2Rlcjogb3B0aW9ucy5jb250ZW50VHlwZUVuY29kZXIgPz8gKDAsIHJhbF8xLmRlZmF1bHQpKCkuYXBwbGljYXRpb25Kc29uLmVuY29kZXIgfTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBSZXNvbHZlZE1lc3NhZ2VXcml0ZXJPcHRpb25zLmZyb21PcHRpb25zID0gZnJvbU9wdGlvbnM7XG59KShSZXNvbHZlZE1lc3NhZ2VXcml0ZXJPcHRpb25zIHx8IChSZXNvbHZlZE1lc3NhZ2VXcml0ZXJPcHRpb25zID0ge30pKTtcbmNsYXNzIFdyaXRlYWJsZVN0cmVhbU1lc3NhZ2VXcml0ZXIgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VXcml0ZXIge1xuICAgIGNvbnN0cnVjdG9yKHdyaXRhYmxlLCBvcHRpb25zKSB7XG4gICAgICAgIHN1cGVyKCk7XG4gICAgICAgIHRoaXMud3JpdGFibGUgPSB3cml0YWJsZTtcbiAgICAgICAgdGhpcy5vcHRpb25zID0gUmVzb2x2ZWRNZXNzYWdlV3JpdGVyT3B0aW9ucy5mcm9tT3B0aW9ucyhvcHRpb25zKTtcbiAgICAgICAgdGhpcy5lcnJvckNvdW50ID0gMDtcbiAgICAgICAgdGhpcy53cml0ZVNlbWFwaG9yZSA9IG5ldyBzZW1hcGhvcmVfMS5TZW1hcGhvcmUoMSk7XG4gICAgICAgIHRoaXMud3JpdGFibGUub25FcnJvcigoZXJyb3IpID0+IHRoaXMuZmlyZUVycm9yKGVycm9yKSk7XG4gICAgICAgIHRoaXMud3JpdGFibGUub25DbG9zZSgoKSA9PiB0aGlzLmZpcmVDbG9zZSgpKTtcbiAgICB9XG4gICAgYXN5bmMgd3JpdGUobXNnKSB7XG4gICAgICAgIHJldHVybiB0aGlzLndyaXRlU2VtYXBob3JlLmxvY2soYXN5bmMgKCkgPT4ge1xuICAgICAgICAgICAgY29uc3QgcGF5bG9hZCA9IHRoaXMub3B0aW9ucy5jb250ZW50VHlwZUVuY29kZXIuZW5jb2RlKG1zZywgdGhpcy5vcHRpb25zKS50aGVuKChidWZmZXIpID0+IHtcbiAgICAgICAgICAgICAgICBpZiAodGhpcy5vcHRpb25zLmNvbnRlbnRFbmNvZGVyICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIHRoaXMub3B0aW9ucy5jb250ZW50RW5jb2Rlci5lbmNvZGUoYnVmZmVyKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiBidWZmZXI7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICByZXR1cm4gcGF5bG9hZC50aGVuKChidWZmZXIpID0+IHtcbiAgICAgICAgICAgICAgICBjb25zdCBoZWFkZXJzID0gW107XG4gICAgICAgICAgICAgICAgaGVhZGVycy5wdXNoKENvbnRlbnRMZW5ndGgsIGJ1ZmZlci5ieXRlTGVuZ3RoLnRvU3RyaW5nKCksIENSTEYpO1xuICAgICAgICAgICAgICAgIGhlYWRlcnMucHVzaChDUkxGKTtcbiAgICAgICAgICAgICAgICByZXR1cm4gdGhpcy5kb1dyaXRlKG1zZywgaGVhZGVycywgYnVmZmVyKTtcbiAgICAgICAgICAgIH0sIChlcnJvcikgPT4ge1xuICAgICAgICAgICAgICAgIHRoaXMuZmlyZUVycm9yKGVycm9yKTtcbiAgICAgICAgICAgICAgICB0aHJvdyBlcnJvcjtcbiAgICAgICAgICAgIH0pO1xuICAgICAgICB9KTtcbiAgICB9XG4gICAgYXN5bmMgZG9Xcml0ZShtc2csIGhlYWRlcnMsIGRhdGEpIHtcbiAgICAgICAgdHJ5IHtcbiAgICAgICAgICAgIGF3YWl0IHRoaXMud3JpdGFibGUud3JpdGUoaGVhZGVycy5qb2luKCcnKSwgJ2FzY2lpJyk7XG4gICAgICAgICAgICByZXR1cm4gdGhpcy53cml0YWJsZS53cml0ZShkYXRhKTtcbiAgICAgICAgfVxuICAgICAgICBjYXRjaCAoZXJyb3IpIHtcbiAgICAgICAgICAgIHRoaXMuaGFuZGxlRXJyb3IoZXJyb3IsIG1zZyk7XG4gICAgICAgICAgICByZXR1cm4gUHJvbWlzZS5yZWplY3QoZXJyb3IpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGhhbmRsZUVycm9yKGVycm9yLCBtc2cpIHtcbiAgICAgICAgdGhpcy5lcnJvckNvdW50Kys7XG4gICAgICAgIHRoaXMuZmlyZUVycm9yKGVycm9yLCBtc2csIHRoaXMuZXJyb3JDb3VudCk7XG4gICAgfVxuICAgIGVuZCgpIHtcbiAgICAgICAgdGhpcy53cml0YWJsZS5lbmQoKTtcbiAgICB9XG59XG5leHBvcnRzLldyaXRlYWJsZVN0cmVhbU1lc3NhZ2VXcml0ZXIgPSBXcml0ZWFibGVTdHJlYW1NZXNzYWdlV3JpdGVyO1xuIiwgIlwidXNlIHN0cmljdFwiO1xuLyotLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbiAqICBDb3B5cmlnaHQgKGMpIE1pY3Jvc29mdCBDb3Jwb3JhdGlvbi4gQWxsIHJpZ2h0cyByZXNlcnZlZC5cbiAqICBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICotLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSovXG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJfX2VzTW9kdWxlXCIsIHsgdmFsdWU6IHRydWUgfSk7XG5leHBvcnRzLkFic3RyYWN0TWVzc2FnZUJ1ZmZlciA9IHZvaWQgMDtcbmNvbnN0IENSID0gMTM7XG5jb25zdCBMRiA9IDEwO1xuY29uc3QgQ1JMRiA9ICdcXHJcXG4nO1xuY2xhc3MgQWJzdHJhY3RNZXNzYWdlQnVmZmVyIHtcbiAgICBjb25zdHJ1Y3RvcihlbmNvZGluZyA9ICd1dGYtOCcpIHtcbiAgICAgICAgdGhpcy5fZW5jb2RpbmcgPSBlbmNvZGluZztcbiAgICAgICAgdGhpcy5fY2h1bmtzID0gW107XG4gICAgICAgIHRoaXMuX3RvdGFsTGVuZ3RoID0gMDtcbiAgICB9XG4gICAgZ2V0IGVuY29kaW5nKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5fZW5jb2Rpbmc7XG4gICAgfVxuICAgIGFwcGVuZChjaHVuaykge1xuICAgICAgICBjb25zdCB0b0FwcGVuZCA9IHR5cGVvZiBjaHVuayA9PT0gJ3N0cmluZycgPyB0aGlzLmZyb21TdHJpbmcoY2h1bmssIHRoaXMuX2VuY29kaW5nKSA6IGNodW5rO1xuICAgICAgICB0aGlzLl9jaHVua3MucHVzaCh0b0FwcGVuZCk7XG4gICAgICAgIHRoaXMuX3RvdGFsTGVuZ3RoICs9IHRvQXBwZW5kLmJ5dGVMZW5ndGg7XG4gICAgfVxuICAgIHRyeVJlYWRIZWFkZXJzKGxvd2VyQ2FzZUtleXMgPSBmYWxzZSkge1xuICAgICAgICBpZiAodGhpcy5fY2h1bmtzLmxlbmd0aCA9PT0gMCkge1xuICAgICAgICAgICAgcmV0dXJuIHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgICAgICBsZXQgc3RhdGUgPSAwO1xuICAgICAgICBsZXQgY2h1bmtJbmRleCA9IDA7XG4gICAgICAgIGxldCBvZmZzZXQgPSAwO1xuICAgICAgICBsZXQgY2h1bmtCeXRlc1JlYWQgPSAwO1xuICAgICAgICByb3c6IHdoaWxlIChjaHVua0luZGV4IDwgdGhpcy5fY2h1bmtzLmxlbmd0aCkge1xuICAgICAgICAgICAgY29uc3QgY2h1bmsgPSB0aGlzLl9jaHVua3NbY2h1bmtJbmRleF07XG4gICAgICAgICAgICBvZmZzZXQgPSAwO1xuICAgICAgICAgICAgY29sdW1uOiB3aGlsZSAob2Zmc2V0IDwgY2h1bmsubGVuZ3RoKSB7XG4gICAgICAgICAgICAgICAgY29uc3QgdmFsdWUgPSBjaHVua1tvZmZzZXRdO1xuICAgICAgICAgICAgICAgIHN3aXRjaCAodmFsdWUpIHtcbiAgICAgICAgICAgICAgICAgICAgY2FzZSBDUjpcbiAgICAgICAgICAgICAgICAgICAgICAgIHN3aXRjaCAoc3RhdGUpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBjYXNlIDA6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHN0YXRlID0gMTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgY2FzZSAyOlxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBzdGF0ZSA9IDM7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGRlZmF1bHQ6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHN0YXRlID0gMDtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICBjYXNlIExGOlxuICAgICAgICAgICAgICAgICAgICAgICAgc3dpdGNoIChzdGF0ZSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNhc2UgMTpcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgc3RhdGUgPSAyO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBjYXNlIDM6XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHN0YXRlID0gNDtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgb2Zmc2V0Kys7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrIHJvdztcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBkZWZhdWx0OlxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBzdGF0ZSA9IDA7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICAgICAgZGVmYXVsdDpcbiAgICAgICAgICAgICAgICAgICAgICAgIHN0YXRlID0gMDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgb2Zmc2V0Kys7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBjaHVua0J5dGVzUmVhZCArPSBjaHVuay5ieXRlTGVuZ3RoO1xuICAgICAgICAgICAgY2h1bmtJbmRleCsrO1xuICAgICAgICB9XG4gICAgICAgIGlmIChzdGF0ZSAhPT0gNCkge1xuICAgICAgICAgICAgcmV0dXJuIHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgICAgICAvLyBUaGUgYnVmZmVyIGNvbnRhaW5zIHRoZSB0d28gQ1JMRiBhdCB0aGUgZW5kLiBTbyB3ZSB3aWxsXG4gICAgICAgIC8vIGhhdmUgdHdvIGVtcHR5IGxpbmVzIGFmdGVyIHRoZSBzcGxpdCBhdCB0aGUgZW5kIGFzIHdlbGwuXG4gICAgICAgIGNvbnN0IGJ1ZmZlciA9IHRoaXMuX3JlYWQoY2h1bmtCeXRlc1JlYWQgKyBvZmZzZXQpO1xuICAgICAgICBjb25zdCByZXN1bHQgPSBuZXcgTWFwKCk7XG4gICAgICAgIGNvbnN0IGhlYWRlcnMgPSB0aGlzLnRvU3RyaW5nKGJ1ZmZlciwgJ2FzY2lpJykuc3BsaXQoQ1JMRik7XG4gICAgICAgIGlmIChoZWFkZXJzLmxlbmd0aCA8IDIpIHtcbiAgICAgICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgICAgIH1cbiAgICAgICAgZm9yIChsZXQgaSA9IDA7IGkgPCBoZWFkZXJzLmxlbmd0aCAtIDI7IGkrKykge1xuICAgICAgICAgICAgY29uc3QgaGVhZGVyID0gaGVhZGVyc1tpXTtcbiAgICAgICAgICAgIGNvbnN0IGluZGV4ID0gaGVhZGVyLmluZGV4T2YoJzonKTtcbiAgICAgICAgICAgIGlmIChpbmRleCA9PT0gLTEpIHtcbiAgICAgICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYE1lc3NhZ2UgaGVhZGVyIG11c3Qgc2VwYXJhdGUga2V5IGFuZCB2YWx1ZSB1c2luZyAnOidcXG4ke2hlYWRlcn1gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNvbnN0IGtleSA9IGhlYWRlci5zdWJzdHIoMCwgaW5kZXgpO1xuICAgICAgICAgICAgY29uc3QgdmFsdWUgPSBoZWFkZXIuc3Vic3RyKGluZGV4ICsgMSkudHJpbSgpO1xuICAgICAgICAgICAgcmVzdWx0LnNldChsb3dlckNhc2VLZXlzID8ga2V5LnRvTG93ZXJDYXNlKCkgOiBrZXksIHZhbHVlKTtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cbiAgICB0cnlSZWFkQm9keShsZW5ndGgpIHtcbiAgICAgICAgaWYgKHRoaXMuX3RvdGFsTGVuZ3RoIDwgbGVuZ3RoKSB7XG4gICAgICAgICAgICByZXR1cm4gdW5kZWZpbmVkO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiB0aGlzLl9yZWFkKGxlbmd0aCk7XG4gICAgfVxuICAgIGdldCBudW1iZXJPZkJ5dGVzKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5fdG90YWxMZW5ndGg7XG4gICAgfVxuICAgIF9yZWFkKGJ5dGVDb3VudCkge1xuICAgICAgICBpZiAoYnl0ZUNvdW50ID09PSAwKSB7XG4gICAgICAgICAgICByZXR1cm4gdGhpcy5lbXB0eUJ1ZmZlcigpO1xuICAgICAgICB9XG4gICAgICAgIGlmIChieXRlQ291bnQgPiB0aGlzLl90b3RhbExlbmd0aCkge1xuICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBDYW5ub3QgcmVhZCBzbyBtYW55IGJ5dGVzIWApO1xuICAgICAgICB9XG4gICAgICAgIGlmICh0aGlzLl9jaHVua3NbMF0uYnl0ZUxlbmd0aCA9PT0gYnl0ZUNvdW50KSB7XG4gICAgICAgICAgICAvLyBzdXBlciBmYXN0IHBhdGgsIHByZWNpc2VseSBmaXJzdCBjaHVuayBtdXN0IGJlIHJldHVybmVkXG4gICAgICAgICAgICBjb25zdCBjaHVuayA9IHRoaXMuX2NodW5rc1swXTtcbiAgICAgICAgICAgIHRoaXMuX2NodW5rcy5zaGlmdCgpO1xuICAgICAgICAgICAgdGhpcy5fdG90YWxMZW5ndGggLT0gYnl0ZUNvdW50O1xuICAgICAgICAgICAgcmV0dXJuIHRoaXMuYXNOYXRpdmUoY2h1bmspO1xuICAgICAgICB9XG4gICAgICAgIGlmICh0aGlzLl9jaHVua3NbMF0uYnl0ZUxlbmd0aCA+IGJ5dGVDb3VudCkge1xuICAgICAgICAgICAgLy8gZmFzdCBwYXRoLCB0aGUgcmVhZGluZyBpcyBlbnRpcmVseSB3aXRoaW4gdGhlIGZpcnN0IGNodW5rXG4gICAgICAgICAgICBjb25zdCBjaHVuayA9IHRoaXMuX2NodW5rc1swXTtcbiAgICAgICAgICAgIGNvbnN0IHJlc3VsdCA9IHRoaXMuYXNOYXRpdmUoY2h1bmssIGJ5dGVDb3VudCk7XG4gICAgICAgICAgICB0aGlzLl9jaHVua3NbMF0gPSBjaHVuay5zbGljZShieXRlQ291bnQpO1xuICAgICAgICAgICAgdGhpcy5fdG90YWxMZW5ndGggLT0gYnl0ZUNvdW50O1xuICAgICAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICAgICAgfVxuICAgICAgICBjb25zdCByZXN1bHQgPSB0aGlzLmFsbG9jTmF0aXZlKGJ5dGVDb3VudCk7XG4gICAgICAgIGxldCByZXN1bHRPZmZzZXQgPSAwO1xuICAgICAgICBsZXQgY2h1bmtJbmRleCA9IDA7XG4gICAgICAgIHdoaWxlIChieXRlQ291bnQgPiAwKSB7XG4gICAgICAgICAgICBjb25zdCBjaHVuayA9IHRoaXMuX2NodW5rc1tjaHVua0luZGV4XTtcbiAgICAgICAgICAgIGlmIChjaHVuay5ieXRlTGVuZ3RoID4gYnl0ZUNvdW50KSB7XG4gICAgICAgICAgICAgICAgLy8gdGhpcyBjaHVuayB3aWxsIHN1cnZpdmVcbiAgICAgICAgICAgICAgICBjb25zdCBjaHVua1BhcnQgPSBjaHVuay5zbGljZSgwLCBieXRlQ291bnQpO1xuICAgICAgICAgICAgICAgIHJlc3VsdC5zZXQoY2h1bmtQYXJ0LCByZXN1bHRPZmZzZXQpO1xuICAgICAgICAgICAgICAgIHJlc3VsdE9mZnNldCArPSBieXRlQ291bnQ7XG4gICAgICAgICAgICAgICAgdGhpcy5fY2h1bmtzW2NodW5rSW5kZXhdID0gY2h1bmsuc2xpY2UoYnl0ZUNvdW50KTtcbiAgICAgICAgICAgICAgICB0aGlzLl90b3RhbExlbmd0aCAtPSBieXRlQ291bnQ7XG4gICAgICAgICAgICAgICAgYnl0ZUNvdW50IC09IGJ5dGVDb3VudDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIC8vIHRoaXMgY2h1bmsgd2lsbCBiZSBlbnRpcmVseSByZWFkXG4gICAgICAgICAgICAgICAgcmVzdWx0LnNldChjaHVuaywgcmVzdWx0T2Zmc2V0KTtcbiAgICAgICAgICAgICAgICByZXN1bHRPZmZzZXQgKz0gY2h1bmsuYnl0ZUxlbmd0aDtcbiAgICAgICAgICAgICAgICB0aGlzLl9jaHVua3Muc2hpZnQoKTtcbiAgICAgICAgICAgICAgICB0aGlzLl90b3RhbExlbmd0aCAtPSBjaHVuay5ieXRlTGVuZ3RoO1xuICAgICAgICAgICAgICAgIGJ5dGVDb3VudCAtPSBjaHVuay5ieXRlTGVuZ3RoO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxufVxuZXhwb3J0cy5BYnN0cmFjdE1lc3NhZ2VCdWZmZXIgPSBBYnN0cmFjdE1lc3NhZ2VCdWZmZXI7XG4iLCAiXCJ1c2Ugc3RyaWN0XCI7XG4vKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJfX2VzTW9kdWxlXCIsIHsgdmFsdWU6IHRydWUgfSk7XG5leHBvcnRzLmNyZWF0ZU1lc3NhZ2VDb25uZWN0aW9uID0gZXhwb3J0cy5Db25uZWN0aW9uT3B0aW9ucyA9IGV4cG9ydHMuTWVzc2FnZVN0cmF0ZWd5ID0gZXhwb3J0cy5DYW5jZWxsYXRpb25TdHJhdGVneSA9IGV4cG9ydHMuQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3kgPSBleHBvcnRzLkNhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kgPSBleHBvcnRzLlJlcXVlc3RDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5ID0gZXhwb3J0cy5JZENhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kgPSBleHBvcnRzLkNvbm5lY3Rpb25TdHJhdGVneSA9IGV4cG9ydHMuQ29ubmVjdGlvbkVycm9yID0gZXhwb3J0cy5Db25uZWN0aW9uRXJyb3JzID0gZXhwb3J0cy5Mb2dUcmFjZU5vdGlmaWNhdGlvbiA9IGV4cG9ydHMuU2V0VHJhY2VOb3RpZmljYXRpb24gPSBleHBvcnRzLlRyYWNlRm9ybWF0ID0gZXhwb3J0cy5UcmFjZVZhbHVlcyA9IGV4cG9ydHMuVHJhY2UgPSBleHBvcnRzLk51bGxMb2dnZXIgPSBleHBvcnRzLlByb2dyZXNzVHlwZSA9IGV4cG9ydHMuUHJvZ3Jlc3NUb2tlbiA9IHZvaWQgMDtcbmNvbnN0IHJhbF8xID0gcmVxdWlyZShcIi4vcmFsXCIpO1xuY29uc3QgSXMgPSByZXF1aXJlKFwiLi9pc1wiKTtcbmNvbnN0IG1lc3NhZ2VzXzEgPSByZXF1aXJlKFwiLi9tZXNzYWdlc1wiKTtcbmNvbnN0IGxpbmtlZE1hcF8xID0gcmVxdWlyZShcIi4vbGlua2VkTWFwXCIpO1xuY29uc3QgZXZlbnRzXzEgPSByZXF1aXJlKFwiLi9ldmVudHNcIik7XG5jb25zdCBjYW5jZWxsYXRpb25fMSA9IHJlcXVpcmUoXCIuL2NhbmNlbGxhdGlvblwiKTtcbnZhciBDYW5jZWxOb3RpZmljYXRpb247XG4oZnVuY3Rpb24gKENhbmNlbE5vdGlmaWNhdGlvbikge1xuICAgIENhbmNlbE5vdGlmaWNhdGlvbi50eXBlID0gbmV3IG1lc3NhZ2VzXzEuTm90aWZpY2F0aW9uVHlwZSgnJC9jYW5jZWxSZXF1ZXN0Jyk7XG59KShDYW5jZWxOb3RpZmljYXRpb24gfHwgKENhbmNlbE5vdGlmaWNhdGlvbiA9IHt9KSk7XG52YXIgUHJvZ3Jlc3NUb2tlbjtcbihmdW5jdGlvbiAoUHJvZ3Jlc3NUb2tlbikge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiB0eXBlb2YgdmFsdWUgPT09ICdzdHJpbmcnIHx8IHR5cGVvZiB2YWx1ZSA9PT0gJ251bWJlcic7XG4gICAgfVxuICAgIFByb2dyZXNzVG9rZW4uaXMgPSBpcztcbn0pKFByb2dyZXNzVG9rZW4gfHwgKGV4cG9ydHMuUHJvZ3Jlc3NUb2tlbiA9IFByb2dyZXNzVG9rZW4gPSB7fSkpO1xudmFyIFByb2dyZXNzTm90aWZpY2F0aW9uO1xuKGZ1bmN0aW9uIChQcm9ncmVzc05vdGlmaWNhdGlvbikge1xuICAgIFByb2dyZXNzTm90aWZpY2F0aW9uLnR5cGUgPSBuZXcgbWVzc2FnZXNfMS5Ob3RpZmljYXRpb25UeXBlKCckL3Byb2dyZXNzJyk7XG59KShQcm9ncmVzc05vdGlmaWNhdGlvbiB8fCAoUHJvZ3Jlc3NOb3RpZmljYXRpb24gPSB7fSkpO1xuY2xhc3MgUHJvZ3Jlc3NUeXBlIHtcbiAgICBjb25zdHJ1Y3RvcigpIHtcbiAgICB9XG59XG5leHBvcnRzLlByb2dyZXNzVHlwZSA9IFByb2dyZXNzVHlwZTtcbnZhciBTdGFyUmVxdWVzdEhhbmRsZXI7XG4oZnVuY3Rpb24gKFN0YXJSZXF1ZXN0SGFuZGxlcikge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiBJcy5mdW5jKHZhbHVlKTtcbiAgICB9XG4gICAgU3RhclJlcXVlc3RIYW5kbGVyLmlzID0gaXM7XG59KShTdGFyUmVxdWVzdEhhbmRsZXIgfHwgKFN0YXJSZXF1ZXN0SGFuZGxlciA9IHt9KSk7XG5leHBvcnRzLk51bGxMb2dnZXIgPSBPYmplY3QuZnJlZXplKHtcbiAgICBlcnJvcjogKCkgPT4geyB9LFxuICAgIHdhcm46ICgpID0+IHsgfSxcbiAgICBpbmZvOiAoKSA9PiB7IH0sXG4gICAgbG9nOiAoKSA9PiB7IH1cbn0pO1xudmFyIFRyYWNlO1xuKGZ1bmN0aW9uIChUcmFjZSkge1xuICAgIFRyYWNlW1RyYWNlW1wiT2ZmXCJdID0gMF0gPSBcIk9mZlwiO1xuICAgIFRyYWNlW1RyYWNlW1wiTWVzc2FnZXNcIl0gPSAxXSA9IFwiTWVzc2FnZXNcIjtcbiAgICBUcmFjZVtUcmFjZVtcIkNvbXBhY3RcIl0gPSAyXSA9IFwiQ29tcGFjdFwiO1xuICAgIFRyYWNlW1RyYWNlW1wiVmVyYm9zZVwiXSA9IDNdID0gXCJWZXJib3NlXCI7XG59KShUcmFjZSB8fCAoZXhwb3J0cy5UcmFjZSA9IFRyYWNlID0ge30pKTtcbnZhciBUcmFjZVZhbHVlcztcbihmdW5jdGlvbiAoVHJhY2VWYWx1ZXMpIHtcbiAgICAvKipcbiAgICAgKiBUdXJuIHRyYWNpbmcgb2ZmLlxuICAgICAqL1xuICAgIFRyYWNlVmFsdWVzLk9mZiA9ICdvZmYnO1xuICAgIC8qKlxuICAgICAqIFRyYWNlIG1lc3NhZ2VzIG9ubHkuXG4gICAgICovXG4gICAgVHJhY2VWYWx1ZXMuTWVzc2FnZXMgPSAnbWVzc2FnZXMnO1xuICAgIC8qKlxuICAgICAqIENvbXBhY3QgbWVzc2FnZSB0cmFjaW5nLlxuICAgICAqL1xuICAgIFRyYWNlVmFsdWVzLkNvbXBhY3QgPSAnY29tcGFjdCc7XG4gICAgLyoqXG4gICAgICogVmVyYm9zZSBtZXNzYWdlIHRyYWNpbmcuXG4gICAgICovXG4gICAgVHJhY2VWYWx1ZXMuVmVyYm9zZSA9ICd2ZXJib3NlJztcbn0pKFRyYWNlVmFsdWVzIHx8IChleHBvcnRzLlRyYWNlVmFsdWVzID0gVHJhY2VWYWx1ZXMgPSB7fSkpO1xuKGZ1bmN0aW9uIChUcmFjZSkge1xuICAgIGZ1bmN0aW9uIGZyb21TdHJpbmcodmFsdWUpIHtcbiAgICAgICAgaWYgKCFJcy5zdHJpbmcodmFsdWUpKSB7XG4gICAgICAgICAgICByZXR1cm4gVHJhY2UuT2ZmO1xuICAgICAgICB9XG4gICAgICAgIHZhbHVlID0gdmFsdWUudG9Mb3dlckNhc2UoKTtcbiAgICAgICAgc3dpdGNoICh2YWx1ZSkge1xuICAgICAgICAgICAgY2FzZSAnb2ZmJzpcbiAgICAgICAgICAgICAgICByZXR1cm4gVHJhY2UuT2ZmO1xuICAgICAgICAgICAgY2FzZSAnbWVzc2FnZXMnOlxuICAgICAgICAgICAgICAgIHJldHVybiBUcmFjZS5NZXNzYWdlcztcbiAgICAgICAgICAgIGNhc2UgJ2NvbXBhY3QnOlxuICAgICAgICAgICAgICAgIHJldHVybiBUcmFjZS5Db21wYWN0O1xuICAgICAgICAgICAgY2FzZSAndmVyYm9zZSc6XG4gICAgICAgICAgICAgICAgcmV0dXJuIFRyYWNlLlZlcmJvc2U7XG4gICAgICAgICAgICBkZWZhdWx0OlxuICAgICAgICAgICAgICAgIHJldHVybiBUcmFjZS5PZmY7XG4gICAgICAgIH1cbiAgICB9XG4gICAgVHJhY2UuZnJvbVN0cmluZyA9IGZyb21TdHJpbmc7XG4gICAgZnVuY3Rpb24gdG9TdHJpbmcodmFsdWUpIHtcbiAgICAgICAgc3dpdGNoICh2YWx1ZSkge1xuICAgICAgICAgICAgY2FzZSBUcmFjZS5PZmY6XG4gICAgICAgICAgICAgICAgcmV0dXJuICdvZmYnO1xuICAgICAgICAgICAgY2FzZSBUcmFjZS5NZXNzYWdlczpcbiAgICAgICAgICAgICAgICByZXR1cm4gJ21lc3NhZ2VzJztcbiAgICAgICAgICAgIGNhc2UgVHJhY2UuQ29tcGFjdDpcbiAgICAgICAgICAgICAgICByZXR1cm4gJ2NvbXBhY3QnO1xuICAgICAgICAgICAgY2FzZSBUcmFjZS5WZXJib3NlOlxuICAgICAgICAgICAgICAgIHJldHVybiAndmVyYm9zZSc7XG4gICAgICAgICAgICBkZWZhdWx0OlxuICAgICAgICAgICAgICAgIHJldHVybiAnb2ZmJztcbiAgICAgICAgfVxuICAgIH1cbiAgICBUcmFjZS50b1N0cmluZyA9IHRvU3RyaW5nO1xufSkoVHJhY2UgfHwgKGV4cG9ydHMuVHJhY2UgPSBUcmFjZSA9IHt9KSk7XG52YXIgVHJhY2VGb3JtYXQ7XG4oZnVuY3Rpb24gKFRyYWNlRm9ybWF0KSB7XG4gICAgVHJhY2VGb3JtYXRbXCJUZXh0XCJdID0gXCJ0ZXh0XCI7XG4gICAgVHJhY2VGb3JtYXRbXCJKU09OXCJdID0gXCJqc29uXCI7XG59KShUcmFjZUZvcm1hdCB8fCAoZXhwb3J0cy5UcmFjZUZvcm1hdCA9IFRyYWNlRm9ybWF0ID0ge30pKTtcbihmdW5jdGlvbiAoVHJhY2VGb3JtYXQpIHtcbiAgICBmdW5jdGlvbiBmcm9tU3RyaW5nKHZhbHVlKSB7XG4gICAgICAgIGlmICghSXMuc3RyaW5nKHZhbHVlKSkge1xuICAgICAgICAgICAgcmV0dXJuIFRyYWNlRm9ybWF0LlRleHQ7XG4gICAgICAgIH1cbiAgICAgICAgdmFsdWUgPSB2YWx1ZS50b0xvd2VyQ2FzZSgpO1xuICAgICAgICBpZiAodmFsdWUgPT09ICdqc29uJykge1xuICAgICAgICAgICAgcmV0dXJuIFRyYWNlRm9ybWF0LkpTT047XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICByZXR1cm4gVHJhY2VGb3JtYXQuVGV4dDtcbiAgICAgICAgfVxuICAgIH1cbiAgICBUcmFjZUZvcm1hdC5mcm9tU3RyaW5nID0gZnJvbVN0cmluZztcbn0pKFRyYWNlRm9ybWF0IHx8IChleHBvcnRzLlRyYWNlRm9ybWF0ID0gVHJhY2VGb3JtYXQgPSB7fSkpO1xudmFyIFNldFRyYWNlTm90aWZpY2F0aW9uO1xuKGZ1bmN0aW9uIChTZXRUcmFjZU5vdGlmaWNhdGlvbikge1xuICAgIFNldFRyYWNlTm90aWZpY2F0aW9uLnR5cGUgPSBuZXcgbWVzc2FnZXNfMS5Ob3RpZmljYXRpb25UeXBlKCckL3NldFRyYWNlJyk7XG59KShTZXRUcmFjZU5vdGlmaWNhdGlvbiB8fCAoZXhwb3J0cy5TZXRUcmFjZU5vdGlmaWNhdGlvbiA9IFNldFRyYWNlTm90aWZpY2F0aW9uID0ge30pKTtcbnZhciBMb2dUcmFjZU5vdGlmaWNhdGlvbjtcbihmdW5jdGlvbiAoTG9nVHJhY2VOb3RpZmljYXRpb24pIHtcbiAgICBMb2dUcmFjZU5vdGlmaWNhdGlvbi50eXBlID0gbmV3IG1lc3NhZ2VzXzEuTm90aWZpY2F0aW9uVHlwZSgnJC9sb2dUcmFjZScpO1xufSkoTG9nVHJhY2VOb3RpZmljYXRpb24gfHwgKGV4cG9ydHMuTG9nVHJhY2VOb3RpZmljYXRpb24gPSBMb2dUcmFjZU5vdGlmaWNhdGlvbiA9IHt9KSk7XG52YXIgQ29ubmVjdGlvbkVycm9ycztcbihmdW5jdGlvbiAoQ29ubmVjdGlvbkVycm9ycykge1xuICAgIC8qKlxuICAgICAqIFRoZSBjb25uZWN0aW9uIGlzIGNsb3NlZC5cbiAgICAgKi9cbiAgICBDb25uZWN0aW9uRXJyb3JzW0Nvbm5lY3Rpb25FcnJvcnNbXCJDbG9zZWRcIl0gPSAxXSA9IFwiQ2xvc2VkXCI7XG4gICAgLyoqXG4gICAgICogVGhlIGNvbm5lY3Rpb24gZ290IGRpc3Bvc2VkLlxuICAgICAqL1xuICAgIENvbm5lY3Rpb25FcnJvcnNbQ29ubmVjdGlvbkVycm9yc1tcIkRpc3Bvc2VkXCJdID0gMl0gPSBcIkRpc3Bvc2VkXCI7XG4gICAgLyoqXG4gICAgICogVGhlIGNvbm5lY3Rpb24gaXMgYWxyZWFkeSBpbiBsaXN0ZW5pbmcgbW9kZS5cbiAgICAgKi9cbiAgICBDb25uZWN0aW9uRXJyb3JzW0Nvbm5lY3Rpb25FcnJvcnNbXCJBbHJlYWR5TGlzdGVuaW5nXCJdID0gM10gPSBcIkFscmVhZHlMaXN0ZW5pbmdcIjtcbn0pKENvbm5lY3Rpb25FcnJvcnMgfHwgKGV4cG9ydHMuQ29ubmVjdGlvbkVycm9ycyA9IENvbm5lY3Rpb25FcnJvcnMgPSB7fSkpO1xuY2xhc3MgQ29ubmVjdGlvbkVycm9yIGV4dGVuZHMgRXJyb3Ige1xuICAgIGNvbnN0cnVjdG9yKGNvZGUsIG1lc3NhZ2UpIHtcbiAgICAgICAgc3VwZXIobWVzc2FnZSk7XG4gICAgICAgIHRoaXMuY29kZSA9IGNvZGU7XG4gICAgICAgIE9iamVjdC5zZXRQcm90b3R5cGVPZih0aGlzLCBDb25uZWN0aW9uRXJyb3IucHJvdG90eXBlKTtcbiAgICB9XG59XG5leHBvcnRzLkNvbm5lY3Rpb25FcnJvciA9IENvbm5lY3Rpb25FcnJvcjtcbnZhciBDb25uZWN0aW9uU3RyYXRlZ3k7XG4oZnVuY3Rpb24gKENvbm5lY3Rpb25TdHJhdGVneSkge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICYmIElzLmZ1bmMoY2FuZGlkYXRlLmNhbmNlbFVuZGlzcGF0Y2hlZCk7XG4gICAgfVxuICAgIENvbm5lY3Rpb25TdHJhdGVneS5pcyA9IGlzO1xufSkoQ29ubmVjdGlvblN0cmF0ZWd5IHx8IChleHBvcnRzLkNvbm5lY3Rpb25TdHJhdGVneSA9IENvbm5lY3Rpb25TdHJhdGVneSA9IHt9KSk7XG52YXIgSWRDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5O1xuKGZ1bmN0aW9uIChJZENhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kpIHtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiAoY2FuZGlkYXRlLmtpbmQgPT09IHVuZGVmaW5lZCB8fCBjYW5kaWRhdGUua2luZCA9PT0gJ2lkJykgJiYgSXMuZnVuYyhjYW5kaWRhdGUuY3JlYXRlQ2FuY2VsbGF0aW9uVG9rZW5Tb3VyY2UpICYmIChjYW5kaWRhdGUuZGlzcG9zZSA9PT0gdW5kZWZpbmVkIHx8IElzLmZ1bmMoY2FuZGlkYXRlLmRpc3Bvc2UpKTtcbiAgICB9XG4gICAgSWRDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5LmlzID0gaXM7XG59KShJZENhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kgfHwgKGV4cG9ydHMuSWRDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5ID0gSWRDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5ID0ge30pKTtcbnZhciBSZXF1ZXN0Q2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneTtcbihmdW5jdGlvbiAoUmVxdWVzdENhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kpIHtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiBjYW5kaWRhdGUua2luZCA9PT0gJ3JlcXVlc3QnICYmIElzLmZ1bmMoY2FuZGlkYXRlLmNyZWF0ZUNhbmNlbGxhdGlvblRva2VuU291cmNlKSAmJiAoY2FuZGlkYXRlLmRpc3Bvc2UgPT09IHVuZGVmaW5lZCB8fCBJcy5mdW5jKGNhbmRpZGF0ZS5kaXNwb3NlKSk7XG4gICAgfVxuICAgIFJlcXVlc3RDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5LmlzID0gaXM7XG59KShSZXF1ZXN0Q2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneSB8fCAoZXhwb3J0cy5SZXF1ZXN0Q2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneSA9IFJlcXVlc3RDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5ID0ge30pKTtcbnZhciBDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5O1xuKGZ1bmN0aW9uIChDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5KSB7XG4gICAgQ2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneS5NZXNzYWdlID0gT2JqZWN0LmZyZWV6ZSh7XG4gICAgICAgIGNyZWF0ZUNhbmNlbGxhdGlvblRva2VuU291cmNlKF8pIHtcbiAgICAgICAgICAgIHJldHVybiBuZXcgY2FuY2VsbGF0aW9uXzEuQ2FuY2VsbGF0aW9uVG9rZW5Tb3VyY2UoKTtcbiAgICAgICAgfVxuICAgIH0pO1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiBJZENhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kuaXModmFsdWUpIHx8IFJlcXVlc3RDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5LmlzKHZhbHVlKTtcbiAgICB9XG4gICAgQ2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneS5pcyA9IGlzO1xufSkoQ2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneSB8fCAoZXhwb3J0cy5DYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5ID0gQ2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneSA9IHt9KSk7XG52YXIgQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3k7XG4oZnVuY3Rpb24gKENhbmNlbGxhdGlvblNlbmRlclN0cmF0ZWd5KSB7XG4gICAgQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3kuTWVzc2FnZSA9IE9iamVjdC5mcmVlemUoe1xuICAgICAgICBzZW5kQ2FuY2VsbGF0aW9uKGNvbm4sIGlkKSB7XG4gICAgICAgICAgICByZXR1cm4gY29ubi5zZW5kTm90aWZpY2F0aW9uKENhbmNlbE5vdGlmaWNhdGlvbi50eXBlLCB7IGlkIH0pO1xuICAgICAgICB9LFxuICAgICAgICBjbGVhbnVwKF8pIHsgfVxuICAgIH0pO1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICYmIElzLmZ1bmMoY2FuZGlkYXRlLnNlbmRDYW5jZWxsYXRpb24pICYmIElzLmZ1bmMoY2FuZGlkYXRlLmNsZWFudXApO1xuICAgIH1cbiAgICBDYW5jZWxsYXRpb25TZW5kZXJTdHJhdGVneS5pcyA9IGlzO1xufSkoQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3kgfHwgKGV4cG9ydHMuQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3kgPSBDYW5jZWxsYXRpb25TZW5kZXJTdHJhdGVneSA9IHt9KSk7XG52YXIgQ2FuY2VsbGF0aW9uU3RyYXRlZ3k7XG4oZnVuY3Rpb24gKENhbmNlbGxhdGlvblN0cmF0ZWd5KSB7XG4gICAgQ2FuY2VsbGF0aW9uU3RyYXRlZ3kuTWVzc2FnZSA9IE9iamVjdC5mcmVlemUoe1xuICAgICAgICByZWNlaXZlcjogQ2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneS5NZXNzYWdlLFxuICAgICAgICBzZW5kZXI6IENhbmNlbGxhdGlvblNlbmRlclN0cmF0ZWd5Lk1lc3NhZ2VcbiAgICB9KTtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiBDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5LmlzKGNhbmRpZGF0ZS5yZWNlaXZlcikgJiYgQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3kuaXMoY2FuZGlkYXRlLnNlbmRlcik7XG4gICAgfVxuICAgIENhbmNlbGxhdGlvblN0cmF0ZWd5LmlzID0gaXM7XG59KShDYW5jZWxsYXRpb25TdHJhdGVneSB8fCAoZXhwb3J0cy5DYW5jZWxsYXRpb25TdHJhdGVneSA9IENhbmNlbGxhdGlvblN0cmF0ZWd5ID0ge30pKTtcbnZhciBNZXNzYWdlU3RyYXRlZ3k7XG4oZnVuY3Rpb24gKE1lc3NhZ2VTdHJhdGVneSkge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICYmIElzLmZ1bmMoY2FuZGlkYXRlLmhhbmRsZU1lc3NhZ2UpO1xuICAgIH1cbiAgICBNZXNzYWdlU3RyYXRlZ3kuaXMgPSBpcztcbn0pKE1lc3NhZ2VTdHJhdGVneSB8fCAoZXhwb3J0cy5NZXNzYWdlU3RyYXRlZ3kgPSBNZXNzYWdlU3RyYXRlZ3kgPSB7fSkpO1xudmFyIENvbm5lY3Rpb25PcHRpb25zO1xuKGZ1bmN0aW9uIChDb25uZWN0aW9uT3B0aW9ucykge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICYmIChDYW5jZWxsYXRpb25TdHJhdGVneS5pcyhjYW5kaWRhdGUuY2FuY2VsbGF0aW9uU3RyYXRlZ3kpIHx8IENvbm5lY3Rpb25TdHJhdGVneS5pcyhjYW5kaWRhdGUuY29ubmVjdGlvblN0cmF0ZWd5KSB8fCBNZXNzYWdlU3RyYXRlZ3kuaXMoY2FuZGlkYXRlLm1lc3NhZ2VTdHJhdGVneSkpO1xuICAgIH1cbiAgICBDb25uZWN0aW9uT3B0aW9ucy5pcyA9IGlzO1xufSkoQ29ubmVjdGlvbk9wdGlvbnMgfHwgKGV4cG9ydHMuQ29ubmVjdGlvbk9wdGlvbnMgPSBDb25uZWN0aW9uT3B0aW9ucyA9IHt9KSk7XG52YXIgQ29ubmVjdGlvblN0YXRlO1xuKGZ1bmN0aW9uIChDb25uZWN0aW9uU3RhdGUpIHtcbiAgICBDb25uZWN0aW9uU3RhdGVbQ29ubmVjdGlvblN0YXRlW1wiTmV3XCJdID0gMV0gPSBcIk5ld1wiO1xuICAgIENvbm5lY3Rpb25TdGF0ZVtDb25uZWN0aW9uU3RhdGVbXCJMaXN0ZW5pbmdcIl0gPSAyXSA9IFwiTGlzdGVuaW5nXCI7XG4gICAgQ29ubmVjdGlvblN0YXRlW0Nvbm5lY3Rpb25TdGF0ZVtcIkNsb3NlZFwiXSA9IDNdID0gXCJDbG9zZWRcIjtcbiAgICBDb25uZWN0aW9uU3RhdGVbQ29ubmVjdGlvblN0YXRlW1wiRGlzcG9zZWRcIl0gPSA0XSA9IFwiRGlzcG9zZWRcIjtcbn0pKENvbm5lY3Rpb25TdGF0ZSB8fCAoQ29ubmVjdGlvblN0YXRlID0ge30pKTtcbmZ1bmN0aW9uIGNyZWF0ZU1lc3NhZ2VDb25uZWN0aW9uKG1lc3NhZ2VSZWFkZXIsIG1lc3NhZ2VXcml0ZXIsIF9sb2dnZXIsIG9wdGlvbnMpIHtcbiAgICBjb25zdCBsb2dnZXIgPSBfbG9nZ2VyICE9PSB1bmRlZmluZWQgPyBfbG9nZ2VyIDogZXhwb3J0cy5OdWxsTG9nZ2VyO1xuICAgIGxldCBzZXF1ZW5jZU51bWJlciA9IDA7XG4gICAgbGV0IG5vdGlmaWNhdGlvblNlcXVlbmNlTnVtYmVyID0gMDtcbiAgICBsZXQgdW5rbm93blJlc3BvbnNlU2VxdWVuY2VOdW1iZXIgPSAwO1xuICAgIGNvbnN0IHZlcnNpb24gPSAnMi4wJztcbiAgICBsZXQgc3RhclJlcXVlc3RIYW5kbGVyID0gdW5kZWZpbmVkO1xuICAgIGNvbnN0IHJlcXVlc3RIYW5kbGVycyA9IG5ldyBNYXAoKTtcbiAgICBsZXQgc3Rhck5vdGlmaWNhdGlvbkhhbmRsZXIgPSB1bmRlZmluZWQ7XG4gICAgY29uc3Qgbm90aWZpY2F0aW9uSGFuZGxlcnMgPSBuZXcgTWFwKCk7XG4gICAgY29uc3QgcHJvZ3Jlc3NIYW5kbGVycyA9IG5ldyBNYXAoKTtcbiAgICBsZXQgdGltZXI7XG4gICAgbGV0IG1lc3NhZ2VRdWV1ZSA9IG5ldyBsaW5rZWRNYXBfMS5MaW5rZWRNYXAoKTtcbiAgICBsZXQgcmVzcG9uc2VQcm9taXNlcyA9IG5ldyBNYXAoKTtcbiAgICBsZXQga25vd25DYW5jZWxlZFJlcXVlc3RzID0gbmV3IFNldCgpO1xuICAgIGxldCByZXF1ZXN0VG9rZW5zID0gbmV3IE1hcCgpO1xuICAgIGxldCB0cmFjZSA9IFRyYWNlLk9mZjtcbiAgICBsZXQgdHJhY2VGb3JtYXQgPSBUcmFjZUZvcm1hdC5UZXh0O1xuICAgIGxldCB0cmFjZXI7XG4gICAgbGV0IHN0YXRlID0gQ29ubmVjdGlvblN0YXRlLk5ldztcbiAgICBjb25zdCBlcnJvckVtaXR0ZXIgPSBuZXcgZXZlbnRzXzEuRW1pdHRlcigpO1xuICAgIGNvbnN0IGNsb3NlRW1pdHRlciA9IG5ldyBldmVudHNfMS5FbWl0dGVyKCk7XG4gICAgY29uc3QgdW5oYW5kbGVkTm90aWZpY2F0aW9uRW1pdHRlciA9IG5ldyBldmVudHNfMS5FbWl0dGVyKCk7XG4gICAgY29uc3QgdW5oYW5kbGVkUHJvZ3Jlc3NFbWl0dGVyID0gbmV3IGV2ZW50c18xLkVtaXR0ZXIoKTtcbiAgICBjb25zdCBkaXNwb3NlRW1pdHRlciA9IG5ldyBldmVudHNfMS5FbWl0dGVyKCk7XG4gICAgY29uc3QgY2FuY2VsbGF0aW9uU3RyYXRlZ3kgPSAob3B0aW9ucyAmJiBvcHRpb25zLmNhbmNlbGxhdGlvblN0cmF0ZWd5KSA/IG9wdGlvbnMuY2FuY2VsbGF0aW9uU3RyYXRlZ3kgOiBDYW5jZWxsYXRpb25TdHJhdGVneS5NZXNzYWdlO1xuICAgIGZ1bmN0aW9uIGNyZWF0ZVJlcXVlc3RRdWV1ZUtleShpZCkge1xuICAgICAgICBpZiAoaWQgPT09IG51bGwpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgQ2FuJ3Qgc2VuZCByZXF1ZXN0cyB3aXRoIGlkIG51bGwgc2luY2UgdGhlIHJlc3BvbnNlIGNhbid0IGJlIGNvcnJlbGF0ZWQuYCk7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuICdyZXEtJyArIGlkLnRvU3RyaW5nKCk7XG4gICAgfVxuICAgIGZ1bmN0aW9uIGNyZWF0ZVJlc3BvbnNlUXVldWVLZXkoaWQpIHtcbiAgICAgICAgaWYgKGlkID09PSBudWxsKSB7XG4gICAgICAgICAgICByZXR1cm4gJ3Jlcy11bmtub3duLScgKyAoKyt1bmtub3duUmVzcG9uc2VTZXF1ZW5jZU51bWJlcikudG9TdHJpbmcoKTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHJldHVybiAncmVzLScgKyBpZC50b1N0cmluZygpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIGNyZWF0ZU5vdGlmaWNhdGlvblF1ZXVlS2V5KCkge1xuICAgICAgICByZXR1cm4gJ25vdC0nICsgKCsrbm90aWZpY2F0aW9uU2VxdWVuY2VOdW1iZXIpLnRvU3RyaW5nKCk7XG4gICAgfVxuICAgIGZ1bmN0aW9uIGFkZE1lc3NhZ2VUb1F1ZXVlKHF1ZXVlLCBtZXNzYWdlKSB7XG4gICAgICAgIGlmIChtZXNzYWdlc18xLk1lc3NhZ2UuaXNSZXF1ZXN0KG1lc3NhZ2UpKSB7XG4gICAgICAgICAgICBxdWV1ZS5zZXQoY3JlYXRlUmVxdWVzdFF1ZXVlS2V5KG1lc3NhZ2UuaWQpLCBtZXNzYWdlKTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIGlmIChtZXNzYWdlc18xLk1lc3NhZ2UuaXNSZXNwb25zZShtZXNzYWdlKSkge1xuICAgICAgICAgICAgcXVldWUuc2V0KGNyZWF0ZVJlc3BvbnNlUXVldWVLZXkobWVzc2FnZS5pZCksIG1lc3NhZ2UpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgcXVldWUuc2V0KGNyZWF0ZU5vdGlmaWNhdGlvblF1ZXVlS2V5KCksIG1lc3NhZ2UpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIGNhbmNlbFVuZGlzcGF0Y2hlZChfbWVzc2FnZSkge1xuICAgICAgICByZXR1cm4gdW5kZWZpbmVkO1xuICAgIH1cbiAgICBmdW5jdGlvbiBpc0xpc3RlbmluZygpIHtcbiAgICAgICAgcmV0dXJuIHN0YXRlID09PSBDb25uZWN0aW9uU3RhdGUuTGlzdGVuaW5nO1xuICAgIH1cbiAgICBmdW5jdGlvbiBpc0Nsb3NlZCgpIHtcbiAgICAgICAgcmV0dXJuIHN0YXRlID09PSBDb25uZWN0aW9uU3RhdGUuQ2xvc2VkO1xuICAgIH1cbiAgICBmdW5jdGlvbiBpc0Rpc3Bvc2VkKCkge1xuICAgICAgICByZXR1cm4gc3RhdGUgPT09IENvbm5lY3Rpb25TdGF0ZS5EaXNwb3NlZDtcbiAgICB9XG4gICAgZnVuY3Rpb24gY2xvc2VIYW5kbGVyKCkge1xuICAgICAgICBpZiAoc3RhdGUgPT09IENvbm5lY3Rpb25TdGF0ZS5OZXcgfHwgc3RhdGUgPT09IENvbm5lY3Rpb25TdGF0ZS5MaXN0ZW5pbmcpIHtcbiAgICAgICAgICAgIHN0YXRlID0gQ29ubmVjdGlvblN0YXRlLkNsb3NlZDtcbiAgICAgICAgICAgIGNsb3NlRW1pdHRlci5maXJlKHVuZGVmaW5lZCk7XG4gICAgICAgIH1cbiAgICAgICAgLy8gSWYgdGhlIGNvbm5lY3Rpb24gaXMgZGlzcG9zZWQgZG9uJ3Qgc2VudCBjbG9zZSBldmVudHMuXG4gICAgfVxuICAgIGZ1bmN0aW9uIHJlYWRFcnJvckhhbmRsZXIoZXJyb3IpIHtcbiAgICAgICAgZXJyb3JFbWl0dGVyLmZpcmUoW2Vycm9yLCB1bmRlZmluZWQsIHVuZGVmaW5lZF0pO1xuICAgIH1cbiAgICBmdW5jdGlvbiB3cml0ZUVycm9ySGFuZGxlcihkYXRhKSB7XG4gICAgICAgIGVycm9yRW1pdHRlci5maXJlKGRhdGEpO1xuICAgIH1cbiAgICBtZXNzYWdlUmVhZGVyLm9uQ2xvc2UoY2xvc2VIYW5kbGVyKTtcbiAgICBtZXNzYWdlUmVhZGVyLm9uRXJyb3IocmVhZEVycm9ySGFuZGxlcik7XG4gICAgbWVzc2FnZVdyaXRlci5vbkNsb3NlKGNsb3NlSGFuZGxlcik7XG4gICAgbWVzc2FnZVdyaXRlci5vbkVycm9yKHdyaXRlRXJyb3JIYW5kbGVyKTtcbiAgICBmdW5jdGlvbiB0cmlnZ2VyTWVzc2FnZVF1ZXVlKCkge1xuICAgICAgICBpZiAodGltZXIgfHwgbWVzc2FnZVF1ZXVlLnNpemUgPT09IDApIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICB0aW1lciA9ICgwLCByYWxfMS5kZWZhdWx0KSgpLnRpbWVyLnNldEltbWVkaWF0ZSgoKSA9PiB7XG4gICAgICAgICAgICB0aW1lciA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIHByb2Nlc3NNZXNzYWdlUXVldWUoKTtcbiAgICAgICAgfSk7XG4gICAgfVxuICAgIGZ1bmN0aW9uIGhhbmRsZU1lc3NhZ2UobWVzc2FnZSkge1xuICAgICAgICBpZiAobWVzc2FnZXNfMS5NZXNzYWdlLmlzUmVxdWVzdChtZXNzYWdlKSkge1xuICAgICAgICAgICAgaGFuZGxlUmVxdWVzdChtZXNzYWdlKTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIGlmIChtZXNzYWdlc18xLk1lc3NhZ2UuaXNOb3RpZmljYXRpb24obWVzc2FnZSkpIHtcbiAgICAgICAgICAgIGhhbmRsZU5vdGlmaWNhdGlvbihtZXNzYWdlKTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIGlmIChtZXNzYWdlc18xLk1lc3NhZ2UuaXNSZXNwb25zZShtZXNzYWdlKSkge1xuICAgICAgICAgICAgaGFuZGxlUmVzcG9uc2UobWVzc2FnZSk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBoYW5kbGVJbnZhbGlkTWVzc2FnZShtZXNzYWdlKTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBmdW5jdGlvbiBwcm9jZXNzTWVzc2FnZVF1ZXVlKCkge1xuICAgICAgICBpZiAobWVzc2FnZVF1ZXVlLnNpemUgPT09IDApIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBjb25zdCBtZXNzYWdlID0gbWVzc2FnZVF1ZXVlLnNoaWZ0KCk7XG4gICAgICAgIHRyeSB7XG4gICAgICAgICAgICBjb25zdCBtZXNzYWdlU3RyYXRlZ3kgPSBvcHRpb25zPy5tZXNzYWdlU3RyYXRlZ3k7XG4gICAgICAgICAgICBpZiAoTWVzc2FnZVN0cmF0ZWd5LmlzKG1lc3NhZ2VTdHJhdGVneSkpIHtcbiAgICAgICAgICAgICAgICBtZXNzYWdlU3RyYXRlZ3kuaGFuZGxlTWVzc2FnZShtZXNzYWdlLCBoYW5kbGVNZXNzYWdlKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIGhhbmRsZU1lc3NhZ2UobWVzc2FnZSk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgZmluYWxseSB7XG4gICAgICAgICAgICB0cmlnZ2VyTWVzc2FnZVF1ZXVlKCk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgY29uc3QgY2FsbGJhY2sgPSAobWVzc2FnZSkgPT4ge1xuICAgICAgICB0cnkge1xuICAgICAgICAgICAgLy8gV2UgaGF2ZSByZWNlaXZlZCBhIGNhbmNlbGxhdGlvbiBtZXNzYWdlLiBDaGVjayBpZiB0aGUgbWVzc2FnZSBpcyBzdGlsbCBpbiB0aGUgcXVldWVcbiAgICAgICAgICAgIC8vIGFuZCBjYW5jZWwgaXQgaWYgYWxsb3dlZCB0byBkbyBzby5cbiAgICAgICAgICAgIGlmIChtZXNzYWdlc18xLk1lc3NhZ2UuaXNOb3RpZmljYXRpb24obWVzc2FnZSkgJiYgbWVzc2FnZS5tZXRob2QgPT09IENhbmNlbE5vdGlmaWNhdGlvbi50eXBlLm1ldGhvZCkge1xuICAgICAgICAgICAgICAgIGNvbnN0IGNhbmNlbElkID0gbWVzc2FnZS5wYXJhbXMuaWQ7XG4gICAgICAgICAgICAgICAgY29uc3Qga2V5ID0gY3JlYXRlUmVxdWVzdFF1ZXVlS2V5KGNhbmNlbElkKTtcbiAgICAgICAgICAgICAgICBjb25zdCB0b0NhbmNlbCA9IG1lc3NhZ2VRdWV1ZS5nZXQoa2V5KTtcbiAgICAgICAgICAgICAgICBpZiAobWVzc2FnZXNfMS5NZXNzYWdlLmlzUmVxdWVzdCh0b0NhbmNlbCkpIHtcbiAgICAgICAgICAgICAgICAgICAgY29uc3Qgc3RyYXRlZ3kgPSBvcHRpb25zPy5jb25uZWN0aW9uU3RyYXRlZ3k7XG4gICAgICAgICAgICAgICAgICAgIGNvbnN0IHJlc3BvbnNlID0gKHN0cmF0ZWd5ICYmIHN0cmF0ZWd5LmNhbmNlbFVuZGlzcGF0Y2hlZCkgPyBzdHJhdGVneS5jYW5jZWxVbmRpc3BhdGNoZWQodG9DYW5jZWwsIGNhbmNlbFVuZGlzcGF0Y2hlZCkgOiBjYW5jZWxVbmRpc3BhdGNoZWQodG9DYW5jZWwpO1xuICAgICAgICAgICAgICAgICAgICBpZiAocmVzcG9uc2UgJiYgKHJlc3BvbnNlLmVycm9yICE9PSB1bmRlZmluZWQgfHwgcmVzcG9uc2UucmVzdWx0ICE9PSB1bmRlZmluZWQpKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBtZXNzYWdlUXVldWUuZGVsZXRlKGtleSk7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXF1ZXN0VG9rZW5zLmRlbGV0ZShjYW5jZWxJZCk7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXNwb25zZS5pZCA9IHRvQ2FuY2VsLmlkO1xuICAgICAgICAgICAgICAgICAgICAgICAgdHJhY2VTZW5kaW5nUmVzcG9uc2UocmVzcG9uc2UsIG1lc3NhZ2UubWV0aG9kLCBEYXRlLm5vdygpKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIG1lc3NhZ2VXcml0ZXIud3JpdGUocmVzcG9uc2UpLmNhdGNoKCgpID0+IGxvZ2dlci5lcnJvcihgU2VuZGluZyByZXNwb25zZSBmb3IgY2FuY2VsZWQgbWVzc2FnZSBmYWlsZWQuYCkpO1xuICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGNvbnN0IGNhbmNlbGxhdGlvblRva2VuID0gcmVxdWVzdFRva2Vucy5nZXQoY2FuY2VsSWQpO1xuICAgICAgICAgICAgICAgIC8vIFRoZSByZXF1ZXN0IGlzIGFscmVhZHkgcnVubmluZy4gQ2FuY2VsIHRoZSB0b2tlblxuICAgICAgICAgICAgICAgIGlmIChjYW5jZWxsYXRpb25Ub2tlbiAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICAgICAgICAgIGNhbmNlbGxhdGlvblRva2VuLmNhbmNlbCgpO1xuICAgICAgICAgICAgICAgICAgICB0cmFjZVJlY2VpdmVkTm90aWZpY2F0aW9uKG1lc3NhZ2UpO1xuICAgICAgICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAvLyBSZW1lbWJlciB0aGUgY2FuY2VsIGJ1dCBzdGlsbCBxdWV1ZSB0aGUgbWVzc2FnZSB0b1xuICAgICAgICAgICAgICAgICAgICAvLyBjbGVhbiB1cCBzdGF0ZSBpbiBwcm9jZXNzIG1lc3NhZ2UuXG4gICAgICAgICAgICAgICAgICAgIGtub3duQ2FuY2VsZWRSZXF1ZXN0cy5hZGQoY2FuY2VsSWQpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGFkZE1lc3NhZ2VUb1F1ZXVlKG1lc3NhZ2VRdWV1ZSwgbWVzc2FnZSk7XG4gICAgICAgIH1cbiAgICAgICAgZmluYWxseSB7XG4gICAgICAgICAgICB0cmlnZ2VyTWVzc2FnZVF1ZXVlKCk7XG4gICAgICAgIH1cbiAgICB9O1xuICAgIGZ1bmN0aW9uIGhhbmRsZVJlcXVlc3QocmVxdWVzdE1lc3NhZ2UpIHtcbiAgICAgICAgaWYgKGlzRGlzcG9zZWQoKSkge1xuICAgICAgICAgICAgLy8gd2UgcmV0dXJuIGhlcmUgc2lsZW50bHkgc2luY2Ugd2UgZmlyZWQgYW4gZXZlbnQgd2hlbiB0aGVcbiAgICAgICAgICAgIC8vIGNvbm5lY3Rpb24gZ290IGRpc3Bvc2VkLlxuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGZ1bmN0aW9uIHJlcGx5KHJlc3VsdE9yRXJyb3IsIG1ldGhvZCwgc3RhcnRUaW1lKSB7XG4gICAgICAgICAgICBjb25zdCBtZXNzYWdlID0ge1xuICAgICAgICAgICAgICAgIGpzb25ycGM6IHZlcnNpb24sXG4gICAgICAgICAgICAgICAgaWQ6IHJlcXVlc3RNZXNzYWdlLmlkXG4gICAgICAgICAgICB9O1xuICAgICAgICAgICAgaWYgKHJlc3VsdE9yRXJyb3IgaW5zdGFuY2VvZiBtZXNzYWdlc18xLlJlc3BvbnNlRXJyb3IpIHtcbiAgICAgICAgICAgICAgICBtZXNzYWdlLmVycm9yID0gcmVzdWx0T3JFcnJvci50b0pzb24oKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIG1lc3NhZ2UucmVzdWx0ID0gcmVzdWx0T3JFcnJvciA9PT0gdW5kZWZpbmVkID8gbnVsbCA6IHJlc3VsdE9yRXJyb3I7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICB0cmFjZVNlbmRpbmdSZXNwb25zZShtZXNzYWdlLCBtZXRob2QsIHN0YXJ0VGltZSk7XG4gICAgICAgICAgICBtZXNzYWdlV3JpdGVyLndyaXRlKG1lc3NhZ2UpLmNhdGNoKCgpID0+IGxvZ2dlci5lcnJvcihgU2VuZGluZyByZXNwb25zZSBmYWlsZWQuYCkpO1xuICAgICAgICB9XG4gICAgICAgIGZ1bmN0aW9uIHJlcGx5RXJyb3IoZXJyb3IsIG1ldGhvZCwgc3RhcnRUaW1lKSB7XG4gICAgICAgICAgICBjb25zdCBtZXNzYWdlID0ge1xuICAgICAgICAgICAgICAgIGpzb25ycGM6IHZlcnNpb24sXG4gICAgICAgICAgICAgICAgaWQ6IHJlcXVlc3RNZXNzYWdlLmlkLFxuICAgICAgICAgICAgICAgIGVycm9yOiBlcnJvci50b0pzb24oKVxuICAgICAgICAgICAgfTtcbiAgICAgICAgICAgIHRyYWNlU2VuZGluZ1Jlc3BvbnNlKG1lc3NhZ2UsIG1ldGhvZCwgc3RhcnRUaW1lKTtcbiAgICAgICAgICAgIG1lc3NhZ2VXcml0ZXIud3JpdGUobWVzc2FnZSkuY2F0Y2goKCkgPT4gbG9nZ2VyLmVycm9yKGBTZW5kaW5nIHJlc3BvbnNlIGZhaWxlZC5gKSk7XG4gICAgICAgIH1cbiAgICAgICAgZnVuY3Rpb24gcmVwbHlTdWNjZXNzKHJlc3VsdCwgbWV0aG9kLCBzdGFydFRpbWUpIHtcbiAgICAgICAgICAgIC8vIFRoZSBKU09OIFJQQyBkZWZpbmVzIHRoYXQgYSByZXNwb25zZSBtdXN0IGVpdGhlciBoYXZlIGEgcmVzdWx0IG9yIGFuIGVycm9yXG4gICAgICAgICAgICAvLyBTbyB3ZSBjYW4ndCB0cmVhdCB1bmRlZmluZWQgYXMgYSB2YWxpZCByZXNwb25zZSByZXN1bHQuXG4gICAgICAgICAgICBpZiAocmVzdWx0ID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICByZXN1bHQgPSBudWxsO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgY29uc3QgbWVzc2FnZSA9IHtcbiAgICAgICAgICAgICAgICBqc29ucnBjOiB2ZXJzaW9uLFxuICAgICAgICAgICAgICAgIGlkOiByZXF1ZXN0TWVzc2FnZS5pZCxcbiAgICAgICAgICAgICAgICByZXN1bHQ6IHJlc3VsdFxuICAgICAgICAgICAgfTtcbiAgICAgICAgICAgIHRyYWNlU2VuZGluZ1Jlc3BvbnNlKG1lc3NhZ2UsIG1ldGhvZCwgc3RhcnRUaW1lKTtcbiAgICAgICAgICAgIG1lc3NhZ2VXcml0ZXIud3JpdGUobWVzc2FnZSkuY2F0Y2goKCkgPT4gbG9nZ2VyLmVycm9yKGBTZW5kaW5nIHJlc3BvbnNlIGZhaWxlZC5gKSk7XG4gICAgICAgIH1cbiAgICAgICAgdHJhY2VSZWNlaXZlZFJlcXVlc3QocmVxdWVzdE1lc3NhZ2UpO1xuICAgICAgICBjb25zdCBlbGVtZW50ID0gcmVxdWVzdEhhbmRsZXJzLmdldChyZXF1ZXN0TWVzc2FnZS5tZXRob2QpO1xuICAgICAgICBsZXQgdHlwZTtcbiAgICAgICAgbGV0IHJlcXVlc3RIYW5kbGVyO1xuICAgICAgICBpZiAoZWxlbWVudCkge1xuICAgICAgICAgICAgdHlwZSA9IGVsZW1lbnQudHlwZTtcbiAgICAgICAgICAgIHJlcXVlc3RIYW5kbGVyID0gZWxlbWVudC5oYW5kbGVyO1xuICAgICAgICB9XG4gICAgICAgIGNvbnN0IHN0YXJ0VGltZSA9IERhdGUubm93KCk7XG4gICAgICAgIGlmIChyZXF1ZXN0SGFuZGxlciB8fCBzdGFyUmVxdWVzdEhhbmRsZXIpIHtcbiAgICAgICAgICAgIGNvbnN0IHRva2VuS2V5ID0gcmVxdWVzdE1lc3NhZ2UuaWQgPz8gU3RyaW5nKERhdGUubm93KCkpOyAvL1xuICAgICAgICAgICAgY29uc3QgY2FuY2VsbGF0aW9uU291cmNlID0gSWRDYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5LmlzKGNhbmNlbGxhdGlvblN0cmF0ZWd5LnJlY2VpdmVyKVxuICAgICAgICAgICAgICAgID8gY2FuY2VsbGF0aW9uU3RyYXRlZ3kucmVjZWl2ZXIuY3JlYXRlQ2FuY2VsbGF0aW9uVG9rZW5Tb3VyY2UodG9rZW5LZXkpXG4gICAgICAgICAgICAgICAgOiBjYW5jZWxsYXRpb25TdHJhdGVneS5yZWNlaXZlci5jcmVhdGVDYW5jZWxsYXRpb25Ub2tlblNvdXJjZShyZXF1ZXN0TWVzc2FnZSk7XG4gICAgICAgICAgICBpZiAocmVxdWVzdE1lc3NhZ2UuaWQgIT09IG51bGwgJiYga25vd25DYW5jZWxlZFJlcXVlc3RzLmhhcyhyZXF1ZXN0TWVzc2FnZS5pZCkpIHtcbiAgICAgICAgICAgICAgICBjYW5jZWxsYXRpb25Tb3VyY2UuY2FuY2VsKCk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAocmVxdWVzdE1lc3NhZ2UuaWQgIT09IG51bGwpIHtcbiAgICAgICAgICAgICAgICByZXF1ZXN0VG9rZW5zLnNldCh0b2tlbktleSwgY2FuY2VsbGF0aW9uU291cmNlKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHRyeSB7XG4gICAgICAgICAgICAgICAgbGV0IGhhbmRsZXJSZXN1bHQ7XG4gICAgICAgICAgICAgICAgaWYgKHJlcXVlc3RIYW5kbGVyKSB7XG4gICAgICAgICAgICAgICAgICAgIGlmIChyZXF1ZXN0TWVzc2FnZS5wYXJhbXMgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKHR5cGUgIT09IHVuZGVmaW5lZCAmJiB0eXBlLm51bWJlck9mUGFyYW1zICE9PSAwKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgcmVwbHlFcnJvcihuZXcgbWVzc2FnZXNfMS5SZXNwb25zZUVycm9yKG1lc3NhZ2VzXzEuRXJyb3JDb2Rlcy5JbnZhbGlkUGFyYW1zLCBgUmVxdWVzdCAke3JlcXVlc3RNZXNzYWdlLm1ldGhvZH0gZGVmaW5lcyAke3R5cGUubnVtYmVyT2ZQYXJhbXN9IHBhcmFtcyBidXQgcmVjZWl2ZWQgbm9uZS5gKSwgcmVxdWVzdE1lc3NhZ2UubWV0aG9kLCBzdGFydFRpbWUpO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIGhhbmRsZXJSZXN1bHQgPSByZXF1ZXN0SGFuZGxlcihjYW5jZWxsYXRpb25Tb3VyY2UudG9rZW4pO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2UgaWYgKEFycmF5LmlzQXJyYXkocmVxdWVzdE1lc3NhZ2UucGFyYW1zKSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKHR5cGUgIT09IHVuZGVmaW5lZCAmJiB0eXBlLnBhcmFtZXRlclN0cnVjdHVyZXMgPT09IG1lc3NhZ2VzXzEuUGFyYW1ldGVyU3RydWN0dXJlcy5ieU5hbWUpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICByZXBseUVycm9yKG5ldyBtZXNzYWdlc18xLlJlc3BvbnNlRXJyb3IobWVzc2FnZXNfMS5FcnJvckNvZGVzLkludmFsaWRQYXJhbXMsIGBSZXF1ZXN0ICR7cmVxdWVzdE1lc3NhZ2UubWV0aG9kfSBkZWZpbmVzIHBhcmFtZXRlcnMgYnkgbmFtZSBidXQgcmVjZWl2ZWQgcGFyYW1ldGVycyBieSBwb3NpdGlvbmApLCByZXF1ZXN0TWVzc2FnZS5tZXRob2QsIHN0YXJ0VGltZSk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgaGFuZGxlclJlc3VsdCA9IHJlcXVlc3RIYW5kbGVyKC4uLnJlcXVlc3RNZXNzYWdlLnBhcmFtcywgY2FuY2VsbGF0aW9uU291cmNlLnRva2VuKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICh0eXBlICE9PSB1bmRlZmluZWQgJiYgdHlwZS5wYXJhbWV0ZXJTdHJ1Y3R1cmVzID09PSBtZXNzYWdlc18xLlBhcmFtZXRlclN0cnVjdHVyZXMuYnlQb3NpdGlvbikge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJlcGx5RXJyb3IobmV3IG1lc3NhZ2VzXzEuUmVzcG9uc2VFcnJvcihtZXNzYWdlc18xLkVycm9yQ29kZXMuSW52YWxpZFBhcmFtcywgYFJlcXVlc3QgJHtyZXF1ZXN0TWVzc2FnZS5tZXRob2R9IGRlZmluZXMgcGFyYW1ldGVycyBieSBwb3NpdGlvbiBidXQgcmVjZWl2ZWQgcGFyYW1ldGVycyBieSBuYW1lYCksIHJlcXVlc3RNZXNzYWdlLm1ldGhvZCwgc3RhcnRUaW1lKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBoYW5kbGVyUmVzdWx0ID0gcmVxdWVzdEhhbmRsZXIocmVxdWVzdE1lc3NhZ2UucGFyYW1zLCBjYW5jZWxsYXRpb25Tb3VyY2UudG9rZW4pO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2UgaWYgKHN0YXJSZXF1ZXN0SGFuZGxlcikge1xuICAgICAgICAgICAgICAgICAgICBoYW5kbGVyUmVzdWx0ID0gc3RhclJlcXVlc3RIYW5kbGVyKHJlcXVlc3RNZXNzYWdlLm1ldGhvZCwgcmVxdWVzdE1lc3NhZ2UucGFyYW1zLCBjYW5jZWxsYXRpb25Tb3VyY2UudG9rZW4pO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBjb25zdCBwcm9taXNlID0gaGFuZGxlclJlc3VsdDtcbiAgICAgICAgICAgICAgICBpZiAoIWhhbmRsZXJSZXN1bHQpIHtcbiAgICAgICAgICAgICAgICAgICAgcmVxdWVzdFRva2Vucy5kZWxldGUodG9rZW5LZXkpO1xuICAgICAgICAgICAgICAgICAgICByZXBseVN1Y2Nlc3MoaGFuZGxlclJlc3VsdCwgcmVxdWVzdE1lc3NhZ2UubWV0aG9kLCBzdGFydFRpbWUpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIGlmIChwcm9taXNlLnRoZW4pIHtcbiAgICAgICAgICAgICAgICAgICAgcHJvbWlzZS50aGVuKChyZXN1bHRPckVycm9yKSA9PiB7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXF1ZXN0VG9rZW5zLmRlbGV0ZSh0b2tlbktleSk7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXBseShyZXN1bHRPckVycm9yLCByZXF1ZXN0TWVzc2FnZS5tZXRob2QsIHN0YXJ0VGltZSk7XG4gICAgICAgICAgICAgICAgICAgIH0sIGVycm9yID0+IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJlcXVlc3RUb2tlbnMuZGVsZXRlKHRva2VuS2V5KTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmIChlcnJvciBpbnN0YW5jZW9mIG1lc3NhZ2VzXzEuUmVzcG9uc2VFcnJvcikge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJlcGx5RXJyb3IoZXJyb3IsIHJlcXVlc3RNZXNzYWdlLm1ldGhvZCwgc3RhcnRUaW1lKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIGVsc2UgaWYgKGVycm9yICYmIElzLnN0cmluZyhlcnJvci5tZXNzYWdlKSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJlcGx5RXJyb3IobmV3IG1lc3NhZ2VzXzEuUmVzcG9uc2VFcnJvcihtZXNzYWdlc18xLkVycm9yQ29kZXMuSW50ZXJuYWxFcnJvciwgYFJlcXVlc3QgJHtyZXF1ZXN0TWVzc2FnZS5tZXRob2R9IGZhaWxlZCB3aXRoIG1lc3NhZ2U6ICR7ZXJyb3IubWVzc2FnZX1gKSwgcmVxdWVzdE1lc3NhZ2UubWV0aG9kLCBzdGFydFRpbWUpO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgcmVwbHlFcnJvcihuZXcgbWVzc2FnZXNfMS5SZXNwb25zZUVycm9yKG1lc3NhZ2VzXzEuRXJyb3JDb2Rlcy5JbnRlcm5hbEVycm9yLCBgUmVxdWVzdCAke3JlcXVlc3RNZXNzYWdlLm1ldGhvZH0gZmFpbGVkIHVuZXhwZWN0ZWRseSB3aXRob3V0IHByb3ZpZGluZyBhbnkgZGV0YWlscy5gKSwgcmVxdWVzdE1lc3NhZ2UubWV0aG9kLCBzdGFydFRpbWUpO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB9KTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIHJlcXVlc3RUb2tlbnMuZGVsZXRlKHRva2VuS2V5KTtcbiAgICAgICAgICAgICAgICAgICAgcmVwbHkoaGFuZGxlclJlc3VsdCwgcmVxdWVzdE1lc3NhZ2UubWV0aG9kLCBzdGFydFRpbWUpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNhdGNoIChlcnJvcikge1xuICAgICAgICAgICAgICAgIHJlcXVlc3RUb2tlbnMuZGVsZXRlKHRva2VuS2V5KTtcbiAgICAgICAgICAgICAgICBpZiAoZXJyb3IgaW5zdGFuY2VvZiBtZXNzYWdlc18xLlJlc3BvbnNlRXJyb3IpIHtcbiAgICAgICAgICAgICAgICAgICAgcmVwbHkoZXJyb3IsIHJlcXVlc3RNZXNzYWdlLm1ldGhvZCwgc3RhcnRUaW1lKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSBpZiAoZXJyb3IgJiYgSXMuc3RyaW5nKGVycm9yLm1lc3NhZ2UpKSB7XG4gICAgICAgICAgICAgICAgICAgIHJlcGx5RXJyb3IobmV3IG1lc3NhZ2VzXzEuUmVzcG9uc2VFcnJvcihtZXNzYWdlc18xLkVycm9yQ29kZXMuSW50ZXJuYWxFcnJvciwgYFJlcXVlc3QgJHtyZXF1ZXN0TWVzc2FnZS5tZXRob2R9IGZhaWxlZCB3aXRoIG1lc3NhZ2U6ICR7ZXJyb3IubWVzc2FnZX1gKSwgcmVxdWVzdE1lc3NhZ2UubWV0aG9kLCBzdGFydFRpbWUpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgcmVwbHlFcnJvcihuZXcgbWVzc2FnZXNfMS5SZXNwb25zZUVycm9yKG1lc3NhZ2VzXzEuRXJyb3JDb2Rlcy5JbnRlcm5hbEVycm9yLCBgUmVxdWVzdCAke3JlcXVlc3RNZXNzYWdlLm1ldGhvZH0gZmFpbGVkIHVuZXhwZWN0ZWRseSB3aXRob3V0IHByb3ZpZGluZyBhbnkgZGV0YWlscy5gKSwgcmVxdWVzdE1lc3NhZ2UubWV0aG9kLCBzdGFydFRpbWUpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHJlcGx5RXJyb3IobmV3IG1lc3NhZ2VzXzEuUmVzcG9uc2VFcnJvcihtZXNzYWdlc18xLkVycm9yQ29kZXMuTWV0aG9kTm90Rm91bmQsIGBVbmhhbmRsZWQgbWV0aG9kICR7cmVxdWVzdE1lc3NhZ2UubWV0aG9kfWApLCByZXF1ZXN0TWVzc2FnZS5tZXRob2QsIHN0YXJ0VGltZSk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgZnVuY3Rpb24gaGFuZGxlUmVzcG9uc2UocmVzcG9uc2VNZXNzYWdlKSB7XG4gICAgICAgIGlmIChpc0Rpc3Bvc2VkKCkpIHtcbiAgICAgICAgICAgIC8vIFNlZSBoYW5kbGUgcmVxdWVzdC5cbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBpZiAocmVzcG9uc2VNZXNzYWdlLmlkID09PSBudWxsKSB7XG4gICAgICAgICAgICBpZiAocmVzcG9uc2VNZXNzYWdlLmVycm9yKSB7XG4gICAgICAgICAgICAgICAgbG9nZ2VyLmVycm9yKGBSZWNlaXZlZCByZXNwb25zZSBtZXNzYWdlIHdpdGhvdXQgaWQ6IEVycm9yIGlzOiBcXG4ke0pTT04uc3RyaW5naWZ5KHJlc3BvbnNlTWVzc2FnZS5lcnJvciwgdW5kZWZpbmVkLCA0KX1gKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIGxvZ2dlci5lcnJvcihgUmVjZWl2ZWQgcmVzcG9uc2UgbWVzc2FnZSB3aXRob3V0IGlkLiBObyBmdXJ0aGVyIGVycm9yIGluZm9ybWF0aW9uIHByb3ZpZGVkLmApO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgY29uc3Qga2V5ID0gcmVzcG9uc2VNZXNzYWdlLmlkO1xuICAgICAgICAgICAgY29uc3QgcmVzcG9uc2VQcm9taXNlID0gcmVzcG9uc2VQcm9taXNlcy5nZXQoa2V5KTtcbiAgICAgICAgICAgIHRyYWNlUmVjZWl2ZWRSZXNwb25zZShyZXNwb25zZU1lc3NhZ2UsIHJlc3BvbnNlUHJvbWlzZSk7XG4gICAgICAgICAgICBpZiAocmVzcG9uc2VQcm9taXNlICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICByZXNwb25zZVByb21pc2VzLmRlbGV0ZShrZXkpO1xuICAgICAgICAgICAgICAgIHRyeSB7XG4gICAgICAgICAgICAgICAgICAgIGlmIChyZXNwb25zZU1lc3NhZ2UuZXJyb3IpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGNvbnN0IGVycm9yID0gcmVzcG9uc2VNZXNzYWdlLmVycm9yO1xuICAgICAgICAgICAgICAgICAgICAgICAgcmVzcG9uc2VQcm9taXNlLnJlamVjdChuZXcgbWVzc2FnZXNfMS5SZXNwb25zZUVycm9yKGVycm9yLmNvZGUsIGVycm9yLm1lc3NhZ2UsIGVycm9yLmRhdGEpKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBlbHNlIGlmIChyZXNwb25zZU1lc3NhZ2UucmVzdWx0ICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJlc3BvbnNlUHJvbWlzZS5yZXNvbHZlKHJlc3BvbnNlTWVzc2FnZS5yZXN1bHQpO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdTaG91bGQgbmV2ZXIgaGFwcGVuLicpO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGNhdGNoIChlcnJvcikge1xuICAgICAgICAgICAgICAgICAgICBpZiAoZXJyb3IubWVzc2FnZSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgbG9nZ2VyLmVycm9yKGBSZXNwb25zZSBoYW5kbGVyICcke3Jlc3BvbnNlUHJvbWlzZS5tZXRob2R9JyBmYWlsZWQgd2l0aCBtZXNzYWdlOiAke2Vycm9yLm1lc3NhZ2V9YCk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBsb2dnZXIuZXJyb3IoYFJlc3BvbnNlIGhhbmRsZXIgJyR7cmVzcG9uc2VQcm9taXNlLm1ldGhvZH0nIGZhaWxlZCB1bmV4cGVjdGVkbHkuYCk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICB9XG4gICAgZnVuY3Rpb24gaGFuZGxlTm90aWZpY2F0aW9uKG1lc3NhZ2UpIHtcbiAgICAgICAgaWYgKGlzRGlzcG9zZWQoKSkge1xuICAgICAgICAgICAgLy8gU2VlIGhhbmRsZSByZXF1ZXN0LlxuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGxldCB0eXBlID0gdW5kZWZpbmVkO1xuICAgICAgICBsZXQgbm90aWZpY2F0aW9uSGFuZGxlcjtcbiAgICAgICAgaWYgKG1lc3NhZ2UubWV0aG9kID09PSBDYW5jZWxOb3RpZmljYXRpb24udHlwZS5tZXRob2QpIHtcbiAgICAgICAgICAgIGNvbnN0IGNhbmNlbElkID0gbWVzc2FnZS5wYXJhbXMuaWQ7XG4gICAgICAgICAgICBrbm93bkNhbmNlbGVkUmVxdWVzdHMuZGVsZXRlKGNhbmNlbElkKTtcbiAgICAgICAgICAgIHRyYWNlUmVjZWl2ZWROb3RpZmljYXRpb24obWVzc2FnZSk7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBjb25zdCBlbGVtZW50ID0gbm90aWZpY2F0aW9uSGFuZGxlcnMuZ2V0KG1lc3NhZ2UubWV0aG9kKTtcbiAgICAgICAgICAgIGlmIChlbGVtZW50KSB7XG4gICAgICAgICAgICAgICAgbm90aWZpY2F0aW9uSGFuZGxlciA9IGVsZW1lbnQuaGFuZGxlcjtcbiAgICAgICAgICAgICAgICB0eXBlID0gZWxlbWVudC50eXBlO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIGlmIChub3RpZmljYXRpb25IYW5kbGVyIHx8IHN0YXJOb3RpZmljYXRpb25IYW5kbGVyKSB7XG4gICAgICAgICAgICB0cnkge1xuICAgICAgICAgICAgICAgIHRyYWNlUmVjZWl2ZWROb3RpZmljYXRpb24obWVzc2FnZSk7XG4gICAgICAgICAgICAgICAgaWYgKG5vdGlmaWNhdGlvbkhhbmRsZXIpIHtcbiAgICAgICAgICAgICAgICAgICAgaWYgKG1lc3NhZ2UucGFyYW1zID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICh0eXBlICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBpZiAodHlwZS5udW1iZXJPZlBhcmFtcyAhPT0gMCAmJiB0eXBlLnBhcmFtZXRlclN0cnVjdHVyZXMgIT09IG1lc3NhZ2VzXzEuUGFyYW1ldGVyU3RydWN0dXJlcy5ieU5hbWUpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbG9nZ2VyLmVycm9yKGBOb3RpZmljYXRpb24gJHttZXNzYWdlLm1ldGhvZH0gZGVmaW5lcyAke3R5cGUubnVtYmVyT2ZQYXJhbXN9IHBhcmFtcyBidXQgcmVjZWl2ZWQgbm9uZS5gKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBub3RpZmljYXRpb25IYW5kbGVyKCk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZWxzZSBpZiAoQXJyYXkuaXNBcnJheShtZXNzYWdlLnBhcmFtcykpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIC8vIFRoZXJlIGFyZSBKU09OLVJQQyBsaWJyYXJpZXMgdGhhdCBzZW5kIHByb2dyZXNzIG1lc3NhZ2UgYXMgcG9zaXRpb25hbCBwYXJhbXMgYWx0aG91Z2hcbiAgICAgICAgICAgICAgICAgICAgICAgIC8vIHNwZWNpZmllZCBhcyBuYW1lZC4gU28gY29udmVydCB0aGVtIGlmIHRoaXMgaXMgdGhlIGNhc2UuXG4gICAgICAgICAgICAgICAgICAgICAgICBjb25zdCBwYXJhbXMgPSBtZXNzYWdlLnBhcmFtcztcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmIChtZXNzYWdlLm1ldGhvZCA9PT0gUHJvZ3Jlc3NOb3RpZmljYXRpb24udHlwZS5tZXRob2QgJiYgcGFyYW1zLmxlbmd0aCA9PT0gMiAmJiBQcm9ncmVzc1Rva2VuLmlzKHBhcmFtc1swXSkpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBub3RpZmljYXRpb25IYW5kbGVyKHsgdG9rZW46IHBhcmFtc1swXSwgdmFsdWU6IHBhcmFtc1sxXSB9KTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGlmICh0eXBlICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaWYgKHR5cGUucGFyYW1ldGVyU3RydWN0dXJlcyA9PT0gbWVzc2FnZXNfMS5QYXJhbWV0ZXJTdHJ1Y3R1cmVzLmJ5TmFtZSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbG9nZ2VyLmVycm9yKGBOb3RpZmljYXRpb24gJHttZXNzYWdlLm1ldGhvZH0gZGVmaW5lcyBwYXJhbWV0ZXJzIGJ5IG5hbWUgYnV0IHJlY2VpdmVkIHBhcmFtZXRlcnMgYnkgcG9zaXRpb25gKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBpZiAodHlwZS5udW1iZXJPZlBhcmFtcyAhPT0gbWVzc2FnZS5wYXJhbXMubGVuZ3RoKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBsb2dnZXIuZXJyb3IoYE5vdGlmaWNhdGlvbiAke21lc3NhZ2UubWV0aG9kfSBkZWZpbmVzICR7dHlwZS5udW1iZXJPZlBhcmFtc30gcGFyYW1zIGJ1dCByZWNlaXZlZCAke3BhcmFtcy5sZW5ndGh9IGFyZ3VtZW50c2ApO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5vdGlmaWNhdGlvbkhhbmRsZXIoLi4ucGFyYW1zKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICh0eXBlICE9PSB1bmRlZmluZWQgJiYgdHlwZS5wYXJhbWV0ZXJTdHJ1Y3R1cmVzID09PSBtZXNzYWdlc18xLlBhcmFtZXRlclN0cnVjdHVyZXMuYnlQb3NpdGlvbikge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGxvZ2dlci5lcnJvcihgTm90aWZpY2F0aW9uICR7bWVzc2FnZS5tZXRob2R9IGRlZmluZXMgcGFyYW1ldGVycyBieSBwb3NpdGlvbiBidXQgcmVjZWl2ZWQgcGFyYW1ldGVycyBieSBuYW1lYCk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBub3RpZmljYXRpb25IYW5kbGVyKG1lc3NhZ2UucGFyYW1zKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIGlmIChzdGFyTm90aWZpY2F0aW9uSGFuZGxlcikge1xuICAgICAgICAgICAgICAgICAgICBzdGFyTm90aWZpY2F0aW9uSGFuZGxlcihtZXNzYWdlLm1ldGhvZCwgbWVzc2FnZS5wYXJhbXMpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNhdGNoIChlcnJvcikge1xuICAgICAgICAgICAgICAgIGlmIChlcnJvci5tZXNzYWdlKSB7XG4gICAgICAgICAgICAgICAgICAgIGxvZ2dlci5lcnJvcihgTm90aWZpY2F0aW9uIGhhbmRsZXIgJyR7bWVzc2FnZS5tZXRob2R9JyBmYWlsZWQgd2l0aCBtZXNzYWdlOiAke2Vycm9yLm1lc3NhZ2V9YCk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICBsb2dnZXIuZXJyb3IoYE5vdGlmaWNhdGlvbiBoYW5kbGVyICcke21lc3NhZ2UubWV0aG9kfScgZmFpbGVkIHVuZXhwZWN0ZWRseS5gKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICB1bmhhbmRsZWROb3RpZmljYXRpb25FbWl0dGVyLmZpcmUobWVzc2FnZSk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgZnVuY3Rpb24gaGFuZGxlSW52YWxpZE1lc3NhZ2UobWVzc2FnZSkge1xuICAgICAgICBpZiAoIW1lc3NhZ2UpIHtcbiAgICAgICAgICAgIGxvZ2dlci5lcnJvcignUmVjZWl2ZWQgZW1wdHkgbWVzc2FnZS4nKTtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBsb2dnZXIuZXJyb3IoYFJlY2VpdmVkIG1lc3NhZ2Ugd2hpY2ggaXMgbmVpdGhlciBhIHJlc3BvbnNlIG5vciBhIG5vdGlmaWNhdGlvbiBtZXNzYWdlOlxcbiR7SlNPTi5zdHJpbmdpZnkobWVzc2FnZSwgbnVsbCwgNCl9YCk7XG4gICAgICAgIC8vIFRlc3Qgd2hldGhlciB3ZSBmaW5kIGFuIGlkIHRvIHJlamVjdCB0aGUgcHJvbWlzZVxuICAgICAgICBjb25zdCByZXNwb25zZU1lc3NhZ2UgPSBtZXNzYWdlO1xuICAgICAgICBpZiAoSXMuc3RyaW5nKHJlc3BvbnNlTWVzc2FnZS5pZCkgfHwgSXMubnVtYmVyKHJlc3BvbnNlTWVzc2FnZS5pZCkpIHtcbiAgICAgICAgICAgIGNvbnN0IGtleSA9IHJlc3BvbnNlTWVzc2FnZS5pZDtcbiAgICAgICAgICAgIGNvbnN0IHJlc3BvbnNlSGFuZGxlciA9IHJlc3BvbnNlUHJvbWlzZXMuZ2V0KGtleSk7XG4gICAgICAgICAgICBpZiAocmVzcG9uc2VIYW5kbGVyKSB7XG4gICAgICAgICAgICAgICAgcmVzcG9uc2VIYW5kbGVyLnJlamVjdChuZXcgRXJyb3IoJ1RoZSByZWNlaXZlZCByZXNwb25zZSBoYXMgbmVpdGhlciBhIHJlc3VsdCBub3IgYW4gZXJyb3IgcHJvcGVydHkuJykpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIHN0cmluZ2lmeVRyYWNlKHBhcmFtcykge1xuICAgICAgICBpZiAocGFyYW1zID09PSB1bmRlZmluZWQgfHwgcGFyYW1zID09PSBudWxsKSB7XG4gICAgICAgICAgICByZXR1cm4gdW5kZWZpbmVkO1xuICAgICAgICB9XG4gICAgICAgIHN3aXRjaCAodHJhY2UpIHtcbiAgICAgICAgICAgIGNhc2UgVHJhY2UuVmVyYm9zZTpcbiAgICAgICAgICAgICAgICByZXR1cm4gSlNPTi5zdHJpbmdpZnkocGFyYW1zLCBudWxsLCA0KTtcbiAgICAgICAgICAgIGNhc2UgVHJhY2UuQ29tcGFjdDpcbiAgICAgICAgICAgICAgICByZXR1cm4gSlNPTi5zdHJpbmdpZnkocGFyYW1zKTtcbiAgICAgICAgICAgIGRlZmF1bHQ6XG4gICAgICAgICAgICAgICAgcmV0dXJuIHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgIH1cbiAgICBmdW5jdGlvbiB0cmFjZVNlbmRpbmdSZXF1ZXN0KG1lc3NhZ2UpIHtcbiAgICAgICAgaWYgKHRyYWNlID09PSBUcmFjZS5PZmYgfHwgIXRyYWNlcikge1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGlmICh0cmFjZUZvcm1hdCA9PT0gVHJhY2VGb3JtYXQuVGV4dCkge1xuICAgICAgICAgICAgbGV0IGRhdGEgPSB1bmRlZmluZWQ7XG4gICAgICAgICAgICBpZiAoKHRyYWNlID09PSBUcmFjZS5WZXJib3NlIHx8IHRyYWNlID09PSBUcmFjZS5Db21wYWN0KSAmJiBtZXNzYWdlLnBhcmFtcykge1xuICAgICAgICAgICAgICAgIGRhdGEgPSBgUGFyYW1zOiAke3N0cmluZ2lmeVRyYWNlKG1lc3NhZ2UucGFyYW1zKX1cXG5cXG5gO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgdHJhY2VyLmxvZyhgU2VuZGluZyByZXF1ZXN0ICcke21lc3NhZ2UubWV0aG9kfSAtICgke21lc3NhZ2UuaWR9KScuYCwgZGF0YSk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBsb2dMU1BNZXNzYWdlKCdzZW5kLXJlcXVlc3QnLCBtZXNzYWdlKTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBmdW5jdGlvbiB0cmFjZVNlbmRpbmdOb3RpZmljYXRpb24obWVzc2FnZSkge1xuICAgICAgICBpZiAodHJhY2UgPT09IFRyYWNlLk9mZiB8fCAhdHJhY2VyKSB7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgICAgaWYgKHRyYWNlRm9ybWF0ID09PSBUcmFjZUZvcm1hdC5UZXh0KSB7XG4gICAgICAgICAgICBsZXQgZGF0YSA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIGlmICh0cmFjZSA9PT0gVHJhY2UuVmVyYm9zZSB8fCB0cmFjZSA9PT0gVHJhY2UuQ29tcGFjdCkge1xuICAgICAgICAgICAgICAgIGlmIChtZXNzYWdlLnBhcmFtcykge1xuICAgICAgICAgICAgICAgICAgICBkYXRhID0gYFBhcmFtczogJHtzdHJpbmdpZnlUcmFjZShtZXNzYWdlLnBhcmFtcyl9XFxuXFxuYDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIGRhdGEgPSAnTm8gcGFyYW1ldGVycyBwcm92aWRlZC5cXG5cXG4nO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHRyYWNlci5sb2coYFNlbmRpbmcgbm90aWZpY2F0aW9uICcke21lc3NhZ2UubWV0aG9kfScuYCwgZGF0YSk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBsb2dMU1BNZXNzYWdlKCdzZW5kLW5vdGlmaWNhdGlvbicsIG1lc3NhZ2UpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIHRyYWNlU2VuZGluZ1Jlc3BvbnNlKG1lc3NhZ2UsIG1ldGhvZCwgc3RhcnRUaW1lKSB7XG4gICAgICAgIGlmICh0cmFjZSA9PT0gVHJhY2UuT2ZmIHx8ICF0cmFjZXIpIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBpZiAodHJhY2VGb3JtYXQgPT09IFRyYWNlRm9ybWF0LlRleHQpIHtcbiAgICAgICAgICAgIGxldCBkYXRhID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgaWYgKHRyYWNlID09PSBUcmFjZS5WZXJib3NlIHx8IHRyYWNlID09PSBUcmFjZS5Db21wYWN0KSB7XG4gICAgICAgICAgICAgICAgaWYgKG1lc3NhZ2UuZXJyb3IgJiYgbWVzc2FnZS5lcnJvci5kYXRhKSB7XG4gICAgICAgICAgICAgICAgICAgIGRhdGEgPSBgRXJyb3IgZGF0YTogJHtzdHJpbmdpZnlUcmFjZShtZXNzYWdlLmVycm9yLmRhdGEpfVxcblxcbmA7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICBpZiAobWVzc2FnZS5yZXN1bHQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGRhdGEgPSBgUmVzdWx0OiAke3N0cmluZ2lmeVRyYWNlKG1lc3NhZ2UucmVzdWx0KX1cXG5cXG5gO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2UgaWYgKG1lc3NhZ2UuZXJyb3IgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgZGF0YSA9ICdObyByZXN1bHQgcmV0dXJuZWQuXFxuXFxuJztcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHRyYWNlci5sb2coYFNlbmRpbmcgcmVzcG9uc2UgJyR7bWV0aG9kfSAtICgke21lc3NhZ2UuaWR9KScuIFByb2Nlc3NpbmcgcmVxdWVzdCB0b29rICR7RGF0ZS5ub3coKSAtIHN0YXJ0VGltZX1tc2AsIGRhdGEpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgbG9nTFNQTWVzc2FnZSgnc2VuZC1yZXNwb25zZScsIG1lc3NhZ2UpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIHRyYWNlUmVjZWl2ZWRSZXF1ZXN0KG1lc3NhZ2UpIHtcbiAgICAgICAgaWYgKHRyYWNlID09PSBUcmFjZS5PZmYgfHwgIXRyYWNlcikge1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGlmICh0cmFjZUZvcm1hdCA9PT0gVHJhY2VGb3JtYXQuVGV4dCkge1xuICAgICAgICAgICAgbGV0IGRhdGEgPSB1bmRlZmluZWQ7XG4gICAgICAgICAgICBpZiAoKHRyYWNlID09PSBUcmFjZS5WZXJib3NlIHx8IHRyYWNlID09PSBUcmFjZS5Db21wYWN0KSAmJiBtZXNzYWdlLnBhcmFtcykge1xuICAgICAgICAgICAgICAgIGRhdGEgPSBgUGFyYW1zOiAke3N0cmluZ2lmeVRyYWNlKG1lc3NhZ2UucGFyYW1zKX1cXG5cXG5gO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgdHJhY2VyLmxvZyhgUmVjZWl2ZWQgcmVxdWVzdCAnJHttZXNzYWdlLm1ldGhvZH0gLSAoJHttZXNzYWdlLmlkfSknLmAsIGRhdGEpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgbG9nTFNQTWVzc2FnZSgncmVjZWl2ZS1yZXF1ZXN0JywgbWVzc2FnZSk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgZnVuY3Rpb24gdHJhY2VSZWNlaXZlZE5vdGlmaWNhdGlvbihtZXNzYWdlKSB7XG4gICAgICAgIGlmICh0cmFjZSA9PT0gVHJhY2UuT2ZmIHx8ICF0cmFjZXIgfHwgbWVzc2FnZS5tZXRob2QgPT09IExvZ1RyYWNlTm90aWZpY2F0aW9uLnR5cGUubWV0aG9kKSB7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgICAgaWYgKHRyYWNlRm9ybWF0ID09PSBUcmFjZUZvcm1hdC5UZXh0KSB7XG4gICAgICAgICAgICBsZXQgZGF0YSA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIGlmICh0cmFjZSA9PT0gVHJhY2UuVmVyYm9zZSB8fCB0cmFjZSA9PT0gVHJhY2UuQ29tcGFjdCkge1xuICAgICAgICAgICAgICAgIGlmIChtZXNzYWdlLnBhcmFtcykge1xuICAgICAgICAgICAgICAgICAgICBkYXRhID0gYFBhcmFtczogJHtzdHJpbmdpZnlUcmFjZShtZXNzYWdlLnBhcmFtcyl9XFxuXFxuYDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIGRhdGEgPSAnTm8gcGFyYW1ldGVycyBwcm92aWRlZC5cXG5cXG4nO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHRyYWNlci5sb2coYFJlY2VpdmVkIG5vdGlmaWNhdGlvbiAnJHttZXNzYWdlLm1ldGhvZH0nLmAsIGRhdGEpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgbG9nTFNQTWVzc2FnZSgncmVjZWl2ZS1ub3RpZmljYXRpb24nLCBtZXNzYWdlKTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBmdW5jdGlvbiB0cmFjZVJlY2VpdmVkUmVzcG9uc2UobWVzc2FnZSwgcmVzcG9uc2VQcm9taXNlKSB7XG4gICAgICAgIGlmICh0cmFjZSA9PT0gVHJhY2UuT2ZmIHx8ICF0cmFjZXIpIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBpZiAodHJhY2VGb3JtYXQgPT09IFRyYWNlRm9ybWF0LlRleHQpIHtcbiAgICAgICAgICAgIGxldCBkYXRhID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgaWYgKHRyYWNlID09PSBUcmFjZS5WZXJib3NlIHx8IHRyYWNlID09PSBUcmFjZS5Db21wYWN0KSB7XG4gICAgICAgICAgICAgICAgaWYgKG1lc3NhZ2UuZXJyb3IgJiYgbWVzc2FnZS5lcnJvci5kYXRhKSB7XG4gICAgICAgICAgICAgICAgICAgIGRhdGEgPSBgRXJyb3IgZGF0YTogJHtzdHJpbmdpZnlUcmFjZShtZXNzYWdlLmVycm9yLmRhdGEpfVxcblxcbmA7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICBpZiAobWVzc2FnZS5yZXN1bHQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGRhdGEgPSBgUmVzdWx0OiAke3N0cmluZ2lmeVRyYWNlKG1lc3NhZ2UucmVzdWx0KX1cXG5cXG5gO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2UgaWYgKG1lc3NhZ2UuZXJyb3IgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgZGF0YSA9ICdObyByZXN1bHQgcmV0dXJuZWQuXFxuXFxuJztcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmIChyZXNwb25zZVByb21pc2UpIHtcbiAgICAgICAgICAgICAgICBjb25zdCBlcnJvciA9IG1lc3NhZ2UuZXJyb3IgPyBgIFJlcXVlc3QgZmFpbGVkOiAke21lc3NhZ2UuZXJyb3IubWVzc2FnZX0gKCR7bWVzc2FnZS5lcnJvci5jb2RlfSkuYCA6ICcnO1xuICAgICAgICAgICAgICAgIHRyYWNlci5sb2coYFJlY2VpdmVkIHJlc3BvbnNlICcke3Jlc3BvbnNlUHJvbWlzZS5tZXRob2R9IC0gKCR7bWVzc2FnZS5pZH0pJyBpbiAke0RhdGUubm93KCkgLSByZXNwb25zZVByb21pc2UudGltZXJTdGFydH1tcy4ke2Vycm9yfWAsIGRhdGEpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgdHJhY2VyLmxvZyhgUmVjZWl2ZWQgcmVzcG9uc2UgJHttZXNzYWdlLmlkfSB3aXRob3V0IGFjdGl2ZSByZXNwb25zZSBwcm9taXNlLmAsIGRhdGEpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgbG9nTFNQTWVzc2FnZSgncmVjZWl2ZS1yZXNwb25zZScsIG1lc3NhZ2UpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIGxvZ0xTUE1lc3NhZ2UodHlwZSwgbWVzc2FnZSkge1xuICAgICAgICBpZiAoIXRyYWNlciB8fCB0cmFjZSA9PT0gVHJhY2UuT2ZmKSB7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgICAgY29uc3QgbHNwTWVzc2FnZSA9IHtcbiAgICAgICAgICAgIGlzTFNQTWVzc2FnZTogdHJ1ZSxcbiAgICAgICAgICAgIHR5cGUsXG4gICAgICAgICAgICBtZXNzYWdlLFxuICAgICAgICAgICAgdGltZXN0YW1wOiBEYXRlLm5vdygpXG4gICAgICAgIH07XG4gICAgICAgIHRyYWNlci5sb2cobHNwTWVzc2FnZSk7XG4gICAgfVxuICAgIGZ1bmN0aW9uIHRocm93SWZDbG9zZWRPckRpc3Bvc2VkKCkge1xuICAgICAgICBpZiAoaXNDbG9zZWQoKSkge1xuICAgICAgICAgICAgdGhyb3cgbmV3IENvbm5lY3Rpb25FcnJvcihDb25uZWN0aW9uRXJyb3JzLkNsb3NlZCwgJ0Nvbm5lY3Rpb24gaXMgY2xvc2VkLicpO1xuICAgICAgICB9XG4gICAgICAgIGlmIChpc0Rpc3Bvc2VkKCkpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBDb25uZWN0aW9uRXJyb3IoQ29ubmVjdGlvbkVycm9ycy5EaXNwb3NlZCwgJ0Nvbm5lY3Rpb24gaXMgZGlzcG9zZWQuJyk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgZnVuY3Rpb24gdGhyb3dJZkxpc3RlbmluZygpIHtcbiAgICAgICAgaWYgKGlzTGlzdGVuaW5nKCkpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBDb25uZWN0aW9uRXJyb3IoQ29ubmVjdGlvbkVycm9ycy5BbHJlYWR5TGlzdGVuaW5nLCAnQ29ubmVjdGlvbiBpcyBhbHJlYWR5IGxpc3RlbmluZycpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIHRocm93SWZOb3RMaXN0ZW5pbmcoKSB7XG4gICAgICAgIGlmICghaXNMaXN0ZW5pbmcoKSkge1xuICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdDYWxsIGxpc3RlbigpIGZpcnN0LicpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIHVuZGVmaW5lZFRvTnVsbChwYXJhbSkge1xuICAgICAgICBpZiAocGFyYW0gPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmV0dXJuIG51bGw7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICByZXR1cm4gcGFyYW07XG4gICAgICAgIH1cbiAgICB9XG4gICAgZnVuY3Rpb24gbnVsbFRvVW5kZWZpbmVkKHBhcmFtKSB7XG4gICAgICAgIGlmIChwYXJhbSA9PT0gbnVsbCkge1xuICAgICAgICAgICAgcmV0dXJuIHVuZGVmaW5lZDtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHJldHVybiBwYXJhbTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBmdW5jdGlvbiBpc05hbWVkUGFyYW0ocGFyYW0pIHtcbiAgICAgICAgcmV0dXJuIHBhcmFtICE9PSB1bmRlZmluZWQgJiYgcGFyYW0gIT09IG51bGwgJiYgIUFycmF5LmlzQXJyYXkocGFyYW0pICYmIHR5cGVvZiBwYXJhbSA9PT0gJ29iamVjdCc7XG4gICAgfVxuICAgIGZ1bmN0aW9uIGNvbXB1dGVTaW5nbGVQYXJhbShwYXJhbWV0ZXJTdHJ1Y3R1cmVzLCBwYXJhbSkge1xuICAgICAgICBzd2l0Y2ggKHBhcmFtZXRlclN0cnVjdHVyZXMpIHtcbiAgICAgICAgICAgIGNhc2UgbWVzc2FnZXNfMS5QYXJhbWV0ZXJTdHJ1Y3R1cmVzLmF1dG86XG4gICAgICAgICAgICAgICAgaWYgKGlzTmFtZWRQYXJhbShwYXJhbSkpIHtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIG51bGxUb1VuZGVmaW5lZChwYXJhbSk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4gW3VuZGVmaW5lZFRvTnVsbChwYXJhbSldO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNhc2UgbWVzc2FnZXNfMS5QYXJhbWV0ZXJTdHJ1Y3R1cmVzLmJ5TmFtZTpcbiAgICAgICAgICAgICAgICBpZiAoIWlzTmFtZWRQYXJhbShwYXJhbSkpIHtcbiAgICAgICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBSZWNlaXZlZCBwYXJhbWV0ZXJzIGJ5IG5hbWUgYnV0IHBhcmFtIGlzIG5vdCBhbiBvYmplY3QgbGl0ZXJhbC5gKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgcmV0dXJuIG51bGxUb1VuZGVmaW5lZChwYXJhbSk7XG4gICAgICAgICAgICBjYXNlIG1lc3NhZ2VzXzEuUGFyYW1ldGVyU3RydWN0dXJlcy5ieVBvc2l0aW9uOlxuICAgICAgICAgICAgICAgIHJldHVybiBbdW5kZWZpbmVkVG9OdWxsKHBhcmFtKV07XG4gICAgICAgICAgICBkZWZhdWx0OlxuICAgICAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgVW5rbm93biBwYXJhbWV0ZXIgc3RydWN0dXJlICR7cGFyYW1ldGVyU3RydWN0dXJlcy50b1N0cmluZygpfWApO1xuICAgICAgICB9XG4gICAgfVxuICAgIGZ1bmN0aW9uIGNvbXB1dGVNZXNzYWdlUGFyYW1zKHR5cGUsIHBhcmFtcykge1xuICAgICAgICBsZXQgcmVzdWx0O1xuICAgICAgICBjb25zdCBudW1iZXJPZlBhcmFtcyA9IHR5cGUubnVtYmVyT2ZQYXJhbXM7XG4gICAgICAgIHN3aXRjaCAobnVtYmVyT2ZQYXJhbXMpIHtcbiAgICAgICAgICAgIGNhc2UgMDpcbiAgICAgICAgICAgICAgICByZXN1bHQgPSB1bmRlZmluZWQ7XG4gICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICBjYXNlIDE6XG4gICAgICAgICAgICAgICAgcmVzdWx0ID0gY29tcHV0ZVNpbmdsZVBhcmFtKHR5cGUucGFyYW1ldGVyU3RydWN0dXJlcywgcGFyYW1zWzBdKTtcbiAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgIGRlZmF1bHQ6XG4gICAgICAgICAgICAgICAgcmVzdWx0ID0gW107XG4gICAgICAgICAgICAgICAgZm9yIChsZXQgaSA9IDA7IGkgPCBwYXJhbXMubGVuZ3RoICYmIGkgPCBudW1iZXJPZlBhcmFtczsgaSsrKSB7XG4gICAgICAgICAgICAgICAgICAgIHJlc3VsdC5wdXNoKHVuZGVmaW5lZFRvTnVsbChwYXJhbXNbaV0pKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgaWYgKHBhcmFtcy5sZW5ndGggPCBudW1iZXJPZlBhcmFtcykge1xuICAgICAgICAgICAgICAgICAgICBmb3IgKGxldCBpID0gcGFyYW1zLmxlbmd0aDsgaSA8IG51bWJlck9mUGFyYW1zOyBpKyspIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJlc3VsdC5wdXNoKG51bGwpO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxuICAgIGNvbnN0IGNvbm5lY3Rpb24gPSB7XG4gICAgICAgIHNlbmROb3RpZmljYXRpb246ICh0eXBlLCAuLi5hcmdzKSA9PiB7XG4gICAgICAgICAgICB0aHJvd0lmQ2xvc2VkT3JEaXNwb3NlZCgpO1xuICAgICAgICAgICAgbGV0IG1ldGhvZDtcbiAgICAgICAgICAgIGxldCBtZXNzYWdlUGFyYW1zO1xuICAgICAgICAgICAgaWYgKElzLnN0cmluZyh0eXBlKSkge1xuICAgICAgICAgICAgICAgIG1ldGhvZCA9IHR5cGU7XG4gICAgICAgICAgICAgICAgY29uc3QgZmlyc3QgPSBhcmdzWzBdO1xuICAgICAgICAgICAgICAgIGxldCBwYXJhbVN0YXJ0ID0gMDtcbiAgICAgICAgICAgICAgICBsZXQgcGFyYW1ldGVyU3RydWN0dXJlcyA9IG1lc3NhZ2VzXzEuUGFyYW1ldGVyU3RydWN0dXJlcy5hdXRvO1xuICAgICAgICAgICAgICAgIGlmIChtZXNzYWdlc18xLlBhcmFtZXRlclN0cnVjdHVyZXMuaXMoZmlyc3QpKSB7XG4gICAgICAgICAgICAgICAgICAgIHBhcmFtU3RhcnQgPSAxO1xuICAgICAgICAgICAgICAgICAgICBwYXJhbWV0ZXJTdHJ1Y3R1cmVzID0gZmlyc3Q7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGxldCBwYXJhbUVuZCA9IGFyZ3MubGVuZ3RoO1xuICAgICAgICAgICAgICAgIGNvbnN0IG51bWJlck9mUGFyYW1zID0gcGFyYW1FbmQgLSBwYXJhbVN0YXJ0O1xuICAgICAgICAgICAgICAgIHN3aXRjaCAobnVtYmVyT2ZQYXJhbXMpIHtcbiAgICAgICAgICAgICAgICAgICAgY2FzZSAwOlxuICAgICAgICAgICAgICAgICAgICAgICAgbWVzc2FnZVBhcmFtcyA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICBjYXNlIDE6XG4gICAgICAgICAgICAgICAgICAgICAgICBtZXNzYWdlUGFyYW1zID0gY29tcHV0ZVNpbmdsZVBhcmFtKHBhcmFtZXRlclN0cnVjdHVyZXMsIGFyZ3NbcGFyYW1TdGFydF0pO1xuICAgICAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgICAgIGRlZmF1bHQ6XG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAocGFyYW1ldGVyU3RydWN0dXJlcyA9PT0gbWVzc2FnZXNfMS5QYXJhbWV0ZXJTdHJ1Y3R1cmVzLmJ5TmFtZSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgUmVjZWl2ZWQgJHtudW1iZXJPZlBhcmFtc30gcGFyYW1ldGVycyBmb3IgJ2J5IE5hbWUnIG5vdGlmaWNhdGlvbiBwYXJhbWV0ZXIgc3RydWN0dXJlLmApO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgbWVzc2FnZVBhcmFtcyA9IGFyZ3Muc2xpY2UocGFyYW1TdGFydCwgcGFyYW1FbmQpLm1hcCh2YWx1ZSA9PiB1bmRlZmluZWRUb051bGwodmFsdWUpKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIGNvbnN0IHBhcmFtcyA9IGFyZ3M7XG4gICAgICAgICAgICAgICAgbWV0aG9kID0gdHlwZS5tZXRob2Q7XG4gICAgICAgICAgICAgICAgbWVzc2FnZVBhcmFtcyA9IGNvbXB1dGVNZXNzYWdlUGFyYW1zKHR5cGUsIHBhcmFtcyk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBjb25zdCBub3RpZmljYXRpb25NZXNzYWdlID0ge1xuICAgICAgICAgICAgICAgIGpzb25ycGM6IHZlcnNpb24sXG4gICAgICAgICAgICAgICAgbWV0aG9kOiBtZXRob2QsXG4gICAgICAgICAgICAgICAgcGFyYW1zOiBtZXNzYWdlUGFyYW1zXG4gICAgICAgICAgICB9O1xuICAgICAgICAgICAgdHJhY2VTZW5kaW5nTm90aWZpY2F0aW9uKG5vdGlmaWNhdGlvbk1lc3NhZ2UpO1xuICAgICAgICAgICAgcmV0dXJuIG1lc3NhZ2VXcml0ZXIud3JpdGUobm90aWZpY2F0aW9uTWVzc2FnZSkuY2F0Y2goKGVycm9yKSA9PiB7XG4gICAgICAgICAgICAgICAgbG9nZ2VyLmVycm9yKGBTZW5kaW5nIG5vdGlmaWNhdGlvbiBmYWlsZWQuYCk7XG4gICAgICAgICAgICAgICAgdGhyb3cgZXJyb3I7XG4gICAgICAgICAgICB9KTtcbiAgICAgICAgfSxcbiAgICAgICAgb25Ob3RpZmljYXRpb246ICh0eXBlLCBoYW5kbGVyKSA9PiB7XG4gICAgICAgICAgICB0aHJvd0lmQ2xvc2VkT3JEaXNwb3NlZCgpO1xuICAgICAgICAgICAgbGV0IG1ldGhvZDtcbiAgICAgICAgICAgIGlmIChJcy5mdW5jKHR5cGUpKSB7XG4gICAgICAgICAgICAgICAgc3Rhck5vdGlmaWNhdGlvbkhhbmRsZXIgPSB0eXBlO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSBpZiAoaGFuZGxlcikge1xuICAgICAgICAgICAgICAgIGlmIChJcy5zdHJpbmcodHlwZSkpIHtcbiAgICAgICAgICAgICAgICAgICAgbWV0aG9kID0gdHlwZTtcbiAgICAgICAgICAgICAgICAgICAgbm90aWZpY2F0aW9uSGFuZGxlcnMuc2V0KHR5cGUsIHsgdHlwZTogdW5kZWZpbmVkLCBoYW5kbGVyIH0pO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgbWV0aG9kID0gdHlwZS5tZXRob2Q7XG4gICAgICAgICAgICAgICAgICAgIG5vdGlmaWNhdGlvbkhhbmRsZXJzLnNldCh0eXBlLm1ldGhvZCwgeyB0eXBlLCBoYW5kbGVyIH0pO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICAgICAgZGlzcG9zZTogKCkgPT4ge1xuICAgICAgICAgICAgICAgICAgICBpZiAobWV0aG9kICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIG5vdGlmaWNhdGlvbkhhbmRsZXJzLmRlbGV0ZShtZXRob2QpO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgc3Rhck5vdGlmaWNhdGlvbkhhbmRsZXIgPSB1bmRlZmluZWQ7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9O1xuICAgICAgICB9LFxuICAgICAgICBvblByb2dyZXNzOiAoX3R5cGUsIHRva2VuLCBoYW5kbGVyKSA9PiB7XG4gICAgICAgICAgICBpZiAocHJvZ3Jlc3NIYW5kbGVycy5oYXModG9rZW4pKSB7XG4gICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBQcm9ncmVzcyBoYW5kbGVyIGZvciB0b2tlbiAke3Rva2VufSBhbHJlYWR5IHJlZ2lzdGVyZWRgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHByb2dyZXNzSGFuZGxlcnMuc2V0KHRva2VuLCBoYW5kbGVyKTtcbiAgICAgICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICAgICAgZGlzcG9zZTogKCkgPT4ge1xuICAgICAgICAgICAgICAgICAgICBwcm9ncmVzc0hhbmRsZXJzLmRlbGV0ZSh0b2tlbik7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfTtcbiAgICAgICAgfSxcbiAgICAgICAgc2VuZFByb2dyZXNzOiAoX3R5cGUsIHRva2VuLCB2YWx1ZSkgPT4ge1xuICAgICAgICAgICAgLy8gVGhpcyBzaG91bGQgbm90IGF3YWl0IGJ1dCBzaW1wbGUgcmV0dXJuIHRvIGVuc3VyZSB0aGF0IHdlIGRvbid0IGhhdmUgYW5vdGhlclxuICAgICAgICAgICAgLy8gYXN5bmMgc2NoZWR1bGluZy4gT3RoZXJ3aXNlIG9uZSBzZW5kIGNvdWxkIG92ZXJ0YWtlIGFub3RoZXIgc2VuZC5cbiAgICAgICAgICAgIHJldHVybiBjb25uZWN0aW9uLnNlbmROb3RpZmljYXRpb24oUHJvZ3Jlc3NOb3RpZmljYXRpb24udHlwZSwgeyB0b2tlbiwgdmFsdWUgfSk7XG4gICAgICAgIH0sXG4gICAgICAgIG9uVW5oYW5kbGVkUHJvZ3Jlc3M6IHVuaGFuZGxlZFByb2dyZXNzRW1pdHRlci5ldmVudCxcbiAgICAgICAgc2VuZFJlcXVlc3Q6ICh0eXBlLCAuLi5hcmdzKSA9PiB7XG4gICAgICAgICAgICB0aHJvd0lmQ2xvc2VkT3JEaXNwb3NlZCgpO1xuICAgICAgICAgICAgdGhyb3dJZk5vdExpc3RlbmluZygpO1xuICAgICAgICAgICAgbGV0IG1ldGhvZDtcbiAgICAgICAgICAgIGxldCBtZXNzYWdlUGFyYW1zO1xuICAgICAgICAgICAgbGV0IHRva2VuID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgaWYgKElzLnN0cmluZyh0eXBlKSkge1xuICAgICAgICAgICAgICAgIG1ldGhvZCA9IHR5cGU7XG4gICAgICAgICAgICAgICAgY29uc3QgZmlyc3QgPSBhcmdzWzBdO1xuICAgICAgICAgICAgICAgIGNvbnN0IGxhc3QgPSBhcmdzW2FyZ3MubGVuZ3RoIC0gMV07XG4gICAgICAgICAgICAgICAgbGV0IHBhcmFtU3RhcnQgPSAwO1xuICAgICAgICAgICAgICAgIGxldCBwYXJhbWV0ZXJTdHJ1Y3R1cmVzID0gbWVzc2FnZXNfMS5QYXJhbWV0ZXJTdHJ1Y3R1cmVzLmF1dG87XG4gICAgICAgICAgICAgICAgaWYgKG1lc3NhZ2VzXzEuUGFyYW1ldGVyU3RydWN0dXJlcy5pcyhmaXJzdCkpIHtcbiAgICAgICAgICAgICAgICAgICAgcGFyYW1TdGFydCA9IDE7XG4gICAgICAgICAgICAgICAgICAgIHBhcmFtZXRlclN0cnVjdHVyZXMgPSBmaXJzdDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgbGV0IHBhcmFtRW5kID0gYXJncy5sZW5ndGg7XG4gICAgICAgICAgICAgICAgaWYgKGNhbmNlbGxhdGlvbl8xLkNhbmNlbGxhdGlvblRva2VuLmlzKGxhc3QpKSB7XG4gICAgICAgICAgICAgICAgICAgIHBhcmFtRW5kID0gcGFyYW1FbmQgLSAxO1xuICAgICAgICAgICAgICAgICAgICB0b2tlbiA9IGxhc3Q7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGNvbnN0IG51bWJlck9mUGFyYW1zID0gcGFyYW1FbmQgLSBwYXJhbVN0YXJ0O1xuICAgICAgICAgICAgICAgIHN3aXRjaCAobnVtYmVyT2ZQYXJhbXMpIHtcbiAgICAgICAgICAgICAgICAgICAgY2FzZSAwOlxuICAgICAgICAgICAgICAgICAgICAgICAgbWVzc2FnZVBhcmFtcyA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICBjYXNlIDE6XG4gICAgICAgICAgICAgICAgICAgICAgICBtZXNzYWdlUGFyYW1zID0gY29tcHV0ZVNpbmdsZVBhcmFtKHBhcmFtZXRlclN0cnVjdHVyZXMsIGFyZ3NbcGFyYW1TdGFydF0pO1xuICAgICAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgICAgIGRlZmF1bHQ6XG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAocGFyYW1ldGVyU3RydWN0dXJlcyA9PT0gbWVzc2FnZXNfMS5QYXJhbWV0ZXJTdHJ1Y3R1cmVzLmJ5TmFtZSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgUmVjZWl2ZWQgJHtudW1iZXJPZlBhcmFtc30gcGFyYW1ldGVycyBmb3IgJ2J5IE5hbWUnIHJlcXVlc3QgcGFyYW1ldGVyIHN0cnVjdHVyZS5gKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIG1lc3NhZ2VQYXJhbXMgPSBhcmdzLnNsaWNlKHBhcmFtU3RhcnQsIHBhcmFtRW5kKS5tYXAodmFsdWUgPT4gdW5kZWZpbmVkVG9OdWxsKHZhbHVlKSk7XG4gICAgICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICBjb25zdCBwYXJhbXMgPSBhcmdzO1xuICAgICAgICAgICAgICAgIG1ldGhvZCA9IHR5cGUubWV0aG9kO1xuICAgICAgICAgICAgICAgIG1lc3NhZ2VQYXJhbXMgPSBjb21wdXRlTWVzc2FnZVBhcmFtcyh0eXBlLCBwYXJhbXMpO1xuICAgICAgICAgICAgICAgIGNvbnN0IG51bWJlck9mUGFyYW1zID0gdHlwZS5udW1iZXJPZlBhcmFtcztcbiAgICAgICAgICAgICAgICB0b2tlbiA9IGNhbmNlbGxhdGlvbl8xLkNhbmNlbGxhdGlvblRva2VuLmlzKHBhcmFtc1tudW1iZXJPZlBhcmFtc10pID8gcGFyYW1zW251bWJlck9mUGFyYW1zXSA6IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGNvbnN0IGlkID0gc2VxdWVuY2VOdW1iZXIrKztcbiAgICAgICAgICAgIGxldCBkaXNwb3NhYmxlO1xuICAgICAgICAgICAgaWYgKHRva2VuKSB7XG4gICAgICAgICAgICAgICAgZGlzcG9zYWJsZSA9IHRva2VuLm9uQ2FuY2VsbGF0aW9uUmVxdWVzdGVkKCgpID0+IHtcbiAgICAgICAgICAgICAgICAgICAgY29uc3QgcCA9IGNhbmNlbGxhdGlvblN0cmF0ZWd5LnNlbmRlci5zZW5kQ2FuY2VsbGF0aW9uKGNvbm5lY3Rpb24sIGlkKTtcbiAgICAgICAgICAgICAgICAgICAgaWYgKHAgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgbG9nZ2VyLmxvZyhgUmVjZWl2ZWQgbm8gcHJvbWlzZSBmcm9tIGNhbmNlbGxhdGlvbiBzdHJhdGVneSB3aGVuIGNhbmNlbGxpbmcgaWQgJHtpZH1gKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiBQcm9taXNlLnJlc29sdmUoKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiBwLmNhdGNoKCgpID0+IHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBsb2dnZXIubG9nKGBTZW5kaW5nIGNhbmNlbGxhdGlvbiBtZXNzYWdlcyBmb3IgaWQgJHtpZH0gZmFpbGVkYCk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9KTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgY29uc3QgcmVxdWVzdE1lc3NhZ2UgPSB7XG4gICAgICAgICAgICAgICAganNvbnJwYzogdmVyc2lvbixcbiAgICAgICAgICAgICAgICBpZDogaWQsXG4gICAgICAgICAgICAgICAgbWV0aG9kOiBtZXRob2QsXG4gICAgICAgICAgICAgICAgcGFyYW1zOiBtZXNzYWdlUGFyYW1zXG4gICAgICAgICAgICB9O1xuICAgICAgICAgICAgdHJhY2VTZW5kaW5nUmVxdWVzdChyZXF1ZXN0TWVzc2FnZSk7XG4gICAgICAgICAgICBpZiAodHlwZW9mIGNhbmNlbGxhdGlvblN0cmF0ZWd5LnNlbmRlci5lbmFibGVDYW5jZWxsYXRpb24gPT09ICdmdW5jdGlvbicpIHtcbiAgICAgICAgICAgICAgICBjYW5jZWxsYXRpb25TdHJhdGVneS5zZW5kZXIuZW5hYmxlQ2FuY2VsbGF0aW9uKHJlcXVlc3RNZXNzYWdlKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJldHVybiBuZXcgUHJvbWlzZShhc3luYyAocmVzb2x2ZSwgcmVqZWN0KSA9PiB7XG4gICAgICAgICAgICAgICAgY29uc3QgcmVzb2x2ZVdpdGhDbGVhbnVwID0gKHIpID0+IHtcbiAgICAgICAgICAgICAgICAgICAgcmVzb2x2ZShyKTtcbiAgICAgICAgICAgICAgICAgICAgY2FuY2VsbGF0aW9uU3RyYXRlZ3kuc2VuZGVyLmNsZWFudXAoaWQpO1xuICAgICAgICAgICAgICAgICAgICBkaXNwb3NhYmxlPy5kaXNwb3NlKCk7XG4gICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgICAgICBjb25zdCByZWplY3RXaXRoQ2xlYW51cCA9IChyKSA9PiB7XG4gICAgICAgICAgICAgICAgICAgIHJlamVjdChyKTtcbiAgICAgICAgICAgICAgICAgICAgY2FuY2VsbGF0aW9uU3RyYXRlZ3kuc2VuZGVyLmNsZWFudXAoaWQpO1xuICAgICAgICAgICAgICAgICAgICBkaXNwb3NhYmxlPy5kaXNwb3NlKCk7XG4gICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgICAgICBjb25zdCByZXNwb25zZVByb21pc2UgPSB7IG1ldGhvZDogbWV0aG9kLCB0aW1lclN0YXJ0OiBEYXRlLm5vdygpLCByZXNvbHZlOiByZXNvbHZlV2l0aENsZWFudXAsIHJlamVjdDogcmVqZWN0V2l0aENsZWFudXAgfTtcbiAgICAgICAgICAgICAgICB0cnkge1xuICAgICAgICAgICAgICAgICAgICByZXNwb25zZVByb21pc2VzLnNldChpZCwgcmVzcG9uc2VQcm9taXNlKTtcbiAgICAgICAgICAgICAgICAgICAgYXdhaXQgbWVzc2FnZVdyaXRlci53cml0ZShyZXF1ZXN0TWVzc2FnZSk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGNhdGNoIChlcnJvcikge1xuICAgICAgICAgICAgICAgICAgICAvLyBXcml0aW5nIHRoZSBtZXNzYWdlIGZhaWxlZC4gU28gd2UgbmVlZCB0byBkZWxldGUgaXQgZnJvbSB0aGUgcmVzcG9uc2UgcHJvbWlzZXMgYW5kXG4gICAgICAgICAgICAgICAgICAgIC8vIHJlamVjdCBpdC5cbiAgICAgICAgICAgICAgICAgICAgcmVzcG9uc2VQcm9taXNlcy5kZWxldGUoaWQpO1xuICAgICAgICAgICAgICAgICAgICByZXNwb25zZVByb21pc2UucmVqZWN0KG5ldyBtZXNzYWdlc18xLlJlc3BvbnNlRXJyb3IobWVzc2FnZXNfMS5FcnJvckNvZGVzLk1lc3NhZ2VXcml0ZUVycm9yLCBlcnJvci5tZXNzYWdlID8gZXJyb3IubWVzc2FnZSA6ICdVbmtub3duIHJlYXNvbicpKTtcbiAgICAgICAgICAgICAgICAgICAgbG9nZ2VyLmVycm9yKGBTZW5kaW5nIHJlcXVlc3QgZmFpbGVkLmApO1xuICAgICAgICAgICAgICAgICAgICB0aHJvdyBlcnJvcjtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9KTtcbiAgICAgICAgfSxcbiAgICAgICAgb25SZXF1ZXN0OiAodHlwZSwgaGFuZGxlcikgPT4ge1xuICAgICAgICAgICAgdGhyb3dJZkNsb3NlZE9yRGlzcG9zZWQoKTtcbiAgICAgICAgICAgIGxldCBtZXRob2QgPSBudWxsO1xuICAgICAgICAgICAgaWYgKFN0YXJSZXF1ZXN0SGFuZGxlci5pcyh0eXBlKSkge1xuICAgICAgICAgICAgICAgIG1ldGhvZCA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgICAgICBzdGFyUmVxdWVzdEhhbmRsZXIgPSB0eXBlO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSBpZiAoSXMuc3RyaW5nKHR5cGUpKSB7XG4gICAgICAgICAgICAgICAgbWV0aG9kID0gbnVsbDtcbiAgICAgICAgICAgICAgICBpZiAoaGFuZGxlciAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICAgICAgICAgIG1ldGhvZCA9IHR5cGU7XG4gICAgICAgICAgICAgICAgICAgIHJlcXVlc3RIYW5kbGVycy5zZXQodHlwZSwgeyBoYW5kbGVyOiBoYW5kbGVyLCB0eXBlOiB1bmRlZmluZWQgfSk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgaWYgKGhhbmRsZXIgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgICAgICAgICBtZXRob2QgPSB0eXBlLm1ldGhvZDtcbiAgICAgICAgICAgICAgICAgICAgcmVxdWVzdEhhbmRsZXJzLnNldCh0eXBlLm1ldGhvZCwgeyB0eXBlLCBoYW5kbGVyIH0pO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICAgICAgZGlzcG9zZTogKCkgPT4ge1xuICAgICAgICAgICAgICAgICAgICBpZiAobWV0aG9kID09PSBudWxsKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgaWYgKG1ldGhvZCAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXF1ZXN0SGFuZGxlcnMuZGVsZXRlKG1ldGhvZCk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBzdGFyUmVxdWVzdEhhbmRsZXIgPSB1bmRlZmluZWQ7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9O1xuICAgICAgICB9LFxuICAgICAgICBoYXNQZW5kaW5nUmVzcG9uc2U6ICgpID0+IHtcbiAgICAgICAgICAgIHJldHVybiByZXNwb25zZVByb21pc2VzLnNpemUgPiAwO1xuICAgICAgICB9LFxuICAgICAgICB0cmFjZTogYXN5bmMgKF92YWx1ZSwgX3RyYWNlciwgc2VuZE5vdGlmaWNhdGlvbk9yVHJhY2VPcHRpb25zKSA9PiB7XG4gICAgICAgICAgICBsZXQgX3NlbmROb3RpZmljYXRpb24gPSBmYWxzZTtcbiAgICAgICAgICAgIGxldCBfdHJhY2VGb3JtYXQgPSBUcmFjZUZvcm1hdC5UZXh0O1xuICAgICAgICAgICAgaWYgKHNlbmROb3RpZmljYXRpb25PclRyYWNlT3B0aW9ucyAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICAgICAgaWYgKElzLmJvb2xlYW4oc2VuZE5vdGlmaWNhdGlvbk9yVHJhY2VPcHRpb25zKSkge1xuICAgICAgICAgICAgICAgICAgICBfc2VuZE5vdGlmaWNhdGlvbiA9IHNlbmROb3RpZmljYXRpb25PclRyYWNlT3B0aW9ucztcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIF9zZW5kTm90aWZpY2F0aW9uID0gc2VuZE5vdGlmaWNhdGlvbk9yVHJhY2VPcHRpb25zLnNlbmROb3RpZmljYXRpb24gfHwgZmFsc2U7XG4gICAgICAgICAgICAgICAgICAgIF90cmFjZUZvcm1hdCA9IHNlbmROb3RpZmljYXRpb25PclRyYWNlT3B0aW9ucy50cmFjZUZvcm1hdCB8fCBUcmFjZUZvcm1hdC5UZXh0O1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHRyYWNlID0gX3ZhbHVlO1xuICAgICAgICAgICAgdHJhY2VGb3JtYXQgPSBfdHJhY2VGb3JtYXQ7XG4gICAgICAgICAgICBpZiAodHJhY2UgPT09IFRyYWNlLk9mZikge1xuICAgICAgICAgICAgICAgIHRyYWNlciA9IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIHRyYWNlciA9IF90cmFjZXI7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAoX3NlbmROb3RpZmljYXRpb24gJiYgIWlzQ2xvc2VkKCkgJiYgIWlzRGlzcG9zZWQoKSkge1xuICAgICAgICAgICAgICAgIGF3YWl0IGNvbm5lY3Rpb24uc2VuZE5vdGlmaWNhdGlvbihTZXRUcmFjZU5vdGlmaWNhdGlvbi50eXBlLCB7IHZhbHVlOiBUcmFjZS50b1N0cmluZyhfdmFsdWUpIH0pO1xuICAgICAgICAgICAgfVxuICAgICAgICB9LFxuICAgICAgICBvbkVycm9yOiBlcnJvckVtaXR0ZXIuZXZlbnQsXG4gICAgICAgIG9uQ2xvc2U6IGNsb3NlRW1pdHRlci5ldmVudCxcbiAgICAgICAgb25VbmhhbmRsZWROb3RpZmljYXRpb246IHVuaGFuZGxlZE5vdGlmaWNhdGlvbkVtaXR0ZXIuZXZlbnQsXG4gICAgICAgIG9uRGlzcG9zZTogZGlzcG9zZUVtaXR0ZXIuZXZlbnQsXG4gICAgICAgIGVuZDogKCkgPT4ge1xuICAgICAgICAgICAgbWVzc2FnZVdyaXRlci5lbmQoKTtcbiAgICAgICAgfSxcbiAgICAgICAgZGlzcG9zZTogKCkgPT4ge1xuICAgICAgICAgICAgaWYgKGlzRGlzcG9zZWQoKSkge1xuICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHN0YXRlID0gQ29ubmVjdGlvblN0YXRlLkRpc3Bvc2VkO1xuICAgICAgICAgICAgZGlzcG9zZUVtaXR0ZXIuZmlyZSh1bmRlZmluZWQpO1xuICAgICAgICAgICAgY29uc3QgZXJyb3IgPSBuZXcgbWVzc2FnZXNfMS5SZXNwb25zZUVycm9yKG1lc3NhZ2VzXzEuRXJyb3JDb2Rlcy5QZW5kaW5nUmVzcG9uc2VSZWplY3RlZCwgJ1BlbmRpbmcgcmVzcG9uc2UgcmVqZWN0ZWQgc2luY2UgY29ubmVjdGlvbiBnb3QgZGlzcG9zZWQnKTtcbiAgICAgICAgICAgIGZvciAoY29uc3QgcHJvbWlzZSBvZiByZXNwb25zZVByb21pc2VzLnZhbHVlcygpKSB7XG4gICAgICAgICAgICAgICAgcHJvbWlzZS5yZWplY3QoZXJyb3IpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgcmVzcG9uc2VQcm9taXNlcyA9IG5ldyBNYXAoKTtcbiAgICAgICAgICAgIHJlcXVlc3RUb2tlbnMgPSBuZXcgTWFwKCk7XG4gICAgICAgICAgICBrbm93bkNhbmNlbGVkUmVxdWVzdHMgPSBuZXcgU2V0KCk7XG4gICAgICAgICAgICBtZXNzYWdlUXVldWUgPSBuZXcgbGlua2VkTWFwXzEuTGlua2VkTWFwKCk7XG4gICAgICAgICAgICAvLyBUZXN0IGZvciBiYWNrd2FyZHMgY29tcGF0aWJpbGl0eVxuICAgICAgICAgICAgaWYgKElzLmZ1bmMobWVzc2FnZVdyaXRlci5kaXNwb3NlKSkge1xuICAgICAgICAgICAgICAgIG1lc3NhZ2VXcml0ZXIuZGlzcG9zZSgpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaWYgKElzLmZ1bmMobWVzc2FnZVJlYWRlci5kaXNwb3NlKSkge1xuICAgICAgICAgICAgICAgIG1lc3NhZ2VSZWFkZXIuZGlzcG9zZSgpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9LFxuICAgICAgICBsaXN0ZW46ICgpID0+IHtcbiAgICAgICAgICAgIHRocm93SWZDbG9zZWRPckRpc3Bvc2VkKCk7XG4gICAgICAgICAgICB0aHJvd0lmTGlzdGVuaW5nKCk7XG4gICAgICAgICAgICBzdGF0ZSA9IENvbm5lY3Rpb25TdGF0ZS5MaXN0ZW5pbmc7XG4gICAgICAgICAgICBtZXNzYWdlUmVhZGVyLmxpc3RlbihjYWxsYmFjayk7XG4gICAgICAgIH0sXG4gICAgICAgIGluc3BlY3Q6ICgpID0+IHtcbiAgICAgICAgICAgIC8vIGVzbGludC1kaXNhYmxlLW5leHQtbGluZSBuby1jb25zb2xlXG4gICAgICAgICAgICAoMCwgcmFsXzEuZGVmYXVsdCkoKS5jb25zb2xlLmxvZygnaW5zcGVjdCcpO1xuICAgICAgICB9XG4gICAgfTtcbiAgICBjb25uZWN0aW9uLm9uTm90aWZpY2F0aW9uKExvZ1RyYWNlTm90aWZpY2F0aW9uLnR5cGUsIChwYXJhbXMpID0+IHtcbiAgICAgICAgaWYgKHRyYWNlID09PSBUcmFjZS5PZmYgfHwgIXRyYWNlcikge1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIGNvbnN0IHZlcmJvc2UgPSB0cmFjZSA9PT0gVHJhY2UuVmVyYm9zZSB8fCB0cmFjZSA9PT0gVHJhY2UuQ29tcGFjdDtcbiAgICAgICAgdHJhY2VyLmxvZyhwYXJhbXMubWVzc2FnZSwgdmVyYm9zZSA/IHBhcmFtcy52ZXJib3NlIDogdW5kZWZpbmVkKTtcbiAgICB9KTtcbiAgICBjb25uZWN0aW9uLm9uTm90aWZpY2F0aW9uKFByb2dyZXNzTm90aWZpY2F0aW9uLnR5cGUsIChwYXJhbXMpID0+IHtcbiAgICAgICAgY29uc3QgaGFuZGxlciA9IHByb2dyZXNzSGFuZGxlcnMuZ2V0KHBhcmFtcy50b2tlbik7XG4gICAgICAgIGlmIChoYW5kbGVyKSB7XG4gICAgICAgICAgICBoYW5kbGVyKHBhcmFtcy52YWx1ZSk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICB1bmhhbmRsZWRQcm9ncmVzc0VtaXR0ZXIuZmlyZShwYXJhbXMpO1xuICAgICAgICB9XG4gICAgfSk7XG4gICAgcmV0dXJuIGNvbm5lY3Rpb247XG59XG5leHBvcnRzLmNyZWF0ZU1lc3NhZ2VDb25uZWN0aW9uID0gY3JlYXRlTWVzc2FnZUNvbm5lY3Rpb247XG4iLCAiXCJ1c2Ugc3RyaWN0XCI7XG4vKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG4vLy8gPHJlZmVyZW5jZSBwYXRoPVwiLi4vLi4vdHlwaW5ncy90aGVuYWJsZS5kLnRzXCIgLz5cbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIl9fZXNNb2R1bGVcIiwgeyB2YWx1ZTogdHJ1ZSB9KTtcbmV4cG9ydHMuUHJvZ3Jlc3NUeXBlID0gZXhwb3J0cy5Qcm9ncmVzc1Rva2VuID0gZXhwb3J0cy5jcmVhdGVNZXNzYWdlQ29ubmVjdGlvbiA9IGV4cG9ydHMuTnVsbExvZ2dlciA9IGV4cG9ydHMuQ29ubmVjdGlvbk9wdGlvbnMgPSBleHBvcnRzLkNvbm5lY3Rpb25TdHJhdGVneSA9IGV4cG9ydHMuQWJzdHJhY3RNZXNzYWdlQnVmZmVyID0gZXhwb3J0cy5Xcml0ZWFibGVTdHJlYW1NZXNzYWdlV3JpdGVyID0gZXhwb3J0cy5BYnN0cmFjdE1lc3NhZ2VXcml0ZXIgPSBleHBvcnRzLk1lc3NhZ2VXcml0ZXIgPSBleHBvcnRzLlJlYWRhYmxlU3RyZWFtTWVzc2FnZVJlYWRlciA9IGV4cG9ydHMuQWJzdHJhY3RNZXNzYWdlUmVhZGVyID0gZXhwb3J0cy5NZXNzYWdlUmVhZGVyID0gZXhwb3J0cy5TaGFyZWRBcnJheVJlY2VpdmVyU3RyYXRlZ3kgPSBleHBvcnRzLlNoYXJlZEFycmF5U2VuZGVyU3RyYXRlZ3kgPSBleHBvcnRzLkNhbmNlbGxhdGlvblRva2VuID0gZXhwb3J0cy5DYW5jZWxsYXRpb25Ub2tlblNvdXJjZSA9IGV4cG9ydHMuRW1pdHRlciA9IGV4cG9ydHMuRXZlbnQgPSBleHBvcnRzLkRpc3Bvc2FibGUgPSBleHBvcnRzLkxSVUNhY2hlID0gZXhwb3J0cy5Ub3VjaCA9IGV4cG9ydHMuTGlua2VkTWFwID0gZXhwb3J0cy5QYXJhbWV0ZXJTdHJ1Y3R1cmVzID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlOSA9IGV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTggPSBleHBvcnRzLk5vdGlmaWNhdGlvblR5cGU3ID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlNiA9IGV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTUgPSBleHBvcnRzLk5vdGlmaWNhdGlvblR5cGU0ID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlMyA9IGV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZTIgPSBleHBvcnRzLk5vdGlmaWNhdGlvblR5cGUxID0gZXhwb3J0cy5Ob3RpZmljYXRpb25UeXBlMCA9IGV4cG9ydHMuTm90aWZpY2F0aW9uVHlwZSA9IGV4cG9ydHMuRXJyb3JDb2RlcyA9IGV4cG9ydHMuUmVzcG9uc2VFcnJvciA9IGV4cG9ydHMuUmVxdWVzdFR5cGU5ID0gZXhwb3J0cy5SZXF1ZXN0VHlwZTggPSBleHBvcnRzLlJlcXVlc3RUeXBlNyA9IGV4cG9ydHMuUmVxdWVzdFR5cGU2ID0gZXhwb3J0cy5SZXF1ZXN0VHlwZTUgPSBleHBvcnRzLlJlcXVlc3RUeXBlNCA9IGV4cG9ydHMuUmVxdWVzdFR5cGUzID0gZXhwb3J0cy5SZXF1ZXN0VHlwZTIgPSBleHBvcnRzLlJlcXVlc3RUeXBlMSA9IGV4cG9ydHMuUmVxdWVzdFR5cGUwID0gZXhwb3J0cy5SZXF1ZXN0VHlwZSA9IGV4cG9ydHMuTWVzc2FnZSA9IGV4cG9ydHMuUkFMID0gdm9pZCAwO1xuZXhwb3J0cy5NZXNzYWdlU3RyYXRlZ3kgPSBleHBvcnRzLkNhbmNlbGxhdGlvblN0cmF0ZWd5ID0gZXhwb3J0cy5DYW5jZWxsYXRpb25TZW5kZXJTdHJhdGVneSA9IGV4cG9ydHMuQ2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneSA9IGV4cG9ydHMuQ29ubmVjdGlvbkVycm9yID0gZXhwb3J0cy5Db25uZWN0aW9uRXJyb3JzID0gZXhwb3J0cy5Mb2dUcmFjZU5vdGlmaWNhdGlvbiA9IGV4cG9ydHMuU2V0VHJhY2VOb3RpZmljYXRpb24gPSBleHBvcnRzLlRyYWNlRm9ybWF0ID0gZXhwb3J0cy5UcmFjZVZhbHVlcyA9IGV4cG9ydHMuVHJhY2UgPSB2b2lkIDA7XG5jb25zdCBtZXNzYWdlc18xID0gcmVxdWlyZShcIi4vbWVzc2FnZXNcIik7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJNZXNzYWdlXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLk1lc3NhZ2U7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJSZXF1ZXN0VHlwZVwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5SZXF1ZXN0VHlwZTsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlJlcXVlc3RUeXBlMFwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5SZXF1ZXN0VHlwZTA7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJSZXF1ZXN0VHlwZTFcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VzXzEuUmVxdWVzdFR5cGUxOyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiUmVxdWVzdFR5cGUyXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLlJlcXVlc3RUeXBlMjsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlJlcXVlc3RUeXBlM1wiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5SZXF1ZXN0VHlwZTM7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJSZXF1ZXN0VHlwZTRcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VzXzEuUmVxdWVzdFR5cGU0OyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiUmVxdWVzdFR5cGU1XCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLlJlcXVlc3RUeXBlNTsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlJlcXVlc3RUeXBlNlwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5SZXF1ZXN0VHlwZTY7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJSZXF1ZXN0VHlwZTdcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VzXzEuUmVxdWVzdFR5cGU3OyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiUmVxdWVzdFR5cGU4XCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLlJlcXVlc3RUeXBlODsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlJlcXVlc3RUeXBlOVwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5SZXF1ZXN0VHlwZTk7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJSZXNwb25zZUVycm9yXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLlJlc3BvbnNlRXJyb3I7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJFcnJvckNvZGVzXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLkVycm9yQ29kZXM7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJOb3RpZmljYXRpb25UeXBlXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLk5vdGlmaWNhdGlvblR5cGU7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJOb3RpZmljYXRpb25UeXBlMFwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5Ob3RpZmljYXRpb25UeXBlMDsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIk5vdGlmaWNhdGlvblR5cGUxXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLk5vdGlmaWNhdGlvblR5cGUxOyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiTm90aWZpY2F0aW9uVHlwZTJcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VzXzEuTm90aWZpY2F0aW9uVHlwZTI7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJOb3RpZmljYXRpb25UeXBlM1wiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5Ob3RpZmljYXRpb25UeXBlMzsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIk5vdGlmaWNhdGlvblR5cGU0XCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLk5vdGlmaWNhdGlvblR5cGU0OyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiTm90aWZpY2F0aW9uVHlwZTVcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VzXzEuTm90aWZpY2F0aW9uVHlwZTU7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJOb3RpZmljYXRpb25UeXBlNlwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5Ob3RpZmljYXRpb25UeXBlNjsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIk5vdGlmaWNhdGlvblR5cGU3XCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlc18xLk5vdGlmaWNhdGlvblR5cGU3OyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiTm90aWZpY2F0aW9uVHlwZThcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VzXzEuTm90aWZpY2F0aW9uVHlwZTg7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJOb3RpZmljYXRpb25UeXBlOVwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZXNfMS5Ob3RpZmljYXRpb25UeXBlOTsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlBhcmFtZXRlclN0cnVjdHVyZXNcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VzXzEuUGFyYW1ldGVyU3RydWN0dXJlczsgfSB9KTtcbmNvbnN0IGxpbmtlZE1hcF8xID0gcmVxdWlyZShcIi4vbGlua2VkTWFwXCIpO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiTGlua2VkTWFwXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBsaW5rZWRNYXBfMS5MaW5rZWRNYXA7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJMUlVDYWNoZVwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbGlua2VkTWFwXzEuTFJVQ2FjaGU7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJUb3VjaFwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbGlua2VkTWFwXzEuVG91Y2g7IH0gfSk7XG5jb25zdCBkaXNwb3NhYmxlXzEgPSByZXF1aXJlKFwiLi9kaXNwb3NhYmxlXCIpO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiRGlzcG9zYWJsZVwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gZGlzcG9zYWJsZV8xLkRpc3Bvc2FibGU7IH0gfSk7XG5jb25zdCBldmVudHNfMSA9IHJlcXVpcmUoXCIuL2V2ZW50c1wiKTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkV2ZW50XCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBldmVudHNfMS5FdmVudDsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkVtaXR0ZXJcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGV2ZW50c18xLkVtaXR0ZXI7IH0gfSk7XG5jb25zdCBjYW5jZWxsYXRpb25fMSA9IHJlcXVpcmUoXCIuL2NhbmNlbGxhdGlvblwiKTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkNhbmNlbGxhdGlvblRva2VuU291cmNlXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjYW5jZWxsYXRpb25fMS5DYW5jZWxsYXRpb25Ub2tlblNvdXJjZTsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkNhbmNlbGxhdGlvblRva2VuXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjYW5jZWxsYXRpb25fMS5DYW5jZWxsYXRpb25Ub2tlbjsgfSB9KTtcbmNvbnN0IHNoYXJlZEFycmF5Q2FuY2VsbGF0aW9uXzEgPSByZXF1aXJlKFwiLi9zaGFyZWRBcnJheUNhbmNlbGxhdGlvblwiKTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlNoYXJlZEFycmF5U2VuZGVyU3RyYXRlZ3lcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIHNoYXJlZEFycmF5Q2FuY2VsbGF0aW9uXzEuU2hhcmVkQXJyYXlTZW5kZXJTdHJhdGVneTsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlNoYXJlZEFycmF5UmVjZWl2ZXJTdHJhdGVneVwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gc2hhcmVkQXJyYXlDYW5jZWxsYXRpb25fMS5TaGFyZWRBcnJheVJlY2VpdmVyU3RyYXRlZ3k7IH0gfSk7XG5jb25zdCBtZXNzYWdlUmVhZGVyXzEgPSByZXF1aXJlKFwiLi9tZXNzYWdlUmVhZGVyXCIpO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiTWVzc2FnZVJlYWRlclwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZVJlYWRlcl8xLk1lc3NhZ2VSZWFkZXI7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJBYnN0cmFjdE1lc3NhZ2VSZWFkZXJcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VSZWFkZXJfMS5BYnN0cmFjdE1lc3NhZ2VSZWFkZXI7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJSZWFkYWJsZVN0cmVhbU1lc3NhZ2VSZWFkZXJcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VSZWFkZXJfMS5SZWFkYWJsZVN0cmVhbU1lc3NhZ2VSZWFkZXI7IH0gfSk7XG5jb25zdCBtZXNzYWdlV3JpdGVyXzEgPSByZXF1aXJlKFwiLi9tZXNzYWdlV3JpdGVyXCIpO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiTWVzc2FnZVdyaXRlclwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gbWVzc2FnZVdyaXRlcl8xLk1lc3NhZ2VXcml0ZXI7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJBYnN0cmFjdE1lc3NhZ2VXcml0ZXJcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VXcml0ZXJfMS5BYnN0cmFjdE1lc3NhZ2VXcml0ZXI7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJXcml0ZWFibGVTdHJlYW1NZXNzYWdlV3JpdGVyXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBtZXNzYWdlV3JpdGVyXzEuV3JpdGVhYmxlU3RyZWFtTWVzc2FnZVdyaXRlcjsgfSB9KTtcbmNvbnN0IG1lc3NhZ2VCdWZmZXJfMSA9IHJlcXVpcmUoXCIuL21lc3NhZ2VCdWZmZXJcIik7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJBYnN0cmFjdE1lc3NhZ2VCdWZmZXJcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIG1lc3NhZ2VCdWZmZXJfMS5BYnN0cmFjdE1lc3NhZ2VCdWZmZXI7IH0gfSk7XG5jb25zdCBjb25uZWN0aW9uXzEgPSByZXF1aXJlKFwiLi9jb25uZWN0aW9uXCIpO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiQ29ubmVjdGlvblN0cmF0ZWd5XCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjb25uZWN0aW9uXzEuQ29ubmVjdGlvblN0cmF0ZWd5OyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiQ29ubmVjdGlvbk9wdGlvbnNcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5Db25uZWN0aW9uT3B0aW9uczsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIk51bGxMb2dnZXJcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5OdWxsTG9nZ2VyOyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiY3JlYXRlTWVzc2FnZUNvbm5lY3Rpb25cIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5jcmVhdGVNZXNzYWdlQ29ubmVjdGlvbjsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlByb2dyZXNzVG9rZW5cIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5Qcm9ncmVzc1Rva2VuOyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiUHJvZ3Jlc3NUeXBlXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjb25uZWN0aW9uXzEuUHJvZ3Jlc3NUeXBlOyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiVHJhY2VcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5UcmFjZTsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIlRyYWNlVmFsdWVzXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjb25uZWN0aW9uXzEuVHJhY2VWYWx1ZXM7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJUcmFjZUZvcm1hdFwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gY29ubmVjdGlvbl8xLlRyYWNlRm9ybWF0OyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiU2V0VHJhY2VOb3RpZmljYXRpb25cIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5TZXRUcmFjZU5vdGlmaWNhdGlvbjsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkxvZ1RyYWNlTm90aWZpY2F0aW9uXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjb25uZWN0aW9uXzEuTG9nVHJhY2VOb3RpZmljYXRpb247IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJDb25uZWN0aW9uRXJyb3JzXCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjb25uZWN0aW9uXzEuQ29ubmVjdGlvbkVycm9yczsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkNvbm5lY3Rpb25FcnJvclwiLCB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24gKCkgeyByZXR1cm4gY29ubmVjdGlvbl8xLkNvbm5lY3Rpb25FcnJvcjsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkNhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3lcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5DYW5jZWxsYXRpb25SZWNlaXZlclN0cmF0ZWd5OyB9IH0pO1xuT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydHMsIFwiQ2FuY2VsbGF0aW9uU2VuZGVyU3RyYXRlZ3lcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5DYW5jZWxsYXRpb25TZW5kZXJTdHJhdGVneTsgfSB9KTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIkNhbmNlbGxhdGlvblN0cmF0ZWd5XCIsIHsgZW51bWVyYWJsZTogdHJ1ZSwgZ2V0OiBmdW5jdGlvbiAoKSB7IHJldHVybiBjb25uZWN0aW9uXzEuQ2FuY2VsbGF0aW9uU3RyYXRlZ3k7IH0gfSk7XG5PYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0cywgXCJNZXNzYWdlU3RyYXRlZ3lcIiwgeyBlbnVtZXJhYmxlOiB0cnVlLCBnZXQ6IGZ1bmN0aW9uICgpIHsgcmV0dXJuIGNvbm5lY3Rpb25fMS5NZXNzYWdlU3RyYXRlZ3k7IH0gfSk7XG5jb25zdCByYWxfMSA9IHJlcXVpcmUoXCIuL3JhbFwiKTtcbmV4cG9ydHMuUkFMID0gcmFsXzEuZGVmYXVsdDtcbiIsICJcInVzZSBzdHJpY3RcIjtcbi8qIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiBDb3B5cmlnaHQgKGMpIE1pY3Jvc29mdCBDb3Jwb3JhdGlvbi4gQWxsIHJpZ2h0cyByZXNlcnZlZC5cbiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBNSVQgTGljZW5zZS4gU2VlIExpY2Vuc2UudHh0IGluIHRoZSBwcm9qZWN0IHJvb3QgZm9yIGxpY2Vuc2UgaW5mb3JtYXRpb24uXG4gKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0gKi9cbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIl9fZXNNb2R1bGVcIiwgeyB2YWx1ZTogdHJ1ZSB9KTtcbmNvbnN0IGFwaV8xID0gcmVxdWlyZShcIi4uL2NvbW1vbi9hcGlcIik7XG5jbGFzcyBNZXNzYWdlQnVmZmVyIGV4dGVuZHMgYXBpXzEuQWJzdHJhY3RNZXNzYWdlQnVmZmVyIHtcbiAgICBjb25zdHJ1Y3RvcihlbmNvZGluZyA9ICd1dGYtOCcpIHtcbiAgICAgICAgc3VwZXIoZW5jb2RpbmcpO1xuICAgICAgICB0aGlzLmFzY2lpRGVjb2RlciA9IG5ldyBUZXh0RGVjb2RlcignYXNjaWknKTtcbiAgICB9XG4gICAgZW1wdHlCdWZmZXIoKSB7XG4gICAgICAgIHJldHVybiBNZXNzYWdlQnVmZmVyLmVtcHR5QnVmZmVyO1xuICAgIH1cbiAgICBmcm9tU3RyaW5nKHZhbHVlLCBfZW5jb2RpbmcpIHtcbiAgICAgICAgcmV0dXJuIChuZXcgVGV4dEVuY29kZXIoKSkuZW5jb2RlKHZhbHVlKTtcbiAgICB9XG4gICAgdG9TdHJpbmcodmFsdWUsIGVuY29kaW5nKSB7XG4gICAgICAgIGlmIChlbmNvZGluZyA9PT0gJ2FzY2lpJykge1xuICAgICAgICAgICAgcmV0dXJuIHRoaXMuYXNjaWlEZWNvZGVyLmRlY29kZSh2YWx1ZSk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICByZXR1cm4gKG5ldyBUZXh0RGVjb2RlcihlbmNvZGluZykpLmRlY29kZSh2YWx1ZSk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgYXNOYXRpdmUoYnVmZmVyLCBsZW5ndGgpIHtcbiAgICAgICAgaWYgKGxlbmd0aCA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXR1cm4gYnVmZmVyO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgcmV0dXJuIGJ1ZmZlci5zbGljZSgwLCBsZW5ndGgpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGFsbG9jTmF0aXZlKGxlbmd0aCkge1xuICAgICAgICByZXR1cm4gbmV3IFVpbnQ4QXJyYXkobGVuZ3RoKTtcbiAgICB9XG59XG5NZXNzYWdlQnVmZmVyLmVtcHR5QnVmZmVyID0gbmV3IFVpbnQ4QXJyYXkoMCk7XG5jbGFzcyBSZWFkYWJsZVN0cmVhbVdyYXBwZXIge1xuICAgIGNvbnN0cnVjdG9yKHNvY2tldCkge1xuICAgICAgICB0aGlzLnNvY2tldCA9IHNvY2tldDtcbiAgICAgICAgdGhpcy5fb25EYXRhID0gbmV3IGFwaV8xLkVtaXR0ZXIoKTtcbiAgICAgICAgdGhpcy5fbWVzc2FnZUxpc3RlbmVyID0gKGV2ZW50KSA9PiB7XG4gICAgICAgICAgICBjb25zdCBibG9iID0gZXZlbnQuZGF0YTtcbiAgICAgICAgICAgIGJsb2IuYXJyYXlCdWZmZXIoKS50aGVuKChidWZmZXIpID0+IHtcbiAgICAgICAgICAgICAgICB0aGlzLl9vbkRhdGEuZmlyZShuZXcgVWludDhBcnJheShidWZmZXIpKTtcbiAgICAgICAgICAgIH0sICgpID0+IHtcbiAgICAgICAgICAgICAgICAoMCwgYXBpXzEuUkFMKSgpLmNvbnNvbGUuZXJyb3IoYENvbnZlcnRpbmcgYmxvYiB0byBhcnJheSBidWZmZXIgZmFpbGVkLmApO1xuICAgICAgICAgICAgfSk7XG4gICAgICAgIH07XG4gICAgICAgIHRoaXMuc29ja2V0LmFkZEV2ZW50TGlzdGVuZXIoJ21lc3NhZ2UnLCB0aGlzLl9tZXNzYWdlTGlzdGVuZXIpO1xuICAgIH1cbiAgICBvbkNsb3NlKGxpc3RlbmVyKSB7XG4gICAgICAgIHRoaXMuc29ja2V0LmFkZEV2ZW50TGlzdGVuZXIoJ2Nsb3NlJywgbGlzdGVuZXIpO1xuICAgICAgICByZXR1cm4gYXBpXzEuRGlzcG9zYWJsZS5jcmVhdGUoKCkgPT4gdGhpcy5zb2NrZXQucmVtb3ZlRXZlbnRMaXN0ZW5lcignY2xvc2UnLCBsaXN0ZW5lcikpO1xuICAgIH1cbiAgICBvbkVycm9yKGxpc3RlbmVyKSB7XG4gICAgICAgIHRoaXMuc29ja2V0LmFkZEV2ZW50TGlzdGVuZXIoJ2Vycm9yJywgbGlzdGVuZXIpO1xuICAgICAgICByZXR1cm4gYXBpXzEuRGlzcG9zYWJsZS5jcmVhdGUoKCkgPT4gdGhpcy5zb2NrZXQucmVtb3ZlRXZlbnRMaXN0ZW5lcignZXJyb3InLCBsaXN0ZW5lcikpO1xuICAgIH1cbiAgICBvbkVuZChsaXN0ZW5lcikge1xuICAgICAgICB0aGlzLnNvY2tldC5hZGRFdmVudExpc3RlbmVyKCdlbmQnLCBsaXN0ZW5lcik7XG4gICAgICAgIHJldHVybiBhcGlfMS5EaXNwb3NhYmxlLmNyZWF0ZSgoKSA9PiB0aGlzLnNvY2tldC5yZW1vdmVFdmVudExpc3RlbmVyKCdlbmQnLCBsaXN0ZW5lcikpO1xuICAgIH1cbiAgICBvbkRhdGEobGlzdGVuZXIpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX29uRGF0YS5ldmVudChsaXN0ZW5lcik7XG4gICAgfVxufVxuY2xhc3MgV3JpdGFibGVTdHJlYW1XcmFwcGVyIHtcbiAgICBjb25zdHJ1Y3Rvcihzb2NrZXQpIHtcbiAgICAgICAgdGhpcy5zb2NrZXQgPSBzb2NrZXQ7XG4gICAgfVxuICAgIG9uQ2xvc2UobGlzdGVuZXIpIHtcbiAgICAgICAgdGhpcy5zb2NrZXQuYWRkRXZlbnRMaXN0ZW5lcignY2xvc2UnLCBsaXN0ZW5lcik7XG4gICAgICAgIHJldHVybiBhcGlfMS5EaXNwb3NhYmxlLmNyZWF0ZSgoKSA9PiB0aGlzLnNvY2tldC5yZW1vdmVFdmVudExpc3RlbmVyKCdjbG9zZScsIGxpc3RlbmVyKSk7XG4gICAgfVxuICAgIG9uRXJyb3IobGlzdGVuZXIpIHtcbiAgICAgICAgdGhpcy5zb2NrZXQuYWRkRXZlbnRMaXN0ZW5lcignZXJyb3InLCBsaXN0ZW5lcik7XG4gICAgICAgIHJldHVybiBhcGlfMS5EaXNwb3NhYmxlLmNyZWF0ZSgoKSA9PiB0aGlzLnNvY2tldC5yZW1vdmVFdmVudExpc3RlbmVyKCdlcnJvcicsIGxpc3RlbmVyKSk7XG4gICAgfVxuICAgIG9uRW5kKGxpc3RlbmVyKSB7XG4gICAgICAgIHRoaXMuc29ja2V0LmFkZEV2ZW50TGlzdGVuZXIoJ2VuZCcsIGxpc3RlbmVyKTtcbiAgICAgICAgcmV0dXJuIGFwaV8xLkRpc3Bvc2FibGUuY3JlYXRlKCgpID0+IHRoaXMuc29ja2V0LnJlbW92ZUV2ZW50TGlzdGVuZXIoJ2VuZCcsIGxpc3RlbmVyKSk7XG4gICAgfVxuICAgIHdyaXRlKGRhdGEsIGVuY29kaW5nKSB7XG4gICAgICAgIGlmICh0eXBlb2YgZGF0YSA9PT0gJ3N0cmluZycpIHtcbiAgICAgICAgICAgIGlmIChlbmNvZGluZyAhPT0gdW5kZWZpbmVkICYmIGVuY29kaW5nICE9PSAndXRmLTgnKSB7XG4gICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBJbiBhIEJyb3dzZXIgZW52aXJvbm1lbnRzIG9ubHkgdXRmLTggdGV4dCBlbmNvZGluZyBpcyBzdXBwb3J0ZWQuIEJ1dCBnb3QgZW5jb2Rpbmc6ICR7ZW5jb2Rpbmd9YCk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICB0aGlzLnNvY2tldC5zZW5kKGRhdGEpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgdGhpcy5zb2NrZXQuc2VuZChkYXRhKTtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gUHJvbWlzZS5yZXNvbHZlKCk7XG4gICAgfVxuICAgIGVuZCgpIHtcbiAgICAgICAgdGhpcy5zb2NrZXQuY2xvc2UoKTtcbiAgICB9XG59XG5jb25zdCBfdGV4dEVuY29kZXIgPSBuZXcgVGV4dEVuY29kZXIoKTtcbmNvbnN0IF9yaWwgPSBPYmplY3QuZnJlZXplKHtcbiAgICBtZXNzYWdlQnVmZmVyOiBPYmplY3QuZnJlZXplKHtcbiAgICAgICAgY3JlYXRlOiAoZW5jb2RpbmcpID0+IG5ldyBNZXNzYWdlQnVmZmVyKGVuY29kaW5nKVxuICAgIH0pLFxuICAgIGFwcGxpY2F0aW9uSnNvbjogT2JqZWN0LmZyZWV6ZSh7XG4gICAgICAgIGVuY29kZXI6IE9iamVjdC5mcmVlemUoe1xuICAgICAgICAgICAgbmFtZTogJ2FwcGxpY2F0aW9uL2pzb24nLFxuICAgICAgICAgICAgZW5jb2RlOiAobXNnLCBvcHRpb25zKSA9PiB7XG4gICAgICAgICAgICAgICAgaWYgKG9wdGlvbnMuY2hhcnNldCAhPT0gJ3V0Zi04Jykge1xuICAgICAgICAgICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYEluIGEgQnJvd3NlciBlbnZpcm9ubWVudHMgb25seSB1dGYtOCB0ZXh0IGVuY29kaW5nIGlzIHN1cHBvcnRlZC4gQnV0IGdvdCBlbmNvZGluZzogJHtvcHRpb25zLmNoYXJzZXR9YCk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIHJldHVybiBQcm9taXNlLnJlc29sdmUoX3RleHRFbmNvZGVyLmVuY29kZShKU09OLnN0cmluZ2lmeShtc2csIHVuZGVmaW5lZCwgMCkpKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSksXG4gICAgICAgIGRlY29kZXI6IE9iamVjdC5mcmVlemUoe1xuICAgICAgICAgICAgbmFtZTogJ2FwcGxpY2F0aW9uL2pzb24nLFxuICAgICAgICAgICAgZGVjb2RlOiAoYnVmZmVyLCBvcHRpb25zKSA9PiB7XG4gICAgICAgICAgICAgICAgaWYgKCEoYnVmZmVyIGluc3RhbmNlb2YgVWludDhBcnJheSkpIHtcbiAgICAgICAgICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBJbiBhIEJyb3dzZXIgZW52aXJvbm1lbnRzIG9ubHkgVWludDhBcnJheXMgYXJlIHN1cHBvcnRlZC5gKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgcmV0dXJuIFByb21pc2UucmVzb2x2ZShKU09OLnBhcnNlKG5ldyBUZXh0RGVjb2RlcihvcHRpb25zLmNoYXJzZXQpLmRlY29kZShidWZmZXIpKSk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0pXG4gICAgfSksXG4gICAgc3RyZWFtOiBPYmplY3QuZnJlZXplKHtcbiAgICAgICAgYXNSZWFkYWJsZVN0cmVhbTogKHNvY2tldCkgPT4gbmV3IFJlYWRhYmxlU3RyZWFtV3JhcHBlcihzb2NrZXQpLFxuICAgICAgICBhc1dyaXRhYmxlU3RyZWFtOiAoc29ja2V0KSA9PiBuZXcgV3JpdGFibGVTdHJlYW1XcmFwcGVyKHNvY2tldClcbiAgICB9KSxcbiAgICBjb25zb2xlOiBjb25zb2xlLFxuICAgIHRpbWVyOiBPYmplY3QuZnJlZXplKHtcbiAgICAgICAgc2V0VGltZW91dChjYWxsYmFjaywgbXMsIC4uLmFyZ3MpIHtcbiAgICAgICAgICAgIGNvbnN0IGhhbmRsZSA9IHNldFRpbWVvdXQoY2FsbGJhY2ssIG1zLCAuLi5hcmdzKTtcbiAgICAgICAgICAgIHJldHVybiB7IGRpc3Bvc2U6ICgpID0+IGNsZWFyVGltZW91dChoYW5kbGUpIH07XG4gICAgICAgIH0sXG4gICAgICAgIHNldEltbWVkaWF0ZShjYWxsYmFjaywgLi4uYXJncykge1xuICAgICAgICAgICAgY29uc3QgaGFuZGxlID0gc2V0VGltZW91dChjYWxsYmFjaywgMCwgLi4uYXJncyk7XG4gICAgICAgICAgICByZXR1cm4geyBkaXNwb3NlOiAoKSA9PiBjbGVhclRpbWVvdXQoaGFuZGxlKSB9O1xuICAgICAgICB9LFxuICAgICAgICBzZXRJbnRlcnZhbChjYWxsYmFjaywgbXMsIC4uLmFyZ3MpIHtcbiAgICAgICAgICAgIGNvbnN0IGhhbmRsZSA9IHNldEludGVydmFsKGNhbGxiYWNrLCBtcywgLi4uYXJncyk7XG4gICAgICAgICAgICByZXR1cm4geyBkaXNwb3NlOiAoKSA9PiBjbGVhckludGVydmFsKGhhbmRsZSkgfTtcbiAgICAgICAgfSxcbiAgICB9KVxufSk7XG5mdW5jdGlvbiBSSUwoKSB7XG4gICAgcmV0dXJuIF9yaWw7XG59XG4oZnVuY3Rpb24gKFJJTCkge1xuICAgIGZ1bmN0aW9uIGluc3RhbGwoKSB7XG4gICAgICAgIGFwaV8xLlJBTC5pbnN0YWxsKF9yaWwpO1xuICAgIH1cbiAgICBSSUwuaW5zdGFsbCA9IGluc3RhbGw7XG59KShSSUwgfHwgKFJJTCA9IHt9KSk7XG5leHBvcnRzLmRlZmF1bHQgPSBSSUw7XG4iLCAiXCJ1c2Ugc3RyaWN0XCI7XG4vKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSBNaWNyb3NvZnQgQ29ycG9yYXRpb24uIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMaWNlbnNlLnR4dCBpbiB0aGUgcHJvamVjdCByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG52YXIgX19jcmVhdGVCaW5kaW5nID0gKHRoaXMgJiYgdGhpcy5fX2NyZWF0ZUJpbmRpbmcpIHx8IChPYmplY3QuY3JlYXRlID8gKGZ1bmN0aW9uKG8sIG0sIGssIGsyKSB7XG4gICAgaWYgKGsyID09PSB1bmRlZmluZWQpIGsyID0gaztcbiAgICB2YXIgZGVzYyA9IE9iamVjdC5nZXRPd25Qcm9wZXJ0eURlc2NyaXB0b3IobSwgayk7XG4gICAgaWYgKCFkZXNjIHx8IChcImdldFwiIGluIGRlc2MgPyAhbS5fX2VzTW9kdWxlIDogZGVzYy53cml0YWJsZSB8fCBkZXNjLmNvbmZpZ3VyYWJsZSkpIHtcbiAgICAgIGRlc2MgPSB7IGVudW1lcmFibGU6IHRydWUsIGdldDogZnVuY3Rpb24oKSB7IHJldHVybiBtW2tdOyB9IH07XG4gICAgfVxuICAgIE9iamVjdC5kZWZpbmVQcm9wZXJ0eShvLCBrMiwgZGVzYyk7XG59KSA6IChmdW5jdGlvbihvLCBtLCBrLCBrMikge1xuICAgIGlmIChrMiA9PT0gdW5kZWZpbmVkKSBrMiA9IGs7XG4gICAgb1trMl0gPSBtW2tdO1xufSkpO1xudmFyIF9fZXhwb3J0U3RhciA9ICh0aGlzICYmIHRoaXMuX19leHBvcnRTdGFyKSB8fCBmdW5jdGlvbihtLCBleHBvcnRzKSB7XG4gICAgZm9yICh2YXIgcCBpbiBtKSBpZiAocCAhPT0gXCJkZWZhdWx0XCIgJiYgIU9iamVjdC5wcm90b3R5cGUuaGFzT3duUHJvcGVydHkuY2FsbChleHBvcnRzLCBwKSkgX19jcmVhdGVCaW5kaW5nKGV4cG9ydHMsIG0sIHApO1xufTtcbk9iamVjdC5kZWZpbmVQcm9wZXJ0eShleHBvcnRzLCBcIl9fZXNNb2R1bGVcIiwgeyB2YWx1ZTogdHJ1ZSB9KTtcbmV4cG9ydHMuY3JlYXRlTWVzc2FnZUNvbm5lY3Rpb24gPSBleHBvcnRzLkJyb3dzZXJNZXNzYWdlV3JpdGVyID0gZXhwb3J0cy5Ccm93c2VyTWVzc2FnZVJlYWRlciA9IHZvaWQgMDtcbmNvbnN0IHJpbF8xID0gcmVxdWlyZShcIi4vcmlsXCIpO1xuLy8gSW5zdGFsbCB0aGUgYnJvd3NlciBydW50aW1lIGFic3RyYWN0LlxucmlsXzEuZGVmYXVsdC5pbnN0YWxsKCk7XG5jb25zdCBhcGlfMSA9IHJlcXVpcmUoXCIuLi9jb21tb24vYXBpXCIpO1xuX19leHBvcnRTdGFyKHJlcXVpcmUoXCIuLi9jb21tb24vYXBpXCIpLCBleHBvcnRzKTtcbmNsYXNzIEJyb3dzZXJNZXNzYWdlUmVhZGVyIGV4dGVuZHMgYXBpXzEuQWJzdHJhY3RNZXNzYWdlUmVhZGVyIHtcbiAgICBjb25zdHJ1Y3Rvcihwb3J0KSB7XG4gICAgICAgIHN1cGVyKCk7XG4gICAgICAgIHRoaXMuX29uRGF0YSA9IG5ldyBhcGlfMS5FbWl0dGVyKCk7XG4gICAgICAgIHRoaXMuX21lc3NhZ2VMaXN0ZW5lciA9IChldmVudCkgPT4ge1xuICAgICAgICAgICAgdGhpcy5fb25EYXRhLmZpcmUoZXZlbnQuZGF0YSk7XG4gICAgICAgIH07XG4gICAgICAgIHBvcnQuYWRkRXZlbnRMaXN0ZW5lcignZXJyb3InLCAoZXZlbnQpID0+IHRoaXMuZmlyZUVycm9yKGV2ZW50KSk7XG4gICAgICAgIHBvcnQub25tZXNzYWdlID0gdGhpcy5fbWVzc2FnZUxpc3RlbmVyO1xuICAgIH1cbiAgICBsaXN0ZW4oY2FsbGJhY2spIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX29uRGF0YS5ldmVudChjYWxsYmFjayk7XG4gICAgfVxufVxuZXhwb3J0cy5Ccm93c2VyTWVzc2FnZVJlYWRlciA9IEJyb3dzZXJNZXNzYWdlUmVhZGVyO1xuY2xhc3MgQnJvd3Nlck1lc3NhZ2VXcml0ZXIgZXh0ZW5kcyBhcGlfMS5BYnN0cmFjdE1lc3NhZ2VXcml0ZXIge1xuICAgIGNvbnN0cnVjdG9yKHBvcnQpIHtcbiAgICAgICAgc3VwZXIoKTtcbiAgICAgICAgdGhpcy5wb3J0ID0gcG9ydDtcbiAgICAgICAgdGhpcy5lcnJvckNvdW50ID0gMDtcbiAgICAgICAgcG9ydC5hZGRFdmVudExpc3RlbmVyKCdlcnJvcicsIChldmVudCkgPT4gdGhpcy5maXJlRXJyb3IoZXZlbnQpKTtcbiAgICB9XG4gICAgd3JpdGUobXNnKSB7XG4gICAgICAgIHRyeSB7XG4gICAgICAgICAgICB0aGlzLnBvcnQucG9zdE1lc3NhZ2UobXNnKTtcbiAgICAgICAgICAgIHJldHVybiBQcm9taXNlLnJlc29sdmUoKTtcbiAgICAgICAgfVxuICAgICAgICBjYXRjaCAoZXJyb3IpIHtcbiAgICAgICAgICAgIHRoaXMuaGFuZGxlRXJyb3IoZXJyb3IsIG1zZyk7XG4gICAgICAgICAgICByZXR1cm4gUHJvbWlzZS5yZWplY3QoZXJyb3IpO1xuICAgICAgICB9XG4gICAgfVxuICAgIGhhbmRsZUVycm9yKGVycm9yLCBtc2cpIHtcbiAgICAgICAgdGhpcy5lcnJvckNvdW50Kys7XG4gICAgICAgIHRoaXMuZmlyZUVycm9yKGVycm9yLCBtc2csIHRoaXMuZXJyb3JDb3VudCk7XG4gICAgfVxuICAgIGVuZCgpIHtcbiAgICB9XG59XG5leHBvcnRzLkJyb3dzZXJNZXNzYWdlV3JpdGVyID0gQnJvd3Nlck1lc3NhZ2VXcml0ZXI7XG5mdW5jdGlvbiBjcmVhdGVNZXNzYWdlQ29ubmVjdGlvbihyZWFkZXIsIHdyaXRlciwgbG9nZ2VyLCBvcHRpb25zKSB7XG4gICAgaWYgKGxvZ2dlciA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgIGxvZ2dlciA9IGFwaV8xLk51bGxMb2dnZXI7XG4gICAgfVxuICAgIGlmIChhcGlfMS5Db25uZWN0aW9uU3RyYXRlZ3kuaXMob3B0aW9ucykpIHtcbiAgICAgICAgb3B0aW9ucyA9IHsgY29ubmVjdGlvblN0cmF0ZWd5OiBvcHRpb25zIH07XG4gICAgfVxuICAgIHJldHVybiAoMCwgYXBpXzEuY3JlYXRlTWVzc2FnZUNvbm5lY3Rpb24pKHJlYWRlciwgd3JpdGVyLCBsb2dnZXIsIG9wdGlvbnMpO1xufVxuZXhwb3J0cy5jcmVhdGVNZXNzYWdlQ29ubmVjdGlvbiA9IGNyZWF0ZU1lc3NhZ2VDb25uZWN0aW9uO1xuIiwgIi8qIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiBDb3B5cmlnaHQgKGMpIE1pY3Jvc29mdCBDb3Jwb3JhdGlvbi4gQWxsIHJpZ2h0cyByZXNlcnZlZC5cbiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBNSVQgTGljZW5zZS4gU2VlIExpY2Vuc2UudHh0IGluIHRoZSBwcm9qZWN0IHJvb3QgZm9yIGxpY2Vuc2UgaW5mb3JtYXRpb24uXG4gKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSAqL1xuJ3VzZSBzdHJpY3QnO1xuXG5tb2R1bGUuZXhwb3J0cyA9IHJlcXVpcmUoJy4vbGliL2Jyb3dzZXIvbWFpbicpOyIsICIvKlxuICogQ29weXJpZ2h0IChjKSAyMDEwLTIwMjYgRWNsaXBzZSBEaXJpZ2libGUgY29udHJpYnV0b3JzXG4gKlxuICogQWxsIHJpZ2h0cyByZXNlcnZlZC4gVGhpcyBwcm9ncmFtIGFuZCB0aGUgYWNjb21wYW55aW5nIG1hdGVyaWFscyBhcmUgbWFkZSBhdmFpbGFibGUgdW5kZXIgdGhlXG4gKiB0ZXJtcyBvZiB0aGUgRWNsaXBzZSBQdWJsaWMgTGljZW5zZSB2Mi4wIHdoaWNoIGFjY29tcGFuaWVzIHRoaXMgZGlzdHJpYnV0aW9uLCBhbmQgaXMgYXZhaWxhYmxlIGF0XG4gKiBodHRwOi8vd3d3LmVjbGlwc2Uub3JnL2xlZ2FsL2VwbC12MjAuaHRtbFxuICpcbiAqIFNQRFgtRmlsZUNvcHlyaWdodFRleHQ6IEVjbGlwc2UgRGlyaWdpYmxlIGNvbnRyaWJ1dG9ycyBTUERYLUxpY2Vuc2UtSWRlbnRpZmllcjogRVBMLTIuMFxuICovXG5cbi8qKlxuICogSmF2YSBMU1AgY2xpZW50IGJ1bmRsZS5cbiAqXG4gKiBVc2VzIHZzY29kZS13cy1qc29ucnBjIGZvciB0eXBlZCBKU09OLVJQQyBvdmVyIFdlYlNvY2tldCBhbmQgcmVnaXN0ZXJzIE1vbmFjb1xuICogcHJvdmlkZXJzIHRoYXQgZGVsZWdhdGUgdG8gSkRULkxTLiBFeHBvc2VkIGFzIGdsb2JhbCBKYXZhTHNwQ2xpZW50TGliIHNvIHRoYXRcbiAqIGVkaXRvci5qcyBjYW4gY2FsbCBKYXZhTHNwQ2xpZW50TGliLmNvbm5lY3QocmVzb3VyY2VQYXRoKS5cbiAqXG4gKiBPbmUgSkRULkxTIHByb2Nlc3MgY292ZXJzIHRoZSBlbnRpcmUgd29ya3NwYWNlLCBzbyBhIHNpbmdsZSBXZWJTb2NrZXQgY29ubmVjdGlvblxuICogaXMgc2hhcmVkIGFjcm9zcyBhbGwgSmF2YSBmaWxlcyBvcGVuIGluIHRoZSBzYW1lIGJyb3dzZXIgcGFnZS4gVGhlIGNvbm5lY3Rpb24gaXNcbiAqIGVzdGFibGlzaGVkIG9uIHRoZSBmaXJzdCBjb25uZWN0KCkgY2FsbCBhbmQgcmV1c2VkIGZvciBhbGwgc3Vic2VxdWVudCBjYWxscy5cbiAqXG4gKiBlZGl0b3IuanMgc2V0cyB3aW5kb3cubW9uYWNvIGJlZm9yZSBjYWxsaW5nIGNvbm5lY3QoKSwgc28gdGhlIG1vbmFjby1zaGltJ3MgbGF6eVxuICogUHJveGllcyByZXNvbHZlIGNvcnJlY3RseSBhdCBjYWxsIHRpbWUuXG4gKi9cblxuaW1wb3J0IHsgY3JlYXRlTWVzc2FnZUNvbm5lY3Rpb24sIE1lc3NhZ2VDb25uZWN0aW9uIH0gZnJvbSAndnNjb2RlLWpzb25ycGMvYnJvd3Nlcic7XG5pbXBvcnQgeyB0b1NvY2tldCwgV2ViU29ja2V0TWVzc2FnZVJlYWRlciwgV2ViU29ja2V0TWVzc2FnZVdyaXRlciB9IGZyb20gJ3ZzY29kZS13cy1qc29ucnBjJztcbmltcG9ydCAqIGFzIG1vbmFjbyBmcm9tICdtb25hY28tZWRpdG9yJztcbmltcG9ydCB7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLFxuICAgIERpYWdub3N0aWNTZXZlcml0eSxcbiAgICBJbnNlcnRUZXh0Rm9ybWF0LFxuICAgIE1hcmt1cENvbnRlbnQsXG4gICAgTWFya3VwS2luZCxcbiAgICB0eXBlIENvZGVBY3Rpb24sXG4gICAgdHlwZSBDb21tYW5kLFxuICAgIHR5cGUgQ29tcGxldGlvbkl0ZW0sXG4gICAgdHlwZSBDb21wbGV0aW9uTGlzdCxcbiAgICB0eXBlIERpYWdub3N0aWMsXG4gICAgdHlwZSBIb3ZlcixcbiAgICB0eXBlIExvY2F0aW9uLFxuICAgIHR5cGUgUGFyYW1ldGVySW5mb3JtYXRpb24sXG4gICAgdHlwZSBTaWduYXR1cmVIZWxwLFxuICAgIHR5cGUgU2lnbmF0dXJlSW5mb3JtYXRpb24sXG4gICAgdHlwZSBUZXh0RWRpdCxcbiAgICB0eXBlIFdvcmtzcGFjZUVkaXQsXG59IGZyb20gJ3ZzY29kZS1sYW5ndWFnZXNlcnZlci10eXBlcyc7XG5cbi8vIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbi8vIFNpbmdsZXRvbiBzdGF0ZVxuLy8gLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuXG4vKiogU2hhcmVkIGNvbm5lY3Rpb24gXHUyMDE0IG9uZSBwZXIgYnJvd3NlciBwYWdlLCBjb3ZlcmluZyBhbGwgcHJvamVjdHMgaW4gdGhlIHdvcmtzcGFjZS4gKi9cbmxldCBfY29ubjogTWVzc2FnZUNvbm5lY3Rpb24gfCBudWxsID0gbnVsbDtcblxuLyoqXG4gKiBWaXJ0dWFsIHdvcmtzcGFjZSByb290IFVSSSwgZS5nLiB7QGNvZGUgZmlsZTovLy93b3Jrc3BhY2Uvd29ya3NwYWNlL30uXG4gKiBTZXQgb25jZSBvbiBmaXJzdCBjb25uZWN0KCk7IHVzZWQgdG8gc2NvcGUgTW9uYWNvIHByb3ZpZGVycyB0byB3b3Jrc3BhY2UgZmlsZXMuXG4gKi9cbmxldCBfd29ya3NwYWNlUm9vdCA9ICcnO1xuXG4vKiogVVJJcyBmb3Igd2hpY2ggdGV4dERvY3VtZW50L2RpZE9wZW4gaGFzIGFscmVhZHkgYmVlbiBzZW50LiAqL1xuY29uc3QgX29wZW5GaWxlczogU2V0PHN0cmluZz4gPSBuZXcgU2V0KCk7XG5cbi8qKiBQZW5kaW5nIGRlYm91bmNlZCBkaWRDaGFuZ2UgdGltZXJzIHBlciBmaWxlIFVSSSwgc28gYSBjaGFuZ2UgY2FuIGJlIGZsdXNoZWQgYmVmb3JlIGNvbXBsZXRpb24uICovXG5jb25zdCBfY2hhbmdlVGltZXJzOiBNYXA8c3RyaW5nLCBSZXR1cm5UeXBlPHR5cGVvZiBzZXRUaW1lb3V0Pj4gPSBuZXcgTWFwKCk7XG5cbi8qKlxuICogTGFzdCBkaWFnbm9zdGljcyBwdWJsaXNoZWQgcGVyIGZpbGUgVVJJLCBrZXB0IHZlcmJhdGltICh3aXRoIHRoZWlyIExTUCBjb2RlL2RhdGEpLiBDb2RlLWFjdGlvblxuICogcmVxdWVzdHMgbXVzdCBzZW5kIHRoZXNlIFx1MjAxNCBKRFQuTFMgbWF0Y2hlcyBxdWljay1maXhlcyBieSB0aGUgZGlhZ25vc3RpYydzIGNvZGUvZGF0YSwgd2hpY2ggTW9uYWNvJ3NcbiAqIElNYXJrZXJEYXRhIGNhbm5vdCByb3VuZC10cmlwLCBzbyByZWNvbnN0cnVjdGluZyBkaWFnbm9zdGljcyBmcm9tIG1hcmtlcnMgeWllbGRzIG5vIHF1aWNrLWZpeGVzLlxuICovXG5jb25zdCBfZGlhZ25vc3RpY3M6IE1hcDxzdHJpbmcsIERpYWdub3N0aWNbXT4gPSBuZXcgTWFwKCk7XG5cbmxldCBfcHJvdmlkZXJzUmVnaXN0ZXJlZCA9IGZhbHNlO1xuXG4vKiogU2VtYW50aWMtdG9rZW4gbGVnZW5kIHJlcG9ydGVkIGJ5IEpEVC5MUyBpbiB0aGUgaW5pdGlhbGl6ZSByZXN1bHQ7IG5lZWRlZCB0byBkZWNvZGUgdG9rZW4gZGF0YS4gKi9cbmxldCBfc2VtYW50aWNUb2tlbnNMZWdlbmQ6IHsgdG9rZW5UeXBlczogc3RyaW5nW107IHRva2VuTW9kaWZpZXJzOiBzdHJpbmdbXSB9IHwgbnVsbCA9IG51bGw7XG5cbi8vIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbi8vIFB1YmxpYyBBUElcbi8vIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cblxuLyoqIENhbGxlZCBieSBlZGl0b3IuanMgd2hlbiBhIEphdmEgZmlsZSBpcyBvcGVuZWQuIFNhZmUgdG8gY2FsbCBtdWx0aXBsZSB0aW1lcy4gKi9cbmV4cG9ydCBhc3luYyBmdW5jdGlvbiBjb25uZWN0KHJlc291cmNlUGF0aDogc3RyaW5nKTogUHJvbWlzZTx2b2lkPiB7XG4gICAgY29uc3QgcGFydHMgPSByZXNvdXJjZVBhdGgucmVwbGFjZSgvXlxcLy8sICcnKS5zcGxpdCgnLycpO1xuICAgIGNvbnN0IHdvcmtzcGFjZSA9IHBhcnRzWzBdO1xuICAgIGNvbnN0IHByb2plY3QgICA9IHBhcnRzWzFdO1xuICAgIGNvbnN0IGZpbGVVcmkgPSBgZmlsZTovLy93b3Jrc3BhY2UvJHt3b3Jrc3BhY2V9LyR7cHJvamVjdH0vJHtwYXJ0cy5zbGljZSgyKS5qb2luKCcvJyl9YDtcblxuICAgIGlmIChfY29ubikge1xuICAgICAgICAvLyBDb25uZWN0aW9uIGFscmVhZHkgZXN0YWJsaXNoZWQgXHUyMDE0IGp1c3Qgb3BlbiB0aGUgbmV3IGZpbGUuXG4gICAgICAgIG9wZW5GaWxlKGZpbGVVcmkpO1xuICAgICAgICByZXR1cm47XG4gICAgfVxuXG4gICAgX3dvcmtzcGFjZVJvb3QgPSBgZmlsZTovLy93b3Jrc3BhY2UvJHt3b3Jrc3BhY2V9L2A7XG5cbiAgICBjb25zdCBwcm90byA9IGxvY2F0aW9uLnByb3RvY29sID09PSAnaHR0cHM6JyA/ICd3c3MnIDogJ3dzJztcbiAgICBjb25zdCB3c1VybCA9IGAke3Byb3RvfTovLyR7bG9jYXRpb24uaG9zdH0vd2Vic29ja2V0cy9pZGUvamF2YS1sc3BgXG4gICAgICAgICAgICAgICAgKyBgP3dvcmtzcGFjZT0ke2VuY29kZVVSSUNvbXBvbmVudCh3b3Jrc3BhY2UpfWA7XG5cbiAgICBjb25zdCB3cyA9IG5ldyBXZWJTb2NrZXQod3NVcmwpO1xuICAgIGF3YWl0IG5ldyBQcm9taXNlPHZvaWQ+KChyZXNvbHZlLCByZWplY3QpID0+IHtcbiAgICAgICAgd3Mub25vcGVuICA9ICgpID0+IHJlc29sdmUoKTtcbiAgICAgICAgd3Mub25lcnJvciA9ICgpID0+IHJlamVjdChuZXcgRXJyb3IoYFtqYXZhLWxzcF0gV2ViU29ja2V0IGNvbm5lY3QgZmFpbGVkOiAke3dzVXJsfWApKTtcbiAgICB9KTtcblxuICAgIGNvbnN0IHNvY2tldCA9IHRvU29ja2V0KHdzKTtcbiAgICBjb25zdCByZWFkZXIgPSBuZXcgV2ViU29ja2V0TWVzc2FnZVJlYWRlcihzb2NrZXQpO1xuICAgIGNvbnN0IHdyaXRlciA9IG5ldyBXZWJTb2NrZXRNZXNzYWdlV3JpdGVyKHNvY2tldCk7XG4gICAgX2Nvbm4gPSBjcmVhdGVNZXNzYWdlQ29ubmVjdGlvbihyZWFkZXIsIHdyaXRlcik7XG5cbiAgICAvLyBEaWFnbm9zdGljcyBub3RpZmljYXRpb24gXHUyMTkyIE1vbmFjbyBtYXJrZXJzIChhcHBsaWVzIHRvIGFueSB3b3Jrc3BhY2UgZmlsZSlcbiAgICBfY29ubi5vbk5vdGlmaWNhdGlvbigndGV4dERvY3VtZW50L3B1Ymxpc2hEaWFnbm9zdGljcycsIChwYXJhbXM6IHsgdXJpOiBzdHJpbmc7IGRpYWdub3N0aWNzOiBEaWFnbm9zdGljW10gfSkgPT4ge1xuICAgICAgICBfZGlhZ25vc3RpY3Muc2V0KHBhcmFtcy51cmksIHBhcmFtcy5kaWFnbm9zdGljcyA/PyBbXSk7XG4gICAgICAgIGNvbnN0IG1vZGVsID0gbW9uYWNvLmVkaXRvci5nZXRNb2RlbHMoKS5maW5kKG0gPT4gbS51cmkudG9TdHJpbmcoKSA9PT0gcGFyYW1zLnVyaSk7XG4gICAgICAgIGlmICghbW9kZWwpIHJldHVybjtcbiAgICAgICAgbW9uYWNvLmVkaXRvci5zZXRNb2RlbE1hcmtlcnMobW9kZWwsICdqYXZhLWxzcCcsIHBhcmFtcy5kaWFnbm9zdGljcy5tYXAoZCA9PiAoe1xuICAgICAgICAgICAgc2V2ZXJpdHk6ICAgICAgICBsc3BTZXZlcml0eShkLnNldmVyaXR5KSxcbiAgICAgICAgICAgIG1lc3NhZ2U6ICAgICAgICAgZC5tZXNzYWdlLFxuICAgICAgICAgICAgc291cmNlOiAgICAgICAgICBkLnNvdXJjZSA/PyAnamF2YScsXG4gICAgICAgICAgICBzdGFydExpbmVOdW1iZXI6IGQucmFuZ2Uuc3RhcnQubGluZSArIDEsXG4gICAgICAgICAgICBzdGFydENvbHVtbjogICAgIGQucmFuZ2Uuc3RhcnQuY2hhcmFjdGVyICsgMSxcbiAgICAgICAgICAgIGVuZExpbmVOdW1iZXI6ICAgZC5yYW5nZS5lbmQubGluZSArIDEsXG4gICAgICAgICAgICBlbmRDb2x1bW46ICAgICAgIGQucmFuZ2UuZW5kLmNoYXJhY3RlciArIDEsXG4gICAgICAgIH0pKSk7XG4gICAgfSk7XG5cbiAgICAvLyBTZXJ2ZXIgLT4gY2xpZW50IHJlcXVlc3RzIHRoYXQgZHJpdmUgcmVmYWN0b3IgLyBnZW5lcmF0ZSByZXN1bHRzIGFuZCBkeW5hbWljIHJlZ2lzdHJhdGlvbi5cbiAgICBfY29ubi5vblJlcXVlc3QoJ3dvcmtzcGFjZS9hcHBseUVkaXQnLCAocGFyYW1zOiB7IGVkaXQ6IFdvcmtzcGFjZUVkaXQgfSkgPT4ge1xuICAgICAgICBhcHBseVdvcmtzcGFjZUVkaXQocGFyYW1zLmVkaXQpO1xuICAgICAgICByZXR1cm4geyBhcHBsaWVkOiB0cnVlIH07XG4gICAgfSk7XG4gICAgX2Nvbm4ub25SZXF1ZXN0KCd3b3Jrc3BhY2UvY29uZmlndXJhdGlvbicsIChwYXJhbXM6IHsgaXRlbXM6IEFycmF5PHsgc2VjdGlvbj86IHN0cmluZyB9PiB9KSA9PlxuICAgICAgICAocGFyYW1zLml0ZW1zID8/IFtdKS5tYXAoKCkgPT4gamR0bHNTZXR0aW5ncygpLmphdmEpKTtcbiAgICBfY29ubi5vblJlcXVlc3QoJ2NsaWVudC9yZWdpc3RlckNhcGFiaWxpdHknLCAoKSA9PiBudWxsKTtcbiAgICBfY29ubi5vblJlcXVlc3QoJ2NsaWVudC91bnJlZ2lzdGVyQ2FwYWJpbGl0eScsICgpID0+IG51bGwpO1xuICAgIF9jb25uLm9uUmVxdWVzdCgnd2luZG93L3Nob3dNZXNzYWdlUmVxdWVzdCcsICgpID0+IG51bGwpO1xuICAgIF9jb25uLm9uUmVxdWVzdCgnd2luZG93L3dvcmtEb25lUHJvZ3Jlc3MvY3JlYXRlJywgKCkgPT4gbnVsbCk7XG4gICAgX2Nvbm4ub25Ob3RpZmljYXRpb24oJ3dpbmRvdy9sb2dNZXNzYWdlJywgKHA6IHsgbWVzc2FnZTogc3RyaW5nIH0pID0+IGNvbnNvbGUuZGVidWcoJ1tqYXZhLWxzcF0nLCBwPy5tZXNzYWdlKSk7XG4gICAgX2Nvbm4ub25Ob3RpZmljYXRpb24oJ3dpbmRvdy9zaG93TWVzc2FnZScsIChwOiB7IG1lc3NhZ2U6IHN0cmluZyB9KSA9PiBjb25zb2xlLmluZm8oJ1tqYXZhLWxzcF0nLCBwPy5tZXNzYWdlKSk7XG4gICAgLy8gSkRULkxTIGxhbmd1YWdlLXN0YXR1cyAvIHByb2dyZXNzIG5vdGlmaWNhdGlvbnMgXHUyMDE0IGFja25vd2xlZGdlZCBzaWxlbnRseS5cbiAgICBfY29ubi5vbk5vdGlmaWNhdGlvbignbGFuZ3VhZ2Uvc3RhdHVzJywgKCkgPT4geyAvKiBpbmRleGluZy9yZWFkeSBzdGF0dXMsIGlnbm9yZWQgKi8gfSk7XG4gICAgX2Nvbm4ub25Ob3RpZmljYXRpb24oJ2xhbmd1YWdlL3Byb2dyZXNzUmVwb3J0JywgKCkgPT4geyAvKiBidWlsZCBwcm9ncmVzcywgaWdub3JlZCAqLyB9KTtcblxuICAgIF9jb25uLmxpc3RlbigpO1xuXG4gICAgY29uc3Qgcm9vdFVyaSA9IF93b3Jrc3BhY2VSb290O1xuICAgIGNvbnN0IGluaXRSZXN1bHQ6IGFueSA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCdpbml0aWFsaXplJywge1xuICAgICAgICBwcm9jZXNzSWQ6IG51bGwsXG4gICAgICAgIHJvb3RVcmksXG4gICAgICAgIGluaXRpYWxpemF0aW9uT3B0aW9uczoge1xuICAgICAgICAgICAgc2V0dGluZ3M6IGpkdGxzU2V0dGluZ3MoKSxcbiAgICAgICAgICAgIGV4dGVuZGVkQ2xpZW50Q2FwYWJpbGl0aWVzOiB7XG4gICAgICAgICAgICAgICAgcHJvZ3Jlc3NSZXBvcnRQcm92aWRlcjogICAgICAgICAgICBmYWxzZSxcbiAgICAgICAgICAgICAgICBjbGFzc0ZpbGVDb250ZW50c1N1cHBvcnQ6ICAgICAgICAgIHRydWUsXG4gICAgICAgICAgICAgICAgcmVzb2x2ZUFkZGl0aW9uYWxUZXh0RWRpdHNTdXBwb3J0OiB0cnVlLFxuICAgICAgICAgICAgICAgIC8vIERvIE5PVCBhZHZlcnRpc2UgdGhlICpQcm9tcHRTdXBwb3J0IGZsYWdzOiB0aG9zZSBtYWtlIEpEVC5MUyByZXR1cm4gc291cmNlIGFjdGlvbnNcbiAgICAgICAgICAgICAgICAvLyAoZ2VuZXJhdGUgdG9TdHJpbmcvY29uc3RydWN0b3JzL2FjY2Vzc29ycywgb3ZlcnJpZGUvaW1wbGVtZW50LCBvcmdhbml6ZSBpbXBvcnRzKSBhc1xuICAgICAgICAgICAgICAgIC8vIGNsaWVudC1zaWRlIFwiKlByb21wdFwiIGNvbW1hbmRzIHRoZSB2c2NvZGUtamF2YSBleHRlbnNpb24gaW1wbGVtZW50cyBidXQgd2UgZG9uJ3QuXG4gICAgICAgICAgICAgICAgLy8gV2l0aCB0aGVtIG9mZiwgSkRULkxTIHJldHVybnMgdGhlIHNhbWUgYWN0aW9ucyBhcyByZXNvbHZhYmxlIFdvcmtzcGFjZUVkaXRzIG9wZXJhdGluZ1xuICAgICAgICAgICAgICAgIC8vIG9uIGFsbCBtZW1iZXJzLCB3aGljaCBhcHBseUNvZGVBY3Rpb24gcmVzb2x2ZXMgYW5kIGFwcGxpZXMgZGlyZWN0bHkuXG4gICAgICAgICAgICAgICAgaW5mZXJTZWxlY3Rpb25TdXBwb3J0OiAgICAgICAgICAgICBbJ2V4dHJhY3RNZXRob2QnLCAnZXh0cmFjdFZhcmlhYmxlJywgJ2V4dHJhY3RGaWVsZCddLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgfSxcbiAgICAgICAgd29ya3NwYWNlRm9sZGVyczogW3sgdXJpOiByb290VXJpLCBuYW1lOiB3b3Jrc3BhY2UgfV0sXG4gICAgICAgIGNhcGFiaWxpdGllczoge1xuICAgICAgICAgICAgdGV4dERvY3VtZW50OiB7XG4gICAgICAgICAgICAgICAgc3luY2hyb25pemF0aW9uOiB7IGR5bmFtaWNSZWdpc3RyYXRpb246IHRydWUsIHdpbGxTYXZlOiBmYWxzZSwgZGlkU2F2ZTogdHJ1ZSwgd2lsbFNhdmVXYWl0VW50aWw6IGZhbHNlIH0sXG4gICAgICAgICAgICAgICAgY29tcGxldGlvbjoge1xuICAgICAgICAgICAgICAgICAgICBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlLFxuICAgICAgICAgICAgICAgICAgICBjb21wbGV0aW9uSXRlbToge1xuICAgICAgICAgICAgICAgICAgICAgICAgc25pcHBldFN1cHBvcnQ6ICAgICAgICB0cnVlLFxuICAgICAgICAgICAgICAgICAgICAgICAgZG9jdW1lbnRhdGlvbkZvcm1hdDogICBbJ21hcmtkb3duJywgJ3BsYWludGV4dCddLFxuICAgICAgICAgICAgICAgICAgICAgICAgZGVwcmVjYXRlZFN1cHBvcnQ6ICAgICB0cnVlLFxuICAgICAgICAgICAgICAgICAgICAgICAgY29tbWl0Q2hhcmFjdGVyc1N1cHBvcnQ6IHRydWUsXG4gICAgICAgICAgICAgICAgICAgICAgICByZXNvbHZlU3VwcG9ydDogICAgICAgIHsgcHJvcGVydGllczogWydkb2N1bWVudGF0aW9uJywgJ2RldGFpbCcsICdhZGRpdGlvbmFsVGV4dEVkaXRzJ10gfSxcbiAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgY29udGV4dFN1cHBvcnQ6IHRydWUsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICBob3ZlcjogICAgICAgICAgeyBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlLCBjb250ZW50Rm9ybWF0OiBbJ21hcmtkb3duJywgJ3BsYWludGV4dCddIH0sXG4gICAgICAgICAgICAgICAgc2lnbmF0dXJlSGVscDogIHsgZHluYW1pY1JlZ2lzdHJhdGlvbjogdHJ1ZSwgc2lnbmF0dXJlSW5mb3JtYXRpb246IHsgZG9jdW1lbnRhdGlvbkZvcm1hdDogWydtYXJrZG93bicsICdwbGFpbnRleHQnXSwgcGFyYW1ldGVySW5mb3JtYXRpb246IHsgbGFiZWxPZmZzZXRTdXBwb3J0OiB0cnVlIH0gfSB9LFxuICAgICAgICAgICAgICAgIGRlZmluaXRpb246ICAgICB7IGR5bmFtaWNSZWdpc3RyYXRpb246IHRydWUgfSxcbiAgICAgICAgICAgICAgICByZWZlcmVuY2VzOiAgICAgeyBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlIH0sXG4gICAgICAgICAgICAgICAgaW1wbGVtZW50YXRpb246IHsgZHluYW1pY1JlZ2lzdHJhdGlvbjogdHJ1ZSB9LFxuICAgICAgICAgICAgICAgIHR5cGVEZWZpbml0aW9uOiB7IGR5bmFtaWNSZWdpc3RyYXRpb246IHRydWUgfSxcbiAgICAgICAgICAgICAgICBkb2N1bWVudEhpZ2hsaWdodDogeyBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlIH0sXG4gICAgICAgICAgICAgICAgZG9jdW1lbnRTeW1ib2w6IHsgZHluYW1pY1JlZ2lzdHJhdGlvbjogdHJ1ZSwgaGllcmFyY2hpY2FsRG9jdW1lbnRTeW1ib2xTdXBwb3J0OiB0cnVlIH0sXG4gICAgICAgICAgICAgICAgZm9sZGluZ1JhbmdlOiAgIHsgZHluYW1pY1JlZ2lzdHJhdGlvbjogdHJ1ZSwgbGluZUZvbGRpbmdPbmx5OiBmYWxzZSB9LFxuICAgICAgICAgICAgICAgIHNlbGVjdGlvblJhbmdlOiB7IGR5bmFtaWNSZWdpc3RyYXRpb246IHRydWUgfSxcbiAgICAgICAgICAgICAgICBjb2RlTGVuczogICAgICAgeyBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlIH0sXG4gICAgICAgICAgICAgICAgaW5sYXlIaW50OiAgICAgIHsgZHluYW1pY1JlZ2lzdHJhdGlvbjogdHJ1ZSwgcmVzb2x2ZVN1cHBvcnQ6IHsgcHJvcGVydGllczogWydsYWJlbCddIH0gfSxcbiAgICAgICAgICAgICAgICBzZW1hbnRpY1Rva2Vuczoge1xuICAgICAgICAgICAgICAgICAgICBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlLFxuICAgICAgICAgICAgICAgICAgICByZXF1ZXN0czogICAgICAgIHsgcmFuZ2U6IGZhbHNlLCBmdWxsOiB7IGRlbHRhOiBmYWxzZSB9IH0sXG4gICAgICAgICAgICAgICAgICAgIHRva2VuVHlwZXM6ICAgICAgWyduYW1lc3BhY2UnLCAndHlwZScsICdjbGFzcycsICdlbnVtJywgJ2ludGVyZmFjZScsICdzdHJ1Y3QnLCAndHlwZVBhcmFtZXRlcicsXG4gICAgICAgICAgICAgICAgICAgICAgICAncGFyYW1ldGVyJywgJ3ZhcmlhYmxlJywgJ3Byb3BlcnR5JywgJ2VudW1NZW1iZXInLCAnZXZlbnQnLCAnZnVuY3Rpb24nLCAnbWV0aG9kJywgJ21hY3JvJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICdrZXl3b3JkJywgJ21vZGlmaWVyJywgJ2NvbW1lbnQnLCAnc3RyaW5nJywgJ251bWJlcicsICdyZWdleHAnLCAnb3BlcmF0b3InLCAnZGVjb3JhdG9yJ10sXG4gICAgICAgICAgICAgICAgICAgIHRva2VuTW9kaWZpZXJzOiAgWydkZWNsYXJhdGlvbicsICdkZWZpbml0aW9uJywgJ3JlYWRvbmx5JywgJ3N0YXRpYycsICdkZXByZWNhdGVkJywgJ2Fic3RyYWN0JyxcbiAgICAgICAgICAgICAgICAgICAgICAgICdhc3luYycsICdtb2RpZmljYXRpb24nLCAnZG9jdW1lbnRhdGlvbicsICdkZWZhdWx0TGlicmFyeSddLFxuICAgICAgICAgICAgICAgICAgICBmb3JtYXRzOiAgICAgICAgIFsncmVsYXRpdmUnXSxcbiAgICAgICAgICAgICAgICAgICAgb3ZlcmxhcHBpbmdUb2tlblN1cHBvcnQ6IGZhbHNlLFxuICAgICAgICAgICAgICAgICAgICBtdWx0aWxpbmVUb2tlblN1cHBvcnQ6ICAgZmFsc2UsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICBmb3JtYXR0aW5nOiAgICAgeyBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlIH0sXG4gICAgICAgICAgICAgICAgcmFuZ2VGb3JtYXR0aW5nOiB7IGR5bmFtaWNSZWdpc3RyYXRpb246IHRydWUgfSxcbiAgICAgICAgICAgICAgICByZW5hbWU6ICAgICAgICAgeyBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlLCBwcmVwYXJlU3VwcG9ydDogdHJ1ZSB9LFxuICAgICAgICAgICAgICAgIGNvZGVBY3Rpb246IHtcbiAgICAgICAgICAgICAgICAgICAgZHluYW1pY1JlZ2lzdHJhdGlvbjogdHJ1ZSxcbiAgICAgICAgICAgICAgICAgICAgY29kZUFjdGlvbkxpdGVyYWxTdXBwb3J0OiB7XG4gICAgICAgICAgICAgICAgICAgICAgICBjb2RlQWN0aW9uS2luZDoge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhbHVlU2V0OiBbJ3F1aWNrZml4JywgJ3JlZmFjdG9yJywgJ3JlZmFjdG9yLmV4dHJhY3QnLCAncmVmYWN0b3IuaW5saW5lJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgJ3JlZmFjdG9yLnJld3JpdGUnLCAnc291cmNlJywgJ3NvdXJjZS5vcmdhbml6ZUltcG9ydHMnXSxcbiAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgIGlzUHJlZmVycmVkU3VwcG9ydDogdHJ1ZSxcbiAgICAgICAgICAgICAgICAgICAgZGF0YVN1cHBvcnQ6ICAgICAgICB0cnVlLFxuICAgICAgICAgICAgICAgICAgICByZXNvbHZlU3VwcG9ydDogICAgIHsgcHJvcGVydGllczogWydlZGl0J10gfSxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIHB1Ymxpc2hEaWFnbm9zdGljczogeyByZWxhdGVkSW5mb3JtYXRpb246IHRydWUgfSxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICB3b3Jrc3BhY2U6IHtcbiAgICAgICAgICAgICAgICBhcHBseUVkaXQ6ICAgICAgICAgICAgICB0cnVlLFxuICAgICAgICAgICAgICAgIGNvbmZpZ3VyYXRpb246ICAgICAgICAgIHRydWUsXG4gICAgICAgICAgICAgICAgZXhlY3V0ZUNvbW1hbmQ6ICAgICAgICAgeyBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlIH0sXG4gICAgICAgICAgICAgICAgZGlkQ2hhbmdlQ29uZmlndXJhdGlvbjogeyBkeW5hbWljUmVnaXN0cmF0aW9uOiB0cnVlIH0sXG4gICAgICAgICAgICAgICAgd29ya3NwYWNlRWRpdDogICAgICAgICAgeyBkb2N1bWVudENoYW5nZXM6IHRydWUsIHJlc291cmNlT3BlcmF0aW9uczogWydjcmVhdGUnLCAncmVuYW1lJywgJ2RlbGV0ZSddIH0sXG4gICAgICAgICAgICB9LFxuICAgICAgICB9LFxuICAgIH0pO1xuXG4gICAgX3NlbWFudGljVG9rZW5zTGVnZW5kID0gaW5pdFJlc3VsdD8uY2FwYWJpbGl0aWVzPy5zZW1hbnRpY1Rva2Vuc1Byb3ZpZGVyPy5sZWdlbmQgPz8gbnVsbDtcblxuICAgIF9jb25uLnNlbmROb3RpZmljYXRpb24oJ2luaXRpYWxpemVkJywge30pO1xuICAgIF9jb25uLnNlbmROb3RpZmljYXRpb24oJ3dvcmtzcGFjZS9kaWRDaGFuZ2VDb25maWd1cmF0aW9uJywgeyBzZXR0aW5nczogamR0bHNTZXR0aW5ncygpIH0pO1xuXG4gICAgb3BlbkZpbGUoZmlsZVVyaSk7XG5cbiAgICBpZiAoIV9wcm92aWRlcnNSZWdpc3RlcmVkKSB7XG4gICAgICAgIF9wcm92aWRlcnNSZWdpc3RlcmVkID0gdHJ1ZTtcbiAgICAgICAgcmVnaXN0ZXJQcm92aWRlcnMoKTtcbiAgICB9XG59XG5cbi8vIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbi8vIEZpbGUgbGlmZWN5Y2xlXG4vLyAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG5cbi8qKlxuICogU2VuZHMgdGV4dERvY3VtZW50L2RpZE9wZW4gZm9yIHRoZSBnaXZlbiBVUkkgKGlmIG5vdCBhbHJlYWR5IHNlbnQpIGFuZCByZWdpc3RlcnMgYSBkZWJvdW5jZWRcbiAqIHRleHREb2N1bWVudC9kaWRDaGFuZ2UgbGlzdGVuZXIgb24gdGhlIGNvcnJlc3BvbmRpbmcgTW9uYWNvIG1vZGVsLlxuICovXG5mdW5jdGlvbiBvcGVuRmlsZShmaWxlVXJpOiBzdHJpbmcpOiB2b2lkIHtcbiAgICBpZiAoX29wZW5GaWxlcy5oYXMoZmlsZVVyaSkgfHwgIV9jb25uKSByZXR1cm47XG4gICAgX29wZW5GaWxlcy5hZGQoZmlsZVVyaSk7XG5cbiAgICBjb25zdCBtb2RlbCA9IG1vbmFjby5lZGl0b3IuZ2V0TW9kZWxzKCkuZmluZChtID0+IG0udXJpLnRvU3RyaW5nKCkgPT09IGZpbGVVcmkpO1xuICAgIF9jb25uLnNlbmROb3RpZmljYXRpb24oJ3RleHREb2N1bWVudC9kaWRPcGVuJywge1xuICAgICAgICB0ZXh0RG9jdW1lbnQ6IHtcbiAgICAgICAgICAgIHVyaTogICAgICAgIGZpbGVVcmksXG4gICAgICAgICAgICBsYW5ndWFnZUlkOiAnamF2YScsXG4gICAgICAgICAgICB2ZXJzaW9uOiAgICAxLFxuICAgICAgICAgICAgdGV4dDogICAgICAgbW9kZWw/LmdldFZhbHVlKCkgPz8gJycsXG4gICAgICAgIH0sXG4gICAgfSk7XG5cbiAgICBpZiAobW9kZWwpIHtcbiAgICAgICAgbW9kZWwub25EaWRDaGFuZ2VDb250ZW50KCgpID0+IHtcbiAgICAgICAgICAgIGNvbnN0IGV4aXN0aW5nID0gX2NoYW5nZVRpbWVycy5nZXQoZmlsZVVyaSk7XG4gICAgICAgICAgICBpZiAoZXhpc3RpbmcpIGNsZWFyVGltZW91dChleGlzdGluZyk7XG4gICAgICAgICAgICBfY2hhbmdlVGltZXJzLnNldChmaWxlVXJpLCBzZXRUaW1lb3V0KCgpID0+IHNlbmREaWRDaGFuZ2UoZmlsZVVyaSksIDQwMCkpO1xuICAgICAgICB9KTtcbiAgICB9XG59XG5cbi8qKiBTZW5kcyB0aGUgY3VycmVudCBtb2RlbCBjb250ZW50IGFzIGEgZGlkQ2hhbmdlIGFuZCBjbGVhcnMgYW55IHBlbmRpbmcgZGVib3VuY2UgZm9yIHRoZSBmaWxlLiAqL1xuZnVuY3Rpb24gc2VuZERpZENoYW5nZShmaWxlVXJpOiBzdHJpbmcpOiB2b2lkIHtcbiAgICBfY2hhbmdlVGltZXJzLmRlbGV0ZShmaWxlVXJpKTtcbiAgICBjb25zdCBtb2RlbCA9IG1vbmFjby5lZGl0b3IuZ2V0TW9kZWwobW9uYWNvLlVyaS5wYXJzZShmaWxlVXJpKSk7XG4gICAgaWYgKF9jb25uICYmIG1vZGVsKSB7XG4gICAgICAgIF9jb25uLnNlbmROb3RpZmljYXRpb24oJ3RleHREb2N1bWVudC9kaWRDaGFuZ2UnLCB7XG4gICAgICAgICAgICB0ZXh0RG9jdW1lbnQ6ICAgeyB1cmk6IGZpbGVVcmksIHZlcnNpb246IG1vZGVsLmdldFZlcnNpb25JZCgpIH0sXG4gICAgICAgICAgICBjb250ZW50Q2hhbmdlczogW3sgdGV4dDogbW9kZWwuZ2V0VmFsdWUoKSB9XSxcbiAgICAgICAgfSk7XG4gICAgfVxufVxuXG4vKipcbiAqIEZsdXNoZXMgYSBwZW5kaW5nIGRlYm91bmNlZCBjaGFuZ2UgaW1tZWRpYXRlbHkuIENhbGxlZCBiZWZvcmUgYSBjb21wbGV0aW9uIHJlcXVlc3Qgc28gSkRULkxTIHNlZXMgdGhlXG4gKiBqdXN0LXR5cGVkIHRleHQgb24gdGhlIGZpcnN0IEN0cmwrU3BhY2UsIGluc3RlYWQgb2YgY29tcGxldGluZyBhZ2FpbnN0IHN0YWxlIGNvbnRlbnQgKHRoZSBkZWJvdW5jZVxuICogb3RoZXJ3aXNlIGRlbGF5cyB0aGUgY2hhbmdlIGJ5IHVwIHRvIDQwMG1zIFx1MjAxNCB0aGUgY2F1c2Ugb2YgXCJmaXJzdCBDdHJsK1NwYWNlIHNob3dzIG5vdGhpbmdcIikuXG4gKi9cbmZ1bmN0aW9uIGZsdXNoUGVuZGluZ0NoYW5nZShmaWxlVXJpOiBzdHJpbmcpOiB2b2lkIHtcbiAgICBpZiAoX2NoYW5nZVRpbWVycy5oYXMoZmlsZVVyaSkpIHtcbiAgICAgICAgY2xlYXJUaW1lb3V0KF9jaGFuZ2VUaW1lcnMuZ2V0KGZpbGVVcmkpISk7XG4gICAgICAgIHNlbmREaWRDaGFuZ2UoZmlsZVVyaSk7XG4gICAgfVxufVxuXG4vLyAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4vLyBXb3Jrc3BhY2UgZmlsZSBwcmVkaWNhdGVcbi8vIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cblxuLyoqXG4gKiBSZXR1cm5zIHtAY29kZSB0cnVlfSB3aGVuIHRoZSBnaXZlbiBtb2RlbCBVUkkgYmVsb25ncyB0byB0aGUgY3VycmVudGx5IGNvbm5lY3RlZCB3b3Jrc3BhY2UuIFVzZWRcbiAqIGJ5IE1vbmFjbyBwcm92aWRlcnMgdG8gc2tpcCBub24tSmF2YS13b3Jrc3BhY2UgbW9kZWxzIHdpdGhvdXQgYW4gZXhhY3QtVVJJIGNvbXBhcmlzb24uXG4gKi9cbmZ1bmN0aW9uIGlzV29ya3NwYWNlRmlsZSh1cmk6IHN0cmluZyk6IGJvb2xlYW4ge1xuICAgIHJldHVybiBfd29ya3NwYWNlUm9vdCAhPT0gJycgJiYgdXJpLnN0YXJ0c1dpdGgoX3dvcmtzcGFjZVJvb3QpO1xufVxuXG4vLyAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4vLyBNb25hY28gcHJvdmlkZXIgcmVnaXN0cmF0aW9uXG4vLyAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG5cbmZ1bmN0aW9uIHJlZ2lzdGVyUHJvdmlkZXJzKCk6IHZvaWQge1xuXG4gICAgbW9uYWNvLmVkaXRvci5yZWdpc3RlckNvbW1hbmQoQVBQTFlfQUNUSU9OX0NPTU1BTkQsIChfYWNjZXNzb3I6IHVua25vd24sIGFjdGlvbjogQ29kZUFjdGlvbiB8IENvbW1hbmQpID0+IHtcbiAgICAgICAgYXBwbHlDb2RlQWN0aW9uKGFjdGlvbik7XG4gICAgfSk7XG4gICAgbW9uYWNvLmVkaXRvci5yZWdpc3RlckNvbW1hbmQoTk9PUF9DT01NQU5ELCAoKSA9PiB7IC8qIGRpc3BsYXktb25seSBDb2RlTGVucyAqLyB9KTtcblxuICAgIC8vIENyb3NzLWZpbGUgbmF2aWdhdGlvbjogdGhpcyBzaW5nbGUtZmlsZSBlZGl0b3IgaGFzIG5vIG1vZGVsIGZvciBvdGhlciB3b3Jrc3BhY2UgZmlsZXMsIHNvIEdvIHRvXG4gICAgLy8gRGVmaW5pdGlvbiAvIEZpbmQgUmVmZXJlbmNlcyB0byBhbm90aGVyIGZpbGUgd291bGQgc2lsZW50bHkgZG8gbm90aGluZy4gSGFuZCB0aG9zZSBvZmYgdG8gdGhlIElERVxuICAgIC8vIHRvIG9wZW4gdGhlIHRhcmdldCBmaWxlIChhbmQgcmV2ZWFsIHRoZSBsaW5lKS4gU2FtZS1maWxlIHRhcmdldHMgZmFsbCB0aHJvdWdoIHRvIE1vbmFjby5cbiAgICBtb25hY28uZWRpdG9yLnJlZ2lzdGVyRWRpdG9yT3BlbmVyKHtcbiAgICAgICAgb3BlbkNvZGVFZGl0b3I6IChzb3VyY2UsIHJlc291cmNlLCBzZWxlY3Rpb25PclBvc2l0aW9uKSA9PiB7XG4gICAgICAgICAgICBjb25zdCB1cmkgPSByZXNvdXJjZS50b1N0cmluZygpO1xuICAgICAgICAgICAgaWYgKCFpc1dvcmtzcGFjZUZpbGUodXJpKSB8fCAhdXJpLnN0YXJ0c1dpdGgoVklSVFVBTF9GSUxFX1BSRUZJWCkpIHJldHVybiBmYWxzZTtcbiAgICAgICAgICAgIGNvbnN0IGN1cnJlbnRNb2RlbCA9IHNvdXJjZS5nZXRNb2RlbCgpO1xuICAgICAgICAgICAgaWYgKGN1cnJlbnRNb2RlbCAmJiBjdXJyZW50TW9kZWwudXJpLnRvU3RyaW5nKCkgPT09IHVyaSkgcmV0dXJuIGZhbHNlOyAvLyBzYW1lIGZpbGUgXHUyMDE0IGxldCBNb25hY28ganVtcFxuICAgICAgICAgICAgY29uc3Qgb3BlbmVyID0gKGdsb2JhbFRoaXMgYXMgYW55KS5qYXZhTHNwT3BlbkZpbGU7XG4gICAgICAgICAgICBpZiAodHlwZW9mIG9wZW5lciAhPT0gJ2Z1bmN0aW9uJykgcmV0dXJuIGZhbHNlO1xuICAgICAgICAgICAgY29uc3QgcG9zID0gc2VsZWN0aW9uT3JQb3NpdGlvbiBhcyB7IHN0YXJ0TGluZU51bWJlcj86IG51bWJlcjsgbGluZU51bWJlcj86IG51bWJlcjsgc3RhcnRDb2x1bW4/OiBudW1iZXI7IGNvbHVtbj86IG51bWJlciB9IHwgdW5kZWZpbmVkO1xuICAgICAgICAgICAgY29uc3QgbGluZSA9IHBvcyA/IChwb3Muc3RhcnRMaW5lTnVtYmVyID8/IHBvcy5saW5lTnVtYmVyKSA6IHVuZGVmaW5lZDtcbiAgICAgICAgICAgIGNvbnN0IGNvbHVtbiA9IHBvcyA/IChwb3Muc3RhcnRDb2x1bW4gPz8gcG9zLmNvbHVtbikgOiB1bmRlZmluZWQ7XG4gICAgICAgICAgICBvcGVuZXIodXJpLnN1YnN0cmluZyhWSVJUVUFMX0ZJTEVfUFJFRklYLmxlbmd0aCksIGxpbmUsIGNvbHVtbik7XG4gICAgICAgICAgICByZXR1cm4gdHJ1ZTtcbiAgICAgICAgfSxcbiAgICB9KTtcblxuICAgIG1vbmFjby5sYW5ndWFnZXMucmVnaXN0ZXJDb21wbGV0aW9uSXRlbVByb3ZpZGVyKCdqYXZhJywge1xuICAgICAgICB0cmlnZ2VyQ2hhcmFjdGVyczogWycuJywgJ0AnLCAnPCddLFxuICAgICAgICBwcm92aWRlQ29tcGxldGlvbkl0ZW1zOiBhc3luYyAobW9kZWwsIHBvc2l0aW9uLCBjb250ZXh0KSA9PiB7XG4gICAgICAgICAgICBpZiAoIV9jb25uIHx8ICFpc1dvcmtzcGFjZUZpbGUobW9kZWwudXJpLnRvU3RyaW5nKCkpKSByZXR1cm4gbnVsbDtcbiAgICAgICAgICAgIGNvbnN0IGZpbGVVcmkgPSBtb2RlbC51cmkudG9TdHJpbmcoKTtcbiAgICAgICAgICAgIC8vIE1ha2Ugc3VyZSBKRFQuTFMgaGFzIHRoZSBqdXN0LXR5cGVkIHRleHQgYmVmb3JlIGNvbXBsZXRpbmcgKHNlZSBmbHVzaFBlbmRpbmdDaGFuZ2UpLlxuICAgICAgICAgICAgZmx1c2hQZW5kaW5nQ2hhbmdlKGZpbGVVcmkpO1xuICAgICAgICAgICAgY29uc3QgcmVzdWx0OiBDb21wbGV0aW9uTGlzdCB8IENvbXBsZXRpb25JdGVtW10gfCBudWxsID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ3RleHREb2N1bWVudC9jb21wbGV0aW9uJywge1xuICAgICAgICAgICAgICAgIHRleHREb2N1bWVudDogeyB1cmk6IGZpbGVVcmkgfSxcbiAgICAgICAgICAgICAgICBwb3NpdGlvbjogICAgIHsgbGluZTogcG9zaXRpb24ubGluZU51bWJlciAtIDEsIGNoYXJhY3RlcjogcG9zaXRpb24uY29sdW1uIC0gMSB9LFxuICAgICAgICAgICAgICAgIC8vIE1vbmFjbyB0cmlnZ2VyIGtpbmRzIGFyZSAwLWJhc2VkIChJbnZva2UvVHJpZ2dlckNoYXJhY3Rlci9Gb3JJbmNvbXBsZXRlKTsgTFNQIGlzIDEtYmFzZWQuXG4gICAgICAgICAgICAgICAgY29udGV4dDogICAgICB7IHRyaWdnZXJLaW5kOiAoY29udGV4dC50cmlnZ2VyS2luZCA/PyAwKSArIDEsIHRyaWdnZXJDaGFyYWN0ZXI6IGNvbnRleHQudHJpZ2dlckNoYXJhY3RlciB9LFxuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICBjb25zdCBpdGVtcyA9IEFycmF5LmlzQXJyYXkocmVzdWx0KSA/IHJlc3VsdCA6IChyZXN1bHQ/Lml0ZW1zID8/IFtdKTtcbiAgICAgICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICAgICAgc3VnZ2VzdGlvbnM6IGl0ZW1zLm1hcChpdGVtID0+IGxzcENvbXBsZXRpb25Ub01vbmFjbyhpdGVtLCBtb2RlbCwgcG9zaXRpb24pKSxcbiAgICAgICAgICAgICAgICAvLyBKRFQuTFMgcmV0dXJucyBhIHRydW5jYXRlZCBsaXN0IG9uIHRoZSBmaXJzdCBrZXlzdHJva2VzOyBwcm9wYWdhdGluZyBcImluY29tcGxldGVcIiBtYWtlc1xuICAgICAgICAgICAgICAgIC8vIE1vbmFjbyByZS1xdWVyeSBhcyB0aGUgdXNlciB0eXBlcyBpbnN0ZWFkIG9mIGNhY2hpbmcgdGhlIGZpcnN0IChvZnRlbiBlbXB0eSkgcmVzdWx0LlxuICAgICAgICAgICAgICAgIGluY29tcGxldGU6ICBBcnJheS5pc0FycmF5KHJlc3VsdCkgPyBmYWxzZSA6ICEhcmVzdWx0Py5pc0luY29tcGxldGUsXG4gICAgICAgICAgICB9O1xuICAgICAgICB9LFxuICAgICAgICAvLyBSZXNvbHZlIGRvY3VtZW50YXRpb24gYW5kLCBjcnVjaWFsbHksIHRoZSBhdXRvLWltcG9ydCBhZGRpdGlvbmFsVGV4dEVkaXRzIHdoaWNoIEpEVC5MUyBvbmx5XG4gICAgICAgIC8vIGF0dGFjaGVzIG9uIHJlc29sdmUgXHUyMDE0IHNlbGVjdGluZyBhIHR5cGUgdGhlbiBpbnNlcnRzIGl0cyBpbXBvcnQgc3RhdGVtZW50LlxuICAgICAgICByZXNvbHZlQ29tcGxldGlvbkl0ZW06IGFzeW5jIChpdGVtKSA9PiB7XG4gICAgICAgICAgICBjb25zdCBsc3AgPSAoaXRlbSBhcyBNb25hY29Db21wbGV0aW9uSXRlbSkuX2xzcDtcbiAgICAgICAgICAgIGlmICghX2Nvbm4gfHwgIWxzcCkgcmV0dXJuIGl0ZW07XG4gICAgICAgICAgICB0cnkge1xuICAgICAgICAgICAgICAgIGNvbnN0IHJlc29sdmVkOiBDb21wbGV0aW9uSXRlbSA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCdjb21wbGV0aW9uSXRlbS9yZXNvbHZlJywgbHNwKTtcbiAgICAgICAgICAgICAgICBpZiAocmVzb2x2ZWQuZG9jdW1lbnRhdGlvbikge1xuICAgICAgICAgICAgICAgICAgICBpdGVtLmRvY3VtZW50YXRpb24gPSB7IHZhbHVlOiBtYXJrdXBUb1N0cmluZyhyZXNvbHZlZC5kb2N1bWVudGF0aW9uKSwgaXNUcnVzdGVkOiBmYWxzZSB9O1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBpZiAocmVzb2x2ZWQuZGV0YWlsKSBpdGVtLmRldGFpbCA9IHJlc29sdmVkLmRldGFpbDtcbiAgICAgICAgICAgICAgICBpZiAocmVzb2x2ZWQuYWRkaXRpb25hbFRleHRFZGl0cz8ubGVuZ3RoKSB7XG4gICAgICAgICAgICAgICAgICAgIGl0ZW0uYWRkaXRpb25hbFRleHRFZGl0cyA9IHJlc29sdmVkLmFkZGl0aW9uYWxUZXh0RWRpdHMubWFwKHRleHRFZGl0VG9Nb25hY28pO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH0gY2F0Y2ggKGUpIHtcbiAgICAgICAgICAgICAgICBjb25zb2xlLmRlYnVnKCdbamF2YS1sc3BdIGNvbXBsZXRpb24gcmVzb2x2ZSBmYWlsZWQ6JywgKGUgYXMgRXJyb3IpPy5tZXNzYWdlKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJldHVybiBpdGVtO1xuICAgICAgICB9LFxuICAgIH0pO1xuXG4gICAgbW9uYWNvLmxhbmd1YWdlcy5yZWdpc3RlckhvdmVyUHJvdmlkZXIoJ2phdmEnLCB7XG4gICAgICAgIHByb3ZpZGVIb3ZlcjogYXN5bmMgKG1vZGVsLCBwb3NpdGlvbikgPT4ge1xuICAgICAgICAgICAgaWYgKCFfY29ubiB8fCAhaXNXb3Jrc3BhY2VGaWxlKG1vZGVsLnVyaS50b1N0cmluZygpKSkgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICBjb25zdCBmaWxlVXJpID0gbW9kZWwudXJpLnRvU3RyaW5nKCk7XG4gICAgICAgICAgICBjb25zdCByZXN1bHQ6IEhvdmVyIHwgbnVsbCA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCd0ZXh0RG9jdW1lbnQvaG92ZXInLCB7XG4gICAgICAgICAgICAgICAgdGV4dERvY3VtZW50OiB7IHVyaTogZmlsZVVyaSB9LFxuICAgICAgICAgICAgICAgIHBvc2l0aW9uOiAgICAgeyBsaW5lOiBwb3NpdGlvbi5saW5lTnVtYmVyIC0gMSwgY2hhcmFjdGVyOiBwb3NpdGlvbi5jb2x1bW4gLSAxIH0sXG4gICAgICAgICAgICB9KTtcbiAgICAgICAgICAgIGlmICghcmVzdWx0Py5jb250ZW50cykgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICBjb25zdCBjb250ZW50cyA9IEFycmF5LmlzQXJyYXkocmVzdWx0LmNvbnRlbnRzKSA/IHJlc3VsdC5jb250ZW50cyA6IFtyZXN1bHQuY29udGVudHNdO1xuICAgICAgICAgICAgcmV0dXJuIHtcbiAgICAgICAgICAgICAgICBjb250ZW50czogY29udGVudHMubWFwKGMgPT4gKHtcbiAgICAgICAgICAgICAgICAgICAgdmFsdWU6IHR5cGVvZiBjID09PSAnc3RyaW5nJyA/IGMgOiAoYyBhcyBNYXJrdXBDb250ZW50KS52YWx1ZSxcbiAgICAgICAgICAgICAgICAgICAgaXNUcnVzdGVkOiBmYWxzZSxcbiAgICAgICAgICAgICAgICB9KSksXG4gICAgICAgICAgICAgICAgcmFuZ2U6IHJlc3VsdC5yYW5nZSA/IGxzcFJhbmdlVG9Nb25hY28ocmVzdWx0LnJhbmdlKSA6IHVuZGVmaW5lZCxcbiAgICAgICAgICAgIH07XG4gICAgICAgIH0sXG4gICAgfSk7XG5cbiAgICBtb25hY28ubGFuZ3VhZ2VzLnJlZ2lzdGVyU2lnbmF0dXJlSGVscFByb3ZpZGVyKCdqYXZhJywge1xuICAgICAgICBzaWduYXR1cmVIZWxwVHJpZ2dlckNoYXJhY3RlcnM6IFsnKCcsICcsJ10sXG4gICAgICAgIHByb3ZpZGVTaWduYXR1cmVIZWxwOiBhc3luYyAobW9kZWwsIHBvc2l0aW9uKSA9PiB7XG4gICAgICAgICAgICBpZiAoIV9jb25uIHx8ICFpc1dvcmtzcGFjZUZpbGUobW9kZWwudXJpLnRvU3RyaW5nKCkpKSByZXR1cm4gbnVsbDtcbiAgICAgICAgICAgIGNvbnN0IGZpbGVVcmkgPSBtb2RlbC51cmkudG9TdHJpbmcoKTtcbiAgICAgICAgICAgIGNvbnN0IHJlc3VsdDogU2lnbmF0dXJlSGVscCB8IG51bGwgPSBhd2FpdCBfY29ubi5zZW5kUmVxdWVzdCgndGV4dERvY3VtZW50L3NpZ25hdHVyZUhlbHAnLCB7XG4gICAgICAgICAgICAgICAgdGV4dERvY3VtZW50OiB7IHVyaTogZmlsZVVyaSB9LFxuICAgICAgICAgICAgICAgIHBvc2l0aW9uOiAgICAgeyBsaW5lOiBwb3NpdGlvbi5saW5lTnVtYmVyIC0gMSwgY2hhcmFjdGVyOiBwb3NpdGlvbi5jb2x1bW4gLSAxIH0sXG4gICAgICAgICAgICB9KTtcbiAgICAgICAgICAgIGlmICghcmVzdWx0KSByZXR1cm4gbnVsbDtcbiAgICAgICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICAgICAgdmFsdWU6IHtcbiAgICAgICAgICAgICAgICAgICAgc2lnbmF0dXJlczogcmVzdWx0LnNpZ25hdHVyZXMubWFwKChzaWc6IFNpZ25hdHVyZUluZm9ybWF0aW9uKSA9PiAoe1xuICAgICAgICAgICAgICAgICAgICAgICAgbGFiZWw6ICAgICAgICAgc2lnLmxhYmVsLFxuICAgICAgICAgICAgICAgICAgICAgICAgZG9jdW1lbnRhdGlvbjogc2lnLmRvY3VtZW50YXRpb24gPyBtYXJrdXBUb1N0cmluZyhzaWcuZG9jdW1lbnRhdGlvbikgOiB1bmRlZmluZWQsXG4gICAgICAgICAgICAgICAgICAgICAgICBwYXJhbWV0ZXJzOiAgICAoc2lnLnBhcmFtZXRlcnMgPz8gW10pLm1hcCgocDogUGFyYW1ldGVySW5mb3JtYXRpb24pID0+ICh7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgbGFiZWw6ICAgICAgICAgcC5sYWJlbCxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBkb2N1bWVudGF0aW9uOiBwLmRvY3VtZW50YXRpb24gPyBtYXJrdXBUb1N0cmluZyhwLmRvY3VtZW50YXRpb24pIDogdW5kZWZpbmVkLFxuICAgICAgICAgICAgICAgICAgICAgICAgfSkpLFxuICAgICAgICAgICAgICAgICAgICB9KSksXG4gICAgICAgICAgICAgICAgICAgIGFjdGl2ZVNpZ25hdHVyZTogcmVzdWx0LmFjdGl2ZVNpZ25hdHVyZSA/PyAwLFxuICAgICAgICAgICAgICAgICAgICBhY3RpdmVQYXJhbWV0ZXI6IHJlc3VsdC5hY3RpdmVQYXJhbWV0ZXIgPz8gMCxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIGRpc3Bvc2U6ICgpID0+IHt9LFxuICAgICAgICAgICAgfTtcbiAgICAgICAgfSxcbiAgICB9KTtcblxuICAgIG1vbmFjby5sYW5ndWFnZXMucmVnaXN0ZXJEZWZpbml0aW9uUHJvdmlkZXIoJ2phdmEnLCB7XG4gICAgICAgIHByb3ZpZGVEZWZpbml0aW9uOiBhc3luYyAobW9kZWwsIHBvc2l0aW9uKSA9PiB7XG4gICAgICAgICAgICBpZiAoIV9jb25uIHx8ICFpc1dvcmtzcGFjZUZpbGUobW9kZWwudXJpLnRvU3RyaW5nKCkpKSByZXR1cm4gbnVsbDtcbiAgICAgICAgICAgIGNvbnN0IGZpbGVVcmkgPSBtb2RlbC51cmkudG9TdHJpbmcoKTtcbiAgICAgICAgICAgIGNvbnN0IHJlc3VsdDogTG9jYXRpb24gfCBMb2NhdGlvbltdIHwgbnVsbCA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCd0ZXh0RG9jdW1lbnQvZGVmaW5pdGlvbicsIHtcbiAgICAgICAgICAgICAgICB0ZXh0RG9jdW1lbnQ6IHsgdXJpOiBmaWxlVXJpIH0sXG4gICAgICAgICAgICAgICAgcG9zaXRpb246ICAgICB7IGxpbmU6IHBvc2l0aW9uLmxpbmVOdW1iZXIgLSAxLCBjaGFyYWN0ZXI6IHBvc2l0aW9uLmNvbHVtbiAtIDEgfSxcbiAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgaWYgKCFyZXN1bHQpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgY29uc3QgbG9jYXRpb25zID0gKEFycmF5LmlzQXJyYXkocmVzdWx0KSA/IHJlc3VsdCA6IFtyZXN1bHRdKS5tYXAobG9jID0+ICh7XG4gICAgICAgICAgICAgICAgdXJpOiAgIG1vbmFjby5VcmkucGFyc2UobG9jLnVyaSksXG4gICAgICAgICAgICAgICAgcmFuZ2U6IGxzcFJhbmdlVG9Nb25hY28obG9jLnJhbmdlKSxcbiAgICAgICAgICAgIH0pKTtcbiAgICAgICAgICAgIGF3YWl0IGVuc3VyZU1vZGVsc0ZvckxvY2F0aW9ucyhsb2NhdGlvbnMpO1xuICAgICAgICAgICAgcmV0dXJuIGxvY2F0aW9ucztcbiAgICAgICAgfSxcbiAgICB9KTtcblxuICAgIG1vbmFjby5sYW5ndWFnZXMucmVnaXN0ZXJSZWZlcmVuY2VQcm92aWRlcignamF2YScsIHtcbiAgICAgICAgcHJvdmlkZVJlZmVyZW5jZXM6IGFzeW5jIChtb2RlbCwgcG9zaXRpb24sIGNvbnRleHQpID0+IHtcbiAgICAgICAgICAgIGlmICghX2Nvbm4gfHwgIWlzV29ya3NwYWNlRmlsZShtb2RlbC51cmkudG9TdHJpbmcoKSkpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgY29uc3QgcmVzdWx0OiBMb2NhdGlvbltdIHwgbnVsbCA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCd0ZXh0RG9jdW1lbnQvcmVmZXJlbmNlcycsIHtcbiAgICAgICAgICAgICAgICB0ZXh0RG9jdW1lbnQ6IHsgdXJpOiBtb2RlbC51cmkudG9TdHJpbmcoKSB9LFxuICAgICAgICAgICAgICAgIHBvc2l0aW9uOiAgICAgeyBsaW5lOiBwb3NpdGlvbi5saW5lTnVtYmVyIC0gMSwgY2hhcmFjdGVyOiBwb3NpdGlvbi5jb2x1bW4gLSAxIH0sXG4gICAgICAgICAgICAgICAgY29udGV4dDogICAgICB7IGluY2x1ZGVEZWNsYXJhdGlvbjogY29udGV4dC5pbmNsdWRlRGVjbGFyYXRpb24gfSxcbiAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgaWYgKCFyZXN1bHQpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgY29uc3QgbG9jYXRpb25zID0gcmVzdWx0Lm1hcChsb2MgPT4gKHsgdXJpOiBtb25hY28uVXJpLnBhcnNlKGxvYy51cmkpLCByYW5nZTogbHNwUmFuZ2VUb01vbmFjbyhsb2MucmFuZ2UpIH0pKTtcbiAgICAgICAgICAgIC8vIFRoZSByZWZlcmVuY2VzIHBlZWsgY2FuIG9ubHkgc2hvdyBhIGNvZGUgcHJldmlldyBmb3IgZmlsZXMgaXQgaGFzIGEgbW9kZWwgZm9yOyBjcmVhdGVcbiAgICAgICAgICAgIC8vIGluLW1lbW9yeSBtb2RlbHMgZm9yIHRoZSByZWZlcmVuY2VkIChwb3NzaWJseSB1bm9wZW5lZCkgZmlsZXMgc28gcHJldmlld3MgcmVuZGVyLlxuICAgICAgICAgICAgYXdhaXQgZW5zdXJlTW9kZWxzRm9yTG9jYXRpb25zKGxvY2F0aW9ucyk7XG4gICAgICAgICAgICByZXR1cm4gbG9jYXRpb25zO1xuICAgICAgICB9LFxuICAgIH0pO1xuXG4gICAgbW9uYWNvLmxhbmd1YWdlcy5yZWdpc3RlclJlbmFtZVByb3ZpZGVyKCdqYXZhJywge1xuICAgICAgICBwcm92aWRlUmVuYW1lRWRpdHM6IGFzeW5jIChtb2RlbCwgcG9zaXRpb24sIG5ld05hbWUpID0+IHtcbiAgICAgICAgICAgIGlmICghX2Nvbm4gfHwgIWlzV29ya3NwYWNlRmlsZShtb2RlbC51cmkudG9TdHJpbmcoKSkpIHJldHVybiB7IGVkaXRzOiBbXSB9O1xuICAgICAgICAgICAgY29uc3QgZWRpdDogV29ya3NwYWNlRWRpdCB8IG51bGwgPSBhd2FpdCBfY29ubi5zZW5kUmVxdWVzdCgndGV4dERvY3VtZW50L3JlbmFtZScsIHtcbiAgICAgICAgICAgICAgICB0ZXh0RG9jdW1lbnQ6IHsgdXJpOiBtb2RlbC51cmkudG9TdHJpbmcoKSB9LFxuICAgICAgICAgICAgICAgIHBvc2l0aW9uOiAgICAgeyBsaW5lOiBwb3NpdGlvbi5saW5lTnVtYmVyIC0gMSwgY2hhcmFjdGVyOiBwb3NpdGlvbi5jb2x1bW4gLSAxIH0sXG4gICAgICAgICAgICAgICAgbmV3TmFtZSxcbiAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgaWYgKCFlZGl0KSByZXR1cm4geyBlZGl0czogW10gfTtcbiAgICAgICAgICAgIC8vIEEgSkRULkxTIHJlbmFtZSBjYW4gc3BhbiBtYW55IGZpbGVzIGFuZCBldmVuIHJlbmFtZSB0aGUgdHlwZSdzIG93biAuamF2YSBmaWxlLiBNb25hY28nc1xuICAgICAgICAgICAgLy8gc2luZ2xlLWZpbGUgZWRpdG9yIHdvdWxkIGRyb3AgZXZlcnl0aGluZyBidXQgdGhlIGN1cnJlbnQgbW9kZWwsIHNvIGFwcGx5IHRoZSB3aG9sZSBlZGl0XG4gICAgICAgICAgICAvLyB0aHJvdWdoIHRoZSB3b3Jrc3BhY2Ugb3Vyc2VsdmVzIHdoZW4gdGhlIElERSBwZXJzaXN0ZW5jZSBob29rIGlzIGF2YWlsYWJsZS5cbiAgICAgICAgICAgIGlmICh0eXBlb2YgKGdsb2JhbFRoaXMgYXMgYW55KS5qYXZhTHNwUGVyc2lzdFJlbmFtZSA9PT0gJ2Z1bmN0aW9uJykge1xuICAgICAgICAgICAgICAgIHRyeSB7XG4gICAgICAgICAgICAgICAgICAgIGF3YWl0IGFwcGx5UmVuYW1lQWNyb3NzV29ya3NwYWNlKG1vZGVsLCBlZGl0KTtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIHsgZWRpdHM6IFtdIH07XG4gICAgICAgICAgICAgICAgfSBjYXRjaCAoZSkge1xuICAgICAgICAgICAgICAgICAgICBjb25zb2xlLmVycm9yKCdbamF2YS1sc3BdIGNyb3NzLWZpbGUgcmVuYW1lIGZhaWxlZCwgYXBwbHlpbmcgdG8gdGhlIGN1cnJlbnQgZmlsZSBvbmx5OicsIGUpO1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4gd29ya3NwYWNlRWRpdFRvTW9uYWNvKGVkaXQpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJldHVybiB3b3Jrc3BhY2VFZGl0VG9Nb25hY28oZWRpdCk7XG4gICAgICAgIH0sXG4gICAgICAgIHJlc29sdmVSZW5hbWVMb2NhdGlvbjogYXN5bmMgKG1vZGVsLCBwb3NpdGlvbikgPT4ge1xuICAgICAgICAgICAgaWYgKCFfY29ubiB8fCAhaXNXb3Jrc3BhY2VGaWxlKG1vZGVsLnVyaS50b1N0cmluZygpKSkgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICB0cnkge1xuICAgICAgICAgICAgICAgIGNvbnN0IHJlc3VsdDogeyByYW5nZTogTHNwUmFuZ2U7IHBsYWNlaG9sZGVyPzogc3RyaW5nIH0gfCBMc3BSYW5nZSB8IG51bGwgPVxuICAgICAgICAgICAgICAgICAgICBhd2FpdCBfY29ubi5zZW5kUmVxdWVzdCgndGV4dERvY3VtZW50L3ByZXBhcmVSZW5hbWUnLCB7XG4gICAgICAgICAgICAgICAgICAgICAgICB0ZXh0RG9jdW1lbnQ6IHsgdXJpOiBtb2RlbC51cmkudG9TdHJpbmcoKSB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgcG9zaXRpb246ICAgICB7IGxpbmU6IHBvc2l0aW9uLmxpbmVOdW1iZXIgLSAxLCBjaGFyYWN0ZXI6IHBvc2l0aW9uLmNvbHVtbiAtIDEgfSxcbiAgICAgICAgICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICAgICAgaWYgKCFyZXN1bHQpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgICAgIGNvbnN0IHJhbmdlID0gJ3JhbmdlJyBpbiByZXN1bHQgPyByZXN1bHQucmFuZ2UgOiByZXN1bHQ7XG4gICAgICAgICAgICAgICAgY29uc3QgcGxhY2Vob2xkZXIgPSAncGxhY2Vob2xkZXInIGluIHJlc3VsdCAmJiByZXN1bHQucGxhY2Vob2xkZXJcbiAgICAgICAgICAgICAgICAgICAgPyByZXN1bHQucGxhY2Vob2xkZXJcbiAgICAgICAgICAgICAgICAgICAgOiBtb2RlbC5nZXRXb3JkQXRQb3NpdGlvbihwb3NpdGlvbik/LndvcmQgPz8gJyc7XG4gICAgICAgICAgICAgICAgcmV0dXJuIHsgcmFuZ2U6IGxzcFJhbmdlVG9Nb25hY28ocmFuZ2UpLCB0ZXh0OiBwbGFjZWhvbGRlciB9O1xuICAgICAgICAgICAgfSBjYXRjaCB7XG4gICAgICAgICAgICAgICAgY29uc3Qgd29yZCA9IG1vZGVsLmdldFdvcmRBdFBvc2l0aW9uKHBvc2l0aW9uKTtcbiAgICAgICAgICAgICAgICByZXR1cm4gd29yZCA/IHtcbiAgICAgICAgICAgICAgICAgICAgcmFuZ2U6IHsgc3RhcnRMaW5lTnVtYmVyOiBwb3NpdGlvbi5saW5lTnVtYmVyLCBzdGFydENvbHVtbjogd29yZC5zdGFydENvbHVtbiwgZW5kTGluZU51bWJlcjogcG9zaXRpb24ubGluZU51bWJlciwgZW5kQ29sdW1uOiB3b3JkLmVuZENvbHVtbiB9LFxuICAgICAgICAgICAgICAgICAgICB0ZXh0OiB3b3JkLndvcmQsXG4gICAgICAgICAgICAgICAgfSA6IG51bGw7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0sXG4gICAgfSk7XG5cbiAgICAvLyBSZWdpc3RlcmluZyB0aGlzIHByb3ZpZGVyIGFsc28gZW5hYmxlcyBlZGl0b3IuanMncyBleGlzdGluZyBmb3JtYXQtb24tc2F2ZSBwYXRoIGZvciBKYXZhOiB0aGVcbiAgICAvLyBzaGFyZWQgU2F2ZSBhY3Rpb24gcnVucyBlZGl0b3IuYWN0aW9uLmZvcm1hdERvY3VtZW50IHdoZW4gYXV0by1mb3JtYXR0aW5nIGlzIG9uICh0aGUgc2FtZVxuICAgIC8vIG1lY2hhbmlzbSBhbmQgZ2xvYmFsIHRvZ2dsZSB1c2VkIGZvciBUeXBlU2NyaXB0KS5cbiAgICBtb25hY28ubGFuZ3VhZ2VzLnJlZ2lzdGVyRG9jdW1lbnRGb3JtYXR0aW5nRWRpdFByb3ZpZGVyKCdqYXZhJywge1xuICAgICAgICBwcm92aWRlRG9jdW1lbnRGb3JtYXR0aW5nRWRpdHM6IGFzeW5jIChtb2RlbCkgPT4ge1xuICAgICAgICAgICAgaWYgKCFfY29ubiB8fCAhaXNXb3Jrc3BhY2VGaWxlKG1vZGVsLnVyaS50b1N0cmluZygpKSkgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICBjb25zdCBlZGl0czogVGV4dEVkaXRbXSB8IG51bGwgPSBhd2FpdCBfY29ubi5zZW5kUmVxdWVzdCgndGV4dERvY3VtZW50L2Zvcm1hdHRpbmcnLCB7XG4gICAgICAgICAgICAgICAgdGV4dERvY3VtZW50OiB7IHVyaTogbW9kZWwudXJpLnRvU3RyaW5nKCkgfSxcbiAgICAgICAgICAgICAgICBvcHRpb25zOiAgICAgIHsgdGFiU2l6ZTogbW9kZWwuZ2V0T3B0aW9ucygpLnRhYlNpemUsIGluc2VydFNwYWNlczogbW9kZWwuZ2V0T3B0aW9ucygpLmluc2VydFNwYWNlcyB9LFxuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICByZXR1cm4gZWRpdHMgPyBlZGl0cy5tYXAodGV4dEVkaXRUb01vbmFjbykgOiBudWxsO1xuICAgICAgICB9LFxuICAgIH0pO1xuXG4gICAgbW9uYWNvLmxhbmd1YWdlcy5yZWdpc3RlckNvZGVBY3Rpb25Qcm92aWRlcignamF2YScsIHtcbiAgICAgICAgcHJvdmlkZUNvZGVBY3Rpb25zOiBhc3luYyAobW9kZWwsIHJhbmdlLCBjb250ZXh0KSA9PiB7XG4gICAgICAgICAgICBjb25zdCBlbXB0eSA9IHsgYWN0aW9uczogW10sIGRpc3Bvc2UoKSB7IC8qIG5vdGhpbmcgdG8gZGlzcG9zZSAqLyB9IH07XG4gICAgICAgICAgICBpZiAoIV9jb25uIHx8ICFpc1dvcmtzcGFjZUZpbGUobW9kZWwudXJpLnRvU3RyaW5nKCkpKSByZXR1cm4gZW1wdHk7XG4gICAgICAgICAgICAvLyBTZW5kIHRoZSBvcmlnaW5hbCBMU1AgZGlhZ25vc3RpY3MgdGhhdCBvdmVybGFwIHRoZSByYW5nZSAodGhleSBjYXJyeSB0aGUgY29kZS9kYXRhIEpEVC5MU1xuICAgICAgICAgICAgLy8gbmVlZHMgdG8gY29tcHV0ZSBxdWljay1maXhlcyksIG5vdCBvbmVzIHJlY29uc3RydWN0ZWQgZnJvbSBNb25hY28gbWFya2Vycy5cbiAgICAgICAgICAgIGNvbnN0IGxzcFJhbmdlID0gbW9uYWNvUmFuZ2VUb0xzcChyYW5nZSk7XG4gICAgICAgICAgICBjb25zdCBkaWFnbm9zdGljcyA9IChfZGlhZ25vc3RpY3MuZ2V0KG1vZGVsLnVyaS50b1N0cmluZygpKSA/PyBbXSkuZmlsdGVyKGQgPT4gcmFuZ2VzT3ZlcmxhcChkLnJhbmdlLCBsc3BSYW5nZSkpO1xuICAgICAgICAgICAgY29uc3QgcmVzdWx0OiBBcnJheTxDb2RlQWN0aW9uIHwgQ29tbWFuZD4gfCBudWxsID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ3RleHREb2N1bWVudC9jb2RlQWN0aW9uJywge1xuICAgICAgICAgICAgICAgIHRleHREb2N1bWVudDogeyB1cmk6IG1vZGVsLnVyaS50b1N0cmluZygpIH0sXG4gICAgICAgICAgICAgICAgcmFuZ2U6ICAgICAgICBsc3BSYW5nZSxcbiAgICAgICAgICAgICAgICAvLyBNb25hY28ncyBDb2RlQWN0aW9uVHJpZ2dlclR5cGUgKEludm9rZT0xLCBBdXRvPTIpIG1hcHMgMToxIHRvIHRoZSBMU1AgdHJpZ2dlciBraW5kLlxuICAgICAgICAgICAgICAgIC8vIEZvcndhcmRpbmcgaXQgbGV0cyBKRFQuTFMgY29tcHV0ZSBvbmx5IHF1aWNrLWZpeGVzIGZvciB0aGUgcGFzc2l2ZSBsaWdodGJ1bGIgKGNoZWFwKVxuICAgICAgICAgICAgICAgIC8vIGFuZCB0aGUgZnVsbCBhc3Npc3RzL3JlZmFjdG9yaW5ncyBvbmx5IG9uIGV4cGxpY2l0IEN0cmwrLiAvIFJlZmFjdG9yXHUyMDI2IChJbnZva2VkKS5cbiAgICAgICAgICAgICAgICBjb250ZXh0OiAgICAgIHsgZGlhZ25vc3RpY3MsIG9ubHk6IGNvbnRleHQub25seSA/IFtjb250ZXh0Lm9ubHldIDogdW5kZWZpbmVkLCB0cmlnZ2VyS2luZDogY29udGV4dC50cmlnZ2VyIH0sXG4gICAgICAgICAgICB9KTtcbiAgICAgICAgICAgIGlmICghcmVzdWx0Py5sZW5ndGgpIHJldHVybiBlbXB0eTtcbiAgICAgICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICAgICAgYWN0aW9uczogcmVzdWx0Lm1hcChsc3BDb2RlQWN0aW9uVG9Nb25hY28pLFxuICAgICAgICAgICAgICAgIGRpc3Bvc2UoKSB7IC8qIG5vdGhpbmcgdG8gZGlzcG9zZSAqLyB9LFxuICAgICAgICAgICAgfTtcbiAgICAgICAgfSxcbiAgICB9LCB7XG4gICAgICAgIHByb3ZpZGVkQ29kZUFjdGlvbktpbmRzOiBbJ3F1aWNrZml4JywgJ3JlZmFjdG9yJywgJ3JlZmFjdG9yLmV4dHJhY3QnLCAncmVmYWN0b3IuaW5saW5lJyxcbiAgICAgICAgICAgICdyZWZhY3Rvci5yZXdyaXRlJywgJ3NvdXJjZScsICdzb3VyY2Uub3JnYW5pemVJbXBvcnRzJ10sXG4gICAgfSk7XG5cbiAgICAvLyAtLS0gTmF2aWdhdGlvbiAmIHN0cnVjdHVyZSAoUGFjayAxKSAtLS1cblxuICAgIG1vbmFjby5sYW5ndWFnZXMucmVnaXN0ZXJJbXBsZW1lbnRhdGlvblByb3ZpZGVyKCdqYXZhJywge1xuICAgICAgICBwcm92aWRlSW1wbGVtZW50YXRpb246IChtb2RlbCwgcG9zaXRpb24pID0+IHJlcXVlc3RMb2NhdGlvbnMoJ3RleHREb2N1bWVudC9pbXBsZW1lbnRhdGlvbicsIG1vZGVsLCBwb3NpdGlvbiksXG4gICAgfSk7XG5cbiAgICBtb25hY28ubGFuZ3VhZ2VzLnJlZ2lzdGVyVHlwZURlZmluaXRpb25Qcm92aWRlcignamF2YScsIHtcbiAgICAgICAgcHJvdmlkZVR5cGVEZWZpbml0aW9uOiAobW9kZWwsIHBvc2l0aW9uKSA9PiByZXF1ZXN0TG9jYXRpb25zKCd0ZXh0RG9jdW1lbnQvdHlwZURlZmluaXRpb24nLCBtb2RlbCwgcG9zaXRpb24pLFxuICAgIH0pO1xuXG4gICAgbW9uYWNvLmxhbmd1YWdlcy5yZWdpc3RlckRvY3VtZW50SGlnaGxpZ2h0UHJvdmlkZXIoJ2phdmEnLCB7XG4gICAgICAgIHByb3ZpZGVEb2N1bWVudEhpZ2hsaWdodHM6IGFzeW5jIChtb2RlbCwgcG9zaXRpb24pID0+IHtcbiAgICAgICAgICAgIGlmICghX2Nvbm4gfHwgIWlzV29ya3NwYWNlRmlsZShtb2RlbC51cmkudG9TdHJpbmcoKSkpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgY29uc3QgcmVzdWx0OiBhbnlbXSB8IG51bGwgPSBhd2FpdCBfY29ubi5zZW5kUmVxdWVzdCgndGV4dERvY3VtZW50L2RvY3VtZW50SGlnaGxpZ2h0Jywge1xuICAgICAgICAgICAgICAgIHRleHREb2N1bWVudDogeyB1cmk6IG1vZGVsLnVyaS50b1N0cmluZygpIH0sXG4gICAgICAgICAgICAgICAgcG9zaXRpb246ICAgICB7IGxpbmU6IHBvc2l0aW9uLmxpbmVOdW1iZXIgLSAxLCBjaGFyYWN0ZXI6IHBvc2l0aW9uLmNvbHVtbiAtIDEgfSxcbiAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgaWYgKCFyZXN1bHQpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgcmV0dXJuIHJlc3VsdC5tYXAoaCA9PiAoeyByYW5nZTogbHNwUmFuZ2VUb01vbmFjbyhoLnJhbmdlKSwga2luZDogaC5raW5kID8gaC5raW5kIC0gMSA6IHVuZGVmaW5lZCB9KSk7XG4gICAgICAgIH0sXG4gICAgfSk7XG5cbiAgICBtb25hY28ubGFuZ3VhZ2VzLnJlZ2lzdGVyRG9jdW1lbnRTeW1ib2xQcm92aWRlcignamF2YScsIHtcbiAgICAgICAgcHJvdmlkZURvY3VtZW50U3ltYm9sczogYXN5bmMgKG1vZGVsKSA9PiB7XG4gICAgICAgICAgICBpZiAoIV9jb25uIHx8ICFpc1dvcmtzcGFjZUZpbGUobW9kZWwudXJpLnRvU3RyaW5nKCkpKSByZXR1cm4gbnVsbDtcbiAgICAgICAgICAgIGNvbnN0IHJlc3VsdDogYW55W10gfCBudWxsID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ3RleHREb2N1bWVudC9kb2N1bWVudFN5bWJvbCcsIHtcbiAgICAgICAgICAgICAgICB0ZXh0RG9jdW1lbnQ6IHsgdXJpOiBtb2RlbC51cmkudG9TdHJpbmcoKSB9LFxuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICByZXR1cm4gcmVzdWx0ID8gbWFwRG9jdW1lbnRTeW1ib2xzKHJlc3VsdCkgOiBudWxsO1xuICAgICAgICB9LFxuICAgIH0pO1xuXG4gICAgbW9uYWNvLmxhbmd1YWdlcy5yZWdpc3RlckZvbGRpbmdSYW5nZVByb3ZpZGVyKCdqYXZhJywge1xuICAgICAgICBwcm92aWRlRm9sZGluZ1JhbmdlczogYXN5bmMgKG1vZGVsKSA9PiB7XG4gICAgICAgICAgICBpZiAoIV9jb25uIHx8ICFpc1dvcmtzcGFjZUZpbGUobW9kZWwudXJpLnRvU3RyaW5nKCkpKSByZXR1cm4gbnVsbDtcbiAgICAgICAgICAgIGNvbnN0IHJlc3VsdDogYW55W10gfCBudWxsID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ3RleHREb2N1bWVudC9mb2xkaW5nUmFuZ2UnLCB7XG4gICAgICAgICAgICAgICAgdGV4dERvY3VtZW50OiB7IHVyaTogbW9kZWwudXJpLnRvU3RyaW5nKCkgfSxcbiAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgaWYgKCFyZXN1bHQpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgcmV0dXJuIHJlc3VsdC5tYXAociA9PiAoeyBzdGFydDogci5zdGFydExpbmUgKyAxLCBlbmQ6IHIuZW5kTGluZSArIDEsIGtpbmQ6IGZvbGRpbmdLaW5kKHIua2luZCkgfSkpO1xuICAgICAgICB9LFxuICAgIH0pO1xuXG4gICAgbW9uYWNvLmxhbmd1YWdlcy5yZWdpc3RlclNlbGVjdGlvblJhbmdlUHJvdmlkZXIoJ2phdmEnLCB7XG4gICAgICAgIHByb3ZpZGVTZWxlY3Rpb25SYW5nZXM6IGFzeW5jIChtb2RlbCwgcG9zaXRpb25zKSA9PiB7XG4gICAgICAgICAgICBpZiAoIV9jb25uIHx8ICFpc1dvcmtzcGFjZUZpbGUobW9kZWwudXJpLnRvU3RyaW5nKCkpKSByZXR1cm4gbnVsbDtcbiAgICAgICAgICAgIGNvbnN0IHJlc3VsdDogYW55W10gfCBudWxsID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ3RleHREb2N1bWVudC9zZWxlY3Rpb25SYW5nZScsIHtcbiAgICAgICAgICAgICAgICB0ZXh0RG9jdW1lbnQ6IHsgdXJpOiBtb2RlbC51cmkudG9TdHJpbmcoKSB9LFxuICAgICAgICAgICAgICAgIHBvc2l0aW9uczogICAgcG9zaXRpb25zLm1hcChwID0+ICh7IGxpbmU6IHAubGluZU51bWJlciAtIDEsIGNoYXJhY3RlcjogcC5jb2x1bW4gLSAxIH0pKSxcbiAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgaWYgKCFyZXN1bHQpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgcmV0dXJuIHJlc3VsdC5tYXAoZmxhdHRlblNlbGVjdGlvblJhbmdlKTtcbiAgICAgICAgfSxcbiAgICB9KTtcblxuICAgIG1vbmFjby5sYW5ndWFnZXMucmVnaXN0ZXJEb2N1bWVudFJhbmdlRm9ybWF0dGluZ0VkaXRQcm92aWRlcignamF2YScsIHtcbiAgICAgICAgcHJvdmlkZURvY3VtZW50UmFuZ2VGb3JtYXR0aW5nRWRpdHM6IGFzeW5jIChtb2RlbCwgcmFuZ2UpID0+IHtcbiAgICAgICAgICAgIGlmICghX2Nvbm4gfHwgIWlzV29ya3NwYWNlRmlsZShtb2RlbC51cmkudG9TdHJpbmcoKSkpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgY29uc3QgZWRpdHM6IFRleHRFZGl0W10gfCBudWxsID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QoJ3RleHREb2N1bWVudC9yYW5nZUZvcm1hdHRpbmcnLCB7XG4gICAgICAgICAgICAgICAgdGV4dERvY3VtZW50OiB7IHVyaTogbW9kZWwudXJpLnRvU3RyaW5nKCkgfSxcbiAgICAgICAgICAgICAgICByYW5nZTogICAgICAgIG1vbmFjb1JhbmdlVG9Mc3AocmFuZ2UpLFxuICAgICAgICAgICAgICAgIG9wdGlvbnM6ICAgICAgeyB0YWJTaXplOiBtb2RlbC5nZXRPcHRpb25zKCkudGFiU2l6ZSwgaW5zZXJ0U3BhY2VzOiBtb2RlbC5nZXRPcHRpb25zKCkuaW5zZXJ0U3BhY2VzIH0sXG4gICAgICAgICAgICB9KTtcbiAgICAgICAgICAgIHJldHVybiBlZGl0cyA/IGVkaXRzLm1hcCh0ZXh0RWRpdFRvTW9uYWNvKSA6IG51bGw7XG4gICAgICAgIH0sXG4gICAgfSk7XG5cbiAgICAvLyAtLS0gSW5sYXkgaGludHMgKyBzZW1hbnRpYyBoaWdobGlnaHRpbmcgKFBhY2sgMikgLS0tXG5cbiAgICBtb25hY28ubGFuZ3VhZ2VzLnJlZ2lzdGVySW5sYXlIaW50c1Byb3ZpZGVyKCdqYXZhJywge1xuICAgICAgICBwcm92aWRlSW5sYXlIaW50czogYXN5bmMgKG1vZGVsLCByYW5nZSkgPT4ge1xuICAgICAgICAgICAgaWYgKCFfY29ubiB8fCAhaXNXb3Jrc3BhY2VGaWxlKG1vZGVsLnVyaS50b1N0cmluZygpKSkgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICBjb25zdCByZXN1bHQ6IGFueVtdIHwgbnVsbCA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCd0ZXh0RG9jdW1lbnQvaW5sYXlIaW50Jywge1xuICAgICAgICAgICAgICAgIHRleHREb2N1bWVudDogeyB1cmk6IG1vZGVsLnVyaS50b1N0cmluZygpIH0sXG4gICAgICAgICAgICAgICAgcmFuZ2U6ICAgICAgICBtb25hY29SYW5nZVRvTHNwKHJhbmdlKSxcbiAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgaWYgKCFyZXN1bHQpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgcmV0dXJuIHtcbiAgICAgICAgICAgICAgICBoaW50czogcmVzdWx0Lm1hcChoID0+ICh7XG4gICAgICAgICAgICAgICAgICAgIHBvc2l0aW9uOiAgICAgeyBsaW5lTnVtYmVyOiBoLnBvc2l0aW9uLmxpbmUgKyAxLCBjb2x1bW46IGgucG9zaXRpb24uY2hhcmFjdGVyICsgMSB9LFxuICAgICAgICAgICAgICAgICAgICBsYWJlbDogICAgICAgIHR5cGVvZiBoLmxhYmVsID09PSAnc3RyaW5nJyA/IGgubGFiZWwgOiAoaC5sYWJlbCA/PyBbXSkubWFwKChwOiBhbnkpID0+ICh7IGxhYmVsOiBwLnZhbHVlIH0pKSxcbiAgICAgICAgICAgICAgICAgICAga2luZDogICAgICAgICBoLmtpbmQsXG4gICAgICAgICAgICAgICAgICAgIHBhZGRpbmdMZWZ0OiAgaC5wYWRkaW5nTGVmdCxcbiAgICAgICAgICAgICAgICAgICAgcGFkZGluZ1JpZ2h0OiBoLnBhZGRpbmdSaWdodCxcbiAgICAgICAgICAgICAgICAgICAgdG9vbHRpcDogICAgICBoLnRvb2x0aXAgPyBtYXJrdXBUb1N0cmluZyhoLnRvb2x0aXApIDogdW5kZWZpbmVkLFxuICAgICAgICAgICAgICAgIH0pKSxcbiAgICAgICAgICAgICAgICBkaXNwb3NlKCkgeyAvKiBub3RoaW5nIHRvIGRpc3Bvc2UgKi8gfSxcbiAgICAgICAgICAgIH07XG4gICAgICAgIH0sXG4gICAgfSk7XG5cbiAgICBtb25hY28ubGFuZ3VhZ2VzLnJlZ2lzdGVyRG9jdW1lbnRTZW1hbnRpY1Rva2Vuc1Byb3ZpZGVyKCdqYXZhJywge1xuICAgICAgICBnZXRMZWdlbmQ6ICgpID0+IF9zZW1hbnRpY1Rva2Vuc0xlZ2VuZCA/PyB7IHRva2VuVHlwZXM6IFtdLCB0b2tlbk1vZGlmaWVyczogW10gfSxcbiAgICAgICAgcHJvdmlkZURvY3VtZW50U2VtYW50aWNUb2tlbnM6IGFzeW5jIChtb2RlbCkgPT4ge1xuICAgICAgICAgICAgaWYgKCFfY29ubiB8fCAhaXNXb3Jrc3BhY2VGaWxlKG1vZGVsLnVyaS50b1N0cmluZygpKSB8fCAhX3NlbWFudGljVG9rZW5zTGVnZW5kKSByZXR1cm4gbnVsbDtcbiAgICAgICAgICAgIGNvbnN0IHJlc3VsdDogeyBkYXRhOiBudW1iZXJbXTsgcmVzdWx0SWQ/OiBzdHJpbmcgfSB8IG51bGwgPSBhd2FpdCBfY29ubi5zZW5kUmVxdWVzdCgndGV4dERvY3VtZW50L3NlbWFudGljVG9rZW5zL2Z1bGwnLCB7XG4gICAgICAgICAgICAgICAgdGV4dERvY3VtZW50OiB7IHVyaTogbW9kZWwudXJpLnRvU3RyaW5nKCkgfSxcbiAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgaWYgKCFyZXN1bHQ/LmRhdGEpIHJldHVybiBudWxsO1xuICAgICAgICAgICAgcmV0dXJuIHsgZGF0YTogbmV3IFVpbnQzMkFycmF5KHJlc3VsdC5kYXRhKSwgcmVzdWx0SWQ6IHJlc3VsdC5yZXN1bHRJZCB9O1xuICAgICAgICB9LFxuICAgICAgICByZWxlYXNlRG9jdW1lbnRTZW1hbnRpY1Rva2VuczogKCkgPT4geyAvKiBub3RoaW5nIHRvIHJlbGVhc2UgKi8gfSxcbiAgICB9KTtcblxuICAgIC8vIC0tLSBDb2RlTGVucyAoUGFjayAzKSAtLS1cblxuICAgIG1vbmFjby5sYW5ndWFnZXMucmVnaXN0ZXJDb2RlTGVuc1Byb3ZpZGVyKCdqYXZhJywge1xuICAgICAgICBwcm92aWRlQ29kZUxlbnNlczogYXN5bmMgKG1vZGVsKSA9PiB7XG4gICAgICAgICAgICBpZiAoIV9jb25uIHx8ICFpc1dvcmtzcGFjZUZpbGUobW9kZWwudXJpLnRvU3RyaW5nKCkpKSByZXR1cm4geyBsZW5zZXM6IFtdLCBkaXNwb3NlKCkgeyAvKiAqLyB9IH07XG4gICAgICAgICAgICBjb25zdCByZXN1bHQ6IGFueVtdIHwgbnVsbCA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCd0ZXh0RG9jdW1lbnQvY29kZUxlbnMnLCB7XG4gICAgICAgICAgICAgICAgdGV4dERvY3VtZW50OiB7IHVyaTogbW9kZWwudXJpLnRvU3RyaW5nKCkgfSxcbiAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgY29uc3QgbGVuc2VzID0gKHJlc3VsdCA/PyBbXSkubWFwKChsZW5zLCBpKSA9PiAoe1xuICAgICAgICAgICAgICAgIHJhbmdlOiAgICBsc3BSYW5nZVRvTW9uYWNvKGxlbnMucmFuZ2UpLFxuICAgICAgICAgICAgICAgIGlkOiAgICAgICBTdHJpbmcoaSksXG4gICAgICAgICAgICAgICAgY29tbWFuZDogIGxlbnMuY29tbWFuZCA/IG1hcExlbnNDb21tYW5kKGxlbnMuY29tbWFuZCkgOiB1bmRlZmluZWQsXG4gICAgICAgICAgICAgICAgX2xzcDogICAgIGxlbnMsXG4gICAgICAgICAgICB9KSk7XG4gICAgICAgICAgICByZXR1cm4geyBsZW5zZXMsIGRpc3Bvc2UoKSB7IC8qICovIH0gfTtcbiAgICAgICAgfSxcbiAgICAgICAgcmVzb2x2ZUNvZGVMZW5zOiBhc3luYyAoX21vZGVsLCBjb2RlTGVucykgPT4ge1xuICAgICAgICAgICAgY29uc3QgbHNwID0gKGNvZGVMZW5zIGFzIGFueSkuX2xzcDtcbiAgICAgICAgICAgIGlmIChfY29ubiAmJiBsc3AgJiYgIWxzcC5jb21tYW5kKSB7XG4gICAgICAgICAgICAgICAgdHJ5IHtcbiAgICAgICAgICAgICAgICAgICAgY29uc3QgcmVzb2x2ZWQ6IGFueSA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCdjb2RlTGVucy9yZXNvbHZlJywgbHNwKTtcbiAgICAgICAgICAgICAgICAgICAgY29kZUxlbnMuY29tbWFuZCA9IHJlc29sdmVkPy5jb21tYW5kID8gbWFwTGVuc0NvbW1hbmQocmVzb2x2ZWQuY29tbWFuZCkgOiB7IGlkOiBOT09QX0NPTU1BTkQsIHRpdGxlOiAnJyB9O1xuICAgICAgICAgICAgICAgIH0gY2F0Y2gge1xuICAgICAgICAgICAgICAgICAgICBjb2RlTGVucy5jb21tYW5kID0geyBpZDogTk9PUF9DT01NQU5ELCB0aXRsZTogJycgfTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICByZXR1cm4gY29kZUxlbnM7XG4gICAgICAgIH0sXG4gICAgfSk7XG5cbiAgICAvLyAtLS0gSmF2YSBrZXl3b3JkIGNvbXBsZXRpb24gKFBhY2sgMWIpOiBhbHdheXMtYXZhaWxhYmxlLCByYW5rZWQgYmVsb3cgU0RLL0xTUCByZXN1bHRzIC0tLVxuXG4gICAgbW9uYWNvLmxhbmd1YWdlcy5yZWdpc3RlckNvbXBsZXRpb25JdGVtUHJvdmlkZXIoJ2phdmEnLCB7XG4gICAgICAgIHByb3ZpZGVDb21wbGV0aW9uSXRlbXM6IChtb2RlbCwgcG9zaXRpb24pID0+IHtcbiAgICAgICAgICAgIGlmICghaXNXb3Jrc3BhY2VGaWxlKG1vZGVsLnVyaS50b1N0cmluZygpKSkgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICBjb25zdCB3b3JkID0gbW9kZWwuZ2V0V29yZFVudGlsUG9zaXRpb24ocG9zaXRpb24pO1xuICAgICAgICAgICAgY29uc3QgcmFuZ2U6IG1vbmFjby5JUmFuZ2UgPSB7XG4gICAgICAgICAgICAgICAgc3RhcnRMaW5lTnVtYmVyOiBwb3NpdGlvbi5saW5lTnVtYmVyLCBzdGFydENvbHVtbjogd29yZC5zdGFydENvbHVtbixcbiAgICAgICAgICAgICAgICBlbmRMaW5lTnVtYmVyOiAgIHBvc2l0aW9uLmxpbmVOdW1iZXIsIGVuZENvbHVtbjogICB3b3JkLmVuZENvbHVtbixcbiAgICAgICAgICAgIH07XG4gICAgICAgICAgICByZXR1cm4ge1xuICAgICAgICAgICAgICAgIHN1Z2dlc3Rpb25zOiBKQVZBX0tFWVdPUkRTLm1hcChrZXl3b3JkID0+ICh7XG4gICAgICAgICAgICAgICAgICAgIGxhYmVsOiAgICAgIGtleXdvcmQsXG4gICAgICAgICAgICAgICAgICAgIGtpbmQ6ICAgICAgIG1vbmFjby5sYW5ndWFnZXMuQ29tcGxldGlvbkl0ZW1LaW5kLktleXdvcmQsXG4gICAgICAgICAgICAgICAgICAgIGluc2VydFRleHQ6IGtleXdvcmQsXG4gICAgICAgICAgICAgICAgICAgIHJhbmdlLFxuICAgICAgICAgICAgICAgICAgICBzb3J0VGV4dDogICBgOV8ke2tleXdvcmR9YCxcbiAgICAgICAgICAgICAgICB9KSksXG4gICAgICAgICAgICB9O1xuICAgICAgICB9LFxuICAgIH0pO1xufVxuXG4vLyAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4vLyBIZWxwZXJzXG4vLyAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG5cbmZ1bmN0aW9uIGxzcFNldmVyaXR5KHNldmVyaXR5OiBEaWFnbm9zdGljU2V2ZXJpdHkgfCB1bmRlZmluZWQpOiBtb25hY28uTWFya2VyU2V2ZXJpdHkge1xuICAgIHN3aXRjaCAoc2V2ZXJpdHkpIHtcbiAgICAgICAgY2FzZSBEaWFnbm9zdGljU2V2ZXJpdHkuRXJyb3I6ICAgICAgIHJldHVybiBtb25hY28uTWFya2VyU2V2ZXJpdHkuRXJyb3I7XG4gICAgICAgIGNhc2UgRGlhZ25vc3RpY1NldmVyaXR5Lldhcm5pbmc6ICAgICByZXR1cm4gbW9uYWNvLk1hcmtlclNldmVyaXR5Lldhcm5pbmc7XG4gICAgICAgIGNhc2UgRGlhZ25vc3RpY1NldmVyaXR5LkluZm9ybWF0aW9uOiByZXR1cm4gbW9uYWNvLk1hcmtlclNldmVyaXR5LkluZm87XG4gICAgICAgIGNhc2UgRGlhZ25vc3RpY1NldmVyaXR5LkhpbnQ6ICAgICAgICByZXR1cm4gbW9uYWNvLk1hcmtlclNldmVyaXR5LkhpbnQ7XG4gICAgICAgIGRlZmF1bHQ6ICAgICAgICAgICAgICAgICAgICAgICAgICAgICByZXR1cm4gbW9uYWNvLk1hcmtlclNldmVyaXR5LkVycm9yO1xuICAgIH1cbn1cblxuZnVuY3Rpb24gbHNwUmFuZ2VUb01vbmFjbyhyOiB7IHN0YXJ0OiB7IGxpbmU6IG51bWJlcjsgY2hhcmFjdGVyOiBudW1iZXIgfTsgZW5kOiB7IGxpbmU6IG51bWJlcjsgY2hhcmFjdGVyOiBudW1iZXIgfSB9KTogbW9uYWNvLklSYW5nZSB7XG4gICAgcmV0dXJuIHtcbiAgICAgICAgc3RhcnRMaW5lTnVtYmVyOiByLnN0YXJ0LmxpbmUgKyAxLFxuICAgICAgICBzdGFydENvbHVtbjogICAgIHIuc3RhcnQuY2hhcmFjdGVyICsgMSxcbiAgICAgICAgZW5kTGluZU51bWJlcjogICByLmVuZC5saW5lICsgMSxcbiAgICAgICAgZW5kQ29sdW1uOiAgICAgICByLmVuZC5jaGFyYWN0ZXIgKyAxLFxuICAgIH07XG59XG5cbmZ1bmN0aW9uIG1hcmt1cFRvU3RyaW5nKGM6IHN0cmluZyB8IE1hcmt1cENvbnRlbnQpOiBzdHJpbmcge1xuICAgIHJldHVybiB0eXBlb2YgYyA9PT0gJ3N0cmluZycgPyBjIDogYy52YWx1ZTtcbn1cblxuZnVuY3Rpb24gbHNwQ29tcGxldGlvbktpbmQoa2luZDogQ29tcGxldGlvbkl0ZW1LaW5kIHwgdW5kZWZpbmVkKTogbW9uYWNvLmxhbmd1YWdlcy5Db21wbGV0aW9uSXRlbUtpbmQge1xuICAgIGNvbnN0IEsgPSBtb25hY28ubGFuZ3VhZ2VzLkNvbXBsZXRpb25JdGVtS2luZDtcbiAgICBzd2l0Y2ggKGtpbmQpIHtcbiAgICAgICAgY2FzZSBDb21wbGV0aW9uSXRlbUtpbmQuVGV4dDogICAgICAgICAgcmV0dXJuIEsuVGV4dDtcbiAgICAgICAgY2FzZSBDb21wbGV0aW9uSXRlbUtpbmQuTWV0aG9kOiAgICAgICAgcmV0dXJuIEsuTWV0aG9kO1xuICAgICAgICBjYXNlIENvbXBsZXRpb25JdGVtS2luZC5GdW5jdGlvbjogICAgICByZXR1cm4gSy5GdW5jdGlvbjtcbiAgICAgICAgY2FzZSBDb21wbGV0aW9uSXRlbUtpbmQuQ29uc3RydWN0b3I6ICAgcmV0dXJuIEsuQ29uc3RydWN0b3I7XG4gICAgICAgIGNhc2UgQ29tcGxldGlvbkl0ZW1LaW5kLkZpZWxkOiAgICAgICAgIHJldHVybiBLLkZpZWxkO1xuICAgICAgICBjYXNlIENvbXBsZXRpb25JdGVtS2luZC5WYXJpYWJsZTogICAgICByZXR1cm4gSy5WYXJpYWJsZTtcbiAgICAgICAgY2FzZSBDb21wbGV0aW9uSXRlbUtpbmQuQ2xhc3M6ICAgICAgICAgcmV0dXJuIEsuQ2xhc3M7XG4gICAgICAgIGNhc2UgQ29tcGxldGlvbkl0ZW1LaW5kLkludGVyZmFjZTogICAgIHJldHVybiBLLkludGVyZmFjZTtcbiAgICAgICAgY2FzZSBDb21wbGV0aW9uSXRlbUtpbmQuTW9kdWxlOiAgICAgICAgcmV0dXJuIEsuTW9kdWxlO1xuICAgICAgICBjYXNlIENvbXBsZXRpb25JdGVtS2luZC5Qcm9wZXJ0eTogICAgICByZXR1cm4gSy5Qcm9wZXJ0eTtcbiAgICAgICAgY2FzZSBDb21wbGV0aW9uSXRlbUtpbmQuS2V5d29yZDogICAgICAgcmV0dXJuIEsuS2V5d29yZDtcbiAgICAgICAgY2FzZSBDb21wbGV0aW9uSXRlbUtpbmQuU25pcHBldDogICAgICAgcmV0dXJuIEsuU25pcHBldDtcbiAgICAgICAgY2FzZSBDb21wbGV0aW9uSXRlbUtpbmQuQ29uc3RhbnQ6ICAgICAgcmV0dXJuIEsuQ29uc3RhbnQ7XG4gICAgICAgIGNhc2UgQ29tcGxldGlvbkl0ZW1LaW5kLlN0cnVjdDogICAgICAgIHJldHVybiBLLlN0cnVjdDtcbiAgICAgICAgY2FzZSBDb21wbGV0aW9uSXRlbUtpbmQuVHlwZVBhcmFtZXRlcjogcmV0dXJuIEsuVHlwZVBhcmFtZXRlcjtcbiAgICAgICAgZGVmYXVsdDogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuIEsuVGV4dDtcbiAgICB9XG59XG5cbmZ1bmN0aW9uIGxzcENvbXBsZXRpb25Ub01vbmFjbyhcbiAgICBpdGVtOiBDb21wbGV0aW9uSXRlbSxcbiAgICBtb2RlbDogbW9uYWNvLmVkaXRvci5JVGV4dE1vZGVsLFxuICAgIHBvc2l0aW9uOiBtb25hY28uUG9zaXRpb24sXG4pOiBNb25hY29Db21wbGV0aW9uSXRlbSB7XG4gICAgY29uc3Qgd29yZCA9IG1vZGVsLmdldFdvcmRVbnRpbFBvc2l0aW9uKHBvc2l0aW9uKTtcbiAgICBsZXQgcmFuZ2U6IG1vbmFjby5JUmFuZ2UgPSB7XG4gICAgICAgIHN0YXJ0TGluZU51bWJlcjogcG9zaXRpb24ubGluZU51bWJlcixcbiAgICAgICAgc3RhcnRDb2x1bW46ICAgICB3b3JkLnN0YXJ0Q29sdW1uLFxuICAgICAgICBlbmRMaW5lTnVtYmVyOiAgIHBvc2l0aW9uLmxpbmVOdW1iZXIsXG4gICAgICAgIGVuZENvbHVtbjogICAgICAgd29yZC5lbmRDb2x1bW4sXG4gICAgfTtcbiAgICBsZXQgaW5zZXJ0VGV4dCA9IGl0ZW0uaW5zZXJ0VGV4dCA/PyBpdGVtLmxhYmVsO1xuICAgIGNvbnN0IHRleHRFZGl0ID0gaXRlbS50ZXh0RWRpdCBhcyB7IHJhbmdlPzogTHNwUmFuZ2U7IHJlcGxhY2U/OiBMc3BSYW5nZTsgaW5zZXJ0PzogTHNwUmFuZ2U7IG5ld1RleHQ/OiBzdHJpbmcgfSB8IHVuZGVmaW5lZDtcbiAgICBpZiAodGV4dEVkaXQpIHtcbiAgICAgICAgY29uc3QgciA9IHRleHRFZGl0LnJhbmdlID8/IHRleHRFZGl0LnJlcGxhY2UgPz8gdGV4dEVkaXQuaW5zZXJ0O1xuICAgICAgICBpZiAocikgcmFuZ2UgPSBsc3BSYW5nZVRvTW9uYWNvKHIpO1xuICAgICAgICBpZiAodHlwZW9mIHRleHRFZGl0Lm5ld1RleHQgPT09ICdzdHJpbmcnKSBpbnNlcnRUZXh0ID0gdGV4dEVkaXQubmV3VGV4dDtcbiAgICB9XG4gICAgY29uc3QgcmVzdWx0OiBNb25hY29Db21wbGV0aW9uSXRlbSA9IHtcbiAgICAgICAgbGFiZWw6ICAgICAgICAgICBpdGVtLmxhYmVsLFxuICAgICAgICBraW5kOiAgICAgICAgICAgIGxzcENvbXBsZXRpb25LaW5kKGl0ZW0ua2luZCksXG4gICAgICAgIGRldGFpbDogICAgICAgICAgaXRlbS5kZXRhaWwsXG4gICAgICAgIGRvY3VtZW50YXRpb246ICAgaXRlbS5kb2N1bWVudGF0aW9uID8geyB2YWx1ZTogbWFya3VwVG9TdHJpbmcoaXRlbS5kb2N1bWVudGF0aW9uKSwgaXNUcnVzdGVkOiBmYWxzZSB9IDogdW5kZWZpbmVkLFxuICAgICAgICBpbnNlcnRUZXh0LFxuICAgICAgICBpbnNlcnRUZXh0UnVsZXM6IGl0ZW0uaW5zZXJ0VGV4dEZvcm1hdCA9PT0gSW5zZXJ0VGV4dEZvcm1hdC5TbmlwcGV0XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgID8gbW9uYWNvLmxhbmd1YWdlcy5Db21wbGV0aW9uSXRlbUluc2VydFRleHRSdWxlLkluc2VydEFzU25pcHBldFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICA6IHVuZGVmaW5lZCxcbiAgICAgICAgcmFuZ2UsXG4gICAgICAgIHNvcnRUZXh0OiAgICAgICAgICAgIHNka1ByaW9yaXRpc2VkU29ydFRleHQoaXRlbSksXG4gICAgICAgIGZpbHRlclRleHQ6ICAgICAgICAgIGl0ZW0uZmlsdGVyVGV4dCxcbiAgICAgICAgcHJlc2VsZWN0OiAgICAgICAgICAgaXRlbS5wcmVzZWxlY3QsXG4gICAgICAgIGNvbW1pdENoYXJhY3RlcnM6ICAgIGl0ZW0uY29tbWl0Q2hhcmFjdGVycyxcbiAgICAgICAgYWRkaXRpb25hbFRleHRFZGl0czogaXRlbS5hZGRpdGlvbmFsVGV4dEVkaXRzPy5tYXAodGV4dEVkaXRUb01vbmFjbyksXG4gICAgfTtcbiAgICByZXN1bHQuX2xzcCA9IGl0ZW07XG4gICAgcmV0dXJuIHJlc3VsdDtcbn1cblxuLyoqXG4gKiBSYW5rcyBEaXJpZ2libGUgU0RLIHN1Z2dlc3Rpb25zICh7QGNvZGUgb3JnLmVjbGlwc2UuZGlyaWdpYmxlLnNkay4qfSkgYWJvdmUgZXZlcnl0aGluZyBlbHNlIGJ5XG4gKiBwcmVmaXhpbmcgdGhlIHNlcnZlciBzb3J0VGV4dCB3aXRoIGEgcHJpb3JpdHkgYnVja2V0LCBwcmVzZXJ2aW5nIHRoZSBzZXJ2ZXIgb3JkZXIgd2l0aGluIGVhY2ggYnVja2V0LlxuICovXG5mdW5jdGlvbiBzZGtQcmlvcml0aXNlZFNvcnRUZXh0KGl0ZW06IENvbXBsZXRpb25JdGVtKTogc3RyaW5nIHtcbiAgICBjb25zdCBiYXNlID0gaXRlbS5zb3J0VGV4dCA/PyAodHlwZW9mIGl0ZW0ubGFiZWwgPT09ICdzdHJpbmcnID8gaXRlbS5sYWJlbCA6ICcnKTtcbiAgICBjb25zdCBkZXNjcmlwdGlvbiA9IChpdGVtLmxhYmVsRGV0YWlscyAmJiB0eXBlb2YgaXRlbS5sYWJlbERldGFpbHMuZGVzY3JpcHRpb24gPT09ICdzdHJpbmcnKVxuICAgICAgICA/IGl0ZW0ubGFiZWxEZXRhaWxzLmRlc2NyaXB0aW9uIDogJyc7XG4gICAgY29uc3QgaGF5c3RhY2sgPSBgJHtpdGVtLmRldGFpbCA/PyAnJ30gJHtkZXNjcmlwdGlvbn1gO1xuICAgIHJldHVybiBoYXlzdGFjay5pbmNsdWRlcygnb3JnLmVjbGlwc2UuZGlyaWdpYmxlLnNkaycpID8gYDBfJHtiYXNlfWAgOiBgMV8ke2Jhc2V9YDtcbn1cblxuLy8gLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuLy8gQ29kZSBhY3Rpb25zLCBjb21tYW5kcywgcmVmYWN0b3IvZ2VuZXJhdGUsIHdvcmtzcGFjZSBlZGl0c1xuLy8gLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuXG4vKiogTW9uYWNvIGNvbW1hbmQgaWQgdXNlZCB0byBhcHBseSBhIGRlZmVycmVkIExTUCBjb2RlIGFjdGlvbiB3aGVuIHRoZSB1c2VyIHNlbGVjdHMgaXQuICovXG5jb25zdCBBUFBMWV9BQ1RJT05fQ09NTUFORCA9ICdkaXJpZ2libGUuamF2YS5hcHBseUNvZGVBY3Rpb24nO1xuXG4vKiogVmlydHVhbCBVUkkgcHJlZml4IG9mIGVkaXRvciBtb2RlbHM7IHN0cmlwcGluZyBpdCB5aWVsZHMgdGhlIElERSB3b3Jrc3BhY2UgcGF0aCAoL3dzL3Byb2ovLi4uKS4gKi9cbmNvbnN0IFZJUlRVQUxfRklMRV9QUkVGSVggPSAnZmlsZTovLy93b3Jrc3BhY2UnO1xuXG4vKiogTW9uYWNvIGNvbW1hbmQgaWQgdXNlZCBmb3IgZGlzcGxheS1vbmx5IENvZGVMZW5zZXMgKGUuZy4gYSByZWZlcmVuY2UgY291bnQgd2l0aCBubyByZXNvbHZlZCBhY3Rpb24pLiAqL1xuY29uc3QgTk9PUF9DT01NQU5EID0gJ2RpcmlnaWJsZS5qYXZhLm5vb3BMZW5zJztcblxuLyoqIEphdmEgU0Uga2V5d29yZHMvbGl0ZXJhbHMgb2ZmZXJlZCBhcyBsb3ctcHJpb3JpdHkgY29tcGxldGlvbiBzbyB0aGV5IGFsd2F5cyBhcHBlYXIgd2hpbGUgdHlwaW5nLiAqL1xuY29uc3QgSkFWQV9LRVlXT1JEUyA9IFsnYWJzdHJhY3QnLCAnYXNzZXJ0JywgJ2Jvb2xlYW4nLCAnYnJlYWsnLCAnYnl0ZScsICdjYXNlJywgJ2NhdGNoJywgJ2NoYXInLCAnY2xhc3MnLFxuICAgICdjb25zdCcsICdjb250aW51ZScsICdkZWZhdWx0JywgJ2RvJywgJ2RvdWJsZScsICdlbHNlJywgJ2VudW0nLCAnZXh0ZW5kcycsICdmaW5hbCcsICdmaW5hbGx5JywgJ2Zsb2F0JyxcbiAgICAnZm9yJywgJ2dvdG8nLCAnaWYnLCAnaW1wbGVtZW50cycsICdpbXBvcnQnLCAnaW5zdGFuY2VvZicsICdpbnQnLCAnaW50ZXJmYWNlJywgJ2xvbmcnLCAnbmF0aXZlJywgJ25ldycsXG4gICAgJ3BhY2thZ2UnLCAncHJpdmF0ZScsICdwcm90ZWN0ZWQnLCAncHVibGljJywgJ3JldHVybicsICdzaG9ydCcsICdzdGF0aWMnLCAnc3RyaWN0ZnAnLCAnc3VwZXInLCAnc3dpdGNoJyxcbiAgICAnc3luY2hyb25pemVkJywgJ3RoaXMnLCAndGhyb3cnLCAndGhyb3dzJywgJ3RyYW5zaWVudCcsICd0cnknLCAndm9pZCcsICd2b2xhdGlsZScsICd3aGlsZScsICd2YXInLFxuICAgICd5aWVsZCcsICdyZWNvcmQnLCAnc2VhbGVkJywgJ3Blcm1pdHMnLCAndHJ1ZScsICdmYWxzZScsICdudWxsJ107XG5cbi8qKiBTaGFyZWQgZGVmaW5pdGlvbi1zdHlsZSBsb2NhdGlvbiByZXF1ZXN0IHVzZWQgYnkgZ28tdG8tZGVmaW5pdGlvbi9pbXBsZW1lbnRhdGlvbi90eXBlLWRlZmluaXRpb24uICovXG5hc3luYyBmdW5jdGlvbiByZXF1ZXN0TG9jYXRpb25zKG1ldGhvZDogc3RyaW5nLCBtb2RlbDogbW9uYWNvLmVkaXRvci5JVGV4dE1vZGVsLCBwb3NpdGlvbjogbW9uYWNvLlBvc2l0aW9uKSB7XG4gICAgaWYgKCFfY29ubiB8fCAhaXNXb3Jrc3BhY2VGaWxlKG1vZGVsLnVyaS50b1N0cmluZygpKSkgcmV0dXJuIG51bGw7XG4gICAgY29uc3QgcmVzdWx0OiBMb2NhdGlvbiB8IExvY2F0aW9uW10gfCBudWxsID0gYXdhaXQgX2Nvbm4uc2VuZFJlcXVlc3QobWV0aG9kLCB7XG4gICAgICAgIHRleHREb2N1bWVudDogeyB1cmk6IG1vZGVsLnVyaS50b1N0cmluZygpIH0sXG4gICAgICAgIHBvc2l0aW9uOiAgICAgeyBsaW5lOiBwb3NpdGlvbi5saW5lTnVtYmVyIC0gMSwgY2hhcmFjdGVyOiBwb3NpdGlvbi5jb2x1bW4gLSAxIH0sXG4gICAgfSk7XG4gICAgaWYgKCFyZXN1bHQpIHJldHVybiBudWxsO1xuICAgIGNvbnN0IGxvY2F0aW9ucyA9IChBcnJheS5pc0FycmF5KHJlc3VsdCkgPyByZXN1bHQgOiBbcmVzdWx0XSkubWFwKGxvYyA9PiAoeyB1cmk6IG1vbmFjby5VcmkucGFyc2UobG9jLnVyaSksIHJhbmdlOiBsc3BSYW5nZVRvTW9uYWNvKGxvYy5yYW5nZSkgfSkpO1xuICAgIGF3YWl0IGVuc3VyZU1vZGVsc0ZvckxvY2F0aW9ucyhsb2NhdGlvbnMpO1xuICAgIHJldHVybiBsb2NhdGlvbnM7XG59XG5cbi8qKlxuICogQ3JlYXRlcyBpbi1tZW1vcnkgTW9uYWNvIG1vZGVscyBmb3IgdGhlIHdvcmtzcGFjZSBmaWxlcyByZWZlcmVuY2VkIGJ5IHRoZSBnaXZlbiBsb2NhdGlvbnMgKGZldGNoZWRcbiAqIG92ZXIgUkVTVCkgc28gdGhlIHJlZmVyZW5jZXMgLyBwZWVrIHdpZGdldHMgY2FuIHJlbmRlciBhIGNvZGUgcHJldmlldyBcdTIwMTQgTW9uYWNvIGNhbiBvbmx5IHByZXZpZXcgZmlsZXNcbiAqIGl0IGhhcyBhIG1vZGVsIGZvciwgYW5kIHRoaXMgc2luZ2xlLWZpbGUgZWRpdG9yIG90aGVyd2lzZSBoYXMgbm9uZSBmb3Igb3RoZXIgZmlsZXMuXG4gKi9cbmFzeW5jIGZ1bmN0aW9uIGVuc3VyZU1vZGVsc0ZvckxvY2F0aW9ucyhsb2NhdGlvbnM6IEFycmF5PHsgdXJpOiBtb25hY28uVXJpIH0+KTogUHJvbWlzZTx2b2lkPiB7XG4gICAgY29uc3Qgc2VlbiA9IG5ldyBTZXQ8c3RyaW5nPigpO1xuICAgIGF3YWl0IFByb21pc2UuYWxsKGxvY2F0aW9ucy5tYXAoYXN5bmMgKHsgdXJpIH0pID0+IHtcbiAgICAgICAgY29uc3QgdXJpU3RyID0gdXJpLnRvU3RyaW5nKCk7XG4gICAgICAgIGlmIChzZWVuLmhhcyh1cmlTdHIpIHx8ICF1cmlTdHIuc3RhcnRzV2l0aChWSVJUVUFMX0ZJTEVfUFJFRklYKSB8fCBtb25hY28uZWRpdG9yLmdldE1vZGVsKHVyaSkpIHJldHVybjtcbiAgICAgICAgc2Vlbi5hZGQodXJpU3RyKTtcbiAgICAgICAgdHJ5IHtcbiAgICAgICAgICAgIGNvbnN0IHRleHQgPSBhd2FpdCBmZXRjaFdvcmtzcGFjZUZpbGVUZXh0KHVyaVRvV29ya3NwYWNlUGF0aCh1cmlTdHIpID8/IHVyaVN0cik7XG4gICAgICAgICAgICBpZiAoIW1vbmFjby5lZGl0b3IuZ2V0TW9kZWwodXJpKSkgbW9uYWNvLmVkaXRvci5jcmVhdGVNb2RlbCh0ZXh0LCAnamF2YScsIHVyaSk7XG4gICAgICAgIH0gY2F0Y2gge1xuICAgICAgICAgICAgLy8gcHJldmlldyBqdXN0IHdvbid0IHJlbmRlciBmb3IgdGhpcyBvbmVcbiAgICAgICAgfVxuICAgIH0pKTtcbn1cblxuLyoqIFJlY3Vyc2l2ZWx5IG1hcHMgTFNQIGhpZXJhcmNoaWNhbCBEb2N1bWVudFN5bWJvbHMgdG8gTW9uYWNvJ3Mgc2hhcGUgKGtpbmRzIGFyZSAxLWJhc2VkIHZzIDAtYmFzZWQpLiAqL1xuZnVuY3Rpb24gbWFwRG9jdW1lbnRTeW1ib2xzKHN5bWJvbHM6IGFueVtdKTogbW9uYWNvLmxhbmd1YWdlcy5Eb2N1bWVudFN5bWJvbFtdIHtcbiAgICByZXR1cm4gKHN5bWJvbHMgPz8gW10pLm1hcChzID0+ICh7XG4gICAgICAgIG5hbWU6ICAgICAgICAgICBzLm5hbWUsXG4gICAgICAgIGRldGFpbDogICAgICAgICBzLmRldGFpbCA/PyAnJyxcbiAgICAgICAga2luZDogICAgICAgICAgIChzLmtpbmQgPz8gMSkgLSAxLFxuICAgICAgICB0YWdzOiAgICAgICAgICAgcy50YWdzID8/IFtdLFxuICAgICAgICByYW5nZTogICAgICAgICAgbHNwUmFuZ2VUb01vbmFjbyhzLnJhbmdlKSxcbiAgICAgICAgc2VsZWN0aW9uUmFuZ2U6IGxzcFJhbmdlVG9Nb25hY28ocy5zZWxlY3Rpb25SYW5nZSA/PyBzLnJhbmdlKSxcbiAgICAgICAgY2hpbGRyZW46ICAgICAgIHMuY2hpbGRyZW4gPyBtYXBEb2N1bWVudFN5bWJvbHMocy5jaGlsZHJlbikgOiBbXSxcbiAgICB9KSk7XG59XG5cbmZ1bmN0aW9uIGZvbGRpbmdLaW5kKGtpbmQ6IHN0cmluZyB8IHVuZGVmaW5lZCk6IG1vbmFjby5sYW5ndWFnZXMuRm9sZGluZ1JhbmdlS2luZCB8IHVuZGVmaW5lZCB7XG4gICAgY29uc3QgRksgPSBtb25hY28ubGFuZ3VhZ2VzLkZvbGRpbmdSYW5nZUtpbmQ7XG4gICAgc3dpdGNoIChraW5kKSB7XG4gICAgICAgIGNhc2UgJ2NvbW1lbnQnOiByZXR1cm4gRksuQ29tbWVudDtcbiAgICAgICAgY2FzZSAnaW1wb3J0cyc6IHJldHVybiBGSy5JbXBvcnRzO1xuICAgICAgICBjYXNlICdyZWdpb24nOiAgcmV0dXJuIEZLLlJlZ2lvbjtcbiAgICAgICAgZGVmYXVsdDogICAgICAgIHJldHVybiB1bmRlZmluZWQ7XG4gICAgfVxufVxuXG4vKiogRmxhdHRlbnMgYW4gTFNQIFNlbGVjdGlvblJhbmdlIHBhcmVudC1jaGFpbiBpbnRvIE1vbmFjbydzIGlubmVybW9zdC10by1vdXRlcm1vc3QgYXJyYXkuICovXG5mdW5jdGlvbiBmbGF0dGVuU2VsZWN0aW9uUmFuZ2Uoc2VsZWN0aW9uUmFuZ2U6IGFueSk6IG1vbmFjby5sYW5ndWFnZXMuU2VsZWN0aW9uUmFuZ2VbXSB7XG4gICAgY29uc3QgcmFuZ2VzOiBtb25hY28ubGFuZ3VhZ2VzLlNlbGVjdGlvblJhbmdlW10gPSBbXTtcbiAgICBsZXQgY3VycmVudCA9IHNlbGVjdGlvblJhbmdlO1xuICAgIHdoaWxlIChjdXJyZW50KSB7XG4gICAgICAgIHJhbmdlcy5wdXNoKHsgcmFuZ2U6IGxzcFJhbmdlVG9Nb25hY28oY3VycmVudC5yYW5nZSkgfSk7XG4gICAgICAgIGN1cnJlbnQgPSBjdXJyZW50LnBhcmVudDtcbiAgICB9XG4gICAgcmV0dXJuIHJhbmdlcztcbn1cblxuLyoqIE1hcHMgYSBKRFQuTFMgQ29kZUxlbnMgY29tbWFuZCB0byBhIE1vbmFjbyBjb21tYW5kLCB3aXJpbmcgdGhlIHJlZmVyZW5jZXMvaW1wbGVtZW50YXRpb25zIHBlZWsuICovXG5mdW5jdGlvbiBtYXBMZW5zQ29tbWFuZChjbWQ6IGFueSk6IG1vbmFjby5sYW5ndWFnZXMuQ29tbWFuZCB7XG4gICAgY29uc3QgYXJncyA9IGNtZC5hcmd1bWVudHMgPz8gW107XG4gICAgaWYgKChjbWQuY29tbWFuZCA9PT0gJ2phdmEuc2hvdy5yZWZlcmVuY2VzJyB8fCBjbWQuY29tbWFuZCA9PT0gJ2phdmEuc2hvdy5pbXBsZW1lbnRhdGlvbnMnKSAmJiBhcmdzLmxlbmd0aCA+PSAzKSB7XG4gICAgICAgIGNvbnN0IGxvY2F0aW9ucyA9IChhcmdzWzJdID8/IFtdKS5tYXAoKGw6IGFueSkgPT4gKHsgdXJpOiBtb25hY28uVXJpLnBhcnNlKGwudXJpKSwgcmFuZ2U6IGxzcFJhbmdlVG9Nb25hY28obC5yYW5nZSkgfSkpO1xuICAgICAgICByZXR1cm4ge1xuICAgICAgICAgICAgaWQ6ICAgICAgICAnZWRpdG9yLmFjdGlvbi5zaG93UmVmZXJlbmNlcycsXG4gICAgICAgICAgICB0aXRsZTogICAgIGNtZC50aXRsZSxcbiAgICAgICAgICAgIGFyZ3VtZW50czogW21vbmFjby5VcmkucGFyc2UoYXJnc1swXSksIHsgbGluZU51bWJlcjogYXJnc1sxXS5saW5lICsgMSwgY29sdW1uOiBhcmdzWzFdLmNoYXJhY3RlciArIDEgfSwgbG9jYXRpb25zXSxcbiAgICAgICAgfTtcbiAgICB9XG4gICAgcmV0dXJuIHsgaWQ6IE5PT1BfQ09NTUFORCwgdGl0bGU6IGNtZC50aXRsZSB9O1xufVxuXG50eXBlIExzcFJhbmdlID0geyBzdGFydDogeyBsaW5lOiBudW1iZXI7IGNoYXJhY3RlcjogbnVtYmVyIH07IGVuZDogeyBsaW5lOiBudW1iZXI7IGNoYXJhY3RlcjogbnVtYmVyIH0gfTtcblxuLyoqIE1vbmFjbyBjb21wbGV0aW9uIGl0ZW0gY2FycnlpbmcgdGhlIG9yaWdpbmF0aW5nIExTUCBpdGVtIHNvIHJlc29sdmUgY2FuIGZldGNoIGl0cyBpbXBvcnQgZWRpdHMuICovXG50eXBlIE1vbmFjb0NvbXBsZXRpb25JdGVtID0gbW9uYWNvLmxhbmd1YWdlcy5Db21wbGV0aW9uSXRlbSAmIHsgX2xzcD86IENvbXBsZXRpb25JdGVtIH07XG5cbmZ1bmN0aW9uIHRleHRFZGl0VG9Nb25hY28oZWRpdDogVGV4dEVkaXQpOiBtb25hY28ubGFuZ3VhZ2VzLlRleHRFZGl0IHtcbiAgICByZXR1cm4geyByYW5nZTogbHNwUmFuZ2VUb01vbmFjbyhlZGl0LnJhbmdlKSwgdGV4dDogZWRpdC5uZXdUZXh0IH07XG59XG5cbmZ1bmN0aW9uIG1vbmFjb1JhbmdlVG9Mc3AocjogbW9uYWNvLklSYW5nZSk6IExzcFJhbmdlIHtcbiAgICByZXR1cm4ge1xuICAgICAgICBzdGFydDogeyBsaW5lOiByLnN0YXJ0TGluZU51bWJlciAtIDEsIGNoYXJhY3Rlcjogci5zdGFydENvbHVtbiAtIDEgfSxcbiAgICAgICAgZW5kOiAgIHsgbGluZTogci5lbmRMaW5lTnVtYmVyIC0gMSwgY2hhcmFjdGVyOiByLmVuZENvbHVtbiAtIDEgfSxcbiAgICB9O1xufVxuXG4vKiogVHJ1ZSB3aGVuIHR3byBMU1AgcmFuZ2VzIGludGVyc2VjdCAodXNlZCB0byBwaWNrIHRoZSBkaWFnbm9zdGljcyByZWxldmFudCB0byBhIGNvZGUtYWN0aW9uIHJlcXVlc3QpLiAqL1xuZnVuY3Rpb24gcmFuZ2VzT3ZlcmxhcChhOiBMc3BSYW5nZSwgYjogTHNwUmFuZ2UpOiBib29sZWFuIHtcbiAgICBjb25zdCBub3RBZnRlciA9IChwOiB7IGxpbmU6IG51bWJlcjsgY2hhcmFjdGVyOiBudW1iZXIgfSwgcTogeyBsaW5lOiBudW1iZXI7IGNoYXJhY3RlcjogbnVtYmVyIH0pID0+XG4gICAgICAgIHAubGluZSA8IHEubGluZSB8fCAocC5saW5lID09PSBxLmxpbmUgJiYgcC5jaGFyYWN0ZXIgPD0gcS5jaGFyYWN0ZXIpO1xuICAgIHJldHVybiBub3RBZnRlcihhLnN0YXJ0LCBiLmVuZCkgJiYgbm90QWZ0ZXIoYi5zdGFydCwgYS5lbmQpO1xufVxuXG5mdW5jdGlvbiBsc3BDb2RlQWN0aW9uVG9Nb25hY28oYWN0aW9uOiBDb2RlQWN0aW9uIHwgQ29tbWFuZCk6IG1vbmFjby5sYW5ndWFnZXMuQ29kZUFjdGlvbiB7XG4gICAgY29uc3QgaXNDb21tYW5kID0gdHlwZW9mIChhY3Rpb24gYXMgQ29tbWFuZCkuY29tbWFuZCA9PT0gJ3N0cmluZyc7XG4gICAgY29uc3QgdGl0bGUgPSBhY3Rpb24udGl0bGVcbiAgICAgICAgPz8gKGlzQ29tbWFuZCA/IChhY3Rpb24gYXMgQ29tbWFuZCkuY29tbWFuZCA6IChhY3Rpb24gYXMgQ29kZUFjdGlvbikuY29tbWFuZD8udGl0bGUpXG4gICAgICAgID8/ICdBY3Rpb24nO1xuICAgIGNvbnN0IGtpbmQgPSBpc0NvbW1hbmQgPyAncXVpY2tmaXgnIDogKChhY3Rpb24gYXMgQ29kZUFjdGlvbikua2luZCA/PyAncXVpY2tmaXgnKTtcbiAgICByZXR1cm4ge1xuICAgICAgICB0aXRsZSxcbiAgICAgICAga2luZCxcbiAgICAgICAgZGlhZ25vc3RpY3M6IFtdLFxuICAgICAgICBpc1ByZWZlcnJlZDogKGFjdGlvbiBhcyBDb2RlQWN0aW9uKS5pc1ByZWZlcnJlZCxcbiAgICAgICAgLy8gQXBwbHkgbGF6aWx5IHRocm91Z2ggb3VyIGNvbW1hbmQgc28gd2UgY2FuIHJlc29sdmUsIHJ1biBzZXJ2ZXIgY29tbWFuZHMgYW5kIGFwcGx5IGVkaXRzIHVuaWZvcm1seS5cbiAgICAgICAgY29tbWFuZDogeyBpZDogQVBQTFlfQUNUSU9OX0NPTU1BTkQsIHRpdGxlLCBhcmd1bWVudHM6IFthY3Rpb25dIH0sXG4gICAgfTtcbn1cblxuYXN5bmMgZnVuY3Rpb24gYXBwbHlDb2RlQWN0aW9uKGFjdGlvbjogQ29kZUFjdGlvbiB8IENvbW1hbmQpOiBQcm9taXNlPHZvaWQ+IHtcbiAgICBpZiAoIV9jb25uKSByZXR1cm47XG4gICAgdHJ5IHtcbiAgICAgICAgaWYgKHR5cGVvZiAoYWN0aW9uIGFzIENvbW1hbmQpLmNvbW1hbmQgPT09ICdzdHJpbmcnKSB7XG4gICAgICAgICAgICBhd2FpdCBydW5TZXJ2ZXJDb21tYW5kKGFjdGlvbiBhcyBDb21tYW5kKTtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBsZXQgcmVzb2x2ZWQgPSBhY3Rpb24gYXMgQ29kZUFjdGlvbjtcbiAgICAgICAgaWYgKCFyZXNvbHZlZC5lZGl0ICYmIChyZXNvbHZlZCBhcyB7IGRhdGE/OiB1bmtub3duIH0pLmRhdGEgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmVzb2x2ZWQgPSBhd2FpdCBfY29ubi5zZW5kUmVxdWVzdCgnY29kZUFjdGlvbi9yZXNvbHZlJywgcmVzb2x2ZWQpO1xuICAgICAgICB9XG4gICAgICAgIGlmIChyZXNvbHZlZC5lZGl0KSBhcHBseVdvcmtzcGFjZUVkaXQocmVzb2x2ZWQuZWRpdCk7XG4gICAgICAgIGlmIChyZXNvbHZlZC5jb21tYW5kKSBhd2FpdCBydW5TZXJ2ZXJDb21tYW5kKHJlc29sdmVkLmNvbW1hbmQpO1xuICAgIH0gY2F0Y2ggKGUpIHtcbiAgICAgICAgY29uc29sZS53YXJuKCdbamF2YS1sc3BdIGNvZGUgYWN0aW9uIGZhaWxlZDonLCAoZSBhcyBFcnJvcik/Lm1lc3NhZ2UgPz8gZSk7XG4gICAgfVxufVxuXG5mdW5jdGlvbiBpc1dvcmtzcGFjZUVkaXQodmFsdWU6IHVua25vd24pOiB2YWx1ZSBpcyBXb3Jrc3BhY2VFZGl0IHtcbiAgICByZXR1cm4gISF2YWx1ZSAmJiAoISEodmFsdWUgYXMgV29ya3NwYWNlRWRpdCkuY2hhbmdlcyB8fCAhISh2YWx1ZSBhcyBXb3Jrc3BhY2VFZGl0KS5kb2N1bWVudENoYW5nZXMpO1xufVxuXG5hc3luYyBmdW5jdGlvbiBydW5TZXJ2ZXJDb21tYW5kKGNtZDogQ29tbWFuZCk6IFByb21pc2U8dm9pZD4ge1xuICAgIGlmICghX2Nvbm4gfHwgIWNtZD8uY29tbWFuZCkgcmV0dXJuO1xuICAgIGlmIChHRU5FUkFURVtjbWQuY29tbWFuZF0pIHtcbiAgICAgICAgYXdhaXQgcnVuR2VuZXJhdGUoY21kLmNvbW1hbmQsIGNtZC5hcmd1bWVudHMgPz8gW10pO1xuICAgICAgICByZXR1cm47XG4gICAgfVxuICAgIGNvbnN0IHJlc3VsdCA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCd3b3Jrc3BhY2UvZXhlY3V0ZUNvbW1hbmQnLCB7IGNvbW1hbmQ6IGNtZC5jb21tYW5kLCBhcmd1bWVudHM6IGNtZC5hcmd1bWVudHMgPz8gW10gfSk7XG4gICAgaWYgKGlzV29ya3NwYWNlRWRpdChyZXN1bHQpKSBhcHBseVdvcmtzcGFjZUVkaXQocmVzdWx0KTtcbn1cblxuaW50ZXJmYWNlIEdlbmVyYXRlU3BlYyB7XG4gICAgbGFiZWw6IHN0cmluZztcbiAgICBzdGF0dXM6IHN0cmluZztcbiAgICBnZW5lcmF0ZTogc3RyaW5nO1xuICAgIG1lbWJlcnM6IChzdGF0dXM6IGFueSkgPT4gQXJyYXk8eyBsYWJlbDogc3RyaW5nOyByZWY6IGFueSB9PjtcbiAgICBidWlsZEFyZ3M6IChwcm9tcHRBcmdzOiBhbnlbXSwgc3RhdHVzOiBhbnksIHNlbGVjdGVkOiBhbnlbXSkgPT4gYW55W107XG59XG5cbi8qKiBNZW1iZXItbmFtZWQgZmllbGRzIFx1MjE5MiBwaWNrZXIgbGFiZWxzLiAqL1xuZnVuY3Rpb24gZmllbGRMYWJlbChmOiBhbnkpOiBzdHJpbmcge1xuICAgIGNvbnN0IG5hbWUgPSBmPy5uYW1lID8/IGY/LmZpZWxkTmFtZSA/PyAnJztcbiAgICBjb25zdCB0eXBlID0gZj8udHlwZSA/PyBmPy50eXBlTmFtZTtcbiAgICByZXR1cm4gdHlwZSA/IGAke25hbWV9OiAke3R5cGV9YCA6IGAke25hbWV9YDtcbn1cblxuLyoqXG4gKiBUaGUgSkRULkxTIHNvdXJjZS1nZW5lcmF0aW9uIGNvbW1hbmRzIChjb25zdHJ1Y3RvcnMsIGdldHRlcnMvc2V0dGVycywgdG9TdHJpbmcsIGhhc2hDb2RlL2VxdWFscykuXG4gKiBFYWNoIG1hcHMgdGhlIGNsaWVudCBcIipQcm9tcHRcIiBjb21tYW5kIHRvIHRoZSBzZXJ2ZXIgc3RhdHVzICsgZ2VuZXJhdGUgZGVsZWdhdGUgY29tbWFuZHM7IHRoZSBzdGF0dXNcbiAqIGNhbGwgeWllbGRzIHRoZSBjYW5kaWRhdGUgZmllbGRzIHNob3duIGluIHRoZSBtZW1iZXIgcGlja2VyLCB0aGUgZ2VuZXJhdGUgY2FsbCByZXR1cm5zIHRoZSBlZGl0LlxuICovXG5jb25zdCBHRU5FUkFURTogUmVjb3JkPHN0cmluZywgR2VuZXJhdGVTcGVjPiA9IHtcbiAgICAnamF2YS5hY3Rpb24uZ2VuZXJhdGVDb25zdHJ1Y3RvcnNQcm9tcHQnOiB7XG4gICAgICAgIGxhYmVsOiAnU2VsZWN0IGZpZWxkcyBhbmQgY29uc3RydWN0b3JzJyxcbiAgICAgICAgc3RhdHVzOiAnamF2YS5hY3Rpb24uY2hlY2tDb25zdHJ1Y3RvcnNTdGF0dXMnLFxuICAgICAgICBnZW5lcmF0ZTogJ2phdmEuYWN0aW9uLmdlbmVyYXRlQ29uc3RydWN0b3JzJyxcbiAgICAgICAgbWVtYmVyczogKHMpID0+IChzPy5maWVsZHMgPz8gW10pLm1hcCgoZjogYW55KSA9PiAoeyBsYWJlbDogZmllbGRMYWJlbChmKSwgcmVmOiBmIH0pKSxcbiAgICAgICAgYnVpbGRBcmdzOiAoYXJncywgcywgc2VsKSA9PiBbYXJnc1swXSwgeyBjb25zdHJ1Y3RvcnM6IHM/LmNvbnN0cnVjdG9ycyA/PyBbXSwgZmllbGRzOiBzZWwubWFwKG0gPT4gbS5yZWYpIH1dLFxuICAgIH0sXG4gICAgJ2phdmEuYWN0aW9uLmdlbmVyYXRlVG9TdHJpbmdQcm9tcHQnOiB7XG4gICAgICAgIGxhYmVsOiAnU2VsZWN0IGZpZWxkcyB0byBpbmNsdWRlIGluIHRvU3RyaW5nKCknLFxuICAgICAgICBzdGF0dXM6ICdqYXZhLmFjdGlvbi5jaGVja1RvU3RyaW5nU3RhdHVzJyxcbiAgICAgICAgZ2VuZXJhdGU6ICdqYXZhLmFjdGlvbi5nZW5lcmF0ZVRvU3RyaW5nJyxcbiAgICAgICAgbWVtYmVyczogKHMpID0+IChzPy5maWVsZHMgPz8gW10pLm1hcCgoZjogYW55KSA9PiAoeyBsYWJlbDogZmllbGRMYWJlbChmKSwgcmVmOiBmIH0pKSxcbiAgICAgICAgYnVpbGRBcmdzOiAoYXJncywgX3MsIHNlbCkgPT4gW2FyZ3NbMF0sIHNlbC5tYXAobSA9PiBtLnJlZildLFxuICAgIH0sXG4gICAgJ2phdmEuYWN0aW9uLmhhc2hDb2RlRXF1YWxzUHJvbXB0Jzoge1xuICAgICAgICBsYWJlbDogJ1NlbGVjdCBmaWVsZHMgZm9yIGhhc2hDb2RlKCkgYW5kIGVxdWFscygpJyxcbiAgICAgICAgc3RhdHVzOiAnamF2YS5hY3Rpb24uY2hlY2tIYXNoQ29kZUVxdWFsc1N0YXR1cycsXG4gICAgICAgIGdlbmVyYXRlOiAnamF2YS5hY3Rpb24uZ2VuZXJhdGVIYXNoQ29kZUVxdWFscycsXG4gICAgICAgIG1lbWJlcnM6IChzKSA9PiAocz8uZmllbGRzID8/IFtdKS5tYXAoKGY6IGFueSkgPT4gKHsgbGFiZWw6IGZpZWxkTGFiZWwoZiksIHJlZjogZiB9KSksXG4gICAgICAgIGJ1aWxkQXJnczogKGFyZ3MsIF9zLCBzZWwpID0+IFthcmdzWzBdLCBzZWwubWFwKG0gPT4gbS5yZWYpLCBmYWxzZV0sXG4gICAgfSxcbiAgICAnamF2YS5hY3Rpb24uZ2VuZXJhdGVBY2Nlc3NvcnNQcm9tcHQnOiB7XG4gICAgICAgIGxhYmVsOiAnU2VsZWN0IGZpZWxkcyB0byBnZW5lcmF0ZSBnZXR0ZXJzIGFuZCBzZXR0ZXJzJyxcbiAgICAgICAgc3RhdHVzOiAnamF2YS5hY3Rpb24uY2hlY2tBY2Nlc3NvcnNTdGF0dXMnLFxuICAgICAgICBnZW5lcmF0ZTogJ2phdmEuYWN0aW9uLmdlbmVyYXRlQWNjZXNzb3JzJyxcbiAgICAgICAgbWVtYmVyczogKHMpID0+IChzPy5hY2Nlc3NvcnMgPz8gcyA/PyBbXSkubWFwKChhOiBhbnkpID0+ICh7IGxhYmVsOiBmaWVsZExhYmVsKGEpLCByZWY6IGEgfSkpLFxuICAgICAgICBidWlsZEFyZ3M6IChhcmdzLCBfcywgc2VsKSA9PiBbYXJnc1swXSwgc2VsLm1hcChtID0+IG0ucmVmKV0sXG4gICAgfSxcbiAgICAnamF2YS5hY3Rpb24ub3ZlcnJpZGVNZXRob2RzUHJvbXB0Jzoge1xuICAgICAgICBsYWJlbDogJ1NlbGVjdCBtZXRob2RzIHRvIG92ZXJyaWRlIG9yIGltcGxlbWVudCcsXG4gICAgICAgIHN0YXR1czogJ2phdmEuYWN0aW9uLmxpc3RPdmVycmlkYWJsZU1ldGhvZHMnLFxuICAgICAgICBnZW5lcmF0ZTogJ2phdmEuYWN0aW9uLmFkZE92ZXJyaWRhYmxlTWV0aG9kcycsXG4gICAgICAgIG1lbWJlcnM6IChzKSA9PiAocz8ubWV0aG9kcyA/PyBbXSkubWFwKChtOiBhbnkpID0+ICh7XG4gICAgICAgICAgICBsYWJlbDogYCR7bS5uYW1lfSgkeyhtLnBhcmFtZXRlcnMgPz8gW10pLmpvaW4oJywgJyl9KSR7bS5kZWNsYXJpbmdDbGFzcyA/ICcgOiAnICsgbS5kZWNsYXJpbmdDbGFzcyA6ICcnfWAsXG4gICAgICAgICAgICByZWY6IG0sXG4gICAgICAgIH0pKSxcbiAgICAgICAgYnVpbGRBcmdzOiAoYXJncywgc3RhdHVzLCBzZWwpID0+IFthcmdzWzBdLCB7IG92ZXJyaWRhYmxlTWV0aG9kczogc2VsLm1hcChtID0+IG0ucmVmKSwgdHlwZTogc3RhdHVzPy50eXBlIH1dLFxuICAgIH0sXG59O1xuXG5hc3luYyBmdW5jdGlvbiBydW5HZW5lcmF0ZShwcm9tcHRJZDogc3RyaW5nLCBhcmdzOiBhbnlbXSk6IFByb21pc2U8dm9pZD4ge1xuICAgIGlmICghX2Nvbm4pIHJldHVybjtcbiAgICBjb25zdCBzcGVjID0gR0VORVJBVEVbcHJvbXB0SWRdO1xuICAgIGNvbnN0IHN0YXR1cyA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCd3b3Jrc3BhY2UvZXhlY3V0ZUNvbW1hbmQnLCB7IGNvbW1hbmQ6IHNwZWMuc3RhdHVzLCBhcmd1bWVudHM6IGFyZ3MgfSk7XG4gICAgaWYgKCFzdGF0dXMpIHJldHVybjtcbiAgICBjb25zdCBtZW1iZXJzID0gc3BlYy5tZW1iZXJzKHN0YXR1cyk7XG4gICAgbGV0IHNlbGVjdGVkID0gbWVtYmVycztcbiAgICBjb25zdCBwaWNrZXIgPSAoZ2xvYmFsVGhpcyBhcyBhbnkpLmphdmFMc3BNZW1iZXJQaWNrZXI7XG4gICAgaWYgKG1lbWJlcnMubGVuZ3RoICYmIHR5cGVvZiBwaWNrZXIgPT09ICdmdW5jdGlvbicpIHtcbiAgICAgICAgY29uc3QgY2hvc2VuOiBzdHJpbmdbXSB8IG51bGwgPSBhd2FpdCBwaWNrZXIoc3BlYy5sYWJlbCwgbWVtYmVycy5tYXAobSA9PiBtLmxhYmVsKSk7XG4gICAgICAgIGlmIChjaG9zZW4gPT09IG51bGwpIHJldHVybjsgLy8gdXNlciBjYW5jZWxsZWQgdGhlIGRpYWxvZ1xuICAgICAgICBzZWxlY3RlZCA9IG1lbWJlcnMuZmlsdGVyKG0gPT4gY2hvc2VuLmluY2x1ZGVzKG0ubGFiZWwpKTtcbiAgICB9XG4gICAgY29uc3QgZWRpdCA9IGF3YWl0IF9jb25uLnNlbmRSZXF1ZXN0KCd3b3Jrc3BhY2UvZXhlY3V0ZUNvbW1hbmQnLCB7XG4gICAgICAgIGNvbW1hbmQ6IHNwZWMuZ2VuZXJhdGUsXG4gICAgICAgIGFyZ3VtZW50czogc3BlYy5idWlsZEFyZ3MoYXJncywgc3RhdHVzLCBzZWxlY3RlZCksXG4gICAgfSk7XG4gICAgaWYgKGlzV29ya3NwYWNlRWRpdChlZGl0KSkgYXBwbHlXb3Jrc3BhY2VFZGl0KGVkaXQpO1xufVxuXG4vKiogR3JvdXBzIGEgd29ya3NwYWNlIGVkaXQncyB0ZXh0IGVkaXRzIGJ5IGRvY3VtZW50IGFuZCBhcHBsaWVzIHRoZW0gaW4gcGxhY2UgdG8gb3BlbiBNb25hY28gbW9kZWxzLiAqL1xuZnVuY3Rpb24gYXBwbHlXb3Jrc3BhY2VFZGl0KGVkaXQ6IFdvcmtzcGFjZUVkaXQgfCBudWxsIHwgdW5kZWZpbmVkKTogdm9pZCB7XG4gICAgaWYgKCFlZGl0KSByZXR1cm47XG4gICAgY29uc3QgYnlVcmk6IFJlY29yZDxzdHJpbmcsIFRleHRFZGl0W10+ID0ge307XG4gICAgaWYgKGVkaXQuY2hhbmdlcykge1xuICAgICAgICBmb3IgKGNvbnN0IHVyaSBpbiBlZGl0LmNoYW5nZXMpIGJ5VXJpW3VyaV0gPSAoYnlVcmlbdXJpXSA/PyBbXSkuY29uY2F0KGVkaXQuY2hhbmdlc1t1cmldKTtcbiAgICB9XG4gICAgaWYgKGVkaXQuZG9jdW1lbnRDaGFuZ2VzKSB7XG4gICAgICAgIGZvciAoY29uc3QgZGMgb2YgZWRpdC5kb2N1bWVudENoYW5nZXMgYXMgYW55W10pIHtcbiAgICAgICAgICAgIGlmIChkYz8udGV4dERvY3VtZW50Py51cmkgJiYgQXJyYXkuaXNBcnJheShkYy5lZGl0cykpIHtcbiAgICAgICAgICAgICAgICBieVVyaVtkYy50ZXh0RG9jdW1lbnQudXJpXSA9IChieVVyaVtkYy50ZXh0RG9jdW1lbnQudXJpXSA/PyBbXSkuY29uY2F0KGRjLmVkaXRzKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgIH1cbiAgICBmb3IgKGNvbnN0IHVyaSBpbiBieVVyaSkge1xuICAgICAgICBjb25zdCBtb2RlbCA9IG1vbmFjby5lZGl0b3IuZ2V0TW9kZWxzKCkuZmluZChtID0+IG0udXJpLnRvU3RyaW5nKCkgPT09IHVyaSk7XG4gICAgICAgIGlmICghbW9kZWwpIGNvbnRpbnVlO1xuICAgICAgICBjb25zdCBvcHMgPSBieVVyaVt1cmldLm1hcChlID0+ICh7IHJhbmdlOiBsc3BSYW5nZVRvTW9uYWNvKGUucmFuZ2UpLCB0ZXh0OiBlLm5ld1RleHQsIGZvcmNlTW92ZU1hcmtlcnM6IHRydWUgfSkpO1xuICAgICAgICBtb2RlbC5wdXNoRWRpdE9wZXJhdGlvbnMoW10sIG9wcywgKCkgPT4gbnVsbCk7XG4gICAgfVxufVxuXG4vKiogQ29udmVydHMgYW4gTFNQIHdvcmtzcGFjZSBlZGl0IGludG8gdGhlIE1vbmFjbyBzaGFwZSByZXR1cm5lZCBieSB0aGUgcmVuYW1lIHByb3ZpZGVyLiAqL1xuZnVuY3Rpb24gd29ya3NwYWNlRWRpdFRvTW9uYWNvKGVkaXQ6IFdvcmtzcGFjZUVkaXQgfCBudWxsKTogbW9uYWNvLmxhbmd1YWdlcy5Xb3Jrc3BhY2VFZGl0IHtcbiAgICBjb25zdCBlZGl0czogYW55W10gPSBbXTtcbiAgICBjb25zdCBwdXNoID0gKHVyaTogc3RyaW5nLCBsaXN0OiBUZXh0RWRpdFtdKSA9PiB7XG4gICAgICAgIGZvciAoY29uc3QgZSBvZiBsaXN0KSB7XG4gICAgICAgICAgICBlZGl0cy5wdXNoKHsgcmVzb3VyY2U6IG1vbmFjby5VcmkucGFyc2UodXJpKSwgdGV4dEVkaXQ6IHsgcmFuZ2U6IGxzcFJhbmdlVG9Nb25hY28oZS5yYW5nZSksIHRleHQ6IGUubmV3VGV4dCB9LCB2ZXJzaW9uSWQ6IHVuZGVmaW5lZCB9KTtcbiAgICAgICAgfVxuICAgIH07XG4gICAgaWYgKGVkaXQ/LmNoYW5nZXMpIHtcbiAgICAgICAgZm9yIChjb25zdCB1cmkgaW4gZWRpdC5jaGFuZ2VzKSBwdXNoKHVyaSwgZWRpdC5jaGFuZ2VzW3VyaV0pO1xuICAgIH1cbiAgICBpZiAoZWRpdD8uZG9jdW1lbnRDaGFuZ2VzKSB7XG4gICAgICAgIGZvciAoY29uc3QgZGMgb2YgZWRpdC5kb2N1bWVudENoYW5nZXMgYXMgYW55W10pIHtcbiAgICAgICAgICAgIGlmIChkYz8udGV4dERvY3VtZW50Py51cmkgJiYgQXJyYXkuaXNBcnJheShkYy5lZGl0cykpIHB1c2goZGMudGV4dERvY3VtZW50LnVyaSwgZGMuZWRpdHMpO1xuICAgICAgICB9XG4gICAgfVxuICAgIHJldHVybiB7IGVkaXRzIH07XG59XG5cbi8qKiBNYXBzIGEgdmlydHVhbCBlZGl0b3IgVVJJIGJhY2sgdG8gdGhlIElERSB3b3Jrc3BhY2UgcGF0aCAoe0Bjb2RlIC93cy9wcm9qLy4uLn0pLiAqL1xuZnVuY3Rpb24gdXJpVG9Xb3Jrc3BhY2VQYXRoKHVyaTogc3RyaW5nKTogc3RyaW5nIHwgbnVsbCB7XG4gICAgaWYgKCF1cmkuc3RhcnRzV2l0aChWSVJUVUFMX0ZJTEVfUFJFRklYKSkgcmV0dXJuIG51bGw7XG4gICAgcmV0dXJuIGRlY29kZVVSSUNvbXBvbmVudCh1cmkuc3Vic3RyaW5nKFZJUlRVQUxfRklMRV9QUkVGSVgubGVuZ3RoKSk7XG59XG5cbi8qKiBSZWFkcyBhIHdvcmtzcGFjZSBmaWxlJ3MgY3VycmVudCB0ZXh0IG92ZXIgdGhlIElERSBSRVNUIEFQSS4gKi9cbmFzeW5jIGZ1bmN0aW9uIGZldGNoV29ya3NwYWNlRmlsZVRleHQoaWRlUGF0aDogc3RyaW5nKTogUHJvbWlzZTxzdHJpbmc+IHtcbiAgICBjb25zdCByZXNwb25zZSA9IGF3YWl0IGZldGNoKCcvc2VydmljZXMvaWRlL3dvcmtzcGFjZXMnICsgaWRlUGF0aCwgeyBoZWFkZXJzOiB7ICdYLVJlcXVlc3RlZC1XaXRoJzogJ0ZldGNoJyB9IH0pO1xuICAgIGlmICghcmVzcG9uc2Uub2spIHtcbiAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBDb3VsZCBub3QgcmVhZCAke2lkZVBhdGh9IChIVFRQICR7cmVzcG9uc2Uuc3RhdHVzfSlgKTtcbiAgICB9XG4gICAgcmV0dXJuIHJlc3BvbnNlLnRleHQoKTtcbn1cblxuLyoqIEFwcGxpZXMgTFNQIHRleHQgZWRpdHMgdG8gYSBzdHJpbmcuIE9mZnNldHMgYXJlIHJlc29sdmVkIGFnYWluc3QgdGhlIG9yaWdpbmFsIHRleHQgYW5kIGVkaXRzIGFyZVxuICogIGFwcGxpZWQgZnJvbSB0aGUgZW5kIGJhY2t3YXJkcywgc28gZWFybGllciBvZmZzZXRzIHN0YXkgdmFsaWQuICovXG5mdW5jdGlvbiBhcHBseUVkaXRzVG9UZXh0KHRleHQ6IHN0cmluZywgZWRpdHM6IFRleHRFZGl0W10pOiBzdHJpbmcge1xuICAgIGNvbnN0IGxpbmVTdGFydHMgPSBbMF07XG4gICAgZm9yIChsZXQgaSA9IDA7IGkgPCB0ZXh0Lmxlbmd0aDsgaSsrKSB7XG4gICAgICAgIGlmICh0ZXh0LmNoYXJDb2RlQXQoaSkgPT09IDEwIC8qIFxcbiAqLykgbGluZVN0YXJ0cy5wdXNoKGkgKyAxKTtcbiAgICB9XG4gICAgY29uc3Qgb2Zmc2V0ID0gKHA6IHsgbGluZTogbnVtYmVyOyBjaGFyYWN0ZXI6IG51bWJlciB9KSA9PiAobGluZVN0YXJ0c1twLmxpbmVdID8/IHRleHQubGVuZ3RoKSArIHAuY2hhcmFjdGVyO1xuICAgIGNvbnN0IG9yZGVyZWQgPSBlZGl0cy5zbGljZSgpXG4gICAgICAgICAgICAgICAgICAgICAgICAgLnNvcnQoKGEsIGIpID0+IG9mZnNldChiLnJhbmdlLnN0YXJ0KSAtIG9mZnNldChhLnJhbmdlLnN0YXJ0KSk7XG4gICAgbGV0IHJlc3VsdCA9IHRleHQ7XG4gICAgZm9yIChjb25zdCBlIG9mIG9yZGVyZWQpIHtcbiAgICAgICAgcmVzdWx0ID0gcmVzdWx0LnNsaWNlKDAsIG9mZnNldChlLnJhbmdlLnN0YXJ0KSkgKyBlLm5ld1RleHQgKyByZXN1bHQuc2xpY2Uob2Zmc2V0KGUucmFuZ2UuZW5kKSk7XG4gICAgfVxuICAgIHJldHVybiByZXN1bHQ7XG59XG5cbi8qKlxuICogQXBwbGllcyBhIEpEVC5MUyByZW5hbWUge0BsaW5rIFdvcmtzcGFjZUVkaXR9IGFjcm9zcyB0aGUgd2hvbGUgd29ya3NwYWNlOiB0ZXh0IGVkaXRzIGluIGV2ZXJ5XG4gKiBhZmZlY3RlZCBmaWxlIHBsdXMgYW55IHtAY29kZSBSZW5hbWVGaWxlfSBvcGVyYXRpb24gKGEgcHVibGljLXR5cGUgcmVuYW1lIHJlbmFtZXMgaXRzIG93blxuICoge0Bjb2RlIC5qYXZhfSBmaWxlKS4gVGhlIGN1cnJlbnQgZmlsZSdzIGVkaXRzIGdvIHRocm91Z2ggdGhlIGxpdmUgTW9uYWNvIG1vZGVsOyB0aGUgcmVzdCBhcmUgcmVhZCxcbiAqIGVkaXRlZCBhbmQgd3JpdHRlbiBiYWNrIG92ZXIgUkVTVC4gUGVyc2lzdGVuY2UgKENTUkYtZ3VhcmRlZCB3cml0ZXMsIHRoZSB0YWIgc3dpdGNoIHdoZW4gdGhlIGN1cnJlbnRcbiAqIGZpbGUgaXMgcmVuYW1lZCwgYW5kIHJlbG9hZGluZyBvdGhlciBvcGVuIGVkaXRvcnMpIGlzIGRlbGVnYXRlZCB0byB0aGUgSURFIHZpYSBqYXZhTHNwUGVyc2lzdFJlbmFtZS5cbiAqL1xuYXN5bmMgZnVuY3Rpb24gYXBwbHlSZW5hbWVBY3Jvc3NXb3Jrc3BhY2UobW9kZWw6IG1vbmFjby5lZGl0b3IuSVRleHRNb2RlbCwgZWRpdDogV29ya3NwYWNlRWRpdCk6IFByb21pc2U8dm9pZD4ge1xuICAgIGNvbnN0IGN1cnJlbnRVcmkgPSBtb2RlbC51cmkudG9TdHJpbmcoKTtcbiAgICBjb25zdCB0ZXh0QnlVcmk6IFJlY29yZDxzdHJpbmcsIFRleHRFZGl0W10+ID0ge307XG4gICAgY29uc3QgcmVuYW1lQnlVcmk6IFJlY29yZDxzdHJpbmcsIHN0cmluZz4gPSB7fTtcblxuICAgIGlmIChlZGl0LmNoYW5nZXMpIHtcbiAgICAgICAgZm9yIChjb25zdCB1cmkgaW4gZWRpdC5jaGFuZ2VzKSB0ZXh0QnlVcmlbdXJpXSA9ICh0ZXh0QnlVcmlbdXJpXSA/PyBbXSkuY29uY2F0KGVkaXQuY2hhbmdlc1t1cmldKTtcbiAgICB9XG4gICAgaWYgKGVkaXQuZG9jdW1lbnRDaGFuZ2VzKSB7XG4gICAgICAgIGZvciAoY29uc3QgZGMgb2YgZWRpdC5kb2N1bWVudENoYW5nZXMgYXMgYW55W10pIHtcbiAgICAgICAgICAgIGlmIChkYz8ua2luZCA9PT0gJ3JlbmFtZScgJiYgZGMub2xkVXJpICYmIGRjLm5ld1VyaSkge1xuICAgICAgICAgICAgICAgIHJlbmFtZUJ5VXJpW2RjLm9sZFVyaV0gPSBkYy5uZXdVcmk7XG4gICAgICAgICAgICB9IGVsc2UgaWYgKGRjPy50ZXh0RG9jdW1lbnQ/LnVyaSAmJiBBcnJheS5pc0FycmF5KGRjLmVkaXRzKSkge1xuICAgICAgICAgICAgICAgIHRleHRCeVVyaVtkYy50ZXh0RG9jdW1lbnQudXJpXSA9ICh0ZXh0QnlVcmlbZGMudGV4dERvY3VtZW50LnVyaV0gPz8gW10pLmNvbmNhdChkYy5lZGl0cyk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICB9XG5cbiAgICAvLyBKRFQuTFMgbWF5IGtleSBhIGZpbGUncyB0ZXh0IGVkaXRzIGJ5IGl0cyBQT1NULXJlbmFtZSBVUkkgKGRvY3VtZW50Q2hhbmdlcyBhcmUgb3JkZXJlZCwgYW5kIHRoZVxuICAgIC8vIHJlbmFtZSBjYW4gcHJlY2VkZSB0aGUgZWRpdCkuIFJlLWF0dHJpYnV0ZSBldmVyeSBlZGl0IHRvIHRoZSBvbi1kaXNrIChvbGQpIFVSSSBzbyB0aGUgY29udGVudCBpc1xuICAgIC8vIGVkaXRlZCBjb3JyZWN0bHkgYmVmb3JlIHRoZSBmaWxlIGlzIHdyaXR0ZW4vcmVuYW1lZCBcdTIwMTQgb3RoZXJ3aXNlIHRoZSBuZXcgZmlsZSBrZWVwcyB0aGUgb2xkIHR5cGVcbiAgICAvLyBuYW1lIGFuZCB0cmlnZ2VycyBcIlRoZSBwdWJsaWMgdHlwZSBYIG11c3QgYmUgZGVmaW5lZCBpbiBpdHMgb3duIGZpbGVcIi5cbiAgICBjb25zdCBuZXdUb09sZDogUmVjb3JkPHN0cmluZywgc3RyaW5nPiA9IHt9O1xuICAgIGZvciAoY29uc3Qgb2xkVXJpIGluIHJlbmFtZUJ5VXJpKSBuZXdUb09sZFtyZW5hbWVCeVVyaVtvbGRVcmldXSA9IG9sZFVyaTtcbiAgICBjb25zdCBlZGl0c0J5T2xkOiBSZWNvcmQ8c3RyaW5nLCBUZXh0RWRpdFtdPiA9IHt9O1xuICAgIGZvciAoY29uc3QgdXJpIGluIHRleHRCeVVyaSkge1xuICAgICAgICBjb25zdCBvbkRpc2tVcmkgPSBuZXdUb09sZFt1cmldID8/IHVyaTtcbiAgICAgICAgZWRpdHNCeU9sZFtvbkRpc2tVcmldID0gKGVkaXRzQnlPbGRbb25EaXNrVXJpXSA/PyBbXSkuY29uY2F0KHRleHRCeVVyaVt1cmldKTtcbiAgICB9XG5cbiAgICBjb25zdCBwYXlsb2FkOiB7XG4gICAgICAgIGN1cnJlbnRQYXRoOiBzdHJpbmcgfCBudWxsO1xuICAgICAgICBjdXJyZW50Q29udGVudDogc3RyaW5nIHwgbnVsbDtcbiAgICAgICAgY3VycmVudE5ld1BhdGg6IHN0cmluZyB8IG51bGw7XG4gICAgICAgIHdyaXRlczogQXJyYXk8eyBwYXRoOiBzdHJpbmcgfCBudWxsOyBjb250ZW50OiBzdHJpbmcgfT47XG4gICAgICAgIHJlbmFtZXM6IEFycmF5PHsgb2xkUGF0aDogc3RyaW5nIHwgbnVsbDsgbmV3UGF0aDogc3RyaW5nIHwgbnVsbDsgY29udGVudDogc3RyaW5nIH0+O1xuICAgIH0gPSB7IGN1cnJlbnRQYXRoOiB1cmlUb1dvcmtzcGFjZVBhdGgoY3VycmVudFVyaSksIGN1cnJlbnRDb250ZW50OiBudWxsLCBjdXJyZW50TmV3UGF0aDogbnVsbCwgd3JpdGVzOiBbXSwgcmVuYW1lczogW10gfTtcblxuICAgIGNvbnN0IHdhdGNoZWRDaGFuZ2VzOiBBcnJheTx7IHVyaTogc3RyaW5nOyB0eXBlOiBudW1iZXIgfT4gPSBbXTtcbiAgICBjb25zdCBvbGRVcmlzID0gbmV3IFNldDxzdHJpbmc+KFsuLi5PYmplY3Qua2V5cyhlZGl0c0J5T2xkKSwgLi4uT2JqZWN0LmtleXMocmVuYW1lQnlVcmkpXSk7XG4gICAgZm9yIChjb25zdCBvbGRVcmkgb2Ygb2xkVXJpcykge1xuICAgICAgICBjb25zdCBlZGl0cyA9IGVkaXRzQnlPbGRbb2xkVXJpXSA/PyBbXTtcbiAgICAgICAgbGV0IGNvbnRlbnQ6IHN0cmluZztcbiAgICAgICAgaWYgKG9sZFVyaSA9PT0gY3VycmVudFVyaSkge1xuICAgICAgICAgICAgaWYgKGVkaXRzLmxlbmd0aCkge1xuICAgICAgICAgICAgICAgIG1vZGVsLnB1c2hFZGl0T3BlcmF0aW9ucyhbXSwgZWRpdHMubWFwKGUgPT4gKHsgcmFuZ2U6IGxzcFJhbmdlVG9Nb25hY28oZS5yYW5nZSksIHRleHQ6IGUubmV3VGV4dCwgZm9yY2VNb3ZlTWFya2VyczogdHJ1ZSB9KSksICgpID0+IG51bGwpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgY29udGVudCA9IG1vZGVsLmdldFZhbHVlKCk7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBjb25zdCBzb3VyY2UgPSBhd2FpdCBmZXRjaFdvcmtzcGFjZUZpbGVUZXh0KHVyaVRvV29ya3NwYWNlUGF0aChvbGRVcmkpID8/IG9sZFVyaSk7XG4gICAgICAgICAgICBjb250ZW50ID0gZWRpdHMubGVuZ3RoID8gYXBwbHlFZGl0c1RvVGV4dChzb3VyY2UsIGVkaXRzKSA6IHNvdXJjZTtcbiAgICAgICAgfVxuICAgICAgICBjb25zdCBuZXdVcmkgPSByZW5hbWVCeVVyaVtvbGRVcmldO1xuICAgICAgICBpZiAob2xkVXJpID09PSBjdXJyZW50VXJpKSB7XG4gICAgICAgICAgICBwYXlsb2FkLmN1cnJlbnRDb250ZW50ID0gY29udGVudDtcbiAgICAgICAgICAgIHBheWxvYWQuY3VycmVudE5ld1BhdGggPSBuZXdVcmkgPyB1cmlUb1dvcmtzcGFjZVBhdGgobmV3VXJpKSA6IG51bGw7XG4gICAgICAgIH0gZWxzZSBpZiAobmV3VXJpKSB7XG4gICAgICAgICAgICBwYXlsb2FkLnJlbmFtZXMucHVzaCh7IG9sZFBhdGg6IHVyaVRvV29ya3NwYWNlUGF0aChvbGRVcmkpLCBuZXdQYXRoOiB1cmlUb1dvcmtzcGFjZVBhdGgobmV3VXJpKSwgY29udGVudCB9KTtcbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIHBheWxvYWQud3JpdGVzLnB1c2goeyBwYXRoOiB1cmlUb1dvcmtzcGFjZVBhdGgob2xkVXJpKSwgY29udGVudCB9KTtcbiAgICAgICAgfVxuICAgICAgICAvLyBGaWxlLWNoYW5nZSBldmVudHMgZm9yIEpEVC5MUyBzbyBpdCByZS1zeW5jcyB3aXRob3V0IGEgcGFnZSByZWZyZXNoLlxuICAgICAgICBpZiAobmV3VXJpKSB7XG4gICAgICAgICAgICB3YXRjaGVkQ2hhbmdlcy5wdXNoKHsgdXJpOiBvbGRVcmksIHR5cGU6IDMgLyogRGVsZXRlZCAqLyB9LCB7IHVyaTogbmV3VXJpLCB0eXBlOiAxIC8qIENyZWF0ZWQgKi8gfSk7XG4gICAgICAgICAgICBfb3BlbkZpbGVzLmRlbGV0ZShvbGRVcmkpO1xuICAgICAgICAgICAgX2Nvbm4/LnNlbmROb3RpZmljYXRpb24oJ3RleHREb2N1bWVudC9kaWRDbG9zZScsIHsgdGV4dERvY3VtZW50OiB7IHVyaTogb2xkVXJpIH0gfSk7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICB3YXRjaGVkQ2hhbmdlcy5wdXNoKHsgdXJpOiBvbGRVcmksIHR5cGU6IDIgLyogQ2hhbmdlZCAqLyB9KTtcbiAgICAgICAgfVxuICAgIH1cblxuICAgIGF3YWl0IChnbG9iYWxUaGlzIGFzIGFueSkuamF2YUxzcFBlcnNpc3RSZW5hbWUocGF5bG9hZCk7XG5cbiAgICAvLyBJbmZvcm0gSkRULkxTIG9mIHRoZSBvbi1kaXNrIGNoYW5nZXMgc28gdGhlIHJlbmFtZWQgdHlwZSdzIGRpYWdub3N0aWNzIGNsZWFyIGltbWVkaWF0ZWx5LlxuICAgIGlmIChfY29ubiAmJiB3YXRjaGVkQ2hhbmdlcy5sZW5ndGgpIHtcbiAgICAgICAgX2Nvbm4uc2VuZE5vdGlmaWNhdGlvbignd29ya3NwYWNlL2RpZENoYW5nZVdhdGNoZWRGaWxlcycsIHsgY2hhbmdlczogd2F0Y2hlZENoYW5nZXMgfSk7XG4gICAgfVxufVxuXG5mdW5jdGlvbiBqZHRsc1NldHRpbmdzKCkge1xuICAgIHJldHVybiB7XG4gICAgICAgIGphdmE6IHtcbiAgICAgICAgICAgIGltcG9ydDoge1xuICAgICAgICAgICAgICAgIG1hdmVuOiAgICAgIHsgZW5hYmxlZDogdHJ1ZSB9LFxuICAgICAgICAgICAgICAgIGdyYWRsZTogICAgIHsgZW5hYmxlZDogZmFsc2UgfSxcbiAgICAgICAgICAgICAgICBleGNsdXNpb25zOiBbJyoqL25vZGVfbW9kdWxlcy8qKicsICcqKi8ubWV0YWRhdGEvKionLCAnKiovYXJjaGV0eXBlLXJlc291cmNlcy8qKiddLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIGF1dG9idWlsZDogeyBlbmFibGVkOiB0cnVlIH0sXG4gICAgICAgICAgICBjb21wbGV0aW9uOiB7XG4gICAgICAgICAgICAgICAgb3ZlcndyaXRlOiAgICAgICAgICAgIHRydWUsXG4gICAgICAgICAgICAgICAgZ3Vlc3NNZXRob2RBcmd1bWVudHM6IGZhbHNlLFxuICAgICAgICAgICAgICAgIC8vIENhcCByZXN1bHRzIHNvIGltcG9ydC90eXBlIGNvbXBsZXRpb24gb3ZlciB0aGUgbGFyZ2UgcGxhdGZvcm0gY2xhc3NwYXRoIHN0YXlzIHNuYXBweS5cbiAgICAgICAgICAgICAgICBtYXhSZXN1bHRzOiAgICAgICAgICAgNTAsXG4gICAgICAgICAgICAgICAgZmlsdGVyZWRUeXBlczogW1xuICAgICAgICAgICAgICAgICAgICAnY29tLnN1bi4qJywgJ3N1bi4qJywgJ2pkay4qJyxcbiAgICAgICAgICAgICAgICAgICAgJ29yZy5lY2xpcHNlLmpkdC5pbnRlcm5hbC4qJyxcbiAgICAgICAgICAgICAgICAgICAgJ29yZy5lY2xpcHNlLmNvcmUuaW50ZXJuYWwuKicsXG4gICAgICAgICAgICAgICAgICAgICdvcmcuZWNsaXBzZS5vc2dpLmludGVybmFsLionLFxuICAgICAgICAgICAgICAgIF0sXG4gICAgICAgICAgICAgICAgaW1wb3J0T3JkZXI6IFsnamF2YScsICdqYXZheCcsICdvcmcnLCAnY29tJywgJyddLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIHNpZ25hdHVyZUhlbHA6ICB7IGVuYWJsZWQ6IHRydWUgfSxcbiAgICAgICAgICAgIGZvcm1hdDogICAgICAgICB7IGVuYWJsZWQ6IHRydWUgfSxcbiAgICAgICAgICAgIHNhdmVBY3Rpb25zOiAgICB7IG9yZ2FuaXplSW1wb3J0czogZmFsc2UgfSxcbiAgICAgICAgICAgIGlubGF5SGludHM6ICAgICB7IHBhcmFtZXRlck5hbWVzOiB7IGVuYWJsZWQ6ICdhbGwnIH0gfSxcbiAgICAgICAgICAgIC8vIE9mZiBieSBkZWZhdWx0OiB0aGUgcmVmZXJlbmNlL2ltcGxlbWVudGF0aW9uIHNlYXJjaCBiZWhpbmQgdGhlc2UgQ29kZUxlbnNlcyBydW5zIGZvciBldmVyeVxuICAgICAgICAgICAgLy8gZGVjbGFyYXRpb24gb24gb3BlbiBhbmQgb24gZXZlcnkgZWRpdCBhbmQgZG9taW5hdGVzIEpEVC5MUyBsb2FkIG9uIGEgbGFyZ2UgY2xhc3NwYXRoLlxuICAgICAgICAgICAgcmVmZXJlbmNlc0NvZGVMZW5zOiAgICAgeyBlbmFibGVkOiBmYWxzZSB9LFxuICAgICAgICAgICAgaW1wbGVtZW50YXRpb25zQ29kZUxlbnM6IHsgZW5hYmxlZDogZmFsc2UgfSxcbiAgICAgICAgfSxcbiAgICB9O1xufVxuIiwgIi8qIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiBDb3B5cmlnaHQgKGMpIDIwMjQgVHlwZUZveCBhbmQgb3RoZXJzLlxuICogTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTElDRU5TRSBpbiB0aGUgcGFja2FnZSByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG5cbmltcG9ydCB7IERpc3Bvc2FibGUgfSBmcm9tICd2c2NvZGUtanNvbnJwYyc7XG5cbmV4cG9ydCBjbGFzcyBEaXNwb3NhYmxlQ29sbGVjdGlvbiBpbXBsZW1lbnRzIERpc3Bvc2FibGUge1xuICAgIHByb3RlY3RlZCByZWFkb25seSBkaXNwb3NhYmxlczogRGlzcG9zYWJsZVtdID0gW107XG5cbiAgICBkaXNwb3NlKCk6IHZvaWQge1xuICAgICAgICB3aGlsZSAodGhpcy5kaXNwb3NhYmxlcy5sZW5ndGggIT09IDApIHtcbiAgICAgICAgICAgIHRoaXMuZGlzcG9zYWJsZXMucG9wKCkhLmRpc3Bvc2UoKTtcbiAgICAgICAgfVxuICAgIH1cblxuICAgIHB1c2goZGlzcG9zYWJsZTogRGlzcG9zYWJsZSk6IERpc3Bvc2FibGUge1xuICAgICAgICBjb25zdCBkaXNwb3NhYmxlcyA9IHRoaXMuZGlzcG9zYWJsZXM7XG4gICAgICAgIGRpc3Bvc2FibGVzLnB1c2goZGlzcG9zYWJsZSk7XG4gICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICBkaXNwb3NlKCk6IHZvaWQge1xuICAgICAgICAgICAgICAgIGNvbnN0IGluZGV4ID0gZGlzcG9zYWJsZXMuaW5kZXhPZihkaXNwb3NhYmxlKTtcbiAgICAgICAgICAgICAgICBpZiAoaW5kZXggIT09IC0xKSB7XG4gICAgICAgICAgICAgICAgICAgIGRpc3Bvc2FibGVzLnNwbGljZShpbmRleCwgMSk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICB9O1xuICAgIH1cbn1cbiIsICIvKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSAyMDI0IFR5cGVGb3ggYW5kIG90aGVycy5cbiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBNSVQgTGljZW5zZS4gU2VlIExJQ0VOU0UgaW4gdGhlIHBhY2thZ2Ugcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSAqL1xuXG5pbXBvcnQgeyBEaXNwb3NhYmxlIH0gZnJvbSAndnNjb2RlLWpzb25ycGMnO1xuaW1wb3J0IHR5cGUgeyBJQ29ubmVjdGlvbiB9IGZyb20gJy4uL3NlcnZlci9jb25uZWN0aW9uLmpzJztcblxuZXhwb3J0IGludGVyZmFjZSBJV2ViU29ja2V0IGV4dGVuZHMgRGlzcG9zYWJsZSB7XG4gICAgc2VuZChjb250ZW50OiBzdHJpbmcpOiB2b2lkO1xuICAgIC8vIGVzbGludC1kaXNhYmxlLW5leHQtbGluZSBAdHlwZXNjcmlwdC1lc2xpbnQvbm8tZXhwbGljaXQtYW55XG4gICAgb25NZXNzYWdlKGNiOiAoZGF0YTogYW55KSA9PiB2b2lkKTogdm9pZDtcbiAgICAvLyBlc2xpbnQtZGlzYWJsZS1uZXh0LWxpbmUgQHR5cGVzY3JpcHQtZXNsaW50L25vLWV4cGxpY2l0LWFueVxuICAgIG9uRXJyb3IoY2I6IChyZWFzb246IGFueSkgPT4gdm9pZCk6IHZvaWQ7XG4gICAgb25DbG9zZShjYjogKGNvZGU6IG51bWJlciwgcmVhc29uOiBzdHJpbmcpID0+IHZvaWQpOiB2b2lkO1xufVxuXG5leHBvcnQgaW50ZXJmYWNlIElXZWJTb2NrZXRDb25uZWN0aW9uIGV4dGVuZHMgSUNvbm5lY3Rpb24ge1xuICAgIHJlYWRvbmx5IHNvY2tldDogSVdlYlNvY2tldDtcbn1cbiIsICIvKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSAyMDI0IFR5cGVGb3ggYW5kIG90aGVycy5cbiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBNSVQgTGljZW5zZS4gU2VlIExJQ0VOU0UgaW4gdGhlIHBhY2thZ2Ugcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSAqL1xuXG5pbXBvcnQgeyBEaXNwb3NhYmxlIH0gZnJvbSAndnNjb2RlLWpzb25ycGMnO1xuLy8gVE9ETzogVXNlIGVudmlyb25tZW50LXNwZWNpZmljIGltcG9ydHMgKHZzY29kZS1qc29ucnBjL2Jyb3dzZXIgb3IgdnNjb2RlLWpzb25ycGMvbm9kZSlcbi8vIHdoZW4gdXBncmFkaW5nIHRvIHZzY29kZS1qc29ucnBjQDkueC54LW5leHQuWCB3aGljaCBzdXBwb3J0cyBwcm9wZXIgZXhwb3J0IG1hcHNcbmltcG9ydCB7IHR5cGUgRGF0YUNhbGxiYWNrLCBBYnN0cmFjdE1lc3NhZ2VSZWFkZXIsIE1lc3NhZ2VSZWFkZXIgfSBmcm9tICd2c2NvZGUtanNvbnJwYyc7XG5pbXBvcnQgdHlwZSB7IElXZWJTb2NrZXQgfSBmcm9tICcuL3NvY2tldC5qcyc7XG5cbmV4cG9ydCBjbGFzcyBXZWJTb2NrZXRNZXNzYWdlUmVhZGVyIGV4dGVuZHMgQWJzdHJhY3RNZXNzYWdlUmVhZGVyIGltcGxlbWVudHMgTWVzc2FnZVJlYWRlciB7XG4gICAgcHJvdGVjdGVkIHJlYWRvbmx5IHNvY2tldDogSVdlYlNvY2tldDtcbiAgICBwcm90ZWN0ZWQgc3RhdGU6ICdpbml0aWFsJyB8ICdsaXN0ZW5pbmcnIHwgJ2Nsb3NlZCcgPSAnaW5pdGlhbCc7XG4gICAgcHJvdGVjdGVkIGNhbGxiYWNrOiBEYXRhQ2FsbGJhY2sgfCB1bmRlZmluZWQ7XG4gICAgLy8gZXNsaW50LWRpc2FibGUtbmV4dC1saW5lIEB0eXBlc2NyaXB0LWVzbGludC9uby1leHBsaWNpdC1hbnlcbiAgICBwcm90ZWN0ZWQgcmVhZG9ubHkgZXZlbnRzOiBBcnJheTx7IG1lc3NhZ2U/OiBhbnksIGVycm9yPzogYW55IH0+ID0gW107XG5cbiAgICBjb25zdHJ1Y3Rvcihzb2NrZXQ6IElXZWJTb2NrZXQpIHtcbiAgICAgICAgc3VwZXIoKTtcbiAgICAgICAgdGhpcy5zb2NrZXQgPSBzb2NrZXQ7XG4gICAgICAgIHRoaXMuc29ja2V0Lm9uTWVzc2FnZShtZXNzYWdlID0+XG4gICAgICAgICAgICB0aGlzLnJlYWRNZXNzYWdlKG1lc3NhZ2UpXG4gICAgICAgICk7XG4gICAgICAgIHRoaXMuc29ja2V0Lm9uRXJyb3IoZXJyb3IgPT5cbiAgICAgICAgICAgIHRoaXMuZmlyZUVycm9yKGVycm9yKVxuICAgICAgICApO1xuICAgICAgICB0aGlzLnNvY2tldC5vbkNsb3NlKChjb2RlLCByZWFzb24pID0+IHtcbiAgICAgICAgICAgIGlmIChjb2RlICE9PSAxMDAwKSB7XG4gICAgICAgICAgICAgICAgY29uc3QgZXJyb3I6IEVycm9yID0ge1xuICAgICAgICAgICAgICAgICAgICBuYW1lOiAnJyArIGNvZGUsXG4gICAgICAgICAgICAgICAgICAgIG1lc3NhZ2U6IGBFcnJvciBkdXJpbmcgc29ja2V0IHJlY29ubmVjdDogY29kZSA9ICR7Y29kZX0sIHJlYXNvbiA9ICR7cmVhc29ufWBcbiAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICAgIHRoaXMuZmlyZUVycm9yKGVycm9yKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHRoaXMuZmlyZUNsb3NlKCk7XG4gICAgICAgIH0pO1xuICAgIH1cblxuICAgIGxpc3RlbihjYWxsYmFjazogRGF0YUNhbGxiYWNrKTogRGlzcG9zYWJsZSB7XG4gICAgICAgIGlmICh0aGlzLnN0YXRlID09PSAnaW5pdGlhbCcpIHtcbiAgICAgICAgICAgIHRoaXMuc3RhdGUgPSAnbGlzdGVuaW5nJztcbiAgICAgICAgICAgIHRoaXMuY2FsbGJhY2sgPSBjYWxsYmFjaztcbiAgICAgICAgICAgIHdoaWxlICh0aGlzLmV2ZW50cy5sZW5ndGggIT09IDApIHtcbiAgICAgICAgICAgICAgICBjb25zdCBldmVudCA9IHRoaXMuZXZlbnRzLnBvcCgpITtcbiAgICAgICAgICAgICAgICBpZiAoZXZlbnQubWVzc2FnZSAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICAgICAgICAgIHRoaXMucmVhZE1lc3NhZ2UoZXZlbnQubWVzc2FnZSk7XG4gICAgICAgICAgICAgICAgfSBlbHNlIGlmIChldmVudC5lcnJvciAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICAgICAgICAgIHRoaXMuZmlyZUVycm9yKGV2ZW50LmVycm9yKTtcbiAgICAgICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICB0aGlzLmZpcmVDbG9zZSgpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICByZXR1cm4ge1xuICAgICAgICAgICAgZGlzcG9zZTogKCkgPT4ge1xuICAgICAgICAgICAgICAgIGlmICh0aGlzLmNhbGxiYWNrID09PSBjYWxsYmFjaykge1xuICAgICAgICAgICAgICAgICAgICB0aGlzLnN0YXRlID0gJ2luaXRpYWwnO1xuICAgICAgICAgICAgICAgICAgICB0aGlzLmNhbGxiYWNrID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgfTtcbiAgICB9XG5cbiAgICBvdmVycmlkZSBkaXNwb3NlKCkge1xuICAgICAgICBzdXBlci5kaXNwb3NlKCk7XG4gICAgICAgIHRoaXMuc3RhdGUgPSAnaW5pdGlhbCc7XG4gICAgICAgIHRoaXMuY2FsbGJhY2sgPSB1bmRlZmluZWQ7XG4gICAgICAgIHRoaXMuZXZlbnRzLnNwbGljZSgwLCB0aGlzLmV2ZW50cy5sZW5ndGgpO1xuICAgIH1cblxuICAgIC8vIGVzbGludC1kaXNhYmxlLW5leHQtbGluZSBAdHlwZXNjcmlwdC1lc2xpbnQvbm8tZXhwbGljaXQtYW55XG4gICAgcHJvdGVjdGVkIHJlYWRNZXNzYWdlKG1lc3NhZ2U6IGFueSk6IHZvaWQge1xuICAgICAgICBpZiAodGhpcy5zdGF0ZSA9PT0gJ2luaXRpYWwnKSB7XG4gICAgICAgICAgICB0aGlzLmV2ZW50cy5zcGxpY2UoMCwgMCwgeyBtZXNzYWdlIH0pO1xuICAgICAgICB9IGVsc2UgaWYgKHRoaXMuc3RhdGUgPT09ICdsaXN0ZW5pbmcnKSB7XG4gICAgICAgICAgICB0cnkge1xuICAgICAgICAgICAgICAgIGNvbnN0IGRhdGEgPSBKU09OLnBhcnNlKG1lc3NhZ2UpO1xuICAgICAgICAgICAgICAgIHRoaXMuY2FsbGJhY2shKGRhdGEpO1xuICAgICAgICAgICAgfSBjYXRjaCAoZXJyKSB7XG4gICAgICAgICAgICAgICAgY29uc3QgZXJyb3I6IEVycm9yID0ge1xuICAgICAgICAgICAgICAgICAgICBuYW1lOiAnJyArIDQwMCxcbiAgICAgICAgICAgICAgICAgICAgLy8gZXNsaW50LWRpc2FibGUtbmV4dC1saW5lIEB0eXBlc2NyaXB0LWVzbGludC9uby1leHBsaWNpdC1hbnlcbiAgICAgICAgICAgICAgICAgICAgbWVzc2FnZTogYEVycm9yIGR1cmluZyBtZXNzYWdlIHBhcnNpbmcsIHJlYXNvbiA9ICR7dHlwZW9mIGVyciA9PT0gJ29iamVjdCcgPyAoZXJyIGFzIGFueSkubWVzc2FnZSA6ICd1bmtub3duJ31gXG4gICAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgICAgICB0aGlzLmZpcmVFcnJvcihlcnJvcik7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICB9XG5cbiAgICAvLyBlc2xpbnQtZGlzYWJsZS1uZXh0LWxpbmUgQHR5cGVzY3JpcHQtZXNsaW50L25vLWV4cGxpY2l0LWFueVxuICAgIHByb3RlY3RlZCBvdmVycmlkZSBmaXJlRXJyb3IoZXJyb3I6IGFueSk6IHZvaWQge1xuICAgICAgICBpZiAodGhpcy5zdGF0ZSA9PT0gJ2luaXRpYWwnKSB7XG4gICAgICAgICAgICB0aGlzLmV2ZW50cy5zcGxpY2UoMCwgMCwgeyBlcnJvciB9KTtcbiAgICAgICAgfSBlbHNlIGlmICh0aGlzLnN0YXRlID09PSAnbGlzdGVuaW5nJykge1xuICAgICAgICAgICAgc3VwZXIuZmlyZUVycm9yKGVycm9yKTtcbiAgICAgICAgfVxuICAgIH1cblxuICAgIHByb3RlY3RlZCBvdmVycmlkZSBmaXJlQ2xvc2UoKTogdm9pZCB7XG4gICAgICAgIGlmICh0aGlzLnN0YXRlID09PSAnaW5pdGlhbCcpIHtcbiAgICAgICAgICAgIHRoaXMuZXZlbnRzLnNwbGljZSgwLCAwLCB7fSk7XG4gICAgICAgIH0gZWxzZSBpZiAodGhpcy5zdGF0ZSA9PT0gJ2xpc3RlbmluZycpIHtcbiAgICAgICAgICAgIHN1cGVyLmZpcmVDbG9zZSgpO1xuICAgICAgICB9XG4gICAgICAgIHRoaXMuc3RhdGUgPSAnY2xvc2VkJztcbiAgICB9XG59XG4iLCAiLyogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbiAqIENvcHlyaWdodCAoYykgMjAyNCBUeXBlRm94IGFuZCBvdGhlcnMuXG4gKiBMaWNlbnNlZCB1bmRlciB0aGUgTUlUIExpY2Vuc2UuIFNlZSBMSUNFTlNFIGluIHRoZSBwYWNrYWdlIHJvb3QgZm9yIGxpY2Vuc2UgaW5mb3JtYXRpb24uXG4gKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0gKi9cblxuaW1wb3J0IHsgTWVzc2FnZSB9IGZyb20gJ3ZzY29kZS1qc29ucnBjJztcbmltcG9ydCB7IEFic3RyYWN0TWVzc2FnZVdyaXRlciwgTWVzc2FnZVdyaXRlciB9IGZyb20gJ3ZzY29kZS1qc29ucnBjJztcbmltcG9ydCB0eXBlIHsgSVdlYlNvY2tldCB9IGZyb20gJy4vc29ja2V0LmpzJztcblxuZXhwb3J0IGNsYXNzIFdlYlNvY2tldE1lc3NhZ2VXcml0ZXIgZXh0ZW5kcyBBYnN0cmFjdE1lc3NhZ2VXcml0ZXIgaW1wbGVtZW50cyBNZXNzYWdlV3JpdGVyIHtcbiAgICBwcm90ZWN0ZWQgZXJyb3JDb3VudCA9IDA7XG4gICAgcHJvdGVjdGVkIHJlYWRvbmx5IHNvY2tldDogSVdlYlNvY2tldDtcblxuICAgIGNvbnN0cnVjdG9yKHNvY2tldDogSVdlYlNvY2tldCkge1xuICAgICAgICBzdXBlcigpO1xuICAgICAgICB0aGlzLnNvY2tldCA9IHNvY2tldDtcbiAgICB9XG5cbiAgICBlbmQoKTogdm9pZCB7XG4gICAgfVxuXG4gICAgYXN5bmMgd3JpdGUobXNnOiBNZXNzYWdlKTogUHJvbWlzZTx2b2lkPiB7XG4gICAgICAgIHRyeSB7XG4gICAgICAgICAgICBjb25zdCBjb250ZW50ID0gSlNPTi5zdHJpbmdpZnkobXNnKTtcbiAgICAgICAgICAgIHRoaXMuc29ja2V0LnNlbmQoY29udGVudCk7XG4gICAgICAgIH0gY2F0Y2ggKGUpIHtcbiAgICAgICAgICAgIHRoaXMuZXJyb3JDb3VudCsrO1xuICAgICAgICAgICAgdGhpcy5maXJlRXJyb3IoZSwgbXNnLCB0aGlzLmVycm9yQ291bnQpO1xuICAgICAgICB9XG4gICAgfVxufVxuIiwgIi8qIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4gKiBDb3B5cmlnaHQgKGMpIDIwMjQgVHlwZUZveCBhbmQgb3RoZXJzLlxuICogTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTElDRU5TRSBpbiB0aGUgcGFja2FnZSByb290IGZvciBsaWNlbnNlIGluZm9ybWF0aW9uLlxuICogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tICovXG5cbmltcG9ydCB0eXBlIHsgTWVzc2FnZUNvbm5lY3Rpb24sIExvZ2dlciB9IGZyb20gJ3ZzY29kZS1qc29ucnBjJztcbmltcG9ydCB7IGNyZWF0ZU1lc3NhZ2VDb25uZWN0aW9uIH0gZnJvbSAndnNjb2RlLWpzb25ycGMnO1xuaW1wb3J0IHR5cGUgeyBJV2ViU29ja2V0IH0gZnJvbSAnLi9zb2NrZXQuanMnO1xuaW1wb3J0IHsgV2ViU29ja2V0TWVzc2FnZVJlYWRlciB9IGZyb20gJy4vcmVhZGVyLmpzJztcbmltcG9ydCB7IFdlYlNvY2tldE1lc3NhZ2VXcml0ZXIgfSBmcm9tICcuL3dyaXRlci5qcyc7XG5cbmV4cG9ydCBmdW5jdGlvbiBjcmVhdGVXZWJTb2NrZXRDb25uZWN0aW9uKHNvY2tldDogSVdlYlNvY2tldCwgbG9nZ2VyOiBMb2dnZXIpOiBNZXNzYWdlQ29ubmVjdGlvbiB7XG4gICAgY29uc3QgbWVzc2FnZVJlYWRlciA9IG5ldyBXZWJTb2NrZXRNZXNzYWdlUmVhZGVyKHNvY2tldCk7XG4gICAgY29uc3QgbWVzc2FnZVdyaXRlciA9IG5ldyBXZWJTb2NrZXRNZXNzYWdlV3JpdGVyKHNvY2tldCk7XG4gICAgY29uc3QgY29ubmVjdGlvbiA9IGNyZWF0ZU1lc3NhZ2VDb25uZWN0aW9uKG1lc3NhZ2VSZWFkZXIsIG1lc3NhZ2VXcml0ZXIsIGxvZ2dlcik7XG4gICAgY29ubmVjdGlvbi5vbkNsb3NlKCgpID0+IGNvbm5lY3Rpb24uZGlzcG9zZSgpKTtcbiAgICByZXR1cm4gY29ubmVjdGlvbjtcbn1cbiIsICIvKiAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuICogQ29weXJpZ2h0IChjKSAyMDI0IFR5cGVGb3ggYW5kIG90aGVycy5cbiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBNSVQgTGljZW5zZS4gU2VlIExJQ0VOU0UgaW4gdGhlIHBhY2thZ2Ugcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSAqL1xuXG5pbXBvcnQgdHlwZSB7IE1lc3NhZ2VDb25uZWN0aW9uLCBMb2dnZXIgfSBmcm9tICd2c2NvZGUtanNvbnJwYyc7XG5pbXBvcnQgeyBjcmVhdGVXZWJTb2NrZXRDb25uZWN0aW9uIH0gZnJvbSAnLi9zb2NrZXQvY29ubmVjdGlvbi5qcyc7XG5pbXBvcnQgdHlwZSB7IElXZWJTb2NrZXQgfSBmcm9tICcuL3NvY2tldC9zb2NrZXQuanMnO1xuaW1wb3J0IHsgQ29uc29sZUxvZ2dlciB9IGZyb20gJy4vbG9nZ2VyLmpzJztcblxuZXhwb3J0IGZ1bmN0aW9uIGxpc3RlbihvcHRpb25zOiB7XG4gICAgd2ViU29ja2V0OiBXZWJTb2NrZXQ7XG4gICAgbG9nZ2VyPzogTG9nZ2VyO1xuICAgIG9uQ29ubmVjdGlvbjogKGNvbm5lY3Rpb246IE1lc3NhZ2VDb25uZWN0aW9uKSA9PiB2b2lkO1xufSkge1xuICAgIGNvbnN0IHsgd2ViU29ja2V0LCBvbkNvbm5lY3Rpb24gfSA9IG9wdGlvbnM7XG4gICAgY29uc3QgbG9nZ2VyID0gb3B0aW9ucy5sb2dnZXIgfHwgbmV3IENvbnNvbGVMb2dnZXIoKTtcbiAgICB3ZWJTb2NrZXQub25vcGVuID0gKCkgPT4ge1xuICAgICAgICBjb25zdCBzb2NrZXQgPSB0b1NvY2tldCh3ZWJTb2NrZXQpO1xuICAgICAgICBjb25zdCBjb25uZWN0aW9uID0gY3JlYXRlV2ViU29ja2V0Q29ubmVjdGlvbihzb2NrZXQsIGxvZ2dlcik7XG4gICAgICAgIG9uQ29ubmVjdGlvbihjb25uZWN0aW9uKTtcbiAgICB9O1xufVxuXG5leHBvcnQgZnVuY3Rpb24gdG9Tb2NrZXQod2ViU29ja2V0OiBXZWJTb2NrZXQpOiBJV2ViU29ja2V0IHtcbiAgICByZXR1cm4ge1xuICAgICAgICBzZW5kOiBjb250ZW50ID0+IHdlYlNvY2tldC5zZW5kKGNvbnRlbnQpLFxuICAgICAgICBvbk1lc3NhZ2U6IGNiID0+IHtcbiAgICAgICAgICAgIHdlYlNvY2tldC5vbm1lc3NhZ2UgPSBldmVudCA9PiBjYihldmVudC5kYXRhKTtcbiAgICAgICAgfSxcbiAgICAgICAgb25FcnJvcjogY2IgPT4ge1xuICAgICAgICAgICAgLy8gZXNsaW50LWRpc2FibGUtbmV4dC1saW5lIEB0eXBlc2NyaXB0LWVzbGludC9uby1leHBsaWNpdC1hbnlcbiAgICAgICAgICAgIHdlYlNvY2tldC5vbmVycm9yID0gKGV2ZW50OiBhbnkpID0+IHtcbiAgICAgICAgICAgICAgICBpZiAoT2JqZWN0Lmhhc093bihldmVudCwgJ21lc3NhZ2UnKSkge1xuICAgICAgICAgICAgICAgICAgICBjYihldmVudC5tZXNzYWdlKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9O1xuICAgICAgICB9LFxuICAgICAgICBvbkNsb3NlOiBjYiA9PiB7XG4gICAgICAgICAgICB3ZWJTb2NrZXQub25jbG9zZSA9IGV2ZW50ID0+IGNiKGV2ZW50LmNvZGUsIGV2ZW50LnJlYXNvbik7XG4gICAgICAgIH0sXG4gICAgICAgIGRpc3Bvc2U6ICgpID0+IHdlYlNvY2tldC5jbG9zZSgpXG4gICAgfTtcbn1cbiIsICIvKlxuICogQ29weXJpZ2h0IChjKSAyMDEwLTIwMjYgRWNsaXBzZSBEaXJpZ2libGUgY29udHJpYnV0b3JzXG4gKlxuICogQWxsIHJpZ2h0cyByZXNlcnZlZC4gVGhpcyBwcm9ncmFtIGFuZCB0aGUgYWNjb21wYW55aW5nIG1hdGVyaWFscyBhcmUgbWFkZSBhdmFpbGFibGUgdW5kZXIgdGhlXG4gKiB0ZXJtcyBvZiB0aGUgRWNsaXBzZSBQdWJsaWMgTGljZW5zZSB2Mi4wIHdoaWNoIGFjY29tcGFuaWVzIHRoaXMgZGlzdHJpYnV0aW9uLCBhbmQgaXMgYXZhaWxhYmxlIGF0XG4gKiBodHRwOi8vd3d3LmVjbGlwc2Uub3JnL2xlZ2FsL2VwbC12MjAuaHRtbFxuICpcbiAqIFNQRFgtRmlsZUNvcHlyaWdodFRleHQ6IEVjbGlwc2UgRGlyaWdpYmxlIGNvbnRyaWJ1dG9ycyBTUERYLUxpY2Vuc2UtSWRlbnRpZmllcjogRVBMLTIuMFxuICovXG5cbi8qKlxuICogTGF6eSBQcm94eSBzaGltIGZvciBNb25hY28gRWRpdG9yIGxvYWRlZCB2aWEgQU1ELlxuICpcbiAqIGVkaXRvci5qcyBzZXRzIGdsb2JhbFRoaXMubW9uYWNvIGluc2lkZSB0aGUgQU1EIHJlcXVpcmUoKSBjYWxsYmFjaywgYmVmb3JlIGFueVxuICogSmF2YUxzcENsaWVudExpYi5jb25uZWN0KCkgY2FsbC4gQWxsIHByb3BlcnR5IGFjY2Vzc2VzIGdvIHRocm91Z2ggUHJveHkuZ2V0KCkgdHJhcHNcbiAqIHNvIHRoZXkgcmVzb2x2ZSBhZ2FpbnN0IHRoZSBsaXZlIHdpbmRvdy5tb25hY28gYXQgY2FsbCB0aW1lLCBub3QgYXQgYnVuZGxlIGxvYWQgdGltZS5cbiAqXG4gKiBUeXBlU2NyaXB0IHJlc29sdmVzIHR5cGVzIGZyb20gdGhlIHJlYWwgbW9uYWNvLWVkaXRvciBkZXZEZXBlbmRlbmN5OyBlc2J1aWxkIHJlcGxhY2VzXG4gKiB0aGUgXCJtb25hY28tZWRpdG9yXCIgaW1wb3J0IHdpdGggdGhpcyBmaWxlIGF0IGJ1bmRsZSB0aW1lIHZpYSB0aGUgYWxpYXMgb3B0aW9uLlxuICovXG5cbnR5cGUgTSA9IHR5cGVvZiBpbXBvcnQoJ21vbmFjby1lZGl0b3InKTtcblxuZnVuY3Rpb24gbSgpOiBNIHtcbiAgICByZXR1cm4gKGdsb2JhbFRoaXMgYXMgYW55KS5tb25hY28gYXMgTTtcbn1cblxuZnVuY3Rpb24gbnM8VCBleHRlbmRzIG9iamVjdD4oZ2V0dGVyOiAoKSA9PiBUKTogVCB7XG4gICAgcmV0dXJuIG5ldyBQcm94eSh7fSBhcyBULCB7XG4gICAgICAgIGdldDogKF8sIGspID0+IChnZXR0ZXIoKSBhcyBhbnkpW2sgYXMgc3RyaW5nXSxcbiAgICB9KTtcbn1cblxuZnVuY3Rpb24gY2xzPFQ+KGdldHRlcjogKCkgPT4gVCk6IFQge1xuICAgIHJldHVybiBuZXcgUHJveHkoZnVuY3Rpb24gKCkge30gYXMgYW55LCB7XG4gICAgICAgIGNvbnN0cnVjdDogKF8sIGFyZ3MpID0+IG5ldyAoZ2V0dGVyKCkgYXMgYW55KSguLi5hcmdzKSxcbiAgICAgICAgZ2V0OiAgICAgICAoXywgaykgICAgPT4gKGdldHRlcigpIGFzIGFueSlbayBhcyBzdHJpbmddLFxuICAgIH0pIGFzIFQ7XG59XG5cbmV4cG9ydCBjb25zdCBlZGl0b3IgICAgICAgICA9IG5zKCgpID0+IG0oKS5lZGl0b3IpO1xuZXhwb3J0IGNvbnN0IGxhbmd1YWdlcyAgICAgID0gbnMoKCkgPT4gbSgpLmxhbmd1YWdlcyk7XG5leHBvcnQgY29uc3QgTWFya2VyU2V2ZXJpdHkgPSBucygoKSA9PiBtKCkuTWFya2VyU2V2ZXJpdHkpO1xuZXhwb3J0IGNvbnN0IE1hcmtlclRhZyAgICAgID0gbnMoKCkgPT4gKG0oKSBhcyBhbnkpLk1hcmtlclRhZyk7XG5leHBvcnQgY29uc3QgVXJpICAgICAgICAgICAgPSBjbHMoKCkgPT4gbSgpLlVyaSk7XG5leHBvcnQgY29uc3QgUmFuZ2UgICAgICAgICAgPSBjbHMoKCkgPT4gbSgpLlJhbmdlKTtcbmV4cG9ydCBjb25zdCBQb3NpdGlvbiAgICAgICA9IGNscygoKSA9PiBtKCkuUG9zaXRpb24pO1xuZXhwb3J0IGNvbnN0IFNlbGVjdGlvbiAgICAgID0gY2xzKCgpID0+IG0oKS5TZWxlY3Rpb24pO1xuZXhwb3J0IGNvbnN0IEtleUNvZGUgICAgICAgID0gbnMoKCkgPT4gbSgpLktleUNvZGUpO1xuZXhwb3J0IGNvbnN0IEtleU1vZCAgICAgICAgID0gbnMoKCkgPT4gbSgpLktleU1vZCk7XG4iLCAiLyogLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbiAqIENvcHlyaWdodCAoYykgTWljcm9zb2Z0IENvcnBvcmF0aW9uLiBBbGwgcmlnaHRzIHJlc2VydmVkLlxuICogTGljZW5zZWQgdW5kZXIgdGhlIE1JVCBMaWNlbnNlLiBTZWUgTGljZW5zZS50eHQgaW4gdGhlIHByb2plY3Qgcm9vdCBmb3IgbGljZW5zZSBpbmZvcm1hdGlvbi5cbiAqIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLSAqL1xuJ3VzZSBzdHJpY3QnO1xuZXhwb3J0IHZhciBEb2N1bWVudFVyaTtcbihmdW5jdGlvbiAoRG9jdW1lbnRVcmkpIHtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICByZXR1cm4gdHlwZW9mIHZhbHVlID09PSAnc3RyaW5nJztcbiAgICB9XG4gICAgRG9jdW1lbnRVcmkuaXMgPSBpcztcbn0pKERvY3VtZW50VXJpIHx8IChEb2N1bWVudFVyaSA9IHt9KSk7XG5leHBvcnQgdmFyIFVSSTtcbihmdW5jdGlvbiAoVVJJKSB7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgcmV0dXJuIHR5cGVvZiB2YWx1ZSA9PT0gJ3N0cmluZyc7XG4gICAgfVxuICAgIFVSSS5pcyA9IGlzO1xufSkoVVJJIHx8IChVUkkgPSB7fSkpO1xuZXhwb3J0IHZhciBpbnRlZ2VyO1xuKGZ1bmN0aW9uIChpbnRlZ2VyKSB7XG4gICAgaW50ZWdlci5NSU5fVkFMVUUgPSAtMjE0NzQ4MzY0ODtcbiAgICBpbnRlZ2VyLk1BWF9WQUxVRSA9IDIxNDc0ODM2NDc7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgcmV0dXJuIHR5cGVvZiB2YWx1ZSA9PT0gJ251bWJlcicgJiYgaW50ZWdlci5NSU5fVkFMVUUgPD0gdmFsdWUgJiYgdmFsdWUgPD0gaW50ZWdlci5NQVhfVkFMVUU7XG4gICAgfVxuICAgIGludGVnZXIuaXMgPSBpcztcbn0pKGludGVnZXIgfHwgKGludGVnZXIgPSB7fSkpO1xuZXhwb3J0IHZhciB1aW50ZWdlcjtcbihmdW5jdGlvbiAodWludGVnZXIpIHtcbiAgICB1aW50ZWdlci5NSU5fVkFMVUUgPSAwO1xuICAgIHVpbnRlZ2VyLk1BWF9WQUxVRSA9IDIxNDc0ODM2NDc7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgcmV0dXJuIHR5cGVvZiB2YWx1ZSA9PT0gJ251bWJlcicgJiYgdWludGVnZXIuTUlOX1ZBTFVFIDw9IHZhbHVlICYmIHZhbHVlIDw9IHVpbnRlZ2VyLk1BWF9WQUxVRTtcbiAgICB9XG4gICAgdWludGVnZXIuaXMgPSBpcztcbn0pKHVpbnRlZ2VyIHx8ICh1aW50ZWdlciA9IHt9KSk7XG4vKipcbiAqIFRoZSBQb3NpdGlvbiBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBQb3NpdGlvbn0gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgUG9zaXRpb247XG4oZnVuY3Rpb24gKFBvc2l0aW9uKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBQb3NpdGlvbiBsaXRlcmFsIGZyb20gdGhlIGdpdmVuIGxpbmUgYW5kIGNoYXJhY3Rlci5cbiAgICAgKiBAcGFyYW0gbGluZSBUaGUgcG9zaXRpb24ncyBsaW5lLlxuICAgICAqIEBwYXJhbSBjaGFyYWN0ZXIgVGhlIHBvc2l0aW9uJ3MgY2hhcmFjdGVyLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShsaW5lLCBjaGFyYWN0ZXIpIHtcbiAgICAgICAgaWYgKGxpbmUgPT09IE51bWJlci5NQVhfVkFMVUUpIHtcbiAgICAgICAgICAgIGxpbmUgPSB1aW50ZWdlci5NQVhfVkFMVUU7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKGNoYXJhY3RlciA9PT0gTnVtYmVyLk1BWF9WQUxVRSkge1xuICAgICAgICAgICAgY2hhcmFjdGVyID0gdWludGVnZXIuTUFYX1ZBTFVFO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiB7IGxpbmUsIGNoYXJhY3RlciB9O1xuICAgIH1cbiAgICBQb3NpdGlvbi5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBQb3NpdGlvbn0gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLm9iamVjdExpdGVyYWwoY2FuZGlkYXRlKSAmJiBJcy51aW50ZWdlcihjYW5kaWRhdGUubGluZSkgJiYgSXMudWludGVnZXIoY2FuZGlkYXRlLmNoYXJhY3Rlcik7XG4gICAgfVxuICAgIFBvc2l0aW9uLmlzID0gaXM7XG59KShQb3NpdGlvbiB8fCAoUG9zaXRpb24gPSB7fSkpO1xuLyoqXG4gKiBUaGUgUmFuZ2UgbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgUmFuZ2V9IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIFJhbmdlO1xuKGZ1bmN0aW9uIChSYW5nZSkge1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShvbmUsIHR3bywgdGhyZWUsIGZvdXIpIHtcbiAgICAgICAgaWYgKElzLnVpbnRlZ2VyKG9uZSkgJiYgSXMudWludGVnZXIodHdvKSAmJiBJcy51aW50ZWdlcih0aHJlZSkgJiYgSXMudWludGVnZXIoZm91cikpIHtcbiAgICAgICAgICAgIHJldHVybiB7IHN0YXJ0OiBQb3NpdGlvbi5jcmVhdGUob25lLCB0d28pLCBlbmQ6IFBvc2l0aW9uLmNyZWF0ZSh0aHJlZSwgZm91cikgfTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIGlmIChQb3NpdGlvbi5pcyhvbmUpICYmIFBvc2l0aW9uLmlzKHR3bykpIHtcbiAgICAgICAgICAgIHJldHVybiB7IHN0YXJ0OiBvbmUsIGVuZDogdHdvIH07XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoYFJhbmdlI2NyZWF0ZSBjYWxsZWQgd2l0aCBpbnZhbGlkIGFyZ3VtZW50c1ske29uZX0sICR7dHdvfSwgJHt0aHJlZX0sICR7Zm91cn1dYCk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgUmFuZ2UuY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgUmFuZ2V9IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5vYmplY3RMaXRlcmFsKGNhbmRpZGF0ZSkgJiYgUG9zaXRpb24uaXMoY2FuZGlkYXRlLnN0YXJ0KSAmJiBQb3NpdGlvbi5pcyhjYW5kaWRhdGUuZW5kKTtcbiAgICB9XG4gICAgUmFuZ2UuaXMgPSBpcztcbn0pKFJhbmdlIHx8IChSYW5nZSA9IHt9KSk7XG4vKipcbiAqIFRoZSBMb2NhdGlvbiBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBMb2NhdGlvbn0gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgTG9jYXRpb247XG4oZnVuY3Rpb24gKExvY2F0aW9uKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIExvY2F0aW9uIGxpdGVyYWwuXG4gICAgICogQHBhcmFtIHVyaSBUaGUgbG9jYXRpb24ncyB1cmkuXG4gICAgICogQHBhcmFtIHJhbmdlIFRoZSBsb2NhdGlvbidzIHJhbmdlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZSh1cmksIHJhbmdlKSB7XG4gICAgICAgIHJldHVybiB7IHVyaSwgcmFuZ2UgfTtcbiAgICB9XG4gICAgTG9jYXRpb24uY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgTG9jYXRpb259IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5vYmplY3RMaXRlcmFsKGNhbmRpZGF0ZSkgJiYgUmFuZ2UuaXMoY2FuZGlkYXRlLnJhbmdlKSAmJiAoSXMuc3RyaW5nKGNhbmRpZGF0ZS51cmkpIHx8IElzLnVuZGVmaW5lZChjYW5kaWRhdGUudXJpKSk7XG4gICAgfVxuICAgIExvY2F0aW9uLmlzID0gaXM7XG59KShMb2NhdGlvbiB8fCAoTG9jYXRpb24gPSB7fSkpO1xuLyoqXG4gKiBUaGUgTG9jYXRpb25MaW5rIG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIExvY2F0aW9uTGlua30gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgTG9jYXRpb25MaW5rO1xuKGZ1bmN0aW9uIChMb2NhdGlvbkxpbmspIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgTG9jYXRpb25MaW5rIGxpdGVyYWwuXG4gICAgICogQHBhcmFtIHRhcmdldFVyaSBUaGUgZGVmaW5pdGlvbidzIHVyaS5cbiAgICAgKiBAcGFyYW0gdGFyZ2V0UmFuZ2UgVGhlIGZ1bGwgcmFuZ2Ugb2YgdGhlIGRlZmluaXRpb24uXG4gICAgICogQHBhcmFtIHRhcmdldFNlbGVjdGlvblJhbmdlIFRoZSBzcGFuIG9mIHRoZSBzeW1ib2wgZGVmaW5pdGlvbiBhdCB0aGUgdGFyZ2V0LlxuICAgICAqIEBwYXJhbSBvcmlnaW5TZWxlY3Rpb25SYW5nZSBUaGUgc3BhbiBvZiB0aGUgc3ltYm9sIGJlaW5nIGRlZmluZWQgaW4gdGhlIG9yaWdpbmF0aW5nIHNvdXJjZSBmaWxlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZSh0YXJnZXRVcmksIHRhcmdldFJhbmdlLCB0YXJnZXRTZWxlY3Rpb25SYW5nZSwgb3JpZ2luU2VsZWN0aW9uUmFuZ2UpIHtcbiAgICAgICAgcmV0dXJuIHsgdGFyZ2V0VXJpLCB0YXJnZXRSYW5nZSwgdGFyZ2V0U2VsZWN0aW9uUmFuZ2UsIG9yaWdpblNlbGVjdGlvblJhbmdlIH07XG4gICAgfVxuICAgIExvY2F0aW9uTGluay5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBMb2NhdGlvbkxpbmt9IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5vYmplY3RMaXRlcmFsKGNhbmRpZGF0ZSkgJiYgUmFuZ2UuaXMoY2FuZGlkYXRlLnRhcmdldFJhbmdlKSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLnRhcmdldFVyaSlcbiAgICAgICAgICAgICYmIFJhbmdlLmlzKGNhbmRpZGF0ZS50YXJnZXRTZWxlY3Rpb25SYW5nZSlcbiAgICAgICAgICAgICYmIChSYW5nZS5pcyhjYW5kaWRhdGUub3JpZ2luU2VsZWN0aW9uUmFuZ2UpIHx8IElzLnVuZGVmaW5lZChjYW5kaWRhdGUub3JpZ2luU2VsZWN0aW9uUmFuZ2UpKTtcbiAgICB9XG4gICAgTG9jYXRpb25MaW5rLmlzID0gaXM7XG59KShMb2NhdGlvbkxpbmsgfHwgKExvY2F0aW9uTGluayA9IHt9KSk7XG4vKipcbiAqIFRoZSBDb2xvciBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBDb2xvcn0gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgQ29sb3I7XG4oZnVuY3Rpb24gKENvbG9yKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBDb2xvciBsaXRlcmFsLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShyZWQsIGdyZWVuLCBibHVlLCBhbHBoYSkge1xuICAgICAgICByZXR1cm4ge1xuICAgICAgICAgICAgcmVkLFxuICAgICAgICAgICAgZ3JlZW4sXG4gICAgICAgICAgICBibHVlLFxuICAgICAgICAgICAgYWxwaGEsXG4gICAgICAgIH07XG4gICAgfVxuICAgIENvbG9yLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIENvbG9yfSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5vYmplY3RMaXRlcmFsKGNhbmRpZGF0ZSkgJiYgSXMubnVtYmVyUmFuZ2UoY2FuZGlkYXRlLnJlZCwgMCwgMSlcbiAgICAgICAgICAgICYmIElzLm51bWJlclJhbmdlKGNhbmRpZGF0ZS5ncmVlbiwgMCwgMSlcbiAgICAgICAgICAgICYmIElzLm51bWJlclJhbmdlKGNhbmRpZGF0ZS5ibHVlLCAwLCAxKVxuICAgICAgICAgICAgJiYgSXMubnVtYmVyUmFuZ2UoY2FuZGlkYXRlLmFscGhhLCAwLCAxKTtcbiAgICB9XG4gICAgQ29sb3IuaXMgPSBpcztcbn0pKENvbG9yIHx8IChDb2xvciA9IHt9KSk7XG4vKipcbiAqIFRoZSBDb2xvckluZm9ybWF0aW9uIG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIENvbG9ySW5mb3JtYXRpb259IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIENvbG9ySW5mb3JtYXRpb247XG4oZnVuY3Rpb24gKENvbG9ySW5mb3JtYXRpb24pIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IENvbG9ySW5mb3JtYXRpb24gbGl0ZXJhbC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUocmFuZ2UsIGNvbG9yKSB7XG4gICAgICAgIHJldHVybiB7XG4gICAgICAgICAgICByYW5nZSxcbiAgICAgICAgICAgIGNvbG9yLFxuICAgICAgICB9O1xuICAgIH1cbiAgICBDb2xvckluZm9ybWF0aW9uLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIENvbG9ySW5mb3JtYXRpb259IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLm9iamVjdExpdGVyYWwoY2FuZGlkYXRlKSAmJiBSYW5nZS5pcyhjYW5kaWRhdGUucmFuZ2UpICYmIENvbG9yLmlzKGNhbmRpZGF0ZS5jb2xvcik7XG4gICAgfVxuICAgIENvbG9ySW5mb3JtYXRpb24uaXMgPSBpcztcbn0pKENvbG9ySW5mb3JtYXRpb24gfHwgKENvbG9ySW5mb3JtYXRpb24gPSB7fSkpO1xuLyoqXG4gKiBUaGUgQ29sb3IgbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgQ29sb3JQcmVzZW50YXRpb259IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIENvbG9yUHJlc2VudGF0aW9uO1xuKGZ1bmN0aW9uIChDb2xvclByZXNlbnRhdGlvbikge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgQ29sb3JJbmZvcm1hdGlvbiBsaXRlcmFsLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShsYWJlbCwgdGV4dEVkaXQsIGFkZGl0aW9uYWxUZXh0RWRpdHMpIHtcbiAgICAgICAgcmV0dXJuIHtcbiAgICAgICAgICAgIGxhYmVsLFxuICAgICAgICAgICAgdGV4dEVkaXQsXG4gICAgICAgICAgICBhZGRpdGlvbmFsVGV4dEVkaXRzLFxuICAgICAgICB9O1xuICAgIH1cbiAgICBDb2xvclByZXNlbnRhdGlvbi5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBDb2xvckluZm9ybWF0aW9ufSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5vYmplY3RMaXRlcmFsKGNhbmRpZGF0ZSkgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS5sYWJlbClcbiAgICAgICAgICAgICYmIChJcy51bmRlZmluZWQoY2FuZGlkYXRlLnRleHRFZGl0KSB8fCBUZXh0RWRpdC5pcyhjYW5kaWRhdGUpKVxuICAgICAgICAgICAgJiYgKElzLnVuZGVmaW5lZChjYW5kaWRhdGUuYWRkaXRpb25hbFRleHRFZGl0cykgfHwgSXMudHlwZWRBcnJheShjYW5kaWRhdGUuYWRkaXRpb25hbFRleHRFZGl0cywgVGV4dEVkaXQuaXMpKTtcbiAgICB9XG4gICAgQ29sb3JQcmVzZW50YXRpb24uaXMgPSBpcztcbn0pKENvbG9yUHJlc2VudGF0aW9uIHx8IChDb2xvclByZXNlbnRhdGlvbiA9IHt9KSk7XG4vKipcbiAqIEEgc2V0IG9mIHByZWRlZmluZWQgcmFuZ2Uga2luZHMuXG4gKi9cbmV4cG9ydCB2YXIgRm9sZGluZ1JhbmdlS2luZDtcbihmdW5jdGlvbiAoRm9sZGluZ1JhbmdlS2luZCkge1xuICAgIC8qKlxuICAgICAqIEZvbGRpbmcgcmFuZ2UgZm9yIGEgY29tbWVudFxuICAgICAqL1xuICAgIEZvbGRpbmdSYW5nZUtpbmQuQ29tbWVudCA9ICdjb21tZW50JztcbiAgICAvKipcbiAgICAgKiBGb2xkaW5nIHJhbmdlIGZvciBhbiBpbXBvcnQgb3IgaW5jbHVkZVxuICAgICAqL1xuICAgIEZvbGRpbmdSYW5nZUtpbmQuSW1wb3J0cyA9ICdpbXBvcnRzJztcbiAgICAvKipcbiAgICAgKiBGb2xkaW5nIHJhbmdlIGZvciBhIHJlZ2lvbiAoZS5nLiBgI3JlZ2lvbmApXG4gICAgICovXG4gICAgRm9sZGluZ1JhbmdlS2luZC5SZWdpb24gPSAncmVnaW9uJztcbn0pKEZvbGRpbmdSYW5nZUtpbmQgfHwgKEZvbGRpbmdSYW5nZUtpbmQgPSB7fSkpO1xuLyoqXG4gKiBUaGUgZm9sZGluZyByYW5nZSBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBGb2xkaW5nUmFuZ2V9IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIEZvbGRpbmdSYW5nZTtcbihmdW5jdGlvbiAoRm9sZGluZ1JhbmdlKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBGb2xkaW5nUmFuZ2UgbGl0ZXJhbC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUoc3RhcnRMaW5lLCBlbmRMaW5lLCBzdGFydENoYXJhY3RlciwgZW5kQ2hhcmFjdGVyLCBraW5kLCBjb2xsYXBzZWRUZXh0KSB7XG4gICAgICAgIGNvbnN0IHJlc3VsdCA9IHtcbiAgICAgICAgICAgIHN0YXJ0TGluZSxcbiAgICAgICAgICAgIGVuZExpbmVcbiAgICAgICAgfTtcbiAgICAgICAgaWYgKElzLmRlZmluZWQoc3RhcnRDaGFyYWN0ZXIpKSB7XG4gICAgICAgICAgICByZXN1bHQuc3RhcnRDaGFyYWN0ZXIgPSBzdGFydENoYXJhY3RlcjtcbiAgICAgICAgfVxuICAgICAgICBpZiAoSXMuZGVmaW5lZChlbmRDaGFyYWN0ZXIpKSB7XG4gICAgICAgICAgICByZXN1bHQuZW5kQ2hhcmFjdGVyID0gZW5kQ2hhcmFjdGVyO1xuICAgICAgICB9XG4gICAgICAgIGlmIChJcy5kZWZpbmVkKGtpbmQpKSB7XG4gICAgICAgICAgICByZXN1bHQua2luZCA9IGtpbmQ7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKElzLmRlZmluZWQoY29sbGFwc2VkVGV4dCkpIHtcbiAgICAgICAgICAgIHJlc3VsdC5jb2xsYXBzZWRUZXh0ID0gY29sbGFwc2VkVGV4dDtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cbiAgICBGb2xkaW5nUmFuZ2UuY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgRm9sZGluZ1JhbmdlfSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5vYmplY3RMaXRlcmFsKGNhbmRpZGF0ZSkgJiYgSXMudWludGVnZXIoY2FuZGlkYXRlLnN0YXJ0TGluZSkgJiYgSXMudWludGVnZXIoY2FuZGlkYXRlLnN0YXJ0TGluZSlcbiAgICAgICAgICAgICYmIChJcy51bmRlZmluZWQoY2FuZGlkYXRlLnN0YXJ0Q2hhcmFjdGVyKSB8fCBJcy51aW50ZWdlcihjYW5kaWRhdGUuc3RhcnRDaGFyYWN0ZXIpKVxuICAgICAgICAgICAgJiYgKElzLnVuZGVmaW5lZChjYW5kaWRhdGUuZW5kQ2hhcmFjdGVyKSB8fCBJcy51aW50ZWdlcihjYW5kaWRhdGUuZW5kQ2hhcmFjdGVyKSlcbiAgICAgICAgICAgICYmIChJcy51bmRlZmluZWQoY2FuZGlkYXRlLmtpbmQpIHx8IElzLnN0cmluZyhjYW5kaWRhdGUua2luZCkpO1xuICAgIH1cbiAgICBGb2xkaW5nUmFuZ2UuaXMgPSBpcztcbn0pKEZvbGRpbmdSYW5nZSB8fCAoRm9sZGluZ1JhbmdlID0ge30pKTtcbi8qKlxuICogVGhlIERpYWdub3N0aWNSZWxhdGVkSW5mb3JtYXRpb24gbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgRGlhZ25vc3RpY1JlbGF0ZWRJbmZvcm1hdGlvbn0gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgRGlhZ25vc3RpY1JlbGF0ZWRJbmZvcm1hdGlvbjtcbihmdW5jdGlvbiAoRGlhZ25vc3RpY1JlbGF0ZWRJbmZvcm1hdGlvbikge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgRGlhZ25vc3RpY1JlbGF0ZWRJbmZvcm1hdGlvbiBsaXRlcmFsLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShsb2NhdGlvbiwgbWVzc2FnZSkge1xuICAgICAgICByZXR1cm4ge1xuICAgICAgICAgICAgbG9jYXRpb24sXG4gICAgICAgICAgICBtZXNzYWdlXG4gICAgICAgIH07XG4gICAgfVxuICAgIERpYWdub3N0aWNSZWxhdGVkSW5mb3JtYXRpb24uY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgRGlhZ25vc3RpY1JlbGF0ZWRJbmZvcm1hdGlvbn0gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLmRlZmluZWQoY2FuZGlkYXRlKSAmJiBMb2NhdGlvbi5pcyhjYW5kaWRhdGUubG9jYXRpb24pICYmIElzLnN0cmluZyhjYW5kaWRhdGUubWVzc2FnZSk7XG4gICAgfVxuICAgIERpYWdub3N0aWNSZWxhdGVkSW5mb3JtYXRpb24uaXMgPSBpcztcbn0pKERpYWdub3N0aWNSZWxhdGVkSW5mb3JtYXRpb24gfHwgKERpYWdub3N0aWNSZWxhdGVkSW5mb3JtYXRpb24gPSB7fSkpO1xuLyoqXG4gKiBUaGUgZGlhZ25vc3RpYydzIHNldmVyaXR5LlxuICovXG5leHBvcnQgdmFyIERpYWdub3N0aWNTZXZlcml0eTtcbihmdW5jdGlvbiAoRGlhZ25vc3RpY1NldmVyaXR5KSB7XG4gICAgLyoqXG4gICAgICogUmVwb3J0cyBhbiBlcnJvci5cbiAgICAgKi9cbiAgICBEaWFnbm9zdGljU2V2ZXJpdHkuRXJyb3IgPSAxO1xuICAgIC8qKlxuICAgICAqIFJlcG9ydHMgYSB3YXJuaW5nLlxuICAgICAqL1xuICAgIERpYWdub3N0aWNTZXZlcml0eS5XYXJuaW5nID0gMjtcbiAgICAvKipcbiAgICAgKiBSZXBvcnRzIGFuIGluZm9ybWF0aW9uLlxuICAgICAqL1xuICAgIERpYWdub3N0aWNTZXZlcml0eS5JbmZvcm1hdGlvbiA9IDM7XG4gICAgLyoqXG4gICAgICogUmVwb3J0cyBhIGhpbnQuXG4gICAgICovXG4gICAgRGlhZ25vc3RpY1NldmVyaXR5LkhpbnQgPSA0O1xufSkoRGlhZ25vc3RpY1NldmVyaXR5IHx8IChEaWFnbm9zdGljU2V2ZXJpdHkgPSB7fSkpO1xuLyoqXG4gKiBUaGUgZGlhZ25vc3RpYyB0YWdzLlxuICpcbiAqIEBzaW5jZSAzLjE1LjBcbiAqL1xuZXhwb3J0IHZhciBEaWFnbm9zdGljVGFnO1xuKGZ1bmN0aW9uIChEaWFnbm9zdGljVGFnKSB7XG4gICAgLyoqXG4gICAgICogVW51c2VkIG9yIHVubmVjZXNzYXJ5IGNvZGUuXG4gICAgICpcbiAgICAgKiBDbGllbnRzIGFyZSBhbGxvd2VkIHRvIHJlbmRlciBkaWFnbm9zdGljcyB3aXRoIHRoaXMgdGFnIGZhZGVkIG91dCBpbnN0ZWFkIG9mIGhhdmluZ1xuICAgICAqIGFuIGVycm9yIHNxdWlnZ2xlLlxuICAgICAqL1xuICAgIERpYWdub3N0aWNUYWcuVW5uZWNlc3NhcnkgPSAxO1xuICAgIC8qKlxuICAgICAqIERlcHJlY2F0ZWQgb3Igb2Jzb2xldGUgY29kZS5cbiAgICAgKlxuICAgICAqIENsaWVudHMgYXJlIGFsbG93ZWQgdG8gcmVuZGVyZWQgZGlhZ25vc3RpY3Mgd2l0aCB0aGlzIHRhZyBzdHJpa2UgdGhyb3VnaC5cbiAgICAgKi9cbiAgICBEaWFnbm9zdGljVGFnLkRlcHJlY2F0ZWQgPSAyO1xufSkoRGlhZ25vc3RpY1RhZyB8fCAoRGlhZ25vc3RpY1RhZyA9IHt9KSk7XG4vKipcbiAqIFRoZSBDb2RlRGVzY3JpcHRpb24gbmFtZXNwYWNlIHByb3ZpZGVzIGZ1bmN0aW9ucyB0byBkZWFsIHdpdGggZGVzY3JpcHRpb25zIGZvciBkaWFnbm9zdGljIGNvZGVzLlxuICpcbiAqIEBzaW5jZSAzLjE2LjBcbiAqL1xuZXhwb3J0IHZhciBDb2RlRGVzY3JpcHRpb247XG4oZnVuY3Rpb24gKENvZGVEZXNjcmlwdGlvbikge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMub2JqZWN0TGl0ZXJhbChjYW5kaWRhdGUpICYmIElzLnN0cmluZyhjYW5kaWRhdGUuaHJlZik7XG4gICAgfVxuICAgIENvZGVEZXNjcmlwdGlvbi5pcyA9IGlzO1xufSkoQ29kZURlc2NyaXB0aW9uIHx8IChDb2RlRGVzY3JpcHRpb24gPSB7fSkpO1xuLyoqXG4gKiBUaGUgRGlhZ25vc3RpYyBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9ucyB0byB3b3JrIHdpdGhcbiAqIHtAbGluayBEaWFnbm9zdGljfSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBEaWFnbm9zdGljO1xuKGZ1bmN0aW9uIChEaWFnbm9zdGljKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBEaWFnbm9zdGljIGxpdGVyYWwuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKHJhbmdlLCBtZXNzYWdlLCBzZXZlcml0eSwgY29kZSwgc291cmNlLCByZWxhdGVkSW5mb3JtYXRpb24pIHtcbiAgICAgICAgbGV0IHJlc3VsdCA9IHsgcmFuZ2UsIG1lc3NhZ2UgfTtcbiAgICAgICAgaWYgKElzLmRlZmluZWQoc2V2ZXJpdHkpKSB7XG4gICAgICAgICAgICByZXN1bHQuc2V2ZXJpdHkgPSBzZXZlcml0eTtcbiAgICAgICAgfVxuICAgICAgICBpZiAoSXMuZGVmaW5lZChjb2RlKSkge1xuICAgICAgICAgICAgcmVzdWx0LmNvZGUgPSBjb2RlO1xuICAgICAgICB9XG4gICAgICAgIGlmIChJcy5kZWZpbmVkKHNvdXJjZSkpIHtcbiAgICAgICAgICAgIHJlc3VsdC5zb3VyY2UgPSBzb3VyY2U7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKElzLmRlZmluZWQocmVsYXRlZEluZm9ybWF0aW9uKSkge1xuICAgICAgICAgICAgcmVzdWx0LnJlbGF0ZWRJbmZvcm1hdGlvbiA9IHJlbGF0ZWRJbmZvcm1hdGlvbjtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cbiAgICBEaWFnbm9zdGljLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIERpYWdub3N0aWN9IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICB2YXIgX2E7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLmRlZmluZWQoY2FuZGlkYXRlKVxuICAgICAgICAgICAgJiYgUmFuZ2UuaXMoY2FuZGlkYXRlLnJhbmdlKVxuICAgICAgICAgICAgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS5tZXNzYWdlKVxuICAgICAgICAgICAgJiYgKElzLm51bWJlcihjYW5kaWRhdGUuc2V2ZXJpdHkpIHx8IElzLnVuZGVmaW5lZChjYW5kaWRhdGUuc2V2ZXJpdHkpKVxuICAgICAgICAgICAgJiYgKElzLmludGVnZXIoY2FuZGlkYXRlLmNvZGUpIHx8IElzLnN0cmluZyhjYW5kaWRhdGUuY29kZSkgfHwgSXMudW5kZWZpbmVkKGNhbmRpZGF0ZS5jb2RlKSlcbiAgICAgICAgICAgICYmIChJcy51bmRlZmluZWQoY2FuZGlkYXRlLmNvZGVEZXNjcmlwdGlvbikgfHwgKElzLnN0cmluZygoX2EgPSBjYW5kaWRhdGUuY29kZURlc2NyaXB0aW9uKSA9PT0gbnVsbCB8fCBfYSA9PT0gdm9pZCAwID8gdm9pZCAwIDogX2EuaHJlZikpKVxuICAgICAgICAgICAgJiYgKElzLnN0cmluZyhjYW5kaWRhdGUuc291cmNlKSB8fCBJcy51bmRlZmluZWQoY2FuZGlkYXRlLnNvdXJjZSkpXG4gICAgICAgICAgICAmJiAoSXMudW5kZWZpbmVkKGNhbmRpZGF0ZS5yZWxhdGVkSW5mb3JtYXRpb24pIHx8IElzLnR5cGVkQXJyYXkoY2FuZGlkYXRlLnJlbGF0ZWRJbmZvcm1hdGlvbiwgRGlhZ25vc3RpY1JlbGF0ZWRJbmZvcm1hdGlvbi5pcykpO1xuICAgIH1cbiAgICBEaWFnbm9zdGljLmlzID0gaXM7XG59KShEaWFnbm9zdGljIHx8IChEaWFnbm9zdGljID0ge30pKTtcbi8qKlxuICogVGhlIENvbW1hbmQgbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgQ29tbWFuZH0gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgQ29tbWFuZDtcbihmdW5jdGlvbiAoQ29tbWFuZCkge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgQ29tbWFuZCBsaXRlcmFsLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZSh0aXRsZSwgY29tbWFuZCwgLi4uYXJncykge1xuICAgICAgICBsZXQgcmVzdWx0ID0geyB0aXRsZSwgY29tbWFuZCB9O1xuICAgICAgICBpZiAoSXMuZGVmaW5lZChhcmdzKSAmJiBhcmdzLmxlbmd0aCA+IDApIHtcbiAgICAgICAgICAgIHJlc3VsdC5hcmd1bWVudHMgPSBhcmdzO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxuICAgIENvbW1hbmQuY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgQ29tbWFuZH0gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLmRlZmluZWQoY2FuZGlkYXRlKSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLnRpdGxlKSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLmNvbW1hbmQpO1xuICAgIH1cbiAgICBDb21tYW5kLmlzID0gaXM7XG59KShDb21tYW5kIHx8IChDb21tYW5kID0ge30pKTtcbi8qKlxuICogVGhlIFRleHRFZGl0IG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb24gdG8gY3JlYXRlIHJlcGxhY2UsXG4gKiBpbnNlcnQgYW5kIGRlbGV0ZSBlZGl0cyBtb3JlIGVhc2lseS5cbiAqL1xuZXhwb3J0IHZhciBUZXh0RWRpdDtcbihmdW5jdGlvbiAoVGV4dEVkaXQpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgcmVwbGFjZSB0ZXh0IGVkaXQuXG4gICAgICogQHBhcmFtIHJhbmdlIFRoZSByYW5nZSBvZiB0ZXh0IHRvIGJlIHJlcGxhY2VkLlxuICAgICAqIEBwYXJhbSBuZXdUZXh0IFRoZSBuZXcgdGV4dC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiByZXBsYWNlKHJhbmdlLCBuZXdUZXh0KSB7XG4gICAgICAgIHJldHVybiB7IHJhbmdlLCBuZXdUZXh0IH07XG4gICAgfVxuICAgIFRleHRFZGl0LnJlcGxhY2UgPSByZXBsYWNlO1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYW4gaW5zZXJ0IHRleHQgZWRpdC5cbiAgICAgKiBAcGFyYW0gcG9zaXRpb24gVGhlIHBvc2l0aW9uIHRvIGluc2VydCB0aGUgdGV4dCBhdC5cbiAgICAgKiBAcGFyYW0gbmV3VGV4dCBUaGUgdGV4dCB0byBiZSBpbnNlcnRlZC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpbnNlcnQocG9zaXRpb24sIG5ld1RleHQpIHtcbiAgICAgICAgcmV0dXJuIHsgcmFuZ2U6IHsgc3RhcnQ6IHBvc2l0aW9uLCBlbmQ6IHBvc2l0aW9uIH0sIG5ld1RleHQgfTtcbiAgICB9XG4gICAgVGV4dEVkaXQuaW5zZXJ0ID0gaW5zZXJ0O1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBkZWxldGUgdGV4dCBlZGl0LlxuICAgICAqIEBwYXJhbSByYW5nZSBUaGUgcmFuZ2Ugb2YgdGV4dCB0byBiZSBkZWxldGVkLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGRlbChyYW5nZSkge1xuICAgICAgICByZXR1cm4geyByYW5nZSwgbmV3VGV4dDogJycgfTtcbiAgICB9XG4gICAgVGV4dEVkaXQuZGVsID0gZGVsO1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMub2JqZWN0TGl0ZXJhbChjYW5kaWRhdGUpXG4gICAgICAgICAgICAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLm5ld1RleHQpXG4gICAgICAgICAgICAmJiBSYW5nZS5pcyhjYW5kaWRhdGUucmFuZ2UpO1xuICAgIH1cbiAgICBUZXh0RWRpdC5pcyA9IGlzO1xufSkoVGV4dEVkaXQgfHwgKFRleHRFZGl0ID0ge30pKTtcbmV4cG9ydCB2YXIgQ2hhbmdlQW5ub3RhdGlvbjtcbihmdW5jdGlvbiAoQ2hhbmdlQW5ub3RhdGlvbikge1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShsYWJlbCwgbmVlZHNDb25maXJtYXRpb24sIGRlc2NyaXB0aW9uKSB7XG4gICAgICAgIGNvbnN0IHJlc3VsdCA9IHsgbGFiZWwgfTtcbiAgICAgICAgaWYgKG5lZWRzQ29uZmlybWF0aW9uICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHJlc3VsdC5uZWVkc0NvbmZpcm1hdGlvbiA9IG5lZWRzQ29uZmlybWF0aW9uO1xuICAgICAgICB9XG4gICAgICAgIGlmIChkZXNjcmlwdGlvbiAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXN1bHQuZGVzY3JpcHRpb24gPSBkZXNjcmlwdGlvbjtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cbiAgICBDaGFuZ2VBbm5vdGF0aW9uLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLm9iamVjdExpdGVyYWwoY2FuZGlkYXRlKSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLmxhYmVsKSAmJlxuICAgICAgICAgICAgKElzLmJvb2xlYW4oY2FuZGlkYXRlLm5lZWRzQ29uZmlybWF0aW9uKSB8fCBjYW5kaWRhdGUubmVlZHNDb25maXJtYXRpb24gPT09IHVuZGVmaW5lZCkgJiZcbiAgICAgICAgICAgIChJcy5zdHJpbmcoY2FuZGlkYXRlLmRlc2NyaXB0aW9uKSB8fCBjYW5kaWRhdGUuZGVzY3JpcHRpb24gPT09IHVuZGVmaW5lZCk7XG4gICAgfVxuICAgIENoYW5nZUFubm90YXRpb24uaXMgPSBpcztcbn0pKENoYW5nZUFubm90YXRpb24gfHwgKENoYW5nZUFubm90YXRpb24gPSB7fSkpO1xuZXhwb3J0IHZhciBDaGFuZ2VBbm5vdGF0aW9uSWRlbnRpZmllcjtcbihmdW5jdGlvbiAoQ2hhbmdlQW5ub3RhdGlvbklkZW50aWZpZXIpIHtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLnN0cmluZyhjYW5kaWRhdGUpO1xuICAgIH1cbiAgICBDaGFuZ2VBbm5vdGF0aW9uSWRlbnRpZmllci5pcyA9IGlzO1xufSkoQ2hhbmdlQW5ub3RhdGlvbklkZW50aWZpZXIgfHwgKENoYW5nZUFubm90YXRpb25JZGVudGlmaWVyID0ge30pKTtcbmV4cG9ydCB2YXIgQW5ub3RhdGVkVGV4dEVkaXQ7XG4oZnVuY3Rpb24gKEFubm90YXRlZFRleHRFZGl0KSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhbiBhbm5vdGF0ZWQgcmVwbGFjZSB0ZXh0IGVkaXQuXG4gICAgICpcbiAgICAgKiBAcGFyYW0gcmFuZ2UgVGhlIHJhbmdlIG9mIHRleHQgdG8gYmUgcmVwbGFjZWQuXG4gICAgICogQHBhcmFtIG5ld1RleHQgVGhlIG5ldyB0ZXh0LlxuICAgICAqIEBwYXJhbSBhbm5vdGF0aW9uIFRoZSBhbm5vdGF0aW9uLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIHJlcGxhY2UocmFuZ2UsIG5ld1RleHQsIGFubm90YXRpb24pIHtcbiAgICAgICAgcmV0dXJuIHsgcmFuZ2UsIG5ld1RleHQsIGFubm90YXRpb25JZDogYW5ub3RhdGlvbiB9O1xuICAgIH1cbiAgICBBbm5vdGF0ZWRUZXh0RWRpdC5yZXBsYWNlID0gcmVwbGFjZTtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGFuIGFubm90YXRlZCBpbnNlcnQgdGV4dCBlZGl0LlxuICAgICAqXG4gICAgICogQHBhcmFtIHBvc2l0aW9uIFRoZSBwb3NpdGlvbiB0byBpbnNlcnQgdGhlIHRleHQgYXQuXG4gICAgICogQHBhcmFtIG5ld1RleHQgVGhlIHRleHQgdG8gYmUgaW5zZXJ0ZWQuXG4gICAgICogQHBhcmFtIGFubm90YXRpb24gVGhlIGFubm90YXRpb24uXG4gICAgICovXG4gICAgZnVuY3Rpb24gaW5zZXJ0KHBvc2l0aW9uLCBuZXdUZXh0LCBhbm5vdGF0aW9uKSB7XG4gICAgICAgIHJldHVybiB7IHJhbmdlOiB7IHN0YXJ0OiBwb3NpdGlvbiwgZW5kOiBwb3NpdGlvbiB9LCBuZXdUZXh0LCBhbm5vdGF0aW9uSWQ6IGFubm90YXRpb24gfTtcbiAgICB9XG4gICAgQW5ub3RhdGVkVGV4dEVkaXQuaW5zZXJ0ID0gaW5zZXJ0O1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYW4gYW5ub3RhdGVkIGRlbGV0ZSB0ZXh0IGVkaXQuXG4gICAgICpcbiAgICAgKiBAcGFyYW0gcmFuZ2UgVGhlIHJhbmdlIG9mIHRleHQgdG8gYmUgZGVsZXRlZC5cbiAgICAgKiBAcGFyYW0gYW5ub3RhdGlvbiBUaGUgYW5ub3RhdGlvbi5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBkZWwocmFuZ2UsIGFubm90YXRpb24pIHtcbiAgICAgICAgcmV0dXJuIHsgcmFuZ2UsIG5ld1RleHQ6ICcnLCBhbm5vdGF0aW9uSWQ6IGFubm90YXRpb24gfTtcbiAgICB9XG4gICAgQW5ub3RhdGVkVGV4dEVkaXQuZGVsID0gZGVsO1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gVGV4dEVkaXQuaXMoY2FuZGlkYXRlKSAmJiAoQ2hhbmdlQW5ub3RhdGlvbi5pcyhjYW5kaWRhdGUuYW5ub3RhdGlvbklkKSB8fCBDaGFuZ2VBbm5vdGF0aW9uSWRlbnRpZmllci5pcyhjYW5kaWRhdGUuYW5ub3RhdGlvbklkKSk7XG4gICAgfVxuICAgIEFubm90YXRlZFRleHRFZGl0LmlzID0gaXM7XG59KShBbm5vdGF0ZWRUZXh0RWRpdCB8fCAoQW5ub3RhdGVkVGV4dEVkaXQgPSB7fSkpO1xuLyoqXG4gKiBUaGUgVGV4dERvY3VtZW50RWRpdCBuYW1lc3BhY2UgcHJvdmlkZXMgaGVscGVyIGZ1bmN0aW9uIHRvIGNyZWF0ZVxuICogYW4gZWRpdCB0aGF0IG1hbmlwdWxhdGVzIGEgdGV4dCBkb2N1bWVudC5cbiAqL1xuZXhwb3J0IHZhciBUZXh0RG9jdW1lbnRFZGl0O1xuKGZ1bmN0aW9uIChUZXh0RG9jdW1lbnRFZGl0KSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBgVGV4dERvY3VtZW50RWRpdGBcbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUodGV4dERvY3VtZW50LCBlZGl0cykge1xuICAgICAgICByZXR1cm4geyB0ZXh0RG9jdW1lbnQsIGVkaXRzIH07XG4gICAgfVxuICAgIFRleHREb2N1bWVudEVkaXQuY3JlYXRlID0gY3JlYXRlO1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLmRlZmluZWQoY2FuZGlkYXRlKVxuICAgICAgICAgICAgJiYgT3B0aW9uYWxWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyLmlzKGNhbmRpZGF0ZS50ZXh0RG9jdW1lbnQpXG4gICAgICAgICAgICAmJiBBcnJheS5pc0FycmF5KGNhbmRpZGF0ZS5lZGl0cyk7XG4gICAgfVxuICAgIFRleHREb2N1bWVudEVkaXQuaXMgPSBpcztcbn0pKFRleHREb2N1bWVudEVkaXQgfHwgKFRleHREb2N1bWVudEVkaXQgPSB7fSkpO1xuZXhwb3J0IHZhciBDcmVhdGVGaWxlO1xuKGZ1bmN0aW9uIChDcmVhdGVGaWxlKSB7XG4gICAgZnVuY3Rpb24gY3JlYXRlKHVyaSwgb3B0aW9ucywgYW5ub3RhdGlvbikge1xuICAgICAgICBsZXQgcmVzdWx0ID0ge1xuICAgICAgICAgICAga2luZDogJ2NyZWF0ZScsXG4gICAgICAgICAgICB1cmlcbiAgICAgICAgfTtcbiAgICAgICAgaWYgKG9wdGlvbnMgIT09IHVuZGVmaW5lZCAmJiAob3B0aW9ucy5vdmVyd3JpdGUgIT09IHVuZGVmaW5lZCB8fCBvcHRpb25zLmlnbm9yZUlmRXhpc3RzICE9PSB1bmRlZmluZWQpKSB7XG4gICAgICAgICAgICByZXN1bHQub3B0aW9ucyA9IG9wdGlvbnM7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKGFubm90YXRpb24gIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmVzdWx0LmFubm90YXRpb25JZCA9IGFubm90YXRpb247XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICB9XG4gICAgQ3JlYXRlRmlsZS5jcmVhdGUgPSBjcmVhdGU7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICYmIGNhbmRpZGF0ZS5raW5kID09PSAnY3JlYXRlJyAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLnVyaSkgJiYgKGNhbmRpZGF0ZS5vcHRpb25zID09PSB1bmRlZmluZWQgfHxcbiAgICAgICAgICAgICgoY2FuZGlkYXRlLm9wdGlvbnMub3ZlcndyaXRlID09PSB1bmRlZmluZWQgfHwgSXMuYm9vbGVhbihjYW5kaWRhdGUub3B0aW9ucy5vdmVyd3JpdGUpKSAmJiAoY2FuZGlkYXRlLm9wdGlvbnMuaWdub3JlSWZFeGlzdHMgPT09IHVuZGVmaW5lZCB8fCBJcy5ib29sZWFuKGNhbmRpZGF0ZS5vcHRpb25zLmlnbm9yZUlmRXhpc3RzKSkpKSAmJiAoY2FuZGlkYXRlLmFubm90YXRpb25JZCA9PT0gdW5kZWZpbmVkIHx8IENoYW5nZUFubm90YXRpb25JZGVudGlmaWVyLmlzKGNhbmRpZGF0ZS5hbm5vdGF0aW9uSWQpKTtcbiAgICB9XG4gICAgQ3JlYXRlRmlsZS5pcyA9IGlzO1xufSkoQ3JlYXRlRmlsZSB8fCAoQ3JlYXRlRmlsZSA9IHt9KSk7XG5leHBvcnQgdmFyIFJlbmFtZUZpbGU7XG4oZnVuY3Rpb24gKFJlbmFtZUZpbGUpIHtcbiAgICBmdW5jdGlvbiBjcmVhdGUob2xkVXJpLCBuZXdVcmksIG9wdGlvbnMsIGFubm90YXRpb24pIHtcbiAgICAgICAgbGV0IHJlc3VsdCA9IHtcbiAgICAgICAgICAgIGtpbmQ6ICdyZW5hbWUnLFxuICAgICAgICAgICAgb2xkVXJpLFxuICAgICAgICAgICAgbmV3VXJpXG4gICAgICAgIH07XG4gICAgICAgIGlmIChvcHRpb25zICE9PSB1bmRlZmluZWQgJiYgKG9wdGlvbnMub3ZlcndyaXRlICE9PSB1bmRlZmluZWQgfHwgb3B0aW9ucy5pZ25vcmVJZkV4aXN0cyAhPT0gdW5kZWZpbmVkKSkge1xuICAgICAgICAgICAgcmVzdWx0Lm9wdGlvbnMgPSBvcHRpb25zO1xuICAgICAgICB9XG4gICAgICAgIGlmIChhbm5vdGF0aW9uICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHJlc3VsdC5hbm5vdGF0aW9uSWQgPSBhbm5vdGF0aW9uO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxuICAgIFJlbmFtZUZpbGUuY3JlYXRlID0gY3JlYXRlO1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiBjYW5kaWRhdGUua2luZCA9PT0gJ3JlbmFtZScgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS5vbGRVcmkpICYmIElzLnN0cmluZyhjYW5kaWRhdGUubmV3VXJpKSAmJiAoY2FuZGlkYXRlLm9wdGlvbnMgPT09IHVuZGVmaW5lZCB8fFxuICAgICAgICAgICAgKChjYW5kaWRhdGUub3B0aW9ucy5vdmVyd3JpdGUgPT09IHVuZGVmaW5lZCB8fCBJcy5ib29sZWFuKGNhbmRpZGF0ZS5vcHRpb25zLm92ZXJ3cml0ZSkpICYmIChjYW5kaWRhdGUub3B0aW9ucy5pZ25vcmVJZkV4aXN0cyA9PT0gdW5kZWZpbmVkIHx8IElzLmJvb2xlYW4oY2FuZGlkYXRlLm9wdGlvbnMuaWdub3JlSWZFeGlzdHMpKSkpICYmIChjYW5kaWRhdGUuYW5ub3RhdGlvbklkID09PSB1bmRlZmluZWQgfHwgQ2hhbmdlQW5ub3RhdGlvbklkZW50aWZpZXIuaXMoY2FuZGlkYXRlLmFubm90YXRpb25JZCkpO1xuICAgIH1cbiAgICBSZW5hbWVGaWxlLmlzID0gaXM7XG59KShSZW5hbWVGaWxlIHx8IChSZW5hbWVGaWxlID0ge30pKTtcbmV4cG9ydCB2YXIgRGVsZXRlRmlsZTtcbihmdW5jdGlvbiAoRGVsZXRlRmlsZSkge1xuICAgIGZ1bmN0aW9uIGNyZWF0ZSh1cmksIG9wdGlvbnMsIGFubm90YXRpb24pIHtcbiAgICAgICAgbGV0IHJlc3VsdCA9IHtcbiAgICAgICAgICAgIGtpbmQ6ICdkZWxldGUnLFxuICAgICAgICAgICAgdXJpXG4gICAgICAgIH07XG4gICAgICAgIGlmIChvcHRpb25zICE9PSB1bmRlZmluZWQgJiYgKG9wdGlvbnMucmVjdXJzaXZlICE9PSB1bmRlZmluZWQgfHwgb3B0aW9ucy5pZ25vcmVJZk5vdEV4aXN0cyAhPT0gdW5kZWZpbmVkKSkge1xuICAgICAgICAgICAgcmVzdWx0Lm9wdGlvbnMgPSBvcHRpb25zO1xuICAgICAgICB9XG4gICAgICAgIGlmIChhbm5vdGF0aW9uICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHJlc3VsdC5hbm5vdGF0aW9uSWQgPSBhbm5vdGF0aW9uO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxuICAgIERlbGV0ZUZpbGUuY3JlYXRlID0gY3JlYXRlO1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiBjYW5kaWRhdGUua2luZCA9PT0gJ2RlbGV0ZScgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS51cmkpICYmIChjYW5kaWRhdGUub3B0aW9ucyA9PT0gdW5kZWZpbmVkIHx8XG4gICAgICAgICAgICAoKGNhbmRpZGF0ZS5vcHRpb25zLnJlY3Vyc2l2ZSA9PT0gdW5kZWZpbmVkIHx8IElzLmJvb2xlYW4oY2FuZGlkYXRlLm9wdGlvbnMucmVjdXJzaXZlKSkgJiYgKGNhbmRpZGF0ZS5vcHRpb25zLmlnbm9yZUlmTm90RXhpc3RzID09PSB1bmRlZmluZWQgfHwgSXMuYm9vbGVhbihjYW5kaWRhdGUub3B0aW9ucy5pZ25vcmVJZk5vdEV4aXN0cykpKSkgJiYgKGNhbmRpZGF0ZS5hbm5vdGF0aW9uSWQgPT09IHVuZGVmaW5lZCB8fCBDaGFuZ2VBbm5vdGF0aW9uSWRlbnRpZmllci5pcyhjYW5kaWRhdGUuYW5ub3RhdGlvbklkKSk7XG4gICAgfVxuICAgIERlbGV0ZUZpbGUuaXMgPSBpcztcbn0pKERlbGV0ZUZpbGUgfHwgKERlbGV0ZUZpbGUgPSB7fSkpO1xuZXhwb3J0IHZhciBXb3Jrc3BhY2VFZGl0O1xuKGZ1bmN0aW9uIChXb3Jrc3BhY2VFZGl0KSB7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICYmXG4gICAgICAgICAgICAoY2FuZGlkYXRlLmNoYW5nZXMgIT09IHVuZGVmaW5lZCB8fCBjYW5kaWRhdGUuZG9jdW1lbnRDaGFuZ2VzICE9PSB1bmRlZmluZWQpICYmXG4gICAgICAgICAgICAoY2FuZGlkYXRlLmRvY3VtZW50Q2hhbmdlcyA9PT0gdW5kZWZpbmVkIHx8IGNhbmRpZGF0ZS5kb2N1bWVudENoYW5nZXMuZXZlcnkoKGNoYW5nZSkgPT4ge1xuICAgICAgICAgICAgICAgIGlmIChJcy5zdHJpbmcoY2hhbmdlLmtpbmQpKSB7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiBDcmVhdGVGaWxlLmlzKGNoYW5nZSkgfHwgUmVuYW1lRmlsZS5pcyhjaGFuZ2UpIHx8IERlbGV0ZUZpbGUuaXMoY2hhbmdlKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiBUZXh0RG9jdW1lbnRFZGl0LmlzKGNoYW5nZSk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfSkpO1xuICAgIH1cbiAgICBXb3Jrc3BhY2VFZGl0LmlzID0gaXM7XG59KShXb3Jrc3BhY2VFZGl0IHx8IChXb3Jrc3BhY2VFZGl0ID0ge30pKTtcbmNsYXNzIFRleHRFZGl0Q2hhbmdlSW1wbCB7XG4gICAgY29uc3RydWN0b3IoZWRpdHMsIGNoYW5nZUFubm90YXRpb25zKSB7XG4gICAgICAgIHRoaXMuZWRpdHMgPSBlZGl0cztcbiAgICAgICAgdGhpcy5jaGFuZ2VBbm5vdGF0aW9ucyA9IGNoYW5nZUFubm90YXRpb25zO1xuICAgIH1cbiAgICBpbnNlcnQocG9zaXRpb24sIG5ld1RleHQsIGFubm90YXRpb24pIHtcbiAgICAgICAgbGV0IGVkaXQ7XG4gICAgICAgIGxldCBpZDtcbiAgICAgICAgaWYgKGFubm90YXRpb24gPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgZWRpdCA9IFRleHRFZGl0Lmluc2VydChwb3NpdGlvbiwgbmV3VGV4dCk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSBpZiAoQ2hhbmdlQW5ub3RhdGlvbklkZW50aWZpZXIuaXMoYW5ub3RhdGlvbikpIHtcbiAgICAgICAgICAgIGlkID0gYW5ub3RhdGlvbjtcbiAgICAgICAgICAgIGVkaXQgPSBBbm5vdGF0ZWRUZXh0RWRpdC5pbnNlcnQocG9zaXRpb24sIG5ld1RleHQsIGFubm90YXRpb24pO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgdGhpcy5hc3NlcnRDaGFuZ2VBbm5vdGF0aW9ucyh0aGlzLmNoYW5nZUFubm90YXRpb25zKTtcbiAgICAgICAgICAgIGlkID0gdGhpcy5jaGFuZ2VBbm5vdGF0aW9ucy5tYW5hZ2UoYW5ub3RhdGlvbik7XG4gICAgICAgICAgICBlZGl0ID0gQW5ub3RhdGVkVGV4dEVkaXQuaW5zZXJ0KHBvc2l0aW9uLCBuZXdUZXh0LCBpZCk7XG4gICAgICAgIH1cbiAgICAgICAgdGhpcy5lZGl0cy5wdXNoKGVkaXQpO1xuICAgICAgICBpZiAoaWQgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmV0dXJuIGlkO1xuICAgICAgICB9XG4gICAgfVxuICAgIHJlcGxhY2UocmFuZ2UsIG5ld1RleHQsIGFubm90YXRpb24pIHtcbiAgICAgICAgbGV0IGVkaXQ7XG4gICAgICAgIGxldCBpZDtcbiAgICAgICAgaWYgKGFubm90YXRpb24gPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgZWRpdCA9IFRleHRFZGl0LnJlcGxhY2UocmFuZ2UsIG5ld1RleHQpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2UgaWYgKENoYW5nZUFubm90YXRpb25JZGVudGlmaWVyLmlzKGFubm90YXRpb24pKSB7XG4gICAgICAgICAgICBpZCA9IGFubm90YXRpb247XG4gICAgICAgICAgICBlZGl0ID0gQW5ub3RhdGVkVGV4dEVkaXQucmVwbGFjZShyYW5nZSwgbmV3VGV4dCwgYW5ub3RhdGlvbik7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICB0aGlzLmFzc2VydENoYW5nZUFubm90YXRpb25zKHRoaXMuY2hhbmdlQW5ub3RhdGlvbnMpO1xuICAgICAgICAgICAgaWQgPSB0aGlzLmNoYW5nZUFubm90YXRpb25zLm1hbmFnZShhbm5vdGF0aW9uKTtcbiAgICAgICAgICAgIGVkaXQgPSBBbm5vdGF0ZWRUZXh0RWRpdC5yZXBsYWNlKHJhbmdlLCBuZXdUZXh0LCBpZCk7XG4gICAgICAgIH1cbiAgICAgICAgdGhpcy5lZGl0cy5wdXNoKGVkaXQpO1xuICAgICAgICBpZiAoaWQgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmV0dXJuIGlkO1xuICAgICAgICB9XG4gICAgfVxuICAgIGRlbGV0ZShyYW5nZSwgYW5ub3RhdGlvbikge1xuICAgICAgICBsZXQgZWRpdDtcbiAgICAgICAgbGV0IGlkO1xuICAgICAgICBpZiAoYW5ub3RhdGlvbiA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICBlZGl0ID0gVGV4dEVkaXQuZGVsKHJhbmdlKTtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIGlmIChDaGFuZ2VBbm5vdGF0aW9uSWRlbnRpZmllci5pcyhhbm5vdGF0aW9uKSkge1xuICAgICAgICAgICAgaWQgPSBhbm5vdGF0aW9uO1xuICAgICAgICAgICAgZWRpdCA9IEFubm90YXRlZFRleHRFZGl0LmRlbChyYW5nZSwgYW5ub3RhdGlvbik7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICB0aGlzLmFzc2VydENoYW5nZUFubm90YXRpb25zKHRoaXMuY2hhbmdlQW5ub3RhdGlvbnMpO1xuICAgICAgICAgICAgaWQgPSB0aGlzLmNoYW5nZUFubm90YXRpb25zLm1hbmFnZShhbm5vdGF0aW9uKTtcbiAgICAgICAgICAgIGVkaXQgPSBBbm5vdGF0ZWRUZXh0RWRpdC5kZWwocmFuZ2UsIGlkKTtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLmVkaXRzLnB1c2goZWRpdCk7XG4gICAgICAgIGlmIChpZCAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXR1cm4gaWQ7XG4gICAgICAgIH1cbiAgICB9XG4gICAgYWRkKGVkaXQpIHtcbiAgICAgICAgdGhpcy5lZGl0cy5wdXNoKGVkaXQpO1xuICAgIH1cbiAgICBhbGwoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLmVkaXRzO1xuICAgIH1cbiAgICBjbGVhcigpIHtcbiAgICAgICAgdGhpcy5lZGl0cy5zcGxpY2UoMCwgdGhpcy5lZGl0cy5sZW5ndGgpO1xuICAgIH1cbiAgICBhc3NlcnRDaGFuZ2VBbm5vdGF0aW9ucyh2YWx1ZSkge1xuICAgICAgICBpZiAodmFsdWUgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBUZXh0IGVkaXQgY2hhbmdlIGlzIG5vdCBjb25maWd1cmVkIHRvIG1hbmFnZSBjaGFuZ2UgYW5ub3RhdGlvbnMuYCk7XG4gICAgICAgIH1cbiAgICB9XG59XG4vKipcbiAqIEEgaGVscGVyIGNsYXNzXG4gKi9cbmNsYXNzIENoYW5nZUFubm90YXRpb25zIHtcbiAgICBjb25zdHJ1Y3Rvcihhbm5vdGF0aW9ucykge1xuICAgICAgICB0aGlzLl9hbm5vdGF0aW9ucyA9IGFubm90YXRpb25zID09PSB1bmRlZmluZWQgPyBPYmplY3QuY3JlYXRlKG51bGwpIDogYW5ub3RhdGlvbnM7XG4gICAgICAgIHRoaXMuX2NvdW50ZXIgPSAwO1xuICAgICAgICB0aGlzLl9zaXplID0gMDtcbiAgICB9XG4gICAgYWxsKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5fYW5ub3RhdGlvbnM7XG4gICAgfVxuICAgIGdldCBzaXplKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5fc2l6ZTtcbiAgICB9XG4gICAgbWFuYWdlKGlkT3JBbm5vdGF0aW9uLCBhbm5vdGF0aW9uKSB7XG4gICAgICAgIGxldCBpZDtcbiAgICAgICAgaWYgKENoYW5nZUFubm90YXRpb25JZGVudGlmaWVyLmlzKGlkT3JBbm5vdGF0aW9uKSkge1xuICAgICAgICAgICAgaWQgPSBpZE9yQW5ub3RhdGlvbjtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIGlkID0gdGhpcy5uZXh0SWQoKTtcbiAgICAgICAgICAgIGFubm90YXRpb24gPSBpZE9yQW5ub3RhdGlvbjtcbiAgICAgICAgfVxuICAgICAgICBpZiAodGhpcy5fYW5ub3RhdGlvbnNbaWRdICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcihgSWQgJHtpZH0gaXMgYWxyZWFkeSBpbiB1c2UuYCk7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKGFubm90YXRpb24gPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKGBObyBhbm5vdGF0aW9uIHByb3ZpZGVkIGZvciBpZCAke2lkfWApO1xuICAgICAgICB9XG4gICAgICAgIHRoaXMuX2Fubm90YXRpb25zW2lkXSA9IGFubm90YXRpb247XG4gICAgICAgIHRoaXMuX3NpemUrKztcbiAgICAgICAgcmV0dXJuIGlkO1xuICAgIH1cbiAgICBuZXh0SWQoKSB7XG4gICAgICAgIHRoaXMuX2NvdW50ZXIrKztcbiAgICAgICAgcmV0dXJuIHRoaXMuX2NvdW50ZXIudG9TdHJpbmcoKTtcbiAgICB9XG59XG4vKipcbiAqIEEgd29ya3NwYWNlIGNoYW5nZSBoZWxwcyBjb25zdHJ1Y3RpbmcgY2hhbmdlcyB0byBhIHdvcmtzcGFjZS5cbiAqL1xuZXhwb3J0IGNsYXNzIFdvcmtzcGFjZUNoYW5nZSB7XG4gICAgY29uc3RydWN0b3Iod29ya3NwYWNlRWRpdCkge1xuICAgICAgICB0aGlzLl90ZXh0RWRpdENoYW5nZXMgPSBPYmplY3QuY3JlYXRlKG51bGwpO1xuICAgICAgICBpZiAod29ya3NwYWNlRWRpdCAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICB0aGlzLl93b3Jrc3BhY2VFZGl0ID0gd29ya3NwYWNlRWRpdDtcbiAgICAgICAgICAgIGlmICh3b3Jrc3BhY2VFZGl0LmRvY3VtZW50Q2hhbmdlcykge1xuICAgICAgICAgICAgICAgIHRoaXMuX2NoYW5nZUFubm90YXRpb25zID0gbmV3IENoYW5nZUFubm90YXRpb25zKHdvcmtzcGFjZUVkaXQuY2hhbmdlQW5ub3RhdGlvbnMpO1xuICAgICAgICAgICAgICAgIHdvcmtzcGFjZUVkaXQuY2hhbmdlQW5ub3RhdGlvbnMgPSB0aGlzLl9jaGFuZ2VBbm5vdGF0aW9ucy5hbGwoKTtcbiAgICAgICAgICAgICAgICB3b3Jrc3BhY2VFZGl0LmRvY3VtZW50Q2hhbmdlcy5mb3JFYWNoKChjaGFuZ2UpID0+IHtcbiAgICAgICAgICAgICAgICAgICAgaWYgKFRleHREb2N1bWVudEVkaXQuaXMoY2hhbmdlKSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgY29uc3QgdGV4dEVkaXRDaGFuZ2UgPSBuZXcgVGV4dEVkaXRDaGFuZ2VJbXBsKGNoYW5nZS5lZGl0cywgdGhpcy5fY2hhbmdlQW5ub3RhdGlvbnMpO1xuICAgICAgICAgICAgICAgICAgICAgICAgdGhpcy5fdGV4dEVkaXRDaGFuZ2VzW2NoYW5nZS50ZXh0RG9jdW1lbnQudXJpXSA9IHRleHRFZGl0Q2hhbmdlO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIGlmICh3b3Jrc3BhY2VFZGl0LmNoYW5nZXMpIHtcbiAgICAgICAgICAgICAgICBPYmplY3Qua2V5cyh3b3Jrc3BhY2VFZGl0LmNoYW5nZXMpLmZvckVhY2goKGtleSkgPT4ge1xuICAgICAgICAgICAgICAgICAgICBjb25zdCB0ZXh0RWRpdENoYW5nZSA9IG5ldyBUZXh0RWRpdENoYW5nZUltcGwod29ya3NwYWNlRWRpdC5jaGFuZ2VzW2tleV0pO1xuICAgICAgICAgICAgICAgICAgICB0aGlzLl90ZXh0RWRpdENoYW5nZXNba2V5XSA9IHRleHRFZGl0Q2hhbmdlO1xuICAgICAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgdGhpcy5fd29ya3NwYWNlRWRpdCA9IHt9O1xuICAgICAgICB9XG4gICAgfVxuICAgIC8qKlxuICAgICAqIFJldHVybnMgdGhlIHVuZGVybHlpbmcge0BsaW5rIFdvcmtzcGFjZUVkaXR9IGxpdGVyYWxcbiAgICAgKiB1c2UgdG8gYmUgcmV0dXJuZWQgZnJvbSBhIHdvcmtzcGFjZSBlZGl0IG9wZXJhdGlvbiBsaWtlIHJlbmFtZS5cbiAgICAgKi9cbiAgICBnZXQgZWRpdCgpIHtcbiAgICAgICAgdGhpcy5pbml0RG9jdW1lbnRDaGFuZ2VzKCk7XG4gICAgICAgIGlmICh0aGlzLl9jaGFuZ2VBbm5vdGF0aW9ucyAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICBpZiAodGhpcy5fY2hhbmdlQW5ub3RhdGlvbnMuc2l6ZSA9PT0gMCkge1xuICAgICAgICAgICAgICAgIHRoaXMuX3dvcmtzcGFjZUVkaXQuY2hhbmdlQW5ub3RhdGlvbnMgPSB1bmRlZmluZWQ7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICB0aGlzLl93b3Jrc3BhY2VFZGl0LmNoYW5nZUFubm90YXRpb25zID0gdGhpcy5fY2hhbmdlQW5ub3RhdGlvbnMuYWxsKCk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHRoaXMuX3dvcmtzcGFjZUVkaXQ7XG4gICAgfVxuICAgIGdldFRleHRFZGl0Q2hhbmdlKGtleSkge1xuICAgICAgICBpZiAoT3B0aW9uYWxWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyLmlzKGtleSkpIHtcbiAgICAgICAgICAgIHRoaXMuaW5pdERvY3VtZW50Q2hhbmdlcygpO1xuICAgICAgICAgICAgaWYgKHRoaXMuX3dvcmtzcGFjZUVkaXQuZG9jdW1lbnRDaGFuZ2VzID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoJ1dvcmtzcGFjZSBlZGl0IGlzIG5vdCBjb25maWd1cmVkIGZvciBkb2N1bWVudCBjaGFuZ2VzLicpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgY29uc3QgdGV4dERvY3VtZW50ID0geyB1cmk6IGtleS51cmksIHZlcnNpb246IGtleS52ZXJzaW9uIH07XG4gICAgICAgICAgICBsZXQgcmVzdWx0ID0gdGhpcy5fdGV4dEVkaXRDaGFuZ2VzW3RleHREb2N1bWVudC51cmldO1xuICAgICAgICAgICAgaWYgKCFyZXN1bHQpIHtcbiAgICAgICAgICAgICAgICBjb25zdCBlZGl0cyA9IFtdO1xuICAgICAgICAgICAgICAgIGNvbnN0IHRleHREb2N1bWVudEVkaXQgPSB7XG4gICAgICAgICAgICAgICAgICAgIHRleHREb2N1bWVudCxcbiAgICAgICAgICAgICAgICAgICAgZWRpdHNcbiAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICAgIHRoaXMuX3dvcmtzcGFjZUVkaXQuZG9jdW1lbnRDaGFuZ2VzLnB1c2godGV4dERvY3VtZW50RWRpdCk7XG4gICAgICAgICAgICAgICAgcmVzdWx0ID0gbmV3IFRleHRFZGl0Q2hhbmdlSW1wbChlZGl0cywgdGhpcy5fY2hhbmdlQW5ub3RhdGlvbnMpO1xuICAgICAgICAgICAgICAgIHRoaXMuX3RleHRFZGl0Q2hhbmdlc1t0ZXh0RG9jdW1lbnQudXJpXSA9IHJlc3VsdDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICB0aGlzLmluaXRDaGFuZ2VzKCk7XG4gICAgICAgICAgICBpZiAodGhpcy5fd29ya3NwYWNlRWRpdC5jaGFuZ2VzID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoJ1dvcmtzcGFjZSBlZGl0IGlzIG5vdCBjb25maWd1cmVkIGZvciBub3JtYWwgdGV4dCBlZGl0IGNoYW5nZXMuJyk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBsZXQgcmVzdWx0ID0gdGhpcy5fdGV4dEVkaXRDaGFuZ2VzW2tleV07XG4gICAgICAgICAgICBpZiAoIXJlc3VsdCkge1xuICAgICAgICAgICAgICAgIGxldCBlZGl0cyA9IFtdO1xuICAgICAgICAgICAgICAgIHRoaXMuX3dvcmtzcGFjZUVkaXQuY2hhbmdlc1trZXldID0gZWRpdHM7XG4gICAgICAgICAgICAgICAgcmVzdWx0ID0gbmV3IFRleHRFZGl0Q2hhbmdlSW1wbChlZGl0cyk7XG4gICAgICAgICAgICAgICAgdGhpcy5fdGV4dEVkaXRDaGFuZ2VzW2tleV0gPSByZXN1bHQ7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgICAgICB9XG4gICAgfVxuICAgIGluaXREb2N1bWVudENoYW5nZXMoKSB7XG4gICAgICAgIGlmICh0aGlzLl93b3Jrc3BhY2VFZGl0LmRvY3VtZW50Q2hhbmdlcyA9PT0gdW5kZWZpbmVkICYmIHRoaXMuX3dvcmtzcGFjZUVkaXQuY2hhbmdlcyA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICB0aGlzLl9jaGFuZ2VBbm5vdGF0aW9ucyA9IG5ldyBDaGFuZ2VBbm5vdGF0aW9ucygpO1xuICAgICAgICAgICAgdGhpcy5fd29ya3NwYWNlRWRpdC5kb2N1bWVudENoYW5nZXMgPSBbXTtcbiAgICAgICAgICAgIHRoaXMuX3dvcmtzcGFjZUVkaXQuY2hhbmdlQW5ub3RhdGlvbnMgPSB0aGlzLl9jaGFuZ2VBbm5vdGF0aW9ucy5hbGwoKTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBpbml0Q2hhbmdlcygpIHtcbiAgICAgICAgaWYgKHRoaXMuX3dvcmtzcGFjZUVkaXQuZG9jdW1lbnRDaGFuZ2VzID09PSB1bmRlZmluZWQgJiYgdGhpcy5fd29ya3NwYWNlRWRpdC5jaGFuZ2VzID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHRoaXMuX3dvcmtzcGFjZUVkaXQuY2hhbmdlcyA9IE9iamVjdC5jcmVhdGUobnVsbCk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgY3JlYXRlRmlsZSh1cmksIG9wdGlvbnNPckFubm90YXRpb24sIG9wdGlvbnMpIHtcbiAgICAgICAgdGhpcy5pbml0RG9jdW1lbnRDaGFuZ2VzKCk7XG4gICAgICAgIGlmICh0aGlzLl93b3Jrc3BhY2VFZGl0LmRvY3VtZW50Q2hhbmdlcyA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoJ1dvcmtzcGFjZSBlZGl0IGlzIG5vdCBjb25maWd1cmVkIGZvciBkb2N1bWVudCBjaGFuZ2VzLicpO1xuICAgICAgICB9XG4gICAgICAgIGxldCBhbm5vdGF0aW9uO1xuICAgICAgICBpZiAoQ2hhbmdlQW5ub3RhdGlvbi5pcyhvcHRpb25zT3JBbm5vdGF0aW9uKSB8fCBDaGFuZ2VBbm5vdGF0aW9uSWRlbnRpZmllci5pcyhvcHRpb25zT3JBbm5vdGF0aW9uKSkge1xuICAgICAgICAgICAgYW5ub3RhdGlvbiA9IG9wdGlvbnNPckFubm90YXRpb247XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBvcHRpb25zID0gb3B0aW9uc09yQW5ub3RhdGlvbjtcbiAgICAgICAgfVxuICAgICAgICBsZXQgb3BlcmF0aW9uO1xuICAgICAgICBsZXQgaWQ7XG4gICAgICAgIGlmIChhbm5vdGF0aW9uID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIG9wZXJhdGlvbiA9IENyZWF0ZUZpbGUuY3JlYXRlKHVyaSwgb3B0aW9ucyk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBpZCA9IENoYW5nZUFubm90YXRpb25JZGVudGlmaWVyLmlzKGFubm90YXRpb24pID8gYW5ub3RhdGlvbiA6IHRoaXMuX2NoYW5nZUFubm90YXRpb25zLm1hbmFnZShhbm5vdGF0aW9uKTtcbiAgICAgICAgICAgIG9wZXJhdGlvbiA9IENyZWF0ZUZpbGUuY3JlYXRlKHVyaSwgb3B0aW9ucywgaWQpO1xuICAgICAgICB9XG4gICAgICAgIHRoaXMuX3dvcmtzcGFjZUVkaXQuZG9jdW1lbnRDaGFuZ2VzLnB1c2gob3BlcmF0aW9uKTtcbiAgICAgICAgaWYgKGlkICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHJldHVybiBpZDtcbiAgICAgICAgfVxuICAgIH1cbiAgICByZW5hbWVGaWxlKG9sZFVyaSwgbmV3VXJpLCBvcHRpb25zT3JBbm5vdGF0aW9uLCBvcHRpb25zKSB7XG4gICAgICAgIHRoaXMuaW5pdERvY3VtZW50Q2hhbmdlcygpO1xuICAgICAgICBpZiAodGhpcy5fd29ya3NwYWNlRWRpdC5kb2N1bWVudENoYW5nZXMgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdXb3Jrc3BhY2UgZWRpdCBpcyBub3QgY29uZmlndXJlZCBmb3IgZG9jdW1lbnQgY2hhbmdlcy4nKTtcbiAgICAgICAgfVxuICAgICAgICBsZXQgYW5ub3RhdGlvbjtcbiAgICAgICAgaWYgKENoYW5nZUFubm90YXRpb24uaXMob3B0aW9uc09yQW5ub3RhdGlvbikgfHwgQ2hhbmdlQW5ub3RhdGlvbklkZW50aWZpZXIuaXMob3B0aW9uc09yQW5ub3RhdGlvbikpIHtcbiAgICAgICAgICAgIGFubm90YXRpb24gPSBvcHRpb25zT3JBbm5vdGF0aW9uO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgb3B0aW9ucyA9IG9wdGlvbnNPckFubm90YXRpb247XG4gICAgICAgIH1cbiAgICAgICAgbGV0IG9wZXJhdGlvbjtcbiAgICAgICAgbGV0IGlkO1xuICAgICAgICBpZiAoYW5ub3RhdGlvbiA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICBvcGVyYXRpb24gPSBSZW5hbWVGaWxlLmNyZWF0ZShvbGRVcmksIG5ld1VyaSwgb3B0aW9ucyk7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgICBpZCA9IENoYW5nZUFubm90YXRpb25JZGVudGlmaWVyLmlzKGFubm90YXRpb24pID8gYW5ub3RhdGlvbiA6IHRoaXMuX2NoYW5nZUFubm90YXRpb25zLm1hbmFnZShhbm5vdGF0aW9uKTtcbiAgICAgICAgICAgIG9wZXJhdGlvbiA9IFJlbmFtZUZpbGUuY3JlYXRlKG9sZFVyaSwgbmV3VXJpLCBvcHRpb25zLCBpZCk7XG4gICAgICAgIH1cbiAgICAgICAgdGhpcy5fd29ya3NwYWNlRWRpdC5kb2N1bWVudENoYW5nZXMucHVzaChvcGVyYXRpb24pO1xuICAgICAgICBpZiAoaWQgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmV0dXJuIGlkO1xuICAgICAgICB9XG4gICAgfVxuICAgIGRlbGV0ZUZpbGUodXJpLCBvcHRpb25zT3JBbm5vdGF0aW9uLCBvcHRpb25zKSB7XG4gICAgICAgIHRoaXMuaW5pdERvY3VtZW50Q2hhbmdlcygpO1xuICAgICAgICBpZiAodGhpcy5fd29ya3NwYWNlRWRpdC5kb2N1bWVudENoYW5nZXMgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdXb3Jrc3BhY2UgZWRpdCBpcyBub3QgY29uZmlndXJlZCBmb3IgZG9jdW1lbnQgY2hhbmdlcy4nKTtcbiAgICAgICAgfVxuICAgICAgICBsZXQgYW5ub3RhdGlvbjtcbiAgICAgICAgaWYgKENoYW5nZUFubm90YXRpb24uaXMob3B0aW9uc09yQW5ub3RhdGlvbikgfHwgQ2hhbmdlQW5ub3RhdGlvbklkZW50aWZpZXIuaXMob3B0aW9uc09yQW5ub3RhdGlvbikpIHtcbiAgICAgICAgICAgIGFubm90YXRpb24gPSBvcHRpb25zT3JBbm5vdGF0aW9uO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgb3B0aW9ucyA9IG9wdGlvbnNPckFubm90YXRpb247XG4gICAgICAgIH1cbiAgICAgICAgbGV0IG9wZXJhdGlvbjtcbiAgICAgICAgbGV0IGlkO1xuICAgICAgICBpZiAoYW5ub3RhdGlvbiA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICBvcGVyYXRpb24gPSBEZWxldGVGaWxlLmNyZWF0ZSh1cmksIG9wdGlvbnMpO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgaWQgPSBDaGFuZ2VBbm5vdGF0aW9uSWRlbnRpZmllci5pcyhhbm5vdGF0aW9uKSA/IGFubm90YXRpb24gOiB0aGlzLl9jaGFuZ2VBbm5vdGF0aW9ucy5tYW5hZ2UoYW5ub3RhdGlvbik7XG4gICAgICAgICAgICBvcGVyYXRpb24gPSBEZWxldGVGaWxlLmNyZWF0ZSh1cmksIG9wdGlvbnMsIGlkKTtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLl93b3Jrc3BhY2VFZGl0LmRvY3VtZW50Q2hhbmdlcy5wdXNoKG9wZXJhdGlvbik7XG4gICAgICAgIGlmIChpZCAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXR1cm4gaWQ7XG4gICAgICAgIH1cbiAgICB9XG59XG4vKipcbiAqIFRoZSBUZXh0RG9jdW1lbnRJZGVudGlmaWVyIG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIFRleHREb2N1bWVudElkZW50aWZpZXJ9IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIFRleHREb2N1bWVudElkZW50aWZpZXI7XG4oZnVuY3Rpb24gKFRleHREb2N1bWVudElkZW50aWZpZXIpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IFRleHREb2N1bWVudElkZW50aWZpZXIgbGl0ZXJhbC5cbiAgICAgKiBAcGFyYW0gdXJpIFRoZSBkb2N1bWVudCdzIHVyaS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUodXJpKSB7XG4gICAgICAgIHJldHVybiB7IHVyaSB9O1xuICAgIH1cbiAgICBUZXh0RG9jdW1lbnRJZGVudGlmaWVyLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIFRleHREb2N1bWVudElkZW50aWZpZXJ9IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5kZWZpbmVkKGNhbmRpZGF0ZSkgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS51cmkpO1xuICAgIH1cbiAgICBUZXh0RG9jdW1lbnRJZGVudGlmaWVyLmlzID0gaXM7XG59KShUZXh0RG9jdW1lbnRJZGVudGlmaWVyIHx8IChUZXh0RG9jdW1lbnRJZGVudGlmaWVyID0ge30pKTtcbi8qKlxuICogVGhlIFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXIgbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllcn0gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllcjtcbihmdW5jdGlvbiAoVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllcikge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllciBsaXRlcmFsLlxuICAgICAqIEBwYXJhbSB1cmkgVGhlIGRvY3VtZW50J3MgdXJpLlxuICAgICAqIEBwYXJhbSB2ZXJzaW9uIFRoZSBkb2N1bWVudCdzIHZlcnNpb24uXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKHVyaSwgdmVyc2lvbikge1xuICAgICAgICByZXR1cm4geyB1cmksIHZlcnNpb24gfTtcbiAgICB9XG4gICAgVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllci5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyfSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMuZGVmaW5lZChjYW5kaWRhdGUpICYmIElzLnN0cmluZyhjYW5kaWRhdGUudXJpKSAmJiBJcy5pbnRlZ2VyKGNhbmRpZGF0ZS52ZXJzaW9uKTtcbiAgICB9XG4gICAgVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllci5pcyA9IGlzO1xufSkoVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllciB8fCAoVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllciA9IHt9KSk7XG4vKipcbiAqIFRoZSBPcHRpb25hbFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXIgbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgT3B0aW9uYWxWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyfSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBPcHRpb25hbFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXI7XG4oZnVuY3Rpb24gKE9wdGlvbmFsVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllcikge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgT3B0aW9uYWxWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyIGxpdGVyYWwuXG4gICAgICogQHBhcmFtIHVyaSBUaGUgZG9jdW1lbnQncyB1cmkuXG4gICAgICogQHBhcmFtIHZlcnNpb24gVGhlIGRvY3VtZW50J3MgdmVyc2lvbi5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUodXJpLCB2ZXJzaW9uKSB7XG4gICAgICAgIHJldHVybiB7IHVyaSwgdmVyc2lvbiB9O1xuICAgIH1cbiAgICBPcHRpb25hbFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXIuY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgT3B0aW9uYWxWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyfSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMuZGVmaW5lZChjYW5kaWRhdGUpICYmIElzLnN0cmluZyhjYW5kaWRhdGUudXJpKSAmJiAoY2FuZGlkYXRlLnZlcnNpb24gPT09IG51bGwgfHwgSXMuaW50ZWdlcihjYW5kaWRhdGUudmVyc2lvbikpO1xuICAgIH1cbiAgICBPcHRpb25hbFZlcnNpb25lZFRleHREb2N1bWVudElkZW50aWZpZXIuaXMgPSBpcztcbn0pKE9wdGlvbmFsVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllciB8fCAoT3B0aW9uYWxWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyID0ge30pKTtcbi8qKlxuICogVGhlIFRleHREb2N1bWVudEl0ZW0gbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgVGV4dERvY3VtZW50SXRlbX0gbGl0ZXJhbHMuXG4gKi9cbmV4cG9ydCB2YXIgVGV4dERvY3VtZW50SXRlbTtcbihmdW5jdGlvbiAoVGV4dERvY3VtZW50SXRlbSkge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgVGV4dERvY3VtZW50SXRlbSBsaXRlcmFsLlxuICAgICAqIEBwYXJhbSB1cmkgVGhlIGRvY3VtZW50J3MgdXJpLlxuICAgICAqIEBwYXJhbSBsYW5ndWFnZUlkIFRoZSBkb2N1bWVudCdzIGxhbmd1YWdlIGlkZW50aWZpZXIuXG4gICAgICogQHBhcmFtIHZlcnNpb24gVGhlIGRvY3VtZW50J3MgdmVyc2lvbiBudW1iZXIuXG4gICAgICogQHBhcmFtIHRleHQgVGhlIGRvY3VtZW50J3MgdGV4dC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUodXJpLCBsYW5ndWFnZUlkLCB2ZXJzaW9uLCB0ZXh0KSB7XG4gICAgICAgIHJldHVybiB7IHVyaSwgbGFuZ3VhZ2VJZCwgdmVyc2lvbiwgdGV4dCB9O1xuICAgIH1cbiAgICBUZXh0RG9jdW1lbnRJdGVtLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIFRleHREb2N1bWVudEl0ZW19IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5kZWZpbmVkKGNhbmRpZGF0ZSkgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS51cmkpICYmIElzLnN0cmluZyhjYW5kaWRhdGUubGFuZ3VhZ2VJZCkgJiYgSXMuaW50ZWdlcihjYW5kaWRhdGUudmVyc2lvbikgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS50ZXh0KTtcbiAgICB9XG4gICAgVGV4dERvY3VtZW50SXRlbS5pcyA9IGlzO1xufSkoVGV4dERvY3VtZW50SXRlbSB8fCAoVGV4dERvY3VtZW50SXRlbSA9IHt9KSk7XG4vKipcbiAqIERlc2NyaWJlcyB0aGUgY29udGVudCB0eXBlIHRoYXQgYSBjbGllbnQgc3VwcG9ydHMgaW4gdmFyaW91c1xuICogcmVzdWx0IGxpdGVyYWxzIGxpa2UgYEhvdmVyYCwgYFBhcmFtZXRlckluZm9gIG9yIGBDb21wbGV0aW9uSXRlbWAuXG4gKlxuICogUGxlYXNlIG5vdGUgdGhhdCBgTWFya3VwS2luZHNgIG11c3Qgbm90IHN0YXJ0IHdpdGggYSBgJGAuIFRoaXMga2luZHNcbiAqIGFyZSByZXNlcnZlZCBmb3IgaW50ZXJuYWwgdXNhZ2UuXG4gKi9cbmV4cG9ydCB2YXIgTWFya3VwS2luZDtcbihmdW5jdGlvbiAoTWFya3VwS2luZCkge1xuICAgIC8qKlxuICAgICAqIFBsYWluIHRleHQgaXMgc3VwcG9ydGVkIGFzIGEgY29udGVudCBmb3JtYXRcbiAgICAgKi9cbiAgICBNYXJrdXBLaW5kLlBsYWluVGV4dCA9ICdwbGFpbnRleHQnO1xuICAgIC8qKlxuICAgICAqIE1hcmtkb3duIGlzIHN1cHBvcnRlZCBhcyBhIGNvbnRlbnQgZm9ybWF0XG4gICAgICovXG4gICAgTWFya3VwS2luZC5NYXJrZG93biA9ICdtYXJrZG93bic7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIHZhbHVlIGlzIGEgdmFsdWUgb2YgdGhlIHtAbGluayBNYXJrdXBLaW5kfSB0eXBlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlID09PSBNYXJrdXBLaW5kLlBsYWluVGV4dCB8fCBjYW5kaWRhdGUgPT09IE1hcmt1cEtpbmQuTWFya2Rvd247XG4gICAgfVxuICAgIE1hcmt1cEtpbmQuaXMgPSBpcztcbn0pKE1hcmt1cEtpbmQgfHwgKE1hcmt1cEtpbmQgPSB7fSkpO1xuZXhwb3J0IHZhciBNYXJrdXBDb250ZW50O1xuKGZ1bmN0aW9uIChNYXJrdXBDb250ZW50KSB7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIHZhbHVlIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgTWFya3VwQ29udGVudH0gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMub2JqZWN0TGl0ZXJhbCh2YWx1ZSkgJiYgTWFya3VwS2luZC5pcyhjYW5kaWRhdGUua2luZCkgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS52YWx1ZSk7XG4gICAgfVxuICAgIE1hcmt1cENvbnRlbnQuaXMgPSBpcztcbn0pKE1hcmt1cENvbnRlbnQgfHwgKE1hcmt1cENvbnRlbnQgPSB7fSkpO1xuLyoqXG4gKiBUaGUga2luZCBvZiBhIGNvbXBsZXRpb24gZW50cnkuXG4gKi9cbmV4cG9ydCB2YXIgQ29tcGxldGlvbkl0ZW1LaW5kO1xuKGZ1bmN0aW9uIChDb21wbGV0aW9uSXRlbUtpbmQpIHtcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuVGV4dCA9IDE7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLk1ldGhvZCA9IDI7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLkZ1bmN0aW9uID0gMztcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuQ29uc3RydWN0b3IgPSA0O1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5GaWVsZCA9IDU7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLlZhcmlhYmxlID0gNjtcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuQ2xhc3MgPSA3O1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5JbnRlcmZhY2UgPSA4O1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5Nb2R1bGUgPSA5O1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5Qcm9wZXJ0eSA9IDEwO1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5Vbml0ID0gMTE7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLlZhbHVlID0gMTI7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLkVudW0gPSAxMztcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuS2V5d29yZCA9IDE0O1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5TbmlwcGV0ID0gMTU7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLkNvbG9yID0gMTY7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLkZpbGUgPSAxNztcbiAgICBDb21wbGV0aW9uSXRlbUtpbmQuUmVmZXJlbmNlID0gMTg7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLkZvbGRlciA9IDE5O1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5FbnVtTWVtYmVyID0gMjA7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLkNvbnN0YW50ID0gMjE7XG4gICAgQ29tcGxldGlvbkl0ZW1LaW5kLlN0cnVjdCA9IDIyO1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5FdmVudCA9IDIzO1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5PcGVyYXRvciA9IDI0O1xuICAgIENvbXBsZXRpb25JdGVtS2luZC5UeXBlUGFyYW1ldGVyID0gMjU7XG59KShDb21wbGV0aW9uSXRlbUtpbmQgfHwgKENvbXBsZXRpb25JdGVtS2luZCA9IHt9KSk7XG4vKipcbiAqIERlZmluZXMgd2hldGhlciB0aGUgaW5zZXJ0IHRleHQgaW4gYSBjb21wbGV0aW9uIGl0ZW0gc2hvdWxkIGJlIGludGVycHJldGVkIGFzXG4gKiBwbGFpbiB0ZXh0IG9yIGEgc25pcHBldC5cbiAqL1xuZXhwb3J0IHZhciBJbnNlcnRUZXh0Rm9ybWF0O1xuKGZ1bmN0aW9uIChJbnNlcnRUZXh0Rm9ybWF0KSB7XG4gICAgLyoqXG4gICAgICogVGhlIHByaW1hcnkgdGV4dCB0byBiZSBpbnNlcnRlZCBpcyB0cmVhdGVkIGFzIGEgcGxhaW4gc3RyaW5nLlxuICAgICAqL1xuICAgIEluc2VydFRleHRGb3JtYXQuUGxhaW5UZXh0ID0gMTtcbiAgICAvKipcbiAgICAgKiBUaGUgcHJpbWFyeSB0ZXh0IHRvIGJlIGluc2VydGVkIGlzIHRyZWF0ZWQgYXMgYSBzbmlwcGV0LlxuICAgICAqXG4gICAgICogQSBzbmlwcGV0IGNhbiBkZWZpbmUgdGFiIHN0b3BzIGFuZCBwbGFjZWhvbGRlcnMgd2l0aCBgJDFgLCBgJDJgXG4gICAgICogYW5kIGAkezM6Zm9vfWAuIGAkMGAgZGVmaW5lcyB0aGUgZmluYWwgdGFiIHN0b3AsIGl0IGRlZmF1bHRzIHRvXG4gICAgICogdGhlIGVuZCBvZiB0aGUgc25pcHBldC4gUGxhY2Vob2xkZXJzIHdpdGggZXF1YWwgaWRlbnRpZmllcnMgYXJlIGxpbmtlZCxcbiAgICAgKiB0aGF0IGlzIHR5cGluZyBpbiBvbmUgd2lsbCB1cGRhdGUgb3RoZXJzIHRvby5cbiAgICAgKlxuICAgICAqIFNlZSBhbHNvOiBodHRwczovL21pY3Jvc29mdC5naXRodWIuaW8vbGFuZ3VhZ2Utc2VydmVyLXByb3RvY29sL3NwZWNpZmljYXRpb25zL3NwZWNpZmljYXRpb24tY3VycmVudC8jc25pcHBldF9zeW50YXhcbiAgICAgKi9cbiAgICBJbnNlcnRUZXh0Rm9ybWF0LlNuaXBwZXQgPSAyO1xufSkoSW5zZXJ0VGV4dEZvcm1hdCB8fCAoSW5zZXJ0VGV4dEZvcm1hdCA9IHt9KSk7XG4vKipcbiAqIENvbXBsZXRpb24gaXRlbSB0YWdzIGFyZSBleHRyYSBhbm5vdGF0aW9ucyB0aGF0IHR3ZWFrIHRoZSByZW5kZXJpbmcgb2YgYSBjb21wbGV0aW9uXG4gKiBpdGVtLlxuICpcbiAqIEBzaW5jZSAzLjE1LjBcbiAqL1xuZXhwb3J0IHZhciBDb21wbGV0aW9uSXRlbVRhZztcbihmdW5jdGlvbiAoQ29tcGxldGlvbkl0ZW1UYWcpIHtcbiAgICAvKipcbiAgICAgKiBSZW5kZXIgYSBjb21wbGV0aW9uIGFzIG9ic29sZXRlLCB1c3VhbGx5IHVzaW5nIGEgc3RyaWtlLW91dC5cbiAgICAgKi9cbiAgICBDb21wbGV0aW9uSXRlbVRhZy5EZXByZWNhdGVkID0gMTtcbn0pKENvbXBsZXRpb25JdGVtVGFnIHx8IChDb21wbGV0aW9uSXRlbVRhZyA9IHt9KSk7XG4vKipcbiAqIFRoZSBJbnNlcnRSZXBsYWNlRWRpdCBuYW1lc3BhY2UgcHJvdmlkZXMgZnVuY3Rpb25zIHRvIGRlYWwgd2l0aCBpbnNlcnQgLyByZXBsYWNlIGVkaXRzLlxuICpcbiAqIEBzaW5jZSAzLjE2LjBcbiAqL1xuZXhwb3J0IHZhciBJbnNlcnRSZXBsYWNlRWRpdDtcbihmdW5jdGlvbiAoSW5zZXJ0UmVwbGFjZUVkaXQpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IGluc2VydCAvIHJlcGxhY2UgZWRpdFxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShuZXdUZXh0LCBpbnNlcnQsIHJlcGxhY2UpIHtcbiAgICAgICAgcmV0dXJuIHsgbmV3VGV4dCwgaW5zZXJ0LCByZXBsYWNlIH07XG4gICAgfVxuICAgIEluc2VydFJlcGxhY2VFZGl0LmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gbGl0ZXJhbCBjb25mb3JtcyB0byB0aGUge0BsaW5rIEluc2VydFJlcGxhY2VFZGl0fSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBjYW5kaWRhdGUgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS5uZXdUZXh0KSAmJiBSYW5nZS5pcyhjYW5kaWRhdGUuaW5zZXJ0KSAmJiBSYW5nZS5pcyhjYW5kaWRhdGUucmVwbGFjZSk7XG4gICAgfVxuICAgIEluc2VydFJlcGxhY2VFZGl0LmlzID0gaXM7XG59KShJbnNlcnRSZXBsYWNlRWRpdCB8fCAoSW5zZXJ0UmVwbGFjZUVkaXQgPSB7fSkpO1xuLyoqXG4gKiBIb3cgd2hpdGVzcGFjZSBhbmQgaW5kZW50YXRpb24gaXMgaGFuZGxlZCBkdXJpbmcgY29tcGxldGlvblxuICogaXRlbSBpbnNlcnRpb24uXG4gKlxuICogQHNpbmNlIDMuMTYuMFxuICovXG5leHBvcnQgdmFyIEluc2VydFRleHRNb2RlO1xuKGZ1bmN0aW9uIChJbnNlcnRUZXh0TW9kZSkge1xuICAgIC8qKlxuICAgICAqIFRoZSBpbnNlcnRpb24gb3IgcmVwbGFjZSBzdHJpbmdzIGlzIHRha2VuIGFzIGl0IGlzLiBJZiB0aGVcbiAgICAgKiB2YWx1ZSBpcyBtdWx0aSBsaW5lIHRoZSBsaW5lcyBiZWxvdyB0aGUgY3Vyc29yIHdpbGwgYmVcbiAgICAgKiBpbnNlcnRlZCB1c2luZyB0aGUgaW5kZW50YXRpb24gZGVmaW5lZCBpbiB0aGUgc3RyaW5nIHZhbHVlLlxuICAgICAqIFRoZSBjbGllbnQgd2lsbCBub3QgYXBwbHkgYW55IGtpbmQgb2YgYWRqdXN0bWVudHMgdG8gdGhlXG4gICAgICogc3RyaW5nLlxuICAgICAqL1xuICAgIEluc2VydFRleHRNb2RlLmFzSXMgPSAxO1xuICAgIC8qKlxuICAgICAqIFRoZSBlZGl0b3IgYWRqdXN0cyBsZWFkaW5nIHdoaXRlc3BhY2Ugb2YgbmV3IGxpbmVzIHNvIHRoYXRcbiAgICAgKiB0aGV5IG1hdGNoIHRoZSBpbmRlbnRhdGlvbiB1cCB0byB0aGUgY3Vyc29yIG9mIHRoZSBsaW5lIGZvclxuICAgICAqIHdoaWNoIHRoZSBpdGVtIGlzIGFjY2VwdGVkLlxuICAgICAqXG4gICAgICogQ29uc2lkZXIgYSBsaW5lIGxpa2UgdGhpczogPDJ0YWJzPjxjdXJzb3I+PDN0YWJzPmZvby4gQWNjZXB0aW5nIGFcbiAgICAgKiBtdWx0aSBsaW5lIGNvbXBsZXRpb24gaXRlbSBpcyBpbmRlbnRlZCB1c2luZyAyIHRhYnMgYW5kIGFsbFxuICAgICAqIGZvbGxvd2luZyBsaW5lcyBpbnNlcnRlZCB3aWxsIGJlIGluZGVudGVkIHVzaW5nIDIgdGFicyBhcyB3ZWxsLlxuICAgICAqL1xuICAgIEluc2VydFRleHRNb2RlLmFkanVzdEluZGVudGF0aW9uID0gMjtcbn0pKEluc2VydFRleHRNb2RlIHx8IChJbnNlcnRUZXh0TW9kZSA9IHt9KSk7XG5leHBvcnQgdmFyIENvbXBsZXRpb25JdGVtTGFiZWxEZXRhaWxzO1xuKGZ1bmN0aW9uIChDb21wbGV0aW9uSXRlbUxhYmVsRGV0YWlscykge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICYmIChJcy5zdHJpbmcoY2FuZGlkYXRlLmRldGFpbCkgfHwgY2FuZGlkYXRlLmRldGFpbCA9PT0gdW5kZWZpbmVkKSAmJlxuICAgICAgICAgICAgKElzLnN0cmluZyhjYW5kaWRhdGUuZGVzY3JpcHRpb24pIHx8IGNhbmRpZGF0ZS5kZXNjcmlwdGlvbiA9PT0gdW5kZWZpbmVkKTtcbiAgICB9XG4gICAgQ29tcGxldGlvbkl0ZW1MYWJlbERldGFpbHMuaXMgPSBpcztcbn0pKENvbXBsZXRpb25JdGVtTGFiZWxEZXRhaWxzIHx8IChDb21wbGV0aW9uSXRlbUxhYmVsRGV0YWlscyA9IHt9KSk7XG4vKipcbiAqIFRoZSBDb21wbGV0aW9uSXRlbSBuYW1lc3BhY2UgcHJvdmlkZXMgZnVuY3Rpb25zIHRvIGRlYWwgd2l0aFxuICogY29tcGxldGlvbiBpdGVtcy5cbiAqL1xuZXhwb3J0IHZhciBDb21wbGV0aW9uSXRlbTtcbihmdW5jdGlvbiAoQ29tcGxldGlvbkl0ZW0pIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGUgYSBjb21wbGV0aW9uIGl0ZW0gYW5kIHNlZWQgaXQgd2l0aCBhIGxhYmVsLlxuICAgICAqIEBwYXJhbSBsYWJlbCBUaGUgY29tcGxldGlvbiBpdGVtJ3MgbGFiZWxcbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUobGFiZWwpIHtcbiAgICAgICAgcmV0dXJuIHsgbGFiZWwgfTtcbiAgICB9XG4gICAgQ29tcGxldGlvbkl0ZW0uY3JlYXRlID0gY3JlYXRlO1xufSkoQ29tcGxldGlvbkl0ZW0gfHwgKENvbXBsZXRpb25JdGVtID0ge30pKTtcbi8qKlxuICogVGhlIENvbXBsZXRpb25MaXN0IG5hbWVzcGFjZSBwcm92aWRlcyBmdW5jdGlvbnMgdG8gZGVhbCB3aXRoXG4gKiBjb21wbGV0aW9uIGxpc3RzLlxuICovXG5leHBvcnQgdmFyIENvbXBsZXRpb25MaXN0O1xuKGZ1bmN0aW9uIChDb21wbGV0aW9uTGlzdCkge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgY29tcGxldGlvbiBsaXN0LlxuICAgICAqXG4gICAgICogQHBhcmFtIGl0ZW1zIFRoZSBjb21wbGV0aW9uIGl0ZW1zLlxuICAgICAqIEBwYXJhbSBpc0luY29tcGxldGUgVGhlIGxpc3QgaXMgbm90IGNvbXBsZXRlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShpdGVtcywgaXNJbmNvbXBsZXRlKSB7XG4gICAgICAgIHJldHVybiB7IGl0ZW1zOiBpdGVtcyA/IGl0ZW1zIDogW10sIGlzSW5jb21wbGV0ZTogISFpc0luY29tcGxldGUgfTtcbiAgICB9XG4gICAgQ29tcGxldGlvbkxpc3QuY3JlYXRlID0gY3JlYXRlO1xufSkoQ29tcGxldGlvbkxpc3QgfHwgKENvbXBsZXRpb25MaXN0ID0ge30pKTtcbmV4cG9ydCB2YXIgTWFya2VkU3RyaW5nO1xuKGZ1bmN0aW9uIChNYXJrZWRTdHJpbmcpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbWFya2VkIHN0cmluZyBmcm9tIHBsYWluIHRleHQuXG4gICAgICpcbiAgICAgKiBAcGFyYW0gcGxhaW5UZXh0IFRoZSBwbGFpbiB0ZXh0LlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGZyb21QbGFpblRleHQocGxhaW5UZXh0KSB7XG4gICAgICAgIHJldHVybiBwbGFpblRleHQucmVwbGFjZSgvW1xcXFxgKl97fVtcXF0oKSMrXFwtLiFdL2csICdcXFxcJCYnKTsgLy8gZXNjYXBlIG1hcmtkb3duIHN5bnRheCB0b2tlbnM6IGh0dHA6Ly9kYXJpbmdmaXJlYmFsbC5uZXQvcHJvamVjdHMvbWFya2Rvd24vc3ludGF4I2JhY2tzbGFzaFxuICAgIH1cbiAgICBNYXJrZWRTdHJpbmcuZnJvbVBsYWluVGV4dCA9IGZyb21QbGFpblRleHQ7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIHZhbHVlIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgTWFya2VkU3RyaW5nfSB0eXBlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMuc3RyaW5nKGNhbmRpZGF0ZSkgfHwgKElzLm9iamVjdExpdGVyYWwoY2FuZGlkYXRlKSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLmxhbmd1YWdlKSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLnZhbHVlKSk7XG4gICAgfVxuICAgIE1hcmtlZFN0cmluZy5pcyA9IGlzO1xufSkoTWFya2VkU3RyaW5nIHx8IChNYXJrZWRTdHJpbmcgPSB7fSkpO1xuZXhwb3J0IHZhciBIb3ZlcjtcbihmdW5jdGlvbiAoSG92ZXIpIHtcbiAgICAvKipcbiAgICAgKiBDaGVja3Mgd2hldGhlciB0aGUgZ2l2ZW4gdmFsdWUgY29uZm9ybXMgdG8gdGhlIHtAbGluayBIb3Zlcn0gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuICEhY2FuZGlkYXRlICYmIElzLm9iamVjdExpdGVyYWwoY2FuZGlkYXRlKSAmJiAoTWFya3VwQ29udGVudC5pcyhjYW5kaWRhdGUuY29udGVudHMpIHx8XG4gICAgICAgICAgICBNYXJrZWRTdHJpbmcuaXMoY2FuZGlkYXRlLmNvbnRlbnRzKSB8fFxuICAgICAgICAgICAgSXMudHlwZWRBcnJheShjYW5kaWRhdGUuY29udGVudHMsIE1hcmtlZFN0cmluZy5pcykpICYmICh2YWx1ZS5yYW5nZSA9PT0gdW5kZWZpbmVkIHx8IFJhbmdlLmlzKHZhbHVlLnJhbmdlKSk7XG4gICAgfVxuICAgIEhvdmVyLmlzID0gaXM7XG59KShIb3ZlciB8fCAoSG92ZXIgPSB7fSkpO1xuLyoqXG4gKiBUaGUgUGFyYW1ldGVySW5mb3JtYXRpb24gbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgUGFyYW1ldGVySW5mb3JtYXRpb259IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIFBhcmFtZXRlckluZm9ybWF0aW9uO1xuKGZ1bmN0aW9uIChQYXJhbWV0ZXJJbmZvcm1hdGlvbikge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgcGFyYW1ldGVyIGluZm9ybWF0aW9uIGxpdGVyYWwuXG4gICAgICpcbiAgICAgKiBAcGFyYW0gbGFiZWwgQSBsYWJlbCBzdHJpbmcuXG4gICAgICogQHBhcmFtIGRvY3VtZW50YXRpb24gQSBkb2Mgc3RyaW5nLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShsYWJlbCwgZG9jdW1lbnRhdGlvbikge1xuICAgICAgICByZXR1cm4gZG9jdW1lbnRhdGlvbiA/IHsgbGFiZWwsIGRvY3VtZW50YXRpb24gfSA6IHsgbGFiZWwgfTtcbiAgICB9XG4gICAgUGFyYW1ldGVySW5mb3JtYXRpb24uY3JlYXRlID0gY3JlYXRlO1xufSkoUGFyYW1ldGVySW5mb3JtYXRpb24gfHwgKFBhcmFtZXRlckluZm9ybWF0aW9uID0ge30pKTtcbi8qKlxuICogVGhlIFNpZ25hdHVyZUluZm9ybWF0aW9uIG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIFNpZ25hdHVyZUluZm9ybWF0aW9ufSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBTaWduYXR1cmVJbmZvcm1hdGlvbjtcbihmdW5jdGlvbiAoU2lnbmF0dXJlSW5mb3JtYXRpb24pIHtcbiAgICBmdW5jdGlvbiBjcmVhdGUobGFiZWwsIGRvY3VtZW50YXRpb24sIC4uLnBhcmFtZXRlcnMpIHtcbiAgICAgICAgbGV0IHJlc3VsdCA9IHsgbGFiZWwgfTtcbiAgICAgICAgaWYgKElzLmRlZmluZWQoZG9jdW1lbnRhdGlvbikpIHtcbiAgICAgICAgICAgIHJlc3VsdC5kb2N1bWVudGF0aW9uID0gZG9jdW1lbnRhdGlvbjtcbiAgICAgICAgfVxuICAgICAgICBpZiAoSXMuZGVmaW5lZChwYXJhbWV0ZXJzKSkge1xuICAgICAgICAgICAgcmVzdWx0LnBhcmFtZXRlcnMgPSBwYXJhbWV0ZXJzO1xuICAgICAgICB9XG4gICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgcmVzdWx0LnBhcmFtZXRlcnMgPSBbXTtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cbiAgICBTaWduYXR1cmVJbmZvcm1hdGlvbi5jcmVhdGUgPSBjcmVhdGU7XG59KShTaWduYXR1cmVJbmZvcm1hdGlvbiB8fCAoU2lnbmF0dXJlSW5mb3JtYXRpb24gPSB7fSkpO1xuLyoqXG4gKiBBIGRvY3VtZW50IGhpZ2hsaWdodCBraW5kLlxuICovXG5leHBvcnQgdmFyIERvY3VtZW50SGlnaGxpZ2h0S2luZDtcbihmdW5jdGlvbiAoRG9jdW1lbnRIaWdobGlnaHRLaW5kKSB7XG4gICAgLyoqXG4gICAgICogQSB0ZXh0dWFsIG9jY3VycmVuY2UuXG4gICAgICovXG4gICAgRG9jdW1lbnRIaWdobGlnaHRLaW5kLlRleHQgPSAxO1xuICAgIC8qKlxuICAgICAqIFJlYWQtYWNjZXNzIG9mIGEgc3ltYm9sLCBsaWtlIHJlYWRpbmcgYSB2YXJpYWJsZS5cbiAgICAgKi9cbiAgICBEb2N1bWVudEhpZ2hsaWdodEtpbmQuUmVhZCA9IDI7XG4gICAgLyoqXG4gICAgICogV3JpdGUtYWNjZXNzIG9mIGEgc3ltYm9sLCBsaWtlIHdyaXRpbmcgdG8gYSB2YXJpYWJsZS5cbiAgICAgKi9cbiAgICBEb2N1bWVudEhpZ2hsaWdodEtpbmQuV3JpdGUgPSAzO1xufSkoRG9jdW1lbnRIaWdobGlnaHRLaW5kIHx8IChEb2N1bWVudEhpZ2hsaWdodEtpbmQgPSB7fSkpO1xuLyoqXG4gKiBEb2N1bWVudEhpZ2hsaWdodCBuYW1lc3BhY2UgdG8gcHJvdmlkZSBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIERvY3VtZW50SGlnaGxpZ2h0fSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBEb2N1bWVudEhpZ2hsaWdodDtcbihmdW5jdGlvbiAoRG9jdW1lbnRIaWdobGlnaHQpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGUgYSBEb2N1bWVudEhpZ2hsaWdodCBvYmplY3QuXG4gICAgICogQHBhcmFtIHJhbmdlIFRoZSByYW5nZSB0aGUgaGlnaGxpZ2h0IGFwcGxpZXMgdG8uXG4gICAgICogQHBhcmFtIGtpbmQgVGhlIGhpZ2hsaWdodCBraW5kXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKHJhbmdlLCBraW5kKSB7XG4gICAgICAgIGxldCByZXN1bHQgPSB7IHJhbmdlIH07XG4gICAgICAgIGlmIChJcy5udW1iZXIoa2luZCkpIHtcbiAgICAgICAgICAgIHJlc3VsdC5raW5kID0ga2luZDtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cbiAgICBEb2N1bWVudEhpZ2hsaWdodC5jcmVhdGUgPSBjcmVhdGU7XG59KShEb2N1bWVudEhpZ2hsaWdodCB8fCAoRG9jdW1lbnRIaWdobGlnaHQgPSB7fSkpO1xuLyoqXG4gKiBBIHN5bWJvbCBraW5kLlxuICovXG5leHBvcnQgdmFyIFN5bWJvbEtpbmQ7XG4oZnVuY3Rpb24gKFN5bWJvbEtpbmQpIHtcbiAgICBTeW1ib2xLaW5kLkZpbGUgPSAxO1xuICAgIFN5bWJvbEtpbmQuTW9kdWxlID0gMjtcbiAgICBTeW1ib2xLaW5kLk5hbWVzcGFjZSA9IDM7XG4gICAgU3ltYm9sS2luZC5QYWNrYWdlID0gNDtcbiAgICBTeW1ib2xLaW5kLkNsYXNzID0gNTtcbiAgICBTeW1ib2xLaW5kLk1ldGhvZCA9IDY7XG4gICAgU3ltYm9sS2luZC5Qcm9wZXJ0eSA9IDc7XG4gICAgU3ltYm9sS2luZC5GaWVsZCA9IDg7XG4gICAgU3ltYm9sS2luZC5Db25zdHJ1Y3RvciA9IDk7XG4gICAgU3ltYm9sS2luZC5FbnVtID0gMTA7XG4gICAgU3ltYm9sS2luZC5JbnRlcmZhY2UgPSAxMTtcbiAgICBTeW1ib2xLaW5kLkZ1bmN0aW9uID0gMTI7XG4gICAgU3ltYm9sS2luZC5WYXJpYWJsZSA9IDEzO1xuICAgIFN5bWJvbEtpbmQuQ29uc3RhbnQgPSAxNDtcbiAgICBTeW1ib2xLaW5kLlN0cmluZyA9IDE1O1xuICAgIFN5bWJvbEtpbmQuTnVtYmVyID0gMTY7XG4gICAgU3ltYm9sS2luZC5Cb29sZWFuID0gMTc7XG4gICAgU3ltYm9sS2luZC5BcnJheSA9IDE4O1xuICAgIFN5bWJvbEtpbmQuT2JqZWN0ID0gMTk7XG4gICAgU3ltYm9sS2luZC5LZXkgPSAyMDtcbiAgICBTeW1ib2xLaW5kLk51bGwgPSAyMTtcbiAgICBTeW1ib2xLaW5kLkVudW1NZW1iZXIgPSAyMjtcbiAgICBTeW1ib2xLaW5kLlN0cnVjdCA9IDIzO1xuICAgIFN5bWJvbEtpbmQuRXZlbnQgPSAyNDtcbiAgICBTeW1ib2xLaW5kLk9wZXJhdG9yID0gMjU7XG4gICAgU3ltYm9sS2luZC5UeXBlUGFyYW1ldGVyID0gMjY7XG59KShTeW1ib2xLaW5kIHx8IChTeW1ib2xLaW5kID0ge30pKTtcbi8qKlxuICogU3ltYm9sIHRhZ3MgYXJlIGV4dHJhIGFubm90YXRpb25zIHRoYXQgdHdlYWsgdGhlIHJlbmRlcmluZyBvZiBhIHN5bWJvbC5cbiAqXG4gKiBAc2luY2UgMy4xNlxuICovXG5leHBvcnQgdmFyIFN5bWJvbFRhZztcbihmdW5jdGlvbiAoU3ltYm9sVGFnKSB7XG4gICAgLyoqXG4gICAgICogUmVuZGVyIGEgc3ltYm9sIGFzIG9ic29sZXRlLCB1c3VhbGx5IHVzaW5nIGEgc3RyaWtlLW91dC5cbiAgICAgKi9cbiAgICBTeW1ib2xUYWcuRGVwcmVjYXRlZCA9IDE7XG59KShTeW1ib2xUYWcgfHwgKFN5bWJvbFRhZyA9IHt9KSk7XG5leHBvcnQgdmFyIFN5bWJvbEluZm9ybWF0aW9uO1xuKGZ1bmN0aW9uIChTeW1ib2xJbmZvcm1hdGlvbikge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgc3ltYm9sIGluZm9ybWF0aW9uIGxpdGVyYWwuXG4gICAgICpcbiAgICAgKiBAcGFyYW0gbmFtZSBUaGUgbmFtZSBvZiB0aGUgc3ltYm9sLlxuICAgICAqIEBwYXJhbSBraW5kIFRoZSBraW5kIG9mIHRoZSBzeW1ib2wuXG4gICAgICogQHBhcmFtIHJhbmdlIFRoZSByYW5nZSBvZiB0aGUgbG9jYXRpb24gb2YgdGhlIHN5bWJvbC5cbiAgICAgKiBAcGFyYW0gdXJpIFRoZSByZXNvdXJjZSBvZiB0aGUgbG9jYXRpb24gb2Ygc3ltYm9sLlxuICAgICAqIEBwYXJhbSBjb250YWluZXJOYW1lIFRoZSBuYW1lIG9mIHRoZSBzeW1ib2wgY29udGFpbmluZyB0aGUgc3ltYm9sLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShuYW1lLCBraW5kLCByYW5nZSwgdXJpLCBjb250YWluZXJOYW1lKSB7XG4gICAgICAgIGxldCByZXN1bHQgPSB7XG4gICAgICAgICAgICBuYW1lLFxuICAgICAgICAgICAga2luZCxcbiAgICAgICAgICAgIGxvY2F0aW9uOiB7IHVyaSwgcmFuZ2UgfVxuICAgICAgICB9O1xuICAgICAgICBpZiAoY29udGFpbmVyTmFtZSkge1xuICAgICAgICAgICAgcmVzdWx0LmNvbnRhaW5lck5hbWUgPSBjb250YWluZXJOYW1lO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxuICAgIFN5bWJvbEluZm9ybWF0aW9uLmNyZWF0ZSA9IGNyZWF0ZTtcbn0pKFN5bWJvbEluZm9ybWF0aW9uIHx8IChTeW1ib2xJbmZvcm1hdGlvbiA9IHt9KSk7XG5leHBvcnQgdmFyIFdvcmtzcGFjZVN5bWJvbDtcbihmdW5jdGlvbiAoV29ya3NwYWNlU3ltYm9sKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlIGEgbmV3IHdvcmtzcGFjZSBzeW1ib2wuXG4gICAgICpcbiAgICAgKiBAcGFyYW0gbmFtZSBUaGUgbmFtZSBvZiB0aGUgc3ltYm9sLlxuICAgICAqIEBwYXJhbSBraW5kIFRoZSBraW5kIG9mIHRoZSBzeW1ib2wuXG4gICAgICogQHBhcmFtIHVyaSBUaGUgcmVzb3VyY2Ugb2YgdGhlIGxvY2F0aW9uIG9mIHRoZSBzeW1ib2wuXG4gICAgICogQHBhcmFtIHJhbmdlIEFuIG9wdGlvbnMgcmFuZ2Ugb2YgdGhlIGxvY2F0aW9uLlxuICAgICAqIEByZXR1cm5zIEEgV29ya3NwYWNlU3ltYm9sLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShuYW1lLCBraW5kLCB1cmksIHJhbmdlKSB7XG4gICAgICAgIHJldHVybiByYW5nZSAhPT0gdW5kZWZpbmVkXG4gICAgICAgICAgICA/IHsgbmFtZSwga2luZCwgbG9jYXRpb246IHsgdXJpLCByYW5nZSB9IH1cbiAgICAgICAgICAgIDogeyBuYW1lLCBraW5kLCBsb2NhdGlvbjogeyB1cmkgfSB9O1xuICAgIH1cbiAgICBXb3Jrc3BhY2VTeW1ib2wuY3JlYXRlID0gY3JlYXRlO1xufSkoV29ya3NwYWNlU3ltYm9sIHx8IChXb3Jrc3BhY2VTeW1ib2wgPSB7fSkpO1xuZXhwb3J0IHZhciBEb2N1bWVudFN5bWJvbDtcbihmdW5jdGlvbiAoRG9jdW1lbnRTeW1ib2wpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IHN5bWJvbCBpbmZvcm1hdGlvbiBsaXRlcmFsLlxuICAgICAqXG4gICAgICogQHBhcmFtIG5hbWUgVGhlIG5hbWUgb2YgdGhlIHN5bWJvbC5cbiAgICAgKiBAcGFyYW0gZGV0YWlsIFRoZSBkZXRhaWwgb2YgdGhlIHN5bWJvbC5cbiAgICAgKiBAcGFyYW0ga2luZCBUaGUga2luZCBvZiB0aGUgc3ltYm9sLlxuICAgICAqIEBwYXJhbSByYW5nZSBUaGUgcmFuZ2Ugb2YgdGhlIHN5bWJvbC5cbiAgICAgKiBAcGFyYW0gc2VsZWN0aW9uUmFuZ2UgVGhlIHNlbGVjdGlvblJhbmdlIG9mIHRoZSBzeW1ib2wuXG4gICAgICogQHBhcmFtIGNoaWxkcmVuIENoaWxkcmVuIG9mIHRoZSBzeW1ib2wuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKG5hbWUsIGRldGFpbCwga2luZCwgcmFuZ2UsIHNlbGVjdGlvblJhbmdlLCBjaGlsZHJlbikge1xuICAgICAgICBsZXQgcmVzdWx0ID0ge1xuICAgICAgICAgICAgbmFtZSxcbiAgICAgICAgICAgIGRldGFpbCxcbiAgICAgICAgICAgIGtpbmQsXG4gICAgICAgICAgICByYW5nZSxcbiAgICAgICAgICAgIHNlbGVjdGlvblJhbmdlXG4gICAgICAgIH07XG4gICAgICAgIGlmIChjaGlsZHJlbiAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgICByZXN1bHQuY2hpbGRyZW4gPSBjaGlsZHJlbjtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cbiAgICBEb2N1bWVudFN5bWJvbC5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBEb2N1bWVudFN5bWJvbH0gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJlxuICAgICAgICAgICAgSXMuc3RyaW5nKGNhbmRpZGF0ZS5uYW1lKSAmJiBJcy5udW1iZXIoY2FuZGlkYXRlLmtpbmQpICYmXG4gICAgICAgICAgICBSYW5nZS5pcyhjYW5kaWRhdGUucmFuZ2UpICYmIFJhbmdlLmlzKGNhbmRpZGF0ZS5zZWxlY3Rpb25SYW5nZSkgJiZcbiAgICAgICAgICAgIChjYW5kaWRhdGUuZGV0YWlsID09PSB1bmRlZmluZWQgfHwgSXMuc3RyaW5nKGNhbmRpZGF0ZS5kZXRhaWwpKSAmJlxuICAgICAgICAgICAgKGNhbmRpZGF0ZS5kZXByZWNhdGVkID09PSB1bmRlZmluZWQgfHwgSXMuYm9vbGVhbihjYW5kaWRhdGUuZGVwcmVjYXRlZCkpICYmXG4gICAgICAgICAgICAoY2FuZGlkYXRlLmNoaWxkcmVuID09PSB1bmRlZmluZWQgfHwgQXJyYXkuaXNBcnJheShjYW5kaWRhdGUuY2hpbGRyZW4pKSAmJlxuICAgICAgICAgICAgKGNhbmRpZGF0ZS50YWdzID09PSB1bmRlZmluZWQgfHwgQXJyYXkuaXNBcnJheShjYW5kaWRhdGUudGFncykpO1xuICAgIH1cbiAgICBEb2N1bWVudFN5bWJvbC5pcyA9IGlzO1xufSkoRG9jdW1lbnRTeW1ib2wgfHwgKERvY3VtZW50U3ltYm9sID0ge30pKTtcbi8qKlxuICogQSBzZXQgb2YgcHJlZGVmaW5lZCBjb2RlIGFjdGlvbiBraW5kc1xuICovXG5leHBvcnQgdmFyIENvZGVBY3Rpb25LaW5kO1xuKGZ1bmN0aW9uIChDb2RlQWN0aW9uS2luZCkge1xuICAgIC8qKlxuICAgICAqIEVtcHR5IGtpbmQuXG4gICAgICovXG4gICAgQ29kZUFjdGlvbktpbmQuRW1wdHkgPSAnJztcbiAgICAvKipcbiAgICAgKiBCYXNlIGtpbmQgZm9yIHF1aWNrZml4IGFjdGlvbnM6ICdxdWlja2ZpeCdcbiAgICAgKi9cbiAgICBDb2RlQWN0aW9uS2luZC5RdWlja0ZpeCA9ICdxdWlja2ZpeCc7XG4gICAgLyoqXG4gICAgICogQmFzZSBraW5kIGZvciByZWZhY3RvcmluZyBhY3Rpb25zOiAncmVmYWN0b3InXG4gICAgICovXG4gICAgQ29kZUFjdGlvbktpbmQuUmVmYWN0b3IgPSAncmVmYWN0b3InO1xuICAgIC8qKlxuICAgICAqIEJhc2Uga2luZCBmb3IgcmVmYWN0b3JpbmcgZXh0cmFjdGlvbiBhY3Rpb25zOiAncmVmYWN0b3IuZXh0cmFjdCdcbiAgICAgKlxuICAgICAqIEV4YW1wbGUgZXh0cmFjdCBhY3Rpb25zOlxuICAgICAqXG4gICAgICogLSBFeHRyYWN0IG1ldGhvZFxuICAgICAqIC0gRXh0cmFjdCBmdW5jdGlvblxuICAgICAqIC0gRXh0cmFjdCB2YXJpYWJsZVxuICAgICAqIC0gRXh0cmFjdCBpbnRlcmZhY2UgZnJvbSBjbGFzc1xuICAgICAqIC0gLi4uXG4gICAgICovXG4gICAgQ29kZUFjdGlvbktpbmQuUmVmYWN0b3JFeHRyYWN0ID0gJ3JlZmFjdG9yLmV4dHJhY3QnO1xuICAgIC8qKlxuICAgICAqIEJhc2Uga2luZCBmb3IgcmVmYWN0b3JpbmcgaW5saW5lIGFjdGlvbnM6ICdyZWZhY3Rvci5pbmxpbmUnXG4gICAgICpcbiAgICAgKiBFeGFtcGxlIGlubGluZSBhY3Rpb25zOlxuICAgICAqXG4gICAgICogLSBJbmxpbmUgZnVuY3Rpb25cbiAgICAgKiAtIElubGluZSB2YXJpYWJsZVxuICAgICAqIC0gSW5saW5lIGNvbnN0YW50XG4gICAgICogLSAuLi5cbiAgICAgKi9cbiAgICBDb2RlQWN0aW9uS2luZC5SZWZhY3RvcklubGluZSA9ICdyZWZhY3Rvci5pbmxpbmUnO1xuICAgIC8qKlxuICAgICAqIEJhc2Uga2luZCBmb3IgcmVmYWN0b3JpbmcgcmV3cml0ZSBhY3Rpb25zOiAncmVmYWN0b3IucmV3cml0ZSdcbiAgICAgKlxuICAgICAqIEV4YW1wbGUgcmV3cml0ZSBhY3Rpb25zOlxuICAgICAqXG4gICAgICogLSBDb252ZXJ0IEphdmFTY3JpcHQgZnVuY3Rpb24gdG8gY2xhc3NcbiAgICAgKiAtIEFkZCBvciByZW1vdmUgcGFyYW1ldGVyXG4gICAgICogLSBFbmNhcHN1bGF0ZSBmaWVsZFxuICAgICAqIC0gTWFrZSBtZXRob2Qgc3RhdGljXG4gICAgICogLSBNb3ZlIG1ldGhvZCB0byBiYXNlIGNsYXNzXG4gICAgICogLSAuLi5cbiAgICAgKi9cbiAgICBDb2RlQWN0aW9uS2luZC5SZWZhY3RvclJld3JpdGUgPSAncmVmYWN0b3IucmV3cml0ZSc7XG4gICAgLyoqXG4gICAgICogQmFzZSBraW5kIGZvciBzb3VyY2UgYWN0aW9uczogYHNvdXJjZWBcbiAgICAgKlxuICAgICAqIFNvdXJjZSBjb2RlIGFjdGlvbnMgYXBwbHkgdG8gdGhlIGVudGlyZSBmaWxlLlxuICAgICAqL1xuICAgIENvZGVBY3Rpb25LaW5kLlNvdXJjZSA9ICdzb3VyY2UnO1xuICAgIC8qKlxuICAgICAqIEJhc2Uga2luZCBmb3IgYW4gb3JnYW5pemUgaW1wb3J0cyBzb3VyY2UgYWN0aW9uOiBgc291cmNlLm9yZ2FuaXplSW1wb3J0c2BcbiAgICAgKi9cbiAgICBDb2RlQWN0aW9uS2luZC5Tb3VyY2VPcmdhbml6ZUltcG9ydHMgPSAnc291cmNlLm9yZ2FuaXplSW1wb3J0cyc7XG4gICAgLyoqXG4gICAgICogQmFzZSBraW5kIGZvciBhdXRvLWZpeCBzb3VyY2UgYWN0aW9uczogYHNvdXJjZS5maXhBbGxgLlxuICAgICAqXG4gICAgICogRml4IGFsbCBhY3Rpb25zIGF1dG9tYXRpY2FsbHkgZml4IGVycm9ycyB0aGF0IGhhdmUgYSBjbGVhciBmaXggdGhhdCBkbyBub3QgcmVxdWlyZSB1c2VyIGlucHV0LlxuICAgICAqIFRoZXkgc2hvdWxkIG5vdCBzdXBwcmVzcyBlcnJvcnMgb3IgcGVyZm9ybSB1bnNhZmUgZml4ZXMgc3VjaCBhcyBnZW5lcmF0aW5nIG5ldyB0eXBlcyBvciBjbGFzc2VzLlxuICAgICAqXG4gICAgICogQHNpbmNlIDMuMTUuMFxuICAgICAqL1xuICAgIENvZGVBY3Rpb25LaW5kLlNvdXJjZUZpeEFsbCA9ICdzb3VyY2UuZml4QWxsJztcbn0pKENvZGVBY3Rpb25LaW5kIHx8IChDb2RlQWN0aW9uS2luZCA9IHt9KSk7XG4vKipcbiAqIFRoZSByZWFzb24gd2h5IGNvZGUgYWN0aW9ucyB3ZXJlIHJlcXVlc3RlZC5cbiAqXG4gKiBAc2luY2UgMy4xNy4wXG4gKi9cbmV4cG9ydCB2YXIgQ29kZUFjdGlvblRyaWdnZXJLaW5kO1xuKGZ1bmN0aW9uIChDb2RlQWN0aW9uVHJpZ2dlcktpbmQpIHtcbiAgICAvKipcbiAgICAgKiBDb2RlIGFjdGlvbnMgd2VyZSBleHBsaWNpdGx5IHJlcXVlc3RlZCBieSB0aGUgdXNlciBvciBieSBhbiBleHRlbnNpb24uXG4gICAgICovXG4gICAgQ29kZUFjdGlvblRyaWdnZXJLaW5kLkludm9rZWQgPSAxO1xuICAgIC8qKlxuICAgICAqIENvZGUgYWN0aW9ucyB3ZXJlIHJlcXVlc3RlZCBhdXRvbWF0aWNhbGx5LlxuICAgICAqXG4gICAgICogVGhpcyB0eXBpY2FsbHkgaGFwcGVucyB3aGVuIGN1cnJlbnQgc2VsZWN0aW9uIGluIGEgZmlsZSBjaGFuZ2VzLCBidXQgY2FuXG4gICAgICogYWxzbyBiZSB0cmlnZ2VyZWQgd2hlbiBmaWxlIGNvbnRlbnQgY2hhbmdlcy5cbiAgICAgKi9cbiAgICBDb2RlQWN0aW9uVHJpZ2dlcktpbmQuQXV0b21hdGljID0gMjtcbn0pKENvZGVBY3Rpb25UcmlnZ2VyS2luZCB8fCAoQ29kZUFjdGlvblRyaWdnZXJLaW5kID0ge30pKTtcbi8qKlxuICogVGhlIENvZGVBY3Rpb25Db250ZXh0IG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIENvZGVBY3Rpb25Db250ZXh0fSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBDb2RlQWN0aW9uQ29udGV4dDtcbihmdW5jdGlvbiAoQ29kZUFjdGlvbkNvbnRleHQpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IENvZGVBY3Rpb25Db250ZXh0IGxpdGVyYWwuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKGRpYWdub3N0aWNzLCBvbmx5LCB0cmlnZ2VyS2luZCkge1xuICAgICAgICBsZXQgcmVzdWx0ID0geyBkaWFnbm9zdGljcyB9O1xuICAgICAgICBpZiAob25seSAhPT0gdW5kZWZpbmVkICYmIG9ubHkgIT09IG51bGwpIHtcbiAgICAgICAgICAgIHJlc3VsdC5vbmx5ID0gb25seTtcbiAgICAgICAgfVxuICAgICAgICBpZiAodHJpZ2dlcktpbmQgIT09IHVuZGVmaW5lZCAmJiB0cmlnZ2VyS2luZCAhPT0gbnVsbCkge1xuICAgICAgICAgICAgcmVzdWx0LnRyaWdnZXJLaW5kID0gdHJpZ2dlcktpbmQ7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICB9XG4gICAgQ29kZUFjdGlvbkNvbnRleHQuY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgQ29kZUFjdGlvbkNvbnRleHR9IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5kZWZpbmVkKGNhbmRpZGF0ZSkgJiYgSXMudHlwZWRBcnJheShjYW5kaWRhdGUuZGlhZ25vc3RpY3MsIERpYWdub3N0aWMuaXMpXG4gICAgICAgICAgICAmJiAoY2FuZGlkYXRlLm9ubHkgPT09IHVuZGVmaW5lZCB8fCBJcy50eXBlZEFycmF5KGNhbmRpZGF0ZS5vbmx5LCBJcy5zdHJpbmcpKVxuICAgICAgICAgICAgJiYgKGNhbmRpZGF0ZS50cmlnZ2VyS2luZCA9PT0gdW5kZWZpbmVkIHx8IGNhbmRpZGF0ZS50cmlnZ2VyS2luZCA9PT0gQ29kZUFjdGlvblRyaWdnZXJLaW5kLkludm9rZWQgfHwgY2FuZGlkYXRlLnRyaWdnZXJLaW5kID09PSBDb2RlQWN0aW9uVHJpZ2dlcktpbmQuQXV0b21hdGljKTtcbiAgICB9XG4gICAgQ29kZUFjdGlvbkNvbnRleHQuaXMgPSBpcztcbn0pKENvZGVBY3Rpb25Db250ZXh0IHx8IChDb2RlQWN0aW9uQ29udGV4dCA9IHt9KSk7XG5leHBvcnQgdmFyIENvZGVBY3Rpb247XG4oZnVuY3Rpb24gKENvZGVBY3Rpb24pIHtcbiAgICBmdW5jdGlvbiBjcmVhdGUodGl0bGUsIGtpbmRPckNvbW1hbmRPckVkaXQsIGtpbmQpIHtcbiAgICAgICAgbGV0IHJlc3VsdCA9IHsgdGl0bGUgfTtcbiAgICAgICAgbGV0IGNoZWNrS2luZCA9IHRydWU7XG4gICAgICAgIGlmICh0eXBlb2Yga2luZE9yQ29tbWFuZE9yRWRpdCA9PT0gJ3N0cmluZycpIHtcbiAgICAgICAgICAgIGNoZWNrS2luZCA9IGZhbHNlO1xuICAgICAgICAgICAgcmVzdWx0LmtpbmQgPSBraW5kT3JDb21tYW5kT3JFZGl0O1xuICAgICAgICB9XG4gICAgICAgIGVsc2UgaWYgKENvbW1hbmQuaXMoa2luZE9yQ29tbWFuZE9yRWRpdCkpIHtcbiAgICAgICAgICAgIHJlc3VsdC5jb21tYW5kID0ga2luZE9yQ29tbWFuZE9yRWRpdDtcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgIHJlc3VsdC5lZGl0ID0ga2luZE9yQ29tbWFuZE9yRWRpdDtcbiAgICAgICAgfVxuICAgICAgICBpZiAoY2hlY2tLaW5kICYmIGtpbmQgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgICAgcmVzdWx0LmtpbmQgPSBraW5kO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxuICAgIENvZGVBY3Rpb24uY3JlYXRlID0gY3JlYXRlO1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIGNhbmRpZGF0ZSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLnRpdGxlKSAmJlxuICAgICAgICAgICAgKGNhbmRpZGF0ZS5kaWFnbm9zdGljcyA9PT0gdW5kZWZpbmVkIHx8IElzLnR5cGVkQXJyYXkoY2FuZGlkYXRlLmRpYWdub3N0aWNzLCBEaWFnbm9zdGljLmlzKSkgJiZcbiAgICAgICAgICAgIChjYW5kaWRhdGUua2luZCA9PT0gdW5kZWZpbmVkIHx8IElzLnN0cmluZyhjYW5kaWRhdGUua2luZCkpICYmXG4gICAgICAgICAgICAoY2FuZGlkYXRlLmVkaXQgIT09IHVuZGVmaW5lZCB8fCBjYW5kaWRhdGUuY29tbWFuZCAhPT0gdW5kZWZpbmVkKSAmJlxuICAgICAgICAgICAgKGNhbmRpZGF0ZS5jb21tYW5kID09PSB1bmRlZmluZWQgfHwgQ29tbWFuZC5pcyhjYW5kaWRhdGUuY29tbWFuZCkpICYmXG4gICAgICAgICAgICAoY2FuZGlkYXRlLmlzUHJlZmVycmVkID09PSB1bmRlZmluZWQgfHwgSXMuYm9vbGVhbihjYW5kaWRhdGUuaXNQcmVmZXJyZWQpKSAmJlxuICAgICAgICAgICAgKGNhbmRpZGF0ZS5lZGl0ID09PSB1bmRlZmluZWQgfHwgV29ya3NwYWNlRWRpdC5pcyhjYW5kaWRhdGUuZWRpdCkpO1xuICAgIH1cbiAgICBDb2RlQWN0aW9uLmlzID0gaXM7XG59KShDb2RlQWN0aW9uIHx8IChDb2RlQWN0aW9uID0ge30pKTtcbi8qKlxuICogVGhlIENvZGVMZW5zIG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIENvZGVMZW5zfSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBDb2RlTGVucztcbihmdW5jdGlvbiAoQ29kZUxlbnMpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IENvZGVMZW5zIGxpdGVyYWwuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKHJhbmdlLCBkYXRhKSB7XG4gICAgICAgIGxldCByZXN1bHQgPSB7IHJhbmdlIH07XG4gICAgICAgIGlmIChJcy5kZWZpbmVkKGRhdGEpKSB7XG4gICAgICAgICAgICByZXN1bHQuZGF0YSA9IGRhdGE7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICB9XG4gICAgQ29kZUxlbnMuY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgQ29kZUxlbnN9IGludGVyZmFjZS5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5kZWZpbmVkKGNhbmRpZGF0ZSkgJiYgUmFuZ2UuaXMoY2FuZGlkYXRlLnJhbmdlKSAmJiAoSXMudW5kZWZpbmVkKGNhbmRpZGF0ZS5jb21tYW5kKSB8fCBDb21tYW5kLmlzKGNhbmRpZGF0ZS5jb21tYW5kKSk7XG4gICAgfVxuICAgIENvZGVMZW5zLmlzID0gaXM7XG59KShDb2RlTGVucyB8fCAoQ29kZUxlbnMgPSB7fSkpO1xuLyoqXG4gKiBUaGUgRm9ybWF0dGluZ09wdGlvbnMgbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgRm9ybWF0dGluZ09wdGlvbnN9IGxpdGVyYWxzLlxuICovXG5leHBvcnQgdmFyIEZvcm1hdHRpbmdPcHRpb25zO1xuKGZ1bmN0aW9uIChGb3JtYXR0aW5nT3B0aW9ucykge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgRm9ybWF0dGluZ09wdGlvbnMgbGl0ZXJhbC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUodGFiU2l6ZSwgaW5zZXJ0U3BhY2VzKSB7XG4gICAgICAgIHJldHVybiB7IHRhYlNpemUsIGluc2VydFNwYWNlcyB9O1xuICAgIH1cbiAgICBGb3JtYXR0aW5nT3B0aW9ucy5jcmVhdGUgPSBjcmVhdGU7XG4gICAgLyoqXG4gICAgICogQ2hlY2tzIHdoZXRoZXIgdGhlIGdpdmVuIGxpdGVyYWwgY29uZm9ybXMgdG8gdGhlIHtAbGluayBGb3JtYXR0aW5nT3B0aW9uc30gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLmRlZmluZWQoY2FuZGlkYXRlKSAmJiBJcy51aW50ZWdlcihjYW5kaWRhdGUudGFiU2l6ZSkgJiYgSXMuYm9vbGVhbihjYW5kaWRhdGUuaW5zZXJ0U3BhY2VzKTtcbiAgICB9XG4gICAgRm9ybWF0dGluZ09wdGlvbnMuaXMgPSBpcztcbn0pKEZvcm1hdHRpbmdPcHRpb25zIHx8IChGb3JtYXR0aW5nT3B0aW9ucyA9IHt9KSk7XG4vKipcbiAqIFRoZSBEb2N1bWVudExpbmsgbmFtZXNwYWNlIHByb3ZpZGVzIGhlbHBlciBmdW5jdGlvbnMgdG8gd29yayB3aXRoXG4gKiB7QGxpbmsgRG9jdW1lbnRMaW5rfSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBEb2N1bWVudExpbms7XG4oZnVuY3Rpb24gKERvY3VtZW50TGluaykge1xuICAgIC8qKlxuICAgICAqIENyZWF0ZXMgYSBuZXcgRG9jdW1lbnRMaW5rIGxpdGVyYWwuXG4gICAgICovXG4gICAgZnVuY3Rpb24gY3JlYXRlKHJhbmdlLCB0YXJnZXQsIGRhdGEpIHtcbiAgICAgICAgcmV0dXJuIHsgcmFuZ2UsIHRhcmdldCwgZGF0YSB9O1xuICAgIH1cbiAgICBEb2N1bWVudExpbmsuY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgRG9jdW1lbnRMaW5rfSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgbGV0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMuZGVmaW5lZChjYW5kaWRhdGUpICYmIFJhbmdlLmlzKGNhbmRpZGF0ZS5yYW5nZSkgJiYgKElzLnVuZGVmaW5lZChjYW5kaWRhdGUudGFyZ2V0KSB8fCBJcy5zdHJpbmcoY2FuZGlkYXRlLnRhcmdldCkpO1xuICAgIH1cbiAgICBEb2N1bWVudExpbmsuaXMgPSBpcztcbn0pKERvY3VtZW50TGluayB8fCAoRG9jdW1lbnRMaW5rID0ge30pKTtcbi8qKlxuICogVGhlIFNlbGVjdGlvblJhbmdlIG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb24gdG8gd29yayB3aXRoXG4gKiBTZWxlY3Rpb25SYW5nZSBsaXRlcmFscy5cbiAqL1xuZXhwb3J0IHZhciBTZWxlY3Rpb25SYW5nZTtcbihmdW5jdGlvbiAoU2VsZWN0aW9uUmFuZ2UpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IFNlbGVjdGlvblJhbmdlXG4gICAgICogQHBhcmFtIHJhbmdlIHRoZSByYW5nZS5cbiAgICAgKiBAcGFyYW0gcGFyZW50IGFuIG9wdGlvbmFsIHBhcmVudC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUocmFuZ2UsIHBhcmVudCkge1xuICAgICAgICByZXR1cm4geyByYW5nZSwgcGFyZW50IH07XG4gICAgfVxuICAgIFNlbGVjdGlvblJhbmdlLmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBsZXQgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5vYmplY3RMaXRlcmFsKGNhbmRpZGF0ZSkgJiYgUmFuZ2UuaXMoY2FuZGlkYXRlLnJhbmdlKSAmJiAoY2FuZGlkYXRlLnBhcmVudCA9PT0gdW5kZWZpbmVkIHx8IFNlbGVjdGlvblJhbmdlLmlzKGNhbmRpZGF0ZS5wYXJlbnQpKTtcbiAgICB9XG4gICAgU2VsZWN0aW9uUmFuZ2UuaXMgPSBpcztcbn0pKFNlbGVjdGlvblJhbmdlIHx8IChTZWxlY3Rpb25SYW5nZSA9IHt9KSk7XG4vKipcbiAqIEEgc2V0IG9mIHByZWRlZmluZWQgdG9rZW4gdHlwZXMuIFRoaXMgc2V0IGlzIG5vdCBmaXhlZFxuICogYW4gY2xpZW50cyBjYW4gc3BlY2lmeSBhZGRpdGlvbmFsIHRva2VuIHR5cGVzIHZpYSB0aGVcbiAqIGNvcnJlc3BvbmRpbmcgY2xpZW50IGNhcGFiaWxpdGllcy5cbiAqXG4gKiBAc2luY2UgMy4xNi4wXG4gKi9cbmV4cG9ydCB2YXIgU2VtYW50aWNUb2tlblR5cGVzO1xuKGZ1bmN0aW9uIChTZW1hbnRpY1Rva2VuVHlwZXMpIHtcbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJuYW1lc3BhY2VcIl0gPSBcIm5hbWVzcGFjZVwiO1xuICAgIC8qKlxuICAgICAqIFJlcHJlc2VudHMgYSBnZW5lcmljIHR5cGUuIEFjdHMgYXMgYSBmYWxsYmFjayBmb3IgdHlwZXMgd2hpY2ggY2FuJ3QgYmUgbWFwcGVkIHRvXG4gICAgICogYSBzcGVjaWZpYyB0eXBlIGxpa2UgY2xhc3Mgb3IgZW51bS5cbiAgICAgKi9cbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJ0eXBlXCJdID0gXCJ0eXBlXCI7XG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1wiY2xhc3NcIl0gPSBcImNsYXNzXCI7XG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1wiZW51bVwiXSA9IFwiZW51bVwiO1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcImludGVyZmFjZVwiXSA9IFwiaW50ZXJmYWNlXCI7XG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1wic3RydWN0XCJdID0gXCJzdHJ1Y3RcIjtcbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJ0eXBlUGFyYW1ldGVyXCJdID0gXCJ0eXBlUGFyYW1ldGVyXCI7XG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1wicGFyYW1ldGVyXCJdID0gXCJwYXJhbWV0ZXJcIjtcbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJ2YXJpYWJsZVwiXSA9IFwidmFyaWFibGVcIjtcbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJwcm9wZXJ0eVwiXSA9IFwicHJvcGVydHlcIjtcbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJlbnVtTWVtYmVyXCJdID0gXCJlbnVtTWVtYmVyXCI7XG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1wiZXZlbnRcIl0gPSBcImV2ZW50XCI7XG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1wiZnVuY3Rpb25cIl0gPSBcImZ1bmN0aW9uXCI7XG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1wibWV0aG9kXCJdID0gXCJtZXRob2RcIjtcbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJtYWNyb1wiXSA9IFwibWFjcm9cIjtcbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJrZXl3b3JkXCJdID0gXCJrZXl3b3JkXCI7XG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1wibW9kaWZpZXJcIl0gPSBcIm1vZGlmaWVyXCI7XG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1wiY29tbWVudFwiXSA9IFwiY29tbWVudFwiO1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcInN0cmluZ1wiXSA9IFwic3RyaW5nXCI7XG4gICAgU2VtYW50aWNUb2tlblR5cGVzW1wibnVtYmVyXCJdID0gXCJudW1iZXJcIjtcbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJyZWdleHBcIl0gPSBcInJlZ2V4cFwiO1xuICAgIFNlbWFudGljVG9rZW5UeXBlc1tcIm9wZXJhdG9yXCJdID0gXCJvcGVyYXRvclwiO1xuICAgIC8qKlxuICAgICAqIEBzaW5jZSAzLjE3LjBcbiAgICAgKi9cbiAgICBTZW1hbnRpY1Rva2VuVHlwZXNbXCJkZWNvcmF0b3JcIl0gPSBcImRlY29yYXRvclwiO1xufSkoU2VtYW50aWNUb2tlblR5cGVzIHx8IChTZW1hbnRpY1Rva2VuVHlwZXMgPSB7fSkpO1xuLyoqXG4gKiBBIHNldCBvZiBwcmVkZWZpbmVkIHRva2VuIG1vZGlmaWVycy4gVGhpcyBzZXQgaXMgbm90IGZpeGVkXG4gKiBhbiBjbGllbnRzIGNhbiBzcGVjaWZ5IGFkZGl0aW9uYWwgdG9rZW4gdHlwZXMgdmlhIHRoZVxuICogY29ycmVzcG9uZGluZyBjbGllbnQgY2FwYWJpbGl0aWVzLlxuICpcbiAqIEBzaW5jZSAzLjE2LjBcbiAqL1xuZXhwb3J0IHZhciBTZW1hbnRpY1Rva2VuTW9kaWZpZXJzO1xuKGZ1bmN0aW9uIChTZW1hbnRpY1Rva2VuTW9kaWZpZXJzKSB7XG4gICAgU2VtYW50aWNUb2tlbk1vZGlmaWVyc1tcImRlY2xhcmF0aW9uXCJdID0gXCJkZWNsYXJhdGlvblwiO1xuICAgIFNlbWFudGljVG9rZW5Nb2RpZmllcnNbXCJkZWZpbml0aW9uXCJdID0gXCJkZWZpbml0aW9uXCI7XG4gICAgU2VtYW50aWNUb2tlbk1vZGlmaWVyc1tcInJlYWRvbmx5XCJdID0gXCJyZWFkb25seVwiO1xuICAgIFNlbWFudGljVG9rZW5Nb2RpZmllcnNbXCJzdGF0aWNcIl0gPSBcInN0YXRpY1wiO1xuICAgIFNlbWFudGljVG9rZW5Nb2RpZmllcnNbXCJkZXByZWNhdGVkXCJdID0gXCJkZXByZWNhdGVkXCI7XG4gICAgU2VtYW50aWNUb2tlbk1vZGlmaWVyc1tcImFic3RyYWN0XCJdID0gXCJhYnN0cmFjdFwiO1xuICAgIFNlbWFudGljVG9rZW5Nb2RpZmllcnNbXCJhc3luY1wiXSA9IFwiYXN5bmNcIjtcbiAgICBTZW1hbnRpY1Rva2VuTW9kaWZpZXJzW1wibW9kaWZpY2F0aW9uXCJdID0gXCJtb2RpZmljYXRpb25cIjtcbiAgICBTZW1hbnRpY1Rva2VuTW9kaWZpZXJzW1wiZG9jdW1lbnRhdGlvblwiXSA9IFwiZG9jdW1lbnRhdGlvblwiO1xuICAgIFNlbWFudGljVG9rZW5Nb2RpZmllcnNbXCJkZWZhdWx0TGlicmFyeVwiXSA9IFwiZGVmYXVsdExpYnJhcnlcIjtcbn0pKFNlbWFudGljVG9rZW5Nb2RpZmllcnMgfHwgKFNlbWFudGljVG9rZW5Nb2RpZmllcnMgPSB7fSkpO1xuLyoqXG4gKiBAc2luY2UgMy4xNi4wXG4gKi9cbmV4cG9ydCB2YXIgU2VtYW50aWNUb2tlbnM7XG4oZnVuY3Rpb24gKFNlbWFudGljVG9rZW5zKSB7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5vYmplY3RMaXRlcmFsKGNhbmRpZGF0ZSkgJiYgKGNhbmRpZGF0ZS5yZXN1bHRJZCA9PT0gdW5kZWZpbmVkIHx8IHR5cGVvZiBjYW5kaWRhdGUucmVzdWx0SWQgPT09ICdzdHJpbmcnKSAmJlxuICAgICAgICAgICAgQXJyYXkuaXNBcnJheShjYW5kaWRhdGUuZGF0YSkgJiYgKGNhbmRpZGF0ZS5kYXRhLmxlbmd0aCA9PT0gMCB8fCB0eXBlb2YgY2FuZGlkYXRlLmRhdGFbMF0gPT09ICdudW1iZXInKTtcbiAgICB9XG4gICAgU2VtYW50aWNUb2tlbnMuaXMgPSBpcztcbn0pKFNlbWFudGljVG9rZW5zIHx8IChTZW1hbnRpY1Rva2VucyA9IHt9KSk7XG4vKipcbiAqIFRoZSBJbmxpbmVWYWx1ZVRleHQgbmFtZXNwYWNlIHByb3ZpZGVzIGZ1bmN0aW9ucyB0byBkZWFsIHdpdGggSW5saW5lVmFsdWVUZXh0cy5cbiAqXG4gKiBAc2luY2UgMy4xNy4wXG4gKi9cbmV4cG9ydCB2YXIgSW5saW5lVmFsdWVUZXh0O1xuKGZ1bmN0aW9uIChJbmxpbmVWYWx1ZVRleHQpIHtcbiAgICAvKipcbiAgICAgKiBDcmVhdGVzIGEgbmV3IElubGluZVZhbHVlVGV4dCBsaXRlcmFsLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShyYW5nZSwgdGV4dCkge1xuICAgICAgICByZXR1cm4geyByYW5nZSwgdGV4dCB9O1xuICAgIH1cbiAgICBJbmxpbmVWYWx1ZVRleHQuY3JlYXRlID0gY3JlYXRlO1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICE9PSB1bmRlZmluZWQgJiYgY2FuZGlkYXRlICE9PSBudWxsICYmIFJhbmdlLmlzKGNhbmRpZGF0ZS5yYW5nZSkgJiYgSXMuc3RyaW5nKGNhbmRpZGF0ZS50ZXh0KTtcbiAgICB9XG4gICAgSW5saW5lVmFsdWVUZXh0LmlzID0gaXM7XG59KShJbmxpbmVWYWx1ZVRleHQgfHwgKElubGluZVZhbHVlVGV4dCA9IHt9KSk7XG4vKipcbiAqIFRoZSBJbmxpbmVWYWx1ZVZhcmlhYmxlTG9va3VwIG5hbWVzcGFjZSBwcm92aWRlcyBmdW5jdGlvbnMgdG8gZGVhbCB3aXRoIElubGluZVZhbHVlVmFyaWFibGVMb29rdXBzLlxuICpcbiAqIEBzaW5jZSAzLjE3LjBcbiAqL1xuZXhwb3J0IHZhciBJbmxpbmVWYWx1ZVZhcmlhYmxlTG9va3VwO1xuKGZ1bmN0aW9uIChJbmxpbmVWYWx1ZVZhcmlhYmxlTG9va3VwKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBJbmxpbmVWYWx1ZVRleHQgbGl0ZXJhbC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUocmFuZ2UsIHZhcmlhYmxlTmFtZSwgY2FzZVNlbnNpdGl2ZUxvb2t1cCkge1xuICAgICAgICByZXR1cm4geyByYW5nZSwgdmFyaWFibGVOYW1lLCBjYXNlU2Vuc2l0aXZlTG9va3VwIH07XG4gICAgfVxuICAgIElubGluZVZhbHVlVmFyaWFibGVMb29rdXAuY3JlYXRlID0gY3JlYXRlO1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gY2FuZGlkYXRlICE9PSB1bmRlZmluZWQgJiYgY2FuZGlkYXRlICE9PSBudWxsICYmIFJhbmdlLmlzKGNhbmRpZGF0ZS5yYW5nZSkgJiYgSXMuYm9vbGVhbihjYW5kaWRhdGUuY2FzZVNlbnNpdGl2ZUxvb2t1cClcbiAgICAgICAgICAgICYmIChJcy5zdHJpbmcoY2FuZGlkYXRlLnZhcmlhYmxlTmFtZSkgfHwgY2FuZGlkYXRlLnZhcmlhYmxlTmFtZSA9PT0gdW5kZWZpbmVkKTtcbiAgICB9XG4gICAgSW5saW5lVmFsdWVWYXJpYWJsZUxvb2t1cC5pcyA9IGlzO1xufSkoSW5saW5lVmFsdWVWYXJpYWJsZUxvb2t1cCB8fCAoSW5saW5lVmFsdWVWYXJpYWJsZUxvb2t1cCA9IHt9KSk7XG4vKipcbiAqIFRoZSBJbmxpbmVWYWx1ZUV2YWx1YXRhYmxlRXhwcmVzc2lvbiBuYW1lc3BhY2UgcHJvdmlkZXMgZnVuY3Rpb25zIHRvIGRlYWwgd2l0aCBJbmxpbmVWYWx1ZUV2YWx1YXRhYmxlRXhwcmVzc2lvbi5cbiAqXG4gKiBAc2luY2UgMy4xNy4wXG4gKi9cbmV4cG9ydCB2YXIgSW5saW5lVmFsdWVFdmFsdWF0YWJsZUV4cHJlc3Npb247XG4oZnVuY3Rpb24gKElubGluZVZhbHVlRXZhbHVhdGFibGVFeHByZXNzaW9uKSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBJbmxpbmVWYWx1ZUV2YWx1YXRhYmxlRXhwcmVzc2lvbiBsaXRlcmFsLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShyYW5nZSwgZXhwcmVzc2lvbikge1xuICAgICAgICByZXR1cm4geyByYW5nZSwgZXhwcmVzc2lvbiB9O1xuICAgIH1cbiAgICBJbmxpbmVWYWx1ZUV2YWx1YXRhYmxlRXhwcmVzc2lvbi5jcmVhdGUgPSBjcmVhdGU7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBjYW5kaWRhdGUgIT09IHVuZGVmaW5lZCAmJiBjYW5kaWRhdGUgIT09IG51bGwgJiYgUmFuZ2UuaXMoY2FuZGlkYXRlLnJhbmdlKVxuICAgICAgICAgICAgJiYgKElzLnN0cmluZyhjYW5kaWRhdGUuZXhwcmVzc2lvbikgfHwgY2FuZGlkYXRlLmV4cHJlc3Npb24gPT09IHVuZGVmaW5lZCk7XG4gICAgfVxuICAgIElubGluZVZhbHVlRXZhbHVhdGFibGVFeHByZXNzaW9uLmlzID0gaXM7XG59KShJbmxpbmVWYWx1ZUV2YWx1YXRhYmxlRXhwcmVzc2lvbiB8fCAoSW5saW5lVmFsdWVFdmFsdWF0YWJsZUV4cHJlc3Npb24gPSB7fSkpO1xuLyoqXG4gKiBUaGUgSW5saW5lVmFsdWVDb250ZXh0IG5hbWVzcGFjZSBwcm92aWRlcyBoZWxwZXIgZnVuY3Rpb25zIHRvIHdvcmsgd2l0aFxuICoge0BsaW5rIElubGluZVZhbHVlQ29udGV4dH0gbGl0ZXJhbHMuXG4gKlxuICogQHNpbmNlIDMuMTcuMFxuICovXG5leHBvcnQgdmFyIElubGluZVZhbHVlQ29udGV4dDtcbihmdW5jdGlvbiAoSW5saW5lVmFsdWVDb250ZXh0KSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBJbmxpbmVWYWx1ZUNvbnRleHQgbGl0ZXJhbC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUoZnJhbWVJZCwgc3RvcHBlZExvY2F0aW9uKSB7XG4gICAgICAgIHJldHVybiB7IGZyYW1lSWQsIHN0b3BwZWRMb2NhdGlvbiB9O1xuICAgIH1cbiAgICBJbmxpbmVWYWx1ZUNvbnRleHQuY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgSW5saW5lVmFsdWVDb250ZXh0fSBpbnRlcmZhY2UuXG4gICAgICovXG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgY29uc3QgY2FuZGlkYXRlID0gdmFsdWU7XG4gICAgICAgIHJldHVybiBJcy5kZWZpbmVkKGNhbmRpZGF0ZSkgJiYgUmFuZ2UuaXModmFsdWUuc3RvcHBlZExvY2F0aW9uKTtcbiAgICB9XG4gICAgSW5saW5lVmFsdWVDb250ZXh0LmlzID0gaXM7XG59KShJbmxpbmVWYWx1ZUNvbnRleHQgfHwgKElubGluZVZhbHVlQ29udGV4dCA9IHt9KSk7XG4vKipcbiAqIElubGF5IGhpbnQga2luZHMuXG4gKlxuICogQHNpbmNlIDMuMTcuMFxuICovXG5leHBvcnQgdmFyIElubGF5SGludEtpbmQ7XG4oZnVuY3Rpb24gKElubGF5SGludEtpbmQpIHtcbiAgICAvKipcbiAgICAgKiBBbiBpbmxheSBoaW50IHRoYXQgZm9yIGEgdHlwZSBhbm5vdGF0aW9uLlxuICAgICAqL1xuICAgIElubGF5SGludEtpbmQuVHlwZSA9IDE7XG4gICAgLyoqXG4gICAgICogQW4gaW5sYXkgaGludCB0aGF0IGlzIGZvciBhIHBhcmFtZXRlci5cbiAgICAgKi9cbiAgICBJbmxheUhpbnRLaW5kLlBhcmFtZXRlciA9IDI7XG4gICAgZnVuY3Rpb24gaXModmFsdWUpIHtcbiAgICAgICAgcmV0dXJuIHZhbHVlID09PSAxIHx8IHZhbHVlID09PSAyO1xuICAgIH1cbiAgICBJbmxheUhpbnRLaW5kLmlzID0gaXM7XG59KShJbmxheUhpbnRLaW5kIHx8IChJbmxheUhpbnRLaW5kID0ge30pKTtcbmV4cG9ydCB2YXIgSW5sYXlIaW50TGFiZWxQYXJ0O1xuKGZ1bmN0aW9uIChJbmxheUhpbnRMYWJlbFBhcnQpIHtcbiAgICBmdW5jdGlvbiBjcmVhdGUodmFsdWUpIHtcbiAgICAgICAgcmV0dXJuIHsgdmFsdWUgfTtcbiAgICB9XG4gICAgSW5sYXlIaW50TGFiZWxQYXJ0LmNyZWF0ZSA9IGNyZWF0ZTtcbiAgICBmdW5jdGlvbiBpcyh2YWx1ZSkge1xuICAgICAgICBjb25zdCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLm9iamVjdExpdGVyYWwoY2FuZGlkYXRlKVxuICAgICAgICAgICAgJiYgKGNhbmRpZGF0ZS50b29sdGlwID09PSB1bmRlZmluZWQgfHwgSXMuc3RyaW5nKGNhbmRpZGF0ZS50b29sdGlwKSB8fCBNYXJrdXBDb250ZW50LmlzKGNhbmRpZGF0ZS50b29sdGlwKSlcbiAgICAgICAgICAgICYmIChjYW5kaWRhdGUubG9jYXRpb24gPT09IHVuZGVmaW5lZCB8fCBMb2NhdGlvbi5pcyhjYW5kaWRhdGUubG9jYXRpb24pKVxuICAgICAgICAgICAgJiYgKGNhbmRpZGF0ZS5jb21tYW5kID09PSB1bmRlZmluZWQgfHwgQ29tbWFuZC5pcyhjYW5kaWRhdGUuY29tbWFuZCkpO1xuICAgIH1cbiAgICBJbmxheUhpbnRMYWJlbFBhcnQuaXMgPSBpcztcbn0pKElubGF5SGludExhYmVsUGFydCB8fCAoSW5sYXlIaW50TGFiZWxQYXJ0ID0ge30pKTtcbmV4cG9ydCB2YXIgSW5sYXlIaW50O1xuKGZ1bmN0aW9uIChJbmxheUhpbnQpIHtcbiAgICBmdW5jdGlvbiBjcmVhdGUocG9zaXRpb24sIGxhYmVsLCBraW5kKSB7XG4gICAgICAgIGNvbnN0IHJlc3VsdCA9IHsgcG9zaXRpb24sIGxhYmVsIH07XG4gICAgICAgIGlmIChraW5kICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIHJlc3VsdC5raW5kID0ga2luZDtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cbiAgICBJbmxheUhpbnQuY3JlYXRlID0gY3JlYXRlO1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMub2JqZWN0TGl0ZXJhbChjYW5kaWRhdGUpICYmIFBvc2l0aW9uLmlzKGNhbmRpZGF0ZS5wb3NpdGlvbilcbiAgICAgICAgICAgICYmIChJcy5zdHJpbmcoY2FuZGlkYXRlLmxhYmVsKSB8fCBJcy50eXBlZEFycmF5KGNhbmRpZGF0ZS5sYWJlbCwgSW5sYXlIaW50TGFiZWxQYXJ0LmlzKSlcbiAgICAgICAgICAgICYmIChjYW5kaWRhdGUua2luZCA9PT0gdW5kZWZpbmVkIHx8IElubGF5SGludEtpbmQuaXMoY2FuZGlkYXRlLmtpbmQpKVxuICAgICAgICAgICAgJiYgKGNhbmRpZGF0ZS50ZXh0RWRpdHMgPT09IHVuZGVmaW5lZCkgfHwgSXMudHlwZWRBcnJheShjYW5kaWRhdGUudGV4dEVkaXRzLCBUZXh0RWRpdC5pcylcbiAgICAgICAgICAgICYmIChjYW5kaWRhdGUudG9vbHRpcCA9PT0gdW5kZWZpbmVkIHx8IElzLnN0cmluZyhjYW5kaWRhdGUudG9vbHRpcCkgfHwgTWFya3VwQ29udGVudC5pcyhjYW5kaWRhdGUudG9vbHRpcCkpXG4gICAgICAgICAgICAmJiAoY2FuZGlkYXRlLnBhZGRpbmdMZWZ0ID09PSB1bmRlZmluZWQgfHwgSXMuYm9vbGVhbihjYW5kaWRhdGUucGFkZGluZ0xlZnQpKVxuICAgICAgICAgICAgJiYgKGNhbmRpZGF0ZS5wYWRkaW5nUmlnaHQgPT09IHVuZGVmaW5lZCB8fCBJcy5ib29sZWFuKGNhbmRpZGF0ZS5wYWRkaW5nUmlnaHQpKTtcbiAgICB9XG4gICAgSW5sYXlIaW50LmlzID0gaXM7XG59KShJbmxheUhpbnQgfHwgKElubGF5SGludCA9IHt9KSk7XG5leHBvcnQgdmFyIFN0cmluZ1ZhbHVlO1xuKGZ1bmN0aW9uIChTdHJpbmdWYWx1ZSkge1xuICAgIGZ1bmN0aW9uIGNyZWF0ZVNuaXBwZXQodmFsdWUpIHtcbiAgICAgICAgcmV0dXJuIHsga2luZDogJ3NuaXBwZXQnLCB2YWx1ZSB9O1xuICAgIH1cbiAgICBTdHJpbmdWYWx1ZS5jcmVhdGVTbmlwcGV0ID0gY3JlYXRlU25pcHBldDtcbn0pKFN0cmluZ1ZhbHVlIHx8IChTdHJpbmdWYWx1ZSA9IHt9KSk7XG5leHBvcnQgdmFyIElubGluZUNvbXBsZXRpb25JdGVtO1xuKGZ1bmN0aW9uIChJbmxpbmVDb21wbGV0aW9uSXRlbSkge1xuICAgIGZ1bmN0aW9uIGNyZWF0ZShpbnNlcnRUZXh0LCBmaWx0ZXJUZXh0LCByYW5nZSwgY29tbWFuZCkge1xuICAgICAgICByZXR1cm4geyBpbnNlcnRUZXh0LCBmaWx0ZXJUZXh0LCByYW5nZSwgY29tbWFuZCB9O1xuICAgIH1cbiAgICBJbmxpbmVDb21wbGV0aW9uSXRlbS5jcmVhdGUgPSBjcmVhdGU7XG59KShJbmxpbmVDb21wbGV0aW9uSXRlbSB8fCAoSW5saW5lQ29tcGxldGlvbkl0ZW0gPSB7fSkpO1xuZXhwb3J0IHZhciBJbmxpbmVDb21wbGV0aW9uTGlzdDtcbihmdW5jdGlvbiAoSW5saW5lQ29tcGxldGlvbkxpc3QpIHtcbiAgICBmdW5jdGlvbiBjcmVhdGUoaXRlbXMpIHtcbiAgICAgICAgcmV0dXJuIHsgaXRlbXMgfTtcbiAgICB9XG4gICAgSW5saW5lQ29tcGxldGlvbkxpc3QuY3JlYXRlID0gY3JlYXRlO1xufSkoSW5saW5lQ29tcGxldGlvbkxpc3QgfHwgKElubGluZUNvbXBsZXRpb25MaXN0ID0ge30pKTtcbi8qKlxuICogRGVzY3JpYmVzIGhvdyBhbiB7QGxpbmsgSW5saW5lQ29tcGxldGlvbkl0ZW1Qcm92aWRlciBpbmxpbmUgY29tcGxldGlvbiBwcm92aWRlcn0gd2FzIHRyaWdnZXJlZC5cbiAqXG4gKiBAc2luY2UgMy4xOC4wXG4gKiBAcHJvcG9zZWRcbiAqL1xuZXhwb3J0IHZhciBJbmxpbmVDb21wbGV0aW9uVHJpZ2dlcktpbmQ7XG4oZnVuY3Rpb24gKElubGluZUNvbXBsZXRpb25UcmlnZ2VyS2luZCkge1xuICAgIC8qKlxuICAgICAqIENvbXBsZXRpb24gd2FzIHRyaWdnZXJlZCBleHBsaWNpdGx5IGJ5IGEgdXNlciBnZXN0dXJlLlxuICAgICAqL1xuICAgIElubGluZUNvbXBsZXRpb25UcmlnZ2VyS2luZC5JbnZva2VkID0gMDtcbiAgICAvKipcbiAgICAgKiBDb21wbGV0aW9uIHdhcyB0cmlnZ2VyZWQgYXV0b21hdGljYWxseSB3aGlsZSBlZGl0aW5nLlxuICAgICAqL1xuICAgIElubGluZUNvbXBsZXRpb25UcmlnZ2VyS2luZC5BdXRvbWF0aWMgPSAxO1xufSkoSW5saW5lQ29tcGxldGlvblRyaWdnZXJLaW5kIHx8IChJbmxpbmVDb21wbGV0aW9uVHJpZ2dlcktpbmQgPSB7fSkpO1xuZXhwb3J0IHZhciBTZWxlY3RlZENvbXBsZXRpb25JbmZvO1xuKGZ1bmN0aW9uIChTZWxlY3RlZENvbXBsZXRpb25JbmZvKSB7XG4gICAgZnVuY3Rpb24gY3JlYXRlKHJhbmdlLCB0ZXh0KSB7XG4gICAgICAgIHJldHVybiB7IHJhbmdlLCB0ZXh0IH07XG4gICAgfVxuICAgIFNlbGVjdGVkQ29tcGxldGlvbkluZm8uY3JlYXRlID0gY3JlYXRlO1xufSkoU2VsZWN0ZWRDb21wbGV0aW9uSW5mbyB8fCAoU2VsZWN0ZWRDb21wbGV0aW9uSW5mbyA9IHt9KSk7XG5leHBvcnQgdmFyIElubGluZUNvbXBsZXRpb25Db250ZXh0O1xuKGZ1bmN0aW9uIChJbmxpbmVDb21wbGV0aW9uQ29udGV4dCkge1xuICAgIGZ1bmN0aW9uIGNyZWF0ZSh0cmlnZ2VyS2luZCwgc2VsZWN0ZWRDb21wbGV0aW9uSW5mbykge1xuICAgICAgICByZXR1cm4geyB0cmlnZ2VyS2luZCwgc2VsZWN0ZWRDb21wbGV0aW9uSW5mbyB9O1xuICAgIH1cbiAgICBJbmxpbmVDb21wbGV0aW9uQ29udGV4dC5jcmVhdGUgPSBjcmVhdGU7XG59KShJbmxpbmVDb21wbGV0aW9uQ29udGV4dCB8fCAoSW5saW5lQ29tcGxldGlvbkNvbnRleHQgPSB7fSkpO1xuZXhwb3J0IHZhciBXb3Jrc3BhY2VGb2xkZXI7XG4oZnVuY3Rpb24gKFdvcmtzcGFjZUZvbGRlcikge1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGNvbnN0IGNhbmRpZGF0ZSA9IHZhbHVlO1xuICAgICAgICByZXR1cm4gSXMub2JqZWN0TGl0ZXJhbChjYW5kaWRhdGUpICYmIFVSSS5pcyhjYW5kaWRhdGUudXJpKSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLm5hbWUpO1xuICAgIH1cbiAgICBXb3Jrc3BhY2VGb2xkZXIuaXMgPSBpcztcbn0pKFdvcmtzcGFjZUZvbGRlciB8fCAoV29ya3NwYWNlRm9sZGVyID0ge30pKTtcbmV4cG9ydCBjb25zdCBFT0wgPSBbJ1xcbicsICdcXHJcXG4nLCAnXFxyJ107XG4vKipcbiAqIEBkZXByZWNhdGVkIFVzZSB0aGUgdGV4dCBkb2N1bWVudCBmcm9tIHRoZSBuZXcgdnNjb2RlLWxhbmd1YWdlc2VydmVyLXRleHRkb2N1bWVudCBwYWNrYWdlLlxuICovXG5leHBvcnQgdmFyIFRleHREb2N1bWVudDtcbihmdW5jdGlvbiAoVGV4dERvY3VtZW50KSB7XG4gICAgLyoqXG4gICAgICogQ3JlYXRlcyBhIG5ldyBJVGV4dERvY3VtZW50IGxpdGVyYWwgZnJvbSB0aGUgZ2l2ZW4gdXJpIGFuZCBjb250ZW50LlxuICAgICAqIEBwYXJhbSB1cmkgVGhlIGRvY3VtZW50J3MgdXJpLlxuICAgICAqIEBwYXJhbSBsYW5ndWFnZUlkIFRoZSBkb2N1bWVudCdzIGxhbmd1YWdlIElkLlxuICAgICAqIEBwYXJhbSB2ZXJzaW9uIFRoZSBkb2N1bWVudCdzIHZlcnNpb24uXG4gICAgICogQHBhcmFtIGNvbnRlbnQgVGhlIGRvY3VtZW50J3MgY29udGVudC5cbiAgICAgKi9cbiAgICBmdW5jdGlvbiBjcmVhdGUodXJpLCBsYW5ndWFnZUlkLCB2ZXJzaW9uLCBjb250ZW50KSB7XG4gICAgICAgIHJldHVybiBuZXcgRnVsbFRleHREb2N1bWVudCh1cmksIGxhbmd1YWdlSWQsIHZlcnNpb24sIGNvbnRlbnQpO1xuICAgIH1cbiAgICBUZXh0RG9jdW1lbnQuY3JlYXRlID0gY3JlYXRlO1xuICAgIC8qKlxuICAgICAqIENoZWNrcyB3aGV0aGVyIHRoZSBnaXZlbiBsaXRlcmFsIGNvbmZvcm1zIHRvIHRoZSB7QGxpbmsgSVRleHREb2N1bWVudH0gaW50ZXJmYWNlLlxuICAgICAqL1xuICAgIGZ1bmN0aW9uIGlzKHZhbHVlKSB7XG4gICAgICAgIGxldCBjYW5kaWRhdGUgPSB2YWx1ZTtcbiAgICAgICAgcmV0dXJuIElzLmRlZmluZWQoY2FuZGlkYXRlKSAmJiBJcy5zdHJpbmcoY2FuZGlkYXRlLnVyaSkgJiYgKElzLnVuZGVmaW5lZChjYW5kaWRhdGUubGFuZ3VhZ2VJZCkgfHwgSXMuc3RyaW5nKGNhbmRpZGF0ZS5sYW5ndWFnZUlkKSkgJiYgSXMudWludGVnZXIoY2FuZGlkYXRlLmxpbmVDb3VudClcbiAgICAgICAgICAgICYmIElzLmZ1bmMoY2FuZGlkYXRlLmdldFRleHQpICYmIElzLmZ1bmMoY2FuZGlkYXRlLnBvc2l0aW9uQXQpICYmIElzLmZ1bmMoY2FuZGlkYXRlLm9mZnNldEF0KSA/IHRydWUgOiBmYWxzZTtcbiAgICB9XG4gICAgVGV4dERvY3VtZW50LmlzID0gaXM7XG4gICAgZnVuY3Rpb24gYXBwbHlFZGl0cyhkb2N1bWVudCwgZWRpdHMpIHtcbiAgICAgICAgbGV0IHRleHQgPSBkb2N1bWVudC5nZXRUZXh0KCk7XG4gICAgICAgIGxldCBzb3J0ZWRFZGl0cyA9IG1lcmdlU29ydChlZGl0cywgKGEsIGIpID0+IHtcbiAgICAgICAgICAgIGxldCBkaWZmID0gYS5yYW5nZS5zdGFydC5saW5lIC0gYi5yYW5nZS5zdGFydC5saW5lO1xuICAgICAgICAgICAgaWYgKGRpZmYgPT09IDApIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gYS5yYW5nZS5zdGFydC5jaGFyYWN0ZXIgLSBiLnJhbmdlLnN0YXJ0LmNoYXJhY3RlcjtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJldHVybiBkaWZmO1xuICAgICAgICB9KTtcbiAgICAgICAgbGV0IGxhc3RNb2RpZmllZE9mZnNldCA9IHRleHQubGVuZ3RoO1xuICAgICAgICBmb3IgKGxldCBpID0gc29ydGVkRWRpdHMubGVuZ3RoIC0gMTsgaSA+PSAwOyBpLS0pIHtcbiAgICAgICAgICAgIGxldCBlID0gc29ydGVkRWRpdHNbaV07XG4gICAgICAgICAgICBsZXQgc3RhcnRPZmZzZXQgPSBkb2N1bWVudC5vZmZzZXRBdChlLnJhbmdlLnN0YXJ0KTtcbiAgICAgICAgICAgIGxldCBlbmRPZmZzZXQgPSBkb2N1bWVudC5vZmZzZXRBdChlLnJhbmdlLmVuZCk7XG4gICAgICAgICAgICBpZiAoZW5kT2Zmc2V0IDw9IGxhc3RNb2RpZmllZE9mZnNldCkge1xuICAgICAgICAgICAgICAgIHRleHQgPSB0ZXh0LnN1YnN0cmluZygwLCBzdGFydE9mZnNldCkgKyBlLm5ld1RleHQgKyB0ZXh0LnN1YnN0cmluZyhlbmRPZmZzZXQsIHRleHQubGVuZ3RoKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsc2Uge1xuICAgICAgICAgICAgICAgIHRocm93IG5ldyBFcnJvcignT3ZlcmxhcHBpbmcgZWRpdCcpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgbGFzdE1vZGlmaWVkT2Zmc2V0ID0gc3RhcnRPZmZzZXQ7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHRleHQ7XG4gICAgfVxuICAgIFRleHREb2N1bWVudC5hcHBseUVkaXRzID0gYXBwbHlFZGl0cztcbiAgICBmdW5jdGlvbiBtZXJnZVNvcnQoZGF0YSwgY29tcGFyZSkge1xuICAgICAgICBpZiAoZGF0YS5sZW5ndGggPD0gMSkge1xuICAgICAgICAgICAgLy8gc29ydGVkXG4gICAgICAgICAgICByZXR1cm4gZGF0YTtcbiAgICAgICAgfVxuICAgICAgICBjb25zdCBwID0gKGRhdGEubGVuZ3RoIC8gMikgfCAwO1xuICAgICAgICBjb25zdCBsZWZ0ID0gZGF0YS5zbGljZSgwLCBwKTtcbiAgICAgICAgY29uc3QgcmlnaHQgPSBkYXRhLnNsaWNlKHApO1xuICAgICAgICBtZXJnZVNvcnQobGVmdCwgY29tcGFyZSk7XG4gICAgICAgIG1lcmdlU29ydChyaWdodCwgY29tcGFyZSk7XG4gICAgICAgIGxldCBsZWZ0SWR4ID0gMDtcbiAgICAgICAgbGV0IHJpZ2h0SWR4ID0gMDtcbiAgICAgICAgbGV0IGkgPSAwO1xuICAgICAgICB3aGlsZSAobGVmdElkeCA8IGxlZnQubGVuZ3RoICYmIHJpZ2h0SWR4IDwgcmlnaHQubGVuZ3RoKSB7XG4gICAgICAgICAgICBsZXQgcmV0ID0gY29tcGFyZShsZWZ0W2xlZnRJZHhdLCByaWdodFtyaWdodElkeF0pO1xuICAgICAgICAgICAgaWYgKHJldCA8PSAwKSB7XG4gICAgICAgICAgICAgICAgLy8gc21hbGxlcl9lcXVhbCAtPiB0YWtlIGxlZnQgdG8gcHJlc2VydmUgb3JkZXJcbiAgICAgICAgICAgICAgICBkYXRhW2krK10gPSBsZWZ0W2xlZnRJZHgrK107XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICAvLyBncmVhdGVyIC0+IHRha2UgcmlnaHRcbiAgICAgICAgICAgICAgICBkYXRhW2krK10gPSByaWdodFtyaWdodElkeCsrXTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICB3aGlsZSAobGVmdElkeCA8IGxlZnQubGVuZ3RoKSB7XG4gICAgICAgICAgICBkYXRhW2krK10gPSBsZWZ0W2xlZnRJZHgrK107XG4gICAgICAgIH1cbiAgICAgICAgd2hpbGUgKHJpZ2h0SWR4IDwgcmlnaHQubGVuZ3RoKSB7XG4gICAgICAgICAgICBkYXRhW2krK10gPSByaWdodFtyaWdodElkeCsrXTtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gZGF0YTtcbiAgICB9XG59KShUZXh0RG9jdW1lbnQgfHwgKFRleHREb2N1bWVudCA9IHt9KSk7XG4vKipcbiAqIEBkZXByZWNhdGVkIFVzZSB0aGUgdGV4dCBkb2N1bWVudCBmcm9tIHRoZSBuZXcgdnNjb2RlLWxhbmd1YWdlc2VydmVyLXRleHRkb2N1bWVudCBwYWNrYWdlLlxuICovXG5jbGFzcyBGdWxsVGV4dERvY3VtZW50IHtcbiAgICBjb25zdHJ1Y3Rvcih1cmksIGxhbmd1YWdlSWQsIHZlcnNpb24sIGNvbnRlbnQpIHtcbiAgICAgICAgdGhpcy5fdXJpID0gdXJpO1xuICAgICAgICB0aGlzLl9sYW5ndWFnZUlkID0gbGFuZ3VhZ2VJZDtcbiAgICAgICAgdGhpcy5fdmVyc2lvbiA9IHZlcnNpb247XG4gICAgICAgIHRoaXMuX2NvbnRlbnQgPSBjb250ZW50O1xuICAgICAgICB0aGlzLl9saW5lT2Zmc2V0cyA9IHVuZGVmaW5lZDtcbiAgICB9XG4gICAgZ2V0IHVyaSgpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX3VyaTtcbiAgICB9XG4gICAgZ2V0IGxhbmd1YWdlSWQoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLl9sYW5ndWFnZUlkO1xuICAgIH1cbiAgICBnZXQgdmVyc2lvbigpIHtcbiAgICAgICAgcmV0dXJuIHRoaXMuX3ZlcnNpb247XG4gICAgfVxuICAgIGdldFRleHQocmFuZ2UpIHtcbiAgICAgICAgaWYgKHJhbmdlKSB7XG4gICAgICAgICAgICBsZXQgc3RhcnQgPSB0aGlzLm9mZnNldEF0KHJhbmdlLnN0YXJ0KTtcbiAgICAgICAgICAgIGxldCBlbmQgPSB0aGlzLm9mZnNldEF0KHJhbmdlLmVuZCk7XG4gICAgICAgICAgICByZXR1cm4gdGhpcy5fY29udGVudC5zdWJzdHJpbmcoc3RhcnQsIGVuZCk7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHRoaXMuX2NvbnRlbnQ7XG4gICAgfVxuICAgIHVwZGF0ZShldmVudCwgdmVyc2lvbikge1xuICAgICAgICB0aGlzLl9jb250ZW50ID0gZXZlbnQudGV4dDtcbiAgICAgICAgdGhpcy5fdmVyc2lvbiA9IHZlcnNpb247XG4gICAgICAgIHRoaXMuX2xpbmVPZmZzZXRzID0gdW5kZWZpbmVkO1xuICAgIH1cbiAgICBnZXRMaW5lT2Zmc2V0cygpIHtcbiAgICAgICAgaWYgKHRoaXMuX2xpbmVPZmZzZXRzID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAgIGxldCBsaW5lT2Zmc2V0cyA9IFtdO1xuICAgICAgICAgICAgbGV0IHRleHQgPSB0aGlzLl9jb250ZW50O1xuICAgICAgICAgICAgbGV0IGlzTGluZVN0YXJ0ID0gdHJ1ZTtcbiAgICAgICAgICAgIGZvciAobGV0IGkgPSAwOyBpIDwgdGV4dC5sZW5ndGg7IGkrKykge1xuICAgICAgICAgICAgICAgIGlmIChpc0xpbmVTdGFydCkge1xuICAgICAgICAgICAgICAgICAgICBsaW5lT2Zmc2V0cy5wdXNoKGkpO1xuICAgICAgICAgICAgICAgICAgICBpc0xpbmVTdGFydCA9IGZhbHNlO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBsZXQgY2ggPSB0ZXh0LmNoYXJBdChpKTtcbiAgICAgICAgICAgICAgICBpc0xpbmVTdGFydCA9IChjaCA9PT0gJ1xccicgfHwgY2ggPT09ICdcXG4nKTtcbiAgICAgICAgICAgICAgICBpZiAoY2ggPT09ICdcXHInICYmIGkgKyAxIDwgdGV4dC5sZW5ndGggJiYgdGV4dC5jaGFyQXQoaSArIDEpID09PSAnXFxuJykge1xuICAgICAgICAgICAgICAgICAgICBpKys7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaWYgKGlzTGluZVN0YXJ0ICYmIHRleHQubGVuZ3RoID4gMCkge1xuICAgICAgICAgICAgICAgIGxpbmVPZmZzZXRzLnB1c2godGV4dC5sZW5ndGgpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgdGhpcy5fbGluZU9mZnNldHMgPSBsaW5lT2Zmc2V0cztcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gdGhpcy5fbGluZU9mZnNldHM7XG4gICAgfVxuICAgIHBvc2l0aW9uQXQob2Zmc2V0KSB7XG4gICAgICAgIG9mZnNldCA9IE1hdGgubWF4KE1hdGgubWluKG9mZnNldCwgdGhpcy5fY29udGVudC5sZW5ndGgpLCAwKTtcbiAgICAgICAgbGV0IGxpbmVPZmZzZXRzID0gdGhpcy5nZXRMaW5lT2Zmc2V0cygpO1xuICAgICAgICBsZXQgbG93ID0gMCwgaGlnaCA9IGxpbmVPZmZzZXRzLmxlbmd0aDtcbiAgICAgICAgaWYgKGhpZ2ggPT09IDApIHtcbiAgICAgICAgICAgIHJldHVybiBQb3NpdGlvbi5jcmVhdGUoMCwgb2Zmc2V0KTtcbiAgICAgICAgfVxuICAgICAgICB3aGlsZSAobG93IDwgaGlnaCkge1xuICAgICAgICAgICAgbGV0IG1pZCA9IE1hdGguZmxvb3IoKGxvdyArIGhpZ2gpIC8gMik7XG4gICAgICAgICAgICBpZiAobGluZU9mZnNldHNbbWlkXSA+IG9mZnNldCkge1xuICAgICAgICAgICAgICAgIGhpZ2ggPSBtaWQ7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbHNlIHtcbiAgICAgICAgICAgICAgICBsb3cgPSBtaWQgKyAxO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIC8vIGxvdyBpcyB0aGUgbGVhc3QgeCBmb3Igd2hpY2ggdGhlIGxpbmUgb2Zmc2V0IGlzIGxhcmdlciB0aGFuIHRoZSBjdXJyZW50IG9mZnNldFxuICAgICAgICAvLyBvciBhcnJheS5sZW5ndGggaWYgbm8gbGluZSBvZmZzZXQgaXMgbGFyZ2VyIHRoYW4gdGhlIGN1cnJlbnQgb2Zmc2V0XG4gICAgICAgIGxldCBsaW5lID0gbG93IC0gMTtcbiAgICAgICAgcmV0dXJuIFBvc2l0aW9uLmNyZWF0ZShsaW5lLCBvZmZzZXQgLSBsaW5lT2Zmc2V0c1tsaW5lXSk7XG4gICAgfVxuICAgIG9mZnNldEF0KHBvc2l0aW9uKSB7XG4gICAgICAgIGxldCBsaW5lT2Zmc2V0cyA9IHRoaXMuZ2V0TGluZU9mZnNldHMoKTtcbiAgICAgICAgaWYgKHBvc2l0aW9uLmxpbmUgPj0gbGluZU9mZnNldHMubGVuZ3RoKSB7XG4gICAgICAgICAgICByZXR1cm4gdGhpcy5fY29udGVudC5sZW5ndGg7XG4gICAgICAgIH1cbiAgICAgICAgZWxzZSBpZiAocG9zaXRpb24ubGluZSA8IDApIHtcbiAgICAgICAgICAgIHJldHVybiAwO1xuICAgICAgICB9XG4gICAgICAgIGxldCBsaW5lT2Zmc2V0ID0gbGluZU9mZnNldHNbcG9zaXRpb24ubGluZV07XG4gICAgICAgIGxldCBuZXh0TGluZU9mZnNldCA9IChwb3NpdGlvbi5saW5lICsgMSA8IGxpbmVPZmZzZXRzLmxlbmd0aCkgPyBsaW5lT2Zmc2V0c1twb3NpdGlvbi5saW5lICsgMV0gOiB0aGlzLl9jb250ZW50Lmxlbmd0aDtcbiAgICAgICAgcmV0dXJuIE1hdGgubWF4KE1hdGgubWluKGxpbmVPZmZzZXQgKyBwb3NpdGlvbi5jaGFyYWN0ZXIsIG5leHRMaW5lT2Zmc2V0KSwgbGluZU9mZnNldCk7XG4gICAgfVxuICAgIGdldCBsaW5lQ291bnQoKSB7XG4gICAgICAgIHJldHVybiB0aGlzLmdldExpbmVPZmZzZXRzKCkubGVuZ3RoO1xuICAgIH1cbn1cbnZhciBJcztcbihmdW5jdGlvbiAoSXMpIHtcbiAgICBjb25zdCB0b1N0cmluZyA9IE9iamVjdC5wcm90b3R5cGUudG9TdHJpbmc7XG4gICAgZnVuY3Rpb24gZGVmaW5lZCh2YWx1ZSkge1xuICAgICAgICByZXR1cm4gdHlwZW9mIHZhbHVlICE9PSAndW5kZWZpbmVkJztcbiAgICB9XG4gICAgSXMuZGVmaW5lZCA9IGRlZmluZWQ7XG4gICAgZnVuY3Rpb24gdW5kZWZpbmVkKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiB0eXBlb2YgdmFsdWUgPT09ICd1bmRlZmluZWQnO1xuICAgIH1cbiAgICBJcy51bmRlZmluZWQgPSB1bmRlZmluZWQ7XG4gICAgZnVuY3Rpb24gYm9vbGVhbih2YWx1ZSkge1xuICAgICAgICByZXR1cm4gdmFsdWUgPT09IHRydWUgfHwgdmFsdWUgPT09IGZhbHNlO1xuICAgIH1cbiAgICBJcy5ib29sZWFuID0gYm9vbGVhbjtcbiAgICBmdW5jdGlvbiBzdHJpbmcodmFsdWUpIHtcbiAgICAgICAgcmV0dXJuIHRvU3RyaW5nLmNhbGwodmFsdWUpID09PSAnW29iamVjdCBTdHJpbmddJztcbiAgICB9XG4gICAgSXMuc3RyaW5nID0gc3RyaW5nO1xuICAgIGZ1bmN0aW9uIG51bWJlcih2YWx1ZSkge1xuICAgICAgICByZXR1cm4gdG9TdHJpbmcuY2FsbCh2YWx1ZSkgPT09ICdbb2JqZWN0IE51bWJlcl0nO1xuICAgIH1cbiAgICBJcy5udW1iZXIgPSBudW1iZXI7XG4gICAgZnVuY3Rpb24gbnVtYmVyUmFuZ2UodmFsdWUsIG1pbiwgbWF4KSB7XG4gICAgICAgIHJldHVybiB0b1N0cmluZy5jYWxsKHZhbHVlKSA9PT0gJ1tvYmplY3QgTnVtYmVyXScgJiYgbWluIDw9IHZhbHVlICYmIHZhbHVlIDw9IG1heDtcbiAgICB9XG4gICAgSXMubnVtYmVyUmFuZ2UgPSBudW1iZXJSYW5nZTtcbiAgICBmdW5jdGlvbiBpbnRlZ2VyKHZhbHVlKSB7XG4gICAgICAgIHJldHVybiB0b1N0cmluZy5jYWxsKHZhbHVlKSA9PT0gJ1tvYmplY3QgTnVtYmVyXScgJiYgLTIxNDc0ODM2NDggPD0gdmFsdWUgJiYgdmFsdWUgPD0gMjE0NzQ4MzY0NztcbiAgICB9XG4gICAgSXMuaW50ZWdlciA9IGludGVnZXI7XG4gICAgZnVuY3Rpb24gdWludGVnZXIodmFsdWUpIHtcbiAgICAgICAgcmV0dXJuIHRvU3RyaW5nLmNhbGwodmFsdWUpID09PSAnW29iamVjdCBOdW1iZXJdJyAmJiAwIDw9IHZhbHVlICYmIHZhbHVlIDw9IDIxNDc0ODM2NDc7XG4gICAgfVxuICAgIElzLnVpbnRlZ2VyID0gdWludGVnZXI7XG4gICAgZnVuY3Rpb24gZnVuYyh2YWx1ZSkge1xuICAgICAgICByZXR1cm4gdG9TdHJpbmcuY2FsbCh2YWx1ZSkgPT09ICdbb2JqZWN0IEZ1bmN0aW9uXSc7XG4gICAgfVxuICAgIElzLmZ1bmMgPSBmdW5jO1xuICAgIGZ1bmN0aW9uIG9iamVjdExpdGVyYWwodmFsdWUpIHtcbiAgICAgICAgLy8gU3RyaWN0bHkgc3BlYWtpbmcgY2xhc3MgaW5zdGFuY2VzIHBhc3MgdGhpcyBjaGVjayBhcyB3ZWxsLiBTaW5jZSB0aGUgTFNQXG4gICAgICAgIC8vIGRvZXNuJ3QgdXNlIGNsYXNzZXMgd2UgaWdub3JlIHRoaXMgZm9yIG5vdy4gSWYgd2UgZG8gd2UgbmVlZCB0byBhZGQgc29tZXRoaW5nXG4gICAgICAgIC8vIGxpa2UgdGhpczogYE9iamVjdC5nZXRQcm90b3R5cGVPZihPYmplY3QuZ2V0UHJvdG90eXBlT2YoeCkpID09PSBudWxsYFxuICAgICAgICByZXR1cm4gdmFsdWUgIT09IG51bGwgJiYgdHlwZW9mIHZhbHVlID09PSAnb2JqZWN0JztcbiAgICB9XG4gICAgSXMub2JqZWN0TGl0ZXJhbCA9IG9iamVjdExpdGVyYWw7XG4gICAgZnVuY3Rpb24gdHlwZWRBcnJheSh2YWx1ZSwgY2hlY2spIHtcbiAgICAgICAgcmV0dXJuIEFycmF5LmlzQXJyYXkodmFsdWUpICYmIHZhbHVlLmV2ZXJ5KGNoZWNrKTtcbiAgICB9XG4gICAgSXMudHlwZWRBcnJheSA9IHR5cGVkQXJyYXk7XG59KShJcyB8fCAoSXMgPSB7fSkpO1xuIl0sCiAgIm1hcHBpbmdzIjogIjs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7O0FBQUE7QUFBQTtBQUFBO0FBS0EsYUFBTyxlQUFlLFNBQVMsY0FBYyxFQUFFLE9BQU8sS0FBSyxDQUFDO0FBQzVELGNBQVEsY0FBYyxRQUFRLFFBQVEsUUFBUSxPQUFPLFFBQVEsUUFBUSxRQUFRLFNBQVMsUUFBUSxTQUFTLFFBQVEsVUFBVTtBQUN6SCxlQUFTLFFBQVEsT0FBTztBQUNwQixlQUFPLFVBQVUsUUFBUSxVQUFVO0FBQUEsTUFDdkM7QUFDQSxjQUFRLFVBQVU7QUFDbEIsZUFBUyxPQUFPLE9BQU87QUFDbkIsZUFBTyxPQUFPLFVBQVUsWUFBWSxpQkFBaUI7QUFBQSxNQUN6RDtBQUNBLGNBQVEsU0FBUztBQUNqQixlQUFTLE9BQU8sT0FBTztBQUNuQixlQUFPLE9BQU8sVUFBVSxZQUFZLGlCQUFpQjtBQUFBLE1BQ3pEO0FBQ0EsY0FBUSxTQUFTO0FBQ2pCLGVBQVMsTUFBTSxPQUFPO0FBQ2xC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QVMsZ0JBQWdCO0FBQ3JCLFdBQU87QUFBQSxNQUNILE1BQU07QUFBQSxRQUNGLFFBQVE7QUFBQSxVQUNKLE9BQVksRUFBRSxTQUFTLEtBQUs7QUFBQSxVQUM1QixRQUFZLEVBQUUsU0FBUyxNQUFNO0FBQUEsVUFDN0IsWUFBWSxDQUFDLHNCQUFzQixtQkFBbUIsMkJBQTJCO0FBQUEsUUFDckY7QUFBQSxRQUNBLFdBQVcsRUFBRSxTQUFTLEtBQUs7QUFBQSxRQUMzQixZQUFZO0FBQUEsVUFDUixXQUFzQjtBQUFBLFVBQ3RCLHNCQUFzQjtBQUFBO0FBQUEsVUFFdEIsWUFBc0I7QUFBQSxVQUN0QixlQUFlO0FBQUEsWUFDWDtBQUFBLFlBQWE7QUFBQSxZQUFTO0FBQUEsWUFDdEI7QUFBQSxZQUNBO0FBQUEsWUFDQTtBQUFBLFVBQ0o7QUFBQSxVQUNBLGFBQWEsQ0FBQyxRQUFRLFNBQVMsT0FBTyxPQUFPLEVBQUU7QUFBQSxRQUNuRDtBQUFBLFFBQ0EsZUFBZ0IsRUFBRSxTQUFTLEtBQUs7QUFBQSxRQUNoQyxRQUFnQixFQUFFLFNBQVMsS0FBSztBQUFBLFFBQ2hDLGFBQWdCLEVBQUUsaUJBQWlCLE1BQU07QUFBQSxRQUN6QyxZQUFnQixFQUFFLGdCQUFnQixFQUFFLFNBQVMsTUFBTSxFQUFFO0FBQUE7QUFBQTtBQUFBLFFBR3JELG9CQUF3QixFQUFFLFNBQVMsTUFBTTtBQUFBLFFBQ3pDLHlCQUF5QixFQUFFLFNBQVMsTUFBTTtBQUFBLE1BQzlDO0FBQUEsSUFDSjtBQUFBLEVBQ0o7IiwKICAibmFtZXMiOiBbIkVycm9yQ29kZXMiLCAiTWVzc2FnZSIsICJUb3VjaCIsICJEaXNwb3NhYmxlIiwgIlJBTCIsICJFdmVudCIsICJJcyIsICJDYW5jZWxsYXRpb25Ub2tlbiIsICJDYW5jZWxsYXRpb25TdGF0ZSIsICJfY29ubiIsICJJcyIsICJNZXNzYWdlUmVhZGVyIiwgIkFic3RyYWN0TWVzc2FnZVJlYWRlciIsICJSZXNvbHZlZE1lc3NhZ2VSZWFkZXJPcHRpb25zIiwgIklzIiwgIk1lc3NhZ2VXcml0ZXIiLCAiQWJzdHJhY3RNZXNzYWdlV3JpdGVyIiwgIlJlc29sdmVkTWVzc2FnZVdyaXRlck9wdGlvbnMiLCAicmVzdWx0IiwgIklzIiwgIkNhbmNlbE5vdGlmaWNhdGlvbiIsICJQcm9ncmVzc1Rva2VuIiwgIlByb2dyZXNzTm90aWZpY2F0aW9uIiwgIlN0YXJSZXF1ZXN0SGFuZGxlciIsICJUcmFjZSIsICJUcmFjZVZhbHVlcyIsICJUcmFjZUZvcm1hdCIsICJTZXRUcmFjZU5vdGlmaWNhdGlvbiIsICJMb2dUcmFjZU5vdGlmaWNhdGlvbiIsICJDb25uZWN0aW9uRXJyb3JzIiwgIkNvbm5lY3Rpb25TdHJhdGVneSIsICJJZENhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kiLCAiUmVxdWVzdENhbmNlbGxhdGlvblJlY2VpdmVyU3RyYXRlZ3kiLCAiQ2FuY2VsbGF0aW9uUmVjZWl2ZXJTdHJhdGVneSIsICJDYW5jZWxsYXRpb25TZW5kZXJTdHJhdGVneSIsICJDYW5jZWxsYXRpb25TdHJhdGVneSIsICJNZXNzYWdlU3RyYXRlZ3kiLCAiQ29ubmVjdGlvbk9wdGlvbnMiLCAiQ29ubmVjdGlvblN0YXRlIiwgImNyZWF0ZU1lc3NhZ2VDb25uZWN0aW9uIiwgInN0YXJ0VGltZSIsICJSSUwiLCAibSIsICJleHBvcnRzIiwgImNyZWF0ZU1lc3NhZ2VDb25uZWN0aW9uIiwgImltcG9ydF92c2NvZGVfanNvbnJwYyIsICJpbXBvcnRfdnNjb2RlX2pzb25ycGMiLCAiaW1wb3J0X3ZzY29kZV9qc29ucnBjIiwgImltcG9ydF92c2NvZGVfanNvbnJwYyIsICJEb2N1bWVudFVyaSIsICJVUkkiLCAiaW50ZWdlciIsICJ1aW50ZWdlciIsICJQb3NpdGlvbiIsICJSYW5nZSIsICJMb2NhdGlvbiIsICJMb2NhdGlvbkxpbmsiLCAiQ29sb3IiLCAiQ29sb3JJbmZvcm1hdGlvbiIsICJDb2xvclByZXNlbnRhdGlvbiIsICJGb2xkaW5nUmFuZ2VLaW5kIiwgIkZvbGRpbmdSYW5nZSIsICJEaWFnbm9zdGljUmVsYXRlZEluZm9ybWF0aW9uIiwgImxvY2F0aW9uIiwgIkRpYWdub3N0aWNTZXZlcml0eSIsICJEaWFnbm9zdGljVGFnIiwgIkNvZGVEZXNjcmlwdGlvbiIsICJEaWFnbm9zdGljIiwgIkNvbW1hbmQiLCAiVGV4dEVkaXQiLCAiQ2hhbmdlQW5ub3RhdGlvbiIsICJDaGFuZ2VBbm5vdGF0aW9uSWRlbnRpZmllciIsICJBbm5vdGF0ZWRUZXh0RWRpdCIsICJUZXh0RG9jdW1lbnRFZGl0IiwgIkNyZWF0ZUZpbGUiLCAiUmVuYW1lRmlsZSIsICJEZWxldGVGaWxlIiwgIldvcmtzcGFjZUVkaXQiLCAiVGV4dERvY3VtZW50SWRlbnRpZmllciIsICJWZXJzaW9uZWRUZXh0RG9jdW1lbnRJZGVudGlmaWVyIiwgIk9wdGlvbmFsVmVyc2lvbmVkVGV4dERvY3VtZW50SWRlbnRpZmllciIsICJUZXh0RG9jdW1lbnRJdGVtIiwgIk1hcmt1cEtpbmQiLCAiTWFya3VwQ29udGVudCIsICJDb21wbGV0aW9uSXRlbUtpbmQiLCAiSW5zZXJ0VGV4dEZvcm1hdCIsICJDb21wbGV0aW9uSXRlbVRhZyIsICJJbnNlcnRSZXBsYWNlRWRpdCIsICJSYW5nZSIsICJJbnNlcnRUZXh0TW9kZSIsICJDb21wbGV0aW9uSXRlbUxhYmVsRGV0YWlscyIsICJDb21wbGV0aW9uSXRlbSIsICJDb21wbGV0aW9uTGlzdCIsICJNYXJrZWRTdHJpbmciLCAiSG92ZXIiLCAiUGFyYW1ldGVySW5mb3JtYXRpb24iLCAiU2lnbmF0dXJlSW5mb3JtYXRpb24iLCAiRG9jdW1lbnRIaWdobGlnaHRLaW5kIiwgIkRvY3VtZW50SGlnaGxpZ2h0IiwgIlN5bWJvbEtpbmQiLCAiU3ltYm9sVGFnIiwgIlN5bWJvbEluZm9ybWF0aW9uIiwgIldvcmtzcGFjZVN5bWJvbCIsICJEb2N1bWVudFN5bWJvbCIsICJDb2RlQWN0aW9uS2luZCIsICJDb2RlQWN0aW9uVHJpZ2dlcktpbmQiLCAiQ29kZUFjdGlvbkNvbnRleHQiLCAiQ29kZUFjdGlvbiIsICJDb2RlTGVucyIsICJGb3JtYXR0aW5nT3B0aW9ucyIsICJEb2N1bWVudExpbmsiLCAiU2VsZWN0aW9uUmFuZ2UiLCAiU2VtYW50aWNUb2tlblR5cGVzIiwgIlNlbWFudGljVG9rZW5Nb2RpZmllcnMiLCAiU2VtYW50aWNUb2tlbnMiLCAiSW5saW5lVmFsdWVUZXh0IiwgIklubGluZVZhbHVlVmFyaWFibGVMb29rdXAiLCAiSW5saW5lVmFsdWVFdmFsdWF0YWJsZUV4cHJlc3Npb24iLCAiSW5saW5lVmFsdWVDb250ZXh0IiwgIklubGF5SGludEtpbmQiLCAiSW5sYXlIaW50TGFiZWxQYXJ0IiwgIklubGF5SGludCIsICJQb3NpdGlvbiIsICJTdHJpbmdWYWx1ZSIsICJJbmxpbmVDb21wbGV0aW9uSXRlbSIsICJJbmxpbmVDb21wbGV0aW9uTGlzdCIsICJJbmxpbmVDb21wbGV0aW9uVHJpZ2dlcktpbmQiLCAiU2VsZWN0ZWRDb21wbGV0aW9uSW5mbyIsICJJbmxpbmVDb21wbGV0aW9uQ29udGV4dCIsICJXb3Jrc3BhY2VGb2xkZXIiLCAiVGV4dERvY3VtZW50IiwgIlBvc2l0aW9uIiwgIklzIiwgInVuZGVmaW5lZCIsICJpbnRlZ2VyIiwgInVpbnRlZ2VyIiwgIm0iXQp9Cg==
