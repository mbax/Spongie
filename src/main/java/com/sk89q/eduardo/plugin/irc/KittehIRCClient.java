/*
 * Eduardo, an IRC bot framework
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) Eduardo team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.eduardo.plugin.irc;

import com.google.inject.Inject;
import com.sk89q.eduardo.event.StartupEvent;
import com.sk89q.eduardo.event.message.BroadcastEvent;
import com.sk89q.eduardo.event.message.MessageEvent;
import com.sk89q.eduardo.model.context.Context;
import com.sk89q.eduardo.model.context.Mode;
import com.sk89q.eduardo.model.response.Response;
import com.sk89q.eduardo.service.event.EventBus;
import com.sk89q.eduardo.service.event.Subscribe;
import com.sk89q.eduardo.service.plugin.Plugin;
import com.sk89q.eduardo.util.config.Config;
import com.sk89q.eduardo.util.formatting.StyledFragment;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.helper.ActorEvent;
import org.kitteh.irc.client.library.event.helper.ChannelEvent;
import org.kitteh.irc.client.library.event.helper.ReplyableEvent;
import org.kitteh.irc.client.library.event.user.PrivateCTCPQueryEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;
import org.kitteh.irc.client.library.feature.auth.NickServ;
import org.kitteh.irc.client.library.util.AcceptingTrustManagerFactory;
import org.kitteh.irc.lib.net.engio.mbassy.listener.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Plugin(id = "irc")
public class KittehIRCClient {

    private static final Logger log = LoggerFactory.getLogger(KittehIRCClient.class);

    private final Map<Client.Builder, List<String>> builders = new HashMap<>();
    private final List<Client> bots = new ArrayList<>();
    @Inject private EventBus eventBus;

    @Inject
    public KittehIRCClient(Config config, EventBus eventBus) {
        eventBus.register(this);

        Config irc = config.getConfig("irc");

        for (Config server : irc.getList("servers", Config.class)) {
            Client.Builder builder = Client.builder();
            String name = server.getString("name", "MyNewBot");
            String nickServPassword = server.getString("nickserv.password", "");
            builder.serverHost(server.getString("host", "localhost"))
                    .serverPort(server.getInt("port", 6667))
                    .name(server.getString("id", "noname-" + System.currentTimeMillis()))
                    .nick(name)
                    .user(name)
                    .realName(name)
                    .secure(server.getBoolean("ssl", false))
                    .secureTrustManagerFactory(new AcceptingTrustManagerFactory())
                    .afterBuildConsumer(client -> {
                        if (nickServPassword != null && !nickServPassword.isEmpty()) {
                            client.getAuthManager().addProtocol(new NickServ(client, name, nickServPassword));
                        }
                        client.getEventManager().registerEventListener(KittehIRCClient.this);
                    });
            String password = server.getString("password", "");
            if (password != null && !password.isEmpty()) {
                builder.serverPassword(password);
            }

            builders.put(builder, server.getList("auto-join", String.class));
        }
    }

    @Subscribe
    public void onStartup(StartupEvent event) {
        this.builders.forEach((key, value) -> {
            Client client = key.build();
            value.forEach(channel -> {
                client.addChannel(channel);
                log.info("Auto-joining channel {}", channel);
            });
            bots.add(client);
        });
    }

    @Handler
    public void onCTCP(PrivateCTCPQueryEvent event) {
        switch (event.getMessage().split(" ")[0].toLowerCase()) {
            case "version":
                event.setReply("Spongie");
                break;
            case "finger":
            case "sponge":
            case "spongie":
            case "help":
                event.setReply("I am Spongie!");
        }
    }

    @Handler
    public void onChannelMessage(ChannelMessageEvent event) {
        eventBus.post(new MessageEvent(createContext(event), event.getMessage(), new ResponseImpl(event)));
    }

    @Handler
    public void onPrivateMessage(PrivateMessageEvent event) {
        eventBus.post(new MessageEvent(createContext(event), event.getMessage(), new ResponseImpl(event)));
    }

    @Subscribe
    public void onBroadcast(BroadcastEvent event) {
        String target = event.getTarget();
        if (target.startsWith("#")) {
            for (Client bot : this.bots) {
                bot.sendMessage(target, IRCColorBuilder.asColorCodes(event.getMessage()));
            }
        }
    }

    private static Context createContext(ActorEvent<User> event) {
        User user = event.getActor();
        @Nullable Channel channel;
        List<Mode> modes = new ArrayList<>();

        if (event instanceof ChannelEvent) {
            channel = ((ChannelEvent) event).getChannel();
        } else {
            channel = null;
        }

        if (channel != null) {
            channel.getUserModes(user).ifPresent(modesList -> modesList.forEach(mode -> {
                switch(mode.getChar()) {
                    case 'o':
                        modes.add(Mode.OPERATOR);
                        break;
                    case 'h':
                        modes.add(Mode.HALF_OP);
                        break;
                    case 'v':
                        modes.add(Mode.VOICED);
                        break;
                }
            }));
        }

        return new Context(
                new IRCNetwork(event.getClient()),
                new IRCUser(event.getActor()),
                channel != null ? new IRCRoom(channel) : null,
                modes.isEmpty() ? Collections.emptySet() : EnumSet.copyOf(modes));
    }

    private static class ResponseImpl implements Response {
        private final ReplyableEvent event;

        private ResponseImpl(ReplyableEvent event) {
            this.event = event;
        }

        @Override
        public void respond(StyledFragment fragment) {
            String message = IRCColorBuilder.asColorCodes(fragment);
            if (event instanceof ChannelEvent && event instanceof ActorEvent && ((ActorEvent<?>) event).getActor() instanceof User) {
                ((User) ((ActorEvent<?>) event).getActor()).sendMessage(message);
            } else {
                event.sendReply(message);
            }
        }

        @Override
        public void broadcast(StyledFragment fragment) {
            event.sendReply(IRCColorBuilder.asColorCodes(fragment));
        }
    }

}
