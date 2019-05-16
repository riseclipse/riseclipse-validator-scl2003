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

import org.eclipse.emf.common.util.DiagnosticChain;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.AnyLNClass;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.CDC;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.DataObject;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DO;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DOType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.LNodeType;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;

public class LNodeTypeValidator {

    private DataObjectPresenceConditionValidator dataObjectPresenceConditionValidator;
    private HashMap< String, DOTypeValidator > doTypeValidatorMap = new HashMap<>();

    public LNodeTypeValidator( AnyLNClass anyLNClass ) {
        dataObjectPresenceConditionValidator = DataObjectPresenceConditionValidator.get( anyLNClass );
        
        AnyLNClass lnClass = anyLNClass;
        while( lnClass != null ) {
            for( DataObject do_ : lnClass.getDataObject() ) {
                CDC cdc = do_.getRefersToCDC();
                if( cdc != null ) {
                    doTypeValidatorMap.put( do_.getName(), new DOTypeValidator( cdc ));
                }
                else {
                    AbstractRiseClipseConsole.getConsole().warning( "[NSD setup] CDC for DataObject " + do_.getName() + " not found" );
                }
            }

            lnClass = lnClass.getRefersToAbstractLNClass();
        }
    }
    
    public boolean validateLNodeType( LNodeType lNodeType, DiagnosticChain diagnostics ) {
        AbstractRiseClipseConsole.getConsole().verbose( "[NSD] validateLNodeType( " + lNodeType.getId() + " )" );
        boolean res = true;
        dataObjectPresenceConditionValidator.reset();
        
        lNodeType
        .getDO()
        .stream()
        .forEach( d -> dataObjectPresenceConditionValidator.addDO( d, diagnostics ));
      
        res = res && dataObjectPresenceConditionValidator.validate( lNodeType, diagnostics );
        
        for( DO do_ : lNodeType.getDO() ) {
            DOType doType = do_.getRefersToDOType();
            if( doType != null ) {
                String[] names;
                if( do_.getName().matches( "[a-zA-Z]+\\d+" )) {
                    names = do_.getName().split( "(?=\\d)", 2 );
                }
                else {
                    names = new String[] { do_.getName() };
                }
                if( names.length == 0 ) {
                    // error should have been already displayed
                    //AbstractRiseClipseConsole.getConsole().error( "[NSD validation] Unexpected DO name " + do_.getName() + " in LNodeType (line " + do_.getParentLNodeType().getLineNumber() );
                    continue;
                }
                DOTypeValidator validator = doTypeValidatorMap.get( names[0] );
                if( validator != null ) {
                    res = res && validator.validateDOType( doType, diagnostics );
                }
                else {
                    // This error will be detected in DataObjectPresenceConditionValidator.addDO() who will check if it is right if dataNs attribute is present
                    //AbstractRiseClipseConsole.getConsole().warning( "[NSD validation] while validating LNodeType (line " + lNodeType.getLineNumber() + "): validator for DO " + do_.getName() + " not found" );
                }
            }
            else {
                AbstractRiseClipseConsole.getConsole().warning( "[NSD validation] DOType for DO " + do_.getName() + " not found" );
            }
        }
        
        return res;
    }

}
