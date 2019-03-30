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
package fr.centralesupelec.edf.riseclipse.iec61850.scl.validator;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EValidator;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.LNClass;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsdResourceSetImpl;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AnyLN;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;

public class NsdEObjectValidator implements EValidator {

    private NsdResourceSetImpl nsdResourceSet;
    private HashMap< String, AnyLNValidator > lnMap;

    public NsdEObjectValidator( NsdResourceSetImpl nsdResourceSet ) {
        this.nsdResourceSet = nsdResourceSet;
    }

    public void initializeValidationData() {
        this.lnMap = this.nsdResourceSet.getLNClassStream()
                .map( lnClass -> generateValidators( lnClass ) )
                .reduce( ( a, b ) -> {
                    a.putAll( b );
                    return a;
                } ).get();
    }

    private HashMap< String, AnyLNValidator > generateValidators( LNClass lnClass ) {
        HashMap< String, AnyLNValidator > lnMap = new HashMap<>();
        lnMap.put( lnClass.getName(), new AnyLNValidator( lnClass ) );
        return lnMap;
    }

    @Override
    public boolean validate( EObject eObject, DiagnosticChain diagnostics, Map< Object, Object > context ) {
        return validate( eObject.eClass(), eObject, diagnostics, context );
    }

    @Override
    public boolean validate( EClass eClass, EObject eObject, DiagnosticChain diagnostics, Map< Object, Object > context ) {

        if( this.lnMap == null ) {
            this.initializeValidationData();
        }

        switch( eClass.getName() ) {
        case "LN0":
        case "LN":
            AnyLN ln = ( AnyLN ) eObject;
            return validateLN( ln );
        default:
            return false;
        }
    }

    @Override
    public boolean validate( EDataType eDataType, Object value, DiagnosticChain diagnostics, Map< Object, Object > context ) {
        AbstractRiseClipseConsole.getConsole().info( "NOT IMPLEMENTED: NSDEObjectValidator.validate( EDataType ): " + eDataType.getName() );

        // TODO: use nsdResource to validate value

        return true;
    }

    public boolean validateLN( AnyLN ln ) {
        AbstractRiseClipseConsole.getConsole().verbose( "" );
        AbstractRiseClipseConsole.getConsole().verbose( "NSDEObjectValidator.validateLN( " + ln.getLnClass() + " )" );

        //LN has valid LNClass
        if( ! this.lnMap.containsKey( ln.getLnClass() ) ) {
            AbstractRiseClipseConsole.getConsole().error( "LNClass " + ln.getLnClass() + " not found in NSD files" );
            return false;
        }
        AbstractRiseClipseConsole.getConsole().verbose( "found LNClass " + ln.getLnClass() + " in NSD files" );

        //AnyLNValidator validates LN content
        return lnMap.get( ln.getLnClass() ).validateLN( ln );
    }

}
