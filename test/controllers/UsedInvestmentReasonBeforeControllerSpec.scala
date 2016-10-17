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
import java.util.UUID

import auth.{Enrolment, Identifier, MockAuthConnector, MockConfig}
import builders.SessionBuilder
import common.Constants
import config.{FrontendAppConfig, FrontendAuthConnector}
import connectors.{EnrolmentConnector, S4LConnector}
import controllers.helpers.FakeRequestHelper
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class UsedInvestmentReasonBeforeControllerSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with OneServerPerSuite with FakeRequestHelper {

  val mockS4lConnector = mock[S4LConnector]

  object UsedInvestmentReasonBeforeControllerTest extends UsedInvestmentReasonBeforeController {
    override lazy val applicationConfig = FrontendAppConfig
    override lazy val authConnector = MockAuthConnector
    val s4lConnector: S4LConnector = mockS4lConnector
    override lazy val enrolmentConnector = mock[EnrolmentConnector]
  }

  private def mockEnrolledRequest = when(UsedInvestmentReasonBeforeControllerTest.enrolmentConnector.getTAVCEnrolment(Matchers.any())(Matchers.any()))
    .thenReturn(Future.successful(Option(Enrolment("HMRC-TAVC-ORG",Seq(Identifier("TavcReference","1234")),"Activated"))))

  private def mockNotEnrolledRequest = when(UsedInvestmentReasonBeforeControllerTest.enrolmentConnector.getTAVCEnrolment(Matchers.any())(Matchers.any()))
    .thenReturn(Future.successful(None))

  val modelYes = UsedInvestmentReasonBeforeModel(Constants.StandardRadioButtonYesValue)
  val modelNo = UsedInvestmentReasonBeforeModel(Constants.StandardRadioButtonNoValue)
  val emptyModel = UsedInvestmentReasonBeforeModel("")
  val cacheMap: CacheMap = CacheMap("", Map("" -> Json.toJson(modelYes)))
  val keyStoreSavedUsedInvestmentReasonBefore = UsedInvestmentReasonBeforeModel(Constants.StandardRadioButtonYesValue)

  implicit val hc = HeaderCarrier()

  override def beforeEach() {
    reset(mockS4lConnector)
  }

  when(mockS4lConnector.saveFormData(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(cacheMap)

  "UsedInvestmentReasonBeforeController" should {
    "use the correct keystore connector" in {
      UsedInvestmentReasonBeforeController.s4lConnector shouldBe S4LConnector
    }
    "use the correct auth connector" in {
      UsedInvestmentReasonBeforeController.authConnector shouldBe FrontendAuthConnector
    }
    "use the correct enrolment connector" in {
      UsedInvestmentReasonBeforeController.enrolmentConnector shouldBe EnrolmentConnector
    }
  }

  "Sending a GET request to UsedInvestmentReasonBeforeController when authenticated and enrolled" should {
    "return a 200 when something is fetched from keystore" in {
      when(mockS4lConnector.fetchAndGetFormData[UsedInvestmentReasonBeforeModel](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Option(keyStoreSavedUsedInvestmentReasonBefore)))
      mockEnrolledRequest
      showWithSessionAndAuth(UsedInvestmentReasonBeforeControllerTest.show)(
        result => status(result) shouldBe OK
      )
    }

    "provide an empty model and return a 200 when nothing is fetched using keystore" in {
      when(mockS4lConnector.fetchAndGetFormData[UsedInvestmentReasonBeforeModel](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(None))
      mockEnrolledRequest
      showWithSessionAndAuth(UsedInvestmentReasonBeforeControllerTest.show)(
        result => status(result) shouldBe OK
      )
    }
  }

  "Sending a valid 'Yes' form submit to the UsedInvestmentReasonBeforeController when authenticated and enrolled" should {
    "redirect to the subsidiaries page" in {
      mockEnrolledRequest
      submitWithSessionAndAuth(UsedInvestmentReasonBeforeControllerTest.submit,
        "usedInvestmentReasonBefore" -> Constants.StandardRadioButtonYesValue)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/investment-tax-relief/previous-before-dofcs")
        }
      )
    }
  }

  "Sending a valid 'No' form submit to the UsedInvestmentReasonBeforeController when authenticated and enrolled" should {
    "redirect the ten year plan page" in {
      mockEnrolledRequest
      submitWithSessionAndAuth(UsedInvestmentReasonBeforeControllerTest.submit,
        "usedInvestmentReasonBefore" -> Constants.StandardRadioButtonNoValue)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/investment-tax-relief/new-geographical-market")
        }
      )
    }
  }
  
  "Sending an invalid form submission with validation errors to the UsedInvestmentReasonBeforeController" should {
    "redirect to itself" in {
      mockEnrolledRequest
      submitWithSessionAndAuth(UsedInvestmentReasonBeforeControllerTest.submit,
        "usedInvestmentReasonBefore" -> "")(
        result => {
          status(result) shouldBe BAD_REQUEST
        }
      )
    }
  }

  "Sending a request with no session to UsedInvestmentReasonBeforeController" should {
    "return a 303" in {
      status(UsedInvestmentReasonBeforeControllerTest.show(fakeRequest)) shouldBe SEE_OTHER
    }

    s"should redirect to GG login" in {
      redirectLocation(UsedInvestmentReasonBeforeControllerTest.show(fakeRequest)) shouldBe Some(s"${FrontendAppConfig.ggSignInUrl}?continue=${
        URLEncoder.encode(MockConfig.introductionUrl,"UTF-8")}&origin=investment-tax-relief-submission-frontend&accountType=organisation")
    }
  }

  "Sending an Unauthenticated request with a session to UsedInvestmentReasonBeforeController" should {
    "return a 303" in {
      status(UsedInvestmentReasonBeforeControllerTest.show(fakeRequestWithSession)) shouldBe SEE_OTHER
    }

    s"should redirect to GG login" in {
      redirectLocation(UsedInvestmentReasonBeforeControllerTest.show(fakeRequestWithSession)) shouldBe Some(s"${FrontendAppConfig.ggSignInUrl}?continue=${
        URLEncoder.encode(MockConfig.introductionUrl,"UTF-8")}&origin=investment-tax-relief-submission-frontend&accountType=organisation")
    }
  }

  "Sending a timed-out request to UsedInvestmentReasonBeforeController" should {

    "return a 303 in" in {
      status(UsedInvestmentReasonBeforeControllerTest.show(timedOutFakeRequest)) shouldBe SEE_OTHER
    }

    s"should redirect to timeout page" in {
      redirectLocation(UsedInvestmentReasonBeforeControllerTest.show(timedOutFakeRequest)) shouldBe Some(routes.TimeoutController.timeout().url)
    }
  }

  "Sending a request to UsedInvestmentReasonBeforeController when NOT enrolled" should {

    "return a 303 in" in {
      mockNotEnrolledRequest
      status(UsedInvestmentReasonBeforeControllerTest.show(authorisedFakeRequest)) shouldBe SEE_OTHER
    }

    s"should redirect to Subscription Service" in {
      mockNotEnrolledRequest
      redirectLocation(UsedInvestmentReasonBeforeControllerTest.show(authorisedFakeRequest)) shouldBe Some(FrontendAppConfig.subscriptionUrl)
    }
  }

  "Sending a submission to the UsedInvestmentReasonBeforeController when not authenticated" should {

    "redirect to the GG login page when having a session but not authenticated" in {
      submitWithSessionWithoutAuth(UsedInvestmentReasonBeforeControllerTest.submit)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${FrontendAppConfig.ggSignInUrl}?continue=${
            URLEncoder.encode(MockConfig.introductionUrl,"UTF-8")
          }&origin=investment-tax-relief-submission-frontend&accountType=organisation")
        }
      )
    }
  }

  "Sending a submission to the UsedInvestmentReasonBeforeController with no session" should {

    "redirect to the GG login page with no session" in {
      submitWithoutSession(UsedInvestmentReasonBeforeControllerTest.submit)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${FrontendAppConfig.ggSignInUrl}?continue=${
            URLEncoder.encode(MockConfig.introductionUrl,"UTF-8")
          }&origin=investment-tax-relief-submission-frontend&accountType=organisation")
        }
      )
    }
  }

  "Sending a submission to the UsedInvestmentReasonBeforeController when a timeout has occured" should {
    "redirect to the Timeout page when session has timed out" in {
      submitWithTimeout(UsedInvestmentReasonBeforeControllerTest.submit)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.TimeoutController.timeout().url)
        }
      )
    }
  }

  "Sending a submission to the UsedInvestmentReasonBeforeController when NOT enrolled" should {
    "redirect to the Subscription Service" in {
      mockNotEnrolledRequest
      submitWithSessionAndAuth(UsedInvestmentReasonBeforeControllerTest.submit)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(FrontendAppConfig.subscriptionUrl)
        }
      )
    }
  }

}
