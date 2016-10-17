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
import common.{Constants, KeystoreKeys}
import config.{FrontendAppConfig, FrontendAuthConnector}
import connectors.{EnrolmentConnector, S4LConnector}
import forms.ConfirmCorrespondAddressForm._
import models.ConfirmCorrespondAddressModel
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html.contactInformation.ConfirmCorrespondAddress

import scala.concurrent.Future

object ConfirmCorrespondAddressController extends ConfirmCorrespondAddressController{
  val s4lConnector: S4LConnector = S4LConnector
  override lazy val applicationConfig = FrontendAppConfig
  override lazy val authConnector = FrontendAuthConnector
  override lazy val enrolmentConnector = EnrolmentConnector
}

trait ConfirmCorrespondAddressController extends FrontendController with AuthorisedAndEnrolledForTAVC {

  val s4lConnector: S4LConnector

  val show = AuthorisedAndEnrolled.async { implicit user => implicit request =>
    s4lConnector.fetchAndGetFormData[ConfirmCorrespondAddressModel](KeystoreKeys.confirmContactAddress).map {
      case Some(data) => Ok(ConfirmCorrespondAddress(confirmCorrespondAddressForm.fill(data)))
      case None => Ok(ConfirmCorrespondAddress(confirmCorrespondAddressForm))
    }
  }

  val submit = AuthorisedAndEnrolled.async { implicit user => implicit request =>
    confirmCorrespondAddressForm.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(ConfirmCorrespondAddress(formWithErrors)))
      },
      validFormData => {
        s4lConnector.saveFormData(KeystoreKeys.confirmContactAddress, validFormData)

        validFormData.contactAddressUse match {
          case Constants.StandardRadioButtonYesValue => {
            s4lConnector.saveFormData(KeystoreKeys.backLinkSupportingDocs,
              routes.ConfirmCorrespondAddressController.show().toString())
            Future.successful(Redirect(routes.SupportingDocumentsController.show()))
          }
          case Constants.StandardRadioButtonNoValue => Future.successful(Redirect(routes.ContactAddressController.show()))
        }
      }
    )
  }
}
