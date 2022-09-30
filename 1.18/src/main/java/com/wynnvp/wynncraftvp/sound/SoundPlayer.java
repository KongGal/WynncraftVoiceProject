package com.wynnvp.wynncraftvp.sound;

import com.wynnvp.wynncraftvp.ModCore;
import com.wynnvp.wynncraftvp.config.VOWConfig;
import com.wynnvp.wynncraftvp.npc.NPCHandler;
import com.wynnvp.wynncraftvp.npc.QuestMarkHandler;
import com.wynnvp.wynncraftvp.sound.at.SoundAtArmorStand;
import com.wynnvp.wynncraftvp.sound.at.SoundAtPlayer;
import com.wynnvp.wynncraftvp.sound.line.LineData;
import com.wynnvp.wynncraftvp.sound.line.LineReporter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class SoundPlayer {

    private Thread musicThread = null;
    private final LineReporter lineReporter;
    //public static boolean SPEAKING = false;

    public SoundPlayer() {
        lineReporter = new LineReporter();
    }

    //Code that is run to play all the sounds
    public void playSound(LineData lineData) {
        String line = lineData.getSoundLine();

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        ClientWorld world = MinecraftClient.getInstance().world;

        SoundsHandler soundsHandler = ModCore.instance.soundsHandler;
        if (soundsHandler.get(line).isEmpty()) {
           // System.out.println("Does not contain line: " + lineData.getRealLine());
            lineReporter.MissingLine(lineData);
            return;
        }

        if (player == null) {
            System.out.println("Player is null! Sound not played.");
            return;
        }

        if (world == null) {
            System.out.println("World is null! Sound not played.");
            return;
        }

        SoundManager manager = MinecraftClient.getInstance().getSoundManager();

        manager.stopAll();
        soundsHandler.get(line).ifPresent(sound -> {

            try {
                File dump = new File("./entity_dump/" + lineData.getSoundLine().replaceAll("/", "\\/") + ".txt");
                dump.getParentFile().mkdirs();
                FileOutputStream fout = new FileOutputStream(dump);
                PlayerEntity p = MinecraftClient.getInstance().player;
                String npn = lineData.getNPCName().replaceAll("§.", "").replaceAll("§", "").toLowerCase().replaceAll("[^a-z\\d]", "");
                fout.write(("Searched for " + npn + "\n").getBytes());
                for (Entity e : MinecraftClient.getInstance().world.getEntities()) {
                    double dist = e.getPos().distanceTo(p.getEyePos());
                    String n = e.getDisplayName().getString().replaceAll("§.", "").replaceAll("§", "").toLowerCase().replaceAll("[^a-z\\d]", "");
                    if (dist < 250) {
                        fout.write(String.format("{used_name:\"%s\", name:\"%s\", display_name:\"%s\", entity_name:\"%s\", custom_name:\"%s\", type_untranslated:\"%s\", distance:\"%s\", position:\"%s\"\n}", n, e.getName(), e.getDisplayName(), e.getEntityName(), e.getCustomName(), e.getType().getUntranslatedName(), String.valueOf(dist), e.getPos().toString()).getBytes());
                    }
                }
                fout.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            final CustomSoundClass customSoundClass = sound.getCustomSoundClass();
            final SoundEvent soundEvent = customSoundClass.soundEvent();

            //Solves ArmorStand problem with ??? as name
            //WARNING: not yet tested
            QuestMarkHandler.put(getQuest(sound.getId()));

            //If this is a moving sound or it is set to play all sounds on player
            //ModCore.instance.controller.playAtPlayer(new File("C:/Users/ender/AppData/Roaming/.minecraft/wynnvp/kingsrecruit/kingsrecruit-caravandriver-2.ogg"));
            //ModCore.instance.controller.playAtPlayer(new File(Utils.FILE_ROOT, getQuest(sound.getId())+"/"+sound.getId()+".ogg"));

            if (customSoundClass.movingSound() || VOWConfig.playAllSoundsOnPlayer) {
                //Play the sound at the player
                manager.play(new SoundAtPlayer(soundEvent));
                return;
            }

            String rawName = getRawName(sound.getId());
            Vec3d vector = NPCHandler.find(rawName);
            if (vector == null || player.getPos().distanceTo( vector) >= 30) {
                playSoundAtCoords(player.getPos(), soundEvent, player);
            } else {
                manager.play(new SoundAtArmorStand(soundEvent, rawName));
            }
        });
    }

    private void playSoundAtCoords(Vec3d blockPos, SoundEvent soundEvent, ClientPlayerEntity pl) {

        pl.clientWorld.playSound(blockPos.x, blockPos.y, blockPos.z, soundEvent, SoundCategory.VOICE, VOWConfig.blockCutOff / 16f, 1, false);
    }

    private String getQuest(String id) {
        String result = "none";
        if (id.contains("-")) {
            String[] args = id.split("-");
            result = args[0];
        }
        return result;
    }

    private String getRawName(String name) {
        return ModCore.instance.soundsHandler.findNPCName(name);
    }

}
