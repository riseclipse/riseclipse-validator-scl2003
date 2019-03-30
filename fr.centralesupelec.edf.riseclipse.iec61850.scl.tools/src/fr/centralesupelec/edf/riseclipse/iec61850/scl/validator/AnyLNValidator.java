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
package fr.centralesupelec.edf.riseclipse.iec61850.scl.validator;

import java.util.HashMap;
import java.util.HashSet;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.AbstractLNClass;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.DataObject;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.LNClass;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.AnyLNClass;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AnyLN;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DOI;
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

    public void generateValidators( HashMap< String, DataObject > doMap, HashMap< String, DOIValidator > cdcMap, AnyLNClass lnClass ) {
        for( DataObject dObj : lnClass.getDataObject() ) {
            this.doMap.put( dObj.getName(), dObj );
            if( dObj.getRefersToCDC() != null ) {
                if( ! cdcMap.containsKey( dObj.getRefersToCDC().getName() )) {
                    this.cdcMap.put( dObj.getRefersToCDC().getName(), new DOIValidator( dObj.getRefersToCDC() ));
                }
            }
        }
    }

    public boolean validateLN( AnyLN ln ) {
        HashSet< String > checkedDO = new HashSet<>();

        for( DOI doi : ln.getDOI() ) {
            AbstractRiseClipseConsole.getConsole().verbose( "validateDOI( " + doi.getName() + " )" );

            // Test if DOI is a possible DOI in this LN
            if( !this.doMap.containsKey( doi.getName() ) ) {
                AbstractRiseClipseConsole.getConsole()
                        .error( "DO " + doi.getName() + " not found in LNClass " + ln.getLnClass() );
                return false;
            }

            // Control of DOI presence in LN  
            String presCond = this.doMap.get( doi.getName() ).getPresCond();
            this.updateCompulsory( doi.getName(), presCond, checkedDO );

            // Validation of DOI content
            if( ! validateDOI( doi ) ) {
                return false;
            }

        }

        // Verify all necessary DOI were present
        if( !this.doMap.entrySet().stream()
                .map( x -> checkCompulsory( x.getKey(), x.getValue().getPresCond(), checkedDO ))
                .reduce( ( a, b ) -> a && b ).get() ) {
            AbstractRiseClipseConsole.getConsole()
                    .error( "LN does not contain all mandatory DO from class " + ln.getLnClass() );
            return false;
        }
        return true;
    }

    public boolean checkCompulsory( String name, String presCond, HashSet< String > checked ) {
        switch( presCond ) {
        case "M":
            if( ! checked.contains( name ) ) {
                AbstractRiseClipseConsole.getConsole().error( "DO " + name + " is missing" );
                return false;
            }
        }
        return true;
    }

    public boolean updateCompulsory( String name, String presCond, HashSet< String > checked ) {
        switch( presCond ) {
        case "M":
        case "O":
            if( checked.contains( name )) {
                AbstractRiseClipseConsole.getConsole().error( "DO " + name + " cannot appear more than once" );
                return false;
            }
            else {
                checked.add( name );
                break;
            }
        case "F":
            AbstractRiseClipseConsole.getConsole().error( "DO " + name + " is forbidden" );
            return false;
        }
        return true;
    }

    public boolean validateDOI( DOI doi ) {

        AbstractRiseClipseConsole.getConsole().verbose( "found DO " + doi.getName() + " in LNClass " + this.lnClass );

        // DOIValidator validates DOI content
        String cdc = this.doMap.get( doi.getName() ).getRefersToCDC().getName();
        return cdcMap.get( cdc ).validateDOI( doi );
    }

}
