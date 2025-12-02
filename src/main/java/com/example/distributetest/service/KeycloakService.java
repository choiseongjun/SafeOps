package com.example.distributetest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${keycloak.auth-server-url:http://localhost:8180}")
    private String authServerUrl;

    @Value("${keycloak.realm:safeops}")
    private String realm;

    @Value("${keycloak.resource:safeops-client}")
    private String clientId;

    @Value("${keycloak.credentials.secret:}")
    private String clientSecret;

    public String createUser(String username, String email, String password) {
        try {
            String adminToken = getAdminToken();

            String url = String.format("%s/admin/realms/%s/users", authServerUrl, realm);

            Map<String, Object> userRepresentation = new HashMap<>();
            userRepresentation.put("username", username);
            userRepresentation.put("email", email);
            userRepresentation.put("enabled", true);
            userRepresentation.put("emailVerified", true);

            Map<String, String> credentials = new HashMap<>();
            credentials.put("type", "password");
            credentials.put("value", password);
            credentials.put("temporary", "false");

            userRepresentation.put("credentials", Collections.singletonList(credentials));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(userRepresentation, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                String location = response.getHeaders().getLocation().toString();
                String userId = location.substring(location.lastIndexOf('/') + 1);
                log.info("User created in Keycloak with ID: {}", userId);
                return userId;
            }

            throw new RuntimeException("Failed to create user in Keycloak");

        } catch (Exception e) {
            log.error("Error creating user in Keycloak", e);
            throw new RuntimeException("Failed to create user in Keycloak: " + e.getMessage());
        }
    }

    public Map<String, Object> authenticateUser(String username, String password) {
        try {
            String url = String.format("%s/realms/%s/protocol/openid-connect/token", authServerUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", clientId);
            body.add("username", username);
            body.add("password", password);

            if (clientSecret != null && !clientSecret.isEmpty()) {
                body.add("client_secret", clientSecret);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());

                Map<String, Object> tokenData = new HashMap<>();
                tokenData.put("access_token", jsonNode.get("access_token").asText());
                tokenData.put("refresh_token", jsonNode.get("refresh_token").asText());
                tokenData.put("expires_in", jsonNode.get("expires_in").asLong());

                return tokenData;
            }

            throw new RuntimeException("Authentication failed");

        } catch (Exception e) {
            log.error("Error authenticating user", e);
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }

    private String getAdminToken() {
        try {
            String url = String.format("%s/realms/master/protocol/openid-connect/token", authServerUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", "admin-cli");
            body.add("username", "admin");
            body.add("password", "admin");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return jsonNode.get("access_token").asText();
            }

            throw new RuntimeException("Failed to get admin token");

        } catch (Exception e) {
            log.error("Error getting admin token", e);
            throw new RuntimeException("Failed to get admin token: " + e.getMessage());
        }
    }
}
