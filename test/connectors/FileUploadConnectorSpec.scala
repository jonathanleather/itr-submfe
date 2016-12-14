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

package connectors

import config.FrontendAppConfig
import org.scalatest.mock.MockitoSugar

import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class FileUploadConnectorSpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  "FileUploadConnector" should {

    "Use the service url from app config" in {
      FileUploadConnector.serviceURL shouldBe FrontendAppConfig.fileUploadUrl
    }

  }

}