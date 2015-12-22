package com.vfpowertech.jsbridge

import com.fasterxml.jackson.annotation.JacksonInject
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import javafx.stage.Stage
import netscape.javascript.JSObject
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import org.slf4j.LoggerFactory
import java.util.*

class SampleService {
    var value: Int = 0
    private val listeners = ArrayList<(Int) -> Unit>()

    fun addListener(listener: (Int) -> Unit) {
        listeners.add(listener)
    }

    fun callListeners(v: Int) {
        listeners.forEach { it(v) }
    }
}

interface JSProxy {
    fun call(methodName: String, methodArgs: String, callbackId: String)
}

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder("value")
data class SampleServiceSetValueArgs(
    @JsonProperty("value") val value: Int
)

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder("listener")
data class SampleServiceAddListenerArgs(
    @JsonProperty("listener") val listener: JSCallbackInt
)

data class V(
    @JsonProperty("p") val p: Int,
    @JsonProperty("q") val q: Int
)

data class R(
    @JsonProperty("p") val p: Int,
    @JsonProperty("q") val q: Int
)

//TODO need to be able to call into js; use a kovevant deferred for this
//need to define an interface of a js service, then generate an impl from it
interface JSService {
    fun syncFn(v: V, n: Int): Promise<R, Exception>
    //fun asyncFn(v: Int): Promise<Int, Exception>
}

//TODO don't generate args class if no args required
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder("v")
data class JSServiceSyncFnArgs(val v: V, val n: Int)

//TODO the dispatcher needs to handle callbacks; just have a map<string, promise>
//need some special callback to tell native->js and js->native calls apart
class JSServiceImpl(private val dispatcher: Dispatcher) : JSService {
    private val objectMapper = ObjectMapper()

    override fun syncFn(v: V, n: Int): Promise<R, Exception> {
        val d = deferred<R, Exception>()
        val args = objectMapper.writeValueAsString(JSServiceSyncFnArgs(v, n))
        //should generate these as methods instead? less mem usage if multiple callbacks to same function
        val resolve: (String) -> Unit = { jsonRetVal ->
            //don't need a special struct for this; just unserialize as whatever the ret type is
            val retVal = objectMapper.readValue(jsonRetVal, R::class.java)
            d.resolve(retVal)
        }
        val reject: (String) -> Unit = { jsonExc ->
            d.reject(RuntimeException(jsonExc))
        }
        dispatcher.callJS("window.jsService", "syncFn", args, resolve, reject)
        return d.promise
    }

    //override fun asyncFn(v: Int): Promise<Int, Exception> {
    //}
}

//two types of functions:
//sync
//async
//also, listeners
class SampleServiceJSProxy(private val base: SampleService, private val dispatcher: Dispatcher) : JSProxy {
    private val log = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper()

    init {
        val injectionableValues = InjectableValues.Std()
        injectionableValues.addValue(Dispatcher::class.java, dispatcher)
        objectMapper.setInjectableValues(injectionableValues)
    }

    //TODO catch exceptions
    override fun call(methodName: String, methodArgs: String, callbackId: String) {
        //sync method, no args example
        if (methodName == "getValue") {
            val json = objectMapper.writeValueAsString(base.value)
            dispatcher.sendValueBackToJS(callbackId, json)
        }
        //sync method, args example
        else if (methodName == "setValue") {
            val args = objectMapper.readValue(methodArgs, SampleServiceSetValueArgs::class.java)
            base.value = args.value
            dispatcher.sendValueBackToJS(callbackId, null)
        }
        //listener registration method example
        else if (methodName == "addListener") {
            val args = objectMapper.readValue(methodArgs, SampleServiceAddListenerArgs::class.java)
            base.addListener(args.listener)
            dispatcher.sendValueBackToJS(callbackId, null)
        }
        else {
            log.error("Unknown method: {} for callbackId={}", methodName, callbackId)
        }
    }
}

//for getting responses from native->js calls
data class PromiseCallbacks(
    val resolve: (String) -> Unit,
    val reject: (String) -> Unit
)

//TODO portable engine interface
//called from js
//needs some abstract send method for shipping stuff back to js
class Dispatcher(private val engine: WebEngine) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val services = HashMap<String, JSProxy>()

    private var nextCallbackId = 0
    private val pendingPromises = HashMap<String, PromiseCallbacks>()

    fun registerService(serviceName: String, service: JSProxy) {
        services[serviceName] = service
    }

    //string to avoid having to recast constantly
    private fun getNextCallbackId(): String {
        val r = nextCallbackId
        ++nextCallbackId
        return r.toString()
    }

    fun call(serviceName: String, methodName: String, methodArgs: String, callbackId: String) {
        log.debug("js->native: {}.{}({}) -> {}", serviceName, methodName, methodArgs, callbackId)

        val service = services[serviceName]
        if (service == null) {
            log.error("Unknown service: {} for callbackId={}", serviceName, callbackId)
            //send error back to js
            return
        }

        service.call(methodName, methodArgs, callbackId)
    }

    fun sendValueBackToJS(callbackId: String, json: String?) {
        log.debug("Dispatching <<<{}>>> for callbackId={}", json, callbackId)
        //TODO need to make sure this is available for each platform
        engine.executeScript("window.dispatcher.sendValue(\"$callbackId\", false, $json);")
    }

    fun sendExcBackToJS(callbackId: String, json: String) {

    }

    //target: something like window.service
    //methodArgs is json
    //need to register a callback with th
    fun callJS(target: String, methodName: String, methodArgs: String, resolve: (String) -> Unit, reject: (String) -> Unit) {
        val callbackId = getNextCallbackId()
        log.debug("native->js: {}.{}({}) -> {}", target, methodName, methodArgs, callbackId)
        //we add this first, as executeScript is sync
        pendingPromises[callbackId] = PromiseCallbacks(resolve, reject)
        //TODO catch exceptions and fail the promise?
        engine.executeScript("window.dispatcher.callFromNative(\"$target\", \"$methodName\", $methodArgs, \"$callbackId\");")
    }

    fun callbackFromJS(callbackId: String, isError: Boolean, jsonRetVal: String) {
        log.debug("response from js: {} -> {}", callbackId, jsonRetVal)
        val callbacks = pendingPromises[callbackId]
        if (callbacks == null) {
            log.error("Value from js received for callbackId={}, but no pending request was found", callbackId)
            return
        }
        pendingPromises.remove(callbackId)

        try {
            if (!isError)
                callbacks.resolve(jsonRetVal)
            else
                callbacks.reject(jsonRetVal)
        }
        catch (e: Throwable) {
            log.error("Unhandled error while resolving callbackId={}", e)
            return
        }
    }
}

//gonna need to generate all possible combinations of these...
//gonna need to hash all the param names together as well (for namespace issues)
@JsonDeserialize(using = JSCallbackIntDeserializer::class)
class JSCallbackInt(
    private val callbackId: String,
    @JacksonInject private val dispatcher: Dispatcher) : (Int) -> Unit {
    override fun invoke(p1: Int) {
        val json = ObjectMapper().writeValueAsString(p1)
        dispatcher.sendValueBackToJS(callbackId, json)
    }
}

class JSCallbackIntDeserializer : JsonDeserializer<JSCallbackInt>() {
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): JSCallbackInt {
        val dispatcher = ctxt.findInjectableValue(Dispatcher::class.java.name, null, null) as Dispatcher
        return JSCallbackInt(jp.valueAsString, dispatcher)
    }
}

class App : Application() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun start(primaryStage: Stage) {
        val vb = VBox()

        val webview = WebView()
        VBox.setVgrow(webview, Priority.ALWAYS)
        vb.children.add(webview)

        val engine = webview.engine

        val objectMapper = ObjectMapper()

        val debugger = engine.impl_getDebugger()
        debugger.isEnabled = true
        val jsLog = LoggerFactory.getLogger("Javascript")
        debugger.setMessageCallback { msg ->
            val root = objectMapper.readTree(msg)
            if (root.has("method")) {
                val method = root.get("method").asText()
                if (method == "Console.messageAdded") {
                    val message = objectMapper.convertValue(root.get("params"), ConsoleMessageAdded::class.java).message
                    val level = message.level
                    val text = "[{}:{}] {}"
                    val args = arrayOf(message.url, message.line, message.text)
                    if (level == "log")
                        jsLog.info(text, *args)
                    else if (level == "error")
                        jsLog.error(text, *args)
                    else
                        println("Unknown level: $level")

                }
            }
            null
        }
        debugger.sendMessage("{\"id\": 1, \"method\": \"Console.enable\"}")

        val dispatcher = Dispatcher(engine)

        val sampleService = SampleService()
        dispatcher.registerService("SampleService", SampleServiceJSProxy(sampleService, dispatcher))

        val window = engine.executeScript("window") as JSObject
        window.setMember("nativeDispatcher", dispatcher)

        val btnBox = HBox()
        vb.children.add(btnBox)

        val notifyBtn = Button("Notify")
        btnBox.children.add(notifyBtn)
        notifyBtn.setOnAction { sampleService.callListeners(5) }

        val jsService = JSServiceImpl(dispatcher)
        val callBtn = Button("Call JS")
        btnBox.children.add(callBtn)
        callBtn.setOnAction {
            jsService.syncFn(V(5, 6), 5) success {
                log.info("Result of syncFn: {}", it)
            } fail {
                log.info("syncFn failed: {}", it)
            }
        }

        engine.load(javaClass.getResource("/index.html").toExternalForm())

        primaryStage.scene = Scene(vb, 852.0, 480.0)
        primaryStage.show()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            //val v = ObjectMapper().writeValueAsString(JSServiceSyncFnArgs(5))
            //println("args: $v")
            //return
            launch(App::class.java, *args)
        }
    }
}

