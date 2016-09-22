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

import java.net.URLEncoder

import auth.{MockAuthConnector, MockConfig}
import common.Constants
import config.{FrontendAppConfig, FrontendAuthConnector}
import connectors.KeystoreConnector
import controllers.helpers.FakeRequestHelper
import models.ConfirmCorrespondAddressModel
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class ConfirmCorrespondAddressControllerSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with OneServerPerSuite with FakeRequestHelper {

  val mockKeyStoreConnector = mock[KeystoreConnector]


  object ConfirmCorrespondAddressControllerTest extends ConfirmCorrespondAddressController {
    override lazy val applicationConfig = FrontendAppConfig
    override lazy val authConnector = MockAuthConnector
    val keyStoreConnector: KeystoreConnector = mockKeyStoreConnector
  }

  val model = ConfirmCorrespondAddressModel(Constants.StandardRadioButtonYesValue)
  val cacheMap: CacheMap = CacheMap("", Map("" -> Json.toJson(model)))
  val keyStoreSavedConfirmCorrespondAddress = ConfirmCorrespondAddressModel(Constants.StandardRadioButtonYesValue)

  implicit val hc = HeaderCarrier()

  override def beforeEach() {
    reset(mockKeyStoreConnector)
  }

  "ConfirmCorrespondAddressController" should {
    "use the correct keystore connector" in {
      ConfirmCorrespondAddressController.keyStoreConnector shouldBe KeystoreConnector
    }
  }

  "ConfirmCorrespondAddressController" should {
    "use the correct auth connector" in {
      ConfirmCorrespondAddressController.authConnector shouldBe FrontendAuthConnector
    }
  }

  "Sending an Authenticated GET request with a session to ConfirmCorrespondAddressController" should {
    "return a 200 when something is fetched from keystore" in {
      when(mockKeyStoreConnector.saveFormData(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(cacheMap)
      when(mockKeyStoreConnector.fetchAndGetFormData[ConfirmCorrespondAddressModel](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Option(keyStoreSavedConfirmCorrespondAddress)))
      showWithSessionAndAuth(ConfirmCorrespondAddressControllerTest.show())(
        result => status(result) shouldBe OK
      )
    }

    "provide an empty model and return a 200 when nothing is fetched using keystore" in {
      when(mockKeyStoreConnector.saveFormData(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(cacheMap)
      when(mockKeyStoreConnector.fetchAndGetFormData[ConfirmCorrespondAddressModel](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(None))
      showWithSessionAndAuth(ConfirmCorrespondAddressControllerTest.show())(
        result => status(result) shouldBe OK
      )
    }
  }


  "Sending an Unauthenticated request with a session to ConfirmCorrespondAddressController" should {
    "return a 302 and redirect to the GG login page" in {
      showWithSessionWithoutAuth(ConfirmCorrespondAddressControllerTest.show())(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${FrontendAppConfig.ggSignInUrl}?continue=${
            URLEncoder.encode(MockConfig.introductionUrl, "UTF-8")
          }&origin=investment-tax-relief-submission-frontend&accountType=organisation")
        }
      )
    }
  }

  "Sending a request with no session to ConfirmCorrespondAddressController" should {
    "return a 302 and redirect to GG login" in {
      showWithoutSession(ConfirmCorrespondAddressControllerTest.show())(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${FrontendAppConfig.ggSignInUrl}?continue=${
            URLEncoder.encode(MockConfig.introductionUrl, "UTF-8")
          }&origin=investment-tax-relief-submission-frontend&accountType=organisation")
        }
      )
    }
  }

  "Sending a timed-out request to ConfirmCorrespondAddressController" should {
    "return a 302 and redirect to the timeout page" in {
      showWithTimeout(ConfirmCorrespondAddressControllerTest.show())(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.TimeoutController.timeout().url)
        }
      )
    }
  }


  "Submitting a valid form submission to ConfirmCorrespondAddressController while authenticated" should {
    "redirect Supporting Documents when the Yes option is selected" in {
      val formInput = "contactAddressUse" -> Constants.StandardRadioButtonYesValue
      submitWithSessionAndAuth(ConfirmCorrespondAddressControllerTest.submit, formInput)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/investment-tax-relief/supporting-documents")
        }
      )
    }
    "redirect to Contact Address page when the no option is selected" in {
      val formInput = "contactAddressUse" -> Constants.StandardRadioButtonNoValue

      submitWithSessionAndAuth(ConfirmCorrespondAddressControllerTest.submit, formInput)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/investment-tax-relief/contact-address")
        }
      )
    }
  }

  "Submitting a invalid form submission to ConfirmCorrespondAddressController while authenticated" should {
    "redirect to itself when there is validation errors" in {
       val formInput = "contactAddressUse" -> ""
       submitWithSessionAndAuth(ConfirmCorrespondAddressControllerTest.submit, formInput)(
          result => {
            status(result) shouldBe BAD_REQUEST
          }
        )
      }
    }

  "Submitting a form to ConfirmCorrespondAddressController with a session but not authenticated" should {

    val formInput = "contactAddressUse" -> Constants.StandardRadioButtonYesValue
    "return a 303 and redirect to the GG login page" in {
      submitWithSessionWithoutAuth(ConfirmCorrespondAddressControllerTest.submit, formInput)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${FrontendAppConfig.ggSignInUrl}?continue=${
            URLEncoder.encode(MockConfig.introductionUrl, "UTF-8")
          }&origin=investment-tax-relief-submission-frontend&accountType=organisation")
        }
      )
    }
  }

  "Submitting a form to ConfirmCorrespondAddressController with no session" should {

    val formInput = "contactAddressUse" -> Constants.StandardRadioButtonYesValue
    "return a 303 and redirect to the GG login page" in {
      submitWithoutSession(ConfirmCorrespondAddressControllerTest.submit, formInput)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${FrontendAppConfig.ggSignInUrl}?continue=${
            URLEncoder.encode(MockConfig.introductionUrl, "UTF-8")
          }&origin=investment-tax-relief-submission-frontend&accountType=organisation")
        }
      )
    }
  }

  "Submitting a form to ConfirmCorrespondAddressController with a timeout" should {

    val formInput = "contactAddressUse" -> Constants.StandardRadioButtonYesValue
    "return a 303 and redirect to the timeout page" in {
      submitWithTimeout(ConfirmCorrespondAddressControllerTest.submit, formInput)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.TimeoutController.timeout().url)
        }
      )
    }
  }
}
