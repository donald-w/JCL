/**
 * Copyright 2015 Kamran Zafar
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xeustechnologies.jcl;

import org.xeustechnologies.jcl.exception.JclException;
import org.xeustechnologies.jcl.exception.ResourceNotFoundException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads the class bytes from jar files and other resources using
 * ClasspathResources
 *
 * @author Kamran Zafar
 */
@SuppressWarnings("unchecked")
public class JarClassLoader extends AbstractClassLoader {
    private static Logger logger = Logger.getLogger(JarClassLoader.class.getName());
    /**
     * Class cache
     */
    protected final Map<String, Class> classes;
    protected final ClasspathResources classpathResources;
    private final ProxyClassLoader localLoader = new LocalLoader();
    private char classNameReplacementChar;

    public JarClassLoader() {
        classpathResources = new ClasspathResources();
        classes = Collections.synchronizedMap(new HashMap<String, Class>());
        initialize();
    }

    /**
     * Loads classes from different sources
     *
     * @param sources
     */
    public JarClassLoader(Object[] sources) {
        this();
        addAll(sources);
    }

    /**
     * Loads classes from different sources
     *
     * @param sources
     */
    public JarClassLoader(List sources) {
        this();
        addAll(sources);
    }

    /**
     * Some initialisations
     */
    public void initialize() {
        addLoader(localLoader);
    }

    /**
     * Add all jar/class sources
     *
     * @param sources
     */
    public void addAll(Object[] sources) {
        for (Object source : sources) {
            add(source);
        }
    }

    /**
     * Add all jar/class sources
     *
     * @param sources
     */
    public void addAll(List sources) {
        for (Object source : sources) {
            add(source);
        }
    }

    /**
     * Loads local/remote source
     *
     * @param source
     */
    public void add(Object source) {
        if (source instanceof InputStream)
            add((InputStream) source);
        else if (source instanceof URL)
            add((URL) source);
        else if (source instanceof String)
            add((String) source);
        else
            throw new JclException("Unknown Resource type");

    }

    /**
     * Loads local/remote resource
     *
     * @param resourceName
     */
    public void add(String resourceName) {
        classpathResources.loadResource(resourceName);
    }

    /**
     * Loads classes from InputStream
     *
     * @param jarStream
     */
    public void add(InputStream jarStream) {
        classpathResources.loadJar(null, jarStream);
    }

    /**
     * Loads local/remote resource
     *
     * @param url
     */
    public void add(URL url) {
        classpathResources.loadResource(url);
    }

    /**
     * Reads the class bytes from different local and remote resources using
     * ClasspathResources
     *
     * @param className
     * @return byte[]
     */
    protected byte[] loadClassBytes(String className) {
        className = formatClassName(className);

        return classpathResources.getResource(className);
    }

    /**
     * Attempts to unload class, it only unloads the locally loaded classes by
     * JCL
     *
     * @param className
     */
    public void unloadClass(String className) {
        if (logger.isLoggable(Level.FINEST))
            logger.finest("Unloading class " + className);

        if (classes.containsKey(className)) {
            if (logger.isLoggable(Level.FINEST))
                logger.finest("Removing loaded class " + className);
            classes.remove(className);
            try {
                classpathResources.unload(formatClassName(className));
            } catch (ResourceNotFoundException e) {
                throw new JclException("Something is very wrong!!!"
                        + "The locally loaded classes must be in synch with ClasspathResources", e);
            }
        } else {
            try {
                classpathResources.unload(formatClassName(className));
            } catch (ResourceNotFoundException e) {
                throw new JclException("Class could not be unloaded "
                        + "[Possible reason: Class belongs to the system]", e);
            }
        }
    }

    /**
     * @param className
     * @return String
     */
    protected String formatClassName(String className) {
        className = className.replace('/', '~');

        if (classNameReplacementChar == '\u0000') {
            // '/' is used to map the package to the path
            className = className.replace('.', '/') + ".class";
        } else {
            // Replace '.' with custom char, such as '_'
            className = className.replace('.', classNameReplacementChar) + ".class";
        }

        className = className.replace('~', '/');
        return className;
    }

    public char getClassNameReplacementChar() {
        return classNameReplacementChar;
    }

    public void setClassNameReplacementChar(char classNameReplacementChar) {
        this.classNameReplacementChar = classNameReplacementChar;
    }

    /**
     * Returns all loaded classes and resources
     *
     * @return Map
     */
    public Map<String, byte[]> getLoadedResources() {
        return classpathResources.getResources();
    }

    /**
     * @return Local JCL ProxyClassLoader
     */
    public ProxyClassLoader getLocalLoader() {
        return localLoader;
    }

    /**
     * Returns all JCL-loaded classes as an immutable Map
     *
     * @return Map
     */
    public Map<String, Class> getLoadedClasses() {
        return Collections.unmodifiableMap(classes);
    }

    /**
     * Local class loader
     */
    class LocalLoader extends ProxyClassLoader {

        private final Logger logger = Logger.getLogger(LocalLoader.class.getName());

        public LocalLoader() {
            order = 10;
            enabled = Configuration.isLocalLoaderEnabled();
        }

        @Override
        public Class loadClass(String className, boolean resolveIt) {
            Class result = null;
            byte[] classBytes;

            result = classes.get(className);
            if (result != null) {
                if (logger.isLoggable(Level.FINEST))
                    logger.finest("Returning local loaded class [" + className + "] from cache");
                return result;
            }

            classBytes = loadClassBytes(className);
            if (classBytes == null) {
                return null;
            }

            result = defineClass(className, classBytes, 0, classBytes.length);

            if (result == null) {
                return null;
            }

            /*
             * Preserve package name.
             */
            if (result.getPackage() == null) {
                int lastDotIndex = className.lastIndexOf('.');
                String packageName = (lastDotIndex >= 0) ? className.substring(0, lastDotIndex) : "";
                definePackage(packageName, null, null, null, null, null, null, null);
            }

            if (resolveIt)
                resolveClass(result);

            classes.put(className, result);
            if (logger.isLoggable(Level.FINEST))
                logger.finest("Return new local loaded class " + className);
            return result;
        }

        @Override
        public InputStream loadResource(String name) {
            byte[] arr = classpathResources.getResource(name);
            if (arr != null) {
                if (logger.isLoggable(Level.FINEST))
                    logger.finest("Returning newly loaded resource " + name);

                return new ByteArrayInputStream(arr);
            }

            return null;
        }

        @Override
        public URL findResource(String name) {
            URL url = classpathResources.getResourceURL(name);
            if (url != null) {
                if (logger.isLoggable(Level.FINEST))
                    logger.finest("Returning newly loaded resource " + name);

                return url;
            }

            return null;
        }
    }
}
