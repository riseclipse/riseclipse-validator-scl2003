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

import java.util.Optional;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.jdt.annotation.NonNull;

import fr.centralesupelec.edf.riseclipse.iec61850.scl.AbstractDataAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DAI;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.EnumType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.EnumVal;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.Val;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.RiseClipseMessage;

public class EnumeratedTypeValidator extends TypeValidator {

    @Override
    public boolean validateAbstractDataAttribute( AbstractDataAttribute ada, DiagnosticChain diagnostics ) {
        @NonNull
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.debug( NsdValidator.VALIDATION_NSD_CATEGORY, ada.getLineNumber(),
                       "EnumeratedTypeValidator.validateAbstractDataAttribute( ", ada.getName(), " ) in namespace ", "TODO" );
        
        if( ! "Enum".equals( ada.getBType() )) {
            RiseClipseMessage error = RiseClipseMessage.error( NsdValidator.VALIDATION_NSD_CATEGORY, ada.getLineNumber(), 
                    "bType of DA/BDA \"", ada.getName(), "\" is not Enum" );
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    error.getMessage(),
                    new Object[] { ada, error } ));
            return false;
        }
        
        if( ada.getRefersToEnumType() == null ) {
            // Error message has already been displayed in SCL links phase
            return false;
        }
        
        boolean res = true;
        for( DAI dai : ada.getReferredByDAI() ) {
            // name is OK because it has been used to create link DAI -> DA
            for( Val val : dai.getVal() ) {
                res = validateValue( dai, val.getValue(), ada.getRefersToEnumType(), diagnostics ) && res;
            }
        }
        return true;
    }

    private boolean validateValue( DAI dai, String value, EnumType enumType, DiagnosticChain diagnostics ) {
        boolean res = true;
        
        Optional< EnumVal > found =
                 enumType
                .getEnumVal()
                .stream()
                .filter( e -> value.equals( e.getValue() ))
                .findAny();
        
        if( ! found.isPresent() ) {
            RiseClipseMessage error = RiseClipseMessage.error( NsdValidator.VALIDATION_NSD_CATEGORY, dai.getLineNumber(), 
                    "value \"", value, "\" of DAI \"", dai.getName(), "\" is not valid for EnumType \"",
                            enumType.getId(), "\" (line = ", enumType.getLineNumber(), ")" );
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    error.getMessage(),
                    new Object[] { dai, error } ));
            res = false;
        }
        
        return res;
    }

    @Override
    public void reset() {
        // Nothing
    }

}
