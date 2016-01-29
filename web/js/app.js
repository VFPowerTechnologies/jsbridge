var Dispatcher = require('jsbridge').Dispatcher;
var TestService = require('./jvm-service/test-service.js').TestService;

window.dispatcher = new Dispatcher(true);
window.testService = new TestService();
window.jsTestService = require('./service/js-test-service').getService();
