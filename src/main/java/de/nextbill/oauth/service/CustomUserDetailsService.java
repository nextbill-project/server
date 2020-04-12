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

package de.nextbill.oauth.service;

import de.nextbill.domain.enums.Right;
import de.nextbill.domain.model.AppRight;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.oauth.dtos.AuthUserDTO;
import de.nextbill.oauth.dtos.UserRoleDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

	@Autowired
	private AppUserRepository appUserRepository;

	@Override
	public AuthUserDTO loadUserByUsername(String loginName) throws UsernameNotFoundException {
		logger.info("** SEARCH by login: " + loginName);
		AppUser appUser = appUserRepository.findOneByEmailIgnoreCase(loginName);
		if (appUser == null) {
			throw new UsernameNotFoundException("Username " + loginName + " not found");
		}

		Set<AppRight> appRights = appUser.getUserRights();
		Optional<AppRight> appRight = appRights.stream().filter(t -> t.getCode().equals(Right.ENABLED)).findAny();
		if (!appRight.isPresent()) {
			throw new DisabledException("Account wurde deaktiviert. Bitte wenden Sie sich an Ihren Administrator.");
		}

		if (appUser.getDeleted() != null && appUser.getDeleted()) {
			throw new DisabledException("Account wurde gelöscht. Bitte wenden Sie sich an Ihren Administrator.");
		}

		List<UserRoleDTO> userRoleDTOS = new ArrayList<>();
		for (AppRight userRight : appRights) {
			userRoleDTOS.add(new UserRoleDTO("ROLE_" + userRight.getCode()));
		}

		AuthUserDTO userDTO = new AuthUserDTO(appUser.getAppUserId().toString(), appUser.getAppUserPassword(),
				true, true, true, true, userRoleDTOS);
		userDTO.setAppUserPassword(appUser.getAppUserPassword());
		userDTO.setAppUserId(appUser.getAppUserId());
		userDTO.setAppUserName(appUser.getAppUserName());
		userDTO.setEmail(appUser.getEmail());
		return userDTO;
	}

}