package org.ksmcbrigade;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.ksmcbrigade.Utils.*;

public class Main {

    public static String cmd;
    public static String input;

    public static String DOWNLOAD_URL = "https://bmclapi2.bangbang93.com";
    public static String MC_VERSIONS;

    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        init();
        cmd = getCommand(args);
        input = getInput(args);
        if(cmd==null){
            while (true){
                System.out.print("Command:");
                Scanner scanner = new Scanner(System.in);
                System.out.println(ExecuteCommand(scanner.nextLine()));
            }
        }
        else{
            System.out.println("\n"+ExecuteCommand(cmd));
        }
    }

    public static void init() throws IOException {
        String[] dirs = new String[]{"CML","CML/tmp",".minecraft",".minecraft/versions",".minecraft/libraries",".minecraft/assets",".minecraft/assets/indexes",".minecraft/assets/objects"};
        for(String dir:dirs){
            if(!new File(dir).exists()){
                new File(dir).mkdirs();
            }
        }

        if(!new File("CML/config.json").exists()){
            JsonObject json = new JsonObject();
            JsonArray jsonArray = new JsonArray();
            json.add("users",jsonArray);
            json.addProperty("maxMemory",2048);
            Files.write(Paths.get("CML/config.json"),json.toString().getBytes());
        }

        if(!new File(".minecraft/launcher_profiles.json").exists()){  //for forge
            JsonObject json = new JsonObject();
            json.add("profiles",new JsonObject());
            Files.write(Paths.get(".minecraft/launcher_profiles.json"),json.toString().getBytes());
        }

        MC_VERSIONS = Utils.GetHttps("http://launchermeta.mojang.com/mc/game/version_manifest_v2.json");
        System.out.println("CML Version: 1.0");
        System.out.println("CML Use BMCL-API And Vanilla Download API.");
        System.out.println("| = \" \"");
        System.out.println("Welcome to CML!");
    }

    public static String ExecuteCommand(String command) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String RCommand;
        if(command.contains(" ")){
            RCommand = command.split(" ")[0].toLowerCase();
        }
        else{
         RCommand = (command+"").split(" ")[0].toLowerCase();
        }
        if(FindCommand(RCommand)!=-1){
            String[] args = command.replaceAll("(?i)"+RCommand+" ","").split(" ");
            return CommandManager.run(RCommand,args);
        }
        else{
            return "Can't found command: "+command;
        }
    }
}