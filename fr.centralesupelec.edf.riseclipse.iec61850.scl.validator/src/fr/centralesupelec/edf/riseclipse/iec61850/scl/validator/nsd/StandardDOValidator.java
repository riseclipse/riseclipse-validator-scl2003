/*
*************************************************************************
**  Copyright (c) 2024 CentraleSupélec & EDF.
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
import java.util.stream.Stream;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.LNClass;

public class StandardDOValidator {
    
    private static HashMap< String, String > cdcOfDataObject = new HashMap<>();
    private static HashSet< String > multiDataObject = new HashSet<>();

    public static void addFrom( Stream< LNClass > stream ) {
        stream.forEach( lnClass -> lnClass.getDataObject().stream().forEach( do_ -> {
            cdcOfDataObject.put( do_.getName(), do_.getType() );
            if( do_.getPresCond().contains( "multi" )) {
                multiDataObject.add( do_.getName() );
            }
        }));
    }

    public static boolean isStandardDoName( String doName ) {
        return cdcOfDataObject.containsKey( doName );
    }
    
    public static String getStandardCdcOfDataObject( String doName ) {
        return cdcOfDataObject.get( doName );
    }

    public static boolean validateCdcOfExtendedDO( String doName, String cdcName ) {
        return getStandardCdcOfDataObject( doName ).equals( cdcName );
    }
    
    public static boolean isStandardDoMulti( String doName ) {
        return multiDataObject.contains( doName );
    }

    private StandardDOValidator() {}
}

