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

import de.nextbill.domain.dtos.DiagrammDataChartDTO;
import de.nextbill.domain.dtos.SearchDTO;
import de.nextbill.domain.enums.DiagramType;
import de.nextbill.domain.pojos.DiagrammDataChart;
import de.nextbill.domain.pojos.InvoiceCostDistributionItem;
import de.nextbill.domain.services.ChartService;
import de.nextbill.domain.services.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping({ "webapp/api","api" })
public class ChartController {

    @Autowired
    private SearchService searchService;

    @Autowired
    private ChartService chartHelper;

    @ResponseBody
    @RequestMapping(value = "/charts/withFilterBean", method = RequestMethod.POST)
    public ResponseEntity<?> searchWithFilter(@RequestBody SearchDTO searchDTO, @RequestParam("useAbsolutValues") Boolean useAbsolutValues, @RequestParam("chartType") DiagramType diagramType) {

        List<InvoiceCostDistributionItem> invoiceCostDistributionItems = searchService.search(true, searchDTO);

        if (diagramType.equals(DiagramType.CATEGORY)){
            DiagrammDataChart diagrammDataChart = chartHelper.chartForCategories(invoiceCostDistributionItems, useAbsolutValues);
            DiagrammDataChartDTO diagrammDataChartDTO = chartHelper.convertDiagrammForChart(diagrammDataChart);
            return new ResponseEntity<>(diagrammDataChartDTO, HttpStatus.OK);
        }else if(diagramType.equals(DiagramType.COST_PAYER)){
            DiagrammDataChart diagrammDataChart = chartHelper.chartForCostPayer(invoiceCostDistributionItems, useAbsolutValues);
            DiagrammDataChartDTO diagrammDataChartDTO = chartHelper.convertDiagrammForChart(diagrammDataChart);
            return new ResponseEntity<>(diagrammDataChartDTO, HttpStatus.OK);
        }else if(diagramType.equals(DiagramType.DAY)){
            DiagrammDataChart diagrammDataChart = chartHelper.chartForDaysInMonth(invoiceCostDistributionItems, useAbsolutValues);
            DiagrammDataChartDTO diagrammDataChartDTO = chartHelper.convertDiagrammForChart(diagrammDataChart);
            return new ResponseEntity<>(diagrammDataChartDTO, HttpStatus.OK);
        }else if(diagramType.equals(DiagramType.PAYMENT_RECIPIENTS)){
            DiagrammDataChart diagrammDataChart = chartHelper.chartForPaymentRecipients(invoiceCostDistributionItems, useAbsolutValues);
            DiagrammDataChartDTO diagrammDataChartDTO = chartHelper.convertDiagrammForChart(diagrammDataChart);
            return new ResponseEntity<>(diagrammDataChartDTO, HttpStatus.OK);
        }else if(diagramType.equals(DiagramType.MONTH)){
            DiagrammDataChart diagrammDataChart = chartHelper.chartForTimeRange(invoiceCostDistributionItems, useAbsolutValues);
            DiagrammDataChartDTO diagrammDataChartDTO = chartHelper.convertDiagrammForChart(diagrammDataChart);
            return new ResponseEntity<>(diagrammDataChartDTO, HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

}
