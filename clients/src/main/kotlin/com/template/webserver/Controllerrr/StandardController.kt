package com.template.webserver.Controllerrr

import com.template.webserver.NodeRPCConnection
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle



private const val STANDARD_CONTROLLER_NAME = "config.controller.name"

//@Value("\${$STANDARD_CONTROLLER_NAME}") private val standardControllerName: String

@RestController

@RequestMapping("/")

class StandardController(
        private val rpc: NodeRPCConnection
){
    private val proxy = rpc.proxy
    private val myIdentity = rpc.proxy.nodeInfo().legalIdentities.first().name
    /** Returns the node's name. */
    @GetMapping(value = "/me", produces = arrayOf("application/json"))
    private fun myName() = mapOf("me" to myIdentity.toString())
    /**

     * Returns status

     */
    @GetMapping(value = "/status", produces = arrayOf("application/json"))
    @ResponseBody
    private fun status() = mapOf("status" to "200")
    /**

     * Returns server time

     */
    @GetMapping(value = "/servertime", produces = arrayOf("application/json") )
    private fun getServerTime(): Map<String, Any>{
        val currentDateTime = LocalDateTime.now()
        val date = currentDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
        val time = currentDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM))
        return mapOf("date" to date, "time" to time)
    }

}