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

package de.nextbill;

import de.nextbill.domain.services.DesktopService;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;

@EnableJpaAuditing(auditorAwareRef="auditorProvider")
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class Application {

    public static void main(String[] args) {
        boolean isUnix = SystemUtils.IS_OS_LINUX;

        if (!isUnix){
            DesktopService.initTaskBar();
        }

        new SpringApplicationBuilder().sources(Application.class).headless(isUnix).run(args);
    }

    @PostConstruct
    public void init(){
        boolean isUnix = SystemUtils.IS_OS_LINUX;
        if (!isUnix){
            DesktopService.sendTaskBarMessage("NextBill", "WebService gestartet.\nBeenden bitte über die Taskleiste.");
        }
    }

}
