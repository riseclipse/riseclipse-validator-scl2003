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
import java.util.HashSet;
import java.util.Optional;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.Enumeration;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AbstractDataAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DAI;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.EnumType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.EnumVal;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.UnNaming;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.Val;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;

public class EnumerationValidator extends TypeValidator {
    
    public static void initialize() {
        // Nothing here
    }
    
    private HashSet< String > validatedEnumType;

    // Name of EnumVal may be empty, so we use LiteralVal as key
    private HashMap< Integer, String > literals = new HashMap<>();
    private String name;
    private String inheritedFromName;
    private EnumerationValidator inheritedFrom;

    public EnumerationValidator( Enumeration enumeration ) {
        this.name = enumeration.getName();
        this.inheritedFromName = enumeration.getInheritedFrom();
        
        enumeration
        .getLiteral()
        .stream()
        .forEach( e -> literals.put( e.getLiteralVal(), e.getName() ));
        
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
        
        if( inheritedFrom != null ) inheritedFrom.reset();
    }

    @Override
    public boolean validateAbstractDataAttribute( AbstractDataAttribute ada, DiagnosticChain diagnostics ) {
        AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] EnumerationValidator.validateAbstractDataAttribute( " + ada.getName() + " ) at line " + ada.getLineNumber() );
        
        if(( inheritedFromName != null ) && ( inheritedFrom == null )) {
            TypeValidator inheritedValidator = TypeValidator.get( inheritedFromName );
            if(( inheritedValidator != null ) && ( inheritedValidator instanceof EnumerationValidator )) {
                inheritedFrom = ( EnumerationValidator ) inheritedValidator;
            }
            else {
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD validation] validator for inherited enumeration \"" + inheritedFromName + "\" not found",
                        new Object[] { ada } ));
                // Avoid checking again
                inheritedFromName = null;
            }
        }

        boolean res = true;
        if( ! "Enum".equals(  ada.getBType() )) {
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    "[NSD validation] bType of DA/BDA \"" + ada.getName() + "\" (line = " + ada.getLineNumber() + ") is not Enum",
                    new Object[] { ada } ));
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
        for( Val val : ada.getVal() ) {
            res = validateValue( ada, val.getValue(), diagnostics ) && res;
        }
        
        if( ada.getRefersToEnumType() != null ) {
            res = validateEnumType( ada.getRefersToEnumType(), diagnostics ) && res;
        }
        
        for( DAI dai : ada.getReferredByDAI() ) {
            // name is OK because it has been used to create link DAI -> DA
            for( Val val : dai.getVal() ) {
                res = validateValue( dai, val.getValue(), diagnostics ) && res;
            }
        }
        return res;
    }
    
    protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
        boolean res = true;
        
        if( ! literals.containsValue( value )) {
            if( inheritedFrom != null ) {
                res = inheritedFrom.validateValue( daOrDai, value, diagnostics ) && res;
            }
            else {
                String name = "";
                if( daOrDai instanceof AbstractDataAttribute ) name = (( AbstractDataAttribute ) daOrDai ).getName();
                if( daOrDai instanceof DAI                   ) name = (( DAI ) daOrDai ).getName();
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD validation] value \"" + value + "\" of DA/BDA \"" + name + "\" (line = " + daOrDai.getLineNumber() + ") is not valid for enumeration \"" + this.name + "\"",
                        new Object[] { daOrDai } ));
                res = false;
            }
        }
        
        return res;
    }

    public boolean validateEnumType( EnumType enumType, DiagnosticChain diagnostics ) {
        if( validatedEnumType.contains( enumType.getId() )) return true;
        AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] EnumerationValidator.validateEnumType( " + enumType.getId() + " ) at line " + enumType.getLineNumber() );
        validatedEnumType.add( enumType.getId() );
        
        boolean res = true;
        
        // EnumType may extend or restrict the set of EnumVal, but another name must be used 
        boolean sameName = enumType.getId().equals( getName() );
        
        for( EnumVal enumVal : enumType.getEnumVal() ) {
            if( ! literals.containsKey( enumVal.getOrd() )) {
                if( inheritedFrom != null ) {
                    res = inheritedFrom.validateEnumType( enumType, diagnostics ) && res;
                }
                else {
                    diagnostics.add( new BasicDiagnostic(
                            sameName ? Diagnostic.ERROR : Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] EnumVal with ord \"" + enumVal.getOrd() + "\" in EnumType (id = " + enumType.getId()
                                    + ") at line " + enumVal.getLineNumber() + " is no defined as LiteralVal in Enumeration " + getName(),
                            new Object[] { enumVal } ));
                    res = false;
                }
            }
            else {
                if( ! literals.get( enumVal.getOrd() ).equals( enumVal.getValue() )) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] EnumVal with ord \"" + enumVal.getOrd() + "\" in EnumType (id = " + enumType.getId()
                                    + ") at line " + enumVal.getLineNumber() + " has incorrect name (\"" + enumVal.getValue()
                                    + "\" instead of \"" + literals.get( enumVal.getOrd() ) + "\")",
                            new Object[] { enumVal } ));
                    res = false;
                }
            }
        }
        
        // Literals in Enumeration missing in EnumType as EnumVal allowed if name differ
        EnumerationValidator current = this;
        while( true ) {
            for( Integer literalVal : current.literals.keySet() ) {
                Optional< EnumVal > found =
                        enumType
                        .getEnumVal()
                        .stream()
                        .filter( v -> v.getOrd().equals( literalVal ))
                        .findFirst();
                if( ! found.isPresent() ) {
                    diagnostics.add( new BasicDiagnostic(
                            sameName ? Diagnostic.ERROR : Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] LiteralVal \"" + literalVal + "\" in Enumeration " + getName()
                                    + " is not present as EnumVal in EnumType (id = " + enumType.getId()
                                    + ") at line " + enumType.getLineNumber(),
                            new Object[] { enumType } ));
                    res = false;
                }
            }
            TypeValidator inheritedValidator = TypeValidator.get( inheritedFromName );
            if(( inheritedValidator != null ) && ( inheritedValidator instanceof EnumerationValidator )) {
                current = ( EnumerationValidator ) inheritedValidator;
            }
            else {
                break;
            }
        }
        
        if( sameName && ! res ) {
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    "[NSD validation] EnumType (id = " + enumType.getId() + ") at line " + enumType.getLineNumber()
                        + " must use a different id because it extends or restricts the standard Enumeration",
                    new Object[] { enumType } ));
        }
        
        return res;
    }

}
