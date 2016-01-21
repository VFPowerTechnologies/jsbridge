function SampleService() {
};

SampleService.prototype.getValue = function () {
    return new Promise(function (resolve, reject) {
        function deserializeValue(value) {
            var v = JSON.parse(value);
            //TODO assert is int
            resolve(v);
        }
        //TODO deserialize exception
        window.dispatcher.call("SampleService", "getValue", JSON.stringify([]), deserializeValue, reject)
    });
};

SampleService.prototype.setValue = function (value) {
    return new Promise(function (resolve, reject) {
        //for functions with no return values
        window.dispatcher.call("SampleService", "setValue", JSON.stringify([value]), resolve, reject)
    });
};

//TODO need to somehow be able to get the proper id to unregister
//need to keep a reverse map of callback -> id in the dispatcher
SampleService.prototype.addListener = function (callback) {
    return new Promise(function (resolve, reject) {
        //for functions with a callback
        window.dispatcher.call("SampleService", "addListener", JSON.stringify([dispatcher.createListener(callback)]), resolve, reject)
    });
};

