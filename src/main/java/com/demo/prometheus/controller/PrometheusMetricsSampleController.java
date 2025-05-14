package com.demo.prometheus.controller;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.common.TextFormat;
import jakarta.servlet.http.HttpServletResponse;


@RestController
public class PrometheusMetricsSampleController {

    private static final Gauge jsonDataGauge = Gauge.build()
            .name("json_data_gauge")
            .help("Gauge metric for JSON data")
            .register();
    
    private static final Gauge name = Gauge.build()
    		.name("name")
    		.help("name metric for test")
    		.register();
    
    private static final Gauge keyword = Gauge.build()
    		.name("keyword")
    		.help("keyword metric for test")
    		.register();
    
    private static final Gauge duration = Gauge.build()
    		.name("duration")
    		.help("duration metric for test")
    		.register();
    
    private static final Gauge status = Gauge.build()
    		.name("status")
    		.help("status metric for test")
    		.register();

    @GetMapping("/metrics")
    public void getMetrics(HttpServletResponse response) throws IOException {
    	String jsonData = "{ \"key\": \"value\", \"count\": 42 }";

        int jsonCount = extractCountFromJson(jsonData);
        jsonDataGauge.set(jsonCount);

        response.setContentType(TextFormat.CONTENT_TYPE_004);
        
        TextFormat.write004(response.getWriter(), io.prometheus.client.CollectorRegistry.defaultRegistry.metricFamilySamples());
    }
    
    private int extractCountFromJson(String jsonData) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonData);
        return jsonNode.get("count").asInt();  // Assuming 'count' is an integer field
    }
    
    private void extractCountFromJsonDataReceivedFromFile(String jsonData) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(jsonData);
        for (JsonNode featureNode : root) {
            JsonNode elements = featureNode.get("elements");
            if (elements != null && elements.isArray()) {
                for (JsonNode element : elements) {
                    JsonNode steps = element.get("steps");
                    if (steps != null && steps.isArray()) {
                        for (JsonNode step : steps) {
                            String name = step.has("name") ? step.get("name").asText() : "";
                            String keyword = step.has("keyword") ? step.get("keyword").asText() : "";

                            JsonNode result = step.get("result");
                            long duration = result != null && result.has("duration") ? result.get("duration").asLong() : 0L;
                            String status = result != null && result.has("status") ? result.get("status").asText() : "";

                            System.out.println("Name    : " + name);
                            System.out.println("Keyword : " + keyword);
                            System.out.println("Duration: " + duration);
                            System.out.println("Status  : " + status);
                            System.out.println("--------------------------------");
                        }
                    }
                }
            }
        }
        
    }
    
    private String readJsonFromFile(String filePath) throws IOException {
        File file = new File(filePath);
        return Files.readString(file.toPath());  // Java 11 and above
    }
}
