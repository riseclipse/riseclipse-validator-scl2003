FROM eclipse-temurin:11 as jre-build

# Create a custom Java runtime
RUN $JAVA_HOME/bin/jlink \
         --add-modules java.base,java.desktop,java.logging,java.xml \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /javaruntime

# Base image
FROM debian:bookworm-slim

## environment settings
ENV HOME="/config"
ENV TZ=Europe/Paris
ARG DEBIAN_FRONTEND=noninteractive

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH "${JAVA_HOME}/bin:${PATH}"
COPY --from=jre-build /javaruntime $JAVA_HOME

RUN \
     apt-get update \
  && apt-get install -y \
       python3 \
       python3-pytest \
       python3-pytest-xdist \
       python3-jinja2 \
       zip \
  && ln -s /usr/bin/python3 /usr/bin/python \
  && apt-get clean \
  && rm -rf \
       /tmp/* \
       /var/lib/apt/lists/* \
       /var/tmp/*

# "Download artifact" action creates subdirectory named from the artifact's name
COPY artifacts/RiseClipseValidatorSCL-*/RiseClipseValidatorSCL-*.jar /usr/riseclipse/bin/RiseClipseValidatorSCL.jar

WORKDIR /usr/riseclipse

CMD java -jar bin/RiseClipseValidatorSCL.jar data/*
