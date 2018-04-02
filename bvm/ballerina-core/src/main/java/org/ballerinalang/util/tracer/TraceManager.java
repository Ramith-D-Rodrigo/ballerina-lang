/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.util.tracer;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import org.ballerinalang.bre.bvm.WorkerExecutionContext;
import org.ballerinalang.util.tracer.config.ConfigLoader;
import org.ballerinalang.util.tracer.config.OpenTracingConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

import static org.ballerinalang.util.tracer.TraceConstants.TRACE_PREFIX;
import static org.ballerinalang.util.tracer.TraceConstants.TRACE_PREFIX_LENGTH;

/**
 * {@link TraceManager} loads {@link TraceManager} implementation
 * and wraps it's functionality.
 *
 * @since 0.964.1
 */
public class TraceManager {
    private static final TraceManager instance = new TraceManager();
    private TracersStore tracerStore;
    private Stack<BTracer> tracerStack;

    private TraceManager() {
        OpenTracingConfig openTracingConfig = ConfigLoader.load();
        tracerStore = new TracersStore(openTracingConfig);
        tracerStack = new Stack<>();
    }

    public static TraceManager getInstance() {
        return instance;
    }

    public void startSpan(WorkerExecutionContext ctx) {
        BTracer activeTracer = TraceUtil.getTracer(ctx);
        if (activeTracer != null) {
            BTracer parentTracer = !tracerStack.empty() ? tracerStack.peek() : null;
            String service = activeTracer.getConnectorName();
            String resource = activeTracer.getActionName();

            Map<String, Span> spanList;
            if (parentTracer != null) {
                spanList = startSpan(resource, parentTracer.getSpans(),
                        activeTracer.getTags(), service, false);
            } else {
                Map<String, String> spanHeaders = activeTracer.getProperties()
                        .entrySet().stream().collect(
                                Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue()))
                        );
                spanList = startSpan(resource, extractSpanContext(removeTracePrefix(spanHeaders), service),
                        activeTracer.getTags(), service, true);
            }

            activeTracer.setSpans(spanList);
            tracerStack.push(activeTracer);
        }
    }

    public void finishSpan(BTracer bTracer) {
        bTracer.getSpans().values().forEach(Span::finish);
        if (!tracerStack.empty()) {
            tracerStack.pop();
        }
    }

    public void log(BTracer bTracer, Map<String, Object> fields) {
        bTracer.getSpans().values().forEach(span -> span.log(fields));
    }

    public void addTags(BTracer bTracer, Map<String, String> tags) {
        bTracer.getSpans().values().forEach(span -> {
            tags.forEach((key, value) -> span.setTag(key, String.valueOf(value)));
        });
    }

    public BTracer getParentTracer() {
        return !tracerStack.empty() ? tracerStack.peek() : null;
    }

    public Map<String, String> extractTraceContext(Map<String, Span> activeSpanMap, String serviceName) {
        Map<String, String> carrierMap = new HashMap<>();
        for (Map.Entry<String, Span> activeSpanEntry : activeSpanMap.entrySet()) {
            Map<String, Tracer> tracers = tracerStore.getTracers(serviceName);
            Tracer tracer = tracers.get(activeSpanEntry.getKey());
            if (tracer != null) {
                Span span = activeSpanEntry.getValue();
                if (span != null) {
                    tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS,
                            new RequestInjector(TRACE_PREFIX, carrierMap));
                }
            }
        }
        return carrierMap;
    }

    private Map<String, Span> startSpan(String spanName, Map<String, ?> spanContextMap,
                                        Map<String, String> tags, String serviceName, boolean isParent) {
        Map<String, Span> spanMap = new HashMap<>();
        Map<String, Tracer> tracers = tracerStore.getTracers(serviceName);
        for (Map.Entry spanContextEntry : spanContextMap.entrySet()) {
            Tracer tracer = tracers.get(spanContextEntry.getKey().toString());
            Tracer.SpanBuilder spanBuilder = tracer.buildSpan(spanName);

            for (Map.Entry<String, String> tag : tags.entrySet()) {
                spanBuilder = spanBuilder.withTag(tag.getKey(), tag.getValue());
            }

            if (spanContextEntry.getValue() != null) {
                if (isParent) {
                    spanBuilder = spanBuilder.asChildOf((SpanContext) spanContextEntry.getValue());
                } else {
                    spanBuilder = spanBuilder.asChildOf((Span) spanContextEntry.getValue());
                }
            }

            Span span = spanBuilder.start();
            spanMap.put(spanContextEntry.getKey().toString(), span);
        }
        return spanMap;
    }

    private Map<String, Object> extractSpanContext(Map<String, String> headers, String serviceName) {
        Map<String, Object> spanContext = new HashMap<>();
        Map<String, Tracer> tracers = tracerStore.getTracers(serviceName);
        for (Map.Entry<String, Tracer> tracerEntry : tracers.entrySet()) {
            spanContext.put(tracerEntry.getKey(), tracerEntry.getValue()
                    .extract(Format.Builtin.HTTP_HEADERS, new RequestExtractor(headers)));
        }
        return spanContext;
    }

    private static Map<String, String> removeTracePrefix(Map<String, String> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().substring(TRACE_PREFIX_LENGTH),
                        Map.Entry::getValue));
    }

}
