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

import de.nextbill.domain.dtos.*;
import de.nextbill.domain.enums.BasicStatusEnum;
import de.nextbill.domain.enums.InvoiceStatusEnum;
import de.nextbill.domain.model.*;
import de.nextbill.domain.pojos.DatabaseChange;
import de.nextbill.domain.repositories.*;
import de.nextbill.domain.utils.BeanMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@Service
public class AuditService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private BusinessPartnerService businessPartnerService;

    @Autowired
    private CostDistributionItemRepository costDistributionItemRepository;

    @Autowired
    private InvoiceCategoryService invoiceCategoryService;

    @Autowired
    private InvoiceHelperService invoiceHelperService;

    @Autowired
    private UserContactRepository userContactRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CostDistributionItemService costDistributionItemService;

    public <T> List<DatabaseChange> findDatabaseChange(Class<T> classType, List<UUID> allowedIds, Date startDate, CrudRepository jpaRepository){

        if (allowedIds == null || allowedIds.isEmpty()){
            return new ArrayList<>();
        }

        List<Object> objects = new ArrayList<>();
        if (AppUser.class.equals(classType)) {
            AppUserRepository repository = (AppUserRepository) jpaRepository;
            objects.addAll(repository.findAllByAppUserIdInAndLastModifiedAtAfter(allowedIds, startDate));
        }else if (CostDistribution.class.equals(classType)){
            CostDistributionRepository repository = (CostDistributionRepository) jpaRepository;
            objects.addAll(repository.findAllByCostDistributionIdInAndLastModifiedAtAfter(allowedIds, startDate));
        }else if (CostDistributionItem.class.equals(classType)){
            CostDistributionItemRepository repository = (CostDistributionItemRepository) jpaRepository;
            objects.addAll(repository.findAllByCostDistributionItemIdInAndLastModifiedAtAfter(allowedIds, startDate));
        }else if (InvoiceFailure.class.equals(classType)){
            InvoiceFailureRepository repository = (InvoiceFailureRepository) jpaRepository;
            objects.addAll(repository.findAllByInvoiceFailureIdInAndLastModifiedAtAfter(allowedIds, startDate));
        }else if (InvoiceCategory.class.equals(classType)){
            InvoiceCategoryRepository repository = (InvoiceCategoryRepository) jpaRepository;
            objects.addAll(repository.findAllByInvoiceCategoryIdInAndLastModifiedAtAfter(allowedIds, startDate));
        }else if (UserContact.class.equals(classType)){
            UserContactRepository repository = (UserContactRepository) jpaRepository;
            objects.addAll(repository.findAllByUserContactIdInAndLastModifiedAtAfter(allowedIds, startDate));
        }else if (BusinessPartner.class.equals(classType)){
            BusinessPartnerRepository repository = (BusinessPartnerRepository) jpaRepository;
            objects.addAll(repository.findAllByBusinessPartnerIdInAndLastModifiedAtAfter(allowedIds, startDate));
        }else if (Invoice.class.equals(classType)){
            InvoiceRepository repository = (InvoiceRepository) jpaRepository;
            objects.addAll(repository.findAllByInvoiceIdInAndLastModifiedAtAfter(allowedIds, startDate));
        }else if (BasicData.class.equals(classType)){
            BasicDataRepository repository = (BasicDataRepository) jpaRepository;
            objects.addAll(repository.findAllByBasicDataIdInAndLastModifiedAtAfter(allowedIds, startDate));
        }

        List<DatabaseChange> databaseChangeBeen = new ArrayList<>();

        String fieldName = findPrimaryKeyFieldName(classType);

        for (Object object : objects) {
            AuditFields auditFields = (AuditFields) object;
            Integer revisionType = 1;
            if (auditFields.getLastModifiedAt() != null && auditFields.getCreatedDate() != null){
                revisionType = auditFields.getLastModifiedAt().compareTo(auditFields.getCreatedDate()) == 0 ? 0 : 1;
            }

            DatabaseChange<T> databaseChange = new DatabaseChange();
            databaseChange.setRevisionType(revisionType);
            UUID id = findIdForObject(object, fieldName);
            T objectResult = (T) jpaRepository.findById(id).orElse(null);
            databaseChange.setObject(objectResult);

            databaseChangeBeen.add(databaseChange);
        }

        return databaseChangeBeen;
    }

    public UUID findIdForObject(Object object, String getterName){
        try {
            Method method = object.getClass().getMethod(getterName);
            return (UUID) method.invoke(object);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String findPrimaryKeyFieldName(Class classType){
        Field[] fields = classType.getDeclaredFields();
        String fieldName = null;
        for (Field field : fields) {
            List<Annotation> annotations = Arrays.asList(field.getAnnotations());
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(javax.persistence.Id.class)){
                    fieldName = field.getName();
                }
            }
        }
        if (fieldName != null){
            return "get" + fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);

        }
        return fieldName;
    }

    public <T> DatabaseChange convertToDatabaseChangeDTO(T object){
        DatabaseChange<T> databaseChange = new DatabaseChange<>();
        databaseChange.setRevisionType(1);
        databaseChange.setObject(object);

        return databaseChange;
    }

    public <T> List<DatabaseChange<T>> convertToDatabaseChangeDTOs(List<T> objects){

        List<DatabaseChange<T>> databaseChanges = new ArrayList<>();
        for (T t : objects) {
            DatabaseChange<T> databaseChange = new DatabaseChange<>();
            databaseChange.setRevisionType(1);
            databaseChange.setObject(t);
            databaseChanges.add(databaseChange);
        }

        return databaseChanges;
    }

    public AllDataPatchDTO mapToObject(List<DatabaseChange> databaseChangeBeen, AppUser currentUser, boolean isFullUpdate){
        BeanMapper beanMapper = new BeanMapper();

        AllDataPatchDTO allDataPatchDTO = new AllDataPatchDTO();

        Map<UUID, StandingOrder> invoiceStandingOrderMap = invoiceHelperService.findInvoiceStandingOrdersByDataChangeBeans(databaseChangeBeen);

        for (DatabaseChange databaseChange : databaseChangeBeen) {

            if (databaseChange.getClassType().equals(AppUser.class)){
                DatabaseChangeDTO<AppUserDTO> databaseChangeDTO = new DatabaseChangeDTO<>();
                databaseChangeDTO.setRevisionType(databaseChange.getRevisionType());

                AppUserDTO appUserDTO = beanMapper.map(databaseChange.getObject(), AppUserDTO.class);

                Set<AppRight> appRights = ((AppUser) databaseChange.getObject()).getUserRights();

                List<AppRightDTO> appRightDTOs = new ArrayList<>();
                for (AppRight appRight : appRights) {
                    appRightDTOs.add(beanMapper.map(appRight, AppRightDTO.class));
                }
                appUserDTO.setAppRightDTOs(appRightDTOs);

                databaseChangeDTO.setMappedObject(appUserDTO);

                allDataPatchDTO.getAppUserDTOs().add(databaseChangeDTO);
            }else if (databaseChange.getClassType().equals(BasicData.class)){
                DatabaseChangeDTO<BasicDataDTO> databaseChangeDTO = new DatabaseChangeDTO<>();
                databaseChangeDTO.setRevisionType(databaseChange.getRevisionType());

                BasicDataDTO basicDataDTO = beanMapper.map(databaseChange.getObject(), BasicDataDTO.class);
                basicDataDTO.setAppUserId(currentUser.getAppUserId());

                databaseChangeDTO.setMappedObject(basicDataDTO);

                allDataPatchDTO.getBasicDataDTOs().add(databaseChangeDTO);
            }
            else if (databaseChange.getClassType().equals(BasicDataDTO.class)){
                DatabaseChangeDTO<BasicDataDTO> databaseChangeDTO = new DatabaseChangeDTO<>();
                databaseChangeDTO.setRevisionType(databaseChange.getRevisionType());

                databaseChangeDTO.setMappedObject((BasicDataDTO) databaseChange.getObject());

                allDataPatchDTO.getBasicDataDTOs().add(databaseChangeDTO);
            }else if (databaseChange.getClassType().equals(BusinessPartner.class)){
                DatabaseChangeDTO<BusinessPartnerDTO> databaseChangeDTO = new DatabaseChangeDTO<>();
                databaseChangeDTO.setRevisionType(databaseChange.getRevisionType());

                BusinessPartnerDTO resultBusinessPartnerDTO = businessPartnerService.mapToDTO((BusinessPartner) databaseChange.getObject(), currentUser);

                databaseChangeDTO.setMappedObject(resultBusinessPartnerDTO);

                allDataPatchDTO.getBusinessPartnerDTOs().add(databaseChangeDTO);
            }else if (databaseChange.getClassType().equals(CostDistribution.class)){
                DatabaseChangeDTO<CostDistributionDTO> databaseChangeDTO = new DatabaseChangeDTO<>();
                Integer revisionType = databaseChange.getRevisionType();

                CostDistribution costDistribution = (CostDistribution) databaseChange.getObject();
                CostDistributionDTO costDistributionDTO = beanMapper.map(costDistribution, CostDistributionDTO.class);
                if (costDistribution.getCreatedBy() != null){
                    AppUser createdBy = appUserRepository.findOneByAppUserId(UUID.fromString(costDistribution.getCreatedBy()));
                    costDistributionDTO.setCreatedById(createdBy.getAppUserId());
                }

                if (costDistribution.getBasicStatusEnum() != null && costDistribution.getBasicStatusEnum().equals(BasicStatusEnum.DELETED)){
                    revisionType = 2;
                    costDistributionDTO.setCostDistributionId(costDistribution.getCostDistributionId());
                }else{
                    List<CostDistributionItemDTO> costDistributionItemDTOs = new ArrayList<>();
                    List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByCostDistribution(costDistribution);
                    for (CostDistributionItem costDistributionItem : costDistributionItems) {
                        costDistributionItemDTOs.add(costDistributionItemService.mapToDTO(costDistributionItem));
                    }
                    costDistributionDTO.setCostDistributionItemDTOS(costDistributionItemDTOs);
                }

                databaseChangeDTO.setRevisionType(revisionType);
                databaseChangeDTO.setMappedObject(costDistributionDTO);

                allDataPatchDTO.getCostDistributionDTOs().add(databaseChangeDTO);
            }else if (databaseChange.getClassType().equals(CostDistributionItem.class)){
                DatabaseChangeDTO<CostDistributionItemDTO> databaseChangeDTO = new DatabaseChangeDTO<>();
                Integer revisionType = databaseChange.getRevisionType();

                CostDistributionItem costDistributionItem = (CostDistributionItem) databaseChange.getObject();

                CostDistributionItemDTO costDistributionItemDTO = new CostDistributionItemDTO();
                Invoice invoice = costDistributionItem.getInvoice();
                if (invoice != null && invoice.getInvoiceStatusEnum().equals(InvoiceStatusEnum.DELETED)){
                    revisionType = 2;
                    costDistributionItemDTO.setCostDistributionItemId(costDistributionItem.getCostDistributionItemId());
                }else{
                    costDistributionItemDTO = costDistributionItemService.mapToDTO(costDistributionItem, true);
                }

                databaseChangeDTO.setRevisionType(revisionType);
                databaseChangeDTO.setMappedObject(costDistributionItemDTO);

                allDataPatchDTO.getCostDistributionItemDTOs().add(databaseChangeDTO);
            }else if (databaseChange.getClassType().equals(Invoice.class)){
                DatabaseChangeDTO<InvoiceDTO> databaseChangeDTO = new DatabaseChangeDTO<>();
                Integer revisionType = databaseChange.getRevisionType();

                InvoiceDTO invoiceDTO = new InvoiceDTO();
                Invoice invoice = (Invoice) databaseChange.getObject();
                if (invoice.getInvoiceStatusEnum() != null && invoice.getInvoiceStatusEnum().equals(InvoiceStatusEnum.DELETED)){
                    revisionType = 2;
                    invoiceDTO.setInvoiceId(invoice.getInvoiceId());
                }else{
                    invoiceDTO = invoiceHelperService.mapToDTO(invoice, true, currentUser, invoiceStandingOrderMap);

                    List<CostDistributionItemDTO> costDistributionItemDTOs = new ArrayList<>();
                    if (!isFullUpdate){
                        List<CostDistributionItem> costDistributionItems = null;
                        if (invoice.getCostDistributionItems() == null || invoice.getCostDistributionItems().isEmpty()){
                            costDistributionItems = costDistributionItemRepository.findByInvoice(invoice);
                        }else{
                            costDistributionItems = invoice.getCostDistributionItems();
                        }
                        for (CostDistributionItem costDistributionItem : costDistributionItems) {
                            costDistributionItemDTOs.add(costDistributionItemService.mapToDTO(costDistributionItem, true));
                        }
                    }

                    invoiceDTO.setCostDistributionItemDTOs(costDistributionItemDTOs);
                }

                databaseChangeDTO.setRevisionType(revisionType);
                databaseChangeDTO.setMappedObject(invoiceDTO);

                allDataPatchDTO.getInvoiceDTOs().add(databaseChangeDTO);
            }else if (databaseChange.getClassType().equals(InvoiceCategory.class)){
                DatabaseChangeDTO<InvoiceCategoryDTO> databaseChangeDTO = new DatabaseChangeDTO<>();
                Integer revisionType = databaseChange.getRevisionType();

                InvoiceCategoryDTO resultInvoiceCategoryDTO = invoiceCategoryService.mapToDTO((InvoiceCategory) databaseChange.getObject(), currentUser);

                databaseChangeDTO.setRevisionType(revisionType);
                databaseChangeDTO.setMappedObject(resultInvoiceCategoryDTO);

                allDataPatchDTO.getInvoiceCategorieDTOs().add(databaseChangeDTO);
            }else if (databaseChange.getClassType().equals(InvoiceFailure.class)){
                DatabaseChangeDTO<InvoiceFailureDTO> databaseChangeDTO = new DatabaseChangeDTO<>();
                databaseChangeDTO.setRevisionType(databaseChange.getRevisionType());

                InvoiceFailure invoiceFailure = (InvoiceFailure) databaseChange.getObject();
                InvoiceFailureDTO invoiceFailureDTO = beanMapper.map(invoiceFailure, InvoiceFailureDTO.class);
                if (invoiceFailure.getInvoice() != null){
                    invoiceFailureDTO.setInvoiceId(invoiceFailure.getInvoice().getInvoiceId());
                }

                databaseChangeDTO.setMappedObject(invoiceFailureDTO);

                allDataPatchDTO.getInvoiceFailureDTOs().add(databaseChangeDTO);
            }else if (databaseChange.getClassType().equals(UserContact.class)){
                DatabaseChangeDTO<UserContactDTO> databaseChangeDTO = new DatabaseChangeDTO<>();
                Integer revisionType = databaseChange.getRevisionType();

                UserContact userContact = (UserContact) databaseChange.getObject();
                UserContactDTO userContactDTO = beanMapper.map(userContact, UserContactDTO.class);

                AppUser appUser = userContact.getAppUser();
                if (appUser != null){
                    userContactDTO.setAppUserId(appUser.getAppUserId());
                }

                AppUser appUserContact = userContact.getAppUserContact();
                if (appUserContact != null){
                    userContactDTO.setAppUserContactId(appUserContact.getAppUserId());
                }

                if (userContactDTO.getBasicStatusEnum() == null){
                    userContactDTO.setBasicStatusEnum(BasicStatusEnum.OK);
                    userContact.setBasicStatusEnum(BasicStatusEnum.OK);
                    userContactRepository.save(userContact);
                }

                databaseChangeDTO.setRevisionType(revisionType);
                databaseChangeDTO.setMappedObject(userContactDTO);

                allDataPatchDTO.getUserContactDTOs().add(databaseChangeDTO);
            }
        }

        return allDataPatchDTO;
    }

}
