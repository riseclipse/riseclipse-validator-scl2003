FROM openjdk:11.0.13-jre-slim

COPY RiseClipseValidatorSCL-*.jar /home

# Create environment variable
RUN RCV="/home/RiseClipseValidatorSCL-*.jar"

# Execute CLI
ENTRYPOINT java -jar ./home/RiseClipseValidatorSCL-*.jar

#Default command (version)
CMD -version