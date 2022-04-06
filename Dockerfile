FROM adoptopenjdk:11.0.11_9-jre-hotspot

# "Download artifact" action creates subdirectory named from the artifact's name
COPY artifacts/RiseClipseValidatorSCL-*.jar/RiseClipseValidatorSCL-*.jar /usr/riseclipse/bin/RiseClipseValidatorSCL.jar

WORKDIR /usr/riseclipse

CMD java -jar bin/RiseClipseValidatorSCL.jar data/*
