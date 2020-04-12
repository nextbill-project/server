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

import de.nextbill.domain.dtos.InvoiceDTO;
import de.nextbill.domain.dtos.MessageDTO;
import de.nextbill.domain.enums.CorrectionStatus;
import de.nextbill.domain.enums.FirebaseMessageType;
import de.nextbill.domain.enums.InvoiceFailureTypeEnum;
import de.nextbill.domain.enums.InvoiceStatusEnum;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.BasicData;
import de.nextbill.domain.model.Invoice;
import de.nextbill.domain.model.InvoiceFailure;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.BasicDataRepository;
import de.nextbill.domain.repositories.InvoiceFailureRepository;
import de.nextbill.domain.repositories.InvoiceRepository;
import de.nextbill.domain.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class WorkflowController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private MessagingService messagingService;

    @Autowired
    private BasicDataRepository basicDataRepository;

    @Autowired
    private FirebaseService firebaseService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private InvoiceHelperService invoiceHelperService;

    @Autowired
    private BusinessPartnerService businessPartnerService;

    @Autowired
    private InvoiceFailureRepository invoiceFailureRepository;

    @RequestMapping(value = "/invoices/setCorrectionStatus", method = RequestMethod.POST)
    public ResponseEntity<?> setCorrectionStatus(@RequestBody List<InvoiceDTO> invoiceDTOs, @RequestParam("setWorkflow") CorrectionStatus correctionStatus) {

        String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

        List<InvoiceDTO> invoiceDTOResults = new ArrayList<>();
        for (InvoiceDTO invoiceDTO : invoiceDTOs) {
            Invoice invoice = invoiceRepository.findById(invoiceDTO.getInvoiceId()).orElse(null);
            invoiceService.setCorrectionStatus(invoice, correctionStatus, currentUser);

            Double costPaidOld = invoice.getCostPaid() != null ? invoice.getCostPaid().doubleValue() : 0;
            Double costPaidNew = invoiceDTO.getCostPaid() != null ? invoiceDTO.getCostPaid().doubleValue() : 0;
            billingService.refreshBillingsIfNecessaryAsync(invoice, costPaidNew, costPaidOld);

            businessPartnerService.refreshBusinessPartnerMetrics(invoice);

            invoiceDTOResults.add(invoiceHelperService.mapToDTO(invoice, false));
        }

        return new ResponseEntity<>(invoiceDTOResults, HttpStatus.OK);
    }

    @RequestMapping(value = "/invoices/{cid}/mistake", method = RequestMethod.POST)
    public ResponseEntity<?> mistakeInInvoice(@PathVariable(value="cid") UUID cid, @RequestBody MessageDTO messageDTO) {

        Invoice invoice = invoiceRepository.findById(cid).orElse(null);

        if (invoice == null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

        if (messageDTO != null){
            BasicData basicData = messagingService.createMistakeMessage(messageDTO.getMessage(), invoice, currentUser);
            MessageDTO messageDTO1 = messagingService.createMessageDTOFromJson(basicData.getValue());
            AppUser createdBy = appUserRepository.findOneByAppUserId(UUID.fromString(invoice.getCreatedBy()));
            firebaseService.sendTextMessage(createdBy, FirebaseMessageType.MESSAGE_CONFLICT, messageDTO1.getSubject(), messageDTO1.getMessage());

            InvoiceFailure invoiceFailure = new InvoiceFailure();
            invoiceFailure.setInvoiceFailureId(UUID.randomUUID());
            invoiceFailure.setInvoice(invoice);
            invoiceFailure.setInvoiceFailureTypeEnum(InvoiceFailureTypeEnum.USER_PROBLEM);
            invoiceFailure.setMessage(currentUser.getAppUserName() + ": " + messageDTO.getMessage());
            invoiceFailureRepository.save(invoiceFailure);

            invoice.setInvoiceStatusEnum(InvoiceStatusEnum.CHECK);
            invoiceRepository.save(invoice);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/invoices/{cid}/problemSolved", method = RequestMethod.DELETE)
    public ResponseEntity<?> problemSolvedInInvoice(@PathVariable(value="cid") UUID cid) {

        BasicData basicData = basicDataRepository.findById(cid).orElse(null);

        if (basicData == null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (basicData.getObject2Id() != null && basicData.getObject1Id() != null){
            AppUser appUserFrom = appUserRepository.findById(UUID.fromString(basicData.getObject2Id())).orElse(null);
            Invoice invoice = invoiceRepository.findById(UUID.fromString(basicData.getObject1Id())).orElse(null);
            if (appUserFrom != null && invoice != null){

                List<InvoiceFailure> invoiceFailures = invoiceFailureRepository.findByInvoice(invoice);
                invoiceFailureRepository.deleteAll(invoiceFailures);

                BasicData basicData2 = messagingService.createMistakeSolvedMessage(invoice, appUserFrom);
                MessageDTO messageDTO1 = messagingService.createMessageDTOFromJson(basicData2.getValue());
                firebaseService.sendTextMessage(appUserFrom, FirebaseMessageType.MESSAGE_CONFLICT, messageDTO1.getSubject(), messageDTO1.getMessage());
            }
        }

        basicDataRepository.delete(basicData);

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
