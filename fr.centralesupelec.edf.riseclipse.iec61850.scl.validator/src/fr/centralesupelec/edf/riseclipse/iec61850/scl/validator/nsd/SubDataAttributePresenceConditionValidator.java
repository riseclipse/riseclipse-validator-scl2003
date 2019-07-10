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
**      http://wdi.supelec.fr/software/RiseClipse/
*************************************************************************
*/
package fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.nsd;

import java.util.HashMap;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.jdt.annotation.Nullable;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.ConstructedAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.BDA;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DAType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;

public class SubDataAttributePresenceConditionValidator extends GenericPresenceConditionValidator< ConstructedAttribute, DAType, @Nullable BDA >{
    
    private static HashMap< String, SubDataAttributePresenceConditionValidator > validators;
    
    public static void initialize() {
        validators = new HashMap<>();
    }
    
    public static SubDataAttributePresenceConditionValidator get( ConstructedAttribute constructedAttribute ) {
        if( ! validators.containsKey( constructedAttribute.getName() )) {
            validators.put( constructedAttribute.getName(), new SubDataAttributePresenceConditionValidator( constructedAttribute ));
        }
        return validators.get( constructedAttribute.getName() );
    }
    
    private ConstructedAttribute constructedAttribute;
    
    public SubDataAttributePresenceConditionValidator( ConstructedAttribute constructedAttribute ) {
        super( constructedAttribute );

        this.constructedAttribute = constructedAttribute;
    }
    
    @Override
    protected void createSpecifications( ConstructedAttribute constructedAttribute ) {
        constructedAttribute
        .getSubDataAttribute()
        .stream()
        .forEach( sda -> addSpecification( sda.getName(), sda.getPresCond(), sda.getPresCondArgs(), sda.getRefersToPresCondArgsDoc(), sda.getLineNumber(), sda.getFilename() )); 
    }
    
    @Override
    protected String getPresenceConditionValidatorName() {
        return "SubDataAttributePresenceConditionValidator";
    }

    @Override
    protected String getNsdModelName() {
        return constructedAttribute.getName();
    }

    @Override
    protected int getNsdModelLineNumber() {
        return constructedAttribute.getLineNumber();
    }

    @Override
    protected String getNsdModelClassName() {
        return "ConstructedAttribute";
    }

    @Override
    protected String getNsdComponentClassName() {
        return "SubDataAttribute";
    }

    @Override
    protected String getSclModelClassName() {
        return "DAType";
    }

    @Override
    protected String getSclComponentClassName() {
        return "BDA";
    }

    @Override
    protected boolean validateMFln0( DAType sclModel, DiagnosticChain diagnostics ) {
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
    protected boolean validateMOln0( DAType sclModel, DiagnosticChain diagnostics ) {
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
    protected boolean validateOMSynPh( DAType sclModel, DiagnosticChain diagnostics ) {
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
