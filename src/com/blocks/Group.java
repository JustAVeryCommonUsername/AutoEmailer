package com.blocks;


import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds information about a group of blocks, including its name, color, and held block types<br>
 *     Add the VARIABLE block base to a group to have it autofill with the available variables
 *     and variable creation block
 */
public record Group(String name, Color color, List<BlockBase> blocks) {

    /**
     * The global groups that are used by all programs
     */
    public static List<Group> groups = new ArrayList<>();

    public Group(String name, Color color) {
        this(name, color, new ArrayList<>());
    }

    public Group(String name, Color color, List<BlockBase> blocks) {
        this.name = name;
        this.color = color;
        this.blocks = blocks;

        groups.add(this);
    }

    /*
    Prevent infinite recursion
     */
    @Override
    public String toString() {
        return String.format("Group{name=%s, color=%s}", name, color);
    }
}
