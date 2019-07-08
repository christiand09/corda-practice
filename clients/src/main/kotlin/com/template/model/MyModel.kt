package com.template.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.template.states.Registered
import net.corda.core.contracts.UniqueIdentifier

data class MyModel @JsonCreator constructor(
        val registered: Registered,
        val verify : Boolean,
        val linearId : UniqueIdentifier
)

data class MyModelRegister @JsonCreator constructor(
        val registered: Registered
)

data class MyModelVerify @JsonCreator constructor(
        val counterParty: String,
        val linearId: UniqueIdentifier
)

data class MyModelUpdate@JsonCreator constructor(
        val registered: Registered,
        val counterParty: String,
        val linearId: UniqueIdentifier
)
data class MyModelUpdateRegister @JsonCreator constructor(
        val registered: Registered,
        val counterParty: String,
        val linearId: UniqueIdentifier
)