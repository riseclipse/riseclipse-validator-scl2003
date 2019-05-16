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
import java.util.Map;
import java.util.Optional;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EValidator;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsdResourceSetImpl;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AnyLN;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DA;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DOType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.LNodeType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.Val;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.util.SclSwitch;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;

public class NsdEObjectValidator implements EValidator {

    private NsdResourceSetImpl nsdResourceSet;
    private HashMap< String, AnyLNValidator > anyLNValidatorMap = new HashMap<>();
    private HashMap<String,LNodeTypeValidator> lNodeTypeValidatorMap = new HashMap<>();

    public NsdEObjectValidator( NsdResourceSetImpl nsdResourceSet ) {
        this.nsdResourceSet = nsdResourceSet;
    }

    public void initializeValidationData() {
        nsdResourceSet
        .getLNClassStream()
        .forEach( lnClass -> anyLNValidatorMap.put( lnClass.getName(), new AnyLNValidator( lnClass )));

        nsdResourceSet
        .getLNClassStream()
        .forEach( lnClass -> lNodeTypeValidatorMap.put( lnClass.getName(), new LNodeTypeValidator( lnClass )));
    }

    @Override
    public boolean validate( EObject eObject, DiagnosticChain diagnostics, Map< Object, Object > context ) {
        return validate( eObject.eClass(), eObject, diagnostics, context );
    }

    @Override
    public boolean validate( EClass eClass, EObject eObject, DiagnosticChain diagnostics, Map< Object, Object > context ) {

        if( this.anyLNValidatorMap == null ) {
            this.initializeValidationData();
        }
        
        SclSwitch< Boolean > sw = new SclSwitch< Boolean >() {

            @Override
            public Boolean caseAnyLN( AnyLN anyLN ) {
                return true;//validateAnyLN( anyLN, diagnostics );
            }

            @Override
            public Boolean caseLNodeType( LNodeType lNodeType ) {
                return validateLNodeType( lNodeType, diagnostics );
            }

            @Override
            public Boolean defaultCase( EObject object ) {
                //AbstractRiseClipseConsole.getConsole().info( "NOT IMPLEMENTED: NsdEObjectValidator.validate( " + object.eClass().getName() + " )" );
                return true;
            }
            
        };

        return sw.doSwitch( eObject );
    }

    @Override
    public boolean validate( EDataType eDataType, Object value, DiagnosticChain diagnostics, Map< Object, Object > context ) {
        AbstractRiseClipseConsole.getConsole().info( "[NSD validation] NOT IMPLEMENTED: NsdEObjectValidator.validate( " + eDataType.getName() + " )" );

        // TODO: use nsdResource to validate value

        return true;
    }

    private boolean validateAnyLN( AnyLN ln, DiagnosticChain diagnostics ) {
        AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] NsdEObjectValidator.validateAnyLN( " + ln.getLnClass() + " )" );

        // Check that LN has valid LNClass
        if( ! this.anyLNValidatorMap.containsKey( ln.getLnClass() )) {
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    "[NSD validation] LNClass " + ln.getLnClass() + " not found for AnyLN at line " + ln.getLineNumber(),
                    new Object[] { ln } ));
            return false;
        }
        AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] found LNClass " + ln.getLnClass() + " for AnyLN at line " + ln.getLineNumber() );

        // AnyLNValidator validates LN content
        return anyLNValidatorMap.get( ln.getLnClass() ).validateAnyLN( ln, diagnostics );
    }

    protected Boolean validateLNodeType( LNodeType lNodeType, DiagnosticChain diagnostics ) {
        AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] NsdEObjectValidator.validateLNodeType( " + lNodeType.getLnClass() + " )" );

        // Check that LNodeType has valid LNClass
        if( this.anyLNValidatorMap.containsKey( lNodeType.getLnClass() )) {
            AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] LNClass " + lNodeType.getLnClass()
                + " found for LNodeType at line " + lNodeType.getLineNumber() );

            // LNodeTypeValidator validates LNodeType content
            return lNodeTypeValidatorMap.get( lNodeType.getLnClass() ).validateLNodeType( lNodeType, diagnostics );
        }
        
        // A specific LNodeType:
        // - must have a DO with name "NamPlt"
        // - its DOType must have a DA with name "lnNs"
        Optional< DOType > doType =
                lNodeType
                .getDO()
                .stream()
                .filter( d -> "NamPlt".equals( d.getName() ))
                .findAny()
                .map( d -> d.getRefersToDOType() );
        if( doType.isPresent() ) {
            Optional< DA > da =
                    doType
                    .get()
                    .getDA()
                    .stream()
                    .filter( d -> "lnNs".equals( d.getName() ))
                    .findAny();
            if( da.isPresent() ) {
                String value = " without value";
                if( da.get().getVal().size() > 0 ) {
                    value = " with value [";
                    for( Val v : da.get().getVal() ) {
                        value += " " + v.getValue();
                    }
                    value += " ]";
                }
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.INFO,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD validation] LNodeType at line " + lNodeType.getLineNumber() + " with lnClass " + lNodeType.getLnClass()
                            + " is specific and has DA \"lnNs\" in DO \"NamPlt\"" + value,
                        new Object[] { lNodeType } ));
                return true;
            }
                
        }

        diagnostics.add( new BasicDiagnostic(
                Diagnostic.ERROR,
                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                0,
                "[NSD validation] LNClass " + lNodeType.getLnClass() + " not found for LNodeType at line " + lNodeType.getLineNumber()
                + " and DA \"lnNs\" in DO \"NamPlt\" not found",
                new Object[] { lNodeType } ));
        return false;
    }

}
