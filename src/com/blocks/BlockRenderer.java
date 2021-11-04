package com.blocks;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.blocks.BlockBase.BlockType.*;
import static com.blocks.BlockPanel.FONT_METRICS;

/**
 * Handles rendering blocks to the screen
 */
public class BlockRenderer {
    private BlockPanel panel;
    Shape startBounds = new Polygon();

    /**
     * Creates a new block renderer attached to the specified block panel
     */
    public BlockRenderer(BlockPanel panel) {
        this.panel = panel;
    }

    /**
     * Draws the start block to the specified graphics
     */
    public void drawStartBlock(Graphics2D g) {
        int stringWidth = FONT_METRICS.stringWidth("Start");
        g.setColor(Color.LIGHT_GRAY);
        Polygon polygon = new Polygon(new int[]{0, stringWidth + 4, stringWidth + 4, 20, 20, 10, 10, 0}, new int[]{0, 0, 25, 25, 30, 30, 25, 25}, 8);
        startBounds = getRenderingTransformation(g).createTransformedShape(polygon);
        g.fillPolygon(polygon);
        g.setColor(Color.BLACK);
        g.drawPolygon(polygon);
        g.drawString("Start", 2, 16);
    }

    /**
     * Draws a block
     * @param g The graphics to draw the block onto
     * @param block The block to be drawn
     * @param x The x coordinate to draw the block
     * @param y The y coordinate to draw the block
     * @param opacity The opacity of the block
     */
    public void drawBlock(Graphics2D g, Block block, int x, int y, int opacity) {
        // Setup commonly used references
        BlockBase base = block.getBase();
        boolean shadow = base.getType() == SHADOW;
        ControlBlock controlBlock = block.getBase().isControlBlock() ? (ControlBlock) block : null;

        // Setup colors
        Color blockColor;
        Color textColor = new Color(0, 0, 0, opacity);
        Color inputColor = new Color(234, 222, 222, opacity);
        Color lineColor = new Color(52, 52, 52, opacity);
        if (shadow) {
            blockColor = new Color(61, 61, 61, 150);
            lineColor = blockColor;
        } else {
            blockColor = base.getGroup().color();
            blockColor = new Color(blockColor.getRed(), blockColor.getGreen(), blockColor.getBlue(), opacity);
        }

        // Transform block bounds to match rendering
        AffineTransform transform = getRenderingTransformation(g);

        // Draw block
        BlockBase.BlockType type;
        if(shadow) {
            Block shadowedBlock = ((ShadowBlock) block).getShadowedBlock();
            if (shadowedBlock.getBase().isControlBlock())
                controlBlock = (ControlBlock) shadowedBlock;
            type = shadowedBlock.getBase().getType();
        } else {
            type = base.getType();
        }
        g.setColor(blockColor);
        switch (type) {
            case NORMAL -> {
                Polygon polygon = new Polygon(new int[]{x, x + 10, x + 10, x + 20, x + 20, x + block.width, x + block.width, x + 20, x + 20, x + 10, x + 10, x},
                        new int[]{y, y, y + 5, y + 5, y, y, y + block.height, y + block.height, y + block.height + 5, y + block.height + 5, y + block.height, y + block.height}, 12);
                g.fill(polygon);
                g.setColor(lineColor);
                g.draw(polygon);
                block.bounds = transform.createTransformedShape(polygon);
            }
            case IF, IF_ELSE, WHILE -> {
                List<Integer> polyX = new ArrayList<>(java.util.List.of(x, x + 10, x + 10, x + 20, x + 20, x + controlBlock.width, x + controlBlock.width, x + 30, x + 30, x + 20, x + 20, x + 10));
                List<Integer> polyY = new ArrayList<>(java.util.List.of(y, y, y + 5, y + 5, y, y, y + controlBlock.inputHeight, y + controlBlock.inputHeight, y + controlBlock.inputHeight + 5, y + controlBlock.inputHeight + 5, y + controlBlock.inputHeight, y + controlBlock.inputHeight));

                int bodyY = y + controlBlock.inputHeight;
                for (int i = 0; i < controlBlock.getHeldBlocks().size(); i++) {
                    int bodyWidth = i == 0 ? controlBlock.width : 100;
                    controlBlock.bodyBounds[i] = transform.createTransformedShape(new Rectangle(x + 10, bodyY,
                            bodyWidth, controlBlock.bodyHeights[i]));
                    bodyY += controlBlock.bodyHeights[i];
                    if (i == controlBlock.getHeldBlocks().size() - 1) {
                        polyX.addAll(java.util.List.of(x + 10, x + 20, x + 20, x + 30, x + 30, x + 100, x + 100, x + 20, x + 20, x + 10, x + 10, x));
                        polyY.addAll(java.util.List.of(bodyY, bodyY, bodyY + 5, bodyY + 5, bodyY, bodyY, bodyY + 15, bodyY + 15
                                , bodyY + 20, bodyY + 20, bodyY + 15, bodyY + 15));
                    } else {
                        polyX.addAll(java.util.List.of(x + 10, x + 20, x + 20, x + 30, x + 30, x + 100, x + 100, x + 30, x + 30, x + 20, x + 20, x + 10));
                        polyY.addAll(java.util.List.of(bodyY, bodyY, bodyY + 5, bodyY + 5, bodyY, bodyY, bodyY + 25, bodyY + 25, bodyY + 30, bodyY + 30, bodyY + 25, bodyY + 25));
                        bodyY += 25;
                    }
                }
                Polygon polygon = new Polygon(polyX.stream().mapToInt(i->i).toArray(), polyY.stream().mapToInt(i->i).toArray(), polyX.size());
                g.fillPolygon(polygon);
                g.setColor(lineColor);
                g.drawPolygon(polygon);
                block.bounds = transform.createTransformedShape(polygon);
                if (!shadow) {
                    if (base.getType() == IF_ELSE) {
                        g.setColor(textColor);
                        g.drawString("else", x + 5, y + controlBlock.inputHeight + controlBlock.bodyHeights[0] + 19);
                    }
                }
            }
            case FIELD, VARIABLE, VAR_CREATE -> {
                g.fillRoundRect(x, y, block.width, block.height, 20, 20);
                g.setColor(lineColor);
                g.drawRoundRect(x, y, block.width, block.height, 20, 20);
                block.bounds = transform.createTransformedShape(new Rectangle(x, y, block.width, block.height));
            }
        }

        // Stop here if the block is a shadow
        if (shadow)
            return;

        g.setColor(textColor);
        if (type == VARIABLE) {
            VariableBlock varBlock = (VariableBlock) block;
            g.drawString(varBlock.getVariable(), x + 5, y + ((block.height + 14) / 2));
        } else {
            // Drawing inputs and labels
            List<String> labels = base.getLabels();
            int inputX = x + (labels.get(0).isBlank() ? 3 : 5);
            int height = controlBlock == null ? block.height : controlBlock.inputHeight;
            Object[] inputs = block.getInputs();

            // Drawing first label
            String label = labels.get(0);
            if (!label.isBlank()) {
                g.drawString(label, inputX, y + ((height + 14) / 2));
                inputX += FONT_METRICS.stringWidth(label) + 5;
            } else {
                inputX += 2;
            }

            for (int i = 0; i < inputs.length; i++) {
                Object input = inputs[i];
                if (input instanceof Block) {
                    Block inputBlock = (Block) input;
                    drawBlock(g, inputBlock, inputX, y + ((height - inputBlock.height) / 2) + 1, opacity);
                    block.inputBounds[i] = inputBlock.bounds;
                    inputX += inputBlock.width + 7;
                } else {
                    // Drawing input values
                    String string = Block.getDisplayText(input);
                    int stringWidth = FONT_METRICS.stringWidth(string);
                    if (string.isEmpty()) stringWidth = 22;
                    g.setColor(inputColor);
                    g.fillRoundRect(inputX, y + ((height - 16) / 2), stringWidth + 10, 18, 20, 20);
                    g.setColor(textColor);
                    if (panel.selectedInput == null || panel.selectedInput.block != block || panel.selectedInput.index != i) {
                        g.drawString(string, inputX + 5, y + ((height + 14) / 2));
                    }
                    block.inputBounds[i] = transform.createTransformedShape(new Rectangle(inputX, y + ((height - 16) / 2), stringWidth + 10, 18));
                    inputX += stringWidth + 15;
                }

                // Drawing labels
                g.setColor(textColor);
                label = labels.get(i + 1);
                if (!label.isBlank()) {
                    g.drawString(label, inputX, y + ((height + 14) / 2));
                    inputX += FONT_METRICS.stringWidth(label) + 5;
                }
            }

            // Draw held blocks
            if (base.isControlBlock()) {
                List<List<Block>> heldBlocks = controlBlock.getHeldBlocks();
                for (int i = 0; i < heldBlocks.size(); i++) {
                    List<Block> blocks = heldBlocks.get(i);
                    int bodyY = y + controlBlock.inputHeight;
                    if (i > 0) bodyY += controlBlock.bodyHeights[i - 1] + 25;

                    for (Block b : blocks) {
                        drawBlock(g, b, x + 10, bodyY, opacity);
                        bodyY += b.height;
                    }
                }
            }
        }
    }

    /**
     * Draws the selection box to the specified graphics
     */
    public void drawSelectionBox(Graphics2D g) {
        // Draw group selection
        g.setColor(Color.LIGHT_GRAY);
        List<Group> groups = Group.groups;
        int longest = 0;
        for (Group group : groups) {
            int length = FONT_METRICS.stringWidth(group.name());
            if (length > longest) longest = length;
        }
        int x = panel.getWidth() - longest - 40;
        int y = 5;
        g.fillRect(x, 0, longest + 40, panel.getHeight());
        panel.groupSelectionBounds = new Rectangle(x, 0, longest + 40, panel.getHeight());
        for (Group group : groups) {
            g.setColor(group.color());
            g.fillRect(x + 5, y, 7, 14);
            g.setColor(Color.BLACK);
            g.drawString(group.name(), x + 20, y + 12);
            y += 20;
        }

        // Draw block selection
        if (panel.selectedGroup != null) {
            g.setColor(new Color(184, 184, 184));
            Optional<Block> optional = panel.blockSelection.stream().max(Comparator.comparingInt(b -> {
                if (b.getBase().isControlBlock() && b.width < 100) return 100;
                else return b.width;
            }));
            int width = 75;
            if (optional.isPresent()) {
                Block longestBlock = optional.get();
                width = longestBlock.width;
                if (longestBlock.getBase().isControlBlock() && width < 100) width = 100;
            }
            x = x - width - 10;
            y = -panel.selectionY + 5;
            g.fillRect(x, 0, width + 10, panel.getHeight());
            panel.blockSelectionBounds = new Rectangle(x, 0, width + 10, panel.getHeight());
            g.setColor(new Color(52, 52, 52));
            g.drawLine(x + width + 10, 0, x + width + 10, panel.getHeight());
            g.setColor(new Color(184, 184, 184));
            for (Block block : panel.blockSelection) {
                drawBlock(g, block, x + 5, y, 255);
                y += block.height + 20;
            }
        }
    }

    private AffineTransform getRenderingTransformation(Graphics2D g) {
        AffineTransform gTransform = g.getTransform();
        AffineTransform transform = new AffineTransform();
        transform.translate(gTransform.getTranslateX() / 1.25, gTransform.getTranslateY() / 1.25);
        transform.scale(gTransform.getScaleX() / 1.25, gTransform.getScaleY() / 1.25);
        return transform;
    }
}
