/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package io.ballerina.semantic.api.test.symbols;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.PathParameterSymbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.resourcepath.util.PathSegment;
import io.ballerina.projects.Document;
import io.ballerina.projects.Project;
import org.ballerinalang.test.BCompileUtil;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static io.ballerina.semantic.api.test.util.SemanticAPITestUtils.assertBasicsAndGetSymbol;
import static io.ballerina.semantic.api.test.util.SemanticAPITestUtils.assertList;
import static io.ballerina.semantic.api.test.util.SemanticAPITestUtils.getDefaultModulesSemanticModel;
import static io.ballerina.semantic.api.test.util.SemanticAPITestUtils.getDocumentForSingleSource;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Test cases for symbols in resource methods.
 *
 * @since 2.0.0
 */
public class SymbolsInResourceMethodsTest {

    private SemanticModel model;
    private Document srcFile;

    @BeforeClass
    public void setup() {
        Project project = BCompileUtil.loadProject("test-src/symbols/resource_method_symbols.bal");
        model = getDefaultModulesSemanticModel(project);
        srcFile = getDocumentForSingleSource(project);
    }

    @Test(dataProvider = "ResourceMethodPosProvider")
    public void testSymbolsInResourceMethod(int line, int col, String name, SymbolKind kind) {
        assertBasicsAndGetSymbol(model, srcFile, line, col, name, kind);
    }

    @DataProvider(name = "ResourceMethodPosProvider")
    public Object[][] getSymbolPos() {
        return new Object[][]{
                {24, 23, "get", SymbolKind.RESOURCE_METHOD},
                {24, 26, "authors", SymbolKind.PATH_SEGMENT},
                {24, 45, "names", SymbolKind.PATH_PARAMETER},
                {32, 52, "isbn", SymbolKind.PATH_PARAMETER},
                {32, 68, "Book", SymbolKind.TYPE},

                // TODO: The following tests need to be verified again
                {36, 27, "$^", SymbolKind.PATH_PARAMETER},
                {40, 38, "$^^", SymbolKind.PATH_PARAMETER},

        };
    }

    @Test(dataProvider = "PathParamPosProvider")
    public void testPathParamSymbols(int line, int col, String name, PathSegment.Kind pathKind, TypeDescKind typekind) {
        PathParameterSymbol symbol = (PathParameterSymbol) assertBasicsAndGetSymbol(model, srcFile, line, col,
                                                                                    name, SymbolKind.PATH_PARAMETER);
        assertEquals(symbol.pathSegmentKind(), pathKind);

        TypeSymbol typeDescriptor = symbol.typeDescriptor();
        assertEquals(typeDescriptor.typeKind(), typekind);
    }

    @DataProvider(name = "PathParamPosProvider")
    public Object[][] getPathParamPos() {
        return new Object[][]{
                {24, 45, "names", PathSegment.Kind.PATH_REST_PARAMETER, TypeDescKind.ARRAY},
                {32, 52, "isbn", PathSegment.Kind.PATH_PARAMETER, TypeDescKind.UNION},
                {36, 48, "author", PathSegment.Kind.PATH_PARAMETER, TypeDescKind.STRING},
                {36, 61, "bookId", PathSegment.Kind.PATH_PARAMETER, TypeDescKind.INT},
        };
    }
}
