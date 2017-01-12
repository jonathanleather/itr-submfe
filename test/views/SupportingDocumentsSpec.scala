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

package views

import auth.{MockAuthConnector, MockConfig}
import common.KeystoreKeys
import config.FrontendAppConfig
import controllers.{SupportingDocumentsController, routes}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.test.Helpers._
import views.helpers.ViewSpec

import scala.concurrent.Future

class SupportingDocumentsSpec extends ViewSpec {

  object TestController extends SupportingDocumentsController {
    override lazy val applicationConfig = FrontendAppConfig
    override lazy val authConnector = MockAuthConnector
    override lazy val s4lConnector = mockS4lConnector
    override lazy val uploadFeature = false
    override lazy val enrolmentConnector = mockEnrolmentConnector
    override lazy val attachmentsFrontEndUrl = MockConfig.attachmentFileUploadUrl
  }
  
  def setupMocks(backLink: Option[String] = None): Unit =
    when(mockS4lConnector.fetchAndGetFormData[String](Matchers.eq(KeystoreKeys.backLinkSupportingDocs))(Matchers.any(), Matchers.any(),Matchers.any()))
      .thenReturn(Future.successful(backLink))

  "The Supporting documents page" should {

    "Verify that the Supporting documents page contains the correct elements with Correspondence Address back link" in new Setup {
      val document: Document = {
        setupMocks(Some(routes.ConfirmCorrespondAddressController.show().url))
        val result = TestController.show.apply(authorisedFakeRequest)
        Jsoup.parse(contentAsString(result))
      }
      document.title() shouldBe Messages("page.supportingDocuments.SupportingDocuments.title")
      document.getElementById("main-heading").text() shouldBe Messages("page.supportingDocuments.SupportingDocuments.heading")
      document.getElementById("next").text() shouldBe Messages("common.button.checkAnswers")
      document.body.getElementById("back-link").attr("href") shouldEqual routes.ConfirmCorrespondAddressController.show().url
      document.getElementById("description-one").text() shouldBe Messages("page.supportingDocuments.SupportingDocuments.text.one")
      document .select("tr").get(0).getElementById("supportingDocs-business-plan").text() shouldBe
        Messages("page.supportingDocuments.SupportingDocuments.bullet.one")
      document .select("tr").get(1).getElementById("supportingDocs-company-accounts").text() shouldBe
        Messages("page.supportingDocuments.SupportingDocuments.bullet.two")
      document .select("tr").get(2).getElementById("shareholder-agree").text() shouldBe
        Messages("page.supportingDocuments.SupportingDocuments.bullet.three")
      document .select("tr").get(3).getElementById("memorandum-docs").text() shouldBe
        Messages("page.supportingDocuments.SupportingDocuments.bullet.four")
      document .select("tr").get(4).getElementById("supportingDocs-prospectus").text() shouldBe
        Messages("page.supportingDocuments.SupportingDocuments.bullet.five")
      document.getElementById("description-two").text() shouldBe Messages("page.supportingDocuments.SupportingDocuments.text.two")
      document.body.getElementById("get-help-action").text shouldBe Messages("common.error.help.text")
      document.body.getElementById("progress-section").text shouldBe Messages("common.section.progress.company.details.five")
    }

  }
}
