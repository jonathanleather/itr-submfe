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
import config.{FrontendAppConfig, FrontendAuthConnector}
import connectors.{EnrolmentConnector, KeystoreConnector}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.mvc._
import forms.TurnoverCostsForm._
import common._
import models.AnnualTurnoverCostsModel
import models.submission.CostModel
import play.api.libs.json.Json
import views.html.investment.TurnoverCosts

import scala.concurrent.Future

object TurnoverCostsController extends TurnoverCostsController {
  val keyStoreConnector: KeystoreConnector = KeystoreConnector
  override lazy val applicationConfig = FrontendAppConfig
  override lazy val authConnector = FrontendAuthConnector
  override lazy val enrolmentConnector = EnrolmentConnector
}

trait TurnoverCostsController extends FrontendController with AuthorisedAndEnrolledForTAVC {

  implicit val formatCostModel = Json.format[CostModel]
  val keyStoreConnector: KeystoreConnector

  val show = AuthorisedAndEnrolled.async { implicit user => implicit request =>
    keyStoreConnector.fetchAndGetFormData[AnnualTurnoverCostsModel](KeystoreKeys.turnoverCosts).map {
      case Some(data) => Ok(TurnoverCosts(turnoverCostsForm.fill(data)))
      case None => Ok(TurnoverCosts(turnoverCostsForm))
    }
  }

  val submit = AuthorisedAndEnrolled.async { implicit user => implicit request =>
    turnoverCostsForm.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(TurnoverCosts(formWithErrors)))
      },
      validFormData => {
        //TODO: add the annual aveage turnover check and navigtion to error or correct page etc..subsidiaries temporary
        keyStoreConnector.saveFormData(KeystoreKeys.turnoverCosts, validFormData)
        Future.successful(Redirect(routes.SubsidiariesSpendingInvestmentController.show()))
      }
    )
  }
}