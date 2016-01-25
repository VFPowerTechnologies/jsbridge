var Dispatcher = require('./dispatcher').Dispatcher;
var TestService = require('./jvm-service/test-service.js').TestService;

window.dispatcher = new Dispatcher();
window.testService = new TestService();
window.jsService = require('./service/js-service').getService();
