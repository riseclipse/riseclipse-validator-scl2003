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

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jdt.annotation.Nullable;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.CDC;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentification;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentificationName;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AbstractDataObject;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DA;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DO;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DOType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.RiseClipseMessage;

public class DataAttributePresenceConditionValidator extends GenericPresenceConditionValidator< CDC, DOType, @Nullable DA >{

    private static final String DA_SETUP_NSD_CATEGORY      = NsdValidator.SETUP_NSD_CATEGORY      + "/DataAttribute";
    private static final String DA_VALIDATION_NSD_CATEGORY = NsdValidator.VALIDATION_NSD_CATEGORY + "/DataAttribute";

    private static HashMap< String, DataAttributePresenceConditionValidator > validators = new HashMap<>();
    
    public static DataAttributePresenceConditionValidator get( NsIdentification nsIdentification, CDC cdc ) {
        if( ! validators.containsKey( new NsIdentificationName( nsIdentification, cdc.getName() ))) {
            validators.put( new NsIdentificationName( nsIdentification, cdc.getName() ), new DataAttributePresenceConditionValidator( nsIdentification, cdc ));
        }
        return validators.get( new NsIdentificationName( nsIdentification, cdc.getName() ));
    }
    
    private CDC cdc;

    public DataAttributePresenceConditionValidator( NsIdentification nsIdentification, CDC cdc ) {
        super( nsIdentification, cdc );
        
        this.cdc = cdc;
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
    protected void createSpecifications( CDC cdc ) {
        cdc
        .getDataAttribute()
        .stream()
        .forEach( da -> addSpecification( da.getName(), da.getPresCond(), da.getPresCondArgs(), da.getRefersToPresCondArgsDoc(), da.getLineNumber(), da.getFilename() ));
    }

    @Override
    protected String getPresenceConditionValidatorName() {
        return "DataAttributePresenceConditionValidator";
    }

    @Override
    protected String getNsdModelName() {
        return cdc.getName();
    }

    @Override
    protected int getNsdModelLineNumber() {
        return cdc.getLineNumber();
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
    protected boolean validateMFln0( DOType sclModel, DiagnosticChain diagnostics ) {
        boolean res = true;
        EList< AbstractDataObject > adoList = sclModel.getReferredByAbstractDataObject();
        for( AbstractDataObject ado : adoList ) {
            if( ado instanceof DO ) {
                DO do_ = ( DO ) ado;
                if( "LLN0".equals( do_.getParentLNodeType().getLnClass() )) {
                    for( String attribute : mandatoryInLLN0ElseOptional ) {
                        DA da = presentSclComponent.get( attribute );
                        if( da == null ) {
                            RiseClipseMessage error = RiseClipseMessage.error( DA_VALIDATION_NSD_CATEGORY, sclModel.getLineNumber(), 
                                                      getSclComponentClassName(), " ", attribute, " is mandatory in ", getSclModelClassName(), " with LNClass LLN0" );
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
            else {
                // ado instanceof SDO
            }
        }
        return res;
    }

    @Override
    protected boolean validateMOln0( DOType sclModel, DiagnosticChain diagnostics ) {
        boolean res = true;
        EList< AbstractDataObject > adoList = sclModel.getReferredByAbstractDataObject();
        for( AbstractDataObject ado : adoList ) {
            if( ado instanceof DO ) {
                DO do_ = ( DO ) ado;
                if( "LLN0".equals( do_.getParentLNodeType().getLnClass() )) {
                    for( String attribute : mandatoryInLLN0ElseForbidden ) {
                        DA da = presentSclComponent.get( attribute );
                        if( da == null ) {
                            RiseClipseMessage error = RiseClipseMessage.error( DA_VALIDATION_NSD_CATEGORY, sclModel.getLineNumber(), 
                                                      getSclComponentClassName(), " ", attribute, " is mandatory in ", getSclModelClassName(), " with LNClass LLN0" );
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
                else {
                    for( String attribute : mandatoryInLLN0ElseForbidden ) {
                        DA da = presentSclComponent.get( attribute );
                        if( da != null ) {
                            RiseClipseMessage error = RiseClipseMessage.error( DA_VALIDATION_NSD_CATEGORY, sclModel.getLineNumber(), 
                                                      getSclComponentClassName(), " ", attribute, " is forbidden in ", getSclModelClassName(), " with LNClass ", do_.getParentLNodeType().getLnClass() );
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
            else {
                // ado instanceof SDO
            }
        }
        return res;
    }

    @Override
    protected boolean validateOMSynPh( DOType sclModel, DiagnosticChain diagnostics ) {
        for( String name : optionalIfPhsRefIsSynchrophasorElseMandatory ) {
            if( presentSclComponent.get( name ) != null ) {
                RiseClipseMessage warning = RiseClipseMessage.warning( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getLineNumber(), 
                                            "verification of PresenceCondition \"OMSynPh\" for", getSclComponentClassName(), " is not implemented in ", getSclModelClassName(), ") with ", getNsdModelClassName(), " ", getNsdModelName() );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        warning.getMessage(),
                        new Object[] { sclModel, warning } ));
            }
        }
        return true;
    }

}
