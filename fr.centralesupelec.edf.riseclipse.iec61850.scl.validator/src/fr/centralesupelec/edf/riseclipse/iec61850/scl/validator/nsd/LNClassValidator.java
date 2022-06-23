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
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.jdt.annotation.NonNull;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.AnyLNClass;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.DataObject;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.LNClass;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentification;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentificationName;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DO;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.LNodeType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.Pair;
import fr.centralesupelec.edf.riseclipse.util.RiseClipseMessage;

public class LNClassValidator {
    
    private static final String LNCLASS_SETUP_NSD_CATEGORY      = NsdValidator.SETUP_NSD_CATEGORY      + "/LNClass";
    private static final String LNCLASS_VALIDATION_NSD_CATEGORY = NsdValidator.VALIDATION_NSD_CATEGORY + "/LNClass";

    private static IdentityHashMap< NsIdentificationName, LNClassValidator > validators = new IdentityHashMap<>();
    
    public static Pair<LNClassValidator,NsIdentification> get( NsIdentification nsIdentification, String lnClassName ) {
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
        validatedLNodeType = new HashSet<>();
        
        dataObjectValidatorMap.values().stream().forEach( v -> v.reset() );
    }

    private NsIdentification nsIdentification;
    private HashSet< String > validatedLNodeType;

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
    private DataObjectPresenceConditionValidator dataObjectPresenceConditionValidator;
    // Key is DataObject name (the corresponding DO has the same name)
    // Value is the CDCValidator given by the DataObject type
    private HashMap< String, CDCValidator > dataObjectValidatorMap = new HashMap<>();

    private LNClassValidator( NsIdentification nsIdentification, AnyLNClass anyLNClass, IRiseClipseConsole console ) {
        console.debug( LNCLASS_SETUP_NSD_CATEGORY, anyLNClass.getFilename(), anyLNClass.getLineNumber(),
                       "LNClassValidator( ", anyLNClass.getName(), " ) in namespace \"", nsIdentification, "\"" );
        
        this.nsIdentification = nsIdentification;
        notStatisticalDataObjectPresenceConditionValidator = DataObjectPresenceConditionValidator.get( nsIdentification, anyLNClass, false );
        statisticalDataObjectPresenceConditionValidator = DataObjectPresenceConditionValidator.get( nsIdentification, anyLNClass, true );
        
        AnyLNClass lnClass = anyLNClass;
        while( lnClass != null ) {
            for( DataObject do_ : lnClass.getDataObject() ) {
                Pair< CDCValidator, NsIdentification > res = CDCValidator.get( this.nsIdentification, do_.getType() );
                CDCValidator cdcValidator = res.getLeft();
                if( cdcValidator != null ) {
                    if( do_.isSetUnderlyingType() ) {
                        console.notice( LNCLASS_SETUP_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(),
                                "making specific CDC ", cdcValidator.getName(), " for DataObject ", do_.getName(), " with underlyingType ", do_.getUnderlyingType() );
                        cdcValidator = cdcValidator.getParameterizedCdcValidatorFor( do_.getUnderlyingType(), this.nsIdentification, console );
                    }
                    dataObjectValidatorMap.put( do_.getName(), cdcValidator );
                    console.notice( LNCLASS_SETUP_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(),
                                    "CDC for DataObject ", do_.getName(), " found with type ", do_.getType() );
                }
                else {
                    console.warning( LNCLASS_SETUP_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(),
                                     "CDC not found for DataObject ", do_.getName(), " in namespace \"", this.nsIdentification, "\"" );
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
        console.debug( LNCLASS_VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                       "LNClassValidator.validateLNodeType( ", lNodeType.getId(), " in namespace \"", this.nsIdentification, "\"" );
        validatedLNodeType.add( lNodeType.getId() );

        boolean isStatistic = lNodeType
                .getDO()
                .stream()
                .anyMatch( d -> "ClcSrc".equals( d.getName() ));
        dataObjectPresenceConditionValidator = isStatistic  ? statisticalDataObjectPresenceConditionValidator : notStatisticalDataObjectPresenceConditionValidator;
        
        boolean res = true;

        // Each DO of an LNodeType must satisfy the presence condition of the corresponding DataObject (same name)
        // Do with another namespace are not concerned by this rule
        dataObjectPresenceConditionValidator.reset();
        lNodeType
        .getDO()
        .stream()
        .forEach( do_ -> {
            if(( do_.getNamespace() == null ) || nsIdentification.equals( NsIdentification.of( do_.getNamespace() ))) {
                dataObjectPresenceConditionValidator.addDO( do_, diagnostics );
            }
            else {
                RiseClipseMessage warning = RiseClipseMessage.warning( LNCLASS_VALIDATION_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(), 
                        "Presence condition of DO ", do_.getName(),
                        " is not checked because its namespace \"", do_.getNamespace(),
                        "\" is not the same as the namespace of its LNodeType \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        warning.getMessage(),
                        new Object[] { do_, warning } ));
            }
        });
      
        res = dataObjectPresenceConditionValidator.validate( lNodeType, diagnostics ) && res;
        
        // The type of each DO must conform to the CDC of the corresponding DataObject
        for( DO do_ : lNodeType.getDO() ) {
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
            if(( do_.getNamespace() == null ) || nsIdentification.equals( NsIdentification.of( do_.getNamespace() ))) {
                CDCValidator cdcValidator = dataObjectValidatorMap.get( names[0] );
                if( cdcValidator != null ) {
                    if(( do_.getRefersToDOType() != null ) && ! cdcValidator.getName().equals( do_.getRefersToDOType().getCdc() )) {
                        RiseClipseMessage error = RiseClipseMessage.warning( LNCLASS_VALIDATION_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(), 
                                "DOType id = ", do_.getRefersToDOType().getId(), " at line ", do_.getRefersToDOType().getLineNumber(),
                                " used by DO ", do_.getName(), " has wrong CDC ", do_.getRefersToDOType().getCdc(),
                                ", it should be ", cdcValidator.getName(), " in namespace \"", nsIdentification + "\"" );
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.WARNING,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                error.getMessage(),
                                new Object[] { do_, error } ));
                    }
                    res = cdcValidator.validateDO( do_, diagnostics ) && res;
                }
                else {
                    RiseClipseMessage warning = RiseClipseMessage.warning( LNCLASS_VALIDATION_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(), 
                            "DO ", do_.getName(), " cannot be verified because there is no validator for it in namespace \"", nsIdentification, "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { do_, warning } ));
                }
            }
            else {
                if( do_.getRefersToDOType() == null ) {
                    RiseClipseMessage error = RiseClipseMessage.warning( LNCLASS_VALIDATION_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(), 
                            "DO ", do_.getName(), " cannot be verified because its DOType is unknown" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            error.getMessage(),
                            new Object[] { do_, error } ));
                    res = false;
                    continue;
                }

                RiseClipseMessage warning = RiseClipseMessage.warning( LNCLASS_VALIDATION_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(), 
                        "DO ", do_.getName(), " cannot be checked against the CDC given by the LNClass of its AnyLN  because its namespace \"",
                        do_.getNamespace(), "\" differs from current namespace \"", nsIdentification, "\". It will be checked using the CDC ",
                        " of its DOType ", do_.getRefersToDOType().getCdc() );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        warning.getMessage(),
                        new Object[] { do_, warning } ));
                Pair< CDCValidator, NsIdentification > pair = CDCValidator.get( NsIdentification.of( do_.getNamespace() ), do_.getRefersToDOType().getCdc() );
                CDCValidator cdcValidator = pair.getLeft();
                if( cdcValidator != null ) {
                    res = cdcValidator.validateDO( do_, diagnostics ) && res;
                }
                else {
                    warning = RiseClipseMessage.warning( LNCLASS_VALIDATION_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(), 
                            "DO ", do_.getName(), " cannot be verified because there is no CDC validator for it in namespace \"" + do_.getNamespace() + "\"" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { do_, warning } ));
                    
                }
            }
        }
        
        return res;
    }
}
