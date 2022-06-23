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
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.jdt.annotation.Nullable;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.ConstructedAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentification;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentificationName;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.BDA;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DAType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.RiseClipseMessage;

public class SubDataAttributePresenceConditionValidator extends GenericPresenceConditionValidator< ConstructedAttribute, DAType, @Nullable BDA >{
    
    private static final String SDA_SETUP_NSD_CATEGORY      = NsdValidator.SETUP_NSD_CATEGORY      + "/SubDataAttribute";
    private static final String SDA_VALIDATION_NSD_CATEGORY = NsdValidator.VALIDATION_NSD_CATEGORY + "/SubDataAttribute";

    private static IdentityHashMap< NsIdentificationName, SubDataAttributePresenceConditionValidator > validators = new IdentityHashMap<>();
    
    public static SubDataAttributePresenceConditionValidator get( NsIdentification nsIdentification, ConstructedAttribute constructedAttribute ) {
        // TODO: do we need to use dependsOn links?
        if( ! validators.containsKey( NsIdentificationName.of( nsIdentification, constructedAttribute.getName() ))) {
            validators.put( NsIdentificationName.of( nsIdentification, constructedAttribute.getName() ), new SubDataAttributePresenceConditionValidator( nsIdentification, constructedAttribute ));
        }
        return validators.get( NsIdentificationName.of( nsIdentification, constructedAttribute.getName() ) );
    }
    
    public SubDataAttributePresenceConditionValidator( NsIdentification nsIdentification, ConstructedAttribute constructedAttribute ) {
        super( nsIdentification, constructedAttribute );

        initialize();
    }
    
    @Override
    protected String getSetupMessageCategory() {
        return SDA_SETUP_NSD_CATEGORY;
    }

    @Override
    protected String getValidationMessageCategory() {
        return SDA_VALIDATION_NSD_CATEGORY;
    }

    @Override
    protected void createSpecifications() {
        nsdModel
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
        return nsdModel.getName();
    }

    @Override
    protected int getNsdModelLineNumber() {
        return nsdModel.getLineNumber();
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
                RiseClipseMessage warning = RiseClipseMessage.warning( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                            "verification of PresenceCondition \"MFln0\" for ", getSclComponentClassName(), " ", name, " is not implemented in ",
                                            getSclModelClassName(), " with ", getNsdModelClassName(), " ", getNsdModelName(), " at line ", getNsdModelLineNumber(),
                                            " in namespace \"", nsIdentification, "\"" );
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
    protected boolean validateMOln0( DAType sclModel, DiagnosticChain diagnostics ) {
        for( String name : mandatoryInLLN0ElseOptional ) {
            if( presentSclComponent.get( name ) != null ) {
                RiseClipseMessage warning = RiseClipseMessage.warning( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                            "verification of PresenceCondition \"MOln0\" for ", getSclComponentClassName(), " ", name, " is not implemented in ",
                                            getSclModelClassName(), " with ", getNsdModelClassName(), " ", getNsdModelName(), " at line ", getNsdModelLineNumber(),
                                            " in namespace \"", nsIdentification, "\"" );
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
    protected boolean validateOMSynPh( DAType sclModel, DiagnosticChain diagnostics ) {
        for( String name : optionalIfPhsRefIsSynchrophasorElseMandatory ) {
            if( presentSclComponent.get( name ) != null ) {
                RiseClipseMessage warning = RiseClipseMessage.warning( NsdValidator.NOTIMPLEMENTED_NSD_CATEGORY, sclModel.getFilename(), sclModel.getLineNumber(), 
                                            "verification of PresenceCondition \"OMSynPh\" for ", getSclComponentClassName(), " ", name, " is not implemented in ",
                                            getSclModelClassName(), " with ", getNsdModelClassName(), " ", getNsdModelName(), " at line ", getNsdModelLineNumber(),
                                            " in namespace \"", nsIdentification, "\"" );
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
