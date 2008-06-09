/*
 *
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2008 RomRaider.com
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
 *
 */

package com.romraider.logger.ecu.ui.swing.menubar.util;

import com.romraider.logger.ecu.profile.UserProfile;
import static com.romraider.util.ParamChecker.isNullOrEmpty;
import javax.swing.JFileChooser;
import static javax.swing.JFileChooser.DIRECTORIES_ONLY;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class FileHelper {
    private static final String USER_HOME_DIR = System.getProperty("user.home");

    private FileHelper() {
        throw new UnsupportedOperationException();
    }

    public static File getFile(String filePath) {
        return isNullOrEmpty(filePath) ? new File(USER_HOME_DIR) : new File(filePath);
    }

    public static JFileChooser getProfileFileChooser(File lastProfileFile) {
        JFileChooser fc;
        if (lastProfileFile.exists() && lastProfileFile.isFile() && lastProfileFile.getParentFile() != null) {
            fc = new JFileChooser(lastProfileFile.getParentFile().getAbsolutePath());
        } else {
            fc = new JFileChooser();
        }
        fc.setFileFilter(new UserProfileFileFilter());
        return fc;
    }

    public static String saveProfileToFile(UserProfile profile, File destinationFile) throws IOException {
        String profileFilePath = destinationFile.getAbsolutePath();
        if (!profileFilePath.endsWith(".xml")) {
            profileFilePath += ".xml";
            destinationFile = new File(profileFilePath);
        }
        FileOutputStream fos = new FileOutputStream(destinationFile);
        try {
            fos.write(profile.getBytes());
        } finally {
            fos.close();
        }
        return profileFilePath;
    }

    public static JFileChooser getLoggerOutputDirFileChooser(File lastLoggerOutputDir) {
        JFileChooser fc;
        if (lastLoggerOutputDir.exists() && lastLoggerOutputDir.isDirectory()) {
            fc = new JFileChooser(lastLoggerOutputDir.getAbsolutePath());
        } else {
            fc = new JFileChooser();
        }
        fc.setFileSelectionMode(DIRECTORIES_ONLY);
        return fc;
    }
}