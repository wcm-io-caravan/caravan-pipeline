package io.wcm.caravan.pipeline.extensions.hal.crawler;


public final class StopCriteria {

  private StopCriteria() {
    // nothing to do
  }

  /**
   * Allows crawler to be enabled always
   * @return stop criteria which is always enabled
   */
  public static StopCriterion alwaysEnabled() {
    return new StopCriterion() {

      @Override
      public boolean isStopRequested() {
        return false;
      }

      @Override
      public String getId() {
        return "ALWAYS-ENABLED";
      }

    };
  }
  
}
