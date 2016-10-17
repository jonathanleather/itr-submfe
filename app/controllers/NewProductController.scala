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
import forms.NewProductForm._
import models.{NewProductModel, SubsidiariesModel}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.mvc._

import scala.concurrent.Future
import views.html.investment.NewProduct

object NewProductController extends NewProductController{
  val s4lConnector: S4LConnector = S4LConnector
  override lazy val applicationConfig = FrontendAppConfig
  override lazy val authConnector = FrontendAuthConnector
  override lazy val enrolmentConnector = EnrolmentConnector
}

trait NewProductController extends FrontendController with AuthorisedAndEnrolledForTAVC {

  val s4lConnector: S4LConnector

  val show = AuthorisedAndEnrolled.async { implicit user => implicit request =>
    s4lConnector.fetchAndGetFormData[NewProductModel](KeystoreKeys.newProduct).map {
      case Some(data) => Ok(NewProduct(newProductForm.fill(data)))
      case None => Ok(NewProduct(newProductForm))
    }
  }

  val submit = AuthorisedAndEnrolled.async { implicit user => implicit request =>

    def routeRequest(hasSubsidiaries: Option[SubsidiariesModel]): Future[Result] = {
      hasSubsidiaries match {
        case Some(data) if data.ownSubsidiaries == Constants.StandardRadioButtonYesValue =>
          s4lConnector.saveFormData(KeystoreKeys.backLinkSubSpendingInvestment, routes.NewProductController.show().toString())
          Future.successful(Redirect(routes.SubsidiariesSpendingInvestmentController.show()))
        case Some(_) =>
          s4lConnector.saveFormData(KeystoreKeys.backLinkInvestmentGrow, routes.NewProductController.show().toString())
          Future.successful(Redirect(routes.InvestmentGrowController.show()))
        case None => Future.successful(Redirect(routes.SubsidiariesController.show()))
      }
    }

    newProductForm.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(NewProduct(formWithErrors)))
      },
      validFormData => {
        s4lConnector.saveFormData(KeystoreKeys.newProduct, validFormData)
        validFormData.isNewProduct match {
          case Constants.StandardRadioButtonYesValue => for {
            hasSubsidiaries <- s4lConnector.fetchAndGetFormData[SubsidiariesModel](KeystoreKeys.subsidiaries)
            route <- routeRequest(hasSubsidiaries)
          } yield route
          case _ => for {
            // TODO: Uncomment below with correct behaviour for 'No' error once decided (goes to ERR2)
            // i.e. replaces existing case Constants.StandardRadioButtonNoValue with line below
            //case Constants.StandardRadioButtonNoValue => Future.successful(Redirect(routes.TOBeDecidedController.show))
            date <- s4lConnector.fetchAndGetFormData[SubsidiariesModel](KeystoreKeys.subsidiaries)
            route <- routeRequest(date)
          } yield route
        }
      }
    )
  }
}