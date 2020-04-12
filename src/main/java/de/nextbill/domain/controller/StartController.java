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

import de.nextbill.domain.model.Settings;
import de.nextbill.domain.services.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class StartController {

    @Value("${security.xsrfTokenName}")
    private String xsrfTokenName;

    @Value("${build.version}")
    private String buildVersion;

    @Autowired
    private SettingsService settingsService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String start(Model model) {
       model.addAttribute("xsrfTokenName", xsrfTokenName);
       model.addAttribute("buildVersion", buildVersion);
       return "app";
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(Model model) {

        Settings settings = settingsService.getCurrentSettings();

        if (!settings.getIsCustomized()){
            return "redirect:/public/?fullscreen#/setup";
        }

        model.addAttribute("xsrfTokenName", xsrfTokenName);
        model.addAttribute("buildVersion", buildVersion);
        model.addAttribute("usePasswordForgotMail", settings.getSmtpMailServiceEnabled());
        return "login";
    }


    @RequestMapping(value = "/public", method = RequestMethod.GET)
    public String publicEndpoint(Model model) {
        model.addAttribute("xsrfTokenName", xsrfTokenName);
        model.addAttribute("buildVersion", buildVersion);
        return "app";
    }
}
