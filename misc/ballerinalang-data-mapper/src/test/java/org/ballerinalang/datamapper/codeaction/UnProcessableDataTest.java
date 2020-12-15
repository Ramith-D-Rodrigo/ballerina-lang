/*
 * Copyright (c) 2020, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ballerinalang.datamapper.codeaction;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ballerinalang.datamapper.util.FileUtils;
import org.ballerinalang.datamapper.util.TestUtil;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static org.ballerinalang.datamapper.util.DataMapperTestUtils.getCodeActionResponse;

/**
 * Test Cases for CodeActions.
 *
 * @since 2.0.0
 */
public class UnProcessableDataTest {

    private static Endpoint serviceEndpoint;
    private static final Logger log = LoggerFactory.getLogger(CodeActionTest.class);
    private static Server server;

    @BeforeClass
    private void init() throws Exception {
        serviceEndpoint = TestUtil.initializeLanguageSever();
        String startConfigPath = "codeaction" + File.separator + "config" + File.separator +
                "unProcDataConfig.json";
        JsonObject configs = FileUtils.fileContentAsObject(startConfigPath);
        TestUtil.setWorkspaceConfig(serviceEndpoint, configs);

        class HelloWorldHandler extends AbstractHandler {
            String responseData = "";
            int statusCode = 422;

            @Override
            public void handle(String target, Request jettyRequest, HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
                // Mark the request as handled by this Handler.
                jettyRequest.setHandled(true);

                response.setStatus(statusCode);
                response.setContentType("text/html; charset=UTF-8");
                response.getWriter().print(responseData);
            }
        }

        server = new Server(9010);
        Connector connector = new ServerConnector(server);
        server.addConnector(connector);

        // Set the Hello World Handler.
        server.setHandler(new HelloWorldHandler());
        server.start();
    }

    @Test(dataProvider = "codeAction-data-mapper-data-provider-un-processable-data")
    public void testDataMapperCodeActionWithUnProcessableData(String config, String source) throws Exception {

        // Read expected results
        String configJsonPath = "codeaction" + File.separator + config;
        JsonObject configJsonObject = FileUtils.fileContentAsObject(configJsonPath);
        JsonObject expectedResponse = configJsonObject.get("expected").getAsJsonObject();
        String title = expectedResponse.get("title").getAsString();

        // Get code action from language server
        JsonObject responseJson = getCodeActionResponse(source, configJsonObject, serviceEndpoint);

        boolean codeActionFound = false;
        for (JsonElement jsonElement : responseJson.getAsJsonArray("result")) {
            JsonElement right = jsonElement.getAsJsonObject().get("right");
            JsonElement editText = right.getAsJsonObject().get("edit");
            if (editText == null) {
                continue;
            }
            if (right.getAsJsonObject().get("title").getAsString().equals(title)) {
                codeActionFound = true;
            }
        }
        Assert.assertFalse(codeActionFound, "Returned an invalid code action");
    }

    @DataProvider(name = "codeAction-data-mapper-data-provider-un-processable-data")
    public Object[][] codeActionDataMapperDataProviderUnProcessableData() {
        log.info("Test textDocument/codeAction QuickFixes");
        return new Object[][]{
                {"dataMapper3.json", "dataMapper3.bal"},
        };
    }

    @AfterClass
    private void cleanupLanguageServer() {
        TestUtil.shutdownLanguageServer(serviceEndpoint);
        try {
            server.stop();
            server.destroy();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}

