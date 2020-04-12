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

package de.nextbill.domain.utils;

import org.dozer.DozerBeanMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BeanMapper {

	private static final DozerBeanMapper mapper;

	static {
		mapper = new DozerBeanMapper();
		mapper.setMappingFiles(Collections.singletonList("mappings.xml"));
	}

	public <T> T map(Object obj, Class<T> classz) {
		return mapper.map(obj, classz);
	}

	public <T, S> void map(S sourceObj, T targetObj) {
		mapper.map(sourceObj, targetObj);
	}

	public <T> List<T> map(List<?> objects, Class<T> classz) {
		List<T> list = new ArrayList<>();
		for (Object object : objects) {
			list.add(mapper.map(object, classz));
		}
		return list;
	}
}
