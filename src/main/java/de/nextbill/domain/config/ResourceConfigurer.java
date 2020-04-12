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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.EncodedResourceResolver;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Controller
@SessionAttributes("authorizationRequest")
public class ResourceConfigurer implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/oauth/confirm_access").setViewName("authorize");
        registry.addViewController("/error").setViewName("error");
        registry.addViewController("/public/sendPassword").setViewName("sendPassword");
        registry.addViewController("/public/passwordReset").setViewName("passwordReset");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/public/**").addResourceLocations("classpath:/static/").setCachePeriod(3600).resourceChain(true).addResolver(new EncodedResourceResolver()).addResolver(new PathResourceResolver());
    }
}
