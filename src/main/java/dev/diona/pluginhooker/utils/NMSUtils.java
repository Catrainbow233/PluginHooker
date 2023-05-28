package dev.diona.pluginhooker.utils;

import cn.nukkit.Player;
import cn.nukkit.Server;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;


public class NMSUtils {

    private static final String BUKKIT_PACKAGE = Server.getInstance().getClass().getPackage().getName();

    private static final Field playerConnectionField;
    private static final Field networkManagerField;
    private static final Field channelField;
    private static final MethodHandle getHandleMethod;
    private static final MethodHandle pipelineMethod;

    static {
        try {
            int majorVersion = getNMSMajorVersion();
            Method getHandle = Class.forName(Player.class.getPackage().getName())
                    .getMethod("getHandle");
            getHandleMethod = MethodHandles.lookup().unreflect(getHandle);

            playerConnectionField = getHandle.getReturnType()
                    .getField(majorVersion > 16 ? "b" : "playerConnection");
            networkManagerField = playerConnectionField.getType()
                    .getField(majorVersion > 16 ? "a" : "networkManager");
            channelField = networkManagerField.getType()
                    .getField(majorVersion > 16 ? "k" : "channel");
            pipelineMethod = MethodHandles.lookup().unreflect(channelField.getType().getMethod("pipeline"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static String getNMSVersion() {
        return BUKKIT_PACKAGE.substring(BUKKIT_PACKAGE.lastIndexOf('.') + 1);
    }

    public static int getNMSMajorVersion() {
        String nmsVersion = getNMSVersion();
        return Integer.parseInt(nmsVersion.substring(nmsVersion.indexOf('_') + 1, nmsVersion.lastIndexOf('_')));
    }

    public static ChannelPipeline getPipelineByPlayer(Player player) {
        try {
            Object channel = getChannelByPlayer(player);
            return (ChannelPipeline) pipelineMethod.invoke(channel);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Channel getChannelByPlayer(Player player) {
        try {
            Object entityPlayer = getHandleMethod.invoke(player);
            Object playerConnection = playerConnectionField.get(entityPlayer);
            Object networkManager = networkManagerField.get(playerConnection);
            return (Channel) channelField.get(networkManager);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
