/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
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

package org.ballerinalang.test.worker;

import org.ballerinalang.test.BCompileUtil;
import org.ballerinalang.test.BRunUtil;
import org.ballerinalang.test.CompileResult;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * worker alternative receive related tests.
 */
public class WorkerAlternateReceiveTest {

    private CompileResult result;

    @BeforeClass
    public void setup() {
        this.result = BCompileUtil.compile("test-src/workers/workers_alt_receive.bal");
        Assert.assertEquals(result.getErrorCount(), 0);
    }

    @Test(dataProvider = "functionProvider")
    public void workerAlternateReceiveTest(String funcName) {
        BRunUtil.invoke(result, funcName, new Object[0]);
    }

    @DataProvider
    public static String[] functionProvider() {
        return new String[] {
                "workerAlternateReceiveTest",
                "workerAlternateReceiveTest2",
                "alternateReceiveWithSenderPanic",
                "alternateReceiveWithMultiplePanic",
                "alternateReceiveWithSenderError",
                "alternateReceiveWithMultipleError",
                "alternateReceiveWithPanicAndError",
                "alternateReceiveWithReceiverPanic",
                "alternateReceiveWithReceiverError",
                "alternateReceiveWithSameWorkerSend",
                "alternateReceiveWithSameWorkerSendError1",
                "alternateReceiveWithSameWorkerSendError2",
                "alternateReceiveWithSameWorkerSendPanic",
                "multilpleAlternateReceive1",
                "multilpleAlternateReceive2"
        };
    }

    @AfterClass
    public void tearDown() {
        result = null;
    }
}
