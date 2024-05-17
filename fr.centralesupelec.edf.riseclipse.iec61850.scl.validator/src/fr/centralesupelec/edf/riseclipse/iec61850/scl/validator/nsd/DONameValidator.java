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

import java.util.HashSet;
import java.util.stream.Stream;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.Abbreviation;

public class DONameValidator {
    
    private static HashSet< String > abreviations = new HashSet<>();

    public static void addFrom( Stream< Abbreviation > abrevStream ) {
        abrevStream.forEach( a -> abreviations.add( a.getName() ));
    }

    public static boolean validateName( String name ) {
        if( name.length() == 0 ) return false;
        int start = 0;
        int end = name.length();
        while( true ) {
            // We search for the longest existing abbreviation
            if( abreviations.contains( name.substring( start, end ))) {
                if( end == name.length() ) return true;
                start = end;
                end = name.length();;
            }
            else {
                end = end - 1;
                // There are abbreviations of 1 letter
                if( end == start ) return false;
            }
        }
    }

}
