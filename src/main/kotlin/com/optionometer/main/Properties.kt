package com.optionometer.main

import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class Properties : InitializingBean {

  @Value("\${trade.commission}")
  val commissionPerContract: Double = 0.0

  companion object {
    var commissionPerShare: Double = 0.0
  }

  override fun afterPropertiesSet() {
    commissionPerShare = commissionPerContract / 100
  }
}
