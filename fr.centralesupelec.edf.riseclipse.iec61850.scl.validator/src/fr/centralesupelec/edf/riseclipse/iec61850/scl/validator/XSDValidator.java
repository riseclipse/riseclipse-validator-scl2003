/*
*************************************************************************
**  Copyright (c) 2022 CentraleSupélec & EDF.
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

package fr.centralesupelec.edf.riseclipse.iec61850.scl.validator;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;

public class XSDValidator {

    private static final String VALIDATION_XSD_CATEGORY = "XSD/Validation";
    
    private static Validator xsdValidator;

    public static boolean prepare( String xsdFile ) {
        
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        
        SchemaFactory factory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );

        Source schemaFile = new StreamSource( new File( xsdFile ) );
        Schema schema;
        try {
            schema = factory.newSchema( schemaFile );
            xsdValidator = schema.newValidator();
        }
        catch( SAXException e ) {
            console.error( VALIDATION_XSD_CATEGORY, 0, "SAXException: ", e.getMessage() );
            return false;
        }
        
        xsdValidator.setErrorHandler( new ErrorHandler() {

            @Override
            public void warning( SAXParseException exception ) {
                console.warning( VALIDATION_XSD_CATEGORY, exception.getLineNumber(), exception.getMessage(),
                                 "(column: ", exception.getColumnNumber(), ")" );
            }

            @Override
            public void error( SAXParseException exception ) {
                console.error( VALIDATION_XSD_CATEGORY, exception.getLineNumber(), exception.getMessage(),
                               "(column: ", exception.getColumnNumber(), ")" );
            }

            @Override
            public void fatalError( SAXParseException exception ) {
                console.error( VALIDATION_XSD_CATEGORY, exception.getLineNumber(), exception.getMessage(),
                               "(column: ", exception.getColumnNumber(), ")" );
                console.error( VALIDATION_XSD_CATEGORY, 0, "fatal error for schema validation, stopping" );
                return;
            }
        } );

        return true;
    }

    public static void validate( String sclFile ) {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        
        xsdValidator.reset();

        try {
            Path sclPath = Paths.get( sclFile );
            Reader reader = Files.newBufferedReader( sclPath );
            removeBOM( reader );
            SAXSource source = new SAXSource( new InputSource( reader ) );
            xsdValidator.validate( source );
            reader.close();        
        }
        catch( IOException e ) {
            console.error( VALIDATION_XSD_CATEGORY, 0, "IOException: " + e.getMessage() );
        }
        catch( SAXException e ) {
            console.error( VALIDATION_XSD_CATEGORY, 0, "SAXException: " + e.getMessage() );
        }
    }
    
    // From https://stackoverflow.com/questions/5353783/why-org-apache-xerces-parsers-saxparser-does-not-skip-bom-in-utf8-encoded-xml
    
    private static char[] UTF32BE = { 0x0000, 0xFEFF };
    private static char[] UTF32LE = { 0xFFFE, 0x0000 };
    private static char[] UTF16BE = { 0xFEFF         };
    private static char[] UTF16LE = { 0xFFFE         };
    private static char[] UTF8    = { 0xEFBB, 0xBF   };

    private static boolean removeBOM( Reader reader, char[] bom ) throws IOException {
        int bomLength = bom.length;
        reader.mark( bomLength );
        char[] possibleBOM = new char[bomLength];
        reader.read( possibleBOM );
        for( int x = 0; x < bomLength; x++ ) {
            if( ( int ) bom[x] != ( int ) possibleBOM[x] ) {
                reader.reset();
                return false;
            }
        }
        return true;
    }

    private static void removeBOM( Reader reader ) throws IOException {
        if( removeBOM( reader, UTF32BE )) {
            return;
        }
        if( removeBOM( reader, UTF32LE )) {
            return;
        }
        if( removeBOM( reader, UTF16BE )) {
            return;
        }
        if( removeBOM( reader, UTF16LE )) {
            return;
        }
        if( removeBOM( reader, UTF8 )) {
            return;
        }
    }
}
