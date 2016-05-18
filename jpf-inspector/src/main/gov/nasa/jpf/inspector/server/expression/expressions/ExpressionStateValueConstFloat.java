//
// Copyright (C) 2011 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
// 
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
// 
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//  

package gov.nasa.jpf.inspector.server.expression.expressions;

import gov.nasa.jpf.inspector.interfaces.JPFInspectorException;
import gov.nasa.jpf.inspector.migration.MigrationUtilities;
import gov.nasa.jpf.inspector.server.expression.InspectorState;
import gov.nasa.jpf.inspector.server.jpf.JPFInspector;
import gov.nasa.jpf.inspector.server.programstate.StateNodeInterface;
import gov.nasa.jpf.inspector.server.programstate.StateReadableConstValue;
import gov.nasa.jpf.vm.ClassInfo;

/**
 * @author Alf
 * 
 */
public class ExpressionStateValueConstFloat extends ExpressionStateValueConst {

  private final float value;

  public ExpressionStateValueConstFloat (float value) {
    super();
    this.value = value;
  }

  /*
   * @see gov.nasa.jpf.inspector.server.expression.ExpressionStateRootNode#getResultExpression(gov.nasa.jpf.inspector.server.jpf.JPFInspector,
   * gov.nasa.jpf.inspector.server.expression.InspectorState)
   */
  @Override
  public StateNodeInterface getResultExpression (JPFInspector inspector, InspectorState state) throws JPFInspectorException {
    return new StateReadableConstValue(inspector, 1, MigrationUtilities.getResolvedClassInfo("float"), Float.valueOf(value));
  }

  /* @see gov.nasa.jpf.inspector.server.expression.ExpressionNodeInterface#getNormalizedExpression() */
  @Override
  public String getNormalizedExpression () {
    if (value == Float.POSITIVE_INFINITY) {
      // TOKEN_POSITIVE_INFINITY2 : '+inf' ;
      return ExpressionStateValueConstDouble.TOKEN_POSITIVE_INFINITY2;
    }

    if (value == Float.NEGATIVE_INFINITY) {
      // TOKEN_NEGATIVE_INFINITY2 : '-inf' ;
      return ExpressionStateValueConstDouble.TOKEN_NEGATIVE_INFINITY2;
    }

    if (Float.isNaN(value)) {
      // TOKEN_NAN : 'NaN' ; // Not-a-number
      return ExpressionStateValueConstDouble.TOKEN_NAN;
    }

    // Ordinary number
    return Float.toString(value) + 'f';
  }

}
