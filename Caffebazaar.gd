extends Node

signal connection_succeed()
signal connection_failed(error)
signal disconnected()

signal purchase_flow_began()
signal failed_to_begin_flow(error)
signal purchase_succeed(purchase_info)
signal purchase_canceled()
signal purchase_failed(error)

signal consume_succeed()
signal consume_failed(error)

signal query_succeed(results)
signal query_failed(error)

var plugin
var plugin_name = "GodotCaffebazaar"

func _ready():
	if Engine.has_singleton(plugin_name):
		plugin = Engine.get_singleton(plugin_name)
		print("initing ...")
		plugin.init("<PUBLIC RSA KEY GOES HERE>")

		var signals := [
			"connection_succeed",
			"connection_failed",
			"disconnected",

			"purchase_flow_began",
			"failed_to_begin_flow",
			"purchase_succeed",
			"purchase_canceled",
			"purchase_failed",

			"consume_succeed",
			"consume_failed",

			"query_succeed",
			"query_failed",
		]

		for sig in signals:
			plugin.connect(sig, self, "_" + sig)

	else:
		print("Could not load plugin: ", plugin_name)

func _connection_succeed():            emit_signal("connection_succeed")
func _connection_failed(error):        emit_signal("connection_failed", error)
func _disconnected():                  emit_signal("disconnected")

func _purchase_flow_began():           emit_signal("purchase_flow_began")
func _failed_to_begin_flow(error):     emit_signal("failed_to_begin_flow", error)
func _purchase_succeed(purchase_info): emit_signal("purchase_succeed", purchase_info)
func _purchase_canceled():             emit_signal("purchase_canceled")
func _purchase_failed(error):          emit_signal("purchase_failed", error)

func _consume_succeed():               emit_signal("consume_succeed")
func _consume_failed(error):           emit_signal("consume_failed", error)

func _query_succeed(results):          emit_signal("query_succeed", results)
func _query_failed(error):             emit_signal("query_failed", error)
