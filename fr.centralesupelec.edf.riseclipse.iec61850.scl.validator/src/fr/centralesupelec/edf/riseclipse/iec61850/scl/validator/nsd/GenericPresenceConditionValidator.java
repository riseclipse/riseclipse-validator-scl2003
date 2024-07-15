/*
*************************************************************************
**  Copyright (c) 2019-2022 CentraleSupélec & EDF.
**  All rights reserved. This program and the accompanying materials
**  are made available under the terms of the Eclipse Public License v2.0
**  which accompanies this distribution, and is available at
**  https://www.eclipse.org/legal/epl-v20.html
** 
**  This file is part of the RiseClipse tool
**  
**  Contributors:
**      Computer Science Department, CentraleSupélec
**      EDF R&D
**  Contacts:
**      dominique.marcadet@centralesupelec.fr
**      aurelie.dehouck-neveu@edf.fr
**  Web site:
**      https://riseclipse.github.io/
*************************************************************************
*/
package fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.nsd;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.Doc;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.DocumentedClass;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentification;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.IDNaming;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.SclObject;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.RiseClipseMessage;

public abstract class GenericPresenceConditionValidator< NsdModel extends DocumentedClass, SclModel extends IDNaming, @Nullable SclComponent extends SclObject > {
    
    // Name of the NsdComponent/SclComponent, SclComponent
    protected HashMap< String, SclComponent > presentSclComponent = new HashMap<>();
    
    protected HashSet< String > mandatory;
    protected HashSet< String > optional;
    protected HashSet< String > forbidden;
    protected HashSet< String > notApplicable;
    protected HashSet< String > mandatoryMulti;
    protected HashSet< String > optionalMulti;
    protected HashMap< Integer, HashSet< String > > atLeastOne;
    protected HashSet< String > atMostOne;
    protected HashMap< Integer, HashSet< String > > allOrNonePerGroup;
    protected HashMap< Integer, HashSet< String > > allOnlyOneGroup;
    protected HashMap< Integer, HashSet< String > > allAtLeastOneGroup;
    protected HashMap< String, String > mandatoryIfSiblingPresentElseForbidden;
    protected HashMap< String, String > mandatoryIfSiblingPresentElseOptional;
    protected HashMap< String, String > optionalIfSiblingPresentElseMandatory;
    protected HashMap< String, String > forbiddenIfSiblingPresentElseMandatory;
    protected HashMap< String, String > mandatoryIfTextConditionElseOptional;
    protected HashMap< String, String > mandatoryIfTextConditionElseOptionalDoc;
    protected HashMap< String, String > mandatoryIfTextConditionElseForbidden;
    protected HashMap< String, String > mandatoryIfTextConditionElseForbiddenDoc;
    protected HashMap< String, String > optionalIfTextConditionElseForbidden;
    protected HashMap< String, String > optionalIfTextConditionElseForbiddenDoc;
    protected HashMap< String, Pair< Integer, Integer > > mandatoryMultiRange;
    protected HashMap< String, Pair< Integer, Integer > > optionalMultiRange;
    protected HashSet< String > mandatoryIfSubstitutionElseForbidden;
    protected HashSet< String > mandatoryInLLN0ElseOptional;
    protected HashSet< String > mandatoryInLLN0ElseForbidden;
    protected HashSet< String > mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional;
    protected HashSet< String > mandatoryIfNameSpaceOfDataClassDeviatesElseOptional;
    protected HashSet< String > mandatoryIfNameSpaceOfDataObjectDeviatesElseOptional;
    protected HashSet< String > mandatoryIfAnalogValueIncludesIElseForbidden;
    protected HashSet< String > mandatoryIfVectorSiblingIncludesIAsChildMagElseForbidden;
    protected HashSet< String > mandatoryIfVectorSiblingIncludesIAsChildAngElseForbidden;
    protected HashSet< String > mandatoryIfHarmonicValuesCalculatedAsRatioElseOptional;
    protected HashSet< String > mandatoryInRootLogicalDeviceElseOptional;
    protected HashSet< String > mandatoryIfControlSupportsTimeElseOptional;
    protected HashMap< String, String > oneOrMoreIfSiblingPresentElseForbidden;
    protected HashSet< String > mandatoryIfControlSupportsSecurity1ElseOptional;
    protected HashSet< String > mandatoryIfControlSupportsSecurity2ElseOptional;
    protected HashMap< String, String > optionalIfSiblingPresentElseForbidden;
    protected HashSet< String > mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional2;
    protected HashSet< String > mandatoryIfMeasuredValueExposesRange;
    protected HashSet< String > optionalIfPhsRefIsSynchrophasorElseMandatory;
    protected HashSet< String > mAllOrNonePerGroup;
    protected HashSet< String > mOctrl;
    protected HashSet< String > mOsboNormal;
    protected HashSet< String > mOsboEnhanced;
    
    protected final IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();

    protected NsIdentification nsIdentification;
    protected NsdModel nsdModel;
    
    protected HashSet< String > deprecated = new HashSet<>();

    protected GenericPresenceConditionValidator( NsIdentification nsIdentification, NsdModel nsdModel ) {
        this.nsIdentification = nsIdentification;
        this.nsdModel = nsdModel;
    }
    
    protected void initialize() {
        // Was initially in constructor.
        // But a subclass may need to be constructed before building specifications. 
        createSpecifications();
        checkSpecification();
    }
    
    protected abstract String getSetupMessageCategory();
    protected abstract String getValidationMessageCategory();

    protected abstract void createSpecifications();

    protected abstract String getPresenceConditionValidatorName();

    protected abstract String getNsdModelName();
    protected abstract int    getNsdModelLineNumber();
    protected abstract String getNsdModelClassName();
    protected abstract String getNsdComponentClassName();

    protected abstract String getSclModelClassName();
    protected abstract String getSclComponentClassName();

    protected void addSpecification( String name, String presCond, String presCondArgs, Doc doc, int lineNumber, String filename ) {
        if( presentSclComponent.containsKey( name )) {
            console.warning( getSetupMessageCategory(), filename, lineNumber,
                             name, " has already been added to ", getPresenceConditionValidatorName() );
            return;
        }
        console.debug( getSetupMessageCategory(), filename, lineNumber,
                "adding ", getSclComponentClassName(), " \"", name, "\" to ", getNsdModelClassName(), " \"", getNsdModelName(), "\"" );
        presentSclComponent.put( name, null );

        switch( presCond ) {
        case "M" :
            // Element is mandatory
            if( mandatory == null ) mandatory = new HashSet<>();
            mandatory.add( name );
            break;
        case "O" :
            // Element is optional
            if( optional == null ) optional = new HashSet<>();
            optional.add( name );
            break;
        case "F" :
            // Element is forbidden
            if( forbidden == null ) forbidden = new HashSet<>();
            forbidden.add( name );
            break;
        case "na" :
            // Element is not applicable
            // -> TODO: what does it mean ? what do we have to check ?
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: ", getNsdComponentClassName(), " \"", name, "\" declared as \"na\" in PresenceCondition" );
//            if( notApplicable == null ) notApplicable = new HashSet<>();
//            notApplicable.add( name );
            break;
        case "Mmulti" :
            // At least one element shall be present; all instances have an instance number > 0
            // -> TODO: not sure what is the instance number, it is assumed to be the suffix of DO name
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: ", getNsdComponentClassName(), " \"", name, "\" declared as \"Mmulti\" in PresenceCondition" );
//            if( mandatoryMulti == null ) mandatoryMulti = new HashSet<>();
//            mandatoryMulti.add( name );
            break;
        case "Omulti" :
            // Zero or more elements may be present; all instances have an instance number > 0
            // -> TODO: not sure what is the instance number, it is assumed to be the suffix of DO name
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: ", getNsdComponentClassName(), " \"", name, "\" declared as \"Omulti\" in PresenceCondition" );
//            if( optionalMulti == null ) optionalMulti = new HashSet<>();
//            optionalMulti.add( name );
            break;
        case "AtLeastOne" :
            // Parameter n: group number (> 0).
            // At least one of marked elements of a group n shall be present
            if( atLeastOne == null ) atLeastOne = new HashMap<>();
            try {
                Integer arg = Integer.valueOf( presCondArgs );
                if( arg <= 0 ) {
                    console.warning( getSetupMessageCategory(), filename, lineNumber,
                            "argument of PresenceCondition \"AtLeastOne\" is not a positive integer" );
                    break;
                }
                if( ! atLeastOne.containsKey( arg )) {
                    atLeastOne.put( arg, new HashSet<>() );
                }
                atLeastOne.get( arg ).add( name );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( getSetupMessageCategory(), filename, lineNumber,
                        "argument of PresenceCondition \"AtLeastOne\" is not an integer" );
                break;
            }
        case "AtMostOne" :
            // At most one of marked elements shall be present
            if( atMostOne == null ) atMostOne = new HashSet<>();
            atMostOne.add( name );
            break;
        case "AllOrNonePerGroup" :
            // Parameter n: group number (> 0).
            // All or none of the elements of a group n shall be present
            if( allOrNonePerGroup == null ) allOrNonePerGroup = new HashMap<>();
            try {
                Integer arg = Integer.valueOf( presCondArgs );
                if( arg <= 0 ) {
                    console.warning( getSetupMessageCategory(), filename, lineNumber,
                            "argument of PresenceCondition \"AllOrNonePerGroup\" is not a positive integer" );
                    break;
                }
                if( ! allOrNonePerGroup.containsKey( arg )) {
                    allOrNonePerGroup.put( arg, new HashSet<>() );
                }
                allOrNonePerGroup.get( arg ).add( name );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( getSetupMessageCategory(), filename, lineNumber,
                        "argument of PresenceCondition \"AllOrNonePerGroup\" is not an integer" );
                break;
            }
        case "AllOnlyOneGroup" :
            // Parameter n: group number (> 0).
            // All elements of only one group n shall be present
            if( allOnlyOneGroup == null ) allOnlyOneGroup = new HashMap<>();
            try {
                Integer arg = Integer.valueOf( presCondArgs );
                if( arg <= 0 ) {
                    console.warning( getSetupMessageCategory(), filename, lineNumber,
                            "argument of PresenceCondition \"AllOnlyOneGroup\" is not a positive integer" );
                    break;
                }
                if( ! allOnlyOneGroup.containsKey( arg )) {
                    allOnlyOneGroup.put( arg, new HashSet<>() );
                }
                allOnlyOneGroup.get( arg ).add( name );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( getSetupMessageCategory(), filename, lineNumber,
                        "argument of PresenceCondition \"AllOnlyOneGroup\" is not an integer" );
                break;
            }
        case "AllAtLeastOneGroup" :
            // Parameter n: group number (> 0).
            // All elements of at least one group n shall be present
            if( allAtLeastOneGroup == null ) allAtLeastOneGroup = new HashMap<>();
            try {
                Integer arg = Integer.valueOf( presCondArgs );
                if( arg <= 0 ) {
                    console.warning( getSetupMessageCategory(), filename, lineNumber,
                            "argument of PresenceCondition \"AllAtLeastOneGroup\" is not a positive integer" );
                    break;
                }
                if( ! allAtLeastOneGroup.containsKey( arg )) {
                    allAtLeastOneGroup.put( arg, new HashSet<>() );
                }
                allAtLeastOneGroup.get( arg ).add( name );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( getSetupMessageCategory(), filename, lineNumber,
                        "argument of PresenceCondition \"AllAtLeastOneGroup\" is not an integer" );
                break;
            }
        case "MF" :
            // Parameter sibling: sibling element name.
            // Mandatory if sibling element is present, otherwise forbidden
            if( mandatoryIfSiblingPresentElseForbidden == null ) mandatoryIfSiblingPresentElseForbidden = new HashMap<>();
            mandatoryIfSiblingPresentElseForbidden.put( name, presCondArgs );
            break;
        case "MO" :
            // Parameter sibling: sibling element name.
            // Mandatory if sibling element is present, otherwise optional
            if( mandatoryIfSiblingPresentElseOptional == null ) mandatoryIfSiblingPresentElseOptional = new HashMap<>();
            mandatoryIfSiblingPresentElseOptional.put( name, presCondArgs );
            break;
        case "OM" :
            // Parameter sibling: sibling element name.
            // Optional if sibling element is present, otherwise mandatory
            if( optionalIfSiblingPresentElseMandatory == null ) optionalIfSiblingPresentElseMandatory = new HashMap<>();
            optionalIfSiblingPresentElseMandatory.put( name, presCondArgs );
            break;
        case "FM" :
            // Parameter sibling: sibling element name.
            // Forbidden if sibling element is present, otherwise mandatory
            if( forbiddenIfSiblingPresentElseMandatory == null ) forbiddenIfSiblingPresentElseMandatory = new HashMap<>();
            forbiddenIfSiblingPresentElseMandatory.put( name, presCondArgs );
            break;
        case "MOcond" :
            // Parameter condID: condition number (> 0).
            // Textual presence condition (non-machine processable) with reference condID to context specific text.
            // If satisfied, the element is mandatory, otherwise optional
            if( mandatoryIfTextConditionElseOptional == null ) mandatoryIfTextConditionElseOptional = new HashMap<>();
            if( mandatoryIfTextConditionElseOptionalDoc == null ) mandatoryIfTextConditionElseOptionalDoc = new HashMap<>();
            try {
                Integer arg = Integer.valueOf( presCondArgs );
                if( arg <= 0 ) {
                    console.warning( getSetupMessageCategory(), filename, lineNumber,
                            "argument of PresenceCondition \"MOcond\" is not a positive integer" );
                    break;
                }
                mandatoryIfTextConditionElseOptional.put( name, presCondArgs );
                mandatoryIfTextConditionElseOptionalDoc.put( name, doc.getMixed().get( 0 ).getValue().toString() );
                
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( getSetupMessageCategory(), filename, lineNumber,
                                 "argument of PresenceCondition \"MOcond\" is not an integer" );
                break;
            }
        case "MFcond" :
            // Parameter condID: condition number (> 0).
            // Textual presence condition (non-machine processable) with reference condID to context specific text.
            // If satisfied, the element is mandatory, otherwise forbidden
            if( mandatoryIfTextConditionElseForbidden == null ) mandatoryIfTextConditionElseForbidden = new HashMap<>();
            if( mandatoryIfTextConditionElseForbiddenDoc == null ) mandatoryIfTextConditionElseForbiddenDoc = new HashMap<>();
            try {
                Integer arg = Integer.valueOf( presCondArgs );
                if( arg <= 0 ) {
                    console.warning( getSetupMessageCategory(), filename, lineNumber,
                            "argument of PresenceCondition \"MFcond\" is not a positive integer" );
                    break;
                }
                mandatoryIfTextConditionElseForbidden.put( name, presCondArgs );
                mandatoryIfTextConditionElseForbiddenDoc.put( name, doc.getMixed().get( 0 ).getValue().toString() );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( getSetupMessageCategory(), filename, lineNumber,
                        "argument of PresenceCondition \"MFcond\" is not an integer" );
                break;
            }
        case "OFcond" :
            // Parameter condID: condition number (> 0).
            // Textual presence condition (non-machine processable) with reference condID to context specific text.
            // If satisfied, the element is optional, otherwise forbidden
            if( optionalIfTextConditionElseForbidden == null ) optionalIfTextConditionElseForbidden = new HashMap<>();
            if( optionalIfTextConditionElseForbiddenDoc == null ) optionalIfTextConditionElseForbiddenDoc = new HashMap<>();
            try {
                Integer arg = Integer.valueOf( presCondArgs );
                if( arg <= 0 ) {
                    console.warning( getSetupMessageCategory(), filename, lineNumber,
                            "argument of PresenceCondition \"OFcond\" is not a positive integer" );
                    break;
                }
                optionalIfTextConditionElseForbidden.put( name, presCondArgs );
                optionalIfTextConditionElseForbiddenDoc.put( name, doc.getMixed().get( 0 ).getValue().toString() );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( getSetupMessageCategory(), filename, lineNumber,
                                 "argument of PresenceCondition \"OFcond\" is not an integer" );
                break;
            }
        case "MmultiRange" :
            // Parameters min, max: limits for instance number (> 0).
            // One or more elements shall be present; all instances have an instance number within range [min, max] (see IEC 61850-7-1)
            // -> TODO: not sure what is the instance number, it is assumed to be the suffix of DO name
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: " + getNsdComponentClassName(), " \"", name, "\" declared as \"MmultiRange\" in PresenceCondition" );
            if( mandatoryMultiRange == null ) mandatoryMultiRange = new HashMap<>();
            String[] limits1 = presCondArgs.split( "[ ,]+" );
            if( limits1.length != 2 ) {
                console.warning( getSetupMessageCategory(), filename, lineNumber,
                        "argument of PresenceCondition \"MmultiRange\" is not two integers" );
                break;
            }
            Integer min1 = Integer.valueOf( limits1[0] );
            if( min1 <= 0 ) {
                console.warning( getSetupMessageCategory(), filename, lineNumber,
                        "first argument of PresenceCondition \"MmultiRange\" is not a positive integer" );
                break;
            }
            Integer max1 = Integer.valueOf( limits1[1] );
            if( max1 <= 0 ) {
                console.warning( getSetupMessageCategory(), filename, lineNumber,
                        "second argument of PresenceCondition \"MmultiRange\" is not a positive integer" );
                break;
            }
            mandatoryMultiRange.put( name, Pair.of( min1, max1 ));
            break;
        case "OmultiRange" :
            // Parameters min, max: limits for instance number (> 0).
            // Zero or more elements may be present; all instances have an instance number within range [min, max] (see IEC 61850-7-1)
            // -> TODO: not sure what is the instance number, it is assumed to be the suffix of DO name
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: " + getNsdComponentClassName(), " \"", name, "\" declared as \"OmultiRange\" in PresenceCondition" );
            if( optionalMultiRange == null ) optionalMultiRange = new HashMap<>();
            String[] limits2 = presCondArgs.split( "[ ,]+" );
            if( limits2.length != 2 ) {
                console.warning( getSetupMessageCategory(), filename, lineNumber,
                        "argument of PresenceCondition \"OmultiRange\" is not two integers" );
                break;
            }
            Integer min2 = Integer.valueOf( limits2[0] );
            if( min2 <= 0 ) {
                console.warning( getSetupMessageCategory(), filename, lineNumber,
                        "first argument of PresenceCondition \"OmultiRange\" is not a positive integer" );
                break;
            }
            Integer max2 = Integer.valueOf( limits2[1] );
            if( max2 <= 0 ) {
                console.warning( getSetupMessageCategory(), filename, lineNumber,
                        "second argument of PresenceCondition \"OmultiRange\" is not a positive integer" );
                break;
            }
            optionalMultiRange.put( name, Pair.of( min2, max2 ));
            break;
        case "MFsubst" :
            // Element is mandatory if substitution is supported (for substitution, see IEC 61850-7-3), otherwise forbidden
            // TODO: how do we know if substitution is supported ?
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: " + getNsdComponentClassName(), " \"", name, "\" declared as \"MFsubst\" in PresenceCondition" );
//            if( mandatoryIfSubstitutionElseForbidden == null ) mandatoryIfSubstitutionElseForbidden = new HashSet<>();
//            mandatoryIfSubstitutionElseForbidden.add( name );
            break;
        case "MOln0" :
            // Element is mandatory in the context of LLN0; otherwise optional
            if( mandatoryInLLN0ElseOptional == null ) mandatoryInLLN0ElseOptional = new HashSet<>();
            mandatoryInLLN0ElseOptional.add( name );
            break;
        case "MFln0" :
            // Element is mandatory in the context of LLN0; otherwise forbidden
            if( mandatoryInLLN0ElseForbidden == null ) mandatoryInLLN0ElseForbidden = new HashSet<>();
            mandatoryInLLN0ElseForbidden.add( name );
            break;
        case "MOlnNs" :
            // Element is mandatory if the name space of its logical node deviates from the name space of the containing
            // logical device, otherwise optional. See IEC 61850-7-1 for use of name space
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: " + getNsdComponentClassName(), " \"", name, "\" declared as \"MOlnNs\" in PresenceCondition" );
//            if( mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional == null ) mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional = new HashSet<>();
//            mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional.add( name );
            break;
        case "MOcdcNs" :
            // Seen in IEC_61850-7-2_2007A2 and IEC_61850-7-2_2007A3, its significance is based on MOlnNs and MOdataNs
            // Element is mandatory if the name space of its data class deviates from the name space of its logical node,
            // otherwise optional. See IEC 61850-7-1 for use of name space
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: " + getNsdComponentClassName(), " \"", name, "\" declared as \"MOcdcNs\" in PresenceCondition" );
//            if( mandatoryIfNameSpaceOfDataClassDeviatesElseOptional == null ) mandatoryIfNameSpaceOfDataClassDeviatesElseOptional = new HashSet<>();
//            mandatoryIfNameSpaceOfDataClassDeviatesElseOptional.add( name );
            break;
        case "MOdataNs" :
            // Element is mandatory if the name space of its data object deviates from the name space of its logical node,
            // otherwise optional. See IEC 61850-7-1 for use of name space
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: " + getNsdComponentClassName(), " \"", name, "\" declared as \"MOdataNs\" in PresenceCondition" );
//            if( mandatoryIfNameSpaceOfDataObjectDeviatesElseOptional == null ) mandatoryIfNameSpaceOfDataObjectDeviatesElseOptional = new HashSet<>();
//            mandatoryIfNameSpaceOfDataObjectDeviatesElseOptional.add( name );
            break;
        case "MFscaledAV" :
            // Element is mandatory* if any sibling elements of type AnalogueValue include 'i' as a child, otherwise forbidden.
            // *Even though devices without floating point capability cannot exchange floating point values through ACSI services,
            // the description of scaling remains mandatory for their (SCL) configuration
            if( mandatoryIfAnalogValueIncludesIElseForbidden == null ) mandatoryIfAnalogValueIncludesIElseForbidden = new HashSet<>();
            mandatoryIfAnalogValueIncludesIElseForbidden.add( name );
            break;
        case "MFscaledMagV" :
            // Element is mandatory* if any sibling elements of type Vector include 'i' as a child of their 'mag' attribute, otherwise forbidden.
            // *See MFscaledAV
            if( mandatoryIfVectorSiblingIncludesIAsChildMagElseForbidden == null ) mandatoryIfVectorSiblingIncludesIAsChildMagElseForbidden = new HashSet<>();
            mandatoryIfVectorSiblingIncludesIAsChildMagElseForbidden.add( name );
            break;
        case "MFscaledAngV" :
            // Element is mandatory* if any sibling elements of type Vector include 'i' as a child of their 'ang' attribute, otherwise forbidden.
            // *See MFscaledAV
            if( mandatoryIfVectorSiblingIncludesIAsChildAngElseForbidden == null ) mandatoryIfVectorSiblingIncludesIAsChildAngElseForbidden = new HashSet<>();
            mandatoryIfVectorSiblingIncludesIAsChildAngElseForbidden.add( name );
            break;
        case "MOrms" :
            // Element is mandatory if the harmonic values in the context are calculated as a ratio to RMS value
            // (value of data attribute 'hvRef' is 'rms'), optional otherwise
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: " + getNsdComponentClassName(), " \"", name, "\" declared as \"MOrms\" in PresenceCondition" );
//            if( mandatoryIfHarmonicValuesCalculatedAsRatioElseOptional == null ) mandatoryIfHarmonicValuesCalculatedAsRatioElseOptional = new HashSet<>();
//            mandatoryIfHarmonicValuesCalculatedAsRatioElseOptional.add( name );
            break;
        case "MOrootLD" :
            // Element is mandatory in the context of a root logical device; otherwise it is optional
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: " + getNsdComponentClassName(), " \"", name, "\" declared as \"MOrootLD\" in PresenceCondition" );
//            if( mandatoryInRootLogicalDeviceElseOptional == null ) mandatoryInRootLogicalDeviceElseOptional = new HashSet<>();
//            mandatoryInRootLogicalDeviceElseOptional.add( name );
            break;
        case "MOoperTm" :
            // Element is mandatory if at least one controlled object on the IED supports time activation service; otherwise it is optional
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: " + getNsdComponentClassName(), " \"", name, "\" declared as \"MOoperTm\" in PresenceCondition" );
//            if( mandatoryIfControlSupportsTimeElseOptional == null ) mandatoryIfControlSupportsTimeElseOptional = new HashSet<>();
//            mandatoryIfControlSupportsTimeElseOptional.add( name );
            break;
        case "MmultiF" :
            // Parameter sibling: sibling element name.
            // One or more elements must be present if sibling element is present, otherwise forbidden
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: " + getNsdComponentClassName(), " \"", name, "\" declared as \"MOoperTm\" in PresenceCondition" );
//            if( oneOrMoreIfSiblingPresentElseForbidden == null ) oneOrMoreIfSiblingPresentElseForbidden = new HashMap<>();
//            oneOrMoreIfSiblingPresentElseForbidden.put( name, presCondArgs );
            break;
        case "MOsbo" :
            // Element is mandatory if declared control model supports 'sbo-with-normal-security' or 'sbo-with-enhanced-security',
            // otherwise optional and value is of no impact
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: " + getNsdComponentClassName(), " \"", name, "\" declared as \"MOsbo\" in PresenceCondition" );
//            if( mandatoryIfControlSupportsSecurity1ElseOptional == null ) mandatoryIfControlSupportsSecurity1ElseOptional = new HashSet<>();
//            mandatoryIfControlSupportsSecurity1ElseOptional.add( name );
            break;
        case "MOenhanced" :
            // Element is mandatory if declared control model supports 'direct-with-enhanced-security' or 'sbo-with-enhanced-security',
            // otherwise optional and value is of no impact
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: " + getNsdComponentClassName(), " \"", name, "\" declared as \"MOenhanced\" in PresenceCondition" );
//            if( mandatoryIfControlSupportsSecurity2ElseOptional == null ) mandatoryIfControlSupportsSecurity2ElseOptional = new HashSet<>();
//            mandatoryIfControlSupportsSecurity2ElseOptional.add( name );
            break;
        case "MONamPlt" :
            // Element is mandatory if the name space of its logical node deviates from the name space of the containing
            // logical device, otherwise optional. See IEC 61850-7-1 for use of name space
            // TODO: same as "MOlnNs" ?
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: " + getNsdComponentClassName(), " \"", name, "\" declared as \"MONamPlt\" in PresenceCondition" );
//            if( mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional2 == null ) mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional2 = new HashSet<>();
//            mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional2.add( name );
            break;
        case "OF" :
            // Parameter sibling: sibling element name.
            // Optional if sibling element is present, otherwise forbidden
            if( optionalIfSiblingPresentElseForbidden == null ) optionalIfSiblingPresentElseForbidden = new HashMap<>();
            optionalIfSiblingPresentElseForbidden.put( name, presCondArgs );
            break;
        case "MORange" :
            // Element is mandatory if the measured value associated (amplitude respectively angle) exposes the range eventing
            // (with the attribute range respectively rangeAng)
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: " + getNsdComponentClassName(), " \"", name, "\" declared as \"MORange\" in PresenceCondition" );
//            if( mandatoryIfMeasuredValueExposesRange == null ) mandatoryIfMeasuredValueExposesRange = new HashSet<>();
//            mandatoryIfMeasuredValueExposesRange.add( name );
            break;
        case "OMSynPh" :
            // This attribute is optional if value of 'phsRef'' is Synchrophasor otherwise Mandatory]]></Doc>
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: " + getNsdComponentClassName(), " \"", name, "\" declared as \"OMSynPh\" in PresenceCondition" );
//            if( optionalIfPhsRefIsSynchrophasorElseMandatory == null ) optionalIfPhsRefIsSynchrophasorElseMandatory = new HashSet<>();
//            optionalIfPhsRefIsSynchrophasorElseMandatory.add( name );
            break;
        case "MAllOrNonePerGroup" :
            // Parameter n: group number (> 0).
            // Element is mandatory if declared control model supports 'direct-with-enhanced- security'
            // or 'sbo-with-enhanced-security', otherwise all or none of the elements of a group n shall be present.
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: " + getNsdComponentClassName(), " \"", name, "\" declared as \"MAllOrNonePerGroup\" in PresenceCondition" );
//            if( mAllOrNonePerGroup == null ) mAllOrNonePerGroup = new HashSet<>();
//            mAllOrNonePerGroup.add( name );
            break;
        case "MOctrl" :
            // Seen in IEC_61850-8-1_2003A2.snsd
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: " + getNsdComponentClassName(), " \"", name, "\" declared as \"MOctrl\" in PresenceCondition" );
//            if( mOctrl == null ) mOctrl = new HashSet<>();
//            mOctrl.add( name );
            break;
        case "MOsboNormal" :
            // Seen in IEC_61850-8-1_2003A2.snsd
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: " + getNsdComponentClassName(), " \"", name, "\" declared as \"MOsboNormal\" in PresenceCondition" );
//            if( mOsboNormal == null ) mOsboNormal = new HashSet<>();
//            mOsboNormal.add( name );
            break;
        case "MOsboEnhanced" :
            // Seen in IEC_61850-8-1_2003A2.snsd
            console.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: " + getNsdComponentClassName(), " \"", name, "\" declared as \"MOsboEnhanced\" in PresenceCondition" );
//            if( mOsboEnhanced == null ) mOsboEnhanced = new HashSet<>();
//            mOsboEnhanced.add( name );
            break;
       default:
           console.warning( getSetupMessageCategory(), filename, lineNumber,
                            "the PresenceCondition ", presCond, " of ", getNsdComponentClassName(), " \"", name, "\" is unknown" );
            break;
        }
        
    }
    
    protected void checkSpecification() {
        // TODO: do we have to check the presence of the sibling in inherited AbstractLNClass ?
        if( mandatoryIfSiblingPresentElseForbidden != null ) {
            for( Entry< String, String > e : mandatoryIfSiblingPresentElseForbidden.entrySet() ) {
                if( ! presentSclComponent.containsKey( e.getValue() )) {
                    console.warning( getSetupMessageCategory(), 0,
                                     "the sibling of ", e.getKey(), " in PresenceCondition of ", getNsdComponentClassName(), " ", e.getKey(), " is unknown in namespace \"", nsIdentification, "\"" );
                }
            }
        }
        if( mandatoryIfSiblingPresentElseOptional != null ) {
            for( Entry< String, String > e : mandatoryIfSiblingPresentElseOptional.entrySet() ) {
                if( ! presentSclComponent.containsKey( e.getValue() )) {
                    console.warning( getSetupMessageCategory(), 0,
                                     "the sibling of ", e.getKey(), " in PresenceCondition of ", getNsdComponentClassName(), " ", e.getKey(), " is unknown in namespace \"", nsIdentification, "\"" );
                }
            }
        }
        if( optionalIfSiblingPresentElseMandatory != null ) {
            for( Entry< String, String > e : optionalIfSiblingPresentElseMandatory.entrySet() ) {
                if( ! presentSclComponent.containsKey( e.getValue() )) {
                    console.warning( getSetupMessageCategory(), 0,
                                     "the sibling of ", e.getKey(), " in PresenceCondition of ", getNsdComponentClassName(), " ", e.getKey(), " is unknown in namespace \"", nsIdentification, "\"" );
                }
            }
        }
        if( forbiddenIfSiblingPresentElseMandatory != null ) {
            for( Entry< String, String > e : forbiddenIfSiblingPresentElseMandatory.entrySet() ) {
                if( ! presentSclComponent.containsKey( e.getValue() )) {
                    console.warning( getSetupMessageCategory(), 0,
                                     "the sibling of ", e.getKey(), " in PresenceCondition of ", getNsdComponentClassName(), " ", e.getKey(), " is unknown in namespace \"", nsIdentification, "\"" );
                }
            }
        }
        if( oneOrMoreIfSiblingPresentElseForbidden != null ) {
            for( Entry< String, String > e : oneOrMoreIfSiblingPresentElseForbidden.entrySet() ) {
                if( ! presentSclComponent.containsKey( e.getValue() )) {
                    console.warning( getSetupMessageCategory(), 0,
                                     "the sibling of ", e.getKey(), " in PresenceCondition of ", getNsdComponentClassName(), " ", e.getKey(), " is unknown in namespace \"", nsIdentification, "\"" );
                }
            }
        }
        if( optionalIfSiblingPresentElseForbidden != null ) {
            for( Entry< String, String > e : optionalIfSiblingPresentElseForbidden.entrySet() ) {
                if( ! presentSclComponent.containsKey( e.getValue() )) {
                    console.warning( getSetupMessageCategory(), 0,
                                     "the sibling of ", e.getKey(), " in PresenceCondition of ", getNsdComponentClassName(), " ", e.getKey(), " is unknown in namespace \"", nsIdentification, "\"" );
                }
            }
        }
    }

    public void resetModelData() {
        for( String sclComponent : presentSclComponent.keySet() ) {
            presentSclComponent.put( sclComponent, null );
        }
    }
    
    public boolean addModelData( @NonNull SclComponent sclComponent, String sclComponentName, DiagnosticChain diagnostics ) {
        if( ! presentSclComponent.containsKey( sclComponentName )) {
            RiseClipseMessage error = RiseClipseMessage.error( getValidationMessageCategory(), sclComponent.getFilename(), sclComponent.getLineNumber(), 
                                      getSclComponentClassName(), " \"", sclComponentName, "\" not expected in ", getNsdModelClassName(), " \"", getNsdModelName(),
                                      "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    error.getMessage(),
                    new Object[] { sclComponent, error } ));
            return false;
        }

        if( presentSclComponent.get( sclComponentName ) != null ) {
            RiseClipseMessage error = RiseClipseMessage.error( getValidationMessageCategory(), sclComponent.getFilename(), sclComponent.getLineNumber(), 
                                      getSclComponentClassName(), " \"", sclComponentName, "\" already present in ", getNsdModelClassName(), " \"", getNsdModelName(),
                                      "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    error.getMessage(),
                    new Object[] { sclComponent, error } ));
            return false;
        }
        presentSclComponent.put( sclComponentName, sclComponent );
        return true;
    }
    
    public boolean validate( @NonNull SclModel sclModel, DiagnosticChain diagnostics ) {
        console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                getPresenceConditionValidatorName(), ".validate( ", getSclModelClassName(), " id = \"", sclModel.getId(), "\" ) in namespace \"", nsIdentification, "\"" );

        boolean res = true;
        
        if( nsdModel.isDeprecated() ) {
            RiseClipseMessage warning = RiseClipseMessage.warning( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(), 
                    getSclComponentClassName(), "\"", sclModel.getId(), " refers to deprecated ", getNsdModelClassName(), " \"", getNsdModelName(), "\" in namespace \"", nsIdentification, "\"" );
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.WARNING,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    warning.getMessage(),
                    new Object[] { sclModel, warning } ));
        }
        
        // presCond: "M"
        // Element is mandatory
        // Usage in standard NSD files (version 2007B): DataObject and DataAttribute and SubDataAttribute
        if( mandatory != null ) {
            for( String name : this.mandatory ) {
                if( deprecated.contains( name )) {
                    // No errors for deprecated elements
                    continue;
                }
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"M\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) == null ) {
                    RiseClipseMessage error = RiseClipseMessage.error( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(), 
                                              getSclComponentClassName(), " \"", name, "\" is mandatory in ", getSclModelClassName(), " id = \"",
                                              sclModel.getId(), "\" with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ",
                                              getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                          Diagnostic.ERROR,
                          RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                          0,
                          error.getMessage(),
                          new Object[] { sclModel, error } ));
                  res = false;
                }
            }
        }

        // presCond: "O"
        // Element is optional
        // Usage in standard NSD files (version 2007B): DataObject and DataAttribute and SubDataAttribute
        if( optional != null ) {
            for( String name : this.optional ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"O\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) == null ) {
                    // Nothing
                }
            }
        }

        // presCond: "F"
        // Element is forbidden
        // Usage in standard NSD files (version 2007B): DataObject
        if( forbidden != null ) {
            for( String name : this.forbidden ) {
                if( deprecated.contains( name )) {
                    // No errors for deprecated elements
                    continue;
                }
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"F\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage error = RiseClipseMessage.error( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(), 
                            getSclComponentClassName(), " \"", name, "\" is forbidden in ", getSclModelClassName(), " id = \"",
                            sclModel.getId(), "\" with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ",
                            getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                          Diagnostic.ERROR,
                          RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                          0,
                          error.getMessage(),
                          new Object[] { sclModel, error } ));
                  res = false;
                }
            }
        }

        // presCond: "na"
        // Element is not applicable
        // Usage in standard NSD files (version 2007B): only for dsPresCond
        // -> TODO: what does it mean ? what do we have to check ?
        if( notApplicable != null ) {
            for( String name : notApplicable ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"na\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                                "verification of PresenceCondition \"na\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ",
                                                getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(),
                                                " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.INFO,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            notice.getMessage(),
                            new Object[] { sclModel, notice } ));
                }
            }
        }
        
        // presCond: "Mmulti"
        // At least one element shall be present; all instances have an instance number > 0
        // Usage in standard NSD files (version 2007B): DataObject
        if( mandatoryMulti != null ) {
            for( String name : mandatoryMulti ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"Mmulti\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                                "verification of PresenceCondition \"Mmulti\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ",
                                                getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(),
                                                " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.INFO,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            notice.getMessage(),
                            new Object[] { sclModel, notice } ));
                }
            }
        }

        // presCond: "Omulti"
        // Zero or more elements may be present; all instances have an instance number > 0
        // Usage in standard NSD files (version 2007B): DataObject
        if( optionalMulti != null ) {
            for( String name : optionalMulti ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"Omulti\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                                "verification of PresenceCondition \"Mmulti\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ",
                                                getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(),
                                                " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.INFO,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            notice.getMessage(),
                            new Object[] { sclModel, notice } ));
                }
            }
        }
        
        // presCond: "AtLeastOne"
        // Parameter n: group number (> 0).
        // At least one of marked elements of a group n shall be present
        // Usage in standard NSD files (version 2007B): DataObject and SubDataObject and DataAttribute and SubDataAttribute
        if( atLeastOne != null ) {
            for( Entry< Integer, HashSet< String > > e1 : atLeastOne.entrySet() ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"AtLeastOne\" for ", getSclComponentClassName(), " group ", e1.getKey(), " in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                boolean groupOK = false;
                String atLeastOneOf = " (at least one of:";
                for( String member : e1.getValue() ) {
                    atLeastOneOf += " " + member;
                    if( presentSclComponent.get( member ) != null ) {
                        groupOK = true;
                        break;
                    }
                }
                atLeastOneOf += ")";
                if( ! groupOK ) {
                    RiseClipseMessage error = RiseClipseMessage.error( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(), 
                                              "group ", e1.getKey(), " has no elements in ", getSclModelClassName(), " with ", getNsdModelClassName(),
                                              " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(),
                                              atLeastOneOf, " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            error.getMessage(),
                            new Object[] { sclModel, error } ));
                    res = false;
                }
            }
        }
        
        // presCond: "AtMostOne" :
        // At most one of marked elements shall be present
        // Usage in standard NSD files (version 2007B): DataObject
        if( atMostOne != null ) {
            console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                    "validation of presence condition \"AtMostOne\" for ", getSclComponentClassName(), " in ", getSclModelClassName(), " id = \"",
                    sclModel.getId(), "\" with ", getNsdModelClassName(),
                    " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
            int count = 0;
            String atMostOneOf = " (at most one of:";
            for( String s : atMostOne ) {
                atMostOneOf += " " + s;
                if( presentSclComponent.get( s ) != null ) {
                    ++count;
                }
            }
            atMostOneOf += ")";
            if( count > 1 ) {
                RiseClipseMessage error = RiseClipseMessage.error( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(), 
                                          getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(),
                                          "\" has more than one element marked AtMostOne", atMostOneOf, " in namespace \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        error.getMessage(),
                        new Object[] { sclModel, error } ));
                res = false;
            }
        }
        
        // presCond: "AllOrNonePerGroup" :
        // Parameter n: group number (> 0).
        // All or none of the elements of a group n shall be present
        // Usage in standard NSD files (version 2007B): DataAttribute
        if( allOrNonePerGroup != null ) {
            for( Entry< Integer, HashSet< String > > e1 : allOrNonePerGroup.entrySet() ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"AllOrNonePerGroup\" for ", getSclComponentClassName(), " group ", e1.getKey(), " in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                int groupCount = 0;
                String expectedMembers = " (expected members:";
                for( String member : e1.getValue() ) {
                    expectedMembers += " " + member;
                    if( presentSclComponent.get( member ) != null ) {
                        ++groupCount;
                    }
                }
                expectedMembers += ")";
                if(( groupCount > 0 ) && ( groupCount < e1.getValue().size() )) {
                    RiseClipseMessage error = RiseClipseMessage.error( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(), 
                                              "group ", e1.getKey(), " has neither none nor all elements in ", getSclModelClassName(),
                                              " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(),
                                              expectedMembers, " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            error.getMessage(),
                            new Object[] { sclModel, error } ));
                    res = false;
                }
            }
        }
        
        // presCond: "AllOnlyOneGroup" :
        // Parameter n: group number (> 0).
        // All elements of only one group n shall be present
        // Usage in standard NSD files (version 2007B): DataObject and SubDataAttribute
        if( allOnlyOneGroup != null ) {
            int groupNumber = 0;
            for( Entry< Integer, HashSet< String > > e1 : allOnlyOneGroup.entrySet() ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"AllOnlyOneGroup\" for ", getSclComponentClassName(), " group ", e1.getKey(), " in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                int groupCount = 0;
                String expectedMembers = " (expected members:";
                for( String member : e1.getValue() ) {
                    expectedMembers += " " + member;
                    if( presentSclComponent.get( member ) != null ) {
                        ++groupCount;
                    }
                }
                expectedMembers += ")";
                if(( groupCount > 0 ) && ( groupCount < e1.getValue().size() )) {
                    RiseClipseMessage error = RiseClipseMessage.error( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(), 
                                              "group ", e1.getKey(), " has neither none nor all elements in ", getSclModelClassName(),
                                              " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(),
                                              expectedMembers, " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            error.getMessage(),
                            new Object[] { sclModel, error } ));
                    res = false;
                }
                else if( groupCount > 0 ) {
                    if( groupNumber == 0 ) {
                        groupNumber = e1.getKey();
                    }
                    else {
                        RiseClipseMessage error = RiseClipseMessage.error( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(), 
                                                  getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(),
                                                  "\" has several groups with all elements", " in namespace \"", nsIdentification, "\"" );
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                error.getMessage(),
                                new Object[] { sclModel, error } ));
                        res = false;
                    }
                }
            }
            if( groupNumber == 0 ) {
                RiseClipseMessage error = RiseClipseMessage.error( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(), 
                                          "no group in ", getSclModelClassName(), " with ", getNsdModelClassName(), " \"",
                                          getNsdModelName(), "\" has all elements", " in namespace \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        error.getMessage(),
                        new Object[] { sclModel, error } ));
                res = false;
            }
        }
        
        // presCond: "AllAtLeastOneGroup" :
        // Parameter n: group number (> 0).
        // All elements of at least one group n shall be present
        // Usage in standard NSD files (version 2007B): DataAttribute
        if( allAtLeastOneGroup != null ) {
            int groupNumber = 0;
            for( Entry< Integer, HashSet< String > > e1 : allAtLeastOneGroup.entrySet() ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"AllAtLeastOneGroup\" for ", getSclComponentClassName(), " group ", e1.getKey(), " in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                int groupCount = 0;
                for( String member : e1.getValue() ) {
                    if( presentSclComponent.get( member ) != null ) {
                        ++groupCount;
                    }
                }
                if( groupCount == e1.getValue().size() ) {
                    groupNumber = e1.getKey();
                }
            }
            if( groupNumber == 0 ) {
                RiseClipseMessage error = RiseClipseMessage.error( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(), 
                                          "no group in ", getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(),
                                          "\" has all elements", " in namespace \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        error.getMessage(),
                        new Object[] { sclModel, error } ));
                res = false;
            }
        }
        
        // presCond: "MF" :
        // Parameter sibling: sibling element name.
        // Mandatory if sibling element is present, otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataObject
        if( mandatoryIfSiblingPresentElseForbidden != null ) {
            for( Entry< String, String > entry : mandatoryIfSiblingPresentElseForbidden.entrySet() ) {
                if( deprecated.contains( entry.getKey() )) {
                    // No errors for deprecated elements
                    continue;
                }
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"MF\" for ", getSclComponentClassName(), " element \"", entry.getKey(), "\" sibling \"", entry.getValue(),
                        "\" in ", getSclModelClassName(), " id = \"", sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( entry.getValue() ) != null ) {
                    if( presentSclComponent.get( entry.getKey() ) == null ) {
                        RiseClipseMessage error = RiseClipseMessage.error( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(), 
                                                  getSclComponentClassName(), " ", entry.getKey() + " is mandatory in ", getSclModelClassName(), " with ",
                                                  getNsdModelClassName(), " \"", getNsdModelName(), "\" because sibling ", entry.getValue(), " is present"
                                                  , " in namespace \"", nsIdentification, "\"" );
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                error.getMessage(),
                                new Object[] { sclModel, error } ));
                        res = false;
                    }
                }
                else {
                    if( presentSclComponent.get( entry.getKey() ) != null ) {
                        RiseClipseMessage error = RiseClipseMessage.error( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(), 
                                                  getSclComponentClassName(), " ", entry.getKey() + " is forbidden in ", getSclModelClassName(), " with ",
                                                  getNsdModelClassName(), " \"", getNsdModelName(), "\" because sibling ", entry.getValue(), " is not present",
                                                  " in namespace \"", nsIdentification, "\"" );
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                error.getMessage(),
                                new Object[] { sclModel, error } ));
                        res = false;
                    }
                }
            }
        }
        
        // presCond: "MO" :
        // Parameter sibling: sibling element name.
        // Mandatory if sibling element is present, otherwise optional
        // Usage in standard NSD files (version 2007B): DataAttribute
        if( mandatoryIfSiblingPresentElseOptional != null ) {
            for( Entry< String, String > entry : mandatoryIfSiblingPresentElseOptional.entrySet() ) {
                if( deprecated.contains( entry.getKey() )) {
                    // No errors for deprecated elements
                    continue;
                }
                 console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"MO\" for ", getSclComponentClassName(), " element \"", entry.getKey(), "\" sibling \"", entry.getValue(),
                        "\" in ", getSclModelClassName(), " id = \"", sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( entry.getValue() ) != null ) {
                    if( presentSclComponent.get( entry.getKey() ) == null ) {
                        RiseClipseMessage error = RiseClipseMessage.error( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(), 
                                                  getSclComponentClassName(), " ", entry.getKey() + " is mandatory in ", getSclModelClassName(), " with ",
                                                  getNsdModelClassName(), " \"", getNsdModelName(), "\" because sibling ", entry.getValue(), " is present"
                                                  , " in namespace \"", nsIdentification, "\"" );
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                error.getMessage(),
                                new Object[] { sclModel, error } ));
                        res = false;
                    }
                }
            }
        }
        
        // presCond: "OM" :
        // Parameter sibling: sibling element name.
        // Optional if sibling element is present, otherwise mandatory
        // Usage in standard NSD files (version 2007B): None
        if( optionalIfSiblingPresentElseMandatory != null ) {
            for( Entry< String, String > entry : optionalIfSiblingPresentElseMandatory.entrySet() ) {
                if( deprecated.contains( entry.getKey() )) {
                    // No errors for deprecated elements
                    continue;
                }
                 console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"OM\" for ", getSclComponentClassName(), " element \"", entry.getKey(), "\" sibling \"", entry.getValue(),
                        "\" in ", getSclModelClassName(), " id = \"", sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( entry.getValue() ) == null ) {
                    if( presentSclComponent.get( entry.getKey() ) == null ) {
                        RiseClipseMessage error = RiseClipseMessage.error( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(), 
                                                  getSclComponentClassName(), " ", entry.getKey() + " is mandatory in ", getSclModelClassName(), " with ",
                                                  getNsdModelClassName(), " \"", getNsdModelName(), "\" because sibling ", entry.getValue(), " is not present",
                                                  " in namespace \"", nsIdentification, "\"" );
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                error.getMessage(),
                                new Object[] { sclModel, error } ));
                        res = false;
                    }
                }
            }
        }
        
        // presCond: "FM" :
        // Parameter sibling: sibling element name.
        // Forbidden if sibling element is present, otherwise mandatory
        // Usage in standard NSD files (version 2007B): None
        if( forbiddenIfSiblingPresentElseMandatory != null ) {
            for( Entry< String, String > entry : forbiddenIfSiblingPresentElseMandatory.entrySet() ) {
                if( deprecated.contains( entry.getKey() )) {
                    // No errors for deprecated elements
                    continue;
                }
                 console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"FM\" for ", getSclComponentClassName(), " element \"", entry.getKey(), "\" sibling \"", entry.getValue(),
                        "\" in ", getSclModelClassName(), " id = \"", sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( entry.getValue() ) != null ) {
                    if( presentSclComponent.get( entry.getKey() ) != null ) {
                        RiseClipseMessage error = RiseClipseMessage.error( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(), 
                                                  getSclComponentClassName(), " ", entry.getKey() + " is forbidden in ", getSclModelClassName(), " with ",
                                                  getNsdModelClassName(), " \"", getNsdModelName(), "\" because sibling ", entry.getValue(), " is present"
                                                  , " in namespace \"", nsIdentification, "\"" );
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                error.getMessage(),
                                new Object[] { sclModel, error } ));
                        res = false;
                    }
                }
                else {
                    if( presentSclComponent.get( entry.getKey() ) == null ) {
                        RiseClipseMessage error = RiseClipseMessage.error( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(), 
                                                  getSclComponentClassName(), " ", entry.getKey() + " is mandatory in ", getSclModelClassName(), " with ",
                                                  getNsdModelClassName(), " \"", getNsdModelName(), "\" because sibling ", entry.getValue(), " is not present",
                                                  " in namespace \"", nsIdentification, "\"" );
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                error.getMessage(),
                                new Object[] { sclModel, error } ));
                        res = false;
                    }
                }
            }
        }
        
        // presCond: "MOcond" :
        // Parameter condID: condition number (> 0).
        // Textual presence condition (non-machine processable) with reference condID to context specific text.
        // If satisfied, the element is mandatory, otherwise optional
        // Usage in standard NSD files (version 2007B): DataObject
        if( mandatoryIfTextConditionElseOptional != null ) {
            for( Entry< String, String > entry : mandatoryIfTextConditionElseOptional.entrySet() ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"MOcond\" for ", getSclComponentClassName(), " ", entry.getKey(), " textual condition number ", entry.getValue(),
                        " in ", getSclModelClassName(), " id = \"", sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                String doc = mandatoryIfTextConditionElseOptionalDoc.get( entry.getKey() );

                RiseClipseMessage warning = RiseClipseMessage.error( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(), 
                                          getSclComponentClassName(), " ", entry.getKey(), " is mandatory in ", getSclModelClassName(), " with ",
                                          getNsdModelClassName(), " \"", getNsdModelName(), "\" if textual condition number ", entry.getValue(), " (not evaluated) is true, else optional. It is ",
                                          ( presentSclComponent.get( entry.getKey() ) == null ? "absent." : "present." ), ( doc != null ? " Textual condition is: \"" + doc + "\"." : "" ),
                                          " in namespace \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        warning.getMessage(),
                        new Object[] { sclModel, warning } ));
            }
        }
        
        // presCond: "MFcond" :
        // Parameter condID: condition number (> 0).
        // Textual presence condition (non-machine processable) with reference condID to context specific text.
        // If satisfied, the element is mandatory, otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataObject
        if( mandatoryIfTextConditionElseForbidden != null ) {
            for( Entry< String, String > entry : mandatoryIfTextConditionElseForbidden.entrySet() ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"MFcond\" for ", getSclComponentClassName(), entry.getKey(), " textual condition number ", entry.getValue(),
                        " in ", getSclModelClassName(), " id = \"", sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                String doc = mandatoryIfTextConditionElseForbiddenDoc.get( entry.getKey() );

                RiseClipseMessage error = RiseClipseMessage.error( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(), 
                                          getSclComponentClassName(), " ", entry.getKey(), " is mandatory in ", getSclModelClassName(), " with ",
                                          getNsdModelClassName(), " \"", getNsdModelName(), "\" if textual condition number ", entry.getValue(), " (not evaluated) is true, else forbidden. It is ",
                                          ( presentSclComponent.get( entry.getKey() ) == null ? "absent." : "present." ), ( doc != null ? " Textual condition is: \"" + doc + "\"." : "" ),
                                          " in namespace \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        error.getMessage(),
                        new Object[] { sclModel, error } ));
            }
        }
        
        // presCond: "OFcond" :
        // Parameter condID: condition number (> 0).
        // Textual presence condition (non-machine processable) with reference condID to context specific text.
        // If satisfied, the element is optional, otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataObject
        if( optionalIfTextConditionElseForbidden != null ) {
            for( Entry< String, String > entry : optionalIfTextConditionElseForbidden.entrySet() ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"OFcond\" for ", getSclComponentClassName(), entry.getKey(), " textual condition number ", entry.getValue(),
                        " in ", getSclModelClassName(), " id = \"", sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                String doc = optionalIfTextConditionElseForbiddenDoc.get( entry.getKey() );

                RiseClipseMessage error = RiseClipseMessage.error( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(), 
                                          getSclComponentClassName(), " ", entry.getKey(), " is optional in ", getSclModelClassName(), " with ",
                                          getNsdModelClassName(), " \"", getNsdModelName(), "\" if textual condition number ", entry.getValue(), " (not evaluated) is true, else forbidden. It is ",
                                          ( presentSclComponent.get( entry.getKey() ) == null ? "absent." : "present." ), ( doc != null ? " Textual condition is: \"" + doc + "\"." : "" )
                                          , " in namespace \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        error.getMessage(),
                        new Object[] { sclModel, error } ));
            }
        }
        
        // presCond: "MmultiRange" :
        // Parameters min, max: limits for instance number (> 0).
        // One or more elements shall be present; all instances have an instance number within range [min, max] (see IEC 61850-7-1)
        // Usage in standard NSD files (version 2007B): None
        if( mandatoryMultiRange != null ) {
            for( String name : mandatoryMultiRange.keySet() ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"MmultiRange\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                                "verification of PresenceCondition \"MmultiRange\" for ", getSclComponentClassName(), " ", name,
                                                " is not implemented in ", getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(),
                                                "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.INFO,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            notice.getMessage(),
                            new Object[] { sclModel, notice } ));
                }
            }
        }

        // presCond: "OmultiRange" :
        // Parameters min, max: limits for instance number (> 0).
        // Zero or more elements may be present; all instances have an instance number within range [min, max] (see IEC 61850-7-1)
        // Usage in standard NSD files (version 2007B): DataObject
        if( optionalMultiRange != null ) {
            for( String name : optionalMultiRange.keySet() ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"OmultiRange\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                                "verification of PresenceCondition \"OmultiRange\" for ", getSclComponentClassName(), " ", name,
                                                " is not implemented in ", getSclModelClassName(), " with ", getNsdModelClassName(), " \"",
                                                getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.INFO,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            notice.getMessage(),
                            new Object[] { sclModel, notice } ));
                }
            }
        }

        // presCond: "MFsubst" :
        // Element is mandatory if substitution is supported (for substitution, see IEC 61850-7-3), otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO
        if( mandatoryIfSubstitutionElseForbidden != null ) {
            for( String name : mandatoryIfSubstitutionElseForbidden ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"MFsubst\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                                "verification of PresenceCondition \"MFsubst\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ",
                                                getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(),
                                                " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.INFO,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            notice.getMessage(),
                            new Object[] { sclModel, notice } ));
                }
            }
        }
        
        // presCond: "MOln0" :
        // Element is mandatory in the context of LLN0; otherwise optional
        // Usage in standard NSD files (version 2007B): DataAttribute
        if( mandatoryInLLN0ElseOptional != null ) {
            console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                    "validation of presence condition \"MOln0\" for ", getSclComponentClassName(), " in ", getSclModelClassName(), " id = \"",
                    sclModel.getId(), "\" with ", getNsdModelClassName(),
                    " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
            res = validateMOln0( sclModel, diagnostics ) && res;
        }
        
        // presCond: "MFln0" :
        // Element is mandatory in the context of LLN0; otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataAttribute
        if( mandatoryInLLN0ElseForbidden != null ) {
            console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                    "validation of presence condition \"MFln0\" for ", getSclComponentClassName(), " in ", getSclModelClassName(), " id = \"",
                    sclModel.getId(), "\" with ", getNsdModelClassName(),
                    " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
            res = validateMFln0( sclModel, diagnostics ) && res;
        }

        // presCond: "MOlnNs" :
        // Element is mandatory if the name space of its logical node deviates from the name space of the containing
        // logical device, otherwise optional. See IEC 61850-7-1 for use of name space
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO: The meaning is not clear.
        if( mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional != null ) {
            for( String name : mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"MOlnNs\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                                "verification of PresenceCondition \"MOlnNs\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ",
                                                getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(),
                                                " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.INFO,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            notice.getMessage(),
                            new Object[] { sclModel, notice } ));
                }
            }
        }

        // presCond: "MOcdcNs" :
        // Element is mandatory if the name space of its data class deviates from the name space of its logical node,
        // otherwise optional. See IEC 61850-7-1 for use of name space
        // Usage in standard NSD files (version 2007A2/A3): DataAttribute
        // TODO: The meaning is not clear.
        if( mandatoryIfNameSpaceOfDataClassDeviatesElseOptional != null ) {
            for( String name : mandatoryIfNameSpaceOfDataClassDeviatesElseOptional ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"MOcdcNs\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                            "verification of PresenceCondition \"MOcdcNs\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ", getSclModelClassName(),
                            " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.INFO,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            notice.getMessage(),
                            new Object[] { sclModel, notice } ));
                }
            }
        }

        // presCond: "MOdataNs" :
        // Element is mandatory if the name space of its data object deviates from the name space of its logical node,
        // otherwise optional. See IEC 61850-7-1 for use of name space
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO: The meaning is not clear.
        if( mandatoryIfNameSpaceOfDataObjectDeviatesElseOptional != null ) {
            for( String name : mandatoryIfNameSpaceOfDataObjectDeviatesElseOptional ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"MOdataNs\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                                "verification of PresenceCondition \"MOdataNs\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ",
                                                getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(),
                                                " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.INFO,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            notice.getMessage(),
                            new Object[] { sclModel, notice } ));
                }
            }
        }

        // presCond: "MFscaledAV" :
        // Element is mandatory* if any sibling elements of type AnalogueValue include 'i' as a child, otherwise forbidden.
        // *Even though devices without floating point capability cannot exchange floating point values through ACSI services,
        // the description of scaling remains mandatory for their (SCL) configuration
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO
        if( mandatoryIfAnalogValueIncludesIElseForbidden != null ) {
            console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                    "validation of presence condition \"MFscaledAV\" for ", getSclComponentClassName(), " in ", getSclModelClassName(), " id = \"",
                    sclModel.getId(), "\" with ", getNsdModelClassName(),
                    " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
            res = validateMFscaledAV( sclModel, diagnostics ) && res;
        }

        // presCond: "MFscaledMagV" :
        // Element is mandatory* if any sibling elements of type Vector include 'i' as a child of their 'mag' attribute, otherwise forbidden.
        // *See MFscaledAV
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO
        if( mandatoryIfVectorSiblingIncludesIAsChildMagElseForbidden != null ) {
            console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                    "validation of presence condition \"MFscaledMagV\" for ", getSclComponentClassName(), " in ", getSclModelClassName(), " id = \"",
                    sclModel.getId(), "\" with ", getNsdModelClassName(),
                    " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
            res = validateMFscaledMagV( sclModel, diagnostics ) && res;
        }

        // presCond: "MFscaledAngV" :
        // Element is mandatory* if any sibling elements of type Vector include 'i' as a child of their 'ang' attribute, otherwise forbidden.
        // *See MFscaledAV
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO
        if( mandatoryIfVectorSiblingIncludesIAsChildAngElseForbidden != null ) {
            console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                    "validation of presence condition \"MFscaledAngV\" for ", getSclComponentClassName(), " in ", getSclModelClassName(), " id = \"",
                    sclModel.getId(), "\" with ", getNsdModelClassName(),
                    " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
            res = validateMFscaledAngV( sclModel, diagnostics ) && res;
        }

        // presCond: "MOrms" :
        // Element is mandatory if the harmonic values in the context are calculated as a ratio to RMS value
        // (value of data attribute 'hvRef' is 'rms'), optional otherwise
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO
        if( mandatoryIfHarmonicValuesCalculatedAsRatioElseOptional != null ) {
            for( String name : mandatoryIfHarmonicValuesCalculatedAsRatioElseOptional ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"MOrms\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                                "verification of PresenceCondition \"MOrms\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ",
                                                getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(),
                                                " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.INFO,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            notice.getMessage(),
                            new Object[] { sclModel, notice } ));
                }
            }
        }

        // presCond: "MOrootLD" :
        // Element is mandatory in the context of a root logical device; otherwise it is optional
        // Usage in standard NSD files (version 2007B): DataObject
        if( mandatoryInRootLogicalDeviceElseOptional != null ) {
            for( String name : mandatoryInRootLogicalDeviceElseOptional ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"MOrootLD\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                                "verification of PresenceCondition \"MOrootLD\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ",
                                                getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(),
                                                " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.INFO,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            notice.getMessage(),
                            new Object[] { sclModel, notice } ));
                }
            }
        }

        // presCond: "MOoperTm" :
        // Element is mandatory if at least one controlled object on the IED supports time activation service; otherwise it is optional
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO
        if( mandatoryIfControlSupportsTimeElseOptional != null ) {
            for( String name : mandatoryIfControlSupportsTimeElseOptional ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"MOoperTm\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                                "verification of PresenceCondition \"MOoperTm\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ",
                                                getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(),
                                                " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.INFO,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            notice.getMessage(),
                            new Object[] { sclModel, notice } ));
                }
            }
        }

        // presCond: "MmultiF" :
        // Parameter sibling: sibling element name.
        // One or more elements must be present if sibling element is present, otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataObject
        // TODO: One or more elements ? Is there an instance number ?
        if( oneOrMoreIfSiblingPresentElseForbidden != null ) {
            for( String name : oneOrMoreIfSiblingPresentElseForbidden.keySet() ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"MmultiF\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                                "verification of PresenceCondition \"MmultiF\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ",
                                                getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(),
                                                " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.INFO,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            notice.getMessage(),
                            new Object[] { sclModel, notice } ));
                }
            }
        }

        // presCond: "MOsbo" :
        // Element is mandatory if declared control model supports 'sbo-with-normal-security' or 'sbo-with-enhanced-security',
        // otherwise optional and value is of no impact
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO
        if( mandatoryIfControlSupportsSecurity1ElseOptional != null ) {
            for( String name : mandatoryIfControlSupportsSecurity1ElseOptional ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"MOsbo\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                                "verification of PresenceCondition \"MOsbo\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ",
                                                getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(),
                                                " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.INFO,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            notice.getMessage(),
                            new Object[] { sclModel, notice } ));
                }
            }
        }

        // presCond: "MOenhanced" :
        // Element is mandatory if declared control model supports 'direct-with-enhanced-security' or 'sbo-with-enhanced-security',
        // otherwise optional and value is of no impact
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO
        if( mandatoryIfControlSupportsSecurity2ElseOptional != null ) {
            for( String name : mandatoryIfControlSupportsSecurity2ElseOptional ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"MOenhanced\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                                "verification of PresenceCondition \"MOenhanced\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ",
                                                getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(),
                                                " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.INFO,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            notice.getMessage(),
                            new Object[] { sclModel, notice } ));
                }
            }
        }

        // presCond: "MONamPlt" :
        // Element is mandatory if the name space of its logical node deviates from the name space of the containing
        // logical device, otherwise optional. See IEC 61850-7-1 for use of name space
        // Usage in standard NSD files (version 2007B): DataObject
        // TODO: same as "MOlnNs" ?
        if( mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional2 != null ) {
            for( String name : mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional2 ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"MONamPlt\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                                "verification of PresenceCondition \"MONamPlt\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ",
                                                getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(),
                                                " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.INFO,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            notice.getMessage(),
                            new Object[] { sclModel, notice } ));
                }
            }
        }

        // presCond: "OF" :
        // Parameter sibling: sibling element name.
        // Optional if sibling element is present, otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataObject and DataAttribute
        if( optionalIfSiblingPresentElseForbidden != null ) {
            for( Entry< String, String > entry : optionalIfSiblingPresentElseForbidden.entrySet() ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"OF\" for ", getSclComponentClassName(), " element ", entry.getKey(), " sibling ", entry.getValue(),
                        " in ", getSclModelClassName(), " id = \"", sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( entry.getValue() ) == null ) {
                    if( presentSclComponent.get( entry.getKey() ) != null ) {
                        RiseClipseMessage error = RiseClipseMessage.error( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(), 
                                                  getSclComponentClassName(), " ", entry.getKey() + " is forbidden in ", getSclModelClassName(), " with ",
                                                  getNsdModelClassName(), " \"", getNsdModelName(), "\" because sibling ", entry.getValue(), " is not present",
                                                  " in namespace \"", nsIdentification, "\"" );
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                error.getMessage(),
                                new Object[] { sclModel, error } ));
                        res = false;
                    }
                }
            }
        }

        // presCond: "MORange" :
        // Element is mandatory if the measured value associated (amplitude respectively angle) exposes the range eventing
        // (with the attribute range respectively rangeAng)
        // Usage in standard NSD files (version 2007B): SubDataAttribute
        // TODO
        if( mandatoryIfMeasuredValueExposesRange != null ) {
            for( String name : mandatoryIfMeasuredValueExposesRange ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"MORange\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                                "verification of PresenceCondition \"MORange\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ",
                                                getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(),
                                                " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.INFO,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            notice.getMessage(),
                            new Object[] { sclModel, notice } ));
                }
            }
        }

        // presCond: "OMSynPh" :
        // This attribute is optional if value of 'phsRef'' is Synchrophasor otherwise Mandatory]]></Doc>
        // Usage in standard NSD files (version 2007B): SubDataObject
        // TODO
        if( optionalIfPhsRefIsSynchrophasorElseMandatory != null ) {
            console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                    "validation of presence condition \"OMSynPh\" for ", getSclComponentClassName(), " in ", getSclModelClassName(), " id = \"",
                    sclModel.getId(), "\" with ", getNsdModelClassName(),
                    " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
            res = validateOMSynPh( sclModel, diagnostics ) && res;
        }

        // presCond: "MAllOrNonePerGroup" :
        // TODO
        if( mAllOrNonePerGroup != null ) {
            for( String name : mAllOrNonePerGroup ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"MAllOrNonePerGroup\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                            "verification of PresenceCondition \"MAllOrNonePerGroup\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ",
                            getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(),
                            " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.INFO,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            notice.getMessage(),
                            new Object[] { sclModel, notice } ));
                }
            }
        }

        // presCond: "MOctrl" :
        // TODO
        if( mOctrl != null ) {
            for( String name : mOctrl ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"MOctrl\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                            "verification of PresenceCondition \"MOctrl\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ", getSclModelClassName(),
                            " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.INFO,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            notice.getMessage(),
                            new Object[] { sclModel, notice } ));
                }
            }
        }

        // presCond: "MOsboNormal" :
        // TODO
        if( mOsboNormal != null ) {
            for( String name : mOsboNormal ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"MOsboNormal\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                            "verification of PresenceCondition \"MOsboNormal\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ", getSclModelClassName(),
                            " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.INFO,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            notice.getMessage(),
                            new Object[] { sclModel, notice } ));
                }
            }
        }

        // presCond: "MOsboEnhanced" :
        // TODO
        if( mOsboEnhanced != null ) {
            for( String name : mOsboEnhanced ) {
                console.debug( getValidationMessageCategory(), sclModel.getFilename(), sclModel.getLineNumber(),
                        "validation of presence condition \"MOsboEnhanced\" for ", getSclComponentClassName(), " \"", name, "\" in ", getSclModelClassName(), " id = \"",
                        sclModel.getId(), "\" with ", getNsdModelClassName(),
                        " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                if( presentSclComponent.get( name ) != null ) {
                    RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                            "verification of PresenceCondition \"MOsboEnhanced\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ", getSclModelClassName(),
                            " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(), " in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.INFO,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            notice.getMessage(),
                            new Object[] { sclModel, notice } ));
                }
            }
        }

        return res;
    }

    protected abstract boolean validateMFln0( SclModel sclModel, DiagnosticChain diagnostics );

    protected abstract boolean validateMOln0( SclModel sclModel, DiagnosticChain diagnostics );

    protected abstract boolean validateOMSynPh( SclModel sclModel, DiagnosticChain diagnostics );
    
    protected boolean validateMFscaledAV( SclModel sclModel, DiagnosticChain diagnostics ) {
        for( String name : mandatoryIfAnalogValueIncludesIElseForbidden ) {
            if( presentSclComponent.get( name ) != null ) {
                RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                            "verification of PresenceCondition \"MFscaledAV\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ",
                                            getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(),
                                            " in namespace \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.INFO,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        notice.getMessage(),
                        new Object[] { sclModel, notice } ));
            }
        }
        return true;
    }
    
    protected boolean validateMFscaledMagV( SclModel sclModel, DiagnosticChain diagnostics ) {
        for( String name : mandatoryIfVectorSiblingIncludesIAsChildMagElseForbidden ) {
            if( presentSclComponent.get( name ) != null ) {
                RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                            "verification of PresenceCondition \"MFscaledMagV\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ",
                                            getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(),
                                            " in namespace \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.INFO,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        notice.getMessage(),
                        new Object[] { sclModel, notice } ));
            }
        }
        return true;
    }
    
    protected boolean validateMFscaledAngV( SclModel sclModel, DiagnosticChain diagnostics ) {
        for( String name : mandatoryIfVectorSiblingIncludesIAsChildAngElseForbidden ) {
            if( presentSclComponent.get( name ) != null ) {
                RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                            "verification of PresenceCondition \"MFscaledAngV\" for ", getSclComponentClassName(), " \"", name, "\" is not implemented in ",
                                            getSclModelClassName(), " with ", getNsdModelClassName(), " \"", getNsdModelName(), "\" at line ", getNsdModelLineNumber(),
                                            " in namespace \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.INFO,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        notice.getMessage(),
                        new Object[] { sclModel, notice } ));
            }
        }
        return true;
    }

}
