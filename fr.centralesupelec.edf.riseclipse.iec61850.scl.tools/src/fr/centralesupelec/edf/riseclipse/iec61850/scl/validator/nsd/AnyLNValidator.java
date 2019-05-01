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

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.AbstractLNClass;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.DataObject;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.LNClass;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.AnyLNClass;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AnyLN;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DOI;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;

public class AnyLNValidator {

    private String lnClass;
    private HashMap< String, DataObject > doMap;
    private HashMap< String, DOIValidator > cdcMap;

    public AnyLNValidator( LNClass lnClass ) {
        this.lnClass = lnClass.getName();
        this.doMap = new HashMap<>(); // link between DOI (name) and its respective DataObject
        this.cdcMap = new HashMap<>(); // link between CDC (name) and its respective DOIValidator
        
        generateValidators( doMap, cdcMap, lnClass );

        // LNClass hierarchy taken into account
        AbstractLNClass parent = lnClass.getRefersToAbstractLNClass();
        while( parent != null ) {
            generateValidators( doMap, cdcMap, parent );
            parent = parent.getRefersToAbstractLNClass();
        }

    }

    private void generateValidators( HashMap< String, DataObject > doMap, HashMap< String, DOIValidator > cdcMap, AnyLNClass lnClass ) {
        for( DataObject dObj : lnClass.getDataObject() ) {
            doMap.put( dObj.getName(), dObj );
            if( dObj.getRefersToCDC() != null ) {
                if( ! cdcMap.containsKey( dObj.getRefersToCDC().getName() )) {
                    cdcMap.put( dObj.getRefersToCDC().getName(), new DOIValidator( dObj.getRefersToCDC() ));
                }
            }
        }
    }

    public boolean validateLN( AnyLN ln, DiagnosticChain diagnostics ) {
        boolean res = true;

        HashSet< String > checkedDO = new HashSet<>();

        for( DOI doi : ln.getDOI() ) {
            AbstractRiseClipseConsole.getConsole().verbose( "validateDOI( " + doi.getName() + " )" );

            // Test if DOI is a possible DOI in this LN
            if( ! doMap.containsKey( doi.getName() ) ) {
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "DO " + doi.getName() + " in LN at line " + ln.getLineNumber() + " not found in LNClass " + ln.getLnClass(),
                        new Object[] { ln } ));
                continue;
            }

            // Control of DOI presence in LN  
            updateCompulsory( doi, checkedDO, diagnostics );

            // Validation of DOI content
            if( ! validateDOI( doi, diagnostics ) ) {
                res = false;
            }

        }

        // Verify all necessary DOI were present
        if( ! doMap.values().stream()
                .map( x -> checkCompulsory( ln, x, checkedDO, diagnostics ))
                .reduce( ( a, b ) -> a && b ).get() ) {
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    "LN at line " + ln.getLineNumber() + " does not contain all mandatory DO from class " + ln.getLnClass(),
                    new Object[] { ln } ));
            res = false;
        }
        return res;
    }

    private boolean checkCompulsory( AnyLN ln, DataObject dataObject, HashSet< String > checkedDO, DiagnosticChain diagnostics ) {
        switch( dataObject.getPresCond() ) {
        case "M":
            if( ! checkedDO.contains( dataObject.getName() ) ) {
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "DO " + dataObject.getName() + " is missing in LN at line " + ln.getLineNumber(),
                        new Object[] { ln } ));
                return false;
            }
            break;
        default:
            AbstractRiseClipseConsole.getConsole().info( "NOT IMPLEMENTED: AnyLNValidator.checkCompulsory( " + dataObject.getPresCond() + " )" );
            break;
        }
        return true;
    }

    private boolean updateCompulsory( DOI doi, HashSet< String > checkedDO, DiagnosticChain diagnostics ) {
        switch( doMap.get( doi.getName() ).getPresCond() ) {
        case "M":
        case "O":
            if( checkedDO.contains( doi.getName() )) {
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "DO " + doi + " cannot appear more than once in LN at line " + doi.getParentAnyLN().getLineNumber(),
                        new Object[] { doi } ));
                return false;
            }
            checkedDO.add( doi.getName() );
            break;
        case "F":
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    "DO " + doi + " is forbidden in LN at line " + doi.getParentAnyLN().getLineNumber(),
                    new Object[] { doi } ));
            return false;
        default:
            AbstractRiseClipseConsole.getConsole().info( "NOT IMPLEMENTED: AnyLNValidator.updateCompulsory( " + doMap.get( doi.getName() ).getPresCond() + " )" );
            break;
        }
        return true;
    }

    private boolean validateDOI( DOI doi, DiagnosticChain diagnostics ) {

        AbstractRiseClipseConsole.getConsole().verbose( "found DO " + doi.getName() + " in LNClass " + lnClass );

        // DOIValidator validates DOI content
        String cdc = doMap.get( doi.getName() ).getRefersToCDC().getName();
        return cdcMap.get( cdc ).validateDOI( doi, diagnostics );
    }

}
