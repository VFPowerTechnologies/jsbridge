var jsTestService = {
    hasArgs: function (v, n, resolve, reject) {
        console.log("hasArgs(" + v + ")");
        resolve({p: v.q+n, q: v.q});
    },
    noArgs: function (resolve, reject) {
        console.log("noArgs()");
        resolve();
    },
    rejectsPromise: function (resolve, reject) {
        reject(new Error('reject js error'));
    },
    throwsError: function (resolve, reject) {
        throw new Error('thrown js error');
    }
};

module.exports = {
    getService: function () {
        return jsTestService;
    }
};
