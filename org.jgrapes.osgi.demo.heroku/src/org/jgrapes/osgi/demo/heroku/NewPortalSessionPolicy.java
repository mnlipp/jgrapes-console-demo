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
import org.jgrapes.portal.PortalSession;
import org.jgrapes.portal.Portlet;
import org.jgrapes.portal.events.AddPortletRequest;
import org.jgrapes.portal.events.PortalConfigured;
import org.jgrapes.portal.events.PortalPrepared;
import org.jgrapes.portal.events.RenderPortlet;
import org.jgrapes.portal.events.UpdatePortletModel;
import org.jgrapes.portlets.markdowndisplay.MarkdownDisplayPortlet;

/**
 * 
 */
public class NewPortalSessionPolicy extends Component {

	private static final String INTRO_PORTLET_ID = "IntroPortlet";
	
	/**
	 * Creates a new component with its channel set to
	 * itself.
	 */
	public NewPortalSessionPolicy() {
	}

	/**
	 * Creates a new component with its channel set to the given channel.
	 * 
	 * @param componentChannel
	 */
	public NewPortalSessionPolicy(Channel componentChannel) {
		super(componentChannel);
	}

	@Handler
	public void onPortalPrepared(PortalPrepared event, PortalSession portalSession) {
		portalSession.setAssociated(NewPortalSessionPolicy.class, false);
	}
	
	@Handler
	public void onRenderPortlet(RenderPortlet event, PortalSession portalSession) {
		if (event.portletId().equals(INTRO_PORTLET_ID)) {
			portalSession.setAssociated(NewPortalSessionPolicy.class, true);
		}
	}
	
	@Handler
	public void onPortalConfigured(PortalConfigured event, PortalSession portalSession) 
			throws InterruptedException, IOException {
		boolean foundIntro = portalSession.associated(
				NewPortalSessionPolicy.class, Boolean.class).orElse(false);
		String shortDesc;
		try (BufferedReader shortDescReader = new BufferedReader(new InputStreamReader(
				NewPortalSessionPolicy.class.getResourceAsStream("PortalIntro-Preview.md"),
				"utf-8"))) {
			shortDesc = shortDescReader.lines().collect(Collectors.joining("\n"));
		}
		String longDesc;
		try (BufferedReader shortDescReader = new BufferedReader(new InputStreamReader(
				NewPortalSessionPolicy.class.getResourceAsStream("PortalIntro-View.md"),
				"utf-8"))) {
			longDesc = shortDescReader.lines().collect(Collectors.joining("\n"));
		}
		if (!foundIntro) {
			fire(new AddPortletRequest(event.event().event().renderSupport(), 
					MarkdownDisplayPortlet.class.getName(),
					Portlet.RenderMode.Preview)
					.addProperty(MarkdownDisplayPortlet.PORTLET_ID, INTRO_PORTLET_ID)
					.addProperty(MarkdownDisplayPortlet.TITLE, "Demo Portal")
					.addProperty(MarkdownDisplayPortlet.PREVIEW_SOURCE, shortDesc)
					.addProperty(MarkdownDisplayPortlet.DELETABLE, false)
					.addProperty(MarkdownDisplayPortlet.VIEW_SOURCE, longDesc)
					.addProperty(MarkdownDisplayPortlet.EDITABLE_BY,  Collections.EMPTY_SET),
					portalSession);
		} else {
			fire(new UpdatePortletModel(INTRO_PORTLET_ID)
					.addPreference(MarkdownDisplayPortlet.PREVIEW_SOURCE, shortDesc)
					.addPreference(MarkdownDisplayPortlet.VIEW_SOURCE, longDesc),
					portalSession);
		}
	}

}
