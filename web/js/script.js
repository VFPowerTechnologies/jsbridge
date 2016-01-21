dispatcher = new Dispatcher();
sampleService = new SampleService()

sampleService.getValue().then(function (value) {
    console.log("Got value: " + value);
}).catch(function (exc) {
    console.log("Got error: " + exc);
});

sampleService.setValue(1).then(function () {
    console.log("Value successfully set");
}).catch(function (exc) {
    console.log("Error setting value");
});

sampleService.addListener(function (v) {
    console.log("Listener got value: " + v);
}).then(function () {
    console.log("Listener added");
}).catch(function (exc) {
    console.log("Failed to add listener: " + exc);
});

jsService = {
    syncFn: function (v, n, resolve, reject) {
        console.log("syncFn(" + v + ")");
        resolve({"p": v.q+n, "q" : v.q});
    },
    asyncFn: function (resolve, reject) {
    }
}
