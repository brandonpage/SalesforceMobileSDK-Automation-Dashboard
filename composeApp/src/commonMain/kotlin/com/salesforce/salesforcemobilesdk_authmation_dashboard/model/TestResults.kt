package com.salesforce.salesforcemobilesdk_authmation_dashboard.model

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
@XmlSerialName("testsuites")
data class TestSuites(
    @XmlSerialName("testsuite")
    val testsuites: List<TestSuite> = emptyList()
)

@Serializable
@XmlSerialName("testsuite")
data class TestSuite(
    @XmlElement(false)
    val name: String = "",
    @XmlElement(false)
    val tests: Int = 0,
    @XmlElement(false)
    val failures: Int = 0,
    @XmlElement(false)
    val errors: Int = 0,
    @XmlElement(false)
    val skipped: Int = 0,
    @XmlElement(false)
    val time: Double = 0.0,
    @XmlSerialName("testcase")
    val testcases: List<TestCase> = emptyList()
)

@Serializable
@XmlSerialName("testcase")
data class TestCase(
    @XmlElement(false)
    val name: String = "",
    @XmlElement(false)
    val classname: String = "",
    @XmlElement(false)
    val time: Double = 0.0,
    @XmlSerialName("failure")
    val failure: TestFailure? = null
)

@Serializable
@XmlSerialName("failure")
data class TestFailure(
    @XmlElement(false)
    val message: String = "",
    @XmlValue val content: String = ""
)
