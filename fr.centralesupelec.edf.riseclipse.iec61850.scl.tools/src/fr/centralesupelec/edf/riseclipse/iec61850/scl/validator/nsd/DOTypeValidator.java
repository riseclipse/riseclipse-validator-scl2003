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

import org.eclipse.emf.common.util.DiagnosticChain;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.CDC;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DOType;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;

public class DOTypeValidator {

    private DataAttributePresenceConditionValidator dataAttributePresenceConditionValidator;

    public DOTypeValidator( CDC cdc ) {
        dataAttributePresenceConditionValidator = DataAttributePresenceConditionValidator.get( cdc );
    }

    public boolean validateDOType( DOType doType, DiagnosticChain diagnostics ) {
        AbstractRiseClipseConsole.getConsole().verbose( "[NSD] validateDOType( " + doType.getId() + " )" );
        dataAttributePresenceConditionValidator.reset();
        
        doType
        .getDA()
        .stream()
        .forEach( d -> dataAttributePresenceConditionValidator.addDA( d, diagnostics ));
      
        return dataAttributePresenceConditionValidator.validate( doType, diagnostics );
        
    }

}
