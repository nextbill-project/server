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

import de.nextbill.Application;
import de.nextbill.domain.dtos.SettingsDTO;
import de.nextbill.domain.model.Settings;
import de.nextbill.domain.repositories.SettingsRepository;
import de.nextbill.domain.utils.BeanMapper;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class SettingsService {

    @Autowired
    private Environment environment;

    @Autowired
    private SettingsRepository settingsRepository;

    @Value("${server.port}")
    private String serverPort;

    @Value("${settings.folderName}")
    private String filesPath;

    @Value("${security.androidClientSecret}")
    private String androidClientSecret;

    @Autowired
    private PathService pathService;

    public SettingsDTO mapToDTO(Settings settings){
        BeanMapper beanMapper = new BeanMapper();
        return beanMapper.map(settings, SettingsDTO.class);
    }

    public Settings mapToEntity(SettingsDTO settingsDTO){
        BeanMapper beanMapper = new BeanMapper();
        return beanMapper.map(settingsDTO, Settings.class);
    }

    public boolean areSettingsInitialized() {
        List<Settings> settingsDB = settingsRepository.findAll();

        if (settingsDB.isEmpty()) {
            return false;
        }

        if (!settingsDB.get(0).getIsCustomized()) {
            return false;
        }

        return true;
    }

    public Settings getCurrentSettings() {
        List<Settings> settingsDB = settingsRepository.findAll();

        if (settingsDB.isEmpty()) {
            return initCreateSettings();
        }
        return settingsDB.get(0);
    }

    public Settings saveSettings(Settings settings) {
        return settingsRepository.save(settings);
    }

    public Settings initCreateSettings() {

        Settings settings = new Settings();
        settings.setSettingsId(UUID.randomUUID());
        settings.setIsCustomized(false);

        File file = new File(System.getProperty("user.home"));
        if (file.exists()){
            settings.setFilesPath(file.toPath().resolve("nextbill").toAbsolutePath().toString());
        }else{
            File jarDir = null;
            try {
                jarDir = new File(Application.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                settings.setFilesPath(Paths.get(jarDir.getAbsolutePath()).resolve(filesPath).toAbsolutePath().toString());
            } catch (URISyntaxException e) {
                settings.setFilesPath(Paths.get("/").resolve(filesPath).toAbsolutePath().toString());
            }

        }

        settings.setImapMailServiceEnabled(false);
        settings.setImapPath("INBOX");
        settings.setDeleteMail(false);
        settings.setSmtpMailServiceEnabled(false);
        settings.setScansioEnabled(false);
        settings.setLinksInMails(true);
        settings.setUseFirefoxForHtmlToImage(false);

        if (SystemUtils.IS_OS_MAC_OSX) {
            settings.setPathToFirefox("/Applications/Firefox.app/Contents/MacOS/firefox");
        }else if (SystemUtils.IS_OS_WINDOWS){
            settings.setPathToFirefox("C:\\Program Files (x86)\\Mozilla Firefox\\firefox.exe");
        }else {
            settings.setPathToFirefox("/usr/bin/firefox");
        }

        String domainPath = null;
        try {
            InetAddress host = InetAddress.getLocalHost();
            domainPath = "http://" + host.getHostName() + ":" + serverPort;
        } catch (UnknownHostException e) {
            domainPath = "http://localhost:" + serverPort;
        }
        settings.setDomainUrl(domainPath);

        settings.setSalt(UUID.randomUUID().toString() + UUID.randomUUID().toString());
        settings.setClientSecret(androidClientSecret);
        settings.setJwtStoreKey(UUID.randomUUID().toString());

        try {
            settings = extendIfDevEnvironment(settings);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return saveSettings(settings);
    }

    private Settings extendIfDevEnvironment(Settings settings) throws IOException {
        String[] activeProfilesTmp = environment.getActiveProfiles();

        List<String> activeProfiles = Arrays.asList(activeProfilesTmp);
        if (!activeProfiles.isEmpty() && activeProfiles.contains("dev")){
            settings = settingsRepository.save(settings);
            pathService.initPaths(settings);
            settings.setIsCustomized(true);
            settings = settingsRepository.save(settings);
        }

        return settings;
    }
}
