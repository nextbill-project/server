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

import de.nextbill.domain.dtos.AppRightDTO;
import de.nextbill.domain.dtos.AppUserDTO;
import de.nextbill.domain.dtos.UserContactDTO;
import de.nextbill.domain.enums.Right;
import de.nextbill.domain.exceptions.RightException;
import de.nextbill.domain.model.AppRight;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.Settings;
import de.nextbill.domain.model.UserContact;
import de.nextbill.domain.repositories.AppRightRepository;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.UserContactRepository;
import de.nextbill.domain.utils.BeanMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AppUserService {

    @Autowired
    private UserContactRepository userContactRepository;

    @Autowired
    private AppRightRepository appRightRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private UserContactService userContactService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SettingsService settingsService;

    public AppUserDTO mapToDTO(AppUser appUser){
        BeanMapper beanMapper = new BeanMapper();

        AppUserDTO appUserDTO = beanMapper.map(appUser, AppUserDTO.class);
        appUserDTO.setAppUserPassword(null);

        List<UserContact> userContactList = userContactRepository.findAllByAppUserContact(appUser);

        List<UserContactDTO> userContactDTOs = new ArrayList<>();
        for (UserContact userContact : userContactList) {
            userContactDTOs.add(userContactService.mapToDTO(userContact));
        }
        appUserDTO.setUserContactDTOs(userContactDTOs);

        Set<AppRight> appRights = appUser.getUserRights();

        Settings settings = settingsService.getCurrentSettings();
        List<AppRightDTO> appRightDTOs = new ArrayList<>();
        for (AppRight appRight : appRights) {
            if (appRight.getCode().equals(Right.OCR)){
                if (settings.getScansioEnabled()) {
                    appRightDTOs.add(beanMapper.map(appRight, AppRightDTO.class));
                }
            }else{
                appRightDTOs.add(beanMapper.map(appRight, AppRightDTO.class));
            }
        }
        appUserDTO.setAppRightDTOs(appRightDTOs);

        appUserDTO.setEnabled(appRightDTOs.stream().anyMatch(t -> t.getCode().equals(Right.ENABLED)));
        appUserDTO.setUseAndroid(appRightDTOs.stream().anyMatch(t -> t.getCode().equals(Right.CAN_USE_ANDROID)));
        appUserDTO.setEditUsers(appRightDTOs.stream().anyMatch(t -> t.getCode().equals(Right.EDIT_USERS)));
        appUserDTO.setEditSettings(appRightDTOs.stream().anyMatch(t -> t.getCode().equals(Right.EDIT_SETTINGS)));

        if (settings.getScansioEnabled()){
            appUserDTO.setUseOcr(appRightDTOs.stream().anyMatch(t -> t.getCode().equals(Right.OCR)));
        }else{
            appUserDTO.setUseOcr(false);
        }


        return appUserDTO;
    }

    public AppUser mapToEntity(AppUser currentUser, AppUserDTO appUserDTO) throws RightException {

        AppUser appUser = appUserRepository.findById(appUserDTO.getAppUserId()).orElse(null);

        BeanMapper beanMapper = new BeanMapper();

        AppUser appUserNew = beanMapper.map(appUserDTO, AppUser.class);

        Boolean currentUserHasUsersRight = currentUser.getUserRights().stream()
                .anyMatch(t -> t.getCode().equals(Right.EDIT_USERS));

        if (!currentUserHasUsersRight) {
            throw new RightException("Not enough rights!");
        }

        if (StringUtils.isEmpty(appUserNew.getAppUserPassword())) {
            if (appUser != null){
                appUserNew.setAppUserPassword(appUser.getAppUserPassword());
            }else{
                throw new RightException("No password set!");
            }
        }else{
            appUserNew.setAppUserPassword(passwordEncoder.encode(appUserNew.getAppUserPassword()));
        }

        appUserNew.setUserRights(convertToRights(currentUser, appUser, appUserDTO));

        return appUserNew;
    }

    public Set<AppRight> convertToRights(AppUser currentUser, AppUser appUser, AppUserDTO appUserDTO) throws RightException {

        AppRight appRightEnabled = appRightRepository.findByCode(Right.ENABLED);

        Set<AppRight> appRights = new HashSet<>();

        if (appUserDTO.getUseAndroid()){
            appRights.add(appRightRepository.findByCode(Right.CAN_USE_ANDROID));
        }

        if (appUserDTO.getUseOcr()){
            appRights.add(appRightRepository.findByCode(Right.OCR));
        }

        AppRight appRightSettings = appRightRepository.findByCode(Right.EDIT_SETTINGS);
        List<AppUser> appUsersWithSettingsEditRight = appUserRepository.findAllByUserRightsContainsAndUserRightsContainsAndAppUserIdIsNot(appRightSettings,appRightEnabled,  appUserDTO.getAppUserId());
        
        Boolean currentUserHasSettingsRight = currentUser.getUserRights().stream()
                .anyMatch(t -> t.getCode().equals(Right.EDIT_SETTINGS));

        Boolean appUserHasSettingsRight = appUser != null && appUser.getUserRights().stream()
                .anyMatch(t -> t.getCode().equals(Right.EDIT_SETTINGS));
        
        if (currentUserHasSettingsRight){
            if (appUserHasSettingsRight && !appUserDTO.getEditSettings()){
                if (appUsersWithSettingsEditRight.isEmpty()) {
                    throw new RightException("Mindestens ein Benutzer muss das Recht für die Konfiguration haben.");
                }
            }else{
                if (appUserDTO.getEditSettings()) {
                    appRights.add(appRightRepository.findByCode(Right.EDIT_SETTINGS));
                }
            }
        }

        AppRight appRightUsers = appRightRepository.findByCode(Right.EDIT_USERS);
        List<AppUser> appUsersWithUsersEditRight = appUserRepository.findAllByUserRightsContainsAndUserRightsContainsAndAppUserIdIsNot(appRightUsers, appRightEnabled, appUserDTO.getAppUserId());

        Boolean currentUserHasUsersRight = currentUser.getUserRights().stream()
                .anyMatch(t -> t.getCode().equals(Right.EDIT_USERS));

        Boolean appUserHasUsersRight = appUser != null &&appUser.getUserRights().stream()
                .anyMatch(t -> t.getCode().equals(Right.EDIT_USERS));

        if (currentUserHasUsersRight){
            if (appUserHasUsersRight && !appUserDTO.getEditUsers()){
                if (appUsersWithUsersEditRight.isEmpty()) {
                    throw new RightException("Mindestens ein Benutzer muss das Recht für die Benutzerverwaltung haben.");
                }
            }else{
                if (appUserDTO.getEditUsers()) {
                    appRights.add(appRightRepository.findByCode(Right.EDIT_USERS));
                }
            }
        }

        appUsersWithUsersEditRight = appUserRepository.findAllByUserRightsContainsAndUserRightsContainsAndAppUserIdIsNot(appRightUsers, appRightEnabled, appUserDTO.getAppUserId());

        if (currentUserHasUsersRight){
            if (appUserHasUsersRight && !appUserDTO.getEnabled()){
                if (appUsersWithUsersEditRight.isEmpty()) {
                    throw new RightException("Mindestens ein Benutzer muss aktiv sein und das Recht für die Benutzerverwaltung haben.");
                }
            }else{
                if (appUserDTO.getEnabled()) {
                    appRights.add(appRightRepository.findByCode(Right.ENABLED));
                }
            }
        }

        return appRights;
    }
}
