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

import connectors.{KeystoreConnector, SubmissionConnector}
import controllers.predicates.ValidActiveSession
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.mvc._
import models.{KiProcessingModel, TenYearPlanModel}
import common._
import forms.TenYearPlanForm._
import views.html.knowledgeIntensive.TenYearPlan

import scala.concurrent.Future

object TenYearPlanController extends TenYearPlanController {
  val keyStoreConnector: KeystoreConnector = KeystoreConnector
  val submissionConnector: SubmissionConnector = SubmissionConnector
}

trait TenYearPlanController extends FrontendController with ValidActiveSession {

  val keyStoreConnector: KeystoreConnector
  val submissionConnector: SubmissionConnector

  val show = ValidateSession.async { implicit request =>
    keyStoreConnector.fetchAndGetFormData[TenYearPlanModel](KeystoreKeys.tenYearPlan).map {
      case Some(data) => Ok(TenYearPlan(tenYearPlanForm.fill(data)))
      case None => Ok(TenYearPlan(tenYearPlanForm))
    }
  }

  val submit = Action.async { implicit request =>

    def routeRequest(kiModel: Option[KiProcessingModel], hasTenYearPlan: Boolean,
                     isSecondaryKiConditionsMet: Option[Boolean]): Future[Result] = {
      kiModel match {
        // check previous answers present
        case Some(data) if isMissingData(data) =>
          /**not sure if we are still using isMissingData**/
          Future.successful(Redirect(routes.DateOfIncorporationController.show()))
        case Some(dataWithPrevious) if !dataWithPrevious.companyAssertsIsKi.get =>
          Future.successful(Redirect(routes.IsKnowledgeIntensiveController.show()))
        case Some(dataWithPreviousValid) => {
          // all good - save the cost condition result returned from API and navigate accordingly
          val updatedModel = dataWithPreviousValid.copy(secondaryCondtionsMet = isSecondaryKiConditionsMet,
            hasTenYearPlan = Some(hasTenYearPlan))
          keyStoreConnector.saveFormData(KeystoreKeys.kiProcessingModel, updatedModel)

          // check the current model to see if valid as this is last page but user could navigate via url out of sequence
          if (updatedModel.isKi) {
            keyStoreConnector.saveFormData(KeystoreKeys.backLinkSubsidiaries, routes.TenYearPlanController.show().toString())
            Future.successful(Redirect(routes.SubsidiariesController.show()))
          }
          else {
            // KI condition not met. end of the road..
            keyStoreConnector.saveFormData(KeystoreKeys.backLinkIneligibleForKI, routes.TenYearPlanController.show().toString())
            Future.successful(Redirect(routes.IneligibleForKIController.show()))
          }
        }
        case None => Future.successful(Redirect(routes.DateOfIncorporationController.show()))
      }
    }

    tenYearPlanForm.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(TenYearPlan(formWithErrors)))
      },
      validFormData => {
        keyStoreConnector.saveFormData(KeystoreKeys.tenYearPlan, validFormData)
        val hasTenYearPlan: Boolean = if (validFormData.hasTenYearPlan ==
          Constants.StandardRadioButtonYesValue) true
        else false
        for {
          kiModel <- keyStoreConnector.fetchAndGetFormData[KiProcessingModel](KeystoreKeys.kiProcessingModel)
          // Call API
          isSecondaryKiConditionsMet <- submissionConnector.validateSecondaryKiConditions(
            if (kiModel.isDefined) kiModel.get.hasPercentageWithMasters.getOrElse(false) else false, hasTenYearPlan) //TO DO - PROPER API CALL
          route <- routeRequest(kiModel, hasTenYearPlan, isSecondaryKiConditionsMet)
        } yield route
      }
    )
  }

  def isMissingData(data: KiProcessingModel): Boolean = {
    data.dateConditionMet.isEmpty || data.companyAssertsIsKi.isEmpty ||
      data.costsConditionMet.isEmpty || data.hasPercentageWithMasters.isEmpty
  }

}
