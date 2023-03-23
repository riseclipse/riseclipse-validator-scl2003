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
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import org.eclipse.emf.common.util.DiagnosticChain;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.BasicType;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.ConstructedAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.Enumeration;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.NsdObject;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentification;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentificationObject;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AbstractDataAttribute;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;

public abstract class TypeValidator {

    // ServiceConstructedAttribute may be parameterized, therefore the name is not an identifier
    // private static IdentityHashMap< NsIdentificationName, TypeValidator > validators = new IdentityHashMap<>();
    private static IdentityHashMap< NsIdentificationObject, TypeValidator > validators = new IdentityHashMap<>();
    
    public static Pair< TypeValidator, NsIdentification > get( NsIdentification nsIdentification, NsdObject type ) {
        NsIdentification nsId = nsIdentification;
        TypeValidator typeValidator = null;
        while(( typeValidator == null ) && ( nsId != null )) {
            typeValidator = validators.get( NsIdentificationObject.of( nsId, type ));
            nsIdentification = nsId;
            nsId = nsId.getDependsOn();
        }
        return Pair.of( typeValidator, nsIdentification );
    }
    
    public static Pair< TypeValidator, NsIdentification > getByName( NsIdentification nsIdentification, String typeName ) {
        NsIdentification nsId = nsIdentification;
        while( nsId != null ) {
            for( TypeValidator validator : validators.values() ) {
                if( validator.getName().equals( typeName ))
                    return Pair.of( validator, nsIdentification );
            }
            nsIdentification = nsId;
            nsId = nsId.getDependsOn();
        }
        return Pair.of( null, nsIdentification );
    }
    
    protected abstract String getName();

    public static void buildBasicTypeValidators( NsIdentification nsIdentification, Stream< BasicType > basicTypeStream, IRiseClipseConsole console ) {
        basicTypeStream
        .forEach( basicType -> {
            NsIdentificationObject nsId = NsIdentificationObject.of( nsIdentification, basicType );
            if( validators.get( nsId ) != null ) {
                console.warning( BasicTypeValidator.BASIC_TYPE_SETUP_NSD_CATEGORY, basicType.getFilename(), basicType.getLineNumber(),
                                 "BasicType ", basicType.getName(), " has already a validator in namespace \"",
                                 nsIdentification, "\", it will be overwritten" );
            }
            else {
                console.debug( BasicTypeValidator.BASIC_TYPE_SETUP_NSD_CATEGORY, basicType.getFilename(), basicType.getLineNumber(),
                                "Adding validator for BasicType ", basicType.getName(), " in namespace \"",
                                nsIdentification, "\"" );
            }
            // BasicTypes are predefined
            validators.put( nsId, BasicTypeValidator.get( basicType ));
        });
    }

    public static void builEnumerationdValidators( NsIdentification nsIdentification, Stream< Enumeration > enumerationStream, IRiseClipseConsole console ) {
        enumerationStream
        .forEach( enumeration -> {
            NsIdentificationObject nsId = NsIdentificationObject.of( nsIdentification, enumeration );
            if( validators.get( nsId ) != null ) {
                console.warning( EnumerationValidator.ENUMERATION_SETUP_NSD_CATEGORY, enumeration.getFilename(), enumeration.getLineNumber(),
                                 "Enumeration ", enumeration.getName(), " has already a validator in namespace \"",
                                 nsIdentification, "\", it will be overwritten" );
            }
            else {
                console.debug( EnumerationValidator.ENUMERATION_SETUP_NSD_CATEGORY, enumeration.getFilename(), enumeration.getLineNumber(),
                                "Adding validator for Enumeration ", enumeration.getName(), " in namespace \"",
                                nsIdentification, "\"" );
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
        NsIdentificationObject nsId = NsIdentificationObject.of( nsIdentification, constructedAttribute );
        if( validators.get( nsId ) != null ) {
            // The usual case is when it has been built because used as the type of a SubDataAttribute
            console.notice( ConstructedAttributeValidator.CA_SETUP_NSD_CATEGORY, constructedAttribute.getFilename(), constructedAttribute.getLineNumber(),
                             "ConstructedAttribute ", constructedAttribute.getName(), " has already a validator in namespace \"",
                             nsIdentification, "\", it will be overwritten" );
        }
        else {
            console.debug( ConstructedAttributeValidator.CA_SETUP_NSD_CATEGORY, constructedAttribute.getFilename(), constructedAttribute.getLineNumber(),
                            "Adding validator for ConstructedAttribute ", constructedAttribute.getName(), " in namespace \"",
                            nsIdentification, "\"" );
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
