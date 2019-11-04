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

public class Server {

    private static Logger logger = System.getLogger(Server.class.getName());

    private HttpServer webServer;
    private int port = 8000;
    private String keystore = "";
    private String password = "";
    private String stopWord = "";
    private File webDir;

    public static void main(String[] args) {
        try {
            Server instance = new Server(args);
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
        if (config.has("port")) {
            port = config.getInt("port");
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
        if (config.has("stopWord")) {
            stopWord = config.getString("stopWord");
        }
    }

    public Server(String[] args) throws IOException, KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyManagementException, CertificateException {
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
            if (param.equals("-port") && (i + 1) < params.length) {
                port = Integer.parseInt(params[i + 1]);
            }
            if (param.equals("-keystore") && (i + 1) < params.length) {
                keystore = params[i + 1];
            }
            if (param.equals("-password") && (i + 1) < params.length) {
                password = params[i + 1];
            }
            if (param.equals("-webDir") && (i + 1) < params.length) {
                setWebDir(params[i + 1]);
            }
            if (param.equals("-stopWord")&& (i + 1) < params.length) {
                stopWord = params[i+1];
            }
        }

        if (!keystore.isEmpty() && !password.isEmpty()) {
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

            webServer = HttpsServer.create(new InetSocketAddress(port), 0);
            ((HttpsServer) webServer).setHttpsConfigurator(new HttpsConfigurator(sslContext));
            logger.log(Level.INFO, "HTTPS Server created");
        } else {
            webServer = HttpServer.create(new InetSocketAddress(port), 0);
        }
    }

    private static void help() {
        String launcher = "    server.sh ";
        if (File.separator.equals("\\")) {
            launcher = "   server.bat ";
        }
        String help = "Usage:\n\n" + launcher
                + "[-help] [-version] [-config config.json][-port portNumber] [-keystore storeLocation]\n"
                + "              [-password storePassword] [-webDir webDirectory]\n"
                + "              [-workDir workDirectory] [-stopWord word]\n " + "Where:\n\n"
                + "   -help:      (optional) Display this help information and exit\n"
                + "   -version:   (optional) Display version & build information and exit\n"
                + "   -config:    (optional) Load configuration from JSON file\n"
                + "   -port:      (optional) Port for running HTTP or HTTPS server. Default is 8000\n"
                + "   -keystore:  (optional) Java Keystore that contains SSL certificate for HTTPS\n"
                + "   -password:  (optional) Password for the Java Keystore\n"
                + "   -webDir:    (optional) Directory with web files to serve\n"
                + "   -stopWord:  (optional) Security word for stopping the server\n";
        System.out.println(help);
    }

    private void run() throws IOException {
        webServer.createContext("/", new FileHandler(this));
        webServer.setExecutor(new ThreadPoolExecutor(4, 8, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100)));
        webServer.start();
        logger.log(Level.INFO, "Server started");
    }

    public File getWebDir() {
        if (webDir == null) {
            webDir = new File(System.getProperty("user.dir") + File.separator + "www");
        }
        return webDir;
    }

    public void stopServer(String word) {
        if (!stopWord.isEmpty() && stopWord.equals(word)) {
            logger.log(Level.INFO, "Stopping server");
            System.exit(0);
        }
    }
}