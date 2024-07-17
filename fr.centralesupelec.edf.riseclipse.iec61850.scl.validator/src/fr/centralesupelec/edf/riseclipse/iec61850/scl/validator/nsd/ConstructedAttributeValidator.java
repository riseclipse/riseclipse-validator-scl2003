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
import java.util.HashSet;

import org.apache.commons.lang3.tuple.Pair;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.jdt.annotation.NonNull;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.ConstructedAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.NsdObject;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.ServiceConstructedAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.SubDataAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentification;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AbstractDataAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.BDA;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DAType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.RiseClipseMessage;

public class ConstructedAttributeValidator extends TypeValidator {

    static final String CA_SETUP_NSD_CATEGORY      = NsdValidator.SETUP_NSD_CATEGORY      + "/ConstructedAttribute";
    static final String CA_VALIDATION_NSD_CATEGORY = NsdValidator.VALIDATION_NSD_CATEGORY + "/ConstructedAttribute";

    private HashSet< String > validatedDAType; 

    private SubDataAttributePresenceConditionValidator subDataAttributePresenceConditionValidator;
    private HashMap< String, TypeValidator > subDataAttributeValidatorMap = new HashMap<>();
    private HashMap< String, String > subDataAttributeUnknownTypeMap = new HashMap<>();

    private NsIdentification nsIdentification;
    private ConstructedAttribute constructedAttribute;

    public ConstructedAttributeValidator( NsIdentification nsIdentification, ConstructedAttribute constructedAttribute, IRiseClipseConsole console ) {
        console.debug( CA_SETUP_NSD_CATEGORY, constructedAttribute.getLineNumber(),
                "ConstructedAttributeValidator( ", constructedAttribute.getName(), " ) in namespace \"", nsIdentification, "\"" );

        this.nsIdentification = nsIdentification;
        this.constructedAttribute = constructedAttribute;
        this.subDataAttributePresenceConditionValidator = SubDataAttributePresenceConditionValidator.get( nsIdentification, constructedAttribute );
        
        for( SubDataAttribute sda : constructedAttribute.getSubDataAttribute() ) {
            if( sda.getType() == null ) {
                if(( !( constructedAttribute instanceof ServiceConstructedAttribute )) || (! (( ServiceConstructedAttribute ) constructedAttribute ).isTypeKindParameterized() )) {
                    console.warning( CA_SETUP_NSD_CATEGORY, sda.getFilename(), sda.getLineNumber(),
                                 "type not specified for SubDataAttribute ", sda.getName() );
                }
                continue;
            }
            NsdObject type = sda.getRefersToBasicType();
            if( type == null ) {
                type = sda.getRefersToEnumeration();
            }
            if( type == null ) {
                type = sda.getRefersToConstructedAttribute();
            }
            if( type == null ) {
                console.warning( CA_SETUP_NSD_CATEGORY, sda.getFilename(), sda.getLineNumber(),
                        "Type ", sda.getType(), " not found for SubDataAttribute ", sda.getName() );
                continue;
            }
            Pair< TypeValidator, NsIdentification > res = TypeValidator.get( this.nsIdentification, type );
            TypeValidator typeValidator = res.getLeft();
            // The type of the SubDataAttribute may be a ConstructedAttribute whose validator is not yet built
            if(( typeValidator == null ) && ( sda.getRefersToConstructedAttribute() != null )) {
                console.notice( CA_SETUP_NSD_CATEGORY, sda.getFilename(), sda.getLineNumber(),
                              "Validator for ConstructedAttribute ", constructedAttribute.getName(),
                              " needs validator for SubDataAttribute ", sda.getName(), " of type ", sda.getType(), " which is not yet built" );
                typeValidator = TypeValidator.buildConstructedAttributeValidator( this.nsIdentification, sda.getRefersToConstructedAttribute(), console );
            }
            if( typeValidator != null ) {
                // Up to 1.2.6, the namespace of the found TypeValidator (res.getRight()) was used here in the key (using an NsIdentificationObject)
                // No comment about this choice, and I don't see any reason for
                // So we only use the SubDataAttribute name as key
                subDataAttributeValidatorMap.put( sda.getName(), typeValidator );
            }
            else {
                console.warning( CA_SETUP_NSD_CATEGORY, sda.getFilename(), sda.getLineNumber(),
                                 "Type ", sda.getType(), " not found for SubDataAttribute ", sda.getName() );
                subDataAttributeUnknownTypeMap.put( sda.getName(), sda.getType() );
            }
        }
        
        reset();
    }

    /*
     * Called before another file is validated
     */
    @Override
    public void reset() {
        validatedDAType = new HashSet<>();
        
        subDataAttributeValidatorMap.values().stream().forEach( v -> v.reset() );
    }

    @Override
    public boolean validateAbstractDataAttribute( AbstractDataAttribute da, DiagnosticChain diagnostics ) {
        @NonNull
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.debug( CA_VALIDATION_NSD_CATEGORY, da.getLineNumber(),
                       "ConstructedAttributeValidator.validateAbstractDataAttribute( ", da.getName(), " ) in namespace \"", nsIdentification, "\"" );
        boolean res = true;
        
        if( da.getRefersToDAType() != null ) {
            res = validateDAType( da.getRefersToDAType(), diagnostics ) && res;
        }
        return res;
    }

    private boolean validateDAType( DAType daType, DiagnosticChain diagnostics ) {
        if( validatedDAType.contains( daType.getId() )) return true;
        @NonNull
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.debug( CA_VALIDATION_NSD_CATEGORY, daType.getLineNumber(),
                       "ConstructedAttributeValidator.validateDAType( ", daType.getId(), " ) in namespace \"", nsIdentification, "\"" );
        validatedDAType.add( daType.getId() );
        
        if( constructedAttribute.isDeprecated() ) {
            RiseClipseMessage warning = RiseClipseMessage.warning( CA_VALIDATION_NSD_CATEGORY, daType.getFilename(), daType.getLineNumber(), 
                    "DAType id = \"", daType.getId(), " refers to deprecated ConstructedAttribute \"", constructedAttribute.getName(), "\" in namespace \"", nsIdentification, "\"" );
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.WARNING,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    warning.getMessage(),
                    new Object[] { daType, warning } ));
        }
        
        subDataAttributePresenceConditionValidator.resetModelData();
        
        daType
        .getBDA()
        .stream()
        .forEach( bda -> subDataAttributePresenceConditionValidator.addModelData( bda, bda.getName(), diagnostics ));
      
        boolean res = subDataAttributePresenceConditionValidator.validate( daType, diagnostics );
        
        for( BDA bda : daType.getBDA() ) {
            TypeValidator typeValidator = subDataAttributeValidatorMap.get( bda.getName() );
            if( typeValidator != null ) {
                typeValidator.validateAbstractDataAttribute( bda, diagnostics );
            }
            else {
                // if BDA not allowed, error will be reported by PresenceConditionValidator
                // if BDA has unknown type, tell it
                String ofType = "";
                if( subDataAttributeUnknownTypeMap.containsKey( bda.getName() )) {
                    ofType = "\" of type \"" + subDataAttributeUnknownTypeMap.get( bda.getName() );
                }
                RiseClipseMessage warning = RiseClipseMessage.warning( CA_VALIDATION_NSD_CATEGORY, daType.getFilename(), daType.getLineNumber(), 
                        "while validating DAType: validator for BDA \"", bda.getName(), ofType, "\" not found in namespace \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        warning.getMessage(),
                        new Object[] { daType, warning } ));
            }
        }
      
        return res;
    }

    @Override
    protected String getName() {
        return constructedAttribute.getName();
    }

}
