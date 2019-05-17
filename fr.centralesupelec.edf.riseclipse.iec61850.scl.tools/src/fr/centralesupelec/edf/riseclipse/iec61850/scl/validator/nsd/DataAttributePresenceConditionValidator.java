/**
 *  Copyright (c) 2019 CentraleSupélec & EDF.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  This file is part of the RiseClipse tool
 *  
 *  Contributors:
 *      Computer Science Department, CentraleSupélec
 *      EDF R&D
 *  Contacts:
 *      dominique.marcadet@centralesupelec.fr
 *      aurelie.dehouck-neveu@edf.fr
 *  Web site:
 *      http://wdi.supelec.fr/software/RiseClipse/
 */
package fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.nsd;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.common.util.EList;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.CDC;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AbstractDataObject;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DA;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DO;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DOType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.SDO;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;

public class DataAttributePresenceConditionValidator {
    
    private static HashMap< String, DataAttributePresenceConditionValidator > validators = new HashMap<>();
    
    public static DataAttributePresenceConditionValidator get( CDC cdc ) {
        if( ! validators.containsKey( cdc.getName() )) {
            validators.put( cdc.getName(), new DataAttributePresenceConditionValidator( cdc ));
        }
        return validators.get( cdc.getName() );
    }
    
    private CDC cdc;
    
    // Name of the DataAttribute/DA, DA
    private HashMap< String, DA > presentDA = new HashMap<>();
    
    private HashSet< String > mandatory;
    private HashSet< String > optional;
    private HashSet< String > forbidden;
//    private HashSet< String > notApplicable;
//    private HashSet< String > mandatoryMulti;
//    private HashSet< String > optionalMulti;
    private HashMap< Integer, HashSet< String > > atLeastOne;
    private HashSet< String > atMostOne;
    private HashMap< Integer, HashSet< String > > allOrNonePerGroup;
    private HashMap< Integer, HashSet< String > > allOnlyOneGroup;
    private HashMap< Integer, HashSet< String > > allAtLeastOneGroup;
    private HashMap< String, String > mandatoryIfSiblingPresentElseForbidden;
    private HashMap< String, String > mandatoryIfSiblingPresentElseOptional;
    private HashMap< String, String > optionalIfSiblingPresentElseMandatory;
    private HashMap< String, String > forbiddenIfSiblingPresentElseMandatory;
    private HashMap< String, String > mandatoryIfTextConditionElseOptional;
    private HashMap< String, String > mandatoryIfTextConditionElseForbidden;
    private HashMap< String, String > optionalIfTextConditionElseForbidden;
//    private HashMap< String, Pair< Integer, Integer > > mandatoryMultiRange;
//    private HashMap< String, Pair< Integer, Integer > > optionalMultiRange;
//    private HashSet< String > mandatoryIfSubstitutionElseForbidden;
    private HashSet< String > mandatoryInLLN0ElseOptional;
    private HashSet< String > mandatoryInLLN0ElseForbidden;
//    private HashSet< String > mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional;
//    private HashSet< String > mandatoryIfNameSpaceOfDataObjectDeviatesElseOptional;
//    private HashSet< String > mandatoryIfAnalogValueIncludesIElseForbidden;
//    private HashSet< String > mandatoryIfVectorSiblingIncludesIAsChildMagElseForbidden;
//    private HashSet< String > mandatoryIfVectorSiblingIncludesIAsChildAngElseForbidden;
//    private HashSet< String > mandatoryIfHarmonicValuesCalculatedAsRatioElseOptional;
//    private HashSet< String > mandatoryInRootLogicalDeviceElseOptional;
//    private HashSet< String > mandatoryIfControlSupportsTimeElseOptional;
//    private HashMap< String, String > oneOrMoreIfSiblingPresentElseForbidden;
//    private HashSet< String > mandatoryIfControlSupportsSecurity1ElseOptional;
//    private HashSet< String > mandatoryIfControlSupportsSecurity2ElseOptional;
    private HashMap< String, String > optionalIfSiblingPresentElseForbidden;
//    private HashSet< String > mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional2;
//    private HashSet< String > mandatoryIfMeasuredValueExposesRange;
//    private HashSet< String > optionalIfPhsRefIsSynchrophasorElseMandatory;
    
    private final IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
    
    public DataAttributePresenceConditionValidator( CDC cdc ) {
        this.cdc = cdc;
        
        cdc
        .getDataAttribute()
        .stream()
        .forEach( da -> addSpecification( da.getName(), da.getPresCond(), da.getPresCondArgs() ) );

        checkSpecification();
    }
    
    public void reset() {
        for( String da : presentDA.keySet() ) {
            presentDA.put( da, null );
        }
    }
    
    private void addSpecification( String name, String presCond, String presCondArgs ) {
        if( presentDA.containsKey( name )) {
            console.warning( "[NSD setup] " + name + " has already been added to DataAttributePresenceConditionValidator" );
            return;
        }
        presentDA.put( name, null );

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
            console.warning( "[NSD setup] NOT IMPLEMENTED: DataAttribute " + name + " declared as \"na\" in PresenceCondition" );
//            if( notApplicable == null ) notApplicable = new HashSet<>();
//            notApplicable.add( name );
            break;
        case "Mmulti" :
            // At least one element shall be present; all instances have an instance number > 0
            // -> TODO: not sure what is the instance number, it is assumed to be the suffix of DO name
            console.warning( "[NSD setup] NOT IMPLEMENTED: DataAttribute " + name + " declared as \"Mmulti\" in PresenceCondition" );
//            if( mandatoryMulti == null ) mandatoryMulti = new HashSet<>();
//            mandatoryMulti.add( name );
            break;
        case "Omulti" :
            // Zero or more elements may be present; all instances have an instance number > 0
            // -> TODO: not sure what is the instance number, it is assumed to be the suffix of DO name
            console.warning( "[NSD setup] NOT IMPLEMENTED: DataAttribute " + name + " declared as \"Omulti\" in PresenceCondition" );
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
                    console.warning( "[NSD setup] argument of PresenceCondition \"AtLeastOne\" is not a positive integer" );
                    break;
                }
                if( ! atLeastOne.containsKey( arg )) {
                    atLeastOne.put( arg, new HashSet<>() );
                }
                atLeastOne.get( arg ).add( name );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( "[NSD setup] argument of PresenceCondition \"AtLeastOne\" is not an integer" );
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
                    console.warning( "[NSD setup] argument of PresenceCondition \"AllOrNonePerGroup\" is not a positive integer" );
                    break;
                }
                if( ! allOrNonePerGroup.containsKey( arg )) {
                    allOrNonePerGroup.put( arg, new HashSet<>() );
                }
                allOrNonePerGroup.get( arg ).add( name );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( "[NSD setup] argument of PresenceCondition \"AllOrNonePerGroup\" is not an integer" );
                break;
            }
        case "AllOnlyOneGroup" :
            // Parameter n: group number (> 0).
            // All elements of only one group n shall be present
            if( allOnlyOneGroup == null ) allOnlyOneGroup = new HashMap<>();
            try {
                Integer arg = Integer.valueOf( presCondArgs );
                if( arg <= 0 ) {
                    console.warning( "[NSD setup] argument of PresenceCondition \"AllOnlyOneGroup\" is not a positive integer" );
                    break;
                }
                if( ! allOnlyOneGroup.containsKey( arg )) {
                    allOnlyOneGroup.put( arg, new HashSet<>() );
                }
                allOnlyOneGroup.get( arg ).add( name );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( "[NSD setup] argument of PresenceCondition \"AllOnlyOneGroup\" is not an integer" );
                break;
            }
        case "AllAtLeastOneGroup" :
            // Parameter n: group number (> 0).
            // All elements of at least one group n shall be present
            if( allAtLeastOneGroup == null ) allAtLeastOneGroup = new HashMap<>();
            try {
                Integer arg = Integer.valueOf( presCondArgs );
                if( arg <= 0 ) {
                    console.warning( "[NSD setup] argument of PresenceCondition \"AllAtLeastOneGroup\" is not a positive integer" );
                    break;
                }
                if( ! allAtLeastOneGroup.containsKey( arg )) {
                    allAtLeastOneGroup.put( arg, new HashSet<>() );
                }
                allAtLeastOneGroup.get( arg ).add( name );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( "[NSD setup] argument of PresenceCondition \"AllAtLeastOneGroup\" is not an integer" );
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
            try {
                Integer arg = Integer.valueOf( presCondArgs );
                if( arg <= 0 ) {
                    console.warning( "[NSD setup] argument of PresenceCondition \"MOcond\" is not a positive integer" );
                    break;
                }
                mandatoryIfTextConditionElseOptional.put( name, presCondArgs );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( "[NSD setup] argument of PresenceCondition \"MOcond\" is not an integer" );
                break;
            }
        case "MFcond" :
            // Parameter condID: condition number (> 0).
            // Textual presence condition (non-machine processable) with reference condID to context specific text.
            // If satisfied, the element is mandatory, otherwise forbidden
            if( mandatoryIfTextConditionElseForbidden == null ) mandatoryIfTextConditionElseForbidden = new HashMap<>();
            try {
                Integer arg = Integer.valueOf( presCondArgs );
                if( arg <= 0 ) {
                    console.warning( "[NSD setup] argument of PresenceCondition \"MFcond\" is not a positive integer" );
                    break;
                }
                mandatoryIfTextConditionElseForbidden.put( name, presCondArgs );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( "[NSD setup] argument of PresenceCondition \"MFcond\" is not an integer" );
                break;
            }
        case "OFcond" :
            // Parameter condID: condition number (> 0).
            // Textual presence condition (non-machine processable) with reference condID to context specific text.
            // If satisfied, the element is optional, otherwise forbidden
            if( optionalIfTextConditionElseForbidden == null ) optionalIfTextConditionElseForbidden = new HashMap<>();
            try {
                Integer arg = Integer.valueOf( presCondArgs );
                if( arg <= 0 ) {
                    console.warning( "[NSD setup] argument of PresenceCondition \"MFcond\" is not a positive integer" );
                    break;
                }
                optionalIfTextConditionElseForbidden.put( name, presCondArgs );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( "[NSD setup] argument of PresenceCondition \"MFcond\" is not an integer" );
                break;
            }
        case "MmultiRange" :
            // Parameters min, max: limits for instance number (> 0).
            // One or more elements shall be present; all instances have an instance number within range [min, max] (see IEC 61850-7-1)
            // -> TODO: not sure what is the instance number, it is assumed to be the suffix of DO name
            console.warning( "[NSD setup] NOT IMPLEMENTED: DataAttribute " + name + " declared as \"MmultiRange\" in PresenceCondition" );
//            if( mandatoryMultiRange == null ) mandatoryMultiRange = new HashMap<>();
//            String[] limits1 = presCondArgs.split( "[ ,]+" );
//            if( limits1.length != 2 ) {
//                console.warning( "[NSD setup] argument of PresenceCondition \"MmultiRange\" is not two integers" );
//                break;
//            }
//            Integer min1 = Integer.valueOf( limits1[0] );
//            if( min1 <= 0 ) {
//                console.warning( "[NSD setup] first argument of PresenceCondition \"MmultiRange\" is not a positive integer" );
//                break;
//            }
//            Integer max1 = Integer.valueOf( limits1[1] );
//            if( max1 <= 0 ) {
//                console.warning( "[NSD setup] second argument of PresenceCondition \"MmultiRange\" is not a positive integer" );
//                break;
//            }
//            mandatoryMultiRange.put( name, Pair.of( min1, max1 ));
            break;
        case "OmultiRange" :
            // Parameters min, max: limits for instance number (> 0).
            // Zero or more elements may be present; all instances have an instance number within range [min, max] (see IEC 61850-7-1)
            // -> TODO: not sure what is the instance number, it is assumed to be the suffix of DO name
            console.warning( "[NSD setup] NOT IMPLEMENTED: DataAttribute " + name + " declared as \"OmultiRange\" in PresenceCondition" );
//            if( optionalMultiRange == null ) optionalMultiRange = new HashMap<>();
//            String[] limits2 = presCondArgs.split( "[ ,]+" );
//            if( limits2.length != 2 ) {
//                console.warning( "[NSD setup] argument of PresenceCondition \"OmultiRange\" is not two integers" );
//                break;
//            }
//            Integer min2 = Integer.valueOf( limits2[0] );
//            if( min2 <= 0 ) {
//                console.warning( "[NSD setup] first argument of PresenceCondition \"OmultiRange\" is not a positive integer" );
//                break;
//            }
//            Integer max2 = Integer.valueOf( limits2[1] );
//            if( max2 <= 0 ) {
//                console.warning( "[NSD setup] second argument of PresenceCondition \"OmultiRange\" is not a positive integer" );
//                break;
//            }
//            optionalMultiRange.put( name, Pair.of( min2, max2 ));
            break;
        case "MFsubst" :
            // Element is mandatory if substitution is supported (for substitution, see IEC 61850-7-3), otherwise forbidden
            // TODO: how do we know if substitution is supported ?
            console.warning( "[NSD setup] NOT IMPLEMENTED: DataAttribute " + name + " declared as \"MFsubst\" in PresenceCondition" );
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
            console.warning( "[NSD setup] NOT IMPLEMENTED: DataAttribute " + name + " declared as \"MOlnNs\" in PresenceCondition" );
//            if( mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional == null ) mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional = new HashSet<>();
//            mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional.add( name );
            break;
        case "MOdataNs" :
            // Element is mandatory if the name space of its data object deviates from the name space of its logical node,
            // otherwise optional. See IEC 61850-7-1 for use of name space
            console.warning( "[NSD setup] NOT IMPLEMENTED: DataAttribute " + name + " declared as \"MOdataNs\" in PresenceCondition" );
//            if( mandatoryIfNameSpaceOfDataObjectDeviatesElseOptional == null ) mandatoryIfNameSpaceOfDataObjectDeviatesElseOptional = new HashSet<>();
//            mandatoryIfNameSpaceOfDataObjectDeviatesElseOptional.add( name );
            break;
        case "MFscaledAV" :
            // Element is mandatory* if any sibling elements of type AnalogueValue include 'i' as a child, otherwise forbidden.
            // *Even though devices without floating point capability cannot exchange floating point values through ACSI services,
            // the description of scaling remains mandatory for their (SCL) configuration
            console.warning( "[NSD setup] NOT IMPLEMENTED: DataAttribute " + name + " declared as \"MFscaledAV\" in PresenceCondition" );
//            if( mandatoryIfAnalogValueIncludesIElseForbidden == null ) mandatoryIfAnalogValueIncludesIElseForbidden = new HashSet<>();
//            mandatoryIfAnalogValueIncludesIElseForbidden.add( name );
            break;
        case "MFscaledMagV" :
            // Element is mandatory* if any sibling elements of type Vector include 'i' as a child of their 'mag' attribute, otherwise forbidden.
            // *See MFscaledAV
            console.warning( "[NSD setup] NOT IMPLEMENTED: DataAttribute " + name + " declared as \"MFscaledMagV\" in PresenceCondition" );
//            if( mandatoryIfVectorSiblingIncludesIAsChildMagElseForbidden == null ) mandatoryIfVectorSiblingIncludesIAsChildMagElseForbidden = new HashSet<>();
//            mandatoryIfVectorSiblingIncludesIAsChildMagElseForbidden.add( name );
            break;
        case "MFscaledAngV" :
            // Element is mandatory* if any sibling elements of type Vector include 'i' as a child of their 'ang' attribute, otherwise forbidden.
            // *See MFscaledAV
            console.warning( "[NSD setup] NOT IMPLEMENTED: DataAttribute " + name + " declared as \"MFscaledAngV\" in PresenceCondition" );
//            if( mandatoryIfVectorSiblingIncludesIAsChildAngElseForbidden == null ) mandatoryIfVectorSiblingIncludesIAsChildAngElseForbidden = new HashSet<>();
//            mandatoryIfVectorSiblingIncludesIAsChildAngElseForbidden.add( name );
            break;
        case "MOrms" :
            // Element is mandatory if the harmonic values in the context are calculated as a ratio to RMS value
            // (value of data attribute 'hvRef' is 'rms'), optional otherwise
            console.warning( "[NSD setup] NOT IMPLEMENTED: DataAttribute " + name + " declared as \"MOrms\" in PresenceCondition" );
//            if( mandatoryIfHarmonicValuesCalculatedAsRatioElseOptional == null ) mandatoryIfHarmonicValuesCalculatedAsRatioElseOptional = new HashSet<>();
//            mandatoryIfHarmonicValuesCalculatedAsRatioElseOptional.add( name );
            break;
        case "MOrootLD" :
            // Element is mandatory in the context of a root logical device; otherwise it is optional
            console.warning( "[NSD setup] NOT IMPLEMENTED: DataAttribute " + name + " declared as \"MOrootLD\" in PresenceCondition" );
//            if( mandatoryInRootLogicalDeviceElseOptional == null ) mandatoryInRootLogicalDeviceElseOptional = new HashSet<>();
//            mandatoryInRootLogicalDeviceElseOptional.add( name );
            break;
        case "MOoperTm" :
            // Element is mandatory if at least one controlled object on the IED supports time activation service; otherwise it is optional
            console.warning( "[NSD setup] NOT IMPLEMENTED: DataAttribute " + name + " declared as \"MOoperTm\" in PresenceCondition" );
//            if( mandatoryIfControlSupportsTimeElseOptional == null ) mandatoryIfControlSupportsTimeElseOptional = new HashSet<>();
//            mandatoryIfControlSupportsTimeElseOptional.add( name );
            break;
        case "MmultiF" :
            // Parameter sibling: sibling element name.
            // One or more elements must be present if sibling element is present, otherwise forbidden
            console.warning( "[NSD setup] NOT IMPLEMENTED: DataAttribute " + name + " declared as \"MmultiF\" in PresenceCondition" );
//            if( oneOrMoreIfSiblingPresentElseForbidden == null ) oneOrMoreIfSiblingPresentElseForbidden = new HashMap<>();
//            oneOrMoreIfSiblingPresentElseForbidden.put( name, presCondArgs );
            break;
        case "MOsbo" :
            // Element is mandatory if declared control model supports 'sbo-with-normal-security' or 'sbo-with-enhanced-security',
            // otherwise optional and value is of no impact
            console.warning( "[NSD setup] NOT IMPLEMENTED: DataAttribute " + name + " declared as \"MOsbo\" in PresenceCondition" );
//            if( mandatoryIfControlSupportsSecurity1ElseOptional == null ) mandatoryIfControlSupportsSecurity1ElseOptional = new HashSet<>();
//            mandatoryIfControlSupportsSecurity1ElseOptional.add( name );
            break;
        case "MOenhanced" :
            // Element is mandatory if declared control model supports 'direct-with-enhanced-security' or 'sbo-with-enhanced-security',
            // otherwise optional and value is of no impact
            console.warning( "[NSD setup] NOT IMPLEMENTED: DataAttribute " + name + " declared as \"MOenhanced\" in PresenceCondition" );
//            if( mandatoryIfControlSupportsSecurity2ElseOptional == null ) mandatoryIfControlSupportsSecurity2ElseOptional = new HashSet<>();
//            mandatoryIfControlSupportsSecurity2ElseOptional.add( name );
            break;
        case "MONamPlt" :
            // Element is mandatory if the name space of its logical node deviates from the name space of the containing
            // logical device, otherwise optional. See IEC 61850-7-1 for use of name space
            // TODO: same as "MOlnNs" ?
            console.warning( "[NSD setup] NOT IMPLEMENTED: DataAttribute " + name + " declared as \"MONamPlt\" in PresenceCondition" );
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
            console.warning( "[NSD setup] NOT IMPLEMENTED: DataAttribute " + name + " declared as \"MORange\" in PresenceCondition" );
//            if( mandatoryIfMeasuredValueExposesRange == null ) mandatoryIfMeasuredValueExposesRange = new HashSet<>();
//            mandatoryIfMeasuredValueExposesRange.add( name );
            break;
        case "OMSynPh" :
            // This attribute is optional if value of 'phsRef'' is Synchrophasor otherwise Mandatory]]></Doc>
            console.warning( "[NSD setup] NOT IMPLEMENTED: DataAttribute " + name + " declared as \"OMSynPh\" in PresenceCondition" );
//            if( optionalIfPhsRefIsSynchrophasorElseMandatory == null ) optionalIfPhsRefIsSynchrophasorElseMandatory = new HashSet<>();
//            optionalIfPhsRefIsSynchrophasorElseMandatory.add( name );
            break;
        default:
            console.warning( "[NSD setup] the PresenceCondition " + presCond + " of AnyLNClass " + name + " is unknown" );
            break;
        }
        
    }
    
    private void checkSpecification() {
        // TODO: do we have to check the presence of the sibling in inherited AbstractLNClass ?
        if( mandatoryIfSiblingPresentElseForbidden != null ) {
            for( Entry< String, String > e : mandatoryIfSiblingPresentElseForbidden.entrySet() ) {
                if( ! presentDA.containsKey( e.getValue() )) {
                    console.warning( "[NSD setup] the sibling of " + e.getKey() + " in PresenceCondition of DataObject " + e.getKey() + " is unknown" );
                }
            }
        }
        if( mandatoryIfSiblingPresentElseOptional != null ) {
            for( Entry< String, String > e : mandatoryIfSiblingPresentElseOptional.entrySet() ) {
                if( ! presentDA.containsKey( e.getValue() )) {
                    console.warning( "[NSD setup] the sibling of " + e.getKey() + " in PresenceCondition of DataObject " + e.getKey() + " is unknown" );
                }
            }
        }
        if( optionalIfSiblingPresentElseMandatory != null ) {
            for( Entry< String, String > e : optionalIfSiblingPresentElseMandatory.entrySet() ) {
                if( ! presentDA.containsKey( e.getValue() )) {
                    console.warning( "[NSD setup] the sibling of " + e.getKey() + " in PresenceCondition of DataObject " + e.getKey() + " is unknown" );
                }
            }
        }
        if( forbiddenIfSiblingPresentElseMandatory != null ) {
            for( Entry< String, String > e : forbiddenIfSiblingPresentElseMandatory.entrySet() ) {
                if( ! presentDA.containsKey( e.getValue() )) {
                    console.warning( "[NSD setup] the sibling of " + e.getKey() + " in PresenceCondition of DataObject " + e.getKey() + " is unknown" );
                }
            }
        }
//        if( oneOrMoreIfSiblingPresentElseForbidden != null ) {
//            for( Entry< String, String > e : oneOrMoreIfSiblingPresentElseForbidden.entrySet() ) {
//                if( ! presentDA.containsKey( e.getValue() )) {
//                    console.warning( "[NSD setup] the sibling of " + e.getKey() + " in PresenceCondition of DataObject " + e.getKey() + " is unknown" );
//                }
//            }
//        }
        if( optionalIfSiblingPresentElseForbidden != null ) {
            for( Entry< String, String > e : optionalIfSiblingPresentElseForbidden.entrySet() ) {
                if( ! presentDA.containsKey( e.getValue() )) {
                    console.warning( "[NSD setup] the sibling of " + e.getKey() + " in PresenceCondition of DataObject " + e.getKey() + " is unknown" );
                }
            }
        }
    }

    public boolean addDA( DA da, DiagnosticChain diagnostics ) {
        if( ! presentDA.containsKey( da.getName() )) {
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    "[NSD validation] DA " + da.getName() + " in DOType (line " + da.getParentDOType().getLineNumber() + ") not found in CDC " + cdc.getName(),
                    new Object[] { da } ));
            return false;
        }

        if( presentDA.get( da.getName() ) != null ) {
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    "[NSD validation] DA " + da.getName() + " in DOType (line " + da.getParentDOType().getLineNumber() + ") already present in CDC " + cdc.getName(),
                    new Object[] { da } ));
            return false;
        }
        presentDA.put( da.getName(), da );
        return true;
    }
    
    public boolean validate( DOType doType, DiagnosticChain diagnostics ) {
        boolean res = true;
        
        // presCond: "M"
        // Element is mandatory
        // Usage in standard NSD files (version 2007B): DataObject and DataAttribute and SubDataAttribute
        if( mandatory != null ) {
            for( String name : this.mandatory ) {
                if( presentDA.get( name ) == null ) {
                  diagnostics.add( new BasicDiagnostic(
                          Diagnostic.ERROR,
                          RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                          0,
                          "[NSD validation] DA " + name + " is mandatory in DOType (line " + doType.getLineNumber() + ") with CDC " + cdc.getName(),
                          new Object[] { doType } ));
                  res = false;
                }
            }
        }

        // presCond: "O"
        // Element is optional
        // Usage in standard NSD files (version 2007B): DataObject and DataAttribute and SubDataAttribute
        if( optional != null ) {
            for( String name : this.optional ) {
                if( presentDA.get( name ) == null ) {
                    // Nothing
                }
            }
        }

        // presCond: "F"
        // Element is forbidden
        // Usage in standard NSD files (version 2007B): DataObject
        if( forbidden != null ) {
            for( String name : this.forbidden ) {
                if( presentDA.get( name ) != null ) {
                  diagnostics.add( new BasicDiagnostic(
                          Diagnostic.ERROR,
                          RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                          0,
                          "[NSD validation] DA " + name + " is forbidden in DOType (line " + doType.getLineNumber() + ") with CDC " + cdc.getName(),
                          new Object[] { doType } ));
                  res = false;
                }
            }
        }

        // presCond: "na"
        // Element is not applicable
        // Usage in standard NSD files (version 2007B): only for dsPresCond
        // -> TODO: what does it mean ? what do we have to check ?
//        if( notApplicable != null ) {
//
//        }
        
        // presCond: "Mmulti"
        // At least one element shall be present; all instances have an instance number > 0
        // Usage in standard NSD files (version 2007B): DataObject
//        if( mandatoryMulti != null ) {
//
//        }

        // presCond: "Omulti"
        // Zero or more elements may be present; all instances have an instance number > 0
        // Usage in standard NSD files (version 2007B): DataObject
//        if( optionalMulti != null ) {
//
//        }
        
        // presCond: "AtLeastOne"
        // Parameter n: group number (> 0).
        // At least one of marked elements of a group n shall be present
        // Usage in standard NSD files (version 2007B): DataObject and SubDataObject and DataAttribute and SubDataAttribute
        if( atLeastOne != null ) {
            for( Entry< Integer, HashSet< String > > e1 : atLeastOne.entrySet() ) {
                boolean groupOK = false;
                for( String member : e1.getValue() ) {
                    if( presentDA.get( member ) != null ) {
                        groupOK = true;
                        break;
                    }
                }
                if( ! groupOK ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] group " + e1.getKey() + " has no elements in DOType (line " + doType.getLineNumber() + ") with CDC " + cdc.getName(),
                            new Object[] { doType } ));
                    res = false;
                }
            }
        }
        
        // presCond: "AtMostOne" :
        // At most one of marked elements shall be present
        // Usage in standard NSD files (version 2007B): DataObject
        if( atMostOne != null ) {
            int count = 0;
            for( String s : atMostOne ) {
                if( presentDA.get( s ) != null ) {
                    ++count;
                }
            }
            if( count > 1 ) {
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD validation] LNodeType (line " + doType.getLineNumber() + ") with CDC " + cdc.getName() + " has more than one element marked AtMostOne",
                        new Object[] { doType } ));
                res = false;
            }
        }
        
        // presCond: "AllOrNonePerGroup" :
        // Parameter n: group number (> 0).
        // All or none of the elements of a group n shall be present
        // Usage in standard NSD files (version 2007B): DataAttribute
        if( allOrNonePerGroup != null ) {
            for( Entry< Integer, HashSet< String > > e1 : allOrNonePerGroup.entrySet() ) {
                int groupCount = 0;
                for( String member : e1.getValue() ) {
                    if( presentDA.get( member ) != null ) {
                        ++groupCount;
                    }
                }
                if(( groupCount > 0 ) && (groupCount < e1.getValue().size() )) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] group " + e1.getKey() + " has neither none nor all elements in DOType (line " + doType.getLineNumber() + ") with CDC " + cdc.getName(),
                            new Object[] { doType } ));
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
                int groupCount = 0;
                for( String member : e1.getValue() ) {
                    if( presentDA.get( member ) != null ) {
                        ++groupCount;
                    }
                }
                if(( groupCount > 0 ) && (groupCount < e1.getValue().size() )) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] group " + e1.getKey() + " has neither none nor all elements in DOType (line " + doType.getLineNumber() + ") with CDC " + cdc.getName(),
                            new Object[] { doType } ));
                    res = false;
                }
                else if( groupCount > 0 ) {
                    if( groupNumber == 0 ) {
                        groupNumber = e1.getKey();
                    }
                    else {
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                "[NSD validation] LNodeType (line " + doType.getLineNumber() + ") with CDC " + cdc.getName() + " has several groups with all elements",
                                new Object[] { doType } ));
                        res = false;
                    }
                }
            }
            if( groupNumber == 0 ) {
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD validation] no group in DOType (line " + doType.getLineNumber() + ") with CDC " + cdc.getName() + " has all elements",
                        new Object[] { doType } ));
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
                int groupCount = 0;
                for( String member : e1.getValue() ) {
                    if( presentDA.get( member ) != null ) {
                        ++groupCount;
                    }
                }
                if( groupCount == e1.getValue().size() ) {
                    groupNumber = e1.getKey();
                }
            }
            if( groupNumber == 0 ) {
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD validation] no group in DOType (line " + doType.getLineNumber() + ") with CDC " + cdc.getName() + " has all elements",
                        new Object[] { doType } ));
                res = false;
            }
        }
        
        // presCond: "MF" :
        // Parameter sibling: sibling element name.
        // Mandatory if sibling element is present, otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataObject
        if( mandatoryIfSiblingPresentElseForbidden != null ) {
            for( Entry< String, String > entry : mandatoryIfSiblingPresentElseForbidden.entrySet() ) {
                if( presentDA.get( entry.getValue() ) != null ) {
                    if( presentDA.get( entry.getKey() ) == null ) {
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                "[NSD validation] DA " + entry.getKey() + " is mandatory in DOType (line " + doType.getLineNumber() + ") with LNClass "
                                        + cdc.getName() + " because sibling " + entry.getValue() + " is present",
                                new Object[] { doType } ));
                        res = false;
                    }
                }
                else {
                    if( presentDA.get( entry.getKey() ) != null ) {
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                "[NSD validation] DA " + entry.getKey() + " is forbidden in DOType (line " + doType.getLineNumber() + ") with LNClass "
                                        + cdc.getName() + " because sibling " + entry.getValue() + " is not present",
                                new Object[] { doType } ));
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
                if( presentDA.get( entry.getValue() ) != null ) {
                    if( presentDA.get( entry.getKey() ) == null ) {
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                "[NSD validation] DA " + entry.getKey() + " is mandatory in DOType (line " + doType.getLineNumber() + ") with LNClass "
                                        + cdc.getName() + " because sibling " + entry.getValue() + " is present",
                                new Object[] { doType } ));
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
                if( presentDA.get( entry.getValue() ) == null ) {
                    if( presentDA.get( entry.getKey() ) == null ) {
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                "[NSD validation] DA " + entry.getKey() + " is mandatory in DOType (line " + doType.getLineNumber() + ") with LNClass "
                                        + cdc.getName() + " because sibling " + entry.getValue() + " is not present",
                                new Object[] { doType } ));
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
                if( presentDA.get( entry.getValue() ) != null ) {
                    if( presentDA.get( entry.getKey() ) != null ) {
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                "[NSD validation] DA " + entry.getKey() + " is forbidden in DOType (line " + doType.getLineNumber() + ") with LNClass "
                                        + cdc.getName() + " because sibling " + entry.getValue() + " is present",
                                new Object[] { doType } ));
                        res = false;
                    }
                }
                else {
                    if( presentDA.get( entry.getKey() ) == null ) {
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                "[NSD validation] DA " + entry.getKey() + " is mandatory in DOType (line " + doType.getLineNumber() + ") with LNClass "
                                        + cdc.getName() + " because sibling " + entry.getValue() + " is not present",
                                new Object[] { doType } ));
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
                String doc = cdc
                        .getDataAttribute()
                        .stream()
                        .filter( d -> d.getName().equals( entry.getKey() ))
                        .findFirst()
                        .map( x -> x.getRefersToPresCondArgsDoc() )
                        .map( p -> p.getMixed() )
                        .map( p -> p.get( 0 ) )
                        .map( p -> p.getValue() )
                        .map( p -> p.toString() )
                        .orElse( null );

                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD validation] DA " + entry.getKey() + " is mandatory in DOType (line " + doType.getLineNumber() + ") with CDC "
                                + cdc.getName() + " if textual condition number " + entry.getValue() + " (not evaluated) is true, else optional. It is "
                                + ( presentDA.get( entry.getKey() ) == null ? "absent." : "present." ) + ( doc != null ? " Textual condition is: \"" + doc + "\"." : "" ),
                        new Object[] { doType } ));
            }
        }
        
        // presCond: "MFcond" :
        // Parameter condID: condition number (> 0).
        // Textual presence condition (non-machine processable) with reference condID to context specific text.
        // If satisfied, the element is mandatory, otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataObject
        if( mandatoryIfTextConditionElseForbidden != null ) {
            for( Entry< String, String > entry : mandatoryIfTextConditionElseForbidden.entrySet() ) {
                String doc = cdc
                        .getDataAttribute()
                        .stream()
                        .filter( d -> d.getName().equals( entry.getKey() ))
                        .findFirst()
                        .map( x -> x.getRefersToPresCondArgsDoc() )
                        .map( p -> p.getMixed() )
                        .map( p -> p.get( 0 ) )
                        .map( p -> p.getValue() )
                        .map( p -> p.toString() )
                        .orElse( null );

                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD validation] DA " + entry.getKey() + " is mandatory in DOType (line " + doType.getLineNumber() + ") with CDC "
                                + cdc.getName() + " if textual condition number " + entry.getValue() + " (not evaluated) is true, else forbidden. It is " 
                                + ( presentDA.get( entry.getKey() ) == null ? "absent." : "present." ) + ( doc != null ? " Textual condition is: \"" + doc + "\"." : "" ),
                        new Object[] { doType } ));
            }
        }
        
        // presCond: "OFcond" :
        // Parameter condID: condition number (> 0).
        // Textual presence condition (non-machine processable) with reference condID to context specific text.
        // If satisfied, the element is optional, otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataObject
        if( optionalIfTextConditionElseForbidden != null ) {
            for( Entry< String, String > entry : optionalIfTextConditionElseForbidden.entrySet() ) {
                String doc = cdc
                        .getDataAttribute()
                        .stream()
                        .filter( d -> d.getName().equals( entry.getKey() ))
                        .findFirst()
                        .map( x -> x.getRefersToPresCondArgsDoc() )
                        .map( p -> p.getMixed() )
                        .map( p -> p.get( 0 ) )
                        .map( p -> p.getValue() )
                        .map( p -> p.toString() )
                        .orElse( null );

                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD validation] DA " + entry.getKey() + " is optional in DOType (line " + doType.getLineNumber() + ") with CDC "
                                + cdc.getName() + " if textual condition number " + entry.getValue() + " (not evaluated) is true, else forbidden. It is " 
                                + ( presentDA.get( entry.getKey() ) == null ? "absent." : "present." ) + ( doc != null ? " Textual condition is: \"" + doc + "\"." : "" ),
                        new Object[] { doType } ));
            }
        }
        
        // presCond: "MmultiRange" :
        // Parameters min, max: limits for instance number (> 0).
        // One or more elements shall be present; all instances have an instance number within range [min, max] (see IEC 61850-7-1)
        // Usage in standard NSD files (version 2007B): None
//        if( mandatoryMultiRange != null ) {
//
//        }

        // presCond: "OmultiRange" :
        // Parameters min, max: limits for instance number (> 0).
        // Zero or more elements may be present; all instances have an instance number within range [min, max] (see IEC 61850-7-1)
        // Usage in standard NSD files (version 2007B): DataObject
//        if( optionalMultiRange != null ) {
//
//        }

        // presCond: "MFsubst" :
        // Element is mandatory if substitution is supported (for substitution, see IEC 61850-7-3), otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO
//        if( mandatoryIfSubstitutionElseForbidden != null ) {
//            
//        }
        
        // presCond: "MOln0" :
        // Element is mandatory in the context of LLN0; otherwise optional
        // Usage in standard NSD files (version 2007B): DataAttribute
        if( mandatoryInLLN0ElseOptional != null ) {
            EList< AbstractDataObject > adoList = doType.getReferredByAbstractDataObject();
            for( AbstractDataObject ado : adoList ) {
                if( ado instanceof DO ) {
                    DO do_ = ( DO ) ado;
                    if( "LLN0".equals( do_.getParentLNodeType().getLnClass() )) {
                        for( String attribute : mandatoryInLLN0ElseOptional ) {
                            DA da = presentDA.get( attribute );
                            if( da == null ) {
                                diagnostics.add( new BasicDiagnostic(
                                        Diagnostic.ERROR,
                                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                        0,
                                        "[NSD validation] DA " + attribute + " is mandatory in DOType (line " + doType.getLineNumber() + ") with LNClass LLN0",
                                        new Object[] { doType } ));
                                res = false;
                            }
                        }
                    }
                }
                else {
                    // ado instanceof SDO
                }
            }
        }
        
        // presCond: "MFln0" :
        // Element is mandatory in the context of LLN0; otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataAttribute
        if( mandatoryInLLN0ElseForbidden != null ) {
            EList< AbstractDataObject > adoList = doType.getReferredByAbstractDataObject();
            for( AbstractDataObject ado : adoList ) {
                if( ado instanceof DO ) {
                    DO do_ = ( DO ) ado;
                    if( "LLN0".equals( do_.getParentLNodeType().getLnClass() )) {
                        for( String attribute : mandatoryInLLN0ElseForbidden ) {
                            DA da = presentDA.get( attribute );
                            if( da == null ) {
                                diagnostics.add( new BasicDiagnostic(
                                        Diagnostic.ERROR,
                                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                        0,
                                        "[NSD validation] DA " + attribute + " is mandatory in DOType (line " + doType.getLineNumber() + ") with LNClass LLN0",
                                        new Object[] { doType } ));
                                res = false;
                            }
                        }
                    }
                    else {
                        for( String attribute : mandatoryInLLN0ElseForbidden ) {
                            DA da = presentDA.get( attribute );
                            if( da != null ) {
                                diagnostics.add( new BasicDiagnostic(
                                        Diagnostic.ERROR,
                                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                        0,
                                        "[NSD validation] DA " + attribute + " is forbidden in DOType (line " + doType.getLineNumber() + ") with LNClass " + do_.getParentLNodeType().getLnClass(),
                                        new Object[] { doType } ));
                                res = false;
                            }
                        }
                    }
                }
                else {
                    // ado instanceof SDO
                }
            }
        }

        // presCond: "MOlnNs" :
        // Element is mandatory if the name space of its logical node deviates from the name space of the containing
        // logical device, otherwise optional. See IEC 61850-7-1 for use of name space
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO: The meaning is not clear.
//        if( mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional != null ) {
//        
//        }

        // presCond: "MOdataNs" :
        // Element is mandatory if the name space of its data object deviates from the name space of its logical node,
        // otherwise optional. See IEC 61850-7-1 for use of name space
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO: The meaning is not clear.
//        if( mandatoryIfNameSpaceOfDataObjectDeviatesElseOptional != null ) {
//            
//        }

        // presCond: "MFscaledAV" :
        // Element is mandatory* if any sibling elements of type AnalogueValue include 'i' as a child, otherwise forbidden.
        // *Even though devices without floating point capability cannot exchange floating point values through ACSI services,
        // the description of scaling remains mandatory for their (SCL) configuration
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO
//        if( mandatoryIfAnalogValueIncludesIElseForbidden != null ) {
//            
//        }

        // presCond: "MFscaledMagV" :
        // Element is mandatory* if any sibling elements of type Vector include 'i' as a child of their 'mag' attribute, otherwise forbidden.
        // *See MFscaledAV
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO
//        if( mandatoryIfVectorSiblingIncludesIAsChildMagElseForbidden != null ) {
//        
//        }

        // presCond: "MFscaledAngV" :
        // Element is mandatory* if any sibling elements of type Vector include 'i' as a child of their 'ang' attribute, otherwise forbidden.
        // *See MFscaledAV
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO
//        if( mandatoryIfVectorSiblingIncludesIAsChildAngElseForbidden != null ) {
//            
//        }

        // presCond: "MOrms" :
        // Element is mandatory if the harmonic values in the context are calculated as a ratio to RMS value
        // (value of data attribute 'hvRef' is 'rms'), optional otherwise
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO
//        if( mandatoryIfHarmonicValuesCalculatedAsRatioElseOptional != null ) {
//            
//        }

        // presCond: "MOrootLD" :
        // Element is mandatory in the context of a root logical device; otherwise it is optional
        // Usage in standard NSD files (version 2007B): DataObject
//        if( mandatoryInRootLogicalDeviceElseOptional != null ) {
//
//        }

        // presCond: "MOoperTm" :
        // Element is mandatory if at least one controlled object on the IED supports time activation service; otherwise it is optional
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO
//        if( mandatoryIfControlSupportsTimeElseOptional != null ) {
//            
//        }

        // presCond: "MmultiF" :
        // Parameter sibling: sibling element name.
        // One or more elements must be present if sibling element is present, otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataObject
        // TODO: One or more elements ? Is there an instance number ?
//        if( oneOrMoreIfSiblingPresentElseForbidden != null ) {
//            
//        }

        // presCond: "MOsbo" :
        // Element is mandatory if declared control model supports 'sbo-with-normal-security' or 'sbo-with-enhanced-security',
        // otherwise optional and value is of no impact
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO
//        if( mandatoryIfControlSupportsSecurity1ElseOptional != null ) {
//            
//        }

        // presCond: "MOenhanced" :
        // Element is mandatory if declared control model supports 'direct-with-enhanced-security' or 'sbo-with-enhanced-security',
        // otherwise optional and value is of no impact
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO
//        if( mandatoryIfControlSupportsSecurity2ElseOptional != null ) {
//            
//        }

        // presCond: "MONamPlt" :
        // Element is mandatory if the name space of its logical node deviates from the name space of the containing
        // logical device, otherwise optional. See IEC 61850-7-1 for use of name space
        // Usage in standard NSD files (version 2007B): DataObject
        // TODO: same as "MOlnNs" ?
//        if( mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional2 != null ) {
//            
//        }

        // presCond: "OF" :
        // Parameter sibling: sibling element name.
        // Optional if sibling element is present, otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataObject and DataAttribute
        if( optionalIfSiblingPresentElseForbidden != null ) {
            for( Entry< String, String > entry : optionalIfSiblingPresentElseForbidden.entrySet() ) {
                if( presentDA.get( entry.getValue() ) == null ) {
                    if( presentDA.get( entry.getKey() ) != null ) {
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                "[NSD validation] DA " + entry.getKey() + " is forbidden in DOType (line " + doType.getLineNumber() + ") with LNClass "
                                        + cdc.getName() + " because sibling " + entry.getValue() + " is not present",
                                new Object[] { doType } ));
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
//        if( mandatoryIfMeasuredValueExposesRange != null ) {
//            
//        }

        // presCond: "OMSynPh" :
        // This attribute is optional if value of 'phsRef'' is Synchrophasor otherwise Mandatory]]></Doc>
        // Usage in standard NSD files (version 2007B): SubDataObject
        // TODO
//        if( optionalIfPhsRefIsSynchrophasorElseMandatory != null ) {
//            
//        }

        return res;
    }

}
