/*
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.contracts;

import com.google.gson.Gson;
import com.thoughtworks.xstream.XStream;
import org.evosuite.testcase.execution.Scope;
import org.evosuite.testcase.statements.Statement;
import org.evosuite.testcase.variable.VariableReference;

import java.util.List;

/**
 * A contract checking for equality of an object before and after serialization with GSON.
 *
 * @author Stephan Lukasczyk
 */
public class GsonContract extends Contract {

    private final Gson gson;
    private final XStream xstream;

    GsonContract() {
        gson = new Gson();
        xstream = new XStream();
    }

    @Override
    public ContractViolation check(Statement statement, Scope scope, Throwable exception) {
        for (Object originalObject : scope.getObjects()) {
            final String gsonResult = gson.toJson(originalObject);
            final Object gsonDeserialize = gson.fromJson(gsonResult, originalObject.getClass());

            if (!originalObject.equals(gsonDeserialize)) {
                final String oXML = xstream.toXML(originalObject);
                final String deserializeXML = xstream.toXML(gsonDeserialize);
                if (!oXML.equals(deserializeXML)) {
                    return new ContractViolation(this, statement, exception);
                }
            }
        }
        return null;
    }

    @Override
    public void addAssertionAndComments(
            Statement statement, List<VariableReference> variables, Throwable exception) {
        statement.addComment("Equality could neither be shown using GSON nor using XStream."
                + exception.getMessage());
    }
}
