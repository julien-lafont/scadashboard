package services

import com.google.inject.Inject

import actors.EventBus

/**
 * Helpers allowing to inject services into actors
 */
class Services @Inject()(
  val aws: AWS,
  val codeship: Codeship,
  val github: Github,
  val twitter: Twitter,
  val eventBus: EventBus
)
