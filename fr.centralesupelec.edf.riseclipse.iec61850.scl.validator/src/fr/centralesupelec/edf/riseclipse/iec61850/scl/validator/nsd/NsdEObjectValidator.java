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
                String inNamespace = anyLN.getNamespace();
                if(( inNamespace == null ) || ( inNamespace.isEmpty() ) ) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( NsdValidator.VALIDATION_NSD_CATEGORY,
                            anyLN.getFilename(), anyLN.getLineNumber(),
                            "AnyLN type=\"", anyLN.getLnType(), "\" class=\"", anyLN.getLnClass(), "\" has no namespace" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { anyLN, warning } ) );
                    return true;
                }

                NsIdentification nsId = NsIdentification.of( inNamespace );
                if( nsdResourceSet.getNS( nsId ) == null ) {
                    RiseClipseMessage warning = RiseClipseMessage.warning( NsdValidator.VALIDATION_NSD_CATEGORY,
                            anyLN.getFilename(), anyLN.getLineNumber(),
                            "Cannot validate AnyLN type=\"", anyLN.getLnType(), "\" class=\"", anyLN.getLnClass(),
                            "\" in namespace \"", inNamespace,
                            "\" because this namespace is unknown" );
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.WARNING,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            warning.getMessage(),
                            new Object[] { anyLN, warning } ) );
                    return true;
                }
                
                // Presence condition validation must be done using the namespace of DOI
                HashMap< String, String > doiNamespaces = new HashMap<>(); 
                anyLN.getDOI().stream().forEach( doi -> doiNamespaces.put( doi.getName(), doi.getNamespace() ));

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

    protected boolean validateLNodeType( LNodeType lNodeType, NsIdentification inNamespace, Map< String, String > doNamespaces, DiagnosticChain diagnostics ) {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.debug( NsdValidator.VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                "NsdEObjectValidator.validateLNodeType( ", lNodeType.getId(), ")" );
        
        Pair< LNClassValidator, NsIdentification > lnClassValidator = LNClassValidator.get( inNamespace, lNodeType.getLnClass() );
        
        if( lnClassValidator.getLeft() == null ) {
            RiseClipseMessage error = RiseClipseMessage.error( NsdValidator.VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                    "LNClassValidator ", lNodeType.getLnClass(), " not found for LNodeType in namespace \"", inNamespace, "\"" );
            diagnostics.add( new BasicDiagnostic(
                  Diagnostic.ERROR,
                  RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                  0,
                  error.getMessage(),
                  new Object[] { lNodeType, error } ));
            return false;
        }

        console.notice( NsdValidator.VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                           "LNClassValidator ", lNodeType.getLnClass(), " found for LNodeType in namespace \"" + lnClassValidator.getRight() + "\"" );

        return lnClassValidator.getLeft().validateLNodeType( lNodeType, doNamespaces, diagnostics );
    }

    @Override
    public boolean validate( EDataType eDataType, Object value, DiagnosticChain diagnostics, Map< Object, Object > context ) {
        return true;
    }

}
