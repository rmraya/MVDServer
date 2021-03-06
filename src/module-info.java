/*******************************************************************************
 * Copyright (c) 2019-2020 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/

 module mvdserver {
    exports com.maxprograms.mvdserver;

    requires json;
    requires transitive jdk.httpserver;
}