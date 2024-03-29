/*
*************************************************************************
**  Copyright (c) 2021-2022 CentraleSupélec & EDF.
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
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.jdt.annotation.NonNull;

import fr.centralesupelec.edf.riseclipse.iec61850.scl.DA;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.FCEnum;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.RiseClipseMessage;

public class FunctionalConstraintValidator {

    private static final String FC_VALIDATION_NSD_CATEGORY = NsdValidator.VALIDATION_NSD_CATEGORY + "/FunctionalConstraint";

    private static HashMap< FCEnum, FunctionalConstraintValidator > validators = new HashMap<>();
    
    public static FunctionalConstraintValidator get( @NonNull FCEnum fc ) {
        return validators.get( fc );
    }
    
    static {
        
        for( FCEnum fc : FCEnum.values() ) {
            validators.put( fc, new FunctionalConstraintValidator( fc ));
        }
    }
    
    final private FCEnum code;
    
    private FunctionalConstraintValidator( FCEnum fc ) {
        this.code = fc;
    }

    public void validateAbstractDataAttribute( DA da, DiagnosticChain diagnostics ) {
        if( ! code.equals( da.getFc() )) {
            RiseClipseMessage error = RiseClipseMessage.error( FC_VALIDATION_NSD_CATEGORY, da.getFilename(), da.getLineNumber(), 
                                      "functional contraint \"", da.getFc(), "\" of DA \"", da.getName(), "\" is wrong, it should be \"", code + "\"" );
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    error.getMessage(),
                    new Object[] { da, error } ));
        }
    }

}
