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

import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.jdt.annotation.NonNull;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.CDC;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.DataAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.NsdObject;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.SubDataObject;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentification;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentificationName;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentificationObject;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DA;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DO;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DOType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.FCEnum;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.SDO;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.RiseClipseMessage;

public class CDCValidator {

    private static final String CDC_SETUP_NSD_CATEGORY      = NsdValidator.SETUP_NSD_CATEGORY      + "/CDC";
    private static final String CDC_VALIDATION_NSD_CATEGORY = NsdValidator.VALIDATION_NSD_CATEGORY + "/CDC";

    // CDC may be parameterized.
    // When the parameter is given by the DataObject, a copy of the CDC is done with the right data attribute type
    // (see DataObjectImpl.createParameterizedComponents()).
    // The CDCImpl class ensure uniqueness of a given parameterized CDC (see CDCImpl.getParameterizedCDC()).
    // Therefore, we look for the CDCValidator using the CDC object.
    private static IdentityHashMap< NsIdentificationObject, CDCValidator > validators = new IdentityHashMap<>();
    
    public static Pair< CDCValidator, NsIdentification > get( NsIdentification nsIdentification, CDC cdc ) {
        NsIdentification nsId = nsIdentification;
        CDCValidator cdcValidator = null;
        while(( cdcValidator == null ) && ( nsId != null )) {
            cdcValidator = validators.get( NsIdentificationObject.of( nsId, cdc ));
            nsIdentification = nsId;
            nsId = nsId.getDependsOn();
        }
        return Pair.of( cdcValidator, nsIdentification );
    }
    
    public static void buildValidators( NsIdentification nsIdentification, Stream< CDC > stream, IRiseClipseConsole console ) {
        stream
        .forEach( cdc -> validators.put(
                NsIdentificationObject.of( nsIdentification, cdc ),
                new CDCValidator( nsIdentification, cdc, console )));
    }

    /*
     * Called before another file is validated
     */
    public static void resetValidators() {
        validators.values().stream().forEach( v -> v.reset() );
    }

    public void reset() {
        validatedDOType = new HashSet<>();
        
//        dataAttributeTypeValidatorMap.values().stream().forEach( v -> v.reset() );
//        subDataObjectValidatorMap.values().stream().forEach( v -> v.reset() );
    }

    private NsIdentification nsIdentification;
    private CDC cdc;
    private HashSet< String > validatedDOType; 

    // CDC defines a set of DataAttribute, SubDataObject, ServiceParameter
    // Each DataAttribute has a name, a type (basic, enumerated or constructed), a functional constraint and a presence condition
    // Type may be missing if enumParameterized is true (then at least one DataAttribute has typeKind="ENUMERATED")
    // Type may be missing if typeKindParameterized is true  (then at least one DataAttribute has typeKind="undefined")
    // Each SubDataObject has a name, a type (name of a CDC) and a presence condition
    
    
    // This will check the presence condition of DataAttribute
    private DataAttributePresenceConditionValidator dataAttributePresenceConditionValidator;
    // This will check the presence condition of SubDataObject
    private SubDataObjectPresenceConditionValidator subDataObjectPresenceConditionValidator;
    
    // Key is DataAttribute name (the corresponding DA has the same name)
    // Value is the TypeValidator given by the DataAttribute type
    private IdentityHashMap< NsIdentificationName, TypeValidator > dataAttributeTypeValidatorMap;
    // Key is DataAttribute name (the corresponding DA has the same name)
    // Value is the FunctionalConstraintValidator given by the DataAttribute fc
    // TODO: not used?
    private IdentityHashMap< NsIdentificationName, FunctionalConstraintValidator > dataAttributeFunctionalConstraintValidatorMap;
    // Key is SubDataObject name (the corresponding SDO has the same name)
    // Value is the CDCValidator given by the SubDataObject type
    private IdentityHashMap< NsIdentificationObject, CDCValidator > subDataObjectValidatorMap;
    
    private CDCValidator( NsIdentification nsIdentification, CDC cdc, IRiseClipseConsole console ) {
        String parameter = "";
        if( cdc.isEnumParameterized() ) {
            for( DataAttribute da : cdc.getDataAttribute() ) {
                if( cdc.getParameterizedDataAttributeNames().contains( da.getName() )) {
                    if( da.isSetType() ) {
                        parameter = " for parameter " + da.getType();
                    }
                    else {
                        parameter = " not parameterized";
                    }
                }
            }
        }
        console.debug( CDC_SETUP_NSD_CATEGORY, cdc.getFilename(), cdc.getLineNumber(),
                "CDCValidator( ", cdc.getName(), parameter, " ) in namespace \"", nsIdentification, "\"" );
        this.cdc = cdc;
        this.nsIdentification = nsIdentification;
        this.dataAttributePresenceConditionValidator = DataAttributePresenceConditionValidator.get( nsIdentification, cdc );
        this.subDataObjectPresenceConditionValidator = SubDataObjectPresenceConditionValidator.get( nsIdentification, cdc );
        this.dataAttributeTypeValidatorMap = new IdentityHashMap<>();
        this.dataAttributeFunctionalConstraintValidatorMap = new IdentityHashMap<>();
        this.subDataObjectValidatorMap = new IdentityHashMap<>();
        
        for( DataAttribute da : cdc.getDataAttribute() ) {
            NsdObject type = da.getRefersToBasicType();
            if( type == null ) {
                type = da.getRefersToEnumeration();
            }
            if( type == null ) {
                type = da.getRefersToConstructedAttribute();
            }
            if( type != null ) {
                Pair< TypeValidator, NsIdentification > typeValidator = TypeValidator.get( this.nsIdentification, type );
                if(( typeValidator != null ) && ( typeValidator.getLeft() != null )) {
                    dataAttributeTypeValidatorMap.put( NsIdentificationName.of( typeValidator.getRight(), da.getName() ), typeValidator.getLeft() );
                    console.info( CDC_SETUP_NSD_CATEGORY, da.getFilename(), da.getLineNumber(),
                                  "type validator for DataAttribute ", da.getName(), " found with type ", da.getType(),
                                  " in namespace \"", typeValidator.getRight(), "\"" );
                }
                else {
                    console.warning( CDC_SETUP_NSD_CATEGORY, da.getFilename(), da.getLineNumber(),
                                     "type validator not found for DataAttribute ", da.getName(), " with type ", da.getType(),
                                     " in namespace \"", this.nsIdentification, "\"" );
                }
            }
            else {
                console.warning( CDC_SETUP_NSD_CATEGORY, da.getFilename(), da.getLineNumber(),
                                 "Type not found for DataAttribute ", da.getName(),
                                 " in namespace \"", this.nsIdentification, "\"" );
            }
            
            FunctionalConstraintValidator fcValidator = FunctionalConstraintValidator.get( FCEnum.getByName( da.getFc() ));
            if( fcValidator != null ) {
                dataAttributeFunctionalConstraintValidatorMap.put( NsIdentificationName.of( this.nsIdentification, da.getName() ), fcValidator );
                console.info( CDC_SETUP_NSD_CATEGORY, da.getFilename(), da.getLineNumber(),
                              "Functional constraint validator for DataAttribute " + da.getName() + " found with fc " + da.getFc(),
                              " in namespace \"", this.nsIdentification, "\"" );
            }
            else {
                console.warning( CDC_SETUP_NSD_CATEGORY, da.getFilename(), da.getLineNumber(),
                                 "Functional Constraint unknown for DataAttribute ", da.getName(), " with fc ", da.getFc(),
                                 " in namespace \"", this.nsIdentification, "\"" );
            }
        }
        
        for( SubDataObject sdo : cdc.getSubDataObject() ) {
            if( sdo.getRefersToCDC() == null ) {
                console.warning( CDC_SETUP_NSD_CATEGORY, sdo.getFilename(), sdo.getLineNumber(),
                        "CDC unknown for SubDataObject ", sdo.getName(), " in namespace \"", this.nsIdentification, "\"" );
                continue;
            }
            Pair< CDCValidator, NsIdentification > cdcValidator = CDCValidator.get( this.nsIdentification, sdo.getRefersToCDC() );
            if(( cdcValidator != null ) && ( cdcValidator.getLeft() != null )) {
                subDataObjectValidatorMap.put( NsIdentificationObject.of( cdcValidator.getRight(), sdo.getRefersToCDC() ), cdcValidator.getLeft() );
                console.info( CDC_SETUP_NSD_CATEGORY, sdo.getFilename(), sdo.getLineNumber(),
                              "CDC validator for SubDataObject ", sdo.getName(), " found with type ", sdo.getType(), " in namespace \"", cdcValidator.getRight(), "\"" );
            }
            else {
                console.warning( CDC_SETUP_NSD_CATEGORY, sdo.getFilename(), sdo.getLineNumber(),
                                 "CDC not found for SubDataObject ", sdo.getName(), " in namespace \"", this.nsIdentification, "\"" );
            }
        }
        
        reset();
    }
    
    public String getName() {
        return cdc.getName();
    }

    private boolean validateDOType( DOType doType, DiagnosticChain diagnostics ) {
        if( validatedDOType.contains( doType.getId() )) return true;
        @NonNull
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.debug( CDC_VALIDATION_NSD_CATEGORY, doType.getFilename(), doType.getLineNumber(),
                       "CDCValidator( ", getName(), " ).validateDOType( ", doType.getId(), " ) in namespace \"", nsIdentification, "\"" );
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
        .forEach( sdo -> {
            if(( sdo.getNamespace() == null ) || nsIdentification.equals( NsIdentification.of( sdo.getNamespace() ))) {
                subDataObjectPresenceConditionValidator.addModelData( sdo, sdo.getName(), diagnostics );
            }
            else {
                RiseClipseMessage warning = RiseClipseMessage.warning( CDC_VALIDATION_NSD_CATEGORY, sdo.getFilename(), sdo.getLineNumber(), 
                        "Presence condition of SDO ", sdo.getName(),
                        " is not checked because its namespace \"", sdo.getNamespace(),
                        "\" is not the same as the namespace of its DOType \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        warning.getMessage(),
                        new Object[] { sdo, warning } ));
            }
        });
        
        res = subDataObjectPresenceConditionValidator.validate( doType, diagnostics ) && res;
        
        for( DA da : doType.getDA() ) {
            TypeValidator typeValidator = null;
            NsIdentification nsId = nsIdentification;
            while(( typeValidator == null ) && ( nsId != null )) {
                typeValidator = dataAttributeTypeValidatorMap.get( NsIdentificationName.of( nsId, da.getName() ));
                nsId = nsId.getDependsOn();
            }
            if( typeValidator != null ) {
                typeValidator.validateAbstractDataAttribute( da, diagnostics );
            }
            else {
                String daType = ( da.getType() == null ) ? ( " of bType " + da.getBType() ) : ( " of type " + da.getType() );
                RiseClipseMessage error = RiseClipseMessage.warning( CDC_VALIDATION_NSD_CATEGORY, da.getFilename(), da.getLineNumber(), 
                        "DA ", da.getName(), daType,
                        " cannot be verified because there is no validator for it in namespace \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        error.getMessage(),
                        new Object[] { da, error } ));
            }

            FunctionalConstraintValidator fcValidator = FunctionalConstraintValidator.get( da.getFc() );
            if( fcValidator != null ) {
                fcValidator.validateAbstractDataAttribute( da, diagnostics );
            }
            else {
                RiseClipseMessage error = RiseClipseMessage.warning( CDC_VALIDATION_NSD_CATEGORY, da.getFilename(), da.getLineNumber(), 
                        "FunctionalConstraint ", da.getFc(), " of DA " + da.getName(),
                        " cannot be verified because there is no validator for it in namespace \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        error.getMessage(),
                        new Object[] { da, error } ));
            }
        }
      
        for( SDO sdo : doType.getSDO() ) {
            NsIdentification nsId = nsIdentification;
            if( sdo.getNamespace() != null ) {
                nsId = NsIdentification.of( sdo.getNamespace() );
            }
            CDCValidator cdcValidator = null;
            while(( cdcValidator == null ) && ( nsId != null )) {
                cdcValidator = subDataObjectValidatorMap.get( NsIdentificationObject.of( nsIdentification, sdo.getRefersToDOType() ));
                nsId = nsId.getDependsOn();
            }
            if( cdcValidator != null ) {
                if( sdo.getRefersToDOType() != null ) {
                    res = cdcValidator.validateDOType( sdo.getRefersToDOType(), diagnostics ) && res;
                }
                else {
                    RiseClipseMessage warning = RiseClipseMessage.warning( CDC_VALIDATION_NSD_CATEGORY, doType.getFilename(), doType.getLineNumber(), 
                            "while validating DOType: DOType for SDO ", sdo.getName(), " not found in namespace \"", nsId, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { doType, warning } ));
                }
            }
            else {
                nsId = nsIdentification;
                if( sdo.getNamespace() != null ) {
                    nsId = NsIdentification.of( sdo.getNamespace() );
                }
                RiseClipseMessage warning = RiseClipseMessage.warning( CDC_VALIDATION_NSD_CATEGORY, doType.getFilename(), doType.getLineNumber(), 
                        "while validating DOType: validator for SDO ", sdo.getType(), " not found in namespace \"", nsId, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        warning.getMessage(),
                        new Object[] { doType, warning } ));
            }
        }

        return res;
    }

    public boolean validateDO( DO do_, DiagnosticChain diagnostics ) {
        @NonNull
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.debug( CDC_VALIDATION_NSD_CATEGORY, do_.getLineNumber(),
                "CDCValidator( ", getName(), " ).validateDO( ", do_.getName(), " ) in namespace \"", nsIdentification, "\"" );
        DOType doType = do_.getRefersToDOType();
        if( doType == null ) {
            RiseClipseMessage error = RiseClipseMessage.warning( CDC_VALIDATION_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(), 
                    "DOType for DO \"", do_.getName(), " not found in namespace \"", nsIdentification, "\"" );
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    error.getMessage(),
                    new Object[] { do_, error } ));
            return false;
        }
        return validateDOType( doType, diagnostics );
    }
    
}
