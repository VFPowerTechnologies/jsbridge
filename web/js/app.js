var Dispatcher = require('./dispatcher').Dispatcher;
var SampleService = require('./jvm-service/sample-service.js').SampleService;

window.dispatcher = new Dispatcher();
window.sampleService = new SampleService();
window.jsService = require('./service/js-service').getService();

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
