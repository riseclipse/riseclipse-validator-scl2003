VALIDATOR_ROOT=../fr.centralesupelec.edf.riseclipse.iec61850.scl.validator

case $# in
0) 
    VALIDATOR_VERSION=$(grep -oPm1 '(?<=version>)[^<]*' $VALIDATOR_ROOT/pom.xml)
    JAR_NAME=RiseClipseValidatorSCL-${VALIDATOR_VERSION}.jar
    JAR_PATH=$VALIDATOR_ROOT/target/$JAR_NAME;;
1) 
    JAR_PATH=$1;;
*) 
    echo "Please provide at most 1 argument" 1>&2
    exit 1;;
esac

if [ ! -f $JAR_PATH ]; then
    echo "Please provide a valid jar path" 1>&2
    exit 1
fi
