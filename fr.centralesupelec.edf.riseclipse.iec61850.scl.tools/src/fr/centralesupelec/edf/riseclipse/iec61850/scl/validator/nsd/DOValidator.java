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

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.CDC;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.DataAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DA;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DO;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;

public class DOValidator {

    private String cdc;
    private HashMap< String, DataAttribute > daMap;

    public DOValidator( CDC cdc ) {
        this.cdc = cdc.getName();
        this.daMap = new HashMap<>(); // link between DAI (name) and its respective DataAttribute
        
        for( DataAttribute da : cdc.getDataAttribute() ) {
            this.daMap.put( da.getName(), da );
        }
    }

    public boolean validateDO( DO do_, DiagnosticChain diagnostics ) {
        AbstractRiseClipseConsole.getConsole().verbose( "validateDO( " + do_.getName() + " )" );
        boolean res = true;
        HashSet< String > checkedDA = new HashSet<>();

        if( do_.getRefersToDOType() == null ) {
            AbstractRiseClipseConsole.getConsole().warning( "validateDO: DO " + do_.getName() + " has no RefersToDOType" );
        }
        else {
            for( DA da : do_.getRefersToDOType().getDA() ) {
                AbstractRiseClipseConsole.getConsole().verbose( "validateDO on DA " + da.getName() + " (line" + da.getLineNumber() + ")" );
    
                // Test if DA is a possible DA in this DO
                if( ! daMap.containsKey( da.getName() ) ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "DA " + da.getName() + " (line" + da.getLineNumber() + ") not found in CDC",
                            new Object[] { do_, cdc } ));
                    res = false;
                    continue;
                }
    
                // Control of DAI presence in DO
                updateCompulsory( da, checkedDA, diagnostics );
            }
        }

        // Verify all necessary DA were present
        if( ! daMap.values().stream()
                .map( x -> checkCompulsory( do_, x, checkedDA, diagnostics ) )
                .reduce( ( a, b ) -> a && b ).get() ) {
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    "DO (line " + do_.getLineNumber() + ") does not contain all mandatory DA from CDC ",
                    new Object[] { do_, cdc } ));
            res = false;
        }
        return res;
    }

    public boolean checkCompulsory( DO do_, DataAttribute da, HashSet< String > checked, DiagnosticChain diagnostics ) {
        switch( da.getPresCond() ) {
        case "M":
            if( ! checked.contains( da.getName() )) {
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "DA " + da.getName() + " not found in DO (line " + do_.getLineNumber() + ")",
                        new Object[] { da } ));
                return false;
            }
            break;
        default:
            AbstractRiseClipseConsole.getConsole().info( "NOT IMPLEMENTED: DOValidator.checkCompulsory( " + da.getPresCond() + " )" );
            break;
        }
        return true;
    }

    public boolean updateCompulsory( DA da, HashSet< String > checked, DiagnosticChain diagnostics ) {
        switch( daMap.get( da.getName() ).getPresCond() ) {
        case "M":
        case "O":
            if( checked.contains( da.getName() ) ) {
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "DA " + da.getName() + " (line " + da.getLineNumber() + ") cannot appear more than once",
                        new Object[] { da } ));
                return false;
            }
            else {
                checked.add( da.getName() );
                break;
            }
        case "F":
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    "DA " + da.getName() + " (line " + da.getLineNumber() + ") is forbidden",
                    new Object[] { da } ));
            return false;
        default:
            AbstractRiseClipseConsole.getConsole().info( "NOT IMPLEMENTED: DOIValidator.updateCompulsory( " + daMap.get( da.getName() ).getPresCond() + " )" );
            break;
        }
        return true;
    }

}
