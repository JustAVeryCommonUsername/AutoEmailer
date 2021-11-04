package com.blocks;

public class ShadowBlock extends Block {
    private Object inputValue;
    private Block shadowedBlock;

    public ShadowBlock(Block shadowedBlock) {
        super(BlockBase.SHADOW);
        this.shadowedBlock = switch (shadowedBlock.getBase().getType()) {
            case NORMAL, FIELD -> new Block(shadowedBlock.getBase());
            case IF, IF_ELSE, WHILE -> new ControlBlock(shadowedBlock.getBase());
            case VARIABLE -> new VariableBlock(((VariableBlock) shadowedBlock).getVariable());
            default -> null;
        };
        computeBlockSize();
    }

    public ShadowBlock(Block shadowedBlock, Object inputValue) {
        this(shadowedBlock);
        this.inputValue = inputValue;
    }

    public Object getInputValue() {
        return inputValue;
    }

    public Block getShadowedBlock() {
        return shadowedBlock;
    }

    @Override
    public Object invoke() {
        return inputValue instanceof Block ? ((Block) inputValue).invoke() : inputValue;
    }

    @Override
    void computeBlockSize() {
        if (shadowedBlock != null) {
            shadowedBlock.computeBlockSize();
            width = shadowedBlock.width;
            height = shadowedBlock.height;
        }
    }
}
