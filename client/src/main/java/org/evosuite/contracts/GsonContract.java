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
import java.lang.reflect.TypeVariable;
import java.util.List;
import org.evosuite.Properties;
import org.evosuite.testcase.execution.Scope;
import org.evosuite.testcase.statements.Statement;
import org.evosuite.testcase.variable.VariableReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A contract checking for equality of an object before and after serialization with GSON.
 *
 * <p>Each object will be fed to Google's GSON library, serialized to JSON format, and de-serialized
 * back from JSON. The resulting object should be equal to the initial object. If this is not the
 * case, we also compare the XML representations, created by XStream; this is because we want to
 * remove false positives that occur only due to an insufficient {@code equals} implementation of
 * some types.
 *
 * @author Stephan Lukasczyk
 */
public class GsonContract extends Contract {

  private static final Logger logger = LoggerFactory.getLogger(Contract.class);

  private final Gson gson;
  private final XStream xstream;

  GsonContract() {
    gson = new Gson();
    xstream = new XStream();
  }

  /** {@inheritDoc} */
  @Override
  public ContractViolation check(
      final Statement statement, final Scope scope, final Throwable exception) {
    for (Object originalObject : scope.getObjects()) {
      // if object has generic type, ignore it, because GSON cannot handle those properly
      final TypeVariable<? extends Class<?>>[] typeParameters =
          originalObject.getClass().getTypeParameters();
      if (typeParameters.length > 0) {
        continue;
      }

      // consider only objects of the target class' type to decrease number of false positives
      if (!originalObject.getClass().equals(Properties.getTargetClassAndDontInitialise())) {
        continue;
      }

      // skip Object, as it will not be de-serialized correctly
      if ("Object".equals(originalObject.getClass().getSimpleName())) {
        continue;
      }

      final String gsonResult = gson.toJson(originalObject);
      final Object gsonDeserialize = gson.fromJson(gsonResult, originalObject.getClass());

      if (!originalObject.equals(gsonDeserialize)) {
        final String oXML = xstream.toXML(originalObject);
        final String deserializeXML = xstream.toXML(gsonDeserialize);
        if (!oXML.equals(deserializeXML)) {
          logger.debug(
              "Found a contract violation.\n\t\tOrig: "
                  + originalObject
                  + "\n\t\tGSON: "
                  + gsonDeserialize
                  + "\n\t\tJSON: "
                  + gsonResult);
          return new ContractViolation(this, statement, exception);
        }
      }
    }
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public void addAssertionAndComments(
      final Statement statement,
      final List<VariableReference> variables,
      final Throwable exception) {
    final String vars =
        variables != null && variables.size() > 0 ? ", Variables: " + variables.toString() : "";
    final String ex = exception != null ? ", Exception: " + exception.getMessage() : "";
    statement.addComment("GsonContract failed. Statement: " + statement + vars + ex);
  }
}
