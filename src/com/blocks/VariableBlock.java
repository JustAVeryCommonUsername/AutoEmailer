package com.blocks;

import org.json.JSONObject;

public class VariableBlock extends Block {
    private String variable;

    public VariableBlock(String variable) {
        super(BlockBase.VARIABLE);
        setVariable(variable);
    }

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
        computeBlockSize();
    }

    @Override
    public Object invoke() {
        return Program.variables.get(variable);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("variable", variable);
        return json;
    }

    @Override
    void computeBlockSize() {
        if (variable != null) {
            width = BlockPanel.FONT_METRICS.stringWidth(variable) + 9;
        }
    }
}
