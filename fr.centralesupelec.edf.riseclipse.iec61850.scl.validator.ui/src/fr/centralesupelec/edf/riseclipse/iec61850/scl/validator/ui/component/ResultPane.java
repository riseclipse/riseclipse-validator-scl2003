/*
*************************************************************************
**  Copyright (c) 2019-2022 CentraleSupélec & EDF.
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
    
    private String formatString = "%1$s: [%2$s] %4$s at line %3$d";
    private final static String newline = "\n";
    
    private ArrayList< String > messages = new ArrayList<>();
    private ArrayList< Severity > levels = new ArrayList<>();
    private JCheckBox cbNotice;
    private JCheckBox cbInfo;
    private JCheckBox cbWarning;
    private JCheckBox cbError;
    private JTextArea text;

    private JButton btnOpenFile;

    private JButton btnSaveResults;

    private String filename;

    public ResultPane( String filename, boolean withButtons ) {
        this.filename = filename;
        
        setLayout( new BorderLayout( 0, 0 ));
        
        JPanel cbPanel = new JPanel();
        add( cbPanel, BorderLayout.NORTH );

        cbInfo = new JCheckBox( "Info" );
        cbInfo.setSelected( false );
        cbInfo.addActionListener( this );
        cbPanel.add( cbInfo );

        cbNotice = new JCheckBox( "Notice" );
        cbNotice.setSelected( false );
        cbNotice.addActionListener( this );
        cbPanel.add( cbNotice );

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
            boolean display = cbNotice.isSelected();
			switch( levels.get( i )) {
            case INFO:
                display = cbInfo.isSelected();
                break;
            case NOTICE:
                display = cbNotice.isSelected();
                break;
            case WARNING:
                display = cbWarning.isSelected();
                break;
            case ERROR:
                display = cbError.isSelected();
                break;
            default:
                break;
            }
            if( display ) {
                buf.append( messages.get( i ));
                buf.append( newline );
            }
        }

        return buf.toString();
    }

    @Override
    public Severity setLevel( Severity level ) {
        // We keep all messages except debug ones
        return Severity.INFO;
    }

    @Override
    public void output( RiseClipseMessage message ) {
    	// We need to use the current formatString
        Formatter formatter = new Formatter();
        formatter.format(
                formatString,
                message.getSeverity(),
                message.getCategory(),
                message.getLineNumber(),
                message.getMessage(),
                "", "", ""
        );
        String m = formatter.toString();
        formatter.close();
        messages.add( m );
        levels.add( message.getSeverity() );
    }

    @Override
    public void actionPerformed( ActionEvent e ) {
        Object source = e.getSource();
        
        if(( source == cbNotice ) || ( source == cbInfo ) || ( source == cbWarning ) || ( source == cbError )) {
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
        return Severity.INFO;
    }

    @Override
    public String getFormatString() {
        return formatString;
    }

    @Override
    public String setFormatString( String newFormatString ) {
        String previous = formatString;
        formatString = newFormatString;
        return previous;
    }

}
