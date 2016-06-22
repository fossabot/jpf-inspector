package gov.nasa.jpf.inspector.interfaces;


/**
 * Indicates whether a thread should be permitted to be scheduled when a choice generator chooses a thread to schedule.
 */
public enum ThreadSchedulingOption {
  /**
   * The thread should be allowed to be scheduled to be run. This is the default state.
   */
  SCHEDULE_AS_NORMAL,
  /**
   * The thread cannot be scheduled to run.
   */
  DO_NOT_SCHEDULE
}
