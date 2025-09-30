package com.hdfclife.api_gateway.api_gateway.controller;

import com.hdfclife.api_gateway.api_gateway.dto.ErrorResponse;
import com.hdfclife.api_gateway.api_gateway.dto.GenericProxyResponse;
import com.hdfclife.api_gateway.api_gateway.dto.LoginRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


//@RestController
//
//@RequestMapping("/api/auth")
//public class GatewayController {
//
//    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);
//    private final RestTemplate restTemplate;
//    private final String authServiceUrl;
//
//    public GatewayController(RestTemplate restTemplate, @Value("${auth.service.url}") String authServiceUrl) {
//        this.restTemplate = restTemplate;
//        this.authServiceUrl = authServiceUrl;
//    }
//
//    @PostMapping("/login")
//    //@RateLimiter(name = "authRateLimiter", fallbackMethod = "rateLimiterFallback")
//    @CircuitBreaker(name = "authCircuitBreaker", fallbackMethod = "circuitBreakerFallback")
//    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
//        logger.info("Gateway has validated and is now forwarding login request for user: {}", loginRequest.getUsername());
//        String loginUrl = authServiceUrl + "/login";
//
//        try {
//            return restTemplate.postForEntity(loginUrl, loginRequest, String.class);
//        } catch (HttpClientErrorException e) {
//            // Catches 4xx errors (like 401 Unauthorized) and forwards them.
//            logger.warn("Client error from auth-service: {} for user {}", e.getStatusCode(), loginRequest.getUsername());
//            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
//        } catch (HttpServerErrorException e) {
//            // CATCHES 5xx ERRORS from auth-service (like 500 Internal Server Error).
//            logger.error("Server error from auth-service: {}", e.getStatusCode(), e);
//            // Re-throw to ensure the Circuit Breaker counts this as a failure.
//            throw e;
//        } catch (ResourceAccessException e) {
//            // Catches network errors (e.g., connection refused). This is the primary trigger for the Circuit Breaker.
//            logger.error("Network error connecting to auth-service: {}", e.getMessage());
//            // Re-throw to ensure the Circuit Breaker counts this as a failure.
//            throw e;
//        } catch (Exception e) {
//            // A final catch-all for any other unexpected exceptions.
//            logger.error("An unexpected error occurred while proxying to auth-service: {}", e.getMessage(), e);
//            return ResponseEntity
//                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(new ErrorResponse("An unexpected gateway error occurred. Please try again later."));
//        }
//    }
//
//    @PostMapping("/logout")
//    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authorizationHeader) {
//        logger.info("Gateway forwarding logout request.");
//        String logoutUrl = authServiceUrl + "/logout";
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", authorizationHeader);
//        HttpEntity<String> entity = new HttpEntity<>(headers);
//
//        try {
//            // CHANGED: We deserialize the response and then re-serialize it.
//            ResponseEntity<GenericProxyResponse> responseEntity =
//                    restTemplate.exchange(logoutUrl, HttpMethod.POST, entity, GenericProxyResponse.class);
//
//            return ResponseEntity.ok(responseEntity.getBody().getProperties());
//
//        } catch (HttpClientErrorException e) {
//            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
//        }
//    }
//
//    @PostMapping("/v1/partners")
//    public ResponseEntity<?> createPartner(@RequestBody String partnerPayload, @RequestHeader("Authorization") String token) {
//        String partnerUrl = "http://partner-service/api/v1/partners";
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.set("Authorization", token); // Forward the token
//        HttpEntity<String> entity = new HttpEntity<>(partnerPayload, headers);
//
//        try {
//            return restTemplate.postForEntity(partnerUrl, entity, String.class);
//        } catch (HttpClientErrorException e) {
//            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
//        }
//    }
//
//    @GetMapping("/v1/partners")
//    public ResponseEntity<?> listPartners(@RequestHeader("Authorization") String token) {
//        String partnerUrl = "http://partner-service/api/v1/partners";
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", token);
//        HttpEntity<String> entity = new HttpEntity<>(headers);
//
//        try {
//            return restTemplate.exchange(partnerUrl, HttpMethod.GET, entity, String.class);
//        } catch (HttpClientErrorException e) {
//            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
//        }
//    }
//
//
//    // Fallback methods remain unchanged.
//    public ResponseEntity<ErrorResponse> rateLimiterFallback(LoginRequest loginRequest, Throwable t) {
//        logger.warn("Rate limit exceeded for login attempt by user: {}. Cause: {}", loginRequest.getUsername(), t.getMessage());
//        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
//                .body(new ErrorResponse("Too many login attempts. Please try again later."));
//    }
//
//    public ResponseEntity<ErrorResponse> circuitBreakerFallback(LoginRequest loginRequest, Throwable t) {
//        logger.error("Circuit breaker is open for auth-service. Login blocked for user: {}. Failure: {}", loginRequest.getUsername(), t.getMessage());
//        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//                .body(new ErrorResponse("Authentication service is temporarily unavailable. Please try again later."));
//    }
//}


@RestController
@RequestMapping("/api") // A generic base path for all proxied APIs
public class GatewayController {

    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);
    private final RestTemplate restTemplate;

    public GatewayController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ===================================================================================
    // ==                             AUTH SERVICE PROXIES                              ==
    // ===================================================================================

    @PostMapping("/auth/login")
    @RateLimiter(name = "authRateLimiter") // Fallback defined in Resilience4j config
    @CircuitBreaker(name = "authCircuitBreaker")
    public ResponseEntity<String> login(@Valid @RequestBody String loginPayload) {
        logger.info("Gateway forwarding login request to auth-service...");
        String loginUrl = "http://auth-service/api/internal/auth/login";
        return postRequest(loginUrl, loginPayload);
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<String> refreshToken(@RequestBody String refreshPayload) {
        logger.info("Gateway forwarding refresh token request to auth-service...");
        String refreshUrl = "http://auth-service/api/internal/auth/refresh";
        return postRequest(refreshUrl, refreshPayload);
    }

    // ===================================================================================
    // ==                            PARTNER SERVICE PROXIES                            ==
    // ===================================================================================

    @PostMapping("/v1/partners")
    public ResponseEntity<String> createPartner(@RequestBody String partnerPayload, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        logger.info("Gateway forwarding CREATE partner request to partner-service...");
        String partnerUrl = "http://partner-service/api/v1/partners";
        return postRequestWithAuth(partnerUrl, partnerPayload, token);
    }

    @GetMapping("/v1/partners/{partnerId}")
    public ResponseEntity<String> getPartnerById(@PathVariable String partnerId, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        logger.info("Gateway forwarding GET partner by ID request to partner-service...");
        String partnerUrl = "http://partner-service/api/v1/partners/" + partnerId;
        return getRequestWithAuth(partnerUrl, token);
    }

    @PatchMapping("/v1/partners/{partnerId}")
    public ResponseEntity<String> updatePartner(@PathVariable String partnerId, @RequestBody String partnerPayload, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        logger.info("Gateway forwarding UPDATE partner request to partner-service...");
        String partnerUrl = "http://partner-service/api/v1/partners/" + partnerId;
        return patchRequestWithAuth(partnerUrl, partnerPayload, token);
    }

    @GetMapping("/v1/partners")
    public ResponseEntity<String> listPartners(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, HttpServletRequest request) {
        logger.info("Gateway forwarding LIST partners request to partner-service...");

        // This builds the base URL for the partner service
        String partnerUrl = "http://partner-service/api/v1/partners";

        // This dynamically forwards all query parameters (like ?sort=...&page=...)
        // from the original request to the downstream service.
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(partnerUrl)
                .query(request.getQueryString());

        return getRequestWithAuth(builder.toUriString(), token);
    }

    // ===================================================================================
    // ==                       PRIVATE HELPER METHODS FOR PROXYING                     ==
    // ===================================================================================

    private ResponseEntity<String> postRequest(String url, String payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        try {
            return restTemplate.postForEntity(url, entity, String.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    private ResponseEntity<String> postRequestWithAuth(String url, String payload, String token) {
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        try {
            return restTemplate.postForEntity(url, entity, String.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    private ResponseEntity<String> patchRequestWithAuth(String url, String payload, String token) {
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        try {
            return restTemplate.exchange(url, HttpMethod.PATCH, entity, String.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    private ResponseEntity<String> getRequestWithAuth(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, token);
        return headers;
    }

    // NOTE: You will need to define your Resilience4j fallback methods here if they are not
    //       configured globally. This example assumes they are handled by a default config.
}