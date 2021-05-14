/*
*************************************************************************
**  Copyright (c) 2019 CentraleSupélec & EDF.
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
**      http://wdi.centralesupelec.fr/software/RiseClipse/
*************************************************************************
*/
package fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.nsd;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.AnyLNClass;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AnyLN;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DA;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DO;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DOI;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.LN0;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.LNodeType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.Val;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;

public class DataObjectPresenceConditionValidator {
    
    private static HashMap< String, DataObjectPresenceConditionValidator > validators = new HashMap<>();
    
    public static DataObjectPresenceConditionValidator get( AnyLNClass anyLNClass ) {
        if( ! validators.containsKey( anyLNClass.getName() )) {
            validators.put( anyLNClass.getName(), new DataObjectPresenceConditionValidator( anyLNClass ));
        }
        return validators.get( anyLNClass.getName() );
    }
    
    private AnyLNClass anyLNClass;
    private DataObjectPresenceConditionValidator base;
    
    private static class SingleOrMultiDO {
    }
    private static class SingleDO extends SingleOrMultiDO {
        @SuppressWarnings( "unused" )
        DO do_;

        SingleDO( DO do_ ) {
            this.do_ = do_;
        }
    }
    private static class MultiDO extends SingleOrMultiDO {
        HashMap< Integer, DO > numberedDOs = new HashMap<>();
        
        void add( Integer number, DO do_ ) {
            this.numberedDOs.put( number, do_ );
        }
    }
        
    // Name of the DataObject/DO, DO
    private HashMap< String, SingleOrMultiDO > presentDO = new HashMap<>();
    
    private HashSet< String > mandatory;
    private HashSet< String > optional;
    private HashSet< String > forbidden;
    private HashSet< String > notApplicable;
    private HashSet< String > mandatoryMulti;
    private HashSet< String > optionalMulti;
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
    private HashMap< String, Pair< Integer, Integer > > mandatoryMultiRange;
    private HashMap< String, Pair< Integer, Integer > > optionalMultiRange;
    private HashSet< String > mandatoryIfSubstitutionElseForbidden;
    private HashSet< String > mandatoryInLLN0ElseOptional;
    private HashSet< String > mandatoryInLLN0ElseForbidden;
    private HashSet< String > mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional;
    private HashSet< String > mandatoryIfNameSpaceOfDataObjectDeviatesElseOptional;
    private HashSet< String > mandatoryIfAnalogValueIncludesIElseForbidden;
    private HashSet< String > mandatoryIfVectorSiblingIncludesIAsChildMagElseForbidden;
    private HashSet< String > mandatoryIfVectorSiblingIncludesIAsChildAngElseForbidden;
    private HashSet< String > mandatoryIfHarmonicValuesCalculatedAsRatioElseOptional;
    private HashSet< String > mandatoryInRootLogicalDeviceElseOptional;
    private HashSet< String > mandatoryIfControlSupportsTimeElseOptional;
    private HashMap< String, String > oneOrMoreIfSiblingPresentElseForbidden;
    private HashSet< String > mandatoryIfControlSupportsSecurity1ElseOptional;
    private HashSet< String > mandatoryIfControlSupportsSecurity2ElseOptional;
    private HashMap< String, String > optionalIfSiblingPresentElseForbidden;
    private HashSet< String > mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional2;
    private HashSet< String > mandatoryIfMeasuredValueExposesRange;
    private HashSet< String > optionalIfPhsRefIsSynchrophasorElseMandatory;
    
    private final IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
    
    @SuppressWarnings( "unchecked" )        // cast of HashMap.clone() result
    private DataObjectPresenceConditionValidator( AnyLNClass anyLNClass ) {
        this.anyLNClass = anyLNClass;
        
        // Build validator for parent first, because it is needed (atLeastOne for example)
        AnyLNClass parent = anyLNClass.getRefersToAbstractLNClass();
        if( parent != null ) {
            base = get( parent );
        }
        
        // Some presence condition must be checked at the lowest LNClass (in the inheritance
        // hierarchy) having this specification (AtLeastOne for example, see other changes 
        // in the same commit).
        // To make it simple, we will check such presence condition only in the leaves of
        // the inheritance hierarchy, even if there is no such element with this presence
        // condition.
        // To do this, we put a non null dictionary for the corresponding condition in the root,
        // and duplicate it in each sub-LNClass for adding specific elements
        if( base == null ) {
            atLeastOne = new HashMap<>();
            atMostOne = new HashSet<>();
            allOrNonePerGroup = new HashMap<>();
            allOnlyOneGroup = new HashMap<>();
            allAtLeastOneGroup = new HashMap<>();
        }
        else {
            atLeastOne = ( HashMap< Integer, HashSet< String > > ) base.atLeastOne.clone();
            // We also need to add corresponding keys in presentDO
            for( Integer group : atLeastOne.keySet() ) {
                for( String name : atLeastOne.get( group )) {
                    presentDO.put( name, null );
                }
            }
            
            atMostOne = ( HashSet< String > ) base.atMostOne.clone();
            for( String name : atMostOne ) {
                presentDO.put( name, null );
            }

            allOrNonePerGroup = ( HashMap< Integer, HashSet< String > > ) base.allOrNonePerGroup.clone();
            for( Integer group : allOrNonePerGroup.keySet() ) {
                for( String name : allOrNonePerGroup.get( group )) {
                    presentDO.put( name, null );
                }
            }
            
            allOnlyOneGroup = ( HashMap< Integer, HashSet< String > > ) base.allOnlyOneGroup.clone();
            for( Integer group : allOnlyOneGroup.keySet() ) {
                for( String name : allOnlyOneGroup.get( group )) {
                    presentDO.put( name, null );
                }
            }
            
            allAtLeastOneGroup = ( HashMap< Integer, HashSet< String > > ) base.allAtLeastOneGroup.clone();
            for( Integer group : allAtLeastOneGroup.keySet() ) {
                for( String name : allAtLeastOneGroup.get( group )) {
                    presentDO.put( name, null );
                }
            }
        }

        anyLNClass
        .getDataObject()
        .stream()
        .forEach( d -> addSpecification( d.getName(), d.getPresCond(), d.getPresCondArgs(), d.getLineNumber(), d.getFilename() ));
        
        checkSpecification();
    }
    
    public void reset() {
        for( String do_ : presentDO.keySet() ) {
            presentDO.put( do_, null );
        }
        
        if( base != null ) base.reset();
    }
    
    private void addSpecification( String name, String presCond, String presCondArgs, int lineNumber, String filename ) {
        if( presentDO.containsKey( name )) {
            console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") " + name + " has already been added to DataObjectPresenceConditionValidator" );
            return;
        }
        presentDO.put( name, null );

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
            console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") NOT IMPLEMENTED: DataObject " + name + " declared as \"na\" in PresenceCondition" );
            if( notApplicable == null ) notApplicable = new HashSet<>();
            notApplicable.add( name );
            break;
        case "Mmulti" :
            // At least one element shall be present; all instances have an instance number > 0
            // -> TODO: not sure what is the instance number, it is assumed to be the suffix of DO name
            if( mandatoryMulti == null ) mandatoryMulti = new HashSet<>();
            mandatoryMulti.add( name );
            break;
        case "Omulti" :
            // Zero or more elements may be present; all instances have an instance number > 0
            // -> TODO: not sure what is the instance number, it is assumed to be the suffix of DO name
            if( optionalMulti == null ) optionalMulti = new HashSet<>();
            optionalMulti.add( name );
            break;
        case "AtLeastOne" :
            // Parameter n: group number (> 0).
            // At least one of marked elements of a group n shall be present
            // Not needed (see constructor)
            //if( atLeastOne == null ) atLeastOne = new HashMap<>();
            try {
                Integer arg = Integer.valueOf( presCondArgs );
                if( arg <= 0 ) {
                    console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") argument of PresenceCondition \"AtLeastOne\" is not a positive integer" );
                    break;
                }
                if( ! atLeastOne.containsKey( arg )) {
                    atLeastOne.put( arg, new HashSet<>() );
                }
                atLeastOne.get( arg ).add( name );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") argument of PresenceCondition \"AtLeastOne\" is not an integer" );
                break;
            }
        case "AtMostOne" :
            // At most one of marked elements shall be present
            //if( atMostOne == null ) atMostOne = new HashSet<>();
            atMostOne.add( name );
            break;
        case "AllOrNonePerGroup" :
            // Parameter n: group number (> 0).
            // All or none of the elements of a group n shall be present
            //if( allOrNonePerGroup == null ) allOrNonePerGroup = new HashMap<>();
            try {
                Integer arg = Integer.valueOf( presCondArgs );
                if( arg <= 0 ) {
                    console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") argument of PresenceCondition \"AllOrNonePerGroup\" is not a positive integer" );
                    break;
                }
                if( ! allOrNonePerGroup.containsKey( arg )) {
                    allOrNonePerGroup.put( arg, new HashSet<>() );
                }
                allOrNonePerGroup.get( arg ).add( name );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") argument of PresenceCondition \"AllOrNonePerGroup\" is not an integer" );
                break;
            }
        case "AllOnlyOneGroup" :
            // Parameter n: group number (> 0).
            // All elements of only one group n shall be present
            //if( allOnlyOneGroup == null ) allOnlyOneGroup = new HashMap<>();
            try {
                Integer arg = Integer.valueOf( presCondArgs );
                if( arg <= 0 ) {
                    console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") argument of PresenceCondition \"AllOnlyOneGroup\" is not a positive integer" );
                    break;
                }
                if( ! allOnlyOneGroup.containsKey( arg )) {
                    allOnlyOneGroup.put( arg, new HashSet<>() );
                }
                allOnlyOneGroup.get( arg ).add( name );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") argument of PresenceCondition \"AllOnlyOneGroup\" is not an integer" );
                break;
            }
        case "AllAtLeastOneGroup" :
            // Parameter n: group number (> 0).
            // All elements of at least one group n shall be present
            //if( allAtLeastOneGroup == null ) allAtLeastOneGroup = new HashMap<>();
            try {
                Integer arg = Integer.valueOf( presCondArgs );
                if( arg <= 0 ) {
                    console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") argument of PresenceCondition \"AllAtLeastOneGroup\" is not a positive integer" );
                    break;
                }
                if( ! allAtLeastOneGroup.containsKey( arg )) {
                    allAtLeastOneGroup.put( arg, new HashSet<>() );
                }
                allAtLeastOneGroup.get( arg ).add( name );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") argument of PresenceCondition \"AllAtLeastOneGroup\" is not an integer" );
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
                    console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") argument of PresenceCondition \"MOcond\" is not a positive integer" );
                    break;
                }
                mandatoryIfTextConditionElseOptional.put( name, presCondArgs );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") argument of PresenceCondition \"MOcond\" is not an integer" );
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
                    console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") argument of PresenceCondition \"MFcond\" is not a positive integer" );
                    break;
                }
                mandatoryIfTextConditionElseForbidden.put( name, presCondArgs );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") argument of PresenceCondition \"MFcond\" is not an integer" );
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
                    console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") argument of PresenceCondition \"MFcond\" is not a positive integer" );
                    break;
                }
                optionalIfTextConditionElseForbidden.put( name, presCondArgs );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") argument of PresenceCondition \"MFcond\" is not an integer" );
                break;
            }
        case "MmultiRange" :
            // Parameters min, max: limits for instance number (> 0).
            // One or more elements shall be present; all instances have an instance number within range [min, max] (see IEC 61850-7-1)
            // -> TODO: not sure what is the instance number, it is assumed to be the suffix of DO name
            if( mandatoryMultiRange == null ) mandatoryMultiRange = new HashMap<>();
            String[] limits1 = presCondArgs.split( "[ ,]+" );
            if( limits1.length != 2 ) {
                console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") argument of PresenceCondition \"MmultiRange\" is not two integers" );
                break;
            }
            Integer min1 = Integer.valueOf( limits1[0] );
            if( min1 <= 0 ) {
                console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") first argument of PresenceCondition \"MmultiRange\" is not a positive integer" );
                break;
            }
            Integer max1 = Integer.valueOf( limits1[1] );
            if( max1 <= 0 ) {
                console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") second argument of PresenceCondition \"MmultiRange\" is not a positive integer" );
                break;
            }
            mandatoryMultiRange.put( name, Pair.of( min1, max1 ));
            break;
        case "OmultiRange" :
            // Parameters min, max: limits for instance number (> 0).
            // Zero or more elements may be present; all instances have an instance number within range [min, max] (see IEC 61850-7-1)
            // -> TODO: not sure what is the instance number, it is assumed to be the suffix of DO name
            if( optionalMultiRange == null ) optionalMultiRange = new HashMap<>();
            String[] limits2 = presCondArgs.split( "[ ,]+" );
            if( limits2.length != 2 ) {
                console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") argument of PresenceCondition \"OmultiRange\" is not two integers" );
                break;
            }
            Integer min2 = Integer.valueOf( limits2[0] );
            if( min2 <= 0 ) {
                console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") first argument of PresenceCondition \"OmultiRange\" is not a positive integer" );
                break;
            }
            Integer max2 = Integer.valueOf( limits2[1] );
            if( max2 <= 0 ) {
                console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") second argument of PresenceCondition \"OmultiRange\" is not a positive integer" );
                break;
            }
            optionalMultiRange.put( name, Pair.of( min2, max2 ));
            break;
        case "MFsubst" :
            // Element is mandatory if substitution is supported (for substitution, see IEC 61850-7-3), otherwise forbidden
            // TODO: how do we know if substitution is supported ?
            console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") NOT IMPLEMENTED: DataObject " + name + " declared as \"MFsubst\" in PresenceCondition" );
            if( mandatoryIfSubstitutionElseForbidden == null ) mandatoryIfSubstitutionElseForbidden = new HashSet<>();
            mandatoryIfSubstitutionElseForbidden.add( name );
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
            console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") NOT IMPLEMENTED: DataObject " + name + " declared as \"MOlnNs\" in PresenceCondition" );
            if( mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional == null ) mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional = new HashSet<>();
            mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional.add( name );
            break;
        case "MOdataNs" :
            // Element is mandatory if the name space of its data object deviates from the name space of its logical node,
            // otherwise optional. See IEC 61850-7-1 for use of name space
            console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") NOT IMPLEMENTED: DataObject " + name + " declared as \"MOdataNs\" in PresenceCondition" );
            if( mandatoryIfNameSpaceOfDataObjectDeviatesElseOptional == null ) mandatoryIfNameSpaceOfDataObjectDeviatesElseOptional = new HashSet<>();
            mandatoryIfNameSpaceOfDataObjectDeviatesElseOptional.add( name );
            break;
        case "MFscaledAV" :
            // Element is mandatory* if any sibling elements of type AnalogueValue include 'i' as a child, otherwise forbidden.
            // *Even though devices without floating point capability cannot exchange floating point values through ACSI services,
            // the description of scaling remains mandatory for their (SCL) configuration
            console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") NOT IMPLEMENTED: DataObject " + name + " declared as \"MFscaledAV\" in PresenceCondition" );
            if( mandatoryIfAnalogValueIncludesIElseForbidden == null ) mandatoryIfAnalogValueIncludesIElseForbidden = new HashSet<>();
            mandatoryIfAnalogValueIncludesIElseForbidden.add( name );
            break;
        case "MFscaledMagV" :
            // Element is mandatory* if any sibling elements of type Vector include 'i' as a child of their 'mag' attribute, otherwise forbidden.
            // *See MFscaledAV
            console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") NOT IMPLEMENTED: DataObject " + name + " declared as \"MFscaledMagV\" in PresenceCondition" );
            if( mandatoryIfVectorSiblingIncludesIAsChildMagElseForbidden == null ) mandatoryIfVectorSiblingIncludesIAsChildMagElseForbidden = new HashSet<>();
            mandatoryIfVectorSiblingIncludesIAsChildMagElseForbidden.add( name );
            break;
        case "MFscaledAngV" :
            // Element is mandatory* if any sibling elements of type Vector include 'i' as a child of their 'ang' attribute, otherwise forbidden.
            // *See MFscaledAV
            console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") NOT IMPLEMENTED: DataObject " + name + " declared as \"MFscaledAngV\" in PresenceCondition" );
            if( mandatoryIfVectorSiblingIncludesIAsChildAngElseForbidden == null ) mandatoryIfVectorSiblingIncludesIAsChildAngElseForbidden = new HashSet<>();
            mandatoryIfVectorSiblingIncludesIAsChildAngElseForbidden.add( name );
            break;
        case "MOrms" :
            // Element is mandatory if the harmonic values in the context are calculated as a ratio to RMS value
            // (value of data attribute 'hvRef' is 'rms'), optional otherwise
            console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") NOT IMPLEMENTED: DataObject " + name + " declared as \"MOrms\" in PresenceCondition" );
            if( mandatoryIfHarmonicValuesCalculatedAsRatioElseOptional == null ) mandatoryIfHarmonicValuesCalculatedAsRatioElseOptional = new HashSet<>();
            mandatoryIfHarmonicValuesCalculatedAsRatioElseOptional.add( name );
            break;
        case "MOrootLD" :
            // Element is mandatory in the context of a root logical device; otherwise it is optional
            if( mandatoryInRootLogicalDeviceElseOptional == null ) mandatoryInRootLogicalDeviceElseOptional = new HashSet<>();
            mandatoryInRootLogicalDeviceElseOptional.add( name );
            break;
        case "MOoperTm" :
            // Element is mandatory if at least one controlled object on the IED supports time activation service; otherwise it is optional
            console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") NOT IMPLEMENTED: DataObject " + name + " declared as \"MOoperTm\" in PresenceCondition" );
            if( mandatoryIfControlSupportsTimeElseOptional == null ) mandatoryIfControlSupportsTimeElseOptional = new HashSet<>();
            mandatoryIfControlSupportsTimeElseOptional.add( name );
            break;
        case "MmultiF" :
            // Parameter sibling: sibling element name.
            // One or more elements must be present if sibling element is present, otherwise forbidden
            console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") NOT IMPLEMENTED: DataObject " + name + " declared as \"MmultiF\" in PresenceCondition" );
            if( oneOrMoreIfSiblingPresentElseForbidden == null ) oneOrMoreIfSiblingPresentElseForbidden = new HashMap<>();
            oneOrMoreIfSiblingPresentElseForbidden.put( name, presCondArgs );
            break;
        case "MOsbo" :
            // Element is mandatory if declared control model supports 'sbo-with-normal-security' or 'sbo-with-enhanced-security',
            // otherwise optional and value is of no impact
            console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") NOT IMPLEMENTED: DataObject " + name + " declared as \"MOsbo\" in PresenceCondition" );
            if( mandatoryIfControlSupportsSecurity1ElseOptional == null ) mandatoryIfControlSupportsSecurity1ElseOptional = new HashSet<>();
            mandatoryIfControlSupportsSecurity1ElseOptional.add( name );
            break;
        case "MOenhanced" :
            // Element is mandatory if declared control model supports 'direct-with-enhanced-security' or 'sbo-with-enhanced-security',
            // otherwise optional and value is of no impact
            console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") NOT IMPLEMENTED: DataObject " + name + " declared as \"MOenhanced\" in PresenceCondition" );
            if( mandatoryIfControlSupportsSecurity2ElseOptional == null ) mandatoryIfControlSupportsSecurity2ElseOptional = new HashSet<>();
            mandatoryIfControlSupportsSecurity2ElseOptional.add( name );
            break;
        case "MONamPlt" :
            // Element is mandatory if the name space of its logical node deviates from the name space of the containing
            // logical device, otherwise optional. See IEC 61850-7-1 for use of name space
            // TODO: same as "MOlnNs" ?
            if( mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional2 == null ) mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional2 = new HashSet<>();
            mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional2.add( name );
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
            console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") NOT IMPLEMENTED: DataObject " + name + " declared as \"MORange\" in PresenceCondition" );
            if( mandatoryIfMeasuredValueExposesRange == null ) mandatoryIfMeasuredValueExposesRange = new HashSet<>();
            mandatoryIfMeasuredValueExposesRange.add( name );
            break;
        case "OMSynPh" :
            // This attribute is optional if value of 'phsRef'' is Synchrophasor otherwise Mandatory]]></Doc>
            console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") NOT IMPLEMENTED: DataObject " + name + " declared as \"OMSynPh\" in PresenceCondition" );
            if( optionalIfPhsRefIsSynchrophasorElseMandatory == null ) optionalIfPhsRefIsSynchrophasorElseMandatory = new HashSet<>();
            optionalIfPhsRefIsSynchrophasorElseMandatory.add( name );
            break;
        default:
            console.warning( "[NSD setup] (" + filename + ":" + lineNumber + ") the PresenceCondition " + presCond + " of AnyLNClass " + name + " is unknown" );
            break;
        }
        
    }
    
    private void checkSpecification() {
        // TODO: do we have to check the presence of the sibling in inherited AbstractLNClass ?
        if( mandatoryIfSiblingPresentElseForbidden != null ) {
            for( Entry< String, String > e : mandatoryIfSiblingPresentElseForbidden.entrySet() ) {
                if( ! presentDO.containsKey( e.getValue() )) {
                    console.warning( "[NSD setup] the sibling of " + e.getKey() + " in PresenceCondition of DataObject " + e.getKey() + " is unknown" );
                }
            }
        }
        if( mandatoryIfSiblingPresentElseOptional != null ) {
            for( Entry< String, String > e : mandatoryIfSiblingPresentElseOptional.entrySet() ) {
                if( ! presentDO.containsKey( e.getValue() )) {
                    console.warning( "[NSD setup] the sibling of " + e.getKey() + " in PresenceCondition of DataObject " + e.getKey() + " is unknown" );
                }
            }
        }
        if( optionalIfSiblingPresentElseMandatory != null ) {
            for( Entry< String, String > e : optionalIfSiblingPresentElseMandatory.entrySet() ) {
                if( ! presentDO.containsKey( e.getValue() )) {
                    console.warning( "[NSD setup] the sibling of " + e.getKey() + " in PresenceCondition of DataObject " + e.getKey() + " is unknown" );
                }
            }
        }
        if( forbiddenIfSiblingPresentElseMandatory != null ) {
            for( Entry< String, String > e : forbiddenIfSiblingPresentElseMandatory.entrySet() ) {
                if( ! presentDO.containsKey( e.getValue() )) {
                    console.warning( "[NSD setup] the sibling of " + e.getKey() + " in PresenceCondition of DataObject " + e.getKey() + " is unknown" );
                }
            }
        }
        if( oneOrMoreIfSiblingPresentElseForbidden != null ) {
            for( Entry< String, String > e : oneOrMoreIfSiblingPresentElseForbidden.entrySet() ) {
                if( ! presentDO.containsKey( e.getValue() )) {
                    console.warning( "[NSD setup] the sibling of " + e.getKey() + " in PresenceCondition of DataObject " + e.getKey() + " is unknown" );
                }
            }
        }
        if( optionalIfSiblingPresentElseForbidden != null ) {
            for( Entry< String, String > e : optionalIfSiblingPresentElseForbidden.entrySet() ) {
                if( ! presentDO.containsKey( e.getValue() )) {
                    console.warning( "[NSD setup] the sibling of " + e.getKey() + " in PresenceCondition of DataObject " + e.getKey() + " is unknown" );
                }
            }
        }
    }

    public boolean addDO( DO do_, DiagnosticChain diagnostics ) {
        return addDO( do_, anyLNClass.getName(), diagnostics );
    }
    
    private boolean addDO( DO do_, String anyLNClassName, DiagnosticChain diagnostics ) {
        // An instance number may be set as a suffix
        // but a number at the end of the name is not always an instance number !
        // Therefore, we first look for with the full name, then with the name without the suffix
        
        String[] names = new String[] { do_.getName() };
        if( ! presentDO.containsKey( names[0] )) {
            if( do_.getName().matches( "[a-zA-Z]+\\d+" )) {
                names = do_.getName().split( "(?=\\d)", 2 );
                if( names.length != 2 ) {
                    console.error( "[NSD validation] Unexpected DO name " + do_.getName() + " in LNodeType (line " + do_.getParentLNodeType().getLineNumber() );
                    return false;
                }
            }
        }
        if( ! presentDO.containsKey( names[0] )) {
            if( base != null ) {
                return base.addDO( do_, anyLNClassName, diagnostics );
            }
            // A specific DO may be added if its DOType contains a DA with name "dataNs"
            if( do_.getRefersToDOType() != null ) {
                Optional< DA > da =
                        do_
                        .getRefersToDOType()
                        .getDA()
                        .stream()
                        .filter( d -> "dataNs".equals( d.getName() ))
                        .findAny();
                if( da.isPresent() ) {
                    if( da.get().getVal().size() > 0 ) {
                        String value = "";
                        for( Val v : da.get().getVal() ) {
                            value += " " + v.getValue();
                        }
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.INFO,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                "[NSD validation] DO " + do_.getName() + " in LNodeType (line " + do_.getParentLNodeType().getLineNumber()
                                        + " is specific because it has DA \"dataNs\" with value [" + value + " ]",
                                new Object[] { do_ } ));
                        return true;
                    }
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] DO " + do_.getName() + " in LNodeType (line " + do_.getParentLNodeType().getLineNumber()
                                    + " is specific because it has DA \"dataNs\" but value is missing",
                            new Object[] { do_ } ));
                    return false;
                }
            }
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    "[NSD validation] DO " + do_.getName() + " in LNodeType (line " + do_.getParentLNodeType().getLineNumber() + ") not found in LNClass "
                            + anyLNClassName + " and DA \"dataNs\" not found",
                    new Object[] { do_ } ));
            return false;
        }

        if( names.length == 1 ) {
            if( presentDO.get( do_.getName() ) != null ) {
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD validation] DO " + do_.getName() + " in LNodeType (line " + do_.getParentLNodeType().getLineNumber() + ") already present in LNClass " + anyLNClassName,
                        new Object[] { do_ } ));
                return false;
            }
            presentDO.put( do_.getName(), new SingleDO( do_ ));
            return true;
        }
        if( names.length == 2 ) {
            if( presentDO.get( names[0] ) == null ) {
                presentDO.put( names[0], new MultiDO() );
            }
            else if( presentDO.get( names[0] ) instanceof SingleDO ) {
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD validation] DO " + do_.getName() + " in LNodeType (line " + do_.getParentLNodeType().getLineNumber() + ") already present without instance number in LNClass " + anyLNClassName,
                        new Object[] { do_ } ));
                return false;
            }

            MultiDO m = ( MultiDO ) presentDO.get( names[0] );
            Integer number = Integer.valueOf( names[1] );
                
            if( m.numberedDOs.containsKey( number )) {
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD validation] DO " + do_.getName() + " in LNodeType (line " + do_.getParentLNodeType().getLineNumber() + ") already present with same instance number in LNClass " + anyLNClassName,
                        new Object[] { do_ } ));
                return false;
            }
            m.add( number, do_ );
            return true;
        }
        console.warning( "[NSD validation] DO " + do_.getName() + " in LNodeType (line " + do_.getParentLNodeType().getLineNumber() + "has an unrecognized name" );
        return false;
    }
    
    public boolean validate( LNodeType lNodeType, DiagnosticChain diagnostics ) {
        AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] DataObjectPresenceConditionValidator.validate( " + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
        return validate( lNodeType, anyLNClass.getName(), false, diagnostics );
    }
    
    private boolean validate( LNodeType lNodeType, String anyLNClassName, boolean asSuperclass, DiagnosticChain diagnostics ) {
        boolean res = true;
        
        // Some presence conditions must only be checked by the final LNClass, not by any superLNClass.
        // For example, for atLeastOne, the group contains all the DataObject of the full hierarchy,
        // so only the final LNClass can do the check.
        // The argument asSuperclass is used for that.
        if( base != null ) {
            res = base.validate( lNodeType, anyLNClassName, true, diagnostics );
        }
        
        // presCond: "M"
        // Element is mandatory
        // Usage in standard NSD files (version 2007B): DataObject and DataAttribute and SubDataAttribute
        if( mandatory != null ) {
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"M\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            for( String name : this.mandatory ) {
                if( presentDO.get( name ) == null ) {
                  diagnostics.add( new BasicDiagnostic(
                          Diagnostic.ERROR,
                          RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                          0,
                          "[NSD validation] DO " + name + " is mandatory in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                          new Object[] { lNodeType } ));
                  res = false;
                }
                else if( presentDO.get( name ) instanceof MultiDO ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] DO " + name + " should not have an instance number in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
                    res = false;
                  }
            }
        }

        // presCond: "O"
        // Element is optional
        // Usage in standard NSD files (version 2007B): DataObject and DataAttribute and SubDataAttribute
        if( optional != null ) {
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"O\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            for( String name : this.optional ) {
                if( presentDO.get( name ) == null ) {
                }
                else if( presentDO.get( name ) instanceof MultiDO ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] DO " + name + " should not have an instance number in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
                    res = false;
                }
            }
        }

        // presCond: "F"
        // Element is forbidden
        // Usage in standard NSD files (version 2007B): DataObject
        if( forbidden != null ) {
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"F\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            for( String name : this.forbidden ) {
                if( presentDO.get( name ) != null ) {
                  diagnostics.add( new BasicDiagnostic(
                          Diagnostic.ERROR,
                          RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                          0,
                          "[NSD validation] DO " + name + " is forbidden in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                          new Object[] { lNodeType } ));
                  res = false;
                }
                else if( presentDO.get( name ) instanceof MultiDO ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] DO " + name + " should not have an instance number in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
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
                if( presentDO.get( name ) != null ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] verification of PresenceCondition \"na\" for DO " + name + " is not implemented in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
                }
            }
        }
        
        // presCond: "Mmulti"
        // At least one element shall be present; all instances have an instance number > 0
        // Usage in standard NSD files (version 2007B): DataObject
        if( mandatoryMulti != null ) {
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"Mmulti\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            for( String name : this.mandatoryMulti ) {
                if( presentDO.get( name ) == null ) {
                  diagnostics.add( new BasicDiagnostic(
                          Diagnostic.ERROR,
                          RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                          0,
                          "[NSD validation] At least one DO " + name + " is mandatory in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                          new Object[] { lNodeType } ));
                  res = false;
                }
                else if( presentDO.get( name ) instanceof SingleDO ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] DO " + name + " should have an instance number in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
                    res = false;
                }
            }
        }

        // presCond: "Omulti"
        // Zero or more elements may be present; all instances have an instance number > 0
        // Usage in standard NSD files (version 2007B): DataObject
        if( optionalMulti != null ) {
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"Omulti\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            for( String name : this.optionalMulti ) {
                if( presentDO.get( name ) == null ) {
                }
                else if( presentDO.get( name ) instanceof SingleDO ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] DO " + name + " should have an instance number in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
                    res = false;
                }
            }
        }

        // presCond: "AtLeastOne"
        // Parameter n: group number (> 0).
        // At least one of marked elements of a group n shall be present
        // Usage in standard NSD files (version 2007B): DataObject and SubDataObject and DataAttribute and SubDataAttribute
        //if( atLeastOne != null ) {
        if( ! asSuperclass ) {
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"AtLeastOne\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            for( Entry< Integer, HashSet< String > > e1 : atLeastOne.entrySet() ) {
                boolean groupOK = false;
                for( String member : e1.getValue() ) {
                    if( presentDO.get( member ) != null ) {
                        groupOK = true;
                        break;
                    }
                }
                if( ! groupOK ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] group " + e1.getKey() + " has no elements in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
                    res = false;
                }
            }
        }
        
        // presCond: "AtMostOne" :
        // At most one of marked elements shall be present
        // Usage in standard NSD files (version 2007B): DataObject
        //if( atMostOne != null ) {
        if( ! asSuperclass ) {
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"AtMostOne\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            int count = 0;
            for( String s : atMostOne ) {
                if( presentDO.get( s ) != null ) {
                    ++count;
                }
            }
            if( count > 1 ) {
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD validation] LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName + " has more than one element marked AtMostOne",
                        new Object[] { lNodeType } ));
                res = false;
            }
        }
        
        // presCond: "AllOrNonePerGroup" :
        // Parameter n: group number (> 0).
        // All or none of the elements of a group n shall be present
        // Usage in standard NSD files (version 2007B): DataAttribute
        //if( allOrNonePerGroup != null ) {
        if( ! asSuperclass ) {
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"AllOrNonePerGroup\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            for( Entry< Integer, HashSet< String > > e1 : allOrNonePerGroup.entrySet() ) {
                int groupCount = 0;
                for( String member : e1.getValue() ) {
                    if( presentDO.get( member ) != null ) {
                        ++groupCount;
                    }
                }
                if(( groupCount > 0 ) && (groupCount < e1.getValue().size() )) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] group " + e1.getKey() + " has neither none nor all elements in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
                    res = false;
                }
            }
        }
        
        // presCond: "AllOnlyOneGroup" :
        // Parameter n: group number (> 0).
        // All elements of only one group n shall be present
        // Usage in standard NSD files (version 2007B): DataObject and SubDataAttribute
        //if( allOnlyOneGroup != null ) {
        if(( ! asSuperclass ) && ( allOnlyOneGroup.size() != 0 )) {         // groupNumber == 0 not an error if empty
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"AllOnlyOneGroup\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            int groupNumber = 0;
            for( Entry< Integer, HashSet< String > > e1 : allOnlyOneGroup.entrySet() ) {
                int groupCount = 0;
                for( String member : e1.getValue() ) {
                    if( presentDO.get( member ) != null ) {
                        ++groupCount;
                    }
                }
                if(( groupCount > 0 ) && (groupCount < e1.getValue().size() )) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] group " + e1.getKey() + " has neither none nor all elements in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
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
                                "[NSD validation] LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName + " has several groups with all elements",
                                new Object[] { lNodeType } ));
                        res = false;
                    }
                }
            }
            if( groupNumber == 0 ) {
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD validation] no group in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName + " has all elements",
                        new Object[] { lNodeType } ));
                res = false;
            }
        }
        
        // presCond: "AllAtLeastOneGroup" :
        // Parameter n: group number (> 0).
        // All elements of at least one group n shall be present
        // Usage in standard NSD files (version 2007B): DataAttribute
        //if( allAtLeastOneGroup != null ) {
        if(( ! asSuperclass ) && ( allAtLeastOneGroup.size() != 0 )) {         // groupNumber == 0 not an error if empty
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"AllAtLeastOneGroup\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            int groupNumber = 0;
            for( Entry< Integer, HashSet< String > > e1 : allAtLeastOneGroup.entrySet() ) {
                int groupCount = 0;
                for( String member : e1.getValue() ) {
                    if( presentDO.get( member ) != null ) {
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
                        "[NSD validation] no group in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName + " has all elements",
                        new Object[] { lNodeType } ));
                res = false;
            }
        }
        
        // presCond: "MF" :
        // Parameter sibling: sibling element name.
        // Mandatory if sibling element is present, otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataObject
        if( mandatoryIfSiblingPresentElseForbidden != null ) {
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"MF\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            for( Entry< String, String > entry : mandatoryIfSiblingPresentElseForbidden.entrySet() ) {
                if( presentDO.get( entry.getValue() ) != null ) {
                    if( presentDO.get( entry.getKey() ) == null ) {
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                "[NSD validation] DO " + entry.getKey() + " is mandatory in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass "
                                        + anyLNClassName + " because sibling " + entry.getValue() + " is present",
                                new Object[] { lNodeType } ));
                        res = false;
                    }
                }
                else {
                    if( presentDO.get( entry.getKey() ) != null ) {
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                "[NSD validation] DO " + entry.getKey() + " is forbidden in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass "
                                        + anyLNClassName + " because sibling " + entry.getValue() + " is not present",
                                new Object[] { lNodeType } ));
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
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"MO\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            for( Entry< String, String > entry : mandatoryIfSiblingPresentElseOptional.entrySet() ) {
                if( presentDO.get( entry.getValue() ) != null ) {
                    if( presentDO.get( entry.getKey() ) == null ) {
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                "[NSD validation] DO " + entry.getKey() + " is mandatory in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass "
                                        + anyLNClassName + " because sibling " + entry.getValue() + " is present",
                                new Object[] { lNodeType } ));
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
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"OM\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            for( Entry< String, String > entry : optionalIfSiblingPresentElseMandatory.entrySet() ) {
                if( presentDO.get( entry.getValue() ) == null ) {
                    if( presentDO.get( entry.getKey() ) == null ) {
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                "[NSD validation] DO " + entry.getKey() + " is mandatory in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass "
                                        + anyLNClassName + " because sibling " + entry.getValue() + " is not present",
                                new Object[] { lNodeType } ));
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
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"FM\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            for( Entry< String, String > entry : forbiddenIfSiblingPresentElseMandatory.entrySet() ) {
                if( presentDO.get( entry.getValue() ) != null ) {
                    if( presentDO.get( entry.getKey() ) != null ) {
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                "[NSD validation] DO " + entry.getKey() + " is forbidden in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass "
                                        + anyLNClassName + " because sibling " + entry.getValue() + " is present",
                                new Object[] { lNodeType } ));
                        res = false;
                    }
                }
                else {
                    if( presentDO.get( entry.getKey() ) == null ) {
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                "[NSD validation] DO " + entry.getKey() + " is mandatory in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass "
                                        + anyLNClassName + " because sibling " + entry.getValue() + " is not present",
                                new Object[] { lNodeType } ));
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
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"MOcond\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            for( Entry< String, String > entry : mandatoryIfTextConditionElseOptional.entrySet() ) {
                String doc = anyLNClass
                        .getDataObject()
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
                        "[NSD validation] DO " + entry.getKey() + " is mandatory in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass "
                                + anyLNClassName + " if textual condition number " + entry.getValue() + " (not evaluated) is true, else optional. It is "
                                + ( presentDO.get( entry.getKey() ) == null ? "absent." : "present." ) + ( doc != null ? " Textual condition is: \"" + doc + "\"." : "" ),
                        new Object[] { lNodeType } ));
            }
        }
        
        // presCond: "MFcond" :
        // Parameter condID: condition number (> 0).
        // Textual presence condition (non-machine processable) with reference condID to context specific text.
        // If satisfied, the element is mandatory, otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataObject
        if( mandatoryIfTextConditionElseForbidden != null ) {
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"MFcond\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            for( Entry< String, String > entry : mandatoryIfTextConditionElseForbidden.entrySet() ) {
                String doc = anyLNClass
                        .getDataObject()
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
                        "[NSD validation] DO " + entry.getKey() + " is mandatory in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass "
                                + anyLNClassName + " if textual condition number " + entry.getValue() + " (not evaluated) is true, else forbidden. It is " 
                                + ( presentDO.get( entry.getKey() ) == null ? "absent." : "present." ) + ( doc != null ? " Textual condition is: \"" + doc + "\"." : "" ),
                        new Object[] { lNodeType } ));
            }
        }
        
        // presCond: "OFcond" :
        // Parameter condID: condition number (> 0).
        // Textual presence condition (non-machine processable) with reference condID to context specific text.
        // If satisfied, the element is optional, otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataObject
        if( optionalIfTextConditionElseForbidden != null ) {
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"OFcond\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            for( Entry< String, String > entry : optionalIfTextConditionElseForbidden.entrySet() ) {
                String doc = anyLNClass
                        .getDataObject()
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
                        "[NSD validation] DO " + entry.getKey() + " is optional in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass "
                                + anyLNClassName + " if textual condition number " + entry.getValue() + " (not evaluated) is true, else forbidden. It is " 
                                + ( presentDO.get( entry.getKey() ) == null ? "absent." : "present." ) + ( doc != null ? " Textual condition is: \"" + doc + "\"." : "" ),
                        new Object[] { lNodeType } ));
            }
        }
        
        // presCond: "MmultiRange" :
        // Parameters min, max: limits for instance number (> 0).
        // One or more elements shall be present; all instances have an instance number within range [min, max] (see IEC 61850-7-1)
        // Usage in standard NSD files (version 2007B): None
        if( mandatoryMultiRange != null ) {
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"MmultiRange\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            for( String name : this.mandatoryMultiRange.keySet() ) {
                if( presentDO.get( name ) == null ) {
                  diagnostics.add( new BasicDiagnostic(
                          Diagnostic.ERROR,
                          RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                          0,
                          "[NSD validation] At least one DO " + name + " is mandatory in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                          new Object[] { lNodeType } ));
                  res = false;
                }
                else if( presentDO.get( name ) instanceof SingleDO ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] DO " + name + " should have an instance number in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
                    res = false;
                }
                else {
                    MultiDO m = ( MultiDO ) presentDO.get( name );
                    for( Integer n : m.numberedDOs.keySet() ) {
                        Integer min = mandatoryMultiRange.get( name ).getLeft();
                        Integer max = mandatoryMultiRange.get( name ).getRight();
                        if(( n < min ) || ( n > max )) {
                            diagnostics.add( new BasicDiagnostic(
                                    Diagnostic.ERROR,
                                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                    0,
                                    "[NSD validation] DO " + name + " should have an instance number in range [" + min + "," + max + "] in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                                    new Object[] { lNodeType } ));
                            res = false;
                        }
                    }
                }
            }
        }

        // presCond: "OmultiRange" :
        // Parameters min, max: limits for instance number (> 0).
        // Zero or more elements may be present; all instances have an instance number within range [min, max] (see IEC 61850-7-1)
        // Usage in standard NSD files (version 2007B): DataObject
        if( optionalMultiRange != null ) {
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"OmultiRange\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            for( String name : this.optionalMultiRange.keySet() ) {
                if( presentDO.get( name ) == null ) {
                }
                else if( presentDO.get( name ) instanceof SingleDO ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] DO " + name + " should have an instance number in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
                    res = false;
                }
                else {
                    MultiDO m = ( MultiDO ) presentDO.get( name );
                    for( Integer n : m.numberedDOs.keySet() ) {
                        Integer min = optionalMultiRange.get( name ).getLeft();
                        Integer max = optionalMultiRange.get( name ).getRight();
                        if(( n < min ) || ( n > max )) {
                            diagnostics.add( new BasicDiagnostic(
                                    Diagnostic.ERROR,
                                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                    0,
                                    "[NSD validation] DO " + name + " should have an instance number in range [" + min + "," + max + "] in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                                    new Object[] { lNodeType } ));
                            res = false;
                        }
                    }
                }
            }
        }

        // presCond: "MFsubst" :
        // Element is mandatory if substitution is supported (for substitution, see IEC 61850-7-3), otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO
        if( mandatoryIfSubstitutionElseForbidden != null ) {
            for( String name : mandatoryIfSubstitutionElseForbidden ) {
                if( presentDO.get( name ) != null ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] verification of PresenceCondition \"MFsubst\" for DO " + name + " is not implemented in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
                }
            }
        }
        
        // presCond: "MOln0" :
        // Element is mandatory in the context of LLN0; otherwise optional
        // Usage in standard NSD files (version 2007B): DataAttribute
        if( mandatoryInLLN0ElseOptional != null ) {
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"MOln0\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            for( String name : mandatoryInLLN0ElseOptional ) {
                if( presentDO.get( name ) == null ) {
                    for( AnyLN anyLN : lNodeType.getReferredByAnyLN() ) {
                        if( anyLN instanceof LN0 ) {
                            diagnostics.add( new BasicDiagnostic(
                                    Diagnostic.ERROR,
                                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                    0,
                                    "[NSD validation] DO " + name + " is mandatory in LN0 in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                                    new Object[] { lNodeType } ));
                            res = false;
                        }
                    }
                }
            }
        }
        
        // presCond: "MFln0" :
        // Element is mandatory in the context of LLN0; otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataAttribute
        if( mandatoryInLLN0ElseForbidden != null ) {
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"MFln0\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            for( String name : mandatoryInLLN0ElseForbidden ) {
                for( AnyLN anyLN : lNodeType.getReferredByAnyLN() ) {
                    if( presentDO.get( name ) == null ) {
                        if( anyLN instanceof LN0 ) {
                            diagnostics.add( new BasicDiagnostic(
                                    Diagnostic.ERROR,
                                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                    0,
                                    "[NSD validation] DO " + name + " is mandatory in LN0 in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                                    new Object[] { lNodeType } ));
                            res = false;
                        }
                    }
                    else {
                        if( ! ( anyLN instanceof LN0 )) {
                            diagnostics.add( new BasicDiagnostic(
                                    Diagnostic.ERROR,
                                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                    0,
                                    "[NSD validation] DO " + name + " is forbidden in LN in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                                    new Object[] { lNodeType } ));
                            res = false;
                        }
                    }
                }
            }
        }

        // presCond: "MOlnNs" :
        // Element is mandatory if the name space of its logical node deviates from the name space of the containing
        // logical device, otherwise optional. See IEC 61850-7-1 for use of name space
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO: The meaning is not clear.
        if( mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional != null ) {
            for( String name : mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional ) {
                if( presentDO.get( name ) != null ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] verification of PresenceCondition \"MOlnNs\" for DO " + name + " is not implemented in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
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
                if( presentDO.get( name ) != null ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] verification of PresenceCondition \"MOdataNs\" for DO " + name + " is not implemented in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
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
            for( String name : mandatoryIfAnalogValueIncludesIElseForbidden ) {
                if( presentDO.get( name ) != null ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] verification of PresenceCondition \"MFscaledAV\" for DO " + name + " is not implemented in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
                }
            }
        }

        // presCond: "MFscaledMagV" :
        // Element is mandatory* if any sibling elements of type Vector include 'i' as a child of their 'mag' attribute, otherwise forbidden.
        // *See MFscaledAV
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO
        if( mandatoryIfVectorSiblingIncludesIAsChildMagElseForbidden != null ) {
            for( String name : mandatoryIfVectorSiblingIncludesIAsChildMagElseForbidden ) {
                if( presentDO.get( name ) != null ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] verification of PresenceCondition \"MFscaledMagV\" for DO " + name + " is not implemented in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
                }
            }
        }

        // presCond: "MFscaledAngV" :
        // Element is mandatory* if any sibling elements of type Vector include 'i' as a child of their 'ang' attribute, otherwise forbidden.
        // *See MFscaledAV
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO
        if( mandatoryIfVectorSiblingIncludesIAsChildAngElseForbidden != null ) {
            for( String name : mandatoryIfVectorSiblingIncludesIAsChildAngElseForbidden ) {
                if( presentDO.get( name ) != null ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] verification of PresenceCondition \"MFscaledAngV\" for DO " + name + " is not implemented in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
                }
            }
        }

        // presCond: "MOrms" :
        // Element is mandatory if the harmonic values in the context are calculated as a ratio to RMS value
        // (value of data attribute 'hvRef' is 'rms'), optional otherwise
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO
        if( mandatoryIfHarmonicValuesCalculatedAsRatioElseOptional != null ) {
            for( String name : mandatoryIfHarmonicValuesCalculatedAsRatioElseOptional ) {
                if( presentDO.get( name ) != null ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] verification of PresenceCondition \"MOrms\" for DO " + name + " is not implemented in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
                }
            }
        }

        // presCond: "MOrootLD" :
        // Element is mandatory in the context of a root logical device; otherwise it is optional
        // Usage in standard NSD files (version 2007B): DataObject
        if( mandatoryInRootLogicalDeviceElseOptional != null ) {
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"MOrootLD\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            for( String name : mandatoryInRootLogicalDeviceElseOptional ) {
                for( AnyLN anyLN : lNodeType.getReferredByAnyLN() ) {
                    Optional< DOI > grRef =
                            anyLN
                            .getParentLDevice()
                            .getLN0()
                            .getDOI()
                            .stream()
                            .filter( doi -> "GrRef".equals( doi.getName() ))
                            .findFirst();
                    if( ! grRef.isPresent() ) {
                        if( presentDO.get( name ) == null ) {
                            diagnostics.add( new BasicDiagnostic(
                                    Diagnostic.ERROR,
                                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                    0,
                                    "[NSD validation] DO " + name + " is mandatory in LN in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass "
                                            + anyLNClassName + " in the context of a root logical device",
                                    new Object[] { lNodeType } ));
                            res = false;
                        }
                    }
                }
            }
        }

        // presCond: "MOoperTm" :
        // Element is mandatory if at least one controlled object on the IED supports time activation service; otherwise it is optional
        // Usage in standard NSD files (version 2007B): DataAttribute
        // TODO
        if( mandatoryIfControlSupportsTimeElseOptional != null ) {
            for( String name : mandatoryIfControlSupportsTimeElseOptional ) {
                if( presentDO.get( name ) != null ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] verification of PresenceCondition \"MOoperTm\" for DO " + name + " is not implemented in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
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
                if( presentDO.get( name ) != null ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] verification of PresenceCondition \"MmultiF\" for DO " + name + " is not implemented in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
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
                if( presentDO.get( name ) != null ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] verification of PresenceCondition \"MOsbo\" for DO " + name + " is not implemented in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
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
                if( presentDO.get( name ) != null ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] verification of PresenceCondition \"MOenhanced\" for DO " + name + " is not implemented in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
                }
            }
        }

        // presCond: "MONamPlt" :
        // Element is mandatory if the name space of its logical node deviates from the name space of the containing
        // logical device, otherwise optional. See IEC 61850-7-1 for use of name space
        // Usage in standard NSD files (version 2007B): DataObject
        // TODO: same as "MOlnNs" ?
        if( mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional2 != null ) {
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"MONamPlt\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            
            for( AnyLN ln : lNodeType.getReferredByAnyLN() ) {
                String lnNs = ln.getNamespace();
                if(( lnNs == null ) || lnNs.isEmpty() ) {
                    continue;
                }
                if( ln.getParentLDevice() == null ) {
                    continue;
                }
                String ldNs = ln.getParentLDevice().getNamespace();
                if(( ldNs == null ) || ldNs.isEmpty() ) {
                    continue;
                }
                if( ! lnNs.equals( ldNs )) {
                    for( String name : mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional2 ) {
                        if( presentDO.get( name ) == null ) {
                            diagnostics.add( new BasicDiagnostic(
                                    Diagnostic.ERROR,
                                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                    0,
                                    "[NSD validation] DO " + name + " is mandatory in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName
                                    + " because the name space of its logical node (\"" + lnNs + "\") deviates from the name space of the containing logical device (\""
                                    + ldNs + "\")",
                                    new Object[] { lNodeType } ));
                        }
                    }
                }
            }

        }

        // presCond: "OF" :
        // Parameter sibling: sibling element name.
        // Optional if sibling element is present, otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataObject and DataAttribute
        if( optionalIfSiblingPresentElseForbidden != null ) {
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validation of presence condition \"OF\" on LNodeType (id=" + lNodeType.getId() + ") at line " + lNodeType.getLineNumber() );
            for( Entry< String, String > entry : optionalIfSiblingPresentElseForbidden.entrySet() ) {
                if( presentDO.get( entry.getValue() ) == null ) {
                    if( presentDO.get( entry.getKey() ) != null ) {
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                "[NSD validation] DO " + entry.getKey() + " is forbidden in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass "
                                        + anyLNClassName + " because sibling " + entry.getValue() + " is not present",
                                new Object[] { lNodeType } ));
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
                if( presentDO.get( name ) != null ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] verification of PresenceCondition \"MORange\" for DO " + name + " is not implemented in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
                }
            }
        }

        // presCond: "OMSynPh" :
        // This attribute is optional if value of 'phsRef'' is Synchrophasor otherwise Mandatory]]></Doc>
        // Usage in standard NSD files (version 2007B): SubDataObject
        // TODO
        if( optionalIfPhsRefIsSynchrophasorElseMandatory != null ) {
            for( String name : optionalIfPhsRefIsSynchrophasorElseMandatory ) {
                if( presentDO.get( name ) != null ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] verification of PresenceCondition \"OMSynPh\" for DO " + name + " is not implemented in LNodeType (line " + lNodeType.getLineNumber() + ") with LNClass " + anyLNClassName,
                            new Object[] { lNodeType } ));
                }
            }
        }

        return res;
    }

}
