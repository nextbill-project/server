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

package de.nextbill.commons.mailmanager.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class Mail {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private final String CHARSET_UTF8 = "UTF-8";

    private final String MIMETYPE_TEXT_PLAIN = "text/plain";
    private final String MIMETYPE_TEXT_HTML = "text/html";

    private final String UNKNOWN_FILE_TYPE = "Attachment";

    private String messageId;
    private String imapFolder;

    private MailRecipient from;
    private MailRecipient sender;

    private List<MailRecipient> recipients = new ArrayList<>();
    private List<MailRecipient> ccs = new ArrayList<>();
    private List<MailRecipient> bccs = new ArrayList<>();
    private String subject;

    private Date sentDate;

    private String messageTextPlain;
    private String messageTextHtml;
    private boolean isMessageTextHtml;
    private boolean messageTextHtmlType = false;

    private List<File> attachments = new ArrayList<>();

    private String messageIdReplyTo;

    private boolean hasBeenSeen;

    public List<MailRecipient> getRecipients() {
        if (recipients == null) {
            recipients = new ArrayList<>();
        }
        return recipients;
    }

    private List<MailRecipient> getBccs() {
        if (bccs == null) {
            bccs = new ArrayList<>();
        }
        return bccs;
    }

    private List<MailRecipient> getCcs() {
        if (ccs == null) {
            ccs = new ArrayList<>();
        }
        return ccs;
    }

    public List<File> getAttachments() {
        if (attachments == null) {
            attachments = new ArrayList<>();
        }
        return attachments;
    }

    public Mail(MimeMessage message, String tempMailFiles) throws MessagingException, IOException {

        String[] msgID = message.getHeader("Message-ID");

        if (msgID != null && msgID.length == 1) {
            setMessageId(msgID[0]);
        }

        String[] msgReplyID = message.getHeader("In-Reply-To");

        if (msgReplyID != null && msgReplyID.length == 1) {
            setMessageIdReplyTo(msgReplyID[0]);
        }

        setImapFolder(message.getFolder().getFullName());


        MailRecipient tmpFrom = new MailRecipient();
        if (message.getFrom() != null) {
            InternetAddress addr = (InternetAddress) message.getFrom()[0];

            if (addr.getPersonal() != null && addr.getPersonal() != "") {
                tmpFrom.setName(addr.getPersonal());
            }

            if (addr.getAddress() != null && addr.getAddress() != "") {
                tmpFrom.setAddress(addr.getAddress());
            }
        }
        if (message.getSender() != null) {
            InternetAddress addr = (InternetAddress) message.getSender();

            if (addr.getPersonal() != null && addr.getPersonal() != "") {
                tmpFrom.setName(addr.getPersonal());
            }

            if (addr.getAddress() != null && addr.getAddress() != "") {
                tmpFrom.setAddress(addr.getAddress());
            }
        }

        setFrom(tmpFrom);

        MailRecipient tmpSender = new MailRecipient();
        if (message.getSender() != null) {
            InternetAddress addr = (InternetAddress) message.getSender();

            if (addr.getPersonal() != null && addr.getPersonal() != "") {
                tmpSender.setName(addr.getPersonal());
            }

            if (addr.getAddress() != null && addr.getAddress() != "") {
                tmpSender.setAddress(addr.getAddress());
            }
        }
        setSender(tmpSender);

        MailRecipient tmpRecipient;
        ArrayList<MailRecipient> tmpRecipients = new ArrayList<MailRecipient>();

        Address[] recAddrs = message.getAllRecipients();

        if (recAddrs != null) {
            for (int i = 0; i < recAddrs.length; i++) {

                InternetAddress addr = (InternetAddress) recAddrs[i];
                tmpRecipient = new MailRecipient();

                if (addr.getPersonal() != null && addr.getPersonal() != "") {
                    tmpRecipient.setName(addr.getPersonal());
                }

                if (addr.getAddress() != null && addr.getAddress() != "") {
                    tmpRecipient.setAddress(addr.getAddress());
                }

                tmpRecipients.add(tmpRecipient);
            }
        }

        setRecipients(tmpRecipients);

        MailRecipient tmpCcRecipient;
        ArrayList<MailRecipient> tmpCcRecipients = new ArrayList<MailRecipient>();

        Address[] recAddrsCc = message.getRecipients(Message.RecipientType.CC);

        if (recAddrsCc != null) {
            for (int i = 0; i < recAddrsCc.length; i++) {

                InternetAddress addr = (InternetAddress) recAddrsCc[i];
                tmpCcRecipient = new MailRecipient();

                if (addr.getPersonal() != null && addr.getPersonal() != "") {
                    tmpCcRecipient.setName(addr.getPersonal());
                }

                if (addr.getAddress() != null && addr.getAddress() != "") {
                    tmpCcRecipient.setAddress(addr.getAddress());
                }

                tmpCcRecipients.add(tmpCcRecipient);
            }
        }

        setCcs(tmpCcRecipients);

        MailRecipient tmpBccRecipient;
        ArrayList<MailRecipient> tmpBccRecipients = new ArrayList<MailRecipient>();

        Address[] recAddrsBcc = message.getRecipients(Message.RecipientType.BCC);

        if (recAddrsBcc != null) {
            for (int i = 0; i < recAddrsBcc.length; i++) {

                InternetAddress addr = (InternetAddress) recAddrsBcc[i];
                tmpBccRecipient = new MailRecipient();

                if (addr.getPersonal() != null && addr.getPersonal() != "") {
                    tmpBccRecipient.setName(addr.getPersonal());
                }

                if (addr.getAddress() != null && addr.getAddress() != "") {
                    tmpBccRecipient.setAddress(addr.getAddress());
                }

                tmpBccRecipients.add(tmpBccRecipient);
            }
        }

        setBccs(tmpBccRecipients);

        setSubject(message.getSubject());

        setHasBeenSeen(message.isSet(Flags.Flag.SEEN));

        setSentDate(message.getSentDate());

        Object mailContent = message.getContent();

        if (mailContent instanceof String) {
            String text = (String) mailContent;
            if (isSubString(message.getContentType(), MIMETYPE_TEXT_HTML)) {
                this.messageTextHtmlType = true;
                this.messageTextHtml = text;
            } else {
                this.messageTextPlain = text;
            }

        }else if (mailContent instanceof Multipart) {

            MimeMultipart multipart = (MimeMultipart) mailContent;
            ArrayList<File> tmpFiles = new ArrayList<File>();

            for (int i = 0; i < multipart.getCount(); i++) {

                Part part = multipart.getBodyPart(i);

                if (i == 0) {
                    if (part.getContent() instanceof String) {
                        String text = (String) part.getContent();

                        if (isSubString(part.getContentType(), MIMETYPE_TEXT_HTML)) {
                            messageTextHtmlType = true;
                            this.messageTextHtml = text;
                        } else {
                            this.messageTextPlain = text;
                        }
                    } else {

                        if (part.getContent() instanceof Multipart) {
                            Multipart subMultipart = (Multipart) part.getContent();

                            Part lastPart = subMultipart.getBodyPart(subMultipart.getCount() - 1);

                            if (lastPart.getContent() instanceof String) {
                                this.messageTextHtml = (String) lastPart.getContent();

                                if (isSubString(lastPart.getContentType(), MIMETYPE_TEXT_HTML)) {
                                    messageTextHtmlType = true;
                                }
                            }

                            for (int k = 0; k < subMultipart.getCount(); k++) {
                                Part currentPart = subMultipart.getBodyPart(k);

                                if (currentPart.getContent() instanceof String) {
                                    String text = (String) currentPart.getContent();

                                    if (isSubString(currentPart.getContentType(), MIMETYPE_TEXT_HTML)) {
                                        messageTextHtmlType = true;
                                        this.messageTextHtml = text;
                                    } else if (isSubString(currentPart.getContentType(), MIMETYPE_TEXT_PLAIN)) {
                                        this.messageTextPlain = text;
                                    }
                                }
                            }
                        }
                    }
                }

                if (part.getDisposition() != null) {

                    File tmpFile = new File(tempMailFiles + part.getFileName());

                    if (!tmpFile.exists()) {
                        try {

                            InputStream is = part.getInputStream();
                            FileOutputStream fos = new FileOutputStream(tmpFile);

                            byte[] readData = new byte[1024];

                            int r = is.read(readData);

                            while (r != -1) { // EOF
                                fos.write(readData, 0, r);
                                r = is.read(readData);
                            }

                            is.close();
                            fos.close();

                            tmpFiles.add(tmpFile);

                        } catch (Exception e) {
                            log.info("Error during file save: " + e.getMessage());
                        }
                    } else {
                        tmpFiles.add(tmpFile);
                    }

                } else {
                    if (part.getContent() instanceof String) {
                        if (part != multipart.getBodyPart(0)) {
                            if (isSubString(part.getContentType(), MIMETYPE_TEXT_HTML)) {
                                this.messageTextHtml = (String) part.getContent();
                                messageTextHtmlType = true;
                            }
                        }
                    }
                }

            }

            setAttachments(tmpFiles);
        }
    }

    public MimeMessage getAsMimeMessage(MimeMessage message) throws MessagingException, UnsupportedEncodingException {

        InternetAddress fromAddr;

        if (from != null) {
            InternetAddress senderAddr;

            if (from.getName() != null && !from.getName().trim().equals("")) {
                senderAddr = new InternetAddress(from.getAddress(), from.getName(), CHARSET_UTF8);
            }else {
                senderAddr = new InternetAddress(from.getAddress());
            }
            message.setFrom(senderAddr);
        }

        if (sender != null) {
            InternetAddress senderAddr;

            if (sender.getName() != null && !sender.getName().trim().equals("")) {
                senderAddr = new InternetAddress(sender.getAddress(), sender.getName(), CHARSET_UTF8);
            }else {
                senderAddr = new InternetAddress(sender.getAddress());
            }
            message.setSender(senderAddr);
        }

        for (MailRecipient receiver : getRecipients()) {

            InternetAddress currentReceiverAddr;

            if (receiver.getName() != null && !receiver.getName().equals("")) {
                currentReceiverAddr = new InternetAddress(receiver.getAddress(), receiver.getName());
            } else {
                currentReceiverAddr = new InternetAddress(receiver.getAddress());
            }

            Message.RecipientType type = Message.RecipientType.TO;
            message.addRecipient(type, currentReceiverAddr);
        }

        for (MailRecipient cc : getCcs()) {

            InternetAddress currentReceiverAddr;

            if (cc.getName() != null && !cc.getName().equals("")) {
                currentReceiverAddr = new InternetAddress(cc.getAddress(), cc.getName());
            } else {
                currentReceiverAddr = new InternetAddress(cc.getAddress());
            }

            Message.RecipientType type = Message.RecipientType.CC;
            message.addRecipient(type, currentReceiverAddr);
        }

        for (MailRecipient bcc : getBccs()) {

            InternetAddress currentReceiverAddr;

            if (bcc.getName() != null && !bcc.getName().equals("")) {
                currentReceiverAddr = new InternetAddress(bcc.getAddress(), bcc.getName());
            } else {
                currentReceiverAddr = new InternetAddress(bcc.getAddress());
            }

            Message.RecipientType type = Message.RecipientType.BCC;
            message.addRecipient(type, currentReceiverAddr);
        }

        if (subject != null) {
            message.setSubject(subject, CHARSET_UTF8);
        } else {
            message.setSubject("");
        }

        if (isMessageTextHtml) {
            if (getAttachments().isEmpty()) {

                Multipart multipart = new MimeMultipart();

                BodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setText(messageTextHtml);
                messageBodyPart.setContent(messageTextHtml, MIMETYPE_TEXT_HTML + "; charset=" + CHARSET_UTF8);

                multipart.addBodyPart(messageBodyPart);

                message.setContent(multipart);

            }else {

                Multipart multipart = new MimeMultipart();

                /* BodyPart for the message text */
                BodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setText(messageTextHtml);
                messageBodyPart.setContent(messageTextHtml, MIMETYPE_TEXT_HTML + "; charset=" + CHARSET_UTF8);

                multipart.addBodyPart(messageBodyPart);

                for (File file : getAttachments()) {
                    BodyPart currentBodyPart = new MimeBodyPart();

                    FileDataSource fds = new FileDataSource(file);
                    currentBodyPart.setDataHandler(new DataHandler(fds));
                    currentBodyPart.setFileName(fds.getName());

                    multipart.addBodyPart(currentBodyPart);
                }

                message.setContent(multipart);
            }

        }else{

            if (getAttachments().isEmpty()) {
                message.setText(messageTextPlain, CHARSET_UTF8);
            }else {

                Multipart multipart = new MimeMultipart();

                BodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setText(messageTextPlain);
                multipart.addBodyPart(messageBodyPart);

                BodyPart currentBodyPart = new MimeBodyPart();

                for (File file : getAttachments()) {
                    FileDataSource fds = new FileDataSource(file);
                    currentBodyPart.setDataHandler(new DataHandler(fds));
                    currentBodyPart.setFileName(fds.getName());

                    multipart.addBodyPart(currentBodyPart);
                }

                message.setContent(multipart);
            }
        }

        message.setSentDate(new Date());
        return message;
    }

    private boolean isSubString(String str, String subStr) {
        return str.toLowerCase().contains(subStr.toLowerCase());
    }

    @Override
    public String toString() {
        return "Mail [messageId=" + messageId + ", imapFolder=" + imapFolder + ", sender=" + from + ", recipients=" + recipients + ", subject=" + subject + ", sentDate=" + sentDate + ", isMessageTextHtml=" + messageTextHtmlType + ", attachments=" + attachments + "]";
    }

    public String infos() {
        return "Mail [recipients=" + recipients + ", subject=" + subject + ", sentDate=" + sentDate + "]";
    }

}
