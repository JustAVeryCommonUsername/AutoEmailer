package com.blocks;

import java.util.List;

/**
 * Points to a specific block location
 */
public class BlockPointer {
    public Program program;
    public Block block;
    public int index, body = -1;
    public boolean underMain;

    /**
     * Creates a new block pointer that points to a held block in a control block
     */
    public BlockPointer(Program program, ControlBlock block, int body, int index) {
        this(program, block, index, false);
        this.body = body;
    }

    /**
     * Creates a new block pointer that points to an input in a block.<br>
     * if <code>index</code> is -1, that signifies that it points to the block itself.<br>
     */
    public BlockPointer(Program program, Block block, int index, boolean underMain) {
        this.program = program;
        this.block = block;
        this.index = index;
        this.underMain = underMain;
    }

    public boolean isInput() {
        return index != -1 && body == -1 && !underMain;
    }

    /**
     * Sets the value at the current input
     */
    public void setInput(Object object) {
        if (isInput()) {
            block.getInputs()[index] = object;
        }
    }

    /**
     * Sets the blocks at the index in the program or control block
     */
    public void setBlocks(List<Block> blocks) {
        List<Block> held;
        if (body == -1)
            held = program.getBlocks();
        else
            held = ((ControlBlock) block).getHeldBlocks().get(body);
        if (index <= held.size())
            held.addAll(index, blocks);
    }

    /**
     * Removes the block from the program or control block
     */
    public void remove() {
        if (underMain) {
            if (index < program.getBlocks().size())
                program.getBlocks().remove(index);
        } else {
            List<Block> held = ((ControlBlock) block).getHeldBlocks().get(body);
            if (index < held.size())
                held.remove(index);
        }
    }

    /**
     * Gets the value at the current location
     */
    public Object get() {
        if (index == -1)
            return block;

        if (underMain) {
            if (program.getBlocks().size() > index)
                return program.getBlocks().get(index);
        } else {
            if (body == -1) {
                return block.getInputs()[index];
            } else {
                List<Block> held = ((ControlBlock) block).getHeldBlocks().get(body);
                if (held.size() > index) return held.get(index);
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return String.format("BlockPointer[block=%s,index=%s,body=%s,underMain=%s]", block, index, body, underMain);
    }
}
