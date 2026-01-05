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
public class DepartmentServiceClient {

    private final RestTemplate restTemplate;

    @SuppressWarnings("unchecked")
    public List<String> getDepartmentUserIds(String departmentId) {
        try {
            String url = "http://department-service/departments/" + departmentId;
            log.info("[DEBUG] Fetching department from: {}", url);
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getBody() != null) {
                log.info("[DEBUG] Department response body: {}", response.getBody());
                List<String> userIds = (List<String>) response.getBody().get("userIds");
                log.info("[DEBUG] Department {} has {} users: {}", departmentId, 
                        userIds != null ? userIds.size() : 0, userIds);
                return userIds;
            }
            log.warn("[DEBUG] Department response body is null for departmentId: {}", departmentId);
            return null;
        } catch (Exception e) {
            log.error("[DEBUG] Error fetching department {}: {}", departmentId, e.getMessage(), e);
            return null;
        }
    }
}