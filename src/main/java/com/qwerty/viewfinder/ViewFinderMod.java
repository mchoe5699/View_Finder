package com.qwerty.viewfinder;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(ViewFinderMod.MODID)
public class ViewFinderMod {
    public static final String MODID = "view_finder";

    public ViewFinderMod() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ViewFinderConfig.SPEC);
        }
    }
}
