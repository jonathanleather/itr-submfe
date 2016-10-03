/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import auth.AuthorisedAndEnrolledForTAVC
import common.KeystoreKeys
import config.{FrontendAppConfig, FrontendAuthConnector}
import connectors.{EnrolmentConnector, KeystoreConnector}
import controllers.Helpers.ControllerHelpers
import models.SubsidiariesModel
import forms.SubsidiariesForm._
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.mvc._

import scala.concurrent.Future
import views.html._
import views.html.companyDetails.Subsidiaries

object SubsidiariesController extends SubsidiariesController {
  val keyStoreConnector: KeystoreConnector = KeystoreConnector
  override lazy val applicationConfig = FrontendAppConfig
  override lazy val authConnector = FrontendAuthConnector
  override lazy val enrolmentConnector = EnrolmentConnector
}

trait SubsidiariesController extends FrontendController with AuthorisedAndEnrolledForTAVC {

  val keyStoreConnector: KeystoreConnector

  val show = AuthorisedAndEnrolled.async { implicit user => implicit request =>

    def routeRequest(backUrl: Option[String]) = {
      if (backUrl.isDefined) {

        keyStoreConnector.fetchAndGetFormData[SubsidiariesModel](KeystoreKeys.subsidiaries) map {
          case Some(data) => Ok(companyDetails.Subsidiaries(subsidiariesForm.fill(data), backUrl.get))
          case None => Ok(Subsidiaries(subsidiariesForm, backUrl.get))
        }
      }
      else {
        // no back link - user skipping - redirect to start of flow point
        Future.successful(Redirect(routes.DateOfIncorporationController.show()))
      }
    }

    for {
      link <- ControllerHelpers.getSavedBackLink(KeystoreKeys.backLinkSubsidiaries, keyStoreConnector)(hc)
      route <- routeRequest(link)
    } yield route
  }

  val submit = AuthorisedAndEnrolled.async { implicit user => implicit request =>
    subsidiariesForm.bindFromRequest.fold(
      invalidForm => ControllerHelpers.getSavedBackLink(KeystoreKeys.backLinkSubsidiaries, keyStoreConnector)(hc)
        .flatMap(url => Future.successful(BadRequest(companyDetails.Subsidiaries(invalidForm, url.
          getOrElse(routes.DateOfIncorporationController.show().toString))))),
      validForm => {
        keyStoreConnector.saveFormData[SubsidiariesModel](KeystoreKeys.subsidiaries, validForm)
        Future.successful(Redirect(routes.HadPreviousRFIController.show()))
      }
    )
  }
}
