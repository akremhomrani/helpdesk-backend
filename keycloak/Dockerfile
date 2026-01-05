FROM quay.io/keycloak/keycloak:24.0.2

# Copy realm configuration
COPY realm.json /opt/keycloak/data/import/
COPY themes /opt/keycloak/themes

# Set environment for production
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_HTTP_ENABLED=true
ENV KC_HOSTNAME_STRICT=false
ENV KEYCLOAK_ADMIN=admin
ENV KEYCLOAK_ADMIN_PASSWORD=admin

# Build optimized Keycloak - disable clustering for single instance
RUN /opt/keycloak/bin/kc.sh build --cache=local

# Run Keycloak with import - will use PORT env var from Render
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", "--import-realm", "--http-port=${PORT:-8080}", "--hostname-strict=false", "--proxy=edge", "--cache=local"]
