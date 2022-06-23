/*
*************************************************************************
**  Copyright (c) 2016-2022 CentraleSupélec & EDF.
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

import java.util.Map;
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
        for( NsIdentification nsIdentification : nsdResourceSet.getNsIdentificationOrderedList( console )) {
            console.info( NsdValidator.SETUP_NSD_CATEGORY, 0, "Getting NSD rules for namespace \"", nsIdentification, "\"" );
            // Order is important !
            TypeValidator.buildBasicTypeValidators(
                    nsIdentification,
                    nsdResourceSet.getBasicTypeStream( nsIdentification, false ),
                    console );
            TypeValidator.builEnumerationdValidators(
                    nsIdentification,
                    nsdResourceSet.getEnumerationStream( nsIdentification, false ),
                    console );
            TypeValidator.buildConstructedAttributeValidators(
                    nsIdentification,
                    nsdResourceSet.getConstructedAttributeStream( nsIdentification, false ),
                    console );
            CDCValidator.buildValidators(
                    nsIdentification,
                    nsdResourceSet.getCDCStream( nsIdentification, false ),
                    console );
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
            public Boolean caseLNodeType( LNodeType lNodeType ) {
                AbstractRiseClipseConsole.getConsole().debug( NsdValidator.VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                                                              "NsdEObjectValidator.validate( ", lNodeType.getId(), " )" );
                return validateLNodeType( lNodeType, diagnostics );
            }

            @Override
            public Boolean defaultCase( EObject object ) {
//                AbstractRiseClipseConsole.getConsole().info( "NOT IMPLEMENTED: NsdEObjectValidator.validate( " + object.eClass().getName() + " )" );
                return true;
            }
            
        };

        return sw.doSwitch( eObject );
    }

    @Override
    public boolean validate( EDataType eDataType, Object value, DiagnosticChain diagnostics, Map< Object, Object > context ) {
//        AbstractRiseClipseConsole.getConsole().info( "[NSD validation] NOT IMPLEMENTED: NsdEObjectValidator.validate( " + eDataType.getName() + " )" );

        // TODO: use nsdResource to validate value

        return true;
    }

    private boolean validateLNodeType( LNodeType lNodeType, DiagnosticChain diagnostics ) {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.debug( NsdValidator.VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                       "NsdEObjectValidator.validateLNodeType( ", lNodeType.getLnClass(), " )" );

        boolean res = true;
        
        if( lNodeType.getNamespace() != null ) {
            res = validateLNodeType( lNodeType, lNodeType.getNamespace(), diagnostics ) && res;
        }
        else {
            RiseClipseMessage info = RiseClipseMessage.warning( NsdValidator.VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                    "LNodeType ", lNodeType.getId(), " has no namespace and cannot be validated in isolation.",
                    " It will be checked if any LN with a namespace points to it." );
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.INFO,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    info.getMessage(),
                    new Object[] { lNodeType, info } ));
        }

        if( lNodeType.getReferredByAnyLN().size() == 0 ) {
            if( lNodeType.getNamespace() != null ) return res;
            
            RiseClipseMessage warning = RiseClipseMessage.warning( NsdValidator.VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                             "LNodeType ", lNodeType.getId(), " will not be validated, no LN with a namespace points to it." );
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    warning.getMessage(),
                    new Object[] { lNodeType, warning } ));
            return false;
        }
        
        for( AnyLN ln : lNodeType.getReferredByAnyLN() ) {
            if( ln.getNamespace() != null ) {
                res = validateLNodeType( lNodeType, ln.getNamespace(), diagnostics ) && res;
            }
        }

        return res;
    }

    private boolean validateLNodeType( LNodeType lNodeType, String namespace, DiagnosticChain diagnostics ) {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.debug( NsdValidator.VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                       "NsdEObjectValidator.validateLNodeType( ", lNodeType.getId(), " in namespace ", namespace );

        NsIdentification id = NsIdentification.of( namespace );
        if( nsdResourceSet.getNS( id ) == null ) {
            RiseClipseMessage warning = RiseClipseMessage.warning( NsdValidator.VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                      "Cannot validate LNodeType ", lNodeType.getId(), " in namespace \"", namespace, "\" because this namespace is unknown" );
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.WARNING,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    warning.getMessage(),
                    new Object[] { lNodeType, warning } ));
            return false;
        }
        // Check that LNodeType has a known LNClass in the given namespace
        Pair< LNClassValidator, NsIdentification > lnClassValidator = LNClassValidator.get( id, lNodeType.getLnClass() );
        if( lnClassValidator.getLeft() != null ) {
            console.notice( NsdValidator.VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(),
                           "LNClass ", lNodeType.getLnClass(), " found for LNodeType in namespace \"" + lnClassValidator.getRight() + "\"" );

            return lnClassValidator.getLeft().validateLNodeType( lNodeType, diagnostics );
        }
        
        RiseClipseMessage error = RiseClipseMessage.error( NsdValidator.VALIDATION_NSD_CATEGORY, lNodeType.getFilename(), lNodeType.getLineNumber(), 
                  "LNClass ", lNodeType.getLnClass(), " not found for LNodeType in namespace \"", namespace, "\"" );
        diagnostics.add( new BasicDiagnostic(
                Diagnostic.ERROR,
                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                0,
                error.getMessage(),
                new Object[] { lNodeType, error } ));
        return false;
    }
}
