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

import de.nextbill.domain.model.Settings;
import de.nextbill.domain.services.PathService;
import de.nextbill.domain.services.SettingsService;
import de.nextbill.domain.utils.AuditorAwareImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.servlet.MultipartConfigElement;
import java.io.IOException;
import java.security.SecureRandom;

@Configuration
public class GeneralConfig {

    @Autowired
    private PathService pathService;

    @Autowired
    private SettingsService settingsService;

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }

    @Bean
    public TaskScheduler taskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() throws IOException {

        String path = "/tmp";
        if (settingsService.areSettingsInitialized()) {
            path = pathService.getTempPath(null).getAbsolutePath();
        }

        MultipartConfigElement multipartConfigElement = new MultipartConfigElement(path, 90971520, 90971520, 1);
        return multipartConfigElement;
    }

    @Bean
    public PasswordEncoder passwordEncoder(SettingsService settingsService) {

        Settings settings = settingsService.getCurrentSettings();

        return new BCryptPasswordEncoder(10, new SecureRandom(settings.getSalt().getBytes()));
    }

}
