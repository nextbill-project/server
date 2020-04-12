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

package de.nextbill.domain.dtos;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AppUserDTO implements java.io.Serializable {

	private static final long serialVersionUID = 1L;

	private UUID appUserId;
	private String appUserName;
	private String appUserPassword;
	private String email;
	private String paypalName;

	private Boolean enabled;
	private Boolean useAndroid;
	private Boolean useOcr;
	private Boolean editUsers;
	private Boolean editSettings;

	private List<UserContactDTO> userContactDTOs;
	private List<AppRightDTO> appRightDTOs;

}
