var assert = chai.assert;

//load dispatcher & co
require('./app.js');

var JavaError = require('./dispatcher').JavaError;

describe('TestService', function () {
    beforeEach(function () {
        return testService.resetState();
    });

    /* Listener functions */
    describe('addListener', function () {
        it('should successfully register a listener', function () {
            //first we register a listener, then we call all registered listeners
            //then we use the callback to fufill the promise (or reject/timeout on error)
            return new Promise(function (resolve, reject) {
                function cb(value) {
                    resolve(value);
                }

                return testService.addListener(cb).then(function () {
                    return testService.callListeners(5);
                }).catch(function (e) {
                    reject(e);
                });
            });
        });
    });

    /* Sync functions */

    describe('syncAdd', function () {
        it('should work when provided with proper values', function () {
            return assert.becomes(testService.syncAdd(2, 3), 5);
        });

        it('should fail when given invalid values', function () {
            return assert.isRejected(testService.syncAdd('a', 'b'), JavaError);
        });

        it('should fail when missing arguments', function () {
            assert.throws(function () { testService.syncAdd(1); }, /Invalid number of arguments/);
        });
    });

    describe('syncThrow', function () {
        it('should fail on call', function () {
            return assert.isRejected(testService.syncThrow(), JavaError);
        });
    });

    describe('syncVoid', function () {
        it('should complete successfully when called', function () {
            return assert.isFulfilled(testService.syncVoid());
        });
    });

    describe('getValue', function () {
        it('should return 0', function () {
            return assert.becomes(testService.getValue(), 0);
        });
    });

    describe('setValue', function () {
        it('should set the value to 1', function() {
            var promise = testService.setValue(1).then(function (v) {
                return testService.getValue();
            });

            return assert.becomes(promise, 1);
        });
    });

    /* Async functions */

    describe('asyncAdd', function () {
        it('should work when promised with valid values', function () {
            return assert.becomes(testService.asyncAdd(2, 3), 5);
        });
    });

    describe('asyncThrow', function () {
        it('should fail on call', function () {
            return assert.isRejected(testService.syncThrow(), JavaError);
        });
    });

    describe('asyncVoid', function () {
        it('should complete successfully when called', function () {
            return assert.isFulfilled(testService.asyncVoid());
        });
    });
});
