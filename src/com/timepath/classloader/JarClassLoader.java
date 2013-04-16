package com.timepath.classloader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class JarClassLoader extends ClassLoader {

    private static final Logger LOG = Logger.getLogger(JarClassLoader.class.getName());

    public JarClassLoader() {
        this(ClassLoader.getSystemClassLoader());
    }

    public JarClassLoader(ClassLoader parent) {
        super(parent);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
            }
        });
    }

    public void run(String className, String[] args) throws Throwable {
        Class<?> clazz = loadClass(className);
        Method method = clazz.getMethod("main", new Class<?>[]{String[].class});
        // ensure 'method' is 'public static void main(args[])'
        boolean modifiersValid = false;
        boolean returnTypeValid = false;
        if(method != null) {
            method.setAccessible(true);
            int modifiers = method.getModifiers();
            modifiersValid = Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers);
            Class<?> returnType = method.getReturnType();
            returnTypeValid = (returnType == void.class);
        }
        if(method == null || !modifiersValid || !returnTypeValid) {
            throw new NoSuchMethodException("Class \"" + className + "\" does not have a main() method.");
        }

        try {
            method.invoke(null, (Object) args);
        } catch(InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        LOG.log(Level.INFO, "findClass({0})", name);
        return super.findClass(name);
    }

    @Override
    protected String findLibrary(String libname) {
        LOG.log(Level.INFO, "findLibrary({0})", libname);
        return super.findLibrary(libname);
    }
}