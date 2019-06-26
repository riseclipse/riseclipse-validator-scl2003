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

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jdt.annotation.Nullable;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.CDC;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AbstractDataObject;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DA;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DO;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DOType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;

public class DataAttributePresenceConditionValidator extends GenericPresenceConditionValidator< CDC, DOType, @Nullable DA >{

    private static HashMap< String, DataAttributePresenceConditionValidator > validators = new HashMap<>();
    
    public static DataAttributePresenceConditionValidator get( CDC cdc ) {
        if( ! validators.containsKey( cdc.getName() )) {
            validators.put( cdc.getName(), new DataAttributePresenceConditionValidator( cdc ));
        }
        return validators.get( cdc.getName() );
    }
    
    private CDC cdc;

    public DataAttributePresenceConditionValidator( CDC cdc ) {
        super( cdc );
        
        this.cdc = cdc;
    }

    @Override
    protected void createSpecifications( CDC cdc ) {
        cdc
        .getDataAttribute()
        .stream()
        .forEach( da -> addSpecification( da.getName(), da.getPresCond(), da.getPresCondArgs(), da.getRefersToPresCondArgsDoc() ));
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
                            diagnostics.add( new BasicDiagnostic(
                                    Diagnostic.ERROR,
                                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                    0,
                                    "[NSD validation] " + getSclComponentClassName() + " " + attribute + " is mandatory in " + getSclModelClassName() + " (line " + sclModel.getLineNumber() + ") with LNClass LLN0",
                                    new Object[] { sclModel } ));
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
                            diagnostics.add( new BasicDiagnostic(
                                    Diagnostic.ERROR,
                                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                    0,
                                    "[NSD validation] " + getSclComponentClassName() + " " + attribute + " is mandatory in " + getSclModelClassName() + " (line " + sclModel.getLineNumber() + ") with LNClass LLN0",
                                    new Object[] { sclModel } ));
                            res = false;
                        }
                    }
                }
                else {
                    for( String attribute : mandatoryInLLN0ElseForbidden ) {
                        DA da = presentSclComponent.get( attribute );
                        if( da != null ) {
                            diagnostics.add( new BasicDiagnostic(
                                    Diagnostic.ERROR,
                                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                    0,
                                    "[NSD validation] " + getSclComponentClassName() + " " + attribute + " is forbidden in " + getSclModelClassName() + " (line " + sclModel.getLineNumber() + ") with LNClass " + do_.getParentLNodeType().getLnClass(),
                                    new Object[] { sclModel } ));
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
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD validation] verification of PresenceCondition \"OMSynPh\" for " + getSclComponentClassName() + " " + name + " is not implemented in " + getSclModelClassName() + " (line " + sclModel.getLineNumber() + ") with " + getNsdModelClassName() + " " + getNsdModelName(),
                        new Object[] { sclModel } ));
            }
        }
        return true;
    }

}
