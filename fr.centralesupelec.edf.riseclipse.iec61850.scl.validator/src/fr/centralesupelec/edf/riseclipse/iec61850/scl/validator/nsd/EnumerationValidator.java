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
import java.util.Optional;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.jdt.annotation.NonNull;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.Enumeration;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AbstractDataAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DAI;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.EnumType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.EnumVal;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.UnNaming;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.Val;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.RiseClipseMessage;

public class EnumerationValidator extends TypeValidator {
    
    public static void initialize() {
        // Nothing here
    }
    
    private HashSet< String > validatedEnumType;

    // Name of EnumVal may be empty, so we use LiteralVal as key
    private HashMap< Integer, String > literals = new HashMap<>();
    private String name;
    private boolean isMultiplierKind;

    public EnumerationValidator( Enumeration enumeration ) {
        this.name = enumeration.getName();
        String inheritedFromName = enumeration.getInheritedFrom();
        
        if(( inheritedFromName != null ) && ( ! inheritedFromName.isEmpty() )) {
            TypeValidator inheritedValidator = TypeValidator.get( inheritedFromName );
            if(( inheritedValidator != null ) && ( inheritedValidator instanceof EnumerationValidator )) {
                EnumerationValidator inheritedFrom = ( EnumerationValidator ) inheritedValidator;
                literals.putAll( inheritedFrom.literals );
            }
            else {
                @NonNull
                IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
                console.error( NsdValidator.SETUP_NSD_CATEGORY, 0,
                               "validator for inherited enumeration \"", inheritedFromName, "\" not found" );
            }
        }
        
        // TODO: check for duplicate values
         enumeration
        .getLiteral()
        .stream()
        .forEach( e -> literals.put( e.getLiteralVal(), e.getName() ));
        
        // the positive range of values is reserved for standardized value of enumerations,
        // except for the IEC 61850-7-3 multiplierKind that standardizes also values in the negative range,
        isMultiplierKind = "multiplierKind".equals( getName() );
        
        reset();
    }
    
    public String getName() {
        return name;
    }
    
    /*
     * Called before another file is validated
     */
    @Override
    public void reset() {
        validatedEnumType = new HashSet<>();
    }

    @Override
    public boolean validateAbstractDataAttribute( AbstractDataAttribute ada, DiagnosticChain diagnostics ) {
        @NonNull
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.verbose( NsdValidator.VALIDATION_NSD_CATEGORY, ada.getLineNumber(),
                         "EnumerationValidator.validateAbstractDataAttribute( ", ada.getName(), " )" );
        
        boolean res = true;
        if( ! "Enum".equals(  ada.getBType() )) {
            RiseClipseMessage error = RiseClipseMessage.error( NsdValidator.VALIDATION_NSD_CATEGORY, ada.getLineNumber(), 
                                      "bType of DA/BDA \"", ada.getName(), "\" is not Enum" );
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    error.getMessage(),
                    new Object[] { ada, error } ));
            res = false;
        }

        // Name may differ
//        if( ! getName().equals( ada.getType() )) {
//            diagnostics.add( new BasicDiagnostic(
//                    Diagnostic.ERROR,
//                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
//                    0,
//                    "[NSD validation] type of DA/BDA \"" + ada.getName() + "\" (line = " + ada.getLineNumber() + ") is not " + getName(),
//                    new Object[] { ada } ));
//            res = false;
//        }

        if( ada.getRefersToEnumType() != null ) {
            res = validateEnumType( ada.getRefersToEnumType(), diagnostics ) && res;
            
            // Values must be validated against EnumType, not Enumeration
            for( Val val : ada.getVal() ) {
                res = validateValue( ada, val.getValue(), ada.getRefersToEnumType(), diagnostics ) && res;
            }
            
            for( DAI dai : ada.getReferredByDAI() ) {
                // name is OK because it has been used to create link DAI -> DA
                for( Val val : dai.getVal() ) {
                    res = validateValue( dai, val.getValue(), ada.getRefersToEnumType(), diagnostics ) && res;
                }
            }
        }
        
        return res;
    }
    
    protected boolean validateValue( UnNaming daOrDai, String value, EnumType enumType, DiagnosticChain diagnostics ) {
        boolean res = true;
        
        Optional< EnumVal > found =
                 enumType
                .getEnumVal()
                .stream()
                .filter( e -> value.equals( e.getValue() ))
                .findAny();
        
        if( ! found.isPresent() ) {
            String name = "";
            if( daOrDai instanceof AbstractDataAttribute ) name = (( AbstractDataAttribute ) daOrDai ).getName();
            if( daOrDai instanceof DAI                   ) name = (( DAI ) daOrDai ).getName();
            RiseClipseMessage error = RiseClipseMessage.error( NsdValidator.VALIDATION_NSD_CATEGORY, daOrDai.getLineNumber(), 
                                      "value \"", value, "\" of DA/BDA/DAI \"", name, "\" is not valid for EnumType \"",
                                      enumType.getId(), "\" (line = ", enumType.getLineNumber(), ")" );
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    error.getMessage(),
                    new Object[] { daOrDai, error } ));
            res = false;
        }
        
        return res;
    }

    public boolean validateEnumType( EnumType enumType, DiagnosticChain diagnostics ) {
        if( validatedEnumType.contains( enumType.getId() )) return true;
        @NonNull
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.verbose( NsdValidator.VALIDATION_NSD_CATEGORY, enumType.getLineNumber(),
                         "EnumerationValidator.validateEnumType( ", enumType.getId(), " )" );
        validatedEnumType.add( enumType.getId() );
        
        boolean res = true;
        
        // The name of the enumeration type is not a standardized name that shall be used by the implementation
        //boolean sameName = enumType.getId().equals( getName() );
        
        for( EnumVal enumVal : enumType.getEnumVal() ) {
            // the positive range of values is reserved for standardized value of enumerations, except for the IEC 61850-7-3
            // multiplierKind that standardizes also values in the negative range
            if(( enumVal.getOrd() < 0 ) && ( ! isMultiplierKind )) continue;
            
            if( ! literals.containsKey( enumVal.getOrd() )) {
                RiseClipseMessage error = RiseClipseMessage.error( NsdValidator.VALIDATION_NSD_CATEGORY, enumVal.getLineNumber(), 
                                          "EnumVal with ord \"", enumVal.getOrd(), "\" in EnumType (id = ", enumType.getId(),
                                          ") is not defined as LiteralVal in standard Enumeration ", getName() );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        error.getMessage(),
                        new Object[] { enumVal, error } ));
                res = false;
            }
            else {
                // while the supported positive value of the enumeration items shall be used by the implementation.
                if( ! literals.get( enumVal.getOrd() ).equals( enumVal.getValue() )) {
                    RiseClipseMessage error = RiseClipseMessage.error( NsdValidator.VALIDATION_NSD_CATEGORY, enumVal.getLineNumber(), 
                                              "EnumVal with ord \"", enumVal.getOrd(), "\" in EnumType (id = " , enumType.getId(),
                                              ") has incorrect name (\"", enumVal.getValue(), "\" instead of \"", literals.get( enumVal.getOrd() ), "\")" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            error.getMessage(),
                            new Object[] { enumVal, error } ));
                    res = false;
                }
            }
        }
        
        // an implementation can decide to implement/support only part of the standardized set of the enumeration
        // Therefore literals in Enumeration missing in EnumType as EnumVal is allowed
        
        return res;
    }

}
