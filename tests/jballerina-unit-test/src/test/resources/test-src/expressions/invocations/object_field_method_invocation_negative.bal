// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

class F {
    function () returns int f = function() returns int => 1;
    function (int) returns int g = function(int i) returns int => i;
    function (int...) returns int k = function(int... r) returns int {
        return r[0];
    };
}

function (int) fk = function(int i) {
};

function extraArg() {
    int i = (new F()).f(1); // too many arguments in call to 'f()'
    F f = new ();
    int iDash = f.g(); //  missing required parameter '' in call to 'g()'
    string iInvalid = f.g(0); // incompatible types: expected 'string', found 'int'
    int argInvalid = f.g("hello"); // incompatible types: expected 'int', found 'string'
    int restSum = f.k();
    restSum = f.k("hi"); //incompatible types: expected 'int', found 'string'
}
