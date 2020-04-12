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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.nextbill.domain.dtos.InvoiceCategoryDTO;
import de.nextbill.domain.enums.BasicStatusEnum;
import de.nextbill.domain.model.*;
import de.nextbill.domain.pojos.wikipedia.search.WikipediaQuery;
import de.nextbill.domain.pojos.wikipedia.search.WikipediaQueryRoot;
import de.nextbill.domain.pojos.wikipedia.search.WikipediaSearchItem;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.InvoiceCategoryKeywordRepository;
import de.nextbill.domain.repositories.InvoiceCategoryRepository;
import de.nextbill.domain.utils.BeanMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InvoiceCategoryService {

    @Autowired
    private InvoiceCategoryRepository invoiceCategoryRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ScansioService scansioService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private ComparisonService comparisonService;

    @Autowired
    private InvoiceCategoryKeywordRepository invoiceCategoryKeywordRepository;

    public InvoiceCategoryDTO mapToDTO(InvoiceCategory invoiceCategory){
        return mapToDTO(invoiceCategory, null);
    }

    public InvoiceCategoryDTO mapToDTO(InvoiceCategory invoiceCategory, AppUser currentUser){
        BeanMapper beanMapper = new BeanMapper();
        InvoiceCategoryDTO resultInvoiceCategoryDTO = beanMapper.map(invoiceCategory, InvoiceCategoryDTO.class);

        InvoiceCategory parentInvoiceCategory = invoiceCategory.getParentInvoiceCategory();
        if (parentInvoiceCategory != null){
            resultInvoiceCategoryDTO.setParentInvoiceCategoryDTO(mapToDTO(parentInvoiceCategory, currentUser));
        }

        if (invoiceCategory.getBasicStatusEnum() == null){
            invoiceCategory.setBasicStatusEnum(BasicStatusEnum.OK);
            invoiceCategoryRepository.save(invoiceCategory);
            resultInvoiceCategoryDTO.setBasicStatusEnum(BasicStatusEnum.OK);
        }

        AppUser appUser = invoiceCategory.getAppUser();
        if (appUser != null){
            resultInvoiceCategoryDTO.setAppUserId(appUser.getAppUserId());
        }

        if (currentUser == null){
            String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
            currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));
        }

        if (invoiceCategory.getAppUser() != null && currentUser != null && invoiceCategory.getAppUser().getAppUserId().equals(currentUser.getAppUserId())){
            resultInvoiceCategoryDTO.setCanBeDeleted(true);
        }else{
            resultInvoiceCategoryDTO.setCanBeDeleted(false);
        }

        return resultInvoiceCategoryDTO;
    }

    public InvoiceCategory mapToEntity(InvoiceCategoryDTO invoiceCategoryDTO){
        BeanMapper beanMapper = new BeanMapper();
        InvoiceCategory invoiceCategory = beanMapper.map(invoiceCategoryDTO, InvoiceCategory.class);

        InvoiceCategoryDTO parentInvoiceCategoryDTO = invoiceCategoryDTO.getParentInvoiceCategoryDTO();
        if (parentInvoiceCategoryDTO != null){
            InvoiceCategory parentInvoiceCategory = invoiceCategoryRepository.findById(parentInvoiceCategoryDTO.getInvoiceCategoryId()).orElse(null);
            invoiceCategory.setParentInvoiceCategory(parentInvoiceCategory);
            if (parentInvoiceCategory != null){
                invoiceCategory.setInvoiceCategoryType(parentInvoiceCategory.getInvoiceCategoryType());
            }
        }else{
            invoiceCategory.setParentInvoiceCategory(null);
        }

        UUID appUserId = invoiceCategoryDTO.getAppUserId();
        if (appUserId != null){
            AppUser appUser = appUserRepository.findById(appUserId).orElse(null);
            invoiceCategory.setAppUser(appUser);
        }

        return invoiceCategory;
    }

    public InvoiceCategory requestExternalServicesForInvoiceCategory(BusinessPartner businessPartner, Invoice invoice){
        return requestExternalServicesForInvoiceCategory(businessPartner.getBusinessPartnerReceiptName(), invoice);
    }

    public InvoiceCategory requestExternalServicesForInvoiceCategory(String businessPartnerName, Invoice invoice){

        try{

            log.debug("Search in Wikipedia for businss partner name: "+ businessPartnerName);

            URI uriWikipediaSearch = UriComponentsBuilder.fromHttpUrl("https://de.wikipedia.org/w/api.php")
                    .queryParam("action", "query")
                    .queryParam("format", "json")
                    .queryParam("list", "search")
                    .queryParam("utf8", "1")
//                    .queryParam("srqiprofile", "classic")
                    .queryParam("srlimit", "3")
                    .queryParam("srsearch", businessPartnerName + " Unternehmen")
                    .build().encode().toUri();

            ResponseEntity<WikipediaQueryRoot> wikipediaRequestRootEntity = new RestTemplate().getForEntity(uriWikipediaSearch, WikipediaQueryRoot.class);
            WikipediaQueryRoot wikipediaRequestRoot = wikipediaRequestRootEntity.getBody();
            WikipediaQuery wikipediaQuery = wikipediaRequestRoot.getQuery();
            List<WikipediaSearchItem> wikipediaSearchItems = wikipediaQuery.getSearch();

            boolean businessPartnerNameFound = false;

            String title = null;
            if (wikipediaSearchItems != null){

                for (WikipediaSearchItem wikipediaSearchItem : wikipediaSearchItems) {
                    String titleTmp = wikipediaSearchItem.getTitle();

                    if (titleTmp != null){
                        title = titleTmp;

                        List<Pattern> patternsOfBusinessPartner = comparisonService.convertStringToPatterns(businessPartnerName);

                        for (Pattern pattern : patternsOfBusinessPartner) {
                            Matcher m = pattern.matcher(title);

                            while (m.find()) {
                                String foundBusinessPartner = m.group(1);

                                Double similarity = comparisonService.similarityWithComparableStrings(businessPartnerName, foundBusinessPartner);
                                if (similarity >= 0.65) {
                                    businessPartnerNameFound = true;
                                    break;
                                }
                            }
                            if (businessPartnerNameFound){
                                break;
                            }
                        }
                    }

                    if (businessPartnerNameFound){
                        break;
                    }
                }

                if (!businessPartnerNameFound){

                    for (WikipediaSearchItem wikipediaSearchItem : wikipediaSearchItems) {
                        String snippet = wikipediaSearchItem.getSnippet();
                        snippet = Jsoup.parse(snippet).text();

                        if (snippet != null){
                            title = wikipediaSearchItem.getTitle();

                            List<Pattern> patternsOfBusinessPartner = comparisonService.convertStringToPatterns(businessPartnerName);

                            for (Pattern pattern : patternsOfBusinessPartner) {
                                Matcher m = pattern.matcher(snippet);

                                while (m.find()) {
                                    String foundBusinessPartner = m.group(1);

                                    Double similarity = comparisonService.similarityWithComparableStrings(businessPartnerName, foundBusinessPartner);
                                    if (similarity >= 0.75) {
                                        businessPartnerNameFound = true;
                                        break;
                                    }
                                }
                                if (businessPartnerNameFound){
                                    break;
                                }
                            }
                        }

                        if (businessPartnerNameFound){
                            break;
                        }
                    }
                }
            }

            if (businessPartnerNameFound){

                log.debug("Usage of Wikipedia title: "+ title);

                URI uriWikipediaSection = UriComponentsBuilder.fromHttpUrl("https://de.wikipedia.org/w/api.php")
                        .queryParam("action", "parse")
                        .queryParam("format", "json")
                        .queryParam("page", title)
                        .queryParam("prop", "text")
                        .queryParam("section", "0")
                        .queryParam("disablelimitreport", "true")
                        .build().encode().toUri();

                String responseSection = new RestTemplate().getForObject(uriWikipediaSection, String.class);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readValue(responseSection, JsonNode.class);
                String htmlText = title + " " + rootNode.get("parse").get("text").get("*").asText();

                InvoiceCategory invoiceCategory = analyzeCategoryForHtml(htmlText);

                if (invoiceCategory != null){
                    return invoiceCategory;
                }

            }

            log.debug("No Wikipedia entry found.");

            InvoiceCategory invoiceCategory = null;
            if (invoice != null && settingsService.getCurrentSettings().getScansioEnabled()){
                log.debug("Try to find category by analyzing invoice.");
                invoiceCategory = scansioService.sendDataOrImageToScansioForCategorySearch(invoice);
            }

            if (invoiceCategory != null) {
                return invoiceCategory;
            }

        }catch(RestClientException exception){
            log.debug("could not send text to Wikipedia API", exception);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        log.debug("No InvoiceCategory found!");

        return null;
    }

    public InvoiceCategory analyzeCategoryForHtml(String htmlText) {

        Map<String, List<String>> wordsMap = new HashMap<>();

        for (InvoiceCategory invoiceCategory : invoiceCategoryRepository.findAllByAppUserIsNullAndBasicStatusEnum(BasicStatusEnum.OK)) {
            List<String> keywords = invoiceCategoryKeywordRepository.findAllByInvoiceCategory(invoiceCategory).stream().map(InvoiceCategoryKeyword::getKeyword).collect(Collectors.toList());

            wordsMap.put(invoiceCategory.getInvoiceCategoryId().toString(), keywords);
        }

        Map<String, List<String>> stringEvaluationMap = new HashMap<>();

        String textWithoutHtml = Jsoup.parse(htmlText).text();

        textWithoutHtml = textWithoutHtml
                .replaceAll("([\\\\][n][\\s]|[\\s][\\\\][n]|[\\\\][n])+", "\n")
                .replaceAll("([\\\\][n])+", "\n")
                .replace(",", " ")
                .replace(";", " ")
                .replace(":", " ")
                .replace(".", " ")
                .replace("?", " ")
                .replace("!", " ");

        List<String> wordsList = new ArrayList<>();
        for (List<String> values : wordsMap.values()) {
            wordsList.addAll(values);
        }
        Map<String, List<Pattern>> chosenWords = comparisonService.generateVariantsOfPatterns(wordsList);

        String hit = textWithoutHtml;

        Set<SmartHit> smartHitsFound = new HashSet<>();

        for (String word : chosenWords.keySet()) {
            List<Pattern> patternsOfBusinessPartner = chosenWords.get(word);
            for (Pattern pattern : patternsOfBusinessPartner) {
                Matcher m = pattern.matcher(hit);

                while (m.find()) {
                    String foundKeyword = m.group(1);

                    Double similarity = comparisonService.similarityWithComparableStrings(word, foundKeyword);
                    if (similarity > 0.85) {

                        SmartHit smartHit = new SmartHit(m.start(), m.end());
                        if (smartHitsFound.contains(smartHit)) {
                            continue;
                        }

                        String resultKey = null;
                        for (Map.Entry<String, List<String>> wordsEntry : wordsMap.entrySet()) {
                            boolean abortLoop = false;
                            for (String innerWord : wordsEntry.getValue()) {
                                if (innerWord.equals(word)){
                                    resultKey = wordsEntry.getKey();
                                    abortLoop = true;
                                    break;
                                }
                            }
                            if (abortLoop){
                                break;
                            }
                        }



                        smartHitsFound.add(smartHit);
                        if (stringEvaluationMap.get(resultKey) == null){
                            List<String> innerFoundWordsList = new ArrayList<>();
                            innerFoundWordsList.add(foundKeyword);

                            stringEvaluationMap.put(resultKey, innerFoundWordsList);
                        }else{
                            stringEvaluationMap.get(resultKey).add(foundKeyword);
                        }
                    }
                }
            }
        }

        Integer counterHits = -1;
        String bestOcrPositionEvaluationResult = null;
        String resultKey = null;
        if (!stringEvaluationMap.isEmpty()) {
            for (Map.Entry<String, List<String>> keyHitsEntry : stringEvaluationMap.entrySet()) {
                Integer sizeHits = keyHitsEntry.getValue().size();

                if (sizeHits > counterHits){
                    counterHits = sizeHits;
                    resultKey = keyHitsEntry.getKey();
                    bestOcrPositionEvaluationResult = keyHitsEntry.getValue().get(0);
                }
            }
        }

        if (bestOcrPositionEvaluationResult != null) {
            return invoiceCategoryRepository.findById(UUID.fromString(resultKey)).orElse(null);
        }

        return null;
    }

    @Data
    @EqualsAndHashCode
    class SmartHit{

        private Integer start;
        private Integer end;

        SmartHit(Integer start, Integer end){
            this.start = start;
            this.end = end;
        }
    }
}
