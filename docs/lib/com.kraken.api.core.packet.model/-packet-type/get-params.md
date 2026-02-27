//[lib](../../../index.md)/[com.kraken.api.core.packet.model](../index.md)/[PacketType](index.md)/[getParams](get-params.md)

# getParams

[Kraken API]\
open fun [getParams](get-params.md)(): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;

Retrieves a list of parameter names required for the current @PacketType. 

 The specific parameters returned depend on the value of the @PacketType associated with this packet definition. Each @PacketType corresponds to a particular game action or event and requires different parameters. 

 For example: 

- @PacketType.RESUME_NAMEDIALOG or @PacketType.RESUME_STRINGDIALOG require: &quot;length&quot;, &quot;string&quot;.
- @PacketType.OPHELDD requires: &quot;selectedId&quot;, &quot;selectedChildIndex&quot;, &quot;selectedItemId&quot;, &quot;destId&quot;, &quot;destChildIndex&quot;, &quot;destItemId&quot;.
- @PacketType.MOVE_GAMECLICK requires: &quot;worldPointX&quot;, &quot;worldPointY&quot;, &quot;ctrlDown&quot;, &quot;5&quot;.

 Other @PacketType values will similarly yield different parameter lists based on their associated requirements. If no matching @PacketType is set, the method will return `null`.

#### Return

A List&lt;String&gt; containing the parameter names for the current @PacketType, or `null` if no parameters are defined for the current type.
