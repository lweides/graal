/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.nativebridge;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.BooleanSupplier;

/**
 * Generate HotSpot to native bridge for an annotated class.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface GenerateHotSpotToNativeBridge {

    /**
     * If the supplier returns {@code true}, this entry point is added automatically when building a
     * shared library. This means the method is a root method for compilation, and everything
     * reachable from it is compiled too. * The provided class must have a nullary constructor,
     * which is used to instantiate the class. The supplier function is called on the newly
     * instantiated instance.
     */
    Class<? extends BooleanSupplier> include() default AlwaysIncluded.class;

    /**
     * The native bridge configuration. The returned class must have an accessible static
     * {@code getInstance()} method returning a {@link JNIConfig} instance. The returned
     * {@link JNIConfig} instance is used for marshallers lookup.
     */
    Class<?> jniConfig();

    class AlwaysIncluded implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return true;
        }
    }
}
