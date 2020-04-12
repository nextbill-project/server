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

package de.nextbill.domain.utils.views;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

@Slf4j
public class ViewHelper {

    public static boolean fieldValidForView(Class<?> classz, String fieldName, Class<? extends MappingView.OnlyNameAndId> view) {
        if (classz == null || fieldName == null) {
            return false;
        }

        boolean isValidField = true;
        try {
            Field ratingsField = classz.getDeclaredField(fieldName);

            if (ratingsField.isAnnotationPresent(JsonView.class)){
                final JsonView[] jsonView = ratingsField.getAnnotationsByType(JsonView.class);
                Class<?> fieldClass = jsonView[0].value()[0];
                isValidField = fieldClass.isAssignableFrom(view);
            }
        } catch (Exception e) {
            log.error("Could not identify JsonView", e);
        }

        return isValidField;
    }
}

