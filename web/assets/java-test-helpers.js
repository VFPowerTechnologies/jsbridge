//div[id=mocha] > ul[id=mocha-report] > suites*
//li[class=suite] > h1{name} > items*
//suites can be nested
//li[class=test pass fail"] > h2{desc} + pre[class=error]

var _suites = {};

function clearSuites() {
    for (var k in _suites) {
        var li = _suites[k];
        li.parentNode.removeChild(li);
        delete _suites[k];
    }
}

//initialSuite is a hack to create a toplevel suite
function addSuite(suiteName, initialSuite) {
    //ignore if suite already exists
    if (!initialSuite && _suites[suiteName] !== undefined)
        return;

    if (initialSuite)
        var reports = document.getElementById('mocha-report');
    else
        var reports = document.getElementById('java-report');

    var li = document.createElement('li');
    li.setAttribute('class', 'suite');
    if (initialSuite)
        li.setAttribute('id', 'java-report');

    var h1 = document.createElement('h1');
    h1.textContent = suiteName;
    li.appendChild(h1);

    reports.appendChild(li);
    if (!initialSuite)
        _suites[suiteName] = li;
    return li;
}

function addResult(testResult) {
    var suiteList = _suites[testResult.suiteName];
    if (suiteList === undefined)
        suiteList = addSuite(testResult.suiteName);

    var li = document.createElement('li');

    var h2 = document.createElement('h2');
    h2.textContent = testResult.testName;
    li.appendChild(h2);

    var classes = 'test ' + (testResult.passed ? 'pass' : 'fail');
    li.setAttribute('class', classes);

    var e = testResult.exception;
    if (!testResult.pass && e) {
        e = JSON.parse(e);
        var pre = document.createElement('pre');
        pre.setAttribute('class', 'error');
        if (e.message)
            pre.textContent += e.message + "\n\n";
        pre.textContent += e.stacktrace;
        li.appendChild(pre);
    }

    suiteList.appendChild(li);
}

addSuite('Java Tests', true);

//addSuite('Something');
//addResult({
//    suiteName: 'Something',
//    testName: 'Some test',
//    passed: true,
//    exception: null
//});

//addSuite('Something2');
//addResult({
//    suiteName: 'Something2',
//    testName: 'Some test',
//    passed: false,
//    exception: 'some exception'
//});

//addResult({
//    suiteName: 'Something2',
//    testName: 'Some test 2',
//    passed: true,
//    exception: null
//});
