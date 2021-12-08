VALIDATOR_VERSION=$(grep -oPm1 '(?<=version>)[^<]*' ../pom.xml)
JAR_NAME=RiseClipseValidatorSCL-${VALIDATOR_VERSION}.jar
JAR_PATH=../target/$JAR_NAME
