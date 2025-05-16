package com.demo.prometheus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.prometheus.client.Gauge;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PrometheusPushService {

    private static final CollectorRegistry registry = new CollectorRegistry();

    @Value("${prometheus.pushgateway.url}")
    private String pushgatewayUrl;

    private static final Gauge stepDurationGauge = Gauge.build()
            .name("test_step_duration_seconds")
            .help("Duration of each test step in milliseconds")
            .labelNames("feature_file", "step_name", "keyword")
            .register(registry);

    private static final Gauge stepStatusGauge = Gauge.build()
            .name("test_step_status")
            .help("Status of each test step (1 = passed, 0 = failed)")
            .labelNames("feature_file", "step_name", "keyword", "status")
            .register(registry);

    private static final Gauge featureDurationGauge = Gauge.build()
            .name("test_feature_duration_seconds")
            .help("Total duration of the test feature in milliseconds")
            .labelNames("feature_file")
            .register(registry);

    public void pushMetrics(String filePath) throws Exception {
        String jsonData = readJsonFromFile(filePath);
        processJsonToMetrics(jsonData);

        // Push to PushGateway
        PushGateway pg = new PushGateway(pushgatewayUrl);
        pg.pushAdd(registry, "spring_boot_app"); // "spring_boot_app" is the job name in Prometheus

        System.out.println("Metrics pushed to PushGateway!");
    }

    private static void processJsonToMetrics(String jsonData) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonData);

        for (JsonNode feature : root) {
            // Extract URI
            String featureFileRaw = feature.has("uri") ? feature.get("uri").asText() : "unknown";
            String featureFile = featureFileRaw.substring(51); // Adjust to get the correct feature file path

            // Initialize total duration for the feature
            long totalDuration = 0;

            JsonNode elements = feature.get("elements");
            if (elements != null && elements.isArray()) {
                for (JsonNode element : elements) {
                    JsonNode steps = element.get("steps");
                    if (steps != null && steps.isArray()) {
                        for (JsonNode step : steps) {
                            String name = step.has("name") ? step.get("name").asText() : "unknown_step";
                            String keyword = step.has("keyword") ? step.get("keyword").asText() : "unknown";

                            JsonNode result = step.get("result");
                            long duration = (result != null && result.has("duration")) ? result.get("duration").asLong() / 1000000 : 0;
                            String status = (result != null && result.has("status")) ? result.get("status").asText() : "unknown";

                            double statusValue = "passed".equalsIgnoreCase(status) ? 1.0 : 0.0;

                            // Set the step-level metrics
                            stepDurationGauge.labels(featureFile, name, keyword).set(duration);
                            stepStatusGauge.labels(featureFile, name, keyword, status).set(statusValue);

                            // Accumulate total duration
                            totalDuration += duration;
                        }
                    }
                }
            }

            // Set the total feature duration gauge
            featureDurationGauge.labels(featureFile).set(totalDuration / 1000.0); // Convert to seconds for Prometheus
        }
    }

    private String readJsonFromFile(String filePath) throws IOException {
        return Files.readString(new File(filePath).toPath());
    }

}
