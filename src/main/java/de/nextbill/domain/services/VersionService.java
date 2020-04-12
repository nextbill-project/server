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

import de.nextbill.domain.dtos.VersionDTO;
import de.nextbill.domain.model.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class VersionService {

    @Value("${build.version}")
    private String buildVersion;

    @Autowired
    private SettingsService settingsService;

    @Value("${build.breakingChangesForAndroid}")
    private String breakingChangesForAndroid;

    public String requestCurrentVersionOnGitHub() throws ParserConfigurationException, IOException, SAXException {

        URI uriPomOfCurrentVersion = UriComponentsBuilder.fromHttpUrl("http://www.mocky.io/v2/5e68e6c62f000085cad8b0b6")
                .build().encode().toUri();

        String xmlResponse = new RestTemplate().getForObject(uriPomOfCurrentVersion, String.class);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new InputSource(new StringReader(xmlResponse)));

        NodeList nodeList = doc.getDocumentElement().getElementsByTagName("version");

        String newestVersion = nodeList.item(0).getTextContent();

        Settings settings = settingsService.getCurrentSettings();
        settings.setNewestVersion(newestVersion);

        settings.setLastVersionCheck(new Date());

        settingsService.saveSettings(settings);

        return newestVersion;
    }

    public VersionDTO generateVersionInformation(String currentAndroidVersion) throws IOException, SAXException, ParserConfigurationException {

        String newestVersion = null;

        Settings settings = settingsService.getCurrentSettings();
        if (settings.getLastVersionCheck() != null) {
            LocalDateTime lastVersionCheck = LocalDateTime.ofInstant(settings.getLastVersionCheck().toInstant(), ZoneId.systemDefault());

            Date nowDate = new Date();
            LocalDateTime beforeOneDay = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).minus(24, ChronoUnit.HOURS);

            if (lastVersionCheck.isAfter(beforeOneDay)) {
                newestVersion = settings.getNewestVersion();
            }else{
                newestVersion = requestCurrentVersionOnGitHub();
            }

        }else{
            newestVersion = requestCurrentVersionOnGitHub();
        }

        VersionDTO versionDTO = new VersionDTO();
        versionDTO.setCurrentVersion(buildVersion);
        versionDTO.setNewestVersion(newestVersion);
        versionDTO.setMailSentActive(settings.getSmtpMailServiceEnabled());

        if (currentAndroidVersion != null) {
            versionDTO.setBreakingChangeForAndroid(hasBreakingChange(buildVersion, currentAndroidVersion));
        }

        return versionDTO;
    }

    private boolean hasBreakingChange(String serverVersion, String androidVersion) {
        String[] breakingChangeVersions = breakingChangesForAndroid.split(",");

        for (String breakingChangeVersion : breakingChangeVersions) {

            if (breakingChangeVersion == null || breakingChangeVersion.trim().equals("")){
                continue;
            }

            String[] versionsMixed = breakingChangeVersion.split("x");

            String serverVersionChange = versionsMixed[0];
            String androidVersionChange = versionsMixed[1];

            boolean isCurrentServerVersionEqualOrHigher = isVersion1EqualOrNewerThanVersion2(serverVersion, serverVersionChange);
            boolean isCurrentAndroidVersionEqualOrHigher = isVersion1EqualOrNewerThanVersion2(androidVersion, androidVersionChange);

            if ((isCurrentServerVersionEqualOrHigher && !isCurrentAndroidVersionEqualOrHigher) ||
                (!isCurrentServerVersionEqualOrHigher && isCurrentAndroidVersionEqualOrHigher)) {
                return true;
            }
        }

        return false;
    }

    private boolean isVersion1EqualOrNewerThanVersion2(String version1, String version2){

        String firstPartOfVersion1 = version1.split("-")[0];
        String firstPartOfVersion2 = version2.split("-")[0];

        String[] versionPartsOfVersion1 = firstPartOfVersion1.split(".");
        String[] versionPartsOfVersion2 = firstPartOfVersion2.split(".");

        if (firstPartOfVersion1.equals(firstPartOfVersion2)) {
            return true;
        }

        for (int i = 0; i < versionPartsOfVersion1.length; i++) {
            Long partOfVersion1 = Long.valueOf(versionPartsOfVersion1[i]);
            Long partOfVersion2 = Long.valueOf(versionPartsOfVersion2[i]);

            if ( partOfVersion1 < partOfVersion2 ) {
                return false;
            }
        }

        return true;
    }

}
