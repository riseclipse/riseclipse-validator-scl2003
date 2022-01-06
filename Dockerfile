FROM openjdk:11.0.13-jre-slim

ARG GH_WS

COPY /${GH_WS}/RiseClipse-Validator-SCL-CLI.jar /home

# Create environment variable
RUN RCV="/home/RiseClipse-Validator-SCL-CLI.jar"

# Execute CLI
ENTRYPOINT java -jar ./home/RiseClipse-Validator-SCL-CLI.jar

#Default command (version)
CMD -version