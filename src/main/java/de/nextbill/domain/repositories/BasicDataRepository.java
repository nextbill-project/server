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

import de.nextbill.domain.enums.BasicDataSubType;
import de.nextbill.domain.enums.BasicDataType;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.BasicData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface BasicDataRepository extends JpaRepository<BasicData, UUID>, AuditFieldsRepository<BasicData>{
    BasicData findOneByAppUserAndBasicDataTypeAndBasicDataSubType(AppUser appUser, BasicDataType basicDataType, BasicDataSubType basicDataSubType);
    BasicData findOneByAppUserAndBasicDataTypeAndBasicDataSubTypeAndObject1ClassAndObject1Id(AppUser appUser, BasicDataType basicDataType, BasicDataSubType basicDataSubType, String object1Class, String object1Id);
    List<BasicData> findAllByAppUserAndBasicDataType(AppUser appUser, BasicDataType basicDataType);
    List<BasicData> findAllByAppUserAndBasicDataTypeAndBasicDataSubType(AppUser appUser, BasicDataType basicDataType, BasicDataSubType basicDataSubType);
    List<BasicData> findAllByBasicDataIdInAndLastModifiedAtAfter(List<UUID> id, Date lastModifiedDate);
}
