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

import de.nextbill.domain.dtos.CostDistributionItemDTO;
import de.nextbill.domain.dtos.InvoiceCategoryDTO;
import de.nextbill.domain.enums.BasicStatusEnum;
import de.nextbill.domain.enums.RepetitionTypeEnum;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.CostDistribution;
import de.nextbill.domain.model.CostDistributionItem;
import de.nextbill.domain.model.InvoiceCategory;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.CostDistributionItemRepository;
import de.nextbill.domain.repositories.InvoiceCategoryRepository;
import de.nextbill.domain.services.AutoFillHelperService;
import de.nextbill.domain.services.CostDistributionItemService;
import de.nextbill.domain.services.InvoiceCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping({ "webapp/api","api" })
public class InvoiceCategoryController {

	@Autowired
	private InvoiceCategoryRepository invoiceCategoryRepository;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private InvoiceCategoryService invoiceCategoryService;

	@Autowired
	private AutoFillHelperService autoFillHelperService;

	@Autowired
	private CostDistributionItemRepository costDistributionItemRepository;

	@Autowired
	private CostDistributionItemService costDistributionItemService;

	@RequestMapping(value = "/invoicecategories", method = RequestMethod.GET)
	public @ResponseBody
	ResponseEntity<?> list() {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser appUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		List<InvoiceCategory> invoiceCategorys = invoiceCategoryRepository.findAllByAppUserIsNullOrAppUserAndBasicStatusEnum(appUser, BasicStatusEnum.OK);

		List<InvoiceCategoryDTO> invoiceCategoryDTOs = new ArrayList<>();
		for (InvoiceCategory invoiceCategory : invoiceCategorys) {
			InvoiceCategoryDTO resultInvoiceCategoryDTO = invoiceCategoryService.mapToDTO(invoiceCategory);

			invoiceCategoryDTOs.add(resultInvoiceCategoryDTO);
		}

		return new ResponseEntity<>(invoiceCategoryDTOs, HttpStatus.OK);
	}

	@RequestMapping(value = "/invoicecategories/{uuid}/autoAttributes", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<?> attributesForCategory(@PathVariable("uuid") UUID id) {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser appUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));


		InvoiceCategory invoiceCategory = invoiceCategoryRepository.findById(id).orElse(null);
		if (invoiceCategory == null){
			new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		Map<String, Object> results = new HashMap<>();

		CostDistribution costDistribution = autoFillHelperService.findCostDistributionForCategory(appUser, invoiceCategory);

		if (costDistribution != null){
			List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByCostDistribution(costDistribution);

			List<CostDistributionItemDTO> newCostDistributionItemDTOs = new ArrayList<>();
			for (CostDistributionItem costDistributionItem : costDistributionItems) {
				CostDistributionItem costDistributionItem1 = autoFillHelperService.prepareItemForCostDistribution(costDistributionItem, null);
//				costDistributionItem1.setMoneyValue(CostDistributionHelper.calculateAmountForCostDistributionItemPrecise(costDistributionItem, costDistributionItems, currentInvoice.getSumOfInvoice()));
//				costDistributionItem1.setInvoice(currentInvoice);
				costDistributionItem1.setCostDistribution(null);
				CostDistributionItemDTO costDistributionItemDTO = costDistributionItemService.mapToDTO(costDistributionItem);

				newCostDistributionItemDTOs.add(costDistributionItemDTO);
			}
			results.put("costDistributionItems", newCostDistributionItemDTOs);
		}

		RepetitionTypeEnum repetitionTypeEnum = autoFillHelperService.findRepetitionTypeForCategory(appUser, invoiceCategory);

		if (repetitionTypeEnum != null){
			results.put("repetitionTypeEnum", repetitionTypeEnum);
		}

		Boolean isSpecialType = autoFillHelperService.findSpecialTypeForCategory(appUser, invoiceCategory);

		if (isSpecialType != null){
			results.put("isSpecialType", isSpecialType);
		}

		return new ResponseEntity<>(results, HttpStatus.OK);
	}

	@RequestMapping(value = "/invoicecategories/{cid}", method = RequestMethod.PUT)
	public @ResponseBody ResponseEntity<?> update(@PathVariable(value="cid") UUID cid, @RequestBody InvoiceCategoryDTO invoiceCategoryDTO) {

		InvoiceCategory invoiceCategory = invoiceCategoryService.mapToEntity(invoiceCategoryDTO);
		InvoiceCategory resultInvoiceCategory = invoiceCategoryRepository.save(invoiceCategory);

		InvoiceCategoryDTO resultInvoiceCategoryDTO = invoiceCategoryService.mapToDTO(resultInvoiceCategory);

		return new ResponseEntity<>(resultInvoiceCategoryDTO, HttpStatus.OK);
	}

	@RequestMapping(value = "/invoicecategories", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<?> insert(@RequestBody InvoiceCategoryDTO invoiceCategoryDTO) {

		if (invoiceCategoryDTO.getInvoiceCategoryId() == null){
			invoiceCategoryDTO.setInvoiceCategoryId(UUID.randomUUID());
		}

		if (invoiceCategoryRepository.findById(invoiceCategoryDTO.getInvoiceCategoryId()).isPresent()){
			return new ResponseEntity<>("InvoiceCategory already exists!", HttpStatus.CONFLICT);
		}

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser appUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		InvoiceCategory invoiceCategory = invoiceCategoryService.mapToEntity(invoiceCategoryDTO);
		invoiceCategory.setAppUser(appUser);
		invoiceCategory.setBasicStatusEnum(BasicStatusEnum.OK);

		InvoiceCategory resultInvoiceCategory = invoiceCategoryRepository.save(invoiceCategory);

		InvoiceCategoryDTO resultInvoiceCategoryDTO = invoiceCategoryService.mapToDTO(resultInvoiceCategory);

		return new ResponseEntity<>(resultInvoiceCategoryDTO, HttpStatus.OK);
	}

	@RequestMapping(value = "/invoicecategories/{cid}", method = RequestMethod.DELETE)
	public @ResponseBody ResponseEntity<?> delete(@PathVariable(value="cid") UUID cid) {

		InvoiceCategory invoiceCategory = invoiceCategoryRepository.findById(cid).orElse(null);
		if (invoiceCategory == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		invoiceCategory.setBasicStatusEnum(BasicStatusEnum.DELETED);
		invoiceCategoryRepository.save(invoiceCategory);

		return new ResponseEntity<>(HttpStatus.OK);
	}

}