/*
 * Copyright (c) 2004-2005 by OpenSymphony
 * All rights reserved.
 * 
 * Previously Copyright (c) 2001-2004 James House
 */
package org.quartz.simpl;

import java.util.Iterator;
import java.util.LinkedList;

import org.quartz.spi.ClassLoadHelper;

/**
 * A <code>ClassLoadHelper</code> uses all of the <code>ClassLoadHelper</code>
 * types that are found in this package in its attempts to load a class, when
 * one scheme is found to work, it is promoted to the scheme that will be used
 * first the next time a class is loaded (in order to improve perfomance).
 * 
 * @see org.quartz.spi.ClassLoadHelper
 * @see org.quartz.simpl.SimpleClassLoadHelper
 * @see org.quartz.simpl.ThreadContextClassLoadHelper
 * @see org.quartz.simpl.InitThreadContextClassLoadHelper
 * 
 * @author jhouse
 */
public class CascadingClassLoadHelper implements ClassLoadHelper {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private LinkedList loadHelpers;

    private ClassLoadHelper bestCandidate;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * Called to give the ClassLoadHelper a chance to initialize itself,
     * including the oportunity to "steal" the class loader off of the calling
     * thread, which is the thread that is initializing Quartz.
     */
    public void initialize() {
        loadHelpers = new LinkedList();

        loadHelpers.add(new SimpleClassLoadHelper());
        loadHelpers.add(new ThreadContextClassLoadHelper());
        loadHelpers.add(new InitThreadContextClassLoadHelper());

        Iterator iter = loadHelpers.iterator();
        while (iter.hasNext()) {
            ClassLoadHelper loadHelper = (ClassLoadHelper) iter.next();
            loadHelper.initialize();
        }
    }

    /**
     * Return the class with the given name.
     */
    public Class loadClass(String name) throws ClassNotFoundException {

        if (bestCandidate != null) {
            try {
                return bestCandidate.loadClass(name);
            } catch (Exception e) {
                bestCandidate = null;
            }
        }

        ClassNotFoundException cnfe = null;
        Class clazz = null;
        ClassLoadHelper loadHelper = null;

        Iterator iter = loadHelpers.iterator();
        while (iter.hasNext()) {
            loadHelper = (ClassLoadHelper) iter.next();

            try {
                clazz = loadHelper.loadClass(name);
                break;
            } catch (ClassNotFoundException e) {
                cnfe = e;
            }
        }

        if (clazz == null) throw cnfe;

        bestCandidate = loadHelper;

        return clazz;
    }

}
