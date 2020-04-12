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
import de.nextbill.domain.dtos.SingleValueDTO;
import de.nextbill.domain.enums.Right;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.Settings;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.services.AppUserService;
import de.nextbill.domain.services.SettingsService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
public class PasswordController {

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private AppUserService appUserService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private MailService mailService;

	@Autowired
	private SettingsService settingsService;

	@RequestMapping(value = "/public/passwordReset", method = RequestMethod.GET)
	public String sendPasswordKeyPublic(@RequestParam("mail") Optional<String> mailInput,
										@RequestParam("key") Optional<String> keyInput) {

		if (!mailInput.isPresent() || !keyInput.isPresent()){
			return "passwordReset";
		}

		String mail = mailInput.get();
		String key = keyInput.get();

		AppUser appUser = appUserRepository.findOneByEmail(mail);
		if (appUser == null) {
			return "redirect:/login";
		}

		if (appUser.getPasswordForgotKeyHash() == null) {
			return "redirect:/login";
		}

		if (StringUtils.isEmpty(key)) {
			return "redirect:/login";
		}

		boolean keyValid = passwordEncoder.matches(key, appUser.getPasswordForgotKeyHash());
		if (!keyValid) {
			return "redirect:/login";
		}

		return "passwordReset";
	}

	@RequestMapping(value = "/public/sendPasswordKey", method = RequestMethod.POST)
	public String sendPasswordKeyPublic(@RequestParam("mail") String mailInput) throws IOException, MessagingException {

		String mailTrimmed = mailInput.trim();

		AppUser appUser = appUserRepository.findOneByEmail(mailTrimmed);
		if (appUser == null) {
//			Is 'ok' to avoid brute force of mail adress
			return "redirect:/public/sendPassword?ok";
		}

		String generatedKey = UUID.randomUUID().toString();

		Settings settings = settingsService.getCurrentSettings();

		boolean sentMail = settings.getSmtpMailServiceEnabled();

		if (sentMail){
			Mail mail = Mail.builder()
					.isMessageTextHtml(true)
					.subject("Passwort-Wiederherstellung")
					.build();

			mail.getRecipients().add(MailRecipient.builder()
					.address(mailTrimmed).name(mailTrimmed).build());

			Map<String, Object> viewModel = new HashMap<>();
			String url = settings.getDomainUrl() + "/public/passwordReset?key="+generatedKey + "&mail="+mailTrimmed;
			viewModel.put("url", url);

			mail.setMessageTextHtml(mailService.generateMessageTemplate("passwordForgot", viewModel));

			mailService.sendMail(mail);
		}

		appUser.setPasswordForgotKeyHash(passwordEncoder.encode(generatedKey));

		appUserRepository.save(appUser);

		if (sentMail){
			return "redirect:/public/sendPassword?ok";
		}else{
			return "redirect:/public/sendPassword?error";
		}
	}

	@PreAuthorize("hasRole('EDIT_USERS')")
	@RequestMapping(value = "/webapp/api/sendPasswordKey", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<?> sendPasswordKeyInternal(@RequestBody SingleValueDTO singleValueDTO) throws IOException, MessagingException {

		String mailTrimmed = singleValueDTO.getValue().trim();

		AppUser appUser = appUserRepository.findOneByEmail(mailTrimmed);
		if (appUser == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}

		String generatedKey = UUID.randomUUID().toString();

		Settings settings = settingsService.getCurrentSettings();

		boolean sentMail = settings.getSmtpMailServiceEnabled();

		if (sentMail){
			Mail mail = Mail.builder()
					.isMessageTextHtml(true)
					.subject("Passwort-Wiederherstellung")
					.build();

			mail.getRecipients().add(MailRecipient.builder()
					.address(mailTrimmed).name(mailTrimmed).build());

			Map<String, Object> viewModel = new HashMap<>();
			String url = settings.getDomainUrl() + "/public/passwordReset?key="+generatedKey + "&mail="+mailTrimmed;
			viewModel.put("url", url);

			mail.setMessageTextHtml(mailService.generateMessageTemplate("passwordForgot", viewModel));

			mailService.sendMail(mail);
		}

		appUser.setPasswordForgotKeyHash(passwordEncoder.encode(generatedKey));

		appUserRepository.save(appUser);

		if (sentMail){
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}else{
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {

				AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(authentication.getName()));

				if (currentUser != null){
					boolean hasUserRight = currentUser.getUserRights().stream().anyMatch(t -> t.getCode().equals(Right.EDIT_USERS));
					if (hasUserRight){
						return new ResponseEntity<>(new SingleValueDTO(generatedKey), HttpStatus.OK);
					}
				}
			}
			return new ResponseEntity<>("No settings for mail configured", HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(value = "/public/forgotPassword", method = RequestMethod.POST)
	public String forgotPassword(@RequestParam("password") String password,
														  @RequestParam("passwordRepetition") String passwordRepetition,
														  @RequestParam("mail") String mailInput,
														  @RequestParam("key") String key) {

		String keyAndMailAppending = "key=" + key + "&mail=" + mailInput;

		AppUser appUser = appUserRepository.findOneByEmail(mailInput);
		if (appUser == null) {
			return "redirect:/public/passwordReset?badRequest&" + keyAndMailAppending;
		}

		if (appUser.getPasswordForgotKeyHash() == null) {
			return "redirect:/public/passwordReset?forbidden&" + keyAndMailAppending;
		}

		if (StringUtils.isEmpty(key)) {
			return "redirect:/public/passwordReset?badRequest&" + keyAndMailAppending;
		}

		boolean keyValid = passwordEncoder.matches(key, appUser.getPasswordForgotKeyHash());
		if (!keyValid) {
			return "redirect:/public/passwordReset?badRequest&" + keyAndMailAppending;
		}

		if (StringUtils.isEmpty(password) || StringUtils.isEmpty(passwordRepetition)) {
			return "redirect:/public/passwordReset?badRequest&" + keyAndMailAppending;
		}

		if (!password.equals(passwordRepetition)) {
			return "redirect:/public/passwordReset?badRequest&" + keyAndMailAppending;
		}

		appUser.setAppUserPassword(passwordEncoder.encode(password));
		appUser.setPasswordForgotKeyHash(null);
		appUserRepository.save(appUser);

		return "redirect:/login";
	}
}