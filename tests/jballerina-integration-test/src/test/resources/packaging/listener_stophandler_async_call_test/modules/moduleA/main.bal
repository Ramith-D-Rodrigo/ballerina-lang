// Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/jballerina.java;
import ballerina/lang.runtime;

public function stopHandlerFunc1() returns error? {
    runtime:sleep(1);
    println("calling StopHandler1 of moduleA");
}

public function stopHandlerFunc2() returns error? {
    runtime:sleep(1);
    println("calling StopHandler2 of moduleA");
}

function init() {
    runtime:onGracefulStop(stopHandlerFunc1);
    runtime:onGracefulStop(stopHandlerFunc2);
}

public class Listener1 {

    *runtime:DynamicListener;
    private string name = "";
    public function init(string name) {
        self.name = name;
    }

    public function 'start() returns error? {
    }

    public function gracefulStop() returns error? {
        runtime:sleep(2);
        println("calling gracefulStop for " + self.name);
    }

    public function immediateStop() returns error? {
    }

    public function attach(service object {} s, string[]|string? name = ()) returns error? {
    }

    public function detach(service object {} s) returns error? {
    }
}

public class Listener2 {

    *runtime:DynamicListener;
    private string name = "";
    public function init(string name) {
        self.name = name;
    }

    public function 'start() returns error? {
    }

    public function gracefulStop() returns error? {
        runtime:sleep(2);
    }

    public function immediateStop() returns error? {
    }

    public function attach(service object {} s, string[]|string? name = ()) returns error? {
    }

    public function detach(service object {} s) returns error? {
    }
}

listener Listener1 listener1 = new Listener1("static Listener1 of ModuleA");

listener listener2 = new Listener2("static Listener2 of ModuleA");

public function main() {
    final Listener1 dynListener1 = new Listener1("dynamic Listener1 of ModuleA");
    final Listener2 dynListener2 = new Listener2("dynamic Listener2 of ModuleA");
    runtime:registerListener(dynListener1);
    runtime:registerListener(dynListener2);
    runtime:sleep(2);
    runtime:deregisterListener(dynListener1);
    runtime:deregisterListener(dynListener2);
}

public function println(string value) {
    handle strValue = java:fromString(value);
    handle stdout1 = stdout();
    printlnInternal(stdout1, strValue);
}
public function stdout() returns handle = @java:FieldGet {
    name: "out",
    'class: "java/lang/System"
} external;

public function printlnInternal(handle receiver, handle strValue)  = @java:Method {
    name: "println",
    'class: "java/io/PrintStream",
    paramTypes: ["java.lang.String"]
} external;
