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

import de.nextbill.domain.dtos.InvoiceListDTO;
import de.nextbill.domain.dtos.SearchDTO;
import de.nextbill.domain.enums.InvoiceStatusEnum;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.services.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping({ "webapp/api","api" })
public class SearchController {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private SearchService searchService;

    @ResponseBody
    @RequestMapping(value = "/search/withFilterBean", method = RequestMethod.POST)
    public ResponseEntity<?> searchWithFilter(@RequestBody SearchDTO searchDTOInput, @RequestParam("onlyStandingOrder") Optional<Boolean> onlyStandingOrder, @RequestParam("invoiceStatusEnum") Optional<InvoiceStatusEnum> invoiceStatusEnum, @RequestParam("pageNumber") Optional<Integer> pageNumber) {

        String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

        InvoiceListDTO invoiceListDTO = searchService.searchForInvoices(currentUser, searchDTOInput, onlyStandingOrder, invoiceStatusEnum, pageNumber);

        if ((!onlyStandingOrder.isPresent() || !onlyStandingOrder.get()) && invoiceStatusEnum.isPresent() && invoiceStatusEnum.get().equals(InvoiceStatusEnum.READY)){
            return new ResponseEntity<>(invoiceListDTO, HttpStatus.OK);
        }


        return new ResponseEntity<>(invoiceListDTO.getInvoiceDTOs(), HttpStatus.OK);
    }

}
