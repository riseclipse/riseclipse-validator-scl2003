
## 1.2.7 (2024/03/29)
- update to Eclipse 2024-03
- complete review of NSD validation
- recognized IEC WG10-OCL-TF formatted OCL messages
- catch all Java exceptions to avoid them being printed
- a new command line option allows different exit codes according to the severity of validation messages (issue riseclipse/riseclipse-validator-scl2003#134)
- add an explicit link from an LN to a supervised ControlWithIEDName (issue riseclipse/riseclipse-metamodel-scl2003#38)
- improve naming of elements when SCL metamodel is used as an Eclipse plugin (issue riseclipse/riseclipse-metamodel-scl2003#39)
- add new naming attributes (issue riseclipse/riseclipse-metamodel-scl2003#40)
- change the level of some messages to improve lisibility of output

## 1.2.6 (2023/11/06)
- update to Eclipse 2023-09, Java 17 is now required
- deployment is now also done on [EDF RiseClipse Web](https://rise-clipse.pam-retd.fr)
- The validator exits now with a non zero return code (EXIT_FAILURE = 1) if any validation error is detected (issue riseclipse/riseclipse-validator-scl2003#112)
- The docker image use now Eclipse Temurin (issue riseclipse/riseclipse-validator-scl2003#113)

## 1.2.5 (2023/03/23)
- support for zip files given on the command line (issue riseclipse/riseclipse-validator-scl2003#105)
- update to Eclipse 2023-03

## 1.2.4 (2022/07/12)
- support for extensions LNClasses (used in IEC 61869-9:2016)
- support parameterized CDC and ServiceConstructedAttribute
- take into account namespace specified in a DAI
- no error if EnumDA is used as underlyingType
- handle MFscaledAV, MFscaledMagV and MFscaledAngV presence conditions

## 1.2.3 (2022/05/31)
- SCL objects know their filename, it allows OCL constraints to depend on the kind of file (ICD, SCD…) using the filename extension

## 1.2.2 (2022/05/16)
- support for multiple NSD namespaces
- take into account whether an AnyLNClass is statistic or not for presence conditions
- compute missing link from LogControl to Log and remove useless link from LogControl to AnyLN

## 1.2.1 (2022/04/19)
- the output of the tool can be customized with a command line option
- add sonar analysis of code
- trigger functional tests

## 1.2.0 (2022/04/11)
- setup workflows on GitHub for automatic build and deployment
- use RiseClipse components on Maven Central to build validators

## 1.1.0a24 (2022/01/20)
- use JavaSE-11 and lastest Eclipse components
- correct some deprecated calls

## 1.1.0a23 (2021/10/13)
- files whose name starts with a dot are ignored by default, an option allows to take them into account (issue riseclipse/riseclipse-validator-scl2003#13)
- use last dot in filename to get extension (issue riseclipse/riseclipse-validator-scl2003#11)

## 1.1.0a22 (2021/10/01)
- handle directories on command line

## 1.1.0a21 (2021/04/12)
- remove possible NPE (issue riseclipse/riseclipse-validator-scl2003#4)
- add an option to validate with an XML Schema
- improve loading performance
- correct namespace access

## 1.1.0a20 (2021/02/26)
- new way to get Eclipse plugins for adding to fat jars
- use an Eclipse target platform definition for reproductible builds
- add validation of functional constraints (issue riseclipse/riseclipse-validator-scl2003#2)
- remove error on unknown XML namespace
- default value of bufMode in ConfReportControl set to 'both' (issue riseclipse/riseclipse-metamodel-scl2003#2)
- add a namespace attribute to relevant SCL objects
- take into accounts SDO for explicit link from FCDA (issue riseclipse/riseclipse-metamodel-scl2003#4)
- private content is now just text (issue riseclipse/riseclipse-metamodel-scl2003#1 and riseclipse/riseclipse-metamodel-scl2003#5)
- correct ClientLN explicit link (issue riseclipse/riseclipse-metamodel-scl2003#6)

## 1.1.0a19 (2020/03/31)
- avoid NPE when lnInst is missing in ExtRef (issue [riseclipse-metamodel-scl2003#64](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-metamodel-scl2003/-/issues/64)
- put back inadvertently removed code of DOIImpl.getNamespace (issue [riseclipse-metamodel-scl2003#65](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-metamodel-scl2003/-/issues/65))
- do not considered digits at the end of a DO name as an instance number (issues [#30](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-validator-scl2003/-/issues/30) and [#31](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-validator-scl2003/-/issues/31))

## 1.1.0a18 (2020/02/19)
- solve multiple displays of OCL messages (issue [#26](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-validator-scl2003/-/issues/26))
- use of FeatureMap in SCL metamodel removed (issue [riseclipse-metamodel-scl2003#63](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-metamodel-scl2003/-/issues/63))

## 1.1.0a17 (2020/02/12)
- add explicit links for LDevice hierarchy using GrRef (issue [riseclipse-metamodel-scl2003#46](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-metamodel-scl2003/-/issues/46))
- validation of enumeration revisited (issue [#23](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-validator-scl2003/-/issues/23))


## 1.1.0a16 (2020/02/09)
- add environment variables as an alternative to options on command line; this is needed for docker use (issue [#29](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-validator-scl2003/-/issues/29))

## 1.1.0a15 (2020/02/04)
- correct atLeastOne presence condition (issue [#28](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-validator-scl2003/-/issues/28))

## 1.1.0a14 (2019/10/24)
- take into account prefixes of OCL messages to classify them as error or warning (issue [#27](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-validator-scl2003/-/issues/27))

## 1.1.0a13 (2019/07/26)
- switch to EPL 2.0 licence (issue [#21](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-validator-scl2003/-/issues/21))
- correct EnumType must not use a different id if it is identical to the NSD one (issue [#22](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-validator-scl2003/-/issues/22))

## 1.1.0a11 (2019/07/04)
- improve README (issue [#20](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-validator-scl2003/-/issues/20))
- allow for NSD validation on multiple SCL files (issue [#18](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-validator-scl2003/-/issues/18))

## 1.1.0a10 (2019/07/01)
- correct command line parsing (issue [#17](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-validator-scl2003/-/issues/17))

## 1.1.0a9 (2019/06/28)
- add --output option (issue [#16](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-validator-scl2003/-/issues/16))
- add some missing line numbers in message  (issue [#15](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-validator-scl2003/-/issues/15))
- update GUI application for NSD files (issue [#14](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-validator-scl2003/-/issues/14))

## 1.1.0a7 (2019/06/05)
- allow for enumeration name to differ (issue [#13](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-validator-scl2003/-/issues/13))


## 1.1.0a6 (2019/05/31)
- first version with NSD validation (issue [#11](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-validator-scl2003/-/issues/11)) made available (issue [#12](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-validator-scl2003/-/issues/12))
- add warning level (issue [#10](https://gitlab-research.centralesupelec.fr/RiseClipseGroup/riseclipse-validator-scl2003/-/issues/10))
