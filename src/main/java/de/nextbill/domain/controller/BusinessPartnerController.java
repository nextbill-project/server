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

import de.nextbill.domain.dtos.BusinessPartnerDTO;
import de.nextbill.domain.dtos.BusinessPartnersResponseDTO;
import de.nextbill.domain.enums.BasicStatusEnum;
import de.nextbill.domain.enums.PublicStatus;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.BusinessPartner;
import de.nextbill.domain.model.InvoiceCategory;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.BusinessPartnerRepository;
import de.nextbill.domain.services.AutoFillHelperService;
import de.nextbill.domain.services.BusinessPartnerService;
import de.nextbill.domain.services.InvoiceCategoryService;
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
public class BusinessPartnerController {

	@Autowired
	private BusinessPartnerRepository businessPartnerRepository;

	@Autowired
	private BusinessPartnerService businessPartnerService;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private AutoFillHelperService autoFillHelperService;

	@Autowired
	private InvoiceCategoryService invoiceCategoryService;

	@RequestMapping(value = "/businesspartners", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<?> list() {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser appUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		List<BusinessPartner> businessPartners = businessPartnerRepository.findAllByAppUserIsNullOrAppUserAndBasicStatusEnum(appUser, BasicStatusEnum.OK);

		List<BusinessPartnerDTO> businessPartnerDTOs = new ArrayList<>();

		for (BusinessPartner businessPartner : businessPartners) {
			BusinessPartnerDTO resultBusinessPartnerDTO = businessPartnerService.mapToDTO(businessPartner);
			businessPartnerDTOs.add(resultBusinessPartnerDTO);
		}

		return new ResponseEntity<>(businessPartnerDTOs, HttpStatus.OK);

	}

	@RequestMapping(value = "/businesspartners/search", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<?> search(@RequestBody BusinessPartnerDTO businessPartnerDTO) {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		List<BusinessPartner> businessPartners = businessPartnerRepository.findBusinessPartnerCaseInsensitiveForUser(businessPartnerDTO.getBusinessPartnerName(), BasicStatusEnum.OK, currentUser);

		List<BusinessPartnerDTO> businessPartnerDTOs = new ArrayList<>();

		for (BusinessPartner businessPartner : businessPartners) {
			BusinessPartnerDTO resultBusinessPartnerDTO = businessPartnerService.mapToDTO(businessPartner);
			businessPartnerDTOs.add(resultBusinessPartnerDTO);
		}

		BusinessPartnersResponseDTO businessPartnersResponseDTO = new BusinessPartnersResponseDTO();
		businessPartnersResponseDTO.setBusinessPartnerDTOList(businessPartnerDTOs);
		BusinessPartner businessPartnerNew = businessPartnerRepository.findByBusinessPartnerNameAndBasicStatusEnumAndIsValidForUser(businessPartnerDTO.getBusinessPartnerName(), BasicStatusEnum.OK, currentUser);
		if (businessPartnerNew == null){
			businessPartnersResponseDTO.setNewUserName(businessPartnerDTO.getBusinessPartnerName());
		}

		return new ResponseEntity<>(businessPartnersResponseDTO, HttpStatus.OK);
	}

	@RequestMapping(value = "/businesspartners/{uuid}/autoCategory", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<?> categoryForBusinessPartner(@PathVariable("uuid") UUID id) {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser appUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		BusinessPartner businessPartner = businessPartnerRepository.findById(id).orElse(null);
		if (businessPartner == null){
			new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		InvoiceCategory invoiceCategory = autoFillHelperService.findCategoryForBusinessPartner(appUser, businessPartner, null, null, true);

		if (invoiceCategory != null){
			return new ResponseEntity<>(invoiceCategoryService.mapToDTO(invoiceCategory), HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

	@RequestMapping(value = "/businesspartners/{uuid}", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<?> getOne(@PathVariable("uuid") UUID id) {

		BusinessPartner businessPartner = businessPartnerRepository.findById(id).orElse(null);
		if (businessPartner == null){
			new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<>(businessPartnerService.mapToDTO(businessPartner), HttpStatus.OK);

	}

	@RequestMapping(value = "/businesspartners", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<?> create(@RequestBody BusinessPartnerDTO businessPartnerDTO) {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		if (businessPartnerRepository.findByBusinessPartnerNameAndBasicStatusEnumAndIsValidForUser(businessPartnerDTO.getBusinessPartnerName(), BasicStatusEnum.OK, currentUser) != null){
			return new ResponseEntity<>("Business partner name already exists!", HttpStatus.CONFLICT);
		}

		BeanMapper beanMapper = new BeanMapper();
		BusinessPartner businessPartner = beanMapper.map(businessPartnerDTO, BusinessPartner.class);

		businessPartner.setAppUser(currentUser);

		if (businessPartnerDTO.getBusinessPartnerId() == null){
			businessPartner.setBusinessPartnerId(UUID.randomUUID());
		}
		if (businessPartnerDTO.getBusinessPartnerReceiptName() == null){
			businessPartner.setBusinessPartnerReceiptName(businessPartnerDTO.getBusinessPartnerName());
		}
		if (businessPartnerDTO.getBasicStatusEnum() == null){
			businessPartner.setBasicStatusEnum(BasicStatusEnum.OK);
		}

		businessPartner.setBusinessPartnerPublicStatus(PublicStatus.PRIVATE);
		businessPartner.setCategoryPublicStatus(PublicStatus.PRIVATE);

		businessPartner = businessPartnerRepository.save(businessPartner);

		return new ResponseEntity<>(businessPartnerService.mapToDTO(businessPartner), HttpStatus.OK);
	}

	@RequestMapping(value = "/businesspartners/{id}", method = RequestMethod.DELETE)
	public @ResponseBody ResponseEntity<?> delete(@PathVariable("id") UUID id) {

		BusinessPartner businessPartner = businessPartnerRepository.findById(id).orElse(null);
		if (businessPartner == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		businessPartner.setBasicStatusEnum(BasicStatusEnum.DELETED);
		businessPartnerRepository.save(businessPartner);

		return new ResponseEntity<>(HttpStatus.OK);

	}
}