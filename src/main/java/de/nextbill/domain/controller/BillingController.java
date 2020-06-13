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
import de.nextbill.domain.enums.BillingStatusEnum;
import de.nextbill.domain.enums.FirebaseMessageType;
import de.nextbill.domain.model.*;
import de.nextbill.domain.pojos.BillingListItem;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.BillingRepository;
import de.nextbill.domain.repositories.UserContactRepository;
import de.nextbill.domain.services.*;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import javax.transaction.Transactional;
import java.io.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping({ "webapp/api","api" })
public class BillingController {

	@Autowired
	private FirebaseService firebaseService;

	@Autowired
	private MessagingService messagingService;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private UserContactRepository userContactRepository;

	@Autowired
	private BillingRepository billingRepository;

	@Autowired
	private BillingService billingService;

	@Autowired
	private InvoiceHelperService invoiceHelperService;

	@Autowired
	private PathService pathService;

	@Value("${server.port}")
	private String port;

	@ResponseBody
	@RequestMapping(value = "/billings/{id}", method = RequestMethod.DELETE)
	public ResponseEntity<?> abortBilling(@PathVariable("id") UUID id) {

		Billing billing = billingRepository.findById(id).orElse(null);
		if (billing == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (BillingStatusEnum.ARCHIVED.equals(billing.getBillingStatusEnum())){
			billing.setBillingStatusEnum(BillingStatusEnum.ARCHIVED_DELETED);
		}else{
			billing.setBillingStatusEnum(BillingStatusEnum.ABORTED);
		}

		billingRepository.save(billing);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/billings/checkReminderMessage/{appUserId}", method = RequestMethod.POST)
	public ResponseEntity<?> sendCheckReminderMessage(@PathVariable(value="appUserId") UUID appUserId) {

		AppUser appUser = appUserRepository.findById(appUserId).orElse(null);

		if (appUser == null){

			UserContact userContact = userContactRepository.findById(appUserId).orElse(null);
			if (userContact != null){
				appUser = userContact.getAppUserContact();
			}

			if (appUser == null){
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
		}

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		BasicData basicData = messagingService.createCheckReminderMessage(appUser, currentUser);
		MessageDTO messageDTO1 = messagingService.createMessageDTOFromJson(basicData.getValue());
		firebaseService.sendTextMessage(appUser, FirebaseMessageType.MESSAGE_BILLING, messageDTO1.getSubject(), messageDTO1.getMessage());

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@ResponseBody
	@RequestMapping(value = "/billings/billingListItems/grouped", method = RequestMethod.GET)
	public ResponseEntity<?> groupedBillingListItems(@RequestParam("archived") Optional<Boolean> archived) {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser appUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		List<GroupedBillingListItemsDTO> groupedBillingListItemsDTO = billingService.groupedBillingListProcessItems(appUser, archived.orElse(false), true);

		return new ResponseEntity<>(groupedBillingListItemsDTO, HttpStatus.OK);
	}

	@ResponseBody
	@RequestMapping(value = "/billings/billingListItems", method = RequestMethod.GET)
	public ResponseEntity<?> billingListItems(@RequestParam("archived") Optional<Boolean> archived) {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser appUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		List<BillingListItemDTO> billingListItemsDTO = billingService.groupedBillingListProcessItems(appUser, archived.orElse(false), false).stream()
				.map(GroupedBillingListItemsDTO::getBillingListItemDTO).collect(Collectors.toList());

		return new ResponseEntity<>(billingListItemsDTO, HttpStatus.OK);
	}

	@ResponseBody
	@RequestMapping(value = "/billings/{id}/archive", method = RequestMethod.POST)
	public ResponseEntity<?> archiveBilling(@PathVariable("id") UUID id) {

		Billing billing = billingRepository.findById(id).orElse(null);
		if (billing == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		billing.setBillingStatusEnum(BillingStatusEnum.ARCHIVED);
		billingRepository.save(billing);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@ResponseBody
	@RequestMapping(value = "/billings/{id}", method = RequestMethod.GET)
	public ResponseEntity<?> getBilling(@PathVariable("id") UUID id) {

		Billing billing = billingRepository.findById(id).orElse(null);
		if (billing == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser loggedInUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		BillingDTO billingDTO = billingService.mapToDTO(billing, loggedInUser);

		return new ResponseEntity<>(billingDTO, HttpStatus.OK);
	}

	@ResponseBody
	@RequestMapping(value = "/billings/{id}/createCompensation", method = RequestMethod.POST)
	public ResponseEntity<?> createCompensation(@PathVariable("id") UUID id) {

		Billing billing = billingRepository.findById(id).orElse(null);
		if (billing == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser loggedInUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		Invoice invoice = billingService.createCompensation(loggedInUser, billing);

		if (invoice == null){
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		InvoiceDTO invoiceDTO = invoiceHelperService.mapToDTO(invoice, false);

		return new ResponseEntity<>(invoiceDTO, HttpStatus.OK);
	}

	@ResponseBody
	@RequestMapping(value = "/billings/{id}/executeEquality", method = RequestMethod.POST)
	public ResponseEntity<?> executeEquality(@PathVariable("id") UUID id) {

		Billing billing = billingRepository.findById(id).orElse(null);
		if (billing == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		BigDecimal currentSumToPay = billingService.sumToPay(billing);
		billing.setSumToPay(currentSumToPay);
		billingRepository.save(billing);

		billingService.createBillingReport(billing);

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser loggedInUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		BillingDTO billingDTO = billingService.mapToDTO(billing, loggedInUser);

		return new ResponseEntity<>(billingDTO, HttpStatus.OK);
	}

	@ResponseBody
	@RequestMapping(value = "/billings/{id}/payment", method = RequestMethod.POST)
	public ResponseEntity<?> billingPayment(@PathVariable("id") UUID id, @RequestBody BillingPaymentDTO billingPaymentDTO) {

		Billing billing = billingRepository.findById(id).orElse(null);
		if (billing == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (billingPaymentDTO.getBillingStatusEnum() == null){
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		Billing billingSaved = billingService.changeBillingStatus(billing, billingPaymentDTO.getBillingStatusEnum());

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser loggedInUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		BillingDTO billingDTO = billingService.mapToDTO(billingSaved, loggedInUser);

		return new ResponseEntity<>(billingDTO, HttpStatus.OK);
	}

	@RequestMapping(value = "/billings/{id}/downloadReport/Kostenuebersicht.pdf", method = RequestMethod.GET, produces = MediaType.APPLICATION_PDF_VALUE)
	public @ResponseBody
	ResponseEntity<?> downloadBillingReport(@PathVariable("id") UUID id) throws IOException {

		Billing billing = billingRepository.findById(id).orElse(null);
		if (billing == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		try {
			BillingListItem billingListItem = billingService.startCreateBillingListItem(billing);
			File reportFile = billingService.createAndSaveBillingReport(billingListItem, billing);

			InputStream in = null;
			byte[] bytes;

			in = new FileInputStream(reportFile);
			bytes = IOUtils.toByteArray(in);
			return new ResponseEntity<>(bytes, HttpStatus.OK);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return new ResponseEntity<>( HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (IOException e) {
			e.printStackTrace();
			return new ResponseEntity<>( HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Transactional
	@RequestMapping(value = "/billings/payment", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<?> createPayment(@RequestBody BillingConfigDTO billingConfigDTO) throws IOException, MessagingException {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser loggedInUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		billingService.createBilling(loggedInUser, billingConfigDTO);

		return new ResponseEntity<>(HttpStatus.OK);
	}
}