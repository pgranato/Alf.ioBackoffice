/**
 * This file is part of alf.io backoffice.

 * alf.io backoffice is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * alf.io backoffice is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with alf.io backoffice.  If not, see //www.gnu.org/licenses/>.
 */
package alfio.backoffice.task

import alfio.backoffice.Common
import alfio.backoffice.model.AlfioConfiguration
import alfio.backoffice.model.CheckInStatus
import alfio.backoffice.model.Ticket
import alfio.backoffice.model.TicketAndCheckInResult
import android.content.Context
import com.squareup.okhttp.Response
import java.io.Serializable
import java.math.BigDecimal

open class TicketDetailLoader(caller: Context) : AlfioAsyncTask<Ticket, TicketDetailParam, TicketDetailResult>(caller) {
    override fun emptyResult(): TicketDetailResult = TicketDetailResult(CheckInStatus.TICKET_NOT_FOUND, null)
    override fun errorResult(error: Throwable) = TicketDetailResult(CheckInStatus.TICKET_NOT_FOUND, null, error=error)

    override final fun work(param: TicketDetailParam): Pair<TicketDetailParam, TicketDetailResult> {
        val response = performRequest(param)
        val body = response.body()
        try {
            if(response.isSuccessful) {
                val result = Common.gson.fromJson(body.string(), TicketAndCheckInResult::class.java)
                return param to evaluateCheckInResult(result)
            }
            return param to emptyResult()
        } finally {
            body.close()
        }
    }

    protected open fun performRequest(param: TicketDetailParam) : Response {
        return checkInService.getTicketDetail(param.code, param.conf)
    }

    fun evaluateCheckInResult(tcResult: TicketAndCheckInResult): TicketDetailResult {
        val result = tcResult.result
        if(result != null) {
            return TicketDetailResult(result.status, tcResult.ticket, tcResult.result!!.dueAmount, tcResult.result!!.currency)
        }
        return TicketDetailResult(CheckInStatus.TICKET_NOT_FOUND, null)
    }
}

class TicketDetailParam(val conf: AlfioConfiguration, val code: String) : TaskParam
class TicketDetailResult(val status: CheckInStatus, val ticket: Ticket?, val dueAmount: BigDecimal = BigDecimal.ZERO, val currency: String = "", error: Throwable? = null) : TaskResult<Ticket>(ticket, error), Serializable {

    override fun isSuccessful(): Boolean {
        return status.successful
    }
}