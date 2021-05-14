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
import java.util.stream.Stream;

import org.eclipse.emf.common.util.DiagnosticChain;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.BasicType;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.ConstructedAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.Enumeration;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AbstractDataAttribute;

public abstract class TypeValidator {

    private static HashMap< String, TypeValidator > validators = new HashMap<>();
    
    public static TypeValidator get( String name ) {
        if( validators == null ) return null;
        return validators.get( name );
    }
    
    public static void buildValidators( Stream< BasicType > basicTypeStream, Stream< Enumeration > enumerationStream, Stream< ConstructedAttribute > constructedAttributeStream ) {
        basicTypeStream
        .forEach( basicType -> validators.put( basicType.getName(), BasicTypeValidator.get( basicType )));
        
        enumerationStream
        .forEach( enumeration -> validators.put( enumeration.getName(), new EnumerationValidator( enumeration )));
        
        constructedAttributeStream
        .forEach( contructedAttribute -> validators.put( contructedAttribute.getName(), new ConstructedAttributeValidator( contructedAttribute )));
    }

    public abstract boolean validateAbstractDataAttribute( AbstractDataAttribute ada, DiagnosticChain diagnostics );

    /*
     * Called before another file is validated
     */
    public static void resetValidators() {
        validators.values().stream().forEach( v -> v.reset() );
    }

    public abstract void reset();
}
