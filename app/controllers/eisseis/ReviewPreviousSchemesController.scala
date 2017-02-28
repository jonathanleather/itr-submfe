/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.eisseis

import auth._
import common.{Constants, KeystoreKeys}
import config.FrontendGlobal._
import config.{FrontendAppConfig, FrontendAuthConnector}
import connectors.{EnrolmentConnector, S4LConnector, SubmissionConnector}
import controllers.Helpers.{ControllerHelpers, EisSeisHelper, PreviousSchemesHelper}
import controllers.predicates.FeatureSwitch
import play.Logger
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html.eisseis.previousInvestment.ReviewPreviousSchemes
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future

object ReviewPreviousSchemesController extends ReviewPreviousSchemesController {
  override lazy val s4lConnector = S4LConnector
  override lazy val applicationConfig = FrontendAppConfig
  override lazy val authConnector = FrontendAuthConnector
  override lazy val enrolmentConnector = EnrolmentConnector
  override lazy val submissionConnector = SubmissionConnector
}

trait ReviewPreviousSchemesController extends FrontendController with AuthorisedAndEnrolledForTAVC with FeatureSwitch with PreviousSchemesHelper {

  override val acceptedFlows = Seq(Seq(EIS,SEIS,VCT),Seq(SEIS,VCT), Seq(EIS,SEIS))

  val submissionConnector: SubmissionConnector

  def show: Action[AnyContent] = featureSwitch(applicationConfig.seisFlowEnabled) {
    AuthorisedAndEnrolled.async { implicit user => implicit request =>
      def routeRequest(backUrl: Option[String]) = {
        if (backUrl.isDefined) {
          PreviousSchemesHelper.getAllInvestmentFromKeystore(s4lConnector).map {
            previousSchemes =>
              if(previousSchemes.nonEmpty) {
                if (!previousSchemes.exists(scheme => scheme.schemeTypeDesc == Constants.schemeTypeEis || scheme.schemeTypeDesc == Constants.schemeTypeVct))
                  EisSeisHelper.setIneligiblePreviousSchemeTypeCondition(s4lConnector, previousSchemeTypeConditionIneligible = false)
                Ok(ReviewPreviousSchemes(previousSchemes, backUrl.get))
              }
              else Redirect(routes.HadPreviousRFIController.show())
          }
        } else {
          // no back link - send to beginning of flow
          Future.successful(Redirect(routes.HadPreviousRFIController.show()))
        }
      }
      for {
        link <- ControllerHelpers.getSavedBackLink(KeystoreKeys.backLinkReviewPreviousSchemes, s4lConnector)
        route <- routeRequest(link)
      } yield route
    }
  }

  def add: Action[AnyContent] = featureSwitch(applicationConfig.seisFlowEnabled) {
    AuthorisedAndEnrolled.async { implicit user => implicit request =>
      s4lConnector.saveFormData(KeystoreKeys.backLinkPreviousScheme, routes.ReviewPreviousSchemesController.show().url)
      Future.successful(Redirect(routes.PreviousSchemeController.show(None)))
    }
  }

  def change(id: Int): Action[AnyContent] = featureSwitch(applicationConfig.seisFlowEnabled) {
    AuthorisedAndEnrolled.apply { implicit user => implicit request =>
      s4lConnector.saveFormData(KeystoreKeys.backLinkPreviousScheme, routes.ReviewPreviousSchemesController.show().url)
      Redirect(routes.PreviousSchemeController.show(Some(id)))
    }
  }

  def remove(id: Int): Action[AnyContent] = featureSwitch(applicationConfig.seisFlowEnabled) {
    AuthorisedAndEnrolled.async { implicit user => implicit request =>
      s4lConnector.saveFormData(KeystoreKeys.backLinkPreviousScheme, routes.ReviewPreviousSchemesController.show().url)
      PreviousSchemesHelper.removeKeystorePreviousInvestment(s4lConnector, id).map {
        _ => Redirect(routes.ReviewPreviousSchemesController.show())
      }
    }
  }

  def submit: Action[AnyContent] = featureSwitch(applicationConfig.seisFlowEnabled) {
    AuthorisedAndEnrolled.async { implicit user => implicit request =>
      s4lConnector.saveFormData(KeystoreKeys.backLinkProposedInvestment, routes.ReviewPreviousSchemesController.show().url)
      PreviousSchemesHelper.previousInvestmentsExist(s4lConnector).map {
        case true => Redirect(routes.ProposedInvestmentController.show())
        case false => Redirect(routes.ReviewPreviousSchemesController.show())
      }.recover {
        case e: NoSuchElementException => Redirect(routes.ProposedInvestmentController.show())
        case e: Exception =>
          Logger.warn(s"[ReviewPreviousSchemesController][submit] - Exception checkPreviousInvestmentSeisAllowanceExceeded: ${e.getMessage}")
          InternalServerError(internalServerErrorTemplate)
      }
    }
  }

}
