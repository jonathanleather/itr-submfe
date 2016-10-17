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
import connectors.{EnrolmentConnector, S4LConnector, SubmissionConnector}
import controllers.Helpers.{ControllerHelpers, PreviousSchemesHelper}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.mvc._
import models.{HadPreviousRFIModel, KiProcessingModel, ProposedInvestmentModel}
import common._
import common.Constants._
import forms.ProposedInvestmentForm._

import scala.concurrent.Future
import views.html.investment.ProposedInvestment

object ProposedInvestmentController extends ProposedInvestmentController
{
  val s4lConnector: S4LConnector = S4LConnector
  val submissionConnector: SubmissionConnector = SubmissionConnector
  override lazy val applicationConfig = FrontendAppConfig
  override lazy val authConnector = FrontendAuthConnector
  override lazy val enrolmentConnector = EnrolmentConnector
}

trait ProposedInvestmentController extends FrontendController with AuthorisedAndEnrolledForTAVC {

  val s4lConnector: S4LConnector
  val submissionConnector: SubmissionConnector

  val show: Action[AnyContent] = AuthorisedAndEnrolled.async { implicit user => implicit request =>
    def routeRequest(backUrl: Option[String]) = {
      if (backUrl.isDefined) {
        s4lConnector.fetchAndGetFormData[ProposedInvestmentModel](KeystoreKeys.proposedInvestment).map {
          case Some(data) => Ok(ProposedInvestment(proposedInvestmentForm.fill(data), backUrl.get))
          case None => Ok(ProposedInvestment(proposedInvestmentForm, backUrl.get))
        }

      } else {
        // no back link - send to beginning of flow
        Future.successful(Redirect(routes.HadPreviousRFIController.show()))
      }
    }
    for {
      link <- ControllerHelpers.getSavedBackLink(KeystoreKeys.backLinkProposedInvestment, s4lConnector)(hc)
      route <- routeRequest(link)
    } yield route
  }

  val submit = AuthorisedAndEnrolled.async { implicit user => implicit request =>

    def routeRequest(kiModel: Option[KiProcessingModel], isLifeTimeAllowanceExceeded: Option[Boolean]): Future[Result] = {
      kiModel match {
        // check previous answers present
        case Some(data) if isMissingKiData(data) => {
          Future.successful(Redirect(routes.DateOfIncorporationController.show()))
        }
        case Some(dataWithPreviousValid) => {
          // all good - TODO:Save the lifetime exceeded flag? - decide how to handle. For now I put it in keystore..
          s4lConnector.saveFormData(KeystoreKeys.lifeTimeAllowanceExceeded, isLifeTimeAllowanceExceeded)

          isLifeTimeAllowanceExceeded match {
            case Some(data) =>
              // if it's exceeded go to the error page
              if (data) {
                Future.successful(Redirect(routes.LifetimeAllowanceExceededController.show()))
              }
              else {
                // not exceeded - continue
                Future.successful(Redirect(routes.WhatWillUseForController.show()))
              }

            // if none, redirect back to HadPreviousRFI page.
            // Will only hit this if there is no backend connected.
            case None => Future.successful(Redirect(routes.HadPreviousRFIController.show()))
          }
        }
        case None => Future.successful(Redirect(routes.DateOfIncorporationController.show()))
      }
    }

    proposedInvestmentForm.bindFromRequest().fold(
      formWithErrors => {
        ControllerHelpers.getSavedBackLink(KeystoreKeys.backLinkProposedInvestment, s4lConnector).flatMap(url =>
          Future.successful(BadRequest(ProposedInvestment(formWithErrors,
            url.getOrElse(routes.HadPreviousRFIController.show().toString)))))
      },
      validFormData => {
        s4lConnector.saveFormData(KeystoreKeys.proposedInvestment, validFormData)
        for {
          kiModel <- s4lConnector.fetchAndGetFormData[KiProcessingModel](KeystoreKeys.kiProcessingModel)
          hadPrevRFI <- s4lConnector.fetchAndGetFormData[HadPreviousRFIModel](KeystoreKeys.hadPreviousRFI)
          previousInvestments <- PreviousSchemesHelper.getPreviousInvestmentTotalFromKeystore(s4lConnector)
          // Call API

          isLifeTimeAllowanceExceeded <- submissionConnector.checkLifetimeAllowanceExceeded(
            if(hadPrevRFI.get.hadPreviousRFI == StandardRadioButtonYesValue) true else false,
            if (kiModel.isDefined) kiModel.get.isKi else false, previousInvestments,
            validFormData.investmentAmount)

          route <- routeRequest(kiModel, isLifeTimeAllowanceExceeded)
        } yield route
      }
    )
  }

  def isMissingKiData(data: KiProcessingModel): Boolean = {

    false

//    if(data.companyAssertsIsKi.isEmpty){
//      true
//    }
//    else if (data.companyAssertsIsKi.get){
//      if(data.costsConditionMet.isEmpty){
//        true
//      } else {
//        if (!data.costsConditionMet.get){
//          data.secondaryCondtionsMet.isEmpty
//        } else false
//      }
//    }
//    else if (data.dateConditionMet.isEmpty) {
//      true
//    }
//    else {
//      false
//    }
  }
}

