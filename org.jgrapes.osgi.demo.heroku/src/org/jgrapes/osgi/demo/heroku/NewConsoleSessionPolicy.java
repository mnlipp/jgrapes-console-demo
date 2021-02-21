/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017  Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.osgi.demo.heroku;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.stream.Collectors;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.webconlet.markdowndisplay.MarkdownDisplayConlet;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.ConsoleSession;
import org.jgrapes.webconsole.base.events.AddConletRequest;
import org.jgrapes.webconsole.base.events.ConsoleConfigured;
import org.jgrapes.webconsole.base.events.ConsolePrepared;
import org.jgrapes.webconsole.base.events.RenderConlet;
import org.jgrapes.webconsole.base.events.UpdateConletModel;

/**
 * 
 */
public class NewConsoleSessionPolicy extends Component {

    private static final String INTRO_CONLET_ID = "IntroConlet";

    /**
     * Creates a new component with its channel set to
     * itself.
     */
    public NewConsoleSessionPolicy() {
    }

    /**
     * Creates a new component with its channel set to the given channel.
     * 
     * @param componentChannel
     */
    public NewConsoleSessionPolicy(Channel componentChannel) {
        super(componentChannel);
    }

    @Handler
    public void onConsolePrepared(ConsolePrepared event,
            ConsoleSession portalSession) {
        portalSession.setAssociated(NewConsoleSessionPolicy.class, false);
    }

    @Handler
    public void onRenderConlet(RenderConlet event,
            ConsoleSession consoleSession) {
        if (event.conletId().equals(INTRO_CONLET_ID)) {
            consoleSession.setAssociated(NewConsoleSessionPolicy.class, true);
        }
    }

    @Handler
    public void onConsoleConfigured(ConsoleConfigured event,
            ConsoleSession consoleSession)
            throws InterruptedException, IOException {
        boolean foundIntro = consoleSession.associated(
            NewConsoleSessionPolicy.class, Boolean.class).orElse(false);
        String shortDesc;
        try (BufferedReader shortDescReader
            = new BufferedReader(new InputStreamReader(
                NewConsoleSessionPolicy.class
                    .getResourceAsStream("ConsoleIntro-Preview.md"),
                "utf-8"))) {
            shortDesc
                = shortDescReader.lines().collect(Collectors.joining("\n"));
        }
        String longDesc;
        try (BufferedReader shortDescReader
            = new BufferedReader(new InputStreamReader(
                NewConsoleSessionPolicy.class
                    .getResourceAsStream("ConsoleIntro-View.md"),
                "utf-8"))) {
            longDesc
                = shortDescReader.lines().collect(Collectors.joining("\n"));
        }
        if (!foundIntro) {
            fire(new AddConletRequest(event.event().event().renderSupport(),
                MarkdownDisplayConlet.class.getName(),
                RenderMode.asSet(RenderMode.Preview))
                    .addProperty(MarkdownDisplayConlet.CONLET_ID,
                        INTRO_CONLET_ID)
                    .addProperty(MarkdownDisplayConlet.TITLE, "Demo Console")
                    .addProperty(MarkdownDisplayConlet.PREVIEW_SOURCE,
                        shortDesc)
                    .addProperty(MarkdownDisplayConlet.DELETABLE, false)
                    .addProperty(MarkdownDisplayConlet.VIEW_SOURCE, longDesc)
                    .addProperty(MarkdownDisplayConlet.EDITABLE_BY,
                        Collections.EMPTY_SET),
                consoleSession);
        } else {
            fire(new UpdateConletModel(INTRO_CONLET_ID)
                .addPreference(MarkdownDisplayConlet.PREVIEW_SOURCE, shortDesc)
                .addPreference(MarkdownDisplayConlet.VIEW_SOURCE, longDesc),
                consoleSession);
        }
    }

}
