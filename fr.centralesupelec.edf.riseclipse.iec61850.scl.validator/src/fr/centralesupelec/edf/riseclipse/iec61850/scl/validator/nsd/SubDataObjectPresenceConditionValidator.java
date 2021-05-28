/*
*************************************************************************
**  Copyright (c) 2019-2021 CentraleSupélec & EDF.
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

public class SubDataObjectPresenceConditionValidator extends GenericPresenceConditionValidator< CDC, DOType, @Nullable SDO >{

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
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD validation] verification of PresenceCondition \"MFln0\" for " + getSclComponentClassName() + " " + name + " is not implemented in " + getSclModelClassName() + " (line " + sclModel.getLineNumber() + ") with " + getNsdModelClassName() + " " + getNsdModelName(),
                        new Object[] { sclModel } ));
            }
        }
        return true;
    }

    @Override
    protected boolean validateMOln0( DOType sclModel, DiagnosticChain diagnostics ) {
        for( String name : mandatoryInLLN0ElseOptional ) {
            if( presentSclComponent.get( name ) != null ) {
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD validation] verification of PresenceCondition \"MOln0\" for " + getSclComponentClassName() + " " + name + " is not implemented in " + getSclModelClassName() + " (line " + sclModel.getLineNumber() + ") with " + getNsdModelClassName() + " " + getNsdModelName(),
                        new Object[] { sclModel } ));
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
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD validation] verification of PresenceCondition \"OMSynPh\" for SDO " + sdoName + " for DOType (line " + doType.getLineNumber() + "): no value for phsRef",
                        new Object[] { doType } ));
            }
            else if( vals.size() == 1 ) {
                phsRefIsSynchrophasor = "Synchrophasor".equals( vals.get( 0 ).getValue() );
            }
            else {
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD validation] verification of PresenceCondition \"OMSynPh\" for SDO " + sdoName + " for DOType (line " + doType.getLineNumber() + "): multiple values for phsRef",
                        new Object[] { doType } ));
            }
        }
        else {
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.WARNING,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    "[NSD validation] verification of PresenceCondition \"OMSynPh\" for SDO " + sdoName + " for DOType (line " + doType.getLineNumber() + "): DA phsRef not found",
                    new Object[] { doType } ));
        }
        if( ! phsRefIsSynchrophasor ) {
            for( String name : optionalIfPhsRefIsSynchrophasorElseMandatory ) {
                if( presentSclComponent.get( name ) == null ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] SDO " + name + " is mandatory in DOType (line " + doType.getLineNumber() + ") because phsRef is not Synchrophasor",
                            new Object[] { doType } ));
                    res = false;
                }
            }
        }
        return res;
    }

}
