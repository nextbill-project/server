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

package services;

import de.nextbill.Application;
import de.nextbill.domain.model.InvoiceCategory;
import de.nextbill.domain.services.InvoiceCategoryService;
import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@Slf4j
@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Application.class})
public class InvoiceCategoryServiceTest {

    @Autowired
    private InvoiceCategoryService invoiceCategoryService;

    @Test
    public void executeRequestToWikipediaForKleidung() {

        InvoiceCategory invoiceCategory = invoiceCategoryService.requestExternalServicesForInvoiceCategory("Tom Tailor", null);

        assertThat(invoiceCategory).isNotNull();
        assertThat(invoiceCategory.getInvoiceCategoryName()).isEqualTo("Kleidung & Accessoires");
    }


}
