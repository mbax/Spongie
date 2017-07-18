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
 * ANY WARRANTY), without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.eduardo.util.formatting;

import org.kitteh.irc.client.library.util.Format;

/**
 * All supported color values for chat.
 */
public enum Style {

    RESET(Format.RESET, false),
    BOLD(Format.BOLD, false),
    UNDERLINE(Format.UNDERLINE, false),
    REVERSE(Format.REVERSE, false),

    WHITE(Format.WHITE),
    BLACK(Format.BLACK),
    DARK_BLUE(Format.DARK_BLUE),
    DARK_GREEN(Format.DARK_GREEN),
    RED(Format.RED),
    BROWN(Format.BROWN),
    PURPLE(Format.PURPLE),
    OLIVE(Format.OLIVE),
    YELLOW(Format.YELLOW),
    GREEN(Format.GREEN),
    TEAL(Format.TEAL),
    CYAN(Format.CYAN),
    BLUE(Format.BLUE),
    MAGENTA(Format.MAGENTA),
    DARK_GRAY(Format.DARK_GRAY),
    LIGHT_GRAY(Format.LIGHT_GRAY);

    private final String code;
    private final boolean color;

    private Style(Format code) {
        this.code = code.toString();
        this.color = true;
    }

    private Style(Format code, boolean color) {
        this.code = code.toString();
        this.color = color;
    }

    public String getCode() {
        return code;
    }

    public boolean isColor() {
        return color;
    }

    public static String stripColor(String str) {
        return Format.stripAll(str);
    }

    @Override
    public String toString() {
        return code;
    }

}
