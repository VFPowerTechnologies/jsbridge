var Dispatcher = require('./dispatcher').Dispatcher;
var SampleService = require('./jvm-service/sample-service.js').SampleService;

window.dispatcher = new Dispatcher();
window.sampleService = new SampleService();
window.jsService = require('./service/js-service').getService();

sampleService.getValue().then(function (value) {
    console.log("Got value: " + value);
}).catch(function (exc) {
    console.error("Got error: " + exc);
});

sampleService.setValue(1).then(function () {
    console.log("Value successfully set");
}).catch(function (exc) {
    console.error("Error setting value");
});

sampleService.addListener(function (v) {
    console.log("Listener got value: " + v);
}).then(function () {
    console.log("Listener added");
}).catch(function (exc) {
    console.error("Failed to add listener: " + exc);
    console.error('Stacktrace:\n' + exc.stacktrace);
});

document.getElementById('throwJavaBtn').addEventListener('click', function (ev) {
    console.log('Calling throwing function');
    sampleService.throwException().then(function () {
        console.log("Didn't receive exception");
    }).catch(function (exc) {
        console.error('Received java exception: ' + exc);
    })
}, false);

document.getElementById('asyncJavaBtn').addEventListener('click', function (ev) {
    console.log('Calling async function');
    sampleService.asyncMethod(2, 3).then(function (v) {
        console.log('async method completed: ' + v);
    }).catch(function (exc) {
        console.error('async method failed: ' + exc);
    });
}, false);

document.getElementById('asyncVoidJavaBtn').addEventListener('click', function (ev) {
    console.log('Calling async void function');
    sampleService.asyncVoidMethod(2).then(function (v) {
        console.log('async void method completed: ' + v);
    }).catch(function (exc) {
        console.error('async void method failed: ' + exc);
    });
}, false);

document.getElementById('asyncThrowJavaBtn').addEventListener('click', function (ev) {
    console.log('Calling async throw function');
    sampleService.asyncThrow(2).then(function (v) {
        console.log('async throw method completed: ' + v);
    }).catch(function (exc) {
        console.error('async throw method failed: ' + exc);
    });
}, false);
