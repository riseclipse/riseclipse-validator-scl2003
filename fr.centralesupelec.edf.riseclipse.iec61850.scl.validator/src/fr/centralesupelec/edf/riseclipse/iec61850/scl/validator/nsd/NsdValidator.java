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

import java.util.Map;

import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.jdt.annotation.NonNull;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.utilities.NsdModelLoader;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.Severity;

public class NsdValidator  implements EValidator {

    // Package visibility
    static final String          SETUP_NSD_CATEGORY = "NSD/Setup";
    static final String     VALIDATION_NSD_CATEGORY = "NSD/Validation";
    static final String NOTIMPLEMENTED_NSD_CATEGORY = "NSD/NotImplemented";
    
    private @NonNull NsdModelLoader nsdLoader;
    private @NonNull NsdEObjectValidator nsdEObjectValidator;

    public NsdValidator( @NonNull EPackage modelPackage ) {
        nsdLoader = new NsdModelLoader();
    }

    public void addNsdDocument( @NonNull String nsdFile, @NonNull IRiseClipseConsole console ) {
        nsdLoader.load( nsdFile, console );
    }
    
    public void prepare( @NonNull IRiseClipseConsole console, boolean displayNsdMessages ) {
        Severity level = Severity.WARNING;
        if( ! displayNsdMessages ) {
            level = console.setLevel( Severity.ERROR );            
        }
        nsdLoader.getResourceSet().finalizeLoad( console );
        nsdEObjectValidator = new NsdEObjectValidator( nsdLoader.getResourceSet(), console );
        if( ! displayNsdMessages ) {
            console.setLevel( level );            
        }
    }

    public @NonNull NsdModelLoader getNsdLoader() {
        return nsdLoader;
    }

    public void reset() {
        nsdEObjectValidator.reset();
    }

    @Override
    public boolean validate( EObject eObject, DiagnosticChain diagnostics, Map< Object, Object > context ) {
        return nsdEObjectValidator.validate( eObject, diagnostics, context );
    }

    @Override
    public boolean validate( EClass eClass, EObject eObject, DiagnosticChain diagnostics, Map< Object, Object > context ) {
        return nsdEObjectValidator.validate( eClass, eObject, diagnostics, context );
    }

    @Override
    public boolean validate( EDataType eDataType, Object value, DiagnosticChain diagnostics, Map< Object, Object > context ) {
        return nsdEObjectValidator.validate( eDataType, value, diagnostics, context );
    }

}
