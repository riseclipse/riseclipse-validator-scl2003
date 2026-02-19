/*
*************************************************************************
**  Copyright (c) 2026 CentraleSupélec & EDF.
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
package fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.ui.component;

import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

public class FileRadioButton {

    private File file;
    private static ButtonGroup group = new ButtonGroup();
    private JRadioButton radioButton;

    public FileRadioButton( File file ) {
        this.file = file;
        this.radioButton = new JRadioButton( file.getName() );
        group.add( this.radioButton );
        this.radioButton.setSelected( true );
    }

    public File getFile() {
        return file;
    }

    public JRadioButton getRadioButton() {
        return radioButton;
    }
}
