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

package testOnly.forms

import utils.Transformers._
import models.PreviousSchemeModel
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.data.Form
import utils.Validation._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

object TestPreviousSchemeForm {

  val maxAllowableAmount: Int = 999999999
  val minAllowableAmount: Int = 1
  val otherSchemeMaxLength:Int = 50

  val testPreviousSchemeForm = Form(
    mapping(
      "schemeTypeDesc" -> nonEmptyText,
      "previousSchemeInvestmentAmount" -> text
        .verifying(Messages("validation.common.error.fieldRequired"), mandatoryCheck)
        .verifying(Messages("page.investment.amount.invalidAmount"), integerCheck)
        .transform[Int](stringToInteger, _.toString())
        .verifying(Messages("page.investment.PreviousScheme.investmentAmount.OutOfRange"), minIntCheck(minAllowableAmount))
        .verifying(Messages("page.investment.PreviousScheme.investmentAmount.OutOfRange"), maxIntCheck(maxAllowableAmount)),
      "previousSchemeInvestmentSpent" ->  optional(text
        .verifying(Messages("validation.common.error.fieldRequired"), mandatoryCheck)
        .verifying(Messages("page.investment.amount.invalidAmount"), integerCheck)
        .transform[Int](stringToInteger, _.toString())
        .verifying(Messages("page.investment.PreviousScheme.investmentAmount.OutOfRange"), minIntCheck(minAllowableAmount))
        .verifying(Messages("page.investment.PreviousScheme.investmentAmount.OutOfRange"), maxIntCheck(maxAllowableAmount))),
      "previousSchemeOtherSchemeName" -> optional(nonEmptyText.verifying(Messages("page.investment.PreviousScheme.otherScheme.OutOfRange"), (_.length <= otherSchemeMaxLength))),
      "previousSchemeInvestmentDay" -> optional(number),
      "previousSchemeInvestmentMonth" -> optional(number),
      "previousSchemeInvestmentYear" -> optional(number),
      "previousSchemeProcessingId" -> optional(number)

    )(PreviousSchemeModel.apply)(PreviousSchemeModel.unapply).verifying(previousSchemeValidation))
}
