var jsService = {
    syncFn: function (v, n, resolve, reject) {
        console.log("syncFn(" + v + ")");
        resolve({"p": v.q+n, "q" : v.q});
    },
    noArgsFn: function (resolve, reject) {
        console.log("noArgsFn()");
        resolve();
    },
    rejects: function (resolve, reject) {
        reject(new Error('reject js error'));
    },
    throwsError: function (resolve, reject) {
        throw new Error('thrown js error');
    },
    asyncFn: function (resolve, reject) {
    }
};

module.exports = {
    getService: function () {
        return jsService;
    }
};
