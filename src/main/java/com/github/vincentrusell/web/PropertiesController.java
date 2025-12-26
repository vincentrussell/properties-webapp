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
@RequestMapping(path = "/properties")
public class PropertiesController {

    private final Properties properties = new Properties();
    private final ScheduledExecutorService saveFileService = Executors.newScheduledThreadPool(1);
    private File propertiesFile;

    @PostConstruct
    private void init() throws IOException {
        String propertiesFileLocation = System.getProperty("properties.file");

        if (propertiesFileLocation != null) {
            propertiesFile = new File(propertiesFileLocation);
            if (!propertiesFile.exists()) {
                propertiesFile.createNewFile();
            }

            synchronized (propertiesFile) {
                try (FileInputStream fileInputStream = new FileInputStream(propertiesFile)) {
                    properties.load(fileInputStream);
                }
            }
        }

        saveFileService.scheduleAtFixedRate(() -> saveFile(), 45, 45, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> saveFile()));

    }

    private void saveFile()  {
        synchronized (propertiesFile) {
            try (FileOutputStream fileOutputStream = new FileOutputStream(propertiesFile)) {
                properties.store(fileOutputStream, "properties-webapp properties save file");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @GetMapping(path="/property/{key}", produces = "text/plain")
    @ResponseBody
    public String get(@PathVariable String key) throws FileNotFoundException {
        String s = (String) properties.get(key);
        if (s == null) {
            throw new FileNotFoundException(String.format("key %s not found", key));
        }
        return s;
    }

    @RequestMapping(value = "/property/{key}", method = RequestMethod.PUT, produces = "text/plain")
    @ResponseBody
    public String put(@PathVariable String key, @RequestBody String value) {
        return (String) properties.put(key, value);
    }

    @RequestMapping(value = "/property/{key}", method = RequestMethod.DELETE, produces = "text/plain")
    @ResponseBody
    public String remove(@PathVariable String key) throws FileNotFoundException {
        String s = (String) properties.remove(key);

        if (s == null) {
            throw new FileNotFoundException(String.format("key %s not found", key));
        }

        return s;
    }

    @RequestMapping(value = "/properties", method = RequestMethod.PUT, consumes = "application/json")
    public void putAll(@RequestBody Map<String, String> t) {
        properties.putAll(t);
    }

    @GetMapping(path="/properties/keys", produces = "application/json")
    @ResponseBody
    public Set<String> keySet() {
        return properties.keySet().stream().map(o -> o.toString()).collect(Collectors.toSet());
    }

    @GetMapping(path="/properties", produces = "application/json")
    @ResponseBody
    public Map<String, String> asMap() {
        Map<String,String> result = new LinkedHashMap<>();
        properties.entrySet().stream().forEach(objectObjectEntry ->
                result.put(objectObjectEntry.getKey().toString(), objectObjectEntry.getValue().toString()));
        return result;
    }

}
