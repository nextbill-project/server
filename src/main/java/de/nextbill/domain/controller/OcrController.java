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

import com.drew.imaging.ImageProcessingException;
import de.nextbill.domain.dtos.ArticleDTO;
import de.nextbill.domain.dtos.CostDistributionItemDTO;
import de.nextbill.domain.dtos.InvoiceDTO;
import de.nextbill.domain.dtos.TextRecognitionDTO;
import de.nextbill.domain.enums.*;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.CostDistributionItem;
import de.nextbill.domain.model.Invoice;
import de.nextbill.domain.model.Settings;
import de.nextbill.domain.pojos.scansio.RecognitionResponseV1;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.CostDistributionItemRepository;
import de.nextbill.domain.repositories.InvoiceRepository;
import de.nextbill.domain.services.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping({ "webapp/api","api" })
public class OcrController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AutoFillHelperService autoFillHelperService;

    @Autowired
    private ImageConversionService imageConversionService;

    @Autowired
    private ScansioService scansioService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private InvoiceHelperService invoiceHelperService;

    @Autowired
    private CostDistributionItemRepository costDistributionItemRepository;

    @Autowired
    private PathService pathService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private CostDistributionItemService costDistributionItemService;

    @RequestMapping(value = "/service/upload/{uuid}", method = RequestMethod.POST)
    public @ResponseBody
    ResponseEntity<?> uploadAndOcr(@RequestParam("fileUpload") MultipartFile uploadedFileRef, @PathVariable("uuid") UUID uuid) throws ImageProcessingException, IOException {
        logger.info("OCR/upload started");

        String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser appUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

        SimpleDateFormat sdf = new SimpleDateFormat("ddMMYYHHmmssS");
        Date now = new Date();
        String datePartInFilename = sdf.format(now);

        File tempFile = pathService.getTempPath(datePartInFilename);
        uploadedFileRef.transferTo(tempFile);

        Invoice invoice = new Invoice();
        invoice.setInvoiceStatusEnum(InvoiceStatusEnum.ANALYZING);
        invoice.setPayerId(appUser.getAppUserId());
        invoice.setPayerTypeEnum(PaymentPersonTypeEnum.USER);
        invoice.setInvoiceId(uuid);
        invoice.setSpecialType(false);
        invoice.setCreatedBy(appUser.getAppUserId().toString());
        invoice.setRemarks("");
        invoice.setInvoiceSource(InvoiceSource.CAMERA);
        invoice.setRepetitionTypeEnum(RepetitionTypeEnum.ONCE);
        Invoice savedInvoice = invoiceRepository.save(invoice);

        autoFillHelperService.generateDefaultCostDistributionItem(invoice);

        Invoice invoiceWithImages = imageConversionService.generatePreviewAndThumbnail(tempFile, savedInvoice, true);

        tempFile.delete();

        Settings settings = settingsService.getCurrentSettings();
        if (settings.getScansioEnabled()) {
            RecognitionResponseV1 recognitionResponseV1 = scansioService.sendImageForInvoiceToScansio(invoiceWithImages, true);
            savedInvoice = scansioService.readScansioResponse(recognitionResponseV1, invoiceWithImages);
        }else{
            savedInvoice.setInvoiceStatusEnum(InvoiceStatusEnum.CHECK);
            savedInvoice.setSumOfInvoice(BigDecimal.ZERO);
            savedInvoice.setDateOfInvoice(new Date());
            savedInvoice = invoiceRepository.save(savedInvoice);
        }

        List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByInvoice(savedInvoice);
        savedInvoice.setCostDistributionItems(costDistributionItems);

        InvoiceDTO invoiceDTO1 = invoiceHelperService.mapToDTO(savedInvoice, false);
        List<CostDistributionItemDTO> costDistributionItemDTOs = new ArrayList<>();
        for (CostDistributionItem costDistributionItem : costDistributionItems) {
            costDistributionItemDTOs.add(costDistributionItemService.mapToDTO(costDistributionItem));
        }

        invoiceDTO1.setCostDistributionItemDTOs(costDistributionItemDTOs);

        return new ResponseEntity<>(invoiceDTO1, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('OCR')")
    @RequestMapping(value = "/service/upload/{uuid}/articleAnalysisPreparement", method = RequestMethod.POST)
    public @ResponseBody
    ResponseEntity<?> analyzeArticlePreparement(@PathVariable("uuid") UUID uuid) throws IOException {
        logger.info("OCR/upload started");

        Invoice savedInvoice = invoiceRepository.findById(uuid).orElse(null);

        Settings settings = settingsService.getCurrentSettings();
        if (settings.getScansioEnabled()) {
            scansioService.prepareArticleAnalysis(savedInvoice);
        }else{
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasRole('OCR')")
    @RequestMapping(value = "/service/upload/{uuid}/repeatAnalysis", method = RequestMethod.POST)
    public @ResponseBody
    ResponseEntity<?> repeatOcrAnalysis(@PathVariable("uuid") UUID uuid) throws IOException, ImageProcessingException {
        logger.info("OCR/upload started");

        String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser appUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

        Invoice invoice = invoiceRepository.findById(uuid).orElse(null);

        Settings settings = settingsService.getCurrentSettings();
        if (settings.getScansioEnabled()) {
            RecognitionResponseV1 recognitionResponseV1 = scansioService.sendImageForInvoiceToScansio(invoice, true);
            invoice = scansioService.readScansioResponse(recognitionResponseV1, invoice);

            invoiceService.setCorrectionStatus(invoice, CorrectionStatus.CHECK, appUser);

            invoice.setInvoiceStatusEnum(InvoiceStatusEnum.CHECK);
            invoice = invoiceRepository.save(invoice);
        }

        InvoiceDTO invoiceDTO1 = invoiceHelperService.mapToDTO(invoice, false);

        return new ResponseEntity<>(invoiceDTO1, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('OCR')")
    @RequestMapping(value = "/service/upload/{uuid}/articleAnalysis", method = RequestMethod.POST)
    public @ResponseBody
    ResponseEntity<?> analyzeArticle(@PathVariable("uuid") UUID uuid, @RequestParam("x") BigDecimal x, @RequestParam("y") BigDecimal y) throws IOException {
        logger.info("OCR/upload started");

        Invoice savedInvoice = invoiceRepository.findById(uuid).orElse(null);

        ArticleDTO articleDTO = null;

        Settings settings = settingsService.getCurrentSettings();
        if (settings.getScansioEnabled()) {
            articleDTO = scansioService.sendImageToScansioForArticle(savedInvoice, x, y);
        }else{
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        if (articleDTO == null){
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(articleDTO, HttpStatus.OK);
    }

    @RequestMapping(value = "/service/upload/mic/{uuid}", method = RequestMethod.POST)
    public @ResponseBody
    ResponseEntity<?> uploadTextAndOcr(@PathVariable("uuid") UUID uuid, @RequestBody TextRecognitionDTO textRecognitionDTO) throws IOException, InterruptedException, ImageProcessingException {
        logger.info("OCR/upload started");

        String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser appUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

        Invoice invoice = new Invoice();
        invoice.setInvoiceStatusEnum(InvoiceStatusEnum.ANALYZING);
        invoice.setPayerId(appUser.getAppUserId());
        invoice.setPayerTypeEnum(PaymentPersonTypeEnum.USER);
        invoice.setInvoiceId(uuid);
        invoice.setSpecialType(false);
        invoice.setCreatedBy(appUser.getAppUserId().toString());
        invoice.setRemarks("");
        invoice.setInvoiceSource(InvoiceSource.MIC);
        invoice.setRepetitionTypeEnum(RepetitionTypeEnum.ONCE);
        Invoice savedInvoice = invoiceRepository.save(invoice);

        autoFillHelperService.generateDefaultCostDistributionItem(savedInvoice);

        Settings settings = settingsService.getCurrentSettings();
        if (settings.getUseFirefoxForHtmlToImage()){
            File tempFile = File.createTempFile(UUID.randomUUID().toString(), "");

            String text = imageConversionService.createHtmlFromText(textRecognitionDTO.getTextToRecognize());
            BufferedImage bufferedImage = imageConversionService.convertHtmlToImage(text);
            ImageIO.write(bufferedImage, "jpg", tempFile);

            savedInvoice = imageConversionService.generatePreviewAndThumbnail(tempFile, savedInvoice, false);

            tempFile.delete();
        }

        if (settings.getScansioEnabled()) {
            RecognitionResponseV1 recognitionResponseV1 = scansioService.sendTextToScansio(savedInvoice, textRecognitionDTO.getTextToRecognize());
            savedInvoice = scansioService.readScansioResponse(recognitionResponseV1, savedInvoice);

            savedInvoice.setRemarks(textRecognitionDTO.getTextToRecognize());
            savedInvoice = invoiceRepository.save(savedInvoice);
        }else{
            savedInvoice.setRemarks(textRecognitionDTO.getTextToRecognize());
            savedInvoice.setInvoiceStatusEnum(InvoiceStatusEnum.CHECK);
            savedInvoice.setSumOfInvoice(BigDecimal.ZERO);
            savedInvoice.setDateOfInvoice(new Date());
            savedInvoice = invoiceRepository.save(savedInvoice);
        }

        InvoiceDTO invoiceDTO = invoiceHelperService.mapToDTO(savedInvoice, false);

        List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByInvoice(savedInvoice);
        List<CostDistributionItemDTO> costDistributionItemDTOs = new ArrayList<>();
        for (CostDistributionItem costDistributionItem : costDistributionItems) {
            costDistributionItemDTOs.add(costDistributionItemService.mapToDTO(costDistributionItem));
        }

        invoiceDTO.setCostDistributionItemDTOs(costDistributionItemDTOs);

        return new ResponseEntity<>(invoiceDTO, HttpStatus.OK);
    }

}
