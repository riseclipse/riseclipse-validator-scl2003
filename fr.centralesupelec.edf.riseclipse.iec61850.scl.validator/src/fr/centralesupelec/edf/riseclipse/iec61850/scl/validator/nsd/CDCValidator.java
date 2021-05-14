/*
*************************************************************************
**  Copyright (c) 2019-2021 CentraleSupélec & EDF.
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

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.CDC;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.DataAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.SubDataObject;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DA;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DOType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.FCEnum;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.SDO;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;

public class CDCValidator {

    private static HashMap< String, CDCValidator > validators = new HashMap<>();
    
    public static CDCValidator get( String name ) {
        if( validators == null ) return null;
       return validators.get( name );
    }
    
    public static void buildValidators( Stream< CDC > stream ) {
        stream
        .forEach( cdc -> validators.put( cdc.getName(), new CDCValidator( cdc )));
    }

    /*
     * Called before another file is validated
     */
    public static void resetValidators() {
        validators.values().stream().forEach( v -> v.reset() );
    }

    public void reset() {
        validatedDOType = new HashSet<>();
        
        dataAttributeTypeValidatorMap.values().stream().forEach( v -> v.reset() );
        subDataObjectValidatorMap.values().stream().forEach( v -> v.reset() );
    }

    private HashSet< String > validatedDOType; 

    private DataAttributePresenceConditionValidator dataAttributePresenceConditionValidator;
    private SubDataObjectPresenceConditionValidator subDataObjectPresenceConditionValidator;
    
    private HashMap< String, TypeValidator > dataAttributeTypeValidatorMap = new HashMap<>();
    private HashMap< String, CDCValidator > subDataObjectValidatorMap = new HashMap<>();
    private HashMap< String, FunctionalConstraintValidator > dataAttributeFunctionalConstraintValidatorMap = new HashMap<>();

    private CDCValidator( CDC cdc ) {
        dataAttributePresenceConditionValidator = DataAttributePresenceConditionValidator.get( cdc );
        subDataObjectPresenceConditionValidator = SubDataObjectPresenceConditionValidator.get( cdc );
        
        for( DataAttribute da : cdc.getDataAttribute() ) {
            TypeValidator typeValidator = TypeValidator.get( da.getType() );
            if( typeValidator != null ) {
                dataAttributeTypeValidatorMap.put( da.getName(), typeValidator );
            }
            else {
                AbstractRiseClipseConsole.getConsole().warning( "[NSD setup] (" + da.getFilename() + ":" + da.getLineNumber() + ") Type not found for DataAttribute " + da.getName() );
            }
            
            FunctionalConstraintValidator fcValidator = FunctionalConstraintValidator.get( FCEnum.getByName( da.getFc() ));
            if( fcValidator != null ) {
                dataAttributeFunctionalConstraintValidatorMap.put( da.getName(), fcValidator );
            }
            else {
                AbstractRiseClipseConsole.getConsole().warning( "[NSD setup] (" + da.getFilename() + ":" + da.getLineNumber() + ") Functional Constraint unknown for DataAttribute " + da.getName() );
            }
        }
        
        for( SubDataObject sdo : cdc.getSubDataObject() ) {
            CDCValidator validator = CDCValidator.get( sdo.getType() );
            if( validator != null ) {
                subDataObjectValidatorMap.put( sdo.getName(), validator );
            }
            else {
                AbstractRiseClipseConsole.getConsole().warning( "[NSD setup] (" + sdo.getFilename() + ":" + sdo.getLineNumber() + ") CDC not found for SubDataObject " + sdo.getName() );
            }
        }
        
        reset();
    }

    public boolean validateDOType( DOType doType, DiagnosticChain diagnostics ) {
        if( validatedDOType.contains( doType.getId() )) return true;
        AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] CDCValidator.validateDOType( " + doType.getId() + " ) at line " + doType.getLineNumber() );
        validatedDOType.add( doType.getId() );
        
        dataAttributePresenceConditionValidator.resetModelData();
        
        doType
        .getDA()
        .stream()
        .forEach( d -> dataAttributePresenceConditionValidator.addModelData( d, d.getName(), diagnostics ));
      
        boolean res = dataAttributePresenceConditionValidator.validate( doType, diagnostics );
        
        subDataObjectPresenceConditionValidator.resetModelData();
        
        doType
        .getSDO()
        .stream()
        .forEach( d -> subDataObjectPresenceConditionValidator.addModelData( d, d.getName(), diagnostics ));
        
        res = subDataObjectPresenceConditionValidator.validate( doType, diagnostics ) && res;
        
        for( DA da : doType.getDA() ) {
            TypeValidator validator = dataAttributeTypeValidatorMap.get( da.getName() );
            if( validator != null ) {
                validator.validateAbstractDataAttribute( da, diagnostics );
            }
            else {
                // DA not allowed, error will be reported by PresenceConditionValidator
                //AbstractRiseClipseConsole.getConsole().warning( "[NSD validation] while validating DOType (line " + doType.getLineNumber() + "): type validator for DA " + da.getName() + " not found" );
            }

            FunctionalConstraintValidator fcValidator = dataAttributeFunctionalConstraintValidatorMap.get( da.getName() );
            if( fcValidator != null ) {
                fcValidator.validateAbstractDataAttribute( da, diagnostics );
            }
            else {
                // DA not allowed, error will be reported by PresenceConditionValidator
                //AbstractRiseClipseConsole.getConsole().warning( "[NSD validation] while validating DOType (line " + doType.getLineNumber() + "): functional constraint validator for DA " + da.getName() + " not found" );
            }
        }
      
        for( SDO sdo : doType.getSDO() ) {
            CDCValidator validator = subDataObjectValidatorMap.get( sdo.getName() );
            if( validator != null ) {
                if( sdo.getRefersToDOType() != null ) {
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
