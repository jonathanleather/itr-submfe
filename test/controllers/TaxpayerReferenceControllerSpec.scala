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
import config.{FrontendAppConfig, FrontendAuthConnector}
import connectors.{EnrolmentConnector, S4LConnector}
import helpers.FakeRequestHelper
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.OneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import org.scalatest.mock.MockitoSugar

import scala.concurrent.Future

class TaxpayerReferenceControllerSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with OneServerPerSuite with FakeRequestHelper {

  val mockS4lConnector = mock[S4LConnector]

  object TaxpayerReferenceControllerTest extends TaxpayerReferenceController {
    override lazy val applicationConfig = FrontendAppConfig
    override lazy val authConnector = MockAuthConnector
    val s4lConnector: S4LConnector = mockS4lConnector
    override lazy val enrolmentConnector = mock[EnrolmentConnector]
  }

  val addressAsJson =
    """
      |{
      |  "lines":[
      |    "Flat 1",
      |    "Some Street 1",
      |    "Some Place 1"
      |  ],
      |  "town":"Some Town 1",
      |  "postcode":"ZE99 1XZ",
      |  "country":{
      |    "code":"GB",
      |    "name":"UK"
      |  }
      |}""".stripMargin

  val model = TaxpayerReferenceModel("1234567890")
  val emptyModel = TaxpayerReferenceModel("")
  val cacheMap: CacheMap = CacheMap("", Map("" -> Json.toJson(model)))
  val keyStoreSavedTaxpayerReference = TaxpayerReferenceModel("0987654321")

  implicit val hc = HeaderCarrier()

  override def beforeEach() {
    reset(mockS4lConnector)
  }

  private def mockEnrolledRequest = when(TaxpayerReferenceControllerTest.enrolmentConnector.getTAVCEnrolment(Matchers.any())(Matchers.any()))
    .thenReturn(Future.successful(Option(Enrolment("HMRC-TAVC-ORG",Seq(Identifier("TavcReference","1234")),"Activated"))))

  private def mockNotEnrolledRequest = when(TaxpayerReferenceControllerTest.enrolmentConnector.getTAVCEnrolment(Matchers.any())(Matchers.any()))
    .thenReturn(Future.successful(None))

  "TaxpayerReferenceController" should {
    "use the correct keystore connector" in {
      TaxpayerReferenceController.s4lConnector shouldBe S4LConnector
    }
    "use the correct auth connector" in {
      TaxpayerReferenceController.authConnector shouldBe FrontendAuthConnector
    }
    "use the correct enrolment connector" in {
      TaxpayerReferenceController.enrolmentConnector shouldBe EnrolmentConnector
    }
  }

  "Sending a GET request to TaxpayerReferenceController when authenticated and enrolled" should {
    "return a 200 when something is fetched from keystore" in {
      when(mockS4lConnector.saveFormData(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(),Matchers.any())).thenReturn(cacheMap)
      when(mockS4lConnector.fetchAndGetFormData[TaxpayerReferenceModel](Matchers.any())(Matchers.any(), Matchers.any(),Matchers.any()))
        .thenReturn(Future.successful(Option(keyStoreSavedTaxpayerReference)))
      mockEnrolledRequest
      showWithSessionAndAuth(TaxpayerReferenceControllerTest.show)(
        result => status(result) shouldBe OK
      )
    }

    "provide an empty model and return a 200 when nothing is fetched using keystore" in {
      when(mockS4lConnector.saveFormData(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(),Matchers.any())).thenReturn(cacheMap)
      when(mockS4lConnector.fetchAndGetFormData[TaxpayerReferenceModel](Matchers.any())(Matchers.any(), Matchers.any(),Matchers.any()))
        .thenReturn(Future.successful(None))
      mockEnrolledRequest
      showWithSessionAndAuth(TaxpayerReferenceControllerTest.show)(
        result => status(result) shouldBe OK
      )
    }
  }

  "Sending a valid form submit to the TaxpayerReferenceController when authenticated and enrolled" should {
    "redirect to the  company's registered address page" in {
      mockEnrolledRequest
      submitWithSessionAndAuth(TaxpayerReferenceControllerTest.submit, "utr" -> "1234567891")(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/investment-tax-relief/registered-address")
        }
      )
    }
  }

  "Sending an invalid form submission with validation errors to the TaxpayerReferenceController" should {
    "redirect with a bad request" in {
      mockEnrolledRequest
      submitWithSessionAndAuth(TaxpayerReferenceControllerTest.submit, "utr" -> "fff")(
        result => {
          status(result) shouldBe BAD_REQUEST
        }
      )
    }
  }

  "Sending a request with no session to TaxpayerReferenceController" should {
    "return a 303" in {
      status(TaxpayerReferenceControllerTest.show(fakeRequest)) shouldBe SEE_OTHER
    }

    s"should redirect to GG login" in {
      redirectLocation(TaxpayerReferenceControllerTest.show(fakeRequest)) shouldBe Some(s"${FrontendAppConfig.ggSignInUrl}?continue=${
        URLEncoder.encode(MockConfig.introductionUrl,"UTF-8")}&origin=investment-tax-relief-submission-frontend&accountType=organisation")
    }
  }

  "Sending an Unauthenticated request with a session to TaxpayerReferenceController" should {
    "return a 303" in {
      status(TaxpayerReferenceControllerTest.show(fakeRequestWithSession)) shouldBe SEE_OTHER
    }

    s"should redirect to GG login" in {
      redirectLocation(TaxpayerReferenceControllerTest.show(fakeRequestWithSession)) shouldBe Some(s"${FrontendAppConfig.ggSignInUrl}?continue=${
        URLEncoder.encode(MockConfig.introductionUrl,"UTF-8")}&origin=investment-tax-relief-submission-frontend&accountType=organisation")
    }
  }

  "Sending a timed-out request to TaxpayerReferenceController" should {

    "return a 303 in" in {
      status(TaxpayerReferenceControllerTest.show(timedOutFakeRequest)) shouldBe SEE_OTHER
    }

    s"should redirect to timeout page" in {
      redirectLocation(TaxpayerReferenceControllerTest.show(timedOutFakeRequest)) shouldBe Some(routes.TimeoutController.timeout().url)
    }
  }

  "Sending a request to TaxpayerReferenceController when NOT enrolled" should {

    "return a 303 in" in {
      mockNotEnrolledRequest
      status(TaxpayerReferenceControllerTest.show(authorisedFakeRequest)) shouldBe SEE_OTHER
    }

    s"should redirect to Subscription Service" in {
      mockNotEnrolledRequest
      redirectLocation(TaxpayerReferenceControllerTest.show(authorisedFakeRequest)) shouldBe Some(FrontendAppConfig.subscriptionUrl)
    }
  }

  "Sending a submission to the TaxpayerReferenceController when not authenticated" should {

    "redirect to the GG login page when having a session but not authenticated" in {
      submitWithSessionWithoutAuth(TaxpayerReferenceControllerTest.submit)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${FrontendAppConfig.ggSignInUrl}?continue=${
            URLEncoder.encode(MockConfig.introductionUrl,"UTF-8")
          }&origin=investment-tax-relief-submission-frontend&accountType=organisation")
        }
      )
    }
  }

  "Sending a submission to the TaxpayerReferenceController with no session" should {

    "redirect to the GG login page with no session" in {
      submitWithoutSession(TaxpayerReferenceControllerTest.submit)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(s"${FrontendAppConfig.ggSignInUrl}?continue=${
            URLEncoder.encode(MockConfig.introductionUrl,"UTF-8")
          }&origin=investment-tax-relief-submission-frontend&accountType=organisation")
        }
      )
    }
  }

  "Sending a submission to the TaxpayerReferenceController when a timeout has occured" should {
    "redirect to the Timeout page when session has timed out" in {
      submitWithTimeout(TaxpayerReferenceControllerTest.submit)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.TimeoutController.timeout().url)
        }
      )
    }
  }

  "Sending a submission to the TaxpayerReferenceController when NOT enrolled" should {
    "redirect to the Subscription Service" in {
      mockNotEnrolledRequest
      submitWithSessionAndAuth(TaxpayerReferenceControllerTest.submit)(
        result => {
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(FrontendAppConfig.subscriptionUrl)
        }
      )
    }
  }

}
