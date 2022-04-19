/*
*************************************************************************
**  Copyright (c) 2019-2022 CentraleSupélec & EDF.
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

import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.jdt.annotation.NonNull;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.AnyLNClass;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.DataObject;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.LNClass;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DO;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DOType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.LNodeType;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;

public class LNClassValidator {
    
    private static final String LNCLASS_SETUP_NSD_CATEGORY      = NsdValidator.SETUP_NSD_CATEGORY      + "/LNClass";
    private static final String LNCLASS_VALIDATION_NSD_CATEGORY = NsdValidator.VALIDATION_NSD_CATEGORY + "/LNClass";

    private static HashMap< String, LNClassValidator > validators;
    
    public static void initialize() {
        // Allow reentrancy
        validators = new HashMap<>();
        
        DataObjectPresenceConditionValidator.initialize();
    }

    public static LNClassValidator get( String name ) {
        if( validators == null ) return null;
        return validators.get( name );
    }
    
    public static void buildValidators( Stream< LNClass > stream ) {
        stream
        .forEach( lnClass -> validators.put( lnClass.getName(), new LNClassValidator( lnClass )));
    }

    /*
     * Called before another file is validated
     */
    public static void resetValidators() {
        validators.values().stream().forEach( v -> v.reset() );
    }

    private void reset() {
        validatedLNodeType = new HashSet<>();
        
        dataObjectValidatorMap.values().stream().forEach( v -> v.reset() );
    }

    private HashSet< String > validatedLNodeType;

    private DataObjectPresenceConditionValidator dataObjectPresenceConditionValidator;
    private HashMap< String, CDCValidator > dataObjectValidatorMap = new HashMap<>();

    private LNClassValidator( AnyLNClass anyLNClass ) {
        dataObjectPresenceConditionValidator = DataObjectPresenceConditionValidator.get( anyLNClass );
        
        AnyLNClass lnClass = anyLNClass;
        @NonNull
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        while( lnClass != null ) {
            for( DataObject do_ : lnClass.getDataObject() ) {
                if( CDCValidator.get( do_.getType() ) != null ) {
                    dataObjectValidatorMap.put( do_.getName(), CDCValidator.get( do_.getType() ));
                    console.notice( LNCLASS_SETUP_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(),
                                    "CDC for DataObject ", do_.getName(), " found with type ", do_.getType() );
                }
                else {
                    console.warning( LNCLASS_SETUP_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(),
                                     "CDC not found for DataObject ", do_.getName() );
                }
            }

            lnClass = lnClass.getRefersToAbstractLNClass();
        }
        
        reset();
    }
    
    public boolean validateLNodeType( LNodeType lNodeType, DiagnosticChain diagnostics ) {
        if( validatedLNodeType.contains( lNodeType.getId() )) return true;
        @NonNull
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.debug( LNCLASS_VALIDATION_NSD_CATEGORY, lNodeType.getLineNumber(),
                       "LNClassValidator.validateLNodeType( ", lNodeType.getId(), " )" );
        validatedLNodeType.add( lNodeType.getId() );

        boolean res = true;

        dataObjectPresenceConditionValidator.reset();
        lNodeType
        .getDO()
        .stream()
        .forEach( d -> dataObjectPresenceConditionValidator.addDO( d, diagnostics ));
      
        res = dataObjectPresenceConditionValidator.validate( lNodeType, diagnostics ) && res;
        
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
                CDCValidator validator = dataObjectValidatorMap.get( names[0] );
                if( validator != null ) {
                    res = validator.validateDOType( doType, diagnostics ) && res;
                }
                else {
                    // This error will be detected in DataObjectPresenceConditionValidator.addDO() who will check if it is right if dataNs attribute is present
                    //AbstractRiseClipseConsole.getConsole().warning( "[NSD validation] while validating LNodeType (line " + lNodeType.getLineNumber() + "): validator for DO " + do_.getName() + " not found" );
                }
            }
            else {
                console.warning( LNCLASS_VALIDATION_NSD_CATEGORY, do_.getLineNumber(),
                                 "DOType for DO " + do_.getName() + " not found" );
            }
        }
        
        return res;
    }

}
