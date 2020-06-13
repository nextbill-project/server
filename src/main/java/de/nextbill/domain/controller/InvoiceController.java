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

import de.nextbill.domain.dtos.*;
import de.nextbill.domain.enums.CorrectionStatus;
import de.nextbill.domain.enums.PaymentTypeEnum;
import de.nextbill.domain.enums.RepetitionTypeEnum;
import de.nextbill.domain.interfaces.PaymentPerson;
import de.nextbill.domain.model.*;
import de.nextbill.domain.repositories.*;
import de.nextbill.domain.services.*;
import de.nextbill.domain.utils.BeanMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@Controller
@RequestMapping({ "webapp/api","api" })
public class InvoiceController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private InvoiceRepository invoiceRepository;

	@Autowired
	private InvoiceFailureRepository invoiceFailureRepository;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private InvoiceService invoiceService;

	@Autowired
	private InvoiceCategoryService invoiceCategoryService;

	@Autowired
	private CostDistributionItemRepository costDistributionItemRepository;

	@Autowired
	private CostDistributionItemService costDistributionItemService;

	@Autowired
	private BillingService billingService;

	@Autowired
	private InvoiceHelperService invoiceHelperService;

	@Autowired
	private BudgetService budgetService;

	@ResponseBody
	@RequestMapping(value = "/invoices/paymentTypeEnums", method = RequestMethod.GET)
	public ResponseEntity<?> invoicePaymentTypeEnums() {

		List<TypeEnumDTO> typeEnumDTOS = new ArrayList<>();

		for (PaymentTypeEnum paymentTypeEnum : PaymentTypeEnum.values()) {
			TypeEnumDTO typeEnumDTO = new TypeEnumDTO();
			typeEnumDTO.setDisplayName(paymentTypeEnum.getDisplayName());
			typeEnumDTO.setName(paymentTypeEnum.name());

			typeEnumDTOS.add(typeEnumDTO);
		}

		return new ResponseEntity<>(typeEnumDTOS, HttpStatus.OK);
	}

	@ResponseBody
	@RequestMapping(value = "/invoices/repetitionTypeEnums", method = RequestMethod.GET)
	public ResponseEntity<?> invoiceRepetitionTypeEnums() {

		List<TypeEnumDTO> typeEnumDTOS = new ArrayList<>();

		for (RepetitionTypeEnum repetitionTypeEnum : RepetitionTypeEnum.values()) {
			TypeEnumDTO typeEnumDTO = new TypeEnumDTO();
			typeEnumDTO.setDisplayName(repetitionTypeEnum.getDisplayName());
			typeEnumDTO.setName(repetitionTypeEnum.name());

			typeEnumDTOS.add(typeEnumDTO);
		}

		return new ResponseEntity<>(typeEnumDTOS, HttpStatus.OK);
	}

	@ResponseBody
	@RequestMapping(value = "/invoices/{cid}", method = RequestMethod.GET)
	public ResponseEntity<?> getOne(@PathVariable("cid") UUID id) {

		BeanMapper beanMapper = new BeanMapper();

		Invoice invoice = invoiceRepository.findById(id).orElse(null);

		if (invoice != null){
			InvoiceDTO invoiceDTO = invoiceHelperService.mapToDTO(invoice, false);

			if (invoice.getInvoiceCategory() != null){
				InvoiceCategoryDTO invoiceCategoryDTO = invoiceCategoryService.mapToDTO(invoice.getInvoiceCategory());
				invoiceDTO.setInvoiceCategoryDTO(invoiceCategoryDTO);
			}

			List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByInvoice(invoice);
			List<CostDistributionItemDTO> costDistributionItemDTOs = new ArrayList<>();
			for (CostDistributionItem costDistributionItem : costDistributionItems) {
				costDistributionItemDTOs.add(costDistributionItemService.mapToDTO(costDistributionItem));
			}

			invoiceDTO.setCostDistributionItemDTOs(costDistributionItemDTOs);

			List<InvoiceFailure> invoiceFailures = invoiceFailureRepository.findByInvoice(invoice);
			List<InvoiceFailureDTO> invoiceFailureDTOs = new ArrayList<>();
			for (InvoiceFailure invoiceFailure : invoiceFailures) {
				InvoiceFailureDTO invoiceFailureDTO = beanMapper.map(invoiceFailure, InvoiceFailureDTO.class);
				if (invoiceFailure.getInvoice() != null){
					invoiceFailureDTO.setInvoiceId(invoiceFailure.getInvoice().getInvoiceId());
				}
				invoiceFailureDTOs.add(invoiceFailureDTO);
			}
			invoiceDTO.setInvoiceFailureDTOs(invoiceFailureDTOs);

			return new ResponseEntity<>(invoiceDTO, HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}
	
	@RequestMapping(value = "/invoices/{cid}", method = RequestMethod.PUT)
	public ResponseEntity<?> updateInvoice(@PathVariable(value="cid") UUID cid, @RequestBody InvoiceDTO invoiceDTO, @RequestParam("setWorkflow") Optional<CorrectionStatus> correctionStatus) {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		invoiceDTO.setInvoiceId(cid);

		Invoice invoice = invoiceService.updateInvoice(currentUser, invoiceDTO, correctionStatus);

		InvoiceDTO invoiceDtoUpdated = invoiceHelperService.mapToDTO(invoice, false);

		return new ResponseEntity<>(invoiceDtoUpdated, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/invoices", method = RequestMethod.POST)
	public ResponseEntity<?> insertInvoice(@RequestBody InvoiceDTO invoiceDTO) {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		Invoice invoice = invoiceService.createInvoice(currentUser, invoiceDTO);

		InvoiceDTO invoiceDtoNew = invoiceHelperService.mapToDTO(invoice, false);

	    return new ResponseEntity<>(invoiceDtoNew, HttpStatus.OK);
	}

	@RequestMapping(value = "/invoices/{cid}", method = RequestMethod.DELETE)
	public ResponseEntity<?> deleteInvoice(@PathVariable(value="cid") UUID cid) {

		Invoice invoice = invoiceRepository.findById(cid).orElse(null);

		if (invoice == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		BigDecimal costPaidOld = invoice.getCostPaid() != null ? invoice.getCostPaid() : new BigDecimal(0);

		invoiceService.deleteInvoice(invoice, false, false);
		billingService.refreshBillingsIfNecessary(invoice, 0D, costPaidOld.doubleValue());

		invoice.setBillings(new HashSet<>());
		invoiceRepository.save(invoice);

		budgetService.updateBudgetsIfNecessary(invoice);

		return new ResponseEntity<>(HttpStatus.OK);
	}
}