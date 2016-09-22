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

import controllers.helpers.FakeRequestHelper
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.UnitSpec

class TimeoutControllerSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with OneServerPerSuite with FakeRequestHelper {

  "Sending a GET request to TimeoutController" should {
    "return a 200" in {
      status(TimeoutController.timeout(fakeRequest)) shouldBe OK
    }
  }
}
