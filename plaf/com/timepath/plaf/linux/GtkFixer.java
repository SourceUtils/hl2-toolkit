package com.timepath.plaf.linux;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

/**
 *
 * @author http://www.ailis.de/~k/archives/67-Workaround-for-borderless-Java-Swing-menus-on-Linux.html
 * @author timepath
 */
public class GtkFixer {

    /**
     * Swing menus are looking pretty bad on Linux when the GTK LaF is used (See
     * bug #6925412). It will most likely never be fixed anytime soon so this
     * method provides a workaround for it. It uses reflection to change the GTK
     * style objects of Swing so popup menu borders have a minimum thickness of
     * 1 and menu separators have a minimum vertical thickness of 1.
     */
    public static void installGtkPopupBugWorkaround() {
        // Get current look-and-feel implementation class
        LookAndFeel laf = UIManager.getLookAndFeel();
        Class<?> lafClass = laf.getClass();

        // Do nothing when not using the problematic LaF
        if(!lafClass.getName().equals("com.sun.java.swing.plaf.gtk.GTKLookAndFeel")) {
            return;
        }

        // We do reflection from here on. Failure is silently ignored. The
        // workaround is simply not installed when something goes wrong here
        try {
            // Access the GTK style factory
            Field field = lafClass.getDeclaredField("styleFactory");
            boolean accessible = field.isAccessible();
            field.setAccessible(true);
            Object styleFactory = field.get(laf); // com.sun.java.swing.plaf.gtk.GTKStyleFactory
            field.setAccessible(accessible);

            // Fix the horizontal and vertical thickness of popup menu style
            Object style = getGtkStyle(styleFactory, new JPopupMenu(), "POPUP_MENU");
            fixGtkThickness(style, "yThickness");
            fixGtkThickness(style, "xThickness");

            // Fix the vertical thickness of the popup menu separator style
            style = getGtkStyle(styleFactory, new JSeparator(), "POPUP_MENU_SEPARATOR");
            fixGtkThickness(style, "yThickness");

            // TODO: Fix the disabled menu text color
//            style = getGtkStyle(styleFactory, new JLabel(), "LABEL");
//            String fieldName = "WHITE_COLOR"; // the disabled text is white
//            Field f2 = style.getClass().getDeclaredField(fieldName);
//            boolean ac2 = f2.isAccessible();
//            f2.setAccessible(true);
//            f2.set(style, new ColorUIResource(new Color(255, 0, 0)));
//            f2.setAccessible(ac2);
        } catch(Exception e) {
            // Silently ignored. Workaround can't be applied.
        }
    }

    /**
     * Called internally by installGtkPopupBugWorkaround to fix the thickness
     * of a GTK style field by setting it to a minimum value of 1.
     *
     * @param style     The GTK style object.
     * @param fieldName The field name.
     *
     * @throws Exception When reflection fails.
     */
    private static void fixGtkThickness(Object style, String fieldName) throws Exception {
        Field field = style.getClass().getDeclaredField(fieldName);
        boolean accessible = field.isAccessible();
        field.setAccessible(true);
        field.setInt(style, Math.max(1, field.getInt(style)));
        field.setAccessible(accessible);
    }

    /**
     * Called internally by installGtkPopupBugWorkaround. Returns a specific
     * GTK style object.
     *
     * @param styleFactory The GTK style factory.
     * @param component    The target component of the style.
     * @param regionName   The name of the target region of the style.
     *
     * @return The GTK style.
     *
     * @throws Exception When reflection fails.
     */
    private static Object getGtkStyle(Object styleFactory, JComponent component, String regionName) throws Exception {
        // Create the region object
        Class<?> regionClass = Class.forName("javax.swing.plaf.synth.Region");
        Field field = regionClass.getField(regionName);
        Object region = field.get(regionClass); // javax.swing.plaf.synth.Region

        // Get and return the style
        Class<?> styleFactoryClass = styleFactory.getClass();
        Method method = styleFactoryClass.getMethod("getStyle", new Class<?>[]{JComponent.class, regionClass});
        boolean accessible = method.isAccessible();
        method.setAccessible(true);
        Object style = method.invoke(styleFactory, component, region); // javax.swing.plaf.synth.SynthStyle, com.sun.java.swing.plaf.gtk.GTKStyle
        method.setAccessible(accessible);
        return style;
    }

    private static final Logger LOG = Logger.getLogger(GtkFixer.class.getName());

    private GtkFixer() {
    }
}