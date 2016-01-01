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

package org.xeustechnologies.jcl.exception;

/**
 * @author Kamran
 */
public class JclContextException extends JclException {

    /**
     * serialVersionUID:long
     */
    private static final long serialVersionUID = -799657685317877954L;

    public JclContextException() {
        super();
    }

    public JclContextException(String message, Throwable cause) {
        super(message, cause);
    }

    public JclContextException(String message) {
        super(message);
    }

    public JclContextException(Throwable cause) {
        super(cause);
    }
}