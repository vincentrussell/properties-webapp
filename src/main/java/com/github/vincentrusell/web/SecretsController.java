package com.github.vincentrusell.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/secrets")
public class SecretsController {

    private final Properties secrets = new Properties();
    private final ScheduledExecutorService saveFileService = Executors.newScheduledThreadPool(1);
    private File secretsFile = null;

    private final SecretsManager secretsManager;

    public SecretsController(SecretsManager secretsManager) {
        this.secretsManager = secretsManager;
    }

    @PostConstruct
    private void init() throws IOException {
        String secretsFileLocation = System.getProperty("secrets.file");

        if (secretsFileLocation != null) {
            secretsFile = new File(secretsFileLocation);
            if (!secretsFile.exists()) {
                secretsFile.createNewFile();
            }

            synchronized (secretsFile) {
                Properties encryptedSecrets = new Properties();
                try (FileInputStream fileInputStream = new FileInputStream(secretsFile)) {
                    encryptedSecrets.load(fileInputStream);
                }

                encryptedSecrets.forEach((o, o2) -> {
                    try {
                        secrets.put(secretsManager.decryptString(o.toString()),
                                secretsManager.decryptString(o2.toString()));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        saveFileService.scheduleAtFixedRate(() -> saveFile(), 45, 45, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> saveFile()));

    }

    private void saveFile()  {
        synchronized (secretsFile) {
            Properties encryptedSecrets = new Properties();

            secrets.forEach((o, o2) -> {
                try {
                    encryptedSecrets.put(secretsManager.encryptString(o.toString()),
                            secretsManager.encryptString(o2.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            });

            try (FileOutputStream fileOutputStream = new FileOutputStream(secretsFile)) {
                encryptedSecrets.store(fileOutputStream, "properties-webapp encrypted secrets save file");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @GetMapping(path="/secret/{key}", produces = "text/plain")
    @ResponseBody
    public String get(@PathVariable String key) throws FileNotFoundException {
        String s = (String) secrets.get(key);
        if (s == null) {
            throw new FileNotFoundException(String.format("key %s not found", key));
        }
        return s;
    }

    @RequestMapping(value = "/secret/{key}", method = RequestMethod.PUT, produces = "text/plain")
    @ResponseBody
    public String put(@PathVariable String key, @RequestBody String value) {
        return (String) secrets.put(key, value);
    }

    @RequestMapping(value = "/secret/{key}", method = RequestMethod.DELETE, produces = "text/plain")
    @ResponseBody
    public String remove(@PathVariable String key) throws FileNotFoundException {
        String s = (String) secrets.remove(key);

        if (s == null) {
            throw new FileNotFoundException(String.format("key %s not found", key));
        }

        return s;
    }

    @RequestMapping(value = "/secrets", method = RequestMethod.PUT, consumes = "application/json")
    public void putAll(@RequestBody Map<String, String> t) {
        secrets.putAll(t);
    }

    @GetMapping(path="/secrets/keys", produces = "application/json")
    @ResponseBody
    public Set<String> keySet() {
        return secrets.keySet().stream().map(o -> o.toString()).collect(Collectors.toSet());
    }

    @GetMapping(path="/secrets", produces = "application/json")
    @ResponseBody
    public Map<String, String> asMap() {
        Map<String,String> result = new LinkedHashMap<>();
        secrets.entrySet().stream().forEach(objectObjectEntry ->
                result.put(objectObjectEntry.getKey().toString(), objectObjectEntry.getValue().toString()));
        return result;
    }

    @RequestMapping(value = "/secret/encryptString", method = RequestMethod.PUT,
            consumes = "text/plain", produces = "text/plain")
    @ResponseBody
    public String encryptString(@RequestBody String plainText) throws Exception {
        return secretsManager.encryptString(plainText);
    }

    @RequestMapping(value = "/secret/decryptString", method = RequestMethod.PUT,
            consumes = "text/plain", produces = "text/plain")
    @ResponseBody
    public String decryptString(@RequestBody final String encryptedText) throws Exception {
        return secretsManager.decryptString(encryptedText);
    }
}
