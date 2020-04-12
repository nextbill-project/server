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

package de.nextbill.domain.repositories;

import de.nextbill.domain.enums.BasicStatusEnum;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.UserContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface UserContactRepository extends JpaRepository<UserContact, UUID>, AuditFieldsRepository<UserContact>{

    List<UserContact> findAllByAppUser(AppUser appUser);

    List<UserContact> findAllByAppUserAndBasicStatusEnum(AppUser appUser, BasicStatusEnum basicStatusEnum);

    UserContact findOneByAppUserAndAppUserContact(AppUser appUser, AppUser appUserContact);

    List<UserContact> findAllByAppUserAndAppUserContactAndBasicStatusEnum(AppUser appUser, AppUser appUserContact, BasicStatusEnum basicStatusEnum);

    List<UserContact> findAllByAppUserContact(AppUser appUserContact);

    List<UserContact> findAllByAppUserContactAndBasicStatusEnum(AppUser appUserContact, BasicStatusEnum basicStatusEnum);

    List<UserContact> findAllByUserContactIdInAndLastModifiedAtAfter(List<UUID> id, Date lastModifiedDate);
}
