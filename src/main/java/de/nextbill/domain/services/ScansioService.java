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

import com.drew.imaging.ImageProcessingException;
import de.nextbill.domain.dtos.ArticleDTO;
import de.nextbill.domain.enums.BasicStatusEnum;
import de.nextbill.domain.enums.InvoiceFailureTypeEnum;
import de.nextbill.domain.enums.InvoiceStatusEnum;
import de.nextbill.domain.enums.PaymentPersonTypeEnum;
import de.nextbill.domain.model.*;
import de.nextbill.domain.pojos.scansio.*;
import de.nextbill.domain.pojos.scansio.enums.DataType;
import de.nextbill.domain.pojos.scansio.enums.ItemWorkflowStatus;
import de.nextbill.domain.pojos.scansio.enums.SioContentType;
import de.nextbill.domain.pojos.scansio.resultData.ExternalOcrResultData;
import de.nextbill.domain.pojos.scansio.resultData.ExternalOcrResultDataItem;
import de.nextbill.domain.pojos.scansio.resultData.OcrHit;
import de.nextbill.domain.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class ScansioService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ImageConversionService imageConversionService;

    @Autowired
    private InvoiceCategoryRepository invoiceCategoryRepository;

    @Autowired
    private InvoiceCategoryKeywordRepository invoiceCategoryKeywordRepository;

    @Value("${external.service.scansio.url}")
    private String scanioUrl;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PathService pathService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private BusinessPartnerService businessPartnerService;

    @Autowired
    private AutoFillHelperService autoFillHelperService;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private InvoiceFailureRepository invoiceFailureRepository;

    @Autowired
    private SettingsService settingsService;

    public RecognitionResponseV1 sendTextToScansio(Invoice invoice, String html) {

        URI uri = UriComponentsBuilder.fromHttpUrl(scanioUrl + "text")
                .queryParam("id", invoice.getInvoiceId().toString())
                .queryParam("createImage", true)
                .queryParam("async", false)
                .build().encode().toUri();

        SioTextRequest sioTextRequest = new SioTextRequest();
        sioTextRequest.setConfigurationName("invoiceMic");
        sioTextRequest.setText(html);

        try{
            RestTemplate restTemplate = createRestTemplate();
            HttpEntity<SioTextRequest> request = new HttpEntity<>(sioTextRequest, createOAuth2Headers());
            ResponseEntity<RecognitionResponseV1> responseEntity = restTemplate.postForEntity(uri, request, RecognitionResponseV1.class);
            return responseEntity.getBody();
        }catch(RestClientException exception){
            resetInvoiceToError(invoice);
            logger.debug("could not send image to scansio", exception);
        }catch(Exception exception){
            logger.debug("could not send image to scansio", exception);
        }

        return null;
    }

    public RecognitionResponseV1 sendHtmlToScansioForInvoice(Invoice invoice, String html) throws IOException {

        SioHtmlRequest sioHtmlRequest = new SioHtmlRequest();
        sioHtmlRequest.setConfigurationName("invoice");
        sioHtmlRequest.setHtml(html);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(scanioUrl + "html")
                .queryParam("id", invoice.getInvoiceId().toString())
                .queryParam("createImage", true);

        return sendHtmlToScansio(sioHtmlRequest, uriBuilder, false);
    }

    public InvoiceCategory sendDataOrImageToScansioForCategorySearch(Invoice invoice) throws IOException {

        Map<String, List<String>> wordsMap = new HashMap<>();

        for (InvoiceCategory invoiceCategory : invoiceCategoryRepository.findAllByAppUserIsNullAndBasicStatusEnum(BasicStatusEnum.OK)) {
            List<String> keywords = invoiceCategoryKeywordRepository.findAllByInvoiceCategory(invoiceCategory).stream().map(InvoiceCategoryKeyword::getKeyword).collect(Collectors.toList());

            wordsMap.put(invoiceCategory.getInvoiceCategoryId().toString(), keywords);
        }

        ExternalTextSelectionParsingConfiguration externalTextSelectionParsingConfiguration = new ExternalTextSelectionParsingConfiguration();
        externalTextSelectionParsingConfiguration.setWordsMap(wordsMap);

        ExternalRecognitionItem externalRecognitionItem = new ExternalRecognitionItem();
        externalRecognitionItem.setDataType(DataType.TEXT_SELECTION);
        externalRecognitionItem.setId(UUID.randomUUID().toString());
        externalRecognitionItem.setTextSelectionConfiguration(externalTextSelectionParsingConfiguration);

        ExternalScanConfiguration externalScanConfiguration = new ExternalScanConfiguration();
        externalScanConfiguration.setItems(Arrays.asList(externalRecognitionItem));

        RecognitionResponseV1 response = sendDataOrImageToScansioForCategory(invoice, externalScanConfiguration);

        if (response != null && !response.getItemResults().isEmpty()){
            RecognitionItemResponseV1 category = response.getItemResults().get(0);
            if (ItemWorkflowStatus.HIT_FOUND.equals(category.getItemWorkflowStatus()) && category.getResultValue() != null){
                UUID categoryId = UUID.fromString((String) category.getResultValue());

                InvoiceCategory invoiceCategory = invoiceCategoryRepository.findById(categoryId).orElse(null);
                log.info("InvoiceCategory found from Invoice Image: " + invoiceCategory.getInvoiceCategoryName());

                return invoiceCategory;
            }
        }

        return null;
    }

    public RecognitionResponseV1 sendHtmlToScansio(SioHtmlRequest sioHtmlRequest, UriComponentsBuilder uriBuilder, boolean async) throws IOException {

        if (!async){
            uriBuilder = uriBuilder.queryParam("async", "false");
        }

        ResponseEntity<RecognitionResponseV1> response = null;
        URI uri = uriBuilder.build().encode().toUri();
        try{
            RestTemplate restTemplate = createRestTemplate();
            HttpEntity<SioHtmlRequest> request = new HttpEntity<>(sioHtmlRequest, createOAuth2Headers());
            response = restTemplate.postForEntity(uri, request, RecognitionResponseV1.class);
            return response.getBody();
        }catch(RestClientException exception){
            logger.debug("could not send image to scansio", exception);
            throw exception;
        }catch(Exception exception){
            logger.debug("could not send image to scansio", exception);
        }

        return null;
    }

    public RecognitionResponseV1 sendImageForInvoiceToScansio(Invoice invoice, boolean rotate) throws IOException {
        return sendImageToScansio(invoice, rotate, RecognitionResponseV1.class, "invoice", false);
    }

    private <T> T sendImageToScansio(Invoice invoice, boolean rotate, Class<T> responseClass, String configurationName, boolean async) throws IOException {

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(scanioUrl + "image/body")
                .queryParam("id", invoice.getInvoiceId().toString())
                .queryParam("rotate", rotate)
                .queryParam("returnDataResults", true)
                .queryParam("configurationName", configurationName);

        if (!async){
            uriComponentsBuilder = uriComponentsBuilder.queryParam("async", false);
        }

        URI uri = uriComponentsBuilder.build().encode().toUri();

        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.add("Authorization", "Bearer " + settingsService.getCurrentSettings().getScansioAccessToken() );
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(imageConversionService.fileForOriginal(invoice)));
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            RestTemplate restTemplate = createRestTemplate();

            ResponseEntity<T> response = restTemplate.postForEntity(uri, requestEntity, responseClass);
            return response.getBody();
        }catch(Exception exception){
            logger.debug("could not send image to scansio", exception);

            resetInvoiceToError(invoice);
        }

        return null;
    }

    private void resetInvoiceToError(Invoice invoice) {
        InvoiceFailure invoiceFailure = new InvoiceFailure();
        invoiceFailure.setInvoice(invoice);
        invoiceFailure.setInvoiceFailureId(UUID.randomUUID());
        invoiceFailure.setMessage("Fehler bei der OCR-Analyse! Bitte manuell eingeben.");
        invoiceFailure.setInvoiceFailureTypeEnum(InvoiceFailureTypeEnum.OCR_ERROR);
        invoiceFailureRepository.save(invoiceFailure);

        invoice.setInvoiceStatusEnum(InvoiceStatusEnum.CHECK);
        invoice.setSumOfInvoice(BigDecimal.ZERO);
        invoice.setDateOfInvoice(new Date());
        invoiceRepository.save(invoice);
    }

    public RecognitionResponseV1 sendDataOrImageToScansioForCategory(Invoice invoice, ExternalScanConfiguration configuration) throws IOException {

        try{
            RecognitionResponseV1 sioCategoryResponse = null;

            RestTemplate restTemplate = createRestTemplate();

            if (invoice.getScansioResultData() == null){
                File file = imageConversionService.fileForOriginal(invoice);
                if (file == null){
                    return null;
                }

                SioFileRequest sioFileRequest = new SioFileRequest();
                sioFileRequest.setFile(new String(Base64.encodeBase64(Files.readAllBytes(file.toPath())), StandardCharsets.UTF_8));
                sioFileRequest.setConfiguration(configuration);

                URI uri = UriComponentsBuilder.fromHttpUrl(scanioUrl + "image/json")
                        .queryParam("id", invoice.getInvoiceId().toString())
                        .queryParam("async", false)
                        .queryParam("returnDataResults", true)
                        .build().encode().toUri();

                HttpEntity<SioFileRequest> request = new HttpEntity<>(sioFileRequest, createOAuth2Headers());
                ResponseEntity<RecognitionResponseV1> sioCategoryResponseEntity = restTemplate.postForEntity(uri, request, RecognitionResponseV1.class);
                sioCategoryResponse = sioCategoryResponseEntity.getBody();
            }else {
                SioDataRequest sioDataRequest = new SioDataRequest();
                sioDataRequest.setConfiguration(configuration);
                sioDataRequest.setResultData(invoice.getScansioResultData());

                URI uri = UriComponentsBuilder.fromHttpUrl(scanioUrl + "data")
                        .queryParam("id", invoice.getInvoiceId().toString())
                        .queryParam("async", false)
                        .build().encode().toUri();

                HttpEntity<SioDataRequest> request = new HttpEntity<>(sioDataRequest, createOAuth2Headers());
                ResponseEntity<RecognitionResponseV1> sioCategoryResponseEntity = restTemplate.postForEntity(uri, request, RecognitionResponseV1.class);
                sioCategoryResponse = sioCategoryResponseEntity.getBody();
            }

            if (sioCategoryResponse == null){
                return null;
            }

            if ( sioCategoryResponse.getResultData() != null){
                invoice.setScansioResultData(sioCategoryResponse.getResultData());
                invoice.setOcrFullText(extractFullTextromResultData(sioCategoryResponse.getResultData()));
                invoiceRepository.save(invoice);
            }

            return sioCategoryResponse;

        }catch(RestClientException exception){
            logger.debug("could not send image to scansio", exception);

            resetInvoiceToError(invoice);
        }catch(Exception exception){
            logger.debug("could not send image to scansio", exception);
        }

        return null;
    }

    public RestTemplate createRestTemplate() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                    .loadTrustMaterial(null, acceptingTrustStrategy)
                    .build();

        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(csf)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory();

        requestFactory.setHttpClient(httpClient);

        return new RestTemplate(requestFactory);
    }

    private MultiValueMap<String, String> createOAuth2Headers() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", "Bearer " + settingsService.getCurrentSettings().getScansioAccessToken() );
        headers.add("Content-Type", "application/json");

        return headers;
    }

    public ArticleDTO sendImageToScansioForArticle(Invoice invoice, BigDecimal xPercent, BigDecimal yPercent) throws IOException {

        Map<String, String> changes = new HashMap<>();
        changes.put("ARTICLE.textConfiguration.allowedXPositions.startRange", xPercent.toString());
        changes.put("ARTICLE.textConfiguration.allowedXPositions.endRange", xPercent.toString());
        changes.put("ARTICLE.textConfiguration.allowedYPositions.startRange", yPercent.toString());
        changes.put("ARTICLE.textConfiguration.allowedYPositions.endRange", yPercent.toString());

        changes.put("ARTICLE_DEPENDENCY.textConfiguration.allowedXPositions.startRange", xPercent.toString());
        changes.put("ARTICLE_DEPENDENCY.textConfiguration.allowedXPositions.endRange", xPercent.toString());
        changes.put("ARTICLE_DEPENDENCY.textConfiguration.allowedYPositions.startRange", yPercent.toString());
        changes.put("ARTICLE_DEPENDENCY.textConfiguration.allowedYPositions.endRange", yPercent.toString());

        changes.put("ARTICLE_ALTERNATIVE_DEPENDENCY.textConfiguration.allowedXPositions.startRange", xPercent.toString());
        changes.put("ARTICLE_ALTERNATIVE_DEPENDENCY.textConfiguration.allowedXPositions.endRange", xPercent.toString());
        changes.put("ARTICLE_ALTERNATIVE_DEPENDENCY.textConfiguration.allowedYPositions.startRange", yPercent.toString());
        changes.put("ARTICLE_ALTERNATIVE_DEPENDENCY.textConfiguration.allowedYPositions.endRange", yPercent.toString());

        ArticleDTO articleDTO = new ArticleDTO();

        try{
            RecognitionResponseV1 sioInvoiceLineResponse = null;

            RestTemplate restTemplate = createRestTemplate();

            if (invoice.getScansioResultData() == null){
                File file = imageConversionService.fileForOriginal(invoice);
                if (file == null){
                    return null;
                }

                SioFileRequest sioFileRequest = new SioFileRequest();
                sioFileRequest.setConfigurationName("article");
                sioFileRequest.setFile(new String(Base64.encodeBase64(Files.readAllBytes(file.toPath())), StandardCharsets.UTF_8));
                sioFileRequest.setChanges(changes);

                URI uri = UriComponentsBuilder.fromHttpUrl(scanioUrl + "image/json")
                        .queryParam("id", invoice.getInvoiceId().toString())
                        .queryParam("async", false)
                        .queryParam("returnDataResults", true)
                        .build().encode().toUri();

                HttpEntity<SioFileRequest> request = new HttpEntity<>(sioFileRequest, createOAuth2Headers());
                ResponseEntity<RecognitionResponseV1> sioInvoiceLineResponseEntity = restTemplate.postForEntity(uri, request, RecognitionResponseV1.class);
                sioInvoiceLineResponse = sioInvoiceLineResponseEntity.getBody();
            }else {
                SioDataRequest sioDataRequest = new SioDataRequest();
                sioDataRequest.setConfigurationName("article");
                sioDataRequest.setResultData(invoice.getScansioResultData());
                sioDataRequest.setChanges(changes);

                URI uri = UriComponentsBuilder.fromHttpUrl(scanioUrl + "data")
                        .queryParam("id", invoice.getInvoiceId().toString())
                        .queryParam("async", false)
                        .build().encode().toUri();

                HttpEntity<SioDataRequest> request = new HttpEntity<>(sioDataRequest, createOAuth2Headers());
                ResponseEntity<RecognitionResponseV1> sioInvoiceLineResponseEntity = restTemplate.postForEntity(uri, request, RecognitionResponseV1.class);
                sioInvoiceLineResponse = sioInvoiceLineResponseEntity.getBody();
            }

            if (sioInvoiceLineResponse == null){
                return null;
            }

            if ( sioInvoiceLineResponse.getResultData() != null){
                invoice.setScansioResultData(sioInvoiceLineResponse.getResultData());
                invoice.setOcrFullText(extractFullTextromResultData(sioInvoiceLineResponse.getResultData()));
                invoiceRepository.save(invoice);
            }

            List<RecognitionItemResponseV1> itemsForRectangle = new ArrayList<>();

            Optional<RecognitionItemResponseV1> article = sioInvoiceLineResponse.getItemResults().stream().filter(t -> t.getIdentificationCode().equals("ARTICLE")).findFirst();
            if (article.isPresent() && ItemWorkflowStatus.HIT_FOUND.equals(article.get().getItemWorkflowStatus())){
                articleDTO.setName((String) article.get().getResultValue());
                itemsForRectangle.add(article.get());
            }else{
                return null;
            }

            Optional<RecognitionItemResponseV1> sumValue = sioInvoiceLineResponse.getItemResults().stream().filter(t -> t.getIdentificationCode().equals("SUM_VALUE")).findFirst();
            Optional<RecognitionItemResponseV1> sumValueAlternative = sioInvoiceLineResponse.getItemResults().stream().filter(t -> t.getIdentificationCode().equals("SUM_VALUE_ALTERNATIVE")).findFirst();
            if (sumValue.isPresent() && ItemWorkflowStatus.HIT_FOUND.equals(sumValue.get().getItemWorkflowStatus())) {
                articleDTO.setPrice(new BigDecimal(sumValue.get().getResultValue() instanceof Double ? (Double) sumValue.get().getResultValue() : (Long) sumValue.get().getResultValue()).setScale(2, RoundingMode.HALF_EVEN));
                itemsForRectangle.add(sumValue.get());
            }else if (sumValueAlternative.isPresent() && ItemWorkflowStatus.HIT_FOUND.equals(sumValueAlternative.get().getItemWorkflowStatus())){
                articleDTO.setPrice(new BigDecimal(sumValueAlternative.get().getResultValue() instanceof Double ? (Double) sumValueAlternative.get().getResultValue() : (Long) sumValueAlternative.get().getResultValue()).setScale(2, RoundingMode.HALF_EVEN));
                itemsForRectangle.add(sumValueAlternative.get());
            }

            createOcrRectangleFromItemList(articleDTO, itemsForRectangle);

        }catch(RestClientException exception){
            logger.debug("could not send image to scansio", exception);
            throw exception;
        }catch(Exception exception){
            logger.debug("could not send image to scansio", exception);
        }

        return articleDTO;
    }

    private String extractFullTextromResultData(String base64Zip) {

        try {

            byte[] decodedZip = java.util.Base64.getDecoder().decode(base64Zip);

            ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(decodedZip));

            ByteArrayOutputStream bos = null;

            while (true) {

                if (zis.getNextEntry() == null) break;

                int size;
                byte[] buffer = new byte[2048];

                bos = new ByteArrayOutputStream(buffer.length);

                while ((size = zis.read(buffer, 0, buffer.length)) != -1) {
                    bos.write(buffer, 0, size);
                }
                bos.flush();
                bos.close();
            }
            zis.close();

            String resultJson = new String(bos.toByteArray());

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            ExternalOcrResultData externalOcrResultData = objectMapper.readValue(resultJson, ExternalOcrResultData.class);

            ExternalOcrResultDataItem externalOcrResultDataItem = externalOcrResultData.getScanResponseItems() != null ? externalOcrResultData.getScanResponseItems().get(0) : null;
            if (externalOcrResultDataItem == null) return null;

            List<String> values = externalOcrResultDataItem.getOcrHits().stream().map(OcrHit::getValue).collect(Collectors.toList());

            return StringUtils.join(values, " ");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void createOcrRectangleFromItemList(ArticleDTO articleDTO, List<RecognitionItemResponseV1> items){

        BigDecimal lowestStartX = new BigDecimal(Long.MAX_VALUE);
        BigDecimal lowestStartY = new BigDecimal(Long.MAX_VALUE);
        BigDecimal highestEndY = new BigDecimal(0);
        BigDecimal highestEndX = new BigDecimal(0);

        for (RecognitionItemResponseV1 item : items) {
            if (item.getStartX().compareTo(lowestStartX) <= 0){
                lowestStartX = item.getStartX();
            }
            if (item.getStartY().compareTo(lowestStartY) <= 0){
                lowestStartY = item.getStartY();
            }
            if (item.getEndX().compareTo(highestEndX) >= 0){
                highestEndX = item.getEndX();
            }
            if (item.getEndY().compareTo(highestEndY) >= 0){
                highestEndY = item.getEndY();
            }
        }

        articleDTO.setStartX(lowestStartX);
        articleDTO.setEndX(highestEndX);
        articleDTO.setStartY(lowestStartY);
        articleDTO.setEndY(highestEndY);
    }

    public void prepareArticleAnalysis(Invoice invoice) throws IOException {

        try{
            RestTemplate restTemplate = createRestTemplate();

            if (invoice.getScansioResultData() == null){
                File file = imageConversionService.fileForOriginal(invoice);
                if (file == null){
                    return;
                }

                SioFileRequest sioFileRequest = new SioFileRequest();
                sioFileRequest.setFile(new String(Base64.encodeBase64(Files.readAllBytes(file.toPath())), StandardCharsets.UTF_8));

                URI uri = UriComponentsBuilder.fromHttpUrl(scanioUrl + "image/json")
                        .queryParam("id", invoice.getInvoiceId().toString())
                        .queryParam("async", false)
                        .queryParam("returnDataResults", true)
                        .build().encode().toUri();

                HttpEntity<SioFileRequest> request = new HttpEntity<>(sioFileRequest, createOAuth2Headers());
                ResponseEntity<RecognitionResponseV1> recognitionResponse = restTemplate.postForEntity(uri, request, RecognitionResponseV1.class);
                RecognitionResponseV1 recognitionItemResponseV1 = recognitionResponse.getBody();

                if ( recognitionItemResponseV1.getResultData() != null){
                    invoice.setScansioResultData(recognitionItemResponseV1.getResultData());
                    invoice.setOcrFullText(extractFullTextromResultData(recognitionItemResponseV1.getResultData()));
                    invoiceRepository.save(invoice);
                }
            }

        }catch(RestClientException exception){
            logger.debug("could not send image to scansio", exception);
            throw exception;
        }catch(Exception exception){
            logger.debug("could not send image to scansio", exception);
        }
    }

    public Invoice readScansioResponse(RecognitionResponseV1 sioInvoiceResponse, Invoice foundInvoice) throws IOException, ImageProcessingException {

        if (sioInvoiceResponse == null) {
            return foundInvoice;
        }

        if (InvoiceStatusEnum.DELETED.equals(foundInvoice.getInvoiceStatusEnum())){
            return foundInvoice;
        }

        SioContentType sioContentType = sioInvoiceResponse.getOcrEngineType();
        if ((SioContentType.HTML.equals(sioContentType) || SioContentType.TEXT.equals(sioContentType)) && sioInvoiceResponse.getImageBase64() != null){

            byte[] imageBytes = java.util.Base64.getDecoder().decode(sioInvoiceResponse.getImageBase64());

            SimpleDateFormat sdf = new SimpleDateFormat("ddMMYYHHmmssS");
            Date now = new Date();
            String datePartInFilename = sdf.format(now);

            File tempFile = pathService.getTempPath(datePartInFilename);

            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(imageBytes, 0, imageBytes.length);
            fos.close();

            foundInvoice = imageConversionService.generatePreviewAndThumbnail(tempFile, foundInvoice, false);

            tempFile.delete();
        }

        if (sioInvoiceResponse.getResultData() != null){
            foundInvoice.setScansioResultData(sioInvoiceResponse.getResultData());
            foundInvoice.setOcrFullText(extractFullTextromResultData(sioInvoiceResponse.getResultData()));
        }

        boolean hasError = false;

        AppUser createdByUser =	 appUserRepository.findOneByAppUserId(UUID.fromString(foundInvoice.getCreatedBy()));

        BusinessPartner resultBusinessPartner = null;
        try{
            foundInvoice.setDateOfInvoice(new Date());
            Optional<RecognitionItemResponseV1> dateOfInvoice = sioInvoiceResponse.getItemResults().stream().filter(t -> t.getIdentificationCode().equals("DATE_OF_INVOICE")).findFirst();
            if (dateOfInvoice.isPresent()){
                if (ItemWorkflowStatus.HIT_FOUND.equals(dateOfInvoice.get().getItemWorkflowStatus())){
                    foundInvoice.setDateOfInvoice(new Date((Long) dateOfInvoice.get().getResultValue()));
                } else if (ItemWorkflowStatus.ERROR.equals(dateOfInvoice.get().getItemWorkflowStatus())){
                    hasError = true;
                }
            }

            foundInvoice.setSumOfInvoice(new BigDecimal(0.0).setScale(2, RoundingMode.HALF_EVEN));
            Optional<RecognitionItemResponseV1> sumValue = sioInvoiceResponse.getItemResults().stream().filter(t -> t.getIdentificationCode().equals("SUM_VALUE")).findFirst();
            if (sumValue.isPresent()){
                if (ItemWorkflowStatus.HIT_FOUND.equals(sumValue.get().getItemWorkflowStatus())){
                    foundInvoice.setSumOfInvoice(new BigDecimal((Double) sumValue.get().getResultValue()).setScale(2, RoundingMode.HALF_EVEN));
                } else if (ItemWorkflowStatus.ERROR.equals(sumValue.get().getItemWorkflowStatus())){
                    hasError = true;
                }
            }

            Optional<RecognitionItemResponseV1> businessPartner = sioInvoiceResponse.getItemResults().stream().filter(t -> t.getIdentificationCode().equals("BUSINESS_PARTNER")).findFirst();
            if (businessPartner.isPresent()) {
                if (ItemWorkflowStatus.HIT_FOUND.equals(businessPartner.get().getItemWorkflowStatus())){
                    String businessPartnerName = (String) businessPartner.get().getResultValue();

                    resultBusinessPartner = businessPartnerService.findOrCreateBusinessPartner(businessPartnerName, foundInvoice.getCreatedBy());
                    foundInvoice.setPaymentRecipientId(resultBusinessPartner.getBusinessPartnerId());
                    foundInvoice.setPaymentRecipientTypeEnum(PaymentPersonTypeEnum.BUSINESS_PARTNER);
                } else if (ItemWorkflowStatus.ERROR.equals(businessPartner.get().getItemWorkflowStatus())){
                    hasError = true;
                }
            }
        }catch(Exception e){
            hasError = true;
            foundInvoice.setSumOfInvoice(new BigDecimal(0.0).setScale(2, RoundingMode.HALF_EVEN));
            foundInvoice.setDateOfInvoice(new Date());
        }

        invoiceRepository.save(foundInvoice);

        if (hasError){
            resetInvoiceToError(foundInvoice);
            return foundInvoice;
        }

        InvoiceCategory invoiceCategory = null;
        if (resultBusinessPartner != null){
            invoiceCategory = autoFillHelperService.findCategoryForBusinessPartner(createdByUser, resultBusinessPartner, foundInvoice, null, true);
        }

        if (invoiceCategory != null){
            foundInvoice.setInvoiceCategory(invoiceCategory);
            autoFillHelperService.generateCostDistributionForCategory(foundInvoice, createdByUser, invoiceCategory);

            if (invoiceCategory.getInvoiceCategoryName().contains("Geldanlage")) {
                Optional<RecognitionItemResponseV1> businessPartner = sioInvoiceResponse.getItemResults().stream().filter(t -> t.getIdentificationCode().equals("ORIGIN_BUSINESS_PARTNER")).findFirst();
                if (businessPartner.isPresent()) {
                    if (ItemWorkflowStatus.HIT_FOUND.equals(businessPartner.get().getItemWorkflowStatus())){
                        String businessPartnerName = (String) businessPartner.get().getResultValue();

                        resultBusinessPartner = businessPartnerService.findOrCreateBusinessPartner(businessPartnerName, foundInvoice.getCreatedBy());
                        foundInvoice.setPaymentRecipientId(resultBusinessPartner.getBusinessPartnerId());
                        foundInvoice.setPaymentRecipientTypeEnum(PaymentPersonTypeEnum.BUSINESS_PARTNER);

                        InvoiceCategory invoiceCategoryRenewed = autoFillHelperService.findCategoryForBusinessPartner(createdByUser, resultBusinessPartner, foundInvoice, null, true);

                        if (invoiceCategoryRenewed != null){
                            foundInvoice.setInvoiceCategory(invoiceCategoryRenewed);
                            autoFillHelperService.generateCostDistributionForCategory(foundInvoice, createdByUser, invoiceCategoryRenewed);
                        }
                    }
                }
            }
        }else{
            autoFillHelperService.refreshMoneyValues(foundInvoice);
        }

        foundInvoice.setInvoiceStatusEnum(InvoiceStatusEnum.CHECK);

        invoiceRepository.save(foundInvoice);

//                firebaseService.sendDataMessage(createdByUser, FirebaseMessageType.IMAGE_OCR_COMPLETED, foundInvoice.getInvoiceId().toString(), null, null);

        budgetService.updateBudgetsIfNecessary(foundInvoice);

        return foundInvoice;
    }
}
