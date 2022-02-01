/*
*************************************************************************
**  Copyright (c) 2021 CentraleSupélec & EDF.
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

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.jdt.annotation.NonNull;

import fr.centralesupelec.edf.riseclipse.iec61850.scl.AbstractDataAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;

public class UndefinedTypeValidator extends TypeValidator {

    @Override
    public boolean validateAbstractDataAttribute( AbstractDataAttribute ada, DiagnosticChain diagnostics ) {
        @NonNull
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.debug( NsdValidator.VALIDATION_NSD_CATEGORY, ada.getLineNumber(),
                       "UndefinedTypeValidator.validateAbstractDataAttribute( ", ada.getName(), " )" );

        if( "Enum".equals( ada.getBType() )) {
            return new EnumeratedTypeValidator().validateAbstractDataAttribute( ada, diagnostics );
        }

        if( "Struct".equals( ada.getBType() )) {
            if( ada.getRefersToDAType() == null ) {
                diagnostics.add( new BasicDiagnostic(
                        Diagnostic.ERROR,
                        RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                        0,
                        "[NSD validation] type of DA/BDA \"" + ada.getName() + "\" (line = " + ada.getLineNumber() + ") is unknown",
                        new Object[] { ada } ));
                return false;
            }
            
            // TODO
            console.warning( NsdValidator.VALIDATION_NSD_CATEGORY, ada.getLineNumber(),
                             "NOT IMPLEMENTED: validation of DAType ", ada.getRefersToDAType().getId(),
                             " refered by DA/BDA \"", ada.getName() );
            return true;
        }
        
        BasicTypeValidator basicTypeValidator = BasicTypeValidator.get( ada.getBType() );
        if( basicTypeValidator != null ) {
            return basicTypeValidator.validateAbstractDataAttribute( ada, diagnostics );
        }

        diagnostics.add( new BasicDiagnostic(
                Diagnostic.ERROR,
                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                0,
                "[NSD validation] bType of DA/BDA \"" + ada.getName() + "\" (line = " + ada.getLineNumber() + ") is unknown",
                new Object[] { ada } ));
        return false;
    }

    @Override
    public void reset() {
        // Nothing
    }

}
