// Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/test;

function trapInsideFunctionArg() {

    function () fn = function () {
    };

    function () panicfn = function () {
        panic error("panic inside function");
    };

    errFn(fn());
    errFn(trap fn());
    errFn(trap panicfn());
    error? errFnResult = trap errFn(panicfn());

    test:assertTrue(errFnResult is error);
    if (errFnResult is error) {
        test:assertEquals("panic inside function", errFnResult.message());
    }
}

function errFn(error? res) {
}

int loopTrapCounter = 0;

function testTrapInsideForLoop() {
    foreach int i in 0 ..< 10 {
        error? unionResult = trap func();
    }
    test:assertEquals(loopTrapCounter, 10);
}

function func() {
    loopTrapCounter += 1;
    panic error("err");
}
