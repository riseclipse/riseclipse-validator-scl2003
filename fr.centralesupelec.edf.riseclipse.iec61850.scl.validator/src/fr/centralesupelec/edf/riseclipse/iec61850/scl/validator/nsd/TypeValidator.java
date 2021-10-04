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
import java.util.stream.Stream;

import org.eclipse.emf.common.util.DiagnosticChain;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.BasicType;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.ConstructedAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.Enumeration;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentification;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentificationName;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AbstractDataAttribute;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;

public abstract class TypeValidator {

    private static HashMap< NsIdentificationName, TypeValidator > validators = new HashMap<>();
    
    public static TypeValidator get( NsIdentification nsIdentification, String typeName ) {
        if( validators == null ) return null;
        return validators.get( new NsIdentificationName( nsIdentification, typeName ));
    }
    
    public static void buildBasicTypeValidators( NsIdentification nsIdentification, Stream< BasicType > basicTypeStream, IRiseClipseConsole console ) {
        basicTypeStream
        .forEach( basicType -> {
            NsIdentificationName nsId = new NsIdentificationName( nsIdentification, basicType.getName() );
            if( validators.get( nsId ) != null ) {
                console.warning( "[NSD setup] (" + basicType.getFilename() + ":" + basicType.getLineNumber() + ") BasicType " + basicType.getName() + " has already a validator in namespace \""
                        + nsIdentification + "\", it will be overwritten" );
            }
            else {
                console.verbose( "[NSD setup] (" + basicType.getFilename() + ":" + basicType.getLineNumber() + ") adding validator for BasicType " + basicType.getName() + " in namespace \""
                        + nsIdentification + "\"" );
            }
            // BasicTypes are predefined
            validators.put( nsId, BasicTypeValidator.get( basicType ));
        });
    }

    public static void builEnumerationdValidators( NsIdentification nsIdentification, Stream< Enumeration > enumerationStream, IRiseClipseConsole console ) {
        enumerationStream
        .forEach( enumeration -> {
            NsIdentificationName nsId = new NsIdentificationName( nsIdentification, enumeration.getName() );
            if( validators.get( nsId ) != null ) {
                console.warning( "[NSD setup] (" + enumeration.getFilename() + ":" + enumeration.getLineNumber() + ") Enumeration " + enumeration.getName() + " has already a validator in namespace \""
                        + nsIdentification + "\", it will be overwritten" );
            }
            else {
                console.verbose( "[NSD setup] (" + enumeration.getFilename() + ":" + enumeration.getLineNumber() + ") adding validator for Enumeration " + enumeration.getName() + " in namespace \""
                        + nsIdentification + "\"" );
            }
            validators.put( nsId, new EnumerationValidator( enumeration, nsIdentification, console ));
        });
    }

    public static void buildConstructedAttributeValidators( NsIdentification nsIdentification, Stream< ConstructedAttribute > constructedAttributeStream, IRiseClipseConsole console ) {
        constructedAttributeStream
        .forEach( constructedAttribute -> buildConstructedAttributeValidator( nsIdentification, constructedAttribute, console ));
    }

    // A ConstructedAttribute may use another one whose validator has not yet being built
    public static TypeValidator buildConstructedAttributeValidator( NsIdentification nsIdentification, ConstructedAttribute constructedAttribute, IRiseClipseConsole console ) {
        NsIdentificationName nsId = new NsIdentificationName( nsIdentification, constructedAttribute.getName() );
        if( validators.get( nsId ) != null ) {
            console.warning( "[NSD setup] (" + constructedAttribute.getFilename() + ":" + constructedAttribute.getLineNumber() + ") ConstructedAttribute " + constructedAttribute.getName() + " has already a validator in namespace \""
                    + nsIdentification + "\", it will be overwritten" );
        }
        else {
            console.verbose( "[NSD setup] (" + constructedAttribute.getFilename() + ":" + constructedAttribute.getLineNumber() + ") adding validator for ConstructedAttribute " + constructedAttribute.getName() + " in namespace \""
                    + nsIdentification + "\"" );
        }
        ConstructedAttributeValidator validator = new ConstructedAttributeValidator( nsIdentification, constructedAttribute, console );
        validators.put( nsId, validator );
        return validator;
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
