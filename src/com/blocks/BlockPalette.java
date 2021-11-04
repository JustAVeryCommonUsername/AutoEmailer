package com.blocks;

import com.automailer.GUI;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.blocks.BlockBase.InputType.*;

/**
 * Contains static methods to load useful block palettes
 */
public class BlockPalette {
    @SuppressWarnings(value="unchecked")
    public static List<Group> loadDefaultPalette() {
        return l(
                new Group("Operations", new Color(104, 157, 242), l(
                        b(l(DOUBLE, DOUBLE), l("", "+", ""), (i) -> (double) i[0] + (double) i[1]),
                        b(l(DOUBLE, DOUBLE), l("", "-", ""), (i) -> (double) i[0] - (double) i[1]),
                        b(l(DOUBLE, DOUBLE), l("", "*", ""), (i) -> (double) i[0] * (double) i[1]),
                        b(l(DOUBLE, DOUBLE), l("", "/", ""), (i) -> (double) i[0] / (double) i[1]),
                        b(l(DOUBLE, DOUBLE), l("", "mod", ""), (i) -> (double) i[0] % (double) i[1]),
                        b(l(DOUBLE, DOUBLE), l("", ">", ""), (i) -> (double) i[0] > (double) i[1]),
                        b(l(DOUBLE, DOUBLE), l("", "<", ""), (i) -> (double) i[0] < (double) i[1]),
                        b(l(DOUBLE), l("round", ""), (i) -> (int) (Math.round((double) i[0]))),
                        b(l(ANY, ANY), l("", "=", ""), (i) -> (i[0].equals(i[1]))),
                        b(l(BOOLEAN, BOOLEAN), l("", "and", ""), (i) -> (boolean) i[0] && (boolean) i[1]),
                        b(l(BOOLEAN, BOOLEAN), l("", "or", ""), (i) -> (boolean) i[0] || (boolean) i[1]),
                        b(l(BOOLEAN), l("not", ""), (i) -> !(boolean) i[0]),
                        b(l(DOUBLE, DOUBLE), l("random from", "to", ""), (i) -> (Math.random() * ((double) i[1] - (double) i[0])) + (double) i[0])
                )),
                new Group("Control", new Color(252, 186, 3), l(
                        BlockBase.IF, BlockBase.IF_ELSE, BlockBase.WHILE,
                        b(l(), l("stop"), (i) -> {Thread.currentThread().stop();}),
                        b(l(DOUBLE), l("wait", "seconds"), (i) -> {
                            try {
                                Thread.sleep((long) ((double) i[0] * 1000));
                            } catch (InterruptedException ignored) {
                            }
                        })
                )),
                new Group("Time", new Color(56, 232, 223), l(
                        b(l(), l("date"), (i) -> (LocalDate.now().toString())),
                        b(l(), l("time"), (i) -> (LocalTime.now().toString())),
                        b(l(), l("year"), (i) -> (LocalDate.now().getYear())),
                        b(l(), l("month"), (i) -> (LocalDate.now().getMonthValue())),
                        b(l(), l("current month"), (i) -> (LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()))),
                        b(l(), l("day"), (i) -> (LocalDate.now().getDayOfMonth())),
                        b(l(), l("current day"), (i) -> (LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()))),
                        b(l(), l("hour"), (i) -> (LocalTime.now().getHour())),
                        b(l(), l("minutes"), (i) -> (LocalTime.now().getMinute())),
                        b(l(), l("seconds"), (i) -> (LocalTime.now().getSecond()))
                )),
                new Group("System", new Color(208, 214, 209), l(
                        b(l(), l("beep"), (i) -> {Toolkit.getDefaultToolkit().beep();}),
                        b(l(STRING), l("notify", ""), (i) -> {
                            SystemTray tray = SystemTray.getSystemTray();
                            TrayIcon icon = new TrayIcon(GUI.icon, "AutoEmailer");
                            icon.setImageAutoSize(true);
                            try {
                                tray.add(icon);
                            } catch (AWTException ignored) {
                            }
                            icon.displayMessage("AutoEmailer", (String) i[0], TrayIcon.MessageType.NONE);
                        }),
                        b(l(STRING), l("open file", ""), (i) -> {
                            try {
                                Desktop.getDesktop().open(new File((String) i[0]));
                            } catch (IOException ignored) {
                            }
                        }),
                        b(l(STRING), l("open url", ""), (i) -> {
                            try {
                                Desktop.getDesktop().browse(new URI((String) i[0]));
                            } catch (URISyntaxException | IOException ignored) {
                            }
                        })
                )),
                new Group("Files", new Color(158, 122, 109), l(
                        b(l(STRING), l("read file", ""), (i) -> {
                            try {
                                return Files.readAllLines(new File((String) i[0]).toPath());
                            } catch (IOException ignored) {
                            }
                            return l();
                        }),
                        b(l(STRING), l("create file", ""), (i) -> {
                            try {
                                new File((String) i[0]).createNewFile();
                            } catch (IOException ignored) {
                            }
                        }),
                        b(l(STRING), l("create folder", ""), (i) -> {
                            new File((String) i[0]).mkdirs();
                        }),
                        b(l(STRING), l("file", "exists"), (i) -> (new File((String) i[0]).exists())),
                        b(l(INTEGER, INTEGER, STRING), l("remove lines", "to", "in file", ""), (i) -> {
                            try {
                                File file = new File((String) i[2]);
                                List<String> lines = Files.readAllLines(file.toPath());
                                lines.subList((int) i[0], (int) i[1]).clear();
                                Files.write(file.toPath(), lines);
                            } catch (IOException ignored) {
                            }
                        }),
                        b(l(LIST, INTEGER, STRING), l("add lines", "at", "in file", ""), (i) -> {
                            try {
                                File file = new File((String) i[2]);
                                List<String> lines = Files.readAllLines(file.toPath());
                                ((List) i[0]).forEach((l) -> lines.add((int) i[1], l.toString()));
                                Files.write(file.toPath(), lines);
                            } catch (IOException ignored) {
                            }
                        })
                )),
                new Group("Text", new Color(198, 30, 123), l(
                        b(l(STRING, STRING), l("", "+", ""), (i) -> i[0] + (String) i[1]),
                        b(l(STRING), l("length of", ""), (i) -> (((String) i[0]).length())),
                        b(l(STRING), l("is", "blank"), (i) -> (((String) i[0]).isEmpty())),
                        b(l(STRING, STRING), l("", "contains", ""), (i) -> (((String) i[0]).contains((String) i[1]))),
                        b(l(STRING, INTEGER, INTEGER), l("substring", "from", "to", ""), (i) -> (((String) i[0]).substring((int) i[1], (int) i[2]))),
                        b(l(STRING, STRING), l("split", "with", ""), (i) -> (List.of(((String) i[0]).split((String) i[1])))),
                        b(l(STRING), l("upper case", ""), (i) -> (((String) i[0]).toUpperCase())),
                        b(l(STRING), l("lower case", ""), (i) -> (((String) i[0]).toLowerCase()))
                )),
                new Group("Lists", new Color(223, 92, 242), l(
                        b(l(), l("new list"), (i) -> {return l();}),
                        b(l(LIST), l("size of", ""), (i) -> (((List) i[0]).size())),
                        b(l(LIST), l("is", "empty"), (i) -> (((List) i[0]).isEmpty())),
                        b(l(INTEGER, LIST), l("get item", "of", ""), (i) -> (((List) i[1]).get((int) i[0]))),
                        b(l(LIST, ANY), l("list", "contains", ""), (i) -> (((List) i[0]).contains(i[1]))),
                        b(l(ANY, LIST), l("add", "to list", ""), (i) -> {((List) i[1]).add(i[0]);}),
                        b(l(ANY, INTEGER, LIST), l("insert", "at", "in", ""), (i) -> {((List) i[2]).add((int) i[1], i[0]);}),
                        b(l(INTEGER, LIST), l("delete item", "of", ""), (i) -> {((List) i[1]).remove((int) i[0]);}),
                        b(l(LIST, LIST), l("combine lists", "and", ""), (i) -> {((List) i[0]).addAll(((List) i[1]));}),
                        b(l(LIST), l("sort list", ""), (i) -> {((List) i[0]).sort(Comparator.naturalOrder());})
                )),
                new Group("Variables", new Color(125, 214, 15), l(
                        b(l(STRING, ANY), l("set variable", "to", ""), (i) -> {Program.variables.replace((String) i[0], i[1]);}),
                        BlockBase.VARIABLE
                ))
        );
    }

    private static <T> List<T> l(T... elements) {
        return List.of(elements);
    }

    private static List l() {
        return new ArrayList();
    }

    private static BlockBase b(List<BlockBase.InputType> inputs, List<String> labels, Consumer<Object[]> normalCode) {
        return new BlockBase(inputs, labels, normalCode);
    }

    private static BlockBase b(List<BlockBase.InputType> inputs, List<String> labels, Function<Object[], Object> fieldCode) {
        return new BlockBase(inputs, labels, fieldCode);
    }
}
