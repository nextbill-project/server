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

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import de.nextbill.domain.enums.InvoiceSource;
import de.nextbill.domain.model.Invoice;
import de.nextbill.domain.model.InvoiceImage;
import de.nextbill.domain.repositories.InvoiceImageRepository;
import de.nextbill.domain.repositories.InvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.SystemUtils;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class ImageConversionService {

	private static final Logger logger = LoggerFactory.getLogger(ImageConversionService.class);

	@Autowired
	private InvoiceRepository invoiceRepository;

	@Autowired
	private InvoiceImageRepository invoiceImageRepository;

	@Autowired
	private PathService pathService;

	@Autowired
	private SettingsService settingsService;

	public BufferedImage convertHtmlToImage(String htmlText) throws IOException, InterruptedException {
		Writer out = null;
		File tmpNewFile = null;

		try{
			SimpleDateFormat sdf = new SimpleDateFormat("ddMMYYHHmmssS");
			Date now = new Date();
			String tempFileName = sdf.format(now);

			tmpNewFile = pathService.getTempPath(tempFileName + ".html");

			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpNewFile), "UTF-8"));
			out.write(htmlText);
			out.close();

			return convertUriToImage(tmpNewFile.getAbsolutePath());
		} finally {
			if (out != null){
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (tmpNewFile != null){
				tmpNewFile.delete();
			}
		}
	}

	private BufferedImage convertUriToImage(String uriToHtml) throws IOException, InterruptedException {

		File generatedImageFile = null;

		try{
			SimpleDateFormat sdf = new SimpleDateFormat("ddMMYYHHmmssS");
			Date now = new Date();
			String tempFileNameImage = sdf.format(now) + "_output.png";

			boolean isUnix = SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_UNIX;

			ProcessBuilder prb;
			if (isUnix){
				String convertCLIcommand = settingsService.getCurrentSettings().getPathToFirefox() + " -screenshot " + pathService.getTempPath(tempFileNameImage) + " " + uriToHtml;
				prb = new ProcessBuilder("/bin/bash", "-c", convertCLIcommand);
				log.info(convertCLIcommand);
			}else{
				uriToHtml = "file:///" + uriToHtml;
				String convertCLIcommand = "\"" + settingsService.getCurrentSettings().getPathToFirefox() + "\" --screenshot " + pathService.getTempPath(tempFileNameImage) + " " + uriToHtml;
				prb = new ProcessBuilder("CMD", "/C", convertCLIcommand);
				log.info(convertCLIcommand);
			}
			prb.directory(pathService.getInvoicesPath(null));

			Process pr = prb.start();

			int terminationNr = pr.waitFor();

//			BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
//			StringBuilder builder = new StringBuilder();
//			String line = null;
//			while ( (line = reader.readLine()) != null) {
//				builder.append(line);
//				builder.append(System.getProperty("line.separator"));
//			}
//			String result = builder.toString();
//			log.info(result);

			generatedImageFile = pathService.getTempPath(tempFileNameImage);

			BufferedImage inputBufferedImage = ImageIO.read(generatedImageFile);

			BufferedImage bufferedImage = new BufferedImage(inputBufferedImage.getWidth(null), inputBufferedImage.getHeight(null), BufferedImage.TYPE_INT_RGB);
			Graphics2D g = bufferedImage.createGraphics();

			g.drawImage(inputBufferedImage, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), Color.WHITE, null);
			g.dispose();

			return bufferedImage;
		} finally {

			if (generatedImageFile != null){
				generatedImageFile.delete();
			}
		}
	}

	public String createHtmlFromText(String text){

		return "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
				"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
				"<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
				"<html>\n" +
				"<head>\n" +
//				"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />" +
				"  <title>Html Dummy</title>\n" +
				"\n" +
				"</head>\n" +
				"\n" +
				"<body style=\"font-size: 18px;font-family: Arial, Verdana;text-align:center;padding:50px;\">\n" +
				StringEscapeUtils.escapeHtml(text).replace("\r\n", "<br/>").replace("Absatz", "<br/>") +
				"\n</body>\n" +
				"</html>";
	}

	public Invoice generatePreviewAndThumbnail(File tempFile, Invoice savedInvoice, boolean rotate) throws IOException, ImageProcessingException {

		try {
			SimpleDateFormat sdf = new SimpleDateFormat("ddMMYYHHmmssS");
			Date now = new Date();
			String datePartInFilename = sdf.format(now);

			String newFilename = String.valueOf(savedInvoice.getInvoiceId()) + datePartInFilename;
			InvoiceImage invoiceImage = new InvoiceImage();
			invoiceImage.setInvoiceImageId(UUID.randomUUID());
			invoiceImage.setFileName(newFilename);
			invoiceImage.setRotate(rotate);
			invoiceImage = invoiceImageRepository.save(invoiceImage);

			savedInvoice.setInvoiceImage(invoiceImage);
			savedInvoice = invoiceRepository.save(savedInvoice);

			File originalFile = pathService.getInvoicesPath(newFilename);
			String jpgThumbnailFilename = newFilename + "_thumbnail";
			File thumbnailFile = pathService.getInvoicesPath(jpgThumbnailFilename);

			java.nio.file.Files.copy(Paths.get(tempFile.getAbsolutePath()), Paths.get( originalFile.getAbsolutePath() ));

			BufferedImage bufferedImage = ImageIO.read(originalFile);
			bufferedImage = rescaleImage(bufferedImage, 1400);

			if (rotate){
				bufferedImage = recognizeAndRotateImage(savedInvoice.getInvoiceSource(), originalFile, bufferedImage);
				invoiceImage.setRotate(false);
			}

			ImageIO.write(bufferedImage, "jpg", thumbnailFile);

			invoiceImage.setFileName(jpgThumbnailFilename);
			invoiceImage.setFileNameOriginal(newFilename);

			invoiceImageRepository.save(invoiceImage);
			savedInvoice = invoiceRepository.save(savedInvoice);

		}catch (IOException e){
			e.printStackTrace();
		}

		return savedInvoice;
	}

	public void removeImageData(Invoice invoice) throws IOException {
		InvoiceImage invoiceImage = invoice.getInvoiceImage();
		if (invoiceImage != null){
			File invoiceImageFile = pathService.getInvoicesPath(invoiceImage.getFileName());
			if (invoiceImageFile.exists()){
				invoiceImageFile.delete();
			}
			invoice.setInvoiceImage(null);
			invoice.setScansioResultData(null);

			invoiceRepository.save(invoice);
			invoiceImageRepository.delete(invoiceImage);
		}
	}

	public File fileForOriginal(Invoice invoice) throws IOException{
		if (invoice.getInvoiceImage() != null) {
			InvoiceImage invoiceImage = invoice.getInvoiceImage();
			File filePreview = pathService.getInvoicesPath(invoiceImage.getFileNameOriginal());
			if (filePreview.exists() && filePreview.canRead()){
				return filePreview;
			}else{
				throw new IOException();
			}
		}

		return null;
	}
	
	public BufferedImage rescaleImage(BufferedImage bufferedImage, int width){
		BigDecimal widthImage = new BigDecimal(bufferedImage.getWidth());
		BigDecimal heightImage = new BigDecimal(bufferedImage.getHeight());
		
		BigDecimal ratio = (widthImage).divide(heightImage, 10000, RoundingMode.HALF_EVEN);
		Integer height = (new BigDecimal(width)).divide(ratio, 0, RoundingMode.HALF_EVEN).intValue();
		
		BufferedImage bufferedImage2 = rescaleImage(bufferedImage, width, height);
		
		return bufferedImage2;
	}
	
	public BufferedImage rescaleImage(BufferedImage bufferedImage, int width, int height){
		Image scaledImage = bufferedImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
		BufferedImage imageBuff = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics g = imageBuff.createGraphics();
		g.drawImage(scaledImage, 0, 0, new Color(0,0,0), null);
		g.dispose();
		
		return imageBuff;
	}

	public BufferedImage convertToMonochrome(BufferedImage bufferedImage, BigDecimal level) {
		BufferedImage brightenedImage = brighten(bufferedImage, level);
        BufferedImage bw = new BufferedImage(
        		bufferedImage.getWidth(),
        		bufferedImage.getHeight(),
                BufferedImage.TYPE_BYTE_BINARY);
        Graphics g = bw.createGraphics();
        g.drawImage(brightenedImage, 0, 0, null);
        g.dispose();

		return bw;
	}

	public BufferedImage convertToMonochrome(BufferedImage bufferedImage) {
		BufferedImage bw = new BufferedImage(
				bufferedImage.getWidth(),
				bufferedImage.getHeight(),
				BufferedImage.TYPE_BYTE_BINARY);
		Graphics g = bw.createGraphics();
		g.drawImage(bufferedImage, 0, 0, null);
		g.dispose();

		return bw;
	}
	
    public BufferedImage brighten(BufferedImage src, BigDecimal levelDouble) {

        BufferedImage dst = new BufferedImage(
                src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        float level = levelDouble.floatValue();
        float[] scales = {level, level, level};
        float[] offsets = new float[4];
        RescaleOp rop = new RescaleOp(scales, offsets, null);

        Graphics2D g = dst.createGraphics();
        g.drawImage(src, rop, 0, 0);
        g.dispose();

        return dst;
    }


	public BufferedImage recognizeAndRotateImage(InvoiceSource invoiceSource, File imageFile, BufferedImage inputBufferedImage) throws IOException, ImageProcessingException {

		Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
		Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

		Integer orientation = 1;

		Integer width = inputBufferedImage.getWidth();
		Integer height = inputBufferedImage.getHeight();

		boolean autoImageRotationOk = true;
		if (directory != null) {
			try {
				orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
			} catch (MetadataException me) {
				autoImageRotationOk = false;
				log.warn("Could not get orientation");
			}
		}

		if (directory == null || !autoImageRotationOk) {
			if (InvoiceSource.CAMERA.equals(invoiceSource) && height < width) {
                return Scalr.rotate(inputBufferedImage, Scalr.Rotation.CW_90);
			}
		}

		AffineTransform t = new AffineTransform();

		switch (orientation) {
			case 1:
				break;
			case 2: // Flip X
				t.scale(-1.0, 1.0);
				t.translate(-width, 0);
				break;
			case 3: // PI rotation
				t.translate(width, height);
				t.rotate(Math.PI);
				break;
			case 4: // Flip Y
				t.scale(1.0, -1.0);
				t.translate(0, -height);
				break;
			case 5: // - PI/2 and Flip X
				t.rotate(-Math.PI / 2);
				t.scale(-1.0, 1.0);
				break;
			case 6: // -PI/2 and -width
				t.translate(height, 0);
				t.rotate(Math.PI / 2);
				break;
			case 7: // PI/2 and Flip
				t.scale(-1.0, 1.0);
				t.translate(-height, 0);
				t.translate(0, width);
				t.rotate(  3 * Math.PI / 2);
				break;
			case 8: // PI / 2
				t.translate(0, width);
				t.rotate(  3 * Math.PI / 2);
				break;
		}

		if (orientation != 1){
			AffineTransformOp op = new AffineTransformOp(t, AffineTransformOp.TYPE_BICUBIC);

			return Scalr.apply(inputBufferedImage, op);
		}

		return inputBufferedImage;
	}
}
