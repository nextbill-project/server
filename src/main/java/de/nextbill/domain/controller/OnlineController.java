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

import de.nextbill.domain.dtos.VersionDTO;
import de.nextbill.domain.services.VersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Optional;

@RestController
public class OnlineController {

	@Autowired
	private VersionService versionService;

	@ResponseBody
	@RequestMapping(value = "/online", method = RequestMethod.GET)
	public ResponseEntity<VersionDTO> isOnline(@RequestParam("currentAndroidVersion") Optional<String> currentVersion) throws ParserConfigurationException, SAXException, IOException {

		return new ResponseEntity<>(versionService.generateVersionInformation(currentVersion.orElse(null)), HttpStatus.OK);
	}

	@ResponseBody
	@RequestMapping(value = "/online/check", method = RequestMethod.GET)
	public ResponseEntity<VersionDTO> isOnlineCheck() {
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
}