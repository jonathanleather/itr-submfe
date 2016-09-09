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

import common.Constants
import config.{TavcSessionCache, WSHttp}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SubmissionConnector extends SubmissionConnector with ServicesConfig {
  override val sessionCache = TavcSessionCache
  val serviceUrl = baseUrl("investment-tax-relief-submission")
  val http = WSHttp
}

trait SubmissionConnector {
  val sessionCache: SessionCache
  val serviceUrl: String
  val http: HttpGet with HttpPost with HttpPut

  def validateKiCostConditions(operatingCostYear1: Int, operatingCostYear2: Int, operatingCostYear3: Int,
                               rAndDCostsYear1: Int, rAndDCostsYear2: Int, rAndDCostsYear3: Int)
                              (implicit hc: HeaderCarrier): Future[Option[Boolean]] = {

    http.GET[Option[Boolean]](s"$serviceUrl/investment-tax-relief/knowledge-intensive/check-ki-costs/" +
      s"operating-costs/$operatingCostYear1/$operatingCostYear2/$operatingCostYear3/" +
      s"rd-costs/$rAndDCostsYear1/$rAndDCostsYear2/$rAndDCostsYear3")
  }

  def validateSecondaryKiConditions(hasPercentageWithMasters: Boolean,
                                    hasTenYearPlan: Boolean)
                                   (implicit hc: HeaderCarrier): Future[Option[Boolean]] = {

    http.GET[Option[Boolean]](s"$serviceUrl/investment-tax-relief/knowledge-intensive/check-secondary-conditions/has-percentage-with-masters/" +
      s"$hasPercentageWithMasters/has-ten-year-plan/$hasTenYearPlan")
  }

  def checkLifetimeAllowanceExceeded(isKi: Boolean, previousInvestmentSchemesTotal: Int,
    proposedAmount: Int)
                            (implicit hc: HeaderCarrier): Future[Option[Boolean]] = {

    http.GET[Option[Boolean]](s"$serviceUrl/investment-tax-relief/lifetime-allowance/lifetime-allowance-checker/is-knowledge-intensive/" +
      s"$isKi/previous-schemes-total/$previousInvestmentSchemesTotal/proposed-amount/$proposedAmount")

  }

}