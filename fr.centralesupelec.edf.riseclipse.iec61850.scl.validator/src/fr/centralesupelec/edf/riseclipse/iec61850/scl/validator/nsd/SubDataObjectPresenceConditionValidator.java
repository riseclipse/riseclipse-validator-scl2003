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
import java.util.Optional;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jdt.annotation.Nullable;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.CDC;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentification;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentificationName;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DA;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DOType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.SDO;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.Val;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.RiseClipseMessage;

public class SubDataObjectPresenceConditionValidator extends GenericPresenceConditionValidator< CDC, DOType, @Nullable SDO >{

    private static final String SDO_SETUP_NSD_CATEGORY      = NsdValidator.SETUP_NSD_CATEGORY      + "/SubDataObject";
    private static final String SDO_VALIDATION_NSD_CATEGORY = NsdValidator.VALIDATION_NSD_CATEGORY + "/SubDataObject";

    private static HashMap< NsIdentificationName, SubDataObjectPresenceConditionValidator > validators = new HashMap<>();
    
    public static SubDataObjectPresenceConditionValidator get( NsIdentification nsIdentification, CDC cdc ) {
        if( ! validators.containsKey( new NsIdentificationName( nsIdentification, cdc.getName() ))) {
            validators.put( new NsIdentificationName( nsIdentification, cdc.getName() ), new SubDataObjectPresenceConditionValidator( nsIdentification, cdc ));
        }
        return validators.get( new NsIdentificationName( nsIdentification, cdc.getName() ) );
    }
    
    private CDC cdc;

    public SubDataObjectPresenceConditionValidator( NsIdentification nsIdentification, CDC cdc ) {
        super( nsIdentification, cdc );
        
        this.cdc = cdc;
    }

    @Override
    protected String getSetupMessageCategory() {
        return SDO_SETUP_NSD_CATEGORY;
    }

    @Override
    protected String getValidationMessageCategory() {
        return SDO_VALIDATION_NSD_CATEGORY;
    }

    @Override
    protected void createSpecifications( CDC cdc ) {
        cdc
        .getSubDataObject()
        .stream()
        .forEach( sda -> addSpecification( sda.getName(), sda.getPresCond(), sda.getPresCondArgs(), sda.getRefersToPresCondArgsDoc(), sda.getLineNumber(), sda.getFilename() )); 
    }

    @Override
    protected String getPresenceConditionValidatorName() {
        return "SubDataObjectPresenceConditionValidator";
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
        return "SubDataObject";
    }

    @Override
    protected String getSclModelClassName() {
        return "DOType";
    }

    @Override
    protected String getSclComponentClassName() {
        return "SDO";
    }

    @Override
    protected boolean validateMFln0( DOType sclModel, DiagnosticChain diagnostics ) {
        for( String name : mandatoryInLLN0ElseForbidden ) {
            if( presentSclComponent.get( name ) != null ) {
                RiseClipseMessage warning = RiseClipseMessage.warning( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getLineNumber(), 
                                            "verification of PresenceCondition \"MFln0\" for ", getSclComponentClassName(), " ", name, " is not implemented in ", getSclModelClassName(), " with ", getNsdModelClassName(), " ", getNsdModelName() );
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

    @Override
    protected boolean validateMOln0( DOType sclModel, DiagnosticChain diagnostics ) {
        for( String name : mandatoryInLLN0ElseOptional ) {
            if( presentSclComponent.get( name ) != null ) {
                RiseClipseMessage warning = RiseClipseMessage.warning( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getLineNumber(), 
                                            "verification of PresenceCondition \"MOln0\" for ", getSclComponentClassName(), " ", name, " is not implemented in ", getSclModelClassName(), " with ", getNsdModelClassName(), " ", getNsdModelName() );
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

    @Override
    protected boolean validateOMSynPh( DOType doType, DiagnosticChain diagnostics ) {
        boolean res = true;
        
        String sdoName = optionalIfPhsRefIsSynchrophasorElseMandatory.stream().findFirst().get();
        boolean phsRefIsSynchrophasor = false;
        Optional< DA > phsRef = doType
                .getDA()
                .stream()
                .filter( da -> "phsRef".equals( da.getName() ))
                .findAny();
        if( phsRef.isPresent() ) {
            EList< Val > vals = phsRef.get().getVal();
            if( vals.size() == 0 ) {
                RiseClipseMessage warning = RiseClipseMessage.warning( SDO_VALIDATION_NSD_CATEGORY, doType.getLineNumber(), 
                                            "verification of PresenceCondition \"OMSynPh\" for SDO ", sdoName, " for DOType: no value for phsRef" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        warning.getMessage(),
                        new Object[] { doType, warning } ));
            }
            else if( vals.size() == 1 ) {
                phsRefIsSynchrophasor = "Synchrophasor".equals( vals.get( 0 ).getValue() );
            }
            else {
                RiseClipseMessage warning = RiseClipseMessage.warning( SDO_VALIDATION_NSD_CATEGORY, doType.getLineNumber(), 
                                            "verification of PresenceCondition \"OMSynPh\" for SDO ", sdoName, " for DOType: multiple values for phsRef" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        warning.getMessage(),
                        new Object[] { doType, warning } ));
            }
        }
        else {
            RiseClipseMessage warning = RiseClipseMessage.warning( SDO_VALIDATION_NSD_CATEGORY, doType.getLineNumber(), 
                                        "verification of PresenceCondition \"OMSynPh\" for SDO ", sdoName, " for DOType: DA phsRef not found" );
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.WARNING,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    warning.getMessage(),
                    new Object[] { doType, warning } ));
        }
        if( ! phsRefIsSynchrophasor ) {
            for( String name : optionalIfPhsRefIsSynchrophasorElseMandatory ) {
                if( presentSclComponent.get( name ) == null ) {
                    RiseClipseMessage error = RiseClipseMessage.error( SDO_VALIDATION_NSD_CATEGORY, doType.getLineNumber(), 
                                              "SDO ", sdoName, " is mandatory in DOType because phsRef is not Synchrophasor" );
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
        return res;
    }

}
