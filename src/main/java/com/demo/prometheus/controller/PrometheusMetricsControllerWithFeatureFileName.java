package com.demo.prometheus.controller;
import com.demo.prometheus.service.PrometheusPushService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.common.TextFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/with-feature-file")
public class PrometheusMetricsControllerWithFeatureFileName {
	
	@Value("${json.data.file}")
	private String filePath;

	@Autowired
	private PrometheusPushService prometheusPushService;
	
	private static final Gauge stepDurationGauge = Gauge.build()
	        .name("test_step_duration_seconds")
	        .help("Duration of each test step in miliseconds")
	        .labelNames("feature_file", "step_name", "keyword")
	        .register();

	private static final Gauge stepStatusGauge = Gauge.build()
	        .name("test_step_status")
	        .help("Status of each test step (1 = passed, 0 = failed)")
	        .labelNames("feature_file", "step_name", "keyword", "status")
	        .register();


    @GetMapping("/sample-metrics")
    public void getMetrics(HttpServletResponse response) throws IOException {
        // Read JSON from file
    	
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
            // Extract feature file name from URI
            String featureFileRaw = feature.has("uri") ? feature.get("uri").asText() : "unknown";
            String featureFile = featureFileRaw.substring(51);
            JsonNode elements = feature.get("elements");
            if (elements != null && elements.isArray()) {
                for (JsonNode element : elements) {
                    JsonNode steps = element.get("steps");
                    if (steps != null && steps.isArray()) {
                        for (JsonNode step : steps) {
                            String name = step.has("name") ? step.get("name").asText() : "unknown_step";
                            String keyword = step.has("keyword") ? step.get("keyword").asText() : "unknown";

                            JsonNode result = step.get("result");
                            long duration = (result != null && result.has("duration")) ? result.get("duration").asLong()/1000000 : 0;
                            String status = (result != null && result.has("status")) ? result.get("status").asText() : "unknown";

                            double statusValue = "passed".equalsIgnoreCase(status) ? 1.0 : 0.0;

                            // Register metrics with feature file name as a label
                            stepDurationGauge.labels(featureFile, name, keyword).set(duration);
                            stepStatusGauge.labels(featureFile, name, keyword, status).set(statusValue);
                        }
                    }
                }
            }
        }
    }
    
    @GetMapping("/push-metrics")
    public ResponseEntity<String> getPushBasedMetrics() throws Exception{
    	prometheusPushService.pushMetrics(filePath);
    	return new ResponseEntity<>("Metrics Pushed Successfully",HttpStatus.OK);
    }
}
