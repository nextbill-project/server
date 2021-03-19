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

package de.nextbill.domain.pojos.scansio;

import de.nextbill.domain.pojos.scansio.enums.SioContentType;
import de.nextbill.domain.pojos.scansio.enums.WorkflowStatus;
import lombok.Data;

import java.util.List;

@Data
public class RecognitionResponseV1 {

    private String id;

    private WorkflowStatus workflowStatus;

    private SioContentType ocrEngineType;

    private String identificationKey;

    private String imageBase64;

    private String resultData;

    private String configurationName;

    private List<RecognitionItemResponseV1> results;
}
