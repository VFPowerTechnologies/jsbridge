function JavaError(message, type, stacktrace) {
    this.name = 'JavaError';
    this.message = message;
    this.type = type;
    this.stacktrace = stacktrace;
}
JavaError.prototype = Object.create(Error.prototype);
JavaError.prototype.constructor = JavaError;
JavaError.prototype.toString = function () {
    return 'JavaError(' + this.type + '): ' + this.message;
};

module.exports = JavaError;
