/*
*************************************************************************
**  Copyright (c) 2019-2024 CentraleSupélec & EDF.
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
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.jdt.annotation.NonNull;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.AnyLNClass;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.CDC;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.DataObject;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.LNClass;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentification;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentificationName;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DO;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.LNodeType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.RiseClipseMessage;

public class LNClassValidator {
    
    private static final String LNCLASS_SETUP_NSD_CATEGORY      = NsdValidator.SETUP_NSD_CATEGORY      + "/LNClass";
    private static final String LNCLASS_VALIDATION_NSD_CATEGORY = NsdValidator.VALIDATION_NSD_CATEGORY + "/LNClass";

    // The name of an LNClass in a namespace is unique
    private static IdentityHashMap< NsIdentificationName, LNClassValidator > validators = new IdentityHashMap<>();
    
    public static Pair< LNClassValidator, NsIdentification > get( NsIdentification nsIdentification, String lnClassName ) {
        NsIdentification nsId = nsIdentification;
        LNClassValidator lnClassValidator = null;
        while(( lnClassValidator == null ) && ( nsId != null )) {
            lnClassValidator = validators.get( NsIdentificationName.of( nsId, lnClassName ));
            nsIdentification = nsId;
            nsId = nsId.getDependsOn();
        }
        return Pair.of( lnClassValidator, nsIdentification );
    }
    
    public static void buildValidators( NsIdentification nsIdentification, Stream< LNClass > stream, IRiseClipseConsole console ) {
        stream
        .forEach( lnClass -> validators.put(
                NsIdentificationName.of( nsIdentification, lnClass.getName() ),
                new LNClassValidator( nsIdentification, lnClass, console )));
    }

    /*
     * Called before another file is validated
     */
    public static void resetValidators() {
        validators.values().stream().forEach( v -> v.reset() );
    }

    private void reset() {
        dataObjectValidatorMap.values().stream().forEach( v -> v.reset() );
    }

    private NsIdentification nsIdentification;
    // For validation of presence conditions of DO, the namespace of the DOI must be taken into account.
    // Therefore, we cannot remember that an LNodeType has already being validated
    //private HashSet< String > validatedLNodeType;

    // An LNClass defines a set of DataObject, each has a name, a type (name of a CDC) and a presence condition
    // An LNClass is referenced by an LNodeType (lnClass attribute) 
    // An LNodeType defines a set of DO, each has a name, a type (id of a DOType)
    // The association between DataObject and DO is done using their name attribute
    // A DOType has also a cdc attribute which is the name of a CDC
    
    // To validate an LNodeType:
    // - find its LNClass (LNodeType.lnClass == LNClass.name) in the namespace of the LNodeType
    //   - if the LNodeType has no namespace, use the namespaces (there may be several) of AnyLN referencing the LNodeType
    // - check whether all the DO of the LNType respect the presence conditions of the corresponding DataObject (DO.name == DataObject.name)
    //   - this cannot be done if the namespace of the DO is not the same as the namespace of the LNClass
    // - for each DO, verify that its DOType.cdc is the same as DataObject.cdc
    // - validate its DOType using the CDC
    
    // This will check the presence condition
    private DataObjectPresenceConditionValidator notStatisticalDataObjectPresenceConditionValidator;
    private DataObjectPresenceConditionValidator statisticalDataObjectPresenceConditionValidator;
    // Key is DataObject name (the corresponding DO has the same name)
    // Value is the CDCValidator given by the DataObject type
    private HashMap< String, CDCValidator > dataObjectValidatorMap = new HashMap<>();
    private HashSet< String > doWithInstanceNumber = new HashSet<>();

    private LNClassValidator( NsIdentification nsIdentification, AnyLNClass anyLNClass, IRiseClipseConsole console ) {
        console.debug( LNCLASS_SETUP_NSD_CATEGORY, anyLNClass.getFilename(), anyLNClass.getLineNumber(),
                       "LNClassValidator( ", anyLNClass.getName(), " ) in namespace \"", nsIdentification, "\"" );
        
        this.nsIdentification = nsIdentification;
        notStatisticalDataObjectPresenceConditionValidator = DataObjectPresenceConditionValidator.get( nsIdentification, anyLNClass, false );
        statisticalDataObjectPresenceConditionValidator = DataObjectPresenceConditionValidator.get( nsIdentification, anyLNClass, true );
        
        AnyLNClass lnClass = anyLNClass;
        while( lnClass != null ) {
            for( DataObject do_ : lnClass.getDataObject() ) {
                CDC cdc = do_.getRefersToCDC();
                if( cdc == null ) {
                    // Not an NSD error
//                    console.warning( LNCLASS_SETUP_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(),
//                            "CDC unknown for DataObject \"", do_.getName(), "\" in namespace \"", this.nsIdentification, "\"" );
                    continue;
                }
                
                Pair< CDCValidator, NsIdentification > res = CDCValidator.get( this.nsIdentification, cdc );
                CDCValidator cdcValidator = res.getLeft();
                if( cdcValidator != null ) {
                    dataObjectValidatorMap.put( do_.getName(), cdcValidator );
                    console.info( LNCLASS_SETUP_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(),
                                    "CDC for DataObject \"", do_.getName(), "\" found with type ", do_.getType() );
                }
                else {
                    console.warning( LNCLASS_SETUP_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(),
                                     "CDC not found for DataObject \"", do_.getName(), "\" in namespace \"", this.nsIdentification, "\"" );
                }
                if( "Mmulti"     .equals( do_.getPresCond() ) || "Omulti"     .equals( do_.getPresCond() )
                 || "MmultiRange".equals( do_.getPresCond() ) || "OmultiRange".equals( do_.getPresCond() )) {
                    doWithInstanceNumber.add( do_.getName() );
                }
            }

            lnClass = lnClass.getRefersToAbstractLNClass();
        }
        
        reset();
    }
    
    public boolean validateLNodeType( LNodeType lNodeType, Map< String, String > doNamespaces, DiagnosticChain diagnostics ) {
        @NonNull
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.debug( LNCLASS_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                       "LNClassValidator.validateLNodeType( ", lNodeType.getId(), " in namespace \"", this.nsIdentification, "\"" );
        
        boolean isStatistic = lNodeType
                .getDO()
                .stream()
                .anyMatch( d -> "ClcSrc".equals( d.getName() ));
        DataObjectPresenceConditionValidator dataObjectPresenceConditionValidator =
                isStatistic
                    ? statisticalDataObjectPresenceConditionValidator
                    : notStatisticalDataObjectPresenceConditionValidator;
        
        boolean res = true;

        // Each DO of an LNodeType must satisfy the presence condition of the corresponding DataObject (same name)
        dataObjectPresenceConditionValidator.reset();
        lNodeType
        .getDO()
        .stream()
        .filter( do_ -> nsIdentification.dependsOn( NsIdentification.of( doNamespaces.get( do_.getName() ))))
        .forEach( do_ -> {
            dataObjectPresenceConditionValidator.addDO( do_, diagnostics );
        });
      
        res = dataObjectPresenceConditionValidator.validate( lNodeType, diagnostics ) && res;
        
        // The type of each DO must conform to the CDC of the corresponding DataObject
        for( DO do_ : lNodeType.getDO() ) {
            
            // If the namespace given by the DOI is not the one used when building dataObjectValidatorMap,
            // we cannot verify the DO
            if( ! NsIdentification.of( doNamespaces.get( do_.getName() )).dependsOn( nsIdentification )) {
                RiseClipseMessage notice = RiseClipseMessage.notice( LNCLASS_VALIDATION_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(), 
                        "DO \"", do_.getName(), "\" namespace \"", doNamespaces.get( do_.getName() ), "\" is different from the LNClass namespace \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.INFO,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        notice.getMessage(),
                        new Object[] { do_, notice } ));
                res = false;
                continue;
            }
            
            // Look for first with the full name, keeping potential ending digits
            CDCValidator cdcValidator = dataObjectValidatorMap.get( do_.getName() );
            if( cdcValidator == null ) {
                if( do_.getName().matches( "[a-zA-Z]+\\d+" )) {
                    String name = do_.getName().split( "(?=\\d)", 2 )[0];
                    if( ! doWithInstanceNumber.contains( name )) {
                        RiseClipseMessage error = RiseClipseMessage.error( LNCLASS_VALIDATION_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(), 
                                "DO name \"", do_.getName(), "\" has an instance number, it shouldn't because the presCond of its corresponding DataObject is not multi" );
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                error.getMessage(),
                                new Object[] { do_, error } ));
                        res = false;
                        continue;
                    }
                    cdcValidator = dataObjectValidatorMap.get( name );

                }
            }

            if( cdcValidator != null ) {
                if(( do_.getRefersToDOType() != null ) && ! cdcValidator.getName().equals( do_.getRefersToDOType().getCdc() )) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( LNCLASS_VALIDATION_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(), 
                            "DOType id = \"", do_.getRefersToDOType().getId(), "\" at line ", do_.getRefersToDOType().getLineNumber(),
                            " used by DO \"", do_.getName(), "\" has wrong CDC \"", do_.getRefersToDOType().getCdc(),
                            "\", it should be \"", cdcValidator.getName(), "\" in namespace \"", nsIdentification + "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { do_, warning } ));
                }
                res = cdcValidator.validateDO( do_, diagnostics ) && res;
            }
            else {
                RiseClipseMessage warning = RiseClipseMessage.warning( LNCLASS_VALIDATION_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(), 
                        "DO \"", do_.getName(), "\" cannot be verified because there is no validator for it in namespace \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        warning.getMessage(),
                        new Object[] { do_, warning } ));
            }
        }
        
        return res;
    }
}
