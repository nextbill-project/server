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

import de.nextbill.domain.enums.*;
import de.nextbill.domain.interfaces.PaymentItem;
import de.nextbill.domain.model.Billing;
import de.nextbill.domain.model.CostDistributionItem;
import de.nextbill.domain.model.Invoice;
import de.nextbill.domain.model.StandingOrder;
import de.nextbill.domain.repositories.CostDistributionItemRepositoryCustomMethods;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class CostDistributionItemRepositoryImpl implements CostDistributionItemRepositoryCustomMethods {

    @PersistenceContext
    EntityManager em;

    @Override
    public List<PaymentItem> findCostDistributionItems(MainFunctionEnum mainFunctionEnum, UUID invoicePayerId, PaymentPersonTypeEnum invoicePaymentPersonTypeEnum, UUID costPayerId, PaymentPersonTypeEnum costPaymentPersonTypeEnum, Date startDate, Date endDate, InvoiceStatusEnum invoiceStatusEnum, CorrectionStatus correctionStatus, boolean paidAndNotPaid) {

        boolean onlyPaid = !paidAndNotPaid;

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<CostDistributionItem> criteriaQuery = builder.createQuery(CostDistributionItem.class);

        Root<CostDistributionItem> costDistributionItemRoot = criteriaQuery.from(CostDistributionItem.class);
        criteriaQuery.select(costDistributionItemRoot);

        Predicate costDistributionPayerIsPred = builder.equal(costDistributionItemRoot.get("payerId"), costPayerId);
        Predicate andPredicates = builder.and(costDistributionPayerIsPred);
        Predicate costDistributionPayerTypeIsPred = builder.equal(costDistributionItemRoot.get("paymentPersonTypeEnum"), costPaymentPersonTypeEnum);
        andPredicates = builder.and(andPredicates, costDistributionPayerTypeIsPred);

        if (mainFunctionEnum.equals(MainFunctionEnum.EXPENSE)){
            Predicate invoicePayerIsPred = builder.equal(costDistributionItemRoot.join("invoice").get("payerId"), invoicePayerId);
            andPredicates = builder.and(andPredicates, invoicePayerIsPred);
            Predicate invoicePayerTypeIsPred = builder.equal(costDistributionItemRoot.join("invoice").get("payerTypeEnum"), invoicePaymentPersonTypeEnum);
            andPredicates = builder.and(andPredicates, invoicePayerTypeIsPred);
        }else{
            Predicate invoicePayerIsPred = builder.equal(costDistributionItemRoot.join("invoice").get("paymentRecipientId"), invoicePayerId);
            andPredicates = builder.and(andPredicates, invoicePayerIsPred);
            Predicate invoicePayerTypeIsPred = builder.equal(costDistributionItemRoot.join("invoice").get("paymentRecipientTypeEnum"), invoicePaymentPersonTypeEnum);
            andPredicates = builder.and(andPredicates, invoicePayerTypeIsPred);

            Predicate payerIdAndPaymentRecipientIdEqualPred = builder.equal(costDistributionItemRoot.join("invoice").get("payerId"), costDistributionItemRoot.join("invoice").get("paymentRecipientId"));
            Predicate createdByPred = builder.equal(costDistributionItemRoot.join("invoice").get("createdBy"), invoicePayerId.toString());
            Predicate isExpensePred = builder.not(builder.and(createdByPred, payerIdAndPaymentRecipientIdEqualPred));
            andPredicates = builder.and(andPredicates, isExpensePred);
        }

        Predicate invoiceStatusEnumPred = builder.equal(costDistributionItemRoot.join("invoice").get("invoiceStatusEnum"), invoiceStatusEnum);
        andPredicates = builder.and(andPredicates, invoiceStatusEnumPred);

        if (startDate != null && endDate != null){
            Predicate dateBetweenPred = builder.between(costDistributionItemRoot.join("invoice").get("dateOfInvoice"), startDate, endDate);
            andPredicates = builder.and(andPredicates, dateBetweenPred);
        }

        Predicate invoiceNotNullPred = builder.isNotNull(costDistributionItemRoot.get("invoice"));
        andPredicates = builder.and(andPredicates, invoiceNotNullPred);

        Subquery<UUID> sq = criteriaQuery.subquery(UUID.class);
        Root<StandingOrder> standingOrderRoot = sq.from(StandingOrder.class);
        Join<StandingOrder, Invoice> sqEmp = standingOrderRoot.join("invoiceTemplate");
        sq.select(sqEmp.get("invoiceId"));
        Predicate notStandingOrderPredicate = builder.not(builder.in(costDistributionItemRoot.join("invoice").get("invoiceId")).value(sq));
        andPredicates = builder.and(andPredicates, notStandingOrderPredicate);

        if (onlyPaid){
            Predicate onlyNotPaidPred = builder.equal(costDistributionItemRoot.get("costPaid"), 0);
            Predicate costPaidNotNullPred = builder.isNull(costDistributionItemRoot.get("costPaid"));
            Predicate orPredicate = builder.or(onlyNotPaidPred, costPaidNotNullPred);

            Predicate costPaidCorrectionStatusPred = null;
            if (CorrectionStatus.CHECK.equals(correctionStatus)){
                Predicate isCheckStatusPred = builder.equal(costDistributionItemRoot.get("correctionStatus"), correctionStatus);
                Predicate isNullStatusPred = builder.isNull(costDistributionItemRoot.get("correctionStatus"));
                costPaidCorrectionStatusPred = builder.or(isCheckStatusPred, isNullStatusPred);
            }else{
                costPaidCorrectionStatusPred = builder.equal(costDistributionItemRoot.get("correctionStatus"), correctionStatus);
            }

            Subquery<UUID> sq1 = criteriaQuery.subquery(UUID.class);
            Root<CostDistributionItem> costDistributionItemRoot1 = sq1.from(CostDistributionItem.class);
            Join<CostDistributionItem, Billing> sqEmp1 = costDistributionItemRoot1.join("billings");
            sq1.select(costDistributionItemRoot1.get("costDistributionItemId"));
            sq1.where(builder.not(builder.equal(sqEmp1.get("billingStatusEnum"), BillingStatusEnum.ABORTED)));
            Predicate notInBillingPredicate = builder.not(builder.in(costDistributionItemRoot.get("costDistributionItemId")).value(sq1));
            andPredicates = builder.and(andPredicates, notInBillingPredicate);

            Predicate andPredicateTmp = builder.and(orPredicate, costPaidCorrectionStatusPred);
            andPredicates = builder.and(andPredicates, andPredicateTmp);
        }

        criteriaQuery.where(andPredicates);
        criteriaQuery.orderBy(builder.desc (costDistributionItemRoot.join("invoice").get("dateOfInvoice")));

        List<PaymentItem> paymentItems = new ArrayList<>();
        paymentItems.addAll(em.createQuery(criteriaQuery).getResultList());

        return paymentItems;
    }

    @Override
    public List<PaymentItem> findDirectTransactionsCostDistributionItems(MainFunctionEnum mainFunctionEnum, UUID invoicePayerId, PaymentPersonTypeEnum invoicePaymentPersonTypeEnum, UUID costPayerId, PaymentPersonTypeEnum costPaymentPersonTypeEnum, Date startDate, Date endDate, InvoiceStatusEnum invoiceStatusEnum, CorrectionStatus correctionStatus, boolean paidAndNotPaid) {
        boolean onlyPaid = !paidAndNotPaid;

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Invoice> criteriaQuery = builder.createQuery(Invoice.class);

        Root<Invoice> invoiceRoot = criteriaQuery.from(Invoice.class);
        criteriaQuery.select(invoiceRoot);

        Predicate invoiceStatusEnumPred = builder.equal(invoiceRoot.get("invoiceStatusEnum"), invoiceStatusEnum);
        Predicate andPredicates = builder.and(invoiceStatusEnumPred);

        if (mainFunctionEnum.equals(MainFunctionEnum.EXPENSE)){
            Predicate invoicePaymentRecipientIsPred = builder.equal(invoiceRoot.get("paymentRecipientId"), costPayerId);
            andPredicates = builder.and(andPredicates, invoicePaymentRecipientIsPred);
            Predicate invoicePaymentRecipientTypeIsPred = builder.equal(invoiceRoot.get("paymentRecipientTypeEnum"), costPaymentPersonTypeEnum);
            andPredicates = builder.and(andPredicates, invoicePaymentRecipientTypeIsPred);

            Predicate invoicePayerIsPred = builder.equal(invoiceRoot.get("payerId"), invoicePayerId);
            andPredicates = builder.and(andPredicates, invoicePayerIsPred);
            Predicate invoicePayerTypeIsPred = builder.equal(invoiceRoot.get("payerTypeEnum"), invoicePaymentPersonTypeEnum);
            andPredicates = builder.and(andPredicates, invoicePayerTypeIsPred);
        }else{
            Predicate invoicePayerIsPred = builder.equal(invoiceRoot.get("payerId"), costPayerId);
            andPredicates = builder.and(andPredicates, invoicePayerIsPred);
            Predicate invoicePayerTypeIsPred = builder.equal(invoiceRoot.get("payerTypeEnum"), costPaymentPersonTypeEnum);
            andPredicates = builder.and(andPredicates, invoicePayerTypeIsPred);

            Predicate invoicePaymentRecipientIsPred = builder.equal(invoiceRoot.get("paymentRecipientId"), invoicePayerId);
            andPredicates = builder.and(andPredicates, invoicePaymentRecipientIsPred);
            Predicate invoicePaymentRecipientTypeIsPred = builder.equal(invoiceRoot.get("paymentRecipientTypeEnum"), invoicePaymentPersonTypeEnum);
            andPredicates = builder.and(andPredicates, invoicePaymentRecipientTypeIsPred);
        }

        if (onlyPaid){
            Predicate onlyNotPaidPred = builder.equal(invoiceRoot.get("costPaid"), 0);
            Predicate costPaidNotNullPred = builder.isNull(invoiceRoot.get("costPaid"));
            Predicate orPredicate = builder.or(onlyNotPaidPred, costPaidNotNullPred);

            Predicate costPaidCorrectionStatusPred = null;
            if (CorrectionStatus.CHECK.equals(correctionStatus)){
                Predicate isCheckStatusPred = builder.equal(invoiceRoot.get("correctionStatus"), correctionStatus);
                Predicate isNullStatusPred = builder.isNull(invoiceRoot.get("correctionStatus"));
                costPaidCorrectionStatusPred = builder.or(isCheckStatusPred, isNullStatusPred);
            }else{
                costPaidCorrectionStatusPred = builder.equal(invoiceRoot.get("correctionStatus"), correctionStatus);
            }

            Subquery<UUID> sq1 = criteriaQuery.subquery(UUID.class);
            Root<Invoice> invoiceRoot1 = sq1.from(Invoice.class);
            Join<Invoice, Billing> sqEmp1 = invoiceRoot1.join("billings");
            sq1.select(invoiceRoot1.get("invoiceId"));
            sq1.where(builder.not(builder.equal(sqEmp1.get("billingStatusEnum"), BillingStatusEnum.ABORTED)));
            Predicate notInBillingPredicate = builder.not(builder.in(invoiceRoot.get("invoiceId")).value(sq1));
            andPredicates = builder.and(andPredicates, notInBillingPredicate);

            Predicate andPredicateTmp = builder.and(orPredicate, costPaidCorrectionStatusPred);
            andPredicates = builder.and(andPredicates, andPredicateTmp);
        }

        if (startDate != null && endDate != null){
            Predicate dateBetweenPred = builder.between(invoiceRoot.get("dateOfInvoice"), startDate, endDate);
            andPredicates = builder.and(andPredicates, dateBetweenPred);
        }

        Subquery<UUID> sq = criteriaQuery.subquery(UUID.class);
        Root<StandingOrder> standingOrderRoot = sq.from(StandingOrder.class);
        Join<StandingOrder, Invoice> sqEmp = standingOrderRoot.join("invoiceTemplate");
        sq.select(sqEmp.get("invoiceId"));
        Predicate notStandingOrderPredicate = builder.not(builder.in(invoiceRoot.get("invoiceId")).value(sq));
        andPredicates = builder.and(andPredicates, notStandingOrderPredicate);

        criteriaQuery.where(andPredicates);
        criteriaQuery.orderBy(builder.desc (invoiceRoot.get("dateOfInvoice")));

        List<PaymentItem> paymentItems = new ArrayList<>();
        paymentItems.addAll(em.createQuery(criteriaQuery).getResultList());

        return paymentItems;
    }
}
