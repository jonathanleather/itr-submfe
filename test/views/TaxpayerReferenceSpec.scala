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

package views

import java.util.UUID

import connectors.KeystoreConnector

import controllers.{TaxpayerReferenceController, routes}
import controllers.helpers.FakeRequestHelper
import models.TaxpayerReferenceModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class TaxpayerReferenceSpec extends UnitSpec with WithFakeApplication with MockitoSugar with FakeRequestHelper{

  val mockKeystoreConnector = mock[KeystoreConnector]

  val taxpayerReferenceModel = new TaxpayerReferenceModel("1234567890")
  val emptyTaxpayerReferenceModel = new TaxpayerReferenceModel("")

  class SetupPage {

    val controller = new TaxpayerReferenceController{
      val keyStoreConnector: KeystoreConnector = mockKeystoreConnector
    }
  }

  "The Contact Details page" should {

    "Verify that the taxpayer reference page contains the correct elements when a valid TaxpayerReferenceModel is passed" in new SetupPage {
      val document: Document = {
        val userId = s"user-${UUID.randomUUID}"

        when(mockKeystoreConnector.fetchAndGetFormData[TaxpayerReferenceModel](Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(Option(taxpayerReferenceModel)))
        val result = controller.show.apply((fakeRequestWithSession.withFormUrlEncodedBody(
          "utr" -> "1234567890"
        )))
        Jsoup.parse(contentAsString(result))
      }

      document.title() shouldBe Messages("page.companyDetails.utr.title")
      document.getElementById("main-heading").text() shouldBe Messages("page.companyDetails.utr.heading")
      document.getElementById("help").text() shouldBe Messages("page.companyDetails.utr.help.link")
      document.getElementById("help-text").text() shouldBe Messages("page.companyDetails.utr.help.text")
      document.getElementById("label-utr").select("span").hasClass("visuallyhidden") shouldBe true
      document.getElementById("label-utr").select(".visuallyhidden").text() shouldBe Messages("page.companyDetails.utr.heading")
      document.getElementById("label-utr-hint").text() shouldBe Messages("page.companyDetails.utr.question.hint")
      document.getElementById("next").text() shouldBe Messages("common.button.continue")
      document.body.getElementById("back-link").attr("href") shouldEqual routes.WhatWeAskYouController.show.toString()
      document.body.getElementById("progress-section").text shouldBe  Messages("common.section.progress.company.details.one")
    }

    "Verify that the taxpayer reference page contains the correct elements when an invalid TaxpayerReferenceModel is passed" in new SetupPage {
      val document: Document = {
        val userId = s"user-${UUID.randomUUID}"

        when(mockKeystoreConnector.fetchAndGetFormData[TaxpayerReferenceModel](Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(Option(emptyTaxpayerReferenceModel)))
        val result = controller.submit.apply((fakeRequestWithSession))
        Jsoup.parse(contentAsString(result))
      }
      document.title() shouldBe Messages("page.companyDetails.utr.title")
      document.getElementById("main-heading").text() shouldBe Messages("page.companyDetails.utr.heading")
      document.getElementById("help").text() shouldBe Messages("page.companyDetails.utr.help.link")
      document.getElementById("help-text").text() shouldBe Messages("page.companyDetails.utr.help.text")
      document.getElementById("label-utr").select("span").hasClass("visuallyhidden") shouldBe true
      document.getElementById("label-utr").select(".visuallyhidden").text() shouldBe Messages("page.companyDetails.utr.heading")
      document.getElementById("label-utr-hint").text() shouldBe Messages("page.companyDetails.utr.question.hint")
      document.getElementById("next").text() shouldBe Messages("common.button.continue")
      document.body.getElementById("back-link").attr("href") shouldEqual routes.WhatWeAskYouController.show.toString()
      document.body.getElementById("progress-section").text shouldBe  Messages("common.section.progress.company.details.one")
      document.getElementById("error-summary-display").hasClass("error-summary--show")
    }

  }

}