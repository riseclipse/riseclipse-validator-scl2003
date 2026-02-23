# *************************************************************************
# **  Copyright (c) 2026 CentraleSupélec & EDF.
# **  All rights reserved. This program and the accompanying materials
# **  are made available under the terms of the Eclipse Public License v2.0
# **  which accompanies this distribution, and is available at
# **  https://www.eclipse.org/legal/epl-v20.html
# **
# **  This file is part of the RiseClipse tool
# **
# **  Contributors:
# **      Computer Science Department, CentraleSupélec
# **      EDF R&D
# **  Contacts:
# **      dominique.marcadet@centralesupelec.fr
# **      aurelie.dehouck-neveu@edf.fr
# **  Web site:
# **      https://riseclipse.github.io
# *************************************************************************

FROM eclipse-temurin:21 AS jre-build

# Create a custom Java runtime
RUN                                         \
     $JAVA_HOME/bin/jlink                   \
         --add-modules java.base,java.desktop,java.logging,java.xml \
         --strip-debug                      \
         --no-man-pages                     \
         --no-header-files                  \
         --compress=2                       \
         --output /javaruntime

# Base image
FROM debian:bookworm-slim

## environment settings
ENV HOME="/config"
ENV TZ=Europe/Paris
ARG DEBIAN_FRONTEND=noninteractive

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=jre-build /javaruntime $JAVA_HOME

RUN apt update \
    && apt install -y curl \
        texlive-latex-base make zip texlive-latex-base \
        texlive-latex-extra latexmk tex-gyre \
     && apt-get clean \
     && rm -rf \
          /tmp/* \
          /var/lib/apt/lists/* \
          /var/tmp/* \
     && update-alternatives --install /usr/bin/java java /opt/java/openjdk/bin/java 2000
COPY --from=ghcr.io/astral-sh/uv:0.6.0 /uv /uvx /bin/

ARG RISECLIPSE_PROJECT=riseclipse-validator-scl2003
ARG RISECLIPSE_TOOL=RiseClipseValidatorSCL

RUN \
       echo "/usr/riseclipse/bin/${RISECLIPSE_TOOL}.jar" \
    && echo "https://github.com/riseclipse/${RISECLIPSE_PROJECT}/releases/download/${RISECLIPSE_PROJECT}-${RELEASE_VERSION}/${RISECLIPSE_TOOL}-${RELEASE_VERSION}.jar" \
    && curl -L -o "/usr/riseclipse/bin/RiseClipseValidatorSCL.jar" "https://github.com/riseclipse/riseclipse-validator-scl2003/releases/download/riseclipse-validator-scl2003-1.3.0-SNAPSHOT/RiseClipseValidatorSCL-1.3.0-SNAPSHOT.jar" \
    && curl -L -o "/usr/riseclipse/bin/${RISECLIPSE_TOOL}.jar" "https://github.com/riseclipse/${RISECLIPSE_PROJECT}/releases/download/${RISECLIPSE_PROJECT}-${RELEASE_VERSION}/${RISECLIPSE_TOOL}-${RELEASE_VERSION}.jar"


WORKDIR /usr/riseclipse

CMD java -jar bin/RiseClipseValidatorSCL.jar data/*
