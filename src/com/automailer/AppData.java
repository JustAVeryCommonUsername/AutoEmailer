package com.automailer;

import com.blocks.Program;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class AppData {
    private String emailAddress;
    private String password;
    private List<Program> programs;

    public AppData(String emailAddress, String password, List<Program> programs) {
        this.emailAddress = emailAddress;
        this.password = password;
        this.programs = programs;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Program> getPrograms() {
        return programs;
    }

    public static AppData getAppData(File file) {
        if (!file.exists())
            return new AppData("", "", new ArrayList<>());

        JSONObject json = new JSONObject();
        try {
            json = new JSONObject(Files.readString(file.toPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String emailAddress = json.getString("emailaddress");
        String password = json.getString("password");

        JSONArray array = json.getJSONArray("programs");
        List<Program> programs = new ArrayList<>();
        for(Object obj : array) {
            programs.add(new Program((JSONArray) obj));
        }

        JSONObject variables = json.getJSONObject("variables");
        variables.keySet().forEach((k) -> Program.variables.put(k, variables.get(k)));

        return new AppData(emailAddress, password, programs);
    }

    public void saveAppData(File file) {
        JSONObject json = new JSONObject();
        json.put("emailaddress", emailAddress);
        json.put("password", password);
        JSONArray array = new JSONArray();
        programs.forEach((p) -> array.put(p.toJSON()));
        json.put("programs", array);
        json.put("variables", Program.variables);

        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(json.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}