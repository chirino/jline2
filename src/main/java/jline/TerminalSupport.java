/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */

package jline;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jline.internal.Configuration;
import jline.internal.Log;

/**
 * Provides support for {@link Terminal} instances.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.0
 */
public abstract class TerminalSupport
    implements Terminal
{
    public static final String JLINE_SHUTDOWNHOOK = "jline.shutdownhook";

    public static final int DEFAULT_WIDTH = 80;

    public static final int DEFAULT_HEIGHT = 24;

    private Thread shutdownHook;
    
    private boolean shutdownHookEnabled;

    private boolean supported;

    private boolean echoEnabled;

    private boolean ansiSupported;

    private Configuration configuration;

    protected TerminalSupport(final boolean supported) {
        this.supported = supported;
        this.shutdownHookEnabled = Configuration.getBoolean(JLINE_SHUTDOWNHOOK, true);
    }

    public void init() throws Exception {
        installShutdownHook(new RestoreHook());
    }

    public void restore() throws Exception {
        TerminalFactory.resetIf(this);
        removeShutdownHook();
    }

    public void reset() throws Exception {
        restore();
        init();
    }

    // Shutdown hooks causes classloader leakage in sbt,
    // so they are only installed if -Djline.shutdownhook is true.
    protected void installShutdownHook(final Thread hook) {
        if (!shutdownHookEnabled) {
            Log.debug("Not install shutdown hook " + hook + " because they are disabled.");
            return;
        }
            
        assert hook != null;

        if (shutdownHook != null) {
            throw new IllegalStateException("Shutdown hook already installed");
        }

        try {
            Runtime.getRuntime().addShutdownHook(hook);
            shutdownHook = hook;
        }
        catch (AbstractMethodError e) {
            // JDK 1.3+ only method. Bummer.
            Log.trace("Failed to register shutdown hook: ", e);
        }
    }

    protected void removeShutdownHook() {
        if (!shutdownHookEnabled)
            return;
      
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            }
            catch (AbstractMethodError e) {
                // JDK 1.3+ only method. Bummer.
                Log.trace("Failed to remove shutdown hook: ", e);
            }
            catch (IllegalStateException e) {
                // The VM is shutting down, not a big deal; ignore
            }
            shutdownHook = null;
        }
    }

    public final boolean isSupported() {
        return supported;
    }

    public synchronized boolean isAnsiSupported() {
        return ansiSupported;
    }

    protected synchronized void setAnsiSupported(final boolean supported) {
        this.ansiSupported = supported;
        Log.debug("Ansi supported: ", supported);
    }

    /**
     * Subclass to change behavior if needed. 
     * @return the passed out
     */
    public OutputStream wrapOutIfNeeded(OutputStream out) {
        return out;
    }

    /**
     * Defaults to true which was the behaviour before this method was added.
     */
    public boolean hasWeirdWrap() {
        return true;
    }

    public int getWidth() {
        return DEFAULT_WIDTH;
    }

    public int getHeight() {
        return DEFAULT_HEIGHT;
    }

    public synchronized boolean isEchoEnabled() {
        return echoEnabled;
    }

    public synchronized void setEchoEnabled(final boolean enabled) {
        this.echoEnabled = enabled;
        Log.debug("Echo enabled: ", enabled);
    }

    public InputStream wrapInIfNeeded(InputStream in) throws IOException {
        return in;
    }

    //
    // RestoreHook
    //

    protected class RestoreHook
        extends Thread
    {
        public void start() {
            try {
                restore();
            }
            catch (Exception e) {
                Log.trace("Failed to restore: ", e);
            }
        }
    }
}