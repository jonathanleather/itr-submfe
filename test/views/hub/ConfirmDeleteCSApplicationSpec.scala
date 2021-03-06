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

package views.hub

import controllers.helpers.BaseSpec
import controllers.routes
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import views.html.hubPartials.ConfirmDeleteCSApplication

class ConfirmDeleteCSApplicationSpec  extends BaseSpec {

  val removeDescription = "Enterprise Investment Scheme"
  val schemeType = "Compliance Statement"

  "The Confirm Delete CS Application page" should {
    "contain the correct elements" in {

      lazy val page = ConfirmDeleteCSApplication(schemeType, removeDescription)(fakeRequest, applicationMessages)
      lazy val document = Jsoup.parse(page.body)

      //title and heading
      document.title() shouldBe Messages("page.deleteApplication.title.cs", schemeType)
      document.body.getElementById("main-heading").text() shouldBe Messages("page.deleteApplication.heading.cs", schemeType)
      document.body.getElementById("application-delete-hint").text() shouldBe removeDescription
      document.body.getElementById("application-remove").text() shouldBe Messages("page.deleteApplication.confirm")
      document.body.getElementById("application-cancel").text() shouldBe Messages("page.deleteApplication.cancel")
      document.body.getElementById("application-cancel").attr("href") shouldEqual routes.ApplicationHubController.show().url
      document.body.getElementById("back-link").attr("href") shouldEqual routes.ApplicationHubController.show().url
    }
  }

}
