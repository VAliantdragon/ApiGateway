package com.hdfclife.api_gateway.api_gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardGatewayController {

    @Value("${userDetailsService.url}")
    private String userDetailsServiceUrl;  // e.g., http://localhost:8083

    private final RestTemplate restTemplate;

    public DashboardGatewayController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping
    public ResponseEntity<?> getUserDashboard(HttpServletRequest request) {
        String url = userDetailsServiceUrl + "/api/dashboard";

        // Forward the Authorization header to the downstream service
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", request.getHeader("Authorization"));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<?> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }
}

