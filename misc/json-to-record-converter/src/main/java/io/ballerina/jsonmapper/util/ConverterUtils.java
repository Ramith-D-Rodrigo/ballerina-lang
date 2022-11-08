/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.jsonmapper.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.ArrayTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ParenthesisedTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SyntaxInfo;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.UnionTypeDescriptorNode;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectException;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.directory.SingleFileProject;
import io.ballerina.projects.util.ProjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.ballerina.identifier.Utils.escapeSpecialCharacters;
import static io.ballerina.identifier.Utils.unescapeUnicodeCodepoints;

/**
 * Util methods for JSON to record direct converter.
 *
 * @since 2201.2.0
 */
public final class ConverterUtils {

    private ConverterUtils() {}

    private static final String ARRAY_RECORD_SUFFIX = "Item";
    private static final String QUOTED_IDENTIFIER_PREFIX = "'";
    private static final String ESCAPE_NUMERIC_PATTERN = "\\b\\d.*";
    private static final List<String> KEYWORDS = SyntaxInfo.keywords();

    /**
     * This method returns the identifiers with special characters.
     *
     * @param identifier Identifier name.
     * @return {@link String} Special characters escaped identifier.
     */
    public static String escapeIdentifier(String identifier) {
        if (KEYWORDS.stream().anyMatch(identifier::equals)) {
            return "'" + identifier;
        } else {
            if (identifier.startsWith(QUOTED_IDENTIFIER_PREFIX)) {
                identifier = identifier.substring(1);
            }
            identifier = unescapeUnicodeCodepoints(identifier);
            // TODO: Escape Special Character does not escapes backslashes.
            //  Refer - https://github.com/ballerina-platform/ballerina-lang/issues/36912
            identifier = escapeSpecialCharacters(identifier);
            if (identifier.matches(ESCAPE_NUMERIC_PATTERN)) {
                identifier = "\\" + identifier;
            }
            return identifier;
        }
    }

    public static List<String> getExistingTypeNames(Path filePath) {
        List<String> existingTypeNames = new ArrayList<>();
        if (filePath == null) {
            return existingTypeNames;
        }

        Project project;
        // Check if the provided file is a SingleFileProject
        try {
            project = SingleFileProject.load(filePath);
            List<Symbol> moduleSymbols =
                    project.currentPackage().getDefaultModule().getCompilation().getSemanticModel().moduleSymbols();
            moduleSymbols.forEach(symbol -> {
                if (symbol.getName().isPresent()) {
                    existingTypeNames.add(symbol.getName().get());
                }
            });
        } catch (ProjectException pe) {
            // Check if the provided file is a part of BuildProject
            Path projectRoot = ProjectUtils.findProjectRoot(filePath);
            if (projectRoot != null) {
                try {
                    project = BuildProject.load(projectRoot);
                    List<Symbol> moduleSymbols = project.currentPackage().module(project.documentId(filePath).moduleId())
                            .getCompilation().getSemanticModel().moduleSymbols();
                    moduleSymbols.forEach(symbol -> {
                        if (symbol.getName().isPresent()) {
                            existingTypeNames.add(symbol.getName().get());
                        }
                    });
                } catch (ProjectException pe1) {
                    return existingTypeNames;
                }
            }
        }
        return existingTypeNames;
    }

    public static JsonObject modifyJsonObjectWithUpdatedFieldNames(JsonObject jsonObject,
                                                                   List<String> existingFieldNames,
                                                                   Map<String, String> updatedFieldNames) {
        Set<Map.Entry<String, JsonElement>> jsonObjectEntries = jsonObject.deepCopy().entrySet();
        for (Map.Entry<String, JsonElement> entry : jsonObjectEntries) {
            if (entry.getValue().isJsonObject()) {
                JsonObject updatedJsonObject =
                        modifyJsonObjectWithUpdatedFieldNames(jsonObject.remove(entry.getKey()).getAsJsonObject(),
                                existingFieldNames, updatedFieldNames);
                jsonObject.add(entry.getKey(), updatedJsonObject);

                String fieldName = StringUtils.capitalize(entry.getKey());
                if (existingFieldNames.contains(fieldName)) {
                    String updatedFieldName = updatedFieldNames.containsKey(fieldName) ?
                            updatedFieldNames.get(fieldName) : getUpdatedFieldName(fieldName, existingFieldNames);
                    updatedFieldNames.put(fieldName, updatedFieldName);
                    JsonElement removedJsonObject = jsonObject.remove(entry.getKey());
                    jsonObject.add(fieldName.equals(entry.getKey()) ? updatedFieldName :
                            StringUtils.uncapitalize(updatedFieldName) , removedJsonObject);
                }

            } else if (entry.getValue().isJsonArray()) {
                for (JsonElement element : entry.getValue().getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        JsonObject updatedJsonObject =
                                modifyJsonObjectWithUpdatedFieldNames(jsonObject.remove(entry.getKey()).getAsJsonObject(),
                                        existingFieldNames, updatedFieldNames);
                        jsonObject.add(entry.getKey(), updatedJsonObject);
                    }
                }
                String arrayItemFieldName = StringUtils.capitalize(entry.getKey()) + ARRAY_RECORD_SUFFIX;
                if (existingFieldNames.contains(arrayItemFieldName)) {
                    String updatedFieldName = updatedFieldNames.containsKey(arrayItemFieldName) ?
                            updatedFieldNames.get(arrayItemFieldName) :
                            getUpdatedFieldName(arrayItemFieldName, existingFieldNames);
                    String updatedArrayItemFieldName = StringUtils.capitalize(entry.getKey()) +
                            updatedFieldName.substring(arrayItemFieldName.length());
                    updatedFieldNames.put(arrayItemFieldName, updatedArrayItemFieldName + ARRAY_RECORD_SUFFIX);
                    JsonElement removedJsonArray = jsonObject.remove(entry.getKey());
                    jsonObject.add(StringUtils.capitalize(entry.getKey()).equals(entry.getKey()) ? updatedArrayItemFieldName :
                            StringUtils.uncapitalize(updatedArrayItemFieldName) , removedJsonArray);
                }
            }
            jsonObject.add(entry.getKey(), jsonObject.remove(entry.getKey()));
        }

        return jsonObject;
    }

    /**
     * This method returns the SyntaxToken corresponding to the JsonPrimitive.
     *
     * @param value JsonPrimitive that has to be classified.
     * @return {@link Token} Classified Syntax Token.
     */
    public static Token getPrimitiveTypeName(JsonPrimitive value) {
        if (value.isString()) {
            return AbstractNodeFactory.createToken(SyntaxKind.STRING_KEYWORD);
        } else if (value.isBoolean()) {
            return AbstractNodeFactory.createToken(SyntaxKind.BOOLEAN_KEYWORD);
        } else if (value.isNumber()) {
            String strValue = value.getAsNumber().toString();
            if (strValue.contains(".")) {
                return AbstractNodeFactory.createToken(SyntaxKind.DECIMAL_KEYWORD);
            } else {
                return AbstractNodeFactory.createToken(SyntaxKind.INT_KEYWORD);
            }
        }
        return AbstractNodeFactory.createToken(SyntaxKind.ANYDATA_KEYWORD);
    }

    /**
     * This method extracts TypeDescriptorNodes within any UnionTypeDescriptorNodes or ParenthesisedTypeDescriptorNode.
     *
     * @param typeDescNodes List of Union and Parenthesised TypeDescriptorNodes
     * @return {@link List<TypeDescriptorNode>} Extracted SimpleNameReferenceNodes.
     */
    public static List<TypeDescriptorNode> extractTypeDescriptorNodes(List<TypeDescriptorNode> typeDescNodes) {
        List<TypeDescriptorNode> extractedTypeNames = new ArrayList<>();
        for (TypeDescriptorNode typeDescNode : typeDescNodes) {
            TypeDescriptorNode extractedTypeDescNode = extractParenthesisedTypeDescNode(typeDescNode);
            if (extractedTypeDescNode instanceof UnionTypeDescriptorNode) {
                List<TypeDescriptorNode> childTypeDescNodes =
                        List.of(((UnionTypeDescriptorNode) extractedTypeDescNode).leftTypeDesc(),
                                ((UnionTypeDescriptorNode) extractedTypeDescNode).rightTypeDesc());
                addIfNotExist(extractedTypeNames, extractTypeDescriptorNodes(childTypeDescNodes));
            } else {
                addIfNotExist(extractedTypeNames, List.of(extractedTypeDescNode));
            }
        }
        return extractedTypeNames;
    }

    /**
     * This method returns the sorted TypeDescriptorNode list.
     *
     * @param typeDescriptorNodes List of TypeDescriptorNodes has to be sorted.
     * @return {@link List<TypeDescriptorNode>} The sorted TypeDescriptorNode list.
     */
    public static List<TypeDescriptorNode> sortTypeDescriptorNodes(List<TypeDescriptorNode> typeDescriptorNodes) {
        List<TypeDescriptorNode> nonArrayNodes = typeDescriptorNodes.stream()
                .filter(node -> !(node instanceof ArrayTypeDescriptorNode)).collect(Collectors.toList());
        List<TypeDescriptorNode> arrayNodes = typeDescriptorNodes.stream()
                .filter(node -> (node instanceof ArrayTypeDescriptorNode)).collect(Collectors.toList());
        nonArrayNodes.sort(Comparator.comparing(TypeDescriptorNode::toSourceCode));
        arrayNodes.sort((node1, node2) -> {
            ArrayTypeDescriptorNode arrayNode1 = (ArrayTypeDescriptorNode) node1;
            ArrayTypeDescriptorNode arrayNode2 = (ArrayTypeDescriptorNode) node2;
            return getNumberOfDimensions(arrayNode1).equals(getNumberOfDimensions(arrayNode2)) ?
                    (arrayNode1).memberTypeDesc().toSourceCode()
                            .compareTo((arrayNode2).memberTypeDesc().toSourceCode()) :
                    getNumberOfDimensions(arrayNode1) - getNumberOfDimensions(arrayNode2);
        });
        return Stream.concat(nonArrayNodes.stream(), arrayNodes.stream()).collect(Collectors.toList());
    }

    /**
     * This method returns the memberTypeDesc node of an ArrayTypeDescriptorNode.
     *
     * @param typeDescNode ArrayTypeDescriptorNode for which it has to be extracted.
     * @return {@link TypeDescriptorNode} The memberTypeDesc node of the ArrayTypeDescriptor node.
     */
    public static TypeDescriptorNode extractArrayTypeDescNode(TypeDescriptorNode typeDescNode) {
        if (typeDescNode.kind().equals(SyntaxKind.ARRAY_TYPE_DESC)) {
            ArrayTypeDescriptorNode arrayTypeDescNode = (ArrayTypeDescriptorNode) typeDescNode;
            return extractArrayTypeDescNode(arrayTypeDescNode.memberTypeDesc());
        } else {
            return typeDescNode;
        }
    }

    /**
     * This method returns a list of TypeDescriptorNodes extracted from a UnionTypeDescriptorNode.
     *
     * @param typeDescNode UnionTypeDescriptorNode for which that has to be extracted.
     * @return {@link List<TypeDescriptorNode>} The list of extracted TypeDescriptorNodes.
     */
    public static List<TypeDescriptorNode> extractUnionTypeDescNode(TypeDescriptorNode typeDescNode) {
        List<TypeDescriptorNode> extractedTypeDescNodes = new ArrayList<>();
        TypeDescriptorNode extractedTypeDescNode = typeDescNode;
        if (typeDescNode.kind().equals(SyntaxKind.PARENTHESISED_TYPE_DESC)) {
            extractedTypeDescNode = extractParenthesisedTypeDescNode(typeDescNode);
        }
        if (extractedTypeDescNode.kind().equals(SyntaxKind.UNION_TYPE_DESC)) {
            UnionTypeDescriptorNode unionTypeDescNode = (UnionTypeDescriptorNode) extractedTypeDescNode;
            TypeDescriptorNode leftTypeDescNode = unionTypeDescNode.leftTypeDesc();
            TypeDescriptorNode rightTypeDescNode = unionTypeDescNode.rightTypeDesc();
            extractedTypeDescNodes.addAll(extractUnionTypeDescNode(leftTypeDescNode));
            extractedTypeDescNodes.addAll(extractUnionTypeDescNode(rightTypeDescNode));
        } else {
            extractedTypeDescNodes.add(extractedTypeDescNode);
        }
        return extractedTypeDescNodes;
    }

    /**
     * This method returns the number of dimensions of an ArrayTypeDescriptorNode.
     *
     * @param arrayNode ArrayTypeDescriptorNode for which the no. of dimensions has to be calculated.
     * @return {@link Integer} The total no. of dimensions of the ArrayTypeDescriptorNode.
     */
    public static Integer getNumberOfDimensions(ArrayTypeDescriptorNode arrayNode) {
        int totalDimensions = arrayNode.dimensions().size();
        if (arrayNode.memberTypeDesc() instanceof ArrayTypeDescriptorNode) {
            totalDimensions += getNumberOfDimensions((ArrayTypeDescriptorNode) arrayNode.memberTypeDesc());
        }
        return totalDimensions;
    }

    private static TypeDescriptorNode extractParenthesisedTypeDescNode(TypeDescriptorNode typeDescNode) {
        if (typeDescNode instanceof ParenthesisedTypeDescriptorNode) {
            return extractParenthesisedTypeDescNode(((ParenthesisedTypeDescriptorNode) typeDescNode).typedesc());
        } else {
            return typeDescNode;
        }
    }

    private static void addIfNotExist(List<TypeDescriptorNode> typeDescNodes,
                                      List<TypeDescriptorNode> typeDescNodesToBeInserted) {
        for (TypeDescriptorNode typeDescNodeToBeInserted : typeDescNodesToBeInserted) {
            if (typeDescNodes.stream().noneMatch(typeDescNode -> typeDescNode.toSourceCode()
                    .equals(typeDescNodeToBeInserted.toSourceCode()))) {
                typeDescNodes.add(typeDescNodeToBeInserted);
            }
        }
    }

    private static String getUpdatedFieldName(String fieldName, List<String> existingFieldNames) {
        if (!existingFieldNames.contains(fieldName)) {
            return fieldName;
        } else {
            return getUpdatedFieldName(fieldName + "Duplicate", existingFieldNames);
        }
    }
}
