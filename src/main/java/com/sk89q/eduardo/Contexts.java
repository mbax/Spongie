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

package com.sk89q.eduardo;

import com.sk89q.eduardo.irc.ChannelUserMode;
import com.sk89q.eduardo.irc.IRCBot;
import com.sk89q.eduardo.util.irc.Users;
import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class Contexts {

    private Contexts() {
    }

    public static Context create(GenericMessageEvent<? extends IRCBot> event) {
        User user = event.getUser();
        @Nullable Channel channel;
        List<ChannelUserMode> modes = new ArrayList<>();

        if (event instanceof MessageEvent) {
            channel = ((MessageEvent) event).getChannel();
        } else if (event instanceof NoticeEvent) {
            channel = ((NoticeEvent) event).getChannel();
        } else {
            channel = null;
        }

        if (channel != null) {
            if (user.getChannelsOpIn().contains(channel)) {
                modes.add(ChannelUserMode.OPERATOR);
            }

            if (user.getChannelsHalfOpIn().contains(channel)) {
                modes.add(ChannelUserMode.HALF_OP);
            }

            if (user.getChannelsVoiceIn().contains(channel)) {
                modes.add(ChannelUserMode.VOICED);
            }
        }

        Context context = new Context();
        context.setNetwork(event.getBot().getId());
        context.setUser(Users.getUserMask(user));
        context.setChannel(channel != null ? channel.getName() : null);
        context.setModes(modes.isEmpty() ? EnumSet.noneOf(ChannelUserMode.class) : EnumSet.copyOf(modes));
        return context;
    }

}