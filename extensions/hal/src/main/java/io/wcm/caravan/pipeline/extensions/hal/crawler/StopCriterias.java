package io.wcm.caravan.pipeline.extensions.hal.crawler;


public final class StopCriterias {

  private StopCriterias() {
    // nothing to do
  }

  /**
   * Allows crawler to be enabled always
   * @return stop criteria which is always enabled
   */
  public static StopCriteria alwaysEnabled() {
    return new StopCriteria() {

      @Override
      public boolean isStopped() {
        return false;
      }

      @Override
      public String getId() {
        return "ALWAYS-ENABLED-CRITERIA";
      }

    };
  }
  
}
