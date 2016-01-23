var jsService = {
    syncFn: function (v, n, resolve, reject) {
        console.log("syncFn(" + v + ")");
        resolve({"p": v.q+n, "q" : v.q});
    },
    noArgsFn: function (resolve, reject) {
        console.log("noArgsFn()");
        resolve();
    },
    throwError: function (resolve, reject) {
        reject(new Error('js error'));
    },
    asyncFn: function (resolve, reject) {
    }
};

module.exports = {
    getService: function () {
        return jsService;
    }
};
