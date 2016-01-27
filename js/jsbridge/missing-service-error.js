function MissingServiceError(message, type, stacktrace) {
    this.name = 'MissingServiceError';
    this.message = message;
}
MissingServiceError.prototype = Object.create(Error.prototype);
MissingServiceError.prototype.constructor = MissingServiceError;

module.exports = MissingServiceError;
