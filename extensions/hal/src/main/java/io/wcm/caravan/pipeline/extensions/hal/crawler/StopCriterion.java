package io.wcm.caravan.pipeline.extensions.hal.crawler;

public interface StopCriterion {
	
  /**
   * @return true if crawler should be stopped
   */
  boolean isStopRequested();

  /**
   * @return Unique ID
   */
  String getId();  	  

}
