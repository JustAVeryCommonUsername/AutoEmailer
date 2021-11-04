package com.blocks;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.blocks.BlockBase.BlockType.VARIABLE;

/**
 * Acts as a component that renderers the specified program
 * along with the block selecting box<br>
 * Also handles reacting to user input and manipulating the blocks
 */
public class BlockPanel extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener
        , ComponentListener, WindowListener {
    static FontMetrics FONT_METRICS = Toolkit.getDefaultToolkit().getFontMetrics(new Font("Oswald", Font.PLAIN, 16));
    static final double MIN_SCALE = 0.5, MAX_SCALE = 5;

    private Program program;
    private BlockRenderer renderer;

    private final List<Block> cursor = new ArrayList<>();
    Group selectedGroup;
    List<Block> blockSelection;
    private Point mouse;
    private boolean up, down, left, right;

    private BlockPointer shadow;
    BlockPointer selectedInput;
    private JTextField inputField;
    int variableStart;

    Rectangle groupSelectionBounds = new Rectangle(), blockSelectionBounds = new Rectangle();

    double scale = 1.25;
    int transX = 0, transY = 0, selectionY = 0, selectionHeight;

    public BlockPanel(Program program, JFrame frame) {
        this.program = program;
        this.renderer = new BlockRenderer(this);

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addKeyListener(this);
        addComponentListener(this);
        frame.addWindowListener(this);

        setFocusable(true);

        Point point = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(point, this);
        mouse = point;

        computeBlockSizes();
    }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
        computeBlockSizes();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setFont(FONT_METRICS.getFont());
        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(1));
        g2d.translate(transX, transY);
        g2d.scale(scale, scale);

        // React to key presses
        boolean selection = blockSelectionBounds.contains(mouse);
        if (up) {
            if (selection) {
                if (selectionY >= 5) selectionY -= 5;
                else selectionY = 0;
            } else {
                transY += 5 * scale;
            }
        }
        if (down) {
            if (selection) {
                if (selectionHeight - selectionY > getHeight()) selectionY += 5;
                else if (selectionHeight > getHeight()) selectionY = selectionHeight - getHeight();
            } else {
                transY -= 5 * scale;
            }
        }
        if (left)
            transX += 5 * scale;
        if (right)
            transX -= 5 * scale;


        int y = 25;
        for (Block block : program.getBlocks()) {
            renderer.drawBlock(g2d, block, 0, y, 255);
            y += block.height;
        }

        renderer.drawStartBlock(g2d);

        g2d.setTransform(getGraphicsConfiguration().getDefaultTransform());

        renderer.drawSelectionBox(g2d);

        if (!cursor.isEmpty()) {
            y = mouse.y;
            for (Block block : cursor) {
                renderer.drawBlock(g2d, block, mouse.x, y, 210);
                y += block.height;
            }
        }
    }

    private void computeBlockSizes() {
        program.getBlocks().forEach(Block::computeBlockSize);
        cursor.forEach(Block::computeBlockSize);
    }

    /**
     * Updates the current block selection for the selected group
     */
    private void updateBlockSelection() {
        List<Block> blocks = new ArrayList<>();
        selectedGroup.blocks().forEach((b) -> {
            if (b.getType() != VARIABLE) {
                blocks.add(b.isControlBlock() ? new ControlBlock(b) : new Block(b));
            }
        });
        if (selectedGroup.blocks().contains(BlockBase.VARIABLE)) {
            blocks.add(new Block(BlockBase.VAR_CREATE));
            Program.variables.keySet().forEach((k) -> blocks.add(new VariableBlock(k)));
        }
        blockSelection = blocks;
        selectionHeight = 30;
        selectionY = 0;
        boolean varStart = true;
        for (Block block : blockSelection) {
            if (block.getBase().getType() == VARIABLE) {
                if (varStart) {
                    variableStart = selectionHeight;
                    varStart = false;
                }
            }
            selectionHeight += block.height + 20;
        }
    }

    /**
     * Gets the block or input at the point on the screen
     * @param block Block to search for input blocks or held blocks from
     * @param index The index of the block in the program
     * @param point Point on the screen
     */
    private BlockPointer getBlockAt(Block block, int index, Point point) {
        if (block.bounds.contains(point)) {
            for (int i = 0; i < block.getInputs().length; i++) {
                Shape inputBounds = block.inputBounds[i];
                if (inputBounds.contains(point)) {
                    Object input = block.getInputs()[i];
                    if (input instanceof Block) {
                        // A block was found in an input
                        BlockPointer pointer = getBlockAt((Block) input, -1, point);
                        if (pointer != null && pointer.underMain) {
                            return new BlockPointer(program, block, i, false);
                        }
                        return pointer;
                    } else {
                        // Non-block input was found
                        return new BlockPointer(program, block, i, false);
                    }
                }
            }
            // Block itself was found, directly under the main program
            return new BlockPointer(program, block, index, true);
        } else if (block.getBase().isControlBlock()) {
            ControlBlock cBlock = (ControlBlock) block;
            for (int i = 0; i < cBlock.getHeldBlocks().size(); i++) {
                List<Block> heldBlocks = cBlock.getHeldBlocks().get(i);
                for (int b = 0; b < heldBlocks.size(); b++) {
                    Block held = heldBlocks.get(b);
                    BlockPointer heldPointer = getBlockAt(held, -1, point);
                    if (heldPointer != null) {
                        if (heldPointer.index != -1) {
                            return heldPointer;
                        } else {
                            // A held block was found
                            return new BlockPointer(program, cBlock, i, b);
                        }
                    }
                }
            }
        }
        // No block was found
        return null;
    }

    /**
     * Gets the block pointer of where the shadow of the block would be placed
     * @param block Block to hover over
     * @param point Point on the screen
     * @param index The index of the block in the program
     */
    private BlockPointer hoverOverBlock(Block block, Point point, int index) {
        BlockPointer pointer = getBlockAt(block, index, point);

        if (pointer != null) {
            // Found an input of a block
            if (!(pointer.get() instanceof Block)) {
                return pointer;
            }
        }

        // Hover around neighbouring blocks
        int x = point.x, y = point.y, pad = (int) (10 * scale);
        Rectangle bounds = block.bounds.getBounds();
        if (x > bounds.x && x < bounds.x + bounds.width) {
            if (y > bounds.y - pad && y < bounds.y + pad) {
                return new BlockPointer(program, block, index, true);
            } else if (y > bounds.y + bounds.height - pad && y < bounds.y + bounds.height + pad) {
                return new BlockPointer(program, block, index + 1, true);
            }
        }

        // Hover over held blocks in the control block
        if (block.getBase().isControlBlock()) {
            ControlBlock cBlock = (ControlBlock) block;
            for (int b = 0; b < cBlock.getHeldBlocks().size(); b++) {
                List<Block> held = cBlock.getHeldBlocks().get(b);
                for (int i = 0; i < held.size(); i++) {
                    BlockPointer newPointer = hoverOverBlock(held.get(i), point, i);
                    if (newPointer != null) {
                        if (newPointer.body == -1) {
                            return new BlockPointer(program, cBlock, b, newPointer.index);
                        } else {
                            return newPointer;
                        }
                    }
                }

                // Hover over empty body
                if (held.isEmpty() && cBlock.bodyBounds[b].contains(point)) {
                    return new BlockPointer(program, cBlock, b, 0);
                }
            }
        }

        return null;
    }

    private void removeShadow() {
        if (shadow != null) {
            if (shadow.isInput()) {
                shadow.setInput(((ShadowBlock) shadow.get()).getInputValue());
            } else {
                shadow.remove();
            }
            shadow = null;
            computeBlockSizes();
        }
    }
    
    private void createInputField() {
        if (inputField == null) {
            Object input = selectedInput.get();
            if (input instanceof List) {
                input = "";
            }

            inputField = new JTextField(Block.getDisplayText(input));
            inputField.setBorder(BorderFactory.createEmptyBorder());
            inputField.setOpaque(false);

            Font font = FONT_METRICS.getFont();
            Font newFont = new Font(font.getFontName(), font.getStyle(), (int) (font.getSize() * scale));
            inputField.setFont(newFont);
            inputField.setDoubleBuffered(false);

            computeInputFieldSize();

            add(inputField);

            inputField.addKeyListener(new KeyListener() {
                public void keyTyped(KeyEvent e) {
                }
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        removeInputField();
                    }
                }
                public void keyReleased(KeyEvent e) {
                }
            });
            inputField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    selectedInput.setInput(inputField.getText());
                    computeInputFieldSize();
                    computeBlockSizes();
                }
                @Override
                public void removeUpdate(DocumentEvent e) {
                    selectedInput.setInput(inputField.getText());
                    computeInputFieldSize();
                    computeBlockSizes();
                }
                @Override
                public void changedUpdate(DocumentEvent e) {
                }
            });
        }
    }

    private void computeInputFieldSize() {
        if (inputField != null) {
            Rectangle bounds = selectedInput.block.inputBounds[selectedInput.index].getBounds();
            bounds.setSize(bounds.width, bounds.height);
            bounds.translate((int) (5 * scale), 0);
            inputField.setBounds(bounds);
        }
    }

    private void removeInputField() {
        if (inputField != null) {
            BlockBase.InputType type = selectedInput.block.getBase().getInputs().get(selectedInput.index);
            selectedInput.setInput(Block.convert(type, inputField.getText()));
            remove(inputField);
            selectedInput = null;
            inputField = null;
            computeBlockSizes();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mousePressed(MouseEvent e) {
        Point point = e.getPoint();

        if (inputField != null && !inputField.getBounds().contains(mouse))
            removeInputField();

        if (groupSelectionBounds.contains(point)) { // Click on group selection
            int groupIndex = (int) Math.floor(((point.getY() - 3) / 20));
            if (groupIndex < 0 || groupIndex >= Group.groups.size())
                return;
            Group group = Group.groups.get(groupIndex);
            if (selectedGroup == group) {
                selectedGroup = null;
            } else {
                selectedGroup = group;
                updateBlockSelection();
            }
        } else if (blockSelectionBounds.contains(point)) { // Click on block selection
            if (cursor.isEmpty()) {
                for (Block block : blockSelection) {
                    Shape bounds = block.bounds;
                    if (bounds.contains(point)) {
                        BlockBase base = block.getBase();
                        cursor.add(switch (base.getType()) {
                            case NORMAL, FIELD, VAR_CREATE -> new Block(base);
                            case IF, IF_ELSE, WHILE -> new ControlBlock(base);
                            case VARIABLE -> new VariableBlock(((VariableBlock) block).getVariable());
                            case SHADOW -> new ShadowBlock(block);
                        });
                    }
                }
            }
        } else { // Click on block
            if (cursor.isEmpty()) {
                BlockPointer pointer = null;
                int blockIndex = 0;
                for (int i = 0; i < program.getBlocks().size(); i++) {
                    Block block = program.getBlocks().get(i);
                    pointer = getBlockAt(block, i, point);
                    if (pointer != null) {
                        blockIndex = i;
                        break;
                    }
                }

                if (pointer == null)
                    return;
                Object input = pointer.get();
                boolean rightClick = e.getButton() == MouseEvent.BUTTON3;

                if (program.isRunning()) {
                    program.getThread().stop();
                }

                if (pointer.underMain) {
                    // Click on block under main program
                    List<Block> blocks = program.getBlocks();
                    blocks = blocks.subList(blockIndex, rightClick ? blockIndex + 1 : blocks.size());
                    cursor.addAll(blocks);
                    blocks.clear();
                } else {
                    if (pointer.body == -1) {
                        if (pointer.block instanceof ShadowBlock) return;

                        // Click on input
                        if (pointer.index != -1 && input instanceof Block) {
                                cursor.add((Block) input);
                                pointer.setInput(Block.getDefaultInput(pointer.block.getBase().getInputs().get(pointer.index)));
                        }
                    } else {
                        // Click on block in a control block
                        List<Block> blocks = ((ControlBlock) pointer.block).getHeldBlocks().get(pointer.body);
                        blocks = blocks.subList(pointer.index, rightClick ? pointer.index + 1 : blocks.size());
                        cursor.addAll(blocks);
                        blocks.clear();
                    }
                }

                computeBlockSizes();
            }
        }
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        Point point = e.getPoint();
        if (blockSelectionBounds.contains(point) || groupSelectionBounds.contains(point)) {
            cursor.clear();
        } else if (!cursor.isEmpty() && shadow != null) {
            if (shadow.isInput()) {
                shadow.setInput(cursor.get(0));
            } else {
                shadow.remove();
                shadow.setBlocks(cursor);
            }
            shadow = null;
            computeBlockSizes();
        }
        cursor.clear();
    }
    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) { }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (!cursor.isEmpty() || groupSelectionBounds.contains(e.getPoint()))
            return;
        double scrollAmount = e.getPreciseWheelRotation() / 10;

        if (inputField != null) {
            removeInputField();
        }

        if (blockSelectionBounds.contains(e.getPoint())) {
            double newScroll = scrollAmount * 200;
            if (scrollAmount < 0) {
                if (selectionY >= -newScroll)
                    selectionY += newScroll;
                else
                    selectionY = 0;
            } else if (selectionHeight - selectionY > getHeight() && scrollAmount > 0) {
                selectionY += newScroll;
                if (selectionHeight - selectionY < getHeight())
                    selectionY = (selectionHeight - getHeight());
            }
        } else {
            double newScale = scale + scrollAmount;
            if (newScale < MIN_SCALE) newScale = MIN_SCALE;
            if (newScale > MAX_SCALE) newScale = MAX_SCALE;
            scale = newScale;
        }
    }
    @Override
    public void mouseDragged(MouseEvent e) {
        if (cursor.isEmpty() && inputField == null) {
            if (groupSelectionBounds.contains(e.getPoint()) || blockSelectionBounds.contains(e.getPoint())) return;
            transX += e.getX() - mouse.x;
            transY += e.getY() - mouse.y;
        }
        mouseMoved(e);
    }
    @Override
    public void mouseMoved(MouseEvent e) {
        mouse = e.getPoint();
        if (groupSelectionBounds.contains(mouse) || blockSelectionBounds.contains(mouse)) {
            removeShadow();
            return;
        }

        // Hover over start block if the program is empty
        if (!cursor.isEmpty() && program.getBlocks().isEmpty() && !cursor.get(0).getBase().isFieldBlock()) {
            int x = mouse.x, y = mouse.y, pad = (int) (10 * scale);
            Rectangle bounds = renderer.startBounds.getBounds();
            if (x >= bounds.x && x <= bounds.x + bounds.getWidth() &&
                    y > bounds.y + bounds.getHeight() - pad && y < bounds.y + bounds.getHeight() + pad) {
                ShadowBlock sBlock = new ShadowBlock(cursor.get(0));
                BlockPointer pointer = new BlockPointer(program, sBlock, 0, true);
                pointer.setBlocks(List.of(sBlock));
                shadow = pointer;
                computeBlockSizes();
                return;
            }
        }

        for (int i = 0; i < program.getBlocks().size(); i++) {
            Block block = program.getBlocks().get(i);

            BlockPointer pointer = hoverOverBlock(block, mouse, i);
            if (pointer != null) {
                if (pointer.get() != null && !(pointer.get() instanceof Block)) {
                    if (cursor.isEmpty()) {
                        removeInputField();
                        selectedInput = pointer;
                        createInputField();
                    } else if (cursor.get(0).getBase().isFieldBlock()) {
                        // Hover over input
                        removeShadow();
                        ShadowBlock sBlock = new ShadowBlock(cursor.get(0), pointer.get());
                        pointer.setInput(sBlock);
                        shadow = pointer;
                    }
                } else if (!cursor.isEmpty() && !pointer.isInput() && !cursor.get(0).getBase().isFieldBlock()) {
                    // Hover around other blocks
                    removeShadow();
                    ShadowBlock sBlock = new ShadowBlock(cursor.get(0));
                    pointer.setBlocks(List.of(sBlock));
                    shadow = pointer;
                }
                computeBlockSizes();
                break;
            }
        }
    }
    @Override
    public void keyTyped(KeyEvent e) { }
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_R) {
            program.run();
            return;
        }

        if (groupSelectionBounds.contains(mouse) || selectedInput != null)
            return;

        switch(e.getKeyCode()) {
            case KeyEvent.VK_UP -> up = true;
            case KeyEvent.VK_DOWN -> down = true;
        }
        if (!blockSelectionBounds.contains(mouse)) {
            switch(e.getKeyCode()) {
                case KeyEvent.VK_LEFT -> left = true;
                case KeyEvent.VK_RIGHT -> right = true;
            }
        }
    }
    @Override
    public void keyReleased(KeyEvent e) {
        switch(e.getKeyCode()) {
            case KeyEvent.VK_UP -> up = false;
            case KeyEvent.VK_DOWN -> down = false;
            case KeyEvent.VK_LEFT -> left = false;
            case KeyEvent.VK_RIGHT -> right = false;
        }
    }
    @Override
    public void componentResized(ComponentEvent e) {
        removeInputField();
    }
    @Override
    public void componentMoved(ComponentEvent e) { }
    @Override
    public void componentShown(ComponentEvent e) { }
    @Override
    public void componentHidden(ComponentEvent e) { }
    @Override
    public void windowOpened(WindowEvent e) { }
    @Override
    public void windowClosing(WindowEvent e) { }
    @Override
    public void windowClosed(WindowEvent e) { }
    @Override
    public void windowIconified(WindowEvent e) {removeInputField();}
    @Override
    public void windowDeiconified(WindowEvent e) { }
    @Override
    public void windowActivated(WindowEvent e) { }
    @Override
    public void windowDeactivated(WindowEvent e) { }
}
