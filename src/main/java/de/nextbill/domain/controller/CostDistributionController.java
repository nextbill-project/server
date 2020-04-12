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

import de.nextbill.domain.dtos.CostDistributionDTO;
import de.nextbill.domain.dtos.CostDistributionItemDTO;
import de.nextbill.domain.enums.BasicStatusEnum;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.CostDistribution;
import de.nextbill.domain.model.CostDistributionItem;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.CostDistributionItemRepository;
import de.nextbill.domain.repositories.CostDistributionRepository;
import de.nextbill.domain.services.CostDistributionItemService;
import de.nextbill.domain.utils.BeanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.persistence.Transient;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping({ "webapp/api","api" })
public class CostDistributionController {

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private CostDistributionRepository costDistributionRepository;

	@Autowired
	private CostDistributionItemRepository costDistributionItemRepository;

	@Autowired
	private CostDistributionItemService costDistributionItemService;

	@ResponseBody
	@RequestMapping(value = "/costdistributions", method = RequestMethod.GET)
	public ResponseEntity<?> list() {

		String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
		AppUser appUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

		List<CostDistribution> costDistributions = costDistributionRepository.findByCreatedByAndBasicStatusEnum(appUser.getAppUserId().toString(), BasicStatusEnum.OK);

		List<CostDistributionDTO> costDistributionDTOs = new ArrayList<>();
		for (CostDistribution costDistribution : costDistributions) {
			BeanMapper beanMapper = new BeanMapper();
			CostDistributionDTO costDistributionDTO = beanMapper.map(costDistribution, CostDistributionDTO.class);

			if (costDistribution.getCreatedBy() != null){
				AppUser createdBy = appUserRepository.findOneByAppUserId(UUID.fromString(costDistribution.getCreatedBy()));
				costDistributionDTO.setCreatedById(createdBy.getAppUserId());
			}

			List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByCostDistribution(costDistribution);

			List<CostDistributionItemDTO> costDistributionItemDTOs = new ArrayList<>();
			for (CostDistributionItem costDistributionItem : costDistributionItems) {
				CostDistributionItemDTO costDistributionItemDTO = costDistributionItemService.mapToDTO(costDistributionItem);
				costDistributionItemDTOs.add(costDistributionItemDTO);
			}
			costDistributionDTO.setCostDistributionItemDTOS(costDistributionItemDTOs);

			costDistributionDTOs.add(costDistributionDTO);
		}

		return new ResponseEntity<>(costDistributionDTOs, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/costdistributions/{cid}", method = RequestMethod.PUT)
	public ResponseEntity<?> updateCostDistribution(@PathVariable(value="cid") String cid, @RequestBody CostDistributionDTO costDistributionDTO) {
		CostDistribution costDistribution = null;
		
		if (costDistributionDTO != null){
			BeanMapper beanMapper = new BeanMapper();
			costDistribution = beanMapper.map(costDistributionDTO, CostDistribution.class);

			costDistribution.setBasicStatusEnum(BasicStatusEnum.OK);

			if (costDistributionDTO.getCreatedById() != null){
				costDistribution.setCreatedBy(appUserRepository.findById(costDistributionDTO.getCreatedById()).orElse(null).getAppUserId().toString());
			}
		}

		CostDistribution costDistributionNew = costDistributionRepository.save(costDistribution);

		return new ResponseEntity<UUID>(costDistributionNew.getCostDistributionId(), HttpStatus.OK);
	}

	@Transient
	@RequestMapping(value = "/costdistributions/{cid}", method = RequestMethod.DELETE)
	public ResponseEntity<?> updateCostDistribution(@PathVariable(value="cid") UUID cid) {
		CostDistribution costDistribution = costDistributionRepository.findById(cid).orElse(null);

		if (costDistribution == null){
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByCostDistribution(costDistribution);

		costDistributionItemRepository.deleteAll(costDistributionItems);

		costDistribution.setBasicStatusEnum(BasicStatusEnum.DELETED);
		costDistributionRepository.save(costDistribution);

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@RequestMapping(value = "/costdistributions", method = RequestMethod.POST)
	public ResponseEntity<?> insertCostDistribution(@RequestBody CostDistributionDTO costDistributionDTO) {
		CostDistribution costDistribution = null;
		
		if (costDistributionDTO != null){
			BeanMapper beanMapper = new BeanMapper();
			costDistribution= beanMapper.map(costDistributionDTO, CostDistribution.class);

			costDistribution.setBasicStatusEnum(BasicStatusEnum.OK);
//			String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
//			AppUser appUser = appUserRepository.findOneByEmail(loggedInUserName);
//
//			costDistribution.setCreatedBy(appUser);
		}

		CostDistribution newCostDistribution = costDistributionRepository.save(costDistribution);

	    return new ResponseEntity<UUID>(newCostDistribution.getCostDistributionId(), HttpStatus.OK);
	}

	@RequestMapping(value = "/costdistributions/complete", method = RequestMethod.POST)
	public ResponseEntity<?> insertCostDistributionNew(@RequestBody CostDistributionDTO costDistributionDTO) {

		BeanMapper beanMapper = new BeanMapper();

		CostDistribution costDistribution = new CostDistribution();
		costDistribution.setName(costDistributionDTO.getName());
		costDistribution.setBasicStatusEnum(BasicStatusEnum.OK);
		if (costDistribution.getCostDistributionId() == null){
			costDistribution.setCostDistributionId(UUID.randomUUID());
		}
		costDistribution = costDistributionRepository.save(costDistribution);

		for (CostDistributionItemDTO costDistributionItemDTO : costDistributionDTO.getCostDistributionItemDTOS()) {
			CostDistributionItem costDistributionItemNew = new CostDistributionItem();
			costDistributionItemNew.setPaymentPersonTypeEnum(costDistributionItemDTO.getPaymentPersonTypeEnum());
			costDistributionItemNew.setValue(new BigDecimal(costDistributionItemDTO.getValue()));
			costDistributionItemNew.setPayerId(costDistributionItemDTO.getPayerId());
			costDistributionItemNew.setCostDistribution(costDistribution);
			costDistributionItemNew.setCostDistributionItemTypeEnum(costDistributionItemDTO.getCostDistributionItemTypeEnum());
			costDistributionItemNew.setPosition(costDistributionItemDTO.getPosition());
			costDistributionItemNew.setCostDistributionItemId(UUID.randomUUID());
			costDistributionItemRepository.save(costDistributionItemNew);
		}

		CostDistributionDTO costDistributionDTOResult = beanMapper.map(costDistribution, CostDistributionDTO.class);
		if (costDistribution.getCreatedBy() != null){
			AppUser createdBy = appUserRepository.findOneByAppUserId(UUID.fromString(costDistribution.getCreatedBy()));
			costDistributionDTO.setCreatedById(createdBy.getAppUserId());
		}

		return new ResponseEntity<>(costDistributionDTOResult, HttpStatus.OK);
	}


}