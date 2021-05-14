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

package io.ballerina.toml.semantic.diagnostics;

import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextRange;

import java.util.Objects;

/**
 * Represents the location of a TOML AST Node.
 *
 * @since 2.0.0
 */
public class TomlNodeLocation implements Location {

    private final LineRange lineRange;
    private final TextRange textRange;

    public TomlNodeLocation(LineRange lineRange, TextRange textRange) {
        this.lineRange = lineRange;
        this.textRange = textRange;
    }

    @Override
    public LineRange lineRange() {
        return lineRange;
    }

    @Override
    public TextRange textRange() {
        return textRange;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TomlNodeLocation that = (TomlNodeLocation) o;
        return Objects.equals(lineRange, that.lineRange) && Objects.equals(textRange, that.textRange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineRange, textRange);
    }
}
