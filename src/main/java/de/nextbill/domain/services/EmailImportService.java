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

import de.nextbill.commons.mailmanager.model.Mail;
import de.nextbill.commons.mailmanager.service.MailService;
import de.nextbill.domain.enums.InvoiceSource;
import de.nextbill.domain.enums.InvoiceStatusEnum;
import de.nextbill.domain.enums.PaymentPersonTypeEnum;
import de.nextbill.domain.enums.RepetitionTypeEnum;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.Invoice;
import de.nextbill.domain.model.Settings;
import de.nextbill.domain.pojos.scansio.RecognitionResponseV1;
import de.nextbill.domain.pojos.scansio.enums.SioContentType;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.InvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class EmailImportService {

	@Autowired
	private InvoiceRepository invoiceRepository;

	@Autowired
	private ImageConversionService imageConversionService;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private AutoFillHelperService autoFillHelperService;

	@Autowired
	private ScansioService scansioService;

	@Autowired
	private MailService mailService;

	@Autowired
	private FirebaseService firebaseService;

	@Autowired
	private FileAnalysisService fileAnalysisService;

	@Autowired
	private PathService pathService;

	@Autowired
	private SettingsService settingsService;

	private static final Logger logger = LoggerFactory.getLogger(EmailImportService.class);

	@PostConstruct
	public void importEmails() {

		try {

			ArrayList<Mail> mails = mailService.getAllMailsFromFolder();
			Settings settings = settingsService.getCurrentSettings();

			for (Mail mail : mails) {

				if (mail.isHasBeenSeen()){
					continue;
				}

				Invoice invoice = new Invoice();

				try {
					AppUser appUser = appUserRepository.findOneByEmailIgnoreCaseAndDeletedIsNull(mail.getFrom().getAddress().toLowerCase());

					if (appUser == null){
						if (!mail.getRecipients().isEmpty()){
							appUser = appUserRepository.findOneByEmailIgnoreCaseAndDeletedIsNull(mail.getRecipients().get(0).getAddress().toLowerCase());
						}
					}

					if (appUser == null){
						if (settings.getDeleteMail()){
							mailService.deleteMail(mail.getMessageId());
						}else{
							mailService.setMailRead(mail.getMessageId());
						}
						logger.info("EmailImportService: AppUser not found!!! " + mail.getSender().getAddress() + " " + mail.getFrom().getAddress());
						continue;
					}

					logger.info("*** EMailImportJob: Analyze Mail ***");

					invoice.setInvoiceStatusEnum(InvoiceStatusEnum.ANALYZING);
					invoice.setPayerId(appUser.getAppUserId());
					invoice.setPayerTypeEnum(PaymentPersonTypeEnum.USER);
					invoice.setInvoiceId(UUID.randomUUID());

					invoice.setRepetitionTypeEnum(RepetitionTypeEnum.ONCE);
					invoice.setSpecialType(false);
					invoice.setRemarks("");
					invoice.setInvoiceSource(InvoiceSource.MAIL);
					invoice = invoiceRepository.save(invoice);
					invoice.setCreatedBy(appUser.getAppUserId().toString());
					invoice = invoiceRepository.save(invoice);

					autoFillHelperService.generateDefaultCostDistributionItem(invoice);

					SioContentType sioContentType = null;
					String html = null;
					String text = null;
					if (!mail.getAttachments().isEmpty()) {

						for (File tmpFile : mail.getAttachments()) {
							MediaType mediaType = fileAnalysisService.detectFileType(tmpFile);

							if (mediaType != null && mediaType.toString().contains("image")){
								try {
									invoice = imageConversionService.generatePreviewAndThumbnail(tmpFile, invoice, false);
									break;
								} catch (Exception e) {
									log.info("conversion error", e);
								}
							}
						}

						sioContentType = SioContentType.IMAGE;
					}

					text = mail.getMessageTextPlain();
					html = mail.getMessageTextHtml();

					if (sioContentType == null && mail.isMessageTextHtmlType()) {
						sioContentType = sioContentType.HTML;
					}

					if (sioContentType == null && StringUtils.isNotEmpty(mail.getMessageTextPlain())){
						sioContentType = sioContentType.TEXT;
					}

					if (sioContentType != null){
						boolean scanned = false;

						if (text != null){
							invoice.setRemarks(text.trim());
							invoice = invoiceRepository.save(invoice);
						}

						if (settings.getUseFirefoxForHtmlToImage()){
							BufferedImage bufferedImage = imageConversionService.convertHtmlToImage(html);
							File newFile = pathService.getTempPath(UUID.randomUUID().toString() + ".jpg");
							ImageIO.write(bufferedImage, "jpg", newFile);

							invoice = imageConversionService.generatePreviewAndThumbnail(newFile, invoice, false);
							newFile.delete();
						}

						if (settings.getScansioEnabled()) {
							try {
								if (SioContentType.IMAGE.equals(sioContentType)) {
									RecognitionResponseV1 recognitionResponseV1 = scansioService.sendImageForInvoiceToScansio(invoice, false);
									scansioService.readScansioResponse(recognitionResponseV1, invoice);
								}else if (SioContentType.HTML.equals(sioContentType)){

									RecognitionResponseV1 recognitionResponseV1 = null;
									if (settings.getUseFirefoxForHtmlToImage() && imageConversionService.fileForOriginal(invoice) != null){
										recognitionResponseV1 = scansioService.sendImageForInvoiceToScansio(invoice, false);
									}else{
										if (text != null){
											recognitionResponseV1 = scansioService.sendTextToScansio(invoice, text);
										}
									}

									if (recognitionResponseV1 != null){
										scansioService.readScansioResponse(recognitionResponseV1, invoice);
									}
								}else {
									RecognitionResponseV1 recognitionResponseV1 = scansioService.sendTextToScansio(invoice, text);
									scansioService.readScansioResponse(recognitionResponseV1, invoice);
								}

								scanned = true;
							}catch (Exception e){
								logger.warn("could not send image to scansio", e);
							}
						}

						if (!scanned){
							invoice.setInvoiceStatusEnum(InvoiceStatusEnum.CHECK);
							invoice.setSumOfInvoice(BigDecimal.ZERO);
							invoice.setDateOfInvoice(new Date());
							invoice = invoiceRepository.save(invoice);
						}
					}

//					firebaseService.sendDataMessage(appUser, FirebaseMessageType.MAIL_ANALYSIS_COMPLETED, invoice.getInvoiceId().toString(), null, null);

					if (settings.getDeleteMail()){
						mailService.deleteMail(mail.getMessageId());
					}else{
						mailService.setMailRead(mail.getMessageId());
					}

				} catch (Exception e) {
					logger.info("E-Mail error", e);
					invoice.setInvoiceStatusEnum(InvoiceStatusEnum.CHECK);
					invoice.setSumOfInvoice(BigDecimal.ZERO);
					invoice.setDateOfInvoice(new Date());
					invoiceRepository.save(invoice);
				}
			}

		} catch (Exception e) {
			logger.info("E-Mail error", e);
		}
	}

}
