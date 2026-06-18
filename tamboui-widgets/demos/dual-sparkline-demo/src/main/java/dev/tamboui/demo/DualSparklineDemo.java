///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.util.Random;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.widgets.sparkline.DualSparkline;
import dev.tamboui.widgets.sparkline.Sparkline;

/**
 * Demo TUI application showcasing the DualSparkline widget.
 * <p>
 * Demonstrates dual-series charts displaying network IN/OUT and disk
 * read/write rates with compact and expanded layout modes.
 * <p>
 * Keys:
 * <ul>
 *   <li>{@code Tab} — toggle compact/expanded layout</li>
 *   <li>{@code +/-} — adjust height in compact mode</li>
 *   <li>{@code y} — toggle Y-axis labels</li>
 *   <li>{@code x} — toggle X-axis labels</li>
 *   <li>{@code q} — quit</li>
 * </ul>
 */
public class DualSparklineDemo {

    private static final int DATA_SIZE = 200;
    private static final String[] X_LABELS = {"-60s", "-45s", "-30s", "-15s", "now"};
    private static final int MIN_HEIGHT = 4;
    private static final int MAX_HEIGHT = 30;

    private boolean running = true;
    private boolean compact;
    private int compactHeight = 11;
    private boolean showYAxis = true;
    private boolean showXAxis = true;
    private final long[] netIn = new long[DATA_SIZE];
    private final long[] netOut = new long[DATA_SIZE];
    private final long[] diskRead = new long[DATA_SIZE];
    private final long[] diskWrite = new long[DATA_SIZE];
    private final Random random = new Random();
    private long frameCount = 0;

    /**
     * Demo entry point.
     *
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new DualSparklineDemo().run();
    }

    private DualSparklineDemo() {
        for (int i = 0; i < DATA_SIZE; i++) {
            netIn[i] = 20 + random.nextInt(60);
            netOut[i] = 10 + random.nextInt(40);
            diskRead[i] = 5 + random.nextInt(50);
            diskWrite[i] = 5 + random.nextInt(30);
        }
    }

    /**
     * Runs the demo application.
     *
     * @throws Exception if an error occurs
     */
    public void run() throws Exception {
        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);

            backend.onResize(() -> terminal.draw(this::ui));

            while (running) {
                terminal.draw(this::ui);

                int c = backend.read(100);
                if (c == 'q' || c == 'Q' || c == 3) {
                    running = false;
                } else if (c == 9) { // Tab
                    compact = !compact;
                } else if (c == '+' || c == '=') {
                    compactHeight = Math.min(MAX_HEIGHT, compactHeight + 2);
                } else if (c == '-' || c == '_') {
                    compactHeight = Math.max(MIN_HEIGHT, compactHeight - 2);
                } else if (c == 'y' || c == 'Y') {
                    showYAxis = !showYAxis;
                } else if (c == 'x' || c == 'X') {
                    showXAxis = !showXAxis;
                }

                updateData();
                frameCount++;
            }
        }
    }

    private void updateData() {
        System.arraycopy(netIn, 1, netIn, 0, DATA_SIZE - 1);
        System.arraycopy(netOut, 1, netOut, 0, DATA_SIZE - 1);
        System.arraycopy(diskRead, 1, diskRead, 0, DATA_SIZE - 1);
        System.arraycopy(diskWrite, 1, diskWrite, 0, DATA_SIZE - 1);

        netIn[DATA_SIZE - 1] = clamp(netIn[DATA_SIZE - 2] + random.nextInt(21) - 10, 0, 100);
        netOut[DATA_SIZE - 1] = clamp(netOut[DATA_SIZE - 2] + random.nextInt(21) - 10, 0, 100);
        diskRead[DATA_SIZE - 1] = clamp(diskRead[DATA_SIZE - 2] + random.nextInt(15) - 7, 0, 80);
        diskWrite[DATA_SIZE - 1] = clamp(diskWrite[DATA_SIZE - 2] + random.nextInt(11) - 5, 0, 60);
    }

    private long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    private void ui(Frame frame) {
        Rect area = frame.area();

        var layout = Layout.vertical()
            .constraints(
                Constraint.length(3),
                Constraint.fill(),
                Constraint.length(3)
            )
            .split(area);

        renderHeader(frame, layout.get(0));
        if (compact) {
            renderCompactLayout(frame, layout.get(1));
        } else {
            renderExpandedLayout(frame, layout.get(1));
        }
        renderFooter(frame, layout.get(2));
    }

    private void renderHeader(Frame frame, Rect area) {
        String mode = compact ? "Compact (" + compactHeight + " rows)" : "Expanded";
        Block block = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.CYAN))
            .title(Title.from(
                Line.from(
                    Span.raw(" TamboUI ").bold().cyan(),
                    Span.raw("DualSparkline Demo ").yellow(),
                    Span.raw("— " + mode + " ").dim()
                )
            ).centered())
            .build();

        frame.renderWidget(block, area);
    }

    // --- Compact layout: 2 charts stacked at fixed height ---

    private void renderCompactLayout(Frame frame, Rect area) {
        var rows = Layout.vertical()
            .constraints(
                Constraint.length(compactHeight),
                Constraint.length(compactHeight),
                Constraint.fill()
            )
            .split(area);

        renderChart(frame, rows.get(0), "Network",
                netIn, netOut, "IN", "OUT",
                Color.GREEN, Color.BLUE, Color.CYAN,
                Sparkline.BarSet.NINE_LEVELS);
        renderChart(frame, rows.get(1), "Disk",
                diskRead, diskWrite, "READ", "WRITE",
                Color.YELLOW, Color.MAGENTA, Color.YELLOW,
                Sparkline.BarSet.NINE_LEVELS);
    }

    // --- Expanded layout: 2 charts filling the space ---

    private void renderExpandedLayout(Frame frame, Rect area) {
        var rows = Layout.vertical()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .split(area);

        renderChart(frame, rows.get(0), "Network",
                netIn, netOut, "IN", "OUT",
                Color.GREEN, Color.BLUE, Color.CYAN,
                Sparkline.BarSet.NINE_LEVELS);
        renderChart(frame, rows.get(1), "Disk",
                diskRead, diskWrite, "READ", "WRITE",
                Color.YELLOW, Color.MAGENTA, Color.YELLOW,
                Sparkline.BarSet.NINE_LEVELS);
    }

    private void renderChart(Frame frame, Rect area, String name,
            long[] topData, long[] bottomData, String topLabel, String bottomLabel,
            Color topColor, Color bottomColor, Color borderColor,
            Sparkline.BarSet barSet) {
        long curTop = topData[DATA_SIZE - 1];
        long curBot = bottomData[DATA_SIZE - 1];

        DualSparkline chart = DualSparkline.builder()
            .topData(topData)
            .bottomData(bottomData)
            .topForeground(topColor)
            .bottomForeground(bottomColor)
            .max(100)
            .showYAxis(showYAxis)
            .barSet(barSet)
            .xLabels(showXAxis ? X_LABELS : null)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(borderColor))
                .title(Title.from(Line.from(
                    Span.styled(" " + name + " ", Style.EMPTY.bold().fg(borderColor)),
                    Span.styled(topLabel + " ", Style.EMPTY.fg(topColor)),
                    Span.styled(String.format("%d MB/s", curTop), Style.EMPTY.bold().fg(topColor)),
                    Span.raw("  "),
                    Span.styled(bottomLabel + " ", Style.EMPTY.fg(bottomColor)),
                    Span.styled(String.format("%d MB/s ", curBot), Style.EMPTY.bold().fg(bottomColor))
                )))
                .build())
            .build();

        frame.renderWidget(chart, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        Line helpLine = Line.from(
            Span.raw(" Frame: ").dim(),
            Span.raw(String.valueOf(frameCount)).bold().cyan(),
            Span.raw("  "),
            Span.raw("Tab").bold().yellow(),
            Span.raw(" Layout").dim(),
            Span.raw("  "),
            Span.raw("+/-").bold().yellow(),
            Span.raw(" Height").dim(),
            Span.raw("  "),
            Span.raw("y").bold().yellow(),
            Span.raw(" Y-axis").dim(),
            Span.raw(showYAxis ? " ✓" : "  ").green(),
            Span.raw("  "),
            Span.raw("x").bold().yellow(),
            Span.raw(" X-axis").dim(),
            Span.raw(showXAxis ? " ✓" : "  ").green(),
            Span.raw("  "),
            Span.raw("q").bold().yellow(),
            Span.raw(" Quit").dim()
        );

        Paragraph footer = Paragraph.builder()
            .text(Text.from(helpLine))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .build();

        frame.renderWidget(footer, area);
    }
}
