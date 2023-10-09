/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017 Michael N. Lipp
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

package org.jgrapes.osgi.demo.console;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.NamedChannel;
import org.jgrapes.core.events.Stop;
import org.jgrapes.http.HttpServer;
import org.jgrapes.http.InMemorySessionManager;
import org.jgrapes.http.LanguageSelector;
import org.jgrapes.http.StaticContentDispatcher;
import org.jgrapes.http.events.Request;
import org.jgrapes.io.FileStorage;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.net.SocketServer;
import org.jgrapes.osgi.core.ComponentCollector;
import org.jgrapes.webconsole.base.BrowserLocalBackedKVStore;
import org.jgrapes.webconsole.base.ConletComponentFactory;
import org.jgrapes.webconsole.base.ConsoleWeblet;
import org.jgrapes.webconsole.base.KVStoreBasedConsolePolicy;
import org.jgrapes.webconsole.base.PageResourceProviderFactory;
import org.jgrapes.webconsole.base.WebConsole;
import org.jgrapes.webconsole.vuejs.VueJsConsoleWeblet;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 *
 */
public class Application extends Component implements BundleActivator {

    private static BundleContext context;
    private Application app;

    public static BundleContext context() {
        return context;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.
     * BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        Application.context = context;
        // The demo component is the application
        app = new Application();
        // Attach a general nio dispatcher
        app.attach(new NioDispatcher());

        // Network level unencrypted channel.
        Channel httpTransport = new NamedChannel("httpTransport");
        // Create a TCP server listening on port 5000
        app.attach(new SocketServer(httpTransport)
            .setServerAddress(new InetSocketAddress(
                Optional.ofNullable(System.getenv("PORT"))
                    .map(Integer::parseInt).orElse(5000))));

        // Create an HTTP server as converter between transport and application
        // layer.
        app.attach(new HttpServer(app,
            httpTransport, Request.In.Get.class, Request.In.Post.class));

        // Build application layer
        app.attach(new InMemorySessionManager(app.channel()));
        app.attach(new LanguageSelector(app.channel()));
        app.attach(new FileStorage(app.channel(), 65536));
        app.attach(new StaticContentDispatcher(app.channel(),
            "/static/**",
            Application.class.getResource("static/README.txt").toURI()));
        ConsoleWeblet portalWeblet
            = app.attach(new VueJsConsoleWeblet(app.channel(), Channel.SELF,
                new URI("/")))
                .prependClassTemplateLoader(this.getClass())
                .prependConsoleResourceProvider(getClass())
                .prependResourceBundleProvider(getClass())
                .setConnectionInactivityTimeout(Duration.ofMinutes(5));
        WebConsole console = portalWeblet.console();
        console.attach(new BrowserLocalBackedKVStore(
            console, portalWeblet.prefix().getPath()));
        console.attach(new KVStoreBasedConsolePolicy(console));
        console.attach(new NewConsoleSessionPolicy(console));
        console.attach(new ActionFilter(console));
        console.attach(new ComponentCollector<>(
            console, context, PageResourceProviderFactory.class,
            type -> {
                switch (type) {
                case "org.jgrapes.webconsole.provider.gridstack.GridstackProvider":
                    return Arrays.asList(Map.of("configuration",
                        "CoreWithJQUiPlugin"));
                default:
                    return Arrays.asList(Collections.emptyMap());
                }
            }));
        console.attach(new ComponentCollector<>(
            console, context, ConletComponentFactory.class));
        Components.start(app);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        app.fire(new Stop(), Channel.BROADCAST);
        Components.awaitExhaustion();
    }
}
