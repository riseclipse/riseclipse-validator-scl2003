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
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.jdt.annotation.NonNull;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.AnyLNClass;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentification;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentificationName;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AnyLN;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DO;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DOI;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.LN0;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.LNodeType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.RiseClipseMessage;

public class DataObjectPresenceConditionValidator {
    
    private static final String DO_SETUP_NSD_CATEGORY      = NsdValidator.SETUP_NSD_CATEGORY      + "/DataObject";
    private static final String DO_VALIDATION_NSD_CATEGORY = NsdValidator.VALIDATION_NSD_CATEGORY + "/DataObject";

    private static IdentityHashMap< NsIdentificationName, DataObjectPresenceConditionValidator > notStatisticalValidators = new IdentityHashMap<>();
    private static IdentityHashMap< NsIdentificationName, DataObjectPresenceConditionValidator > statisticalValidators = new IdentityHashMap<>();
    
    public static DataObjectPresenceConditionValidator get( NsIdentification nsIdentification, AnyLNClass anyLNClass, boolean isStatistic ) {
        var validators = isStatistic ? statisticalValidators : notStatisticalValidators;
        // TODO: do we need to use dependsOn links?
        if( ! validators.containsKey( NsIdentificationName.of( nsIdentification, anyLNClass.getName() ))) {
            validators.put( NsIdentificationName.of( nsIdentification, anyLNClass.getName() ), new DataObjectPresenceConditionValidator( nsIdentification, anyLNClass, isStatistic ));
        }
        return validators.get( NsIdentificationName.of( nsIdentification, anyLNClass.getName() ));
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
    private HashSet< String > mAllOrNonePerGroup;
    private HashSet< String > mOctrl;
    private HashSet< String > mOsboNormal;
    private HashSet< String > mOsboEnhanced;
    
    private final IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
    private NsIdentification nsIdentification;
    private boolean isStatistic;
    
    @SuppressWarnings( "unchecked" )        // cast of HashMap.clone() result
    private DataObjectPresenceConditionValidator( NsIdentification nsIdentification, AnyLNClass anyLNClass, boolean isStatistic ) {
        console.debug( DO_SETUP_NSD_CATEGORY, anyLNClass.getFilename(), anyLNClass.getLineNumber(),
                "DataObjectPresenceConditionValidator( \"", anyLNClass.getName(), "\" in namespace \"", nsIdentification, "\" )");
        
        this.nsIdentification = nsIdentification;
        this.anyLNClass = anyLNClass;
        this.isStatistic = isStatistic;
        
        // Build validator for parent first, because it is needed (atLeastOne for example)
        AnyLNClass parent = anyLNClass.getRefersToAbstractLNClass();
        if( parent != null ) {
            base = get( nsIdentification, parent, isStatistic );
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
        .forEach( d -> {
            if( isStatistic ) {
                addSpecification( d.getName(), d.getDsPresCond(), d.getDsPresCondArgs(), d.getLineNumber(), d.getFilename() );
            }
            else {
                addSpecification( d.getName(), d.getPresCond(), d.getPresCondArgs(), d.getLineNumber(), d.getFilename() );
            }
        } );
        
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
            console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
                             name, " has already been added to DataObjectPresenceConditionValidator" );
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
            // TODO: what does it mean ? what do we have to check ?
            // DONE: if used by statistics presence condition, no need to display message
            if( ! isStatistic ) {
                console.warning( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: DataObject \"", name, "\" declared as \"na\" in non statistic PresenceCondition" );
            }
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
                    console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
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
                console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
                                 "argument of PresenceCondition \"AtLeastOne\" is not an integer" );
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
                    console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
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
                console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
                                 "argument of PresenceCondition \"AllOrNonePerGroup\" is not an integer" );
                break;
            }
        case "AllOnlyOneGroup" :
            // Parameter n: group number (> 0).
            // All elements of only one group n shall be present
            //if( allOnlyOneGroup == null ) allOnlyOneGroup = new HashMap<>();
            try {
                Integer arg = Integer.valueOf( presCondArgs );
                if( arg <= 0 ) {
                    console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
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
                console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
                        "argument of PresenceCondition \"AllOnlyOneGroup\" is not an integer" );
                break;
            }
        case "AllAtLeastOneGroup" :
            // Parameter n: group number (> 0).
            // All elements of at least one group n shall be present
            //if( allAtLeastOneGroup == null ) allAtLeastOneGroup = new HashMap<>();
            try {
                Integer arg = Integer.valueOf( presCondArgs );
                if( arg <= 0 ) {
                    console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
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
                console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
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
            try {
                Integer arg = Integer.valueOf( presCondArgs );
                if( arg <= 0 ) {
                    console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
                            "argument of PresenceCondition \"MOcond\" is not a positive integer" );
                    break;
                }
                mandatoryIfTextConditionElseOptional.put( name, presCondArgs );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
                                 "argument of PresenceCondition \"MOcond\" is not an integer" );
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
                    console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
                            "argument of PresenceCondition \"MFcond\" is not a positive integer" );
                    break;
                }
                mandatoryIfTextConditionElseForbidden.put( name, presCondArgs );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
                                 "argument of PresenceCondition \"MFcond\" is not an integer" );
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
                    console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
                            "argument of PresenceCondition \"OFcond\" is not a positive integer" );
                    break;
                }
                optionalIfTextConditionElseForbidden.put( name, presCondArgs );
                break;
            }
            catch( NumberFormatException e ) {
                console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
                                 "argument of PresenceCondition \"OFcond\" is not an integer" );
                break;
            }
        case "MmultiRange" :
            // Parameters min, max: limits for instance number (> 0).
            // One or more elements shall be present; all instances have an instance number within range [min, max] (see IEC 61850-7-1)
            // -> TODO: not sure what is the instance number, it is assumed to be the suffix of DO name
            if( mandatoryMultiRange == null ) mandatoryMultiRange = new HashMap<>();
            String[] limits1 = presCondArgs.split( "[ ,]+" );
            if( limits1.length != 2 ) {
                console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
                                 "argument of PresenceCondition \"MmultiRange\" is not two integers" );
                break;
            }
            Integer min1 = Integer.valueOf( limits1[0] );
            if( min1 <= 0 ) {
                console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
                        "first argument of PresenceCondition \"MmultiRange\" is not a positive integer" );
                break;
            }
            Integer max1 = Integer.valueOf( limits1[1] );
            if( max1 <= 0 ) {
                console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
                        "second argument of PresenceCondition \"MmultiRange\" is not a positive integer" );
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
                console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
                        "argument of PresenceCondition \"OmultiRange\" is not two integers" );
                break;
            }
            Integer min2 = Integer.valueOf( limits2[0] );
            if( min2 <= 0 ) {
                console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
                        "first argument of PresenceCondition \"OmultiRange\" is not a positive integer" );
                break;
            }
            Integer max2 = Integer.valueOf( limits2[1] );
            if( max2 <= 0 ) {
                console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
                        "second argument of PresenceCondition \"OmultiRange\" is not a positive integer" );
                break;
            }
            optionalMultiRange.put( name, Pair.of( min2, max2 ));
            break;
        case "MFsubst" :
            // Element is mandatory if substitution is supported (for substitution, see IEC 61850-7-3), otherwise forbidden
            // TODO: how do we know if substitution is supported ?
            console.warning( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: DataObject \"", name, "\" declared as \"MFsubst\" in PresenceCondition" );
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
            console.warning( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: DataObject \"", name, "\" declared as \"MOlnNs\" in PresenceCondition" );
            if( mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional == null ) mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional = new HashSet<>();
            mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional.add( name );
            break;
        case "MOdataNs" :
            // Element is mandatory if the name space of its data object deviates from the name space of its logical node,
            // otherwise optional. See IEC 61850-7-1 for use of name space
            console.warning( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: DataObject \"", name, "\" declared as \"MOdataNs\" in PresenceCondition" );
            if( mandatoryIfNameSpaceOfDataObjectDeviatesElseOptional == null ) mandatoryIfNameSpaceOfDataObjectDeviatesElseOptional = new HashSet<>();
            mandatoryIfNameSpaceOfDataObjectDeviatesElseOptional.add( name );
            break;
        case "MFscaledAV" :
            // Element is mandatory* if any sibling elements of type AnalogueValue include 'i' as a child, otherwise forbidden.
            // *Even though devices without floating point capability cannot exchange floating point values through ACSI services,
            // the description of scaling remains mandatory for their (SCL) configuration
            console.warning( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: DataObject \"", name, "\" declared as \"MFscaledAV\" in PresenceCondition" );
            if( mandatoryIfAnalogValueIncludesIElseForbidden == null ) mandatoryIfAnalogValueIncludesIElseForbidden = new HashSet<>();
            mandatoryIfAnalogValueIncludesIElseForbidden.add( name );
            break;
        case "MFscaledMagV" :
            // Element is mandatory* if any sibling elements of type Vector include 'i' as a child of their 'mag' attribute, otherwise forbidden.
            // *See MFscaledAV
            console.warning( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: DataObject \"", name, "\" declared as \"MFscaledMagV\" in PresenceCondition" );
            if( mandatoryIfVectorSiblingIncludesIAsChildMagElseForbidden == null ) mandatoryIfVectorSiblingIncludesIAsChildMagElseForbidden = new HashSet<>();
            mandatoryIfVectorSiblingIncludesIAsChildMagElseForbidden.add( name );
            break;
        case "MFscaledAngV" :
            // Element is mandatory* if any sibling elements of type Vector include 'i' as a child of their 'ang' attribute, otherwise forbidden.
            // *See MFscaledAV
            console.warning( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: DataObject \"", name, "\" declared as \"MFscaledAngV\" in PresenceCondition" );
            if( mandatoryIfVectorSiblingIncludesIAsChildAngElseForbidden == null ) mandatoryIfVectorSiblingIncludesIAsChildAngElseForbidden = new HashSet<>();
            mandatoryIfVectorSiblingIncludesIAsChildAngElseForbidden.add( name );
            break;
        case "MOrms" :
            // Element is mandatory if the harmonic values in the context are calculated as a ratio to RMS value
            // (value of data attribute 'hvRef' is 'rms'), optional otherwise
            console.warning( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: DataObject \"", name, "\" declared as \"MOrms\" in PresenceCondition" );
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
            console.warning( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: DataObject \"", name, "\" declared as \"MOoperTm\" in PresenceCondition" );
            if( mandatoryIfControlSupportsTimeElseOptional == null ) mandatoryIfControlSupportsTimeElseOptional = new HashSet<>();
            mandatoryIfControlSupportsTimeElseOptional.add( name );
            break;
        case "MmultiF" :
            // Parameter sibling: sibling element name.
            // One or more elements must be present if sibling element is present, otherwise forbidden
            console.warning( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: DataObject \"", name, "\" declared as \"MmultiF\" in PresenceCondition" );
            if( oneOrMoreIfSiblingPresentElseForbidden == null ) oneOrMoreIfSiblingPresentElseForbidden = new HashMap<>();
            oneOrMoreIfSiblingPresentElseForbidden.put( name, presCondArgs );
            break;
        case "MOsbo" :
            // Element is mandatory if declared control model supports 'sbo-with-normal-security' or 'sbo-with-enhanced-security',
            // otherwise optional and value is of no impact
            console.warning( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: DataObject \"", name, "\" declared as \"MOsbo\" in PresenceCondition" );
            if( mandatoryIfControlSupportsSecurity1ElseOptional == null ) mandatoryIfControlSupportsSecurity1ElseOptional = new HashSet<>();
            mandatoryIfControlSupportsSecurity1ElseOptional.add( name );
            break;
        case "MOenhanced" :
            // Element is mandatory if declared control model supports 'direct-with-enhanced-security' or 'sbo-with-enhanced-security',
            // otherwise optional and value is of no impact
            console.warning( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: DataObject \"", name, "\" declared as \"MOenhanced\" in PresenceCondition" );
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
            console.warning( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: DataObject \"", name, "\" declared as \"MORange\" in PresenceCondition" );
            if( mandatoryIfMeasuredValueExposesRange == null ) mandatoryIfMeasuredValueExposesRange = new HashSet<>();
            mandatoryIfMeasuredValueExposesRange.add( name );
            break;
        case "OMSynPh" :
            // This attribute is optional if value of 'phsRef'' is Synchrophasor otherwise Mandatory]]></Doc>
            console.warning( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: DataObject \"", name, "\" declared as \"OMSynPh\" in PresenceCondition" );
            if( optionalIfPhsRefIsSynchrophasorElseMandatory == null ) optionalIfPhsRefIsSynchrophasorElseMandatory = new HashSet<>();
            optionalIfPhsRefIsSynchrophasorElseMandatory.add( name );
            break;
        case "MAllOrNonePerGroup" :
            // Parameter n: group number (> 0).
            // Element is mandatory if declared control model supports 'direct-with-enhanced- security'
            // or 'sbo-with-enhanced-security', otherwise all or none of the elements of a group n shall be present.
            console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: DataObject \"", name, "\" declared as \"MAllOrNonePerGroup\" in PresenceCondition" );
            if( mAllOrNonePerGroup == null ) mAllOrNonePerGroup = new HashSet<>();
            mAllOrNonePerGroup.add( name );
            break;
        case "MOctrl" :
            // Seen in IEC_61850-8-1_2003A2.snsd
            console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: DataObject \"", name, "\" declared as \"MOctrl\" in PresenceCondition" );
            if( mOctrl == null ) mOctrl = new HashSet<>();
            mOctrl.add( name );
            break;
        case "MOsboNormal" :
            // Seen in IEC_61850-8-1_2003A2.snsd
            console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: DataObject \"", name, "\" declared as \"MOsboNormal\" in PresenceCondition" );
            if( mOsboNormal == null ) mOsboNormal = new HashSet<>();
            mOsboNormal.add( name );
            break;
        case "MOsboEnhanced" :
            // Seen in IEC_61850-8-1_2003A2.snsd
            console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
                             "NOT IMPLEMENTED: DataObject \"", name, "\" declared as \"MOsboEnhanced\" in PresenceCondition" );
            if( mOsboEnhanced == null ) mOsboEnhanced = new HashSet<>();
            mOsboEnhanced.add( name );
            break;
        default:
            console.warning( DO_SETUP_NSD_CATEGORY, filename, lineNumber,
                             "the PresenceCondition ", presCond, " of DataObject \"", name, "\" is unknown" );
            break;
        }
        
    }
    
    private void checkSpecification() {
        // TODO: do we have to check the presence of the sibling in inherited AbstractLNClass ?
        if( mandatoryIfSiblingPresentElseForbidden != null ) {
            for( Entry< String, String > e : mandatoryIfSiblingPresentElseForbidden.entrySet() ) {
                if( ! presentDO.containsKey( e.getValue() )) {
                    console.warning( DO_SETUP_NSD_CATEGORY, 0,
                                     "the sibling of ", e.getKey(), " in PresenceCondition of DataObject ", e.getKey(), " is unknown in namespace \"", nsIdentification, "\"" );
                }
            }
        }
        if( mandatoryIfSiblingPresentElseOptional != null ) {
            for( Entry< String, String > e : mandatoryIfSiblingPresentElseOptional.entrySet() ) {
                if( ! presentDO.containsKey( e.getValue() )) {
                    console.warning( DO_SETUP_NSD_CATEGORY, 0,
                                     "the sibling of ", e.getKey(), " in PresenceCondition of DataObject ", e.getKey(), " is unknown in namespace \"", nsIdentification, "\"" );
                }
            }
        }
        if( optionalIfSiblingPresentElseMandatory != null ) {
            for( Entry< String, String > e : optionalIfSiblingPresentElseMandatory.entrySet() ) {
                if( ! presentDO.containsKey( e.getValue() )) {
                    console.warning( DO_SETUP_NSD_CATEGORY, 0,
                                     "the sibling of ", e.getKey(), " in PresenceCondition of DataObject ", e.getKey(), " is unknown in namespace \"", nsIdentification, "\"" );
                }
            }
        }
        if( forbiddenIfSiblingPresentElseMandatory != null ) {
            for( Entry< String, String > e : forbiddenIfSiblingPresentElseMandatory.entrySet() ) {
                if( ! presentDO.containsKey( e.getValue() )) {
                    console.warning( DO_SETUP_NSD_CATEGORY, 0,
                                     "the sibling of ", e.getKey(), " in PresenceCondition of DataObject ", e.getKey(), " is unknown in namespace \"", nsIdentification, "\"" );
                }
            }
        }
        if( oneOrMoreIfSiblingPresentElseForbidden != null ) {
            for( Entry< String, String > e : oneOrMoreIfSiblingPresentElseForbidden.entrySet() ) {
                if( ! presentDO.containsKey( e.getValue() )) {
                    console.warning( DO_SETUP_NSD_CATEGORY, 0,
                                     "the sibling of ", e.getKey(), " in PresenceCondition of DataObject ", e.getKey(), " is unknown in namespace \"", nsIdentification, "\"" );
                }
            }
        }
        if( optionalIfSiblingPresentElseForbidden != null ) {
            for( Entry< String, String > e : optionalIfSiblingPresentElseForbidden.entrySet() ) {
                if( ! presentDO.containsKey( e.getValue() )) {
                    console.warning( DO_SETUP_NSD_CATEGORY, 0,
                                     "the sibling of ", e.getKey(), " in PresenceCondition of DataObject ", e.getKey(), " is unknown in namespace \"", nsIdentification, "\"" );
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
                    RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(), 
                            "Unexpected DO name \"", do_.getName(), "\" in LNodeType id \"", do_.getParentLNodeType().getId(), "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            error.getMessage(),
                            new Object[] { do_, error } ));
                    return false;
                }
            }
        }
        if( ! presentDO.containsKey( names[0] )) {
            if( base != null ) {
                return base.addDO( do_, anyLNClassName, diagnostics );
            }
            RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, do_.getParentLNodeType().getFilename(), do_.getParentLNodeType().getLineNumber(), 
                                      "DO \"", do_.getName(), "\" in LNodeType id \"", do_.getParentLNodeType().getId(), "\" not found in LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    error.getMessage(),
                    new Object[] { do_, error } ));
            return false;
        }

        if( names.length == 1 ) {
            if( presentDO.get( do_.getName() ) != null ) {
                RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, do_.getParentLNodeType().getFilename(), do_.getParentLNodeType().getLineNumber(), 
                                          "DO \"", do_.getName(), "\" in LNodeType id \"", do_.getParentLNodeType().getId(), "\" already present in LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        error.getMessage(),
                        new Object[] { do_, error } ));
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
                RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, do_.getParentLNodeType().getFilename(), do_.getParentLNodeType().getLineNumber(), 
                                          "DO \"", do_.getName(), "\" in LNodeType id \"", do_.getParentLNodeType().getId(), "\" already present without instance number in LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        error.getMessage(),
                        new Object[] { do_, error } ));
                return false;
            }

            MultiDO m = ( MultiDO ) presentDO.get( names[0] );
            Integer number = Integer.valueOf( names[1] );
                
            if( m.numberedDOs.containsKey( number )) {
                RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, do_.getParentLNodeType().getFilename(), do_.getParentLNodeType().getLineNumber(), 
                                          "DO \"", do_.getName(), "\" in LNodeType id \"", do_.getParentLNodeType().getId(), "\" already present with same instance number in LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        error.getMessage(),
                        new Object[] { do_, error } ));
                return false;
            }
            m.add( number, do_ );
            return true;
        }
        console.warning( DO_VALIDATION_NSD_CATEGORY, do_.getParentLNodeType().getLineNumber(), 
                         "DO \"", do_.getName(), "\" in LNodeType id \"", do_.getParentLNodeType().getId(), "\" has an unrecognized name in namespace \"", nsIdentification, "\"" );
        return false;
    }
    
    public boolean validate( LNodeType lNodeType, DiagnosticChain diagnostics ) {
        @NonNull
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                       "DataObjectPresenceConditionValidator.validate( ", lNodeType.getId(), " ) in namespace \"", nsIdentification, "\"" );
        return validate( lNodeType, anyLNClass.getName(), false, diagnostics );
    }
    
    private boolean validate( LNodeType lNodeType, String anyLNClassName, boolean asSuperclass, DiagnosticChain diagnostics ) {
        boolean res = true;
        
        @NonNull
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();

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
            for( String name : this.mandatory ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"M\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) == null ) {
                    RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                              "DO \"", name, "\" is mandatory in LNodeType ( id=", lNodeType.getId(), " ) with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                          Diagnostic.ERROR,
                          RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                          0,
                          error.getMessage(),
                          new Object[] { lNodeType, error } ));
                  res = false;
                }
                else if( presentDO.get( name ) instanceof MultiDO ) {
                    RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                              "DO \"", name, "\" should not have an instance number in LNodeType ( id=", lNodeType.getId(), " ) with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            error.getMessage(),
                            new Object[] { lNodeType, error } ));
                    res = false;
                  }
            }
        }

        // presCond: "O"
        // Element is optional
        // Usage in standard NSD files (version 2007B): DataObject and DataAttribute and SubDataAttribute
        if( optional != null ) {
            for( String name : this.optional ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"O\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) == null ) {
                }
                else if( presentDO.get( name ) instanceof MultiDO ) {
                    RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                              "DO \"", name, "\" should not have an instance number in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            error.getMessage(),
                            new Object[] { lNodeType, error } ));
                    res = false;
                }
            }
        }

        // presCond: "F"
        // Element is forbidden
        // Usage in standard NSD files (version 2007B): DataObject
        if( forbidden != null ) {
            for( String name : this.forbidden ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"F\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) != null ) {
                    RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                              "DO \"", name, "\" is forbidden in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                          Diagnostic.ERROR,
                          RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                          0,
                          error.getMessage(),
                          new Object[] { lNodeType, error } ));
                  res = false;
                }
                else if( presentDO.get( name ) instanceof MultiDO ) {
                    RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                              "DO \"", name, "\" should not have an instance number in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            error.getMessage(),
                            new Object[] { lNodeType, error } ));
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
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition naM\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) != null ) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                "verification of PresenceCondition \"na\" for DO \"", name, "\" is not implemented in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { lNodeType, warning } ));
                }
            }
        }
        
        // presCond: "Mmulti"
        // At least one element shall be present; all instances have an instance number > 0
        // Usage in standard NSD files (version 2007B): DataObject
        if( mandatoryMulti != null ) {
            for( String name : this.mandatoryMulti ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"Mmulti\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) == null ) {
                    RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getLineNumber(), 
                                              "At least one DO \"", name, "\" is mandatory in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                          Diagnostic.ERROR,
                          RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                          0,
                          error.getMessage(),
                          new Object[] { lNodeType, error } ));
                  res = false;
                }
                else if( presentDO.get( name ) instanceof SingleDO ) {
                    RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                              "DO \"", name, "\" should have an instance number in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            error.getMessage(),
                            new Object[] { lNodeType, error } ));
                    res = false;
                }
            }
        }

        // presCond: "Omulti"
        // Zero or more elements may be present; all instances have an instance number > 0
        // Usage in standard NSD files (version 2007B): DataObject
        if( optionalMulti != null ) {
            for( String name : this.optionalMulti ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"Omulti\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) == null ) {
                }
                else if( presentDO.get( name ) instanceof SingleDO ) {
                    RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                              "DO \"", name, "\" should have an instance number in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            error.getMessage(),
                            new Object[] { lNodeType, error } ));
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
            for( Entry< Integer, HashSet< String > > e1 : atLeastOne.entrySet() ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"AtLeastOne\" for group ", e1.getKey(), " in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                boolean groupOK = false;
                String atLeastOneOf = " (at least one of:";
                for( String member : e1.getValue() ) {
                    atLeastOneOf += " \"" + member + "\"";
                    if( presentDO.get( member ) != null ) {
                        groupOK = true;
                        break;
                    }
                }
                atLeastOneOf += ")";
                if( ! groupOK ) {
                    RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                              "group ", e1.getKey(), " has no elements in LNodeType with LNClass ", anyLNClassName,
                                              " in namespace \"", nsIdentification, "\"", atLeastOneOf );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            error.getMessage(),
                            new Object[] { lNodeType, error } ));
                    res = false;
                }
            }
        }
        
        // presCond: "AtMostOne" :
        // At most one of marked elements shall be present
        // Usage in standard NSD files (version 2007B): DataObject
        //if( atMostOne != null ) {
        if( ! asSuperclass ) {
            console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                    "validation of presence condition \"AtMostOne\" in LNodeType id = \"",
                    lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
            int count = 0;
            String atMostOneOf = " (at most one of:";
            for( String s : atMostOne ) {
                atMostOneOf += " \"" + s + "\"";
                if( presentDO.get( s ) != null ) {
                    ++count;
                }
            }
            atMostOneOf += ")";
            if( count > 1 ) {
                RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                          "LNodeType with LNClass \"", anyLNClassName, "\" has more than one element marked AtMostOne in namespace \"",
                                          nsIdentification, "\"", atMostOneOf );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        error.getMessage(),
                        new Object[] { lNodeType, error } ));
                res = false;
            }
        }
        
        // presCond: "AllOrNonePerGroup" :
        // Parameter n: group number (> 0).
        // All or none of the elements of a group n shall be present
        // Usage in standard NSD files (version 2007B): DataAttribute
        //if( allOrNonePerGroup != null ) {
        if( ! asSuperclass ) {
            for( Entry< Integer, HashSet< String > > e1 : allOrNonePerGroup.entrySet() ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"AllOrNonePerGroup\" for group ", e1.getKey(), " in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                int groupCount = 0;
                String expectedMembers = " (expected members:";
                for( String member : e1.getValue() ) {
                    expectedMembers += " \"" + member + "\"";
                    if( presentDO.get( member ) != null ) {
                        ++groupCount;
                    }
                }
                expectedMembers += ")";
                if(( groupCount > 0 ) && ( groupCount < e1.getValue().size() )) {
                    RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                              "group ", e1.getKey(), " has neither none nor all elements in LNodeType with LNClass ", anyLNClassName,
                                              " in namespace \"", nsIdentification, "\"", expectedMembers );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            error.getMessage(),
                            new Object[] { lNodeType, error } ));
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
            int groupNumber = 0;
            for( Entry< Integer, HashSet< String > > e1 : allOnlyOneGroup.entrySet() ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"AllOnlyOneGroup\" for group ", e1.getKey(), " in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                int groupCount = 0;
                String expectedMembers = " (expected members:";
                for( String member : e1.getValue() ) {
                    expectedMembers += " \"" + member + "\"";
                    if( presentDO.get( member ) != null ) {
                        ++groupCount;
                    }
                }
                expectedMembers += ")";
                if(( groupCount > 0 ) && ( groupCount < e1.getValue().size() )) {
                    RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                              "group ", e1.getKey(), " has neither none nor all elements in LNodeType with LNClass ", anyLNClassName,
                                              " in namespace \"", nsIdentification, "\"", expectedMembers );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            error.getMessage(),
                            new Object[] { lNodeType, error } ));
                    res = false;
                }
                else if( groupCount > 0 ) {
                    if( groupNumber == 0 ) {
                        groupNumber = e1.getKey();
                    }
                    else {
                        RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                  "LNodeType with LNClass \"", anyLNClassName, "\" has several groups with all elements in namespace \"", nsIdentification, "\"" );
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                error.getMessage(),
                                new Object[] { lNodeType, error } ));
                        res = false;
                    }
                }
            }
            if( groupNumber == 0 ) {
                RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                          "no group in LNodeType with LNClass \"", anyLNClassName, "\" has all elements in namespace \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        error.getMessage(),
                        new Object[] { lNodeType, error } ));
                res = false;
            }
        }
        
        // presCond: "AllAtLeastOneGroup" :
        // Parameter n: group number (> 0).
        // All elements of at least one group n shall be present
        // Usage in standard NSD files (version 2007B): DataAttribute
        //if( allAtLeastOneGroup != null ) {
        if(( ! asSuperclass ) && ( allAtLeastOneGroup.size() != 0 )) {         // groupNumber == 0 not an error if empty
            int groupNumber = 0;
            for( Entry< Integer, HashSet< String > > e1 : allAtLeastOneGroup.entrySet() ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"AllAtLeastOneGroup\" for group ", e1.getKey(), " in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
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
                RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                          "no group in LNodeType with LNClass \"", anyLNClassName, "\" has all elements in namespace \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        error.getMessage(),
                        new Object[] { lNodeType, error } ));
                res = false;
            }
        }
        
        // presCond: "MF" :
        // Parameter sibling: sibling element name.
        // Mandatory if sibling element is present, otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataObject
        if( mandatoryIfSiblingPresentElseForbidden != null ) {
            for( Entry< String, String > entry : mandatoryIfSiblingPresentElseForbidden.entrySet() ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MF\" for DO \"", entry.getKey(), "\" sibling \"", entry.getValue(), "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( entry.getValue() ) != null ) {
                    if( presentDO.get( entry.getKey() ) == null ) {
                        RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                  "DO \"", entry.getKey(), "\" is mandatory in LNodeType with LNClass \"", anyLNClassName, "\" because sibling ", entry.getValue(), " is present in namespace \"", nsIdentification, "\"" );
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                error.getMessage(),
                                new Object[] { lNodeType, error } ));
                        res = false;
                    }
                }
                else {
                    if( presentDO.get( entry.getKey() ) != null ) {
                        RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                  "DO \"", entry.getKey(), "\" is forbidden in LNodeType with LNClass \"", anyLNClassName, "\" because sibling ", entry.getValue(), " is not present in namespace \"", nsIdentification, "\"" );
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                error.getMessage(),
                                new Object[] { lNodeType, error } ));
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
                if( presentDO.get( entry.getValue() ) != null ) {
                    console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                            "validation of presence condition \"MO\" for DO \"", entry.getKey(), "\" sibling \"", entry.getValue(), "\" in LNodeType id = \"",
                            lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    if( presentDO.get( entry.getKey() ) == null ) {
                        RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                  "DO \"", entry.getKey(), "\" is mandatory in LNodeType with LNClass \"", anyLNClassName, "\" because sibling ", entry.getValue(), " is present in namespace \"", nsIdentification, "\"" );
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                error.getMessage(),
                                new Object[] { lNodeType, error } ));
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
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"OM\" for DO \"", entry.getKey(), "\" sibling \"", entry.getValue(), "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( entry.getValue() ) == null ) {
                    if( presentDO.get( entry.getKey() ) == null ) {
                        RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                  "DO \"", entry.getKey(), "\" is mandatory in LNodeType with LNClass \"", anyLNClassName, "\" because sibling ", entry.getValue(), " is not present in namespace \"", nsIdentification, "\"" );
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                error.getMessage(),
                                new Object[] { lNodeType, error } ));
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
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"FM\" for DO \"", entry.getKey(), "\" sibling \"", entry.getValue(), "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( entry.getValue() ) != null ) {
                    if( presentDO.get( entry.getKey() ) != null ) {
                        RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                  "DO \"", entry.getKey(), "\" is forbidden in LNodeType with LNClass \"", anyLNClassName, "\" because sibling ", entry.getValue(), " is present in namespace \"", nsIdentification, "\"" );
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                error.getMessage(),
                                new Object[] { lNodeType, error } ));
                        res = false;
                    }
                }
                else {
                    if( presentDO.get( entry.getKey() ) == null ) {
                        RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                  "DO \"", entry.getKey(), "\" is mandatory in LNodeType with LNClass \"", anyLNClassName, "\" because sibling ", entry.getValue(), " is not present in namespace \"", nsIdentification, "\"" );
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                error.getMessage(),
                                new Object[] { lNodeType, error } ));
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
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MOcond\" for DO \"", entry.getKey(), "\" textual condition number ", entry.getValue(), " in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
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

                RiseClipseMessage warning = RiseClipseMessage.warning( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                            "DO \"", entry.getKey(), "\" is mandatory in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"",
                                            " if textual condition number " + entry.getValue(), " (not evaluated) is true, else optional. It is ",
                                            ( presentDO.get( entry.getKey() ) == null ? "absent." : "present." ),
                                            ( doc != null ? " Textual condition is: \"" + doc + "\"." : "" ));
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        warning.getMessage(),
                        new Object[] { lNodeType, warning } ));
            }
        }
        
        // presCond: "MFcond" :
        // Parameter condID: condition number (> 0).
        // Textual presence condition (non-machine processable) with reference condID to context specific text.
        // If satisfied, the element is mandatory, otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataObject
        if( mandatoryIfTextConditionElseForbidden != null ) {
            for( Entry< String, String > entry : mandatoryIfTextConditionElseForbidden.entrySet() ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MFcond\" for DO \"", entry.getKey(), "\" textual condition number ", entry.getValue(), " in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
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

                RiseClipseMessage warning = RiseClipseMessage.warning( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                            "DO \"", entry.getKey(), "\" is mandatory in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"",
                                            " if textual condition number " + entry.getValue(), " (not evaluated) is true, else forbidden. It is ",
                                            ( presentDO.get( entry.getKey() ) == null ? "absent." : "present." ),
                                            ( doc != null ? " Textual condition is: \"" + doc + "\"." : "" ));
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        warning.getMessage(),
                        new Object[] { lNodeType, warning } ));
            }
        }
        
        // presCond: "OFcond" :
        // Parameter condID: condition number (> 0).
        // Textual presence condition (non-machine processable) with reference condID to context specific text.
        // If satisfied, the element is optional, otherwise forbidden
        // Usage in standard NSD files (version 2007B): DataObject
        if( optionalIfTextConditionElseForbidden != null ) {
            for( Entry< String, String > entry : optionalIfTextConditionElseForbidden.entrySet() ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"OFcond\" for DO \"", entry.getKey(), "\" textual condition number ", entry.getValue(), " in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
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

                RiseClipseMessage warning = RiseClipseMessage.warning( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                            "DO \"", entry.getKey(), "\" is optional in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"",
                                            " if textual condition number " + entry.getValue(), " (not evaluated) is true, else forbidden. It is ",
                                            ( presentDO.get( entry.getKey() ) == null ? "absent." : "present." ),
                                            ( doc != null ? " Textual condition is: \"" + doc + "\"." : "" ));
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        warning.getMessage(),
                        new Object[] { lNodeType, warning } ));
            }
        }
        
        // presCond: "MmultiRange" :
        // Parameters min, max: limits for instance number (> 0).
        // One or more elements shall be present; all instances have an instance number within range [min, max] (see IEC 61850-7-1)
        // Usage in standard NSD files (version 2007B): None
        if( mandatoryMultiRange != null ) {
            for( String name : this.mandatoryMultiRange.keySet() ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MmultiRange\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) == null ) {
                    RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                              "At least one DO \"", name, "\" is mandatory in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                          Diagnostic.ERROR,
                          RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                          0,
                          error.getMessage(),
                          new Object[] { lNodeType, error } ));
                    res = false;
                }
                else if( presentDO.get( name ) instanceof SingleDO ) {
                    RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                              "DO \"", name, "\" should have an instance number in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            error.getMessage(),
                            new Object[] { lNodeType, error } ));
                    res = false;
                }
                else {
                    MultiDO m = ( MultiDO ) presentDO.get( name );
                    for( Integer n : m.numberedDOs.keySet() ) {
                        Integer min = mandatoryMultiRange.get( name ).getLeft();
                        Integer max = mandatoryMultiRange.get( name ).getRight();
                        if(( n < min ) || ( n > max )) {
                            RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                       "DO \"", name, "\" should have an instance number in range [", min, ",", max, "] in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                            diagnostics.add( new BasicDiagnostic(
                                    Diagnostic.ERROR,
                                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                    0,
                                    error.getMessage(),
                                    new Object[] { lNodeType, error } ));
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
            for( String name : this.optionalMultiRange.keySet() ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"OmultiRange\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) == null ) {
                }
                else if( presentDO.get( name ) instanceof SingleDO ) {
                    RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                              "DO \"", name, "\" should have an instance number in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            error.getMessage(),
                            new Object[] { lNodeType, error } ));
                    res = false;
                }
                else {
                    MultiDO m = ( MultiDO ) presentDO.get( name );
                    for( Integer n : m.numberedDOs.keySet() ) {
                        Integer min = optionalMultiRange.get( name ).getLeft();
                        Integer max = optionalMultiRange.get( name ).getRight();
                        if(( n < min ) || ( n > max )) {
                            RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                       "DO \"", name, "\" should have an instance number in range [", min, ",", max, "] in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                            diagnostics.add( new BasicDiagnostic(
                                    Diagnostic.ERROR,
                                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                    0,
                                    error.getMessage(),
                                    new Object[] { lNodeType, error } ));
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
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MFsubst\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) != null ) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                "verification of PresenceCondition \"MFsubst\" for DO \"", name, "\" is not implemented in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { lNodeType, warning } ));
                }
            }
        }
        
        // presCond: "MOln0" :
        // Element is mandatory in the context of LLN0; otherwise optional
        // Usage in standard NSD files (version 2007B): DataAttribute
        if( mandatoryInLLN0ElseOptional != null ) {
            for( String name : mandatoryInLLN0ElseOptional ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MOln0\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) == null ) {
                    for( AnyLN anyLN : lNodeType.getReferredByAnyLN() ) {
                        if( anyLN instanceof LN0 ) {
                            RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                      "DO \"", name, "\" is mandatory in LN0 in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                            diagnostics.add( new BasicDiagnostic(
                                    Diagnostic.ERROR,
                                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                    0,
                                    error.getMessage(),
                                    new Object[] { lNodeType, error } ));
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
            for( String name : mandatoryInLLN0ElseForbidden ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MFln0\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                for( AnyLN anyLN : lNodeType.getReferredByAnyLN() ) {
                    if( presentDO.get( name ) == null ) {
                        if( anyLN instanceof LN0 ) {
                            RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                      "DO \"", name, "\" is mandatory in LN0 in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                            diagnostics.add( new BasicDiagnostic(
                                    Diagnostic.ERROR,
                                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                    0,
                                    error.getMessage(),
                                    new Object[] { lNodeType, error } ));
                            res = false;
                        }
                    }
                    else {
                        if( ! ( anyLN instanceof LN0 )) {
                            RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                      "DO \"", name, "\" is forbidden in LN0 in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                            diagnostics.add( new BasicDiagnostic(
                                    Diagnostic.ERROR,
                                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                    0,
                                    error.getMessage(),
                                    new Object[] { lNodeType, error } ));
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
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MOlnNs\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) != null ) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                "verification of PresenceCondition \"MOlnNs\" for DO \"", name, "\" is not implemented in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { lNodeType, warning } ));
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
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MOdataNs\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) != null ) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                "verification of PresenceCondition \"MOdataNs\" for DO \"", name, "\" is not implemented in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { lNodeType, warning } ));
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
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MFscaledAV\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) != null ) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                "verification of PresenceCondition \"MFscaledAV\" for DO \"", name, "\" is not implemented in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { lNodeType, warning } ));
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
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MFscaledMagV\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) != null ) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                "verification of PresenceCondition \"MFscaledMagV\" for DO \"", name, "\" is not implemented in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { lNodeType, warning } ));
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
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MFscaledAngV\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) != null ) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                "verification of PresenceCondition \"MFscaledAngV\" for DO \"", name, "\" is not implemented in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { lNodeType, warning } ));
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
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MOrms\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) != null ) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                "verification of PresenceCondition \"MOrms\" for DO \"", name, "\" is not implemented in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { lNodeType, warning } ));
                }
            }
        }

        // presCond: "MOrootLD" :
        // Element is mandatory in the context of a root logical device; otherwise it is optional
        // Usage in standard NSD files (version 2007B): DataObject
        if( mandatoryInRootLogicalDeviceElseOptional != null ) {
            for( String name : mandatoryInRootLogicalDeviceElseOptional ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MOrootLD\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
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
                            RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                      "DO \"", name, "\" is mandatory in LN in LNodeType with LNClass \"", anyLNClassName, "\" in the context of a root logical device", " in namespace \"", nsIdentification, "\"" );
                            diagnostics.add( new BasicDiagnostic(
                                    Diagnostic.ERROR,
                                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                    0,
                                    error.getMessage(),
                                    new Object[] { lNodeType, error } ));
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
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MOoperTm\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) != null ) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                "verification of PresenceCondition \"MOoperTm\" for DO \"", name, "\" is not implemented in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { lNodeType, warning } ));
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
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MmultiF\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) != null ) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                "verification of PresenceCondition \"MmultiF\" for DO \"", name, "\" is not implemented in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { lNodeType, warning } ));
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
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MOsbo\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) != null ) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                "verification of PresenceCondition \"MOsbo\" for DO \"", name, "\" is not implemented in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { lNodeType, warning } ));
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
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MOenhanced\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) != null ) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                 "verification of PresenceCondition \"MOenhanced\" for DO \"", name, "\" is not implemented in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { lNodeType, warning } ));
                }
            }
        }

        // presCond: "MONamPlt" :
        // Element is mandatory if the name space of its logical node deviates from the name space of the containing
        // logical device, otherwise optional. See IEC 61850-7-1 for use of name space
        // Usage in standard NSD files (version 2007B): DataObject
        // TODO: same as "MOlnNs" ?
        if( mandatoryIfNameSpaceOfLogicalNodeDeviatesElseOptional2 != null ) {
            console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                           "validation of presence condition \"MONamPlt\" on LNodeType ( id=", lNodeType.getId(), " ) in namespace \"", nsIdentification, "\"" );
            
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
                        console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                                "validation of presence condition \"MONamPlt\" for DO \"", name, "\" in LNodeType id = \"",
                                lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                        if( presentDO.get( name ) == null ) {
                            RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                      "DO \"", name, "\" is mandatory in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"",
                                                      " because the name space of its logical node (\"", lnNs,
                                                      "\") deviates from the name space of the containing logical device (\"", ldNs, "\")" );
                            diagnostics.add( new BasicDiagnostic(
                                    Diagnostic.ERROR,
                                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                    0,
                                    error.getMessage(),
                                    new Object[] { lNodeType, error } ));
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
            for( Entry< String, String > entry : optionalIfSiblingPresentElseForbidden.entrySet() ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"OF\" for DO \"", entry.getKey(), "\" sibling \"", entry.getValue(), "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( entry.getValue() ) == null ) {
                    if( presentDO.get( entry.getKey() ) != null ) {
                        RiseClipseMessage error = RiseClipseMessage.error( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                  "DO \"", entry.getKey(), "\" is forbidden in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"", " because sibling ", entry.getValue(), " is not present" );
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                error.getMessage(),
                                new Object[] { lNodeType, error } ));
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
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MOrange\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) != null ) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                "verification of PresenceCondition \"MORange\" for DO \"", name, "\" is not implemented in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { lNodeType, warning } ));
                }
            }
        }

        // presCond: "OMSynPh" :
        // This attribute is optional if value of 'phsRef'' is Synchrophasor otherwise Mandatory]]></Doc>
        // Usage in standard NSD files (version 2007B): SubDataObject
        // TODO
        if( optionalIfPhsRefIsSynchrophasorElseMandatory != null ) {
            for( String name : optionalIfPhsRefIsSynchrophasorElseMandatory ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"OMSynPh\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) != null ) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                "verification of PresenceCondition \"OMSynPh\" for DO \"", name, "\" is not implemented in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { lNodeType, warning } ));
                }
            }
        }

        // presCond: "MAllOrNonePerGroup" :
        // TODO
        if( mAllOrNonePerGroup != null ) {
            for( String name : mAllOrNonePerGroup ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MAllOrNonePerGroup\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) != null ) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                "verification of PresenceCondition \"MAllOrNonePerGroup\" for DO \"", name, "\" is not implemented in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { lNodeType, warning } ));
                }
            }
        }

        // presCond: "MOctrl" :
        // TODO
        if( mOctrl != null ) {
            for( String name : mOctrl ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MOctrl\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) != null ) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                "verification of PresenceCondition \"MOctrl\" for DO \"", name, "\" is not implemented in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { lNodeType, warning } ));
                }
            }
        }

        // presCond: "MOsboNormal" :
        // TODO
        if( mOsboNormal != null ) {
            for( String name : mOsboNormal ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MOsboNormal\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) != null ) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                "verification of PresenceCondition \"MOsboNormal\" for DO \"", name, "\" is not implemented in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { lNodeType, warning } ));
                }
            }
        }

        // presCond: "MOsboEnhanced" :
        // TODO
        if( mOsboEnhanced != null ) {
            for( String name : mOsboEnhanced ) {
                console.debug( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                        "validation of presence condition \"MOsboEnhanced\" for DO \"", name, "\" in LNodeType id = \"",
                        lNodeType.getId(), "\" with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                if( presentDO.get( name ) != null ) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( DO_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                                                "verification of PresenceCondition \"MOsboEnhanced\" for DO \"", name, "\" is not implemented in LNodeType with LNClass \"", anyLNClassName, "\" in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { lNodeType, warning } ));
                }
            }
        }

        return res;
    }

}
