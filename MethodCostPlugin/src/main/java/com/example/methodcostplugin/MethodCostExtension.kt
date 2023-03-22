package com.example.methodcostplugin

open class MethodCostExtension (
    var logImplClass: String = "",
    var configFilePath: String = "./method_cost_config.json",
    var enabled: Boolean = false,
    var targetBuildType: String = "debug",
    var traceClass: Boolean = false,
)

data class MethodCostFilter (
    val classNameList:List<String> = emptyList(),
    val methodNameList:List<String> = emptyList(),
): java.io.Serializable