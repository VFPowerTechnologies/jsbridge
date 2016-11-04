var JavaError = require('./java-error');
var MissingMethodError = require('./missing-method-error');
var MissingServiceError = require('./missing-service-error');

function deserializeJavaException(repr) {
    return new JavaError(
        repr.message,
        repr.type,
        repr.stacktrace);
};

//XXX might be better to keep separate maps for callbacks and listeners? no need to verify ids before putting them in, etc
function Dispatcher(debug) {
    //TODO replace with guid (or just check to make sure there's not already such a
    //registered callback or pick a diff number (for handling overflows)
    this._nextCallbackId = 0;
    this._callbacks = [];
    this.debug = !!debug
};

Dispatcher.prototype._logInfo = function (msg) {
    if (!this.debug)
        return;
    console.log(msg);
};

//TODO make sure methodArgs is an array, or null for no args
Dispatcher.prototype.call = function (service, methodName, methodArgs, resolve, reject) {
    var callbackId = this._getNextCallbackId();
    this._callbacks[callbackId] = [resolve, reject, true];

    this._logInfo('js->native: ' + service + '.' + methodName + '(' + methodArgs + ')');

    window.nativeDispatcher.call(service, methodName, methodArgs, callbackId);
};

Dispatcher.prototype.handleCallFromNative = function (serviceName, methodName, methodArgs, callbackId) {
    var args = methodArgs;
    this._logInfo(serviceName + "." + methodName + "(" + args + ") (" + callbackId + ")");

    //TODO maybe have variants for sync and async fns
    //for sync, just run the fun and send data back
    var resolve = function (v) {
        if (v === undefined)
            v = null;
        window.nativeDispatcher.callbackFromJS(callbackId, false, JSON.stringify(v));
    };
    args.push(resolve);

    var reject = function (v) {
        if (!(v instanceof Error))
            throw new Error("An Error instance must be provided to reject");

        var repr = {
            message: v.message,
            type: v.name
        };

        window.nativeDispatcher.callbackFromJS(callbackId, true, JSON.stringify(repr));
    };
    args.push(reject);

    var target = eval(serviceName);
    if (target === undefined) {
        reject(new MissingServiceError(serviceName));
        return;
    }

    var fn = target[methodName];
    if (fn === undefined) {
        reject(new MissingMethodError(methodName));
        return;
    }

    try {
        fn.apply(target, args);
    }
    catch (e) {
        reject(e);
    }
}

Dispatcher.prototype.sendValueToCallback = function (callbackId, isError, value) {
    this._logInfo("Received " + value + " for callbackId=" + callbackId)

    //TODO if a listener, reject can be null, so maybe check and emit a warning?
    var r = this._callbacks[callbackId];
    if (!r) {
        console.error('Unable to send value as callbackId=' + callbackId + ' no longer exists');
        return;
    }
    var resolve = r[0];
    var reject = r[1];
    var remove = r[2];
    if (remove)
        delete this._callbacks[callbackId];

    if (isError)
        reject(deserializeJavaException(value));
    else
        resolve(value);
};

Dispatcher.prototype.createListener = function (callback) {
    var callbackId = this._getNextCallbackId();
    this._callbacks[callbackId] = [callback, null, false];
    return callbackId;
};

Dispatcher.prototype._getNextCallbackId = function () {
    var n = this._nextCallbackId;
    ++this._nextCallbackId;
    //for simplicity (and not having to cast back later)
    return n.toString();
};

//IOS Webview
if (window.nativeDispatcher === undefined) {
    if (window.webkit.messageHandlers !== undefined) {
        window.nativeDispatcher = {
            call: function (serviceName, methodName, methodArgs, callbackId) {
                window.webkit.messageHandlers.call.postMessage([serviceName, methodName, methodArgs, callbackId]);
            },
            callbackFromJS: function (callbackId, isError, jsonRetVal) {
                window.webkit.messageHandlers.callbackFromJS.postMessage([callbackId, isError, jsonRetVal]);
            }
        };
    }
    else
        console.log('Unsupported platform');
}

module.exports = {
    Dispatcher: Dispatcher
};
