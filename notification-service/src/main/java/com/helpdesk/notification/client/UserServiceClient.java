package com.helpdesk.notification.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @SuppressWarnings("unchecked")
    public boolean hasTechnicalSupportRole(String userId) {
        try {
            String url = "http://user-service/users/" + userId;
            log.info("[DEBUG] Fetching user from: {}", url);
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getBody() != null) {
                log.info("[DEBUG] User response body: {}", response.getBody());
                
                // Check both 'role' (singular string) and 'roles' (list)
                String singleRole = (String) response.getBody().get("role");
                List<String> rolesList = (List<String>) response.getBody().get("roles");
                
                log.info("[DEBUG] User {} role (singular): {}, roles (list): {}", userId, singleRole, rolesList);
                
                // Check singular role field
                if (singleRole != null && "TECH_SUPPORT".equals(singleRole)) {
                    log.info("[DEBUG] User {} has TECH_SUPPORT role (singular field): true", userId);
                    return true;
                }
                
                // Check roles list field
                boolean hasTechSupport = rolesList != null && rolesList.contains("TECH_SUPPORT");
                log.info("[DEBUG] User {} has TECH_SUPPORT role (list field): {}", userId, hasTechSupport);
                return hasTechSupport;
            }
            log.warn("[DEBUG] User response body is null for userId: {}", userId);
            return false;
        } catch (Exception e) {
            log.error("[DEBUG] Error fetching user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }
}