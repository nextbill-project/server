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

import org.apache.commons.lang3.SystemUtils;
import org.springframework.core.io.ClassPathResource;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;

public class DesktopService {

    private static TrayIcon trayIcon;

    public static void sendTaskBarMessage(String subject, String text) {
        boolean isUnix = SystemUtils.IS_OS_LINUX;
        if (!isUnix && trayIcon != null) {
            trayIcon.displayMessage(subject, text, TrayIcon.MessageType.INFO);
        }
    }

    public static void initTaskBar() {

        boolean isUnix = SystemUtils.IS_OS_LINUX;
        if (isUnix) {
            return;
        }

        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (UnsupportedLookAndFeelException ex) {

        } catch (IllegalAccessException ex) {

        } catch (InstantiationException ex) {

        } catch (ClassNotFoundException ex) {

        }

        UIManager.put("swing.boldMetal", Boolean.FALSE);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    createAndShowGUI();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static void createAndShowGUI() throws IOException {

        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            return;
        }
        final PopupMenu popup = new PopupMenu();
        trayIcon = new TrayIcon(createImage("tray icon"));
        final SystemTray tray = SystemTray.getSystemTray();

        MenuItem browserItem = new MenuItem("Öffnen");
        MenuItem aboutItem = new MenuItem("Über NextBill");
        MenuItem exitItem = new MenuItem("Beenden");

        popup.add(browserItem);
        popup.addSeparator();
        popup.add(aboutItem);
        popup.add(exitItem);

        trayIcon.setPopupMenu(popup);
        trayIcon.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 && e.getButton() != 3) {
                    openBrowser();
                }
            }
        });

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.out.println("TrayIcon could not be added.");
            return;
        }

        browserItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openBrowser();
            }
        });

        aboutItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null,
                        "NextBill 1.1");
            }
        });

        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tray.remove(trayIcon);
                System.exit(0);
            }
        });
    }

    protected static Image createImage(String description) throws IOException {
        URL imageURL = new ClassPathResource("money.png").getURL();

        if (imageURL == null) {
            System.err.println("Resource not found");
            return null;
        } else {
            return (new ImageIcon(imageURL, description)).getImage();
        }
    }

    public static void openBrowser() {
        String url = "http://localhost:8010";
        String os = System.getProperty("os.name").toLowerCase();
        Runtime rt = Runtime.getRuntime();

        try{
            if (os.indexOf( "win" ) >= 0) {
                rt.exec( "rundll32 url.dll,FileProtocolHandler " + url);
            } else if (os.indexOf( "mac" ) >= 0) {
                rt.exec( "open " + url);
            } else if (os.indexOf( "nix") >=0 || os.indexOf( "nux") >=0) {
                String[] browsers = {"epiphany", "firefox", "mozilla", "konqueror",
                        "netscape","opera","links","lynx"};
                StringBuffer cmd = new StringBuffer();
                for (int i=0; i<browsers.length; i++) {
                    cmd.append( (i==0  ? "" : " || " ) + browsers[i] +" \"" + url + "\" ");
                }
                rt.exec(new String[] { "sh", "-c", cmd.toString() });
            }
        }catch (Exception e){
        }
    }

}
