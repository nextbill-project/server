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

package de.nextbill.domain.controller;

import de.nextbill.commons.mailmanager.model.Mail;
import de.nextbill.commons.mailmanager.model.MailRecipient;
import de.nextbill.commons.mailmanager.service.MailService;
import de.nextbill.domain.dtos.SettingsDTO;
import de.nextbill.domain.dtos.SettingsExtendedDTO;
import de.nextbill.domain.dtos.SingleSettingDTO;
import de.nextbill.domain.dtos.VersionDTO;
import de.nextbill.domain.model.AppRight;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.Settings;
import de.nextbill.domain.repositories.AppRightRepository;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.services.PathService;
import de.nextbill.domain.services.SettingsService;
import de.nextbill.domain.services.VersionService;
import de.nextbill.oauth.dtos.UserRoleDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;

@Controller
@RequestMapping({ "webapp/api"})
@Slf4j
public class SettingsController {

	@Autowired
	private SettingsService settingsService;

	@Autowired
	private PathService pathService;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private AppRightRepository appRightRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private MailService mailService;

	@Autowired
	private VersionService versionService;

	@PreAuthorize("hasRole('EDIT_SETTINGS')")
	@RequestMapping(value = "/settings", method = RequestMethod.PUT)
	public @ResponseBody ResponseEntity<?> updateSettings(@RequestBody SettingsDTO settingsDTO) throws IOException {

		Settings currentSettings = settingsService.getCurrentSettings();

		Settings settings = settingsService.mapToEntity(settingsDTO);
		settings.setSettingsId(currentSettings.getSettingsId());

		pathService.initPaths(settings);

		settings.setIsCustomized(true);
		settings = settingsService.saveSettings(settings);

		SettingsDTO settingsDTOResult = settingsService.mapToDTO(settings);

		return new ResponseEntity<>(settingsDTOResult, HttpStatus.OK);
	}

	@ResponseBody
	@RequestMapping(value = "/settings/searchForUpdate", method = RequestMethod.GET)
	public ResponseEntity<VersionDTO> searchForUpdate() throws ParserConfigurationException, IOException, SAXException {

		return new ResponseEntity<>(versionService.generateVersionInformation(null), HttpStatus.OK);
	}

	@RequestMapping(value = "/settings/sendTestMail", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<?> sendTestMail(@RequestBody SettingsDTO settingsDTO)  {

		try {
			String mailTrimmed = settingsDTO.getSmtpEmail().trim();

			Mail mail = Mail.builder()
					.isMessageTextHtml(true)
					.subject("Test-Mail")
					.build();

			mail.getRecipients().add(MailRecipient.builder()
					.address(mailTrimmed).name(mailTrimmed).build());

			mail.setMessageTextHtml(mailService.generateMessageTemplate("testMail", new HashMap<>()));

			mailService.sendMail(mail, true, settingsDTO.getSmtpServer(), settingsDTO.getSmtpEmail(), settingsDTO.getSmtpUser(), settingsDTO.getSmtpPassword());
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PreAuthorize("hasRole('EDIT_SETTINGS')")
	@RequestMapping(value = "/settings", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<?> getSettings() {

		Settings settings = settingsService.getCurrentSettings();

		SettingsDTO settingsDTO = settingsService.mapToDTO(settings);

		return new ResponseEntity<>(settingsDTO, HttpStatus.OK);
	}

	@RequestMapping(value = "/settings/initSetupData", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<?> initSetupData() {

		Settings settings = settingsService.getCurrentSettings();

		if (!settings.getIsCustomized()){
			SettingsDTO settingsDTO = settingsService.mapToDTO(settings);
			return new ResponseEntity<>(settingsDTO, HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@RequestMapping(value = "/settings/initSetupData", method = RequestMethod.PUT)
	public @ResponseBody ResponseEntity<?> saveInitSetupData(@RequestBody SettingsExtendedDTO settingsDTO) throws IOException {

		Settings settings = settingsService.getCurrentSettings();

		if (!settings.getIsCustomized()){
			Settings settingsNew = settingsService.mapToEntity(settingsDTO);
			settingsNew.setSettingsId(settings.getSettingsId());
			settingsService.saveSettings(settingsNew);

			pathService.initPaths(settingsNew);

			AppUser appUser = new AppUser();
			appUser.setAppUserId(UUID.randomUUID());
			appUser.setAppUserName(settingsDTO.getAdminName());
			appUser.setEmail(settingsDTO.getAdminMail());
			appUser.setAppUserPassword(passwordEncoder.encode(settingsDTO.getAdminPassword()));
			appUser.setUserRights(new HashSet<>(appRightRepository.findAll()));
			appUser = appUserRepository.save(appUser);

			Set<AppRight> appRights = appUser.getUserRights();
			List<UserRoleDTO> userRoleDTOS = new ArrayList<>();
			for (AppRight userRight : appRights) {
				userRoleDTOS.add(new UserRoleDTO("ROLE_" + userRight.getCode()));
			}

			Authentication authentication = new UsernamePasswordAuthenticationToken(appUser.getAppUserId().toString(), null, userRoleDTOS);
			SecurityContextHolder.getContext().setAuthentication(authentication);

			settingsNew.setIsCustomized(true);
			settingsService.saveSettings(settingsNew);

			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}

		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@RequestMapping(value = "/settings/scansioEnabled", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<?> getSettingsScansioEnabled() {

		Settings settings = settingsService.getCurrentSettings();

		return new ResponseEntity<>(new SingleSettingDTO(settings.getScansioEnabled()), HttpStatus.OK);
	}

	@RequestMapping(value = "/settings/mailSendEnabled", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<?> getMailSendEnabled() {

		Settings settings = settingsService.getCurrentSettings();

		return new ResponseEntity<>(new SingleSettingDTO(settings.getSmtpMailServiceEnabled()), HttpStatus.OK);
	}

	@RequestMapping(value = "/settings/isCustomized", method = RequestMethod.GET)
	public ResponseEntity<?> getSettingsIsCustomized() {

		Settings settings = settingsService.getCurrentSettings();

		return new ResponseEntity<>(new SingleSettingDTO(settings.getIsCustomized()), HttpStatus.OK);
	}
}