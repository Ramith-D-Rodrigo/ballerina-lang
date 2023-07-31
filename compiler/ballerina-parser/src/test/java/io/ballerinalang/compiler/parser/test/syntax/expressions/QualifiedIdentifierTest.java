/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerinalang.compiler.parser.test.syntax.expressions;

import org.testng.annotations.Test;

/**
 * Test parsing qualified identifiers.
 */
public class QualifiedIdentifierTest extends AbstractExpressionsTest {

    // Valid syntax

    @Test
    public void testVarRef() {
        test("pkg:foo", "qualified-identifier/qualified_identifier_assert_01.json");
    }

    @Test
    public void testFuncCall() {
        test("pkg:foo()", "qualified-identifier/qualified_identifier_assert_02.json");
    }

    // Recovery tests

    @Test
    public void testThreeLevelsOfQualIdentifiers() {
        test("pkg:foo:bar", "qualified-identifier/qualified_identifier_assert_03.json");
    }

    @Test
    public void testThreeLevelsOfQualIdentifiersInFuncCall() {
        test("pkg:foo:bar()", "qualified-identifier/qualified_identifier_assert_04.json");
    }

    @Test
    public void testAdditionalColonsInFuncCall() {
        test("pkg::::bar()", "qualified-identifier/qualified_identifier_assert_05.json");
    }

    @Test
    public void testAdditionalColons() {
        test("pkg::::bar", "qualified-identifier/qualified_identifier_assert_06.json");
    }

    @Test
    public void testIncompleteQualifiedIdent() {
        test("pkg:", "qualified-identifier/qualified_identifier_assert_07.json");
    }

    @Test
    public void testInterveningWSNotAllowed() {
        testFile("qualified-identifier/qualified_identifier_assert_08.bal", 
                "qualified-identifier/qualified_identifier_assert_08.json");
    }

    @Test
    public void testMapTypeAfterColon() {
        testFile("qualified-identifier/qualified_identifier_assert_09.bal",
                "qualified-identifier/qualified_identifier_assert_09.json");
    }
}
