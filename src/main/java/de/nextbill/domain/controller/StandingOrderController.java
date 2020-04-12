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

import de.nextbill.domain.dtos.StandingOrderDTO;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.Invoice;
import de.nextbill.domain.model.StandingOrder;
import de.nextbill.domain.model.StandingOrderItem;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.InvoiceRepository;
import de.nextbill.domain.repositories.StandingOrderItemRepository;
import de.nextbill.domain.repositories.StandingOrderRepository;
import de.nextbill.domain.services.InvoiceService;
import de.nextbill.domain.services.StandingOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping({ "webapp/api","api" })
public class StandingOrderController {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private StandingOrderRepository standingOrderRepository;

    @Autowired
    private StandingOrderItemRepository standingOrderItemRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private StandingOrderService standingOrderService;

    @Autowired
    private AppUserRepository appUserRepository;

    @ResponseBody
    @RequestMapping(value = "/standingOrders", method = RequestMethod.POST)
    public ResponseEntity<?> createStandingOrder(@RequestBody StandingOrderDTO standingOrderDTO, @RequestParam("deleteInvoiceTemplate") Optional<Boolean> deleteInvoiceTemplate) {

        String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

        Invoice invoiceTemplate = invoiceRepository.findById(standingOrderDTO.getInvoiceTemplateId()).orElse(null);

        if (standingOrderItemRepository.findOneByCreatedInvoice(invoiceTemplate) != null){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        standingOrderService.createStandingOrder(standingOrderDTO, invoiceTemplate, currentUser, deleteInvoiceTemplate);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/standingOrders/invoiceTemplates/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> updateByInvoiceTemplate(@PathVariable("id") UUID id, @RequestBody StandingOrderDTO standingOrderDTO) {

        Invoice invoiceTemplate = invoiceRepository.findById(id).orElse(null);
        if (invoiceTemplate == null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        StandingOrder standingOrder = standingOrderRepository.findOneByStandingOrderInvoiceTemplate(invoiceTemplate);
        if (standingOrder == null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        standingOrder.setStartDate(standingOrderDTO.getStartDate());
        standingOrder.setRepetitionTypeEnum(standingOrderDTO.getRepetitionTypeEnum());

        standingOrder = standingOrderRepository.save(standingOrder);

        standingOrderService.generateStandingOrderInvoices(standingOrder);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/standingOrders/invoiceTemplates/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getByInvoiceTemplate(@PathVariable("id") UUID id) {

        Invoice invoiceTemplate = invoiceRepository.findById(id).orElse(null);
        if (invoiceTemplate == null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        StandingOrder standingOrder = standingOrderRepository.findOneByStandingOrderInvoiceTemplate(invoiceTemplate);
        if (standingOrder == null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        StandingOrderDTO standingOrderDTO = new StandingOrderDTO();
        standingOrderDTO.setRepetitionTypeEnum(standingOrder.getRepetitionTypeEnum());
        standingOrderDTO.setStartDate(standingOrder.getStartDate());

        return new ResponseEntity<>(standingOrderDTO, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/standingOrders/deleteByCreatedInvoice/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteByCreatedInvoice(@PathVariable("id") UUID id) {

        Invoice createdInvoice = invoiceRepository.findById(id).orElse(null);
        if (createdInvoice == null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        StandingOrder standingOrder = standingOrderRepository.findOneByStandingOrderInvoice(createdInvoice);
        if (standingOrder == null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Invoice invoiceTemplate = standingOrder.getInvoiceTemplate();

        List<StandingOrderItem> standingOrderItems = standingOrderItemRepository.findItemsForStandingOrder(standingOrder.getStandingOrderId());
        standingOrderItemRepository.deleteAll(standingOrderItems);
        standingOrderRepository.delete(standingOrder);

        invoiceService.deleteInvoice(invoiceTemplate, true, true);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/standingOrders/deleteByInvoiceTemplate/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteByInvoiceTemplate(@PathVariable("id") UUID id) {

        Invoice invoiceTemplate = invoiceRepository.findById(id).orElse(null);
        if (invoiceTemplate == null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        StandingOrder standingOrder = standingOrderRepository.findOneByStandingOrderInvoiceTemplate(invoiceTemplate);
        if (standingOrder == null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        List<StandingOrderItem> standingOrderItems = standingOrderItemRepository.findItemsForStandingOrder(standingOrder.getStandingOrderId());
        standingOrderItemRepository.deleteAll(standingOrderItems);
        standingOrderRepository.delete(standingOrder);

        invoiceService.deleteInvoice(invoiceTemplate, true, true);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
