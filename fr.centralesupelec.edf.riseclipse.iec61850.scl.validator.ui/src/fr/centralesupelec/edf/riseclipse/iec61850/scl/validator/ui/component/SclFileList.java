/**
 *  Copyright (c) 2018 CentraleSupélec & EDF.
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
package fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.ui.component;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/*
 * Adapted from http://www.devx.com/tips/Tip/5342
 */
@SuppressWarnings( "serial" )
public class SclFileList extends JList< SclFileCheckBox > {
    
    protected static Border noFocusBorder = new EmptyBorder( 1, 1, 1, 1 );
    
    private DefaultListModel< SclFileCheckBox > model;

    public SclFileList() {
        model = new DefaultListModel< SclFileCheckBox >();
        setModel( model );
        
        setCellRenderer( new SclFileCellRenderer() );

        addMouseListener( new MouseAdapter() {
            public void mousePressed( MouseEvent e ) {
                int index = locationToIndex( e.getPoint() );

                if( index != -1 ) {
                    JCheckBox checkbox = getModel().getElementAt( index ).getCheckBox();
                    checkbox.setSelected( !checkbox.isSelected() );
                    repaint();
                }
            }
        } );

        setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
    }

    public void add( File file ) {
        for( int i = 0; i < model.size(); ++i ) {
            if( model.getElementAt( i ).getFile().getAbsolutePath().equals( file.getAbsolutePath() )) {
                return;
            }
        }
        
        SclFileCheckBox check = new SclFileCheckBox( file );
        model.addElement( check );
    }

    protected class SclFileCellRenderer implements ListCellRenderer< SclFileCheckBox > {

        @Override
        public Component getListCellRendererComponent( JList< ? extends SclFileCheckBox > list, SclFileCheckBox file, int index,
                boolean isSelected, boolean cellHasFocus ) {
            JCheckBox checkbox = file.getCheckBox();
            checkbox.setBackground( isSelected ? getSelectionBackground() : getBackground() );
            checkbox.setForeground( isSelected ? getSelectionForeground() : getForeground() );
            checkbox.setEnabled( isEnabled() );
            checkbox.setFont( getFont() );
            checkbox.setFocusPainted( false );
            checkbox.setBorderPainted( true );
            checkbox.setBorder( isSelected ? UIManager.getBorder( "List.focusCellHighlightBorder" ) : noFocusBorder );
            return checkbox;
        }
    }

    public ArrayList< String > getSclFiles() {
        ArrayList< String > sclFiles = new ArrayList< String >();
        
        for( int i = 0; i < model.size(); ++i ) {
            if( model.getElementAt( i ).getCheckBox().isSelected() ) {
                sclFiles.add( model.getElementAt( i ).getFile().getAbsolutePath() );
            }
        }
        
        return sclFiles;
    }

}


