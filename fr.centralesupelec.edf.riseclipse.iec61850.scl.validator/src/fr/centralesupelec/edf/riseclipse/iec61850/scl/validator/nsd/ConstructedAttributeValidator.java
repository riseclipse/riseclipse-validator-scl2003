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

import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.jdt.annotation.NonNull;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.ConstructedAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.SubDataAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentification;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentificationName;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsdResourceSetImpl;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AbstractDataAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.BDA;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DAType;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;

public class ConstructedAttributeValidator extends TypeValidator {

    private HashSet< String > validatedDAType; 

    private SubDataAttributePresenceConditionValidator subDataAttributePresenceConditionValidator;
    private HashMap< NsIdentificationName, TypeValidator > subDataAttributeValidatorMap = new HashMap<>();

    private NsIdentification nsIdentification;

    public ConstructedAttributeValidator( NsIdentification nsIdentification, ConstructedAttribute constructedAttribute, IRiseClipseConsole console ) {
        this.nsIdentification = nsIdentification;
        subDataAttributePresenceConditionValidator = SubDataAttributePresenceConditionValidator.get( nsIdentification, constructedAttribute );
        
        for( SubDataAttribute sda : constructedAttribute.getSubDataAttribute() ) {
            if( sda.getType() == null ) {
                console.warning( NsdValidator.SETUP_NSD_CATEGORY, sda.getFilename(), sda.getLineNumber(),
                                 "type not specified for SubDataAttribute ", sda.getName() );
                continue;
            }
            // When we look for a validator, we first search in the current namespace.
            // If this fails, we use DependsOn links
            TypeValidator typeValidator = null;
            // Reinitialize before searching
            nsIdentification = this.nsIdentification;
            while( nsIdentification != null ) {
                typeValidator = TypeValidator.get( nsIdentification, sda.getType() );
                if( typeValidator != null ) break;
                if(( sda.eResource() != null ) && ( sda.eResource().getResourceSet() instanceof NsdResourceSetImpl )) {
                    nsIdentification = (( NsdResourceSetImpl ) sda.eResource().getResourceSet() ).getDependsOn( nsIdentification );
                }
                else {
                    break;
                }
            }
            // The type of the SubDataAttribute may be a ConstructedAttribute whose validator is not yet built
            if(( typeValidator == null ) && ( sda.getRefersToConstructedAttribute() != null )) {
                console.info( NsdValidator.SETUP_NSD_CATEGORY, sda.getFilename(), sda.getLineNumber(),
                              "Validator for ConstructedAttribute ", constructedAttribute.getName(),
                              " needs validator for SubDataAttribute ", sda.getName(), " of type ", sda.getType(), " which is not yet built" );
                typeValidator = TypeValidator.buildConstructedAttributeValidator( this.nsIdentification, sda.getRefersToConstructedAttribute(), console );
            }
            if( typeValidator != null ) {
                subDataAttributeValidatorMap.put( new NsIdentificationName( this.nsIdentification, sda.getName() ), typeValidator );
            }
            else {
                console.warning( NsdValidator.SETUP_NSD_CATEGORY, sda.getFilename(), sda.getLineNumber(),
                                 "Type not found for SubDataAttribute ", sda.getName() );
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
        console.debug( NsdValidator.VALIDATION_NSD_CATEGORY, da.getLineNumber(),
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
        console.debug( NsdValidator.VALIDATION_NSD_CATEGORY, daType.getLineNumber(),
                       "ConstructedAttributeValidator.validateDAType( ", daType.getId(), " ) in namespace \"", nsIdentification, "\"" );
        validatedDAType.add( daType.getId() );
        
        subDataAttributePresenceConditionValidator.resetModelData();
        
        daType
        .getBDA()
        .stream()
        .forEach( bda -> subDataAttributePresenceConditionValidator.addModelData( bda, bda.getName(), diagnostics ));
      
        boolean res = subDataAttributePresenceConditionValidator.validate( daType, diagnostics );
        
        for( BDA bda : daType.getBDA() ) {
            TypeValidator validator = subDataAttributeValidatorMap.get( new NsIdentificationName( nsIdentification, bda.getName() ) );
            if( validator != null ) {
                validator.validateAbstractDataAttribute( bda, diagnostics );
            }
            else {
                // BDA not allowed, error will be reported by PresenceConditionValidator
                //AbstractRiseClipseConsole.getConsole().warning( "[NSD validation] while validating DAType (line " + daType.getLineNumber() + "): validator for BDA " + bda.getName() + " not found" );
            }
        }
      
        return res;
    }

}
