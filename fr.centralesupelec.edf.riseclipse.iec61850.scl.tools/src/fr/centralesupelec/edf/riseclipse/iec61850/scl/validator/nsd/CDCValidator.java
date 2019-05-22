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
import java.util.stream.Stream;

import org.eclipse.emf.common.util.DiagnosticChain;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.CDC;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.DataAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.SubDataObject;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DA;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DOType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.SDO;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;

public class CDCValidator {

    private static HashMap< String, CDCValidator > validators = new HashMap<>();
    
    public static CDCValidator get( String name ) {
        return validators.get( name );
    }
    
    public static void buildValidators( Stream< CDC > stream ) {
        stream
        .forEach( cdc -> validators.put( cdc.getName(), new CDCValidator( cdc )));
    }

    private DataAttributePresenceConditionValidator dataAttributePresenceConditionValidator;
    private SubDataObjectPresenceConditionValidator subDataObjectPresenceConditionValidator;
    private HashMap< String, TypeValidator > dataAttributeValidatorMap = new HashMap<>();
    private HashMap< String, CDCValidator > subDataObjectValidatorMap = new HashMap<>();
    private HashSet< DOType > validatedDOType = new HashSet<>(); 

    private CDCValidator( CDC cdc ) {
        dataAttributePresenceConditionValidator = DataAttributePresenceConditionValidator.get( cdc );
        subDataObjectPresenceConditionValidator = SubDataObjectPresenceConditionValidator.get( cdc );
        
        for( DataAttribute da : cdc.getDataAttribute() ) {
            TypeValidator validator = TypeValidator.get( da.getType() );
            if( validator != null ) {
                dataAttributeValidatorMap.put( da.getName(), validator );
            }
            else {
                AbstractRiseClipseConsole.getConsole().warning( "[NSD setup] Type not found for DataAttribute " + da.getName() );
            }
        }
        
        for( SubDataObject sdo : cdc.getSubDataObject() ) {
            CDCValidator validator = CDCValidator.get( sdo.getType() );
            if( validator != null ) {
                subDataObjectValidatorMap.put( sdo.getName(), validator );
            }
            else {
                AbstractRiseClipseConsole.getConsole().warning( "[NSD setup] CDC not found for SubDataObject " + sdo.getName() );
            }
        }
        
    }

    public boolean validateDOType( DOType doType, DiagnosticChain diagnostics ) {
        if( validatedDOType.contains( doType )) return true;
        AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] CDCValidator.validateDOType( " + doType.getId() + " ) at line " + doType.getLineNumber() );
        validatedDOType.add( doType );
        
        dataAttributePresenceConditionValidator.reset();
        doType
        .getDA()
        .stream()
        .forEach( d -> dataAttributePresenceConditionValidator.addDA( d, diagnostics ));
      
        boolean res = dataAttributePresenceConditionValidator.validate( doType, diagnostics );
        
        subDataObjectPresenceConditionValidator.reset();
        doType
        .getSDO()
        .stream()
        .forEach( d -> subDataObjectPresenceConditionValidator.addSDO( d, diagnostics ));
        
        res = subDataObjectPresenceConditionValidator.validate( doType, diagnostics ) && res;
        
        for( DA da : doType.getDA() ) {
            TypeValidator validator = dataAttributeValidatorMap.get( da.getName() );
            if( validator != null ) {
                validator.validateDA( da, diagnostics );
            }
            else {
                AbstractRiseClipseConsole.getConsole().warning( "[NSD validation] while validating DOType (line " + doType.getLineNumber() + "): validator for DA " + da.getName() + " not found" );
            }
        }
      
        for( SDO sdo : doType.getSDO() ) {
            CDCValidator validator = subDataObjectValidatorMap.get( sdo.getName() );
            if( validator != null ) {
                if( sdo.getRefersToDOType() != null ) {
                    AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] validateDOType( " + doType.getId() + " ) on sdo " + sdo.getName() );
                    res = validator.validateDOType( sdo.getRefersToDOType(), diagnostics ) && res;
                }
                else {
                    AbstractRiseClipseConsole.getConsole().warning( "[NSD validation] while validating DOType (line " + doType.getLineNumber() + "): DOType for SDO " + sdo.getName() + " not found" );
                }
            }
            else {
                AbstractRiseClipseConsole.getConsole().warning( "[NSD validation] while validating DOType (line " + doType.getLineNumber() + "): validator for SDO " + sdo.getType() + " not found" );
            }
        }

        return res;
    }

}
