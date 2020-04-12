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

import de.nextbill.domain.dtos.SearchDTO;
import de.nextbill.domain.enums.InvoiceStatusEnum;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.pojos.InvoiceCostDistributionItem;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.services.ExportService;
import de.nextbill.domain.services.SearchService;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping({ "webapp/api","api" })
public class ExportController {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private SearchService searchService;

    @Autowired
    private ExportService exportService;

    @ResponseBody
    @RequestMapping(value = "/export/withFilter/export.csv", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] exportCSV(@RequestParam("filter") String searchDtoAsBase64, @RequestParam("invoiceStatusEnum") InvoiceStatusEnum invoiceStatusEnum) throws IOException {

        String searchDtoJson = new String(Base64.getDecoder().decode(searchDtoAsBase64));
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SearchDTO searchDTOInput = objectMapper.readValue(searchDtoJson, SearchDTO.class);

        String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

        List<InvoiceCostDistributionItem> invoiceCostDistributionItems = searchService.searchForInvoices(currentUser, searchDTOInput, invoiceStatusEnum);

        ByteArrayOutputStream byteArrayOutputStream = exportService.exportToCsv(invoiceCostDistributionItems);

        return byteArrayOutputStream.toByteArray();
    }

}
