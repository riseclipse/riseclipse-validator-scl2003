/*
*************************************************************************
**  Copyright (c) 2019-2021 CentraleSupélec & EDF.
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.BasicType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AbstractDataAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DAI;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.UnNaming;
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
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Boolean: {0, 1} or {false, true}
                switch( value ) {
                case "0" :
                case "1" :
                case "false" :
                case "true" :
                    return true;
                default :
                    return addDiagnosticErrorIfTrue( true, value, daOrDai, diagnostics );
                }
            }
            
        });
        
        validators.put( "INT8", new BasicTypeValidator( "INT8" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Signed integer: [-128, 127]
                try {
                    new Byte( value );
                }
                catch( NumberFormatException e ) {
                    return addDiagnosticErrorIfTrue( true, value, daOrDai, diagnostics );
                }
                return true;
            }
            
        });
        
        validators.put( "INT16", new BasicTypeValidator( "INT16" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Signed integer: [-32 768, 32 767]
                try {
                    new Short( value );
                }
                catch( NumberFormatException e ) {
                    return addDiagnosticErrorIfTrue( true, value, daOrDai, diagnostics );
                }
                return true;
            }
            
        });
        
        validators.put( "INT32", new BasicTypeValidator( "INT32" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Signed integer: [-2 147 483 648, 2 147 483 647]
                try {
                    new Integer( value );
                }
                catch( NumberFormatException e ) {
                    return addDiagnosticErrorIfTrue( true, value, daOrDai, diagnostics );
                }
                return true;
            }
            
        });
        
        validators.put( "INT64", new BasicTypeValidator( "INT64" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Signed integer: [-2**63, (2**63)-1]
                try {
                    new Long( value );
                }
                catch( NumberFormatException e ) {
                    return addDiagnosticErrorIfTrue( true, value, daOrDai, diagnostics );
                }
                return true;
            }
            
        });
        
        validators.put( "INT8U", new BasicTypeValidator( "INT8U" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Unsigned integer: [0, 255]
                Long v;
                try {
                    v = new Long( value );
                }
                catch( NumberFormatException e ) {
                    return addDiagnosticErrorIfTrue( true, value, daOrDai, diagnostics );
                }
                return addDiagnosticErrorIfTrue(( v < 0 ) || ( v > 255 ), value, daOrDai, diagnostics );
            }
            
        });
        
        validators.put( "INT16U", new BasicTypeValidator( "INT16U" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Unsigned integer: [0, 65 535]
                Long v;
                try {
                    v = new Long( value );
                }
                catch( NumberFormatException e ) {
                    return addDiagnosticErrorIfTrue( true, value, daOrDai, diagnostics );
                }
                return addDiagnosticErrorIfTrue(( v < 0 ) || ( v > 65535 ), value, daOrDai, diagnostics );
            }
            
        });
        
        validators.put( "INT32U", new BasicTypeValidator( "INT32U" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Unsigned integer: [0, 4 294 967 295]
                Long v;
                try {
                    v = new Long( value );
                }
                catch( NumberFormatException e ) {
                    return addDiagnosticErrorIfTrue( true, value, daOrDai, diagnostics );
                }
                return addDiagnosticErrorIfTrue(( v < 0 ) || ( v > 4294967295L ), value, daOrDai, diagnostics );
            }
            
        });
        
        validators.put( "FLOAT32", new BasicTypeValidator( "FLOAT32" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Single-precision floating point according to IEEE 754)
                try {
                    new Float( value );
                }
                catch( NumberFormatException e ) {
                    return addDiagnosticErrorIfTrue( true, value, daOrDai, diagnostics );
                }
                return true;
            }
            
        });
        
        validators.put( "Octet64", new BasicTypeValidator( "Octet64" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Should be able to hold up to 64 bytes. NULL string has length 0
                return addDiagnosticErrorIfTrue( value.getBytes().length > 64, value, daOrDai, diagnostics );
            }
            
            @Override
            protected boolean acceptEmptyValue() {
                return true;
            }
        });
        
        validators.put( "VisString64", new BasicTypeValidator( "VisString64" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Should be able to hold up to 64 characters. NULL string has length 0
                // IEC 61850-6:
                //   IEC 61850-7-x basic type:    VISIBLE STRING
                //   XML Schema (xs) data type:   normalizedString
                //   Value representation:        A character string without tabs, linefeeds and carriage return, restricted to
                //                                8-bit characters (ISO/IEC 8859-1 characters limited to UTF-8 single byte coding)
                boolean ok =
                        value
                        .codePoints()
                        .allMatch( codePoint -> Character.UnicodeBlock.BASIC_LATIN       .equals( Character.UnicodeBlock.of( codePoint ))
                                             || Character.UnicodeBlock.LATIN_1_SUPPLEMENT.equals( Character.UnicodeBlock.of( codePoint )));
                if( ok ) ok =
                           ( ! value.contains( "\t"))
                        && ( ! value.contains( "\n" ))
                        && ( ! value.contains( "\r" ));
                if( ok ) ok = value.length() <= 64;
                return addDiagnosticErrorIfTrue( ! ok, value, daOrDai, diagnostics );
            }
            
            @Override
            protected boolean acceptEmptyValue() {
                return true;
            }
        });
        
        validators.put( "VisString129", new BasicTypeValidator( "VisString129" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Should be able to hold up to 129 characters. NULL string has length 0
                boolean ok =
                        value
                        .codePoints()
                        .allMatch( codePoint -> Character.UnicodeBlock.BASIC_LATIN       .equals( Character.UnicodeBlock.of( codePoint ))
                                             || Character.UnicodeBlock.LATIN_1_SUPPLEMENT.equals( Character.UnicodeBlock.of( codePoint )));
                if( ok ) ok =
                           ( ! value.contains( "\t"))
                        && ( ! value.contains( "\n" ))
                        && ( ! value.contains( "\r" ));
                if( ok ) ok = value.length() <= 129;
                return addDiagnosticErrorIfTrue( ! ok, value, daOrDai, diagnostics );
            }
            
            @Override
            protected boolean acceptEmptyValue() {
                return true;
            }
        });
        
        validators.put( "VisString255", new BasicTypeValidator( "VisString255" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Should be able to hold up to 255 characters. NULL string has length 0
                boolean ok =
                        value
                        .codePoints()
                        .allMatch( codePoint -> Character.UnicodeBlock.BASIC_LATIN       .equals( Character.UnicodeBlock.of( codePoint ))
                                             || Character.UnicodeBlock.LATIN_1_SUPPLEMENT.equals( Character.UnicodeBlock.of( codePoint )));
                if( ok ) ok =
                           ( ! value.contains( "\t"))
                        && ( ! value.contains( "\n" ))
                        && ( ! value.contains( "\r" ));
                if( ok ) ok = value.length() <= 255;
                return addDiagnosticErrorIfTrue( ! ok, value, daOrDai, diagnostics );
            }
            
            @Override
            protected boolean acceptEmptyValue() {
                return true;
            }
        });
        
        validators.put( "Unicode255", new BasicTypeValidator( "Unicode255" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Should be able to hold up to 255 Unicode characters. NULL string has length 0.
                // IEC 61850-6:
                //   IEC 61850-7-x basic type:    UNICODE STRING
                //   XML Schema (xs) data type:   normalizedString
                //   Value representation:        A character string without tabs, linefeeds and carriage return. All characters
                //                                in an XML file are principally Unicode, for example in UTF-8 coding
                boolean ok =
                           ( ! value.contains( "\t"))
                        && ( ! value.contains( "\n" ))
                        && ( ! value.contains( "\r" ));
                if( ok ) ok = value.length() <= 255;
                return addDiagnosticErrorIfTrue( ! ok, value, daOrDai, diagnostics );
            }
            
            @Override
            protected boolean acceptEmptyValue() {
                return true;
            }
        });
        
        validators.put( "PhyComAddr", new BasicTypeValidator( "PhyComAddr" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Type used for physical communication address (e.g. media access address, priority, and other information) as defined by a SCSM
                // TODO
                return addDiagnosticWarningNotImplemented( value, daOrDai, diagnostics );
            }
            
        });
        
        validators.put( "ObjRef", new BasicTypeValidator( "ObjRef" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Instances of classes in the hierarchical information model (ACSI class hierarchy of logical device, logical node, data, data attributes)
                // shall be constructed by the concatenation of all instance names comprising the whole path-name of an instance of a class that identifies
                // the instance uniquely.
                // This type is a case sensitive VisString129 and follows the character set of the ObjectName extended with the "/", "$", ".", "(", ")", "@".
                // The syntax of an object reference within the scope of a logical device shall be: LDName/LNName[.Name[. ...]], where the "/" shall separate 
                // the LDName from the rest of the object reference, and the "." shall separate the further names in the hierarchy.
                // - The "[. ]" indicates an option.
                // - The "[. ...]" indicates further names of recursively nested definitions.
                // - The "(…)" shall indicate an array element.
                // The NULL ObjectReference is an empty ObjectReference whose length is zero(0).
                // The syntax within the scope of a TPAA shall be: @ObjectName.
                // The constraints defined in Clause 23 on the use of this type shall be applied.
                // NOTE When this type is used in SCL, there is an additional possibility to represent references relative to the IED (instead of the full references).
                // However the appropriate on-line values shall always be absolute references.
                return addDiagnosticErrorIfTrue( ! value.matches( "@?[\\w\\$]+(/[\\w\\$]+(\\.[\\w\\$]+)*)?" ), value, daOrDai, diagnostics );
            }
            
            @Override
            protected boolean acceptEmptyValue() {
                return true;
            }
        });
        
        validators.put( "EntryID", new BasicTypeValidator( "EntryID" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // An arbitrary octet string used to identify an entry in a sequence of events such as a log or a buffered report as specified by an SCSM.
                // NOTE The EntryID (handle) allows a client to re-synchronize, for example, with the sequence of the events stored in the IED. The syntax
                // of the value is a local issue outside the scope of this standard. However the NULL EntryID used in the standard must be the octet string
                // whose octets have all the value 0 (zero) and is reserved to indicate an unassigned ID.
                // TODO
                return addDiagnosticWarningNotImplemented( value, daOrDai, diagnostics );
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
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // A currency identification code based on ISO 4217 3-character currency code. The NULL currency is represented by the 3-characters "XXX".
                // The concrete coding shall be defined by the SCSMs.
                // TODO: what means "The concrete coding shall be defined by the SCSMs." ?
                // TODO: are lower case letters accepted ?
                return addDiagnosticErrorIfTrue( ! ISO_4217_3_characterCurrencyCode.contains( value ), value, daOrDai, diagnostics );
            }
            
        });
        
        validators.put( "Timestamp", new BasicTypeValidator( "Timestamp" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // UTC time with the epoch of midnight (00:00:00) of 1970-01-01. The presentation is defined in the SCSMs.
                // The NULL time stamp has all fields set to 0 (zero).
                // The relation between a timestamp value, the synchronization of an internal time with an external time source
                // (for example, UTC time), and other information related to time model are available as requirements in Clause 21
                // TODO
                return addDiagnosticWarningNotImplemented( value, daOrDai, diagnostics );
            }
            
        });
        
        validators.put( "Quality", new BasicTypeValidator( "Quality" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Quality contains data that describe the quality of the data from the server. Quality of the data is also related to
                // the mode of a logical node. Further details can be found in IEC 61850-7-4. The different quality attributes are not independent.
                // The default value shall be applied if the functionality of the related attribute is not supported. The mapping may specify to
                // exclude the attribute from the message if it is not supported or if the default value applies.
                // TODO
                return addDiagnosticWarningNotImplemented( value, daOrDai, diagnostics );
            }
            
        });
        
        validators.put( "EntryTime", new BasicTypeValidator( "EntryTime" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // GMT time and date used internally by an IED, mainly in the context of reporting and logging. A pair of values {EntryID, EntryTime}
                // should guarantee an entry/item/row (in a report or log) to be unique. The concrete type is defined by an SCSM.
                // The NULL EntryTime has all fields set to 0 (zero).
                // NOTE In an SCSM, this EntryTime type may or may not be the same as Timestamp type used for common data classes in IEC 61850-7-3
                // and definition of compatible data object classes in IEC 61850-7-4
                // TODO
                return addDiagnosticWarningNotImplemented( value, daOrDai, diagnostics );
            }
            
        });
        
        validators.put( "TrgOps", new BasicTypeValidator( "TrgOps" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Configuration options for control blocks, used to specify one or more conditions under which reports or logs shall be generated
                // TODO
                return addDiagnosticWarningNotImplemented( value, daOrDai, diagnostics );
            }
            
        });
        
        validators.put( "OptFlds", new BasicTypeValidator( "OptFlds" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Options for formatting of reports or to indicate how report has been formatted (depending the context)
                // TODO
                return addDiagnosticWarningNotImplemented( value, daOrDai, diagnostics );
            }
            
        });
        
        validators.put( "SvOptFlds", new BasicTypeValidator( "SvOptFlds" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Options for formatting of SVCB message. Applies to SVMessage issued by both MSVCB and USVCB
                // TODO
                return addDiagnosticWarningNotImplemented( value, daOrDai, diagnostics );
            }
            
        });
        
        validators.put( "Check", new BasicTypeValidator( "Check" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Indicates what kind of checks a control object of type DPC (double point control, see IEC 61850-7-3)
                // shall perform before issuing the control operation
                // TODO
                return addDiagnosticWarningNotImplemented( value, daOrDai, diagnostics );
            }
            
        });
        
        validators.put( "Tcmd", new BasicTypeValidator( "Tcmd" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Step control kind
                // TODO
                return addDiagnosticWarningNotImplemented( value, daOrDai, diagnostics );
            }
            
        });
        
        validators.put( "Dbpos", new BasicTypeValidator( "Dbpos" ) {

            @Override
            protected boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics ) {
                // Double point status kind
                // TODO
                return addDiagnosticWarningNotImplemented( value, daOrDai, diagnostics );
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
    
    /*
     * Called before another file is validated
     */
    @Override
    public void reset() {
        // Nothing
    }
    
    @Override
    public boolean validateAbstractDataAttribute( AbstractDataAttribute ada, DiagnosticChain diagnostics ) {
        AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] BasicTypeValidator.validateAbstractDataAttribute( " + ada.getName() + " ) at line " + ada.getLineNumber() );
        boolean res = true;
        if( ! getName().equals( ada.getBType() )) {
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    "[NSD validation] type of DA/BDA \"" + ada.getName() + "\" (line = " + ada.getLineNumber() + ") is not " + getName(),
                    new Object[] { ada } ));
            res = false;
        }
        for( Val val : ada.getVal() ) {
            if( val.getValue().isEmpty() ) {
                if( ! acceptEmptyValue() ) {
                    diagnostics.add( new BasicDiagnostic(
                            Diagnostic.ERROR,
                            RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                            0,
                            "[NSD validation] empty value of Val in DA/BDA \"" + ada.getName() + "\" (line = " + ada.getLineNumber() + ") is not valid for " + getName() + " type",
                            new Object[] { ada } ));
                    res = false;
                }
            }
            else {
                res = validateValue( ada, val.getValue(), diagnostics ) && res;
            }
        }
        for( DAI dai : ada.getReferredByDAI() ) {
            // name is OK because it has been used to create link DAI -> DA
            for( Val val : dai.getVal() ) {
                if( val.getValue().isEmpty() ) {
                    if( ! acceptEmptyValue() ) {
                        diagnostics.add( new BasicDiagnostic(
                                Diagnostic.ERROR,
                                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                                0,
                                "[NSD validation] empty value of Val in DAI \"" + dai.getName() + "\" (line = " + dai.getLineNumber() + ") is not valid for " + getName() + " type",
                                new Object[] { ada } ));
                        res = false;
                    }
                }
                else {
                    res = validateValue( dai, val.getValue(), diagnostics ) && res;
                }
            }
        }
        
        return res;
    }
    
    protected boolean addDiagnosticErrorIfTrue( boolean condition, String value, UnNaming daOrDai, DiagnosticChain diagnostics ) {
        if( condition ) {
            String name = "";
            if( daOrDai instanceof AbstractDataAttribute ) name = (( AbstractDataAttribute ) daOrDai ).getName();
            if( daOrDai instanceof DAI                   ) name = (( DAI ) daOrDai ).getName();
            String msgValue = value.isEmpty() ? "empty value" : "value \"" + value + "\"";
            diagnostics.add( new BasicDiagnostic(
                    Diagnostic.ERROR,
                    RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                    0,
                    "[NSD validation] " + msgValue + " of Val in DA/BDA/DAI \"" + name + "\" (line = " + daOrDai.getLineNumber() + ") is not a valid " + getName() + " value",
                    new Object[] { daOrDai } ));
            return false;
            
        }
        return true;
    }
    
    protected boolean addDiagnosticWarningNotImplemented( String value, UnNaming daOrDai, DiagnosticChain diagnostics ) {
        String name = "";
        if( daOrDai instanceof AbstractDataAttribute ) name = (( AbstractDataAttribute ) daOrDai ).getName();
        if( daOrDai instanceof DAI                   ) name = (( DAI ) daOrDai ).getName();
        diagnostics.add( new BasicDiagnostic(
                Diagnostic.WARNING,
                RiseClipseValidatorSCL.DIAGNOSTIC_SOURCE,
                0,
                "[NSD validation] verification of value \"" + value + "\" of Val in DA/BDA/DAI \"" + name + "\" (line = " + daOrDai.getLineNumber() + ") is not implemented for BasicType " + getName(),
                new Object[] { daOrDai } ));
        return true;
    }

    protected abstract boolean validateValue( UnNaming daOrDai, String value, DiagnosticChain diagnostics );
    
    protected boolean acceptEmptyValue() {
        return false;
    }

}
