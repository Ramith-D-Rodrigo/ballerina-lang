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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.projects;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.impl.BallerinaSemanticModel;
import io.ballerina.projects.CompilerBackend.TargetPlatform;
import io.ballerina.projects.environment.ProjectEnvironment;
import io.ballerina.projects.internal.DefaultDiagnosticResult;
import io.ballerina.projects.internal.PackageDiagnostic;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.ballerinalang.compiler.plugins.CompilerPlugin;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.CompilerOptions;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;

import static org.ballerinalang.compiler.CompilerOptionName.CLOUD;
import static org.ballerinalang.compiler.CompilerOptionName.DUMP_BIR;
import static org.ballerinalang.compiler.CompilerOptionName.DUMP_BIR_FILE;
import static org.ballerinalang.compiler.CompilerOptionName.EXPERIMENTAL_FEATURES_ENABLED;
import static org.ballerinalang.compiler.CompilerOptionName.OBSERVABILITY_INCLUDED;
import static org.ballerinalang.compiler.CompilerOptionName.OFFLINE;
import static org.ballerinalang.compiler.CompilerOptionName.SKIP_TESTS;

/**
 * Compilation at package level by resolving all the dependencies.
 *
 * @since 2.0.0
 */
public class PackageCompilation {

    private final PackageContext rootPackageContext;
    private final PackageResolution packageResolution;
    private final CompilerContext compilerContext;
    private final Map<TargetPlatform, CompilerBackend> compilerBackends;
    private final List<Diagnostic> pluginDiagnostics;

    private DiagnosticResult diagnosticResult;
    private volatile boolean compiled;
    private CompilerPluginManager compilerPluginManager;

    private PackageCompilation(PackageContext rootPackageContext) {
        this.rootPackageContext = rootPackageContext;
        this.packageResolution = rootPackageContext.getResolution();

        ProjectEnvironment projectEnvContext = rootPackageContext.project().projectEnvironmentContext();
        this.compilerContext = projectEnvContext.getService(CompilerContext.class);

        // Set compilation options retrieved from the build options
        setCompilerOptions(rootPackageContext.compilationOptions());

        // We have only the jvm backend for now.
        this.compilerBackends = new HashMap<>(1);
        this.pluginDiagnostics = new ArrayList<>();
    }

    private void setCompilerOptions(CompilationOptions compilationOptions) {
        CompilerOptions options = CompilerOptions.getInstance(compilerContext);
        options.put(OFFLINE, Boolean.toString(compilationOptions.offlineBuild()));
        options.put(SKIP_TESTS, Boolean.toString(compilationOptions.skipTests()));
        options.put(EXPERIMENTAL_FEATURES_ENABLED, Boolean.toString(compilationOptions.experimental()));
        options.put(OBSERVABILITY_INCLUDED, Boolean.toString(compilationOptions.observabilityIncluded()));
        options.put(DUMP_BIR, Boolean.toString(compilationOptions.dumpBir()));
        options.put(DUMP_BIR_FILE, compilationOptions.getBirDumpFile());
        options.put(CLOUD, compilationOptions.getCloud());
    }

    static PackageCompilation from(PackageContext rootPackageContext) {
        PackageCompilation compilation = new PackageCompilation(rootPackageContext);

        // Compile modules in the dependency graph
        compilation.compileModules();

        // Now the modules are compiled, initialize the compiler plugin manager
        CompilerPluginManager compilerPluginManager = CompilerPluginManager.from(compilation);
        compilation.setCompilerPluginManager(compilerPluginManager);

        // Run the CodeAnalyzer tasks.
        CodeAnalyzerManager codeAnalyzerManager = compilerPluginManager.getCodeAnalyzerManager();
        // At the moment, we run SyntaxNodeAnalysis and CompilationAnalysis tasks at the same time.
        // We can run SyntaxNodeAnalysis for each module compilation in the future.
        List<Diagnostic> reportedDiagnostics = codeAnalyzerManager.runCodeAnalyzerTasks();
        addCompilerPluginDiagnostics(compilation, reportedDiagnostics);
        return compilation;
    }

    public List<Diagnostic> notifyCompilationCompletion(Path filePath) {
        CompilerLifecycleManager manager = this.compilerPluginManager.getCompilerLifecycleListenerManager();
        List<Diagnostic> diagnostics = manager.runCodeGeneratedTasks(filePath);
        this.pluginDiagnostics.addAll(diagnostics);
        return diagnostics;
    }

    public PackageResolution getResolution() {
        return packageResolution;
    }

    public DiagnosticResult diagnosticResult() {
        return diagnosticResult;
    }

    public SemanticModel getSemanticModel(ModuleId moduleId) {
        ModuleContext moduleContext = this.rootPackageContext.moduleContext(moduleId);
        // We check whether the particular module compilation state equal to the typecheck phase here. 
        // If the states do not match, then this is a illegal state exception.
        if (moduleContext.compilationState() != ModuleCompilationState.COMPILED) {
            throw new IllegalStateException("Semantic model cannot be retrieved when the module is in " +
                    "compilation state '" + moduleContext.compilationState().name() + "'. " +
                    "This is an internal error which will be fixed in a later release.");
        }

        return new BallerinaSemanticModel(moduleContext.bLangPackage(), this.compilerContext);
    }

    CompilerPluginManager compilerPluginManager() {
        return compilerPluginManager;
    }

    // TODO Remove this method. We should not expose BLangPackage from this class
    public BLangPackage defaultModuleBLangPackage() {
        return this.rootPackageContext.defaultModuleContext().bLangPackage();
    }

    @SuppressWarnings("unchecked")
    <T extends CompilerBackend> T getCompilerBackend(TargetPlatform targetPlatform,
                                                     Function<TargetPlatform, T> backendCreator) {
        return (T) compilerBackends.computeIfAbsent(targetPlatform, backendCreator);
    }

    PackageContext packageContext() {
        return rootPackageContext;
    }

    private void compileModules() {
        if (compiled) {
            return;
        }

        synchronized (this.compilerContext) {
            if (compiled) {
                return;
            }
            compileModulesInternal();
            compiled = true;
        }
    }

    private void compileModulesInternal() {
        List<Diagnostic> diagnostics = new ArrayList<>();
        // add manifest diagnostics
        diagnostics.addAll(packageContext().manifest().diagnostics().allDiagnostics);
        // add resolution diagnostics
        diagnostics.addAll(packageResolution.diagnostics());
        // add compilation diagnostics
        for (ModuleContext moduleContext : packageResolution.topologicallySortedModuleList()) {
            moduleContext.compile(compilerContext);
            for (Diagnostic diagnostic : moduleContext.diagnostics()) {
                diagnostics.add(new PackageDiagnostic(diagnostic, moduleContext.descriptor(), moduleContext.project()));
            }
        }
        // add plugin diagnostics
        runPluginCodeAnalysis(diagnostics);
        diagnosticResult = new DefaultDiagnosticResult(diagnostics);
    }

    private void runPluginCodeAnalysis(List<Diagnostic> diagnostics) {
        // only run plugins for build projects
        if (rootPackageContext.project().kind().equals(ProjectKind.BUILD_PROJECT)) {
            ServiceLoader<CompilerPlugin> processorServiceLoader = ServiceLoader.load(CompilerPlugin.class);
            for (CompilerPlugin plugin : processorServiceLoader) {
                List<Diagnostic> pluginDiagnostics = plugin.codeAnalyze(rootPackageContext.project());
                diagnostics.addAll(pluginDiagnostics);
                this.pluginDiagnostics.addAll(pluginDiagnostics);
            }
        }
    }

    private void setCompilerPluginManager(CompilerPluginManager compilerPluginManager) {
        this.compilerPluginManager = compilerPluginManager;
    }

    List<Diagnostic> pluginDiagnostics() {
        return pluginDiagnostics;
    }

    private static void addCompilerPluginDiagnostics(PackageCompilation compilation,
                                                     List<Diagnostic> reportedDiagnostics) {
        List<Diagnostic> allDiagnostics = new ArrayList<>(compilation.diagnosticResult.diagnostics());
        allDiagnostics.addAll(reportedDiagnostics);
        compilation.diagnosticResult = new DefaultDiagnosticResult(allDiagnostics);

        // TODO We need to refactor how diagnostics are stored and returned
        // TODO I had to put the following line in order to make compiler plugin diagnostics
        //  available to the build command
        compilation.pluginDiagnostics.addAll(reportedDiagnostics);
    }
}
