package org.example.hercules.utils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.reflect.*;
import java.net.*;

public class CustomClassLoader extends URLClassLoader {
	protected CustomClassLoader(URL[] urls, ClassLoader parent){super(urls, parent);}
	protected CustomClassLoader(URL[] urls){super(urls);}

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
	    synchronized (getClassLoadingLock(name)){
		    Class<?> cls = findLoadedClass(name);
		    try{
			    if(cls == null)
				    cls = findClass(name);
			    if(cls == null)
				    if(getParent() != null)
					    cls = getParent().loadClass(name);
					else
						cls = findSystemClass(name);
			    if(resolve)
				    resolveClass(cls);
		    }catch(ClassNotFoundException e){
			    if(getParent() != null)
				    cls = getParent().loadClass(name);
		    		else
					cls = findSystemClass(name);
		    }
		    return cls;
	    }
    }

}

