//TODO
//We need to map each java type we send back to a js class from json

//TODO
//calls reject with the deserialized value of exc
function deserializeException(exc, reject) {
};

//XXX might be better to keep separate maps for callbacks and listeners? no need to verify ids before putting them in, etc
function Dispatcher() {
    //TODO replace with guid (or just check to make sure there's not already such a
    //registered callback or pick a diff number (for handling overflows)
    this._nextCallbackId = 0;
    this._callbacks = [];
};

//TODO make sure methodArgs is an array, or null for no args
Dispatcher.prototype.call = function (service, methodName, methodArgs, resolve, reject) {
    var callbackId = this._getNextCallbackId();
    this._callbacks[callbackId] = [resolve, reject, true];

    //TODO this needs to be different for each platform
    //so if ios is detected (window.webkit.dispatchers isn't null), do postMessage instead of call()
    nativeDispatcher.call(service, methodName, methodArgs, callbackId);
};

Dispatcher.prototype.callFromNative = function (targetStr, methodName, methodArgs, callbackId) {
    var args = methodArgs;
    console.log(targetStr + "." + methodName + "(" + args + ") (" + callbackId + ")");
    //TODO if this doesn't exist, reject with an error
    var target = eval(targetStr);

    //TODO maybe have variants for sync and async fns
    //for sync, just run the fun and send data back
    var resolve = function (v) {
        if (v === undefined)
            v = null;
        nativeDispatcher.callbackFromJS(callbackId, false, JSON.stringify(v));
    };
    args.push(resolve);

    var reject = function (v) {
        if (v === undefined)
            v = null;
        nativeDispatcher.callbackFromJS(callbackId, true, JSON.stringify(v));
    };
    args.push(reject);

    //TODO maybe replace eval with a fn, which might be faster
    //TODO if this doesn't exist, reject with an error
    var fn = target[methodName];

    //TODO if this throws, reject
    fn.apply(target, args);
}

Dispatcher.prototype.sendValue = function (callbackId, isError, value) {
    console.log("Received " + value + " for callbackId=" + callbackId)

    //TODO if a listener, reject can be null, so maybe check and emit a warning?
    var r = this._callbacks[callbackId];
    var resolve = r[0];
    var reject = r[1];
    var remove = r[2];
    if (remove)
        delete this._callbacks[callbackId];

    if (isError)
        reject(value);
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
//TODO make sure this isn't also used on OSX when using jfx
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
