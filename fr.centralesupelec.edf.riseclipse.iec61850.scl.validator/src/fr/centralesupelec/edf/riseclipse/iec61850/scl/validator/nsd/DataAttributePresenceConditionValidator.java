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

import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jdt.annotation.Nullable;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.CDC;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentification;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentificationName;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AbstractDataObject;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.BDA;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DA;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DO;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DOType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.RiseClipseMessage;

public class DataAttributePresenceConditionValidator extends GenericPresenceConditionValidator< CDC, DOType, @Nullable DA >{

    private static final String DA_SETUP_NSD_CATEGORY      = NsdValidator.SETUP_NSD_CATEGORY      + "/DataAttribute";
    private static final String DA_VALIDATION_NSD_CATEGORY = NsdValidator.VALIDATION_NSD_CATEGORY + "/DataAttribute";

    private static IdentityHashMap< NsIdentificationName, DataAttributePresenceConditionValidator > validators = new IdentityHashMap<>();
    
    public static DataAttributePresenceConditionValidator get( NsIdentification nsIdentification, CDC cdc ) {
        // TODO: do we need to use dependsOn links?
        if( ! validators.containsKey( NsIdentificationName.of( nsIdentification, cdc.getName() ))) {
            validators.put( NsIdentificationName.of( nsIdentification, cdc.getName() ), new DataAttributePresenceConditionValidator( nsIdentification, cdc ));
        }
        return validators.get( NsIdentificationName.of( nsIdentification, cdc.getName() ));
    }
    
    private Set< String > analogueValues;
    private Set< String > vectors;

    public DataAttributePresenceConditionValidator( NsIdentification nsIdentification, CDC cdc ) {
        super( nsIdentification, cdc );
        
        console.debug( DA_SETUP_NSD_CATEGORY, cdc.getFilename(), cdc.getLineNumber(),
                "DataAttributePresenceConditionValidator( \"", cdc.getName(), "\" in namespace \"", nsIdentification, "\" )");
        
        analogueValues = new HashSet<>();
        vectors        = new HashSet<>();
        
        initialize();
    }

    @Override
    protected String getSetupMessageCategory() {
        return DA_SETUP_NSD_CATEGORY;
    }

    @Override
    protected String getValidationMessageCategory() {
        return DA_VALIDATION_NSD_CATEGORY;
    }

    @Override
    protected void createSpecifications() {
        nsdModel
        .getDataAttribute()
        .stream()
        .forEach( da -> {
            addSpecification( da.getName(), da.getPresCond(), da.getPresCondArgs(), da.getRefersToPresCondArgsDoc(), da.getLineNumber(), da.getFilename() );

            // For presence condition "MFscaledAV"                     , we need to know which DataAttribute is of type AnalogueValue
            // For presence condition "MFscaledMagV" and "MFscaledAngV", we need to know which DataAttribute is of type Vector
            if( "AnalogueValue".equals( da.getType() )) analogueValues.add( da.getName() );
            if( "Vector"       .equals( da.getType() )) vectors       .add( da.getName() );
        });
    }

    @Override
    protected String getPresenceConditionValidatorName() {
        return "DataAttributePresenceConditionValidator";
    }

    @Override
    protected String getNsdModelName() {
        return nsdModel.getName();
    }

    @Override
    protected int getNsdModelLineNumber() {
        return nsdModel.getLineNumber();
    }

    @Override
    protected String getNsdModelClassName() {
        return "CDC";
    }

    @Override
    protected String getNsdComponentClassName() {
        return "DataAttribute";
    }

    @Override
    protected String getSclModelClassName() {
        return "DOType";
    }

    @Override
    protected String getSclComponentClassName() {
        return "DA";
    }

    @Override
    protected boolean validateMFln0( DOType doType, DiagnosticChain diagnostics ) {
        boolean res = true;
        EList< AbstractDataObject > adoList = doType.getReferredByAbstractDataObject();
        for( AbstractDataObject ado : adoList ) {
            if( ado instanceof DO ) {
                DO do_ = ( DO ) ado;
                if( "LLN0".equals( do_.getParentLNodeType().getLnClass() )) {
                    for( String attribute : mandatoryInLLN0ElseOptional ) {
                        DA da = presentSclComponent.get( attribute );
                        if( da == null ) {
                            RiseClipseMessage error = RiseClipseMessage.error( DA_VALIDATION_NSD_CATEGORY, doType.getLineNumber(), 
                                                      getSclComponentClassName(), " ", attribute, " is mandatory in ", getSclModelClassName(),
                                                      " with LNClass LLN0", " in namespace \"", nsIdentification, "\"" );
                            diagnostics.add( new BasicDiagnostic(
                                    Diagnostic.ERROR,
                                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                    0,
                                    error.getMessage(),
                                    new Object[] { doType, error } ));
                            res = false;
                        }
                    }
                }
            }
            else {
                // ado instanceof SDO
            }
        }
        return res;
    }

    @Override
    protected boolean validateMOln0( DOType doType, DiagnosticChain diagnostics ) {
        boolean res = true;
        EList< AbstractDataObject > adoList = doType.getReferredByAbstractDataObject();
        for( AbstractDataObject ado : adoList ) {
            if( ado instanceof DO ) {
                DO do_ = ( DO ) ado;
                if( "LLN0".equals( do_.getParentLNodeType().getLnClass() )) {
                    for( String attribute : mandatoryInLLN0ElseForbidden ) {
                        DA da = presentSclComponent.get( attribute );
                        if( da == null ) {
                            RiseClipseMessage error = RiseClipseMessage.error( DA_VALIDATION_NSD_CATEGORY, doType.getLineNumber(), 
                                                      getSclComponentClassName(), " \"", attribute, "\" is mandatory in ", getSclModelClassName(),
                                                      " with LNClass LLN0", " in namespace \"", nsIdentification, "\"" );
                            diagnostics.add( new BasicDiagnostic(
                                    Diagnostic.ERROR,
                                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                    0,
                                    error.getMessage(),
                                    new Object[] { doType, error } ));
                            res = false;
                        }
                    }
                }
                else {
                    for( String attribute : mandatoryInLLN0ElseForbidden ) {
                        DA da = presentSclComponent.get( attribute );
                        if( da != null ) {
                            RiseClipseMessage error = RiseClipseMessage.error( DA_VALIDATION_NSD_CATEGORY, doType.getFilename(), doType.getLineNumber(), 
                                                      getSclComponentClassName(), " \"", attribute, "\" is forbidden in ", getSclModelClassName(),
                                                      " with LNClass ", do_.getParentLNodeType().getLnClass(), " at line ", 
                                                      do_.getParentLNodeType().getLineNumber(), " in namespace \"", nsIdentification, "\"" );
                            diagnostics.add( new BasicDiagnostic(
                                    Diagnostic.ERROR,
                                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                    0,
                                    error.getMessage(),
                                    new Object[] { doType, error } ));
                            res = false;
                        }
                    }
                }
            }
            else {
                // ado instanceof SDO
            }
        }
        return res;
    }

    @Override
    protected boolean validateOMSynPh( DOType doType, DiagnosticChain diagnostics ) {
        for( String name : optionalIfPhsRefIsSynchrophasorElseMandatory ) {
            if( presentSclComponent.get( name ) != null ) {
                RiseClipseMessage notice = RiseClipseMessage.notice( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, doType.getFilename(), doType.getLineNumber(), 
                                            "verification of PresenceCondition \"OMSynPh\" for ", getSclComponentClassName(),
                                            " is not implemented in ", getSclModelClassName(), ") with ", getNsdModelClassName(),
                                            " \"", getNsdModelName(), "\" in namespace \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.INFO,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        notice.getMessage(),
                        new Object[] { doType, notice } ));
            }
        }
        return true;
    }

    @Override
    protected boolean validateMFscaledAV( DOType doType, DiagnosticChain diagnostics ) {
        boolean res = true;
        // Element is mandatory* if any sibling elements of type AnalogueValue include 'i' as a child, otherwise forbidden.
        // *Even though devices without floating point capability cannot exchange floating point values through ACSI services,
        // the description of scaling remains mandatory for their (SCL) configuration
        boolean iIsPresent = false;
        for( DA da : doType.getDA() ) {
            if( analogueValues.contains( da.getName() )) {
                boolean iFound = 
                    da
                    .getRefersToDAType()
                    .getBDA()
                    .stream()
                    .anyMatch( bda -> "i".equals( bda.getName() ));
                if( iFound ) {
                    iIsPresent = true;
                    break;
                }
            }
        }
        
        for( String name : mandatoryIfAnalogValueIncludesIElseForbidden ) {
            if( iIsPresent && ( presentSclComponent.get( name ) == null )) {
                RiseClipseMessage error = RiseClipseMessage.error( DA_VALIDATION_NSD_CATEGORY, doType.getFilename(), doType.getLineNumber(), 
                        getSclComponentClassName(), " \"", name, "\" is mandatory in ", getSclModelClassName(), " in namespace \"", nsIdentification, "\"",
                        " because there are sibling elements of type AnalogueValue which includes 'i' as a child" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        error.getMessage(),
                        new Object[] { doType, error } ));
                res = false;
            }
            else if( ! iIsPresent && ( presentSclComponent.get( name ) != null )) {
                RiseClipseMessage error = RiseClipseMessage.error( DA_VALIDATION_NSD_CATEGORY, doType.getFilename(), doType.getLineNumber(), 
                        getSclComponentClassName(), " \"", name, "\" is forbidden in ", getSclModelClassName(), " in namespace \"", nsIdentification, "\"",
                        " because there are no sibling element of type AnalogueValue which includes 'i' as a child" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        error.getMessage(),
                        new Object[] { doType, error } ));
                res = false;
            }
        }
        return res;
    }
    
    @Override
    protected boolean validateMFscaledMagV( DOType doType, DiagnosticChain diagnostics ) {
        // Element is mandatory* if any sibling elements of type Vector include 'i' as a child of their 'mag' attribute, otherwise forbidden.
        // *See MFscaledAV
        return validateMFscaledMagOrAngV( doType, "mag", mandatoryIfVectorSiblingIncludesIAsChildMagElseForbidden, diagnostics );
    }

    @Override
    protected boolean validateMFscaledAngV( DOType doType, DiagnosticChain diagnostics ) {
        // Element is mandatory* if any sibling elements of type Vector include 'i' as a child of their 'ang' attribute, otherwise forbidden.
        // *See MFscaledAV
        return validateMFscaledMagOrAngV( doType, "ang", mandatoryIfVectorSiblingIncludesIAsChildAngElseForbidden, diagnostics );
    }

    private boolean validateMFscaledMagOrAngV( DOType doType, String marOrAng, Set< String> toTest, DiagnosticChain diagnostics ) {
        boolean res = true;
        boolean iIsPresent = false;
        for( DA da : doType.getDA() ) {
            if( vectors.contains( da.getName() )) {
                Optional< BDA > magBDA =
                         da
                         .getRefersToDAType()
                         .getBDA()
                         .stream()
                         .filter( bda -> marOrAng.equals( bda.getName() ))
                         .findAny();
                if( magBDA.isPresent() ) {
                    boolean iFound = 
                        magBDA
                        .get()
                        .getRefersToDAType()
                        .getBDA()
                        .stream()
                        .anyMatch( bda -> "i".equals( bda.getName() ));
                    if( iFound ) {
                        iIsPresent = true;
                        break;
                    }
                }
            }
        }
        for( String name : toTest ) {
            if( iIsPresent && ( presentSclComponent.get( name ) == null )) {
                RiseClipseMessage error = RiseClipseMessage.error( DA_VALIDATION_NSD_CATEGORY, doType.getFilename(), doType.getLineNumber(), 
                        getSclComponentClassName(), " \"", name, "\" is mandatory in ", getSclModelClassName(), " in namespace \"", nsIdentification, "\"",
                        " because there are sibling elements of type Vector which includes 'i' as a child of their " + marOrAng + " attribute" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        error.getMessage(),
                        new Object[] { doType, error } ));
                res = false;
            }
            else if( ! iIsPresent && ( presentSclComponent.get( name ) != null )) {
                RiseClipseMessage error = RiseClipseMessage.error( DA_VALIDATION_NSD_CATEGORY, doType.getFilename(), doType.getLineNumber(), 
                        getSclComponentClassName(), " \"", name, "\" is forbidden in ", getSclModelClassName(), " in namespace \"", nsIdentification, "\"",
                        " because there are no sibling element of type Vector which includes 'i' as a child of their " + marOrAng + " attribute" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        error.getMessage(),
                        new Object[] { doType, error } ));
                res = false;
            }
        }
        return res;
    }
    
}
