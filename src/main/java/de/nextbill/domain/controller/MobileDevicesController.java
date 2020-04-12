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

import de.nextbill.domain.dtos.AllDataPatchDTO;
import de.nextbill.domain.dtos.MobileDeviceDTO;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.MobileDevice;
import de.nextbill.domain.pojos.DatabaseChange;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.MobileDeviceRepository;
import de.nextbill.domain.services.AuditService;
import de.nextbill.domain.services.FirebaseService;
import de.nextbill.domain.services.MobileDeviceService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping({ "webapp/api","api" })
public class MobileDevicesController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private MobileDeviceRepository mobileDeviceRepository;

	@Autowired
	private FirebaseService firebaseService;

	@Autowired
	private AuditService auditService;

	@Autowired
	private MobileDeviceService mobileDeviceService;

	@Value("${internal.paths.apkFile}")
	private String apkFilePath;

	private static Date lastUpdated = new Date();

	@ResponseBody
	@PreAuthorize("hasRole('CAN_USE_ANDROID')")
	@RequestMapping(value = "/mobileDevices/data", method = RequestMethod.POST)
	public ResponseEntity<?> patchUpdateTest(HttpServletRequest request) {
		Date startDate = new Date();

		Date mofifiedSinceDate = null;

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		Date lastModified = new Date();
		List<DatabaseChange> databaseChanges = new ArrayList<>();
		if (mofifiedSinceDate == null){
			databaseChanges.addAll(mobileDeviceService.allRelevantData(currentUser));
		}else{
			databaseChanges.addAll(mobileDeviceService.allRelevantPatchData(currentUser, mofifiedSinceDate));
			lastModified = new Date();
		}

		Date endDate = new Date();
		long diff = endDate.getTime() - startDate.getTime();

		long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
		logger.info("1 Duration of patch update: " + seconds + " sec.");
		startDate = new Date();

		boolean isFullUpate = (mofifiedSinceDate == null);
		AllDataPatchDTO allDataPatchDTO = auditService.mapToObject(databaseChanges, currentUser, isFullUpate);
		allDataPatchDTO.setLastModifiedDateFromServer(lastModified);

		endDate = new Date();
		diff = endDate.getTime() - startDate.getTime();

		seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
		logger.info("2 Duration of patch update: " + seconds + " sec.");

//		lastUpdated = new Date();

		return new ResponseEntity<>(allDataPatchDTO, HttpStatus.OK);
	}

	@ResponseBody
	@PreAuthorize("hasRole('CAN_USE_ANDROID')")
	@RequestMapping(value = "/mobileDevices/data", method = RequestMethod.PATCH)
	public ResponseEntity<?> patchUpdate(HttpServletRequest request) {

		Date startDate = new Date();

		Date mofifiedSinceDate = null;
		Long ifModifiedSince = request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
		if (ifModifiedSince != -1){
			mofifiedSinceDate = new Date(ifModifiedSince);
		}

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		Date lastModified = new Date();
		List<DatabaseChange> databaseChanges = new ArrayList<>();
		if (mofifiedSinceDate == null){
			databaseChanges.addAll(mobileDeviceService.allRelevantData(currentUser));
		}else{
			databaseChanges.addAll(mobileDeviceService.allRelevantPatchData(currentUser, mofifiedSinceDate));
			lastModified = new Date();
		}

        Date endDate = new Date();
        long diff = endDate.getTime() - startDate.getTime();

        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        logger.info("1 Duration of patch update: " + seconds + " sec.");
        startDate = new Date();

		boolean isFullUpate = (mofifiedSinceDate == null);
		AllDataPatchDTO allDataPatchDTO = auditService.mapToObject(databaseChanges, currentUser, isFullUpate);
		allDataPatchDTO.setLastModifiedDateFromServer(lastModified);

		endDate = new Date();
		diff = endDate.getTime() - startDate.getTime();

		seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
		logger.info("2 Duration of patch update: " + seconds + " sec.");

		return new ResponseEntity<>(allDataPatchDTO, HttpStatus.OK);
	}

	@RequestMapping(value = "/mobileDevices", method = RequestMethod.POST)
	public @ResponseBody
	ResponseEntity<?> create(@RequestBody MobileDeviceDTO mobileDeviceDTO) {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		MobileDevice mobileDevice = mobileDeviceRepository.findOneByDeviceId(mobileDeviceDTO.getDeviceId());

		if (mobileDevice == null){
			mobileDevice = new MobileDevice();
			mobileDevice.setMobileDeviceId(UUID.randomUUID());
			mobileDevice.setDeviceId(mobileDeviceDTO.getDeviceId());
		}

		mobileDevice.setFirebaseToken(mobileDeviceDTO.getFirebaseToken());

		mobileDevice.setAppUser(currentUser);

		mobileDeviceRepository.save(mobileDevice);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/mobileDevices/delete/{id}", method = RequestMethod.DELETE)
	public @ResponseBody ResponseEntity<?> delete(@PathVariable("id") String id) {

		MobileDevice mobileDevice = mobileDeviceRepository.findOneByDeviceId(id);
		if (mobileDevice == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		mobileDeviceRepository.delete(mobileDevice);

		return new ResponseEntity<>(HttpStatus.OK);

	}

	@RequestMapping(value = "/android/client/download/nextbill.apk", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public @ResponseBody byte[] downloadApk() throws IOException {
		File apkFile = new File(apkFilePath);
		InputStream in = new FileInputStream(apkFile);
		return IOUtils.toByteArray(in);
	}
}