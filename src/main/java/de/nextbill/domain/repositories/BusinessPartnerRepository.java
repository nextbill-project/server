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
import de.nextbill.domain.model.BusinessPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface BusinessPartnerRepository extends JpaRepository<BusinessPartner, UUID>, AuditFieldsRepository<BusinessPartner> {

    @Query("SELECT j FROM BusinessPartner j WHERE lower(j.businessPartnerName) like lower(concat('%', :appUserName,'%')) AND j.basicStatusEnum = :basicStatusEnum AND (j.appUser = :appUser or (j.businessPartnerPublicStatus = 'EXPERIMENTAL' or j.businessPartnerPublicStatus = 'PUBLIC'))")
    List<BusinessPartner> findBusinessPartnerCaseInsensitiveForUser(@Param("appUserName") String appUserName, @Param("basicStatusEnum") BasicStatusEnum basicStatusEnum, @Param("appUser") AppUser appUser);

    @Query("SELECT j FROM BusinessPartner j WHERE j.businessPartnerName = :appUserName AND j.basicStatusEnum = :basicStatusEnum AND (j.appUser = :appUser or (j.businessPartnerPublicStatus = 'EXPERIMENTAL' or j.businessPartnerPublicStatus = 'PUBLIC'))")
    BusinessPartner findByBusinessPartnerNameAndBasicStatusEnumAndIsValidForUser(@Param("appUserName") String appUserName, @Param("basicStatusEnum") BasicStatusEnum basicStatusEnum, @Param("appUser") AppUser appUser);

    List<BusinessPartner> findAllByAppUserIsNullOrAppUserAndBasicStatusEnum(AppUser appUser, BasicStatusEnum basicStatusEnum);

    List<BusinessPartner> findAllByAppUserIsNullAndBasicStatusEnum(BasicStatusEnum basicStatusEnum);

    List<BusinessPartner> findAllByLastModifiedAtAfter(Date lastModifiedDate);

    List<BusinessPartner> findAllByBasicStatusEnum(BasicStatusEnum basicStatusEnum);

    List<BusinessPartner> findAllByBusinessPartnerIdInAndLastModifiedAtAfter(List<UUID> id, Date lastModifiedDate);
}
