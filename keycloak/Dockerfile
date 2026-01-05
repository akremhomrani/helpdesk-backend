FROM quay.io/keycloak/keycloak:24.0.2

# Copy realm configuration
COPY realm.json /opt/keycloak/data/import/
COPY themes /opt/keycloak/themes

# Set environment for production
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Run Keycloak with import
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start-dev", "--import-realm"]
