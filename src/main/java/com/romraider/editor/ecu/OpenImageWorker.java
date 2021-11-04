/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2021 RomRaider.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.romraider.editor.ecu;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.FileInputStream;
import java.text.MessageFormat;

import javax.swing.SwingWorker;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

import com.romraider.Settings;
import com.romraider.maps.Rom;
import com.romraider.util.SettingsManager;
import com.romraider.xml.DOMRomUnmarshaller;
import com.romraider.xml.RomNotFoundException;
import com.romraider.xml.ConversionLayer.ConversionLayer;
import com.romraider.xml.ConversionLayer.ConversionLayerFactory;

public class OpenImageWorker extends SwingWorker<Void, Void> {
    private final File inputFile;
   
    public OpenImageWorker(File inputFile) {
        this.inputFile = inputFile;
    }
    
    @Override
    protected Void doInBackground() throws Exception {
        ECUEditor editor = ECUEditorManager.getECUEditor();
        Settings settings = SettingsManager.getSettings();
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(true);
        DocumentBuilder docBuilder = factory.newDocumentBuilder();

        Document doc = null;
        FileInputStream fileStream;
        final String errorLoading = MessageFormat.format(
                ECUEditor.rb.getString("ERRORFILE"),
                inputFile.getName());

        try {
            editor.getStatusPanel().setStatus(
                    ECUEditor.rb.getString("STATUSPARSING"));
            setProgress(0);

            byte[] input = ECUEditor.readFile(inputFile);

            editor.getStatusPanel().setStatus(
                    ECUEditor.rb.getString("STATUSFINDING"));
            setProgress(10);

            // parse ecu definition files until result found
            for (int i = 0; i < settings.getEcuDefinitionFiles().size(); i++) {
                if (!settings.getEcuDefinitionFiles().get(i).exists()) {
                    showMessageDialog(editor,
                            MessageFormat.format(
                                    ECUEditor.rb.getString("MISSINGMOVED"),
                                    settings.getEcuDefinitionFiles().get(i).getAbsolutePath()),
                            MessageFormat.format(
                                    ECUEditor.rb.getString("MISSINGFILE"),
                                    settings.getEcuDefinitionFiles().get(i).getName()),
                            ERROR_MESSAGE);
                    continue;
                }
                File f = settings.getEcuDefinitionFiles().get(i);
                fileStream = new FileInputStream(f);
                
                Rom rom;
                
                //Check if definition is standard or
                //if it has to be converted first                
                if(ConversionLayerFactory.requiresConversionLayer(f)) {
                	ConversionLayer l = ConversionLayerFactory.getConversionLayerForFile(f);
                	if(l != null) {
                		doc = l.convertToDocumentTree(f);
                		
                		if(doc == null) {
                			fileStream.close();
                			throw new SAXParseException("Unknown file format!", null);
                		}
                	}
                	
                }        
            	//Default case
                else {
                	doc = docBuilder.parse(fileStream, f.getAbsolutePath());
                }
                              
                try {	
                    rom = new DOMRomUnmarshaller().unmarshallXMLDefinition(doc.getDocumentElement(), input, editor.getStatusPanel());
                } catch (RomNotFoundException rex) {
                    // rom was not found in current file, skip to next
                    continue;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showMessageDialog(editor,
                            ECUEditor.rb.getString("LOADEXCEPTION"),
                            errorLoading,
                            ERROR_MESSAGE);
                    continue;
                } finally {
                    // Release mem after unmarshall.
                	docBuilder.reset();
                	
                	if(doc != null)
                		doc.removeChild(doc.getDocumentElement());
                    doc = null;
                    fileStream.close();
                    System.gc();
                }
                
                editor.getStatusPanel().setStatus(
                        ECUEditor.rb.getString("POPULATING"));
                setProgress(50);

                rom.setFullFileName(inputFile);
                rom.populateTables(input, editor.getStatusPanel());

                editor.getStatusPanel().setStatus(
                        ECUEditor.rb.getString("FINALIZING"));
                setProgress(90);

                editor.addRom(rom);
                editor.refreshTableCompareMenus();

                editor.getStatusPanel().setStatus(
                        ECUEditor.rb.getString("DONELOAD"));
                setProgress(95);

                  editor.getStatusPanel().setStatus(
                          ECUEditor.rb.getString("CHECKSUM"));
                   rom.validateChecksum();
                
                setProgress(100);
                return null;
            }

            // if code executes to this point, no ROM was found, report to user
            showMessageDialog(editor,
                    ECUEditor.rb.getString("DEFNOTFOUND"),
                    errorLoading,
                    ERROR_MESSAGE);

        } catch (SAXParseException spe) {
            // catch general parsing exception - enough people don't unzip the defs that a better error message is in order
            showMessageDialog(editor,
                    ECUEditor.rb.getString("UNREADABLEDEF"),
                    errorLoading,
                    ERROR_MESSAGE);

        } catch (StackOverflowError ex) {
            // handles looped inheritance, which will use up all available memory
            showMessageDialog(editor,
                    ECUEditor.rb.getString("LOOPEDBASE"),
                    errorLoading,
                    ERROR_MESSAGE);

        } catch (OutOfMemoryError ome) {
            // handles Java heap space issues when loading multiple Roms.
            showMessageDialog(editor,
                    ECUEditor.rb.getString("OUTOFMEMORY"),
                    errorLoading,
                    ERROR_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            showMessageDialog(editor,
                    MessageFormat.format(
                            ECUEditor.rb.getString("CAUGHTEXCEPTION"),
                            ex.getMessage()),
                    errorLoading,
                    ERROR_MESSAGE);
        }
        return null;
    }

    public void propertyChange(PropertyChangeEvent evnt)
    {
        SwingWorker<?, ?> source = (SwingWorker<?, ?>) evnt.getSource();
        if (null != source && "state".equals( evnt.getPropertyName() )
                && (source.isDone() || source.isCancelled() ) )
        {
            source.removePropertyChangeListener(ECUEditorManager.getECUEditor().getStatusPanel());
        }
    }

    @Override
    public void done() {
        ECUEditor editor = ECUEditorManager.getECUEditor();
        editor.getStatusPanel().setStatus(ECUEditor.rb.getString("STATUSREADY"));
        setProgress(0);
        editor.setCursor(null);
        editor.refreshUI();
        System.gc();
    }
}