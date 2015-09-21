package io.wcm.caravan.pipeline.extensions.hal.crawler;

public interface StopCriteria {
	
  /**
   * @return true if crawler should be stopped
   */
  boolean isStopped();

  /**
   * @return Unique ID
   */
  String getId();  	  

}
