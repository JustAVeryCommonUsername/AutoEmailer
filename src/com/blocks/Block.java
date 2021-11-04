package com.blocks;

import com.automailer.Main;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.blocks.BlockBase.InputType.DOUBLE;
import static com.blocks.BlockPanel.FONT_METRICS;

/**
 * Holds information about a block state, including its base type and current inputs,
 */
public class Block {
    private final BlockBase base;
    private final Object[] inputs;
    int width = 5, height = 25;
    Shape bounds = new Polygon();
    Shape[] inputBounds;

    /**
     * Constructs a new block with a base type
     */
    public Block(BlockBase base) {
        this.base = base;
        List<BlockBase.InputType> inputTypes = base.getInputs();
        inputs = new Object[inputTypes.size()];
        for (int i = 0; i < inputTypes.size(); i++) {
                inputs[i] = getDefaultInput(inputTypes.get(i));
        }

        if (Main.gui != null)
            computeBlockSize();
    }

    /**
     * Constructs a block with a base code from the given json array
     */
    public Block(BlockBase base, JSONArray json) {
        this(base);
        Iterator<Object> elements = json.iterator();
        for (int i = 0; i < inputs.length; i++) {
            Object element = elements.next();
            if (element instanceof BigDecimal) {
                inputs[i] = ((BigDecimal) element).doubleValue();
            } else if (element instanceof JSONObject) {
                // Parse inner input blocks
                JSONObject obj = (JSONObject) element;
                String baseCode = obj.keys().next();
                BlockBase inputBase = BlockBase.fromBaseCode(baseCode);
                if (inputBase.getType() == BlockBase.BlockType.VARIABLE) {
                     inputs[i] = new VariableBlock(obj.getString("variable"));
                } else {
                    inputs[i] = new Block(inputBase, obj.getJSONArray(baseCode));
                }
            } else {
                inputs[i] = element;
            }
        }
    }

    public BlockBase getBase() {
        return base;
    }

    public Object[] getInputs() {
        return inputs;
    }

    /**
     * Invokes the block and all it's held blocks and inputs
     */
    public Object invoke() {
        switch(base.getType()) {
            case NORMAL:
                base.getNormalCode().accept(invokeInputs());
                return null;
            case FIELD:
                return base.getFieldCode().apply(invokeInputs());
            default:
                return null;
        }
    }

    /**
     * Returns a new array of inputs. For each input it either stays the same if it was a value,
     * or returns the output from an invoked field block.
     */
    public Object[] invokeInputs() {
        Object[] newInputs = new Object[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            Object input = inputs[i];
            if (input instanceof Block) {
                Block block = (Block) input;
                newInputs[i] = convert(base.getInputs().get(i), block.invoke());
            } else {
                newInputs[i] = convert(base.getInputs().get(i), input);
            }
        }
        return newInputs;
    }

    /**
     * Saves the block into a json object
     */
    public JSONObject toJSON() {
        JSONArray array = new JSONArray();
        for(Object input : inputs) {
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

        JSONObject json = new JSONObject();
        json.put(getBase().getBaseCode(), array);
        return json;
    }


    /**
     * Computes the drawing dimensions of this block
     */
    void computeBlockSize() {
        if (inputBounds == null)
            inputBounds = new Shape[inputs.length];

        // Compute label widths
        width = 4;
        height = 25;
        for (String label : base.getLabels()) {
            if (!label.isBlank()) width += FONT_METRICS.stringWidth(label) + 8;
        }

        // Compute input widths
        for (Object input : inputs) {
            if (input instanceof Block) {
                Block block = (Block) input;
                block.computeBlockSize();
                width += block.width + 5;

                int newHeight = 25 + (block.height - 18);
                if (height <= newHeight) height = newHeight;
            } else {
                String string = getDisplayText(input);
                int stringWidth = FONT_METRICS.stringWidth(string);
                width += (input.toString().isEmpty() || input instanceof List ? 32 : stringWidth + 10) + 5;
            }
        }
    }

    static String getDisplayText(Object input) {
        String string = input.toString();
        if (input instanceof Double) {
            double number = (Double) input;
            if (number % 1 == 0)
                string = "" + (int) number;
        } else if(input instanceof List) {
            List list = (List) input;
            if (!list.isEmpty())
                string = list.get(0).toString();
            else
                string = "";
        }
        return string;
    }

    static Object getDefaultInput(BlockBase.InputType type) {
        return switch(type) {
            case DOUBLE, INTEGER -> 0;
            case BOOLEAN -> false;
            case LIST -> new ArrayList<>();
            case STRING, ANY -> "";
        };
    }

    static Object convert(BlockBase.InputType type, Object obj) {
        if (obj instanceof Double) {
            double number = (double) obj;
            return switch (type) {
                case DOUBLE, ANY -> number;
                case INTEGER -> (int) number;
                case STRING -> "" + number;
                case BOOLEAN -> number > 0;
                case LIST -> List.of(number);
            };
        } else if (obj instanceof Integer) {
            int number = (int) obj;
            return switch (type) {
                case DOUBLE -> (double) number;
                case INTEGER, ANY -> number;
                case STRING -> "" + number;
                case BOOLEAN -> number > 0;
                case LIST -> List.of(number);
            };
        } else if (obj instanceof String) {
            String string = (String) obj;
            try {
                return switch (type) {
                    case DOUBLE -> Double.parseDouble(string);
                    case INTEGER -> Integer.parseInt(string);
                    case STRING, ANY -> string;
                    case BOOLEAN -> Boolean.valueOf(string);
                    case LIST -> string.isEmpty() ? new ArrayList<>() : List.of(string);
                };
            } catch(NumberFormatException e) {
                return type == DOUBLE ? 0.0 : 0;
            }
        } else if (obj instanceof Boolean) {
            boolean bool = (boolean) obj;
            return switch (type) {
                case DOUBLE -> bool ? 1.0 : 0.0;
                case INTEGER -> bool ? 1 : 0;
                case STRING -> "" + bool;
                case BOOLEAN, ANY -> bool;
                case LIST -> List.of(bool);
            };
        } else if (obj instanceof List) {
            List<Object> list = (List<Object>) obj;
            return switch (type) {
                case DOUBLE, INTEGER -> list.size();
                case STRING -> list.toString();
                case BOOLEAN -> !list.isEmpty();
                case LIST, ANY -> list;
            };
        }
        throw new IllegalArgumentException("Unexpected input type " + obj.getClass().getName());
    }
}
