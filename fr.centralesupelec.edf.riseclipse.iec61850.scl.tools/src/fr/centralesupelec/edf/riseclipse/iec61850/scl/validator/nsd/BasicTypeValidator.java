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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.BasicType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AbstractDataAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.Val;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;

public abstract class BasicTypeValidator extends TypeValidator {

    private static HashMap< String, BasicTypeValidator > validators = new HashMap<>();

    public static BasicTypeValidator get( BasicType basicType ) {
        return validators.get( basicType.getName() );
    }
    
    static {
        validators.put( "BOOLEAN", new BasicTypeValidator( "BOOLEAN" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                switch( value ) {
                case "0" :
                case "1" :
                case "false" :
                case "true" :
                    return true;
                default :
                    return addDiagnosticErrorIfTrue( true, value, da, diagnostics );
                }
            }
            
        });
        
        validators.put( "INT8", new BasicTypeValidator( "INT8" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                try {
                    new Byte( value );
                }
                catch( NumberFormatException e ) {
                    return addDiagnosticErrorIfTrue( true, value, da, diagnostics );
                }
                return true;
            }
            
        });
        
        validators.put( "INT16", new BasicTypeValidator( "INT16" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                try {
                    new Short( value );
                }
                catch( NumberFormatException e ) {
                    return addDiagnosticErrorIfTrue( true, value, da, diagnostics );
                }
                return true;
            }
            
        });
        
        validators.put( "INT32", new BasicTypeValidator( "INT32" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                try {
                    new Integer( value );
                }
                catch( NumberFormatException e ) {
                    return addDiagnosticErrorIfTrue( true, value, da, diagnostics );
                }
                return true;
            }
            
        });
        
        validators.put( "INT64", new BasicTypeValidator( "INT64" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                try {
                    new Long( value );
                }
                catch( NumberFormatException e ) {
                    return addDiagnosticErrorIfTrue( true, value, da, diagnostics );
                }
                return true;
            }
            
        });
        
        validators.put( "INT8U", new BasicTypeValidator( "INT8U" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                Long v;
                try {
                    v = new Long( value );
                }
                catch( NumberFormatException e ) {
                    return addDiagnosticErrorIfTrue( true, value, da, diagnostics );
                }
                return addDiagnosticErrorIfTrue(( v < 0 ) || ( v > 255 ), value, da, diagnostics );
            }
            
        });
        
        validators.put( "INT16U", new BasicTypeValidator( "INT16U" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                Long v;
                try {
                    v = new Long( value );
                }
                catch( NumberFormatException e ) {
                    return addDiagnosticErrorIfTrue( true, value, da, diagnostics );
                }
                return addDiagnosticErrorIfTrue(( v < 0 ) || ( v > 65535 ), value, da, diagnostics );
            }
            
        });
        
        validators.put( "INT32U", new BasicTypeValidator( "INT32U" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                Long v;
                try {
                    v = new Long( value );
                }
                catch( NumberFormatException e ) {
                    return addDiagnosticErrorIfTrue( true, value, da, diagnostics );
                }
                return addDiagnosticErrorIfTrue(( v < 0 ) || ( v > 4294967295L ), value, da, diagnostics );
            }
            
        });
        
        validators.put( "FLOAT32", new BasicTypeValidator( "FLOAT32" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                try {
                    new Float( value );
                }
                catch( NumberFormatException e ) {
                    return addDiagnosticErrorIfTrue( true, value, da, diagnostics );
                }
                return true;
            }
            
        });
        
        validators.put( "Octet64", new BasicTypeValidator( "Octet64" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                return addDiagnosticErrorIfTrue( value.getBytes().length > 64, value, da, diagnostics );
            }
            
        });
        
        validators.put( "VisString64", new BasicTypeValidator( "VisString64" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                // TODO: what is a VisString ?
                return addDiagnosticErrorIfTrue( value.getBytes().length > 64, value, da, diagnostics );
            }
            
        });
        
        validators.put( "VisString129", new BasicTypeValidator( "VisString129" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                // TODO: what is a VisString ?
                return addDiagnosticErrorIfTrue( value.getBytes().length > 129, value, da, diagnostics );
            }
            
        });
        
        validators.put( "VisString255", new BasicTypeValidator( "VisString255" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                // TODO: what is a VisString ?
                return addDiagnosticErrorIfTrue( value.getBytes().length > 255, value, da, diagnostics );
            }
            
        });
        
        validators.put( "Unicode255", new BasicTypeValidator( "Unicode255" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                // TODO: how do we handle Unicode ?
                return addDiagnosticErrorIfTrue( value.getBytes().length > 255, value, da, diagnostics );
            }
            
        });
        
        validators.put( "PhyComAddr", new BasicTypeValidator( "PhyComAddr" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                // TODO
                return addDiagnosticWarningNotImplemented( value, da, diagnostics );
            }
            
        });
        
        validators.put( "ObjRef", new BasicTypeValidator( "ObjRef" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                // TODO
                return addDiagnosticWarningNotImplemented( value, da, diagnostics );
            }
            
        });
        
        validators.put( "EntryID", new BasicTypeValidator( "EntryID" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                // TODO
                return addDiagnosticWarningNotImplemented( value, da, diagnostics );
            }
            
        });
        
        validators.put( "Currency", new BasicTypeValidator( "Currency" ) {
            
            final HashSet< String > ISO_4217_3_characterCurrencyCode = new HashSet< String >( Arrays.asList( 
                    "AED", "AFN", "ALL", "AMD", "ANG", "AOA", "ARS", "AUD", "AWG", "AZN", "BAM", "BBD",
                    "BDT", "BGN", "BHD", "BIF", "BMD", "BND", "BOB", "BOV", "BRL", "BSD", "BTN", "BWP",
                    "BYN", "BZD", "CAD", "CDF", "CHE", "CHF", "CHW", "CLF", "CLP", "CNY", "COP", "COU",
                    "CRC", "CUC", "CUP", "CVE", "CZK", "DJF", "DKK", "DOP", "DZD", "EGP", "ERN", "ETB",
                    "EUR", "FJD", "FKP", "GBP", "GEL", "GHS", "GIP", "GMD", "GNF", "GTQ", "GYD", "HKD",
                    "HNL", "HRK", "HTG", "HUF", "IDR", "ILS", "INR", "IQD", "IRR", "ISK", "JMD", "JOD",
                    "JPY", "KES", "KGS", "KHR", "KMF", "KPW", "KRW", "KWD", "KYD", "KZT", "LAK", "LBP",
                    "LKR", "LRD", "LSL", "LYD", "MAD", "MDL", "MGA", "MKD", "MMK", "MNT", "MOP", "MRU",
                    "MUR", "MVR", "MWK", "MXN", "MXV", "MYR", "MZN", "NAD", "NGN", "NIO", "NOK", "NPR",
                    "NZD", "OMR", "PAB", "PEN", "PGK", "PHP", "PKR", "PLN", "PYG", "QAR", "RON", "RSD",
                    "RUB", "RWF", "SAR", "SBD", "SCR", "SDG", "SEK", "SGD", "SHP", "SLL", "SOS", "SRD",
                    "SSP", "STN", "SVC", "SYP", "SZL", "THB", "TJS", "TMT", "TND", "TOP", "TRY", "TTD",
                    "TWD", "TZS", "UAH", "UGX", "USD", "USN", "UYI", "UYU", "UYW", "UZS", "VES", "VND",
                    "VUV", "WST", "XAF", "XAG", "XAU", "XBA", "XBB", "XBC", "XBD", "XCD", "XDR", "XOF",
                    "XPD", "XPF", "XPT", "XSU", "XTS", "XUA", "XXX", "YER", "ZAR", "ZMW", "ZWL", "XXX"
            ));
            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                // TODO: what means "The concrete coding shall be defined by the SCSMs." ?
                return addDiagnosticErrorIfTrue( ! ISO_4217_3_characterCurrencyCode.contains( value ), value, da, diagnostics );
            }
            
        });
        
        validators.put( "Timestamp", new BasicTypeValidator( "Timestamp" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                // TODO
                return addDiagnosticWarningNotImplemented( value, da, diagnostics );
            }
            
        });
        
        validators.put( "Quality", new BasicTypeValidator( "Quality" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                // TODO
                return addDiagnosticWarningNotImplemented( value, da, diagnostics );
            }
            
        });
        
        validators.put( "EntryTime", new BasicTypeValidator( "EntryTime" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                // TODO
                return addDiagnosticWarningNotImplemented( value, da, diagnostics );
            }
            
        });
        
        validators.put( "TrgOps", new BasicTypeValidator( "TrgOps" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                // TODO
                return addDiagnosticWarningNotImplemented( value, da, diagnostics );
            }
            
        });
        
        validators.put( "OptFlds", new BasicTypeValidator( "OptFlds" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                // TODO
                return addDiagnosticWarningNotImplemented( value, da, diagnostics );
            }
            
        });
        
        validators.put( "SvOptFlds", new BasicTypeValidator( "SvOptFlds" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                // TODO
                return addDiagnosticWarningNotImplemented( value, da, diagnostics );
            }
            
        });
        
        validators.put( "Check", new BasicTypeValidator( "Check" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                // TODO
                return addDiagnosticWarningNotImplemented( value, da, diagnostics );
            }
            
        });
        
        validators.put( "Tcmd", new BasicTypeValidator( "Tcmd" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                // TODO
                return addDiagnosticWarningNotImplemented( value, da, diagnostics );
            }
            
        });
        
        validators.put( "Dbpos", new BasicTypeValidator( "Dbpos" ) {

            @Override
            protected boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics ) {
                // TODO
                return addDiagnosticWarningNotImplemented( value, da, diagnostics );
            }
            
        });
        
    }
    
    private String name;
    
    public BasicTypeValidator( String name ) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public boolean validateAbstractDataAttribute( AbstractDataAttribute ada, DiagnosticChain diagnostics ) {
        AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] BasicTypeValidator.validateDA( " + ada.getName() + " ) at line " + ada.getLineNumber() );
        boolean res = true;
        if( ! getName().equals( ada.getBType() )) {
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    "[NSD validation] type of DA/BDA " + ada.getName() + " line = " + ada.getLineNumber() + ") is not " + getName(),
                    new Object[] { ada } ));
            res = false;
        }
        for( Val val : ada.getVal() ) {
            res = validateValue( ada, val.getValue(), diagnostics ) && res;
        }
        
        return res;
    }
    
    protected boolean addDiagnosticErrorIfTrue( boolean condition, String value, AbstractDataAttribute da, DiagnosticChain diagnostics ) {
        if( condition ) {
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    "[NSD validation] value " + value + " of Val in DA/BDA " + da + " line = " + da.getLineNumber() + ") is not a valid " + getName() + " value",
                    new Object[] { da } ));
            return false;
            
        }
        return true;
    }
    
    protected boolean addDiagnosticWarningNotImplemented( String value, AbstractDataAttribute da, DiagnosticChain diagnostics ) {
        diagnostics.add( new BasicDiagnostic(
                Diagnostic.WARNING,
                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                0,
                "[NSD validation] verification of value " + value + " of Val in DA/BDA " + da + " line = " + da.getLineNumber() + ") is not implemented for BasicType " + getName(),
                new Object[] { da } ));
        return true;
    }

    protected abstract boolean validateValue( AbstractDataAttribute da, String value, DiagnosticChain diagnostics );

}
