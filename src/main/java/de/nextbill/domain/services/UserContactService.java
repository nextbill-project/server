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

import de.nextbill.domain.dtos.UserContactDTO;
import de.nextbill.domain.enums.BasicStatusEnum;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.UserContact;
import de.nextbill.domain.repositories.UserContactRepository;
import de.nextbill.domain.utils.BeanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserContactService {

    @Autowired
    private UserContactRepository userContactRepository;

    public UserContactDTO mapToDTO(UserContact userContact){
        BeanMapper beanMapper = new BeanMapper();

        UserContactDTO userContactDTO = beanMapper.map(userContact, UserContactDTO.class);

        AppUser appUser = userContact.getAppUser();
        if (appUser != null){
            userContactDTO.setAppUserId(appUser.getAppUserId());
        }

        AppUser appUserContact = userContact.getAppUserContact();
        if (appUserContact != null){
            userContactDTO.setAppUserContactId(appUserContact.getAppUserId());
        }

        if (userContactDTO.getBasicStatusEnum() == null){
            userContactDTO.setBasicStatusEnum(BasicStatusEnum.OK);
            userContact.setBasicStatusEnum(BasicStatusEnum.OK);
            userContactRepository.save(userContact);
        }

        return userContactDTO;
    }
}
