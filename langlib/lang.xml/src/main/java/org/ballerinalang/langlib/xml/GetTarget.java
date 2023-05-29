/*
 *   Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.langlib.xml;

import io.ballerina.runtime.api.types.XmlNodeType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BXml;
import io.ballerina.runtime.internal.errors.ErrorHelper;
import io.ballerina.runtime.internal.errors.RuntimeErrors;
import io.ballerina.runtime.internal.values.XmlPi;

/**
 * Create XML processing instruction.
 *
 * @since 1.0
 */

public class GetTarget {

    public static BString getTarget(BXml xmlValue) {
        if (!IsProcessingInstruction.isProcessingInstruction(xmlValue)) {
            throw ErrorHelper.getRuntimeException(RuntimeErrors.XML_FUNC_TYPE_ERROR,
                    "getTarget", "processing instruction");
        }

        if (xmlValue.getNodeType() == XmlNodeType.PI) {
            return StringUtils.fromString(((XmlPi) xmlValue).getTarget());
        }

        return StringUtils.fromString(((XmlPi) xmlValue.getItem(0)).getTarget());
    }
}
