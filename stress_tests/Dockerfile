# JMeter Dockerfile for Stress Testing
FROM alpine:3.18

# Install Java and JMeter
RUN apk add --no-cache \
    openjdk11-jre \
    curl \
    bash \
    && rm -rf /var/cache/apk/*

# Set JMeter version and download
ENV JMETER_VERSION=5.5
ENV JMETER_HOME=/opt/apache-jmeter-${JMETER_VERSION}
ENV PATH=${JMETER_HOME}/bin:${PATH}

# Download and install JMeter
RUN curl -L https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-${JMETER_VERSION}.tgz | tar -xz -C /opt/ \
    && chmod +x ${JMETER_HOME}/bin/*.sh

# Create working directory
WORKDIR /tests

# Copy test plans and data
COPY test-plans/ /tests/test-plans/
COPY test-data/ /tests/test-data/

# Create results directory
RUN mkdir -p /tests/results

# Set JMeter properties for better performance
RUN echo "jmeter.save.saveservice.output_format=csv" >> ${JMETER_HOME}/bin/jmeter.properties \
    && echo "jmeter.save.saveservice.response_data=true" >> ${JMETER_HOME}/bin/jmeter.properties \
    && echo "jmeter.save.saveservice.samplerData=true" >> ${JMETER_HOME}/bin/jmeter.properties \
    && echo "jmeter.save.saveservice.requestHeaders=true" >> ${JMETER_HOME}/bin/jmeter.properties \
    && echo "jmeter.save.saveservice.responseHeaders=true" >> ${JMETER_HOME}/bin/jmeter.properties

# Default command
CMD ["jmeter", "--version"]