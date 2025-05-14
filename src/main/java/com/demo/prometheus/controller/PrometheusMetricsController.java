package com.demo.prometheus.controller;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.common.TextFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/sample")
public class PrometheusMetricsController {

    // Gauge for duration metric (labeled by name and keyword)
    private static final Gauge stepDurationGauge = Gauge.build()
            .name("test_step_duration_seconds_without_feature")
            .help("Duration of each test step in nanoseconds")
            .labelNames("step_name", "keyword")
            .register();

    // Gauge for status metric (1 = passed, 0 = failed)
    private static final Gauge stepStatusGauge = Gauge.build()
            .name("test_step_status_without_feature")
            .help("Status of each test step (1 = passed, 0 = failed)")
            .labelNames("step_name", "keyword")
            .register();

    @GetMapping("/metrics")
    public void getMetrics(HttpServletResponse response) throws IOException {
        // Read JSON from file
    	String filePath = "C:\\Users\\KaranYadav\\Downloads\\data.json";
        String jsonData = readJsonFromFile(filePath);

        // Parse and populate metrics
        processJsonToMetrics(jsonData);

        // Set response type to Prometheus format
        response.setContentType(TextFormat.CONTENT_TYPE_004);

        // Write all metrics
        TextFormat.write004(response.getWriter(),
                io.prometheus.client.CollectorRegistry.defaultRegistry.metricFamilySamples());
    }

    private String readJsonFromFile(String filePath) throws IOException {
        return Files.readString(new File(filePath).toPath());
    }

    private void processJsonToMetrics(String jsonData) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonData);

        for (JsonNode feature : root) {
            JsonNode elements = feature.get("elements");
            if (elements != null && elements.isArray()) {
                for (JsonNode element : elements) {
                    JsonNode steps = element.get("steps");
                    if (steps != null && steps.isArray()) {
                        for (JsonNode step : steps) {
                            String name = step.has("name") ? step.get("name").asText() : "unknown_step";
                            String keyword = step.has("keyword") ? step.get("keyword").asText() : "unknown";

                            JsonNode result = step.get("result");
                            long duration = (result != null && result.has("duration"))
                                    ? result.get("duration").asLong() : 0;
                            String status = (result != null && result.has("status"))
                                    ? result.get("status").asText() : "unknown";

                            double statusValue = "passed".equalsIgnoreCase(status) ? 1.0 : 0.0;

                            // Set values in Gauges
                            stepDurationGauge.labels(name, keyword).set(duration);
                            stepStatusGauge.labels(name, keyword).set(statusValue);
                        }
                    }
                }
            }
        }
    }
}
