package com.ultreon.mods.exitconfirmation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Mod(ExitConfirmation.MOD_ID)
public class ExitConfirmation {

    public static final String MOD_ID = "exit_confirm";
    static boolean didAccept;
    private boolean escPress = false;

    // Directly reference a log4j logger.
    @SuppressWarnings("unused")
    static final Logger LOGGER = LogManager.getLogger();

    public ExitConfirmation() {
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        Config.initialize();
    }

    public static boolean didAccept() {
        return didAccept;
    }

    @SubscribeEvent
    public synchronized void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getInstance();

        if (event.getAction() == GLFW.GLFW_PRESS) {
            if (event.getKey() == 256 && Config.closePrompt.get() && Config.quitOnEscInTitle.get()) {
                if (!escPress) {
                    escPress = true;
                    if (mc.screen instanceof TitleScreen) {
                        mc.pushGuiLayer(new ConfirmExitScreen());
                    }
                }
            }
        }
        if (event.getAction() == GLFW.GLFW_RELEASE) {
            if (event.getKey() == 256) {
                escPress = false;
            }
        }
    }

    @SubscribeEvent
    public void onWindowClose(WindowCloseEvent event) {
        Minecraft mc = Minecraft.getInstance();

        if (didAccept) return;

        if (event.getSource() == WindowCloseEvent.Source.GENERIC) {
            if (mc.level == null && mc.screen == null) {
                event.setCanceled(true);
                return;
            }

            if (mc.screen instanceof LevelLoadingScreen) {
                event.setCanceled(true);
                return;
            }

            if (Config.closePrompt.get()) {
                if (mc.level != null && !Config.closePromptInGame.get()) {
                    return;
                }
                event.setCanceled(true);
                if (!(mc.screen instanceof ConfirmExitScreen)) {
                    mc.pushGuiLayer(new ConfirmExitScreen());
                }
            }
        } else if (event.getSource() == WindowCloseEvent.Source.QUIT_BUTTON) {
            if (Config.closePrompt.get() && Config.closePromptQuitButton.get() && !(mc.screen instanceof ConfirmExitScreen)) {
                mc.pushGuiLayer(new ConfirmExitScreen());
            }
        }
    }

    @SubscribeEvent
    public static void onTitleScreenInit(ScreenEvent.InitScreenEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = event.getScreen();
        if (screen instanceof TitleScreen titleScreen) {
            overrideQuitButton(mc, titleScreen);
        }
    }

    private static void overrideQuitButton(Minecraft mc, TitleScreen titleScreen) {
        List<? extends GuiEventListener> buttons = titleScreen.children();
        if (buttons.size() >= 2) {
            if (buttons.get(buttons.size() - 2) instanceof Button widget) {
                widget.onPress = (button) -> {
                    boolean flag = MinecraftForge.EVENT_BUS.post(new WindowCloseEvent(null, WindowCloseEvent.Source.QUIT_BUTTON));
                    if (!flag) {
                        mc.stop();
                    }
                };
            }
        }
    }
}
