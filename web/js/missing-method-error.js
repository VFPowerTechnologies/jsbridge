function MissingMethodError(message, type, stacktrace) {
    this.name = 'MissingMethodError';
    this.message = message;
}
MissingMethodError.prototype = Object.create(Error.prototype);
MissingMethodError.prototype.constructor = MissingMethodError;

module.exports = MissingMethodError;
