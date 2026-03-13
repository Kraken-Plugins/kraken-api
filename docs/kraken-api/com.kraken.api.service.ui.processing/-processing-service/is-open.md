//[kraken-api](../../../index.md)/[com.kraken.api.service.ui.processing](../index.md)/[ProcessingService](index.md)/[isOpen](is-open.md)

# isOpen

[Kraken API]\
open fun [isOpen](is-open.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Determines whether the widget corresponding to the specified interface ID is currently open and visible. 

This method retrieves the widget associated with the `InterfaceID.Skillmulti.UNIVERSE`. If the widget is `null`, it returns `false`, indicating that it is not open. Otherwise, it checks the visibility of the widget and returns `true` if the widget is visible.

#### Return

`true` if the widget is open and currently visible; `false` otherwise.
