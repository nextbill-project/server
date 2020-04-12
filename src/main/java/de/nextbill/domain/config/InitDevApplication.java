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

package de.nextbill.domain.config;

import de.nextbill.domain.enums.Right;
import de.nextbill.domain.model.AppRight;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.repositories.AppRightRepository;
import de.nextbill.domain.repositories.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.UUID;

@Component
@Profile("dev")
public class InitDevApplication {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AppRightRepository appRightRepository;

    @PostConstruct
    public void init(){
        initRights();
        initAdminUser();
        initBatchUser();
        initDemoUser();
    }

    private void initRights(){

        for (Right value : Right.values()) {
            AppRight appRight = appRightRepository.findByCode(value);

            if (appRight == null) {
                appRight = new AppRight();
                appRight.setAppRightId(UUID.randomUUID());
            }
            appRight.setCode(value);

            appRightRepository.save(appRight);
        }
    }

    public void initBatchUser(){
        AppUser appUser = appUserRepository.findOneByEmail("BatchUser");
        if (appUser == null){
            appUser = new AppUser();
            appUser.setEmail("BatchUser");
            appUser.setAppUserName("BatchUser");
            appUser.setAppUserPassword(UUID.randomUUID().toString());
            appUser.setAppUserId(UUID.randomUUID());
            appUser.setUserRights(new HashSet<>(appRightRepository.findAll()));
            appUserRepository.save(appUser);
        }
    }

    public void initAdminUser(){
        AppUser appUser = appUserRepository.findOneByEmail("dev@nextbill.de");
        if (appUser == null) {
            appUser = new AppUser();
            appUser.setAppUserId(UUID.fromString("c1ec9fec-78d7-3574-a1db-22d82079e036"));
        }
        appUser.setEmail("dev@nextbill.de");
        appUser.setAppUserName("Dev User");
        appUser.setAppUserPassword(passwordEncoder.encode("12345"));
        appUser.setUserRights(new HashSet<>(appRightRepository.findAll()));

        appUserRepository.save(appUser);
    }

    public void initDemoUser(){
        AppUser appUser = appUserRepository.findOneByEmail("demo@nextbill.de");
        if (appUser == null) {
            appUser = new AppUser();
            appUser.setAppUserId(UUID.fromString("a2ad1fec-78d7-1574-a0db-22d82079a8da"));
        }
        appUser.setEmail("demo@nextbill.de");
        appUser.setAppUserName("Demo User");
        appUser.setAppUserPassword(passwordEncoder.encode("12345"));
        appUser.setUserRights(new HashSet<>(appRightRepository.findAll()));

        appUserRepository.save(appUser);
    }

}
