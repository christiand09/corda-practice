package com.template.models

import com.fasterxml.jackson.annotation.JsonCreator

data class requestModel(
        val infoOwner : String,
        val requestor : String,
        val name : String,
        val listOfParties : Any,
        val linearId : Any
)

data class createRequest@JsonCreator constructor(
        val infoOwner : String,
        val name : String

)