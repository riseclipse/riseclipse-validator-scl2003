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

import java.util.Map;

import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;

import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;

public class NSDEObjectValidator implements EValidator {
    
    private Resource nsdResource;

    public NSDEObjectValidator( Resource nsdResource ) {
        this.nsdResource = nsdResource;
    }

    @Override
    public boolean validate( EObject eObject, DiagnosticChain diagnostics, Map< Object, Object > context ) {
        return validate( eObject.eClass(), eObject, diagnostics, context );
    }

    @Override
    public boolean validate( EClass eClass, EObject eObject, DiagnosticChain diagnostics,
            Map< Object, Object > context ) {
        AbstractRiseClipseConsole.getConsole().info( "NSDEObjectValidator.validate( EClass ): " + eClass.getName() );
        // TODO: use nsdResource to validate eObject
        return true;
    }

    @Override
    public boolean validate( EDataType eDataType, Object value, DiagnosticChain diagnostics,
            Map< Object, Object > context ) {
        AbstractRiseClipseConsole.getConsole().info( "NSDEObjectValidator.validate( EDataType ): " + eDataType.getName() );
        // TODO: use nsdResource to validate value
        return true;
    }

}
