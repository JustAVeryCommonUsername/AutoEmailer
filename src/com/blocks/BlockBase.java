package com.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.blocks.BlockBase.BlockType.FIELD;

/**
 * A BlockBase is a constant that holds information about a block type, including it's internal code,
 * color, inputs, and labels.
 */
public class BlockBase {
    private final BlockType type;
    private Group group;
    private List<InputType> inputs;
    private List<String> labels;
    private Consumer<Object[]> normalCode;
    private Function<Object[], Object> fieldCode;

    public static final BlockBase IF = new BlockBase(BlockType.IF);
    public static final BlockBase IF_ELSE = new BlockBase(BlockType.IF_ELSE);
    public static final BlockBase WHILE = new BlockBase(BlockType.WHILE);
    public static final BlockBase VARIABLE = new BlockBase(BlockType.VARIABLE);
    static final BlockBase SHADOW = new BlockBase(BlockType.SHADOW);
    static final BlockBase VAR_CREATE = new BlockBase(BlockType.VAR_CREATE);

    private BlockBase(BlockType type) {
        this.type = type;

        switch (type) {
            case IF, IF_ELSE -> {
                labels = List.of("if", "");
                inputs = List.of(InputType.BOOLEAN);
            }
            case WHILE -> {
                labels = List.of("while", "");
                inputs = List.of(InputType.BOOLEAN);
            }
            case VARIABLE, SHADOW -> {
                labels = new ArrayList<>();
                inputs = new ArrayList<>();
            }
            case VAR_CREATE -> {
                labels = List.of("Create variable");
                inputs = new ArrayList<>();
            }
        }
    }

    private BlockBase(List<InputType> inputs, List<String> labels, BlockType type) {
        this(type);
        this.inputs = inputs;
        this.labels = labels;
    }

    /**
     * Constructs a normal block.<br>
     *     Normal blocks take input to run code with.
     */
    public BlockBase(List<InputType> inputs, List<String> labels, Consumer<Object[]> normalCode) {
        this(inputs, labels, BlockType.NORMAL);
        this.normalCode = normalCode;
    }

    /**
     * Constructs a field block.<br>
     *     Field blocks take input to run code with and also act as input for other blocks.
     */
    public BlockBase(List<InputType> inputs, List<String> labels, Function<Object[], Object> fieldCode) {
        this(inputs, labels, FIELD);
        this.fieldCode = fieldCode;
    }

    /**
     * Gets the block base from the given code.<br>
     *     Format: Group-Id.Block-Id<br>
     *     Example: 4.3
     */
    public static BlockBase fromBaseCode(String code) {
        if (code.equalsIgnoreCase("variable"))
            return VARIABLE;
        String[] splitCode = code.split("\\.");
        return Group.groups.get(Integer.parseInt(splitCode[0])).blocks().get(Integer.parseInt(splitCode[1]));
    }

    /**
     * Gets the base code
     * @see #fromBaseCode(String)
     */
    public String getBaseCode() {
        return Group.groups.indexOf(getGroup()) + "." + getGroup().blocks().indexOf(this);
    }

    public BlockType getType() {
        return type;
    }

    public Group getGroup() {
        if (type == BlockType.VAR_CREATE)
            return VARIABLE.getGroup();

        if(group == null) {
            outer: for (Group group : Group.groups) {
                for (BlockBase block : group.blocks()) {
                    if (block == this) {
                        this.group = group;
                        break outer;
                    }
                }
            }
        }
        return group;
    }

    public List<InputType> getInputs() {
        return inputs;
    }

    public List<String> getLabels() {
        return labels;
    }

    public Consumer<Object[]> getNormalCode() {
        return normalCode;
    }

    public Function<Object[], Object> getFieldCode() {
        return fieldCode;
    }

    public boolean isControlBlock() {
        return switch (type) {
            case IF, IF_ELSE, WHILE -> true;
            default -> false;
        };
    }

    public boolean isFieldBlock() {
        return type == BlockType.FIELD || type == BlockType.VARIABLE;
    }

    public enum InputType {
        DOUBLE,
        INTEGER,
        BOOLEAN,
        STRING,
        LIST,
        ANY
    }

    public enum BlockType {
        NORMAL,
        FIELD,
        IF,
        IF_ELSE,
        WHILE,
        VARIABLE,
        SHADOW,
        VAR_CREATE
    }
}
