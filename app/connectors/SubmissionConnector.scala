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
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, HttpPut}

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
    
    //http.GET[Option[..TODO pass correct params, call API with params, check response (return an object not just bool)

    // dodgy code just for now - ripped off existing..
    println("========================== IM IN validateKiCostConditions")
    println(s"========================== rd1 is: $rAndDCostsYear1")
    println(s"========================== rd2 is: $rAndDCostsYear2")
    println(s"========================== rd3 is: $rAndDCostsYear3")
    println(s"========================== oc1 is: $operatingCostYear1")
    println(s"========================== oc2 is: $operatingCostYear2")
    println(s"========================== oc3 is: $operatingCostYear3")

    def findCosts(operatingCosts: scala.Double, divideBy: Int): Double = {
      val amount = (operatingCosts / 100) * divideBy
      amount
    }

    if ((rAndDCostsYear1.toDouble >= findCosts(operatingCostYear1.toDouble, Constants.KI10Percent) &&
      rAndDCostsYear2.toDouble >= findCosts(operatingCostYear2.toDouble, Constants.KI10Percent) &&
      rAndDCostsYear3.toDouble >= findCosts(operatingCostYear3.toDouble, Constants.KI10Percent)) ||
      (rAndDCostsYear1.toDouble >= findCosts(operatingCostYear1.toDouble, Constants.KI15Percent) ||
        rAndDCostsYear2.toDouble >= findCosts(operatingCostYear2.toDouble, Constants.KI15Percent) ||
        rAndDCostsYear3.toDouble >= findCosts(operatingCostYear3.toDouble, Constants.KI15Percent)))
      Future(Some(true))
    else
      Future(Some(false))
  }

  def validateSecondaryKiConditions(hasPercentageWithMasters: Option[Boolean],
                                    hasTenYearPlan: Option[Boolean])
                                   (implicit hc: HeaderCarrier): Future[Option[Boolean]] = {
    //http.GET[Option[..TODO pass correct params, call API with params, check response (return an object not just bool)
                     // Handle errors..?

    // dodgy code just for now
    println("========================== IM IN validateSecondaryKiConditions")
    println(s"========================== hasPercentageWithMasters is: $hasPercentageWithMasters")
    println(s"========================== hasTenYearPlan is: $hasTenYearPlan")
    if (hasPercentageWithMasters.getOrElse(false) || hasTenYearPlan.getOrElse(false))
      {
        println("================== result is true")
        Future(Some(true))
      }
    else {
      println("================== result is false")
      Future(Some(false))
    }
  }

  def checkLifetimeAllowanceExceeded(isKi: Boolean, previousInvestmentSchemesTotal: Int,
    proposedAmount: Int)
                            (implicit hc: HeaderCarrier): Future[Option[Boolean]] = {

    // dodgy code for now
    println("========================== IM IN checklifetimeAllowanceExceeded")
    println(s"========================== isKi is: $isKi")
    println(s"========================== proposed invest is: $proposedAmount")
    println(s"========================== previousInvestmentSchemesTotal is: $previousInvestmentSchemesTotal")
    Future(Some(false))

    val maxKi: Int = 20000000
    val maxNonKi: Int = 12000000

    if(isKi)
      {
        Future(Some(proposedAmount + previousInvestmentSchemesTotal > maxKi))
      }
    else
    {
      Future(Some(proposedAmount + previousInvestmentSchemesTotal > maxNonKi))
    }


  }

}