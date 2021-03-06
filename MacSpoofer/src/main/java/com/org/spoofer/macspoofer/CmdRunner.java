/**
 * Copyright (C) 2012-2013 Iakovos Gurulian
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.org.spoofer.macspoofer;

import android.net.wifi.WifiManager;

import com.stericson.RootTools.RootTools;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author jack gurulian
 */
public class CmdRunner {

    private static final int BUFF_LEN = 64;
    private Process p;
    private DataOutputStream stdin;
    protected InputStream stdout;
    private byte[] buffer = new byte[BUFF_LEN];
    protected WifiManager wifi;

    /**
     * Changes the mac address for a given interface
     *
     * @param newMac     the new mac address
     * @param iface      the interface
     * @param methodUsed 0 if ifconfig method is used, 1 if MAC file method
     */
    public void changeMac(String newMac, String iface, int methodUsed, WifiManager wifi) {

        this.wifi = wifi;

        if (methodUsed == 0) {
            String command = "busybox ifconfig " + iface + " up; busybox ifconfig "
                    + iface + " hw ether " + newMac;

            try {
                stdin.writeBytes(command + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (methodUsed == 1) {
            String macFile = getMacDir();
            if (macFile != null) {
                wifi.setWifiEnabled(false);
                changeMacAddr(newMac, macFile);
                wifi.setWifiEnabled(true);
            }
        }

    }

    private void changeMacAddr(String newMac, String file) {
        String command = "echo " + newMac.trim() + " > " + file;

        try {
            stdin.writeBytes(command + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Looks for files that may contain the MAC address and if they exist they are returned
     *
     * @return the directory where the MAC address is at or null if not found
     */
    public String getMacDir() {
        String[] filenames = {"/efs/wifi/.mac.info"};

        int i = 0;
        while (i < filenames.length) {
            File file = new File(filenames[i]);
            if (file.exists())
                return filenames[i];
            i++;
        }
        return null;
    }

    /**
     * Keeps a backup of the MAC address the first time the program runs
     * <p/>
     * The file is stored as a .bak file in the directory where
     * the original file is at, so that it can be restored even
     * if the user uninstalls the application or clears cache.
     * Not 100% safe, but as safe as it can be.
     */
    public void saveMac() {
        String fileName = getMacDir();
        String fileBak = fileName + ".bak";

        if (fileName != null) {
            File file = new File(fileBak);
            if (!file.exists()) {
                String command = "cp " + fileName + " " + fileBak;

                try {
                    stdin.writeBytes(command + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void restoreMac() {
        String fileName = getMacDir();
        String fileBak = fileName + ".bak";

        if (fileName != null) {
            String command = "cp " + fileBak + " " + fileName;

            try {
                stdin.writeBytes(command + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks if the user has busybox installed
     *
     * @return true if busybox is present
     */
    public boolean checkBusybox() {
        return RootTools.isBusyboxAvailable();
    }

    /**
     * Checks if the user has a rooted device
     *
     * @return true if root is present
     */
    public boolean checkRoot() {

        return RootTools.isRootAvailable();
    }

    /**
     * Checks if root access is given
     *
     * @return true if access is given
     */
    public boolean checkAccess() {

        return RootTools.isAccessGiven();
    }

    /**
     * Returns the current mac address
     *
     * @param iface the interface for which we will get the mac address
     * @return the current mac address
     */
    public String getCurrentMac(String iface) {

        String mac = "";

        String command = "busybox ip link show " + iface
                + " | sed -n 2p | tr -s ' ' | cut -d ' ' -f3";

        try {
            stdin.writeBytes(command + "\n");
            stdout = p.getInputStream();
            int read;
            while (true) {
                read = stdout.read(buffer);
                mac += new String(buffer, 0, read);
                if (read < BUFF_LEN) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mac;
    }

    /**
     * Get superuser access
     */
    public void getRoot() {
        try {
            p = Runtime.getRuntime().exec(new String[]{"su"});
            stdin = new DataOutputStream(p.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * !HIGHLY EXPERIMENTAL FEATURE! sed command has only been tested on a Linux system. It may have
     * a different behaviour on an Android device.
     * <p/>
     * nvram.txt contains the device's MAC address. It can be replaced using sed, it has not been
     * tested yet though.
     * <p/>
     * Warning: modifying nvram.txt may cause issues on the device (ex. the device's language being
     * reset to a default value).
     * <p/>
     * USE CAREFULLY!
     *
     * @param currentMac The MAC address which is currently being used
     * @param newMac     The MAC address that will be set
     * @param file       The file that contains the MAC address (nvram.txt)
     */
    public void changeMacWithSed(String currentMac, String newMac, String file) {
        String command = "sed -i.tmpBk s/" + currentMac.trim() + "/" + newMac.trim() + "/g " + file;

        try {
            stdin.writeBytes(command + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * !HIGHLY EXPERIMENTAL FEATURE! sed command has only been tested on a Linux system. It may have
     * a different behaviour on an Android device.
     * <p/>
     * In case of a MAC address spoof failure, this method might help
     *
     * @param newMac The MAC address that will be set
     * @param file   The file that contains the MAC address (nvram.txt)
     */
    public void changeAnyMac(String newMac, String file) {
        String command = "sed -i.tmpBk s/..:..:..:..:..:../" + newMac.trim() + "/g " + file;

        try {
            stdin.writeBytes(command + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}