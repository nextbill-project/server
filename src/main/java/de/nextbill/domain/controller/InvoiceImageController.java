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
import de.nextbill.domain.dtos.InvoiceDTO;
import de.nextbill.domain.model.Invoice;
import de.nextbill.domain.model.InvoiceImage;
import de.nextbill.domain.repositories.InvoiceImageRepository;
import de.nextbill.domain.repositories.InvoiceRepository;
import de.nextbill.domain.services.ImageConversionService;
import de.nextbill.domain.services.InvoiceHelperService;
import de.nextbill.domain.services.PathService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Controller
@RequestMapping({ "webapp/api","api" })
public class InvoiceImageController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private InvoiceRepository invoiceRepository;

	@Autowired
	private InvoiceImageRepository invoiceImageRepository;

	@Autowired
	private ImageConversionService imageConversionService;

	@Autowired
	private InvoiceHelperService invoiceHelperService;

	@Autowired
	private PathService pathService;

	@ResponseBody
	@RequestMapping(value = "/invoices/image/{invoiceImageId}", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
	public byte[] downloadImage(@PathVariable(value="invoiceImageId") UUID invoiceImageId) throws IOException, ImageProcessingException {
		UUID invoiceImageIdInt = null;
		
		if (invoiceImageId != null){
			invoiceImageIdInt = invoiceImageId;
		}

		InvoiceImage invoiceImage = invoiceImageRepository.findById(invoiceImageIdInt).orElse(null);
		
		if (invoiceImage != null){
			File pathThumbnail = pathService.getInvoicesPath(invoiceImage.getFileName());
			if (pathThumbnail.exists()){
				BufferedImage bufferedImage = ImageIO.read(pathThumbnail);

				if (invoiceImage.getRotate() == null || invoiceImage.getRotate()){

					Invoice invoiceForImage = invoiceRepository.findInvoiceByInvoiceImage(invoiceImage);

					File pathOriginal = imageConversionService.fileForOriginal(invoiceForImage);
					bufferedImage = imageConversionService.recognizeAndRotateImage(invoiceForImage.getInvoiceSource(), pathOriginal, bufferedImage);
					invoiceImage.setRotate(false);
					ImageIO.write(bufferedImage, "jpg", pathThumbnail);

					invoiceImageRepository.save(invoiceImage);
				}

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write( bufferedImage, "jpg", baos );
				baos.flush();
				byte[] imageInByte = baos.toByteArray();
				baos.close();

				return imageInByte;
			}
		}

		return null;
	}

	@ResponseBody
	@RequestMapping(value = "/invoices/invoiceImage/{invoiceId}", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
	public ResponseEntity<?> downloadInvoiceImage(@PathVariable(value="invoiceId") UUID invoiceId) throws IOException, ImageProcessingException {
		Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);

		if (invoice == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		InvoiceImage invoiceImage = invoice.getInvoiceImage();

		if (invoiceImage != null){
			File pathThumbnail = pathService.getInvoicesPath(invoiceImage.getFileName());
			if (pathThumbnail.exists()){
				BufferedImage bufferedImage = ImageIO.read(pathThumbnail);

				if (invoiceImage.getRotate() == null || invoiceImage.getRotate()){

					File pathOriginal = imageConversionService.fileForOriginal(invoice);
					bufferedImage = imageConversionService.recognizeAndRotateImage(invoice.getInvoiceSource(), pathOriginal, bufferedImage);
					invoiceImage.setRotate(false);
					ImageIO.write(bufferedImage, "jpg", pathThumbnail);

					invoiceImageRepository.save(invoiceImage);
				}

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write( bufferedImage, "jpg", baos );
				baos.flush();
				byte[] imageInByte = baos.toByteArray();
				baos.close();
				return new ResponseEntity<>(imageInByte, HttpStatus.OK);
			}
		}

		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

	@ResponseBody
	@RequestMapping(value = "/invoices/invoiceImage/original/{invoiceId}", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
	public ResponseEntity<?> downloadOriginalInvoiceImage(@PathVariable(value="invoiceId") UUID invoiceId) throws IOException, ImageProcessingException {
		Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);

		if (invoice == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		InvoiceImage invoiceImage = invoice.getInvoiceImage();

		if (invoiceImage != null){
			File pathOriginal = imageConversionService.fileForOriginal(invoice);
			if (pathOriginal.exists()){
				BufferedImage bufferedImage = ImageIO.read(pathOriginal);

				if (invoiceImage.getRotate() == null || invoiceImage.getRotate()){
					bufferedImage = imageConversionService.recognizeAndRotateImage(invoice.getInvoiceSource(), pathOriginal, bufferedImage);
				}

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write( bufferedImage, "jpg", baos );
				baos.flush();
				byte[] imageInByte = baos.toByteArray();
				baos.close();
				return new ResponseEntity<>(imageInByte, HttpStatus.OK);
			}
		}

		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

	@RequestMapping(value = "/invoices/image/{invoiceId}", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<?> uploadImage(@RequestParam("fileUpload") MultipartFile uploadedFileRef, @PathVariable("invoiceId") UUID invoiceId) {

		try {

			SimpleDateFormat sdf = new SimpleDateFormat("ddMMYYHHmmssS");
			Date now = new Date();
			String datePartInFilename = sdf.format(now);

			File tempFile = pathService.getTempPath(datePartInFilename);

			uploadedFileRef.transferTo(tempFile);

			Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
			if (invoice == null){
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}

			imageConversionService.removeImageData(invoice);

			Invoice newInvoice = imageConversionService.generatePreviewAndThumbnail(tempFile, invoice, true);

			InvoiceDTO invoiceDTO = invoiceHelperService.mapToDTO(newInvoice, false);

			tempFile.delete();

			return new ResponseEntity<>(invoiceDTO, HttpStatus.OK);

		} catch (IOException e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (ImageProcessingException e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@RequestMapping(value = "/invoices/image/{invoiceId}", method = RequestMethod.DELETE)
	public @ResponseBody
	ResponseEntity<?> deleteImage(@PathVariable("invoiceId") UUID invoiceId) throws IOException {

		Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
		if (invoice == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		InvoiceImage invoiceImage = invoice.getInvoiceImage();
		if (invoiceImage != null){

			File tempFile = pathService.getInvoicesPath(invoiceImage.getFileName());
			if (tempFile.exists()){
				tempFile.delete();
			}

			invoice.setInvoiceImage(null);
			invoice.setScansioResultData(null);

			invoiceRepository.save(invoice);
			invoiceImageRepository.delete(invoiceImage);
		}

		return new ResponseEntity<>(HttpStatus.OK);

	}
}