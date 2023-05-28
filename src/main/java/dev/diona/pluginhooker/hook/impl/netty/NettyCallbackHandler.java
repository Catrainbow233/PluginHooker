package dev.diona.pluginhooker.hook.impl.netty;

import cn.nukkit.Player;
import cn.nukkit.plugin.Plugin;
import com.google.common.collect.Lists;
import dev.diona.pluginhooker.PluginHooker;
import dev.diona.pluginhooker.hook.impl.netty.channelhandler.DecoderWrapper;
import dev.diona.pluginhooker.hook.impl.netty.channelhandler.DuplexHandlerWrapper;
import dev.diona.pluginhooker.hook.impl.netty.channelhandler.EncoderWrapper;
import dev.diona.pluginhooker.player.DionaPlayer;
import dev.diona.pluginhooker.utils.HookerUtils;
import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;

public class NettyCallbackHandler {

    private final static Field handlerField;
    private final static Method channelMethod;

    static {
        try {
            Class<?> channelHandlerContext = Class.forName("io.netty.channel.DefaultChannelHandlerContext");
            handlerField = channelHandlerContext.getDeclaredField("handler");
            handlerField.setAccessible(true);

            Class<?> abstractChannelHandlerContext = channelHandlerContext.getSuperclass();
            channelMethod = abstractChannelHandlerContext.getMethod("channel");
            channelMethod.setAccessible(true);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //ctx = DefaultChannelHandlerContext
    public void handlePipelineAdd(Object ctx) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        for (int i = 2; i < stackTraceElements.length; i++) {
            if (stackTraceElements[i].getClassName().startsWith("io.netty"))
                continue;

            if (stackTraceElements[i].getClassName().startsWith("com.comphenix.protocol"))
                continue;

            try {
                Class<?> aClass = Class.forName(stackTraceElements[i].getClassName());

                if (aClass.getClassLoader() == null || aClass.getClassLoader() == PluginHooker.class.getClassLoader())
                    continue;

                if (!aClass.getClassLoader().getClass().getSimpleName().equals("PluginClassLoader"))
                    continue;

                // 修复Netty线程获取插件列表后线程死锁的bug
                List<Plugin> pluginList = HookerUtils.getServerPlugins();

                for (Plugin plugin : pluginList) {
                    if (plugin.getClass().getClassLoader() != aClass.getClassLoader())
                        continue;


                    if (!PluginHooker.getPluginManager().getPluginsToHook().contains(plugin))
                        break;

                    ChannelHandler handler = getContextHandler(ctx);

                    Consumer<Player> consumer = player -> {
                        if (handler instanceof MessageToMessageDecoder) {
                            setContextHandler(ctx, new DecoderWrapper((MessageToMessageDecoder<?>) handler, plugin, player));
                            // System.out.println("plugin: " + plugin.getName() + " MessageToMessageDecoder");
                        } else if (handler instanceof MessageToMessageEncoder) {
                            setContextHandler(ctx, new EncoderWrapper((MessageToMessageEncoder<?>) handler, plugin, player));
                            // System.out.println("plugin: " + plugin.getName() + " MessageToMessageEncoder");
                        } else if (handler instanceof ChannelDuplexHandler) {
                            setContextHandler(ctx, new DuplexHandlerWrapper((ChannelDuplexHandler) handler, plugin, player));
                            // System.out.println("plugin: " + plugin.getName() + " ChannelDuplexHandler");
                        }
                    };

                    Player player = HookerUtils.getPlayerByChannelContext(ctx);
                    if (player != null) {

                        DionaPlayer dionaPlayer = PluginHooker.getPlayerManager().getDionaPlayer(player);
                        if (dionaPlayer != null && dionaPlayer.isInitialized()) {
                            consumer.accept(player);
                            return;
                        }
                    }

                    Channel channel = getChannel(ctx);
                    this.appendConsumer(consumer, channel);
                    return;
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            break;
        }
    }

    private void appendConsumer(Consumer<Player> consumer, Channel channel) {
        List<Consumer<Player>> list = channel.attr(HookerUtils.HANDLER_REPLACEMENT_FUNCTIONS).get();
        if (list == null) {
            list = Lists.newArrayList();
        }
        list.add(consumer);
        channel.attr(HookerUtils.HANDLER_REPLACEMENT_FUNCTIONS).set(list);
    }

    public static AbstractChannel getChannel(Object ctx) {
        try {
            return (AbstractChannel) channelMethod.invoke(ctx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ChannelHandler getContextHandler(Object ctx) {
        try {
            return (ChannelHandler) handlerField.get(ctx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setContextHandler(Object ctx, ChannelHandler handler) {
        try {
            handlerField.set(ctx, handler);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
