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
import de.nextbill.domain.enums.BasicStatusEnum;
import de.nextbill.domain.enums.InvoiceStatusEnum;
import de.nextbill.domain.interfaces.PaymentPerson;
import de.nextbill.domain.model.*;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.CostDistributionItemRepository;
import de.nextbill.domain.repositories.CostDistributionRepository;
import de.nextbill.domain.repositories.InvoiceRepository;
import de.nextbill.domain.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping({ "webapp/api","api" })
public class CostDistributionItemController {

	@Autowired
	private InvoiceRepository invoiceRepository;

	@Autowired
	private CostDistributionItemRepository costDistributionItemRepository;

	@Autowired
	private CostDistributionRepository costDistributionRepository;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private CostDistributionItemService costDistributionItemService;

	@Autowired
	private BillingService billingService;

	@Autowired
	private BudgetService budgetService;

	@Autowired
	private PaymentPersonService paymentPersonService;

	@Autowired
	private MessagingService messagingService;

	@ResponseBody
	@RequestMapping(value = "/costdistributionitems", method = RequestMethod.GET)
	public ResponseEntity<?> list() {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		List<CostDistribution> costDistributions = costDistributionRepository.findByCreatedByAndBasicStatusEnum(currentUser.getAppUserId().toString(), BasicStatusEnum.OK);

		List<Invoice> invoices = new ArrayList<>();
		invoices.addAll(invoiceRepository.findWithUserAndNotStatus(currentUser.getAppUserId().toString(), InvoiceStatusEnum.DELETED, PageRequest.of( 0, Integer.MAX_VALUE, Sort.Direction.ASC, "dateOfInvoice" )).getContent());

		List<CostDistributionItem> tmpCostDistributionItems = costDistributionItemRepository.findByPayerAndInvoiceNotNullAndNoStandingOrder(currentUser);
		for (CostDistributionItem costDistributionItem : tmpCostDistributionItems) {
			Invoice invoice = invoiceRepository.findById(costDistributionItem.getInvoice().getInvoiceId()).orElse(null);
			if (!invoices.contains(invoice)){
				invoices.add(invoice);
			}
		}

		List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByInvoiceIn(invoices);
		List<CostDistributionItem> costDistributionItems2 = costDistributionItemRepository.findByCostDistributionIn(costDistributions);
		costDistributionItems.addAll(costDistributionItems2);

		List<CostDistributionItemDTO> costDistributionItemDTOs = new ArrayList<>();
		for (CostDistributionItem costDistributionItem : costDistributionItems) {
			CostDistributionItemDTO costDistributionItemDTO = costDistributionItemService.mapToDTO(costDistributionItem);
			costDistributionItemDTOs.add(costDistributionItemDTO);
		}

		return new ResponseEntity<>(costDistributionItemDTOs, HttpStatus.OK);
	}

	@ResponseBody
	@RequestMapping(value = "/invoices/{id}/costdistributionitems", method = RequestMethod.GET)
	public ResponseEntity<?> listForInvoice(@PathVariable(value="id") UUID id) {

		Invoice invoice = invoiceRepository.findById(id).orElse(null);

		if (invoice == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByInvoice(invoice);

		List<CostDistributionItemDTO> costDistributionItemDTOs = new ArrayList<>();
		for (CostDistributionItem costDistributionItem : costDistributionItems) {
			CostDistributionItemDTO costDistributionItemDTO = costDistributionItemService.mapToDTO(costDistributionItem);
			costDistributionItemDTOs.add(costDistributionItemDTO);
		}

		return new ResponseEntity<>(costDistributionItemDTOs, HttpStatus.OK);
	}

	@ResponseBody
	@RequestMapping(value = "/invoices/{id}/costdistributionitems", method = RequestMethod.PUT)
	public ResponseEntity<?> putForInvoice(@PathVariable(value="id") UUID id, @RequestBody List<CostDistributionItemDTO> costDistributionItemDTOs) {

		Invoice invoice = invoiceRepository.findById(id).orElse(null);

		if (invoice == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		List<CostDistributionItem> costDistributionItems = costDistributionItemService.updateCostDistributionItemsOfInvoice(invoice, costDistributionItemDTOs);

		budgetService.updateBudgetsIfNecessary(invoice);

		List<CostDistributionItemDTO> costDistributionItemResponseDTOs = new ArrayList<>();
		for (CostDistributionItem costDistributionItem : costDistributionItems) {
			CostDistributionItemDTO costDistributionItemDTO = costDistributionItemService.mapToDTO(costDistributionItem);
			costDistributionItemResponseDTOs.add(costDistributionItemDTO);
		}

		return new ResponseEntity<>(costDistributionItemResponseDTOs, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/costdistributionitems/{cid}", method = RequestMethod.PUT)
	public ResponseEntity<?> updateCostDistributionItem(@PathVariable(value="cid") String cid, @RequestBody CostDistributionItemDTO costDistributionItemDTO) {

		Double costPaidNew = costDistributionItemDTO.getCostPaid();
		CostDistributionItem costDistributionItem = null;

		CostDistributionItem costDistributionItemDb = costDistributionItemRepository.findById(costDistributionItemDTO.getCostDistributionItemId()).orElse(null);

		BigDecimal oldCostPaid = costDistributionItemDb != null && costDistributionItemDb.getCostPaid() != null ? costDistributionItemDb.getCostPaid() : new BigDecimal(0);
		
		if (costDistributionItemDTO != null){
			costDistributionItem = costDistributionItemService.mapToEntity(costDistributionItemDTO);

			if (costDistributionItem.getCorrectionStatus() == null){
				costDistributionItemService.autoSetCorrectionStatus(costDistributionItem);
			}
		}

		CostDistributionItem costDistributionItemNew = costDistributionItemRepository.save(costDistributionItem);

		budgetService.updateBudgetsIfNecessary(costDistributionItemNew.getInvoice());

		billingService.refreshBillingsIfNecessaryAsync(costDistributionItemNew, costPaidNew, oldCostPaid.doubleValue());

		return new ResponseEntity<UUID>(costDistributionItemNew.getCostDistributionItemId(), HttpStatus.OK);
	}

	@RequestMapping(value = "/costdistributionitems/{cid}", method = RequestMethod.DELETE)
	public ResponseEntity<?> deleteCostDistributionItem(@PathVariable(value="cid") UUID cid) {

		CostDistributionItem costDistributionItem = costDistributionItemRepository.findById(cid).orElse(null);

		if (costDistributionItem == null){
			return new ResponseEntity<>(HttpStatus.OK);
		}

		Invoice invoice = costDistributionItem.getInvoice();
		if (invoice != null) {
			PaymentPerson paymentPerson = paymentPersonService.findPaymentPerson(costDistributionItem.getCostPayerId(), costDistributionItem.getPaymentPersonTypeEnum());
			AppUser appUser = null;
			if (paymentPerson instanceof AppUser) {
				appUser = (AppUser) paymentPerson;
			}else if (paymentPerson instanceof UserContact) {
				appUser = ((UserContact) paymentPerson).getAppUserContact();
			}
			if (appUser != null) {
				messagingService.createInternalDataInvoiceDeletedMessage(appUser, invoice);
			}
		}

		BigDecimal costPaidOld = costDistributionItem.getCostPaid() != null ? costDistributionItem.getCostPaid() : new BigDecimal(0);

		billingService.refreshBillingsIfNecessary(costDistributionItem, 0D, costPaidOld.doubleValue());

		invoice.setLastModifiedAt(new Date());
		invoiceRepository.save(invoice);
		costDistributionItemRepository.delete(costDistributionItem);

		budgetService.updateBudgetsIfNecessary(invoice);

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@RequestMapping(value = "/costdistributionitems", method = RequestMethod.POST)
	public ResponseEntity<?> insertCostDistributionItem(@RequestBody CostDistributionItemDTO costDistributionItemDTO) {

		CostDistributionItem costDistributionItem = null;
		
		if (costDistributionItemDTO != null){
			costDistributionItem = costDistributionItemService.mapToEntity(costDistributionItemDTO);

			if (costDistributionItem.getCorrectionStatus() == null){
				costDistributionItemService.autoSetCorrectionStatus(costDistributionItem);
			}
		}

		costDistributionItemRepository.save(costDistributionItem);

		budgetService.updateBudgetsIfNecessary(costDistributionItem.getInvoice());

	    return new ResponseEntity<UUID>(costDistributionItem.getCostDistributionItemId(), HttpStatus.OK);
	}
}