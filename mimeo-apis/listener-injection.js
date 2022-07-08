'use strict'

const statusOverrideRegex = /overrideEventStatus:<([^>]+)>/;

exports.fn = function (req, res, callback) {
    if (res.body !== 'OVERRIDE_THIS') return callback(null, res)

    const reqBody = JSON.parse(req.body)
    const eventPayload = JSON.parse(reqBody.eventPayload)

    const statusMatch = reqBody.eventPayload.match(statusOverrideRegex)
    let eventStatus = statusMatch ? statusMatch[1] : "DONE"

    // Skip messages that this "agent" cannot process
    if (
        (reqBody.eventName === "ADDRESSES_UPDATE_AIS_COMMON" && eventPayload.addresses.every(addr => addr.addressType !== 'Physical')) ||
        (reqBody.eventName === "ADDRESSES_UPDATE_TSYS_ADDRESSES" && eventPayload.addresses.every(addr => addr.addressType !== 'Mailing'))
    ) {
        eventStatus = 'SKIPPED'
    }

    const respBody = {
        eventId: reqBody.eventId,
        eventName: reqBody.eventName,
        eventStatus: eventStatus,
        eventContext: {
            "DONE_MESSAGE": "All accounts successfully updated",
        }
    }

    res.status = 200
    res.headers = {'Content-Type': 'application/json'}
    res.body = JSON.stringify(respBody)
    return callback(null, res)
}