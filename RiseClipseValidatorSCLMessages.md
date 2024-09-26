# RiseClipseValidatorSCL messages

| Category | Severity | Message |
|:---------------|:---------|:--------|
| NSD/Validation | ERROR | AnyLN type="xxx" class="yyy" has no namespace |
| NSD/Validation | ERROR | LNClassValidator "xxx" not found for LNodeType "yyy" in namespace "ppp" |
| NSD/Validation | ERROR | DO name "xxx" has an instance number, it shouldn't because the presCond of its corresponding DataObject is not multi |
| NSD/Validation/BasicType | ERROR | type of DA/BDA "xxx" is not "yyy", it is "zzz" |
| NSD/Validation/BasicType | ERROR | empty value of Val in DA/BDA "xxx" is not valid for "yyy" type |
| NSD/Validation/BasicType | ERROR | empty value of Val in DAI "xxx" is not valid for "yyy" type |
| NSD/Validation/BasicType | ERROR | value "xxx" of Val in DA/BDA/DAI "yyy" is not a valid "zzz" value |
| NSD/Validation/BasicType | ERROR | empty value of Val in DA/BDA/DAI "yyy" is not a valid "zzz" value |
| NSD/Validation/CDC | ERROR | attribute dchg of DA "xxx" is true while the corresponding one in DataAttribute is false or absent in namespace "ppp" |
| NSD/Validation/CDC | ERROR | attribute qchg of DA "xxx" is true while the corresponding one in DataAttribute is false or absent in namespace "ppp" |
| NSD/Validation/CDC | ERROR | attribute dupd of DA "xxx" is true while the corresponding one in DataAttribute is false or absent in namespace "ppp" |
| NSD/Validation/CDC | ERROR | DA "xxx" has a count attribute while the corresponding DataAttribute has not isArray="true" in namespace "ppp" |
| NSD/Validation/CDC | ERROR | SDO "xxx" has a count attribute while the corresponding SubDataObject has not isArray="true" in namespace "ppp" |
| NSD/Validation/DataAttribute | ERROR | DataAttribute "xxx" is mandatory in DOType with LNClass LLN0 in namespace "ppp" |
| NSD/Validation/DataAttribute | ERROR | DataAttribute "xxx" is mandatory in DOType with LNClass LLN0 in namespace "ppp" |
| NSD/Validation/DataAttribute | ERROR | DataAttribute "xxx" is forbidden in DOType with LNClass "yyy" at line nnn in namespace "ppp" |
| NSD/Validation/DataAttribute | ERROR | DataAttribute "xxx" is mandatory in DOType in namespace "ppp" because there are sibling elements of type AnalogueValue which includes 'i' as a child |
| NSD/Validation/DataAttribute | ERROR | DataAttribute "xxx" is forbidden in DOType in namespace "ppp" because there are no sibling element of type AnalogueValue which includes 'i' as a child |
| NSD/Validation/DataAttribute | ERROR | DataAttribute "xxx" is mandatory in DOType in namespace "ppp" because there are sibling elements of type Vector which includes 'i' as a child of their "ang" attribute |
| NSD/Validation/DataAttribute | ERROR | DataAttribute "xxx" is mandatory in DOType in namespace "ppp" because there are sibling elements of type Vector which includes 'i' as a child of their "mag" attribute |
| NSD/Validation/DataAttribute | ERROR | DataAttribute "xxx" is forbidden in DOType in namespace "ppp" because there are no sibling element of type Vector which includes 'i' as a child of their "ang" attribute |
| NSD/Validation/DataAttribute | ERROR | DataAttribute "xxx" is forbidden in DOType in namespace "ppp" because there are no sibling element of type Vector which includes 'i' as a child of their "mag" attribute |
| NSD/Validation/DataAttribute | ERROR | DA "xxx" not expected in CDC "zzz" in namespace "ppp" |
| NSD/Validation/DataAttribute | ERROR | DA "xxx" already present in CDC "zzz" in namespace "ppp" |
| NSD/Validation/DataAttribute | ERROR | DA "xxx" is mandatory in DOType id = "yyy" with CDC "zzz" in namespace "ppp" |
| NSD/Validation/DataAttribute | ERROR | DA "xxx" is forbidden in DOType id = "yyy" with CDC "zzz" in namespace "ppp" |
| NSD/Validation/DataAttribute | ERROR | group "xxx" has no elements in DOType id = "yyy" with CDC "zzz" in namespace "ppp" (at least one of: "aaa" "bbb") |
| NSD/Validation/DataAttribute | ERROR | DOType id = "yyy" with CDC "zzz" has more than one element marked AtMostOne in namespace "ppp" (at most one of: "aaa" "bbb") |
| NSD/Validation/DataAttribute | ERROR | group "xxx" has neither none nor all elements in DOType id = "yyy" with CDC "zzz" in namespace "ppp" (expected members: "aaa" "bbb") |
| NSD/Validation/DataAttribute | ERROR | DOType id = "yyy" with CDC "zzz" has several groups with all elements in namespace "ppp" |
| NSD/Validation/DataAttribute | ERROR | no group in DOType id = "yyy" with CDC "zzz" has all elements in namespace "ppp" |
| NSD/Validation/DataAttribute | ERROR | DA "xxx" is mandatory in DOType id = "yyy" with CDC "zzz" because sibling "sss" is present in namespace "ppp" |
| NSD/Validation/DataAttribute | ERROR | DA "xxx" is forbidden in DOType id = "yyy" with CDC "zzz" because sibling "sss" is not present in namespace "ppp" |
| NSD/Validation/DataAttribute | ERROR | DA "xxx" is mandatory in DOType id = "yyy" with CDC "zzz" because sibling "sss" is not present in namespace "ppp" |
| NSD/Validation/DataAttribute | ERROR | DA "xxx" is forbidden in DOType id = "yyy" with CDC "zzz" because sibling "sss" is present in namespace "ppp" |
| NSD/Validation/DataObject | ERROR | At least one DO "xxx" is mandatory in LNodeType id "yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | ERROR | DO "xxx" in LNodeType id "yyy" already present in LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | ERROR | DO "xxx" in LNodeType id "yyy" already present with same instance number in LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | ERROR | DO "xxx" in LNodeType id "yyy" already present without instance number in LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | ERROR | DO "xxx" in LNodeType id "yyy" not found in LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | ERROR | DO "xxx" is forbidden in LN0 in LNodeType id "yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | ERROR | DO "xxx" is forbidden in LNodeType id "yyy" with LNClass "zzz" because sibling "sss" is not present in namespace "ppp" |
| NSD/Validation/DataObject | ERROR | DO "xxx" is forbidden in LNodeType id "yyy" with LNClass "zzz" because sibling "sss" is present in namespace "ppp" |
| NSD/Validation/DataObject | ERROR | DO "xxx" is forbidden in LNodeType id "yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | ERROR | DO "xxx" is forbidden in LNodeType id "yyy" with LNClass "zzz" in namespace "ppp", " because sibling "sss" is not present |
| NSD/Validation/DataObject | ERROR | DO "xxx" is mandatory in LN in LNodeType id "yyy" with LNClass "zzz" in the context of a root logical device in namespace "ppp" |
| NSD/Validation/DataObject | ERROR | DO "xxx" is mandatory in LN0 in LNodeType id "yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | ERROR | DO "xxx" is mandatory in LNodeType id "yyy" with LNClass "zzz" because sibling "sss" is not present in namespace "ppp" |
| NSD/Validation/DataObject | ERROR | DO "xxx" is mandatory in LNodeType id "yyy" with LNClass "zzz" because sibling "sss" is present in namespace "ppp" |
| NSD/Validation/DataObject | ERROR | DO "xxx" is mandatory in LNodeType id "yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | ERROR | DO "xxx" is mandatory in LNodeType id "yyy" with LNClass "zzz" in namespace "ppp"," because the namespace of its logical node "nnn" deviates from the namespace of the containing logical device "ddd" |
| NSD/Validation/DataObject | ERROR | DO "xxx" should have an instance number in LNodeType id "yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | ERROR | DO "xxx" should have an instance number in range [mmm, nnn] in LNodeType id "yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | ERROR | DO "xxx" should not have an instance number in LNodeType id "yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | ERROR | group "xxx" has neither none nor all elements in LNodeType id "yyy" with LNClass "zzz" in namespace "ppp" (expected members: "aaa" "bbb") |
| NSD/Validation/DataObject | ERROR | group "xxx" has no elements in LNodeType id "yyy" with LNClass "zzz" in namespace "ppp" (at least one of: "aaa" "bbb") |
| NSD/Validation/DataObject | ERROR | LNodeType id "yyy" with LNClass "zzz" has more than one element marked AtMostOne in namespace "ppp" (at most one of: "aaa" "bbb") |
| NSD/Validation/DataObject | ERROR | LNodeType id "yyy" with LNClass "zzz" has several groups with all elements in namespace "ppp" |
| NSD/Validation/DataObject | ERROR | no group in LNodeType id "yyy" with LNClass "zzz" has all elements in namespace "ppp" |
| NSD/Validation/Enumeration | ERROR | bType of DA/BDA "xxx" is not Enum" |
| NSD/Validation/Enumeration | ERROR | value "xxx" of DA/BDA/DAI "yyy" is not valid for EnumType "zzz" line = nnn in namespace "ppp" |
| NSD/Validation/Enumeration | ERROR | EnumVal with ord "xxx" in EnumType id = "yyy" is not defined as LiteralVal in standard Enumeration "zzz" in namespace "ppp" |
| NSD/Validation/Enumeration | ERROR | EnumVal with ord "xxx" in EnumType id = "yyy" has incorrect name "aaa" instead of "bbb" in standard enumeration "zzz" in namespace "ppp" |
| NSD/Validation/FunctionalConstraint | ERROR | functional contraint "fff" of DA "xxx" is wrong, it should be "ggg" |
| NSD/Validation/SubDataAttribute | ERROR | BDA "xxx" not expected in ConstructedAttribute "zzz" in namespace "ppp" |
| NSD/Validation/SubDataAttribute | ERROR | BDA "xxx" already present in ConstructedAttribute "zzz" in namespace "ppp" |
| NSD/Validation/SubDataAttribute | ERROR | BDA "xxx" is mandatory in DAType id = "yyy" with ConstructedAttribute "zzz" in namespace "ppp" |
| NSD/Validation/SubDataAttribute | ERROR | BDA "xxx" is forbidden in DAType id = "yyy" with ConstructedAttribute "zzz" in namespace "ppp" |
| NSD/Validation/SubDataAttribute | ERROR | group "xxx" has no elements in DAType id = "yyy" with ConstructedAttribute "zzz" in namespace "ppp" (at least one of: "aaa" "bbb") |
| NSD/Validation/SubDataAttribute | ERROR | DAType id = "yyy" with ConstructedAttribute "zzz" has more than one element marked AtMostOne in namespace "ppp" (at most one of: "aaa" "bbb") |
| NSD/Validation/SubDataAttribute | ERROR | group "xxx" has neither none nor all elements in DAType id = "yyy" with ConstructedAttribute "zzz" in namespace "ppp" (expected members: "aaa" "bbb") |
| NSD/Validation/SubDataAttribute | ERROR | DAType id = "yyy" with ConstructedAttribute "zzz" has several groups with all elements in namespace "ppp" |
| NSD/Validation/SubDataAttribute | ERROR | no group in DAType id = "yyy" with ConstructedAttribute "zzz" has all elements in namespace "ppp" |
| NSD/Validation/SubDataAttribute | ERROR | BDA "xxx" is mandatory in DAType id = "yyy" with ConstructedAttribute "zzz" because sibling "sss" is present in namespace "ppp" |
| NSD/Validation/SubDataAttribute | ERROR | BDA "xxx" is forbidden in DAType id = "yyy" with ConstructedAttribute "zzz" because sibling "sss" is not present in namespace "ppp" |
| NSD/Validation/SubDataAttribute | ERROR | BDA "xxx" is mandatory in DAType id = "yyy" with ConstructedAttribute "zzz" because sibling "sss" is not present in namespace "ppp" |
| NSD/Validation/SubDataAttribute | ERROR | BDA "xxx" is forbidden in DAType id = "yyy" with ConstructedAttribute "zzz" because sibling "sss" is present in namespace "ppp" |
| NSD/Validation/SubDataObject | ERROR | SDO "xxx" not expected in CDC "zzz" in namespace "ppp" |
| NSD/Validation/SubDataObject | ERROR | SDO "xxx" already present in CDC "zzz" in namespace "ppp" |
| NSD/Validation/SubDataObject | ERROR | SDO "xxx" is mandatory in DOType id = "yyy" with CDC "zzz" in namespace "ppp" |
| NSD/Validation/SubDataObject | ERROR | SDO "xxx" is forbidden in DOType id = "yyy" with CDC "zzz" in namespace "ppp" |
| NSD/Validation/SubDataObject | ERROR | group "xxx" has no elements in DOType id = "yyy" with CDC "zzz" in namespace "ppp" (at least one of: "aaa" "bbb") |
| NSD/Validation/SubDataObject | ERROR | DOType id = "yyy" with CDC "zzz" has more than one element marked AtMostOne in namespace "ppp" (at most one of: "aaa" "bbb") |
| NSD/Validation/SubDataObject | ERROR | group "xxx" has neither none nor all elements in DOType id = "yyy" with CDC "zzz" in namespace "ppp" (expected members: "aaa" "bbb") |
| NSD/Validation/SubDataObject | ERROR | DOType id = "yyy" with CDC "zzz" has several groups with all elements in namespace "ppp" |
| NSD/Validation/SubDataObject | ERROR | no group in DOType id = "yyy" with CDC "zzz" has all elements in namespace "ppp" |
| NSD/Validation/SubDataObject | ERROR | SDO "xxx" is mandatory in DOType id = "yyy" with CDC "zzz" because sibling "sss" is present in namespace "ppp" |
| NSD/Validation/SubDataObject | ERROR | SDO "xxx" is forbidden in DOType id = "yyy" with CDC "zzz" because sibling "sss" is not present in namespace "ppp" |
| NSD/Validation/SubDataObject | ERROR | SDO "xxx" is mandatory in DOType id = "yyy" with CDC "zzz" because sibling "sss" is not present in namespace "ppp" |
| NSD/Validation/SubDataObject | ERROR | SDO "xxx" is forbidden in DOType id = "yyy" with CDC "zzz" because sibling "sss" is present in namespace "ppp" |
| NSD/Validation/SubDataObject | ERROR | SDO "xxx" is mandatory in DOType because "phsRef" is not Synchrophasor in namespace "ppp" |
| NSD/Validation | WARNING | DO "xxx" is not composed using standardised abbreviations |
| NSD/Validation | WARNING | DO "xxx" use a standard name, but not the standard CDC, it is "yyy", it should be "zzz" |
| NSD/Validation | WARNING | DO "xxx" use a standard name, but is instantiated while the standard one is not |
| NSD/Validation | WARNING | AnyLN type="xxx" class="yyy" is in an unknown namespace "zzz", only partial validation will be done |
| NSD/Validation/CDC | WARNING | DA "xxx" of type "yyy" cannot be verified because there is no validator for it in namespace "ppp" |
| NSD/Validation/CDC | WARNING | DA "xxx" of bType "yyy" cannot be verified because there is no validator for it in namespace "ppp" |
| NSD/Validation/CDC | WARNING | FunctionalConstraint "xxx" of DA "yyy" cannot be verified because there is no validator for it in namespace "ppp" |
| NSD/Validation/CDC | WARNING | SDO name "xxx" is not composed using standardised abbreviations |
| NSD/Validation/CDC | WARNING | while validating DOType: DOType for SDO "xxx" not found in namespace "ppp" |
| NSD/Validation/CDC | WARNING | while validating DOType: validator for SDO "xxx" not found in namespace "ppp" |
| NSD/Validation/CDC | WARNING | DOType id = "yyy" refers to deprecated CDC "xxx" in namespace "ppp" |
| NSD/Validation/ConstructedAttribute | WARNING | while validating DAType: validator for BDA "xxx" of type "yyy" not found in namespace "ppp" |
| NSD/Validation/ConstructedAttribute | WARNING | while validating DAType: validator for BDA "xxx" not found in namespace "ppp" |
| NSD/Validation/ConstructedAttribute | WARNING | DAType id = "yyy" refers to deprecated ConstructedAttribute "xxx" in namespace "ppp" |
| NSD/Validation/DataAttribute | WARNING | DOType id="yyy" refers to deprecated CDC "xxx" in namespace "ppp" |
| NSD/Validation/DataObject | WARNING | verification of PresenceCondition "na" for DO "xxx" is not implemented in LNodeType id="yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | WARNING | verification of PresenceCondition "MFsubst" for DO "xxx" is not implemented in LNodeType id="yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | WARNING | verification of PresenceCondition "MOlnNs" for DO "xxx" is not implemented in LNodeType id="yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | WARNING | verification of PresenceCondition "MOdataNs" for DO "xxx" is not implemented in LNodeType id="yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | WARNING | verification of PresenceCondition "MFscaledAV" for DO "xxx" is not implemented in LNodeType id="yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | WARNING | verification of PresenceCondition "MFscaledMagV" for DO "xxx" is not implemented in LNodeType id="yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | WARNING | verification of PresenceCondition "MFscaledAngV" for DO "xxx" is not implemented in LNodeType id="yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | WARNING | verification of PresenceCondition "MOrms" for DO "xxx" is not implemented in LNodeType id="yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | WARNING | verification of PresenceCondition "MOoperTm" for DO "xxx" is not implemented in LNodeType id="yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | WARNING | verification of PresenceCondition "MmultiF" for DO "xxx" is not implemented in LNodeType id="yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | WARNING | verification of PresenceCondition "MOsbo" for DO "xxx" is not implemented in LNodeType id="yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | WARNING | verification of PresenceCondition "MOenhanced" for DO "xxx" is not implemented in LNodeType id="yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | WARNING | verification of PresenceCondition "MORange" for DO "xxx" is not implemented in LNodeType id="yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | WARNING | verification of PresenceCondition "OMSynPh" for DO "xxx" is not implemented in LNodeType id="yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | WARNING | verification of PresenceCondition "MAllOrNonePerGroup" for DO "xxx" is not implemented in LNodeType id="yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | WARNING | verification of PresenceCondition "MOctrl" for DO "xxx" is not implemented in LNodeType id="yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | WARNING | verification of PresenceCondition "MOsboNormal" for DO "xxx" is not implemented in LNodeType id="yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | WARNING | verification of PresenceCondition "MOsboEnhanced" for DO "xxx" is not implemented in LNodeType id="yyy" with LNClass "zzz" in namespace "ppp" |
| NSD/Validation/DataObject | WARNING | LNodeType id="yyy" refers to deprecated LNClass "xxx" in namespace "ppp" |
| NSD/Validation/DataObject | WARNING | DO "xxx"" in LNodeType id = "yyy" is deprecated in "zzz" in namespace "ppp" |
| NSD/Validation/Enumeration | WARNING | EnumVal with name "xxx" in EnumType id = "yyy" is deprecated in standard enumeration "zzz" in namespace "ppp" |
| NSD/Validation/LNClass | WARNING | DOType id = "yyy" at line nnn used by DO "xxx"" has wrong CDC "aaa", it should be "bbb" in namespace "ppp" |
| NSD/Validation/LNClass | WARNING | DO "xxx"" cannot be verified because there is no validator for it in namespace "ppp" |
| NSD/Validation/SubDataAttribute | WARNING | DAType id="yyy" refers to deprecated ConstructedAttribute "xxx" in namespace "ppp" |
| NSD/Validation/SubDataObject | WARNING | verification of PresenceCondition "OMSynPh" for SDO "xxx" for DOType: no value for "phsRef" in namespace "ppp" |
| NSD/Validation/SubDataObject | WARNING | verification of PresenceCondition "OMSynPh" for SDO "xxx" for DOType: multiple values for "phsRef" in namespace "ppp" |
| NSD/Validation/SubDataObject | WARNING | verification of PresenceCondition "OMSynPh" for SDO "xxx" for DOType: DA "phsRef" not found in namespace "ppp" |
| NSD/Validation/SubDataObject | WARNING | DOType id="yyy" refers to deprecated CDC "xxx" in namespace "ppp" |
