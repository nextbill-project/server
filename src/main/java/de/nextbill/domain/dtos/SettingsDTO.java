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

package de.nextbill.domain.dtos;

import lombok.Data;

import java.util.UUID;

@Data
public class SettingsDTO {

    private UUID settingsId;

    private Boolean isCustomized;

    private String filesPath;
    private boolean filesPathEditable;

    private String salt;
    private String clientSecret;
    private String jwtStoreKey;

    private Boolean imapMailServiceEnabled;

    private String imapServer;
    private String imapUser;
    private String imapPassword;
    private String imapEmail;
    private String imapPath;
    private Boolean deleteMail;

    private Boolean smtpMailServiceEnabled;

    private String smtpServer;
    private String smtpUser;
    private String smtpPassword;
    private String smtpEmail;

    private String domainUrl;

    private Boolean linksInMails;

    private Boolean useFirefoxForHtmlToImage;
    private String pathToFirefox;

    private Boolean scansioEnabled;
    private String scansioAccessToken;
}
