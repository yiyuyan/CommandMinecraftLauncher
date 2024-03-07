package org.ksmcbrigade;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.UUID;
import java.util.jar.JarFile;

import static org.ksmcbrigade.Utils.*;

public class CommandManager {
    public static String[] commands = new String[]{"help","dm","dmod","list","lj","lm","start","exit","us","mm","lav","msa","dt"};
    public static int[] commandsArgsLength = new int[]{1,4,3,1,1,2,5,1,4,3,1,2,1};
    public static String[] commandsUsage = new String[]{"View all instruction usage.","Download Minecraft.","Download Minecraft mods.","View all downloaded game versions.","View all java path and version.","View all mods for the game version specified for download.","Start Minecraft.","Exit launcher.","View all users or add/del user.","Modify or view the maximum heap memory.","View all Minecraft versions.","msa login and save to config file.","clear the launcher tmp files."};
    public static String[] commandsUsages = new String[]{"help","dm <v|f|fo> <version> <version_name>","dmod <version_name> <mod_name>","list","lj","lm <version_name>","start <version_name> <msa|other> <user_name> <java_path>","exit","us <list|add|del> <user_name> <msa|other>","mm <list|set> <memory(MB)>","list","msa <url>","dt"};
    public static boolean[] commandsAvailability = new boolean[]{true,true,false,true,true,false,true,true,true,true,true,true,true};

    public static String run(String command,String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if(args.length<(commandsArgsLength[FindCommand(command)]-1)){
            return "\nCan't execute command: "+command+"\nCommand args number: "+ (commandsArgsLength[FindCommand(command)] - 1) +"\nCommand usage: "+commandsUsages[FindCommand(command)];
        }
        else{
            try {
                return CommandManager.class.getDeclaredMethod(command,String[].class).invoke(CommandManager.class,(Object)args).toString();
            }
            catch (Exception e){
                e.printStackTrace();
                return "\nCan't execute command.";
            }
        }
    }

    public static String help(String[] args){
        String xx = "Commands:";
        for(int i=0;i<commands.length;i++){
            xx = xx+"\n\nCommand: "+commands[i]+"\nCommand args number: "+ (commandsArgsLength[i] - 1) +"\nCommand description: "+commandsUsage[i] +"\nCommand usage: "+commandsUsages[i]+"\nCommand availability: "+commandsAvailability[i];;
        }
        return xx;
    }

    public static String dm(String[] args) throws IOException, InterruptedException {
        if(args[0].equalsIgnoreCase("v")){
            System.out.println("Downloading...");
            long start_time = System.currentTimeMillis();
            JsonObject versions = (JsonObject) JsonParser.parseString(Main.MC_VERSIONS);
            JsonObject json = new JsonObject();
            for (Iterator<JsonElement> it = versions.get("versions").getAsJsonArray().iterator(); it.hasNext(); ) {
                JsonElement version = it.next();
                if(version.getAsJsonObject().get("id").getAsString().equalsIgnoreCase(args[1])){
                    json = (JsonObject) JsonParser.parseString(GetHttps(version.getAsJsonObject().get("url").getAsString()));
                    break;
                }
            }
            json.addProperty("id",args[2]);
            new File(".minecraft/versions/"+args[2]+"/").mkdirs();
            new File(".minecraft/versions/"+args[2]+"/natives").mkdirs();
            mkd("CML/tmp/natives");
            Files.write(Paths.get(".minecraft/versions/"+args[2]+"/"+args[2]+".json"),json.toString().getBytes());
            downloadFile(json.get("downloads").getAsJsonObject().get("client").getAsJsonObject().get("url").getAsString(),".minecraft/versions/"+args[2]+"/"+args[2]+".jar");

            for (Iterator<JsonElement> it = json.get("libraries").getAsJsonArray().iterator(); it.hasNext(); ) {
                JsonElement version = it.next();
                if(version.getAsJsonObject().get("downloads").getAsJsonObject().has("artifact")){
                    String path = version.getAsJsonObject().get("downloads").getAsJsonObject().getAsJsonObject("artifact").get("path").getAsString();
                    mkd(".minecraft/libraries/"+path.replace(path.split("/")[path.split("/").length-1],""));
                    downloadFile(version.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("artifact").get("url").getAsString(),".minecraft/libraries/"+path);
                }
                JsonObject downloads = version.getAsJsonObject().getAsJsonObject("downloads");
                if(version.getAsJsonObject().get("downloads").getAsJsonObject().getAsJsonObject("classifiers")!=null){
                    String path2 = "CML/tmp/natives/tmp.jar";
                    for(String key:downloads.getAsJsonObject("classifiers").keySet()){
                        downloadFile(downloads.getAsJsonObject("classifiers").getAsJsonObject(key).get("url").getAsString(),path2);
                        extractFiles(path2,".minecraft/versions/"+args[2]+"/natives");
                        new File(System.getProperty("user.dir").replace("\\","/")+"/CML/tmp/natives/tmp.jar").delete();
                    }
                }
            }

            JsonObject assets = (JsonObject) JsonParser.parseString(GetHttps(json.getAsJsonObject("assetIndex").get("url").getAsString()));
            Files.write(Paths.get(".minecraft/assets/indexes/"+json.get("assets").getAsString()+".json"),assets.toString().getBytes());
            for(String key:assets.getAsJsonObject("objects").keySet()) {
                JsonObject item = assets.getAsJsonObject("objects").getAsJsonObject(key);
                String hash = item.get("hash").getAsString();
                String hash2 = hash.substring(0,2);
                mkd(".minecraft/assets/objects/"+hash2);
                downloadFile("https://resources.download.minecraft.net/"+hash2+"/"+hash,".minecraft/assets/objects/"+hash2+"/"+hash);
            }

            long end_time = System.currentTimeMillis();
            System.out.println("Download completed,time-consuming: "+(end_time-start_time)/1000+" seconds");
            System.out.println("Done.");
        } else if (args[0].equalsIgnoreCase("f")) { //fabric
            System.out.println("Downloading...");
            long start_time = System.currentTimeMillis();
            JsonObject versions = (JsonObject) JsonParser.parseString(Main.MC_VERSIONS);
            JsonObject json = new JsonObject();
            for (Iterator<JsonElement> it = versions.get("versions").getAsJsonArray().iterator(); it.hasNext(); ) {
                JsonElement version = it.next();
                if(version.getAsJsonObject().get("id").getAsString().equalsIgnoreCase(args[1])){
                    json = (JsonObject) JsonParser.parseString(GetHttps(version.getAsJsonObject().get("url").getAsString()));
                    break;
                }
            }
            json.addProperty("id",args[2]);

            //install fabric
            downloadFile("https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.0/fabric-installer-1.0.0.jar","CML/tmp/fabric-installer.jar");
            ProcessBuilder pb;
            if(System.getProperty("os.name").contains("Windows")){
                pb = new ProcessBuilder(System.getProperty("java.home")+"\\bin\\java.exe","-jar","CML/tmp/fabric-installer.jar","client","-dir",".minecraft","-mcversion",args[1],"--downloadMinecraft");
            }
            else{
                pb = new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-jar","CML/tmp/fabric-installer.jar","client","-dir",".minecraft","-mcversion",args[1],"--downloadMinecraft");
            }
            Process process = pb.start();
            process.waitFor();

            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String LoaderLine = "0.15.7";
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                if(line.contains("Installing "+args[1]+" with fabric ")){
                    LoaderLine = line;
                }
            }

            String loader = LoaderLine.replace("Installing "+args[1]+" with fabric ","");
            new File(".minecraft/versions/fabric-loader-"+loader+"-"+args[1]+"/").renameTo(new File(".minecraft/versions/"+args[2]+"/"));
            new File(".minecraft/versions/"+args[2]+"/natives").mkdirs();
            mkd("CML/tmp/natives");

            new File(".minecraft/versions/"+args[2]+"/fabric-loader-"+loader+"-"+args[1]+".json").renameTo(new File(".minecraft/versions/"+args[2]+"/"+args[2]+".json"));
            JsonObject fabricJson = JsonParser.parseString(new String(Files.readAllBytes(Paths.get(".minecraft/versions/"+args[2]+"/"+args[2]+".json")))).getAsJsonObject();
            fabricJson.addProperty("id",args[2]);
            json = (JsonObject) mergeElements(json, fabricJson);
            Files.write(Paths.get(".minecraft/versions/"+args[2]+"/"+args[2]+".json"),json.toString().getBytes());
            downloadFile(json.get("downloads").getAsJsonObject().get("client").getAsJsonObject().get("url").getAsString(),".minecraft/versions/"+args[2]+"/"+args[2]+".jar");

            for (Iterator<JsonElement> it = json.get("libraries").getAsJsonArray().iterator(); it.hasNext(); ) {
                JsonElement version = it.next();
                try {
                    if(version.getAsJsonObject().get("downloads").getAsJsonObject().has("artifact")){
                        String path = version.getAsJsonObject().get("downloads").getAsJsonObject().getAsJsonObject("artifact").get("path").getAsString();
                        mkd(".minecraft/libraries/"+path.replace(path.split("/")[path.split("/").length-1],""));
                        downloadFile(version.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("artifact").get("url").getAsString(),".minecraft/libraries/"+path);
                    }
                }
                catch (NullPointerException e){
                    //install fabric
                    String name = version.getAsJsonObject().get("name").getAsString();
                    String path = name.split(":")[0].replaceAll("\\.","/")+"/"+name.split(":")[1]+"/"+name.split(":")[2];
                    String fileName = name.split(":")[1]+"-"+name.split(":")[2]+".jar";
                    mkd(".minecraft/libraries/"+path);
                    downloadFile(version.getAsJsonObject().get("url").getAsString()+path+"/"+fileName,".minecraft/libraries/"+path+"/"+fileName);
                }
                try {
                    JsonObject downloads = version.getAsJsonObject().getAsJsonObject("downloads");
                    if(version.getAsJsonObject().get("downloads").getAsJsonObject().getAsJsonObject("classifiers")!=null){
                        String path2 = "CML/tmp/natives/tmp.jar";
                        for(String key:downloads.getAsJsonObject("classifiers").keySet()){
                            downloadFile(downloads.getAsJsonObject("classifiers").getAsJsonObject(key).get("url").getAsString(),path2);
                            extractFiles(path2,".minecraft/versions/"+args[2]+"/natives");
                            new File(System.getProperty("user.dir").replace("\\","/")+"/CML/tmp/natives/tmp.jar").delete();
                        }
                    }
                }
                catch (Exception e){
                    System.out.println("past a native.");
                }
            }

            JsonObject assets = (JsonObject) JsonParser.parseString(GetHttps(json.getAsJsonObject("assetIndex").get("url").getAsString()));
            Files.write(Paths.get(".minecraft/assets/indexes/"+json.get("assets").getAsString()+".json"),assets.toString().getBytes());
            for(String key:assets.getAsJsonObject("objects").keySet()) {
                JsonObject item = assets.getAsJsonObject("objects").getAsJsonObject(key);
                String hash = item.get("hash").getAsString();
                String hash2 = hash.substring(0,2);
                mkd(".minecraft/assets/objects/"+hash2);
                downloadFile("https://resources.download.minecraft.net/"+hash2+"/"+hash,".minecraft/assets/objects/"+hash2+"/"+hash);
            }

            long end_time = System.currentTimeMillis();
            System.out.println("Download completed,time-consuming: "+(end_time-start_time)/1000+" seconds");
            System.out.println("Done.");
        }
        else if (args[0].equalsIgnoreCase("fo")) //forge
        {
            System.out.println("Downloading...");
            long start_time = System.currentTimeMillis();
            JsonObject versions = (JsonObject) JsonParser.parseString(Main.MC_VERSIONS);
            JsonObject json = new JsonObject();
            for (Iterator<JsonElement> it = versions.get("versions").getAsJsonArray().iterator(); it.hasNext(); ) {
                JsonElement version = it.next();
                if(version.getAsJsonObject().get("id").getAsString().equalsIgnoreCase(args[1])){
                    json = (JsonObject) JsonParser.parseString(GetHttps(version.getAsJsonObject().get("url").getAsString()));
                    break;
                }
            }
            json.addProperty("id",args[2]);

            //install forge
            new File("CML/tmp/forge-installer.jar").delete();
            Object obj = DownloadForgeInstaller(args[1]);
            if(obj instanceof Boolean){
                return "";
            }
            JarFile jarFile = new JarFile(System.getProperty("user.dir").replace("\\","/")+"/CML/tmp/forge-installer.jar");

            try {
                InputStream in = jarFile.getInputStream(jarFile.getEntry("install_profile.json"));

                byte[] bytes = new byte[in.available()];
                in.read(bytes);
                String context = new String(bytes);
                JsonObject profile = JsonParser.parseString(context).getAsJsonObject();

                in.close();
                jarFile.close();

                if(profile.get("versionInfo")!=null){
                    //old forge version
                    new File(".minecraft/versions/"+args[2]).mkdirs();
                    Files.write(Paths.get(".minecraft/versions/"+args[2]+"/"+args[2]+".json"),profile.getAsJsonObject("versionInfo").toString().getBytes());
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }

            System.out.println("Building the forge.");
            ProcessBuilder pb;
            if(System.getProperty("os.name").contains("Windows")){
                pb = new ProcessBuilder(System.getProperty("java.home")+"\\bin\\java.exe","-jar","CML/tmp/forge-installer.jar","--installClient",".minecraft");
            }
            else{
                pb = new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-jar","CML/tmp/forge-installer.jar","--installClient",".minecraft");
            }
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();

            String ver = (String)obj;
            new File(".minecraft/versions/"+args[1]+"-forge-"+ver).renameTo(new File(".minecraft/versions/"+args[2]+"/"));
            new File(".minecraft/versions/"+args[2]+"/natives").mkdirs();
            mkd("CML/tmp/natives");

            new File(".minecraft/versions/"+args[2]+"/"+args[1]+"-forge-"+ver+".json").renameTo(new File(".minecraft/versions/"+args[2]+"/"+args[2]+".json"));
            JsonObject fabricJson = JsonParser.parseString(new String(Files.readAllBytes(Paths.get(".minecraft/versions/"+args[2]+"/"+args[2]+".json")))).getAsJsonObject();
            fabricJson.addProperty("id",args[2]);
            json = (JsonObject) mergeElements(json, fabricJson);
            Files.write(Paths.get(".minecraft/versions/"+args[2]+"/"+args[2]+".json"),json.toString().getBytes());
            downloadFile(json.get("downloads").getAsJsonObject().get("client").getAsJsonObject().get("url").getAsString(),".minecraft/versions/"+args[2]+"/"+args[2]+".jar");

            /*for (Iterator<JsonElement> it = json.get("libraries").getAsJsonArray().iterator(); it.hasNext(); ) {
                JsonElement version = it.next();
                try {
                    if(version.getAsJsonObject().get("downloads").getAsJsonObject().has("artifact")){
                        String path = version.getAsJsonObject().get("downloads").getAsJsonObject().getAsJsonObject("artifact").get("path").getAsString();
                        mkd(".minecraft/libraries/"+path.replace(path.split("/")[path.split("/").length-1],""));
                        downloadFile(version.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("artifact").get("url").getAsString(),".minecraft/libraries/"+path);
                    }
                }
                catch (NullPointerException e){
                    System.out.println(e.getMessage()+" ?");
                }
                try {
                    JsonObject downloads = version.getAsJsonObject().getAsJsonObject("downloads");
                    if(version.getAsJsonObject().get("downloads").getAsJsonObject().getAsJsonObject("classifiers")!=null){
                        String path2 = "CML/tmp/natives/tmp.jar";
                        for(String key:downloads.getAsJsonObject("classifiers").keySet()){
                            downloadFile(downloads.getAsJsonObject("classifiers").getAsJsonObject(key).get("url").getAsString(),path2);
                            extractFiles(path2,".minecraft/versions/"+args[2]+"/natives");
                            new File(System.getProperty("user.dir").replace("\\","/")+"/CML/tmp/natives/tmp.jar").delete();
                        }
                    }
                }
                catch (Exception e){
                    System.out.println("past a native.");
                }
            }*/
            getLibraries(json,args[2],System.getProperty("user.dir").replace("\\","/")+"/CML/tmp/forge-installer.jar");

            JsonObject assets = (JsonObject) JsonParser.parseString(GetHttps(json.getAsJsonObject("assetIndex").get("url").getAsString()));
            Files.write(Paths.get(".minecraft/assets/indexes/"+json.get("assets").getAsString()+".json"),assets.toString().getBytes());
            for(String key:assets.getAsJsonObject("objects").keySet()) {
                JsonObject item = assets.getAsJsonObject("objects").getAsJsonObject(key);
                String hash = item.get("hash").getAsString();
                String hash2 = hash.substring(0,2);
                mkd(".minecraft/assets/objects/"+hash2);
                downloadFile("https://resources.download.minecraft.net/"+hash2+"/"+hash,".minecraft/assets/objects/"+hash2+"/"+hash);
            }

            long end_time = System.currentTimeMillis();
            System.out.println("Download completed,time-consuming: "+(end_time-start_time)/1000+" seconds");
            System.out.println("Done.");
        }
        return "";
    }

    public static String dmod(String[] args){
        String xx = "No";
        return xx;
    }

    public static String list(String[] args) throws IOException {
        File dirs = new File(".minecraft/versions");
        System.out.println("Versions:");
        for(String dir:dirs.list()){
            try {
                System.out.println("\nVersion name:"+JsonParser.parseString(readFile(".minecraft/versions/"+dir+"/"+dir+".json")).getAsJsonObject().get("id").getAsString()+"\nVersion:"+getVersion(JsonParser.parseString(readFile(".minecraft/versions/"+dir+"/"+dir+".json")).getAsJsonObject())+"\nVersion type:"+JsonParser.parseString(readFile(".minecraft/versions/"+dir+"/"+dir+".json")).getAsJsonObject().get("type").getAsString());
            }
            catch (Exception e){
                System.out.println("past a error version: "+e.getMessage());
            }
        }
        return "";
    }

    public static String lj(String[] args) throws IOException {
        System.out.println("Javas:");
        ArrayList<String> javaPaths = new ArrayList<>();
        javaPaths.add(System.getProperty("java.home"));
        String[] javaHomes = new String[]{System.getenv("JAVA_HOME")};
        if(System.getenv("JAVA_HOME").contains(File.pathSeparator)){
            javaHomes = System.getenv("JAVA_HOME").split(File.pathSeparator);
        }
        for (String javaHome : javaHomes) {
            javaPaths.add(javaHome);
        }

        for(String java:javaPaths){
            if(System.getProperty("os.name").contains("Windows")){
                System.out.println(("\nJava bin path:"+java+"\\bin\nJava java.exe path:"+java+"\\bin\\"+"java.exe\nJava javaw.exe path:"+java+"\\bin\\"+"javaw.exe").replace(" ","|"));
            }
            else{
                System.out.println(("\nJava bin path:"+java+"/bin\nJava java path:"+java+"/bin/"+"java\nJava javaw path:"+java+"/bin/"+"javaw").replace(" ","|"));
            }
        }
        return "";
    }

    public static String lm(String[] args){
        String xx = "No";
        return xx;
    }

    public static String start(String[] args) throws Exception {
        System.out.println("Game is starting...");
        long start_time = System.currentTimeMillis();
        mkd(".minecraft/versions/"+args[0]+"/natives");
        JsonObject json = (JsonObject) JsonParser.parseString(readFile(System.getProperty("user.dir").replace("\\","/")+"/.minecraft/versions/"+args[0]+"/"+args[0]+".json"));
        ArrayList<String> all_args = new ArrayList<>();
        JsonArray jvm_args = JsonParser.parseString(GetHttps("https://piston-meta.mojang.com/v1/packages/c24c2fd37c8ca2e1c18721e2c77caf4d24c87f92/1.13.json")).getAsJsonObject().getAsJsonObject("arguments").getAsJsonArray("jvm");
        if(json.get("arguments")!=null){
            jvm_args = json.getAsJsonObject("arguments").getAsJsonArray("jvm");
        }
        boolean forge = false;
        for(JsonElement jvm:jvm_args){
            if(jvm.isJsonObject()){
                if(jvm.getAsJsonObject().get("value").isJsonArray()){
                    for(JsonElement ga:jvm.getAsJsonObject().getAsJsonArray("value")){
                        all_args.add(ga.getAsString().replace(" ","").replace(",","，"));
                    }
                }
                else{
                    all_args.add(jvm.getAsJsonObject().get("value").getAsString().replace(" ","").replace(",","，"));
                }
            }
            else{
                all_args.add(jvm.getAsString().replace(" ","").replace(",","，"));
                if(jvm.getAsString().toLowerCase().contains("forge")){
                    forge=true;
                }
            }
        }
        String mainClass = json.get("mainClass").getAsString();
        if(forge){
            all_args.add("-Dlog4j2.formatMsgNoLookups=true");
            all_args.add("--add-exports");
            all_args.add("{class}/{class}=ALL-UNNAMED".replace("{class}",mainClass.replaceAll("\\."+mainClass.split("\\.")[3],"")));
        }
        all_args.add(mainClass);
        JsonArray jvm_game = JsonParser.parseString(GetHttps("https://piston-meta.mojang.com/v1/packages/c24c2fd37c8ca2e1c18721e2c77caf4d24c87f92/1.13.json")).getAsJsonObject().getAsJsonObject("arguments").getAsJsonArray("game");
        boolean forge2 = false;
        if(json.has("arguments")){
            jvm_game = json.getAsJsonObject("arguments").getAsJsonArray("game");
        }
        if(json.has("minecraftArguments")){
            jvm_game = new JsonArray();
            String MinecraftArgs = json.get("minecraftArguments").getAsString();
            for(String name:MinecraftArgs.split(" ")){
                jvm_game.add(name);
                if(name.toLowerCase().contains("forge")){
                    forge2=true;
                }
            }
        }
        for(JsonElement game:jvm_game){
            if(game.isJsonObject()){
                if(game.getAsJsonObject().get("value").isJsonArray()){
                    for(JsonElement ga:game.getAsJsonObject().getAsJsonArray("value")){
                        all_args.add(ga.getAsString());
                    }
                }
                else{
                    all_args.add(game.getAsJsonObject().get("value").getAsString());
                }
            }
            else{
                all_args.add(game.getAsString());
                if(game.getAsString().toLowerCase().contains("forge")){
                    forge2=true;
                }
            }
        }
        String ACCESS_TOKEN = "0";
        String UUid = UUID.randomUUID().toString().replace("-","");
        String zn = args[2];
        JsonObject json2 = (JsonObject) JsonParser.parseString(readFile("CML/config.json"));
        if(getUser(json2.getAsJsonArray("users"),zn)==null){
            JsonObject user_json = new JsonObject();
            user_json.addProperty("name",zn);
            user_json.addProperty("uuid",UUid);
            user_json.addProperty("access_token",ACCESS_TOKEN);
            json2.getAsJsonArray("users").add(user_json);
            Files.write(Paths.get("CML/config.json"),json2.toString().getBytes());
        }
        if(args[1].equalsIgnoreCase("msa")){
            if(getUser(json2.getAsJsonArray("users"),zn).get("access_token").getAsString().equals("0")){
                String url;
                if(Main.cmd==null){
                    Desktop.getDesktop().browse(new URI("https://login.live.com/oauth20_authorize.srf?client_id=00000000402b5328&response_type=code&scope=service%3A%3Auser.auth.xboxlive.com%3A%3AMBI_SSL&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf"));
                    System.out.println("Return url:");
                    Scanner scanner = new Scanner(System.in);
                    url = scanner.nextLine();
                }
                else if(Main.input==null){
                    Desktop.getDesktop().browse(new URI("https://login.live.com/oauth20_authorize.srf?client_id=00000000402b5328&response_type=code&scope=service%3A%3Auser.auth.xboxlive.com%3A%3AMBI_SSL&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf"));
                    System.out.println("Return url:");
                    Scanner scanner = new Scanner(System.in);
                    url = scanner.nextLine();
                }
                else{
                    url = Main.input;
                }
                String code = url.split("\\?")[1].split("&")[0].split("=")[1];
                String access_token = JsonParser.parseString(GetHttps("https://login.live.com/oauth20_token.srf?client_id=00000000402b5328&code={code}&grant_type=authorization_code&redirect_uri=https://login.live.com/oauth20_desktop.srf&scope=service::user.auth.xboxlive.com::MBI_SSL".replace("{code}",code))).getAsJsonObject().get("access_token").getAsString();
                JsonObject XBL_JSON = JsonParser.parseString(sendPostRequest("https://user.auth.xboxlive.com:443/user/authenticate","{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"{ac}\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}".replace("{ac}",access_token))).getAsJsonObject();
                String XBL_Token = XBL_JSON.get("Token").getAsString();
                String uhs = XBL_JSON.getAsJsonObject("DisplayClaims").get("xui").getAsJsonArray().get(0).getAsJsonObject().get("uhs").getAsString();
                JsonObject XSTS_JSON = JsonParser.parseString(sendPostRequest("https://xsts.auth.xboxlive.com/xsts/authorize","{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"{xbl_token}\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}".replace("{xbl_token}",XBL_Token))).getAsJsonObject();
                String XSTS_Token = XSTS_JSON.get("Token").getAsString();
                JsonObject AC_JSON = JsonParser.parseString(sendPostRequest("https://api.minecraftservices.com/authentication/login_with_xbox","{\"identityToken\":\"XBL3.0 x={uhs};{xsts_token}\"}".replace("{uhs}",uhs).replace("{xsts_token}",XSTS_Token))).getAsJsonObject();
                ACCESS_TOKEN = AC_JSON.get("access_token").getAsString();
                JsonObject Minecraft_ProFile = JsonParser.parseString(sendGetRequest("https://api.minecraftservices.com/minecraft/profile",ACCESS_TOKEN)).getAsJsonObject();
                UUid = Minecraft_ProFile.get("id").getAsString();
                zn = Minecraft_ProFile.get("name").getAsString();
                if(getUser(json2.getAsJsonArray("users"),zn)==null){
                    JsonObject user_json = new JsonObject();
                    user_json.addProperty("name",zn);
                    user_json.addProperty("uuid",UUid);
                    user_json.addProperty("access_token",ACCESS_TOKEN);
                    json2.getAsJsonArray("users").add(user_json);
                    Files.write(Paths.get("CML/config.json"),json2.toString().getBytes());
                }
                else{
                    JsonArray users = json2.getAsJsonArray("users");
                    for (int i = 0; i < users.size(); i++) {
                        JsonObject user = users.get(i).getAsJsonObject();
                        if (user.get("name").getAsString().equals(zn)) {
                            users.remove(i);
                            break;
                        }
                    }

                    JsonObject user_json = new JsonObject();
                    user_json.addProperty("name",zn);
                    user_json.addProperty("uuid",UUid);
                    user_json.addProperty("access_token",ACCESS_TOKEN);
                    json2.getAsJsonArray("users").add(user_json);
                    Files.write(Paths.get("CML/config.json"),json2.toString().getBytes());
                }
            }
        }

        String libraries_path = System.getProperty("user.dir").replace("\\","/")+"/.minecraft/libraries";
        if(System.getProperty("os.name").contains("Windows")){
            libraries_path=libraries_path.replace("/","\\");
        }

        System.out.println(forge+" : "+forge2);
        if(forge || forge2){
            DownloadForgeInstaller(json.get("inheritsFrom")!=null?json.get("inheritsFrom").getAsString():args[0]);
        }

        String gameArgs = "CD ${game_directory}\n\""+args[3].replace("|"," ")+"\" -Xmx"+ getMaxMemory() +"m -XX:+UseG1GC -XX:-UseAdaptiveSizePolicy -XX:-OmitStackTraceInFastThrow -Dfml.ignoreInvalidMinecraftCertificates=True -Dfml.ignorePatchDiscrepancies=True -Dlog4j2.formatMsgNoLookups=true " + all_args.toString().replace("[","").replace("]","").replace(",","").replace("，",",").replace("${game_directory}","\""+(System.getProperty("os.name").contains("Windows")?(System.getProperty("user.dir").replace("\\","/")+"/.minecraft/versions/"+args[0]+"\"").replace("/","\\"):System.getProperty("user.dir").replace("\\","/")+"/.minecraft/versions/"+args[0]+"\""));
        gameArgs = gameArgs.replace("${natives_directory}", "\""+(System.getProperty("os.name").contains("Windows")?(System.getProperty("user.dir").replace("\\","/")+"/.minecraft/versions/"+args[0]+"/natives\"").replace("/","\\"):System.getProperty("user.dir").replace("\\","/")+"/.minecraft/versions/"+args[0]+"/natives\""));
        gameArgs = gameArgs.replace("${launcher_name}","CML");
        gameArgs = gameArgs.replace("${launcher_version}","1.0.0");
        gameArgs = gameArgs.replace("${classpath}",getLibraries(json,args[0],System.getProperty("user.dir").replace("\\","/")+"/CML/tmp/forge-installer.jar"));
        gameArgs = gameArgs.replace("${auth_player_name}",zn);
        gameArgs = gameArgs.replace("${version_name}",args[0]);
        gameArgs = gameArgs.replace("${game_directory}","\""+(System.getProperty("os.name").contains("Windows")?(System.getProperty("user.dir").replace("\\","/")+"/.minecraft/versions/"+args[0]+"\"").replace("/","\\"):System.getProperty("user.dir").replace("\\","/")+"/.minecraft/versions/"+args[0]+"\""));
        gameArgs = gameArgs.replace("${assets_root}","\""+(System.getProperty("os.name").contains("Windows")?(System.getProperty("user.dir").replace("\\","/")+"/.minecraft/assets\"").replace("/","\\"):System.getProperty("user.dir").replace("\\","/")+"/.minecraft/assets\""));
        gameArgs = gameArgs.replace("${assets_index_name}",json.getAsJsonObject("assetIndex").get("id").getAsString());
        gameArgs = gameArgs.replace("${user_type}",args[1]);
        gameArgs = gameArgs.replace("${version_type}","CML");
        gameArgs = gameArgs.replace("${auth_uuid}", getUser(json2.getAsJsonArray("users"),zn).get("uuid").getAsString());
        gameArgs = gameArgs.replace("${auth_access_token}",getUser(json2.getAsJsonArray("users"),zn).get("access_token").getAsString());
        gameArgs = gameArgs.replace("${resolution_width}","854");
        gameArgs = gameArgs.replace("${resolution_height}","480");
        gameArgs = gameArgs.replace("${classpath_separator}",";");
        gameArgs = gameArgs.replace("${library_directory}","\""+libraries_path+"\"");
        gameArgs = gameArgs.replace("${user_properties}","{}");
        gameArgs = gameArgs.replace(" -XstartOnFirstThread","");
        gameArgs = gameArgs.replace(" --demo","");
        gameArgs = gameArgs.replace(" --quickPlaySingleplayer ${quickPlaySingleplayer} --quickPlayMultiplayer ${quickPlayMultiplayer} --quickPlayRealms ${quickPlayRealms}","");
        gameArgs = gameArgs.replace(" -Dos.name=Windows 10"," -Dos.name=\"Windows 10\"");

        if(System.getProperty("os.name").contains("Windows")){
            DelDirFiles(System.getProperty("user.dir").replace("\\","/")+"/.minecraft/versions/"+args[0]+"/natives","dylib");
            DelDirFiles(System.getProperty("user.dir").replace("\\","/")+"/.minecraft/versions/"+args[0]+"/natives","so");
        }
        else if(System.getProperty("os.name").toLowerCase().contains("mac")){
            DelDirFiles(System.getProperty("user.dir").replace("\\","/")+"/.minecraft/versions/"+args[0]+"/natives","dll");
            DelDirFiles(System.getProperty("user.dir").replace("\\","/")+"/.minecraft/versions/"+args[0]+"/natives","so");
        }
        else if(System.getProperty("os.name").contains("Linux")){
            DelDirFiles(System.getProperty("user.dir").replace("\\","/")+"/.minecraft/versions/"+args[0]+"/natives","dll");
            DelDirFiles(System.getProperty("user.dir").replace("\\","/")+"/.minecraft/versions/"+args[0]+"/natives","dylib");
        }
        long end_time = System.currentTimeMillis();
        if(!gameArgs.contains("--userProperties")){
            gameArgs = gameArgs + " --userProperties {}";
        }
        System.out.println("The operation is completed before startup,which takes time: "+(end_time-start_time)/1000+" seconds");
        String zdir = System.getProperty("user.dir");
        if(System.getProperty("os.name").contains("Windows")){
            Files.write(Paths.get("CML/tmp/StartCommand.bat"),gameArgs.getBytes());
            runBatchFile("\""+zdir+"\\CML\\tmp\\StartCommand.bat\"");
            //runBatchFile(gameArgs.split("\n")[1],(System.getProperty("os.name").contains("Windows")?(System.getProperty("user.dir").replace("\\","/")+"/.minecraft/versions/"+args[0]+"\"").replace("/","\\"):System.getProperty("user.dir").replace("\\","/")+"/.minecraft/versions/"+args[0]));
        }
        else{
            Files.write(Paths.get("CML/tmp/StartCommand.sh"),gameArgs.getBytes());
            Runtime.getRuntime().exec("./\""+zdir+"/CML/tmp/StartCommand.sh\"");
        }
        return "";
    }

    public static void exit(String[] args){
        File file = new File("CML/tmp/StartCommands.bat");
        if(file.exists()){
            file.delete();
        }
        File file2 = new File("CML/tmp/StartCommands.sh");
        if(file2.exists()){
            file2.delete();
        }
        System.exit(0);
    }

    public static String us(String[] args) throws Exception {
        String xx = "";
        JsonObject json = JsonParser.parseString(readFile("CML/config.json")).getAsJsonObject();
        JsonArray users = json.getAsJsonArray("users");
        if(args[0].equalsIgnoreCase("list")){
            xx = "Users:";
            for(JsonElement user:users){
                boolean msa = false;
                if(!user.getAsJsonObject().get("access_token").getAsString().equals("0")){
                    msa = true;
                }
                xx = xx+"\n\nUser: "+user.getAsJsonObject().get("name").getAsString()+"\nUser uuid: "+ user.getAsJsonObject().get("uuid").getAsString() +"\nUser access token: "+user.getAsJsonObject().get("access_token").getAsString()+"\nUser msa: "+msa;
            }
            return xx;
        }
        else if(args[0].equalsIgnoreCase("add")){
            if(getUser(users,args[1])==null){
                String UUid = UUID.randomUUID().toString().replace("-","");
                String ACCESS_TOKEN = "0";
                JsonObject user_json = new JsonObject();
                user_json.addProperty("name",args[1]);
                if(args[2].equalsIgnoreCase("msa")){
                    String url;
                    if(Main.cmd==null){
                        Desktop.getDesktop().browse(new URI("https://login.live.com/oauth20_authorize.srf?client_id=00000000402b5328&response_type=code&scope=service%3A%3Auser.auth.xboxlive.com%3A%3AMBI_SSL&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf"));
                        System.out.println("Return url:");
                        Scanner scanner = new Scanner(System.in);
                        url = scanner.nextLine();
                    }
                    else if(Main.input==null){
                        Desktop.getDesktop().browse(new URI("https://login.live.com/oauth20_authorize.srf?client_id=00000000402b5328&response_type=code&scope=service%3A%3Auser.auth.xboxlive.com%3A%3AMBI_SSL&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf"));
                        System.out.println("Return url:");
                        Scanner scanner = new Scanner(System.in);
                        url = scanner.nextLine();
                    }
                    else{
                        url = Main.input;
                    }
                    String code = url.split("\\?")[1].split("&")[0].split("=")[1];
                    String access_token = JsonParser.parseString(GetHttps("https://login.live.com/oauth20_token.srf?client_id=00000000402b5328&code={code}&grant_type=authorization_code&redirect_uri=https://login.live.com/oauth20_desktop.srf&scope=service::user.auth.xboxlive.com::MBI_SSL".replace("{code}",code))).getAsJsonObject().get("access_token").getAsString();
                    JsonObject XBL_JSON = JsonParser.parseString(sendPostRequest("https://user.auth.xboxlive.com:443/user/authenticate","{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"{ac}\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}".replace("{ac}",access_token))).getAsJsonObject();
                    String XBL_Token = XBL_JSON.get("Token").getAsString();
                    String uhs = XBL_JSON.getAsJsonObject("DisplayClaims").get("xui").getAsJsonArray().get(0).getAsJsonObject().get("uhs").getAsString();
                    JsonObject XSTS_JSON = JsonParser.parseString(sendPostRequest("https://xsts.auth.xboxlive.com/xsts/authorize","{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"{xbl_token}\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}".replace("{xbl_token}",XBL_Token))).getAsJsonObject();
                    String XSTS_Token = XSTS_JSON.get("Token").getAsString();
                    JsonObject AC_JSON = JsonParser.parseString(sendPostRequest("https://api.minecraftservices.com/authentication/login_with_xbox","{\"identityToken\":\"XBL3.0 x={uhs};{xsts_token}\"}".replace("{uhs}",uhs).replace("{xsts_token}",XSTS_Token))).getAsJsonObject();
                    ACCESS_TOKEN = AC_JSON.get("access_token").getAsString();
                    JsonObject Minecraft_ProFile = JsonParser.parseString(sendGetRequest("https://api.minecraftservices.com/minecraft/profile",ACCESS_TOKEN)).getAsJsonObject();
                    UUid = Minecraft_ProFile.get("id").getAsString();
                }
                user_json.addProperty("uuid",UUid);
                user_json.addProperty("access_token",ACCESS_TOKEN);
                users.add(user_json);
                Files.write(Paths.get("CML/config.json"),json.toString().getBytes());
            }
            else{
                for (int i = 0; i < users.size(); i++) {
                    JsonObject user = users.get(i).getAsJsonObject();
                    if (user.get("name").getAsString().equals(args[1])) {
                        users.remove(i);
                        break;
                    }
                }

                String UUid = UUID.randomUUID().toString().replace("-","");
                String ACCESS_TOKEN = "0";
                JsonObject user_json = new JsonObject();
                user_json.addProperty("name",args[1]);
                if(args[2].equalsIgnoreCase("msa")){
                    String url;
                    if(Main.cmd==null){
                        Desktop.getDesktop().browse(new URI("https://login.live.com/oauth20_authorize.srf?client_id=00000000402b5328&response_type=code&scope=service%3A%3Auser.auth.xboxlive.com%3A%3AMBI_SSL&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf"));
                        System.out.println("Return url:");
                        Scanner scanner = new Scanner(System.in);
                        url = scanner.nextLine();
                    }
                    else if(Main.input==null){
                        Desktop.getDesktop().browse(new URI("https://login.live.com/oauth20_authorize.srf?client_id=00000000402b5328&response_type=code&scope=service%3A%3Auser.auth.xboxlive.com%3A%3AMBI_SSL&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf"));
                        System.out.println("Return url:");
                        Scanner scanner = new Scanner(System.in);
                        url = scanner.nextLine();
                    }
                    else{
                        url = Main.input;
                    }
                    String code = url.split("\\?")[1].split("&")[0].split("=")[1];
                    String access_token = JsonParser.parseString(GetHttps("https://login.live.com/oauth20_token.srf?client_id=00000000402b5328&code={code}&grant_type=authorization_code&redirect_uri=https://login.live.com/oauth20_desktop.srf&scope=service::user.auth.xboxlive.com::MBI_SSL".replace("{code}",code))).getAsJsonObject().get("access_token").getAsString();
                    JsonObject XBL_JSON = JsonParser.parseString(sendPostRequest("https://user.auth.xboxlive.com:443/user/authenticate","{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"{ac}\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}".replace("{ac}",access_token))).getAsJsonObject();
                    String XBL_Token = XBL_JSON.get("Token").getAsString();
                    String uhs = XBL_JSON.getAsJsonObject("DisplayClaims").get("xui").getAsJsonArray().get(0).getAsJsonObject().get("uhs").getAsString();
                    JsonObject XSTS_JSON = JsonParser.parseString(sendPostRequest("https://xsts.auth.xboxlive.com/xsts/authorize","{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"{xbl_token}\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}".replace("{xbl_token}",XBL_Token))).getAsJsonObject();
                    String XSTS_Token = XSTS_JSON.get("Token").getAsString();
                    JsonObject AC_JSON = JsonParser.parseString(sendPostRequest("https://api.minecraftservices.com/authentication/login_with_xbox","{\"identityToken\":\"XBL3.0 x={uhs};{xsts_token}\"}".replace("{uhs}",uhs).replace("{xsts_token}",XSTS_Token))).getAsJsonObject();
                    ACCESS_TOKEN = AC_JSON.get("access_token").getAsString();
                    JsonObject Minecraft_ProFile = JsonParser.parseString(sendGetRequest("https://api.minecraftservices.com/minecraft/profile",ACCESS_TOKEN)).getAsJsonObject();
                    UUid = Minecraft_ProFile.get("id").getAsString();
                }
                user_json.addProperty("uuid",UUid);
                user_json.addProperty("access_token",ACCESS_TOKEN);

                Files.write(Paths.get("CML/config.json"),json.toString().getBytes());
            }
        }
        else if(args[0].equalsIgnoreCase("del")){
            for (int i = 0; i < users.size(); i++) {
                JsonObject user = users.get(i).getAsJsonObject();
                if (user.get("name").getAsString().equals(args[1])) {
                    users.remove(i);
                    break;
                }
            }
            Files.write(Paths.get("CML/config.json"),json.toString().getBytes());
        }
        return xx;
    }

    public static String mm(String[] args) throws IOException {
        String xx = "";
        if(args[0].equalsIgnoreCase("list")){
            xx = "The maximum heap memory is {mm} MB.".replace("{mm}",String.valueOf(getMaxMemory()));
        }
        else if(args[0].equalsIgnoreCase("set")){
            setMaxMemory(Integer.parseInt(args[1]));
            xx = "Done.";
        }
        return xx;
    }

    public static String lav(String[] args){
        String xx = "Version   Type        Time";
        JsonObject versions = JsonParser.parseString(Main.MC_VERSIONS).getAsJsonObject();
        for (JsonElement version:versions.getAsJsonArray("versions")) {
            xx = xx+"\n"+version.getAsJsonObject().get("id").getAsString() + "    "+version.getAsJsonObject().get("type").getAsString()+"    "+version.getAsJsonObject().get("releaseTime").getAsString();
        }
        return xx;
    }

    public static String msa(String[] args) throws Exception {
        String code = args[0].split("\\?")[1].split("&")[0].split("=")[1];
        String access_token = JsonParser.parseString(GetHttps("https://login.live.com/oauth20_token.srf?client_id=00000000402b5328&code={code}&grant_type=authorization_code&redirect_uri=https://login.live.com/oauth20_desktop.srf&scope=service::user.auth.xboxlive.com::MBI_SSL".replace("{code}",code))).getAsJsonObject().get("access_token").getAsString();
        JsonObject XBL_JSON = JsonParser.parseString(sendPostRequest("https://user.auth.xboxlive.com:443/user/authenticate","{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"{ac}\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}".replace("{ac}",access_token))).getAsJsonObject();
        String XBL_Token = XBL_JSON.get("Token").getAsString();
        String uhs = XBL_JSON.getAsJsonObject("DisplayClaims").get("xui").getAsJsonArray().get(0).getAsJsonObject().get("uhs").getAsString();
        JsonObject XSTS_JSON = JsonParser.parseString(sendPostRequest("https://xsts.auth.xboxlive.com/xsts/authorize","{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"{xbl_token}\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}".replace("{xbl_token}",XBL_Token))).getAsJsonObject();
        String XSTS_Token = XSTS_JSON.get("Token").getAsString();
        JsonObject AC_JSON = JsonParser.parseString(sendPostRequest("https://api.minecraftservices.com/authentication/login_with_xbox","{\"identityToken\":\"XBL3.0 x={uhs};{xsts_token}\"}".replace("{uhs}",uhs).replace("{xsts_token}",XSTS_Token))).getAsJsonObject();
        String ACCESS_TOKEN = AC_JSON.get("access_token").getAsString();
        JsonObject Minecraft_ProFile = JsonParser.parseString(sendGetRequest("https://api.minecraftservices.com/minecraft/profile",ACCESS_TOKEN)).getAsJsonObject();
        String UUid = Minecraft_ProFile.get("id").getAsString();
        JsonObject user_json = new JsonObject();
        String name = Minecraft_ProFile.get("name").getAsString();
        user_json.addProperty("name",name);
        user_json.addProperty("uuid",UUid);
        user_json.addProperty("access_token",ACCESS_TOKEN);
        JsonObject json = JsonParser.parseString(readFile("CML/config.json")).getAsJsonObject();
        JsonArray users = json.getAsJsonArray("users");
        users.add(user_json);
        Files.write(Paths.get("CML/config.json"),json.toString().getBytes());
        return "Done. :"+name;
    }

    public static String dt(String[] args){
        File file = new File("CML/tmp");
        if(DelDir(file)){
            file.mkdirs();
            return "Done.";
        }
        else{
            return "Can't delete the tmp dir: CML/tmp";
        }
    }
}
