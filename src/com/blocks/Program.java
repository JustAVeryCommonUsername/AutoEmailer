package com.blocks;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Holds information about a program, including it's held blocks and state
 */
public class Program {
    /**
     * Stores the values of the global variables used within programs
     */
    public static HashMap<String, Object> variables = new HashMap<>();

    private final List<Block> blocks;
    private Thread thread;
    private volatile boolean running;

    /**
     * Constructs an empty program
     */
    public Program() {
        this(new ArrayList<>());
    }

    /**
     * Constructs a new program that contains the given blocks
     */
    public Program(List<Block> blocks) {
        this.blocks = blocks;
    }

    /**
     * Constructs a new program from the given json array
     */
    public Program(JSONArray json) {
        blocks = getBlockListFromJSON(json);
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public Thread getThread() {
        return thread;
    }

    public boolean isRunning() {return running;}

    /**
     * Runs the program in a new thread
     */
    public void run() {
        if (thread == null || !thread.isAlive()) {
            running = true;
            thread = new Thread(() -> {
                for (Block block : blocks) {
                    block.invoke();
                }
                running = false;
            });
            thread.start();
        }
    }

    /**
     * Saves the program into a json array
     */
    public JSONArray toJSON() {
        return saveBlockListToJSON(blocks);
    }

    /**
     * Gets a list of blocks from the given json array
     */
    static List<Block> getBlockListFromJSON(JSONArray array) {
        List<Block> blocks = new ArrayList<>();
        for(Object obj : array) {
            JSONObject jObj = (JSONObject) obj;
            String key = jObj.keys().next();
            BlockBase base = BlockBase.fromBaseCode(key);
            if (base.isControlBlock())
                blocks.add(new ControlBlock(base, jObj.getJSONObject(key)));
            else
                blocks.add(new Block(base, jObj.getJSONArray(key)));
        }
        return blocks;
    }

    /**
     * Saves a list of blocks to a JSON array
     */
    static JSONArray saveBlockListToJSON(List<Block> blocks) {
        JSONArray json = new JSONArray();
        blocks.forEach((b) -> { if(b.getBase().getType() != BlockBase.BlockType.SHADOW) json.put(b.toJSON()); });
        return json;
    }
}

/*
TODO:
    Variables can be created and deleted
    Scaling is centered on the cursor
    Blocks look more polished and clean
    Custom control blocks
    Blocks and block inputs have descriptions when hovered over
FIXME:
    Block input bounds lose precision lower down
    Block hovering over control block flickers around it
    Text rendering loses precision the longer it is
    Input text field is rendered over block selection
 */