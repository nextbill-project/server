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

package de.nextbill.domain.services;

import de.nextbill.domain.dtos.BusinessPartnerDTO;
import de.nextbill.domain.enums.BasicStatusEnum;
import de.nextbill.domain.enums.InvoiceStatusEnum;
import de.nextbill.domain.enums.PaymentPersonTypeEnum;
import de.nextbill.domain.enums.PublicStatus;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.BusinessPartner;
import de.nextbill.domain.model.Invoice;
import de.nextbill.domain.model.InvoiceCategory;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.BusinessPartnerRepository;
import de.nextbill.domain.utils.BeanMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class BusinessPartnerService {

    @Autowired
    private BusinessPartnerRepository businessPartnerRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ComparisonService comparisonService;

    public BusinessPartnerDTO mapToDTO(BusinessPartner businessPartner){
        return mapToDTO(businessPartner, null);
    }

    public BusinessPartnerDTO mapToDTO(BusinessPartner businessPartner, AppUser currentUser){
        BeanMapper beanMapper = new BeanMapper();
        BusinessPartnerDTO resultBusinessPartnerDTO = beanMapper.map(businessPartner, BusinessPartnerDTO.class);

        if (businessPartner.getBasicStatusEnum() == null){
            businessPartner.setBasicStatusEnum(BasicStatusEnum.OK);
            businessPartnerRepository.save(businessPartner);
            resultBusinessPartnerDTO.setBasicStatusEnum(BasicStatusEnum.OK);
        }

        if (currentUser == null){
            String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
            currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));
        }

        AppUser appUser = businessPartner.getAppUser();
        if (appUser != null && currentUser != null && appUser.getAppUserId().equals(currentUser.getAppUserId())){
            resultBusinessPartnerDTO.setCanBeDeleted(true);
        }else{
            resultBusinessPartnerDTO.setCanBeDeleted(false);
        }

        if (appUser != null){
            resultBusinessPartnerDTO.setAppUserId(appUser.getAppUserId());
        }

        return resultBusinessPartnerDTO;
    }

    public InvoiceCategory mapToEntity(BusinessPartnerDTO businessPartnerDTO){
        BeanMapper beanMapper = new BeanMapper();
        InvoiceCategory businessPartner = beanMapper.map(businessPartnerDTO, InvoiceCategory.class);

        UUID appUserId = businessPartnerDTO.getAppUserId();
        if (appUserId != null){
            AppUser appUser = appUserRepository.findById(appUserId).orElse(null);
            businessPartner.setAppUser(appUser);
        }

        return businessPartner;
    }

    public BusinessPartner findOrCreateBusinessPartner(String businessPartnerName, String appUserId){

        List<BusinessPartner> businessPartners = businessPartnerRepository.findAllByBasicStatusEnum(BasicStatusEnum.OK);

        for (BusinessPartner businessPartner : businessPartners) {

            List<Pattern> patternsOfBusinessPartner = comparisonService.convertStringToPatterns(businessPartner.getBusinessPartnerReceiptName());

            for (Pattern pattern : patternsOfBusinessPartner) {
                Matcher m = pattern.matcher(businessPartnerName);

                while (m.find()) {
                    String foundBusinessPartner = m.group(1);

                    Double similarity = comparisonService.similarityWithComparableStrings(businessPartner.getBusinessPartnerReceiptName(), foundBusinessPartner);
                    if (similarity >= 0.85) {

                        long currentAmountUsages = businessPartner.getAmountUsages() != null ? businessPartner.getAmountUsages() : 0;

                        currentAmountUsages = currentAmountUsages + 1;
                        businessPartner.setAmountUsages(currentAmountUsages);

                        return businessPartner;
                    }
                }
            }
        }

        BusinessPartner businessPartner = new BusinessPartner();
        businessPartner.setBasicStatusEnum(BasicStatusEnum.OK);
        businessPartner.setBusinessPartnerId(UUID.randomUUID());
        businessPartner.setBusinessPartnerName(businessPartnerName);
        businessPartner.setBusinessPartnerReceiptName(businessPartnerName);
        businessPartner.setBusinessPartnerPublicStatus(PublicStatus.EXPERIMENTAL);
        businessPartner.setCategoryPublicStatus(PublicStatus.EXPERIMENTAL);

        businessPartner = businessPartnerRepository.save(businessPartner);
        businessPartner.setCreatedBy(appUserId);
        businessPartner = businessPartnerRepository.save(businessPartner);

        return businessPartner;
    }

    public void refreshBusinessPartnerMetrics(Invoice invoice){

        if (invoice == null){
            return;
        }

        UUID businessPartnerUuid = invoice.getPaymentRecipientTypeEnum() != null && PaymentPersonTypeEnum.BUSINESS_PARTNER.equals(invoice.getPaymentRecipientTypeEnum()) ? invoice.getPaymentRecipientId() : null;
        if (businessPartnerUuid == null){
            businessPartnerUuid = invoice.getPayerTypeEnum() != null && PaymentPersonTypeEnum.BUSINESS_PARTNER.equals(invoice.getPayerTypeEnum()) ? invoice.getPayerId() : null;
        }

        BusinessPartner businessPartner = businessPartnerUuid != null ? businessPartnerRepository.findById(businessPartnerUuid).orElse(null) : null;

        if (businessPartner != null){

            long currentAmountUsages = businessPartner.getAmountUsages() != null ? businessPartner.getAmountUsages() : 0;
            long amountBusinessPartnerCategoryUsage = businessPartner.getAmountBusinessPartnerCategoryUsage() != null ? businessPartner.getAmountBusinessPartnerCategoryUsage() : 0;

            if (InvoiceStatusEnum.READY.equals(invoice.getInvoiceStatusEnum())){
                currentAmountUsages = currentAmountUsages + 1;
                businessPartner.setAmountUsages(currentAmountUsages);

                InvoiceCategory invoiceCategoryOfInvoice = invoice.getInvoiceCategory();
                if (invoiceCategoryOfInvoice != null) {
                    InvoiceCategory invoiceCategoryOfBusinssPartner = businessPartner.getInvoiceCategory();

                    if (invoiceCategoryOfBusinssPartner != null &&
                            invoiceCategoryOfBusinssPartner.getInvoiceCategoryId().toString().equals(invoiceCategoryOfInvoice.getInvoiceCategoryId().toString())) {
                        amountBusinessPartnerCategoryUsage = amountBusinessPartnerCategoryUsage + 1;
                        businessPartner.setAmountBusinessPartnerCategoryUsage(amountBusinessPartnerCategoryUsage);
                    }
                }
            }

            businessPartnerRepository.save(businessPartner);
        }

    }

    public void analyzeStatusOfBusinessPartners() {
        List<BusinessPartner> businessPartners = businessPartnerRepository.findAllByAppUserIsNullAndBasicStatusEnum(BasicStatusEnum.OK);

        for (BusinessPartner businessPartner : businessPartners) {

            if (businessPartner.getAmountUsages() == null) businessPartner.setAmountUsages(0L);
            if (businessPartner.getBusinessPartnerPublicStatus() == null) {
                AppUser appUser = businessPartner.getAppUser();
                if (appUser == null){
                    businessPartner.setBusinessPartnerPublicStatus(PublicStatus.PUBLIC);
                } else {
                    businessPartner.setBusinessPartnerPublicStatus(PublicStatus.PRIVATE);
                    businessPartner.setCategoryPublicStatus(PublicStatus.PRIVATE);
                }
            }else{
                AppUser appUser = businessPartner.getAppUser();
                if (appUser != null){
                    businessPartner.setBusinessPartnerPublicStatus(PublicStatus.PRIVATE);
                    businessPartner.setCategoryPublicStatus(PublicStatus.PRIVATE);
                }
            }

            InvoiceCategory invoiceCategory = businessPartner.getInvoiceCategory();
            if (invoiceCategory != null) {
                if (businessPartner.getAmountBusinessPartnerCategoryUsage() == null) businessPartner.setAmountBusinessPartnerCategoryUsage(0L);

                if (businessPartner.getCategoryPublicStatus() == null) {
                    if (invoiceCategory.getAppUser() == null &&
                            (businessPartner.getBusinessPartnerPublicStatus().equals(PublicStatus.EXPERIMENTAL) || businessPartner.getBusinessPartnerPublicStatus().equals(PublicStatus.PUBLIC)) ) {
                        businessPartner.setCategoryPublicStatus(PublicStatus.EXPERIMENTAL);
                    }else{
                        businessPartner.setCategoryPublicStatus(PublicStatus.PRIVATE);
                    }
                }else{
                    AppUser appUser = invoiceCategory.getAppUser();
                    if (appUser != null){
                        businessPartner.setCategoryPublicStatus(PublicStatus.PRIVATE);
                    }
                }
            }else{
                businessPartner.setAmountBusinessPartnerCategoryUsage(null);
                businessPartner.setCategoryPublicStatus(null);
            }

            businessPartner = businessPartnerRepository.save(businessPartner);

            Date businessPartnerCreatedDate = businessPartner.getCreatedDate();

            if (businessPartnerCreatedDate == null) {
                businessPartner.setCreatedDate(new Date());
                businessPartner = businessPartnerRepository.save(businessPartner);
                businessPartnerCreatedDate = businessPartner.getCreatedDate();
            }

            LocalDate businessPartnerCreatedLocalDate = LocalDateTime.ofInstant(businessPartnerCreatedDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            businessPartnerCreatedLocalDate = businessPartnerCreatedLocalDate.plusMonths(4);
            Date businessPartnerFutureDate = Date.from(businessPartnerCreatedLocalDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());


            if (businessPartner.getBusinessPartnerPublicStatus().equals(PublicStatus.EXPERIMENTAL)){
                if (businessPartnerFutureDate.after(new Date())) {

                    if (businessPartner.getAmountUsages() > 6) {
                        businessPartner.setBusinessPartnerPublicStatus(PublicStatus.PUBLIC);
                        businessPartner.setAppUser(null);
                    }

                }else{
                    String appUserId = businessPartner.getCreatedBy();

                    AppUser appUser = null;
                    if (appUserId != null) {
                        appUser = appUserRepository.findById(UUID.fromString(appUserId)).orElse(null);
                    }

                    if (appUser != null && !appUser.getAppUserName().equals("BatchUser")){
                        businessPartner.setBusinessPartnerPublicStatus(PublicStatus.PRIVATE);

                        businessPartner.setAppUser(appUser);

                        if (invoiceCategory != null){
                            businessPartner.setCategoryPublicStatus(PublicStatus.PRIVATE);
                        }else{
                            businessPartner.setCategoryPublicStatus(null);
                        }
                    }else{
                        businessPartner.setBasicStatusEnum(BasicStatusEnum.DELETED);
                    }

                }

            }

            if (invoiceCategory != null && businessPartner.getBusinessPartnerPublicStatus().equals(PublicStatus.PUBLIC) && businessPartner.getBasicStatusEnum().equals(BasicStatusEnum.OK)) {

                if (businessPartner.getCategoryPublicStatus() != null && businessPartner.getCategoryPublicStatus().equals(PublicStatus.EXPERIMENTAL)){

                    if (businessPartnerFutureDate.after(new Date())) {

                        Double ratio = 0D;
                        if (businessPartner.getAmountUsages() > 0){
                            ratio = new BigDecimal(businessPartner.getAmountBusinessPartnerCategoryUsage()).divide(new BigDecimal(businessPartner.getAmountUsages()), 10, RoundingMode.HALF_EVEN).doubleValue();
                        }

                        if (businessPartner.getAmountBusinessPartnerCategoryUsage() > 6 && ratio > 0.7) {
                            businessPartner.setCategoryPublicStatus(PublicStatus.PUBLIC);
                        }
                    }else{
                        businessPartner.setInvoiceCategory(null);
//                        Should be checked manually, because this combination of business partner and category is little used.
                        businessPartner.setCategoryPublicStatus(PublicStatus.MANUAL_CHECK);
                    }

                }
            }

            businessPartnerRepository.save(businessPartner);
        }
    }
}
