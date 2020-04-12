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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.nextbill.domain.enums.FirebaseMessageType;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.MobileDevice;
import de.nextbill.domain.pojos.FirebaseMessage;
import de.nextbill.domain.pojos.FirebaseNotification;
import de.nextbill.domain.repositories.MobileDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FirebaseService {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseService.class);

    @Autowired
    private MobileDeviceRepository mobileDeviceRepository;

    @Value("${internal.firebaseToken}")
    private String firebaseToken;

    public void sendTextMessage(AppUser appUser, FirebaseMessageType type, String title, String message) throws RestClientException {
        sendMessage(appUser, type, title, message, null, null, null);
    }

    public void sendDataMessage(AppUser appUser, FirebaseMessageType type, String objectId, Object objectData, Map<String, String> otherData) throws RestClientException {
        sendMessage(appUser, type, null, null, objectId, objectData, otherData);
    }

    public void sendMessage(AppUser appUser, FirebaseMessageType type, String title, String message, String objectId, Object objectData, Map<String, String> otherData) throws RestClientException {
        RestTemplate restTemplate = new RestTemplate();

        if (firebaseToken == null || firebaseToken.equals("")){
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "key=" + firebaseToken);

        List<MobileDevice> mobileDevices = mobileDeviceRepository.findAllByAppUser(appUser);

        LocalDateTime localDateTime = LocalDateTime.now();
        localDateTime = localDateTime.minusMonths(2);
        Date date2MonthsBefore = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());

        for (MobileDevice mobileDevice : mobileDevices) {

            if (mobileDevice.getFirebaseToken() == null || mobileDevice.getFirebaseToken().equals("") || (mobileDevice.getCreatedDate() != null && mobileDevice.getCreatedDate().before(date2MonthsBefore)) || mobileDevice.getCreatedDate() == null){
                mobileDeviceRepository.delete(mobileDevice);
                continue;
            }

            FirebaseMessage firebaseMessage = new FirebaseMessage();
            firebaseMessage.setTo(mobileDevice.getFirebaseToken());

            if (title != null && message != null){
                FirebaseNotification firebaseNotification = new FirebaseNotification();
                firebaseNotification.setTitle(title);
                firebaseNotification.setBody(message);
                firebaseMessage.setNotification(firebaseNotification);
            }

            Map<String, String> dataValues = new HashMap<>();

            if (type != null){
                dataValues.put("messageType", type.name());
            }

            if (objectData != null){
                Gson gson = new GsonBuilder()
                        .setDateFormat("dd MMM yyyy HH:mm:ss").create();
                String jsonString = gson.toJson(objectData);

                dataValues.put("objectJson", jsonString);
            }

            if (objectId != null){
                dataValues.put("objectId", objectId);
            }

            if (otherData != null){
                dataValues.putAll(otherData);
            }

            if (dataValues.size() > 0){
                firebaseMessage.setData(dataValues);
            }

            Gson gson = new GsonBuilder()
                    .setDateFormat("dd MMM yyyy HH:mm:ss").create();
            byte[] jsonString = null;
            try {
                jsonString = gson.toJson(firebaseMessage).getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            HttpEntity<byte[]> requestEntity = new HttpEntity<>(jsonString, headers);

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://fcm.googleapis.com/fcm/send");
            try{
                restTemplate.postForEntity(builder.build().encode().toUri(), requestEntity, String.class).getBody();
            } catch (HttpServerErrorException e) {
                logger.info("Firebase server failure", e);
            } catch (HttpClientErrorException e){
                logger.info("Firebase client failure", e);
            }
        }
    }
}
