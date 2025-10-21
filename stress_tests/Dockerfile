# JMeter Dockerfile for Stress Testing
FROM alpine:3.18

# Install Java and JMeter
RUN apk add --no-cache openjdk11-jre-headless curl

# Set JMeter version
ARG JMETER_VERSION=5.6.2
ARG JMETER_HOME=/opt/apache-jmeter-${JMETER_VERSION}
ARG JMETER_DOWNLOAD_URL=https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-${JMETER_VERSION}.tgz

# Download and install JMeter
RUN mkdir -p /opt && \
    curl -L --silent ${JMETER_DOWNLOAD_URL} | tar -xz -C /opt && \
    ln -s ${JMETER_HOME} /opt/jmeter

# Add JMeter to PATH
ENV PATH="${JMETER_HOME}/bin:${PATH}"

# Create working directory
WORKDIR /tests

# Copy test plans
COPY test-plans/ /tests/test-plans/
COPY results/ /tests/results/

# Create entrypoint script
RUN echo '#!/bin/sh' > /entrypoint.sh && \
    echo 'exec "$@"' >> /entrypoint.sh && \
    chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
CMD ["jmeter", "--version"]
