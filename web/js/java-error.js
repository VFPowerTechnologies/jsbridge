function JavaError(message, type, stacktrace) {
    this.message = message;
    this.type = type;
    this.stacktrace = stacktrace;
}
JavaError.prototype = Object.create(Error.prototype);
JavaError.prototype.constructor = JavaError;

module.exports = JavaError;
