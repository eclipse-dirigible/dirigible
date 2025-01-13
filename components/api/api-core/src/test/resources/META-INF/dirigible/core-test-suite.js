
var QUnit = require("qunit/qunit");

QUnit.module('core:');
QUnit.test("configurations", function (assert) {
    assert.ok(require('core-tests/configurations-get-test'), "get - ok");
});
QUnit.test("context", function (assert) {
    assert.ok(require('core-tests/context-get-test'), "get - ok");
});
QUnit.test("env", function (assert) {
    assert.ok(require('core-tests/env-get-test'), "get - ok");
    assert.ok(require('core-tests/env-list-test'), "list - ok");
});
QUnit.test("globals", function (assert) {
    assert.ok(require('core-tests/globals-get-test'), "get - ok");
    assert.ok(require('core-tests/globals-list-test'), "list - ok");
});
QUnit.test("destinations", function (assert) {
    assert.ok(require('core-tests/destinations-get-test'), "get - ok");
});

require("qunit/runner").run();