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

    private String lnClassName;
    private HashMap< String, DataObject > dataObjectMap;
    private HashMap< String, DOIValidator > doiValidatorMap;

    public AnyLNValidator( LNClass lnClass ) {
        this.lnClassName = lnClass.getName();
        this.dataObjectMap = new HashMap<>(); // link between DOI (name) and its respective DataObject
        this.doiValidatorMap = new HashMap<>(); // link between CDC (name) and its respective DOIValidator
        
        generateValidators( lnClass );

        // LNClass hierarchy taken into account
        AbstractLNClass parent = lnClass.getRefersToAbstractLNClass();
        while( parent != null ) {
            generateValidators( parent );
            parent = parent.getRefersToAbstractLNClass();
        }

    }

    private void generateValidators( AnyLNClass lnClass ) {
        for( DataObject dObj : lnClass.getDataObject() ) {
            dataObjectMap.put( dObj.getName(), dObj );
            if( dObj.getRefersToCDC() != null ) {
                if( ! doiValidatorMap.containsKey( dObj.getRefersToCDC().getName() )) {
                    doiValidatorMap.put( dObj.getRefersToCDC().getName(), new DOIValidator( dObj.getRefersToCDC() ));
                }
            }
        }
    }

    public boolean validateAnyLN( AnyLN anyLN, DiagnosticChain diagnostics ) {
        AbstractRiseClipseConsole.getConsole().verbose( "[NSD] validateAnyLN( " + anyLN.getLnClass() + " )" );
        boolean res = true;

        HashSet< String > checkedDO = new HashSet<>();

        for( DOI doi : anyLN.getDOI() ) {
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD] validateDOI( " + doi.getName() + " )" );

            // Test if DOI is a possible DOI in this LN
            if( ! dataObjectMap.containsKey( doi.getName() ) ) {
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD] DO " + doi.getName() + " in AnyLN at line " + anyLN.getLineNumber() + " not found in LNClass " + anyLN.getLnClass(),
                        new Object[] { anyLN } ));
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
        if( ! dataObjectMap.values().stream()
                .map( x -> checkCompulsory( anyLN, x, checkedDO, diagnostics ))
                .reduce( ( a, b ) -> a && b ).get() ) {
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    "[NSD] AnyLN at line " + anyLN.getLineNumber() + " does not contain all mandatory DO from class " + anyLN.getLnClass(),
                    new Object[] { anyLN } ));
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
                        "[NSD] DO " + dataObject.getName() + " is missing in LN at line " + ln.getLineNumber(),
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
        switch( dataObjectMap.get( doi.getName() ).getPresCond() ) {
        case "M":
        case "O":
            if( checkedDO.contains( doi.getName() )) {
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD] DO " + doi + " cannot appear more than once in LN at line " + doi.getParentAnyLN().getLineNumber(),
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
                    "[NSD] DO " + doi + " is forbidden in LN at line " + doi.getParentAnyLN().getLineNumber(),
                    new Object[] { doi } ));
            return false;
        default:
            AbstractRiseClipseConsole.getConsole().info( "NOT IMPLEMENTED: AnyLNValidator.updateCompulsory( " + dataObjectMap.get( doi.getName() ).getPresCond() + " )" );
            break;
        }
        return true;
    }

    private boolean validateDOI( DOI doi, DiagnosticChain diagnostics ) {
        AbstractRiseClipseConsole.getConsole().verbose( "[NSD] found DO " + doi.getName() + " in LNClass " + lnClassName );

        // DOIValidator validates DOI content
        String cdc = dataObjectMap.get( doi.getName() ).getRefersToCDC().getName();
        return doiValidatorMap.get( cdc ).validateDOI( doi, diagnostics );
    }

}
