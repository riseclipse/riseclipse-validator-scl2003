FROM openjdk:11.0.13-jre-slim

# Download artifact places jars in nested folder with the same name for some reason
COPY RiseClipseValidatorSCL-*.jar/RiseClipseValidatorSCL-*.jar /home

# Create environment variable
RUN SCLValidator="/home/RiseClipseValidatorSCL-*.jar"

# Execute CLI
ENTRYPOINT java -jar ./home/RiseClipseValidatorSCL-*.jar

#Default command (version)
CMD -version