/*
*************************************************************************
**  Copyright (c) 2019 CentraleSupélec & EDF.
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
**      http://wdi.supelec.fr/software/RiseClipse/
*************************************************************************
*/
package fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.ui.component;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.RiseClipseMessage;
import fr.centralesupelec.edf.riseclipse.util.Severity;

@SuppressWarnings( "serial" )
public class ResultPane extends JPanel implements IRiseClipseConsole, ActionListener {
    
    private static final String VALIDATOR_UI_CATEGORY = "SCL/ValidatorUI";
    
    private final static String formatString = "%1$7s: [$2s] $4s at line $3d";
    private final static Formatter formatter = new Formatter();
    private final static String newline = "\n";
    
    private ArrayList< RiseClipseMessage > messages;
    private JCheckBox cbVerbose;
    private JCheckBox cbInfo;
    private JCheckBox cbWarning;
    private JCheckBox cbError;
    private JTextArea text;

    private JButton btnOpenFile;

    private JButton btnSaveResults;

    private String filename;

    public ResultPane( String filename, boolean withButtons ) {
        this.filename = filename;
        
        messages = new ArrayList<>();

        setLayout( new BorderLayout( 0, 0 ));
        
        JPanel cbPanel = new JPanel();
        add( cbPanel, BorderLayout.NORTH );

        cbVerbose = new JCheckBox( "Verbose" );
        cbVerbose.setSelected( false );
        cbVerbose.addActionListener( this );
        cbPanel.add( cbVerbose );

        cbInfo = new JCheckBox( "Info" );
        cbInfo.setSelected( true );
        cbInfo.addActionListener( this );
        cbPanel.add( cbInfo );

        cbWarning = new JCheckBox( "Warning" );
        cbWarning.setSelected( true );
        cbWarning.addActionListener( this );
        cbPanel.add( cbWarning );

        cbError = new JCheckBox( "Error" );
        cbError.setSelected( true );
        cbError.addActionListener( this );
        cbPanel.add( cbError );
        
        text = new JTextArea( 0, 0 );
        text.setEditable( false );

        JScrollPane scrollPane = new JScrollPane( text );
        add( scrollPane, BorderLayout.CENTER );

        JPanel btnPanel = new JPanel();
        add( btnPanel, BorderLayout.SOUTH );

        if( withButtons ) {
            btnOpenFile = new JButton( "Open file" );
            btnOpenFile.addActionListener( this );
            btnPanel.add( btnOpenFile );
    
            btnSaveResults = new JButton( "Save results" );
            btnSaveResults.addActionListener( this );
            btnPanel.add( btnSaveResults );
        }
    }

    @Override
    public void paint( Graphics g ) {
        text.setText( allMesages() );

        super.paint( g );
    }
    
    private String allMesages() {
        StringBuffer buf = new StringBuffer();
        
        for( int i = 0; i < messages.size(); ++i ) {
            boolean display = cbVerbose.isSelected();
            switch( messages.get( i ).getSeverity()) {
            case VERBOSE:
                break;
            case INFO:
                display = cbInfo.isSelected();
                break;
            case WARNING:
                display = cbWarning.isSelected();
                break;
            case ERROR:
                display = cbError.isSelected();
                break;
            case FATAL:
                break;
            default:
                break;
            }
            if( display ) {
                String m = formatter.format(
                         formatString,
                         messages.get( i ).getSeverity(),
                         messages.get( i ).getCategory(),
                         messages.get( i ).getLineNumber(),
                         messages.get( i ).getMessage()
                ).toString();
                buf.append( m );
                buf.append( newline );
            }
        }

        return buf.toString();
    }

    @Override
    public Severity setLevel( Severity level ) {
        // We keep all messages
        return Severity.VERBOSE;
    }

    @Override
    public void output( RiseClipseMessage message ) {
        messages.add( message );
    }

    @Override
    public void actionPerformed( ActionEvent e ) {
        Object source = e.getSource();
        
        if(( source == cbVerbose ) || ( source == cbInfo ) || ( source == cbWarning ) || ( source == cbError )) {
            // The state of the checkbox is directly tested, so just repaint
            repaint();
            return;
        }
        
        if( source == btnOpenFile ) {
            
            JFrame frame = new JFrame( filename ); 
            frame.setBounds( 300, 300, 900, 700 );
            
            JTextArea textArea = new JTextArea();
            try {
                BufferedReader br = new BufferedReader( new FileReader( filename ));
                textArea.read( br, null );
                br.close();
            }
            catch( IOException ex ) {
                AbstractRiseClipseConsole.getConsole().error( VALIDATOR_UI_CATEGORY, 0, ex.getMessage() );
                return;
            }
            
            JScrollPane scrollPane = new JScrollPane( textArea );
            TextLineNumber tln = new TextLineNumber( textArea );
            scrollPane.setRowHeaderView( tln );
            frame.getContentPane().add( scrollPane );
            
            frame.setVisible( true );

            return;
        }
        
        if( source == btnSaveResults ) {
            JFileChooser fileChooser = new JFileChooser();
            if( fileChooser.showSaveDialog( this ) == JFileChooser.APPROVE_OPTION ) {
                File file = fileChooser.getSelectedFile();
                try {
                    BufferedWriter writer = new BufferedWriter( new FileWriter( file ));
                    writer.write( allMesages() );
                    writer.close();
                }
                catch( IOException ex ) {
                    AbstractRiseClipseConsole.getConsole().error( VALIDATOR_UI_CATEGORY, 0, ex.getMessage() );
                }
            }
            
        }
    }

    @Override
    public void displayIdenticalMessages() {
        // Not taken into account for the moment
    }

    @Override
    public void doNotDisplayIdenticalMessages() {
        // Not taken into account for the moment
    }

    @Override
    public Severity getLevel() {
        return Severity.VERBOSE;
    }

    @Override
    public String getFormatString() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String setFormatString( String formatString ) {
        // TODO Auto-generated method stub
        return null;
    }

}
