# *************************************************************************
# **  Copyright (c) 2022-24 CentraleSupélec & EDF.
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
FROM debian:buster-slim

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
          /var/tmp/*
COPY --from=ghcr.io/astral-sh/uv:0.6.0 /uv /uvx /bin/


# "Download artifact" action creates subdirectory named from the artifact's name
COPY artifacts/RiseClipseValidatorSCL-*/RiseClipseValidatorSCL-*.jar /usr/riseclipse/bin/RiseClipseValidatorSCL.jar

WORKDIR /usr/riseclipse

CMD java -jar bin/RiseClipseValidatorSCL.jar data/*
