//
// Copyright (C) 2010 United States Government as represented by the
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

package gov.nasa.jpf.inspector.server.jpf;

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.inspector.interfaces.CommandsInterface.InspectorStates;
import gov.nasa.jpf.inspector.interfaces.InspectorCallBacks;
import gov.nasa.jpf.inspector.migration.MigrationUtilities;
import gov.nasa.jpf.inspector.server.expression.InspectorState;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.VM;

/**
 * Handles resuming and stopping JPF execution.
 * 
 * This class also holds current state of the SuT (JVM).
 *
 * Implementors: When modifying this class, make sure to think about thread safety and synchronization.
 */
public class StopHolder {
  private static final boolean DEBUG = false;

  /**
   * State of the stopped JVM.
   */
  private InspectorState inspState;
  /**
   * Inspector server.
   */
  private final JPFInspector inspector;
  private final InspectorCallBacks callbacks;

  /**
   * Guarded by class instance lock.
   */
  private boolean terminating = false;
  private boolean terminatingClientNotified = false;
  private boolean stopped = false;
  private boolean condTerminateAfterResume = false;

  public StopHolder (JPFInspector inspector, InspectorCallBacks callbacks) {
    this.inspector = inspector;
    this.callbacks = callbacks;
  }

  /**
   * Notifies the client that JPF is now stopped and then blocks execution until it is woken up by a notify call.
   * Executed by the JPF thread.
   *
   * Note: To prevent deadlocks cannot hold any lock if this methods is called.
   *
   * @param inspState Current state of the JVM and Inspector. Can't be null.
   */
  public void stopExecution (InspectorState inspState) {
    assert inspState != null;

    boolean terminate; // Local synchronized version of this.terminating variable
    synchronized (this) {
      terminate = terminating; // Use single value even if we leave synchronized block

      try {
        if (!terminate) {
          this.inspState = inspState;
          stopped = true;

          callbacks.notifyStateChange(InspectorStates.JPF_STOPPED, getLocationDetails(inspState));

          notifyAll(); // Notify all threads waiting for JPF to be stopped (they are woken up after the wait)
          wait();

          stopped = false;
          this.inspState = null;

          checkConditionTerminateAfterResume();

        }
      } catch (InterruptedException e) {
        stopped = false;
        if (DEBUG) {
          inspector.getDebugPrintStream().println(this.getClass().getSimpleName() + ": INTERRUPTED.");
        }
      }
    } // End of synchronized block

    // Cannot be in synchronized block when send and callback
    if (terminate) {
      notifyClientTerminating();
    } else {
      callbacks.notifyStateChange(InspectorStates.JPF_RUNNING, getLocationDetails(inspState));
    }
  }

  public synchronized void terminating () {
    terminating = true;
  }

  public synchronized void newJPF () {
    terminating = false;
    terminatingClientNotified = false;
  }

  /**
   * Gets current state of the SuT or NULL if execution not stopped.
   */
  public VM getJVM () {
    return inspState.getJVM();
  }

  /**
   * Gets current state of the JPF and of the inspector or NULL if execution not stopped.
   */
  public InspectorState getInspectorState () {
    return inspState;
  }

  public synchronized boolean isStopped () {
    return stopped;
  }

  /**
   * Blocks until the JPF (SuT) becomes stopped. If JPF is already stopped, then returns immediately.
   *
   * This method is thread-safe.
   */
  public synchronized void waitUntilStopped () {
    if (DEBUG) {
      inspector.getDebugPrintStream().println(this.getClass().getSimpleName() + ".waitUntilStopped()");
    }
    try {
      while (!isStopped()) {
        wait();
      }
    } catch (InterruptedException ignored) {
    }
    if (DEBUG) {
      inspector.getDebugPrintStream().println(this.getClass().getSimpleName() + ".waitUntilStopped() - exit");
    }
  }
  /**
   * The only blocked thread should be the JPF thread in the {@link #stopExecution(InspectorState)} method
   *
   * Threads which calls {@link #waitUntilStopped()}
   * a) before {@link #stopExecution(InspectorState)} are notified in {@link #stopExecution(InspectorState)}
   * b) after {@link #stopExecution(InspectorState)} are not blocked and pass through {@link #waitUntilStopped()} method.
   */
  public synchronized void resumeExecution () {
    assert (isStopped()); // Illegal usage


    this.stopped = false;
    notifyAll(); // TODO used to be: notify() only.]
  }

  private static String getLocationDetails(InspectorState inspState) {
    StringBuilder sb = new StringBuilder(100);

    VM vm = inspState.getJVM();
    assert vm != null;
    Instruction instr = MigrationUtilities.getLastInstruction(vm);
    if (instr == null) {
      return null;
    }

    sb.append("SuT ");
    if (vm.getCurrentThread() != null) {
      sb.append("(thread ")
        .append(vm.getCurrentThread().getId())
        .append(") ");
    }

    String sourceline = "source unavailable";
    if (instr.getSourceLine() != null) {
      sourceline = "source: " + instr.getSourceLine().trim();
    }
    sb.append("will now execute ")
            .append(instr.getMethodInfo().getSourceFileName())
            .append(":")
            .append(instr.getLineNumber())
            .append(" (")
            .append(instr.toString())
            .append("), " + sourceline);
    return sb.toString();
  }

  public void notifyClientTerminating () {
    if (!terminatingClientNotified) {
      callbacks.notifyStateChange(InspectorStates.JPF_TERMINATING, null);
      terminatingClientNotified = true;
      synchronized (this) {
        notifyAll(); // Added to unblock commands waiting for JPF to be stopped or terminated.
      }
    }
  }

  /**
   * Terminates the search after the resume if forward step is planned.
   * <p>Note: Forward step is detected by the actual InspectorListener
   * <p>Note: Needed to be able to do backward steps after property violation.
   * 
   * <p>Note: If more such "hook are required use Command pattern"
   */
  public void terminateAfterResume () {
    condTerminateAfterResume = true;
  }

  /**
   * Check if terminate the search if forward step is planned.
   */
  private void checkConditionTerminateAfterResume () {
    if (condTerminateAfterResume) {
      condTerminateAfterResume = false;

      // Check which step is planned after the resume
      InspectorListener listener = inspector.getInspectorListener();
      ListenerAdapter listenerMode = listener.getCurrentMode();
      if (!(listenerMode instanceof InspectorListenerModeSilent)) {
        // Not a backward step
        inspState.getSearch().terminate();
      }
    }
  }

}
