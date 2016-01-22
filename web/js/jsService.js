jsService = {
    syncFn: function (v, n, resolve, reject) {
        console.log("syncFn(" + v + ")");
        resolve({"p": v.q+n, "q" : v.q});
    },
    noArgsFn: function (resolve, reject) {
        console.log("noArgsFn()");
        resolve();
    },
    asyncFn: function (resolve, reject) {
    }
}
