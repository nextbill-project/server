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

import de.nextbill.domain.dtos.BudgetDTO;
import de.nextbill.domain.dtos.TypeEnumDTO;
import de.nextbill.domain.enums.BudgetRepetitionType;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.Budget;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.BudgetRepository;
import de.nextbill.domain.services.BudgetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping({ "webapp/api","api" })
public class BudgetController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private BudgetService budgetService;

	@Autowired
	private BudgetRepository budgetRepository;

	@Autowired
	private AppUserRepository appUserRepository;

	@ResponseBody
	@RequestMapping(value = "/budgets/budgetRepetitionTypeEnums", method = RequestMethod.GET)
	public ResponseEntity<?> invoiceRepetitionTypeEnums() {

		List<TypeEnumDTO> typeEnumDTOS = new ArrayList<>();

		for (BudgetRepetitionType budgetRepetitionType : BudgetRepetitionType.values()) {
			TypeEnumDTO typeEnumDTO = new TypeEnumDTO();
			typeEnumDTO.setDisplayName(budgetRepetitionType.getDisplayName());
			typeEnumDTO.setName(budgetRepetitionType.name());

			typeEnumDTOS.add(typeEnumDTO);
		}

		return new ResponseEntity<>(typeEnumDTOS, HttpStatus.OK);
	}

	@RequestMapping(value = "/budgets/{cid}", method = RequestMethod.GET)
	public ResponseEntity<?> getBudget(@PathVariable("cid") UUID id) {

		Budget budget = budgetRepository.findById(id).orElse(null);

		if (budget == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		BudgetDTO budgetDTO = budgetService.mapToDTO(budget);

		return new ResponseEntity<>(budgetDTO, HttpStatus.OK);
	}

	@RequestMapping(value = "/budgets", method = RequestMethod.POST)
	public ResponseEntity<?> insertBudget(@RequestBody BudgetDTO budgetDTO) {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser appUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		Budget budget = budgetService.saveBudget(appUser, budgetDTO);

		budgetDTO = budgetService.mapToDTO(budget);

		return new ResponseEntity<>(budgetDTO, HttpStatus.OK);
	}

	@RequestMapping(value = "/budgets", method = RequestMethod.GET)
	public ResponseEntity<?> getBudgets() {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser appUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		List<Budget> budgets = budgetRepository.findAllByAppUser(appUser);

		List<BudgetDTO> budgetDTOs = new ArrayList<>();
		for (Budget budget : budgets) {
			budgetDTOs.add(budgetService.mapToDTO(budget));
		}

		return new ResponseEntity<>(budgetDTOs, HttpStatus.OK);
	}

	@RequestMapping(value = "/budgets/{cid}", method = RequestMethod.PUT)
	public ResponseEntity<?> updateBudget(@RequestBody BudgetDTO budgetDTO, @PathVariable("cid") UUID id) {

		budgetDTO.setBudgetId(id);

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser appUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		Budget budget = budgetService.saveBudget(appUser, budgetDTO);

		budgetDTO = budgetService.mapToDTO(budget);

	    return new ResponseEntity<>(budgetDTO, HttpStatus.OK);
	}

	@RequestMapping(value = "/budgets/{cid}", method = RequestMethod.DELETE)
	public ResponseEntity<?> deleteBudget(@PathVariable("cid") UUID id) {

		Budget budget = budgetRepository.findById(id).orElse(null);

		if (budget == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		budgetService.deleteBudget(budget);

		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

}