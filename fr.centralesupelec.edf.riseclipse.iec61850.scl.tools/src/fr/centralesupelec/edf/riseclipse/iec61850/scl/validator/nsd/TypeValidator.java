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
import java.util.stream.Stream;

import org.eclipse.emf.common.util.DiagnosticChain;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.BasicType;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.ConstructedAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.Enumeration;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DA;

public abstract class TypeValidator {

    private static HashMap< String, TypeValidator > validators = new HashMap<>();
    
    public static TypeValidator get( String name ) {
        return validators.get( name );
    }
    
    public static void buildValidators( Stream< BasicType > basicTypeStream, Stream< Enumeration > enumerationStream, Stream< ConstructedAttribute > constructedAttributeStream ) {
        basicTypeStream
        .forEach( basicType -> validators.put( basicType.getName(), BasicTypeValidator.get( basicType )));
        enumerationStream
        .forEach( enumeration -> validators.put( enumeration.getName(), new EnumerationValidator( enumeration )));
    }

    public abstract boolean validateDA( DA da, DiagnosticChain diagnostics );
}
