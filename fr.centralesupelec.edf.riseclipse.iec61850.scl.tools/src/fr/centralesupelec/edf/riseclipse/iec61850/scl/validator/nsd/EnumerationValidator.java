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

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.Enumeration;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AbstractDataAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.EnumType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.EnumVal;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.Val;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;

public class EnumerationValidator extends TypeValidator {
    
    private HashMap< String, Integer > literals = new HashMap<>();
    private String name;
    private String inheritedFromName;
    private EnumerationValidator inheritedFrom;
    private HashSet< EnumType > validatedEnumType = new HashSet<>();

    public EnumerationValidator( Enumeration enumeration ) {
        this.name = enumeration.getName();
        this.inheritedFromName = enumeration.getInheritedFrom();
        
        enumeration
        .getLiteral()
        .stream()
        .forEach( e -> literals.put( e.getName(), e.getLiteralVal() ));
    }
    
    public String getName() {
        return name;
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
                        "[NSD validation] validator for inherited enumeration " + inheritedFromName + " not found",
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
                    "[NSD validation] bType of DA/BDA " + ada.getName() + " line = " + ada.getLineNumber() + ") is not Enum",
                    new Object[] { ada } ));
            res = false;
        }
        if( ! getName().equals( ada.getType() )) {
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    "[NSD validation] type of DA/BDA " + ada.getName() + " line = " + ada.getLineNumber() + ") is not " + getName(),
                    new Object[] { ada } ));
            res = false;
        }
        for( Val val : ada.getVal() ) {
            res = validateValue( ada, val.getValue(), diagnostics ) && res;
        }
        
        if( ada.getRefersToEnumType() != null ) {
            res = validateEnumType( ada.getRefersToEnumType(), diagnostics ) && res;
        }
        
        return res;
    }
    
    protected boolean validateValue( AbstractDataAttribute ada, String value, DiagnosticChain diagnostics ) {
        boolean res = true;
        
        if( ! literals.containsKey( value )) {
            if( inheritedFrom != null ) {
                res = inheritedFrom.validateValue( ada, value, diagnostics ) && res;
            }
            else {
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD validation] value of DA/BDA " + ada.getName() + " line = " + ada.getLineNumber() + ") is not valid",
                        new Object[] { ada } ));
                res = false;
            }
        }
        
        return res;
    }

    public boolean validateEnumType( EnumType enumType, DiagnosticChain diagnostics ) {
        if( validatedEnumType.contains( enumType )) return true;
        AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] EnumerationValidator.validateEnumType( " + enumType.getId() + " ) at line " + enumType.getLineNumber() );
        validatedEnumType.add( enumType );
        
        boolean res = true;
        
        // enumType.getId().equals( getName() ) already tested because enumType.getId().equals( da.getType() )
        
        for( EnumVal enumVal : enumType.getEnumVal() ) {
            if( ! literals.containsKey( enumVal.getValue() )) {
                if( inheritedFrom != null ) {
                    res = inheritedFrom.validateEnumType( enumType, diagnostics ) && res;
                }
                else {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] EnumVal " + enumVal.getValue() + " in EnumType (id = " + enumType.getId()
                                    + ") at line " + enumVal.getLineNumber() + " is unknown",
                            new Object[] { enumVal } ));
                    res = false;
                }
            }
            else {
                try {
                    Integer val = new Integer( literals.get( enumVal.getValue() ));
                    if( ! val.equals( enumVal.getOrd() )) {
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                "[NSD validation] EnumVal " + enumVal.getValue() + " in EnumType (id = " + enumType.getId()
                                        + ") at line " + enumVal.getLineNumber() + " has incorrect ord (" + enumVal.getOrd()
                                        + " instead of " + literals.get( enumVal.getValue() ) + ")",
                                new Object[] { enumVal } ));
                        res = false;
                    }
                }
                catch( NumberFormatException e ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] EnumVal " + enumVal.getValue() + " in EnumType (id = " + enumType.getId()
                                    + ") at line " + enumVal.getLineNumber() + " is not an integer",
                            new Object[] { enumVal } ));
                    res = false;
                }
            }
        }
        
        // TODO: do we have to check that all literals in Enumeration are present as EnumVal ?
        
        return res;
    }

}
