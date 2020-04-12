/*
 * NextBill server application
 *
 * @author Michael Roedel
 * Copyright (c) 2020 Michael Roedel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.nextbill.domain.services;

import de.nextbill.domain.model.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.*;
import java.util.Collections;
import java.util.EnumSet;

@Service
public class PathService {

    @Value("${internal.paths.billings}")
    private String billingsPath;

    @Value("${internal.paths.invoices}")
    private String invoicesPath;

    @Value("${internal.paths.temp}")
    private String tempPath;

    @Autowired
    private SettingsService settingsService;

    public void initPaths(Settings settings) throws IOException {

        if (settings.getFilesPath() == null){
            throw new IOException();
        }

        Path filesPath = Paths.get(settings.getFilesPath());
        Path rootFolder = Files.createDirectories(filesPath);

        boolean rootIsReadable = rootFolder.toFile().setReadable(true);
        boolean rootIsWritable = rootFolder.toFile().setWritable(true);

        Path billingsPathResult = filesPath.resolve(billingsPath);
        Path billingsFolder = Files.createDirectories(billingsPathResult);

        boolean billingsIsReadable = billingsFolder.toFile().setReadable(true);
        boolean billingsIsWritable = billingsFolder.toFile().setWritable(true);

        Path invoicesPathResult = filesPath.resolve(invoicesPath);
        Path invoicesFolder = Files.createDirectories(invoicesPathResult);

        boolean invoicesIsReadable = invoicesFolder.toFile().setReadable(true);
        boolean invoicesIsWritable = invoicesFolder.toFile().setWritable(true);

        Path tempPathResult = filesPath.resolve(tempPath);
        Path tempFolder = Files.createDirectories(tempPathResult);

        boolean tempIsReadable = tempFolder.toFile().setReadable(true);
        boolean tempIsWritable = tempFolder.toFile().setWritable(true);
    }

    public File getBillingsPath(String fileName) throws IOException {
        return getPath(billingsPath, fileName).toFile();
    }

    public File getInvoicesPath(String fileName) throws IOException {
        return getPath(invoicesPath, fileName).toFile();
    }
    public File getTempPath(String fileName) throws IOException {
        return getPath(tempPath, fileName).toFile();
    }

    private Path getPath(String pathToAppend, String fileName) throws IOException {
        Settings settings = settingsService.getCurrentSettings();
        if (settings.getFilesPath() == null || !settings.getIsCustomized()){
            throw new IOException();
        }

        Path filesPath = Paths.get(settings.getFilesPath());

        if (fileName != null){
            return filesPath.resolve(pathToAppend).resolve(fileName);
        }

        return filesPath.resolve(pathToAppend);
    }
}
