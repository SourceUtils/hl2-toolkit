package com.timepath.plaf.mac;

import apple.OSXAdapter;
import java.awt.Image;
import java.awt.PopupMenu;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuBar;

/**
 * Reflected com.apple.eawt.Application
 *
 * @author timepath
 */
@SuppressWarnings("rawtypes")
public class Application {

    private static final Logger LOG = Logger.getLogger(Application.class.getName());

    public class AboutEvent {

        public AboutEvent() {
        }
    }

    public interface AboutHandler {

        public abstract void handleAbout(AboutEvent e);

    }

    public class PreferencesEvent {

        public PreferencesEvent() {
        }
    }

    public interface PreferencesHandler {

        public abstract void handlePreferences(PreferencesEvent e);

    }

    public class QuitEvent {

        public QuitEvent() {
        }
    }

    public class QuitResponse {

        public QuitResponse() {
        }
    }

    public interface QuitHandler {

        public abstract void handleQuitRequestWith(QuitEvent qe, QuitResponse qr);

    }

    private Application() {
    }

    public static Application getApplication() {
        return new Application();
    }

    public void setAboutHandler(AboutHandler aboutHandler) {

    }

    public void setDefaultMenuBar(JMenuBar menuBar) {

    }

    public void setDockIconBadge(String badge) {

    }

    public void setDockIconImage(Image image) {

    }

    public void setDockMenu(PopupMenu popup) {

    }

    public void setPreferencesHandler(PreferencesHandler preferencesHandler) {

    }

    public void	setQuitHandler(QuitHandler quitHandler) {
    	setHandler(new OSXHandler(/*quitHandler*/));
    }

    //

    private Object macOSXApplication;

    private Object getMacOSXApplication() {
        return macOSXApplication;
    }

    private void setMacOSXApplication(Object amacOSXApplication) {
        macOSXApplication = amacOSXApplication;
    }
    
    private class OSXHandler implements InvocationHandler {
    	
    	private OSXHandler() {
    		
    	}

		public Object invoke(Object arg0, Method arg1, Object[] arg2)
				throws Throwable {
			// TODO Auto-generated method stub
			return null;
		}
    	
    }

    private void setHandler(OSXHandler adapter) {
        try {
			Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
            if(getMacOSXApplication() == null) {
                 // com.apple.eawt.Application()
                setMacOSXApplication(applicationClass.getConstructor((Class[]) null).newInstance((Object[]) null));
            }
            Class applicationListenerClass = Class.forName("com.apple.eawt.ApplicationListener");
            // com.apple.eawt.Application.addApplicationListener(com.apple.eawt.ApplicationListener)
            Method addListenerMethod = applicationClass.getDeclaredMethod("addApplicationListener", new Class[]{applicationListenerClass});

            Object osxAdapterProxy = Proxy.newProxyInstance(OSXAdapter.class.getClassLoader(), new Class[]{applicationListenerClass}, adapter);
            addListenerMethod.invoke(getMacOSXApplication(), new Object[]{osxAdapterProxy});
        } catch(ClassNotFoundException cnfe) {
            LOG.log(Level.WARNING, "This version of Mac OS X does not support the Apple EAWT. ApplicationEvent handling has been disabled ({0})", cnfe);
        } catch(Exception ex) {
            LOG.warning("Mac OS X Adapter could not talk to EAWT.");
            LOG.log(Level.SEVERE, null, ex);
        }
    }

}
