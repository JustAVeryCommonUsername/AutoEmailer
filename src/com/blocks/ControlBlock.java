package com.blocks;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds information about a control block, including the blocks it contains
 */
public class ControlBlock extends Block {
    private List<List<Block>> heldBlocks = new ArrayList<>();
    int inputHeight;
    int[] bodyHeights;
    Shape[] bodyBounds;

    /**
     * Constructs a new control block with a base type
     */
    public ControlBlock(BlockBase base) {
        super(base);
        heldBlocks.add(new ArrayList<>());
        if (getBase().getType() == BlockBase.BlockType.IF_ELSE)
            heldBlocks.add(new ArrayList<>());
    }

    /**
     * Constructs a control block with a base code from the given json array
     */
    public ControlBlock(BlockBase base, JSONObject json) {
        super(base, json.getJSONArray("input"));

        heldBlocks.add(Program.getBlockListFromJSON(json.getJSONArray("body1")));
        if(base.getType() == BlockBase.BlockType.IF_ELSE) {
            heldBlocks.add(Program.getBlockListFromJSON(json.getJSONArray("body2")));
        }
    }

    /**
     * Returns the list of held blocks<br>
     *     Will return 1 list for if and while blocks, but 2 for if-else blocks
     */
    public List<List<Block>> getHeldBlocks() {
        return heldBlocks;
    }

    @Override
    public Object invoke() {
        switch(getBase().getType()) {
            case IF:
                if ((boolean) invokeInputs()[0]) {
                    for (Block block : heldBlocks.get(0))
                        block.invoke();
                }
                break;
            case IF_ELSE:
                if ((boolean) invokeInputs()[0]) {
                    for (Block block : heldBlocks.get(0))
                        block.invoke();
                } else {
                    for (Block block : heldBlocks.get(1))
                        block.invoke();
                }
                break;
            case WHILE:
                while((boolean) invokeInputs()[0]) {
                    for (Block block : heldBlocks.get(0))
                        block.invoke();
                }
                break;
        }
        return null;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        JSONArray array = new JSONArray();
        for(Object input : getInputs()) {
            if (input instanceof Block) {
                Block block = (Block) input;
                if (block.getBase().getType() == BlockBase.BlockType.SHADOW) {
                    array.put(((ShadowBlock)block).getInputValue());
                } else {
                    array.put(block.toJSON());
                }
            } else {
                array.put(input);
            }
        }

        json.put("input", array);

        json.put("body1", Program.saveBlockListToJSON(heldBlocks.get(0)));
        if(getBase().getType() == BlockBase.BlockType.IF_ELSE) {
            json.put("body2", Program.saveBlockListToJSON(heldBlocks.get(1)));
        }
        return new JSONObject().put(getBase().getBaseCode(), json);
    }

    @Override
    void computeBlockSize() {
        super.computeBlockSize();
        inputHeight = height;
        if (heldBlocks == null) {
            heldBlocks = new ArrayList<>();
            heldBlocks.add(new ArrayList<>());
            if (getBase().getType() == BlockBase.BlockType.IF_ELSE) heldBlocks.add(new ArrayList<>());
        }
        if (bodyHeights == null)
            bodyHeights = new int[heldBlocks.size()];
        if (bodyBounds == null)
            bodyBounds = new Shape[heldBlocks.size()];
        for (int i = 0; i < heldBlocks.size(); i++) {
            List<Block> blocks = heldBlocks.get(i);
            int height = 0;
            for(Block block : blocks) {
                block.computeBlockSize();
                height += block.height;
            }
            if (blocks.isEmpty())
                bodyHeights[i] = 25;
            else
                bodyHeights[i] = height;
            this.height += bodyHeights[i] + (i == heldBlocks.size() - 1 ? 15 : 25);
        }
    }
}
