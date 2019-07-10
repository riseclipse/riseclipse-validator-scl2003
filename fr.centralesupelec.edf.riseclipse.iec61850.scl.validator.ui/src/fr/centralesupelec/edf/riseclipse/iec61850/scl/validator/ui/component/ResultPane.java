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

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;

@SuppressWarnings( "serial" )
public class ResultPane extends JPanel implements IRiseClipseConsole, ActionListener {
    
    private final static String newline = "\n";
    
    private ArrayList< Integer > levels;
    private ArrayList< String > messages;
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
        
        levels = new ArrayList<>();
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
            String level = "VERBOSE";
            switch( levels.get( i )) {
            case IRiseClipseConsole.VERBOSE_LEVEL:
                break;
            case IRiseClipseConsole.INFO_LEVEL:
                display = cbInfo.isSelected();
                level = "INFO";
                break;
            case IRiseClipseConsole.WARNING_LEVEL:
                display = cbWarning.isSelected();
                level = "WARNING";
                break;
            case IRiseClipseConsole.ERROR_LEVEL:
                display = cbError.isSelected();
                level = "ERROR";
                break;
            }
            if( display ) {
                String m = messages.get( i );
                if(( m.length() > 0 ) && ( m.charAt( 0 ) == '\t' )) {
                    buf.append( level + ":" + m + newline );
                }
                else {
                    buf.append( level + ":\t" + m + newline );
                }
            }
        }

        return buf.toString();
    }

    @Override
    public int setLevel( int level ) {
        // We keep all messages
        return IRiseClipseConsole.VERBOSE_LEVEL;
    }

    /**
     * Utility to create a String by concatenation of Object
     */
    private String toString( Object... objects ) {
        StringBuilder s = new StringBuilder();
        for( int i = 0; i < objects.length; ++i ) s.append( objects[i].toString() );
        return s.toString();
    }
    
    @Override
    public void verbose( Object... o ) {
        levels.add( IRiseClipseConsole.VERBOSE_LEVEL );
        messages.add( toString( o ));
    }

    @Override
    public void info( Object... o ) {
        levels.add( IRiseClipseConsole.INFO_LEVEL );
        messages.add( toString( o ));
    }

    @Override
    public void warning( Object... o ) {
        levels.add( IRiseClipseConsole.WARNING_LEVEL );
        messages.add( toString( o ));
    }

    @Override
    public void error( Object... o ) {
        levels.add( IRiseClipseConsole.ERROR_LEVEL );
        messages.add( toString( o ));
    }

    @Override
    public void fatal( Object... o ) {
        levels.add( IRiseClipseConsole.FATAL_LEVEL );
        messages.add( toString( o ));
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
                AbstractRiseClipseConsole.getConsole().error( ex.getMessage() );
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
                    AbstractRiseClipseConsole.getConsole().error( ex.getMessage() );
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

}
