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

import de.nextbill.domain.dtos.UserContactDTO;
import de.nextbill.domain.enums.BasicStatusEnum;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.UserContact;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.UserContactRepository;
import de.nextbill.domain.services.UserContactService;
import de.nextbill.domain.utils.BeanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping({ "webapp/api","api" })
public class UserContactController {

	@Autowired
	private UserContactRepository userContactRepository;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private UserContactService userContactService;

	@RequestMapping(value = "/usercontacts", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<?> list() {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		List<UserContact> userContacts = userContactRepository.findAllByAppUserAndBasicStatusEnum(currentUser, BasicStatusEnum.OK);

		BeanMapper beanMapper = new BeanMapper();

		List<UserContactDTO> resultUserContactDTOs = new ArrayList<>();
		for (UserContact userContact : userContacts) {
			UserContactDTO userContactDTO = userContactService.mapToDTO(userContact);
			resultUserContactDTOs.add(userContactDTO);
		}

		return new ResponseEntity<>(resultUserContactDTOs, HttpStatus.OK);
	}

	@RequestMapping(value = "/usercontacts/{id}", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<?> getOne(@PathVariable("id") UUID id) {

		UserContact userContact = userContactRepository.findById(id).orElse(null);
		if (userContact == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		UserContactDTO userContactDTO = userContactService.mapToDTO(userContact);

		return new ResponseEntity<>(userContactDTO, HttpStatus.OK);

	}

	@RequestMapping(value = "/usercontacts", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<?> create(@RequestBody UserContactDTO userContactDTO) {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		UserContact newUserContact = null;
		AppUser appUser = null;
		if (userContactDTO.getAppUserContactId() != null){
			appUser = appUserRepository.findById(userContactDTO.getAppUserContactId()).orElse(null);
		}

		if (appUser != null){
			newUserContact = userContactRepository.findOneByAppUserAndAppUserContact(currentUser, appUser);
		}

		if (newUserContact == null){
			BeanMapper beanMapper = new BeanMapper();
			newUserContact = beanMapper.map(userContactDTO, UserContact.class);
		}

		if (newUserContact.getUserContactId() == null){
			newUserContact.setUserContactId(UUID.randomUUID());
		}

		if (userContactDTO.getAppUserContactId() != null){
			AppUser appUserContact = appUserRepository.findById(userContactDTO.getAppUserContactId()).orElse(null);
			newUserContact.setAppUserContact(appUserContact);
		}
		newUserContact.setAppUser(currentUser);
		newUserContact.setBasicStatusEnum(BasicStatusEnum.OK);

		userContactRepository.save(newUserContact);

		UserContactDTO newUserContactDTO = userContactService.mapToDTO(newUserContact);

		return new ResponseEntity<>(newUserContactDTO, HttpStatus.OK);
	}

	@RequestMapping(value = "/usercontacts/{id}", method = RequestMethod.PUT)
	public @ResponseBody ResponseEntity<?> update(@PathVariable("id") UUID id, @RequestBody UserContactDTO userContactDTO) {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		BeanMapper beanMapper = new BeanMapper();
		UserContact newUserContact = beanMapper.map(userContactDTO, UserContact.class);

		if (userContactDTO.getAppUserContactId() != null){
			AppUser appUserContact = appUserRepository.findById(userContactDTO.getAppUserContactId()).orElse(null);
			newUserContact.setAppUserContact(appUserContact);
		}
		newUserContact.setAppUser(currentUser);

		userContactRepository.save(newUserContact);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/usercontacts/{id}", method = RequestMethod.DELETE)
	public @ResponseBody ResponseEntity<?> delete(@PathVariable("id") UUID id) {

		UserContact userContact = userContactRepository.findById(id).orElse(null);
		if (userContact == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		userContact.setBasicStatusEnum(BasicStatusEnum.DELETED);
		userContactRepository.save(userContact);

		return new ResponseEntity<>(HttpStatus.OK);

	}
}