package org.ksmcbrigade;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

public class Utils {

    public static int FindCommand(String command){
        for(int i=0;i<CommandManager.commands.length;i++){
            if(command.equalsIgnoreCase(CommandManager.commands[i])){
                return i;
            }
        }
        return -1;
    }

    public static String GetHttps(String urlz){
        String re = "";
        try {
            URL url = new URL(urlz);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            //int responseCode = connection.getResponseCode();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            re = response.toString();

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return re;
    }

    public static String sendGetRequest(String url, String token) throws IOException {
        URL apiUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Accept", "application/json");

        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }

        connection.disconnect();

        return response.toString();
    }

    public static String sendPostRequest(String url, String jsonInputString) throws Exception {
        URL apiUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }

        connection.disconnect();

        return response.toString();
    }

    public static void downloadFile(String fileUrl, String fileName) {
        if(!new File(fileName).exists()){
            System.out.println("Downloading: "+fileUrl);
            try (BufferedInputStream in = new BufferedInputStream(new URL(fileUrl).openStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void downloadWithThreads(String fileUrl, String fileName) {
        ExecutorService executor = Executors.newFixedThreadPool(64);
        for (int i = 0; i < 64; i++) {
            executor.execute(() -> downloadFile(fileUrl, fileName));
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static long getLatency(String ipAddress) {
        try {
           URL url = new URL(ipAddress);
           long startTime = System.currentTimeMillis();
           HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
           httpURLConnection.setRequestMethod("GET");
           httpURLConnection.connect();
           return new Date().getTime() - startTime;
        } catch (IOException e) {
            return -1;
        }
    }

    public static void mkd(String dir){
        String[] dirs = dir.replace("\"","").split("/");
        String last = "";
        for(String s:dirs){
            if(last.equals("")){
                last = s;
                new File(last).mkdirs();
            }else{
                last = last + "/" + s;
               new File(last).mkdirs();
            }
        }
    }

    public static String readFile(String filePath) throws IOException {
        String path = filePath;
        if(System.getProperty("os.name").contains("Windows")){
            path = filePath.replace("/","\\");
        }
        File file = new File(path);
        long fileSize = file.length();
        FileInputStream fis = new FileInputStream(file);
        byte[] fileData = new byte[(int) fileSize];
        fis.read(fileData);
        fis.close();
        return new String(fileData, StandardCharsets.UTF_8);
    }

    public static String getLibraries(JsonObject json,String vn) throws IOException {
        String libs = "\"";
        mkd("CML/tmp/natives");
        String jvm_args = json.get("arguments").getAsJsonObject().get("jvm").toString();
        for (Iterator<JsonElement> it = json.get("libraries").getAsJsonArray().iterator(); it.hasNext(); ) {
            JsonElement version = it.next();
            try {
                if(version.getAsJsonObject().get("downloads").getAsJsonObject().has("artifact")){
                    String path = version.getAsJsonObject().get("downloads").getAsJsonObject().getAsJsonObject("artifact").get("path").getAsString();
                    if(!new File(".minecraft/libraries/"+path).exists()){
                        mkd(".minecraft/libraries/"+path.replace(path.split("/")[path.split("/").length-1],""));
                        downloadFile(version.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("artifact").get("url").getAsString(),".minecraft/libraries/"+path);
                    }
                    //if(!isLibrary(System.getProperty("user.dir").replace("\\","/")+"/.minecraft/libraries/"+path)){
                    //    System.out.println(System.getProperty("user.dir").replace("\\","/")+"/.minecraft/libraries/"+path);
                    if(!jvm_args.contains(path)){
                        if(libs.equals("\"")){
                            libs = libs + System.getProperty("user.dir").replace("\\","/")+"/.minecraft/libraries/"+path;
                        }
                        else{
                            libs = libs + ";" +System.getProperty("user.dir").replace("\\","/")+"/.minecraft/libraries/"+path;
                        }
                    }
                    //}
                }
            }
            catch (NullPointerException e){
                //install fabric or other
                String name = version.getAsJsonObject().get("name").getAsString();
                String path = name.split(":")[0].replaceAll("\\.","/")+"/"+name.split(":")[1]+"/"+name.split(":")[2];
                String fileName = name.split(":")[1]+"-"+name.split(":")[2]+".jar";
                mkd(".minecraft/libraries/"+path);
                downloadFile(version.getAsJsonObject().get("url").getAsString()+path+"/"+fileName,".minecraft/libraries/"+path+"/"+fileName);

                if(libs.equals("\"")){
                    libs = libs + System.getProperty("user.dir").replace("\\","/")+"/.minecraft/libraries/"+path+"/"+fileName;
                }
                else{
                    libs = libs + ";" +System.getProperty("user.dir").replace("\\","/")+"/.minecraft/libraries/"+path+"/"+fileName;
                }
            }
            try {
                JsonObject downloads = version.getAsJsonObject().getAsJsonObject("downloads");
                if(version.getAsJsonObject().get("downloads").getAsJsonObject().getAsJsonObject("classifiers")!=null){
                    String path2 = "CML/tmp/natives/tmp.jar";
                    for(String key:downloads.getAsJsonObject("classifiers").keySet()){
                        downloadFile(downloads.getAsJsonObject("classifiers").getAsJsonObject(key).get("url").getAsString(),path2);
                        extractFiles(path2,".minecraft/versions/"+vn+"/natives");
                        new File(System.getProperty("user.dir").replace("\\","/")+"/CML/tmp/natives/tmp.jar").delete();
                    }
                }
            }
            catch (Exception e){
                System.out.println("past a native.");
            }
        }
        libs = libs + ";" + System.getProperty("user.dir").replace("\\","/")+"/.minecraft/versions/"+json.get("id").getAsString()+"/"+json.get("id").getAsString()+ ".jar\"";
        //return onlyOneLWJGL(removeDuplicates(libs),System.getProperty("user.dir").replace("\\","/")+"/.minecraft/versions/"+json.get("id").getAsString()+"/"+json.get("id").getAsString()+ ".jar");
        return removeDuplicates(libs);
    }

    public static String getLibraryName(String pathz) throws IOException {
        String path = pathz.replace("\"","");
        if(System.getProperty("os.name").contains("Windows")){
            path = path.replace("/","\\");
        }
        JarFile file = new JarFile(path);
        Manifest manifest = file.getManifest();
        Attributes attributes = manifest.getMainAttributes();
        String ret = attributes.getValue("Implementation-Title");
        file.close();
        return ret;
    }

    public static String onlyOneLWJGL(String classpath,String a) throws IOException {
        StringBuilder stringBuilder = new StringBuilder("\"");
        String last = null;
        for(String cp:classpath.split(";")){
            String name = getLibraryName(cp);
            System.out.println(name);
            if(!Objects.equals(name, last) | name == null){
                if (!stringBuilder.toString().equals("\"")) {
                    stringBuilder.append(";");
                }
                stringBuilder.append(cp);
            }
            last = name;
        }
        stringBuilder.append(";");
        stringBuilder.append(a);
        stringBuilder.append("\"");
        return stringBuilder.toString();
    }

    public static JsonObject getUser(JsonArray jsons,String userName){
        for(JsonElement user:jsons){
            if(user.getAsJsonObject().get("name").getAsString().equals(userName)){
                return (JsonObject) user;
            }
        }
        return null;
    }

    public static String getVersion(JsonObject json){
        String time = json.get("time").getAsString();
        JsonObject json2 = (JsonObject) JsonParser.parseString(Main.MC_VERSIONS);
        for(JsonElement jsonElement:json2.getAsJsonArray("versions")){
            if(jsonElement.isJsonObject()){
                if(jsonElement.getAsJsonObject().get("releaseTime").getAsString().equals(time)){
                    return jsonElement.getAsJsonObject().get("id").getAsString();
                }
            }
        }
        return "";
    }

    public static void runBatchFile(String batchFilePath) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", batchFilePath);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            System.out.println("External command finished with exit code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void extractFiles(String jarFilePath, String outputDir) {
        System.out.println("Extracting: "+jarFilePath);
        try (ZipFile zipFile = new ZipFile(jarFilePath)) {
            zipFile.stream()
                    .filter(entry -> entry.getName().toLowerCase().endsWith(".dll"))
                    .forEach(entry -> {
                        try (InputStream input = zipFile.getInputStream(entry)) {
                            String fileName = new File(entry.getName()).getName();
                            try (OutputStream output = new FileOutputStream(new File(outputDir, fileName))) {
                                byte[] buffer = new byte[1024];
                                int bytesRead;
                                while ((bytesRead = input.read(buffer)) != -1) {
                                    output.write(buffer, 0, bytesRead);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            zipFile.stream()
                    .filter(entry -> entry.getName().toLowerCase().endsWith(".dylib"))
                    .forEach(entry -> {
                        try (InputStream input = zipFile.getInputStream(entry)) {
                            String fileName = new File(entry.getName()).getName();
                            try (OutputStream output = new FileOutputStream(new File(outputDir, fileName))) {
                                byte[] buffer = new byte[1024];
                                int bytesRead;
                                while ((bytesRead = input.read(buffer)) != -1) {
                                    output.write(buffer, 0, bytesRead);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            zipFile.stream()
                    .filter(entry -> entry.getName().toLowerCase().endsWith(".so"))
                    .forEach(entry -> {
                        try (InputStream input = zipFile.getInputStream(entry)) {
                            String fileName = new File(entry.getName()).getName();
                            try (OutputStream output = new FileOutputStream(new File(outputDir, fileName))) {
                                byte[] buffer = new byte[1024];
                                int bytesRead;
                                while ((bytesRead = input.read(buffer)) != -1) {
                                    output.write(buffer, 0, bytesRead);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void DelDirFiles(String dir,String hz){
        File files = new File(dir);
        for(File file:files.listFiles()){
            if(file.getName().endsWith("."+hz)){
                file.delete();
            }
        }
    }

    public static boolean DelDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = DelDir
                        (new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        if(dir.delete()) {
            return true;
        } else {
            return false;
        }
    }

    public static String removeDuplicates(String classpath) {
        String[] parts = classpath.split(";");
        Set<String> set = new LinkedHashSet<>();
        for (String part : parts) {
            set.add(part.trim());
        }
        return String.join(";", set);
    }

    public static String getCommand(String[] args){
        for(String arg:args){
            if(arg.contains("=") && arg.split("=")[0].equalsIgnoreCase("-c")){
                return arg.split("=")[1].replace("\"","");
            }
        }
        return null;
    }

    public static String getInput(String[] args){
        for(String arg:args){
            if(arg.contains("[") && arg.split("\\[")[0].equalsIgnoreCase("-i")){
                return arg.split("\\[")[1].replace("\"","");
            }
        }
        return null;
    }

    public static int getMaxMemory() throws IOException {
        JsonObject json = JsonParser.parseString(readFile("CML/config.json")).getAsJsonObject();
        return json.get("maxMemory").getAsInt();
    }

    public static void setMaxMemory(int maxMemory) throws IOException {
        JsonObject json = JsonParser.parseString(readFile("CML/config.json")).getAsJsonObject();
        json.addProperty("maxMemory",maxMemory);
        Files.write(Paths.get("CML/config.json"),json.toString().getBytes());
    }

    public static JsonElement mergeElements(JsonElement element1, JsonElement element2) {
        if (element1.isJsonObject() && element2.isJsonObject()) {
            return mergeJsonObjects(element1.getAsJsonObject(), element2.getAsJsonObject());
        } else if (element1.isJsonArray() && element2.isJsonArray()) {
            return mergeJsonArrays(element1.getAsJsonArray(), element2.getAsJsonArray());
        } else {
            return element2;
        }
    }

    public static JsonObject mergeJsonObjects(JsonObject obj1, JsonObject obj2) {
        JsonObject mergedObj = new JsonObject();

        for (String key : obj1.keySet()) {
            mergedObj.add(key, obj1.get(key));
        }

        for (String key : obj2.keySet()) {
            if (obj1.has(key)) {
                mergedObj.add(key, mergeElements(obj1.get(key), obj2.get(key)));
            } else {
                mergedObj.add(key, obj2.get(key));
            }
        }

        return mergedObj;
    }

    public static JsonArray mergeJsonArrays(JsonArray array1, JsonArray array2) {
        JsonArray mergedArray = new JsonArray();
        mergedArray.addAll(array1);
        mergedArray.addAll(array2);
        return mergedArray;
    }

    //forge
    public static Object DownloadForgeInstaller(String version){
        try {
            JsonArray forgeInfos = JsonParser.parseString(GetHttps("https://bmclapi2.bangbang93.com/forge/minecraft/"+version)).getAsJsonArray();
            JsonObject forgeInfo = forgeInfos.get(forgeInfos.size()-1).getAsJsonObject();
            String forgeVersion = forgeInfo.get("version").getAsString();
            downloadFile("https://bmclapi2.bangbang93.com/forge/download?mcversion="+version+"&version="+forgeVersion+"&category=installer&format=jar","CML/tmp/forge-installer.jar");
            return forgeVersion;
        }
        catch (Exception e){
            System.out.println("Failed to get the forge version.");
            return false;
        }
    }
}
