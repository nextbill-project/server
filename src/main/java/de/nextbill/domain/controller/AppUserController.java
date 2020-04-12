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

import de.nextbill.domain.dtos.AppUserDTO;
import de.nextbill.domain.enums.BasicStatusEnum;
import de.nextbill.domain.enums.Right;
import de.nextbill.domain.exceptions.RightException;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.UserContact;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.UserContactRepository;
import de.nextbill.domain.services.AppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping({ "webapp/api","api" })
public class AppUserController {

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private UserContactRepository userContactRepository;

	@Autowired
	private AppUserService appUserService;

	@RequestMapping(value = "/appusers/findByEmail", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<?> find(@RequestBody String email) {
		AppUser appUser = appUserRepository.findOneByEmailAndDeletedIsNull(email.replace("\"", ""));

		if (appUser == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		AppUserDTO authUserDTO = appUserService.mapToDTO(appUser);

		return new ResponseEntity<>(authUserDTO, HttpStatus.OK);
	}

	@RequestMapping(value = "/appusers/findForUserContact", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<?> findByEmailDTO(@RequestBody AppUserDTO appUserDTO) {
		AppUser appUser = appUserRepository.findOneByEmailAndDeletedIsNull(appUserDTO.getEmail());

		if (appUser == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		UserContact userContact = userContactRepository.findOneByAppUserAndAppUserContact(currentUser, appUser);
		if (userContact != null){
			userContact.setBasicStatusEnum(BasicStatusEnum.OK);
			userContactRepository.save(userContact);
		}

		AppUserDTO authUserDTO = appUserService.mapToDTO(appUser);

		return new ResponseEntity<>(authUserDTO, HttpStatus.OK);
	}

	@PreAuthorize("hasRole('EDIT_USERS')")
	@RequestMapping(value = "/appusers", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<?> appUsersList() {
		List<AppUser> appUsers = appUserRepository.findAllByDeletedIsNull();

		List<AppUserDTO> appUserDTOS = new ArrayList<>();
		for (AppUser appUser : appUsers) {
			appUserDTOS.add(appUserService.mapToDTO(appUser));
		}

		return new ResponseEntity<>(appUserDTOS, HttpStatus.OK);
	}

	@PreAuthorize("hasRole('EDIT_USERS')")
	@RequestMapping(value = "/appusers/{cid}", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<?> get(@PathVariable("cid") UUID cid) {
		AppUser appUser = appUserRepository.findById(cid).orElse(null);

		if (appUser == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		AppUserDTO authUserDTO = appUserService.mapToDTO(appUser);

		return new ResponseEntity<>(authUserDTO, HttpStatus.OK);
	}

	@PreAuthorize("hasRole('EDIT_USERS')")
	@RequestMapping(value = "/appusers/{cid}", method = RequestMethod.DELETE)
	public @ResponseBody ResponseEntity<?> delete(@PathVariable("cid") UUID cid) {
		AppUser appUser = appUserRepository.findById(cid).orElse(null);

		if (appUser == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		List<AppUser> appUsers = appUserRepository.findAllByDeletedIsNull();
		if (appUsers.size() == 1) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		List<UserContact> userContacts = userContactRepository.findAllByAppUser(appUser);
		for (UserContact userContact : userContacts) {
			userContact.setBasicStatusEnum(BasicStatusEnum.DELETED);
            userContact.setEmail(null);
			userContact.setContactName("Gelöscht");
			userContactRepository.save(userContact);
		}

		userContacts = userContactRepository.findAllByAppUserContact(appUser);
		for (UserContact userContact : userContacts) {
			userContact.setBasicStatusEnum(BasicStatusEnum.DELETED);
			userContact.setEmail(null);
            userContact.setContactName("Gelöscht");
			userContactRepository.save(userContact);
		}

		appUser.setDeleted(true);
		appUser.setUserRights(new HashSet<>());
		appUser.setEmail(null);
		appUser.setAppUserPassword(UUID.randomUUID().toString());
		appUser.setPaypalName(null);
		appUser.setAppUserName("Gelöscht");
		appUserRepository.save(appUser);

		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PreAuthorize("hasRole('EDIT_USERS')")
	@RequestMapping(value = "/appusers", method = RequestMethod.PUT)
	public @ResponseBody ResponseEntity<?> updateAppUser(@RequestBody AppUserDTO appUserDTO) throws RightException {
		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		AppUser appUser = appUserRepository.findById(appUserDTO.getAppUserId()).orElse(null);;
		if (appUser == null){

			AppUser appUserTmp = appUserRepository.findOneByEmailAndDeletedIsNull(appUserDTO.getEmail());
			if (appUserTmp != null) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			appUser = new AppUser();
			appUser.setAppUserId(appUser.getAppUserId());
		}

		appUser = appUserService.mapToEntity(currentUser, appUserDTO);

		appUser = appUserRepository.save(appUser);

		AppUserDTO appUserDTOResult = appUserService.mapToDTO(appUser);

		return new ResponseEntity<>(appUserDTOResult, HttpStatus.OK);
	}

	@RequestMapping(value = "/appusers/currentUser", method = RequestMethod.PUT)
	public @ResponseBody ResponseEntity<?> updateCurrentAppUser(@RequestBody AppUserDTO appUserDTO) throws RightException {
		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		currentUser.setAppUserName(appUserDTO.getAppUserName());
		currentUser.setPaypalName(appUserDTO.getPaypalName());

		if (currentUser.getUserRights().stream().anyMatch(t -> t.getCode().equals(Right.EDIT_USERS))) {
			currentUser.setUserRights(appUserService.convertToRights(currentUser, currentUser, appUserDTO));
		}

		currentUser = appUserRepository.save(currentUser);

		AppUserDTO appUserDTOResult = appUserService.mapToDTO(currentUser);

		return new ResponseEntity<>(appUserDTOResult, HttpStatus.OK);
	}

	@RequestMapping(value = "/appusers/currentUser", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<?> currentUser() {
		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		AppUserDTO authUserDTO = appUserService.mapToDTO(currentUser);

		return new ResponseEntity<>(authUserDTO, HttpStatus.OK);
	}

}