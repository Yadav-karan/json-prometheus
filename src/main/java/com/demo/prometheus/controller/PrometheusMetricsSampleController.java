package com.demo.prometheus.controller;
import java.io.IOException;

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

}
