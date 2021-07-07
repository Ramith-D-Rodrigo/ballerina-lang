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

import configStructuredTypes.imported_records;
import configStructuredTypes.open_records;
import configStructuredTypes.util;
import configStructuredTypes.default_value_records as def_records;

public function main() {
    testRecords();
    testTables();
    testArrays();
    testMaps();
    testComplexRecords();

    imported_records:testRecords();
    imported_records:testTables();
    imported_records:testArrays();
    imported_records:testMaps();

    open_records:testOpenRecords();
    open_records:testRestFields();

    def_records:testDefaultValues();
    
    util:print("Tests passed");
}
