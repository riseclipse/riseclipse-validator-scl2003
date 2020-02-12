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
import java.util.HashSet;

import org.eclipse.emf.common.util.DiagnosticChain;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.ConstructedAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.SubDataAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AbstractDataAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.BDA;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DAType;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;

public class ConstructedAttributeValidator extends TypeValidator {

    public static void initialize() {
        SubDataAttributePresenceConditionValidator.initialize();
    }
    
    private HashSet< String > validatedDAType; 

    private SubDataAttributePresenceConditionValidator subDataAttributePresenceConditionValidator;
    private HashMap< String, TypeValidator > subDataAttributeValidatorMap = new HashMap<>();

    public ConstructedAttributeValidator( ConstructedAttribute contructedAttribute ) {
        subDataAttributePresenceConditionValidator = SubDataAttributePresenceConditionValidator.get( contructedAttribute );
        
        for( SubDataAttribute sda : contructedAttribute.getSubDataAttribute() ) {
            TypeValidator validator = TypeValidator.get( sda.getType() );
            if( validator != null ) {
                subDataAttributeValidatorMap.put( sda.getName(), validator );
            }
            else {
                AbstractRiseClipseConsole.getConsole().warning( "[NSD setup] (" + sda.getFilename() + ":" + sda.getLineNumber() + ") Type not found for DataAttribute " + sda.getName() );
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
        AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] ConstructedAttributeValidator.validateAbstractDataAttribute( " + da.getName() + " ) at line " + da.getLineNumber() );
        boolean res = true;
        
        if( da.getRefersToDAType() != null ) {
            res = validateDAType( da.getRefersToDAType(), diagnostics ) && res;
        }
        return res;
    }

    private boolean validateDAType( DAType daType, DiagnosticChain diagnostics ) {
        if( validatedDAType.contains( daType.getId() )) return true;
        AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] ConstructedAttributeValidator.validateDAType( " + daType.getId() + " ) at line " + daType.getLineNumber() );
        validatedDAType.add( daType.getId() );
        
        subDataAttributePresenceConditionValidator.resetModelData();
        
        daType
        .getBDA()
        .stream()
        .forEach( bda -> subDataAttributePresenceConditionValidator.addModelData( bda, bda.getName(), diagnostics ));
      
        boolean res = subDataAttributePresenceConditionValidator.validate( daType, diagnostics );
        
        for( BDA bda : daType.getBDA() ) {
            TypeValidator validator = subDataAttributeValidatorMap.get( bda.getName() );
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
