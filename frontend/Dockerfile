# Multi-stage Dockerfile for Frontend + Nginx with TLS
# Stage 1: Generate SSL certificates
FROM alpine:3.18 AS ssl-generator
RUN apk add --no-cache openssl
WORKDIR /ssl
RUN openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout server.key \
    -out server.crt \
    -subj "/C=BG/ST=Sofia/L=Sofia/O=FinLab Challenge/OU=IT Department/CN=localhost" \
    -addext "subjectAltName=DNS:localhost,DNS:frontend,IP:127.0.0.1"

# Stage 2: Build the production image
FROM nginx:1.25-alpine
LABEL maintainer="FinLab Challenge - Instant Invoice: Fraud Shield"
LABEL description="Frontend + Nginx Reverse Proxy with TLS for supplier payment fraud detection"

# Install required packages
RUN apk add --no-cache openssl

# Copy SSL certificates from previous stage
COPY --from=ssl-generator /ssl/server.crt /etc/nginx/ssl/
COPY --from=ssl-generator /ssl/server.key /etc/nginx/ssl/

# Set proper permissions for SSL certificates
RUN chmod 600 /etc/nginx/ssl/server.key && \
    chmod 644 /etc/nginx/ssl/server.crt

# Copy custom Nginx configuration
COPY nginx/nginx.conf /etc/nginx/nginx.conf

# Copy static files
COPY html/ /usr/share/nginx/html/
COPY css/ /usr/share/nginx/html/css/
COPY js/ /usr/share/nginx/html/js/

# Create necessary directories
RUN mkdir -p /var/log/nginx /var/cache/nginx /etc/nginx/conf.d

# Set proper permissions
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chown -R nginx:nginx /var/log/nginx && \
    chown -R nginx:nginx /var/cache/nginx

# Expose ports
EXPOSE 80 443

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost/health || exit 1

# Start Nginx
CMD ["nginx", "-g", "daemon off;"]
