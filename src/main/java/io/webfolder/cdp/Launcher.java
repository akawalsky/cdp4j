/**
 * The MIT License
 * Copyright © 2017 WebFolder OÜ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.webfolder.cdp;

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.nio.file.Paths.get;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Locale.ENGLISH;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import io.webfolder.cdp.exception.CdpException;
import io.webfolder.cdp.session.SessionFactory;
import io.webfolder.cdp.session.SessionInfo;

public class Launcher {

    private final SessionFactory factory;

    public Launcher() {
        this(new SessionFactory());
    }

    public Launcher(final SessionFactory factory) {
        this.factory = factory;
    }

    public String findChrome() {
        String os = getProperty("os.name")
                        .toLowerCase(ENGLISH);
        boolean windows = os.startsWith("windows");
        if (windows) {
            try {
                // Chrome Canary
                Process pcanary = getRuntime().exec(new String[] {
                        "cmd",
                        "/c",
                        "echo",
                        "%localappdata%\\Google\\Chrome SxS\\Application\\chrome.exe"
                });
                int ecanary = pcanary.waitFor();
                if (ecanary == 0) {
                    String canary = toString(pcanary.getInputStream()).trim().replace("\"", "");
                    File executableCanary = new File(canary);
                    if (executableCanary.exists() && executableCanary.canExecute()) {
                        return executableCanary.toString();
                    }
                }
                // Chrome
                Process pchrome = getRuntime().exec(new String[] {
                        "cmd",
                        "/c",
                        "echo",
                        "%programfiles%\\Google\\Chrome\\Application\\chrome.exe"
                });
                int echrome = pchrome.waitFor();
                if (echrome == 0) {
                    String chrome = toString(pchrome.getInputStream()).trim().replace("\"", "");
                    File executable = new File(chrome);
                    if (executable.exists() && executable.canExecute()) {
                        return executable.toString();
                    }
                }
                // Chrome x86
                Process pchrome86 = getRuntime().exec(new String[] {
                        "cmd",
                        "/c",
                        "echo",
                        "%programfiles(x86)%\\Google\\Chrome\\Application\\chrome.exe"
                });
                int echrome86 = pchrome86.waitFor();
                if (echrome86 == 0) {
                    String chromex86 = toString(pchrome86.getInputStream()).trim().replace("\"", "");
                    File executable86 = new File(chromex86);
                    if (executable86.exists() && executable86.canExecute()) {
                        return executable86.toString();
                    }
                }
                throw new CdpException("Unable to find chrome.exe");
            } catch (Throwable e) {
                // ignore
            }
        } else {
            return "google-chrome";
        }
        return null;
    }

    
    public SessionFactory launch() {
        return launch(new String[] { });
    }

    public SessionFactory launch(String... arguments) {
        if (launched()) {
            return factory;
        }
        String chromePath = findChrome();
        Path remoteProfileData = get(getProperty("java.io.tmpdir"))
                                        .resolve("remote-profile");
        List<String> list = new ArrayList<>();
        list.add(chromePath);
        list.add(format("--remote-debugging-port=%d", factory.getPort()));
        list.add(format("--user-data-dir=%s", remoteProfileData.toString()));
        list.add(format("--remote-debugging-address=%s", factory.getHost()));
        list.add("--disable-translate");
        list.add("--disable-extensions");
        list.add("--disable-plugin-power-saver");
        list.add("--disable-sync");
        list.add("--no-first-run");
        list.add("--safebrowsing-disable-auto-update");
        list.add("--disable-popup-blocking");
        if (arguments != null) {
            list.addAll(asList(arguments));
        }
        try {
            Process process = getRuntime().exec(list.toArray(new String[0]));
            process.getOutputStream().close();
            process.getInputStream().close();
        } catch (IOException e) {
            throw new CdpException(e);
        }
        return factory;
    }

    protected String toString(InputStream is) {
        try (Scanner scanner = new Scanner(is)) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    public boolean launched() {
        List<SessionInfo> list = emptyList();
        try {
            list = factory.list();
        } catch (Throwable t) {
            // ignore
        }
        return ! list.isEmpty() ? true : false;
    }
}
