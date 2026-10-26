package com.enderx;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Redstonecontrol implements ModInitializer {

    private static final int PORT = 9090;
    private static final int LED_COUNT = 15;
    private static final int SCAN_RADIUS = 20;

    private static final ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private static volatile MinecraftServer currentServer;
    private static volatile String ledState = "off";
    private static final Map<Block, String> CONCRETE_COLORS = new HashMap<>();
    private static int tickCounter = 0;
    private HttpServer httpServer;

    static {
        CONCRETE_COLORS.put(Blocks.CONCRETE.white(), "ffffff");
        CONCRETE_COLORS.put(Blocks.CONCRETE.orange(), "ff6600");
        CONCRETE_COLORS.put(Blocks.CONCRETE.magenta(), "ff00ff");
        CONCRETE_COLORS.put(Blocks.CONCRETE.lightBlue(), "33aaff");
        CONCRETE_COLORS.put(Blocks.CONCRETE.yellow(), "ffee00");
        CONCRETE_COLORS.put(Blocks.CONCRETE.lime(), "66ff00");
        CONCRETE_COLORS.put(Blocks.CONCRETE.pink(), "ff6699");
        CONCRETE_COLORS.put(Blocks.CONCRETE.gray(), "444444");
        CONCRETE_COLORS.put(Blocks.CONCRETE.lightGray(), "999999");
        CONCRETE_COLORS.put(Blocks.CONCRETE.cyan(), "00ffff");
        CONCRETE_COLORS.put(Blocks.CONCRETE.purple(), "8800ff");
        CONCRETE_COLORS.put(Blocks.CONCRETE.blue(), "0000ff");
        CONCRETE_COLORS.put(Blocks.CONCRETE.brown(), "663300");
        CONCRETE_COLORS.put(Blocks.CONCRETE.green(), "00aa00");
        CONCRETE_COLORS.put(Blocks.CONCRETE.red(), "ff0000");
        CONCRETE_COLORS.put(Blocks.CONCRETE.black(), "000000");
    }

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> currentServer = s);
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
            currentServer = null;
            if (httpServer != null) httpServer.stop(0);
        });

        ServerTickEvents.END_SERVER_TICK.register(s -> {
            Runnable task;
            while ((task = taskQueue.poll()) != null) {
                task.run();
            }

            if (++tickCounter >= 10) {
                tickCounter = 0;
                updateLedState(s);
            }
        });

        try {
            httpServer = HttpServer.create(new InetSocketAddress(PORT), 0);
            httpServer.createContext("/press", new ButtonRequestHandler());
            httpServer.createContext("/leds", new LedRequestHandler());
            httpServer.setExecutor(null);
            httpServer.start();
            System.out.println("[Pi] Server started at port: " + PORT + "!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void respond(HttpExchange exchange, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain");
        exchange.getResponseHeaders().add("Connection", "close");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static class ButtonRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                taskQueue.add(Redstonecontrol::pressAllJigsawButtons);
                respond(exchange, "OK");
            } finally {
                exchange.close();
            }
        }
    }


    private static class LedRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                respond(exchange, ledState);
            } finally {
                exchange.close();
            }
        }
    }

    private static boolean isCopperBulb(BlockState state) {
        return state.getBlock() instanceof CopperBulbBlock;
    }

    private static void updateLedState(MinecraftServer server) {
        if (server.getPlayerList().getPlayers().isEmpty()) {
            ledState = "off";
            return;
        }

        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        ServerLevel level = (ServerLevel) player.level();
        BlockPos playerPos = player.blockPosition();

        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -SCAN_RADIUS; y <= SCAN_RADIUS; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    BlockPos bulbPos = playerPos.offset(x, y, z);
                    BlockState bulbState = level.getBlockState(bulbPos);

                    if (!isCopperBulb(bulbState)) continue;
                    if (!bulbState.getValue(BlockStateProperties.LIT)) continue;

                    for (Direction dir : Direction.values()) {
                        String colors = readLine(level, bulbPos, dir);
                        if (colors != null) {
                            ledState = colors;
                            return;
                        }
                    }
                }
            }
        }
        ledState = "off";
    }

    private static String readLine(ServerLevel level, BlockPos bulbPos, Direction dir) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= LED_COUNT; i++) {
            BlockPos p = bulbPos.relative(dir, i);
            String color = CONCRETE_COLORS.get(level.getBlockState(p).getBlock());
            if (color == null) return null;
            if (i > 1) sb.append(',');
            sb.append(color);
        }
        return sb.toString();
    }

    private static void pressAllJigsawButtons() {
        MinecraftServer server = currentServer;
        if (server == null || server.getPlayerList().getPlayers().isEmpty()) return;

        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        ServerLevel level = (ServerLevel) player.level();
        BlockPos playerPos = player.blockPosition();

        int radius = 15;
        int pressed = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);

                    if (level.getBlockState(pos).is(Blocks.JIGSAW)) {
                        for (Direction dir : Direction.values()) {
                            BlockPos neighborPos = pos.relative(dir);
                            BlockState neighborState = level.getBlockState(neighborPos);

                            if (neighborState.is(Blocks.STONE_BUTTON)) {
                                ((ButtonBlock) neighborState.getBlock())
                                        .press(neighborState, level, neighborPos, null);
                                pressed++;
                            }
                        }
                    }
                }
            }
        }
        System.out.println("[Pi] Pressed buttons(amout): " + pressed);
    }
}