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

import de.nextbill.domain.dtos.InvoiceFailureDTO;
import de.nextbill.domain.enums.InvoiceStatusEnum;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.Invoice;
import de.nextbill.domain.model.InvoiceFailure;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.InvoiceFailureRepository;
import de.nextbill.domain.repositories.InvoiceRepository;
import de.nextbill.domain.utils.BeanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
public class InvoiceFailureController {

	@Autowired
	private InvoiceRepository invoiceRepository;

	@Autowired
	private InvoiceFailureRepository invoiceFailureRepository;

	@Autowired
	private AppUserRepository appUserRepository;

	@RequestMapping(value = "/invoicefailures", method = RequestMethod.GET)
	public @ResponseBody
	ResponseEntity<?> list() {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser appUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		BeanMapper beanMapper = new BeanMapper();

		List<Invoice> invoices = new ArrayList<>();
		invoices.addAll(invoiceRepository.findWithUserAndNotStatus(appUser.getAppUserId().toString(), InvoiceStatusEnum.DELETED, PageRequest.of( 0, Integer.MAX_VALUE, Sort.Direction.ASC, "dateOfInvoice" )).getContent());

		List<InvoiceFailure> invoiceFailures = invoiceFailureRepository.findByInvoiceIn(invoices);
		List<InvoiceFailureDTO> invoiceFailureDTOs = new ArrayList<>();
		for (InvoiceFailure invoiceFailure : invoiceFailures) {
			InvoiceFailureDTO invoiceFailureDTO = beanMapper.map(invoiceFailure, InvoiceFailureDTO.class);
			if (invoiceFailure.getInvoice() != null){
				invoiceFailureDTO.setInvoiceId(invoiceFailure.getInvoice().getInvoiceId());
			}
			invoiceFailureDTOs.add(invoiceFailureDTO);
		}

		return new ResponseEntity<>(invoiceFailureDTOs, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/invoicefailures/{cid}", method = RequestMethod.DELETE)
	public ResponseEntity<?> deleteInvoiceFailure(@PathVariable(value="cid") UUID cid) {
		InvoiceFailure invoiceFailure = invoiceFailureRepository.findById(cid).orElse(null);
		
		if (invoiceFailure == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		invoiceFailureRepository.delete(invoiceFailure);

		return new ResponseEntity<>(HttpStatus.OK);
	}
}