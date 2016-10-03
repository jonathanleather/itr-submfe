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

import auth.{Enrolment, Identifier, MockAuthConnector, MockConfig}
import common.{Constants, KeystoreKeys}
import config.{FrontendAppConfig, FrontendAuthConnector}
import connectors.{EnrolmentConnector, KeystoreConnector}
import controllers.helpers.FakeRequestHelper
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.OneServerPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import org.scalatest.mock.MockitoSugar

import scala.concurrent.Future

class PreviousBeforeDOFCSControllerSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with OneServerPerSuite with FakeRequestHelper {

  val mockKeyStoreConnector = mock[KeystoreConnector]

  object PreviousBeforeDOFCSControllerTest extends PreviousBeforeDOFCSController {
    override lazy val applicationConfig = FrontendAppConfig
    override lazy val authConnector = MockAuthConnector
    val keyStoreConnector: KeystoreConnector = mockKeyStoreConnector
    override lazy val enrolmentConnector = mock[EnrolmentConnector]
  }

  private def mockEnrolledRequest = when(PreviousBeforeDOFCSControllerTest.enrolmentConnector.getTAVCEnrolment(Matchers.any())(Matchers.any()))
    .thenReturn(Future.successful(Option(Enrolment("HMRC-TAVC-ORG",Seq(Identifier("TavcReference","1234")),"Activated"))))

  private def mockNotEnrolledRequest = when(PreviousBeforeDOFCSControllerTest.enrolmentConnector.getTAVCEnrolment(Matchers.any())(Matchers.any()))
    .thenReturn(Future.successful(None))

  val modelYes = PreviousBeforeDOFCSModel(Constants.StandardRadioButtonYesValue)
  val modelNo = PreviousBeforeDOFCSModel(Constants.StandardRadioButtonNoValue)
  val emptyModel = PreviousBeforeDOFCSModel("")
  val cacheMap: CacheMap = CacheMap("", Map("" -> Json.toJson(modelYes)))
  val keyStoreSavedPreviousBeforeDOFCS = PreviousBeforeDOFCSModel(Constants.StandardRadioButtonYesValue)
  val keyStoreSavedSubsidiariesYes = SubsidiariesModel(Constants.StandardRadioButtonYesValue)
  val keyStoreSavedSubsidiariesNo = SubsidiariesModel(Constants.StandardRadioButtonNoValue)

  implicit val hc = HeaderCarrier()

  override def beforeEach() {
    reset(mockKeyStoreConnector)
  }

  "PreviousBeforeDOFCSController" should {
    "use the correct keystore connector" in {
      PreviousBeforeDOFCSController.keyStoreConnector shouldBe KeystoreConnector
    }
    "use the correct auth connector" in {
      PreviousBeforeDOFCSController.authConnector shouldBe FrontendAuthConnector
    }
  }

  "Sending a GET formInput to PreviousBeforeDOFCSController when Authenticated and enrolled" should {
    "return a 200 when something is fetched from keystore" in {
      when(mockKeyStoreConnector.saveFormData(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(cacheMap)
      when(mockKeyStoreConnector.fetchAndGetFormData[PreviousBeforeDOFCSModel](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Option(keyStoreSavedPreviousBeforeDOFCS)))
      mockEnrolledRequest
      showWithSessionAndAuth(PreviousBeforeDOFCSControllerTest.show())(
        result => status(result) shouldBe OK
      )
    }

    "provide an empty model and return a 200 when nothing is fetched using keystore when Authenticated and enrolled" in {
      when(mockKeyStoreConnector.saveFormData(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(cacheMap)
      when(mockKeyStoreConnector.fetchAndGetFormData[PreviousBeforeDOFCSModel](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(None))
      mockEnrolledRequest
      showWithSessionAndAuth(PreviousBeforeDOFCSControllerTest.show())(
        result => status(result) shouldBe OK
      )
    }
  }

  "Sending a GET formInput to PreviousBeforeDOFCSController when Authenticated and NOT enrolled" should {
    "redirect to the Subscription Service" in {
      when(mockKeyStoreConnector.saveFormData(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(cacheMap)
      when(mockKeyStoreConnector.fetchAndGetFormData[PreviousBeforeDOFCSModel](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Option(keyStoreSavedPreviousBeforeDOFCS)))
      mockNotEnrolledRequest
      showWithSessionAndAuth(PreviousBeforeDOFCSControllerTest.show())(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(FrontendAppConfig.subscriptionUrl)
        }
      )
    }
  }

  "Sending an Unauthenticated formInput with a session to PreviousBeforeDOFCSController when Authenticated and enrolled" should {
    "return a 302 and redirect to GG login" in {
      mockEnrolledRequest
      showWithSessionWithoutAuth(PreviousBeforeDOFCSControllerTest.show())(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${FrontendAppConfig.ggSignInUrl}?continue=${
            URLEncoder.encode(MockConfig.introductionUrl, "UTF-8")
          }&origin=investment-tax-relief-submission-frontend&accountType=organisation")
        }
      )
    }
  }

  "Sending a formInput with no session to PreviousBeforeDOFCSController when Authenticated and enrolled" should {
    "return a 302 and redirect to GG login" in {
      mockEnrolledRequest
      showWithoutSession(PreviousBeforeDOFCSControllerTest.show())(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${FrontendAppConfig.ggSignInUrl}?continue=${
            URLEncoder.encode(MockConfig.introductionUrl, "UTF-8")
          }&origin=investment-tax-relief-submission-frontend&accountType=organisation")
        }
      )
    }
  }

  "Sending a timed-out formInput to PreviousBeforeDOFCSController when Authenticated and enrolled" should {
    "return a 302 and redirect to the timeout page" in {
      mockEnrolledRequest
      showWithTimeout(PreviousBeforeDOFCSControllerTest.show())(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.TimeoutController.timeout().url)
        }
      )
    }
  }

  "Sending a valid 'No' form submit to the PreviousBeforeDOFCSController when Authenticated and enrolled" should {
    "redirect to new geographical market" in {
      mockEnrolledRequest
      val formInput = "previousBeforeDOFCS" -> Constants.StandardRadioButtonNoValue
      submitWithSessionAndAuth(PreviousBeforeDOFCSControllerTest.submit, formInput)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/investment-tax-relief/new-geographical-market")
        }
      )
    }
  }

  "Sending a valid 'Yes' form submit to the PreviousBeforeDOFCSController with 'No' to Subsidiaries Model when Authenticated and enrolled" should {
    "redirect to the how-plan-to-use-investment page" in {
     when(mockKeyStoreConnector.saveFormData(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(cacheMap)
      when(mockKeyStoreConnector.fetchAndGetFormData[SubsidiariesModel](Matchers.eq(KeystoreKeys.subsidiaries))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Option(keyStoreSavedSubsidiariesNo)))
      mockEnrolledRequest
      val formInput = "previousBeforeDOFCS" -> Constants.StandardRadioButtonYesValue
      submitWithSessionAndAuth(PreviousBeforeDOFCSControllerTest.submit, formInput)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/investment-tax-relief/how-plan-to-use-investment")
        }
      )
    }
  }

  "Sending a valid 'Yes' form submit to the PreviousBeforeDOFCSController with 'Yes' to Subsidiaries Model when Authenticated and enrolled" should {
    "redirect to the subsidiaries-spending-investment page" in {
      when(mockKeyStoreConnector.saveFormData(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(cacheMap)
      when(mockKeyStoreConnector.fetchAndGetFormData[SubsidiariesModel](Matchers.eq(KeystoreKeys.subsidiaries))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Option(keyStoreSavedSubsidiariesYes)))
      mockEnrolledRequest
      val formInput = "previousBeforeDOFCS" -> Constants.StandardRadioButtonYesValue
      submitWithSessionAndAuth(PreviousBeforeDOFCSControllerTest.submit, formInput)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/investment-tax-relief/subsidiaries-spending-investment")
        }
      )
    }
  }

  "Sending a valid 'No' form submit to the PreviousBeforeDOFCSController with 'Yes' to Subsidiaries Model when Authenticated and enrolled" should {
    "redirect to new geographical market" in {
      when(mockKeyStoreConnector.saveFormData(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(cacheMap)
      when(mockKeyStoreConnector.fetchAndGetFormData[SubsidiariesModel](Matchers.eq(KeystoreKeys.subsidiaries))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Option(keyStoreSavedSubsidiariesYes)))
      mockEnrolledRequest
      val formInput = "previousBeforeDOFCS" -> Constants.StandardRadioButtonNoValue
      submitWithSessionAndAuth(PreviousBeforeDOFCSControllerTest.submit, formInput)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/investment-tax-relief/new-geographical-market")
        }
      )
    }
  }

  "Sending a valid 'No' form submit to the PreviousBeforeDOFCSController with 'No' to Subsidiaries Model when Authenticated and enrolled" should {
    "redirect to new geographical market" in {
      when(mockKeyStoreConnector.saveFormData(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(cacheMap)
      when(mockKeyStoreConnector.fetchAndGetFormData[SubsidiariesModel](Matchers.eq(KeystoreKeys.subsidiaries))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Option(keyStoreSavedSubsidiariesNo)))
      mockEnrolledRequest
      val formInput = "previousBeforeDOFCS" -> Constants.StandardRadioButtonNoValue
      submitWithSessionAndAuth(PreviousBeforeDOFCSControllerTest.submit, formInput)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/investment-tax-relief/new-geographical-market")
        }
      )
    }
  }

  "Sending a valid form submit to the PreviousBeforeDOFCSController without a Subsidiaries Model when Authenticated and enrolled" should {
    "redirect to Subsidiaries page" in {
      when(mockKeyStoreConnector.saveFormData(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(cacheMap)
      when(mockKeyStoreConnector.fetchAndGetFormData[SubsidiariesModel](Matchers.eq(KeystoreKeys.subsidiaries))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(None))
      mockEnrolledRequest
      val formInput = "previousBeforeDOFCS" -> Constants.StandardRadioButtonYesValue
      submitWithSessionAndAuth(PreviousBeforeDOFCSControllerTest.submit, formInput)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/investment-tax-relief/subsidiaries")
        }
      )
    }
  }


  "Sending an invalid form submission with validation errors to the PreviousBeforeDOFCSController when Authenticated and enrolled" should {
    "redirect to itself" in {
      mockEnrolledRequest
      val formInput = "previousBeforeDOFCS" -> ""
      submitWithSessionAndAuth(PreviousBeforeDOFCSControllerTest.submit, formInput)(
        result => {
          status(result) shouldBe BAD_REQUEST
        }
      )
    }
  }


  "Sending a submission to the PreviousBeforeDOFCSController when not authenticated" should {

    "redirect to the GG login page when having a session but not authenticated" in {
      submitWithSessionWithoutAuth(PreviousBeforeDOFCSControllerTest.submit)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${FrontendAppConfig.ggSignInUrl}?continue=${
            URLEncoder.encode(MockConfig.introductionUrl, "UTF-8")
          }&origin=investment-tax-relief-submission-frontend&accountType=organisation")
        }
      )
    }

    "redirect to the GG login page with no session" in {
      submitWithoutSession(PreviousBeforeDOFCSControllerTest.submit)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${FrontendAppConfig.ggSignInUrl}?continue=${
            URLEncoder.encode(MockConfig.introductionUrl, "UTF-8")
          }&origin=investment-tax-relief-submission-frontend&accountType=organisation")
        }
      )
    }
  }

  "Sending a submission to the PreviousBeforeDOFCSController when a timeout has occurred" should {
    "redirect to the Timeout page when session has timed out" in {
      submitWithTimeout(PreviousBeforeDOFCSControllerTest.submit)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.TimeoutController.timeout().url)
        }
      )
    }
  }

  "Sending a submission to the PreviousBeforeDOFCSController when NOT enrolled" should {
    "redirect to the Subscription Service" in {
      mockNotEnrolledRequest
      submitWithSessionAndAuth(PreviousBeforeDOFCSControllerTest.submit)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(FrontendAppConfig.subscriptionUrl)
        }
      )
    }
  }
}
