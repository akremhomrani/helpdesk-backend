FROM quay.io/keycloak/keycloak:24.0.2

# Copy realm configuration
COPY realm.json /opt/keycloak/data/import/
COPY themes /opt/keycloak/themes

# Set environment for production
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_HTTP_ENABLED=true
ENV KC_HOSTNAME_STRICT=false

# Build optimized Keycloak - disable clustering for single instance
RUN /opt/keycloak/bin/kc.sh build --cache=local

# Run Keycloak with import - PORT from Render environment
CMD ["/bin/sh", "-c", "/opt/keycloak/bin/kc.sh start --import-realm --http-port=${PORT:-10000} --hostname-strict=false --proxy=edge --cache=local"]
