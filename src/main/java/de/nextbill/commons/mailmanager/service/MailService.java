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

package de.nextbill.commons.mailmanager.service;

import com.sun.mail.imap.IMAPFolder;
import de.nextbill.commons.mailmanager.model.Mail;
import de.nextbill.commons.mailmanager.model.MailRecipient;
import de.nextbill.domain.model.Settings;
import de.nextbill.domain.services.PathService;
import de.nextbill.domain.services.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import javax.mail.search.HeaderTerm;
import javax.mail.search.SearchTerm;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

@Service
public class MailService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static final String PATH_TO_TEMPLATES = "/mail-templates/";

    @Autowired
    private PathService pathService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private ApplicationContext applicationContext;

    public void sendMail(Mail mail) throws SendFailedException {
        Settings settings = settingsService.getCurrentSettings();
        sendMail(mail, settings.getSmtpMailServiceEnabled(), settings.getSmtpServer(), settings.getSmtpEmail(), settings.getSmtpUser(), settings.getSmtpPassword());
    }

    public void sendMail(Mail mail, boolean smtpMailServiceEnabled, String smtpServer, String smtpMail, String smtpUser, String smtpPassword) throws SendFailedException {

        if (!smtpMailServiceEnabled) {
            log.warn("Sending mail disabled. Would send mail with subject " + mail.getSubject());
            return;
        }

        try {
            if (mail.getFrom() == null){
                mail.setFrom(MailRecipient.builder().address(smtpMail).name("NextBill").build());
            }

            Authenticator authentificator = new MailAuthenticator(smtpUser, smtpPassword);
            Session session = Session.getInstance(createProperties(false, true, null, smtpServer), authentificator);

            MimeMessage preparedMimeMessage = new MimeMessage(session);

            Message mm;

            mm = mail.getAsMimeMessage(preparedMimeMessage);
            mm.setHeader("User-Agent", "NextBill MailService");

            session.getTransport("smtp");
            Transport.send(mm);

            String[] msgID = mm.getHeader("Message-ID");

            log.info("Mail sent! Message-Id: " + msgID[0]);
            log.info("# " + mail.infos());

        } catch(SendFailedException e) {
            throw e;
        } catch(Exception e) {
            throw  new MailDeliveryException(e);
        }

    }

    public ArrayList<Mail> getAllMailsFromFolder() throws MessagingException, IOException {

        Settings settings = settingsService.getCurrentSettings();

        if (!settings.getImapMailServiceEnabled()) {
            return new ArrayList<>();
        }

        ArrayList<Mail> mails = new ArrayList<>();

        Store store = null;
        try {
            try {
                Session session = Session.getDefaultInstance(new Properties());

                store = session.getStore("imaps");
                store.connect(settings.getImapServer(), settings.getImapUser(), settings.getImapPassword());

                IMAPFolder folder = (IMAPFolder) store.getFolder(settings.getImapPath());

                if (!folder.exists()) {
                    folder.create(Folder.HOLDS_MESSAGES);
                }

                folder.open(Folder.READ_WRITE);

                Message message[] = folder.getMessages();
                for (int i = 0; i < message.length; i++) {

                    MimeMessage tmpMsg = (MimeMessage) message[i];
                    Mail tmpMail = new Mail(tmpMsg, pathService.getTempPath(null).getAbsolutePath());
                    tmpMail.setImapFolder(settings.getImapPath());

                    mails.add(tmpMail);
                }

                folder.close(false);
            } catch (IOException e) {
                e.printStackTrace();
                throw new IOException(e.getMessage());

            } catch (javax.mail.MessagingException e) {
                throw new javax.mail.MessagingException(e.getMessage());
            }
        } finally {
            try {
                store.close();
            } catch (javax.mail.MessagingException e) {
                e.printStackTrace();
            }
        }

        return new ArrayList<>(mails);
    }

    public boolean deleteMail(String messageID) throws javax.mail.MessagingException {

        Session session = Session.getDefaultInstance(new Properties());
        Folder folder = null;

        Store store = null;
        try {
            Settings settings = settingsService.getCurrentSettings();

            store = session.getStore("imaps");
            store.connect(settings.getImapServer(), settings.getImapUser(), settings.getImapPassword());

            folder = store.getFolder(settings.getImapPath());

            SearchTerm st = new HeaderTerm("Message-ID", messageID);
            folder.open(Folder.READ_WRITE);

            Message msg[] = folder.search(st);

            if (msg.length < 1) {
                return false;
            }

            if (msg.length >= 1) {

                for (int i = 0; i < msg.length; i++) {
                    msg[i].setFlag(Flags.Flag.DELETED, true);
                }
            }

        } catch (javax.mail.MessagingException e) {
            throw new javax.mail.MessagingException(e.getMessage());
        } finally {
            try {
                folder.close(true);
                store.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    public boolean setMailRead(String messageID) throws javax.mail.MessagingException {

        Session session = Session.getDefaultInstance(new Properties());
        Folder folder = null;

        Store store = null;
        try {
            Settings settings = settingsService.getCurrentSettings();

            store = session.getStore("imaps");
            store.connect(settings.getImapServer(), settings.getImapUser(), settings.getImapPassword());

            folder = store.getFolder(settings.getImapPath());

            SearchTerm st = new HeaderTerm("Message-ID", messageID);
            folder.open(Folder.READ_WRITE);

            Message msg[] = folder.search(st);

            if (msg.length < 1) {
                return false;
            }

            if (msg.length >= 1) {
                for (int i = 0; i < msg.length; i++) {
                    msg[i].setFlag(Flags.Flag.SEEN, true);
                }
            }
        } catch (javax.mail.MessagingException e) {
            throw new javax.mail.MessagingException(e.getMessage());
        } finally {
            try {
                folder.close(true);
                store.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return true;
    }

    public String generateMessageTemplate(String templateName,  Map<String, Object> variables) {

        if (variables == null){
            variables = new HashMap<>();
        }

        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setTemplateMode("HTML5");
        templateResolver.setPrefix("classpath:"+PATH_TO_TEMPLATES);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setSuffix(".html");
        templateResolver.setApplicationContext(applicationContext);
        templateResolver.setCacheable(false);

        StringWriter writer = new StringWriter();
        Context context = new Context(Locale.GERMAN, variables);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(templateResolver);
        engine.process(templateName, context, writer);

        return writer.toString();
    }

    private Properties createProperties(){
        Settings settings = settingsService.getCurrentSettings();
        return createProperties(settings.getImapMailServiceEnabled(), settings.getSmtpMailServiceEnabled(), settings.getImapServer(), settings.getSmtpServer());
    }

    private Properties createProperties(boolean imapMailServiceEnabled, boolean smtpMailServiceEnabled, String imapServer, String smtpServer){

        Properties properties = new Properties();

        if (imapMailServiceEnabled){
            properties.put("mail.host", imapServer);
            properties.put("mail.imap.socketFactory.port", "993");
        }

        if (smtpMailServiceEnabled){
            properties.put("mail.smtp.host", smtpServer);
        }

        properties.put("mail.smtp.socketFactory.port", "465");
        properties.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.ssl.checkserveridentity", "false");
        properties.put("mail.smtp.ssl.trust", "*");
        return properties;
    }

    public class MailAuthenticator extends Authenticator {
        private String user;
        private String password;

        public MailAuthenticator(String user, String password) {
            this.user = user;
            this.password = password;
        }

        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(user, password);
        }
    }
}
