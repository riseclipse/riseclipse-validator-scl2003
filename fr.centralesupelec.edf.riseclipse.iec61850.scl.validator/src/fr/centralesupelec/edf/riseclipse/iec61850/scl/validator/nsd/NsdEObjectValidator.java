/*
*************************************************************************
**  Copyright (c) 2016-2024 CentraleSupélec & EDF.
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
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EValidator;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsdResourceSetImpl;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentification;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AnyLN;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.LNodeType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.util.SclSwitch;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.RiseClipseMessage;

public class NsdEObjectValidator implements EValidator {

    private NsdResourceSetImpl nsdResourceSet;

    public NsdEObjectValidator( NsdResourceSetImpl nsdResourceSet, IRiseClipseConsole console ) {
        // We keep it to improve some error messages
        this.nsdResourceSet = nsdResourceSet;
        
        // To avoid building several times the validators, we process the ordered list of NsIdentification (root first)
        
        // CDC "ENS" in 61850-7-3 is enumParameterized
        // AbstractLNClass "DomainLN" in 61850-7-4 uses it with enumeration "BehaviourModeKind" which is also defined in 61850-7-4
        // The tool makes a copy of the CDC and sets the right type to the appropriate DataAttribute, but the CDC is still in 61850-7-3
        // Validators for 61850-7-3 are built before those of 61850-7-4, and when the one for the instantiated CDC is built,
        // validator for "BehaviourModeKind" is not yet built and therefore not found
        // This is why there are several loops
        for( NsIdentification nsIdentification : nsdResourceSet.getNsIdentificationOrderedList( console )) {
            console.debug( NsdValidator.SETUP_NSD_CATEGORY, 0, "Building basic and enumeration validators in namespace \"", nsIdentification, "\"" );
            // Order is important !
            TypeValidator.buildBasicTypeValidators(
                    nsIdentification,
                    nsdResourceSet.getBasicTypeStream( nsIdentification, false ),
                    console );
            TypeValidator.builEnumerationdValidators(
                    nsIdentification,
                    nsdResourceSet.getEnumerationStream( nsIdentification, false ),
                    console );
        }
        for( NsIdentification nsIdentification : nsdResourceSet.getNsIdentificationOrderedList( console )) {
            console.debug( NsdValidator.SETUP_NSD_CATEGORY, 0, "Building constructed attributes validators in namespace \"", nsIdentification, "\"" );
            TypeValidator.buildConstructedAttributeValidators(
                    nsIdentification,
                    nsdResourceSet.getConstructedAttributeStream( nsIdentification, false ),
                    console );
        }
        for( NsIdentification nsIdentification : nsdResourceSet.getNsIdentificationOrderedList( console )) {
            console.debug( NsdValidator.SETUP_NSD_CATEGORY, 0, "Building CDC validators in namespace \"", nsIdentification, "\"" );
            CDCValidator.buildValidators(
                    nsIdentification,
                    nsdResourceSet.getCDCStream( nsIdentification, false ),
                    console );
        }
        for( NsIdentification nsIdentification : nsdResourceSet.getNsIdentificationOrderedList( console )) {
            console.debug( NsdValidator.SETUP_NSD_CATEGORY, 0, "Building LNClass validators in namespace \"", nsIdentification, "\"" );
            LNClassValidator.buildValidators(
                    nsIdentification,
                    nsdResourceSet.getLNClassStream( nsIdentification, false ),
                    console );
        }
        
        // Issue https://github.com/riseclipse/riseclipse-validator-scl2003/issues/145
        // DO/SDO names should be composed of standardised abbreviations listed in IEC 61850-7-4
        // No need to look for the namespace to find the correct version of 7-4 to apply : just use the latest one available
        
        // We take all abbreviations of all namespaces, and use a singleton validator implemented with static 
        for( NsIdentification nsIdentification : nsdResourceSet.getNsIdentificationOrderedList( console )) {
            DONameValidator.addFrom( nsdResourceSet.getAbbreviationStream( nsIdentification, false ));
        }
        
        // Issue https://github.com/riseclipse/riseclipse-validator-scl2003/issues/161
        // We need the « standard » DataObjects
        for( NsIdentification nsIdentification : nsdResourceSet.getNsIdentificationOrderedList( console )) {
            // TODO: what is a standard namespace?
            if( nsIdentification.getId().startsWith( "IEC" )) {
                StandardDOValidator.addFrom( nsdResourceSet.getLNClassStream( nsIdentification, false ));
            }
        }
    }

    /*
     * Called before another file is validated
     */
    public void reset() {
        TypeValidator.resetValidators();
        CDCValidator.resetValidators();
        LNClassValidator.resetValidators();
    }

    @Override
    public boolean validate( EObject eObject, DiagnosticChain diagnostics, Map< Object, Object > context ) {
        return validate( eObject.eClass(), eObject, diagnostics, context );
    }

    @Override
    public boolean validate( EClass eClass, EObject eObject, DiagnosticChain diagnostics, Map< Object, Object > context ) {

        SclSwitch< Boolean > sw = new SclSwitch< Boolean >() {

            @Override
            public Boolean caseAnyLN( AnyLN anyLN ) {
                // Check first for existing LNodeType
                if( anyLN.getRefersToLNodeType() == null ) {
                    // This is not an NSD error, should be detected elsewhere (OCL)
                    return false;
                }
                
                String inNamespace = anyLN.getNamespace();
                if(( inNamespace == null ) || ( inNamespace.isEmpty() )) {
                    RiseClipseMessage error = RiseClipseMessage.error( NsdValidator.VALIDATION_NSD_CATEGORY,
                            anyLN.getFilename(), anyLN.getLineNumber(),
                            "AnyLN type=\"", anyLN.getLnType(), "\" class=\"", anyLN.getLnClass(), "\" has no namespace" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            error.getMessage(),
                            new Object[] { anyLN, error } ));
                    inNamespace = null;
                }

                NsIdentification nsId = null;
                if( inNamespace != null ) {
                    nsId = NsIdentification.of( inNamespace );
                    if( nsdResourceSet.getNS( nsId ) == null ) {
                        RiseClipseMessage warning = RiseClipseMessage.warning( NsdValidator.VALIDATION_NSD_CATEGORY,
                                anyLN.getFilename(), anyLN.getLineNumber(),
                                "AnyLN type=\"", anyLN.getLnType(), "\" class=\"", anyLN.getLnClass(),
                                "\" is in an unknown namespace \"", inNamespace,
                                "\", only partial validation will be done" );
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.WARNING,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                warning.getMessage(),
                                new Object[] { anyLN, warning } ) );
                    }
                }
                
                // Presence condition validation must be done using the namespace of DOI
                HashMap< String, String > doiNamespaces = new HashMap<>(); 
                anyLN.getDOI().stream().forEach(
                    doi -> doiNamespaces.put( doi.getName(), doi.getNamespace() )
                );
                // But all DO of the LNodeType may not be present as DOI
                anyLN.getRefersToLNodeType().getDO().stream().forEach(
                    do_ -> doiNamespaces.putIfAbsent( do_.getName(), do_.getNamespace() == null ? anyLN.getNamespace() : do_.getNamespace() )
                );

                return validateLNodeType( anyLN.getRefersToLNodeType(), nsId, doiNamespaces, diagnostics );
            }

            @Override
            public Boolean defaultCase( EObject object ) {
//                AbstractRiseClipseConsole.getConsole().info( "NOT IMPLEMENTED: NsdEObjectValidator.validate( " + object.eClass().getName() + " )" );
                return true;
            }
            
        };

        return sw.doSwitch( eObject );
    }

    protected boolean validateLNodeType( LNodeType lNodeType, NsIdentification nsIdentification, Map< String, String > doNamespaces, DiagnosticChain diagnostics ) {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.debug( NsdValidator.VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                "NsdEObjectValidator.validateLNodeType( ", lNodeType.getId(), ")" );
        
        // Part of validation that can be done even if the LNClass is unknown
        lNodeType
        .getDO()
        .stream()
        .forEach( do_ -> {
            // DO.Name shall be a combination of the abbreviations listed in 7-4 NSD file
            // This must be verified even for an unknown namespace
            if( ! DONameValidator.validateDoName( do_.getName() )) {
                RiseClipseMessage warning = RiseClipseMessage.warning( LNClassValidator.LNCLASS_VALIDATION_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(), 
                        "DO name \"", do_.getName(), "\" is not composed using standardised abbreviations" );
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.WARNING,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        warning.getMessage(),
                        new Object[] { do_, warning } ));
            }
            
            // If the DO use a standard name, it must use the same CDC and respect the multi presence condition
            // The check should be OK for standard namespace, so let's do it for all
            if( StandardDOValidator.isStandardDoName( do_.getName() )) {
                if( do_.getRefersToDOType() != null ) {
                    if( ! StandardDOValidator.validateCdcOfExtendedDO( do_.getName(), do_.getRefersToDOType().getCdc() )) {
                        RiseClipseMessage warning = RiseClipseMessage.warning( LNClassValidator.LNCLASS_VALIDATION_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(), 
                                "DO name \"", do_.getName(), "\" use a standard name, but not the standard CDC, it is ", do_.getRefersToDOType().getCdc(),
                                ", it should be ", StandardDOValidator.getStandardCdcOfDataObject( do_.getName() ));
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.WARNING,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                warning.getMessage(),
                                new Object[] { do_, warning } ));
                    }
                }
                
                if( ! StandardDOValidator.isStandardDoMulti( do_.getName() ) && do_.getName().matches( "[a-zA-Z]+\\d+" )) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( LNClassValidator.LNCLASS_VALIDATION_NSD_CATEGORY, do_.getFilename(), do_.getLineNumber(), 
                            "DO name \"", do_.getName(), "\" use a standard name, but is instantiated while the standard one is not" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { do_, warning } ));
                }
            }
        });
        
        if( nsIdentification == null ) return false;
        
        Pair< LNClassValidator, NsIdentification > lnClassValidator = LNClassValidator.get( nsIdentification, lNodeType.getLnClass() );
        
        if( lnClassValidator.getLeft() == null ) {
            // Message already displayed for unknown namespaces
            if( nsdResourceSet.getNS( nsIdentification ) != null ) {
                RiseClipseMessage error = RiseClipseMessage.error( NsdValidator.VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                        "LNClassValidator ", lNodeType.getLnClass(), " not found for LNodeType in namespace \"", nsIdentification, "\"" );
                diagnostics.add( new BasicDiagnostic(
                      Diagnostic.ERROR,
                      RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                      0,
                      error.getMessage(),
                      new Object[] { lNodeType, error } ));
            }
            return false;
        }

        console.info( NsdValidator.VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                           "LNClassValidator ", lNodeType.getLnClass(), " found for LNodeType in namespace \"" + lnClassValidator.getRight() + "\"" );

        return lnClassValidator.getLeft().validateLNodeType( lNodeType, doNamespaces, diagnostics );
    }

    @Override
    public boolean validate( EDataType eDataType, Object value, DiagnosticChain diagnostics, Map< Object, Object > context ) {
        return true;
    }

}
