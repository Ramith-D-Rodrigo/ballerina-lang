// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

int i;
string s;
int a;

const ASSERTION_ERROR_REASON = "AssertionError";

function init() {
    i = 10;
    s = "Test string";
    int x = 2;
    a = x + 10;
}

function testGlobalVarInitialization() {
    if (i == 10 && s == "Test string" && a == 12) {
        return;
    }

    panic error(ASSERTION_ERROR_REASON, message = "expected 'true', found 'false'");
}
