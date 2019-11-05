package com.maxprograms.mvdserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import com.maxprograms.converters.Utils;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import org.json.JSONObject;

public class MVDServer {

    private static Logger logger = System.getLogger(MVDServer.class.getName());

    private HttpServer webServer;
    private HttpsServer secureServer;

    private int httpPort = 8080;
    private int httpsPort = -1;
    private String hostName = "";
    private String keystore = "";
    private String password = "";
    private String stopWord = "";
    private File webDir;

    public static void main(String[] args) {
        try {
            MVDServer instance = new MVDServer(args);
            instance.run();
        } catch (IOException | UnrecoverableKeyException | KeyManagementException | KeyStoreException
                | NoSuchAlgorithmException | CertificateException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    private void setWebDir(String dir) throws IOException {
        webDir = new File(dir);
        if (!webDir.exists()) {
            Files.createDirectories(webDir.toPath());
        }
    }

    private void loadConfig(String file) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (FileInputStream stream = new FileInputStream(new File(file))) {
            try (InputStreamReader reader = new InputStreamReader(stream)) {
                try (BufferedReader buffer = new BufferedReader(reader)) {
                    String line = buffer.readLine();
                    while (line != null) {
                        builder.append(line);
                        builder.append('\n');
                        line = buffer.readLine();
                    }
                }
            }
        }
        JSONObject config = new JSONObject(builder.toString());
        if (config.has("httpPort")) {
            httpPort = config.getInt("httpPort");
        }
        if (config.has("httpsPort")) {
            httpsPort = config.getInt("httpsPort");
        }
        if (config.has("keystore")) {
            keystore = config.getString("keystore");
        }
        if (config.has("password")) {
            password = config.getString("password");
        }
        if (config.has("webDir")) {
            setWebDir(config.getString("webDir"));
        }
        if (config.has("hostName")) {
            hostName = config.getString("hostName");
        }
        if (config.has("stopWord")) {
            stopWord = config.getString("stopWord");
        }
    }

    public MVDServer(String[] args) throws IOException, KeyStoreException, UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyManagementException, CertificateException {
        String[] params = Utils.fixPath(args);

        for (int i = 0; i < params.length; i++) {
            String param = params[i];
            if (param.equals("-help")) {
                help();
                System.exit(0);
            }
            if (param.equals("-version")) {
                logger.log(Level.INFO, () -> "Version: " + Constants.VERSION + " Build: " + Constants.BUILD);
                System.exit(0);
            }
            if (param.equals("-config") && (i + 1) < params.length) {
                loadConfig(params[i + 1]);
            }
        }

        webServer = HttpServer.create(new InetSocketAddress(httpPort), 0);
        webServer
                .setExecutor(new ThreadPoolExecutor(4, 8, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100)));

        if (!keystore.isEmpty() && !password.isEmpty() && httpsPort != -1) {
            KeyStore store = KeyStore.getInstance("JKS");
            try (FileInputStream inputStream = new FileInputStream(keystore)) {
                store.load(inputStream, password.toCharArray());
            }

            KeyManagerFactory keyManager = KeyManagerFactory.getInstance("SunX509");
            keyManager.init(store, password.toCharArray());

            TrustManagerFactory trustManager = TrustManagerFactory.getInstance("SunX509");
            trustManager.init(store);

            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(keyManager.getKeyManagers(), trustManager.getTrustManagers(), new SecureRandom());

            secureServer = HttpsServer.create(new InetSocketAddress(httpsPort), 0);
            secureServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
            secureServer.createContext("/", new FileHandler(this));
            secureServer.setExecutor(
                    new ThreadPoolExecutor(4, 8, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100)));

            logger.log(Level.INFO, "HTTPS Server created");
            webServer.createContext("/", new RedirectHandler(this));
        } else {
            webServer.createContext("/", new FileHandler(this));
        }
    }

    private static void help() {
        String launcher = "    server.sh ";
        if (File.separator.equals("\\")) {
            launcher = "   server.bat ";
        }
        String help = "Usage:\n\n" + launcher + "[-help] [-version] -config config.json\n"
                + "              [-workDir workDirectory] [-stopWord word]\n " + "Where:\n\n"
                + "   -help:      (optional) Display this help information and exit\n"
                + "   -version:   (optional) Display version & build information and exit\n"
                + "   -config:    Load configuration from JSON file\n";
        System.out.println(help);
    }

    private void run() throws IOException {
        if (secureServer != null) {
            secureServer.start();
            logger.log(Level.INFO, "HTTPS Server started");
        }
        webServer.start();
        logger.log(Level.INFO, "Server started");
    }

    protected File getWebDir() {
        if (webDir == null) {
            webDir = new File(System.getProperty("user.dir") + File.separator + "www");
        }
        return webDir;
    }

    protected void stopServer(String word) {
        if (!stopWord.isEmpty() && stopWord.equals(word)) {
            logger.log(Level.INFO, "Stopping server");
            System.exit(0);
        }
    }

    protected String getHostName() {
        return hostName;
    }
}