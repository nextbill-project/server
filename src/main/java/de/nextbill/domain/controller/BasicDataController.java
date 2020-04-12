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

import de.nextbill.domain.dtos.BasicDataDTO;
import de.nextbill.domain.enums.BasicDataType;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.BasicData;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.BasicDataRepository;
import de.nextbill.domain.services.MessagingService;
import de.nextbill.domain.utils.BeanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping({ "webapp/api","api" })
public class BasicDataController {

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private BasicDataRepository basicDataRepository;

	@Autowired
	private MessagingService messagingService;

	@RequestMapping(value = "/basicdata", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<?> list() {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		List<BasicDataDTO> basicDatasDtos = messagingService.createBasicDataMessages(currentUser);

		BeanMapper beanMapper = new BeanMapper();

		List<BasicDataDTO> basicDataTmpDTOs = new ArrayList<>();
		for (BasicData basicData : basicDataRepository.findAllByAppUserAndBasicDataType(currentUser, BasicDataType.STATISTIC)) {
			BasicDataDTO basicDataDTO = beanMapper.map(basicData, BasicDataDTO.class);
			basicDataDTO.setAppUserId(currentUser.getAppUserId());
			basicDataTmpDTOs.add(basicDataDTO);
		}

		basicDatasDtos.addAll(basicDataTmpDTOs);

		return new ResponseEntity<>(basicDatasDtos, HttpStatus.OK);
	}

	@RequestMapping(value = "/basicdata/{id}", method = RequestMethod.DELETE)
	public @ResponseBody ResponseEntity<?> list(@PathVariable("id") UUID id) {

		BasicData basicData = basicDataRepository.findById(id).orElse(null);
		if (basicData == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		basicDataRepository.delete(basicData);

		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@RequestMapping(value = "/statistics", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<?> statistics() {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));
		BeanMapper beanMapper = new BeanMapper();

		List<BasicData> basicDatas = basicDataRepository.findAllByAppUserAndBasicDataType(currentUser, BasicDataType.STATISTIC);

		List<BasicDataDTO> basicDataDTOs = new ArrayList<>();
		for (BasicData basicData : basicDatas) {
			BasicDataDTO basicDataDTO = beanMapper.map(basicData, BasicDataDTO.class);
			basicDataDTO.setAppUserId(currentUser.getAppUserId());
			basicDataDTOs.add(basicDataDTO);
		}

		return new ResponseEntity<>(basicDataDTOs, HttpStatus.OK);
	}

	@RequestMapping(value = "/messages", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<?> messageList() {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		List<BasicDataDTO> basicDatasDtos = messagingService.createBasicDataMessages(currentUser);

		BeanMapper beanMapper = new BeanMapper();

		List<BasicDataDTO> basicDataTmpDTOs = new ArrayList<>();
		for (BasicData basicData : basicDataRepository.findAllByAppUserAndBasicDataType(currentUser, BasicDataType.BILLING_MESSAGE)) {
			BasicDataDTO basicDataDTO = beanMapper.map(basicData, BasicDataDTO.class);
			basicDataDTO.setAppUserId(currentUser.getAppUserId());
			basicDataTmpDTOs.add(basicDataDTO);
		}

		basicDatasDtos.addAll(basicDataTmpDTOs);

		return new ResponseEntity<>(basicDatasDtos, HttpStatus.OK);
	}
}