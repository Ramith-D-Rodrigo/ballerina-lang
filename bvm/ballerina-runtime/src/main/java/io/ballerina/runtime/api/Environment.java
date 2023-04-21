/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.runtime.api;

import io.ballerina.runtime.api.async.StrandMetadata;
import io.ballerina.runtime.api.types.Parameter;

import java.util.Optional;

/**
 * When this class is used as the first argument of an interop method, Ballerina will inject an instance of the class
 * when calling. That instance can be used to communicate with currently executing Ballerina runtime.
 *
 * @since 2.0.0
 */
public interface Environment {

    /**
     * Returns the Ballerina function name for the corresponding external interop method.
     *
     * @return function name
     */
    String getFunctionName();

    /**
     * Returns an array consisting of the path parameters of the resource function defined as external.
     *
     * @return array of {@link Parameter}
     */
    Parameter[] getFunctionPathParameters();

    /**
     * Mark the current executing strand as async. Execution of Ballerina code after the current
     * interop will stop until given BalFuture is completed. However the java thread will not be blocked
     * and will be reused for running other Ballerina code in the meantime. Therefore callee of this method
     * must return as soon as possible to avoid starvation of ballerina code execution.
     *
     * @return BalFuture which will resume the current strand when completed.
     */
    Future markAsync();

    Runtime getRuntime();

    /**
     * Gets current module {@link Module}.
     *
     * @return module of the environment.
     */
    Module getCurrentModule();

    /**
     * Gets the strand id. This will be generated on strand initialization.
     *
     * @return Strand id.
     */
    int getStrandId();

    /**
     * Gets the strand name. This will be optional. Strand name can be either name given in strand annotation or async
     * call or function pointer variable name.
     *
     * @return Optional strand name.
     */
    Optional<String> getStrandName();

    /**
     * Gets {@link StrandMetadata}.
     *
     * @return metadata of the strand.
     */
    StrandMetadata getStrandMetadata();

    /**
     * Sets given local key value pair in strand.
     *
     * @param key   string key
     * @param value value to be store in the strand
     */
    void setStrandLocal(String key, Object value);

    /**
     * Gets the value stored in the strand on given key.
     *
     * @param key key
     * @return value stored in the strand.
     */
    Object getStrandLocal(String key);
}
