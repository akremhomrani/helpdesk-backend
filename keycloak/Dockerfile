FROM quay.io/keycloak/keycloak:24.0.2

# Copy realm configuration
COPY realm.json /opt/keycloak/data/import/
COPY themes /opt/keycloak/themes

# Set environment for production
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_HTTP_ENABLED=true
ENV KC_HOSTNAME_STRICT=false

# Build optimized Keycloak
RUN /opt/keycloak/bin/kc.sh build

# Run Keycloak with import - will use PORT env var from Render
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", "--import-realm", "--http-port=${PORT:-8080}", "--hostname-strict=false", "--proxy=edge"]
