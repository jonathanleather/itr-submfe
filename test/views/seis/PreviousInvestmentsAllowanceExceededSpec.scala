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

package views.seis

import auth.MockAuthConnector
import config.FrontendAppConfig
import controllers.seis.{PreviousInvestmentsAllowanceExceededController, routes}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.test.Helpers._
import views.helpers.ViewSpec
import play.api.i18n.Messages.Implicits._

class PreviousInvestmentsAllowanceExceededSpec extends ViewSpec {

  object TestController extends PreviousInvestmentsAllowanceExceededController {
    override lazy val applicationConfig = FrontendAppConfig
    override lazy val authConnector = MockAuthConnector
    override lazy val enrolmentConnector = mockEnrolmentConnector
  }

  "The Invalid Previous Scheme error page" should {

    "Verify that start page contains the correct elements" in new Setup {
      val document: Document = {
        val result = TestController.show.apply(authorisedFakeRequest)
        Jsoup.parse(contentAsString(result))
      }
      document.title shouldEqual Messages("page.previousInvestment.previousInvestmentsExceededExceeded.title")
      document.body.getElementById("main-heading").text() shouldEqual Messages("page.previousInvestment.InvalidPreviousScheme.heading")
      document.body.getElementById("investments-exceeded-reason").text() shouldEqual Messages("page.previousInvestment.previousInvestmentsExceededExceeded.reason")
      document.body.getElementById("change-answers-text").text() shouldEqual Messages("page.previousInvestment.InvalidPreviousScheme.change-text")
      document.body.getElementById("previous-seis-investments").text() shouldEqual Messages("page.previousInvestment.previousInvestmentsExceededExceeded.bullet.one")
      document.body.getElementById("de-minimis-aid").text() shouldEqual Messages("page.previousInvestment.previousInvestmentsExceededExceeded.bullet.two")
      document.body.getElementById("back-link").attr("href") shouldEqual routes.ReviewPreviousSchemesController.show().url
      document.body.getElementById("change-answers-link").attr("href") shouldEqual routes.ReviewPreviousSchemesController.show().url
    }
  }
}